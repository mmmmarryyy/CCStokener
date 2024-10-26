package org.rbx.sims.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Country enum maintaining all Country constants.
 * User: Anai
 * Date: Feb 16, 2009
 * Time: 9:06:38 AM
 */
public enum Country {

    UnitedStates("United States", "840", "USA", "US");

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
        Country country = alpha2Map.get(alpha2Code);
        if (country == null) {
            String msg = "Specified alpha 2 code [" + alpha2Code + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return country;
    }

    public static Country fromAlpha3Code(String alpha3Code) {
        Country country = alpha3Map.get(alpha3Code);
        if (country == null) {
            String msg = "Specified alpha 3 code [" + alpha3Code + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return country;
    }

    public static Country fromNumericCode(String numericCode) {
        Country country = numericMap.get(numericCode);
        if (country == null) {
            String msg = "Specified numeric code [" + numericCode + "] does not correspond to " + "any known Country";
            throw new IllegalArgumentException(msg);
        }
        return country;
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
        Map<String, Country> map = new HashMap<String, Country>(240);
        map.put("US", Country.UnitedStates);
        return map;
    }

    private static Map<String, Country> initAlpha3Map() {
        Map<String, Country> map = new HashMap<String, Country>(240);
        map.put("USA", Country.UnitedStates);
        return map;
    }

    private static Map<String, Country> initNumericMap() {
        Map<String, Country> map = new HashMap<String, Country>(240);
        map.put("840", Country.UnitedStates);
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
        if (enumName.startsWith("�")) {
            enumName = enumName.replaceFirst("�", "A");
        } else if (enumName.startsWith("C�tedIvoire")) {
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
        } else if (enumName.equals("R�union")) {
            enumName = "Reunion";
        } else if (enumName.startsWith("S�oTom�")) {
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
