package de.psisystems.dmachinery.tasks;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.stream.EventFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.io.IOUtil;
import de.psisystems.dmachinery.xml.pipe.PipeHead;
import de.psisystems.dmachinery.xml.pipe.filter.ElementEndFilter;
import de.psisystems.dmachinery.xml.pipe.filter.ElementNameFilter;
import de.psisystems.dmachinery.xml.pipe.filter.NegFilter;
import de.psisystems.dmachinery.xml.pipe.items.BlocksXSLPipeItem;
import de.psisystems.dmachinery.xml.pipe.items.FilterPipeItem;
import de.psisystems.dmachinery.xml.pipe.items.MergeXml;
import de.psisystems.dmachinery.xml.pipe.items.PipeItem;
import de.psisystems.dmachinery.xml.pipe.items.WriterPipeItem;

public class PrintJobTransfomHelper {

    private static final Log log = LogFactory.getLog(SimplePrintTask.class);

    public static URL transform(URL dest, URL data, URL dataToInject) throws PrintException {
        StringWriter xmlWriter = new StringWriter();
        StringWriter xslWriter = new StringWriter();
        mergeAndSplit(data, dataToInject, xmlWriter, xslWriter);
        transform(xmlWriter.toString(), xslWriter.toString(), dest);
        return dest;
    }

    private static void transform(String xml, String xsl, URL dest) throws PrintException {
        Writer result = null;
        URLConnection urlConnection = null;
        try {
            urlConnection = dest.openConnection();
            urlConnection.setDoOutput(true);
            result = new OutputStreamWriter(urlConnection.getOutputStream(), "utf-8");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(xsl)));
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
            if (log.isDebugEnabled()) {
                log.debug("transformed");
            }
        } catch (IOException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (TransformerConfigurationException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (TransformerException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            if (result != null) IOUtil.close(result);
        }
    }

    private static void mergeAndSplit(URL data, URL mergeData, StringWriter xmlWriter, StringWriter xslWriter) throws PrintException {
        EventFilter dataPart = new NegFilter(new ElementNameFilter("", "blocks", false));
        EventFilter textblockPart = new ElementNameFilter("", "blocks", false);
        EventFilter dataEndElement = new ElementEndFilter("", "data");
        PipeItem dataPipe = new FilterPipeItem(dataPart, new MergeXml(mergeData, dataEndElement, new WriterPipeItem(xmlWriter)));
        PipeItem xslPipe = new FilterPipeItem(textblockPart, new BlocksXSLPipeItem(new WriterPipeItem(xslWriter)));
        Reader reader = null;
        try {
            reader = new InputStreamReader(data.openStream(), "utf-8");
            PipeHead pipeFeeder = new PipeHead(reader, dataPipe, xslPipe);
            pipeFeeder.feed();
            if (log.isDebugEnabled()) {
                log.debug("Pipe complete ");
            }
        } catch (XMLStreamException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (FactoryConfigurationError e) {
            throw new PrintException(e.getMessage());
        } catch (IOException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            IOUtil.close(reader);
            IOUtil.flushAndClose(xslWriter);
            IOUtil.flushAndClose(xmlWriter);
        }
    }
}
