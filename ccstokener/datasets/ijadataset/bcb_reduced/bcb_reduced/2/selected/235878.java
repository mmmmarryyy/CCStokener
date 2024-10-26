package org.semtinel.plugins.kai.oaipmh;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.semtinel.core.data.api.CoreManager;
import org.semtinel.core.data.api.DocumentSetInserter;
import org.semtinel.core.util.ProgressListener;
import org.semtinel.core.util.StringUtility;

/**
 * Top component which displays something.
 */
final class OaiPmhAccessTopComponent extends TopComponent {

    private static OaiPmhAccessTopComponent instance;

    private static final String PREFERRED_ID = "OaiPmhAccessTopComponent";

    private OaiPmhAccessTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(OaiPmhAccessTopComponent.class, "CTL_OaiPmhAccessTopComponent"));
        setToolTipText(NbBundle.getMessage(OaiPmhAccessTopComponent.class, "HINT_OaiPmhAccessTopComponent"));
        dataProviderField.setText("http://cs1.ist.psu.edu/cgi-bin/oai.cgi");
        fromField.setText("2002-01-01");
        untilField.setText("2002-01-31");
    }

    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        recordTable = new javax.swing.JTable();
        listRecordsButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        fromField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        untilField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        setField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        maxField = new javax.swing.JTextField();
        dataProviderField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        numberLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        skipEmptyAbstracts = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        recordSetName = new javax.swing.JTextField();
        importButton = new javax.swing.JButton();
        filterLanguageCheckbox = new javax.swing.JCheckBox();
        languageField = new javax.swing.JTextField();
        recordTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        jScrollPane1.setViewportView(recordTable);
        org.openide.awt.Mnemonics.setLocalizedText(listRecordsButton, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.listRecordsButton.text"));
        listRecordsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listRecordsButtonActionPerformed(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel2.text"));
        fromField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.fromField.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel3.text"));
        untilField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.untilField.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel4.text"));
        setField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.setField.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel6.text"));
        maxField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.maxField.text"));
        dataProviderField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.dataProviderField.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel1.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel5.text"));
        org.openide.awt.Mnemonics.setLocalizedText(numberLabel, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.numberLabel.text"));
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(dataProviderField, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().addComponent(listRecordsButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel5).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(numberLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(fromField, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel3).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(untilField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel4))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(setField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 94, Short.MAX_VALUE).addComponent(jLabel6).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(maxField, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(dataProviderField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel2).addComponent(fromField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(setField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel3).addComponent(untilField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel4).addComponent(maxField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel6)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(listRecordsButton).addComponent(jLabel5).addComponent(numberLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE).addContainerGap()));
        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jPanel2.TabConstraints.tabTitle"), jPanel2);
        skipEmptyAbstracts.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(skipEmptyAbstracts, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.skipEmptyAbstracts.text"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jLabel7.text"));
        recordSetName.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.recordSetName.text"));
        org.openide.awt.Mnemonics.setLocalizedText(importButton, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.importButton.text"));
        importButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(filterLanguageCheckbox, org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.filterLanguageCheckbox.text"));
        languageField.setText(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.languageField.text"));
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(filterLanguageCheckbox).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(languageField, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(skipEmptyAbstracts).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel7).addGap(18, 18, 18).addComponent(recordSetName, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(importButton)).addContainerGap(321, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(skipEmptyAbstracts).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(filterLanguageCheckbox).addComponent(languageField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel7).addComponent(recordSetName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(importButton).addContainerGap(247, Short.MAX_VALUE)));
        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(OaiPmhAccessTopComponent.class, "OaiPmhAccessTopComponent.jPanel1.TabConstraints.tabTitle"), jPanel1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE));
    }

    private List<OaiPmhRecord> recordList;

    private void listRecordsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (dataProviderField.getText().trim().length() == 0) {
            return;
        }
        final int maxRecords;
        try {
            final URL url = new URL(dataProviderField.getText().trim());
            final URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write("verb=ListRecords");
            writer.write("&metadataPrefix=oai_dc");
            if (fromField.getText().trim().length() > 0) {
                writer.write("&from=" + fromField.getText().trim());
            }
            if (untilField.getText().trim().length() > 0) {
                writer.write("&until=" + untilField.getText().trim());
            }
            if (setField.getText().trim().length() > 0) {
                writer.write("&set=" + setField.getText().trim());
            }
            int tmpMax = -1;
            if (maxField.getText().trim().length() > 0) {
                try {
                    tmpMax = Integer.parseInt(maxField.getText());
                } catch (NumberFormatException nfe) {
                    tmpMax = -1;
                }
            }
            maxRecords = tmpMax;
            writer.flush();
            conn.connect();
            final ProgressHandle p = ProgressHandleFactory.createHandle("Import OAI-PMH");
            Runnable run = new Runnable() {

                public void run() {
                    final OaiPmhParser parser = new OaiPmhParser();
                    parser.addProgressListener(new ProgressListener() {

                        public void progress(String message, int progress) {
                            p.progress(message);
                        }

                        public void finish() {
                            p.finish();
                            recordList = parser.getRecordList();
                            numberLabel.setText("" + parser.getRecordList().size());
                            recordTable.setModel(new AbstractTableModel() {

                                public int getRowCount() {
                                    return parser.getRecordList().size();
                                }

                                public int getColumnCount() {
                                    return 5;
                                }

                                public Object getValueAt(int rowIndex, int columnIndex) {
                                    switch(columnIndex) {
                                        case 0:
                                            return parser.getRecordList().get(rowIndex).getIdentifier();
                                        case 1:
                                            return parser.getRecordList().get(rowIndex).getTitle();
                                        case 2:
                                            return StringUtility.join(", ", parser.getRecordList().get(rowIndex).getAuthors());
                                        case 3:
                                            return StringUtility.join(", ", parser.getRecordList().get(rowIndex).getDescriptions());
                                        case 4:
                                            return parser.getRecordList().get(rowIndex).getLanguage();
                                        default:
                                            return null;
                                    }
                                }
                            });
                        }

                        public void start(int max) {
                            p.start();
                        }
                    });
                    try {
                        parser.process(conn.getInputStream(), maxRecords);
                        while (parser.getResumptionToken() != null && (maxRecords == -1 || parser.getRecordList().size() < maxRecords)) {
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setDoOutput(true);
                            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                            writer.write("verb=ListRecords");
                            writer.write("&resumptionToken=" + parser.getResumptionToken());
                            writer.flush();
                            conn.connect();
                            if (conn.getResponseCode() == 500) {
                                continue;
                            }
                            parser.process(conn.getInputStream(), maxRecords);
                        }
                    } catch (IOException ioe) {
                        throw new RuntimeException("IO Error: " + ioe, ioe);
                    }
                }
            };
            Thread thread = new Thread(run);
            thread.start();
        } catch (IOException ioe) {
            throw new RuntimeException("IO Error: " + ioe, ioe);
        }
    }

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Runnable run = new Runnable() {

            public void run() {
                ProgressHandle p = ProgressHandleFactory.createHandle("Import Pubmed", new Cancellable() {

                    public boolean cancel() {
                        Thread.currentThread().interrupt();
                        return true;
                    }
                });
                InputOutput io = IOProvider.getDefault().getIO("Import Pubmed", false);
                CoreManager cm = Lookup.getDefault().lookup(CoreManager.class);
                DocumentSetInserter dsInserter = cm.createDocumentSetInserter(recordSetName.getText());
                p.start(recordList.size());
                for (int i = 0; i < recordList.size(); i++) {
                    if (Thread.currentThread().interrupted()) {
                        break;
                    }
                    OaiPmhRecord record = recordList.get(i);
                    String pmid = record.getIdentifier();
                    String title = record.getTitle();
                    List<String> authors = record.getAuthors();
                    if (skipEmptyAbstracts.isSelected() && (record.getDescriptions().isEmpty() || record.getDescriptions().get(0).getText().trim().isEmpty())) {
                        continue;
                    }
                    if (filterLanguageCheckbox.isSelected() && (!languageField.getText().equals(record.getLanguage()))) {
                        continue;
                    }
                    if (authors.size() > 1) {
                        List<String> newauthors = new ArrayList<String>();
                        newauthors.add(authors.get(0));
                        authors = newauthors;
                    }
                    dsInserter.insertRecord(pmid, title, authors, record.getDescriptions(), record.getLanguage(), "", 0);
                    io.getOut().println("Processed: " + title);
                    p.progress(title, i);
                }
                p.finish();
                io.getErr().close();
                io.getOut().close();
                dsInserter.release();
            }
        };
        Thread t = new Thread(run);
        t.start();
    }

    private javax.swing.JTextField dataProviderField;

    private javax.swing.JCheckBox filterLanguageCheckbox;

    private javax.swing.JTextField fromField;

    private javax.swing.JButton importButton;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTextField languageField;

    private javax.swing.JButton listRecordsButton;

    private javax.swing.JTextField maxField;

    private javax.swing.JLabel numberLabel;

    private javax.swing.JTextField recordSetName;

    private javax.swing.JTable recordTable;

    private javax.swing.JTextField setField;

    private javax.swing.JCheckBox skipEmptyAbstracts;

    private javax.swing.JTextField untilField;

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized OaiPmhAccessTopComponent getDefault() {
        if (instance == null) {
            instance = new OaiPmhAccessTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the OaiPmhAccessTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized OaiPmhAccessTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(OaiPmhAccessTopComponent.class.getName()).warning("Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof OaiPmhAccessTopComponent) {
            return (OaiPmhAccessTopComponent) win;
        }
        Logger.getLogger(OaiPmhAccessTopComponent.class.getName()).warning("There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    static final class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return OaiPmhAccessTopComponent.getDefault();
        }
    }
}
