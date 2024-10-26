package jsattrak.gui;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import jsattrak.utilities.TreeGroundStationTransferHandler;

/**
 *
 * @author  sgano
 */
public class JGroundStationBrowser extends javax.swing.JPanel implements java.io.Serializable {

    public static String groundStationDB = "data/groundstations/groundstations_db.csv";

    public static String groundStationDir = "data/groundstations/";

    public static String groundStationCustomDB = "data/groundstations/groundstations_custom.csv";

    DefaultTreeModel treeModel;

    DefaultMutableTreeNode topTreeNode;

    DefaultMutableTreeNode customSecondaryNode;

    Hashtable<String, double[]> gsHash = new Hashtable<String, double[]>();

    Frame parent;

    /** Creates new form JGroundStationBrowser */
    public JGroundStationBrowser(Frame parent) {
        this.parent = parent;
        initComponents();
        topTreeNode = new DefaultMutableTreeNode("Ground Stations");
        treeModel = new DefaultTreeModel(topTreeNode);
        groundStationTree.setModel(treeModel);
        String currentSecondaryNodeName = null;
        DefaultMutableTreeNode currentSecondaryNode = null;
        try {
            BufferedReader gsReader = null;
            if (new File(groundStationDB).exists()) {
                File gsFile = new File(groundStationDB);
                FileReader gsFileReader = new FileReader(gsFile);
                gsReader = new BufferedReader(gsFileReader);
            } else {
                URL url = new URL("http://www.gano.name/shawn/JSatTrak/" + groundStationDB);
                URLConnection c = url.openConnection();
                InputStreamReader isr = new InputStreamReader(c.getInputStream());
                gsReader = new BufferedReader(isr);
            }
            String nextLine = null;
            int gsCount = 0;
            while ((nextLine = gsReader.readLine()) != null) {
                String[] elements = nextLine.split(",");
                if (elements.length == 5) {
                    String network = elements[0];
                    String stationName = elements[1];
                    double stationLat = Double.parseDouble(elements[2]);
                    double stationLon = Double.parseDouble(elements[3]);
                    double stationAlt = Double.parseDouble(elements[4]);
                    gsHash.put(stationName, new double[] { stationLat, stationLon, stationAlt });
                    if (!network.equalsIgnoreCase(currentSecondaryNodeName)) {
                        currentSecondaryNode = new DefaultMutableTreeNode(network);
                        topTreeNode.add(currentSecondaryNode);
                        currentSecondaryNodeName = network;
                    }
                    currentSecondaryNode.add(new DefaultMutableTreeNode(stationName));
                    gsCount++;
                }
            }
            gsReader.close();
            if (new File(groundStationCustomDB).exists()) {
                File gsFile = new File(groundStationCustomDB);
                FileReader gsFileReader = new FileReader(gsFile);
                gsReader = new BufferedReader(gsFileReader);
                customSecondaryNode = new DefaultMutableTreeNode("Custom");
                topTreeNode.add(customSecondaryNode);
                while ((nextLine = gsReader.readLine()) != null) {
                    String[] elements = nextLine.split(",");
                    if (elements.length == 5) {
                        String network = elements[0];
                        String stationName = elements[1];
                        double stationLat = Double.parseDouble(elements[2]);
                        double stationLon = Double.parseDouble(elements[3]);
                        double stationAlt = Double.parseDouble(elements[4]);
                        gsHash.put(stationName, new double[] { stationLat, stationLon, stationAlt });
                        customSecondaryNode.add(new DefaultMutableTreeNode(stationName));
                        gsCount++;
                    }
                }
                gsReader.close();
            } else {
                customSecondaryNode = new DefaultMutableTreeNode("Custom");
                topTreeNode.add(customSecondaryNode);
            }
            statusTextField.setText("Total Ground Stations loaded: " + gsCount);
            groundStationTree.expandRow(0);
        } catch (Exception e) {
            System.out.println("ERROR IN GROUND STATION READING POSSIBLE FILE FORMAT OR MISSING FILES:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error Loading Ground Station Data. Check data.\n" + e.toString(), "Data LOADING ERROR", JOptionPane.ERROR_MESSAGE);
        }
        groundStationTree.setTransferHandler(new TreeGroundStationTransferHandler(gsHash));
        groundStationTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        groundStationTree = new javax.swing.JTree();
        statusTextField = new javax.swing.JTextField();
        addGSButton = new javax.swing.JButton();
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setText("Ground Station Browser");
        groundStationTree.setDragEnabled(true);
        groundStationTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                groundStationTreeValueChanged(evt);
            }
        });
        groundStationTree.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                groundStationTreePropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(groundStationTree);
        statusTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusTextFieldActionPerformed(evt);
            }
        });
        addGSButton.setText("+");
        addGSButton.setToolTipText("Add Custom Ground Station");
        addGSButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addGSButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE).addComponent(addGSButton)).addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(addGSButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(statusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
    }

    private void statusTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void groundStationTreePropertyChange(java.beans.PropertyChangeEvent evt) {
    }

    private void groundStationTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {
        if (groundStationTree.getSelectionCount() > 0) {
            if (gsHash.containsKey(groundStationTree.getLastSelectedPathComponent().toString())) {
                double[] lla = gsHash.get(groundStationTree.getLastSelectedPathComponent().toString());
                statusTextField.setText("Lat:" + lla[0] + ", Lon:" + lla[1] + ", Alt[m]:" + lla[2]);
            } else {
                statusTextField.setText("");
            }
        }
    }

    private void addGSButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JAddGroundStationDialog dlg = new JAddGroundStationDialog(parent, true);
        dlg.setVisible(true);
        if (dlg.isOkHit()) {
            String network = dlg.getNetwork();
            String siteName = dlg.getSiteName();
            double latitude = dlg.getLatitude();
            double longitude = dlg.getLongitude();
            double altitude = dlg.getAltitude();
            boolean saveData = dlg.isSaveData();
            gsHash.put(siteName, new double[] { latitude, longitude, altitude });
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(siteName);
            customSecondaryNode.add(newNode);
            groundStationTree.repaint();
            groundStationTree.scrollPathToVisible(getPath(newNode));
            if (saveData) {
                try {
                    File gsFile = new File(groundStationCustomDB);
                    boolean append = true;
                    if (!gsFile.exists()) {
                        (new File(groundStationDir)).mkdirs();
                        gsFile.createNewFile();
                        append = false;
                    }
                    FileWriter gsFileWriter = new FileWriter(gsFile, append);
                    BufferedWriter gsWriter = new BufferedWriter(gsFileWriter);
                    gsWriter.write("\n" + network + "," + siteName + "," + latitude + "," + longitude + "," + altitude);
                    gsWriter.close();
                } catch (Exception e) {
                    System.out.println("ERROR SAVING GROUND STATION - Check permissions or format:");
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parent, "Error Saving Ground Station Data. Check data and permissions. \n" + e.toString(), "Data SAVING ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public TreePath getPath(TreeNode node) {
        List<TreeNode> list = new ArrayList<TreeNode>();
        while (node != null) {
            list.add(node);
            node = node.getParent();
        }
        Collections.reverse(list);
        return new TreePath(list.toArray());
    }

    private javax.swing.JButton addGSButton;

    private javax.swing.JTree groundStationTree;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextField statusTextField;
}
