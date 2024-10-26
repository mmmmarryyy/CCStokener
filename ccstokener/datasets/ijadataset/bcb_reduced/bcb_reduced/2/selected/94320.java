package org.merlotxml.util.xml.xerces;

import java.io.BufferedInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.parsers.XMLGrammarCachingConfiguration;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xml.serialize.DOMSerializer;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.merlotxml.merlot.MerlotDebug;
import org.merlotxml.merlot.plugin.PluginClassLoader;
import org.merlotxml.merlot.plugin.PluginConfig;
import org.merlotxml.merlot.plugin.PluginManager;
import org.merlotxml.util.xml.DOMLiaisonImplException;
import org.merlotxml.util.xml.DTDCache;
import org.merlotxml.util.xml.DTDCacheEntry;
import org.merlotxml.util.xml.DTDDocument;
import org.merlotxml.util.xml.GrammarDocument;
import org.merlotxml.util.xml.ValidDocument;
import org.tolven.logging.TolvenLogger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Xerces DOM Liaison Implementation
 * 
 * @author Tim McCune
 */
public class DOMLiaison implements org.merlotxml.util.xml.ValidDOMLiaison {

    protected Vector<EntityResolver> _entityResolverList;

    private String _encoding = "UTF-8";

    /**
     * Create a Document
     * @return An empty Document
     */
    public Document createDocument() {
        return new DocumentImpl();
    }

    public void print(ValidDocument doc, Writer output, String resultns, boolean format) throws DOMLiaisonImplException {
        Document d = doc.getDocument();
        DocumentType doctype = d.getDoctype();
        String externid = null;
        DTDDocument dtd = doc.getMainDTDDocument();
        if (dtd != null) {
            externid = dtd.getExternalID();
        } else {
            TolvenLogger.info("Doctype was not a DTD", DOMLiaison.class);
            if (doctype != null) {
                TolvenLogger.info("doctype.class = " + doctype.getClass(), DOMLiaison.class);
            } else {
                TolvenLogger.info("doctype was null", DOMLiaison.class);
            }
        }
        printImpl(d, output, resultns, format, externid);
    }

    /**
     * Print a Document
     * 
     * @param doc The Document to print
     * @param output Writer to send the output to
     * @param resultns Result name space for the output.  Used for things
     *		like HTML hacks.
     * @param format If true, output will be nicely tab-formatted.
     *		If false, there shouldn't be any line breaks or tabs between
     *		elements in the output.  Sometimes setting this to false
     *		is necessary to get your HTML to work right.
     * @exception DOMLiaisonImplException
     *		Wrapper exception that is thrown if the implementing class
     *		throws any kind of exception.
     */
    public void print(Document doc, Writer output, String resultns, boolean format) throws DOMLiaisonImplException {
        printImpl(doc, output, resultns, format, null);
    }

    private void printImpl(Document doc, Writer output, String resultns, boolean format, String externid) throws DOMLiaisonImplException {
        String enc = _encoding;
        try {
            SerializerFactory sfact = SerializerFactory.getSerializerFactory(Method.XML);
            OutputFormat outformat = new OutputFormat(doc, enc, format);
            outformat.setLineWidth(0);
            outformat.setPreserveSpace(false);
            Serializer serializer = sfact.makeSerializer(output, outformat);
            if (serializer instanceof DOMSerializer) {
                ((DOMSerializer) serializer).serialize(doc);
            }
        } catch (IOException ex) {
            throw new DOMLiaisonImplException(ex);
        }
    }

    /**
     * Parse a stream of XML into a Document
     * 
     * @param xmlReader XML stream reader
     * @return The Document that was parsed
     * @exception DOMLiaisonImplException
     *		Wrapper exception that is thrown if the implementing class
     *		throws any kind of exception.
     * @deprecated Use parseXMLStream(Reader)
     */
    public Document parseXMLStream(InputStream is) throws DOMLiaisonImplException {
        return parseXMLStream(new InputSource(is));
    }

    public Document parseXMLStream(Reader in) throws DOMLiaisonImplException {
        return parseXMLStream(new InputSource(in));
    }

