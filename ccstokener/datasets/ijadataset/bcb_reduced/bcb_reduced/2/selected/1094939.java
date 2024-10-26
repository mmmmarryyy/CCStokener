package aimee_agent;

import bsh.EvalError;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 *
 * @author steven
 */
class ConnectionManager {

    private static ConnectionManager instance = null;

    private static ChatListener xmppListener = null;

    private static Chat currentChat = null;

    private Message currentMessage;

    public ConnectionManager() {
        currentMessage = new Message();
        currentMessage.addBody("EN-US", "ping");
    }

    static ConnectionManager getDefault() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        try {
            Main.getShell().set("ConnectionManager", instance);
        } catch (EvalError ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
        return instance;
    }

    /**
     * Returns the default chat listener for XMPP
     * @return
     */
    MessageListener getChatListener() {
        if (xmppListener == null) {
            xmppListener = new ChatListener();
        }
        return xmppListener;
    }

    /**
     * Registers a new chat object so that we can take action on it later.
     * @param chat
     */
    void registerChat(Chat chat) {
        currentChat = chat;
    }

    Chat getChat() {
        return currentChat;
    }

    void sendFile(File file, String where) {
        try {
            URL url = new URL(where);
            if (url.getProtocol().startsWith("xmpp")) {
                sendFileByXMPP(file);
            } else {
                sendFileByHTTPs(file, url);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
    }

    /**
     * Sends a file via XMPP to currentChat
     * @param file
     */
    public void sendFileByXMPP(File file) {
        try {
            Message message = new Message();
            String contents = FileService.fileToEncryptedString(file);
            message.setProperty("BackupFileName", file.getAbsolutePath());
            message.setProperty("CurrentKey", PreferenceManager.getDefault().getRaw("key"));
            message.setProperty("BackupFile", contents);
            message.addBody("English", "Sending File");
            if (message.toXML().length() < 102400) {
                currentChat.sendMessage(message);
                Notifier.sendAlert("Backup Sent!", "Backup file was sent to " + currentChat.getParticipant());
            } else {
                try {
                    sendMessageByHTTPs(message.toXML(), new URL(PreferenceManager.getDefault().get("backupURL")));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
                    Notifier.sendException(ex);
                }
            }
        } catch (XMPPException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
    }

    private void sendFileByHTTPs(File file, URL url) {
        sendMessageByHTTPs(FileService.fileToEncryptedString(file), url);
    }

    /**
     * Sends a message where-ever and posts the results back via Notifier
     * @param msg
     * @param url
     */
    private void sendMessageByHTTPs(String msg, URL url) {
        try {
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            String name = PreferenceManager.getDefault().get("login");
            out.writeBytes("requestor=" + URLEncoder.encode(name, "UTF-8"));
            out.writeBytes("&content=" + URLEncoder.encode(msg, "UTF-8"));
            out.writeBytes(msg);
            out.flush();
            out.close();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line).append("\n");
            }
            conn.getInputStream().close();
            Notifier.processLog(result);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
    }

    void addResult(String name, String result) {
        synchronized (currentMessage) {
            currentMessage.setProperty("SeviceCheckResult|" + name, name + "|" + result);
        }
    }

    void send() {
        synchronized (currentMessage) {
            try {
                currentChat.sendMessage(currentMessage);
                currentMessage = new Message();
                currentMessage.addBody("EN-US", "ping");
            } catch (XMPPException ex) {
                Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static class ChatListener implements MessageListener {

        /**
         * Called each time we get an XMPP message
         * @param chat
         * @param msg
         */
        public void processMessage(Chat chat, Message msg) {
            String[] txt = msg.getBody().split("|");
            if (!txt[0].equals("Command")) {
                txt[2] = EncryptionService.Decrypt(txt[2]);
                ExecutionManager.getDefault().add(txt);
            }
        }
    }
}
