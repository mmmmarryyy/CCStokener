package org.xith3d.loaders.models.impl.obj;

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
import java.util.StringTokenizer;
import javax.vecmath.Vector3f;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.LoadedGraph;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.util.precomputed.Animation;
import org.xith3d.loaders.models.util.precomputed.PrecomputedAnimatedModel;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.scenegraph.Appearance;
import org.xith3d.scenegraph.Material;
import org.xith3d.scenegraph.SGUtils;
import org.xith3d.scenegraph.SharedGroup;
import org.xith3d.scenegraph.TransformGroup;
import org.xith3d.scenegraph.TriangleArray;

/*********************************************************************
 * A loader to create Xith3D geometry from a Wavefront OBJ file.
 * 
 * To load from the classpath, use ClassLoader.getResource().
 * 
 * @version
 *   $Id: OBJLoader.java 1126 2007-02-01 03:02:10Z qudus $
 * @since
 *   2005-08-17
 *   
 * @author Kevin Glass
 * @author <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 *********************************************************************/
public class OBJLoader extends ModelLoader {

    private static Map<String, PrecomputedAnimatedModel> precompCache = new Hashtable<String, PrecomputedAnimatedModel>();

    public static final String STANDARD_MODEL_FILE_EXTENSION = "obj";

    private static OBJLoader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("mtl");
    }

    private List<SharedGroup> loadOBJFrames(final String baseURL) {
        List<SharedGroup> frames = new ArrayList<SharedGroup>();
        int frameCount = -1;
        for (int i = 1; i < Integer.MAX_VALUE; i += 1) {
            String num = Integer.toString(i);
            final float length = num.length();
            for (int j = 0; j < 6 - length; j++) {
                num = "0" + num;
            }
            final String frameURL = baseURL + "_" + num + ".obj";
            try {
                SharedGroup loaded = new SharedGroup();
                loaded.addChild(loadModel(new URL(frameURL)));
                frames.add(loaded);
                if (SGUtils.findFirstShape(loaded) == null) {
                    System.out.println("Incorrectly loaded file : " + frameURL);
                }
                ((TriangleArray) SGUtils.findFirstShape(loaded).getGeometry()).calculateFaceNormals();
                SGUtils.findFirstShape(loaded).getAppearance().getMaterial().setLightingEnabled(true);
            } catch (final FileNotFoundException e) {
                if (frameCount == -1) {
                    e.printStackTrace();
                }
                return (frames);
            } catch (final IOException e) {
                if (frameCount == -1) {
                    e.printStackTrace();
                }
                return (frames);
            }
            frameCount++;
        }
        return (frames);
    }

    private PrecomputedAnimatedModel loadPrecomputedModel_(URL url) {
        if (precompCache.containsKey(url.toExternalForm())) {
            return (precompCache.get(url.toExternalForm()).copy());
        }
        TextureLoader.getInstance().getTexture("");
        List<SharedGroup> frames = new ArrayList<SharedGroup>();
        Map<String, Animation> animations = new Hashtable<String, Animation>();
        if (url.toExternalForm().endsWith(".amo")) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String objFileName = reader.readLine();
                objFileName = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf("/")) + "/" + objFileName;
                frames = loadOBJFrames(objFileName);
                String line;
                while ((line = reader.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    String animName = tokenizer.nextToken();
                    int from = Integer.valueOf(tokenizer.nextToken());
                    int to = Integer.valueOf(tokenizer.nextToken());
                    tokenizer.nextToken();
                    animations.put(animName, new Animation(animName, from, to));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            frames = loadOBJFrames(url.toExternalForm());
        }
        PrecomputedAnimatedModel precompModel = new PrecomputedAnimatedModel(frames, animations);
        precompCache.put(url.toExternalForm(), precompModel);
        return (precompModel);
    }

    private void load(BufferedReader bufferedReader, NodeFactory factory, Vector3f vec, LoadedGraph<TransformGroup> graph) throws IOException {
        Map<String, Appearance> matMap;
        MaterialLibLoader matLoader = new MaterialLibLoader(getBaseURL(), getBasePath());
        if (graph instanceof OBJScene) matMap = ((OBJScene) graph).getAppearancesMap(); else matMap = ((OBJModel) graph).getAppearancesMap();
        try {
            Appearance currentApp = new Appearance();
            Material temp = new Material();
            temp.setLightingEnabled(true);
            currentApp.setMaterial(temp);
            VertexList verts = new VertexList();
            NormalList normals = new NormalList();
            TexList texs = new TexList();
            OBJGroup topGroup = new OBJGroup(factory, "top");
            OBJGroup currentGroup = topGroup;
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#")) {
                } else if (line.equals("")) {
                } else if (line.startsWith("vn")) {
                    normals.add(line);
                } else if (line.startsWith("vt")) {
                    texs.add(line);
                } else if (line.startsWith("v")) {
                    verts.add(line, vec);
                } else if (line.startsWith("f")) {
                    currentGroup.add(line, currentApp);
                } else if (line.startsWith("g")) {
                    OBJGroup g = new OBJGroup(factory, "");
                    topGroup.addGroup(g);
                    currentGroup = g;
                } else if (line.startsWith("o")) {
                    OBJGroup g = new OBJGroup(factory, line.substring(2));
                    topGroup.addGroup(g);
                    currentGroup = g;
                } else if (line.startsWith("mtllib")) {
                    StringTokenizer tokens = new StringTokenizer(line);
                    tokens.nextToken();
                    String name = tokens.nextToken();
                    List<Appearance> appList = matLoader.parse(name);
                    for (Appearance app : appList) {
                        if (app != null) matMap.put(app.getName(), app);
                    }
                } else if (line.startsWith("usemtl")) {
                    String name = line.substring(line.indexOf(" ") + 1);
                    currentApp = matMap.get(name);
                } else if (line.startsWith("s")) {
                    System.err.println(getClass().getName() + ":  smoothing groups not currently supported:  \"" + line + "\"");
                } else {
                    System.err.println(getClass().getName() + ":  ignoring unknown OBJ tag:  \"" + line + "\"");
                }
            }
            graph.addChild(topGroup.build(verts, texs, normals, graph));
        } finally {
            bufferedReader.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
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
        OBJModel model = new OBJModel();
        load((BufferedReader) reader, new DefaultNodeFactory(), new Vector3f(0f, 0f, 0f), model);
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((OBJModel) super.loadModel(reader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(new InputStreamReader(in), skin));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((OBJModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        OBJModel model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((OBJModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        OBJModel model = loadModel(new FileReader(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((OBJModel) super.loadModel(filename));
    }

    /**
     * Loads and precomputes an OBJ animated model exported with Blender
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
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        PrecomputedAnimatedModel model = loadPrecomputedModel_(url);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * Loads and precomputes an OBJ animated model exported with Blender
     * 
     * @param fileName The file to load
     * 
     * @return a PrecomputedAnimatedModel
     * 
     * @throws IOException 
     * @throws ParsingErrorException 
     * @throws IncorrectFormatException 
     */
    public PrecomputedAnimatedModel loadPrecomputedModel(String fileName) throws IncorrectFormatException, ParsingErrorException, IOException {
        URL url = new File(fileName).toURI().toURL();
        return (loadPrecomputedModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
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
        OBJScene scene = new OBJScene();
        load((BufferedReader) reader, new DefaultNodeFactory(), new Vector3f(0f, 0f, 0f), scene);
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new InputStreamReader(in)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        OBJScene scene = loadScene(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OBJScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        OBJScene scene = loadScene(new FileReader(filename));
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
    public OBJLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public OBJLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public OBJLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public OBJLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public OBJLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public OBJLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public OBJLoader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static OBJLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new OBJLoader();
        return (singletonInstance);
    }
}
