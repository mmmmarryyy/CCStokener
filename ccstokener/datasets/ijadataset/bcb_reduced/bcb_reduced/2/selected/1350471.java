package pl.edu.mimuw.xqtav.exec.xqtavengine_1;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import pl.edu.mimuw.xqtav.exec.ExecEvent;
import pl.edu.mimuw.xqtav.exec.ExecEventCode;
import pl.edu.mimuw.xqtav.exec.ExecException;
import pl.edu.mimuw.xqtav.exec.TLSData;
import pl.edu.mimuw.xqtav.exec.api.ErrorHandler;
import pl.edu.mimuw.xqtav.exec.api.EventLogger;
import pl.edu.mimuw.xqtav.exec.api.InputProvider;
import pl.edu.mimuw.xqtav.exec.api.OutputProvider;
import pl.edu.mimuw.xqtav.exec.api.ProcessorRunner;
import pl.edu.mimuw.xqtav.exec.api.ProgressMonitor;
import pl.edu.mimuw.xqtav.exec.api.TavEngine;

/**
 * @author marchant
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XQTavEngine_1 implements TavEngine, ErrorListener {

    protected ErrorHandler intEH = null;

    protected EventLogger intEL = null;

    protected ProgressMonitor intPM = null;

    protected InputProvider intIP = null;

    protected OutputProvider intOP = null;

    protected ProcessorRunner intPR = null;

    protected String xqURI = null;

    public synchronized ErrorHandler getErrorHandler() {
        return intEH;
    }

    public synchronized void setErrorHandler(ErrorHandler h) {
        if (intEH != null) {
            intEH.flush();
            intEH = null;
        }
        intEH = h;
        reconnectHandlers();
    }

    public synchronized EventLogger getEventLogger() {
        return intEL;
    }

    public synchronized void setEventLogger(EventLogger h) {
        if (intEL != null) {
            intEL.flush();
            intEL = null;
        }
        intEL = h;
        reconnectHandlers();
    }

    public synchronized ProgressMonitor getProgressMonitor() {
        return intPM;
    }

    public synchronized void setProgressMonitor(ProgressMonitor h) {
        if (intPM != null) {
            intPM.flush();
            intPM = null;
        }
        intPM = h;
        reconnectHandlers();
    }

    public synchronized InputProvider getInputProvider() {
        return intIP;
    }

    public synchronized void setInputProvider(InputProvider h) {
        if (intIP != null) {
            intIP.flush();
            intIP = null;
        }
        intIP = h;
        reconnectHandlers();
    }

    public synchronized OutputProvider getOutputProvider() {
        return intOP;
    }

    public synchronized void setOutputProvider(OutputProvider h) {
        if (intOP != null) {
            intOP.flush();
            intOP = null;
        }
        intOP = h;
        reconnectHandlers();
    }

    public synchronized String getXQUri() {
        return xqURI;
    }

    public synchronized void setXQUri(String uri) throws ExecException {
        xqURI = uri;
    }

    public synchronized void executeXQ() throws ExecException {
        intPM.xquery_starting();
        try {
            TLSData.setEngine(this);
            intEL.registerEvent(new ExecEvent(ExecEvent.PRI_INFO, ExecEventCode.InfSaxonInitialize, "XQTavEngine_1", "Saxon initialized properly.", null));
            if (xqURI == null || xqURI.equals(":other:")) {
                intEL.registerEvent(new ExecEvent(ExecEvent.PRI_INFO, ExecEventCode.InfXQueryLoaded, "XQTavEngine_1", "XQuery load ignored (:other: specified or xqURI was NULL).", null));
            } else {
                loadXQueryFromURI(xqURI, null);
                intEL.registerEvent(new ExecEvent(ExecEvent.PRI_INFO, ExecEventCode.InfXQueryLoaded, "XQTavEngine_1", "XQuery loaded.", null));
            }
            processQuery();
        } catch (Exception e) {
            intPM.xquery_complete_failed();
            e.printStackTrace();
            throw new ExecException("Failed to process XQuery: " + e);
        }
        intPM.xquery_complete_ok();
    }

    public synchronized void resetXQ() {
    }

    public synchronized void resetAll() {
        resetXQ();
        if (intEH != null) intEH.reset();
        if (intEL != null) intEL.reset();
        if (intIP != null) intIP.reset();
        if (intOP != null) intOP.reset();
        if (intPM != null) intPM.reset();
    }

    public synchronized void flushAll() {
        if (intEH != null) intEH.flush();
        if (intEL != null) intEL.flush();
        if (intIP != null) intIP.flush();
        if (intOP != null) intOP.flush();
        if (intPM != null) intPM.flush();
    }

    public synchronized ProcessorRunner getProcessorRunner() {
        return intPR;
    }

    public synchronized void setProcessorRunner(ProcessorRunner h) {
        intPR = h;
        reconnectHandlers();
    }

    protected void reconnectHandlers() {
        if (intPR != null) {
            intPR.setErrorHandler(intEH);
            intPR.setEventLogger(intEL);
            intPR.setProgressMonitor(intPM);
        }
        if (intIP != null) {
            intIP.setPM(intPM);
        }
        if (intOP != null) {
            intOP.setPM(intPM);
        }
    }

    public void closeAll() {
        if (intEH != null) intEH.close();
        if (intEL != null) intEL.close();
        if (intIP != null) intIP.close();
        if (intOP != null) intOP.close();
        if (intPM != null) intPM.close();
    }

    /** IMPLEMENTACJA **/
    protected Configuration saxonConfiguration = null;

    protected Reader xqueryByReader = null;

    protected String xqueryByString = null;

    protected String xqueryByURI = null;

    protected String xqueryByURIContent = null;

    protected String xqueryByURICharset = null;

    protected XQueryExpression compiledXQuery = null;

    protected StaticQueryContext staticContext = null;

    protected DynamicQueryContext dynamicContext = null;

    /**
	 * Po to, zeby mozna sie bylo dobrac do struktur danych Saxona
	 * @return
	 */
    public DynamicQueryContext getDynamicContext() {
        return dynamicContext;
    }

    /**
	 * Po to, zeby mozna sie bylo dobrac do struktur danych Saxona
	 * @return
	 */
    public StaticQueryContext getStaticContext() {
        return staticContext;
    }

    protected DocumentInfo inputXMLtree = null;

    protected DocumentInfo resultDocument = null;

    public synchronized DocumentInfo getResultDocument() {
        return resultDocument;
    }

    public static final int maxXQueryLength = 10000000;

    public static final int maxXMLFileLength = 1000000000;

    public void preconfigure() throws ExecException {
        saxonConfiguration = new Configuration();
        saxonConfiguration.setAllowExternalFunctions(true);
        saxonConfiguration.setErrorListener(this);
        saxonConfiguration.setHostLanguage(Configuration.XQUERY);
        saxonConfiguration.setValidation(false);
        saxonConfiguration.setValidationWarnings(true);
    }

    public void loadXQueryFromURI(String docUri, String charset) throws ExecException {
        clearAllXQueryInputSources();
        xqueryByURI = docUri;
        xqueryByURICharset = charset;
        xqueryByURIContent = getDocumentByURI(docUri, charset, maxXQueryLength);
        compileXQuery();
    }

    public void loadXQueryFromReader(Reader reader) throws ExecException {
        clearAllXQueryInputSources();
        xqueryByReader = reader;
        compileXQuery();
    }

    public void loadXQueryFromString(String content) throws ExecException {
        clearAllXQueryInputSources();
        xqueryByString = content;
        compileXQuery();
    }

    /** 
	 * @see pl.edu.mimuw.xqtav.TavProcessor#loadXmlInputFromURI(java.lang.String)
	 * Additional info: prepares URL input stream and then passes it to loadXmlInputFromStream.
	 */
    public void loadXmlInputFromURI(String docUri) throws ExecException {
        if (docUri == null) {
            inputXMLtree = null;
            return;
        }
        try {
            URL url = URI.create(docUri).toURL();
            URLConnection urlconn = url.openConnection();
            urlconn.setAllowUserInteraction(false);
            urlconn.setDefaultUseCaches(true);
            urlconn.setDoInput(true);
            urlconn.setDoOutput(false);
            urlconn.connect();
            InputStream is = (InputStream) urlconn.getContent();
            loadXmlInputFromStream(is);
            is.close();
        } catch (Exception e) {
            throw new ExecException("Failed to load XML data by URI: " + e);
        }
    }

    /**
	 * @see pl.edu.mimuw.xqtav.TavProcessor#loadXmlInputFromString(java.lang.String) 
	 */
    public void loadXmlInputFromString(String content) throws ExecException {
        if (content == null) {
            inputXMLtree = null;
            return;
        }
        try {
            StringReader sr = new StringReader(content);
            StreamSource ss = new StreamSource(sr);
            inputXMLtree = staticContext.buildDocument(ss);
        } catch (Exception e) {
            throw new ExecException("Failed to load XML data by string: " + e);
        }
    }

    public void loadXmlInputFromStream(InputStream stream) throws ExecException {
        if (stream == null) {
            inputXMLtree = null;
            return;
        }
        try {
            StreamSource ss = new StreamSource(stream);
            inputXMLtree = staticContext.buildDocument(ss);
        } catch (Exception e) {
            throw new ExecException("Failed to load XML data by stream: " + e);
        }
    }

    public void processQuery() throws ExecException {
        try {
            resultDocument = null;
            dynamicContext = new DynamicQueryContext(saxonConfiguration);
            if (inputXMLtree != null) {
                dynamicContext.setContextNode(inputXMLtree);
            }
            SequenceIterator si = compiledXQuery.iterator(dynamicContext);
            resultDocument = QueryResult.wrap(si, saxonConfiguration);
            System.err.println("After execution: Saxon context node: " + dynamicContext.getContextNode());
        } catch (Exception e) {
            throw new ExecException("Failed to process query: " + e);
        }
    }

    public void fetchResult(OutputStream os) throws ExecException {
        try {
            Properties props = new Properties();
            props.setProperty(OutputKeys.METHOD, "xml");
            props.setProperty(OutputKeys.INDENT, "yes");
            QueryResult.serialize(resultDocument, new StreamResult(os), props, saxonConfiguration);
        } catch (Exception e) {
            throw new ExecException("Failed to fetch result: " + e);
        }
    }

    /*************** ErrorListener implementation *** BEGIN *****/
    public void fatalError(TransformerException te) throws TransformerException {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_CRITICAL, ExecEventCode.InfExceptionCaught, "xqtavengine_1", "Caught exception during XQuery processing", te);
        System.err.println("XQTav fatal error: " + te);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    public void error(TransformerException te) throws TransformerException {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_ERROR, ExecEventCode.InfExceptionCaught, "xqtavengine_1", "Caught exception during XQuery processing", te);
        System.err.println("XQTav error: " + te);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    public void warning(TransformerException te) throws TransformerException {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_WARNING, ExecEventCode.InfExceptionCaught, "xqtavengine_1", "Caught exception during XQuery processing", te);
        System.err.println("XQTav warning: " + te);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    /**
	 * Removes all potential data sources from which xquery document could be get
	 */
    protected void clearAllXQueryInputSources() {
        if (compiledXQuery != null) {
            compiledXQuery = null;
        }
        if (staticContext != null) {
            staticContext = null;
        }
        if (xqueryByReader != null) {
            try {
                xqueryByReader.close();
            } catch (Exception e) {
            }
            xqueryByReader = null;
        }
        if (xqueryByString != null) {
            xqueryByString = null;
        }
        if (xqueryByURI != null) {
            xqueryByURI = null;
            xqueryByURIContent = null;
            xqueryByURICharset = null;
        }
    }

    /**
	 * Compiles XQuery from any of sources.
	 * Check sequence: reader, string, uri
	 * @throws ExecException
	 */
    protected void compileXQuery() throws ExecException {
        if (xqueryByReader != null) {
            buildStaticQueryContext();
            try {
                compiledXQuery = staticContext.compileQuery(xqueryByReader);
            } catch (Exception e) {
                throw new ExecException("Failed to compile query: " + e);
            }
            return;
        }
        if (xqueryByString != null) {
            buildStaticQueryContext();
            try {
                compiledXQuery = staticContext.compileQuery(xqueryByString);
            } catch (Exception e) {
                throw new ExecException("Failed to compile query: " + e);
            }
        }
        if (xqueryByURI != null) {
            buildStaticQueryContext();
            if (xqueryByURIContent == null) {
                xqueryByURIContent = getDocumentByURI(xqueryByURI, xqueryByURICharset, maxXQueryLength);
            }
            try {
                compiledXQuery = staticContext.compileQuery(xqueryByURIContent);
            } catch (Exception e) {
                throw new ExecException("Failed to compile query: " + e);
            }
        }
    }

    /**
	 * Creates a new sttaic query context, setting advanced options when needed.
	 * @throws ExecException
	 */
    protected void buildStaticQueryContext() throws ExecException {
        staticContext = new StaticQueryContext(saxonConfiguration);
    }

    /**
	 * Downloads document from a specified URI and returns its context
	 * @param uri URI of resource to get
	 * @return document content
	 * @throws ExecException
	 */
    protected String getDocumentByURI(String uri, String declCharset, int maxLength) throws ExecException {
        try {
            URL url = URI.create(uri).toURL();
            URLConnection urlconn = url.openConnection();
            urlconn.setAllowUserInteraction(false);
            urlconn.setDefaultUseCaches(true);
            urlconn.setDoInput(true);
            urlconn.setDoOutput(false);
            urlconn.connect();
            int csize = urlconn.getContentLength();
            if (csize < 0) {
                csize = maxLength;
            } else {
                if (csize > maxLength) {
                    throw new ExecException("Input XQuery file too big");
                }
            }
            byte[] buf = new byte[csize];
            InputStream is = (InputStream) urlconn.getContent();
            int i = 0;
            int ret = 0;
            while (i < csize) {
                ret = is.read(buf, i, csize - i);
                if (ret < 0) {
                    break;
                }
                i += ret;
            }
            is.close();
            String val;
            if (declCharset != null) {
                val = new String(buf, 0, i, declCharset);
            } else {
                val = new String(buf, 0, i);
            }
            return val;
        } catch (Exception e) {
            throw new ExecException("Failed to download document content by URI: " + e);
        }
    }

    public Object readInput(String inputName) {
        return intIP.getInput(inputName);
    }

    public void writeOutput(String outputName, Object data) {
        intOP.consumeOutput(outputName, data);
    }

    public synchronized void fireError(String idname, String message, Object data) {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_ERROR, ExecEventCode.ErrFromXQuery, idname, message, data);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    public synchronized void fireFatalError(String idname, String message, Object data) {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_CRITICAL, ExecEventCode.ErrFromXQuery, idname, message, data);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    public synchronized void fireWarning(String idname, String message, Object data) {
        ExecEvent ev = new ExecEvent(ExecEvent.PRI_WARNING, ExecEventCode.ErrFromXQuery, idname, message, data);
        intEL.registerEvent(ev);
        intEH.registerEvent(ev);
    }

    public Configuration getConfiuration() {
        return saxonConfiguration;
    }
}
