package org.vastenhouw.jphotar.help;

import java.awt.Container;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;
import org.vastenhouw.util.Debug;

public class WebScrollPane extends JScrollPane {

    private JTextPane WebTextPane;

    private Vector historyVect;

    private int histStop;

    private int histCurr;

    public WebScrollPane() {
        WebTextPane = new JTextPane() {

            protected InputStream getStream(URL url) throws IOException {
                return url.openStream();
            }
        };
        histStop = 0;
        histCurr = -1;
        historyVect = new Vector(1);
        WebTextPane.setToolTipText("");
        WebTextPane.setEditorKit(new HTMLEditorKit());
        WebTextPane.setEditable(false);
        WebTextPane.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent hyperlinkevent) {
                WebTextPane_hyperlinkUpdate(hyperlinkevent);
            }
        });
        getViewport().add(WebTextPane, null);
    }

    private void WebTextPane_hyperlinkUpdate(HyperlinkEvent hyperlinkevent) {
        if (hyperlinkevent.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            try {
                gotoPage(hyperlinkevent.getURL());
            } catch (IOException _ex) {
                _ex.printStackTrace(Debug.out);
            }
        }
    }

    private void changePage(URL url) throws IOException {
        WebTextPane.setPage(url);
    }

    public URL getPage() {
        URL url = null;
        try {
            url = new URL(((URL) historyVect.get(histCurr)).toString());
        } catch (MalformedURLException _ex) {
            _ex.printStackTrace(Debug.out);
        }
        return url;
    }

    public void gotoPage(String s) throws IOException, MalformedURLException {
        gotoPage(new URL(s));
    }

    public void gotoPage(URL url) throws IOException {
        changePage(url);
        if (histCurr >= 0 && ((URL) historyVect.get(histCurr)).equals(url)) {
            return;
        }
        histCurr++;
        if (histCurr >= historyVect.size()) {
            historyVect.add(new URL(url.toString()));
        } else {
            historyVect.set(histCurr, new URL(url.toString()));
        }
        histStop = histCurr;
    }

    public void nextPage() throws IOException {
        if (histCurr < histStop) {
            histCurr++;
            changePage((URL) historyVect.get(histCurr));
        }
    }

    public void prevPage() throws IOException {
        if (histCurr > 0) {
            histCurr--;
            changePage((URL) historyVect.get(histCurr));
        }
    }
}
