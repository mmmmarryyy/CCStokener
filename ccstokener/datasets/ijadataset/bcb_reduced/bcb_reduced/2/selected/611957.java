package de.spieleck.config;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.xml.sax.InputSource;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import de.spieleck.net.URLTools;

class ConfigBuilder extends HandlerBase {

    /** 
     * The prefix of the internal attributes.
     * Note the se: are no (real) namespaces, since we use SAX1 parsing! 
     */
    public static final String PFX = "se:";

    /** Attribute for file inclusions */
    public static final String INCLUDEELEM = PFX + "include";

    /** Attribute to assign a node a direct value (without text) */
    public static final String THISATTR = PFX + "this";

    /** Element for an named param */
    public static final String PARAMELEM = PFX + "param";

    /** Attribute for the name of a parameter */
    public static final String NAME_ATTR = PFX + "name";

    /** Attribute for the (default) value of a parameter */
    public static final String VALUE_ATTR = PFX + "value";

    /** Separator between different bunches of character data. */
    public static final char TEXTSEPARATOR = ' ';

    /** Extension for setup files in included directories */
    public static final String SETUPEXTENSION = ".conf";

    /** Attribute for URL includes */
    public static final String INC_HREF = PFX + "href";

    /** Attribute for file includes */
    public static final String INC_PATH = PFX + "path";

    /** Attribute for listed includes */
    public static final String INC_LIST = PFX + "list";

    /** Attribute for directory includes */
    public static final String INC_DIR = PFX + "dir";

    /** Attribute for exclusions with directory includes */
    public static final String INC_EXCL = PFX + "exclude";

    /** Separator for lists of inclusions */
    public static final String INC_SEP = "@";

    private ConfigParamMap pm;

    private Locator locator;

    private ConfigNodeImpl node;

    private StringBuffer text = new StringBuffer(200);

    private boolean hasThis;

    ConfigBuilder(ConfigNodeImpl top, ConfigParamMap pm) {
        this.node = top;
        this.pm = pm;
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void startElement(String name, AttributeList attrs) throws SAXException {
        if (text.length() != 0) configError("subnodes must preceede text '" + name + "'", null);
        hasThis = false;
        if (name.equals(PARAMELEM)) doParam(attrs); else if (name.equals(INCLUDEELEM)) doInclude(attrs); else {
            node = node.addChild(name, null, locator.getLineNumber());
            int length = attrs.getLength();
            for (int i = 0; i < length; i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);
                if (key.equals(THISATTR)) {
                    hasThis = true;
                    node.setValue(value);
                } else if (!key.startsWith("xmlns:")) node.addChild(key, value, locator.getLineNumber());
            }
        }
    }

    protected void doParam(AttributeList atts) throws SAXException {
        String name = atts.getValue(NAME_ATTR);
        String value = atts.getValue(VALUE_ATTR);
        if (name == null) configError(PARAMELEM + " needs " + NAME_ATTR, null);
        if (value == null) value = System.getProperty(name);
        pm.set(name, value);
    }

    /**
     * obtainExtraConfig
     *
     * obtains the first existing file out of a list.
     * the syntax for this list is:
     *
     * you can mix relative with absolute paths. separate them with a comma
     * mark a relative path with 'href' and an absolute one with 'path' like this
     * href@relativepath or path@absolutepath
     *
     * @param   String  comma-separated list with relative or absolute pathinformation [href/path@relative/absolute-path]
     * @return  URL     tested URL with the first existing configuration file
     */
    private URL obtainExtraConfig(String pathlist) throws SAXException, IOException {
        StringTokenizer st = new StringTokenizer(pathlist, ",");
        while (st.hasMoreTokens()) {
            String path = st.nextToken().trim();
            StringTokenizer str = new StringTokenizer(path, INC_SEP);
            if (str.countTokens() != 2) configError(INCLUDEELEM + " with list needs the syntax " + INC_HREF + INC_SEP + "relativepath or " + INC_PATH + INC_SEP + "absolutepath", null);
            String type = str.nextToken();
            path = str.nextToken();
            URL url = null;
            if (INC_HREF.equals(type)) {
                url = getHrefURL(path);
                try {
                    url.openStream();
                    return url;
                } catch (IOException iox) {
                    configError("path error: ", iox);
                }
            } else if (INC_PATH.equals(type)) {
                if (new File(path).exists()) {
                    url = getPathURL(path);
                    return url;
                } else configError(INCLUDEELEM + ": " + path + " not found", null);
            }
        }
        return null;
    }

    private URL getHrefURL(String href) throws SAXException, IOException {
        String sid = locator.getSystemId();
        if (sid == null) configError(INCLUDEELEM + " with href needs a SystemId with inputSource", null);
        return new URL(new URL(sid), href);
    }

