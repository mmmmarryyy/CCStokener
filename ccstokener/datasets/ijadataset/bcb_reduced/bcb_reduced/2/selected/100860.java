package newgen.presentation.cataloguing.advanced;

import java.awt.Component;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import newgen.presentation.MainFrame;
import newgen.presentation.component.NGLResourceBundle;
import newgen.presentation.component.ServletConnector;
import newgen.presentation.component.SimpleCombobBoxItem;
import newgen.presentation.component.Utility;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author siddartha
 */
public class AttachmentsPanel extends javax.swing.JPanel {

    private String catalogueRecordId;

    private String ownerLibraryId;

    private SimpleCombobBoxItem sciBookCover = null;

    private SimpleCombobBoxItem sciTOC = null;

    private SimpleCombobBoxItem sciPreview = null;

    private SimpleCombobBoxItem sciFullView = null;

    private SimpleCombobBoxItem sciAttachments = null;

    private DefaultTableModel dtmAttachments = null;

    /** Creates new form AttachmentsPanel */
    public AttachmentsPanel() {
        initComponents();
        dialogProgress.setSize(513, 175);
        dialogProgress.setLocation(Utility.getInstance().getLocation(513, 175));
        String[] colsA = { NGLResourceBundle.getInstance().getString("Type"), NGLResourceBundle.getInstance().getString("FileLocation") };
        dtmAttachments = new DefaultTableModel(colsA, 0);
        jTable1.setModel(dtmAttachments);
        jTable1.setRowHeight(jTable1.getRowHeight() + 5);
        Vector<SimpleCombobBoxItem> items = new Vector<SimpleCombobBoxItem>();
        items.add(sciBookCover = new SimpleCombobBoxItem(0, NGLResourceBundle.getInstance().getString("BookCover")));
        items.add(sciTOC = new SimpleCombobBoxItem(1, NGLResourceBundle.getInstance().getString("TableOfContents")));
        items.add(sciPreview = new SimpleCombobBoxItem(2, NGLResourceBundle.getInstance().getString("Preview")));
        items.add(sciFullView = new SimpleCombobBoxItem(3, NGLResourceBundle.getInstance().getString("FullView")));
        items.add(sciAttachments = new SimpleCombobBoxItem(4, NGLResourceBundle.getInstance().getString("Attachments")));
        TableColumn col = jTable1.getColumnModel().getColumn(0);
        col.setCellEditor(new AttachmentsComboBoxEditor(items));
        col.setCellRenderer(new AttachmentsComboBoxRenderer(items));
    }

    public void clearScreen() {
        dtmAttachments.setRowCount(0);
    }

    public void setModification(String catalogueRecordId, String ownerLibraryId) {
        this.catalogueRecordId = catalogueRecordId;
        this.ownerLibraryId = ownerLibraryId;
        initialLoadAttachments();
    }

    public void uploadFiles() {
        boolean anyUploads = false;
        for (int i = 0; i < dtmAttachments.getRowCount(); i++) {
            SimpleCombobBoxItem scbi = (SimpleCombobBoxItem) dtmAttachments.getValueAt(i, 0);
            if (!dtmAttachments.getValueAt(i, 1).getClass().getName().contains("String")) {
                AttachmentsTableCellValue atcv = (AttachmentsTableCellValue) dtmAttachments.getValueAt(i, 1);
                if (!atcv.isLocatedOnServer()) {
                    anyUploads = true;
                    break;
                }
            }
        }
        if (anyUploads) {
            dialogProgress.setVisible(true);
            final String catId = getCatalogueRecordId();
            final String libId = getOwnerLibraryId();
            progressbar.setIndeterminate(true);
            javax.swing.SwingWorker sw = new javax.swing.SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {
                    for (int i = 0; i < dtmAttachments.getRowCount(); i++) {
                        SimpleCombobBoxItem scbi = (SimpleCombobBoxItem) dtmAttachments.getValueAt(i, 0);
                        if (!dtmAttachments.getValueAt(i, 1).getClass().getName().contains("String")) {
                            AttachmentsTableCellValue atcv = (AttachmentsTableCellValue) dtmAttachments.getValueAt(i, 1);
                            if (!atcv.isLocatedOnServer()) {
                                String path = atcv.getPath();
                                try {
                                    HttpClient httpclient = new DefaultHttpClient();
                                    HttpPost httppost = new HttpPost("http://" + ServletConnector.serverURL + ":8080/newgenlibctxt/AttachmentUploadServlet?Type=" + scbi.getIndex() + "&Id=" + catId + "&LibId=" + libId);
                                    FileBody bin = new FileBody(new File(path));
                                    StringBody comment = new StringBody("Filename: " + atcv.getName());
                                    lbMessage.setText("Uploading " + atcv.getName());
                                    MultipartEntity reqEntity = new MultipartEntity();
                                    reqEntity.addPart("bin", bin);
                                    reqEntity.addPart("comment", comment);
                                    httppost.setEntity(reqEntity);
                                    HttpResponse response = httpclient.execute(httppost);
                                    HttpEntity resEntity = response.getEntity();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    }
                    lbMessage.setText("Upload completed");
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), "Upload completed", "Uploaded completed", JOptionPane.INFORMATION_MESSAGE);
                    dialogProgress.dispose();
                    progressbar.setIndeterminate(false);
                    return null;
                }
            };
            sw.execute();
        }
    }

