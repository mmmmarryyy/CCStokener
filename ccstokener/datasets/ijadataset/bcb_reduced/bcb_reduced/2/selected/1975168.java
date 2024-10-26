package edu.xtec.jclic.project;

import edu.xtec.util.JDomUtility;
import edu.xtec.util.Options;
import edu.xtec.util.Domable;

/**
 *
 * @author Francesc Busquets (fbusquets@xtec.net)
 * @version 1.0
 */
public class LibraryManagerElement implements Domable {

    protected String name;

    protected String path;

    protected Options options;

    protected boolean exists;

    protected boolean editable;

    protected boolean isUrl;

    public static final String ELEMENT_NAME = "library";

    public static final String NAME = "name", PATH = "path";

    public LibraryManagerElement(Options options) {
        this.options = options;
        name = options.getMsg("UNNAMED");
        path = null;
        exists = false;
        editable = false;
        isUrl = false;
    }

    public LibraryManagerElement(String name, String path, Options options) {
        this.name = name;
        this.path = path;
        this.options = options;
        checkAttributes();
    }

    public static LibraryManagerElement getLibraryManagerElement(org.jdom.Element e, Options options) throws Exception {
        LibraryManagerElement lme = new LibraryManagerElement(options);
        lme.setProperties(e, null);
        return lme;
    }

    public void setProperties(org.jdom.Element e, Object aux) throws Exception {
        JDomUtility.checkName(e, ELEMENT_NAME);
        name = JDomUtility.getStringAttr(e, NAME, name, false);
        path = JDomUtility.getStringAttr(e, PATH, path, false);
        checkAttributes();
    }

    public org.jdom.Element getJDomElement() {
        org.jdom.Element e = new org.jdom.Element(ELEMENT_NAME);
        e.setAttribute(NAME, name);
        e.setAttribute(PATH, path);
        return e;
    }

    public javax.swing.Icon getIcon() {
        String base = "icons/database";
        if (exists) {
            if (!editable) base = base + "_locked";
        } else base = base + "_unavailable";
        return edu.xtec.util.ResourceManager.getImageIcon(base + ".gif");
    }

    protected void checkAttributes() {
        exists = false;
        editable = false;
        isUrl = false;
        if (path != null) {
            if (path.startsWith("http:")) {
                isUrl = true;
                try {
                    java.net.URL url = new java.net.URL(path);
                    java.net.URLConnection con = url.openConnection();
                    exists = (con != null);
                } catch (Exception ex) {
                }
            } else {
                java.io.File file = new java.io.File(path);
                exists = file.exists() && !file.isDirectory() && file.canRead();
                if (exists) editable = file.canWrite();
            }
        }
    }

    public String toString() {
        return name;
    }
}
