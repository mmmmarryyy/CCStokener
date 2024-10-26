package sk.tuke.ess.lib.generator.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLHandshakeException;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author popko
 */
public class HttpNumberFetcher implements Runnable {

    public static int NUMBER_FETCH_COUNT = 500;

    public static String EMAIL = "tomino.fcbk@gmail.com";

    private static final String GET_NUMBERS_FORMAT_STRING = "http://www.random.org/integers/?num=%d&min=0&max=65535&col=2&base=16&format=plain&rnd=%s";

    private static HttpGet getNumbers;

    private static HttpGet getQuota;

    private static DefaultHttpClient client;

    private HttpNumberProvider provider;

    private long quota = 0;

    static {
        HttpNumberFetcher.getNumbers = new HttpGet();
        HttpNumberFetcher.getNumbers.setHeader("User-Agent", String.format("KPI TUKE SK/0.1 Beta/%s", EMAIL));
        HttpNumberFetcher.getQuota = new HttpGet("http://www.random.org/quota/?format=plain");
        HttpNumberFetcher.getQuota.setHeader("User-Agent", String.format("KPI TUKE SK/0.1 Beta/%s", EMAIL));
        HttpNumberFetcher.client = new DefaultHttpClient(new ThreadSafeClientConnManager());
        HttpNumberFetcher.client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {

            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 5) {
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {
                    return false;
                }
                return false;
            }
        });
    }

    public HttpNumberFetcher(HttpNumberProvider provider) {
        this.provider = provider;
    }

    private long updateQuota() {
        if (quota < NUMBER_FETCH_COUNT * 64) {
            try {
                HttpResponse response = client.execute(getQuota);
                InputStream stream = response.getEntity().getContent();
                BufferedReader r = new BufferedReader(new InputStreamReader(stream));
                try {
                    quota = Integer.valueOf(r.readLine());
                } finally {
                    r.close();
                }
            } catch (IOException iOException) {
                Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.WARNING, null, iOException);
                return quota = -1;
            } catch (IllegalStateException illegalStateException) {
                Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.WARNING, null, illegalStateException);
                return quota = -1;
            } catch (NumberFormatException numberFormatException) {
                Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.SEVERE, null, numberFormatException);
                return quota = -1;
            }
        }
        return quota;
    }

    private static final Lock l = new ReentrantLock(true);

    public void run() {
        l.lock();
        try {
            updateQuota();
            if (this.quota < 0) {
                provider.setQuota(quota);
                return;
            }
            getNumbers.setURI(new URI(String.format(GET_NUMBERS_FORMAT_STRING, NUMBER_FETCH_COUNT * 2, provider.getSeed() < 0 ? "new" : provider.getSeed())));
            HttpResponse response = client.execute(getNumbers);
            InputStream stream = response.getEntity().getContent();
            LinkedList<Integer> list = new LinkedList<Integer>();
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            try {
                while (r.ready()) {
                    list.add((int) Long.parseLong(r.readLine().replaceAll("\t", ""), 16));
                }
            } finally {
                r.close();
            }
            provider.addNumbers(list);
            updateQuota();
            this.quota -= NUMBER_FETCH_COUNT * 32;
            System.out.println("quota: " + quota);
        } catch (URISyntaxException ex) {
            System.out.println(ex.getMessage() + " - " + ex.getReason());
            Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Logger.getLogger(HttpNumberFetcher.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            l.unlock();
        }
    }
}
