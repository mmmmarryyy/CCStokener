package org.moonwave.dconfig.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.moonwave.dconfig.ui.util.AppProperties;
import org.moonwave.dconfig.ui.model.PropertyItem;
import org.moonwave.dconfig.util.FileUtil;

/**
 *
 * @author  Jonathan Luo
 */
public class DlgAbout extends javax.swing.JDialog {

    private static final Log log = LogFactory.getLog(DlgAbout.class);

    javax.swing.JDialog dialog;

    static List propList = new ArrayList();

    static JScrollPane aboutPane;

    static JScrollPane licesePane;

    static JScrollPane propertyPane;

    public static List getPropertyList() {
        return propList;
    }

    /** Creates new form DlgAbout */
    public DlgAbout(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 6, p.y + parentSize.height / 4);
        }
        initComponents();
        KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
        Action escapeActionListener = new AbstractAction() {

            public void actionPerformed(ActionEvent actionEvent) {
                btnOk.doClick();
            }
        };
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(escape, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", escapeActionListener);
        postInitialization();
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jtp = new javax.swing.JTabbedPane();
        btnOk = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        btnOk.setMnemonic('O');
        btnOk.setText("Ok");
        btnOk.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jtp, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE).add(jPanel1Layout.createSequentialGroup().addContainerGap(220, Short.MAX_VALUE).add(btnOk, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(220, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().add(jtp, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE).add(12, 12, 12).add(btnOk)));
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public void postInitialization() {
        jtp.addTab("About", createAboutPane("about.text"));
        jtp.addTab("License", createLicensePane("license.text"));
        jtp.addTab("Properties", createPropertiesPane());
        dialog = this;
        KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
        Action escapeActionListener = new AbstractAction() {

            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);
            }
        };
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(escape, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", escapeActionListener);
    }

    private StyledDocument createDocument(String propertyKey) {
        StyleContext context = new StyleContext();
        StyledDocument document = new DefaultStyledDocument(context);
        Style style = context.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(style, 4);
        StyleConstants.setSpaceBelow(style, 4);
        StyleConstants.setFontSize(style, 14);
        try {
            String text = AppProperties.getInstance().getProperty(propertyKey);
            document.insertString(document.getLength(), text, style);
        } catch (BadLocationException e) {
            log.error(e);
        }
        return document;
    }

    private Component createAboutPane(String propertyKey) {
        if (aboutPane == null) {
            StyledDocument document = createDocument(propertyKey);
            JTextPane textPane = new JTextPane(document);
            textPane.setEditable(false);
            aboutPane = new JScrollPane(textPane);
        }
        return aboutPane;
    }

    private Component createLicensePane(String propertyKey) {
        if (licesePane == null) {
            String licenseText = "";
            BufferedReader in = null;
            try {
                String filename = "conf/LICENSE.txt";
                java.net.URL url = FileUtil.toURL(filename);
                in = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = null;
                while (true) {
                    line = in.readLine();
                    if (line == null) break;
                    licenseText += line;
                }
            } catch (Exception e) {
                log.error(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
                }
            }
            licenseText = StringUtils.replace(licenseText, "<br>", "\n");
            licenseText = StringUtils.replace(licenseText, "<p>", "\n\n");
            StyleContext context = new StyleContext();
            StyledDocument document = new DefaultStyledDocument(context);
            Style style = context.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
            StyleConstants.setSpaceAbove(style, 4);
            StyleConstants.setSpaceBelow(style, 4);
            StyleConstants.setFontSize(style, 14);
            try {
                document.insertString(document.getLength(), licenseText, style);
            } catch (BadLocationException e) {
                log.error(e);
            }
            JTextPane textPane = new JTextPane(document);
            textPane.setEditable(false);
            licesePane = new JScrollPane(textPane);
        }
        return licesePane;
    }

    private Component createPropertiesPane() {
        String[] columnNames = { "Name", "Value" };
        if (propertyPane == null) {
            Properties props = System.getProperties();
            Enumeration ePropNames = props.propertyNames();
            for (; ePropNames.hasMoreElements(); ) {
                String propName = (String) ePropNames.nextElement();
                String propValue = (String) props.get(propName);
                propList.add(new PropertyItem(propName, propValue));
            }
            Collections.sort(propList);
            String[][] propData = new String[propList.size()][2];
            PropertyItem item = null;
            for (int i = 0; i < propList.size(); i++) {
                item = (PropertyItem) propList.get(i);
                propData[i][0] = item.getName();
                propData[i][1] = item.getValue();
            }
            final JTable table = new JTable(propData, columnNames);
            table.setEnabled(false);
            propertyPane = new JScrollPane(table);
            propertyPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            TableColumn column = null;
            column = table.getColumnModel().getColumn(0);
            column.setPreferredWidth(160);
            column = table.getColumnModel().getColumn(1);
            column.setPreferredWidth(400);
        }
        return propertyPane;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new DlgAbout(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }

    private javax.swing.JButton btnOk;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JTabbedPane jtp;
}
