package org.xith3d.loaders.models.impl.kmz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.collada._2005._11.colladaschema.COLLADAType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.LoadedGraph;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.impl.dae.DaeImporter;
import org.xith3d.loaders.models.impl.dae.DaeModel;
import org.xith3d.loaders.models.impl.dae.DaeScene;
import org.xith3d.loaders.models.impl.dae.collada.ColladaConstants;
import org.xith3d.loaders.models.impl.dae.misc.JaxbCoder;
import org.xith3d.loaders.models.impl.dae.misc.NullArgumentException;
import org.xith3d.loaders.models.impl.dae.misc.XmlLib;
import org.xith3d.scenegraph.SceneGraphObject;

/*********************************************************************
     * Xith Whoola Google Earth Loader
     * 
     * <p>
     * Since the Google Earth format is a zip file (.kmz), it is read
     * using an instance of class ZipFile.  ZipFile does not accept a URL
     * constructor argument, probably because it requires random access.
     * If the load(URL) method of this Loader is called and the URL uses a
     * protocol such as "http" instead of "file", the Loader will first
     * download the data to a temporary file and then pass the filename to
     * the load(filename) method.
     * </p>
     * 
     * <p> 
     * The Google Earth format is a basically a zip file containing a
     * COLLADA file plus textures.  The COLLADA format that it uses is not
     * quite schema compliant so this Loader first calls the method
     * fixKmzColladaDocument() before passing it on.
     * </p>
     * 
     * @see
     *   java.util.zip.ZipFile
     * @version
     *   $Id: KmzLoader.java 851 2006-11-27 21:23:55 +0000 (Mo, 27 Nov 2006) Qudus $
     * @since
     *   2005-04-22
     * @author
     *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
     * @author
     *   Marvin Froehlich (aka Qudus)
     *********************************************************************/
