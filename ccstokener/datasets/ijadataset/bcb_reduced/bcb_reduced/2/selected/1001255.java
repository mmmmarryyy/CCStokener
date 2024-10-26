package com.FindHotel;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Xml;

/**
 * questa classe si occupa di interfacciarsi con la rete per recuperare la lista di alberghi
 * da visualizzare e le immagini da inserire nel pannello trasparente
 * 
 * @author pawaman
 *
 */
public class NetInterface {

    private final String TAG = this.getClass().getSimpleName();

    private Vector mHotels;

    private static SchemeRegistry supportedSchemes;

    private static HttpParams defaultParameters = null;

    private final Random rand = new Random();

    private HttpClient client;

    /**
 * dato il nome di una città, si connette al server del sistema hotel per recuperare il file xml
 * contenente la lista di tutti gli alberghi presenti in quella città
 * 	
 * @param city la città richiesta
 * @return un vettore di oggetti Hotel presenti nella città
 */
    public Vector getHotels(String city) {
        mHotels = new Vector();
        HttpGet method;
        String xml;
        HttpResponse resp = null;
        double i = 0;
        try {
            setup();
            client = createHttpClient();
            HttpRequest req = createRequest();
            String url = "http://labos.diee.unica.it/hotel/Availability/xml/list.htm?city=" + city + "&locale=it";
            HttpGet httpget = new HttpGet(url);
            HttpEntity entity = null;
            try {
                HttpResponse rsp = client.execute(httpget);
                entity = rsp.getEntity();
                xml = EntityUtils.toString(entity);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new StringReader(xml));
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                    } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    } else if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("structure")) {
                            parser.nextTag();
                            parser.nextText();
                            String id = parser.getText();
                            parser.nextTag();
                            parser.nextTag();
                            parser.nextText();
                            String name = parser.getText();
                            parser.nextTag();
                            parser.nextTag();
                            parser.nextText();
                            String phone = parser.getText();
                            parser.nextTag();
                            parser.nextTag();
                            parser.nextText();
                            double lat = Double.parseDouble(parser.getText());
                            parser.nextTag();
                            parser.nextTag();
                            parser.nextText();
                            double lon = Double.parseDouble(parser.getText());
                            parser.nextTag();
                            parser.nextTag();
                            parser.nextText();
                            String desc = parser.getText();
                            lat = 39.20536000 + (((float) rand.nextInt(1000)) / 50000);
                            lon = 9.13206700 + (((float) rand.nextInt(1000)) / 50000);
                            Hotel hotel = new Hotel(id, name, phone, lat, lon, desc);
                            mHotels.add(hotel);
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                    } else if (eventType == XmlPullParser.TEXT) {
                    }
                    eventType = parser.next();
                }
            } finally {
                if (entity != null) entity.consumeContent();
            }
        } catch (Exception e) {
        }
        return mHotels;
    }

    /**
 * restituisce l'immagine dell'albergo corrispondente ad un dato id
 * 	
 * @param id	l'id dell'albergo nel database
 * @return		l'immagine associata
 */
    public BitmapDrawable getHotelImage(String id) {
        BitmapDrawable image = null;
        String url = "http://labos.diee.unica.it/hotel/Image/view.htm?type=hotel&id=" + id;
        HttpGet httpget = new HttpGet(url);
        try {
            HttpResponse rsp = client.execute(httpget);
            InputStream is = rsp.getEntity().getContent();
            image = new BitmapDrawable(is);
        } catch (Exception e) {
        }
        return image;
    }

    private static final HttpClient createHttpClient() {
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(getParams(), supportedSchemes);
        DefaultHttpClient dhc = new DefaultHttpClient(ccm, getParams());
        return dhc;
    }

    /**
	 * Performs general setup. This should be called only once.
	 */
    private static final void setup() {
        supportedSchemes = new SchemeRegistry();
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        defaultParameters = params;
    }

    private static final HttpParams getParams() {
        return defaultParameters;
    }

    /**
	 * Creates a request to execute in this example.
	 * 
	 * @return a request without an entity
	 */
    private static final HttpRequest createRequest() {
        HttpRequest req = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
        return req;
    }

    public boolean book(String id, String in, String out, String name, String surname) {
        String bookUrl = "http://labos.diee.unica.it/hotel/Sms/create.htm";
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy");
        Date dateIn = df.parse(in, new ParsePosition(0));
        Date dateOut = df.parse(out, new ParsePosition(0));
        in = in.replace("/", ".");
        int days = (int) ((dateOut.getTime() - dateIn.getTime()) / (1000 * 24 * 60 * 60));
        String parameters = "11796 book owner" + id + " " + "password 204 " + in + " " + days + " " + name + " " + surname;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("TextMessage", parameters));
        client = createHttpClient();
        HttpPost httppost = new HttpPost(bookUrl);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse rsp = client.execute(httppost);
            int result = rsp.getStatusLine().getStatusCode();
            Log.d(TAG, "I'm booking @: " + id + "; result code is: " + result);
            if (result == 200) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
