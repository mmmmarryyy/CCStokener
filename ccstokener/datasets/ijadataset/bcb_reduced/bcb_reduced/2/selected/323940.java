package edu.mit.lcs.haystack.ozone.web;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.mozilla.nsIDOMEventTarget;
import org.eclipse.swt.internal.mozilla.nsIDOMNode;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.w3c.dom.NodeList;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IViewPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartCache;
import edu.mit.lcs.haystack.ozone.web.IWebBrowser;
import edu.mit.lcs.haystack.ozone.web.IWebBrowserNavigateListener;
import edu.mit.lcs.haystack.ozone.web.IWebOperationListener;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.weboperation.WebOpManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Pattern;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.PatternResult;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.mozilla.MozDOMElement;

/**
 * Default view that displays the resource using a web browser.
 * 
 * @author Dennis Quan
 * @author Andrew Hogue
 */
public class WebViewPart extends ControlPart implements IViewPart, IBlockGUIHandler, IWebBrowserNavigateListener, MouseListener, Listener, IWebOperationListener {

    public static final Resource s_part = new Resource(OzoneConstants.s_namespace + "webViewPart");

    public static final String NAMESPACE = "http://haystack.lcs.mit.edu/ui/progress#";

    public static final Resource ADD_PROGRESS_ITEM = new Resource(NAMESPACE + "addProgressItem");

    public static final Resource REMOVE_PROGRESS_ITEM = new Resource(NAMESPACE + "removeProgressItem");

    public static final Resource WEB_PROGRESS_ITEM = new Resource(NAMESPACE + "webProgressItem");

    public static final Resource WEB_DESTINATION_TEXT = new Resource(NAMESPACE + "webDestinationText");

    public static final Resource WEB_STATUS_TEXT = new Resource(NAMESPACE + "webStatusText");

    public static final Resource WEB_PROGRESS_TEXT = new Resource(NAMESPACE + "webProgressText");

    public static final Resource TEXT = new Resource(NAMESPACE + "text");

    protected IWebBrowser m_webBrowser;

    protected HashMap currentMatches;

    protected Interpreter m_interpreter;

    protected DynamicEnvironment m_denv;

    protected String m_currentOpURL;

    protected String m_currentOpHeaders;

    protected String m_currentOpPostData;

    protected String m_initiatingURL;

    protected INode[] m_initDoc;

    static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(WebViewPart.class);

    public WebViewPart() {
        this.currentMatches = new HashMap();
    }

