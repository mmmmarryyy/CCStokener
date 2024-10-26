package org.xith3d.loaders.models.impl.bsp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import org.xith3d.Xith3DDefaults;
import org.xith3d.image.DirectBufferedImage;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.base.Model;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPDirectory;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPFace;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPLeaf;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPNode;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPPlane;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPRawData;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPSubModel;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPVertex;
import org.xith3d.loaders.models.impl.bsp.lumps.BSPVisData;
import org.xith3d.utility.general.ReaderInputStream;

/**
 * Loads the Quake 3 BSP file according to spec.
 * It is not expected that it would be rendered from this data structure,
 * but used to convert into a better format for rendering via Xith3D.
 * 
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus)
 * @author Amos Wenger (aka BlueSky)
 */
public class BSPLoader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "bsp";

    protected static final Boolean DEBUG = null;

    private static BSPLoader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("");
    }

    private BSPVisData readVisData(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kVisData);
        BSPVisData visData = new BSPVisData();
        visData.numOfClusters = file.readInt();
        visData.bytesPerCluster = file.readInt();
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("There are " + visData.numOfClusters + " clusters with " + visData.bytesPerCluster + " bytes of vis data each");
        visData.pBitsets = file.readFully(visData.bytesPerCluster * visData.numOfClusters);
        return (visData);
    }

    private BSPPlane[] readPlanes(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kPlanes);
        int num = file.lumps[BSPDirectory.kPlanes].length / (4 * 4);
        BSPPlane[] planes = new BSPPlane[num];
        for (int i = 0; i < num; i++) {
            planes[i] = new BSPPlane();
            planes[i].normal.x = file.readFloat();
            planes[i].normal.y = file.readFloat();
            planes[i].normal.z = file.readFloat();
            planes[i].d = file.readFloat();
        }
        return (planes);
    }

    private BSPNode[] readNodes(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kNodes);
        int num = file.lumps[BSPDirectory.kNodes].length / (4 * 9);
        BSPNode[] nodes = new BSPNode[num];
        for (int i = 0; i < num; i++) {
            nodes[i] = new BSPNode();
            nodes[i].plane = file.readInt();
            nodes[i].front = file.readInt();
            nodes[i].back = file.readInt();
            nodes[i].mins[0] = file.readInt();
            nodes[i].mins[1] = file.readInt();
            nodes[i].mins[2] = file.readInt();
            nodes[i].maxs[0] = file.readInt();
            nodes[i].maxs[1] = file.readInt();
            nodes[i].maxs[2] = file.readInt();
        }
        return (nodes);
    }

    private BSPSubModel[] readSubModels(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kModels);
        int num = file.lumps[BSPDirectory.kModels].length / (4 * 10);
        BSPSubModel[] subModels = new BSPSubModel[num];
        for (int i = 0; i < num; i++) {
            subModels[i] = new BSPSubModel();
            subModels[i].min[0] = file.readFloat();
            subModels[i].min[1] = file.readFloat();
            subModels[i].min[2] = file.readFloat();
            subModels[i].max[0] = file.readFloat();
            subModels[i].max[1] = file.readFloat();
            subModels[i].max[2] = file.readFloat();
            subModels[i].faceIndex = file.readInt();
            subModels[i].numOfFaces = file.readInt();
            subModels[i].brushIndex = file.readInt();
            subModels[i].numOfBrushes = file.readInt();
        }
        return (subModels);
    }

    private String readEntities(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kEntities);
        int num = file.lumps[BSPDirectory.kEntities].length;
        byte[] ca = file.readFully(num);
        String s = new String(ca);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println(s);
        return (s);
    }

    private DirectBufferedImage[] readLightmaps(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kLightmaps);
        int num = file.lumps[BSPDirectory.kLightmaps].length / (128 * 128 * 3);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " lightmaps");
        DirectBufferedImage[] lightMaps = new DirectBufferedImage[num];
        for (int i = 0; i < num; i++) {
            lightMaps[i] = DirectBufferedImage.getDirectImageRGB(128, 128);
            file.readFully(lightMaps[i].getBackingStore());
        }
        return (lightMaps);
    }

    private String[] readTextures(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kTextures);
        int num = file.lumps[BSPDirectory.kTextures].length / (64 + 2 * 4);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " textures");
        byte[] ca = new byte[64];
        String[] textures = new String[num];
        for (int i = 0; i < num; i++) {
            file.readFully(ca);
            file.readInt();
            file.readInt();
            String s = new String(ca);
            s = s.substring(0, s.indexOf(0));
            textures[i] = s;
            if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println(s);
        }
        return (textures);
    }

    private BSPLeaf[] readLeafs(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kLeafs);
        int num = file.lumps[BSPDirectory.kLeafs].length / (12 * 4);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " leafs");
        BSPLeaf[] leafs = new BSPLeaf[num];
        for (int i = 0; i < num; i++) {
            leafs[i] = new BSPLeaf();
            leafs[i].cluster = file.readInt();
            leafs[i].area = file.readInt();
            leafs[i].mins[0] = file.readInt();
            leafs[i].mins[1] = file.readInt();
            leafs[i].mins[2] = file.readInt();
            leafs[i].maxs[0] = file.readInt();
            leafs[i].maxs[1] = file.readInt();
            leafs[i].maxs[2] = file.readInt();
            leafs[i].leafFace = file.readInt();
            leafs[i].numOfLeafFaces = file.readInt();
            leafs[i].leafBrush = file.readInt();
            leafs[i].numOfLeafBrushes = file.readInt();
        }
        return (leafs);
    }

    private int[] readLeafFaces(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kLeafFaces);
        int num = file.lumps[BSPDirectory.kLeafFaces].length / 4;
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " leaf faces");
        int[] leafFaces = new int[num];
        for (int i = 0; i < num; i++) {
            leafFaces[i] = file.readInt();
        }
        return (leafFaces);
    }

    private int[] readMeshVertices(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kMeshVerts);
        int num = file.lumps[BSPDirectory.kMeshVerts].length / 4;
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " mesh vertices");
        int[] meshVertices = new int[num];
        for (int i = 0; i < num; i++) {
            meshVertices[i] = file.readInt();
        }
        return (meshVertices);
    }

    private BSPVertex[] readVertices(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kVertices);
        int num = file.lumps[BSPDirectory.kVertices].length / (11 * 4);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " vertices");
        BSPVertex[] vertices = new BSPVertex[num];
        for (int i = 0; i < num; i++) {
            vertices[i] = new BSPVertex();
            vertices[i].position.x = file.readFloat();
            vertices[i].position.y = file.readFloat();
            vertices[i].position.z = file.readFloat();
            vertices[i].texCoord.x = file.readFloat();
            vertices[i].texCoord.y = file.readFloat();
            vertices[i].lightTexCoord.x = file.readFloat();
            vertices[i].lightTexCoord.y = file.readFloat();
            vertices[i].normal.x = file.readFloat();
            vertices[i].normal.y = file.readFloat();
            vertices[i].normal.z = file.readFloat();
            int r = file.readByte();
            if (r < 0) r = -r + 127;
            int g = file.readByte();
            if (g < 0) g = -g + 127;
            int b = file.readByte();
            if (b < 0) b = -b + 127;
            int a = file.readByte();
            if (a < 0) a = -a + 127;
            vertices[i].color.x = (float) r / 255f;
            vertices[i].color.y = (float) g / 255f;
            vertices[i].color.z = (float) b / 255f;
            vertices[i].color.w = (float) a / 255f;
        }
        return (vertices);
    }

    private BSPFace[] readFaces(BSPFile file) throws IOException {
        file.seek(BSPDirectory.kFaces);
        int num = file.lumps[BSPDirectory.kFaces].length / (26 * 4);
        if (Xith3DDefaults.getLocalDebug(BSPLoader.DEBUG)) System.out.println("there are " + num + " faces");
        BSPFace[] faces = new BSPFace[num];
        for (int i = 0; i < num; i++) {
            faces[i] = new BSPFace();
            faces[i].textureID = file.readInt();
            faces[i].effect = file.readInt();
            faces[i].type = file.readInt();
            faces[i].vertexIndex = file.readInt();
            faces[i].numOfVerts = file.readInt();
            faces[i].meshVertIndex = file.readInt();
            faces[i].numMeshVerts = file.readInt();
            faces[i].lightmapID = file.readInt();
            faces[i].lMapCorner[0] = file.readInt();
            faces[i].lMapCorner[1] = file.readInt();
            faces[i].lMapSize[0] = file.readInt();
            faces[i].lMapSize[1] = file.readInt();
            faces[i].lMapPos[0] = file.readFloat();
            faces[i].lMapPos[1] = file.readFloat();
            faces[i].lMapPos[2] = file.readFloat();
            faces[i].lMapBitsets[0][0] = file.readFloat();
            faces[i].lMapBitsets[0][1] = file.readFloat();
            faces[i].lMapBitsets[0][2] = file.readFloat();
            faces[i].lMapBitsets[1][0] = file.readFloat();
            faces[i].lMapBitsets[1][1] = file.readFloat();
            faces[i].lMapBitsets[1][2] = file.readFloat();
            faces[i].vNormal[0] = file.readFloat();
            faces[i].vNormal[1] = file.readFloat();
            faces[i].vNormal[2] = file.readFloat();
            faces[i].size[0] = file.readInt();
            faces[i].size[1] = file.readInt();
        }
        return (faces);
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * A BSP loader will always load Scenes. So this method will always throw an
     * UnsupportedOperationException.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public Model<?> loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        throw (new UnsupportedOperationException("A BSP loader will always load Scenes."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BSPScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new ReaderInputStream(reader)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BSPScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        if (!(in instanceof BufferedInputStream)) in = new BufferedInputStream(in);
        BSPFile bspFile = new BSPResource(in);
        BSPRawData rawData = new BSPRawData();
        try {
            rawData.faces = readFaces(bspFile);
            rawData.vertices = readVertices(bspFile);
            rawData.lightMaps = readLightmaps(bspFile);
            rawData.visData = readVisData(bspFile);
            rawData.leafs = readLeafs(bspFile);
            rawData.textureNames = readTextures(bspFile);
            rawData.leafFaces = readLeafFaces(bspFile);
            rawData.meshVertices = readMeshVertices(bspFile);
            rawData.entities = readEntities(bspFile);
            rawData.planes = readPlanes(bspFile);
            rawData.nodes = readNodes(bspFile);
            rawData.subModels = readSubModels(bspFile);
            bspFile.close();
        } catch (IOException e) {
            throw (new ParsingErrorException(e.getMessage()));
        }
        BSPScene scene = new BSPScene();
        BSPConverter converter = new BSPConverter();
        converter.convert(rawData, scene);
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BSPScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        final boolean baseURLWasNull = setBaseURLFromModelURL(url);
        BSPScene scene;
        if (url.getProtocol().equals("file")) {
            try {
                final boolean basePathWasNull = (getBasePath() == null);
                setBasePath(new File(getBaseURL().toURI()).getAbsolutePath());
                scene = loadScene(new File(url.toURI()).getAbsolutePath());
                if (basePathWasNull) popBasePath();
            } catch (URISyntaxException e) {
                final IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw (ioe);
            }
        } else {
            scene = loadScene(url.openStream());
        }
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BSPScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        BSPFile bspFile;
        try {
            bspFile = new BSPFile(filename);
        } catch (IOException e) {
            throw (new FileNotFoundException(e.getMessage()));
        }
        BSPRawData rawData = new BSPRawData();
        try {
            rawData.faces = readFaces(bspFile);
            rawData.vertices = readVertices(bspFile);
            rawData.lightMaps = readLightmaps(bspFile);
            rawData.visData = readVisData(bspFile);
            rawData.leafs = readLeafs(bspFile);
            rawData.textureNames = readTextures(bspFile);
            rawData.leafFaces = readLeafFaces(bspFile);
            rawData.meshVertices = readMeshVertices(bspFile);
            rawData.entities = readEntities(bspFile);
            rawData.planes = readPlanes(bspFile);
            rawData.nodes = readNodes(bspFile);
            rawData.subModels = readSubModels(bspFile);
            bspFile.close();
        } catch (IOException e) {
            throw (new ParsingErrorException(e.getMessage()));
        }
        BSPScene scene = new BSPScene();
        BSPConverter converter = new BSPConverter();
        converter.convert(rawData, scene);
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
    public BSPLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public BSPLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public BSPLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public BSPLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public BSPLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public BSPLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public BSPLoader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static BSPLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new BSPLoader();
        return (singletonInstance);
    }
}
