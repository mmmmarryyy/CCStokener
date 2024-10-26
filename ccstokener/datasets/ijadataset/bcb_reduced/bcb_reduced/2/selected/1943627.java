package com.insanityengine.ghia.util;

import java.io.*;
import java.net.*;
import java.awt.event.*;
import com.insanityengine.ghia.events.*;

/**
 *
 * <P>
 * Takes html and pulls out href and anchors which is sends to listeners...
 * </P>
 *
 * @author BrianHammond
 *
 * $Header: /cvsroot/ghia/ghia/src/java/com/insanityengine/ghia/util/Hreferee.java,v 1.4 2006/11/29 06:15:30 brianin3d Exp $
 *
 */
public class Hreferee extends ActionEventGenerator implements ActionListener {

    public static final void main(String argv[]) {
        Hreferee href = new Hreferee();
        href.addActionListener(href);
        if (0 == argv.length) {
            href.ref(System.in);
        } else {
            for (int i = 0; i < argv.length; i++) href.ref(argv[i]);
        }
    }

    public Hreferee() {
    }

    /**
	 *
	 * Chunk up a stream to produce events in the form of HREF&gt;ANCHOR
	 *
	 * @param href to parse
	 *
	 */
    public void ref(String href) {
        try {
            ref(new URL(href));
        } catch (Exception e) {
        }
    }

    /**
	 *
	 * Chunk up a stream to produce events in the form of HREF&gt;ANCHOR
	 *
	 * @param url to parse
	 *
	 */
    public void ref(URL url) {
        try {
            setUrl(url);
            ref(url.openStream());
        } catch (Exception e) {
        }
    }

    /**
	 *
	 * Chunk up a stream to produce events in the form of HREF&gt;ANCHOR
	 *
	 * @param in stream
	 *
	 */
    public void ref(InputStream in) {
        _init();
        char c;
        byte[] bytes = new byte[1024];
        int i;
        int len = 0;
        StringBuffer buf = new StringBuffer();
        try {
            while (-1 != (len = in.read(bytes))) {
                for (i = 0; i < len; ++i) {
                    c = (char) bytes[i];
                    if ('\n' != c && '\r' != c) {
                        if ('<' == c) {
                            ref(buf);
                            buf = new StringBuffer();
                        }
                        buf.append(c);
                    }
                }
            }
        } catch (Exception e) {
        }
        ref(buf);
    }

    /**
	 *
	 * For ActionListener interface
	 *
	 * @param event to act on
	 *
	 */
    public void actionPerformed(ActionEvent event) {
        com.insanityengine.ghia.util.SimpleLogger.info(event.getActionCommand());
    }

    /**
	  *
	  * Set the value
	  *
	  * @param newValue to use
	  *
	  */
    URL getUrl() {
        return url;
    }

    /**
	  *
	  * Set the value
	  *
	  * @param newValue to use
	  *
	  */
    void setUrl(URL newValue) {
        url = newValue;
    }

    /**
	 *
	 *
	 */
    private void ref(StringBuffer buf) {
        String lower = lowerIt(buf, 10);
        if (!inScript) {
            inScript = (0 != lower.indexOf("</script"));
        } else {
            inScript = (0 == lower.indexOf("<script"));
        }
        if (!inScript && !inStyle) refA(buf);
    }

    /**
	 *
	 *
	 */
    private void refA(StringBuffer buf) {
        if (0 == lowerIt(buf, 2).indexOf("<a")) {
            fireActionPerformed(absUrl(href(buf)) + ">" + anchor(buf));
        }
    }

    private String absUrl(String href) {
        if (null != getUrl()) {
            try {
                URL url = new URL(getUrl(), href);
                href = url.toExternalForm();
            } catch (Exception e) {
            }
        }
        return href;
    }

    /**
	 *
	 *
	 */
    private static String href(StringBuffer buf) {
        int hrefIdx = lowerIt(buf).indexOf("href");
        if (-1 == hrefIdx) return "";
        String notIn = " =\"'\\>";
        for (hrefIdx += 4; hrefIdx < buf.length(); hrefIdx++) {
            if (-1 == notIn.indexOf(buf.charAt(hrefIdx))) {
                break;
            }
        }
        int space = buf.substring(hrefIdx).indexOf(" ");
        int close = buf.indexOf(">");
        if (-1 != space && space < close) close = space;
        if (-1 == close) {
            close = buf.length();
        } else {
            for (close--; close > hrefIdx; close--) {
                if (-1 == notIn.indexOf(buf.charAt(close))) {
                    close++;
                    break;
                }
            }
        }
        return buf.substring(hrefIdx, close);
    }

    /**
	 *
	 *
	 */
    private static String anchor(StringBuffer buf) {
        return buf.substring(1 + buf.indexOf(">"));
    }

    /**
	 *
	 *
	 */
    private static final String lowerIt(StringBuffer buf, int n) {
        int idx = buf.length();
        if (idx > n) idx = n;
        return buf.substring(0, idx).toLowerCase();
    }

    /**
	 *
	 *
	 */
    private static final String lowerIt(StringBuffer buf) {
        return buf.toString().toLowerCase();
    }

    /**
	 *
	 *
	 */
    private void _init() {
        inScript = inStyle = false;
    }

    private boolean inScript, inStyle;

    private URL url = null;
}

;
