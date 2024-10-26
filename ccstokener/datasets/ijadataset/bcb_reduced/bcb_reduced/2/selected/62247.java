package carassius.BLL;

import carassius.DAL.AnimalRow;
import carassius.DAL.PictureRow;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author siebz0r
 */
public final class AquavisieImporter {

    private static final String AQUAVISIE = "http://aquavisie.retry.org/";

    private static final String DATABASE;

    private static final String FISH_NL;

    private static final String FISH_EN;

    private static final String SCIENTIFIC_NL;

    private static final String NL;

    private static final String SCIENTIFIC_EN;

    private static final String EN;

    static {
        DATABASE = new StringBuilder().append(AQUAVISIE).append("Database/").toString();
        FISH_NL = new StringBuilder().append(DATABASE).append("Vissen/").toString();
        SCIENTIFIC_NL = new StringBuilder().append(FISH_NL).append("Vissen.html").toString();
        NL = new StringBuilder().append(FISH_NL).append("Vissen1.html").toString();
        FISH_EN = new StringBuilder().append(DATABASE).append("Aquariumfish/").toString();
        SCIENTIFIC_EN = new StringBuilder().append(FISH_EN).append("Aquariumfish.html").toString();
        EN = new StringBuilder().append(FISH_EN).append("Aquariumfish1.html").toString();
    }

