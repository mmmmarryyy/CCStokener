package com.volantis.synergetics.testtools.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Creates a temporary file from a jar resource.
 */
public class ResourceTemporaryFileCreator implements TemporaryFileCreator {

    /**
     * The copyright statement.
     */
    private static String mark = "(c) Volantis Systems Ltd 2003.";

    /**
     * Class to look up the resource relative to.
     */
    private Class clazz;

    /**
     * Path to the jar resource.
     */
    private String resource;

    /**
     * Create an instance of this class, using the path to the jar resource
     * provided.
     * 
     * @param resource the path to the jar resource that this class creates
     *      temporary files from.
     */
    public ResourceTemporaryFileCreator(Class clazz, String resource) {
        this.clazz = clazz;
        this.resource = resource;
    }

    public File createTemporaryFile() throws IOException {
        URL url = clazz.getResource(resource);
        if (url == null) {
            throw new IOException("No resource available from '" + clazz.getName() + "' for '" + resource + "'");
        }
        String extension = getExtension(resource);
        String prefix = "resource-temporary-file-creator";
        File file = File.createTempFile(prefix, extension);
        InputStream input = url.openConnection().getInputStream();
        FileOutputStream output = new FileOutputStream(file);
        com.volantis.synergetics.io.IOUtils.copyAndClose(input, output);
        return file;
    }

    /**
     * Calculate and return the extension of the filename.
     * <p>
     * If one cannot be found an empty string will be returned. This is so that
     * {@link File#createTempFile} does not use a default extension in this
     * case.
     *
     * @param fileName the name of the file
     * @return the extension of the file, or an empty string.
     */
    private String getExtension(String fileName) {
        String extension = null;
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            extension = fileName.substring(dot);
        } else {
            extension = "";
        }
        return extension;
    }
}
