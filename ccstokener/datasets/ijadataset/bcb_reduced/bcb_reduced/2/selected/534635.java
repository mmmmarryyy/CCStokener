package com.germinus.xpression.struts;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.MessageResourcesFactory;
import org.apache.struts.util.PropertyMessageResources;
import com.liferay.portal.kernel.util.StringUtil;

public class MultiMessageResources extends PropertyMessageResources {

    private static final long serialVersionUID = 754560194905938057L;

    public MultiMessageResources(MessageResourcesFactory factory, String config) {
        super(factory, config);
    }

    public MultiMessageResources(MessageResourcesFactory factory, String config, boolean returnNull) {
        super(factory, config, returnNull);
    }

    @SuppressWarnings("unchecked")
    public Map getMessages() {
        return messages;
    }

    public void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;
    }

    @SuppressWarnings("unchecked")
    protected void loadLocale(String localeKey) {
        synchronized (locales) {
            if (locales.get(localeKey) != null) {
                return;
            }
            locales.put(localeKey, localeKey);
        }
        String[] names = StringUtil.split(config.replace('.', '/'));
        loadSubNamesProps(localeKey, names, false);
        if (_servletContext != null) {
            loadSubNamesProps(localeKey, names, true);
        }
    }

    private void loadSubNamesProps(String localeKey, String[] names, boolean useServletContext) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (localeKey.length() > 0) {
                name += "_" + localeKey;
            }
            name += ".properties";
            _loadProps(name, localeKey, false);
        }
    }

    @SuppressWarnings("unchecked")
    private void _loadProps(String name, String localeKey, boolean useServletContext) {
        Properties props = new Properties();
        try {
            URL url = null;
            if (useServletContext) {
                url = _servletContext.getResource("/WEB-INF/" + name);
            } else {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                url = classLoader.getResource(name);
            }
            if (url != null) {
                InputStream is = url.openStream();
                props.load(is);
                is.close();
                if (_log.isInfoEnabled()) {
                    _log.info("Loading " + url);
                }
            }
        } catch (Exception e) {
            _log.warn(e);
        }
        if (props.size() < 1) {
            return;
        }
        synchronized (messages) {
            Enumeration names = props.keys();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                messages.put(messageKey(localeKey, key), props.getProperty(key));
            }
        }
    }

    private static Log _log = LogFactory.getLog(MultiMessageResources.class);

    private transient ServletContext _servletContext;
}
