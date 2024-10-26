package net.assimilator.examples.sca.web.tomcat.balancer.utilities;

import org.apache.catalina.cluster.Member;
import org.apache.catalina.cluster.mcast.McastService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @version $Id: ServerUtil.java 140 2007-04-25 01:19:37Z khartig $
 */
public class ServerUtil {

    public static String clusterNodeHost = "127.0.0.1";

    public static int clusterNodePort = 4001;

    public static String strURL = "http://127.0.0.1:8081/clusterapp/test.jsp";

    private static Logger logger = Logger.getLogger("net.assimilator.examples.sca.web.tomcat.utilities");

    public ServerUtil() {
    }

    public boolean isServerAlive(String host, int port) {
        boolean isAlive = false;
        long t1 = System.currentTimeMillis();
        logger.info("Start Time: " + t1);
        try {
            String tcpListHost = "127.0.0.1";
            String tcpListPort = "4009";
            McastService service = new McastService();
            java.util.Properties p = new java.util.Properties();
            p.setProperty("mcastAddress", "228.0.0.4");
            p.setProperty("mcastPort", "45564");
            p.setProperty("bindAddress", "localhost");
            p.setProperty("memberDropTime", "3000");
            p.setProperty("msgFrequency", "500");
            p.setProperty("tcpListenHost", tcpListHost);
            p.setProperty("tcpListenPort", tcpListPort);
            service.setProperties(p);
            service.start();
            Thread.sleep(500);
            Member[] members = service.getMembers();
            int m = members.length;
            logger.info("# of nodes in the cluster : " + m);
            logger.info("Check the cluster nodes for [host=" + host + ",port=" + port + "]");
            for (int i = 0; i < members.length; i++) {
                logger.info("Cluster node " + (i + 1) + ": [host=" + members[i].getHost() + ",port=" + members[i].getPort() + "]");
                if (host.equals(members[i].getHost()) && port == members[i].getPort()) {
                    isAlive = true;
                    break;
                }
            }
            service.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        logger.info("Time taken to check connection: " + (t2 - t1) + " ms.");
        return isAlive;
    }

    public boolean isServerAlive(String pStrURL) {
        boolean isAlive;
        long t1 = System.currentTimeMillis();
        try {
            URL url = new URL(pStrURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.fine(inputLine);
            }
            logger.info("**  Connection successful..  **");
            in.close();
            isAlive = true;
        } catch (Exception e) {
            logger.info("**  Connection failed..  **");
            e.printStackTrace();
            isAlive = false;
        }
        long t2 = System.currentTimeMillis();
        logger.info("Time taken to check connection: " + (t2 - t1) + " ms.");
        return isAlive;
    }

    public static void main(String[] args) throws Exception {
        ServerUtil test = new ServerUtil();
        if (test.isServerAlive(clusterNodeHost, clusterNodePort)) {
            logger.info("**  IT'S ALIVE  **");
        } else {
            logger.info("**  THE SERVER IS NOT AVAILABLE  **");
        }
        if (test.isServerAlive(strURL)) {
            logger.info("**  IT'S ALIVE  **");
        } else {
            logger.info("**  THE SERVER IS NOT AVAILABLE  **");
        }
    }
}
