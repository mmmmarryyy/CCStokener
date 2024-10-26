package org.form4G.net.microServlet;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author efrigerio
 *
 */
public class MicroServletHandler extends URLStreamHandler {

    private static Logger log = Logger.getLogger(MicroServletHandler.class.getName());

    private static final WeakHashMap<String, MicroServlet> dictionaryServlet = new WeakHashMap<String, MicroServlet>();

    private static Hashtable<String, MSSession> hashSession = new Hashtable<String, MSSession>();

    private static int maxInactiveInterval = -1;

    protected URLConnection openConnection(URL url) throws IOException {
        log.log(Level.FINE, url.toString());
        MSServletRequest urlManager = new MSServletRequest(url);
        MicroServlet servlet = getServlet(urlManager);
        return (new MSConnection(url, servlet, urlManager));
    }

    private static MicroServlet getServlet(MSServletRequest urlManager) throws IOException {
        MicroServlet rr = null;
        if (!dictionaryServlet.containsKey(urlManager.getServletName())) {
            try {
                rr = (MicroServlet) Class.forName(urlManager.getServletName()).newInstance();
                if (!urlManager.getServletName().equals(rr.getClass().getName())) throw new IOException("Exception in new Instance of " + rr.getClass().getName());
                dictionaryServlet.put(urlManager.getServletName(), rr);
            } catch (Exception ext) {
                log.log(Level.SEVERE, "in new instance " + urlManager.getServletName() + ": " + ext.getMessage(), ext);
                throw new IOException(ext.getClass().getName() + "\n" + ext.getMessage());
            }
        } else rr = dictionaryServlet.get(urlManager.getServletName());
        log.log(Level.FINE, rr.getClass().getName());
        return rr;
    }

    public static void stopSession(MSSession session) {
        hashSession.remove(session.getId());
        log.log(Level.INFO, "remove sessionId = " + session.getId());
    }

    public static int totalOfSessions() {
        return hashSession.size();
    }

    public static MSSession getSession(String sessionId) {
        MSSession session = hashSession.get(sessionId);
        if (session == null) {
            session = new MSSession(sessionId);
            session.setMaxInactiveInterval(maxInactiveInterval);
            hashSession.put(sessionId, session);
            log.log(Level.INFO, "create sessionId = " + sessionId);
        } else {
            session.setLastAccessedTime(System.currentTimeMillis());
        }
        return session;
    }

    public static MSSession getSession(MSSession session) {
        if (!hashSession.containsKey(session.getId())) {
            hashSession.put(session.getId(), session);
            log.log(Level.INFO, "restore sessionId = " + session.getId());
        }
        return session;
    }
}
