package edu.upmc.opi.caBIG.caTIES.map.gmaps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import com.google.gdata.client.maps.MapsService;
import com.google.gdata.data.Link;
import com.google.gdata.data.maps.MapEntry;
import com.google.gdata.data.maps.MapFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class CaTIES_MapsTester {

    public static void main(String args[]) {
        try {
            MapsService myService = new MapsService("SPIRIT Maps Tester");
            myService.setUserCredentials(args[0], args[1]);
            getMapInfo(myService);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    public static void getMapInfo(MapsService myService) throws ServiceException, IOException {
        final URL mapSelfUrl = new URL("http://maps.google.com/maps/feeds/maps/userID/full/mapID");
        MapEntry map = myService.getEntry(mapSelfUrl, MapEntry.class);
        System.out.println("\t" + map.getSelfLink().getHref());
        System.out.println(map.getTitle().getPlainText());
        System.out.println(map.getSummary().getPlainText());
    }

    private static void tryThis() {
    }

    private static byte[] pullMapBytes(String directoryLocation) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            URL url = new URL(directoryLocation);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            int response = httpURLConnection.getResponseCode();
            System.out.println("Got response of " + response + " from call to httpUrl");
            if (response == HttpURLConnection.HTTP_OK) {
                InputStream is = httpURLConnection.getInputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toByteArray();
    }

    public static void printUserMaps(MapsService myService) throws ServiceException, IOException {
        final URL feedUrl = new URL("http://maps.google.com/maps/feeds/maps/default/full");
        MapFeed resultFeed = myService.getFeed(feedUrl, MapFeed.class);
        System.out.println(resultFeed.getTitle().getPlainText());
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            MapEntry entry = resultFeed.getEntries().get(i);
            System.out.println(entry.getTitle().getPlainText());
            System.out.println("  Summary: " + entry.getSummary().getPlainText());
            System.out.println("  Self Link: " + entry.getSelfLink().getHref() + "\n");
        }
    }
}
