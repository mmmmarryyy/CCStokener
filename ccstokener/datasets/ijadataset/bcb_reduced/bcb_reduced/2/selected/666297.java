package com.lyrisoft.chat.server.local;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Vector;
import com.lyrisoft.chat.IConnectionHandler;
import com.lyrisoft.chat.IConnectionListener;

public class HttpConnectionHandler implements IConnectionHandler, Runnable {

    private IConnectionListener _listener;

    private String _readURL;

    private String _writeURL;

    private String _id;

    private DataInputStream _in;

    private int _linesRead = 0;

    private Thread _reader;

    private boolean _keepGoing = true;

    private HttpWriterThread _writer;

    public HttpConnectionHandler(String readURL, String writeURL) throws IOException {
        _readURL = readURL;
        _writeURL = writeURL;
        URL read = new URL(_readURL);
        URLConnection urlConn = read.openConnection();
        urlConn.setUseCaches(false);
        _in = new DataInputStream(urlConn.getInputStream());
        _id = _in.readLine();
        _linesRead++;
        _reader = new Thread(this);
        _writer = new HttpWriterThread(_id, _writeURL);
    }

    public void run() {
        try {
            URL read = null;
            if (_readURL.indexOf("?") >= 0) {
                read = new URL(_readURL + "&id=" + _id);
            } else {
                read = new URL(_readURL + "?id=" + _id);
            }
            while (_keepGoing) {
                String line;
                while ((line = _in.readLine()) != null) {
                    ConnectionHandlerLocal.DEBUG("< " + line);
                    _linesRead++;
                    _listener.incomingMessage(line);
                }
                if (_linesRead == 0) {
                    shutdown(true);
                    return;
                }
                if (_keepGoing) {
                    URLConnection urlConn = read.openConnection();
                    urlConn.setUseCaches(false);
                    _in = new DataInputStream(urlConn.getInputStream());
                    _linesRead = 0;
                }
            }
            System.err.println("HttpReaderThread: stopping gracefully.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            shutdown(true);
        }
    }

    public void queueMessage(String message) {
        _writer.queueMessage(message);
    }

    public void sendImmediately(String s) {
        _writer.send(s);
    }

    public void shutdown(boolean notify) {
        _keepGoing = false;
        _writer.pleaseStop();
        _writer.flushOutputQueue();
        if (notify) {
            _listener.socketClosed();
        }
    }

    public void setListener(IConnectionListener listener) {
        _listener = listener;
        _reader.start();
        _writer.start();
    }
}

/**
 * Write the messages in the queue to the socket's output stream
 */
class HttpWriterThread extends Thread {

    private boolean keepGoing = true;

    private Vector outgoingMessages;

    private String _writeUrl;

    private String _id;

    HttpWriterThread(String id, String url) {
        super("HttpConnectionHandler$WriterThread");
        _id = id;
        _writeUrl = url;
        outgoingMessages = new Vector();
    }

    public void run() {
        try {
            while (keepGoing) {
                flushOutputQueue();
                sleep(25);
            }
            System.err.println("HttpWriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            System.err.println("HttpConnectionHandlerLocal$HttpWriterThread.run(): Interrupted!");
        }
    }

    void flushOutputQueue() {
        while (outgoingMessages.size() > 0) {
            String message = (String) outgoingMessages.elementAt(0);
            outgoingMessages.removeElementAt(0);
            ConnectionHandlerLocal.DEBUG("> " + message);
            send(message);
        }
    }

    void queueMessage(String s) {
        outgoingMessages.addElement(s);
    }

    void pleaseStop() {
        keepGoing = false;
    }

    public void send(String message) {
        String query = "id=" + _id + "&arg=" + URLEncoder.encode(message);
        URL url = null;
        try {
            url = new URL(_writeUrl);
        } catch (MalformedURLException e) {
            return;
        }
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            url = new URL(_writeUrl);
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(query);
            out.flush();
            in = new DataInputStream(conn.getInputStream());
            String line = null;
            while ((line = in.readLine()) != null) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
