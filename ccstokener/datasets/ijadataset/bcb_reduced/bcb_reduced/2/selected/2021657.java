package fi.foyt.cs.android.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;
import fi.foyt.cs.android.Settings;

public class CriminalSoulsClient {

    public static CriminalSoulsClient getInstance() {
        return INSTANCE;
    }

    private static final CriminalSoulsClient INSTANCE = new CriminalSoulsClient();

    public CriminalSoulsClient() {
        protocol = Settings.getSetting("server.protocol");
        host = Settings.getSetting("server.host");
        port = Settings.getSetting("server.port");
        clientId = Settings.getSetting("client.id");
        clientSecret = Settings.getSetting("client.secret");
        authorizePath = Settings.getSetting("server.authorizePath");
        redirectUrl = Settings.getSetting("client.redirectUrl");
        accessTokenPath = Settings.getSetting("server.accessTokenPath");
    }

    public boolean isAuthorized() {
        return getoAuthToken() != null;
    }

    private void setoAuthToken(String oAuthToken) {
        this.oAuthToken = oAuthToken;
    }

    public String getoAuthToken() {
        return oAuthToken;
    }

    public String getAuthorizationUrl() {
        return new StringBuilder().append(protocol).append("://").append(host).append(':').append(port).append(authorizePath).append("?response_type=code").append("&client_id=").append(clientId).append("&redirect_uri=").append(redirectUrl).append("&scope=scenes+tasks+users").toString();
    }

    public void authorize(String code) throws JSONException, IOException {
        StringBuilder urlBuilder = new StringBuilder().append(protocol).append("://").append(host).append(':').append(port).append(accessTokenPath);
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes("client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code + "&grant_type=authorization_code&redirect_uri=" + redirectUrl);
        outputStream.flush();
        outputStream.close();
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = inputStream.readLine()) != null) {
            response.append(line);
        }
        inputStream.close();
        JSONObject responseObject = new JSONObject(response.toString());
        setoAuthToken(responseObject.getString("access_token"));
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthorizePath() {
        return authorizePath;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    private String protocol;

    private String host;

    private String port;

    private String clientId;

    private String clientSecret;

    private String authorizePath;

    private String accessTokenPath;

    private String redirectUrl;

    private String oAuthToken;
}
