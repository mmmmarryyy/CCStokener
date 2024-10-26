package com.sri.emo.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * This is a sample client that can use grab the contents of a page.  The single command line
 * argument is the URL to retrieve.  The result is sent to standard out.
 * @author Michael Rimov
 *
 */
public class SSLClient {

    /**
	 * URL to connect to.  Should include full SCHEME (http/https) and port.
	 */
    private String url;

    /**
	 * Main method.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
    public void run() throws UnknownHostException, IOException {
        URL url = new URL(getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        try {
            int oneChar;
            while ((oneChar = is.read()) != -1) {
                System.out.print((char) oneChar);
            }
        } finally {
            is.close();
        }
        connection.disconnect();
    }

    /**
	 * Retrieves the URL to connect.
	 * @return
	 */
    protected String getUrl() {
        assert url != null;
        assert url.length() > 0;
        return url;
    }

    /**
	 * Sets the URL to connect to.
	 * @param url
	 */
    protected void setUrl(String url) {
        this.url = url;
    }

    /**
	 * Runs the program.
	 * @param args  The first argument should be the URL to connect to.
	 */
    public static void main(String[] args) throws Exception {
        SSLClient sslClient = new SSLClient();
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }
        sslClient.setUrl(args[0]);
        sslClient.run();
    }

    private static void usage() {
        System.out.println("Usage: SSLClient  [url]");
    }
}
