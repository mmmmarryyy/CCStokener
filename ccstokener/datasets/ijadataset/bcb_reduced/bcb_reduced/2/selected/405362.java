package net.sourceforge.statelessfilter.filter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.statelessfilter.backend.ISessionBackend;
import net.sourceforge.statelessfilter.processor.IRequestProcessor;
import net.sourceforge.statelessfilter.spring.SpringContextChecker;
import net.sourceforge.statelessfilter.spring.SpringObjectInstantiationListener;
import net.sourceforge.statelessfilter.wrappers.BufferedHttpResponseWrapper;
import net.sourceforge.statelessfilter.wrappers.StatelessRequestWrapper;
import net.sourceforge.statelessfilter.wrappers.headers.HeaderBufferedHttpResponseWrapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The filter overrides the default session management and use multiple and
 * configurable backends instead.
 * 
 * @author Nicolas Richeton - Capgemini
 * @author Guillaume Mary - Capgemini
 * 
 */
public class StatelessFilter implements Filter {

    private static final String CONFIG_ATTRIBUTE_PREFIX = "attribute.";

    private static final String CONFIG_DEFAULT_BACKEND = "default";

    private static final String CONFIG_DIRTY = "dirtycheck";

    private static final String CONFIG_INSTANTIATION_LISTENER = "instantiationListener";

    private static final String CONFIG_LOCATION = "configurationLocation";

    private static final String CONFIG_LOCATION_DEFAULT = "/stateless.properties";

    /**
     * Location of backend configuration file.
     */
    private static final String CONFIG_PLUGIN_BACKEND = "stateless-backend.properties";

    private static final String CONFIG_PLUGIN_BACKEND_IMPL = "backendImpl";

    /**
     * Location of request processor configuration file.
     */
    private static final String CONFIG_PLUGIN_PROCESSOR = "stateless-processor.properties";

    private static final String CONFIG_PLUGIN_PROCESSOR_IMPL = "processorImpl";

    private static final String DEBUG_INIT = "Stateless filter init...";

    private static final String DEBUG_PROCESSING = "Processing ";

    private static final String DOT = ".";

    private static final String EXCLUDE_PATTERN_SEPARATOR = ",";

    private static final String INFO_BACKEND = "Backend ";

    private static final String INFO_BUFFERING = " enables output buffering.";

    private static final String INFO_DEFAULT_BACKEND = "Default Session backend is ";

    private static final String INFO_READY = " ready.";

    private static final String INFO_REQUEST_PROCESSOR = "Request processor ";

    private static Logger logger = LoggerFactory.getLogger(StatelessFilter.class);

    private static final String WARN_BACKEND_NOT_FOUND1 = "Specified backend '";

    private static final String WARN_BACKEND_NOT_FOUND2 = "' is not installed. Missing jar ? ";

    private static final String WARN_LOAD_CONF = "Cannot load global configuration /stateless.properties. Using defaults";

    private IObjectInstantiationListener instantiationListener = null;

    private List<Pattern> excludePatterns = null;

    private ServletContext servletContext = null;

    protected Configuration configuration = new Configuration();

