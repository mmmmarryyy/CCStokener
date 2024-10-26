package genreman;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.*;
import javax.swing.event.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 *
 * @author Daniel Dreibrodt
 */
public class Main extends JFrame implements ActionListener, ListSelectionListener, WindowListener {

    /** The version UID, needed as it extends a serializable class */
    public static final long serialVersionUID = 0;

    /** The version name */
    public final String VERSION = "1.3.2";

    /** The variable which will hold the update URL */
    private String updateURL_s = "";

    /** The opened genre property list file into which changes will be saved */
    protected File iTunesGenreIndex;

    private JList genreList;

    private JTextField matchWord_tf;

    private JTextField resourceFile_tf;

    private JButton resourceFile_btn, add_genre, save_genre, remove_genre;

    private JPanel display_p;

    private JComboBox kind_cb;

    private JMenuBar menu;

    private JMenu file_menu, help_menu;

    private JMenuItem file_import, file_export, file_save, file_quit, help_website, help_about;

    private DefaultListModel listModel = new DefaultListModel();

    private ListRenderer listRenderer = new ListRenderer();

    /** The currently for selected genre */
    protected Genre selectedGenre;

    private JPanel jtb;

    private JButton up_btn, down_btn, save_btn, add_btn, remove_btn;

    /** Tells whether changes were saved */
    public boolean saved = true;

    /** genre kind strings as used in the property list */
    public String[] kinds;

    /** translated genre kind strings to show in the GUI */
    public String[] kinds_n;

    private ListTransferHandler ltf;

    private ImageTransferHandler itf;

    private FrameUpdater fu;

