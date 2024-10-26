package com.linktone.market.client;

import android.util.Log;
import com.linktone.market.client.bean.AppInfo;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author mxf <a href="mailto:maxuefengs@gmail.com">mxf</a>
 *         11-8-18 ����9:34
 * @since version 1.0
 */
public class Tools {

    public static List<AppInfo> getUpdateAbleApp(Map<String, Integer> requestMap) {
        return null;
    }

    public static AppInfo getInfoById(int id) {
        return null;
    }

    public static List<AppInfo> getApps(String strUrl) {
        byte[] bytes = null;
        Map headers = new HashMap();
        List<AppInfo> apps = new ArrayList<AppInfo>();
        try {
            bytes = HttpUtils.webGet(new URL(strUrl), headers, 300000, 60000);
            Log.v("appsxml", new String(bytes));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(new ByteArrayInputStream(bytes));
            Element root = dom.getDocumentElement();
            NodeList nodeList = root.getElementsByTagName("app");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attrs = node.getAttributes();
                AppInfo app = new AppInfo();
                app.setId(Integer.parseInt(getAttrValue(attrs, "id")));
                app.setRating(Integer.parseInt(getAttrValue(attrs, "score")));
                app.setAppName(getAttrValue(attrs, "name"));
                app.setPackageName(getAttrValue(attrs, "pk_name"));
                app.setVersionCode(Integer.parseInt(getAttrValue(attrs, "v_code")));
                app.setVersionName(getAttrValue(attrs, "v_name"));
                app.setIconUrl(getAttrValue(attrs, "icon"));
                app.setTotalSize(Integer.parseInt(getAttrValue(attrs, "size")));
                app.setUrl("http://sc.hiapk.com/Download.aspx?aid=" + getAttrValue(attrs, "id"));
                app.setTime(getAttrValue(attrs, "time"));
                app.setDownloads(getAttrValue(attrs, "downloads"));
                app.setRating(Float.parseFloat(getAttrValue(attrs, "score")));
                apps.add(app);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v("apps", apps.toString());
        return apps;
    }

    public static List<AppInfo> getApps(String columnId, String start, String count) {
        String strUrl = "http://116.213.116.29:8080/Client?type=list&clnId=" + columnId + "&start=" + start + "&count=" + count;
        return getApps(strUrl);
    }

    private static String getAttrValue(NamedNodeMap attrs, String name) {
        return attrs.getNamedItem(name).getTextContent();
    }

    public static void main(String[] args) {
        System.out.println(getApps("", "1", "10"));
    }
}

class HttpUtils {

    public static byte[] webGet(URL url, java.util.Map headers, int connectTimeout, int readTimeout) throws IOException {
        if (headers == null) headers = new java.util.HashMap();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            String jdkV = System.getProperty("java.vm.version");
            if (jdkV != null && jdkV.indexOf("1.5") != -1) {
                Class clazz = conn.getClass();
                try {
                    java.lang.reflect.Method method = clazz.getMethod("setConnectTimeout", new Class[] { int.class });
                    method.invoke(conn, new Object[] { new Integer(connectTimeout) });
                    method = clazz.getMethod("setReadTimeout", new Class[] { int.class });
                    method.invoke(conn, new Object[] { new Integer(readTimeout) });
                } catch (Exception ec) {
                    ec.printStackTrace();
                }
            } else {
                System.setProperty("sun.net.client.defaultConnectTimeout", "" + connectTimeout);
                System.setProperty("sun.net.client.defaultReadTimeout", "" + readTimeout);
            }
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                String key = (String) me.getKey();
                String value = (String) me.getValue();
                if (key != null && value != null) {
                    conn.setRequestProperty(key, value);
                }
            }
            String contentType = conn.getContentType();
            String encoding = null;
            if (contentType != null && contentType.toLowerCase().indexOf("charset") > 0) {
                int k = contentType.toLowerCase().indexOf("charset");
                if (contentType.length() > k + 7) {
                    String sss = contentType.substring(k + 7).trim();
                    k = sss.indexOf("=");
                    if (k >= 0 && sss.length() > k + 1) {
                        encoding = sss.substring(k + 1).trim();
                        if (encoding.indexOf(";") > 0) {
                            encoding = encoding.substring(0, encoding.indexOf(";")).trim();
                        }
                    }
                }
            }
            headers.clear();
            int k = 0;
            String feildValue = null;
            while ((feildValue = conn.getHeaderField(k)) != null) {
                String key = conn.getHeaderFieldKey(k);
                k++;
                if (key != null) {
                    headers.put(key, feildValue);
                }
            }
            headers.put("Response-Code", new Integer(conn.getResponseCode()));
            headers.put("Response-Message", conn.getResponseMessage());
            java.io.InputStream bis = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte bytes[] = new byte[1024];
            int n = 0;
            while ((n = bis.read(bytes)) > 0) {
                out.write(bytes, 0, n);
            }
            bis.close();
            bytes = out.toByteArray();
            if (encoding == null) {
                try {
                    for (int i = 0; i < 64 && i < bytes.length - 2; i++) {
                        if (bytes[i] == '?' && bytes[i + 1] == '>') {
                            String s = new String(bytes, 0, i);
                            if (s.indexOf("encoding") > 0) {
                                s = s.substring(s.indexOf("encoding") + 8);
                                if (s.indexOf("=") >= 0) {
                                    s = s.substring(s.indexOf("=") + 1).trim();
                                    if (s.charAt(0) == '"') {
                                        s = s.substring(1);
                                    }
                                    if (s.indexOf("\"") > 0) {
                                        encoding = s.substring(0, s.indexOf("\""));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (encoding == null) {
                encoding = "UTF-8";
            }
            headers.put("Encoding", encoding);
            return bytes;
        } catch (IOException e) {
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
