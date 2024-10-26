package org.jtools.protocol.client.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpattern.io.Input;
import org.jtools.io.handler.ObjectInputHandler;
import org.jtools.io.serial.BackedOutput;
import org.jtools.io.serial.InputImpl;
import org.jtools.protocol.ProtocolUtils;
import org.jtools.protocol.client.AbstractClient;
import org.jtools.protocol.client.ResponseHandler;

public class HttpClient extends AbstractClient {

    private static final Logger LOG = Logger.getLogger(HttpClient.class.getName());

    private final URL url;

    public HttpClient(URL url) {
        this.url = url;
    }

    public HttpClient(String host, int port) throws MalformedURLException {
        this(new URL("http://" + host + ":" + port + "/"));
    }

    protected HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection;
    }

    @Override
    public void send(BackedOutput<ByteArrayOutputStream> output, ResponseHandler handle) throws IOException {
        output.flush();
        HttpURLConnection connection = openConnection();
        connection.setDoOutput(true);
        byte[] arr = output.getBacking().toByteArray();
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        ProtocolUtils.writeMsg(out, arr);
        arr = null;
        out.close();
        DataInputStream in = new DataInputStream(connection.getInputStream());
        arr = ProtocolUtils.readMsg(in);
        in.close();
        LOG.logp(Level.FINEST, HttpClient.class.getName(), "send(BackedOutput<ByteArrayOutputStream>, ResponseHandler)", "read %1$s + 4 bytes", arr.length);
        Input input = new InputImpl<ObjectInputStream>(ObjectInputHandler.getInstance(), new ObjectInputStream(new ByteArrayInputStream(arr)));
        input.getLong();
        handle.handleResponse(input);
    }

    public final void start(boolean deamon) {
    }
}
