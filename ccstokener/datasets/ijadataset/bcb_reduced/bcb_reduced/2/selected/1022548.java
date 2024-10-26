package org.sakuracms.util;

import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Country enum maintaining all Country constants.
 *
 * @author Les Hazlewood
 */
public enum Country {

    Afghanistan("Afghanistan", "004", "AFG", "AF"), AlandIslands("Åland Islands", "248", "ALA", "AX"), Albania("Albania", "008", "ALB", "AL"), Algeria("Algeria", "012", "DZA", "DZ"), AmericanSamoa("American Samoa", "016", "ASM", "AS"), Andorra("Andorra", "020", "AND", "AD"), Angola("Angola", "024", "AGO", "AO"), Anguilla("Anguilla", "660", "AIA", "AI"), Antarctica("Antarctica", "010", "ATA", "AQ"), AntiguaAndBarbuda("Antigua and Barbuda", "028", "ATG", "AG"), Argentina("Argentina", "032", "ARG", "AR"), Armenia("Armenia", "051", "ARM", "AM"), Aruba("Aruba", "533", "ABW", "AW"), Australia("Australia", "036", "AUS", "AU"), Austria("Austria", "040", "AUT", "AT"), Azerbaijan("Azerbaijan", "031", "AZE", "AZ"), Bahamas("Bahamas", "044", "BHS", "BS"), Bahrain("Bahrain", "048", "BHR", "BH"), Bangladesh("Bangladesh", "050", "BGD", "BD"), Barbados("Barbados", "052", "BRB", "BB"), Belarus("Belarus", "112", "BLR", "BY"), Belgium("Belgium", "056", "BEL", "BE"), Belize("Belize", "084", "BLZ", "BZ"), Benin("Benin", "204", "BEN", "BJ"), Bermuda("Bermuda", "060", "BMU", "BM"), Bhutan("Bhutan", "064", "BTN", "BT"), Bolivia("Bolivia", "068", "BOL", "BO"), BosniaAndHerzegovina("Bosnia and Herzegovina", "070", "BIH", "BA"), Botswana("Botswana", "072", "BWA", "BW"), BouvetIsland("Bouvet Island", "074", "BVT", "BV"), Brazil("Brazil", "076", "BRA", "BR"), BritishIndianOceanTerritory("British Indian Ocean Territory", "086", "IOT", "IO"), BruneiDarussalam("Brunei Darussalam", "096", "BRN", "BN"), Bulgaria("Bulgaria", "100", "BGR", "BG"), BurkinaFaso("Burkina Faso", "854", "BFA", "BF"), Burundi("Burundi", "108", "BDI", "BI"), Cambodia("Cambodia", "116", "KHM", "KH"), Cameroon("Cameroon", "120", "CMR", "CM"), Canada("Canada", "124", "CAN", "CA"), CapeVerde("Cape Verde", "132", "CPV", "CV"), CaymanIslands("Cayman Islands", "136", "CYM", "KY"), CentralAfricanRepublic("Central African Republic", "140", "CAF", "CF"), Chad("Chad", "148", "TCD", "TD"), Chile("Chile", "152", "CHL", "CL"), China("China", "156", "CHN", "CN"), ChristmasIsland("Christmas Island", "162", "CXR", "CX"), CocosIslands("Cocos (Keeling) Islands", "166", "CCK", "CC"), Colombia("Colombia", "170", "COL", "CO"), Comoros("Comoros", "174", "COM", "KM"), CongoRepublic("Congo, Republic of the", "178", "COG", "CG"), CongoDemocraticRepublic("Congo, The Democratic Republic Of The", "180", "COD", "CD"), CookIslands("Cook Islands", "184", "COK", "CK"), CostaRica("Costa Rica", "188", "CRI", "CR"), CoteDIvoire("Côte d'Ivoire", "384", "CIV", "CI"), Croatia("Croatia", "191", "HRV", "HR"), Cuba("Cuba", "192", "CUB", "CU"), Cyprus("Cyprus", "196", "CYP", "CY"), CzechRepublic("Czech Republic", "203", "CZE", "CZ"), Denmark("Denmark", "208", "DNK", "DK"), Djibouti("Djibouti", "262", "DJI", "DJ"), Dominica("Dominica", "212", "DMA", "DM"), DominicanRepublic("Dominican Republic", "214", "DOM", "DO"), Ecuador("Ecuador", "218", "ECU", "EC"), Egypt("Egypt", "818", "EGY", "EG"), ElSalvador("El Salvador", "222", "SLV", "SV"), EquatorialGuinea("Equatorial Guinea", "226", "GNQ", "GQ"), Eritrea("Eritrea", "232", "ERI", "ER"), Estonia("Estonia", "233", "EST", "EE"), Ethiopia("Ethiopia", "231", "ETH", "ET"), FalklandIslands("Falkland Islands", "238", "FLK", "FK"), FaroeIslands("Faroe Islands", "234", "FRO", "FO"), Fiji("Fiji", "242", "FJI", "FJ"), Finland("Finland", "246", "FIN", "FI"), France("France", "250", "FRA", "FR"), FrenchGuiana("French Guiana", "254", "GUF", "GF"), FrenchPolynesia("French Polynesia", "258", "PYF", "PF"), FrenchSouthernTerritories("French Southern Territories", "260", "ATF", "TF"), Gabon("Gabon", "266", "GAB", "GA"), Gambia("Gambia", "270", "GMB", "GM"), Georgia("Georgia", "268", "GEO", "GE"), Germany("Germany", "276", "DEU", "DE"), Ghana("Ghana", "288", "GHA", "GH"), Gibraltar("Gibraltar", "292", "GIB", "GI"), Greece("Greece", "300", "GRC", "GR"), Greenland("Greenland", "304", "GRL", "GL"), Grenada("Grenada", "308", "GRD", "GD"), Guadeloupe("Guadeloupe", "312", "GLP", "GP"), Guam("Guam", "316", "GUM", "GU"), Guatemala("Guatemala", "320", "GTM", "GT"), Guinea("Guinea", "324", "GIN", "GN"), GuineaBissau("Guinea-Bissau", "624", "GNB", "GW"), Guyana("Guyana", "328", "GUY", "GY"), Haiti("Haiti", "332", "HTI", "HT"), HeardIslandAndMcDonaldIslands("Heard Island and McDonald Islands", "334", "HMD", "HM"), Honduras("Honduras", "340", "HND", "HN"), HongKong("Hong Kong", "344", "HKG", "HK"), Hungary("Hungary", "348", "HUN", "HU"), Iceland("Iceland", "352", "ISL", "IS"), India("India", "356", "IND", "IN"), Indonesia("Indonesia", "360", "IDN", "ID"), Iran("Iran, Islamic Republic of", "364", "IRN", "IR"), Iraq("Iraq", "368", "IRQ", "IQ"), Ireland("Ireland", "372", "IRL", "IE"), Israel("Israel", "376", "ISR", "IL"), Italy("Italy", "380", "ITA", "IT"), Jamaica("Jamaica", "388", "JAM", "JM"), Japan("Japan", "392", "JPN", "JP"), Jordan("Jordan", "400", "JOR", "JO"), Kazakhstan("Kazakhstan", "398", "KAZ", "KZ"), Kenya("Kenya", "404", "KEN", "KE"), Kiribati("Kiribati", "296", "KIR", "KI"), NorthKorea("Korea, Democratic People's Republic of", "408", "PRK", "KP"), SouthKorea("Korea, Republic of", "410", "KOR", "KR"), Kuwait("Kuwait", "414", "KWT", "KW"), Kyrgyzstan("Kyrgyzstan", "417", "KGZ", "KG"), Laos("Lao People's Democratic Republic", "418", "LAO", "LA"), Latvia("Latvia", "428", "LVA", "LV"), Lebanon("Lebanon", "422", "LBN", "LB"), Lesotho("Lesotho", "426", "LSO", "LS"), Liberia("Liberia", "430", "LBR", "LR"), Libya("Libyan Arab Jamahiriya", "434", "LBY", "LY"), Liechtenstein("Liechtenstein", "438", "LIE", "LI"), Lithuania("Lithuania", "440", "LTU", "LT"), Luxembourg("Luxembourg", "442", "LUX", "LU"), Macao("Macao", "446", "MAC", "MO"), Macedonia("Macedonia, The Former Yugoslav Republic of", "807", "MKD", "MK"), Madagascar("Madagascar", "450", "MDG", "MG"), Malawi("Malawi", "454", "MWI", "MW"), Malaysia("Malaysia", "458", "MYS", "MY"), Maldives("Maldives", "462", "MDV", "MV"), Mali("Mali", "466", "MLI", "ML"), Malta("Malta", "470", "MLT", "MT"), MarshallIslands("Marshall Islands", "584", "MHL", "MH"), Martinique("Martinique", "474", "MTQ", "MQ"), Mauritania("Mauritania", "478", "MRT", "MR"), Mauritius("Mauritius", "480", "MUS", "MU"), Mayotte("Mayotte", "175", "MYT", "YT"), Mexico("Mexico", "484", "MEX", "MX"), Micronesia("Micronesia, Federated States of", "583", "FSM", "FM"), Moldova("Moldova, Republic of", "498", "MDA", "MD"), Monaco("Monaco", "492", "MCO", "MC"), Mongolia("Mongolia", "496", "MNG", "MN"), Montserrat("Montserrat", "500", "MSR", "MS"), Morocco("Morocco", "504", "MAR", "MA"), Mozambique("Mozambique", "508", "MOZ", "MZ"), Myanmar("Myanmar", "104", "MMR", "MM"), Namibia("Namibia", "516", "NAM", "NA"), Nauru("Nauru", "520", "NRU", "NR"), Nepal("Nepal", "524", "NPL", "NP"), Netherlands("Netherlands", "528", "NLD", "NL"), NetherlandsAntilles("Netherlands Antilles", "530", "ANT", "AN"), NewCaledonia("New Caledonia", "540", "NCL", "NC"), NewZealand("New Zealand", "554", "NZL", "NZ"), Nicaragua("Nicaragua", "558", "NIC", "NI"), Niger("Niger", "562", "NER", "NE"), Nigeria("Nigeria", "566", "NGA", "NG"), Niue("Niue", "570", "NIU", "NU"), NorfolkIsland("Norfolk Island", "574", "NFK", "NF"), NorthernMarianaIslands("Northern Mariana Islands", "580", "MNP", "MP"), Norway("Norway", "578", "NOR", "NO"), Oman("Oman", "512", "OMN", "OM"), Pakistan("Pakistan", "586", "PAK", "PK"), Palau("Palau", "585", "PLW", "PW"), PalestinianTerritory("Palestinian Territory, Occupied", "275", "PSE", "PS"), Panama("Panama", "591", "PAN", "PA"), PapuaNewGuinea("Papua New Guinea", "598", "PNG", "PG"), Paraguay("Paraguay", "600", "PRY", "PY"), Peru("Peru", "604", "PER", "PE"), Philippines("Philippines", "608", "PHL", "PH"), Pitcairn("Pitcairn", "612", "PCN", "PN"), Poland("Poland", "616", "POL", "PL"), Portugal("Portugal", "620", "PRT", "PT"), PuertoRico("Puerto Rico", "630", "PRI", "PR"), Qatar("Qatar", "634", "QAT", "QA"), Reunion("Réunion", "638", "REU", "RE"), Romania("Romania", "642", "ROU", "RO"), RussianFederation("Russian Federation", "643", "RUS", "RU"), Rwanda("Rwanda", "646", "RWA", "RW"), SaintHelena("Saint Helena", "654", "SHN", "SH"), SaintKittsAndNevis("Saint Kitts and Nevis", "659", "KNA", "KN"), SaintLucia("Saint Lucia", "662", "LCA", "LC"), SaintPierreAndMiquelon("Saint-Pierre and Miquelon", "666", "SPM", "PM"), SaintVincentAndTheGrenadines("Saint Vincent and the Grenadines", "670", "VCT", "VC"), Samoa("Samoa", "882", "WSM", "WS"), SanMarino("San Marino", "674", "SMR", "SM"), SaoTomeAndPrincipe("São Tomé and Príncipe", "678", "STP", "ST"), SaudiArabia("Saudi Arabia", "682", "SAU", "SA"), Senegal("Senegal", "686", "SEN", "SN"), SerbiaAndMontenegro("Serbia and Montenegro", "891", "SCG", "CS"), Seychelles("Seychelles", "690", "SYC", "SC"), SierraLeone("Sierra Leone", "694", "SLE", "SL"), Singapore("Singapore", "702", "SGP", "SG"), Slovakia("Slovakia", "703", "SVK", "SK"), Slovenia("Slovenia", "705", "SVN", "SI"), SolomonIslands("Solomon Islands", "090", "SLB", "SB"), Somalia("Somalia", "706", "SOM", "SO"), SouthAfrica("South Africa", "710", "ZAF", "ZA"), SouthGeorgiaAndTheSouthSandwichIslands("South Georgia and the South Sandwich Islands", "239", "SGS", "GS"), Spain("Spain", "724", "ESP", "ES"), SriLanka("Sri Lanka", "144", "LKA", "LK"), Sudan("Sudan", "736", "SDN", "SD"), Suriname("Suriname", "740", "SUR", "SR"), SvalbardAndJanMayen("Svalbard and Jan Mayen", "744", "SJM", "SJ"), Swaziland("Swaziland", "748", "SWZ", "SZ"), Sweden("Sweden", "752", "SWE", "SE"), Switzerland("Switzerland", "756", "CHE", "CH"), SyrianArabRepublic("Syrian Arab Republic", "760", "SYR", "SY"), Taiwan("Taiwan (Republic of China)", "158", "TWN", "TW"), Tajikistan("Tajikistan", "762", "TJK", "TJ"), TanzaniaUnitedRepublicOf("Tanzania, United Republic Of", "834", "TZA", "TZ"), Thailand("Thailand", "764", "THA", "TH"), TimorLeste("Timor-Leste", "626", "TLS", "TL"), Togo("Togo", "768", "TGO", "TG"), Tokelau("Tokelau", "772", "TKL", "TK"), Tonga("Tonga", "776", "TON", "TO"), TrinidadAndTobago("Trinidad and Tobago", "780", "TTO", "TT"), Tunisia("Tunisia", "788", "TUN", "TN"), Turkey("Turkey", "792", "TUR", "TR"), Turkmenistan("Turkmenistan", "795", "TKM", "TM"), TurksAndCaicosIslands("Turks and Caicos Islands", "796", "TCA", "TC"), Tuvalu("Tuvalu", "798", "TUV", "TV"), Uganda("Uganda", "800", "UGA", "UG"), Ukraine("Ukraine", "804", "UKR", "UA"), UnitedArabEmirates("United Arab Emirates", "784", "ARE", "AE"), UnitedKingdom("United Kingdom", "826", "GBR", "GB"), UnitedStates("United States", "840", "USA", "US"), UnitedStatesMinorOutlyingIslands("United States Minor Outlying Islands", "581", "UMI", "UM"), Uruguay("Uruguay", "858", "URY", "UY"), Uzbekistan("Uzbekistan", "860", "UZB", "UZ"), Vanuatu("Vanuatu", "548", "VUT", "VU"), VaticanCityState("Vatican City State", "336", "VAT", "VA"), Venezuela("Venezuela", "862", "VEN", "VE"), VietNam("Viet Nam", "704", "VNM", "VN"), BritishVirginIslands("Virgin Islands, British", "092", "VGB", "VG"), USVirginIslands("Virgin Islands, U.S.", "850", "VIR", "VI"), WallisAndFutuna("Wallis and Futuna", "876", "WLF", "WF"), WesternSahara("Western Sahara", "732", "ESH", "EH"), Yemen("Yemen", "887", "YEM", "YE"), Zambia("Zambia", "894", "ZMB", "ZM"), Zimbabwe("Zimbabwe", "716", "ZWE", "ZW");

