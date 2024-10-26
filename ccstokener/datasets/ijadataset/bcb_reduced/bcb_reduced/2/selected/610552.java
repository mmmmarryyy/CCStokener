package worldwind.kml;

import gov.nasa.worldwind.render.Renderable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import worldwind.kml.model.AltitudeMode;
import worldwind.kml.model.KML3DModel;
import worldwind.kml.model.KMLColor;
import worldwind.kml.model.KMLCoord;
import worldwind.kml.model.KMLFile;
import worldwind.kml.model.KMLFolder;
import worldwind.kml.model.KMLLineString;
import worldwind.kml.model.KMLMultiGeometry;
import worldwind.kml.model.KMLObject;
import worldwind.kml.model.KMLPlacemark;
import worldwind.kml.model.KMLPoint;
import worldwind.kml.model.KMLPolygon;
import worldwind.kml.model.KMLStyle;

/**
 * Created by IntelliJ IDEA. User: tgleason Date: Sep 1, 2008 Time: 9:04:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class KMLParser implements IKMLFormat {

    public static KMLFile parseFile(String fileName) throws Exception {
        File f = new File(fileName);
        if (!f.exists()) throw new IOException("Could not find file: " + f.getPath());
        return parseURL(f.toURL());
    }

    public static KMLFile parseURL(URL urlName) throws Exception {
        InputStream is = null;
        KMLFile kml = null;
        try {
            URLConnection urlConn = urlName.openConnection();
            String contentType = urlConn.getHeaderField("Content-Type");
            System.out.println("ContentType: " + contentType);
            is = urlConn.getInputStream();
            if (urlName.getFile().endsWith("kml") || "application/vnd.google-earth.kml+xml".equals(contentType)) {
            } else if (urlName.getFile().endsWith("kmz") || "application/vnd.google-earth.kmz".equals(contentType)) {
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry = zis.getNextEntry();
                while (entry != null && !entry.getName().endsWith("kml")) {
                    entry = zis.getNextEntry();
                }
                if (entry == null) {
                    throw new Exception("No KML file found in the KMZ package");
                }
                is = zis;
            } else {
                throw new IOException("Not a KML/KMZ file.  Expected '.kml/.kmz' got '" + urlName + "'");
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(is);
            Element rootEl = doc.getDocumentElement();
            kml = new KMLFile();
            if (!"kml".equals(rootEl.getLocalName())) {
                throw new Exception("Not a KML file.  Expected 'kml' got '" + rootEl.getLocalName() + "'");
            }
            for (Element child = firstChildElement(rootEl); child != null; child = nextSiblingElement(child)) {
                String nodeName = child.getLocalName();
                if ("Document".equals(nodeName)) {
                    parseDocument(child, kml);
                } else if (FOLDER_NODE.equals(nodeName)) {
                    parseFolder(child, kml, kml.getRootFolder(), urlName);
                } else if (PLACEMARK_NODE.equals(nodeName)) {
                    parsePlacemark(child, kml, kml.getRootFolder(), urlName);
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (is != null) is.close();
        }
        return kml;
    }

    private static void parseDocument(Element docEl, KMLFile kml) {
        NodeList nl = docEl.getElementsByTagName("Schema");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            kml.addAlias(el.getAttribute("parent"), el.getAttribute(NAME_NODE));
        }
        for (Element child = firstChildElement(docEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (NAME_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                kml.getRootFolder().setName(text.trim());
            } else if (FOLDER_NODE.equals(nodeName)) {
                parseFolder(child, kml, kml.getRootFolder(), null);
            } else if (PLACEMARK_NODE.equals(nodeName) || kml.getAliasesFor(PLACEMARK_NODE).contains(nodeName)) {
                parsePlacemark(child, kml, kml.getRootFolder(), null);
            } else if (STYLE_NODE.equals(nodeName)) {
                parseStyle(child, kml);
            }
        }
    }

    private static KMLStyle parseStyle(Element styleEl, KMLFile kml) {
        KMLStyle style = new KMLStyle();
        String id = styleEl.getAttribute("id");
        if (id != null && id.trim().length() > 0) kml.addStyle(id, style);
        for (Element child = firstChildElement(styleEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if ("LineStyle".equals(nodeName)) {
                Element colorEl = childElementNamed(child, "color");
                if (colorEl != null) {
                    style.setLineStyle("color", new KMLColor(colorEl.getTextContent()));
                }
                Element widthEl = childElementNamed(child, "width");
                if (widthEl != null) {
                    style.setLineStyle("width", Float.parseFloat(widthEl.getTextContent()));
                }
            } else if (POLYSTYLE_NODE.equals(nodeName)) {
                Element colorEl = childElementNamed(child, "color");
                if (colorEl != null) {
                    style.setPolyStyle("color", new KMLColor(colorEl.getTextContent()));
                }
                Element outlineEl = childElementNamed(child, "outline");
                if (outlineEl != null) {
                    String text = outlineEl.getTextContent();
                    if (text.trim().equals("0")) {
                        style.setPolyStyle("outline", Boolean.FALSE);
                    }
                }
            }
        }
        return style;
    }

    private static void parseFolder(Element folderEl, KMLFile kml, KMLFolder parentFolder, URL url) {
        KMLFolder folder = new KMLFolder();
        parentFolder.addChildFolder(folder);
        for (Element child = firstChildElement(folderEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (NAME_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                folder.setName(text.trim());
            } else if (DESCRIPTION_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                folder.setDescription(text.trim());
            } else if (FOLDER_NODE.equals(nodeName)) {
                parseFolder(child, kml, folder, url);
            } else if (PLACEMARK_NODE.equals(nodeName) || kml.getAliasesFor(PLACEMARK_NODE).contains(nodeName)) {
                parsePlacemark(child, kml, folder, url);
            } else if (VISIBILITY_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                if (text.trim().equals("0") || text.trim().equalsIgnoreCase("false")) {
                    folder.setVisible(false);
                }
            }
        }
        if (!folder.isVisible()) {
            boolean visibile = false;
            for (KMLFolder childFolder : folder.getChildFolders()) {
                if (childFolder.isVisible()) {
                    visibile = true;
                    break;
                }
            }
            for (KMLObject childObj : folder.getObjects()) {
                if (childObj instanceof KMLPlacemark && ((KMLPlacemark) childObj).isVisible()) {
                    visibile = true;
                    break;
                }
            }
            folder.setVisible(visibile);
        }
    }

    private static void parsePlacemark(Element placemarkEl, KMLFile kml, KMLFolder folder, URL url) {
        KMLPlacemark placemark = new KMLPlacemark();
        folder.addObject(placemark);
        for (Element child = firstChildElement(placemarkEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (NAME_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                placemark.setName(text.trim());
            } else if (DESCRIPTION_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                placemark.setDescription(text.trim());
            } else if (MULTIGEOMETRY_NODE.equals(nodeName)) {
                placemark.setGraphic(parseMultiGeometry(child, kml, placemark));
            } else if (LINESTRING_NODE.equals(nodeName)) {
                placemark.setGraphic(parseLineString(child, kml, placemark));
            } else if (POINT_NODE.equals(nodeName)) {
                placemark.setGraphic(parsePoint(child, kml, placemark));
            } else if (POLYGON_NODE.equals(nodeName)) {
                placemark.setGraphic(parsePolygon(child, kml, placemark));
            } else if (STYLE_NODE.equals(nodeName)) {
                KMLStyle style = parseStyle(child, kml);
                placemark.setStyle(style);
            } else if (STYLEURL_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                if (text.startsWith("#")) {
                    text = text.substring(1);
                    KMLStyle style = kml.getStyle(text);
                    placemark.setStyle(style);
                }
            } else if (VISIBILITY_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                if (text.trim().equals("0") || text.trim().equalsIgnoreCase("false")) {
                    placemark.setVisible(false);
                }
            } else if (MODEL_NODE.equals(nodeName)) {
                KML3DModel model = parseModel(child, kml, url);
                placemark.setGraphic(model);
            }
        }
    }

    /**
	 * Parse an external 3D model.
	 */
    private static KML3DModel parseModel(Element modelElt, KMLFile kml, URL url) {
        KML3DModel model = new KML3DModel();
        for (Element child = firstChildElement(modelElt); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (ALTITUDEMODE_NODE.equalsIgnoreCase(nodeName)) {
                String content = child.getTextContent().trim();
                if (content != null) {
                    AltitudeMode mode = parseAltitudeMode(content);
                    if (mode != null) model.setAltitudeMode(mode);
                }
            } else if (LOCATION_NODE.equalsIgnoreCase(nodeName)) {
                String content = child.getTextContent().trim();
                KMLCoord coord = parseLocation(content);
                if (coord != null) model.setLocation(coord);
            } else if (LINK_NODE.equalsIgnoreCase(nodeName)) {
                parseLink(child, model, url);
            }
        }
        return model;
    }

    /**
	 * Parse link node and loads 3D model data into specified model.
	 */
    private static void parseLink(Element linkElt, KML3DModel model, URL url) {
        if (linkElt == null || model == null) throw new InvalidParameterException("Nor link neither model could be null");
        for (Element child = firstChildElement(linkElt); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (HREF_NODE.equalsIgnoreCase(nodeName)) {
                String content = child.getTextContent().trim();
                if (content != null) {
                    String file = url.getFile();
                    if (file != null) {
                        File f = new File(file);
                        if (f.exists()) {
                            File dir = f.getParentFile();
                            File target = new File(dir.getPath() + "/" + content);
                            if (content.endsWith(".dae")) {
                                Renderable mesh = loadDae(target);
                                model.setMesh(mesh);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Renderable loadDae(File target) {
        try {
            Class<?> c = Class.forName("org.worldwind.collada.Movable3DModel");
            Constructor<?> ctor = c.getDeclaredConstructor(String.class, double.class);
            Renderable mesh = (Renderable) ctor.newInstance(target.getPath(), 50000.0);
            return mesh;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static KMLLineString parseLineString(Element lsEl, KMLFile kml, KMLPlacemark placemark) {
        KMLLineString lineString = new KMLLineString();
        for (Element child = firstChildElement(lsEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (COORDINATES_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                ArrayList<KMLCoord> coords = parseCoordinates(text);
                lineString.setCoords(coords);
            } else if (TESSELLATE_NODE.equals(nodeName)) {
                String tessStr = child.getTextContent();
                boolean tess = tessStr.trim().equals("1");
                lineString.setTessellate(tess);
            } else if (ALTITUDEMODE_NODE.equals(nodeName)) {
                String altModeStr = child.getTextContent();
                if (altModeStr != null && altModeStr.trim().equals("absolute")) {
                    lineString.setAbsolute(true);
                }
            } else if (EXTRUDE_NODE.equals(nodeName)) {
                String extrude = child.getTextContent();
                if (extrude != null && extrude.trim().equals("1")) {
                    lineString.setExtrude(true);
                }
            }
        }
        return lineString;
    }

    private static KMLPoint parsePoint(Element pointEl, KMLFile kml, KMLPlacemark placemark) {
        KMLPoint point = new KMLPoint();
        for (Element child = firstChildElement(pointEl); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (COORDINATES_NODE.equals(nodeName)) {
                String text = child.getTextContent();
                ArrayList<KMLCoord> coords = parseCoordinates(text);
                KMLCoord coord = coords.get(0);
                point.setCoord(coord);
            } else if (ALTITUDEMODE_NODE.equals(nodeName)) {
                String alt = child.getTextContent();
                if ("relativeToGround".equals(alt)) {
                    point.setAltitudeMode(KMLPoint.RELATIVE_TO_GROUND);
                } else if ("absolute".equals(alt)) {
                    point.setAltitudeMode(KMLPoint.ABSOLUTE);
                }
            }
        }
        return point;
    }

    private static KMLMultiGeometry parseMultiGeometry(Element multiEL, KMLFile kml, KMLPlacemark placemark) {
        KMLMultiGeometry multigeom = new KMLMultiGeometry();
        for (Element child = firstChildElement(multiEL); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (LINESTRING_NODE.equals(nodeName)) {
                multigeom.addGeometry(parseLineString(child, kml, placemark));
            } else if (POINT_NODE.equals(nodeName)) {
                multigeom.addGeometry(parsePoint(child, kml, placemark));
            } else if (POLYGON_NODE.equals(nodeName)) {
                multigeom.addGeometry(parsePolygon(child, kml, placemark));
            }
        }
        return multigeom;
    }

    private static KMLPolygon parsePolygon(Element polyEL, KMLFile kml, KMLPlacemark placemark) {
        KMLPolygon poly = new KMLPolygon();
        for (Element child = firstChildElement(polyEL); child != null; child = nextSiblingElement(child)) {
            String nodeName = child.getLocalName();
            if (ALTITUDEMODE_NODE.equalsIgnoreCase(nodeName)) {
                String content = child.getTextContent().trim();
                if (content != null) {
                    AltitudeMode mode = parseAltitudeMode(content);
                    if (mode != null) poly.setAltitudeMode(mode);
                }
            } else if (EXTRUDE_NODE.equalsIgnoreCase(nodeName)) {
                if (child.getTextContent().trim().equalsIgnoreCase("true") || child.getTextContent().trim().equalsIgnoreCase("1")) poly.setExtrude(true); else poly.setExtrude(false);
            } else if ("outerBoundaryIs".equals(nodeName)) {
                Element linRingEl = childElementNamed(child, "LinearRing");
                if (linRingEl != null) {
                    Element coordEl = childElementNamed(linRingEl, COORDINATES_NODE);
                    if (coordEl != null) {
                        String text = coordEl.getTextContent();
                        ArrayList<KMLCoord> coords = parseCoordinates(text);
                        poly.setOuter(coords);
                    }
                }
            } else if ("innerBoundaryIs".equals(nodeName)) {
                Element linRingEl = childElementNamed(child, "LinearRing");
                if (linRingEl != null) {
                    Element coordEl = childElementNamed(linRingEl, COORDINATES_NODE);
                    if (coordEl != null) {
                        String text = coordEl.getTextContent();
                        ArrayList<KMLCoord> coords = parseCoordinates(text);
                        poly.setInner(coords);
                    }
                }
            }
        }
        return poly;
    }

    private static AltitudeMode parseAltitudeMode(String content) {
        AltitudeMode mode = null;
        for (AltitudeMode am : AltitudeMode.values()) {
            if (content.equalsIgnoreCase(am.name())) {
                mode = am;
                break;
            }
        }
        return mode;
    }

    /**
	 * Parse location definition like :<br/>
	 * &lt;longitude&gt;-105.283000000000&lt;/longitude&gt;<br/>
	 * &lt;latitude&gt;40.017000000000&lt;/latitude&gt;<br/>
	 * &lt;altitude&gt;12.000000000000&lt;/altitude&gt;<br/>
	 */
    private static KMLCoord parseLocation(String text) {
        CoordScanner scanner = new CoordScanner(text);
        int token = 0;
        while (token != CoordScanner.EOF) {
            if (token == CoordScanner.WHITESPACE) {
                while ((token = scanner.nextTokenType()) == CoordScanner.WHITESPACE) ;
            }
            if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [1], got: " + scanner.nextToken());
            if (token == CoordScanner.EOF) break;
            double lon = Double.parseDouble(scanner.nextToken());
            token = scanner.nextTokenType();
            while (token == CoordScanner.COMMA || token == CoordScanner.WHITESPACE) {
                token = scanner.nextTokenType();
            }
            if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [2], got: " + scanner.nextToken());
            double lat = Double.parseDouble(scanner.nextToken());
            token = scanner.nextTokenType();
            while (token == CoordScanner.COMMA || token == CoordScanner.WHITESPACE) {
                token = scanner.nextTokenType();
            }
            if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [3], got: " + scanner.nextToken());
            double height = Double.parseDouble(scanner.nextToken());
            if (Math.abs(lon) > 180 || Math.abs(lat) > 180) {
                System.out.println("OOps! " + lon + " " + lat);
                System.out.println("**>  " + text);
            }
            return new KMLCoord(lon, lat, height);
        }
        return null;
    }

    private static ArrayList<KMLCoord> parseCoordinates(String text) {
        ArrayList<KMLCoord> coords = new ArrayList<KMLCoord>();
        CoordScanner scanner = new CoordScanner(text);
        int token = 0;
        while (token != CoordScanner.EOF) {
            if (token == CoordScanner.WHITESPACE) {
                while ((token = scanner.nextTokenType()) == CoordScanner.WHITESPACE) ;
            }
            if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [1], got: " + scanner.nextToken());
            if (token == CoordScanner.EOF) break;
            double lon = Double.parseDouble(scanner.nextToken());
            token = scanner.nextTokenType();
            while (token == CoordScanner.COMMA || token == CoordScanner.WHITESPACE) {
                token = scanner.nextTokenType();
            }
            if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [2], got: " + scanner.nextToken());
            double lat = Double.parseDouble(scanner.nextToken());
            double height = 0;
            token = scanner.nextTokenType();
            while (token == CoordScanner.WHITESPACE) {
                token = scanner.nextTokenType();
            }
            if (token == CoordScanner.COMMA) {
                token = scanner.nextTokenType();
                while (token == CoordScanner.WHITESPACE) {
                    token = scanner.nextTokenType();
                }
                if (token != CoordScanner.NUMBER && token != CoordScanner.EOF) throw new RuntimeException("Expected a number [3], got: " + scanner.nextToken());
                if (token == CoordScanner.NUMBER) {
                    height = Double.parseDouble(scanner.nextToken());
                    token = scanner.nextTokenType();
                }
            }
            if (Math.abs(lon) > 180 || Math.abs(lat) > 180) {
                System.out.println("OOps! " + lon + " " + lat);
                System.out.println("**>  " + text);
            }
            coords.add(new KMLCoord(lon, lat, height));
        }
        return coords;
    }

    public static Element firstChildElement(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public static Element nextSiblingElement(Node node) {
        Node sibling = node.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    public static Element childElementNamed(Node parent, String childName) {
        for (Element child = firstChildElement(parent); child != null; child = nextSiblingElement(child)) {
            if (child.getLocalName().equals(childName)) {
                return child;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        KMLFile file = parseFile("/Users/tgleason/IdeaProjects/WorldWindKML/app-trail.kml");
        System.out.println("******");
        KMLFile file2 = parseFile("/Users/tgleason/IdeaProjects/WorldWindKML/KML_Samples.kml");
    }

    static class CoordScanner {

        String cs;

        int tokenStart = 0;

        int tokenEnd = 0;

        public static int EOF = -1;

        public static int WHITESPACE = 0;

        public static int NUMBER = 1;

        public static int COMMA = 2;

        CoordScanner(String cs) {
            this.cs = cs;
        }

        public int nextTokenType() {
            if (tokenEnd == (cs.length() - 1)) return EOF;
            tokenStart = tokenEnd;
            char c = cs.charAt(tokenEnd);
            if (Character.isWhitespace(c)) {
                while (Character.isWhitespace(c) && tokenEnd < (cs.length() - 1)) {
                    tokenEnd++;
                    c = cs.charAt(tokenEnd);
                }
                return WHITESPACE;
            } else if (c == ',') {
                tokenEnd++;
                return COMMA;
            } else {
                while (!(Character.isWhitespace(c) || c == ',') && tokenEnd < (cs.length() - 1)) {
                    tokenEnd++;
                    c = cs.charAt(tokenEnd);
                }
                return NUMBER;
            }
        }

        public String nextToken() {
            return cs.substring(tokenStart, tokenEnd);
        }
    }
}
