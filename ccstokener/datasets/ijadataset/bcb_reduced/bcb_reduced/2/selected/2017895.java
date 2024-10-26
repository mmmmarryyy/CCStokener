package org.xith3d.loaders.models.impl.ase;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.ModelLoader;

/**
 * This Loader loads 3DStudio ASCII files (.ase) into Models or Scenes.
 * 
 * @author William Denniss
 * @author Marvin Froehlich (aka Qudus)
 */
public class AseLoader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "ase";

    private static AseLoader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("");
    }

    /**
     * <p>
     * Flag used with the getTransformGroupTree method, indicates that the
     * returned tree will consist only of the root TransformGroup and one
     * TransformGroup for each GROUP node (from the ASE file).
     * </p>
     * 
     * <p>
     * The difference being that a TransformGroup is usually created for each
     * GEOM node which contains (as a child) the Shape3D of the model data whose
     * coordinates are relative to it's pivot point. A translation is then
     * applied to that TransformGroup to move it into place. Using this flag
     * will cause the said Shape3D consisting of the GEOM node data to be added
     * directly to it's parent TransformGroup and cause it's coordinates to be
     * relative to it's parents pivot point rather than it's own (thus not
     * needing a translation to move it into place). The parent of a GEOM node
     * in this context is either the root of the model as a whole (centered at
     * (0,0,0)) or it's parent GROUP node.
     * </p>
     */
    public static final int TGT_GROUPS_ONLY = 128;

    /**
     * Flag used with the getTransformGroupTree method, indicates that the
     * returned tree will consist only of the root TransformGroup and one
     * TransformGroup for each GEOM node (including those which are members of a
     * GROUP node).
     */
    public static final int TGT_NO_GROUPS = 256;

    /**
     * Flag used with the getTransformGroupTree method, indicates that the
     * translation of the ase node (GROUP or GEOM) to its correct location will
     * be applied directly to it's TransformGroup rather than creating a new
     * TransformGroup just to contain the translation and the nodes
     * TransformGroup. Use this flag to cut down on the number of
     * TransformGroupS created but take caution when applying further
     * translations to the TransformGroup that you don't accidently apply the
     * identity matrix.
     * 
     */
    public static final int TGT_NO_TRANSLATE_TG = 512;

    /**
     * Flag used with the getTransformGroupTree method, causes the x attribute
     * of the orientationAngle vector (which can be passed to
     * getTransformGroupTree) to have 90 degrees added to it. The main use of
     * this flag is to cater for the difference in axis orientation between 3D
     * Studio MAX and Xith3D.
     * 
     */
    public static final int TGT_ROTATE90 = 1024;

    /**
     * Unless this flag is set, the root TransformGroup of the tree which is
     * returned by the getTransformGroupTree method will also be added to the
     * named nodes with the name "Root" for conveniance.
     */
    public static final int TGT_NO_ROOT = 2048;

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        AseReader aseReader = new AseReader(reader);
        AseFile aseFile = new AseFile();
        aseFile.parse(aseReader);
        AseModel model = new AseModel(this, aseFile);
        aseFile.getTransformGroupTree(this.getFlags(), model);
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AseModel) super.loadModel(reader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(new InputStreamReader(in), skin));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AseModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        AseModel model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AseModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        AseModel model = loadModel(new FileReader(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((AseModel) super.loadModel(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        AseReader aseReader = new AseReader(reader);
        AseFile aseFile = new AseFile();
        aseFile.parse(aseReader);
        AseScene scene = new AseScene();
        aseFile.getTransformGroupTree(this.getFlags(), scene);
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new InputStreamReader(in)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        AseScene scene = loadScene(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AseScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        AseScene scene = loadScene(new FileReader(filename));
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
    public AseLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public AseLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public AseLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public AseLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public AseLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public AseLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public AseLoader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static AseLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new AseLoader();
        return (singletonInstance);
    }
}
