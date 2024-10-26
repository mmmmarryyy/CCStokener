package net.sf.gham.core.control.download.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.gham.core.control.download.InvalidUserException;
import net.sf.gham.swing.util.Config;
import net.sf.gham.swing.util.XMLUtils;
import net.sf.jtwa.Messages;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class HatConnection implements IHatConnection {

    private final String userAgent;

    private String hattrickServerURL = null;

    private String sessionCookie = null;

    public HatConnection() throws IOException, JDOMException {
        userAgent = "gham " + Config.singleton().getVersionText();
    }

    /**Connects to hattrick server, receiving the best server currently available
     *
     * @throws IOException
     * @throws JDOMException
     */
    private void connection() throws IOException, JDOMException {
        URL login = new URL("http://www.hattrick.org/Community/CHPP/System/chppxml.axd?file=servers");
        HttpURLConnection huc = (HttpURLConnection) login.openConnection();
        huc.setRequestProperty("User-Agent", userAgent);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(huc.getInputStream());
        hattrickServerURL = doc.getRootElement().getChildText("RecommendedURL");
        Logger.getRootLogger().debug("hattrickServerURL " + hattrickServerURL);
    }

    /**Login with the provided username & password
     *
     * @param username String
     * @param password String
     * @throws IOException
     * @throws MalformedURLException
     * @throws JDOMException
     * @throws InvalidUserException
     */
    public void login(String username, String password) throws IOException, MalformedURLException, JDOMException, InvalidUserException {
        connection();
        URL login = new URL(hattrickServerURL + "/chppxml.axd?file=login&actionType=login&loginName=" + username + "&chppID=1223&chppKey=1924DC67-87DA-40A8-AEEF-D6BCDDA850FD&readonlypassword=" + password);
        Logger.getRootLogger().debug("Login to " + login);
        HttpURLConnection huc = (HttpURLConnection) login.openConnection();
        huc.setRequestProperty("User-Agent", userAgent);
        sessionCookie = "";
        String headerName = null;
        for (int i = 1; (headerName = huc.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                String tempCookie = huc.getHeaderField(i);
                tempCookie = tempCookie.substring(0, tempCookie.indexOf(";"));
                String cookieName = tempCookie.substring(0, tempCookie.indexOf("="));
                String cookieValue = tempCookie.substring(tempCookie.indexOf("=") + 1, tempCookie.length());
                sessionCookie += cookieName + "=" + cookieValue + ";";
            }
        }
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(huc.getInputStream());
        if (!doc.getRootElement().getChildText("LoginResult").equalsIgnoreCase("0")) {
            throw new InvalidUserException();
        }
    }

    /**Logout
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public void logout() throws IOException, MalformedURLException {
        URL logout = new URL(hattrickServerURL + "/chppxml.axd?file=login&actionType=logout");
        HttpURLConnection huc = (HttpURLConnection) logout.openConnection();
        huc.setRequestProperty("User-Agent", userAgent);
        BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream()));
        while (br.readLine() != null) {
        }
        br.close();
    }

    /**Opens a stream to fetch the XML file specified in the String parameter
     *
     * @param page String
     * @param params String
     * @throws IOException
     * @throws MalformedURLException
     * @return InputStream
     */
    public InputStream getPage(String page) throws IOException {
        URL url = new URL(hattrickServerURL + "/Common/" + page);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestProperty("Cookie", sessionCookie);
        return huc.getInputStream();
    }

    public void downloadPage(String page, String fileName) throws IOException {
        int task = 0;
        int maxTask = 5;
        while (task < maxTask) {
            InputStream is = getPage(page);
            XMLUtils.writeToXML(is, fileName);
            File f = new File(fileName);
            try {
                XMLUtils.createElement(f);
                return;
            } catch (IOException e) {
            }
            task++;
        }
        throw new IOException(Messages.getString("Error_while_trying_to_download_file") + " " + page);
    }

    public static void main(String[] args) throws MalformedURLException, IOException, JDOMException, InvalidUserException {
    }
}
