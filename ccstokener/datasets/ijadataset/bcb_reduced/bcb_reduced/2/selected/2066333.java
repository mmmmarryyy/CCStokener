package it.secondlifelab.p2pSL.jserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author hachreak
 * 
 * Interface with php sync server
 */
public class P2PSLphpInterface {

    public static final String server = "http://127.0.0.1";

    public static final String myip = "myip.php";

    public static final String getKey = "database.php";

    private Debug debug;

    private P2PSLconfig config;

    private String last_ip;

    public static final String NONCE = ":0";

    private int timeout = 5;

    private int count = 5;

    public P2PSLphpInterface(Debug debug, P2PSLconfig config) {
        this.debug = debug;
        this.config = config;
        this.config = config;
    }

    /**
	 * Return the ip of the client where application run
	 * 
	 * @return
	 * @throws IOException
	 */
    public String getMyIp() {
        String ip = "";
        if (count <= 0) count = timeout;
        if (count == timeout) {
            URL url;
            try {
                url = new URL(config.getServerPhp() + "/" + myip);
                BufferedReader data = new BufferedReader(new InputStreamReader(url.openStream()));
                last_ip = ip = data.readLine();
            } catch (IOException e) {
                last_ip = "127.0.0.1";
                e.printStackTrace();
            }
        } else {
            ip = last_ip;
        }
        count--;
        debug.print("My IP: " + ip);
        return ip;
    }

    /**
	 * Get the key of object client on SecondLife
	 * 
	 * @param md5
	 *            md5 of user+password
	 * @return The key of object client on SecondLife
	 * @throws IOException
	 */
    public String getKey(String md5) throws IOException {
        URL url = new URL(config.getServerPhp() + "/" + getKey + "?op=read&name=key&md5=" + md5);
        String key = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
        debug.print("SL Object key: " + key);
        return key;
    }

    /**
	 * Set the ip on php database
	 * 
	 * @param md5
	 *            md5 of user+password
	 * @return a value from php db
	 * @throws IOException
	 */
    public String setIp(String md5) {
        String ret = null;
        try {
            URL url = new URL(config.getServerPhp() + "/" + getKey + "?op=write&md5=" + md5 + "&ip=" + this.getMyIp() + ":" + config.getPort());
            ret = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
            debug.print("[ OK ] Sign on php server " + config.getServerPhp());
        } catch (IOException e) {
            debug.print("[ ERROR ] Sign on php server " + config.getServerPhp());
        }
        return ret;
    }
}
