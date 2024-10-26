package riafswing.helper;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.test.SimpleHtmlRendererContext;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.lobobrowser.util.Urls;
import org.lobobrowser.util.io.BufferExceededException;
import org.lobobrowser.util.io.RecordedInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.html2.HTMLElement;
import org.xml.sax.SAXException;
import riaf.controller.RiafMgr;
import riaf.interceptors.BasicInterceptor;
import riaf.models.CheckboxModel;
import riaf.models.ModelEvent;
import riaf.models.StaticModel;
import riafswing.RWebPanel;
import riafswing.RWebPanel.ContentType;
import core.utils.Message;

public class RWebHtmlRendererContext extends SimpleHtmlRendererContext {

    /**
	 * static logger-object
	 */
    private static final Logger logger = Logger.getLogger(RWebHtmlRendererContext.class);

    private static final String STYLE_ATTR = "style";

    private static final String MARKSTYLE = "border: 2px solid #00FF00;background-color:#00FF00;";

    private static final String SELECTSTYLE = "border: 2px solid #ff0000;background-color:#ff0000;";

    private RWebPanel panel;

    private HTMLElement lastMarked;

    private boolean markElements = true;

    private String sourceCode = null;

    private HTMLElement lastClickElement;

    private static Set<String> loaded = new HashSet<String>();

    protected final RSimpleUserAgentContext rSimpleUserAgentContext = new RSimpleUserAgentContext();

    /**
	 * @param contextComponent
	 * @param parentRcontext
	 */
    public RWebHtmlRendererContext(HtmlPanel contextComponent, HtmlRendererContext parentRcontext, RWebPanel panel) {
        super(contextComponent, parentRcontext);
        this.panel = panel;
    }

    public void setPanel(RWebPanel panel) {
        this.panel = panel;
    }

    public void clear() {
        loaded.clear();
        sourceCode = null;
        lastClickElement = null;
        lastMarked = null;
    }

    @Override
    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public void linkClicked(HTMLElement linkNode, URL url, String target) {
        ModelEvent contentEvent = new ModelEvent(panel.getModel(), StaticModel.CONTENT);
        contentEvent.setParameter(url.toString());
    }

    @Override
    protected void submitFormSync(String method, URL action, String target, String enctype, FormInput[] formInputs) throws IOException, SAXException {
        if (loaded.contains(action.toString())) {
            return;
        }
        loaded.add(action.toString());
        final String actualMethod = method.toUpperCase();
        URL resolvedURL = resolveUrl(action, actualMethod, formInputs);
        URL urlForLoading;
        boolean isFileProtocol = resolvedURL.getProtocol().equalsIgnoreCase("file");
        if (isFileProtocol) {
            try {
                String ref = action.getRef();
                String refText = ref == null || ref.length() == 0 ? BasicInterceptor.EMPTY_STRING : "#" + ref;
                urlForLoading = new URL(resolvedURL.getProtocol(), action.getHost(), action.getPort(), action.getPath() + refText);
            } catch (java.net.MalformedURLException throwable) {
                this.warn("malformed", throwable);
                urlForLoading = action;
            }
        } else {
            urlForLoading = resolvedURL;
        }
        URLConnection connection = obtainConnection(urlForLoading);
        this.currentConnection = connection;
        boolean contentLoaded = false;
        try {
            connection.setRequestProperty("User-Agent", getUserAgentContext().getUserAgent());
            connection.setRequestProperty("Cookie", BasicInterceptor.EMPTY_STRING);
            for (Map.Entry<String, String> entry : panel.getConnectionParameters().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection hc = (HttpURLConnection) connection;
                hc.setRequestMethod(actualMethod);
                hc.setInstanceFollowRedirects(false);
            }
            if ("POST".equals(actualMethod)) {
                encodePostParams(connection, formInputs);
            }
            if (redirect(connection, action, target)) {
                return;
            }
            byte[] input = readStream(connection.getInputStream());
            RWebPanel.ContentType contentType = ContentType.getContentType(urlForLoading);
            ByteArrayInputStream in = new ByteArrayInputStream(input);
            try {
                sourceCode = null;
                if (ContentType.PDF.equals(contentType)) {
                    contentLoaded = loadPDF(in);
                    if (!contentLoaded) in.reset(); else panel.showComponentForContent(contentType);
                }
                if (ContentType.Image.equals(contentType)) {
                    contentLoaded = loadImage(in);
                    if (!contentLoaded) in.reset(); else panel.showComponentForContent(contentType);
                }
                if (!contentLoaded) {
                    contentType = ContentType.WEB;
                }
                if (contentType.equals(ContentType.WEB)) {
                    loadHtml(connection, in, urlForLoading);
                    contentLoaded = true;
                    panel.showComponentForContent(contentType);
                }
            } catch (Exception ex) {
                contentLoaded = false;
            } finally {
                if (!contentLoaded) {
                    Message msg = new Message("CouldNotLoadURL", logger);
                    msg.addParam(urlForLoading.toExternalForm());
                    msg.addParam(ContentType.getContentType(urlForLoading).name());
                    msg.log();
                }
                in.close();
            }
        } finally {
            this.currentConnection = null;
        }
    }

