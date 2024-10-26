package org.openamf.examples;

import java.net.URL;
import java.net.URLConnection;

/**
 * @author Jason Calabrese <jasonc@missionvi.com>
 * @version $Revision: 1.1 $, $Date: 2003/08/20 19:32:21 $
 */
public class SubmitToURL {

    public String submit(String urlString) {
        String result = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            result = conn.getContent().toString();
        } catch (Exception e) {
            result = e.toString();
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(new SubmitToURL().submit("http://lists.sourceforge.net/lists/subscribe/openamf-user?email=jasonc@missionvi.com&pw=test&pw-conf=test"));
    }
}
