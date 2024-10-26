package ro.codemart.installer.plugin.packer;

import ro.codemart.installer.plugin.packer.exception.BuildInstallerException;
import ro.codemart.installer.plugin.packer.util.PackerUtils;
import ro.codemart.installer.plugin.packer.util.MockArtifact;
import ro.codemart.commons.StreamHelper;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.jar.*;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Creates a nested jar in the following form : <br>
 * <ul>
 * <li>/lib</li>
 * <li>/resources</li>
 * <li>/ro/codemart/installer/plugin/packer/... for internally used classes</li>
 * </ul>
 *
 * @author marius.ani
 */
public class NestedJarPackerImpl implements Packer {

    private static final String LOG4J_CONFIG_FILE = "/log4j.xml";

    File destFile;

    /**
     * Creates a jar file with other jar files nested, as well as any other resource and config files.
     * It is used by the ant task specially designed for this job.
     *
     * @param config the configuration object holding reference to the files that will be packed
     */
    public void pack(PackerConfig config) {
        FileOutputStream fos = null;
        JarOutputStream jarOutputStream = null;
        try {
            destFile = new File(config.getDestfile());
            fos = new FileOutputStream(destFile);
            Manifest manifest = createManifest(config);
            jarOutputStream = new JarOutputStream(fos, manifest);
            ArtifactRepository repository = config.getRepository();
            MavenProject mvnProject = config.getMavenProject();
            packExtractor(config.getRepository(), config.getExtractor(), jarOutputStream);
            packFiles(getInstallerDependencies(repository, mvnProject, config.getExcludeArtifactIds().getIncludes()), "lib/codemart", jarOutputStream);
            packFiles(getLibsFromRepository(config.getRepository(), GRAPHICS_LIB_GROUP_ID, GRAPHICS_LIB_ARTIFACT_IDS, GRAPHICS_LIB_VERSION), "lib/3rd-party", jarOutputStream);
            List<MockArtifact> mockArtifacts = createMockArtifacts();
            for (MockArtifact mock : mockArtifacts) {
                packFile(getLibFromRepository(repository, mock.getGroupId(), mock.getArtifactId(), mock.getVersion()), "lib/3rd-party", jarOutputStream);
            }
            packFile(PackerUtils.createFileFromResource("/3rd-party/charva/libTerminal.so"), "lib/3rd-party", jarOutputStream);
            packFile(PackerUtils.createFileFromResource("/3rd-party/charva/Terminal.dll"), "lib/3rd-party", jarOutputStream);
            packFile(PackerUtils.createFileFromResource("/3rd-party/charva/Toolkit.o"), "lib/3rd-party", jarOutputStream);
            for (String resourceLicense : LICENSES_RESOURCES) {
                packFile(PackerUtils.createFileFromResource(resourceLicense), "lib/licenses", jarOutputStream);
            }
            packFiles(getLibsFromDir(config.getTargetDir()), "lib", jarOutputStream);
            packFiles(getFilteredResources(config), "resources", jarOutputStream);
            packLog4jConfigFile(config, jarOutputStream);
        } catch (FileNotFoundException e) {
            throw new BuildInstallerException("Could not create destination file: " + destFile + ". Reason: " + e.getMessage());
        } catch (IOException e) {
            throw new BuildInstallerException(e);
        } finally {
            PackerUtils.closeStream(jarOutputStream);
            PackerUtils.closeStream(fos);
        }
    }

