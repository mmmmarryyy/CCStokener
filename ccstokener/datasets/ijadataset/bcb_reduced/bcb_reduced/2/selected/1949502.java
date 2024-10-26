package cz.cube.mtheory.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.util.export.binary.BinaryImporter;

/**
 * Utility trida pro nacitani modelu
 * 
 * @author NkD
 */
public class ModelLoader {

    /**
     * Nacte model a vrati ho jako Spatial (Spatial je predek Node i TriMeshe).
     * Convertor pro nacteni modelu je vybran podle pripony souboru v parametru
     * path
     * 
     * @param path
     *            - relativni cesta k souboru modelu. Musi koncit priponou
     *            souboru (priklad path: "/resource/ship/spacecraft.3ds")
     * @return Nacteny model
     */
    public static Spatial loadSpatial(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1);
        return loadSpatial(path, ModelFormat.findConvertor(extension));
    }

    /**
     * Nacte model a vrati ho jako Spatial (Spatial je predek Node i TriMeshe).
     * 
     * @param path
     *            - relativni cesta k souboru modelu.
     * @param modelFormat
     *            - explicitni oznaceni o jako format modelu jde. Podle toho
     *            formatu bude vybran convertor pro nacteni modelu.
     * @return Nacteny model
     */
    public static Spatial loadSpatial(String path, ModelFormat modelFormat) {
        ByteArrayOutputStream BO = new ByteArrayOutputStream();
        Object model = null;
        try {
            URL url = getResource(path);
            modelFormat.converter().setProperty("mtllib", url);
            modelFormat.converter().convert(url.openStream(), BO);
            model = new BinaryImporter().load(new ByteArrayInputStream(BO.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (Spatial) model;
    }

    /**
     * Nacte model a vrati ho jako TriMesh. Convertor pro nacteni modelu je
     * vybran podle pripony souboru v parametru path
     * 
     * @param path
     *            - relativni cesta k souboru modelu. Musi koncit priponou
     *            souboru (priklad path: "/resource/ship/spacecraft.3ds")
     * @return Nacteny model
     */
    public static TriMesh loadTriMesh(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1);
        return loadTriMesh(path, ModelFormat.findConvertor(extension));
    }

    /**
     * Nacte model a vrati ho jako TriMesh.
     * 
     * @param path
     *            - relativni cesta k souboru modelu.
     * @param modelFormat
     *            - explicitni oznaceni o jako format modelu jde. Podle toho
     *            formatu bude vybran convertor pro nacteni modelu.
     * @return Nacteny model
     */
    public static TriMesh loadTriMesh(String path, ModelFormat modelFormat) {
        return unpackTrimesh(loadSpatial(path, modelFormat));
    }

    private static TriMesh unpackTrimesh(Object model) {
        if (model instanceof TriMesh) return (TriMesh) model;
        if (model instanceof Node) {
            List<Spatial> children = ((Node) model).getChildren();
            if (children.size() != 1) throw new RuntimeException("Can not unpack one trimesh from model. Children.size = " + children.size());
            return unpackTrimesh(children.get(0));
        }
        return null;
    }

    private static URL getResource(String path) {
        URL url = ModelLoader.class.getResource(path);
        if (url == null) throw new RuntimeException("Resource '" + path + "' doesnt exist");
        return url;
    }
}
