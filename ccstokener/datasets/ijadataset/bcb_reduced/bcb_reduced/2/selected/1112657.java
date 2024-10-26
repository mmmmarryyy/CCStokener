package vmap.modes.browsemode;

import vmap.main.VmapMain;
import vmap.modes.MapAdapter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import vmap.modes.LinkRegistryAdapter;
import vmap.modes.MindMapLinkRegistry;

public class BrowseMapModel extends MapAdapter {

    private URL url;

    private LinkRegistryAdapter linkRegistry;

    public BrowseMapModel(VmapMain frame) {
        this(null, frame);
    }

    public BrowseMapModel(BrowseNodeModel root, VmapMain frame) {
        super(frame);
        if (root != null) setRoot(root); else setRoot(new BrowseNodeModel(getFrame().getResources().getString("new_mindmap"), getFrame()));
        linkRegistry = new LinkRegistryAdapter();
    }

    public MindMapLinkRegistry getLinkRegistry() {
        return linkRegistry;
    }

    public String toString() {
        if (getURL() == null) {
            return null;
        } else {
            return getURL().toString();
        }
    }

    public File getFile() {
        return null;
    }

    protected void setFile() {
    }

    /**
       * Get the value of url.
       * @return Value of url.
       */
    public URL getURL() {
        return url;
    }

    /**
       * Set the value of url.
       * @param v  Value to assign to url.
       */
    public void setURL(URL v) {
        this.url = v;
    }

    public boolean save(File file) {
        return true;
    }

    public boolean save(File file, boolean pres) {
        return true;
    }

    public boolean isSaved() {
        return true;
    }

    public void load(File file) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    public void load(URL url) throws Exception {
        setURL(url);
        BrowseNodeModel root = loadTree(url);
        if (root != null) {
            setRoot(root);
        } else {
            throw new Exception();
        }
    }

    BrowseNodeModel loadTree(URL url) {
        BrowseNodeModel root = null;
        BrowseXMLElement mapElement = new BrowseXMLElement(getFrame());
        InputStreamReader urlStreamReader = null;
        URLConnection uc = null;
        try {
            urlStreamReader = new InputStreamReader(url.openStream());
        } catch (AccessControlException ex) {
            getFrame().getController().errorMessage("Could not open URL " + url.toString() + ". Access Denied.");
            System.err.println(ex);
            return null;
        } catch (Exception ex) {
            getFrame().getController().errorMessage("Could not open URL " + url.toString() + ".");
            System.err.println(ex);
            return null;
        }
        try {
            mapElement.parseFromReader(urlStreamReader);
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
        mapElement.processUnfinishedLinks(getLinkRegistry());
        root = (BrowseNodeModel) mapElement.getMapChild();
        return root;
    }
}
