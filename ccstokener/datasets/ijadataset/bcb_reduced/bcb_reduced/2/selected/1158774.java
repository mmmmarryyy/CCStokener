package cn.imgdpu.fetion;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.protocol.HTTP;

class FetionInit {

    String sipc_proxy = "", ssi_app_sign_in = "", ssi_app_sign_out = "", uri = "", sid = "";

    static String sysConfigUrl = "http://nav.fetion.com.cn/nav/getsystemconfig.aspx";

    public static ArrayList<String> centerStr(String sourceStr, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(sourceStr);
        ArrayList<String> list = new ArrayList<String>();
        if (matcher.find()) for (int i = 0; i <= matcher.groupCount(); i++) list.add(matcher.group(i));
        return list;
    }

    public String postUrl(String urlStr, String sendData) throws IOException {
        URL url;
        String htmlData = "";
        url = new URL(urlStr);
        String content = sendData;
        URLConnection uc = url.openConnection();
        uc.setDoOutput(true);
        uc.setUseCaches(false);
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Content-Length", "" + content.length());
        HttpURLConnection hc = (HttpURLConnection) uc;
        hc.setRequestMethod("POST");
        OutputStream os = uc.getOutputStream();
        os.write(content.getBytes());
        os.close();
        InputStream is = uc.getInputStream();
        BufferedReader bufReader = new BufferedReader(new InputStreamReader(is, HTTP.ISO_8859_1));
        StringBuffer sb = new StringBuffer();
        String tempStr;
        while ((tempStr = bufReader.readLine()) != null) {
            sb.append(tempStr).append("\r\n");
        }
        is.close();
        bufReader.close();
        htmlData = new String(sb.toString().getBytes(HTTP.ISO_8859_1), "utf-8");
        return htmlData;
    }

    public ArrayList<String> InitAction(String phone, String passwd) throws IOException {
        String result;
        String sysConfigArg = "<config><user mobile-no=\"" + phone + "\" /><client type=\"PC\" version=\"3.3.0370\" platform=\"W5.1\" /><servers version=\"0\" /><service-no version=\"12\" /><parameters version=\"15\" /><hints version=\"13\" /><http-applications version=\"14\" /><client-config version=\"17\" /></config>";
        ArrayList<String> loginInfo = new ArrayList<String>();
        result = postUrl(sysConfigUrl, sysConfigArg);
        sipc_proxy = centerStr(result, "<sipc-proxy>(.*?)</sipc-proxy>").get(1);
        ssi_app_sign_in = centerStr(result, "<ssi-app-sign-in>(.*?)</ssi-app-sign-in>").get(1);
        ssi_app_sign_out = centerStr(result, "<ssi-app-sign-out>(.*?)</ssi-app-sign-out>").get(1);
        ssi_app_sign_in = ssi_app_sign_in.replace("https", "http");
        result = postUrl(ssi_app_sign_in, "mobileno=" + phone + "&pwd=" + passwd + "");
        sid = centerStr(result, "<user uri=\"sip:([0-9]+)@fetion.com.cn;p=[0-9]+\"").get(1);
        loginInfo.add(sipc_proxy);
        loginInfo.add(ssi_app_sign_in);
        loginInfo.add(ssi_app_sign_out);
        loginInfo.add(sid);
        return loginInfo;
    }
}
