package com.etracks.dades;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ListIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.etracks.R;
import com.etracks.domini.CtrlListRoutes;
import com.etracks.domini.CtrlUser;
import com.etracks.domini.Route;
import com.etracks.domini.Video;

/**
 * This class handles all the connections to upload a route (the complete route).
 * 
 * @author Albert
 * 
 */
public class ServerPostRoute extends AsyncTask<Void, Void, Integer> {

    /**
	 * Tag for the log.
	 */
    private static String TAG = "ServerPostRoute";

    /**
	 * Execution context.
	 */
    private Context ctx;

    /**
	 * Local identifier of the route.
	 */
    private long routeId;

    /**
	 * Server identifier of the route.
	 */
    private long serverId;

    /**
	 * URL to post routes.
	 */
    private static String URL = Server.URL + "routes.xml";

    /**
	 * List routes controller
	 */
    private CtrlListRoutes ctrlLR;

    /**
	 * Constructor of the class.
	 * @param c Execution context.
	 * @param ctrl List routes controller
	 * @param id Local identifier of the route.
	 */
    public ServerPostRoute(Context c, CtrlListRoutes ctrl, long id) {
        ctx = c;
        routeId = id;
        ctrlLR = ctrl;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Integer error = null;
        try {
            error = uploadRoute();
        } catch (IOException e) {
            Log.e(TAG, "input/output problem", e);
        }
        return error;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != null && result == HttpStatus.SC_UNAUTHORIZED) {
            ctrlLR.error("Error d'autentificacio");
        } else if (result == null || result != HttpStatus.SC_OK) {
            ctrlLR.error("Error de connexió");
        }
    }

    private Integer uploadRoute() throws IOException {
        CtrlMemory cm = new CtrlMemory(ctx);
        Route r = cm.getFullRoute(routeId);
        String distance = Double.toString(r.getDistance());
        String desnivell = Double.toString(r.getDesnivell());
        String type = r.getType();
        CtrlUser cu = new CtrlUser(ctx);
        String user = cu.getUser();
        String pword = cu.getPassword();
        KmzAdapter k = new KmzAdapter(ctx);
        File kmz = k.getTmpKmz(r);
        HttpClient httpClient = new DefaultHttpClient();
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            Log.e(TAG, "error creating keystore", e);
        }
        InputStream instream = ctx.getResources().openRawResource(R.raw.etracks_keystore);
        Log.d(TAG, "loading keystore...");
        try {
            trustStore.load(instream, "pxc4l0h4".toCharArray());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "keystore load: no such algorithm", e);
        } catch (CertificateException e) {
            Log.e(TAG, "keystore load: certificate problem", e);
        } catch (IOException e) {
            Log.e(TAG, "keystore load: input/output error", e);
        } finally {
            instream.close();
        }
        Log.d(TAG, "open SSL socket...");
        SSLSocketFactory socketFactory = null;
        try {
            socketFactory = new SSLSocketFactory(trustStore);
        } catch (KeyManagementException e) {
            Log.e(TAG, "KeyManagementException", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException", e);
        } catch (KeyStoreException e) {
            Log.e(TAG, "KeyStoreException", e);
        } catch (UnrecoverableKeyException e) {
            Log.e(TAG, "UnrecoverableKeyException", e);
        }
        Scheme sch = new Scheme("https", socketFactory, 443);
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
        HttpPost request = new HttpPost(URL);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("data[User][username]", new StringBody(user));
        entity.addPart("data[User][password]", new StringBody(pword));
        entity.addPart("data[Route][type]", new StringBody(type));
        entity.addPart("data[Route][ramp]", new StringBody(desnivell));
        entity.addPart("data[Route][distance]", new StringBody(distance));
        entity.addPart("data[Route][file_up]", new FileBody(kmz));
        ListIterator<Video> iter = cm.getAllVideos(routeId).listIterator();
        int i = 0;
        while (iter.hasNext()) {
            Video v = iter.next();
            File fv = new File(v.getPath());
            entity.addPart("data[Video][" + i + "][file]", new FileBody(fv));
            entity.addPart("data[Video][" + i + "][description]", new StringBody(v.getDesc()));
            entity.addPart("data[Video][" + i + "][point][long]", new StringBody(v.getLon() + ""));
            entity.addPart("data[Video][" + i + "][point][latitude]", new StringBody(v.getLat() + ""));
            i++;
        }
        request.setEntity(entity);
        Log.i(TAG, "Executing HTTP POST request to upload route");
        HttpResponse response = httpClient.execute(request);
        Integer status = response.getStatusLine().getStatusCode();
        Log.d(TAG, "HTTP POST request executed, status return code: " + status);
        if (status == HttpStatus.SC_OK) {
            Log.d(TAG, "Route uploaded without errors, now save it to DB");
            serverId = getRouteId(response.getEntity());
            DataBaseRoutes db = new DataBaseRoutes(ctx);
            db.open();
            db.setServerId(routeId, serverId);
            db.close();
        }
        k.deleteTmpFile();
        return status;
    }

    private long getRouteId(HttpEntity rEntity) {
        XmlPullParser xmlParser = null;
        long id = -1;
        XmlPullParserFactory parserCreator;
        try {
            parserCreator = XmlPullParserFactory.newInstance();
            xmlParser = parserCreator.newPullParser();
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Initializing XmlPullParser", e);
        }
        try {
            xmlParser.setInput(rEntity.getContent(), null);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Initializing XmlPullParser", e);
        } catch (IOException e) {
            Log.e(TAG, "Reading XML response input", e);
        }
        Log.v(TAG, xmlParser.toString());
        try {
            int parserEvent = xmlParser.getEventType();
            while (parserEvent != XmlPullParser.END_DOCUMENT && id == -1) {
                if (parserEvent == XmlPullParser.START_TAG) {
                    String tag = xmlParser.getName();
                    if (tag.equals("route")) {
                        String idS = xmlParser.getAttributeValue(null, "id");
                        id = Long.valueOf(idS);
                    }
                }
                parserEvent = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Parsing XML response", e);
        } catch (IOException e) {
            Log.e(TAG, "Reading next XML tag", e);
        }
        return id;
    }
}
