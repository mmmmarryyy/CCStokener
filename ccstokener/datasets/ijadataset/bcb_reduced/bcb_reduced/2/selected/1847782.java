package test.cnoja.jmsncn.kernel;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import com.cnoja.jmsncn.kernel.HttpMessengerImpl;

public class TestHttpMessengerImpl extends TestCase {

    private HttpMessengerImpl messenger = null;

    private String officialSite = "http://messenger.hotmail.com:1863";

    private String officialSite2 = "http://gateway.messenger.hotmail.com";

    @Override
    protected void setUp() throws Exception {
        this.messenger = new HttpMessengerImpl();
    }

    public void testConnect() {
        try {
            String adres = "gateway.messenger.hotmail.com";
            String urlStr = "http://" + adres + "/gateway/gateway.dll?";
            urlStr += URLEncoder.encode("Action", "UTF-8") + "=" + URLEncoder.encode("open", "UTF-8");
            urlStr += "&" + URLEncoder.encode("Server", "UTF-8") + "=" + URLEncoder.encode("NS", "UTF-8");
            urlStr += "&" + URLEncoder.encode("IP", "UTF-8") + "=" + URLEncoder.encode("messenger.hotmail.com HTTP/1.1", "UTF-8");
            System.out.println(urlStr);
            String data = "VER 1 MSNP14 MSNP13 CVR0\r\n";
            data = URLEncoder.encode(data, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection msnCon = (HttpURLConnection) url.openConnection();
            msnCon.setRequestMethod("POST");
            msnCon.setFollowRedirects(false);
            msnCon.setDoOutput(true);
            msnCon.setDoInput(true);
            msnCon.setRequestProperty("Accept", URLEncoder.encode("*/*", "UTF-8"));
            msnCon.setRequestProperty("Accept-Language", URLEncoder.encode("en-us", "UTF-8"));
            msnCon.setRequestProperty("Accept-Encoding", URLEncoder.encode("gzip, deflate", "UTF-8"));
            msnCon.setRequestProperty("User-Agent", URLEncoder.encode("MSMSGS", "UTF-8"));
            msnCon.setRequestProperty("Host", URLEncoder.encode("gateway.messenger.hotmail.com", "UTF-8"));
            msnCon.setRequestProperty("Proxy-Connection", URLEncoder.encode("Keep-Alive", "UTF-8"));
            msnCon.setRequestProperty("Connection", URLEncoder.encode("Keep-Alive", "UTF-8"));
            msnCon.setRequestProperty("Pragma", URLEncoder.encode("no-cache", "UTF-8"));
            msnCon.setRequestProperty("Content-Type", URLEncoder.encode("application/x-msn-messenger", "UTF-8"));
            msnCon.setRequestProperty("Content-Length", URLEncoder.encode(data.getBytes().length + "", "UTF-8"));
            OutputStream out = msnCon.getOutputStream();
            out.write(data.getBytes());
            out.flush();
            System.out.println(msnCon.getResponseMessage());
            Map<String, List<String>> headers = msnCon.getHeaderFields();
            System.out.println(headers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        this.messenger = null;
    }
}
