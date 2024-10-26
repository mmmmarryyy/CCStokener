package org.lindenb.tinytools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import org.lindenb.me.Me;
import org.lindenb.sw.vocabulary.DC;
import org.lindenb.sw.vocabulary.RDF;
import org.lindenb.swing.SwingUtils;
import org.lindenb.swing.layout.InputLayout;
import org.lindenb.util.Cast;
import org.lindenb.util.Compilation;
import org.lindenb.util.StringUtils;

/**
 * 
 * QuoteEditor
 *
 */
public class QuoteEditor extends JFrame {

    private static final String NS = "urn:ontology:quotes:";

    private static final String PREFIX = "q";

    private static final long serialVersionUID = 1L;

    private JTextArea quoteArea;

    private JComboBox comboLang;

    private JTextField tfAuthor, tfSource, tfDate, tfKeywords, tfQuoteSource;

    private final String WP_PREFIX = "http://en.wikipedia.org/wiki/";

    private File rdfFile = null;

    private XMLInputFactory xmlInputFactory4WP;

    private static class Quote {

        String author = null;

        String source = null;

        String date = null;

        Set<String> keywords = new HashSet<String>();

        String quoteSource = null;

        String theQuote = null;

        String lang = null;

        void echo(XMLStreamWriter w) throws XMLStreamException {
            w.writeStartElement(PREFIX, "Quote", NS);
            w.writeStartElement(PREFIX, "quote", NS);
            if (!StringUtils.isBlank(lang)) {
                w.writeAttribute("xml", XMLConstants.XML_NS_URI, "lang", lang);
            }
            w.writeCharacters(theQuote);
            w.writeEndElement();
            w.writeEmptyElement(PREFIX, "author", NS);
            w.writeAttribute("rdf", RDF.NS, "resource", author);
            if (!StringUtils.isBlank(source)) {
                w.writeStartElement(PREFIX, "source", NS);
                w.writeCharacters(source);
                w.writeEndElement();
            }
            if (!StringUtils.isBlank(date)) {
                w.writeStartElement(PREFIX, "date", NS);
                w.writeCharacters(date);
                w.writeEndElement();
            }
            if (!StringUtils.isBlank(quoteSource)) {
                w.writeStartElement(PREFIX, "origin", NS);
                w.writeCharacters(quoteSource);
                w.writeEndElement();
            }
            for (String kw : keywords) {
                w.writeEmptyElement(PREFIX, "subject", NS);
                w.writeAttribute("rdf", RDF.NS, "resource", kw);
            }
            w.writeEndElement();
        }
    }

