package org.simcom.commons.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FileFinder {

    public static InputStream findFileAsInputStream(String fileName) throws IOException {
        File findedFile = findFile(fileName);
        if (findedFile != null) return new FileInputStream(findedFile);
        URL url = SimComUtil.findFileInConfigBundle(fileName);
        if (url != null) return url.openStream();
        throw new RuntimeException("File not found for " + fileName);
    }

    public static File findFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) return file;
        return null;
    }
}
