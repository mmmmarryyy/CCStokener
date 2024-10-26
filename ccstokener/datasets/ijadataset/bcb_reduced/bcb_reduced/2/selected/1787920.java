package fr.cephb.lindenb.bio.ncbo.bioportal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.lindenb.lang.ResourceUtils;
import org.lindenb.lang.RunnableObject;
import org.lindenb.swing.SwingUtils;
import org.lindenb.swing.table.GenericTableModel;

public class NCBOSearchPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static class TermTableModel extends GenericTableModel<NCBOSearchBean> {

        private static final long serialVersionUID = 1L;

        @Override
        public int getColumnCount() {
            return 8;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex < 2 ? Integer.class : String.class;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "Ontology Version-Id";
                case 1:
                    return "Ontology-Id";
                case 2:
                    return "Ontology Label";
                case 3:
                    return "Record-Type";
                case 4:
                    return "Concept-Id";
                case 5:
                    return "ConceptId Short";
                case 6:
                    return "Preferred Name";
                case 7:
                    return "Contents";
            }
            return null;
        }

        @Override
        public Object getValueOf(NCBOSearchBean bean, int column) {
            switch(column) {
                case 0:
                    return bean.getOntologyVersionId();
                case 1:
                    return bean.getOntologyId();
                case 2:
                    return bean.getOntologyLabel();
                case 3:
                    return bean.getRecordType();
                case 4:
                    return bean.getConceptId();
                case 5:
                    return bean.getConceptIdShort();
                case 6:
                    return bean.getPreferredName();
                case 7:
                    return bean.getContents();
            }
            return null;
        }
    }

    private JTextField tfTerm;

    private TermTableModel tableModel;

    private AbstractAction searchAction;

    private JTable table;

    private Runner thread = null;

    private JCheckBox cbPerfect;

    private SpinnerNumberModel pageIndex;

    private SpinnerNumberModel pageCount;

    private JTextField tfIndo;

    private JProgressBar progressBar;

    private JTextPane beanInfo;

    private BeanInfoRunner beanInfoThread = null;

    private class Runner extends Thread {

        NCBOSearch engine = new NCBOSearch();

        String term;

        Runner(String term) {
            this.term = term;
            this.engine.setExactmatch(cbPerfect.isSelected());
            this.engine.setPageIndex(pageIndex.getNumber().intValue());
            this.engine.setResultCount(pageCount.getNumber().intValue());
        }

        @Override
        public void run() {
            try {
                progressBar.setIndeterminate(true);
                tfIndo.setText("Searching " + term);
                tfIndo.setCaretPosition(0);
                List<NCBOSearchBean> items = this.engine.search(this.term);
                if (thread == this) {
                    tableModel.clear();
                    tableModel.addAll(items);
                    thread = null;
                }
                tfIndo.setText("");
            } catch (Throwable err) {
                tfIndo.setText("" + err.getMessage());
                java.awt.Toolkit.getDefaultToolkit().beep();
                err.printStackTrace();
            } finally {
                thread = null;
                progressBar.setIndeterminate(false);
                tfIndo.setCaretPosition(0);
            }
        }
    }

    private class BeanInfoRunner extends Thread {

        NCBOSearchBean term;

        BeanInfoRunner(NCBOSearchBean term) {
            this.term = term;
        }

        @Override
        public void run() {
            try {
                if (this.term == null || beanInfoThread != this) return;
                StringBuilder builder = new StringBuilder(NCBO.BIOPORTAL_URL);
                builder.append("/concepts/");
                builder.append(term.getOntologyVersionId());
                builder.append("/");
                builder.append(URLEncoder.encode(term.getConceptIdShort(), "UTF-8"));
                URL url = new URL(builder.toString());
                InputStream in = url.openStream();
                TransformerFactory f = TransformerFactory.newInstance();
                StreamSource stylesheet = new StreamSource(ResourceUtils.getResourceAsStream(NCBOSearchPane.class, "bioportal2html.xsl"));
                Transformer transformer = f.newTransformer(stylesheet);
                StringWriter strw = new StringWriter();
                StreamResult result = new StreamResult(strw);
                transformer.transform(new StreamSource(in), result);
                in.close();
                if (beanInfoThread != this) return;
                SwingUtilities.invokeLater(new RunnableObject<String>(strw.toString()) {

                    @Override
                    public void run() {
                        beanInfo.setText(getObject().toString().trim());
                        beanInfo.setCaretPosition(0);
                    }
                });
            } catch (Throwable err) {
                beanInfo.setText("" + err.getMessage());
                java.awt.Toolkit.getDefaultToolkit().beep();
                err.printStackTrace();
            } finally {
                beanInfoThread = null;
                beanInfo.setCaretPosition(0);
            }
        }
    }

    public NCBOSearchPane() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        add(pane, BorderLayout.NORTH);
        pane.add(new JLabel("Search:", JLabel.RIGHT));
        pane.add(this.tfTerm = new JTextField(20));
        this.searchAction = new AbstractAction("Go") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                String term = tfTerm.getText().trim();
                search(term);
            }
        };
        this.tfTerm.addActionListener(this.searchAction);
        pane.add(new JButton(this.searchAction));
        pane.add(this.cbPerfect = new JCheckBox("Perfect", false));
        pane.add(new JCheckBox("Properties", false));
        pane.add(new JLabel("Page:", JLabel.RIGHT));
        pane.add(new JSpinner(this.pageIndex = new SpinnerNumberModel(1, 1, 100, 1)));
        pane.add(new JLabel("Count:", JLabel.RIGHT));
        pane.add(new JSpinner(this.pageCount = new SpinnerNumberModel(100, 1, 1000, 1)));
        JPanel left = new JPanel(new BorderLayout());
        this.tableModel = new TermTableModel();
        this.table = new JTable(this.tableModel);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        left.add(new JScrollPane(table), BorderLayout.CENTER);
        pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        this.add(pane, BorderLayout.SOUTH);
        pane.add(this.tfIndo = new JTextField(50));
        this.tfIndo.setEditable(false);
        pane.add(this.progressBar = new JProgressBar());
        this.progressBar.setPreferredSize(new Dimension(100, 20));
        JPanel right = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(this.beanInfo = new JTextPane());
        right.add(scroll, BorderLayout.CENTER);
        this.beanInfo.setEditable(false);
        this.beanInfo.setContentType("text/html");
        this.beanInfo.setEditorKit(new HTMLEditorKit());
        right.setPreferredSize(new Dimension(200, 200));
        this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right));
        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                search(getSelectedBean());
            }
        });
    }

    public NCBOSearchBean getSelectedBean() {
        int i = table.getSelectedRow();
        if (i == -1) return null;
        NCBOSearchBean bean = tableModel.elementAt(i);
        return bean;
    }

    public void search(String term) {
        if (term == null || term.length() == 0) return;
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception e) {
            }
            return;
        }
        this.thread = new Runner(term);
        this.thread.start();
    }

    public void search(NCBOSearchBean bean) {
        if (bean == null) {
            beanInfo.setText("");
            return;
        }
        if (beanInfoThread != null) {
            try {
                beanInfoThread.interrupt();
            } catch (Exception e) {
            }
            return;
        }
        this.beanInfoThread = new BeanInfoRunner(bean);
        this.beanInfoThread.start();
    }

    public static void main(String[] args) {
        try {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new NCBOSearchPane());
            SwingUtils.center(f, 200, 200);
            SwingUtils.show(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
