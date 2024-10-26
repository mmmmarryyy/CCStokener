package horizons.retriever;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;

public class MyHttpClient {

    private String urlStr;

    private File lastResult;

    private String cookie;

    public MyHttpClient(String webPage) {
        urlStr = webPage;
    }

    public void executeMethod(Hashtable<String, String> data) {
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String urlParameters = getUrlParameters(data);
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            if (cookie != null) {
                connection.setRequestProperty("Cookie", cookie);
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            int i = 1;
            String hdrKey = null;
            while ((hdrKey = connection.getHeaderFieldKey(i)) != null) {
                if (hdrKey.equals("Set-Cookie")) {
                    cookie = connection.getHeaderField(i);
                }
                i++;
            }
            InputStream is = connection.getInputStream();
            lastResult = File.createTempFile("cached", "horizon");
            FileOutputStream out = new FileOutputStream(lastResult);
            final int BUFFER_SIZE = 1 << 10 << 6;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = is.read(buffer)) > -1) {
                out.write(buffer, 0, bytesRead);
            }
            is.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getUrlParameters(Hashtable<String, String> data) throws UnsupportedEncodingException {
        StringBuffer urlParameters = new StringBuffer();
        Enumeration<String> keys = data.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            urlParameters.append(String.format("%s=%s", key, URLEncoder.encode(data.get(key), "UTF-8")));
            if (keys.hasMoreElements()) {
                urlParameters.append("&");
            }
        }
        return urlParameters.toString();
    }

    public InputStream getResponseBodyAsStream() throws FileNotFoundException {
        return new FileInputStream(lastResult);
    }
}
