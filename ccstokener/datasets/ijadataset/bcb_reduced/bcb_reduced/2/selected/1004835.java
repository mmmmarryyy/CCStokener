package com.sun.faces.config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter;
import com.sun.faces.config.configprovider.ConfigurationResourceProvider;
import com.sun.faces.config.configprovider.MetaInfResourceProvider;
import com.sun.faces.config.configprovider.RIConfigResourceProvider;
import com.sun.faces.config.configprovider.WebResourceProvider;
import com.sun.faces.config.processor.ApplicationConfigProcessor;
import com.sun.faces.config.processor.ComponentConfigProcessor;
import com.sun.faces.config.processor.ConfigProcessor;
import com.sun.faces.config.processor.ConverterConfigProcessor;
import com.sun.faces.config.processor.FactoryConfigProcessor;
import com.sun.faces.config.processor.LifecycleConfigProcessor;
import com.sun.faces.config.processor.ManagedBeanConfigProcessor;
import com.sun.faces.config.processor.NavigationConfigProcessor;
import com.sun.faces.config.processor.RenderKitConfigProcessor;
import com.sun.faces.config.processor.ValidatorConfigProcessor;
import com.sun.faces.util.FacesLogger;
import com.sun.faces.util.Timer;

/**
 * <p>
 *  This class manages the initialization of each web application that uses
 *  JSF.
 * </p>
 */
public class ConfigManager {

    private static final Logger LOGGER = FacesLogger.CONFIG.getLogger();

    /**
     * <p>
     *  The list of resource providers.  By default, this contains a provider
     *  for the RI, and two providers to satisfy the requirements of the
     *  specification.
     * </p>
     */
    private static final List<ConfigurationResourceProvider> RESOURCE_PROVIDERS;

    /**
     * <p>
     *  There is only once instance of <code>ConfigManager</code>.
     * <p>
     */
    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    /**
     * <p>
     *   Contains each <code>ServletContext</code> that we've initialized.
     *   The <code>ServletContext</code> will be removed when the application
     *   is destroyed.
     * </p>
     */
    @SuppressWarnings({ "CollectionWithoutInitialCapacity" })
    private List<ServletContext> initializedContexts = new CopyOnWriteArrayList<ServletContext>();

    /**
     * <p>
     *  The chain of {@link ConfigProcessor}, used to initialize JSF.
     * </p>
     */
    private static final ConfigProcessor CONFIG_PROCESSOR_CHAIN;

    private static final String XSL = "/com/sun/faces/jsf1_0-1_1toSchema.xsl";

    static {
        List<ConfigurationResourceProvider> l = new ArrayList<ConfigurationResourceProvider>(3);
        l.add(new RIConfigResourceProvider());
        l.add(new MetaInfResourceProvider());
        l.add(new WebResourceProvider());
        RESOURCE_PROVIDERS = Collections.unmodifiableList(l);
        ConfigProcessor[] configProcessors = { new FactoryConfigProcessor(), new LifecycleConfigProcessor(), new ApplicationConfigProcessor(), new ComponentConfigProcessor(), new ConverterConfigProcessor(), new ValidatorConfigProcessor(), new ManagedBeanConfigProcessor(), new RenderKitConfigProcessor(), new NavigationConfigProcessor() };
        for (int i = 0; i < configProcessors.length; i++) {
            ConfigProcessor p = configProcessors[i];
            if ((i + 1) < configProcessors.length) {
                p.setNext(configProcessors[i + 1]);
            }
        }
        CONFIG_PROCESSOR_CHAIN = configProcessors[0];
    }

    /**
     * @return a <code>ConfigManager</code> instance
     */
    public static ConfigManager getInstance() {
        return CONFIG_MANAGER;
    }

