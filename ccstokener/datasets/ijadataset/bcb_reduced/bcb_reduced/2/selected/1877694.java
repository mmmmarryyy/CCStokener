package org.fao.waicent.kids.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xpath.XPathAPI;
import org.fao.waicent.kids.Configuration;
import org.fao.waicent.util.Debug;
import org.fao.waicent.util.XMLUtil;
import org.fao.waicent.xmap2D.FeatureLayer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GeonetMetadata {

    private String layer_name = "";

    private Document meta_doc = null;

    private Element ele = null;

    private String catalog_url = "";

    private String metadata_url = "";

    private Configuration configuration;

    public GeonetMetadata(Configuration config, int geonetwork_id) {
        this.configuration = config;
        this.catalog_url = configuration.getGeonetworkCatalogURL() + "?id=" + geonetwork_id;
        this.metadata_url = configuration.getGeonetworkMetadataURL() + "&id=" + geonetwork_id;
        setFormats(configuration.getLayerFormatsFileList());
    }

    public void setMetadataURL(String url) {
        this.metadata_url = url;
    }

    public String getMetadataURL() {
        return this.metadata_url;
    }

    public boolean connect() {
        try {
            URL url = new URL(catalog_url);
            InputStream in = url.openStream();
            if (!loadInputStream(in)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            this._message = e.getMessage();
            return false;
        }
    }

    public OutputStream loadOutputStream(int index) {
        return null;
    }

    public boolean loadInputStream(InputStream in) {
        try {
            Document doc = XMLUtil.loadDocument(in);
            this.meta_doc = doc;
            this.ele = meta_doc.getDocumentElement();
            return true;
        } catch (Exception e) {
            this._message = e.getMessage();
            return false;
        }
    }

    public GeonetMetadata(Document doc, Element ele) throws IOException {
        this.meta_doc = doc;
        this.ele = ele;
    }

    public Element getFilesListElement() {
        return filesListElem;
    }

    private void initFilesListElement() {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            layers_doc = docBuilder.newDocument();
            filesListElem = layers_doc.createElement("GEONET_MAP");
            filesListElem.setAttribute("metadata_url", metadata_url);
            filesListElem.setAttribute("layer_name", layer_name);
        } catch (Exception ex) {
            Debug.println("Error in initializing GEONET_MAP element");
        }
    }

    private Document layers_doc;

    public void save(Document doc, Element ele) throws IOException {
    }

    public boolean validate() {
        if (!connect()) {
            setMessage("Unable to obtain connection to the requested map from Geonetwork");
            return false;
        }
        return initListElem();
    }

    Hashtable hash = new Hashtable();

    private boolean initListElem() {
        setLayerName();
        String xpath = "/response/record/files/file";
        try {
            NodeList nodeList = XPathAPI.selectNodeList(meta_doc, xpath);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node filenameNode = XPathAPI.selectSingleNode(nodeList.item(i), ".//filename/text()");
                    if (filenameNode != null) {
                        String str = filenameNode.getNodeValue();
                        hash.put(str, nodeList.item(i).cloneNode(true));
                    }
                }
                initFilesListElement();
            }
            Enumeration filenames = hash.keys();
            while (filenames.hasMoreElements()) {
                String str = (String) filenames.nextElement();
                if (ImportLayerWizard.getLayerType(str) != -1) {
                    addToListElem(str);
                }
            }
        } catch (Exception ex) {
            Debug.println("Error in GeonetMetadata.initListElem: " + ex.getMessage());
        }
        if (hasImportableLayer()) {
            setMessage("");
            return true;
        } else {
            setMessage("Map does not have any importable layer. ");
            return false;
        }
    }

    private Vector getSupportExtensions(String ext) {
        Vector supportExtensions = new Vector();
        int ctr = 0;
        for (int i = 0; i < layerFormatsFileList.length; i++) {
            if (ext.equalsIgnoreCase(layerFormatsFileList[i][0])) {
                if (layerFormatsFileList[i].length > 1) {
                    for (int j = 1; j < layerFormatsFileList[i].length; j++, ctr++) {
                        supportExtensions.add(layerFormatsFileList[i][j]);
                    }
                }
                break;
            }
        }
        return supportExtensions;
    }

    private boolean addToListElem(String filename) {
        String ext = "";
        try {
            ext = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        } catch (Exception ex) {
            ext = "";
        }
        try {
            Element layerElem = layers_doc.createElement("LAYER");
            layerElem.setAttribute("mainfile", filename);
            int tot_size = 0;
            Element imported_element = (Element) getNode(filename);
            imported_element = (Element) layers_doc.importNode(imported_element, true);
            if (imported_element != null) {
                layerElem.appendChild(imported_element);
                tot_size += Integer.parseInt(imported_element.getAttribute("size"));
            }
            Vector supportExt = getSupportExtensions(ext);
            for (int i = 0; i < supportExt.size(); i++) {
                filename = filename.substring(0, filename.lastIndexOf(".") + 1);
                filename += supportExt.elementAt(i);
                imported_element = (Element) getNode(filename);
                imported_element = (Element) layers_doc.importNode(imported_element, true);
                if (imported_element != null) {
                    layerElem.appendChild(imported_element);
                    tot_size += Integer.parseInt(imported_element.getAttribute("size"));
                }
            }
            layerElem.setAttribute("total_size", Integer.toString(tot_size));
            filesListElem.appendChild(layerElem);
        } catch (Exception e) {
            System.out.println("   Error in creating GEONET_MAP : " + e);
        }
        return true;
    }

    /**
     * gets the hash the value for the key filename
     * transforms to a single tag <file>
     * <file name="146.dbf"
     path="\\artemis1\geonet\agll\farming\world\global_suitability_f_146\146.gif"
     absolute-path="http://tectest:9280/geonet/agll/farming/world/global_suitability_f_146/146.gif"
     date="Sun Oct 14 10:32:05 CEST 2001"
     size="53054"/>
     * */
    private Node getNode(String filename) {
        Node filenode = (Node) hash.get(filename);
        Element fileElem = layers_doc.createElement("file");
        try {
            String attr = (XPathAPI.selectSingleNode(filenode, ".//filename/text()")).getNodeValue();
            fileElem.setAttribute("name", attr);
            attr = (XPathAPI.selectSingleNode(filenode, ".//path/text()")).getNodeValue();
            fileElem.setAttribute("path", attr);
            attr = (XPathAPI.selectSingleNode(filenode, ".//absolute-path/text()")).getNodeValue();
            fileElem.setAttribute("absolute-path", attr);
            attr = (XPathAPI.selectSingleNode(filenode, ".//date/text()")).getNodeValue();
            fileElem.setAttribute("date", attr);
            attr = (XPathAPI.selectSingleNode(filenode, ".//size/text()")).getNodeValue();
            fileElem.setAttribute("size", attr);
        } catch (Exception ex) {
            Debug.println("Error in GeonetMetadata.getNode(" + filename + ") : " + ex.getMessage());
        }
        return fileElem;
    }

    private Element filesListElem;

    public Element getListElem() {
        return filesListElem;
    }

    private void setLayerName() {
        String xpath = "/response/record/Metadata/dataIdInfo/idCitation/resTitle/text()";
        try {
            Node node = XPathAPI.selectSingleNode(meta_doc, xpath);
            if (node != null) {
                layer_name = node.getNodeValue();
                if (layer_name.length() > 25) {
                    layer_name.substring(0, 24);
                }
            } else {
                layer_name = "Unnamed";
            }
        } catch (Exception e) {
            layer_name = "Unnamed";
        }
    }

    public String getLayerName() {
        return layer_name;
    }

    public void setLayerName(String str) {
        layer_name = str;
    }

    private String[][] layerFormatsFileList;

    public void setFormats(String[][] fileList) {
        this.layerFormatsFileList = (String[][]) fileList.clone();
        for (int i = 0; i < fileList.length; ++i) {
            if (layerFormatsFileList[i] != null) {
                this.layerFormatsFileList[i] = (String[]) fileList[i].clone();
            }
        }
    }

    private String _message = "";

    public String getMessage() {
        return _message;
    }

    public void setMessage(String str) {
        this._message += str;
    }

    private FeatureLayer f_layer = null;

    public FeatureLayer getFeatureLayer() {
        return f_layer;
    }

    public void setConfiguration(Configuration config) {
        this.configuration = config;
    }

    public boolean hasImportableLayer() {
        return getListElem().hasChildNodes();
    }

    public boolean checkFiles(int idx) {
        layerFilesPath.clear();
        if (hasImportableLayer()) {
            Node selected_layer_node = getListElem().getChildNodes().item(idx);
            if (selected_layer_node != null) {
                NodeList filesNodes = selected_layer_node.getChildNodes();
                for (int i = 0; i < filesNodes.getLength(); i++) {
                    String abs_path = ((Element) filesNodes.item(i)).getAttribute("absolute-path");
                    layerFilesPath.add(abs_path);
                }
            }
        }
        return true;
    }

    private Vector layerFilesPath = new Vector();

    public Vector getLayerFilesPath() {
        return layerFilesPath;
    }
}
