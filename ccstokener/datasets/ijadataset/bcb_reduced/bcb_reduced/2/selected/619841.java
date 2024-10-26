package org.lindenb.tinytools;

import java.io.Console;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.me.Me;
import org.lindenb.sw.vocabulary.Atom;
import org.lindenb.sw.vocabulary.DC;
import org.lindenb.sw.vocabulary.FOAF;
import org.lindenb.sw.vocabulary.RDF;
import org.lindenb.util.Base64;
import org.lindenb.util.Cast;
import org.lindenb.util.Compilation;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import static org.lindenb.xml.XMLUtilities.escape;

/**
 * TwitterOmics
 * @author pierre Lindenbaum PhD
 * plindenbaum@yahoo.fr
 * TODO:
 *  + iterate over the ATOM feeds
 *  + do not use memory storage
 *  + create a daemon
 */
public class TwitterOmics {

    /** default where the tweet should le sent */
    private String account = "biotecher";

    /** required hastag at the end of the tweet */
    private String hashtag = "interactome";

    /** tweeter login */
    private String login = null;

    /** tweeter password */
    private char[] password = null;

    /** all the persons' URI seen */
    private Set<String> seenFoaf = new HashSet<String>();

    /** all the ncbi taxonomy id seen */
    private Set<Integer> seenOrganism = new HashSet<Integer>();

    /** all the protein ncbi-gi seen */
    private Set<Integer> seenGi = new HashSet<Integer>();

    /** all the pubmed-id seen */
    private Set<Integer> seenPmid = new HashSet<Integer>();

    /** A pubmed record */
    private static class PubmedEntry {

        private int pmid;

        /** title of the article */
        private String title;

