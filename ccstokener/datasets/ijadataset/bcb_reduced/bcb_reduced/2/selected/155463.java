package wtanaka.praya.yahoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import wtanaka.debug.Debug;

/**
 * Utility methods for interacting with Yahoo!
 *
 * <p>
 * Return to <A href="http://sourceforge.net/projects/praya/">
 * <IMG src="http://sourceforge.net/sflogo.php?group_id=2302&type=1"
 *   alt="Sourceforge" width="88" height="31" border="0"></A>
 * or the <a href="http://praya.sourceforge.net/">Praya Homepage</a>
 *
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2003/12/17 01:30:15 $
 **/
public class YahooUtils {

    /**
    * Attempts to get a photo for the given yahoo username.  Does this
    * by fetching profiles.yahoo.com/id and searching for the image in
    * the returned page.  If the parse fails, returns
    * http://us.i1.yimg.com/us.yimg.com/i/ppl/no_photo.gif
    *
    * @param id the yahoo ID.
    * @return a url for an image.
    **/
    public static URL getIconURLForUser(String id) {
        try {
            URL url = new URL("http://profiles.yahoo.com/" + id);
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String input = null;
            while ((input = r.readLine()) != null) {
                if (input.indexOf("<a href=\"") < 0) continue;
                if (input.indexOf("<img src=\"") < 0) continue;
                if (input.indexOf("<a href=\"") > input.indexOf("<img src=\"")) continue;
                String href = input.substring(input.indexOf("<a href=\"") + 9);
                String src = input.substring(input.indexOf("<img src=\"") + 10);
                if (href.indexOf("\"") < 0) continue;
                if (src.indexOf("\"") < 0) continue;
                href = href.substring(0, href.indexOf("\""));
                src = src.substring(0, src.indexOf("\""));
                if (href.equals(src)) {
                    return new URL(href);
                }
            }
        } catch (IOException e) {
        }
        URL toReturn = null;
        try {
            toReturn = new URL("http://us.i1.yimg.com/us.yimg.com/i/ppl/no_photo.gif");
        } catch (MalformedURLException e) {
            Debug.assrt(false);
        }
        return toReturn;
    }
}
