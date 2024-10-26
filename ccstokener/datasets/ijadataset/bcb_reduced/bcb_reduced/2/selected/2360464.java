package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class SkyDriveUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static String tmp;

    private static BufferedReader br;

    private static String loginurl;

    private static String msprcookie = "";

    private static String mspokcookie = "";

    private static PrintWriter pw;

    private static String ppft;

    private static String location;

    public static void main(String[] args) throws Exception {
        initialize();
        loginSkyDrive();
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    private static void initialize() throws Exception {
        System.out.println("Getting startup cookie from login.live.com");
        u = new URL("https://login.live.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("MSPRequ")) {
                    msprcookie = tmp;
                }
                if (tmp.contains("MSPOK")) {
                    mspokcookie = tmp;
                }
            }
        }
        msprcookie = msprcookie.substring(0, msprcookie.indexOf(";"));
        mspokcookie = mspokcookie.substring(0, mspokcookie.indexOf(";"));
        System.out.println("msprcookie : " + msprcookie);
        System.out.println("mspokcookie : " + mspokcookie);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        loginurl = parseResponse(k, "srf_uPost='", "'");
        System.out.println("Login URL : " + loginurl);
        ppft = parseResponse(k, "value=\"", "\"");
        System.out.println(ppft);
    }

    public static void loginSkyDrive() throws Exception {
        System.out.println("login ");
        u = new URL(loginurl);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", msprcookie + ";" + mspokcookie);
        uc.setDoOutput(true);
        uc.setRequestMethod("POST");
        uc.setInstanceFollowRedirects(false);
        pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream()), true);
        pw.print("login=dinesh007007%40hotmail.com&passwd=&SI=Sign+in&type=11&LoginOptions=3&NewUser=1&MEST=&PPSX=Passpor&PPFT=" + ppft + "&PwdPad=&sso=&i1=&i2=1&i3=10524&i4=&i12=1&i13=&i14=437&i15=624&i16=3438");
        pw.flush();
        pw.close();
        System.out.println(uc.getResponseCode());
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                System.out.println(tmp);
            }
        }
        location = uc.getHeaderField("Location");
        System.out.println("Location : " + location);
        System.out.println("going to open paaport page");
        DefaultHttpClient d = new DefaultHttpClient();
        HttpGet hg = new HttpGet("https://skydrive.live.com");
        hg.setHeader("Cookie", msprcookie + ";" + mspokcookie);
        HttpResponse execute = d.execute(hg);
        HttpEntity entity = execute.getEntity();
        System.out.println(EntityUtils.toString(entity));
        System.out.println(execute.getStatusLine());
        Header[] allHeaders = execute.getAllHeaders();
        for (int i = 0; i < allHeaders.length; i++) {
            System.out.println(allHeaders[i].getName() + " : " + allHeaders[i].getValue());
        }
    }

    private static void getData(String myurl) throws Exception {
        u = new URL(myurl);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Referer", "https://login.live.com/");
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
    }
}
