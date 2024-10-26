package fr.udata.server;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import com.ibm.icu.util.Calendar;

public class WebParser {

    private static String date = "1119";

    public static String getResponse(String url, String encoding) throws ClientProtocolException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter("http.protocol.content-charset", encoding);
        BasicHttpContext localContext = new BasicHttpContext();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget, localContext);
        HttpHost target = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        System.out.println("Final target: " + target);
        InputStream is = response.getEntity().getContent();
        String result = IOUtils.toString(is, encoding);
        return result;
    }

    public static String getCode(String content) {
        String code = null;
        String regex = "\\[{2}AST" + date + "-\\S{8}-\\w{2}\\]{2}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            code = matcher.group();
        }
        return code;
    }

    public static Date getTime(String content) {
        String time = null;
        String date = null;
        String regex = "</b></a>：(\\d{4}/\\d{2}/\\d{2}).*\\s(\\d{1,2}:\\d{1,2}:\\d{1,2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            date = matcher.group(1);
            time = matcher.group(2);
            String[] dateArray = date.split("/");
            int year = Integer.valueOf(dateArray[0]);
            int month = Integer.valueOf(dateArray[1]);
            int day = Integer.valueOf(dateArray[2]);
            String[] timeArray = time.split(":");
            int hour = Integer.valueOf(timeArray[0]);
            int minute = Integer.valueOf(timeArray[1]);
            int second = Integer.valueOf(timeArray[2]);
            Calendar cal = Calendar.getInstance();
            System.out.println(year + "" + month + "" + day + "" + hour + "" + minute + "" + second);
            cal.set(year, month, day, hour, minute, second);
            Date result = new Date(cal.getTime().getTime());
            return result;
        }
        return null;
    }

    public static String getIds(String content) {
        String id = null;
        String regex = "ID:\\S{8}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            id = matcher.group();
        }
        return id;
    }

    public static List<String> getNames(String content) {
        List<String> names = new ArrayList<String>();
        String regex = "&lt;&lt;((?!&lt;).)*&gt;&gt;";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            names.add(matcher.group().replace("&lt;&lt;", "").replace("&gt;&gt;", ""));
        }
        return names;
    }
}
