package org.web3d.x3d.palette.items;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;

/**
 * ExternProtoDeclareSyncHelper.java
 * Created on Feb 10, 2009, 10:55 AM
 *
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA, USA
 * www.nps.edu
 *
 * @author Mike Bailey
 * @version $Id$
 */
public class ExternProtoDeclareSyncHelper extends javax.swing.JPanel implements ListSelectionListener {

    public static void checkFields(UrlExpandableList2 uList, ExpandableList fieldsList) {
        ExternProtoDeclareSyncHelper pan = new ExternProtoDeclareSyncHelper();
        pan.setData(uList, fieldsList);
        DialogDescriptor descriptor = new DialogDescriptor(pan, NbBundle.getMessage(ExternProtoDeclareSyncHelper.class, "ExternProtoDeclareSyncHelper.VerifyFieldsDialogTitle"));
        Dialog dlg = null;
        try {
            dlg = DialogDisplayer.getDefault().createDialog(descriptor);
            dlg.setResizable(true);
            dlg.pack();
            dlg.setVisible(true);
            if (pan.checkThread != null) pan.checkThread.interrupt();
        } finally {
            if (dlg != null) dlg.dispose();
        }
    }

    public ExternProtoDeclareSyncHelper() {
        initComponents();
        urlJlist.addListSelectionListener(this);
    }

    private UrlExpandableList2 uLis;

    private ExpandableList fieldsLis;

    public void setData(UrlExpandableList2 uLis, ExpandableList fieldsLis) {
        this.uLis = uLis;
        this.fieldsLis = fieldsLis;
        String[] sa = uLis.getUrlData();
        Vector<String> v = new Vector<String>();
        int i = 0;
        for (String s : sa) if (uLis.isGreenLink(i++)) v.add(s);
        urlJlist.setListData(v);
        String[][] fa = fieldsLis.getData();
        Object[][] oaa = new Object[fa.length][];
        i = 0;
        ExternColumn ec = new ExternColumn();
        for (String[] ssa : fa) {
            Object[] oa = new Object[5];
            oa[0] = ssa[0];
            oa[1] = ssa[1];
            oa[2] = ssa[2];
            oa[3] = ec;
            oa[4] = ec;
            oaa[i++] = oa;
        }
        ((DefaultTableModel) jTable1.getModel()).setDataVector(oaa, columnNames);
        jTable1.getColumnModel().getColumn(3).setCellRenderer(new MyTwoColumnRenderer());
        jTable1.getColumnModel().getColumn(4).setCellRenderer(new MyTwoColumnRenderer());
    }

    class ExternColumn {
    }

    String[] columnNames = { "Field name", "Local type", "Local access type", "Extern type", "Extern access type" };

