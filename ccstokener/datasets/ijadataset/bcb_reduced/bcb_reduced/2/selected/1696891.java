package org.streets.commons.xml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.streets.commons.error.NestedRuntimeException;

/**
 * <b>Simple</b> and <b>fast</b> importer for XML configuration or import files. <br>
 * <br>
 * It is based on SAX and can be considered an extension to it. This means it is
 * callback oriented and does not build an internal data structure like the DOM.
 * While SAX is simple, fast, and memory friendly it might be a bit too
 * rudimentary for most tasks. <code>SAXImportParser</code> adds more high level
 * means for importing XML while preserving the SAX's benefits. <br>
 * <br>
 * As with SAX you register a callback handler ({@link SAXImportHandler})
 * that is called upon events. Consider the following example implementation
 * of a {@link SAXImportHandler}:<br><br>
 * <code><pre>
 * public class DemoHandler implements SAXImportHandler {
 * public void startDocument() { }
 * public void endDocument() { }
 *
 * public void cData(SimplePath path, String cdata) { }
 *
 * public void startElement(SimplePath path, String name, AttributesImpl attributes, String leadingCDdata) {
 * &nbsp;&nbsp;if (path.matches("/root/interesting-element")) {
 * &nbsp;&nbsp;&nbsp;&nbsp;System.out.println(leadingCDdata);
 * &nbsp;&nbsp;}
 * }
 * public void endElement(SimplePath path, String name) { }
 *
 * }
 * </pre></code>
 *
 * Registering this class with {@link #addSAXImportHandler} and call
 * {@link #parse} on an input stream or {@link #parseUrlOrFile} will dump
 * the leading text of the element matching the path ({@link SimplePath})
 * "/root/interesting-element".<br>
 * <br>
 * <em>Note</em>: This class is thread safe.
 *
 */
public class SAXImportParser {

    private boolean trimContent = true;

    private boolean makeCopy = false;

    private boolean zeroLengthIsNull = true;

    private boolean isCallbackCDataOnStartElement = true;

    private boolean fullDebug = false;

    private boolean useQName = true;

    private boolean buildComplexPath = false;

    protected SAXParserFactory factory;

    protected List callbackHandlerList = new ArrayList();

    protected StringBuffer currentMixedPCData = null;

    protected boolean foundMixedPCData = false;

    protected StringBuffer firstPCData = null;

    protected boolean isFirstPCData = true;

    protected ParseElement currentElement = null;

    protected PathStack parseStack = new PathStack();

    protected String debugBuffer = null;

    /** Creates a new SAXImportParser object having default property settings. It is recommended
     * to set all properties explicitly for clearity.
     */
    public SAXImportParser() {
        factory = SAXParserFactory.newInstance();
    }

    /** Determines if we have found any mixed content while parsing. */
    public boolean getFoundMixedPCData() {
        return foundMixedPCData;
    }

    /**
     * Determines if the path shall be assembled of the full qualified names. <code>true</code> is the default.
     */
    public boolean getUseQName() {
        return useQName;
    }

    /**
     * Sets if the path shall be assembled of the full qualified names. <code>true</code> is the default.
     */
    public void setUseQName(boolean useQName) {
        this.useQName = useQName;
    }

    /**
     * Determines if the simple path created will have complex additional info.
     */
    public boolean getBuildComplexPath() {
        return buildComplexPath;
    }

    /**
     * Sets if the simple path created will have complex additional info.
     */
    public void setBuildComplexPath(boolean buildComplexPath) {
        this.buildComplexPath = buildComplexPath;
    }

    /** Sets the full debug mode which enables us to get the parsed stream
     * as string via the {@link #getParsedStreamForDebug()}
     * method even if an error occured.
     */
    public void setFullDebugMode(boolean fullDebug) {
        this.fullDebug = fullDebug;
    }

    /** Gets the property described in
     * {@link #setFullDebugMode}.
     */
    public boolean getFullDebugMode() {
        return fullDebug;
    }

    /** Gets the whole stream parsed in the {@link #parse} method. As this requires some actions
     * significantly slowing down the whole parse, this only works if it has been enabled
     * by the the {@link #setFullDebugMode} method.
     */
    public String getParsedStreamForDebug() {
        if (!getFullDebugMode()) {
            return null;
        } else {
            return debugBuffer;
        }
    }

