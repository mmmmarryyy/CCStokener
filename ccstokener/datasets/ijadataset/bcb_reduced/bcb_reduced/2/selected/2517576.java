package de.psisystems.dmachinery.engines.itext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.core.types.InputFormat;
import de.psisystems.dmachinery.core.types.OutputFormat;
import de.psisystems.dmachinery.engines.AbstractPrintEngine;
import de.psisystems.dmachinery.engines.PrintEngineFactory;
import de.psisystems.dmachinery.io.IOUtil;

/**
 * Created by IntelliJ IDEA.
 * User: stefanpudig
 * Date: Jul 25, 2009
 * Time: 11:18:14 PM
 * PrintEngine to fill PDF file fields via iText
 */
public class ITextPrintEngine extends AbstractPrintEngine {

    private static final Log log = LogFactory.getLog(ITextPrintEngine.class);

    private static final InputFormat[] INPUTFORMATS = { InputFormat.PDF };

    private static final OutputFormat[] OUTPUTFORMATS = { OutputFormat.PDF };

    public ITextPrintEngine() {
        PrintEngineFactory.getInstance().register(new HashSet<InputFormat>(Arrays.asList(INPUTFORMATS)), new HashSet<OutputFormat>(Arrays.asList(OUTPUTFORMATS)), this.getClass());
    }

    public ITextPrintEngine(Map<String, Object> attributes) {
        super(INPUTFORMATS, OUTPUTFORMATS, attributes);
    }

    protected void printAfterParameterCheck(URL templateURL, URL dataURL, OutputFormat outputFormat, URL destURL) throws PrintException {
        log.debug("Printing... " + templateURL);
        InputStream inputStream = null;
        Reader dataReader = null;
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            byte[] t = (byte[]) getFromCache(templateURL);
            inputStream = new ByteArrayInputStream(t);
            try {
                reader = ITextHelper.createPdfReader(inputStream);
            } catch (PrintException e) {
                throw new RuntimeException("template not found " + templateURL, e);
            }
            if (log.isDebugEnabled()) {
                log.debug("create PDFReader for" + templateURL);
            }
            URLConnection urlConnection = null;
            try {
                urlConnection = destURL.openConnection();
                urlConnection.setDoOutput(true);
                stamper = ITextHelper.createPdfStamper(reader, urlConnection.getOutputStream(), getAttributes());
            } catch (IOException e) {
                throw new RuntimeException("destURL not found " + destURL, e);
            }
            if (log.isDebugEnabled()) {
                log.debug("create PDFStamper for" + templateURL);
            }
            try {
                ITextHelper.mergeFormFields(stamper, templateURL, dataURL);
            } catch (IOException e) {
                throw new RuntimeException("merge fields ", e);
            }
            if (log.isDebugEnabled()) {
                log.debug("merge Formfields");
            }
        } finally {
            ITextHelper.close(stamper);
            IOUtil.close(dataReader);
            IOUtil.close(inputStream);
            ITextHelper.close(reader);
        }
    }
}
