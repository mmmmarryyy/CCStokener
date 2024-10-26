package org.xith3d.loaders.models.impl.md2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.base.Scene;
import org.xith3d.loaders.models.impl.md2.util.DefaultNodeFactory;
import org.xith3d.loaders.models.impl.md2.util.NodeFactory;
import org.xith3d.loaders.models.impl.md2.util.pcx.PCXLoader;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.loaders.texture.TextureStreamLocator;
import org.xith3d.loaders.texture.TextureStreamLocatorFile;
import org.xith3d.scenegraph.Appearance;
import org.xith3d.scenegraph.GeometryArray;
import org.xith3d.scenegraph.PolygonAttributes;
import org.xith3d.scenegraph.Texture;

/**
 * A Loader to load quake 2 model files
 * 
 * If the texture filename is not provided, assumes that it is the same as the
 * MD2 filename but with the filename extension ".jpg", e.g., example.md2 and
 * example.jpg.
 * 
 * To load from the classpath, use ClassLoader.getResource().
 * 
 * <pre>
 *  Current limitations:
 *  - ignores flags
 * </pre>
 * 
 * @since 2005-08-23
 * 
 * @author Kevin Glass
 * @author <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
 * @author 5parrowhawk
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 */
public class MD2Loader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "md2";

    private static MD2Loader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("pcx");
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            while ((i = in.read()) > -1) {
                byteArrayOutputStream.write(i);
            }
            return (byteArrayOutputStream.toByteArray());
        } finally {
            in.close();
        }
    }

    /**
     * Checks if the specified frame is valid.
     * 
     * @param frameName The name of the frame to check
     * @param filter The list of string to filter the frame against
     * @return <i>true</i>, if this frame should be rendered
     */
    private boolean isFrameValid(String frameName, List filter) {
        if (filter == null) return (true);
        for (int i = 0; i < filter.size(); i++) {
            String f = (String) filter.get(i);
            if (frameName.startsWith(f)) return (true);
        }
        return (false);
    }

    /**
     * Renders the specified frame with the command provided.
     * 
     * @param frame The frame to render
     * @param commandList The commands to use for the render
     * @param app The appearence to apply to the strips
     * @param factory The factory to use to produce the rendered shapes
     * @param bNormals Whether or not to render normal data
     */
    private MD2RenderedFrame render(MD2Frame frame, MD2GLCommandList commandList, NodeFactory factory, boolean bNormals) {
        int format = GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2;
        if (bNormals) format = format | GeometryArray.NORMALS;
        int stripCount = 0;
        int stripVertexCount = 0;
        int fanCount = 0;
        int fanVertexCount = 0;
        for (int i = 0; i < commandList.getCommandSetCount(); i++) {
            MD2GLCommands commands = commandList.getCommandSet(i);
            if (commands.getType() == MD2GLCommands.STRIP) {
                stripVertexCount += commands.getCount();
                stripCount++;
            }
            if (commands.getType() == MD2GLCommands.FAN) {
                fanVertexCount += commands.getCount();
                fanCount++;
            }
        }
        int[] stripCounts = new int[stripCount];
        int stripIndex = 0;
        int[] fanCounts = new int[fanCount];
        int fanIndex = 0;
        for (int i = 0; i < commandList.getCommandSetCount(); i++) {
            MD2GLCommands commands = commandList.getCommandSet(i);
            if (commands.getType() == MD2GLCommands.STRIP) {
                stripCounts[stripIndex] = commands.getCount();
                stripIndex++;
            }
            if (commands.getType() == MD2GLCommands.FAN) {
                fanCounts[fanIndex] = commands.getCount();
                fanIndex++;
            }
        }
        Point3f[] stripArray = new Point3f[stripVertexCount];
        TexCoord2f[] stripTexArray = new TexCoord2f[stripVertexCount];
        Vector3f[] stripNormArray = new Vector3f[stripVertexCount];
        stripIndex = 0;
        Point3f[] fanArray = new Point3f[fanVertexCount];
        TexCoord2f[] fanTexArray = new TexCoord2f[fanVertexCount];
        Vector3f[] fanNormArray = new Vector3f[fanVertexCount];
        fanIndex = 0;
        for (int i = 0; i < commandList.getCommandSetCount(); i++) {
            MD2GLCommands commands = commandList.getCommandSet(i);
            if (commands.getType() == MD2GLCommands.STRIP) {
                for (int j = 0; j < commands.getCount(); j++) {
                    MD2GLCommand command = commands.getCommand(j);
                    stripArray[stripIndex] = frame.getVertex(command.getVertexIndex()).getFloats();
                    stripTexArray[stripIndex] = command.getTextureCoordinates();
                    stripNormArray[stripIndex] = frame.getVertex(command.getVertexIndex()).getNormal();
                    stripIndex++;
                }
            }
            if (commands.getType() == MD2GLCommands.FAN) {
                for (int j = 0; j < commands.getCount(); j++) {
                    MD2GLCommand command = commands.getCommand(j);
                    fanArray[fanIndex] = frame.getVertex(command.getVertexIndex()).getFloats();
                    fanTexArray[fanIndex] = command.getTextureCoordinates();
                    fanNormArray[fanIndex] = frame.getVertex(command.getVertexIndex()).getNormal();
                    fanIndex++;
                }
            }
        }
        return (new MD2RenderedFrame(frame.getName(), fanArray, stripArray, fanTexArray, stripTexArray, bNormals ? fanNormArray : null, bNormals ? stripNormArray : null, fanCounts, stripCounts));
    }

    /**
     * Renders the specified frame with the command provided.
     * 
     * @param frame The frame to render
     * @param commandList The commands to use for the render
     * @param app The appearence to apply to the strips
     * @param factory The factory to use to produce the rendered shapes
     */
    private MD2RenderedFrame render(MD2Frame frame, MD2GLCommandList commandList, NodeFactory factory) {
        return (render(frame, commandList, factory, true));
    }

    private Appearance loadSkin(String skin) throws ParsingErrorException {
        Appearance app = new Appearance();
        try {
            Texture tex = null;
            if ((skin.endsWith(".pcx")) || (skin.endsWith(".PCX"))) {
                InputStream skinStream = null;
                for (TextureStreamLocator tsl : TextureLoader.getInstance().getTextureStreamLocators()) {
                    try {
                        skinStream = tsl.openTextureStream(skin);
                        if (skinStream != null) {
                            skinStream = new BufferedInputStream(skinStream);
                            tex = new PCXLoader(skinStream).getTexture();
                            break;
                        }
                    } catch (Throwable t) {
                    }
                }
                if (tex == null) {
                    try {
                        URL skinURL = new URL(skin);
                        skinStream = new BufferedInputStream(skinURL.openStream());
                        tex = new PCXLoader(skinStream).getTexture();
                    } catch (Throwable t) {
                    }
                }
                if (tex == null) {
                    URL skinURL = null;
                    File skinFile = new File(skin);
                    if (skinFile.exists()) skinURL = skinFile.toURI().toURL(); else if (getBaseURL() != null) skinURL = new URL(getBaseURL(), skin); else if (getBasePath() != null) skinURL = new URL(new File(getBasePath()).toURI().toURL(), skin);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(skinURL.openStream());
                    tex = new PCXLoader(bufferedInputStream).getTexture();
                }
            } else {
                tex = TextureLoader.getInstance().getTexture(skin, false);
                if (tex == TextureLoader.getFallbackTexture()) {
                    throw (new ParsingErrorException("Texture \"" + skin + "\" not found."));
                }
            }
            PolygonAttributes poly = new PolygonAttributes();
            poly.setCullFace(PolygonAttributes.CULL_NONE);
            app.setPolygonAttributes(poly);
            app.setTexture(tex);
        } catch (IOException e) {
            ParsingErrorException pe = new ParsingErrorException(e.getMessage());
            pe.initCause(e);
            throw (pe);
        }
        return (app);
    }

    /**
     * Loads the specified file as an MD2 file with a skin specified as a PCX
     * file.
     * 
     * @param in The file to load
     * @param skin The texture to apply (skin) as PCX
     * @param factory The node factory used to generate the displayable shapes
     * @param filter A list of <code>String</code> to filter the frames
     * @param bNormals Whether or not to load vertex-normal data
     */
    private MD2ModelDefinition loadDefinition(InputStream in, String skin, NodeFactory factory, List filter, boolean bNormals) throws IOException, ParsingErrorException {
        Appearance app = null;
        if (skin != null) app = loadSkin(skin);
        byte[] b = toByteArray(in);
        MD2Header header = new MD2Header(b);
        MD2Frames frames = new MD2Frames(b, header);
        MD2GLCommandList list = new MD2GLCommandList(b, header);
        int frameCount = header.getFrameCount();
        MD2RenderedFrame[] rendered = new MD2RenderedFrame[frameCount];
        for (int i = 0; i < frameCount; i++) {
            if ((isFrameValid(frames.getFrame(i).getName(), filter) || (i == 0))) {
                if (bNormals) {
                    rendered[i] = render(frames.getFrame(i), list, factory);
                } else {
                    rendered[i] = render(frames.getFrame(i), list, factory, false);
                }
            }
        }
        return (new MD2ModelDefinition(this, factory, rendered, app));
    }

    /**
     * MD2 models cannot be read from a Reader.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public MD2Model loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("MD2 models cannot be read from a Reader."));
    }

    /**
     * MD2 models cannot be read from a Reader.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public MD2Model loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("MD2 models cannot be read from a Reader."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD2Model loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        if (!(in instanceof BufferedInputStream)) in = new BufferedInputStream(in);
        MD2ModelDefinition modelDef = loadDefinition(in, skin, new DefaultNodeFactory(), null, true);
        return (modelDef.getInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD2Model loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD2Model) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD2Model loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        MD2Model model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD2Model loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD2Model) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MD2Model loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        TextureStreamLocator tsl = null;
        if ((skin != null) && (!skin.endsWith(".pcx")) && (!skin.endsWith(".PCX")) && (new File(skin).exists())) {
            tsl = new TextureStreamLocatorFile(new File(skin).getParentFile());
            TextureLoader.getInstance().addTextureStreamLocator(tsl);
            skin = new File(skin).getName();
        }
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        MD2Model model = loadModel(new FileInputStream(filename), skin);
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
    public MD2Model loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((MD2Model) super.loadModel(filename));
    }

    /**
     * An MD2Loader will always load models.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Scene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("An MD2Loader will always load models."));
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
    public MD2Loader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public MD2Loader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public MD2Loader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public MD2Loader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public MD2Loader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public MD2Loader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public MD2Loader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static MD2Loader getInstance() {
        if (singletonInstance == null) singletonInstance = new MD2Loader();
        return (singletonInstance);
    }
}
