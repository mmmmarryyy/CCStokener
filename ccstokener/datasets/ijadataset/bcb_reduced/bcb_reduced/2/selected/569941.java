package tiled.io.xml;

import java.awt.Color;
import java.awt.Image;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tiled.core.*;
import tiled.io.ImageHelper;
import tiled.io.MapReader;
import tiled.io.PluginLogger;
import tiled.mapeditor.Resources;
import tiled.mapeditor.util.cutter.BasicTileCutter;
import tiled.util.Base64;
import tiled.util.Util;

/**
 * The standard map reader for TMX files.
 */
public class XMLMapTransformer implements MapReader {

    private Map map;

    private String xmlPath;

    private PluginLogger logger;

    private final EntityResolver entityResolver = new MapEntityResolver();

    private Class dummy = null;

    public XMLMapTransformer() {
        logger = new PluginLogger();
    }

    private static String makeUrl(String filename) throws MalformedURLException {
        final String url;
        if (filename.indexOf("://") > 0 || filename.startsWith("file:")) {
            url = filename;
        } else {
            url = new File(filename).toURI().toString();
        }
        return url;
    }

    private static int reflectFindMethodByName(Class c, String methodName) {
        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equalsIgnoreCase(methodName)) {
                return i;
            }
        }
        return -1;
    }

    private void reflectInvokeMethod(Object invokeVictim, Method method, String[] args) throws Exception {
        Class[] parameterTypes = method.getParameterTypes();
        Object[] conformingArguments = new Object[parameterTypes.length];
        if (args.length < parameterTypes.length) {
            throw new Exception("Insufficient arguments were supplied");
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if ("int".equalsIgnoreCase(parameterTypes[i].getName())) {
                conformingArguments[i] = new Integer(args[i]);
            } else if ("float".equalsIgnoreCase(parameterTypes[i].getName())) {
                conformingArguments[i] = new Float(args[i]);
            } else if (parameterTypes[i].getName().endsWith("String")) {
                conformingArguments[i] = args[i];
            } else if ("boolean".equalsIgnoreCase(parameterTypes[i].getName())) {
                conformingArguments[i] = Boolean.valueOf(args[i]);
            } else {
                logger.debug("Unsupported argument type " + parameterTypes[i].getName() + ", defaulting to java.lang.String");
                conformingArguments[i] = args[i];
            }
        }
        method.invoke(invokeVictim, conformingArguments);
    }

    private void setOrientation(String o) {
        if ("isometric".equalsIgnoreCase(o)) {
            map.setOrientation(Map.MDO_ISO);
        } else if ("orthogonal".equalsIgnoreCase(o)) {
            map.setOrientation(Map.MDO_ORTHO);
        } else if ("hexagonal".equalsIgnoreCase(o)) {
            map.setOrientation(Map.MDO_HEX);
        } else if ("shifted".equalsIgnoreCase(o)) {
            map.setOrientation(Map.MDO_SHIFTED);
        } else {
            logger.warn("Unknown orientation '" + o + "'");
        }
    }

    private static String getAttributeValue(Node node, String attribname) {
        final NamedNodeMap attributes = node.getAttributes();
        String value = null;
        if (attributes != null) {
            Node attribute = attributes.getNamedItem(attribname);
            if (attribute != null) {
                value = attribute.getNodeValue();
            }
        }
        return value;
    }

    private static int getAttribute(Node node, String attribname, int def) {
        final String attr = getAttributeValue(node, attribname);
        if (attr != null) {
            return Integer.parseInt(attr);
        } else {
            return def;
        }
    }

    private Object unmarshalClass(Class reflector, Node node) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor cons = null;
        try {
            cons = reflector.getConstructor((java.lang.Class) null);
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
            return null;
        }
        Object o = cons.newInstance((java.lang.Class) null);
        Node n;
        Method[] methods = reflector.getMethods();
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                n = nnm.item(i);
                try {
                    int j = reflectFindMethodByName(reflector, "set" + n.getNodeName());
                    if (j >= 0) {
                        reflectInvokeMethod(o, methods[j], new String[] { n.getNodeValue() });
                    } else {
                        logger.warn("Unsupported attribute '" + n.getNodeName() + "' on <" + node.getNodeName() + "> tag");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return o;
    }

    private Image unmarshalImage(Node t, String baseDir) throws IOException {
        Image img = null;
        String source = getAttributeValue(t, "source");
        if (source != null) {
            if (Util.checkRoot(source)) {
                source = makeUrl(source);
            } else {
                source = makeUrl(baseDir + source);
            }
            img = ImageIO.read(new URL(source));
        } else {
            NodeList nl = t.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if ("data".equals(node.getNodeName())) {
                    Node cdata = node.getFirstChild();
                    if (cdata == null) {
                        logger.warn("image <data> tag enclosed no " + "data. (empty data tag)");
                    } else {
                        String sdata = cdata.getNodeValue();
                        char[] charArray = sdata.trim().toCharArray();
                        byte[] imageData = Base64.decode(charArray);
                        img = ImageHelper.bytesToImage(imageData);
                        img = img.getScaledInstance(img.getWidth(null), img.getHeight(null), Image.SCALE_FAST);
                    }
                    break;
                }
            }
        }
        return img;
    }

    private TileSet unmarshalTilesetFile(InputStream in, String filename) throws Exception {
        TileSet set = null;
        Node tsNode;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document tsDoc = builder.parse(in, ".");
            String xmlPathSave = xmlPath;
            if (filename.indexOf(File.separatorChar) >= 0) {
                xmlPath = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
            }
            NodeList tsNodeList = tsDoc.getElementsByTagName("tileset");
            tsNode = tsNodeList.item(0);
            if (tsNode != null) {
                set = unmarshalTileset(tsNode);
                if (set.getSource() != null) {
                    logger.warn("Recursive external Tilesets are not supported.");
                }
                set.setSource(filename);
            }
            xmlPath = xmlPathSave;
        } catch (SAXException e) {
            logger.error("Failed while loading " + filename + ": " + e.getLocalizedMessage());
        }
        return set;
    }

    private TileSet unmarshalTileset(Node t) throws Exception {
        String source = getAttributeValue(t, "source");
        String basedir = getAttributeValue(t, "basedir");
        int firstGid = getAttribute(t, "firstgid", 1);
        String tilesetBaseDir = xmlPath;
        if (basedir != null) {
            tilesetBaseDir = basedir;
        }
        if (source != null) {
            String filename = tilesetBaseDir + source;
            TileSet ext = null;
            try {
                String extention = source.substring(source.lastIndexOf('.') + 1);
                if (!"tsx".equals(extention.toLowerCase())) {
                    logger.warn("tileset files should end in .tsx! (" + source + ")");
                }
                InputStream in = new URL(makeUrl(filename)).openStream();
                ext = unmarshalTilesetFile(in, filename);
            } catch (FileNotFoundException fnf) {
                logger.error("Could not find external tileset file " + filename);
            }
            if (ext == null) {
                logger.error("tileset " + source + " was not loaded correctly!");
                ext = new TileSet();
            }
            ext.setFirstGid(firstGid);
            return ext;
        } else {
            final int tileWidth = getAttribute(t, "tilewidth", map != null ? map.getTileWidth() : 0);
            final int tileHeight = getAttribute(t, "tileheight", map != null ? map.getTileHeight() : 0);
            final int tileSpacing = getAttribute(t, "spacing", 0);
            final int tileMargin = getAttribute(t, "margin", 0);
            TileSet set = new TileSet();
            set.setName(getAttributeValue(t, "name"));
            set.setBaseDir(basedir);
            set.setFirstGid(firstGid);
            boolean hasTilesetImage = false;
            NodeList children = t.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeName().equalsIgnoreCase("image")) {
                    if (hasTilesetImage) {
                        logger.warn("Ignoring illegal image element after tileset image.");
                        continue;
                    }
                    String imgSource = getAttributeValue(child, "source");
                    String id = getAttributeValue(child, "id");
                    String transStr = getAttributeValue(child, "trans");
                    if (imgSource != null && id == null) {
                        hasTilesetImage = true;
                        String sourcePath = imgSource;
                        if (!new File(imgSource).isAbsolute()) {
                            sourcePath = tilesetBaseDir + imgSource;
                        }
                        logger.info("Importing " + sourcePath + "...");
                        if (transStr != null) {
                            int colorInt = Integer.parseInt(transStr, 16);
                            Color color = new Color(colorInt);
                            set.setTransparentColor(color);
                        }
                        set.importTileBitmap(sourcePath, new BasicTileCutter(tileWidth, tileHeight, tileSpacing, tileMargin));
                    } else {
                        Image image = unmarshalImage(child, tilesetBaseDir);
                        String idValue = getAttributeValue(child, "id");
                        int imageId = Integer.parseInt(idValue);
                        set.addImage(image, imageId);
                    }
                } else if (child.getNodeName().equalsIgnoreCase("tile")) {
                    Tile tile = unmarshalTile(set, child, tilesetBaseDir);
                    if (!hasTilesetImage || tile.getId() > set.getMaxTileId()) {
                        set.addTile(tile);
                    } else {
                        Tile myTile = set.getTile(tile.getId());
                        myTile.setProperties(tile.getProperties());
                    }
                }
            }
            return set;
        }
    }

    private MapObject readMapObject(Node t) throws Exception {
        final String name = getAttributeValue(t, "name");
        final String type = getAttributeValue(t, "type");
        final int x = getAttribute(t, "x", 0);
        final int y = getAttribute(t, "y", 0);
        final int width = getAttribute(t, "width", 0);
        final int height = getAttribute(t, "height", 0);
        MapObject obj = new MapObject(x, y, width, height);
        if (name != null) obj.setName(name);
        if (type != null) obj.setType(type);
        NodeList children = t.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("image".equalsIgnoreCase(child.getNodeName())) {
                String source = getAttributeValue(child, "source");
                if (source != null) {
                    if (!new File(source).isAbsolute()) {
                        source = xmlPath + source;
                    }
                    obj.setImageSource(source);
                }
                break;
            }
        }
        Properties props = new Properties();
        readProperties(children, props);
        obj.setProperties(props);
        return obj;
    }

    /**
     * Reads properties from amongst the given children. When a "properties"
     * element is encountered, it recursively calls itself with the children
     * of this node. This function ensures backward compatibility with tmx
     * version 0.99a.
     *
     * Support for reading property values stored as character data was added
     * in Tiled 0.7.0 (tmx version 0.99c).
     *
     * @param children the children amongst which to find properties
     * @param props    the properties object to set the properties of
     */
    private static void readProperties(NodeList children, Properties props) {
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("property".equalsIgnoreCase(child.getNodeName())) {
                final String key = getAttributeValue(child, "name");
                String value = getAttributeValue(child, "value");
                if (value == null) {
                    Node grandChild = child.getFirstChild();
                    if (grandChild != null) {
                        value = grandChild.getNodeValue();
                        if (value != null) value = value.trim();
                    }
                }
                if (value != null) props.setProperty(key, value);
            } else if ("properties".equals(child.getNodeName())) {
                readProperties(child.getChildNodes(), props);
            }
        }
    }

    private Tile unmarshalTile(TileSet set, Node t, String baseDir) throws Exception {
        Tile tile = null;
        NodeList children = t.getChildNodes();
        boolean isAnimated = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("animation".equalsIgnoreCase(child.getNodeName())) {
                isAnimated = true;
                break;
            }
        }
        try {
            if (isAnimated) {
                tile = (Tile) unmarshalClass(AnimatedTile.class, t);
            } else {
                tile = (Tile) unmarshalClass(Tile.class, t);
            }
        } catch (Exception e) {
            logger.error("failed creating tile: " + e.getLocalizedMessage());
            return tile;
        }
        tile.setTileSet(set);
        readProperties(children, tile.getProperties());
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("image".equalsIgnoreCase(child.getNodeName())) {
                int id = getAttribute(child, "id", -1);
                Image img = unmarshalImage(child, baseDir);
                if (id < 0) {
                    id = set.addImage(img);
                }
                tile.setImage(id);
            } else if ("animation".equalsIgnoreCase(child.getNodeName())) {
            }
        }
        return tile;
    }

    private MapLayer unmarshalObjectGroup(Node t) throws Exception {
        ObjectGroup og = null;
        try {
            og = (ObjectGroup) unmarshalClass(ObjectGroup.class, t);
        } catch (Exception e) {
            e.printStackTrace();
            return og;
        }
        final int offsetX = getAttribute(t, "x", 0);
        final int offsetY = getAttribute(t, "y", 0);
        og.setOffset(offsetX, offsetY);
        NodeList children = t.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("object".equalsIgnoreCase(child.getNodeName())) {
                og.addObject(readMapObject(child));
            }
        }
        Properties props = new Properties();
        readProperties(children, props);
        og.setProperties(props);
        return og;
    }

    /**
     * Loads a map layer from a layer node.
     * @param t the node representing the "layer" element
     * @return the loaded map layer
     * @throws Exception
     */
    private MapLayer readLayer(Node t) throws Exception {
        final int layerWidth = getAttribute(t, "width", map.getWidth());
        final int layerHeight = getAttribute(t, "height", map.getHeight());
        TileLayer ml = new TileLayer(layerWidth, layerHeight);
        final int offsetX = getAttribute(t, "x", 0);
        final int offsetY = getAttribute(t, "y", 0);
        final int visible = getAttribute(t, "visible", 1);
        String opacity = getAttributeValue(t, "opacity");
        ml.setName(getAttributeValue(t, "name"));
        if (opacity != null) {
            ml.setOpacity(Float.parseFloat(opacity));
        }
        readProperties(t.getChildNodes(), ml.getProperties());
        for (Node child = t.getFirstChild(); child != null; child = child.getNextSibling()) {
            String nodeName = child.getNodeName();
            if ("data".equalsIgnoreCase(nodeName)) {
                String encoding = getAttributeValue(child, "encoding");
                if (encoding != null && "base64".equalsIgnoreCase(encoding)) {
                    Node cdata = child.getFirstChild();
                    if (cdata == null) {
                        logger.warn("layer <data> tag enclosed no data. (empty data tag)");
                    } else {
                        char[] enc = cdata.getNodeValue().trim().toCharArray();
                        byte[] dec = Base64.decode(enc);
                        ByteArrayInputStream bais = new ByteArrayInputStream(dec);
                        InputStream is;
                        String comp = getAttributeValue(child, "compression");
                        if (comp != null && "gzip".equalsIgnoreCase(comp)) {
                            is = new GZIPInputStream(bais);
                        } else {
                            is = bais;
                        }
                        for (int y = 0; y < ml.getHeight(); y++) {
                            for (int x = 0; x < ml.getWidth(); x++) {
                                int tileId = 0;
                                tileId |= is.read();
                                tileId |= is.read() << 8;
                                tileId |= is.read() << 16;
                                tileId |= is.read() << 24;
                                TileSet ts = map.findTileSetForTileGID(tileId);
                                if (ts != null) {
                                    ml.setTileAt(x, y, ts.getTile(tileId - ts.getFirstGid()));
                                } else {
                                    ml.setTileAt(x, y, null);
                                }
                            }
                        }
                    }
                } else {
                    int x = 0, y = 0;
                    for (Node dataChild = child.getFirstChild(); dataChild != null; dataChild = dataChild.getNextSibling()) {
                        if ("tile".equalsIgnoreCase(dataChild.getNodeName())) {
                            int tileId = getAttribute(dataChild, "gid", -1);
                            TileSet ts = map.findTileSetForTileGID(tileId);
                            if (ts != null) {
                                ml.setTileAt(x, y, ts.getTile(tileId - ts.getFirstGid()));
                            } else {
                                ml.setTileAt(x, y, null);
                            }
                            x++;
                            if (x == ml.getWidth()) {
                                x = 0;
                                y++;
                            }
                            if (y == ml.getHeight()) {
                                break;
                            }
                        }
                    }
                }
            } else if ("tileproperties".equalsIgnoreCase(nodeName)) {
                for (Node tpn = child.getFirstChild(); tpn != null; tpn = tpn.getNextSibling()) {
                    if ("tile".equalsIgnoreCase(tpn.getNodeName())) {
                        int x = getAttribute(tpn, "x", -1);
                        int y = getAttribute(tpn, "y", -1);
                        Properties tip = new Properties();
                        readProperties(tpn.getChildNodes(), tip);
                        ml.setTileInstancePropertiesAt(x, y, tip);
                    }
                }
            }
        }
        ml.setOffset(offsetX, offsetY);
        ml.setVisible(visible == 1);
        return ml;
    }

    private void buildMap(Document doc) throws Exception {
        Node item, mapNode;
        mapNode = doc.getDocumentElement();
        if (!"map".equals(mapNode.getNodeName())) {
            throw new Exception("Not a valid tmx map file.");
        }
        int mapWidth = getAttribute(mapNode, "width", 0);
        int mapHeight = getAttribute(mapNode, "height", 0);
        if (mapWidth > 0 && mapHeight > 0) {
            map = new Map(mapWidth, mapHeight);
        } else {
            NodeList l = doc.getElementsByTagName("dimensions");
            for (int i = 0; (item = l.item(i)) != null; i++) {
                if (item.getParentNode() == mapNode) {
                    mapWidth = getAttribute(item, "width", 0);
                    mapHeight = getAttribute(item, "height", 0);
                    if (mapWidth > 0 && mapHeight > 0) {
                        map = new Map(mapWidth, mapHeight);
                    }
                }
            }
        }
        if (map == null) {
            throw new Exception("Couldn't locate map dimensions.");
        }
        String orientation = getAttributeValue(mapNode, "orientation");
        int tileWidth = getAttribute(mapNode, "tilewidth", 0);
        int tileHeight = getAttribute(mapNode, "tileheight", 0);
        if (tileWidth > 0) {
            map.setTileWidth(tileWidth);
        }
        if (tileHeight > 0) {
            map.setTileHeight(tileHeight);
        }
        if (orientation != null) {
            setOrientation(orientation);
        } else {
            setOrientation("orthogonal");
        }
        readProperties(mapNode.getChildNodes(), map.getProperties());
        NodeList l = doc.getElementsByTagName("tileset");
        for (int i = 0; (item = l.item(i)) != null; i++) {
            map.addTileset(unmarshalTileset(item));
        }
        for (Node sibs = mapNode.getFirstChild(); sibs != null; sibs = sibs.getNextSibling()) {
            if ("layer".equals(sibs.getNodeName())) {
                MapLayer layer = readLayer(sibs);
                if (layer != null) {
                    map.addLayer(layer);
                }
            } else if ("objectgroup".equals(sibs.getNodeName())) {
                MapLayer layer = unmarshalObjectGroup(sibs);
                if (layer != null) {
                    map.addLayer(layer);
                }
            }
        }
    }

    private Map unmarshal(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(entityResolver);
            InputSource insrc = new InputSource(in);
            insrc.setSystemId(xmlPath);
            insrc.setEncoding("UTF8");
            doc = builder.parse(insrc);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new Exception("Error while parsing map file: " + e.toString());
        }
        buildMap(doc);
        return map;
    }

    public Map readMap(String filename) throws Exception {
        xmlPath = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
        String xmlFile = makeUrl(filename);
        URL url = new URL(xmlFile);
        InputStream is = url.openStream();
        if (filename.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        Map unmarshalledMap = unmarshal(is);
        unmarshalledMap.setFilename(filename);
        map = null;
        return unmarshalledMap;
    }

    public Map readMap(InputStream in) throws Exception {
        xmlPath = makeUrl(".");
        Map unmarshalledMap = unmarshal(in);
        return unmarshalledMap;
    }

    public TileSet readTileset(String filename) throws Exception {
        String xmlFile = filename;
        xmlPath = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
        xmlFile = makeUrl(xmlFile);
        xmlPath = makeUrl(xmlPath);
        URL url = new URL(xmlFile);
        return unmarshalTilesetFile(url.openStream(), filename);
    }

    public TileSet readTileset(InputStream in) throws Exception {
        return unmarshalTilesetFile(in, ".");
    }

    /**
     * @see tiled.io.PluggableMapIO#getFilter()
     */
    public String getFilter() throws Exception {
        return "*.tmx,*.tmx.gz,*.tsx";
    }

    public String getPluginPackage() {
        return "Tiled internal TMX reader/writer";
    }

    /**
     * @see tiled.io.PluggableMapIO#getDescription()
     */
    public String getDescription() {
        return "This is the core Tiled TMX format reader\n" + "\n" + "Tiled Map Editor, (c) 2004-2008\n" + "Adam Turk\n" + "Bjorn Lindeijer";
    }

    public String getName() {
        return "Default Tiled XML (TMX) map reader";
    }

    public boolean accept(File pathname) {
        try {
            String path = pathname.getCanonicalPath();
            if (path.endsWith(".tmx") || path.endsWith(".tsx") || path.endsWith(".tmx.gz")) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public void setLogger(PluginLogger logger) {
        this.logger = logger;
    }

    private class MapEntityResolver implements EntityResolver {

        public InputSource resolveEntity(String publicId, String systemId) {
            if (systemId.equals("http://mapeditor.org/dtd/1.0/map.dtd")) {
                return new InputSource(Resources.class.getResourceAsStream("resources/map.dtd"));
            }
            return null;
        }
    }
}