    private static ArrayList<FishPage> getDutchScrientificNames() throws IOException {
        try {
            return getFishPagesFromPage(new URL(SCIENTIFIC_NL));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public static ArrayList<FishPage> getDutchNames() throws IOException {
        try {
            return getFishPagesFromPage(new URL(NL));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    private static ArrayList<FishPage> getEnglishScrientificNames() throws IOException {
        try {
            return getFishPagesFromPage(new URL(SCIENTIFIC_EN));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public static ArrayList<FishPage> getScientificNames() throws IOException {
        String locale = System.getProperty("user.locale");
        if ("nl".equals(locale)) {
            return getDutchScrientificNames();
        } else {
            return getEnglishScrientificNames();
        }
    }

    public static ArrayList<FishPage> getEnglishNames() throws IOException {
        try {
            return getFishPagesFromPage(new URL(EN));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
	 * Fetches a web page and returns the page in a BufferedReader
	 * @param url The URL of the web page
	 * @return Page as a BufferedReader
	 * @throws IOException If page cannot be fetched
	 */
    private static BufferedReader getPage(URL url) throws IOException {
        return new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
    }

    /**
	 * Gets the page and removes all the newline characters (and trims each line)
	 * @param page The url of the web page
	 * @return Page as a line
	 */
    private static String convertPageToLine(URL page) throws IOException {
        BufferedReader bufferedPage = getPage(page);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = bufferedPage.readLine()) != null) {
            sb.append(line.replaceAll("[\\p{javaWhitespace}]{2,}", " "));
        }
        String parsedPage = StringEscapeUtils.unescapeHtml(sb.toString());
        return parsedPage;
    }

    /**
	 * Searches for {@code <a href="link to fish.html">name of fish</a>} and converts
	 * this to an ArrayList of FishPages.
	 * @param sourceURL The URL of the web page with the fish names on it.
	 * @return an ArrayList of FishPage
	 */
    private static ArrayList<FishPage> getFishPagesFromPage(URL sourceURL) throws IOException {
        final String page = convertPageToLine(sourceURL);
        ArrayList<FishPage> fishPage = new ArrayList<FishPage>();
        final String baseURL = getBaseUrl(sourceURL.toString());
        Pattern p = Pattern.compile("<a" + "[\\p{javaWhitespace}]+" + "href=\"" + "([^\\.#]+\\.html)" + "\">" + "[\\p{javaWhitespace}]*" + "(?:<font[^>]+>[\\p{javaWhitespace}]*)?" + "([^<]+)");
        Matcher m = p.matcher(page);
        StringBuilder link;
        while (m.find()) {
            if (!"Vissen.html".equals(m.group(1)) && !"Vissen1.html".equals(m.group(1)) && !"Aquariumfish.html".equals(m.group(1)) && !"Aquariumfish1.html".equals(m.group(1))) {
                link = new StringBuilder(baseURL);
                link.append(m.group(1));
                try {
                    fishPage.add(new FishPage(m.group(2), new URL(link.toString())));
                } catch (MalformedURLException ex) {
                    StringBuilder sb = new StringBuilder("Couldn't create fish page!\nLink: ");
                    sb.append(link);
                    sb.append("\nName: ");
                    sb.append(m.group(2));
                    System.out.println();
                }
            }
        }
        return (fishPage.isEmpty()) ? null : fishPage;
    }

    private static String getBaseUrl(String url) {
        Pattern p = Pattern.compile("(([^/]*/)+)[^/]*");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static AnimalRow getFish(URL page) throws IOException {
        String pageAsLine = convertPageToLine(page);
        if (pageAsLine != null) {
            String scientificName = getScientificNameFromPage(pageAsLine);
            String alternativeName = null;
            if (page.toString().contains(FISH_NL)) {
                alternativeName = getDutchNameFromPage(pageAsLine);
            } else if (page.toString().contains(FISH_EN)) {
                alternativeName = getEnglishNameFromPage(pageAsLine);
            }
            String description = null;
            String feed = null;
            String breed = null;
            String origin = getOriginFromPage(pageAsLine);
            double length = getLengthFromPage(pageAsLine);
            int minLengthAquarium = getMinLengthAquariumFromPage(pageAsLine);
            int[] temperatures = getTemperaturesFromPage(pageAsLine);
            int minTemperature = 0, maxTemperature = 0;
            if (temperatures[0] > 0) {
                if (temperatures[1] > 0) {
                    minTemperature = Math.min(temperatures[0], temperatures[1]);
                    if (Settings.getSettingAsBoolean("debug")) {
                        System.out.print("Fetched min.temp: ");
                        System.out.println(minTemperature);
                    }
                    maxTemperature = Math.max(temperatures[0], temperatures[1]);
                    if (Settings.getSettingAsBoolean("debug")) {
                        System.out.print("Fetched max.temp: ");
                        System.out.println(maxTemperature);
                    }
                } else {
                    minTemperature = temperatures[0];
                    maxTemperature = temperatures[0];
                    if (Settings.getSettingAsBoolean("debug")) {
                        System.out.print("Fetched temps: ");
                        System.out.println(temperatures[0]);
                    }
                }
            }
            File picture = null;
            PictureRow pictureRow = null;
            return new AnimalRow(-1, scientificName, alternativeName, description, minTemperature, maxTemperature, length, 0, pictureRow, feed, minLengthAquarium, 0, 0, breed, origin, false);
        }
        return null;
    }

    private static String getScientificNameFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("(?:(?:<center>[\\p{javaSpaceChar}]*(?:<p>)?)||(?:<p[\\p{javaSpaceChar}]+align=\"center\">[\\p{javaSpaceChar}]*))" + "<font[\\p{javaSpaceChar}]+size=\"[\\d]+\">" + "[\\p{javaSpaceChar}]*" + "<b>" + "[\\p{javaSpaceChar}]*" + "((?:" + "(?:<a[^>]*>[\\p{javaSpaceChar}]*)?" + "[^<]+" + "(?:</a>)?" + "[\\p{javaSpaceChar}]*" + ")+)" + "(?:<br>[\\p{javaSpaceChar}]+)?" + "</b>" + "[\\p{javaSpaceChar}]*" + "</font>");
        Matcher m = p.matcher(pageAsLine);
        StringBuilder sb = null;
        if (m.find()) {
            p = Pattern.compile("(?:<a[^>]*>)?" + "[\\p{javaSpaceChar}]*" + "([^<]+)" + "(?:</a>)?" + "[\\p{javaSpaceChar}]*");
            m = p.matcher(m.group(1));
            if (m.find()) {
                sb = new StringBuilder(m.group(1));
                while (m.find()) {
                    sb.append(' ');
                    sb.append(m.group(1));
                }
                if (Settings.getSettingAsBoolean("debug")) {
                    System.out.print("Fetched scientific name: ");
                    System.out.println(sb.toString());
                }
            }
        }
        return (sb != null) ? sb.toString() : null;
    }

    private static String getOriginFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<(td)[^>]*>" + "(Herkomst|Origin)" + "</\\1>" + "[\\p{javaWhitespace}]*" + "<\\1[^>]*>" + "[:\\p{javaWhitespace}]*" + "(?:<[^>]+>)?" + "([^<]+)" + "</" + "(?:\\1>|[^>]*></\\1>)");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (Settings.getSettingAsBoolean("debug")) {
                System.out.print("Fetched origin: ");
                System.out.println(m.group(3));
            }
            return m.group(3);
        }
        return null;
    }

    private static double getLengthFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<(td)[^>]*>" + "(?:Lengte vd vis[\\p{javaSpaceChar}]*|Length[\\w\\p{javaWhitespace}]*)" + "</\\1>" + "[\\p{javaSpaceChar}]*" + "<\\1[^>]*>" + "[\\D]*" + "([\\d]+(?:[,\\.][\\d]+)?)" + "[\\p{javaSpaceChar}]*" + "(?:-[\\p{javaSpaceChar}]*([\\d]+(?:[,\\.][\\d]+)?)[\\p{javaSpaceChar}]*)?" + "cm" + "[\\p{javaSpaceChar}]*" + "(?:-[\\p{javaSpaceChar}]*([\\d]+(?:[,\\.][\\d]+)?)[\\p{javaSpaceChar}\"]*)?" + "(?:-[\\p{javaSpaceChar}]*([\\d]+(?:[,\\.][\\d]+)?)[\\p{javaSpaceChar}\"]*)?" + "", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (m.group(3) != null && !m.group(3).isEmpty()) {
                if (Settings.getSettingAsBoolean("debug")) {
                    System.out.print("Fetched length: ");
                    System.out.println(m.group(3));
                }
                return parseDoubleFromString(m.group(3));
            } else if (m.group(2) != null && !m.group(2).isEmpty()) {
                if (Settings.getSettingAsBoolean("debug")) {
                    System.out.print("Fetched length: ");
                    System.out.println(m.group(2));
                }
                return parseDoubleFromString(m.group(2));
            }
        }
        return 0;
    }

    private static int getMinLengthAquariumFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<td[^>]*>" + "Min\\.(?:lengte|length) aquarium" + "</td>" + "[\\p{javaWhitespace}]*" + "<td[^>]*>" + "[:\\p{javaWhitespace}]*" + "([\\d]+)" + "[\\p{javaWhitespace}]*" + "cm" + "[\\p{javaWhitespace}]*" + "(?:-[\\p{javaWhitespace}]*[\\d]+\"[\\p{javaWhitespace}]*)?" + "</td>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            int length = parseIntegerFromString(m.group(1));
            if (Settings.getSettingAsBoolean("debug")) {
                System.out.print("Fetched min.length aquarium: ");
                System.out.println(length);
            }
            return length;
        }
        return 0;
    }

    /**
	 * Parses the first double found in a string
	 * @param s the string to parse
	 * @return double found in string
	 * @throws NumberFormatException if there was no double found or if
	 * something went wrong
	 */
    private static double parseDoubleFromString(String s) throws NumberFormatException {
        Pattern p = Pattern.compile("([\\d]+)(?:[,\\.]([\\d]+))?");
        Matcher m = p.matcher(s);
        if (m.find()) {
            String a = m.group(1);
            if (m.group(2) != null) {
                String b = m.group(2);
                double divider = Math.pow(10, b.length());
                return Double.parseDouble(a) + (Double.parseDouble(b) / divider);
            }
            return Double.parseDouble(a);
        }
        throw new NumberFormatException();
    }

    /**
	 * Parses the first integer found in a string
	 * @param s the string to parse
	 * @return integer found in string
	 * @throws NumberFormatException if there was no integer found or if
	 * something went wrong
	 */
    private static int parseIntegerFromString(String s) throws NumberFormatException {
        Pattern p = Pattern.compile("[\\d]+");
        Matcher m = p.matcher(s);
        if (m.find()) {
            return Integer.parseInt(m.group(0));
        }
        throw new NumberFormatException();
    }

    private static String getDutchNameFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<td[^>]*>Nederlandse naam</td>[^<]*<td[^>]*>([: ]*)([^<]+)</td>");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (Settings.getSettingAsBoolean("debug")) {
                System.out.print("Fetched dutch name: ");
                System.out.println(m.group(2));
            }
            return m.group(2);
        }
        return null;
    }

    private static String getEnglishNameFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<(td)[^>]*>" + "English name[s]?" + "[\\p{javaWhitespace}]*" + "</\\1>" + "[\\p{javaWhitespace}]*" + "[^<]*" + "<\\1[^>]*>" + "[:\\p{javaWhitespace}]*" + "([^<]+)" + "</\\1>");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (Settings.getSettingAsBoolean("debug")) {
                System.out.print("Fetched english name: ");
                System.out.println(m.group(2));
            }
            return m.group(2);
        }
        return null;
    }

    private static int[] getTemperaturesFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("Temp(\\d{2})C\\.gif");
        Matcher m = p.matcher(pageAsLine);
        int[] temperatures = new int[2];
        int i = 0;
        while (m.find() && i < 2) {
            temperatures[i] = Integer.parseInt(m.group(1));
        }
        return temperatures;
    }

    private static File getPictureFromPage(String pageAsLine) throws IOException {
        Pattern p = Pattern.compile("href=\"(\\.){2}/((Grotere(\\s|%20)foto/)(([^\\.]+)(\\.html)))\">");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            try {
                String picPageUrl = new StringBuilder().append(DATABASE).append(m.group(2)).toString();
                pageAsLine = convertPageToLine(new URL(picPageUrl));
                if (pageAsLine != null) {
                    StringBuilder pattern = new StringBuilder();
                    pattern.append("src=\"(");
                    pattern.append(m.group(6));
                    pattern.append("\\.(jpg|jpeg|gif|png))\"");
                    p = Pattern.compile(pattern.toString());
                    m = p.matcher(pageAsLine);
                    if (m.find()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(DATABASE);
                        sb.append("Grotere foto/");
                        sb.append(m.group(1));
                        return new File(sb.toString());
                    }
                }
            } catch (MalformedURLException ex) {
            }
        }
        return null;
    }

    private static String getDescriptionFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<td[^>]*>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "<p>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "</p>[\\p{javaWhitespace}]*<p>[\\p{javaWhitespace}]*" + "([^<]+)" + "<(?:(?:/p>[\\p{javaWhitespace}]*(?:([^<]+)||<p>)</)||/td>||br>)");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            for (int i = 0; i <= m.groupCount(); i++) {
                String group = m.group(i);
                group.toString();
            }
            if (m.group(4) != null && !m.group(4).contains("<")) {
                StringBuilder sb = new StringBuilder();
                sb.append(m.group(1));
                sb.append(m.group(2));
                return sb.toString();
            } else {
                return m.group(1);
            }
        }
        return null;
    }

    private static String getFeedFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<td[^>]*>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "<p>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "</p>" + "[\\p{javaWhitespace}]*" + "<p>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "</(?:p>[\\p{javaWhitespace}]*(?:([^<]+)||<p>)</)?td>");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (m.group(4) != null && !m.group(4).contains("<")) {
                return m.group(3);
            } else {
                return m.group(2);
            }
        }
        return null;
    }

    private static String getBreedFromPage(String pageAsLine) {
        Pattern p = Pattern.compile("<td[^>]*>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "<p>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "</p>" + "[\\p{javaWhitespace}]*" + "<p>" + "[\\p{javaWhitespace}]*" + "([^<]+)" + "</(?:p>" + "[\\p{javaWhitespace}]*" + "(?:([^<]+)||<p>)" + "</)?td>");
        Matcher m = p.matcher(pageAsLine);
        if (m.find()) {
            if (m.group(4) != null && !m.group(4).contains("<")) {
                return m.group(3);
            } else {
                return m.group(4);
            }
        }
        return null;
    }
}
