package net.jetrix.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * A utility class that fetches a list of tetrinet servers from online
 * directories like tetrinet.org and tfast.org.
 *
 * @since 0.2
 *
 * @author Emmanuel Bourg
 * @version $Revision: 589 $, $Date: 2005-01-12 14:05:07 -0500 (Wed, 12 Jan 2005) $
 */
public class ServerDirectory {

    /**
     * Return the list of registered servers.
     */
    public static List<String> getServers() throws Exception {
        List<String> servers = new ArrayList<String>();
        URL url = new URL("http://tfast.org/en/servers.php");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains("serv=")) {
                int i = line.indexOf("serv=");
                servers.add(line.substring(i + 5, line.indexOf("\"", i)));
            }
        }
        in.close();
        return servers;
    }
}
