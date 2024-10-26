package org.lindenb.tinytools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.io.IOUtils;
import org.lindenb.me.Me;
import org.lindenb.util.Compilation;
import org.lindenb.util.Digest;

/**
 * Finds protein interactors at two degrees of separation
 * output a graphviz dot picture
 * @author lindenb
 *
 */
public class EmblStrings {

    private XMLInputFactory inputFactory;

    /** an external source of candidate loaded in the 'main'
	 * with a p-value.
	 */
    private static class Candidate {

        String acn = null;

        String protName = null;

        double foldChange = 0.0;
    }

    /** all the candidates */
    private List<Candidate> candidates = new ArrayList<Candidate>();

    /** base class for the elements of the PSI file */
    private static class Base {

        /** getAttribute */
        protected String getAttribute(StartElement e, String name) {
            for (Iterator<?> iter = e.getAttributes(); iter.hasNext(); ) {
                Attribute att = (Attribute) iter.next();
                if (name.equals(att.getName().getLocalPart())) {
                    return att.getValue();
                }
            }
            return null;
        }

        /** skip the node and its children */
        protected void skip(XMLEventReader r, String localName) throws XMLStreamException {
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isEndElement() && evt.asEndElement().getName().getLocalPart().equals(localName)) {
                    return;
                }
            }
        }
    }

    /** description of an experiment */
    private static class ExperimentDescription extends Base {

        private Set<String> pmids = new HashSet<String>();

        /** parse the XML stream and fetch the PMID */
        ExperimentDescription(XMLEventReader r) throws XMLStreamException {
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isStartElement()) {
                    StartElement e = evt.asStartElement();
                    String localName = e.getName().getLocalPart();
                    if (localName.equals("primaryRef") || localName.equals("secondaryRef")) {
                        Attribute att = e.getAttributeByName(new QName("db"));
                        if (!"pubmed".equals(att.getValue())) continue;
                        this.pmids.add(e.getAttributeByName(new QName("id")).getValue());
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    if (e.getName().getLocalPart().equals("experimentDescription")) {
                        return;
                    }
                }
            }
        }
    }

    /** class about an interactor */
    private static class Interactor extends Base {

        private String shortLabel;

        private String fullName;

        private String primaryRef;

        private Set<String> names = new HashSet<String>();

        Interactor(XMLEventReader r) throws XMLStreamException {
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isStartElement()) {
                    StartElement e = evt.asStartElement();
                    String localName = e.getName().getLocalPart();
                    if (localName.equals("shortLabel")) {
                        this.shortLabel = r.getElementText();
                    } else if (localName.equals("fullName")) {
                        this.fullName = r.getElementText();
                    } else if (localName.equals("primaryRef")) {
                        this.primaryRef = getAttribute(e, "id");
                        if (this.primaryRef == null) throw new XMLStreamException("Cannot find @id in " + localName + " " + this.shortLabel);
                        this.names.add(primaryRef.toUpperCase());
                    } else if (localName.equals("secondaryRef")) {
                        String att = getAttribute(e, "refType");
                        if (att != null && "identity".equals(att)) {
                            this.names.add(getAttribute(e, "id").toUpperCase());
                        }
                    } else if (localName.equals("interactorType")) {
                        skip(r, localName);
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    if (e.getName().getLocalPart().equals("interactor")) {
                        return;
                    }
                }
            }
        }

        public String getPrimaryRef() {
            return primaryRef;
        }

        public String getFullName() {
            return fullName;
        }

        public String getShortLabel() {
            return shortLabel;
        }

        public Set<String> getNames() {
            return names;
        }

        /** return true if the this.names contains the string */
        public boolean hasName(String s) {
            return getNames().contains(s.toUpperCase());
        }

        /** two Interactors are the same if they share the same getPrimaryRef */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || this.getClass() != obj.getClass()) return false;
            boolean b = getPrimaryRef().equals(Interactor.class.cast(obj).getPrimaryRef());
            return b;
        }

        @Override
        public int hashCode() {
            return getPrimaryRef().hashCode();
        }

        @Override
        public String toString() {
            return getShortLabel() + " : " + getFullName();
        }
    }

    /** An interaction = 2 interactors */
    private static class Interaction extends Base {

        /** the two interactors */
        private Interactor interactors[] = new Interactor[] { null, null };

        /** the experiment associated */
        private ExperimentDescription experiment;

        /** confidence */
        private Double confidence = null;

        Interaction(Interactor i1, Interactor i2, ExperimentDescription experiment, Double confidence) {
            this.interactors[0] = i1;
            this.interactors[1] = i2;
            this.experiment = experiment;
            this.confidence = confidence;
        }

        /** constructor */
        Interaction(EntrySet entrySet, XMLEventReader r) throws XMLStreamException {
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isStartElement()) {
                    StartElement e = evt.asStartElement();
                    String localName = e.getName().getLocalPart();
                    if (localName.equals("interactorRef")) {
                        String iRef = r.getElementText();
                        Interactor i = entrySet.id2interactor.get(iRef);
                        if (i == null) throw new XMLStreamException("Cannot find interactor id" + iRef + " in " + entrySet.id2interactor.keySet());
                        this.interactors[this.interactors[0] == null ? 0 : 1] = i;
                    } else if (localName.equals("experimentRef")) {
                        String exp = r.getElementText();
                        this.experiment = entrySet.id2experiment.get(exp);
                        if (this.experiment == null) {
                            System.err.println("[WARNING]cannot get experiment id=" + exp + " ? ignoring this experiment");
                        }
                    } else if (localName.equals("confidenceList")) {
                        this.confidence = parseConfidence(r);
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    if (e.getName().getLocalPart().equals("interaction")) {
                        if (this.interactors[0] == null && this.interactors[1] == null) {
                            throw new XMLStreamException("interactors==null !");
                        }
                        return;
                    }
                }
            }
        }

        private Double parseConfidence(XMLEventReader r) throws XMLStreamException {
            Double confidence = null;
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isStartElement()) {
                    StartElement e = evt.asStartElement();
                    String localName = e.getName().getLocalPart();
                    if (localName.equals("value")) {
                        if (confidence != null) throw new XMLStreamException("confidence found twice");
                        confidence = Double.parseDouble(r.getElementText().trim());
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    if (e.getName().getLocalPart().equals("confidenceList")) {
                        return confidence;
                    }
                }
            }
            return confidence;
        }

        @Override
        public int hashCode() {
            return (this.interactors[0].hashCode() + this.interactors[1].hashCode()) % 3571;
        }

        public Interactor first() {
            return this.interactors[0].getPrimaryRef().compareTo(this.interactors[1].getPrimaryRef()) < 0 ? this.interactors[0] : this.interactors[1];
        }

        public Interactor second() {
            return this.interactors[0].getPrimaryRef().compareTo(this.interactors[1].getPrimaryRef()) < 0 ? this.interactors[1] : this.interactors[0];
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || this.getClass() != obj.getClass()) return false;
            Interaction cp = Interaction.class.cast(obj);
            return (first().equals(cp.first()) && second().equals(cp.second()));
        }

        public String pmidAsComma() {
            StringBuilder b = new StringBuilder();
            boolean f = false;
            if (this.experiment == null) return "";
            for (String e : this.experiment.pmids) {
                if (f) b.append(",");
                f = true;
                b.append(e);
            }
            return b.toString();
        }

        @Override
        public String toString() {
            return "(" + this.interactors[0].toString() + " / " + this.interactors[1] + ")";
        }
    }

    /** describe a PSI XML File downloaded from EMBL */
    private static class EntrySet {

        private Map<String, Interactor> id2interactor = new HashMap<String, Interactor>();

        private Map<String, Interaction> id2interaction = new HashMap<String, Interaction>();

        private Map<String, ExperimentDescription> id2experiment = new HashMap<String, ExperimentDescription>();

        EntrySet(XMLEventReader r) throws XMLStreamException {
            final QName idAtt = new QName("id");
            while ((r.hasNext())) {
                XMLEvent evt = r.nextEvent();
                if (evt.isStartElement()) {
                    String id = null;
                    StartElement e = evt.asStartElement();
                    String localName = e.getName().getLocalPart();
                    if (localName.equals("interactor")) {
                        id = e.getAttributeByName(idAtt).getValue();
                        if (id == null) throw new XMLStreamException("id missing");
                        id2interactor.put(id, new Interactor(r));
                    } else if (localName.equals("interaction")) {
                        id = e.getAttributeByName(idAtt).getValue();
                        if (id == null) throw new XMLStreamException("id missing");
                        id2interaction.put(id, new Interaction(this, r));
                    } else if (localName.equals("experimentDescription")) {
                        id = e.getAttributeByName(idAtt).getValue();
                        if (id == null) throw new XMLStreamException("id missing");
                        id2experiment.put(id, new ExperimentDescription(r));
                    }
                } else if (evt.isEndElement()) {
                    EndElement e = evt.asEndElement();
                    if (e.getName().getLocalPart().equals("entry")) {
                        return;
                    }
                }
            }
        }
    }

    /** constructor
	 * we need the input Factory
	 */
    private EmblStrings() {
        this.inputFactory = XMLInputFactory.newInstance();
        this.inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        this.inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        this.inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    }

    /** store the XML file on disk instead of downloading from the server each time */
    private boolean useCache = false;

    /** open a stream from a url, check before if it is stored on the disk */
    private InputStream open(String url) throws IOException {
        debug(url);
        if (!useCache) {
            return new URL(url).openStream();
        }
        File f = new File(System.getProperty("java.io.tmpdir", "."), Digest.SHA1.encrypt(url) + ".xml");
        debug("Cache : " + f);
        if (f.exists()) {
            return new FileInputStream(f);
        }
        InputStream in = new URL(url).openStream();
        OutputStream out = new FileOutputStream(f);
        IOUtils.copyTo(in, out);
        out.flush();
        out.close();
        in.close();
        return new FileInputStream(f);
    }

    /** download an entry set for this identifier */
    private EntrySet parse(String identifier) throws XMLStreamException, IOException {
        EntrySet set = null;
        String url = "http://string.embl.de/api/psi-mi/interactions?identifier=" + URLEncoder.encode(identifier, "UTF-8");
        InputStream in = open(url);
        XMLEventReader r = this.inputFactory.createXMLEventReader(in);
        while ((r.hasNext())) {
            XMLEvent evt = r.nextEvent();
            if (evt.isStartElement()) {
                StartElement e = evt.asStartElement();
                if (e.getName().getLocalPart().equals("entry")) {
                    if (set != null) throw new IOException("found two set !");
                    set = new EntrySet(r);
                }
            }
        }
        in.close();
        return set;
    }

    /** main node */
    private Interactor partner1 = null;

    /** all the interactions */
    private Set<Interaction> interactions = new HashSet<Interaction>();

    /** all the interactors */
    private Set<Interactor> interactors = new HashSet<Interactor>();

    private Interactor merge(Interactor i) {
        for (Interactor interactor : this.interactors) {
            if (interactor.equals(i)) {
                return interactor;
            }
        }
        this.interactors.add(i);
        return i;
    }

    /** build the network from the given query */
    private void build(String query) throws IOException, XMLStreamException {
        this.partner1 = null;
        EntrySet entry = parse(query);
        this.interactions.addAll(entry.id2interaction.values());
        this.interactors.addAll(entry.id2interactor.values());
        if (this.interactions.isEmpty()) return;
        for (Interactor i : this.interactors) {
            if (i.hasName(query)) {
                if (this.partner1 != null) throw new IOException("query found twice");
                this.partner1 = i;
            }
        }
        if (this.partner1 == null) {
            throw new IOException("Cannot find root " + query + " in " + this.interactors);
        }
        for (Interactor i : new HashSet<Interactor>(this.interactors)) {
            entry = parse(i.getPrimaryRef());
            Set<Interaction> newinteractions = new HashSet<Interaction>(entry.id2interaction.values());
            for (Interaction bind : newinteractions) {
                Interactor i1 = merge(bind.first());
                Interactor i2 = merge(bind.second());
                this.interactions.add(new Interaction(i1, i2, bind.experiment, bind.confidence));
            }
        }
    }

    /** output this result to dot */
    public void toDot(PrintStream out) {
        out.println("Graph G {");
        Map<Interactor, Integer> prot2id = new HashMap<Interactor, Integer>();
        int dotId = 0;
        for (Interactor interactor : this.interactors) {
            prot2id.put(interactor, ++dotId);
            String morelabel = "";
            for (Candidate candidate : this.candidates) {
                if (interactor.hasName(candidate.protName)) {
                    morelabel = " " + candidate.acn + " " + candidate.foldChange;
                    break;
                }
            }
            out.print("p" + prot2id.get(interactor) + "[label=\"" + interactor.getShortLabel() + " (" + interactor.getPrimaryRef() + ") " + morelabel + "\" ");
            if (this.partner1.equals(interactor)) {
                out.println("style=filled  shape=box  fillcolor=blue  ");
            } else if (morelabel.length() > 0) {
                out.println("style=filled  shape=pentagon  fillcolor=orange  ");
            }
            out.println("];");
        }
        for (Interaction link : this.interactions) {
            out.println("p" + prot2id.get(link.first()) + " -- p" + +prot2id.get(link.second()) + "[URL=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=retrieve&amp;db=pubmed&amp;list_uids=" + link.pmidAsComma() + "&amp;dopt=AbstractPlus\"," + "label=\"" + link.confidence + " pmid:" + link.pmidAsComma() + "\"];");
        }
        out.println("}");
    }

    private void debug(Object o) {
        System.err.println(String.valueOf(o));
    }

    /** read the candidate name acn/protName/p-value we got from a microaaray analysis*/
    private void readAltNames(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("#") && line.trim().length() == 0) continue;
            String tokens[] = line.split("[\t]");
            Candidate c = new Candidate();
            c.acn = tokens[0];
            c.protName = tokens[1];
            c.foldChange = Double.parseDouble(tokens[2]);
            for (int i = 0; i < candidates.size(); ++i) {
                Candidate c2 = this.candidates.get(i);
                if (c2.acn.equalsIgnoreCase(c.acn)) {
                    if (Math.abs(c2.foldChange) < Math.abs(c.foldChange)) {
                        this.candidates.set(i, c);
                    }
                    c = null;
                    break;
                }
            }
            if (c != null) this.candidates.add(c);
        }
        r.close();
    }

    public static void main(String[] args) {
        try {
            EmblStrings app = new EmblStrings();
            String root = null;
            int optind = 0;
            File output = null;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println("EmblString " + Me.FIRST_NAME + " " + Me.LAST_NAME + " " + Me.MAIL + " " + Me.WWW);
                    System.err.println("Finds protein-protein interaction up to 2 degrees of freedom on Embl-Strings ");
                    System.err.println("  -c use cache (default : false)");
                    System.err.println("  -o ou-file (default : stdout)");
                    System.err.println("  -f <file> read alternate names (name <tab> alt-name <tab> weight )");
                    System.err.println("  <root identifier>");
                    System.err.println(Compilation.getLabel());
                } else if (args[optind].equals("-c")) {
                    app.useCache = true;
                } else if (args[optind].equals("-o")) {
                    output = new File(args[++optind]);
                } else if (args[optind].equals("-f")) {
                    app.readAltNames(new File(args[++optind]));
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
            if (optind + 1 == args.length) {
                root = args[optind++];
            } else {
                System.err.println("illegal number of arguments");
                return;
            }
            if (root == null) {
                System.err.println("root protein missing");
                return;
            }
            app.build(root);
            PrintStream dot = System.out;
            if (output != null) {
                dot = new PrintStream(new FileOutputStream(output));
            }
            app.toDot(dot);
            dot.flush();
            if (output != null) {
                dot.close();
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
