package pl.edu.mimuw.xqtav.proc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.embl.ebi.escience.scufl.Processor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import pl.edu.mimuw.xqtav.Version;
import pl.edu.mimuw.xqtav.XQMultiDocument;
import pl.edu.mimuw.xqtav.xqgen.XQGeneratorException;
import pl.edu.mimuw.xqtav.xqgen.xqgenerator_1.rec.XQRecursiveGenerator;
import pl.edu.mimuw.xqtav.xqgen.xqgenerator_1.rec.XQueryMultiWriter;

/**
 * @author gk
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XQueryAdvancedEditor extends JPanel implements ActionListener, WindowListener {

    public static final long serialVersionUID = 1;

    public Processor myProcessor = null;

    /**
	 * 
	 */
    public XQueryAdvancedEditor() {
        super();
        initializeComponents();
    }

    /**
	 * @param arg0
	 */
    public XQueryAdvancedEditor(boolean arg0) {
        super(arg0);
        initializeComponents();
    }

    /**
	 * @param arg0
	 */
    public XQueryAdvancedEditor(LayoutManager arg0) {
        super(arg0);
        initializeComponents();
    }

    /**
	 * @param arg0
	 * @param arg1
	 */
    public XQueryAdvancedEditor(LayoutManager arg0, boolean arg1) {
        super(arg0, arg1);
        initializeComponents();
    }

    protected JMenuBar mainMenu = null;

    protected JLabel statusBar = null;

    protected JTabbedPane editorTabs = null;

    public XQMultiDocument xmd = new XQMultiDocument();

    public void setupMenuItem(JMenu menu, String text, String command) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(command);
        item.addActionListener(this);
        menu.add(item);
    }

    public void setupMenu() {
        mainMenu = new JMenuBar();
        JMenu menu = new JMenu("XQuery");
        mainMenu.add(menu);
        setupMenuItem(menu, "New XQuery", "menu_xquery_new");
        menu.addSeparator();
        setupMenuItem(menu, "Load from file", "menu_xquery_loadfile");
        setupMenuItem(menu, "Load from Web", "menu_xquery_loaduri");
        menu.addSeparator();
        setupMenuItem(menu, "Save to file", "menu_xquery_savefile");
        setupMenuItem(menu, "Save as XQuery", "menu_xquery_savemain");
        menu.addSeparator();
        setupMenuItem(menu, "Update & Close", "menu_xquery_uclose");
        setupMenuItem(menu, "Discard & Close", "menu_xquery_close");
        menu = new JMenu("Tabs");
        mainMenu.add(menu);
        setupMenuItem(menu, "New tab", "menu_tab_new");
        setupMenuItem(menu, "Set current tab name", "menu_tab_rename");
        setupMenuItem(menu, "Delete current tab", "menu_tab_delete");
        menu = new JMenu("Generator");
        mainMenu.add(menu);
        setupMenuItem(menu, "Get XQuery from SCUFL", "menu_gen_scuflxq");
        menu = new JMenu("Help");
        mainMenu.add(menu);
        setupMenuItem(menu, "About XQTav", "menu_help_about");
    }

    public void initializeComponents() {
        setupMenu();
        this.setLayout(new BorderLayout());
        editorTabs = new JTabbedPane();
        this.add(editorTabs, BorderLayout.CENTER);
        statusBar = new JLabel();
        this.add(statusBar, BorderLayout.SOUTH);
        setStatus("XQTav Advanced Editor");
        this.setVisible(true);
    }

    public void setStatus(String text) {
        statusBar.setText(text);
    }

    public JFrame myFrame = null;

    public JMenuBar getMenu() {
        return mainMenu;
    }

    protected String currentFile = "NewFile.xqq";

    public void setTitle() {
        if (myFrame != null && currentFile != null) {
            myFrame.setTitle("XQTav - " + currentFile);
        }
    }

    public void startEditor() {
        myFrame.setTitle("XQTav Advanced Editor");
        if (xmd == null) {
            xmd = new XQMultiDocument();
        }
        if (xmd.getDocumentCount() == 0) {
            command_createnew();
        }
        command_doc2editor();
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e);
        }
        XQueryAdvancedEditor xae = new XQueryAdvancedEditor();
        jf.getContentPane().add(xae);
        xae.myFrame = jf;
        jf.setJMenuBar(xae.getMenu());
        jf.setSize(800, 600);
        jf.setVisible(true);
        xae.startEditor();
    }

    /*************************************************************/
    protected boolean isOpen = false;

    public void showEditor(ActionEvent ae) {
        if (myProcessor == null) {
            error("XQuery Advanced Editor called but no processor present");
        }
        if (isOpen) {
            error("Editor already open");
            return;
        }
        this.myFrame = new JFrame();
        this.myFrame.addWindowListener(this);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e);
        }
        myFrame.getContentPane().add(this);
        myFrame.setJMenuBar(getMenu());
        myFrame.setSize(800, 600);
        isOpen = true;
        myFrame.setVisible(true);
        if (myProcessor != null) {
            xmd = ((TavProcessor) myProcessor).getDocument();
        }
        startEditor();
    }

    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd == null) {
            System.err.println("XQTAV WARNING: action command null");
            return;
        }
        if (cmd.equals("Edit XQuery...")) {
            showEditor(ae);
            return;
        }
        if (cmd.equals("menu_xquery_close")) {
            event_close();
            return;
        }
        if (cmd.equals("menu_xquery_uclose")) {
            event_uclose();
            return;
        }
        if (cmd.equals("menu_help_about")) {
            event_about();
            return;
        }
        if (cmd.equals("menu_xquery_savefile")) {
            event_save();
            return;
        }
        if (cmd.equals("menu_xquery_savemain")) {
            event_savemain();
            return;
        }
        if (cmd.equals("menu_xquery_loadfile")) {
            event_loadfile();
            return;
        }
        if (cmd.equals("menu_xquery_loaduri")) {
            event_loaduri();
            return;
        }
        if (cmd.equals("menu_xquery_new")) {
            event_new();
            return;
        }
        if (cmd.equals("menu_tab_new")) {
            event_newtab();
            return;
        }
        if (cmd.equals("menu_tab_rename")) {
            event_renametab();
            return;
        }
        if (cmd.equals("menu_tab_delete")) {
            event_deletetab();
            return;
        }
        if (cmd.equals("menu_gen_scuflxq")) {
            event_genXQuery();
            return;
        }
        error("Command not registered");
    }

    /*************************************************************/
    public void command_createnew() {
        XQRecursiveGenerator recgen = new XQRecursiveGenerator("", new XQueryMultiWriter());
        recgen.singleWriter = recgen.multiWriter.getWriter("/");
        recgen.dump_namespace_decls();
        recgen.dump_sxl_functions();
        recgen.multiWriter.closeAll();
        xmd = XQMultiDocument.fromXQueryMultiWriter(recgen.multiWriter);
    }

    public void command_doc2editor() {
        if (editorTabs.getTabCount() > 0) {
            int ret = JOptionPane.showConfirmDialog(this, "You are going to close the current document. Any unsaved changed will be lost. Are you sure?", "Confirm close", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret != JOptionPane.YES_OPTION) return;
        }
        editorTabs.removeAll();
        tabNames.clear();
        List names = xmd.getDocumentNames();
        int idx = 0;
        Component mainComp = null;
        for (Iterator it = names.iterator(); it.hasNext(); ) {
            XQueryAdvancedEditPane aep = new XQueryAdvancedEditPane();
            String name = (String) it.next();
            if (tabNames.contains(name)) {
                throw new XQGeneratorException("Duplicate tab name: " + name);
            }
            tabNames.put(name, "x");
            aep.setText(new String(xmd.getDocument(name)));
            if (name.equals("")) {
                name = "/";
                mainComp = aep;
            }
            editorTabs.addTab(name, aep);
            idx++;
        }
        if (mainComp != null) {
            editorTabs.setSelectedComponent(mainComp);
        }
        setTitle();
    }

    public Hashtable tabNames = new Hashtable();

    public void command_editor2doc() {
        int i = 0;
        XQMultiDocument xmd2 = new XQMultiDocument();
        XQueryAdvancedEditPane aep;
        String title;
        for (i = 0; i < editorTabs.getTabCount(); i++) {
            title = editorTabs.getTitleAt(i);
            aep = (XQueryAdvancedEditPane) editorTabs.getComponentAt(i);
            xmd2.setDocument(title, aep.getText().getBytes());
        }
        xmd = xmd2;
    }

    public String save_as_xquery(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            if (xmd == null) {
                throw new XQGeneratorException("Missing document");
            }
            if (xmd.getDocument("/") == null) {
                throw new XQGeneratorException("You should name one tab as root tab ('/')");
            }
            String data = xmd.mergeIncludesInMain();
            fos.write(data.getBytes());
            fos.close();
            return null;
        } catch (Exception e) {
            return "Failed to save as XQuery: " + e;
        }
    }

    public String save_file(File file) {
        try {
            Document doc = new Document();
            command_editor2doc();
            Collection c = xmd.getElementCollection();
            Element root = new Element("document");
            root.setNamespace(Namespace.getNamespace("xmd", XQMultiDocument.xmdNs));
            root.addContent(c);
            doc.setRootElement(root);
            XMLOutputter output = new XMLOutputter();
            Format f = Format.getPrettyFormat();
            output.setFormat(f);
            output.output(doc, new FileOutputStream(file));
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ps.print("Failed to save: " + e);
            e.printStackTrace(ps);
            ps.close();
            return new String(baos.toByteArray());
        }
        return null;
    }

    public String load_file(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            XQMultiDocument xmd2 = XQMultiDocument.fromXML(fis);
            fis.close();
            xmd = xmd2;
            command_doc2editor();
            return null;
        } catch (Exception e) {
            return "Failed to load: " + e;
        }
    }

    public String load_uri(String uri) {
        try {
            URL url = new URL(uri);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            XQMultiDocument xmd2 = XQMultiDocument.fromXML(is);
            is.close();
            xmd = xmd2;
            command_doc2editor();
            return null;
        } catch (Exception e) {
            return "Failed to load: " + e;
        }
    }

    public void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    /*************************************************************/
    public void event_close() {
        myFrame.setVisible(false);
        isOpen = false;
        unregisterEditor();
    }

    public void event_uclose() {
        command_editor2doc();
        if (myProcessor != null) {
            ((TavProcessor) myProcessor).setDocument(xmd);
        }
        event_close();
    }

    public void event_about() {
        String[] messages = new String[] { Version.PRODUCT_NAME + " version " + Version.VERSION + ", " + Version.PRODUCT_DATE, "Copyright (C) 2004-2006 Warsaw University", "This program is distributed under GNU General Public License", "http://xqtav.sourceforge.net" };
        JOptionPane.showMessageDialog(this, messages, "About XQTav", JOptionPane.INFORMATION_MESSAGE);
    }

    public File lastFile = File.listRoots()[0];

    public void event_savemain() {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        jfc.setAcceptAllFileFilterUsed(true);
        jfc.setDialogTitle("What file save to?");
        jfc.setCurrentDirectory(lastFile);
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".xq") || f.getName().endsWith(".XQ");
            }

            public String getDescription() {
                return "XQuery scripts (.xq)";
            }
        });
        int ret = jfc.showDialog(this, "Save");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            if (f.getName().indexOf(".") == -1) f = new File(f.getAbsolutePath() + ".xq");
            if (f.getParentFile() != null) lastFile = f.getParentFile();
            String msg = save_as_xquery(f);
            if (msg == null) {
                currentFile = f.getName();
            } else {
                error(msg);
            }
            return;
        } else {
            File f = jfc.getSelectedFile();
            if (f != null) {
                if (f.getParentFile() != null) {
                    lastFile = f.getParentFile();
                }
            }
            return;
        }
    }

    public void event_save() {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        jfc.setAcceptAllFileFilterUsed(true);
        jfc.setDialogTitle("What file save to?");
        jfc.setCurrentDirectory(lastFile);
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".xqq") || f.getName().endsWith(".XQQ");
            }

            public String getDescription() {
                return "XQTav Scripts (.xqq)";
            }
        });
        int ret = jfc.showDialog(this, "Save");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            if (f.getName().indexOf(".") == -1) f = new File(f.getAbsolutePath() + ".xqq");
            if (f.getParentFile() != null) lastFile = f.getParentFile();
            String msg = save_file(f);
            if (msg == null) {
                currentFile = f.getName();
            } else {
                error(msg);
            }
            return;
        } else {
            File f = jfc.getSelectedFile();
            if (f != null) {
                if (f.getParentFile() != null) {
                    lastFile = f.getParentFile();
                }
            }
            return;
        }
    }

    public void event_loadfile() {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        jfc.setAcceptAllFileFilterUsed(true);
        jfc.setDialogTitle("What file to load from?");
        jfc.setCurrentDirectory(lastFile);
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".xqq") || f.getName().endsWith(".XQQ");
            }

            public String getDescription() {
                return "XQTav Scripts (.xqq)";
            }
        });
        int ret = jfc.showDialog(this, "Load");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            if (f.getParentFile() != null) lastFile = f.getParentFile();
            String msg = load_file(f);
            if (msg == null) {
                currentFile = f.getName();
            } else {
                error(msg);
            }
            return;
        } else {
            File f = jfc.getSelectedFile();
            if (f != null) {
                if (f.getParentFile() != null) {
                    lastFile = f.getParentFile();
                }
            }
            return;
        }
    }

    public void event_loaduri() {
        String uri = JOptionPane.showInputDialog(this, "Insert URI to load from", "Load by URI", JOptionPane.OK_CANCEL_OPTION);
        if (uri != null) {
            String msg = load_uri(uri);
            if (msg != null) {
                error(msg);
            }
        }
    }

    public void event_new() {
        try {
            command_createnew();
            command_doc2editor();
        } catch (Exception e) {
            error("Failed to create new document: " + e);
        }
    }

    public void event_newtab() {
        try {
            String name = JOptionPane.showInputDialog(this, "Insert new tab name", "New tab", JOptionPane.OK_CANCEL_OPTION);
            if (name == null) return;
            name = name.trim();
            if (name.length() == 0) return;
            if (tabNames.containsKey(name)) {
                error("Cannot create a tab named: " + name + ", name exists.");
                return;
            }
            XQueryAdvancedEditPane aep = new XQueryAdvancedEditPane();
            editorTabs.addTab(name, aep);
            tabNames.put(name, "x");
            editorTabs.setSelectedIndex(editorTabs.getTabCount() - 1);
        } catch (Exception e) {
            error("Failed to create new tab: " + e);
        }
    }

    public void event_renametab() {
        try {
            String name = JOptionPane.showInputDialog(this, "Insert new tab name", "Rename tab", JOptionPane.OK_CANCEL_OPTION);
            if (name == null) return;
            name = name.trim();
            if (name.length() == 0) return;
            if (tabNames.containsKey(name)) {
                error("Cannot rename the tab to: " + name + ", name exists.");
                return;
            }
            tabNames.put(name, "x");
            int index = editorTabs.getSelectedIndex();
            String oldname = editorTabs.getTitleAt(index);
            tabNames.remove(oldname);
            editorTabs.setTitleAt(index, name);
        } catch (Exception e) {
            error("Failed to rename tab: " + e);
        }
    }

    public void event_deletetab() {
        try {
            if (editorTabs.getTabCount() > 0) {
                editorTabs.remove(editorTabs.getSelectedIndex());
            }
        } catch (Exception e) {
            error("Failed to delete tab: " + e);
        }
    }

    public void event_genXQuery() {
        try {
            XQWizardGenerator wizgen = new XQWizardGenerator(this.myFrame);
            wizgen.setVisible(true);
            if (wizgen.document != null) {
                xmd = wizgen.document;
                command_doc2editor();
                xmd.dumpDocTitles();
            }
        } catch (Exception e) {
            error("Failed to generate XQuery: " + e);
        }
    }

    /*** window listener implementation ***/
    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
        event_close();
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
        isOpen = true;
        registerAsEditor();
    }

    /*** procesor cooperation API ***/
    public void registerAsEditor() {
        if (myProcessor != null) {
            ((TavProcessor) myProcessor).setEditor(this);
        }
    }

    public void unregisterEditor() {
        if (myProcessor != null) {
            ((TavProcessor) myProcessor).setEditor(null);
        }
    }
}
