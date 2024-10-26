package squirrel.DeaddropDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Deze class beheert de deaddrop database: lezen en schrijven van deaddrop
 * informatie van en naar de database, en de interactie met de deaddrops.com web
 * site.
 * De eigenlijke database interface wordt verzorgt door DeaddropDBProvider,
 * een ContentProvider. Dit om crashes door conflicten te voorkomen.
 */
public class DeaddropDB {

    /**
	 * De huidige revisie van de database.
	 */
    public static final int DATABASE_VERSION = 21;

    /**
	 * De naam van de database.
	 */
    public static final String DATABASE_NAME = "deaddropsdb";

    /**
	 * De naam van de tabel met deaddrops.
	 */
    public static final String DEADDROPS_TABLE = "deaddrops";

    /**
	 * De naam van de tabel met blog informatie.
	 */
    public static final String BLOG_TABLE = "blog";

    /**
	 * De naam van de tabel met tweets.
	 */
    public static final String TWITTER_TABLE = "twitter";

    /**
	 * Geeft aan of we https kunnen gebruiken op deaddrops.com.
	 */
    protected static final boolean HTTPS = true;

    /**
	 * De https URL waar de deaddrop informatie opgehaald moet worden (JSON
	 * interface).
	 */
    private static final String DEADDROPS_SERVER_URL_HTTPS = "https://deaddrops.com/db/mobildrop.php";

    /**
	 * De URL waar de deaddrop informatie opgehaald moet worden (JSON
	 * interface).
	 */
    private static final String DEADDROPS_SERVER_URL = "http://deaddrops.com/db/mobildrop.php";

    /**
	 * URL pad naar de deaddrop foto's, https versie:
	 * IMAGE_URL/deaddropID/photo.jpg
	 */
    protected static final String IMAGE_URL_HTTPS = "https://deaddrops.com/db/images/deaddrops/";

    /**
	 * URL pad naar de deaddrop foto's: IMAGE_URL/deaddropID/photo.jpg
	 */
    protected static final String IMAGE_URL = "http://deaddrops.com/db/images/deaddrops/";

    /**
	 * URL pad naar de deaddrop foto's, https versie:
	 * IMAGE_URL/deaddropID/photo.jpg
	 */
    protected static final String IMAGE_URL_FS_HTTPS = "https://deaddrops.com/db/images/fullsize/";

    /**
	 * URL pad naar de deaddrop foto's: IMAGE_URL/deaddropID/photo.jpg
	 */
    protected static final String IMAGE_URL_FS = "http://deaddrops.com/db/images/fullsize/";

    /**
	 * URL naar de blog's RSS feed (https versie).
	 */
    private static final String BLOG_URL_HTTPS = "https://deaddrops.com/feed/";

    /**
	 * URL naar de blog's RSS feed.
	 */
    private static final String BLOG_URL = "http://deaddrops.com/feed/";

    /**
	 * De search URL van Twitter.
	 */
    private static final String TWITTER_URL = "https://api.twitter.com/1/statuses/user_timeline.json";

    /**
	 * Het Twitter account waarin we geinteresseerd zijn.
	 */
    private static final String TWITTER_ACCOUNT = "dead_drops";

    /**
	 * De deaddrop ID.
	 */
    public static final String KEY_ID = "_id";

    /**
	 * De naam van de deaddrop.
	 */
    protected static final String KEY_DEADDROP_NAME = "name";

    /**
	 * Datum dat deaddrop, tweet of blog item is aangemaakt.
	 */
    public static final String KEY_DATE = "created";

    /**
	 * Formaat waarin de datum/tijd in de database worden opgeslagen.
	 */
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
	 * Formaat waarin de datum in de database wordt opgeslagen.
	 */
    public static final String DATE_FORMAT_SHORT = "yyyy-MM-dd";

    /**
	 * Adres van de deaddrop.
	 */
    protected static final String KEY_DEADDROP_ADDRESS = "address";

    /**
	 * Breedtegraad van de deaddrop (DD.DDDDDD).
	 */
    protected static final String KEY_DEADDROP_LAT = "lat";

    /**
	 * Lengtegraad van de deaddrop (DD.DDDDDD).
	 */
    protected static final String KEY_DEADDROP_LON = "lon";

    /**
	 * Capaciteit van de deaddrop.
	 */
    protected static final String KEY_DEADDROP_SIZE = "size";

    /**
	 * Foto van de deaddrop: overzicht.
	 */
    protected static final String KEY_DEADDROP_PIC_FARSHOT = "farshot";

    /**
	 * Foto van de deaddrop: dichtbij.
	 */
    protected static final String KEY_DEADDROP_PIC_MIDSHOT = "midshot";

    /**
	 * Foto van de deaddrop: close-up.
	 */
    protected static final String KEY_DEADDROP_PIC_CLOSEUP = "closeup";

    /**
	 * JSON key voor de array met fotos.
	 */
    protected static final String KEY_DEADDROP_PICS = "pictures";

    /**
	 * Lijst met de foto keys.
	 */
    protected static final String[] DEADDROP_PICS = { KEY_DEADDROP_PIC_FARSHOT, KEY_DEADDROP_PIC_MIDSHOT, KEY_DEADDROP_PIC_CLOSEUP };