    /** Gets property telling importer to return any leading CDATA, i.e.
     * CDATA directly following a start tag before any other tagging,
     * along with the start element
     * method. If set to false leading CDATA will be returned using method
     * {@link SAXImportHandler#cData} just like any CDATA in a mixed
     * content. <br>
     *
     * @see SAXImportHandler#startElement
     * @see #setIncludeLeadingCDataIntoStartElementCallback
     */
    public boolean isCallbackCDataOnStartElement() {
        return isCallbackCDataOnStartElement;
    }

    /** Sets the property described in
     * {@link #getIncludeLeadingCDataIntoStartElementCallback}.
     */
    public void setIsCallbackCDataOnStartElement(boolean isCallbackCDataOnStartElement) {
        this.isCallbackCDataOnStartElement = isCallbackCDataOnStartElement;
    }

    /** Sets the property described in
     * {@link #setTrimContent}.
     */
    public boolean getTrimContent() {
        return trimContent;
    }

    /** Sets when all content shall be trimed.
     * If set in conjunction with {@link #setZeroLengthIsNull} all whitespace data will not be
     * reported to callback handlers.
     */
    public void setTrimContent(boolean trimContent) {
        this.trimContent = trimContent;
    }

    /** Gets property: When findind zero length content should it be treated as null data?
     * If it is treated as null data nothing is reported to handlers when finding zero length data.
     */
    public boolean getZeroLengthIsNull() {
        return zeroLengthIsNull;
    }

    /** Sets the property described in
     * {@link #getZeroLengthIsNull}.
     */
    public void setZeroLengthIsNull(boolean zeroLengthIsNull) {
        this.zeroLengthIsNull = zeroLengthIsNull;
    }

    /** Gets the property describing if every callback handler gets a fresh copy of the parsed data.
     * This is only important when there is more than one callback handler. If so and it is not set,
     * all handlers will get <em>identical</em> objects. This is bad if you expect them to change any
     * of that data.
     */
    public boolean getMakeCopy() {
        return makeCopy;
    }

    /** Sets the property described in {@link #getMakeCopy}. */
    public void setMakeCopy(boolean makeCopy) {
        this.makeCopy = makeCopy;
    }

    /** Adds a new callback handler if it is not in the callback list, yet.
     * This can be dynamically done while parsing.
     * @see #removeImportHandler
     */
    public void addImportHandler(SAXImportHandler callbackHandler) {
        synchronized (callbackHandlerList) {
            if (!callbackHandlerList.contains(callbackHandler)) {
                callbackHandlerList.add(callbackHandler);
            }
        }
    }

    /** Removes a callback handler if it is in the callback list.
     * This can be dynamically done while parsing.
     * @see #addImportHandler
     */
    public void removeImportHandler(SAXImportHandler callbackHandler) {
        synchronized (callbackHandlerList) {
            callbackHandlerList.remove(callbackHandler);
        }
    }

    /** Tries to parse the file or URL named by parameter <code>urlOrFileName</code>.
     * First it tries to parse it as URL, if this does not work, it tries to parse it as file.
     * If one option works, an input stream will be opened and {@link #parse} will be called with it.
     * If both does not work, an exception is thrown.
     *
     * @see #parse
     */
    public synchronized void parseUrlOrFile(String urlOrFileName) throws ParserConfigurationException, SAXException, IOException {
        Throwable urlException = null;
        Throwable fileException = null;
        InputStream in = null;
        try {
            URL url = new URL(urlOrFileName);
            URLConnection urlConnection = url.openConnection();
            in = urlConnection.getInputStream();
        } catch (MalformedURLException mue) {
            urlException = mue;
        } catch (IOException ioe) {
            urlException = ioe;
        }
        try {
            in = new FileInputStream(urlOrFileName);
        } catch (IOException ioe) {
            fileException = ioe;
        }
        if (in != null) {
            parse(new InputSource(new BufferedInputStream(in)));
        } else {
            throw new SAXException("Could not parse " + urlOrFileName + ", is neither URL (" + urlException.getMessage() + ") nor file (" + fileException.getMessage() + ").");
        }
    }

