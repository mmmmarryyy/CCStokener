package org.fudaa.fudaa.commun.aide;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JViewport;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
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
import com.memoire.bu.BuBorders;
import com.memoire.bu.BuEditorPane;
import com.memoire.bu.BuLib;
import com.memoire.bu.BuPreferences;
import com.memoire.bu.BuPreferencesDialog;
import com.memoire.bu.BuPreferencesMainPanel;
import com.memoire.bu.BuResource;
import com.memoire.bu.BuScrollPane;
import com.memoire.fu.Fu;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.BuNetworkPreferencesPanel;
import org.fudaa.ctulu.CtuluLib;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.gui.CtuluLibSwing;
import org.fudaa.fudaa.commun.FudaaBrowserControl;

/**
 * @author Fred Deniger
 * @version $Id: FudaaHelpPane.java,v 1.8 2007-05-04 13:58:05 deniger Exp $
 */
public class FudaaHelpPane extends BuScrollPane implements HyperlinkListener {

    private static final int GAP = BuPreferences.BU.getIntegerProperty("layout.gap", 5);

    FudaaHelpParentI frame_;

    BuEditorPane html_;

    private final HTMLEditorKit kit_;

    ViewFactory factory_;

    Cursor cursor_;

    private final List avant_, apres_;

    private final int maxHistory_ = 30;

    public final boolean isHttp() {
        return isHttp_;
    }

    public final void setHttp(final boolean _isHttp) {
        isHttp_ = _isHttp;
    }

    private void updateVector(final List _l) {
        if (_l != null && _l.size() > maxHistory_) {
            _l.remove(0);
        }
    }

    boolean isHttp_;

    public FudaaHelpPane(final FudaaHelpParentI _frame, final boolean _isHttp) {
        frame_ = _frame;
        isHttp_ = _isHttp;
        setAutoscrolls(true);
        factory_ = new HTMLEditorKit.HTMLFactory() {

            public View create(final Element _e) {
                View r = null;
                final Object o = _e.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (o instanceof HTML.Tag) {
                    final HTML.Tag tag = (HTML.Tag) o;
                    if (tag == HTML.Tag.IMG) {
                        r = createViewIMG(_e);
                    }
                }
                if (r == null) {
                    r = super.create(_e);
                }
                return r;
            }
        };
        kit_ = new HTMLEditorKit() {

            public ViewFactory getViewFactory() {
                return factory_;
            }
        };
        avant_ = new ArrayList(5);
        apres_ = new ArrayList(5);
        html_ = new BuEditorPane();
        html_.setAutoscrolls(true);
        html_.setEditorKit(kit_);
        html_.setDocument(kit_.createDefaultDocument());
        html_.setEditable(false);
        html_.addHyperlinkListener(this);
        if (GAP == 0) {
            html_.setBorder(BuBorders.EMPTY0000);
        }
        final JViewport v = getViewport();
        cursor_ = v.getCursor();
        v.add(html_);
    }

    protected ImageView createViewIMG(final Element _e) {
        final ImageView r = new ImageView(_e);
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
        final int l = avant_.size();
        if (l > 1) {
            final URL url = (URL) avant_.get(l - 2);
            final URL cur = (URL) avant_.get(l - 1);
            FuLog.trace("BWB: go back    " + url);
            avant_.remove(l - 1);
            apres_.add(0, cur);
            updateVector(apres_);
            linkActivated(url, false);
        }
    }

    public void forward() {
        if (apres_.size() > 0) {
            final URL url = (URL) apres_.get(0);
            FuLog.trace("BWB: go forward " + url);
            apres_.remove(0);
            linkActivated(url, false);
        }
    }

    public void reload() {
        final int l = avant_.size();
        if (l > 0) {
            final URL url = (URL) avant_.get(l - 1);
            FuLog.trace("BWB: reload     " + url);
            avant_.remove(l - 1);
            linkActivated(url, true);
        }
    }

