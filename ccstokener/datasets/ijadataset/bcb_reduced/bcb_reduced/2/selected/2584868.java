package mx.bigdata.jcalais.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisConfig;
import mx.bigdata.jcalais.CalaisConfig.ConnParam;
import mx.bigdata.jcalais.CalaisConfig.ProcessingParam;
import mx.bigdata.jcalais.CalaisConfig.UserParam;
import mx.bigdata.jcalais.CalaisException;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;

public final class CalaisRestClient implements CalaisClient {

    private static final String RESOURCE = "http://api.opencalais.com/enlighten/rest/";

    private static final String TYPE = "application/x-www-form-urlencoded";

    private static final int MAX_CONTENT_SIZE = 100000;

    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;

    public CalaisRestClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public CalaisResponse analyze(URL url) throws IOException {
        return analyze(url, new CalaisConfig());
    }

    public CalaisResponse analyze(URL url, CalaisConfig config) throws IOException {
        config.set(UserParam.EXTERNAL_ID, url.toString());
        config.set(ProcessingParam.CONTENT_TYPE, "TEXT/HTML");
        return analyze(new InputStreamReader(url.openStream()), config);
    }

    public CalaisResponse analyze(Readable readable) throws IOException {
        return analyze(readable, new CalaisConfig());
    }

    public CalaisResponse analyze(Readable readable, CalaisConfig config) throws IOException {
        return analyze(CharStreams.toString(readable), config);
    }

    public CalaisResponse analyze(String content) throws IOException {
        return analyze(content, new CalaisConfig());
    }

    public CalaisResponse analyze(String content, CalaisConfig config) throws IOException {
        if (Strings.isNullOrEmpty(content) || content.length() > MAX_CONTENT_SIZE) {
            throw new IllegalArgumentException("Invalid content, either empty or " + "exceeds maximum allowed size");
        }
        Map<String, String> formData = Maps.newHashMap();
        formData.put("licenseID", apiKey);
        formData.put("content", content);
        formData.put("paramsXML", config.getParamsXml());
        String payload = post(formData, config);
        try {
            Map<String, Object> map = mapper.readValue(payload, Map.class);
            return processResponse(map, payload);
        } catch (JsonParseException e) {
            throw parseError(payload);
        }
    }

    private String post(Map<String, String> formData, CalaisConfig config) throws IOException {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> me : formData.entrySet()) {
            data.append(URLEncoder.encode(me.getKey(), "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(me.getValue(), "UTF-8"));
            data.append("&");
        }
        data.deleteCharAt(data.length() - 1);
        URL url = new URL(RESOURCE);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(config.get(ConnParam.CONNECT_TIMEOUT));
        conn.setReadTimeout(config.get(ConnParam.CONNECT_TIMEOUT));
        conn.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        try {
            out.write(data.toString());
            out.flush();
        } finally {
            out.close();
        }
        Reader in = new InputStreamReader(conn.getInputStream());
        try {
            return CharStreams.toString(in);
        } finally {
            in.close();
        }
    }

    public static CalaisResponse processResponse(Map<String, Object> map, final String payload) {
        Map<String, Object> doc = (Map<String, Object>) map.remove("doc");
        final CalaisObject info = extractObject(doc, "info");
        final CalaisObject meta = extractObject(doc, "meta");
        Multimap<String, CalaisObject> hierarchy = createHierarchy(map);
        final Iterable<CalaisObject> topics = Iterables.unmodifiableIterable(hierarchy.get("topics"));
        final Iterable<CalaisObject> entities = Iterables.unmodifiableIterable(hierarchy.get("entities"));
        final Iterable<CalaisObject> relations = Iterables.unmodifiableIterable(hierarchy.get("relations"));
        final Iterable<CalaisObject> socialTags = Iterables.unmodifiableIterable(hierarchy.get("socialTag"));
        return new CalaisResponse() {

            public CalaisObject getInfo() {
                return info;
            }

            public CalaisObject getMeta() {
                return meta;
            }

            public Iterable<CalaisObject> getTopics() {
                return topics;
            }

            public Iterable<CalaisObject> getEntities() {
                return entities;
            }

            public Iterable<CalaisObject> getRelations() {
                return relations;
            }

            public Iterable<CalaisObject> getSocialTags() {
                return socialTags;
            }

            public String getPayload() {
                return payload;
            }
        };
    }

    private static CalaisObject extractObject(Map<String, Object> map, String key) {
        return new MapBasedCalaisObject((Map<String, Object>) map.remove(key));
    }

    private static final class MapBasedCalaisObject implements CalaisObject {

        private final Map<String, Object> map;

        private MapBasedCalaisObject(Map<String, Object> map) {
            this.map = ImmutableMap.copyOf(map);
        }

        public String getField(String field) {
            Object o = map.get(field);
            return (o == null) ? null : o.toString();
        }

        public Iterable getList(String field) {
            Object o = map.get(field);
            return (o instanceof Iterable) ? Iterables.unmodifiableIterable((Iterable) o) : null;
        }

        public Iterable<String> getFieldNames() {
            return Iterables.unmodifiableIterable(map.keySet());
        }

        public String toString() {
            return map.toString();
        }
    }

    private void resolveReferences(Map<String, Object> root) {
        for (Object o : root.values()) {
            Map<String, Object> map = (Map<String, Object>) o;
            for (Map.Entry<String, Object> me : map.entrySet()) {
                String key = me.getKey();
                Object o2 = me.getValue();
                if (o2 instanceof String) {
                    String value = (String) o2;
                    if (value.startsWith("http://") && root.containsKey(value)) {
                        map.put(key, root.get(value));
                    }
                }
            }
        }
    }

    private static Multimap<String, CalaisObject> createHierarchy(Map<String, Object> root) {
        Multimap<String, CalaisObject> result = ArrayListMultimap.create();
        for (Map.Entry<String, Object> me : root.entrySet()) {
            Map<String, Object> map = (Map<String, Object>) me.getValue();
            map.put("_uri", me.getKey());
            String group = (String) map.get("_typeGroup");
            result.put(group, new MapBasedCalaisObject(map));
        }
        return result;
    }

    private CalaisException parseError(String error) {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            StringReader reader = new StringReader(error);
            Document doc = builder.parse(new InputSource(reader));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            String method = (String) xpath.evaluate("/Error/@Method", doc, XPathConstants.STRING);
            String calaisRequestID = (String) xpath.evaluate("/Error/@calaisRequestID", doc, XPathConstants.STRING);
            String creationDate = (String) xpath.evaluate("/Error/@CreationDate", doc, XPathConstants.STRING);
            String calaisVersion = (String) xpath.evaluate("/Error/@CalaisVersion", doc, XPathConstants.STRING);
            String exception = (String) xpath.evaluate("/Error/Exception/text()", doc, XPathConstants.STRING);
            return new CalaisException(method, calaisRequestID, creationDate, calaisVersion, exception);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse exception", e);
        }
    }
}
