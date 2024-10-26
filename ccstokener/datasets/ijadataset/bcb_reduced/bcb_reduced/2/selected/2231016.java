package org.ezfusion.agent.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import org.ezfusion.dataobject.Ticket;
import org.ezfusion.msgsrv.Message;
import org.ezfusion.serviceint.CommunicationMgrSrv;
import org.ezfusion.serviceint.Logger;
import org.ezfusion.serviceint.MsgClient;
import org.ezfusion.serviceint.SensorInt;
import org.ezfusion.serviceint.SrvMgr;
import org.ezfusion.tools.NetworkUtils;

public class CommunicationMgr3 implements Runnable, MsgClient, CommunicationMgrSrv, SensorInt {

    private SrvMgr srvMgr;

    private Ticket loggerTck;

    private Ticket mgrTck, sensorReg;

    private Logger logger;

    private String agentName;

    private Vector<String> allIP;

    private boolean isRunning;

    private int IPbeg, IPend;

    private Vector<String> possibleIP;

    private Hashtable<String, Vector<String>> remoteEF;

    private Vector<String> helloAckAlreadySent;

    private Vector<String> blackList;

    private NetworkUtils net;

    public CommunicationMgr3(SrvMgr sm, String aName) {
        agentName = aName;
        srvMgr = sm;
        loggerTck = new Ticket();
        logger = (Logger) srvMgr.getService("logger", "", loggerTck);
        net = new NetworkUtils();
        allIP = net.getAllIPAddress();
        blackList = new Vector<String>();
        possibleIP = new Vector<String>();
        IPbeg = 1;
        IPend = 10;
        remoteEF = new Hashtable<String, Vector<String>>();
        helloAckAlreadySent = new Vector<String>();
        mgrTck = srvMgr.registerService(CommunicationMgrSrv.class.getName(), this, "communicationmgr");
        sensorReg = srvMgr.registerService(SensorInt.class.getName(), this, "commgr");
        net.startUDP();
    }

    public String getIP(String efName) {
        String result = "127.0.0.1";
        try {
            if ((efName == null) || (efName == "") || agentName.equalsIgnoreCase(efName)) {
                result = (String) allIP.get(0);
            } else if (remoteEF.containsKey(efName)) {
                result = (String) ((Vector<String>) remoteEF.get(efName)).get(0);
            }
        } catch (Exception e) {
            logger.log(this, Level.WARNING, "cannot return an IP address for framework " + efName + " : " + e.getMessage());
        }
        return result;
    }

    private boolean alreadyRegisteredIP(String IP) {
        boolean result = false;
        Enumeration<String> keys = remoteEF.keys();
        String aName;
        Vector<String> remoteIP;
        while (keys.hasMoreElements()) {
            aName = (String) keys.nextElement();
            remoteIP = remoteEF.get(aName);
            for (int i = 0; i < remoteIP.size(); i++) {
                if (((String) remoteIP.get(i)).equalsIgnoreCase(IP)) {
                    result = true;
                }
            }
        }
        if (this.allIP.contains(IP)) result = true;
        return result;
    }

    private void setIPRange(Message msg) {
        try {
            IPbeg = Integer.parseInt(msg.getContent("ipbeg"));
            IPend = Integer.parseInt(msg.getContent("ipend"));
        } catch (Exception e) {
            IPbeg = 1;
            IPend = 10;
            e.printStackTrace();
        }
        logger.log(this, Level.INFO, "IP range has been set from XXX.XXX.XXX." + IPbeg + " to XXX.XXX.XXX." + IPend);
    }

