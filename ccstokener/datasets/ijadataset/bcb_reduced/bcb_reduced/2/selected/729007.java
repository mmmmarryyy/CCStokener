package commons.httpclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author Swang
 */
public class EmailImporter126 {

    private String dologinUrl = "http://reg.163.com/login.jsp?type=1&product=mail126&url=http://entry.mail.126.com/cgi/ntesdoor?hid%3D10010102%26lightweight%3D1%26language%3D0%26style%3D3";

    private String contactUrl = "http://webmail.mail.126.com/jy3/address/addrlist.jsp?sid=%sid%&gid=all";

    private String email = null;

    private String password = null;

    DefaultHttpClient httpclient = new DefaultHttpClient();

    public EmailImporter126(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void doLogin() {
        try {
            System.out.println("Protocol Version: " + httpclient.getParams().getParameter(CoreProtocolPNames.PROTOCOL_VERSION));
            System.out.println("Cookie Policy: " + httpclient.getParams().getParameter(ClientPNames.COOKIE_POLICY));
            HttpHost proxy = new HttpHost("127.0.0.1", 8888);
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("style", "3"));
            formparams.add(new BasicNameValuePair("username", "testimporter1@126.com"));
            formparams.add(new BasicNameValuePair("password", "atrip42"));
            Map<String, Object> resultMap = doPost(dologinUrl, formparams);
            String dologinPageRtn = (String) resultMap.get("content");
            System.out.println("dologinPageRtn: " + dologinPageRtn);
            String redirectUrl1 = getJSRedirectLocation(dologinPageRtn);
            String loginRedirectPage1 = doGet(redirectUrl1);
            System.out.println("loginRedirectPage1: " + loginRedirectPage1);
            String redirectUrl2 = getJSRedirectLocation(loginRedirectPage1);
            String loginRedirectPage2 = doGet(redirectUrl2);
            System.out.println("loginRedirectPage2: " + loginRedirectPage2);
            String sid = getCookieValue("Coremail.sid");
            System.out.println("Coremail.sid:" + sid);
            String contactListUrl = contactUrl.replace("%sid%", sid);
            String contactListPage = doGet(contactListUrl);
            System.out.println("contactListPage: " + contactListPage);
            DOMParser parser = new DOMParser();
            InputSource is = new InputSource(new ByteArrayInputStream(contactListPage.getBytes("GBK")));
            is.setEncoding("GBK");
            parser.parse(is);
            NodeList nodes = parser.getDocument().getElementsByTagName("td");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getFirstChild().getNodeName().equalsIgnoreCase("input")) {
                    Node nextSib = node.getNextSibling();
                    String username = nextSib.getFirstChild().getFirstChild().getNodeValue();
                    nextSib = nextSib.getNextSibling();
                    String email = nextSib.getFirstChild().getFirstChild().getNodeValue();
                    System.out.println(username + " : " + email);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    public Map<String, Object> doPost(String postUrl, List<NameValuePair> params) throws UnsupportedEncodingException, IOException {
        UrlEncodedFormEntity requestEntity = new UrlEncodedFormEntity(params, "UTF-8");
        HttpPost httppost = new HttpPost(postUrl);
        httppost.setEntity(requestEntity);
        HttpResponse response = httpclient.execute(httppost);
        System.out.println("Status line: " + response.getStatusLine());
        System.out.println("Header: " + getResponseStr(response));
        System.out.println("Cookie: " + getCookiesStr());
        HttpEntity entity = response.getEntity();
        Map<String, Object> resultMap = new HashMap();
        resultMap.put("content", EntityUtils.toString(entity));
        return resultMap;
    }

    public String doGet(String getUrl) throws IOException {
        HttpGet httpget = new HttpGet(getUrl);
        System.out.println("Get Url:" + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        System.out.println("Status line: " + response.getStatusLine());
        System.out.println("Header: " + getResponseStr(response));
        System.out.println("Cookie: " + getCookiesStr());
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    protected Cookie createCookie(String name, String value, String path, String domain, Date expiry, boolean secure) {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setPath(path);
        cookie.setExpiryDate(expiry);
        cookie.setDomain(domain);
        cookie.setSecure(secure);
        return cookie;
    }

    protected String getCookieValue(String name) {
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equalsIgnoreCase(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String getJSRedirectLocation(String content) {
        String name = "window.location.replace(\"";
        int index = content.indexOf(name) + name.length();
        content = content.substring(index);
        content = content.substring(0, content.indexOf("\""));
        return content;
    }

    public String getResponseStr(HttpResponse response) {
        StringBuilder sb = new StringBuilder();
        HeaderIterator it = response.headerIterator();
        while (it.hasNext()) {
            sb.append(it.next()).append("\n");
        }
        return sb.toString();
    }

    public String getCookiesStr() {
        StringBuilder sb = new StringBuilder();
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            return null;
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                sb.append(cookies.get(i).toString()).append("\n");
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        EmailImporter126 emailImporter = new EmailImporter126("magicshuai@126.com", "2025515ws");
        emailImporter.doLogin();
    }
}