    /**
   * Creates the GUI and loads the default genre property list of iTunes
   */
    public Main() {
        if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        setTitle("iTunes Genre Art Manager " + VERSION);
        setIconImage(getImage("img/icon.png"));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        Language.autoLoad();
        ltf = new ListTransferHandler(this);
        itf = new ImageTransferHandler(this);
        kinds = new String[3];
        kinds_n = new String[3];
        kinds[0] = "music";
        kinds_n[0] = Language.get("KIND_MUSIC");
        kinds[1] = "tvshow";
        kinds_n[1] = Language.get("KIND_TVSHOW");
        kinds[2] = "movie";
        kinds_n[2] = Language.get("KIND_MOVIE");
        jtb = new JPanel();
        jtb.setLayout(new BoxLayout(jtb, BoxLayout.LINE_AXIS));
        up_btn = new ToolbarButton(getImage("img/go-up.png"));
        up_btn.addActionListener(this);
        up_btn.setToolTipText(Language.get("TOOLBAR_UP_TIP"));
        jtb.add(up_btn);
        down_btn = new ToolbarButton(getImage("img/go-down.png"));
        down_btn.addActionListener(this);
        down_btn.setToolTipText(Language.get("TOOLBAR_DOWN_TIP"));
        jtb.add(down_btn);
        JToolBar.Separator sep2 = new JToolBar.Separator(new Dimension(10, 22));
        sep2.setOrientation(JSeparator.VERTICAL);
        jtb.add(sep2);
        add_btn = new ToolbarButton(getImage("img/list-add.png"));
        add_btn.addActionListener(this);
        add_btn.setToolTipText(Language.get("TOOLBAR_ADD_TIP"));
        jtb.add(add_btn);
        remove_btn = new ToolbarButton(getImage("img/list-remove.png"));
        remove_btn.addActionListener(this);
        remove_btn.setToolTipText(Language.get("TOOLBAR_REMOVE_TIP"));
        jtb.add(remove_btn);
        add(jtb);
        int mask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        menu = new JMenuBar();
        file_menu = new JMenu(Language.get("MENU_FILE"));
        help_menu = new JMenu(Language.get("MENU_HELP"));
        menu.add(file_menu);
        menu.add(help_menu);
        file_import = new JMenuItem(Language.get("MENU_FILE_IMPORT"));
        file_import.addActionListener(this);
        file_import.setAccelerator(KeyStroke.getKeyStroke('O', mask));
        file_menu.add(file_import);
        file_export = new JMenuItem(Language.get("MENU_FILE_EXPORT"));
        file_export.addActionListener(this);
        file_export.setAccelerator(KeyStroke.getKeyStroke('X', mask));
        file_menu.add(file_export);
        if (System.getProperty("os.name").indexOf("Mac") == -1) {
            file_quit = new JMenuItem(Language.get("MENU_FILE_QUIT"));
            file_quit.addActionListener(this);
            file_quit.setAccelerator(KeyStroke.getKeyStroke('Q', mask));
            file_menu.add(file_quit);
        }
        help_website = new JMenuItem(Language.get("MENU_HELP_WEBSITE"));
        help_website.addActionListener(this);
        help_menu.add(help_website);
        help_about = new JMenuItem(Language.get("MENU_HELP_ABOUT"));
        help_about.addActionListener(this);
        help_menu.add(help_about);
        setJMenuBar(menu);
        SpringLayout layout = new SpringLayout();
        setLayout(layout);
        genreList = new JList();
        genreList.setBackground(Color.decode("#d1d7e2"));
        genreList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        genreList.setModel(listModel);
        genreList.setCellRenderer(listRenderer);
        genreList.setDragEnabled(true);
        genreList.setDropMode(DropMode.INSERT);
        genreList.setTransferHandler(ltf);
        genreList.addListSelectionListener(this);
        JScrollPane jsp = new JScrollPane(genreList);
        if (System.getProperty("os.name").indexOf("Mac") != -1) {
            jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
        add(jsp);
        JPanel genre_panel = new JPanel();
        JLabel matchWord_l = new JLabel(Language.get("FORM_MATCHWORD"));
        matchWord_tf = new JTextField();
        JLabel resourceFile_l = new JLabel(Language.get("FORM_RESOURCEFILE"));
        resourceFile_tf = new JTextField();
        resourceFile_btn = new JButton(Language.get("FORM_RESOURCEFILE_BROWSE"));
        resourceFile_btn.setTransferHandler(itf);
        resourceFile_btn.addActionListener(this);
        display_p = new JPanel() {

            public static final long serialVersionUID = 0;

            @Override
            public void paint(Graphics g) {
                drawSelectedGenreImage(g);
            }

            @Override
            public void update(Graphics g) {
                drawSelectedGenreImage(g);
            }
        };
        display_p.setPreferredSize(new Dimension(256, 256));
        display_p.setMaximumSize(new Dimension(256, 256));
        display_p.setMinimumSize(new Dimension(256, 256));
        display_p.setSize(new Dimension(256, 256));
        display_p.setTransferHandler(itf);
        fu = new FrameUpdater(display_p, 5);
        fu.start();
        JLabel kind_l = new JLabel(Language.get("FORM_KIND"));
        kind_cb = new JComboBox(kinds_n);
        JPanel genre_btns_p = new JPanel(new FlowLayout());
        save_genre = new JButton(Language.get("FORM_SAVE"));
        save_genre.addActionListener(this);
        genre_btns_p.add(save_genre);
        Component[] labels = { matchWord_l, resourceFile_l, kind_l };
        int tf_maxwd = maxWidth(labels);
        matchWord_l.setPreferredSize(new Dimension(tf_maxwd, matchWord_l.getPreferredSize().height));
        resourceFile_l.setPreferredSize(new Dimension(tf_maxwd, resourceFile_l.getPreferredSize().height));
        kind_l.setPreferredSize(new Dimension(tf_maxwd, kind_l.getPreferredSize().height));
        int tf_wd = 200;
        matchWord_tf.setPreferredSize(new Dimension(tf_wd, matchWord_tf.getPreferredSize().height));
        resourceFile_tf.setPreferredSize(new Dimension(tf_wd, resourceFile_tf.getPreferredSize().height));
        kind_cb.setPreferredSize(new Dimension(tf_wd, kind_cb.getPreferredSize().height));
        genre_panel.setLayout(new BoxLayout(genre_panel, BoxLayout.PAGE_AXIS));
        JPanel matchWord_p = new JPanel();
        matchWord_p.setLayout(new BoxLayout(matchWord_p, BoxLayout.LINE_AXIS));
        matchWord_p.add(matchWord_l);
        matchWord_p.add(Box.createHorizontalStrut(10));
        matchWord_p.add(matchWord_tf);
        matchWord_p.add(Box.createHorizontalStrut(10));
        genre_panel.add(matchWord_p);
        genre_panel.add(Box.createVerticalStrut(10));
        JPanel resourceFile_p = new JPanel();
        resourceFile_p.setLayout(new BoxLayout(resourceFile_p, BoxLayout.LINE_AXIS));
        resourceFile_p.add(resourceFile_l);
        resourceFile_p.add(Box.createHorizontalStrut(10));
        resourceFile_p.add(resourceFile_tf);
        resourceFile_p.add(Box.createHorizontalStrut(10));
        resourceFile_p.add(resourceFile_btn);
        resourceFile_p.add(Box.createHorizontalStrut(10));
        genre_panel.add(resourceFile_p);
        genre_panel.add(Box.createVerticalStrut(10));
        genre_panel.add(display_p);
        genre_panel.add(Box.createVerticalStrut(10));
        JPanel kind_p = new JPanel();
        kind_p.setLayout(new BoxLayout(kind_p, BoxLayout.LINE_AXIS));
        kind_p.add(kind_l);
        kind_p.add(Box.createHorizontalStrut(10));
        kind_p.add(kind_cb);
        kind_p.add(Box.createHorizontalStrut(10));
        genre_panel.add(kind_p);
        genre_panel.add(Box.createVerticalStrut(10));
        genre_panel.add(genre_btns_p);
        genre_panel.setMaximumSize(genre_panel.getPreferredSize());
        add(genre_panel);
        layout.putConstraint(SpringLayout.NORTH, jtb, 0, SpringLayout.NORTH, getContentPane());
        layout.putConstraint(SpringLayout.WEST, jtb, 0, SpringLayout.WEST, getContentPane());
        layout.putConstraint(SpringLayout.NORTH, jsp, 0, SpringLayout.SOUTH, jtb);
        layout.putConstraint(SpringLayout.WEST, jsp, 0, SpringLayout.WEST, getContentPane());
        layout.putConstraint(SpringLayout.SOUTH, getContentPane(), 0, SpringLayout.SOUTH, jsp);
        layout.putConstraint(SpringLayout.NORTH, genre_panel, 0, SpringLayout.SOUTH, jtb);
        layout.putConstraint(SpringLayout.EAST, getContentPane(), 0, SpringLayout.EAST, genre_panel);
        layout.putConstraint(SpringLayout.WEST, genre_panel, 5, SpringLayout.EAST, jsp);
        pack();
        int height = genre_panel.getHeight() + 80;
        if (jsp.getHeight() > height) height = jsp.getHeight();
        setMinimumSize(new Dimension(getWidth(), height));
        setSize(getWidth(), height);
        setVisible(true);
        try {
            updateURL_s = "http://genreman.sourceforge.net/update.php?v=" + URLEncoder.encode(VERSION, "UTF-8") + "&os=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8");
            update();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        openITunesGenreFile();
        genreList.updateUI();
    }

    /**
   * Finds and parses the default iTunes genre property list
   */
    private void openITunesGenreFile() {
        File f = null;
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            f = new File(System.getenv("PROGRAMFILES") + "\\iTunes\\iTunes.Resources\\genres.plist");
            if (!f.exists()) {
                try {
                    Process p = Runtime.getRuntime().exec("reg query \"HKLM\\SOFTWARE\\Apple Computer, Inc.\\iTunes\" /v ProgramFolder");
                    InputStreamReader isr = new InputStreamReader(p.getInputStream());
                    BufferedReader br = new BufferedReader(isr);
                    while (true) {
                        String l = br.readLine();
                        if (l == null) break;
                        if (l.indexOf("ProgramFolder") != -1) {
                            int i = l.indexOf("REG_SZ");
                            String programFolder = l.substring(i + 7);
                            f = new File(System.getenv("PROGRAMFILES") + "\\" + programFolder + "iTunes.Resources\\genres.plist");
                            System.out.print(f.toString());
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (System.getProperty("os.name").indexOf("Mac") != -1) {
            f = new File("/Applications/iTunes.app/Contents/Resources/genres.plist");
        }
        if (!(f != null && f.exists())) {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_NOTFOUND_MSG"), Language.get("ERROR_NOTFOUND_TITLE"), JOptionPane.ERROR_MESSAGE);
            openFile();
        } else {
            iTunesGenreIndex = f;
            parseGenreFile(f);
        }
    }

    /**
   * Parses the genre list from a genre property list file
   * @param f The property list file
   */
    private void parseGenreFile(File f) {
        listModel.clear();
        matchWord_tf.setText("");
        resourceFile_tf.setText("");
        kind_cb.setSelectedIndex(0);
        selectedGenre = null;
        display_p.repaint();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(f);
            if (!doc.getDoctype().getName().toLowerCase().equals("plist")) throw new Exception("Selected file is not a valid genre list! It is of type: " + doc.getDoctype().getName());
            NodeList nodes = doc.getElementsByTagName("array");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                NodeList dicts = e.getElementsByTagName("dict");
                for (int j = 0; j < dicts.getLength(); j++) {
                    parseDict((Element) dicts.item(j));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, Language.get("ERROR_PARSE_MSG"), Language.get("ERROR_PARSE_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
        saved = true;
        if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
        repaint();
    }

    /**
   * Parses a dictionary element in the genre property list
   * @param e
   * @throws java.lang.Exception
   */
    private void parseDict(Element e) throws Exception {
        Genre g = new Genre();
        g.setKind(kinds[0]);
        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeName().toLowerCase().equals("key")) {
                NodeList c = n.getChildNodes();
                String type = c.item(0).getNodeValue();
                if (type.toLowerCase().equals("matchstring")) {
                    boolean valueFound = false;
                    while (!valueFound) {
                        Node node = nodes.item(++i);
                        if (node.getNodeName().toLowerCase().equals("string")) {
                            String value = node.getChildNodes().item(0).getNodeValue();
                            g.setMatchString(value);
                            valueFound = true;
                        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                            throw new Exception("Improper List! Element existing between KEY and STRING: " + node.getNodeName());
                        }
                    }
                } else if (type.toLowerCase().equals("resourcefile")) {
                    boolean valueFound = false;
                    while (!valueFound) {
                        Node node = nodes.item(++i);
                        if (node.getNodeName().toLowerCase().equals("string")) {
                            String value = node.getChildNodes().item(0).getNodeValue();
                            g.setResourceFile(value);
                            valueFound = true;
                        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                            throw new Exception("Improper List! Element existing between KEY and STRING: " + node.getNodeName());
                        }
                    }
                } else if (type.toLowerCase().equals("kind")) {
                    boolean valueFound = false;
                    while (!valueFound) {
                        Node node = nodes.item(++i);
                        if (node.getNodeName().toLowerCase().equals("string")) {
                            String value = node.getChildNodes().item(0).getNodeValue();
                            g.setKind(value);
                            valueFound = true;
                        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                            throw new Exception("Improper List! Element existing between KEY and STRING: " + node.getNodeName());
                        }
                    }
                } else {
                }
            } else {
            }
        }
        if (!existsAlready(g, g.getMatchString(), g.getKind())) listModel.addElement(g);
    }

    /**
  * Opens a genre property list for editing, changes made will be saved directly into it
  */
    private void openFile() {
        JFileChooser fc = new JFileChooser();
        if (System.getProperty("os.name").indexOf("Windows") != -1) fc.setCurrentDirectory(new File(System.getenv("PROGRAMFILES") + "\\iTunes\\iTunes.Resources\\")); else fc.setCurrentDirectory(new File("/Applications/iTunes.app/Contents/Resources/"));
        fc.setFileFilter(new CustomFileFilter(".*\\.PLIST", Language.get("OPEN_FILETYPE_DESC")));
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            iTunesGenreIndex = fc.getSelectedFile();
            parseGenreFile(fc.getSelectedFile());
        }
    }

    /**
   * Overwrites the currently loaded genre list by one that the user can choose in a dialog that is opened.
   */
    private void importFile() {
        JFileChooser fc = new JFileChooser();
        if (System.getProperty("os.name").indexOf("Windows") != -1) fc.setCurrentDirectory(new File(System.getenv("PROGRAMFILES") + "\\iTunes\\iTunes.Resources\\")); else fc.setCurrentDirectory(new File("/Applications/iTunes.app/Contents/Resources/"));
        fc.setFileFilter(new CustomFileFilter(".*\\.PLIST", Language.get("OPEN_FILETYPE_DESC")));
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            parseGenreFile(fc.getSelectedFile());
        }
    }

    /**
   * Shows a dialog in which the user chooses where to export the loaded genre list to.
   * Thereafter it is saved there.
   */
    private void exportFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new CustomFileFilter(".*\\.plist", Language.get("OPEN_FILETYPE_DESC")));
        int i = fc.showSaveDialog(this);
        if (i == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fc.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".plist")) f = new File(f.getParentFile(), f.getName() + ".plist");
                saveFile(f);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, Language.get("ERROR_SAVE_MSG"), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
   * Saves the currently opened genre list file
   * @throws java.io.IOException
   */
    protected void saveFile() throws IOException {
        saveFile(iTunesGenreIndex);
        saved = true;
        if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
    }

    /**
   * Saves the loaded genre list into a given file
   * @param f The target file
   * @throws java.io.IOException When an I/O error occurs
   */
    private void saveFile(File f) throws IOException {
        String br = System.getProperty("line.separator");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        xml += br + "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
        xml += br + "<plist version=\"1.0\">";
        xml += br + "  <dict>";
        xml += br + "    <key>entries</key>";
        xml += br + "    <array>";
        for (int i = 0; i < listModel.size(); i++) {
            try {
                Genre g = (Genre) listModel.get(i);
                xml += br + g.getXMLCode(br);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
        xml += br + "    </array>";
        xml += br + "  </dict>";
        xml += br + "</plist>";
        FileOutputStream fis = new FileOutputStream(f);
        OutputStreamWriter osw = new OutputStreamWriter(fis, "UTF-8");
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(xml);
        bw.close();
        osw.close();
        fis.close();
    }

    /**
   * Displays the genre image of the selected genre
   * @param g
   */
    private void drawSelectedGenreImage(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, 256, 256);
        if (selectedGenre == null || resourceFile_tf.getText().equals("")) return;
        Image img = Toolkit.getDefaultToolkit().getImage(iTunesGenreIndex.getParent() + File.separator + resourceFile_tf.getText());
        double w = img.getWidth(this);
        double h = img.getHeight(this);
        if (w > 256 || h > 256) {
            double ar = w / h;
            double nh, nw;
            if (w > h) {
                nw = 256;
                nh = 256 / ar;
            } else {
                nh = 256;
                nw = 256 * ar;
            }
            g.drawImage(img, 0, 0, (int) nw, (int) nh, this);
        } else {
            g.drawImage(img, 0, 0, this);
        }
    }

    /**
   * Saves changes to the previously selected genre and loads the newly selected one into the editor
   * @param e The ListSelectionEvent
   */
    public void valueChanged(ListSelectionEvent e) {
        if (selectedGenre != null && selectedGenre.equals(genreList.getSelectedValue())) return;
        if (selectedGenre != null) {
            if (!saveGenre()) {
                genreList.setSelectedValue(selectedGenre, true);
                return;
            }
        }
        selectedGenre = (Genre) genreList.getSelectedValue();
        if (selectedGenre != null) {
            matchWord_tf.setText(selectedGenre.getMatchString());
            resourceFile_tf.setText(selectedGenre.getResourceFile());
            kind_cb.setSelectedIndex(getKindIndex(selectedGenre.getKind()));
            drawSelectedGenreImage(display_p.getGraphics());
        } else {
            matchWord_tf.setText("");
            resourceFile_tf.setText("");
            kind_cb.setSelectedIndex(0);
            display_p.getGraphics().clearRect(0, 0, 256, 256);
        }
    }

    /**
   * Gets the index of the genre kind in the genre kind array
   * @param k The name of the genre kind, e.g. Music
   * @return The index of the genre kind in the genre kind array
   */
    private int getKindIndex(String k) {
        for (int i = 0; i < kinds.length; i++) if (kinds[i].equals(k)) return i;
        return -1;
    }

    /**
   * Saves changes to the currently selected genre and therafter the modified genre list
   * @return Success
   */
    protected boolean saveGenre() {
        if (iTunesGenreIndex == null) {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTOPEN_MSG"), Language.get("ERROR_FIRSTOPEN_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        if (selectedGenre != null) {
            if (existsAlready(selectedGenre, matchWord_tf.getText(), kinds[kind_cb.getSelectedIndex()])) {
                JOptionPane.showMessageDialog(this, Language.get("ERROR_GEXISTS_MSG"), Language.get("ERROR_GEXISTS_TITLE"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            selectedGenre.setMatchString(matchWord_tf.getText().replaceAll("&", " "));
            selectedGenre.setResourceFile(resourceFile_tf.getText());
            selectedGenre.setKind(kinds[kind_cb.getSelectedIndex()]);
            genreList.repaint();
        } else {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTSELECT_MSG"), Language.get("ERROR_FIRSTSELECT_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        saved = false;
        try {
            saveFile();
            saved = true;
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);
            return false;
        }
    }

    private void addGenre() {
        if (iTunesGenreIndex == null) {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTOPEN_MSG"), Language.get("ERROR_FIRSTOPEN_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (selectedGenre != null) saveGenre();
        Genre g = new Genre();
        g.setMatchString(Language.get("NEW_GENRE"));
        g.setResourceFile("genre-dance.jpg");
        g.setKind(kinds[0]);
        if (existsAlready(g, Language.get("NEW_GENRE"), kinds[0])) return;
        listModel.ensureCapacity(listModel.getSize() + 1);
        listModel.addElement(g);
        genreList.updateUI();
        genreList.setSelectedValue(g, true);
        saved = false;
        if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
    }

    private boolean existsAlready(Genre x, String match, String kind) {
        if (x == null) return false;
        for (int i = 0; i < listModel.getSize(); i++) {
            Genre g = (Genre) listModel.getElementAt(i);
            if (g != x && g.getMatchString().toLowerCase().equals(match.toLowerCase()) && g.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }

    private void moveUp() {
        Object s = selectedGenre;
        int i = listModel.indexOf(s);
        if (i <= 0) return;
        listModel.set(i, listModel.set(i - 1, s));
        genreList.updateUI();
        genreList.setSelectedValue(s, true);
        try {
            saveFile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_SAVE_MSG"), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            saved = false;
            if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
        }
    }

    private void moveDown() {
        Object s = selectedGenre;
        int i = listModel.indexOf(s);
        if (i == -1 || i == listModel.size() - 1) return;
        listModel.set(i, listModel.set(i + 1, s));
        genreList.updateUI();
        genreList.setSelectedValue(s, true);
        try {
            saveFile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Language.get("ERROR_SAVE_MSG"), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            saved = false;
            if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
        }
    }

    private boolean openImage() {
        File f = null;
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(iTunesGenreIndex);
        fc.setFileFilter(new CustomFileFilter(".*\\.(JPG|JPEG)", Language.get("OPEN_JPEG_DESC")));
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            f = fc.getSelectedFile();
        }
        return loadImage(f);
    }

    public boolean loadImage(File f) {
        if (f != null) {
            if (!(f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg"))) return false;
            if (!f.getAbsolutePath().startsWith(iTunesGenreIndex.getParent())) {
                File c = new File(iTunesGenreIndex.getParent(), f.getName());
                String name = c.getName().toLowerCase().replaceAll(".jpg", "").replaceAll(".jpeg", "");
                int i = 2;
                while (c.exists()) {
                    c = new File(iTunesGenreIndex.getParent(), name + "-" + String.valueOf(i) + ".jpg");
                    i++;
                }
                try {
                    Main.copyFile(f, c);
                    f = c;
                    resourceFile_tf.setText(f.getAbsolutePath().replace(iTunesGenreIndex.getParent() + File.separator, ""));
                    display_p.repaint();
                    saved = false;
                    if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);
                    return true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return false;
                }
            } else {
                resourceFile_tf.setText(f.getAbsolutePath().replace(iTunesGenreIndex.getParent() + File.separator, ""));
                display_p.repaint();
                saved = false;
                if (System.getProperty("os.name").indexOf("Mac") != -1) getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);
                return true;
            }
        } else {
            return false;
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if (s.equals(file_import)) {
            importFile();
        } else if (s.equals(file_save) || s.equals(save_btn)) {
            try {
                saveFile();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, Language.get("ERROR_SAVE_MSG"), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            }
        } else if (s.equals(file_export)) {
            exportFile();
        } else if (s.equals(file_quit)) {
            quit();
        } else if (s.equals(save_genre)) {
            saveGenre();
        } else if (s.equals(add_genre) || s.equals(add_btn)) {
            addGenre();
        } else if (s.equals(remove_genre) || s.equals(remove_btn)) {
            if (iTunesGenreIndex == null) {
                JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTOPEN_MSG"), Language.get("ERROR_FIRSTOPEN_TITLE"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (selectedGenre == null) {
                JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTSELECT_MSG"), Language.get("ERROR_FIRSTSELECT_TITLE"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            matchWord_tf.setText("");
            resourceFile_tf.setText("");
            kind_cb.setSelectedIndex(0);
            listModel.removeElement(selectedGenre);
            selectedGenre = null;
            genreList.repaint();
        } else if (s.equals(resourceFile_btn)) {
            if (iTunesGenreIndex == null) {
                JOptionPane.showMessageDialog(this, Language.get("ERROR_FIRSTOPEN_MSG"), Language.get("ERROR_FIRSTOPEN_TITLE"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            openImage();
        } else if (s.equals(help_website)) {
            browse("http://genreman.sourceforge.net");
        } else if (s.equals(help_about)) {
            JOptionPane.showMessageDialog(this, Language.get("ABOUT_MSG").replaceAll("%v", VERSION), Language.get("ABOUT_TITLE"), JOptionPane.INFORMATION_MESSAGE);
        } else if (s.equals(up_btn)) moveUp(); else if (s.equals(down_btn)) moveDown();
    }

    /**
   * Sorts the genre list alphabetically
   */
    private void sort() {
        if (listModel.size() < 2) return;
        Genre g = (Genre) genreList.getSelectedValue();
        sortPartition(0, listModel.size() - 1);
        genreList.setSelectedValue(g, true);
    }

    private void sortPartition(int left, int right) {
        String x = listModel.get((left + right) / 2).toString();
        int l = left;
        int r = right;
        do {
            while (listModel.get(l).toString().compareToIgnoreCase(x) < 0) {
                l++;
            }
            while (listModel.get(r).toString().compareToIgnoreCase(x) > 0) {
                r--;
            }
            if (l <= r) {
                listModel.set(r, listModel.set(l, listModel.get(r)));
                l++;
                r--;
            }
        } while (l < r);
        if (left < r) sortPartition(left, r);
        if (l < right) sortPartition(l, right);
    }

    /**
   * Checks for updates and downloads and installs them if chosen by user
   */
    private void update() {
        if (VERSION.contains("dev")) return;
        System.out.println(updateURL_s);
        try {
            URL updateURL = new URL(updateURL_s);
            InputStream uis = updateURL.openStream();
            InputStreamReader uisr = new InputStreamReader(uis);
            BufferedReader ubr = new BufferedReader(uisr);
            String header = ubr.readLine();
            if (header.equals("GENREMANUPDATEPAGE")) {
                String cver = ubr.readLine();
                String cdl = ubr.readLine();
                if (!cver.equals(VERSION)) {
                    System.out.println("Update available!");
                    int i = JOptionPane.showConfirmDialog(this, Language.get("UPDATE_AVAILABLE_MSG").replaceAll("%o", VERSION).replaceAll("%c", cver), Language.get("UPDATE_AVAILABLE_TITLE"), JOptionPane.YES_NO_OPTION);
                    if (i == 0) {
                        URL url = new URL(cdl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        if (connection.getResponseCode() / 100 != 2) {
                            throw new Exception("Server error! Response code: " + connection.getResponseCode());
                        }
                        int contentLength = connection.getContentLength();
                        if (contentLength < 1) {
                            throw new Exception("Invalid content length!");
                        }
                        int size = contentLength;
                        File tempfile = File.createTempFile("genreman_update", ".zip");
                        tempfile.deleteOnExit();
                        RandomAccessFile file = new RandomAccessFile(tempfile, "rw");
                        InputStream stream = connection.getInputStream();
                        int downloaded = 0;
                        ProgressWindow pwin = new ProgressWindow(this, "Downloading");
                        pwin.setVisible(true);
                        pwin.setProgress(0);
                        pwin.setText("Connecting...");
                        while (downloaded < size) {
                            byte buffer[];
                            if (size - downloaded > 1024) {
                                buffer = new byte[1024];
                            } else {
                                buffer = new byte[size - downloaded];
                            }
                            int read = stream.read(buffer);
                            if (read == -1) break;
                            file.write(buffer, 0, read);
                            downloaded += read;
                            pwin.setProgress(downloaded / size);
                        }
                        file.close();
                        System.out.println("Downloaded file to " + tempfile.getAbsolutePath());
                        pwin.setVisible(false);
                        pwin.dispose();
                        pwin = null;
                        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempfile));
                        ZipEntry entry;
                        while ((entry = zin.getNextEntry()) != null) {
                            File outf = new File(entry.getName());
                            System.out.println(outf.getAbsoluteFile());
                            if (outf.exists()) outf.delete();
                            OutputStream out = new FileOutputStream(outf);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = zin.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.close();
                        }
                        JOptionPane.showMessageDialog(this, Language.get("UPDATE_SUCCESS_MSG"), Language.get("UPDATE_SUCCESS_TITLE"), JOptionPane.INFORMATION_MESSAGE);
                        setVisible(false);
                        if (System.getProperty("os.name").indexOf("Windows") != -1) {
                            Runtime.getRuntime().exec("iTunesGenreArtManager.exe");
                        } else {
                            Runtime.getRuntime().exec("java -jar \"iTunes Genre Art Manager.app/Contents/Resources/Java/iTunes_Genre_Art_Manager.jar\"");
                        }
                        System.exit(0);
                    } else {
                    }
                }
                ubr.close();
                uisr.close();
                uis.close();
            } else {
                while (ubr.ready()) {
                    System.out.println(ubr.readLine());
                }
                ubr.close();
                uisr.close();
                uis.close();
                throw new Exception("Update page had invalid header: " + header);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Language.get("UPDATE_ERROR_MSG"), Language.get("UPDATE_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public void quit() {
        if (!saved) {
            Object[] options = { Language.get("QUIT_YES"), Language.get("QUIT_NO"), Language.get("QUIT_CANCEL") };
            int n = JOptionPane.showOptionDialog(this, Language.get("QUIT_MSG"), Language.get("QUIT_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (n == 0) {
                try {
                    saveFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, Language.get("ERROR_SAVE_MSG"), Language.get("ERROR_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.exit(0);
            } else if (n == 1) {
                System.exit(0);
            } else {
                return;
            }
        } else {
            System.exit(0);
        }
    }

    public static Image getImage(String name) {
        try {
            Image i = Toolkit.getDefaultToolkit().createImage(Main.class.getResource(name));
            return i;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    private static void postUpdate() {
        if (!new File(".updated").exists()) return;
        if (System.getProperty("os.name").indexOf("Mac") != -1) {
        } else {
        }
        new File(".updated").delete();
    }

    /**
   * Gets the greatest width of the given components
   * @param compos The components that should be checked
   * @return The maximum width
   */
    public static int maxWidth(Component[] compos) {
        int w = 0;
        for (Component c : compos) {
            int cw = c.getPreferredSize().width;
            if (cw > w) w = cw;
        }
        return w;
    }

    /**
   * @param args the command line arguments
   */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "iTunes Genre Art Manager");
        postUpdate();
        new Main();
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        quit();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    /**
   * Taken from http://www.rgagnon.com/javadetails/java-0064.html
   * @param in Source File
   * @param out Destination File
   * @throws java.io.IOException
   */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static void browse(String url) {
        Desktop desktop = null;
        if (Desktop.isDesktopSupported()) desktop = Desktop.getDesktop();
        if (desktop != null) {
            try {
                desktop.browse(new java.net.URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.toString(), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, Language.get("ERROR_BROWSE_MSG").replaceAll("%u", url), Language.get("ERROR_BROWSE_TITLE"), JOptionPane.WARNING_MESSAGE);
        }
    }
}
