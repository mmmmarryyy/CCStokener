package org.lindenb.mwtools;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.me.Me;
import org.lindenb.util.Compilation;

/**
 * WPSearch
 * Author: Pierre Lindenbaum
 *  Perform a full text search
 */
public class WPSearch extends WPAbstractTool {

    /** namespaces in WP we are looking */
    private Set<Integer> srnamespaces = new HashSet<Integer>();

    /** private/empty cstor */
    private WPSearch() {
    }

    /**
	 * process query
	 * 
	 * @throws DatabaseException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
    private void process(int optind, String args[]) throws IOException, XMLStreamException {
        final int srlimit = 500;
        final QName att_title = new QName("title");
        final QName att_sroffset = new QName("sroffset");
        String sroffset = null;
        String srnamespace = null;
        if (!this.srnamespaces.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Integer i : this.srnamespaces) {
                if (sb.length() > 0) sb.append("|");
                sb.append(String.valueOf(i));
            }
            srnamespace = sb.toString();
        }
        StringBuilder terms = new StringBuilder();
        while (optind < args.length) {
            if (terms.length() > 0) terms.append(" ");
            terms.append(args[optind++]);
        }
        while (true) {
            String url = this.base_api + "?action=query" + "&list=search" + "&format=xml" + "&srsearch=" + URLEncoder.encode(terms.toString(), "UTF-8") + (srnamespace != null ? "&srnamespace=" + srnamespace : "") + (sroffset == null ? "" : "&sroffset=" + sroffset) + "&srlimit=" + srlimit + "&srwhat=text&srprop=timestamp";
            sroffset = null;
            LOG.info(url);
            XMLEventReader reader = this.xmlInputFactory.createXMLEventReader(openStream(url));
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    StartElement e = event.asStartElement();
                    String name = e.getName().getLocalPart();
                    Attribute att = null;
                    if (name.equals("p") && (att = e.getAttributeByName(att_title)) != null) {
                        System.out.println(att.getValue());
                    } else if (name.equals("search") && (att = e.getAttributeByName(att_sroffset)) != null) {
                        sroffset = att.getValue();
                    }
                }
            }
            reader.close();
            if (sroffset == null) break;
        }
    }

    public static void main(String[] args) {
        LOG.setLevel(Level.OFF);
        WPSearch app = new WPSearch();
        try {
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("Perform a full text search.");
                    System.err.println(Me.FIRST_NAME + " " + Me.LAST_NAME + " " + Me.MAIL + " " + Me.WWW);
                    System.err.println(" -log-level <java.util.logging.Level> default:" + LOG.getLevel());
                    System.err.println(" -api <url> default:" + app.base_api);
                    System.err.println(" -ns  mediawiki namespaces.");
                    System.err.println(" query terms");
                    return;
                } else if (args[optind].equals("-ns")) {
                    app.srnamespaces.add(Integer.parseInt(args[++optind]));
                } else if (args[optind].equals("-log-level")) {
                    LOG.setLevel(Level.parse(args[++optind]));
                } else if (args[optind].equals("-api")) {
                    app.base_api = args[++optind];
                } else if (args[optind].equals("--")) {
                    optind++;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("Unknown option " + args[optind]);
                } else {
                    break;
                }
                ++optind;
            }
            if (optind == args.length) {
                System.err.println("Query missing");
            } else {
                app.process(optind, args);
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
