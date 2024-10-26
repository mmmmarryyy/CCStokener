package com.bbn.vessel.author.header;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdom.Element;
import com.bbn.vessel.author.graphEditor.editor.GraphEditorFrame;
import com.bbn.vessel.author.graphEditor.editor.ImageIcons;
import com.bbn.vessel.author.graphEditor.editor.TabPanel;
import com.bbn.vessel.author.models.Category;
import com.bbn.vessel.author.models.NodeSpec;
import com.bbn.vessel.author.models.NodeSpecTable;
import com.bbn.vessel.core.util.XMLHelper;

/**
 * <Enter the description of this type here>
 *
 * this code is currently dead. at some point we will want to turn it into an
 * AuthoringTool
 *
 * @author RTomlinson
 */
@SuppressWarnings("serial")
public class NodeTypeEditor {

    private final MyRootNode rootNode = new MyRootNode();

    private final DefaultTreeModel nodeTreeModel = new DefaultTreeModel(rootNode);

    private final JTree nodeTree = new JTree(nodeTreeModel);

    private final JScrollPane scrollPane = new JScrollPane(nodeTree);

    private final JPanel panel = new JPanel(new GridBagLayout());

    private final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, panel);

    private final NodeSpecTable nodeSpecTable;

    private final DetailEditor termSpecsEditor;

    private final TerminalEditor terminalEditor;

    private final NodeSpecEditor nodeSpecEditor;

    private final ArgumentsEditor argumentsEditor;

    private final ArgSpecEditor argSpecEditor;

    private final CategoryEditor categoryEditor;

    private DetailEditor currentEditor = null;

    private final List<Category> categories;

    private boolean modified;

    private final TabPanel tabPanel;

    private final Action collapseAction = new MyAction("Collapse All", ImageIcons.collapse, "Collapse All") {

        @Override
        public void actionPerformed(ActionEvent e) {
            expandOrCollapseAll(false, new MyTreePath(rootNode));
        }
    };

    private final Action expandAction = new MyAction("Expand All", ImageIcons.expand, "Expand All") {

        @Override
        public void actionPerformed(ActionEvent e) {
            expandOrCollapseAll(true, new MyTreePath(rootNode));
        }
    };

    private final Action addCategoryAction = new AbstractAction("Add Category") {

        @Override
        public void actionPerformed(ActionEvent e) {
            addCategory();
        }
    };

    private final Action saveAction = new AbstractAction("Save") {

        @Override
        public void actionPerformed(ActionEvent e) {
            save(false);
        }
    };

    private final Action saveAsAction = new AbstractAction("Save As...") {

        @Override
        public void actionPerformed(ActionEvent e) {
            save(true);
        }
    };

    private final TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (currentEditor != null) {
                DetailEditor editor = currentEditor;
                currentEditor = null;
                modified |= editor.saveChanges();
            }
            TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
            if (newLeadSelectionPath == null) {
                return;
            }
            DetailEditor newEditor = null;
            Object lastPathComponent = newLeadSelectionPath.getLastPathComponent();
            if (lastPathComponent instanceof NodeSpecNode) {
                newEditor = nodeSpecEditor;
            } else if (lastPathComponent instanceof TermSpecsNode) {
                newEditor = termSpecsEditor;
            } else if (lastPathComponent instanceof TermSpecNode) {
                newEditor = terminalEditor;
            } else if (lastPathComponent instanceof ArgumentsNode) {
                newEditor = argumentsEditor;
            } else if (lastPathComponent instanceof ArgumentNode) {
                newEditor = argSpecEditor;
            } else if (lastPathComponent instanceof CategoryNode) {
                newEditor = categoryEditor;
            }
            if (newEditor != null) {
                newEditor.setTreeNode((TreeNode) lastPathComponent);
                currentEditor = newEditor;
                panel.validate();
                panel.repaint();
            }
        }
    };

    private final GraphEditorFrame graphEditorFrame;

    /**
     * @param nodeSpecTable
     * @param graphEditorFrame
     */
    public NodeTypeEditor(NodeSpecTable nodeSpecTable, GraphEditorFrame graphEditorFrame) {
        this.graphEditorFrame = graphEditorFrame;
        termSpecsEditor = new TermSpecsEditor(panel, nodeTree, nodeSpecTable);
        terminalEditor = new TerminalEditor(panel, nodeTree, nodeSpecTable);
        nodeSpecEditor = new NodeSpecEditor(panel, nodeTree, nodeSpecTable);
        argumentsEditor = new ArgumentsEditor(panel, nodeTree, nodeSpecTable);
        argSpecEditor = new ArgSpecEditor(panel, nodeTree, nodeSpecTable);
        categoryEditor = new CategoryEditor(panel, nodeTree, nodeSpecTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        scrollPane.setPreferredSize(new Dimension(300, 500));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setPreferredSize(new Dimension(500, 500));
        nodeTree.setRootVisible(false);
        nodeTree.setShowsRootHandles(true);
        this.nodeSpecTable = nodeSpecTable;
        categories = new ArrayList<Category>(nodeSpecTable.getCategories());
        for (Category category : categories) {
            CategoryNode categoryNode = new CategoryNode(category, rootNode);
            List<NodeSpec> nodeSpecs = new ArrayList<NodeSpec>(category.getNodeSpecs());
            for (NodeSpec nodeSpec : nodeSpecs) {
                NodeSpecNode nodeSpecNode = new NodeSpecNode(categoryNode, nodeSpec);
                categoryNode.addNodeSpecNode(nodeSpecNode);
            }
            int[] insertedIndexes = { rootNode.addCategoryNode(categoryNode) };
            nodeTreeModel.nodesWereInserted(rootNode, insertedIndexes);
        }
        nodeTree.addTreeSelectionListener(treeSelectionListener);
        tabPanel = null;
    }

    /**
   *
   */
    protected void addCategory() {
        String inputString = JOptionPane.showInputDialog(null, "Enter category name");
        if (inputString == null || inputString.trim().length() == 0) {
            return;
        }
        inputString = inputString.trim();
        Category category = new Category(inputString);
        CategoryNode categoryNode = new CategoryNode(category, rootNode);
        int[] insertedIndexes = { rootNode.addCategoryNode(categoryNode) };
        nodeTreeModel.nodesWereInserted(rootNode, insertedIndexes);
        TreePath treePath = new MyTreePath(categoryNode);
        nodeTree.setSelectionPath(treePath);
        nodeTree.scrollPathToVisible(treePath);
        nodeSpecTable.addCategory(category);
    }

    /**
     * @return the Component of this Editor
     */
    public Component getComponent() {
        return splitPane;
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#close()
     */
    public void close() {
        while (isModified()) {
            String message = getName() + " has been modified. Save Changes?";
            int option = JOptionPane.showConfirmDialog(getComponent(), message, "Save Node Specification File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (option == JOptionPane.NO_OPTION) {
                break;
            }
            if (option == JOptionPane.YES_OPTION) {
                save(false);
            }
        }
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#getEditorObject()
     */
    public Object getEditorObject() {
        return nodeSpecTable;
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#getName()
     */
    public String getName() {
        return null;
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#getTabPanel()
     */
    public Component getTabPanel() {
        return tabPanel;
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#isModified()
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * @param modified
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#populateEditMenu(javax.swing.JMenu)
     */
    public void populateEditMenu(JMenu editMenu) {
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#populateFileMenu(javax.swing.JMenu)
     */
    public void populateFileMenu(JMenu fileMenu) {
        fileMenu.addSeparator();
        fileMenu.add(saveAction);
        fileMenu.add(saveAsAction);
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#populateToolBar(javax.swing.JToolBar)
     */
    public void populateToolBar(JToolBar toolBar) {
        toolBar.add(collapseAction);
        toolBar.add(expandAction);
        toolBar.add(addCategoryAction);
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#populateViewMenu(javax.swing.JMenu)
     */
    public void populateViewMenu(JMenu viewMenu) {
    }

    /**
     * @see com.bbn.vessel.author.graphEditor.editor.Editor#save(boolean)
     */
    public void save(boolean saveAs) {
        OutputStream outputStream = null;
        if (!saveAs) {
            try {
                URL url = new URL(null);
                outputStream = url.openConnection().getOutputStream();
            } catch (Exception e) {
                outputStream = null;
            }
        }
        if (outputStream == null) {
            JFileChooser fileChooser = graphEditorFrame.getFileChooser();
            int option = fileChooser.showSaveDialog(splitPane);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                tabPanel.setText(file.getName());
                try {
                    outputStream = new FileOutputStream(file);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(splitPane, e);
                }
            } else {
                return;
            }
        }
        try {
            Element rootElement = nodeSpecTable.toXML();
            XMLHelper.write(rootElement, outputStream, null);
            outputStream.close();
            setModified(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(splitPane, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void expandOrCollapseAll(boolean expand, TreePath treePath) {
        TreeNode node = (TreeNode) treePath.getLastPathComponent();
        if (node.getAllowsChildren()) {
            for (Enumeration<TreeNode> e = node.children(); e.hasMoreElements(); ) {
                TreeNode child = e.nextElement();
                expandOrCollapseAll(expand, treePath.pathByAddingChild(child));
            }
        }
        if (node == rootNode) {
            return;
        }
        if (expand) {
            nodeTree.expandPath(treePath);
        } else {
            nodeTree.collapsePath(treePath);
        }
    }
}
