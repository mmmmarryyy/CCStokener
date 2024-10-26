package pipe4j.pipe.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import pipe4j.pipe.SimpleStreamDecoratorPipe;

/**
 * Pipe that reads from an {@link URL} and feeds pipeline.
 * 
 * @author bbennett
 */
public class UrlIn extends SimpleStreamDecoratorPipe {

    private String url;

    public UrlIn(String url) throws IOException {
        this.url = url;
    }

    @Override
    protected InputStream getDecoratedInputStream(InputStream inputStream) throws IOException {
        return new URL(url).openStream();
    }
}
