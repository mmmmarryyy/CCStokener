package com.lineadecodigo.java.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DescargarArchivo {

    public static void main(String[] args) {
        String url = "http://i.imgur.com/HZ1hq.jpg";
        String name = "lineadecodigo.jpg";
        String folder = "descargas/";
        File dir = new File(folder);
        if (!dir.exists()) if (!dir.mkdir()) return;
        File file = new File(folder + name);
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            System.out.println("\nempezando descarga: \n");
            System.out.println(">> URL: " + url);
            System.out.println(">> Nombre: " + name);
            System.out.println(">> tama�o: " + conn.getContentLength() + " bytes");
            InputStream in = conn.getInputStream();
            OutputStream out = new FileOutputStream(file);
            int b = 0;
            while (b != -1) {
                b = in.read();
                if (b != -1) out.write(b);
            }
            out.close();
            in.close();
            System.out.println("\ndescarga finalizada\n");
        } catch (MalformedURLException e) {
            System.out.println("la url: " + url + " no es valida!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
