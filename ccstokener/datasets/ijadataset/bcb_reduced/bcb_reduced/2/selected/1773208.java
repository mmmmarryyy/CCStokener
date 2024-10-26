package xslt.debugger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.Writer;
import java.lang.Runnable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import xslt.debugger.Manager;
import xslt.debugger.Utils;

public abstract class AbstractXSLTDebugger implements Runnable {

    public static final int NOT_RUNNING = 1;

    public static final int RUNNING = 2;

    public static final int STOPPED = 3;

    public static final int DO_NOTHING = 0;

    public static final int DO_STOP = 1;

    public static final int DO_STEP = 2;

    public static final int DO_NEXT = 3;

    public static final int DO_FINISH = 4;

    public static final int DO_CONTINUE = 5;

    public static final int DO_DEFERRED_STOP = 6;

    String processorName = null;

    protected HashMap sheetCache = new HashMap();

    protected Manager manager = null;

    protected String xmlFilename;

    protected String xslFilename;

    protected StreamResult result;

    protected String stylesheetId;

    protected Hashtable transformParameters = null;

    protected int state = NOT_RUNNING;

    protected int action = DO_NOTHING;

    public AbstractXSLTDebugger() {
    }

    public synchronized boolean isStarted() {
        return state != NOT_RUNNING;
    }

    public synchronized void stopProcessing() throws InterruptedException {
        action = DO_STOP;
        notifyAll();
        while (state != STOPPED) wait();
    }

    public abstract TransformerFactory getTransformerFactory(boolean forDebug);

    public abstract void prepareTransformerForDebugging(Transformer transformer, boolean forDebug);

    public SAXParserErrorHandler getSAXParserErrorHandler(Manager manager) {
        return new SAXParserErrorHandler(manager);
    }

    public TrAXErrorListener getTrAXErrorListener(Manager manager) {
        return new TrAXErrorListener(manager);
    }

    public synchronized void run() {
        SAXSource saxSource = null;
        manager.getObserver().processorStarted();
        state = RUNNING;
        notifyAll();
        try {
            TransformerFactory tFactory = getTransformerFactory(manager.forDebug);
            tFactory.setErrorListener(getTrAXErrorListener(manager));
            InputSource stylesheetInputSource;
            Source stylesheetSource;
            InputSource xmlSource = new InputSource(new URL(xmlFilename).toString());
            saxSource = new SAXSource(xmlSource);
            setupXMLReader(saxSource);
            if (xslFilename == null) {
                String media = null, title = null, charset = null;
                stylesheetSource = tFactory.getAssociatedStylesheet(saxSource, media, title, charset);
                if (stylesheetSource == null) throw new TransformerConfigurationException("No matching <?xml-stylesheet?> processing instruction found");
                stylesheetInputSource = new InputSource(stylesheetSource.getSystemId());
            } else {
                stylesheetInputSource = new InputSource(new URL(xslFilename).toString());
                stylesheetSource = new SAXSource(stylesheetInputSource);
            }
            SAXSource stylesheetSAXSource = new SAXSource(stylesheetInputSource);
            setupXMLReader(stylesheetSAXSource);
            result = new StreamResult(manager.getOutputStream());
            stylesheetId = stylesheetInputSource.getSystemId();
            XSLTSheetInfo sheetInfo = (XSLTSheetInfo) sheetCache.get(stylesheetId);
            Templates template;
            if (sheetInfo == null) {
                sheetInfo = new XSLTSheetInfo(stylesheetSource);
                sheetCache.put(stylesheetId, sheetInfo);
                template = sheetInfo.template;
            } else {
                template = sheetInfo.getTemplates(stylesheetSource);
            }
            if (template != null) {
                Transformer transformer = template.newTransformer();
                transformer.setErrorListener(getTrAXErrorListener(manager));
                prepareTransformerForDebugging(transformer, manager.forDebug);
                if (transformer != null) {
                    if (transformParameters != null) {
                        for (Enumeration pnames = transformParameters.keys(); pnames.hasMoreElements(); ) {
                            String name = (String) pnames.nextElement();
                            transformer.setParameter(name, (String) transformParameters.get(name));
                        }
                    }
                    transformer.transform(saxSource, result);
                }
            }
        } catch (TransformerException ex) {
            SourceLocator location = ex.getLocator();
            int lineNumber = 0;
            int columnNumber = -1;
            String systemId = saxSource.getSystemId();
            if (location != null) {
                systemId = location.getSystemId();
                lineNumber = location.getLineNumber();
                columnNumber = location.getColumnNumber();
            }
            String message = processErrorMessage(systemId, lineNumber, columnNumber, ex.getMessage());
            ex = new TransformerException(message);
            manager.getObserver().caughtException(ex);
        } catch (SAXParseException ex) {
            String systemId = ex.getSystemId();
            int lineNumber = ex.getLineNumber();
            int columnNumber = ex.getColumnNumber();
            String message = processErrorMessage(systemId, lineNumber, columnNumber, ex.getMessage());
            ex = new SAXParseException(message, null);
            manager.getObserver().caughtException(ex);
        } catch (Exception e) {
            manager.getObserver().caughtException(e);
        }
        manager.processorFinished();
        manager.getObserver().processorFinished();
        state = NOT_RUNNING;
        action = DO_NOTHING;
        notifyAll();
    }

