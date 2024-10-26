package ch.sahits.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * This utility is a super class for all test classes
 * that read configurations from a property file that
 * lies in the same package.
 * @author Andi Hotz, Sahits GmbH
 */
public class PropertyFileLoader {

    private boolean mavenBuild = true;

    /**
	 * Default constructor initializing this instance for a maven build environment
	 */
    public PropertyFileLoader() {
    }

    /**
	 * Constructor to initialize the property file loader for a specific source folder setup.
	 * Supported are the eclipse default setup (src folder) and the maven setup (src/test/resources).
	 * The maven setup is default.
	 * @param maven true if the maven setup is choosen
	 */
    public PropertyFileLoader(boolean maven) {
        mavenBuild = maven;
    }

    /**
	 * Load the properties from the specified file.
	 * This method is inteded to be called from within the
	 * constructor of the subclass
	 * @param fileName file name of the property file
	 * @return Instance of the property file read from the system or an
	 * empty property file if an error occured during load.
	 */
    protected Properties loadFile(String fileName) {
        Properties prop = new Properties();
        try {
            String packageName = getClass().getName();
            packageName = packageName.substring(0, packageName.lastIndexOf("."));
            String src = "src";
            if (mavenBuild) {
                src = src + File.separator + "test" + File.separator + "resources";
            }
            packageName = src + File.separator + packageName.replace('.', File.separatorChar);
            packageName += File.separator;
            packageName += fileName;
            URL url0 = new File(packageName).toURI().toURL();
            final InputStream input = url0.openStream();
            prop.load(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }
}
