package org.lindenb.mwtools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
 * WPUserStat
 * Author: Pierre Lindenbaum
 * retrieves informations about a given user's edits in wikipedia
 */
public class WPUserStat extends WPAbstractTool {

    /** namespaces */
    private Set<Integer> ucnamespaces = new HashSet<Integer>();

    /** use prefix */
    private boolean use_prefix = false;

    /** private/empty cstor */
    private WPUserStat() {
    }

    /**
	 * process user
	 * 
	 * @throws DatabaseException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
    private void process(String userName) throws IOException, XMLStreamException {
        final int uclimit = 500;
        final QName Attucstart = new QName("ucstart");
        String ucstart = null;
        String ucnamespace = null;
        if (!this.ucnamespaces.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Integer i : this.ucnamespaces) {
                if (sb.length() > 0) sb.append("|");
                sb.append(String.valueOf(i));
            }
            ucnamespace = sb.toString();
        }
        while (true) {
            String url = this.base_api + "?action=query" + "&list=usercontribs" + "&format=xml" + (ucnamespace == null ? "" : "&ucnamespace=" + ucnamespace) + (ucstart != null ? "&ucstart=" + escape(ucstart) : "") + (this.use_prefix ? "&ucuserprefix=" : "&ucuser=") + escape(userName) + "&uclimit=" + uclimit;
            ucstart = null;
            LOG.info(url);
            XMLEventReader reader = this.xmlInputFactory.createXMLEventReader(openStream(url));
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    StartElement e = event.asStartElement();
                    String name = e.getName().getLocalPart();
                    if (name.equals("item")) {
                        System.out.println(userName + "\t" + attr(e, "revid") + "\t" + attr(e, "pageid") + "\t" + attr(e, "ns") + "\t" + attr(e, "title") + "\t" + attr(e, "timestamp") + "\t" + attr(e, "comment") + "\t" + attr(e, "is_new") + "\t" + attr(e, "is_top") + "\t" + attr(e, "is_minor") + "\t");
                    } else if (name.equals("usercontribs")) {
                        Attribute clcont = e.getAttributeByName(Attucstart);
                        if (clcont != null) {
                            ucstart = clcont.getValue();
                        }
                    }
                }
            }
            reader.close();
            if (ucstart == null) break;
        }
    }

    public static void main(String[] args) {
        LOG.setLevel(Level.OFF);
        WPUserStat app = new WPUserStat();
        try {
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("Return informations about a given user in wikipedia.");
                    System.err.println(Me.FIRST_NAME + " " + Me.LAST_NAME + " " + Me.MAIL + " " + Me.WWW);
                    System.err.println(" -log-level <java.util.logging.Level> default:" + LOG.getLevel());
                    System.err.println(" -api <url> default:" + app.base_api);
                    System.err.println(" -p  Retrieve contibutions for all users whose names begin with this value.");
                    System.err.println(" -ns <int> restrict to given namespace default:all");
                    System.err.println(" (stdin|user-names)");
                    return;
                } else if (args[optind].equals("-ns")) {
                    app.ucnamespaces.add(Integer.parseInt(args[++optind]));
                } else if (args[optind].equals("-log-level")) {
                    LOG.setLevel(Level.parse(args[++optind]));
                } else if (args[optind].equals("-api")) {
                    app.base_api = args[++optind];
                } else if (args[optind].equals("-p")) {
                    app.use_prefix = true;
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
            System.out.println("#user\trevid" + "\t" + ("pageid") + "\t" + ("ns") + "\t" + ("title") + "\t" + ("timestamp") + "\t" + ("comment") + "\t" + ("is_new") + "\t" + ("is_top") + "\t" + ("is_minor"));
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