    /**
	 * Type deaddrop.
	 */
    protected static final String KEY_DEADDROP_TYPE = "droptype";

    /**
	 * Status van de deaddrop.
	 */
    protected static final String KEY_DEADDROP_STATUS = "status";

    /**
	 * Algemene informatie over de deaddrop.
	 */
    protected static final String KEY_DEADDROP_ABOUT = "about";

    /**
	 * Notities bij deze deaddrop.
	 */
    protected static final String KEY_DEADDROP_NOTES = "notes";

    /**
	 * Wanneer deze deaddrop is opgeslagen, of 0 als die alleen gecached is.
	 */
    protected static final String KEY_DEADDROP_STORED = "stored";

    /**
	 * De complete lijst met velden in de database.
	 */
    public static final String[] DEADDROP_KEYS = { KEY_ID, KEY_DEADDROP_NAME, KEY_DATE, KEY_DEADDROP_ADDRESS, KEY_DEADDROP_LAT, KEY_DEADDROP_LON, KEY_DEADDROP_SIZE, KEY_DEADDROP_PIC_FARSHOT, KEY_DEADDROP_PIC_MIDSHOT, KEY_DEADDROP_PIC_CLOSEUP, KEY_DEADDROP_TYPE, KEY_DEADDROP_STATUS, KEY_DEADDROP_ABOUT, KEY_DEADDROP_NOTES, KEY_DEADDROP_STORED };

    /**
	 * De bijbehorende SQLite types.
	 */
    public static final String[] DEADDROP_TYPES = { "INTEGER PRIMARY KEY", "TEXT", "TEXT", "TEXT", "REAL", "REAL", "TEXT", "TEXT", "TEXT", "TEXT", "INTEGER", "INTEGER", "TEXT", "TEXT", "INTEGER" };

    /**
	 * De titel van de blog entry.
	 */
    protected static final String KEY_BLOG_TITLE = "title";

    /**
	 * Auteur van de blog entry.
	 */
    protected static final String KEY_BLOG_AUTHOR = "author";

    /**
	 * Categorie van de blog entry.
	 */
    protected static final String KEY_BLOG_CATEGORY = "category";

    /**
	 * Samenvatting van de blog entry.
	 */
    protected static final String KEY_BLOG_SUMMARY = "summary";

    /**
	 * Complete tekst van de blog entry (in HTML formaat).
	 */
    protected static final String KEY_BLOG_CONTENT = "content";

    /**
	 * Complete lijst met velden van de Blog database.
	 */
    public static final String[] BLOG_KEYS = { KEY_ID, KEY_BLOG_TITLE, KEY_DATE, KEY_BLOG_AUTHOR, KEY_BLOG_CATEGORY, KEY_BLOG_SUMMARY, KEY_BLOG_CONTENT };

    /**
	 * Lijst met bijbehorende velden van de RSS feed.
	 */
    protected static final String[] RSS_KEYS = { "", "title", "pubDate", "dc:creator", "category", "description", "content:encoded" };

    /**
	 * Lijst met bijbehorende SQLite types.
	 */
    public static final String[] BLOG_TYPES = { "INTEGER PRIMARY KEY", "TEXT", "TEXT", "TEXT", "TEXT", "TEXT", "TEXT" };

    /**
	 * De datum en tijd van de tweet. Wed, 23 Mar 2011 13:25:43 +0000 format
	 * string: "E, d M y H:m:s Z"
	 */
    protected static final String KEY_TWEET_DATE = "created_at";

    /**
	 * De tweet tekst.
	 */
    protected static final String KEY_TWEET_TEXT = "text";

    /**
	 * Het tweet id (gegeven door Twitter).
	 */
    protected static final String KEY_TWEET_ID = "id_str";

    /**
	 * Complete lijst met velden van de twitter database.
	 */
    public static final String[] TWITTER_KEYS = { KEY_ID, KEY_TWEET_DATE, KEY_TWEET_TEXT, KEY_TWEET_ID };

    /**
	 * Lijst met bijbehorende SQLIte types.
	 */
    public static final String[] TWITTER_TYPES = { "INTEGER PRIMARY KEY", "TEXT", "TEXT", "TEXT" };

    /**
	 * Maximale aantal tweets dat we willen hebben.
	 */
    protected static final int MAX_TWEETS = 80;

    /**
	 * Geeft aan of er een blog update aan de gang is in een andere thread.
	 */
    private static boolean updatingBlog = false;

    /**
	 * Geeft aan of er een twitter update aan de gang is in een andere thread.
	 */
    private static boolean updatingTwitter;

    /**
	 * De naam van de SharedPreferences file waar de statistieken in worden
	 * opgeslagen.
	 */
    protected static final String STORED_STATS = "statistics";

    /**
	 * Statistiek: aantal deaddrops in de wereld.
	 */
    protected static final String KEY_WORLD_NUMBER = "worldNumber";

    /**
	 * Statistiek: totaal capaciteit in de wereld.
	 */
    protected static final String KEY_WORLD_SIZE = "worldSize";

