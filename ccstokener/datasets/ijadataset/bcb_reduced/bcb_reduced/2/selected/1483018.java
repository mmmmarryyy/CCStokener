package net.jlastfm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jlastfm.listeners.LastFmListener;
import net.jlastfm.model.LastFmPlaylist;
import net.jlastfm.model.LastFmTrack;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * This class provides access to all implemented LastFM functions,
 * a radio stream and information on the currently played playlist.
 * 
 * To connect to LastFM create a new instance of this class using either 
 * the default constructor and the connect method or the parameterized 
 * constructor which uses the connect method to establish a connection.  
 * 
 * Next some {@link LastFmListener} may be added to playback the
 * stream with an audio player or simply display the meta data.
 *
 */
public class LastFM extends Thread {

    public static final String VERSION = "0.1a";

    private List<LastFmListener> listeners;

    private Map<String, String> data;

    private String username;

    private String passwordMd5;

    private boolean connected;

    private boolean running;

    private Socket lastFmSocket;

    private InputStream lastFmInputStream;

    private LastFmPlaylist playlist;

    private URL playlistUrl;

    private PlaylistParser playlistParser;

    private Logger logger;

    private boolean skipTrack;

    /**
	 * Creates a new instance which is not connected to lastfm.
	 * Connect has to be called explizit.
	 */
    public LastFM() {
        this.listeners = new ArrayList<LastFmListener>();
        this.running = true;
        this.logger = java.util.logging.Logger.getLogger("LastFmStream");
        this.playlistParser = new PlaylistParser();
        this.skipTrack = false;
        logger.setLevel(Level.FINEST);
    }

    /**
	 * Creates a new instance and connects to lastfm with the
	 * passed on parameters.
	 * 
	 * @param username to logon.
	 * @param password as md5 hash for username. 
	 */
    public LastFM(String username, String passwordMd5) {
        this.username = username;
        this.passwordMd5 = passwordMd5;
    }

