package DB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import DB.DatabaseTables.Table;
import General.Phrase;

public class PhraseConnector {

    final String ImageURLFormat = "http://img.freebase.com/api/trans/image_thumb%s?maxwidth=1000&maxheight=1000";

    final String blurbURLFormat = "https://api.freebase.com/api/trans/blurb%s?maxlength=1200";

    private DatabaseTables tables = DatabaseTables.getInstance();

    private static PhraseConnector instance = null;

    private Phrase nextPhrase = null;

    public static PhraseConnector getInstance() {
        if (instance == null) {
            instance = new PhraseConnector();
        }
        return instance;
    }

    private PhraseConnector() {
        super();
    }

    protected Connection openConnection() {
        return DBConnectionPool.getInstance().getConnection();
    }

    protected void closeConnection(Connection conn) {
        DBConnectionPool.getInstance().returnConnection(conn);
    }

    public InputStream getPicture(String id) {
        URL url;
        try {
            url = new URL(String.format(ImageURLFormat, id));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) return connection.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getBlurb(String id) {
        URL url;
        try {
            url = new URL(String.format(blurbURLFormat, id));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "";
            }
            InputStream is = connection.getInputStream();
            try {
                Writer writer = new StringWriter();
                char[] buffer = new char[1300];
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
                return writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public Phrase getRandomPhrase() {
        Phrase result = null;
        if (nextPhrase != null) {
            result = nextPhrase;
            nextPhrase = null;
            Thread thread = new Thread() {

                public void run() {
                    updateNextPhrase();
                }
            };
            thread.start();
        } else {
            result = updateNextPhrase();
            Thread thread = new Thread() {

                public void run() {
                    updateNextPhrase();
                }
            };
            thread.start();
        }
        return result;
    }

    private Phrase updateNextPhrase() {
        Phrase result = null;
        Connection connection = openConnection();
        try {
            Table view = tables.views;
            int attemps = 10;
            while (attemps-- > 0) {
                Statement stmt = connection.createStatement();
                String cat = categoryConnector.getInstance().selectRandomCategory();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + cat.replace(' ', '_').toLowerCase() + " ORDER BY RAND() LIMIT 10");
                while (rs.next()) {
                    if (rs.getString(view.columns[2]).length() < 20 && rs.getString(view.columns[2]).length() > 3) {
                        result = new Phrase(rs.getString(view.columns[2]), rs.getString(view.columns[0]), rs.getString(view.columns[1]), rs.getString(view.columns[3]));
                        String manualArticle = rs.getString(view.columns[4]);
                        if (manualArticle == null || manualArticle.isEmpty()) {
                            manualArticle = getBlurb(rs.getString(view.columns[0]));
                        }
                        result.setBlurb(manualArticle);
                        nextPhrase = result;
                        return result;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = null;
        } finally {
            closeConnection(connection);
        }
        nextPhrase = result;
        return result;
    }

    public int putArticle(String _id, String article, String updater) {
        Connection con = DBConnectionPool.getInstance().getConnection();
        try {
            if (con == null) {
                return -1;
            }
            Table phrases = tables.phrases;
            Table articles = tables.manualArticles;
            Statement st = con.createStatement();
            String q = "INSERT INTO " + articles.name + " (";
            q += articles.columns[1] + " ,";
            q += articles.columns[2] + " ,";
            q += articles.columns[3] + ") VALUES ('";
            q += _id + "','";
            q += article.replace('\'', ' ').replace('\"', ' ') + "','";
            q += updater;
            q += "')";
            st.executeUpdate(q);
            q = "SELECT LAST_INSERT_ID()";
            int id = -1;
            ResultSet rs = st.executeQuery(q);
            if (rs.next()) {
                id = rs.getInt(1);
            }
            if (id > 0) {
                q = "UPDATE " + phrases.name + " SET " + articles.name + "= + id";
                q += " WHERE " + phrases.columns[0] + "=" + _id;
                st.executeUpdate(q);
            }
            return 0;
        } catch (Exception e) {
            System.out.println(e.toString());
            return -1;
        } finally {
            DBConnectionPool.getInstance().returnConnection(con);
        }
    }

    public void addCategory(freebaseTable table) {
        Importer importer = new Importer();
        importer.importTable(table);
    }

    public boolean InitializeCache() {
        Phrase p = getRandomPhrase();
        return p != null;
    }
}
