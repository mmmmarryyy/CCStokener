package org.web3d.x3d.palette.items;

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
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.openide.filesystems.FileObject;

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
public class ExternProtoDeclareSyncHelper2 {

    public static interface SyncStatusListener {

        public void statusIn(String s);

        public void checkDone(HashMap<String, Object> hm, String info);
    }

    ;

    public static String PROTODECL_APPINFO_KEY = "____appinfo";

    public static String PROTODECL_DOCUMENTATION_KEY = "____documentation";

    private HashMap<String, Object> externHM;

    private SyncStatusListener listener;

    public Thread checkThread;

    public ExternProtoDeclareSyncHelper2(FileObject masterDocLocation, String url, SyncStatusListener lis) {
        listener = lis;
        checkThread = new Thread(new Checker(masterDocLocation, url), "ExternProtoDeclareSyncHelper2Thread");
        checkThread.setPriority(Thread.NORM_PRIORITY);
        checkThread.start();
    }

    class Checker implements Runnable {

        private FileObject masterDocLocation;

        private String urlStr;

        Checker(FileObject masterDocLocation, String s) {
            urlStr = s;
            this.masterDocLocation = masterDocLocation;
        }

        public void run() {
            String err = "Error";
            try {
                setStatus("Loading from selected url...");
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(getUrlContents(urlStr));
                setStatus("Reading xml...");
                processExtern(doc, urlStr);
                checkThread = null;
                return;
            } catch (MalformedURLException ex) {
                err = "Bad URL";
            } catch (JDOMException jex) {
                err = jex.getLocalizedMessage();
            } catch (IOException iex) {
                err = iex.getLocalizedMessage();
            } catch (Exception ex) {
                System.out.println("bp");
            }
            setDone(null, err);
            checkThread = null;
        }

        /**
   * From UrlStatus.java
   * @param urlStr
   * @return
   */
        private Reader getUrlContents(String urlStr) throws Exception {
            URL urlObj = UrlExpandableList2.buildUrl(masterDocLocation, urlStr);
            URLConnection uConn = urlObj.openConnection();
            InputStream inStr = new BufferedInputStream(uConn.getInputStream());
            return new InputStreamReader(inStr);
        }
    }

    private void processExtern(Document doc, String urlStr) {
        String anchor = "";
        Element protoIF = null;
        if (urlStr.indexOf('#') != -1) anchor = urlStr.substring(urlStr.lastIndexOf('#') + 1, urlStr.length());
        Element protoDecl = findProtoDeclare(doc, anchor);
        if (protoDecl != null) {
            HashMap<String, Object> myHM = new HashMap<String, Object>();
            Attribute appInfoAttr = protoDecl.getAttribute("appinfo");
            if (appInfoAttr != null) myHM.put(PROTODECL_APPINFO_KEY, appInfoAttr.getValue());
            Attribute docAttr = protoDecl.getAttribute("documentation");
            if (docAttr != null) myHM.put(PROTODECL_DOCUMENTATION_KEY, docAttr.getValue());
            protoIF = protoDecl.getChild("ProtoInterface");
            if (protoIF != null) {
                List fieldList = protoIF.getChildren("field");
                for (Object o : fieldList) {
                    Element el = (Element) o;
                    Attribute name = el.getAttribute("name");
                    myHM.put(name.getValue(), el);
                }
            }
            externHM = myHM;
        }
        if (externHM != null) {
            if (externHM.isEmpty()) setDone(externHM, "Comparison complete, no ProtoDeclare appinfo/documentation or ProtoInterface field definitions found in external file"); else if (protoIF == null) setDone(externHM, "Comparison complete, no ProtoInterface field definitions found in external file"); else setDone(externHM, "Comparison complete");
        } else setDone(null, "Error: ProtoDeclare not found in external file");
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

    private void setDone(HashMap<String, Object> hm, String s) {
        if (listener != null) listener.checkDone(hm, s);
    }

    private void setStatus(final String s) {
        if (listener != null) listener.statusIn(s);
    }
}
