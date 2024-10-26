package com.liferay.portal.struts;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.MessageResourcesFactory;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;
import com.oroad.stxx.util.PropertyMessageResources;

/**
 * <a href="MultiMessageResources.java.html"><b><i>View Source </i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class MultiMessageResources extends PropertyMessageResources {

    public MultiMessageResources(MessageResourcesFactory factory, String config) {
        super(factory, config);
    }

    public MultiMessageResources(MessageResourcesFactory factory, String config, boolean returnNull) {
        super(factory, config, returnNull);
    }

    public Map getMessages() {
        return messages;
    }

    public void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;
    }

    protected void loadLocale(String localeKey) {
        synchronized (locales) {
            if (locales.get(localeKey) != null) {
                return;
            }
            locales.put(localeKey, localeKey);
        }
        String[] names = StringUtil.split(config.replace('.', '/'));
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (localeKey.length() > 0) {
                name += "_" + localeKey;
            }
            name += ".properties";
            _loadProps(name, localeKey, false);
        }
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (localeKey.length() > 0) {
                name += "_" + localeKey;
            }
            name += ".properties";
            _loadProps(name, localeKey, true);
        }
    }

    private void _loadProps(String name, String localeKey, boolean useServletContext) {
        Properties props = new Properties();
        try {
            URL url = null;
            if (useServletContext) {
                url = _servletContext.getResource("/WEB-INF/" + name);
            } else {
                ClassLoader classLoader = getClass().getClassLoader();
                url = classLoader.getResource(name);
            }
            if (url != null) {
                InputStream is = url.openStream();
                props.load(is);
                is.close();
                _log.info("Loading " + url);
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
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

    private static final Log _log = LogFactory.getLog(MultiMessageResources.class);

    private transient ServletContext _servletContext;
}
