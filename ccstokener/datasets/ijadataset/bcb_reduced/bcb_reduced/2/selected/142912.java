package newgen.presentation.cataloguing;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 *
 * @author  Administrator
 */
public class AuthorityFilesValidation extends javax.swing.JDialog {

    /** Creates new form AuthorityFilesValidation */
    public AuthorityFilesValidation() {
        super();
        initComponents();
        this.setModal(true);
        this.setSize(500, 500);
        String[] col1 = { java.util.ResourceBundle.getBundle("Administration").getString("PersonalName"), java.util.ResourceBundle.getBundle("Administration").getString("FullerFormOfName"), java.util.ResourceBundle.getBundle("Administration").getString("Numeration"), java.util.ResourceBundle.getBundle("Administration").getString("Titles"), java.util.ResourceBundle.getBundle("Administration").getString("DatesAssociated") };
        this.dtmPersonalNameAE = new DefaultTableModel(col1, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col2 = { java.util.ResourceBundle.getBundle("Administration").getString("CorporateName"), java.util.ResourceBundle.getBundle("Administration").getString("SubOrdinateUnit") };
        this.dtmCorporateNameAE = new DefaultTableModel(col2, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col3 = { java.util.ResourceBundle.getBundle("Administration").getString("MeetingName"), java.util.ResourceBundle.getBundle("Administration").getString("LocationOfMeeting"), java.util.ResourceBundle.getBundle("Administration").getString("DateOfMeeting"), java.util.ResourceBundle.getBundle("Administration").getString("NumberOfPartSectionOfWork") };
        this.dtmMeetingNameAE = new DefaultTableModel(col3, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col4 = { java.util.ResourceBundle.getBundle("Administration").getString("TopicalTerm"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmTopicalTermSH = new DefaultTableModel(col4, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col5 = { java.util.ResourceBundle.getBundle("Administration").getString("PersonalName"), java.util.ResourceBundle.getBundle("Administration").getString("FullerFormOfName"), java.util.ResourceBundle.getBundle("Administration").getString("Numeration"), java.util.ResourceBundle.getBundle("Administration").getString("Titles"), java.util.ResourceBundle.getBundle("Administration").getString("DatesAssociated"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmPersonalNameSH = new DefaultTableModel(col5, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col6 = { java.util.ResourceBundle.getBundle("Administration").getString("CorporateName"), java.util.ResourceBundle.getBundle("Administration").getString("SubOrdinateUnit"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmCorporateNameSH = new DefaultTableModel(col6, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col7 = { java.util.ResourceBundle.getBundle("Administration").getString("MeetingName"), java.util.ResourceBundle.getBundle("Administration").getString("LocationOfMeeting"), java.util.ResourceBundle.getBundle("Administration").getString("DateOfMeeting"), java.util.ResourceBundle.getBundle("Administration").getString("NumberOfPartSectionOfWork"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmMeetingNameSH = new DefaultTableModel(col7, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col8 = { java.util.ResourceBundle.getBundle("Administration").getString("UniformTitle"), java.util.ResourceBundle.getBundle("Administration").getString("DateOfWork"), java.util.ResourceBundle.getBundle("Administration").getString("Version"), java.util.ResourceBundle.getBundle("Administration").getString("Language"), java.util.ResourceBundle.getBundle("Administration").getString("PartName"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmUniformTitleSH = new DefaultTableModel(col8, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col9 = { java.util.ResourceBundle.getBundle("Administration").getString("Title"), java.util.ResourceBundle.getBundle("Administration").getString("NumberOfPartSectionOfWork"), java.util.ResourceBundle.getBundle("Administration").getString("NameOfPartSectionOfWork"), java.util.ResourceBundle.getBundle("Administration").getString("VolumeNo"), java.util.ResourceBundle.getBundle("Administration").getString("ISSN") };
        this.dtmSeries = new DefaultTableModel(col9, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col10 = { java.util.ResourceBundle.getBundle("Administration").getString("UniformResourceLocator"), java.util.ResourceBundle.getBundle("Administration").getString("ElectronicFormatType") };
        this.dtmElectronicLocationATTCH = new DefaultTableModel(col10, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[] col11 = { java.util.ResourceBundle.getBundle("Administration").getString("GeographicName"), java.util.ResourceBundle.getBundle("Administration").getString("GeneralSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("ChronologicalSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("FormSubDivision"), java.util.ResourceBundle.getBundle("Administration").getString("GeographicalSubDivision") };
        this.dtmGNSH = new DefaultTableModel(col11, 0) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        newgen.presentation.NewGenMain.getAppletInstance().applyOrientation(this);
    }

    public void setAllInitialData(java.util.ArrayList al) {
        String reqxml = newgen.presentation.cataloguing.CataloguingXMLGenerator.getInstance().getAuthorityFilesMatchedData("3", al, newgen.presentation.NewGenMain.getAppletInstance().getCataloguingPool());
        try {
            java.net.URL url = new java.net.URL(ResourceBundle.getBundle("Administration").getString("ServerURL") + ResourceBundle.getBundle("Administration").getString("ServletSubPath") + "CatalogueRecordServlet");
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            java.io.OutputStream dos = urlconn.getOutputStream();
            dos.write(reqxml.getBytes());
            java.io.InputStream ios = urlconn.getInputStream();
            SAXBuilder saxb = new SAXBuilder();
            Document retdoc = saxb.build(ios);
            Element rootelement = retdoc.getRootElement();
            java.util.List list = rootelement.getChildren("Field");
            for (int i = 0; i < list.size(); i++) {
                Vector vsubmatchdata = new Vector(1, 1);
                String[] fielddata = new String[3];
                fielddata[0] = ((Element) list.get(i)).getChild("FieldId").getText();
                fielddata[1] = ((Element) list.get(i)).getChild("Name").getText();
                fielddata[2] = ((Element) list.get(i)).getChild("TableRowNo").getText();
                vsubmatchdata.addElement(fielddata);
                java.util.List lisubmatchlist = ((Element) list.get(i)).getChildren("Record");
                vsubmatchdata.addElement(lisubmatchlist);
                this.vfullmatchdata.addElement(vsubmatchdata);
            }
            Vector vsubmatchdata = (Vector) this.vfullmatchdata.elementAt(0);
            String[] fulldata = (String[]) vsubmatchdata.elementAt(0);
            this.tfAuthorityFileName.setText(this.getAuthorityFileName(fulldata[0]));
            this.tfMatchedTerm.setText(fulldata[1]);
            this.tfFieldNumber.setText(fulldata[0]);
            this.tableMatchesFound.setModel(this.getAuthorityFileTableModel(fulldata[0]));
            this.getAuthorityFileDataforTableModel(fulldata[0], (java.util.List) vsubmatchdata.elementAt(1));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jPanel1 = new javax.swing.JPanel();
        bback = new javax.swing.JButton();
        bnext = new javax.swing.JButton();
        bfinish = new javax.swing.JButton();
        bcancel = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        tfAuthorityFileName = new newgen.presentation.UnicodeTextField();
        jLabel2 = new javax.swing.JLabel();
        tfFieldNumber = new newgen.presentation.UnicodeTextField();
        jLabel3 = new javax.swing.JLabel();
        tfMatchedTerm = new newgen.presentation.UnicodeTextField();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        taOtherInformation = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableMatchesFound = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        cbUseMatch = new javax.swing.JCheckBox();
        setTitle(java.util.ResourceBundle.getBundle("Administration").getString("ValidateAuthorityFiles"));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        jPanel1.setBorder(new javax.swing.border.EtchedBorder());
        bback.setMnemonic('b');
        bback.setText(java.util.ResourceBundle.getBundle("Administration").getString("Back"));
        jPanel1.add(bback);
        bnext.setMnemonic('n');
        bnext.setText(java.util.ResourceBundle.getBundle("Administration").getString("Next"));
        bnext.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnextActionPerformed(evt);
            }
        });
        jPanel1.add(bnext);
        bfinish.setMnemonic('f');
        bfinish.setText(java.util.ResourceBundle.getBundle("Administration").getString("Finish"));
        jPanel1.add(bfinish);
        bcancel.setMnemonic('c');
        bcancel.setText(java.util.ResourceBundle.getBundle("Administration").getString("Cancel"));
        bcancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bcancelActionPerformed(evt);
            }
        });
        jPanel1.add(bcancel);
        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));
        jPanel3.setBorder(new javax.swing.border.EtchedBorder());
        jPanel5.setLayout(new java.awt.GridBagLayout());
        jPanel5.setPreferredSize(new java.awt.Dimension(453, 103));
        jLabel1.setText(java.util.ResourceBundle.getBundle("Administration").getString("AuthorityFileName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel5.add(jLabel1, gridBagConstraints);
        tfAuthorityFileName.setColumns(35);
        tfAuthorityFileName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        jPanel5.add(tfAuthorityFileName, gridBagConstraints);
        jLabel2.setText(java.util.ResourceBundle.getBundle("Administration").getString("FieldNumber"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel5.add(jLabel2, gridBagConstraints);
        tfFieldNumber.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        jPanel5.add(tfFieldNumber, gridBagConstraints);
        jLabel3.setText(java.util.ResourceBundle.getBundle("Administration").getString("MatchedTerm"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel5.add(jLabel3, gridBagConstraints);
        tfMatchedTerm.setColumns(35);
        tfMatchedTerm.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        jPanel5.add(tfMatchedTerm, gridBagConstraints);
        jPanel3.add(jPanel5);
        jPanel7.setLayout(new java.awt.BorderLayout());
        jPanel7.setBorder(new javax.swing.border.TitledBorder(java.util.ResourceBundle.getBundle("Administration").getString("OtherInformation")));
        jPanel7.setPreferredSize(new java.awt.Dimension(13, 150));
        taOtherInformation.setEditable(false);
        taOtherInformation.setLineWrap(true);
        taOtherInformation.setWrapStyleWord(true);
        jScrollPane4.setViewportView(taOtherInformation);
        jPanel7.add(jScrollPane4, java.awt.BorderLayout.CENTER);
        jPanel3.add(jPanel7);
        jPanel2.add(jPanel3);
        jPanel4.setLayout(new java.awt.BorderLayout());
        jPanel4.setBorder(new javax.swing.border.EtchedBorder());
        tableMatchesFound.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] {}));
        jScrollPane3.setViewportView(tableMatchesFound);
        jPanel4.add(jScrollPane3, java.awt.BorderLayout.CENTER);
        jPanel2.add(jPanel4);
        cbUseMatch.setText(java.util.ResourceBundle.getBundle("Administration").getString("UseSelectedMatch"));
        cbUseMatch.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jPanel8.add(cbUseMatch);
        jPanel2.add(jPanel8);
        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);
        pack();
    }

    private void bnextActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.butcount + 1 >= this.vfullmatchdata.size()) {
            butcount = 0;
        } else {
            butcount++;
        }
        System.out.println("Butcount: " + this.butcount + " Size: " + this.vfullmatchdata.size());
        this.clearTableModels();
        Vector vsubmatchdata = (Vector) this.vfullmatchdata.elementAt(butcount);
        String[] fulldata = (String[]) vsubmatchdata.elementAt(0);
        this.tfAuthorityFileName.setText(this.getAuthorityFileName(fulldata[0]));
        this.tfMatchedTerm.setText(fulldata[1]);
        this.tfFieldNumber.setText(fulldata[0]);
        this.tableMatchesFound.setModel(this.getAuthorityFileTableModel(fulldata[0]));
        this.getAuthorityFileDataforTableModel(fulldata[0], (java.util.List) vsubmatchdata.elementAt(1));
    }

    private void clearTableModels() {
        try {
            for (int i = 0; i < this.dtmCorporateNameAE.getRowCount(); i++) this.dtmCorporateNameAE.removeRow(0);
            for (int i = 0; i < this.dtmCorporateNameSH.getRowCount(); i++) this.dtmCorporateNameSH.removeRow(0);
            for (int i = 0; i < this.dtmGNSH.getRowCount(); i++) this.dtmGNSH.removeRow(0);
            for (int i = 0; i < this.dtmMeetingNameAE.getRowCount(); i++) this.dtmMeetingNameAE.removeRow(0);
            for (int i = 0; i < this.dtmMeetingNameSH.getRowCount(); i++) this.dtmMeetingNameSH.removeRow(0);
            for (int i = 0; i < this.dtmParallelTitleMEPT.getRowCount(); i++) this.dtmParallelTitleMEPT.removeRow(0);
            for (int i = 0; i < this.dtmPersonalNameAE.getRowCount(); i++) this.dtmPersonalNameAE.removeRow(0);
            for (int i = 0; i < this.dtmPersonalNameSH.getRowCount(); i++) this.dtmPersonalNameSH.removeRow(0);
            for (int i = 0; i < this.dtmSeries.getRowCount(); i++) this.dtmSeries.removeRow(0);
            for (int i = 0; i < this.dtmTopicalTermSH.getRowCount(); i++) this.dtmTopicalTermSH.removeRow(0);
            for (int i = 0; i < this.dtmUniformTitleSH.getRowCount(); i++) this.dtmUniformTitleSH.removeRow(0);
        } catch (Exception E) {
            System.out.println(E);
        }
    }

    private void bcancelActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
        dispose();
    }

    public String getAuthorityFileName(String field) {
        String retstr = "";
        if (field.equals("100")) retstr = "MAIN ENTRY--PERSONAL NAME";
        if (field.equals("110")) retstr = "MAIN ENTRY--CORPORATE NAME";
        if (field.equals("111")) retstr = "MAIN ENTRY--MEETING NAME";
        if (field.equals("130") | field.equals("240")) retstr = "MAIN ENTRY--UNIFORM TITLE";
        if (field.equals("700")) retstr = "ADDED ENTRY--PERSONAL NAME";
        if (field.equals("710")) retstr = "ADDED ENTRY--CORPORATE NAME ";
        if (field.equals("711")) retstr = "ADDED ENTRY--MEETING NAME ";
        if (field.equals("800")) retstr = "SERIES ADDED ENTRY--PERSONAL NAME";
        if (field.equals("810")) retstr = "SERIES ADDED ENTRY--CORPORATE NAME";
        if (field.equals("811")) retstr = "SERIES ADDED ENTRY--MEETING NAME";
        if (field.equals("600")) retstr = "SUBJECT ADDED ENTRY--PERSONAL NAME";
        if (field.equals("610")) retstr = "SUBJECT ADDED ENTRY--CORPORATE NAME";
        if (field.equals("611")) retstr = "SUBJECT ADDED ENTRY--MEETING NAME";
        if (field.equals("630")) retstr = "SUBJECT ADDED ENTRY--UNIFORM TITLE";
        if (field.equals("650")) retstr = "SUBJECT ADDED ENTRY--TOPICAL TERM";
        if (field.equals("651")) retstr = "SUBJECT ADDED ENTRY--GEOGRAPHIC NAME";
        if (field.equals("440")) retstr = "SERIES STATEMENT/ADDED ENTRY--TITLE";
        return retstr;
    }

    public javax.swing.table.DefaultTableModel getAuthorityFileTableModel(String field) {
        javax.swing.table.DefaultTableModel dtmret = null;
        if (field.equals("100") | field.equals("700") | field.equals("800")) dtmret = this.dtmPersonalNameAE;
        if (field.equals("110") | field.equals("710") | field.equals("810")) dtmret = this.dtmCorporateNameAE;
        if (field.equals("111") | field.equals("711") | field.equals("811")) dtmret = this.dtmMeetingNameAE;
        if (field.equals("130") | field.equals("240")) dtmret = this.dtmUniformTitleSH;
        if (field.equals("600")) dtmret = this.dtmPersonalNameSH;
        if (field.equals("610")) dtmret = this.dtmCorporateNameSH;
        if (field.equals("611")) dtmret = this.dtmMeetingNameSH;
        if (field.equals("630")) dtmret = this.dtmUniformTitleSH;
        if (field.equals("650")) dtmret = this.dtmTopicalTermSH;
        if (field.equals("651")) dtmret = this.dtmGNSH;
        if (field.equals("440")) dtmret = this.dtmSeries;
        return dtmret;
    }

    public void getAuthorityFileDataforTableModel(String field, java.util.List datalist) {
        System.out.println("Datalist: " + datalist);
        Object[] retobj = null;
        Vector vret = new Vector(1, 1);
        if (field.equals("100") | field.equals("700") | field.equals("800")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[5];
                retobj[0] = ((Element) datalist.get(i)).getChild("Name").getText();
                if (((Element) datalist.get(i)).getChild("Numeration") != null) retobj[2] = ((Element) datalist.get(i)).getChild("Numeration").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("Title");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                if (((Element) datalist.get(i)).getChild("Date") != null) retobj[4] = ((Element) datalist.get(i)).getChild("Date").getText();
                if (((Element) datalist.get(i)).getChild("FullForm") != null) retobj[1] = ((Element) datalist.get(i)).getChild("FullForm").getText();
                this.dtmPersonalNameAE.addRow(retobj);
            }
        }
        if (field.equals("110") | field.equals("710") | field.equals("810")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[2];
                retobj[0] = ((Element) datalist.get(i)).getChild("Name").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("SubUnit");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[1] = str;
                this.dtmCorporateNameAE.addRow(retobj);
            }
        }
        if (field.equals("111") | field.equals("711") | field.equals("811")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[4];
                retobj[0] = ((Element) datalist.get(i)).getChild("Name").getText();
                if (((Element) datalist.get(i)).getChild("Location") != null) retobj[1] = ((Element) datalist.get(i)).getChild("Location").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("PartNumber");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                if (((Element) datalist.get(i)).getChild("DateOfMeeting") != null) retobj[2] = ((Element) datalist.get(i)).getChild("DateOfMeeting").getText();
                this.dtmMeetingNameAE.addRow(retobj);
            }
        }
        if (field.equals("130") | field.equals("240")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[5];
                retobj[0] = ((Element) datalist.get(i)).getChild("Title").getText();
                if (((Element) datalist.get(i)).getChild("LanguageCode") != null) retobj[3] = ((Element) datalist.get(i)).getChild("LanguageCode").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("PartName");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                if (((Element) datalist.get(i)).getChild("DateOfWork") != null) retobj[1] = ((Element) datalist.get(i)).getChild("DateOfWork").getText();
                if (((Element) datalist.get(i)).getChild("Version") != null) retobj[2] = ((Element) datalist.get(i)).getChild("Version").getText();
                this.dtmPersonalNameAE.addRow(retobj);
            }
        }
        if (field.equals("600")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[9];
                retobj[0] = ((Element) datalist.get(i)).getChild("PersonalName").getText();
                if (((Element) datalist.get(i)).getChild("Numeration") != null) retobj[2] = ((Element) datalist.get(i)).getChild("Numeration").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("Title");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                if (((Element) datalist.get(i)).getChild("Date") != null) retobj[4] = ((Element) datalist.get(i)).getChild("Date").getText();
                if (((Element) datalist.get(i)).getChild("FullForm") != null) retobj[1] = ((Element) datalist.get(i)).getChild("FullForm").getText();
                li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[5] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[6] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[7] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[8] = str;
                this.dtmPersonalNameSH.addRow(retobj);
            }
        }
        if (field.equals("610")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[6];
                retobj[0] = ((Element) datalist.get(i)).getChild("Name").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("SubUnit");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[1] = str;
                li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[2] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[5] = str;
                this.dtmCorporateNameSH.addRow(retobj);
            }
        }
        if (field.equals("611")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[9];
                retobj[0] = ((Element) datalist.get(i)).getChild("Name").getText();
                if (((Element) datalist.get(i)).getChild("Location") != null) retobj[1] = ((Element) datalist.get(i)).getChild("Location").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("PartNumber");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                if (((Element) datalist.get(i)).getChild("DateOfMeeting") != null) retobj[2] = ((Element) datalist.get(i)).getChild("DateOfMeeting").getText();
                li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[5] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[6] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[7] = str;
                this.dtmMeetingNameSH.addRow(retobj);
            }
        }
        if (field.equals("630")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[9];
                retobj[0] = ((Element) datalist.get(i)).getChild("Title").getText();
                if (((Element) datalist.get(i)).getChild("LanguageCode") != null) retobj[3] = ((Element) datalist.get(i)).getChild("LanguageCode").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("PartName");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                if (((Element) datalist.get(i)).getChild("DateOfWork") != null) retobj[1] = ((Element) datalist.get(i)).getChild("DateOfWork").getText();
                if (((Element) datalist.get(i)).getChild("Version") != null) retobj[2] = ((Element) datalist.get(i)).getChild("Version").getText();
                li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[5] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[6] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[7] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[8] = str;
                this.dtmUniformTitleSH.addRow(retobj);
            }
        }
        if (field.equals("650")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[5];
                retobj[0] = ((Element) datalist.get(i)).getChild("TopicalTerm").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[1] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[2] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                this.dtmTopicalTermSH.addRow(retobj);
            }
        }
        if (field.equals("651")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[5];
                retobj[0] = ((Element) datalist.get(i)).getChild("GeographicName").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("GeneralSD");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[1] = str;
                li = ((Element) datalist.get(i)).getChildren("ChronologicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[2] = str;
                li = ((Element) datalist.get(i)).getChildren("FormSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[3] = str;
                li = ((Element) datalist.get(i)).getChildren("GeographicalSD");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getChild("Name").getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[4] = str;
                this.dtmGNSH.addRow(retobj);
            }
        }
        if (field.equals("440")) {
            for (int i = 0; i < datalist.size(); i++) {
                retobj = new Object[5];
                retobj[0] = ((Element) datalist.get(i)).getChild("Title").getText();
                if (((Element) datalist.get(i)).getChild("ISSN") != null) retobj[4] = ((Element) datalist.get(i)).getChild("ISSN").getText();
                java.util.List li = ((Element) datalist.get(i)).getChildren("PartNumber");
                String str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[1] = str;
                li = ((Element) datalist.get(i)).getChildren("PartName");
                str = "";
                for (int j = 0; j < li.size(); j++) {
                    if (j == 0) str = ((Element) li.get(j)).getText(); else {
                        str = str + ", " + ((Element) li.get(j)).getText();
                    }
                }
                retobj[2] = str;
                if (((Element) datalist.get(i)).getChild("VolumeNo") != null) retobj[3] = ((Element) datalist.get(i)).getChild("Date").getText();
                this.dtmSeries.addRow(retobj);
            }
        }
    }

    private javax.swing.JButton bback;

    private javax.swing.JButton bcancel;

    private javax.swing.JButton bfinish;

    private javax.swing.JButton bnext;

    private javax.swing.JCheckBox cbUseMatch;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel7;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JTextArea taOtherInformation;

    private javax.swing.JTable tableMatchesFound;

    private newgen.presentation.UnicodeTextField tfAuthorityFileName;

    private newgen.presentation.UnicodeTextField tfFieldNumber;

    private newgen.presentation.UnicodeTextField tfMatchedTerm;

    private DefaultTableModel dtmParallelTitleMEPT, dtmPersonalNameAE, dtmCorporateNameAE, dtmMeetingNameAE, dtmTopicalTermSH, dtmPersonalNameSH;

    private DefaultTableModel dtmCorporateNameSH, dtmMeetingNameSH, dtmUniformTitleSH, dtmSeries, dtmElectronicLocationATTCH, dtmGNSH;

    private Vector vfullmatchdata = new Vector(1, 1);

    private int butcount = 0;
}
