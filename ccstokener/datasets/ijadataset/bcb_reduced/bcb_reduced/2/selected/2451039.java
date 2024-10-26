package vxmlsurfer;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Stack;
import org.w3c.dom.Document;

public class Prefetcher extends Thread {

    private Stack<DocumentWrapper> docStack = new Stack<DocumentWrapper>();

    private Stack<ArrayList<URIElement>> pendingStack = new Stack<ArrayList<URIElement>>();

    private boolean incoming = false;

    private boolean CACHING = false;

    private synchronized DocumentWrapper getFromDocumentStack() {
        if (docStack.isEmpty()) {
            try {
                wait();
            } catch (Exception e) {
            }
        }
        incoming = false;
        return docStack.pop();
    }

    public synchronized void addToDocumentStack(Document doc, String baseURL) {
        System.out.println("Adding for prefetching: " + baseURL);
        docStack.push(new DocumentWrapper(doc, baseURL));
        incoming = true;
        if (docStack.size() == 1) {
            notify();
        }
    }

    public void processDocument() {
        DocumentWrapper vxmlDom = getFromDocumentStack();
        System.out.println("Prefectcer :: processing document");
        ArrayList<URIElement> URIElementList = URIElementCreator.createURIElements(vxmlDom.doc, vxmlDom.baseURL);
        System.out.println("No of uri elements fetched = " + URIElementList.size());
        pendingStack.push(URIElementList);
        processURIElements();
    }

    private void processURIElements() {
        while (!pendingStack.isEmpty()) {
            ArrayList<URIElement> list = pendingStack.pop();
            int i;
            for (i = 0; i < list.size() && !incoming; i++) {
                URIElement currURIElement = list.get(i);
                processURIElement(currURIElement);
            }
            if (incoming && list.size() > 0) {
                pendingStack.push(new ArrayList<URIElement>(list.subList(i, list.size() - 1)));
                incoming = false;
                break;
            }
        }
    }

    private void processURIElement(URIElement element) {
        String uriString = element.getURI();
        FileManager.writeToLog("Prefetcher looking up -> " + element.toString());
        if (!FileManager.isPresent(uriString)) {
            FileManager.writeToLog("... Prefetching");
            try {
                URL url = new URL(uriString);
                URLConnection uc = url.openConnection();
                InputStream is = uc.getInputStream();
                FileManager.add(uriString, element, is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            FileManager.writeToLog("... No need to prefetch");
        }
    }

    public void setCaching(boolean caching) {
        this.CACHING = caching;
    }

    public synchronized void flushStack() {
        incoming = false;
        docStack.clear();
        pendingStack.clear();
        FileManager.flushCache();
    }

    public void run() {
        while (true) {
            processDocument();
            if (docStack.isEmpty() && !pendingStack.isEmpty()) {
                processURIElements();
            }
        }
    }
}
