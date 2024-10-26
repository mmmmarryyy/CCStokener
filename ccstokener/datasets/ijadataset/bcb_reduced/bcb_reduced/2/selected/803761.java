package com.f3p.openoffice;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import com.f3p.commons.FileStreamUtils;
import com.f3p.commons.XercesUtils;
import com.f3p.dataprovider.DataProvider;
import com.f3p.dataprovider.DataProviderConfig;
import com.f3p.dataprovider.DataProviderConnection;
import com.f3p.dataprovider.DataProviderException;
import com.f3p.dataprovider.ParamProvider;
import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.archive.zip.JarDriver;

public class DocMerge {

    private static final Log s_log = LogFactory.getLog(DocMerge.class);

    private static final String URI_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0", TAG_TABLE = "table:table", TAG_ROW = "table:table-row", TAG_FIELD = "text:database-display", TAG_BODY = "office:body", TAG_PARAGRAPH = "text:p", TAG_PARAGRAPH_LOCAL = "p", ATTRIB_TABLE = "text:table-name", ATTRIB_COLUMN = "text:column-name", ATTRIB_TABLENAME = "table:name", TABLE_TITLE_DONTREPLICATE = "DMNR_";

    private DataProviderConfig m_dataProviderConfig;

    private ParamProvider m_paramProvider;

    public DocMerge(DataProviderConfig dtc, ParamProvider pd) {
        m_dataProviderConfig = dtc;
        m_paramProvider = pd;
    }

    public File mergeDoc(URL urlDoc, File fOutput, boolean bMulti) throws Exception {
        if (s_log.isTraceEnabled()) trace(0, "Copying from " + urlDoc.toString() + " to " + fOutput.toString());
        File fOut = null;
        InputStream is = null;
        try {
            is = urlDoc.openStream();
            fOut = mergeDoc(is, fOutput, bMulti);
        } finally {
            is.close();
        }
        return fOut;
    }

