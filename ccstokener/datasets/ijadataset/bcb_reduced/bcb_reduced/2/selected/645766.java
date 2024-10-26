package com.ouroboroswiki.core.content.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ouroboroswiki.core.AbstractContent;
import com.ouroboroswiki.core.Content;
import com.ouroboroswiki.core.ContentUtil;

public class URLContent extends AbstractContent {

    private static final Logger log = Logger.getLogger(URLContent.class.getName());

    private URL url;

    public URLContent(URL url) {
        this.url = url;
    }

    public URLContent(URL url, String repoName, String uniqueId) {
        this(url);
        this.setUniqueName(uniqueId);
        this.setRepositoryName(repoName);
    }

    @Override
    public Map<String, Content> getChildContent() {
        return null;
    }

    @Override
    public void write(OutputStream outs) throws IOException {
        InputStream ins = url.openStream();
        try {
            ContentUtil.pipe(ins, outs);
        } finally {
            try {
                ins.close();
            } catch (Exception ex) {
                log.log(Level.WARNING, "unable to close " + url, ex);
            }
        }
    }
}
