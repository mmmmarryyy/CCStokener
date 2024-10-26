package ajaxnet4j;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.commons.logging.*;

class Settings {

    private static Settings defaultInstance = new Settings();

    private static Log log = LogFactory.getLog(Settings.class);

    public static Settings getDefault() {
        return defaultInstance;
    }

    private String path;

    private boolean useAnnotation;

    private HashMap<String, MetaDataClass> metaDatas;

    private HashMap<String, String> urlClassMappings;

    private HashMap<String, String> urlPackageMappings;

    private ArrayList<IJavaScriptConverter> jsonConverters;

    private ArrayList<String> jsonConvertersTypes;

    private ArrayList<String> jsonConvertersTypesRemoved;

    private HashMap<String, String> scriptReplacements;

    private boolean debugEnabled;

    private Encryption encryption;

    private String scriptCharsetDefault;

    Settings() {
        path = "~/ajaxnet4j";
        useAnnotation = true;
        metaDatas = new HashMap<String, MetaDataClass>();
        urlClassMappings = new HashMap<String, String>();
        urlPackageMappings = new HashMap<String, String>();
        jsonConverters = new ArrayList<IJavaScriptConverter>();
        jsonConvertersTypes = new ArrayList<String>();
        jsonConvertersTypesRemoved = new ArrayList<String>();
        debugEnabled = false;
        scriptReplacements = new HashMap<String, String>();
        encryption = null;
        scriptCharsetDefault = "UTF-8";
    }

    public final String getPath() {
        return path;
    }

    public final boolean getUseAnnotation() {
        return useAnnotation;
    }

    public final HashMap<String, MetaDataClass> MetaDatas() {
        return metaDatas;
    }

    public final HashMap<String, String> UrlClassMappings() {
        return urlClassMappings;
    }

    public final HashMap<String, String> UrlPackageMappings() {
        return urlPackageMappings;
    }

    public final HashMap<String, String> ScriptReplacements() {
        return scriptReplacements;
    }

    public final ArrayList<IJavaScriptConverter> JsonConverters() {
        return jsonConverters;
    }

    public final boolean getDebugEnabled() {
        return debugEnabled;
    }

    public final Encryption getEncryption() {
        return encryption;
    }

    public final String getScriptCharsetDefault() {
        return scriptCharsetDefault;
    }

