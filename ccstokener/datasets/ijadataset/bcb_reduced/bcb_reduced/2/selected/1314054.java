package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.*;

public class RTProperties implements Cloneable, StringTools.KeyValueMap {

    private static final boolean USE_PROPERTIES_LOADER = true;

    private static final String INCLUDE_PROTOCOL_FILE = "file";

    private static final String INCLUDE_PROTOCOL_HTTP = "http";

    private static final String INCLUDE_PROTOCOL_HTTPS = "https";

    public static final char NameSeparatorChar = ':';

    public static final char KeyValSeparatorChar = StringTools.KeyValSeparatorChar;

    public static final char PropertySeparatorChar = StringTools.PropertySeparatorChar;

    public static final char ARRAY_DELIM = StringTools.ARRAY_DELIM;

    public static final String KEY_START_DELIMITER = "${";

    public static final String KEY_END_DELIMITER = "}";

    public static final String KEY_DFT_DELIMITER = "=";

    public static final int KEY_MAX_RECURSION = 6;

    public static final int KEY_REPLACEMENT_NONE = 0;

    public static final int KEY_REPLACEMENT_LOCAL = 1;

    public static final int KEY_REPLACEMENT_GLOBAL = 2;

    private static final boolean DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY = true;

    public static final String KEYVAL_PREFIX = "-";

    public static final char KEYVAL_PREFIX_CHAR = '-';

    public static final char KEYVAL_SEPARATOR_CHAR_1 = '=';

    public static final char KEYVAL_SEPARATOR_CHAR_2 = ':';

    /**
    *** Returns the index of the key/value separator (either '=' or ':').
    *** @param kv  The String parsed for the key/value separator
    *** @return The index of the key/value separator
    **/
    private int _indexOfKeyValSeparator(String kv) {
        for (int i = 0; i < kv.length(); i++) {
            char ch = kv.charAt(i);
            if ((ch == KEYVAL_SEPARATOR_CHAR_1) || (ch == KEYVAL_SEPARATOR_CHAR_2)) {
                return i;
            }
        }
        return -1;
    }

    private String cfgDirRoot = null;

    private Map<Object, Object> cfgProperties = null;

    private boolean ignoreCase = false;

    private boolean allowBlankValues = true;

    private char propertySeparator = PropertySeparatorChar;

    private char keyValueSeparator = KeyValSeparatorChar;

    private int keyReplacementMode = KEY_REPLACEMENT_NONE;

    private int nextCmdLineArg = -1;

    private boolean enableConfigLogMessages = true;

    /**
    *** Constructor
    *** @param map  The Object key/value map used to initialize this instance
    **/
    public RTProperties(Map<?, ?> map) {
        this.setBackingProperties(map);
    }

