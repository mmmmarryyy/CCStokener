package org.apache.shale.tiger.view.faces;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shale.tiger.config.FacesConfigConfig;
import org.apache.shale.tiger.config.FacesConfigParser;
import org.apache.shale.tiger.managed.Bean;
import org.apache.shale.tiger.managed.Property;
import org.apache.shale.tiger.managed.Value;
import org.apache.shale.tiger.managed.config.ManagedBeanConfig;
import org.apache.shale.tiger.managed.config.ManagedPropertyConfig;
import org.apache.shale.tiger.register.FacesComponent;
import org.apache.shale.tiger.register.FacesConverter;
import org.apache.shale.tiger.register.FacesPhaseListener;
import org.apache.shale.tiger.register.FacesRenderer;
import org.apache.shale.tiger.register.FacesValidator;
import org.apache.shale.tiger.register.faces.PhaseListenerAdapter;
import org.apache.shale.tiger.view.Activate;
import org.apache.shale.tiger.view.Destroy;
import org.apache.shale.tiger.view.Init;
import org.apache.shale.tiger.view.Passivate;
import org.apache.shale.tiger.view.Preprocess;
import org.apache.shale.tiger.view.Prerender;
import org.apache.shale.tiger.view.Request;
import org.apache.shale.tiger.view.Session;
import org.apache.shale.tiger.view.View;
import org.apache.shale.util.Messages;
import org.apache.shale.view.AbstractApplicationBean;
import org.apache.shale.view.AbstractRequestBean;
import org.apache.shale.view.AbstractSessionBean;
import org.apache.shale.view.ViewController;
import org.apache.shale.view.faces.FacesConstants;
import org.apache.shale.view.faces.LifecycleListener;
import org.xml.sax.SAXException;

/**
 * <p>
 * Specialized version of <code>org.apache.shale.view.faces.LifecycleListener</code> that implements callbacks to methods tagged by appropriate annotations,
 * rather than requiring the containing classes to implement a particular interface or extend a particular subclass.
 * </p>
 * 
 * <p>
 * <strong>IMPLEMENTATION NOTE:</strong> The standard LifecycleListener instance will <em>delegate</em> to methods of this class after performing its own
 * appropriate processing. Therefore, implementation methods must <strong>NOT</strong> call their superclass counterparts. Doing so will cause any infinite
 * recursion and ultimately a stack overflow error.
 * </p>
 * 
 * $Id: LifecycleListener2.java 489966 2006-12-24 01:43:42Z craigmcc $
 * 
 * @since 1.0.3
 */
public class LifecycleListener2 extends LifecycleListener {

    /**
     * <p>
     * Create a new lifecycle listener.
     * </p>
     */
    public LifecycleListener2() {
    }

    /**
     * <p>
     * Servlet context init parameter which defines which packages to scan for beans.
     * </p>
     */
    public static final String SCAN_PACKAGES = "org.apache.shale.tiger.SCAN_PACKAGES";

    /**
     * <p>
     * Application scope attribute under which a configured {@link FacesConfigConfig} bean will be stored, containing information parsed from the relevant
     * <code>faces-config.xml</code> resource(s) for this application.
     * </p>
     */
    public static final String FACES_CONFIG_CONFIG = "org.apache.shale.tiger.FACES_CONFIG_CONFIG";

    /**
     * <p>
     * Context relative path to the default <code>faces-config.xml</code> resource for a JavaServer Faces application.
     * </p>
     */
    private static final String FACES_CONFIG_DEFAULT = "/WEB-INF/faces-config.xml";

    /**
     * <p>
     * Resource path used to acquire implicit resources buried inside application JARs.
     * </p>
     */
    private static final String FACES_CONFIG_IMPLICIT = "META-INF/faces-config.xml";

    /**
     * <p>
     * Prefix path used to locate web application classes for this web application.
     * </p>
     */
    private static final String WEB_CLASSES_PREFIX = "/WEB-INF/classes/";

    /**
     * <p>
     * Prefix path used to locate web application libraries for this web application.
     * </p>
     */
    private static final String WEB_LIB_PREFIX = "/WEB-INF/lib/";

    /**
     * <p>
     * Parameter values array that passes no parameters.
     * </p>
     */
    private static final Object[] PARAMETERS = new Object[0];

    /**
     * <p>
     * The <code>ServletContext</code> instance for this application.
     * </p>
     */
    private transient ServletContext servletContext = null;