    private static final Map<String, Country> alpha2Map = initAlpha2Map();

    private static final Map<String, Country> alpha3Map = initAlpha3Map();

    private static final Map<String, Country> numericMap = initNumericMap();

    private String name;

    private String numericCode;

    private String alpha3Code;

    private String alpha2Code;

    private Country(String name, String numericCode, String alpha3Code, String alpha2Code) {
        this.name = name;
        this.numericCode = numericCode;
        this.alpha3Code = alpha3Code;
        this.alpha2Code = alpha2Code;
    }

    public String getName() {
        return this.name;
    }

    public String getNumericCode() {
        return this.numericCode;
    }

    public String getAlpha3Code() {
        return this.alpha3Code;
    }

    public String getAlpha2Code() {
        return this.alpha2Code;
    }

    public String toString() {
        return this.name;
    }

    public static Country fromName(String name) {
        return valueOf(getEnumName(name));
    }

    public static Country fromAlpha2Code(String alpha2Code) {
        Country c = alpha2Map.get(alpha2Code);
        if (c == null) {
            String msg = "Specified alpha 2 code [" + alpha2Code + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return c;
    }

    public static Country fromAlpha3Code(String alpha3Code) {
        Country c = alpha3Map.get(alpha3Code);
        if (c == null) {
            String msg = "Specified alpha 3 code [" + alpha3Code + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return c;
    }

    public static Country fromNumericCode(String numericCode) {
        Country c = numericMap.get(numericCode);
        if (c == null) {
            String msg = "Specified numeric code [" + numericCode + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return c;
    }

    public static Country fromNumericCode(int numericCode) {
        String code = "" + numericCode;
        if (code.length() <= 0) {
            code = "000";
        } else if (code.length() == 1) {
            code = "00" + code;
        } else if (code.length() == 2) {
            code = "0" + code;
        }
        return fromNumericCode(code);
    }

    private static Map<String, Country> initAlpha2Map() {
        Map<String, Country> map = new HashMap<String, Country>(values().length);
        for (Country country : values()) {
            map.put(country.getAlpha2Code(), country);
        }
        return map;
    }

    private static Map<String, Country> initAlpha3Map() {
        Map<String, Country> map = new HashMap<String, Country>(values().length);
        for (Country country : values()) {
            map.put(country.getAlpha3Code(), country);
        }
        return map;
    }

    private static Map<String, Country> initNumericMap() {
        Map<String, Country> map = new HashMap<String, Country>(values().length);
        for (Country country : values()) {
            map.put(country.getNumericCode(), country);
        }
        return map;
    }

    private static final String ISO_3166_HOST = "en.wikipedia.org";

    private static final String ISO_3166_TXT_FILE_PATH = "/wiki/ISO_3166-1";

    private static String pullData(String htmlLine) {
        int tdStopIndex = htmlLine.lastIndexOf("</td>");
        return htmlLine.substring(4, tdStopIndex);
    }

    private static String getCountryName(String tableData) {
        int tagCloseIndex = tableData.indexOf('>');
        int tagOpenIndex = tableData.indexOf('<', tagCloseIndex);
        return tableData.substring(tagCloseIndex + 1, tagOpenIndex);
    }

    private static String getEnumName(String countryName) {
        String enumName = countryName.replaceAll(" and ", " And ");
        enumName = enumName.replaceAll(" the ", " The ");
        enumName = enumName.replaceAll("[ \\-,']", "");
        if (enumName.startsWith("Å")) {
            enumName = enumName.replaceFirst("Å", "A");
        } else if (enumName.startsWith("CôtedIvoire")) {
            enumName = "CoteDIvoire";
        } else if (enumName.startsWith("Cocos")) {
            enumName = "CocosIslands";
        } else if (enumName.startsWith("CongoRepublic")) {
            enumName = "CongoRepublic";
        } else if (enumName.startsWith("Congo")) {
            enumName = "CongoDemocraticRepublic";
        } else if (enumName.startsWith("Iran")) {
            enumName = "Iran";
        } else if (enumName.startsWith("KoreaDemocratic")) {
            enumName = "NorthKorea";
        } else if (enumName.startsWith("KoreaRepublic")) {
            enumName = "SouthKorea";
        } else if (enumName.startsWith("LaoPeople")) {
            enumName = "Laos";
        } else if (enumName.startsWith("LibyanArab")) {
            enumName = "Libya";
        } else if (enumName.startsWith("Macedonia")) {
            enumName = "Macedonia";
        } else if (enumName.startsWith("Micronesia")) {
            enumName = "Micronesia";
        } else if (enumName.startsWith("Moldova")) {
            enumName = "Moldova";
        } else if (enumName.startsWith("Palestinian")) {
            enumName = "PalestinianTerritory";
        } else if (enumName.equals("Réunion")) {
            enumName = "Reunion";
        } else if (enumName.startsWith("SãoTomé")) {
            enumName = "SaoTomeAndPrincipe";
        } else if (enumName.startsWith("Taiwan")) {
            enumName = "Taiwan";
        } else if (enumName.startsWith("VirginIslandsB")) {
            enumName = "BritishVirginIslands";
        } else if (enumName.startsWith("VirginIslandsU")) {
            enumName = "USVirginIslands";
        }
        return enumName;
    }

    private static List<CountryEntry> retrieveCountries() throws IOException {
        URL url = new URL("http://" + ISO_3166_HOST + ISO_3166_TXT_FILE_PATH);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        List<CountryEntry> countries = new LinkedList<CountryEntry>();
        boolean parsing = false;
        int trCount = 0;
        int tdCount = 0;
        CountryEntry current = new CountryEntry();
        String nextLine = input.readLine();
        while (nextLine != null) {
            if (nextLine.startsWith("<table")) {
                parsing = true;
            }
            if (nextLine.startsWith("</table>")) {
                break;
            }
            if (parsing) {
                if (nextLine.startsWith("<tr")) {
                    trCount++;
                } else {
                    if ((trCount > 1 && nextLine.startsWith("<td"))) {
                        tdCount++;
                        String data = pullData(nextLine);
                        switch(tdCount) {
                            case 1:
                                current.setName(getCountryName(data));
                                break;
                            case 2:
                                current.setNumber(data);
                                break;
                            case 3:
                                current.setAlpha3(data);
                                break;
                            case 4:
                                current.setAlpha2(data);
                                break;
                            case 5:
                                countries.add(current);
                                current = new CountryEntry();
                                tdCount = 0;
                                break;
                            default:
                                String msg = "Parsing error.  Unexpected column: [" + data + "]";
                                throw new IllegalStateException(msg);
                        }
                    }
                }
            }
            nextLine = input.readLine();
        }
        input.close();
        return countries;
    }

    private static void printCountryDefs(List<CountryEntry> countries) {
        Iterator<CountryEntry> i = countries.iterator();
        while (i.hasNext()) {
            CountryEntry entry = i.next();
            System.out.print("    " + getEnumName(entry.getName()) + "( \"");
            System.out.print(entry.getName() + "\", \"");
            System.out.print(entry.getNumber() + "\", \"");
            System.out.print(entry.getAlpha3() + "\", \"");
            System.out.print(entry.getAlpha2() + "\" )");
            if (i.hasNext()) {
                System.out.println(",");
            } else {
                System.out.println(";");
            }
        }
    }

    private static void printInitAlpha2MapMethod(List<CountryEntry> countries) {
        System.out.println("    public static Map<String,Country> initAlpha2Map() {");
        System.out.println("        Map<String,Country> map = new HashMap<String,Country>(" + countries.size() + ");");
        for (CountryEntry next : countries) {
            System.out.println("        map.put( \"" + next.getAlpha2() + "\", " + "Country." + getEnumName(next.getName()) + " );");
        }
        System.out.println("        return map;");
        System.out.println("    }");
    }

    private static void printInitAlpha3MapMethod(List<CountryEntry> countries) {
        System.out.println("    public static Map<String,Country> initAlpha3Map() {");
        System.out.println("        Map<String,Country> map = new HashMap<String,Country>(" + countries.size() + ");");
        for (CountryEntry next : countries) {
            System.out.println("        map.put( \"" + next.getAlpha3() + "\", " + "Country." + getEnumName(next.getName()) + " );");
        }
        System.out.println("        return map;");
        System.out.println("    }");
    }

    private static void printInitNumericMapMethod(List<CountryEntry> countries) {
        System.out.println("    public static Map<String,Country> initNumericMap() {");
        System.out.println("        Map<String,Country> map = new HashMap<String,Country>(" + countries.size() + ");");
        for (CountryEntry next : countries) {
            System.out.println("        map.put( \"" + next.getNumber() + "\", " + "Country." + getEnumName(next.getName()) + " );");
        }
        System.out.println("        return map;");
        System.out.println("    }");
    }

    /** Utility main method used to generate the source code used in creating this enum. */
    public static void main(String[] args) throws IOException {
        List<CountryEntry> countries = retrieveCountries();
        if (countries != null && !countries.isEmpty()) {
            printCountryDefs(countries);
            System.out.println();
            printInitAlpha2MapMethod(countries);
            System.out.println();
            printInitAlpha3MapMethod(countries);
            System.out.println();
            printInitNumericMapMethod(countries);
            System.out.println();
        }
    }

    private static class CountryEntry {

        private String name = null;

        private String number = null;

        private String alpha2 = null;

        private String alpha3 = null;

        public CountryEntry() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getAlpha2() {
            return alpha2;
        }

        public void setAlpha2(String alpha2) {
            this.alpha2 = alpha2;
        }

        public String getAlpha3() {
            return alpha3;
        }

        public void setAlpha3(String alpha3) {
            this.alpha3 = alpha3;
        }
    }
}