    /** Parses the input source using the standard SAX parser and calls back the callback handlers.
     * If enabled with {@link #setFullDebugMode} the source will be verbosely copied first.<br>
     *<br>
     * <em>Note</em>: This method is synchronized, so you can not have two concurrent parses.
     */
    public synchronized void parse(InputSource is) throws ParserConfigurationException, SAXException, IOException {
        firstPCData = null;
        currentElement = null;
        factory.setNamespaceAware(!useQName || buildComplexPath);
        SAXParser parser = factory.newSAXParser();
        if (getFullDebugMode()) {
            InputSource preReadIn = bufferParserStream(is);
            parser.parse(preReadIn, new SAXInnerHandler());
        } else {
            parser.parse(is, new SAXInnerHandler());
        }
    }

    private InputSource bufferParserStream(InputSource is) throws IOException {
        StringBuffer buf = new StringBuffer();
        Reader reader;
        BufferedReader bufferedReader;
        if (is.getCharacterStream() != null) {
            reader = is.getCharacterStream();
        } else {
            String encoding = is.getEncoding();
            if (encoding != null) {
                reader = new InputStreamReader(is.getByteStream(), encoding);
            } else {
                reader = new InputStreamReader(is.getByteStream());
            }
        }
        if (reader instanceof BufferedReader) {
            bufferedReader = (BufferedReader) reader;
        } else {
            bufferedReader = new BufferedReader(reader);
        }
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            } else {
                buf.append(line).append('\n');
            }
        }
        debugBuffer = buf.toString();
        return new InputSource(new StringReader(debugBuffer));
    }

    private void callBackStartElementWhenReady() {
        if (currentElement != null) {
            String content = getFirstPCData();
            PathUnit path;
            if (buildComplexPath) {
                path = new PathUnit(currentElement.path, (PathItem[]) currentElement.pathList.toArray(new PathItem[currentElement.pathList.size()]));
            } else {
                path = new PathUnit(currentElement.path);
            }
            synchronized (callbackHandlerList) {
                for (Iterator it = callbackHandlerList.iterator(); it.hasNext(); ) {
                    SAXImportHandler callbackHandler = (SAXImportHandler) it.next();
                    if (getMakeCopy()) {
                        callbackHandler.startElement(new PathUnit(path), currentElement.name, new AttributesImpl(currentElement.attributes), content);
                    } else {
                        callbackHandler.startElement(path, currentElement.name, currentElement.attributes, content);
                    }
                }
            }
            firstPCData = null;
            currentElement = null;
        }
    }

    private void sendCharacters(String text) {
        if (text == null) return;
        if (isFirstPCData) {
            if (isCallbackCDataOnStartElement) {
                addToFirstPCData(text);
            } else {
                sendCData(text);
            }
        } else {
            foundMixedPCData = true;
            sendCData(text);
        }
    }

    private void callBackCDATAWhenReady() {
        callBackStartElementWhenReady();
        if (currentMixedPCData == null) {
            return;
        }
        String text = currentMixedPCData.toString();
        text = trimPCData(text);
        if (text == null) {
            return;
        }
        PathUnit path;
        if (buildComplexPath) {
            path = new PathUnit(parseStack.getPath(), (PathItem[]) parseStack.getPathList().toArray(new PathItem[parseStack.getPathList().size()]));
        } else {
            path = new PathUnit(parseStack.getPath());
        }
        synchronized (callbackHandlerList) {
            for (Iterator it = callbackHandlerList.iterator(); it.hasNext(); ) {
                SAXImportHandler callbackHandler = (SAXImportHandler) it.next();
                if (getMakeCopy()) {
                    callbackHandler.cdata(new PathUnit(path), text);
                } else {
                    callbackHandler.cdata(path, text);
                }
            }
        }
        currentMixedPCData = null;
    }

    private void sendCData(String text) {
        if (currentMixedPCData == null) {
            currentMixedPCData = new StringBuffer(text.length());
        }
        currentMixedPCData.append(text);
    }

    private void addToFirstPCData(String text) {
        if (firstPCData == null) {
            firstPCData = new StringBuffer(text.length());
        }
        firstPCData.append(text);
    }

    private String getFirstPCData() {
        if (firstPCData == null) {
            return null;
        } else {
            String text = firstPCData.toString();
            return trimPCData(text);
        }
    }

    private String trimPCData(String pcData) {
        if (pcData == null) {
            return null;
        } else {
            if (getTrimContent()) {
                pcData = pcData.trim();
            }
            if (pcData.length() == 0 && getZeroLengthIsNull()) {
                return null;
            } else {
                return pcData;
            }
        }
    }

    private static final class ParseElement {

        public String name, path;

        public List pathList;

        public AttributesImpl attributes;

        public ParseElement(String name, String path, List pathList, AttributesImpl attributes) {
            this.name = name;
            this.path = path;
            this.attributes = attributes;
            this.pathList = pathList;
        }
    }

    private final class SAXInnerHandler extends DefaultHandler {

        public void startDocument() {
            synchronized (callbackHandlerList) {
                for (Iterator it = callbackHandlerList.iterator(); it.hasNext(); ) {
                    SAXImportHandler callbackHandler = (SAXImportHandler) it.next();
                    callbackHandler.startDocument();
                }
            }
        }

        public void endDocument() {
            callBackStartElementWhenReady();
            callBackCDATAWhenReady();
            synchronized (callbackHandlerList) {
                for (Iterator it = callbackHandlerList.iterator(); it.hasNext(); ) {
                    SAXImportHandler callbackHandler = (SAXImportHandler) it.next();
                    callbackHandler.endDocument();
                }
            }
        }

        public void characters(char ch[], int start, int length) {
            if (length < 1) return;
            String text = new String(ch, start, length);
            sendCharacters(text);
        }

        public void endElement(String namespaceURI, String localName, String qName) {
            callBackStartElementWhenReady();
            callBackCDATAWhenReady();
            String name;
            if (!useQName || qName == null || qName.length() == 0) {
                name = localName;
            } else {
                name = qName;
            }
            PathUnit path;
            if (buildComplexPath) {
                path = new PathUnit(parseStack.getPath(), (PathItem[]) parseStack.getPathList().toArray(new PathItem[parseStack.getPathList().size()]));
            } else {
                path = new PathUnit(parseStack.getPath());
            }
            synchronized (callbackHandlerList) {
                for (Iterator it = callbackHandlerList.iterator(); it.hasNext(); ) {
                    SAXImportHandler callbackHandler = (SAXImportHandler) it.next();
                    if (getMakeCopy()) {
                        callbackHandler.endElement(new PathUnit(path), name);
                    } else {
                        callbackHandler.endElement(path, name);
                    }
                }
            }
            if (parseStack.empty()) {
                throw new NestedRuntimeException("Umatchted end tag: " + name);
            } else {
                Object top = parseStack.peek();
                String topName;
                if (buildComplexPath) {
                    topName = ((PathItem) top).getName();
                } else {
                    topName = (String) top;
                }
                if (!name.equals(topName)) {
                    throw new NestedRuntimeException("End tag " + name + " does not match start tag " + top);
                } else {
                    parseStack.pop();
                }
            }
            isFirstPCData = false;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            callBackStartElementWhenReady();
            callBackCDATAWhenReady();
            String name;
            if (!useQName || qName == null || qName.length() == 0) {
                name = localName;
            } else {
                name = qName;
            }
            parseStack.push(namespaceURI, name);
            AttributesImpl attributesCopy = new AttributesImpl(atts);
            currentElement = new ParseElement(name, parseStack.getPath(), parseStack.getPathList(), attributesCopy);
            isFirstPCData = true;
        }
    }

    private final class PathStack {

        private List pathStack;

        public PathStack(int initialCapacity) {
            pathStack = new ArrayList(initialCapacity);
        }

        public PathStack() {
            pathStack = new ArrayList();
        }

        public String getPath() {
            StringBuffer path = new StringBuffer(100);
            path.append('/');
            for (Iterator it = pathStack.iterator(); it.hasNext(); ) {
                Object element = it.next();
                String pathElement;
                if (buildComplexPath) {
                    pathElement = ((PathItem) element).getName();
                } else {
                    pathElement = (String) element;
                }
                path.append(pathElement).append('/');
            }
            return path.toString();
        }

        public List getPathList() {
            return pathStack;
        }

        public String toString() {
            return getPath();
        }

        public void push(String namespaceURI, String name) {
            if (buildComplexPath) {
                pathStack.add(new PathItem(name, namespaceURI));
            } else {
                pathStack.add(name);
            }
        }

        public int size() {
            return pathStack.size();
        }

        public boolean empty() {
            return (pathStack.size() <= 0);
        }

        public Object peek() {
            int size = pathStack.size();
            if (size > 0) {
                return pathStack.get(size - 1);
            } else {
                return null;
            }
        }

        public Object pop() {
            int size = pathStack.size();
            if (size > 0) {
                Object o = pathStack.get(size - 1);
                pathStack.remove(size - 1);
                return o;
            } else {
                return null;
            }
        }
    }
}
