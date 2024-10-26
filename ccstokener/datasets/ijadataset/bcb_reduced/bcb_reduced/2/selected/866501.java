package org.gnf.seqtracs.seqretrival;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.sql.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.TimeZone;
import java.util.Vector;
import org.biojava.bio.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.gui.sequence.*;
import org.biojava.bio.program.abi.*;
import org.gnf.seqtracs.assembly.*;
import org.gnf.seqtracs.dbinterface.SeqDbInterface;
import org.gnf.seqtracs.utilities.Utilities;
import org.gnf.seqtracs.utilities.DirChooser;
import org.gnf.seqtracs.utilities.SimpleFileFilter;
import org.gnf.seqtracs.utilities.SimplePrinter;
import org.gnf.seqtracs.utilities.Univariate;
import org.gnf.seqtracs.seq.*;
import org.gnf.oracle.*;

/**
 * <p>Title: ProjectSeqsFrame</p>
 * <p>Description: Display of sequences in a sequencing project</p>
 * <p>Copyright: Copyright (c) 2002 GNF</p>
 * <p>Company: GNF</p>
 * @author Christian M. Zmasek (czmasek@gnf.org)
 * @version 1.0
 */
public class ProjectSeqsFrame extends JFrame {

    public ProjectSeqsFrame(final String db_user_name, final String db_user_pwd, final String db_driver_name, final String db_url, final String project_name, final ResubmitJFrame parent) {
        _db_user_name = db_user_name;
        _db_user_pwd = db_user_pwd;
        _db_driver_name = db_driver_name;
        _db_url = db_url;
        _per_plate = false;
        _model = new ProjectSeqsTableModel();
        _parent = parent;
        project_seqs_jtable = new JTable(_model);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            Connection connection = createDBConnection();
            jbInit(connection, project_name, -1);
        } catch (Exception e) {
            Utilities.unexpectedException(e, this, CONTACT);
        }
    }

    public ProjectSeqsFrame(final String db_user_name, final String db_user_pwd, final String db_driver_name, final String db_url, final int seq_plate_oid, final ResubmitJFrame parent) {
        _db_user_name = db_user_name;
        _db_user_pwd = db_user_pwd;
        _db_driver_name = db_driver_name;
        _db_url = db_url;
        _per_plate = true;
        _model = new PlateSeqsTableModel();
        _parent = parent;
        project_seqs_jtable = new JTable(_model);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            Connection connection = createDBConnection();
            jbInit(connection, "", seq_plate_oid);
        } catch (Exception e) {
            Utilities.unexpectedException(e, this, CONTACT);
        }
    }

    private Connection createDBConnection() throws SQLException, ClassNotFoundException {
        Connection connection = null;
        Class.forName(_db_driver_name);
        connection = DriverManager.getConnection(_db_url, _db_user_name, _db_user_pwd);
        connection.setAutoCommit(false);
        return connection;
    }

    /**
     *
     * @param connection
     * @param project_name
     * @param seq_plate_oid
     * @throws Exception
     */
    private void jbInit(final Connection connection, final String project_name, final int seq_plate_oid) throws Exception {
        if (project_name != null && project_name.length() > 0 && _per_plate) {
            Utilities.unexpectedException(new Exception("Cannot set both project name and seq plate oid."), this, CONTACT);
            return;
        }
        _project_name = project_name;
        if (connection == null) {
            Utilities.unexpectedException(new Exception("Database connection is null."), this, CONTACT);
            return;
        } else {
            _connection = connection;
            try {
                setSeqDbInterface(new SeqDbInterface(connection));
            } catch (Exception e) {
                Utilities.unexpectedException(e, this, CONTACT);
            }
        }
        String[] traces = null;
        try {
            if (_per_plate) {
                traces = getSeqDbInterface().getTraceFilesInSeqPlate(seq_plate_oid);
            } else {
                traces = getSeqDbInterface().getTraceFilesInProject(project_name);
            }
        } catch (Exception e) {
            Utilities.unexpectedException(e, this, CONTACT);
            return;
        }
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(borderLayout1);
        setSize(new Dimension(1200, 600));
        if (_per_plate) {
            _platename = getSeqDbInterface().retrievePlateName(seq_plate_oid);
            setTitle("Sequences Per Plate: " + _platename);
        } else {
            setTitle("Sequences In Project: " + _project_name);
        }
        jMenuFile.setText("File");
        jMenuFileExit.setText("Exit");
        jMenuFileExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuFileExit_actionPerformed(e);
            }
        });
        jMenuFileSaveTable.setText("Save Table As...");
        jMenuFileSaveTable.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuFileSaveTable_actionPerformed(e);
            }
        });
        jMenuFilePrintTable.setText("Print Table");
        jMenuFilePrintTable.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuFilePrintTable_actionPerformed(e);
            }
        });
        jMenuHelp.setText("Help");
        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuHelpAbout_actionPerformed(e);
            }
        });
        jMenuHelpHelp.setText("Help");
        jMenuHelpHelp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jMenuHelpHelp_actionPerformed(e);
            }
        });
        instructionMenuItem.setText("Instructions");
        instructionMenuItem.setAction(instructionMenuItemAction);
        saveSelectedSeqsButton.setToolTipText("Click here to save all checked sequences into a directory");
        saveSelectedSeqsButton.setText("Complete");
        saveSelectedSeqsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveSelectedSeqsButton_actionPerformed(e);
            }
        });
        saveTracesButton.setToolTipText("Click here to save the tracefiles for all checked sequences into " + "a directory");
        saveTracesButton.setText("Tracefiles");
        saveTracesButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveTracesButton_actionPerformed(e);
            }
        });
        unselectAllbutton.setToolTipText("Click here to uncheck all sequences");
        unselectAllbutton.setText("Uncheck All");
        unselectAllbutton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                unselectAllbutton_actionPerformed(e);
            }
        });
        selectAllButton.setToolTipText("Click here to check all sequences");
        selectAllButton.setText("Check All");
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectAllButton_actionPerformed(e);
            }
        });
        inverseSelectionButton.setToolTipText("Click here to inverse the check marks");
        inverseSelectionButton.setText("Inverse");
        inverseSelectionButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                inverseSelectionButton_actionPerformed(e);
            }
        });
        selectSelectedButton.setToolTipText("Click here to check the highlighted sequences");
        selectSelectedButton.setText("Check Highlighted");
        selectSelectedButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectSelectedButton_actionPerformed(e);
            }
        });
        saveSelectedHQSButton.setToolTipText("Click here to save the high quality regions of all checked sequences " + "into a directory");
        saveSelectedHQSButton.setText("High Quality");
        saveSelectedHQSButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveSelectedHQSButton_actionPerformed(e);
            }
        });
        saveSelectedHQInsButton.setToolTipText("Click here to save the non vector sequence within the high quality regions of all checked sequences " + "into a directory");
        saveSelectedHQInsButton.setText("HQ Insert");
        saveSelectedHQInsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveSelectedHQInsButton_actionPerformed(e);
            }
        });
        resubmitSelectedButton.setToolTipText("Click here to resumbit the selected sequences");
        resubmitSelectedButton.setText("Resubmit");
        resubmitSelectedButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                resubmitSelectedButton_actionPerformed(e);
            }
        });
        troubleshootButton.setToolTipText("Click here to get troubleshooting information");
        troubleshootButton.setText("Troubleshooting");
        troubleshootButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                troubleshootButton_actionPerformed(e);
            }
        });
        assembleSelectedButton.setToolTipText("Click here to add the selected sequences to an assembly project");
        assembleSelectedButton.setText("Add To Assembly");
        assembleSelectedButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                assembleSelectedButton_actionPerformed(e);
            }
        });
        jPanel1.setLayout(gridLayout1);
        savePanel1.setLayout(gridbag);
        savePanel2.setLayout(new FlowLayout());
        checkPanel.setLayout(new FlowLayout());
        gridLayout1.setColumns(1);
        gridLayout1.setRows(2);
        saveAsFastaCheckBox.setBorder(null);
        saveSingleFastaCheckBox.setBorder(null);
        saveWithTemplatePrimerInNameCheckBox.setToolTipText("Check this box to add template and primer information to the name of saved files");
        saveWithTemplatePrimerInNameCheckBox.setText(" Rxn in Name ");
        saveWithTemplatePrimerInNameCheckBox.setSelected(true);
        saveAsFastaCheckBox.setToolTipText("Check this box to save (text-) sequences in FASTA format (> description line)");
        saveAsFastaCheckBox.setText(" Save Fasta ");
        saveAsFastaCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!saveAsFastaCheckBox.isSelected()) {
                    saveSingleFastaCheckBox.setSelected(false);
                }
            }
        });
        saveSingleFastaCheckBox.setToolTipText("Check to save sequences in a single file.");
        saveSingleFastaCheckBox.setText(" One File ");
        saveSingleFastaCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (saveSingleFastaCheckBox.isSelected()) {
                    saveAsFastaCheckBox.setSelected(true);
                }
            }
        });
        savePanel2.setBorder(BorderFactory.createEtchedBorder());
        saveLabel.setText("Save :");
        checkPanel.setBorder(BorderFactory.createEtchedBorder());
        savePanel2.add(saveLabel, null);
        jMenuFile.add(jMenuFileSaveTable);
        jMenuFile.add(jMenuFilePrintTable);
        jMenuFile.add(new JSeparator());
        jMenuFile.add(jMenuFileExit);
        jMenuHelp.add(jMenuHelpAbout);
        jMenuHelp.add(jMenuHelpHelp);
        jMenuHelp.add(instructionMenuItem);
        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenuHelp);
        contentPane.add(jPanel1, BorderLayout.SOUTH);
        checkPanel.add(selectAllButton, null);
        checkPanel.add(selectSelectedButton, null);
        checkPanel.add(inverseSelectionButton, null);
        checkPanel.add(unselectAllbutton, null);
        jPanel1.add(checkPanel, null);
        jPanel1.add(savePanel1, null);
        savePanel1.add(savePanel2, null);
        savePanel2.add(saveSelectedSeqsButton, null);
        savePanel2.add(saveSelectedHQSButton, null);
        savePanel2.add(saveSelectedHQInsButton, null);
        savePanel2.add(saveTracesButton, null);
        savePanel2.add(saveWithTemplatePrimerInNameCheckBox, null);
        savePanel2.add(saveAsFastaCheckBox, null);
        savePanel2.add(saveSingleFastaCheckBox, null);
        savePanel1.add(assembleSelectedButton, null);
        savePanel1.add(resubmitSelectedButton, null);
        savePanel1.add(troubleshootButton, null);
        contentPane.add(jScrollPane1, BorderLayout.CENTER);
        menuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    showSequence(_row);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    showTraces(_row);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menuItem3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    showQualHisto(_row);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        popupMenu.add(menuItem1);
        popupMenu.add(menuItem2);
        popupMenu.add(menuItem3);
        Object[][] data = getData(traces);
        if (data != null) {
            seqsTableInit(data);
        }
        jScrollPane1.getViewport().add(project_seqs_jtable, null);
        this.setJMenuBar(jMenuBar1);
    }

    private void jMenuFileExit_actionPerformed(ActionEvent e) {
        dispose();
    }

    private void jMenuFileSaveTable_actionPerformed(ActionEvent e) {
        saveTableToFile();
    }

    private void jMenuFilePrintTable_actionPerformed(ActionEvent e) {
        try {
            printTable();
        } catch (Exception ex) {
            Utilities.unexpectedException(ex, this, CONTACT);
        }
    }

    private void jMenuHelpAbout_actionPerformed(ActionEvent e) {
        ProjectSeqsFrame_AboutBox dlg = new ProjectSeqsFrame_AboutBox(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.show();
    }

    private void jMenuHelpHelp_actionPerformed(ActionEvent e) {
        SeqFrame_HelpBox dlg = new SeqFrame_HelpBox(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.show();
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            jMenuFileExit_actionPerformed(null);
        }
    }

    /**
 *
 * @param e
 */
    private void saveSelectedSeqsButton_actionPerformed(ActionEvent e) {
        if (!areCheckBoxesChecked()) {
            return;
        }
        File path = getDirFile();
        saveFiles(path, false, false, false);
    }

    /**
 *
 * @param e
 */
    private void saveSelectedHQSButton_actionPerformed(ActionEvent e) {
        if (!areCheckBoxesChecked()) {
            return;
        }
        File path = getDirFile();
        saveFiles(path, true, false, false);
    }

    /**
     * set up and display the directory selection window
     *
     * @return DirChooser
     */
    private DirChooser getDirChooser() {
        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        DirChooser dc = new DirChooser();
        dc.setFileSelectionMode(dc.DIRECTORIES_ONLY);
        System.setSecurityManager(backup);
        dc.setDialogTitle("Select DIRECTORY to save into");
        return (dc);
    }

    /**
 * process the response to the dirchooser
 * @return File which is the directory path to save in
 */
    private File getDirFile() {
        DirChooser dc = getDirChooser();
        int response;
        while (true) {
            response = dc.showDialog(this, "Use Dir");
            if (response == JFileChooser.CANCEL_OPTION) {
                return (null);
            }
            if (dc.getSelectedFile().isDirectory()) {
                return (dc.getSelectedFile());
            } else {
                JOptionPane.showMessageDialog(this, "Directory " + dc.getSelectedFile().getAbsolutePath() + " does not exist. Press Create Folder Icon to create\na new folder.\n\n" + "NOTE: You cannot create folders in Windows XP in certain locations\n" + "such as the Desktop, My Documents, or the top level of any drive.\n" + "You must either move to a different folder and create the new folder\n" + "or create the folder using Windows XP.");
            }
        }
    }

    /**
 *
 * @param e
 */
    private void saveSelectedHQInsButton_actionPerformed(ActionEvent e) {
        if (!areCheckBoxesChecked()) {
            return;
        }
        File path = getDirFile();
        saveFiles(path, false, true, false);
    }

    /**
 *
 * @param e
 */
    private void saveTracesButton_actionPerformed(ActionEvent e) {
        if (!areCheckBoxesChecked()) {
            return;
        }
        File path = getDirFile();
        saveFiles(path, false, false, true);
    }

    /**
 *
 * @return whether any sequences are selected
 */
    private boolean areCheckBoxesChecked() {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex();
        boolean checked_some = false;
        for (int row = 0; row < project_seqs_jtable.getRowCount(); ++row) {
            if (((Boolean) model.getValueAt(row, save_column)).booleanValue()) {
                checked_some = true;
                break;
            }
        }
        if (!checked_some) {
            Utilities.message("Need to check the sequences which should be saved", this);
        }
        return checked_some;
    }

    private void resubmitSelectedButton_actionPerformed(ActionEvent e) {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), trace_name_column = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex();
        Vector oids_vector = new Vector();
        boolean checked_some = false;
        for (int row = 0; row < project_seqs_jtable.getRowCount(); ++row) {
            if (((Boolean) model.getValueAt(row, save_column)).booleanValue()) {
                checked_some = true;
                String trace_name = (String) model.getValueAt(row, trace_name_column);
                try {
                    oids_vector.add(new Integer(getSeqDbInterface().retrieveSeqReactionOid(trace_name)));
                } catch (Exception ee) {
                    Utilities.unexpectedException(ee, this, CONTACT);
                }
            }
        }
        if (oids_vector.size() > 0) {
            _parent.resubmitSeqs(Utilities.vectorToIntArray(oids_vector));
        } else if (!checked_some) {
            Utilities.message("Need to check the sequences which should be resubmitted for sequencing", this);
        }
    }

    private void troubleshootButton_actionPerformed(ActionEvent e) {
        String SERVER = SeqtracsConfig.SERVER;
        new Browser("http://" + SERVER + "/trouble.htm", "Troubleshooting").setVisible(true);
    }

    private void assembleSelectedButton_actionPerformed(ActionEvent e) {
        boolean checked_some = false;
        try {
            int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), trace_name_column = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex();
            for (int row = 0; row < project_seqs_jtable.getRowCount(); ++row) {
                if (((Boolean) _model.getValueAt(row, save_column)).booleanValue()) {
                    checked_some = true;
                    break;
                }
            }
            if (!checked_some) {
                Utilities.message("Need to check the sequences which should be added to the assembly", this);
                return;
            }
            AssemblyProject ap = new AssemblyProject("" + new java.util.Date().getTime(), 14, new java.sql.Date(new java.util.Date().getTime()), _db_driver_name, _db_url, _db_user_name, _db_user_pwd);
            String trace_name = "";
            for (int row = 0; row < project_seqs_jtable.getRowCount(); ++row) {
                if (((Boolean) _model.getValueAt(row, save_column)).booleanValue()) {
                    checked_some = true;
                    trace_name = (String) _model.getValueAt(row, trace_name_column);
                    int oid = getSeqDbInterface().retrieveBSOid(trace_name);
                    System.out.println("#" + oid);
                    ap.addRawSeqOid(oid);
                }
            }
            AssemblyProjectFrame apf = new AssemblyProjectFrame();
            System.out.println(ap.toString());
            apf.setAssemblyProject(ap);
            apf.setSize(800, 600);
            apf.setVisible(true);
        } catch (Exception ex) {
            Utilities.unexpectedException(ex, this, CONTACT);
        }
    }

    /**
 *
 * @param path
 * @param hqs_only
 * @param ins_only
 * @param save_trace_file
 */
    private void saveFiles(File path, boolean hqs_only, boolean ins_only, boolean save_trace_file) {
        if (path == null) {
            return;
        }
        TableModel model = project_seqs_jtable.getModel();
        int successes = 0, save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), trace_name_column = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex(), template_name_column = project_seqs_jtable.getColumn(_model.TEMPLATE_COLUMN_NAME).getModelIndex(), primer_name_column = project_seqs_jtable.getColumn(_model.PRIMER_COLUMN_NAME).getModelIndex(), path_column = project_seqs_jtable.getColumn(_model.PATH_COLUMN_NAME).getModelIndex(), project_column = -1;
        if (_per_plate) {
            project_column = project_seqs_jtable.getColumn(_model.PROJECT_COLUMN_NAME).getModelIndex();
        }
        boolean appendfile = false;
        boolean yesToAll = false;
        Object[] jOptions = { "Yes", "Yes to All", "No", "Cancel" };
        final int YES = 0;
        final int YESALL = 1;
        final int NO = 2;
        final int CANCEL = 3;
        String trace_name = "", seq = "", template_name = "", primer_name = "", trace_path = "", fasta_annot = "", project = "";
        File file = null;
        String mode = "";
        if (saveSingleFastaCheckBox.isSelected()) {
            saveAsFastaCheckBox.setSelected(true);
        }
        if (hqs_only) {
            mode = "[high qual only]";
        } else if (ins_only) {
            mode = "[high qual insert only]";
        } else if (save_trace_file) {
            saveAsFastaCheckBox.setSelected(false);
            saveSingleFastaCheckBox.setSelected(false);
        } else {
            mode = "[complete sequence]";
        }
        F: for (int row = 0; row < project_seqs_jtable.getRowCount(); ++row) {
            appendfile = false;
            if (((Boolean) model.getValueAt(row, save_column)).booleanValue()) {
                trace_name = (String) model.getValueAt(row, trace_name_column);
                template_name = (String) model.getValueAt(row, template_name_column);
                primer_name = (String) model.getValueAt(row, primer_name_column);
                trace_path = (String) model.getValueAt(row, path_column);
                if (_per_plate) {
                    project = (String) model.getValueAt(row, project_column);
                } else {
                    project = _project_name;
                }
                fasta_annot = trace_name + " " + mode + " project=" + project + " trace path=" + trace_path + " template=" + template_name + " primer=" + primer_name;
                String destination_name = new String(trace_name);
                if (saveWithTemplatePrimerInNameCheckBox.isSelected()) {
                    destination_name = template_name + "_" + primer_name + "_" + destination_name;
                }
                if (saveSingleFastaCheckBox.isSelected()) {
                    if (row == 0) {
                        appendfile = false;
                        destination_name = "fasta." + SEQ_TXT_FILE_SUFFIX;
                        file = new File(path, destination_name);
                    } else {
                        appendfile = true;
                    }
                } else {
                    if (save_trace_file) {
                        file = new File(path, destination_name + "." + TRACE_FILE_SUFFIX);
                    } else {
                        file = new File(path, destination_name + "." + SEQ_TXT_FILE_SUFFIX);
                    }
                }
                if (file.isDirectory()) {
                    JOptionPane.showMessageDialog(this, file + " is a directory.", "Aborting File Save Process", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!yesToAll && file.exists() && ((!saveSingleFastaCheckBox.isSelected()) || (saveSingleFastaCheckBox.isSelected() && row == 0))) {
                    int fileOverwriteChoice = JOptionPane.showOptionDialog(this, file + " already exists.\n" + "Overwrite?", "File Already Exists", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, jOptions, jOptions[2]);
                    if (fileOverwriteChoice == NO) {
                        int j = JOptionPane.showConfirmDialog(this, file + " already exists.\n" + "Save under a different name?", "Save Under a Different Name?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (j == JOptionPane.NO_OPTION) {
                            continue F;
                        } else {
                            boolean done = false;
                            while (!done) {
                                String new_name = JOptionPane.showInputDialog(this, "Enter a new name for " + file, "Save Under a Different Name", JOptionPane.DEFAULT_OPTION);
                                if (new_name == null) {
                                    continue F;
                                } else if (new_name.length() > 0) {
                                    if (save_trace_file) {
                                        if (!new_name.endsWith("." + TRACE_FILE_SUFFIX)) {
                                            new_name += "." + TRACE_FILE_SUFFIX;
                                        }
                                    } else {
                                        if (!new_name.endsWith("." + SEQ_TXT_FILE_SUFFIX)) {
                                            new_name += "." + SEQ_TXT_FILE_SUFFIX;
                                        }
                                    }
                                    File new_file = new File(file.getParent(), new_name);
                                    if (!new_file.exists()) {
                                        file = new_file;
                                        done = true;
                                    }
                                }
                            }
                        }
                    } else if (fileOverwriteChoice == CANCEL) {
                        break F;
                    } else if (fileOverwriteChoice == YESALL) {
                        yesToAll = true;
                    }
                }
                if (!save_trace_file) {
                    try {
                        seq = getSeqDbInterface().retrieveSeq(trace_name);
                    } catch (Exception ee) {
                        Utilities.unexpectedException(ee, this, CONTACT);
                    }
                }
                if (save_trace_file) {
                    successes += writeTraceFile(file, trace_name, trace_path);
                } else if (ins_only) {
                    SequenceFeature sf = null;
                    try {
                        sf = getSeqDbInterface().retrieveSequenceFeature(trace_name, CrossMatchOutput.NAME);
                    } catch (Exception ee) {
                        Utilities.unexpectedException(ee, this, CONTACT);
                    }
                    int nvf = sf.getFirst() - 1, nvl = sf.getLast() - 1;
                    if (nvf != nvl) {
                        successes += writeSequence(file, seq, fasta_annot, true, nvf, nvl, appendfile);
                    }
                } else if (hqs_only) {
                    SequenceFeature sf = null;
                    try {
                        sf = getSeqDbInterface().retrieveSequenceFeature(trace_name, QualSequence.HSR_NAME);
                    } catch (Exception ee) {
                        Utilities.unexpectedException(ee, this, CONTACT);
                    }
                    int hqf = sf.getFirst() - 1, hql = sf.getLast() - 1;
                    if (hqf != hql) {
                        successes += writeSequence(file, seq, fasta_annot, true, hqf, hql, appendfile);
                    }
                } else {
                    successes += writeSequence(file, seq, fasta_annot, false, 0, 0, appendfile);
                }
            }
        }
        if (successes > 0) {
            JOptionPane.showMessageDialog(this, "Saved " + successes + " sequences into " + "directory \"" + path + "\".", "Files Saved", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save files into " + "directory \"" + path + "\".", "No File Saved", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *
     * @param file
     * @param seq
     * @param fasta_annotation
     * @param subsequence
     * @param first
     * @param last
     * @param appendfile whether to create a new file (false) or add to current file (true)
     * @return some int (success? 0 is fail, 1 is success)
     */
    private int writeSequence(File file, String seq, String fasta_annotation, boolean subsequence, int first, int last, boolean appendfile) {
        try {
            if (file == null) {
                return 0;
            }
            FileWriter out = new FileWriter(file, appendfile);
            if (saveAsFastaCheckBox.isSelected()) {
                out.write(">" + fasta_annotation + "\r\n");
            }
            if (subsequence) {
                out.write(seq.substring(first, last + 1));
            } else {
                out.write(seq);
            }
            if (saveAsFastaCheckBox.isSelected()) {
                out.write("\r\n");
            }
            out.close();
        } catch (IOException e) {
            Utilities.unexpectedException(e, this, CONTACT);
        }
        return 1;
    }

    private int writeTraceFile(final File destination_file, final String trace_file_name, final String trace_file_path) {
        URL url = null;
        BufferedInputStream is = null;
        FileOutputStream fo = null;
        BufferedOutputStream os = null;
        int b = 0;
        if (destination_file == null) {
            return 0;
        }
        try {
            url = new URL("http://" + trace_file_path + "/" + trace_file_name);
            is = new BufferedInputStream(url.openStream());
            fo = new FileOutputStream(destination_file);
            os = new BufferedOutputStream(fo);
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            os.flush();
            is.close();
            os.close();
        } catch (Exception e) {
            System.err.println(url.toString());
            Utilities.unexpectedException(e, this, CONTACT);
            return 0;
        }
        return 1;
    }

    private void printTable() throws Exception {
        SimplePrinter p = new SimplePrinter(this, "Demo P", 3, 0.5, 2.5, 0.5, 2.5);
        if (p == null) {
            return;
        }
        PrintWriter out = new PrintWriter(p);
        int ccmo = project_seqs_jtable.getColumnCount() - 1, ccmt = ccmo - 1;
        String s = "";
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        df.setTimeZone(TimeZone.getDefault());
        String time = df.format(new java.util.Date());
        p.setFontStyle(Font.BOLD);
        out.write(getTitle());
        out.write("   [printed on: " + time + "]");
        out.println();
        for (int i = 0; i < ccmo; ++i) {
            s = _model.getColumnNames()[i];
            s = adjustLengthForPrinting(s, i);
            out.write(s);
        }
        out.println();
        for (int i = 0; i < ccmo; ++i) {
            s = "------------------------------------------------------";
            s = adjustLengthForPrinting(s, i);
            out.write(s);
        }
        out.println();
        p.setFontStyle(Font.PLAIN);
        for (int i = 0; i < project_seqs_jtable.getRowCount(); ++i) {
            for (int j = 0; j < ccmo; ++j) {
                s = project_seqs_jtable.getValueAt(i, j).toString();
                s = adjustLengthForPrinting(s, j);
                out.write(s);
            }
            out.println();
        }
        out.close();
    }

    private String adjustLengthForPrinting(String s, int col) {
        int CELL_SIZE_SHORT = 7, CELL_SIZE_MEDIUM = 18, CELL_SIZE_LONG = 54;
        int max = 0;
        String cn = _model.getColumnName(col);
        if (cn == _model.PATH_COLUMN_NAME || cn == _model.TRACE_COLUMN_NAME) {
            max = CELL_SIZE_LONG;
        } else if (cn == _model.NO_COLUMN_NAME || cn == _model.AVERAGE_QUAL_NAME || cn == _model.HQL_COLUMN_NAME || cn == _model.LENGTH_COLUMN_NAME || cn == _model.PNC_COLUMN_NAME || cn == _model.SCORE_COLUMN_NAME) {
            max = CELL_SIZE_SHORT;
        } else {
            max = CELL_SIZE_MEDIUM;
        }
        int diff = max - s.length();
        if (diff < 0) {
            s = s.substring(0, max - 0);
        } else if (diff > 0) {
            for (int z = 0; z < diff; ++z) {
                s += " ";
            }
        }
        s += "  ";
        return s;
    }

    private void saveTableToFile() {
        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        JFileChooser fc = new JFileChooser();
        System.setSecurityManager(backup);
        fc.setDialogTitle("Save Table as Excel File (." + TABLE_FILE_SUFFIX + ")");
        SimpleFileFilter filter = new SimpleFileFilter();
        filter.addExtension(TABLE_FILE_SUFFIX);
        filter.setDescription("Excel Files");
        fc.setFileFilter(filter);
        if (_per_plate) {
            fc.setSelectedFile(new File(_platename));
        } else {
            fc.setSelectedFile(new File(_project_name));
        }
        if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(this)) {
            String save_file_name = fc.getSelectedFile().getPath();
            this.repaint();
            if (!save_file_name.endsWith("." + TABLE_FILE_SUFFIX)) {
                save_file_name += "." + TABLE_FILE_SUFFIX;
            }
            saveTableToFileWriter(save_file_name);
        } else {
            this.repaint();
        }
    }

    private void saveTableToFileWriter(String save_file_name) {
        try {
            if (save_file_name == null || save_file_name.equals("")) {
                return;
            }
            File file = new File(save_file_name);
            FileWriter out = new FileWriter(file);
            int ccmo = project_seqs_jtable.getColumnCount() - 1, ccmt = ccmo - 1;
            for (int i = 1; i < ccmo; ++i) {
                out.write(_model.getColumnNames()[i]);
                if (i != ccmt) {
                    out.write("\t");
                }
            }
            out.write("\r\n");
            for (int i = 0; i < project_seqs_jtable.getRowCount(); ++i) {
                for (int j = 1; j < ccmo; ++j) {
                    out.write(project_seqs_jtable.getValueAt(i, j).toString());
                    if (j != ccmt) {
                        out.write("\t");
                    }
                }
                out.write("\r\n");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            Utilities.unexpectedException(e, this, CONTACT);
        }
    }

    private void unselectAllbutton_actionPerformed(ActionEvent e) {
        unSelectAll();
    }

    private void selectAllButton_actionPerformed(ActionEvent e) {
        selectAll();
    }

    private void inverseSelectionButton_actionPerformed(ActionEvent e) {
        inverseSelection();
    }

    private void selectSelectedButton_actionPerformed(ActionEvent e) {
        selectSelectedColumns();
    }

    private void copyTableDataFrom(Object[][] data) {
        for (int row = 0; row < data.length; ++row) {
            copyTableRowDataFrom(data[row], row);
        }
    }

    private void copyTableRowDataFrom(Object[] row_data, int row) {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex();
        model.setValueAt("" + (row + 1), row, 0);
        model.setValueAt(Boolean.FALSE, row, save_column);
        for (int col = 0; col < row_data.length; ++col) {
            model.setValueAt(row_data[col], row, col + 1);
        }
    }

    private void selectAll() {
        setSaveColumn(Boolean.TRUE);
    }

    private void unSelectAll() {
        setSaveColumn(Boolean.FALSE);
    }

    private void setSaveColumn(Boolean bool) {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), row_count = project_seqs_jtable.getRowCount();
        for (int row = 0; row < row_count; ++row) {
            model.setValueAt(bool, row, save_column);
        }
    }

    private void inverseSelection() {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), row_count = project_seqs_jtable.getRowCount();
        for (int row = 0; row < row_count; ++row) {
            boolean b = ((Boolean) model.getValueAt(row, save_column)).booleanValue();
            model.setValueAt(new Boolean(!b), row, save_column);
        }
    }

    private void selectSelectedColumns() {
        TableModel model = project_seqs_jtable.getModel();
        int save_column = project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).getModelIndex(), row_count = project_seqs_jtable.getRowCount();
        for (int row = 0; row < row_count; ++row) {
            if (project_seqs_jtable.isRowSelected(row)) {
                model.setValueAt(Boolean.TRUE, row, save_column);
            }
        }
    }

    private void seqsTableInit(Object[][] data) {
        copyTableDataFrom(data);
        data = null;
        project_seqs_jtable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                int row = project_seqs_jtable.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2) {
                    if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
                        e.consume();
                        try {
                            showTraces(row);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        e.consume();
                        try {
                            showSequence(row);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else if (e.getClickCount() == 1) {
                    if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
                        tableMouseLeftClicked(e, row);
                        e.consume();
                    }
                }
                e.consume();
            }
        });
        final ProjectSeqsTableSortDecorator sort_decorator = new ProjectSeqsTableSortDecorator(_model);
        project_seqs_jtable.setModel(sort_decorator);
        project_seqs_jtable.getColumn(_model.SAVE_COLUMN_NAME).setMaxWidth(_model.SAVE_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.NO_COLUMN_NAME).setMaxWidth(_model.NO_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.DATE_COLUMN_NAME).setMaxWidth(_model.DATE_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.LENGTH_COLUMN_NAME).setMaxWidth(_model.LENGTH_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.HQL_COLUMN_NAME).setMaxWidth(_model.HQL_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.PNC_COLUMN_NAME).setMaxWidth(_model.PNC_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.SCORE_COLUMN_NAME).setMaxWidth(_model.SCORE_COLUMN_WIDTH);
        project_seqs_jtable.getColumn(_model.AVERAGE_QUAL_NAME).setMaxWidth(_model.AVERAGE_QUAL_WIDTH);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(new Color(230, 230, 230));
        project_seqs_jtable.getColumn(_model.NO_COLUMN_NAME).setCellRenderer(r);
        JTableHeader hdr = (JTableHeader) project_seqs_jtable.getTableHeader();
        hdr.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                TableColumnModel tcm = project_seqs_jtable.getColumnModel();
                int vc = tcm.getColumnIndexAtX(e.getX());
                int mc = project_seqs_jtable.convertColumnIndexToModel(vc);
                if (mc != project_seqs_jtable.getColumn(_model.NO_COLUMN_NAME).getModelIndex()) {
                    sort_decorator.sort(mc);
                }
            }
        });
        sort_decorator.sort(project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex());
        project_seqs_jtable.doLayout();
    }

    private void tableMouseLeftClicked(MouseEvent e, int row) {
        _row = row;
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showSequence(int row) throws Exception {
        int temp_index = project_seqs_jtable.getColumn(_model.TEMPLATE_COLUMN_NAME).getModelIndex(), prim_index = project_seqs_jtable.getColumn(_model.PRIMER_COLUMN_NAME).getModelIndex(), trac_index = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex(), path_index = project_seqs_jtable.getColumn(_model.PATH_COLUMN_NAME).getModelIndex(), length_index = project_seqs_jtable.getColumn(_model.LENGTH_COLUMN_NAME).getModelIndex(), hq_length_index = project_seqs_jtable.getColumn(_model.HQL_COLUMN_NAME).getModelIndex(), hq_score_sum_index = project_seqs_jtable.getColumn(_model.SCORE_COLUMN_NAME).getModelIndex(), perc_no_call_index = project_seqs_jtable.getColumn(_model.PNC_COLUMN_NAME).getModelIndex(), date_index = project_seqs_jtable.getColumn(_model.DATE_COLUMN_NAME).getModelIndex();
        String trace_name = "" + project_seqs_jtable.getValueAt(row, trac_index), trace_path = "" + project_seqs_jtable.getValueAt(row, path_index), template_name = "" + project_seqs_jtable.getValueAt(row, temp_index), primer_name = "" + project_seqs_jtable.getValueAt(row, prim_index), length = "" + project_seqs_jtable.getValueAt(row, length_index), hq_length = "" + project_seqs_jtable.getValueAt(row, hq_length_index), hq_score_sum = "" + project_seqs_jtable.getValueAt(row, hq_score_sum_index), perc_no_call = "" + project_seqs_jtable.getValueAt(row, perc_no_call_index), date = "" + project_seqs_jtable.getValueAt(row, date_index), projectname = "", platename = "";
        if (project_seqs_jtable.getColumnName(project_seqs_jtable.getSelectedColumn()).equals(_model.SAVE_COLUMN_NAME)) {
            return;
        }
        if (_per_plate) {
            platename = _platename;
            int index = project_seqs_jtable.getColumn(_model.PROJECT_COLUMN_NAME).getModelIndex();
            projectname = "" + project_seqs_jtable.getValueAt(row, index);
        } else {
            projectname = _project_name;
            platename = getSeqDbInterface().retrievePlateName(trace_name);
        }
        SequenceFeature hq = null, nv = null;
        hq = getSeqDbInterface().retrieveSequenceFeature(trace_name, QualSequence.HSR_NAME);
        int hqf = hq.getFirst() - 1, hql = hq.getLast() - 1;
        nv = getSeqDbInterface().retrieveSequenceFeature(trace_name, CrossMatchOutput.NAME);
        int nvf = nv.getFirst() - 1, nvl = nv.getLast() - 1;
        int[] qual = null;
        String[] seq = null;
        qual = Utilities.string2IntArray(getSeqDbInterface().retrieveQual(trace_name));
        seq = Utilities.string2StringArray(getSeqDbInterface().retrieveSeq(trace_name));
        SeqFrame frame = new SeqFrame(qual, seq, hqf, hql, nvf, nvl, length, hq_length, hq_score_sum, perc_no_call, trace_name, trace_path, template_name, primer_name, projectname, platename, date);
        frame.validate();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setVisible(true);
    }

    private void showTraces(int row) throws Exception {
        int trac_index = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex(), path_index = project_seqs_jtable.getColumn(_model.PATH_COLUMN_NAME).getModelIndex();
        String trace_name = "" + project_seqs_jtable.getValueAt(row, trac_index), trace_path = "" + project_seqs_jtable.getValueAt(row, path_index);
        new TraceFrame(trace_name, trace_path);
    }

    private void showQualHisto(int row) throws Exception {
        int trac_index = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex(), path_index = project_seqs_jtable.getColumn(_model.PATH_COLUMN_NAME).getModelIndex();
        String trace_name = "" + project_seqs_jtable.getValueAt(row, trac_index), trace_path = "" + project_seqs_jtable.getValueAt(row, path_index);
        int[] qual = null;
        qual = Utilities.string2IntArray(getSeqDbInterface().retrieveQual(trace_name));
        Univariate uni = new Univariate(qual);
        new HistogramFrame(uni, trace_name, trace_path);
    }

    private Object[][] getData(final String[] traces) {
        if (traces == null || traces.length < 1) {
            return null;
        }
        String sql1 = null, sql2 = null;
        if (!_per_plate) {
            sql1 = "select " + SeqDbInterface.SQ_SEQ_REACTS_TMPL_NAME_COL + ", " + SeqDbInterface.SQ_SEQ_REACTS_PRM_NAME_COL + " from " + SeqDbInterface.SQ_SEQ_REACTS_VIEW + " where " + SeqDbInterface.SQ_SEQ_REACTS_TRACE_FILE_COL + " = ?";
        } else {
            sql1 = "select " + SeqDbInterface.SQ_SEQ_REACTS_SPRJ_NAME_COL + ", " + SeqDbInterface.SQ_SEQ_REACTS_TMPL_NAME_COL + ", " + SeqDbInterface.SQ_SEQ_REACTS_PRM_NAME_COL + " from " + SeqDbInterface.SQ_SEQ_REACTS_VIEW + " where " + SeqDbInterface.SQ_SEQ_REACTS_TRACE_FILE_COL + " = ?";
        }
        sql2 = "select " + SeqDbInterface.SQLD_BS_TRACE_PATH_COL + ", " + SeqDbInterface.SQLD_BS_SEQ_LENGTH_COL + ", " + SeqDbInterface.SQLD_BS_HIGH_QUAL_LENGTH_COL + ", " + SeqDbInterface.SQLD_BS_PERCENT_NO_CALL_COL + ", " + SeqDbInterface.SQ_BS_SEQ_CREAT_COL + " from " + SeqDbInterface.SQ_BS_VIEW + " where " + SeqDbInterface.SQLD_BS_TRACE_FILE_COL + " = ?";
        PreparedStatement ps1 = null, ps2 = null;
        ResultSet rs1 = null, rs2 = null;
        String trace_file = null;
        Object[][] data = new Object[traces.length][_model.getColumnNames().length - 2];
        try {
            ps1 = getSeqDbInterface().getPreparedStatement(sql1);
            ps2 = getSeqDbInterface().getPreparedStatement(sql2);
            int temp_index = project_seqs_jtable.getColumn(_model.TEMPLATE_COLUMN_NAME).getModelIndex() - 1, prim_index = project_seqs_jtable.getColumn(_model.PRIMER_COLUMN_NAME).getModelIndex() - 1, trac_index = project_seqs_jtable.getColumn(_model.TRACE_COLUMN_NAME).getModelIndex() - 1, path_index = project_seqs_jtable.getColumn(_model.PATH_COLUMN_NAME).getModelIndex() - 1, len_index = project_seqs_jtable.getColumn(_model.LENGTH_COLUMN_NAME).getModelIndex() - 1, hql_index = project_seqs_jtable.getColumn(_model.HQL_COLUMN_NAME).getModelIndex() - 1, pnc_index = project_seqs_jtable.getColumn(_model.PNC_COLUMN_NAME).getModelIndex() - 1, score_index = project_seqs_jtable.getColumn(_model.SCORE_COLUMN_NAME).getModelIndex() - 1, av_qual_index = project_seqs_jtable.getColumn(_model.AVERAGE_QUAL_NAME).getModelIndex() - 1, date_index = project_seqs_jtable.getColumn(_model.DATE_COLUMN_NAME).getModelIndex() - 1, proj_index = -1;
            if (_per_plate) {
                proj_index = project_seqs_jtable.getColumn(_model.PROJECT_COLUMN_NAME).getModelIndex() - 1;
            }
            for (int i = 0; i < traces.length; ++i) {
                trace_file = traces[i];
                ps1.setString(1, trace_file);
                ps2.setString(1, trace_file);
                rs1 = ps1.executeQuery();
                rs2 = ps2.executeQuery();
                data[i][trac_index] = trace_file;
                if (!rs1.next()) {
                    Utilities.userError("Could not find database entries for " + trace_file + " (in " + SeqDbInterface.SQ_SEQ_REACTS_VIEW + ")", this, CONTACT);
                    continue;
                }
                if (!rs2.next()) {
                    Utilities.userError("Could not find database entries for " + trace_file + " (in " + SeqDbInterface.SQ_BS_VIEW + ")", this, CONTACT);
                    continue;
                }
                data[i][temp_index] = rs1.getString(SeqDbInterface.SQ_SEQ_REACTS_TMPL_NAME_COL);
                data[i][prim_index] = rs1.getString(SeqDbInterface.SQ_SEQ_REACTS_PRM_NAME_COL);
                data[i][path_index] = rs2.getString(SeqDbInterface.SQLD_BS_TRACE_PATH_COL);
                data[i][len_index] = rs2.getString(SeqDbInterface.SQLD_BS_SEQ_LENGTH_COL);
                data[i][hql_index] = rs2.getString(SeqDbInterface.SQLD_BS_HIGH_QUAL_LENGTH_COL);
                data[i][pnc_index] = new Double(rs2.getDouble(SeqDbInterface.SQLD_BS_PERCENT_NO_CALL_COL)).toString();
                data[i][date_index] = rs2.getDate(SeqDbInterface.SQ_BS_SEQ_CREAT_COL);
                if (_per_plate) {
                    data[i][proj_index] = rs1.getString(SeqDbInterface.SQ_SEQ_REACTS_SPRJ_NAME_COL);
                }
                int[] qual_values = Utilities.string2IntArray(getSeqDbInterface().retrieveQual(trace_file));
                SequenceFeature hq = getSeqDbInterface().retrieveSequenceFeature(trace_file, QualSequence.HSR_NAME);
                int hqf = hq.getFirst() - 1, hql = hq.getLast() - 1, l = qual_values.length, sum = QualSequence.sumOfQualValues(qual_values, hqf, hql);
                double av = (double) QualSequence.sumOfQualValues(qual_values, 0, l - 1) / l;
                data[i][score_index] = new Integer(sum);
                data[i][av_qual_index] = new Double(av);
            }
            if (ps1 != null) {
                ps1.close();
            }
            ps1 = null;
            if (ps2 != null) {
                ps2.close();
            }
            ps2 = null;
            if (rs1 != null) {
                rs1.close();
            }
            rs1 = null;
            if (rs2 != null) {
                rs2.close();
            }
            rs2 = null;
        } catch (Exception e) {
            Utilities.unexpectedException(e, this, CONTACT);
        }
        return data;
    }

    private SeqDbInterface getSeqDbInterface() {
        return _sdbi;
    }

    private void setSeqDbInterface(SeqDbInterface sdbi) {
        _sdbi = sdbi;
    }

    JPanel contentPane;

    JMenuBar jMenuBar1 = new JMenuBar();

    JMenu jMenuFile = new JMenu();

    JMenuItem jMenuFileExit = new JMenuItem();

    JMenuItem jMenuFileSaveTable = new JMenuItem();

    JMenuItem jMenuFilePrintTable = new JMenuItem();

    JMenu jMenuHelp = new JMenu();

    JMenuItem jMenuHelpAbout = new JMenuItem();

    JMenuItem jMenuHelpHelp = new JMenuItem();

    JMenuItem instructionMenuItem = new JMenuItem();

    BorderLayout borderLayout1 = new BorderLayout();

    JPanel jPanel1 = new JPanel();

    JButton saveSelectedSeqsButton = new JButton();

    JButton saveTracesButton = new JButton();

    JButton unselectAllbutton = new JButton();

    JButton selectAllButton = new JButton();

    JButton inverseSelectionButton = new JButton();

    JButton selectSelectedButton = new JButton();

    JButton saveSelectedHQSButton = new JButton();

    JButton saveSelectedHQInsButton = new JButton();

    JButton resubmitSelectedButton = new JButton();

    JButton assembleSelectedButton = new JButton();

    JButton troubleshootButton = new JButton();

    GridLayout gridLayout1 = new GridLayout();

    GridBagLayout gridbag = new GridBagLayout();

    JScrollPane jScrollPane1 = new JScrollPane();

    JTable project_seqs_jtable = null;

    JPanel savePanel1 = new JPanel();

    JPanel checkPanel = new JPanel();

    JCheckBox saveWithTemplatePrimerInNameCheckBox = new JCheckBox();

    JCheckBox saveAsFastaCheckBox = new JCheckBox();

    JCheckBox saveSingleFastaCheckBox = new JCheckBox();

    JPanel savePanel2 = new JPanel();

    JLabel saveLabel = new JLabel();

    JMenuItem menuItem1 = new JMenuItem("Show Sequence");

    JMenuItem menuItem2 = new JMenuItem("Show Traces");

    JMenuItem menuItem3 = new JMenuItem("Show Quality Value Histogram");

    JPopupMenu popupMenu = new JPopupMenu();

    public static final String CONTACT = "czmasek@gnf.org", TABLE_FILE_SUFFIX = "xls", SEQ_TXT_FILE_SUFFIX = "txt", TRACE_FILE_SUFFIX = "ab1";

    private SeqDbInterface _sdbi;

    private Connection _connection;

    private final ProjectSeqsTableModel _model;

    private final boolean _per_plate;

    private String _project_name, _platename;

    private final String _db_user_name, _db_user_pwd, _db_driver_name, _db_url;

    private final ResubmitJFrame _parent;

    private int _row;

    private Action instructionMenuItemAction = new AbstractAction("Instructions") {

        {
            putValue(Action.SHORT_DESCRIPTION, "Instructions");
            putValue(Action.LONG_DESCRIPTION, "Instructions");
            putValue(Action.MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_H));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control H"));
        }

        public void actionPerformed(ActionEvent e) {
            String mesg = "";
            mesg += "How to get your sequences out of SeqTRACS\n\n";
            mesg += "Step 1: Select the sequences you want to export.\n\n";
            mesg += "Click on the checkbox in the last column to select the sequence in that row.\n";
            mesg += "The four buttons: Check All, Check Highlighted, Inverse,\nand Uncheck All affect the selected checkboxes.\n";
            mesg += "Pressing Check All will mark all of the check boxes.\n";
            mesg += "Check Highlighted will check rows which are selected by highlighting.\n";
            mesg += "Inverse will check unchecked boxes and uncheck checked boxes.\n";
            mesg += "Uncheck All will uncheck all of the check boxes.\n\n";
            mesg += "Step 2: Choose save options.\n\n";
            mesg += "To save in FASTA format check the Save Fasta Box.\n";
            mesg += "Each sequence will be saved in a separate file unless One File is checked.\n";
            mesg += "Checking Rxn in Name box will name the sequence file with the reaction name.\n\n";
            mesg += "Step 3: Download sequences.\n\n";
            mesg += "Choose Complete to download raw sequence.\n";
            mesg += "Press High Quality to download only the high quality portion of the sequence.\n";
            mesg += "Hit HQ Insert to download only the high quality non-vector sequence.\n";
            mesg += "Select Tracefiles to download the ab1 source files.\n\n\n";
            mesg += "Note: you must choose a DIRECTORY to save into, where the files\n";
            mesg += "will be saved.  Files are named automatically.\n\n";
            JOptionPane.showMessageDialog(new JFrame(), mesg);
        }
    };
}
