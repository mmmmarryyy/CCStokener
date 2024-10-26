package org.jtell.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jtell.ConfigurationException;
import org.jtell.JTellException;
import org.jtell.config.*;
import org.jtell.internal.Constants;
import org.jtell.internal.Empties;
import org.jtell.internal.Guard;
import org.jtell.internal.config.DefaultConfiguration;
import org.jtell.internal.config.EventSinkMetadataImpl;
import org.jtell.internal.config.OrderMetadataImpl;
import org.jtell.internal.config.xml.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * <p>
 * <code>XmlConfigurationSource</code> is an implementation of the {@link ConfigurationSource} interface which reads
 * configuration information from one or more <code>jtell.xml</code> configuration files.
 * </p>
 * <p>
 * <strong>Thread Safety</strong><br/>
 * Instances of this class are safe for multithreaded access.
 * </p>
 */
public class XmlConfigurationSource implements ConfigurationSource {

    private static Log LOG = LogFactory.getLog(XmlConfigurationSource.class);

    /**
     * <p>
     * Enumeration of configuration file URLs.
     * </p>
     */
    private final List<URL> m_urls;

    /**
     * <p>
     * Construct a {@link XmlConfigurationSource} instance.
     * </p>
     */
    public XmlConfigurationSource() {
        super();
        m_urls = getDefaultUrls();
    }

    /**
     * <p>
     * Construct a {@link XmlConfigurationSource} instance.
     * </p>
     *
     * @param urls enumeration of configuration file URLs.
     */
    public XmlConfigurationSource(final List<URL> urls) {
        super();
        Guard.notNull("urls", urls);
        m_urls = urls;
    }

    public Configuration getConfiguration() throws ConfigurationException {
        final ConfigurationContentHandler contentHandler = new ConfigurationContentHandler();
        final XMLReader reader;
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(contentHandler);
            reader.setEntityResolver(new SchemaResourceEntityResolver());
            reader.setFeature("http://xml.org/sax/features/validation", true);
            reader.setFeature("http://apache.org/xml/features/validation/schema", true);
        } catch (SAXException e) {
            throw new JTellException("An error occurred while initializing the configuration file parser.", e);
        }
        final List<JTellElement> rootElements = new ArrayList<JTellElement>(m_urls.size());
        for (final URL url : m_urls) {
            try {
                final InputStream inputStream = url.openStream();
                try {
                    reader.parse(new InputSource(inputStream));
                    rootElements.add(contentHandler.getRootElement());
                } finally {
                    inputStream.close();
                }
            } catch (Exception e) {
                if (e instanceof JTellException) {
                    throw (JTellException) e;
                }
                throw new ConfigurationException(String.format("An error occurred while parsing the configuration file at [%s].", url), e);
            }
        }
        final Configuration result = createConfiguration(rootElements);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Returning [%s] for %d configuration file(s) (%s).", result, m_urls.size(), m_urls));
        }
        return result;
    }

    /**
     * <p>
     * Convert a collection of {@link JTellElement} instances into an appropriate {@link Configuration} instance.
     * </p>
     *
     * @param rootElements the configuration file root elements.
     * @return {@link Configuration} instance.
     * @throws ConfigurationException if an error is encountered in the configuration.
     */
    private Configuration createConfiguration(List<JTellElement> rootElements) throws ConfigurationException {
        final Map<String, Set<EventSinkMetadata>> eventSinkMetadata = new HashMap<String, Set<EventSinkMetadata>>();
        for (final JTellElement rootElement : rootElements) {
            for (final ListenerElement listenerElement : rootElement.getListeners()) {
                for (final HandlerElement handlerElement : listenerElement.getHandlers()) {
                    final Set<OrderElement> afterOrderElements = handlerElement.getAfterOrders();
                    final Set<OrderMetadata> afterOrders;
                    if (afterOrderElements.isEmpty()) {
                        afterOrders = Empties.EMPTY_ORDER_METADATAS;
                    } else {
                        afterOrders = new HashSet<OrderMetadata>(afterOrderElements.size());
                        for (final OrderElement orderElement : handlerElement.getAfterOrders()) {
                            afterOrders.add(new OrderMetadataImpl(orderElement.getToken(), OrderType.AFTER, orderElement.isRequired()));
                        }
                    }
                    final Set<OrderElement> beforeOrderElements = handlerElement.getBeforeOrders();
                    final Set<OrderMetadata> beforeOrders;
                    if (beforeOrderElements.isEmpty()) {
                        beforeOrders = Empties.EMPTY_ORDER_METADATAS;
                    } else {
                        beforeOrders = new HashSet<OrderMetadata>(beforeOrderElements.size());
                        for (final OrderElement orderElement : handlerElement.getBeforeOrders()) {
                            beforeOrders.add(new OrderMetadataImpl(orderElement.getToken(), OrderType.BEFORE, orderElement.isRequired()));
                        }
                    }
                    for (final EventElement eventElement : handlerElement.getEvents()) {
                        final String listenerClassName = listenerElement.getListenerClassName();
                        final Set<EventSinkMetadata> metadataSet;
                        if (eventSinkMetadata.containsKey(listenerClassName)) {
                            metadataSet = eventSinkMetadata.get(listenerClassName);
                        } else {
                            metadataSet = new HashSet<EventSinkMetadata>();
                            eventSinkMetadata.put(listenerClassName, metadataSet);
                        }
                        metadataSet.add(new EventSinkMetadataImpl(listenerElement.getListenerClassName(), handlerElement.getMethodSignature(), eventElement.getEventClassName(), eventElement.getSourceClassName(), eventElement.getAttributes(), afterOrders, beforeOrders, handlerElement.getContributeTokens()));
                    }
                }
            }
        }
        final Configuration result = new DefaultConfiguration(eventSinkMetadata);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Returning configuration object [%s].", result));
        }
        return result;
    }

    /**
     * <p>
     * Get default configuration URLs by searching for all resources on the classpath which match the default resource
     * name.
     * </p>
     *
     * @return {@link List} of {@link URL} instances.
     */
    private List<URL> getDefaultUrls() {
        final List<URL> urls;
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            urls = Collections.list(classLoader.getResources(Constants.JTELL_METADATA_RESOURCE));
        } catch (IOException e) {
            throw new IllegalStateException(String.format("An error occurred while creating the default listener metadata source: [%s]", e.getMessage()), e);
        }
        return urls;
    }
}
