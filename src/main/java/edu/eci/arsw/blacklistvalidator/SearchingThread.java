package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

/**
 * Un SearchingThread ya NO guarda su propio contador ni su propia lista.
 * Todo el estado compartido (conteo total y lista de ocurrencias) vive en
 * HostBlackListsValidator, protegido con métodos synchronized.
 * Esto evita tener dos mecanismos de sincronización distintos (uno acá,
 * otro allá) que podrían quedar inconsistentes entre sí.
 */
public class SearchingThread extends Thread {

    private final int a, b;
    private final String ipaddress;
    private final HostBlackListsValidator validator; // referencia al estado compartido
    private final HostBlacklistsDataSourceFacade skds;

    public SearchingThread(int a, int b, String ipaddress,HostBlackListsValidator validator, HostBlacklistsDataSourceFacade skds) {
        this.a = a;
        this.b = b;
        this.ipaddress = ipaddress;
        this.validator = validator;
        this.skds = skds;
    }

    @Override
    public void run() {
        for (int i = a; i < b; i++) {

            // Chequeo de "¿ya alguien más (u otro hilo) llegó a 5?" ANTES de
            // seguir trabajando. Esto NO es espera activa: no hay un loop
            // vacío esperando, es simplemente una condición de salida dentro
            // de un for que sigue haciendo trabajo útil en cada vuelta.
            if (validator.alarmThresholdReached()) {
                return; // este hilo ya no tiene nada más que hacer
            }

            // La consulta en sí NO necesita synchronized: HostBlacklistsDataSourceFacade
            // ya es Thread-Safe según el README, así que no compite por ningún lock acá.
            if (skds.isInBlackListServer(i, ipaddress)) {
                // Esto SÍ modifica estado compartido -> pasa por un método
                // synchronized del validator (ver clase HostBlackListsValidator).
                validator.reportOcurrence(i);
            }
        }
    }
}