package org.jtools.util.props;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class LinkedProperties extends ResolvedProperties {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 4121133653859842096L;

    private static final String INTERNALKEY_CONFIG_DEFAULTENTRIESPROPERTY = "config-DefaultEntriesProperty";

    private static final String INTERNALKEY_CONFIG_DEFAULTLINKPROPERTY = "config-DefaultLinkProperty";

    private static final String INTERNALKEY_VALUE_DEFAULTENTRIESPROPERTY = "value-DefaultEntriesProperty";

    private static final String INTERNALKEY_VALUE_DEFAULTLINKPROPERTY = "value-DefaultLinkProperty";

    public LinkedProperties() {
        super();
    }

    public LinkedProperties(Properties defaults) {
        super(defaults);
    }

    public LinkedProperties(Properties defaults, SystemPropertyMode systemPropertyMode) {
        super(defaults, systemPropertyMode);
    }

    public LinkedProperties(Properties defaults, SystemPropertyMode systemPropertyMode, String systemPropertyPrefix) {
        super(defaults, systemPropertyMode, systemPropertyPrefix);
    }

    public LinkedProperties(String systemPropertyPrefix) {
        super(systemPropertyPrefix);
    }

    public LinkedProperties(SystemPropertyMode systemPropertyMode) {
        super(systemPropertyMode);
    }

    protected LinkedProperties(SimpleProperties source) {
        super(source);
    }

    public final String getDefaultEntriesProperty() {
        return getInternalStyle().getDefaultEntriesProperty();
    }

    public final String getDefaultLinkProperty() {
        return getInternalStyle().getDefaultLinkProperty();
    }

    @Override()
    protected final String getInternalKey(String key) {
        if (INTERNALKEY_VALUE_DEFAULTENTRIESPROPERTY.equals(key)) return getDefaultEntriesProperty();
        if (INTERNALKEY_VALUE_DEFAULTLINKPROPERTY.equals(key)) return getDefaultLinkProperty();
        return super.getInternalKey(key);
    }

    @Override()
    public Object clone() {
        return new LinkedProperties(this);
    }

    private LoadReturnCode loadChild(Map<Key, ValueItem> map, String fileOrUrl, LoadReturnCode defaultResult) throws IOException {
        try {
            URL childurl = getAsUrl(fileOrUrl);
            if (childurl == null) return defaultResult;
            InputStream childStream = childurl.openStream();
            fileOrUrl = childurl.toString();
            LinkedProperties child = new LinkedProperties();
            child.initFromParent(this);
            child.setFilename(fileOrUrl);
            int p = fileOrUrl.lastIndexOf('/');
            setLoadPath((p < 0) ? null : fileOrUrl.substring(0, p));
            Map<Key, ValueItem> childMap = new HashMap<Key, ValueItem>(map);
            removeLocalKeys(childMap);
            @SuppressWarnings("unused") LoadReturnCode childresult = child.onLoad(childMap, childStream);
            try {
                if (childStream != null) childStream.close();
            } catch (IOException ioex) {
            }
            childStream = null;
            map.putAll(childMap);
            return resolveMap(map);
        } catch (IOException ioe) {
            System.out.println(getFilename() + ": error loading childfile " + fileOrUrl);
            throw ioe;
        }
    }

    @Override()
    protected final void loadConfigProperties(Properties props) {
        setDefaultEntriesProperty(props.getProperty(PropertyConstants.PROPERTY_DEFAULTENTRIESPROPERTY, getDefaultEntriesProperty()));
        setDefaultLinkProperty(props.getProperty(PropertyConstants.PROPERTY_DEFAULTLINKPROPERTY, getDefaultLinkProperty()));
        props.remove(PropertyConstants.PROPERTY_DEFAULTENTRIESPROPERTY);
        props.remove(PropertyConstants.PROPERTY_DEFAULTLINKPROPERTY);
        super.loadConfigProperties(props);
    }

    @Override()
    protected final LoadReturnCode onLoad(Map<Key, ValueItem> map, InputStream inStream) throws IOException {
        if (map == null) throw new NullPointerException("map (CODE ERROR)");
        Properties props = new Properties();
        props.putAll(this);
        if (inStream != null) {
            props.load(inStream);
            loadConfigProperties(props);
        }
        buildMap(map, props);
        LoadReturnCode result = resolveMap(map);
        Key key = parseInternalKey(getDefaultLinkProperty());
        ValueItem value;
        if (key != null) {
            value = map.get(key);
            if (value != null) {
                if (!value.isSolved()) throw new RuntimeException(getFilename() + ": unresolved DefaultLinkProperty '" + value.getValue(this) + "'");
                result = loadChild(map, value.getValue(this), result);
            }
        }
        key = parseInternalKey(getDefaultEntriesProperty());
        if (key != null) {
            value = map.get(key);
            if (value != null) {
                if (!value.isSolved()) throw new RuntimeException(getFilename() + ": unresolved DefaultEntriesProperty '" + value.getValue(this) + "'");
                String entry;
                for (StringTokenizer st = new StringTokenizer(value.getValue(this), ",;"); st.hasMoreTokens(); ) {
                    entry = st.nextToken().trim();
                    if (!"".equals(entry)) {
                        key = parseKey(entry);
                        value = map.get(key);
                        if (value == null) result = loadChild(map, entry, result); else {
                            if (!value.isSolved()) throw new RuntimeException(getFilename() + ": unresolved DefaultEntryProperty '" + entry + "' = '" + value.getValue(this) + "'");
                            result = loadChild(map, value.getValue(this), result);
                        }
                    }
                }
            }
        }
        removeLocalKeys(map);
        return result;
    }

    @Override()
    protected final Key parseInternalKey(String v) {
        if ("".equals(v)) return null;
        String p = getDefaultEntriesProperty();
        if (v.equals(p)) return new Key(INTERNALKEY_VALUE_DEFAULTENTRIESPROPERTY, true, true, true, v);
        p = getDefaultLinkProperty();
        if (v.equals(p)) return new Key(INTERNALKEY_VALUE_DEFAULTLINKPROPERTY, true, true, true, v);
        if (PropertyConstants.PROPERTY_DEFAULTENTRIESPROPERTY.equals(v)) return new Key(INTERNALKEY_CONFIG_DEFAULTENTRIESPROPERTY, true, true, true, v);
        if (PropertyConstants.PROPERTY_DEFAULTLINKPROPERTY.equals(v)) return new Key(INTERNALKEY_CONFIG_DEFAULTLINKPROPERTY, true, true, true, v);
        return super.parseInternalKey(v);
    }

    public final void setDefaultEntriesProperty(String prop) {
        getInternalStyle().setDefaultEntriesProperty(prop);
    }

    public final void setDefaultLinkProperty(String prop) {
        getInternalStyle().setDefaultLinkProperty(prop);
    }

    @Override()
    protected final void setInternalEntries(Map<Key, ValueItem> map) {
        setMapEntry(map, parseInternalKey(PropertyConstants.PROPERTY_DEFAULTENTRIESPROPERTY), getDefaultEntriesProperty(), true);
        setMapEntry(map, parseInternalKey(PropertyConstants.PROPERTY_DEFAULTLINKPROPERTY), getDefaultLinkProperty(), true);
        super.setInternalEntries(map);
    }
}
