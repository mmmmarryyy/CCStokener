package com.siri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Weather {

    Parser parser;

    WeatherNow wn;

    private LinearLayout wl_l;

    private LinearLayout tl_l;

    private TextView TodayCondition;

    private TextView TodayWind;

    private ImageView TodayIcon;

    private TextView TodayC;

    private TextView TodayF;

    private TextView TodayHumidity;

    private TextView day1;

    private TextView day2;

    private TextView day3;

    private TextView day4;

    private ImageView dayImg1;

    private ImageView dayImg2;

    private ImageView dayImg3;

    private ImageView dayImg4;

    private TextView dayH1;

    private TextView dayH2;

    private TextView dayH3;

    private TextView dayH4;

    private TextView dayL1;

    private TextView dayL2;

    private TextView dayL3;

    private TextView dayL4;

    private TextView dayC1;

    private TextView dayC2;

    private TextView dayC3;

    private TextView dayC4;

    public Weather(Activity parent) {
        wl_l = (LinearLayout) parent.findViewById(R.id.mainWeather);
        day1 = (TextView) parent.findViewById(R.id.weekDate1);
        day2 = (TextView) parent.findViewById(R.id.weekDate2);
        day3 = (TextView) parent.findViewById(R.id.weekDate3);
        day4 = (TextView) parent.findViewById(R.id.weekDate4);
        dayImg1 = (ImageView) parent.findViewById(R.id.weekImg1);
        dayImg2 = (ImageView) parent.findViewById(R.id.weekImg2);
        dayImg3 = (ImageView) parent.findViewById(R.id.weekImg3);
        dayImg4 = (ImageView) parent.findViewById(R.id.weekImg4);
        dayH1 = (TextView) parent.findViewById(R.id.weekHigh1);
        dayH2 = (TextView) parent.findViewById(R.id.weekHigh2);
        dayH3 = (TextView) parent.findViewById(R.id.weekHigh3);
        dayH4 = (TextView) parent.findViewById(R.id.weekHigh4);
        dayL1 = (TextView) parent.findViewById(R.id.weekLow1);
        dayL2 = (TextView) parent.findViewById(R.id.weekLow2);
        dayL3 = (TextView) parent.findViewById(R.id.weekLow3);
        dayL4 = (TextView) parent.findViewById(R.id.weekLow4);
        dayC1 = (TextView) parent.findViewById(R.id.weekCon1);
        dayC2 = (TextView) parent.findViewById(R.id.weekCon2);
        dayC3 = (TextView) parent.findViewById(R.id.weekCon3);
        dayC4 = (TextView) parent.findViewById(R.id.weekCon4);
        TodayCondition = (TextView) parent.findViewById(R.id.weekTodayCondition);
        TodayWind = (TextView) parent.findViewById(R.id.weekTodayWind);
        TodayIcon = (ImageView) parent.findViewById(R.id.weekTodayIcon);
        TodayC = (TextView) parent.findViewById(R.id.weekTodayCTemp);
        TodayF = (TextView) parent.findViewById(R.id.weekTodayFTemp);
        TodayHumidity = (TextView) parent.findViewById(R.id.weekTodayhumidity);
        wl_l.setVisibility(View.GONE);
    }

    public String GetLocation(Context c) {
        String Location;
        LocationManager locationManager;
        String context = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) c.getSystemService(context);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Geocoder t_Geocoder = new Geocoder(c, Locale.ENGLISH);
        try {
            List<Address> addresses = t_Geocoder.getFromLocation(latitude, longitude, 1);
            Address address = addresses.get(0);
            Location = address.getLocality();
            return Location;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void SetTodayWeather() {
        TodayCondition.setText(wn.condition);
        TodayC.setText(wn.temp_c + "˚C");
        TodayF.setText(wn.temp_f + "˚F");
        TodayHumidity.setText(wn.humidity);
        TodayWind.setText(wn.wind);
        try {
            Bitmap bm;
            URL url = new URL("http://www.google.com" + wn.icon);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream is = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            TodayIcon.setImageBitmap(bm);
            bis.close();
            is.close();
        } catch (IOException e) {
        }
    }

    public void GetTodayWeather(Context context) {
        parser = new Parser();
        ArrayList<WeatherForecast> temp = parser.connectWeather(context);
        SetTodayWeather();
    }

    public void GetWeekWeather(Context context) {
        parser = new Parser();
        ArrayList<WeatherForecast> temp = parser.connectWeather(context);
        SetTodayWeather();
        wl_l.setVisibility(View.VISIBLE);
        day1.setText(temp.get(0).dayOfWeek);
        day2.setText(temp.get(1).dayOfWeek);
        day3.setText(temp.get(2).dayOfWeek);
        day4.setText(temp.get(3).dayOfWeek);
        dayH1.setText(temp.get(0).high + "˚C");
        dayH2.setText(temp.get(1).high + "˚C");
        dayH3.setText(temp.get(2).high + "˚C");
        dayH4.setText(temp.get(3).high + "˚C");
        dayL1.setText(temp.get(0).low + "˚C");
        dayL2.setText(temp.get(1).low + "˚C");
        dayL3.setText(temp.get(2).low + "˚C");
        dayL4.setText(temp.get(3).low + "˚C");
        dayC1.setText(temp.get(0).condition);
        dayC2.setText(temp.get(1).condition);
        dayC3.setText(temp.get(2).condition);
        dayC4.setText(temp.get(3).condition);
        try {
            Bitmap bm;
            URL url = new URL("http://www.google.com" + temp.get(0).icon);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream is = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            dayImg1.setImageBitmap(bm);
            url = new URL("http://www.google.com" + temp.get(1).icon);
            connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
            bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            dayImg2.setImageBitmap(bm);
            url = new URL("http://www.google.com" + temp.get(2).icon);
            connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
            bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            dayImg3.setImageBitmap(bm);
            url = new URL("http://www.google.com" + temp.get(3).icon);
            connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
            bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            dayImg4.setImageBitmap(bm);
            bis.close();
            is.close();
        } catch (IOException e) {
        }
    }

    public class Parser {

        private String TAG = "Parser";

        ArrayList<WeatherForecast> connectWeather(Context c) {
            InputStream is = null;
            try {
                String url = "http://www.google.com/ig/api?hl=ko&weather=";
                url += GetLocation(c);
                URL targetURL = new URL(url);
                is = targetURL.openStream();
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(is, "euc-kr");
                return parseWeather(parser);
            } catch (Exception e) {
                e.toString();
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            return null;
        }

        ArrayList<WeatherForecast> parseWeather(XmlPullParser parser) throws XmlPullParserException, IOException {
            ArrayList<WeatherForecast> parseWeather = new ArrayList<WeatherForecast>();
            String tag;
            WeatherForecast wf = null;
            boolean isWn = false;
            int parserEvent = parser.getEventType();
            while (parserEvent != XmlPullParser.END_DOCUMENT) {
                switch(parserEvent) {
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if (tag.compareTo("forecast_conditions") == 0) {
                            parseWeather.add(wf);
                        } else if (tag.compareTo("current_conditions") == 0) {
                            isWn = false;
                        }
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (!isWn) {
                            if (tag.compareTo("forecast_conditions") == 0) {
                                wf = new WeatherForecast();
                            } else if (tag.compareTo("current_conditions") == 0) {
                                wn = new WeatherNow();
                                isWn = true;
                            } else if (wf != null && tag.compareTo("day_of_week") == 0) {
                                wf.dayOfWeek = parser.getAttributeValue(null, "data");
                                Log.d(TAG, wf.dayOfWeek);
                            } else if (wf != null && tag.compareTo("low") == 0) {
                                wf.low = parser.getAttributeValue(null, "data");
                            } else if (wf != null && tag.compareTo("high") == 0) {
                                wf.high = parser.getAttributeValue(null, "data");
                            } else if (wf != null && tag.compareTo("icon") == 0) {
                                wf.icon = parser.getAttributeValue(null, "data");
                            } else if (wf != null && tag.compareTo("condition") == 0) {
                                wf.condition = parser.getAttributeValue(null, "data");
                            }
                        } else {
                            if (wn != null && tag.compareTo("humidity") == 0) {
                                wn.humidity = parser.getAttributeValue(null, "data");
                            } else if (wn != null && tag.compareTo("temp_f") == 0) {
                                wn.temp_f = parser.getAttributeValue(null, "data");
                            } else if (wn != null && tag.compareTo("temp_c") == 0) {
                                wn.temp_c = parser.getAttributeValue(null, "data");
                            } else if (wn != null && tag.compareTo("icon") == 0) {
                                wn.icon = parser.getAttributeValue(null, "data");
                            } else if (wn != null && tag.compareTo("condition") == 0) {
                                wn.condition = parser.getAttributeValue(null, "data");
                            } else if (wn != null && tag.compareTo("wind_condition") == 0) {
                                wn.wind = parser.getAttributeValue(null, "data");
                            }
                        }
                        break;
                }
                parserEvent = parser.next();
            }
            return parseWeather;
        }
    }

    public class WeatherNow {

        public String humidity;

        public String temp_f;

        public String temp_c;

        public String condition;

        public String icon;

        public String wind;
    }

    public class WeatherForecast {

        public String dayOfWeek;

        public String high;

        public String low;

        public String icon;

        public String condition;
    }
}
