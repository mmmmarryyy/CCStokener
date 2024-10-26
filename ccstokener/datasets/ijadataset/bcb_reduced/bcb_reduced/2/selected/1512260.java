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
 * Command QUIT. - override the logout method.
 */
public class QuitCommand extends com.coldcore.coloradoftp.command.impl.ftp.QuitCommand {

    private static Logger log = Logger.getLogger(QuitCommand.class);

    /** Log out. Do GeoNetwork logout tasks.
   */
    protected void logout() {
        Session session = getConnection().getSession();
        session.removeAttribute("usercookie.object");
        String urlIn = GeoNetworkContext.url + "/" + GeoNetworkContext.logoutService;
        Element results = null;
        String cookie = (String) session.getAttribute("usercookie.object");
        if (cookie != null) {
            try {
                URL url = new URL(urlIn);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setRequestProperty("Cookie", cookie);
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                try {
                    results = Xml.loadStream(in);
                    log.debug("CheckLogout to GeoNetwork returned " + Xml.getString(results));
                } finally {
                    in.close();
                }
            } catch (Exception e) {
                throw new RuntimeException("User logout to GeoNetwork failed: ", e);
            }
        }
        log.debug("GeoNetwork logout done");
    }
}
