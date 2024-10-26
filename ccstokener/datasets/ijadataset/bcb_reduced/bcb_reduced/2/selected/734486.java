package com.webhiker.enigma2.api;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import com.webhiker.dreambox.api.Utils;

public class Enigma2API {

    private Logger log = Logger.getAnonymousLogger();

    /** The password. */
    private String host, username, password;

    /** The port. */
    private int port;

    /**
	 * Instantiates a new dreambox api.
	 * 
	 * @param host the host
	 * @param port the port
	 * @param username the username
	 * @param password the password
	 */
    public Enigma2API(String host, int port, String username, String password) {
        log.fine("Creating API object");
        setHost(host);
        setPort(port);
        setUsername(username);
        setPassword(password);
    }

    /**
	 * Gets the host.
	 * 
	 * @return the host
	 */
    public String getHost() {
        return host;
    }

    /**
	 * Sets the host.
	 * 
	 * @param host the new host
	 */
    public void setHost(String host) {
        this.host = host;
    }

    /**
	 * Gets the username.
	 * 
	 * @return the username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Sets the username.
	 * 
	 * @param username the new username
	 */
    public void setUsername(String username) {
        this.username = username;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
            }
        });
    }

    /**
	 * Gets the password.
	 * 
	 * @return the password
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * Sets the password.
	 * 
	 * @param password the new password
	 */
    public void setPassword(String password) {
        this.password = password;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
            }
        });
    }

    /**
	 * Gets the port.
	 * 
	 * @return the port
	 */
    public int getPort() {
        return port;
    }

    /**
	 * Sets the port.
	 * 
	 * @param port the new port
	 */
    public void setPort(int port) {
        this.port = port;
    }

    /**
	 * Gets the base.
	 * 
	 * @return the base
	 */
    private String getBase() {
        return "http://" + getHost() + ":" + getPort();
    }

    /**
	 * Gets the connection.
	 * 
	 * @param location the location
	 * 
	 * @return the connection
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    private URLConnection getConnection(String location) throws IOException {
        URL url = new URL(getBase() + location);
        URLConnection uc = url.openConnection();
        uc.setConnectTimeout(3000);
        return uc;
    }

    public JSONObject getAbout() throws IOException, JSONException {
        String location = "/web/about";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return Utils.convertXML(urlConn.getInputStream());
    }

    public Signal getSignal() throws IOException, SAXException, ParserConfigurationException {
        String location = "/web/signal";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return new Signal(Utils.convertXML(urlConn.getInputStream()));
    }

    public List<ServiceObject> getBouquets() throws IOException, JSONException {
        String location = "/web/getservices";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        JSONObject jo = Utils.convertXML(urlConn.getInputStream());
        JSONArray ja = jo.getJSONObject("e2servicelist").getJSONArray("e2service");
        List<ServiceObject> result = new ArrayList<ServiceObject>();
        for (int i = 0; i < ja.length(); i++) {
            result.add(new ServiceObject(ja.getJSONObject(i)));
        }
        return result;
    }

    public List<ServiceObject> getBouquetServices(ServiceObject bouquet) throws JSONException, IOException {
        String location = "/web/getservices?sRef=" + URLEncoder.encode(bouquet.getReference(), "UTF-8");
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        JSONObject jo = Utils.convertXML(urlConn.getInputStream());
        List<ServiceObject> result = new ArrayList<ServiceObject>();
        if (jo.getJSONObject("e2servicelist").has("e2service")) {
            JSONArray ja = jo.getJSONObject("e2servicelist").getJSONArray("e2service");
            for (int i = 0; i < ja.length(); i++) {
                result.add(new ServiceObject(ja.getJSONObject(i)));
            }
        }
        return result;
    }

    public JSONObject watchChannel(JSONObject channel) throws JSONException, IOException {
        String location = "/web/zap?sRef=" + URLEncoder.encode(channel.getString("e2servicereference"), "UTF-8");
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return Utils.convertXML(urlConn.getInputStream());
    }

    public JSONObject zapTo(ServiceObject service) throws IOException {
        String location = "/web/zap?sRef=" + URLEncoder.encode(service.getReference(), "UTF-8");
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return Utils.convertXML(urlConn.getInputStream());
    }

    public ServiceInformationObject getCurrent() throws IOException, JSONException {
        String location = "/web/getcurrent";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return new ServiceInformationObject(Utils.convertXML(urlConn.getInputStream()));
    }

    public ServiceObject getSubservices() throws IOException, JSONException {
        String location = "/web/subservices";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return new ServiceObject(Utils.convertXML(urlConn.getInputStream()).getJSONObject("e2servicelist").getJSONObject("e2service"));
    }

    public List<EPGObject> getServiceEPG(ServiceObject service) throws IOException, JSONException {
        String location = "/web/epgservice?sRef=" + URLEncoder.encode(service.getReference(), "UTF-8");
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        JSONObject jo = Utils.convertXML(urlConn.getInputStream());
        List<EPGObject> result = new ArrayList<EPGObject>();
        if (jo.getJSONObject("e2eventlist").has("e2event")) {
            JSONArray ja = jo.getJSONObject("e2eventlist").getJSONArray("e2event");
            for (int i = 0; i < ja.length(); i++) {
                result.add(new EPGObject(ja.getJSONObject(i)));
            }
        }
        return result;
    }
}
