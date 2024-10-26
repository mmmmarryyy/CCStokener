package com.sts.webmeet.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Vector;
import com.sts.webmeet.api.MessageReader;
import com.sts.webmeet.api.MessageRouter;
import com.sts.webmeet.common.ParticipantInfo;
import com.sts.webmeet.common.RecordedWebmeetMessage;
import com.sts.webmeet.common.RosterExitMessage;
import com.sts.webmeet.common.TimedQueueImpl;
import com.sts.webmeet.common.WebmeetMessage;

public final class HttpMessageRouter implements MessageRouter {

    private Vector vectSubscribers = null;

    private ParticipantInfo pi = null;

    private String strRemote;

    private URL urlInput;

    private boolean bSendExit;

    private boolean bSelfStopped;

    private MessageReceivingThread messageReceivingThread;

    private MessagePostingThread messagePostingThread;

    private TimedQueueImpl queueOut;

    private TimedQueueImpl queueIn;

    private ConnectionEventListener connectionEventListener;

    private TimeListener timeListener;

    public HttpMessageRouter(String strRemote) {
        this.strRemote = strRemote;
        queueOut = new TimedQueueImpl("NetworkMessConn: out", true);
        queueOut.setTimeoutMilliSecs(1000);
        queueIn = new TimedQueueImpl("NetworkMessConn: in", true);
        queueIn.setTimeoutMilliSecs(1000);
        vectSubscribers = new Vector();
    }

    public void setParticipantInfo(ParticipantInfo pi) {
        this.pi = pi;
    }

    public void setConnectionEventListener(ConnectionEventListener cel) {
        connectionEventListener = cel;
    }

    public void setTimeListener(TimeListener listener) {
        this.timeListener = listener;
    }

