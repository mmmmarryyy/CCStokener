package net.walkingtools;

import java.io.*;
import java.net.*;
import net.walkingtools.j2se.editor.*;

/**
 * Communications class for use with the walking tools server (wtserver project)
 * @author Brett Stalbaum
 * @version 0.1.1
 * @since 0.0.5
 */
public class Communicator implements Runnable {

    private String baseUrl = null;

    private boolean get = true;

    private String servicePath = "";

    private String attributes = "";

    private File uploadFile = null;

    private String answer = null;

    private HiperGpsCommunicatorListener listener = null;

    private volatile boolean running = false;

    private byte[] data = null;

    public Communicator(HiperGpsCommunicatorListener listener, String baseUrl) {
        if (baseUrl != null) {
            this.baseUrl = baseUrl;
        }
        this.listener = listener;
    }

    public synchronized void checkVersion() {
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = true;
        servicePath = "GetVersion";
        attributes = "";
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public synchronized void verifyLogin(String user_email, String password) {
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = true;
        servicePath = "VerifyLogin";
        attributes = "?user_email=" + user_email + "&password=" + password;
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public synchronized void verifyProject(String jsessionid, String hiperGeoId, String currentProjectName) {
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = true;
        servicePath = "VerifyProject";
        attributes = ";jsessionid=" + jsessionid + "?hipergeoid=" + hiperGeoId + "&currentprojectname=" + currentProjectName;
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public synchronized void uploadJad(String jsessionid, File file) {
        uploadFile = file;
        data = new byte[0];
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = false;
        servicePath = "PostJad";
        attributes = ";jsessionid=" + jsessionid;
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public synchronized void uploadJar(String jsessionid, File file) {
        uploadFile = file;
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = false;
        servicePath = "PostJar";
        attributes = ";jsessionid=" + jsessionid;
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public synchronized void confirmUpload(String jsessionid, String hiperGeoId, String currentProjectName) {
        if (running) {
            throw new IllegalStateException("Communication Thread still active");
        }
        get = true;
        servicePath = "ConfirmUpload";
        attributes = ";jsessionid=" + jsessionid + "?hipergeoid=" + hiperGeoId + "&currentprojectname=" + currentProjectName;
        running = true;
        Thread t = new Thread(this);
        t.run();
    }

    public void run() {
        if (get) {
            callService();
        } else {
            postFile();
        }
        running = false;
        listener.communicationRecieved(answer);
    }

    private void callService() {
        try {
            URL url = new URL(baseUrl + servicePath + attributes);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            StringBuffer buf = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                buf.append(inputLine);
            }
            in.close();
            answer = buf.toString();
        } catch (MalformedURLException e) {
            answer = "Malformed Url:" + e.getMessage();
            return;
        } catch (IOException e) {
            answer = "I/O exception: " + e.getMessage();
            return;
        }
    }

    private void postFile() {
        try {
            URL url = new URL(baseUrl + servicePath + attributes);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            FileInputStream in = null;
            DataInputStream din = null;
            OutputStream out = null;
            int fileLength = (int) uploadFile.length();
            data = new byte[fileLength];
            in = new FileInputStream(uploadFile);
            try {
                din = new DataInputStream(in);
                din.readFully(data);
            } catch (EOFException e) {
                answer = "EOFException: ";
                return;
            }
            out = connection.getOutputStream();
            out.write(data);
            out.flush();
            in.close();
            din.close();
            out.close();
            BufferedReader bufr = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = null;
            StringBuffer buffer = new StringBuffer();
            while ((response = bufr.readLine()) != null) {
                buffer.append(response);
            }
            answer = buffer.toString();
        } catch (MalformedURLException e) {
            answer = "Malformed Url: " + e.getMessage();
            return;
        } catch (IOException e) {
            answer = "post I/O exception: " + e.getMessage();
            return;
        }
    }
}
