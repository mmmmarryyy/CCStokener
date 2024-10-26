package com.loribel.commons.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import junit.framework.TestCase;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.loribel.commons.xml.csv.GB_Csv2XmlReader;
import com.loribel.commons.xml.csv.GB_Csv2XmlReaderAtt;
import com.loribel.commons.xml.csv.GB_Csv2XmlReaderHtml;

/**
 * Test GB_Csv2Xml
 * 
 * TODO Terminer cette classe de test
 * @author Gregory Borelli
 * @version 0.1
 */
public class GB_Csv2XmlTest extends TestCase {

    public static void main(String argv[]) {
        XMLReader saxReader = new GB_Csv2XmlReaderHtml();
        try {
            File f = new File("C:/gb/gb-commons/test/com/loribel/commons/xml/data/data.txt");
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            FileReader fr = new FileReader(f);
            URL l_url = GB_Csv2XmlTest.class.getResource("data/data.txt");
            InputStream is = l_url.openStream();
            InputSource inputSource = new InputSource(is);
            Source source = new SAXSource(saxReader, inputSource);
            StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty("indent", "yes");
            transformer.transform(source, result);
        } catch (TransformerConfigurationException tce) {
            System.out.println("\n** Transformer Factory error");
            System.out.println("   " + tce.getMessage());
            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            x.printStackTrace();
        } catch (TransformerException te) {
            System.out.println("\n** Transformation error");
            System.out.println("   " + te.getMessage());
            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            x.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void test_bidon() {
    }

    void todoTest_() throws Exception {
        GB_Csv2XmlReader t = new GB_Csv2XmlReaderAtt();
        InputSource l_source = GB_InputSourceBuilder.fromResource(GB_Csv2XmlTest.class, "data/data.txt");
        t.parse(l_source);
    }
}
