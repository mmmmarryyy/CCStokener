package ro.codemart.installer.packer.ant.impl.nestedjar;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import ro.codemart.installer.packer.ant.AntPacker;
import ro.codemart.installer.packer.ant.PackerConfiguration;
import ro.codemart.installer.packer.ant.util.PackerUtils;
import java.io.*;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.net.URL;

/**
 * Creates a nested jar in the following form : <br>
 * <ul>
 * <li>/lib</li>
 * <li>/main</li>
 * <li>/resources</li>
 * <li>/ro/codemart/installer/packer/... for internally used classes</li>
 * </ul>
 *
 * @author marius.ani
 */
public class NestedJarPacker implements AntPacker {

    private static final String LOG4J_CONFIG_FILE = "/config/log4j.xml";

    /**
     * Creates a jar file with other jar files nested, as well as any other resource and config files.
     * It is used by the ant task specially designed for this job.
     *
     * @param config the configuration object holding reference to the files that will be packed
     * @see ro.codemart.installer.packer.ant.PackerTask#execute
     */
    public void pack(PackerConfiguration config) {
        FileOutputStream fos = null;
        JarOutputStream jarOutputStream = null;
        try {
            File destFile = new File(config.getProject().getBaseDir(), config.getDestfile() + ".jar");
            fos = new FileOutputStream(destFile);
            Manifest manifest = createManifest(config);
            jarOutputStream = new JarOutputStream(fos, manifest);
            packResources(config.getLibrariesFileSet(), "lib", jarOutputStream);
            packResources(config.getResourcesFileSet(), "resources", jarOutputStream);
            packLog4jConfigFile(config, jarOutputStream);
            packInternallyUsedFiles(jarOutputStream, config);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (jarOutputStream != null) {
                try {
                    jarOutputStream.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Packs the user specified log4j config file, or the default one if the user didn't specify it
     *
     * @param config          the packer config - holding reference to the files that will be packed
     * @param jarOutputStream the jar output stream
     * @throws IOException if something went wrong
     */
    private void packLog4jConfigFile(PackerConfiguration config, JarOutputStream jarOutputStream) throws IOException {
        String log4jFile = config.getLog4jFile() != null ? config.getLog4jFile() : LOG4J_CONFIG_FILE;
        JarEntry log4jEntry = null;
        InputStream is = null;
        boolean loadDefault = true;
        if (config.getLog4jFile() != null) {
            try {
                is = new FileInputStream(log4jFile);
                loadDefault = false;
                log4jEntry = new JarEntry(new File(log4jFile).getName());
            } catch (FileNotFoundException e) {
                System.out.println("The provided log4j config file doesn't exist: " + e.getMessage() + "\nUsing the default one : ");
            }
        }
        if (loadDefault) {
            URL url = this.getClass().getResource(LOG4J_CONFIG_FILE);
            try {
                is = url.openStream();
                log4jEntry = new JarEntry(new File(url.getFile()).getName());
            } catch (IOException e) {
                System.out.println("Unable to load the default log4j config file : " + e.getMessage());
                throw new BuildException(e);
            }
        }
        try {
            jarOutputStream.putNextEntry(log4jEntry);
            PackerUtils.copyStream(is, jarOutputStream);
        } finally {
            is.close();
        }
        jarOutputStream.closeEntry();
    }

    /**
     * Create the manifest file and set the main-class
     *
     * @param config the configuration object holding reference to the files that will be packed
     * @return the manifest
     */
    private Manifest createManifest(PackerConfiguration config) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, config.getExtractorClass());
        attributes.put(APPLICATION_MAIN_CLASS_ATTRIBUTE, config.getAppMainClass());
        if (config.getSplashScreenImage() != null && !config.getSplashScreenImage().trim().equals("")) {
            attributes.put(SPLASH_SCREEN_ATTRIBUTE, config.getSplashScreenImage());
        }
        return manifest;
    }

    /**
     * Packs the classes that are needed only by the NestedJar
     *
     * @param jarOutputStream the jar where the classes will be packed
     * @param config packer configuration
     * @throws IOException if the jar could not be created
     */
    private void packInternallyUsedFiles(JarOutputStream jarOutputStream, PackerConfiguration config) throws IOException {
        File baseDir = config.getProject().getBaseDir();
        File extractorClassesDir = new File(baseDir, config.getExtractorClassesDir());
        List<String> entries = new ArrayList<String>();
        createEntries(extractorClassesDir.getCanonicalPath(), extractorClassesDir, entries);
        for (String internalClass : entries) {
            InputStream is = new FileInputStream(new File(extractorClassesDir, internalClass));
            if (is != null) {
                String entryName = internalClass.replace(File.separator, "/");
                entryName = entryName.startsWith("/") ? entryName.substring(1) : entryName;
                JarEntry jarEntry = new JarEntry(entryName);
                jarOutputStream.putNextEntry(jarEntry);
                PackerUtils.copyStream(is, jarOutputStream);
                is.close();
                jarOutputStream.closeEntry();
            } else {
                throw new RuntimeException("Can't pack class " + internalClass);
            }
        }
    }

    /**
     * Creates the jar entries to be packaged
     *
     * @param basePath the basepath
     * @param parentDir parent directory for classes
     * @param result the list of entries
     * @throws IOException if any errors
     */
    private void createEntries(String basePath, File parentDir, List<String> result) throws IOException {
        for (File f : parentDir.listFiles()) {
            if (f.isDirectory()) {
                createEntries(basePath, f, result);
            } else {
                String fullPath = f.getCanonicalPath();
                result.add(fullPath.substring(basePath.length(), fullPath.length()));
            }
        }
    }

    /**
     * Packs the FileSet resources
     *
     * @param resources       the FileSet instances
     * @param folder          the parent folder
     * @param jarOutputStream the jar outputstream
     * @throws IOException if the packaging fails
     */
    private void packResources(Vector resources, String folder, JarOutputStream jarOutputStream) throws IOException {
        for (int i = 0; i < resources.size(); i++) {
            FileSet fs = (FileSet) resources.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner();
            ds.scan();
            String[] files = ds.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                Resource resource = ds.getResource(files[j]);
                String s = folder.replace("\\\\", "/") + "/" + files[j].replaceAll("\\\\", "/");
                JarEntry jarEntry = new JarEntry(s);
                jarEntry.setMethod(JarEntry.DEFLATED);
                jarOutputStream.putNextEntry(jarEntry);
                InputStream is = new FileInputStream(ds.getBasedir().getCanonicalPath() + "/" + resource.getName());
                PackerUtils.copyStream(is, jarOutputStream);
                is.close();
                jarOutputStream.closeEntry();
            }
        }
    }
}