    private Document parseXMLStream(InputSource is) throws DOMLiaisonImplException {
        return parseXMLStream(is, true);
    }

    private Document parseXMLStream(InputSource is, boolean validate) throws DOMLiaisonImplException {
        DOMParser parser;
        parser = new DOMParser();
        try {
            parser.setFeature("http://xml.org/sax/features/validation", validate);
        } catch (SAXException e) {
            TolvenLogger.info("error in setting up parser feature", DOMLiaison.class);
        }
        try {
            parser.setFeature("http://apache.org/xml/features/domx/grammar-access", validate);
        } catch (Exception e) {
            TolvenLogger.info("warning: unable to set grammar-access feature.", DOMLiaison.class);
        }
        try {
            parser.parse(is);
        } catch (SAXException e) {
            throw new DOMLiaisonImplException(e);
        } catch (IOException e) {
            throw new DOMLiaisonImplException(e);
        }
        return parser.getDocument();
    }

    public void setProperties(Properties props) {
    }

    public void addEntityResolver(EntityResolver er) {
        if (_entityResolverList == null) _entityResolverList = new Vector<EntityResolver>();
        _entityResolverList.add(er);
    }

    public class MyEntityResolver implements EntityResolver {

        ValidDocument _vdoc = null;

        String publicId = null;

        String systemId = null;

        public MyEntityResolver(ValidDocument doc) {
            _vdoc = doc;
        }

        /**
         * This entity resolver finds a dtd file on the filesystem if it can.
         * It does this by first checking the specified file (given as the
         * systemId paramter which comes from the SYSTEM specifier in the
         * XML &lt;!DOCTYPE&gt; definition. If the systemId isn't a full path or
         * url to a valid file, then the resolver tries to find the file using the
         * path.dtd resource from ResourceCatalog.
         *
         * @param publicId the public identifier for the entity
         * @param systemId the system identifier (usually a filename or url)
         * of the external entitiy.
         * @exception SAXException this is thrown by the DTD parser during DTD parsing.
         * @exception IOException FileNotFound is the typical IOException thrown in the
         * case that the external entity file can't be found. Other IOExceptions may be
         * thrown depending on the external entity file operations.
         */
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            MerlotDebug.msg("Resolving entity: publicId: " + publicId + " systemId: " + systemId);
            this.publicId = publicId;
            this.systemId = systemId;
            DTDCacheEntry dtdentry = null;
            if (systemId.startsWith("plugin:")) {
                String path = systemId.substring(10);
                PluginClassLoader pcl;
                PluginManager pm = PluginManager.getInstance();
                List<PluginConfig> plugins = pm.getPlugins();
                URL url = null;
                File tryPlugin;
                for (int i = 0; i < plugins.size(); i++) {
                    tryPlugin = ((PluginConfig) plugins.get(i)).getSource();
                    pcl = new PluginClassLoader(tryPlugin);
                    url = pcl.findResource(path);
                    if (url != null) {
                        break;
                    }
                }
                if (url != null) {
                    InputSource src = new InputSource(url.openStream());
                    return src;
                }
            }
            dtdentry = DTDCache.getSharedInstance().findDTD(publicId, systemId, _vdoc.getFileLocation());
            if (dtdentry != null) {
                _vdoc.addDTD(dtdentry, systemId);
                InputStream is = null;
                PluginClassLoader pcl;
                PluginManager pm = PluginManager.getInstance();
                List<PluginConfig> plugins = pm.getPlugins();
                URL url = null;
                File tryPlugin;
                for (int i = 0; i < plugins.size(); i++) {
                    tryPlugin = ((PluginConfig) plugins.get(i)).getSource();
                    pcl = new PluginClassLoader(tryPlugin);
                    url = pcl.findResource(dtdentry.getFilePath());
                    if (url != null) {
                        break;
                    }
                }
                if (url != null) {
                    try {
                        is = url.openStream();
                        InputSource src = new InputSource(is);
                        src.setSystemId("plugin:///" + dtdentry.getFilePath());
                        return src;
                    } catch (IOException ioe) {
                    }
                }
                try {
                    String path = dtdentry.getFilePath();
                    if (path != null && path.startsWith("file:")) {
                        path = path.substring(5);
                        is = new FileInputStream(path);
                    } else if (systemId != null && systemId.startsWith("http:")) {
                        url = new URL(systemId);
                        is = url.openStream();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    TolvenLogger.info("plugin uri is probably invalid", DOMLiaison.class);
                    return null;
                }
                if (is != null) {
                    InputSource src = new InputSource(is);
                    src.setSystemId(systemId);
                    return src;
                }
                char[] chars = dtdentry.getCachedDTDStream();
                CharArrayReader r = new CharArrayReader(chars);
                return new InputSource(r);
            }
            if (_entityResolverList != null) {
                if (dtdentry == null) {
                    for (int i = _entityResolverList.size() - 1; i >= 0; i--) {
                        EntityResolver er = _entityResolverList.elementAt(i);
                        try {
                            dtdentry = DTDCache.getSharedInstance().resolveDTD(publicId, systemId, er, _vdoc.getFileLocation());
                        } catch (IOException ioe) {
                            continue;
                        }
                        if (dtdentry != null) {
                            CharArrayReader car = new CharArrayReader(dtdentry.getCachedDTDStream());
                            _vdoc.addDTD(dtdentry, systemId);
                            return new InputSource(car);
                        }
                    }
                }
            }
            TolvenLogger.info("Resolve entity is returning null.", DOMLiaison.class);
            return null;
        }

