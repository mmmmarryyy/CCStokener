package vydavky.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pomocna trieda pre stahovanie aktualnych kurzov CNB.
 */
public final class CNBHelper {

    /** URL, na ktorom sa nachadza aktualny kurzovy listok CNB. */
    private static final String CNB_KURZY = "http://www.cnb.cz/cs/financni_trhy/devizovy_trh/kurzy_devizoveho_trhu/denni_kurz.txt";

    private static final Logger logger = ClientUtils.getLogger(CNBHelper.class);

    /**
   * Privatny bezparametricky konstruktor, braniaci instanciaci triedy.
   */
    private CNBHelper() {
    }

    /**
   * K mene zadanej jej 3-pismenkovou ISO skratkou nacita jej sucasny kurz podla
   * Ceskej Narodnej Banky.
   * @param mena 3-pismenkova skratka meny podla ISO 4217.
   * @return Aktualny kurz zadanej meny, pripadne null, ak sa nacitavanie
   *         z akehokolvek dovodu nepodarilo.
   */
    public static Float getKurzMeny(final String mena) {
        return getKurzMeny(mena, null);
    }

    /**
   * K mene zadanej jej 3-pismenkovou ISO skratkou a kurzoveho listku CNB vrati
   * jej aktualny kurz podla tohto listku.
   * @param mena 3-pismenkova skratka meny podla ISO 4217.
   * @param kurzy Kurzovy listok CNB zadany ako zoznam riadkov.
   * @return Aktualny kurz zadanej meny, pripadne null, ak sa nacitavanie
   *         z akehokolvek dovodu nepodarilo.
   */
    public static Float getKurzMeny(final String mena, final List<String> kurzy) {
        final List<String> pouziteKurzy;
        if (kurzy == null) {
            pouziteKurzy = nacitajURL(CNB_KURZY);
        } else {
            pouziteKurzy = kurzy;
        }
        if (pouziteKurzy == null) {
            logger.log(Level.WARNING, "Zlyhalo nacitanie URL");
            return null;
        }
        for (String str : pouziteKurzy) {
            final StringTokenizer st = new StringTokenizer(str, "|");
            if (st.countTokens() < 5) {
                continue;
            }
            st.nextToken();
            st.nextToken();
            final String pocetJednotiekStr = st.nextToken();
            if (!mena.equals(st.nextToken())) {
                continue;
            }
            try {
                final NumberFormat format = NumberFormat.getInstance(new Locale("cs"));
                final int pocetJednotiek = format.parse(pocetJednotiekStr).intValue();
                return Float.valueOf(format.parse(st.nextToken()).floatValue() / (float) pocetJednotiek);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Zlyhalo parsovanie kurzu alebo poctu jednotiek:\n" + ClientUtils.getStackTrace(e));
                return null;
            }
        }
        return null;
    }

    /**
   * Nacita aktualny kurzovy listok CNB.
   * @return Aktualny kurzovy listok CNB ako zoznam riadkov.
   */
    public static List<String> nacitajKurzovuTabulku() {
        return nacitajURL(CNB_KURZY);
    }

    /**
   * Nacita textovy subor v zadanom URL ako zoznam riadkov v kodovani UTF-8.
   * @param url Zdrojove URL.
   * @return Zoznam stringov, kazdy reprezentuje jeden riadok suboru.
   */
    private static List<String> nacitajURL(final String url) {
        InputStream is = null;
        final List<String> ret = new LinkedList<String>();
        try {
            is = (new URL(url)).openStream();
            final BufferedReader dis = new BufferedReader(new InputStreamReader(is, "utf-8"));
            String s = dis.readLine();
            while (s != null) {
                ret.add(s);
                s = dis.readLine();
            }
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Chybne URL:\n" + ClientUtils.getStackTrace(e));
            return null;
        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O chyba:\n" + ClientUtils.getStackTrace(e));
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "I/O chyba:\n" + ClientUtils.getStackTrace(e));
            }
        }
        return ret;
    }
}
