package se.sics.tac.aw;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TACHttpConnection extends TACConnection implements Runnable {

    private static final Logger log = Logger.getLogger(TACHttpConnection.class.getName());

    private URL url;

    private ArrayList queue = new ArrayList();

    private boolean disconnected = false;

    protected void init() {
        try {
            url = new URL("http://" + agent.getHost() + ':' + agent.getPort() + '/' + agent.getUser() + '/' + agent.getPassword());
            log.fine("Using HTTP TAC server at " + url);
            new Thread(this).start();
            TACMessage msg = new TACMessage("auth");
            msg.setParameter("userName", agent.getUser());
            msg.setParameter("userPW", agent.getPassword());
            msg.setMessageReceiver(agent);
            sendMessage(msg);
        } catch (Exception e) {
            new RuntimeException("Fatal: " + e);
        }
    }

    public boolean isConnected() {
        return !disconnected;
    }

    public void disconnect() {
        disconnected = true;
    }

    public void run() {
        while (true) {
            TACMessage msg = getMessage();
            boolean sent;
            for (int errors = 0; !(sent = sendMsg(msg)) && errors < 3; errors++) {
                log.warning("failed to send message " + msg.getType() + " (retry " + (errors + 1) + ')');
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!sent) {
                agent.fatalError("could not send message " + msg.getType() + " to server");
            }
        }
    }

    private synchronized TACMessage getMessage() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (TACMessage) queue.remove(0);
    }

    private synchronized void addMessage(TACMessage msg) {
        queue.add(msg);
        notify();
    }

    public void sendMessage(TACMessage msg) throws IOException {
        if (disconnected) {
            throw new IOException("Disconnected from server");
        }
        addMessage(msg);
    }

    private boolean sendMsg(TACMessage msg) {
        try {
            String msgStr = msg.getMessageString();
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Content-Length", "" + msgStr.length());
            conn.setDoOutput(true);
            OutputStream output = conn.getOutputStream();
            output.write(msgStr.getBytes());
            output.flush();
            InputStream input = conn.getInputStream();
            int len = conn.getContentLength();
            int totalRead = 0;
            int read;
            byte[] content = new byte[len];
            while ((len > totalRead) && (read = input.read(content, totalRead, len - totalRead)) > 0) {
                totalRead += read;
            }
            output.close();
            input.close();
            if (len < totalRead) {
                log.severe("truncated message response for " + msg.getType());
                return false;
            } else {
                msgStr = new String(content);
                msg.setReceivedMessage(msgStr);
                msg.deliverMessage();
            }
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "could not send message", e);
            return false;
        }
    }
}
