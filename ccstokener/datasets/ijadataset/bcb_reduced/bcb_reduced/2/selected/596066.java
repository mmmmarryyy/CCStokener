package hr.fer.pus.dll_will.sp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import javax.servlet.ServletOutputStream;

public class RemoteUtils {

    /**
	 * Za citanje sa odredjene URL adrese
	 */
    public static String readFromAddress(String address) throws Exception {
        StringBuilder sb = new StringBuilder();
        URL url = new URL(address);
        URLConnection con = url.openConnection();
        con.connect();
        InputStream is = (InputStream) con.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while (true) {
            String redak = br.readLine();
            if (redak == null) break;
            sb.append(redak);
            sb.append(System.getProperty("line.separator"));
        }
        br.close();
        return sb.toString();
    }

    public static String postToAddress(Map<String, String> params, String address) throws Exception {
        String data = "";
        String separator = "";
        for (String key : params.keySet()) {
            data += separator + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(params.get(key), "UTF-8");
            separator = "&";
        }
        System.out.println("sending: " + data);
        URL url = new URL(address);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line + System.getProperty("line.separator"));
        }
        wr.close();
        rd.close();
        return sb.toString();
    }
}
