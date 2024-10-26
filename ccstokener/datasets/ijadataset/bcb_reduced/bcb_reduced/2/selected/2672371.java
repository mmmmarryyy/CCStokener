package fr.brgm.exows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.vividsolutions.jts.geom.Coordinate;
import fr.brgm.exows.configuration.Configuration;
import fr.brgm.exows.configuration.LayerName;
import fr.brgm.exows.configuration.Original;
import fr.brgm.exows.configuration.Server;
import fr.brgm.utilities.Loader;

/**
 * @author BRGM
 * @version $Id$
 */
public class GetCapabilities extends XmlDOM {

    public static final String ls = System.getProperty("line.separator");

    public static final Map<String, CoordinateReferenceSystem> crsMap = new HashMap<String, CoordinateReferenceSystem>();

    public static void loadCapabilities(Configuration config, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String level = "info.";
        if (config.isDebug()) level = "debug.";
        Log log = LogFactory.getLog(level + "fr.brgm.exows.GetCapabilities");
        String service = ConnectorServlet.getParameter(request, "service");
        String version = ConnectorServlet.getParameter(request, "version");
        String lang = ConnectorServlet.getParameter(request, "language");
        if (lang == null) {
            lang = "";
        }
        String configName = ConnectorServlet.getParameter(request, "config");
        if (configName == null) {
            configName = "default";
        }
        String context_path = request.getRequestURL().toString();
        if (!configName.equalsIgnoreCase("default")) context_path += configName + "/";
        context_path += "?";
        response.setCharacterEncoding("UTF8");
        PrintWriter sortie = response.getWriter();
        if (service != null) {
            String result = "";
            Server serv;
            String repository;
            List<LayerName> ll;
            String defaultLang;
            if (service.equalsIgnoreCase("WMS")) {
                if (version == null) version = config.getWMS().getDefaultVersion();
                serv = config.getWMS().getServerByLang(lang);
                repository = config.getWMS().getRepository();
                ll = config.getWMS().getLayerList();
                lang = config.getWMS().getLangOrDefaultLang(lang);
                defaultLang = config.getWMS().getDefaultLang();
            } else {
                if (version == null) version = config.getWFS().getDefaultVersion();
                serv = config.getWFS().getServerByLang(lang);
                repository = config.getWFS().getRepository();
                ll = config.getWFS().getLayerList();
                lang = config.getWFS().getLangOrDefaultLang(lang);
                defaultLang = config.getWFS().getDefaultLang();
            }
            lang = lang.toLowerCase();
            String version_normalisee = "";
            if (version != null) version_normalisee = version.replace(".", "_");
            String searchedFile = "getCapabilities-" + service.toLowerCase() + "-" + version_normalisee + "-" + lang + ".xml";
            File fileSearchedFile = new File(repository + File.separator + searchedFile);
            if (!fileSearchedFile.exists()) {
                searchedFile = "getCapabilities-" + service.toLowerCase() + "-" + version_normalisee + "-" + defaultLang + ".xml";
                fileSearchedFile = new File(repository + File.separator + searchedFile);
            }
            String servURL;
            if (fileSearchedFile.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileSearchedFile)));
                String line;
                while ((line = br.readLine()) != null) {
                    result += line + ls;
                }
                if (serv != null) {
                    servURL = serv.getUrl();
                } else {
                    servURL = StringUtils.substringBetween(result, "xlink:href=\"", "\"");
                    if (StringUtils.contains(servURL, "?")) context_path += "?";
                }
                br.close();
            } else {
                log.trace("Unable to find the file in repository, request sent to server: " + fileSearchedFile.toString());
                servURL = serv.getUrl();
                String separator = "?";
                if (StringUtils.contains(servURL, "?")) separator = "&";
                URL url;
                if (version != null) url = new URL(servURL + separator + "request=GetCapabilities&version=" + version + "&service=" + service); else url = new URL(servURL + separator + "request=GetCapabilities&service=" + service);
                URLConnection urlc = url.openConnection();
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document doc = builder.parse(urlc.getInputStream());
                if (version == null || version.equalsIgnoreCase("1.3.0")) {
                    try {
                        version = ((Element) doc.getElementsByTagName("WMS_Capabilities").item(0)).getAttribute("version");
                    } catch (NullPointerException npe) {
                        try {
                            version = ((Element) doc.getElementsByTagName("WMT_MS_Capabilities").item(0)).getAttribute("version");
                        } catch (NullPointerException npee) {
                        }
                    }
                }
                try {
                    Element GetCapabilitiesNode = (Element) doc.getElementsByTagName("GetCapabilities").item(0);
                    Element GetCapabilitiesGetNode = (Element) GetCapabilitiesNode.getElementsByTagName("Get").item(0);
                    servURL = ((Element) GetCapabilitiesGetNode.getElementsByTagName("OnlineResource").item(0)).getAttribute("xlink:href");
                } catch (NullPointerException npe) {
                }
                TransformerFactory transfac = TransformerFactory.newInstance();
                Transformer trans = transfac.newTransformer();
                trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.setOutputProperty(OutputKeys.ENCODING, "UTF8");
                StringWriter sw = new StringWriter();
                StreamResult result2 = new StreamResult(sw);
                DOMSource source = new DOMSource(modifyGetCapabilities(service, version, doc, config, configName, serv, lang, log, ll));
                trans.transform(source, result2);
                result = sw.toString();
            }
            if (!fileSearchedFile.exists() && config.getMode().equals(ConnectorServlet.EXOWS_MODE_NORMAL)) {
                servURL = StringUtils.replace(servURL, "&", "&amp;");
                result = StringUtils.replace(result, servURL, context_path);
            }
            response.setContentType("text/xml; charset=utf-8");
            sortie.write(result);
        } else {
            sortie.println("eXows");
            sortie.println("---------------");
            sortie.println("REQUEST: getCapabilities");
            sortie.println("SERVICE is empty, the request cannot be completed.");
            log.trace("SERVICE is empty, the request cannot be completed.");
        }
        sortie.close();
    }

    public static String getBBOXAsString(Configuration config) throws Exception {
        String servWMSurl = config.getWMS().getServerByDefault().getUrl();
        String separator = "?";
        if (StringUtils.contains(servWMSurl, "?")) separator = "&";
        URL url = new URL(servWMSurl + separator + "request=GetCapabilities&version=1.1.1&service=WMS");
        URLConnection urlc = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            result.append(line + ls);
        }
        br.close();
        String strbbox = StringUtils.substringBetween(result.toString(), "<LatLonBoundingBox", "/>");
        String bbox = "";
        bbox += StringUtils.substringBetween(strbbox, "minx=\"", "\"") + ",";
        bbox += StringUtils.substringBetween(strbbox, "miny=\"", "\"") + ",";
        bbox += StringUtils.substringBetween(strbbox, "maxx=\"", "\"") + ",";
        bbox += StringUtils.substringBetween(strbbox, "maxy=\"", "\"");
        return bbox;
    }

    /**
	 * "on-the-fly" modification of WMS getCapabilities from the 1GEconnector xml configuration file 
	 * => translate title, abstract upon the language requested
	 * => add INSPIRE tags to be INSPIRE compliant
	 * @throws Exception 
	 */
    public static Document modifyGetCapabilities(String service, String version, Document doc, Configuration config, String configName, Server serv, String requestedLang, Log log, List<LayerName> ll) throws Exception {
        if (config.getMode().equals(ConnectorServlet.EXOWS_MODE_NORMAL)) {
            translateServiceTag(service, doc, serv, log);
        }
        translateLayerTag(doc, ll, requestedLang, config, log);
        if (service.equals("WMS")) {
            addInspire(service, version, doc, config, configName, serv, requestedLang, log);
            if (config.getMode().equals(ConnectorServlet.EXOWS_MODE_NORMAL)) addSRS(version, doc, ll, requestedLang, log); else addSRSWithoutConfig(version, doc, log);
        }
        return doc;
    }

    public static Document addSRSWithoutConfig(String version, Document doc, Log log) throws Exception {
        NodeList nodesLayer = doc.getElementsByTagName("Layer");
        for (int i = 1; i < nodesLayer.getLength(); i++) {
            Element eLayer = (Element) nodesLayer.item(i);
            NodeList nodesBBOX = eLayer.getElementsByTagName("BoundingBox");
            try {
                while (nodesBBOX.getLength() > 0) eLayer.removeChild(nodesBBOX.item(0));
            } catch (DOMException de) {
            }
            Coordinate cMin;
            Coordinate cMax;
            List<Node> crsNodeList = new ArrayList<Node>();
            if (version.equalsIgnoreCase("1.3.0")) {
                NodeList nodesCRS = eLayer.getElementsByTagName("CRS");
                for (int j = 1; j < nodesCRS.getLength(); j++) crsNodeList.add(nodesCRS.item(j));
                try {
                    cMin = new Coordinate(Double.parseDouble(((Element) eLayer.getElementsByTagName("westBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) eLayer.getElementsByTagName("southBoundLatitude").item(0)).getTextContent()));
                    cMax = new Coordinate(Double.parseDouble(((Element) eLayer.getElementsByTagName("eastBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) eLayer.getElementsByTagName("northBoundLatitude").item(0)).getTextContent()));
                } catch (NullPointerException npe) {
                    try {
                        cMin = new Coordinate(Double.parseDouble(((Element) doc.getElementsByTagName("westBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) doc.getElementsByTagName("southBoundLatitude").item(0)).getTextContent()));
                        cMax = new Coordinate(Double.parseDouble(((Element) doc.getElementsByTagName("eastBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) doc.getElementsByTagName("northBoundLatitude").item(0)).getTextContent()));
                    } catch (NullPointerException npee) {
                        cMin = new Coordinate(-180, -90);
                        cMax = new Coordinate(180, 90);
                    }
                }
            } else {
                NodeList nodesCRS = eLayer.getElementsByTagName("SRS");
                for (int j = 1; j < nodesCRS.getLength(); j++) crsNodeList.add(nodesCRS.item(j));
                try {
                    Element LLBBOX = (Element) eLayer.getElementsByTagName("LatLonBoundingBox").item(0);
                    cMin = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("minx")), Double.parseDouble(LLBBOX.getAttribute("miny")));
                    cMax = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("maxx")), Double.parseDouble(LLBBOX.getAttribute("maxy")));
                } catch (NullPointerException npe) {
                    Element LLBBOX = (Element) doc.getElementsByTagName("LatLonBoundingBox").item(0);
                    try {
                        cMin = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("minx")), Double.parseDouble(LLBBOX.getAttribute("miny")));
                        cMax = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("maxx")), Double.parseDouble(LLBBOX.getAttribute("maxy")));
                    } catch (NullPointerException npee) {
                        cMin = new Coordinate(-180, -90);
                        cMax = new Coordinate(180, 90);
                    }
                }
            }
            if (!crsMap.containsKey("EPSG:4326")) crsMap.put("EPSG:4326", CRS.decode("EPSG:4326"));
            CoordinateReferenceSystem sourceCRS = crsMap.get("EPSG:4326");
            Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
            for (Node crsNode : crsNodeList) {
                try {
                    if (!crsMap.containsKey(crsNode.getTextContent())) crsMap.put(crsNode.getTextContent(), CRS.decode(crsNode.getTextContent()));
                    CoordinateReferenceSystem targetCRS = crsMap.get(crsNode.getTextContent());
                    MathTransform transform = ReferencingFactoryFinder.getCoordinateOperationFactory(hints).createOperation(sourceCRS, targetCRS).getMathTransform();
                    Coordinate cMinOut = new Coordinate();
                    Coordinate cMaxOut = new Coordinate();
                    JTS.transform(cMin, cMinOut, transform);
                    JTS.transform(cMax, cMaxOut, transform);
                    Element BBOXNode = doc.createElement("BoundingBox");
                    if (version.equalsIgnoreCase("1.3.0")) {
                        BBOXNode.setAttribute("CRS", crsNode.getTextContent());
                    } else {
                        BBOXNode.setAttribute("SRS", crsNode.getTextContent());
                    }
                    BBOXNode.setAttribute("minx", Double.toString(cMinOut.x));
                    BBOXNode.setAttribute("maxx", Double.toString(cMaxOut.x));
                    BBOXNode.setAttribute("miny", Double.toString(cMinOut.y));
                    BBOXNode.setAttribute("maxy", Double.toString(cMaxOut.y));
                    ((Element) nodesLayer.item(i)).insertBefore(BBOXNode, null);
                } catch (NoSuchAuthorityCodeException nsace) {
                } catch (ProjectionException pe) {
                    log.error(pe.getMessage());
                }
            }
        }
        return doc;
    }

    public static Document addSRS(String version, Document doc, List<LayerName> ll, String lang, Log log) throws Exception {
        for (LayerName ln : ll) {
            if (ln.getSrs().size() > 0) {
                NodeList nodesLayer = doc.getElementsByTagName("Layer");
                for (int i = 1; i < nodesLayer.getLength(); i++) {
                    if (getDirectNodeValue((Element) nodesLayer.item(i), "Name").equalsIgnoreCase(ln.getName())) {
                        Element eLayer = (Element) nodesLayer.item(i);
                        NodeList nodesBBOX = eLayer.getElementsByTagName("BoundingBox");
                        try {
                            while (nodesBBOX.getLength() > 0) eLayer.removeChild(nodesBBOX.item(0));
                        } catch (DOMException de) {
                        }
                        Coordinate cMin;
                        Coordinate cMax;
                        if (version.equalsIgnoreCase("1.3.0")) {
                            NodeList nodesCRS = eLayer.getElementsByTagName("CRS");
                            while (nodesCRS.getLength() > 0) eLayer.removeChild(nodesCRS.item(0));
                            try {
                                cMin = new Coordinate(Double.parseDouble(((Element) eLayer.getElementsByTagName("westBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) eLayer.getElementsByTagName("southBoundLatitude").item(0)).getTextContent()));
                                cMax = new Coordinate(Double.parseDouble(((Element) eLayer.getElementsByTagName("eastBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) eLayer.getElementsByTagName("northBoundLatitude").item(0)).getTextContent()));
                            } catch (NullPointerException npe) {
                                try {
                                    cMin = new Coordinate(Double.parseDouble(((Element) doc.getElementsByTagName("westBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) doc.getElementsByTagName("southBoundLatitude").item(0)).getTextContent()));
                                    cMax = new Coordinate(Double.parseDouble(((Element) doc.getElementsByTagName("eastBoundLongitude").item(0)).getTextContent()), Double.parseDouble(((Element) doc.getElementsByTagName("northBoundLatitude").item(0)).getTextContent()));
                                } catch (NullPointerException npee) {
                                    cMin = new Coordinate(-180, -90);
                                    cMax = new Coordinate(180, 90);
                                }
                            }
                        } else {
                            NodeList nodesSRS = eLayer.getElementsByTagName("SRS");
                            try {
                                while (nodesSRS.getLength() > 0) eLayer.removeChild(nodesSRS.item(0));
                            } catch (DOMException de) {
                            }
                            try {
                                Element LLBBOX = (Element) eLayer.getElementsByTagName("LatLonBoundingBox").item(0);
                                cMin = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("minx")), Double.parseDouble(LLBBOX.getAttribute("miny")));
                                cMax = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("maxx")), Double.parseDouble(LLBBOX.getAttribute("maxy")));
                            } catch (NullPointerException npe) {
                                Element LLBBOX = (Element) doc.getElementsByTagName("LatLonBoundingBox").item(0);
                                try {
                                    cMin = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("minx")), Double.parseDouble(LLBBOX.getAttribute("miny")));
                                    cMax = new Coordinate(Double.parseDouble(LLBBOX.getAttribute("maxx")), Double.parseDouble(LLBBOX.getAttribute("maxy")));
                                } catch (NullPointerException npee) {
                                    cMin = new Coordinate(-180, -90);
                                    cMax = new Coordinate(180, 90);
                                }
                            }
                        }
                        if (!crsMap.containsKey("EPSG:4326")) crsMap.put("EPSG:4326", CRS.decode("EPSG:4326"));
                        CoordinateReferenceSystem sourceCRS = crsMap.get("EPSG:4326");
                        Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
                        for (String srs : ln.getSrs()) {
                            if (!crsMap.containsKey(srs)) crsMap.put(srs, CRS.decode(srs));
                            CoordinateReferenceSystem targetCRS = crsMap.get(srs);
                            MathTransform transform = ReferencingFactoryFinder.getCoordinateOperationFactory(hints).createOperation(sourceCRS, targetCRS).getMathTransform();
                            Coordinate cMinOut = new Coordinate();
                            Coordinate cMaxOut = new Coordinate();
                            JTS.transform(cMin, cMinOut, transform);
                            JTS.transform(cMax, cMaxOut, transform);
                            Element SRSNode;
                            Element BBOXNode = doc.createElement("BoundingBox");
                            if (version.equalsIgnoreCase("1.3.0")) {
                                BBOXNode.setAttribute("CRS", srs);
                                SRSNode = doc.createElement("CRS");
                            } else {
                                BBOXNode.setAttribute("SRS", srs);
                                SRSNode = doc.createElement("SRS");
                            }
                            SRSNode.setTextContent(srs);
                            ((Element) nodesLayer.item(i)).insertBefore(SRSNode, null);
                            BBOXNode.setAttribute("minx", Double.toString(cMinOut.x));
                            BBOXNode.setAttribute("maxx", Double.toString(cMaxOut.x));
                            BBOXNode.setAttribute("miny", Double.toString(cMinOut.y));
                            BBOXNode.setAttribute("maxy", Double.toString(cMaxOut.y));
                            ((Element) nodesLayer.item(i)).insertBefore(BBOXNode, null);
                        }
                    }
                }
            }
        }
        return doc;
    }

    public static Document addInspire(String service, String version, Document doc, Configuration config, String configName, Server serv, String requestedLang, Log log) throws Exception {
        Element root;
        try {
            Node inspireRoot;
            if (!config.getMode().equals(ConnectorServlet.EXOWS_MODE_MINICONFIG)) {
                InputStream in;
                in = Loader.getFileStream("exowsConfiguration_" + configName + "_Inspire.xml", true);
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document docINSPIRE = builder.parse(in);
                Element rootINSPIRE = docINSPIRE.getDocumentElement();
                inspireRoot = doc.importNode(rootINSPIRE, true);
                in.close();
            } else {
                inspireRoot = doc.importNode(config.getNodeINSPIRE(), true);
            }
            root = doc.getDocumentElement();
            root.setAttribute("xmlns:inspire_common", "http://inspire.ec.europa.eu/schemas/common/1.0");
            root.setAttribute("xmlns:inspire_vs", "http://inspire.ec.europa.eu/schemas/inspire_vs/1.0");
            root.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:schemaLocation", "http://inspire.ec.europa.eu/schemas/inspire_vs/1.0 http://inspire.ec.europa.eu/schemas/inspire_vs/1.0/inspire_vs.xsd");
            if (version.equals("1.3.0")) {
                NodeList nodeRootList = doc.getElementsByTagName("Capability");
                Node nodeRoot = nodeRootList.item(0);
                NodeList nodeLayerList = doc.getElementsByTagName("Layer");
                Node nodeLayer = nodeLayerList.item(0);
                nodeRoot.insertBefore(inspireRoot, nodeLayer);
            } else {
                NodeList nodeRootList = doc.getElementsByTagName("VendorSpecificCapabilities");
                if (nodeRootList.getLength() > 0) {
                    Node nodeRoot = nodeRootList.item(0);
                    nodeRoot.appendChild(inspireRoot);
                } else {
                    nodeRootList = doc.getElementsByTagName("Capability");
                    Node nodeRoot = nodeRootList.item(0);
                    NodeList nodeLayerList = doc.getElementsByTagName("Layer");
                    Node nodeLayer = nodeLayerList.item(0);
                    nodeRoot.insertBefore(inspireRoot, nodeLayer);
                }
            }
            NodeList metadataURL = doc.getElementsByTagName("inspire_common:MetadataUrl");
            Element nodeSuppLang = doc.createElement("inspire_common:SupportedLanguages");
            inspireRoot.appendChild(nodeSuppLang);
            nodeSuppLang.setAttribute("xsi:type", "inspire_common:supportedLanguagesType");
            String respLanguage = "";
            if (service.equalsIgnoreCase("WMS")) {
                for (int i = 0; i < config.getWMS().getLangs().size(); i++) {
                    Node defaultOrSupportLang;
                    if (config.getWMS().getLangs().get(i).equalsIgnoreCase(config.getWMS().getDefaultLang())) {
                        defaultOrSupportLang = doc.createElement("inspire_common:DefaultLanguage");
                    } else {
                        defaultOrSupportLang = doc.createElement("inspire_common:SupportedLanguage");
                    }
                    if (requestedLang.equals(config.getWMS().getLangs().get(i))) {
                        respLanguage = requestedLang;
                    } else {
                        respLanguage = config.getWMS().getDefaultLang();
                    }
                    nodeSuppLang.appendChild(defaultOrSupportLang);
                    Node nodeLang = doc.createElement("inspire_common:Language");
                    nodeLang.setTextContent(config.getWMS().getLangs().get(i));
                    defaultOrSupportLang.appendChild(nodeLang);
                }
            } else {
                for (int i = 0; i < config.getWFS().getLangs().size(); i++) {
                    Node defaultOrSupportLang;
                    if (!config.getWFS().getLangs().get(i).equalsIgnoreCase(config.getWFS().getDefaultLang())) {
                        defaultOrSupportLang = doc.createElement("inspire_common:DefaultLanguage");
                    } else {
                        defaultOrSupportLang = doc.createElement("inspire_common:SupportedLanguage");
                    }
                    if (requestedLang.equals(config.getWFS().getLangs().get(i))) {
                        respLanguage = requestedLang;
                    } else {
                        respLanguage = config.getWFS().getDefaultLang();
                    }
                    nodeSuppLang.appendChild(defaultOrSupportLang);
                    Node nodeLang = doc.createElement("inspire_common:Language");
                    nodeLang.setTextContent(config.getWFS().getLangs().get(i));
                    defaultOrSupportLang.appendChild(nodeLang);
                }
            }
            Node nodeRespLang = doc.createElement("inspire_common:ResponseLanguage");
            inspireRoot.appendChild(nodeRespLang);
            Node nodeLang = doc.createElement("inspire_common:Language");
            nodeLang.setTextContent(respLanguage);
            nodeRespLang.appendChild(nodeLang);
            return doc;
        } catch (Exception e) {
            log.fatal("Error in ModifyGetCapabilities to be INSPIRE compliant : ", e);
            e.printStackTrace();
            return null;
        }
    }

    public static Document translateServiceTag(String service, Document doc, Server serv, Log log) {
        try {
            if (service.equalsIgnoreCase("WMS")) {
                Node serviceNode = doc.getElementsByTagName("Service").item(0);
                NodeList serviceChild = serviceNode.getChildNodes();
                for (int i = 0; i < serviceChild.getLength(); i++) {
                    if (serviceChild.item(i).getNodeName().equals("Title") || serviceChild.item(i).getNodeName().equals("wms:Title")) {
                        serviceChild.item(i).setTextContent(serv.getTitleTranslation());
                    }
                    if (serviceChild.item(i).getNodeName().equals("Abstract") || serviceChild.item(i).getNodeName().equals("wms:Abstract")) {
                        serviceChild.item(i).setTextContent(serv.getAbstractTranslation());
                    }
                }
            }
            if (service.equalsIgnoreCase("WFS")) {
                String ns = "";
                if (doc.getDocumentElement().getNodeName().startsWith("wfs:")) ns = "wfs:";
                Node serviceNode = doc.getElementsByTagName(ns + "Service").item(0);
                NodeList serviceChild = serviceNode.getChildNodes();
                for (int i = 0; i < serviceChild.getLength(); i++) {
                    if (serviceChild.item(i).getNodeName().equals(ns + "Title")) {
                        serviceChild.item(i).setTextContent(serv.getTitleTranslation());
                    }
                    if (serviceChild.item(i).getNodeName().equals(ns + "Abstract")) {
                        serviceChild.item(i).setTextContent(serv.getAbstractTranslation());
                    }
                }
            }
            return doc;
        } catch (Exception e) {
            log.fatal("Error in translateServiceTag : ", e);
            e.printStackTrace();
            return null;
        }
    }

    public static Document translateLayerTag(Document doc, List<LayerName> ll, String lang, Configuration config, Log log) {
        try {
            for (LayerName ln : ll) {
                NodeList nodesLayer = doc.getElementsByTagName("Layer");
                for (Original o : ln.getOriginals()) {
                    for (int i = 1; i < nodesLayer.getLength(); i++) {
                        if (o.getLang().equals(lang) && getDirectNodeValue((Element) nodesLayer.item(i), "Name").equalsIgnoreCase(o.getValue())) {
                            updateDirectNodeValue((Element) nodesLayer.item(i), "Name", ln.getName());
                            updateDirectNodeValue((Element) nodesLayer.item(i), "Title", ln.getLayerTitle(lang));
                            updateDirectNodeValue((Element) nodesLayer.item(i), "Abstract", ln.getLayerAbstract(lang));
                            NodeList nodeStyle = ((Element) nodesLayer.item(i)).getElementsByTagName("Style");
                            for (int j = 0; j < nodeStyle.getLength(); j++) {
                                updateDirectNodeValue((Element) nodeStyle.item(j), "Name", o.getStyle());
                                if (o.getUrl() != null) {
                                    if (!updateDirectNodeAttributeValue((Element) nodeStyle.item(j), "OnlineResource", "xlink:href", o.getUrl())) {
                                        Element legend = doc.createElement("OnlineResource");
                                        legend.setAttribute("xlink:href", o.getUrl());
                                        ((Element) nodeStyle.item(j)).appendChild(legend);
                                    }
                                }
                                if (o.getOriginal_style() != null) {
                                    Element element = (Element) nodesLayer.item(i);
                                    element.getParentNode().removeChild(element);
                                    nodesLayer.item(1).appendChild(nodeStyle.item(j));
                                }
                            }
                        }
                    }
                }
            }
            if (!config.getMode().equals(ConnectorServlet.EXOWS_MODE_URL)) removeLayer(doc, ll, log);
            return doc;
        } catch (Exception e) {
            log.fatal("Error in translateServiceTag : ", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Remove Layer in the getCapabilities if they're not defined in the configuration File
	 * @param doc
	 * @param ll
	 * @param log
	 * @return
	 */
    public static Document removeLayer(Document doc, List<LayerName> ll, Log log) {
        try {
            NodeList nodesLayer = doc.getElementsByTagName("Layer");
            for (int i = nodesLayer.getLength(); i > 1; i--) {
                int j = 0;
                for (LayerName ln : ll) {
                    if (getDirectNodeValue((Element) nodesLayer.item(i - 1), "Name").equalsIgnoreCase(ln.getName())) {
                        j++;
                    }
                }
                if (j == 0) {
                    Element element = (Element) nodesLayer.item(i - 1);
                    element.getParentNode().removeChild(element);
                }
            }
            return doc;
        } catch (Exception e) {
            log.fatal("Error in removeLayer : ", e);
            e.printStackTrace();
            return null;
        }
    }
}
