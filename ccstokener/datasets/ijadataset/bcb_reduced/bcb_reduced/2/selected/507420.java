package br.com.pedroboechat.a16bitrpg.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import br.com.pedroboechat.a16bitrpg.LoadingState;

public class HttpClient implements RemoteClient {

    private String urlSpec;

    private LoadingState state = LoadingState.UNLOADED;

    @Override
    public void connect(String host, int port) throws RequestException {
        urlSpec = "http://" + host + ":" + port;
    }

    @Override
    public void connect(String host, int port, String user, String pass) throws RequestException {
        connect(host, port);
    }

    @Override
    public InputStream download(String path) throws RequestException {
        URLConnection urlConnection;
        BufferedInputStream in;
        ByteArrayOutputStream out;
        int b;
        try {
            urlConnection = new URL(urlSpec + path).openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream());
            out = new ByteArrayOutputStream();
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return new ByteArrayInputStream(out.toByteArray());
        } catch (MalformedURLException e) {
            throw new RequestException("Bad server address.");
        } catch (IOException e) {
            throw new RequestException("Error reading file from server.");
        }
    }

    @Override
    public void upload(String path, byte[] data) throws RequestException {
    }

    @Override
    public LoadingState getState() {
        return state;
    }

    @Override
    public void setState(LoadingState arg0) {
        state = arg0;
    }
}