    /**
	 * Statistiek: aantal deaddrops in het land.
	 */
    protected static final String KEY_COUNTRY_NUMBER = "countryNumber";

    /**
	 * Statistiek: totaal capaciteit in het land.
	 */
    protected static final String KEY_COUNTRY_SIZE = "countrySize";

    /**
	 * Statistiek: totaal deaddrops in de stad.
	 */
    protected static final String KEY_CITY_NUMBER = "cityNumber";

    /**
	 * Statistiek: totaal capaciteit in de stad.
	 */
    protected static final String KEY_CITY_SIZE = "citySize";

    /**
	 * Statistiek: totaal deaddrops in de buurt.
	 */
    protected static final String KEY_NEARBY_NUMBER = "nearbyNumber";

    /**
	 * Statistiek: totaal capaciteit in de buurt.
	 */
    protected static final String KEY_NEARBY_SIZE = "nearbySize";

    private final Context context;

    private static final String TAG = DeaddropDroid.TAG;

    /**
	 * De standaard constructor.
	 * 
	 * @param ctx
	 *            de context van de app.
	 */
    public DeaddropDB(final Context ctx) {
        context = ctx;
    }

    /**
	 * query interface.
	 * 
	 * @param table
	 * @param columns
	 * @param selection
	 * @param selectionArgs
	 * @param groupBy
	 * @param orderBy
	 * @return
	 */
    public Cursor query(final String table, final String[] columns, final String selection, final String[] selectionArgs, final String orderBy) {
        if (table.equals(DEADDROPS_TABLE)) return context.getContentResolver().query(DeaddropDBProvider.DEADDROPS_URI, columns, selection, selectionArgs, orderBy); else if (table.equals(BLOG_TABLE)) return context.getContentResolver().query(DeaddropDBProvider.BLOG_URI, columns, selection, selectionArgs, orderBy); else if (table.equals(TWITTER_TABLE)) return context.getContentResolver().query(DeaddropDBProvider.TWITTER_URI, columns, selection, selectionArgs, orderBy);
        return null;
    }

    /**
	 * insert interface.
	 * 
	 * @param table
	 * @param values
	 * @return
	 */
    public Uri insert(final String table, final ContentValues values) {
        if (table.equals(DEADDROPS_TABLE)) return context.getContentResolver().insert(DeaddropDBProvider.DEADDROPS_URI, values); else if (table.equals(BLOG_TABLE)) return context.getContentResolver().insert(DeaddropDBProvider.BLOG_URI, values); else if (table.equals(TWITTER_TABLE)) return context.getContentResolver().insert(DeaddropDBProvider.TWITTER_URI, values);
        return null;
    }

    /**
	 * update interface.
	 * 
	 * @param table
	 * @param values
	 * @param where
	 * @param selectionArgs
	 * @return
	 */
    public int update(final String table, final ContentValues values, final String where, final String[] selectionArgs) {
        if (table.equals(DEADDROPS_TABLE)) return context.getContentResolver().update(DeaddropDBProvider.DEADDROPS_URI, values, where, selectionArgs); else if (table.equals(BLOG_TABLE)) return context.getContentResolver().update(DeaddropDBProvider.BLOG_URI, values, where, selectionArgs); else if (table.equals(TWITTER_TABLE)) return context.getContentResolver().update(DeaddropDBProvider.TWITTER_URI, values, where, selectionArgs);
        return 0;
    }

    /**
	 * delete interface.
	 * 
	 * @param table
	 * @param where
	 * @param selectionArgs
	 * @return
	 */
    public int delete(final String table, final String where, final String[] selectionArgs) {
        if (table.equals(DEADDROPS_TABLE)) return context.getContentResolver().delete(DeaddropDBProvider.DEADDROPS_URI, where, selectionArgs); else if (table.equals(BLOG_TABLE)) return context.getContentResolver().delete(DeaddropDBProvider.BLOG_URI, where, selectionArgs); else if (table.equals(TWITTER_TABLE)) return context.getContentResolver().delete(DeaddropDBProvider.TWITTER_URI, where, selectionArgs);
        return 0;
    }

