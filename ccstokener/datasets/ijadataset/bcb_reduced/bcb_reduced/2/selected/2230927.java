package org.fao.fenix.map.geoserver.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.fao.fenix.domain.exception.FenixException;
import org.fao.fenix.domain.map.geoserver.GeoServer;
import org.fao.fenix.map.geoserver.GeoserverImporter;
import org.fao.fenix.persistence.map.geoserver.GeoServerDao;

public class GeoserverLayerWriter {

    private GeoServerDao geoServerDao;

    private GeoserverImporter geoserverImporter;

    public void writeShapeFileTo(InputStream inputStream, String dsName) {
        List<GeoServer> geoservers = geoServerDao.findAllGeoServers();
        if (geoservers.size() > 1) throw new FenixException("Number of registered Geoservers is more than one (" + geoservers.size() + "). This is not foreseen for now and therefore BOEM!");
        if (geoservers.size() == 0) throw new FenixException("No Geoserver yet published in Fenix at this point. Should be there, something is wrong (usually it is published at startup of Fenix)");
        GeoServer geoserver = geoservers.get(0);
        System.out.println("code to talk with Geoserver RestService to write the shapefile to the database.");
        try {
            System.out.println("------------- Storing Layer to : " + geoserver.getBaseUrl() + "/rest/folders/" + dsName + "/file.zip");
            URL url = new URL(geoserver.getBaseUrl() + "/rest/folders/" + dsName + "/file.zip");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("PUT");
            OutputStream outputStream = con.getOutputStream();
            copyInputStream(inputStream, outputStream);
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStreamReader is = new InputStreamReader(con.getInputStream());
                String response = readIs(is);
                is.close();
                System.out.println(response);
                geoserverImporter.importAddedLayer();
            } else {
                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
                throw new FenixException(con.getResponseCode() + " - " + con.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            throw new FenixException(e);
        } catch (IOException e) {
            throw new FenixException(e);
        }
    }

    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.flush();
        out.close();
    }

    private String readIs(InputStreamReader is) {
        char[] inCh = new char[1024];
        StringBuffer input = new StringBuffer();
        int r;
        try {
            while ((r = is.read(inCh)) > 0) {
                input.append(inCh, 0, r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input.toString();
    }

    public void setGeoServerDao(GeoServerDao geoServerDao) {
        this.geoServerDao = geoServerDao;
    }

    public void setGeoserverImporter(GeoserverImporter geoserverImporter) {
        this.geoserverImporter = geoserverImporter;
    }
}