    /**
     * <p>
     * Respond to a context initialized event. Forcibly replace the managed bean services that are different when the Tiger extensions are loaded. Then, process
     * the <code>faces-config.xml</code> resources for this application in order to record the configuration of managed beans.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void contextInitialized(ServletContextEvent event) {
        if (log().isInfoEnabled()) {
            log().info(messages().getMessage("lifecycle.initialized"));
        }
        servletContext = event.getServletContext();
        servletContext.setAttribute(FacesConstants.VIEW_CALLBACKS, new ViewControllerCallbacks2());
        FacesConfigConfig config = facesConfigConfig();
        servletContext.setAttribute(FACES_CONFIG_CONFIG, config);
        Application application = null;
        try {
            application = application();
        } catch (Exception e) {
            ;
        }
        List<Class> classes;
        String scanPackages = servletContext.getInitParameter(SCAN_PACKAGES);
        if (scanPackages != null) {
            try {
                classes = packageClasses(servletContext, scanPackages);
            } catch (ClassNotFoundException e) {
                throw new FacesException(e);
            } catch (IOException e) {
                throw new FacesException(e);
            }
        } else {
            try {
                classes = webClasses(servletContext);
            } catch (ClassNotFoundException e) {
                throw new FacesException(e);
            }
        }
        try {
            for (Class clazz : classes) {
                if (application != null) {
                    registerClass(clazz, application);
                } else {
                    queueClass(clazz);
                }
                scanClass(clazz, config);
            }
        } catch (Exception e) {
            throw new FacesException(e);
        }
        if (scanPackages == null) {
            List<JarFile> archives = null;
            try {
                archives = webArchives(servletContext);
                for (JarFile archive : archives) {
                    classes = archiveClasses(servletContext, archive);
                    for (Class clazz : classes) {
                        if (application != null) {
                            registerClass(clazz, application);
                        } else {
                            queueClass(clazz);
                        }
                        scanClass(clazz, config);
                    }
                }
            } catch (Exception e) {
                throw new FacesException(e);
            }
        }
        FacesConfigParser parser = facesConfigParser(config);
        List<URL> resources = null;
        URL url = null;
        try {
            resources = implicitResources(servletContext);
            for (URL resource : resources) {
                url = resource;
                parseResource(parser, resource);
            }
        } catch (Exception e) {
            throw new FacesException(messages().getMessage("lifecycle.exception", new Object[] { url.toExternalForm() }), e);
        }
        try {
            resources = explicitResources(servletContext);
            for (URL resource : resources) {
                url = resource;
                parseResource(parser, resource);
            }
        } catch (Exception e) {
            throw new FacesException(messages().getMessage("lifecycle.exception", new Object[] { url.toExternalForm() }), e);
        }
        try {
            url = servletContext.getResource(FACES_CONFIG_DEFAULT);
            if ((url != null) && !resources.contains(url)) {
                parseResource(parser, url);
            }
        } catch (Exception e) {
            throw new FacesException(messages().getMessage("lifecycle.exception", new Object[] { url.toExternalForm() }), e);
        }
        if (log().isInfoEnabled()) {
            log().info(messages().getMessage("lifecycle.completed"));
        }
    }

    /**
     * <p>
     * Return a list of the classes defined within the given packages If there are no such classes, a zero-length list will be returned.
     * </p>
     * 
     * @param scanPackages
     *            the package configuration
     * 
     * @exception ClassNotFoundException
     *                if a located class cannot be loaded
     * @exception IOException
     *                if an input/output error occurs
     */
    private List<Class> packageClasses(ServletContext servletContext, String scanPackages) throws ClassNotFoundException, IOException {
        List<Class> list = new ArrayList<Class>();
        String[] scanPackageTokens = scanPackages.split(",");
        for (String scanPackageToken : scanPackageTokens) {
            if (scanPackageToken.toLowerCase().endsWith(".jar")) {
                URL jarResource = servletContext.getResource(WEB_LIB_PREFIX + scanPackageToken);
                String jarURLString = "jar:" + jarResource.toString() + "!/";
                URL url = new URL(jarURLString);
                JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                list.addAll(archiveClasses(servletContext, jarFile));
            } else {
                PackageInfo.getInstance().getClasses(list, scanPackageToken);
            }
        }
        return list;
    }

