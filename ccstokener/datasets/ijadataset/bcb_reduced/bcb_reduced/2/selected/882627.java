package de.schmaller.apps.TreeDataViewer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import de.schmaller.apps.TreeDataViewer.data.ADataAdapter;
import de.schmaller.apps.TreeDataViewer.data.AJDBCDataAdapter;
import de.schmaller.apps.TreeDataViewer.data.DefaultJDBCAdapter;
import de.schmaller.apps.TreeDataViewer.ui.DefaultControlPanel;
import de.schmaller.apps.TreeDataViewer.ui.GUIList;
import de.schmaller.apps.TreeDataViewer.ui.GUIParmVar;

public class ConfigXMLReader extends AConfigClass {

    /**
    * reads the configuration from a file
    * @param fname path of the configuraion file
    */
    public static void readConfigFromFile(String fname) throws Exception {
        FileInputStream fis = new FileInputStream(fname);
        System.out.println("Reading config from file: " + new File(fname).getAbsolutePath());
        readConfig(fis);
    }

    /**
    * reads the configuration from a resource
    * @param resname path of the resource
    */
    public static void readConfigFromResource(String resname) throws Exception {
        URL url = ConfigXMLReader.class.getClassLoader().getResource(resname);
        if (url == null) throw new FileNotFoundException("Couldn't find the config resource:" + resname);
        System.out.println("Reading config from resource: " + url.toString());
        readConfig(url.openStream());
    }