    public final void loadFromConfigXml(URL url) {
        InputStream input = null;
        try {
            InputSource inputSource = new InputSource(url.toExternalForm());
            input = url.openStream();
            inputSource.setByteStream(input);
            XMLReader xr = XMLReaderFactory.createXMLReader();
            Settings.SAXDocumentHandler handler = new SAXDocumentHandler(this);
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(inputSource);
        } catch (Exception ex) {
            if (log.isErrorEnabled()) log.error("ajaxnet4j configuration load fails. Use default configurations.", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ex) {
                }
            }
        }
        initJsonConverters();
        jsonConvertersTypes.clear();
        jsonConvertersTypesRemoved.clear();
    }

    public IJavaScriptConverter GetDeserializableConverterFor(Class<?> type) {
        AjaxJavaScriptConverter att = type.getAnnotation(AjaxJavaScriptConverter.class);
        if (att != null) {
            try {
                Class<?> jscType = Class.forName(att.value());
                IJavaScriptConverter jsc = (IJavaScriptConverter) jscType.newInstance();
                if (canDeserialize(jsc, type)) {
                    return jsc;
                }
            } catch (Exception ex) {
                if (log.isErrorEnabled()) log.error("Loading Custom JavaScriptConverter '" + att.value() + "' fails.", ex);
            }
        }
        Iterator<IJavaScriptConverter> it = jsonConverters.iterator();
        while (it.hasNext()) {
            IJavaScriptConverter jsc = it.next();
            if (canDeserialize(jsc, type)) {
                return jsc;
            }
        }
        return null;
    }

    public IJavaScriptConverter GetSerializableConverterFor(Class<?> type) {
        AjaxJavaScriptConverter att = type.getAnnotation(AjaxJavaScriptConverter.class);
        if (att != null) {
            try {
                Class<?> jscType = Class.forName(att.value());
                IJavaScriptConverter jsc = (IJavaScriptConverter) jscType.newInstance();
                if (canSerialize(jsc, type)) {
                    return jsc;
                }
            } catch (Exception ex) {
                if (log.isErrorEnabled()) log.error("Loading Custom JavaScriptConverter '" + att.value() + "' fails.", ex);
            }
        }
        Iterator<IJavaScriptConverter> it = jsonConverters.iterator();
        while (it.hasNext()) {
            IJavaScriptConverter jsc = it.next();
            if (canSerialize(jsc, type)) {
                return jsc;
            }
        }
        return null;
    }

    private boolean canDeserialize(IJavaScriptConverter jsc, Class<?> type) {
        Class<?>[] types = jsc.getDeserializableTypes();
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i] == null) continue;
                if (types[i].isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canSerialize(IJavaScriptConverter jsc, Class<?> type) {
        Class<?>[] types = jsc.getSerializableTypes();
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i] == null) continue;
                if (types[i].isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    final void setPath(String value) {
        path = value;
    }

    final void setUseAnnotation(boolean value) {
        useAnnotation = value;
    }

    final void setDebugEnabled(boolean value) {
        debugEnabled = value;
    }

    final void setEncryption(Encryption value) {
        encryption = value;
    }

    final void setScriptCharsetDefault(String value) {
        scriptCharsetDefault = value;
    }

    final ArrayList<String> JsonConvertersTypes() {
        return jsonConvertersTypes;
    }

    final ArrayList<String> JsonConvertersTypesRemoved() {
        return jsonConvertersTypesRemoved;
    }

    final void addUrlClassMapping(String className, String url) {
        if (className.endsWith(".*")) {
            if (url.endsWith(".*")) {
                if (urlPackageMappings.containsKey(className)) {
                    throw new RuntimeException("urlClassMapping duplicated: " + className);
                }
                urlPackageMappings.put(url.substring(0, url.length() - 2), className.substring(0, className.length() - 2));
            } else {
                throw new RuntimeException("urlClassMapping not matches: " + className + " -> " + url);
            }
        } else {
            if (url.endsWith(".*")) {
                throw new RuntimeException("urlClassMapping not matches: " + className + " -> " + url);
            } else {
                if (urlClassMappings.containsKey(className)) {
                    throw new RuntimeException("urlClassMapping duplicated: " + className);
                }
                urlClassMappings.put(url, className);
            }
        }
    }

    final void initJsonConverters() {
        loadBuildInJsonConverter("ajaxnet4j.converters.DateConverter");
        loadBuildInJsonConverter("ajaxnet4j.converters.ListConverter");
        loadBuildInJsonConverter("ajaxnet4j.converters.MapConverter");
        loadBuildInJsonConverter("ajaxnet4j.converters.IteratorConverter");
        Iterator<String> it = jsonConvertersTypes.iterator();
        while (it.hasNext()) {
            loadJsonConverter(it.next());
        }
    }

    final void loadBuildInJsonConverter(String className) {
        if (!(jsonConvertersTypesRemoved.contains(className))) {
            loadJsonConverter(className);
        }
    }

    final void loadJsonConverter(String className) {
        try {
            Class<?> c = Class.forName(className);
            IJavaScriptConverter jsc = (IJavaScriptConverter) c.newInstance();
            jsc.Initialize();
            jsonConverters.add(jsc);
            if (log.isTraceEnabled()) log.trace("JSON Converter'" + className + "' loaded.");
        } catch (Exception ex) {
            if (log.isErrorEnabled()) log.error("Loading JSON Converter '" + className + "' fails.", ex);
        }
    }

    class SAXDocumentHandler extends DefaultHandler {

        private LinkedList<String> levels;

        private MetaDataClass metaDataClass;

        private Settings settings;

        public SAXDocumentHandler(Settings settings) {
            super();
            levels = new LinkedList<String>();
            this.settings = settings;
        }

        public void startDocument() {
            levels.clear();
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String tmp;
            if (log.isTraceEnabled()) log.trace(" ajaxnet4j config XML parsing - qName: " + qName);
            if ("ajaxnet4j-config".equals(qName)) {
                if (levels.size() != 0) {
                    FormatError();
                }
                tmp = attributes.getValue("path");
                if (tmp != null) {
                    if (tmp.endsWith("/")) {
                        tmp = tmp.substring(0, tmp.length() - 1);
                    }
                    settings.setPath(tmp);
                }
            } else if ("metaDatas".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
                tmp = attributes.getValue("useAnnotation");
                if ("false".equals(tmp)) {
                    settings.setUseAnnotation(false);
                }
            } else if ("class".equals(qName)) {
                if (!"metaDatas".equals(levels.getLast())) {
                    FormatError();
                }
                String className = attributes.getValue("name");
                if (className == null) {
                    FormatError();
                }
                MetaDataClass c0 = new MetaDataClass(className);
                c0.setClientAlias(attributes.getValue("clientAlias"));
                tmp = attributes.getValue("allPublicMethods");
                if ("true".equals(tmp)) {
                    c0.setAllPublicMethods(true);
                }
                HashMap<String, MetaDataClass> metaDatas = settings.MetaDatas();
                if (metaDatas.containsKey(className)) {
                    throw new RuntimeException("Duplicate classes in metaDatas: " + className);
                }
                metaDatas.put(className, c0);
                metaDataClass = c0;
            } else if ("method".equals(qName)) {
                if (!"class".equals(levels.getLast())) {
                    FormatError();
                }
                String methodName = attributes.getValue("name");
                if (methodName == null) {
                    FormatError();
                }
                if (metaDataClass.Methods().containsKey(methodName)) {
                    throw new RuntimeException("Duplicate methods in metaDatas: " + methodName);
                }
                MetaDataMethod m0 = new MetaDataMethod(methodName);
                m0.setClientAlias(attributes.getValue("clientAlias"));
                tmp = attributes.getValue("serverCacheSeconds");
                if (tmp != null) {
                    try {
                        int seconds = Integer.parseInt(tmp);
                        m0.setServerCacheSeconds(seconds);
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException("Number Parsing Error: " + tmp);
                    }
                }
                metaDataClass.Methods().put(methodName, m0);
            } else if ("urlClassMappings".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
            } else if ("jsonConverters".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
            } else if ("add".equals(qName)) {
                if ("urlClassMappings".equals(levels.getLast())) {
                    String type = attributes.getValue("type");
                    String path = attributes.getValue("path");
                    if (type == null || path == null) {
                        FormatError();
                    }
                    settings.addUrlClassMapping(type, path);
                } else if ("jsonConverters".equals(levels.getLast())) {
                    tmp = attributes.getValue("type");
                    if (tmp == null) {
                        FormatError();
                    }
                    if (settings.JsonConvertersTypes().contains(tmp)) {
                        throw new RuntimeException("JsonConverters duplicated: " + tmp);
                    }
                    if (settings.JsonConvertersTypesRemoved().contains(tmp)) {
                        settings.JsonConvertersTypesRemoved().remove(tmp);
                    } else if (tmp.startsWith("ajaxnet4j.")) {
                        settings.JsonConvertersTypes().add(tmp);
                    }
                } else {
                    FormatError();
                }
            } else if ("remove".equals(qName)) {
                if (!"jsonConverters".equals(levels.getLast())) {
                    FormatError();
                } else {
                    tmp = attributes.getValue("type");
                    if (tmp == null) {
                        FormatError();
                    }
                    if (settings.JsonConvertersTypesRemoved().contains(tmp)) {
                        throw new RuntimeException("JsonConverters duplicated: " + tmp);
                    }
                    if (settings.JsonConvertersTypes().contains(tmp)) {
                        settings.JsonConvertersTypes().remove(tmp);
                    } else if (tmp.startsWith("ajaxnet4j.")) {
                        settings.JsonConvertersTypesRemoved().add(tmp);
                    }
                }
            } else if ("scriptReplacements".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
            } else if ("file".equals(qName)) {
                if (!"scriptReplacements".equals(levels.getLast())) {
                    FormatError();
                }
                tmp = attributes.getValue("name");
                if (tmp == null) {
                    FormatError();
                }
                if (settings.ScriptReplacements().containsKey(tmp)) {
                    throw new RuntimeException("ScriptReplacement duplicated: " + tmp);
                }
                settings.ScriptReplacements().put(tmp, attributes.getValue("path"));
            } else if ("debug".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
                tmp = attributes.getValue("enabled");
                if ("true".equals(tmp)) {
                    settings.setDebugEnabled(true);
                }
            } else if ("encryption".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
                String cryptType = attributes.getValue("cryptType");
                String keyType = attributes.getValue("keyType");
                if (cryptType != null || keyType != null) {
                    if ("".equals(cryptType) && "".equals(keyType)) {
                    } else if (!"".equals(cryptType) && !"".equals(keyType)) {
                        Encryption encryption = new Encryption(cryptType, keyType);
                        if (encryption.init()) {
                            settings.setEncryption(encryption);
                        } else {
                            throw new RuntimeException("Encryption configuration failed.");
                        }
                    } else {
                        FormatError();
                    }
                }
            } else if ("scriptCharset".equals(qName)) {
                if (!"ajaxnet4j-config".equals(levels.getLast())) {
                    FormatError();
                }
                tmp = attributes.getValue("default");
                if (tmp != null) {
                    settings.setScriptCharsetDefault(tmp);
                }
            }
            levels.add(qName);
        }

        public void endElement(String uri, String localName, String qName) {
            levels.removeLast();
        }

        private void FormatError() {
            String tmp = "";
            Iterator<String> iterator = levels.iterator();
            while (iterator.hasNext()) {
                tmp = tmp + "/" + iterator.next();
            }
            throw new RuntimeException("Configuration file format error. Element: " + tmp);
        }
    }
}
