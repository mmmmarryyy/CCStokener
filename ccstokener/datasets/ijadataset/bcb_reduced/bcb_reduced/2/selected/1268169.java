package org.xith3d.loaders.models.impl.cal3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.xith3d.loaders.models.impl.cal3d.core.CalCoreAnimation;
import org.xith3d.loaders.models.impl.cal3d.core.CalLoader;
import org.xith3d.loaders.models.impl.cal3d.core.CalModel;
import org.xith3d.loaders.models.impl.cal3d.loader.KCal3dLoader;
import org.xith3d.loaders.models.impl.cal3d.loader.KCal3dDefinition.Cal3dModelDef;
import org.xith3d.loaders.models.util.precomputed.Animation;
import org.xith3d.loaders.models.util.precomputed.PrecomputedAnimatedModel;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.LoadedGraph;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.scenegraph.Node;
import org.xith3d.scenegraph.Shape3D;
import org.xith3d.scenegraph.SharedGroup;
import org.xith3d.scenegraph.TransformGroup;

/**
 * @author Dave LLoyd
 * @author kman
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 */
public class Cal3dLoader extends ModelLoader {

    /** Flag for the loader to rotate X to Y axis. */
    public static final int LOADER_ROTATE_X_AXIS = 128;

    /** Flag for the loader to invert the V texture coord. */
    public static final int LOADER_INVERT_V_COORD = 256;

    private static final float DEFAULT_FPS = 25f;

    private static Map<String, PrecomputedAnimatedModel> precompCache = new Hashtable<String, PrecomputedAnimatedModel>();

    public static final String STANDARD_MODEL_FILE_EXTENSION = "cfg";

