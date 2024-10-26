package com.etracks.dades;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.util.Log;

public class ServerListRoutes {

    private static String TAG = "ServerListRoutes";

    private static String URL = Server.URL + "routes.xml";

    private ArrayList<String> names;

    private ArrayList<Long> ids;

    public ServerListRoutes() {
        names = new ArrayList<String>();
        ids = new ArrayList<Long>();
    }

    public void downloadList() throws ClientProtocolException, IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(URL);
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        XmlPullParser xmlParser;
        try {
            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
            xmlParser = parserCreator.newPullParser();
            xmlParser.setInput(entity.getContent(), null);
            int parserEvent = xmlParser.getEventType();
            while (parserEvent != XmlPullParser.END_DOCUMENT) {
                if (parserEvent == XmlPullParser.START_TAG) {
                    String tag = xmlParser.getName();
                    if (tag.equals("route")) {
                        String name = xmlParser.getAttributeValue(null, "title");
                        String id = xmlParser.getAttributeValue(null, "id");
                        names.add(name);
                        ids.add(Long.valueOf(id));
                    }
                }
                parserEvent = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
	 * @return the names
	 */
    public ArrayList<String> getNames() {
        return names;
    }

    /**
	 * @return the ids
	 */
    public ArrayList<Long> getIds() {
        return ids;
    }
}