        public String getSavedPublicId() {
            return publicId;
        }

        public String getSavedSystemId() {
            return systemId;
        }
    }

    public ValidDocument createValidDocument() {
        ValidDocument vdoc = new ValidDocument(createDocument());
        return vdoc;
    }

    /**
     * Parses an input stream containing XML using a validating parser.
     * Returns a ValidDocument which gives access to DTD information
     * and stuff.
     */
    public ValidDocument parseValidXMLStream(InputStream is, String fileLocation) throws DOMLiaisonImplException {
        MerlotDebug.msg("Xerces: parseValidXMLStream");
        ValidDocument doc = new ValidDocument();
        doc.setFileLocation(fileLocation);
        try {
            is = new BufferedInputStream(is);
            if (is.markSupported()) {
                is.mark(1024);
                byte[] b = new byte[512];
                int i = is.read(b);
                if (i > 0) {
                    String s = new String(b);
                    int e = s.indexOf("encoding=");
                    if (e >= 0) {
                        String encbuf = s.substring(e);
                        StringTokenizer tok = new StringTokenizer(encbuf, "\"'", true);
                        boolean encfound = false;
                        while (tok.hasMoreTokens()) {
                            s = tok.nextToken();
                            if (s.equals("'") || s.equals("\"")) {
                                encfound = true;
                            } else {
                                if (encfound) {
                                    _encoding = s;
                                    break;
                                }
                            }
                        }
                    }
                }
                is.reset();
            } else {
            }
        } catch (IOException ex) {
        }
        InputSource inputSource = new InputSource(is);
        inputSource.setSystemId(fileLocation);
        ErrorHandler errorHandler = new DefaultErrorHandler();
        EntityResolver entityResolver = new MyEntityResolver(doc);
        parseValidXMLStream(doc, inputSource, errorHandler, entityResolver);
        return doc;
    }

