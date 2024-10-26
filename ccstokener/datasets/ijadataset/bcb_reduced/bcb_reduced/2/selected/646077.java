package edu.indiana.cs.b534.torrent.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import edu.indiana.cs.b534.torrent.TorrentException;

public class HTTPCommunicationManager {

    private Socket socket;

    private URLConnection connection;

    private InputStream in;

    private OutputStream out;

    public HTTPCommunicationManager() throws TorrentException {
    }

    public InputStream sendReceive(String trackerURL) throws TorrentException {
        try {
            URL url = new URL(trackerURL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            in = conn.getInputStream();
        } catch (MalformedURLException e) {
            throw new TorrentException(e);
        } catch (IOException e) {
            throw new TorrentException(e);
        }
        return in;
    }

    public void close() throws TorrentException {
        try {
            out.close();
            in.close();
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            throw new TorrentException(e);
        }
    }
}
