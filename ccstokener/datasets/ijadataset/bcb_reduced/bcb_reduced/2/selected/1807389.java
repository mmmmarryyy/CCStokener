package panama.tests;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.TestCase;

/**
 * Test for occasional HTTP-500 error
 * @author ridcully
 *
 */
public class StressTest extends TestCase {

    private static final String TEST_URL = "http://localhost:8080/panama-examples/guestbook/";

    private static final int NUM_THREADS = 50;

    private static final int NUM_RUNS = 5;

    private static final int MIN_RANDOM_SLEEP = 100;

    private static final int MAX_RANDOM_SLEEP = 500;

    private static final String OK_STRING = "HTTP/1.1 200 OK";

    public void testSingleAccess() {
        try {
            URL url = new URL(TEST_URL);
            URLConnection conn = url.openConnection();
            System.out.println(conn.getHeaderField(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testParallelAccess() {
        TestRunner threads[] = new TestRunner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            TestRunner t = new TestRunner("Thread-" + (i + 1));
            t.start();
            threads[i] = t;
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < threads.length; i++) {
            System.out.println(threads[i].name + ": " + threads[i].errorCount);
        }
    }

    class TestRunner extends Thread {

        public int errorCount = 0;

        public String name = "";

        public TestRunner(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(TEST_URL);
                for (int r = 0; r < NUM_RUNS; r++) {
                    URLConnection conn = url.openConnection();
                    String status = conn.getHeaderField(null);
                    System.out.println(name + ":" + status);
                    if (!OK_STRING.equals(status)) {
                        errorCount++;
                    }
                    long sleepTime = (long) (Math.random() * (MAX_RANDOM_SLEEP - MIN_RANDOM_SLEEP));
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
