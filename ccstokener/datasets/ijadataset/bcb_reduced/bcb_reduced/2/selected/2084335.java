package org.fudaa.fudaa.commun.aide;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.memoire.bu.BuBorderLayout;
import com.memoire.bu.BuEditorPane;
import com.memoire.bu.BuLib;
import com.memoire.bu.BuPanel;
import com.memoire.bu.BuScrollPane;
import com.memoire.bu.BuSplit2Pane;
import com.memoire.bu.BuTree;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.CtuluLib;

/**
 * @author Fred Deniger
 * @version $Id: FudaaHelpHtmlPanel.java,v 1.4 2007-05-04 13:58:05 deniger Exp $
 */
public final class FudaaHelpHtmlPanel extends BuPanel implements TreeSelectionListener {

    private static class TocHandler implements ContentHandler {

        int currentDepth_;

        DefaultMutableTreeNode currentFile_;

        DefaultMutableTreeNode root_;

        final URL base_;

        /**
     * @param _base
     */
        public TocHandler(final URL _base) {
            super();
            base_ = _base;
        }

        private DefaultMutableTreeNode createFromAtt(final Attributes _atts) {
            String href = _atts.getValue("href");
            if (href == null) {
                href = _atts.getValue("topic");
            }
            try {
                return new DefaultMutableTreeNode(new TreeUserObject(_atts.getValue("label"), new URL(base_ + href)));
            } catch (final MalformedURLException _e) {
                FuLog.warning(_e);
                return null;
            }
        }

        DefaultTreeModel getTreeModel() {
            if (root_ != null) {
                return new DefaultTreeModel(root_);
            }
            return null;
        }

        TreePath getHome() {
            if (root_ != null) {
                return new TreePath(new Object[] { root_.getChildAt(0) });
            }
            return null;
        }

        public void characters(final char[] _ch, final int _start, final int _length) throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void endElement(final String _namespaceURI, final String _localName, final String _name) throws SAXException {
            currentDepth_--;
        }

        public void endPrefixMapping(final String _prefix) throws SAXException {
        }

        public void ignorableWhitespace(final char[] _ch, final int _start, final int _length) throws SAXException {
        }

        public void processingInstruction(final String _target, final String _data) throws SAXException {
        }

        public void setDocumentLocator(final Locator _locator) {
        }

        public void skippedEntity(final String _name) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startElement(final String _namespaceURI, final String _localName, final String _name, final Attributes _atts) throws SAXException {
            if (root_ == null) {
                root_ = createFromAtt(_atts);
                root_.add(createFromAtt(_atts));
            } else if (currentDepth_ == 1) {
                currentFile_ = createFromAtt(_atts);
                root_.add(currentFile_);
            } else if (currentDepth_ == 2 && currentFile_ != null) {
                currentFile_.add(createFromAtt(_atts));
            }
            currentDepth_++;
        }

        public void startPrefixMapping(final String _prefix, final String _uri) throws SAXException {
        }
    }

    /**
   * Utiliser pour identifier les documents.
   * 
   * @author Fred Deniger
   * @version $Id: FudaaHelpHtmlPanel.java,v 1.4 2007-05-04 13:58:05 deniger Exp $
   */
    private static class TreeUserObject {

        String label_;

        URL url_;

        /**
     * @param _label
     * @param _url
     */
        public TreeUserObject(final String _label, final URL _url) {
            super();
            label_ = _label;
            url_ = _url;
        }

        public final String getLabel() {
            return label_;
        }

        public final URL getUrl() {
            return url_;
        }

        public String toString() {
            return label_;
        }
    }

    URL base_;

    FudaaHelpPane pane_;

    JTree trIndex_;

    TreePath home_;

    public FudaaHelpHtmlPanel(final URL _base, final FudaaHelpParentI _parent) {
        base_ = _base;
        buildComponents(_parent);
    }

    protected BuEditorPane getEditorPane() {
        return pane_.getEditorPane();
    }

    public boolean isCopyEnable() {
        return pane_.getEditorPane().getSelectionEnd() != pane_.getEditorPane().getSelectionStart();
    }

