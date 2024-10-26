package net.entropysoft.transmorph.plugin.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

/**
 * Manages templates.
 * 
 * Templates files must be present in the plugin at runtime. For that,
 * build.properties must contain template folders in "Binary build" section
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 * 
 */
public class TemplateManager {

    public static Bundle BUNDLE = Activator.getDefault().getBundle();

    public static InputStream getTemplateInputStream(String templateDir, String path) throws IOException {
        URL url = BUNDLE.getEntry(templateDir + "/" + path);
        return url.openStream();
    }

    public static void createFromTemplate(IContainer destinationFolder, String templateDir) throws CoreException, IOException {
        createFromTemplate(destinationFolder, templateDir, templateDir);
    }

    private static void createFromTemplate(IContainer destinationFolder, String templateDir, String path) throws CoreException, IOException {
        Enumeration<String> enumeration = BUNDLE.getEntryPaths(path);
        if (enumeration == null) {
            return;
        }
        for (; enumeration.hasMoreElements(); ) {
            String entryPath = enumeration.nextElement();
            if (entryPath.endsWith("/")) {
                String folderPath = entryPath.substring(templateDir.length() + 1);
                IFolder folder = destinationFolder.getFolder(new Path(folderPath));
                if (!folder.exists()) {
                    folder.create(true, true, null);
                }
                createFromTemplate(destinationFolder, templateDir, entryPath);
            } else {
                URL url = BUNDLE.getEntry(entryPath);
                String filePath = entryPath.substring(templateDir.length() + 1);
                IFile file = destinationFolder.getFile(new Path(filePath));
                if (!file.exists()) {
                    file.create(url.openStream(), IResource.FORCE, null);
                } else {
                    file.setContents(url.openStream(), true, true, null);
                }
            }
        }
    }
}
