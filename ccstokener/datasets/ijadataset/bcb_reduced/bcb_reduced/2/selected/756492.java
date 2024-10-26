package storyteller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MediaDownloader {

    private String _path;

    private ArrayList<String> _downloaded;

    public MediaDownloader() {
        init(null);
    }

    public MediaDownloader(String path) {
        init(path);
    }

    private void init(String path) {
        _path = path;
        _downloaded = new ArrayList<String>();
    }

    public void delete() {
        for (String s : _downloaded) {
            (new File(s)).delete();
        }
    }

    public String get(String urlAsText) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            String localFilename = nextFilename(urlAsText);
            URL url = new URL(urlAsText);
            out = new BufferedOutputStream(new FileOutputStream(localFilename));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            _downloaded.add(localFilename);
            return localFilename;
        } catch (Exception exception) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    private String nextFilename(String url) {
        String currentName = "tmp";
        String extension = extractExtension(url);
        int counter = 1;
        File file = new File(_path, currentName + extension);
        while (file.exists()) {
            String filename = currentName + (counter++) + extension;
            file = new File(_path, filename);
        }
        return file.getAbsolutePath();
    }

    private String extractExtension(String url) {
        int index = url.lastIndexOf(".");
        if (index == -1) {
            return ".jpg";
        } else {
            return url.substring(index, url.length());
        }
    }
}
