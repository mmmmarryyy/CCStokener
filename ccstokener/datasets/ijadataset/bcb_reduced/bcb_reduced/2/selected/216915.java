package de.wiedmann.testabilityreport;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import de.wiedmann.assembyexpert.ParallelOutput;
import de.wiedmann.board.Board;
import de.wiedmann.partlist.ProAlphaPartsList;
import de.wiedmann.tablecomparator.DoubleComparator;
import de.wiedmann.tablecomparator.IntegerComparator;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

/**
 * @author Walter Wiedmann
 * @version 0.1.0
 * Create / Erstellt: 02.09.2008 
 *
 */
public class TestabilityReport extends JFrame implements ActionListener {

    Color COLOR_NEEDFUL = new Color(0xB0E0E6);

    Color COLOR_OK = Color.GREEN;

    Color COLOR_FAIL = Color.RED;

    Color COLOR_DO = new Color(0x00BFFF);

    private String version;

    private String revision;

    private Date compileDate;

    private String pathPl;

    private String filePl;

    private String pathParallel;

    private String fileParallel;

    private String pathLog;

    private String fileLog;

    private JLabel jLabelPl;

    private JLabel jLabelParallel;

    private JLabel jLabelLog;

    private JButton jToolButtonSelectPl;

    private JButton jToolButtonSelectParallel;

    private JButton jToolButtonSelectLog;

    private JButton jToolButtonReadPl;

    private JButton jToolButtonReadParallel;

    private JButton jToolButtonReadLog;

    private JButton jToolButtonTar;

    private JButton jToolButtonTpg;

    private JMenuBar jMenuBarMain;

    private JMenuItem jMenuItemSelectPl;

    private JMenuItem jMenuItemSelectParallel;

    private JMenuItem jMenuItemSelectLog;

    private JMenuItem jMenuItemReadPl;

    private JMenuItem jMenuItemReadParallel;

    private JMenuItem jMenuItemReadLog;

    private JMenuItem jMenuItemTpg;

    private JMenuItem jMenuItemTar;

    private Board board;

    private JTabbedPane mainPane;

    boolean readyPl;

    boolean readyParallel;

    boolean readyLog;

    private Vector<String> newValuesHeaders;

    private Vector<Vector> newValuesVectors;

    private JTable newValuesTable;

    private Vector<String> tarHeaders;

    private Vector<Vector> tarVectors;

    private JTable tarTable;