    /**
     * Packs the extractor classes. These will be able to extract the generated nested jar, and to run the installer afterwards
     * @param repository the repository where the extractor lib is located
     * @param extractor the extractor dependency
     * @param destJar the destination jar where extractor classes will be put
     * @throws IOException if any errors
     */
    private void packExtractor(ArtifactRepository repository, Dependency extractor, JarOutputStream destJar) throws IOException {
        File extractorJar = getLibFromRepository(repository, extractor.getGroupId(), extractor.getArtifactId(), extractor.getVersion());
        JarInputStream jis = new JarInputStream(new FileInputStream(extractorJar));
        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            destJar.putNextEntry(entry);
            PackerUtils.copyStream(jis, destJar);
            destJar.closeEntry();
        }
        PackerUtils.closeStream(jis);
    }

    /**
     * Creates a list of 'mock' artifacts (required for  groovy)
     *
     * @return the list of mock artifacts
     */
    private List<MockArtifact> createMockArtifacts() {
        List<MockArtifact> mockArtifacts = new ArrayList<MockArtifact>();
        mockArtifacts.add(new MockArtifact("antlr", "antlr", "2.7.6"));
        mockArtifacts.add(new MockArtifact("asm", "asm", "2.2"));
        mockArtifacts.add(new MockArtifact("asm", "asm-analysis", "2.2"));
        mockArtifacts.add(new MockArtifact("asm", "asm-tree", "2.2"));
        mockArtifacts.add(new MockArtifact("asm", "asm-util", "2.2"));
        mockArtifacts.add(new MockArtifact("commons-cli", "commons-cli", "1.0"));
        mockArtifacts.add(new MockArtifact("org/codehaus/groovy", "groovy", "1.5.7"));
        mockArtifacts.add(new MockArtifact("org/swinglabs", "swingx", "0.9.2"));
        mockArtifacts.add(new MockArtifact("log4j", "log4j", "1.2.13"));
        mockArtifacts.add(new MockArtifact("commons-jxpath", "commons-jxpath", "1.2"));
        mockArtifacts.add(new MockArtifact("commons-logging", "commons-logging", "1.0.3"));
        mockArtifacts.add(new MockArtifact("commons-logging", "commons-logging-api", "1.0.3"));
        mockArtifacts.add(new MockArtifact("commons-codec", "commons-codec", "1.3"));
        mockArtifacts.add(new MockArtifact("com/thoughtworks/xstream", "xstream", "1.2.2"));
        return mockArtifacts;
    }

    /**
     * Return a list of filtered files
     *
     * @param config the configuration
     * @return a list of filtered files
     */
    private List<File> getFilteredResources(PackerConfig config) {
        List<File> filteredResources = new ArrayList<File>();
        Resource resources = config.getResources();
        String baseDir = resources.getDirectory() != null ? resources.getDirectory() : config.getResourcesDir();
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(baseDir);
        String[] includes = (String[]) resources.getIncludes().toArray(new String[resources.getIncludes().size()]);
        ds.setIncludes(includes);
        String[] excludes = (String[]) resources.getExcludes().toArray(new String[resources.getExcludes().size()]);
        ds.setExcludes(excludes);
        ds.scan();
        String[] files = ds.getIncludedFiles();
        for (String file : files) {
            filteredResources.add(new File(baseDir, file));
        }
        return filteredResources;
    }

    /**
     * Returns the list of jars from the local repository.
     * <b>It is assumed that files have the same group id and the same version.</b>
     *
     * @param repository  the repository
     * @param groupId     the artifact group id
     * @param artifactIds the artifact ids
     * @param version     the artifact version
     * @return the list of jars
     */
    private List<File> getLibsFromRepository(ArtifactRepository repository, String groupId, String[] artifactIds, String version) {
        List<File> libs = new ArrayList<File>();
        for (String artifactId : artifactIds) {
            File artifact = getLibFromRepository(repository, groupId, artifactId, version);
            libs.add(artifact);
        }
        return libs;
    }

    /**
     * Return the jar file from the repository
     *
     * @param repository the repository
     * @param groupId    jar group id
     * @param artifact   jar artifact name
     * @param version    jar version
     * @return the jar from the repository
     */
    private File getLibFromRepository(ArtifactRepository repository, String groupId, String artifact, String version) {
        File lib = new File(repository.getBasedir() + File.separatorChar + normalize(groupId) + File.separatorChar + artifact + File.separatorChar + version + File.separatorChar + artifact + "-" + version + ".jar");
        if (!lib.exists()) {
            throw new BuildInstallerException("The file: " + lib.getAbsolutePath() + " could not be found in the repository: " + repository + ". Please install it.");
        }
        return lib;
    }

    private String normalize(String groupId) {
        return groupId.replace(".", File.separator);
    }

    /**
     * Return the installer dependency artifact, as specified in installer's pom.xml file
     *
     * @param repository        the repository
     * @param mvnProject        the installer's maven project
     * @param excludedArtifacts the excluded dependencies
     * @return the list of installer dependencies
     */
    private List<File> getInstallerDependencies(ArtifactRepository repository, MavenProject mvnProject, List<String> excludedArtifacts) {
        List<File> libs = new ArrayList<File>();
        List<Dependency> allDeps = mvnProject.getDependencies();
        for (Dependency d : allDeps) {
            if (excludedArtifacts != null && !excludedArtifacts.isEmpty() && excludedArtifacts.contains(d.getArtifactId())) {
                continue;
            }
            File artifact = getLibFromRepository(repository, d.getGroupId(), d.getArtifactId(), d.getVersion());
            libs.add(artifact);
        }
        return libs;
    }

    /**
     * Returns all the jar's found into the given directory, except the "installer jar".
     * Maven default is to put the generated jars under target dir
     *
     * @param targetDir the container folder for libs
     * @return the list of jar found under the given directory
     */
    private List<File> getLibsFromDir(String targetDir) {
        List<File> libs = new ArrayList<File>();
        File baseDir = new File(targetDir);
        if (baseDir.exists()) {
            File[] jars = baseDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar") && !name.equals(destFile.getName());
                }
            });
            libs = Arrays.asList(jars);
        }
        return libs;
    }

    /**
     * Packs the user specified log4j config file, or the default one if the user didn't specify it
     *
     * @param config          the packer config - holding reference to the files that will be packed
     * @param jarOutputStream the jar output stream
     * @throws IOException if something went wrong
     */
    private void packLog4jConfigFile(PackerConfig config, JarOutputStream jarOutputStream) throws IOException {
        String log4jFile = config.getLog4jResourcePath() != null ? config.getLog4jResourcePath() : LOG4J_CONFIG_FILE;
        JarEntry log4jEntry = null;
        InputStream is = null;
        boolean loadDefault = true;
        if (config.getLog4jResourcePath() != null) {
            try {
                is = new FileInputStream(log4jFile);
                loadDefault = false;
                log4jEntry = new JarEntry(new File(log4jFile).getName());
            } catch (FileNotFoundException e) {
                System.out.println("The provided log4j config file doesn't exist: " + e.getMessage() + "\nUsing the default one.");
            }
        }
        if (loadDefault) {
            URL url = this.getClass().getResource(LOG4J_CONFIG_FILE);
            try {
                is = url.openStream();
                log4jEntry = new JarEntry(new File(url.getFile()).getName());
            } catch (IOException e) {
                throw new BuildInstallerException("Unable to load the default log4j config file :" + e.getMessage());
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
    private Manifest createManifest(PackerConfig config) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, config.getExtractorMainClass());
        attributes.put(APPLICATION_MAIN_CLASS_ATTRIBUTE, config.getAppMainClass());
        if (config.getSplashScreenImage() != null && !config.getSplashScreenImage().trim().equals("")) {
            attributes.put(SPLASH_SCREEN_ATTRIBUTE, config.getSplashScreenImage());
        }
        return manifest;
    }

    /**
     * Packs the given list of files
     *
     * @param files           the files to be added
     * @param folder          the parent folder
     * @param jarOutputStream the jar outputstream
     * @throws IOException if the packaging fails
     */
    private void packFiles(List<File> files, String folder, JarOutputStream jarOutputStream) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                packFiles(getAllFiles(file), folder + "/" + file.getName(), jarOutputStream);
            } else {
                packFile(file, folder, jarOutputStream);
            }
        }
    }

    /**
     * Packs a file
     *
     * @param file            the file to be packed
     * @param folder          the parent folder
     * @param jarOutputStream the destination jar
     * @throws IOException if the packaging fails
     */
    private void packFile(File file, String folder, JarOutputStream jarOutputStream) throws IOException {
        String path = file.getCanonicalPath();
        path = path.substring(path.lastIndexOf(File.separator) + 1);
        String s = folder.replace(File.separator, "/") + "/" + path;
        JarEntry jarEntry = new JarEntry(s);
        jarEntry.setMethod(JarEntry.DEFLATED);
        jarOutputStream.putNextEntry(jarEntry);
        InputStream is = new FileInputStream(file);
        StreamHelper.copyStreamContent(is, jarOutputStream);
        is.close();
        jarOutputStream.closeEntry();
    }

    /**
     * Return all files from a directory
     *
     * @param baseDir the parent directory
     * @return the list of all files within that directory
     */
    private List<File> getAllFiles(File baseDir) {
        File[] files = baseDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return !pathname.getName().startsWith(".svn");
            }
        });
        return Arrays.asList(files);
    }
}
