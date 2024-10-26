package net.sf.dozer.util.mapping.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.sf.dozer.util.mapping.classmap.Mappings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal class that reads and parses a single custom mapping xml file into raw ClassMap objects. Only intended for
 * internal use.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 */
public class MappingFileReader {

    private static final Log log = LogFactory.getLog(MappingFileReader.class);

    private final URL url;

    public MappingFileReader(URL url) {
        this.url = url;
    }

    public MappingFileReader(String fileName) {
        ResourceLoader loader = new ResourceLoader();
        url = loader.getResource(fileName);
    }

    public Mappings read() {
        Mappings result = null;
        InputStream stream = null;
        try {
            XMLParser parser = new XMLParser();
            stream = url.openStream();
            result = parser.parse(stream);
        } catch (Throwable e) {
            log.error("Error in loading dozer mapping file url: [" + url + "] : " + e);
            MappingUtils.throwMappingException(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                MappingUtils.throwMappingException(e);
            }
        }
        return result;
    }
}
