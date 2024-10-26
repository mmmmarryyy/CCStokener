package consciouscode.seedling.config.properties;

import consciouscode.seedling.NodeProvisioningException;
import consciouscode.seedling.NodeReference;
import consciouscode.seedling.config.ConfigEvaluator;
import consciouscode.seedling.config.ConfigResource;
import consciouscode.seedling.config.ConfigurationException;
import consciouscode.seedling.config.ConstructionContext;
import consciouscode.util.resource.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

/**
 * A configuration resource stored as a <code>.properties</code> file.
 * The {@link PropertiesConfigLoader} provides a default evaluator, but an
 * individual resource may reconfigure that via the {@code .evaluator}
 * meta-property.
 */
public class PropertiesConfigResource implements ConfigResource {

    /**
     * Names the meta-property that is used to configure the resource's
     * {@link ConfigEvaluator}. Its value must be a full path to an evaluator.
     */
    public static final String EVALUATOR_METAPROPERTY = ".evaluator";

    /**
     * Helper class to handle root configuration.
     * It simply overrides {@link #constructNode(ConstructionContext)}
     * to return the already-constructed root.
     */
    private static final class RootConfigEvaluator extends PropertiesConfigEvaluator {

        @Override
        public Object constructNode(ConstructionContext context) {
            return context.getLocation().getBaseBranch();
        }
    }

    /**
     * @param defaultEvaluator must not be null.
     */
    PropertiesConfigResource(Resource resource, InputStream propertiesData, PropertiesConfigEvaluator defaultEvaluator) throws IOException {
        defaultEvaluator.getClass();
        myResource = resource;
        myProperties = loadAndCloseStream(propertiesData);
        myDefaultEvaluator = defaultEvaluator;
    }

    /**
     * @param identifier identifies this resource; may be null.
     * @param properties must not be null.
     * @param defaultEvaluator must not be null.
     */
    public PropertiesConfigResource(String identifier, Properties properties, PropertiesConfigEvaluator defaultEvaluator) {
        properties.getClass();
        defaultEvaluator.getClass();
        myResource = null;
        myIdentifier = identifier;
        myProperties = properties;
        myDefaultEvaluator = defaultEvaluator;
    }

    public Resource getResource() {
        return myResource;
    }

    public synchronized String getIdentifier() {
        if (myIdentifier == null) {
            return myResource.getUrl().toString();
        }
        return myIdentifier;
    }

    public synchronized void setIdentifier(String id) {
        myIdentifier = id;
    }

    public ConfigEvaluator getEvaluator(NodeReference location) throws ConfigurationException {
        if (location.isRoot()) {
            return new RootConfigEvaluator();
        }
        String evalPath = myProperties.getProperty(EVALUATOR_METAPROPERTY);
        if (evalPath != null) {
            if (evalPath.length() == 0) {
                String reason = "No value for " + EVALUATOR_METAPROPERTY;
                throw new ConfigurationException(location, getIdentifier(), reason);
            }
            Object eval;
            try {
                NodeReference evaluatorReference = location.parent().path(evalPath);
                eval = evaluatorReference.required();
            } catch (NodeProvisioningException e) {
                String reason = "Unable to load evaluator from " + evalPath;
                throw new ConfigurationException(location, getIdentifier(), reason, e);
            }
            if (!(eval instanceof ConfigEvaluator)) {
                String reason = "Evaluator isn't a " + ConfigEvaluator.class.getSimpleName();
                throw new ConfigurationException(location, getIdentifier(), reason);
            }
            return (ConfigEvaluator) eval;
        }
        return myDefaultEvaluator;
    }

    public void writeConfiguration(Writer out) throws IOException {
        if (myResource == null) {
            out.append("# Unable to print configuration resource\n");
        } else {
            URL url = myResource.getUrl();
            InputStream in = url.openStream();
            if (in != null) {
                try {
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            } else {
                out.append("# Unable to print configuration resource\n");
            }
        }
    }

    /**
     * Gets the contents of this config resource, as a Java {@link Properties}
     * collection.
     *
     * @return not {@code null}.
     */
    public Properties getProperties() {
        return myProperties;
    }

    /**
     * Returns the unevaluated text of a specified property, or null if no
     * value is assigned by this resource.
     */
    public String getProperty(String property) {
        return myProperties.getProperty(property, null);
    }

    @Override
    public String toString() {
        return "[PropertiesConfigResource " + myResource + ']';
    }

    private Properties loadAndCloseStream(InputStream in) throws IOException {
        Properties props = new Properties();
        try {
            props.load(in);
        } finally {
            in.close();
        }
        return props;
    }

    private final Resource myResource;

    private String myIdentifier;

    private final Properties myProperties;

    private final PropertiesConfigEvaluator myDefaultEvaluator;
}
