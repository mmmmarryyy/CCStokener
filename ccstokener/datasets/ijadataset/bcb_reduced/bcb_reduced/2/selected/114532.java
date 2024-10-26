package hambo.xpres;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.apache.xerces.parsers.DOMParser;
import hambo.app.util.DOMUtil;
import hambo.util.XMLUtil;
import hambo.util.TaskMgr;
import hambo.util.Task;
import hambo.xpres.XpresApplication;

/**
 * Contains all news. The news are cept in a DOM tree structure.
 * NewsHolder is a Task so that it will periodically refresh the news content 
 * from an XML file.
 */
public class NewsHolder extends Task {

    private static NewsHolder instance = new NewsHolder();

    Document doc;

    final String newsFile = XpresApplication.NEWS_FILE.trim();

    long reloadTime = 15 * 60000;

    public HashMap bookmarksHT = new HashMap();

    public HashSet languages = new HashSet();

    public String nodeDelim = PropertyHandler.getNodeDelim();

    public HashSet applications = PropertyHandler.getApplicationsHS();

    /**
     * Initiation. Fetches the news content. Then starts the task manager TaskMgr so that new
     * content will be fetched periodically. Then set the defaultandlerHT.
     */
    private NewsHolder() {
        execute();
        TaskMgr.getTaskMgr().executePeriodically(reloadTime, this, false);
        try {
            reloadTime = Long.parseLong(XpresApplication.RELOAD_TIME.trim()) * 60000;
        } catch (Exception e) {
            System.err.println("NewsHolder.NewsHolder() : couldn't get xpres reload time from Portal");
        }
    }

    /**
     * Returns an instance of NewsHolder.
     */
    public static NewsHolder getInstance() {
        return instance;
    }

    /**
     * Fetches new news content (an XML file). Parses it, and sets the news tree Document doc.
     * Updates the bookmarksHT hashtable.
     */
    public void execute() {
        long time = System.currentTimeMillis();
        Document newDoc = null;
        InputStream in = null;
        try {
            in = openReader(XpresApplication.SERVER + newsFile);
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(in));
            newDoc = parser.getDocument();
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        if (newDoc == null) {
            System.err.println("NewsHolder: ParserToolXML: doc is null");
            System.err.println("NewsHolder: Document will not change...");
        } else {
            synchronized (this) {
                doc = newDoc;
            }
            HashMap newBookmarksHT = new HashMap();
            updateBookmarks(doc.getDocumentElement(), newBookmarksHT);
            HashSet newLanguages = PropertyHandler.getLanguagesHS();
            synchronized (this) {
                bookmarksHT = newBookmarksHT;
                languages = newLanguages;
            }
            System.out.println("NewsHolder.execute(): Time to fetch content: " + (System.currentTimeMillis() - time));
        }
    }

