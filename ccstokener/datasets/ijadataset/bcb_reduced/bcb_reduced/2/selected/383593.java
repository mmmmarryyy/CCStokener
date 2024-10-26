package org.olga.rebus.gui.help;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.olga.rebus.gui.ColorConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RebusHelp extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JFrame myOwner;

    private final JSplitPane mySplit = new JSplitPane();

    private final JEditorPane myEditor;

    private final JScrollPane myView;

    private final JButton myBackButton = new JButton("<");

    private final JButton myNextButton = new JButton(">");

    private int myCurrentPosition = -1;

    private final LinkedList<URL> myPagesHistory = new LinkedList<URL>();

    public RebusHelp(JFrame owner) {
        super("Help");
        myOwner = owner;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        myEditor = new JEditorPane();
        myEditor.setEditable(false);
        myEditor.setBackground(ColorConstants.HELP_BACKGROUND_COLOR);
        myEditor.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                    return;
                }
                setPage(createHtml(e.getDescription()));
            }
        });
        myView = new JScrollPane(myEditor);
        myBackButton.setEnabled(false);
        myBackButton.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                myCurrentPosition--;
                try {
                    myEditor.setPage(myPagesHistory.get(myCurrentPosition));
                } catch (IOException e1) {
                }
                if (myCurrentPosition == 0) {
                    myBackButton.setEnabled(false);
                }
                myNextButton.setEnabled(true);
            }
        });
        myNextButton.setEnabled(false);
        myNextButton.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                myCurrentPosition++;
                try {
                    myEditor.setPage(myPagesHistory.get(myCurrentPosition));
                } catch (IOException e1) {
                }
                myBackButton.setEnabled(true);
                if (myCurrentPosition == myPagesHistory.size() - 1) {
                    myNextButton.setEnabled(false);
                }
            }
        });
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        controlPanel.add(myBackButton);
        controlPanel.add(myNextButton);
        controlPanel.setBackground(ColorConstants.HELP_BACKGROUND_COLOR);
        controlPanel.setMaximumSize(new Dimension(200, 50));
        JPanel leftSplitPanel = new JPanel();
        leftSplitPanel.setLayout(new BoxLayout(leftSplitPanel, BoxLayout.Y_AXIS));
        leftSplitPanel.add(controlPanel);
        leftSplitPanel.add(myView);
        leftSplitPanel.setBackground(ColorConstants.HELP_BACKGROUND_COLOR);
        mySplit.setContinuousLayout(true);
        mySplit.setDividerLocation(0.1);
        mySplit.setResizeWeight(0.1);
        mySplit.setOneTouchExpandable(true);
        mySplit.setDividerSize(5);
        mySplit.setAlignmentX(Component.CENTER_ALIGNMENT);
        mySplit.setLeftComponent(new JScrollPane(createHelpTree()));
        mySplit.setRightComponent(leftSplitPanel);
        getContentPane().add(mySplit);
        setSize(600, 400);
        setCalculateLocation();
        setVisible(true);
    }

    private void setPage(URL page) {
        try {
            myEditor.setPage(page);
            if (myCurrentPosition != myPagesHistory.size() - 1) {
                for (int i = myCurrentPosition; i < myPagesHistory.size(); i++) {
                    myPagesHistory.removeLast();
                }
                myNextButton.setEnabled(false);
            }
            myPagesHistory.add(page);
            myCurrentPosition++;
            myBackButton.setEnabled(true);
        } catch (IOException ex) {
        }
    }

    private void setCalculateLocation() {
        int x = myOwner.getLocationOnScreen().x + (myOwner.getWidth() - getWidth()) / 2;
        int y = myOwner.getLocationOnScreen().y + (myOwner.getHeight() - getHeight()) / 2;
        setLocation(x, y);
    }

    private DefaultMutableTreeNode createTreeContent(Element element) {
        HelpTreeNode treeNode = new HelpTreeNode(createHtml(element.getAttribute("name")), element.getAttribute("description"));
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(treeNode);
        NodeList childs = element.getChildNodes();
        if (childs.getLength() == 0) {
            node.setAllowsChildren(false);
        } else {
            for (int i = 0; i < childs.getLength(); i++) {
                Node child = childs.item(i);
                if (child instanceof Element) {
                    node.add(createTreeContent((Element) child));
                }
            }
        }
        return node;
    }

    private DefaultMutableTreeNode parseTree() {
        try {
            DefaultMutableTreeNode root;
            URL url = RebusHelp.class.getResource("/org/olga/rebus/gui/help/html/content.xml");
            InputStream is = url.openStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setValidating(false);
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(is);
            NodeList elements = document.getElementsByTagName("help");
            Element element = (Element) elements.item(0);
            root = createTreeContent(element);
            is.close();
            return root;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        } catch (ParserConfigurationException e1) {
            System.out.println(e1.getMessage());
            return null;
        } catch (org.xml.sax.SAXException e2) {
            System.out.println(e2.getMessage());
            return null;
        }
    }

    private JTree createHelpTree() {
        DefaultMutableTreeNode root = parseTree();
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        JTree tree = new JTree(model);
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                JTree tree = (JTree) e.getSource();
                TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath == null) {
                    return;
                }
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                Object object = lastPathComponent.getUserObject();
                if (object != null && object instanceof HelpTreeNode) {
                    HelpTreeNode node = (HelpTreeNode) object;
                    setPage(node.getFile());
                    mySplit.revalidate();
                }
            }
        });
        tree.setBackground(ColorConstants.HELP_BACKGROUND_COLOR);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(ColorConstants.HELP_BACKGROUND_COLOR);
        renderer.setBackgroundSelectionColor(ColorConstants.HELP_SELECTION_COLOR);
        renderer.setTextNonSelectionColor(ColorConstants.HELP_TEXT_COLOR);
        renderer.setClosedIcon(createIcon("closeIcon"));
        renderer.setOpenIcon(createIcon("openIcon"));
        renderer.setLeafIcon(createIcon("leafIcon"));
        tree.setCellRenderer(renderer);
        return tree;
    }

    private URL createHtml(String name) {
        URL url = RebusHelp.class.getResource("/org/olga/rebus/gui/help/html/" + name);
        return url;
    }

    private ImageIcon createIcon(String name) {
        URL url = RebusHelp.class.getResource("/org/olga/rebus/gui/help/images/" + name + ".gif");
        return new ImageIcon(url);
    }
}
