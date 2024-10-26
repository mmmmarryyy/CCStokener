package org.robotframework.javalib.beans.common;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class URLFileFactory {

    private final String localDirectoryPath;

    public URLFileFactory(String localDirectoryPath) {
        this.localDirectoryPath = localDirectoryPath;
    }

    public File createFileFromUrl(String url) {
        File localFile = createLocalFile(url);
        if (shouldCopyFromURL(url, localFile)) copyUrlToFile(url, localFile);
        return localFile;
    }

    private File createLocalFile(String url) {
        return new File(createFileNameFromUrl(url));
    }

    private boolean shouldCopyFromURL(String url, File localFile) {
        return !localFile.exists() || urlLastModified(url) > localFile.lastModified();
    }

    private String createFileNameFromUrl(String url) {
        String fileSeparator = System.getProperty("file.separator");
        return localDirectoryPath + fileSeparator + FilenameUtils.getBaseName(url) + "." + FilenameUtils.getExtension(url);
    }

    private void copyUrlToFile(String url, File localFile) {
        try {
            FileUtils.copyURLToFile(createURL(url), localFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private long urlLastModified(String url) {
        try {
            return createURL(url).openConnection().getLastModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
