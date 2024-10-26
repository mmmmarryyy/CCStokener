package gnu.gettext;

import java.io.*;
import java.net.*;

/**
 * Fetch an URL's contents and emit it to standard output.
 * Exit code: 0 = success
 *            1 = failure
 *            2 = timeout
 * @author Bruno Haible
 */
public class GetURL {

    private static long timeout = 30 * 1000;

    private boolean done;

    private Thread timeoutThread;

    public void fetch(String s) {
        URL url;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            System.exit(1);
            return;
        }
        done = false;
        timeoutThread = new Thread() {

            public void run() {
                try {
                    sleep(timeout);
                    if (!done) {
                        System.exit(2);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        timeoutThread.start();
        try {
            InputStream istream = new BufferedInputStream(url.openStream());
            OutputStream ostream = new BufferedOutputStream(System.out);
            for (; ; ) {
                int b = istream.read();
                if (b < 0) break;
                ostream.write(b);
            }
            ostream.close();
            System.out.flush();
            istream.close();
        } catch (IOException e) {
            System.exit(1);
        }
        done = true;
    }

    public static void main(String[] args) {
        if (args.length != 1) System.exit(1);
        (new GetURL()).fetch(args[0]);
        System.exit(0);
    }
}
