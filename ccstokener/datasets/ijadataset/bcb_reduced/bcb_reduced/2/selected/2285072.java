package net.sf.borg.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.DefaultEditorKit;
import net.sf.borg.common.Errmsg;
import net.sf.borg.common.IOHelper;
import net.sf.borg.common.PrefName;
import net.sf.borg.common.Prefs;
import net.sf.borg.common.Resource;
import net.sf.borg.common.ScrolledDialog;
import net.sf.borg.common.XTree;
import net.sf.borg.control.Borg;
import net.sf.borg.control.SystemManager;
import net.sf.borg.ui.memo.MemoPanel;
import net.sf.borg.model.AddressModel;
import net.sf.borg.model.AppointmentModel;
import net.sf.borg.model.CategoryModel;
import net.sf.borg.model.LinkModel;
import net.sf.borg.model.MemoModel;
import net.sf.borg.model.TaskModel;
import net.sf.borg.model.beans.Address;
import net.sf.borg.model.beans.Appointment;
import net.sf.borg.model.beans.Project;
import net.sf.borg.model.beans.Task;
import net.sf.borg.model.db.jdbc.JdbcDB;
import net.sf.borg.model.undo.UndoLog;
import net.sf.borg.ui.address.AddrListView;
import net.sf.borg.ui.address.AddressView;
import net.sf.borg.ui.calendar.AppointmentListView;
import net.sf.borg.ui.calendar.SearchView;
import net.sf.borg.ui.calendar.TodoView;
import net.sf.borg.ui.task.TaskConfigurator;
import net.sf.borg.ui.task.TaskListPanel;

class MainMenu {

    private JMenuItem AboutMI = new javax.swing.JMenuItem();

    private JMenu fileMenu = new javax.swing.JMenu();

    private JMenu goToMenu = new javax.swing.JMenu();

    private JMenu editMenu = new javax.swing.JMenu();

    private JMenu newMenu = new javax.swing.JMenu();

    private JMenuItem addressMI = new javax.swing.JMenuItem();

    private JMenu catmenu = new javax.swing.JMenu();

    private JMenuItem chglog = new javax.swing.JMenuItem();

    private JMenuItem copy = new javax.swing.JMenuItem();

    private JMenuItem cut = new javax.swing.JMenuItem();

    private JMenuItem dbMI = new javax.swing.JMenuItem();

    /**
	 * This method initializes delcatMI
	 * 
	 * @return javax.swing.JMenuItem
	 */
    private JMenuItem delcatMI;

    private JMenuItem delete = new javax.swing.JMenuItem();

    private JMenuItem editPrefsMenuItem = new javax.swing.JMenuItem();

    private JMenuItem exitMenuItem = new javax.swing.JMenuItem();

    private JMenuItem exportMI = new javax.swing.JMenuItem();

    private JMenuItem expurl = new javax.swing.JMenuItem();

    private JMenu expXML = new javax.swing.JMenu();

    private JMenu helpmenu = new javax.swing.JMenu();

    private JMenuItem helpMI = new javax.swing.JMenuItem();

    private JMenu impexpMenu = new javax.swing.JMenu();

    private JMenuItem importMI = new javax.swing.JMenuItem();

    private JMenuItem impurl = new javax.swing.JMenuItem();

    private JMenu impXML = new javax.swing.JMenu();

    private JMenuItem jMenuItem2 = new javax.swing.JMenuItem();

    private JMenuItem jMenuItem3 = new javax.swing.JMenuItem();

    private JMenuItem jMenuItem4 = new javax.swing.JMenuItem();

    private JMenuItem licsend = new javax.swing.JMenuItem();

    private JMenuBar menuBar = new JMenuBar();

    private JMenu OptionMenu = new javax.swing.JMenu();

    private JMenuItem NewContact = new javax.swing.JMenuItem();

    private JMenuItem NewAppointment = new javax.swing.JMenuItem();

    private JMenuItem NewTask = new javax.swing.JMenuItem();

    private JMenuItem NewMemo = new javax.swing.JMenuItem();

    private JMenuItem NewUserProfile = new javax.swing.JMenuItem();

