import java.net.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.io.*;
import javax.swing.JLabel;

public class HTTP {

    public static String[] cookies = new String[1];

    public static String post(String strUrl, String strPostString) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(true);
            conn.setAllowUserInteraction(true);
            conn.setFollowRedirects(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(strPostString);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String s = "";
            String sRet = "";
            while ((s = in.readLine()) != null) {
                sRet += s;
            }
            in.close();
            return sRet;
        } catch (MalformedURLException e) {
            System.out.println("Internal Error. Malformed URL.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Internal I/O Error.");
            e.printStackTrace();
        }
        return "";
    }

    public static String get(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(true);
            conn.setAllowUserInteraction(true);
            conn.setFollowRedirects(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent:", "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; de-de) AppleWebKit/523.12.2 (KHTML, like Gecko) Version/3.0.4 Safari/523.12.2");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String s = "";
            String sRet = "";
            while ((s = in.readLine()) != null) {
                sRet += '\n' + s;
            }
            return sRet;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void download(String strUrl, String strFileDest) {
        try {
            URL url = new URL(strUrl);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(strFileDest));
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while (((numRead = in.read(buffer)) != -1) && (!Thread.currentThread().isInterrupted())) {
                out.write(buffer, 0, numRead);
            }
            in.close();
            out.close();
        } catch (MalformedURLException e) {
            System.out.println("Internal Error. Malformed URL.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("Internal Error. Could not find file.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Internal I/O Error.");
            e.printStackTrace();
        }
    }
}
