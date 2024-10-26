package org.xith3d.loaders.models.impl.celshading;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.jagatoo.loaders.IncorrectFormatException;
import org.jagatoo.loaders.ParsingErrorException;
import org.openmali.vecmath2.Point3f;
import org.openmali.vecmath2.TexCoord2f;
import org.openmali.vecmath2.Vector3f;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.base.Scene;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.scenegraph.Texture;
import org.xith3d.scenegraph.AssemblyVertexShader;

/**
 * This Loader loads CelShading models.
 * 
 * @author Abdul Bezrati
 * @author William Denniss
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 */
public class CelShadingLoader extends ModelLoader {

    protected static Boolean debug = false;

    public static final int TGT_ROTATE_X90 = 1024;

    public static final int USE_VERTEX_SHADERS = 2048;

    public static final String STANDARD_MODEL_FILE_EXTENSION = "celshading";

    private static CelShadingLoader singletonInstance = null;

    public static void setDebugEnabled(boolean enabled) {
        CelShadingLoader.debug = enabled;
    }

    public static boolean isDebugEnabled() {
        return (CelShadingLoader.debug);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("png");
    }

    private byte[] readNextFourBytes(InputStream in) throws IOException {
        byte bytes[] = { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() };
        return (bytes);
    }

    private int bytesToInt(byte[] array) {
        return ((array[0] & 0xFF) | (array[1] & 0xFF) << 8 | (array[2] & 0xFF) << 16 | (array[3] & 0xFF) << 24);
    }

    private float bytesToFloat(byte[] array) {
        return (Float.intBitsToFloat(bytesToInt(array)));
    }

    private Point3f readNextVertex(InputStream in) throws IOException {
        return (new Point3f(bytesToFloat(readNextFourBytes(in)), bytesToFloat(readNextFourBytes(in)), bytesToFloat(readNextFourBytes(in))));
    }

    private Vector3f readNextNormal(InputStream in) throws IOException {
        return (new Vector3f(bytesToFloat(readNextFourBytes(in)), bytesToFloat(readNextFourBytes(in)), bytesToFloat(readNextFourBytes(in))));
    }

    /**
     * Used in Vertex Shader mode
     * TODO: replace with ShaderLoader implementation!
     */
    private String getShaderCode(InputStream in) {
        StringBuffer fileContent = new StringBuffer();
        int charByChar = 0;
        try {
            while ((charByChar = in.read()) != -1) fileContent.append((char) charByChar);
            in.close();
        } catch (Exception e) {
            return (null);
        }
        return (fileContent.toString());
    }

    private void load(BufferedInputStream in, CelShadingModel model) throws IOException {
        final int polyNumber = bytesToInt(readNextFourBytes(in));
        CelShadingInternalModel internalModel = new CelShadingInternalModel(polyNumber);
        model.init(internalModel);
        for (int i = 0; i < polyNumber; i++) {
            for (int j = 0; j < 3; j++) {
                internalModel.getNormalData()[i * 3 + j] = readNextNormal(in);
                internalModel.getVertexData()[i * 3 + j] = readNextVertex(in);
                internalModel.getTexCoordData()[i * 3 + j] = new TexCoord2f(0f, 0f);
            }
        }
        Texture shaderTex = TextureLoader.getInstance().getTexture("shadertexture.png", Texture.Format.RGB, Texture.MipmapMode.BASE_LEVEL);
        AssemblyVertexShader vertexProgram = null;
        try {
            vertexProgram = new AssemblyVertexShader(getShaderCode(new URL(getBaseURL(), "vertexprogram.celshading").openStream()));
        } catch (MalformedURLException e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw (e2);
        }
        model.finish(shaderTex, vertexProgram);
        if (getFlag(TGT_ROTATE_X90)) {
            model.getTransform().rotX((float) Math.toRadians(90.0));
        }
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
    public CelShadingModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        if (!(in instanceof BufferedInputStream)) in = new BufferedInputStream(in);
        try {
            if (getBasePath() != null) {
                setBaseURL(new File(getBasePath()).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw (fileNotFoundException);
        }
        CelShadingModel model = new CelShadingModel(getFlags());
        load((BufferedInputStream) in, model);
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelShadingModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((CelShadingModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelShadingModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        CelShadingModel model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelShadingModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((CelShadingModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelShadingModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        CelShadingModel model = loadModel(new FileInputStream(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CelShadingModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((CelShadingModel) super.loadModel(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("Scene loading is not supported for CelShading Models"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("Scene loading is not supported for CelShading Models"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("Scene loading is not supported for CelShading Models"));
    }

    /**
     * Constructs a Loader with the specified baseURL, basePath and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public CelShadingLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public CelShadingLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public CelShadingLoader(URL baseURL) {
        super(baseURL, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public CelShadingLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public CelShadingLoader(String basePath) {
        super(basePath, ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public CelShadingLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public CelShadingLoader() {
        super(ModelLoader.USE_DISPLAY_LISTS);
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static CelShadingLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new CelShadingLoader();
        return (singletonInstance);
    }
}
