package sjrd.tricktakinggame.server.logincheckers;

import java.io.*;
import java.net.*;
import sjrd.tricktakinggame.network.server.*;

/**
 * Vérificateur de login qui utilise une requête HTTP et son résultat
 * @author sjrd
 */
public class HTTPRequestLoginChecker implements LoginChecker {

    /**
     * Nom du charset d'encodage des paramètres
     */
    private static final String charsetName = "UTF-8";

    /**
     * URL
     */
    URL url;

    /**
     * Paramètre du nom de login
     */
    String loginNameParam;

    /**
     * Paramètre du mot de passe
     */
    String passwordParam;

    /**
     * Crée le vérificateur de login
     * @param aURL URL à utiliser pour la connexion
     * @param aLoginNameParam Paramètre dans lequel indiquer le nom de login
     * @param aPasswordParam Paramètre dans lequel indiquer le mot de passe
     */
    public HTTPRequestLoginChecker(URL aURL, String aLoginNameParam, String aPasswordParam) {
        super();
        url = aURL;
        loginNameParam = aLoginNameParam;
        passwordParam = aPasswordParam;
    }

    /**
     * {@inheritDoc}
     */
    public LoggedClient checkLogin(String loginName, String password) {
        OutputStreamWriter writer = null;
        BufferedReader response = null;
        try {
            String data = URLEncoder.encode(loginNameParam, charsetName) + "=" + URLEncoder.encode(loginName, charsetName);
            data += "&" + URLEncoder.encode(passwordParam, charsetName) + "=" + URLEncoder.encode(password, charsetName);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(data);
            writer.flush();
            response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = response.readLine()) != null) {
                if (line.equalsIgnoreCase(String.valueOf(true))) return new DefaultLoggedClient(loginName); else if (line.equalsIgnoreCase(String.valueOf(false))) return null;
            }
            return null;
        } catch (IOException error) {
            return null;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
