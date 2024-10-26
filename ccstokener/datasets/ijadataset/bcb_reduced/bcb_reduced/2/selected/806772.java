package org.nbrowse.io.database;

import net.n3.nanoxml.*;
import java.io.*;
import java.util.*;
import java.net.*;

/** <p><b>ParseXML:</b> Allows for reading and grab Taxonomy information from NCBI .</p>
  * this is not currently used in nb2 - but looks like it might be nice?
  */
public class ParseXML {

    IXMLElement tglbXML;

    public ParseXML() {
    }

    public void read(String xmlURL) throws Exception {
        URL xURL = new URL(xmlURL);
        read(xURL, null);
    }

    /** Reads data from a URL <tt>url</tt>, executing the <tt>afterReading</tt> Thread
     * after the data is read in.
     */
    public void read(URL url, Thread afterReading) throws Exception {
        URLConnection xmlConn = url.openConnection();
        SFSInputStream sfsInputStream = new SFSInputStream(xmlConn.getInputStream(), xmlConn.getContentLength());
        read(url.toString(), sfsInputStream, afterReading);
    }

    public void read(String fileName, InputStream in, Thread afterReading) throws Exception {
        InputStream xmlStream = in;
        IXMLParser parser = new StdXMLParser();
        parser.setBuilder(new StdXMLBuilder());
        parser.setValidator(new NonValidator());
        StdXMLReader reader = new StdXMLReader(xmlStream);
        parser.setReader(reader);
        tglbXML = null;
        try {
            tglbXML = (IXMLElement) parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        xmlStream.close();
    }

    private boolean getBooleanAttr(IXMLElement elt, String name, boolean def) {
        String value = elt.getAttribute(name, def ? "true" : "false");
        return value.toLowerCase().equals("true");
    }

    private String retrieveTaxonName() {
        IXMLElement eSummary = (IXMLElement) tglbXML.getChildrenNamed("DocSum").firstElement();
        Enumeration eSummaryEnum = (eSummary).enumerateChildren();
        IXMLElement eS = null;
        String taxonName = null;
        while (eSummaryEnum.hasMoreElements()) {
            eS = (IXMLElement) (eSummaryEnum.nextElement());
            String eSName = eS.getAttribute("Name", null);
            if (eSName != null && eSName.equals("ScientificName")) taxonName = eS.getContent();
        }
        return taxonName;
    }

    public String getTaxonName(String taxon_id) {
        try {
            read("http://www.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=taxonomy&id=" + taxon_id + "&retmode=xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retrieveTaxonName();
    }

    public static void main(String args[]) {
        ParseXML px = new ParseXML();
        System.out.println(px.getTaxonName(args[0]));
    }
}
