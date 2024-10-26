package cn.shining365.webclips.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.cyberneko.html.parsers.SAXParser;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.XMLReader;

public class Utils {

    private static Configuration configuration = null;

    static {
        try {
            ConfigurationFactory factory = new ConfigurationFactory();
            factory.setConfigurationURL(ConfigurationFactory.class.getResource("/configurationSources.xml"));
            configuration = factory.getConfiguration();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Return the configuration of this application.
	 * 
	 * @return The configuration of this application.
	 */
    public static Configuration getConfiguration() {
        return configuration;
    }

    public static byte[] readUrl(URL url) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        InputStream is = url.openStream();
        try {
            IOUtils.copy(is, os);
            return os.toByteArray();
        } finally {
            is.close();
        }
    }

    public static Document htm2Dom(String htm) throws DocumentException, IOException {
        XMLReader xmlReader = new SAXParser();
        SAXReader reader = new SAXReader(xmlReader);
        reader.setMergeAdjacentText(true);
        Reader r = new StringReader(htm);
        try {
            return reader.read(r);
        } finally {
            r.close();
        }
    }

    /**把相对链接转换为绝对链接*/
    @SuppressWarnings("unchecked")
    public static Node resolveHtmlUrl(String baseUrl, Node htmlNode) {
        if (baseUrl.matches("(?i)^http://[^/]+")) {
            baseUrl += "/";
        }
        URI uri = URI.create(baseUrl);
        List<Attribute> linkAttrs = new ArrayList<Attribute>();
        linkAttrs.addAll(htmlNode.selectNodes("//@href"));
        linkAttrs.addAll(htmlNode.selectNodes("//@src"));
        for (Attribute attr : linkAttrs) {
            try {
                attr.setValue(uri.resolve(attr.getValue().replace(' ', '+')).toString());
            } catch (IllegalArgumentException e) {
                logger.debug("ignored: " + attr.getValue());
            }
        }
        return htmlNode;
    }

    public static String toMulPath(String uniPath) {
        return uniPath.replaceAll("\\[\\d+\\]", "");
    }

    public static String[] toPathlets(String path) {
        return path.substring(1).split("/");
    }

    /**参数名和值都被解码了，以便用户阅读*/
    public static Map<String, String> getUrlParamMap(String url, String encoding) throws UnsupportedEncodingException {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (url.contains("?")) {
            for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
                Matcher m = Pattern.compile("(.+)=(.+)").matcher(pair);
                if (!m.matches()) {
                    continue;
                }
                paramMap.put(URLDecoder.decode(m.group(1), encoding), URLDecoder.decode(m.group(2), encoding));
            }
        }
        return paramMap;
    }

    private static Logger logger = Logger.getLogger(Utils.class);
}
