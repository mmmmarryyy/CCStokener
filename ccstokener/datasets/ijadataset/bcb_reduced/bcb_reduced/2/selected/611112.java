package org.jdamico.ircivelaclient.comm;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.jdamico.ircivelaclient.view.StaticData;

public class ServletConnection {

    private String webServerStr;

    public ServletConnection(String webServerString) {
        this.webServerStr = webServerString;
    }

    public void send(Serializable obj) throws IOException, ClassNotFoundException {
        URL serverURL = new URL(webServerStr);
        URLConnection connection = serverURL.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
        outputStream.writeObject(obj);
        outputStream.flush();
        outputStream.close();
        ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
        Object response = (Object) inputStream.readObject();
        inputStream.close();
    }

    public String readRemoteFile() throws IOException {
        String response = "";
        boolean eof = false;
        URL url = new URL(StaticData.remoteFile);
        InputStream is = url.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String s;
        s = br.readLine();
        response = s;
        while (!eof) {
            try {
                s = br.readLine();
                if (s == null) {
                    eof = true;
                    br.close();
                } else response += s;
            } catch (EOFException eo) {
                eof = true;
            } catch (IOException e) {
                System.out.println("IO Error : " + e.getMessage());
            }
        }
        return response;
    }

    public static void main(String[] args) {
        ServletConnection sc = new ServletConnection("http://localhost:8080/ivela-web/SaveObjectServlet");
        ArrayList<String> list = new ArrayList<String>();
        list.add("Teste 1");
        try {
            System.out.println("==>" + sc.readRemoteFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("finished");
    }
}
