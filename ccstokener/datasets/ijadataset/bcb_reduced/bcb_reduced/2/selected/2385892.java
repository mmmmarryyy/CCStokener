package wabclient;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.image.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import javax.swing.table.*;
import org.mozilla.javascript.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.SAXParser;

public class DialogHandler extends ParserHandler {

    WABDialog dialog;

    WABTable table;

    WABTextArea textarea;

    JComboBox combo;

    JList list;

    Vector listdata;

    SortedTableModel model;

    int columnNumber;

    int rowNumber;

    int layout;

    String combo_text = "";

    JPopupMenu rmbmenu;

    Vector groupboxes = new Vector();

    Vector headerWidths;

    Vector types;

    Vector aligns;

    Vector formats;

    public DialogHandler(WABGlobal global, WABDialog dialog, String url) {
        super(global, url);
        this.dialog = dialog;
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
        String menu = prop.getValue("menu", "");
        boolean statusbar = prop.getValue("statusbar", false);
        dialog.setBounds(x, y, width, height + 20);
        dialog.setTitle(caption);
        if (bgcolor != null) dialog.getContentPane().setBackground(bgcolor);
        dialog.setResizable(false);
        if (lay.equalsIgnoreCase("borderlayout")) layout = 2; else if (lay.equalsIgnoreCase("flowlayout")) layout = 1; else layout = Integer.parseInt(lay);
        if (layout == 1) dialog.getContentPane().setLayout(new FlowLayout()); else if (layout == 2) dialog.getContentPane().setLayout(new BorderLayout()); else dialog.getContentPane().setLayout(null);
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
        combo.setBounds(x, y, width, height);
        combo.setName((String) id);
        if (editable) {
            combo.setEditable(editable);
            combo.setSelectedItem(combo_text);
        }
        combo.setVisible(visible);
        combo.setEnabled(enabled);
        if (layout == 0) dialog.getContentPane().add(combo); else dialog.getContentPane().add(combo, constraints);
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
        list.setName((String) id);
        list.setListData(listdata);
        JScrollPane sp = new JScrollPane(list);
        sp.setBounds(x, y, width, height);
        sp.setVisible(visible);
        sp.setEnabled(enabled);
        if (layout == 0) dialog.getContentPane().add(sp); else dialog.getContentPane().add(sp, constraints);
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
        String onmouseup = prop.getValue("onmouseup", "");
        boolean defaultbtn = prop.getValue("default", false);
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        Color bgcolor = prop.getColor("bgcolor");
        Color color = prop.getColor("color");
        String fontname = prop.getValue("font-name", "Arial");
        int fontsize = prop.getValue("font-size", 12);
        int fontstyle = prop.getValue("font-style", 0);
        btn.setBounds(x, y, width, height);
        btn.setText(text);
        if (bgcolor != null) btn.setBackground(bgcolor);
        if (color != null) btn.setForeground(color);
        btn.setFont(new Font(fontname, fontstyle, fontsize));
        if (defaultbtn) dialog.getRootPane().setDefaultButton(btn);
        btn.addActionListener(dialog);
        btn.setActionCommand(onmouseup);
        btn.setVisible(visible);
        btn.setEnabled(enabled);
        if (layout == 0) dialog.getContentPane().add(btn); else dialog.getContentPane().add(btn, constraints);
    }

    private void startGroupbox(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("groupbox without properties");
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
        grb.setBounds(x, y, width, height);
        grb.setText(text);
        grb.setName(id);
        groupboxes.addElement(grb);
        if (layout == 0) dialog.getContentPane().add(grb); else dialog.getContentPane().add(grb, constraints);
    }

    private void startRadiobutton(wabclient.Attributes prop) throws SAXException {
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
        cb.setBounds(x, y, width, height);
        cb.setText(text);
        cb.setName((String) id);
        cb.setSelected(checked.equalsIgnoreCase("true"));
        cb.setVisible(visible);
        cb.setEnabled(enabled);
        if (layout == 0) dialog.getContentPane().add(cb); else dialog.getContentPane().add(cb, constraints);
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
        if (layout == 0) dialog.getContentPane().add(label); else dialog.getContentPane().add(label, constraints);
    }

