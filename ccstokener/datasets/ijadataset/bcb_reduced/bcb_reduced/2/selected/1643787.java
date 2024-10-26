package com.sts.webmeet.client;

import com.sts.webmeet.api.MessageReader;
import com.sts.webmeet.api.MessageRouter;
import com.sts.webmeet.common.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

public class FileMessageRouter implements MessageRouter, Runnable {

    public FileMessageRouter(URL url) {
        this.url = url;
    }

    public void setTimeListener(TimeListener listener) {
        this.timeListener = listener;
    }

    public void start() {
        try {
            ZipInputStream zis = new ZipInputStream(url.openStream());
            zis.getNextEntry();
            ois = new ObjectInputStream(zis);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        bSecure = url.getProtocol().equalsIgnoreCase("https");
        if (null != cel) {
            cel.connected(bSecure);
        }
        dateStart = new Date();
        bContinue = true;
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        bContinue = false;
        try {
            if (null != ois) {
                ois.close();
            }
            if (thread != Thread.currentThread()) {
                thread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setParticipantInfo(ParticipantInfo pi) {
    }

    public void subscribe(MessageReader reader) {
        vectSubscribers.addElement(reader);
    }

    public void sendMessage(WebmeetMessage message) {
    }

    public void sendMessageToSelf(WebmeetMessage message) {
    }

    public void setConnectionEventListener(ConnectionEventListener cel) {
        this.cel = cel;
    }

    public void run() {
        try {
            while (bContinue) {
                RecordedWebmeetMessage recMess = (RecordedWebmeetMessage) ois.readObject();
                waitUntilRipe(recMess);
                if (null != timeListener) {
                    timeListener.timeUpdate(recMess.getTimestamp());
                }
                WebmeetMessage mess = recMess.getMessage();
                Enumeration enumer = vectSubscribers.elements();
                while (enumer.hasMoreElements()) {
                    ((MessageReader) enumer.nextElement()).readMessage(mess);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != cel) {
            cel.disconnected();
        }
    }

    private void waitUntilRipe(RecordedWebmeetMessage recMess) {
        long dateStamp = (new Date()).getTime() - dateStart.getTime();
        if (dateStamp < recMess.getTimestamp()) {
            try {
                long lWaitTime = recMess.getTimestamp() - dateStamp;
                synchronized (objLock) {
                    objLock.wait(lWaitTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ConnectionEventListener cel;

    private TimeListener timeListener;

    private Thread thread;

    private Date dateStart;

    private ObjectInputStream ois;

    private boolean bContinue;

    private Vector vectSubscribers = new Vector();

    private boolean bSecure;

    private URL url;

    private Object objLock = new Object();
}