    public void parseValidXMLStream(ValidDocument doc, InputSource inputSource, ErrorHandler errorHandler, EntityResolver entityResolver) throws DOMLiaisonImplException {
        MerlotDebug.msg("Xerces: parseValidXMLStream");
        DOMParser parser = null;
        try {
            XMLGrammarCachingConfiguration config = new XMLGrammarCachingConfiguration();
            config.clearGrammarPool();
            parser = new DOMParser(config);
            try {
                parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
                parser.setFeature("http://xml.org/sax/features/validation", true);
                parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            } catch (SAXException e) {
                TolvenLogger.info("error in setting up validation feature", DOMLiaison.class);
            }
            parser.setErrorHandler(errorHandler);
            parser.setEntityResolver(entityResolver);
            long start = System.currentTimeMillis();
            parser.parse(inputSource);
            long end = System.currentTimeMillis();
            TolvenLogger.info("Duration - parsing document: " + (end - start) + " ms.", DOMLiaison.class);
            doc.setEncoding(inputSource.getEncoding());
            Document domDocument = parser.getDocument();
            doc.setDocument(domDocument);
            XMLGrammarPool pool = null;
            try {
                pool = (XMLGrammarPool) parser.getProperty("http://apache.org/xml/properties/internal/grammar-pool");
            } catch (org.xml.sax.SAXNotRecognizedException ex) {
                MerlotDebug.msg("Exception: " + ex);
            }
            if (pool != null) {
                GrammarDocument grammarDocument = null;
                Grammar[] dtdGrammars = pool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_DTD);
                Grammar[] schemaGrammars = pool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
                if (dtdGrammars.length > 0) {
                    grammarDocument = new DTDGrammarDocumentImpl(dtdGrammars);
                } else if (schemaGrammars.length > 0) {
                    MerlotDebug.msg("SchemaGrammars: " + schemaGrammars.length);
                    grammarDocument = new SchemaGrammarDocumentImpl(schemaGrammars);
                    new SchemaIdentityConstraintValidator(domDocument, (SchemaGrammarDocumentImpl) grammarDocument);
                    loadSchemaDocument(entityResolver);
                } else MerlotDebug.msg("No Grammars found.");
                doc.setGrammarDocument(grammarDocument);
            } else MerlotDebug.msg("XMLGrammarPool is null.");
            return;
        } catch (Exception ex) {
            MerlotDebug.msg("Exception: class = " + ex.getClass().getName());
            int linenumber;
            int colnumber;
            String appendMsg = null;
            if (ex instanceof SAXParseException) {
                linenumber = ((SAXParseException) ex).getLineNumber();
                colnumber = ((SAXParseException) ex).getColumnNumber();
                appendMsg = " on line " + linenumber + ", column " + colnumber;
                throw new DOMLiaisonImplException(ex, appendMsg);
            } else if (ex instanceof SAXException) {
                Exception inex = ((SAXException) ex).getException();
                if (inex != null) {
                    throw new DOMLiaisonImplException(inex);
                } else {
                    throw new DOMLiaisonImplException(ex);
                }
            } else {
                throw new DOMLiaisonImplException(ex);
            }
        }
    }

    void loadSchemaDocument(EntityResolver entityResolver) {
        try {
            if (entityResolver instanceof MyEntityResolver) {
                String publicId = ((MyEntityResolver) entityResolver).getSavedPublicId();
                String systemId = ((MyEntityResolver) entityResolver).getSavedSystemId();
                InputSource schemaInputSource = entityResolver.resolveEntity(publicId, systemId);
                if (schemaInputSource != null) {
                    Document schemaDocument = this.parseXMLStream(schemaInputSource, false);
                    MerlotDebug.msg("schemaDocument: " + schemaDocument);
                    Schema schema = Schema.getInstance();
                    schema.setDocument(schemaDocument);
                } else MerlotDebug.msg("schemaInputSource is null.");
            } else {
                MerlotDebug.msg("Entity resolver class: " + entityResolver.getClass());
            }
        } catch (Exception ex) {
            MerlotDebug.msg("Exception: " + ex);
        }
    }

    public class DefaultErrorHandler implements ErrorHandler {

        public DefaultErrorHandler() {
        }

        public void warning(SAXParseException exception) throws SAXException {
            MerlotDebug.msg("Line " + exception.getLineNumber() + ": Parser warning: " + exception.getMessage());
        }

        public void error(SAXParseException exception) throws SAXException {
            MerlotDebug.msg("Line " + exception.getLineNumber() + ": Parser error: " + exception.getMessage());
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            MerlotDebug.msg("Line " + exception.getLineNumber() + ": Parser fatal error: " + exception.getMessage());
            throw exception;
        }
    }
}
