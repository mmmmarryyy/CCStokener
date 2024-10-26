package org.xith3d.loaders.models.impl.tds;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import org.xith3d.Xith3DDefaults;
import org.xith3d.loaders.models.IncorrectFormatException;
import org.xith3d.loaders.models.ParsingErrorException;
import org.xith3d.loaders.models.base.ModelLoader;
import org.xith3d.loaders.models.impl.tds.internal.ModelContext;
import org.xith3d.loaders.models.impl.tds.internal.TDSFile;
import org.xith3d.utility.general.ReaderInputStream;

/**
 * A loader to bring 3DS models into Xith
 *
 * Based on an Original Code base by: Rycharde Hawkes
 *
 * @author Kevin Glass
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 */
public class TDSLoader extends ModelLoader {

    public static Boolean debug = null;

    public static final int LOAD_INDEXED = 128;

    public static final int GENERATE_MIPMAPS = 256;

    public static void setDebug(boolean debug) {
        TDSLoader.debug = new Boolean(debug);
    }

    public static final String STANDARD_MODEL_FILE_EXTENSION = "3ds";

    private static TDSLoader singletonInstance = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSkinExtension() {
        return ("");
    }

    private ModelContext load(InputStream in) throws IOException {
        if (Xith3DDefaults.getLocalDebug(TDSLoader.debug)) {
            System.out.println("\n******************************************************");
            System.out.println(" TDSLoader Debug Output");
            System.out.println("******************************************************");
        }
        TDSFile file = new TDSFile(in, getFlag(LOAD_INDEXED), getFlag(GENERATE_MIPMAPS));
        try {
            while (true) {
                file.processChunk();
            }
        } catch (IOException e) {
        }
        file.close();
        if (!file.getContext().animationFound) {
            if (Xith3DDefaults.getLocalDebug(TDSLoader.debug)) {
                System.out.println("No key frames found!");
            }
        }
        if (Xith3DDefaults.getLocalDebug(TDSLoader.debug)) {
            System.out.println("\n******************************************************\n");
        }
        return (file.getContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(Reader reader, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadModel(new ReaderInputStream(reader), skin));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((TDSModel) super.loadModel(reader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(InputStream in, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
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
        ModelContext context = load(in);
        return (new TDSModel(context));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((TDSModel) super.loadModel(in));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(URL url, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        TDSModel model = loadModel(url.openStream(), skin);
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((TDSModel) super.loadModel(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(String filename, String skin) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        TDSModel model = loadModel(new FileInputStream(filename), skin);
        if (basePathWasNull) {
            popBasePath();
        }
        return (model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSModel loadModel(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        return ((TDSModel) super.loadModel(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSScene loadScene(Reader reader) throws IOException, IncorrectFormatException, ParsingErrorException {
        return (loadScene(new ReaderInputStream(reader)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSScene loadScene(InputStream in) throws IOException, IncorrectFormatException, ParsingErrorException {
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
        ModelContext context = load(in);
        return (new TDSScene(context));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSScene loadScene(URL url) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean baseURLWasNull = setBaseURLFromModelURL(url);
        TDSScene scene = loadScene(url.openStream());
        if (baseURLWasNull) {
            popBaseURL();
        }
        return (scene);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDSScene loadScene(String filename) throws IOException, IncorrectFormatException, ParsingErrorException {
        boolean basePathWasNull = setBasePathFromModelFile(filename);
        TDSScene scene = loadScene(new FileInputStream(filename));
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
    public TDSLoader(URL baseURL, String basePath, int flags) {
        super(baseURL, basePath, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     * @param flags the flags for the loader
     */
    public TDSLoader(URL baseURL, int flags) {
        super(baseURL, flags);
    }

    /**
     * Constructs a Loader with the specified baseURL and flags word.
     * 
     * @param baseURL the new baseURL to take resources from
     */
    public TDSLoader(URL baseURL) {
        super(baseURL);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     * @param flags the flags for the loader
     */
    public TDSLoader(String basePath, int flags) {
        super(basePath, flags);
    }

    /**
     * Constructs a Loader with the specified basePath and flags word.
     * 
     * @param basePath the new basePath to take resources from
     */
    public TDSLoader(String basePath) {
        super(basePath);
    }

    /**
     * Constructs a Loader with the specified flags word.
     * 
     * @param flags the flags for the loader
     */
    public TDSLoader(int flags) {
        super(flags);
    }

    /**
     * Constructs a Loader with default values for all variables.
     */
    public TDSLoader() {
        super();
    }

    /**
     * If you decide to use the Loader as a singleton, here is the method to
     * get the instance from.
     * 
     * @return a singleton instance of the Loader
     */
    public static TDSLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new TDSLoader();
        return (singletonInstance);
    }
}
