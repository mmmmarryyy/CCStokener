package com.cromoteca.meshcms.server.toolbox;

import com.cromoteca.meshcms.server.core.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CachedEntityResolver implements EntityResolver {

    private File cacheDir;

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        cacheDir.mkdirs();
        File cache = new File(cacheDir, IO.fixFileName(publicId, true));
        if (!cache.exists()) {
            try {
                URL url = new URL(systemId);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", "Cromoteca Cached Entity Resolver");
                InputStream is = connection.getInputStream();
                OutputStream os = new FileOutputStream(cache);
                IO.copyStream(is, os, true);
            } catch (IOException ex) {
                Context.log("Error while fetching entity", ex);
                return null;
            }
        }
        InputSource inputSource = new InputSource(systemId);
        inputSource.setPublicId(publicId);
        inputSource.setByteStream(new FileInputStream(cache));
        return inputSource;
    }
}
