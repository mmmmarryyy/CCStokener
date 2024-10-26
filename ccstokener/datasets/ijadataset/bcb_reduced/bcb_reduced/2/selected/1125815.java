package com.incendiaryblue.cmslite.static_pub;

import com.incendiaryblue.appframework.ServerConfig;
import com.incendiaryblue.cmslite.Category;
import com.incendiaryblue.cmslite.Content;
import com.incendiaryblue.cmslite.ExtraPage;
import com.incendiaryblue.cmslite.Project;
import com.incendiaryblue.cmslite.Template;
import com.incendiaryblue.io.FileHelper;
import com.incendiaryblue.util.ftp.FTPFileSender;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.http.*;
import com.incendiaryblue.user.User;

public class StaticPublisher {

    /** Whether to print debugging information. */
    private static final boolean debug = false;

    /** Name of the server config property for the temp dir. */
    public static final String CONFIG_TEMP_DIR = "static_publish.temp_dir";

    /** Name of the server config property for the document root. */
    public static final String CONFIG_DOC_ROOT = "document.root";

    /** Name of the server config property for the server root. */
    public static final String CONFIG_SERVER_ROOT = "server.root";

    private URL serverRoot;

    private StaticVersion version;

    private File buildDir;

    private List errors;

    private Project project;

    private boolean prepareArchive;

    private List archiveFiles;

    /**
	 * What extension to use for generated files.
	 * <p>This is used as a defualt if no value is set in the database.</p>
	 */
    public static final String OUTPUT_EXTENSION = "jsp";

    /**
	 * Publish a static version of the site.
	 *
	 * <p>This publishes a static version of every content item and
	 * a any extra pages defined.</p>
	 *
	 * @param project The project to publish.
	 * @param A StaticVersion object specifying the parameters for publishing.
	 */
    public synchronized void publish(Project project, StaticVersion sv) throws StaticPublishException {
        this.project = project;
        this.version = sv;
        try {
            synchronized (project) {
                setBuildDir();
                doPublish();
                if (prepareArchive) transferContent();
            }
        } catch (Exception e) {
            throw new StaticPublishException(e);
        }
    }

    private void setBuildDir() throws StaticPublishException {
        if (version.getLocalDestination() != null) buildDir = version.getLocalDestination(); else if (version.isPreview()) {
            String docRoot = ServerConfig.get(CONFIG_DOC_ROOT);
            if (docRoot == null) throw new StaticPublishException("No doc root set in server config file (" + CONFIG_DOC_ROOT + ")");
            buildDir = new File(docRoot);
        } else {
            String tempPath = ServerConfig.get(CONFIG_TEMP_DIR);
            if (tempPath == null) throw new StaticPublishException("No temp dir set in server config file (" + CONFIG_TEMP_DIR + ")");
            buildDir = new File(tempPath);
        }
        buildDir = new File(buildDir, version.getFullContentPath());
        if (!buildDir.exists()) buildDir.mkdirs(); else {
            if (debug) System.out.println("deleting contentes of directory: " + buildDir);
            FileHelper.deleteDirContents(buildDir);
        }
    }

    /**
	 * Get a list of errors that occurred while publishing.
	 *
	 * <p>Sometime errors in template pages prevent them from being published.
	 * This method returns a list of those pages.<p>
	 *
	 * <p>The list is refreshed every time the publish method is called, and
	 * is not stored in the database.</p>
	 * 
	 * @returns The names of pages that could not be published.
	 */
    public List getErrors() {
        return Collections.unmodifiableList(errors);
    }

    private void doPublish() throws IOException, MalformedURLException, StaticPublishException {
        Content.ContentPrototype contentType = Content.getContentType();
        Content.setContentType(version.isPreview() ? Content.WORKING : Content.LIVE);
        errors = new ArrayList();
        prepareArchive = version.getRemoteDestinationList().size() != 0;
        if (prepareArchive) archiveFiles = new ArrayList();
        try {
            String serverRootProp = ServerConfig.get(CONFIG_SERVER_ROOT);
            if (serverRootProp == null) throw new StaticPublishException("No server root set in server config file (" + CONFIG_SERVER_ROOT + ")");
            serverRoot = new URL(serverRootProp);
            publishRootCategory(project, buildDir);
            publishExtraPages(project, buildDir);
        } finally {
            Content.setContentType(contentType);
        }
    }

    private void publishRootCategory(Project project, File dir) throws IOException {
        dir.mkdirs();
        publishCategory(project.getRootCategory(), version.getContentPath(), dir);
    }

    private void publishCategory(Category cat, String path, File dir) throws IOException {
        if (debug) System.out.println("  publishing category: " + cat.getName());
        if (!cat.isRoot()) path = path + cat.getName() + "/";
        for (Iterator i = cat.getContentItems().iterator(); i.hasNext(); ) publishContent((Content) i.next(), path, dir);
        for (Iterator i = cat.getSubCategories().iterator(); i.hasNext(); ) {
            Category subCat = (Category) i.next();
            File subDir = new File(dir, subCat.getName());
            subDir.mkdir();
            publishCategory(subCat, path, subDir);
        }
    }

