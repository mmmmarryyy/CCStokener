package com.googlecode.servus.content;

import com.google.common.io.ByteStreams;
import com.googlecode.recycled.lang.assertions.Assert;
import com.googlecode.recycled.lang.jse.api.net.Urls;
import org.apache.commons.lang.UnhandledException;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TransferHandler {

    final URL url;

    public TransferHandler(URL url) {
        this.url = Assert.notNull(url);
    }

    public TransferHandler(String url) {
        this.url = Urls.asUrl(url);
    }

    public byte[] transfer(@Nullable final TransferListener transferListener) {
        try {
            InputStream inputStream = url.openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available());
            if (transferListener != null) {
                inputStream = new ObservableInputStream(inputStream, transferListener);
            }
            ByteStreams.copy(InputSuppliers.asInputSupplier(inputStream), outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UnhandledException(e);
        }
    }

    public byte[] transfer() {
        return transfer(null);
    }
}
