package org.tunesremote.daap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.Log;

public class RequestHelper {

    public static final String TAG = RequestHelper.class.toString();

    public static byte[] requestSearch(Session session, String search, int start, int end) throws Exception {
        String encodedSearch = Library.escapeUrlString(search);
        return request(String.format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,daap.songartist,daap.songalbum&type=music&include-sort-headers=1&query=(('com.apple.itunes.mediakind:1','com.apple.itunes.mediakind:4','com.apple.itunes.mediakind:8')+('dmap.itemname:*%s*','daap.songartist:*%s*','daap.songalbum:*%s*'))&sort=name&index=%d-%d", session.getRequestBase(), session.databaseId, session.musicId, session.sessionId, encodedSearch, encodedSearch, encodedSearch, start, end), false);
    }

    public static byte[] requestTracks(Session session, String albumid) throws Exception {
        return request(String.format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist,daap.songalbum,daap.songalbum,daap.songtime,daap.songtracknumber&type=music&sort=album&query='daap.songalbumid:%s'", session.getRequestBase(), session.databaseId, session.musicId, session.sessionId, albumid), false);
    }

    public static byte[] requestAlbums(Session session, int start, int end) throws Exception {
        return request(String.format("%s/databases/%d/containers/%d/items?session-id=%s&meta=dmap.itemname,dmap.itemid,dmap.persistentid,daap.songartist&type=music&group-type=albums&sort=artist&include-sort-headers=1&index=%d-%d", session.getRequestBase(), session.databaseId, session.musicId, session.sessionId, start, end), false);
    }

    public static byte[] requestPlaylists(Session session) throws Exception {
        return request(String.format("%s/databases/%d/containers?session-id=%s&meta=dmap.itemname,dmap.itemcount,dmap.itemid,dmap.persistentid,daap.baseplaylist,com.apple.itunes.special-playlist,com.apple.itunes.smart-playlist,com.apple.itunes.saved-genius,dmap.parentcontainerid,dmap.editcommandssupported", session.getRequestBase(), session.databaseId, session.musicId, session.sessionId), false);
    }

    public static Response requestParsed(String url, boolean keepalive) throws Exception {
        Log.d(TAG, url);
        return ResponseParser.performParse(request(url, keepalive));
    }

    public static void attemptRequest(String url) {
        try {
            request(url, false);
        } catch (Exception e) {
            Log.w(TAG, "attemptRequest:" + e.getMessage());
        }
    }

    /**
    * Performs the HTTP request and gathers the response from the server. The
    * gzip and deflate headers are sent in case the server can respond with
    * compressed answers saving network bandwidth and speeding up responses.
    * <p>
    * @param remoteUrl the HTTP URL to connect to
    * @param keepalive true if keepalive false if not
    * @return a byte array containing the HTTPResponse
    * @throws Exception if any error occurs
    */
    public static byte[] request(String remoteUrl, boolean keepalive) throws Exception {
        Log.d(TAG, String.format("started request(remote=%s)", remoteUrl));
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        byte[] buffer = new byte[1024];
        URL url = new URL(remoteUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Viewer-Only-Client", "1");
        connection.setRequestProperty("Client-Daap-Version", "3.10");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        if (!keepalive) {
            connection.setConnectTimeout(1200000);
            connection.setReadTimeout(1200000);
        } else {
            connection.setReadTimeout(0);
        }
        connection.connect();
        if (connection.getResponseCode() >= HttpURLConnection.HTTP_UNAUTHORIZED) throw new RequestException("HTTP Error Response Code: " + connection.getResponseCode(), connection.getResponseCode());
        String encoding = connection.getContentEncoding();
        InputStream inputStream = null;
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(connection.getInputStream());
        } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
            inputStream = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
        } else {
            inputStream = connection.getInputStream();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return os.toByteArray();
    }

    public static Bitmap requestThumbnail(Session session, int itemid) throws Exception {
        return requestThumbnail(session, itemid, "");
    }

    public static Bitmap requestThumbnail(Session session, int itemid, String type) throws Exception {
        try {
            byte[] raw = request(String.format("%s/databases/%d/items/%d/extra_data/artwork?session-id=%s&mw=55&mh=55%s", session.getRequestBase(), session.databaseId, itemid, session.sessionId, type), false);
            return BitmapFactory.decodeByteArray(raw, 0, raw.length);
        } catch (RequestException e) {
            int code = e.getResponseCode();
            if (code == 404 || code == 500) {
                return null;
            } else {
                throw e;
            }
        }
    }

    public static Bitmap requestBitmap(String remote) throws Exception {
        try {
            byte[] raw = request(remote, false);
            return BitmapFactory.decodeByteArray(raw, 0, raw.length);
        } catch (RequestException e) {
            int code = e.getResponseCode();
            if (code == 404 || code == 500) {
                return null;
            } else {
                throw e;
            }
        }
    }
}
