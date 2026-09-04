package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/***
 * Establishes the main components of a Thread: from the life cycle to the ipdadresses it should compare. 
 * 
 */
public class SearchingThread extends Thread {

    int a,b; 
    String ipaddress; 
    int max; 
    int ocurrencesCount;
    int ocurrences; // adding attribute of number of threads 
     List<Integer> blackListOcurrences;
    HostBlacklistsDataSourceFacade skds=HostBlacklistsDataSourceFacade.getInstance();
    LinkedList<SearchingThread> threads = new LinkedList<>();
    
    public SearchingThread (int a, int b, String ipaddress, HostBlacklistsDataSourceFacade skds, int max) {
        this.a = a;
        this.b = b;
        this.ipaddress = ipaddress; 
        this.skds = skds; 
        this.max = max;
    }


    @Override 
    public void run() { 
        
    for (int i=a; i<b; i++) { 
        if(skds.isInBlackListServer(i,ipaddress)){
            ocurrencesCount++;
            blackListOcurrences.add(i);  
        }
    }


    // Joining the threads at the end
    for (SearchingThread t: threads) {
        try { 
            t.join();
        } catch (InterruptedException e) {
         return; 
        } }
    } 
    

    public int getOcurrencesCount() {
         return ocurrencesCount;
    }

    public List<Integer> getBlackListOcurrences(){
    return blackListOcurrences; 
    }

}








    

    
    


