package com.corratech.opensuite.zimbra;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;

public class ZimbraRestClient {

    private static final Logger log = Logger.getLogger(ZimbraRestClient.class.getName());

    protected String URL_REST_MESSAGE = "/service/home/~/";

    protected String username = null;

    protected String password = null;

    protected String host = null;

    protected ZimbraRestClient() {
    }

    public ZimbraRestClient(String host, String usr, String pwd) {
        this.host = host;
        this.username = usr;
        this.password = pwd;
    }

    protected void authenticateRequest(HttpURLConnection conn) {
        String cred = String.format("%1$s:%2$s", username, password);
        String encoded = new String(Base64.encodeBase64(cred.getBytes()));
        conn.setRequestProperty("Authorization", " Basic " + encoded);
    }

    protected byte[] downloadData(String urlS) throws Exception {
        byte[] data = null;
        try {
            URL url = new URL(urlS);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            authenticateRequest(conn);
            int respCode = conn.getResponseCode();
            log.info("response code = " + respCode);
            if (respCode == 200) {
                InputStream in = conn.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }
                data = bos.toByteArray();
                bos.close();
                conn.disconnect();
                log.info("Response read");
            }
        } catch (Exception ex) {
            throw new Exception("The atachment can not be dowloaded by this URL: " + urlS, ex);
        }
        return data;
    }

    public byte[] downloadAttachment(int messageid, int part) throws Exception {
        String urlS = String.format("%1$s%2$s?id=%3$d&part=%4$d", this.host, URL_REST_MESSAGE, messageid, part);
        return downloadData(urlS);
    }

    public byte[] downloadAttachment(String url) throws Exception {
        return downloadData(url);
    }
}
