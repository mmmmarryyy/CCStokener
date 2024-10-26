import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: Rajeev
 * Date: Nov 11, 2009
 * Time: 8:39:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class PartialGetter {

    public static void main(String[] args) {
        new PartialGetter().doit();
    }

    public void doit() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter URL:");
            url = br.readLine().trim();
            System.out.println("Enter destination path with the desired file name to save:");
            File file = null;
            try {
                file = new File(br.readLine().trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Enter max download limit (in kB):");
            try {
                downLimit = Integer.parseInt(br.readLine().trim());
                downLimit -= 10;
            } catch (Exception e) {
                e.printStackTrace();
                downLimit = 1000;
            }
            downLimit *= 1000;
            client = new DefaultHttpClient();
            HttpHost proxy = new HttpHost("cache.mrt.ac.lk", 3128);
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            byte[] buf = new byte[10000];
            int len = 0;
            file.createNewFile();
            FileOutputStream f = new FileOutputStream(file);
            while (!complete) {
                InputStream in = getPart();
                if (in != null) {
                    while ((len = in.read(buf)) >= 0) {
                        f.write(buf, 0, len);
                    }
                    f.flush();
                }
            }
            f.flush();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean complete = false;

    long startAt = 0;

    int downLimit = 1000000;

    String url = null;

    HttpClient client = null;

    private InputStream getPart() throws IOException {
        HttpGet get = new HttpGet(url);
        get.addHeader("Range", "bytes=" + startAt + "-");
        HttpResponse res = client.execute(get);
        System.out.println("requesting kBs from " + startAt + "     server reply:" + res.getStatusLine());
        if (res.getStatusLine().getStatusCode() == 403 || res.getStatusLine().toString().toLowerCase().contains("forbidden")) {
            get.abort();
            get = new HttpGet(url);
            get.addHeader("Range", "bytes=" + startAt + "-" + (startAt + downLimit));
            res = client.execute(get);
            System.out.println("Again requesting from kBs " + startAt + "     server reply:" + res.getStatusLine());
            startAt += downLimit;
        } else {
            complete = true;
        }
        return res.getEntity() == null ? null : res.getEntity().getContent();
    }
}
