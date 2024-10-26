package net.sf.ipm.baza;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class KnipJDBC {

    private static final String HOST = "http://knip.pol.lublin.pl/~projekt19/query2.php?query=";

    private static final int DANE_POPRAWNE = 0;

    private static final int BLAD_POLACZENIA = 1;

    private static final int BRAK_BAZY = 2;

    private static final int PROBLEM_SELECT = 3;

    private static final int PROBLEM_INSERT = 4;

    private static final int PROBLEM_UPDATE = 5;

    private static final int PROBLEM_MD5 = 6;

    private String query;

    private String checkSum;

    private short state;

    private short resultCount;

    public ResultSet executeQuery(String query) throws SQLException {
        ResultSet result = new ResultSet();
        query = query.trim();
        String url = HOST + translateQuery(query) + "&cs=" + createCheckSum(query);
        if (query.startsWith("SELECT") || query.startsWith("select")) {
            int fromIndex = query.indexOf("FROM") > 0 ? query.indexOf("FROM") : query.indexOf("from");
            String[] kolumny = query.substring("SELECT".length(), fromIndex).split(",");
            for (int i = 0; i < kolumny.length; i++) {
                kolumny[i] = kolumny[i].trim();
                if (kolumny[i].contains(" AS ")) {
                    kolumny[i] = kolumny[i].split(" ")[2];
                }
            }
            result.setColumnNames(new ArrayList<String>(Arrays.asList(kolumny)));
        }
        try {
            BufferedReader we = new BufferedReader(new InputStreamReader(new URI(url).toURL().openStream()));
            String wiersz;
            int end;
            int start;
            while ((wiersz = we.readLine()) != null) {
                if (wiersz.contains("<table border=0><tr><td id='kodStanu'>")) {
                    start = "<table border=0><tr><td id='kodStanu'>".length();
                    end = wiersz.indexOf("</td></tr>");
                    setState(Short.valueOf(wiersz.substring(start, end)).shortValue());
                    if (!checkResultCount()) {
                        return null;
                    }
                    wiersz = we.readLine();
                    if (wiersz.contains("<tr><td id='iloscWynikow'>")) {
                        start = "<tr><td id='iloscWynikow'>".length();
                        end = wiersz.indexOf("</td></tr>");
                        setResultCount(Short.valueOf(wiersz.substring(start, end)).shortValue());
                    }
                }
                if (wiersz.contains("<tr><td>")) {
                    ArrayList<Object> row = new ArrayList<Object>();
                    while (wiersz.contains("<td>")) {
                        start = wiersz.indexOf("<td>") + "<td>".length();
                        end = wiersz.indexOf("</td>");
                        try {
                            row.add(wiersz.substring(start, end));
                        } catch (Exception e) {
                            return result;
                        }
                        wiersz = wiersz.substring(end + 1);
                    }
                    result.addRow(row);
                }
            }
            return result;
        } catch (MalformedURLException e) {
            System.err.println("Blad tworzenia adresu URL!Sprawdz klase KnipJDBC:19\n" + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Blad strumienia I/O!Sprawdz klase KnipJDBC:19\n" + e.getMessage());
            return null;
        } catch (URISyntaxException e) {
            System.err.println("Blad tłumaczenia URL!Sprawdz klase KnipJDBC:19\n" + e.getMessage());
            return null;
        }
    }

    private boolean checkResultCount() {
        short tmp = getState();
        switch(tmp) {
            case KnipJDBC.BLAD_POLACZENIA:
                System.err.println("Wystapil blad przy polaczeniu z baza danych");
                return false;
            case KnipJDBC.BRAK_BAZY:
                System.err.println("Nieodpowiednia nazwa bazy danych");
                return false;
            case KnipJDBC.PROBLEM_INSERT:
                System.err.println("Wystapil blad przy umieszczeniu danych w bazie danych");
                return false;
            case KnipJDBC.PROBLEM_SELECT:
                System.err.println("Wystapil blad przy wyborze danych z bazy danych");
                return false;
            case KnipJDBC.PROBLEM_UPDATE:
                System.err.println("Wystapil blad przy modyfikowaniu danymi w bazie danych");
                return false;
            case KnipJDBC.PROBLEM_MD5:
                System.err.println("Bledna suma kontrolna kwerendy");
                return false;
            case KnipJDBC.DANE_POPRAWNE:
                return true;
            default:
                System.err.println("Wystapil nieznany blad w pracy z baza danych!");
                return false;
        }
    }

    private String translateQuery(String query) {
        String[] q = query.split(" ");
        String result = "";
        for (int i = 0; i < q.length; i++) {
            result += q[i] + "%20";
        }
        return result.substring(0, result.length() - 3);
    }

    public ResultSet executeDefaultQuery() {
        String tmp = getQuery();
        if (tmp != null && !tmp.equals("")) {
            try {
                return executeQuery(tmp);
            } catch (SQLException e) {
                System.err.println("Wystapil blad w zapytaniu SQL!");
                return null;
            }
        }
        System.err.println("Wystapil blad przy probie wywolania metody executeQuery! " + "Brak zdefiniowanej pola query!");
        return null;
    }

    private String createCheckSum(String query) {
        try {
            return MD5.hashData(query.getBytes());
        } catch (Exception e) {
            System.err.println("Wystapil blad wynikajacy z braku pakietu " + "odpowiedzialnego za tworzenie sumy kontrolnej!");
            return null;
        }
    }

    public KnipJDBC(String query) {
        super();
        this.query = query;
    }

    public KnipJDBC() {
        super();
    }

    public String getCheckSum() {
        return checkSum;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public short getResultCount() {
        return resultCount;
    }

    public short getState() {
        return state;
    }

    private void setState(short state) {
        this.state = state;
    }

    private void setResultCount(short resultCount) {
        this.resultCount = resultCount;
    }

    public static void main(String[] args) {
        KnipJDBC k = new KnipJDBC();
        k.setQuery("select * from kraj");
        ResultSet rs = k.executeDefaultQuery();
        System.out.println(rs);
    }
}