    private void publishContent(Content content, String path, File dir) throws IOException {
        if (debug) System.out.println("    publishing content: " + content.getName());
        Template template = content.findDisplayTemplate();
        if (template == null || template.getTemplatePath() == null) return;
        String extension = template.getFilenameExtension();
        if (extension == null) extension = OUTPUT_EXTENSION;
        String leaf = content.getName() + "." + extension;
        path += leaf;
        File outputFile = new File(dir, leaf);
        publishPage(content.getContentUrl(null, version), path, outputFile);
    }

    private void publishExtraPages(Project project, File dir) throws IOException {
        String path = version.getContentPath();
        for (Iterator i = project.getExtraPages().iterator(); i.hasNext(); ) {
            ExtraPage extraPage = (ExtraPage) i.next();
            if (debug) System.out.println("  publishing extra page: " + extraPage.getTemplatePath());
            String outputPath = extraPage.getOutputPath();
            File outputFile = new File(dir, outputPath);
            File parentDir = outputFile.getParentFile();
            parentDir.mkdirs();
            publishPage(extraPage.getAbsoluteUrl(version), path + outputPath, outputFile);
        }
    }

    private void publishPage(URL url, String path, File outputFile) throws IOException {
        if (debug) {
            System.out.println("      publishing page: " + path);
            System.out.println("        url == " + url);
            System.out.println("        file == " + outputFile);
        }
        StringBuffer sb = new StringBuffer();
        try {
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            boolean firstLine = true;
            String line;
            do {
                line = br.readLine();
                if (line != null) {
                    if (!firstLine) sb.append("\n"); else firstLine = false;
                    sb.append(line);
                }
            } while (line != null);
            br.close();
        } catch (IOException e) {
            String mess = outputFile.toString() + ": " + e.getMessage();
            errors.add(mess);
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStreamWriter sw = new OutputStreamWriter(fos);
        sw.write(sb.toString());
        sw.close();
        if (prepareArchive) archiveFiles.add(new ArchiveFile(path, outputFile));
    }

    private void transferContent() throws IOException {
        File archive = prepareArchive();
        try {
            String filename = generateArchiveFilename();
            List remoteDestList = version.getRemoteDestinationList();
            for (Iterator i = remoteDestList.iterator(); i.hasNext(); ) {
                RemoteDestination rd = (RemoteDestination) i.next();
                FTPFileSender fileSender = new FTPFileSender(rd.getFTPAccount());
                String remoteDir = rd.getRemoteDir();
                if (remoteDir != null) fileSender.send(archive, remoteDir, filename); else fileSender.send(archive, filename);
            }
        } finally {
            archive.delete();
        }
    }

    private File prepareArchive() throws IOException {
        if (debug) System.out.println("Preparing archive...");
        List additionalList = version.getAdditionalContentList();
        for (Iterator i = additionalList.iterator(); i.hasNext(); ) {
            AdditionalContent ac = (AdditionalContent) i.next();
            addFiles(ac.getContentDir(), ac.getContentPath());
        }
        File archive = File.createTempFile("content-archive", ".zip");
        try {
            ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(archive));
            outputStream.setMethod(ZipOutputStream.DEFLATED);
            outputStream.setLevel(9);
            byte buffer[] = new byte[4096];
            for (Iterator i = archiveFiles.iterator(); i.hasNext(); ) {
                ArchiveFile af = (ArchiveFile) i.next();
                ZipEntry entry = new ZipEntry(af.getPath());
                outputStream.putNextEntry(entry);
                FileInputStream inputStream = new FileInputStream(af.getFile());
                while (true) {
                    int len = inputStream.read(buffer);
                    if (len == -1) break;
                    outputStream.write(buffer, 0, len);
                }
                inputStream.close();
                outputStream.closeEntry();
            }
            outputStream.finish();
            outputStream.close();
        } catch (IOException e) {
            archive.delete();
            throw e;
        }
        return archive;
    }

    private void addFiles(File dir, String path) throws IOException {
        if (debug) {
            System.out.println("  adding files:");
            System.out.println("    dir == " + dir);
            System.out.println("    path == " + path);
        }
        File files[] = dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
            File f = files[i];
            if (f.isFile()) archiveFiles.add(new ArchiveFile(path + f.getName(), f)); else addFiles(f, path + f.getName() + "/");
        }
    }

    private static String generateArchiveFilename() throws IOException {
        long time = new Date().getTime();
        return "content-archive-" + time + ".zip";
    }

    /**
	 * A file that needs to be added to the content archive.
	 */
    private static class ArchiveFile {

        private String path;

        private File file;

        public ArchiveFile(String p, File f) {
            path = p;
            file = f;
        }

        public String getPath() {
            return path;
        }

        public File getFile() {
            return file;
        }
    }
}
