package uk.ac.ox.cbrg.cpfp.uploadapp;

import java.awt.*;
import java.beans.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import netscape.javascript.*;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.HttpResponse;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import java.net.ProxySelector;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.scheme.Scheme;

public class UploadApplet extends javax.swing.JApplet implements PropertyChangeListener {

    private UploadTask task;

    private Vector<UploadFile> uploadFiles;

    private String uploadURL;

    private long chunkSize;

    private String browserCookie;

    private String fileExtensions;

    private JSObject jso;

    private boolean errorFlag;

    class UploadTask extends SwingWorker<Void, String> {

        private Vector<UploadFile> uploadFiles;

        public UploadTask(Vector<UploadFile> uploadFiles) {
            this.uploadFiles = uploadFiles;
        }

        public Void doInBackground() {
            setProgress(0);
            for (int i = 0; i < uploadFiles.size(); i++) {
                String filePath = uploadFiles.elementAt(i).getFilePath();
                String fileName = uploadFiles.elementAt(i).getFileName();
                String fileMsg = "Uploading file " + (i + 1) + "/" + uploadFiles.size() + "\n";
                this.publish(fileMsg);
                try {
                    File inFile = new File(filePath);
                    FileInputStream in = new FileInputStream(inFile);
                    byte[] inBytes = new byte[(int) chunkSize];
                    int count = 1;
                    int maxCount = (int) (inFile.length() / chunkSize);
                    if (inFile.length() % chunkSize > 0) {
                        maxCount++;
                    }
                    int readCount = 0;
                    readCount = in.read(inBytes);
                    while (readCount > 0) {
                        File splitFile = File.createTempFile("upl", null, null);
                        String splitName = splitFile.getPath();
                        FileOutputStream out = new FileOutputStream(splitFile);
                        out.write(inBytes, 0, readCount);
                        out.close();
                        boolean chunkFinal = (count == maxCount);
                        fileMsg = " - Sending chunk " + count + "/" + maxCount + ": ";
                        this.publish(fileMsg);
                        boolean uploadSuccess = false;
                        int uploadTries = 0;
                        while (!uploadSuccess && uploadTries <= 5) {
                            uploadTries++;
                            boolean uploadStatus = upload(splitName, fileName, count, chunkFinal);
                            if (uploadStatus) {
                                fileMsg = "OK\n";
                                this.publish(fileMsg);
                                uploadSuccess = true;
                            } else {
                                fileMsg = "ERROR\n";
                                this.publish(fileMsg);
                                uploadSuccess = false;
                            }
                        }
                        if (!uploadSuccess) {
                            fileMsg = "There was an error uploading your files. Please let the pipeline administrator know about this problem. Cut and paste the messages in this box, and supply them.\n";
                            this.publish(fileMsg);
                            errorFlag = true;
                            return null;
                        }
                        float thisProgress = (count * 100) / (maxCount);
                        float completeProgress = (i * (100 / uploadFiles.size()));
                        float totalProgress = completeProgress + (thisProgress / uploadFiles.size());
                        setProgress((int) totalProgress);
                        splitFile.delete();
                        readCount = in.read(inBytes);
                        count++;
                    }
                } catch (Exception e) {
                    this.publish(e.toString());
                }
            }
            return null;
        }

        @Override
        protected void done() {
            setCursor(null);
            if (errorFlag) {
                return;
            }
            jPanel1.setVisible(false);
            JLabel lbl = new JLabel("Processing Files. Please Wait.", JLabel.CENTER);
            lbl.setFont(new Font("Sans-Serif", Font.BOLD, 24));
            UploadApplet.this.getContentPane().setLayout(new FlowLayout());
            UploadApplet.this.getContentPane().add(lbl);
            lbl = new JLabel("Do not navigate away from this page.", JLabel.CENTER);
            UploadApplet.this.getContentPane().add(lbl);
            try {
                jso.call("upl_complete", new String[] {});
            } catch (Exception e) {
            }
        }

        @Override
        protected void process(List<String> messages) {
            for (int i = 0; i < messages.size(); i++) {
                txtMessages.append(messages.get(i));
            }
        }

