package server;

import dbwConnection.ResultSet;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Classe permettant de faire le lien entre l'application et le serveur.
 * @author zagyoshi
 */
public class DbwServer {

    private String dbwLocationIndex = null;

    private String dbwLocationIndexSimple = null;

    private String dbwPwd = null;

    private String dbwUserName = null;

    private String encoding = null;

    private String db = null;

    private String host = null;

    private String version = "0.9.4";

    public DbwServer(String locationIndex, String username, String pwd, String host, String db) throws java.sql.SQLException {
        this.dbwLocationIndex = locationIndex + "?psw=" + pwd + "&db=" + db + "&host=" + host + "&username=" + username + "&type=";
        this.dbwLocationIndexSimple = locationIndex;
        this.dbwUserName = username;
        this.host = host;
        this.db = db;
        this.dbwPwd = pwd;
        this.checkConnection();
    }

    private boolean checkConnection() throws java.sql.SQLException {
        String test = this.getPage(dbwLocationIndex + "query");
        if (test.indexOf("<version>" + this.version + "</version>") == -1) throw new java.sql.SQLException("La version du driver 'dbw.php' " + "n'est pas " + this.version + ", merci de mettre le driver 'dbw.php' a jour");
        if (test.indexOf("Unknown MySQL server host") != -1) throw new java.sql.SQLException("Echec connection, le serveur '" + this.host + "' n'existe pas, ou n'est pas accessible");
        if (test.indexOf("<dwbError type=\"parameters mysql\">") != -1) throw new java.sql.SQLException("Echec connection, parametres" + " incorrectes : le login '" + this.dbwUserName + "' ou le password '" + this.dbwPwd + "' est incorecte");
        if (test.indexOf("<dbwError type=\"name_dataBase\">") != -1) throw new java.sql.SQLException("La base " + this.db + " n'existe pas ou n'est pas disponnible");
        this.encoding = Parsing.getEncoding(test);
        return true;
    }

    private String getPage(String sourceAdresse) throws java.sql.SQLException {
        String toreturn = null;
        try {
            URL url = new URL(sourceAdresse);
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            int c = in.read();
            StringBuilder build = new StringBuilder();
            while (c != -1) {
                build.append((char) c);
                c = in.read();
            }
            toreturn = build.toString();
        } catch (Exception e) {
            throw new java.sql.SQLException("Erreur d'adresse url : La page '" + this.dbwLocationIndexSimple + "' n'existe pas");
        }
        return toreturn;
    }

    public ResultSet sendQuery(String query) throws java.sql.SQLException {
        String location = this.dbwLocationIndex + "query";
        String response = this.doPost(query, location);
        return new dbwConnection.ResultSet(Parsing.getData(response));
    }

    public Boolean sendUpdate(String query) {
        String location = this.dbwLocationIndex + "update";
        String response = this.doPost(query, location);
        return response.equals("1");
    }

    /**
     * Execute une requete de type POST sur le serveur
     * @param query la requete a execute
     * @param location l'adresse de la page
     * @return la reponse du serveur
     */
    private String doPost(String query, String location) {
        assert (this.db != null && !this.db.equals("")) : "db ne peut pas etre null ou vide";
        assert (this.dbwLocationIndex != null && !this.dbwLocationIndex.equals("")) : "dbwLocationIndex ne peut pas etre vide ou null";
        assert (this.dbwPwd != null) : "dbwPwd ne peut pas etre null";
        assert (this.dbwUserName != null) : "dbwUserName ne peut pas etre null";
        assert (this.encoding != null && !this.encoding.equals("")) : "encoding ne peut pas etre null ou vide";
        assert (this.host != null && !this.host.equals("")) : "host ne peut pas etre null ou vide";
        String toReturn = "";
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        try {
            String donnees = URLEncoder.encode("request", encoding) + "=" + URLEncoder.encode(query, encoding);
            donnees += "&" + URLEncoder.encode("username", encoding) + "=" + URLEncoder.encode(this.dbwUserName, encoding);
            donnees += "&" + URLEncoder.encode("psw", encoding) + "=" + URLEncoder.encode(this.dbwPwd, encoding);
            donnees += "&" + URLEncoder.encode("db", encoding) + "=" + URLEncoder.encode(this.db, encoding);
            donnees += "&" + URLEncoder.encode("host", encoding) + "=" + URLEncoder.encode(this.host, encoding);
            URL url = new URL(location);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(donnees);
            writer.flush();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                toReturn += ligne;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            }
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
        return toReturn;
    }
}
