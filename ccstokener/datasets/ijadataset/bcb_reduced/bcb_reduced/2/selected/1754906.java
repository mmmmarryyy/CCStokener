package net.sf.ipm.baza;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class KnipJDBC2 implements Statement {

    private static final String HOST = "http://knip.pol.lublin.pl/~projekt19/query2.php?query=";

    private String query;

    private byte state;

    private Long resultCount;

    private SQLWarning warning;

    private ResultSet rs;

    private List<String> columnNames;

    private static final int DANE_POPRAWNE = 0;

    private static final int BLAD_POLACZENIA = 1;

    private static final int BRAK_BAZY = 2;

    private static final int PROBLEM_SELECT = 3;

    private static final int PROBLEM_INSERT = 4;

    private static final int PROBLEM_UPDATE = 5;

    private static final int PROBLEM_MD5 = 6;

    public KnipJDBC2() {
        super();
    }

    public KnipJDBC2(String query) {
        this();
        setQuery(query);
    }

    private ResultSet calculate(String query) {
        ResultSet result = new ResultSet();
        String url = HOST + crypt(query);
        try {
            BufferedReader we = new BufferedReader(new InputStreamReader(new URI(url).toURL().openStream()));
            String wiersz;
            int end;
            int start;
            while ((wiersz = we.readLine()) != null) {
                if (wiersz.contains("<table border=0><tr><td id='kodStanu'>")) {
                    start = "<table border=0><tr><td id='kodStanu'>".length();
                    end = wiersz.indexOf("</td></tr>");
                    setState(Byte.valueOf(wiersz.substring(start, end)).byteValue());
                    wiersz = we.readLine();
                    if (wiersz.contains("<tr><td id='iloscWynikow'>")) {
                        start = "<tr><td id='iloscWynikow'>".length();
                        end = wiersz.indexOf("</td></tr>");
                        short iloscWynikow = Short.valueOf(wiersz.substring(start, end)).shortValue();
                        setResultCount(Long.valueOf(wiersz.substring(start, end)).longValue());
                        if (iloscWynikow == 0) {
                            warning.setNextWarning(new SQLWarning("[WARN] Zapytanienie nie zwróciło żadnych danych!"));
                            System.out.println("[WARN] Zapytanienie nie zwróciło żadnych danych!");
                        } else {
                            System.out.println("[WARN] Zapytanie zwróciło: " + iloscWynikow + " rekordów.");
                        }
                    }
                    if (wiersz.contains("<tr id='nazwyKolumn'>")) {
                        start = "<tr id='nazwyKolumn'>".length();
                        end = wiersz.lastIndexOf("</tr>");
                        while (wiersz.contains("<td>")) {
                            int pstart = wiersz.indexOf("<td>");
                            int pend = wiersz.indexOf("</td>");
                            this.columnNames.add(wiersz.substring(pstart + "<td>".length(), pend).trim());
                            wiersz = wiersz.substring(pend + 1);
                        }
                    }
                }
                if (wiersz.contains("<tr><td>")) {
                    ArrayList<Object> row = new ArrayList<Object>();
                    while (wiersz.contains("<td>")) {
                        start = wiersz.indexOf("<td>") + "<td>".length();
                        end = wiersz.indexOf("</td>");
                        row.add(wiersz.substring(start, end));
                        wiersz = wiersz.substring(end + 1);
                    }
                    result.addRow(row);
                }
            }
            rs = result;
            return result;
        } catch (final MalformedURLException e) {
            warning.setNextException(new SQLException("Błąd tworzenia adresu URL! Sprawdź klasę KnipJDBC\n" + e.getMessage()));
            System.err.println("Błąd tworzenia adresu URL! Sprawdź klasę KnipJDBC\n" + e.getMessage());
            return null;
        } catch (final IOException e) {
            warning.setNextException(new SQLException("Błąd strumienia I/O! Sprawdź klasę KnipJDBC\n" + e.getMessage()));
            System.err.println("Błąd strumienia I/O! Sprawdź klasę KnipJDBC\n" + e.getMessage());
            return null;
        } catch (final URISyntaxException e) {
            warning.setNextException(new SQLException("Błąd tłumaczenia URL! Sprawdź klasę KnipJDBC" + e.getMessage()));
            System.err.println("Błąd tłumaczenia URL! Sprawdź klasę KnipJDBC\n" + e.getMessage());
            return null;
        }
    }

    private boolean checkResultCount(final short stan) {
        switch(stan) {
            case KnipJDBC2.BLAD_POLACZENIA:
                warning.setNextException(new SQLException("Wystąpił błąd przy połączeniu z bazą danych"));
                System.err.println("Wystąpił błąd przy połączeniu z bazą danych");
                return false;
            case KnipJDBC2.BRAK_BAZY:
                warning.setNextException(new SQLException("Nieodpowiednia nazwa bazy danych"));
                System.err.println("Nieodpowiednia nazwa bazy danych");
                return false;
            case KnipJDBC2.PROBLEM_INSERT:
                warning.setNextException(new SQLException("Wystąpił błąd przy umieszczeniu danych w bazie danych"));
                System.err.println("Wystąpił błąd przy umieszczeniu danych w bazie danych");
                return false;
            case KnipJDBC2.PROBLEM_SELECT:
                warning.setNextException(new SQLException("Wystąpił błąd przy wyborze danych z bazy danych"));
                System.err.println("Wystąpił błąd przy wyborze danych z bazy danych");
                return false;
            case KnipJDBC2.PROBLEM_UPDATE:
                warning.setNextException(new SQLException("Wystąpił błąd przy modyfikowaniu danymi w bazie danych"));
                System.err.println("Wystąpił błąd przy modyfikowaniu danymi w bazie danych");
                return false;
            case KnipJDBC2.PROBLEM_MD5:
                warning.setNextException(new SQLException("Błędna suma kontrolna kwerendy"));
                System.err.println("Błędna suma kontrolna kwerendy");
                return false;
            case KnipJDBC2.DANE_POPRAWNE:
                return true;
            default:
                warning.setNextException(new SQLException("Wystąpił nieznany błąd w pracy z baza danych"));
                System.err.println("Wystąpił nieznany błąd w pracy z baza danych!");
                return false;
        }
    }

    private String crypt(String query) {
        System.out.println("Zapytanie do zaszyfrowania: " + query);
        String buf = "";
        int length = query.length();
        for (int i = 0; i < length; i++) {
            buf += (char) ((int) query.charAt(i) ^ 255);
        }
        try {
            Key key = IPMKeys.getKey();
            DES encrypter = new DES((SecretKey) key);
            String encrypted = encrypter.encrypt(query);
            encrypted = encrypted.replaceAll("\r\n", "");
            String decrypted = encrypter.decrypt(encrypted);
        } catch (Exception e) {
        }
        return null;
    }

    public void addBatch(String sql) throws SQLException {
        System.out.println("addBatch() -  method hasn't been implemented yet!");
    }

    public void cancel() throws SQLException {
        System.out.println("cancel() -  method hasn't been implemented yet!");
    }

    public void clearBatch() throws SQLException {
        System.out.println("clearBatch() -  method hasn't been implemented yet!");
    }

    public void clearWarnings() throws SQLException {
        warning = null;
    }

    public void close() throws SQLException {
        System.out.println("close() - method hasn't been implemented yet!");
    }

    public boolean execute(String sql) throws SQLException {
        calculate(sql);
        if (state != 0) {
            return false;
        }
        return true;
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        System.out.println("execute(String sql, int autoGeneratedKeys) - method hasn't been implemented yet!");
        return false;
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        System.out.println("execute(String sql, int[] columnIndexes) - method hasn't been implemented yet!");
        return false;
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        System.out.println("execute(String sql, String[] columnNames) - method hasn't been implemented yet!");
        return false;
    }

    public int[] executeBatch() throws SQLException {
        System.out.println("executeBatch() - method hasn't been implemented yet!");
        return null;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        if (sql.contains("select") && sql.trim().indexOf("select") == 0) {
            return calculate(sql);
        } else {
            System.err.println("To nie jest zapytanie SELECT!");
            return null;
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        if (sql.contains("update") || sql.contains("insert") || sql.contains("delete")) {
            calculate(sql);
            return Long.valueOf(getResultCount()).intValue();
        } else {
            System.err.println("To nie jest zapytanie rodzaju UPDATE!");
            return 0;
        }
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        System.out.println("executeUpdate(String sql, int autoGeneratedKeys) - method hasn't been implemented yet!");
        return 0;
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        System.out.println("executeUpdate(String sql, int[] columnIndexes) - method hasn't been implemented yet!");
        return 0;
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        System.out.println("executeUpdate(String sql, String[] columnNames) - method hasn't been implemented yet!");
        return 0;
    }

    public Connection getConnection() throws SQLException {
        System.out.println("getConnection() - method hasn't been implemented yet!");
        return null;
    }

    public int getFetchDirection() throws SQLException {
        System.out.println("getFetchDirection() - method hasn't been implemented yet!");
        return 0;
    }

    public int getFetchSize() throws SQLException {
        System.out.println("getFetchDirection() - method hasn't been implemented yet!");
        return 0;
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        System.out.println("getFetchDirection() - method hasn't been implemented yet!");
        return null;
    }

    public int getMaxFieldSize() throws SQLException {
        System.out.println("getFetchDirection() - method hasn't been implemented yet!");
        return 0;
    }

    public int getMaxRows() throws SQLException {
        return Long.valueOf(getResultCount()).intValue();
    }

    public boolean getMoreResults() throws SQLException {
        System.out.println("getMoreResults() - method hasn't been implemented yet!");
        return false;
    }

    public boolean getMoreResults(int current) throws SQLException {
        System.out.println("getMoreResults(int current) - method hasn't been implemented yet!");
        return false;
    }

    public int getQueryTimeout() throws SQLException {
        System.out.println("getQueryTimeout() - method hasn't been implemented yet!");
        return 0;
    }

    public ResultSet getResultSet() throws SQLException {
        System.out.println("getQueryTimeout() - method hasn't been implemented yet!");
        return null;
    }

    public int getResultSetConcurrency() throws SQLException {
        System.out.println("getResultSetConcurrency() - method hasn't been implemented yet!");
        return 0;
    }

    public int getResultSetHoldability() throws SQLException {
        System.out.println("getResultSetConcurrency() - method hasn't been implemented yet!");
        return 0;
    }

    public int getResultSetType() throws SQLException {
        System.out.println("getResultSetType() - method hasn't been implemented yet!");
        return 0;
    }

    public int getUpdateCount() throws SQLException {
        return Long.valueOf(getResultCount()).intValue();
    }

    public SQLWarning getWarnings() throws SQLException {
        return warning;
    }

    public boolean isClosed() throws SQLException {
        System.out.println("isClosed() - method hasn't been implemented yet!");
        return false;
    }

    public boolean isPoolable() throws SQLException {
        System.out.println("isPoolable() - method hasn't been implemented yet!");
        return false;
    }

    public void setCursorName(String name) throws SQLException {
        System.out.println("setCursorName90 - method hasn't been implemented yet!");
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        System.out.println("setEscapeProcessig() - method hasn't been implemented yet!");
    }

    public void setFetchDirection(int direction) throws SQLException {
        System.out.println("setFetchDirection() - method hasn't been implemented yet!");
    }

    public void setFetchSize(int rows) throws SQLException {
        System.out.println("setFetchSize() - method hasn't been implemented yet!");
    }

    public void setMaxFieldSize(int max) throws SQLException {
        System.out.println("setMaxField - method hasn't been implemented yet!");
    }

    public void setMaxRows(int max) throws SQLException {
        System.out.println("setMaxRows() - method hasn't been implemented yet!");
    }

    public void setPoolable(boolean poolable) throws SQLException {
        System.out.println("setPoolable() - method hasn't been implemented yet!");
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        System.out.println("setQueriTimeout() - method hasn't been implemented yet!");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        System.out.println("isWraperFor() - method hasn't been implemented yet!");
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        System.out.println("unwrap() - method hasn't been implemented yet!");
        return null;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getResultCount() {
        return resultCount;
    }

    public void setResultCount(Long resultCount) {
        this.resultCount = resultCount;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
        if (!checkResultCount(state)) {
            resultCount = new Long(0);
            rs = null;
        }
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }
}
