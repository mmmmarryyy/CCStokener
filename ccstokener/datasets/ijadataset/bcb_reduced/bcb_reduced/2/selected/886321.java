package uk.icat3.download.test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author gjd37
 */
public class DownloadTest {

    /** Creates a new instance of DownloadTest */
    public DownloadTest() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws MalformedURLException {
        URL url = new URL("http://localhost:8080/icat-clf-download/DownloadServlet?file=1234/images.jpeg&sid=sdf&name=test.jpeg");
        InputStream is = null;
        DataInputStream dis;
        String s;
        try {
            is = url.openStream();
            dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                System.out.println(s);
            }
        } catch (MalformedURLException mue) {
            System.out.println("Ouch - a MalformedURLException happened.");
            mue.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Oops- an IOException happened.");
            ioe.printStackTrace();
            System.exit(1);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }
}
