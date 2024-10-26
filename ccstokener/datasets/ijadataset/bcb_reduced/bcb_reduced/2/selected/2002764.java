package org.iptc.ines.helpers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Ines methods helpers frequently used
 * 
 * @author Bertrand Goupil
 * 
 */
public class InesHelper {

    /**
	 * Get inputStream from a Remote value
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
    public static InputStream loadFile(String filename) throws IOException {
        URL url = new URL(filename);
        return loadFile(url);
    }

    /**
	 * Get InputStream From a Remote Value as URL. <br/> It use only HTTP
	 * protocol
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
    public static InputStream loadFile(URL url) throws IOException {
        InputStream response = null;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        response = connection.getInputStream();
        return response;
    }

    /**
	 * delete BOM data to be parsed by XML parser
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
    public static InputStream getCleanInputStream(InputStream inputStream) throws IOException {
        UnicodeInputStream stream = new UnicodeInputStream(inputStream, "UTF-8");
        stream.getEncoding();
        return stream;
    }

    /**
	 * Use this when BOM signature is set to UTF-8 XML File, Parser send an
	 * error when signature is set.
	 * 
	 * @param xmlFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static InputStream getCleanInputStream(File xmlFile) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(xmlFile);
        return getCleanInputStream(in);
    }

    /**
	 * Use it for process UTF-8 file with BOM signature. Parser send an error
	 * when signature is set.
	 * 
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static InputStream getCleanInputStream(String filename) throws FileNotFoundException, IOException {
        File xmlFile = new File(filename);
        return getCleanInputStream(xmlFile);
    }

    public static Document fileinputStreamToDocument(InputStream in) {
        DOMParser parser = new DOMParser();
        try {
            parser.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputSource source = new InputSource(in);
            parser.parse(source);
            return parser.getDocument();
        } catch (SAXNotRecognizedException e) {
            e.printStackTrace();
            return null;
        } catch (SAXNotSupportedException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static OutputStream documentToOutputStream(Document doc) {
        if (doc == null) return null;
        OutputStream out = new ByteArrayOutputStream();
        OutputFormat outputFormat = new OutputFormat("xml", "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(outputFormat);
        serializer.setNamespaces(true);
        serializer.setOutputByteStream(out);
        try {
            serializer.serialize(doc);
        } catch (IOException e) {
            String message = "couldn't serialize document";
            System.out.println(message);
        }
        return out;
    }
}
