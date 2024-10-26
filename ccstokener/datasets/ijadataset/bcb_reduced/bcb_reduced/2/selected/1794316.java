package gumbo.config.utils;

import gumbo.core.util.AssertUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * Utilities related to Java properties, which are Map-like property sets
 * accessed as system resource files. As specified by java.util.Properties,
 * property names (aka. IDs, map keys) are non-null and non-empty Strings, and
 * property values are non-null, but possibly empty, Strings.
 * @author jonb
 */
public class PropertyUtils {

    private PropertyUtils() {
    }

    /**
	 * Safely returns true if the property is found (recursively), regardless of
	 * value (i.e. possibly empty).
	 * @param props Temp input property store. If null, returns false.
	 * @param propId The property ID (key). If null or empty, returns false.
	 * @return The result.
	 */
    public static boolean hasProperty(Properties props, String propId) {
        if (props == null) return false;
        if (propId == null || propId.isEmpty()) return false;
        String propVal = props.getProperty(propId);
        if (propVal == null) return false;
        return true;
    }

    /**
	 * Safely returns true if the property is found (recursively) and it has a
	 * value (i.e. non-empty).
	 * @param props Temp input property store. If null, returns false.
	 * @param propId The property ID (key). If null or empty, returns false.
	 * @return The result.
	 */
    public static boolean hasPropertyValue(Properties props, String propId) {
        if (props == null) return false;
        if (propId == null || propId.isEmpty()) return false;
        String propVal = props.getProperty(propId);
        if (propVal == null || propVal.isEmpty()) return false;
        return true;
    }

    /**
	 * Safely gets the raw property value from a property store (recursively).
	 * @param props Temp input property store. If null, returns null.
	 * @param propId The property ID (key). If null or empty, returns null.
	 * @return The raw property value. Null only if the property is not found
	 * (by definition, a property value is a non-null String).
	 */
    public static String getPropertyValue(Properties props, String propId) {
        if (props == null) return null;
        if (propId == null || propId.isEmpty()) return null;
        return props.getProperty(propId);
    }

