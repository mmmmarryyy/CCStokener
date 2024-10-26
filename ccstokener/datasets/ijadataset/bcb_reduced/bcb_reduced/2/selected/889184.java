package com.memoire.bu;

import java.awt.Cursor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JViewport;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuLog;

public class BuBrowserPane extends BuScrollPane implements HyperlinkListener {

    private static final int GAP = BuPreferences.BU.getIntegerProperty("layout.gap", 5);

    BuBrowserFrame frame_;

    BuEditorPane html_;

    private HTMLEditorKit kit_;

    ViewFactory factory_;

    Cursor cursor_;

    private Vector avant_, apres_;

    public BuBrowserPane(BuBrowserFrame _frame) {
        frame_ = _frame;
        factory_ = new HTMLEditorKit.HTMLFactory() {

            public View create(Element _e) {
                View r = null;
                Object o = _e.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (o instanceof HTML.Tag) {
                    HTML.Tag tag = (HTML.Tag) o;
                    if (tag == HTML.Tag.IMG) r = createViewIMG(_e);
                }
                if (r == null) r = super.create(_e);
                return r;
            }
        };
        kit_ = new HTMLEditorKit() {

            public ViewFactory getViewFactory() {
                return factory_;
            }
        };
        avant_ = new Vector(5);
        apres_ = new Vector(5);
        html_ = new BuEditorPane();
        html_.setEditorKit(kit_);
        html_.setDocument(kit_.createDefaultDocument());
        html_.setEditable(false);
        html_.addHyperlinkListener(this);
        if (GAP == 0) html_.setBorder(BuBorders.EMPTY0000);
        JViewport v = getViewport();
        cursor_ = v.getCursor();
        v.add(html_);
    }

    protected ImageView createViewIMG(Element _e) {
        ImageView r = new ImageView(_e);
        r.setLoadsSynchronously(true);
        return r;
    }

    public String getHtmlSource() {
        return html_.getText();
    }

    public void copy() {
        html_.copy();
    }

    public void back() {
        int l = avant_.size();
        if (l > 1) {
            URL url = (URL) avant_.elementAt(l - 2);
            URL cur = (URL) avant_.elementAt(l - 1);
            FuLog.trace("BWB: go back    " + url);
            avant_.removeElementAt(l - 1);
            apres_.insertElementAt(cur, 0);
            linkActivated(url);
        }
    }

    public void forward() {
        if (apres_.size() > 0) {
            URL url = (URL) apres_.elementAt(0);
            FuLog.trace("BWB: go forward " + url);
            apres_.removeElementAt(0);
            linkActivated(url);
        }
    }