    ;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        TestabilityReport tar = new TestabilityReport();
        tar = null;
    }

    public TestabilityReport() {
        super();
        newValuesVectors = new Vector<Vector>();
        tarVectors = new Vector<Vector>();
        readyPl = false;
        readyParallel = false;
        readyLog = false;
        setTitle("Testability report");
        try {
            Image logo;
            logo = ImageIO.read(ClassLoader.getSystemResource("images/logo.gif"));
            this.setIconImage(logo);
        } catch (IOException e) {
            System.err.println("Can't load programm - icon!");
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel jPanelMain = new JPanel();
        jMenuBarMain = new JMenuBar();
        jMenuBarMain.add(buildFileMenu());
        jMenuBarMain.add(buildReadMenu());
        jMenuBarMain.add(buildPrintMenu());
        jMenuBarMain.add(buildHelpMenu());
        JPanel jPanelMenu = new JPanel();
        jPanelMenu.setLayout(new BorderLayout());
        jPanelMenu.add(jMenuBarMain, BorderLayout.NORTH);
        jPanelMenu.add(buildToolBar(), BorderLayout.CENTER);
        jPanelMain.setLayout(new BorderLayout());
        jPanelMain.add(jPanelMenu, BorderLayout.NORTH);
        mainPane = new JTabbedPane();
        jPanelMain.add(mainPane, BorderLayout.CENTER);
        JPanel jPanelStatus = new JPanel(new GridLayout(3, 0));
        jLabelPl = new JLabel("Selected partlist file: ?");
        jPanelStatus.add(jLabelPl);
        jLabelParallel = new JLabel("Selected parallel file: ?");
        jPanelStatus.add(jLabelParallel);
        jLabelLog = new JLabel("Selected gernad log: ?");
        jPanelStatus.add(jLabelLog);
        jPanelMain.add(jPanelStatus, BorderLayout.SOUTH);
        getContentPane().add(jPanelMain);
        pack();
        setSize(800, 600);
        this.setLocationByPlatform(true);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if ("quit".equals(e.getActionCommand())) {
            System.exit(0);
            return;
        }
        if ("help".equals(e.getActionCommand())) {
            readVersion();
            String message = "Version: " + version + "\n" + "Rev.: " + revision + "\n" + "Date: " + compileDate.toString() + "\n" + "Default file encoding: " + System.getProperty("file.encoding");
            JOptionPane.showMessageDialog(this, message, "Programminfos", JOptionPane.PLAIN_MESSAGE, new ImageIcon(ClassLoader.getSystemResource("images/logo.gif")));
            return;
        }
        if ("readPartList".equals(e.getActionCommand())) {
            if (filePl != null) {
                File partsList = null;
                partsList = new File(pathPl + filePl);
                if (partsList.canRead()) {
                    ProAlphaPartsList pList = new ProAlphaPartsList();
                    board = new Board(pList.importPartList(partsList));
                    tarTable = new JTable(board);
                    tarTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>();
                    tarTable.setRowSorter(sorter);
                    sorter.setModel(board);
                    IntegerComparator integerComparator = new IntegerComparator();
                    sorter.setComparator(1, integerComparator);
                    sorter.setComparator(23, integerComparator);
                    DoubleComparator doubleComparator = new DoubleComparator();
                    for (int i = 4; i <= 22; i++) {
                        if (i == 10) {
                            continue;
                        }
                        sorter.setComparator(i, doubleComparator);
                    }
                    mainPane.addTab("Testabilityreport", new JScrollPane(tarTable));
                    readyPl = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Can't not read file: " + pathPl + filePl + ".", "File error", JOptionPane.ERROR_MESSAGE);
                }
            }
            setButtonStatus();
            return;
        }
        if ("readParallel".equals(e.getActionCommand())) {
            if (fileParallel != null) {
                File parallel = null;
                parallel = new File(pathParallel + fileParallel);
                if (parallel.canRead()) {
                    ParallelOutput parallelParser = new ParallelOutput();
                    parallelParser.addParallelParts(parallel, board);
                    readyParallel = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Can't not read file: " + pathParallel + fileParallel + ".", "File error", JOptionPane.ERROR_MESSAGE);
                }
            }
            setButtonStatus();
            return;
        }
        if ("readLogFile".equals(e.getActionCommand())) {
            File log = null;
            log = new File(pathLog + fileLog);
            if (log.canRead()) {
                board.readGenradLogFile(log);
                readyLog = true;
            } else {
                JOptionPane.showMessageDialog(this, "Can't not read file: " + pathLog + fileLog + ".", "File error", JOptionPane.ERROR_MESSAGE);
            }
            setButtonStatus();
            return;
        }
        if ("printNewTpgValues".equals(e.getActionCommand())) {
            board.printNewTpgValues();
            return;
        }
        if ("printTar".equals(e.getActionCommand())) {
            board.printTestabilityReport();
        } else {
            FileDialog dialog = new FileDialog(this, "Choose File", FileDialog.LOAD);
            if ("partlist...".equals(e.getActionCommand())) {
                if (pathPl != null) {
                    dialog.setDirectory(pathPl);
                } else {
                    if (pathParallel != null) {
                        dialog.setDirectory(pathParallel);
                    } else {
                        if (pathLog != null) {
                            dialog.setDirectory(pathLog);
                        }
                    }
                }
                if (filePl != null) {
                    dialog.setFile(filePl);
                }
                dialog.setTitle("Choose PartList - File");
                JOptionPane.showMessageDialog(this, "The partlist output must be issued in ProAlpha\n" + "with the encoding ISO-8859-1!\n" + "Also is it a good idea to replace the µ thru a u.", "Notice to coding of the partlist", JOptionPane.INFORMATION_MESSAGE);
            }
            if ("parallel...".equals(e.getActionCommand())) {
                if (pathParallel != null) {
                    dialog.setDirectory(pathParallel);
                } else {
                    if (pathPl != null) {
                        dialog.setDirectory(pathPl);
                    } else {
                        if (pathLog != null) {
                            dialog.setDirectory(pathLog);
                        }
                    }
                }
                if (fileParallel != null) {
                    dialog.setFile(fileParallel);
                }
                dialog.setTitle("Choose Parallel - File");
            }
            if ("log...".equals(e.getActionCommand())) {
                if (pathLog != null) {
                    dialog.setDirectory(pathLog);
                } else {
                    if (pathPl != null) {
                        dialog.setDirectory(pathPl);
                    } else {
                        if (pathParallel != null) {
                            dialog.setDirectory(pathParallel);
                        }
                    }
                }
                if (fileLog != null) {
                    dialog.setFile(fileLog);
                }
                dialog.setTitle("Choose Log - File");
            }
            dialog.setVisible(true);
            if ("partlist...".equals(e.getActionCommand())) {
                if (dialog.getFile() != null) {
                    filePl = dialog.getFile();
                    pathPl = dialog.getDirectory();
                    jLabelPl.setText("Selected partlist file:  " + pathPl + filePl);
                }
            }
            if ("parallel...".equals(e.getActionCommand())) {
                if (dialog.getFile() != null) {
                    fileParallel = dialog.getFile();
                    pathParallel = dialog.getDirectory();
                    jLabelParallel.setText("Selected parallel file:  " + pathParallel + fileParallel);
                }
            }
            if ("log...".equals(e.getActionCommand())) {
                if (dialog.getFile() != null) {
                    fileLog = dialog.getFile();
                    pathLog = dialog.getDirectory();
                    jLabelLog.setText("Selected genrad log:  " + pathLog + fileLog);
                }
            }
            setButtonStatus();
        }
    }

    /**
	 * Function set the buttons
	 */
    private void setButtonStatus() {
        if (filePl != null) {
            jToolButtonSelectPl.setBackground(COLOR_OK);
            jToolButtonReadPl.setEnabled(true);
            jToolButtonReadPl.setBackground(readyPl ? COLOR_OK : COLOR_NEEDFUL);
            jMenuItemReadPl.setEnabled(true);
        } else {
            jToolButtonReadPl.setEnabled(false);
            jToolButtonReadPl.setBackground(null);
            jMenuItemReadPl.setEnabled(false);
        }
        if (fileParallel != null) {
            jToolButtonSelectParallel.setBackground(COLOR_OK);
            jToolButtonReadParallel.setEnabled(readyPl);
            jToolButtonReadParallel.setBackground(readyPl ? (readyParallel ? COLOR_OK : COLOR_NEEDFUL) : null);
            jMenuItemReadParallel.setEnabled(readyPl);
        } else {
            jToolButtonReadParallel.setEnabled(false);
            jToolButtonReadParallel.setBackground(null);
            jMenuItemReadParallel.setEnabled(false);
        }
        if (fileLog != null) {
            jToolButtonSelectLog.setBackground(COLOR_OK);
            jToolButtonReadLog.setEnabled(readyParallel);
            jToolButtonReadLog.setBackground(readyParallel ? (readyLog ? COLOR_OK : COLOR_NEEDFUL) : null);
            jMenuItemReadLog.setEnabled(readyParallel);
        } else {
            jToolButtonReadLog.setEnabled(false);
            jToolButtonReadLog.setBackground(null);
            jMenuItemReadLog.setEnabled(false);
        }
        jToolButtonTpg.setEnabled(readyParallel);
        jToolButtonTpg.setBackground(readyParallel ? COLOR_DO : null);
        jMenuItemTpg.setEnabled(readyParallel);
        jToolButtonTar.setEnabled(readyLog);
        jToolButtonTar.setBackground(readyLog ? COLOR_DO : null);
        jMenuItemTar.setEnabled(readyLog);
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
        jMenuItemSelectPl = new JMenuItem("Select partlist", 'p');
        jMenuItemSelectPl.setAccelerator(KeyStroke.getKeyStroke('P', Event.CTRL_MASK));
        jMenuItemSelectPl.setActionCommand("partlist...");
        jMenuItemSelectPl.addActionListener(this);
        jMenu.add(jMenuItemSelectPl);
        jMenuItemSelectParallel = new JMenuItem("Select parallel file", 'a');
        jMenuItemSelectParallel.setAccelerator(KeyStroke.getKeyStroke('A', Event.CTRL_MASK));
        jMenuItemSelectParallel.setActionCommand("parallel...");
        jMenuItemSelectParallel.addActionListener(this);
        jMenu.add(jMenuItemSelectParallel);
        jMenuItemSelectLog = new JMenuItem("Select genrad log", 'l');
        jMenuItemSelectLog.setAccelerator(KeyStroke.getKeyStroke('L', Event.CTRL_MASK));
        jMenuItemSelectLog.setActionCommand("log...");
        jMenuItemSelectLog.addActionListener(this);
        jMenu.add(jMenuItemSelectLog);
        jMenu.addSeparator();
        JMenuItem jMenuItem = new JMenuItem("Quit", 'q');
        jMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', Event.CTRL_MASK));
        jMenuItem.setActionCommand("quit");
        jMenuItem.addActionListener(this);
        jMenu.add(jMenuItem);
        return jMenu;
    }

    private JMenu buildReadMenu() {
        JMenu jMenu = new JMenu("Read");
        jMenu.setMnemonic('r');
        jMenuItemReadPl = new JMenuItem("Read partlist", 'p');
        jMenuItemReadPl.setEnabled(false);
        jMenuItemReadPl.setAccelerator(KeyStroke.getKeyStroke('P', Event.CTRL_MASK | Event.ALT_MASK));
        jMenuItemReadPl.setActionCommand("readPartList");
        jMenuItemReadPl.addActionListener(this);
        jMenu.add(jMenuItemReadPl);
        jMenuItemReadParallel = new JMenuItem("Read parallel file", 'a');
        jMenuItemReadParallel.setEnabled(false);
        jMenuItemReadParallel.setAccelerator(KeyStroke.getKeyStroke('A', Event.CTRL_MASK | Event.ALT_MASK));
        jMenuItemReadParallel.setActionCommand("readParallel");
        jMenuItemReadParallel.addActionListener(this);
        jMenu.add(jMenuItemReadParallel);
        jMenuItemReadLog = new JMenuItem("Read Genrad Log", 'l');
        jMenuItemReadLog.setEnabled(false);
        jMenuItemReadLog.setAccelerator(KeyStroke.getKeyStroke('L', Event.CTRL_MASK | Event.ALT_MASK));
        jMenuItemReadLog.setActionCommand("readLogFile");
        jMenuItemReadLog.addActionListener(this);
        jMenu.add(jMenuItemReadLog);
        return jMenu;
    }

    private JMenu buildPrintMenu() {
        JMenu jMenu = new JMenu("Print");
        jMenu.setMnemonic('p');
        jMenuItemTpg = new JMenuItem("Print new TPG values", 'g');
        jMenuItemTpg.setEnabled(false);
        jMenuItemTpg.setAccelerator(KeyStroke.getKeyStroke('G', Event.CTRL_MASK));
        jMenuItemTpg.setActionCommand("printNewTpgValues");
        jMenuItemTpg.addActionListener(this);
        jMenu.add(jMenuItemTpg);
        jMenuItemTar = new JMenuItem("Print TAR", 't');
        jMenuItemTar.setEnabled(false);
        jMenuItemTar.setAccelerator(KeyStroke.getKeyStroke('T', Event.CTRL_MASK));
        jMenuItemTar.setActionCommand("printTar");
        jMenuItemTar.addActionListener(this);
        jMenu.add(jMenuItemTar);
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
        jToolButtonSelectPl = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/select_pl.gif")));
        jToolButtonSelectPl.setActionCommand("partlist...");
        jToolButtonSelectPl.addActionListener(this);
        jToolButtonSelectPl.setBackground(COLOR_NEEDFUL);
        toolBar.add(jToolButtonSelectPl);
        jToolButtonSelectParallel = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/select_parallel.gif")));
        jToolButtonSelectParallel.setActionCommand("parallel...");
        jToolButtonSelectParallel.addActionListener(this);
        jToolButtonSelectParallel.setBackground(COLOR_NEEDFUL);
        toolBar.add(jToolButtonSelectParallel);
        jToolButtonSelectLog = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/select_log.gif")));
        jToolButtonSelectLog.setActionCommand("log...");
        jToolButtonSelectLog.addActionListener(this);
        jToolButtonSelectLog.setBackground(COLOR_NEEDFUL);
        toolBar.add(jToolButtonSelectLog);
        toolBar.addSeparator();
        jToolButtonReadPl = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/read_pl.gif")));
        jToolButtonReadPl.setEnabled(false);
        jToolButtonReadPl.setActionCommand("readPartList");
        jToolButtonReadPl.addActionListener(this);
        toolBar.add(jToolButtonReadPl);
        jToolButtonReadParallel = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/read_parallel.gif")));
        jToolButtonReadParallel.setEnabled(false);
        jToolButtonReadParallel.setActionCommand("readParallel");
        jToolButtonReadParallel.addActionListener(this);
        toolBar.add(jToolButtonReadParallel);
        jToolButtonReadLog = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/read_log.gif")));
        jToolButtonReadLog.setEnabled(false);
        jToolButtonReadLog.setActionCommand("readLogFile");
        jToolButtonReadLog.addActionListener(this);
        toolBar.add(jToolButtonReadLog);
        toolBar.addSeparator();
        jToolButtonTpg = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/write_values.gif")));
        jToolButtonTpg.setEnabled(false);
        jToolButtonTpg.setActionCommand("printNewTpgValues");
        jToolButtonTpg.addActionListener(this);
        toolBar.add(jToolButtonTpg);
        jToolButtonTar = new JButton(new ImageIcon(ClassLoader.getSystemResource("images/write_tar.gif")));
        jToolButtonTar.setEnabled(false);
        jToolButtonTar.setActionCommand("printTar");
        jToolButtonTar.addActionListener(this);
        toolBar.add(jToolButtonTar);
        return toolBar;
    }
}
