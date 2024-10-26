package com.rapidlogix.agent.sync;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.rapidlogix.agent.AgentUtils;
import com.rapidlogix.agent.config.ConfigManager;
import com.rapidlogix.agent.java.config.JavaConfigManager;
import com.rapidlogix.agent.sync.objects.ConfigMessage;
import com.rapidlogix.agent.sync.objects.Operation;
import com.rapidlogix.agent.sync.objects.SyncMessage;
import com.rapidlogix.agent.sync.objects.Transaction;
import com.rapidlogix.agent.sync.objects.VariableSnapshot;

public class HttpSync {

    public boolean configure() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(createUri("configure"));
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(20000);
                conn.setUseCaches(false);
                conn.connect();
            } catch (Exception ex) {
                AgentUtils.printMessage("Cannot connect to " + url.toString() + ": " + ex.getMessage());
                return false;
            }
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(conn.getInputStream()));
            ConfigMessage configMessage = (ConfigMessage) ois.readObject();
            ConfigManager.getInstance().getConfig().setDebug(configMessage.isDebug());
            if (configMessage.getStabilityPeriod() < 1440) {
                ConfigManager.getInstance().getConfig().setStabilityPeriod(configMessage.getStabilityPeriod());
            }
            if (ConfigManager.getInstance().getConfig().isDebug()) {
                JavaConfigManager.getInstance().getConfig().setTransactionThreshold(0);
                JavaConfigManager.getInstance().getConfig().setFunctionThreshold(0);
            }
            ConfigManager.getInstance().getConfig().setDisabled(configMessage.isDisabled());
            return true;
        } catch (Exception e) {
            AgentUtils.printStackTrace(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    public boolean upload(SyncMessage syncMessage) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(createUri("upload"));
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(20000);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=SM11042009");
                conn.connect();
            } catch (Exception ex) {
                AgentUtils.printMessage("Cannot connect to " + url.toString() + ": " + ex.getMessage());
                return false;
            }
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
            dos.writeBytes("--SM11042009\r\nContent-Disposition: form-data; name=\"syncmessage\"; filename=\"syncmessage\"\r\n\r\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            ObjectOutputStream oos = new ObjectOutputStream(gos);
            oos.writeObject(syncMessage);
            oos.flush();
            oos.close();
            byte[] syncMessageBytes = baos.toByteArray();
            AgentUtils.printMessage("message size: " + syncMessageBytes.length);
            dos.write(syncMessageBytes);
            dos.writeBytes("\r\n--SM11042009--");
            dos.flush();
            dos.close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            return true;
        } catch (IOException e) {
            AgentUtils.printStackTrace(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    private String createUri(String command) {
        StringBuffer uriBuf = new StringBuffer();
        uriBuf.append(ConfigManager.getInstance().getConfig().getMonitorUrl().trim());
        if (uriBuf.charAt(uriBuf.length() - 1) != '/') {
            uriBuf.append('/');
        }
        uriBuf.append(command);
        uriBuf.append("?agent=java");
        return uriBuf.toString();
    }

    public static void main(String[] args) throws Exception {
        ArrayList list = new ArrayList();
        for (int i = 0; i < 1000; i++) {
            Transaction t = new Transaction();
            t.setId((long) (Math.random() * 10000000));
            t.setName("http-java");
            t.setScope("http://www.wolframscience.com/nksonline/chapter-3");
            t.setTimestamp(new Date(System.currentTimeMillis()));
            t.setExecutionTime(500 + (int) (Math.random() * 100));
            list.add(t);
            SyncManager.getInstance().addSyncObject(t);
        }
        for (int i = 0; i < 1000; i++) {
            Operation o = new Operation();
            o.setId((long) (Math.random() * 10000000));
            o.setTransactionId((long) (Math.random() * 10000000));
            o.setName("response-time");
            o.setScope("http://www.spiegel.de/netzwelt/tech/0,1518,druck-612268,00.html");
            o.setTimestamp(new Date(System.currentTimeMillis()));
            o.setExecutionTime(500 + (int) (Math.random() * 100));
            list.add(o);
            SyncManager.getInstance().addSyncObject(o);
        }
        for (int i = 0; i < 1000; i++) {
            VariableSnapshot vs = new VariableSnapshot();
            vs.setId((long) (Math.random() * 10000000));
            vs.setInterval((short) (Math.random() * 10));
            vs.setUnit((short) (Math.random() * 10));
            vs.setName("response-time");
            vs.setScope("http://www.wolframscience.com/nksonline/chapter-3");
            vs.setTimestamp(new Date(System.currentTimeMillis()));
            list.add(vs);
            SyncManager.getInstance().addSyncObject(vs);
        }
        FileOutputStream fos = new FileOutputStream("foo.xml");
        XMLEncoder enc = new XMLEncoder(fos);
        enc.writeObject(list);
        enc.flush();
        enc.close();
        fos.close();
    }
}
