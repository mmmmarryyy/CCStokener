package src.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ServerIo {

    private static String urlServer = null;

    private static String sessionId = null;

    private ServerIoCB serverIoCB = null;

    protected Message message = null;

    public ServerIo() {
    }

    public ServerIo(ServerIoCB serverIoCB) {
        this.serverIoCB = serverIoCB;
    }

    public static void setUrl(String urlServer) {
        ServerIo.urlServer = urlServer;
    }

    public Message getMessage() {
        return message;
    }

    public void sendReceive() {
        URLConnection urlConnection = send();
        if (serverIoCB != null) {
            IoThread runner = new IoThread(urlConnection);
            runner.start();
            runner.getMessage();
        } else if (urlConnection != null) {
            receive(urlConnection);
            String setCookie = urlConnection.getHeaderField("Set-Cookie");
            boolean cookiFounded = false;
            if (setCookie != null) {
                String[] tabStr = setCookie.split(";");
                for (String str : tabStr) {
                    if (str.contains("JSESSIONID")) {
                        String[] tabValue = str.split("=");
                        sessionId = tabValue[1].trim();
                        cookiFounded = true;
                        break;
                    }
                }
            }
            if (cookiFounded == false) {
                sessionId = null;
            }
        }
    }

    private URLConnection send() {
        return sendServer();
    }

    private URLConnection sendServer() {
        String data = null;
        URL url = null;
        try {
            OutputStreamWriter writer = null;
            url = new URL(urlServer + message.getRequest());
            URLConnection conn = url.openConnection();
            if (sessionId != null && !sessionId.equals("")) {
                conn.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
            }
            conn.setDoOutput(true);
            data = message.getRequestData();
            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            return conn;
        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        }
        return null;
    }

    private void receive(URLConnection urlConnection) {
        if (urlConnection != null) {
            try {
                read(urlConnection.getInputStream(), message);
            } catch (IOException ex) {
            }
        }
    }

    private Message read(InputStream inputStream, Message message) {
        if (inputStream == null) {
            return message;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            int size = 0;
            while ((line = reader.readLine()) != null) {
                message.appendLine(line);
                size += line.length();
                if (serverIoCB != null && message.length() > 100) {
                    serverIoCB.receiveNotify(message.length(), size);
                }
            }
            return (message);
        } catch (IOException ex) {
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
            }
        }
        return message;
    }

    public boolean send(String request, Object object) {
        if (request == null || request.length() == 0 || object == null) {
            return false;
        }
        message = new Message(request);
        message.addXmlParam(object);
        sendReceive();
        return message.getStatus() == 200;
    }

    private class IoThread extends Thread {

        private URLConnection urlConnection = null;

        public IoThread(URLConnection urlConnection) {
            super();
            this.urlConnection = urlConnection;
        }

        public Message getMessage() {
            return message;
        }

        public void run() {
            ServerIo.this.receive(urlConnection);
            if (serverIoCB != null) {
                serverIoCB.setReceiveObject(message.convert());
            }
        }
    }
}
