package de.tum.in.botl.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class BotlPropertiesImpl implements BotlProperties {

    private Properties types2BasicTypes;

    private Properties defaultValuesForTypes;

    public BotlPropertiesImpl(String basicTypesFileName, String defValuesFileName) throws IOException {
        types2BasicTypes = new Properties();
        defaultValuesForTypes = new Properties();
        loadProperties(basicTypesFileName, types2BasicTypes);
        loadProperties(defValuesFileName, defaultValuesForTypes);
    }

    private void loadProperties(String fileName, Properties properties) throws IOException {
        URL url = ClassLoader.getSystemResource(fileName);
        InputStream in = url.openStream();
        properties.load(in);
    }

    public final Properties getDefaultValuesForTypes() {
        return defaultValuesForTypes;
    }

    public final Properties getTypes2BasicTypes() {
        return types2BasicTypes;
    }
}