    protected boolean isHttpBase() {
        return base_ != null && "http".equals(base_.getProtocol());
    }

    protected void buildComponents(final FudaaHelpParentI _parent) {
        setLayout(new BuBorderLayout());
        try {
            buildTree();
        } catch (final Exception _e) {
            FuLog.warning(_e);
        }
        pane_ = new FudaaHelpPane(_parent, isHttpBase());
        pane_.setPreferredSize(new Dimension(350, 450));
        final BuScrollPane scroll = new BuScrollPane(pane_);
        if (trIndex_ == null) {
            add(scroll, BuBorderLayout.CENTER);
        } else {
            add(new BuSplit2Pane(new BuScrollPane(trIndex_), scroll), BuBorderLayout.CENTER);
        }
        if (trIndex_ != null && trIndex_.getModel().getChildCount(trIndex_.getModel().getRoot()) > 0) {
            goHome();
        }
    }

    public TreeNode getChapter(final TreeNode _node) {
        final Object root = trIndex_.getModel().getRoot();
        if (_node == null || _node == root) {
            return null;
        }
        if (_node.getParent() == root) {
            return _node;
        }
        return getChapter(_node.getParent());
    }

    public boolean isNextChapterAvailable(final boolean _do) {
        final TreeNode n = getChapter((DefaultMutableTreeNode) trIndex_.getLastSelectedPathComponent());
        if (n != null) {
            final TreeNode root = (TreeNode) trIndex_.getModel().getRoot();
            final int idx = root.getIndex(n);
            final boolean r = idx >= 0 && ((idx + 1) < root.getChildCount());
            if (r && _do) {
                trIndex_.setSelectionPath(new TreePath(new Object[] { root, root.getChildAt(idx + 1) }));
            }
            return r;
        }
        return false;
    }

    public boolean isBackChapterAvailable(final boolean _do) {
        final TreeNode n = getChapter((DefaultMutableTreeNode) trIndex_.getLastSelectedPathComponent());
        if (n != null) {
            final TreeNode root = (TreeNode) trIndex_.getModel().getRoot();
            final int idx = root.getIndex(n);
            final boolean r = idx > 0;
            if (r && _do) {
                trIndex_.setSelectionPath(new TreePath(new Object[] { root, root.getChildAt(idx - 1) }));
            }
            return r;
        }
        return false;
    }

    public void setUrlBase(final URL _base) throws Exception {
        if (!isSameToc(_base.toExternalForm())) {
            base_ = _base;
            pane_.setHttp(isHttpBase());
            buildTree();
            goHome();
        }
    }

    protected void buildTree() throws Exception {
        if (trIndex_ == null) {
            trIndex_ = new BuTree(new DefaultTreeModel(new DefaultMutableTreeNode("Chargement ...")));
            trIndex_.setPreferredSize(new Dimension(150, 450));
            trIndex_.setRootVisible(false);
            trIndex_.setShowsRootHandles(true);
            trIndex_.setFont(BuLib.deriveFont("Tree", Font.PLAIN, -2));
            trIndex_.addTreeSelectionListener(this);
        }
        new Thread() {

            public void run() {
                final TreeModel model = createIndex();
                BuLib.invokeLater(new Runnable() {

                    public void run() {
                        if (model == null) {
                            trIndex_.setVisible(false);
                        } else {
                            trIndex_.setVisible(true);
                            trIndex_.setModel(model);
                        }
                    }
                });
            }
        }.start();
    }

    String lastToc_;

    protected TreeModel createIndex() {
        final TocHandler handler = new TocHandler(base_);
        URL url = null;
        try {
            url = getToc();
        } catch (final MalformedURLException _e1) {
            FuLog.warning(_e1);
            return null;
        }
        if (url == null) {
            return null;
        }
        lastToc_ = url.toExternalForm();
        InputStream io = null;
        try {
            io = url.openStream();
            final SAXParserFactory parser = SAXParserFactory.newInstance();
            parser.setNamespaceAware(false);
            parser.setValidating(false);
            final SAXParser saxparser = parser.newSAXParser();
            final XMLReader reader = saxparser.getXMLReader();
            reader.setContentHandler(handler);
            final InputSource s = new InputSource(io);
            s.setEncoding("UTF-8");
            saxparser.getXMLReader().parse(s);
            home_ = handler.getHome();
        } catch (final Exception _e) {
            FuLog.warning(_e);
        } finally {
            try {
                if (io != null) {
                    io.close();
                }
            } catch (final IOException _e) {
                FuLog.warning(_e);
            }
        }
        return handler.getTreeModel();
    }

