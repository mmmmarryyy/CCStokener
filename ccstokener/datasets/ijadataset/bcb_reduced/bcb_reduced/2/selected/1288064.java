package cn.poco.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import cn.poco.bean.RegisterData;
import cn.poco.bean.Verify;
import cn.poco.util.UrlConnectionUtil;
import cn.poco.util.UrlMatchUtil;
import android.util.Log;
import android.util.Xml;

public class RegisterServiceImpl {

    public Verify getVerData() throws Exception {
        String verifyCodePath = "http://img-m.poco.cn/mypoco/mtmpfile/MobileAPI/User/get_code_id.php";
        String verifyImge = "http://img-m.poco.cn/mypoco/mtmpfile/MobileAPI/User/get_code_image.php?verifyid=#a";
        String verifyCode = "";
        verifyCodePath = UrlMatchUtil.matchUrl(verifyCodePath);
        verifyImge = UrlMatchUtil.matchUrl(verifyImge);
        Log.i("impl", verifyCodePath);
        Log.i("impl", verifyImge);
        verifyCode = inputStream2String(UrlConnectionUtil.getRequest(verifyCodePath));
        Verify verify = new Verify();
        verify.setVerifyCode(verifyCode);
        verify.setVerifyImage(verifyImge.replace("#a", verifyCode));
        return verify;
    }

    public RegisterData register(String name, String pass, String verifyId, String verifyCode) throws Exception {
        String path = "http://img-m.poco.cn/mypoco/mtmpfile/MobileAPI/User/registe.php";
        String encoding = "UTF-8";
        Map<String, String> params = new HashMap<String, String>();
        params.put("email", name);
        params.put("pass", pass);
        params.put("verify_id", verifyId);
        params.put("verify_code", verifyCode);
        path = UrlMatchUtil.matchUrl(path);
        Log.i("impl", path);
        return sendPostRequest(path, params, encoding);
    }

    public RegisterData sendPostRequest(String path, Map<String, String> params, String encoding) throws Exception {
        StringBuilder sb = new StringBuilder("");
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(), encoding)).append('&');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        byte[] data = sb.toString().getBytes();
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5 * 1000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        OutputStream outStream = conn.getOutputStream();
        outStream.write(data);
        outStream.flush();
        outStream.close();
        if (conn.getResponseCode() == 200) {
            InputStream inputStream = conn.getInputStream();
            return parseXML(inputStream);
        }
        return null;
    }

    public RegisterData parseXML(InputStream inputStream) throws Exception {
        RegisterData registerData = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, "UTF-8");
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            switch(event) {
                case XmlPullParser.START_DOCUMENT:
                    registerData = new RegisterData();
                    break;
                case XmlPullParser.START_TAG:
                    if ("result".equals(parser.getName())) {
                        registerData.setResult(parser.nextText());
                    }
                    if ("message".equals(parser.getName())) {
                        registerData.setMessage(parser.nextText());
                    }
                    if ("poco-id".equals(parser.getName())) {
                        registerData.setPocoId(parser.nextText());
                    }
                    break;
            }
            event = parser.next();
        }
        inputStream.close();
        return registerData;
    }

    public String inputStream2String(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[1024];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}