    private boolean loadPDF(InputStream in) {
        if (panel.getPdfViewer().hasDocument()) {
            panel.getPdfViewer().getController().closeDocument();
        }
        panel.getPdfViewer().getController().openDocument(in, null, null);
        return panel.getPdfViewer().hasDocument();
    }

    private boolean loadImage(InputStream in) throws IOException {
        BufferedImage img = ImageIO.read(in);
        panel.getImageViewer().setImage(img);
        return img != null;
    }

    private void loadHtml(URLConnection connection, InputStream in, URL urlForLoading) throws IOException, SAXException {
        RecordedInputStream rin = new RecordedInputStream(in, 1000000);
        InputStream bin = new BufferedInputStream(rin, 8192);
        String actualURI = urlForLoading.toExternalForm();
        HTMLDocumentImpl document = createDocument(new InputSourceImpl(bin, actualURI, getDocumentCharset(connection)));
        HtmlPanel htmlPanel = getHtmlPanel();
        htmlPanel.setDocument(document, this);
        document.load();
        String ref = urlForLoading.getRef();
        if (ref != null && ref.length() != 0) {
            htmlPanel.scrollToElement(ref);
        }
        try {
            sourceCode = rin.getString("ISO-8859-1");
        } catch (BufferExceededException bee) {
            sourceCode = "[TOO BIG]";
        }
        document.close();
        for (int i = 0; i < document.getChildNodes().getLength(); ++i) {
            Node item = document.getChildNodes().item(i);
            if (item.getChildNodes().getLength() == 0) continue;
            panel.loadNode((NodeImpl) item);
        }
    }

