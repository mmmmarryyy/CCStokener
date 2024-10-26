package uk.org.ogsadai.astro.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Methods for deploying a TAP webapp and initialising the job database. 
 * 
 * @author Amy Krause, EPCC, The University of Edinburgh
 */
public class DeployTAP {

    /**
     * Deploys a new web application to Tomcat.
     * 
     * @param server
     *            URL of the Tomcat server
     * @param webappPath
     *            path name of the new webapp
     * @param contextFile
     *            path of the context file
     * @param warFile
     *            path of the WAR file
     * @param username
     *            Tomcat manager username
     * @param password
     *            Tomcat manager password
     * @throws IOException
     */
    public static void deploy(String server, String webappPath, String contextFile, String warFile, final String username, final String password) throws IOException {
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
        URL url = new URL(server + "/manager/deploy?path=" + webappPath + "&config=" + contextFile);
        InputStream in = url.openStream();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        while (read >= 0) {
            read = in.read(buffer);
        }
        in.close();
        System.out.println("Successfully deployed webapp " + webappPath);
    }

    /**
     * Initialises the job database of a TAP server.
     * 
     * @param webappURL
     *            URL of the TAP webapp
     * @param username
     *            username of the TAP manager
     * @param password
     *            password of the TAP manager
     * @throws IOException
     */
    public static void initialiseJobDatabase(String webappURL, final String username, final String password) throws IOException {
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
        URL url = new URL(webappURL + "/admin/jobs");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        String content = "ACTION=" + URLEncoder.encode("INITIALIZE", "UTF-8");
        out.writeBytes(content);
        out.flush();
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
        }
        in.close();
    }
}
