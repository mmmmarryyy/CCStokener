package fueltrack.server.motormouth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.SAXException;

public class MotorMouthService {

    public static InputStream retrievePricesHTML(String username, String password) throws IOException, SAXException {
        List<String> cookies = new ArrayList<String>();
        URL url = new URL("http://motormouth.com.au/default_fl.aspx");
        HttpURLConnection loginConnection = (HttpURLConnection) url.openConnection();
        String viewStateValue = HTMLParser.parseHTMLInputTagValue(new InputStreamReader(loginConnection.getInputStream()), "__VIEWSTATE");
        setCookies(cookies, loginConnection);
        HttpURLConnection postCredsConnection = (HttpURLConnection) url.openConnection();
        postCredsConnection.setDoOutput(true);
        postCredsConnection.setRequestMethod("POST");
        postCredsConnection.setInstanceFollowRedirects(false);
        postCredsConnection.setRequestProperty("Cookie", buildCookieString(cookies));
        OutputStreamWriter postCredsWriter = new OutputStreamWriter(postCredsConnection.getOutputStream());
        postCredsWriter.append("__VIEWSTATE=").append(URLEncoder.encode(viewStateValue, "UTF-8")).append('&');
        postCredsWriter.append("Login_Module1%3Ausername=").append(URLEncoder.encode(username, "UTF-8")).append('&');
        postCredsWriter.append("Login_Module1%3Apassword=").append(URLEncoder.encode(password, "UTF-8")).append('&');
        postCredsWriter.append("Login_Module1%3AButtonLogin.x=0").append('&');
        postCredsWriter.append("Login_Module1%3AButtonLogin.y=0");
        postCredsWriter.flush();
        postCredsWriter.close();
        int postResponseCode = postCredsConnection.getResponseCode();
        if (postResponseCode == 302) {
            setCookies(cookies, postCredsConnection);
            URL dataUrl = new URL(url, postCredsConnection.getHeaderField("Location"));
            HttpURLConnection dataConnection = (HttpURLConnection) dataUrl.openConnection();
            dataConnection.setRequestProperty("Cookie", buildCookieString(cookies));
            InputStream dataInputStream = dataConnection.getInputStream();
            return dataInputStream;
        } else if (postResponseCode == 200) {
            URL dataUrl = new URL(url, "/secure/mymotormouth.aspx");
            HttpURLConnection dataConnection = (HttpURLConnection) dataUrl.openConnection();
            dataConnection.setRequestProperty("Cookie", buildCookieString(cookies));
            InputStream dataInputStream = dataConnection.getInputStream();
            return dataInputStream;
        } else {
            return null;
        }
    }

    private static void setCookies(List<String> cookies, HttpURLConnection loginConnection) {
        List<String> cookieHeaders = loginConnection.getHeaderFields().get("set-cookie");
        if (cookieHeaders != null) {
            for (String cookieString : cookieHeaders) {
                cookies.add(cookieString.split(";")[0]);
            }
        }
        cookieHeaders = loginConnection.getHeaderFields().get("Set-Cookie");
        if (cookieHeaders != null) {
            for (String cookieString : cookieHeaders) {
                cookies.add(cookieString.split(";")[0]);
            }
        }
    }

    private static String buildCookieString(List<String> cookies) {
        StringBuilder builder = new StringBuilder();
        for (String cookie : cookies) {
            builder.append(cookie).append("; ");
        }
        return (builder.length() > 2 ? builder.substring(0, builder.length() - 2) : "");
    }
}
