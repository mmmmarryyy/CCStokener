package net.sf.jimo.loader.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;
import net.sf.jimo.common.filtermap.PropertiesParser;
import net.sf.jimo.common.resolver.ResolvingMap;
import net.sf.jimo.common.resolver.VariableResolver;
import net.sf.jimo.loader.xmlutil.DocUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * Type: <strong><code>net.sf.jimo.loader.xml.XmlPropertiesReader</code></strong>
 * </p>
 * 
 * Read properties from xml or .properties file; store them as a hashmap.
 *  
 *
 * @version $Rev$
 * @author logicfish
 * @since 0.2
 * 
 * @see net.sf.jimo.common.filtermap.PropertiesParser 
 * @see XmlPropertiesWriter
 * 
 */
public class XmlPropertiesReader {

    private static final String PROPERTYNAME_XPATH = "@key";

    private static final String PROPERTYVALUE_XPATH = "value/text()";

    /**
	 * Read properties into the map from a .properties file
	 * @throws IOException 
	 */
    public static void loadPropertiesMap(Map<String, Object> result, String propertiesURI) throws IOException {
        URL url;
        Properties props = new Properties();
        Reader in = null;
        try {
            url = new URL(propertiesURI);
            URLConnection urlConnect = url.openConnection();
            in = new InputStreamReader(urlConnect.getInputStream());
        } catch (MalformedURLException e) {
            in = new InputStreamReader(new FileInputStream(propertiesURI));
        }
        props.load(in);
        for (Enumeration<?> enumeration = props.keys(); enumeration.hasMoreElements(); ) {
            String key = enumeration.nextElement().toString();
            Object o = PropertiesParser.getPropertyValue(props.getProperty(key));
            result.put(key, o);
        }
    }

    public static void getPropertiesMap(Object node, String propertyElement, Map<String, Object> props) {
        NodeList nodeList;
        try {
            nodeList = DocUtil.selectNodes(propertyElement, node);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            String name;
            try {
                name = DocUtil.selectString(PROPERTYNAME_XPATH, item);
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
            Object value = getValue(item);
            props.put(name, value);
        }
    }

    public static Object getValue(Node item) {
        try {
            NodeList nodes = DocUtil.selectNodes(PROPERTYVALUE_XPATH, item);
            if (nodes.getLength() == 1) {
                String value = DocUtil.selectString(PROPERTYVALUE_XPATH, item);
                return PropertiesParser.getPropertyValue(value);
            } else {
                Object[] result = new Object[nodes.getLength()];
                for (int i = 0; i < nodes.getLength(); ++i) {
                    result[i] = PropertiesParser.getPropertyValue(nodes.item(i).getNodeValue());
                }
                return result;
            }
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }
}
