package org.jives.implementors.network.jxse.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.jives.implementors.network.jxse.JXSEImplementor;
import org.jives.implementors.network.jxse.rendezvous.RendezvousRelayList;
import org.jives.utils.IOManager;
import org.jives.utils.Log;

/**
 * Class through which you can manage the file with the list of
 * rendezvous/relays
 * 
 * @author simonesegalini
 */
public class FileManager {

    private RendezvousRelayList rendezvousList;

    private URL url;

    private List<Object[]> shuffledRendezvousList;

    private URLConnection urlConn;

    private BufferedReader br;

    private final String charset = "UTF-8";

    private String remote_url;

    private String local_url;

    private final String remotePath = "apps/rendezvous_";

    private final String localPath = "config/rendezvous_";

    /**
	 * FileManager singleton instance
	 */
    private static FileManager instance;

    protected FileManager() {
        shuffledRendezvousList = new ArrayList<Object[]>();
    }

    /**
	 * @return FileManager singleton instance
	 */
    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    /**
	 * Method used to open the connection to the server in order to query the
	 * rendezvous file
	 * 
	 * @param query
	 *            the query to be passed to the server
	 * 
	 * @param url
	 *            the url to which we have to connect
	 */
    private void openConnection(String query, String url) {
        try {
            urlConn = new URL(url + "?" + query).openConnection();
            urlConn.setRequestProperty("Accept-Charset", charset);
            InputStream response = urlConn.getInputStream();
            br = new BufferedReader(new InputStreamReader(response));
        } catch (MalformedURLException e) {
            Log.error(this, e.getMessage());
        } catch (IOException e) {
            Log.error(this, e.getMessage());
        }
    }

