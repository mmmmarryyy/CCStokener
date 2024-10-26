package connectivity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Class for use with /from and /fromall to convert IP's to Countries via the use
 * of an ip2c conversion database at wc3banlist.de, the format for the requests is
 * "http://wc3banlist.de/iptc.php?addr=ip1;ip2;ip3..." And the return from the website
 * will be 
 * Nothing : Couldn't get Country
 * Country Name (Code) : got country
 * so the request "http://wc3banlist.de/iptc.php?addr="23.123.232.232;342.232.222.21;192.168.2.1"
 * might return "the United States (US);;Germany (DE) if the second ip was invalid...
 * @author Ilaggoodly
 *
 */
public class LocationCheck {

    public static final String dbAddr = "http://wc3banlist.de/iptc.php?addr=";

    /**
	 * Checks to see where a single IP is from using the online ip2c conversion
	 * @param IPString --a String representation of their IP
	 * @return -- a country name and code, or "Error (NONE)" if we couldn't get their country
	 */
    public static String fromSingle(String IPString) {
        URL url;
        try {
            url = new URL(dbAddr + IPString);
            URLConnection urlConn = url.openConnection();
            urlConn.connect();
            Scanner inc = new Scanner(urlConn.getInputStream());
            try {
                return inc.nextLine();
            } catch (NoSuchElementException e) {
            }
        } catch (MalformedURLException e) {
            System.err.println("Error getting Location");
        } catch (IOException e) {
            System.err.println("Error getting Location");
        }
        return "Error (NONE)";
    }

    /**
	 * Queries the wc3banlist database for a bunch of ips
	 * @param ips --an array of ips as strings
	 * @return --an array of country codes with paranthesis
	 */
    public static String[] fromMany(ArrayList<String> ips) {
        URL url;
        String[] out = new String[ips.size()];
        Arrays.fill(out, "(NONE)");
        String IPString = "";
        for (String x : ips) IPString += x + ";";
        try {
            url = new URL(dbAddr + IPString);
            URLConnection urlConn = url.openConnection();
            urlConn.connect();
            Scanner inc = new Scanner(urlConn.getInputStream());
            String tmpString = inc.nextLine();
            String raw[] = (tmpString.replace(";;", "; ;")).split(";");
            for (int i = 0; i < raw.length; i++) {
                if (!raw[i].trim().equalsIgnoreCase("")) out[i] = raw[i];
            }
            return out;
        } catch (MalformedURLException e) {
            System.err.println("Error getting Location");
        } catch (IOException e) {
            System.err.println("Error getting Location");
        }
        return null;
    }
}
