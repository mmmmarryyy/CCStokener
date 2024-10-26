package com.google.code.javastorage.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import com.google.code.javastorage.util.URLOpener;

/**
 * Only for unit tests
 * 
 * @author thomas.scheuchzer@gmail.com
 *
 */
public class DummyURLOpener extends URLOpener {

    private String content;

    private IOException exception;

    public DummyURLOpener(String content) {
        this.content = content;
    }

    @Override
    public InputStream openStream(URL url) throws IOException {
        try {
            if (exception != null) {
                throw exception;
            }
            return IOUtils.toInputStream(content);
        } finally {
            content = null;
            exception = null;
        }
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setException(IOException exception) {
        this.exception = exception;
    }
}
