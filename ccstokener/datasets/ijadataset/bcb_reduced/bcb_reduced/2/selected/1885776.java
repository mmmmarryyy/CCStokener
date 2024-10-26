package org.fudaa.fudaa.commun;

import com.memoire.bu.*;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Module de test du reseau.
 *
 * @version      $Revision: 1.5 $ $Date: 2003-11-25 10:13:28 $ by $Author: deniger $
 * @author       Axel von Arnim 
 */
public class FudaaNetworkChecker extends ThreadGroup {

    private BuInformationsSoftware appInfo_;

    Properties properties_;

    int active;

    boolean allStarted;

    public FudaaNetworkChecker(BuInformationsSoftware _appInfo) {
        super("NetCheck");
        appInfo_ = _appInfo;
        properties_ = System.getProperties();
        properties_.put("net.access.update", "local");
        properties_.put("net.access.http", "local");
        properties_.put("net.access.man", "local");
        active = 0;
        allStarted = false;
    }

    public synchronized void check() {
        RemoteConnectTask updateT = new RemoteConnectTask("update", appInfo_.update);
        RemoteConnectTask httpT = new RemoteConnectTask("http", appInfo_.http);
        RemoteConnectTask manT = new RemoteConnectTask("man", appInfo_.man);
        System.err.println("BuNetChecker : checking connections (10s)...");
        updateT.start();
        httpT.start();
        manT.start();
        allStarted = true;
        try {
            wait(20000);
        } catch (InterruptedException e) {
            System.err.println("BuNetChecker : error : interrupted while checking");
        }
        System.err.println("BuNetChecker : check ended");
    }

    protected void tryServerConnection(String service, String urlStr) {
        URL url;
        try {
            url = new URL(urlStr);
            System.err.println(" -- " + service + " : trying " + urlStr + "...");
        } catch (MalformedURLException e1) {
            String msg = e1.toString();
            System.err.println(" -- " + service + " : " + msg + " => local");
            releaseWait(service);
            return;
        }
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                System.err.println(" -- " + service + " : ok => remote");
                properties_.put("net.access." + service, "remote");
                releaseWait(service);
            }
        } catch (IOException e2) {
            String msg = e2.toString();
            System.err.println(" -- " + service + " : " + msg + " => local");
            releaseWait(service);
        }
    }

    protected synchronized void releaseWait(String service) {
        active--;
        if ((allStarted == true) && active == 0) {
            System.err.println(" -- notify");
            notifyAll();
        }
    }

    class RemoteConnectTask extends Thread {

        private String service_;

        private String host_;

        public RemoteConnectTask(String service, String host) {
            service_ = service;
            host_ = host;
        }

        public void start() {
            active++;
            super.start();
        }

        public void run() {
            tryServerConnection(service_, host_);
        }
    }
}
