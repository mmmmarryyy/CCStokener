package com.tinywebgears.tuatara.framework.gui.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import com.tinywebgears.tuatara.framework.common.Assert;
import com.vaadin.terminal.StreamResource;

public class UrlStreamSource implements StreamResource.StreamSource, Serializable {

    private final URL url;

    public UrlStreamSource(URL url) {
        this.url = url;
    }

    public InputStream getStream() {
        try {
            return url.openStream();
        } catch (IOException e) {
            Assert.fail("Failed to open stream: " + url, e);
            return null;
        }
    }
}
