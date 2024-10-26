package org.xith3d.loaders.models.impl.ac3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.jagatoo.loaders.IncorrectFormatException;
import org.jagatoo.loaders.ParsingErrorException;
import org.jagatoo.loaders.models.ac3d.AC3DModelPrototype;
import org.jagatoo.loaders.models.ac3d.AC3DPrototypeLoader;
import org.xith3d.loaders.models.base.ModelLoader;

/**
 * A loader to create Xith3D geometry from an AC3D file.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class AC3DLoader extends ModelLoader {

    protected static Boolean debug = false;

    public static final String STANDARD_MODEL_FILE_EXTENSION = "ac";

    private static AC3DLoader singletonInstance = null;

    public static void setDebugEnabled(boolean enabled) {
        AC3DLoader.debug = enabled;
    }

    public static boolean isDebugEnabled() {
        return (AC3DLoader.debug);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canLoad(String ext) {
        if (ext.startsWith(".")) return (STANDARD_MODEL_FILE_EXTENSION.equals(ext.substring(1))); else return (STANDARD_MODEL_FILE_EXTENSION.equals(ext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        try {
            if (getBasePath() != null) {
                setBaseURL(new File(getBasePath()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw (fileNotFoundException);
        }
        AC3DModelPrototype prototype = AC3DPrototypeLoader.load(in, getBaseURL());
        AC3DModel model = new AC3DModel(prototype, getFlags());
        AC3DConverter.convert(prototype, model, getFlags());
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AC3DModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        AC3DModel model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AC3DModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        AC3DModel model = loadModel(new FileInputStream(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AC3DModel) super.loadModel(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        try {
            if (getBasePath() != null) {
                setBaseURL(new File(getBasePath()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw (fileNotFoundException);
        }
        AC3DModelPrototype prototype = AC3DPrototypeLoader.load(in, getBaseURL());
        AC3DScene scene = new AC3DScene(prototype);
        AC3DConverter.convert(prototype, scene, getFlags());
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        AC3DScene scene = loadScene(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AC3DScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        AC3DScene scene = loadScene(new FileInputStream(filename));
        if (basePathWasNull) {
            popBasePath();
        }
        return (scene);
    }

    /**
     * Constructs a Loader with the specified baseURL, basePath and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public AC3DLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public AC3DLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public AC3DLoader(URL baseURL) {
        super(baseURL, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public AC3DLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public AC3DLoader(String basePath) {
        super(basePath, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public AC3DLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public AC3DLoader() {
        super(ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static AC3DLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new AC3DLoader();
        return (singletonInstance);
    }
}
