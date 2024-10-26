package app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.regex.Pattern;
import com.google.gson.Gson;

public class GoogleServices {

    private static final String URL_GOOGL_SERVICE = "https://www.googleapis.com/urlshortener/v1/url";

    private static final Gson gson = new Gson();

    public static String shorten(String longUrl) {
        String result = new String();
        GsonGooGl gsonGooGl = new GsonGooGl(longUrl);
        try {
            URL url = new URL(URL_GOOGL_SERVICE);
            URLConnection urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/json");
            DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
            String content = gson.toJson(gsonGooGl);
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            DataInputStream input = new DataInputStream(urlConn.getInputStream());
            Scanner sc = new Scanner(input);
            while (sc.hasNext()) {
                result += sc.next();
            }
            GooGlResult gooGlResult = gson.fromJson(result, GooGlResult.class);
            return gooGlResult.getId();
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }
}

class GsonGooGl {

    public GsonGooGl() {
    }

    public GsonGooGl(String longUrl) {
        this.longUrl = longUrl;
    }

    private String longUrl;

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }
}

class GooGlResult {

    public GooGlResult() {
    }

    private String kind;

    private String id;

    private String longUrl;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }
}
