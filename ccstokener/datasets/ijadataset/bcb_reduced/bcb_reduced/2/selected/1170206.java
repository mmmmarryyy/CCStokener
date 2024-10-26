package validatorcmd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author leeing
 */
public class Validate implements Runnable {

    private String filepath;

    private String linenum;

    private String url;

    private URL urls;

    private int code;

    private static final Object mutex = new Object();

    private final String LINE_SEP = System.getProperty("line.separator");

    private BufferedWriter bw1;

    public Validate(String filepath, String linenum, String url) throws IOException {
        this.filepath = filepath;
        this.linenum = linenum;
        this.url = url;
        bw1 = new BufferedWriter(new FileWriter("./invalid_urls.txt"));
    }

    @Override
    public void run() {
        System.out.println("Validating url: " + url);
        try {
            urls = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urls.openConnection();
            code = conn.getResponseCode();
            if (code == 200) {
                System.out.println(urls + " is valid");
            } else {
                synchronized (mutex) {
                    bw1 = new BufferedWriter(new FileWriter("./invalid_urls.txt", true));
                    bw1.write(filepath + ";" + linenum + ";" + url + ";HttpCode:" + code + LINE_SEP);
                    bw1.close();
                }
                System.err.println(url + " is invalid, code is " + code);
            }
        } catch (Exception ex) {
            synchronized (mutex) {
                try {
                    bw1 = new BufferedWriter(new FileWriter("./invalid_urls.txt", true));
                    bw1.write(filepath + ";" + linenum + ";" + url + ";Exception: " + ex.getClass().getName() + LINE_SEP);
                    bw1.close();
                } catch (IOException ex1) {
                }
            }
            System.err.println(url + " is invalid, with Exception: " + ex.getMessage() + "," + ex.getCause());
        }
    }
}
