package com.yosimite.secishow.obj.loader;

import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The SimpleQuadScene class extends the SimpleQuadObject class adding the
 * ?
 * </p>
 */
abstract class BaseQuadSceneLoader implements Loader {

    protected int flags;

    private static final int MAX_PATH_LENGTH = 1024;

    private String basePath = null;

    private URL baseUrl = null;

    private boolean fromUrl = false;

    /**
	 * Constructor.  Crease Angle set to default of
	 * 44 degrees (see NormalGenerator utility for details).
	 * @param flags The constants from above or from
	 * com.sun.j3d.loaders.Loader, possibly "or'ed" (|) together.
	 */
    public BaseQuadSceneLoader(int flags) {
        setFlags(flags);
    }

    /**
	 * Default constructor.  Crease Angle set to default of
	 * 44 degrees (see NormalGenerator utility for details).  Flags
	 * set to zero (0).
	 */
    public BaseQuadSceneLoader() {
        this(0);
    }

    protected void setBasePathFromFilename(String fileName) {
        StringTokenizer stok = new StringTokenizer(fileName, java.io.File.separator);
        StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);
        if (fileName != null && fileName.startsWith(java.io.File.separator)) sb.append(java.io.File.separator);
        for (int i = stok.countTokens() - 1; i > 0; i--) {
            String a = stok.nextToken();
            sb.append(a);
            sb.append(java.io.File.separator);
        }
        setBasePath(sb.toString());
    }

    /**
	 * The Quad File is loaded from the .quad file specified by
	 * the URL.
	 * To attach the model to your scene, call getSceneGroup() on
	 * the Scene object passed back, and attach the returned
	 * BranchGroup to your scene graph.  
	 */
    private void setBaseUrlFromUrl(URL url) {
        StringTokenizer stok = new StringTokenizer(url.toString(), "/\\", true);
        int tocount = stok.countTokens() - 1;
        StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);
        for (int i = 0; i < tocount; i++) {
            String a = stok.nextToken();
            sb.append(a);
        }
        try {
            baseUrl = new URL(sb.toString());
        } catch (MalformedURLException e) {
            System.err.println("Error setting base URL: " + e.getMessage());
        }
    }

    /**
	 * The file is loaded off of the web.
	 * To attach the model to your scene, call getSceneGroup() on
	 * the Scene object passed back, and attach the returned
	 * BranchGroup to your scene graph.  
	 */
    public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        BufferedReader reader;
        setBaseUrlFromUrl(url);
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
        fromUrl = true;
        return load(reader);
    }

    /**
	 * For a file loaded from a URL, set the URL where associated files
	 * (like material properties files) will be found.
	 * Only needs to be called to set it to a different URL
	 * from that containing the file.
	 */
    public void setBaseUrl(URL url) {
        baseUrl = url;
    }

    /**
	 * Return the URL where files associated with this file (like
	 * material properties files) will be found.
	 */
    public URL getBaseUrl() {
        return baseUrl;
    }

    /**
	 * Set the path where files associated with this file is located.
	 * Only needs to be called to set it to a different directory
	 * from that containing the file.
	 */
    public void setBasePath(String pathName) {
        basePath = pathName;
        if (basePath == null || basePath == "") basePath = "." + java.io.File.separator;
        basePath = basePath.replace('/', java.io.File.separatorChar);
        basePath = basePath.replace('\\', java.io.File.separatorChar);
        if (!basePath.endsWith(java.io.File.separator)) basePath = basePath + java.io.File.separator;
    }

    /**
	 * Return the path where files associated with this file (like material
	 * files) are located.
	 */
    public String getBasePath() {
        return basePath;
    }

    /**
	 * Set parameters for loading the model.
	 * Flags defined in Loader.java are ignored by the this Loader
	 * because the .quad file format doesn't include lights, fog, background,
	 * behaviors, views, or sounds.  However, several flags are defined
	 * specifically for use with the SimpleQuadFileLoader (see above).
	 */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
	 * Get the parameters currently defined for loading the model.
	 * Flags defined in Loader.java are ignored by the SimpleQuadFileLoader
	 * because the .quad file format doesn't include lights, fog, background,
	 * behaviors, views, or sounds.  However, several flags are defined
	 * specifically for use with the SimpleQuadFileLoader (see above).
	 */
    public int getFlags() {
        return flags;
    }

    /**
	 * The File is loaded from the already opened file.
	 * To attach the model to your scene, call getSceneGroup() on
	 * the Scene object passed back, and attach the returned
	 * BranchGroup to your scene graph.  
	 */
    public Scene load(Reader reader) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        QuadFileParser st = new QuadFileParser(reader);
        QuadObject quad = new QuadObject();
        quad.readQuadFile(st);
        return makeScene(quad);
    }

    /**
	 * The Quad File is loaded from the .quad file specified by
	 * the filename.
	 * To attach the model to your scene, call getSceneGroup() on
	 * the Scene object passed back, and attach the returned
	 * BranchGroup to your scene graph.  
	 */
    public Scene load(String filename) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        setBasePathFromFilename(filename);
        Reader reader = new BufferedReader(new FileReader(filename));
        return load(reader);
    }

    protected abstract SceneBase makeScene(QuadObject quad);
}
