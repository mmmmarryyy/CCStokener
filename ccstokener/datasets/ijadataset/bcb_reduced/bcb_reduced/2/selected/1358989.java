package net.sourceforge.javautil.web.server.descriptor.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.common.io.impl.SystemFile;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;
import net.sourceforge.javautil.common.xml.XMLDocument;
import net.sourceforge.javautil.common.xml.XMLDocumentSerializer;
import net.sourceforge.javautil.common.xml.annotation.XmlCollection;
import net.sourceforge.javautil.common.xml.annotation.XmlMap;
import net.sourceforge.javautil.common.xml.annotation.XmlTag;
import net.sourceforge.javautil.common.xml.annotation.XmlCollection.CollectionType;
import net.sourceforge.javautil.common.xml.annotation.XmlTag.ElementType;
import net.sourceforge.javautil.web.server.application.WebApplicationDefaultMimeMappings;
import net.sourceforge.javautil.web.server.application.servlet.DefaultServlet;
import net.sourceforge.javautil.web.server.descriptor.IWebXml;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlEnvironmentEntry;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlEnvironmentReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlErrorPage;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlFilter;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlFilterMapping;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlIcons;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlLifecycleCallback;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlListener;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlLocalEJBReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlLocaleEncoding;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlLoginConfig;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlMessageDestination;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlMessageDestinationReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlPersistenceContextReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlPersistenceUnitReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlRemoteEJBReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlResourceReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlSecurityConstraint;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlSecurityRole;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlServiceReference;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlServlet;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlServletMapping;
import net.sourceforge.javautil.web.server.descriptor.IWebXmlSessionConfig;

/**
 * The base of a web xml descriptor.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: WebXml.java 2298 2010-06-16 00:20:18Z ponderator $
 */
@XmlTag(name = "web-app", namespace = "http://java.sun.com/xml/ns/javaee")
public class WebXml implements IWebXml {

    public static WebXml createDefaultServerWebXml() {
        WebXml webXml = new WebXml();
        WebXmlServlet servlet = new WebXmlServlet();
        servlet.setServletName("DefaultServlet");
        servlet.setServletClass(DefaultServlet.class.getName());
        webXml.getServlets().add(servlet);
        WebXmlServletMapping mapping = new WebXmlServletMapping();
        mapping.setServletName("DefaultServlet");
        mapping.getPatterns().add("/");
        webXml.getServletMappings().add(mapping);
        webXml.getMimeMappings().putAll(WebApplicationDefaultMimeMappings.getMappings());
        return webXml;
    }