    /**
	 * Add a rendezvous/relay to the file on the server
	 * 
	 * @param rendezvousIPv4
	 *            the IPv4 of the rendezvous to be registered
	 * 
	 * @param rendezvousIPv6
	 *            the IPv6 of the rendezvous to be registered
	 * 
	 * @param rendezvousPID
	 *            the PID of the rendezvous to be registered
	 * 
	 * @param rendezvousProgram
	 *            the program's name of the rendezvous to be registered
	 * 
	 * @param rendezvousMD5
	 *            the MD5 string corresponding to the Jives program to which the
	 *            rendezvous to be registered belongs
	 */
    public void addToFile(String rendezvousIPv4, String rendezvousIPv6, int rendezvousPort, String rendezvousPID, String rendezvousProgram, String rendezvousMD5) {
        try {
            String line = null;
            do {
                XMLConfigParser.readUrlHost();
                String url = XMLConfigParser.urlHost;
                Date currenttime = new Date();
                String query;
                String param1 = "op=add";
                String param2 = "addr=" + rendezvousIPv4;
                String param3 = "addr2=" + rendezvousIPv6;
                String param4 = "port=" + rendezvousPort;
                String param5 = "pid=" + rendezvousPID;
                String param6 = "program=" + rendezvousProgram;
                String param7 = "md5=" + rendezvousMD5;
                String param8 = "time=" + currenttime.getTime();
                String param9 = "token=" + getToken(rendezvousPID, rendezvousProgram);
                query = String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s", param1, param2, param3, param4, param5, param6, param7, param8, param9);
                openConnection(query, url);
                line = br.readLine();
                NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(IPv6 check) Response of the server: " + line);
                line = br.readLine();
                NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(Adding check) Response of the server: " + line);
            } while (line.compareTo("Adding completed!") != 0);
            NetworkLog.logMsg(NetworkLog.LOG_INFO, this, "Adding completed successfully");
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
    }

    /**
	 * Downloads locally a list of rendezvous of a specific Jives program
	 * 
	 * @param connect
	 *            the connection to the file previously established
	 * 
	 * @param program
	 *            the Jives program's name of which a list of rendezvous/relays
	 *            is downloaded in a local file
	 */
    public void downloader(URLConnection connect, String program) {
        if (XMLConfigParser.getInternetChoice() && !XMLConfigParser.getLanChoice()) {
            try {
                connect.setDoInput(true);
                connect.setDoOutput(true);
                connect.setUseCaches(false);
                InputStream stream = connect.getInputStream();
                BufferedInputStream in = new BufferedInputStream(stream);
                String local_url = IOManager.resolveFile(JXSEImplementor.class, this.local_url).getAbsolutePath();
                FileOutputStream file = new FileOutputStream(local_url);
                BufferedOutputStream out = new BufferedOutputStream(file);
                int i;
                while ((i = in.read()) != -1) {
                    out.write(i);
                }
                out.flush();
                out.close();
                file.close();
                in.close();
                stream.close();
            } catch (MalformedURLException e) {
                Log.error(this, e.getMessage());
            } catch (ProtocolException e) {
                Log.error(this, e.getMessage());
            } catch (IOException e) {
                Log.error(this, e.getMessage());
            }
        }
    }

    /**
	 * Starts a free proxy connection for a Peer 
	 * 
	 * @param scriptName
	 *            the name of the running Jives script
	 * 
	 * @param scriptMD5
	 *            the md5 string corresponding to the running Jives script
	 * 
	 * @return a list of rendezvous shuffled
	 */
    public List<Object[]> freeProxyConnection(String scriptName, String scriptMD5) {
        NetworkLog.logMsg(NetworkLog.LOG_INFO, this, "Starting a free proxy connection");
        rendezvousList = new RendezvousRelayList();
        XMLConfigParser.readUrlHost();
        remote_url = XMLConfigParser.urlHost + remotePath + scriptName + ".xml";
        local_url = localPath + scriptName + ".xml";
        if (!XMLConfigParser.getLanChoice() && XMLConfigParser.getInternetChoice()) {
            try {
                queryInactive();
                queryFile(scriptName, scriptMD5);
                url = new URL(remote_url);
                URLConnection urlConn;
                urlConn = url.openConnection();
                downloader(urlConn, scriptName);
                rendezvousList = XMLConfigParser.readRendezvouslist(local_url, rendezvousList);
            } catch (IOException e) {
                NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
            }
        }
        shuffledRendezvousList = rendezvousList.shuffle();
        return shuffledRendezvousList;
    }

    /**
	 * Get the account secret key from the server in order to encrypt/decrypt the local data storages
	 * 
	 * @param id
	 * 						the peer's name
	 * 
	 * @return the secret key of this account
	 * 
	 * @throws IOException If an error occurred while trying to resolve the account secret key 
	 * 
	 */
    public String getAccountSecretKey(String id) throws IOException {
        String secret = null;
        XMLConfigParser.readUrlSecretKey();
        XMLConfigParser.readHttpsCredentials();
        String url = XMLConfigParser.urlSecretKey;
        if (!XMLConfigParser.httpsUsername.isEmpty() && !XMLConfigParser.httpsPassword.isEmpty()) {
            url = "https://" + XMLConfigParser.httpsUsername + ":" + XMLConfigParser.httpsPassword + "@" + url.substring(7);
        }
        String query;
        String param1 = "id=" + id;
        String param2 = "pid=" + 0;
        query = String.format("%s&%s", param1, param2);
        openConnection(query, url);
        if (br != null) {
            secret = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "Account secret key successfully acquired.");
            br.close();
        }
        return secret;
    }

    /**
	 * Get the token md5 from the server
	 * 
	 * @param rendezvousPID
	 *            the PID of the rendezvous
	 * 
	 * @param rendezvousProgram
	 *            the program's name of the rendezvous
	 * 
	 */
    public String getToken(String rendezvousPID, String rendezvousProgram) {
        String token = null;
        try {
            XMLConfigParser.readUrlHost();
            String url = XMLConfigParser.urlHost;
            String query;
            String param1 = "op=gettoken";
            String param2 = "pid=" + rendezvousPID;
            String param3 = "program=" + rendezvousProgram;
            query = String.format("%s&%s&%s", param1, param2, param3);
            openConnection(query, url);
            token = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "Token successfully acquired.");
            br.close();
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
        return token;
    }

