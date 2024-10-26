package com.itextpdf.devoxx.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.itextpdf.devoxx.pojos.Pojo;
import com.itextpdf.text.ExceptionConverter;

/**
 * Object that is able to connect to the REST interface to read JSON Strings
 * and that can create POJOs for our application.
 */
public abstract class RestConnection {

    /**
	 * Gets an JSONArray containing JSONObjects corresponding with a REST request.
	 * @param rest the URI of the request.
	 * @return a JSONArray
	 * @throws IOException something went wrong during I/O
	 */
    public static JSONArray getJSONArrayFromRest(final String rest) throws IOException {
        try {
            return new JSONArray(getStringFromRest(rest));
        } catch (JSONException je) {
            throw new ExceptionConverter(je);
        }
    }

    /**
	 * Gets a JSONObject corresponding with a REST request.
	 * @param rest the URI of the request.
	 * @return a JSONObject
     * @throws IOException something went wrong during I/O
	 */
    public static JSONObject getJSONObjectFromRest(final String rest) throws IOException {
        try {
            return new JSONObject(getStringFromRest(rest));
        } catch (JSONException je) {
            throw new ExceptionConverter(je);
        }
    }

    /**
	 * Gets a String from the REST interface.
	 * @param rest the URI of the request
	 * @return a String containing a JSONArray and/or JSONObject(s)
     * @throws IOException something went wrong during I/O
	 */
    public static String getStringFromRest(final String rest) throws IOException {
        final URL url = new URL(rest);
        final InputStream is = url.openStream();
        if (is == null) {
            throw new IOException(String.format("No response from %s", rest));
        }
        final StringBuilder sb = new StringBuilder();
        final Reader r = new InputStreamReader(is, "UTF-8");
        final char[] buf = new char[1028];
        int read;
        do {
            read = r.read(buf, 0, buf.length);
            if (read > 0) {
                sb.append(buf, 0, read);
            }
        } while (read > 0);
        is.close();
        return sb.toString();
    }

    /**
	 * Populates a POJO with the information stored in a JSON object.
	 * @param pojo the empty POJO
	 * @param json the JSON object with the date
	 */
    public static void populatePojo(final Pojo pojo, final JSONObject json) {
        try {
            String key;
            Object[] value;
            for (final Method method : pojo.getClass().getMethods()) {
                if (method.getName().startsWith("set")) {
                    key = method.getName().substring(3);
                    key = key.substring(0, 1).toLowerCase() + key.substring(1);
                    try {
                        value = getParameterFromJSON(json, method.getParameterTypes()[0], key);
                    } catch (JSONException e) {
                        value = new Object[] { null };
                    }
                    method.invoke(pojo, value);
                }
            }
        } catch (ParseException pe) {
            throw new ExceptionConverter(pe);
        } catch (IllegalArgumentException iae) {
            throw new ExceptionConverter(iae);
        } catch (IllegalAccessException iae) {
            throw new ExceptionConverter(iae);
        } catch (InvocationTargetException ite) {
            throw new ExceptionConverter(ite);
        }
    }

    /**
	 * Get a value from a JSON object.
	 * @param json the JSONObject
	 * @param klass the Class of the value
	 * @param key the key that allows us to find the value
	 * @return a value in the format of klass wrapped in an Object array
	 * @throws ParseException parse problem occurred
     * @throws org.json.JSONException JSON problem occurred
	 */
    private static Object[] getParameterFromJSON(final JSONObject json, final Class<?> klass, final String key) throws JSONException, ParseException {
        final Object[] params = new Object[1];
        if (klass == String.class) {
            params[0] = json.getString(key);
        } else if (klass == Date.class) {
            SimpleDateFormat sdf;
            if (json.getString(key).length() == 10) {
                sdf = new SimpleDateFormat("yyyy-MM-dd");
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            params[0] = sdf.parse(json.getString(key));
        } else if (klass == int.class) {
            params[0] = json.getInt(key);
        } else if (klass == float.class) {
            params[0] = (float) json.getDouble(key);
        } else if (klass == boolean.class) {
            params[0] = json.getBoolean(key);
        } else if (klass.isArray()) {
            params[0] = getPojoArray(klass.getComponentType(), json.getJSONArray(key));
        }
        return params;
    }

    /**
	 * Gets an array of POJOs of a specific type based on the objects in a JSONArray.
     *
	 * @param klass the POJO class that will be used to create the Array
	 * @param jsonArray the array containing JSON objects
	 * @return an array of POJOs
	 * @throws JSONException JSON problem occurred
	 */
    private static Object getPojoArray(final Class<?> klass, final JSONArray jsonArray) throws JSONException {
        try {
            final Object array = Array.newInstance(klass, jsonArray.length());
            Object object;
            for (int i = 0; i < jsonArray.length(); i++) {
                object = klass.newInstance();
                populatePojo((Pojo) object, jsonArray.getJSONObject(i));
                Array.set(array, i, object);
            }
            return array;
        } catch (InstantiationException iae) {
            throw new ExceptionConverter(iae);
        } catch (IllegalAccessException iae) {
            throw new ExceptionConverter(iae);
        }
    }
}
