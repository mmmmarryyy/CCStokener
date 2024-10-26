package utilities.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public abstract class handler {

    public abstract Object processRequest(Object request);

    public abstract Object processResponse(Object response);

    public Object send(URL url, Object params) throws Exception {
        params = processRequest(params);
        String response = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        response += in.readLine();
        while (response != null) response += in.readLine();
        in.close();
        return processResponse(response);
    }
}
