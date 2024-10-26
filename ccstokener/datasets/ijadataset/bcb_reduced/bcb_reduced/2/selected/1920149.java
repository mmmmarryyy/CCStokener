package org.connect2truth.http.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.connect2truth.config.ApplicationConstants;
import org.connect2truth.config.ConfigManager;
import org.connect2truth.tools.upnp.LocalIpHelper;

public class ServerIP {

    private Log logger = LogFactory.getLog(getClass());

    private String publicIP = null;

    private String localHostIP = null;

    public String getLocalHostIP() {
        if (localHostIP == null) {
            localHostIP = LocalIpHelper.getLocalFirstNoneLoopIPAddress();
            logger.debug("local ip address:" + localHostIP);
        }
        return localHostIP;
    }

    public void setIPAfterDetectPublicIPchange(String ip) {
        this.publicIP = ip;
        updatePublicIpInPasswordsTxt();
    }

    public String getPublicIP() {
        return publicIP;
    }

    public void setPublicIP(String ip) {
        if (publicIP == null) {
            publicIP = ip;
            logger.info("Server public ip: " + publicIP);
            updatePublicIpInPasswordsTxt();
        } else {
            logger.debug("Server IP already set!");
        }
    }

    private void updatePublicIpInPasswordsTxt() {
        Configuration passwords = ConfigManager.getInstance().getConfig(ConfigManager.PASSWORDS);
        if (passwords != null) {
            passwords.clearProperty(ApplicationConstants.CURRENT_PUBLIC_IP);
            passwords.addProperty(ApplicationConstants.CURRENT_PUBLIC_IP, this.publicIP);
        }
    }

    /**
	 * 
	 * @return null if not successfully connect to checkip.dyndns.org
	 */
    public static String checkPublicIP() {
        String ipAddress = null;
        try {
            URL url;
            url = new URL("http://checkip.dyndns.org/");
            InputStreamReader in = new InputStreamReader(url.openStream());
            BufferedReader buffer = new BufferedReader(in);
            String line;
            Pattern p = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
            while ((line = buffer.readLine()) != null) {
                if (line.indexOf("IP Address:") != -1) {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        ipAddress = m.group();
                        break;
                    }
                }
            }
            buffer.close();
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ipAddress;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println(checkPublicIP());
    }
}
