package org.lindenb.tinytools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.lindenb.io.IOUtils;
import org.lindenb.swing.SwingUtils;
import org.lindenb.util.Cast;
import org.lindenb.util.Compilation;
import org.lindenb.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

public class FlickrFave {

    private static Logger LOG = Logger.getLogger(FlickrFave.class.getName());

    private static final String FLICKR = "urn:flickr:";

    private static Model MODEL4PROPERTIES = ModelFactory.createDefaultModel();

    private static Resource isChannel = MODEL4PROPERTIES.createResource(FLICKR + "Channel");

    private static Resource rdfRoot = MODEL4PROPERTIES.createResource(FLICKR + "root");

    private static Property isRDFRoot = MODEL4PROPERTIES.createProperty(FLICKR, "Root");

    private static Property lastRead = MODEL4PROPERTIES.createProperty(FLICKR, "lastRead");

    private static Property isBookmark = MODEL4PROPERTIES.createProperty(FLICKR, "Bookmark");

    private static Property thumb = MODEL4PROPERTIES.createProperty(FLICKR, "thumb");

    private static Property image = MODEL4PROPERTIES.createProperty(FLICKR, "image");

    private class Item {

        String title = null;

        String link = null;

        String description = null;

        String imageURL = null;

        String largeImageURL;

        Date date;

        @Override
        public boolean equals(Object obj) {
            return link.equals(Item.class.cast(obj).link);
        }

        @Override
        public int hashCode() {
            return (this.link == null ? -1 : this.link.hashCode());
        }

        @Override
        public String toString() {
            return title + "\n" + link + "\n" + imageURL + "\n" + dateFormat.format(date);
        }
    }

    private class ItemAdapter extends MouseAdapter {

        Item item;

