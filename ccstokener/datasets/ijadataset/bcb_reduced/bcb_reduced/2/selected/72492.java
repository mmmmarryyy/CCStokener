package org.xul.script.viewer;

import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.PropertyResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.xul.script.xul.DefaultScriptManager;
import org.xul.script.xul.model.XULDocument;

/** A simple XUL file Viewer, with javaScript enabled.
 *
 * @version 0.3
 */
public class XULViewer extends JFrame {

    private JTabbedPane pane = new JTabbedPane();

    private JMenuItem closeItem = null;

    public XULViewer() {
        super("XUL Viewer");
        URL url = Thread.currentThread().getContextClassLoader().getResource("org/xul/script/resources/xul.properties");
        try {
            PropertyResourceBundle prb = new PropertyResourceBundle(url.openStream());
            String version = prb.getString("version");
            System.out.println("XULViewer version " + version);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.setLayout(new BorderLayout());
        this.add(pane);
        constructPanel();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(500, 500);
    }

    private void constructPanel() {
        JMenu fileMenu = new JMenu("File");
        JMenuBar mbar = new JMenuBar();
        this.setJMenuBar(mbar);
        mbar.add(fileMenu);
        AbstractAction exitAction = new AbstractAction("Exit") {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        AbstractAction openAction = new AbstractAction("Open") {

            public void actionPerformed(ActionEvent e) {
                openXULFile();
            }
        };
        fileMenu.add(new JMenuItem(openAction));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(exitAction));
        pane.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    manageTabs(e);
                }
            }
        });
    }

    protected void manageTabs(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        JPopupMenu menu = new JPopupMenu();
        JMenuItem close = getCloseItem();
        menu.add(close);
        menu.show(pane, x, y);
    }

    private void removeSelectedTab() {
        pane.remove(pane.getSelectedIndex());
    }

    private void openXULFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle("Select XUL File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            doOpenXULFile(file);
        }
    }

    private void doOpenXULFile(File file) {
        DefaultScriptManager manager = new DefaultScriptManager();
        try {
            URL boxes = file.toURL();
            XULDocument doc = manager.addXULScript(boxes.toString(), boxes);
            JComponent comp = doc.getRootComponent();
            if (comp != null) {
                pane.addTab(doc.getFile().getName(), new JScrollPane(comp));
            }
            manager.setActive(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static final void main(String[] args) {
        XULViewer sample = new XULViewer();
        sample.setVisible(true);
    }

    /** Return a default close item for the selected application tab.
     */
    protected final JMenuItem getCloseItem() {
        if (closeItem == null) {
            closeItem = new JMenuItem("Close");
            closeItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    removeSelectedTab();
                }
            });
        }
        return closeItem;
    }
}
