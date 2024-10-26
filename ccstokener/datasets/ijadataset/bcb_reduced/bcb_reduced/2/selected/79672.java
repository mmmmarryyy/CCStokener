package taxonfinder.wsclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;

public class GoldenRetriever {

    /**
	 * A simple method that hits the golden service to retrieve xml
	 * This code is based on the example give in this following website:
	 * http://kickjava.com/996.htm
	 * @param serviceURL
	 * @param queryString
	 * @return
	 * @throws IOException
	 */
    public static String getServiceContent(String serviceURL) throws IOException {
        URL url = new URL(serviceURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            byte[] buffer = null;
            String stringBuffer = "";
            buffer = new byte[4096];
            int totBytes, bytes, sumBytes = 0;
            totBytes = connection.getContentLength();
            while (true) {
                bytes = is.read(buffer);
                if (bytes <= 0) break;
                stringBuffer = stringBuffer + new String(buffer);
            }
            return stringBuffer;
        }
        return null;
    }

    public static String postServiceContent(String serviceURL, String text) throws IOException {
        URL url = new URL(serviceURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.connect();
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            byte[] buffer = null;
            String stringBuffer = "";
            buffer = new byte[4096];
            int totBytes, bytes, sumBytes = 0;
            totBytes = connection.getContentLength();
            while (true) {
                bytes = is.read(buffer);
                if (bytes <= 0) break;
                stringBuffer = stringBuffer + new String(buffer);
            }
            return stringBuffer;
        }
        return null;
    }

    public static void sendPostRequest() {
        String data = "text=Eschirichia coli";
        try {
            URL url = new URL("http://taxonfinder.ubio.org/analyze?");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            StringBuffer answer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            writer.close();
            reader.close();
            System.out.println(answer.toString());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) {
        sendPostRequest();
    }
}
