package architecture.ext.mms.mose;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import architecture.ext.mms.client.MMSClient;

public class MoseMMSServiceHelper {

    static {
        init();
    }

    private static Properties props;

    private static ClassLoader classloader;

    private static MoseMMSClient client;

    private static MoseMMSClient receiver;

    public MoseMMSServiceHelper() {
    }

    private static void init() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> enumeration = cl.getResources("extension-services.properties");
            do {
                if (!enumeration.hasMoreElements()) break;
                URL url = (URL) enumeration.nextElement();
                System.out.println(" - " + url);
                try {
                    props = new Properties();
                    props.load(url.openStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (true);
        } catch (IOException e) {
        }
    }

    public static ClassLoader getClassLoader() {
        return classloader;
    }

    public static MMSClient getMMSClient() {
        if (client == null) {
            try {
                System.out.println("properties : " + props);
                String host = props.getProperty("extension.mose-client.host");
                int port = Integer.parseInt(props.getProperty("extension.mose-client.port", "13000"));
                String soapUrl = props.getProperty("extension.mose-client.soapUrl");
                String username = props.getProperty("extension.mose-client.username");
                String password = props.getProperty("extension.mose-client.password");
                int timeOut = Integer.parseInt(props.getProperty("extension.mose-client.timeOut", "10000"));
                com.dkitec.mose.client.MoseClient moseClient = new com.dkitec.mose.client.MoseClient(host, port, soapUrl, username, password);
                client = new MoseMMSClient(moseClient);
                client.setHost(host);
                client.setPort(port);
                client.setSoapUrl(password);
                client.setUsername(username);
                client.setPassword(password);
                client.setTimeOut(timeOut);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return client;
    }

    public static boolean isGetMMSResultEabled() {
        return Boolean.parseBoolean(props.getProperty("extension.mose-client-reciver.enabled", "false"));
    }

    public static void getMMSResult() {
        if (receiver == null) {
            try {
                String host = props.getProperty("extension.mose-client-reciver.host");
                int port = Integer.parseInt(props.getProperty("extension.mose-client-reciver.port", "13000"));
                String soapUrl = props.getProperty("extension.mose-client-reciver.soapUrl");
                String username = props.getProperty("extension.mose-client-reciver.username");
                String password = props.getProperty("extension.mose-client-reciver.password");
                int timeOut = Integer.parseInt(props.getProperty("extension.mose-client-reciver.timeOut", "10000"));
                com.dkitec.mose.client.MoseClient moseClient = new com.dkitec.mose.client.MoseClient(host, port, soapUrl, username, password);
                receiver = new MoseMMSClient(moseClient);
                client.setHost(host);
                client.setPort(port);
                client.setSoapUrl(password);
                client.setUsername(username);
                client.setPassword(password);
                receiver.setTimeOut(timeOut);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