    /**
	 * @param path xml configuration file
	 * @return ParmManager which contains all read config entries
	 */
    public static void readConfig(InputStream is) throws Exception {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setXIncludeAware(true);
            dbf.setNamespaceAware(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandler() {

                private void report(SAXParseException e) {
                    System.err.println(e.getMessage());
                }

                public void error(SAXParseException e) {
                    report(e);
                }

                public void fatalError(SAXParseException e) {
                    report(e);
                }

                public void warning(SAXParseException e) {
                    report(e);
                }
            });
            Document doc = db.parse(is);
            Element root = doc.getDocumentElement();
            NodeList children = null;
            if (root.getNodeName().equals("Configuration")) {
                String version = root.getAttribute("version");
                if (version != null && version.length() > 0) {
                    if ("1.0".equals(version)) {
                        conf_version = VERSION_1_0;
                    } else if ("1.1".equals(version)) {
                        conf_version = VERSION_1_1;
                    } else {
                        throw new SAXException("configuration version <" + version + "> is incompatible");
                    }
                } else {
                    conf_version = VERSION_1_0;
                }
                children = root.getElementsByTagName("ParmSetList");
                if (children.getLength() > 1) {
                    throw new SAXException("ParmSetList may occure only once!");
                }
                if (children.getLength() == 1) {
                    parseParmSetList((Element) children.item(0));
                }
                children = root.getElementsByTagName("DataAdapterList");
                if (children.getLength() != 1) {
                    throw new SAXException("DataAdapterList must occure exactly once!");
                }
                parseDataAdapterList((Element) children.item(0));
                children = root.getElementsByTagName("GUIConfig");
                for (int i = 0; i < children.getLength(); i++) {
                    parseGUIConfig((Element) children.item(i));
                }
            }
        } catch (Exception e) {
            throw new Exception("Exception occured while parsing config stream", e);
        }
    }

    /**
	 * parses all &lt;ParmSet&gt;s inside the &lt;ParmSetLis&gt;>
	 * @param e &lt;ParmSetList&gt; node
	 * @param m ParmManager to update
	 * @throws SAXParseException
	 */
    private static void parseParmSetList(Element e) throws SAXParseException {
        String name = null, dispName = null;
        Node attr = null;
        ParmSet set = null;
        NodeList children = e.getElementsByTagName("ParmSet");
        for (int i = 0; i < children.getLength(); i++) {
            attr = children.item(i).getAttributes().getNamedItem("name");
            if (attr == null) {
                throw new SAXParseException("ParmSet without {name}", null);
            }
            name = attr.getNodeValue();
            attr = children.item(i).getAttributes().getNamedItem("dispName");
            if (attr == null) {
                dispName = null;
            } else {
                dispName = attr.getNodeValue();
            }
            if (dispName == null) {
                set = new ParmSet(name);
            } else {
                set = new ParmSet(name, dispName);
            }
            parseParmSet((Element) children.item(i), set);
            ParmManager.addSet(set);
        }
    }

    private static void parseParmSet(Element e, ParmSet s) throws SAXParseException {
        String name = null, dispName = null, value = null;
        Node attr = null;
        ParmVar var = null;
        NodeList children = e.getElementsByTagName("ParmVar");
        for (int i = 0; i < children.getLength(); i++) {
            attr = children.item(i).getAttributes().getNamedItem("name");
            if (attr == null) {
                throw new SAXParseException("ParmVar without {name}", null);
            }
            name = attr.getNodeValue();
            if (name.startsWith("_")) {
                throw new SAXParseException("ParmVar with {name} starting with underscore is not allowed", null);
            }
            attr = children.item(i).getAttributes().getNamedItem("value");
            if (attr == null) {
                throw new SAXParseException("ParmVar in ParmSet must have a {value}", null);
            }
            value = attr.getNodeValue();
            attr = children.item(i).getAttributes().getNamedItem("dispName");
            if (attr == null) {
                dispName = null;
            } else {
                dispName = attr.getNodeValue();
            }
            var = new ParmVar(name, dispName, value, value.length());
            var.setAccess(ParmVar.ACCESS_HIDDEN);
            attr = children.item(i).getAttributes().getNamedItem("type");
            if (attr != null) {
                if (attr.getNodeValue().equals("string")) var.setType(ParmVar.TYPE_STRING); else if (attr.getNodeValue().equals("int")) var.setType(ParmVar.TYPE_INT); else if (attr.getNodeValue().equals("password")) var.setType(ParmVar.TYPE_PASSWD); else throw new SAXParseException("illegal {type} value: " + attr.getNodeValue(), null);
            }
            attr = children.item(i).getAttributes().getNamedItem("delim");
            if (attr != null) {
                if (attr.getNodeValue().length() != 1) {
                    throw new SAXParseException("{delim} must contain exactly one character", null);
                }
                var.setDelim(attr.getNodeValue().charAt(0));
            }
            s.addVar(var);
        }
    }

    /**
    * parses all &lt;DataAdapter&gt; elements inside a &lt;DataAdapterList&gt; 
    * @param e the &lt;DataAdapterList&gt; element
    * @param m
    * @throws SAXParseException
    */
    private static void parseDataAdapterList(Element e) throws SAXParseException, UnsupportedOperationException {
        String name = null, dispName = null;
        int dataAccess = -1, nameSource = -1, type = -1;
        Node attr = null;
        NodeList children = e.getElementsByTagName("DataAdapter");
        for (int i = 0; i < children.getLength(); i++) {
            Element xmlDA = (Element) children.item(i);
            attr = xmlDA.getAttributes().getNamedItem("name");
            if (attr == null) {
                throw new SAXParseException("DataAdapter without {name}", null);
            }
            name = attr.getNodeValue();
            attr = xmlDA.getAttributes().getNamedItem("dispName");
            if (attr == null) {
                dispName = null;
            } else {
                dispName = attr.getNodeValue();
            }
            attr = xmlDA.getAttributes().getNamedItem("dataAccess");
            if (attr == null) {
                dataAccess = -1;
            } else {
                if (attr.getNodeValue().equals("NO_DATA")) {
                    dataAccess = ADataAdapter.DATA_ACCESS_NO_DATA;
                } else if (attr.getNodeValue().equals("DIRECT")) {
                    dataAccess = ADataAdapter.DATA_ACCESS_DIRECT;
                } else if (attr.getNodeValue().equals("DEFERRED")) {
                    dataAccess = ADataAdapter.DATA_ACCESS_DEFERRED;
                } else {
                    throw new SAXParseException("<DataAdapter> has invalid {dataAccess}", null);
                }
            }
            attr = xmlDA.getAttributes().getNamedItem("nameSource");
            if (attr == null) {
                nameSource = -1;
            } else {
                if (attr.getNodeValue().equals("KEY")) {
                    nameSource = ADataAdapter.NAME_SOURCE_KEY;
                } else if (attr.getNodeValue().equals("DATA")) {
                    nameSource = ADataAdapter.NAME_SOURCE_DATA;
                } else if (attr.getNodeValue().equals("NEXT_TO_KEY")) {
                    nameSource = ADataAdapter.NAME_SOURCE_NEXT_TO_KEY;
                } else {
                    throw new SAXParseException("<DataAdapter> has invalid {nameSource}", null);
                }
            }
            if (conf_version >= 1.1f) {
                attr = xmlDA.getAttributes().getNamedItem("type");
                if (attr == null) {
                    type = -1;
                } else {
                    if (attr.getNodeValue().equals("TREE")) {
                        type = ADataAdapter.TYPE_TREE;
                    } else if (attr.getNodeValue().equals("FLAT")) {
                        type = ADataAdapter.TYPE_FLAT;
                    } else {
                        throw new SAXParseException("<DataAdapter> has invalid {type}", null);
                    }
                }
            }
            int numJDBCDA = xmlDA.getElementsByTagName("JDBCDataAdapter").getLength();
            int numFileDA = xmlDA.getElementsByTagName("FileDataAdapter").getLength();
            int numXMLDA = xmlDA.getElementsByTagName("XMLDataAdapter").getLength();
            if (numJDBCDA + numFileDA + numXMLDA != 1) {
                throw new SAXParseException("DataAdapter has too many direct sub-elements", null);
            }
            ADataAdapter thisDA = null;
            if (numJDBCDA == 1) {
                thisDA = new DefaultJDBCAdapter();
            } else if (numFileDA == 1) {
                throw new UnsupportedOperationException("File access not implemented yet!");
            } else if (numXMLDA == 1) {
                throw new UnsupportedOperationException("XML access not implemented yet!");
            }
            thisDA.setName(name);
            if (dispName != null) {
                thisDA.setDispName(dispName);
            } else {
                thisDA.setDispName(name);
            }
            if (dataAccess != -1) {
                thisDA.setDataAccess(dataAccess);
            }
            if (nameSource != -1) {
                thisDA.setNameSource(nameSource);
            }
            if (type != -1) {
                thisDA.setType(type);
            }
            if (numJDBCDA == 1) {
                parseJDBCDataAdapter((Element) xmlDA.getElementsByTagName("JDBCDataAdapter").item(0), (DefaultJDBCAdapter) thisDA);
            } else if (numFileDA == 1) {
                throw new UnsupportedOperationException("File access not implemented yet!");
            } else if (numXMLDA == 1) {
                throw new UnsupportedOperationException("XML access not implemented yet!");
            }
            ParmManager.addAdapter(thisDA);
        }
    }

    private static void parseJDBCDataAdapter(Element e, DefaultJDBCAdapter da) throws SAXParseException {
        NodeList children = null;
        children = e.getElementsByTagName("driverName");
        if (children.getLength() != 1) {
            throw new SAXParseException("too few or too many <driverName> sub-elements", null);
        }
        da.setDriverName(children.item(0).getTextContent().trim());
        children = e.getElementsByTagName("dataSource");
        if (children.getLength() != 1) {
            throw new SAXParseException("too few or too many <dataSource> sub-elements", null);
        }
        da.setDataSource(children.item(0).getTextContent().trim());
        children = e.getElementsByTagName("keySQL");
        if (children.getLength() != 1) {
            throw new SAXParseException("too few or too many <keySQL> sub-elements", null);
        }
        da.setKeySQL(children.item(0).getTextContent().trim());
        children = e.getElementsByTagName("dataSQL");
        if (da.getDataAccess() != ADataAdapter.DATA_ACCESS_NO_DATA) {
            if (children.getLength() != 1) {
                throw new SAXParseException("too few or too many <dataSQL> sub-elements", null);
            }
            if (conf_version >= 1.01f) {
                Node dirAtt = children.item(0).getAttributes().getNamedItem("direction");
                if (dirAtt != null) {
                    if ("ROWS".equals(dirAtt.getNodeValue())) {
                        da.setDataDir(AJDBCDataAdapter.DATA_DIRECTION_ROWS);
                    } else if ("COLUMNS".equals(dirAtt.getNodeValue())) {
                        da.setDataDir(AJDBCDataAdapter.DATA_DIRECTION_COLUMNS);
                    } else {
                        throw new SAXParseException("invalid value for <dataSQL>.{direction}", null);
                    }
                }
            }
            da.setDataSQL(children.item(0).getTextContent().trim());
        }
    }

    private static void parseGUIConfig(Element e) throws SAXParseException {
        String label = e.getAttribute("label");
        if ("".equals(label)) {
            throw new SAXParseException("GUIConfig needs a {label} attribute", null);
        }
        ParmManager pm = new ParmManager();
        DefaultControlPanel dcp = new DefaultControlPanel(pm, label);
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            Element guiElem = (Element) children.item(i);
            if (guiElem.getNodeName().equals("list")) {
                parseGUIList(guiElem, dcp);
            } else if (guiElem.getNodeName().equals("ParmVar")) {
                parseGUIParmVar(guiElem, dcp);
            } else if (guiElem.getNodeName().equals("ParmSet")) {
                parseGUIParmSet(guiElem, dcp);
            } else if (guiElem.getNodeName().equals("DataAdapter")) {
                parseGUIDataAdapter(guiElem, dcp);
            } else {
                throw new SAXParseException("invalid sub-element of <GUIConfig>: " + guiElem.getNodeName(), null);
            }
        }
    }

    /**
	 * @param e
	 * @param dcp
	 * @throws SAXParseException
	 */
    private static void parseGUIList(Element e, DefaultControlPanel dcp) throws SAXParseException {
        String name = e.getAttribute("name");
        String type = e.getAttribute("type");
        GUIList list = null;
        if (name == null || type == null || "".equals(name) || "".equals("type")) {
            throw new SAXParseException("invalid or missing {name} or {type} attribute", null);
        }
        if (type.equals("ParmSet")) {
            list = new GUIList(name, ParmSet.class);
        } else if (type.equals("DataAdapter")) {
            list = new GUIList(name, ADataAdapter.class);
        } else {
            throw new SAXParseException("invalid {type} attribute: " + type, null);
        }
        NodeList entries = e.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            String ename = ((Element) entries.item(i)).getAttribute("name");
            if (ename == null || "".equals(name)) {
                throw new SAXParseException("invalid or missing {name} or {type} attribute", null);
            }
            Object obj = null;
            if (type.equals("ParmSet")) {
                obj = ParmManager.getSet(ename);
            } else if (type.equals("DataAdapter")) {
                obj = ParmManager.getAdapter(ename);
            }
            if (obj == null) {
                throw new SAXParseException("There's no " + type + " with name {" + ename + "}", null);
            }
            list.addItem(obj);
        }
        dcp.add(list);
    }

    /**
	 * @param e
	 * @param dcp
	 * @throws SAXParseException
	 */
    private static void parseGUIParmVar(Element e, DefaultControlPanel dcp) throws SAXParseException {
        String name = null, dispName = null, value = null;
        int len = 0;
        Node attr = null;
        ParmVar var = null;
        attr = e.getAttributes().getNamedItem("name");
        if (attr == null) {
            throw new SAXParseException("ParmVar without {name}", null);
        }
        name = attr.getNodeValue();
        if (name.startsWith("_")) {
            throw new SAXParseException("ParmVar with {name} starting with underscore is not allowed", null);
        }
        attr = e.getAttributes().getNamedItem("value");
        if (attr != null) {
            value = attr.getNodeValue();
        }
        attr = e.getAttributes().getNamedItem("length");
        if (attr != null) {
            len = Integer.parseInt(attr.getNodeValue());
        }
        attr = e.getAttributes().getNamedItem("dispName");
        if (attr != null) {
            dispName = attr.getNodeValue();
        }
        if (len == 0 && value != null) {
            len = value.length();
        }
        var = new ParmVar(name, dispName, value, len);
        attr = e.getAttributes().getNamedItem("type");
        if (attr != null) {
            if (attr.getNodeValue().equals("string")) var.setType(ParmVar.TYPE_STRING); else if (attr.getNodeValue().equals("int")) var.setType(ParmVar.TYPE_INT); else if (attr.getNodeValue().equals("password")) var.setType(ParmVar.TYPE_PASSWD); else throw new SAXParseException("illegal {type} value: " + attr.getNodeValue(), null);
        }
        attr = e.getAttributes().getNamedItem("access");
        if (attr != null) {
            if (attr.getNodeValue().equals("RO")) var.setAccess(ParmVar.ACCESS_RO); else if (attr.getNodeValue().equals("RW")) var.setAccess(ParmVar.ACCESS_RW); else if (attr.getNodeValue().equals("HIDDEN")) var.setAccess(ParmVar.ACCESS_HIDDEN); else throw new SAXParseException("illegal {access} value: " + attr.getNodeValue(), null);
        }
        attr = e.getAttributes().getNamedItem("delim");
        if (attr != null) {
            if (attr.getNodeValue().length() != 1) {
                throw new SAXParseException("{delim} must contain exactly one character", null);
            }
            var.setDelim(attr.getNodeValue().charAt(0));
        }
        if (var.getAccess() != ParmVar.ACCESS_HIDDEN) {
            dcp.add(new GUIParmVar(var));
        } else {
            dcp.getParmManager().putCurrentVar(var);
        }
    }

    /**
    * Parses a <b>&lt;ParmSet&gt;</b> inside the &lt;GUIConfig&gt;.
    * This element contains only a {name} attribute,
    *  which is a reference to the formerly defined &lt;ParmSet&gt;   
    * @param e
    * @param m
	 */
    private static void parseGUIParmSet(Element e, DefaultControlPanel dcp) throws SAXParseException {
        String name = null;
        Node attr = null;
        ParmSet ps = null;
        attr = e.getAttributes().getNamedItem("name");
        if (attr == null) {
            throw new SAXParseException("ParmSet without {name}", null);
        }
        name = attr.getNodeValue();
        ps = ParmManager.getSet(name);
        if (ps == null) {
            throw new SAXParseException("ParmSet with name {" + name + "} not found", null);
        }
        for (int i = 0; i < ps.getVars().size(); i++) {
            dcp.getParmManager().putCurrentVar((ParmVar) ps.getVars().get(i));
        }
    }

    private static void parseGUIDataAdapter(Element e, DefaultControlPanel dcp) throws SAXParseException {
        String name = null;
        Node attr = null;
        ADataAdapter da = null;
        attr = e.getAttributes().getNamedItem("name");
        if (attr == null) {
            throw new SAXParseException("DataAdapter without {name}", null);
        }
        name = attr.getNodeValue();
        da = ParmManager.getAdapter(name);
        if (da == null) {
            throw new SAXParseException("DataAdapter with name {" + name + "} not found", null);
        }
        dcp.getParmManager().setCurrentAdapter(da);
    }
}