    public void hyperlinkUpdate(final HyperlinkEvent _evt) {
        if (_evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if ("ShowProxy".equals(_evt.getDescription())) {
                showProxy();
                return;
            }
            html_.setSelectionStart(0);
            html_.setSelectionEnd(0);
            final URL u = _evt.getURL();
            frame_.setMessage(u == null ? CtuluLibString.ESPACE : u.toExternalForm());
            linkActivated(u, false);
        } else if (_evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            final URL u = _evt.getURL();
            frame_.setMessage(u == null ? CtuluLibString.ESPACE : u.toExternalForm());
        } else if (_evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
            frame_.setMessage(" ");
        }
    }

    public void linkActivated(final URL _url, final boolean _forceReload) {
        if (_url == null) {
            return;
        }
        if ("ShowProxy".equals(_url.toString())) {
            showProxy();
            return;
        }
        if (!isHttp_ && "http".equals(_url.getProtocol())) {
            FudaaBrowserControl.displayURL(_url);
        }
        frame_.setDocumentUrl(_url, _forceReload);
    }

    protected boolean isImage(final URL _url) {
        boolean r = false;
        String f = _url.getFile();
        if (f != null) {
            f = f.toLowerCase();
            if (f.endsWith(".gif") || f.endsWith(".jpg") || f.endsWith(".jpeg")) {
                r = true;
            }
        }
        return r;
    }

    protected URL getUrlFor(final URL _url) {
        URL r = _url;
        final String p = _url.getProtocol();
        if ("mailto".equals(p)) {
            r = null;
        } else if ("file".equals(p)) {
            final String f = _url.getFile();
            if ((f == null) || f.endsWith("/")) {
                try {
                    r = new URL(_url, "index.html");
                } catch (final MalformedURLException ex) {
                }
            }
        }
        return r;
    }

    protected String getSourceFor(final URL _url) {
        String r = null;
        if (isImage(_url)) {
            r = "<IMG SRC=\"" + _url + "\">";
        }
        return r;
    }

    protected String getTitleFor(final URL _url) {
        String r = null;
        if (isImage(_url)) {
            r = _url.toExternalForm();
            r = r.substring(r.lastIndexOf('/') + 1);
        }
        return r;
    }

    protected String getUrlText(final URL _url) {
        if (_url == null) {
            return null;
        }
        final String r = _url.toExternalForm();
        final int idx = r.lastIndexOf('/');
        if (idx < 0) {
            return r;
        }
        return r.substring(idx + 1);
    }

    protected InputStream getInputStreamFor(final URL _url) throws IOException {
        return _url.openStream();
    }

    public final void setContent(final URL _url, final String _title, final boolean _forceRefresh) {
        setContent(_url, null, _title, _forceRefresh);
    }

    public final void setContent(final URL _url, final boolean _forceRefresh) {
        setContent(_url, null, null, _forceRefresh);
    }

    public final void setContent(final URL _url, final String _source, final String _title, final boolean _forceReload) {
        if (!_forceReload && _source == null && lastUrl_ != null && _url != null && lastUrl_.toExternalForm().equals(_url.toExternalForm())) {
            return;
        }
        final Runnable runnable = new Runnable() {

            public void run() {
                URL u = _url;
                String s = _source;
                String t = _title;
                if (_url != null) {
                    u = getUrlFor(_url);
                    if (u != null) {
                        if (s == null) {
                            s = getSourceFor(_url);
                        }
                        if (t == null) {
                            t = getTitleFor(_url);
                        }
                    }
                }
                if ((u != null) || (s != null)) {
                    setContent0(u, s, t, _forceReload);
                }
            }
        };
        final Thread thread = new Thread(runnable, "Loading " + _url);
        thread.setPriority(Thread.MIN_PRIORITY + 1);
        thread.start();
    }