    private QuoteEditor() {
        super("Quote Editor");
        this.xmlInputFactory4WP = XMLInputFactory.newInstance();
        this.xmlInputFactory4WP.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        this.xmlInputFactory4WP.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        this.xmlInputFactory4WP.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                doMenuQuit();
            }
        });
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        JMenu menu = new JMenu("File");
        bar.add(menu);
        AbstractAction action = new AbstractAction("About") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JOptionPane.showMessageDialog(QuoteEditor.this, Me.FIRST_NAME + " " + Me.LAST_NAME + " " + Me.MAIL);
            }
        };
        menu.add(action);
        action = new AbstractAction("Quit") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                doMenuQuit();
            }
        };
        menu.add(action);
        menu = new JMenu("Quote");
        bar.add(menu);
        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        JPanel top = new JPanel(new InputLayout(2, 2));
        JLabel label;
        contentPane.add(top, BorderLayout.NORTH);
        top.add(label = new JLabel("Author:", JLabel.RIGHT));
        top.add(this.tfAuthor = new JTextField());
        labelFor(label, this.tfAuthor);
        top.add(label = new JLabel("Source:", JLabel.RIGHT));
        top.add(this.tfSource = new JTextField());
        labelFor(label, this.tfSource);
        top.add(label = new JLabel("Date:", JLabel.RIGHT));
        top.add(tfDate = new JTextField());
        labelFor(label, this.tfDate);
        top.add(label = new JLabel("Keywords:", JLabel.RIGHT));
        top.add(tfKeywords = new JTextField());
        labelFor(label, this.tfKeywords);
        top.add(label = new JLabel("Quote source:", JLabel.RIGHT));
        top.add(tfQuoteSource = new JTextField());
        labelFor(label, this.tfQuoteSource);
        top.add(label = new JLabel("Lang:", JLabel.RIGHT));
        top.add(comboLang = new JComboBox(new String[] { "fr", "en", "de", "it", "es" }));
        label.setLabelFor(comboLang);
        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new BevelBorder(BevelBorder.LOWERED));
        center.add(new JScrollPane(quoteArea = new JTextArea()));
        quoteArea.setWrapStyleWord(true);
        quoteArea.setLineWrap(true);
        quoteArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentPane.add(center, BorderLayout.CENTER);
        this.quoteArea.setFont(new Font("Dialog", Font.PLAIN, 36));
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        contentPane.add(bot, BorderLayout.SOUTH);
        action = new AbstractAction("Save and Next") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!save()) return;
                tfAuthor.setText("");
                tfDate.setText("");
                tfSource.setText("");
                tfKeywords.setText("");
                tfQuoteSource.setText("");
                quoteArea.setText("");
            }
        };
        bot.add(new JButton(action));
        menu.add(action);
        action = new AbstractAction("Save and Same Source") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!save()) return;
                tfKeywords.setText("");
                quoteArea.setText("");
            }
        };
        bot.add(new JButton(action));
        menu.add(action);
        action = new AbstractAction("Save and Same Author") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!save()) return;
                tfDate.setText("");
                tfSource.setText("");
                tfKeywords.setText("");
                tfQuoteSource.setText("");
                quoteArea.setText("");
            }
        };
        bot.add(new JButton(action));
        menu.add(action);
    }

    private void alert(Object msg) {
        JOptionPane.showMessageDialog(this, msg, "Message", JOptionPane.WARNING_MESSAGE, null);
    }

    private void doMenuQuit() {
        if (!this.quoteArea.getText().trim().isEmpty()) {
            if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(this, "Really close ?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null)) {
                return;
            }
        }
        this.setVisible(false);
        this.dispose();
    }

    private boolean save() {
        Quote q = new Quote();
        q.author = tfAuthor.getText().trim();
        if (q.author.isEmpty()) {
            alert("Empty Author");
            tfAuthor.requestFocus();
            return false;
        }
        q.source = tfSource.getText().trim();
        q.date = tfDate.getText().trim();
        q.quoteSource = tfQuoteSource.getText().trim();
        if (q.quoteSource.isEmpty()) {
            alert("Empty Quote Source");
            tfQuoteSource.requestFocus();
            return false;
        }
        q.theQuote = quoteArea.getText().trim();
        if (q.theQuote.isEmpty()) {
            alert("Empty Quote");
            quoteArea.requestFocus();
            return false;
        }
        String keywords = tfKeywords.getText().trim();
        if (keywords.isEmpty()) {
            alert("Empty Keywords");
            return false;
        }
        Set<String> tags = new HashSet<String>();
        try {
            StringTokenizer st = new StringTokenizer(keywords);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().toString().trim();
                if (tok.isEmpty() || tok.equals(",")) continue;
                tags.add(tok);
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert(String.valueOf(e.getMessage()));
            return false;
        }
        if (tags.isEmpty()) {
            alert("Empty Keywords");
            return false;
        }
        q.author = checkArticle(q.author, 1);
        if (q.author == null) {
            return false;
        }
        for (String s : tags) {
            String t = checkArticle(s, 14);
            if (t == null) return false;
            q.keywords.add(t);
        }
        if (this.comboLang.getSelectedIndex() != -1) {
            q.lang = this.comboLang.getSelectedItem().toString();
        }
        try {
            boolean echo_done = false;
            XMLOutputFactory xmlfactory = XMLOutputFactory.newInstance();
            if (this.rdfFile == null) {
                this.rdfFile = new File(System.getProperty("user.home"), "quotes.rdf");
            }
            if (!this.rdfFile.exists()) {
                this.alert("Created " + this.rdfFile);
                XMLStreamWriter w = xmlfactory.createXMLStreamWriter(new FileWriter(this.rdfFile));
                w.writeStartDocument("UTF-8", "1.0");
                w.writeStartElement("rdf", "RDF", RDF.NS);
                w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "rdf", RDF.NS);
                w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "dc", DC.NS);
                w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, PREFIX, NS);
                q.echo(w);
                echo_done = true;
                w.writeEndElement();
                w.writeEndDocument();
                w.flush();
                w.close();
            } else {
                File tmpFile = File.createTempFile("_quote", ".rdf", this.rdfFile.getParentFile());
                XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
                xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
                xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
                XMLEventReader reader = xmlInputFactory.createXMLEventReader(new StreamSource(this.rdfFile));
                XMLStreamWriter w = xmlfactory.createXMLStreamWriter(new FileWriter(tmpFile));
                w.writeStartDocument("UTF-8", "1.0");
                QName root = null;
                while (reader.hasNext()) {
                    XMLEvent evt = reader.nextEvent();
                    switch(evt.getEventType()) {
                        case XMLEvent.START_DOCUMENT:
                        case XMLEvent.END_DOCUMENT:
                            {
                                break;
                            }
                        case XMLEvent.END_ELEMENT:
                            {
                                EndElement e = evt.asEndElement();
                                QName name = e.getName();
                                if (name.getNamespaceURI().equals(RDF.NS) && name.getLocalPart().equals("RDF")) {
                                    echo_done = true;
                                    q.echo(w);
                                }
                                w.writeEndElement();
                                break;
                            }
                        case XMLEvent.START_ELEMENT:
                            {
                                StartElement e = evt.asStartElement();
                                QName name = e.getName();
                                if (root == null) {
                                    root = name;
                                    if (!(root.getNamespaceURI().equals(RDF.NS) && root.getLocalPart().equals("RDF"))) {
                                        throw new IOException("Expected a rdf:RDF as root but got " + root);
                                    }
                                }
                                XMLEvent next = reader.peek();
                                if (next != null && next.isEndElement()) {
                                    w.writeEmptyElement(name.getPrefix(), name.getLocalPart(), e.getNamespaceURI(name.getPrefix()));
                                    reader.nextEvent();
                                } else {
                                    w.writeStartElement(name.getPrefix(), name.getLocalPart(), e.getNamespaceURI(name.getPrefix()));
                                }
                                for (Iterator<?> it = e.getNamespaces(); it.hasNext(); ) {
                                    Attribute att = (Attribute) it.next();
                                    name = att.getName();
                                    w.writeNamespace(name.getLocalPart(), att.getValue());
                                }
                                for (Iterator<?> it = e.getAttributes(); it.hasNext(); ) {
                                    Attribute att = (Attribute) it.next();
                                    name = att.getName();
                                    w.writeAttribute(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), att.getValue());
                                }
                                break;
                            }
                        case XMLEvent.CHARACTERS:
                            {
                                w.writeCharacters(evt.asCharacters().getData());
                                break;
                            }
                        case XMLEvent.COMMENT:
                            {
                                w.writeComment(evt.asCharacters().getData());
                                break;
                            }
                        case XMLEvent.CDATA:
                            {
                                w.writeCData(evt.asCharacters().getData());
                                break;
                            }
                        default:
                            throw new XMLStreamException("type not handled :" + evt.getClass() + " " + evt.getEventType());
                    }
                }
                reader.close();
                w.flush();
                w.close();
                if (!rdfFile.delete()) {
                    alert("Couldn't replace " + rdfFile);
                    return false;
                }
                if (!tmpFile.renameTo(rdfFile)) {
                    alert("Couldn't replace " + tmpFile + " to " + rdfFile);
                    return false;
                }
            }
            if (!echo_done) {
                alert("IO problem. The current quote was not saved");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert(String.valueOf(e.getMessage()));
            return false;
        }
        return true;
    }

    String checkArticle(String name, int namespaceType) {
        String revid = null;
        InputStream in = null;
        try {
            String prefix = WP_PREFIX;
            if (namespaceType == 14) prefix += "Category:";
            if (name.equals(prefix)) {
                alert("just the WP prefix " + name);
                return null;
            }
            if (name.startsWith(prefix) || Cast.URL.isA(name)) return name;
            name = name.replace(' ', '_');
            if (namespaceType == 14 && !name.startsWith("Category:")) {
                name = "Category:" + name;
            }
            URL url = new URL("http://en.wikipedia.org/w/api.php?action=query&prop=revisions&format=xml&titles=" + URLEncoder.encode(name, "UTF-8"));
            URLConnection con = url.openConnection();
            con.setConnectTimeout(10000);
            in = con.getInputStream();
            XMLEventReader reader = this.xmlInputFactory4WP.createXMLEventReader(in);
            while (reader.hasNext()) {
                XMLEvent evt = reader.nextEvent();
                if (!evt.isStartElement()) continue;
                StartElement e = evt.asStartElement();
                String local = e.getName().getLocalPart();
                if (local.equals("page")) {
                    Attribute att = e.getAttributeByName(new QName("title"));
                    if (att != null) name = att.getValue();
                }
                if (local.equals("rev")) {
                    Attribute att = e.getAttributeByName(new QName("revid"));
                    if (att != null) revid = att.getValue();
                    break;
                }
            }
            reader.close();
            if (revid == null) {
                alert("Cannot find " + name + " in " + url);
                return null;
            }
            name = WP_PREFIX + name;
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            alert(String.valueOf(e.getMessage()));
            return null;
        } finally {
            if (in != null) try {
                in.close();
            } catch (Throwable err) {
            }
        }
    }

    private void labelFor(JLabel label, JTextComponent c) {
        label.setLabelFor(c);
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() < 2) return;
                JLabel label = (JLabel) me.getSource();
                ((JTextComponent) (label.getLabelFor())).setText("");
            }
        });
    }

    public static void main(String[] args) {
        try {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            int optind = 0;
            File source = null;
            while (optind < args.length) {
                if (args[optind].equals("-h") || args[optind].equals("-help") || args[optind].equals("--help")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("Options:");
                    System.err.println(" -h help; This screen.");
                    System.err.println(" -f <rdf-file> (optional)");
                    return;
                } else if (args[optind].equals("-f")) {
                    source = new File(args[++optind]);
                } else if (args[optind].equals("--")) {
                    optind++;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("Unknown option " + args[optind]);
                    return;
                } else {
                    break;
                }
                ++optind;
            }
            if (optind != args.length) {
                System.err.println("Illegal number of arguments.");
                return;
            }
            QuoteEditor ed = new QuoteEditor();
            if (source != null) ed.rdfFile = source;
            SwingUtils.center(ed, 300);
            SwingUtils.show(ed);
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