    /**
	 * Sla een deaddrop op in de database. Als de _id van de deaddrop bestaat,
	 * wordt deze overschreven. Anders wordt een nieuwe regel toegevoegd.
	 * 
	 * @param deaddrop
	 *            Een bundle met de deaddrop informatie.
	 * @return true bij succes.
	 */
    public boolean storeDeaddrop(final Bundle deaddrop) {
        final Date date = new Date();
        deaddrop.putString(KEY_DEADDROP_STORED, "" + date.getTime());
        cacheDeaddrop(deaddrop);
        boolean res = true;
        for (final String key : DEADDROP_PICS) {
            final String pic = deaddrop.getString(key);
            if (!pic.equals("")) {
                final String id = deaddrop.getString(KEY_ID);
                try {
                    if (DeaddropUtil.getImage(context, pic, id, false) == null) res = false;
                    if (DeaddropUtil.getImage(context, pic, id, true) == null) res = false;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    /**
	 * Schrijf de gegevens van de cache in de database.
	 * 
	 * @param deaddrop
	 *            bevat de gegevens van de deaddrop.
	 */
    private void cacheDeaddrop(final Bundle deaddrop) {
        final Cursor c = query(DEADDROPS_TABLE, new String[] { KEY_ID }, KEY_ID + "= ?", new String[] { deaddrop.getString(KEY_ID) }, null);
        final ContentValues values = new ContentValues();
        for (final String k : deaddrop.keySet()) values.put(k, deaddrop.getString(k));
        if (c.getCount() > 0) {
            values.remove(KEY_DEADDROP_NOTES);
            update(DEADDROPS_TABLE, values, KEY_ID + "= ?", new String[] { deaddrop.getString(KEY_ID) });
        } else {
            for (final String k : DEADDROP_KEYS) if (!values.containsKey(k)) values.put(k, "");
            insert(DEADDROPS_TABLE, values);
        }
        c.close();
    }

    /**
	 * Haal deaddrop id op. Is de deaddrop gecached of opgeslagen: haal de
	 * data uit de database. Anders, als we online zijn: haal de informatie 
	 * van de server, en cache deze in de database.
	 * 
	 * @param id
	 *            de deaddropID.
	 * @return een deaddropBundle met daarin een res, en de deaddrop indien
	 *         deze beschikbaar was.
	 * @throws SQLException
	 */
    public Bundle getDeaddrop(final int id) throws SQLException {
        Bundle deaddropBundle = new Bundle();
        final Bundle deaddrop = new Bundle();
        final Bundle res = new Bundle();
        final Cursor c = query(DEADDROPS_TABLE, DEADDROP_KEYS, KEY_ID + "= ?", new String[] { "" + id }, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            for (final String s : DEADDROP_KEYS) deaddrop.putString(s, c.getString(c.getColumnIndex(s)));
            deaddropBundle.putBundle("0", deaddrop);
            res.putString("0", "success");
            deaddropBundle.putBundle("res", res);
            c.close();
            return deaddropBundle;
        }
        c.close();
        deaddropBundle = fetchDeaddrop(id);
        if (deaddropBundle.getBundle("res").getString("0").equals("success")) cacheDeaddrop(deaddropBundle.getBundle("0"));
        return deaddropBundle;
    }

    /**
	 * Lees alle opgeslagen deaddrops uit de database.
	 * 
	 * @return een database cursor.
	 */
    public Cursor getAllStoredDeaddrops() {
        final String where = KEY_DEADDROP_STORED + "!= 0";
        return query(DEADDROPS_TABLE, null, where, null, null);
    }

    /**
	 * Vernieuw alle opgeslagen deaddrops.
	 * 
	 * @return true bij succes.
	 */
    public String refreshAll() {
        final Cursor c = query(DEADDROPS_TABLE, new String[] { KEY_ID }, KEY_DEADDROP_STORED + "> 0", null, null);
        String res = "none";
        if (c.getCount() > 0) {
            c.moveToFirst();
            final int[] ids = new int[c.getCount()];
            for (int i = 0; i < c.getCount(); i++) {
                ids[i] = c.getInt(0);
                c.moveToNext();
            }
            res = refreshDeaddrop(ids);
        }
        c.close();
        return res;
    }

    /**
	 * Vernieuw de informatie over de deaddrop. Foto's worden niet opnieuw
	 * opgehaald tenzij we ze niet hebben (bijv. bestandsnaam veranderd).
	 * 
	 * @param ids
	 *            een lijst met te verversen deaddrop ids.
	 * @return true bij succes.
	 */
    public String refreshDeaddrop(final int[] ids) {
        Bundle deaddropBundle = new Bundle();
        String id = new String();
        for (final int i : ids) id += i + ",";
        id = id.substring(0, id.length() - 1);
        deaddropBundle = fetchDeaddrop(id);
        final String res = deaddropBundle.getBundle("res").getString("0");
        deaddropBundle.remove("res");
        if (res.equals("success")) for (final String k : deaddropBundle.keySet()) storeDeaddrop(deaddropBundle.getBundle(k));
        return res;
    }

    /**
	 * Update de note in de database voor een specifieke deaddrop.
	 * 
	 * @param deaddropID
	 *            de deaddrop id.
	 * @param note
	 *            de note die opgeslagen moet worden.
	 */
    public void updateNote(final int deaddropID, final String note) {
        final ContentValues values = new ContentValues();
        values.put(KEY_DEADDROP_NOTES, note);
        update(DEADDROPS_TABLE, values, KEY_ID + "= ?", new String[] { "" + deaddropID });
    }

    /**
	 * Verwijder de deadddrop van de database. De deaddrop blijft gecached; 
	 * fotos en notes worden niet gewist.
	 * 
	 * @param deaddropID
	 */
    public void dropDeaddrop(final int id) {
        final ContentValues values = new ContentValues();
        values.put(KEY_DEADDROP_STORED, "0");
        update(DEADDROPS_TABLE, values, KEY_ID + "= ?", new String[] { "" + id });
    }

    /**
	 * Doet de eigenlijke GET request met de meegegeven nameValuePairs.
	 * 
	 * @param nameValuePairs
	 *            de request parameters.
	 * @return een JSON object als string, of null in geval van falen.
	 */
    public static String httpGetJson(final List<NameValuePair> nameValuePairs) {
        HttpClient httpclient = null;
        String data = "";
        URI uri = null;
        try {
            final String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
            if (HTTPS) {
                final SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
                final HttpParams params = new BasicHttpParams();
                final SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);
                httpclient = new DefaultHttpClient(mgr, params);
                uri = new URI(DEADDROPS_SERVER_URL_HTTPS + "?" + paramString);
            } else {
                httpclient = new DefaultHttpClient();
                uri = new URI(DEADDROPS_SERVER_URL + "?" + paramString);
            }
            final HttpGet request = new HttpGet();
            request.setURI(uri);
            final HttpResponse response = httpclient.execute(request);
            final BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) data += inputLine;
            in.close();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            return null;
        } catch (final ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }

    /**
	 * Haalt deaddrop id op van de server.
	 * 
	 * @param id
	 *            de deaddrop id.
	 * @return een deaddropBundle.
	 */
    private Bundle fetchDeaddrop(final int id) {
        return fetchDeaddrop("" + id);
    }

    /**
	 * Haalt deaddrop id op van de server. Met deze functie kunnen we ook
	 * meerdere deaddrops tegelijk ophalen door meerdere ids in de string te
	 * zetten: id = "20,35,58"
	 * 
	 * @param id
	 *            de deaddrop id (of comma-gescheiden ids).
	 * @return een deaddropBundle.
	 */
    private Bundle fetchDeaddrop(final String id) {
        final Bundle deaddropBundle = new Bundle();
        final Bundle res = new Bundle();
        if (!DeaddropUtil.isOnline(context)) {
            res.putString("0", "offline");
            deaddropBundle.putBundle("res", res);
            return deaddropBundle;
        }
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("q", "getfull"));
        nameValuePairs.add(new BasicNameValuePair("id", "" + id));
        final String data = httpGetJson(nameValuePairs);
        if (data == null) {
            res.putString("0", "failed");
            deaddropBundle.putBundle("res", res);
            return deaddropBundle;
        }
        return parseDeaddrops(data);
    }

    /**
	 * Haalt alle deaddrops op die binnen de bounding box (x1, x2) en (y1, y2)
	 * vallen.
	 * 
	 * @param x1
	 *            lengtegraad 1 in micro-graden.
	 * @param x2
	 *            lengtegraad 2 in micro-graden.
	 * @param y1
	 *            breedtegraad 1 in micro-graden.
	 * @param y2
	 *            breedtegraad 2 in micro-graden.
	 * @return een deaddropBundle.
	 */
    public Bundle fetchDeaddropRange(final int x1, final int x2, final int y1, final int y2) {
        final Bundle deaddropBundle = new Bundle();
        final Bundle res = new Bundle();
        final double x1d = x1 / 1e6;
        final double x2d = x2 / 1e6;
        final double y1d = y1 / 1e6;
        final double y2d = y2 / 1e6;
        if (DeaddropUtil.isOnline(context)) {
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("q", "getids"));
            nameValuePairs.add(new BasicNameValuePair("latlonbox", x1d + "," + y1d + "," + x2d + "," + y2d));
            nameValuePairs.add(new BasicNameValuePair("fields", "droptype,status"));
            nameValuePairs.add(new BasicNameValuePair("limit", Integer.toString(ShowMap.MAX_DROPS + 1)));
            final String data = httpGetJson(nameValuePairs);
            if (data == null) {
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            return parseDeaddrops(data);
        } else {
            res.putString("0", "offline");
            final String[] args = { "" + x1d, "" + x2d, "" + y1d, "" + y2d };
            final Cursor c = query(DEADDROPS_TABLE, null, KEY_DEADDROP_LAT + ">? and " + KEY_DEADDROP_LAT + "<? and " + KEY_DEADDROP_LON + ">? and " + KEY_DEADDROP_LON + "<?", args, null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                for (int i = 0; i < c.getCount(); i++) {
                    final String[] columns = c.getColumnNames();
                    final Bundle deaddrop = new Bundle();
                    for (final String s : columns) deaddrop.putString(s, c.getString(c.getColumnIndex(s)));
                    deaddropBundle.putBundle("" + i, deaddrop);
                }
            } else res.putString("1", "none");
            c.close();
        }
        if (!deaddropBundle.containsKey("res")) deaddropBundle.putBundle("res", res);
        return deaddropBundle;
    }

    /**
	 * Importeer deaddrops uit een JSON-type string.
	 * 
	 * Deze functie controleert ook of de deaddrop "broken" gemarkeerd
	 * is, en verwijdert deze drops als de preference key KEY_SHOW_BROKEN
	 * is gezet.
	 * 
	 * @param deaddropData
	 *            het JSON object.
	 * @return een deaddropBundle.
	 */
    private Bundle parseDeaddrops(final String deaddropData) {
        final Bundle deaddropBundle = new Bundle();
        final Bundle res = new Bundle();
        JSONObject json = null;
        try {
            json = new JSONObject(deaddropData);
            if (json == null) {
                Log.v(TAG, "json null!");
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            if (json.length() == 0) {
                Log.v(TAG, "geen keys!");
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            if (json.has("failed")) {
                Log.v(TAG, "Server zegt dat de query ongeldig is.");
                if (json.has("reason")) Log.v(TAG, "Reden: " + json.getString("reason"));
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            if (json.has("count")) if (json.getInt("count") == 0) {
                res.putString("0", "none");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            if (json.has("warnings")) {
                final JSONArray warnings = json.getJSONArray("warnings");
                for (int i = 0; i < warnings.length(); i++) Log.w(TAG, "JSON server said: " + warnings.getString(i));
            }
            if (json.has("data") && json.has("count")) {
                Boolean showDead = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.KEY_SHOW_BROKEN, false);
                final JSONArray jsonData = json.getJSONArray("data");
                for (int i = 0; i < jsonData.length(); i++) {
                    final JSONObject data = jsonData.getJSONObject(i);
                    final Bundle deaddrop = new Bundle();
                    if (!showDead && data.getInt(KEY_DEADDROP_STATUS) == 0) continue;
                    for (final String key : DEADDROP_KEYS) if (key.equals(KEY_ID)) deaddrop.putString(KEY_ID, data.getString("id")); else if (data.has(key) && !key.equals(KEY_DEADDROP_ADDRESS) && !Arrays.asList(DEADDROP_PICS).contains(key)) if (key.equals(KEY_DATE)) deaddrop.putString(key, data.getString(key).split(" ")[0]); else deaddrop.putString(key, data.getString(key));
                    if (data.has("address")) {
                        String address = data.getString("address");
                        final String zip = data.getString("zip");
                        final String city = data.getString("city");
                        final String state = data.getString("state");
                        final String country = data.getString("country");
                        if (state.equals(city)) address += "\n" + city + "\n" + zip + " " + country; else address += "\n" + city + "\n" + state + " " + zip + " " + country;
                        deaddrop.putString(KEY_DEADDROP_ADDRESS, address);
                    }
                    if (data.has("pictures")) {
                        final JSONObject pictures = data.getJSONObject(KEY_DEADDROP_PICS);
                        for (final String key : DEADDROP_PICS) deaddrop.putString(key, pictures.getString(key));
                    }
                    deaddrop.putString(KEY_DEADDROP_STORED, "0");
                    for (final String key : DEADDROP_KEYS) if (!deaddrop.containsKey(key)) deaddrop.putString(key, "");
                    deaddropBundle.putBundle("" + i, deaddrop);
                }
            } else {
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
        } catch (final JSONException e) {
            e.printStackTrace();
            Log.v(TAG, "JSON decodering is mislukt.");
            res.putString("0", "failed");
            deaddropBundle.putBundle("res", res);
            return deaddropBundle;
        }
        res.putString("0", "success");
        deaddropBundle.putBundle("res", res);
        return deaddropBundle;
    }

    /**
	 * Vraag de dichtstbijzijnde deaddrops op. Als die niet van de server
	 * beschikbaar zijn, probeer ze uit de eigen database te halen.
	 * 
	 * @param lat
	 *            de breedtegraad in micro-graden.
	 * @param lng
	 *            de lengtegraad in micro-graden.
	 * @return een deaddropbundle.
	 */
    public Bundle fetchNearest(final int lat, final int lng, final int distance) {
        Bundle deaddropBundle = new Bundle();
        final Bundle res = new Bundle();
        if (DeaddropUtil.isOnline(context)) {
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("q", "getshort"));
            nameValuePairs.add(new BasicNameValuePair("lat", "" + (lat / 1e6)));
            nameValuePairs.add(new BasicNameValuePair("lon", "" + (lng / 1e6)));
            nameValuePairs.add(new BasicNameValuePair("limit", "20"));
            nameValuePairs.add(new BasicNameValuePair("maxdistance", "" + distance));
            final String data = httpGetJson(nameValuePairs);
            if (data == null) {
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            deaddropBundle = parseDeaddrops(data);
        } else {
            res.putString("0", "offline");
            final double lat1 = lat / 1e6;
            final double lng1 = lng / 1e6;
            Cursor cursor = query(DEADDROPS_TABLE, new String[] { KEY_ID, KEY_DEADDROP_LAT, KEY_DEADDROP_LON }, null, null, null);
            final double[] dist = new double[cursor.getCount()];
            final HashMap<Double, String> map = new HashMap<Double, String>();
            cursor.moveToFirst();
            String id;
            for (int i = 0; i < cursor.getCount(); i++) {
                final double lat2 = cursor.getFloat(cursor.getColumnIndex(KEY_DEADDROP_LAT));
                final double lng2 = cursor.getFloat(cursor.getColumnIndex(KEY_DEADDROP_LON));
                id = cursor.getString(cursor.getColumnIndex(KEY_ID));
                final double d = DeaddropUtil.distBetween(lat1, lng1, lat2, lng2);
                dist[i] = d;
                map.put(d, id);
                cursor.moveToNext();
            }
            Arrays.sort(dist);
            int lim = 0;
            if (cursor.getCount() < 20) lim = cursor.getCount(); else lim = 20;
            if (lim > 0) for (int i = 0; i < lim; i++) {
                id = map.get(dist[i]);
                cursor = query(DEADDROPS_TABLE, null, KEY_ID + "= ?", new String[] { id }, null);
                cursor.moveToFirst();
                final Bundle deaddrop = new Bundle();
                for (final String s : DEADDROP_KEYS) deaddrop.putString(s, cursor.getString(cursor.getColumnIndex(s)));
                deaddropBundle.putBundle("" + i, deaddrop);
            } else res.putString("1", "none");
        }
        if (!deaddropBundle.containsKey("res")) deaddropBundle.putBundle("res", res);
        return deaddropBundle;
    }

    /**
	 * Verwijder alle gecachede, niet-opgeslagen deaddrops uit de database, 
	 * en verwijder alle foto's zonder bijbehorende deaddrop.
	 */
    protected void purgeCache() {
        delete(DEADDROPS_TABLE, KEY_DEADDROP_STORED + "=0", null);
        final Cursor cursor = query(DEADDROPS_TABLE, new String[] { KEY_ID }, null, null, null);
        cursor.moveToFirst();
        final ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < cursor.getCount(); i++) {
            ids.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        final File directory = DeaddropUtil.getExternalFilesDir(null);
        File f = null;
        final String[] fileList = directory.list();
        for (final String p : fileList) if (!ids.contains(p)) {
            f = new File(directory, p + "/fs/");
            if (f.isDirectory()) {
                final String[] ff = f.list();
                for (final String s : ff) new File(directory, p + "/fs/" + s).delete();
                f.delete();
            }
            f = new File(directory, p);
            if (f.isDirectory()) {
                final String[] ff = f.list();
                for (final String s : ff) new File(directory, p + "/" + s).delete();
                f.delete();
            }
        }
    }

    /**
	 * Haal alle deaddrops op die de string "name" in de naam hebben.
	 * 
	 * @param name
	 * @return
	 */
    protected Bundle getDeaddropByName(final String name) {
        final Bundle deaddropBundle = new Bundle();
        final Bundle res = new Bundle();
        if (DeaddropUtil.isOnline(context)) {
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("q", "getshort"));
            nameValuePairs.add(new BasicNameValuePair("name", name));
            nameValuePairs.add(new BasicNameValuePair("limit", "31"));
            final String data = httpGetJson(nameValuePairs);
            if (data == null) {
                res.putString("0", "failed");
                deaddropBundle.putBundle("res", res);
                return deaddropBundle;
            }
            return parseDeaddrops(data);
        }
        res.putString("0", "offline");
        deaddropBundle.putBundle("res", res);
        return deaddropBundle;
    }

    /**
	 * Haal de nieuwste tweets van dead_drops op.
	 * 
	 * @return
	 */
    protected String updateTwitter() {
        if (updatingTwitter) return null;
        updatingTwitter = true;
        String highestId = null;
        final Cursor cursor = query(TWITTER_TABLE, new String[] { KEY_TWEET_ID }, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            highestId = cursor.getString(cursor.getColumnIndex(KEY_TWEET_ID));
        }
        cursor.close();
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("screen_name", TWITTER_ACCOUNT));
        nameValuePairs.add(new BasicNameValuePair("count", "" + MAX_TWEETS));
        if (highestId != null) nameValuePairs.add(new BasicNameValuePair("since_id", highestId));
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        final HttpParams params = new BasicHttpParams();
        final SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);
        final HttpClient httpclient = new DefaultHttpClient(mgr, params);
        final HttpGet request = new HttpGet();
        final String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        String data = "";
        try {
            final URI uri = new URI(TWITTER_URL + "?" + paramString);
            request.setURI(uri);
            final HttpResponse response = httpclient.execute(request);
            final BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) data += inputLine;
            in.close();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            updatingTwitter = false;
            return "failed";
        } catch (final ClientProtocolException e) {
            e.printStackTrace();
            updatingTwitter = false;
            return "failed";
        } catch (final IOException e) {
            e.printStackTrace();
            updatingTwitter = false;
            return "failed";
        }
        try {
            final JSONArray tweets = new JSONArray(data);
            if (tweets == null) {
                updatingTwitter = false;
                return "failed";
            }
            if (tweets.length() == 0) {
                updatingTwitter = false;
                return "none";
            }
            final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
            final SimpleDateFormat parser = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            for (int i = 0; i < tweets.length(); i++) {
                final JSONObject tweet = tweets.getJSONObject(i);
                final ContentValues values = new ContentValues();
                Log.v(TAG, "Datum van tweet: " + tweet.getString(KEY_TWEET_DATE));
                values.put(KEY_TWEET_DATE, formatter.format(parser.parse(tweet.getString(KEY_TWEET_DATE))));
                values.put(KEY_TWEET_TEXT, tweet.getString(KEY_TWEET_TEXT));
                values.put(KEY_TWEET_ID, tweet.getString(KEY_TWEET_ID));
                insert(TWITTER_TABLE, values);
            }
        } catch (final JSONException e) {
            Log.v(TAG, "JSON decodering is mislukt.");
            e.printStackTrace();
            updatingTwitter = false;
            return "failed";
        } catch (final ParseException e) {
            Log.v(TAG, "Datum decodering is mislukt.");
            e.printStackTrace();
            updatingTwitter = false;
            return "failed";
        }
        purgeTweets();
        updatingTwitter = false;
        return "success";
    }

    /**
	 * Haalt alle tweets op uit de database, in aflopende volgorde van id.
	 * 
	 * @return een database cursor.
	 */
    protected Cursor getTweets() {
        return query(TWITTER_TABLE, TWITTER_KEYS, null, null, KEY_TWEET_ID + " DESC");
    }

    /**
	 * Verwijder de oudste tweets uit de database als het totaal groter is dan
	 * 50.
	 */
    protected void purgeTweets() {
        final Cursor c = getTweets();
        if (c.getCount() > MAX_TWEETS) {
            c.moveToPosition(MAX_TWEETS - 1);
            final String lastID = c.getString(c.getColumnIndex(KEY_TWEET_ID));
            delete(TWITTER_TABLE, KEY_TWEET_ID + " < ?", new String[] { lastID });
        }
        c.close();
    }

    /**
	 * Update de blog entries.
	 */
    protected String updateBlog() {
        if (updatingBlog) return null;
        updatingBlog = true;
        HttpClient httpclient = null;
        URI uri = null;
        InputStream rssData = null;
        try {
            if (HTTPS) {
                final SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
                final HttpParams params = new BasicHttpParams();
                final SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);
                httpclient = new DefaultHttpClient(mgr, params);
                uri = new URI(BLOG_URL_HTTPS);
            } else {
                httpclient = new DefaultHttpClient();
                uri = new URI(BLOG_URL);
            }
            final HttpGet request = new HttpGet();
            request.setURI(uri);
            final HttpResponse response = httpclient.execute(request);
            rssData = response.getEntity().getContent();
        } catch (final IOException e) {
            e.printStackTrace();
            updatingBlog = false;
            return "failed";
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            updatingBlog = false;
            return "failed";
        }
        if (rssData == null) {
            updatingBlog = false;
            return "failed";
        }
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbdr = null;
        try {
            dbdr = dbf.newDocumentBuilder();
        } catch (final ParserConfigurationException e1) {
            e1.printStackTrace();
            updatingBlog = false;
            return "failed";
        }
        Document doc = null;
        try {
            doc = dbdr.parse(rssData);
        } catch (final SAXException e1) {
            e1.printStackTrace();
            updatingBlog = false;
            return "failed";
        } catch (final IOException e1) {
            e1.printStackTrace();
            updatingBlog = false;
            return "failed";
        }
        final Node rssNode = doc.getElementsByTagName("rss").item(0);
        final NodeList channelNodeList = rssNode.getChildNodes();
        Node channelNode = null;
        for (int i = 0; i < channelNodeList.getLength(); i++) if (channelNodeList.item(i).getNodeName().equalsIgnoreCase("channel")) channelNode = channelNodeList.item(i);
        if (channelNode == null) {
            updatingBlog = false;
            return "failed";
        }
        delete(BLOG_TABLE, null, null);
        String k = "";
        final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        final SimpleDateFormat parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        final NodeList itemNodeList = channelNode.getChildNodes();
        for (int i = 0; i < itemNodeList.getLength(); i++) {
            final Node itemNode = itemNodeList.item(i);
            if (itemNode.getNodeType() == Node.ELEMENT_NODE && itemNode.getNodeName().equalsIgnoreCase("item")) {
                final ContentValues item = new ContentValues();
                final Element itemElement = (Element) itemNode;
                for (int j = 1; j < RSS_KEYS.length; j++) {
                    try {
                        final NodeList keyNodeList = itemElement.getElementsByTagName(DeaddropDB.RSS_KEYS[j]);
                        final Element fieldElement = (Element) keyNodeList.item(0);
                        final NodeList value = fieldElement.getChildNodes();
                        k = ((Node) value.item(0)).getNodeValue();
                        if (BLOG_KEYS[j] == KEY_DATE) k = formatter.format(parser.parse(k));
                        item.put(BLOG_KEYS[j], k);
                    } catch (final NullPointerException e) {
                        k = "";
                    } catch (final ParseException e) {
                        Log.v(TAG, "Datum verwerking mislukt! Datum: " + k);
                        e.printStackTrace();
                    }
                }
                for (final String key : BLOG_KEYS) if (!item.containsKey(key) && !key.equals(KEY_ID)) item.put(key, "");
                insert(BLOG_TABLE, item);
            }
        }
        updatingBlog = false;
        return "success";
    }

    /**
	 * Alle blog items (ID, author, category, date, title, summary), gesorteerd
	 * volgens aflopende datum.
	 * 
	 * @return een database cursor.
	 */
    protected Cursor getBlogItems() {
        final String[] columns = new String[] { KEY_ID, KEY_BLOG_AUTHOR, KEY_BLOG_CATEGORY, KEY_DATE, KEY_BLOG_TITLE, KEY_BLOG_SUMMARY };
        return query(BLOG_TABLE, columns, null, null, KEY_DATE + " DESC");
    }
}