    /**
    *** Constructor
    **/
    public RTProperties() {
        this((Map<Object, Object>) null);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    **/
    public RTProperties(String props) {
        this();
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param inclName True to parse and set the name of this instance.
    **/
    public RTProperties(String props, boolean inclName) {
        this();
        this.setProperties(props, inclName);
    }

    /**
    *** Constructor
    *** @param props  A String containing "key=value key=value ..." specifications used to
    ***               initialize this instance.
    *** @param propSep The separator character between one "key=value" pair and the next.
    ***                (ie. in "key=value;key=value", ';' is the property separator)
    **/
    public RTProperties(String props, char propSep) {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param props     A String containing "key=value key=value ..." specifications used to
    ***                  initialize this instance.
    *** @param propSep   The separator character between one "key=value" pair and the next.
    ***                  (ie. in "key=value;key=value", ';' is the property separator)
    *** @param keyValSep The separator character between the property "key" and "value".
    ***                  (ie. in "key=value", ':' is the key/value separator)
    **/
    public RTProperties(String props, char propSep, char keyValSep) {
        this();
        this.setPropertySeparatorChar(propSep);
        this.setKeyValueSeparatorChar(keyValSep);
        this.setProperties(props, true);
    }

    /**
    *** Constructor
    *** @param argv    An array of "key=value" specifications.
    **/
    public RTProperties(String argv[]) {
        this();
        if (argv != null) {
            for (int i = 0; i < argv.length; i++) {
                if (StringTools.isBlank(argv[i])) {
                    continue;
                }
                int p = this._indexOfKeyValSeparator(argv[i]);
                String key = (p >= 0) ? argv[i].substring(0, p).trim() : argv[i];
                String val = (p >= 0) ? argv[i].substring(p + 1).trim() : "";
                if (key.startsWith(KEYVAL_PREFIX)) {
                    while (key.startsWith(KEYVAL_PREFIX)) {
                        key = key.substring(1);
                    }
                    if (p < 0) {
                        if (key.equals("")) {
                            if (i < (argv.length + 1)) {
                                this.nextCmdLineArg = i + 1;
                            }
                            break;
                        }
                        if (((i + 1) < argv.length) && !argv[i + 1].startsWith(KEYVAL_PREFIX)) {
                            i++;
                            val = argv[i];
                        }
                    }
                }
                if (key.equals("")) {
                    Print.logWarn("Ignoring invalid key argument: '%s'", argv[i]);
                } else {
                    this.setString(key, val);
                }
            }
        }
    }

    /**
    *** Constructor
    *** @param cfgFile A file specification from which the key=value properties are loaded.
    **/
    public RTProperties(File cfgFile) {
        this(CreateDefaultMap());
        if ((cfgFile == null) || cfgFile.equals("")) {
        } else if (cfgFile.isFile()) {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET, true)) {
                Print.logInfo("Loading config file: " + cfgFile);
            }
            try {
                this.setProperties(cfgFile, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgFile + " [" + ioe + "]");
            }
        } else {
            Print.logError("Config file doesn't exist: " + cfgFile);
        }
    }

    /**
    *** Constructor
    *** @param cfgURL A URL specification from which the key=value properties are loaded.
    **/
    public RTProperties(URL cfgURL) {
        this(CreateDefaultMap());
        if (cfgURL == null) {
        } else {
            if (!RTConfig.getBoolean(RTKey.RT_QUIET, true)) {
                Print.logInfo("Loading config file: " + cfgURL);
            }
            try {
                this.setProperties(cfgURL, true);
            } catch (IOException ioe) {
                Print.logError("Unable to load config file: " + cfgURL + " [" + ioe + "]");
            }
        }
    }

    /**
    *** Copy Constructor
    *** @param rtp A RTProperties instance from this this instance is initialized
    **/
    public RTProperties(RTProperties rtp) {
        this();
        this.setProperties(rtp, true);
    }

    /**
    *** Returns a clone of this RTProperties instance
    *** @return A clone of this RTProperties instance
    **/
    public Object clone() {
        return new RTProperties(this);
    }

    /**
    *** Returns true if the key case on lookups is to be ignored
    *** @return True if the key case on lookups is to be ignored
    **/
    public boolean getIgnoreKeyCase() {
        return this.ignoreCase;
    }

    /**
    *** Sets whether key-case is to be ignored on propery lookups.  Only valid if the backing Map
    *** is an <code>OrderedMap</code>.
    *** @param ignCase True ignore key-case on lookups, false otherwise
    **/
    public void setIgnoreKeyCase(boolean ignCase) {
        this.ignoreCase = ignCase;
        Map props = this.getProperties();
        if (props instanceof OrderedMap) {
            ((OrderedMap) props).setIgnoreCase(this.ignoreCase);
        } else if (ignCase) {
            Print.logWarn("Backing map is not an 'OrderedMap', case insensitive keys not in effect");
        }
    }

    /**
    *** Returns true if empty String values are allowed
    *** @return True if empty String values are allowed
    **/
    public boolean getAllowBlankValues() {
        return this.allowBlankValues;
    }

    /**
    *** Sets whether empty String values are allowed
    *** @param allowBlank True to allow blank String values
    **/
    public void setAllowBlankValues(boolean allowBlank) {
        this.allowBlankValues = allowBlank;
        if (!allowBlank) {
        }
    }

    /**
    *** Returns true if configuration log messages (ie. "%log=:) are enabled
    *** @return True if configuration log messages (ie. "%log=:) are enabled
    **/
    public boolean getConfigLogMessagesEnabled() {
        return this.enableConfigLogMessages;
    }

    /**
    *** Sets Configuration log messages (ie. "%log=") enabled/disabled
    *** @param enable True to enable, false to disable
    **/
    public void setConfigLogMessagesEnabled(boolean enable) {
        this.enableConfigLogMessages = enable;
    }

    /**
    *** Gets the name of this instance.
    *** @return The name of this instance
    **/
    public String getName() {
        return this.getString(RTKey.NAME, "");
    }

    /**
    *** Sets the name of this instance
    *** @param name  The name of this instance to set
    **/
    public void setName(String name) {
        this.setString(RTKey.NAME, name);
    }

    /**
    *** List all defined property keys which do not have a registered default value.<br>
    *** Used for diagnostice purposes.
    **/
    public void checkDefaults() {
        for (Iterator<?> i = this.keyIterator(); i.hasNext(); ) {
            String key = i.next().toString();
            if (!RTKey.hasDefault(key)) {
                Print.logDebug("No default for key: " + key);
            }
        }
    }

    protected static Class<OrderedMap> DefaultMapClass = OrderedMap.class;

    /**
    *** Creates a default Map object container
    *** @return A default Map object container
    **/
    protected static Map<Object, Object> CreateDefaultMap() {
        return new OrderedMap<Object, Object>();
    }

    /**
    *** Returns the next command-line argument following the last argument
    *** processed by the command-line argument RTProperties constructor.
    *** @return The next command-line argument, or '-1' if there are no additional
    ***         command-line arguments.
    **/
    public int getNextCommandLineArgumentIndex() {
        return this.nextCmdLineArg;
    }

    /**
    *** Validates the key/values against the expected set of keys and value types.
    *** @param keyAttr  A list of expected keys and attributes
    *** @return The index of the first invalid key
    **/
    public boolean validateKeyAttributes(PrintStream out, String keyAttr[]) {
        if (ListTools.isEmpty(keyAttr)) {
            return true;
        }
        int error = 0;
        Set<?> argKeys = new HashSet<Object>(this.getPropertyKeys());
        for (int i = 0; i < keyAttr.length; i++) {
            String aKey[] = null;
            boolean mandatory = false;
            int valType = 0;
            int p = this._indexOfKeyValSeparator(keyAttr[i]);
            if (p == 0) {
            } else if (p < 0) {
                aKey = StringTools.split(keyAttr[i], ',');
                mandatory = false;
                valType = 0;
            } else {
                aKey = StringTools.split(keyAttr[i].substring(0, p), ',');
                mandatory = (keyAttr[i].charAt(p) == '=') ? true : false;
                String attr[] = StringTools.split(keyAttr[i].substring(p + 1), ',');
                for (int a = 0; a < attr.length; a++) {
                    if (attr[a].equals("m")) {
                        mandatory = true;
                    } else if (attr[a].equals("o")) {
                        mandatory = false;
                    } else if (attr[a].equals("s")) {
                        valType = 0;
                    } else if (attr[a].equals("i")) {
                        valType = 1;
                    } else if (attr[a].equals("f")) {
                        valType = 2;
                    } else if (attr[a].equals("d")) {
                        valType = 2;
                    } else if (attr[a].equals("b")) {
                        valType = 3;
                    }
                }
            }
            boolean keyFound = false;
            String keyStr = StringTools.join(aKey, ',');
            if (ListTools.isEmpty(aKey)) {
                continue;
            } else {
                int found = 0;
                for (int k = 0; k < aKey.length; k++) {
                    if (this.hasProperty(aKey[k])) {
                        found++;
                    }
                    argKeys.remove(aKey[k]);
                }
                if (found > 1) {
                    if (out != null) {
                        out.println("ERROR: Multiple values found for keys: " + keyStr);
                    }
                    error++;
                }
                keyFound = (found > 0);
            }
            String keyValue = this.getString(aKey, null);
            if (StringTools.isBlank(keyValue)) {
                if (mandatory && (!keyFound || (valType != 3))) {
                    if (out != null) {
                        out.println("ERROR: Mandatory key not specified: " + keyStr);
                    }
                    error++;
                }
                continue;
            }
            String firstKey = this.getFirstDefinedKey(aKey);
            switch(valType) {
                case 0:
                    break;
                case 1:
                    if (!StringTools.isLong(keyValue, true)) {
                        if (out != null) {
                            out.println("ERROR: Invalid value for key (i): " + firstKey);
                        }
                        error++;
                    }
                    break;
                case 2:
                    if (!StringTools.isDouble(keyValue, true)) {
                        if (out != null) {
                            out.println("ERROR: Invalid value for key (f): " + firstKey);
                        }
                        error++;
                    }
                    break;
                case 3:
                    if (!StringTools.isBoolean(keyValue, true)) {
                        if (out != null) {
                            out.println("ERROR: Invalid value for key (b): " + firstKey);
                        }
                        error++;
                    }
                    break;
            }
        }
        if (!argKeys.isEmpty()) {
            boolean UNRECOGNIZED_ARGUMENT_ERROR = false;
            for (Object key : argKeys) {
                String ks = key.toString();
                if (ks.startsWith("$")) {
                    continue;
                }
                if (UNRECOGNIZED_ARGUMENT_ERROR) {
                    if (out != null) {
                        out.println("ERROR: Unrecognized argument specified: " + ks);
                    }
                    error++;
                } else {
                    if (out != null) {
                        out.println("WARNING: Unrecognized argument specified: " + ks);
                    }
                }
            }
        }
        return (error == 0);
    }

    /**
    *** PropertyChangeListener interface
    **/
    public interface PropertyChangeListener {

        void propertyChange(RTProperties.PropertyChangeEvent pce);
    }

    /**
    *** PropertyChangeEvent class 
    **/
    public class PropertyChangeEvent {

        private Object keyObj = null;

        private Object oldVal = null;

        private Object newVal = null;

        public PropertyChangeEvent(Object key, Object oldValue, Object newValue) {
            this.keyObj = key;
            this.oldVal = oldValue;
            this.newVal = newValue;
        }

        public RTProperties getSource() {
            return RTProperties.this;
        }

        public Object getKey() {
            return this.keyObj;
        }

        public Object getOldValue() {
            return this.oldVal;
        }

        public Object getNewValue() {
            return this.newVal;
        }
    }

    private java.util.List<PropertyChangeListener> changeListeners = null;

    /** 
    *** Adds a PropertyChangeListener to this instance
    *** @param pcl  A PropertyChangeListener to add to this instance
    **/
    public void addChangeListener(PropertyChangeListener pcl) {
        if (this.changeListeners == null) {
            this.changeListeners = new Vector<PropertyChangeListener>();
        }
        this.changeListeners.add(pcl);
    }

    /** 
    *** Removes a PropertyChangeListener from this instance
    *** @param pcl  A PropertyChangeListener to remove from this instance
    **/
    public void removeChangeListener(PropertyChangeListener pcl) {
        if (this.changeListeners != null) {
            this.changeListeners.remove(pcl);
        }
    }

    /**
    *** Fires a PropertyChange event
    *** @param key  The property key which changed
    *** @param oldVal  The old value of the property key which changed
    **/
    protected void firePropertyChanged(Object key, Object oldVal) {
        if (this.changeListeners != null) {
            Object newVal = this.getProperties().get(key);
            RTProperties.PropertyChangeEvent pce = new RTProperties.PropertyChangeEvent(key, oldVal, newVal);
            for (Iterator i = this.changeListeners.iterator(); i.hasNext(); ) {
                ((RTProperties.PropertyChangeListener) i.next()).propertyChange(pce);
            }
        }
    }

    /**
    *** Sets the backing properties Map for this instance
    *** @return  The backing properties Map for this instance
    **/
    @SuppressWarnings("unchecked")
    public void setBackingProperties(Map<?, ?> map) {
        this.cfgProperties = (Map<Object, Object>) map;
    }

    /**
    *** Gets the backing properties Map for this instance
    *** @return  The backing properties Map for this instance
    **/
    public Map<Object, Object> getProperties() {
        if (this.cfgProperties == null) {
            this.cfgProperties = CreateDefaultMap();
            if (this.cfgProperties instanceof OrderedMap) {
                ((OrderedMap) this.cfgProperties).setIgnoreCase(this.ignoreCase);
            }
        }
        return this.cfgProperties;
    }

    /**
    *** Returns true if this RTProperties instance is empty (ie. contains no properties)
    *** @return  True if empty
    **/
    public boolean isEmpty() {
        if (this.cfgProperties == null) {
            return true;
        } else {
            return (this.cfgProperties.size() <= 0);
        }
    }

    /**
    *** Returns an Iterator over the property keys defined in this RTProperties instance
    *** @return An Iterator over the property keys defined in this RTProperties instance
    **/
    public Iterator<?> keyIterator() {
        return this.getPropertyKeys().iterator();
    }

    /**
    *** Gets a set of property keys defined by this RTProperties instance
    *** @return A set of property keys defined by this RTProperties instance
    **/
    public Set<?> getPropertyKeys() {
        return this.getProperties().keySet();
    }

    /**
    *** Returns a set of property keys defined in this RTProperties instance which start with the specified String
    *** @return A set of property keys defined in this RTProperties instance which start with the specified String
    **/
    public Set<?> getPropertyKeys(String startsWith) {
        OrderedSet<String> keys = new OrderedSet<String>();
        for (Iterator<?> i = this.keyIterator(); i.hasNext(); ) {
            String k = i.next().toString();
            if (StringTools.startsWithIgnoreCase(k, startsWith)) {
                keys.add(k);
            }
        }
        return keys;
    }

    /**
    *** Returns a subset of this RTProperties instance containing key/value pairs which match the
    *** specified partial key.
    *** @param keyStartsWith  The partial key used to match keys in this instance
    *** @return The RTProperties subset
    **/
    public RTProperties getSubset(String keyStartsWith) {
        RTProperties rtp = new RTProperties();
        for (Iterator<?> i = this.keyIterator(); i.hasNext(); ) {
            Object k = i.next();
            if (k instanceof String) {
                String ks = (String) k;
                if (StringTools.startsWithIgnoreCase(ks, keyStartsWith)) {
                    String v = this.getString(ks, null);
                    rtp.setProperty(ks, v);
                }
            }
        }
        return rtp;
    }

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public static boolean containsKey(Map<Object, Object> map, Object key, boolean blankOK) {
        if ((map == null) || (key == null)) {
            return false;
        }
        if (blankOK) {
            return map.containsKey(key);
        } else {
            Object val = map.get(key);
            if (val instanceof String) {
                return !StringTools.isBlank((String) val);
            } else {
                return (val != null);
            }
        }
    }

    /**
    *** Returns true if the specified property key is defined
    *** @param key  A property key
    *** @return True if the specified property key is defined
    **/
    public boolean hasProperty(Object key) {
        if (key != null) {
            Map<Object, Object> props = this.getProperties();
            boolean allowBlanks = this.getAllowBlankValues();
            return RTProperties.containsKey(props, key, allowBlanks);
        } else {
            return false;
        }
    }

    /**
    *** Returns the first defined property key in the list 
    *** @param key  An array of property keys
    *** @return the first defined property key in the list
    **/
    public String getFirstDefinedKey(String key[]) {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                if (this.hasProperty(key[i])) {
                    return key[i];
                }
            }
        }
        return null;
    }

    /**
    *** Returns the specified key, if defined
    *** @param key  The propery key
    *** @return The property key if defined, or null otherwise
    **/
    public String getFirstDefinedKey(String key) {
        return this.hasProperty(key) ? key : null;
    }

    /**
    *** Sets the value for the specified key
    *** @param key  The property key
    *** @param value The value to associate with the specified key
    **/
    public void setProperty(Object key, Object value) {
        if (key != null) {
            Map<Object, Object> props = this.getProperties();
            if (!this.getAllowBlankValues() && (value instanceof String) && StringTools.isBlank((String) value)) {
                value = null;
            }
            String k = (key instanceof String) ? (String) key : null;
            if (!StringTools.isBlank(k) && ("|!^".indexOf(k.charAt(0)) >= 0)) {
                key = k.substring(1);
                value = null;
            }
            if ((value != null) && value.getClass().isArray()) {
                Class arrayClass = value.getClass();
                if (arrayClass.getComponentType().isPrimitive()) {
                    value = StringTools.encodeArray(value, ARRAY_DELIM, false);
                } else {
                    Object a[] = (Object[]) value;
                    boolean quote = (a instanceof Number[]) ? false : true;
                    value = StringTools.encodeArray(a, ARRAY_DELIM, quote);
                }
            } else {
            }
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                if (value == null) {
                    props.remove(key);
                } else if ((props instanceof OrderedMap) && key.equals(RTKey.NAME)) {
                    ((OrderedMap<Object, Object>) props).put(0, key, value);
                } else {
                    props.put(key, value);
                }
                this.firePropertyChanged(key, oldVal);
            } else {
            }
        }
    }

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(RTProperties rtp) {
        return this.setProperties(rtp, false);
    }

    /**
    *** Adds the properties in the specified RTProperties instance to this instance
    *** @param rtp  The RTProperties instance from which properties will be copied to this instance
    *** @param inclName  True to set the name of this instace to the instance of the specified RTProperties instance.
    *** @return The name of this RTProperties instance
    **/
    public String setProperties(RTProperties rtp, boolean inclName) {
        if (rtp != null) {
            return this.setProperties(rtp.getProperties(), inclName);
        } else {
            return null;
        }
    }

    public String setProperties(URL url) throws IOException {
        return this.setProperties(url, false);
    }

    public String setProperties(URL url, boolean inclName) throws IOException {
        String name = null;
        if (url != null) {
            InputStream uis = url.openStream();
            try {
                name = this._setProperties(uis, inclName, url);
            } finally {
                try {
                    uis.close();
                } catch (IOException ioe) {
                }
            }
        }
        return name;
    }

    public String setProperties(File file) throws IOException {
        return this.setProperties(file, false);
    }

    public String setProperties(File file, boolean inclName) throws IOException {
        String name = null;
        if (file != null) {
            File absFile = file.getAbsoluteFile();
            FileInputStream fis = new FileInputStream(absFile);
            try {
                name = this._setProperties(fis, inclName, absFile.toURL());
            } finally {
                try {
                    fis.close();
                } catch (IOException ioe) {
                }
            }
        }
        return name;
    }

    public String setProperties(InputStream in) throws IOException {
        return this._setProperties(in, false, null);
    }

    public String setProperties(InputStream in, boolean inclName) throws IOException {
        return this._setProperties(in, false, null);
    }

    private String _setProperties(InputStream in, boolean inclName, URL inputURL) throws IOException {
        OrderedProperties props = new OrderedProperties(inputURL);
        if (inputURL != null) {
            props.put(RTKey.CONFIG_URL, inputURL.toString());
        }
        if (USE_PROPERTIES_LOADER) {
            props.load(in);
        } else {
            Print.logWarn("Non-standard Properties file loading ...");
            RTProperties.loadProperties(props, in);
        }
        return this.setProperties(props.getOrderedMap(), inclName);
    }

    public String setProperties(Map props) {
        return this.setProperties(props, false);
    }

    public String setProperties(Map props, boolean inclName) {
        if (props != null) {
            String n = null;
            for (Iterator i = props.keySet().iterator(); i.hasNext(); ) {
                Object key = i.next();
                Object val = props.get(key);
                if (RTKey.NAME.equals(key)) {
                    n = (val != null) ? val.toString() : null;
                    if (inclName) {
                        this.setName(n);
                    }
                } else {
                    this.setProperty(key, val);
                }
            }
            return n;
        } else {
            return null;
        }
    }

    public void setPropertySeparatorChar(char propSep) {
        this.propertySeparator = propSep;
    }

    public char getPropertySeparatorChar() {
        return this.propertySeparator;
    }

    public void setKeyValueSeparatorChar(char keyValSep) {
        this.keyValueSeparator = keyValSep;
    }

    public char getKeyValueSeparatorChar() {
        return this.keyValueSeparator;
    }

    public String setProperties(String props) {
        return this.setProperties(props, false);
    }

    public String setProperties(String props, char propSep) {
        this.setPropertySeparatorChar(propSep);
        return this.setProperties(props, false);
    }

    public String setProperties(String props, boolean inclName) {
        if (props != null) {
            char propSep = this.getPropertySeparatorChar();
            char keyValSep = this.getKeyValueSeparatorChar();
            String n = null, p = props.trim();
            if (p.startsWith("[")) {
                int x = p.indexOf("]");
                if (x > 0) {
                    n = p.substring(1, x).trim();
                    p = p.substring(x + 1).trim();
                } else {
                    p = p.substring(1).trim();
                }
            }
            Map propMap = StringTools.parseProperties(p, propSep, keyValSep);
            if (n == null) {
                n = this.setProperties(propMap, inclName);
            } else {
                this.setProperties(propMap, false);
                if (inclName) {
                    this.setName(n);
                }
            }
            return n;
        } else {
            return null;
        }
    }

    public void removeProperty(Object key) {
        if (key != null) {
            Map props = this.getProperties();
            if (!(props instanceof Properties) || (key instanceof String)) {
                Object oldVal = props.get(key);
                props.remove(key);
                this.firePropertyChanged(key, oldVal);
            }
        }
    }

    public void clearProperties() {
        this.getProperties().clear();
        this.firePropertyChanged(null, null);
    }

    public void resetProperties(Map props) {
        this.clearProperties();
        this.setProperties(props, true);
    }

    public String insertKeyValues(String text) {
        return this._insertKeyValues(null, text, KEY_START_DELIMITER, KEY_END_DELIMITER);
    }

    public String insertKeyValues(String text, String startDelim, String endDelim) {
        return this._insertKeyValues(null, text, startDelim, endDelim);
    }

    public String _insertKeyValues(Object key, String text) {
        return this._insertKeyValues(key, text, KEY_START_DELIMITER, KEY_END_DELIMITER);
    }

    public String _insertKeyValues(final Object mainKey, String text, String startDelim, String endDelim) {
        if (text != null) {
            StringTools.ReplacementMap rm = new StringTools.ReplacementMap() {

                private Set<Object> thisKeySet = new HashSet<Object>();

                private Set<Object> fullKeySet = new HashSet<Object>();

                public String get(String k) {
                    if (k == null) {
                        fullKeySet.addAll(thisKeySet);
                        if (mainKey != null) {
                            fullKeySet.add(mainKey);
                        }
                        thisKeySet.clear();
                        return null;
                    } else if (fullKeySet.contains(k)) {
                        return null;
                    } else {
                        thisKeySet.add(k);
                        Object obj = RTProperties.this._getProperty(k, null);
                        return (obj != null) ? obj.toString() : null;
                    }
                }
            };
            String s_old = text;
            for (int i = 0; i < RTProperties.KEY_MAX_RECURSION; i++) {
                rm.get(null);
                String s_new = StringTools.insertKeyValues(s_old, startDelim, endDelim, rm, false);
                if (s_new.equals(s_old)) {
                    return s_new;
                }
                s_old = s_new;
            }
            return s_old;
        } else {
            return text;
        }
    }

    public void setKeyReplacementMode(int mode) {
        this.keyReplacementMode = mode;
    }

    private Object _replaceKeyValues(Object key, Object obj) {
        if (this.keyReplacementMode == KEY_REPLACEMENT_NONE) {
            return obj;
        } else if ((obj == null) || !(obj instanceof String)) {
            return obj;
        } else if (this.keyReplacementMode == KEY_REPLACEMENT_LOCAL) {
            return this._insertKeyValues(key, (String) obj);
        } else {
            return RTConfig._insertKeyValues(key, (String) obj);
        }
    }

    private Object _getProperty(Object key, Object dft, Class dftClass, boolean replaceKeys) {
        Object value = this.getProperties().get(key);
        if (value == null) {
            return replaceKeys ? this._replaceKeyValues(key, dft) : dft;
        } else if ((dft == null) && (dftClass == null)) {
            return replaceKeys ? this._replaceKeyValues(key, value) : value;
        } else {
            Class c = (dftClass != null) ? dftClass : dft.getClass();
            try {
                return convertToType(replaceKeys ? this._replaceKeyValues(key, value) : value, c);
            } catch (Throwable t) {
                return replaceKeys ? this._replaceKeyValues(key, dft) : dft;
            }
        }
    }

    public Object _getProperty(Object key, Object dft) {
        return this._getProperty(key, dft, null, false);
    }

    public Object getProperty(Object key, Object dft) {
        return this._getProperty(key, dft, null, true);
    }

    protected static Object convertToType(Object val, Class<?> type) throws Throwable {
        if ((type == null) || (val == null)) {
            return val;
        } else if (type.isAssignableFrom(val.getClass())) {
            return val;
        } else if (type == String.class) {
            return val.toString();
        } else {
            try {
                Constructor meth = type.getConstructor(new Class[] { type });
                return meth.newInstance(new Object[] { val });
            } catch (Throwable t1) {
                try {
                    Constructor meth = type.getConstructor(new Class[] { String.class });
                    return meth.newInstance(new Object[] { val.toString() });
                } catch (Throwable t2) {
                    Print.logError("Can't convert value to " + type.getName() + ": " + val);
                    throw t2;
                }
            }
        }
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key
    *** @return The String value, or null if the key is not found
    **/
    public String getString(String key) {
        return this.getString(key, null);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  An array or property keys.  The value of the first matching 
    ***             key will be returned.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key[], String dft) {
        return this.getString(this.getFirstDefinedKey(key), dft);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft) {
        return this.getString(key, dft, true);
    }

    /**
    *** Gets the String value for the specified key
    *** @param key  The property key.
    *** @param dft  The default value return if the key is not found
    *** @param replaceKeys  True to perform ${...} key replace, false to return raw String
    *** @return The String value, or 'dft' if the key is not found
    **/
    public String getString(String key, String dft, boolean replaceKeys) {
        Object val = this._getProperty(key, dft, String.class, replaceKeys);
        if (val == null) {
            return null;
        } else if (val.equals(RTKey.NULL_VALUE)) {
            return null;
        } else {
            return val.toString();
        }
    }

    /**
    *** Sets the property value for the specified key
    *** @param key    The property key
    *** @param value  The property value to set.
    **/
    public void setString(String key, String value) {
        this.setProperty(key, value);
    }

    /**
    *** "StringTools.KeyValueMap" interface
    *** @param key  The property key
    *** @param arg  The property argument (used as the 'default' String value here)
    *** @return The property value
    **/
    public String getKeyValue(String key, String arg) {
        return this.getString(key, arg);
    }

    public String[] getStringArray(String key) {
        return this.getStringArray(key, null);
    }

    public String[] getStringArray(String key[], String dft[]) {
        return this.getStringArray(this.getFirstDefinedKey(key), dft);
    }

    public String[] getStringArray(String key, String dft[]) {
        String val = this.getString(key, null);
        if (val == null) {
            return dft;
        } else {
            String va[] = StringTools.parseArray(val);
            return va;
        }
    }

    public void setStringArray(String key, String val[]) {
        this.setStringArray(key, val, true);
    }

    public void setStringArray(String key, String val[], boolean alwaysQuote) {
        String valStr = StringTools.encodeArray(val, ARRAY_DELIM, alwaysQuote);
        this.setString(key, valStr);
    }

    public void setProperty(String key, String val[]) {
        this.setStringArray(key, val, true);
    }

    public File getFile(String key) {
        return this.getFile(key, null);
    }

    public File getFile(String key, File dft) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof File) {
            return (File) val;
        } else {
            return new File(val.toString());
        }
    }

    public void setFile(String key, File value) {
        this.setProperty(key, value);
    }

    public double getDouble(String key) {
        return this.getDouble(key, 0.0);
    }

    public double getDouble(String key[], double dft) {
        return this.getDouble(this.getFirstDefinedKey(key), dft);
    }

    public double getDouble(String key, double dft) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else {
            return StringTools.parseDouble(val.toString(), dft);
        }
    }

    public double[] getDoubleArray(String key, double dft[]) {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            double n[] = new double[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseDouble(val[i], 0.0);
            }
            return n;
        }
    }

    public void setDouble(String key, double value) {
        this.setProperty(key, value);
    }

    public void setDoubleArray(String key, double value[]) {
        this.setProperty(key, value);
    }

    public void setProperty(String key, double value) {
        this.setProperty(key, new Double(value));
    }

    public float getFloat(String key) {
        return this.getFloat(key, 0.0F);
    }

    public float getFloat(String key[], float dft) {
        return this.getFloat(this.getFirstDefinedKey(key), dft);
    }

    public float getFloat(String key, float dft) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof Number) {
            return ((Number) val).floatValue();
        } else {
            return StringTools.parseFloat(val.toString(), dft);
        }
    }

    public float[] getFloatArray(String key, float dft[]) {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            float n[] = new float[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseFloat(val[i], 0.0F);
            }
            return n;
        }
    }

    public void setFloat(String key, float value) {
        this.setProperty(key, value);
    }

    public void setFloatArray(String key, float value[]) {
        this.setProperty(key, value);
    }

    public void setProperty(String key, float value) {
        this.setProperty(key, new Float(value));
    }

    public long getLong(String key) {
        return this.getLong(key, 0L);
    }

    public long getLong(String key[], long dft) {
        return this.getLong(this.getFirstDefinedKey(key), dft);
    }

    public long getLong(String key, long dft) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else {
            return StringTools.parseLong(val.toString(), dft);
        }
    }

    public long[] getLongArray(String key, long dft[]) {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            long n[] = new long[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseLong(val[i], 0L);
            }
            return n;
        }
    }

    public void setLong(String key, long value) {
        this.setProperty(key, value);
    }

    public void setLongArray(String key, long value[]) {
        this.setProperty(key, value);
    }

    public void setProperty(String key, long value) {
        this.setProperty(key, new Long(value));
    }

    public int getInt(String key) {
        return this.getInt(key, 0);
    }

    public int getInt(String key[], int dft) {
        return this.getInt(this.getFirstDefinedKey(key), dft);
    }

    public int getInt(String key, int dft) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof Number) {
            return ((Number) val).intValue();
        } else {
            return StringTools.parseInt(val.toString(), dft);
        }
    }

    public int[] getIntArray(String key, int dft[]) {
        String val[] = this.getStringArray(key, null);
        if (val == null) {
            return dft;
        } else {
            int n[] = new int[val.length];
            for (int i = 0; i < val.length; i++) {
                n[i] = StringTools.parseInt(val[i], 0);
            }
            return n;
        }
    }

    public void setInt(String key, int value) {
        this.setProperty(key, value);
    }

    public void setIntArray(String key, int value[]) {
        this.setProperty(key, value);
    }

    public void setProperty(String key, int value) {
        this.setProperty(key, new Integer(value));
    }

    public boolean getBoolean(String key) {
        boolean dft = false;
        return this._getBoolean_dft(key, dft, true);
    }

    public boolean getBoolean(String key[], boolean dft) {
        return this.getBoolean(this.getFirstDefinedKey(key), dft);
    }

    public boolean getBoolean(String key, boolean dft) {
        return this._getBoolean_dft(key, dft, DEFAULT_TRUE_IF_BOOLEAN_STRING_EMPTY);
    }

    private boolean _getBoolean_dft(String key, boolean dft, boolean dftTrueIfEmpty) {
        Object val = this._getProperty(key, null, null, true);
        if (val == null) {
            return dft;
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else if (val.toString().equals("")) {
            return dftTrueIfEmpty ? true : dft;
        } else {
            return StringTools.parseBoolean(val.toString(), dft);
        }
    }

    public void setBoolean(String key, boolean value) {
        this.setProperty(key, value);
    }

    public void setBooleanArray(String key, boolean value[]) {
        this.setProperty(key, value);
    }

    public void setProperty(String key, boolean value) {
        this.setProperty(key, new Boolean(value));
    }

    public void printProperties(String msg) {
        this.printProperties(msg, null, null);
    }

    public void printProperties(String msg, RTProperties exclProps) {
        this.printProperties(msg, exclProps, null);
    }

    public void printProperties(String msg, Collection<?> orderBy) {
        this.printProperties(msg, null, orderBy);
    }

    public void printProperties(String msg, RTProperties exclProps, Collection<?> orderBy) {
        if (!StringTools.isBlank(msg)) {
            Print.sysPrintln(msg);
        }
        String prefix = "   ";
        if (this.isEmpty()) {
            Print.sysPrintln(prefix + "<empty>\n");
        } else {
            if (orderBy == null) {
                orderBy = new Vector<Object>(this.getPropertyKeys());
                ListTools.sort((java.util.List<?>) orderBy, null);
            }
            Print.sysPrintln(this.toString(exclProps, orderBy, prefix));
        }
    }

    public boolean equals(Object other) {
        if (other instanceof RTProperties) {
            RTProperties rtp = (RTProperties) other;
            Map M1 = this.getProperties();
            Map M2 = rtp.getProperties();
            if (M1.size() == M2.size()) {
                for (Iterator i = M1.keySet().iterator(); i.hasNext(); ) {
                    Object key = i.next();
                    if (M2.containsKey(key)) {
                        Object m1Val = M1.get(key);
                        Object m2Val = M2.get(key);
                        String m1ValStr = (m1Val != null) ? m1Val.toString() : null;
                        String m2ValStr = (m2Val != null) ? m2Val.toString() : null;
                        if (m1Val == m2Val) {
                            continue;
                        } else if ((m1ValStr != null) && m1ValStr.equals(m2ValStr)) {
                            continue;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void saveProperties(File cfgFile) throws IOException {
        Map propMap = this.getProperties();
        StringBuffer strProps = new StringBuffer();
        for (Iterator i = propMap.keySet().iterator(); i.hasNext(); ) {
            Object keyObj = i.next();
            Object valObj = propMap.get(keyObj);
            strProps.append(keyObj.toString());
            strProps.append(this.getKeyValueSeparatorChar());
            if (valObj != null) {
                strProps.append(valObj.toString());
            }
            strProps.append("\n");
        }
        FileTools.writeFile(strProps.toString().getBytes(), cfgFile);
    }

    public String toString() {
        return this.toString(null, null, null);
    }

    public String toString(RTProperties exclProps) {
        return this.toString(exclProps, null, null);
    }

    public String toString(Collection<?> orderBy) {
        return this.toString(null, orderBy, null);
    }

    public String toString(RTProperties exclProps, Collection<?> orderBy) {
        return this.toString(null, orderBy, null);
    }

    public String toString(RTProperties exclProps, Collection<?> orderBy, String newLinePrefix) {
        StringBuffer sb = new StringBuffer();
        boolean inclNewLine = (newLinePrefix != null);
        String n = this.getName();
        if (!n.equals("")) {
            sb.append(n).append(NameSeparatorChar).append(" ");
        }
        Map<Object, Object> propMap = this.getProperties();
        Map<Object, Object> exclMap = (exclProps != null) ? exclProps.getProperties() : null;
        Set<Object> orderSet = null;
        if (orderBy != null) {
            orderSet = new OrderedSet<Object>(orderBy, true);
            orderSet.addAll(propMap.keySet());
        } else {
            orderSet = propMap.keySet();
        }
        for (Iterator<Object> i = orderSet.iterator(); i.hasNext(); ) {
            Object keyObj = i.next();
            if (!RTKey.NAME.equals(keyObj) && RTProperties.containsKey(propMap, keyObj, this.getAllowBlankValues())) {
                Object valObj = propMap.get(keyObj);
                if ((exclMap == null) || !RTProperties.compareMapValues(valObj, exclMap.get(keyObj))) {
                    if (inclNewLine) {
                        sb.append(newLinePrefix);
                    }
                    sb.append(keyObj.toString()).append(this.getKeyValueSeparatorChar());
                    String v = (valObj != null) ? valObj.toString() : "";
                    if ((v.indexOf(" ") >= 0) || (v.indexOf("\t") >= 0) || (v.indexOf("\"") >= 0)) {
                        sb.append(StringTools.quoteString(v));
                    } else {
                        sb.append(v);
                    }
                    if (inclNewLine) {
                        sb.append("\n");
                    } else if (i.hasNext()) {
                        sb.append(this.getPropertySeparatorChar());
                    }
                } else {
                }
            }
        }
        return inclNewLine ? sb.toString() : sb.toString().trim();
    }

    private static boolean compareMapValues(Object value, Object target) {
        if ((value == null) && (target == null)) {
            return true;
        } else if ((value == null) || (target == null)) {
            return false;
        } else if (value.equals(target)) {
            return true;
        } else {
            return value.toString().equals(target.toString());
        }
    }

    private static final String KEY_INCLUDE_URL = RTKey.INCLUDE;

    private static final String KEY_INCLUDE_URL_OPT = RTKey.INCLUDE_OPT;

    private static final int MAX_INCLUDE_RECURSION = 3;

    /**
    *** OrderedProperties class
    **/
    public class OrderedProperties extends Properties {

        private boolean debugMode = false;

        private int recursionLevel = 0;

        private OrderedMap<String, String> orderedMap = null;

        private URL inputURL = null;

        public OrderedProperties(URL inputURL) {
            this(1, inputURL);
        }

        private OrderedProperties(int recursion, URL inputURL) {
            super();
            this.recursionLevel = recursion;
            this.orderedMap = new OrderedMap<String, String>();
            this.inputURL = inputURL;
        }

        public Object put(Object key, Object value) {
            if ((key == null) || (value == null)) {
                return value;
            }
            String ks = key.toString();
            String vs = value.toString();
            if (ks.startsWith(RTKey.CONSTANT_PREFIX)) {
                if (this.debugMode) {
                    Print.logInfo("(DEBUG) Found Constant key: " + ks);
                }
                if (ks.equalsIgnoreCase("%debugMode")) {
                    this.debugMode = StringTools.parseBoolean(vs, false);
                    if (this.debugMode) {
                        Print.logInfo("(DEBUG) 'debugMode' set to " + this.debugMode);
                    }
                    return value;
                } else if (ks.equalsIgnoreCase(KEY_INCLUDE_URL) || ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                    String v = RTConfig.insertKeyValues(vs, this.orderedMap);
                    if (StringTools.isBlank(v)) {
                        Print.logError("Invalid/blank 'include' URL: " + vs);
                    } else if (this.recursionLevel >= MAX_INCLUDE_RECURSION) {
                        Print.logWarn("Excessive 'include' recursion [%s] ...", v);
                    } else {
                        InputStream uis = null;
                        URL url = null;
                        try {
                            if (this.debugMode) {
                                Print.logInfo("(DEBUG) Including: " + v);
                            }
                            url = new URL(v);
                            String parent = (this.inputURL != null) ? this.inputURL.toString() : "";
                            String parProto = (this.inputURL != null) ? this.inputURL.getProtocol().toLowerCase() : "";
                            String urlProto = url.getProtocol().toLowerCase();
                            String urlPath = url.getPath();
                            if (StringTools.isBlank(parProto)) {
                            } else if (parProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE) && !urlPath.startsWith("/")) {
                                    int ls = parent.lastIndexOf("/");
                                    if (ls > 0) {
                                        url = new URL(parent.substring(0, ls + 1) + urlPath);
                                    }
                                }
                            } else if (parProto.startsWith(INCLUDE_PROTOCOL_HTTP)) {
                                if (urlProto.equals(INCLUDE_PROTOCOL_FILE)) {
                                    Print.logError("Invalid 'include' URL protocol: " + url);
                                    url = null;
                                } else if (urlProto.equals(parProto) && !urlPath.startsWith("/")) {
                                    int cs = parent.indexOf("://");
                                    int ls = parent.lastIndexOf("/");
                                    if ((cs > 0) && (ls >= (cs + 3))) {
                                        url = new URL(parent.substring(0, ls + 1) + urlPath);
                                    }
                                }
                            } else {
                            }
                            if (url != null) {
                                if (this.debugMode) {
                                    Print.logInfo("(DEBUG) Including URL: [" + vs + "] " + url);
                                }
                                uis = url.openStream();
                                OrderedProperties props = new OrderedProperties(this.recursionLevel + 1, url);
                                props.put(RTKey.CONFIG_URL, url.toString());
                                props.load(uis);
                                props.remove(RTKey.CONFIG_URL);
                                this.orderedMap.putAll(props.getOrderedMap());
                            }
                        } catch (MalformedURLException mue) {
                            Print.logException("Invalid URL: " + url, mue);
                        } catch (IllegalArgumentException iae) {
                            Print.logException("Invalid URL arguments: " + url, iae);
                        } catch (Throwable th) {
                            if (!ks.equalsIgnoreCase(KEY_INCLUDE_URL_OPT)) {
                                Print.logException("Error including properties: " + url, th);
                            } else {
                            }
                        } finally {
                            if (uis != null) {
                                try {
                                    uis.close();
                                } catch (IOException ioe) {
                                }
                            }
                        }
                    }
                    return value;
                } else if (ks.equalsIgnoreCase(RTKey.LOG)) {
                    if (RTProperties.this.getConfigLogMessagesEnabled()) {
                        StringBuffer sb = new StringBuffer();
                        if (this.inputURL != null) {
                            String filePath = this.inputURL.getPath();
                            int p = filePath.lastIndexOf("/");
                            String fileName = (p >= 0) ? filePath.substring(p + 1) : filePath;
                            sb.append("[").append(fileName).append("] ");
                        }
                        RTProperties tempProps = new RTProperties(this);
                        RTConfig.pushTemporaryProperties(tempProps);
                        Print.resetVars();
                        sb.append(RTConfig.insertKeyValues(vs, this.orderedMap)).append("\n");
                        Print._writeLog(sb.toString());
                        RTConfig.popTemporaryProperties(tempProps);
                    }
                    return value;
                } else if (ks.equalsIgnoreCase(RTKey.CONFIG_URL)) {
                    Object rtn = super.put(key, value);
                    this.orderedMap.put(ks, vs);
                    return rtn;
                } else {
                    Print.logError("Invalid/unrecognized key specified: " + ks);
                    return value;
                }
            } else {
                Object rtn = super.put(key, value);
                this.orderedMap.put(ks, vs);
                return rtn;
            }
        }

        public Object remove(Object key) {
            if (key != null) {
                Object rtn = super.remove(key);
                this.orderedMap.remove(key.toString());
                return rtn;
            } else {
                return null;
            }
        }

        public OrderedMap<String, String> getOrderedMap() {
            return this.orderedMap;
        }
    }

    private static boolean STRING_PARSE_PROPS = true;

    private static boolean isEOL(byte b) {
        return ((b == '\n') || (b == '\r'));
    }

    private static boolean isEOL(char b) {
        return ((b == '\n') || (b == '\r'));
    }

    private static boolean isCOMMENT(byte b) {
        return ((b == '#') || (b == '!'));
    }

    private static boolean isCOMMENT(char b) {
        return ((b == '#') || (b == '!'));
    }

    private static boolean isSEP(byte b) {
        return ((b == '=') || (b == ':'));
    }

    private static boolean isSEP(char b) {
        return ((b == '=') || (b == ':'));
    }

    public static Properties loadProperties(Properties props, InputStream in) throws IOException {
        byte data[] = FileTools.readStream(in);
        if (STRING_PARSE_PROPS) {
            String dataStr = StringTools.toStringValue(data);
            String ds[] = StringTools.split(dataStr, '\n');
            for (int i = 0; i < ds.length; i++) {
                String d = ds[i].trim();
                if (d.equals("") || isCOMMENT(d.charAt(0))) {
                    continue;
                }
                int p = d.indexOf("=");
                if (p < 0) {
                    p = d.indexOf(":");
                }
                String key = (p >= 0) ? d.substring(0, p) : d;
                String val = (p >= 0) ? d.substring(p + 1) : "";
                if (!key.equals("")) {
                    Print.logInfo("S)Prop: " + key + " ==> " + val);
                    props.setProperty(key, val);
                }
            }
        } else {
            for (int s = 0; s < data.length; ) {
                while ((s < data.length) && Character.isWhitespace(data[s])) {
                    s++;
                }
                if ((s >= data.length) || isCOMMENT(data[s])) {
                    while ((s < data.length) && !isEOL(data[s])) {
                        s++;
                    }
                    continue;
                }
                int e, sep = -1;
                for (e = s; (e < data.length) && !isEOL(data[e]); e++) {
                    if ((sep < 0) && isSEP(data[e])) {
                        sep = e;
                    }
                }
                String key = "";
                String val = "";
                if (sep >= 0) {
                    key = StringTools.toStringValue(data, s, sep - s).trim();
                    val = StringTools.toStringValue(data, sep + 1, e - sep).trim();
                } else {
                    key = StringTools.toStringValue(data, s, e - s).trim();
                    val = "";
                }
                if (!key.equals("")) {
                    Print.logInfo("B)Prop: " + key + " ==> " + val);
                    props.setProperty(key, val);
                }
                s = e + 1;
            }
        }
        return props;
    }

    public static void main(String argv[]) {
        RTConfig.setCommandLineArgs(argv, new String[] { "s:", "b,bb:m,b", "f,ff,fff:f", "d,dd,ddd,dddd:d", "i=i", "g=o" });
    }
}