    /**
	 * Reads the given stream into a byte buffer and returns the contents as a
	 * byte array.
	 * 
	 * @param in
	 *            the stream to read.
	 * @return a the contents read from the stream.
	 * @throws IOException
	 *             If the first byte cannot be read for any reason other than
	 *             the end of the file, if the input stream has been closed, or
	 *             if some other I/O error occurs.
	 */
    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(100 * 1024);
        try {
            int offset = 0, count = 0;
            byte[] buff = new byte[4 * 1024];
            while ((count = in.read(buff)) != -1) {
                outStream.write(buff, offset, count);
            }
            return outStream.toByteArray();
        } finally {
            in.close();
            outStream.close();
        }
    }

    /**
	 * @param connection
	 * @param formInputs
	 */
    private void encodePostParams(URLConnection connection, FormInput[] formInputs) throws IOException {
        connection.setDoOutput(true);
        ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
        boolean firstParam = true;
        if (formInputs != null) {
            for (int i = 0; i < formInputs.length; i++) {
                FormInput parameter = formInputs[i];
                String name = parameter.getName();
                String encName = URLEncoder.encode(name, "UTF-8");
                if (parameter.isText()) {
                    if (firstParam) {
                        firstParam = false;
                    } else {
                        bufOut.write((byte) '&');
                    }
                    String valueStr = parameter.getTextValue();
                    String encValue = URLEncoder.encode(valueStr, "UTF-8");
                    bufOut.write(encName.getBytes("UTF-8"));
                    bufOut.write((byte) '=');
                    bufOut.write(encValue.getBytes("UTF-8"));
                }
            }
        }
        byte[] postContent = bufOut.toByteArray();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setFixedLengthStreamingMode(postContent.length);
        }
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream postOut = connection.getOutputStream();
        postOut.write(postContent);
        postOut.flush();
    }

    /**
	 * @param connection
	 * @return
	 */
    private boolean redirect(URLConnection connection, URL action, String target) throws IOException {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection hc = (HttpURLConnection) connection;
            int responseCode = hc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String location = hc.getHeaderField("Location");
                if (location != null) {
                    java.net.URL href;
                    href = Urls.createURL(action, location);
                    navigate(href, target);
                }
                return true;
            }
        }
        return false;
    }

    /**
	 * @param urlForLoading
	 * @return
	 */
    private URLConnection obtainConnection(URL urlForLoading) throws IOException {
        Proxy proxy = getProxy();
        if (proxy == null || proxy == Proxy.NO_PROXY) return urlForLoading.openConnection(); else return urlForLoading.openConnection(proxy);
    }

    /**
	 * @param action
	 * @param formInputs
	 * @param actualMethod
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    private URL resolveUrl(URL action, String actualMethod, FormInput[] formInputs) throws IOException {
        if (!("GET".equals(actualMethod) && formInputs != null)) {
            return action;
        }
        URL noRefAction = new URL(action.getProtocol(), action.getHost(), action.getPort(), action.getFile());
        StringBuffer newUrlBuffer = new StringBuffer(noRefAction.toExternalForm());
        if (action.getQuery() == null) {
            newUrlBuffer.append("?");
        } else {
            newUrlBuffer.append("&");
        }
        boolean firstParam = true;
        for (int i = 0; i < formInputs.length; i++) {
            FormInput parameter = formInputs[i];
            String name = parameter.getName();
            String encName = URLEncoder.encode(name, "UTF-8");
            if (parameter.isText()) {
                if (firstParam) {
                    firstParam = false;
                } else {
                    newUrlBuffer.append("&");
                }
                String valueStr = parameter.getTextValue();
                String encValue = URLEncoder.encode(valueStr, "UTF-8");
                newUrlBuffer.append(encName);
                newUrlBuffer.append("=");
                newUrlBuffer.append(encValue);
            }
        }
        return new java.net.URL(newUrlBuffer.toString());
    }

    @Override
    public boolean onMouseClick(HTMLElement element, MouseEvent event) {
        lastClickElement = element;
        ModelEvent selected = new ModelEvent(panel.getModel(), CheckboxModel.SELECTED);
        selected.setParameter(element);
        panel.fireEvent(selected);
        return false;
    }

    @Override
    public boolean onContextMenu(HTMLElement element, MouseEvent event) {
        if ((panel.getContextID(event.getComponent(), event.getPoint()) != null || RiafMgr.global().isDebug())) {
            lastClickElement = element;
            panel.showMenu(event.getComponent(), event.getPoint());
        }
        return false;
    }

    @Override
    public void onMouseOver(HTMLElement element, MouseEvent event) {
        if (!markElements) {
            return;
        }
        unmarkMarkedElements();
        lastMarked = element;
        markElement(element);
    }

    public void select(HTMLElement element) {
        selectElement(element);
    }

    public void unselect(HTMLElement element) {
        if (element == null) return;
        unselectElement(element);
    }

    private void selectElement(HTMLElement e) {
        String style = e.getAttribute(STYLE_ATTR);
        if (style != null && !style.isEmpty()) {
            if (!style.contains(SELECTSTYLE)) {
                style += ";" + SELECTSTYLE;
            }
        } else {
            style = SELECTSTYLE;
        }
        e.setAttribute(STYLE_ATTR, style);
    }

    private void unselectElement(HTMLElement e) {
        String style = e.getAttribute(STYLE_ATTR);
        if (style != null && style.contains(";" + SELECTSTYLE)) {
            style = style.replace(";" + SELECTSTYLE, BasicInterceptor.EMPTY_STRING);
        }
        if (style != null && style.contains(SELECTSTYLE)) {
            style = style.replace(SELECTSTYLE, BasicInterceptor.EMPTY_STRING);
        }
        e.setAttribute(STYLE_ATTR, style);
    }

    private void markElement(HTMLElement e) {
        String style = e.getAttribute(STYLE_ATTR);
        if (style != null && !style.isEmpty()) {
            if (!style.contains(MARKSTYLE)) {
                style += ";" + MARKSTYLE;
            }
        } else {
            style = MARKSTYLE;
        }
        e.setAttribute(STYLE_ATTR, style);
    }

    private void unmarkElement(HTMLElement e) {
        String style = e.getAttribute(STYLE_ATTR);
        if (style != null && style.contains(";" + MARKSTYLE)) {
            style = style.replace(";" + MARKSTYLE, BasicInterceptor.EMPTY_STRING);
        }
        if (style != null && style.contains(MARKSTYLE)) {
            style = style.replace(MARKSTYLE, BasicInterceptor.EMPTY_STRING);
        }
        e.setAttribute(STYLE_ATTR, style);
    }

    public void setMarkElements(boolean mark) {
        if (mark != markElements) {
            markElements = mark;
            if (!markElements) {
                unmarkMarkedElements();
                lastMarked = null;
            }
        }
    }

    private void unmarkMarkedElements() {
        if (lastMarked != null) {
            Node el = lastMarked;
            do {
                if (el instanceof HTMLElement) {
                    unmarkElement((HTMLElement) el);
                }
                el = el.getParentNode();
            } while (el != null);
        }
    }

    @Override
    public void onMouseOut(HTMLElement element, MouseEvent event) {
        unmarkElement(element);
    }

    public HTMLElement getLastClickedElement() {
        return lastClickElement;
    }

    @Override
    public UserAgentContext getUserAgentContext() {
        return rSimpleUserAgentContext;
    }

    private static class RSimpleUserAgentContext extends SimpleUserAgentContext {

        @Override
        public boolean isScriptingEnabled() {
            return super.isScriptingEnabled();
        }

        @Override
        public HttpRequest createHttpRequest() {
            return new RWebHttpRequest(this, this.getProxy());
        }
    }
}
