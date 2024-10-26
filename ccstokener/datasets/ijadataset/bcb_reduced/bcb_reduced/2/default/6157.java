import info.clearthought.layout.TableLayout;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 * @author Markus Plessing
 */
public class PBLZSearch extends JFrame {

    private JPanel buttonPanel = new JPanel();

    private JPanel buttonPanel2 = new JPanel();

    private AbstractAction esc = new AbstractAction("Exit") {

        {
            putValue(Action.SHORT_DESCRIPTION, "Beenden");
        }

        public void actionPerformed(ActionEvent e) {
            processWindowEvent(new WindowEvent(viewer, WindowEvent.WINDOW_CLOSING));
        }
    };

    private AbstractAction searchplz = new AbstractAction("Suche") {

        {
            putValue(Action.SHORT_DESCRIPTION, "Suche");
        }

        public void actionPerformed(ActionEvent e) {
            searchWinPLZ(jTFStr.getText(), jTFNr.getText(), jTFPf.getText(), jTFZip.getText(), jTFLoc.getText());
        }
    };

    private AbstractAction searchblz = new AbstractAction("Suche") {

        {
            putValue(Action.SHORT_DESCRIPTION, "Suche");
        }

        public void actionPerformed(ActionEvent e) {
            searchWinBLZ(jTFblz.getText(), jTFbnm.getText(), jTFZip2.getText(), jTFLoc2.getText());
        }
    };

    private JButton exit = new JButton(esc);

    private JButton exit2 = new JButton(esc);

    private JButton find = new JButton(searchplz);

    private JButton find2 = new JButton(searchblz);

    private JTabbedPane tabby = new JTabbedPane();

    private PBLZSearch viewer;

    private JLabel jLbStr = new JLabel("Strasse : ");

    private JTextField jTFStr = new JTextField("", 10);

    private JLabel jLbNr = new JLabel("HsNr : ");

    private JTextField jTFNr = new JTextField("", 8);

    private JLabel jLbZip = new JLabel("Plz : ");

    private JTextField jTFZip = new JTextField("", 8);

    private JLabel jLbLoc = new JLabel("Ort : ");

    private JTextField jTFLoc = new JTextField("", 25);

    private JLabel jLbPf = new JLabel("Postfach : ");

    private JTextField jTFPf = new JTextField("", 10);

    private double[][] sizes = { { 70, 120, 70, 120 }, { 25, 25, 25, 50 } };

    private JPanel searchPanel = new JPanel(new TableLayout(sizes));

    private JLabel jLbblz = new JLabel("BLZ : ");

    private JTextField jTFblz = new JTextField("", 26);

    private JLabel jLbZip2 = new JLabel("PLZ : ");

    private JTextField jTFZip2 = new JTextField("", 8);

    private JLabel jLbLoc2 = new JLabel("    Ort : ");

    private JTextField jTFLoc2 = new JTextField("", 8);

    private JLabel jLbbnm = new JLabel("Bankname : ");

    private JTextField jTFbnm = new JTextField("", 26);

    private double[][] sizes2 = { { 90, 90, 90, 90 }, { 25, 25, 25, 50 } };

    private JPanel searchPanel2 = new JPanel(new TableLayout(sizes2));

    private URL url;

    private URLConnection urlConn;

    private BufferedReader reader;

    private BufferedWriter writer;

    private OutputStreamWriter printout;

    private InputStreamReader input;

    private String tmppath;

    private JEditorPane jPane;

    private JScrollPane viewerScrollPane;

    private JFrame searchFrame;

    /**
     * Constructor for PBLZSearch
     *
     */
    public PBLZSearch() {
        viewer = this;
        init();
    }

    /**
     * init : setup the viewPane of this Frame
    */
    public void init() {
        this.setResizable(false);
        this.setTitle("Online PLZ / BLZ - Suche");
        this.setIconImage(IconUtils.getCustomIcon("pblz").getImage());
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        searchPanel.add(jLbStr, "0,0, l,c");
        searchPanel.add(jTFStr, "1,0, l,c");
        searchPanel.add(jLbNr, "2,0, c,c");
        searchPanel.add(jTFNr, "3,0, l,c");
        searchPanel.add(jLbPf, "0,1, l,c");
        searchPanel.add(jTFPf, "1,1, l,c");
        searchPanel.add(jLbZip, "2,1, c,c");
        searchPanel.add(jTFZip, "3,1, l,c");
        searchPanel.add(jLbLoc, "0,2, l,c");
        searchPanel.add(jTFLoc, "1,2,3,2, l,c");
        buttonPanel.add(exit);
        buttonPanel.add(find);
        searchPanel.add(buttonPanel, "0,3,3,3, c,c");
        searchPanel2.add(jLbblz, "0,0, l,c");
        searchPanel2.add(jTFblz, "1,0,3,0, l,c");
        searchPanel2.add(jLbbnm, "0,1, l,c");
        searchPanel2.add(jTFbnm, "1,1,3,1, l,c");
        searchPanel2.add(jLbZip2, "0,2, l,c");
        searchPanel2.add(jTFZip2, "1,2,3,2, l,c");
        searchPanel2.add(jLbLoc2, "1,2,3,2, c,c");
        searchPanel2.add(jTFLoc2, "2,2,3,2, r,c");
        buttonPanel2.add(exit2);
        buttonPanel2.add(find2);
        searchPanel2.add(buttonPanel2, "0,3,3,3, c,c");
        tabby.add("PLZ-Suche", searchPanel);
        tabby.add("BLZ-Suche", searchPanel2);
        this.getContentPane().add(tabby, BorderLayout.CENTER);
        this.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension win = this.getSize();
        this.setLocation(screen.width / 2 - win.width / 2, screen.height / 2 - win.height / 2);
        this.show();
    }