    private void addIP(Message msg) {
        try {
            if (msg.getContent("newip") != null) {
                possibleIP.add(msg.getContent("newip"));
                blackList.remove(msg.getContent("newip"));
                logger.log(this, Level.INFO, "new IP added " + msg.getContent("newip"));
            } else {
                logger.log(this, Level.WARNING, "cannot add a new IP whitout the address !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkNeighbour() {
        logger.log(this, Level.FINE, "checking for neighbours...");
        String[] neibrIP = net.neighbourIPUDP();
        for (int i = 0; i < neibrIP.length; i++) {
            if (!alreadyRegisteredIP(neibrIP[i])) {
                try {
                    sendHello(neibrIP[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.log(this, Level.WARNING, "cannot connect to " + neibrIP[i] + " : " + e.getMessage());
                }
            } else {
            }
        }
    }

    private void sendHello(String remoteIP) {
        Message msg = null;
        for (int i = 0; i < allIP.size(); i++) {
            try {
                msg = new Message((String) allIP.get(i), remoteIP, new Vector<String>());
                msg.addContent("topic", "agent");
                msg.addContent("class", "agent");
                msg.addContent("manager", "communication");
                msg.addContent("action", "hello");
                msg.addContent("agentname", agentName);
                sendMessage(msg);
            } catch (Exception e) {
                logger.log(this, Level.WARNING, "cannot send hello message to " + remoteIP + " : " + e.getMessage());
            }
        }
    }

    private void sendHelloAck(String remoteIP) {
        Message msg = null;
        for (int i = 0; i < allIP.size(); i++) {
            try {
                msg = new Message((String) allIP.get(i), remoteIP, new Vector<String>());
                msg.addContent("topic", "agent");
                msg.addContent("class", "agent");
                msg.addContent("manager", "communication");
                msg.addContent("action", "helloack");
                msg.addContent("agentname", agentName);
                sendMessage(msg);
            } catch (Exception e) {
                logger.log(this, Level.WARNING, "cannot send hello acknowledgement message to " + remoteIP + " : " + e.getMessage());
            }
        }
    }

    private void sendGoodbye(String remoteIP) {
        Message msg = null;
        try {
            msg = new Message((String) allIP.get(0), remoteIP, new Vector<String>());
            msg.addContent("topic", "agent");
            msg.addContent("class", "agent");
            msg.addContent("manager", "communication");
            msg.addContent("action", "goodbye");
            msg.addContent("agentname", agentName);
            sendMessage(msg);
        } catch (Exception e) {
            logger.log(this, Level.WARNING, "cannot send goodbye message to " + remoteIP + " : " + e.getMessage());
        }
    }

    private void releaseRemoteAgents() {
        Enumeration<String> remoteNames = remoteEF.keys();
        String remoteName;
        Vector<String> remoteIPs;
        while (remoteNames.hasMoreElements()) {
            remoteName = (String) remoteNames.nextElement();
            remoteIPs = remoteEF.get(remoteName);
            for (int i = 0; i < remoteIPs.size(); i++) {
                sendGoodbye((String) remoteIPs.get(i));
            }
        }
    }

    public void sendMessage(Message msg) {
        if (!blackList.contains(msg.getTo())) {
            Hashtable<String, String> content = msg.getContent();
            Enumeration<String> keys = content.keys();
            String key;
            String data = "to=" + msg.getTo() + "&from=" + msg.getFrom() + "&";
            while (keys.hasMoreElements()) {
                key = (String) keys.nextElement();
                data += key + "=" + content.get(key) + "&";
            }
            URL url = null;
            try {
                logger.log(this, Level.FINER, "sending " + data + " to " + msg.getTo());
                url = new URL("http://" + msg.getTo() + ":8080/webmsgservice?" + data);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                in.readLine();
                in.close();
                logger.log(this, Level.FINER, "message sent to " + msg.getTo());
            } catch (MalformedURLException e) {
                blackList.add(msg.getTo());
                logger.log(this, Level.WARNING, "an error occured during message sending (" + msg.getTo() + ") : " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                logger.log(this, Level.WARNING, "an error occured during message sending (" + msg.getTo() + ") : " + e.getMessage());
                blackList.add(msg.getTo());
            }
        } else {
            logger.log(this, Level.FINE, "will not send message to " + msg.getTo() + " because black listed IP");
        }
    }

    private void receiveHello(Message msg) {
        String remoteIP = msg.getFrom();
        logger.log(this, Level.FINER, "hello received from " + remoteIP);
        blackList.remove(remoteIP);
        if (helloAckAlreadySent.contains(remoteIP)) {
        } else {
            helloAckAlreadySent.add(remoteIP);
            sendHelloAck(remoteIP);
            sendHello(remoteIP);
        }
    }

    private void receiveHelloAck(Message msg) {
        String remoteIP = msg.getFrom();
        String remoteName = null;
        Vector<String> vect;
        try {
            remoteName = msg.getContent("agentname");
            if (!remoteEF.containsKey(remoteName)) {
                vect = new Vector<String>();
                vect.add(remoteIP);
                remoteEF.put(remoteName, vect);
            } else {
                if ((remoteEF.get(remoteName)).contains(remoteIP)) {
                    (remoteEF.get(remoteName)).add(remoteIP);
                } else {
                }
            }
        } catch (Exception e) {
            logger.log(this, Level.WARNING, "an error occured during acknowledgement saving : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void receiveGoodbye(Message msg) {
        String remoteName = null;
        try {
            remoteName = msg.getContent("agentname");
            if (!remoteEF.containsKey(remoteName)) {
                logger.log(this, Level.INFO, "Goodbye message received from " + remoteName + " but wasn't in memory");
            } else {
                helloAckAlreadySent.remove(msg.getFrom());
                remoteEF.remove(remoteName);
            }
        } catch (Exception e) {
            logger.log(this, Level.WARNING, "an error occured during remote agent suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processMsg(Message msg) {
        try {
            String action = msg.getContent("action");
            if (action.equalsIgnoreCase("hello")) {
                receiveHello(msg);
            } else if (action.equalsIgnoreCase("helloack")) {
                receiveHelloAck(msg);
            } else if (action.equalsIgnoreCase("goodbye")) {
                receiveGoodbye(msg);
            } else if (action.equalsIgnoreCase("setip")) {
                setIPRange(msg);
            } else if (action.equalsIgnoreCase("addip")) {
                addIP(msg);
            }
        } catch (Exception e) {
            logger.log(this, Level.WARNING, "cannot read action in message from " + msg.getFrom() + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Vector<String> getNeighbours() {
        Vector<String> names = new Vector<String>();
        Enumeration<String> efNames = remoteEF.keys();
        String efName;
        while (efNames.hasMoreElements()) {
            efName = (String) efNames.nextElement();
            if (!names.contains(efName)) {
                names.add(efName);
            }
        }
        return names;
    }

    public void stopMgr() {
        net.stopUDP();
        isRunning = false;
        releaseRemoteAgents();
        srvMgr.releaseService(loggerTck);
        srvMgr.unregisterService(mgrTck);
        srvMgr.unregisterService(sensorReg);
    }

    public void run() {
        isRunning = true;
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (isRunning) {
            checkNeighbour();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMsgClientName() {
        return agentName;
    }

    public void newMsg(String topic, String serverIP, Integer serverPort) {
    }

    public void topicRemoved(String topic, String serverIP, Integer serverPort) {
    }

    @Override
    public Vector<String> getAllSNames() {
        Vector<String> sNames = new Vector<String>();
        sNames.add("remoteef");
        sNames.add("blacklist");
        return sNames;
    }

    @Override
    public String getSensorName() {
        return "commgr";
    }

    @Override
    public Object getValue(String sName) {
        Vector<String> aNames = null;
        if (sName.equalsIgnoreCase("remoteef")) {
            aNames = new Vector<String>();
            Enumeration<String> temp = remoteEF.keys();
            while (temp.hasMoreElements()) aNames.add(temp.nextElement());
        } else if (sName.equalsIgnoreCase("blacklist")) {
            aNames = blackList;
        }
        return aNames;
    }

    @Override
    public Vector<Object> getValues(Vector<String> sNames) {
        Vector<Object> values = new Vector<Object>();
        for (int i = 0; i < sNames.size(); i++) values.add(getValue(sNames.get(i)));
        return values;
    }
}
