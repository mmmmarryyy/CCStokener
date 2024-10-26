package de.shandschuh.jaolt.gui.core.htmlrenderer;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Policy;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.ReadyStateChangeListener;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.gui.HtmlPanel;
import org.w3c.dom.Document;
import org.w3c.dom.html2.HTMLCollection;
import org.w3c.dom.html2.HTMLElement;
import org.w3c.dom.html2.HTMLLinkElement;
import de.shandschuh.jaolt.core.auction.Template;
import de.shandschuh.jaolt.gui.core.HTMLRenderer;
import de.shandschuh.jaolt.tools.url.URLHelper;

public class CobraHTMLRenderer extends HTMLRenderer {

    private static final long serialVersionUID = 1L;

    private HtmlRendererContext context;

    private HtmlPanel htmlPanel;

    public CobraHTMLRenderer() {
        htmlPanel = new HtmlPanel();
        htmlPanel.setBorder(BorderFactory.createEtchedBorder());
        context = new DummyRendererContext();
        setHTML("", null);
    }

    public void setHTML(String html, Template template) {
        htmlPanel.setHtml(html, template != null && template.getFile() != null ? template.getFile().toURI().toASCIIString() : "", context);
        htmlPanel.revalidate();
        htmlPanel.repaint();
    }

    public JComponent getRenderedComponent() {
        return htmlPanel;
    }

    @Override
    public void setEnabled(boolean b) {
        htmlPanel.setEnabled(b);
    }

    private static class DummyRendererContext implements HtmlRendererContext {

        public void alert(String arg0) {
        }

        public void back() {
        }

        public void blur() {
        }

        public void close() {
        }

        public boolean confirm(String arg0) {
            return false;
        }

        public BrowserFrame createBrowserFrame() {
            return null;
        }

        public void focus() {
        }

        public String getDefaultStatus() {
            return null;
        }

        public HTMLCollection getFrames() {
            return null;
        }

        public HtmlObject getHtmlObject(HTMLElement arg0) {
            return null;
        }

        public String getName() {
            return null;
        }

        public HtmlRendererContext getOpener() {
            return null;
        }

        public HtmlRendererContext getParent() {
            return null;
        }

        public String getStatus() {
            return null;
        }

        public HtmlRendererContext getTop() {
            return null;
        }

        public UserAgentContext getUserAgentContext() {
            return new UserAgentContext() {

                public HttpRequest createHttpRequest() {
                    return new HttpRequest() {

                        private byte[] bytes;

                        private Vector<ReadyStateChangeListener> readyStateChangeListeners = new Vector<ReadyStateChangeListener>();

                        public void abort() {
                        }

                        public void addReadyStateChangeListener(ReadyStateChangeListener readyStateChangeListener) {
                            readyStateChangeListeners.add(readyStateChangeListener);
                        }

                        public String getAllResponseHeaders() {
                            return null;
                        }

                        public int getReadyState() {
                            return bytes != null ? STATE_COMPLETE : STATE_UNINITIALIZED;
                        }

                        public byte[] getResponseBytes() {
                            return bytes;
                        }

                        public String getResponseHeader(String arg0) {
                            return null;
                        }

                        public Image getResponseImage() {
                            return bytes != null ? Toolkit.getDefaultToolkit().createImage(bytes) : null;
                        }

                        public String getResponseText() {
                            return new String(bytes);
                        }

                        public Document getResponseXML() {
                            return null;
                        }

                        public int getStatus() {
                            return 200;
                        }

                        public String getStatusText() {
                            return "OK";
                        }

                        public void open(String method, String url) {
                            open(method, url, false);
                        }

                        public void open(String method, URL url) {
                            open(method, url, false);
                        }

                        public void open(String mehod, URL url, boolean async) {
                            try {
                                URLConnection connection = url.openConnection();
                                bytes = new byte[connection.getContentLength()];
                                InputStream inputStream = connection.getInputStream();
                                inputStream.read(bytes);
                                inputStream.close();
                                for (ReadyStateChangeListener readyStateChangeListener : readyStateChangeListeners) {
                                    readyStateChangeListener.readyStateChanged();
                                }
                            } catch (IOException e) {
                            }
                        }

                        public void open(String method, String url, boolean async) {
                            open(method, URLHelper.createURL(url), async);
                        }

                        public void open(String method, String url, boolean async, String arg3) {
                            open(method, URLHelper.createURL(url), async);
                        }

                        public void open(String method, String url, boolean async, String arg3, String arg4) {
                            open(method, URLHelper.createURL(url), async);
                        }
                    };
                }

                public String getAppCodeName() {
                    return null;
                }

                public String getAppMinorVersion() {
                    return null;
                }

                public String getAppName() {
                    return null;
                }

                public String getAppVersion() {
                    return null;
                }

                public String getBrowserLanguage() {
                    return null;
                }

                public String getCookie(URL arg0) {
                    return null;
                }

                public String getPlatform() {
                    return null;
                }

                public int getScriptingOptimizationLevel() {
                    return 0;
                }

                public Policy getSecurityPolicy() {
                    return null;
                }

                public String getUserAgent() {
                    return null;
                }

                public boolean isCookieEnabled() {
                    return false;
                }

                public boolean isMedia(String arg0) {
                    return false;
                }

                public boolean isScriptingEnabled() {
                    return false;
                }

                public void setCookie(URL arg0, String arg1) {
                }
            };
        }

        public boolean isClosed() {
            return false;
        }

        public boolean isVisitedLink(HTMLLinkElement arg0) {
            return false;
        }

        public void linkClicked(HTMLElement arg0, URL arg1, String arg2) {
        }

        public void navigate(URL arg0, String arg1) {
        }

        public void onContextMenu(HTMLElement arg0, MouseEvent arg1) {
        }

        public void onMouseOut(HTMLElement arg0, MouseEvent arg1) {
        }

        public void onMouseOver(HTMLElement arg0, MouseEvent arg1) {
        }

        public HtmlRendererContext open(String arg0, String arg1, String arg2, boolean arg3) {
            return this;
        }

        public HtmlRendererContext open(URL arg0, String arg1, String arg2, boolean arg3) {
            return this;
        }

        public String prompt(String arg0, String arg1) {
            return null;
        }

        public void reload() {
        }

        public void scroll(int arg0, int arg1) {
        }

        public void setDefaultStatus(String arg0) {
        }

        public void setOpener(HtmlRendererContext arg0) {
        }

        public void setStatus(String arg0) {
        }

        public void submitForm(String arg0, URL arg1, String arg2, String arg3, FormInput[] arg4) {
        }
    }

    @Override
    public void setVisible(boolean visible) {
        htmlPanel.setVisible(visible);
    }
}
