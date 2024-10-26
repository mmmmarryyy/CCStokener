package de.wiedmann.drilloverimage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;

/**
 * @author walter
 *
 */
public class DrillOverImage extends JFrame implements ActionListener, ComponentListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static DrillOverImage fenster;

    private String version;

    private String revision;

    private Date compileDate;

    private String pathImage;

    private String fileImage;

    private String pathDrill;

    private String fileDrill;

    private JLabel jLabelImage;

    private JLabel jLabelDrill;

    private JMenuBar jMenuBarMain;

    private JMenuItem jMenuItemSelectImage;

    private JMenuItem jMenuItemSelectDrill;

    private JMenuItem jMenuItemZoomPlus;

    private JMenuItem jMenuItemZoomNormal;

    private JMenuItem jMenuItemZoomMinus;

    private JScrollPane jScrollPane;

    private double zoom = 1;

    Image bild = null;

    private String ausgabe = "Bitte Baugruppenbild auswahlen!";

    class Leinwand extends JPanel {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        private int breite = 0;

        private int hoehe = 0;

        private double breiteZoom = 1;

        private double hoeheZoom = 1;

        public void paintComponent(Graphics g) {
            super.paintComponents(g);
            int x, y;
            if (bild == null) {
                g.setColor(Color.RED);
                g.setFont(new Font("Georgia", Font.ITALIC, 30));
                FontMetrics fm = g.getFontMetrics();
                x = (this.getWidth() - fm.stringWidth(ausgabe)) / 2;
                if (x < 0) x = 0;
                y = (this.getHeight() - fm.getLeading()) / 2;
                if (y < 0) y = 0;
                g.drawString(ausgabe, x, y);
            } else {
                int tempBreite = (int) (bild.getWidth(this) * breiteZoom * zoom);
                int tempHoehe = (int) (bild.getHeight(this) * breiteZoom * zoom);
                if (tempBreite != breite || tempHoehe != hoehe) {
                    breite = tempBreite;
                    hoehe = tempHoehe;
                    this.setPreferredSize(new Dimension(breite, hoehe));
                    jScrollPane.revalidate();
                }
                g.drawImage(bild, 0, 0, breite, hoehe, this);
            }
        }
    }

    Leinwand leinwand;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        fenster = new DrillOverImage();
        fenster.setSize(800, 600);
        fenster.setLocationByPlatform(true);
        fenster.setVisible(true);
    }

    public DrillOverImage() {
        super();
        setTitle("Drill over Image");
        try {
            Image logo;
            logo = ImageIO.read(ClassLoader.getSystemResource("images/logo.gif"));
            this.setIconImage(logo);
        } catch (IOException e) {
            System.err.println("Can't load programm - icon!");
        } catch (Exception e) {
            System.err.println("Can't load programm - icon!");
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel jPanelMain = new JPanel();
        jMenuBarMain = new JMenuBar();
        jMenuBarMain.add(buildFileMenu());
        jMenuBarMain.add(buildZoomMenu());
        jMenuBarMain.add(buildHelpMenu());
        JPanel jPanelMenu = new JPanel();
        jPanelMenu.setLayout(new BorderLayout());
        jPanelMenu.add(jMenuBarMain, BorderLayout.NORTH);
        jPanelMenu.add(buildToolBar(), BorderLayout.CENTER);
        jPanelMain.setLayout(new BorderLayout());
        jPanelMain.add(jPanelMenu, BorderLayout.NORTH);
        leinwand = new Leinwand();
        jScrollPane = new JScrollPane();
        jScrollPane.getViewport().add(leinwand);
        jPanelMain.add(jScrollPane, BorderLayout.CENTER);
        jScrollPane.getVerticalScrollBar().addComponentListener(this);
        jScrollPane.getHorizontalScrollBar().addComponentListener(this);
        JPanel jPanelStatus = new JPanel(new GridLayout(3, 0));
        jLabelImage = new JLabel("Selected image file: ?");
        jPanelStatus.add(jLabelImage);
        jLabelDrill = new JLabel("Selected drill file: ?");
        jPanelStatus.add(jLabelDrill);
        jPanelMain.add(jPanelStatus, BorderLayout.SOUTH);
        getContentPane().add(jPanelMain);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("actionPerformed: " + e.getActionCommand());
        if ("quit".equals(e.getActionCommand())) {
            System.exit(0);
            return;
        }
        if ("zoomin".equals(e.getActionCommand())) {
            zoom *= 1.1;
            System.out.println("Zoomfaktor: " + zoom);
            jScrollPane.repaint();
            return;
        }
        if ("zoomnormal".equals(e.getActionCommand())) {
            zoom = 1;
            System.out.println("Zoomfaktor: " + zoom);
            jScrollPane.repaint();
            return;
        }
        if ("zoomout".equals(e.getActionCommand())) {
            zoom /= 1.1;
            System.out.println("Zoomfaktor: " + zoom);
            jScrollPane.repaint();
            return;
        }
        if ("help".equals(e.getActionCommand())) {
            readVersion();
            String message = "Version: " + version + "\n" + "Rev.: " + revision + "\n" + "Date: " + compileDate.toString() + "\n" + "Default file encoding: " + System.getProperty("file.encoding");
            JOptionPane.showMessageDialog(this, message, "Programminfos", JOptionPane.PLAIN_MESSAGE, new ImageIcon(ClassLoader.getSystemResource("images/logo.gif")));
            return;
        } else {
            FileDialog dialog = new FileDialog(this, "Choose File", FileDialog.LOAD);
            if ("image...".equals(e.getActionCommand())) {
                if (pathImage != null) {
                    dialog.setDirectory(pathImage);
                } else {
                    if (pathDrill != null) {
                        dialog.setDirectory(pathDrill);
                    }
                }
                if (fileImage != null) {
                    dialog.setFile(fileImage);
                }
                dialog.setTitle("Choose Image - File");
            }
            if ("drill...".equals(e.getActionCommand())) {
                if (pathDrill != null) {
                    dialog.setDirectory(pathDrill);
                } else {
                    if (pathImage != null) {
                        dialog.setDirectory(pathImage);
                    }
                }
                if (fileDrill != null) {
                    dialog.setFile(fileDrill);
                }
                dialog.setTitle("Choose Drill - File");
            }
            dialog.setVisible(true);
            if ("image...".equals(e.getActionCommand())) {
                if (dialog.getFile() != null) {
                    fileImage = dialog.getFile();
                    pathImage = dialog.getDirectory();
                    jLabelImage.setText("Selected image file:  " + pathImage + fileImage);
                }
                try {
                    bild = ImageIO.read(new File(pathImage + fileImage));
                    zoom = 1;
                    leinwand.repaint();
                } catch (IOException exp) {
                    System.err.println(exp.getMessage());
                    bild = null;
                }
            }
            if ("drill...".equals(e.getActionCommand())) {
                if (dialog.getFile() != null) {
                    fileDrill = dialog.getFile();
                    pathDrill = dialog.getDirectory();
                    jLabelDrill.setText("Selected drill file:  " + pathDrill + fileDrill);
                }
            }
        }
    }

    private void readVersion() {
        URL url = ClassLoader.getSystemResource("version");
        if (url == null) {
            return;
        }
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Version=")) {
                    version = (line.split("="))[1];
                }
                if (line.startsWith("Revision=")) {
                    revision = (line.split("="))[1];
                }
                if (line.startsWith("Date=")) {
                    String sSec = (line.split("="))[1];
                    Long lSec = Long.valueOf(sSec);
                    compileDate = new Date(lSec);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    private JMenu buildFileMenu() {
        JMenu jMenu = new JMenu("File");
        jMenu.setMnemonic('F');
        jMenuItemSelectImage = new JMenuItem("Select image file", 'i');
        jMenuItemSelectImage.setAccelerator(KeyStroke.getKeyStroke('I', Event.CTRL_MASK));
        jMenuItemSelectImage.setActionCommand("image...");
        jMenuItemSelectImage.addActionListener(this);
        jMenu.add(jMenuItemSelectImage);
        jMenuItemSelectDrill = new JMenuItem("Select drill file", 'd');
        jMenuItemSelectDrill.setAccelerator(KeyStroke.getKeyStroke('D', Event.CTRL_MASK));
        jMenuItemSelectDrill.setActionCommand("drill...");
        jMenuItemSelectDrill.addActionListener(this);
        jMenu.add(jMenuItemSelectDrill);
        jMenu.addSeparator();
        JMenuItem jMenuItem = new JMenuItem("Quit", 'q');
        jMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', Event.CTRL_MASK));
        jMenuItem.setActionCommand("quit");
        jMenuItem.addActionListener(this);
        jMenu.add(jMenuItem);
        return jMenu;
    }

    private JMenu buildZoomMenu() {
        JMenu jMenu = new JMenu("Zoom");
        jMenu.setMnemonic('Z');
        jMenuItemZoomPlus = new JMenuItem("Zoom IN", 'O');
        jMenuItemZoomPlus.setAccelerator(KeyStroke.getKeyStroke('O', Event.CTRL_MASK));
        jMenuItemZoomPlus.setActionCommand("zoomin");
        jMenuItemZoomPlus.addActionListener(this);
        jMenu.add(jMenuItemZoomPlus);
        jMenuItemZoomNormal = new JMenuItem("Zoom Normal", '0');
        jMenuItemZoomNormal.setAccelerator(KeyStroke.getKeyStroke('0', Event.CTRL_MASK));
        jMenuItemZoomNormal.setActionCommand("zoomnormal");
        jMenuItemZoomNormal.addActionListener(this);
        jMenu.add(jMenuItemZoomNormal);
        jMenuItemZoomMinus = new JMenuItem("Zoom OUT", 'P');
        jMenuItemZoomMinus.setAccelerator(KeyStroke.getKeyStroke('P', Event.CTRL_MASK));
        jMenuItemZoomMinus.setActionCommand("zoomout");
        jMenuItemZoomMinus.addActionListener(this);
        jMenu.add(jMenuItemZoomMinus);
        return jMenu;
    }

    private JMenu buildHelpMenu() {
        JMenu jMenu = new JMenu("Help");
        jMenu.setMnemonic('H');
        JMenuItem jMenuItem = new JMenuItem("Help", 'h');
        jMenuItem.setAccelerator(KeyStroke.getKeyStroke('H', Event.CTRL_MASK));
        jMenuItem.setActionCommand("help");
        jMenuItem.addActionListener(this);
        jMenu.add(jMenuItem);
        return jMenu;
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar();
        return toolBar;
    }

    @Override
    public void componentHidden(ComponentEvent arg0) {
        System.out.println("componentHidden");
        this.jMenuItemZoomMinus.setEnabled(jScrollPane.getVerticalScrollBar().isVisible() || jScrollPane.getHorizontalScrollBar().isVisible());
    }

    @Override
    public void componentMoved(ComponentEvent arg0) {
        System.out.println("componentMoved");
    }

    @Override
    public void componentResized(ComponentEvent arg0) {
        System.out.println("componentResized");
    }

    @Override
    public void componentShown(ComponentEvent arg0) {
        System.out.println("componentShown");
        this.jMenuItemZoomMinus.setEnabled((jScrollPane.getVerticalScrollBar().isVisible() || jScrollPane.getHorizontalScrollBar().isVisible()));
    }
}