    private URL getPathURL(String path) throws IOException {
        return URLTools.toURL(path);
    }

    private URL[] getDirURL(String type, String dir, String exclude) throws SAXException, IOException {
        if (INC_HREF.equals(type)) {
            dir = getHrefURL(dir).getPath();
        }
        final List fv = new LinkedList();
        if (exclude != null) {
            StringTokenizer str = new StringTokenizer(exclude, ",");
            while (str.hasMoreTokens()) fv.add(str.nextToken());
        }
        File file = new File(dir);
        if (!file.exists()) configError(INCLUDEELEM + " with dir: " + dir + " not found", null);
        File[] files = file.listFiles(new FileFilter() {

            public boolean accept(File name) {
                String fname = name.toString();
                if (fv.contains(name.getName())) return false;
                return fname.endsWith(SETUPEXTENSION);
            }
        });
        URL[] url = new URL[files.length];
        for (int i = 0; i < url.length; i++) url[i] = getPathURL(files[i].toString());
        return url;
    }

    private void doInclude(AttributeList args) throws SAXException {
        try {
            String href = args.getValue(INC_HREF);
            String path = args.getValue(INC_PATH);
            String list = args.getValue(INC_LIST);
            String dir = args.getValue(INC_DIR);
            String exclude = args.getValue(INC_EXCL);
            if (exclude != null && dir == null) configError(INCLUDEELEM + " " + INC_EXCL + " only valid with " + INC_DIR + ".", null);
            if (href != null) {
                parseInclude(getHrefURL(href));
            } else if (path != null) {
                parseInclude(getPathURL(path));
            } else if (list != null) {
                parseInclude(obtainExtraConfig(list));
            } else if (dir != null) {
                StringTokenizer str = new StringTokenizer(dir, INC_SEP);
                if (str.countTokens() == 2) parseIncludes(getDirURL(str.nextToken(), str.nextToken(), exclude)); else configError(INCLUDEELEM + " with attribute '" + INC_DIR + "': " + INC_HREF + "/" + INC_PATH + INC_SEP + "directory e.g. " + INC_HREF + INC_SEP + "d:\tmp", null);
            }
        } catch (IOException e) {
            configError("include has IO problem :", e);
        }
    }

    private void parseIncludes(URL[] url) throws IOException {
        for (int i = 0; i < url.length; i++) {
            parseInclude(url[i]);
        }
    }

    private void parseInclude(URL url) throws IOException {
        InputSource is = new InputSource(url.toExternalForm());
        ConfigNodeImpl incNode = null;
        try {
            incNode = Config.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
        }
        if (incNode != null) {
            ConfigFileNode branch = node.getBranchNode();
            if (branch != null) branch.addSubReader(incNode);
            if (incNode != null) node.copyChildren(incNode);
        }
    }

    public void endElement(String name) throws SAXException {
        if (!name.equals(INCLUDEELEM) && !name.equals(PARAMELEM)) {
            if (text.length() != 0) {
                node.setValue(text.substring(0));
                text.setLength(0);
            }
            node = (ConfigNodeImpl) node.getParent();
        } else if (text.length() != 0) configError(name + " nodes must not have text.", null);
    }

    public void characters(char[] chars, int offset, int length) throws SAXException {
        for (; length > 0; length--) {
            if (!Character.isWhitespace(chars[offset])) break;
            offset++;
        }
        for (; length > 0; length--) if (!Character.isWhitespace(chars[offset + length - 1])) break;
        if (length <= 0) return;
        if (hasThis) configError("Mixes use of this and text at '" + new String(chars, offset, length) + "'", null);
        if (length > 0) {
            if (text.length() > 0) text.append(TEXTSEPARATOR);
            text.append(chars, offset, length);
        }
    }

    public void warning(SAXParseException e) {
        showError("warning", e);
    }

    public void error(SAXParseException e) {
        showError("error", e);
    }

    public void fatalError(SAXParseException e) {
        showError("fatal", e);
    }

    private void showError(String type, SAXParseException e) {
        String inputFile = e.getSystemId();
        if (inputFile == null) inputFile = "input file";
        System.err.println("Parser " + type + " @" + inputFile + ",line " + e.getLineNumber() + ",column " + e.getColumnNumber());
        System.err.println("   " + e.getMessage());
    }

    private void configError(String text, Exception e) throws SAXParseException {
        SAXParseException spe;
        if (e == null) spe = new SAXParseException(text, locator); else spe = new SAXParseException(text, locator, e);
        showError("configError", spe);
        throw spe;
    }
}
