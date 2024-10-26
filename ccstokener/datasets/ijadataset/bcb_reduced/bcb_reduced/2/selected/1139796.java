package org.lindenb.tool.oneshot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * NCBIMap
 * @author Pierre Lindenbaum plindenbaum@yahoo.fr
 *
 */
public class NCBIMap {

    private NCBIMap() {
    }

    private static final String Countries[][] = { { "ac", "Ascension Island" }, { "ad", "Andorra" }, { "ae", "United Arab Emirates" }, { "af", "Afghanistan" }, { "ag", "Antigua and Barbuda" }, { "ai", "Anguilla" }, { "al", "Albania" }, { "am", "Armenia" }, { "an", "Netherlands Antilles" }, { "ao", "Angola" }, { "aq", "Antarctica" }, { "ar", "Argentina" }, { "as", "American Samoa" }, { "at", "Austria" }, { "au", "Australia" }, { "aw", "Aruba" }, { "ax", "Aland Islands" }, { "az", "Azerbaijan" }, { "ba", "Bosnia and Herzegovina" }, { "bb", "Barbados" }, { "bd", "Bangladesh" }, { "be", "Belgium" }, { "bf", "Burkina Faso" }, { "bg", "Bulgaria" }, { "bh", "Bahrain" }, { "bi", "Burundi" }, { "bj", "Benin" }, { "bm", "Bermuda" }, { "bn", "Brunei Darussalam" }, { "bo", "Bolivia" }, { "br", "Brazil" }, { "bs", "Bahamas" }, { "bt", "Bhutan" }, { "bv", "Bouvet Island" }, { "bw", "Botswana" }, { "by", "Belarus" }, { "bz", "Belize" }, { "ca", "Canada" }, { "cc", "Cocos (Keeling) Islands" }, { "cd", "Congo, The Democratic Republic of the" }, { "cf", "Central African Republic" }, { "cg", "Congo, Republic of" }, { "ch", "Switzerland" }, { "ci", "Cote d'Ivoire" }, { "ck", "Cook Islands" }, { "cl", "Chile" }, { "cm", "Cameroon" }, { "cn", "China" }, { "co", "Colombia" }, { "cr", "Costa Rica" }, { "cu", "Cuba" }, { "cv", "Cape Verde" }, { "cx", "Christmas Island" }, { "cy", "Cyprus" }, { "cz", "Czech Republic" }, { "de", "Germany" }, { "dj", "Djibouti" }, { "dk", "Denmark" }, { "dm", "Dominica" }, { "do", "Dominican Republic" }, { "dz", "Algeria" }, { "ec", "Ecuador" }, { "ee", "Estonia" }, { "eg", "Egypt" }, { "eh", "Western Sahara" }, { "er", "Eritrea" }, { "es", "Spain" }, { "et", "Ethiopia" }, { "eu", "European Union" }, { "fi", "Finland" }, { "fj", "Fiji" }, { "fk", "Falkland Islands (Malvinas)" }, { "fm", "Micronesia, Federated States of" }, { "fo", "Faroe Islands" }, { "fr", "France" }, { "ga", "Gabon" }, { "gb", "United Kingdom" }, { "gd", "Grenada" }, { "gf", "French Guiana" }, { "gg", "Guernsey" }, { "gh", "Ghana" }, { "gi", "Gibraltar" }, { "gl", "Greenland" }, { "gm", "Gambia" }, { "gn", "Guinea" }, { "gp", "Guadeloupe" }, { "gq", "Equatorial Guinea" }, { "gr", "Greece" }, { "gs", "South Georgia and the South Sandwich Islands" }, { "gt", "Guatemala" }, { "gu", "Guam" }, { "gw", "Guinea-Bissau" }, { "gy", "Guyana" }, { "hk", "Hong Kong" }, { "hm", "Heard and McDonald Islands" }, { "hn", "Honduras" }, { "hr", "Croatia/Hrvatska" }, { "ht", "Haiti" }, { "hu", "Hungary" }, { "id", "Indonesia" }, { "ie", "Ireland" }, { "il", "Israel" }, { "im", "Isle of Man" }, { "in", "India" }, { "io", "British Indian Ocean Territory" }, { "iq", "Iraq" }, { "ir", "Iran, Islamic Republic of" }, { "ir", "Iran" }, { "is", "Iceland" }, { "it", "Italy" }, { "je", "Jersey" }, { "jm", "Jamaica" }, { "jo", "Jordan" }, { "jp", "Japan" }, { "ke", "Kenya" }, { "kg", "Kyrgyzstan" }, { "kh", "Cambodia" }, { "ki", "Kiribati" }, { "km", "Comoros" }, { "kn", "Saint Kitts and Nevis" }, { "kp", "Korea, Democratic People's Republic" }, { "kr", "Korea, Republic of" }, { "kr", "South Korea" }, { "kw", "Kuwait" }, { "ky", "Cayman Islands" }, { "kz", "Kazakhstan" }, { "la", "Lao People's Democratic Republic" }, { "lb", "Lebanon" }, { "lc", "Saint Lucia" }, { "li", "Liechtenstein" }, { "lk", "Sri Lanka" }, { "lr", "Liberia" }, { "ls", "Lesotho" }, { "lt", "Lithuania" }, { "lu", "Luxembourg" }, { "lv", "Latvia" }, { "ly", "Libyan Arab Jamahiriya" }, { "ly", "Libya" }, { "ma", "Morocco" }, { "mc", "Monaco" }, { "md", "Moldova, Republic of" }, { "me", "Montenegro" }, { "mg", "Madagascar" }, { "mh", "Marshall Islands" }, { "mk", "Macedonia, The Former Yugoslav Republic of" }, { "ml", "Mali" }, { "mm", "Myanmar" }, { "mn", "Mongolia" }, { "mo", "Macao" }, { "mp", "Northern Mariana Islands" }, { "mq", "Martinique" }, { "mr", "Mauritania" }, { "ms", "Montserrat" }, { "mt", "Malta" }, { "mu", "Mauritius" }, { "mv", "Maldives" }, { "mw", "Malawi" }, { "mx", "Mexico" }, { "mx", "méxico" }, { "my", "Malaysia" }, { "mz", "Mozambique" }, { "na", "Namibia" }, { "nc", "New Caledonia" }, { "ne", "Niger" }, { "nf", "Norfolk Island" }, { "ng", "Nigeria" }, { "ni", "Nicaragua" }, { "nl", "Netherlands" }, { "no", "Norway" }, { "np", "Nepal" }, { "nr", "Nauru" }, { "nu", "Niue" }, { "nz", "New Zealand" }, { "om", "Oman" }, { "pa", "Panama" }, { "pe", "Peru" }, { "pf", "French Polynesia" }, { "pg", "Papua New Guinea" }, { "ph", "Philippines" }, { "pk", "Pakistan" }, { "pl", "Poland" }, { "pm", "Saint Pierre and Miquelon" }, { "pn", "Pitcairn Island" }, { "pr", "Puerto Rico" }, { "ps", "Palestinian Territory, Occupied" }, { "pt", "Portugal" }, { "pw", "Palau" }, { "py", "Paraguay" }, { "qa", "Qatar" }, { "re", "Reunion Island" }, { "ro", "Romania" }, { "rs", "Serbia" }, { "ru", "Russian Federation" }, { "rw", "Rwanda" }, { "sa", "Saudi Arabia" }, { "sb", "Solomon Islands" }, { "sc", "Seychelles" }, { "sd", "Sudan" }, { "se", "Sweden" }, { "sg", "Singapore" }, { "sh", "Saint Helena" }, { "si", "Slovenia" }, { "sj", "Svalbard and Jan Mayen Islands" }, { "sk", "Slovak Republic" }, { "sl", "Sierra Leone" }, { "sm", "San Marino" }, { "sn", "Senegal" }, { "so", "Somalia" }, { "sr", "Suriname" }, { "st", "Sao Tome and Principe" }, { "su", "Soviet Union (being phased out)" }, { "sv", "El Salvador" }, { "sy", "Syrian Arab Republic" }, { "sz", "Swaziland" }, { "tc", "Turks and Caicos Islands" }, { "td", "Chad" }, { "tf", "French Southern Territories" }, { "tg", "Togo" }, { "th", "Thailand" }, { "tj", "Tajikistan" }, { "tk", "Tokelau" }, { "tl", "Timor-Leste" }, { "tm", "Turkmenistan" }, { "tn", "Tunisia" }, { "to", "Tonga" }, { "tp", "East Timor" }, { "tr", "Turkey" }, { "tt", "Trinidad and Tobago" }, { "tv", "Tuvalu" }, { "tw", "Taiwan" }, { "tz", "Tanzania" }, { "ua", "Ukraine" }, { "ug", "Uganda" }, { "uk", "United Kingdom" }, { "um", "United States Minor Outlying Islands" }, { "us", "United States" }, { "gov", "United States" }, { "uy", "Uruguay" }, { "uz", "Uzbekistan" }, { "va", "Holy See (Vatican City State)" }, { "va", "Vatican" }, { "vc", "Saint Vincent and the Grenadines" }, { "ve", "Venezuela" }, { "vg", "Virgin Islands, British" }, { "vi", "Virgin Islands, U.S." }, { "vn", "Vietnam" }, { "vu", "Vanuatu" }, { "wf", "Wallis and Futuna Islands" }, { "ws", "Samoa" }, { "ye", "Yemen" }, { "yt", "Mayotte" }, { "yu", "Yugoslavia" }, { "za", "South Africa" }, { "zm", "Zambia" }, { "zw", "Zimbabwe" }, { "ge", "Georgia" }, { "uk", " UK." }, { "uk", " UK," }, { "uk", " UK," }, { "us", " england." }, { "us", " u.s.a." }, { "us", " usa," }, { "us", " usa." }, { "us", ",usa," }, { "us", " new york" }, { "ru", "Russia" }, { "br", "Brasil" }, { "es", "españa" }, { "us", "stanford" }, { "us", "cornell" }, { "us", "san-francisco" }, { "us", "san francisco" }, { "us", "calfornia" }, { "us", "boston" }, { "us", "atlanta" }, { "us", "chicago" }, { "fr", "cedex" }, { "kr", "Korea" } };

