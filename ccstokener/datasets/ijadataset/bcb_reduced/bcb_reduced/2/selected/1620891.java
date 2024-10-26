package g1105.ps.crawler;

import g1105.ps.constant.Constant;
import g1105.ps.entity.Photo;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class MSBingImageCrawler extends Thread {

    private String query;

    private int downloadTotal;

    private SessionFactory sessionFactory;

    private Session session;

    public MSBingImageCrawler() {
        iniDatabaseSession();
    }

    private void iniDatabaseSession() {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
        session = sessionFactory.openSession();
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setDownloadTotal(int downloadTotal) {
        this.downloadTotal = downloadTotal;
    }

    private String getHTML(String pageURL, String encoding, String dirPath) throws IOException {
        StringBuilder pageHTML = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(pageURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "MSIE 7.0");
            connection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String line = null;
            while ((line = br.readLine()) != null) {
                pageHTML.append(line);
                pageHTML.append("\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        if (dirPath != null) {
            File file = new File(dirPath);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(pageHTML.toString());
            bufferedWriter.close();
        }
        return pageHTML.toString();
    }

    private List<String> getURLFromQuerypage(String html) {
        String seperator = "/images/search[?]q=";
        List<String> result = new ArrayList<String>();
        String regex = "[^\"]*view[^\"]*id[^\"]*";
        Pattern pattern = Pattern.compile(regex);
        String[] arrayStr = html.split(seperator);
        for (int i = 1; i < arrayStr.length; i++) {
            Matcher matcher = pattern.matcher(arrayStr[i]);
            matcher.find();
            try {
                String[] temp = matcher.group(0).split("amp;");
                String url = "http://cn.bing.com/images/search?q=";
                for (int j = 0; j < temp.length; j++) {
                    url += temp[j];
                }
                result.add(url);
            } catch (Exception e) {
            }
        }
        return result;
    }

    private String getImageURL(String html) {
        String regex = "href=\"([^\"]*)(.){1,100}logDetailPageClientClickEvent[(]'seefulllink'[)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String temp = null;
        try {
            temp = matcher.group(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private String getTag(String html) {
        String regex = "查看完整尺寸图片</a><span(.*)<span(.*)>(.*)</span>(.*)图像来源网页";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String temp = null;
        try {
            temp = matcher.group(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp == null) return ""; else return temp;
    }

    private String getInfoFromHtml(String html, String regex, int groupIndex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        String result = null;
        try {
            result = matcher.group(groupIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String makeImage(String imgUrl, String dirPath) {
        String filePath = null;
        String picName = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(imgUrl).openStream());
            Date date = new Date();
            long nowLong = date.getTime();
            int index = imgUrl.lastIndexOf("/");
            picName = nowLong + imgUrl.substring(index + 1, imgUrl.length());
            filePath = dirPath + picName;
            File imgdir = new File(dirPath);
            if (!imgdir.exists()) {
                imgdir.mkdirs();
            }
            File img = new File(filePath);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(img));
            byte[] buf = new byte[2048];
            int length = in.read(buf);
            while (length != -1) {
                out.write(buf, 0, length);
                length = in.read(buf);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return picName;
    }

    public List<Object[]> downloadPhotosByQuery(String query) {
        String basePath = "http://cn.bing.com/images/search?q=";
        String encoding = "UTF-8";
        String picName = null;
        String picDir = Constant.bingPicDir;
        String queryURL = basePath + query;
        List<Object[]> tuples = new ArrayList<Object[]>();
        for (int i = 0; i < query.length(); i++) {
            if ((int) query.charAt(i) > 128) {
                try {
                    query = URLDecoder.decode(query, encoding);
                    query = URLEncoder.encode(query, encoding);
                    queryURL = basePath + query;
                    query = URLDecoder.decode(query, encoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        String firstHtml = null;
        try {
            firstHtml = getHTML(queryURL, encoding, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(firstHtml.length());
        List<String> urls = getURLFromQuerypage(firstHtml);
        Iterator<String> iterator = urls.iterator();
        while (iterator.hasNext() && downloadTotal > 0) {
            Object[] tuple = new Object[7];
            String secondHtml = null;
            try {
                secondHtml = getHTML(iterator.next(), encoding, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String urlImage = getImageURL(secondHtml);
            if ((picName = makeImage(urlImage, picDir)) != null) {
                String tag = query + " " + getTag(secondHtml);
                syncToDatabase(picName, tag, Constant.bingRefer + picName);
                tuple[0] = picName;
                tuple[1] = tag;
                tuple[5] = "bing";
                tuple[6] = picDir + picName;
                tuples.add(tuple);
                System.out.println(tag);
                downloadTotal--;
            }
        }
        session.close();
        return tuples;
    }

    private void syncToDatabase(String picId, String tags, String picPath) {
        Transaction transaction = session.beginTransaction();
        transaction.begin();
        session.createSQLQuery("INSERT INTO PicTag1(PicId,PicTags,PicPath,Source) value ('" + picId + "','" + tags + "','" + picPath + "','bing')").executeUpdate();
        transaction.commit();
    }

    public void run() {
        downloadPhotosByQuery(query);
    }
}
