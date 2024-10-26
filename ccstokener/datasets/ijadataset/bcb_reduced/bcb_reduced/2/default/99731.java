import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Download {

    public static void main(String[] args) throws Exception {
        new Thread(new Runnable() {

            @Override
            public void run() {
                File file = new File("spring-security-3.0.7.RELEASE.zip");
                while (true) {
                    try {
                        Thread.sleep(1000 * 60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(file.length());
                }
            }
        }).start();
        System.out.println(download("http://s3.amazonaws.com/dist.springframework.org/release/SEC/spring-security-3.0.7.RELEASE.zip", new File("spring-security-3.0.7.RELEASE.zip")));
    }

    public static boolean download(String url, File file) {
        HttpURLConnection conn = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            if (conn.getResponseCode() == 200) {
                System.out.println("length:" + conn.getContentLength());
                in = new BufferedInputStream(conn.getInputStream());
                out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] b = new byte[1024 << 10];
                int i = 0;
                while ((i = in.read(b)) > -1) {
                    if (i > 0) out.write(b, 0, i);
                }
                return true;
            } else {
                System.out.println(conn.getResponseCode() + ":" + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (conn != null) conn.disconnect();
        }
        return false;
    }
}
