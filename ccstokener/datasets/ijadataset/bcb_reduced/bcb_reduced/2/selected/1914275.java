package fr.lri.swingstates.applets;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class TestApplet extends Applet {

    String nomFichier;

    TextArea zoneTexte = new TextArea();

    public void init() {
        nomFichier = getParameter("fichier");
        if (nomFichier == null) nomFichier = "textapplet.java";
        setLayout(new BorderLayout());
        add(zoneTexte, BorderLayout.CENTER);
    }

    public String openURL(URL u) throws UnsupportedEncodingException {
        InputStream is;
        byte[] buf = new byte[1024];
        URLConnection urlc = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            urlc = u.openConnection();
            is = urlc.getInputStream();
            int len = 0;
            while ((len = is.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            is.close();
        } catch (IOException e) {
            try {
                ((HttpURLConnection) urlc).getResponseCode();
                InputStream es = ((HttpURLConnection) urlc).getErrorStream();
                int ret = 0;
                while ((ret = es.read(buf)) > 0) {
                }
                es.close();
            } catch (IOException ex) {
            }
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    public void start() {
        if (this.getCodeBase().getProtocol().compareTo("file") == 0) {
            zoneTexte.append("Useless to try and open an URL to the local file system\n");
        } else {
            zoneTexte.append("Useless to try and open a File to URL\n");
        }
        String testTxt;
        try {
            testTxt = openURL(new URL(this.getCodeBase(), "classifier/classifierNCCC.cl"));
            zoneTexte.append(testTxt);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
