package DCL;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author root
 */
public class Browser {

    protected String _url;

    protected Map _params;

    public static String post(String url) {
        Map par = new HashMap();
        return post(url, par, "");
    }

    public static String post(String url, String ld) {
        Map par = new HashMap();
        return post(url, par, ld);
    }

    public static String post(String url, Map params) {
        return post(url, params, "");
    }

    public static String post(String url, Map params, String line_delimiter) {
        String response = "";
        try {
            URL _url = new URL(url);
            URLConnection conn = _url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            String postdata = "";
            int mapsize = params.size();
            Iterator keyValue = params.entrySet().iterator();
            for (int i = 0; i < mapsize; i++) {
                Map.Entry entry = (Map.Entry) keyValue.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (i > 0) postdata += "&";
                postdata += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
            }
            wr.write(postdata);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) response += line + line_delimiter;
            wr.close();
            rd.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return response;
    }

    public Browser() {
    }
}
