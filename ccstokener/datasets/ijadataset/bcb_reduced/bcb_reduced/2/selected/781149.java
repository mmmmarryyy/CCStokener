package com.nimium.spizd;

import java.io.*;
import java.net.*;
import java.util.*;

/**
HTTP connector reads list of URLs to test with.
*/
public class HTTPConnector implements Connector {

    /** default url list file name */
    public static String URLListFile = "urllist.txt";

    /** default http proxy port */
    public static int proxyPort = 8080;

    /** read buffer size, default 4k */
    public static int bufferSize = 4096;

    /** read entire url content? if false, only first read is issued */
    public static boolean readFull = true;

    String host;

    URL url;

    InputStream in;

    static long totLen = 0;

    static long maxLen = 0;

    static long readPages = 0;

    static long totPages = 0;

    static Runner runner;

    Connection connection;

    /**
  HTTP auth not implemented, null user accepted
  */
    public void init(Connection c) throws Exception {
        totPages++;
        this.connection = c;
        this.host = c.host;
    }

    /**
  Connect to given link, read the data, disconnect.
  */
    public void connect() throws Exception {
        String link = connection.line;
        url = new URL(link);
        URLConnection conn = null;
        if (host == null) {
            conn = url.openConnection();
            if (runner != null && runner.verboseConnect) System.out.println("Direct connection to " + url);
        } else {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getByName(host), port()));
            conn = url.openConnection(proxy);
            if (runner != null && runner.verboseConnect) System.out.println("Proxy connection to " + url + " through " + proxy);
        }
        in = conn.getInputStream();
        byte[] buff = new byte[bufferSize];
        int curLen = 0;
        int readLen = 0;
        while ((readLen = in.read(buff)) > 0 && readFull) {
            curLen += readLen;
        }
        curLen += readLen;
        in.close();
        totLen += curLen;
        if (maxLen < curLen) maxLen = curLen;
        readPages++;
    }

    /**
  Not implemented
  */
    public boolean login() {
        return false;
    }

    /**
  Nothing to do
  */
    public void close() {
    }

    public String getLogin() {
        return null;
    }

    ;

    public String getPassword() {
        return url.toString();
    }

    ;

    public int port() {
        return proxyPort;
    }

    public String getStatistics() {
        return "Pages read/failed/total: " + readPages + "/" + (totPages - readPages) + "/" + totPages + " read " + totLen + " bytes, max page size " + maxLen;
    }

    /**
  Load http-specific properties contained in Runner object; Runner must have
  been initialized, or Runner.loadProperties() called prior to this call.
  */
    public static void loadProperties(Runner r) {
        runner = r;
        String tmp = r.props.getProperty("http.urlListFile");
        if (tmp != null) URLListFile = tmp;
        tmp = r.props.getProperty("http.proxyServer");
        if (tmp != null && r.host == null) r.host = tmp;
        tmp = r.props.getProperty("http.proxyPort");
        if (tmp != null) proxyPort = Integer.parseInt(tmp);
        tmp = r.props.getProperty("http.bufferSize");
        if (tmp != null) bufferSize = Integer.parseInt(tmp);
        tmp = r.props.getProperty("http.readFull");
        if ("false".equals(tmp)) readFull = false; else if ("true".equals(tmp)) readFull = true;
    }

    public static void main(String[] args) throws Exception {
        String proxy = null;
        if (args.length == 0) {
            System.out.println("USAGE: spizd <urllistfile> [proxy]");
            System.exit(1);
        } else {
            if (args.length > 1) proxy = args[1];
        }
        Runner r = new Runner();
        r.init(proxy, null, HTTPConnector.class);
        loadProperties(r);
        URLListFile = args[0];
        r.dictRead.dictFile = URLListFile;
        System.out.println("Reading url list file " + URLListFile + ", using proxy: " + r.host);
        r.start();
    }
}
