import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

public class ClientCustomSSL {

    public static final void main(String[] args) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream(new File("d:\\tomcat.keystore"));
            try {
                trustStore.load(instream, "123456".toCharArray());
            } finally {
                try {
                    instream.close();
                } catch (Exception ignore) {
                }
            }
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
            Scheme sch = new Scheme("https", 8443, socketFactory);
            httpclient.getConnectionManager().getSchemeRegistry().register(sch);
            HttpGet httpget = new HttpGet("https://192.168.0.117:8443");
            System.out.println("executing request" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream input = entity.getContent();
            String contentStr = IOUtils.toString(input);
            System.out.println(contentStr);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }
}
