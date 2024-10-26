package net.sf.fir4j;

import icons.Icons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.Document;
import net.sf.fir4j.dialog.LanguageSettings;
import net.sf.fir4j.dialog.OptionsEdit;
import net.sf.fir4j.generator.Generator;
import net.sf.fir4j.listener.ExitListner;
import net.sf.fir4j.options.Options;
import net.sf.fir4j.options.OutputOptions.ModeChangeListener;
import net.sf.fir4j.view.DropDownButton;
import net.sf.fir4j.view.FileTree;
import net.sf.fir4j.view.ImageList;
import net.sf.fir4j.view.Menu;
import net.sf.fir4j.view.Message;
import net.sf.fir4j.view.Preview;
import net.sf.fir4j.view.Status;
import net.sf.fir4j.view.ImageList.ImageListListener;
import net.sf.fir4j.view.Message.MessageListener;

/**
 * The Main Class is the Entry Point of the Application and build the GUI
 */
public class Main extends JFrame {

    public static final String VERSION = "1.5";

    public static final String TITLE = "Fast Image Resizer for Java";

    public static final String ID = "fir4j";

    private static final long serialVersionUID = 332639523682607840L;

    public static final Color BACKGROUND = new Color(34, 34, 34);

    private Messages mes;

    private Preview preview;

    private FileTree fileTree;

    private Menu menu;

    public ImageList list;

    public Generator generator;

    public Status status;

    private Message outputDoc = Message.getInstance();

    private Main() {
        super();
        Options o = Options.getInstance();
        Messages.setLocale(o.getLocal());
        mes = Messages.getInstance();
        setTitle(TITLE);
        setIconImage(Icons.get("main-icon").getImage());
        generator = new Generator(this);
        preview = new Preview(this);
        status = new Status();
        list = new ImageList(status);
        preview.listenTo(list);
        init(o);
        if (o.isInitial()) {
            setEnabled(false);
            try {
                new LanguageSettings(this);
                o.setInitial(false);
                new OptionsEdit(this);
            } finally {
                setEnabled(true);
            }
            status.update();
            requestFocus();
            restart();
        }
        list.addImageListListener(new ImageListListener() {

            public void sizeChanged(int size) {
                menu.gallerie.setEnabled((size > 0));
                menu.zippen.setEnabled((size > 0));
                menu.gener.setEnabled((size > 0));
            }

            public void acceptedFileDND(File file) {
                outputDoc.insertLine("Accepted file: " + file.getName());
            }

            public void rejectedFileDND(File file) {
                outputDoc.insertLine("Rejected file: " + file.getName());
            }

            public void rejectedURLDND(URL url) {
                outputDoc.insertLine("Rejected file: " + url.toString());
            }
        });
    }

    private void resetLog(Options o) {
        preview.repaint();
        outputDoc.clear();
        outputDoc.insertLine(TITLE + " " + VERSION);
        outputDoc.insertLine("");
        outputDoc.insertLine(mes.getString("OptionsEdit.9") + " " + o.getMode());
        switch(o.getMode()) {
            case resizeAndCrop:
                outputDoc.insertLine(o.getMode().description());
                outputDoc.insertLine("");
                outputDoc.insertLine(o.getFormatMode().description());
                break;
            case resizeAndFill:
                outputDoc.insertLine(o.getMode().description() + " (" + colorToHtmlCode(o.getBackgroundColor()) + ")");
                outputDoc.insertLine("");
                outputDoc.insertLine(o.getFormatMode().description());
                break;
            default:
                outputDoc.insertLine(o.getMode().description());
                break;
        }
    }

    public static String colorToHtmlCode(Color c) {
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        String s = "#";
        if (red < 16) s += "0";
        s += Integer.toHexString(red);
        if (green < 16) s += "0";
        s += Integer.toHexString(green);
        if (blue < 16) s += "0";
        s += Integer.toHexString(blue);
        return s;
    }

