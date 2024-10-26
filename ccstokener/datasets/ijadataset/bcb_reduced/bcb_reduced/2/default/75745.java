import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 *
 * @author Shirkit
 */
public class Internet {

    public static void main(String[] args) {
        try {
            System.setProperty("javax.net.ssl.trustStore", "C:\\xampp\\apache\\conf\\ssl.key\\server.key");
            System.setProperty("javax.net.ssl.trustStorePassword", "171089");
            URL url = new URL("https://localhost/teso.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            String data = URLEncoder.encode("ano", "UTF-8") + "=" + URLEncoder.encode("seliga", "UTF-8");
            os.write(data.getBytes());
            os.flush();
            os.close();
            InputStream istream = conn.getInputStream();
            byte[] buff = new byte[1024];
            String s = "";
            while ((istream.read(buff)) > 0) {
                s += s + new String(buff);
            }
            System.out.println(s);
            istream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
