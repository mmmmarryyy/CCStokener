package com.io_software.utils.web;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

/** The {@link #main} routine takes a string in <tt>args[0]</tt> and
    interprets it as a URL. This URL is opened and the contents are
    printed to <tt>System.out</tt>.

    @see FormContentSearcher#getURLContents
    @author Axel Uhl
    @version $Id:
*/
public class URLContentPrinter {

    public static void main(String[] args) {
        if (args.length == 0) usage();
        try {
            System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
            URL url = new URL(args[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (args.length > 1 && args[1].equals("-n")) connection.setFollowRedirects(false);
            connection.connect();
            for (int i = 0; connection.getHeaderField(i) != null; i++) System.err.println("Field " + i + ": " + connection.getHeaderFieldKey(i) + " / " + connection.getHeaderField(i));
            System.out.println(FormContentSearcher.getURLContents(new URL(args[0])));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.err.println("Usage: java ec.metrics.URLContentPrinter <URL> [ \"-n\" ]");
        System.err.println("The -n option disables the automatic following of redirections");
    }
}