    public void navigate(Resource res) {
        IRDFContainer rdfc = m_denv.getSource();
        try {
            RDFNode type = rdfc.extract(res, Constants.s_rdf_type, null);
            if (type != null && type.getContent().equals(WebOpManager.VISIBLE_WEB_OPERATION.getContent())) {
                RDFNode headers = rdfc.extract(res, WebOpManager.VISIBLE_WEB_OPERATION_HEADERS, null);
                RDFNode url = rdfc.extract(res, WebOpManager.VISIBLE_WEB_OPERATION_URL, null);
                RDFNode postData = rdfc.extract(res, WebOpManager.VISIBLE_WEB_OPERATION_POSTDATA, null);
                m_webBrowser.navigate(url.getContent(), headers.getContent(), postData.getContent());
            } else {
                System.out.println("WebViewPart.navigate(" + res + ")");
                String uri = res.getURI();
                if (!uri.startsWith("http://")) {
                    try {
                        File file = ContentClient.getContentClient(res, m_infoSource, m_context.getServiceAccessor()).getContentAsFile();
                        if (file != null) {
                            uri = file.toURL().toString();
                        }
                        System.out.println(">> " + uri + " " + file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                m_webBrowser.navigate(uri);
            }
        } catch (RDFException e1) {
            e1.printStackTrace();
        }
    }

    /**
	 * @see IWebBrowserNavigateListener#beforeNavigate(String)
	 */
    public void beforeNavigate(String url) {
        if (url.toLowerCase().startsWith("urn:")) {
            ((IBrowserWindow) m_context.getProperty(OzoneConstants.s_browserWindow)).navigate(new Resource(url), null, true);
            return;
        }
        try {
            new Thread() {

                void run(String url) {
                    m_url = url;
                    start();
                }

                String m_url;

                public void run() {
                    try {
                        HttpURLConnection urlc = (HttpURLConnection) (new URL(m_url).openConnection());
                        urlc.setRequestMethod("HEAD");
                        urlc.connect();
                        String contentType = urlc.getContentType();
                        if (contentType != null && Utilities.getLiteralProperty(new Resource(m_url), Constants.s_dc_format, m_source) == null) {
                            int semicolon = contentType.indexOf(';');
                            if (semicolon != -1) {
                                contentType = contentType.substring(0, semicolon);
                            }
                            m_infoSource.add(new Statement(new Resource(m_url), Constants.s_dc_format, new Literal(contentType)));
                        }
                    } catch (Exception e) {
                        s_logger.error("Failed to get head info", e);
                    }
                }
            }.run(url);
            m_source.remove(new Statement(WEB_DESTINATION_TEXT, TEXT, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
            m_source.add(new Statement(WEB_DESTINATION_TEXT, TEXT, new Literal(url)));
        } catch (RDFException e) {
        }
        try {
            m_interpreter.callMethod(ADD_PROGRESS_ITEM, new Object[] { WEB_PROGRESS_ITEM }, m_denv);
        } catch (AdenineException e) {
            s_logger.error("Failed to add web progress item", e);
        }
    }

    /**
	 * @see IWebBrowserNavigateListener#navigateComplete()
	 */
    public void navigateComplete() {
        String url = m_webBrowser.getLocationURL();
        if (url != null && url.startsWith("http://")) {
            m_resUnderlying = new Resource(url);
            try {
                m_infoSource.add(new Statement(m_resUnderlying, Constants.s_rdf_type, Constants.s_web_WebPage));
                m_infoSource.add(new Statement(m_resUnderlying, Constants.s_rdf_type, Constants.s_content_HttpContent));
                PartCache.clearCache(m_context);
            } catch (RDFException e) {
            }
        }
        IViewContainerPart vcp = (IViewContainerPart) m_context.getProperty(OzoneConstants.s_viewContainer);
        if (vcp != null) {
            vcp.onNavigateComplete(m_resUnderlying, this);
        }
    }

    public void documentComplete(String url) {
        System.out.println("WebViewPart.documentComplete(" + url + ")");
        String browserURL = m_webBrowser.getLocationURL();
        try {
            if (browserURL != null && browserURL.startsWith("http://")) {
                String name = m_webBrowser.getLocationName();
                if (!name.equals(browserURL)) {
                    if (Utilities.getLiteralProperty(m_resUnderlying, Constants.s_dc_title, m_source) == null) {
                        try {
                            m_infoSource.add(new Statement(m_resUnderlying, Constants.s_dc_title, new Literal(name)));
                        } catch (RDFException e) {
                        }
                    }
                }
            }
            m_interpreter.callMethod(REMOVE_PROGRESS_ITEM, new Object[] { WEB_PROGRESS_ITEM }, m_denv);
        } catch (AdenineException e) {
            s_logger.error("Failed to process documentComplete event", e);
        }
        if (url.indexOf(browserURL) >= 0) {
            try {
                Resource[] patternRes = WrapperManager.getPatternResources(m_source, url);
                this.currentMatches.clear();
                for (int i = 0; i < patternRes.length; i++) {
                    Pattern currPat = Pattern.fromResource(patternRes[i], m_source);
                    PatternResult currRes = currPat.match(getDOMBrowser().getDocument());
                    currRes.highlight(this);
                    this.currentMatches.put(patternRes[i], currRes);
                }
                if (patternRes.length > 0) {
                    m_source.add(m_resUnderlying, Constants.s_rdf_type, WrapperManager.WRAPPED_PAGE_CLASS);
                }
            } catch (RDFException e) {
                System.out.println("Error highlighting patterns in ErgoBrowser.navigateComplete():\n");
                e.printStackTrace();
            }
        }
    }

    /**
	 * @see IWebBrowserNavigateListener#progressChange(int, int)
	 */
    public void progressChange(int progress, int progressMax) {
        String s = "(" + (((float) progress) * 100 / ((float) progressMax)) + "%)";
        try {
            m_source.remove(new Statement(WEB_PROGRESS_TEXT, TEXT, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
            m_source.add(new Statement(WEB_PROGRESS_TEXT, TEXT, new Literal(s)));
        } catch (RDFException e) {
        }
    }

    /**
	 * @see IWebBrowserNavigateListener#statusTextChange(String)
	 */
    public void statusTextChange(String status) {
        try {
            m_source.remove(new Statement(WEB_STATUS_TEXT, TEXT, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
            m_source.add(new Statement(WEB_STATUS_TEXT, TEXT, new Literal(status != null ? status : "(finishing)")));
        } catch (RDFException e) {
        }
    }

    /**
	 * Does the actual initialization work.
	 */
    protected void internalInitialize() {
        super.internalInitialize();
        this.m_currentOpURL = new String();
        this.m_currentOpHeaders = new String();
        this.m_currentOpPostData = new String();
        this.m_initDoc = null;
        m_interpreter = Ozone.getInterpreter();
        m_denv = new DynamicEnvironment(m_source);
        Ozone.initializeDynamicEnvironment(m_denv, m_context);
        Composite parent = (Composite) m_context.getSWTControl();
        if (System.getProperty("os.name").indexOf("Windows") == 0) {
            try {
                m_webBrowser = (IWebBrowser) CoreLoader.loadClass("edu.mit.lcs.haystack.ozone.web.InternetExplorer").getConstructors()[0].newInstance(new Object[] { parent, this });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                m_webBrowser = (IWebBrowser) CoreLoader.loadClass("edu.mit.lcs.haystack.ozone.web.Mozilla").getConstructors()[0].newInstance(new Object[] { parent, this });
            } catch (Exception e) {
                e.printStackTrace();
                e.getCause().printStackTrace();
            }
        }
        m_control = m_webBrowser.getControl();
        m_webBrowser.addNavigateListener(this);
        m_webBrowser.addWebOperationListener(this);
        if (m_resUnderlying == null && m_prescription != null) {
            m_resUnderlying = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_target, m_source);
        }
        if (m_resUnderlying != null) {
            navigate(m_resUnderlying);
        }
    }

    /**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
    public void dispose() {
        super.dispose();
    }

    /**
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(MouseEvent)
	 */
    public void mouseDoubleClick(MouseEvent e) {
    }

    /**
	 * Implemented to receive context menu requests from browser widget.
	 * 
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(MouseEvent)
	 */
    public void mouseDown(MouseEvent e) {
        if (e.button == 3) {
            try {
                Point b = m_webBrowser.getControl().toControl(new Point(e.x, e.y));
                IDOMElement clicked = ((IDOMBrowser) m_webBrowser).getDocument().getElementAtPoint(b.x, b.y);
                Context clickedContext = WrapperManager.getClickContext(m_infoSource, this, clicked, m_context);
                PartUtilities.showContextMenu(m_infoSource, clickedContext, new Point(e.x, e.y));
            } catch (RDFException ee) {
                ee.printStackTrace();
            }
        }
    }

    /**
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(MouseEvent)
	 */
    public void mouseUp(MouseEvent e) {
    }

    public IDOMBrowser getDOMBrowser() {
        return (IDOMBrowser) this.m_webBrowser;
    }

    public void setWebBrowser(IWebBrowser webBrowser) {
        this.m_webBrowser = webBrowser;
    }

    public Resource getUnderlyingResource() {
        return m_resUnderlying;
    }

    /**
	 * Returns a mapping from Pattern objects to PatternResult objects for the
	 * current page.
	 */
    public HashMap getCurrentMatches() {
        return this.currentMatches;
    }

    public void addMatch(Resource patternRes, PatternResult result) {
        this.currentMatches.put(patternRes, result);
    }

    public void clearMatches() {
        this.currentMatches.clear();
    }

    public PatternResult clearMatch(Resource patternRes) {
        if (this.currentMatches.containsKey(patternRes)) {
            System.out.println("%%% WebViewPart.clearMatch(): containsKey");
            return (PatternResult) this.currentMatches.remove(patternRes);
        } else {
            System.out.println("%%% found no match");
            return null;
        }
    }

    public void handleEvent(Event event) {
        if (event != null && event.type == SWT.MenuDetect) {
            try {
                int[] aRet = new int[1];
                nsIDOMEventTarget target = new nsIDOMEventTarget(((Integer) event.data).intValue());
                target.QueryInterface(nsIDOMNode.NS_IDOMNODE_IID, aRet);
                nsIDOMNode nsNode = new nsIDOMNode(aRet[0]);
                IDOMDocument doc = ((IDOMBrowser) m_webBrowser).getDocument();
                IDOMElement clicked = new MozDOMElement(doc, nsNode);
                Context clickedContext = WrapperManager.getClickContext(m_infoSource, this, clicked, m_context);
                PartUtilities.showContextMenu(m_infoSource, clickedContext, new Point(event.x, event.y));
            } catch (RDFException ee) {
                ee.printStackTrace();
            }
        }
    }

    public void onWebOperation(String url, String headers, String postData) {
        this.m_currentOpURL = url;
        this.m_currentOpHeaders = headers;
        this.m_currentOpPostData = postData;
        this.m_initiatingURL = this.getDOMBrowser().getURL();
        IDOMDocument doc = m_webBrowser.getDocument();
        if (doc != null) {
            NodeList nl = doc.getDocumentElement().getElementsByTagName("form");
            this.m_initDoc = new INode[nl.getLength()];
            for (int i = 0; i < nl.getLength(); i++) {
                this.m_initDoc[i] = ((IDOMElement) nl.item(i)).copy();
            }
        }
    }

    /**
	 * @return Returns the m_currentOpHeaders.
	 */
    public String getCurrentOpHeaders() {
        return m_currentOpHeaders;
    }

    /**
	 * @return Returns the m_currentOpPostData.
	 */
    public String getCurrentOpPostData() {
        return m_currentOpPostData;
    }

    /**
	 * @return Returns the m_currentOpURL.
	 */
    public String getCurrentOpURL() {
        return m_currentOpURL;
    }

    /**
	 * @return Returns the m_initDoc.
	 */
    public INode[] getInitDoc() {
        return m_initDoc;
    }

    /**
	 * @return Returns the m_initiatingURL.
	 */
    public String getInitiatingURL() {
        return m_initiatingURL;
    }

    public boolean getWebOpOccurred() {
        return m_webBrowser.getWebOpOccurred();
    }
}
