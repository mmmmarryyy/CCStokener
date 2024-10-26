package org.fbmc.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.fbmc.FreeboxMediaCenter;

public class EmbeddedVLCServer implements EmbeddedServer {

    private Process vlcProcess;

    public EmbeddedVLCServer() throws EmbeddedServerException {
        try {
            vlcProcess = Runtime.getRuntime().exec(FreeboxMediaCenter.getInstance().getProperty("vlc.cmd"));
            vlcProcess.getOutputStream().close();
            new Thread() {

                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    int nbRead = 0;
                    try {
                        while ((nbRead = vlcProcess.getInputStream().read(buf)) != -1) {
                            System.out.println(new String(buf));
                        }
                    } catch (IOException e) {
                        System.out.println("Error will copying data from a stream to another");
                        e.printStackTrace();
                    }
                }
            }.start();
            new Thread() {

                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    int nbRead = 0;
                    try {
                        while ((nbRead = vlcProcess.getErrorStream().read(buf)) != -1) {
                            System.err.println(new String(buf));
                        }
                    } catch (IOException e) {
                        System.out.println("Error will copying data from a stream to another");
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (IOException ioe) {
            throw new EmbeddedServerException("VLCServer", ioe.getLocalizedMessage());
        }
    }

    public void play(File file) {
        try {
            URL url = new URL("http://127.0.0.1:8081/play.html?type=4&file=" + file.getAbsolutePath() + "&name=toto");
            URLConnection connection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) System.out.println(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        vlcProcess.destroy();
    }
}