    public URL getUrlWithoutAnchor(final URL _url) {
        if (_url == null) {
            return null;
        }
        final String url = _url.toString();
        final int idx = url.indexOf('#');
        if (idx > 0) {
            try {
                return new URL(url.substring(0, idx));
            } catch (final MalformedURLException e) {
                FuLog.warning(e);
            }
        }
        return _url;
    }

    protected void setContent0(final URL _url, final String _source, final String _title, final boolean _forceRefresh) {
        final boolean mustReRead = _forceRefresh || _source != null || _url == null || lastUrl_ == null || !(getUrlWithoutAnchor(_url).toExternalForm().equals(getUrlWithoutAnchor(lastUrl_).toExternalForm()));
        if (mustReRead) {
            final Runnable runnable = new Runnable() {

                public void run() {
                    final JViewport v = getViewport();
                    v.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    v.remove(html_);
                    if (frame_ != null && _url != null) {
                        frame_.setMessage(CtuluLib.getS("Chargement" + CtuluLibString.ESPACE + _url.toExternalForm()));
                    }
                    v.repaint();
                }
            };
            BuLib.invokeNow(runnable);
        }
        final HTMLDocument hd = mustReRead ? (HTMLDocument) kit_.createDefaultDocument() : null;
        if (_source != null) {
            lastUrl_ = null;
            try {
                kit_.read(new StringReader(_source), hd, 0);
            } catch (final Exception ex) {
                String source = FuLib.replace(_source, "&", "&amp;");
                source = FuLib.replace(source, "\"", "&quot;");
                source = FuLib.replace(source, "<", "&lt;");
                source = FuLib.replace(source, ">", "&gt;");
                setError(_url, "The HTML is not valid.<BR><PRE>" + source + "</PRE>");
                return;
            }
        } else if (_url != null) {
            if (mustReRead) {
                try {
                    hd.setBase(_url);
                    hd.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                    kit_.read(new InputStreamReader(getInputStreamFor(_url), "iso-8859-1"), hd, 0);
                } catch (final Exception _ex) {
                    setError(_url, CtuluLib.getS("Le document n'est pas accessible") + ".<BR>" + _url.toExternalForm());
                    return;
                }
            }
            lastUrl_ = _url;
        }
        if (mustReRead) {
            updateStyles(hd.getStyleSheet());
            try {
                html_.setDocument(hd);
            } catch (final Exception ex2) {
                setError(_url, CtuluLib.getS("Le document ne peut pas �tre affich�") + ".<BR>" + getExternForm(_url));
                return;
            }
            try {
                html_.getPreferredSize();
            } catch (final Exception ex1) {
                final StyleSheet ss = hd.getStyleSheet();
                final StyleSheet[] as = ss.getStyleSheets();
                if (as != null) {
                    for (int i = 0; i < as.length; i++) {
                        ss.removeStyleSheet(as[i]);
                    }
                }
                final Enumeration rules = ss.getStyleNames();
                while (rules.hasMoreElements()) {
                    final String name = (String) rules.nextElement();
                    ss.removeStyle(name);
                }
                try {
                    html_.getPreferredSize();
                } catch (final Exception ex2) {
                    setError(_url, CtuluLib.getS("Le document ne peut pas �tre affich�") + ".<BR>" + getExternForm(_url));
                    return;
                }
            }
        }
        final String tt = _title;
        final Runnable runnable = new Runnable() {

            public void run() {
                setContent1(_url, hd, tt, mustReRead);
                frame_.setMessage(CtuluLibString.ESPACE);
            }
        };
        BuLib.invokeLater(runnable);
    }

    private String getExternForm(final URL _url) {
        return (_url == null ? "null" : _url.toExternalForm());
    }

    URL lastUrl_;

    protected BuEditorPane getEditorPane() {
        return html_;
    }

