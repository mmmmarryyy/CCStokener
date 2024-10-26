package com.ouroboroswiki.core.content.url.unversioned;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ouroboroswiki.core.AbstractContentRepository;
import com.ouroboroswiki.core.Content;
import com.ouroboroswiki.core.ContentException;
import com.ouroboroswiki.core.ContentPath;
import com.ouroboroswiki.core.ContentRepository;
import com.ouroboroswiki.core.ContentUtil;
import com.ouroboroswiki.core.Version;
import com.ouroboroswiki.core.VersionBuilder;
import com.ouroboroswiki.core.WritableContentRepository;
import com.ouroboroswiki.core.content.url.URLContent;

public class UnversionedURLContentRepository extends AbstractContentRepository implements WritableContentRepository {

    private static final Logger log = Logger.getLogger(UnversionedURLContentRepository.class.getName());

    private boolean writable;

    private String urlPrefix;

    private String urlPostfix;

    public UnversionedURLContentRepository(boolean writable, String urlPrefix, String urlPostfix) {
        this.writable = writable;
        this.urlPrefix = urlPrefix;
        this.urlPostfix = urlPostfix;
    }

    @Override
    public boolean exists(Object principal, ContentPath path) {
        return true;
    }

    @Override
    public Content getContent(Object principal, ContentPath path, Version version, Map<String, Object> properties) throws ContentException {
        String uniqueName = path.getBaseName();
        URL url = buildURL(uniqueName);
        URLContent content = new URLContent(url, this.getName(), uniqueName);
        content.setUniqueName(uniqueName);
        content.setReadable(true);
        content.setWritable(writable);
        content.setExists(true);
        try {
            URLConnection connection = url.openConnection();
            String mimeType = connection.getContentType();
            content.setMimeType(mimeType);
            content.setWritable(true);
        } catch (IOException ex) {
            throw new ContentException("unable to obtain mime type of " + url, ex);
        }
        return content;
    }

    @Override
    public void writeToContent(Object principal, String uniqueId, InputStream ins) throws IOException, ContentException {
        if (writable) {
            URL url = buildURL(uniqueId);
            URLConnection connection = url.openConnection();
            OutputStream outs = connection.getOutputStream();
            try {
                ContentUtil.pipe(ins, outs);
            } finally {
                try {
                    outs.close();
                } catch (Exception ex) {
                    log.log(Level.WARNING, "unable to close " + url, ex);
                }
            }
        } else {
            throw new ContentException("not writable");
        }
    }

    @Override
    public Version buildVersion(Object principal, ContentPath path, String[] versionPath, VersionBuilder versionBuilder) {
        return versionBuilder.buildVersion(getName(), path.getBaseName(), versionPath, 0);
    }

    private URL buildURL(String uniqueName) throws ContentException {
        String urlString = urlPrefix + uniqueName + urlPostfix;
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new ContentException("could not assemble URL from " + urlString, ex);
        }
    }
}
