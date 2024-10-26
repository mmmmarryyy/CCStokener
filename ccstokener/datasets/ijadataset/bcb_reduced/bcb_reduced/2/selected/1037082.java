package org.gdbi.bkedit;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.gdbi.api.*;
import org.gdbi.prototype.graph.DescendantTree;
import org.gdbi.util.list.UListHelp;
import org.gdbi.util.parse.UParseGedcomParser;

/**
 */
public class BkeditApplet extends JApplet {

    public static boolean debug = false;

    private static final String FRAME_TITLE = "GDBI:BKEdit";

    private AppletBkeditParent editor = null;

    private DescendantTree graph = null;

    /**
     * Create editor window and open button.
     */
    public void init() {
        if (editor == null) {
            String[] gedcomArray = null;
            final String gedcomString = getParameter("lines");
            if (gedcomString != null) {
                gedcomArray = gedcomString.split("\\|");
            } else {
                final String filename = getParameter("file");
                gedcomArray = readFile(filename);
            }
            final UParseGedcomParser parser = UParseGedcomParser.create();
            final GdbiContext[] contexts = parser.parseGedcom(gedcomArray);
            final GdbiDatabase db = parser.getDatabase();
            db.dumpRecords(5);
            editor = new AppletBkeditParent(this, db);
        }
        if (graph == null) {
            GdbiIndi ancestor = editor.getCurrentIndi();
            boolean more = true;
            while (more) {
                ancestor.dump("finding ancestor");
                more = false;
                final GdbiFam[] famcs = ancestor.getFamCs();
                if (famcs.length > 0) {
                    final GdbiFam famc = famcs[0];
                    if (famc != null) {
                        GdbiIndi father = famc.getHusband();
                        if (father != null) {
                            ancestor = father;
                            more = true;
                        }
                    }
                }
            }
            graph = new DescendantTree(ancestor);
        }
        setLayout(new BorderLayout());
        final JLabel l1 = new JLabel("This applets pops up a BKEdit window...", SwingConstants.CENTER);
        final JButton b1 = new JButton("Open");
        add(l1, BorderLayout.CENTER);
        add(b1, BorderLayout.SOUTH);
        b1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showEditor();
            }
        });
    }

    /**
     */
    public void start() {
        showEditor();
    }

    /**
     */
    public void stop() {
    }

    private void showEditor() {
        graph.setVisible(true);
        graph.requestFocus();
        editor.frame.setVisible(true);
        editor.frame.requestFocus();
    }

    private String[] readFile(String filename) {
        final Vector<String> buf = new Vector<String>();
        try {
            final URL url = new URL(getCodeBase(), filename);
            final InputStream in = url.openStream();
            BufferedReader dis = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = dis.readLine()) != null) {
                buf.add(line);
            }
            in.close();
        } catch (IOException e) {
            System.out.println("catch: " + e);
            return null;
        }
        return buf.toArray(new String[0]);
    }

    /**
     * Applet implementation of BkeditParent.
     */
    private static class AppletBkeditParent implements BkeditParent {

        public final GdbiDatabase gedcom;

        public final BkeditGUI bkEdit;

        public final JFrame frame;

        private final BkeditFeatures features = new Features();

        private final Container parent;

        private AppletBkeditParent(Container parent, GdbiDatabase gedcom) {
            if (debug) System.err.println("AppletBkeditParent()");
            this.parent = parent;
            this.gedcom = gedcom;
            frame = new JFrame(FRAME_TITLE);
            System.err.println("calling bkEdit...");
            bkEdit = new BkeditGUI(this);
            System.err.println("bkEdit = " + bkEdit);
            bkEdit.dataFillNonNull(getCurrentIndi());
        }

        public BkeditFeatures getFeatures() {
            return features;
        }

        public JFrame getFrame() {
            return frame;
        }

        public GdbiDatabase getDatabase() {
            return gedcom;
        }

        /**
         * Only called when starting up the editor,
         * and since the applet uses a text database,
         * the first INDI is the current one.
         */
        public GdbiIndi getCurrentIndi() {
            return gedcom.getFirstIndi();
        }

        /**
         * Only needed for jll.
         */
        public JMenu getGuiMenu() {
            return null;
        }

        public void go_back() {
        }

        public void setCurrentIndi(GdbiIndi indi) {
        }

        /**
         * Called by BkeditGUI constructor.
         */
        public void addEdit(JPanel panel, JMenuBar menuBar) {
            if (debug) System.err.println("addEdit()");
            frame.setContentPane(panel);
            frame.setSize(bkEdit.dim640);
            frame.setJMenuBar(menuBar);
            frame.validate();
            if (parent != null) frame.setLocationRelativeTo(parent);
            frame.setVisible(true);
        }

        /**
         * Applet subclass of BkeditFeatures.
         *
         * We exclude ancestors to skip familytree jar.
         */
        private class Features extends BkeditFeatures {

            public boolean hasHelpAbout() {
                return true;
            }

            public boolean hasSetSize() {
                return true;
            }

            public void runHelpAbout(BkeditGUI bkedit) {
                JOptionPane.showMessageDialog(frame, UListHelp.getAboutLines(), UListHelp.getAboutTitle(bkedit.locale), JOptionPane.INFORMATION_MESSAGE);
            }

            public void runSetSize(Dimension dim) {
                frame.setSize(dim);
                frame.validate();
            }
        }
    }
}
