package net.sf.force4maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.force4maven.support.ManifestGenerator;
import net.sf.force4maven.support.PackageArchiver;
import net.sf.force4maven.support.ScontrolWriter;
import net.sf.force4maven.support.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import com.sforce.soap._2006._04.metadata.Encoding;
import com.sforce.soap._2006._04.metadata.SControlContentSource;
import com.sforce.soap._2006._04.metadata.Scontrol;

/**
 * @goal packageMetadata
 * @phase package
 */
public class PackageMetadataMojo extends MetadataMojo {

    private static final Pattern SCRIPT_REFERENCE = Pattern.compile("<\\s*script.*src=\"([^\\\"]*)\"\\s*></script>");

    private static final String CLASSPTAH_PREFIX = "classpath:/";

    private static final String INLINE_PREFIX = "inline:/";

    /**
	 * @parameter expression="${basedir}/src/main/config/objects"
	 */
    private String objectsDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/scf"
	 */
    private String scontrolsDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/config/tabs"
	 */
    private String tabsDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/config/applications"
	 */
    private String applicationsDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/config/profiles"
	 */
    private String profilesDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/config/documents"
	 */
    private String documentsDirectory;

    /**
	 * @parameter expression="${basedir}/src/main/pages"
	 */
    private String pagesDirectory;

    /**
	 * @parameter
	 */
    private String snippetScontrols;

    /**
	 * @parameter expression="${basedir}/target"
	 */
    protected File targetDirectory;

    protected Set<String> snippetNames = new HashSet<String>();

    protected void copyResourcesToStage(String sourceDir, String folder) throws Exception {
        File sd = new File(stageDirectory);
        if (!sd.exists()) {
            sd.mkdirs();
        }
        File dir = new File(sourceDir);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                copyResourcesToStage(files[i].getPath(), (folder == null) ? files[i].getName() : folder + File.separator + files[i].getName());
            } else {
                copyResourceToStage(files[i], folder);
            }
        }
    }

    protected void copyResourceToStage(String resourcePath, String folder) throws Exception {
        copyResourceToStage(new File(resourcePath), folder);
    }

    protected void copyResourceToStage(File resource, String folder) throws Exception {
        if (folder != null) {
            folder = folder + File.separator;
        } else {
            folder = "";
        }
        String name = resource.getName();
        File target = new File(stageDirectory + File.separator + folder + name);
        if (resource.lastModified() > target.lastModified()) {
            FileUtils.copyFile(resource, target, true);
        }
    }

    /**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
    public void execute() throws MojoExecutionException, MojoFailureException {
        assert (stageDirectory != null);
        if (snippetScontrols != null) {
            String[] names = snippetScontrols.split(",");
            snippetNames.addAll(Arrays.asList(names));
        }
        try {
            copyResourcesToStage(applicationsDirectory, "applications");
            copyResourcesToStage(profilesDirectory, "profiles");
            copyResourcesToStage(objectsDirectory, "objects");
            copyResourcesToStage(documentsDirectory, "documents");
            copyResourcesToStage(tabsDirectory, "tabs");
            copyResourcesToStage(pagesDirectory, "pages");
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error copying resources", e);
        }
        try {
            processScontrols(scontrolsDirectory);
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error generating S-Controls", e);
        }
        try {
            ManifestGenerator mg = new ManifestGenerator(stageDirectory);
            mg.generate(packageName);
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error generating manifest file", e);
        }
        try {
            PackageArchiver archiver = new PackageArchiver(new File(stageDirectory), packageName);
            byte[] data = archiver.archive();
            FileOutputStream archive = new FileOutputStream(new File(targetDirectory, packageName + ".zip"));
            archive.write(data);
            archive.close();
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Error generating package archive", e);
        }
    }

    protected void processScontrols(String directory) throws Exception {
        if (directory == null) {
            return;
        }
        File dir = new File(directory);
        File[] files = dir.listFiles((FilenameFilter) new SuffixFileFilter(".scf"));
        if (files == null) {
            return;
        }
        getLog().info("Processing " + files.length + " S-Controls");
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                continue;
            }
            processScontrol(files[i]);
        }
    }

    protected void processScontrol(File file) throws MojoExecutionException, IOException {
        long timestamp = file.lastModified();
        Scontrol scontrol = new Scontrol();
        String name = file.getName();
        scontrol.setName(Utils.stripExtension(name));
        scontrol.setEncodingKey(Encoding.fromValue("UTF-8"));
        if (snippetNames.contains(scontrol.getName())) {
            scontrol.setContentSource(SControlContentSource.Snippet);
        } else {
            scontrol.setContentSource(SControlContentSource.HTML);
        }
        String body = FileUtils.readFileToString(file);
        body = processScriptReferences(body);
        scontrol.setContent(body.getBytes());
        File targetDirectory = new File(stageDirectory + File.separator + "scontrols");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        ScontrolWriter writer = new ScontrolWriter(targetDirectory.getPath());
        writer.setLastModified(timestamp);
        writer.write(scontrol);
    }

    protected String processScriptReferences(String body) throws IOException {
        Matcher m = SCRIPT_REFERENCE.matcher(body);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String scriptBody = null;
            String src = m.group(1);
            if (src.startsWith(CLASSPTAH_PREFIX)) {
                src = src.substring(CLASSPTAH_PREFIX.length());
                InputStream is = getClass().getClassLoader().getResourceAsStream(src);
                if (is == null) {
                    getLog().warn("Unable to find referenced script on classpath: " + src);
                } else {
                    scriptBody = IOUtils.toString(is);
                }
            }
            if (src.startsWith(INLINE_PREFIX)) {
                src = src.substring(INLINE_PREFIX.length());
                URL url = new URL(src);
                InputStream is = null;
                try {
                    is = url.openStream();
                    scriptBody = IOUtils.toString(is);
                } catch (IOException ioe) {
                    getLog().error("Error loading script from: " + src + "[" + ioe.getClass().getName() + ":" + ioe.getMessage() + "]");
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } else {
                File f = new File(scontrolsDirectory + File.separator + src);
                if (f.exists()) {
                    scriptBody = FileUtils.readFileToString(f);
                } else {
                    if (src.startsWith("..")) {
                        getLog().warn("SCRIPT NOT FOUND: " + f.getCanonicalPath());
                    }
                }
            }
            if (scriptBody != null) {
                m.appendReplacement(out, "<script type=\"text/javascript\">\n" + Utils.escapeReplacement(scriptBody) + "\n</script>");
            }
        }
        m.appendTail(out);
        return out.toString();
    }
}