    /**
	 * @return The rendezvous list
	 */
    public List<Object[]> getShuffledRendezvousList() {
        return shuffledRendezvousList;
    }

    /**
	 * Update the rendezvous list
	 * 
	 * @param rendezvousList The new rendezvous list
	 */
    public void setRendezvousList(RendezvousRelayList rendezvousList) {
        this.rendezvousList = rendezvousList;
        shuffledRendezvousList = this.rendezvousList.shuffle();
    }

    /**
	 * Starts a proxy connection
	 * 
	 * @param scriptName
	 *            the name of the running Jives script
	 * 
	 * @param scriptMD5
	 *            the md5 string corresponding to the running Jives script
	 * 
	 * @return a list of rendezvous shuffled
	 */
    public List<Object[]> proxyConnection(String scriptName, String scriptMD5) {
        NetworkLog.logMsg(NetworkLog.LOG_INFO, this, "Starting a proxy connection");
        rendezvousList = new RendezvousRelayList();
        XMLConfigParser.readUrlHost();
        remote_url = XMLConfigParser.urlHost + remotePath + scriptName + ".xml";
        local_url = localPath + scriptName + ".xml";
        try {
            if (XMLConfigParser.getInternetChoice() && !XMLConfigParser.getLanChoice()) {
                XMLConfigParser.readProxyConfiguration();
                System.getProperties().put("proxySet", "true");
                System.getProperties().put("proxyHost", XMLConfigParser.proxyHost);
                System.getProperties().put("proxyPort", XMLConfigParser.proxyPort);
                queryInactive();
                queryFile(scriptName, scriptMD5);
                URL url = new URL(remote_url);
                URLConnection urlConn = url.openConnection();
                String password = XMLConfigParser.proxyUsername + ":" + XMLConfigParser.proxyPassword;
                String encoded = Base64.encodeBase64String(password.getBytes());
                urlConn.setRequestProperty("Proxy-Authorization", encoded);
                downloader(urlConn, scriptName);
                rendezvousList = XMLConfigParser.readRendezvouslist(local_url, rendezvousList);
            }
        } catch (Exception e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
        shuffledRendezvousList = rendezvousList.shuffle();
        return shuffledRendezvousList;
    }

    /**
	 * Remove a rendezvous/relay from the file on the server
	 * 
	 * @param rendezvousIPv4
	 *            the IPv4 of the rendezvous to be removed
	 * 
	 * @param rendezvousIPv6
	 *            the IPv6 of the rendezvous to be removed
	 * 
	 * @param rendezvousPID
	 *            the PID of the rendezvous to be removed
	 * 
	 * @param rendezvousProgram
	 *            the program's name of the rendezvous to be removed
	 * 
	 * @param rendezvousMD5
	 *            the MD5 string corresponding to the Jives program to which the
	 *            rendezvous to be removed belongs
	 */
    public void removefromFile(String rendezvousIPv4, String rendezvousIPv6, int rendezvousPort, String rendezvousPID, String rendezvousProgram, String rendezvousMD5) {
        String line;
        try {
            XMLConfigParser.readUrlHost();
            String url = XMLConfigParser.urlHost;
            String query;
            String param1 = "op=remove";
            String param2 = "addr=" + rendezvousIPv4;
            String param3 = "addr2=" + rendezvousIPv6;
            String param4 = "port=" + rendezvousPort;
            String param5 = "pid=" + rendezvousPID;
            String param6 = "program=" + rendezvousProgram;
            String param7 = "md5=" + rendezvousMD5;
            String param8 = "token=" + getToken(rendezvousPID, rendezvousProgram);
            query = String.format("%s&%s&%s&%s&%s&%s&%s&%s", param1, param2, param3, param4, param5, param6, param7, param8);
            openConnection(query, url);
            line = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(IPv6 check) Response of the server: " + line);
            line = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(Removing check) Response of the server: " + line);
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
    }

    /**
	 * Update the timestamp of a rendezvous/relay belonging to the list on the
	 * server
	 * 
	 * @param rendezvousIPv4
	 *            the IPv4 of the rendezvous to be updated
	 * 
	 * @param rendezvousIPv6
	 *            the IPv6 of the rendezvous to be updated
	 * 
	 * @param rendezvousPort
	 *            the TCP port of the rendezvous to be updated
	 * 
	 * @param rendezvousPID
	 *            the PID of the rendezvous to be updated
	 * 
	 * @param rendezvousProgram
	 *            the program's name of the rendezvous to be updated
	 * 
	 * @param rendezvousMD5
	 *            the MD5 string corresponding to the Jives program to which the
	 *            rendezvous to be updated belongs
	 */
    public void updateTime(String rendezvousIPv4, String rendezvousIPv6, int rendezvousPort, String rendezvousPID, String rendezvousProgram, String rendezvousMD5) {
        String line;
        try {
            XMLConfigParser.readUrlHost();
            String url = XMLConfigParser.urlHost;
            Date currenttime = new Date();
            String query;
            String param1 = "op=updatetime";
            String param2 = "addr=" + rendezvousIPv4;
            String param3 = "addr2=" + rendezvousIPv6;
            String param4 = "port=" + rendezvousPort;
            String param5 = "pid=" + rendezvousPID;
            String param6 = "program=" + rendezvousProgram;
            String param7 = "md5=" + rendezvousMD5;
            String param8 = "time=" + currenttime.getTime();
            String param9 = "token=" + getToken(rendezvousPID, rendezvousProgram);
            query = String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s", param1, param2, param3, param4, param5, param6, param7, param8, param9);
            openConnection(query, url);
            line = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_INFO, this, " (Update check) Response of the server: " + line);
            br.close();
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
    }

    /**
	 * Queries the file on the server by Program's name and MD5, in order to
	 * file a list of corresponding rendezvous
	 * 
	 * @param rendezvousProgram
	 *            the program's name of the rendezvous to be queried
	 * 
	 * @param rendezvousMD5
	 *            the MD5 string corresponding to the Jives program to which the
	 *            rendezvous to be queried belong
	 */
    void queryFile(String rendezvousProgram, String rendezvousMD5) {
        try {
            XMLConfigParser.readUrlHost();
            String url = XMLConfigParser.urlHost;
            String query;
            String param1 = "op=query";
            String param2 = "program=" + rendezvousProgram;
            String param3 = "md5=" + rendezvousMD5;
            query = String.format("%s&%s&%s", param1, param2, param3);
            openConnection(query, url);
            String line = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(Query) Response of the server: " + line);
            br.close();
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
    }

    /**
	 * Queries the file on the server by timestamp, in order to identify the
	 * inactive ones
	 */
    void queryInactive() {
        try {
            XMLConfigParser.readUrlHost();
            String url = XMLConfigParser.urlHost;
            Date currenttime = new Date();
            String query;
            String param1 = "op=queryinactive";
            String param2 = "time=" + currenttime.getTime();
            query = String.format("%s&%s", param1, param2);
            openConnection(query, url);
            String line = br.readLine();
            NetworkLog.logMsg(NetworkLog.LOG_DEBUG, this, "(Query inactive)Response of the server: " + line);
            br.close();
        } catch (MalformedURLException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, "Error" + e);
        } catch (IOException e) {
            NetworkLog.logMsg(NetworkLog.LOG_ERROR, this, e.getMessage());
        }
    }
}