    private void init(final Options o) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsDevice gd = gs[0];
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        try {
            UIManager.setLookAndFeel(o.getLookAndFeel());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception l) {
        }
        Rectangle bounds = gc.getBounds();
        this.setLocation(Math.max((bounds.width / 2) - 450, 0), Math.max((bounds.height / 2) - 350, 0));
        this.setSize(900, 700);
        setMinimumSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(900, 700));
        setResizable(true);
        addWindowListener(new ExitListner());
        menu = new Menu(this);
        resetLog(o);
        o.addModeChangeListener(new ModeChangeListener() {

            @Override
            public void modeChangedEvent(String property) {
                resetLog(o);
            }
        });
        log = new JTextArea();
        log.setVisible(true);
        log.setEditable(false);
        Font font = new Font("Courier", Font.PLAIN, 11);
        log.setForeground(Color.WHITE);
        log.setFont(font);
        log.setDocument(outputDoc.getDocument());
        log.setBackground(new Color(34, 34, 34));
        outputDoc.addMessageListener(new MessageListener() {

            public void textValueChanged(Document document) {
                try {
                    log.setCaretPosition(document.getLength());
                } catch (IllegalArgumentException ignore) {
                }
            }
        });
        status = new Status();
        fileTree = new FileTree(this, list);
        fileTree.setMinimumSize(new Dimension(300, 600));
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.add(menu.exit);
        toolbar.add(menu.gener);
        toolbar.add(menu.zippen);
        toolbar.add(menu.gallerie);
        toolbar.addSeparator(new Dimension(30, 20));
        JButton dropDownButton2 = new DropDownButton(menu.view, menu.extendedView);
        toolbar.add(menu.view);
        toolbar.add(dropDownButton2);
        JButton dropDownButton = new DropDownButton(menu.settings, menu.extendedSettings);
        toolbar.add(menu.settings);
        toolbar.add(dropDownButton);
        reLayout(o);
        setVisible(true);
    }

    private JToolBar toolbar;

    private JTextArea log;

    public void reLayout(final Options o) {
        Container c = getContentPane();
        c.removeAll();
        c.setLayout(new BorderLayout());
        JPanel obenrechts = new JPanel();
        obenrechts.setLayout(new BorderLayout());
        obenrechts.add(list, BorderLayout.CENTER);
        obenrechts.add(preview, BorderLayout.EAST);
        JComponent oben = obenrechts;
        if (o.isShowtree()) {
            JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(fileTree), obenrechts);
            pane.setContinuousLayout(true);
            pane.setOneTouchExpandable(true);
            pane.setResizeWeight(0.1D);
            oben = pane;
        }
        JComponent center = oben;
        if (o.isShowlog()) {
            JScrollPane jsp = new JScrollPane();
            jsp = new JScrollPane(log);
            jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            jsp.add(log);
            jsp.setViewportView(log);
            JSplitPane jsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, oben, jsp);
            jsplit.setContinuousLayout(true);
            jsplit.setOneTouchExpandable(true);
            jsplit.setResizeWeight(0.7D);
            center = jsplit;
        }
        c.add(center, BorderLayout.CENTER);
        c.add(status, BorderLayout.SOUTH);
        c.add(toolbar, BorderLayout.NORTH);
        list.updateView();
        setVisible(false);
        setVisible(true);
    }

    private static final void printlicense() {
        System.out.println();
        System.out.println(TITLE + " " + VERSION);
        System.out.println();
        System.out.println("Copyright (C) 2008-2009 Christian Schulz (www.operatorplease.de)");
        System.out.println();
        System.out.println("Based on Java Mass JPEG Resizer Tool by Johannes Geppert (as at January 2008)");
        System.out.println();
        System.out.println("This program is free software; you can redistribute it and/or modify it");
        System.out.println("under the terms of the GNU General Public License as published by the Free");
        System.out.println("Software Foundation; either version 2 of the License, or (at your option)");
        System.out.println("any later version.");
        System.out.println();
        System.out.println("This program is distributed in the hope that it will be useful, but");
        System.out.println("WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY");
        System.out.println("or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for");
        System.out.println("more details.");
        System.out.println();
        System.out.println("You should have received a copy of the GNU General Public License along");
        System.out.println("with this program; if not, see http://www.gnu.org/licenses/");
        System.out.println();
    }

    public static void main(String[] args) {
        printlicense();
        if (args.length == 0) {
            new Main();
            return;
        }
        Messages mes = Messages.getInstance();
        if (args.length == 1 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("/?") || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help"))) {
            System.out.println(System.getProperty("line.separator") + mes.getString("Main.5") + System.getProperty("line.separator"));
            System.out.println("\t\t" + mes.getString("Main.7") + " quality=75 hmax=300 vmax=200");
            System.out.println("\t\t" + mes.getString("Main.7") + " quality=75 hmax=300");
            System.out.println("\t\t" + mes.getString("Main.7") + " quality=75 vmax=200");
            System.out.println();
            System.out.println(mes.getString("Main.10"));
            System.out.println(mes.getString("Main.11"));
            System.out.println(mes.getString("Main.12"));
            System.out.println(mes.getString("Main.13"));
            System.out.println(mes.getString("Main.14"));
            System.out.println(mes.getString("Main.15"));
        }
        String input = null;
        String output = null;
        String quality = null;
        String hmax = null;
        String vmax = null;
        File fi = null;
        File fo = null;
        int q = 75;
        int h = 0;
        int v = 0;
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            try {
                if (args[i].substring(0, 4).equalsIgnoreCase("hmax")) hmax = args[i].substring(args[i].lastIndexOf(mes.getString("=")) + 1, args[i].length()).trim();
                if (args[i].substring(0, 4).equalsIgnoreCase("vmax")) vmax = args[i].substring(args[i].lastIndexOf(mes.getString("=")) + 1, args[i].length()).trim();
                if (args[i].substring(0, 5).equalsIgnoreCase("input")) input = args[i].substring(args[i].lastIndexOf(mes.getString("=")) + 1, args[i].length()).trim();
                if (args[i].substring(0, 6).equalsIgnoreCase("output")) output = args[i].substring(args[i].lastIndexOf(mes.getString("=")) + 1, args[i].length()).trim();
                if (args[i].substring(0, 7).equalsIgnoreCase("quality")) quality = args[i].substring(args[i].lastIndexOf(mes.getString("=")) + 1, args[i].length()).trim();
            } catch (Exception e) {
            }
        }
        if (input == null) {
            System.out.println(mes.getString("Main.26", "input"));
            System.exit(0);
        } else fi = new File(input);
        if (output == null) {
            System.out.println(mes.getString("Main.26", "output"));
            System.exit(0);
        } else fo = new File(output);
        if (quality == null) {
            System.out.println(mes.getString("Main.26", "quality"));
            System.exit(0);
        } else q = Integer.parseInt(quality);
        if (hmax == null && vmax == null) {
            System.out.println(mes.getString("Main.29"));
            System.exit(0);
        } else {
            if (hmax != null) h = Integer.parseInt(hmax);
            if (vmax != null) v = Integer.parseInt(vmax);
        }
        System.out.println("input=" + fi.toString() + ", output=" + fo.toString() + ", quality=" + q);
        new Generator(null).generateText(fi, fo, h, v);
    }

    /**
	 * <p>
	 * set the Look and Feel of this Application
	 * </p>
	 *
	 * @param look
	 *          the LookAndFeel classname
	 */
    public void setLookFeel(String look) {
        try {
            UIManager.setLookAndFeel(look);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception l) {
            outputDoc.insert(mes.getString("Main.33") + look + Message.LS);
        }
    }

    /**
	 * open the OptionsEdit Dialog
	 */
    public void openOptions() {
        setEnabled(false);
        try {
            new OptionsEdit(this);
        } finally {
            setEnabled(true);
        }
        status.update();
        requestFocus();
    }

    public void openLocaleSettings() {
        setEnabled(false);
        try {
            Locale before = Options.getInstance().getLocal();
            new LanguageSettings(this);
            Locale after = Options.getInstance().getLocal();
            if (!after.equals(before)) restart();
        } finally {
            setEnabled(true);
        }
        status.update();
        requestFocus();
    }

    /**
	 * restart the GUI, after changing the Language
	 */
    public void restart() {
        this.dispose();
        new Main();
    }

    public boolean isNeverVersions(String other) {
        if (other == null || other.length() <= 0) return false;
        try {
            String remoteParts[] = other.split("\\.");
            String localParts[] = VERSION.split("\\.");
            for (int i = 0; i < Math.min(localParts.length, remoteParts.length); i++) {
                int remote = Integer.parseInt(remoteParts[i]);
                int local = Integer.parseInt(localParts[i]);
                if (remote == local) continue;
                return (remote > local);
            }
            if (remoteParts.length > localParts.length) return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
	 * check version on the server
	 */
    public void checkUpdate() {
        try {
            URL url = new URL("http://www.operatorplease.de/versions");
            URLConnection uc = url.openConnection();
            uc.connect();
            Properties props = new Properties();
            try {
                InputStream is = uc.getInputStream();
                props.load(is);
                is.close();
            } catch (FileNotFoundException e) {
                System.out.println("Properties file 'versions' not found!");
            }
            String version = props.getProperty(ID);
            System.out.println("update: id=" + ID + ", version=" + version);
            if (isNeverVersions(version)) {
                JOptionPane.showMessageDialog(this, mes.getString("Update.found") + "\nVersion: " + version, mes.getString("Update.check"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, mes.getString("Update.none"), mes.getString("Update.check"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String err = mes.getString("Update.error") + "\n" + ex.getLocalizedMessage();
            err += "\n\n";
            err += mes.getString("Update.checkWebsite");
            JOptionPane.showMessageDialog(this, err, mes.getString("Update.check"), JOptionPane.ERROR_MESSAGE);
            System.err.println(err);
        }
    }
}