    public void initialLoadAttachments() {
        try {
            String jsonreq = (new JSONObject()).put("Id", getCatalogueRecordId()).put("LibId", getOwnerLibraryId()).put("OperationId", "42").toString();
            String jsonres = ServletConnector.getInstance().sendJSONRequest("JSONServlet", jsonreq);
            JSONObject jobres = new JSONObject(jsonres);
            JSONArray jr = jobres.getJSONArray("BookCover");
            for (int i = 0; i < jr.length(); i++) {
                String val = jr.getString(i);
                Object[] row = new Object[2];
                row[0] = sciBookCover;
                row[1] = new AttachmentsTableCellValue(val, "0", true);
                dtmAttachments.addRow(row);
            }
            jr = jobres.getJSONArray("TOC");
            for (int i = 0; i < jr.length(); i++) {
                String val = jr.getString(i);
                Object[] row = new Object[2];
                row[0] = sciTOC;
                row[1] = new AttachmentsTableCellValue(val, "1", true);
                dtmAttachments.addRow(row);
            }
            jr = jobres.getJSONArray("Preview");
            for (int i = 0; i < jr.length(); i++) {
                String val = jr.getString(i);
                Object[] row = new Object[2];
                row[0] = sciPreview;
                row[1] = new AttachmentsTableCellValue(val, "2", true);
                dtmAttachments.addRow(row);
            }
            jr = jobres.getJSONArray("FullView");
            for (int i = 0; i < jr.length(); i++) {
                String val = jr.getString(i);
                Object[] row = new Object[2];
                row[0] = sciFullView;
                row[1] = new AttachmentsTableCellValue(val, "3", true);
                dtmAttachments.addRow(row);
            }
            jr = jobres.getJSONArray("Attachment");
            for (int i = 0; i < jr.length(); i++) {
                String val = jr.getString(i);
                Object[] row = new Object[2];
                row[0] = sciAttachments;
                row[1] = new AttachmentsTableCellValue(val, "4", true);
                dtmAttachments.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        dialogProgress = new javax.swing.JDialog();
        jPanel12 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        lbMessage = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        dialogProgress.setTitle(NGLResourceBundle.getInstance().getString("UploadInProgressPleaseWait"));
        jPanel12.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel12.setLayout(new java.awt.BorderLayout());
        jPanel12.add(progressbar, java.awt.BorderLayout.CENTER);
        jPanel12.add(lbMessage, java.awt.BorderLayout.PAGE_START);
        dialogProgress.getContentPane().add(jPanel12, java.awt.BorderLayout.CENTER);
        jButton1.setMnemonic('c');
        jButton1.setText(NGLResourceBundle.getInstance().getString("Close"));
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);
        dialogProgress.getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        jPanel8.setLayout(new java.awt.BorderLayout());
        jPanel4.setLayout(new java.awt.BorderLayout());
        jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] {}));
        jScrollPane12.setViewportView(jTable1);
        jPanel4.add(jScrollPane12, java.awt.BorderLayout.CENTER);
        jPanel8.add(jPanel4, java.awt.BorderLayout.CENTER);
        jPanel5.setLayout(new java.awt.GridBagLayout());
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/general/Open16.gif")));
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel5.add(jButton3, gridBagConstraints);
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/general/Add16.gif")));
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel5.add(jButton4, gridBagConstraints);
        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/newgen/images/general/Delete16.gif")));
        jButton5.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel5.add(jButton5, gridBagConstraints);
        jButton6.setText("View");
        jButton6.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel5.add(jButton6, gridBagConstraints);
        jPanel8.add(jPanel5, java.awt.BorderLayout.EAST);
        add(jPanel8);
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        Object[] obx = new Object[2];
        obx[0] = sciAttachments;
        obx[1] = "";
        dtmAttachments.addRow(obx);
    }

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedRowAttachments = jTable1.getSelectedRow();
        if (selectedRowAttachments != -1) {
            String catId = getCatalogueRecordId();
            String libId = getOwnerLibraryId();
            if (!dtmAttachments.getValueAt(selectedRowAttachments, 1).getClass().getName().contains("String")) {
                AttachmentsTableCellValue atcv = (AttachmentsTableCellValue) dtmAttachments.getValueAt(selectedRowAttachments, 1);
                String filename = atcv.getName();
                String path = atcv.getPath();
                boolean isServerSide = atcv.isLocatedOnServer();
                if (isServerSide) {
                    try {
                        String req = (new JSONObject()).put("OperationId", "43").put("Type", path).put("FileName", filename).put("Id", catId).put("LibId", libId).toString();
                        String res = ServletConnector.getInstance().sendJSONRequest("JSONServlet", req);
                        JSONObject jobres = new JSONObject(res);
                        if (jobres.getString("Status").equals("true")) {
                            dtmAttachments.removeRow(selectedRowAttachments);
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.getInstance(), NGLResourceBundle.getInstance().getString("CannotDeleteTheAttachment"), NGLResourceBundle.getInstance().getString("Error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    dtmAttachments.removeRow(selectedRowAttachments);
                }
            }
        }
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow != -1) {
            JFileChooser fc = new JFileChooser();
            fc.showOpenDialog(MainFrame.getInstance());
            File file = fc.getSelectedFile();
            dtmAttachments.setValueAt(new AttachmentsTableCellValue(file.getName(), file.getAbsolutePath(), false), selectedRow, 1);
        }
    }

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedRowAttachments = jTable1.getSelectedRow();
        if (selectedRowAttachments != -1) {
            String catId = getCatalogueRecordId();
            String libId = getOwnerLibraryId();
            if (!dtmAttachments.getValueAt(selectedRowAttachments, 1).getClass().getName().contains("String")) {
                AttachmentsTableCellValue atcv = (AttachmentsTableCellValue) dtmAttachments.getValueAt(selectedRowAttachments, 1);
                String filename = atcv.getName();
                String path = atcv.getPath();
                boolean isServerSide = atcv.isLocatedOnServer();
                if (isServerSide) {
                    try {
                        String fileNameN = URLEncoder.encode(filename, "UTF-8");
                        Utility.getInstance().showBrowser("http://" + ServletConnector.serverURL + ":" + ServletConnector.serverPort + "/newgenlibctxt/AttachmentDownloadServlet?Type=" + path + "&Id=" + catId + "&LibId=" + libId + "&FileName=" + fileNameN);
                    } catch (UnsupportedEncodingException ex) {
                    }
                } else {
                }
            }
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        dialogProgress.dispose();
    }

    private javax.swing.JDialog dialogProgress;

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    private javax.swing.JButton jButton5;

    private javax.swing.JButton jButton6;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel12;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JScrollPane jScrollPane12;

    private javax.swing.JTable jTable1;

    private javax.swing.JLabel lbMessage;

    private javax.swing.JProgressBar progressbar;

    /**
     * @return the catalogueRecordId
     */
    public String getCatalogueRecordId() {
        return catalogueRecordId;
    }

    /**
     * @param catalogueRecordId the catalogueRecordId to set
     */
    public void setCatalogueRecordId(String catalogueRecordId) {
        this.catalogueRecordId = catalogueRecordId;
    }

    /**
     * @return the ownerLibraryId
     */
    public String getOwnerLibraryId() {
        return ownerLibraryId;
    }

    /**
     * @param ownerLibraryId the ownerLibraryId to set
     */
    public void setOwnerLibraryId(String ownerLibraryId) {
        this.ownerLibraryId = ownerLibraryId;
    }
}

class AttachmentsComboBoxRenderer extends JComboBox implements TableCellRenderer {

    public AttachmentsComboBoxRenderer(Vector items) {
        super(items);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setSelectedItem(value);
        return this;
    }
}

class AttachmentsComboBoxEditor extends DefaultCellEditor {

    public AttachmentsComboBoxEditor(Vector items) {
        super(new JComboBox(items));
    }
}

class AttachmentsTableCellValue {

    private boolean locatedOnServer;

    private String name;

    private String path;

    public AttachmentsTableCellValue(String name, String path, boolean locatedOnServer) {
        this.locatedOnServer = locatedOnServer;
        this.name = name;
        this.path = path;
    }

    @Override
    public String toString() {
        if (!locatedOnServer) {
            return name;
        } else {
            return "On Server/" + name;
        }
    }

    /**
     * @return the locatedOnServer
     */
    public boolean isLocatedOnServer() {
        return locatedOnServer;
    }

    /**
     * @param locatedOnServer the locatedOnServer to set
     */
    public void setLocatedOnServer(boolean locatedOnServer) {
        this.locatedOnServer = locatedOnServer;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }
}
