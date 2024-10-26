package com.z8888q.zlottery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class LotteryNumber {

    public static final int SHUANG = 101;

    public static final int F3D = 102;

    public static final int F7LECAI = 104;

    public static final int F15X5 = 103;

    public static final int DALETO = 201;

    public static final int PAILIE3 = 202;

    public static final int PAILIE5 = 203;

    public static final int T7XCAI = 204;

    public static final int F22X5 = 0;

    String path = "/allopenprized/historyprizedetail.jhtml?action=exportFile";

    String hostName = "http://kaijiang.2caipiao.com";

    String fileUrl = "";

    public String sData = "2011-05-14";

    public String eData = "2011-07-14";

    public LotteryNumber() {
    }

    public ArrayList<HashMap<String, Object>> getLotteryNumber(int lotteryType) {
        if (lotteryType == 0) return null;
        return getLotteryData(lotteryType);
    }

    public String generateUrl(int lotteryType, String sData, String eData) {
        String pathString = hostName + path + "&lotteryType=" + lotteryType + "&sDate=" + sData + "&eDate=" + eData;
        return pathString;
    }

    public String generateUrl(int lotteryType) {
        String pathString = hostName + path + "&lotteryType=" + lotteryType + "&sDate=" + generateSData() + "&eDate=" + generateEData();
        return pathString;
    }

    public String generateEData() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateTime = dateFormat.format(calendar.getTime());
        return dateTime;
    }

    public String generateSData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateTime = dateFormat.format(calendar.getTime());
        return dateTime;
    }

    public HashMap<String, Object> parseTxt(String s) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String period = s.substring(0, s.indexOf(' '));
        String s1 = s.substring(s.indexOf(' ') + 1);
        String s2 = s1.trim();
        String result = s2.substring(0, s2.indexOf(' '));
        String s3 = s2.substring(s2.indexOf(' ') + 1);
        String s4 = s3.trim();
        String time = s4;
        map.put("Issue", "��" + period + "��");
        map.put("Date", time);
        map.put("Number", convertToList(result));
        System.out.println("period:" + period + ">>result:" + result + ">>time:" + time);
        return map;
    }

    public ArrayList<HashMap<String, Object>> getLotteryData(int lotteryType) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        try {
            HttpGet httpGet = new HttpGet(generateUrl(lotteryType));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                InputStream inputStream = httpResponse.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    if (line.charAt(0) <= '9' && line.charAt(0) >= '0') {
                        list.add(parseTxt(line));
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ArrayList<String> convertToList(String s) {
        ArrayList<String> returnList;
        returnList = new ArrayList<String>();
        s = s.replace('|', ',');
        int temp_Position = 0;
        while (temp_Position != -1) {
            temp_Position = s.indexOf(',', temp_Position);
            if (temp_Position == -1) {
                returnList.add(s);
            } else {
                returnList.add(s.substring(0, temp_Position));
            }
            s = s.substring(temp_Position + 1);
        }
        return returnList;
    }
}
