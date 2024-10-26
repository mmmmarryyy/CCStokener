package edu.tum.in.campar.twodui.studentcanteen.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Xml;

public class DataProvider {

    private static DataProvider provider;

    private ArrayList<DailyMenu> menuList = new ArrayList<DailyMenu>();

    static {
        provider = new DataProvider();
    }

    public ArrayList<DailyMenu> getData() {
        return menuList;
    }

    public static DataProvider get() {
        return provider;
    }

    public void update(Context context) {
        InputStream is = readMealsFromRSS();
        if (is != null) {
            saveMealsToDisk(context, is);
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    public void load(Context context) {
        menuList.clear();
        InputStream is = readMealsFromDisk(context);
        if (is != null) {
            menuList = parseMenu(is);
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    private InputStream readMealsFromRSS() {
        URL url = null;
        InputStream is;
        try {
            url = new URL("http://songbook.me/services/mensa/");
        } catch (MalformedURLException e1) {
        }
        try {
            is = url.openStream();
        } catch (Exception e) {
            return null;
        }
        return is;
    }

    private InputStream readMealsFromDisk(Context context) {
        try {
            FileInputStream fis = context.openFileInput("meals.xml");
            return fis;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveMealsToDisk(Context context, InputStream is) {
        try {
            FileOutputStream fos = context.openFileOutput("meals.xml", Context.MODE_PRIVATE);
            byte[] b = new byte[1024];
            int read;
            while ((read = is.read(b)) != -1) fos.write(b, 0, read);
        } catch (Exception e) {
        }
    }

    private ArrayList<DailyMenu> parseMenu(InputStream is) {
        menuList = new ArrayList<DailyMenu>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");
            int eventType = parser.getEventType();
            Calendar calendar = Calendar.getInstance();
            Date today = new Date(calendar.get(Calendar.YEAR) - 1900, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && (parser.getName().compareToIgnoreCase("Tagesplan") == 0)) {
                    String date = parser.getAttributeValue("", "tag");
                    Date parsedDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN).parse(date);
                    String day = parser.getAttributeValue("", "taglang");
                    if ((day != null) && (!parsedDate.before(today))) menuList.add(new DailyMenu(day));
                }
                if ((eventType == XmlPullParser.START_TAG) && (parser.getName().compareToIgnoreCase("Gericht") == 0)) {
                    String name = parser.getAttributeValue("", "name");
                    String price = parser.getAttributeValue("", "preis");
                    if ((name != null) && (price != null)) if (!menuList.isEmpty()) menuList.get(menuList.size() - 1).addMeal(new Meal(name, price));
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
        }
        return menuList;
    }
}
