package com.frame.util.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.dom4j.Document;
import org.dom4j.Element;
import com.pioneer.app.comm.ApplicationPathMg;
import com.pioneer.app.util.Dom4jUtil;

public class AppConfig {

    private String appCode = null;

    private String ecode = "beijingaoke";

    private String epassword = null;

    private String initUrl = null;

    private String SMS_SEND_URL = null;

    private String sendUrl = null;

    private String fullUrl = null;

    private AppConfig() {
        init();
    }

    public void init() {
        String configFile = ApplicationPathMg.getInstance().getWebRootPath() + "/appconfig.xml";
        String configFile1 = ApplicationPathMg.getInstance().getWebRootPath() + "/appconfig1.xml";
        CryptFile cryptInst = new CryptFile();
        cryptInst.setPrvKey(ApplicationPathMg.getInstance().getWebRootPath() + "/privateKey.scrpt");
        try {
            cryptInst.decryptFile(configFile, configFile1);
            Document doc = Dom4jUtil.getDocFromFile(configFile1);
            Element root = doc.getRootElement();
            appCode = root.valueOf("@code");
            Element ecodeElt = (Element) root.selectSingleNode("ecode");
            ecode = ecodeElt.getTextTrim();
            Element pwdElt = (Element) root.selectSingleNode("epassword");
            epassword = pwdElt.getTextTrim();
            Element initUrlElt = (Element) root.selectSingleNode("initurl");
            initUrl = initUrlElt.getTextTrim();
            File file1 = new File(configFile1);
            if (file1.exists()) {
                file1.delete();
            }
            File file = new File(configFile);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void netInit() {
        try {
            URL url = new URL(this.initUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.connect();
            InputStream in = conn.getInputStream();
            Document rtnDoc = Dom4jUtil.getDocFromStream(in);
            processInitInfor(rtnDoc);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processInitInfor(Document doc) {
        Element sendurlElt = (Element) doc.selectSingleNode("/sms/sendurl");
        if (null != sendurlElt) this.sendUrl = sendurlElt.getTextTrim();
        Element fullurlElt = (Element) doc.selectSingleNode("/sms/fullurl");
        if (null != fullurlElt) this.fullUrl = fullurlElt.getTextTrim();
    }

    public static AppConfig getInst() {
        return _inst;
    }

    private static AppConfig _inst = new AppConfig();

    public String getAppCode() {
        return appCode;
    }

    public String getEcode() {
        return ecode;
    }

    public String getEpassword() {
        return epassword;
    }

    public String getInitUrl() {
        return initUrl;
    }

    public static void main(String[] args) {
        System.out.println("appcode=" + AppConfig.getInst().getAppCode());
    }

    public String getSendUrl() {
        return sendUrl;
    }

    public void setSendUrl(String sendUrl) {
        this.sendUrl = sendUrl;
    }

    public void setInitUrl(String initUrl) {
        this.initUrl = initUrl;
        netInit();
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }
}
