package test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class GPlacesDeleter implements Callable<Long> {

    private final int num;

    private static final String URL_REST = "http://places-test-datastore.appspot.com/places";

    private final int numIterate;

    public GPlacesDeleter(int num, int numIterate) {
        this.num = num;
        this.numIterate = numIterate;
    }

    @Override
    public Long call() throws Exception {
        System.out.println(">>> alexey: GPlacesDeleter.call = entered = num = " + num);
        long sumTime = 0;
        for (int i = 0; i < numIterate; i++) {
            System.out.println(">>> alexey: GPlacesDeleter.call num = " + num + " i = " + i);
            long spentTime = deletePLace(i);
            sumTime += spentTime;
        }
        long avgTime = sumTime / numIterate;
        System.out.println(">>> alexey: GPlacesDeleter.call avgTime num = " + num + " = " + avgTime);
        return avgTime;
    }

    private long deletePLace(int i) throws IOException {
        long startTime = System.currentTimeMillis();
        URL url = new URL(URL_REST);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(false);
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.connect();
        conn.getInputStream();
        long time = System.currentTimeMillis() - startTime;
        return time;
    }
}
