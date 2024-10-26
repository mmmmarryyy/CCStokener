package uk.ac.lkl.common.util.restlet;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.lkl.common.util.collections.RootlessTree;
import uk.ac.lkl.common.util.collections.TreeNode;
import uk.ac.lkl.common.util.reflect.GenericClass;

public class EntityLayerReader {

    private EntityLayer entityLayer;

    private String serverName;

    private int serverPort;

    private PackagePathManager entityPathManager;

    private PackagePathManager converterPathManager;

    private XMLConverterManager converterManager;

    private EntityTypeManager entityTypeManager;

    private URL url;

    public EntityLayerReader(String serverName, int serverPort, URL url) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.url = url;
    }

    public EntityLayer readEntityLayer() throws RestletException {
        if (entityLayer != null) return entityLayer;
        if (url == null) throw new RestletException("URL for entity space cannot be null.");
        Element rootElement = getRootElement(url);
        entityPathManager = createPathManager(rootElement, "EntityPackageSet");
        entityTypeManager = createEntityTypeManager(rootElement);
        converterPathManager = createPathManager(rootElement, "ConverterPackageSet");
        converterManager = createConverterManager(rootElement);
        entityLayer = readEntityLayer(rootElement);
        return entityLayer;
    }

    private EntityTypeManager createEntityTypeManager(Element rootElement) throws RestletException {
        EntityTypeManager manager = new EntityTypeManager();
        NodeList nodes = rootElement.getElementsByTagName("EntityTypeSet");
        int length = nodes.getLength();
        if (length != 1) throw new RestletException("EntityTypeSet block missing");
        Element entityTypeSetElement = (Element) nodes.item(0);
        NodeList entityTypeElements = entityTypeSetElement.getElementsByTagName("EntityType");
        for (int i = 0; i < entityTypeElements.getLength(); i++) {
            Element entityTypeElement = (Element) entityTypeElements.item(i);
            String packageName = entityTypeElement.getAttribute("package");
            String className = entityTypeElement.getAttribute("class");
            String name = entityTypeElement.getAttribute("name");
            if (name.equals("")) name = className;
            GenericClass<?> entityClass = getClass(entityPathManager, packageName, className);
            EntityType type = new EntityType(name, entityClass);
            manager.addType(type);
        }
        return manager;
    }

    private XMLConverterManager createConverterManager(Element rootElement) throws RestletException {
        NodeList nodes = rootElement.getElementsByTagName("ConverterSet");
        int length = nodes.getLength();
        if (length != 1) throw new RestletException("ConverterSet block missing");
        Element converterSetElement = (Element) nodes.item(0);
        XMLConverterManager converterManager = new XMLConverterManager();
        NodeList packageElements = converterSetElement.getElementsByTagName("Converter");
        for (int i = 0; i < packageElements.getLength(); i++) {
            Element converterElement = (Element) packageElements.item(i);
            String converterPackageName = converterElement.getAttribute("package");
            String entityTypeName = converterElement.getAttribute("entityType");
            String converterClassName = entityTypeName + "XMLConverter";
            GenericClass<?> genericConverterClass = getClass(converterPathManager, converterPackageName, converterClassName);
            Class<?> converterClass = genericConverterClass.getRawType();
            EntityType entityType = entityTypeManager.getType(entityTypeName);
            GenericClass<?> entityClass = entityType.getEntityClass();
            converterManager.setConverter(entityClass, converterClass);
        }
        return converterManager;
    }

    private EntityLayer readEntityLayer(Element rootElement) throws RestletException {
        String name = rootElement.getAttribute("name");
        if (name.trim().equals("")) throw new RestletException("EntityLayer name must be specified.");
        NodeList nodes = rootElement.getElementsByTagName("EntitySpace");
        int length = nodes.getLength();
        if (length != 1) throw new RestletException("EntitySpace block missing");
        Element entitySpaceElement = (Element) nodes.item(0);
        List<TreeNode<Entity>> children = processSubEntities(entitySpaceElement);
        RootlessTree<Entity> tree = new RootlessTree<Entity>(children);
        String handlerPackageName = entitySpaceElement.getAttribute("handlerPackage");
        String manipulatorPackageName = entitySpaceElement.getAttribute("manipulatorPackage");
        return new EntityLayer(name, serverName, serverPort, entityTypeManager, tree, converterManager, handlerPackageName, manipulatorPackageName);
    }

    private PackagePathManager createPathManager(Element rootElement, String packageSetElementName) throws RestletException {
        NodeList nodes = rootElement.getElementsByTagName(packageSetElementName);
        int length = nodes.getLength();
        if (length != 1) throw new RestletException(packageSetElementName + " must be specified");
        Element packageSetElement = (Element) nodes.item(0);
        PackagePathManager pathManager = new PackagePathManager();
        NodeList packageElements = packageSetElement.getElementsByTagName("Package");
        for (int i = 0; i < packageElements.getLength(); i++) {
            Element packageElement = (Element) packageElements.item(i);
            String name = packageElement.getAttribute("name");
            String path = packageElement.getAttribute("path");
            pathManager.setPackagePath(name, path);
        }
        String defaultPackageName = packageSetElement.getAttribute("default");
        pathManager.setDefaultPackageName(defaultPackageName);
        return pathManager;
    }

    private Element getRootElement(URL url) throws RestletException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(url.openStream());
            Element root = document.getDocumentElement();
            return root;
        } catch (Exception e) {
            throw new RestletException(e);
        }
    }

    private TreeNode<Entity> processEntityDescription(Element entityElement) throws RestletException {
        String entityName = entityElement.getAttribute("name");
        String typeName = entityElement.getAttribute("type");
        EntityType type = entityTypeManager.getType(typeName);
        GenericClass<?> entityClass = type.getEntityClass();
        boolean converterDefined = converterManager.isConverterDefinedFor(entityClass);
        if (!converterDefined) throw new RestletException("No Converter defined for " + entityClass.getSimpleName());
        String propertyString = entityElement.getAttribute("property");
        boolean isProperty = propertyString.equals("") ? true : Boolean.parseBoolean(propertyString);
        boolean isCollection = Boolean.parseBoolean(entityElement.getAttribute("collection"));
        Entity pathElement;
        if (isCollection) {
            String pluralEntityName = entityElement.getAttribute("plural");
            pathElement = new CollectionEntity(entityName, type, pluralEntityName, isProperty);
        } else {
            boolean readonly = Boolean.parseBoolean(entityElement.getAttribute("readonly"));
            pathElement = new BasicEntity(entityName, type, isProperty, readonly);
        }
        TreeNode<Entity> entityNode = new TreeNode<Entity>(pathElement);
        List<TreeNode<Entity>> subEntities = processSubEntities(entityElement);
        for (TreeNode<Entity> subEntity : subEntities) {
            entityNode.addChild(subEntity);
        }
        return entityNode;
    }

    private List<TreeNode<Entity>> processSubEntities(Element entityElement) throws RestletException {
        List<TreeNode<Entity>> entityNodes = new ArrayList<TreeNode<Entity>>();
        NodeList list = entityElement.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element childElement = (Element) node;
            String activeString = childElement.getAttribute("active");
            boolean active = activeString.equals("") ? true : Boolean.parseBoolean(activeString);
            if (active) {
                TreeNode<Entity> entityNode = processEntityDescription(childElement);
                entityNodes.add(entityNode);
            } else {
                System.out.println("Skipping: " + childElement.getAttribute("name"));
            }
        }
        return entityNodes;
    }

    private GenericClass<?> getClass(PackagePathManager pathManager, String packageName, String className) throws RestletException {
        if (className.equals("")) throw new RestletException("Classname cannot be empty");
        try {
            String packagePath = pathManager.getPackagePath(packageName);
            String qualifiedClassName = packagePath + "." + className;
            Class<?> entityClass = Class.forName(qualifiedClassName);
            return GenericClass.get(entityClass);
        } catch (ClassNotFoundException e) {
            throw new RestletException(e);
        }
    }
}
