package org.xith3d.loaders.models.impl.md5;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.jagatoo.loaders.IncorrectFormatException;
import org.jagatoo.loaders.ParsingErrorException;
import org.jagatoo.loaders.models.md5.MD5RenderMesh;
import org.jagatoo.loaders.models.md5.animation.MD5Animation;
import org.jagatoo.loaders.models.md5.mesh.MD5Mesh;
import org.jagatoo.loaders.models.md5.mesh.MD5MeshModel;
import org.jagatoo.loaders.models.md5.reader.MD5AnimationReader;
import org.jagatoo.loaders.models.md5.reader.MD5Reader;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.base.Scene;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.loaders.texture.TextureStreamLocator;
import org.xith3d.loaders.texture.TextureStreamLocatorFile;
import org.xith3d.resources.ResourceLocator;

/**
 * A Loader to load Doom 3 (MD5) model files.
 * 
 * @author kman
 * @author Marvin Froehlich (aka Qudus)
 */
public class MD5Loader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "md5";

    private static MD5Loader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("pcx");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canLoad(String ext) {
        if (ext.startsWith(".")) return (STANDARD_MODEL_FILE_EXTENSION.equals(ext.substring(1))); else return (STANDARD_MODEL_FILE_EXTENSION.equals(ext));
    }

    public MD5RenderMesh getRenderMesh(Vector<MD5RenderMesh> renderMeshes, String mName) {
        for (MD5RenderMesh rm : renderMeshes) if (rm.getSubMesh().name.equals(mName)) return (rm);
        return (null);
    }

    private MD5Model loadModel(InputStream in, String modelFileName, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        MD5MeshModel prototype = MD5Reader.readMeshFile(in);
        List<URL> animResources = new ResourceLocator(getBaseURL()).findAllResources("md5anim", true, false);
        HashMap<String, MD5Animation> animsMap = new HashMap<String, MD5Animation>();
        for (URL url : animResources) {
            MD5Animation protoAnim = MD5AnimationReader.readAnimFile(url.openStream());
            final int lastSlash = url.getFile().lastIndexOf('/');
            final String filename;
            if (lastSlash < 0) filename = url.getFile().substring(5); else filename = url.getFile().substring(lastSlash + 1);
            animsMap.put(filename, protoAnim);
        }
        ArrayList<MD5RenderMesh> renderMeshes = new ArrayList<MD5RenderMesh>();
        for (MD5Mesh mesh : prototype.getMeshes()) renderMeshes.add(new MD5RenderMesh(prototype, mesh));
        MD5Model model = new MD5Model(prototype, animsMap, renderMeshes, skin, getFlags());
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(in, null, skin));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD5Model) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        MD5Model model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD5Model) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        TextureStreamLocator tsl = null;
        if ((skin != null) && (new File(skin).exists())) {
            tsl = new TextureStreamLocatorFile(new File(skin).getParentFile());
            TextureLoader.getInstance().addTextureStreamLocator(tsl);
            skin = new File(skin).getName();
        }
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        MD5Model model = loadModel(new FileInputStream(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        if (tsl != null) {
            TextureLoader.getInstance().removeTextureStreamLocator(tsl);
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD5Model loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD5Model) super.loadModel(filename));
    }

    /**
     * An MD2Loader will always load models.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Scene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("An MD2Loader will always load models."));
    }

    /**
     * An MD2Loader will always load models.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Scene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("An MD2Loader will always load models."));
    }

    /**
     * An MD2Loader will always load models.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Scene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("An MD2Loader will always load models."));
    }

    /**
     * Constructs a Loader with the specified baseURL, basePath and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public MD5Loader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public MD5Loader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public MD5Loader(URL baseURL) {
        super(baseURL, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public MD5Loader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public MD5Loader(String basePath) {
        super(basePath, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public MD5Loader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public MD5Loader() {
        super(ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static MD5Loader getInstance() {
        if (singletonInstance == null) singletonInstance = new MD5Loader();
        return (singletonInstance);
    }
}
