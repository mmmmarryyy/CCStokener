package rbsla.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

public class WebsiteAvailable {

    public static Boolean testWebserver(String u) {
        try {
            u = u.replaceAll("\"", "");
            URL url = new URL(u);
            URLConnection connect = url.openConnection();
            connect.connect();
            return Boolean.TRUE;
        } catch (IOException e) {
            return Boolean.FALSE;
        }
    }

    public static Double getWebserverResponseTime(String u) {
        Double t;
        long time = System.currentTimeMillis();
        try {
            u = u.replaceAll("\"", "");
            URL url = new URL(u);
            URLConnection connect = url.openConnection();
            connect.connect();
            t = new Double((double) (System.currentTimeMillis() - time));
            System.out.println(new Long(System.currentTimeMillis() - time));
            return t;
        } catch (IOException e) {
            t = Double.POSITIVE_INFINITY;
            return t;
        }
    }

    public static Calendar getDate() {
        return Calendar.getInstance();
    }

    public static Boolean waiter(Integer value) {
        try {
            Calendar currentTime = Calendar.getInstance();
            currentTime.add(Calendar.SECOND, value.intValue());
            while (currentTime.after(Calendar.getInstance())) {
            }
            return Boolean.TRUE;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    public static Boolean test() {
        return Boolean.TRUE;
    }
}
