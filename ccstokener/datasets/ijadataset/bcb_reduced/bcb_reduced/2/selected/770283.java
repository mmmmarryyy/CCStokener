package jeeves.xlink;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import jeeves.JeevesJCS;
import jeeves.exceptions.JeevesException;
import jeeves.utils.Xml;
import org.apache.jcs.access.exception.CacheException;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Process XML document having XLinks to resolve, remove and detach fragments.
 *
 * TODO : Define when to empty the cache ? and how to clean all or only one
 * fragments in the cache ?
 *
 * @author pvalsecchi
 * @author fxprunayre
 */
public class Processor {

    private static class Failure implements Comparable<Failure> {

        public final String uri;

        public final Throwable t;

        public final long timeOfFailure;

        Failure(String uri, Throwable t, long timeOfFailure) {
            this.uri = uri;
            this.t = t;
            this.timeOfFailure = timeOfFailure;
        }

        @Override
        public int compareTo(Failure o) {
            long diff = timeOfFailure - o.timeOfFailure;
            if (diff == 0) {
                return 0;
            } else {
                return diff > 0 ? 1 : -1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Failure failure = (Failure) o;
            if (timeOfFailure != failure.timeOfFailure) return false;
            if (uri != null ? !uri.equals(failure.uri) : failure.uri != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = uri != null ? uri.hashCode() : 0;
            result = 31 * result + (int) (timeOfFailure ^ (timeOfFailure >>> 32));
            return result;
        }
    }

    private static TreeSet<Failure> failures = new TreeSet<Failure>();

    private static int MAX_FAILURES = 50;

    private static final long ELAPSE_TIME = 30000;

    /**
     * Action to specify to remove all children off elements having an XLink.
     */
    private static final String ACTION_REMOVE = "remove";

    /**
     * Action to specify to resolve all XLinks.
     */
    private static final String ACTION_RESOLVE = "resolve";

    /**
     * Action to specify to resolve and remove all XLinks.
     */
    private static final String ACTION_DETACH = "detach";

    private static final Logger LOGGER = Logger.getLogger(Processor.class);

    private static final String XLINK_JCS = "xlink";

    /**
     * Process all XLinks of the input XML document.
     * <p/>
     * TODO : It could be fine to have cache mechanism for XLink fragment as
     * they are by definition redundant. Could improve performance.
     */
    public static Element processXLink(Element xml) {
        searchXLink(xml, ACTION_RESOLVE);
        return xml;
    }

    /**
     * Remove all XLinks child of the input XML document.
     */
    public static Element removeXLink(Element xml) {
        searchXLink(xml, ACTION_REMOVE);
        return xml;
    }

    /**
     * Remove all XLinks child of the input XML document.
     */
    public static Element detachXLink(Element xml) {
        searchXLink(xml, ACTION_DETACH);
        return xml;
    }

    /**
     * Recursively navigate in XML children and search for XLink. Load and cache
     * remote resource if needed.
     * <p/>
     * TODO : Maybe don't wait to much to load a remote resource. Add timeOfFailure
     * param?
     *
     * @param action
     *            Define what to do with XLink ({@link #ACTION_DETACH,
     *            #ACTION_REMOVE, #ACTION_RESOLVE}).
     *
     */
    private static void searchXLink(Element element, String action) {
        String hrefUri = element.getAttributeValue(XLink.HREF, XLink.NAMESPACE_XLINK);
        if (hrefUri != null && !hrefUri.equals("")) {
            String show = element.getAttributeValue(XLink.SHOW, XLink.NAMESPACE_XLINK);
            if (show == null || show.equals("") || show.equals(XLink.SHOW_EMBED)) {
                if (action.equals(ACTION_RESOLVE) || action.equals(ACTION_DETACH)) {
                    try {
                        Content remoteFragment = resolveXLink(hrefUri);
                        element.removeContent();
                        element.addContent(remoteFragment);
                    } catch (Exception e) {
                        String error = Xml.getString(JeevesException.toElement(e));
                        jeeves.utils.Log.error("jeeves.xlink.Processor", "#searchXLink -- Error embedding element with " + hrefUri + ":\n" + error);
                    }
                    cleanXLinkAttributes(element, action);
                } else if (action.equals(ACTION_REMOVE)) {
                    element.removeContent();
                }
            }
        }
        List children = element.getChildren();
        List<Element> replaceLinks = new ArrayList<Element>();
        for (Object aChild : children) {
            Element child = (Element) aChild;
            hrefUri = child.getAttributeValue(XLink.HREF, XLink.NAMESPACE_XLINK);
            String show = child.getAttributeValue(XLink.SHOW, XLink.NAMESPACE_XLINK);
            boolean isReplace = hrefUri != null && show != null && show.equalsIgnoreCase(XLink.SHOW_REPLACE);
            if (isReplace) {
                replaceLinks.add(child);
            }
        }
        for (Element toReplace : replaceLinks) {
            try {
                hrefUri = toReplace.getAttributeValue(XLink.HREF, XLink.NAMESPACE_XLINK);
                Element remoteFragment = resolveXLink(hrefUri);
                toReplace.removeContent();
                if (!action.equals(ACTION_DETACH)) {
                    remoteFragment.setAttribute((Attribute) toReplace.getAttribute(XLink.HREF, XLink.NAMESPACE_XLINK).clone());
                    remoteFragment.setAttribute((Attribute) toReplace.getAttribute(XLink.SHOW, XLink.NAMESPACE_XLINK).clone());
                    remoteFragment.setAttribute((Attribute) toReplace.getAttribute(XLink.ROLE, XLink.NAMESPACE_XLINK).clone());
                }
                element.setContent(element.indexOf(toReplace), remoteFragment);
            } catch (Exception e) {
                String error = Xml.getString(JeevesException.toElement(e));
                jeeves.utils.Log.error("jeeves.xlink.Processor", "#searchXLink --  Error replacing element with hrefUri:\n" + error);
            }
        }
        children = element.getChildren();
        for (Object aChildren : children) {
            searchXLink((Element) aChildren, action);
        }
    }

    private static void cleanXLinkAttributes(Element element, String action) {
        if (action.equals(ACTION_DETACH)) {
            element.removeAttribute(XLink.HREF, XLink.NAMESPACE_XLINK);
            element.removeAttribute(XLink.ROLE, XLink.NAMESPACE_XLINK);
            element.removeAttribute(XLink.TITLE, XLink.NAMESPACE_XLINK);
        }
    }

    /** Resolves an xlink */
    public static Element resolveXLink(String uri) throws IOException, JDOMException, CacheException {
        cleanFailures();
        JeevesJCS xlinkCache = JeevesJCS.getInstance(XLINK_JCS);
        Element remoteFragment = (Element) xlinkCache.get(uri);
        if (remoteFragment == null) {
            URL url = new URL(uri.replaceAll("&amp;", "&"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            try {
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                try {
                    remoteFragment = Xml.loadStream(in);
                    if (conn.getResponseCode() >= 400) {
                        remoteFragment = null;
                    } else if (remoteFragment != null && remoteFragment.getChildren().size() > 0) {
                        xlinkCache.put(uri, remoteFragment);
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                synchronized (Processor.class) {
                    failures.add(new Failure(uri, e, System.currentTimeMillis()));
                    if (failures.size() > MAX_FAILURES) {
                        StringBuilder builder = new StringBuilder("There have been " + failures.size() + " timeouts resolving xlinks in the last " + ELAPSE_TIME + " ms\n");
                        for (Failure failure : failures) {
                            if (LOGGER.isDebugEnabled()) {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                PrintWriter writer = new PrintWriter(out);
                                failure.t.printStackTrace(writer);
                                writer.close();
                                builder.append('\n').append(failure.uri).append(" -> ").append(out).append("\n================");
                            } else {
                                builder.append('\n').append(failure.uri).append(" -> " + failure.t.getMessage());
                            }
                        }
                        builder.append('\n');
                        failures.clear();
                        throw new RuntimeException(builder.toString(), e);
                    }
                }
            }
            if (LOGGER.isDebugEnabled()) LOGGER.debug("cache miss for " + uri);
        }
        if (remoteFragment == null) {
            return new Element("ERROR").setText("Error resolving element: " + uri);
        } else {
            return (Element) remoteFragment.clone();
        }
    }

    private static synchronized void cleanFailures() {
        long now = System.currentTimeMillis();
        for (Iterator<Failure> iter = failures.iterator(); iter.hasNext(); ) {
            Failure next = iter.next();
            if (now - next.timeOfFailure > ELAPSE_TIME) {
                iter.remove();
            } else {
                break;
            }
        }
    }

    public static void removeFromCache(String xlinkUri) throws CacheException {
        JeevesJCS xlinkCache = JeevesJCS.getInstance(XLINK_JCS);
        if (xlinkCache.get(xlinkUri) != null) {
            xlinkCache.remove(xlinkUri);
        }
    }

    public static void clearCache() throws CacheException {
        JeevesJCS.getInstance(XLINK_JCS).clear();
    }
}