    private TreeMap<String, HashMap<Integer, Integer>> country2year2count = new TreeMap<String, HashMap<Integer, Integer>>();

    private TreeSet<Integer> allYears = new TreeSet<Integer>();

    /** max number or pubmed entries to return */
    private int max_return = 500;

    /** are we debugging */
    private boolean debugging = false;

    /**
	 * got some problem with the DTD of the NCBI. This reader ignores the second line
	 * of the returned XML
	 * @author pierre
	 *
	 */
    private class IgnoreLine2 extends Reader {

        Reader delegate;

        boolean found = false;

        IgnoreLine2(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int c = this.delegate.read();
            if (c == -1) return c;
            if (c == '\n' && !found) {
                while ((c = this.delegate.read()) != -1) {
                    if (c == '\n') break;
                }
                found = true;
                return this.read();
            }
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (found) return this.delegate.read(cbuf, off, len);
            int i = 0;
            while (i < len) {
                int c = read();
                if (c == -1) return (i == 0 ? -1 : i);
                cbuf[off + i] = (char) c;
                ++i;
            }
            return i;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /** creates a new reader */
    private XMLEventReader newReader(URL url) throws IOException {
        debug(url);
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLEventReader reader = null;
        try {
            reader = f.createXMLEventReader(new IgnoreLine2(new InputStreamReader(url.openStream())));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        return reader;
    }

    private void addRecord(String prefix, int year) {
        if (prefix.equals("gov")) prefix = "us";
        if (prefix.equals("su")) prefix = "ru";
        if (prefix.equals("uk")) prefix = "gb";
        HashMap<Integer, Integer> year2count = this.country2year2count.get(prefix);
        if (year2count == null) {
            year2count = new HashMap<Integer, Integer>();
            this.country2year2count.put(prefix, year2count);
        }
        Integer count = year2count.get(year);
        if (count == null) count = 0;
        year2count.put(year, count + 1);
        this.allYears.add(year);
    }

    /** do our stuff */
    private void run(String term) throws IOException, XMLStreamException {
        URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=" + URLEncoder.encode(term, "UTF-8") + "&retstart=0&retmax=" + max_return + "&usehistory=y&retmode=xml&email=plindenbaum_at_yahoo.fr&tool=gis");
        XMLEventReader reader = newReader(url);
        XMLEvent evt;
        String QueryKey = null;
        String WebEnv = null;
        int countId = 0;
        while (!(evt = reader.nextEvent()).isEndDocument()) {
            if (!evt.isStartElement()) continue;
            String tag = evt.asStartElement().getName().getLocalPart();
            if (tag.equals("QueryKey")) {
                QueryKey = reader.getElementText().trim();
            } else if (tag.equals("WebEnv")) {
                WebEnv = reader.getElementText().trim();
            } else if (tag.equals("Id")) {
                countId++;
            }
        }
        reader.close();
        debug(countId);
        if (countId == 0) return;
        url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv=" + URLEncoder.encode(WebEnv, "UTF-8") + "&query_key=" + URLEncoder.encode(QueryKey, "UTF-8") + "&retmode=xml&retmax=" + max_return + "&email=plindenbaum_at_yahoo.fr&tool=gis");
        reader = newReader(url);
        String Affiliation = null;
        Integer year = null;
        this.allYears.clear();
        this.country2year2count.clear();
        countId = 0;
        while (!(evt = reader.nextEvent()).isEndDocument()) {
            if (evt.isStartElement()) {
                String tag = evt.asStartElement().getName().getLocalPart();
                if (tag.equals("Affiliation")) {
                    Affiliation = reader.getElementText().trim().toLowerCase();
                } else if (tag.contains("Year")) {
                    year = Integer.parseInt(reader.getElementText().trim());
                }
            } else if (evt.isEndElement()) {
                String tag = evt.asEndElement().getName().getLocalPart();
                if (tag.equals("PubmedArticle")) {
                    if (Affiliation != null) {
                        Affiliation = Affiliation.replaceAll("\\([ ]*at[ ]*\\)", "@");
                        boolean found = false;
                        for (String mail : Affiliation.split("[ \t\\:\\<,\\>\\(\\)]")) {
                            mail.replaceAll("\\{\\}", "");
                            if (mail.endsWith(".")) mail = mail.substring(0, mail.length() - 1);
                            int index = mail.indexOf('@');
                            if (index == -1) continue;
                            int i = mail.lastIndexOf('.');
                            if (i == -1) continue;
                            String suffix = mail.substring(i + 1);
                            for (i = 0; i < Countries.length; ++i) {
                                if (suffix.equals(Countries[i][0])) {
                                    found = true;
                                    addRecord(Countries[i][0], year);
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            for (int i = 0; i < Countries.length; ++i) {
                                if (Affiliation.contains(Countries[i][1].toLowerCase())) {
                                    found = true;
                                    addRecord(Countries[i][0], year);
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            debug(Affiliation);
                        }
                    } else {
                        debug("No Aff " + Affiliation);
                    }
                    year = null;
                    Affiliation = null;
                }
            }
        }
        reader.close();
        System.out.print("Country");
        for (Integer y : allYears) System.out.print("\t" + y);
        System.out.println("\tTotal");
        int total_of_total = 0;
        for (String prefix : this.country2year2count.keySet()) {
            int total = 0;
            HashMap<Integer, Integer> year2count = this.country2year2count.get(prefix);
            System.out.print(prefix.toUpperCase());
            for (Integer y : allYears) {
                Integer c = year2count.get(y);
                if (c == null) c = 0;
                System.out.print("\t" + c);
                total += c;
            }
            System.out.println("\t" + total);
            total_of_total += total;
        }
        debug(total_of_total);
    }

    private void debug(Object o) {
        if (!debugging) return;
        System.err.println(o);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        NCBIMap prg = new NCBIMap();
        int optind = 0;
        while (optind < args.length) {
            if (args[optind].equals("-n")) {
                prg.max_return = Integer.parseInt(args[++optind]);
            } else if (args[optind].equals("-d")) {
                prg.debugging = !prg.debugging;
            } else if (args[optind].equals("--")) {
                ++optind;
                break;
            } else if (args[optind].startsWith("-")) {
                System.err.print("Unknown option :" + args[optind]);
                return;
            } else {
                break;
            }
            ++optind;
        }
        if (optind + 1 != args.length) {
            System.err.print("Usage:\n\t{-n max-return } {-d} \"Valid Term\"\n-d is for debugging\n");
            return;
        }
        try {
            prg.run(args[optind]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