    /**
     * Servlet config paramter name used to configure the list of the excluded
     * uri patterns.
     */
    public static final String PARAM_EXCLUDE_PATTERN_LIST = "excludePatternList";

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        for (ISessionBackend backend : configuration.backends.values()) {
            backend.destroy();
        }
        configuration.backends.clear();
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (isExcluded(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        StatelessRequestWrapper statelessRequest = new StatelessRequestWrapper((HttpServletRequest) request, configuration);
        HttpServletResponse targetResponse = httpResponse;
        BufferedHttpResponseWrapper bufferedResponse = null;
        if (Configuration.BUFFERING_FULL.equals(configuration.isBufferingRequired)) {
            bufferedResponse = new BufferedHttpResponseWrapper(httpResponse);
            targetResponse = bufferedResponse;
        } else if (Configuration.BUFFERING_HEADERS.equals(configuration.isBufferingRequired)) {
            targetResponse = new HeaderBufferedHttpResponseWrapper(statelessRequest, httpResponse);
        }
        if (configuration.requestProcessors != null && !configuration.requestProcessors.isEmpty()) {
            IRequestProcessor rp = null;
            for (int i = 0; i < configuration.requestProcessors.size(); i++) {
                rp = configuration.requestProcessors.get(i);
                try {
                    rp.preRequest(statelessRequest, targetResponse);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        chain.doFilter(statelessRequest, targetResponse);
        if (!statelessRequest.isSessionWritten()) {
            statelessRequest.writeSession(statelessRequest, httpResponse);
        }
        if (configuration.requestProcessors != null && !configuration.requestProcessors.isEmpty()) {
            IRequestProcessor rp = null;
            for (int i = configuration.requestProcessors.size() - 1; i >= 0; i--) {
                rp = configuration.requestProcessors.get(i);
                try {
                    rp.postProcess(statelessRequest, targetResponse);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        if (bufferedResponse != null) {
            if (!bufferedResponse.performSend()) {
                bufferedResponse.flushBuffer();
                response.getOutputStream().write(bufferedResponse.getBuffer());
                response.flushBuffer();
            }
        }
    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug(DEBUG_INIT);
        }
        this.servletContext = filterConfig.getServletContext();
        Properties globalProp = new Properties();
        String configLocation = filterConfig.getInitParameter(CONFIG_LOCATION);
        if (configLocation == null) {
            configLocation = CONFIG_LOCATION_DEFAULT;
        }
        try {
            globalProp.load(StatelessFilter.class.getResourceAsStream(configLocation));
        } catch (Exception e) {
            logger.warn(WARN_LOAD_CONF);
        }
        if (SpringContextChecker.checkForSpring(servletContext)) {
            logger.info("Enabling Spring instantiation listener");
            instantiationListener = new SpringObjectInstantiationListener();
            instantiationListener.setServletContext(servletContext);
        }
        try {
            initInstantiationListener(globalProp);
        } catch (Exception e) {
            throw new ServletException("Failed to load instantiation listener from /stateless.properties", e);
        }
        try {
            detectAndInitPlugins(CONFIG_PLUGIN_BACKEND, globalProp, IPlugin.TYPE_BACKEND);
            detectAndInitPlugins(CONFIG_PLUGIN_PROCESSOR, globalProp, IPlugin.TYPE_REQUEST_PROCESSOR);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        initAttributeMapping(globalProp);
        initDefaultBackend(globalProp);
        initDirtyState(globalProp);
        initBuffering(globalProp);
        initExcludedPattern(filterConfig);
        checkConfiguration();
    }

    private void initBuffering(Properties globalProp) {
        String buffering = globalProp.getProperty("buffering", Configuration.BUFFERING_FALSE);
        applyBuffering(buffering, "Configuration");
    }

    /**
     * Change buffering mode if current mode is lower than requested.
     * 
     * @param mode
     */
    private void applyBuffering(String mode, String source) {
        if (Configuration.BUFFERING_FULL.equals(mode) && !Configuration.BUFFERING_FULL.equals(configuration.isBufferingRequired)) {
            configuration.isBufferingRequired = Configuration.BUFFERING_FULL;
            if (logger.isInfoEnabled()) logger.info("Switching to buffering mode " + configuration.isBufferingRequired + " (" + source + ")");
        } else if (Configuration.BUFFERING_HEADERS.equals(mode) && Configuration.BUFFERING_FALSE.equals(configuration.isBufferingRequired)) {
            configuration.isBufferingRequired = Configuration.BUFFERING_HEADERS;
            if (logger.isInfoEnabled()) logger.info("Switching to buffering mode " + configuration.isBufferingRequired + " (" + source + ")");
        }
    }

    /**
     * Does some self tests to ensure configuration is valid
     * 
     * @throws ServletException
     */
    private void checkConfiguration() throws ServletException {
        if (this.configuration.backends.size() == 0) {
            throw new ServletException("No backend installed. Please add one (stateless-session for instance) in the classpath");
        }
    }

    private void initExcludedPattern(FilterConfig filterConfig) {
        String excludedPatternList = filterConfig.getInitParameter(PARAM_EXCLUDE_PATTERN_LIST);
        if (excludedPatternList != null) {
            String[] splittedExcludedPatternList = excludedPatternList.split(EXCLUDE_PATTERN_SEPARATOR);
            List<Pattern> patterns = new ArrayList<Pattern>();
            Pattern pattern = null;
            for (String element : splittedExcludedPatternList) {
                pattern = Pattern.compile(element);
                patterns.add(pattern);
            }
            this.excludePatterns = patterns;
        }
    }

    private void detectAndInitPlugins(String propertyFile, Properties filterConfiguration, String type) throws Exception {
        Enumeration<URL> configurationURLs;
        configurationURLs = StatelessFilter.class.getClassLoader().getResources(propertyFile);
        URL url = null;
        Properties pluginConfiguration = null;
        InputStream is = null;
        while (configurationURLs.hasMoreElements()) {
            url = configurationURLs.nextElement();
            if (logger.isDebugEnabled()) {
                logger.debug(DEBUG_PROCESSING + url.toString());
            }
            pluginConfiguration = new Properties();
            is = url.openStream();
            pluginConfiguration.load(is);
            is.close();
            initPlugin(pluginConfiguration, filterConfiguration, type);
        }
    }

    private void initAttributeMapping(Properties globalProp) throws ServletException {
        for (Object key : globalProp.keySet()) {
            String paramName = (String) key;
            if (paramName.startsWith(CONFIG_ATTRIBUTE_PREFIX)) {
                String attrName = paramName.substring(CONFIG_ATTRIBUTE_PREFIX.length());
                String backend = globalProp.getProperty(paramName);
                configuration.backendsAttributeMapping.put(attrName, backend);
                if (!configuration.backends.containsKey(backend)) {
                    throw new ServletException("Attributes are mapped on backend " + backend + " but it is not installed.");
                }
            }
        }
    }

    private void initDefaultBackend(Properties globalProp) {
        String defaultBack = globalProp.getProperty(CONFIG_DEFAULT_BACKEND);
        if (defaultBack != null) {
            if (configuration.backends.containsKey(defaultBack)) {
                configuration.defaultBackend = defaultBack;
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(WARN_BACKEND_NOT_FOUND1 + defaultBack + WARN_BACKEND_NOT_FOUND2);
                }
            }
        }
    }

    private void initDirtyState(Properties globalProp) {
        String useDirty = globalProp.getProperty(CONFIG_DIRTY);
        if (Boolean.parseBoolean(useDirty)) {
            configuration.useDirty = true;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Use dirty state: " + configuration.useDirty);
            logger.info(INFO_DEFAULT_BACKEND + configuration.defaultBackend);
        }
    }

    /**
     * Get and create instantiation listener from configuration file.
     * 
     * @param globalProp
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void initInstantiationListener(Properties globalProp) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String clazz = globalProp.getProperty(CONFIG_INSTANTIATION_LISTENER);
        if (!StringUtils.isEmpty(clazz)) {
            @SuppressWarnings("unchecked") Class<IObjectInstantiationListener> backClazz = (Class<IObjectInstantiationListener>) Class.forName(clazz);
            instantiationListener = backClazz.newInstance();
            instantiationListener.setServletContext(servletContext);
            logger.info("Using instantiation listener {}", clazz);
        }
    }

    /**
     * Does backend initialization.
     * 
     * @param backendProperties
     * @param globalProperties
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void initPlugin(Properties backendProperties, Properties globalProperties, String type) throws Exception {
        IPlugin plugin = null;
        String location = CONFIG_PLUGIN_BACKEND_IMPL;
        if (IPlugin.TYPE_REQUEST_PROCESSOR.equals(type)) {
            location = CONFIG_PLUGIN_PROCESSOR_IMPL;
        }
        if (instantiationListener != null) {
            plugin = (IPlugin) instantiationListener.getInstance((String) backendProperties.get(location));
        }
        if (plugin == null) {
            String clazz = (String) backendProperties.get(location);
            Class<IPlugin> backClazz = (Class<IPlugin>) Class.forName(clazz);
            plugin = backClazz.newInstance();
        }
        HashMap<String, String> conf = new HashMap<String, String>();
        String paramName = null;
        String prefix = null;
        String attrName = null;
        for (Object key : globalProperties.keySet()) {
            paramName = (String) key;
            prefix = plugin.getId() + DOT;
            if (paramName.startsWith(prefix)) {
                attrName = paramName.substring(prefix.length());
                conf.put(attrName, globalProperties.getProperty(paramName));
            }
        }
        plugin.init(conf);
        applyBuffering(plugin.isBufferingRequired(), plugin.getId());
        if (IPlugin.TYPE_REQUEST_PROCESSOR.equals(type)) {
            configuration.requestProcessors.add((IRequestProcessor) plugin);
            if (logger.isInfoEnabled()) {
                logger.info(INFO_REQUEST_PROCESSOR + plugin.getId() + INFO_READY);
            }
        }
        if (IPlugin.TYPE_BACKEND.equals(type)) {
            configuration.backends.put(plugin.getId(), (ISessionBackend) plugin);
            if (logger.isInfoEnabled()) {
                logger.info(INFO_BACKEND + plugin.getId() + INFO_READY);
            }
            if (StringUtils.isEmpty(configuration.defaultBackend)) {
                configuration.defaultBackend = plugin.getId();
            }
        }
    }

    /**
     * Check if this request is excluded from the process.
     * 
     * @param httpRequest
     *            HTTP request
     * @return true if the URI requested match an excluded pattern
     */
    private boolean isExcluded(HttpServletRequest httpRequest) {
        if (this.excludePatterns == null) {
            return false;
        }
        String uri = httpRequest.getRequestURI();
        if (logger.isDebugEnabled()) {
            logger.debug("Check URI : " + uri);
        }
        try {
            uri = new URI(uri).normalize().toString();
            for (Pattern pattern : this.excludePatterns) {
                if (pattern.matcher(uri).matches()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("URI excluded : " + uri);
                    }
                    return true;
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("The following URI has a bad syntax. The request will be processed by the filter. URI : " + uri, e);
        }
        return false;
    }
}
