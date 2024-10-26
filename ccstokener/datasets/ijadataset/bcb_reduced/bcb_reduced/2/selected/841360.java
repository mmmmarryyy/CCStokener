package com.fusteeno.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import com.fusteeno.UserPreferences;
import com.fusteeno.liveConnect.LiveConnect;
import com.fusteeno.util.event.AddressListener;

/**
 * 
 * @author aGO!
 * 
 * Classe (singleton) per la fornitura degli indirizzi dei servent. 
 * Questi vengono forniti:
 * - dai servent al momento della fase di handshake tramite il parametro X-Try
 * - dai UHC 
 * - dalla fase di bootstrap attraverso i GWebCache
 *
 * CONTROLLATO
 */
public class ProviderServentsAddress {

    private static ProviderServentsAddress instance = null;

    private final int MAX_SERVENTS = 120;

    private ArrayList<String> servents;

    private ArrayList<String> uhcs;

    private ArrayList<String> gwcs = null;

    private LiveConnect liveConnect = null;

    private ArrayList<AddressListener> addressListeners;

    private UserPreferences userPrefs;

    private ProviderServentsAddress() {
        servents = new ArrayList<String>(MAX_SERVENTS);
        uhcs = new ArrayList<String>();
        gwcs = new ArrayList<String>();
        addressListeners = new ArrayList<AddressListener>(1);
        userPrefs = UserPreferences.getInstance();
        liveConnect = LiveConnect.getInstance();
    }

    public static synchronized ProviderServentsAddress getInstance() {
        if (instance == null) instance = new ProviderServentsAddress();
        return instance;
    }

    public void init() {
        String addrs, tmp;
        if (userPrefs.getServents() != null) addrs = userPrefs.getServents(); else addrs = "";
        tmp = liveConnect.getCookie("servents");
        if (tmp != null) addrs += tmp;
        if ((addrs != null) && !addrs.trim().equals("")) {
            String[] list = addrs.split(",");
            for (int i = 0; i < list.length; i++) servents.add(list[i].trim());
        }
        addrs = liveConnect.getCookie("uhcs");
        if ((addrs != null) && !addrs.trim().equals("")) {
            String[] list = addrs.split(",");
            for (int i = 0; i < list.length; i++) uhcs.add(list[i].trim());
        }
        if (servents.size() == 0) uhcRequestAddress();
    }

    public void close(Collection<String> serventConnected) {
        liveConnect.setCookie("servents", Util.listToString(serventConnected, liveConnect.getCookie("servents")));
        liveConnect.setCookie("uhcs", Util.listToString(uhcs, liveConnect.getCookie("uhcs")));
    }

    public void addAddressListener(AddressListener l) {
        synchronized (addressListeners) {
            addressListeners.add(l);
        }
    }

    public void removeAddressListener(AddressListener l) {
        synchronized (addressListeners) {
            addressListeners.remove(l);
        }
    }

    private void fireAddressEvent() {
        synchronized (addressListeners) {
            Iterator<AddressListener> iter = addressListeners.iterator();
            while (iter.hasNext()) iter.next().addressAdded();
        }
    }

    public String popAddress() {
        String addr = null;
        synchronized (servents) {
            if (servents.size() > 0) {
                addr = servents.remove(servents.size() - 1);
            } else uhcRequestAddress();
        }
        return addr;
    }

    public void pushAddress(String addr) {
        synchronized (servents) {
            if (servents.size() < MAX_SERVENTS && !servents.contains(addr)) {
                servents.add(addr);
                if (servents.size() == 1) fireAddressEvent();
            }
        }
    }

    private void uhcRequestAddress() {
        gwcRequestAddress();
    }

    private void gwcRequestAddress() {
        try {
            if (gwcs.size() == 0) {
                URL url = new URL(Constants.URL_LIST_GWC);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if ((connection != null) && (connection.getResponseCode() == 200)) {
                    BufferedReader bin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String hostCache;
                    Debug.log("[ProviderServentsAddress] Inizio inizializzazione GWC...");
                    try {
                        while ((hostCache = bin.readLine()) != null) gwcs.add(hostCache);
                    } catch (IOException e) {
                        bin.close();
                        throw e;
                    }
                    bin.close();
                    Debug.log("[ProviderServentsAddress] Inizializzazione GWC completata!");
                } else {
                    Debug.log("[ProviderServentsAddress] Problema nella lettura della lista degli GWebCache");
                }
            }
            Iterator iter = gwcs.iterator();
            while (iter.hasNext()) new GWebCache((String) iter.next());
            Debug.log("[ProviderServentsAddress] Richesta GWC completata!");
        } catch (Exception e) {
            Debug.log("[ProviderServentsAddress] Errore nella funzione di bootStrap...");
            Debug.log(e.getMessage());
        }
    }

    private class GWebCache implements Runnable {

        private Thread thread = null;

        private String addr;

        public GWebCache(String addr) {
            this.addr = addr;
            thread = new Thread(this);
            thread.setName("GWebCache: " + addr);
            thread.start();
        }

        /**
		 * Invia il msg di ping
		 * @return true se l'hostcache risponde con PONG, false altrimenti
		 */
        private boolean pingRequest() {
            try {
                URL url = new URL(addr + "/?client=" + Constants.FUSTEENO_NAME + "&version=1.0&ping=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(Constants.CONNECT_SOCKET_TIMEOUT);
                connection.setRequestMethod("GET");
                connection.connect();
                if ((connection != null) && (connection.getResponseCode() == 200)) {
                    BufferedReader bin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String response;
                    try {
                        if (((response = bin.readLine()) == null) || !response.contains("PONG")) {
                            bin.close();
                            return false;
                        }
                    } catch (IOException e) {
                        Debug.log("[ProviderServentsAddress] Errore nella lettura della response...");
                        bin.close();
                        return false;
                    }
                } else {
                    Debug.log("[GWebCache] Responde: " + connection.getResponseMessage());
                    return false;
                }
            } catch (Exception e) {
                Debug.log("[GWebCache] Errore nel ping del GWC '" + addr + "':");
                Debug.log(e.getMessage());
                return false;
            }
            return true;
        }

        private void hostFileRequest() {
            try {
                URL url = new URL(addr + "/?client=" + Constants.FUSTEENO_NAME + "&version=1.0&hostfile=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(Constants.CONNECT_SOCKET_TIMEOUT);
                connection.setRequestMethod("GET");
                connection.connect();
                if ((connection != null) && (connection.getResponseCode() == 200)) {
                    BufferedReader bin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String node;
                    try {
                        while ((node = bin.readLine()) != null) pushAddress(node);
                    } catch (IOException e) {
                        Debug.log("[GWebCache] Errore nella lettura della response del GWebCache...");
                        bin.close();
                        throw e;
                    }
                    bin.close();
                }
            } catch (Exception e) {
                Debug.log("[GWebCache] Errore nella richiesta della lista all'hostCache " + addr + ":");
                Debug.log(e.getMessage());
            }
        }

        public void run() {
            hostFileRequest();
        }
    }
}
