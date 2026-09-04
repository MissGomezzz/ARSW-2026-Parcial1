package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchingThread extends Thread {

    int a,b; 
    String ipaddress; 
    int max; 
    HostBlacklistsDataSourceFacade skds=HostBlacklistsDataSourceFacade.getInstance();
    
    public SearchingThread (int a, int b, String ipaddress, HostBlacklistsDataSourceFacade skds, int max) {
        this.a = a;
        this.b = b;
        this.ipaddress = ipaddress; 
        this.skds = skds; 
        this.max = max;
    }

    @Override 
    public void run() { 
        
    //    for (int i=a; i<b; i++) { 
    //         if(skds.isInBlackListServer(i,ipaddress)){
    //             ocurrencesCount++;
    //             blackListOcurrences.add(i);
    //         }
            
    //     }
        
    // }
        
    // public int getOcurrencesCount() {
    //     return ocurrencesCount;
    // }

    // public LinkedList<Integer> getBlackListOcurrences(){
    //     return blackListOcurrences; 
    }

}




    

    
    


