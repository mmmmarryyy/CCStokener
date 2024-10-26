package com.rapidminer.operator.io.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;

/**
 * Provides a connection to a URL.
 * 
 * @author Tobias Malbrecht
 */
public class URLConnectionProvider {

    public static final String PARAMETER_URL = "url";

    public static final String PARAMETER_USER_AGENT = "user_agent";

    public static final String PARAMETER_RANDOM_USER_AGENT = "random_user_agent";

    public static final String PARAMETER_CONNECTION_TIMEOUT = "connection_timeout";

    public static final String PARAMETER_READ_TIMEOUT = "read_timeout";

    public static final String DEFAULT_ENCODING = "iso-8859-1";

    public static URL getURL(ParameterHandler handler) throws UndefinedParameterError, MalformedURLException {
        return new URL(handler.getParameterAsString(PARAMETER_URL));
    }

    public static URLConnection getConnectionInstance(ParameterHandler handler) throws UndefinedParameterError, IOException {
        return getConnectionInstance(handler, getURL(handler));
    }

    public static URLConnection getConnectionInstance(ParameterHandler handler, URL url) throws UndefinedParameterError, IOException {
        URLConnection connection = url.openConnection();
        String userAgent = UserAgent.DEFAULT_USER_AGENT;
        if (handler.isParameterSet(PARAMETER_RANDOM_USER_AGENT) && handler.getParameterAsBoolean(PARAMETER_RANDOM_USER_AGENT)) {
            userAgent = UserAgent.getRandomUserAgent();
        } else if (handler.isParameterSet(PARAMETER_USER_AGENT)) {
            userAgent = handler.getParameterAsString(PARAMETER_USER_AGENT);
        }
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(handler.getParameterAsInt(PARAMETER_CONNECTION_TIMEOUT));
        connection.setReadTimeout(handler.getParameterAsInt(PARAMETER_READ_TIMEOUT));
        return connection;
    }

    public static String getEncoding(URLConnection connection) {
        return parseEncoding(connection.getContentType());
    }

    public static String parseEncoding(String contentType) {
        String charset = null;
        if (contentType != null) {
            final String[] parts = contentType.split(";");
            for (int i = 1; i < parts.length && charset == null; i++) {
                final String t = parts[i].trim();
                final int index = t.toLowerCase().indexOf("charset=");
                if (index != -1) {
                    charset = t.substring(index + 8);
                }
            }
        }
        if (charset == null) {
            charset = DEFAULT_ENCODING;
        }
        return charset;
    }

    public static List<ParameterType> getURLParameterTypes(ParameterHandler handler) {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(new ParameterTypeString(PARAMETER_URL, "The url from which should be read.", false, false));
        return types;
    }

    public static List<ParameterType> getParameterTypes(ParameterHandler handler, boolean specifyURL) {
        List<ParameterType> types = new LinkedList<ParameterType>();
        if (specifyURL) {
            types.addAll(getURLParameterTypes(handler));
        }
        types.add(new ParameterTypeBoolean(PARAMETER_RANDOM_USER_AGENT, "Choose a user agent randomly from a set of 7000 user agents", false));
        ParameterType type = new ParameterTypeString(PARAMETER_USER_AGENT, "The user agent property.", true, true);
        type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_RANDOM_USER_AGENT, false, false));
        types.add(type);
        types.add(new ParameterTypeInt(PARAMETER_CONNECTION_TIMEOUT, "The timeout (in ms) for the connection.", 0, Integer.MAX_VALUE, 10000));
        types.add(new ParameterTypeInt(PARAMETER_READ_TIMEOUT, "The timeout (in ms) for reading from the url.", 0, Integer.MAX_VALUE, 10000));
        return types;
    }

    public static List<ParameterType> getParameterTypes(ParameterHandler handler) {
        return getParameterTypes(handler, true);
    }
}
