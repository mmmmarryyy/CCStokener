package com.demo.vo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GetGoogleWeather {

    /**
     * 
     * @param cityName
     *            注意weather那写入城市的拼音转化一下就行， 打开之后是XML格式的然后再提取。
     * @return
     */
    public String getWeather(String cityName, String fileAddr) {
        try {
            URL url = new URL("http://www.google.com/ig/api?hl=zh_cn&weather=" + cityName);
            InputStream inputstream = url.openStream();
            String s, str;
            BufferedReader in = new BufferedReader(new InputStreamReader(inputstream));
            StringBuffer stringbuffer = new StringBuffer();
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileAddr), "utf-8"));
            while ((s = in.readLine()) != null) {
                stringbuffer.append(s);
            }
            str = new String(stringbuffer);
            out.write(str);
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(fileAddr);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String str = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            NodeList nodelist1 = (NodeList) doc.getElementsByTagName("forecast_conditions");
            NodeList nodelist2 = nodelist1.item(0).getChildNodes();
            str = nodelist2.item(4).getAttributes().item(0).getNodeValue() + ",temperature:" + nodelist2.item(1).getAttributes().item(0).getNodeValue() + "℃-" + nodelist2.item(2).getAttributes().item(0).getNodeValue() + "℃";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static void main(String args[]) {
        GetGoogleWeather ggw = new GetGoogleWeather();
        String cityName = "changsha";
        String fileAddr = "C:/changsha.xml";
        String temperature = ggw.getWeather(cityName, fileAddr);
        Date nowDate = new Date();
        DateFormat dateformat = DateFormat.getDateInstance();
        String today = dateformat.format(nowDate);
        System.out.println(today + " " + cityName + "的天气情况是：" + temperature);
    }
}
