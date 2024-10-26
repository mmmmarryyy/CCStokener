package org.vizzini.util.xml;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides convenience methods to send XML documents.
 *
 * @author   Jeffrey M. Thompson
 * @version  v0.4
 * @since    v0.3
 */
public class XMLTransporter {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(XMLTransporter.class.getName());

    /**
     * Send the given XML message to the given URL.
     *
     * @param   urlStr              Destination URL as a <code>String</code>.
     * @param   xmlMessage          XML message.
     * @param   isResponseExpected  Flag indicating if a response is expected.
     *
     * @return  the response string, or null if no response is expected.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     * @throws  IOException            if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String sendXml(String urlStr, String xmlMessage, boolean isResponseExpected) throws MalformedURLException, IOException {
        URL url = new URL(urlStr);
        return sendXml(url, xmlMessage, isResponseExpected);
    }

    /**
     * Send the given XML message to the given URL.
     *
     * @param   url                 Destination URL.
     * @param   xmlMessage          XML message.
     * @param   isResponseExpected  Flag indicating if a response is expected.
     *
     * @return  the response string, or null if no response is expected.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String sendXml(URL url, String xmlMessage, boolean isResponseExpected) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (xmlMessage == null) {
            throw new IllegalArgumentException("xmlMessage == null");
        }
        LOGGER.finer("url = " + url);
        LOGGER.finer("xmlMessage = :" + xmlMessage + ":");
        LOGGER.finer("isResponseExpected = " + isResponseExpected);
        String answer = null;
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Content-type", "text/xml");
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(xmlMessage);
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
            LOGGER.finer("message written");
            StringBuilder sb = new StringBuilder();
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                if (isResponseExpected) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine).append("\n");
                    }
                    answer = sb.toString();
                    LOGGER.finer("response read");
                }
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.SEVERE, "No response", e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (ConnectException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        LOGGER.finer("answer = :" + answer + ":");
        return answer;
    }
}
