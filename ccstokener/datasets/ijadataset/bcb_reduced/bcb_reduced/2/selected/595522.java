package org.jfeedback.transport;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import com.anthonyeden.lib.config.Configuration;
import com.anthonyeden.lib.config.ConfigurationException;
import com.anthonyeden.lib.util.IOUtilities;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jfeedback.FeedbackData;
import org.jfeedback.FeedbackTransport;
import org.jfeedback.TransportException;

/**
 * Transport agent which sends feedback data using HTTP.
 *
 * @author Anthony Eden
 * @todo Switch from JDOM to xmlenc for producing XML documents
 */
public class HTTPTransport implements FeedbackTransport {

    public static final String NAME = "HTTP Transport";

    public static final String DESCRIPTION = "Send feedback data using the HTTP protocol";

    private URL url;

    public HTTPTransport() {
    }

    /**
     * Configure the transport agent.
     *
     * @param configuration The Configuration object
     * @throws ConfigurationException Thrown if a configuration error occurs
     */
    public void configure(Configuration configuration) throws ConfigurationException {
        String urlString = configuration.getChildValue("url");
        if (urlString == null) {
            throw new ConfigurationException("HTTP transport requires URL configuration element");
        }
        try {
            url = new URL(urlString);
        } catch (Exception e) {
            throw new ConfigurationException("Bad URL in HTTP transport configuration", e);
        }
    }

    public String getName() {
        return NAME;
    }

    /**
     * Get a description of the transport agent.
     *
     * @return The agent description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Send the FeedbackData.
     *
     * @param data The data to send
     * @throws TransportException Thrown if an error occurs during sending
     */
    public void send(FeedbackData data) throws TransportException {
        try {
            URLConnection conn = (URLConnection) url.openConnection();
            String dataString = toXML(data);
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = null;
                try {
                    httpConnection = (HttpURLConnection) conn;
                    httpConnection.setDoOutput(true);
                    httpConnection.setRequestMethod("POST");
                    httpConnection.setRequestProperty("Content-Length", Integer.toString(dataString.length()));
                    httpConnection.connect();
                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(new OutputStreamWriter(httpConnection.getOutputStream()));
                        out.print(dataString);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new TransportException("Error sending data", e);
                    } finally {
                        IOUtilities.close(out);
                    }
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new TransportException("Bad HTTP server response: " + responseCode);
                    }
                } finally {
                    httpConnection.disconnect();
                }
            } else {
                throw new TransportException("Connection must be to an HTTP server");
            }
        } catch (TransportException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new TransportException("Error during data send", e);
        }
    }

    private String toXML(FeedbackData data) throws IOException {
        StringWriter writer = new StringWriter();
        Element rootElement = new Element("feedback");
        rootElement.addContent(new Element("error").addContent(data.getError()));
        rootElement.addContent(new Element("user-comment").addContent(data.getUserComments()));
        rootElement.addContent(new Element("user-email").addContent(data.getUserEmail()));
        rootElement.addContent(new Element("type").addContent(data.getType().toString()));
        Document document = new Document(rootElement);
        XMLOutputter outputter = new XMLOutputter();
        outputter.output(document, writer);
        writer.flush();
        return writer.toString();
    }
}
