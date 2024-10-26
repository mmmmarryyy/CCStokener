package imi.loaders;

import imi.loaders.xml.XMLFloatColumn;
import imi.loaders.xml.XMLMatrix;
import imi.loaders.xml.XMLNode;
import imi.loaders.xml.XMLNodeCollection;
import imi.loaders.xml.XMLRefNode;
import imi.repository.ColladaRepoComponent;
import imi.repository.RRL;
import imi.repository.Repository;
import imi.scene.PMatrix;
import imi.scene.PNode;
import imi.scene.PTransform;
import imi.scene.polygonmodel.ModelInstance;
import imi.scene.utils.traverser.NodeProcessor;
import imi.scene.utils.traverser.TreeTraverser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javolution.util.FastList;
import org.jdesktop.mtgame.Entity;

/**
 * Loads a graph with external references from xml
 * @author Lou Hayt
 */
public class GraphLoader {

    private static final Logger logger = Logger.getLogger(GraphLoader.class.getName());

    private static JAXBContext contextJAXB;

    public static JAXBContext getJAXBContext() {
        return contextJAXB;
    }

    static {
        try {
            contextJAXB = JAXBContext.newInstance("imi.loaders.xml", ColladaRepoComponent.class.getClassLoader());
            System.out.println("created JAXBContext for " + GraphLoader.class.getName());
        } catch (JAXBException ex) {
            Logger.getLogger(Repository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ModelInstance loadGraph(RRL rrl) {
        ModelInstance result = loadGraph(Repository.get().getResource(rrl));
        if (result != null) result.setExternalReference(rrl);
        return result;
    }

    public static ModelInstance loadGraph(URL url) {
        assert (url != null);
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOException while opening a URL stream to load: {0}\n{1}", new Object[] { url, ex });
        }
        ModelInstance result = null;
        if (in != null) result = loadGraph(in); else logger.log(Level.SEVERE, "Failed loading graph from url: {0}", url);
        return result;
    }

    public static ModelInstance loadGraph(InputStream in) {
        assert (in != null);
        XMLNode rootDOM = null;
        try {
            Unmarshaller m = getJAXBContext().createUnmarshaller();
            Object loaded = m.unmarshal(in);
            if (((JAXBElement) loaded).getDeclaredType().equals(XMLNode.class)) {
                JAXBElement<XMLNode> jAXBElement = (JAXBElement<XMLNode>) loaded;
                rootDOM = jAXBElement.getValue();
            } else logger.log(Level.SEVERE, "JAXB loaded an unexcpected object: {0}", ((JAXBElement) loaded).getDeclaredType());
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, "JAXBException! exception:\n{0}", ex);
        }
        ModelInstance result = null;
        if (rootDOM != null) result = loadGraph(rootDOM); else logger.severe("Failed loading the graph");
        return result;
    }

    private static ModelInstance loadGraph(XMLNode rootDOM) {
        ModelInstance root = new ModelInstance(rootDOM.getName());
        populateNode(root, rootDOM);
        root.flattenHierarchy();
        final Entity entity = new Entity(rootDOM.getName());
        final FastList<ModelInstance> kidsToRemove = new FastList<ModelInstance>();
        TreeTraverser.depthFirstPost(root, new NodeProcessor() {

            public boolean processNode(PNode currentNode) {
                if (currentNode instanceof ModelInstance) {
                    ModelInstance model = (ModelInstance) currentNode;
                    if (model.getEntity() != null) {
                        model.getTransform().getLocalMatrix().set(model.getTransform().getWorldMatrix());
                        model.getTransform().getWorldMatrix().setIdentity();
                        kidsToRemove.add(model);
                        entity.addEntity(model.getEntity());
                    }
                }
                return true;
            }
        });
        for (ModelInstance kidToRemove : kidsToRemove) kidToRemove.getParent().removeChild(kidToRemove);
        root.setEntity(entity);
        return root;
    }

    private static void populateNode(PNode node, XMLNode nodeDOM) {
        node.setName(nodeDOM.getName());
        node.setRenderStop(nodeDOM.isRenderStop());
        XMLMatrix mat = nodeDOM.getLocalMatrix();
        if (mat != null) node.setTransform(new PTransform(createMatrix(mat)));
        XMLNodeCollection children = nodeDOM.getChildren();
        if (children != null) {
            for (XMLNode kidNodeDOM : children.getNodeEntry()) {
                if (kidNodeDOM instanceof XMLRefNode) {
                    XMLRefNode kidRefNodeDOM = (XMLRefNode) kidNodeDOM;
                    String ref = kidRefNodeDOM.getExternalReference();
                    RRL rrl = new RRL(ref);
                    ModelInstance refNode = CosmicLoader.load(rrl);
                    if (refNode != null) {
                        populateNode(refNode, kidRefNodeDOM);
                        node.addChild(refNode);
                    } else logger.log(Level.SEVERE, "External reference node failed to load: {0}, parent name: {1} reference: {2}", new Object[] { kidRefNodeDOM.getName(), node.getName(), rrl });
                } else {
                    PNode kidNode = new PNode();
                    populateNode(kidNode, kidNodeDOM);
                    node.addChild(kidNode);
                }
            }
        }
    }

    private static PMatrix createMatrix(XMLMatrix matDOM) {
        assert (matDOM != null);
        float[] fArray = new float[16];
        XMLFloatColumn floatsColumn = matDOM.getColumnOne();
        if (floatsColumn != null) {
            fArray[0] = floatsColumn.getX();
            fArray[4] = floatsColumn.getY();
            fArray[8] = floatsColumn.getZ();
            fArray[12] = floatsColumn.getW();
        } else {
            fArray[0] = 1.0f;
            fArray[4] = 0.0f;
            fArray[8] = 0.0f;
            fArray[12] = 0.0f;
        }
        floatsColumn = matDOM.getColumnTwo();
        if (floatsColumn != null) {
            fArray[1] = floatsColumn.getX();
            fArray[5] = floatsColumn.getY();
            fArray[9] = floatsColumn.getZ();
            fArray[13] = floatsColumn.getW();
        } else {
            fArray[1] = 0.0f;
            fArray[5] = 1.0f;
            fArray[9] = 0.0f;
            fArray[13] = 0.0f;
        }
        floatsColumn = matDOM.getColumnThree();
        if (floatsColumn != null) {
            fArray[2] = floatsColumn.getX();
            fArray[6] = floatsColumn.getY();
            fArray[10] = floatsColumn.getZ();
            fArray[14] = floatsColumn.getW();
        } else {
            fArray[2] = 0.0f;
            fArray[6] = 0.0f;
            fArray[10] = 1.0f;
            fArray[14] = 0.0f;
        }
        floatsColumn = matDOM.getColumnFour();
        if (floatsColumn != null) {
            fArray[3] = floatsColumn.getX();
            fArray[7] = floatsColumn.getY();
            fArray[11] = floatsColumn.getZ();
            fArray[15] = floatsColumn.getW();
        } else {
            fArray[3] = 0.0f;
            fArray[7] = 0.0f;
            fArray[11] = 0.0f;
            fArray[15] = 1.0f;
        }
        PMatrix mat = new PMatrix(fArray);
        return mat;
    }
}