    public void start() {
        try {
            print("opening " + strRemote);
            urlInput = new URL(strRemote);
            bSelfStopped = false;
            messagePostingThread = new MessagePostingThread();
            try {
                messagePostingThread.setPriority(Thread.MIN_PRIORITY + 1);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            messagePostingThread.start();
            messageReceivingThread = new MessageReceivingThread();
            try {
                messageReceivingThread.setPriority(Thread.MIN_PRIORITY + 1);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            messageReceivingThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            bSelfStopped = true;
            bSendExit = false;
            messagePostingThread.stopSafe();
            print(getClass().getName() + ".stop returned from" + " messagePostingThread.stopSafe()");
            print(getClass().getName() + ".stop returned from" + " eventFiringThread.stopSafe()");
            messageReceivingThread.stopSafe();
            print(getClass().getName() + ".stop returned from" + " messageReceivingThread.stopSafe()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subscribe(MessageReader reader) {
        vectSubscribers.addElement(reader);
    }

    public void sendMessage(WebmeetMessage message) {
        if (null == pi) {
            System.out.println("sendMessage error - sender info null");
        }
        message.setSender(pi);
        queueOut.enqueue(message);
        print("returned from enqueueing message");
    }

    public void sendMessageToSelf(WebmeetMessage message) {
        message.setRecipient(pi);
        sendMessage(message);
    }

    private void postObject(Object obj, String strURL) throws Exception {
        print("entering post object");
        URL url = new URL(strURL);
        URLConnection urlConn = url.openConnection();
        print("HttpNetworkMessageConnection.postObject:returned from url.openConnection()");
        urlConn.setUseCaches(false);
        urlConn.setDoOutput(true);
        urlConn.setRequestProperty("Connection", "Keep-Alive");
        urlConn.setRequestProperty("Content-Type", "application/octet-stream");
        ObjectOutputStream oos = new ObjectOutputStream(urlConn.getOutputStream());
        print("HttpNetworkMessageConnection.postObject:returned from urlConn.getOutputStream()");
        oos.writeObject(obj);
        print("HttpNetworkMessageConnection.postObject:returned from writeObject()");
        oos.flush();
        oos.close();
        InputStream is = urlConn.getInputStream();
        print("HttpNetworkMessageConnection.postObject:returned from getInputStream()");
        while (is.read() != -1) {
        }
        is.close();
        print("exiting postObject");
    }

    private void notifySubscribers(WebmeetMessage mess) {
        print("HttpMessageRouter - notifySubscribers");
        Enumeration clients = vectSubscribers.elements();
        MessageReader reader;
        while (clients.hasMoreElements()) {
            reader = (MessageReader) clients.nextElement();
            try {
                reader.readMessage(mess);
            } catch (Throwable t) {
                System.out.println("error dispatching message:" + mess);
                t.printStackTrace();
            }
        }
    }

    private void fireMessage(Object obj) {
        if (obj != null) {
            WebmeetMessage message = null;
            if (obj instanceof RecordedWebmeetMessage) {
                RecordedWebmeetMessage recMess = (RecordedWebmeetMessage) obj;
                if (null != HttpMessageRouter.this.timeListener) {
                    HttpMessageRouter.this.timeListener.timeUpdate(recMess.getTimestamp());
                }
                message = recMess.getMessage();
            } else if (obj instanceof WebmeetMessage) {
                message = (WebmeetMessage) obj;
            } else {
                System.out.println("Error: unexpected message: " + obj);
            }
            notifySubscribers(message);
        }
    }

    class MessagePostingThread extends StoppableThread {

        public MessagePostingThread() {
            setName(getClass().getName());
        }

        public void run() {
            try {
                while (getContinue()) {
                    Object obj = queueOut.dequeue();
                    if (obj != null) {
                        try {
                            print("returned from dequeue(with object)");
                            postObject(obj, strRemote);
                        } catch (Exception e) {
                            System.out.println("exception in messagePosting thread " + strRemote);
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (bSendExit) {
                WebmeetMessage mess = new RosterExitMessage();
                mess.setSender(pi);
                try {
                    postObject(mess, strRemote);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MessageReceivingThread extends StoppableThread {

        private ObjectInputStream ois;

        private URLConnection urlConnInbound;

        Exception e;

        public MessageReceivingThread() {
            setName(getClass().getName());
            this.setDaemon(true);
        }

        public synchronized void stopSafe() {
            print("entering MessageReceivingThread.stopSafe");
            this.setContinue(false);
            interruptRead();
            print("exiting MessageReceivingThread.stopSafe");
        }

        private void interruptRead() {
            if (ois != null) {
                try {
                    print("about to close ois");
                    ois.close();
                    print("returned from closing ois");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void run() {
            try {
                connect();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                print("MessageReceivingThread exiting(exception connecting");
                return;
            }
            try {
                while (getContinue()) {
                    print("about to block on read (message)");
                    Object obj = ois.readObject();
                    print("returned from reading Message");
                    fireMessage(obj);
                }
            } catch (Exception e) {
                System.out.println("Error reading from connection: " + e.getClass().getName());
                e.printStackTrace();
            }
            if (null != ois) {
                try {
                    ois.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ois = null;
            }
            if ((!bSelfStopped) && connectionEventListener != null) {
                try {
                    connectionEventListener.disconnected();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            print("MessageReceivingThread exiting");
        }

        private void connect() throws IOException {
            urlConnInbound = urlInput.openConnection();
            print("returned from urlInput.openConnection() [message receiving thread]");
            urlConnInbound.setUseCaches(false);
            urlConnInbound.setDoInput(true);
            urlConnInbound.setDoOutput(false);
            urlConnInbound.setRequestProperty("Connection", "close");
            print("returned from setRequestProperty 'Connection' 'close'");
            urlConnInbound.connect();
            print("returned from urlConnInbound.connect()");
            ois = new ObjectInputStream(urlConnInbound.getInputStream());
            String strProtocol = urlConnInbound.getURL().getProtocol();
            boolean bSecure = strProtocol.indexOf("https") == 0;
            print("HTTPMessageRouter: " + strProtocol + " :: " + urlConnInbound.getURL());
            if (connectionEventListener != null) {
                connectionEventListener.connected(bSecure);
            }
            print("returned from urlConnInbound.getInputStream()");
        }
    }

    private static boolean bDebug = false;

    private static void print(String str) {
        if (bDebug) {
            System.out.println(str);
        }
    }
}

class StoppableThread extends Thread {

    public void stopSafe() {
        if (isAlive() && (this != Thread.currentThread())) {
            setContinue(false);
            try {
                this.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    synchronized boolean getContinue() {
        return bContinue;
    }

    synchronized void setContinue(boolean b) {
        this.bContinue = b;
    }

    private boolean bContinue = true;
}