    /**
	 * Safely gets a property from a property store (recursively), as a String.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 */
    public static String getPropertyString(Properties props, String propId, String defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = props.getProperty(propId);
        if (propVal == null) {
            return defVal;
        } else {
            return propVal;
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as a String.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store,
	 * or if the property value is null or not convertible.
	 */
    public static String getPropertyString(Properties props, String propId) {
        String propVal = getPropertyString(props, propId, null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely gets a property from a property store (recursively), as an Enum. The property
	 * value is assumed to be the name of the enum constant.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 * @throws IllegalStateException if the property is found but cannot be
	 * loaded as an enum.
	 */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getPropertyEnum(Properties props, String propId, T defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = getPropertyString(props, propId, null);
        if (propVal == null) {
            return defVal;
        } else {
            try {
                return (T) Enum.valueOf(defVal.getClass(), propVal);
            } catch (Exception ex) {
                throw new IllegalStateException("Property not an enum constant." + " id=" + propId + " enum=" + defVal.getClass() + " value=" + propVal, ex);
            }
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as an Enum. The property
	 * value must be of the form "<enum_class>.<enum_name>".
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param enumType The enum type. Never null.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store, or if
	 * the property value is null or not convertible.
	 */
    public static <T extends Enum<T>> T getPropertyEnum(Properties props, String propId, Class<T> enumType) {
        T propVal = getPropertyEnum(props, propId, (T) null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely gets a property from a property store (recursively), as a Class.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 * @throws IllegalStateException if the property is found but cannot be
	 * loaded as a class.
	 */
    public static Class<?> getPropertyClass(Properties props, String propId, Class<?> defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = getPropertyString(props, propId, null);
        if (propVal == null) {
            return defVal;
        } else {
            try {
                return Class.forName(propVal);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Property not a Class." + " id=" + propId + " value=" + propVal, ex);
            }
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as a Class.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store,
	 * or if the property value is null or not convertible.
	 */
    public static Class<?> getPropertyClass(Properties props, String propId) {
        Class<?> propVal = getPropertyClass(props, propId, null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely gets a property from a property store (recursively), as a Boolean.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 * @throws IllegalStateException if the property is found but cannot be
	 * converted.
	 */
    public static Boolean getPropertyBoolean(Properties props, String propId, Boolean defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = props.getProperty(propId);
        if (propVal == null) {
            return defVal;
        } else {
            try {
                return Boolean.valueOf(propVal);
            } catch (Exception ex) {
                throw new IllegalStateException("Property not a Boolean." + " id=" + propId + " value=" + propVal);
            }
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as a Boolean.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store,
	 * or if the property value is null or not convertible.
	 */
    public static Boolean getPropertyBoolean(Properties props, String propId) {
        Boolean propVal = getPropertyBoolean(props, propId, null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely gets a property from a property store (recursively), as a Integer.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 * @throws IllegalStateException if the property is found but cannot be
	 * converted.
	 */
    public static Integer getPropertyInteger(Properties props, String propId, Integer defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = props.getProperty(propId);
        if (propVal == null) {
            return defVal;
        } else {
            try {
                return Integer.valueOf(propVal);
            } catch (Exception ex) {
                throw new IllegalStateException("Property not a Integer." + " id=" + propId + " value=" + propVal);
            }
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as a Integer.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store,
	 * or if the property value is null or not convertible.
	 */
    public static Integer getPropertyInteger(Properties props, String propId) {
        Integer propVal = getPropertyInteger(props, propId, null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely gets a property from a property store (recursively), as a Double.
	 * @param props Temp input property store. If null, returns defVal.
	 * @param propId The property ID (key). If null or empty, returns defVal.
	 * @param defVal Default value, if property not found. Possibly null.
	 * @return The property value. Null if property not found and default is
	 * null.
	 * @throws IllegalStateException if the property is found but cannot be
	 * converted.
	 */
    public static Double getPropertyDouble(Properties props, String propId, Double defVal) {
        if (props == null) return defVal;
        if (propId == null || propId.isEmpty()) return defVal;
        String propVal = props.getProperty(propId);
        if (propVal == null) {
            return defVal;
        } else {
            try {
                return Double.valueOf(propVal);
            } catch (Exception ex) {
                throw new IllegalStateException("Property not a Double." + " id=" + propId + " value=" + propVal);
            }
        }
    }

    /**
	 * Assuredly gets a property from a property store (recursively), as a Double.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @return The property value. Never null.
	 * @throws IllegalStateException if the property is not in the store,
	 * or if the property value is null or not convertible.
	 */
    public static Double getPropertyDouble(Properties props, String propId) {
        Double propVal = getPropertyDouble(props, propId, null);
        if (propVal == null) {
            throw new IllegalStateException("Property is missing." + " id=" + propId);
        }
        return propVal;
    }

    /**
	 * Safely puts a raw property value into a property store, replacing any
	 * previous value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new (by
	 * definition, a property value is a non-null String).
	 */
    public static String putPropertyValue(Properties props, String propId, String val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        return (String) props.put(propId, val);
    }

    /**
	 * Assuredly puts a property into a property store, as a String, replacing any
	 * previous value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new.
	 * @throws IllegalStateException if the old property is non-null but not
	 * convertible.
	 */
    public static String putPropertyString(Properties props, String propId, String val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        String oldVal = (String) props.put(propId, val);
        if (oldVal == null) return null;
        return oldVal;
    }

    /**
	 * Assuredly puts a property into a property store, as a Class, replacing any previous
	 * value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new.
	 */
    public static Class<?> putPropertyClass(Properties props, String propId, Class<?> val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        String oldVal = (String) props.put(propId, val.getName());
        if (oldVal == null) return null;
        try {
            return Class.forName(oldVal);
        } catch (Exception ex) {
            throw new IllegalStateException("Old property not a Class." + " id=" + propId + " oldVal=" + oldVal, ex);
        }
    }

    /**
	 * Assuredly puts a property into a property store, as a Boolean, replacing any previous
	 * value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new.
	 */
    public static Boolean putPropertyBoolean(Properties props, String propId, Boolean val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        String oldVal = (String) props.put(propId, val.toString());
        if (oldVal == null) return null;
        try {
            return Boolean.valueOf(oldVal);
        } catch (Exception ex) {
            throw new IllegalStateException("Old property not a Boolean." + " id=" + propId + " oldVal=" + oldVal, ex);
        }
    }

    /**
	 * Assuredly puts a property into a property store, as an Integer, replacing any previous
	 * value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new.
	 */
    public static Integer putPropertyInteger(Properties props, String propId, Integer val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        String oldVal = (String) props.put(propId, val.toString());
        if (oldVal == null) return null;
        try {
            return Integer.valueOf(oldVal);
        } catch (Exception ex) {
            throw new IllegalStateException("Old property not an Integer." + " id=" + propId + " oldVal=" + oldVal, ex);
        }
    }

    /**
	 * Assuredly puts a property into a property store, as a Double, replacing any previous
	 * value.
	 * @param props Temp input property store. Never null.
	 * @param propId The property ID (key). Never null or empty.
	 * @param val The property value. Never null. Possibly empty.
	 * @return The old property value. Null if the property was new.
	 */
    public static Double putPropertyDouble(Properties props, String propId, Double val) {
        if (props == null) throw new IllegalArgumentException();
        if (propId == null || propId.isEmpty()) throw new IllegalArgumentException();
        if (val == null) throw new IllegalArgumentException();
        String oldVal = (String) props.put(propId, val.toString());
        if (oldVal == null) return null;
        try {
            return Double.valueOf(oldVal);
        } catch (Exception ex) {
            throw new IllegalStateException("Old property not a Double." + " id=" + propId + " oldVal=" + oldVal, ex);
        }
    }

    /**
	 * Loads system properties into an existing property store. Properties
	 * with matching keys will be replaced.
	 * @param retVal Temp exposed property store. Loaded entries will replace
	 * corresponding old entries. Never null.
	 * @return Reference to retVal. Never null.
	 * @throws SecurityException if system properties are not accessible.
	 */
    public static <R extends Properties> R loadSystemProperties(R retVal) {
        if (retVal == null) throw new IllegalArgumentException();
        retVal.putAll(System.getProperties());
        return retVal;
    }

    /**
	 * Similar to loadSystemProperties(Properties), but tolerates
	 * SecurityException.
	 * @param retVal Temp exposed property store. Loaded entries will replace
	 * corresponding old entries. Never null.
	 * @return Reference to retVal. Null if failed.
	 * @see #loadSystemProperties(Properties)
	 */
    public static <R extends Properties> R loadSystemPropertiesSafely(R retVal) {
        try {
            loadSystemProperties(retVal);
            return retVal;
        } catch (SecurityException ex) {
            return null;
        }
    }

    /**
	 * Loads properties from file into an existing property store. Properties
	 * with matching keys will be replaced.
	 * @param url Property file URL. Never null.
	 * @param retVal Temp exposed property store. Loaded entries will replace
	 * corresponding old entries. Never null.
	 * @return Reference to retVal. Never null.
	 * @throws IOException if the file is missing or access fails.
	 */
    public static <R extends Properties> R loadProperties(URL url, R retVal) throws IOException {
        AssertUtils.assertNonNullArg(url);
        AssertUtils.assertNonNullArg(retVal);
        try {
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            try {
                retVal.load(in);
            } finally {
                in.close();
            }
        } catch (Exception ex) {
            throw new IOException("Failed to connect to URL. url=" + url);
        }
        return retVal;
    }

    /**
	 * Similar to loadProperties(URL, Properties), but tolerates null URL
	 * and all Exception (i.e. missing or inaccessible files).
	 * @param url Property file URL. Never null.
	 * @param retVal Temp exposed property store. Loaded entries will replace
	 * corresponding old entries. Never null.
	 * @return Reference to retVal. Null if failed
	 * @see #loadProperties(URL, Properties)
	 */
    public static <R extends Properties> R loadPropertiesSafely(URL url, R retVal) {
        if (url == null) return retVal;
        try {
            loadProperties(url, retVal);
            return retVal;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
	 * Saves properties to file, creating the file as needed. Typically called
	 * by an application during shutdown to save its properties as
	 * "user specific" properties.
	 * @param props Temp input property store. Never null.
	 * @param comment Optional comment included in the file. None if null.
	 * @param url Temp input property file URL. Never null.
	 * @throws IOException if the file cannot be created, or it is found but
	 * access fails.
	 */
    public static void saveProperties(Properties props, String comment, URL url) throws IOException {
        if (props == null) throw new IllegalArgumentException();
        if (url == null) throw new IllegalArgumentException();
        OutputStream out = url.openConnection().getOutputStream();
        props.store(out, comment);
        out.close();
    }

    /**
	 * Similar to saveProperties(Properties, String, URL), but tolerates null
	 * props and URL, and IOException (i.e. missing or inaccessible files).
	 * @param props Temp input property store. If null, nothing happens.
	 * @param comment Optional comment included in the file. None if null.
	 * @param url Temp input property file URL. If null, nothing happens.
	 * @return False if failed.
	 * @see #saveProperties(Properties, String, URL)
	 */
    public static boolean savePropertiesSafely(Properties props, String comment, URL url) {
        if (props == null) return true;
        if (url == null) return true;
        try {
            saveProperties(props, comment, url);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
