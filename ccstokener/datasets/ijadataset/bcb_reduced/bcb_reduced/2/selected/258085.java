package net.sourceforge.seriesdownloader.controller.wget;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Allows reading the source of GET and POST requests.
 * @author nweem
 */
public class WGet {

    private StringBuffer buffer = new StringBuffer();

    private final String commandName = WGet.class.getName();

    private int count;

    private boolean verb;

    private boolean output = true;

    private ArrayList<Byte> bytes = new ArrayList<Byte>();

    /**
	 * This class cannot be instantiated.
	 */
    private WGet() {
    }

    /**
	 * Gets the source of an url using a GET request.
	 * @param urlS The url of which the source is needed.
	 * @return The source of the specified URL.
	 */
    public static String getSource(String urlS) {
        WGet wget = new WGet();
        wget.buffer = new StringBuffer();
        try {
            URLConnection url = (new URL(urlS.replaceAll(" ", "+"))).openConnection();
            if (url instanceof HttpURLConnection) {
                wget.readHttpURL((HttpURLConnection) url);
            } else {
                wget.readURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return wget.buffer.toString();
    }

    /**
	 * Returns the source of the url, using the given parameters. This method performs a POST request.
	 * @param urlS The url of which the source is needed.
	 * @param keys An array with key's for the values.
	 * @param values An array with values for each key.
	 * @return The bytes from the result.
	 */
    public static Byte[] getSource(String urlS, String[] keys, String[][] values) {
        return getSource(urlS, keys, values, null);
    }

    /**
	 * Returns the source of the url, using the given parameters. This method performs a POST request.
	 * @param urlS The url of which the source is needed.
	 * @param keys An array with key's for the values.
	 * @param values An array with values for each key.
	 * @return The bytes from the result.
	 */
    public static Byte[] getSource(String urlS, String[] keys, String[][] values, WGetResult result) {
        WGet wget = new WGet();
        wget.buffer = new StringBuffer();
        try {
            URLConnection url = (new URL(urlS.replaceAll(" ", "+"))).openConnection();
            if (url instanceof HttpURLConnection) {
                wget.readHttpURL((HttpURLConnection) url, keys, values, result);
            } else {
                wget.readURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return wget.bytes.toArray(new Byte[wget.bytes.size()]);
    }

    /**
	 * Reads the source from an url
	 * @param url The url
	 * @throws IOException
	 */
    public final void readURL(URLConnection url) throws IOException {
        DataInputStream in = new DataInputStream(url.getInputStream());
        printHeader(url);
        try {
            while (true) {
                writeChar((char) in.readUnsignedByte());
            }
        } catch (EOFException e) {
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": Read " + count + " bytes from " + url.getURL());
        } catch (IOException e) {
            buffer.append(e + ": " + e.getMessage());
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": Read " + count + " bytes from " + url.getURL());
        }
    }

    /**
	 * Reads the source from an HttpUrl
	 * @param url The url
	 * @throws IOException
	 */
    public final void readHttpURL(HttpURLConnection url) throws IOException {
        long before, after;
        url.setAllowUserInteraction(true);
        verbose(commandName + ": Contacting the URL ...");
        url.connect();
        verbose(commandName + ": Connect. Waiting for reply ...");
        before = System.currentTimeMillis();
        DataInputStream in = new DataInputStream(url.getInputStream());
        after = System.currentTimeMillis();
        verbose(commandName + ": The reply takes " + ((int) (after - before) / 1000) + " seconds");
        before = System.currentTimeMillis();
        try {
            if (url.getResponseCode() != HttpURLConnection.HTTP_OK) {
                buffer.append(commandName + ": " + url.getResponseMessage());
            } else {
                printHeader(url);
                while (true) {
                    writeChar((char) in.readUnsignedByte());
                }
            }
        } catch (EOFException e) {
            after = System.currentTimeMillis();
            int milliSeconds = (int) (after - before);
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": Read " + count + " bytes from " + url.getURL());
            verbose(commandName + ": HTTP/1.0 " + url.getResponseCode() + " " + url.getResponseMessage());
            url.disconnect();
            verbose(commandName + ": It takes " + (milliSeconds / 1000) + " seconds" + " (at " + round(count / (float) milliSeconds) + " K/sec).");
            if (url.usingProxy()) {
                verbose(commandName + ": This URL uses a proxy");
            }
        } catch (IOException e) {
            buffer.append(e + ": " + e.getMessage());
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": I/O Error : Read " + count + " bytes from " + url.getURL());
            buffer.append(commandName + ": I/O Error " + url.getResponseMessage());
        }
    }

