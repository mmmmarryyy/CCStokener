package com.etracks.dades;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.Context;
import android.util.Log;

public class ServerDownloadRoute {

    private static String TAG = "ServerDownloadRoute";

    private static String URL = Server.URL + "routes/";

    private long localId;

    private Context ctx;

    public ServerDownloadRoute(Context ctx) {
        this.ctx = ctx;
        localId = -1;
    }

    public void downloadRoute(long id) throws ClientProtocolException, IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(URL + id + ".xml");
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        XmlPullParser xmlParser;
        try {
            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
            xmlParser = parserCreator.newPullParser();
            xmlParser.setInput(entity.getContent(), null);
            double lat = 0;
            double lon = 0;
            double h = 0;
            int parserEvent = xmlParser.getEventType();
            while (parserEvent != XmlPullParser.END_DOCUMENT) {
                if (parserEvent == XmlPullParser.START_TAG) {
                    String tag = xmlParser.getName();
                    if (tag.equals("route")) {
                        String name = xmlParser.getAttributeValue(null, "title");
                        String description = xmlParser.getAttributeValue(null, "description");
                        String type = xmlParser.getAttributeValue(null, "type");
                        double desnivell = Double.valueOf(xmlParser.getAttributeValue(null, "ramp"));
                        double distance = Double.valueOf(xmlParser.getAttributeValue(null, "distance"));
                        long serverId = Long.valueOf(xmlParser.getAttributeValue(null, "id"));
                        DataBaseRoutes dbr = new DataBaseRoutes(ctx);
                        dbr.open();
                        localId = dbr.insertRoute();
                        dbr.updateRoute(localId, name, type, description);
                        dbr.updateDetails(localId, distance, desnivell);
                        dbr.setServerId(localId, serverId);
                        dbr.close();
                    }
                    if (tag.equals("point")) {
                        lat = Double.valueOf(xmlParser.getAttributeValue(null, "latitude"));
                        lon = Double.valueOf(xmlParser.getAttributeValue(null, "long"));
                        h = Double.valueOf(xmlParser.getAttributeValue(null, "heigh"));
                        DataBasePoints dbp = new DataBasePoints(ctx);
                        dbp.open();
                        dbp.insertPoint(localId, lat, lon, h);
                        dbp.close();
                    }
                    if (tag.equals("photo")) {
                        String url = xmlParser.getAttributeValue(null, "url");
                        if (url != null) {
                            String description = xmlParser.getAttributeValue(null, "description");
                            DataBasePhotos dbp = new DataBasePhotos(ctx);
                            dbp.open();
                            long photoId = dbp.insertPhoto(localId, lat, lon, h);
                            dbp.updatePhoto(photoId, description);
                            String dir = ctx.getDir("photos", Context.MODE_PRIVATE).getAbsolutePath();
                            File f = new File(dir + "/" + photoId + ".jpg");
                            dbp.insertPath(photoId, f.getAbsolutePath());
                            dbp.close();
                            downloadPhoto(f, url);
                        }
                    }
                    if (tag.equals("video")) {
                        String url = xmlParser.getAttributeValue(null, "url");
                        if (url != null) {
                            String description = xmlParser.getAttributeValue(null, "description");
                            DataBaseVideo dbv = new DataBaseVideo(ctx);
                            dbv.open();
                            long videoId = dbv.insertVideo(localId, lat, lon, h);
                            dbv.updateVideo(videoId, description);
                            dbv.insertPath(videoId, Server.URL + url);
                            dbv.close();
                        }
                    }
                    if (tag.equals("place")) {
                        String name = xmlParser.getAttributeValue(null, "title");
                        if (name != null) {
                            String description = xmlParser.getAttributeValue(null, "description");
                            DataBasePlaceMarks dbp = new DataBasePlaceMarks(ctx);
                            dbp.open();
                            long placeId = dbp.insertPlacemark(localId, lat, lon, h);
                            dbp.updatePlacemark(placeId, name, description);
                            dbp.close();
                        }
                    }
                }
                parserEvent = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public long getLocalId() {
        return localId;
    }

    private void downloadPhoto(File photo, String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            Log.v(TAG, "Dowloading photo from " + Server.URL + url);
            HttpGet request = new HttpGet(Server.URL + url);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream serverPhoto = entity.getContent();
            photo.createNewFile();
            FileOutputStream photoStream = new FileOutputStream(photo);
            byte[] buf = new byte[1024];
            int len;
            while ((len = serverPhoto.read(buf)) > 0) {
                photoStream.write(buf, 0, len);
            }
            photoStream.flush();
            photoStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