public final class KmzLoader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "kmz";

    /*********************************************************************
     * Fixes a Google Earth COLLADA XML Document.
     *  
     * The Google Earth format is a basically a zip file containing a
     * COLLADA file plus textures.  The COLLADA format that it uses is not
     * quite schema compliant so this method fixes it.
     *********************************************************************/
    public static boolean fixKmzColladaDocument(final Document document) {
        final Element documentElement = document.getDocumentElement();
        final String nodeName = documentElement.getNodeName();
        if (!nodeName.equals("COLLADA")) {
            return false;
        }
        final Node assetNode = XmlLib.getFirstByName(documentElement, "asset");
        if (assetNode == null) {
            return false;
        }
        final Node createdNode = XmlLib.getFirstByName(assetNode, "created");
        if (createdNode == null) {
            System.out.println(KmzLoader.class.getName() + ":  " + "inserting COLLADA/asset/created node");
            final NodeList nodeList = assetNode.getChildNodes();
            final int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                final Node childNode = nodeList.item(i);
                if ("unit".equals(childNode.getNodeName())) {
                    final Element createdElement = document.createElement("created");
                    createdElement.setTextContent("1968-07-17T00:00:00");
                    assetNode.insertBefore(createdElement, childNode);
                    break;
                }
            }
        }
        final Node modifiedNode = XmlLib.getFirstByName(assetNode, "modified");
        if (modifiedNode == null) {
            System.out.println(KmzLoader.class.getName() + ":  " + "inserting COLLADA/asset/modified node");
            final NodeList nodeList = assetNode.getChildNodes();
            final int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                final Node childNode = nodeList.item(i);
                if ("unit".equals(childNode.getNodeName())) {
                    final Element createdElement = document.createElement("modified");
                    createdElement.setTextContent("1968-07-17T00:00:00");
                    assetNode.insertBefore(createdElement, childNode);
                    break;
                }
            }
        }
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate("/COLLADA/library_effects/effect/profile_COMMON/technique/" + "phong/diffuse/texture[@texcoord='1']", document, XPathConstants.NODESET);
            final int length = nodeList.getLength();
            if (length > 0) {
                System.out.println(KmzLoader.class.getName() + ":  " + "changing texcoord attribute values");
            }
            for (int i = 0; i < length; i++) {
                final Element element = (Element) nodeList.item(i);
                element.setAttribute("texcoord", "texcoord-1");
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return false;
        }
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate("/COLLADA/library_effects/effect/profile_COMMON/technique/" + "phong", document, XPathConstants.NODESET);
            final int length = nodeList.getLength();
            if (length > 0) {
                System.out.println(KmzLoader.class.getName() + ":  " + "changing ambient color values");
            }
            for (int i = 0; i < length; i++) {
                final Element phongElement = (Element) nodeList.item(i);
                final Node ambientNode = XmlLib.getFirstByName(phongElement, "ambient");
                if (ambientNode != null) {
                    final Node ambientColorNode = XmlLib.getFirstByName(ambientNode, "color");
                    if (ambientColorNode != null) {
                        final String ambientColorText = ambientColorNode.getTextContent();
                        if ("0.000000 0.000000 0.000000 1".equals(ambientColorText)) {
                            String diffuseColorText = null;
                            final Node diffuseNode = XmlLib.getFirstByName(phongElement, "diffuse");
                            if (diffuseNode != null) {
                                final Node diffuseColorNode = XmlLib.getFirstByName(diffuseNode, "color");
                                if (diffuseColorNode != null) {
                                    diffuseColorText = diffuseColorNode.getTextContent();
                                }
                            }
                            if (diffuseColorText == null) {
                                ambientColorNode.setTextContent("1 1 1 1");
                            } else {
                                ambientColorNode.setTextContent(diffuseColorText);
                            }
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return false;
        }
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate("//instance_geometry", document, XPathConstants.NODESET);
            final int length = nodeList.getLength();
            if (length > 0) {
                System.out.println(KmzLoader.class.getName() + ":  " + "shifting instance_geometry");
            }
            for (int i = 0; i < length; i++) {
                final Element element = (Element) nodeList.item(i);
                final Node parentNode = element.getParentNode();
                final Node firstChildNode = parentNode.getFirstChild();
                parentNode.removeChild(element);
                parentNode.insertBefore(element, firstChildNode);
            }
            System.out.println(KmzLoader.class.getName() + ":  " + "binding materials");
            @SuppressWarnings("unused") final XPath xPath2 = XPathFactory.newInstance().newXPath();
            final NodeList nodeList2 = (NodeList) xPath.evaluate("/COLLADA/library_materials/material", document, XPathConstants.NODESET);
            final int length2 = nodeList2.getLength();
            if (length2 > 0) {
                for (int i = 0; i < length; i++) {
                    final Element element = (Element) nodeList.item(i);
                    Element bindMaterialElement = (Element) XmlLib.getFirstByName(element, "bind_material");
                    if (bindMaterialElement != null) {
                        continue;
                    }
                    bindMaterialElement = document.createElement("bind_material");
                    element.appendChild(bindMaterialElement);
                    final Element techniqueCommonElement = document.createElement("technique_common");
                    bindMaterialElement.appendChild(techniqueCommonElement);
                    for (int j = 0; j < length2; j++) {
                        final Element instanceMaterialElement = document.createElement("instance_material");
                        techniqueCommonElement.appendChild(instanceMaterialElement);
                        final Element materialElement = (Element) nodeList2.item(j);
                        final String symbol = materialElement.getAttribute("id");
                        instanceMaterialElement.setAttribute("symbol", symbol);
                        instanceMaterialElement.setAttribute("target", symbol);
                    }
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return false;
        }
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate("//@id", document, XPathConstants.NODESET);
            final int length = nodeList.getLength();
            if (length > 0) {
                System.out.println(KmzLoader.class.getName() + ":  " + "fixing id attributes");
            }
            final Map<String, String> oldToNewIdMap = new HashMap<String, String>();
            final Random random = new Random();
            for (int i = 0; i < length; i++) {
                final Node node = nodeList.item(i);
                final String name = node.getNodeValue();
                if (!Character.isDigit(name.charAt(0))) {
                    continue;
                }
                String newName = oldToNewIdMap.get(name);
                if (newName == null) {
                    newName = "fixed_" + random.nextLong() + "_" + name;
                    oldToNewIdMap.put(name, newName);
                    final NodeList nodeList2 = (NodeList) xPath.evaluate("//*[@url='#" + name + "']", document, XPathConstants.NODESET);
                    final int length2 = nodeList2.getLength();
                    for (int j = 0; j < length2; j++) {
                        final Element element = (Element) nodeList2.item(j);
                        element.setAttribute("url", "#" + newName);
                    }
                    System.out.println("replaced bad id \"" + name + "\" with \"" + newName + "\"");
                }
                node.setNodeValue(newName);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return false;
        }
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xPath.evaluate("//@name", document, XPathConstants.NODESET);
            final int length = nodeList.getLength();
            if (length > 0) {
                System.out.println(KmzLoader.class.getName() + ":  " + "fixing name attributes");
            }
            final Map<String, String> oldToNewNameMap = new HashMap<String, String>();
            final Random random = new Random();
            for (int i = 0; i < length; i++) {
                final Node node = nodeList.item(i);
                final String name = node.getNodeValue();
                if (!Character.isDigit(name.charAt(0))) {
                    continue;
                }
                String newName = oldToNewNameMap.get(name);
                if (newName == null) {
                    newName = "fixed_" + random.nextLong() + "_" + name;
                    oldToNewNameMap.put(name, newName);
                    System.out.println("replaced bad name \"" + name + "\" with \"" + newName + "\"");
                }
                node.setNodeValue(newName);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    protected String getDefaultSkinExtension() {
        return "kmz";
    }

    public KmzLoader() {
    }

    /*********************************************************************
     * @param  flags
     * 
     *   The flags are currently ignored.
     *********************************************************************/
    public KmzLoader(final int flags) {
        super(flags);
    }

    /*********************************************************************
     * @param  flags
     * 
     *   The flags are currently ignored.
     *********************************************************************/
    public KmzLoader(URL baseURL, final int flags) {
        super(baseURL, flags);
    }

    /*********************************************************************
     * @param  flags
     * 
     *   The flags are currently ignored.
     *********************************************************************/
    public KmzLoader(String basePath, final int flags) {
        super(basePath, flags);
    }

    /*********************************************************************
     * @param  baseURL
     * 
     *   The flags are currently ignored.
     *********************************************************************/
    public KmzLoader(URL baseURL) {
        super(baseURL);
    }

    /*********************************************************************
     * @param  basePath
     * 
     *   The flags are currently ignored.
     *********************************************************************/
    public KmzLoader(String basePath) {
        super(basePath);
    }

    /**
      * Constructs a Loader with the specified baseURL, basePath and flags word.
      * 
      * @param baseURL the new baseURL to take resources from
      * @param basePath the new basePath to take resources from
      * @param flags the flags for the loader
      */
    public KmzLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    private void loadGraph(final String filename, LoadedGraph<SceneGraphObject> graph) throws IOException, ParsingErrorException {
        NullArgumentException.check(filename);
        ZipFile zipFile = null;
        InputStream inputStream = null;
        try {
            zipFile = new ZipFile(filename);
            final ZipEntry zipEntry = zipFile.getEntry("doc.kml");
            final List<String> hrefList = new LinkedList<String>();
            try {
                inputStream = zipFile.getInputStream(zipEntry);
                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                final Document document = documentBuilder.parse(inputStream);
                final XPath xPath = XPathFactory.newInstance().newXPath();
                final NodeList nodeList = (NodeList) xPath.evaluate("/kml/Placemark/Model/Link/href", document, XPathConstants.NODESET);
                final int length = nodeList.getLength();
                for (int i = 0; i < length; i++) {
                    final Element element = (Element) nodeList.item(i);
                    final String href = element.getTextContent();
                    hrefList.add(href);
                }
            } finally {
                inputStream.close();
            }
            URL baseURL2 = getBaseURL();
            if (baseURL2 == null) {
                baseURL2 = new URL("jar:" + new File(filename).toURI().toURL().toExternalForm() + "!/");
            }
            for (final String href : hrefList) {
                final URL sceneURL = new URL(baseURL2, href);
                System.out.println(getClass().getName() + ":  " + "Loading " + sceneURL);
                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                final Document document = documentBuilder.parse(sceneURL.openStream());
                fixKmzColladaDocument(document);
                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                final DOMSource domSource = new DOMSource(document);
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                final StreamResult streamResult = new StreamResult(byteArrayOutputStream);
                transformer.transform(domSource, streamResult);
                final JaxbCoder jaxbCoder = new JaxbCoder(ColladaConstants.JAXB_CONTEXT);
                final byte[] bytes = byteArrayOutputStream.toByteArray();
                final Object o = jaxbCoder.parse(new ByteArrayInputStream(bytes), -1);
                DaeImporter.importColladaScene((COLLADAType) o, sceneURL, graph);
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ParsingErrorException parsingErrorException = new ParsingErrorException(ex.getMessage());
            parsingErrorException.initCause(ex);
            throw parsingErrorException;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        DaeScene scene = new DaeScene();
        loadGraph(filename, scene);
        return scene;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(reader);
        throw new UnsupportedOperationException("loadScene(Reader) not implemented");
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(in);
        throw new UnsupportedOperationException("loadScene(InputStream) not implemented");
    }

    private void loadGraph(URL url, LoadedGraph<SceneGraphObject> graph) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        final String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            final String filename = url.getFile();
            loadGraph(filename, graph);
            return;
        }
        final String path = url.getPath();
        final int filenameIndex = path.lastIndexOf('/');
        final String filename = path.substring(filenameIndex + 1);
        final int extensionIndex = filename.indexOf('.');
        final String prefix = filename.substring(0, extensionIndex) + '_';
        final String suffix = filename.substring(extensionIndex);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit();
            final String tempFilename = tempFile.getCanonicalPath();
            System.out.println(getClass().getName() + ":  " + "creating temporary file \"" + tempFilename + "\"...");
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(url.openStream());
            final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            int i;
            while ((i = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(i);
            }
            bufferedInputStream.close();
            bufferedOutputStream.close();
            loadGraph(tempFilename, graph);
            return;
        } catch (final IOException ex) {
            throw (FileNotFoundException) new FileNotFoundException().initCause(ex);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        DaeScene scene = new DaeScene();
        loadGraph(url, scene);
        return scene;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        DaeModel model = new DaeModel();
        loadGraph(filename, model);
        return model;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        return (DaeModel) super.loadModel(filename);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(reader);
        throw new UnsupportedOperationException("loadScene(Reader) not implemented");
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(in);
        throw new UnsupportedOperationException("loadScene(InputStream) not implemented");
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        DaeModel model = new DaeModel();
        loadGraph(url, model);
        return model;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        return (DaeModel) super.loadModel(url);
    }
}