    public File mergeDoc(InputStream cis, File fOutput, boolean bMulti) throws Exception {
        if (fOutput.exists()) {
            File fTmp = new File(fOutput.getAbsolutePath());
            fTmp.delete();
            fTmp = null;
            System.gc();
            if (fOutput.exists()) {
                s_log.error(fOutput.toString() + " non cancellato !!!");
            }
        }
        java.io.FileOutputStream fos = new java.io.FileOutputStream(fOutput);
        FileStreamUtils.transferStream(cis, fos);
        fos.close();
        de.schlichtherle.io.File content = new de.schlichtherle.io.File(fOutput, "content.xml", new DefaultArchiveDetector(DefaultArchiveDetector.DEFAULT, "odt", new JarDriver()));
        InputStream is = new de.schlichtherle.io.FileInputStream(content);
        Document xmldoc = XercesUtils.getDocument(is);
        is.close();
        Element docroot = xmldoc.getDocumentElement();
        NodeList nodes = docroot.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node root = nodes.item(i);
            if (root instanceof Element && root.getNodeName().equals(TAG_BODY)) {
                recursiveMerge(new ElementContext(xmldoc, (Element) root, null), 0);
            }
        }
        OutputStream os = new de.schlichtherle.io.FileOutputStream(content);
        XercesUtils.writeNode(xmldoc, os);
        os.close();
        de.schlichtherle.io.File.umount();
        return fOutput;
    }

    private void recursiveMerge(ElementContext context, int iLevel) throws DataProviderException {
        boolean bRelase = context.isRelease();
        if (s_log.isTraceEnabled()) trace(iLevel++, "Begin recursive merge for " + context.getElement().getNodeName());
        try {
            List<ElementContext> tableContexts = new ArrayList<ElementContext>();
            Node root = context.getElement();
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String tagName = node.getNodeName().toLowerCase();
                if (node instanceof Element) {
                    Element nodeElement = (Element) node;
                    boolean bRecurseOnNode = true;
                    if (tagName.equals(TAG_TABLE)) {
                        String sName = nodeElement.getAttribute(ATTRIB_TABLENAME);
                        if (sName.startsWith(TABLE_TITLE_DONTREPLICATE) == false) {
                            bRecurseOnNode = false;
                            tableContexts.add(context.buildChild((Element) node));
                        }
                    } else if (tagName.equals(TAG_FIELD)) {
                        String sTable = nodeElement.getAttribute(ATTRIB_TABLE), sColumn = nodeElement.getAttribute(ATTRIB_COLUMN);
                        String sVal = "";
                        DataProvider dp = context.getProvider(sTable, iLevel);
                        if (!dp.isExhausted()) {
                            sVal = dp.getFormatted((sColumn).toString());
                            if (s_log.isTraceEnabled()) trace(iLevel, "Reading value: " + sColumn + " from " + dp.getName() + " [" + dp.hashCode() + "] = " + sVal);
                        }
                        String sValAsPara[] = OOUtils.splitIntoParagraphBlocks(sVal);
                        Node afterPara = node.getPreviousSibling();
                        Node insertBefore = root.getNextSibling();
                        Text nodeFirstLine = context.createTextNode(sValAsPara[0]);
                        root.replaceChild(nodeFirstLine, node);
                        if (sValAsPara.length > 1) {
                            for (int ii = 1; ii < sValAsPara.length; ii++) {
                                Node nodePara = context.createParagraphNode(root);
                                Text nodeLine = context.createTextNode(sValAsPara[ii]);
                                if (ii == sValAsPara.length - 1 && afterPara != null) {
                                    nodeLine.appendData(afterPara.getTextContent());
                                    root.removeChild(afterPara);
                                }
                                nodePara.appendChild(nodeLine);
                                root.getParentNode().insertBefore(nodePara, insertBefore);
                            }
                        }
                        bRecurseOnNode = false;
                    }
                    if (node.hasChildNodes() && bRecurseOnNode) {
                        context.setRelease(false);
                        context.setElement(nodeElement);
                        recursiveMerge(context, iLevel + 1);
                    }
                }
            }
            for (ElementContext tableContext : tableContexts) processTable(tableContext, iLevel);
        } finally {
            context.setRelease(bRelase);
            context.release();
        }
        if (s_log.isTraceEnabled()) trace(--iLevel, "End recursive merge");
    }

    private void processTable(ElementContext context, int iLevel) throws DataProviderException {
        if (s_log.isTraceEnabled()) trace(iLevel++, "Begin processing table: " + context.getElement().getAttribute("table:name") + " " + context.getElement().hashCode());
        context.setTableRoot(true);
        List<ElementContext> rows = new ArrayList<ElementContext>();
        try {
            List<Node> stack = new ArrayList<Node>();
            stack.add(context.getElement());
            while (stack.size() > 0) {
                Node root = stack.remove(0);
                NodeList nodes = root.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    String tagName = node.getNodeName().toLowerCase();
                    if (node instanceof Element) {
                        if (tagName.equals(TAG_TABLE)) {
                        } else if (tagName.equals(TAG_FIELD)) {
                            Node thisRow = node.getParentNode();
                            boolean bPresent = false;
                            while (!thisRow.getNodeName().toLowerCase().equals(TAG_ROW)) thisRow = thisRow.getParentNode();
                            for (ElementContext row : rows) {
                                if (row.getElement().equals(thisRow)) {
                                    bPresent = true;
                                    break;
                                }
                            }
                            if (!bPresent) rows.add(context.buildChild((Element) thisRow));
                        } else {
                            if (node.hasChildNodes()) stack.add(node);
                        }
                    }
                }
            }
            while (rows.size() > 0) {
                ElementContext rowContext = rows.get(0);
                Element originalRow = rowContext.getElement();
                Node parent = originalRow.getParentNode();
                do {
                    Node newRow = originalRow.cloneNode(true);
                    rowContext.setElement((Element) newRow);
                    if (s_log.isTraceEnabled()) trace(iLevel, "Istancing row");
                    recursiveMerge(rowContext, iLevel + 1);
                    if (rowContext.getNewDataProviderHadNext()) parent.insertBefore(newRow, originalRow);
                } while (rowContext.next(iLevel) && rowContext.getNewDataProviderHadNext());
                rowContext.release();
                parent.removeChild(originalRow);
                rows.remove(0);
            }
        } finally {
            for (ElementContext row : rows) row.release();
            context.release();
        }
        if (s_log.isTraceEnabled()) trace(--iLevel, "End processing table " + context.getElement().getAttribute("table:name"));
    }

    protected void trace(int iLevel, String sString) {
        if (s_log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < iLevel; i++) sb.append(' ');
            sb.append(sString);
            s_log.trace(sb.toString());
        }
    }

    class ElementContext {

        private Element m_element;

        private ElementContext m_parent;

        private List<DataProvider> m_lstDataProviders;

        private boolean m_bNewDataProviderHadNext;

        private boolean m_bIsTableRoot;

        private boolean m_bRelease;

        private Document m_document;

        private ElementContext(Document doc, Element element, ElementContext parent) {
            m_element = element;
            m_parent = parent;
            if (parent != null && parent.m_bIsTableRoot) m_lstDataProviders = parent.m_lstDataProviders; else m_lstDataProviders = new ArrayList<DataProvider>();
            m_bNewDataProviderHadNext = true;
            m_bIsTableRoot = false;
            m_document = doc;
            m_bRelease = true;
        }

        public Text createTextNode(String sText) {
            if (sText != null && sText.length() > 0) return m_document.createTextNode(sText); else return m_document.createTextNode("");
        }

        public Node createParagraphNode(Node model) {
            Node nodePara = null;
            if (model != null && model.getNamespaceURI().equals(URI_TEXT) && model.getLocalName().equals(TAG_PARAGRAPH_LOCAL)) {
                nodePara = model.cloneNode(false);
            } else nodePara = m_document.createElementNS(URI_TEXT, "text:p");
            return nodePara;
        }

        public void setTableRoot(boolean bRoot) {
            m_bIsTableRoot = bRoot;
        }

        public String getValue(String sField) {
            return null;
        }

        public Element getElement() {
            return m_element;
        }

        public void setElement(Element e) {
            m_element = e;
        }

        public ElementContext buildChild(Element elm) {
            return new ElementContext(m_document, elm, this);
        }

        public void release() throws DataProviderException {
            if ((m_parent == null || !m_parent.m_bIsTableRoot) && m_bRelease) {
                for (DataProvider dp : m_lstDataProviders) {
                    dp.release();
                }
            }
        }

        public DataProvider getRelatedProvider(String sTable) throws DataProviderException {
            DataProvider ret = null;
            if (m_lstDataProviders.size() == 0) {
                if (m_parent != null) ret = m_parent.getRelatedProvider(sTable);
            } else {
                for (DataProvider dp : m_lstDataProviders) {
                    ret = dp.getRelation(sTable, m_paramProvider);
                    if (ret != null) {
                        break;
                    }
                }
            }
            return ret;
        }

        public DataProvider getProvider(String sTable, int iLevel) throws DataProviderException {
            return getProvider(sTable, false, iLevel);
        }

        public DataProvider getProvider(String sTable, boolean bDontInstance, int iLevel) throws DataProviderException {
            DataProvider ret = null;
            for (DataProvider dp : m_lstDataProviders) {
                if (dp.getName().equals(sTable)) {
                    if (s_log.isTraceEnabled()) trace(iLevel, "Obtained provider for " + sTable + " (already known)");
                    ret = dp;
                    break;
                }
            }
            if (ret == null && m_parent != null) {
                ret = m_parent.getRelatedProvider(sTable);
                if (ret != null) {
                    if (s_log.isTraceEnabled()) trace(iLevel, "Obtained provider for " + sTable + " (related)");
                    m_lstDataProviders.add(ret);
                    m_bNewDataProviderHadNext = ret.next();
                }
            }
            if (ret == null) {
                if (m_parent != null) {
                    ret = m_parent.getProvider(sTable, true, iLevel);
                    if (s_log.isTraceEnabled() && ret != null) trace(iLevel + 1, "Obtained provider for " + sTable + " (from parent)");
                }
            }
            if (ret == null) {
                for (DataProvider dp : m_lstDataProviders) {
                    ret = dp.getRelation(sTable, m_paramProvider);
                    if (ret != null) {
                        if (s_log.isTraceEnabled()) trace(iLevel, "Obtained provider for " + sTable + " (related to context some level)");
                        m_lstDataProviders.add(ret);
                        m_bNewDataProviderHadNext = ret.next();
                        break;
                    }
                }
            }
            if (bDontInstance) {
                if (s_log.isTraceEnabled()) trace(iLevel, "No provider found, and asked not to instance (" + sTable + ")");
                return null;
            }
            if (ret == null) {
                DataProviderConnection dpc = m_dataProviderConfig.getConnection(sTable);
                if (dpc != null) {
                    Set<String> params = dpc.getMetadata().getParameterNames();
                    for (String param : params) {
                        Object obj = m_paramProvider.getParam(param, dpc);
                        if (obj != null) dpc.setGenericParam(param, obj);
                    }
                    ret = dpc.getData();
                    if (ret != null) {
                        if (s_log.isTraceEnabled()) trace(iLevel, "Obtained provider for " + sTable + " (created)");
                        m_lstDataProviders.add(ret);
                        m_bNewDataProviderHadNext = ret.next();
                    }
                }
            }
            if (ret == null) trace(iLevel, "Failed to obtain provider for " + sTable);
            return ret;
        }

        public boolean getNewDataProviderHadNext() {
            return m_bNewDataProviderHadNext;
        }

        public boolean next(int iLevel) throws DataProviderException {
            boolean bRes = (m_lstDataProviders.size() > 0);
            for (DataProvider dp : m_lstDataProviders) {
                if (!dp.next()) {
                    if (s_log.isTraceEnabled()) trace(iLevel, "Next on " + dp.getName() + ": false");
                    bRes = false;
                    break;
                }
                if (s_log.isTraceEnabled()) trace(iLevel, "Next on " + dp.getName() + ": true");
            }
            if (s_log.isTraceEnabled() && m_lstDataProviders.size() == 0) {
                trace(iLevel, "Next called on empty DataProvider list ");
            }
            return bRes;
        }

        public boolean isRelease() {
            return m_bRelease;
        }

        public void setRelease(boolean release) {
            m_bRelease = release;
        }
    }
}
