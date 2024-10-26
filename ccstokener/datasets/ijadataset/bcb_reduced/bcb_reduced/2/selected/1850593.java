package com.projects.utils;

import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.projects.database.DBAdapter;
import com.projects.series.Episode;
import com.projects.series.Serie;
import com.projects.seriesnotifier.R;
import android.R.array;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;

public class SeriesUtils {

    public static String SERIES = "series";

    public static String OWNSERIES = "ownseries";

    public static String fileToString(FileInputStream fis) {
        int ch = 0;
        StringBuffer buffer = new StringBuffer();
        try {
            while ((ch = fis.read()) != -1) {
                buffer.append((char) ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static boolean isEmptyFile(String name, Context context) {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(name);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return true;
        }
        return fileToString(fis).equals("");
    }

    public static List<Serie> getSeries(Context context, String tipo) {
        if (tipo == SERIES) {
            return getListSeriesList(context);
        } else if (tipo == OWNSERIES) {
            return getDBSeries(context);
        } else {
            return null;
        }
    }

    public static String[] getListSeries(Context context) {
        String[] list = new String[100];
        String names;
        FileInputStream fis;
        list = context.fileList();
        if (list.length > 0) {
            try {
                fis = context.openFileInput(SERIES);
                names = fileToString(fis);
                list = names.split(",");
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static List<Serie> getListSeriesList(Context context) {
        List<Serie> list = new ArrayList<Serie>();
        List<String> listString = new ArrayList<String>();
        Serie serie = new Serie();
        String names;
        FileInputStream fis;
        listString = Arrays.asList(context.fileList());
        if (!list.isEmpty()) {
            try {
                fis = context.openFileInput(SERIES);
                names = fileToString(fis);
                listString = Arrays.asList(names.split(","));
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (String s : listString) {
            serie = new Serie();
            serie.setName(s);
            list.add(serie);
        }
        return list;
    }

    public static String[] getOwnSeries(Context context) {
        String[] listFiles = null;
        String[] list = null;
        FileInputStream fis;
        String names;
        listFiles = context.fileList();
        if (listFiles.length > 1) {
            try {
                fis = context.openFileInput(OWNSERIES);
                names = fileToString(fis);
                fis.close();
                if (names != "") {
                    list = names.split(",");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static int addSerie(String file, String serie, Context context) {
        String text = "";
        int ret = 0;
        if (serieAlreadyExists(file, serie, context)) {
            ret = -1;
        } else if (file == OWNSERIES && !serieExists(serie, context)) {
            ret = -2;
        } else {
            if (!isEmptyFile(file, context)) {
                text += ",";
            }
            text += serie;
            FileOutputStream fos;
            try {
                fos = context.openFileOutput(file, Context.MODE_APPEND);
                fos.write(text.getBytes());
                fos.close();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static int deleteSerie(String file, String serie, Context context) {
        int ret = 0;
        if (!serieAlreadyExists(file, serie, context)) {
            ret = -1;
        } else {
            List<Serie> list = getSeries(context, file);
            if (!list.isEmpty()) {
                int i = 0, j = 0;
                List<Serie> listAux = new ArrayList<Serie>();
                for (Serie item : list) {
                    if (!item.getName().toLowerCase().equals(serie.toLowerCase())) {
                        listAux.add(item);
                        i++;
                    }
                    j++;
                }
                String text = toSimpleString(listAux);
                FileOutputStream fos;
                try {
                    fos = context.openFileOutput(file, Context.MODE_PRIVATE);
                    fos.write(text.getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (list != null) {
                String text = "";
                FileOutputStream fos;
                try {
                    fos = context.openFileOutput(file, Context.MODE_PRIVATE);
                    fos.write(text.getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    private static String toSimpleString(List<Serie> listAux) {
        String ret = listAux.get(0).getName();
        for (int i = 1; i < listAux.size(); i++) {
            ret += "," + listAux.get(i).getName();
        }
        return ret;
    }

    private static boolean serieAlreadyExists(String file, String serie, Context context) {
        boolean exists = false;
        String[] list = null;
        FileInputStream fis;
        String names;
        try {
            fis = context.openFileInput(file);
            names = fileToString(fis);
            fis.close();
            list = names.split(",");
            for (String s : list) {
                if (s.toLowerCase().equals(serie.toLowerCase())) {
                    exists = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        return exists;
    }

    public static boolean serieAlreadyExists(int id, Context context) {
        System.out.println("Identificador a comprobar: " + id);
        return serieAlreadyExistsDB(id, context);
    }

    private static boolean serieExists(String serie, Context context) {
        boolean exists = false;
        String[] list = null;
        FileInputStream fis;
        String names;
        try {
            fis = context.openFileInput(SERIES);
            names = fileToString(fis);
            fis.close();
            list = names.split(",");
            for (String s : list) {
                if (s.toLowerCase().equals(serie.toLowerCase())) {
                    exists = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        return exists;
    }

    public static List<Serie> getSeriesByQuery(Context applicationContext, String query) {
        List<Serie> list = null;
        list = getSeriesTvDBList(query, applicationContext);
        return list;
    }

    private static String[] filterSeries(String[] split, String query) {
        String[] listaRet = new String[getNumberOcurrences(split, query)];
        int i = 0;
        for (String serie : split) {
            if (serie.toLowerCase().contains(query.toLowerCase())) {
                listaRet[i] = serie;
                i++;
            }
        }
        return listaRet;
    }

    private static int getNumberOcurrences(String[] split, String query) {
        int ret = 0;
        for (String serie : split) {
            if ((serie.toLowerCase()).contains((query.toLowerCase()))) {
                ret++;
            }
        }
        return ret;
    }

    public static List<Serie> getDBSeries(Context context) {
        DBAdapter db = new DBAdapter(context);
        int cols = 0;
        db.open();
        Serie serie = new Serie();
        List<Serie> series = new ArrayList<Serie>();
        Cursor cursor = db.getSeries();
        cursor.moveToFirst();
        cols = cursor.getCount();
        if (cols > 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                serie = new Serie();
                serie.setId(cursor.getString(0));
                serie.setName(cursor.getString(1));
                series.add(serie);
            }
        }
        db.close();
        return series;
    }

    private static boolean serieAlreadyExistsDB(int id, Context context) {
        boolean exists = false;
        DBAdapter db = new DBAdapter(context);
        db.open();
        exists = db.existsSerie(id);
        db.close();
        return exists;
    }

    public static List<Episode> getDBSeriesUpdates(Context context) {
        DBAdapter db = new DBAdapter(context);
        int cols = 0;
        db.open();
        Episode episode = new Episode();
        List<Episode> episodes = new ArrayList<Episode>();
        Cursor cursor = db.getSeriesUpdates();
        cursor.moveToFirst();
        cols = cursor.getCount();
        if (cols > 0) {
            for (int i = 0; i < cols; i++) {
                episode = new Episode();
                System.out.println("Posicion en la table: " + cursor.getPosition() + "ID: " + cursor.getString(cursor.getColumnIndex("_id")));
                episode.setId(cursor.getString(cursor.getColumnIndex("_id")));
                episode.setSerieId(cursor.getString(1));
                updateEpisodeRate(episode.getId(), context);
                episode.setSerieName(cursor.getString(2));
                episode.setSeason(cursor.getString(3));
                episode.setEpisode(cursor.getString(4));
                episode.setDate(cursor.getString(5));
                episode.setRate(cursor.getFloat(6));
                if (cursor.getInt(7) == 1) episode.markAsShowed();
                episodes.add(episode);
                cursor.moveToNext();
            }
        }
        db.close();
        return episodes;
    }

    private static void updateEpisodeRate(String id, Context context) {
    }

    public static long addDBSerie(String serie, int id, Context context) {
        DBAdapter db = new DBAdapter(context);
        long ret = 0;
        db.open();
        ret = db.insertSerie(serie, id);
        db.close();
        return ret;
    }

    public static long addDBSerieUpdates(Episode ep, Context context) {
        DBAdapter db = new DBAdapter(context);
        long ret = 0;
        db.open();
        ret = db.insertSerieUpdate(Integer.parseInt(ep.getId()), Integer.parseInt(ep.getSerieId()), ep.getSerieName(), ep.getSeason(), ep.getEpisode(), ep.getRate(), ep.getDate());
        db.close();
        return ret;
    }

    public static long deleteDBSerie(String serie, Context context) {
        long ret = 0;
        DBAdapter db = new DBAdapter(context);
        db.open();
        db.deleteSerie(serie);
        db.close();
        return ret;
    }

    public static long deleteDBSerie(int id, Context context) {
        long ret = 0;
        DBAdapter db = new DBAdapter(context);
        db.open();
        db.deleteSerie(id);
        db.close();
        return ret;
    }

    public static int updateDBSeriesUpdates(Context context, int id, int value) {
        DBAdapter db = new DBAdapter(context);
        int ret;
        db.open();
        ret = db.updateSeriesUpdates(id, value);
        db.close();
        return ret;
    }

    public static String getSeriesTvDB(String name, Context context) {
        URL url;
        String ret = "";
        String site = context.getString(R.string.getSeries);
        String paramName = context.getString(R.string.getSeriesParam);
        try {
            url = new URL(site + "?" + paramName + "=" + URLEncoder.encode(name));
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("Series");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry = (Element) nl.item(i);
                        Element SerieName = (Element) entry.getElementsByTagName("SeriesName").item(0);
                        if (i == 0) {
                            ret += SerieName.getFirstChild().getNodeValue();
                        } else {
                            ret += "," + SerieName.getFirstChild().getNodeValue();
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Serie> getSeriesTvDBList(String name, Context context) {
        URL url;
        List<Serie> ret = new ArrayList<Serie>();
        Serie serie;
        String site = context.getString(R.string.getSeries);
        String paramName = context.getString(R.string.getSeriesParam);
        try {
            url = new URL(site + "?" + paramName + "=" + URLEncoder.encode(name));
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("Series");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry = (Element) nl.item(i);
                        Element SerieName = (Element) entry.getElementsByTagName("SeriesName").item(0);
                        Element SerieId = (Element) entry.getElementsByTagName("id").item(0);
                        serie = new Serie();
                        serie.setName(SerieName.getFirstChild().getNodeValue());
                        serie.setId(SerieId.getFirstChild().getNodeValue());
                        serie.setFav(serieAlreadyExists(Integer.parseInt(serie.getId()), context));
                        ret.add(serie);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Serie getSeriesInfo(int id, Context context) {
        URL url;
        Serie serie = new Serie();
        String site = context.getString(R.string.infoSerieUrl);
        String siteEnd = context.getString(R.string.infoSerieUrlEnd);
        try {
            url = new URL(site + id + siteEnd);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("Series");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry = (Element) nl.item(i);
                        Element SerieName = (Element) entry.getElementsByTagName("SeriesName").item(0);
                        Element SerieId = (Element) entry.getElementsByTagName("id").item(0);
                        Element SerieDesc = (Element) entry.getElementsByTagName("Overview").item(0);
                        Element SerieImgUrl = (Element) entry.getElementsByTagName("banner").item(0);
                        Element SerieState = (Element) entry.getElementsByTagName("Status").item(0);
                        Element SerieRating = (Element) entry.getElementsByTagName("Rating").item(0);
                        serie = new Serie();
                        serie.setName(SerieName.getFirstChild().getNodeValue());
                        serie.setId(SerieId.getFirstChild().getNodeValue());
                        serie.setRate(SerieRating.getFirstChild().getNodeValue());
                        if (SerieDesc.getFirstChild() != null) serie.setDesc(SerieDesc.getFirstChild().getNodeValue());
                        if (SerieImgUrl.getFirstChild() != null) serie.setImgUrl(SerieImgUrl.getFirstChild().getNodeValue());
                        if (SerieState.getFirstChild() != null) serie.setEstado(SerieState.getFirstChild().getNodeValue(), context);
                        boolean exists = serieAlreadyExists(Integer.parseInt(serie.getId()), context);
                        serie.setFav(exists);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return serie;
    }

    public static List<Serie> getUpdatesTvDBList(Context context) {
        URL url;
        Serie serie;
        List<Serie> seriesNuevas = new ArrayList<Serie>();
        String site = context.getString(R.string.getUpdates);
        String paramTimeName = context.getString(R.string.getUpdatesTimeParam);
        long epoch = DateUtils.getEpochDate(context);
        if (epoch == -1) {
            epoch = (System.currentTimeMillis() / 1000) - (86400);
        }
        String epochString = String.valueOf(epoch);
        try {
            url = new URL(site + "?" + paramTimeName + "=" + URLEncoder.encode(epochString));
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("Series");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element SerieId = (Element) nl.item(i);
                        serie = new Serie();
                        serie.setId(SerieId.getFirstChild().getNodeValue());
                        seriesNuevas.add(serie);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        DateUtils.insertEpochDate(context, (System.currentTimeMillis() / 1000));
        return filterByOwnSeries(context, seriesNuevas);
    }

    public static List<Episode> getUpdatesService(Context context) {
        URL url;
        Episode ep;
        List<Episode> episodesNuevos = new ArrayList<Episode>();
        List<Serie> seriesDB = new ArrayList<Serie>();
        String site = context.getString(R.string.getUpdatesService);
        String paramTimeName = context.getString(R.string.getUpdatesTimeParam);
        String generateSeriesRequestString = "";
        long epoch = DateUtils.getEpochDate(context);
        if (epoch == -1) {
            epoch = (System.currentTimeMillis() / 1000) - (86400 * 30);
        }
        String epochString = String.valueOf(epoch);
        System.out.println("Obtenemos las actualizaciones de series desde el servicio");
        try {
            seriesDB = getDBSeries(context);
            generateSeriesRequestString = generateSeriesRequestString(seriesDB);
            url = new URL(site + "&" + paramTimeName + "=" + URLEncoder.encode(epochString) + "&series=" + generateSeriesRequestString);
            System.out.println("URL: " + url);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("episode");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element episode = (Element) nl.item(i);
                        Node id = episode.getChildNodes().item(0);
                        Node serieid = episode.getChildNodes().item(2);
                        Node seriename = episode.getChildNodes().item(1);
                        Node seasonNumber = episode.getChildNodes().item(3);
                        Node episodeNumber = episode.getChildNodes().item(4);
                        Node date = episode.getChildNodes().item(5);
                        Node rate = episode.getChildNodes().item(6);
                        Node votes = episode.getChildNodes().item(7);
                        ep = new Episode();
                        ep.setId(id.getFirstChild().getNodeValue());
                        ep.setSerieId(serieid.getFirstChild().getNodeValue());
                        ep.setSerieName(seriename.getFirstChild().getNodeValue());
                        ep.setSeason(seasonNumber.getFirstChild().getNodeValue());
                        ep.setEpisode(episodeNumber.getFirstChild().getNodeValue());
                        int numVotes = (((Integer.parseInt(votes.getFirstChild().getNodeValue()) != 0)) ? Integer.parseInt(votes.getFirstChild().getNodeValue()) : 1);
                        ep.setRate((float) Integer.parseInt(rate.getFirstChild().getNodeValue()) / (float) numVotes);
                        ep.setDate(date.getFirstChild().getNodeValue());
                        episodesNuevos.add(ep);
                        addDBSerieUpdates(ep, context);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DateUtils.insertEpochDate(context, (System.currentTimeMillis() / 1000));
        return episodesNuevos;
    }

    private static String generateSeriesRequestString(List<Serie> series) {
        String seriesRequestString = "";
        for (int i = 0; i < series.size(); i++) {
            seriesRequestString += series.get(i).getId();
            if (i != series.size() - 1) {
                seriesRequestString += ",";
            }
        }
        return seriesRequestString;
    }

    public static List<Serie> filterByOwnSeries(Context context, List<Serie> updatesSeries) {
        List<Serie> myUpdatesSeries = new ArrayList<Serie>();
        List<Serie> mySeries = getDBSeries(context);
        for (Serie serie : updatesSeries) {
            for (Serie mySerie : mySeries) {
                if (serie.getId().equals(mySerie.getId())) {
                    myUpdatesSeries.add(serie);
                }
            }
        }
        return myUpdatesSeries;
    }

    public static void rateSerie(int id, int value, Context context) {
        URL url;
        String site = context.getString(R.string.rateEpisodeUrl);
        String rateParam = context.getString(R.string.rateParam);
        String idParam = context.getString(R.string.idParam);
        try {
            url = new URL(site + "?" + rateParam + "=" + value + "&" + idParam + "=" + id);
            System.out.println("URL: " + url);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode;
            responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Votado");
            } else {
                System.out.println("Error en la votacion");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Serie> getRecommendations(Context context) {
        URL url;
        Serie serie;
        List<Serie> recommendations = new ArrayList<Serie>();
        String site = "http://www.seriesnotifier.com/series/seriesByRate";
        try {
            url = new URL(site);
            System.out.println("URL: " + url);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("serie");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element nSerie = (Element) nl.item(i);
                        Node id = nSerie.getChildNodes().item(0);
                        Node seriename = nSerie.getChildNodes().item(1);
                        Node rate = nSerie.getChildNodes().item(2);
                        Node votes = nSerie.getChildNodes().item(3);
                        serie = new Serie();
                        serie.setId(id.getFirstChild().getNodeValue());
                        serie.setName(seriename.getFirstChild().getNodeValue());
                        serie.setRate(rate.getFirstChild().getNodeValue());
                        serie.setVotes(votes.getFirstChild().getNodeValue());
                        serie.setFav(serieAlreadyExists(Integer.parseInt(serie.getId()), context));
                        recommendations.add(serie);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recommendations;
    }

    public static List<Episode> updateRateSeriesUpdates(List<Episode> episodes, Context applicationContext) {
        String id;
        float rate;
        for (Episode episode : episodes) {
            id = episode.getId();
            rate = Float.parseFloat(getRate(id));
            rate = (float) (Math.round(rate * 100.0) / 100.0);
            episode.setRate(rate);
            DBAdapter db = new DBAdapter(applicationContext);
            db.open();
            db.updateSeriesUpdates(Integer.parseInt(id), rate);
            db.close();
        }
        return episodes;
    }

    private static String getRate(String id) {
        URL url;
        String rate = "";
        String site = "http://www.seriesnotifier.com/series/getRate";
        try {
            url = new URL(site + "?id=" + id);
            System.out.println("URL: " + url);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("rate");
                if (nl != null && nl.getLength() > 0) {
                    Element nSerie = (Element) nl.item(0);
                    rate = nSerie.getFirstChild().getNodeValue();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rate;
    }
}
