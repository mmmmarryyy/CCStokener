package imi.repository;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import imi.loaders.Collada;
import imi.loaders.ColladaLoadingException;
import imi.loaders.LoaderParams;
import imi.repository.CacheBehavior.CachePackageListener;
import imi.repository.Repository.RepositoryComponent;
import imi.repository.RepositoryAsset.WorkOrder;
import imi.scene.PMatrix;
import imi.scene.PScene;
import imi.scene.polygonmodel.ModelInstance;
import imi.scene.polygonmodel.PMeshMaterial;
import imi.scene.polygonmodel.PPolygonMesh;
import imi.scene.utils.PMeshUtils;
import imi.utils.Cosmic;
import imi.utils.FileUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.collada.colladaschema.COLLADA;

/**
 *
 * @author Lou Hayt
 */
public class ColladaRepoComponent implements RepositoryComponent {

    /** Logger ref **/
    private static final Logger logger = Logger.getLogger(Repository.class.getName());

    /** Placeholder PScene in case of loading failure **/
    private PScene placeholderScene = null;

    private final ConcurrentHashMap<RRL, RepositoryAsset> loadedAssets = new ConcurrentHashMap<RRL, RepositoryAsset>();

    /** The unmarshaller used to generate the DOM each time a file is loaded **/
    private javax.xml.bind.Unmarshaller unmarshaller = null;

    private static JAXBContext contextJAXB;

    public static JAXBContext getJAXBContext() {
        return contextJAXB;
    }

    static {
        try {
            contextJAXB = JAXBContext.newInstance("org.collada.colladaschema", ColladaRepoComponent.class.getClassLoader());
            System.out.println("created JAXBContext for " + ColladaRepoComponent.class.getName());
        } catch (JAXBException ex) {
            Logger.getLogger(Repository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean initialize() {
        return true;
    }

    public boolean shutdown() {
        return true;
    }

    public void saveCache(OutputStream out) {
    }

    public void loadCache(RRL resource, CachePackageListener listener) {
    }

    public void clearCache() {
    }

    /**
     * Request to load the specified asset and deliver it to the user on completion.
     * @param asset
     * @param user
     */
    public void loadColladaMT(SharedAsset asset, RepositoryUser user) {
        assert (asset != null);
        RepositoryAsset repoAsset = null;
        boolean found = loadedAssets.containsKey(asset.getLocation());
        if (found) repoAsset = loadedAssets.get(asset.getLocation());
        if (repoAsset == null) {
            repoAsset = new RepositoryAsset(asset.getLocation(), this);
            loadedAssets.put(asset.getLocation(), repoAsset);
            Repository.get().getInstruments().requestStarted(asset);
            Repository.get().submitWork(repoAsset.getWorkOrder(asset, user));
        } else {
            repoAsset.shareAsset(user, asset);
        }
    }

    /**
     * Removes asset data from the repository,
     * will extract the file name and look for a match by that.
     * will clear the cache for that asset.
     * @param path - may be the file name or relative path
     * @return true if found
     */
    public boolean unloadColladaAsset(String path) {
        if (path == null) return false;
        String fileName = FileUtils.getShortFilename(path);
        RRL[] keys = (RRL[]) loadedAssets.keySet().toArray(new RRL[] {});
        RRL foundKey = null;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].getRelativePath().contains(fileName)) {
                foundKey = keys[i];
                break;
            }
        }
        if (foundKey != null) {
            RepositoryAsset asset = loadedAssets.get(foundKey);
            asset.clearCache();
            loadedAssets.remove(foundKey);
            return true;
        }
        return false;
    }

    /**
     * Get a new model instance with a child that is the shared placeholder
     * geometry.
     * @return
     */
    public ModelInstance getPlaceholderModel() {
        ModelInstance model = new ModelInstance("placeholder");
        model.addChild(getPlaceholderGeometry());
        return model;
    }

    /**
     * Returns a reference to the placeholder geometry, please do not modify
     * the returned pscene.
     */
    PScene getPlaceholderGeometry() {
        if (placeholderScene == null) {
            placeholderScene = new PScene("Missing Geometry");
            PPolygonMesh mesh = PMeshUtils.createBox("Placeholder", Vector3f.UNIT_Y, 0.5f, 0.7f, 0.4f, ColorRGBA.green, true, true);
            PMeshMaterial placeholderMaterial = new PMeshMaterial("Placeholder Material");
            placeholderMaterial.setTexture(new RRL(""), 0);
            mesh.setMaterial(placeholderMaterial);
            placeholderScene.addModelInstance(mesh, PMatrix.IDENTITY);
        }
        return placeholderScene;
    }

    public Object getPlaceholderAsset() {
        return getPlaceholderGeometry();
    }

    private static boolean oneErrorBox = true;

    public Object load(RRL location, Object loaderParams, CacheBehavior cache) throws ColladaLoadingException {
        Object data = null;
        if (cache.isCached(location)) {
            CachedItem cachedItem = cache.loadCachedItem(location);
            if (cachedItem != null) data = cachedItem.getLoadedData();
        }
        if (data == null) {
            Collada loader = new Collada();
            if (loaderParams instanceof LoaderParams) loader.applyConfiguration((LoaderParams) loaderParams); else loader.setLoadFlags(true, true, true, false, true);
            boolean zip = false;
            URL url = Repository.get().getResource(location);
            String path = url.toString();
            try {
                url = new URL(path + ".gzip");
                zip = true;
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, "MalformedURLException when trying to make: {0}\n{1}", new Object[] { path, ex });
            }
            boolean tryLoading = true;
            while (tryLoading) {
                try {
                    URLConnection conn = url.openConnection();
                    InputStream in = conn.getInputStream();
                    synchronized (ColladaRepoComponent.getJAXBContext()) {
                        unmarshaller = ColladaRepoComponent.getJAXBContext().createUnmarshaller();
                    }
                    COLLADA collada = null;
                    if (zip) collada = (org.collada.colladaschema.COLLADA) unmarshaller.unmarshal(new GZIPInputStream(in)); else collada = (org.collada.colladaschema.COLLADA) unmarshaller.unmarshal(in);
                    PScene loadingScene = new PScene();
                    loader.load(loadingScene, location, collada);
                    cache.writeToCache(location, loadingScene);
                    data = loadingScene;
                    System.out.println("Loaded from COLLADA: " + location.getRelativePath());
                    tryLoading = false;
                } catch (Exception ex) {
                    if (zip) {
                        zip = false;
                        url = Repository.get().getResource(location);
                        System.out.println("Didn't find gzip for " + url);
                    } else {
                        tryLoading = false;
                        logger.log(Level.SEVERE, "Problem trying to load file: {0}\n{1}", new Object[] { url, ex });
                        if (oneErrorBox) {
                            JOptionPane.showMessageDialog(Cosmic.getOnscreenRenderBuffer().getCanvas(), "Was not able to load collada file", "Error during collada load (error box will only appear once)", JOptionPane.ERROR_MESSAGE, new ImageIcon(Repository.get().getResource(new RRL("assets/textures/imilogo.jpg"))));
                            oneErrorBox = false;
                        }
                    }
                }
            }
        }
        return data;
    }
}
