package resources;

import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.MissingResourceException;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import au.com.bytecode.opencsv.CSVReader;

public class Resource {

    public Resource() {
    }

    public List<String[]> getCSV(String name) {
        return new ResourceLoader<List<String[]>>(name) {

            @Override
            protected List<String[]> get(URL url) throws Exception {
                CSVReader reader = null;
                try {
                    reader = new CSVReader(new InputStreamReader(url.openStream()));
                    return reader.readAll();
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        }.get();
    }

    public BufferedImage getImage(String name) {
        return new ResourceLoader<BufferedImage>(name) {

            @Override
            protected BufferedImage get(URL url) throws Exception {
                return ImageIO.read(url);
            }
        }.get();
    }

    private abstract static class ResourceLoader<V> {

        final String name;

        public ResourceLoader(String name) {
            Validate.notNull(name, "name must not be null");
            this.name = name;
        }

        public final V get() throws MissingResourceException {
            URL url = Resource.class.getResource(name);
            try {
                return get(url);
            } catch (Exception e) {
                String className = Resource.class.getName();
                throw new MissingResourceException(e.getMessage(), className, name);
            }
        }

        protected abstract V get(URL url) throws Exception;
    }
}