    public static WebXml getInstance(URL url) {
        try {
            return XMLDocument.read(url.openStream(), WebXml.class);
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * The version of the web application descriptor
	 */
    private String version;

    /**
	 * The icons for the application
	 */
    private IWebXmlIcons icons;

    /**
	 * The display name for the application
	 */
    private String displayName;

    /**
	 * The description of this application
	 */
    private String description;

    /**
	 * The context initialization parameters
	 */
    private Map<String, String> initParameters = new LinkedHashMap<String, String>();

    /**
	 * The filters defined for the web application
	 */
    private List<IWebXmlFilter> filters = new ArrayList<IWebXmlFilter>();

    /**
	 * The filter mappings defined for each of the {@link #filters}
	 */
    private List<IWebXmlFilterMapping> filterMappings = new ArrayList<IWebXmlFilterMapping>();

    /**
	 * The list of listeners
	 */
    private List<IWebXmlListener> listeners = new ArrayList<IWebXmlListener>();

    /**
	 * The list of servlets
	 */
    private List<IWebXmlServlet> servlets = new ArrayList<IWebXmlServlet>();

    /**
	 * The serlvet mappings defined for each of the {@link #servlets}
	 */
    private List<IWebXmlServletMapping> servletMappings = new ArrayList<IWebXmlServletMapping>();

    /**
	 * The session config for the application
	 */
    private IWebXmlSessionConfig sessionConfig;

    /**
	 * The mime mappings for the application
	 */
    private Map<String, String> mimeMappings = new LinkedHashMap<String, String>();

    /**
	 * Welcome files for the application
	 */
    private List<String> welcomeFiles = new ArrayList<String>();

    /**
	 * Error page definitions
	 */
    private List<IWebXmlErrorPage> errorPages = new ArrayList<IWebXmlErrorPage>();

    /**
	 * Resource references
	 */
    private List<IWebXmlResourceReference> resourceRefs = new ArrayList<IWebXmlResourceReference>();

    /**
	 * Environment resource references
	 */
    private List<IWebXmlEnvironmentReference> environmentRefs = new ArrayList<IWebXmlEnvironmentReference>();

    /**
	 * Environment entries
	 */
    private List<IWebXmlEnvironmentEntry> environmentEntries = new ArrayList<IWebXmlEnvironmentEntry>();

    /**
	 * Web service references
	 */
    private List<IWebXmlServiceReference> serviceRefs = new ArrayList<IWebXmlServiceReference>();

    /**
	 * Local EJB references
	 */
    private List<IWebXmlLocalEJBReference> localEjbRefs = new ArrayList<IWebXmlLocalEJBReference>();

    /**
	 * Remote EJB references
	 */
    private List<IWebXmlRemoteEJBReference> remoteEjbRefs = new ArrayList<IWebXmlRemoteEJBReference>();

    /**
	 * Locale to encoding mappings
	 */
    private List<IWebXmlLocaleEncoding> localeEncodings = new ArrayList<IWebXmlLocaleEncoding>();

    /**
	 * Login configuration
	 */
    private IWebXmlLoginConfig loginConfig;

    /**
	 * Security roles for the application
	 */
    private List<IWebXmlSecurityRole> securityRoles = new ArrayList<IWebXmlSecurityRole>();

    /**
	 * The constraints on access to pages in the application
	 */
    private List<IWebXmlSecurityConstraint> securityConstraints = new ArrayList<IWebXmlSecurityConstraint>();

    /**
	 * Message destinations
	 */
    private List<IWebXmlMessageDestination> messageDestinations = new ArrayList<IWebXmlMessageDestination>();

    /**
	 * Message destination references
	 */
    private List<IWebXmlMessageDestinationReference> messageDestinationRefs = new ArrayList<IWebXmlMessageDestinationReference>();

    /**
	 * JPA persistence context references
	 */
    private List<IWebXmlPersistenceContextReference> persistenceContextRefs = new ArrayList<IWebXmlPersistenceContextReference>();

    /**
	 * JPA persistence unit references
	 */
    private List<IWebXmlPersistenceUnitReference> persistenceUnitRefs = new ArrayList<IWebXmlPersistenceUnitReference>();

    /**
	 * Post constructor call backs
	 */
    private List<IWebXmlLifecycleCallback> postConstructors = new ArrayList<IWebXmlLifecycleCallback>();

    /**
	 * Pre destroy call backs
	 */
    private List<IWebXmlLifecycleCallback> preDestroyers = new ArrayList<IWebXmlLifecycleCallback>();

    /**
	 * @return The {@link #version}
	 */
    @XmlTag(elementType = ElementType.Attribute)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
	 * @return The {@link #icons}
	 */
    @XmlTag(concreteType = WebXmlIcons.class)
    public IWebXmlIcons getIcons() {
        return icons;
    }

    public void setIcons(IWebXmlIcons icons) {
        this.icons = icons;
    }

    /**
	 * @return The {@link #description}
	 */
    @XmlTag(after = "displayName")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
	 * @return {@link #listeners}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlListener.class), after = "sessionConfig")
    public List<IWebXmlListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<IWebXmlListener> listeners) {
        this.listeners = listeners;
    }

    /**
	 * @return The {@link #displayName}
	 */
    @XmlTag(name = "display-name", after = "icons")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
	 * @return The {@link #initParameters}
	 */
    @XmlTag(name = "context-param", after = "description", map = @XmlMap(keyName = "param-name", valueName = "param-value", valueClass = String.class))
    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    public void setInitParameters(Map<String, String> initParameters) {
        this.initParameters = initParameters;
    }

    /**
	 * @return The {@link #filters}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlFilter.class), after = "listeners")
    public List<IWebXmlFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<IWebXmlFilter> filters) {
        this.filters = filters;
    }

    /**
	 * @return The {@link #filterMappings}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlFilterMapping.class), after = "filters")
    public List<IWebXmlFilterMapping> getFilterMappings() {
        return filterMappings;
    }

    public void setFilterMappings(List<IWebXmlFilterMapping> filterMappings) {
        this.filterMappings = filterMappings;
    }

    /**
	 * @return The {@link #servlets}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlServlet.class), after = "filterMappings")
    public List<IWebXmlServlet> getServlets() {
        return servlets;
    }

    public void setServlets(List<IWebXmlServlet> servlets) {
        this.servlets = servlets;
    }

    /**
	 * @return The {@link #servletMappings}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlServletMapping.class), after = "servlets")
    public List<IWebXmlServletMapping> getServletMappings() {
        return servletMappings;
    }

    public void setServletMappings(List<IWebXmlServletMapping> servletMappings) {
        this.servletMappings = servletMappings;
    }

    /**
	 * @return The {@link #sessionConfig}
	 */
    @XmlTag(concreteType = WebXmlSessionConfig.class, after = "mimeMappings")
    public IWebXmlSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(IWebXmlSessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    /**
	 * @return The {@link #mimeMappings}
	 */
    @XmlTag(name = "mime-mapping", map = @XmlMap(keyName = "extension", valueName = "mime-type", valueClass = String.class), after = "initParameters")
    public Map<String, String> getMimeMappings() {
        return mimeMappings;
    }

    public void setMimeMappings(Map<String, String> mimeMappings) {
        this.mimeMappings = mimeMappings;
    }

    /**
	 * @return The {@link #welcomeFiles}
	 */
    @XmlTag(name = "welcome-file-list", collection = @XmlCollection(value = String.class, type = CollectionType.WRAPPED), after = "servletMappings")
    public List<String> getWelcomeFiles() {
        return welcomeFiles;
    }

    public void setWelcomeFiles(List<String> welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
    }

    /**
	 * @return The {@link #errorPages}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlErrorPage.class), after = "welcomeFiles")
    public List<IWebXmlErrorPage> getErrorPages() {
        return errorPages;
    }

    public void setErrorPages(List<IWebXmlErrorPage> errorPages) {
        this.errorPages = errorPages;
    }

    /**
	 * @return The {@link #resourceRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlResourceReference.class), after = "errorPages")
    public List<IWebXmlResourceReference> getResourceRefs() {
        return resourceRefs;
    }

    public void setResourceRefs(List<IWebXmlResourceReference> resourceRefs) {
        this.resourceRefs = resourceRefs;
    }

    /**
	 * @return The {@link #environmentRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlEnvironmentReference.class), after = "resourceRefs")
    public List<IWebXmlEnvironmentReference> getEnvironmentRefs() {
        return environmentRefs;
    }

    public void setEnvironmentRefs(List<IWebXmlEnvironmentReference> environmentRefs) {
        this.environmentRefs = environmentRefs;
    }

    /**
	 * @return The {@link #serviceRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlServiceReference.class), after = "environmentRefs")
    public List<IWebXmlServiceReference> getServiceRefs() {
        return serviceRefs;
    }

    public void setServiceRefs(List<IWebXmlServiceReference> serviceRefs) {
        this.serviceRefs = serviceRefs;
    }

    /**
	 * @return The {@link #localEjbRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlLocalEJBReference.class), after = "serviceRefs")
    public List<IWebXmlLocalEJBReference> getLocalEjbRefs() {
        return localEjbRefs;
    }

    public void setLocalEjbRefs(List<IWebXmlLocalEJBReference> localEjbRefs) {
        this.localEjbRefs = localEjbRefs;
    }

    /**
	 * @return The {@link #remoteEjbRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlRemoteEJBReference.class), after = "localEjbRefs")
    public List<IWebXmlRemoteEJBReference> getRemoteEjbRefs() {
        return remoteEjbRefs;
    }

    public void setRemoteEjbRefs(List<IWebXmlRemoteEJBReference> remoteEjbRefs) {
        this.remoteEjbRefs = remoteEjbRefs;
    }

    /**
	 * @return The {@link #localeEncodings}
	 */
    @XmlTag(name = "locale-encoding-mapping-list", collection = @XmlCollection(value = WebXmlLocaleEncoding.class, type = CollectionType.WRAPPED), after = "remoteEjbRefs")
    public List<IWebXmlLocaleEncoding> getLocaleEncodings() {
        return localeEncodings;
    }

    public void setLocaleEncodings(List<IWebXmlLocaleEncoding> localeEncodings) {
        this.localeEncodings = localeEncodings;
    }

    /**
	 * @return The {@link #loginConfig}
	 */
    @XmlTag(concreteType = WebXmlLoginConfig.class, after = "localEncodings")
    public IWebXmlLoginConfig getLoginConfig() {
        return loginConfig;
    }

    public void setLoginConfig(IWebXmlLoginConfig loginConfig) {
        this.loginConfig = loginConfig;
    }

    /**
	 * @return The {@link #securityRoles}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlSecurityRole.class), after = "loginConfig")
    public List<IWebXmlSecurityRole> getSecurityRoles() {
        return securityRoles;
    }

    public void setSecurityRoles(List<IWebXmlSecurityRole> securityRoles) {
        this.securityRoles = securityRoles;
    }

    /**
	 * @return The {@link #securityConstraints}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlSecurityConstraint.class), after = "securityRoles")
    public List<IWebXmlSecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    public void setSecurityConstraints(List<IWebXmlSecurityConstraint> securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    /**
	 * @return The {@link #messageDestinations}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlMessageDestination.class), after = "securityConstraints")
    public List<IWebXmlMessageDestination> getMessageDestinations() {
        return messageDestinations;
    }

    public void setMessageDestinations(List<IWebXmlMessageDestination> messageDestinations) {
        this.messageDestinations = messageDestinations;
    }

    /**
	 * @return The {@link #messageDestinationRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlMessageDestinationReference.class), after = "messageDestinations")
    public List<IWebXmlMessageDestinationReference> getMessageDestinationRefs() {
        return messageDestinationRefs;
    }

    public void setMessageDestinationRefs(List<IWebXmlMessageDestinationReference> messagDeestinationRefs) {
        this.messageDestinationRefs = messagDeestinationRefs;
    }

    /**
	 * @return The {@link #persistenceContextRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlPersistenceContextReference.class), after = "messageDestinationRefs")
    public List<IWebXmlPersistenceContextReference> getPersistenceContextRefs() {
        return persistenceContextRefs;
    }

    public void setPersistenceContextRefs(List<IWebXmlPersistenceContextReference> persistenceContextRefs) {
        this.persistenceContextRefs = persistenceContextRefs;
    }

    /**
	 * @return The {@link #persistenceUnitRefs}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlPersistenceUnitReference.class), after = "persistenceContextRefs")
    public List<IWebXmlPersistenceUnitReference> getPersistenceUnitRefs() {
        return persistenceUnitRefs;
    }

    public void setPersistenceUnitRefs(List<IWebXmlPersistenceUnitReference> persistenceUnitRefs) {
        this.persistenceUnitRefs = persistenceUnitRefs;
    }

    /**
	 * @return The {@link #postConstructors}
	 */
    @XmlTag(name = "post-construct", collection = @XmlCollection(WebXmlLifecycleCallback.class), after = "persistenceUnitRefs")
    public List<IWebXmlLifecycleCallback> getPostConstructors() {
        return postConstructors;
    }

    public void setPostConstructors(List<IWebXmlLifecycleCallback> postConstructors) {
        this.postConstructors = postConstructors;
    }

    /**
	 * @return The {@link #preDestroyers}
	 */
    @XmlTag(name = "pre-destroy", collection = @XmlCollection(WebXmlLifecycleCallback.class), after = "postConstructors")
    public List<IWebXmlLifecycleCallback> getPreDestroyers() {
        return preDestroyers;
    }

    public void setPreDestroyers(List<IWebXmlLifecycleCallback> preDestroyers) {
        this.preDestroyers = preDestroyers;
    }

    /**
	 * @return The {@link #environmentEntries}
	 */
    @XmlTag(collection = @XmlCollection(WebXmlEnvironmentEntry.class), after = "preDestroyers")
    public List<IWebXmlEnvironmentEntry> getEnvironmentEntries() {
        return environmentEntries;
    }

    public void setEnvironmentEntries(List<IWebXmlEnvironmentEntry> environmentEntries) {
        this.environmentEntries = environmentEntries;
    }

    /**
	 * Set an init paramter
	 * 
	 * @param name The name of the parameter
	 * @param value The value of the parameter
	 * 
	 * @return This for chaining
	 */
    public IWebXml setParameter(String name, String value) {
        this.initParameters.put(name, value);
        return this;
    }

    /**
	 * Add a listener to the web xml
	 * 
	 * @param listenerClass The name of the listener class
	 * 
	 * @return This for chaining
	 */
    public IWebXml addListener(String listenerClass) {
        this.listeners.add(new WebXmlListener(listenerClass));
        return this;
    }

    /**
	 * @param name The name of the servlet
	 * @param servletClass The servlet class name
	 * @param patterns The url patterns for the servlet
	 * @return The servlet definition that was created
	 */
    public IWebXmlServlet addServlet(String name, String servletClass, String... patterns) {
        WebXmlServlet servlet = new WebXmlServlet(name, servletClass);
        this.servlets.add(servlet);
        if (patterns.length > 0) this.servletMappings.add(new WebXmlServletMapping(name, patterns));
        return servlet;
    }

    /**
	 * @param name The name of the filter
	 * @param filterClass The filter class
	 * @param patterns The patterns for url matching
	 * @return The filter definition that was created
	 */
    public IWebXmlFilter addURLFilter(String name, String filterClass, String... patterns) {
        WebXmlFilter filter = new WebXmlFilter(name, filterClass);
        this.filters.add(filter);
        if (patterns.length > 0) this.filterMappings.add(new WebXmlFilterMapping(name, patterns));
        return filter;
    }

    /**
	 * @param name The name of the filter 
	 * @param filterClass The filter class
	 * @param servletNames The servlet names that this filter is for
	 * @return The filter definition that was created
	 */
    public IWebXmlFilter addServletFilter(String name, String filterClass, String... servletNames) {
        WebXmlFilter filter = new WebXmlFilter(name, filterClass);
        this.filters.add(filter);
        if (servletNames.length > 0) this.filterMappings.add(new WebXmlFilterMapping(name).addServlets(servletNames));
        return filter;
    }

    public IWebXmlServlet findServletByClassName(String className) {
        for (IWebXmlServlet servlet : this.servlets) {
            if (servlet.getServletClass().equals(className)) return servlet;
        }
        return null;
    }

    public List<IWebXmlServletMapping> getServletMappings(String servletName) {
        List<IWebXmlServletMapping> mappings = new ArrayList<IWebXmlServletMapping>();
        for (IWebXmlServletMapping mapping : this.servletMappings) {
            if (mapping.getServletName().equals(servletName)) mappings.add(mapping);
        }
        return mappings;
    }

    public IWebXmlFilter findFilterByClassName(String className) {
        for (IWebXmlFilter filter : this.filters) {
            if (filter.getFilterClass().equals(className)) return filter;
        }
        return null;
    }

    public List<IWebXmlFilterMapping> getFilterMappings(String filterName) {
        List<IWebXmlFilterMapping> mappings = new ArrayList<IWebXmlFilterMapping>();
        for (IWebXmlFilterMapping mapping : this.filterMappings) {
            if (mapping.getFilterName().equals(filterName)) mappings.add(mapping);
        }
        return mappings;
    }

    public String toXML() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new XMLDocumentSerializer().serialize(XMLDocument.getInstance(this), baos);
        return new String(baos.toByteArray());
    }

    /**
	 * Save the serialized representation of this web.xml
	 * to the specified file.
	 * 
	 * @param file The file to save to
	 */
    public void save(IVirtualFile file) {
        try {
            this.write(file.getOutputStream());
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * Write/serialize the XML contents to an output stream.
	 * 
	 * @param out The output stream to write to
	 */
    public void write(OutputStream out) {
        new XMLDocumentSerializer().serialize(XMLDocument.getInstance(this), out);
    }
}
