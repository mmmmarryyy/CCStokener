package joodin.impl.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import joodin.impl.application.util.ApplicationWrapper;
import my.stuff.vaadin.miglayout.util.ImageWrapper;
import org.jowidgets.spi.impl.image.IImageFactory;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;

public class VaadinImageLoader implements IImageFactory<Resource> {

    private final URL url;

    public VaadinImageLoader(final URL url) {
        this.url = url;
    }

    @SuppressWarnings("serial")
    public Resource createImage() {
        StreamResource image = new StreamResource(new StreamSource() {

            @Override
            public InputStream getStream() {
                try {
                    return url.openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }, url.getFile(), ApplicationWrapper.getInstance());
        return image;
    }
}
