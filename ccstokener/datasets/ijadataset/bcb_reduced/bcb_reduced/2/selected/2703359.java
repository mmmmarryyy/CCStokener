package client;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import commoms.CommandIF;
import commoms.CommandImpl;
import commoms.ProtocolConstants;

public class AppletConnection extends Thread {

    Socket soc;

    ObjectOutputStream out;

    ObjectInputStream ois;

    RichEditor red;

    String mess;

    ClientApplet cap;

    String sendername;

    boolean accept_reject = false;

    AppletConnection(String ip, int port, ClientApplet cap) {
        try {
            soc = new Socket(ip, port);
            out = new ObjectOutputStream(soc.getOutputStream());
            ois = new ObjectInputStream(soc.getInputStream());
            red = cap.getRichEditor();
            sendername = cap.getSendername();
            this.cap = cap;
        } catch (Exception e) {
            ClientApplet.message = "Connection failed. Check your network";
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        try {
            CommandIF comm = null;
            while (true) {
                comm = (CommandIF) ois.readObject();
                if (comm.getCommand().equals(ProtocolConstants.COMMAND_SEND)) {
                    boolean show = (!comm.isPrivateMessage()) || comm.getMsgFrom().equals(sendername);
                    Iterator itr = comm.getRecipientNames().iterator();
                    boolean flag = false;
                    mess = comm.getMessage();
                    while (itr.hasNext()) {
                        if (itr.next().toString().equals(sendername)) {
                            flag = true;
                        }
                    }
                    show = show || flag;
                    if (show) {
                        red.updateMsg(comm);
                    }
                }
                if (comm.getCommand().equals(ProtocolConstants.UPDATE_PEER_LIST)) {
                    cap.updateUsersList(comm);
                }
                if (comm.getCommand().equals(ProtocolConstants.DOWNLOAD_FILE) && comm.getRecipientNames().contains(sendername)) {
                    showFileAlert(comm);
                    if (cap.acceptFile && downloadFile(comm)) {
                        cap.saveMsg.setText("Saved file " + System.getProperty("user.home") + File.separator + comm.getFileName());
                        cap.ssm.setVisible(true);
                        cap.userPressed = false;
                        cap.acceptFile = false;
                        accept_reject = false;
                        sendDeleteFileCommand(comm);
                    }
                }
            }
        } catch (EOFException eofe) {
        } catch (Exception gg) {
            cap.message = "Failed to connect with server. Try upgrading your JVM";
            cap.showStatus(cap.message);
            gg.printStackTrace();
        }
    }

    boolean sendMessage(CommandIF comm) {
        boolean flag = true;
        try {
            out.writeObject(comm);
        } catch (Exception ie) {
            flag = false;
            ie.printStackTrace();
        }
        if (!flag) {
            red.error(comm.getMessage() + "message sending failed");
        }
        return flag;
    }

    void sendDeleteFileCommand(CommandIF comm) {
        CommandImpl impl = new CommandImpl();
        impl.setMsgFrom(sendername);
        impl.setCommand(ProtocolConstants.DELETE_FILE);
        impl.setFileName(comm.getMsgFrom() + comm.getFileName());
        sendMessage(impl);
    }

    boolean downloadFile(CommandIF comm) {
        boolean flag = false;
        List<String> recp = comm.getRecipientNames();
        for (String str : recp) {
            if (str.equals(sendername)) {
                flag = true;
            }
        }
        flag = flag && cap.acceptFile;
        String fileName = comm.getFileName();
        String url = "http://" + cap.server + "/chat/Upload/" + comm.getMsgFrom() + fileName;
        url = url.replaceAll(" ", "%20");
        try {
            if (flag) {
                URL url1 = new URL(url);
                URLConnection urlconn = url1.openConnection();
                int len = urlconn.getContentLength();
                ByteArrayOutputStream tempBuffer;
                if (len < 0) {
                    tempBuffer = new ByteArrayOutputStream();
                } else {
                    tempBuffer = new ByteArrayOutputStream(len);
                }
                int ch;
                InputStream instream = urlconn.getInputStream();
                while ((ch = instream.read()) >= 0) {
                    tempBuffer.write(ch);
                }
                byte[] b = tempBuffer.toByteArray();
                instream.close();
                tempBuffer.close();
                File out = new File(System.getProperty("user.home") + File.separator + fileName);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(b);
                fos.close();
                cap.acceptFile = false;
            }
        } catch (FileNotFoundException e) {
            flag = false;
            e.printStackTrace();
        } catch (IOException e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    void showFileAlert(CommandIF comm) {
        try {
            String sender = comm.getMsgFrom();
            String filename = comm.getFileName();
            cap.playAlertSound();
            cap.confirmation.setText("Accept " + filename + " from " + sender);
            cap.showConfirmationBox.setVisible(true);
            while (!accept_reject) {
            }
            accept_reject = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