    private void startTextarea(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("textarea without properties");
            return;
        }
        textarea = new WABTextArea();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        Object id = prop.getValue("id");
        String text = prop.getValue("text", "");
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        Color bgcolor = prop.getColor("bgcolor");
        Color color = prop.getColor("color");
        String fontname = prop.getValue("font-name", "Arial");
        int fontsize = prop.getValue("font-size", 12);
        int fontstyle = prop.getValue("font-style", 0);
        JScrollPane sp = new JScrollPane(textarea);
        sp.setBounds(x, y, width, height);
        textarea.setText(text);
        if (bgcolor != null) textarea.setBackground(bgcolor);
        if (color != null) textarea.setForeground(color);
        textarea.setName((String) id);
        textarea.setVisible(visible);
        textarea.setEnabled(enabled);
        textarea.setFont(new Font(fontname, fontstyle, fontsize));
        if (layout == 0) dialog.getContentPane().add(sp); else dialog.getContentPane().add(sp, constraints);
        rmbmenu = textarea.rmbmenu;
    }

    private void startSinglelineedit(wabclient.Attributes prop) throws SAXException {
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
        Object constraints = prop.getValue("constraints");
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        Color bgcolor = prop.getColor("bgcolor");
        Color color = prop.getColor("color");
        String fontname = prop.getValue("font-name", "Arial");
        int fontsize = prop.getValue("font-size", 12);
        int fontstyle = prop.getValue("font-style", 0);
        int align = prop.getValue("align", 0);
        sle.setBounds(x, y, width, height);
        sle.setText(text);
        if (bgcolor != null) sle.setBackground(bgcolor);
        if (color != null) sle.setForeground(color);
        sle.setName((String) id);
        sle.setVisible(visible);
        sle.setEnabled(enabled);
        sle.setFont(new Font(fontname, fontstyle, fontsize));
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
        if (layout == 0) dialog.getContentPane().add(sle); else dialog.getContentPane().add(sle, constraints);
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
        String fontname = prop.getValue("font-name", "Arial");
        int fontsize = prop.getValue("font-size", 12);
        int fontstyle = prop.getValue("font-style", 0);
        String id = prop.getValue("id", "");
        int align = prop.getValue("align", 0);
        label.setName(id);
        label.setBounds(x, y, width, height);
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
        label.setFont(new Font(fontname, fontstyle, fontsize));
        if (layout == 0) dialog.getContentPane().add(label); else dialog.getContentPane().add(label, constraints);
    }

    private void startTable(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("table without properties");
            return;
        }
        String id = prop.getValue("id", "");
        table = new WABTable(global, dialog);
        table.setName((String) id);
        model = (SortedTableModel) table.getModel();
        int x = prop.getValue("x", 0);
        int y = prop.getValue("y", 0);
        int width = prop.getValue("width", 0);
        int height = prop.getValue("height", 0);
        boolean visible = prop.getValue("visible", true);
        boolean enabled = prop.getValue("enabled", true);
        Object constraints = prop.getValue("constraints");
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(x, y, width, height);
        table.setVisible(visible);
        table.setEnabled(enabled);
        if (layout == 0) dialog.getContentPane().add(sp); else dialog.getContentPane().add(sp, constraints);
        rowNumber = 0;
        columnNumber = 0;
        headerWidths = new Vector();
        types = new Vector();
        aligns = new Vector();
        formats = new Vector();
        rmbmenu = table.rmbmenu;
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

    private void startRow(wabclient.Attributes prop) throws SAXException {
        rowNumber++;
        columnNumber = 0;
        model.addRow();
    }

    private void startColumn(wabclient.Attributes prop) throws SAXException {
        columnNumber++;
        if (prop == null) {
            System.err.println("table.column without properties");
            return;
        }
        String value = prop.getValue("value", "");
        model.setValueAt(value, rowNumber - 1, columnNumber - 1);
    }

    private void startRmbmenu(wabclient.Attributes prop) throws SAXException {
        if (prop == null) {
            System.err.println("datawindow.menuitem without properties");
            return;
        }
        String action = prop.getValue("action", "");
        String label = prop.getValue("label", "");
        JMenuItem mi = new JMenuItem(label);
        mi.setActionCommand(action);
        mi.addActionListener(dialog);
        if (rmbmenu != null) rmbmenu.add(mi);
    }

    private void startRmbseparator(wabclient.Attributes prop) throws SAXException {
        if (rmbmenu != null) rmbmenu.addSeparator();
    }

    private void startScript(wabclient.Attributes prop) throws SAXException {
        dialog.beginScript();
        String url = prop.getValue("src");
        if (url.length() > 0) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                String buffer;
                while (true) {
                    buffer = r.readLine();
                    if (buffer == null) break;
                    dialog.script += buffer + "\n";
                }
                r.close();
                dialog.endScript();
            } catch (IOException ioe) {
                System.err.println("[IOError] " + ioe.getMessage());
                System.exit(0);
            }
        }
    }

    public void startElement(String uri, String tag, String qName, org.xml.sax.Attributes attributes) throws SAXException {
        wabclient.Attributes prop = new wabclient.Attributes(attributes);
        try {
            if (tag.equals("window")) startWindow(prop); else if (tag.equals("choice")) startChoice(prop); else if (tag.equals("list")) startList(prop); else if (tag.equals("option")) startOption(prop); else if (tag.equals("button")) startButton(prop); else if (tag.equals("groupbox")) startGroupbox(prop); else if (tag.equals("radiobutton")) startRadiobutton(prop); else if (tag.equals("checkbox")) startCheckbox(prop); else if (tag.equals("image")) startImage(prop); else if (tag.equals("textarea")) startTextarea(prop); else if (tag.equals("singlelineedit")) startSinglelineedit(prop); else if (tag.equals("label")) startLabel(prop); else if (tag.equals("table")) startTable(prop); else if (tag.equals("header")) startHeader(prop); else if (tag.equals("row")) startRow(prop); else if (tag.equals("column")) startColumn(prop); else if (tag.equals("rmbmenuitem")) startRmbmenu(prop); else if (tag.equals("rmbseparator")) startRmbseparator(prop); else if (tag.equals("script")) startScript(prop); else System.err.println("[dlg] unparsed tag: " + tag);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void endElement(String uri, String tag, String qName) throws SAXException {
        if (tag.equals("script")) dialog.endScript(); else if (tag.equals("table")) {
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
            dialog.initScripting();
            dialog.postInternalEvent("onload");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        dialog.characters(new String(ch, start, length));
    }
}
