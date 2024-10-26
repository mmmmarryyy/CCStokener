package net.juantxu.pentaho.launcher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;

public class Descargador {

    static Logger log = Logger.getLogger(DescargaEInstalaPentahoApp.class);

    public void descargaWin(String APP) {
        log.info("descargando aplicación Win de pentaho" + APP);
        String[] arr = APP.split("/");
        String miApp = arr[arr.length - 1];
        log.info("descargando  " + APP + "en " + miApp);
        descarga(APP, "pentahoApps/" + miApp);
        log.info("descargado " + APP + " en pentahoApps/" + miApp);
        log.info("Descomprimiendo... pentahoApps/" + miApp);
        new UnZip().unZip("pentahoApps/" + miApp);
    }

    public void descargaNix(String APP) {
        log.info("descargando aplicación de pentaho: " + APP);
        String[] arr = APP.split("/");
        String miApp = arr[arr.length - 1];
        log.info("descargando  " + APP + " en " + miApp);
        descarga(APP, "pentahoApps/" + miApp);
        unzipNix("pentahoApps/" + miApp);
    }

    public static void unzipNix(String archivo) {
        String str[] = archivo.split("/");
        File miPath = new File(str[0]);
        log.debug("Ejecutando en :" + miPath.getAbsolutePath());
        Runtime r = Runtime.getRuntime();
        if (archivo.contains(".zip")) {
            new UnZip().unZip(archivo);
        } else {
            try {
                r.exec("tar -xzf " + str[1], null, miPath);
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void descarga(String origen, String destino) {
        log.debug("comenzando la descarga");
        URL url;
        URLConnection con;
        DataInputStream dis;
        FileOutputStream fos;
        byte[] fileData;
        try {
            url = new URL(origen);
            con = url.openConnection();
            dis = new DataInputStream(con.getInputStream());
            fileData = new byte[con.getContentLength()];
            for (int x = 0; x < fileData.length; x++) {
                fileData[x] = dis.readByte();
            }
            dis.close();
            fos = new FileOutputStream(new File(destino));
            fos.write(fileData);
            fos.close();
        } catch (MalformedURLException m) {
            System.out.println(m);
        } catch (IOException io) {
            System.out.println(io);
        }
        log.debug("Descarga finalizada");
    }
}