    private static Cal3dLoader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("png");
    }

    private void loadCal3dModel(URL baseURL, String name, Reader definition, LoadedGraph<TransformGroup> graph) {
        if (getFlag(LOADER_ROTATE_X_AXIS)) CalLoader.setLoadingMode(CalLoader.LOADER_ROTATE_X_AXIS); else if (getFlag(LOADER_INVERT_V_COORD)) CalLoader.setLoadingMode(CalLoader.LOADER_INVERT_V_COORD);
        Cal3dModelDef modelDef = KCal3dLoader.loadCfg(baseURL, definition);
        CalModel calModel = KCal3dLoader.getCalModel(baseURL, name, modelDef);
        if (graph instanceof Cal3dModel) ((Cal3dModel) graph).init(name, calModel); else if (graph instanceof Cal3dScene) ((Cal3dScene) graph).init(name, calModel);
    }

    /**
     * Loads a model with precomputed animations.
     * 
     * @param url The URL to load from
     * @param framesPerSecond Frames per second
     * 
     * @return the loaded PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    private PrecomputedAnimatedModel loadPrecomputedModel(Reader reader, String name, float framesPerSecond) throws IncorrectFormatException, ParsingErrorException, IOException {
        if ((name != null) && (precompCache.containsKey(name))) {
            reader.close();
            return (precompCache.get(name).copy());
        }
        TextureLoader.getInstance().getTexture("");
        List<SharedGroup> frames = new ArrayList<SharedGroup>();
        Map<String, Animation> animations = new Hashtable<String, Animation>();
        Cal3dModel model = loadModel(reader);
        int totalFrameCount = 0;
        for (CalCoreAnimation calAnim : model.getInternalModel().getCoreModel().getCoreAnimations().values()) {
            int animFrameCount = (int) (calAnim.getDuration() * framesPerSecond);
            Animation anim = new Animation(calAnim.getName(), totalFrameCount, totalFrameCount + animFrameCount);
            animations.put(anim.name, anim);
            for (int i = 0; i < animFrameCount; i++) {
                model.getInternalModel().getMixer().clearAllAnims();
                model.getInternalModel().getMixer().scrubToPosition(calAnim.getName(), (float) i / (float) animFrameCount);
                model.getCalController().update(1000f);
                SharedGroup frame = new SharedGroup();
                for (Node n : model.getChildren()) {
                    if (n instanceof Cal3dSubmesh) {
                        Shape3D shape = ((Cal3dSubmesh) n).getShape3D();
                        frame.addChild(shape);
                    }
                }
                frames.add(frame);
            }
            totalFrameCount += animFrameCount;
        }
        PrecomputedAnimatedModel precompModel = new PrecomputedAnimatedModel(frames, animations);
        if (name != null) {
            precompCache.put(name, precompModel);
        }
        return (precompModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        if (!(reader instanceof BufferedReader)) reader = new BufferedReader(reader);
        try {
            if (getBasePath() != null) {
                setBaseURL(new File(getBasePath()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw (fileNotFoundException);
        }
        Cal3dModel model = new Cal3dModel(getFlags());
        loadCal3dModel(getBaseURL(), null, reader, model);
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        if ((getBaseURL() == null) && (getBasePath() == null)) throw (new RuntimeException("Neither baseURL nor basePath are set."));
        return ((Cal3dModel) super.loadModel(reader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(new InputStreamReader(in), skin));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((Cal3dModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        Cal3dModel model = new Cal3dModel(getFlags());
        loadCal3dModel(getBaseURL(), url.toExternalForm(), new InputStreamReader(url.openStream()), model);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((Cal3dModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        Cal3dModel model = new Cal3dModel(getFlags());
        URL baseURL = getBaseURL();
        if (baseURL == null) baseURL = new File(getBasePath()).toURI().toURL();
        loadCal3dModel(baseURL, filename, new FileReader(filename), model);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((Cal3dModel) super.loadModel(filename));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param reader the Reader to load from
     * @param framesPerSecond the number of frames per second, usually 25
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(Reader reader, float framesPerSecond) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(reader, null, framesPerSecond));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param reader the Reader to load from
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(Reader reader) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(reader, DEFAULT_FPS));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param in the InputStream to load from
     * @param framesPerSecond the number of frames per second, usually 25
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(InputStream in, float framesPerSecond) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(new InputStreamReader(in), null, framesPerSecond));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param in the InputStream to load from
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(InputStream in) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(in, DEFAULT_FPS));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param url the URL to load from
     * @param framesPerSecond the number of frames per second, usually 25
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(URL url, float framesPerSecond) throws IncorrectFormatException, ParsingErrorException, IOException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        PrecomputedAnimatedModel model = loadPrecomputedModel(new InputStreamReader(url.openStream()), url.toExternalForm(), framesPerSecond);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param url the URL to load from
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(URL url) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(url, DEFAULT_FPS));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param fileName The file to load
     * @param framesPerSecond the number of frames per second, usually 25
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(String fileName, float framesPerSecond) throws IncorrectFormatException, ParsingErrorException, IOException {
        URL url = new File(fileName).toURI().toURL();
        return (loadPrecomputedModel(url, framesPerSecond));
    }

    /**
     * Loads and precomputes a Cal3D model (with shared data optimization)
     * at 25 frames/second
     * 
     * @param fileName the file to load
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(String fileName) throws IncorrectFormatException, ParsingErrorException, IOException {
        return (loadPrecomputedModel(fileName, DEFAULT_FPS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        if (!(reader instanceof BufferedReader)) reader = new BufferedReader(reader);
        try {
            if (getBasePath() != null) {
                setBaseURL(new File(getBasePath()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw (fileNotFoundException);
        }
        Cal3dScene scene = new Cal3dScene();
        loadCal3dModel(getBaseURL(), null, reader, scene);
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new InputStreamReader(in)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        Cal3dScene scene = new Cal3dScene();
        loadCal3dModel(getBaseURL(), url.toExternalForm(), new InputStreamReader(url.openStream()), scene);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cal3dScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        Cal3dScene scene = new Cal3dScene();
        URL baseURL = getBaseURL();
        if (baseURL == null) baseURL = new File(getBasePath()).toURI().toURL();
        loadCal3dModel(baseURL, filename, new FileReader(filename), scene);
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
    public Cal3dLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public Cal3dLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public Cal3dLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public Cal3dLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public Cal3dLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public Cal3dLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public Cal3dLoader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static Cal3dLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new Cal3dLoader();
        return (singletonInstance);
    }
}
