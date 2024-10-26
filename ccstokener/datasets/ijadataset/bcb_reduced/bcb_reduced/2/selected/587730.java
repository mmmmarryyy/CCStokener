package com.diipo.weibo.utils;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HttpRequester {

    /**

* ֱ��ͨ��HTTPЭ���ύ��ݵ�������,ʵ��������?�ύ����:

* <FORM METHOD=POST ACTION="http://192.168.0.200:8080/ssi/fileload/test.do" enctype="multipart/form-data">

<INPUT TYPE="text" NAME="name">

<INPUT TYPE="text" NAME="id">

<input type="file" name="imagefile"/>

<input type="file" name="zip"/>

</FORM>

* @param actionUrl �ϴ�·��(ע������ʹ��localhost��127.0.0.1�����·�����ԣ���Ϊ���ָ���ֻ�ģ�����������ʹ��http://www.cnblogs.com/guoshiandroid��http://192.168.1.10:8080�����·������)

* @param params ������� keyΪ������,valueΪ����ֵ

* @param file �ϴ��ļ�

*/
    public static String post(String actionUrl, Map<String, String> params, FormFile[] files) {
        try {
            String BOUNDARY = "---------7d4a6d158c9";
            String MULTIPART_FORM_DATA = "multipart/form-data";
            URL url = new URL(actionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("--");
                sb.append(BOUNDARY);
                sb.append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                sb.append(entry.getValue());
                sb.append("\r\n");
            }
            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.write(sb.toString().getBytes());
            for (FormFile file : files) {
                StringBuilder split = new StringBuilder();
                split.append("--");
                split.append(BOUNDARY);
                split.append("\r\n");
                split.append("Content-Disposition: form-data;name=\"" + file.getFilname() + "\";filename=\"" + file.getFilname() + "\"\r\n");
                split.append("Content-Type: " + file.getContentType() + "\r\n\r\n");
                outStream.write(split.toString().getBytes());
                if (file.getInStream() != null) {
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = file.getInStream().read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                    }
                    file.getInStream().close();
                } else {
                    outStream.write(file.getData(), 0, file.getData().length);
                }
                outStream.write("\r\n".getBytes());
            }
            byte[] end_data = ("--" + BOUNDARY + "--\r\n").getBytes();
            outStream.write(end_data);
            outStream.flush();
            int cah = conn.getResponseCode();
            if (cah != 200) throw new RuntimeException("����urlʧ��");
            InputStream is = conn.getInputStream();
            int ch;
            StringBuilder b = new StringBuilder();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            outStream.close();
            conn.disconnect();
            return b.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**

* �ύ��ݵ�������

* @param actionUrl �ϴ�·��(ע������ʹ��localhost��127.0.0.1�����·�����ԣ���Ϊ���ָ���ֻ�ģ�����������ʹ��http://www.cnblogs.com/guoshiandroid��http://192.168.1.10:8080�����·������)

* @param params ������� keyΪ������,valueΪ����ֵ

* @param file �ϴ��ļ�

*/
    public static String post(String actionUrl, Map<String, String> params, FormFile file) {
        return post(actionUrl, params, new FormFile[] { file });
    }

    public static byte[] postFromHttpClient(String path, Map<String, String> params, String encode) throws Exception {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        HttpPost httppost = new HttpPost(path);
        httppost.setEntity(entity);
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httppost);
        return StreamTool.readInputStream(response.getEntity().getContent());
    }

    /**

* ��������

* @param path ����·��

* @param params ������� keyΪ������� valueΪ����ֵ

* @param encode �������ı���

*/
    public static byte[] post(String path, Map<String, String> params, String encode) throws Exception {
        StringBuilder parambuilder = new StringBuilder("");
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parambuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), encode)).append("&");
            }
            parambuilder.deleteCharAt(parambuilder.length() - 1);
        }
        byte[] data = parambuilder.toString().getBytes();
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        conn.setRequestProperty("Connection", "Keep-Alive");
        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
        outStream.write(data);
        outStream.flush();
        outStream.close();
        if (conn.getResponseCode() == 200) {
            return StreamTool.readInputStream(conn.getInputStream());
        }
        return null;
    }
}
