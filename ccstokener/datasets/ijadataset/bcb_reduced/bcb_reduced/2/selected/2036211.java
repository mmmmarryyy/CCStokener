package fr.insee.rome.io.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpRomeRequest implements RomeRequest {

    private String jSessionId;

    private Proxy proxy;

    private Map<String, String> parameters;

    public HttpRomeRequest() {
        this.proxy = Proxy.NO_PROXY;
        this.parameters = new HashMap<String, String>();
    }

    public HttpRomeRequest(Proxy proxy) {
        this.proxy = proxy;
        this.parameters = new HashMap<String, String>();
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    private String getParameters() throws IOException {
        String data = "";
        for (String parameter : parameters.keySet()) {
            data = data + URLEncoder.encode(parameter, "UTF-8") + "=" + URLEncoder.encode(parameters.get(parameter), "UTF-8") + "&";
        }
        if (data.length() > 0) {
            data = data.substring(0, data.length() - 1);
        }
        return data;
    }

    public InputStream doPost(String adress) throws IOException {
        InputStream input = null;
        URL url = new URL(adress);
        URLConnection connection = url.openConnection(proxy);
        connection.setDoOutput(true);
        connection.setRequestProperty("Cookie", jSessionId);
        OutputStream output = connection.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output);
        writer.write(this.getParameters());
        writer.flush();
        writer.close();
        output.flush();
        output.close();
        input = connection.getInputStream();
        return input;
    }

    public String retrieveSessionId(String adress) throws IOException {
        String jSessionId = "";
        URL url = new URL(adress);
        URLConnection connection = url.openConnection(proxy);
        jSessionId = connection.getHeaderField("Set-Cookie");
        jSessionId = jSessionId.substring(0, jSessionId.indexOf(";"));
        this.jSessionId = jSessionId;
        return jSessionId;
    }
}
