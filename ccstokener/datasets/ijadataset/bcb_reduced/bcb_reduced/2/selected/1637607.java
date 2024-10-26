package com.memoire.bu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Locale;
import java.util.Stack;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import com.memoire.fu.FuLib;

/**
 * A tiny HTML browser for on-line help.
 * Is an internal frame on the desktop and an alternative
 * to an external browser as lynx ;-)
 */
public class BuHelpFrame extends BuBrowserFrame implements TreeSelectionListener {

    private BuTree trIndex_;

    private URL base_;

    public BuHelpFrame() {
        this(null);
    }

    public BuHelpFrame(BuCommonImplementation _app) {
        super(_app);
        setName("ifAIDE");
        if (app_ != null) {
            app_.installContextHelp(getRootPane(), "bu/p-aide.html");
            try {
                base_ = new URL(app_.getInformationsSoftware().man);
                URL url = new URL(base_, "index-aide.txt");
                String ll = Locale.getDefault().getLanguage();
                String u = url.toString();
                if (!ll.equals("fr")) {
                    ll = "en";
                    int i = u.lastIndexOf(".txt");
                    if (i >= 0) u = u.substring(0, i) + "-" + ll + u.substring(i);
                }
                System.err.println("BHF: index url is " + u);
                url = new URL(u);
                trIndex_ = new BuTree();
                trIndex_.setModel(createIndex(url));
                trIndex_.setRootVisible(false);
                trIndex_.setShowsRootHandles(true);
                trIndex_.setCellRenderer(createRenderer());
                trIndex_.setFont(BuLib.deriveFont("Tree", Font.PLAIN, -2));
                trIndex_.addTreeSelectionListener(this);
                container_.remove(pane_);
                BuScrollPane spt = new BuScrollPane(trIndex_);
                BuSplit2Pane s2p = new BuSplit2Pane(spt, pane_);
                container_.add(s2p, BuBorderLayout.CENTER);
            } catch (Exception ex) {
            }
        }
        setShortcut(KeyEvent.VK_F1);
        setFrameIcon(BuResource.BU.getFrameIcon("aide"));
    }

    protected String getTitleBase() {
        return _("Aide");
    }

    protected TreeCellRenderer createRenderer() {
        return new BuAbstractCellRenderer(BuAbstractCellRenderer.TREE) {

            public Component getTreeCellRendererComponent(JTree _tree, Object _value, boolean _selected, boolean _expanded, boolean _leaf, int _row, boolean _focus) {
                JLabel r = (JLabel) super.getTreeCellRendererComponent(_tree, _value, _selected, _expanded, _leaf, _row, _focus);
                String t = r.getText();
                int i = t.indexOf('|');
                if (i >= 0) {
                    t = t.substring(0, i).trim();
                    r.setText(t);
                    if (!_selected) r.setForeground(Color.blue);
                }
                return r;
            }
        };
    }

    public void valueChanged(TreeSelectionEvent _evt) {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) trIndex_.getLastSelectedPathComponent();
        if (n != null) {
            String u = (String) n.getUserObject();
            if (u != null) {
                int i = u.indexOf('|');
                if (i >= 0) {
                    u = u.substring(i + 1);
                    if ((base_ != null) && !u.startsWith("http:/") && !u.startsWith("file:/")) u = base_ + u;
                    setDocumentUrl(u);
                }
            }
            trIndex_.setSelectionPath(null);
        }
    }

    protected TreeModel createIndex(URL _url) throws Exception {
        LineNumberReader rin = new LineNumberReader(new InputStreamReader(_url.openStream()));
        int ps = -1;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(_("Index"));
        Stack stack = new Stack();
        stack.push(root);
        while (true) {
            String l = rin.readLine();
            if (l == null) break;
            l = FuLib.replace(l, "\t", "        ");
            int ns = 0;
            while ((ns < l.length()) && (l.charAt(ns) == ' ')) ns++;
            if (ns == l.length()) continue;
            ns /= 2;
            l = l.trim();
            if ("".equals(l)) continue;
            if (ns > ps) {
                ns = ps + 1;
                DefaultMutableTreeNode pn = (DefaultMutableTreeNode) stack.peek();
                DefaultMutableTreeNode cn = new DefaultMutableTreeNode(l);
                pn.add(cn);
                stack.push(cn);
            } else if (ns == ps) {
                stack.pop();
                if (stack.isEmpty()) break;
                DefaultMutableTreeNode pn = (DefaultMutableTreeNode) stack.peek();
                DefaultMutableTreeNode cn = new DefaultMutableTreeNode(l);
                pn.add(cn);
                stack.push(cn);
            } else if (ns < ps) {
                while (ns <= ps) {
                    stack.pop();
                    if (stack.isEmpty()) break;
                    ps--;
                }
                DefaultMutableTreeNode pn = (DefaultMutableTreeNode) stack.peek();
                DefaultMutableTreeNode cn = new DefaultMutableTreeNode(l);
                pn.add(cn);
                stack.push(cn);
            }
            ps = ns;
        }
        rin.close();
        return new DefaultTreeModel(root);
    }
}
