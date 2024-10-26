package org.xith3d.loaders.models.impl.dae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.collada._2005._11.colladaschema.COLLADAType;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.LoadedGraph;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.impl.dae.collada.ColladaConverter;
import org.xith3d.loaders.models.impl.dae.collada.ColladaLoader;
import org.xith3d.loaders.models.impl.dae.misc.NullArgumentException;
import org.xith3d.loaders.models.impl.dae.xith.XithToXml;
import org.xith3d.scenegraph.SceneGraphObject;

/*********************************************************************
     * Xith Loader for COLLADA DAE files.
     * 
     * @version
     *   $Id: DaeLoader.java 851 2006-11-27 21:23:55 +0000 (Mo, 27 Nov 2006) Qudus $
     * @since
     *   2005-04-22
     * @author
     *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
     * @author
     *   Marvin Froehlich (aka Qudus)
     *********************************************************************/
public final class DaeLoader extends ModelLoader {

    public static final String STANDARD_MODEL_FILE_EXTENSION = "dae";

    private static DaeLoader singletonInstance = null;

    public static final String AUTHORING_TOOL = "Xith Whoola DaeLoader";

    public static final String INFO = "Xith Whoola DaeLoader\n" + "http://www.xith.org/\n" + "Provide COLLADA filename or URL as command-line argument.\n";

    public static void loadAndPrint(String fileOrUrlName) throws IOException {
        final DaeLoader xithColladaLoader = new DaeLoader();
        DaeScene scene = null;
        if (fileOrUrlName.startsWith("http://")) {
            scene = xithColladaLoader.loadScene(new URL(fileOrUrlName));
        } else {
            scene = xithColladaLoader.loadScene(fileOrUrlName);
        }
        XithToXml.println(scene);
    }

    public static DaeScene loadFromString(String colladaString, URL baseURL) throws IOException {
        final DaeLoader daeLoader = new DaeLoader(ModelLoader.LOAD_ALL);
        daeLoader.setBaseURL(baseURL);
        return daeLoader.loadScene(new StringReader(colladaString));
    }

    /*********************************************************************
     * Loads a scene using COLLADA as the intermediary.
     * 
     * This will first convert non-COLLADA file formats to COLLADA if
     * necessary before importing to Xith.
     *********************************************************************/
    public static DaeScene loadViaCollada(URL sceneURL) throws IOException {
        DaeScene scene = new DaeScene();
        DaeImporter.importColladaScene(ColladaConverter.convert(sceneURL, AUTHORING_TOOL), sceneURL, scene);
        return scene;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    protected String getDefaultSkinExtension() {
        return ("");
    }

    /**
      * Constructs a Loader with the specified baseURL, basePath and flags word.
      * 
      * @param baseURL the new baseURL to take resources from
      * @param basePath the new basePath to take resources from
      * @param flags the flags for the loader
      */
    public DaeLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /*********************************************************************
      * Constructs a Loader with the specified baseURL and flags word.
      * 
      * @param baseURL the new baseURL to take resources from
      * @param flags the flags for the loader
      *********************************************************************/
    public DaeLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /*********************************************************************
      * Constructs a Loader with the specified baseURL and flags word.
      * 
      * @param baseURL the new baseURL to take resources from
      *********************************************************************/
    public DaeLoader(URL baseURL) {
        super(baseURL);
    }

    /*********************************************************************
      * Constructs a Loader with the specified basePath and flags word.
      * 
      * @param basePath the new basePath to take resources from
      * @param flags the flags for the loader
      *********************************************************************/
    public DaeLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /*********************************************************************
      * Constructs a Loader with the specified basePath and flags word.
      * 
      * @param basePath the new basePath to take resources from
      *********************************************************************/
    public DaeLoader(String basePath) {
        super(basePath);
    }

    /*********************************************************************
      * Constructs a Loader with the specified flags word.
      * 
      * @param flags the flags for the loader
      *********************************************************************/
    public DaeLoader(int flags) {
        super(flags);
    }

    /*********************************************************************
      * Constructs a Loader with default values for all variables.
      *********************************************************************/
    public DaeLoader() {
        super();
    }

    /*********************************************************************
      * If you decide to use the Loader as a singleton, here is the method to
      * get the instance from.
      * 
      * @return a singleton instance of the Loader
      *********************************************************************/
    public static DaeLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new DaeLoader();
        return (singletonInstance);
    }

    /**
      * {@inheritDoc}
      */
    private void loadGraph(Reader reader, LoadedGraph<SceneGraphObject> graph) throws IOException, IncorrectFormatException, ParsingErrorException {
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
        try {
            createScene(new ColladaLoader().load(reader), getBaseURL(), graph);
        } catch (Exception e) {
            ParsingErrorException parsingErrorException = new ParsingErrorException(e.getMessage());
            parsingErrorException.initCause(e);
            throw (parsingErrorException);
        }
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(reader);
        DaeModel model = new DaeModel();
        loadGraph(reader, model);
        return model;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(reader);
        return (DaeModel) super.loadModel(reader);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(new InputStreamReader(in)));
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(in);
        return (DaeModel) super.loadModel(in);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        DaeModel scene = loadModel(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        return (DaeModel) super.loadModel(url);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        DaeModel scene = loadModel(new FileReader(filename));
        if (basePathWasNull) {
            popBasePath();
        }
        return (scene);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        return (DaeModel) super.loadModel(filename);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(reader);
        DaeScene scene = new DaeScene();
        loadGraph(reader, scene);
        return scene;
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new InputStreamReader(in)));
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(url);
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        DaeScene scene = loadScene(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
      * {@inheritDoc}
      */
    @Override
    public DaeScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        NullArgumentException.check(filename);
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        DaeScene scene = loadScene(new FileReader(filename));
        if (basePathWasNull) {
            popBasePath();
        }
        return (scene);
    }

    private void createScene(COLLADAType colladaType, URL baseUrl, LoadedGraph<SceneGraphObject> graph) {
        DaeImporter.importColladaScene(colladaType, baseUrl, graph);
    }
}
