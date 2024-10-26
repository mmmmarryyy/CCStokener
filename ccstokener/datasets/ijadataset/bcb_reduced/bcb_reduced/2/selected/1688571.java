package xcordion.lang.java;

import xcordion.lang.java.JDomTestDocument;
import xcordion.util.FileUtils;
import xcordion.util.ResourceFinder;
import xcordion.util.XcordionBug;
import xcordion.api.ResourceReference;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.net.URL;
import junit.framework.AssertionFailedError;

public class MarkupWriter {

    private final File outputDirectory;

    public MarkupWriter(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String write(JDomTestDocument testDocument, String outputPath, Class testClass, List<ResourceReference<JDomTestDocument.JDomTestElement>> resourceRefs) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        for (ResourceReference<JDomTestDocument.JDomTestElement> resourceRef : resourceRefs) {
            writeResourceToOutputDirectory(resourceRef, testClass, outputPath);
        }
        File outputFile = new File(outputDirectory, outputPath);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFile);
            testDocument.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionFailedError("Can't write to output file: " + outputFile.getPath());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return outputFile.getPath();
    }

    private void writeResourceToOutputDirectory(ResourceReference<JDomTestDocument.JDomTestElement> resourceRef, Class testClass, String outputPath) {
        String path = resourceRef.getResourcePath();
        URL url = new ResourceFinder(testClass).getResourceAsURL(path);
        if (url == null) {
            throw new XcordionBug("Cannot find resource: " + path);
        }
        path = path.replace('/', File.separatorChar);
        while (path.startsWith(Character.toString(File.separatorChar))) {
            path = path.substring(1);
        }
        File outputFile = new File(outputDirectory, path);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        resourceRef.setResourceReferenceUri(FileUtils.relativePath(outputPath, path, false, File.separatorChar));
        if (outputFile.exists() && System.currentTimeMillis() - outputFile.lastModified() < 5000) {
            return;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = url.openStream();
            os = new FileOutputStream(outputFile);
            byte[] buffer = new byte[is.available()];
            int i;
            while ((i = is.read(buffer)) > 0) {
                os.write(buffer, 0, i);
            }
        } catch (IOException e) {
            throw new AssertionFailedError(e.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