        PubmedEntry(int pmid, String title) {
            this.pmid = pmid;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /** an organism in the NCBI taxonomy */
    private static class Organism {

        int taxid;

        String name;

        Organism(int taxid, String name) {
            this.taxid = taxid;
            this.name = name;
        }
    }

    /** a protein in the NCBI protein database */
    private static class Protein {

        Organism organism;

        String name;

        int gi;

        Protein(int gi, String name, Organism organism) {
            this.gi = gi;
            this.name = name;
            this.organism = organism;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** a SAX Handler getting the name of an Article */
    private static class PMIDHandler extends DefaultHandler {

        private StringBuilder text = null;

        private int pmid = -1;

        private String title = null;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            text = null;
            if (name.equals("PMID") || name.equals("ArticleTitle")) {
                this.text = new StringBuilder();
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (pmid == -1 && name.equals("PMID")) {
                this.pmid = Integer.parseInt(this.text.toString());
            } else if (title == null && name.equals("ArticleTitle")) {
                this.title = this.text.toString();
            }
            text = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.text != null) text.append(ch, start, length);
        }
    }

    /** A Sax Handler getting the Organim and the title of a Sequence */
    private static class TinySeqHandler extends DefaultHandler {

        private StringBuilder text = null;

        private int TSeq_taxid = -1;

        private String TSeq_orgname = null;

        private String TSeq_defline = null;

        private String TSeq_seqtype = null;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            text = null;
            if (name.equals("TSeq_seqtype")) {
                this.TSeq_seqtype = attributes.getValue("value");
            } else if (name.equals("TSeq_taxid") || name.equals("TSeq_orgname") || name.equals("TSeq_defline")) {
                this.text = new StringBuilder();
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (name.equals("TSeq_taxid")) {
                this.TSeq_taxid = Integer.parseInt(this.text.toString());
            } else if (name.equals("TSeq_orgname")) {
                this.TSeq_orgname = this.text.toString();
            } else if (name.equals("TSeq_defline")) {
                this.TSeq_defline = this.text.toString();
            }
            text = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.text != null) text.append(ch, start, length);
        }
    }

    /** default constructor */
    private TwitterOmics() {
    }

    /** fetch the information about a given protein id */
    private Protein fetchProtein(int gi) throws IOException {
        String api_url = "http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=protein&id=" + gi + "&rettype=fasta&retmode=xml";
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(false);
        f.setValidating(false);
        try {
            SAXParser parser = f.newSAXParser();
            TinySeqHandler dh = new TinySeqHandler();
            parser.parse(api_url, dh);
            if (!"protein".equalsIgnoreCase(dh.TSeq_seqtype)) {
                System.err.println(api_url);
                return null;
            }
            Organism org = new Organism(dh.TSeq_taxid, dh.TSeq_orgname);
            return new Protein(gi, dh.TSeq_defline, org);
        } catch (ParserConfigurationException err) {
            throw new IOException(err);
        } catch (SAXException err) {
            throw new IOException(err);
        }
    }

    /** fetch the information about a pubmed entry */
    private PubmedEntry fetchPubmedEntry(int pmid) throws IOException {
        String api_url = "http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pmid;
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(false);
        f.setValidating(false);
        try {
            SAXParser parser = f.newSAXParser();
            PMIDHandler dh = new PMIDHandler();
            parser.parse(api_url, dh);
            return new PubmedEntry(pmid, dh.title);
        } catch (ParserConfigurationException err) {
            throw new IOException(err);
        } catch (SAXException err) {
            throw new IOException(err);
        }
    }

    /** export information about a twitter USER  to stdout / RDF */
    private void exportFoaf(String foafName, String foafURI) {
        if (this.seenFoaf.contains(foafURI)) return;
        seenFoaf.add(foafURI);
        System.out.println("<foaf:Person rdf:about=\"" + foafURI + "\">");
        System.out.println(" <foaf:name>" + escape(foafName) + "</foaf:name>");
        System.out.println("</foaf:Person>");
    }

    /** exports an organism  to stdout / RDF */
    private void exportOrganism(Organism org) {
        if (this.seenOrganism.contains(org.taxid)) return;
        seenOrganism.add(org.taxid);
        System.out.println("<Organism rdf:about=\"lsid:ncbi.nlm.nih.gov:taxonomy:" + org.taxid + "\">");
        System.out.println(" <taxId>" + org.taxid + "</taxId>");
        System.out.println(" <dc:title>" + escape(org.name) + "</dc:title>");
        System.out.println("</Organism>");
    }

    /** exports a protein to stdout/ RDF */
    private void exportGi(Protein prot) {
        if (this.seenGi.contains(prot.gi)) return;
        seenGi.add(prot.gi);
        exportOrganism(prot.organism);
        System.out.println("<Protein rdf:about=\"lsid:ncbi.nlm.nih.gov:protein:" + prot.gi + "\">");
        System.out.println(" <gi>" + prot.gi + "</gi>");
        System.out.println(" <dc:title>" + escape(prot.name) + "</dc:title>");
        System.out.println(" <organism rdf:resource=\"lsid:ncbi.nlm.nih.gov:taxonomy:" + prot.organism.taxid + "\"/>");
        System.out.println("</Protein>");
    }

    /** exports a protein to stdout using the BIBO ontology */
    private void exportPubmed(PubmedEntry pubmed) {
        if (this.seenPmid.contains(pubmed.pmid)) return;
        seenPmid.add(pubmed.pmid);
        System.out.println("<bibo:Article rdf:about=\"http://www.ncbi.nlm.nih.gov/pubmed/" + pubmed.pmid + "\">");
        System.out.println(" <bibo:pmid>" + pubmed.pmid + "</bibo:pmid>");
        System.out.println(" <dc:title>" + escape(pubmed.title) + "</dc:title>");
        System.out.println("</bibo:Article>");
    }

    /**
	 * Download the atom feed and exports it as RDF
	 * @throws IOException
	 * @throws XMLStreamException
	 */
    private void harvest() throws IOException, XMLStreamException {
        String api_url = "http://search.twitter.com/search.atom?q=+%23" + hashtag + "+to%3A" + account;
        System.err.println(api_url);
        URL url = new URL(api_url);
        URLConnection con = url.openConnection();
        String basic = this.login + ":" + new String(this.password);
        con.setRequestProperty("Authorization", "Basic " + Base64.encode(basic));
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLEventReader reader = factory.createXMLEventReader(con.getInputStream());
        boolean inEntry = false;
        boolean inAuthor = false;
        String published = null;
        String title = null;
        String foafName = null;
        String foafURI = null;
        String link = null;
        while (reader.hasNext()) {
            XMLEvent evt = reader.nextEvent();
            if (evt.isStartElement()) {
                StartElement e = evt.asStartElement();
                QName qName = e.getName();
                if (!inEntry && Atom.NS.equals(qName.getNamespaceURI()) && qName.getLocalPart().equals("entry")) {
                    inEntry = true;
                } else if (inEntry) {
                    String local = qName.getLocalPart();
                    if (local.equals("published")) {
                        published = reader.getElementText();
                    } else if (local.equals("title")) {
                        title = reader.getElementText();
                    } else if (link == null && local.equals("link")) {
                        Attribute att = e.getAttributeByName(new QName("type"));
                        if (att != null && att.getValue().equals("text/html")) {
                            att = e.getAttributeByName(new QName("href"));
                            if (att != null) {
                                link = att.getValue();
                            }
                        }
                    } else if (local.equals("author")) {
                        inAuthor = true;
                    } else if (inAuthor && local.equals("name")) {
                        foafName = reader.getElementText();
                    } else if (inAuthor && local.equals("uri")) {
                        foafURI = reader.getElementText();
                    }
                }
            } else if (evt.isEndElement()) {
                EndElement e = evt.asEndElement();
                QName qName = e.getName();
                if (inEntry && Atom.NS.equals(qName.getNamespaceURI())) {
                    String local = qName.getLocalPart();
                    if (local.equals("entry")) {
                        Protein p1 = null;
                        Protein p2 = null;
                        PubmedEntry pubmed = null;
                        boolean valid = title != null && published != null;
                        String tokens[] = title == null ? new String[0] : title.trim().split("[ \t\n\r]+");
                        if (valid && tokens.length != 5) {
                            System.err.println("Ignoring " + title);
                            valid = false;
                        }
                        if (valid && !tokens[0].equals("@" + account)) {
                            System.err.println("Ignoring " + title + " doesn't start with @" + account);
                            valid = false;
                        }
                        if (valid && !(tokens[1].startsWith("gi:") && Cast.Integer.isA(tokens[1].substring(3)))) {
                            System.err.println("Ignoring " + title + " not a gi:###");
                            valid = false;
                        }
                        if (valid && (p1 = fetchProtein(Integer.parseInt(tokens[1].substring(3)))) == null) {
                            valid = false;
                        }
                        if (valid && !(tokens[2].startsWith("gi:") && Cast.Integer.isA(tokens[2].substring(3)))) {
                            System.err.println("Ignoring " + title + " not a gi:###");
                            valid = false;
                        }
                        if (valid && (p2 = fetchProtein(Integer.parseInt(tokens[2].substring(3)))) == null) {
                            valid = false;
                        }
                        if (valid && !(tokens[3].startsWith("pmid:") && Cast.Integer.isA(tokens[3].substring(5)))) {
                            System.err.println("Ignoring " + title + " not a pmid:###");
                            valid = false;
                        }
                        if (valid && (pubmed = fetchPubmedEntry(Integer.parseInt(tokens[3].substring(5)))) == null) {
                            valid = false;
                        }
                        if (valid && !tokens[4].equals("#" + hashtag)) {
                            System.err.println("Ignoring " + title + " doesn't end with #" + hashtag);
                            valid = false;
                        }
                        if (valid && p1 != null && p2 != null && pubmed != null && foafName != null && foafURI != null) {
                            exportFoaf(foafName, foafURI);
                            exportGi(p1);
                            exportGi(p2);
                            exportPubmed(pubmed);
                            System.out.println("<Interaction rdf:about=\"" + link + "\">");
                            System.out.println(" <interactor rdf:resource=\"lsid:ncbi.nlm.nih.gov:protein:" + p1.gi + "\"/>");
                            System.out.println(" <interactor rdf:resource=\"lsid:ncbi.nlm.nih.gov:protein:" + p2.gi + "\"/>");
                            System.out.println(" <reference rdf:resource=\"http://www.ncbi.nlm.nih.gov/pubmed/" + pubmed.pmid + "\"/>");
                            System.out.println(" <dc:creator rdf:resource=\"" + foafURI + "\"/>");
                            System.out.println(" <dc:date>" + escape(published) + "</dc:date>");
                            System.out.println("</Interaction>");
                        }
                        inEntry = false;
                        title = null;
                        foafName = null;
                        foafURI = null;
                        inAuthor = false;
                        published = null;
                        link = null;
                    } else if (inAuthor && local.equals("author")) {
                        inAuthor = false;
                    }
                }
            }
        }
        reader.close();
    }

    public static void main(String[] args) {
        try {
            TwitterOmics app = new TwitterOmics();
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println("Pierre Lindenbaum PhD. " + Me.MAIL + " " + Me.WWW);
                    System.err.println(Compilation.getLabel());
                    System.err.println("-h this screen");
                    System.err.println("-u twitter login");
                    System.err.println("-p twitter password (will be asked if omitted)");
                    return;
                } else if (args[optind].equals("-u")) {
                    app.login = args[++optind];
                } else if (args[optind].equals("-p")) {
                    app.password = args[++optind].toCharArray();
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (app.login == null) {
                System.err.println("login missing");
                return;
            }
            if (app.password == null) {
                Console console = System.console();
                if (console == null) {
                    System.err.println("password missing");
                    return;
                }
                System.out.print("Password: ");
                app.password = console.readPassword();
            }
            System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            System.out.println("<rdf:RDF\nxmlns:foaf=\"" + FOAF.NS + "\"\nxmlns:bibo=\"http://purl.org/ontology/bibo/\"\nxmlns:rdf=\"" + RDF.NS + "\" xmlns:dc=\"" + DC.NS + "\"\nxmlns=\"http://twitteromics.lindenb.org\">");
            app.harvest();
            System.out.println("</rdf:RDF>");
            System.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