    /**
	 * Reads the source from an URL using a POST command.
	 * @param url The URL.
	 * @param keys The key values.
	 * @param values The values.
	 * @throws IOException
	 */
    public final void readHttpURL(HttpURLConnection url, String[] keys, String[][] values, WGetResult result) throws IOException {
        if (result == null) {
            result = new WGetResult();
        }
        long before, after;
        url.setAllowUserInteraction(true);
        url.setDoOutput(true);
        url.setDoInput(true);
        url.setRequestMethod("POST");
        url.setRequestProperty("Accept-Encoding", "gzip,deflate");
        StringBuffer data = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) {
                data.append("&");
            }
            data.append(URLEncoder.encode(keys[i], "UTF-8") + "=");
            for (int j = 0; j < values[i].length; j++) {
                if (j != 0) {
                    data.append(";");
                }
                data.append(URLEncoder.encode(values[i][j], "UTF-8"));
            }
        }
        OutputStreamWriter wr = new OutputStreamWriter(url.getOutputStream());
        wr.write(data.toString());
        wr.flush();
        verbose(commandName + ": Contacting the URL ...");
        url.connect();
        verbose(commandName + ": Connect. Waiting for reply ...");
        before = System.currentTimeMillis();
        result.setEncoding(url.getContentEncoding());
        DataInputStream in = new DataInputStream(url.getInputStream());
        after = System.currentTimeMillis();
        verbose(commandName + ": The reply takes " + ((int) (after - before) / 1000) + " seconds");
        before = System.currentTimeMillis();
        try {
            if (url.getResponseCode() != HttpURLConnection.HTTP_OK) {
                buffer.append(commandName + ": " + url.getResponseMessage());
            } else {
                printHeader(url);
                while (true) {
                    byte b = (byte) in.readUnsignedByte();
                    bytes.add(b);
                    writeChar((char) b);
                }
            }
        } catch (EOFException e) {
            after = System.currentTimeMillis();
            int milliSeconds = (int) (after - before);
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": Read " + count + " bytes from " + url.getURL());
            verbose(commandName + ": HTTP/1.0 " + url.getResponseCode() + " " + url.getResponseMessage());
            url.disconnect();
            verbose(commandName + ": It takes " + (milliSeconds / 1000) + " seconds" + " (at " + round(count / (float) milliSeconds) + " K/sec).");
            if (url.usingProxy()) {
                verbose(commandName + ": This URL uses a proxy");
            }
        } catch (IOException e) {
            buffer.append(e + ": " + e.getMessage());
            if (output) {
                verbose("\n");
            }
            verbose(commandName + ": I/O Error : Read " + count + " bytes from " + url.getURL());
            buffer.append(commandName + ": I/O Error " + url.getResponseMessage());
        }
    }

    /**
	 * Prints some verbose information in the result.
	 * @param url The url.
	 */
    public final void printHeader(URLConnection url) {
        verbose(WGet.class.getName() + ": Content-Length   : " + url.getContentLength());
        verbose(WGet.class.getName() + ": Content-Type     : " + url.getContentType());
        if (url.getContentEncoding() != null) {
            verbose(WGet.class.getName() + ": Content-Encoding : " + url.getContentEncoding());
        }
        if (output) {
            verbose("");
        }
    }

    /**
	 * Writs a character to the buffer.
	 * @param c The character that was read.
	 */
    public final void writeChar(char c) {
        if (output) {
            buffer.append(c);
        }
        count++;
    }

    /**
	 * Appends a string to the buffer if the <code>verb</code> property is <code>true</code>
	 * @param s The string that needs to be appended.
	 */
    public final void verbose(String s) {
        if (verb) {
            buffer.append(s);
        }
    }

    /**
	 * Returns a float rounded to 2 digits.
	 * @param f A fload that needs to be rounded.
	 * @return The float, rounded to 2 digits.
	 */
    public static final float round(float f) {
        return Math.round(f * 100) / (float) 100;
    }
}