    /**
     * <p>
     * Respond to a context destroyed event. Clean up our allocated application scope attributes.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void contextDestroyed(ServletContextEvent event) {
        if (log().isInfoEnabled()) {
            log().info(messages().getMessage("lifecycle.destroyed"));
        }
        event.getServletContext().removeAttribute(FACES_CONFIG_CONFIG);
    }

    /**
     * <p>
     * Respond to an application scope attribute being added. If the value is an {@link AbstractApplicationBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeAdded(ServletContextAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireApplicationInit(value);
        }
    }

    /**
     * <p>
     * Respond to an application scope attribute being replaced. If the old value was an {@link AbstractApplicationBean}, call its <code>destroy()</code>
     * method. If the new value is an {@link AbstractApplicationBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeReplaced(ServletContextAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireApplicationDestroy(value);
        }
        value = event.getServletContext().getAttribute(event.getName());
        if (value != null) {
            fireApplicationInit(value);
        }
    }

    /**
     * <p>
     * Respond to an application scope attribute being removed. If the old value was an {@link AbstractApplicationBean}, call its <code>destroy()</code>
     * method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeRemoved(ServletContextAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireApplicationDestroy(value);
        }
    }

    /**
     * <p>
     * Respond to a session created event. No special processing is required.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void sessionCreated(HttpSessionEvent event) {
    }

    /**
     * <p>
     * Respond to a session destroyed event. No special processing is required
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void sessionDestroyed(HttpSessionEvent event) {
    }

    /**
     * <p>
     * Respond to a "session will passivate" event. Notify all session scope attributes that are {@link AbstractSessionBean}s.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void sessionWillPassivate(HttpSessionEvent event) {
        Enumeration names = event.getSession().getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Object value = event.getSession().getAttribute(name);
            if (value != null) {
                fireSessionPassivate(value);
            }
        }
    }

    /**
     * <p>
     * Respond to a "session did activate" event. Notify all session scope attributes that are {@link AbstractSessionBean}s.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void sessionDidActivate(HttpSessionEvent event) {
        Enumeration names = event.getSession().getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Object value = event.getSession().getAttribute(name);
            if (value != null) {
                fireSessionActivate(value);
            }
        }
    }

    /**
     * <p>
     * Respond to a session scope attribute being added. If the value is an {@link AbstractSessionBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeAdded(HttpSessionBindingEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireSessionInit(value);
        }
    }

    /**
     * <p>
     * Respond to a session scope attribute being replaced. If the old value was an {@link AbstractSessionBean}, call its <code>destroy()</code> method. If
     * the new value is an {@link AbstractSessionBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeReplaced(HttpSessionBindingEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireSessionDestroy(value);
        }
        value = event.getSession().getAttribute(event.getName());
        if (value != null) {
            fireSessionInit(value);
        }
    }

    /**
     * <p>
     * Respond to a session scope attribute being removed. If the old value was an {@link AbstractSessionBean}, call its <code>destroy()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeRemoved(HttpSessionBindingEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireSessionDestroy(value);
        }
    }

    /**
     * <p>
     * Respond to a request created event. If we have accumulated any classes to register with our JSF implementation (but could not initially because it was
     * not initialized before we were), register them now.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void requestInitialized(ServletRequestEvent event) {
        queueRegister();
    }

    /**
     * <p>
     * Respond to a request destroyed event. Cause any instance of ViewController or AbstractRequestBean, plus any bean whose class contains the appropriate
     * annotations, to be removed (which will trigger an attribute removed event).
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void requestDestroyed(ServletRequestEvent event) {
        List list = new ArrayList();
        ServletRequest request = event.getServletRequest();
        Enumeration names = request.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Object value = request.getAttribute(name);
            if ((value instanceof AbstractRequestBean) || (value instanceof ViewController)) {
                list.add(name);
                continue;
            }
            Class clazz = value.getClass();
            if ((clazz.getAnnotation(Request.class) != null) || (clazz.getAnnotation(View.class) != null)) {
                list.add(name);
                continue;
            }
        }
        Iterator keys = list.iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            event.getServletRequest().removeAttribute(key);
        }
    }

    /**
     * <p>
     * Respond to a request scope attribute being added. If the value is an {@link AbstractRequestBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeAdded(ServletRequestAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireRequestInit(value);
        }
    }

    /**
     * <p>
     * Respond to a request scope attribute being replaced. If the old value was an {@link AbstractRequestBean}, call its <code>destroy()</code> method. If
     * the new value is an {@link AbstractRequestBean}, call its <code>init()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeReplaced(ServletRequestAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireRequestDestroy(value);
        }
        value = event.getServletRequest().getAttribute(event.getName());
        if (value != null) {
            fireRequestInit(value);
        }
    }

    /**
     * <p>
     * Respond to a request scope attribute being removed. If the old value was an {@link AbstractRequestBean}, call its <code>destroy()</code> method.
     * </p>
     * 
     * @param event
     *            Event to be processed
     */
    public void attributeRemoved(ServletRequestAttributeEvent event) {
        Object value = event.getValue();
        if (value != null) {
            fireRequestDestroy(value);
        }
    }

