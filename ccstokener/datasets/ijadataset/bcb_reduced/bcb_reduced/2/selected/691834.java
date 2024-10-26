package cornell.herbivore.app;

import cornell.herbivore.system.*;
import cornell.herbivore.util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;
import xjava.security.*;
import cryptix.provider.rsa.*;

public class HerbivoreWebProxy {

    private static int msgSent = 0;

    public static final short WEBPROXY_REQUEST_PORT = 60;

    public static final short WEBPROXY_RESPONSE_PORT = 51;

    private static Herbivore herbivore;

    private static HerbivoreClique myclique = null;

    public static void main(String args[]) {
        byte[] destpublicKeyBytes = null;
        String command = "";
        String url = "";
        try {
            String herbivoreConfigFile = null;
            if (args.length > 0) herbivoreConfigFile = args[0];
            if (args.length > 1) {
                if (args.length == 3) {
                    command = args[1];
                    url = args[2];
                } else {
                    System.out.println("Usage: WebProxy configfile [get url]");
                    System.exit(-1);
                }
            }
            herbivore = new Herbivore(herbivoreConfigFile);
            myclique = herbivore.joinClique("clique1");
            (new FileReceiver()).start();
            (new QueryReceiver()).start();
            if (command.equals("get")) {
                byte[] webproxykey = myclique.getRandomMemberPublicKey();
                HerbivoreRemoteEndpoint hre = new HerbivoreRemoteEndpoint(webproxykey);
                HerbivoreUnicastDatagramSocket output = myclique.createUnicastDatagramSocket(WEBPROXY_REQUEST_PORT, hre);
                String hostname = InetAddress.getLocalHost().getHostName();
                String payload = url;
                byte[] msg = (payload.toString()).getBytes();
                System.out.println("Sending a request for " + url);
                output.write(msg);
                System.out.println("Sent " + msg.length + " bytes");
                output.flush();
                output.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * listen to queries and act as a webproxy where necessary
     */
    public static class QueryReceiver extends Thread {

        public void run() {
            byte[] msg = new byte[2048];
            HerbivoreUnicastDatagramServerSocket serverSocket = myclique.createUnicastDatagramServerSocket(WEBPROXY_REQUEST_PORT);
            while (true) {
                byte[] input = serverSocket.receive();
                System.out.println("[" + (new Date()) + "] (" + input.length + ") <<" + (new String(input)) + ">>");
                String requestedFile = new String(input);
                try {
                    URL url = new URL(requestedFile);
                    System.out.println("Fetching " + requestedFile);
                    InputStream s = url.openStream();
                    byte[] filecontentbytes = new byte[2000];
                    int n = s.read(filecontentbytes);
                    s.close();
                    System.out.println("Fetched " + requestedFile + " " + n + " bytes.");
                    String filecontent = new String(filecontentbytes);
                    HerbivoreBroadcastStreamSocket output = myclique.createBroadcastStreamSocket(WEBPROXY_RESPONSE_PORT);
                    byte[] mymsg = filecontent.getBytes();
                    System.out.println("Sending " + mymsg.length + " bytes");
                    output.write(mymsg);
                    output.flush();
                    output.close();
                } catch (Exception e) {
                    Log.exception(e);
                }
            }
        }
    }

    public static class FileReceiver extends Thread {

        public void run() {
            byte[] msg = new byte[2048];
            HerbivoreBroadcastStreamServerSocket serverSocket = myclique.createBroadcastStreamServerSocket(WEBPROXY_RESPONSE_PORT, 0);
            while (true) {
                InputStream input = serverSocket.accept();
                System.out.println("================================> After accept");
                try {
                    int bytesRead = -1;
                    String all = "";
                    while ((bytesRead = input.read(msg)) != -1) {
                        String filecontents = new String(msg, 0, bytesRead);
                        System.out.println("[" + (new Date()) + "] (" + bytesRead + ") <<" + (new String(msg, 0, bytesRead)) + ">>");
                        all += new String(msg, 0, bytesRead);
                        msgSent -= bytesRead;
                        System.out.println("msgSent:" + msgSent);
                    }
                    input.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}
