package viewer.core;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sebastian Kuerten (sebastian.kuerten@fu-berlin.de)
 * 
 * @param <T>
 *            the type of things that map to cached images.
 */
public class ImageProviderHttp<T> extends ImageProvider<T, BufferedImageAndBytes> {

    static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

    private PathResoluter<T> resolver;

    private String userAgent;

    private final int nTries;

    ImageProviderHttp(PathResoluter<T> resolver, int nThreads, int nTries) {
        super(nThreads);
        this.resolver = resolver;
        this.nTries = nTries;
    }

    /**
	 * Set the user-agent to use during HTTP-requests.
	 * 
	 * @param userAgent
	 *            the user agent to use.
	 */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public BufferedImageAndBytes load(T thing) {
        String iurl = resolver.getUrl(thing);
        URL url;
        for (int k = 0; k < nTries; k++) {
            if (k > 0) {
                logger.debug("retry #" + k);
            }
            try {
                url = new URL(iurl);
                URLConnection connection = url.openConnection();
                if (userAgent != null) {
                    connection.setRequestProperty("User-Agent", userAgent);
                }
                InputStream is = new BufferedInputStream(connection.getInputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream(40000);
                int b;
                while ((b = is.read()) != -1) {
                    baos.write(b);
                }
                is.close();
                byte[] bytes = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                BufferedImage image = ImageIO.read(bais);
                return new BufferedImageAndBytes(image, bytes);
            } catch (MalformedURLException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
        return null;
    }
}
