package com.luisfcanals.techan.data;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Gets current prices and stores it.
 */
public class SimpleHttpGet {

    private static final Logger log = Logger.getLogger(SimpleHttpGet.class.getPackage().getClass().getName());

    public static void main(final String args[]) throws Exception {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        final SimpleHttpGet getter = new SimpleHttpGet(args[1]);
        getter.refresh();
        final FileWriter output = new FileWriter(args[0] + "." + formatter.format(new Date()) + ".txt");
        output.write(getter.toString());
        output.flush();
        output.close();
    }

    private final URL url;

    private String content;

    public SimpleHttpGet(final String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public void refresh() throws IOException {
        final URLConnection c = this.url.openConnection();
        final BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        String inputLine;
        final StringBuffer cx = new StringBuffer();
        boolean isTheFirst = true;
        while ((inputLine = in.readLine()) != null) {
            if (!isTheFirst) {
                cx.append('\n');
            }
            cx.append(inputLine);
            isTheFirst = false;
        }
        in.close();
        this.content = cx.toString();
    }

    public String toString() {
        return content;
    }
}
