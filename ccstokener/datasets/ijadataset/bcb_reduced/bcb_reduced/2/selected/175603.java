package org.datanucleus.metadata.xml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.datanucleus.ClassConstants;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.InvalidMetaDataException;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.EntityResolverFactory;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to provide the parsing framework for parsing metadata files.
 * This will support parsing of any metadata files where the resultant object is
 * derived from org.datanucleus.metadata.MetaData, so can be used on JDO files, ORM files,
 * JDOQUERY files, JPA files, or "persistence.xml" files. Can be used for any future
 * metadata files too.
 * <P>
 * Provides 3 different entry points depending on whether the caller has a URL,
 * a file, or an InputStream.
 * </P>
 */
public class MetaDataParser extends DefaultHandler {

    /** Localiser for messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** MetaData manager. */
    protected final MetaDataManager mgr;

    /** Plugin Manager. */
    protected final PluginManager pluginMgr;

    /** Whether to validate while parsing. */
    protected final boolean validate;

    /** SAXParser being used. */
    SAXParser parser = null;

    /**
     * Constructor.
     * @param mgr MetaDataManager
     * @param pluginMgr Manager for plugins
     * @param validate Whether to validate while parsing
     */
    public MetaDataParser(MetaDataManager mgr, PluginManager pluginMgr, boolean validate) {
        this.mgr = mgr;
        this.pluginMgr = pluginMgr;
        this.validate = validate;
    }

    /**
     * Method to parse a MetaData file given the URL of the file.
     * @param url Url of the metadata file
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException thrown if error occurred
     */
    public MetaData parseMetaDataURL(URL url, String handlerName) {
        if (url == null) {
            String msg = LOCALISER.msg("044031");
            NucleusLogger.METADATA.error(msg);
            throw new NucleusException(msg);
        }
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (Exception ignore) {
        }
        if (in == null) {
            try {
                in = new FileInputStream(StringUtils.getFileForFilename(url.getFile()));
            } catch (Exception ignore) {
            }
        }
        if (in == null) {
            NucleusLogger.METADATA.error(LOCALISER.msg("044032", url.toString()));
            throw new NucleusException(LOCALISER.msg("044032", url.toString()));
        }
        return parseMetaDataStream(in, url.toString(), handlerName);
    }

    /**
     * Method to parse a MetaData file given the filename.
     * @param fileName Name of the file
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException if error occurred
     */
    public MetaData parseMetaDataFile(String fileName, String handlerName) {
        InputStream in = null;
        try {
            in = new URL(fileName).openStream();
        } catch (Exception ignore) {
        }
        if (in == null) {
            try {
                in = new FileInputStream(StringUtils.getFileForFilename(fileName));
            } catch (Exception ignore) {
            }
        }
        if (in == null) {
            NucleusLogger.METADATA.error(LOCALISER.msg("044032", fileName));
            throw new NucleusException(LOCALISER.msg("044032", fileName));
        }
        return parseMetaDataStream(in, fileName, handlerName);
    }

    /**
     * Method to parse a MetaData file given an InputStream.
     * Closes the input stream when finished.
     * @param in input stream
     * @param filename Name of the file (if applicable)
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException thrown if error occurred
     */
    public synchronized MetaData parseMetaDataStream(InputStream in, String filename, String handlerName) {
        if (in == null) {
            throw new NullPointerException("input stream is null");
        }
        if (NucleusLogger.METADATA.isDebugEnabled()) {
            NucleusLogger.METADATA.debug(LOCALISER.msg("044030", filename, handlerName, validate ? "true" : "false"));
        }
        try {
            if (parser == null) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(validate);
                factory.setNamespaceAware(validate);
                if (validate) {
                    try {
                        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getRegisteredSchemas(pluginMgr));
                        if (schema != null) {
                            try {
                                factory.setSchema(schema);
                            } catch (UnsupportedOperationException e) {
                                NucleusLogger.METADATA.info(e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        NucleusLogger.METADATA.info(e.getMessage());
                    }
                    try {
                        factory.setFeature("http://apache.org/xml/features/validation/schema", true);
                    } catch (Exception e) {
                        NucleusLogger.METADATA.info(e.getMessage());
                    }
                }
                parser = factory.newSAXParser();
            }
            DefaultHandler handler = null;
            EntityResolver entityResolver = null;
            try {
                entityResolver = EntityResolverFactory.getInstance(pluginMgr, handlerName);
                if (entityResolver != null) {
                    parser.getXMLReader().setEntityResolver(entityResolver);
                }
                Class[] argTypes = new Class[] { ClassConstants.METADATA_MANAGER, String.class, EntityResolver.class };
                Object[] argValues = new Object[] { mgr, filename, entityResolver };
                handler = (DefaultHandler) pluginMgr.createExecutableExtension("org.datanucleus.metadata_handler", "name", handlerName, "class-name", argTypes, argValues);
                if (handler == null) {
                    throw new NucleusUserException(LOCALISER.msg("044028", handlerName)).setFatal();
                }
            } catch (Exception e) {
                String msg = LOCALISER.msg("044029", handlerName, e.getMessage());
                throw new NucleusException(msg, e);
            }
            ((AbstractMetaDataHandler) handler).setValidate(validate);
            parser.parse(in, handler);
            return ((AbstractMetaDataHandler) handler).getMetaData();
        } catch (NucleusException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof SAXException) {
                SAXException se = (SAXException) e;
                cause = se.getException();
            }
            cause = e.getCause() == null ? cause : e.getCause();
            NucleusLogger.METADATA.error(LOCALISER.msg("044040", filename, cause));
            if (cause instanceof InvalidMetaDataException) {
                throw (InvalidMetaDataException) cause;
            } else {
                String message = LOCALISER.msg("044033", e);
                throw new NucleusException(message, cause);
            }
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * The list of schemas registered in the plugin metadata_entityresolver
     * @param pm the PluginManager
     * @return the Sources pointing to the .xsd files
     */
    private Source[] getRegisteredSchemas(PluginManager pm) {
        ConfigurationElement[] elems = pm.getConfigurationElementsForExtension("org.datanucleus.metadata_entityresolver", null, null);
        Set<Source> sources = new HashSet<Source>();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i].getAttribute("type") == null) {
                InputStream in = MetaDataParser.class.getResourceAsStream(elems[i].getAttribute("url"));
                if (in == null) {
                    NucleusLogger.METADATA.warn("local resource \"" + elems[i].getAttribute("url") + "\" does not exist!!!");
                }
                sources.add(new StreamSource(in));
            }
        }
        return sources.toArray(new Source[sources.size()]);
    }
}
