package org.lindenb.tool.oneshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.lindenb.io.PreferredDirectory;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.me.Me;
import org.lindenb.swing.ConstrainedAction;
import org.lindenb.swing.ObjectAction;
import org.lindenb.swing.SimpleDialog;
import org.lindenb.swing.table.GenericTableModel;
import org.lindenb.util.Compilation;
import org.lindenb.util.Debug;
import org.lindenb.util.TimeUtils;
import org.lindenb.util.XObject;
import org.lindenb.xml.XMLUtilities;

public class Pubmed2Wikipedia extends JFrame {

    private static final long serialVersionUID = 1L;

    /** wiki test */
    private JTextArea textArea;

    /** word patterns to make internal wikipedia links */
    private Vector<LinkPattern> patterns = new Vector<LinkPattern>();

    /** custom actions MAP */
    private ActionMap textActions = new ActionMap();

    /** prefs */
    private static final String PATTERN_FILE_PREF = "linkPatternFile";

    /** LinkPattern File */
    private File linkPatternFile = null;

    /**
 * got some problem with the DTD of the NCBI. This reader ignores the second line
 * of the returned XML
 * @author pierre
 *
 */
    private static class IgnoreLine2 extends Reader {

        Reader delegate;

        boolean found = false;

        IgnoreLine2(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int c = this.delegate.read();
            if (c == -1) return c;
            if (c == '\n' && !found) {
                while ((c = this.delegate.read()) != -1) {
                    if (c == '\n') break;
                }
                found = true;
                return this.read();
            }
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (found) return this.delegate.read(cbuf, off, len);
            int i = 0;
            while (i < len) {
                int c = read();
                if (c == -1) return (i == 0 ? -1 : i);
                cbuf[off + i] = (char) c;
                ++i;
            }
            return i;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /**
 * 
 * LinkPattern
 *
 */
    private static class LinkPattern {

        Pattern pattern;

        String page;

        LinkPattern(Pattern pattern, String page) {
            this.pattern = pattern;
            this.page = page;
        }

        LinkPattern(String pattern, String page) {
            this(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), page);
        }

        boolean match(String text) {
            return this.pattern.matcher(text).matches();
        }

        void toXML(PrintWriter out) {
            out.println("<Pattern regex=\"" + XMLUtilities.escape(this.pattern.pattern()) + "\">" + XMLUtilities.escape(this.page) + "</Pattern>");
        }
    }

    /**
 * 
 * MakeLinkAction
 *
 */
    private class MakeLinkAction extends ObjectAction<LinkPattern> {

        private static final long serialVersionUID = 1L;

        MakeLinkAction(LinkPattern lp) {
            super(lp, "[[" + lp.page + "]]");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String sel = textArea.getSelectedText();
            textArea.replaceRange("[[" + getObject().page + "|" + sel + "]]", textArea.getSelectionStart(), textArea.getSelectionEnd());
        }
    }

    private static class Author {

        String Suffix = "";

        String LastName = "";

        String FirstName = "";

        String MiddleName = "";

        String Initials = "";

        String Affiliation = "";

        @Override
        public String toString() {
            return FirstName + " " + LastName;
        }

        /** parse the &lt;Author&gt; tag */
        static Author parseAuthor(XMLEventReader reader) throws XMLStreamException {
            XMLEvent evt;
            Author author = new Author();
            while (!(evt = reader.nextEvent()).isEndDocument()) {
                if (evt.isEndElement() && evt.asEndElement().getName().getLocalPart().equals("Author")) {
                    return author;
                }
                if (!evt.isStartElement()) continue;
                String tag = evt.asStartElement().getName().getLocalPart();
                String content = reader.getElementText().trim();
                if (tag.equals("LastName")) {
                    author.LastName = content;
                } else if (tag.equals("FirstName") || tag.equals("ForeName")) {
                    author.FirstName = content;
                } else if (tag.equals("Initials")) {
                    author.Initials = content;
                } else if (tag.equals("MiddleName")) {
                    author.MiddleName = content;
                } else if (tag.equals("CollectiveName")) {
                    return null;
                } else if (tag.equals("Suffix")) {
                    author.Suffix = content;
                } else {
                    Debug.debug("###ignoring " + tag + "=" + content);
                }
            }
            throw new XMLStreamException("Cannot parse Author");
        }
    }

