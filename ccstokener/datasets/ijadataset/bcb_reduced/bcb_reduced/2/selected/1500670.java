package prefuse.demos;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bchisham
 */
public class GraphUrlLoader {

    public static void main(String[] args) {
        try {
            String default_uri = "http://www.cs.nmsu.edu/~bchisham/cgi-bin/phylows/tree/Tree3099?format=graphml";
            URL gurl = new URL(default_uri);
            InputStream is = gurl.openStream();
            Scanner iscan = new Scanner(is);
            while (iscan.hasNext()) {
                System.out.println(iscan.next());
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(GraphUrlLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
        }
    }
}