    public void reload() {
        int l = avant_.size();
        if (l > 0) {
            URL url = (URL) avant_.elementAt(l - 1);
            FuLog.trace("BWB: reload     " + url);
            avant_.removeElementAt(l - 1);
            linkActivated(url);
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent _evt) {
        if (_evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            html_.setSelectionStart(0);
            html_.setSelectionEnd(0);
            URL u = _evt.getURL();
            frame_.setMessage(u == null ? " " : u.toExternalForm());
            linkActivated(u);
        } else if (_evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            URL u = _evt.getURL();
            frame_.setMessage(u == null ? " " : u.toExternalForm());
        } else if (_evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
            frame_.setMessage(" ");
        }
    }

    public void linkActivated(final URL _url) {
        frame_.setDocumentUrl(_url);
    }

    protected boolean isImage(URL _url) {
        boolean r = false;
        String f = _url.getFile();
        if (f != null) {
            f = f.toLowerCase();
            if (f.endsWith(".gif") || f.endsWith(".jpg") || f.endsWith(".jpeg")) r = true;
        }
        return r;
    }

    protected URL getUrlFor(URL _url) {
        URL r = _url;
        String p = _url.getProtocol();
        if (p.equals("mailto")) r = null; else if (p.equals("file")) {
            String f = _url.getFile();
            if ((f == null) || f.endsWith("/")) {
                try {
                    r = new URL(_url, "index.html");
                } catch (MalformedURLException ex) {
                }
            }
        }
        return r;
    }

    protected String getSourceFor(URL _url) {
        String r = null;
        if (isImage(_url)) r = "<IMG SRC=\"" + _url + "\">";
        return r;
    }

    protected String getTitleFor(URL _url) {
        String r = null;
        if (isImage(_url)) {
            r = _url.toExternalForm();
            r = r.substring(r.lastIndexOf('/') + 1);
        }
        return r;
    }

    protected InputStream getInputStreamFor(URL _url) throws IOException {
        return _url.openStream();
    }

    public final void setContent(final URL _url, final String _source, final String _title) {
        Runnable runnable = new Runnable() {

            public void run() {
                URL u = _url;
                String s = _source;
                String t = _title;
                if (_url != null) {
                    u = getUrlFor(_url);
                    if (u != null) {
                        if (s == null) s = getSourceFor(_url);
                        if (t == null) t = getTitleFor(_url);
                    }
                }
                if ((u != null) || (s != null)) {
                    setContent0(u, s, t);
                }
            }
        };
        Thread thread = new Thread(runnable, "Loading " + _url);
        thread.setPriority(Thread.MIN_PRIORITY + 1);
        thread.start();
    }

    protected void setContent0(final URL _url, String _source, String _title) {
        Runnable runnable = new Runnable() {

            public void run() {
                JViewport v = getViewport();
                v.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                v.remove(html_);
                v.repaint();
            }
        };
        BuLib.invokeNow(runnable);
        final HTMLDocument hd = (HTMLDocument) kit_.createDefaultDocument();
        if (_source != null) {
            try {
                kit_.read(new StringReader(_source), hd, 0);
            } catch (Exception ex) {
                String source = FuLib.replace(_source, "&", "&amp;");
                source = FuLib.replace(source, "\"", "&quot;");
                source = FuLib.replace(source, "<", "&lt;");
                source = FuLib.replace(source, ">", "&gt;");
                setError(_url, "The HTML is not valid.<BR><PRE>" + source + "</PRE>");
                return;
            }
        } else if (_url != null) {
            try {
                hd.setBase(_url);
                hd.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                kit_.read(new InputStreamReader(getInputStreamFor(_url), "iso-8859-1"), hd, 0);
            } catch (Exception ex) {
                setError(_url, "The document can not be accessed.<BR>" + _url.toExternalForm());
                return;
            }
        }
        updateStyles(hd.getStyleSheet());
        try {
            html_.setDocument(hd);
        } catch (Exception ex2) {
            setError(_url, "The document can not be displayed.<BR>" + (_url == null ? "?" : _url.toExternalForm()) + "<BR>" + "Contact the Swing Team at Sun Microsystems.");
            return;
        }
        try {
            html_.getPreferredSize();
        } catch (Exception ex1) {
            StyleSheet ss = hd.getStyleSheet();
            StyleSheet[] as = ss.getStyleSheets();
            if (as != null) for (int i = 0; i < as.length; i++) ss.removeStyleSheet(as[i]);
            Enumeration rules = ss.getStyleNames();
            while (rules.hasMoreElements()) {
                String name = (String) rules.nextElement();
                ss.removeStyle(name);
            }
            try {
                html_.getPreferredSize();
            } catch (Exception ex2) {
                setError(_url, "CSS support in Swing is broken.<BR>" + "The document can not be displayed.<BR>" + (_url == null ? "?" : _url.toExternalForm()));
                return;
            }
        }
        final String tt = _title;
        runnable = new Runnable() {

            public void run() {
                setContent1(_url, hd, tt);
            }
        };
        BuLib.invokeLater(runnable);
    }

    protected void setContent1(URL _url, HTMLDocument _hd, String _title) {
        frame_.setUrlText(_url == null ? "" : _url.toExternalForm(), false);
        String title = _title;
        Object t = _hd.getProperty(Document.TitleProperty);
        if (t != null) title = t.toString();
        frame_.setTitle(title);
        JViewport v = getViewport();
        v.add(html_);
        v.setCursor(cursor_);
        if (_url != null) avant_.addElement(_url);
        frame_.setBackEnabled(avant_.size() > 1);
        frame_.setForwardEnabled(apres_.size() > 0);
        frame_.setMessage(" ");
    }

    public void setError(final URL _url, final String _text) {
        FuLog.debug("WBP: setError " + _url + " " + _text);
        Runnable runnable = new Runnable() {

            public void run() {
                if (_url != null) frame_.setUrlText(_url.toExternalForm(), false);
                frame_.setTitle(frame_._("Erreur"));
                html_.setCursor(cursor_);
                String t = _text;
                if (t == null) {
                    t = "This URL can not be displayed";
                    t += (_url != null) ? ":<BR> " + _url.toExternalForm() : ".";
                    t += "<BR>If it is correct, please use an external browser.";
                }
                setContent(_url, "<HTML><BODY>" + t + "</BODY></HTML>", "Error");
            }
        };
        BuLib.invokeLater(runnable);
    }

    private static final void updateStyles(StyleSheet _styles) {
        if (_styles != null) {
            int FONT = 12 * BuPreferences.BU.getFontScaling() / 100;
            Enumeration rules = _styles.getStyleNames();
            while (rules.hasMoreElements()) {
                String name = (String) rules.nextElement();
                Style rule = _styles.getStyle(name);
                if (StyleConstants.ALIGN_JUSTIFIED == StyleConstants.getAlignment(rule)) StyleConstants.setAlignment(rule, StyleConstants.ALIGN_LEFT);
                if (StyleConstants.getFontSize(rule) <= FONT) {
                    StyleConstants.setFontSize(rule, FONT);
                }
            }
        }
    }
}