    private JMenuItem paste = new javax.swing.JMenuItem();

    private JMenuItem PrintMI = new javax.swing.JMenuItem();

    private JMenuItem rlsnotes = new javax.swing.JMenuItem();

    private JMenuItem SearchMI = new javax.swing.JMenuItem();

    private JMenuItem selectAll = new javax.swing.JMenuItem();

    JMenuItem sqlMI = new JMenuItem();

    private JMenuItem switchUser = new javax.swing.JMenuItem();

    private JMenuItem syncMI = new javax.swing.JMenuItem();

    private JMenuItem ToDoMenu = new javax.swing.JMenuItem();

    public MainMenu() {
        menuBar.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
        ResourceHelper.setText(fileMenu, "File");
        ResourceHelper.setText(newMenu, "New");
        ResourceHelper.setText(NewContact, "Contact");
        NewContact.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().addView(AddrListView.getReference());
                Address addr = AddressModel.getReference().newAddress();
                addr.setKey(-1);
                MultiView.getMainView().addView(new AddressView(addr));
            }
        });
        newMenu.add(NewContact);
        ResourceHelper.setText(NewAppointment, "appointment");
        NewAppointment.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GregorianCalendar cal = new GregorianCalendar();
                AppointmentListView ag = new AppointmentListView(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                MultiView.getMainView().addView(ag);
            }
        });
        newMenu.add(NewAppointment);
        ResourceHelper.setText(NewTask, "task");
        NewTask.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().showTasks();
                TaskListPanel t = new TaskListPanel();
                t.task_add();
            }
        });
        newMenu.add(NewTask);
        ResourceHelper.setText(NewMemo, "memo");
        NewMemo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MemoPanel x = new MemoPanel();
                x.newMemo();
                MultiView.getMainView().showMemos(null);
            }
        });
        newMenu.add(NewMemo);
        fileMenu.add(newMenu);
        fileMenu.addSeparator();
        PrintMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Print16.gif")));
        ResourceHelper.setText(PrintMI, "Print");
        PrintMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrintMIActionPerformed(evt);
            }
        });
        fileMenu.add(PrintMI);
        syncMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Refresh16.gif")));
        ResourceHelper.setText(syncMI, "Synchronize");
        syncMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                syncMIActionPerformed(evt);
            }
        });
        fileMenu.add(syncMI);
        sqlMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Refresh16.gif")));
        ResourceHelper.setText(sqlMI, "RunSQL");
        sqlMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new SqlRunner().setVisible(true);
            }
        });
        fileMenu.add(sqlMI);
        JMenuItem closeTabMI = new JMenuItem();
        closeTabMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Delete16.gif")));
        ResourceHelper.setText(closeTabMI, "close_tabs");
        closeTabMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().closeTabs();
            }
        });
        fileMenu.add(closeTabMI);
        fileMenu.addSeparator();
        ResourceHelper.setText(NewUserProfile, "User_Profile");
        NewUserProfile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemManager.doNewUser();
            }
        });
        fileMenu.add(NewUserProfile);
        ResourceHelper.setText(switchUser, "Switch_Users");
        switchUser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemManager.doSwitchUser();
            }
        });
        fileMenu.add(switchUser);
        fileMenu.addSeparator();
        exitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Stop16.gif")));
        ResourceHelper.setText(exitMenuItem, "Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        ResourceHelper.setText(editMenu, "Edit");
        copy = new JMenuItem(new DefaultEditorKit.CopyAction());
        ResourceHelper.setText(copy, "Copy");
        copy.setMnemonic(KeyEvent.VK_C);
        editMenu.add(copy);
        cut = new JMenuItem(new DefaultEditorKit.CutAction());
        cut.setMnemonic(KeyEvent.VK_T);
        ResourceHelper.setText(cut, "Cut");
        editMenu.add(cut);
        paste = new JMenuItem(new DefaultEditorKit.PasteAction());
        ResourceHelper.setText(paste, "Paste");
        paste.setMnemonic(KeyEvent.VK_P);
        editMenu.add(paste);
        selectAll = new JMenuItem(DefaultEditorKit.selectAllAction);
        ResourceHelper.setText(selectAll, "SelectAll");
        selectAll.setMnemonic(KeyEvent.VK_S);
        selectAll.setActionCommand("cmd.select.all");
        editMenu.add(selectAll);
        ResourceHelper.setText(delete, "Delete");
        delete.setMnemonic(KeyEvent.VK_DELETE);
        delete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
            }
        });
        editMenu.add(delete);
        menuBar.add(editMenu);
        goToMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Application16.gif")));
        ResourceHelper.setText(goToMenu, "Go_To");
        ToDoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Properties16.gif")));
        ResourceHelper.setText(ToDoMenu, "To_Do");
        ToDoMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToDoMenuActionPerformed(evt);
            }
        });
        goToMenu.add(ToDoMenu);
        addressMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/addr16.jpg")));
        ResourceHelper.setText(addressMI, "Address_Book");
        addressMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressMIActionPerformed(evt);
            }
        });
        goToMenu.add(addressMI);
        JMenuItem MemoMI = new JMenuItem();
        MemoMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Edit16.gif")));
        ResourceHelper.setText(MemoMI, "Memos");
        MemoMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().showMemos(null);
            }
        });
        goToMenu.add(MemoMI);
        JMenuItem TaskMI = new JMenuItem();
        TaskMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Preferences16.gif")));
        ResourceHelper.setText(TaskMI, "tasks");
        TaskMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().showTasks();
            }
        });
        goToMenu.add(TaskMI);
        SearchMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Find16.gif")));
        ResourceHelper.setText(SearchMI, "srch");
        SearchMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchMIActionPerformed(evt);
            }
        });
        goToMenu.add(SearchMI);
        menuBar.add(goToMenu);
        OptionMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Preferences16.gif")));
        ResourceHelper.setText(OptionMenu, "Options");
        ResourceHelper.setText(editPrefsMenuItem, "ep");
        editPrefsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        OptionMenu.add(editPrefsMenuItem);
        JMenuItem exportPrefsMI = new JMenuItem();
        exportPrefsMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Export16.gif")));
        ResourceHelper.setText(exportPrefsMI, "export_prefs");
        exportPrefsMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expPrefs(evt);
            }
        });
        OptionMenu.add(exportPrefsMI);
        JMenuItem mportPrefsMI = new JMenuItem();
        mportPrefsMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Import16.gif")));
        ResourceHelper.setText(mportPrefsMI, "import_prefs");
        mportPrefsMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                impPrefs(evt);
            }
        });
        OptionMenu.add(mportPrefsMI);
        JMenu tsm = new JMenu(Resource.getPlainResourceString("task_state_options"));
        JMenuItem edittypes = new JMenuItem();
        JMenuItem resetst = new JMenuItem();
        ResourceHelper.setText(edittypes, "edit_types");
        edittypes.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edittypesActionPerformed(evt);
            }
        });
        tsm.add(edittypes);
        ResourceHelper.setText(resetst, "Reset_Task_States_to_Default");
        resetst.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetstActionPerformed(evt);
            }
        });
        tsm.add(resetst);
        OptionMenu.add(tsm);
        menuBar.add(OptionMenu);
        catmenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Preferences16.gif")));
        ResourceHelper.setText(catmenu, "Categories");
        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Preferences16.gif")));
        ResourceHelper.setText(jMenuItem2, "choosecat");
        jMenuItem2.setActionCommand("Choose Displayed Categories");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        catmenu.add(jMenuItem2);
        jMenuItem3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Add16.gif")));
        ResourceHelper.setText(jMenuItem3, "addcat");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        catmenu.add(jMenuItem3);
        jMenuItem4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Delete16.gif")));
        ResourceHelper.setText(jMenuItem4, "remcat");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        catmenu.add(jMenuItem4);
        menuBar.add(catmenu);
        impexpMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Export16.gif")));
        ResourceHelper.setText(impexpMenu, "impexpMenu");
        impXML.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Import16.gif")));
        ResourceHelper.setText(impXML, "impXML");
        ResourceHelper.setText(importMI, "impmenu");
        importMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMIActionPerformed(evt);
            }
        });
        impXML.add(importMI);
        ResourceHelper.setText(impurl, "impurl");
        impurl.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                impurlActionPerformed(evt);
            }
        });
        impXML.add(impurl);
        impexpMenu.add(impXML);
        expXML.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Export16.gif")));
        ResourceHelper.setText(expXML, "expXML");
        ResourceHelper.setText(exportMI, "expmenu");
        exportMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMIActionPerformed(evt);
            }
        });
        expXML.add(exportMI);
        ResourceHelper.setText(expurl, "expurl");
        expurl.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expurlActionPerformed(evt);
            }
        });
        expXML.add(expurl);
        impexpMenu.add(expXML);
        menuBar.add(impexpMenu);
        String dbtype = Prefs.getPref(PrefName.DBTYPE);
        menuBar.add(getUndoMenu());
        menuBar.add(getReportMenu());
        menuBar.add(Box.createHorizontalGlue());
        if (dbtype.equals("local")) {
            JMenu warning = new JMenu();
            warning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/redball.gif")));
            warning.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent arg0) {
                }

                public void menuDeselected(MenuEvent arg0) {
                }

                public void menuSelected(MenuEvent arg0) {
                    Errmsg.notice(Resource.getPlainResourceString("mdb_deprecated"));
                }
            });
            menuBar.add(warning);
        }
        helpmenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Help16.gif")));
        ResourceHelper.setText(helpmenu, "Help");
        ResourceHelper.setText(helpMI, "Help");
        helpMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMIActionPerformed(evt);
            }
        });
        helpmenu.add(helpMI);
        ResourceHelper.setText(licsend, "License");
        licsend.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                licsendActionPerformed(evt);
            }
        });
        helpmenu.add(licsend);
        ResourceHelper.setText(chglog, "viewchglog");
        chglog.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chglogActionPerformed(evt);
            }
        });
        helpmenu.add(chglog);
        ResourceHelper.setText(rlsnotes, "rlsnotes");
        rlsnotes.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MultiView.getMainView().addView(new HelpScreen("/resource/RELEASE_NOTES.txt", Resource.getPlainResourceString("rlsnotes")));
            }
        });
        helpmenu.add(rlsnotes);
        ResourceHelper.setText(dbMI, "DatabaseInformation");
        dbMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dbMIActionPerformed(evt);
            }
        });
        helpmenu.add(dbMI);
        ResourceHelper.setText(AboutMI, "About");
        AboutMI.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutMIActionPerformed(evt);
            }
        });
        helpmenu.add(AboutMI);
        menuBar.add(helpmenu);
        catmenu.add(getDelcatMI());
        if (dbtype.equals("mysql")) {
            syncMI.setEnabled(true);
        } else {
            syncMI.setEnabled(false);
        }
        if (dbtype.equals("hsqldb") || dbtype.equals("mysql")) sqlMI.setEnabled(true); else sqlMI.setEnabled(false);
        importMI.setEnabled(true);
        exportMI.setEnabled(true);
    }

    private void AboutMIActionPerformed(java.awt.event.ActionEvent evt) {
        String build_info = "";
        String version = "";
        try {
            InputStream is = getClass().getResource("/properties").openStream();
            Properties props = new Properties();
            props.load(is);
            is.close();
            version = Resource.getVersion();
            build_info = Resource.getResourceString("Build_Number:_") + props.getProperty("build.number") + Resource.getResourceString("Build_Time:_") + props.getProperty("build.time");
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
        String info = Resource.getResourceString("Berger-Organizer_v") + version + "\n" + Resource.getResourceString("copyright") + " (2003-2009) Michael Berger <i_flem@users.sourceforge.net>\nhttp://borg-calendar.sourceforge.net\n\n" + Resource.getResourceString("contributions_by") + "\n" + Resource.getResourceString("contrib") + "\n" + Resource.getResourceString("translations") + "\n\n" + build_info + "\n" + "Java " + System.getProperty("java.version");
        Object opts[] = { Resource.getPlainResourceString("Dismiss") };
        JOptionPane.showOptionDialog(null, info, Resource.getResourceString("About_BORG"), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("/resource/borg.jpg")), opts, opts[0]);
    }

    private void addressMIActionPerformed(java.awt.event.ActionEvent evt) {
        MultiView.getMainView().addView(AddrListView.getReference());
    }

    private void chglogActionPerformed(java.awt.event.ActionEvent evt) {
        MultiView.getMainView().addView(new HelpScreen("/resource/CHANGES.txt", Resource.getPlainResourceString("viewchglog")));
    }

    private OutputStream createOutputStreamFromURL(String urlstr) throws Exception {
        return IOHelper.createOutputStream(new URL(urlstr));
    }

    private void dbMIActionPerformed(java.awt.event.ActionEvent evt) {
        String dbtype = Prefs.getPref(PrefName.DBTYPE);
        String info = Resource.getPlainResourceString("DatabaseInformation") + ":\n\n";
        info += dbtype + " URL: " + JdbcDB.buildDbDir() + "\n\n";
        try {
            info += Resource.getPlainResourceString("appointments") + ": " + AppointmentModel.getReference().getAllAppts().size() + "\n";
            info += Resource.getPlainResourceString("addresses") + ": " + AddressModel.getReference().getAddresses().size() + "\n";
            info += Resource.getPlainResourceString("tasks") + ": " + TaskModel.getReference().getTasks().size() + "\n";
            info += Resource.getPlainResourceString("SubTasks") + ": " + TaskModel.getReference().getSubTasks().size() + "\n";
            info += Resource.getPlainResourceString("Logs") + ": " + TaskModel.getReference().getLogs().size() + "\n";
            info += Resource.getPlainResourceString("projects") + ": " + TaskModel.getReference().getProjects().size() + "\n";
            if (MemoModel.getReference().hasMemos()) info += Resource.getPlainResourceString("Memos") + ": " + MemoModel.getReference().getMemos().size() + "\n";
            info += Resource.getPlainResourceString("links") + ": " + LinkModel.getReference().getLinks().size() + "\n";
        } catch (Exception e) {
            Errmsg.errmsg(e);
            return;
        }
        ScrolledDialog.showNotice(info);
    }

    private void edittypesActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            TaskConfigurator.getReference().setVisible(true);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        Borg.shutdown();
    }

    private void exportMIActionPerformed(java.awt.event.ActionEvent evt) {
        File dir;
        while (true) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle(Resource.getResourceString("Please_choose_directory_to_place_XML_files"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setApproveButtonText(Resource.getResourceString("select_export_dir"));
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) return;
            String s = chooser.getSelectedFile().getAbsolutePath();
            dir = new File(s);
            String err = null;
            if (!dir.exists()) {
                err = Resource.getResourceString("Directory_[") + s + Resource.getResourceString("]_does_not_exist");
            } else if (!dir.isDirectory()) {
                err = "[" + s + Resource.getResourceString("]_is_not_a_directory");
            }
            if (err == null) break;
            Errmsg.notice(err);
        }
        try {
            JOptionPane.showMessageDialog(null, Resource.getResourceString("export_notice") + dir.getAbsolutePath());
            String fname = dir.getAbsolutePath() + "/borg.xml";
            if (IOHelper.checkOverwrite(fname)) {
                OutputStream ostr = IOHelper.createOutputStream(fname);
                Writer fw = new OutputStreamWriter(ostr, "UTF8");
                AppointmentModel.getReference().export(fw);
                fw.close();
            }
            fname = dir.getAbsolutePath() + "/mrdb.xml";
            if (IOHelper.checkOverwrite(fname)) {
                OutputStream ostr = IOHelper.createOutputStream(fname);
                Writer fw = new OutputStreamWriter(ostr, "UTF8");
                TaskModel.getReference().export(fw);
                fw.close();
            }
            fname = dir.getAbsolutePath() + "/addr.xml";
            if (IOHelper.checkOverwrite(fname)) {
                OutputStream ostr = IOHelper.createOutputStream(fname);
                Writer fw = new OutputStreamWriter(ostr, "UTF8");
                AddressModel.getReference().export(fw);
                fw.close();
            }
            if (MemoModel.getReference().hasMemos()) {
                fname = dir.getAbsolutePath() + "/memo.xml";
                if (IOHelper.checkOverwrite(fname)) {
                    OutputStream ostr = IOHelper.createOutputStream(fname);
                    Writer fw = new OutputStreamWriter(ostr, "UTF8");
                    MemoModel.getReference().export(fw);
                    fw.close();
                }
            }
            if (LinkModel.getReference().hasLinks()) {
                fname = dir.getAbsolutePath() + "/link.xml";
                if (IOHelper.checkOverwrite(fname)) {
                    OutputStream ostr = IOHelper.createOutputStream(fname);
                    Writer fw = new OutputStreamWriter(ostr, "UTF8");
                    LinkModel.getReference().export(fw);
                    fw.close();
                }
            }
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void expPrefs(java.awt.event.ActionEvent evt) {
        File file;
        while (true) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle(Resource.getResourceString("choose_file"));
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) return;
            String s = chooser.getSelectedFile().getAbsolutePath();
            file = new File(s);
            break;
        }
        try {
            Prefs.export(file.getAbsolutePath());
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void expurlActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String prevurl = Prefs.getPref(PrefName.LASTEXPURL);
            String urlst = JOptionPane.showInputDialog(Resource.getResourceString("enturl"), prevurl);
            if (urlst == null || urlst.equals("")) return;
            Prefs.putPref(PrefName.LASTEXPURL, urlst);
            expURLCommon(urlst);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void expURLCommon(String url) throws Exception {
        OutputStream fos = createOutputStreamFromURL(url + "/borg.xml");
        Writer fw = new OutputStreamWriter(fos, "UTF8");
        AppointmentModel.getReference().export(fw);
        fw.close();
        fos = createOutputStreamFromURL(url + "/mrdb.xml");
        fw = new OutputStreamWriter(fos, "UTF8");
        TaskModel.getReference().export(fw);
        fw.close();
        fos = createOutputStreamFromURL(url + "/addr.xml");
        fw = new OutputStreamWriter(fos, "UTF8");
        AddressModel.getReference().export(fw);
        fw.close();
        fos = createOutputStreamFromURL(url + "/memo.xml");
        fw = new OutputStreamWriter(fos, "UTF8");
        MemoModel.getReference().export(fw);
        fw.close();
    }

    private JMenuItem getDelcatMI() {
        if (delcatMI == null) {
            delcatMI = new JMenuItem();
            ResourceHelper.setText(delcatMI, "delete_cat");
            delcatMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Delete16.gif")));
            delcatMI.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        CategoryModel catmod = CategoryModel.getReference();
                        Collection<String> allcats = catmod.getCategories();
                        allcats.remove(CategoryModel.UNCATEGORIZED);
                        if (allcats.isEmpty()) return;
                        Object[] cats = allcats.toArray();
                        Object o = JOptionPane.showInputDialog(null, Resource.getResourceString("delete_cat_choose"), "", JOptionPane.QUESTION_MESSAGE, null, cats, cats[0]);
                        if (o == null) return;
                        int ret = JOptionPane.showConfirmDialog(null, Resource.getResourceString("delcat_warn") + " [" + (String) o + "]!", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (ret == JOptionPane.OK_OPTION) {
                            Iterator<?> itr = AppointmentModel.getReference().getAllAppts().iterator();
                            while (itr.hasNext()) {
                                Appointment ap = (Appointment) itr.next();
                                String cat = ap.getCategory();
                                if (cat != null && cat.equals(o)) AppointmentModel.getReference().delAppt(ap);
                            }
                            itr = TaskModel.getReference().getTasks().iterator();
                            while (itr.hasNext()) {
                                Task t = (Task) itr.next();
                                String cat = t.getCategory();
                                if (cat != null && cat.equals(o)) TaskModel.getReference().delete(t.getKey());
                            }
                            try {
                                CategoryModel.getReference().syncCategories();
                            } catch (Exception ex) {
                                Errmsg.errmsg(ex);
                            }
                        }
                    } catch (Exception ex) {
                        Errmsg.errmsg(ex);
                    }
                }
            });
        }
        return delcatMI;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    private JMenu getReportMenu() {
        JMenu m = new JMenu();
        m.setText(Resource.getPlainResourceString("reports"));
        JMenuItem prr = new JMenuItem();
        prr.setText(Resource.getPlainResourceString("project_report"));
        prr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    Project p = BeanSelector.selectProject();
                    if (p == null) return;
                    Map<String, Integer> map = new HashMap<String, Integer>();
                    map.put("pid", p.getId());
                    Collection<?> allChildren = TaskModel.getReference().getAllSubProjects(p.getId().intValue());
                    Iterator<?> it = allChildren.iterator();
                    for (int i = 2; i <= 10; i++) {
                        if (!it.hasNext()) break;
                        Project sp = (Project) it.next();
                        map.put("pid" + i, sp.getId());
                    }
                    RunReport.runReport("proj", map);
                } catch (NoClassDefFoundError r) {
                    Errmsg.notice(Resource.getPlainResourceString("borg_jasp"));
                } catch (Exception e) {
                    Errmsg.errmsg(e);
                }
            }
        });
        m.add(prr);
        JMenuItem otr = new JMenuItem();
        otr.setText(Resource.getPlainResourceString("open_tasks"));
        otr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                RunReport.runReport("open_tasks", null);
            }
        });
        m.add(otr);
        JMenuItem otpr = new JMenuItem();
        otpr.setText(Resource.getPlainResourceString("open_tasks_proj"));
        otpr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                RunReport.runReport("opentasksproj", null);
            }
        });
        m.add(otpr);
        JMenuItem customrpt = new JMenuItem();
        customrpt.setText(Resource.getPlainResourceString("select_rpt"));
        customrpt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    InputStream is = IOHelper.fileOpen(".", Resource.getPlainResourceString("select_rpt"));
                    if (is == null) return;
                    RunReport.runReport(is, null);
                } catch (Exception e) {
                    Errmsg.errmsg(e);
                }
            }
        });
        m.add(customrpt);
        return m;
    }

    private JMenu getUndoMenu() {
        JMenu m = new JMenu();
        m.setText(Resource.getPlainResourceString("undo"));
        m.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Refresh16.gif")));
        final JMenu menu = m;
        m.addMenuListener(new MenuListener() {

            public void menuCanceled(MenuEvent arg0) {
            }

            public void menuDeselected(MenuEvent arg0) {
            }

            @SuppressWarnings("unchecked")
            public void menuSelected(MenuEvent e) {
                menu.removeAll();
                final String top = UndoLog.getReference().getTopItem();
                if (top != null) {
                    JMenuItem mi = new JMenuItem(Resource.getPlainResourceString("undo") + ": " + top);
                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent arg0) {
                            int ret = JOptionPane.showConfirmDialog(null, Resource.getPlainResourceString("undo") + ": " + top + "\n\n" + Resource.getResourceString("please_confirm"), "", JOptionPane.OK_CANCEL_OPTION);
                            if (ret != JOptionPane.OK_OPTION) return;
                            UndoLog.getReference().executeUndo();
                        }
                    });
                    menu.add(mi);
                    JMenuItem cmi = new JMenuItem();
                    cmi.setText(Resource.getPlainResourceString("clear_undos"));
                    cmi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent arg0) {
                            int ret = JOptionPane.showConfirmDialog(null, Resource.getPlainResourceString("clear_undos") + "\n\n" + Resource.getResourceString("please_confirm"), "", JOptionPane.OK_CANCEL_OPTION);
                            if (ret != JOptionPane.OK_OPTION) return;
                            UndoLog.getReference().clear();
                        }
                    });
                    menu.add(cmi);
                    boolean show_stack = Prefs.getBoolPref(PrefName.SHOW_UNDO_STACK);
                    if (show_stack == true) {
                        JMenu all_mi = new JMenu(Resource.getPlainResourceString("all_undos"));
                        for (String item : UndoLog.getReference().getItemStrings()) {
                            JMenuItem item_mi = new JMenuItem(Resource.getPlainResourceString("undo") + ": " + item);
                            all_mi.add(item_mi);
                        }
                        menu.add(all_mi);
                    }
                } else {
                    menu.add(new JMenuItem(Resource.getPlainResourceString("no_undos")));
                }
            }
        });
        return m;
    }

    private void helpMIActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            HelpProxy.launchHelp();
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void impCommon(XTree xt) throws Exception {
        String type = xt.name();
        int ret = JOptionPane.showConfirmDialog(null, Resource.getResourceString("Importing_") + " " + type + ", OK?", Resource.getResourceString("Import_WARNING"), JOptionPane.OK_CANCEL_OPTION);
        if (ret != JOptionPane.OK_OPTION) return;
        if (type.equals("TASKS")) {
            TaskModel taskmod = TaskModel.getReference();
            taskmod.importXml(xt);
        } else if (type.equals("APPTS")) {
            AppointmentModel calmod = AppointmentModel.getReference();
            calmod.importXml(xt);
        } else if (type.equals("MEMOS")) {
            MemoModel memomod = MemoModel.getReference();
            memomod.importXml(xt);
        } else if (type.equals("ADDRESSES")) {
            AddressModel addrmod = AddressModel.getReference();
            addrmod.importXml(xt);
        } else if (type.equals("LINKS")) {
            LinkModel addrmod = LinkModel.getReference();
            addrmod.importXml(xt);
        }
        CategoryModel.getReference().syncCategories();
        CategoryModel.getReference().showAll();
    }

    private void importMIActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            InputStream istr = IOHelper.fileOpen(".", Resource.getResourceString("Please_choose_File_to_Import_From"));
            if (istr == null) return;
            XTree xt = XTree.readFromStream(istr);
            istr.close();
            if (xt == null) throw new Exception(Resource.getResourceString("Could_not_parse_") + "XML");
            impCommon(xt);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void impPrefs(java.awt.event.ActionEvent evt) {
        File file;
        while (true) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle(Resource.getResourceString("choose_file"));
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) return;
            String s = chooser.getSelectedFile().getAbsolutePath();
            file = new File(s);
            break;
        }
        try {
            Prefs.importPrefs(file.getAbsolutePath());
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void impurlActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String prevurl = Prefs.getPref(PrefName.LASTIMPURL);
            String urlst = JOptionPane.showInputDialog(Resource.getResourceString("enturl"), prevurl);
            if (urlst == null || urlst.equals("")) return;
            Prefs.putPref(PrefName.LASTIMPURL, urlst);
            URL url = new URL(urlst);
            impURLCommon(urlst, url.openStream());
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void impURLCommon(String url, InputStream istr) throws Exception {
        XTree xt = XTree.readFromStream(istr);
        if (xt == null) throw new Exception(Resource.getResourceString("Could_not_parse_") + url);
        impCommon(xt);
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
        OptionsView.getReference().setVisible(true);
    }

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
        CategoryChooser.getReference().setVisible(true);
    }

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {
        String inputValue = JOptionPane.showInputDialog(Resource.getResourceString("AddCat"));
        if (inputValue == null || inputValue.equals("")) return;
        try {
            CategoryModel.getReference().addCategory(inputValue);
            CategoryModel.getReference().showCategory(inputValue);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            CategoryModel.getReference().syncCategories();
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void licsendActionPerformed(java.awt.event.ActionEvent evt) {
        MultiView.getMainView().addView(new HelpScreen("/resource/license.htm", Resource.getPlainResourceString("License")));
    }

    private void PrintMIActionPerformed(java.awt.event.ActionEvent evt) {
        MultiView.getMainView().print();
    }

    private void resetstActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String msg = Resource.getResourceString("reset_state_warning");
            int ret = JOptionPane.showConfirmDialog(null, msg, Resource.getResourceString("Import_WARNING"), JOptionPane.OK_CANCEL_OPTION);
            if (ret != JOptionPane.OK_OPTION) return;
            TaskModel taskmod_ = TaskModel.getReference();
            taskmod_.getTaskTypes().loadDefault();
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void SearchMIActionPerformed(java.awt.event.ActionEvent evt) {
        MultiView.getMainView().addView(new SearchView());
    }

    private void syncMIActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Borg.syncDBs();
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    private void ToDoMenuActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            TodoView tg = TodoView.getReference();
            MultiView.getMainView().addView(tg);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }
}
