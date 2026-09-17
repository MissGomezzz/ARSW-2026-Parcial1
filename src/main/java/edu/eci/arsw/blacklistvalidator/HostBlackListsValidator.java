package edu.eci.arsw.blacklistvalidator;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT = 5;
    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

    // --- Estado COMPARTIDO entre todos los SearchingThread ---
    // Por eso cualquier método que lo lea o modifique debe ser synchronized.
    private int ocurrencesCount = 0;
    private final LinkedList<Integer> blackListOcurrences = new LinkedList<>();

    private final HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();

    /**
     * Llamado por CUALQUIER SearchingThread cuando encuentra la IP en una
     * blacklist. synchronized garantiza que solo un hilo a la vez ejecuta
     * este bloque -> sin esto, dos hilos podrían hacer ocurrencesCount++
     * "al mismo tiempo" y perder un incremento (race condition clásica).
     */
    public synchronized void reportOcurrence(int serverIndex) {
        ocurrencesCount++;
        blackListOcurrences.add(serverIndex);
    }

    /**
     * También synchronized: si no lo fuera, un hilo podría leer
     * ocurrencesCount justo mientras otro hilo está a la mitad de
     * reportOcurrence(), y leer un valor "a medias" o desactualizado
     * (esto se llama un problema de visibilidad de memoria entre hilos,
     * no solo de exclusión mutua). synchronized en AMBOS métodos soluciona
     * ambos problemas (exclusión + visibilidad) a la vez.
     */
    public synchronized boolean alarmThresholdReached() {
        return ocurrencesCount >= BLACK_LIST_ALARM_COUNT;
    }

    public synchronized int getOcurrencesCount() {
        return ocurrencesCount;
    }

    /**
     * Reparte [0, numberServers) en nThreads segmentos lo más parejos
     * posible, repartiendo el residuo (numberServers % nThreads) en los
     * primeros 'residue' segmentos, uno de más cada uno.
     * Esto reemplaza tu cálculo anterior (range*(i-1)) que podía dar
     * índices negativos y dejaba servidores sin cubrir.
     */
    public List<Integer> checkHost(String ipaddress, int nThreads) {

        int numberServers = skds.getRegisteredServersCount();
        int base = numberServers / nThreads;
        int residue = numberServers % nThreads;

        List<SearchingThread> threads = new LinkedList<>();
        int start = 0;

        for (int i = 0; i < nThreads; i++) {
            // Los primeros 'residue' hilos reciben un servidor extra,
            // así se cubren TODOS los servidores sin dejar huecos y sin
            // pasarnos del rango (par o impar, ambos casos quedan cubiertos).
            int size = base + (i < residue ? 1 : 0);
            int end = start + size;

            SearchingThread t = new SearchingThread(start, end, ipaddress, this, skds);
            threads.add(t);
            t.start(); // arranca el hilo (llama a run() en paralelo)

            start = end;
        }

        // --- Aquí está el "wait" que pedía el README, sin escribirlo a mano ---
        // join() bloquea el hilo principal hasta que ESE hilo termine su run().
        // Internamente, join() está implementado con wait()/notifyAll() sobre
        // el propio objeto Thread (la JVM notifica cuando el hilo muere).
        // Por eso NO es sleep ni espera activa: el hilo principal queda
        // bloqueado (no consume CPU) hasta que cada hijo termine.
        //
        // Nota clave: un hilo puede terminar "antes de tiempo" porque ya vio
        // alarmThresholdReached()==true y salió con return en su run().
        // join() sobre ese hilo retorna casi inmediatamente, sin problema.
        for (SearchingThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // Si el hilo principal es interrumpido mientras espera,
                // marcamos la interrupción y salimos del método.
                Thread.currentThread().interrupt();
                return blackListOcurrences;
            }
        }

        // En este punto TODOS los hilos ya terminaron (o porque acabaron su
        // segmento, o porque salieron temprano al llegar a la alarma).
        if (getOcurrencesCount() >= BLACK_LIST_ALARM_COUNT) {
            skds.reportAsNotTrustworthy(ipaddress);
            LOG.log(Level.INFO, "HOST {0} Reported as NOT trustworthy", ipaddress);
        } else {
            skds.reportAsTrustworthy(ipaddress);
            LOG.log(Level.INFO, "HOST {0} Reported as trustworthy", ipaddress);
        }

        return blackListOcurrences;
    }
}