        private boolean upload(String filePath, String fileName, int chunkNum, boolean chunkFinal) throws Exception {
            TrustManager easyTrustManager = new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { easyTrustManager }, null);
            SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme https = new Scheme("https", sf, 443);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.getConnectionManager().getSchemeRegistry().register(https);
            ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(httpclient.getConnectionManager().getSchemeRegistry(), ProxySelector.getDefault());
            httpclient.setRoutePlanner(routePlanner);
            HttpPost httppost = new HttpPost(uploadURL);
            httppost.addHeader("Cookie", browserCookie);
            httppost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("chunk_num", new StringBody(Integer.toString(chunkNum)));
            if (chunkFinal) {
                reqEntity.addPart("chunk_final", new StringBody("1"));
            }
            FileBody uplFile = new FileBody(new File(filePath));
            reqEntity.addPart("file", uplFile);
            reqEntity.addPart("filename", new StringBody(fileName));
            httppost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            return true;
        }
    }

    @Override
    public void init() {
        errorFlag = false;
        uploadFiles = new Vector<UploadFile>();
        uploadURL = getParameter("UPLOADURL");
        if (uploadURL == null) {
            uploadURL = "";
        }
        fileExtensions = getParameter("EXTENSIONS");
        if (fileExtensions == null) {
            fileExtensions = "";
        }
        chunkSize = Long.parseLong(getParameter("CHUNKSIZE"));
        if (chunkSize == 0) {
            chunkSize = 10485760;
        }
        try {
            JSObject myBrowser = (JSObject) JSObject.getWindow(this);
            JSObject myDocument = (JSObject) myBrowser.getMember("document");
            browserCookie = (String) myDocument.getMember("cookie");
            jso = JSObject.getWindow(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    initComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnDel = new javax.swing.JButton();
        btnUpload = new javax.swing.JButton();
        prgUploadProgress = new javax.swing.JProgressBar();
        lblProgress = new javax.swing.JLabel();
        lblMessages = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtMessages = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        fileTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Filename", "Size" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        fileTable.setColumnSelectionAllowed(true);
        fileTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(fileTable);
        fileTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        btnAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/ac/ox/cbrg/cpfp/uploadapp/12-em-plus.png")));
        btnAdd.setText("Add File(s)");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        btnDel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/ac/ox/cbrg/cpfp/uploadapp/12-em-cross.png")));
        btnDel.setText("Remove File(s)");
        btnDel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelActionPerformed(evt);
            }
        });
        btnUpload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/ac/ox/cbrg/cpfp/uploadapp/12-em-up.png")));
        btnUpload.setText("Upload Files");
        btnUpload.setEnabled(false);
        btnUpload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadActionPerformed(evt);
            }
        });
        prgUploadProgress.setStringPainted(true);
        lblProgress.setText("Progress:");
        lblMessages.setText("Messages:");
        txtMessages.setColumns(20);
        txtMessages.setRows(5);
        jScrollPane2.setViewportView(txtMessages);
        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize() + 3));
        jLabel1.setForeground(javax.swing.UIManager.getDefaults().getColor("textText"));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("CPFP File Uploader");
        jLabel2.setText("2011.10.04");
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 609, Short.MAX_VALUE).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 609, Short.MAX_VALUE).addComponent(prgUploadProgress, javax.swing.GroupLayout.DEFAULT_SIZE, 609, Short.MAX_VALUE).addComponent(lblProgress).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(btnAdd).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnDel)).addComponent(lblMessages)).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)).addGroup(jPanel1Layout.createSequentialGroup().addGap(103, 103, 103).addComponent(jLabel2))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnUpload))).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnAdd).addComponent(btnDel).addComponent(btnUpload).addComponent(jLabel1)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lblMessages).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lblProgress).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(prgUploadProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(35, 35, 35)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addComponent(jLabel2).addGap(195, 195, 195)))));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
    }

    private void btnUploadActionPerformed(java.awt.event.ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnUpload.setEnabled(false);
        btnAdd.setEnabled(false);
        btnDel.setEnabled(false);
        task = new UploadTask(uploadFiles);
        task.addPropertyChangeListener(this);
        task.execute();
        return;
    }

    private void btnDelActionPerformed(java.awt.event.ActionEvent evt) {
        int selRows[] = fileTable.getSelectedRows();
        DefaultTableModel fileModel = (DefaultTableModel) fileTable.getModel();
        int numRows = fileTable.getSelectedRows().length;
        for (int i = 0; i < numRows; i++) {
            int delRow = fileTable.getSelectedRow();
            String fileName = (String) fileModel.getValueAt(delRow, 0);
            uploadFiles.remove(delRow);
            fileModel.removeRow(delRow);
        }
        if (uploadFiles.isEmpty()) {
            btnUpload.setEnabled(false);
        }
    }

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new CustomFileFilter(fileExtensions));
        fc.setMultiSelectionEnabled(true);
        fc.showOpenDialog(UploadApplet.this);
        File[] selFiles = fc.getSelectedFiles();
        addFiles(selFiles);
    }

    private void addFiles(File[] files) {
        DefaultTableModel fileModel = (DefaultTableModel) fileTable.getModel();
        for (int i = 0; i < files.length; i++) {
            String filePath = files[i].getAbsolutePath();
            String fileName = files[i].getName();
            long fileSize = files[i].length();
            txtMessages.append("Selected file: " + fileName + "\n");
            UploadFile uf = new UploadFile(filePath, fileName, fileSize, "Not Uploaded");
            if (uploadFiles.contains(uf)) {
                JOptionPane.showMessageDialog(this, "File already in list:\n" + fileName);
            } else {
                uploadFiles.add(uf);
                fileModel.addRow(uf.getTableRow());
                btnUpload.setEnabled(true);
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("progress")) {
            int progress = (Integer) evt.getNewValue();
            prgUploadProgress.setValue(progress);
        }
    }

    private javax.swing.JButton btnAdd;

    private javax.swing.JButton btnDel;

    private javax.swing.JButton btnUpload;

    private javax.swing.JTable fileTable;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JLabel lblMessages;

    private javax.swing.JLabel lblProgress;

    private javax.swing.JProgressBar prgUploadProgress;

    private javax.swing.JTextArea txtMessages;
}
