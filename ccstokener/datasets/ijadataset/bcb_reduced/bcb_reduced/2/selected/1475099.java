package net.sf.imca.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Service for importing data into the IMCA.  This is mainly used by
 * test cases and to migrate data form the XML based site.  It is not 
 * to be used by the Web Application.
 *
 * @author dougculnane
 */
public class ImportData extends Service {

    public static String WEBSITE_PROTOCAL = "http";

    public static String WEBSITE_HOST = "www.moth-sailing.org";

    /**
     * This reads the xml data files for the moth web site and imports the data 
     * into the database.  It is useful for providing test data and to to 
     * import the data into the new database.
     *
     * @return success
     */
    public boolean setUpMasterData() {
        startTransaction();
        em.flush();
        this.endTransaction();
        return true;
    }

    /**
     * Small utility for extraction of node tree value from supplied data file 
     * name.
     */
    private NodeList getNodeListForDataFile(String fileName, String dataType) {
        NodeList list = null;
        try {
            URL url = new URL(WEBSITE_PROTOCAL, WEBSITE_HOST, "/" + fileName + ".xml");
            InputStream is = url.openStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document document = builder.parse(is);
            list = document.getElementsByTagName(dataType);
        } catch (SAXException e) {
            log.error("Error reading " + dataType + " data", e);
        } catch (IOException e) {
            log.error("Error reading " + dataType + " data", e);
        } catch (ParserConfigurationException e) {
            log.error("Error reading " + dataType + " data", e);
        }
        return list;
    }
}
