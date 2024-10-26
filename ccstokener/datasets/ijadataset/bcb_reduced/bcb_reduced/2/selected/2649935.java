package zipperSwing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URItoString {

    private String URLString;

    private String HTMLString;

    private String OutputStreamString;

    private String charsetName;

    private HttpURLConnection urlcon;

    public URItoString() {
        this.URLString = "";
        this.HTMLString = "";
        this.OutputStreamString = "";
        this.charsetName = "JISAutoDetect";
        this.urlcon = null;
    }

    public void setURIString(final String s) {
        this.URLString = s;
    }

    public void setOutputStreamString(final String s) {
        this.OutputStreamString = s;
    }

    public void setCharsetName(final String s) {
        this.charsetName = s;
    }

    public URLConnection makeURLcon() {
        URI uri;
        URL url;
        try {
            uri = new URI(this.URLString);
            url = uri.toURL();
            this.urlcon = (HttpURLConnection) url.openConnection();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return this.urlcon;
    }

    public boolean isContentTypeUTF8(HttpURLConnection hr) {
        Pattern p = Pattern.compile("UTF-8");
        Matcher m;
        boolean flag = false;
        for (int i = 1; hr.getHeaderFieldKey(i) != null; i++) {
            m = p.matcher(hr.getHeaderField(i));
            if (m.find()) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public String getHEADHTMLString() {
        HttpURLConnection hr = this.urlcon;
        String headerString = "";
        try {
            hr.setRequestMethod("HEAD");
            for (int i = 1; hr.getHeaderFieldKey(i) != null; i++) {
                headerString += hr.getHeaderFieldKey(i) + " " + hr.getHeaderField(i) + "\n";
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        return headerString;
    }

    public String getGETHTMLString() {
        InputStream is;
        InputStreamReader isr;
        this.HTMLString = "";
        try {
            this.urlcon.setRequestMethod("GET");
            if (isContentTypeUTF8(this.urlcon)) this.setCharsetName("UTF-8");
            is = this.urlcon.getInputStream();
            is = new BufferedInputStream(is);
            isr = new InputStreamReader(is, this.charsetName);
            int c;
            StringBuilder s = new StringBuilder();
            while ((c = isr.read()) != -1) s.append((char) c);
            this.HTMLString = s.toString();
            isr.close();
            is.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return this.HTMLString;
    }

    public String getPOSTHTMLString() {
        InputStream is;
        InputStreamReader isr;
        this.HTMLString = "";
        if (isContentTypeUTF8(this.urlcon)) setCharsetName("UTF-8");
        try {
            this.urlcon.setDoOutput(true);
            this.urlcon.setRequestMethod("POST");
            this.makeOutputStream(this.urlcon, this.OutputStreamString);
            is = this.urlcon.getInputStream();
            is = new BufferedInputStream(is);
            isr = new InputStreamReader(is, this.charsetName);
            int c;
            StringBuilder s = new StringBuilder();
            while ((c = isr.read()) != -1) s.append((char) c);
            this.HTMLString = s.toString();
            isr.close();
            is.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return this.HTMLString;
    }

    private OutputStream makeOutputStream(final HttpURLConnection urlcon, final String s) {
        OutputStream os = null;
        try {
            os = urlcon.getOutputStream();
            os = new BufferedOutputStream(os);
            final OutputStreamWriter out = new OutputStreamWriter(os, "8859_1");
            out.write(s);
            out.flush();
            out.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return os;
    }
}
