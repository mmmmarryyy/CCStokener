package de.tu_dortmund.cni.peper.googlemaps;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

public class GoogleMapsTileFactoryInfo extends TileFactoryInfo {

    private static char[][] mapsConst = { { 'q', 'r' }, { 't', 's' } };

    private File cacheDir;

    public GoogleMapsTileFactoryInfo(File cacheDir) {
        super(1, 21, 22, 256, true, true, "http://kh0.google.de/kh?n=404&v=24&t=t", "", "", "");
        System.out.println(cacheDir.getAbsolutePath());
        if (cacheDir == null) throw new NullPointerException("cacheDir may not be null.");
        if (cacheDir.isFile()) throw new IllegalArgumentException("cacheDir must not be a file.");
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) throw new IllegalArgumentException("cacheDir could not be created.");
        }
        this.cacheDir = cacheDir;
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        zoom = getMaximumZoomLevel() + 2 - zoom;
        String s3 = "";
        for (int k = zoom - 2; k >= 0; k--) {
            int divisor = (int) Math.pow(2, k);
            s3 += mapsConst[((int) y / divisor) % 2][((int) x / divisor) % 2];
        }
        File cacheFile = new File(cacheDir, s3 + ".jpg");
        if (!cacheFile.exists()) {
            try {
                urlToFile(new URL(baseURL + s3), cacheFile);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        String returnValue = "";
        try {
            returnValue = cacheFile.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    private void urlToFile(URL url, File file) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        ;
        try {
            URLConnection con = url.openConnection();
            con.connect();
            bis = new BufferedInputStream(con.getInputStream());
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int readBytes;
            while ((readBytes = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, readBytes);
            }
            bis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error loading tile " + url.toString());
        } catch (IOException e) {
            System.err.println("Error loading tile " + url.toString());
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
                System.err.println("Error loading tile " + url.toString());
            }
            if (bis != null) try {
                bis.close();
            } catch (IOException e) {
                System.err.println("Error loading tile " + url.toString());
            }
        }
    }
}
