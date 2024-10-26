package net.solosky.maplefetion.demo.robot.app;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import net.solosky.maplefetion.demo.robot.App;
import net.solosky.maplefetion.demo.robot.Gateway;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *
 * 天气预报接口，使用webxml.com.cn提供的天气预报服务
 *
 * @author solosky <solosky772@qq.com>
 */
public class WeatherApp implements App {

    @Override
    public boolean accept(String msg) {
        return msg.startsWith("TQ");
    }

    @Override
    public void action(String msg, String uri, Gateway gateway) throws Exception {
        String city = "成都";
        if (msg.indexOf("#") != -1) {
            city = msg.substring(msg.indexOf("#") + 1);
        }
        String url = "http://webservice.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather?theCityCode={city}&theUserID=";
        url = url.replace("{city}", URLEncoder.encode(city, "UTF8"));
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn.getResponseCode() == 200) {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(conn.getInputStream());
            List strings = doc.getRootElement().getChildren();
            String[] sugguestions = getText(strings.get(6)).split("\n");
            StringBuffer buffer = new StringBuffer();
            buffer.append("欢迎使用MapleSMS的天气服务！\n");
            buffer.append("你查询的是 " + getText(strings.get(1)) + "的天气。\n");
            buffer.append(getText(strings.get(4)) + "。\n");
            buffer.append(getText(strings.get(5)) + "。\n");
            buffer.append(sugguestions[0] + "\n");
            buffer.append(sugguestions[1] + "\n");
            buffer.append(sugguestions[7] + "\n");
            buffer.append("感谢你使用MapleSMS的天气服务！祝你愉快！");
            gateway.sendSMS(uri, buffer.toString());
        } else {
            gateway.sendSMS(uri, "对不起，你输入的城市格式有误，请检查后再试~");
        }
    }

    public String getText(Object node) {
        Element e = (Element) node;
        return e.getText();
    }

    @Override
    public String getName() {
        return "天气预报";
    }

    @Override
    public void init() {
    }

    @Override
    public String getFormat() {
        return "TQ#城市 默认为成都";
    }

    @Override
    public String getIntro() {
        return "可以查询城市的当天天气。";
    }
}