    /**
     * <p>
     * Fire a destroy event on an
     * 
     * @{link AbstractApplicationBean}.
     *        </p>
     * 
     * @param bean
     *            {@link AbstractApplicationBean} to fire event on
     */
    protected void fireApplicationDestroy(Object bean) {
        if (bean instanceof AbstractApplicationBean) {
            super.fireApplicationDestroy(bean);
            return;
        }
        try {
            Method method = method(bean, Destroy.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire an init event on an {@link AbstractApplicationBean}.
     * </p>
     * 
     * @param bean
     *            {@link AbstractApplicationBean} to fire event on
     */
    protected void fireApplicationInit(Object bean) {
        if (bean instanceof AbstractApplicationBean) {
            super.fireApplicationInit(bean);
            return;
        }
        try {
            Method method = method(bean, Init.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire a destroy event on an
     * 
     * @{link AbstractRequestBean}.
     *        </p>
     * 
     * @param bean
     *            {@link AbstractRequestBean} to fire event on
     */
    protected void fireRequestDestroy(Object bean) {
        if ((bean instanceof AbstractRequestBean) || (bean instanceof ViewController)) {
            super.fireRequestDestroy(bean);
            return;
        }
        try {
            Method method = method(bean, Destroy.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire an init event on an {@link AbstractRequestBean}.
     * </p>
     * 
     * @param bean
     *            {@link AbstractRequestBean} to fire event on
     */
    protected void fireRequestInit(Object bean) {
        if ((bean instanceof AbstractRequestBean) || (bean instanceof ViewController)) {
            super.fireRequestInit(bean);
            return;
        }
        try {
            Method method = method(bean, Init.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire an activate event on an
     * 
     * @{link AbstractSessionBean}.
     *        </p>
     * 
     * @param bean
     *            {@link AbstractSessionBean} to fire event on
     */
    protected void fireSessionActivate(Object bean) {
        if (bean instanceof AbstractSessionBean) {
            super.fireSessionActivate(bean);
            return;
        }
        try {
            Method method = method(bean, Activate.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire a destroy event on an
     * 
     * @{link AbstractSessionBean}.
     *        </p>
     * 
     * @param bean
     *            {@link AbstractSessionBean} to fire event on
     */
    protected void fireSessionDestroy(Object bean) {
        if (bean instanceof AbstractSessionBean) {
            super.fireSessionDestroy(bean);
            return;
        }
        try {
            Method method = method(bean, Destroy.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire an init event on an {@link AbstractSessionBean}.
     * </p>
     * 
     * @param bean
     *            {@link AbstractSessionBean} to fire event on
     */
    protected void fireSessionInit(Object bean) {
        if (bean instanceof AbstractSessionBean) {
            super.fireSessionInit(bean);
            return;
        }
        try {
            Method method = method(bean, Init.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * Fire an passivate event on an
     * 
     * @{link AbstractSessionBean}.
     *        </p>
     * 
     * @param bean
     *            {@link AbstractSessionBean} to fire event on
     */
    protected void fireSessionPassivate(Object bean) {
        if (bean instanceof AbstractSessionBean) {
            super.fireSessionPassivate(bean);
            return;
        }
        try {
            Method method = method(bean, Passivate.class);
            if (method != null) {
                method.invoke(bean, PARAMETERS);
            }
        } catch (InvocationTargetException e) {
            handleException(FacesContext.getCurrentInstance(), (Exception) e.getCause());
        } catch (Exception e) {
            handleException(FacesContext.getCurrentInstance(), e);
        }
    }

    /**
     * <p>
     * The Application instance for this application.
     * </p>
     */
    private Application application = null;

    /**
     * <p>
     * Return the <code>Application</code> for this application.
     * </p>
     */
    private Application application() {
        if (application == null) {
            application = ((ApplicationFactory) FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY)).getApplication();
        }
        return application;
    }

    /**
     * <p>
     * Return a list of classes to examine from the specified JAR archive. If this archive has no classes in it, a zero-length list is returned.
     * </p>
     * 
     * @param context
     *            <code>ServletContext</code> instance for this application
     * @param jar
     *            <code>JarFile</code> for the archive to be scanned
     * 
     * @exception ClassNotFoundException
     *                if a located class cannot be loaded
     */
    private List<Class> archiveClasses(ServletContext context, JarFile jar) throws ClassNotFoundException {
        List<Class> list = new ArrayList<Class>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (name.startsWith("META-INF/")) {
                continue;
            }
            if (!name.endsWith(".class")) {
                continue;
            }
            name = name.substring(0, name.length() - 6);
            Class clazz = null;
            try {
                clazz = loader.loadClass(name.replace('/', '.'));
            } catch (NoClassDefFoundError e) {
                ;
            } catch (Exception e) {
                ;
            }
            if (clazz != null) {
                list.add(clazz);
            }
        }
        return list;
    }

    /**
     * <p>
     * Return a list of URLs of <code>faces-config.xml</code> resources for this web application that have been explicitly listed in the appropriate context
     * initialization parameter. If there are no such resources, a zero-length list is returned.
     * </p>
     * 
     * @param servletContext
     *            <code>ServletContext</code> instance for this application
     * 
     * @exception MalformedURLException
     *                if a context relative resource path has incorrect syntax
     */
    private List<URL> explicitResources(ServletContext servletContext) throws MalformedURLException {
        List<URL> list = new ArrayList<URL>();
        String resources = servletContext.getInitParameter("javax.faces.CONFIG_FILES");
        if (resources == null) {
            return list;
        }
        while (resources.length() > 0) {
            int comma = resources.indexOf(',');
            if (comma < 0) {
                resources = resources.trim();
                if (resources.length() > 0) {
                    URL url = servletContext.getResource(resources);
                    if (url != null) {
                        list.add(url);
                    }
                }
                resources = "";
            } else {
                URL url = servletContext.getResource(resources.substring(0, comma).trim());
                if (url != null) {
                    list.add(url);
                }
                resources = resources.substring(comma + 1);
            }
        }
        return list;
    }

    /**
     * <p>
     * Create and return an empty {@link FacesConfigConfig} bean that will be filled with information later on.
     * </p>
     */
    private FacesConfigConfig facesConfigConfig() {
        return new FacesConfigConfig();
    }

    /**
     * <p>
     * Create and return a configured {@link FacesConfigParser} instance to be used for parsing <code>faces-config.xml</code> resources. The caller will need
     * to set the <code>resource</code> property on this instance before calling the <code>parse()</code> method.
     * </p>
     * 
     * @param config
     *            <code>FacesConfigBean</code> used to store the information gathered while parsing configuration resources
     */
    private FacesConfigParser facesConfigParser(FacesConfigConfig config) {
        FacesConfigParser parser = new FacesConfigParser();
        parser.setFacesConfig(config);
        parser.setValidating(false);
        return parser;
    }

    /**
     * <p>
     * Return an array of all <code>Field</code>s reflecting declared fields in this class, or in any superclass other than <code>java.lang.Object</code>.
     * </p>
     * 
     * @param clazz
     *            Class to be analyzed
     */
    private Field[] fields(Class clazz) {
        Map<String, Field> fields = new HashMap<String, Field>();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
        } while ((clazz = clazz.getSuperclass()) != Object.class);
        return (Field[]) fields.values().toArray(new Field[fields.size()]);
    }

    /**
     * <p>
     * Return a list of URLs to implicit configuration resources embedded in this application.
     * </p>
     * 
     * @param servletContext
     *            <code>ServletContext</code> instance for this application
     * 
     * @exception IOException
     *                if an input/output error occurs
     */
    private List<URL> implicitResources(ServletContext servletContext) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }
        Enumeration items = loader.getResources(FACES_CONFIG_IMPLICIT);
        List<URL> list = new ArrayList<URL>();
        while (items.hasMoreElements()) {
            list.add((URL) items.nextElement());
        }
        return list;
    }

    /**
     * <p>
     * The lifecycle instance for this application.
     * </p>
     */
    private Lifecycle lifecycle = null;

    /**
     * <p>
     * Return the <code>Lifecycle</code> for this application.
     * </p>
     */
    private Lifecycle lifecycle() {
        if (lifecycle == null) {
            String lifecycleId = servletContext.getInitParameter("javax.faces.LIFECYCLE_ID");
            if (lifecycleId == null) {
                lifecycleId = LifecycleFactory.DEFAULT_LIFECYCLE;
            }
            lifecycle = ((LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY)).getLifecycle(lifecycleId);
        }
        return lifecycle;
    }

    /**
     * <p>
     * The <code>Log</code> instance we will be using.
     * </p>
     */
    private transient Log log = null;

    /**
     * <p>
     * Return the <code>Log</code> instance to be used for this class, instantiating a new one if necessary.
     * </p>
     */
    private Log log() {
        if (log == null) {
            log = LogFactory.getLog(LifecycleListener2.class);
        }
        return log;
    }

    /**
     * <p>
     * The <code>Messages</code> instance we will be using.
     * </p>
     */
    private transient Messages messages = null;

    /**
     * <p>
     * Return the <code>Messages</code> instance to be used for this class, instantiating a new one if necessary.
     * </p>
     */
    private Messages messages() {
        if (messages == null) {
            messages = new Messages("org.apache.shale.tiger.faces.Bundle", Thread.currentThread().getContextClassLoader());
        }
        return messages;
    }

    /**
     * <p>
     * The set of method annotations for callbacks of interest.
     * </p>
     */
    private static final Class[] annotations = { Init.class, Preprocess.class, Prerender.class, Destroy.class, Activate.class, Passivate.class };

    /**
     * <p>
     * The set of class annotations for classes of interest.
     * </p>
     */
    private static final Class[] markers = { View.class, Request.class, Session.class, org.apache.shale.tiger.view.Application.class };

    /**
     * <p>
     * Data structure to maintain information about annotated methods. In this map, the key is the Class being analyzed, and the value is an inner map. In the
     * inner map, the key is an Annotation class, and the value is the corresponding Method instance.
     * </p>
     */
    private transient Map<Class, Map<Class, Method>> maps = new HashMap<Class, Map<Class, Method>>();

    /**
     * <p>
     * Return the <code>Method</code> to be called for the specified annotation on the specified instance, if any. If there is no such method, return
     * <code>null</code>.
     * </p>
     * 
     * @param instance
     *            Instance on which callbacks will be performed
     * @param annotation
     *            Annotation for which to return a method
     */
    private Method method(Object instance, Class annotation) {
        Class clazz = instance.getClass();
        boolean found = false;
        for (Class marker : markers) {
            if (clazz.getAnnotation(marker) != null) {
                found = true;
                break;
            }
        }
        if (!found) {
            return null;
        }
        synchronized (maps) {
            Map<Class, Method> map = maps.get(clazz);
            if (map != null) {
                return map.get(annotation);
            }
            map = new HashMap<Class, Method>();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length > 0) {
                    continue;
                }
                for (Class anno : annotations) {
                    if (method.getAnnotation(anno) != null) {
                        map.put(anno, method);
                    }
                }
            }
            maps.put(clazz, map);
            return map.get(annotation);
        }
    }

    /**
     * <p>
     * Use the specified parser to parse the resource at the specified URL, which will accumulate additional information into the <code>FacesConfigConfig</code>
     * instance configured on the parser.
     * </p>
     * 
     * @param parser
     *            FacesConfigParser instance to be used
     * @param resource
     *            URL of the resource to be parsed
     * 
     * @exception IOException
     *                if an input/output error occurs
     * @exception SAXException
     *                if an XML parsing error occurs
     */
    private void parseResource(FacesConfigParser parser, URL resource) throws IOException, SAXException {
        if (log().isDebugEnabled()) {
            log().debug("Parsing faces-config.xml resource '" + resource.toExternalForm() + "'");
        }
        parser.setResource(resource);
        parser.parse();
    }

    /**
     * <p>
     * A list of classes that need to be registered with the JSF implementation after it has been started.
     * </p>
     */
    private List<Class> queue = new ArrayList<Class>();

    /**
     * <p>
     * Queue the specified class to be registered (via <code>registerClass()</code>) at a later time.
     * </p>
     * 
     * @param clazz
     *            Class instance to be queued
     */
    private void queueClass(Class clazz) {
        queue.add(clazz);
    }

    /**
     * <p>
     * Register any classes that have been queued. This method is synchronzied because it is called from a request listener, and may therefore be subject to
     * race conditions if multiple requests are received simultaneously.
     * </p>
     */
    private synchronized void queueRegister() {
        if (queue == null) {
            return;
        }
        for (Class clazz : queue) {
            registerClass(clazz, application());
        }
        queue = null;
    }

    /**
     * <p>
     * The render kit factory for this application.
     * </p>
     */
    private RenderKitFactory rkFactory = null;

    /**
     * <p>
     * Return the <code>RenderKitFactory</code> for this application.
     * </p>
     */
    private RenderKitFactory renderKitFactory() {
        if (rkFactory == null) {
            rkFactory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        }
        return rkFactory;
    }

    /**
     * <p>
     * Register the specified class with the specified JavaServer Faces <code>Application</code> instance, based on annotations that the class is annotated
     * with.
     * </p>
     * 
     * @param clazz
     *            Class to be registered
     * @param application
     *            <code>Application</code> instance with which to register this class
     */
    private void registerClass(Class clazz, Application application) {
        if (log().isTraceEnabled()) {
            log().trace("registerClass(" + clazz.getName() + ")");
        }
        FacesComponent comp = (FacesComponent) clazz.getAnnotation(FacesComponent.class);
        if (comp != null) {
            if (log().isTraceEnabled()) {
                log().trace("addComponent(" + comp.value() + "," + clazz.getName() + ")");
            }
            application().addComponent(comp.value(), clazz.getName());
        }
        FacesConverter conv = (FacesConverter) clazz.getAnnotation(FacesConverter.class);
        if (conv != null) {
            if (conv.value() != null && !"".equals(conv.value())) {
                if (log().isTraceEnabled()) {
                    log().trace("addConverter(" + conv.value() + "," + clazz.getName() + ")");
                }
                application().addConverter(conv.value(), clazz.getName());
            }
            if (conv.converterForClass() != null && !(Object.class == conv.converterForClass())) {
                if (log().isTraceEnabled()) {
                    log().trace("addConverter(" + conv.converterForClass() + "," + clazz.getName() + ")");
                }
                application().addConverter(conv.converterForClass(), clazz.getName());
            }
        }
        FacesPhaseListener list = (FacesPhaseListener) clazz.getAnnotation(FacesPhaseListener.class);
        if (list != null) {
            try {
                Lifecycle lifecycle = lifecycle();
                Object instance = clazz.newInstance();
                if (instance instanceof PhaseListener) {
                    lifecycle.addPhaseListener((PhaseListener) instance);
                } else {
                    lifecycle.addPhaseListener(new PhaseListenerAdapter(instance));
                }
            } catch (FacesException e) {
                throw e;
            } catch (Exception e) {
                throw new FacesException(e);
            }
        }
        FacesRenderer rend = (FacesRenderer) clazz.getAnnotation(FacesRenderer.class);
        if (rend != null) {
            String renderKitId = rend.renderKitId();
            if (renderKitId == null) {
                renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
            }
            if (log().isTraceEnabled()) {
                log().trace("addRenderer(" + renderKitId + ", " + rend.componentFamily() + ", " + rend.rendererType() + ", " + clazz.getName() + ")");
            }
            try {
                RenderKit rk = renderKitFactory().getRenderKit(null, renderKitId);
                rk.addRenderer(rend.componentFamily(), rend.rendererType(), (Renderer) clazz.newInstance());
            } catch (Exception e) {
                throw new FacesException(e);
            }
        }
        FacesValidator val = (FacesValidator) clazz.getAnnotation(FacesValidator.class);
        if (val != null) {
            if (log().isTraceEnabled()) {
                log().trace("addValidator(" + val.value() + "," + clazz.getName() + ")");
            }
            application().addValidator(val.value(), clazz.getName());
        }
    }

    /**
     * <p>
     * Scan the specified class for those that have annotations of interest, and construct appropriate configuration metadata attached to the specified
     * {@link FacesConfigConfig} bean.
     * </p>
     * 
     * @param clazz
     *            Class to be scanned
     * @param config
     *            {@link FacesConfigConfig} to be updated
     */
    private void scanClass(Class clazz, FacesConfigConfig config) {
        if (log().isTraceEnabled()) {
            log().trace("Scanning class '" + clazz.getName() + "'");
        }
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        if (bean != null) {
            if (log().isDebugEnabled()) {
                log().debug("Class '" + clazz.getName() + "' has an @Bean annotation");
            }
            ManagedBeanConfig mbc = new ManagedBeanConfig();
            mbc.setName(bean.name());
            mbc.setType(clazz.getName());
            switch(bean.scope()) {
                case APPLICATION:
                    mbc.setScope("application");
                    break;
                case REQUEST:
                    mbc.setScope("request");
                    break;
                case SESSION:
                    mbc.setScope("session");
                    break;
                default:
                    break;
            }
            Field[] fields = fields(clazz);
            for (Field field : fields) {
                if (log().isTraceEnabled()) {
                    log().trace("  Scanning field '" + field.getName() + "'");
                }
                Property property = (Property) field.getAnnotation(Property.class);
                if (property != null) {
                    if (log().isDebugEnabled()) {
                        log().debug("  Field '" + field.getName() + "' has a @Property annotation");
                    }
                    ManagedPropertyConfig mpc = new ManagedPropertyConfig();
                    String name = property.name();
                    if ((name == null) || "".equals(name)) {
                        name = field.getName();
                    }
                    mpc.setName(name);
                    mpc.setType(field.getType().getName());
                    mpc.setValue(property.value());
                    mbc.addProperty(mpc);
                    continue;
                }
                Value value = (Value) field.getAnnotation(Value.class);
                if (value != null) {
                    if (log().isDebugEnabled()) {
                        log().debug("  Field '" + field.getName() + "' has a @Value annotation");
                    }
                    ManagedPropertyConfig mpc = new ManagedPropertyConfig();
                    mpc.setName(field.getName());
                    mpc.setType(field.getType().getName());
                    mpc.setValue(value.value());
                    mbc.addProperty(mpc);
                    continue;
                }
            }
            config.addManagedBean(mbc);
        }
    }

    /**
     * <p>
     * Return a list of the JAR archives defined under the <code>/WEB-INF/lib</code> directory of this web application that contain a
     * <code>META-INF/faces-config.xml</code> resource (even if that resource is empty). If there are no such JAR archives, a zero-length list will be
     * returned.
     * </p>
     * 
     * @param servletContext
     *            <code>ServletContext</code> instance for this application
     * 
     * @exception IOException
     *                if an input/output error occurs
     */
    private List<JarFile> webArchives(ServletContext servletContext) throws IOException {
        List<JarFile> list = new ArrayList<JarFile>();
        Set<Object> paths = servletContext.getResourcePaths(WEB_LIB_PREFIX);
        for (Object pathObject : paths) {
            String path = (String) pathObject;
            if (!path.endsWith(".jar")) {
                continue;
            }
            URL url = servletContext.getResource(path);
            String jarURLString = "jar:" + url.toString() + "!/";
            url = new URL(jarURLString);
            JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
            JarEntry signal = jarFile.getJarEntry(FACES_CONFIG_IMPLICIT);
            if (signal == null) {
                if (log().isTraceEnabled()) {
                    log().trace("Skip JAR file " + path + " because it has no META-INF/faces-config.xml resource");
                }
                continue;
            }
            list.add(jarFile);
        }
        return list;
    }

    /**
     * <p>
     * Return a list of the classes defined under the <code>/WEB-INF/classes</code> directory of this web application. If there are no such classes, a
     * zero-length list will be returned.
     * </p>
     * 
     * @param servletContext
     *            <code>ServletContext</code> instance for this application
     * 
     * @exception ClassNotFoundException
     *                if a located class cannot be loaded
     */
    private List<Class> webClasses(ServletContext servletContext) throws ClassNotFoundException {
        List<Class> list = new ArrayList<Class>();
        webClasses(servletContext, WEB_CLASSES_PREFIX, list);
        return list;
    }

    /**
     * <p>
     * Add classes found in the specified directory to the specified list, recursively calling this method when a directory is encountered.
     * </p>
     * 
     * @param servletContext
     *            <code>ServletContext</code> instance for this application
     * @param prefix
     *            Prefix specifying the "directory path" to be searched
     * @param list
     *            List to be appended to
     * 
     * @exception ClassNotFoundException
     *                if a located class cannot be loaded
     */
    private void webClasses(ServletContext servletContext, String prefix, List<Class> list) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }
        Set<Object> paths = servletContext.getResourcePaths(prefix);
        if (log().isTraceEnabled()) {
            log().trace("webClasses(" + prefix + ") - Received " + paths.size() + " paths to check");
        }
        String path = null;
        if (paths != null) {
            for (Object pathObject : paths) {
                path = (String) pathObject;
                if (path.endsWith("/")) {
                    webClasses(servletContext, path, list);
                } else if (path.endsWith(".class")) {
                    path = path.substring(WEB_CLASSES_PREFIX.length());
                    path = path.substring(0, path.length() - 6);
                    path = path.replace('/', '.');
                    Class clazz = null;
                    try {
                        clazz = loader.loadClass(path);
                    } catch (NoClassDefFoundError e) {
                        ;
                    } catch (Exception e) {
                        ;
                    }
                    if (clazz != null) {
                        list.add(clazz);
                    }
                }
            }
        }
    }
}
