package net.thellium.blipocentric.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import net.thellium.blipocentric.client.services.BlipClientService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class BlipClientServiceImpl extends RemoteServiceServlet implements BlipClientService {

    static final String BLIP_API_URL = "http://api.blip.pl/";

    @Override
    public String getData(String blipApiPath) {
        return getData(blipApiPath, null);
    }

    @Override
    public String getData(String blipApiPath, String authHeader) {
        try {
            URL url = new URL(BLIP_API_URL + blipApiPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (authHeader != null) {
                conn.addRequestProperty("Authorization", "Basic " + authHeader);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer content = new StringBuffer();
            System.out.println("Resp code " + conn.getResponseCode());
            while ((line = reader.readLine()) != null) {
                System.out.println(">> " + line);
                content.append(line);
            }
            reader.close();
            return content.toString();
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
