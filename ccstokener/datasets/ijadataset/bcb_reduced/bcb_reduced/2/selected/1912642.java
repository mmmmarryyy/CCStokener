package com.google.code.ptrends.locators.implementations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.google.code.ptrends.locators.Locator;

public class URILocator implements Locator {

    private byte[] buffer = null;

    private final String sourceLocation;

    private final List<InputStream> streams;

    private static final Logger LOG = Logger.getLogger(URILocator.class);

    public URILocator(final String sourceLocation) {
        if (StringUtils.isBlank(sourceLocation)) {
            throw new IllegalArgumentException("Illegal blank source location");
        }
        this.sourceLocation = sourceLocation;
        this.streams = new ArrayList<InputStream>();
    }

    @Override
    public InputStream getStream() throws IOException {
        if (buffer == null) {
            buffer = initBuffer();
        }
        InputStream stream = new ByteArrayInputStream(buffer);
        streams.add(stream);
        return new ByteArrayInputStream(buffer);
    }

    private byte[] initBuffer() throws IOException {
        final URL url = new URL(sourceLocation);
        final URLConnection urlConnection = url.openConnection();
        final InputStream input = urlConnection.getInputStream();
        final int length = urlConnection.getContentLength();
        final ByteArrayOutputStream output = new ByteArrayOutputStream(length);
        final byte[] bytes = new byte[1024];
        while (true) {
            final int len = input.read(bytes);
            if (len == -1) {
                break;
            }
            output.write(bytes, 0, len);
        }
        input.close();
        output.close();
        return output.toByteArray();
    }

    @Override
    public void close() {
        for (InputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.error("Error closing stream", e);
            }
        }
    }
}
