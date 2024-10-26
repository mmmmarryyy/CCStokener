package gnu.kinsight.view;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import gov.sandia.postscript.PSGr2;
import org.shetline.io.*;
import gnu.kinsight.*;
import javax.imageio.ImageIO;

/**
 * <code>ExtensionFileFilter</code> is a filter for a file chooser that
 * takes a single extension to accept.
 *
 * @author <a href="mailto:gann@pobox.com">Gann Bierner</a>
 * @version $Revision: 1.11 $
 * @see javax.swing.filechooser.FileFilter
 */
class ExtensionFileFilter extends javax.swing.filechooser.FileFilter {

    String ext;

    String description;

    public ExtensionFileFilter(String s, String desc) {
        ext = s;
        description = desc;
    }

    public boolean accept(File f) {
        int pt = f.getName().lastIndexOf(".");
        return pt != -1 && f.getName().substring(pt).equals("." + ext);
    }

    public String getDescription() {
        return description;
    }
}

/**
 * <code>View</code> is a container for viewing Tree data.  I don't
 * particularly like the way I have all of this layed out and will probably
 * change it in the future.
 *
 * @author <a href="mailto:gann@pobox.com">Gann Bierner</a>
 * @version $Revision: 1.11 $
 * @see JFrame
 */
public abstract class View extends JInternalFrame {

    protected Tree tree;

    boolean layed_out = false;

    int font_size = 12;

    String font_name = "SansSerif";

    Font font = new Font(font_name, Font.PLAIN, font_size);

    protected Color bg_color = Color.white;

    protected Color fg_color = Color.black;

    String current_node_layout;

    protected JLabel status = new JLabel();

    JMenu layout_menu = new JMenu("Load Layout");

    JComboBox size_box;

    JComboBox fonts;

    JButton bg_button = new JButton("B");

    JButton fg_button = new JButton("F");

    /**
     * The directory where this class stores layout information
     */
    public static final String layout_dir = System.getProperties().getProperty("user.home") + "/.kinsight/viewlayout/";

    /**
     * The file name for properties associated with layouts-- font size, name,
     * color, etc.  
     */
    public static final String layout_props = "layouts";

    /**
     * The default layout properties
     */
    static Properties default_layouts = new Properties();

    /**
     * User chosen layout properties
     */
    static Properties layouts;

    /**
     * A dialog where the user can determine how a node is layed out
     */
    protected DataLayoutDialog node_layout_dialog = new DataLayoutDialog(null, "Node Layout");

    /**
     * A template that takes a node and produces text
     */
    protected DataLayoutTemplate node_template;

    /**
     * Where the tree is drawn
     */
    protected JPanel canvas = new JPanel() {

        public void paint(Graphics g) {
            Dimension size = this.getSize();
            g.setColor(bg_color);
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(fg_color);
            display(g);
        }
    };

