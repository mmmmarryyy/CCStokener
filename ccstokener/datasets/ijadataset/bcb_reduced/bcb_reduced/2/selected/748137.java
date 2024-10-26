package net.sf.jabref.imports;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.OutputPrinter;

/**
 * Importer for the Refer/Endnote format.
 * 
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class MedlineImporter extends ImportFormat {

    private static Logger logger = Logger.getLogger(MedlineImporter.class.toString());

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
        return "Medline";
    }

    public String getCLIId() {
        return "medline";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        int i = 0;
        while (((str = in.readLine()) != null) && (i < 50)) {
            if (str.toLowerCase().indexOf("<pubmedarticle>") >= 0) return true;
            i++;
        }
        return false;
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     * 
     * @param id One or several ids, separated by ","
     * 
     * @return Will return an empty list on error.
     */
    public static List<BibtexEntry> fetchMedline(String id, OutputPrinter status) {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" + id;
        try {
            URL url = new URL(baseUrl);
            URLConnection data = url.openConnection();
            return new MedlineImporter().importEntries(data.getInputStream(), status);
        } catch (IOException e) {
            return new ArrayList<BibtexEntry>();
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List<BibtexEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);
        ArrayList<BibtexEntry> bibItems = null;
        try {
            SAXParser parser = parserFactory.newSAXParser();
            MedlineHandler handler = new MedlineHandler();
            parser.parse(stream, handler);
            if (false) {
                stream.reset();
                FileOutputStream out = new FileOutputStream(new File("/home/alver/ut.txt"));
                int c;
                while ((c = stream.read()) != -1) {
                    out.write((char) c);
                }
                out.close();
            }
            bibItems = handler.getItems();
        } catch (javax.xml.parsers.ParserConfigurationException e1) {
            logger.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
            status.showMessage(e1.getLocalizedMessage());
        } catch (org.xml.sax.SAXException e2) {
            logger.log(Level.SEVERE, e2.getLocalizedMessage(), e2);
            status.showMessage(e2.getLocalizedMessage());
        } catch (java.io.IOException e3) {
            logger.log(Level.SEVERE, e3.getLocalizedMessage(), e3);
            status.showMessage(e3.getLocalizedMessage());
        }
        return bibItems;
    }
}
