package de.sendorian.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Flushables;

public class FileReader {

    private static final Logger logger = Logger.getLogger(FileReader.class.getName());

    public byte[] getFile(String url) throws IOException {
        InputStream in = null;
        byte[] file = null;
        try {
            in = getInputStream(url);
            file = ByteStreams.toByteArray(in);
        } finally {
            Closeables.closeQuietly(in);
        }
        return file;
    }

    /**
     * Directly saves a file specified by a given URL to a file whithout storing
     * it in memory.
     */
    public boolean save(String url, String dir, String fileName) throws IOException {
        return save(getInputStream(url), dir, fileName);
    }

    /**
     * Directly saves a file specified by a given URL to a file whithout storing
     * it in memory.
     */
    public boolean save(InputStream in, String dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        if (file.exists()) {
            logger.info("The file \"" + dir + java.io.File.separator + fileName + "\" already exists. Skipping.");
            return false;
        }
        return save(in, file, dir);
    }

    private boolean save(InputStream in, File file, String dir) throws IOException {
        File tmpFile = new File(dir, file.getName() + ".part");
        Files.createParentDirs(tmpFile);
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        tmpFile.createNewFile();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), 64 * 1024);
        try {
            ByteStreams.copy(in, out);
        } finally {
            Closeables.closeQuietly(in);
            Flushables.flushQuietly(out);
            Closeables.closeQuietly(out);
        }
        Files.move(tmpFile, file);
        return true;
    }

    private InputStream getInputStream(String url) throws IOException {
        if (url.startsWith("file://")) {
            File file = new File(url.substring("file://".length()));
            return new FileInputStream(file);
        } else {
            HttpClient client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = client.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new IOException("Found no file at " + url);
                }
                return new BufferedInputStream(entity.getContent(), 64 * 1024);
            }
            throw new IOException("Could not get the file '" + url + "'. URL returned: " + response.getStatusLine());
        }
    }
}
