package ch.sahits.hudson.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import ch.sahits.test.PropertyFileLoader;

/**
 * This Parser reads the build properties and retrieves the values in an
 * object oriented way
 * @author Andi Hotz, Sahits GmbH
 */
public class BuildPropertyParser extends PropertyFileLoader {

    /** Source path property */
    protected static final String SOURCE = "source..";

    /** Binary path property */
    protected static final String BINARY = "output..";

    /** Binary include property */
    protected static final String BINARY_INCLUDE = "bin.includes";

    /** Binary exclude property */
    protected static final String BINARY_EXCLUDE = "bin.excludes";

    /** Source include property */
    protected static final String SOURCE_INCLUDE = "src.includes";

    /** Source exclude property */
    protected static final String SOURCE_EXCLUDE = "src.excludes";

    private final String srcPath;

    private final String binpath;

    private final List<String> binInclude;

    private final List<String> binExclude;

    private final List<String> srcInclude;

    private final List<String> srcExclude;

    private final Properties buildProperties;

    /**
	 * Initialize the buld property loader
	 * @param buildPropertyFile
	 */
    public BuildPropertyParser(String buildPropertyFile) {
        super();
        if (buildPropertyFile == null) {
            throw new NullPointerException("The path tho the build properties may not be null");
        }
        this.buildProperties = loadFile(buildPropertyFile);
        String basePath = buildPropertyFile.substring(0, buildPropertyFile.lastIndexOf(File.separator) + 1);
        String s = buildProperties.getProperty(SOURCE);
        if (s == null) {
            srcPath = null;
        } else {
            srcPath = basePath + s;
        }
        s = buildProperties.getProperty(BINARY);
        if (s == null) {
            binpath = null;
        } else {
            binpath = basePath + s;
        }
        srcExclude = convert(buildProperties.getProperty(SOURCE_EXCLUDE));
        srcInclude = convert(buildProperties.getProperty(SOURCE_INCLUDE));
        binExclude = convert(buildProperties.getProperty(BINARY_EXCLUDE));
        binInclude = convert(buildProperties.getProperty(BINARY_INCLUDE));
    }

    /**
	 * Initialize the buld property loader
	 * The property file is expected to be in the same directory under the name of 'build.properties'
	 */
    public BuildPropertyParser() {
        super();
        String buildPropertyFile = "build.properties";
        this.buildProperties = loadFile(buildPropertyFile);
        String basePath = buildPropertyFile.substring(0, buildPropertyFile.lastIndexOf(File.separator) + 1);
        String s = buildProperties.getProperty(SOURCE);
        if (s == null) {
            srcPath = null;
        } else {
            srcPath = basePath + s;
        }
        s = buildProperties.getProperty(BINARY);
        if (s == null) {
            binpath = null;
        } else {
            binpath = basePath + s;
        }
        srcExclude = convert(buildProperties.getProperty(SOURCE_EXCLUDE));
        srcInclude = convert(buildProperties.getProperty(SOURCE_INCLUDE));
        binExclude = convert(buildProperties.getProperty(BINARY_EXCLUDE));
        binInclude = convert(buildProperties.getProperty(BINARY_INCLUDE));
    }

    /**
	 * Load the properties from the specified file.
	 * This method is inteded to be called from within the
	 * constructor of the subclass
	 * @param fileName file name of the property file
	 * @return Instance of the property file read from the system or an
	 * empty property file if an error occured during load.
	 */
    @Override
    protected Properties loadFile(String fileName) {
        Properties prop = new Properties();
        try {
            URL url0 = new File(fileName).toURI().toURL();
            final InputStream input = url0.openStream();
            prop.load(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    /**
	 * Convert a String seperated by spaces into a String List
	 * @param value String
	 * @return List of seperated values
	 */
    private List<String> convert(String value) {
        if (value == null) {
            return Arrays.asList(new String[0]);
        }
        if (value.trim().equals("")) {
            return Arrays.asList(new String[0]);
        }
        String[] parts = value.split(",");
        return Arrays.asList(parts);
    }

    /**
	 * @return the srcPath
	 */
    public String getSrcPath() {
        return srcPath;
    }

    /**
	 * @return the binpath
	 */
    public String getBinpath() {
        return binpath;
    }

    /**
	 * @return the binInclude
	 */
    public List<String> getBinInclude() {
        return binInclude;
    }

    /**
	 * @return the binExclude
	 */
    public List<String> getBinExclude() {
        return binExclude;
    }

    /**
	 * @return the srcInclude
	 */
    public List<String> getSrcInclude() {
        return srcInclude;
    }

    /**
	 * @return the srcExclude
	 */
    public List<String> getSrcExclude() {
        return srcExclude;
    }
}
