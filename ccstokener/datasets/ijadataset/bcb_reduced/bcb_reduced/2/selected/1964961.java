package org.proclos.etlcore.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.proclos.etlcore.util.FileUtil;
import org.proclos.etlcore.config.ConfigurationException;

/**
 * @author Christian Schwarzinger. Mail: christian.schwarzinger@proclos.com
 *
 */
public class XMLReader {

    private URL url;

    public XMLReader() {
    }

    public Document readDocument(String filename) throws ConfigurationException {
        try {
            URL url = new URL(filename);
            return readDocument(url);
        } catch (Exception e) {
        }
        ;
        try {
            File f = new File(filename);
            return readDocument(f.toURL());
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public String readDocumentAsString(String filename) throws ConfigurationException {
        StringWriter writer = new StringWriter();
        try {
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(readDocument(filename), writer);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
        return writer.toString();
    }

    public String getPath() {
        return FileUtil.getDirectory(url);
    }

    public String getFilename() {
        return FileUtil.getFile(url);
    }

    public URL getURL() {
        return url;
    }

    private Document readDocument(URL url) throws IOException, JDOMException {
        this.url = url;
        Reader r = new BufferedReader(new InputStreamReader(url.openStream(), "UTF8"));
        Document document = new SAXBuilder().build(r);
        return ConfigConverter.getInstance().convert(document);
    }
}