    /**
     * A recursive method that updates the bookmarksHT. 
     * @param elem the element to be searched.
     * @param bmHT the hashtable to update.
     */
    void updateBookmarks(Node elem, HashMap bmHT) {
        if (elem != null && elem.getNodeType() == Node.ELEMENT_NODE) {
            String bm = XMLUtil.shadow(((Element) elem).getAttribute("bookmark").trim().toLowerCase().replace(' ', '_'));
            if (!bm.equals("")) {
                bmHT.put(bm, getNewsLink((Element) elem));
            }
            NodeList nl = elem.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) updateBookmarks(nl.item(i), bmHT);
        }
    }

    /**
     * Open a InputStream to the server. Returns null if the connection throws a IOException.
     * @param s is an url string. 
     */
    InputStream openReader(String s) {
        System.err.println("Fetcher: trying url " + s);
        try {
            URL url = new URL(s);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            return url.openStream();
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * Returns the news tree document.
     */
    public final Node getDocument() {
        if (doc == null) {
            System.err.println("NewsHolder.getNodes(): Warning! Document doc is null.");
            return null;
        }
        return doc;
    }

    /**
     * Returns the node in doc with id = appId+nodeDelim+nodeId 
     * @param appId the id of a application in the news tree.
     * @param nodeId the id of a node in the news tree.
     */
    public final Node getNode(String appId, String nodeId) {
        if (doc == null) {
            System.err.println("NewsHolder.getNodes(): Warning! Document doc is null.");
            return null;
        }
        String idStr = appId;
        if (nodeId != null && !nodeId.equals("")) {
            idStr = appId + nodeDelim + nodeId;
        }
        return DOMUtil.getElementById(doc, idStr);
    }

    /**
     * Returns all children of node with id = appId+nodeDelim+nodeId  
     * @param appId the id of a application in the news tree.
     * @param nodeId the id attribute of a node in the news tree.
     */
    public final NodeList getNodes(String appId, String nodeId) {
        if (doc == null) {
            System.err.println("NewsHolder.getNodes(): Warning! Document doc is null.");
            return null;
        }
        String idStr = appId;
        if (nodeId != null && !nodeId.equals("")) {
            idStr = appId + nodeDelim + nodeId;
        }
        if (DOMUtil.getElementById(doc, idStr) == null) {
            System.err.println("NewsHolder.getNodes(): Warning! Can't find element in doc with tag id: " + idStr + ".");
            return getRootNodes(appId);
        }
        return DOMUtil.getElementById(doc, idStr).getChildNodes();
    }

    /**
     * Returns all children of the root node
     */
    public final NodeList getRootNodes(String appId) {
        if (doc == null) {
            System.err.println("NewsHolder.getRootNodes(): Warning! Document doc is null.");
            return null;
        }
        return DOMUtil.getElementById(doc, appId).getChildNodes();
    }

    /**
     * This method creates a NewsLink. The NewsLinks are created 
     * from the element. In other words the method returns a link to the node.
     * @param elem 
     */
    public NewsLink getNewsLink(Element elem) {
        NewsLink nloOut = null;
        if (elem != null) {
            String nodeName = elem.getNodeName().toLowerCase();
            if (nodeName.equals("root")) {
                nloOut = new CategoryNewsLink(elem);
            } else if (nodeName.equals("externallink")) {
                nloOut = new ExternalNewsLink(elem);
            } else if (nodeName.equals("internallink")) {
                nloOut = new InternalNewsLink(elem);
            } else if (nodeName.equals("category")) {
                nloOut = new CategoryNewsLink(elem);
            } else if (nodeName.equals("headline")) {
                nloOut = new StoryNewsLink(getDefaultStoryFromHeadline(elem));
            } else if (nodeName.equals("story")) {
                nloOut = new StoryNewsLink(elem);
            }
        }
        if (nloOut == null) System.err.println("NewsHolder.getNewsLink(): Warning! null NewsLink returned.");
        return nloOut;
    }

    /**
     * This method creates a NewsLink. The NewsLinks are created 
     * from the nodeId and appId.
     * @param appId
     * @param nodeId
     */
    public NewsLink getNewsLink(String appId, String nodeId) {
        Element elem = (Element) getNode(appId, nodeId);
        return getNewsLink(elem);
    }

    /**
     * This method creates a Vector of NewsLinks. The NewsLinks are created 
     * from the children of the node with nodeId and appId. 
     * @param appId 
     * @param nodeId 
     */
    public Vector getNewsLinks(String appId, String nodeId) {
        Vector outV = new Vector();
        NodeList nl = getNodes(appId, nodeId);
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                    if (PropertyHandler.getAllowedLinksHS().contains(node.getNodeName())) {
                        NewsLink nwlk = getNewsLink((Element) node);
                        if (nwlk != null) outV.add(nwlk);
                    }
                }
            }
        }
        return outV;
    }

    /**
     * This method creates a Vector of NewsLinks. The NewsLinks are created 
     * from the children of node with nodeId and appId. 
     * Only children with language lang_select supported are added to output.
     * @param appId
     * @param nodeId
     * @param lang_select
     */
    public Vector getNewsLinks(String appId, String nodeId, String lang_select) {
        if (lang_select == null || !languages.contains(lang_select)) {
            return getNewsLinks(appId, nodeId);
        }
        Vector outV = new Vector();
        NodeList nl = getNodes(appId, nodeId);
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if (PropertyHandler.getAllowedLinksHS().contains(node.getNodeName())) {
                        NewsLink nwlk = getNewsLink((Element) node);
                        if (nwlk != null && nwlk.supportsLanguage(lang_select)) {
                            outV.add(nwlk);
                        }
                    }
                }
            }
        }
        return outV;
    }

    /**
     * If the node with nodeId and appId is a headline or a story. This 
     * method creates the StoryObject.
     * @param appId 
     * @param nodeId 
     */
    public StoryObject getStoryObject(String appId, String nodeId) {
        Node node = getNode(appId, nodeId);
        StoryObject storyObject = null;
        Element storyElem = null;
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase().equals("headline")) {
            storyElem = getDefaultStoryFromHeadline((Element) node);
        } else if (node != null && node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase().equals("story")) {
            storyElem = (Element) node;
        }
        if (storyElem != null) {
            storyObject = new StoryObject(storyElem);
        }
        return storyObject;
    }

    /**
     * If the node with nodeId and appId is a category. This 
     * method creates a vector of the children nodes StoryObjects.
     * @param appId 
     * @param nodeId 
     */
    public Vector getStoryObjects(String appId, String nodeId) {
        Vector outV = new Vector();
        NodeList nl = getNodes(appId, nodeId);
        for (int i = 0; i < nl.getLength(); i++) {
            Element storyElem = null;
            Node node = nl.item(i);
            if (node != null && node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase().equals("headline")) {
                storyElem = getDefaultStoryFromHeadline((Element) node);
            } else if (node != null && node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase().equals("story")) {
                storyElem = (Element) node;
            }
            if (storyElem != null) {
                outV.add(new StoryObject(storyElem));
            }
        }
        return outV;
    }

    /**
     * Return the default story from the headline element he.
     * @param he the headline element.
     */
    Element getDefaultStoryFromHeadline(Element he) {
        String defStoryType = he.getAttribute("defaultstorytype").toLowerCase();
        if (defStoryType.equals("")) defStoryType = PropertyHandler.getDefaultStory();
        NodeList nl = he.getElementsByTagName("story");
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (((Element) node).getAttribute("type").toLowerCase().equals(defStoryType)) return (Element) node;
            }
        }
        if (nl.getLength() > 0) return (Element) nl.item(0);
        return null;
    }
}
