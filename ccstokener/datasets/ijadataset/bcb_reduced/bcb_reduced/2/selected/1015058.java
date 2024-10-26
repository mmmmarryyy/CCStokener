package org.dozer.loader.xml;

import org.dozer.classmap.MappingFileData;
import org.dozer.config.BeanContainer;
import org.dozer.loader.MappingsSource;
import org.dozer.util.DozerClassLoader;
import org.dozer.util.MappingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Internal class that reads and parses a single custom mapping xml file into raw ClassMap objects. Only intended for
 * internal use.
 *
 * @author tierney.matt
 * @author garsombke.franz
 */
public class MappingFileReader {

    private static final Logger log = LoggerFactory.getLogger(MappingFileReader.class);

    private final DocumentBuilder documentBuilder;

    public MappingFileReader(XMLParserFactory parserFactory) {
        documentBuilder = parserFactory.createParser();
    }

    public MappingFileData read(String fileName) {
        DozerClassLoader classLoader = BeanContainer.getInstance().getClassLoader();
        URL url = classLoader.loadResource(fileName);
        return read(url);
    }

    public MappingFileData read(URL url) {
        MappingFileData result = null;
        InputStream stream = null;
        try {
            stream = url.openStream();
            Document document = documentBuilder.parse(stream);
            MappingsSource parser = new XMLParser(document);
            result = parser.load();
        } catch (Throwable e) {
            log.error("Error while loading dozer mapping file url: [" + url + "]", e);
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
