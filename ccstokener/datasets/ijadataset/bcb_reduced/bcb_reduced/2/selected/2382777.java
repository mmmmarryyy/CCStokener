package wabclient;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.text.*;
import org.mozilla.javascript.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

public class WindowHandler extends ParserHandler {

    WABWindow win;

    WABTable table;

    SortedTableModel model;

    DefaultTableModel m;

    JTree treeview = null;

    DefaultMutableTreeNode treeview_root = null;

    DefaultMutableTreeNode act_node = null;

    int columnNumber;

    int rowNumber;

    JMenu windowMenu;

    JMenu menu;

    JPopupMenu rmbmenu;

    JMenuItem windowMenuOverlapped;

    JMenuItem windowMenuTile;

    JMenuItem windowMenuArrange;

    JComboBox combo;

    JList list;

    Vector listdata;

    int layout;

    JToolBar toolbar;

    Vector headerWidths;

    Vector types;

    Vector aligns;

    Vector formats;

    String combo_text = "";

    Vector groupboxes = new Vector();

    Stack componentStack = new Stack();

    public WindowHandler(WABGlobal global, WABWindow win, String url) {
        super(global, url);
        this.win = win;
    }

    private void startWindow(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("window without properties");
            return;
        }
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Color bgcolor = prop.getColor("bgcolor");
        String caption = prop.getValue("caption", "");
        String lay = prop.getValue("layout", "0");
        boolean statusbar = prop.getValue("statusbar", false);
        win.setBounds(x, y, width, height);
        win.setTitle(caption);
        if (bgcolor != null) win.setBackground(bgcolor);
        if (lay.equalsIgnoreCase("borderlayout")) layout = 2; else if (lay.equalsIgnoreCase("flowlayout")) layout = 1; else if (lay.equalsIgnoreCase("null")) layout = 0; else layout = Integer.parseInt(lay);
        if (layout == 1) win.getContentPane().setLayout(new FlowLayout()); else if (layout == 2) win.getContentPane().setLayout(new BorderLayout()); else win.getContentPane().setLayout(null);
        win.restorePosition(caption);
        componentStack.push(win.getContentPane());
    }

    private void startSplitPanel(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("splitpanel without properties");
            return;
        }
        String id = prop.getValue("id", "");
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Color bgcolor = prop.getColor("bgcolor");
        String orientation = prop.getValue("orientation", "VERTICAL");
        Integer dividersize = prop.getInteger("dividersize");
        Integer dividerlocation = prop.getInteger("dividerlocation");
        boolean ote = prop.getValue("onetouchexpandable", false);
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        JSplitPane sp = new JSplitPane();
        sp.setName(id);
        if (bgcolor != null) sp.setBackground(bgcolor);
        sp.setVisible(visible);
        sp.setEnabled(enabled);
        if (orientation.equalsIgnoreCase("VERTICAL")) sp.setOrientation(JSplitPane.VERTICAL_SPLIT); else sp.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        if (dividersize != null) sp.setDividerSize(dividersize.intValue());
        if (dividerlocation != null) sp.setDividerLocation(dividerlocation.intValue());
        if (ote) sp.setOneTouchExpandable(true);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            sp.setBounds(x, y, width, height);
            c.add(sp);
        } else {
            sp.setPreferredSize(new Dimension(width, height));
            c.add(sp, constraints);
        }
        componentStack.push(sp);
    }

    private void startDesktopPane(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("desktoppanel without properties");
            return;
        }
        Object constraints = prop.getValue("constraints");
        String id = prop.getValue("id");
        int dragmode = prop.getValue("dragmode", JDesktopPane.LIVE_DRAG_MODE);
        JDesktopPane dp = new JDesktopPane();
        dp.setName(id);
        dp.setDragMode(dragmode);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) c.add(dp); else c.add(dp, constraints);
        componentStack.push(dp);
    }

    private void startTabcontrol(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("tabcontrol without properties");
            return;
        }
        String id = prop.getValue("id", "");
        String name = prop.getValue("name", "");
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Color bgcolor = prop.getColor("bgcolor");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        String placement = prop.getValue("placement", "TOP");
        JTabbedPane pane = new JTabbedPane();
        pane.setName(id);
        if (bgcolor != null) pane.setBackground(bgcolor);
        pane.setVisible(visible);
        pane.setEnabled(enabled);
        if (placement.equalsIgnoreCase("BOTTOM")) pane.setTabPlacement(JTabbedPane.BOTTOM);
        if (placement.equalsIgnoreCase("LEFT")) pane.setTabPlacement(JTabbedPane.LEFT);
        if (placement.equalsIgnoreCase("RIGHT")) pane.setTabPlacement(JTabbedPane.RIGHT);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            pane.setBounds(x, y, width, height);
            c.add(pane);
        } else {
            pane.setPreferredSize(new Dimension(width, height));
            c.add(pane, constraints);
        }
        componentStack.push(pane);
    }

    private void startPanel(wabclient.Attributes prop) throws SAXException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (prop == null) {
            System.err.println("panel without properties");
            return;
        }
        String id = prop.getValue("id", "");
        String name = prop.getValue("name", "");
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Color bgcolor = prop.getColor("bgcolor");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        String lay = prop.getValue("layout", "0");
        String language = prop.getValue("language", "java");
        String src = prop.getString("src");
        JPanel pane = null;
        if (src == null) {
            pane = new JPanel();
            pane.setName(id);
            if (bgcolor != null) pane.setBackground(bgcolor);
            pane.setVisible(visible);
            pane.setEnabled(enabled);
            if (lay.equalsIgnoreCase("borderlayout")) layout = 2; else if (lay.equalsIgnoreCase("flowlayout")) layout = 1; else if (lay.equalsIgnoreCase("null")) layout = 0; else layout = Integer.parseInt(lay);
            if (layout == 1) pane.setLayout(new FlowLayout()); else if (layout == 2) pane.setLayout(new BorderLayout()); else pane.setLayout(null);
        } else {
            if (language.equalsIgnoreCase("java")) {
                Class cls = Class.forName(src);
                Object obj = cls.newInstance();
                if (obj instanceof JPanel) pane = (JPanel) obj;
            }
        }
        if (pane != null) {
            Container c = (Container) componentStack.peek();
            if (c.getLayout() == null) {
                pane.setBounds(x, y, width, height);
                c.add(pane);
            } else {
                pane.setPreferredSize(new Dimension(width, height));
                c.add(pane, constraints);
            }
            componentStack.push(pane);
        }
    }

    private void startStatusbar(wabclient.Attributes prop) throws SAXException {
        JLabel statusbar = new JLabel();
        statusbar.setPreferredSize(new Dimension(20, 20));
        statusbar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        win.setStatusbar(statusbar);
    }

    private void startToolbar(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("toolbar without properties");
            return;
        }
        String id = prop.getValue("id", "");
        String name = prop.getValue("name", "");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        toolbar = new JToolBar(name);
        toolbar.setName((String) id);
        toolbar.setToolTipText(name);
        toolbar.setVisible(visible);
        toolbar.setEnabled(enabled);
        win.addToolBar(toolbar);
    }

    private void startToolbarbutton(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("toolbarbutton without properties");
            return;
        }
        String id = prop.getValue("id", "");
        String icon = prop.getValue("icon", "");
        String command = prop.getValue("command", "");
        String tooltip = prop.getValue("tooltip", "");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        try {
            ImageIcon img = new ImageIcon(new URL(icon));
            BufferedImage image = new BufferedImage(25, 25, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = image.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, 25, 25);
            g.drawImage(img.getImage(), 4, 4, 16, 16, (ImageObserver) null);
            BufferedImage pressed = new BufferedImage(25, 25, BufferedImage.TYPE_4BYTE_ABGR);
            g = pressed.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, 25, 25);
            g.drawImage(img.getImage(), 5, 5, 16, 16, (ImageObserver) null);
            g.setColor(new Color(132, 132, 132));
            g.drawLine(0, 0, 24, 0);
            g.drawLine(0, 0, 0, 24);
            g.setColor(new Color(255, 255, 255));
            g.drawLine(24, 24, 24, 0);
            g.drawLine(24, 24, 0, 24);
            BufferedImage over = new BufferedImage(25, 25, BufferedImage.TYPE_4BYTE_ABGR);
            g = over.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, 25, 25);
            g.drawImage(img.getImage(), 4, 4, 16, 16, (ImageObserver) null);
            g.setColor(new Color(255, 255, 255));
            g.drawLine(0, 0, 24, 0);
            g.drawLine(0, 0, 0, 24);
            g.setColor(new Color(132, 132, 132));
            g.drawLine(24, 24, 24, 0);
            g.drawLine(24, 24, 0, 24);
            JButton b = new JButton(new ImageIcon(image));
            b.setRolloverEnabled(true);
            b.setPressedIcon(new ImageIcon(pressed));
            b.setRolloverIcon(new ImageIcon(over));
            b.setToolTipText(tooltip);
            b.setActionCommand(command);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.addActionListener(win);
            b.setEnabled(enabled);
            b.setVisible(visible);
            b.setName((String) id);
            toolbar.add(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMenu(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("menu without properties");
            return;
        }
        String id = prop.getValue("id", "");
        String label = prop.getValue("label", "");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        menu = new JMenu();
        menu.setName((String) id);
        menu.setVisible(visible);
        menu.setEnabled(enabled);
        int mnepos = label.indexOf("~");
        if (mnepos < 0) mnepos = label.indexOf("&");
        if (mnepos > -1) {
            label = label.substring(0, mnepos) + label.substring(mnepos + 1);
            menu.setText(label);
            menu.setMnemonic(label.charAt(mnepos));
        } else menu.setText(label);
        win.getJMenuBar().add(menu);
    }

    private void startMenuitem(wabclient.Attributes prop) throws SAXException, MalformedURLException {
        if (prop == null) {
            System.err.println("menuitem without properties");
            return;
        }
        JMenuItem item;
        String id = prop.getValue("id", "");
        String action = prop.getValue("action", "");
        String label = prop.getValue("label", "");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        boolean checked = prop.getValue("checked", false);
        boolean checkable = prop.getValue("checkable", false);
        String icon = prop.getValue("icon", "");
        int index = prop.getValue("index", 0);
        String accelerator = prop.getValue("accelerator", "");
        if (checkable) {
            item = new JCheckBoxMenuItem();
            item.setSelected(checked);
        } else item = new JMenuItem();
        int mnepos = label.indexOf("~");
        if (mnepos < 0) mnepos = label.indexOf("&");
        if (mnepos > -1) {
            label = label.substring(0, mnepos) + label.substring(mnepos + 1);
            item.setText(label);
            item.setMnemonic(label.charAt(mnepos));
        } else item.setText(label);
        item.setName((String) id);
        item.setEnabled(enabled);
        item.setVisible(visible);
        item.setActionCommand(action);
        item.addActionListener(win);
        item.setText(label);
        if (accelerator.length() > 0) {
            int mask = 0;
            int key = 0;
            if (accelerator.startsWith("ctrl-")) {
                mask = KeyEvent.CTRL_MASK;
                key = accelerator.substring(5).charAt(0);
            } else if (accelerator.startsWith("shift-")) {
                mask = KeyEvent.SHIFT_MASK;
                key = accelerator.substring(6).charAt(0);
            } else key = accelerator.charAt(0);
            item.setAccelerator(KeyStroke.getKeyStroke(key, mask, false));
        }
        if (icon.length() > 0) item.setIcon(new ImageIcon(new URL(icon)));
        if (!visible) item.setVisible(false);
        menu.add(item);
    }

    private void startChoice(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("choice without properties");
            return;
        }
        combo = new JComboBox();
        list = null;
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        Object constraints = prop.getValue("constraints");
        boolean editable = prop.getValue("editable", false);
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        combo_text = prop.getValue("text", "");
        combo.setName((String) id);
        if (editable) {
            combo.setEditable(editable);
            combo.setSelectedItem(combo_text);
        }
        combo.setVisible(visible);
        combo.setEnabled(enabled);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            combo.setBounds(x, y, width, height);
            c.add(combo);
        } else {
            combo.setPreferredSize(new Dimension(width, height));
            c.add(combo, constraints);
        }
    }

    private void startList(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("list without properties");
            return;
        }
        list = new JList();
        combo = null;
        listdata = new Vector();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        list.setName(id);
        list.setListData(listdata);
        JScrollPane sp = new JScrollPane(list);
        sp.setVisible(visible);
        sp.setEnabled(enabled);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            sp.setBounds(x, y, width, height);
            c.add(sp);
        } else {
            sp.setPreferredSize(new Dimension(width, height));
            c.add(sp, constraints);
        }
    }

    private void startOption(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("choice.option without properties");
            return;
        }
        String value = prop.getValue("value", "");
        String text = prop.getValue("text", "");
        if (list != null) listdata.addElement(new ComboOption(text, value));
        if (combo != null) {
            ComboOption co = new ComboOption(text, value);
            combo.addItem(co);
            if (combo_text.equals(text.trim())) combo.setSelectedItem(co);
        }
    }

    private void startLabel(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("label without properties");
            return;
        }
        JLabel label = new JLabel();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String text = prop.getValue("text", "");
        Color color = prop.getColor("color");
        Color bgcolor = prop.getColor("bgcolor");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        String id = prop.getValue("id", "");
        int align = prop.getValue("align", 0);
        label.setName(id);
        label.setText(text);
        label.setVisible(visible);
        label.setEnabled(enabled);
        int swing_align;
        switch(align) {
            case 1:
                swing_align = JLabel.RIGHT;
                break;
            case 2:
                swing_align = JLabel.CENTER;
                break;
            default:
                swing_align = JLabel.LEFT;
                break;
        }
        label.setHorizontalAlignment(swing_align);
        if (bgcolor != null) label.setBackground(bgcolor);
        if (color != null) label.setForeground(color);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            label.setBounds(x, y, width, height);
            c.add(label);
        } else {
            label.setPreferredSize(new Dimension(width, height));
            c.add(label, constraints);
        }
    }

    private void startButton(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("button without properties");
            return;
        }
        JButton btn = new JButton();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        String text = prop.getValue("text", "");
        Color color = prop.getColor("color");
        Color bgcolor = prop.getColor("bgcolor");
        String onmouseup = prop.getValue("onmouseup", "");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        if (bgcolor != null) btn.setBackground(bgcolor);
        if (color != null) btn.setForeground(color);
        btn.setText(text);
        btn.addActionListener(win);
        btn.setActionCommand(onmouseup);
        btn.setVisible(visible);
        btn.setEnabled(enabled);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            btn.setBounds(x, y, width, height);
            c.add(btn);
        } else {
            btn.setPreferredSize(new Dimension(width, height));
            c.add(btn, constraints);
        }
    }

    private void startGroupbox(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("radiobutton without properties");
            return;
        }
        WABGroupbox grb = new WABGroupbox();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        String text = prop.getValue("text", "");
        Object constraints = prop.getValue("constraints");
        grb.setText(text);
        grb.setName(id);
        groupboxes.addElement(grb);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            grb.setBounds(x, y, width, height);
            c.add(grb);
        } else {
            grb.setPreferredSize(new Dimension(width, height));
            c.add(grb, constraints);
        }
    }

    private void startRadioButton(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("radiobutton without properties");
            return;
        }
        JRadioButton rb = new JRadioButton();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        String text = prop.getValue("text", "");
        Object constraints = prop.getValue("constraints");
        String checked = prop.getValue("checked", "false");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        String group = prop.getValue("group", "");
        rb.setBounds(x, y, width, height);
        rb.setText(text);
        rb.setName((String) id);
        rb.setSelected(checked.equalsIgnoreCase("true"));
        rb.setVisible(visible);
        rb.setEnabled(enabled);
        for (int i = 0; i < groupboxes.size(); i++) {
            WABGroupbox gb = (WABGroupbox) groupboxes.elementAt(i);
            if (gb.getName().equals(group)) {
                rb.setLocation(rb.getX() - gb.getX(), rb.getY() - gb.getY());
                gb.add(rb);
                break;
            }
        }
    }

    private void startCheckbox(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("checkbox without properties");
            return;
        }
        JCheckBox cb = new JCheckBox();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String id = prop.getValue("id", "");
        String text = prop.getValue("text", "");
        String onmouseup = prop.getValue("onmouseup", "");
        Object constraints = prop.getValue("constraints");
        String checked = prop.getValue("checked", "false");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        cb.setText(text);
        cb.setName((String) id);
        cb.setSelected(checked.equalsIgnoreCase("true"));
        cb.setVisible(visible);
        cb.setEnabled(enabled);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            cb.setBounds(x, y, width, height);
            c.add(cb);
        } else {
            cb.setPreferredSize(new Dimension(width, height));
            c.add(cb, constraints);
        }
    }

    private void startImage(wabclient.Attributes prop) throws SAXException, MalformedURLException {
        if (prop == null) {
            System.err.println("image without properties");
            return;
        }
        JLabel label = new JLabel();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        String src = prop.getValue("src", "");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        label.setIcon(new ImageIcon(new URL(src)));
        label.setBounds(x, y, width, height);
        label.setVisible(visible);
        label.setEnabled(enabled);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            label.setBounds(x, y, width, height);
            c.add(label);
        } else {
            label.setPreferredSize(new Dimension(width, height));
            c.add(label, constraints);
        }
    }

    private void startTextArea(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("textarea without properties");
            return;
        }
        JTextArea ta = new JTextArea();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Object id = prop.getValue("id");
        String text = prop.getValue("text", "");
        Color color = prop.getColor("color");
        Color bgcolor = prop.getColor("bgcolor");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        if (bgcolor != null) ta.setBackground(bgcolor);
        if (color != null) ta.setForeground(color);
        ta.setText(text);
        ta.setName((String) id);
        ta.setVisible(visible);
        ta.setEnabled(enabled);
        JScrollPane sp = new JScrollPane(ta);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            sp.setBounds(x, y, width, height);
            c.add(sp);
        } else {
            sp.setPreferredSize(new Dimension(width, height));
            c.add(sp, constraints);
        }
    }

    private void startSingleLineEdit(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("singlelineedit without properties");
            return;
        }
        String pwd = prop.getValue("password", "");
        JTextField sle;
        if (pwd.equalsIgnoreCase("true")) sle = new JPasswordField(); else sle = new JTextField();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Object id = prop.getValue("id");
        String text = prop.getValue("text", "");
        Color color = prop.getColor("color");
        Color bgcolor = prop.getColor("bgcolor");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        int align = prop.getValue("align", 0);
        if (bgcolor != null) sle.setBackground(bgcolor);
        if (color != null) sle.setForeground(color);
        sle.setText(text);
        sle.setName((String) id);
        sle.setVisible(visible);
        sle.setEnabled(enabled);
        int swing_align;
        switch(align) {
            case 1:
                swing_align = JTextField.RIGHT;
                break;
            case 2:
                swing_align = JTextField.CENTER;
                break;
            default:
                swing_align = JTextField.LEFT;
                break;
        }
        sle.setHorizontalAlignment(swing_align);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            sle.setBounds(x, y, width, height);
            c.add(sle);
        } else {
            sle.setPreferredSize(new Dimension(width, height));
            c.add(sle, constraints);
        }
    }

    private void startTreeview(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("treeview without properties");
            return;
        }
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        boolean rootvisible = prop.getValue("rootvisible", true);
        boolean showsroothandles = prop.getValue("showsroothandles", false);
        treeview = new JTree();
        treeview.setVisible(visible);
        treeview.setEnabled(enabled);
        treeview.setRootVisible(rootvisible);
        treeview.setShowsRootHandles(showsroothandles);
        JScrollPane sp = new JScrollPane(treeview);
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) c.add(sp); else c.add(sp, constraints);
    }

    private void startTreeitem(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("treeview.treeitem without properties");
            return;
        }
        String text = prop.getValue("text", "");
        String value = prop.getValue("value", "");
        boolean expanded = prop.getValue("expanded", false);
        if (treeview_root == null) {
            treeview_root = new DefaultMutableTreeNode(text);
            treeview.setModel(new DefaultTreeModel(treeview_root, false));
            act_node = treeview_root;
        } else {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);
            act_node.add(node);
            act_node = node;
        }
    }

    private void startTable(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("table without properties");
            return;
        }
        String id = prop.getValue("id", "");
        table = new WABTable(global, win);
        table.setName((String) id);
        model = (SortedTableModel) table.getModel();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        JScrollPane sp = new JScrollPane(table);
        table.setVisible(visible);
        table.setEnabled(enabled);
        rowNumber = 0;
        columnNumber = 0;
        headerWidths = new Vector();
        types = new Vector();
        aligns = new Vector();
        formats = new Vector();
        rmbmenu = table.rmbmenu;
        Container c = (Container) componentStack.peek();
        if (c.getLayout() == null) {
            sp.setBounds(x, y, width, height);
            c.add(sp);
        } else {
            sp.setPreferredSize(new Dimension(width, height));
            c.add(sp, constraints);
        }
    }

    private void startHeader(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("table.header without properties");
            return;
        }
        String text = prop.getValue("text", "");
        int width = prop.getValue("width", 0);
        int type = prop.getValue("type", 0);
        int align = prop.getValue("align", 0);
        String format = prop.getValue("format", "");
        boolean editable = prop.getValue("editable", false);
        headerWidths.addElement(new Integer(width));
        types.addElement(new Integer(type));
        switch(align) {
            case 1:
                aligns.addElement(new Integer(javax.swing.SwingConstants.RIGHT));
                break;
            case 2:
                aligns.addElement(new Integer(javax.swing.SwingConstants.CENTER));
                break;
            default:
                aligns.addElement(new Integer(javax.swing.SwingConstants.LEFT));
                break;
        }
        model.addColumn(text, type, editable);
        formats.addElement(format);
    }

    public void startElement(String uri, String tag, String qName, org.xml.sax.Attributes attributes) throws SAXException {
        wabclient.Attributes prop = new wabclient.Attributes(attributes);
        try {
            if (tag.equals("window")) startWindow(prop); else if (tag.equals("splitpanel")) startSplitPanel(prop); else if (tag.equals("desktoppane")) startDesktopPane(prop); else if (tag.equals("tabcontrol")) startTabcontrol(prop); else if (tag.equals("panel")) startPanel(prop); else if (tag.equals("statusbar")) startStatusbar(prop); else if (tag.equals("toolbar")) startToolbar(prop); else if (tag.equals("toolbarbutton")) startToolbarbutton(prop); else if (tag.equals("menu")) startMenu(prop); else if (tag.equals("menuitem")) startMenuitem(prop); else if (tag.equals("separator")) menu.addSeparator(); else if (tag.equals("choice")) startChoice(prop); else if (tag.equals("list")) startList(prop); else if (tag.equals("option")) startOption(prop); else if (tag.equals("label")) startLabel(prop); else if (tag.equals("button")) startButton(prop); else if (tag.equals("groupbox")) startGroupbox(prop); else if (tag.equals("radiobutton")) startRadioButton(prop); else if (tag.equals("checkbox")) startCheckbox(prop); else if (tag.equals("image")) startImage(prop); else if (tag.equals("textarea")) startTextArea(prop); else if (tag.equals("singlelineedit")) startSingleLineEdit(prop); else if (tag.equals("treeview")) startTreeview(prop); else if (tag.equals("treeitem")) startTreeitem(prop); else if (tag.equals("table")) startTable(prop); else if (tag.equals("header")) startHeader(prop); else if (tag.equals("row")) {
                rowNumber++;
                columnNumber = 0;
                model.addRow();
            } else if (tag.equals("column")) {
                columnNumber++;
                if (prop == null) {
                    System.err.println("table.column without properties");
                    return;
                }
                String value = prop.getValue("value", "");
                model.setValueAt(value, rowNumber - 1, columnNumber - 1);
            } else if (tag.equals("rmbmenuitem")) {
                if (prop == null) {
                    System.err.println("datawindow.menuitem without properties");
                    return;
                }
                String action = prop.getValue("action", "");
                String label = prop.getValue("label", "");
                JMenuItem mi = new JMenuItem(label);
                mi.setActionCommand(action);
                mi.addActionListener(win);
                rmbmenu.add(mi);
            } else if (tag.equals("rmbseparator")) {
                rmbmenu.addSeparator();
            } else if (tag.equals("script")) {
                win.beginScript();
                String url = prop.getValue("src");
                if (url.length() > 0) {
                    try {
                        BufferedReader r = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                        String buffer;
                        while (true) {
                            buffer = r.readLine();
                            if (buffer == null) break;
                            win.script += buffer + "\n";
                        }
                        r.close();
                        win.endScript();
                    } catch (IOException ioe) {
                        System.err.println("[IOError] " + ioe.getMessage());
                        System.exit(0);
                    }
                }
            } else System.err.println("[win] unparsed tag: " + tag);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void endElement(String uri, String tag, String qName) throws SAXException {
        if (tag.equals("script")) win.endScript(); else if (tag.equals("splitpanel")) componentStack.pop(); else if (tag.equals("panel")) componentStack.pop(); else if (tag.equals("desktoppane")) componentStack.pop(); else if (tag.equals("tabcontrol")) componentStack.pop(); else if (tag.equals("treeitem")) act_node = (DefaultMutableTreeNode) act_node.getParent(); else if (tag.equals("table")) {
            NumberFormat nf = NumberFormat.getInstance();
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < headerWidths.size(); i++) {
                Integer width = (Integer) headerWidths.elementAt(i);
                TableColumn column = table.getColumnModel().getColumn(i);
                if (width.intValue() == 0) column.setMinWidth(0);
                column.setPreferredWidth(width.intValue());
                column.setHeaderRenderer(new DefaultTableCellRenderer() {

                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isselected, boolean hasfocus, int row, int col) {
                        JLabel l = new JLabel((String) value);
                        l.setBorder(new Border() {

                            public Insets getBorderInsets(Component c) {
                                return new Insets(2, 2, 2, 2);
                            }

                            public boolean isBorderOpaque() {
                                return true;
                            }

                            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                                g.setColor(Color.white);
                                g.drawLine(0, 0, width, 0);
                                g.drawLine(0, 0, 0, height);
                                g.setColor(Color.darkGray);
                                g.drawLine(1, height - 1, width, height - 1);
                                g.drawLine(width - 1, height - 1, width - 1, 1);
                            }
                        });
                        l.setFont(new Font("Arial Narrow", Font.PLAIN, 12));
                        l.setOpaque(true);
                        return l;
                    }
                });
                Integer type = (Integer) types.elementAt(i);
                final String format = (String) formats.elementAt(i);
                final Integer align = (Integer) aligns.elementAt(i);
                if (type.intValue() == 1) {
                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String text = "";
                            try {
                                NumberFormat nf = NumberFormat.getInstance();
                                Number num = null;
                                if (value instanceof Number) num = (Number) value; else if (value instanceof String) num = nf.parse((String) value);
                                text = nf.format(num);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Component c = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
                            c.setFont(new Font("Arial Narrow", Font.PLAIN, 12));
                            if (c instanceof JLabel) {
                                JLabel l = (JLabel) c;
                                l.setHorizontalAlignment(align.intValue());
                            }
                            if (!hasFocus && !isSelected) {
                                SortedTableModel model = (SortedTableModel) table.getModel();
                                c.setForeground(model.getCellColor(row, column));
                            }
                            return c;
                        }
                    });
                } else if (type.intValue() == 2) {
                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String text = null;
                            try {
                                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
                                SimpleDateFormat out;
                                if (format.length() > 0) out = new SimpleDateFormat(format); else out = new SimpleDateFormat("dd.MM.yy");
                                Date d = null;
                                if (value instanceof Date) d = (Date) value; else if (value instanceof String) d = in.parse((String) value);
                                if (d == null) text = ""; else text = out.format(d);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Component c = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
                            c.setFont(new Font("Arial Narrow", Font.PLAIN, 12));
                            if (c instanceof JLabel) {
                                JLabel l = (JLabel) c;
                                l.setHorizontalAlignment(align.intValue());
                            }
                            if (!hasFocus && !isSelected) {
                                SortedTableModel model = (SortedTableModel) table.getModel();
                                c.setForeground(model.getCellColor(row, column));
                            }
                            return c;
                        }
                    });
                } else if (type.intValue() == 3) {
                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            String text = null;
                            try {
                                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                SimpleDateFormat out;
                                if (format.length() > 0) out = new SimpleDateFormat(format); else out = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
                                Date d = null;
                                if (value instanceof Date) d = (Date) value; else if (value instanceof String) d = in.parse((String) value);
                                if (d == null) text = ""; else text = out.format(d);
                            } catch (Exception e) {
                            }
                            Component c = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
                            c.setFont(new Font("Arial Narrow", Font.PLAIN, 12));
                            if (c instanceof JLabel) {
                                JLabel l = (JLabel) c;
                                l.setHorizontalAlignment(align.intValue());
                            }
                            if (!hasFocus && !isSelected) {
                                SortedTableModel model = (SortedTableModel) table.getModel();
                                c.setForeground(model.getCellColor(row, column));
                            }
                            return c;
                        }
                    });
                } else if (type.intValue() == 4) {
                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            boolean val;
                            if (((String) value).equalsIgnoreCase("Y") || ((String) value).equalsIgnoreCase("true")) val = true; else val = false;
                            JCheckBox c = new JCheckBox();
                            c.setPreferredSize(new Dimension(14, 14));
                            c.setSelected(val);
                            JPanel p = new JPanel();
                            if (hasFocus) {
                                p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                                p.setBorder(new LineBorder(Color.yellow));
                            } else p.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
                            p.add(c);
                            if (isSelected) {
                                p.setBackground(table.getSelectionBackground());
                                c.setBackground(table.getSelectionBackground());
                            } else {
                                p.setBackground(table.getBackground());
                                c.setBackground(table.getBackground());
                            }
                            if (!hasFocus && !isSelected) {
                                SortedTableModel model = (SortedTableModel) table.getModel();
                                p.setForeground(model.getCellColor(row, column));
                            }
                            return p;
                        }
                    });
                    column.setCellEditor(new TableCellEditor() {

                        protected int clickCountToStart = 1;

                        protected EventListenerList listenerList = new EventListenerList();

                        protected transient ChangeEvent changeEvent = null;

                        protected JCheckBox cb;

                        protected int row;

                        protected int column;

                        protected Object org_value;

                        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                            org_value = value;
                            boolean val;
                            if (((String) value).equalsIgnoreCase("Y")) val = true; else val = false;
                            this.row = row;
                            this.column = column;
                            cb = new JCheckBox();
                            cb.setPreferredSize(new Dimension(14, 14));
                            cb.setSelected(val);
                            cb.addActionListener(new CheckboxActionListener(row, column) {

                                public void actionPerformed(ActionEvent e) {
                                    Object args[] = { new Integer(this.row), new Integer(this.column), cb.isSelected() ? "Y" : "N" };
                                    win.fireEvent("clicked", args);
                                }
                            });
                            JPanel p = new JPanel();
                            p.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
                            p.add(cb);
                            if (isSelected) {
                                p.setBackground(table.getSelectionBackground());
                                cb.setBackground(table.getSelectionBackground());
                            } else {
                                p.setBackground(table.getBackground());
                                cb.setBackground(table.getBackground());
                            }
                            return p;
                        }

                        public void addCellEditorListener(CellEditorListener l) {
                            listenerList.add(CellEditorListener.class, l);
                        }

                        public void cancelCellEditing() {
                            fireEditingCanceled();
                        }

                        public Object getCellEditorValue() {
                            return cb.isSelected() ? "Y" : "N";
                        }

                        public boolean isCellEditable(EventObject anEvent) {
                            if (anEvent instanceof MouseEvent) {
                                return ((MouseEvent) anEvent).getClickCount() >= clickCountToStart;
                            }
                            return true;
                        }

                        public void removeCellEditorListener(CellEditorListener l) {
                            listenerList.remove(CellEditorListener.class, l);
                        }

                        public boolean shouldSelectCell(EventObject anEvent) {
                            return true;
                        }

                        public boolean stopCellEditing() {
                            fireEditingStopped();
                            return true;
                        }

                        protected void fireEditingCanceled() {
                            Object[] listeners = listenerList.getListenerList();
                            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                                if (listeners[i] == CellEditorListener.class) {
                                    if (changeEvent == null) changeEvent = new ChangeEvent(this);
                                    ((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
                                }
                            }
                        }

                        protected void fireEditingStopped() {
                            Object[] listeners = listenerList.getListenerList();
                            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                                if (listeners[i] == CellEditorListener.class) {
                                    if (changeEvent == null) changeEvent = new ChangeEvent(this);
                                    ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
                                }
                            }
                        }
                    });
                } else {
                    column.setCellRenderer(new DefaultTableCellRenderer() {

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            c.setFont(new Font("Arial Narrow", Font.PLAIN, 12));
                            if (c instanceof JLabel) {
                                JLabel l = (JLabel) c;
                                l.setHorizontalAlignment(align.intValue());
                            }
                            if (!hasFocus && !isSelected) {
                                SortedTableModel model = (SortedTableModel) table.getModel();
                                c.setForeground(model.getCellColor(row, column));
                            }
                            return c;
                        }
                    });
                    column.setCellEditor(new TableCellEditor() {

                        protected int clickCountToStart = 2;

                        protected EventListenerList listenerList = new EventListenerList();

                        protected transient ChangeEvent changeEvent = null;

                        protected JTextField tf;

                        protected int row;

                        protected int column;

                        protected Object org_value;

                        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                            org_value = value;
                            this.row = row;
                            this.column = column;
                            tf = new JTextField();
                            tf.setText((String) value);
                            return tf;
                        }

                        public void addCellEditorListener(CellEditorListener l) {
                            listenerList.add(CellEditorListener.class, l);
                        }

                        public void cancelCellEditing() {
                            fireEditingCanceled();
                        }

                        public Object getCellEditorValue() {
                            return tf.getText();
                        }

                        public boolean isCellEditable(EventObject anEvent) {
                            if (anEvent instanceof MouseEvent) {
                                return ((MouseEvent) anEvent).getClickCount() >= clickCountToStart;
                            }
                            return true;
                        }

                        public void removeCellEditorListener(CellEditorListener l) {
                            listenerList.remove(CellEditorListener.class, l);
                        }

                        public boolean shouldSelectCell(EventObject anEvent) {
                            return true;
                        }

                        public boolean stopCellEditing() {
                            fireEditingStopped();
                            return true;
                        }

                        protected void fireEditingCanceled() {
                            Object[] listeners = listenerList.getListenerList();
                            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                                if (listeners[i] == CellEditorListener.class) {
                                    if (changeEvent == null) changeEvent = new ChangeEvent(this);
                                    ((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
                                }
                            }
                        }

                        protected void fireEditingStopped() {
                            Object[] listeners = listenerList.getListenerList();
                            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                                if (listeners[i] == CellEditorListener.class) {
                                    if (changeEvent == null) changeEvent = new ChangeEvent(this);
                                    ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
                                }
                            }
                        }
                    });
                }
                if (width.intValue() == 0) column.setResizable(false);
            }
        } else if (tag.equals("window")) {
            win.initScripting();
            win.setVisible(true);
            win.postEvent("onload");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (win.scriptOpen) win.script += new String(ch, start, length);
    }

    class CheckboxActionListener implements ActionListener {

        int row, column;

        public CheckboxActionListener(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public void actionPerformed(ActionEvent e) {
        }
    }
}
