package org.marcont.rdf.utils;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.net.*;
import net.sf.saxon.TransformerFactoryImpl;

/**
 * @author marsyn
 * Wrapper class for SAXON XSLT 2.0 classes.
 * Gets XML and XSL content from String, file, URI.
 * Returns results of transformation as String or file.
 */
public class XslTransformer {

    /**
	 * Transformer factory.
	 */
    private TransformerFactory factory;

    /**
	 * input XML filename
	 */
    private String xmlFileName;

    /**
	 * input XML URI
	 */
    @SuppressWarnings("unused")
    private URI xmlFileUri;

    /**
	 * input XSL (stylesheet) filename
	 */
    private String xslFileName;

    /**
	 * input XSL (stylesheet) input stream
	 */
    private InputStream xslInputStream;

    /**
	 * output filename
	 */
    private String resultFileName;

    /**
	 * result String (as a Writer class object)
	 */
    StringWriter resultString = new StringWriter();

    /**
	 * universal Source object for SAXON transformer
	 */
    private Source xmlSource;

    /**
	 * universal Templates class for SAXON transformer
	 */
    private Templates xslTemplate;

    /**
	 * universal Result class for SAXON transformer
	 */
    private Result result;

    /**
	 * Constructor with default input filenames of XML, XSL
	 * and output.
	 * @param xmlf input XML filename
	 * @param xslf input XSL filename
	 * @param out output filename
	 */
    public XslTransformer(String xmlf, String xslf, String out) {
        this();
        setXmlFileName(xmlf);
        setXslFileName(xslf);
        setResultFileName(out);
    }

    /**
	 * Default constructor.
	 */
    public XslTransformer() {
        factory = new TransformerFactoryImpl();
    }

    /**
	 * Loads XML content from URI
	 * @param uri
	 */
    protected void loadXmlFromUri(URI uri) {
        URLConnection urlc;
        try {
            urlc = uri.toURL().openConnection();
            InputStream is = urlc.getInputStream();
            Reader rd = new InputStreamReader(is);
            xmlSource = new StreamSource(rd);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * Loads XSL content from URI
	 * @param uri
	 */
    protected void loadXslFromUri(URI uri) {
        URLConnection urlc;
        try {
            urlc = uri.toURL().openConnection();
            InputStream is = urlc.getInputStream();
            Reader rd = new InputStreamReader(is);
            Source xslSource = new StreamSource(rd);
            try {
                xslTemplate = factory.newTemplates(xslSource);
            } catch (TransformerConfigurationException tce) {
                tce.printStackTrace();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * Loads XML from file
	 */
    public void loadXmlFromFile() {
        File file = new File(xmlFileName);
        xmlSource = new StreamSource(file);
    }

    /**
	 * Loads XSL from file
	 */
    public void loadXslFromFile() {
        File file = new File(xslFileName);
        Source xslSource = new StreamSource(file);
        try {
            xslTemplate = factory.newTemplates(xslSource);
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        }
    }

    /**
	 * Loads XSL from input stream
	 */
    public void loadXslFromInputStream() {
        Source xslSource = new StreamSource(xslInputStream);
        try {
            xslTemplate = factory.newTemplates(xslSource);
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        }
    }

    /**
	 * Prepares output file for writing
	 */
    protected void openResultFile() {
        File file = new File(resultFileName);
        result = new StreamResult(file);
    }

    /**
	 * Performs transformation. A 'go!' method.
	 */
    public void transform() {
        Transformer transformer;
        try {
            transformer = xslTemplate.newTransformer();
            transformer.transform(xmlSource, result);
        } catch (TransformerException te) {
            te.printStackTrace();
        }
    }

    /**
	 * Getter for XSL filename
	 * @return xsl filename
	 */
    public String getXslFileName() {
        return xslFileName;
    }

    /**
	 * Setter for XSL filename
	 * Setting a filename automatically opens it for reading
	 * @param name XSL input filename
	 */
    public void setXslFileName(String name) {
        xslFileName = name;
        loadXslFromFile();
    }

    /**
	 * Setter for XSL input stream
	 * Setting a inputStream automatically opens it for reading
	 * @param name XSL input stream
	 */
    public void setXslInputStream(InputStream inputStream) {
        xslInputStream = inputStream;
        loadXslFromInputStream();
    }

    /**
	 * Getter for XML filename
	 * @return xml filename
	 */
    public String getXmlFileName() {
        return xmlFileName;
    }

    /**
	 * Setter for XML filename
	 * Setting a filename automatically opens it for reading
	 * @param name XML input filename
	 */
    public void setXmlFileName(String name) {
        xmlFileName = name;
        loadXmlFromFile();
    }

    /**
	 * Getter for output filename
	 * @return output filename
	 */
    public String getResultFileName() {
        return resultFileName;
    }

    /**
	 * Setter for output filename
	 * Setting output filename automatically opens it for writing
	 * @param resfn output filename
	 */
    public void setResultFileName(String resfn) {
        resultFileName = resfn;
        openResultFile();
    }

    /**
	 * Sets XSL source as String
	 * XSL content will be taken from provided String
	 * @param xsls XSL string
	 */
    public void setXslString(String xsls) {
        StringReader sr = new StringReader(xsls);
        Source xslSource = new StreamSource(sr);
        try {
            xslTemplate = factory.newTemplates(xslSource);
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        }
    }

    /**
	 * Sets XML source as String
	 * XML content will be taken from provided String
	 * @param xmls
	 */
    public void setXmlString(String xmls) {
        StringReader sr = new StringReader(xmls);
        xmlSource = new StreamSource(sr);
    }

    /**
	 * Sets result as String
	 * Result of the transformation will be available as String
	 */
    public void setResultAsString() {
        resultString = new StringWriter();
        result = new StreamResult(resultString);
    }

    /**
	 * Gets result of transformation as String
	 * (requires setting result as String earlier)
	 * @return result of the transofrmation
	 */
    public String getResultString() {
        return resultString.toString();
    }
}
