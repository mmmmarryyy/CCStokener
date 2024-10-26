package uk.co.zenly.jllama;

import java.io.IOException;
import java.net.URL;
import org.apache.log4j.Logger;
import uk.co.zenly.jllama.workers.Runner;

/**
 * Extension of Properties to load our jLlama properties file
 * 
 * @author dougg
 *
 */
@SuppressWarnings("serial")
public class LocalProperties extends java.util.Properties {

    static Logger logger = Logger.getLogger(Runner.class.getName());

    public LocalProperties() {
        URL url = ClassLoader.getSystemResource("jllama.properties");
        try {
            this.load(url.openStream());
        } catch (IOException ex) {
            logger.error("Failed to load properties file");
        }
    }
}