        ItemAdapter(Item item) {
            this.item = item;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!Desktop.isDesktopSupported() || this.item.link == null || !e.isPopupTrigger()) return;
            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.add(new AbstractAction("Show in Browser") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    try {
                        Desktop.getDesktop().browse(URI.create(item.link));
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Cannot open browser");
                    }
                }
            });
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
 * RssHandler
 * @author pierre
 *
 */
    private class RssHandler extends DefaultHandler {

        StringBuilder content = new StringBuilder();

        Item item = null;

        private HashSet<Item> items = new HashSet<Item>();

        private Date lastDate;

        private Date toDate;

        public RssHandler(Date lastDate, Date toDate) {
            this.lastDate = lastDate;
            this.toDate = toDate;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            this.content.setLength(0);
            if (localName.equals("item") || localName.equals("entry")) {
                item = new Item();
            } else if (localName.equals("thumbnail") && item != null) {
                item.imageURL = attributes.getValue("url");
                LOG.info("found imageURL:" + item.imageURL);
            } else if (localName.equals("link") && item != null) {
                if (item.imageURL == null && "enclosure".equals(attributes.getValue("rel"))) {
                    item.imageURL = attributes.getValue("href");
                    LOG.info("found imageURL:" + item.link);
                }
                if (item.link == null && "alternate".equals(attributes.getValue("rel"))) {
                    item.link = attributes.getValue("href");
                    LOG.info("found link:" + item.link);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if ((localName.equals("item") || localName.equals("entry")) && item != null) {
                LOG.info("item " + item.imageURL + " " + item.date + " " + this.lastDate + " " + item.date + " " + (this.lastDate.compareTo(item.date) < 0));
                if (item.imageURL != null && item.date != null && this.lastDate.compareTo(item.date) < 0 && (this.toDate == null || (this.toDate != null && item.date.compareTo(this.toDate) <= 0))) {
                    LOG.info("ok date");
                    if (this.item.largeImageURL == null) {
                        this.item.largeImageURL = this.item.imageURL;
                    }
                    Resource subject = getModel().createResource(item.link);
                    if (!getModel().containsResource(subject)) {
                        LOG.info("Adding " + item);
                        this.items.add(item);
                    } else {
                        LOG.info("Model already contains " + subject);
                    }
                }
                item = null;
            } else if (localName.equals("item") || localName.equals("entry")) {
                LOG.info("failure");
            } else if (name.equals("title") && item != null) {
                item.title = this.content.toString();
            } else if (name.equals("link") && item != null) {
                if (item.link == null) item.link = this.content.toString();
            } else if (name.equals("description") && item != null) {
                String s = this.content.toString();
                ;
                int i = s.indexOf("src=\"");
                if (i != -1) {
                    i += 5;
                    int j = s.indexOf('\"', i + 1);
                    if (j != -1) {
                        item.largeImageURL = s.substring(i, j);
                    }
                }
            } else if (localName.equals("date.Taken") && DC.NS.equals(uri) && item != null) {
                try {
                    this.item.date = dateFormat.parse(this.content.toString());
                } catch (ParseException e) {
                    this.item.date = null;
                }
            }
            this.content.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.item != null) {
                this.content.append(ch, start, length);
            }
        }
    }

    class Slideshow extends JDialog {

        private static final long serialVersionUID = 1L;

        private Vector<Item> items;

        private int imageIndex = 0;

        private AbstractAction prev;

        private AbstractAction next;

        private JLabel label;

        Slideshow(Vector<Item> items) {
            super((JFrame) null, "Slideshow", true);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            this.items = items;
            this.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    Slideshow.this.setVisible(false);
                    Slideshow.this.dispose();
                }
            });
            this.next = new AbstractAction("Next") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    setImageIndex(imageIndex + 1);
                }
            };
            this.prev = new AbstractAction("Prev") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    setImageIndex(imageIndex - 1);
                }
            };
            JPanel top = new JPanel(new BorderLayout());
            top.add(new JButton(this.prev), BorderLayout.WEST);
            top.add(new JButton(this.next), BorderLayout.EAST);
            JPanel content = new JPanel(new BorderLayout());
            setContentPane(content);
            content.add(top, BorderLayout.NORTH);
            JScrollBar bar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, items.size() - 1);
            top.add(bar);
            bar.addAdjustmentListener(new AdjustmentListener() {

                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    setImageIndex(e.getValue());
                }
            });
            this.label = new JLabel();
            this.label.setOpaque(true);
            this.label.setBackground(Color.BLACK);
            this.label.setHorizontalAlignment(JLabel.CENTER);
            content.add(new JScrollPane(this.label), BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            content.add(bottom, BorderLayout.SOUTH);
            bottom.add(new JButton(new AbstractAction("Open URL") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!Desktop.isDesktopSupported()) return;
                    try {
                        Desktop.getDesktop().browse(URI.create(Slideshow.this.items.elementAt(imageIndex).link));
                    } catch (Exception e2) {
                        JOptionPane.showMessageDialog(Slideshow.this, e2.getMessage());
                    }
                }
            }));
            bottom.add(new JButton(new AbstractAction("Close") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    Slideshow.this.setVisible(false);
                }
            }));
            SwingUtils.center(this, 100);
            setImageIndex(0);
        }

        private void setImageIndex(int index) {
            try {
                Item item = this.items.elementAt(index);
                String imageURL = item.largeImageURL;
                imageURL = imageURL.replace("_m.", ".");
                imageURL = imageURL.replace("_s.", ".");
                InputStream io = openStream(imageURL);
                BufferedImage img = ImageIO.read(io);
                io.close();
                setTitle(imageURL);
                next.setEnabled(index + 1 < this.items.size());
                prev.setEnabled(index - 1 >= 0);
                this.label.setIcon(new ImageIcon(img));
                Dimension d = new Dimension(img.getWidth(), img.getHeight());
                this.label.setPreferredSize(d);
                this.label.setSize(d);
                this.imageIndex = index;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** date format transform */
    private SimpleDateFormat dateFormat;

    /** rdf store */
    private Model model;

    /** file */
    private File file;

    /** saxParser */
    private SAXParser parser;

    /** cookie_session */
    private String cookie_session = null;

    /**
	 * FlickrFave
	 * @param model
	 * @param file
	 * @throws Exception
	 */
    private FlickrFave(Model model, File file) throws Exception {
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        this.model = model;
        this.file = file;
        this.model.setNsPrefix("flickr", FLICKR);
        this.model.setNsPrefix("dc", DC.NS);
        this.model.add(rdfRoot, RDF.type, isRDFRoot);
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(true);
        f.setValidating(false);
        this.parser = f.newSAXParser();
    }

    private void save() throws IOException {
        cout().print("Saving..");
        FileOutputStream out = new FileOutputStream(this.file);
        getModel().write(out);
        out.flush();
        out.close();
        cout().println(" Done.");
    }

    private PrintStream cout() {
        return System.out;
    }

    private Model getModel() {
        return model;
    }

    private Statement selectOneStatement(Resource s, Property p, RDFNode o) {
        Statement stmt = null;
        StmtIterator r = getModel().listStatements(s, p, o);
        while (r.hasNext()) {
            stmt = r.nextStatement();
            break;
        }
        r.close();
        return stmt;
    }

    private void showSlideShow() throws IOException {
        Vector<Item> items = new Vector<Item>();
        ResIterator iter = getModel().listResourcesWithProperty(RDF.type, isBookmark);
        while (iter.hasNext()) {
            Resource link = iter.nextResource();
            Item item = new Item();
            item.link = link.getURI();
            Statement stmt = selectOneStatement(link, DC.title, null);
            item.title = (stmt == null ? item.link : stmt.getLiteral().getString());
            stmt = selectOneStatement(link, thumb, null);
            item.imageURL = (stmt == null ? item.link : stmt.getResource().getURI());
            stmt = selectOneStatement(link, image, null);
            item.largeImageURL = (stmt == null ? item.link : stmt.getResource().getURI());
            items.add(item);
        }
        if (items.isEmpty()) return;
        Collections.shuffle(items);
        Slideshow show = new Slideshow(items);
        SwingUtils.show(show);
    }

    private boolean addSubscription(String url) {
        if (!Cast.URL.isA(url)) {
            cout().print("No a URL: " + url);
            return false;
        }
        Resource subject = getModel().createResource(url);
        if (getModel().containsResource(subject)) {
            cout().print("Model already contains :" + url);
            return false;
        }
        getModel().add(subject, RDF.type, isChannel);
        getModel().add(subject, DC.date, this.dateFormat.format(new Date()));
        return true;
    }

    private void addBookmark(String url) {
        if (!Cast.URL.isA(url)) {
            cout().print("No a URL: " + url);
            return;
        }
        try {
            final String suffix = "/in/photostream/";
            if (url.endsWith(suffix)) {
                url = url.substring(0, url.length() - suffix.length());
            }
            String title = null;
            String src = null;
            InputStream io = openStream(url);
            String html = IOUtils.getReaderContent(new InputStreamReader(io));
            io.close();
            int i = -1;
            while ((i = html.indexOf("<img", i + 1)) != -1) {
                i += 3;
                title = null;
                src = null;
                int j = html.indexOf(">", i);
                if (j == -1) continue;
                String tag = html.substring(i, j);
                String tokens[] = tag.split("[ \t\"\'\n=]+");
                for (int k = 0; k + 1 < tokens.length; ++k) {
                    if (tokens[k].equals("src") && tokens[k + 1].startsWith("http://farm") && tokens[k + 1].contains(".static.flickr.com/")) {
                        src = tokens[k + 1];
                        int m = src.indexOf('?');
                        if (m != -1) src = src.substring(0, m);
                    } else if (tokens[k].equals("alt")) {
                        title = tokens[k + 1];
                    }
                }
                if (title != null && src != null) {
                    Resource subject = getModel().createResource(url);
                    cout().printf(title);
                    if (getModel().containsResource(subject)) {
                        cout().println("Already in Model");
                        continue;
                    }
                    getModel().add(subject, RDF.type, isBookmark).add(subject, DC.title, title).add(subject, DC.date, this.dateFormat.format(new Date())).add(subject, thumb, getModel().createResource(src)).add(subject, image, getModel().createResource(src));
                    return;
                }
            }
            cout().println("Failure");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private InputStream openStream(String uri) throws IOException {
        URL url = new URL(uri);
        URLConnection conn = url.openConnection();
        if (this.cookie_session != null) {
            conn.setRequestProperty("Cookie", "cookie_session=" + cookie_session);
        }
        return conn.getInputStream();
    }

    private void parse(SAXParser parse, String uri, DefaultHandler dh) throws IOException, SAXException {
        InputStream in = openStream(uri);
        parse.parse(in, dh);
        in.close();
    }

    private void readFeeds(Date lastDate, Date toDate) throws IOException, SAXException, ParseException {
        if (lastDate == null) {
            lastDate = new Date(1L);
            NodeIterator r = getModel().listObjectsOfProperty(rdfRoot, lastRead);
            while (r.hasNext()) {
                RDFNode node = r.nextNode();
                if (!node.isLiteral()) continue;
                lastDate = this.dateFormat.parse(Literal.class.cast(node).getString());
                break;
            }
            r.close();
        }
        RssHandler handler = new RssHandler(lastDate, toDate);
        ResIterator iter = getModel().listSubjectsWithProperty(RDF.type, isChannel);
        while (iter.hasNext()) {
            Resource subject = iter.nextResource();
            try {
                parse(this.parser, subject.getURI(), handler);
            } catch (IOException err) {
                cout().println("Error " + err.getMessage() + ":" + subject.getURI());
            } catch (SAXParseException err) {
                cout().println("Error " + err.getMessage() + ":" + subject.getURI());
            }
        }
        iter.close();
        if (handler.items.isEmpty()) {
            cout().println("Nothing new.");
            return;
        }
        Vector<Pair<Item, JToggleButton>> item2button = new Vector<Pair<Item, JToggleButton>>();
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel pane = new JPanel(new GridLayout(0, 6, 5, 5));
        for (Item item : handler.items) {
            InputStream io = null;
            BufferedImage icon = null;
            try {
                LOG.info("reading " + item.imageURL);
                io = openStream(item.imageURL);
                icon = ImageIO.read(io);
            } catch (Exception err) {
                err.printStackTrace();
                continue;
            } finally {
                IOUtils.safeClose(io);
            }
            JToggleButton button = new JToggleButton(new ImageIcon(icon));
            pane.add(button);
            button.addMouseListener(new ItemAdapter(item));
            button.setToolTipText(item.title);
            item2button.add(new Pair<Item, JToggleButton>(item, button));
        }
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(600, 300));
        contentPanel.add(scroll, BorderLayout.CENTER);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        contentPanel.setPreferredSize(new Dimension(screen.width - 200, screen.height - 200));
        JOptionPane.showMessageDialog(null, contentPanel);
        for (Pair<Item, JToggleButton> pair : item2button) {
            if (!pair.second().isSelected()) continue;
            Resource subject = getModel().createResource(pair.first().link);
            cout().printf(pair.first().title);
            if (getModel().containsResource(subject)) {
                cout().println("Already in Model");
                continue;
            }
            getModel().add(subject, RDF.type, isBookmark).add(subject, DC.title, pair.first().title).add(subject, DC.date, this.dateFormat.format(new Date())).add(subject, thumb, getModel().createResource(pair.first().imageURL)).add(subject, image, getModel().createResource(pair.first().largeImageURL));
        }
        StmtIterator iter2 = getModel().listStatements(rdfRoot, lastRead, (RDFNode) null);
        while (iter2.hasNext()) {
            iter2.next();
            iter2.remove();
        }
        iter2.close();
        getModel().add(rdfRoot, lastRead, this.dateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        try {
            LOG.setLevel(Level.OFF);
            JDialog.setDefaultLookAndFeelDecorated(true);
            int optind = 0;
            String filename = null;
            java.sql.Date fromDate = null;
            java.sql.Date toDate = null;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println(" -h this screen");
                    System.err.println(" -m <file> for this model");
                    System.err.println(" -d YYYY-MM-DD from date");
                    System.err.println(" -t YYYY-MM-DD to date");
                    return;
                } else if (args[optind].equals("-m")) {
                    filename = args[++optind];
                } else if (args[optind].equals("-d")) {
                    fromDate = java.sql.Date.valueOf(args[++optind]);
                } else if (args[optind].equals("-t")) {
                    toDate = java.sql.Date.valueOf(args[++optind]);
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
            Model model = ModelFactory.createDefaultModel();
            File file;
            if (filename == null) {
                file = new File(System.getProperty("user.home"), ".flickrfave.rdf");
                filename = file.toString();
            } else {
                file = new File(filename);
            }
            if (!file.exists()) {
                System.err.println(file + " does not exists");
            } else {
                if (!file.isFile()) {
                    System.err.println(file + ": not a regular file");
                    return;
                }
                model.read(FileUtils.toURL(filename));
            }
            FlickrFave app = new FlickrFave(model, file);
            try {
                File flickrProps = new File(System.getProperty("user.home", "."), ".flickr.xml");
                if (flickrProps.exists()) {
                    Properties prop = new Properties();
                    FileInputStream fin = new FileInputStream(flickrProps);
                    prop.loadFromXML(fin);
                    fin.close();
                    app.cookie_session = prop.getProperty("cookie_session", null);
                    System.err.println("cookie_session:" + app.cookie_session);
                }
            } catch (Throwable err) {
                System.err.println(err.getMessage());
            }
            if (optind != args.length) {
                if (args[optind].equals("add")) {
                    ++optind;
                    if (optind == args.length) {
                        System.err.println("URL missing");
                        return;
                    }
                    while (optind < args.length) {
                        if (!Cast.URL.isA(args[optind])) {
                            System.err.println("bad url:" + args[optind]);
                            return;
                        }
                        app.addSubscription(args[optind]);
                        ++optind;
                    }
                    app.save();
                } else if (args[optind].equals("show")) {
                    app.showSlideShow();
                } else if (args[optind].equals("bookmark")) {
                    ++optind;
                    while (optind < args.length) {
                        if (!Cast.URL.isA(args[optind])) {
                            System.err.println("bad url:" + args[optind]);
                            return;
                        }
                        app.addBookmark(args[optind]);
                        ++optind;
                    }
                    app.save();
                } else {
                    System.err.println("unknown command " + args[optind]);
                    return;
                }
            } else {
                if (toDate != null && fromDate == null) fromDate = java.sql.Date.valueOf("2000-01-01");
                app.readFeeds(fromDate, toDate);
                app.save();
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
