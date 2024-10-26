package consciouscode.seedling.config.properties;

import consciouscode.seedling.NodePath;
import consciouscode.seedling.config.ConfigEvaluator;
import consciouscode.seedling.config.ConfigLoader;
import consciouscode.seedling.config.ConfigLoadingException;
import consciouscode.seedling.config.ConfigResource;
import consciouscode.util.resource.Resource;
import consciouscode.util.resource.ResourceTree;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Configuration loader for files with suffix {@code ".properties"}.
 */
public class PropertiesConfigLoader implements ConfigLoader {

    private final String SUFFIX = ".properties";

    private final int SUFFIX_LENGTH = SUFFIX.length();

    public PropertiesConfigLoader(PropertiesConfigEvaluator defaultEvaluator) {
        myDefaultEvaluator = defaultEvaluator;
    }

    public ConfigEvaluator getDefaultEvaluator() {
        return myDefaultEvaluator;
    }

    public String nodeNameForResource(String resourceName) {
        int length = resourceName.length();
        if (length > SUFFIX_LENGTH && resourceName.endsWith(SUFFIX)) {
            return resourceName.substring(0, length - SUFFIX_LENGTH);
        }
        return null;
    }

    public ConfigResource loadConfigResource(ResourceTree resources, NodePath nodeAddress) throws ConfigLoadingException {
        String fileName = nodeAddress + SUFFIX;
        Resource resource = resources.getResource(fileName);
        if (resource == null) return null;
        URL url = resource.getUrl();
        try {
            InputStream inStream = url.openStream();
            if (inStream != null) {
                return new PropertiesConfigResource(resource, inStream, myDefaultEvaluator);
            }
        } catch (IOException e) {
            throw new ConfigLoadingException(url.toString(), null, e);
        }
        return null;
    }

    private final PropertiesConfigEvaluator myDefaultEvaluator;
}
