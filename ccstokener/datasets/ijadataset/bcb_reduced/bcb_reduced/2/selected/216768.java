package uk.ac.osswatch.simal.wicket.widgets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.osswatch.simal.SimalProperties;
import uk.ac.osswatch.simal.rdf.SimalException;
import uk.ac.osswatch.simal.rdf.SimalRepositoryException;

/**
 * A connection to a Woolkie server. This maintains the necessary data for
 * connecting to the server and provides utility methods for making common calls
 * via the Wookie REST API.
 * 
 */
public class WookieServerConnection implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WookieServerConnection.class);

    private static final long serialVersionUID = -3547677143210986405L;

    private HashMap<String, Widget> widgets = new HashMap<String, Widget>();

    private boolean available = false;

    private String url;

    public static final String UNAVAILABLE_MSG = "The Wookie server is currently unavailable.";

    public WookieServerConnection() {
        initialise();
    }

    public void initialise() {
        try {
            initURL();
            generateAvailableWidgets();
            available = true;
        } catch (SimalException e) {
            LOGGER.warn(UNAVAILABLE_MSG);
            available = false;
        }
    }

    /**
   * @throws SimalRepositoryException
   * 
   */
    private void initURL() throws SimalRepositoryException {
        this.url = SimalProperties.getProperty("simal.wookie.url", "http://localhost:8888");
    }

    /**
   * Get the URL of the wookie server.
   * 
   * @return
   * @throws SimalRepositoryException
   */
    public String getURL() {
        return this.url;
    }

    /**
   * Get the API key for this server.
   * 
   * @return
   */
    public String getApiKey() {
        return "TEST";
    }

    /**
   * Get the user identifier for this user and server.
   * 
   * @return
   */
    public String getUserID() {
        return "testuser";
    }

    /**
   * Get the shared data key for this server.
   * 
   * @return
   */
    public String getSharedDatakey() {
        return "mysharedkey";
    }

    /**
   * Find a widget based on its title and either retrieve or create
   * an instance of this widget.
   * @param widgetTitle
   * @return WidgetInstance of the widget with the title.
   */
    public WidgetInstance getInstance(String widgetTitle) {
        WidgetInstance instance = null;
        Widget widget = widgets.get(widgetTitle);
        if (widget != null) {
            try {
                instance = getOrCreateInstance(widget);
            } catch (SimalException e) {
                LOGGER.warn("Could not get or create instance for widget " + widgetTitle, e);
            }
        }
        return instance;
    }

    /**
   * Get or create an instance of a widget.
   * 
   * @param widget
   * @return the ID of the widget instance
   * @throws SimalException 
   */
    public WidgetInstance getOrCreateInstance(Widget widget) throws SimalException {
        StringBuffer data = new StringBuffer();
        try {
            data.append(URLEncoder.encode("api_key", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(getApiKey(), "UTF-8"));
            data.append("&");
            data.append(URLEncoder.encode("userid", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(getUserID(), "UTF-8"));
            data.append("&");
            data.append(URLEncoder.encode("shareddatakey", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(getSharedDatakey(), "UTF-8"));
            data.append("&");
            data.append(URLEncoder.encode("widgetid", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(widget.getIdentifier(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SimalException("UTF-8 encoding must be supported on the server", e);
        }
        URL url;
        WidgetInstance instance;
        OutputStreamWriter wr = null;
        InputStream is = null;
        try {
            url = new URL(getURL() + "/widgetinstances");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            conn.setReadTimeout(1000);
            is = conn.getInputStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            instance = widget.addInstance(db.parse(is));
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL for supplied Wookie Server is malformed", e);
        } catch (ParserConfigurationException e) {
            throw new SimalException("Unable to configure XML parser", e);
        } catch (SAXException e) {
            throw new SimalException("Problem parsing XML from Wookie Server", e);
        } catch (IOException e) {
            throw new SimalException("Problem parsing XML from Wookie Server", e);
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return instance;
    }

    /**
   * Get a set of all the available widgets in the server. If there is an error
   * communicating with the server return an empty set, or the set received so
   * far in order to allow the application to proceed. The application should
   * display an appropriate message in this case.
   * 
   * @return
   * @throws SimalException
   */
    public HashMap<String, Widget> getAvailableWidgets() throws SimalException {
        return this.widgets;
    }

    private void generateAvailableWidgets() throws SimalException {
        try {
            InputStream is = new URL(getURL() + "/widgets?all=true").openStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document widgetsDoc = db.parse(is);
            Element root = widgetsDoc.getDocumentElement();
            NodeList widgetList = root.getElementsByTagName("widget");
            for (int idx = 0; idx < widgetList.getLength(); idx = idx + 1) {
                String title, identifier, desc;
                URL icon;
                Element widgetEl = (Element) widgetList.item(idx);
                title = widgetEl.getElementsByTagName("title").item(0).getTextContent();
                if (widgets.containsKey(title)) {
                    break;
                }
                desc = widgetEl.getElementsByTagName("description").item(0).getTextContent();
                try {
                    Node iconElement = widgetEl.getElementsByTagName("icon").item(0);
                    if (iconElement != null) {
                        icon = new URL(iconElement.getTextContent());
                    } else {
                        throw new MalformedURLException(null);
                    }
                } catch (MalformedURLException e) {
                    icon = new URL("http://www.oss-watch.ac.uk/images/logo2.gif");
                }
                identifier = widgetEl.getAttribute("identifier");
                widgets.put(title, new Widget(identifier, title, desc, icon));
            }
        } catch (ParserConfigurationException e) {
            throw new SimalException("Unable to create XML parser", e);
        } catch (MalformedURLException e) {
            throw new SimalException("URL for Wookie is malformed", e);
        } catch (IOException e) {
            LOGGER.debug("Error communicating with the widget server. " + e.getMessage());
            throw new SimalException("Wookie server at '" + getURL() + "' not available.", e);
        } catch (SAXException e) {
            throw new SimalException("Unable to parse the response from Wookie", e);
        }
    }

    /**
   * @return
   */
    public boolean isAvailable() {
        return this.available;
    }
}