    public void goHome() {
        if (trIndex_ != null) {
            trIndex_.setSelectionPath(home_);
        }
    }

    boolean updatingTree_;

    protected void setDocumentUrl(final URL _u, final String _title, final boolean _forceReload) {
        pane_.setContent(_u, _title, _forceReload);
        if (trIndex_ == null) {
            return;
        }
        final DefaultMutableTreeNode n = (DefaultMutableTreeNode) trIndex_.getLastSelectedPathComponent();
        final URL url = n == null ? null : ((TreeUserObject) n.getUserObject()).getUrl();
        if (_u == null) return;
        if (url == null || !url.toExternalForm().equals(_u.toExternalForm())) {
            updatingTree_ = true;
            final TreePath path = findTreePath(_u);
            if (path == null) {
                trIndex_.clearSelection();
            } else {
                trIndex_.setSelectionPath(path);
                trIndex_.scrollPathToVisible(path);
            }
            updatingTree_ = false;
        }
    }

    TreePath findTreePath(final URL _url) {
        if (trIndex_ == null) {
            return null;
        }
        final DefaultTreeModel model = (DefaultTreeModel) trIndex_.getModel();
        if (model.getRoot() == null) {
            return null;
        }
        final TreeNode[] select = getNodeWith(model, (DefaultMutableTreeNode) model.getRoot(), _url);
        if (select != null) {
            return new TreePath(select);
        }
        return null;
    }

    public TreeNode[] getNodeWith(final DefaultTreeModel _model, final DefaultMutableTreeNode _parent, final URL _url) {
        if (trIndex_ == null) {
            return null;
        }
        if (_parent.getParent() != null && getUrlFor(_parent).toString().equals(_url.toExternalForm())) {
            return _model.getPathToRoot(_parent);
        }
        final int nb = _model.getChildCount(_parent);
        for (int i = 0; i < nb; i++) {
            final TreeNode[] nds = getNodeWith(_model, (DefaultMutableTreeNode) _parent.getChildAt(i), _url);
            if (nds != null) {
                return nds;
            }
        }
        return null;
    }

    public URL getUrlFor(final DefaultMutableTreeNode _node) {
        if (trIndex_.getModel().getRoot() == null) {
            return null;
        }
        return ((TreeUserObject) _node.getUserObject()).getUrl();
    }

    protected void setSource(final String _htmlSource, final String _title) {
        pane_.setContent(null, _htmlSource, _title, true);
    }

    URL getToc() throws MalformedURLException {
        return getToc(base_.toExternalForm());
    }

    static URL getToc(final String _helpdir) throws MalformedURLException {
        String local = "en";
        if (CtuluLib.isFrenchLanguageSelected()) {
            local = "fr";
        }
        String deb = _helpdir;
        if (!deb.endsWith("/")) {
            deb += '/';
        }
        return new URL(deb + "toc." + local + ".xml");
    }

    public boolean isSameToc(final String _helpDir) throws MalformedURLException {
        return lastToc_ != null && lastToc_.equals(getToc(_helpDir).toExternalForm());
    }

    public void back() {
        pane_.back();
    }

    public void copy() {
        pane_.copy();
    }

    public void forward() {
        pane_.forward();
    }

    public void reload() {
        pane_.reload();
    }

    public void showProxy() {
        pane_.showProxy();
    }

    public void valueChanged(final TreeSelectionEvent _evt) {
        if (updatingTree_) {
            return;
        }
        final DefaultMutableTreeNode n = (DefaultMutableTreeNode) trIndex_.getLastSelectedPathComponent();
        if (n != null) {
            final TreeUserObject obj = (TreeUserObject) n.getUserObject();
            pane_.setContent(obj.getUrl(), obj.getLabel(), true);
        }
    }
}
