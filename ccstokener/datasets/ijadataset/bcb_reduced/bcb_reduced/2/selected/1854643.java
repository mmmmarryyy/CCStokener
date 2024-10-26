package cn.edu.wuse.musicxml.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpInputContext extends URLInputContext {

    private boolean isDirect;

    private HttpURLConnection huc;

    public HttpInputContext(String url) {
        super(url);
        distinguishURL();
    }

    public String getFilename() {
        filename = "";
        if (huc == null) {
            try {
                huc = (HttpURLConnection) new URL(urlString).openConnection();
                huc.setRequestProperty("Connection", "Keep-Alive");
                if (!isDirect) {
                    int code = huc.getResponseCode();
                    if (code == 200) {
                        String sHeader;
                        for (int i = 1; ; i++) {
                            sHeader = huc.getHeaderFieldKey(i);
                            if (sHeader != null) {
                                if (sHeader.toLowerCase().equals("content-disposition")) {
                                    String val = huc.getHeaderField(sHeader);
                                    int sind = val.toLowerCase().indexOf("filename=");
                                    if (sind != -1) {
                                        String sub = val.substring(sind);
                                        filename = myTrim(sub);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filename;
    }

    public InputStream getStream() throws Exception {
        if (huc == null) getFilename();
        return huc.getInputStream();
    }

    private String myTrim(String s) {
        byte[] b = s.getBytes();
        for (int i = 0; i < b.length; i++) if (b[i] == '\'' || b[i] == '\"') b[i] = 32;
        return new String(b).trim();
    }

    private void distinguishURL() {
        int ind = urlString.lastIndexOf("?");
        if (ind != -1) {
            int sof = 0;
            for (int i = ind; i >= 6; i--) {
                if (urlString.charAt(i) == '/') {
                    sof = i;
                    break;
                }
            }
            String fn = urlString.substring(sof + 1, ind);
            if (fn.toLowerCase().endsWith(".xml") || fn.toLowerCase().endsWith(".mxl")) {
                filename = fn;
                isDirect = true;
            }
        }
    }

    public boolean isDirect() {
        return isDirect;
    }
}