    /**
     * <p>
     *   This method bootstraps JSF based on the parsed configuration resources.
     * </p>
     *
     * @param sc the <code>ServletContext</code> for the application that
     *  requires initialization
     */
    public void initialize(ServletContext sc) {
        if (!hasBeenInitialized(sc)) {
            initializedContexts.add(sc);
            try {
                CONFIG_PROCESSOR_CHAIN.process(getConfigDocuments(sc));
            } catch (Exception e) {
                releaseFactories();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Unsanitized stacktrace from failed start...", e);
                }
                Throwable t = unwind(e);
                throw new ConfigurationException("CONFIGURATION FAILED! " + t.getMessage(), t);
            }
        }
    }

    /**
     * <p>
     *   This method will remove any information about the application.
     * </p>
     * @param sc the <code>ServletContext</code> for the application that
     *  needs to be removed
     */
    public void destory(ServletContext sc) {
        releaseFactories();
        initializedContexts.remove(sc);
    }

    /**
     * @param sc the <code>ServletContext</code> for the application in question
     * @return <code>true</code> if this application has already been initialized,
     *  otherwise returns </code>fase</code>
     */
    public boolean hasBeenInitialized(ServletContext sc) {
        return (initializedContexts.contains(sc));
    }

    /**
     * <p>
     *   Obtains an array of <code>Document</code>s to be processed
     *   by {@link ConfigManager#CONFIG_PROCESSOR_CHAIN}.
     * </p>
     *
     * @param sc the <code>ServletContext</code> for the application to be
     *  processed
     * @return an array of <code>Document</code>s
     */
    private static Document[] getConfigDocuments(ServletContext sc) {
        List<Collection<URL>> urlCollections = new ArrayList<Collection<URL>>(RESOURCE_PROVIDERS.size());
        for (ConfigurationResourceProvider p : RESOURCE_PROVIDERS) {
            try {
                urlCollections.add(new URLTask(p, sc).call());
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }
        List<Document> docs = new ArrayList<Document>(RESOURCE_PROVIDERS.size() << 1);
        boolean validating = WebConfiguration.getInstance(sc).isOptionEnabled(BooleanWebContextInitParameter.ValidateFacesConfigFiles);
        for (Collection<URL> t : urlCollections) {
            try {
                for (URL u : t) {
                    Document d = new ParseTask(validating, u).call();
                    docs.add(d);
                }
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }
        return docs.toArray(new Document[docs.size()]);
    }

    /**
     * @param throwable Throwable
     * @return the root cause of this error
     */
    private Throwable unwind(Throwable throwable) {
        Throwable t = null;
        if (throwable != null) {
            t = unwind(throwable.getCause());
            if (t == null) {
                t = throwable;
            }
        }
        return t;
    }

    /**
     * Calls through to {@link javax.faces.FactoryFinder#releaseFactories()}
     * ignoring any exceptions.
     */
    private void releaseFactories() {
        try {
            FactoryFinder.releaseFactories();
        } catch (FacesException ignored) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Exception thrown from FactoryFinder.releaseFactories()", ignored);
            }
        }
    }

    /**
     * <p>
     *  This <code>Callable</code> will be used by {@link ConfigManager#getConfigDocuments(javax.servlet.ServletContext)}.
     *  It represents a single configuration resource to be parsed into a DOM.
     * </p>
     */
    private static class ParseTask {

        private static final String FACES_SCHEMA_DEFAULT_NS = "http://java.sun.com/xml/ns/javaee";

        private URL documentURL;

        private DocumentBuilderFactory factory;

        private boolean validating;

        /**
         * <p>
         *   Constructs a new ParseTask instance
         * </p>
         * @param validating whether or not we're validating
         * @param documentURL a URL to the configuration resource to be parsed
         * @throws Exception general error
         */
        public ParseTask(boolean validating, URL documentURL) throws Exception {
            this.documentURL = documentURL;
            this.factory = DbfFactory.getFactory();
            this.validating = validating;
        }

        /**
         * @return the result of the parse operation (a DOM)
         * @throws Exception if an error occurs during the parsing process
         */
        public Document call() throws Exception {
            try {
                Timer timer = Timer.getInstance();
                if (timer != null) {
                    timer.startTiming();
                }
                Document d = getDocument();
                if (timer != null) {
                    timer.stopTiming();
                    timer.logResult("Parse " + documentURL.toExternalForm());
                }
                return d;
            } catch (Exception e) {
                throw new ConfigurationException(MessageFormat.format("Unable to parse document ''{0}'': {1}", documentURL.toExternalForm(), e.getMessage()), e);
            }
        }

        /**
         * @return <code>Document</code> based on <code>documentURL</code>.
         * @throws Exception if an error occurs during the process of building a
         *  <code>Document</code>
         */
        private Document getDocument() throws Exception {
            if (validating) {
                DocumentBuilder db = getNonValidatingBuilder();
                DOMSource domSource = new DOMSource(db.parse(getInputStream(documentURL), documentURL.toExternalForm()));
                if (FACES_SCHEMA_DEFAULT_NS.equals(((Document) domSource.getNode()).getDocumentElement().getNamespaceURI())) {
                    DocumentBuilder builder = getBuilderForSchema(DbfFactory.FacesSchema.FACES_12);
                    builder.getSchema().newValidator().validate(domSource);
                    return ((Document) domSource.getNode());
                } else {
                    DOMResult domResult = new DOMResult();
                    Transformer transformer = getTransformer();
                    transformer.transform(domSource, domResult);
                    DocumentBuilder builder = getBuilderForSchema(DbfFactory.FacesSchema.FACES_11);
                    builder.getSchema().newValidator().validate(new DOMSource(domResult.getNode()));
                    return (Document) domResult.getNode();
                }
            } else {
                DocumentBuilder builder = getNonValidatingBuilder();
                InputSource is = new InputSource(getInputStream(documentURL));
                is.setSystemId(documentURL.toExternalForm());
                return builder.parse(is);
            }
        }

        /**
         * Obtain a <code>Transformer</code> using the style sheet
         * referenced by the <code>XSL</code> constant.
         *
         * @return a new Tranformer instance
         * @throws Exception if a Tranformer instance could not be created
         */
        private static Transformer getTransformer() throws Exception {
            TransformerFactory factory = TransformerFactory.newInstance();
            return factory.newTransformer(new StreamSource(getInputStream(ConfigManager.class.getResource(XSL))));
        }

        /**
         * @return an <code>InputStream</code> to the resource referred to by
         *         <code>url</code>
         * @param url source <code>URL</code>
         * @throws IOException if an error occurs
         */
        private static InputStream getInputStream(URL url) throws IOException {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            return new BufferedInputStream(conn.getInputStream());
        }

        private DocumentBuilder getNonValidatingBuilder() throws Exception {
            DocumentBuilderFactory tFactory = DbfFactory.getFactory();
            tFactory.setValidating(false);
            DocumentBuilder tBuilder = tFactory.newDocumentBuilder();
            tBuilder.setEntityResolver(DbfFactory.FACES_ENTITY_RESOLVER);
            tBuilder.setErrorHandler(DbfFactory.FACES_ERROR_HANDLER);
            return tBuilder;
        }

        private DocumentBuilder getBuilderForSchema(DbfFactory.FacesSchema schema) throws Exception {
            factory.setSchema(schema.getSchema());
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(DbfFactory.FACES_ENTITY_RESOLVER);
            builder.setErrorHandler(DbfFactory.FACES_ERROR_HANDLER);
            return builder;
        }
    }

    /**
     * <p>
     *  This <code>Callable</code> will be used by {@link ConfigManager#getConfigDocuments(javax.servlet.ServletContext)}.
     *  It represents one or more URLs to configuration resources that require
     *  processing.
     * </p>
     */
    private static class URLTask {

        private ConfigurationResourceProvider provider;

        private ServletContext sc;

        /**
         * Constructs a new <code>URLTask</code> instance.
         * @param provider the <code>ConfigurationResourceProvider</code> from
         *  which zero or more <code>URL</code>s will be returned
         * @param sc the <code>ServletContext</code> of the current application
         */
        public URLTask(ConfigurationResourceProvider provider, ServletContext sc) {
            this.provider = provider;
            this.sc = sc;
        }

        /**
         * @return zero or more <code>URL</code> instances
         * @throws Exception if an Exception is thrown by the underlying
         *  <code>ConfigurationResourceProvider</code> 
         */
        public Collection<URL> call() throws Exception {
            return provider.getResources(sc);
        }
    }
}