    /**
* <ref name='ReferenceID'> {{cite journal|title=Title|journal=Journal Name|date=2007-10-10|first=Pierre|last=Lindenbaum|coauthors=Maria Piron, Didier Poncet|volume=1000|issue=26|pages=1043-44|id=PMID 4964084 {{doi|10.1038/2151043a0}}|url=http://example.com|format=|accessdate=2007-10-31 }}</ref>
* @author lindenb
*
*/
    private static class Paper extends XObject {

        private String PMID = "";

        private String AbstractText = "";

        private String ArticleTitle = "";

        private String DOI = null;

        private Vector<Author> authors = new Vector<Author>(10, 1);

        private String Journal;

        private String Date;

        private String Volume;

        private String Issue;

        private String Pages;

        private HashSet<String> mesh = new HashSet<String>();

        Paper(XMLEventReader parser, StartElement startElement) throws XMLStreamException {
            while (parser.hasNext()) {
                XMLEvent event = parser.nextEvent();
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("PubmedArticle")) {
                    return;
                }
                if (event.isStartElement()) {
                    StartElement element = event.asStartElement();
                    if (element.getName().getLocalPart().equals("PMID")) {
                        this.PMID = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("DescriptorName") || element.getName().getLocalPart().equals("NameOfSubstance")) {
                        this.mesh.add(parser.getElementText());
                    } else if (element.getName().getLocalPart().equals("ArticleTitle")) {
                        this.ArticleTitle = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("AbstractText")) {
                        this.AbstractText = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("Volume")) {
                        this.Volume = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("Issue")) {
                        this.Issue = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("MedlineTA") || element.getName().getLocalPart().equals("ISOAbbreviation") || element.getName().getLocalPart().equals("Title")) {
                        if (this.Journal == null) this.Journal = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("MedlinePgn")) {
                        this.Pages = parser.getElementText();
                    } else if (element.getName().getLocalPart().equals("PubDate")) {
                        this.Date = parseDate(parser);
                    } else if (element.getName().getLocalPart().equals("Author")) {
                        Author a = Author.parseAuthor(parser);
                        if (a != null) {
                            authors.addElement(a);
                        }
                    } else if (element.getName().getLocalPart().equals("ArticleId")) {
                        Attribute att = element.getAttributeByName(new QName("IdType"));
                        if (att != null && att.getValue().equals("doi")) {
                            DOI = parser.getElementText();
                        }
                    }
                }
            }
        }

        /** parse the &lt;Author&gt; tag */
        private String parseDate(XMLEventReader reader) throws XMLStreamException {
            XMLEvent evt;
            StringBuilder date = new StringBuilder();
            while (!(evt = reader.nextEvent()).isEndDocument()) {
                if (evt.isEndElement() && evt.asEndElement().getName().getLocalPart().equals("PubDate")) {
                    return date.toString().trim();
                }
                if (!evt.isStartElement()) continue;
                String tag = evt.asStartElement().getName().getLocalPart();
                String content = reader.getElementText().trim();
                if (tag.equals("Year") || tag.equals("Month") || tag.equals("Day") || tag.equals("Season")) {
                    if (date.length() != 0) date.append("-");
                    date.append(" ").append(content);
                }
            }
            throw new XMLStreamException("Cannot parse Author");
        }

        @Override
        public boolean equals(Object obj) {
            return PMID.equals(Paper.class.cast(obj).PMID);
        }

        @Override
        public int hashCode() {
            return PMID.hashCode();
        }

        @Override
        public String toString() {
            return PMID;
        }

        public String asReference() {
            StringBuilder b = new StringBuilder("<ref name='pmid" + PMID + "'> {{cite journal");
            if (ArticleTitle != null) b.append("|title=" + ArticleTitle);
            if (Journal != null) b.append("|journal=" + Journal);
            if (Date != null) b.append("|date=" + Date);
            if (!authors.isEmpty()) {
                Author a = this.authors.firstElement();
                if (a.FirstName != null) b.append("|first=" + a.FirstName);
                if (a.LastName != null) b.append("|last=" + a.LastName);
            }
            if (authors.size() > 1) {
                b.append("|coauthors=");
                for (int i = 1; i < authors.size(); ++i) {
                    if (i > 1) b.append(", ");
                    Author a = this.authors.elementAt(i);
                    if (a.FirstName != null) b.append(a.FirstName);
                    if (a.LastName != null) b.append(" " + a.LastName);
                }
            }
            if (Volume != null) b.append("|volume=" + Volume);
            if (Issue != null) b.append("|issue=" + Issue);
            if (Pages != null) b.append("|pages=" + Pages);
            b.append("|id=" + PMID);
            if (DOI != null) {
                b.append(" {{doi|" + DOI + "}}");
            }
            b.append("|url=http://view.ncbi.nlm.nih.gov/pubmed/" + PMID);
            b.append("|accessdate=" + TimeUtils.toYYYYMMDD('-'));
            b.append("}}</ref>");
            return b.toString();
        }
    }

