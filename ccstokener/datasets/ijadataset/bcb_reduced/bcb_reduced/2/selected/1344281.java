package org.allesta.wsabi.webservice.webserviceclient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * DOCUMENT ME!
 *
 * @author Allesta, LLC
 * @version $Revision: 1.4 $ 
 */
public class WebServiceClientServlet extends HttpServlet {

    private Log _log = LogFactory.getLog(WebServiceClientServlet.class);

    String host_port = null;

    String webapp = null;

    private HashMap _timer = new HashMap();

    /**
     * DOCUMENT ME!
     *
     * @param servletConfig DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        host_port = servletConfig.getInitParameter("hostport");
        webapp = servletConfig.getInitParameter("webapp");
        Timer timer0 = new Timer(true);
        timer0.schedule(new YahooUserPingClient(), 0, 5000);
        timer0.schedule(new YahooUserPingClient(), 0, 10000);
        timer0.schedule(new VersionClient(), 0, 5000);
        timer0.schedule(new VersionClient(), 0, 10000);
        _timer.put("4 clients", timer0);
    }

    /**
     * DOCUMENT ME!
     *
     * @param httpServletRequest DOCUMENT ME!
     * @param httpServletResponse DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String action = httpServletRequest.getParameter("action");
        _log.info("doGet(): action=" + action);
        if ("turn_all_off".equals(action)) {
            Iterator it = _timer.values().iterator();
            while (it.hasNext()) {
                Timer timer = (Timer) it.next();
                timer.cancel();
                _log.info("doGet(): web service clients have been turned off");
            }
            _timer.clear();
        } else if ("turn_all_on".equals(action)) {
            Iterator it = _timer.values().iterator();
            while (it.hasNext()) {
                ((Timer) it.next()).cancel();
            }
            _timer.clear();
            initTimers();
            _log.info("doGet(): web service clients have been turned on");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param httpServletRequest DOCUMENT ME!
     * @param httpServletResponse DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
    }

    /**
     * DOCUMENT ME!
     */
    protected void initTimers() {
        Timer timer0 = new Timer(true);
        timer0.schedule(new YahooUserPingClient(), 0, 5000);
        timer0.schedule(new YahooUserPingClient(), 0, 10000);
        timer0.schedule(new VersionClient(), 0, 5000);
        timer0.schedule(new VersionClient(), 0, 10000);
        timer0.schedule(new PerfIndexClient(), 0, 5000);
        timer0.schedule(new PerfIndexClient(), 0, 10000);
        timer0.schedule(new WSABIPerfIndexClient(), 0, 5000);
        timer0.schedule(new WSABIPerfIndexClient(), 0, 10000);
        _timer.put("yahoo_every_5_seconds", timer0);
    }

    /**
     * DOCUMENT ME!
     *
     * @author Allesta, LLC
     * @version $Revision: 1.4 $ 
     */
    public class PerfIndexClient extends TimerTask {

        /**
         * DOCUMENT ME!
         */
        public void run() {
            _log.info("Pinging PerfIndex");
            SimpleHttpClient client = new SimpleHttpClient();
            client.send("http://" + host_port + "/" + webapp + "/services/WSABIPerfIndexService?method=getPerfIndexes");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @author Allesta, LLC
     * @version $Revision: 1.4 $ 
     */
    public class SimpleHttpClient {

        /**
         * DOCUMENT ME!
         *
         * @param urlstring DOCUMENT ME!
         */
        public void send(String urlstring) {
            BufferedReader reader = null;
            try {
                URL url = new URL(urlstring);
                URLConnection connection = url.openConnection();
                connection.connect();
                reader = getReader(connection);
                String result = null;
                do {
                    result = reader.readLine();
                } while (result != null);
                reader.close();
            } catch (MalformedURLException e) {
                _log.error("send(): caught MalformedURLException: " + e.getMessage(), e);
            } catch (IOException e) {
                _log.error("send(): caught IOException: " + e.getMessage(), e);
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param connection DOCUMENT ME!
         *
         * @return DOCUMENT ME!
         *
         * @throws IOException DOCUMENT ME!
         */
        private BufferedReader getReader(URLConnection connection) throws IOException {
            InputStream is = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader breader = new BufferedReader(reader);
            return breader;
        }

        /**
         * DOCUMENT ME!
         *
         * @param reader DOCUMENT ME!
         */
        private void close(Reader reader) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    _log.warn("close(): unable to close reader");
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @author Allesta, LLC
     * @version $Revision: 1.4 $ 
     */
    public class VersionClient extends TimerTask {

        /**
         * DOCUMENT ME!
         */
        public void run() {
            _log.info("Pinging Version");
            SimpleHttpClient client = new SimpleHttpClient();
            client.send("http://" + host_port + "/" + webapp + "/services/Version?method=getVersion");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @author Allesta, LLC
     * @version $Revision: 1.4 $ 
     */
    public class YahooUserPingClient extends TimerTask {

        /**
         * DOCUMENT ME!
         */
        public void run() {
            _log.info("Pinging Yahoo");
            SimpleHttpClient client = new SimpleHttpClient();
            client.send("http://" + host_port + "/" + webapp + "/services/YahooUserPingService?method=isUserOnLine&username=tyalcinkaya");
        }
    }

    public class WSABIPerfIndexClient extends TimerTask {

        /**
         * DOCUMENT ME!
         */
        public void run() {
            _log.info("Pinging PerfIndex");
            SimpleHttpClient client = new SimpleHttpClient();
            client.send("http://" + host_port + "/" + webapp + "/services/WSABIPerfIndexService?method=getPerfIndexes");
        }
    }
}