    protected void setContent1(final URL _url, final HTMLDocument _hd, final String _title, final boolean _mustReread) {
        frame_.setUrlText(_url == null ? CtuluLibString.EMPTY_STRING : getUrlText(_url), false);
        if (_mustReread) {
            final Object t = _hd.getProperty(Document.TitleProperty);
            frame_.setTitle(t == null ? _title : t.toString());
            final JViewport v = getViewport();
            v.add(html_);
            v.setCursor(cursor_);
        }
        if (_url != null) {
            final String str = _url.toString();
            final int idx = str.indexOf('#');
            Rectangle r = null;
            final Document d = html_.getDocument();
            if (d instanceof HTMLDocument) {
                final HTMLDocument doc = (HTMLDocument) d;
                if (idx > 0) {
                    final String balise = str.substring(idx + 1).trim();
                    final HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
                    for (; iter.isValid(); iter.next()) {
                        final AttributeSet a = iter.getAttributes();
                        final String nm = (String) a.getAttribute(HTML.Attribute.NAME);
                        if ((nm != null) && nm.equals(balise)) {
                            try {
                                r = html_.modelToView(iter.getStartOffset());
                                if (r != null) {
                                    r.height = viewport.getHeight() - 2;
                                }
                            } catch (final BadLocationException e) {
                                FuLog.warning(e);
                            }
                        }
                    }
                } else {
                    try {
                        r = html_.modelToView(0);
                    } catch (final BadLocationException e) {
                        FuLog.warning(e);
                    }
                }
            }
            if (r != null) {
                html_.scrollRectToVisible(r);
            }
            avant_.add(_url);
            updateVector(avant_);
        }
        frame_.setBackEnabled(avant_.size() > 1);
        frame_.setForwardEnabled(apres_.size() > 0);
        frame_.setMessage(" ");
    }

    protected void showProxy() {
        final BuPreferencesMainPanel pn = new BuPreferencesMainPanel();
        pn.addTab(new BuNetworkPreferencesPanel());
        final Frame f = CtuluLibSwing.getFrameAncestorHelper(this);
        final BuPreferencesDialog d = new BuPreferencesDialog(f, pn);
        d.setLocationRelativeTo(f);
        d.show();
        d.dispose();
    }

    public void setError(final URL _url, final String _text) {
        if (Fu.DEBUG && FuLog.isDebug()) {
            FuLog.debug("WBP: setError " + _url + " " + _text);
        }
        final Runnable runnable = new Runnable() {

            public void run() {
                if (_url != null) {
                    frame_.setUrlText(_url.toExternalForm(), false);
                }
                frame_.setTitle(BuResource.BU.getString("Erreur"));
                html_.setCursor(cursor_);
                String t = _text;
                if (_url != null && "http".equals(_url.getProtocol())) {
                    t += "<BR>" + CtuluLib.getS("Il se peut que vos param�tres de connexion ne soient pas � jour") + "<BR>&nbsp;&nbsp;<a href=\"ShowProxy\">" + CtuluLib.getS("V�rifier vos param�tres de connexion") + "</a>";
                }
                if (t == null) {
                    t = "<BR>" + CtuluLib.getS("Cette adresse ne peut pas �tre affich�e");
                    t += (_url != null) ? ":<BR> " + _url.toExternalForm() : CtuluLibString.DOT;
                }
                setContent(_url, "<HTML><BODY>" + t + "</BODY></HTML>", BuResource.BU.getString("Erreur"), true);
            }
        };
        BuLib.invokeLater(runnable);
    }

    private static void updateStyles(final StyleSheet _styles) {
        if (_styles != null) {
            final int font = 12 * BuPreferences.BU.getFontScaling() / 100;
            final Enumeration rules = _styles.getStyleNames();
            while (rules.hasMoreElements()) {
                final String name = (String) rules.nextElement();
                final Style rule = _styles.getStyle(name);
                if (StyleConstants.ALIGN_JUSTIFIED == StyleConstants.getAlignment(rule)) {
                    StyleConstants.setAlignment(rule, StyleConstants.ALIGN_LEFT);
                }
                if (StyleConstants.getFontSize(rule) <= font) {
                    StyleConstants.setFontSize(rule, font);
                }
            }
        }
    }
}
