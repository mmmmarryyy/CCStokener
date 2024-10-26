package uk.ac.ebi.pride.tools.converter.gui.dialogs;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.ebi.pride.tools.converter.gui.component.table.BaseTable;
import uk.ac.ebi.pride.tools.converter.report.model.CvParam;
import uk.ac.ebi.pride.tools.converter.report.model.Param;
import uk.ac.ebi.pride.tools.converter.report.model.Reference;
import uk.ac.ebi.pride.tools.converter.report.model.ReportObject;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author User #3
 */
public class ReferenceDialog extends AbstractDialog {

    public ReferenceDialog(Frame owner, BaseTable parentTable) {
        super(owner);
        initComponents();
        this.callback = parentTable;
    }

    public ReferenceDialog(Dialog owner, BaseTable parentTable) {
        super(owner);
        initComponents();
        this.callback = parentTable;
    }

    private void pubmedLookupButtonActionPerformed() {
        try {
            String ncbiUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=" + pubmedIdField.getText();
            URL url = new URL(ncbiUrl);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(url.openStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("DocSum");
            if (nList.getLength() == 1) {
                PubMedReference ref = new PubMedReference();
                NodeList items = doc.getElementsByTagName("Item");
                for (int i = 0; i < items.getLength(); i++) {
                    Node node = items.item(i);
                    String nodeName = node.getAttributes().getNamedItem("Name").getFirstChild().getNodeValue();
                    if (nodeName.equals("PubDate")) {
                        ref.setDate(getValue(node));
                    }
                    if (nodeName.equals("Source")) {
                        ref.setSource(getValue(node));
                    }
                    if (nodeName.equals("Author")) {
                        ref.addAuthor(getValue(node));
                    }
                    if (nodeName.equals("Title")) {
                        ref.setTitle(getValue(node));
                    }
                    if (nodeName.equals("Volume")) {
                        ref.setVolume(getValue(node));
                    }
                    if (nodeName.equals("Issue")) {
                        ref.setIssue(getValue(node));
                    }
                    if (nodeName.equals("Pages")) {
                        ref.setPages(getValue(node));
                    }
                    if (nodeName.equals("doi")) {
                        ref.setDoi(getValue(node));
                    }
                    if (nodeName.equals("pubmed")) {
                        ref.setPmid(getValue(node));
                    }
                }
                referenceArea.setText(ref.toCitation());
                if (ref.getDOI() != null) {
                    doiIDField.setText(ref.getDOI());
                }
            } else {
                if (nList.getLength() == 0) {
                    JOptionPane.showMessageDialog(this, "Pubmed ID not found", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "More than one document returned for pubmed ID: " + pubmedIdField.getText(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private String getValue(Node node) {
        if (node.getFirstChild() != null) {
            return node.getFirstChild().getNodeValue();
        } else {
            return node.getNodeValue();
        }
    }

    private void okButtonActionPerformed() {
        Param p = new Param();
        if (isNonNullTextField(pubmedIdField.getText())) {
            p.getCvParam().add(new CvParam("PubMed", pubmedIdField.getText(), referenceArea.getText(), null));
        }
        if (isNonNullTextField(doiIDField.getText())) {
            p.getCvParam().add(new CvParam("doi", doiIDField.getText(), doiIDField.getText(), null));
        }
        Reference ref = new Reference(referenceArea.getText(), p);
        if (!isEditing) {
            callback.add(ref);
        } else {
            callback.update(ref);
        }
        setVisible(false);
        dispose();
    }

    private void cancelButtonActionPerformed() {
        setVisible(false);
        dispose();
    }

    @Override
    public void edit(ReportObject object) {
        Reference r = (Reference) object;
        referenceArea.setText(r.getRefLine());
        Param p = r.getAdditional();
        if (p != null) {
            for (CvParam c : p.getCvParam()) {
                if (c.getCvLabel().equalsIgnoreCase("pubmed")) {
                    pubmedIdField.setText(c.getAccession());
                }
                if (c.getCvLabel().equalsIgnoreCase("doi")) {
                    doiIDField.setText(c.getAccession());
                }
            }
        }
    }

    private void pubmedIdFieldFocusLost() {
        if (pubmedIdField.getText() != null) {
            pubmedIdField.setText(pubmedIdField.getText().trim());
        }
    }

    private void initComponents() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        label2 = new JLabel();
        pubmedIdField = new JTextField();
        pubmedLookupButton = new JButton();
        label3 = new JLabel();
        doiIDField = new JTextField();
        label1 = new JLabel();
        scrollPane1 = new JScrollPane();
        referenceArea = new JTextArea();
        okButton = new JButton();
        cancelButton = new JButton();
        setTitle(bundle.getString("NewReferenceDialog.this.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        Container contentPane = getContentPane();
        label2.setText(bundle.getString("NewReferenceDialog.label2.text"));
        pubmedIdField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                pubmedIdFieldFocusLost();
            }
        });
        pubmedLookupButton.setText(bundle.getString("NewReferenceDialog.pubmedLookupButton.text"));
        pubmedLookupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pubmedLookupButtonActionPerformed();
            }
        });
        label3.setText(bundle.getString("NewReferenceDialog.label3.text"));
        label1.setText(bundle.getString("NewReferenceDialog.label1.text"));
        {
            referenceArea.setWrapStyleWord(true);
            referenceArea.setLineWrap(true);
            scrollPane1.setViewportView(referenceArea);
        }
        okButton.setText(bundle.getString("NewReferenceDialog.okButton.text"));
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                okButtonActionPerformed();
            }
        });
        cancelButton.setText(bundle.getString("NewReferenceDialog.cancelButton.text"));
        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButtonActionPerformed();
            }
        });
        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(contentPaneLayout.createParallelGroup().addGroup(contentPaneLayout.createSequentialGroup().addContainerGap().addGroup(contentPaneLayout.createParallelGroup().addGroup(contentPaneLayout.createSequentialGroup().addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(label3).addComponent(label1).addComponent(label2)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(contentPaneLayout.createParallelGroup().addGroup(contentPaneLayout.createSequentialGroup().addComponent(pubmedIdField, GroupLayout.PREFERRED_SIZE, 247, GroupLayout.PREFERRED_SIZE).addGap(9, 9, 9).addComponent(pubmedLookupButton, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE)).addComponent(doiIDField, GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE).addComponent(scrollPane1))).addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup().addComponent(okButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton))).addContainerGap()));
        contentPaneLayout.setVerticalGroup(contentPaneLayout.createParallelGroup().addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup().addContainerGap(12, Short.MAX_VALUE).addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label2).addComponent(pubmedIdField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(pubmedLookupButton)).addGap(12, 12, 12).addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(doiIDField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(label3)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(contentPaneLayout.createParallelGroup().addComponent(label1).addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 119, GroupLayout.PREFERRED_SIZE)).addGap(19, 19, 19).addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(cancelButton).addComponent(okButton)).addContainerGap()));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private JLabel label2;

    private JTextField pubmedIdField;

    private JButton pubmedLookupButton;

    private JLabel label3;

    private JTextField doiIDField;

    private JLabel label1;

    private JScrollPane scrollPane1;

    private JTextArea referenceArea;

    private JButton okButton;

    private JButton cancelButton;

    private static class PubMedReference {

        private String date;

        private String title;

        private String pmid;

        private List<String> authors = new ArrayList<String>();

        private String source;

        private String volume;

        private String issue;

        private String pages;

        private String doi;

        public void setDate(String date) {
            this.date = date;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setPmid(String pmid) {
            this.pmid = pmid;
        }

        public void addAuthor(String author) {
            authors.add(author);
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public void setPages(String pages) {
            this.pages = pages;
        }

        public void setDoi(String doi) {
            this.doi = doi;
        }

        public String toCitation() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> i = authors.iterator(); i.hasNext(); ) {
                sb.append(i.next());
                if (i.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append("; ");
                }
            }
            sb.append(title).append(", ").append(source).append(", ").append(date).append(", ");
            if (volume != null) {
                sb.append(volume).append(", ");
            }
            if (issue != null) {
                sb.append(issue).append(", ");
            }
            if (pages != null) {
                sb.append(pages).append(", ");
            }
            return sb.toString();
        }

        public String getDOI() {
            return doi;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("PubMedReference");
            sb.append("{date='").append(date).append('\'');
            sb.append(", title='").append(title).append('\'');
            sb.append(", pmid='").append(pmid).append('\'');
            sb.append(", authors=").append(authors);
            sb.append(", source='").append(source).append('\'');
            sb.append(", volume='").append(volume).append('\'');
            sb.append(", issue='").append(issue).append('\'');
            sb.append(", pages='").append(pages).append('\'');
            sb.append(", doi='").append(doi).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
