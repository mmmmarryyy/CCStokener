package webService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import com.google.gson.Gson;

public class ServiceMiCuenta {

    private static final String SERVLET_URL = "http://192.168.1.103:8080/ServiceAndroid/MiCuentaServlet";

    private DTOUsuarioMovil getResultFromServlet(String text) {
        DTOUsuarioMovil user;
        Gson gson = new Gson();
        InputStream in = callService(text);
        if (in != null) {
            user = gson.fromJson(convertStreamToString(in), DTOUsuarioMovil.class);
        } else {
            user = null;
        }
        return user;
    }

    private boolean getResultFromServlet1(String text) {
        Gson gson = new Gson();
        InputStream in = callService(text);
        if (in != null) {
            if (gson.fromJson(convertStreamToString(in), boolean.class)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private InputStream callService(String text) {
        InputStream in = null;
        try {
            URL url = new URL(SERVLET_URL);
            URLConnection conn = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setRequestMethod("POST");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.connect();
            DataOutputStream dataStream = new DataOutputStream(conn.getOutputStream());
            dataStream.writeBytes(text);
            dataStream.flush();
            dataStream.close();
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return in;
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader BR = new BufferedReader(new InputStreamReader(is));
        StringBuilder SB = new StringBuilder();
        String line1 = null;
        try {
            while ((line1 = BR.readLine()) != null) {
                SB.append(line1 + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BR.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return SB.toString();
    }

    public DTOUsuarioMovil misDatos(String usuario) {
        return getResultFromServlet(usuario);
    }

    public boolean darBaja(String usuario) {
        return getResultFromServlet1(usuario + "@");
    }
}
