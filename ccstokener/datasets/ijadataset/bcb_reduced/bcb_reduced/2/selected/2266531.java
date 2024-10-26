package de.psisystems.dmachinery.engines.xslfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import de.psisystems.dmachinery.caches.Cache;
import de.psisystems.dmachinery.caches.CacheException;
import de.psisystems.dmachinery.caches.CacheManager;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.core.types.InputFormat;
import de.psisystems.dmachinery.core.types.OutputFormat;
import de.psisystems.dmachinery.engines.AbstractPrintEngine;
import de.psisystems.dmachinery.engines.PrintEngineFactory;
import de.psisystems.dmachinery.io.IOUtil;

/**
 * @author stefanpudig
 * 
 */
public class XSLFOPrintEngine extends AbstractPrintEngine {

    private static final Log log = LogFactory.getLog(XSLFOPrintEngine.class);

    private static final InputFormat[] INPUTFORMATS = { InputFormat.XSLFO };

    private static final OutputFormat[] OUTPUTFORMATS = { OutputFormat.PDF, OutputFormat.AFP };

    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    public XSLFOPrintEngine() {
        PrintEngineFactory.getInstance().register(new HashSet<InputFormat>(Arrays.asList(INPUTFORMATS)), new HashSet<OutputFormat>(Arrays.asList(OUTPUTFORMATS)), this.getClass());
    }

    public XSLFOPrintEngine(Map<String, Object> attributes) {
        super(INPUTFORMATS, OUTPUTFORMATS, attributes);
    }

    @Override
    protected void printAfterParameterCheck(URL templateURL, URL dataURL, OutputFormat outputFormat, URL destURL) throws PrintException {
        Transformer transformer;
        Templates templates;
        transformerFactory.setErrorListener(new ErrorListener() {

            public void error(TransformerException arg0) throws TransformerException {
                log.info(arg0);
            }

            public void fatalError(TransformerException arg0) throws TransformerException {
                log.info(arg0);
            }

            public void warning(TransformerException arg0) throws TransformerException {
                log.info(arg0);
            }
        });
        templates = (Templates) getTemplatesFromCache(templateURL);
        try {
            transformer = templates.newTransformer();
            if (templates != null) log.debug("Transformer created for " + templateURL);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Initialisierung Transformerfactory " + templateURL, e);
        }
        Reader dataReader = null;
        OutputStream outputStream = null;
        try {
            URLConnection connection = destURL.openConnection();
            connection.setDoOutput(true);
            outputStream = connection.getOutputStream();
            FopFactory fopFactory = FopFactory.newInstance();
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            foUserAgent.setBaseURL(templateURL.toExternalForm());
            Fop fop = fopFactory.newFop(outputFormat.toFopMime(), foUserAgent, outputStream);
            Result result = new SAXResult(fop.getDefaultHandler());
            dataReader = new InputStreamReader(dataURL.openStream(), "utf-8");
            if (log.isDebugEnabled()) {
                log.debug("create DataReader for" + dataURL);
            }
            Source dataSource = new StreamSource(dataReader);
            transformer.setParameter("base", templateURL.toExternalForm());
            transformer.setParameter("language", Locale.getDefault().getLanguage());
            transformer.transform(dataSource, result);
            if (log.isDebugEnabled()) log.debug("Transformed " + templateURL);
        } catch (TransformerException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (FOPException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            if (transformer != null) {
                transformer.clearParameters();
                transformer = null;
            }
            IOUtil.close(outputStream);
            IOUtil.close(dataReader);
        }
    }

    protected Templates getTemplatesFromCache(URL url) throws PrintException {
        Templates templates = null;
        Cache cache = CacheManager.getInstance().getCache(this.getClass().getName());
        try {
            templates = (Templates) cache.get(url);
        } catch (CacheException e) {
            throw new PrintException(e.getMessage(), e);
        }
        if (log.isDebugEnabled()) {
            if (templates != null) log.debug("took " + url + " from cache");
        }
        if (templates == null) {
            StreamSource inputSource = null;
            try {
                inputSource = new StreamSource(new InputStreamReader(url.openStream(), "utf-8"), url.toExternalForm());
                if (log.isDebugEnabled()) {
                    log.debug("read " + url + " to cache");
                }
            } catch (IOException e) {
                throw new PrintException("IO Fehler templateURL " + e.getMessage(), e);
            }
            try {
                templates = transformerFactory.newTemplates(inputSource);
            } catch (TransformerConfigurationException e) {
                throw new RuntimeException("Initialisierung Transformerfactory " + url, e);
            }
            try {
                cache.add(url, templates);
            } catch (CacheException e) {
                throw new PrintException(e.getMessage(), e);
            }
        }
        return templates;
    }
}
