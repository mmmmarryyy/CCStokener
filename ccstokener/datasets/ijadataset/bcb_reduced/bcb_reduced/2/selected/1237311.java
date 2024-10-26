package wtkx;

import wtkx.be.Function;
import wtkx.io.UrlcOutputStream;
import wtkx.ui.Cursor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Multiple lines editable.
 *
 * @author jdp
 */
public class Textarea extends P {

    public Textarea() {
        super();
        this.editable = true;
        this.setCursor(Cursor.TEXT);
        this.setView(wtkx.ui.View.Vertical);
        this.transfersFocus = false;
    }

    public void layout() {
        super.layout();
    }

    public final void insertParagraph() {
        this.insertText('\n');
    }

    public boolean save() {
        URL src = this.src;
        if (null != src) {
            String string = this.getText();
            if (null != string) {
                try {
                    byte[] utf = null;
                    int len = 0;
                    {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        Writer writer = new OutputStreamWriter(buf, UTF8);
                        writer.write(string);
                        writer.flush();
                        utf = buf.toByteArray();
                        len = ((null != utf) ? (utf.length) : (0));
                    }
                    URLConnection urlc = src.openConnection();
                    if (urlc instanceof HttpURLConnection) {
                        HttpURLConnection http = (HttpURLConnection) urlc;
                        http.setRequestMethod("PUT");
                        http.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
                        http.setRequestProperty("Content-Length", String.valueOf(len));
                    }
                    UrlcOutputStream out = this.writer(this, urlc);
                    try {
                        out.write(utf, 0, len);
                    } finally {
                        out.close();
                    }
                    if (out.isNotOk()) this.alertBroken("Save failed");
                    this.invalidate();
                    return true;
                } catch (IOException exc) {
                    exc.printStackTrace();
                    this.alertBroken(exc);
                }
            }
        }
        return false;
    }
}
