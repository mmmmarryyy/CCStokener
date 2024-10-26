package gpsxml.io;

import gpsxml.gui.ExporterAuthenticationDialog;
import gpsxml.gui.MainWindow;
import gpsxml.gui.ServerAuthenticationDialog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 *
 * @author kbt  Axiohelix Pvt. Ltd.
 */
public class XMLTransporter {

    static Logger logger = Logger.getLogger(gpsxml.io.XMLTransporter.class.getName());

    private static final String FILENAME_HEADER = "filename";

    private static final String FILETYPE_HEADER = "filetype";

    private static final String USERNAME_HEADER = "username";

    private static final String PASSWORD_HEADER = "password";

    private String _URL = "";

    private HttpURLConnection server;

    private String username = null;

    private char[] password = null;

    private String gps_username = null;

    private char[] gps_password = null;

    private MainWindow mainWindow;

    /** Creates a new instance of XMLTransporter 
     *
     */
    public XMLTransporter() {
    }

    /**
     * XMLTransporter constructor intialize the uploading and downloading process of the configuration XML 
     * for Omic Space Web Service
     *@param serverURL the full URL of the servlets in Omic Space Web Service
     *@param mainWindow a handler to the main window 
     */
    public XMLTransporter(String serverURL, MainWindow mainWindow) throws MalformedURLException, IOException {
        this.mainWindow = mainWindow;
        setServerURL(serverURL);
        init();
        checkConnection();
    }

    /**
     *init method initialize the Transporter with the URL
     *It also prompts the user for validation key if their is a secure connection to be made
     *@throws MalformedURLException,IOException
     */
    private void init() throws MalformedURLException, IOException {
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                if (username == null || password == null) {
                    ServerAuthenticationDialog dialog = new ServerAuthenticationDialog(mainWindow.getMainFrame());
                    dialog.publish();
                    username = dialog.getUserName();
                    password = dialog.getPassword();
                    if (username == null || password == null) return null;
                }
                return new PasswordAuthentication(username, password);
            }
        });
        server = (HttpURLConnection) (new URL(_URL)).openConnection();
    }

    /**
    * getter method for server URL
    *@return String server URL
    */
    public String getServerURL() {
        return _URL;
    }

    /** 
     * setter method for server URL
     *@param serverURL 
     */
    public void setServerURL(String serverURL) {
        _URL = serverURL;
    }

    /**
     * handles the Error, prints to the log
     *@param errorMessage the message to print
     *@param e the associated exception
     */
    public static void ErrorMessageHandler(String errorMessage, Exception e) {
        System.out.println(errorMessage);
        e.printStackTrace();
    }

    /**
     * validates or check the server connection is proper or not
     *@throws IOException
     */
    private void checkConnection() throws IOException {
        int i = server.getResponseCode();
        server = null;
        if (i != 200) logger.debug("Server connection response code: " + i);
        new IOException();
    }

    /**
     * downloads a server file to a local, could well be a static method
     * there is no dependency of this method on this class 
     *@param url the server file
     *@param localFile local File
     *@throws MalformedURLException,FileNotFoundException,IOException
     */
    public void downloadDirect(String url, String localFile) throws MalformedURLException, FileNotFoundException, IOException {
        FileOutputStream toFile = new FileOutputStream(localFile);
        InputStream fromServer = (new URL(url)).openConnection().getInputStream();
        try {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = fromServer.read(buffer)) != -1) {
                toFile.write(buffer, 0, bytesRead);
            }
        } finally {
            fromServer.close();
            toFile.close();
        }
    }

    /**
     * download the configuration XML file to the local directory
     * currently only used in AchiveHandler 
     * the process here is 
     *  ----------               ----------
     * | Server   | ----------> |  Local   |
     *  ----------               ----------
     *@param file server configuration XML file
     *@param type configuration XML file type
     *@param localFile the local
     */
    public void download(String file, String type, String localFile) throws MalformedURLException, FileNotFoundException, IOException {
        if (server == null) init();
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setUseCaches(false);
        server.setRequestProperty(FILENAME_HEADER, file);
        server.setRequestProperty(FILETYPE_HEADER, type);
        InputStream fromServer = server.getInputStream();
        FileOutputStream toFile = new FileOutputStream(localFile);
        try {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = fromServer.read(buffer)) != -1) {
                toFile.write(buffer, 0, bytesRead);
            }
        } finally {
            fromServer.close();
            toFile.close();
            server = null;
        }
    }

    /**
     * upload the configuration XML file to the local directory
     * currently only used in AchiveHandler 
     * this process may be critical as user will change or update the configuration files 
     * already in the server. Hence an extra validation is used for security.
     * the process here is 
     *  ----------               ----------
     * | Local   | ----------> |  Server   |
     *  ----------               ----------
     *@param localFile server configuration XML file
     *@param type configuration XML file type
     *@param serverFile the local
     */
    public boolean upload(String localFile, String type, String serverFile) throws MalformedURLException, FileNotFoundException, IOException {
        if (server == null) init();
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setUseCaches(false);
        server.setRequestProperty(FILENAME_HEADER, serverFile);
        server.setRequestProperty(FILETYPE_HEADER, type);
        server.setRequestProperty(USERNAME_HEADER, gps_username);
        server.setRequestProperty(PASSWORD_HEADER, new String(gps_password));
        OutputStream toServer = server.getOutputStream();
        FileInputStream fromFile = new FileInputStream(localFile);
        try {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = fromFile.read(buffer)) != -1) {
                toServer.write(buffer, 0, bytesRead);
            }
        } finally {
            toServer.close();
            fromFile.close();
        }
        InputStream fromServer = server.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(fromServer));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                logger.info(line);
            }
        } finally {
            br.close();
            fromServer.close();
            server = null;
        }
        return true;
    }

    /**
     * prepareUploader takes the user name and password for uploading
     * this isb required for more security
     *@return boolean true or false
     */
    public boolean prepareUploader() {
        if (gps_username == null || gps_password == null) {
            ExporterAuthenticationDialog dialog = new ExporterAuthenticationDialog(mainWindow.getMainFrame());
            dialog.publish();
            gps_username = dialog.getUserName();
            gps_password = dialog.getPassword();
            if (gps_username == null || gps_username.equals("") || gps_password == null) return false;
        }
        return true;
    }
}
