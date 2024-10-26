package imi.loaders;

import imi.objects.ObjectComponent;
import imi.repository.AssetInitializer;
import imi.repository.RRL;
import imi.repository.Repository;
import imi.repository.SharedAsset;
import imi.scene.PMatrix;
import imi.scene.PNode;
import imi.scene.PScene;
import imi.scene.polygonmodel.ModelInstance;
import imi.scene.polygonmodel.PMeshMaterial;
import imi.scene.utils.PNodeUtils;
import imi.utils.FileUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main cosmic loader
 * @author Lou Hayt
 */
public class CosmicLoader {

    private static int bufferSize = 1024 * 64;

    private static CsgLoader csgLoader = null;

    public static ModelInstance load(RRL rrl) {
        assert (rrl != null);
        String type = FileUtils.getFileExtension(rrl.getRelativePath());
        ModelInstance result = null;
        if (type != null) {
            if (type.equalsIgnoreCase("xml")) result = GraphLoader.loadGraph(rrl); else if (type.equalsIgnoreCase("properties")) result = loadObjectProperties(rrl); else if (type.equalsIgnoreCase("dae")) result = loadCollada(rrl); else if (type.equalsIgnoreCase("csg")) result = loadBinaryCosmicSceneGraph(rrl);
        }
        if (result == null) Logger.getLogger(CosmicLoader.class.getName()).log(Level.SEVERE, "Returning null from CosmicLoader, type: {0} RRL: {1}", new Object[] { type, rrl });
        return result;
    }

    public static ModelInstance loadObjectProperties(RRL rrl) {
        ModelInstance result = null;
        URL url = Repository.get().getResource(rrl);
        Properties props = new Properties();
        try {
            props.load(url.openStream());
        } catch (IOException ex) {
            Logger.getLogger(CosmicLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        String type = props.getProperty("type");
        if (type == null) Logger.getLogger(CosmicLoader.class.getName()).severe("Object type not found in properties"); else {
            Class typeClass = null;
            try {
                typeClass = Class.forName(type);
            } catch (Exception ex) {
                Logger.getLogger(CosmicLoader.class.getName()).log(Level.SEVERE, "Unknown type: {0}\n{1}", new Object[] { type, ex });
            }
            if (typeClass != null) {
                ObjectComponent resultObject = null;
                try {
                    resultObject = (ObjectComponent) typeClass.getConstructor(Properties.class).newInstance(props);
                } catch (Exception ex) {
                    Logger.getLogger(CosmicLoader.class.getName()).log(Level.SEVERE, "Did not instantiate type: {0} due to: {1}", new Object[] { type, ex });
                }
                if (resultObject != null) {
                    result = resultObject.getModelInstance();
                    result.setExternalReference(rrl);
                }
            }
        }
        return result;
    }

    private static ModelInstance loadCollada(RRL rrl) {
        String name = FileUtils.getFileNameWithoutExtension(rrl.getRelativePath());
        LoaderParams params = new LoaderParams.Builder().setLoadGeometry(true).setLoadSkeleton(false).setLoadAnimation(false).setKeepPPolygonMeshData(true).build();
        AssetInitializer init = new AssetInitializer() {

            public boolean initialize(Object asset) {
                PNode root = (PNode) asset;
                PMeshMaterial mat = Repository.get().getDefaultMaterial();
                PNodeUtils.setMaterialOnGraph(mat, root, true, true);
                return true;
            }
        };
        SharedAsset sharedAsset = new SharedAsset(rrl, init, params);
        PScene scene = new PScene();
        ModelInstance result = scene.addModelInstance(name, sharedAsset, new PMatrix());
        result.setExternalReference(rrl);
        return result;
    }

    private static ModelInstance loadBinaryCosmicSceneGraph(RRL rrl) {
        if (csgLoader == null) csgLoader = new CsgLoader();
        URL url = Repository.get().getResource(rrl);
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(url.openStream(), bufferSize));
        } catch (IOException ex) {
            Logger.getLogger(CosmicLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        String name = FileUtils.getFileNameWithoutExtension(rrl.getRelativePath());
        ModelInstance result = new ModelInstance(name);
        result.addChild(csgLoader.loadMain(dis));
        PMeshMaterial mat = Repository.get().getDefaultMaterial();
        PNodeUtils.setMaterialOnGraph(mat, result, true, true);
        result.setExternalReference(rrl);
        return result;
    }
}