    /**
     * Registered to any component that would change how the tree would look.  
     */
    ActionListener layout_listener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            String name = ((JMenuItem) e.getSource()).getText();
            current_node_layout = name;
            resetLayout((String) layouts.getProperty(name));
            setFontSize(font_size);
            setFontName(font_name);
            bg_button.setBackground(bg_color);
            fg_button.setForeground(fg_color);
        }
    };

    static {
        default_layouts.setProperty("default", "basic");
        layouts = new Properties(default_layouts);
        (new File(layout_dir)).mkdirs();
        File layouts_file = new File(layout_dir + layout_props);
        try {
            if (!layouts_file.exists()) layouts_file.createNewFile();
            layouts.load(new FileInputStream(layouts_file));
        } catch (IOException E) {
            E.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                try {
                    layouts.store(new FileOutputStream(layout_dir + layout_props), "Locations of saved layouts");
                } catch (IOException E) {
                    E.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates a new <code>View</code> instance.
     *
     */
    public View() {
        super("Title", true, true, true, true);
    }

    /**
     * A node was selected with the first mouse button
     *
     * @param n a <code>Node</code>
     */
    public abstract void nodeSelected(Node n, boolean doubleClick);

    /**
     * A node was selected with the second mouse button
     *
     * @param n a <code>Node</code>
     */
    public abstract void nodeSelected2(Node n);

    public abstract void nodePopup(Component c, int x, int y);

    /**
     * Sets up the items that can be chosen by the user to layout a node
     */
    public abstract void setupNodeLayoutDialog();

    /**
     * Inform the user of information via the status bar
     *
     * @param text a <code>String</code>
     */
    protected void setStatus(String text) {
        status.setText(text);
    }

    /**
     * Return the tree being layed out in this view
     *
     * @return a <code>Tree</code>
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * Set the tree being layed out in this view
     *
     * @param t a <code>Tree</code>
     */
    public void setTree(Tree t) {
        tree = t;
        setSize(600, 300);
        canvas.setDoubleBuffered(true);
        canvas.setBackground(Color.white);
        canvas.setOpaque(true);
        JScrollPane scroller = new JScrollPane(canvas);
        JMenuBar mb = new JMenuBar();
        setJMenuBar(mb);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroller, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        canvas.revalidate();
        canvas.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    nodePopup(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    nodePopup(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getModifiers() == InputEvent.BUTTON2_MASK) nodeSelected2(tree.getNode(e.getX(), e.getY())); else if (e.getModifiers() == InputEvent.BUTTON1_MASK) nodeSelected(tree.getNode(e.getX(), e.getY()), e.getClickCount() == 2);
            }
        });
        setupNodeLayoutDialog();
        current_node_layout = (String) layouts.getProperty("default");
        resetLayout((String) layouts.getProperty(current_node_layout));
        getContentPane().add(getToolBar(mb), BorderLayout.NORTH);
        mb.add(getViewMenu());
    }

    /**
     * Add elements to the edit menu having to do with layaing out aspects of
     * the tree.
     *
     * @param edit a <code>JMenu</code>
     */
    protected void addEditFormats(JMenu edit) {
        JMenuItem mi = new JMenuItem("Node Layout...");
        edit.add(mi);
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                node_template = node_layout_dialog.getTemplate();
                redraw();
            }
        });
    }

    /**
     * Create the edit menu
     *
     * @return a <code>JMenu</code>
     */
    protected JMenu getViewMenu() {
        JMenu view = new JMenu("View");
        addEditFormats(view);
        view.addSeparator();
        JMenuItem save_layout = new JMenuItem("Save Layout Options");
        JMenuItem default_layout = new JMenuItem("Make Default");
        view.add(save_layout);
        view.add(default_layout);
        save_layout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(layout_dir);
                chooser.setFileFilter(new ExtensionFileFilter("vl", "View Layout Files"));
                chooser.showSaveDialog(View.this);
                File file = chooser.getSelectedFile();
                if (file == null) return; else saveLayout(file);
            }
        });
        default_layout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                layouts.setProperty("default", current_node_layout);
            }
        });
        view.add(readLayouts());
        return view;
    }

    /**
     * Change the way nodes are viewed based on a saved version of data layout
     * dialog
     *
     * @param url a <code>String</code> of a URL containing the saved
     * information
     */
    private void resetLayout(String url) {
        try {
            BufferedReader is = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            resetLayout(is);
            is.close();
            redraw();
        } catch (IOException E) {
            E.printStackTrace();
        } catch (ClassNotFoundException E) {
            E.printStackTrace();
        }
    }

    /**
     * Resets a data layout dialog based on saved information
     *
     * @param is a <code>BufferedReader</code> containing information saved by
     * a data layout dialog
     * @exception IOException
     * @exception ClassNotFoundException
     */
    protected void resetLayout(BufferedReader is) throws IOException, ClassNotFoundException {
        bg_color = new Color(Integer.parseInt(is.readLine()));
        fg_color = new Color(Integer.parseInt(is.readLine()));
        font_size = Integer.parseInt(is.readLine());
        font_name = is.readLine();
        node_template = node_layout_dialog.read(is);
    }

    /**
     * Construct a menu containing all saved and built in node layouts
     *
     * @return a <code>JMenu</code>
     */
    private JMenu readLayouts() {
        BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("gnu/kinsight/person_templates/templates.dat")));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String l = ClassLoader.getSystemResource("gnu/kinsight/person_templates/" + line + ".vl").toString();
                layouts.put(line, l);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Enumeration e = layouts.propertyNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            if (!name.equals("default")) {
                JMenuItem mi = new JMenuItem(name);
                mi.addActionListener(layout_listener);
                layout_menu.add(mi);
            }
        }
        return layout_menu;
    }

    /**
     * Save the current layout setup to a file
     *
     * @param file a <code>File</code>
     */
    private void saveLayout(File file) {
        String name = file.getName();
        int sep = name.indexOf(".vl");
        try {
            if (sep == -1) file = new File(file.getCanonicalPath() + ".vl"); else name = name.substring(0, sep);
            layouts.setProperty(name, file.toURL().toString());
            JMenuItem mi = new JMenuItem(name);
            mi.addActionListener(layout_listener);
            layout_menu.add(mi);
        } catch (IOException E) {
            E.printStackTrace();
        }
        try {
            PrintStream os = new PrintStream(new FileOutputStream(file));
            saveLayout(os);
            os.flush();
            os.close();
        } catch (IOException E) {
            E.printStackTrace();
        }
    }

    /**
     * Save the current layout setup to a print stream
     *
     * @param os a <code>PrintStream</code>
     * @exception IOException
     */
    protected void saveLayout(PrintStream os) throws IOException {
        os.println(bg_color.getRGB());
        os.println(fg_color.getRGB());
        os.println(font_size);
        os.println(font_name);
        node_layout_dialog.write(os);
    }

    /**
     * change the font size
     *
     * @param s an <code>int</code>
     */
    private void setFontSize(int s) {
        int size_index;
        for (size_index = 0; size_index < size_box.getItemCount(); size_index++) if (((Integer) size_box.getItemAt(size_index)).intValue() == font_size) break;
        size_box.setSelectedIndex(size_index);
    }

    /**
     * change the font name
     *
     * @param s a <code>String</code>
     */
    private void setFontName(String s) {
        int font_index = 0;
        while (!fonts.getItemAt(font_index).equals(font_name)) font_index++;
        fonts.setSelectedIndex(font_index);
    }

    /**
     * Create a toolbar containing different ways to customize the tree view
     *
     * @param mb a <code>JMenuBar</code>
     * @return a <code>JToolBar</code>
     */
    private JToolBar getToolBar(JMenuBar mb) {
        Vector v = new Vector();
        for (int i = 6; i <= 12; i++) v.add(new Integer(i));
        for (int i = 14; i <= 28; i += 2) v.add(new Integer(i));
        for (int i = 32; i <= 48; i += 4) v.add(new Integer(i));
        size_box = new JComboBox(v);
        size_box.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                font_size = ((Integer) ((JComboBox) e.getSource()).getSelectedItem()).intValue();
                redraw();
            }
        });
        setFontSize(font_size);
        String[] font_list = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fonts = new JComboBox(font_list);
        setFontName(font_name);
        fonts.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                font_name = (String) ((JComboBox) e.getSource()).getSelectedItem();
                redraw();
            }
        });
        fonts.setMaximumSize(fonts.getPreferredSize());
        size_box.setMaximumSize(size_box.getPreferredSize());
        bg_button.setPreferredSize(new Dimension(29, 29));
        bg_button.setBackground(bg_color);
        bg_button.setFont(new Font("Serif", Font.BOLD, 16));
        bg_button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Color c = JColorChooser.showDialog(View.this, "Choose a Background Color", bg_color);
                if (c != null) {
                    bg_color = c;
                    ((JButton) e.getSource()).setBackground(c);
                    refresh();
                }
            }
        });
        fg_button.setPreferredSize(new Dimension(29, 29));
        fg_button.setForeground(fg_color);
        fg_button.setFont(new Font("Serif", Font.BOLD, 16));
        fg_button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Color c = JColorChooser.showDialog(View.this, "Choose a Background Color", fg_color);
                if (c != null) {
                    fg_color = c;
                    ((JButton) e.getSource()).setForeground(c);
                    refresh();
                }
            }
        });
        JMenu fileM = new JMenu("File");
        mb.add(fileM);
        JMenuItem saveM = new JMenuItem("Export");
        fileM.add(saveM);
        ActionListener saveAL = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.showSaveDialog(View.this);
                File file = chooser.getSelectedFile();
                if (file == null) return;
                if (file.getName().endsWith(".gif")) {
                    Dimension dim = canvas.getSize();
                    Image image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_BYTE_INDEXED);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    g.setColor(bg_color);
                    g.fillRect(0, 0, dim.width, dim.height);
                    g.setColor(fg_color);
                    layed_out = false;
                    display(g);
                    try {
                        GIFOutputStream os = new GIFOutputStream(new FileOutputStream(file));
                        os.write(image);
                    } catch (IOException E) {
                        E.printStackTrace();
                    }
                } else if (file.getName().endsWith(".jpg")) {
                    try {
                        exportToJPG(file);
                    } catch (IOException E) {
                        E.printStackTrace();
                    }
                } else if (file.getName().endsWith(".ps")) {
                    exportToPS(file.toString());
                } else if (file.getName().endsWith(".png")) {
                    try {
                        export(file, "png");
                    } catch (IOException E) {
                        E.printStackTrace();
                    }
                }
            }
        };
        saveM.addActionListener(saveAL);
        JToolBar jt = new JToolBar();
        jt.add(fonts);
        jt.add(size_box);
        jt.addSeparator();
        jt.add(bg_button);
        jt.add(fg_button);
        jt.addSeparator();
        jt.add(Box.createGlue());
        return jt;
    }

    /**
     * Exports this tree to a postscript file
     *
     * @param file the file name
     */
    public void exportToPS(String file) {
        try {
            Graphics postscript = new PSGr2(new FileWriter(file));
            display(postscript);
        } catch (IOException E) {
            E.printStackTrace();
        }
    }

    public void exportToJPG(File file) throws IOException {
        export(file, "jpg");
    }

    /**
     * Output the tree in jpeg format
     *
     * @param os an <code>OutputStream</code>
     */
    public void export(File file, String type) throws IOException {
        Dimension dim = new Dimension(2000, 2000);
        BufferedImage image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bg_color);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(fg_color);
        layed_out = false;
        display(g);
        BufferedImage subimage = image.getSubimage(0, 0, tree.width, tree.height);
        image.flush();
        image = null;
        ImageIO.write(subimage, type, file);
        subimage.flush();
    }

    /**
     * Refresh the tree but do not recompute node layouts
     */
    protected void refresh() {
        if (tree != null) canvas.repaint(new Rectangle(tree.width, tree.height));
    }

    /**
     * Relayout and redraw the tree
     */
    public void redraw() {
        font = new Font(font_name, Font.PLAIN, font_size);
        layed_out = false;
        refresh();
        repaint();
        canvas.revalidate();
    }

    /**
     * Draw the tree into a graphics context
     *
     * @param g a <code>Graphics</code>
     */
    protected void display(Graphics g) {
        g.setFont(font);
        if (!layed_out) {
            tree.layout(g);
            layed_out = true;
        }
        displayChildren(g, tree.getRoot());
        canvas.setPreferredSize(new Dimension(tree.width, tree.height));
        canvas.revalidate();
    }

    /**
     * A special function to draw lines.  This is because jpeg and gif output
     * doesn't work right wihtout the lines being drawn in the special
     * Graphics2D way.  I have no idea why.
     *
     * @param g a <code>Graphics</code>
     * @param x1 an <code>int</code>
     * @param y1 an <code>int</code>
     * @param x2 an <code>int</code>
     * @param y2 an <code>int</code>
     */
    protected void drawLine(Graphics g, int x1, int y1, int x2, int y2) {
        if (g instanceof Graphics2D) ((Graphics2D) g).draw(new Line2D.Double(x1, y1, x2, y2)); else g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draw the children of a particular node to the graphics context
     *
     * @param g a <code>Graphics</code>
     * @param n a <code>Node</code>
     */
    private void displayChildren(Graphics g, Node n) {
        n.draw(g);
        int min = Integer.MAX_VALUE, max = 0;
        Collection children = tree.getChildren(n);
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Node child = (Node) i.next();
            displayChildren(g, child);
            Point pt = child.getPosition();
            if (pt.getY() > max) max = (int) pt.getY();
            if (pt.getY() < min) min = (int) pt.getY();
        }
        if (children.size() > 0) {
            Node child = (Node) children.iterator().next();
            int child_x = (int) child.getPosition().getX();
            drawLine(g, child_x - 2, min - (int) child.getSize(g).getHeight(), child_x - 2, max);
            int n_y = (int) (n.getPosition().getY() - n.getSize(g).getHeight() / 2 + 3);
            int n_x = (int) (n.getPosition().getX() + n.getSize(g).getWidth()) + 2;
            drawLine(g, n_x, n_y, child_x - 2, n_y);
        }
    }
}