    /**
 * 
 * PaperTable
 *
 */
    private static class PaperTable extends GenericTableModel<Paper> {

        private static final long serialVersionUID = 1L;

        PaperTable() {
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "First Author";
                case 1:
                    return "Date";
                case 2:
                    return "Author";
                case 3:
                    return "Title";
                case 4:
                    return "Abstract";
            }
            return null;
        }

        @Override
        public Object getValueOf(Paper paper, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return (paper.authors.isEmpty() ? null : paper.authors.firstElement().toString());
                case 1:
                    return paper.Date;
                case 2:
                    return paper.Journal;
                case 3:
                    return paper.ArticleTitle;
                case 4:
                    return paper.AbstractText;
            }
            return null;
        }
    }

    /**
 * 
 * EFetchPane
 *
 */
    private static class EFetchPane extends SimpleDialog {

        private static final long serialVersionUID = 1L;

        /** query field */
        private JTextField queryField;

        /** center pane  needed d */
        private JTable nodeTable;

        private PaperTable papersModel;

        /** progress bar */
        private JProgressBar progressBar;

        /** maximum returned result */
        private JSpinner maxReturn;

        /** start returned result */
        private JSpinner retStart;

        /** thread looking at NCBi while progress is running */
        private Search theTread = null;

        private class Search extends Thread {

            String term;

            Vector<Paper> papers = new Vector<Paper>(100);

            String maxReturn = "100";

            String retstart = "0";

            Search(String term, String retstart, String maxReturn) {
                this.term = term;
                this.maxReturn = maxReturn;
                this.retstart = retstart;
            }

            public void run() {
                try {
                    EFetchPane.this.progressBar.setIndeterminate(true);
                    String term = queryField.getText().trim();
                    if (term.length() == 0) return;
                    URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed" + "&term=" + URLEncoder.encode(term, "UTF-8") + "&tool=pubmed2wikipedia" + "&email=plindenbaum_at_yahoo.fr" + "&retmode=xml&usehistory=y&retmax=" + maxReturn + "&retstart=" + retstart);
                    Debug.debug(url);
                    InputStream in = url.openStream();
                    XMLInputFactory factory = XMLInputFactory.newInstance();
                    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
                    factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
                    factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
                    XMLEventReader parser = factory.createXMLEventReader(in);
                    String QueryKey = null;
                    String WebEnv = null;
                    int idCount = 0;
                    while (parser.hasNext()) {
                        XMLEvent event = parser.nextEvent();
                        if (event.isStartElement()) {
                            StartElement element = event.asStartElement();
                            if (element.getName().getLocalPart().equals("QueryKey")) {
                                QueryKey = parser.getElementText().trim();
                            } else if (element.getName().getLocalPart().equals("WebEnv")) {
                                WebEnv = parser.getElementText().trim();
                            } else if (element.getName().getLocalPart().equals("Id")) {
                                ++idCount;
                            }
                        }
                    }
                    in.close();
                    if (QueryKey == null || WebEnv == null) {
                        throw new IOException("Cannot find QueryKey or WebEnv in " + url);
                    }
                    url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed" + "&rettype=full" + "&tool=pubmed2wikipedia" + "&email=plindenbaum_at_yahoo.fr" + "&retmode=xml" + "&WebEnv=" + WebEnv + "&query_key=" + QueryKey + "&retmode=xml&usehistory=y&retmax=" + maxReturn + "&retstart=" + retstart);
                    Debug.debug(url);
                    if (idCount > 0) {
                        in = url.openStream();
                        parser = factory.createXMLEventReader(new IgnoreLine2(new InputStreamReader(in)));
                        while (parser.hasNext()) {
                            XMLEvent event = parser.nextEvent();
                            if (event.isStartElement()) {
                                StartElement element = event.asStartElement();
                                if (element.getName().getLocalPart().equals("PubmedArticle")) {
                                    Paper p = new Paper(parser, element);
                                    papers.addElement(p);
                                }
                            }
                        }
                        in.close();
                    }
                    if (EFetchPane.this.theTread == this) {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                EFetchPane.this.setPapers(Search.this.papers);
                                EFetchPane.this.progressBar.setIndeterminate(false);
                            }

                            ;
                        });
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                    EFetchPane.this.progressBar.setIndeterminate(false);
                    ThrowablePane.show(EFetchPane.this, err);
                    Thread.currentThread().interrupt();
                }
            }
        }

        EFetchPane(Component owner) {
            super(owner, "Search Pubmed...");
            JPanel mainPane = new JPanel(new BorderLayout(5, 5));
            super.getContentPane().add(mainPane);
            mainPane.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1), new EmptyBorder(5, 5, 5, 5)));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
            mainPane.add(top, BorderLayout.NORTH);
            AbstractAction action = new AbstractAction("Search") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    doSearch();
                }
            };
            top.add(new JLabel("Search:", JLabel.RIGHT));
            top.add(this.queryField = new JTextField(Debug.isDebugging() ? "Rotavirus NSP4 Estes" : "", 20));
            this.queryField.addActionListener(action);
            this.queryField.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(DocumentEvent e) {
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                }
            });
            top.add(new JLabel("Limit:", JLabel.RIGHT));
            top.add(this.maxReturn = new JSpinner(new SpinnerNumberModel(Debug.isDebugging() ? 1 : 10, 1, 500, 1)));
            Dimension d = this.maxReturn.getPreferredSize();
            top.add(new JLabel("Start:", JLabel.RIGHT));
            top.add(this.retStart = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1)));
            this.retStart.setPreferredSize(d);
            JPanel center = new JPanel(new BorderLayout(2, 2));
            mainPane.add(center, BorderLayout.CENTER);
            center.add(new JScrollPane(nodeTable = new JTable(papersModel = new PaperTable()) {

                private static final long serialVersionUID = 1L;

                @Override
                public String getToolTipText(MouseEvent event) {
                    int r = rowAtPoint(event.getPoint());
                    if (r == -1) return null;
                    r = convertRowIndexToModel(r);
                    if (r == -1) return null;
                    int c = columnAtPoint(event.getPoint());
                    if (c == -1) return null;
                    c = convertColumnIndexToModel(c);
                    if (c == -1) return null;
                    Object o = getModel().getValueAt(r, c);
                    return (o == null ? null : "<html><body width=\"500\">" + XMLUtilities.escape(o.toString()) + "</body></html>");
                }
            }), BorderLayout.CENTER);
            nodeTable.setToolTipText("");
            ConstrainedAction<EFetchPane> rmAction = new ConstrainedAction<EFetchPane>(this, "Remove") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    int indexes[] = nodeTable.getSelectedRows();
                    for (int i = indexes.length - 1; i >= 0; --i) {
                        papersModel.removeElementAt(indexes[i]);
                    }
                }
            };
            rmAction.mustBeSelected(nodeTable);
            top.add(new JButton(action));
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            mainPane.add(bottom, BorderLayout.SOUTH);
            bottom.add(new JButton(rmAction));
            bottom.add(new JButton(new AbstractAction("Clear") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    papersModel.clear();
                }
            }));
            bottom.add(new JSeparator(JSeparator.VERTICAL));
            bottom.add(this.progressBar = new JProgressBar());
            getOKAction().mustHaveRows(nodeTable);
        }

        @Override
        protected void closeDialogWithStatus(int status) {
            if (this.theTread != null && this.theTread.isAlive()) {
                this.theTread.interrupt();
            }
            super.closeDialogWithStatus(status);
        }

        public void doSearch() {
            if (this.theTread != null) {
                this.theTread.interrupt();
            }
            this.theTread = new Search(queryField.getText().trim(), this.retStart.getValue().toString(), this.maxReturn.getValue().toString());
            this.theTread.start();
        }

        public void setPapers(Vector<Paper> papers) {
            for (Paper paper : papers) {
                if (papersModel.contains(paper)) continue;
                papersModel.addElement(paper);
            }
        }
    }

    /**
 * TextAction
 * @author pierre
 *
 */
    private abstract class TextAction extends AbstractAction {

        boolean selectionRequired;

        TextAction(String name, boolean selectionRequired, String iconUrl) {
            this(name);
            this.selectionRequired = selectionRequired;
            Icon icn = makeIcon(iconUrl);
            putValue(AbstractAction.SMALL_ICON, icn);
            putValue(AbstractAction.LARGE_ICON_KEY, icn);
            putValue(AbstractAction.SHORT_DESCRIPTION, name);
        }

        TextAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSelectionRequired() && textArea.getSelectionStart() == textArea.getSelectionEnd()) return;
            String text = textArea.getSelectedText();
            replace(text);
            textArea.requestFocus();
        }

        boolean isSelectionRequired() {
            return this.selectionRequired;
        }

        public void replace(String text) {
        }
    }

    private Pubmed2Wikipedia(Vector<Paper> papers) {
        super("Pubmed2Wikipedia");
        Preferences prefs = Preferences.userNodeForPackage(Pubmed2Wikipedia.class);
        String linkPatternStr = prefs.get(PATTERN_FILE_PREF, null);
        if (linkPatternStr != null) {
            loadLinkPattern(new File(linkPatternStr), true);
        }
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (linkPatternFile != null) {
                    saveLinkPattern(linkPatternFile, true);
                }
                Pubmed2Wikipedia.this.setVisible(false);
                Pubmed2Wikipedia.this.dispose();
            }
        });
        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        JToolBar top = new JToolBar();
        contentPane.add(top, BorderLayout.NORTH);
        JButton button;
        top.add(button = new JButton(addTextAction(new TextAction("Bold", true, "button_bold.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("'''" + text + "'''");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Italic", true, "button_italic.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("''" + text + "''");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Internal Link", true, "button_link.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("[[" + text.trim().replaceAll("[ \t]", "_") + "|" + text + "]]");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Level 2 Headline", false, "button_headline.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("\n== Headline text ==\n", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Embbeded Image", false, "button_image.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("[[Image:" + text.replaceAll("[ \t]", "_") + ".jpg]]");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Media Link", false, "button_media.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("[[Media:" + text.replaceAll("[ \t]", "_") + "]]");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Math (LaTeX)", false, "button_math.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("<math>Insert formula here</math>", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Ignore Wiki Formatting", false, "button_nowiki.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("<nowiki>Insert non-formatted text here</nowiki>", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Insert Your signature", false, "button_sig.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("--~~~~", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Horizontal Line", false, "button_hr.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("\n----\n", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Redirect", false, "Button_redirect.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("#REDIRECT [[" + text.replaceAll("[ \t]", "_") + "]]");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Strike", true, "Button_strike.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("<s>" + text + "</s>");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Line Break", false, "Button_enter.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("\n<br/>\n", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Comment", false, "Button_hide_comment.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("<!-- " + text + " -->");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Quote", false, "Button_blockquote.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void replace(String text) {
                textArea.replaceSelection("<quote>" + text + "</quote>");
            }
        })));
        button.setHideActionText(true);
        top.add(button = new JButton(addTextAction(new TextAction("Table", false, "Button_insert_table.png") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.insert("{| class=\"wikitable\"" + "|-" + "! header 1" + "! header 2" + "! header 3" + "|-" + "| row 1, cell 1" + "| row 1, cell 2" + "| row 1, cell 3" + "|-" + "| row 2, cell 1" + "| row 2, cell 2" + "| row 2, cell 3" + "|}", textArea.getCaretPosition());
                textArea.requestFocus();
            }
        })));
        button.setHideActionText(true);
        StringWriter sw = new StringWriter();
        if (papers != null) {
            TreeSet<String> categories = new TreeSet<String>();
            for (Paper paper : papers) {
                categories.addAll(paper.mesh);
                sw.append("\n\n");
                sw.append("<!-- " + paper.ArticleTitle + " " + paper.Date + " " + paper.Journal + " " + paper.authors + " -->\n");
                sw.append(paper.AbstractText.replaceAll("\\.[ ]", ".\n"));
                sw.append(paper.asReference());
                sw.append("\n<!--  -->\n");
                sw.append("\n\n");
            }
            sw.append("\n\n\n");
            sw.append("<div class='references-small'>\n<references/>\n</div>");
            sw.append("\n\n\n");
            sw.append("{{molecular-cell-biology-stub}}\n");
            sw.append("{{cell-biology-stub}}\n");
            sw.append("{{enzyme-stub}}\n");
            sw.append("{{med-stub}}\n");
            sw.append("\n\n\n");
            for (String s : categories) {
                sw.append("[[Category:" + s + "]]\n");
            }
        }
        this.textArea = new JTextArea(new PlainDocument());
        this.textArea.setLineWrap(true);
        this.textArea.setText(sw.toString());
        this.textArea.setCaretPosition(0);
        this.textArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                JPopupMenu menu = new JPopupMenu("Hello");
                if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                    for (Object key : textActions.keys()) {
                        Action a = textActions.get(key);
                        if (a instanceof TextAction) {
                            TextAction ta = TextAction.class.cast(a);
                            if (ta.isSelectionRequired()) {
                                menu.add(new JMenuItem(a));
                            }
                        }
                    }
                    menu.add(new JSeparator());
                    String sel = textArea.getSelectedText().trim();
                    for (LinkPattern lp : patterns) {
                        if (lp.match(sel)) {
                            menu.add(new JMenuItem(new MakeLinkAction(lp)));
                        }
                    }
                    menu.add(new JSeparator());
                    menu.add(new JMenuItem(new ObjectAction<String>(sel, "Add Pattern " + (sel.length() < 20 ? sel : sel.substring(0, 20)) + "...") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            LinkPattern p = createLinkPattern(getObject());
                            if (p != null) {
                                textArea.replaceRange("[[" + p.page + "|" + getObject() + "]]", textArea.getSelectionStart(), textArea.getSelectionEnd());
                            }
                        }
                    }));
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        this.textArea.getCaret().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                for (Object key : textActions.keys()) {
                    Action a = textActions.get(key);
                    if (a instanceof TextAction) {
                        TextAction ta = TextAction.class.cast(a);
                        if (ta.isSelectionRequired()) {
                            ta.setEnabled(textArea.getSelectionStart() != textArea.getSelectionEnd());
                        }
                    }
                }
            }
        });
        contentPane.add(new JScrollPane(this.textArea), BorderLayout.CENTER);
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        JMenu menu = new JMenu("File");
        bar.add(menu);
        menu.add(new JMenuItem(new AbstractAction("About...") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(Pubmed2Wikipedia.this, "<html><body>" + "<h1 align='center'>Pubmed2Wikipedia</h2>" + "<h2 align='center'>(c)Pierre Lindenbaum 2007 " + Me.MAIL + "</h2>" + "<h3 align='center'>" + Compilation.getLabel() + "</h3>" + "</body></html>", "About", JOptionPane.PLAIN_MESSAGE, null);
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Save Patterns...") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                saveLinkPattern();
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Load Patterns...") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                loadLinkPattern();
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Quit") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                Pubmed2Wikipedia.this.setVisible(false);
                Pubmed2Wikipedia.this.dispose();
            }
        }));
        menu = new JMenu("Format");
        bar.add(menu);
        for (Object key : textActions.keys()) {
            Action a = textActions.get(key);
            if (a instanceof TextAction) {
                menu.add(new JMenuItem(a));
            }
        }
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(100, 100, dim.width - 200, dim.height - 200);
    }

    private LinkPattern createLinkPattern(String sel) {
        sel = sel.trim();
        if (sel.trim().length() == 0) return null;
        JPanel pane = new JPanel(new GridLayout(2, 2));
        pane.setBorder(new TitledBorder("Make New Pattern..."));
        pane.add(new JLabel("Regular Expression", JLabel.RIGHT));
        JTextField f1 = new JTextField(sel, 20);
        pane.add(f1);
        pane.add(new JLabel("Link", JLabel.RIGHT));
        JTextField f2 = new JTextField(sel, 20);
        pane.add(f2);
        SimpleDialog dialog = new SimpleDialog(this, "Create New Pattern");
        dialog.getOKAction().mustBeARegexPattern(f1);
        dialog.getOKAction().mustNotEmpty(f2, true);
        dialog.getContentPane().add(pane);
        if (dialog.showDialog() != SimpleDialog.OK_OPTION) return null;
        try {
            LinkPattern p = new LinkPattern(f1.getText(), f2.getText().trim());
            this.patterns.addElement(p);
            return p;
        } catch (Exception err) {
            ThrowablePane.show(this, err);
            return null;
        }
    }

    private void saveLinkPattern(File file, boolean quiet) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(file));
            out.println("<?xml version='1.0' encoding='UTF-8'?>");
            out.println("<Patterns>");
            for (LinkPattern p : this.patterns) {
                p.toXML(out);
            }
            out.println("</Patterns>");
            out.flush();
            out.close();
            this.linkPatternFile = file;
            Preferences prefs = Preferences.userNodeForPackage(Pubmed2Wikipedia.class);
            prefs.put(PATTERN_FILE_PREF, file.toString());
            prefs.sync();
        } catch (Exception e) {
            if (!quiet) {
                ThrowablePane.show(this, e);
            }
        }
    }

    private Icon makeIcon(String name) {
        InputStream in = Pubmed2Wikipedia.class.getResourceAsStream("/images/" + name);
        try {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                in.close();
                return new ImageIcon(img, name);
            }
        } catch (Exception err) {
            Debug.debug(err);
        }
        Debug.debug(name);
        return new Icon() {

            @Override
            public int getIconHeight() {
                return 22;
            }

            @Override
            public int getIconWidth() {
                return 22;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, 22, 22);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, 21, 21);
            }
        };
    }

    private void saveLinkPattern() {
        try {
            JFileChooser chooser = new JFileChooser(PreferredDirectory.getPreferredDirectory());
            if (this.linkPatternFile != null) chooser.setSelectedFile(linkPatternFile);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File f = chooser.getSelectedFile();
            if (f == null || (f.exists() && JOptionPane.showConfirmDialog(this, f.toString() + " exists. Overwrite ?", "Overwrite ?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null) != JOptionPane.OK_OPTION)) {
                return;
            }
            saveLinkPattern(f, true);
        } catch (Exception err) {
            ThrowablePane.show(this, err);
        }
    }

    private void loadLinkPattern() {
        JFileChooser chooser = new JFileChooser();
        if (this.linkPatternFile != null) {
            chooser.setSelectedFile(this.linkPatternFile);
        } else {
            chooser.setSelectedFile(new File("patterns.xml"));
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        loadLinkPattern(f, false);
    }

    private AbstractAction addTextAction(AbstractAction action) {
        textActions.put(action.getValue(AbstractAction.NAME), action);
        return action;
    }

    private static Vector<Paper> askPubmed(Component owner) {
        EFetchPane dialog = new EFetchPane(owner);
        if (dialog.showDialog() != EFetchPane.OK_OPTION) {
            return null;
        }
        return dialog.papersModel.asVector();
    }

    private void loadLinkPattern(File file, boolean quiet) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            FileReader in = new FileReader(file);
            XMLEventReader parser = factory.createXMLEventReader(in);
            while (parser.hasNext()) {
                XMLEvent event = parser.nextEvent();
                if (event.isStartElement()) {
                    StartElement element = event.asStartElement();
                    if (element.getName().getLocalPart().equals("Pattern")) {
                        Attribute att = element.getAttributeByName(new QName("regex"));
                        if (att != null) {
                            String pattern = att.getValue();
                            String content = parser.getElementText().trim();
                            LinkPattern lp = new LinkPattern(pattern, content);
                            patterns.addElement(lp);
                        }
                    }
                }
            }
            in.close();
            this.linkPatternFile = file;
            Preferences prefs = Preferences.userNodeForPackage(Pubmed2Wikipedia.class);
            prefs.put(PATTERN_FILE_PREF, file.toString());
            prefs.sync();
            Collections.sort(patterns, new Comparator<LinkPattern>() {

                @Override
                public int compare(LinkPattern o1, LinkPattern o2) {
                    return o1.page.compareTo(o2.page);
                }
            });
        } catch (Exception err) {
            if (!quiet) {
                ThrowablePane.show(this, err);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Debug.setDebugging(false);
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("\t-h this screen");
                    System.err.println("\t-d turns debugging on");
                    return;
                } else if (args[optind].equals("-d")) {
                    Debug.setDebugging(true);
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    throw new IllegalArgumentException("Unknown option " + args[optind]);
                } else {
                    break;
                }
                ++optind;
            }
            if (optind != args.length) {
                throw new IllegalArgumentException("Too many arguments");
            }
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    Vector<Paper> papers = askPubmed(null);
                    if (papers != null && !papers.isEmpty()) {
                        Collections.reverse(papers);
                        Pubmed2Wikipedia frame = new Pubmed2Wikipedia(papers);
                        frame.setVisible(true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