    public void addListener(LastFmListener listener) {
        synchronized (this.listeners) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(LastFmListener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    /**
	 * Connects to lastfm and starts streaming to all registered
	 * {@link LastFmListener} using the passed on credential.
	 * 
	 * @param username of the user to login.
	 * @param passwordMd5 MD5 sum of the password.
	 * @throws LastFmException if the connection could not established.
	 */
    public void connect(String username, String passwordMd5) {
        this.username = username;
        this.passwordMd5 = passwordMd5;
        StringBuffer handshakeUrl = new StringBuffer();
        handshakeUrl.append("http://ws.audioscrobbler.com/radio/handshake.php?version=");
        handshakeUrl.append(LastFM.VERSION);
        handshakeUrl.append("&platform=linux&username=");
        handshakeUrl.append(this.username);
        handshakeUrl.append("&passwordmd5=");
        handshakeUrl.append(this.passwordMd5);
        handshakeUrl.append("&language=en&player=aTunes");
        URL url;
        try {
            url = new URL(handshakeUrl.toString());
            URLConnection connection = url.openConnection();
            InputStream inputStream = new BufferedInputStream(connection.getInputStream());
            byte[] buffer = new byte[4069];
            int read = 0;
            StringBuffer result = new StringBuffer();
            while ((read = inputStream.read(buffer)) > -1) {
                result.append((new String(buffer, 0, read)));
            }
            String[] rows = result.toString().split("\n");
            this.data = new HashMap<String, String>();
            for (String row : rows) {
                row = row.trim();
                int firstEquals = row.indexOf("=");
                data.put(row.substring(0, firstEquals), row.substring(firstEquals + 1));
            }
            String streamingUrl = data.get("stream_url");
            streamingUrl = streamingUrl.substring(7);
            int delimiter = streamingUrl.indexOf("/");
            String hostname = streamingUrl.substring(0, delimiter);
            String path = streamingUrl.substring(delimiter + 1);
            String[] tokens = hostname.split(":");
            hostname = tokens[0];
            int port = Integer.parseInt(tokens[1]);
            this.lastFmSocket = new Socket(hostname, port);
            OutputStreamWriter osw = new OutputStreamWriter(this.lastFmSocket.getOutputStream());
            osw.write("GET /" + path + " HTTP/1.0\r\n");
            osw.write("Host: " + hostname + "\r\n");
            osw.write("\r\n");
            osw.flush();
            this.lastFmInputStream = this.lastFmSocket.getInputStream();
            result = new StringBuffer();
            while ((read = this.lastFmInputStream.read(buffer)) > -1) {
                String line = new String(buffer, 0, read);
                result.append(line);
                if (line.contains("\r\n\r\n")) break;
            }
            String response = result.toString();
            logger.info("Result: " + response);
            if (!response.startsWith("HTTP/1.0 200 OK")) {
                this.lastFmSocket.close();
                throw new LastFmException("Could not handshake with lastfm. Check credential!");
            }
            StringBuffer sb = new StringBuffer();
            sb.append("http://");
            sb.append(this.data.get("base_url"));
            sb.append(this.data.get("base_path"));
            sb.append("/xspf.php?sk=");
            sb.append(this.data.get("session"));
            sb.append("&discovery=1&desktop=");
            sb.append(LastFM.VERSION);
            logger.info(sb.toString());
            this.playlistUrl = new URL(sb.toString());
            this.playlist = this.playlistParser.fetchPlaylist(this.playlistUrl.toString());
            Iterator<LastFmTrack> it = this.playlist.iterator();
            while (it.hasNext()) {
                System.out.println(it.next().getCreator());
            }
            this.connected = true;
        } catch (MalformedURLException e) {
            throw new LastFmException("Could not handshake with lastfm", e.getCause());
        } catch (IOException e) {
            throw new LastFmException("Could not initialise lastfm", e.getCause());
        }
    }

    /**
	 * Returns the current playlist used to play
	 * songs.
	 * 
	 * @return the current playlist.
	 */
    public LastFmPlaylist getPlaylist() {
        logger.severe("getPlaylist");
        if (!this.connected) {
            throw new LastFmException("Not connected.");
        }
        this.playlist = this.playlistParser.fetchPlaylist(this.playlistUrl.toString());
        return this.playlist;
    }

    /**
	 * Disconnects from lastFM. To reconnect simply call 
	 * connect. The thread keeps running.
	 */
    public void disconnect() {
        this.connected = false;
    }

    /**
	 * Skipps the current track.
	 */
    public void skip() {
        logger.entering(this.getClass().getName(), "skip");
        StringBuffer skipUrl = new StringBuffer();
        skipUrl.append("control.php?command=skip&session=");
        skipUrl.append(this.data.get("session"));
        OutputStreamWriter osw;
        try {
            osw = new OutputStreamWriter(this.lastFmSocket.getOutputStream());
            osw.write("GET /" + skipUrl.toString() + " HTTP/1.0\r\n");
            osw.write("Host: " + this.lastFmSocket.getInetAddress().getHostName() + "\r\n");
            osw.write("\r\n");
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.skipTrack = true;
        logger.exiting(this.getClass().getName(), "skip");
    }

    /**
	 * Returns the connection state.
	 * 
	 * @return true if successfully connected to lastFM, otherwise false.
	 */
    public boolean isConnected() {
        return connected;
    }

    /**
	 * 
	 * @return
	 */
    public boolean isRunning() {
        return running;
    }

    /**
	 * Closes the connection to lastFm and removes all
	 * registered {@link LastFmListener}.
	 */
    public void shutdown() {
        this.running = false;
        synchronized (this.listeners) {
            Iterator<LastFmListener> it = this.listeners.iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    @Override
    public void run() {
        Iterator<LastFmTrack> it = null;
        while (this.running) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                this.logger.log(Level.FINER, "Thread interrupted");
            }
            while (this.running && !this.connected) {
                continue;
            }
            if (!this.connected) {
                break;
            }
            if (it == null) {
                it = this.playlist.iterator();
            }
            if (!it.hasNext()) {
                this.playlist = this.getPlaylist();
                it = this.playlist.iterator();
                continue;
            }
            LastFmTrack currentTrack = it.next();
            BufferedInputStream is = null;
            try {
                URL trackUrl = new URL(currentTrack.getLocation());
                URLConnection connection = trackUrl.openConnection();
                is = new BufferedInputStream(connection.getInputStream());
                byte[] buffer = new byte[2048];
                int bytesRead = 0;
                synchronized (this.listeners) {
                    for (LastFmListener listener : this.listeners) {
                        listener.startTrack(currentTrack.getCreator(), currentTrack.getTitle(), currentTrack.getAlbum());
                    }
                }
                while (!this.skipTrack && this.connected && (bytesRead = is.read(buffer)) > -1) {
                    byte[] currentData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, currentData, 0, bytesRead);
                    synchronized (this.listeners) {
                        for (LastFmListener listener : this.listeners) {
                            Thread t = new Thread(new DataDispatcher(currentData, listener));
                            t.start();
                        }
                    }
                }
            } catch (IOException e) {
                logger.info("Exception while reading radio data.");
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    logger.info("Could not close radio socket.");
                }
                this.skipTrack = false;
            }
        }
        logger.exiting(this.getClass().getName(), "run()");
    }

    private class DataDispatcher implements Runnable {

        private byte[] data;

        private LastFmListener listener;

        public DataDispatcher(byte[] data, LastFmListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @Override
        public void run() {
            this.listener.newChunkAvailable(data);
        }
    }
}
