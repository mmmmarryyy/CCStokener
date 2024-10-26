package glisten;

import glisten.ButtonTabComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Nathan
 */
public class Client extends javax.swing.JFrame {

    private Header header;

    /** Creates new form Client */
    public Client() {
        initComponents();
        header = new Header();
        header.load();
    }

    static final String baseUrl = "http://teaching.cs.uml.edu/~daniel/sound_library/remote_query.jsp?sql_query=";

    void search(String query, String display) {
        try {
            String safeUrl;
            try {
                safeUrl = baseUrl + URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                safeUrl = baseUrl + query;
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            URL url_connection = new URL(safeUrl);
            url_connection.openConnection();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(url_connection.openStream());
            Vector<SoundEntry> entries = new Vector<SoundEntry>();
            Vector<String> path = new Vector<String>();
            path.add("Results");
            for (Hashtable<String, String> field : DocumentManager.getSubTable(document, path, "Entry")) {
                entries.add(new SoundEntry(field));
            }
            int index;
            ButtonTabComponent btc = new ButtonTabComponent(tpnResults);
            btc.setInheritsPopupMenu(true);
            if (entries.isEmpty()) {
                JLabel msg = new JLabel("No results found");
                tpnResults.add(display, msg);
                index = tpnResults.indexOfComponent(msg);
            } else {
                Enumeration<String> iter = entries.firstElement().fields.keys();
                while (iter.hasMoreElements()) {
                    String field = iter.nextElement();
                    if (!header.contains(field)) {
                        header.addDefaultField(field);
                    }
                }
                JTable result = new JTable();
                Vector<String> fieldNames = header.getShownNames();
                DefaultTableModel model = new DefaultTableModel(fieldNames, 0);
                for (SoundEntry entry : entries) {
                    model.addRow(entry.getShownFields(header.getShownNames()));
                }
                result.setModel(model);
                result.setColumnSelectionAllowed(false);
                result.setSelectionMode(0);
                result.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        ((JTable) e.getSource()).getComponentAt(e.getPoint());
                        int row = ((JTable) e.getSource()).rowAtPoint(e.getPoint());
                        SoundEntry entry = ((ButtonTabComponent) tpnResults.getTabComponentAt(tpnResults.getSelectedIndex())).records.get(row);
                        String file = entry.fields.get("FileName");
                        String title = entry.fields.get("Title");
                        if (file != null && !file.isEmpty()) {
                            try {
                                AudioSource src = new AudioSource(new URL(file), title);
                                src.attachAudioStateListener(new AudioStateListener() {

                                    public void AudioStateReceived(AudioStateEvent event) {
                                        if (event.getAudioState() != AudioStateEvent.AudioState.CLOSED && event.getAudioState() != AudioStateEvent.AudioState.CLOSING) {
                                            llblStatus.setText(event.getAudioState() + ": " + ((AudioSource) event.getSource()).getTitle().toString());
                                        }
                                    }
                                });
                                audioPlayer.open(src);
                            } catch (Exception j) {
                            }
                        }
                    }
                });
                JScrollPane scrollPane = new JScrollPane(result);
                tpnResults.add(display, scrollPane);
                index = tpnResults.indexOfComponent(scrollPane);
                btc.records = entries;
            }
            tpnResults.setTabComponentAt(index, btc);
            tpnResults.setSelectedIndex(index);
        } catch (SAXException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        pmnResults = new javax.swing.JPopupMenu();
        mniCloseResult = new javax.swing.JMenuItem();
        mniCloseOthers = new javax.swing.JMenuItem();
        pnlStatus = new javax.swing.JPanel();
        llblStatus = new javax.swing.JLabel();
        pnlResults = new javax.swing.JPanel();
        tpnResults = new javax.swing.JTabbedPane();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        pnlPlayer = new javax.swing.JPanel();
        audioPlayer = new glisten.AudioPlayerControls();
        mniCloseResult.setText("Close");
        mniCloseResult.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniCloseResultActionPerformed(evt);
            }
        });
        pmnResults.add(mniCloseResult);
        mniCloseOthers.setText("Close Others");
        mniCloseOthers.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniCloseOthersActionPerformed(evt);
            }
        });
        pmnResults.add(mniCloseOthers);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("gListen");
        pnlStatus.setBackground(new java.awt.Color(255, 255, 255));
        pnlStatus.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        javax.swing.GroupLayout pnlStatusLayout = new javax.swing.GroupLayout(pnlStatus);
        pnlStatus.setLayout(pnlStatusLayout);
        pnlStatusLayout.setHorizontalGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(llblStatus, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE));
        pnlStatusLayout.setVerticalGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(llblStatus, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE));
        tpnResults.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        tpnResults.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                tpnResultsMousePressed(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tpnResultsMouseReleased(evt);
            }
        });
        txtSearch.setText("Enter search terms here. Right click for options.");
        txtSearch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }
        });
        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        jButton1.setText("Show All");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout pnlResultsLayout = new javax.swing.GroupLayout(pnlResults);
        pnlResults.setLayout(pnlResultsLayout);
        pnlResultsLayout.setHorizontalGroup(pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlResultsLayout.createSequentialGroup().addContainerGap().addGroup(pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(tpnResults, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE).addGroup(pnlResultsLayout.createSequentialGroup().addComponent(txtSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSearch).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton1))).addContainerGap()));
        pnlResultsLayout.setVerticalGroup(pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlResultsLayout.createSequentialGroup().addContainerGap().addComponent(tpnResults, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jButton1).addComponent(btnSearch)).addContainerGap()));
        pnlPlayer.setMaximumSize(new java.awt.Dimension(300, 150));
        pnlPlayer.setPreferredSize(new java.awt.Dimension(300, 150));
        audioPlayer.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnlPlayer.add(audioPlayer);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(pnlPlayer, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE).addContainerGap()).addComponent(pnlResults, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(pnlStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(pnlResults, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGap(18, 18, 18).addComponent(pnlPlayer, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(pnlStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    private void mniCloseResultActionPerformed(java.awt.event.ActionEvent evt) {
        if (tpnResults.getSelectedIndex() != -1) {
            ((ButtonTabComponent) tpnResults.getTabComponentAt(tpnResults.getSelectedIndex())).close(evt);
        }
    }

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {
        btnSearchActionPerformed(evt);
        txtSearch.selectAll();
    }

    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {
        if (txtSearch.getText().equals("Enter search terms here. Right click for options.")) {
            txtSearch.setText("");
        } else {
            txtSearch.selectAll();
        }
    }

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {
        search(txtSearch.getText(), txtSearch.getText());
    }

    private void mniCloseOthersActionPerformed(java.awt.event.ActionEvent evt) {
        int index = tpnResults.getSelectedIndex();
        for (int i = tpnResults.getTabCount() - 1; i >= 0; i--) {
            if (i != index) {
                ((ButtonTabComponent) tpnResults.getTabComponentAt(i)).close(evt);
            }
        }
    }

    private void handleResultsPopup(java.awt.event.MouseEvent evt) {
        if (tpnResults.indexAtLocation(evt.getX(), evt.getY()) != -1) {
            pmnResults.show(tpnResults, evt.getX(), evt.getY());
        }
    }

    private void tpnResultsMousePressed(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            handleResultsPopup(evt);
        }
    }

    private void tpnResultsMouseReleased(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            handleResultsPopup(evt);
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        search("select * from library", "All Files");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Client().setVisible(true);
            }
        });
    }

    private glisten.AudioPlayerControls audioPlayer;

    private javax.swing.JButton btnSearch;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel llblStatus;

    private javax.swing.JMenuItem mniCloseOthers;

    private javax.swing.JMenuItem mniCloseResult;

    private javax.swing.JPopupMenu pmnResults;

    private javax.swing.JPanel pnlPlayer;

    private javax.swing.JPanel pnlResults;

    private javax.swing.JPanel pnlStatus;

    private javax.swing.JTabbedPane tpnResults;

    private javax.swing.JTextField txtSearch;
}