    /**
     * processWindowEvent : something to do on close
     * @param e WindowEvent to process
     */
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            this.setVisible(false);
            this.dispose();
            viewer = null;
        }
    }

    /**
     * searchWinPLZ : create the Window to display the results for PLZ - Search
     * @param str String the street to search for
     * @param nr String the nbr of the house in the street
     * @param pf String Number of  the post-office box
     * @param plz String Zip
     * @param ort String Location
     */
    public void searchWinPLZ(String str, String nr, String pf, String plz, String ort) {
        searchFrame = new JFrame();
        searchFrame.setSize(new Dimension(750, 300));
        tmppath = System.getProperty("java.io.tmpdir");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension win = searchFrame.getSize();
        searchFrame.setLocation(screen.width / 2 - win.width / 2, screen.height / 2 - win.height / 2);
        try {
            url = new URL("http://149.239.160.196/cgi-bin/plz_suche/search_warp.cgi");
            urlConn = url.openConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try {
            printout = new OutputStreamWriter(urlConn.getOutputStream());
            writer = new BufferedWriter(new FileWriter(tmppath + "/return.html"));
            String content = "src=dpag&action=popup&str=" + URLEncoder.encode(str, "iso-8859-1") + "&nr=" + nr + "&pf=" + URLEncoder.encode(pf, "iso-8859-1") + "&plz=" + plz + "&ort=" + URLEncoder.encode(ort, "iso-8859-1");
            printout.write(content.toCharArray());
            printout.flush();
            printout.close();
            input = new InputStreamReader(urlConn.getInputStream());
            String string;
            reader = new BufferedReader(input);
            while (null != ((string = reader.readLine()))) {
                if (string.indexOf("Error") < 0 && string.indexOf("include") < 0 && string.indexOf("<img src") < 0) {
                    if (string.indexOf("</body>") >= 0) {
                        String copyright = "&copy;&nbsp;Deutsche Post AG  -  <a href='http://www.postdirekt.de/plz_suche/nutzungsbedingungen.html'>Nutzungsbedingungen</a>";
                        writer.write(copyright);
                    }
                    writer.write(string);
                    writer.newLine();
                }
            }
            input.close();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        jPane = new JEditorPane();
        try {
            jPane.setPage("file:///" + tmppath + "/return.html");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        jPane.setContentType("text/html");
        jPane.setEditable(false);
        jPane.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    JEditorPane pane = (JEditorPane) e.getSource();
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                        HTMLDocument doc = (HTMLDocument) pane.getDocument();
                        doc.processHTMLFrameHyperlinkEvent(evt);
                    } else try {
                        pane.setPage(e.getURL());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new ErrorHandler(ex);
                    }
                }
            }
        });
        viewerScrollPane = new JScrollPane(jPane);
        Dimension winSize = this.getSize();
        viewerScrollPane.setPreferredSize(new Dimension(winSize.width, winSize.height / 3 * 2));
        searchFrame.getContentPane().add(viewerScrollPane);
        searchFrame.show();
    }

    /**
     * searchWinBLZ : the Window to show the results for BLZ - Search
     * @param blz String banking account number
     * @param bnm String bank name
     * @param plz String Zip of the bank
     * @param ort String Location of the bank
     */
    public void searchWinBLZ(String blz, String bnm, String plz, String ort) {
        searchFrame = new JFrame();
        searchFrame.setSize(new Dimension(750, 300));
        tmppath = System.getProperty("java.io.tmpdir");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension win = searchFrame.getSize();
        searchFrame.setLocation(screen.width / 2 - win.width / 2, screen.height / 2 - win.height / 2);
        try {
            url = new URL("http://www.hsh-nordbank.de/home/eBanking/BLZ/liste.jsp");
            urlConn = url.openConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try {
            printout = new OutputStreamWriter(urlConn.getOutputStream());
            writer = new BufferedWriter(new FileWriter(tmppath + "/return.html"));
            String content = "blz=" + blz + "&bnm=" + bnm + "&ort=" + ort + "&plz=" + plz + "";
            printout.write(content.toCharArray());
            printout.flush();
            printout.close();
            input = new InputStreamReader(urlConn.getInputStream());
            String string;
            boolean go = false;
            reader = new BufferedReader(input);
            while (null != ((string = reader.readLine()))) {
                if (go == true) {
                    writer.write(string);
                    writer.newLine();
                }
                if (string.indexOf("<TABLE BORDER=\"0\" CELLPADDING=\"2\" CELLSPACING=\"0\" WIDTH=\"370\">") > 0) {
                    writer.write("<b>Die Suche ergab folgende Treffer:</b><br><br><TABLE BORDER=\"0\" CELLPADDING=\"5\" CELLSPACING=\"0\" WIDTH=\"100%\">");
                    go = true;
                }
                if (go == true && string.indexOf("</TABLE>") > 0) {
                    writer.write("</TABLE>");
                    writer.newLine();
                    go = false;
                }
            }
            writer.write("<br><br><b>&copy;&nbsp;HSH Nordbank -  Quelle: www.bundesbank.de</b>");
            input.close();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        jPane = new JEditorPane();
        try {
            jPane.setPage("file:///" + tmppath + "/return.html");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        jPane.setContentType("text/html");
        jPane.setEditable(false);
        viewerScrollPane = new JScrollPane(jPane);
        Dimension winSize = this.getSize();
        viewerScrollPane.setPreferredSize(new Dimension(winSize.width, winSize.height / 3 * 2));
        searchFrame.getContentPane().add(viewerScrollPane);
        searchFrame.show();
    }
}
