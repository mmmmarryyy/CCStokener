package au.csiro.coloradoftp.plugin.geonetwork.command;

import au.csiro.coloradoftp.plugin.geonetwork.GeoNetworkContext;
import com.coldcore.coloradoftp.session.Session;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jeeves.utils.Xml;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * PASS command. Retrieves a cookie to use in communication with GeoNetwork.
 */
public class PassCommand extends com.coldcore.coloradoftp.command.impl.ftp.PassCommand {

    private static Logger log = Logger.getLogger(PassCommand.class);

    protected boolean checkLogin(String username, String password) {
        log.debug("Called checkLogin with " + username);
        String urlIn = GeoNetworkContext.url + "/" + GeoNetworkContext.loginService + "?username=" + username + "&password=" + password;
        Element results = null;
        String cookieValue = null;
        try {
            URL url = new URL(urlIn);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            try {
                results = Xml.loadStream(in);
                log.debug("CheckLogin to GeoNetwork returned " + Xml.getString(results));
            } finally {
                in.close();
            }
            Map<String, List<String>> headers = conn.getHeaderFields();
            List<String> values = headers.get("Set-Cookie");
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                String v = (String) iter.next();
                if (cookieValue == null) {
                    cookieValue = v;
                } else {
                    cookieValue = cookieValue + ";" + v;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("User login to GeoNetwork failed: ", e);
        }
        if (!results.getName().equals("ok")) return false;
        Session session = getConnection().getSession();
        session.removeAttribute("usercookie.object");
        session.setAttribute("usercookie.object", cookieValue);
        log.debug("Cookie set is " + cookieValue);
        return true;
    }
}
