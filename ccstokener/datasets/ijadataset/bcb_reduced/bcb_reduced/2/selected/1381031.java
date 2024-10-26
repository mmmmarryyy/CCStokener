package com.overflow.moneydroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.R;
import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.util.Xml;

public class ExchangeManager {

    private static final String DEBUG_TAG = "ExchangeManager";

    public static ArrayList<MoneyInfo> getMoneyInfo(InputStream inStream) {
        ArrayList<MoneyInfo> al = null;
        MoneyInfo mi = null;
        XmlPullParser xmlPull = Xml.newPullParser();
        try {
            xmlPull.setInput(inStream, "UTF-8");
            int eventCode = xmlPull.getEventType();
            while (eventCode != XmlPullParser.END_DOCUMENT) {
                switch(eventCode) {
                    case XmlPullParser.START_DOCUMENT:
                        al = new ArrayList<MoneyInfo>();
                        break;
                    case XmlPullParser.START_TAG:
                        String name = xmlPull.getName();
                        if (name.equalsIgnoreCase("money")) {
                            mi = new MoneyInfo();
                            mi.setCode(xmlPull.getAttributeValue(null, "code"));
                            mi.setName(xmlPull.getAttributeValue(null, "name"));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (mi != null && xmlPull.getName().equalsIgnoreCase("money")) {
                            al.add(mi);
                            mi = null;
                        }
                        break;
                }
                eventCode = xmlPull.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, e.toString());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, e.toString());
        }
        return al;
    }

    public static double toExchange(String sourceCode, String targetCode, double count) {
        final String DEBUG_TAG = "ExchangeManager";
        String httpUrl = "http://download.finance.yahoo.com/d/quotes.html?s=" + sourceCode + targetCode + "=X&f=l1";
        String resultData = "";
        URL url = null;
        try {
            url = new URL(httpUrl);
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "MalformedURLException");
        }
        if (url != null) {
            try {
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
                BufferedReader buffer = new BufferedReader(in);
                String inputLine = null;
                while (((inputLine = buffer.readLine()) != null)) {
                    resultData += inputLine + "\n";
                }
                Log.e(DEBUG_TAG, httpUrl);
                in.close();
                urlConn.disconnect();
                if (resultData != null) {
                    Log.e(DEBUG_TAG, resultData);
                    count *= (Double.parseDouble(resultData));
                } else {
                    Log.e(DEBUG_TAG, "��ȡ������ΪNULL");
                }
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "IOException");
            }
        } else {
            Log.e(DEBUG_TAG, "Url NULL");
        }
        return count;
    }

    public static boolean isNum(String s) {
        boolean hasDot = false;
        char[] scr = s.toCharArray();
        for (char chr : scr) {
            if (!(chr >= '0' && chr <= '9')) {
                if (chr == '.' && !hasDot) {
                    hasDot = true;
                    continue;
                }
                return false;
            }
        }
        return true;
    }
}
