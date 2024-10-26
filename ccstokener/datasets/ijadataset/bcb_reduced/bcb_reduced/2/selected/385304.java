package glowaxes.glyphs.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * Object that starts threads to load a test case, used for performance logging.
 * 
 * @author <a href="mailto:eddie@tinyelements.com">Eddie Moojen</a>
 * @version $Id: BasicThread.java 184 2009-07-16 11:28:22Z nejoom $
 */
public class BasicThread implements Runnable {

    /** The Constant logger. */
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BasicThread.class.getName());

    @SuppressWarnings("unused")
    private String str;

    @SuppressWarnings("unused")
    private int counter = 0;

    public void run() {
        URL url;
        try {
            url = new URL("http://localhost:8080/glowaxes/dailytrend.jsp");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((str = in.readLine()) != null) {
            }
            in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

    BasicThread(int i) {
        counter = i;
    }

    public static void main(String args[]) {
        for (int i = 0; i < 10; i++) {
            Runnable runnable = new BasicThread(i);
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }
}
