import java.net.*;
import java.util.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.xpath.*;

public class PSCalculator {

    private static final String HTML_FILE_NAME = "ps.html";

    public static void main(String... args) throws Exception {
        String g, d, m, f;
        if (args.length > 3) {
            g = args[0];
            d = args[1];
            m = args[2];
            f = args[3];
        } else {
            g = readValue("G:");
            d = readValue("D:");
            m = readValue("M:");
            f = readValue("F:");
        }
        System.out.print("Wait please...");
        StringBuffer contStr = new StringBuffer();
        contStr.append("xajax=getPS&xajaxr=").append(new Date().getTime()).append("&xajaxargs[]=").append(g).append("&xajaxargs[]=").append(d).append("&xajaxargs[]=").append(m).append("&xajaxargs[]=").append(f);
        HttpURLConnection conn = connect("http://pbliga.com/mng_scout_ajax.php", "POST", "application/x-www-form-urlencoded; charset=UTF-8", contStr.toString(), 5000);
        InputStream is = conn.getInputStream();
        XPath xpath = XPathFactory.newInstance().newXPath();
        String value = xpath.evaluate("//xjx/cmd/text()", new InputSource(is));
        is.close();
        File htmlResult = new File(HTML_FILE_NAME);
        if (htmlResult.exists()) htmlResult.delete();
        htmlResult.createNewFile();
        FileWriter fw = new FileWriter(htmlResult);
        fw.write("<font color=\"red\" size=\"+3\">" + g + "-" + d + "-" + m + "-" + f + "</font><br/>");
        fw.write(value);
        fw.flush();
        fw.close();
        Runtime.getRuntime().exec(new String[] { "cmd", "/c", HTML_FILE_NAME });
    }

    static HttpURLConnection connect(String url, String method, String contentType, String content, int timeoutMillis) throws ProtocolException, IOException, MalformedURLException, UnsupportedEncodingException {
        HttpURLConnection conn = (HttpURLConnection) (new URL(url).openConnection());
        conn.setRequestMethod(method);
        conn.setConnectTimeout(timeoutMillis);
        byte[] bContent = null;
        if (content != null && content.length() > 0) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType);
            bContent = content.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(bContent.length);
        }
        conn.connect();
        if (bContent != null) {
            OutputStream os = conn.getOutputStream();
            os.write(bContent);
            os.flush();
            os.close();
        }
        return conn;
    }

    static String getContentCharset(String contentType) {
        int cInd = contentType.indexOf("charset=");
        if (cInd >= 0) {
            return contentType.substring(cInd + 8);
        }
        return "UTF-8";
    }

    static String readStream(InputStream is) throws IOException {
        byte[] buff = new byte[1024];
        int readCount = 0;
        StringBuffer result = new StringBuffer();
        while ((readCount = is.read(buff)) > 0) {
            result.append(new String(buff, 0, readCount));
        }
        return result.toString();
    }

    static String readValue(String message) throws IOException {
        System.out.print(message);
        StringBuffer result = new StringBuffer();
        int key;
        int count = 0;
        while ((key = System.in.read()) > 0) {
            if (key < 32) {
                if (count > 0) {
                    break;
                } else {
                    continue;
                }
            }
            result.append((char) key);
            count++;
        }
        return result.toString();
    }
}
