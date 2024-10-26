package uk.icat3.io.ids;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uk.icat3.io.entity.FileId;
import uk.icat3.io.util.IOHelper;

public class IDSDownloader {

    private static final String idsBaseUrl = "http://localhost:12345/";

    private static final String GET_URL_FORMAT = "%s/Data/get?filedirectory=%s&filename=%s";

    public Map<FileId, byte[]> extractFiles(Set<FileId> fileNames) {
        Map<FileId, byte[]> files = new HashMap<FileId, byte[]>();
        for (FileId fileId : fileNames) {
            try {
                String filePath = getFilePath(fileId);
                URL url = getUrl(filePath);
                files.put(fileId, readStream(url.openStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    private byte[] readStream(InputStream stream) throws IOException {
        return IOHelper.readToEnd(stream);
    }

    private String getFilePath(FileId fileId) {
        return String.format(GET_URL_FORMAT, idsBaseUrl, fileId.getLocation(), fileId.getName());
    }

    private URL getUrl(String path) throws MalformedURLException {
        return new URL(path);
    }
}
