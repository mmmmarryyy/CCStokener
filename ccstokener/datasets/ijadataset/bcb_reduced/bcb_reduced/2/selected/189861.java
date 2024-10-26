package freedbimporter.util;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.util.StringTokenizer;

/**
 * Determines an ip-address.
 * <p>
 *
 * @version      1.0 by 17.5.2009
 * @author       Copyright 2004 <a href="MAILTO:freedb2mysql@freedb2mysql.de">Christian Kruggel</a> - freedbimporter and all it&acute;s parts are free software and destributed under <a href="http://www.gnu.org/licenses/gpl-2.0.txt" target="_blank">GNU General Public License</a>
 */
public class IpAddressenDeterminator {

    public static String DEFAULT_URL = "http://www.whatismyip.com/automation/n09230945.asp";

    private static String HTTP_PREFIX = "http://";

    InetAddress adresse = null;

    public IpAddressenDeterminator(String site) throws MalformedURLException, UnknownHostException {
        String mS = null;
        try {
            URL url = new URL(site.trim());
            if (url.getProtocol().equalsIgnoreCase(HTTP_PREFIX)) try {
                parseIPFromHTMLPage(url);
            } catch (IOException ie) {
            }
        } catch (MalformedURLException m) {
            mS = m.getMessage();
        }
        String uS = null;
        if (adresse == null) {
            String adr = site.trim().toLowerCase();
            if (adr.startsWith(HTTP_PREFIX)) {
                adr = adr.substring(HTTP_PREFIX.length());
                int endIndex = adr.indexOf('/');
                if (endIndex > 0) adr = adr.substring(0, endIndex);
            }
            try {
                adresse = InetAddress.getByName(adr);
            } catch (UnknownHostException ue) {
                uS = ue.getMessage();
            }
        }
        if (adresse == null) {
            if (uS != null) throw new UnknownHostException(uS);
            if (mS != null) throw new MalformedURLException(mS);
        }
    }

    private void parseIPFromHTMLPage(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.addRequestProperty("User-Agent", "Mozilla/4.76");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        int positionPunktEins, positionPunktZwei, positionPunktDrei;
        while ((inputLine = in.readLine()) != null) {
            positionPunktDrei = 0;
            positionPunktZwei = 0;
            positionPunktEins = 0;
            do {
                positionPunktEins = inputLine.indexOf('.', positionPunktEins);
                if (positionPunktEins > 0) positionPunktZwei = inputLine.indexOf('.', positionPunktEins + 1);
                if (positionPunktZwei > 0) positionPunktDrei = inputLine.indexOf('.', positionPunktZwei + 1);
                if ((positionPunktDrei > 0) && ((positionPunktDrei - positionPunktEins) <= 7)) {
                    int left = positionPunktEins - 1;
                    while ((left >= 0) && ('0' <= inputLine.charAt(left)) && (inputLine.charAt(left) <= '9')) left--;
                    left++;
                    int right = positionPunktDrei + 1;
                    while ((right < inputLine.length()) && ('0' <= inputLine.charAt(right)) && (inputLine.charAt(right) <= '9')) right++;
                    String adressString = inputLine.substring(left, right).trim();
                    int oktettZaehler = 0;
                    StringTokenizer oT = new StringTokenizer(adressString, ".");
                    byte[] oktette = new byte[4];
                    try {
                        while (oT.hasMoreTokens() && (oktettZaehler < 4)) {
                            String okt = oT.nextToken().trim();
                            Integer i = new Integer(okt);
                            oktette[oktettZaehler] = (byte) i.intValue();
                            oktettZaehler++;
                        }
                    } catch (NumberFormatException e) {
                    }
                    if (oktettZaehler == 4) try {
                        adresse = InetAddress.getByName(adressString);
                    } catch (UnknownHostException e) {
                    }
                }
                positionPunktEins = positionPunktEins + 1;
            } while (positionPunktEins != 0);
        }
        in.close();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!o.getClass().equals(getClass())) return false;
        return ((IpAddressenDeterminator) o).adresse.equals(this.adresse);
    }

    public int hashCode() {
        return adresse.hashCode();
    }

    public IpAddressenDeterminator() throws MalformedURLException, UnknownHostException {
        this(DEFAULT_URL);
    }

    public InetAddress getAddress() {
        return adresse;
    }
}