    class MyTwoColumnRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String fieldName = (String) table.getValueAt(row, 0);
            if (externHM != null) {
                Element externElem = externHM.get(fieldName);
                if (externElem != null) {
                    if (column == 3) {
                        Attribute typeAttr = externElem.getAttribute("type");
                        String localType = (String) table.getValueAt(row, 1);
                        if (localType.equals(typeAttr.getValue())) return new JLabel(localType); else {
                            JLabel lab = new JLabel(typeAttr.getValue());
                            lab.setForeground(Color.red);
                            setStatus("Error found");
                            return lab;
                        }
                    } else {
                        Attribute accAttr = externElem.getAttribute("accessType");
                        String accType = (String) table.getValueAt(row, 2);
                        if (accType.equals(accAttr.getValue())) return new JLabel(accType); else {
                            JLabel lab = new JLabel(accAttr.getValue());
                            lab.setForeground(Color.red);
                            setStatus("Error found");
                            return lab;
                        }
                    }
                }
            }
            JLabel lab = new JLabel("");
            lab.setForeground(Color.red);
            return lab;
        }
    }

    public Thread checkThread;

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        if (checkThread != null) {
            checkThread.interrupt();
        }
        String url = (String) urlJlist.getSelectedValue();
        checkThread = new Thread(new Checker(url), "ExternProtoDeclareSyncHelperThread");
        checkThread.setPriority(Thread.NORM_PRIORITY);
        checkThread.start();
    }

    class Checker implements Runnable {

        String urlStr;

        Checker(String s) {
            urlStr = s;
        }

        public void run() {
            String err = "Error";
            try {
                setStatus("Loading from url...");
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(getUrlContents(urlStr));
                setStatus("Reading xml...");
                processExtern(doc, urlStr);
                setStatus("");
                checkThread = null;
                return;
            } catch (MalformedURLException ex) {
                err = "Bad URL";
            } catch (JDOMException jex) {
                err = jex.getLocalizedMessage();
            } catch (IOException iex) {
                err = iex.getLocalizedMessage();
            } catch (Exception ex) {
            }
            setStatus(err);
            checkThread = null;
        }
    }

    /**
   * From UrlStatus.java
   * @param urlStr
   * @return
   */
    private Reader getUrlContents(String urlStr) throws Exception {
        URL urlObj = uLis.buildUrl(urlStr);
        URLConnection uConn = urlObj.openConnection();
        InputStream inStr = new BufferedInputStream(uConn.getInputStream());
        return new InputStreamReader(inStr);
    }

    private HashMap<String, Element> externHM;

    private void processExtern(Document doc, String urlStr) {
        String anchor = "";
        if (urlStr.indexOf('#') != -1) anchor = urlStr.substring(urlStr.lastIndexOf('#') + 1, urlStr.length());
        Element protoDecl = findProtoDeclare(doc, anchor);
        if (protoDecl != null) {
            Element protoIF = protoDecl.getChild("ProtoInterface");
            if (protoIF != null) {
                externHM = new HashMap<String, Element>();
                List fieldList = protoIF.getChildren("field");
                for (Object o : fieldList) {
                    Element el = (Element) o;
                    Attribute name = el.getAttribute("name");
                    externHM.put(name.getValue(), el);
                }
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((DefaultTableModel) jTable1.getModel()).fireTableDataChanged();
                    }
                });
            }
        }
    }

    private Element findProtoDeclare(Document doc, String anchor) {
        ContentFilter filter = new ContentFilter(ContentFilter.ELEMENT);
        Element root = doc.getRootElement();
        return findProtoElement("ProtoDeclare", anchor, root, filter);
    }

    private Element findProtoElement(String elemtype, String nameAttr, Element e, ContentFilter fil) {
        if (e.getName().equals(elemtype)) {
            if (nameAttr == null) return e;
            if (e.getAttribute("name").getValue().equals(nameAttr)) return e;
        }
        List lis = e.getContent(fil);
        for (Object o : lis) {
            Element elm = findProtoElement(elemtype, nameAttr, (Element) o, fil);
            if (elm != null) return elm;
        }
        return null;
    }

    private void setStatus(final String s) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                statusTF.setText(s);
            }
        });
    }

    /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jSplitPane1 = new javax.swing.JSplitPane();
        urlScroller = new javax.swing.JScrollPane();
        urlJlist = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        tableScroller = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        statusTF = new javax.swing.JTextField();
        setName("Form");
        setLayout(new java.awt.GridBagLayout());
        jSplitPane1.setDividerLocation(100);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.2);
        jSplitPane1.setName("jSplitPane1");
        urlScroller.setBorder(javax.swing.BorderFactory.createTitledBorder(NbBundle.getMessage(getClass(), "ExternProtoDeclareSyncHelper.urlScroller.border.title")));
        urlScroller.setName("urlScroller");
        urlJlist.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        urlJlist.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        urlJlist.setName("urlJlist");
        urlScroller.setViewportView(urlJlist);
        jSplitPane1.setTopComponent(urlScroller);
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(NbBundle.getMessage(getClass(), "ExternProtoDeclareSyncHelper.jPanel1.border.title")));
        jPanel1.setName("jPanel1");
        jPanel1.setLayout(new java.awt.GridBagLayout());
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(NbBundle.getMessage(getClass(), "ExternProtoDeclareSyncHelper.jLabel2.text"));
        jLabel2.setName("jLabel2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
        jPanel1.add(jLabel2, gridBagConstraints);
        tableScroller.setName("tableScroller");
        tableScroller.setPreferredSize(new java.awt.Dimension(375, 80));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null }, { null, null, null, null, null } }, new String[] { "Field name", "Local type", "Local access type", "Extern type", "Extern access type" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false, false, false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jTable1.setName("jTable1");
        jTable1.setRequestFocusEnabled(false);
        tableScroller.setViewportView(jTable1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(tableScroller, gridBagConstraints);
        jSplitPane1.setRightComponent(jPanel1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
        add(jSplitPane1, gridBagConstraints);
        statusTF.setEditable(false);
        statusTF.setText(NbBundle.getMessage(getClass(), "ExternProtoDeclareSyncHelper.statusTF.text"));
        statusTF.setName("statusTF");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(statusTF, gridBagConstraints);
    }

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JSplitPane jSplitPane1;

    private javax.swing.JTable jTable1;

    private javax.swing.JTextField statusTF;

    private javax.swing.JScrollPane tableScroller;

    private javax.swing.JList urlJlist;

    private javax.swing.JScrollPane urlScroller;
}
