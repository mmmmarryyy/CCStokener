package game.bin.obj;

import game.bin.gamesys.ResourceLocatorLibaryTool;
import game.bin.gamesys.RuntimeCash;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.model.converters.AseToJme;
import com.jmex.model.converters.FormatConverter;
import com.jmex.model.converters.MaxToJme;
import com.jmex.model.converters.Md2ToJme;
import com.jmex.model.converters.Md3ToJme;
import com.jmex.model.converters.MilkToJme;
import com.jmex.model.converters.ObjToJme;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.material.Material;

public class Mesh extends Node {

    private static final Logger logger = Logger.getLogger(Mesh.class.getName());

    private DisplaySystem display = DisplaySystem.getDisplaySystem();

    private DynamicPhysicsNode dynamicphysicsnode;

    private PhysicsSpace physicsspace;

    public Mesh() {
        super("MESH");
        iniMesh();
        logger.info("Mesh " + name + " created.");
    }

    public Mesh(String name) {
        super(name);
        iniMesh();
        logger.info("Mesh " + name + " created.");
    }

    Texture baseMap;

    Texture normalMap;

    Texture specMap;

    MaterialState material_state;

    public void iniMesh() {
        material_state = display.getRenderer().createMaterialState();
        material_state.setColorMaterial(MaterialState.ColorMaterial.AmbientAndDiffuse);
        material_state.setAmbient(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        material_state.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        material_state.setSpecular(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        material_state.setShininess(25.0f);
    }

    public void addModel(String model_file_name, String model_name) {
        logger.info("Add Mesh " + model_name + " to " + name);
        Node loaded_model = new Node(model_name);
        FormatConverter formatConverter = null;
        ByteArrayOutputStream BO = new ByteArrayOutputStream();
        String modelFormat = model_file_name.substring(model_file_name.lastIndexOf(".") + 1, model_file_name.length());
        String modelBinary = model_file_name.substring(0, model_file_name.lastIndexOf(".") + 1) + "jbin";
        URL model_url = ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_MODEL, model_file_name);
        URL model_url_jbin = ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_MODEL, modelBinary);
        if (model_url_jbin == null) {
            logger.info("Create jbin Format" + " (Node_" + name + " - Child_" + model_name + ")");
            if (modelFormat.equals("3ds") || modelFormat.equals("3DS")) {
                formatConverter = new MaxToJme();
            } else if (modelFormat.equals("md2")) {
                formatConverter = new Md2ToJme();
            } else if (modelFormat.equals("md3")) {
                formatConverter = new Md3ToJme();
            } else if (modelFormat.equals("ms3d")) {
                formatConverter = new MilkToJme();
            } else if (modelFormat.equals("ase")) {
                formatConverter = new AseToJme();
            } else if (modelFormat.equals("obj")) {
                formatConverter = new ObjToJme();
            }
            formatConverter.setProperty("mtllib", model_url);
            try {
                formatConverter.convert(model_url.openStream(), BO);
                loaded_model = (Node) BinaryImporter.getInstance().load(new ByteArrayInputStream(BO.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("Found jbin Format" + " (Node_" + name + " - Child_" + model_name + ")");
            try {
                loaded_model = (Node) BinaryImporter.getInstance().load(model_url_jbin.openStream());
            } catch (IOException e) {
            }
        }
        Quaternion temp = new Quaternion();
        temp.fromAngleAxis(FastMath.PI / 2, new Vector3f(-1, 0, 0));
        loaded_model.setLocalRotation(temp);
        loaded_model.setLocalTranslation(new Vector3f(0.0f, 0.0f, 0.0f));
        removeMaterialStates(loaded_model);
        this.attachChild(loaded_model);
        if (loaded_model.getName().equals("TDS Scene")) {
            cleanNode("TDS Scene");
        }
        logger.info("Child_" + model_name + " has been added to Node_" + name);
    }

    public void setLocalTranslation(float x, float z, float y) {
        this.getLocalTranslation().set(x, z, y);
    }

    public void addTexture(String texture_file_name) {
        logger.info(name + " adding texture " + texture_file_name);
        TextureState texture_state = display.getRenderer().createTextureState();
        String texture_name = texture_file_name.substring(0, texture_file_name.lastIndexOf("."));
        String texture_format = texture_file_name.substring(texture_file_name.lastIndexOf("."), texture_file_name.length());
        String texture_file_name_base = texture_name + "_COLOR" + texture_format;
        String texture_file_name_normal = texture_name + "_NRM" + texture_format;
        String texture_file_name_spec = texture_name + "_SPEC" + texture_format;
        baseMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_base), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear);
        baseMap.setWrap(Texture.WrapMode.Repeat);
        texture_state.setTexture(baseMap, 0);
        if (RuntimeCash.getNormalMapping()) {
            normalMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_normal), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, Image.Format.GuessNoCompression, 0.0f, true);
            normalMap.setWrap(Texture.WrapMode.Repeat);
            texture_state.setTexture(normalMap, 1);
            specMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_spec), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear);
            specMap.setWrap(Texture.WrapMode.Repeat);
            texture_state.setTexture(specMap, 2);
        }
        this.setRenderState(texture_state);
        if (RuntimeCash.getNormalMapping()) {
            this.setRenderState(RuntimeCash.getGLSLShaderObjectNormalMap());
        }
        this.setRenderState(material_state);
    }

    public void addTexture(String texture_file_name, String child) {
        logger.info(name + "-" + child + " adding texture " + texture_file_name);
        TextureState texture_state = display.getRenderer().createTextureState();
        String texture_name = texture_file_name.substring(0, texture_file_name.lastIndexOf("."));
        String texture_format = texture_file_name.substring(texture_file_name.lastIndexOf("."), texture_file_name.length());
        String texture_file_name_base = texture_name + "_COLOR" + texture_format;
        String texture_file_name_normal = texture_name + "_NRM" + texture_format;
        String texture_file_name_spec = texture_name + "_SPEC" + texture_format;
        baseMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_base), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear);
        baseMap.setWrap(Texture.WrapMode.Repeat);
        texture_state.setTexture(baseMap, 0);
        if (RuntimeCash.getNormalMapping()) {
            normalMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_normal), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, Image.Format.GuessNoCompression, 0.0f, true);
            normalMap.setWrap(Texture.WrapMode.Repeat);
            texture_state.setTexture(normalMap, 1);
            specMap = TextureManager.loadTexture(ResourceLocatorLibaryTool.locateResource(ResourceLocatorLibaryTool.TYPE_TEXTURE, texture_file_name_spec), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear);
            specMap.setWrap(Texture.WrapMode.Repeat);
            texture_state.setTexture(specMap, 2);
        }
        this.getChild(child).setRenderState(texture_state);
        if (RuntimeCash.getNormalMapping()) {
            this.setRenderState(RuntimeCash.getGLSLShaderObjectNormalMap());
        }
        this.getChild(child).setRenderState(material_state);
    }

    private void removeMaterialStates(Node node) {
        node.clearRenderState(RenderState.RS_MATERIAL);
        if (node.getQuantity() == 0) {
            return;
        }
        List<Spatial> children = node.getChildren();
        for (int i = 0, cSize = children.size(); i < cSize; i++) {
            Spatial child = children.get(i);
            if (child != null) {
                child.clearRenderState(RenderState.RS_MATERIAL);
                if (child instanceof Node) {
                    removeMaterialStates((Node) child);
                } else if (child instanceof SharedMesh) {
                    SharedMesh sharedMesh = (SharedMesh) child;
                    TriMesh t = sharedMesh.getTarget();
                    t.clearRenderState(RenderState.RS_MATERIAL);
                }
            }
        }
    }

    private void cleanNode(String name) {
        Node buffer = (Node) this.getChild(name);
        int x = buffer.getChildren().size();
        for (int i = 0; i < x; i++) {
            Quaternion temp = new Quaternion();
            temp.fromAngleAxis(FastMath.PI / 2, new Vector3f(-1, 0, 0));
            buffer.getChild(0).setLocalRotation(temp);
            this.attachChild(buffer.getChild(0));
        }
        this.detachChildNamed(name);
    }

    public void printNodeChildren() {
        printNodeChildren(this);
    }

    public void printNodeChildren(Node n) {
        if (n == null) {
            System.err.println("printNodeChildren: Node provided was null.");
            return;
        }
        System.out.println("********** Child list begins for node " + n.getName() + " **********");
        for (Spatial s : n.getChildren()) {
            System.out.println(s);
            if (s instanceof Node) printNodeChildren((Node) s);
        }
        System.out.println("********** Node child list ends (" + n.getName() + ") **********");
    }
}
