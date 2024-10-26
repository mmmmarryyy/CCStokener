package YouTube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author H3R3T1C
 */
public class PraseHTML {

    public PraseHTML() {
    }

    public String getURL(String address) {
        String out = "http://www.youtube.com/get_video?video_id=";
        String Buffer = sendGetRequest(address.substring(0, address.lastIndexOf("&")), "");
        String code = "";
        code = address.substring(address.indexOf("=") + 1, address.lastIndexOf("&"));
        out += code + "&t=";
        code = Buffer.substring(Buffer.indexOf("\"t\": ") + 6, Buffer.indexOf("\"t\": ") + 6 + 46);
        System.out.println(out + code);
        return out + code;
    }

    public static String sendGetRequest(String endpoint, String requestParameters) {
        String result = null;
        if (endpoint.startsWith("http://")) {
            try {
                StringBuffer data = new StringBuffer();
                String urlStr = endpoint;
                if (requestParameters != null && requestParameters.length() > 0) {
                    urlStr += "?" + requestParameters;
                }
                URL url = new URL(urlStr);
                URLConnection conn = url.openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
