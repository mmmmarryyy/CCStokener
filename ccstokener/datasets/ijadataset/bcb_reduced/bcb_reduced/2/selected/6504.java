package org.zkoss.zk.ui.http;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.zkoss.lang.Classes;
import org.zkoss.io.Files;
import org.zkoss.util.logging.Log;
import org.zkoss.util.resource.ResourceCache;
import org.zkoss.idom.Element;
import org.zkoss.idom.util.IDOMs;
import org.zkoss.web.servlet.Servlets;
import org.zkoss.web.util.resource.Extendlet;
import org.zkoss.web.util.resource.ExtendletContext;
import org.zkoss.web.util.resource.ExtendletConfig;
import org.zkoss.web.util.resource.ExtendletLoader;
import org.zkoss.zk.ui.WebApp;

abstract class AbstractExtendlet implements Extendlet {

    static final Log log = Log.lookup(WpdExtendlet.class);

    ExtendletContext _webctx;

    /** DSP interpretation cache. */
    ResourceCache _cache;

    /** The provider. */
    private ThreadLocal _provider = new ThreadLocal();

    private Boolean _debugJS;

    /** Sets whether to generate JS files that is easy to debug. */
    public void setDebugJS(boolean debugJS) {
        _debugJS = Boolean.valueOf(debugJS);
        if (_cache != null) _cache.clear();
    }

    /** Returns whether to generate JS files that is easy to debug. */
    public boolean isDebugJS() {
        if (_debugJS == null) {
            final WebApp wapp = getWebApp();
            if (wapp == null) return true;
            _debugJS = Boolean.valueOf(wapp.getConfiguration().isDebugJS());
        }
        return _debugJS.booleanValue();
    }

    Provider getProvider() {
        return (Provider) _provider.get();
    }

    void setProvider(Provider provider) {
        _provider.set(provider);
    }

    WebApp getWebApp() {
        return _webctx != null ? WebManager.getWebManager(_webctx.getServletContext()).getWebApp() : null;
    }

    ServletContext getServletContext() {
        return _webctx != null ? _webctx.getServletContext() : null;
    }

    void init(ExtendletConfig config, ExtendletLoader loader) {
        _webctx = config.getExtendletContext();
        _cache = new ResourceCache(loader, 16);
        _cache.setMaxSize(1024);
        _cache.setLifetime(60 * 60 * 1000);
        final int checkPeriod = loader.getCheckPeriod();
        _cache.setCheckPeriod(checkPeriod >= 0 ? checkPeriod : 60 * 60 * 1000);
    }

    static MethodInfo getMethodInfo(Element el) {
        final String clsnm = IDOMs.getRequiredAttributeValue(el, "class");
        final String sig = IDOMs.getRequiredAttributeValue(el, "signature");
        final Class cls;
        try {
            cls = Classes.forNameByThread(clsnm);
        } catch (ClassNotFoundException ex) {
            log.error("Class not found: " + clsnm + ", " + el.getLocator());
            return null;
        }
        try {
            final Method mtd = Classes.getMethodBySignature(cls, sig, null);
            if ((mtd.getModifiers() & Modifier.STATIC) == 0) {
                log.error("Not a static method: " + mtd);
                return null;
            }
            final Object[] args = new Object[mtd.getParameterTypes().length];
            for (int j = 0; j < args.length; ++j) args[j] = el.getAttributeValue("arg" + j);
            return new MethodInfo(mtd, args);
        } catch (ClassNotFoundException ex) {
            log.realCauseBriefly("Unable to load class when resolving " + sig + " " + el.getLocator(), ex);
        } catch (NoSuchMethodException ex) {
            log.error("Method not found in " + clsnm + ": " + sig + " " + el.getLocator());
        }
        return null;
    }

    String invoke(MethodInfo mi) {
        final Provider provider = getProvider();
        final Class[] argTypes = mi.method.getParameterTypes();
        final Object[] args = mi.arguments;
        if (provider != null) for (int j = 0; j < args.length; ++j) if (ServletRequest.class.isAssignableFrom(argTypes[j])) args[j] = provider.request; else if (ServletResponse.class.isAssignableFrom(argTypes[j])) args[j] = provider.response; else if (ServletContext.class.isAssignableFrom(argTypes[j])) args[j] = getServletContext();
        try {
            Object o = mi.method.invoke(null, args);
            return o instanceof String ? (String) o : "";
        } catch (Throwable ex) {
            log.error("Unable to invoke " + mi.method, ex);
            return "";
        }
    }

    public boolean getFeature(int feature) {
        return feature == ALLOW_DIRECT_INCLUDE;
    }

    static class MethodInfo {

        final Method method;

        final Object[] arguments;

        MethodInfo(Method method, Object[] arguments) {
            this.method = method;
            this.arguments = arguments;
        }
    }

    class Provider {

        final HttpServletRequest request;

        final HttpServletResponse response;

        Provider(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        InputStream getResourceAsStream(String path, boolean locate) throws IOException, ServletException {
            if (locate) path = Servlets.locate(_webctx.getServletContext(), this.request, path, _webctx.getLocator());
            if (_cache.getCheckPeriod() >= 0) {
                try {
                    URL url = _webctx.getResource(path);
                    if (url != null) return url.openStream();
                } catch (Throwable ex) {
                    log.warningBriefly("Unable to read from URL: " + path, ex);
                }
            }
            return _webctx.getResourceAsStream(path);
        }

        URL getResource(String path) throws IOException {
            return _webctx.getResource(path);
        }
    }

    class FileProvider extends Provider {

        private String _parent;

        FileProvider(File file, boolean debugJS) {
            super(null, null);
            _parent = file.getParent();
        }

        InputStream getResourceAsStream(String path, boolean locate) throws IOException {
            path = getRealPath(path);
            final File file = new File(_parent, path);
            return locate ? new FileInputStream(Files.locate(file.getPath())) : new FileInputStream(file);
        }

        URL getResource(String path) throws IOException {
            path = getRealPath(path);
            return new File(_parent, path).toURI().toURL();
        }

        protected String getRealPath(String path) {
            if (isDebugJS()) {
                final int j = path.lastIndexOf('.');
                if (j >= 0) return path.substring(0, j) + ".src" + path.substring(j);
            }
            return path;
        }
    }
}
