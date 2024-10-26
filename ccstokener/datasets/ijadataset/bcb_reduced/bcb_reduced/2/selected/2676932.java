package org.lindenb.mwtools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.util.Compilation;

/**
 * WPCategories
 * Author: Pierre Lindenbaum
 * retrieves the categories of a given article  in wikipedia
 */
public class WPCategories extends WPAbstractTool {

    /** private/empty cstor */
    private WPCategories() {
    }

    /**
	 * process user
	 * 
	 * @throws DatabaseException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
    private void process(String entryName) throws IOException, XMLStreamException {
        final int cllimit = 500;
        final QName att_clcontinue = new QName("clcontinue");
        final QName att_title = new QName("title");
        String clcontinue = null;
        while (true) {
            String url = this.base_api + "?action=query" + "&prop=categories" + "&format=xml" + (clcontinue != null ? "&clcontinue=" + escape(clcontinue) : "") + "&titles=" + escape(entryName) + "&cllimit=" + cllimit;
            clcontinue = null;
            LOG.info(url);
            XMLEventReader reader = this.xmlInputFactory.createXMLEventReader(openStream(url));
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    StartElement e = event.asStartElement();
                    String name = e.getName().getLocalPart();
                    Attribute att = null;
                    if (name.equals("cl") && (att = e.getAttributeByName(att_title)) != null) {
                        System.out.println(entryName + "\t" + att.getValue());
                    } else if (name.equals("categories") && (att = e.getAttributeByName(att_clcontinue)) != null) {
                        clcontinue = att.getValue();
                    }
                }
            }
            reader.close();
            if (clcontinue == null) break;
        }
    }

    @Override
    protected void usage(PrintStream out) {
        System.err.println(Compilation.getLabel());
        System.err.println("Return categories about a given set of articles in wikipedia.");
        super.usage(out);
        System.err.println(" (stdin|articles-names)");
    }

    public static void main(String[] args) {
        LOG.setLevel(Level.OFF);
        WPCategories app = new WPCategories();
        try {
            int optind = app.processArgs(args);
            if (optind == args.length) {
                String line;
                LOG.info("read from stdin");
                java.io.BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                while ((line = r.readLine()) != null) {
                    app.process(line);
                }
                r.close();
            } else {
                while (optind < args.length) {
                    String fname = args[optind++];
                    LOG.info("opening " + fname);
                    app.process(fname);
                }
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
