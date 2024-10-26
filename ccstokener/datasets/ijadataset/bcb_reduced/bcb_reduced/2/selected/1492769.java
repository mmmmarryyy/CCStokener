package test.cnoja.jmsncn;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.net.www.http.PosterOutputStream;
import junit.framework.TestCase;

public class TestAccessServer extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testAccess() {
        try {
            String data = "VER 1 MSNP14 MSNP13 CVR0\r\n";
            data = URLEncoder.encode(data, "UTF-8");
            String urlStr = "http://gateway.messenger.hotmail.com/gateway/gateway.dll?" + URLEncoder.encode("Action=open&Server=NS&IP=messenger.hotmail.com HTTP/1.1", "utf-8");
            URL url = new URL(urlStr);
            HttpURLConnection msnCon = (HttpURLConnection) url.openConnection();
            msnCon.setFollowRedirects(false);
            msnCon.setDoOutput(true);
            msnCon.setDoInput(true);
            msnCon.setRequestProperty("Accept", "*/*");
            msnCon.setRequestProperty("Accept-Language", "en-us");
            msnCon.setRequestProperty("Accept-Encoding", "gzip, deflate");
            msnCon.setRequestProperty("User-Agent", "MSMSGS");
            msnCon.setRequestProperty("Host", "gateway.messenger.hotmail.com");
            msnCon.setRequestProperty("Proxy-Connection", "Keep-Alive");
            msnCon.setRequestProperty("Connection", "Keep-Alive");
            msnCon.setRequestProperty("Pragma", "no-cache");
            msnCon.setRequestProperty("Content-Type", "application/x-msn-messenger");
            msnCon.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            OutputStream out = msnCon.getOutputStream();
            out.write(data.getBytes());
            out.flush();
            printHeaders(msnCon);
            PosterOutputStream outputStream = (PosterOutputStream) out;
            outputStream.write("xxx".getBytes());
            printHeaders(msnCon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printHeaders(HttpURLConnection msnCon) {
        Map<String, List<String>> headers = msnCon.getHeaderFields();
        Set<String> keySet = headers.keySet();
        for (String key : keySet) {
            System.out.println(key + ":" + headers.get(key));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
