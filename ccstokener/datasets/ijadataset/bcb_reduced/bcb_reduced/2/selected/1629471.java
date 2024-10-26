package edu.mit.lcs.haystack.ozone.web;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import edu.mit.lcs.haystack.server.extensions.weboperation.IWebOpDocCompleteListener;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.OLEUtils;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		Andrew Hogue
 */
public class InternetExplorer implements IWebBrowser, IDOMBrowser {

    IEOleControlSite m_site;

    OleAutomation m_oa;

    OleFrame m_frame;

    HashSet m_navigateListeners = new HashSet();

    HashSet m_selectionListeners = new HashSet();

    HashSet m_webOperationListeners = new HashSet();

    HashSet m_webOpDocCompleteEventListeners = new HashSet();

    DOMSelection m_selection = null;

    Timer m_selectionTimer = new Timer();

    boolean m_loading = false;

    IDOMDocument m_document;

    boolean m_entireDocLoaded = false;

    boolean m_webOpOccurred = false;

    public InternetExplorer(Composite parent) {
        this(parent, null);
    }

    public InternetExplorer(Composite parent, MouseListener contextMenuListener) {
        m_frame = new OleFrame(parent, SWT.NONE);
        m_site = new IEOleControlSite(m_frame, SWT.NONE, "Shell.Explorer", contextMenuListener);
        m_site.doVerb(OLE.OLEIVERB_SHOW);
        m_frame.setVisible(true);
        m_oa = new OleAutomation(m_site);
        m_site.addEventListener(0xfc, new OleListener() {

            public void handleEvent(OleEvent event) {
                m_loading = false;
                Iterator i = m_navigateListeners.iterator();
                while (i.hasNext()) {
                    IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener) i.next();
                    wbnl.navigateComplete();
                }
            }
        });
        m_site.addEventListener(259, new OleListener() {

            public void handleEvent(OleEvent event) {
                String url = event.arguments[1].getString();
                if (url.indexOf(getLocationURL()) != -1) {
                    m_entireDocLoaded = true;
                    Iterator i = m_webOpDocCompleteEventListeners.iterator();
                    while (i.hasNext()) {
                        System.out.println("Document Complete");
                        IWebOpDocCompleteListener wodcl = (IWebOpDocCompleteListener) i.next();
                        wodcl.documentComplete(getDocument(), url);
                    }
                } else {
                    m_entireDocLoaded = false;
                }
                Iterator i = m_navigateListeners.iterator();
                while (i.hasNext()) {
                    IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener) i.next();
                    wbnl.documentComplete(url);
                }
            }
        });
        m_site.addEventListener(0xfa, new OleListener() {

            public void handleEvent(OleEvent event) {
                String strHeaders = new String();
                if (event.arguments[5] != null) {
                    Variant varHeaders = event.arguments[5];
                    strHeaders = varHeaders.getString();
                }
                String strPostData = new String();
                if (event.arguments[4] != null) {
                    strPostData = OLEUtils.readSafeArray(event.arguments[4]);
                }
                String strURL = new String();
                if (event.arguments[1] != null) {
                    strURL = event.arguments[1].getString();
                }
                if (!strPostData.equals("") || isGet(strURL)) {
                    m_webOpOccurred = true;
                    Iterator iWebOperation = m_webOperationListeners.iterator();
                    while (iWebOperation.hasNext()) {
                        IWebOperationListener wol = (IWebOperationListener) iWebOperation.next();
                        try {
                            wol.onWebOperation(strURL, strHeaders, strPostData);
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    m_webOpOccurred = false;
                }
                Iterator iNavigate = m_navigateListeners.iterator();
                while (iNavigate.hasNext()) {
                    IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener) iNavigate.next();
                    wbnl.beforeNavigate(strURL);
                }
            }
        });
        m_site.addEventListener(102, new OleListener() {

            public void handleEvent(OleEvent event) {
                if (m_loading) {
                    String s = event.arguments[0].getString();
                    Iterator i = m_navigateListeners.iterator();
                    while (i.hasNext()) {
                        IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener) i.next();
                        wbnl.statusTextChange(s);
                    }
                }
            }
        });
        m_site.addEventListener(108, new OleListener() {

            public void handleEvent(OleEvent event) {
                if (m_loading) {
                    Iterator i = m_navigateListeners.iterator();
                    while (i.hasNext()) {
                        IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener) i.next();
                        wbnl.progressChange(event.arguments[0].getInt(), event.arguments[1].getInt());
                    }
                }
            }
        });
    }

    /**
     * @see IWebBrowser#navigate(String)
     */
    public void navigate(String url) {
        int[] rgdispid = m_oa.getIDsOfNames(new String[] { "Navigate" });
        int dispIdMember = rgdispid[0];
        m_loading = true;
        Variant varUrl = new Variant(url);
        Variant pVarResult = m_oa.invoke(dispIdMember, new Variant[] { varUrl });
        varUrl.dispose();
        pVarResult.dispose();
    }

    /**
     * @see IDOMBrowser#getURL()
     */
    public String getURL() {
        return getLocationURL();
    }

    public String getLocationName() {
        int[] rgdispid = m_oa.getIDsOfNames(new String[] { "LocationName" });
        int dispIdMember = rgdispid[0];
        return m_oa.getProperty(dispIdMember).getString();
    }

    public String getLocationURL() {
        int[] rgdispid = m_oa.getIDsOfNames(new String[] { "LocationURL" });
        int dispIdMember = rgdispid[0];
        return m_oa.getProperty(dispIdMember).getString();
    }

    /**
     * @see IWebBrowser#getControl()
     * @see IDOMBrowser#getControl()
     */
    public Control getControl() {
        return m_frame;
    }

    /**
     * @see IWebBrowser#addNavigateListener(IWebBrowserNavigateListener)
     */
    public void addNavigateListener(IWebBrowserNavigateListener wbnl) {
        m_navigateListeners.add(wbnl);
    }

    /**
     * @see IWebBrowser#removeNavigateListener(IWebBrowserNavigateListener)
     */
    public void removeNavigateListener(IWebBrowserNavigateListener wbnl) {
        m_navigateListeners.remove(wbnl);
    }

    /**
     * @see IWebBrowser#addWebOpDocCompleteEventListeners(IWebOpDocCompleteListener)
     */
    public void addWebOpDocCompleteEventListeners(IWebOpDocCompleteListener wodcel) {
        m_webOpDocCompleteEventListeners.add(wodcel);
    }

    /**
     * @see IWebBrowser#removeWebOpDocCompleteEventListeners(IWebOpDocCompleteListener)
     */
    public void removeWebOpDocCompleteEventListeners(IWebOpDocCompleteListener wodcel) {
        m_webOpDocCompleteEventListeners.remove(wodcel);
    }

    public void addSelectionListener(IWebBrowserSelectionListener wbsl) {
        if (m_selectionListeners.size() <= 0) {
            m_selectionTimer.scheduleAtFixedRate(new TimerTask() {

                public void run() {
                    DOMSelection selection = m_document.getSelection(false);
                    if (selection != null && !selection.equals(m_selection)) {
                        m_selection = selection;
                        Iterator selectionIterator = m_selectionListeners.iterator();
                        while (selectionIterator.hasNext()) {
                            IWebBrowserSelectionListener currwbsl = (IWebBrowserSelectionListener) selectionIterator.next();
                            WebBrowserSelectionEvent event = new WebBrowserSelectionEvent();
                            event.selection = selection;
                            currwbsl.onSelectionChange(event);
                        }
                    }
                }
            }, 0, 1000);
        }
        m_selectionListeners.add(wbsl);
    }

    public void removeSelectionListener(IWebBrowserSelectionListener wbsl) {
        m_selectionListeners.remove(wbsl);
        if (m_selectionListeners.size() <= 0) {
            m_selectionTimer.cancel();
        }
    }

    /**
     *  Returns the current Document object for this automation
     *
     *  @see IDOMBrowser#getDocument()
     */
    public IDOMDocument getDocument() {
        if (m_oa == null) {
            m_document = null;
        } else if (m_document == null) {
            m_document = new IEDOMDocument(m_oa);
        }
        return m_document;
    }

    protected void finalize() {
        if (this.m_oa != null) this.m_oa.dispose();
    }

    /**
     *  Creates an IDOMDocument without bringing up an actual Browser
     *  control on screen.
     */
    public static IDOMDocument parseURL(String url) {
        String docString = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            BufferedReader docReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer contents = new StringBuffer();
            String currLine = null;
            while ((currLine = docReader.readLine()) != null) {
                contents.append(currLine);
            }
            docString = contents.toString();
            docReader.close();
        } catch (IOException e) {
            System.out.println("Exception in InternetExplorer.parseURL(" + url + "):");
            e.printStackTrace();
        }
        return parseHTML(docString);
    }

    /**
     *  Creates an IDOMDocument without bringing up an actual Browser
     *  control on screen.
     */
    public static IDOMDocument parseHTML(String html) {
        Shell shell = new Shell();
        shell.setLayout(new GridLayout());
        InternetExplorer browser = new InternetExplorer(shell);
        browser.getControl().setLayoutData(new GridData());
        browser.navigate("about:blank");
        IDOMDocument doc = browser.getDocument();
        doc.write(html);
        return doc;
    }

    public static void invokeWebOperation(String url, String headers, String postData, IWebOpDocCompleteListener wodcel) {
        InternetExplorer browser = null;
        IDOMDocument returnDoc = null;
        try {
            Shell shell = new Shell();
            shell.setLayout(new GridLayout());
            browser = new InternetExplorer(shell);
            browser.getControl().setLayoutData(new GridData());
            browser.addWebOpDocCompleteEventListeners(wodcel);
            int[] httpArgs = browser.m_oa.getIDsOfNames(new String[] { "Navigate", "URL", "Headers", "Postdata" });
            int dispIdMember = httpArgs[0];
            int params = 1;
            if (headers != null && !headers.equals("")) params++;
            if (postData != null && !postData.equals("")) params++;
            Variant[] paramVals = new Variant[params];
            int[] paramNames = new int[params];
            paramNames[0] = httpArgs[1];
            paramVals[0] = new Variant(url);
            int currParam = 1;
            if (headers != null && !headers.equals("")) {
                paramNames[currParam] = httpArgs[2];
                paramVals[currParam] = new Variant(headers);
                currParam++;
            }
            if (postData != null && !postData.equals("")) {
                paramNames[currParam] = httpArgs[3];
                paramVals[currParam] = OLEUtils.makeSafeArray(postData);
            }
            browser.m_oa.invoke(dispIdMember, paramVals, paramNames);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void addWebOperationListener(IWebOperationListener wol) {
        m_webOperationListeners.add(wol);
    }

    public void removeWebOperationListener(IWebOperationListener wol) {
        m_webOperationListeners.remove(wol);
    }

    public boolean isGet(String url) {
        boolean bReturn = false;
        boolean getStructured = isGetStructured(url);
        if (getStructured && this.m_entireDocLoaded) {
            bReturn = true;
        }
        return bReturn;
    }

    public boolean isGetStructured(String url) {
        boolean getStructured = true;
        int indexQuestion = url.indexOf("?");
        if (indexQuestion != -1) {
            String urlNoQuestion = url.substring(indexQuestion + 1);
            String[] andSplit = urlNoQuestion.split("&");
            for (int i = 0; i < andSplit.length; i++) {
                String[] equalsSplit = andSplit[i].split("=");
                if (equalsSplit.length != 2 && equalsSplit.length != 1) {
                    getStructured = false;
                    break;
                } else if (equalsSplit.length == 1 && !andSplit[i].endsWith("=")) {
                    getStructured = false;
                    break;
                }
            }
        } else {
            getStructured = false;
        }
        return getStructured;
    }

    public void navigate(String url, String headers, String postData) {
        int[] httpArgs = this.m_oa.getIDsOfNames(new String[] { "Navigate", "URL", "Headers", "Postdata" });
        int dispIdMember = httpArgs[0];
        int params = 1;
        if (headers != null && !headers.equals("")) params++;
        if (postData != null && !postData.equals("")) params++;
        Variant[] paramVals = new Variant[params];
        int[] paramNames = new int[params];
        paramNames[0] = httpArgs[1];
        paramVals[0] = new Variant(url);
        int currParam = 1;
        if (headers != null && !headers.equals("")) {
            paramNames[currParam] = httpArgs[2];
            paramVals[currParam] = new Variant(headers);
            currParam++;
        }
        if (postData != null && !postData.equals("")) {
            paramNames[currParam] = httpArgs[3];
            paramVals[currParam] = OLEUtils.makeSafeArray(postData);
        }
        m_loading = true;
        this.m_oa.invoke(dispIdMember, paramVals, paramNames);
    }

    public boolean getWebOpOccurred() {
        return m_webOpOccurred;
    }
}
