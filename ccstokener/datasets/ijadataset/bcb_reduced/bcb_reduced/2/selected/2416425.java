package com;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author Wes Billman
 * Created: Nov 28, 2006
 */
public class AvailableAddons {

    private final String INDEX_FILE_NAME = "descript.ion";

    private final boolean bDebug = false;

    public final String SUCCESS = "SUCCESS";

    private ArrayList<WowAceAddon> addons;

    public AvailableAddons() {
        addons = new ArrayList<WowAceAddon>();
    }

    /**
     * Reload the wowace file list
     * @param wowAceFilesURL - URL for the wowace files
     * @param proxyEnabled - "true" or "false" for proxy enabled
     * @param proxyHost - The proxy host
     * @param proxyPort - The proxy port
     * @return - Result of the reloading operation.
     */
    public String reloadAvailableAddons(String wowAceFilesURL, String proxyEnabled, String proxyHost, String proxyPort) {
        String result;
        addons.clear();
        if (proxyEnabled.equals("true")) {
            result = doURLRequest(wowAceFilesURL + INDEX_FILE_NAME, proxyHost, Integer.parseInt(proxyPort));
        } else {
            result = doURLRequest(wowAceFilesURL + INDEX_FILE_NAME, "", -1);
        }
        return result;
    }

    private String doURLRequest(String strURL, String strProxy, int iProxyPort) {
        String result = SUCCESS;
        URL url = null;
        URLConnection c = null;
        try {
            System.out.println("\nHTTP Request: " + strURL);
            URL urlOriginal = new URL(strURL);
            if ((null != strProxy) && (0 < strProxy.length())) {
                URL urlProxy = new URL(urlOriginal.getProtocol(), strProxy, iProxyPort, strURL);
                System.out.println("Using Proxy: " + strProxy);
                if (-1 != iProxyPort) {
                    System.out.println("Using Proxy Port: " + iProxyPort);
                }
                url = urlProxy;
            } else {
                url = urlOriginal;
            }
            c = url.openConnection();
            if (c instanceof HttpURLConnection) {
                HttpURLConnection h = (HttpURLConnection) c;
                h.connect();
                String strStatus = h.getResponseMessage() + " (" + h.getResponseCode() + ")";
                System.out.println("HTTP Status: " + strStatus);
                System.out.println("HTTP Response Headers: ");
                for (int i = 1; ; i++) {
                    String strKey = h.getHeaderFieldKey(i);
                    if (null == strKey) {
                        break;
                    }
                    System.out.println(i + ": " + strKey + ": " + h.getHeaderField(i));
                }
                String strContentType = h.getContentType();
                if ((null != strContentType) && (0 == strContentType.compareTo("application/octet-stream"))) {
                    if (bDebug) System.out.println("Received text/html:[");
                    int iNumLines = 0;
                    try {
                        InputStream in = h.getInputStream();
                        BufferedReader data = new BufferedReader(new InputStreamReader(in));
                        String line = null;
                        String[] items;
                        WowAceAddon aceAddon;
                        while ((line = data.readLine()) != null) {
                            items = line.split("\t");
                            aceAddon = new WowAceAddon(items[0], items[1], "", "");
                            addons.add(aceAddon);
                            iNumLines++;
                        }
                    } catch (Exception exc2) {
                        System.out.println("**** IO failure: " + exc2.toString());
                        result = "**** IO failure ****\n" + exc2.getMessage();
                    } finally {
                        if (bDebug) System.out.println("]");
                        System.out.println("Received application/octet-stream has " + iNumLines + " lines");
                    }
                } else {
                    result = "**** Connection Failure ****\n" + "Status - " + strStatus + "\n";
                }
                h.disconnect();
            } else {
                System.out.println("**** No download: connection was not HTTP");
                result = "**** No Download ****\n" + "  Connection was not HTTP";
            }
        } catch (Exception exc) {
            System.out.println("**** Connection failure: " + exc.toString());
            result = "\n**** Connection Failure ****\n" + exc.getMessage() + "\n" + "Check your internet connection and the proxy settings in Settings";
        } finally {
            c = null;
            url = null;
        }
        return result;
    }

    private String getAddonName(File addon) {
        String addonName = null;
        return addonName;
    }

    private String getAddonDescription(File addon) {
        String addonDesc = null;
        return addonDesc;
    }

    private String getAddonVersion(File addon) {
        String addonVersion = null;
        return addonVersion;
    }

    public ArrayList<WowAceAddon> getAddons() {
        return addons;
    }
}
