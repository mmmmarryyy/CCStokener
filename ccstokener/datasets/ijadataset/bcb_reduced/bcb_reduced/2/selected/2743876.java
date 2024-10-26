package org.t2framework.lucy.env;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.t2framework.commons.util.ResourceUtil;
import org.t2framework.commons.util.StreamUtil;
import org.t2framework.commons.util.URLUtil;

/**
 * Environment class provides mode switch feature, such as swiching between
 * development mode and production mode.
 * 
 * @author ryushi
 */
public class Environment extends Properties {

    private static final long serialVersionUID = 877765373397839955L;

    protected Map<String, String> envs = new HashMap<String, String>();

    public void add(String env, String path) {
        this.envs.put(env, path);
    }

    public void mode(String env) {
        String path = this.envs.get(env);
        InputStream in = null;
        try {
            URL url = ResourceUtil.getResourceNoException(path);
            if (url == null) {
                throw new IllegalEnvironmentException(env);
            }
            load(URLUtil.openStream(url));
        } catch (IOException e) {
            throw new IllegalEnvironmentException(env, e);
        } finally {
            StreamUtil.close(in);
        }
    }

    class IllegalEnvironmentException extends RuntimeException {

        private static final long serialVersionUID = 2103745157560861534L;

        public IllegalEnvironmentException(String env) {
            super(env);
        }

        public IllegalEnvironmentException(String env, Throwable cause) {
            super(env, cause);
        }
    }
}
