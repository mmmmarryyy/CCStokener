package com.javaeedev.service.weather;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.web.filter.ExpiresRefreshPolicy;

/**
 * Implementation of WeatherService.
 * 
 * @author Xuefeng
 * 
 * @spring.bean id="weatherService" destroy-method="destroy"
 */
public class YahooWeatherService implements WeatherService, Runnable {

    private static String URL = "http://xml.weather.yahoo.com/forecastrss?u=c&p=";

    private Cache cache;

    private Map<String, String> cities = new HashMap<String, String>();

    private Thread t;

    private volatile boolean running = true;

    public YahooWeatherService() {
        cities.put("北京", "CHXX0008");
        cities.put("上海", "CHXX0116");
        cities.put("天津", "CHXX0133");
        cities.put("重庆", "CHXX0017");
        cities.put("广州", "CHXX0037");
        cities.put("成都", "CHXX0016");
        cities.put("昆明", "CHXX0076");
        cities.put("深圳", "CHXX0120");
        cities.put("沈阳", "CHXX0119");
        cities.put("西安", "CHXX0141");
        cities.put("武汉", "CHXX0138");
        cities.put("南京", "CHXX0099");
        cities.put("杭州", "CHXX0044");
        cities.put("海口", "CHXX0502");
        cities.put("桂林", "CHXX0434");
        cities.put("济南", "CHXX0064");
        cities.put("拉萨", "CHXX0080");
        cities.put("哈尔滨", "CHXX0046");
        cities.put("乌鲁木齐", "CHXX0135");
        cities.put("香港", "CHXX0049");
        cities.put("澳门", "CHXX0512");
        t = new Thread(this);
        t.start();
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Weather getWeatherInfo(String city, Date date) throws WeatherException {
        Object cityCode = cities.get(city);
        if (cityCode == null) throw new WeatherException("没有" + city + "的天气预报数据。");
        if (date == null) date = new Date(); else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.clear(Calendar.HOUR_OF_DAY);
            c.clear(Calendar.MINUTE);
            c.clear(Calendar.SECOND);
            c.clear(Calendar.MILLISECOND);
            if (date.before(c.getTime())) throw new WeatherException("没有" + city + new SimpleDateFormat("MM-dd").format(date) + "的天气预报数据。");
        }
        String key = Weather.makeKey(city, date);
        try {
            return (Weather) cache.getFromCache(key);
        } catch (NeedsRefreshException nre) {
            cache.cancelUpdate(key);
            List list = null;
            try {
                list = _getWeathersFromYahoo(city);
            } catch (WeatherException we) {
                throw we;
            } catch (Exception e) {
                throw new WeatherException(e.getMessage());
            }
            _putInCache(list);
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Weather w = (Weather) it.next();
                if (_isSameDate(w.getDate(), date)) return w;
            }
            throw new WeatherException("没有" + city + new SimpleDateFormat("MM-dd").format(date) + "的天气预报数据。");
        }
    }

    private List _getWeathersFromYahoo(String city) {
        System.out.println("== get weather information of " + city + " from yahoo ==");
        try {
            URL url = new URL(URL + cities.get(city).toString());
            InputStream input = url.openStream();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            SAXParser parser = factory.newSAXParser();
            YahooHandler yh = new YahooHandler();
            yh.setCity(city);
            parser.parse(input, yh);
            return yh.getWeathers();
        } catch (MalformedURLException e) {
            throw new WeatherException("MalformedURLException");
        } catch (IOException e) {
            throw new WeatherException("无法读取数据。");
        } catch (ParserConfigurationException e) {
            throw new WeatherException("ParserConfigurationException");
        } catch (SAXException e) {
            throw new WeatherException("数据格式错误，无法解析。");
        }
    }

    public void destroy() {
        running = false;
        try {
            t.interrupt();
            t.join();
        } catch (InterruptedException e) {
        }
    }

    private void _putInCache(List list) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Weather w = (Weather) it.next();
            cache.putInCache(w.getKey(), w, new ExpiresRefreshPolicy(w.getExpiredPeriod()));
        }
    }

    private boolean _isSameDate(Date d1, Date d2) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d1).equals(new SimpleDateFormat("yyyy-MM-dd").format(d2));
    }

    public void run() {
        Set set = cities.keySet();
        while (running) {
            try {
                Thread.sleep(60 * 1000 * 10);
            } catch (InterruptedException e) {
                if (!running) break;
            }
            if (cache == null) continue;
            System.out.println("== clean all expired weather information ==");
            long shouldExpiredFrom = System.currentTimeMillis() - Weather.AVAILABLE_TIME;
            cache.flushAll(new Date(shouldExpiredFrom));
            Date today = new Date();
            Iterator it = set.iterator();
            while (it.hasNext()) {
                String city = it.next().toString();
                String key = Weather.makeKey(city, today);
                System.out.println("-- check city " + city + " weather of today --");
                boolean needCatch = false;
                try {
                    Weather w = (Weather) cache.getFromCache(key);
                    if (w.isNearExpired()) needCatch = true;
                } catch (NeedsRefreshException e) {
                    cache.cancelUpdate(key);
                    System.out.println(city + " is not catch from yahoo yet.");
                    needCatch = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (needCatch) {
                    try {
                        List list = _getWeathersFromYahoo(city);
                        _putInCache(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

class YahooHandler extends DefaultHandler {

    private static final Map<String, String> translations = new HashMap<String, String>();

    static {
        translations.put("sunny", "晴");
        translations.put("cloudy", "多云");
        translations.put("mostly cloudy", "多云");
        translations.put("partly cloudy", "局部多云");
        translations.put("showers", "小雨");
        translations.put("rain", "中雨");
        translations.put("thunderstorms", "雷雨");
        translations.put("light snow", "小雪");
        translations.put("snow showers", "雨夹雪");
    }

    private String city;

    private Date publish = null;

    private List<Weather> weathers = new ArrayList<Weather>(2);

    public void setCity(String city) {
        this.city = city;
    }

    public List<Weather> getWeathers() {
        return weathers;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("yweather:condition".equals(qName)) {
            String s_date = attributes.getValue(3);
            System.out.println("-- publish @ " + s_date);
            try {
                publish = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a z", Locale.US).parse(s_date);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SAXException("Cannot parse date: " + s_date);
            }
        } else if ("yweather:forecast".equals(qName)) {
            String s_date = attributes.getValue(1);
            Date date = null;
            try {
                date = new SimpleDateFormat("dd MMM yyyy", Locale.US).parse(s_date);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SAXException("Cannot parse date: " + s_date);
            }
            int low = Integer.parseInt(attributes.getValue(2));
            int high = Integer.parseInt(attributes.getValue(3));
            String text = attributes.getValue(4);
            int code = Integer.parseInt(attributes.getValue(5));
            System.out.println("-- found " + city + " @ " + new SimpleDateFormat("MM-dd").format(date) + " from yahoo --");
            weathers.add(new Weather(publish, city, date, low, high, _translate(text), code));
        }
        super.startElement(uri, localName, qName, attributes);
    }

    private static String _translate(String text) {
        Object obj = translations.get(text);
        if (obj == null) return text;
        return (String) obj;
    }
}