    protected String processErrorMessage(String systemId, int lineNumber, int columnNumber, String originalMessage) {
        if (systemId.startsWith("file:")) systemId = systemId.substring(5);
        String message = originalMessage;
        if (message != null && !message.startsWith(systemId)) {
            message = systemId + ":" + lineNumber;
            if (columnNumber != -1) message += ":" + columnNumber;
            message += ": " + originalMessage;
        }
        return message;
    }

    /**
   * <code>setupXMLReader</code> sets up an ErrorHandler for a given
   * SAXSource object.
   *
   * @param saxSource the <code>SAXSource</code> object
   */
    public void setupXMLReader(SAXSource saxSource) throws SAXException, ParserConfigurationException {
        XMLReader xmlReader = saxSource.getXMLReader();
        if (xmlReader == null) {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            saxSource.setXMLReader(xmlReader);
        }
        xmlReader.setErrorHandler(getSAXParserErrorHandler(manager));
    }

    public synchronized void checkRequestToStop() {
        if (action == DO_STOP) {
            state = STOPPED;
            notifyAll();
        }
    }

    public synchronized void debuggerStopped(String filename, int line, int column, int count, String message) {
        try {
            try {
                Writer writer = result.getWriter();
                if (writer != null) {
                    writer.flush();
                }
            } catch (IOException ioe) {
            }
            state = STOPPED;
            notifyAll();
            manager.observer.debuggerStopped(filename, line, column, count, message);
            while (state != RUNNING) wait();
        } catch (InterruptedException e) {
        }
    }

    public synchronized void doStep() {
        state = RUNNING;
        action = DO_STEP;
        notifyAll();
    }

    public synchronized void doNext() {
        state = RUNNING;
        action = DO_NEXT;
        notifyAll();
    }

    public synchronized void doFinish() {
        state = RUNNING;
        action = DO_FINISH;
        notifyAll();
    }

    public synchronized void doContinue() {
        state = RUNNING;
        action = DO_CONTINUE;
        notifyAll();
    }

    public abstract ArrayList getGlobalVariables();

    public Value getValueOfGlobalVariable(String name) throws NoSuchFieldException {
        ArrayList variables = getGlobalVariables();
        Variable variable = null;
        if (variables == null) throw new NoSuchFieldException();
        for (int i = 0; i < variables.size(); i++) {
            Variable var = (Variable) variables.get(i);
            String localName = var.getName();
            if (localName.equals(name)) {
                variable = var;
                break;
            }
        }
        if (variable == null) throw new NoSuchFieldException();
        return variable.getValue();
    }

    /**
   * <code>getAction</code> returns the action to be taken next.
   *
   * @return an <code>int</code> value
   */
    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public Manager getManager() {
        return manager;
    }

    public void setXMLFilename(String filename) {
        xmlFilename = filename;
    }

    public void setXSLTStylesheet(String filename) {
        xslFilename = filename;
    }

    public void setProcessorName(String name) {
        this.processorName = name;
    }

    public void setTransformParameters(Hashtable transformParameters) {
        this.transformParameters = transformParameters;
    }

    public String getProcessorName() {
        return processorName;
    }

    public String getStylesheetId() {
        return stylesheetId;
    }

    /**
   * <code>XSLTSheetInfo</code> is used by {@link
   * AbstractXSLTDebugger} to maintain cached templates.
   */
    class XSLTSheetInfo {

        Templates template = null;

        long lastModified = Long.MIN_VALUE;

        public XSLTSheetInfo(Source stylesheetSource) throws TransformerConfigurationException {
            TransformerFactory tFactory = getTransformerFactory(manager.forDebug);
            template = tFactory.newTemplates(stylesheetSource);
            lastModified = getLastModified(stylesheetSource.getSystemId());
        }

        long getLastModified(String stylesheetId) {
            long lastModified = Long.MIN_VALUE;
            URL url = null;
            try {
                url = new URL(stylesheetId);
                if (url.getProtocol().equals("file")) {
                    File file = new File(url.getFile());
                    lastModified = file.lastModified();
                } else {
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    lastModified = conn.getLastModified();
                }
            } catch (MalformedURLException e) {
                System.err.println("Invalid URL " + url + ": " + e.toString());
            } catch (IOException e) {
                System.err.println("Cannot access " + url + ": " + e.toString());
            }
            return lastModified;
        }

        public Templates getTemplates(Source stylesheetSource) throws TransformerConfigurationException {
            String stylesheetId = stylesheetSource.getSystemId();
            long currentTime = Long.MIN_VALUE;
            currentTime = getLastModified(stylesheetId);
            if (template != null && currentTime <= lastModified) {
                return template;
            }
            TransformerFactory tFactory = getTransformerFactory(manager.forDebug);
            template = tFactory.newTemplates(stylesheetSource);
            lastModified = currentTime;
            return template;
        }
    }
}
