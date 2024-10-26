package gov.lanl.permalink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProxyServlet extends HttpServlet {

    String baseurl = "./";

    String permurl;

    String srv = "info:lanl-repo/svc/permalink";

    String authurl = "http://oppie.lanl.gov/oppie-auth/service";

    private Log mylog;

    static SimpleDateFormat formatter;

    static {
        formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        formatter.setTimeZone(tz);
    }

    public void init() throws ServletException {
        baseurl = getServletConfig().getServletContext().getInitParameter("baseUrl");
        permurl = getServletConfig().getServletContext().getInitParameter("permurl");
        srv = getServletConfig().getServletContext().getInitParameter("srv");
        mylog = LogFactory.getLog(ProxyServlet.class);
        mylog.info("UTC date , permalink ,host,ip");
    }

    public String getAuthInfo(String ip) throws IOException {
        String auth = authurl + "?url_ver=Z39.88-2004&rft_id=info:lanl-repo/oppie&" + "svc_id=info:lanl-repo/svc/oppie/auth-getinst&" + "svc_val_fmt=http://oppie.lanl.gov/openurl/auth-getinst.html&svc.ip=" + ip;
        URL url = new URL(auth);
        HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
        int code = huc.getResponseCode();
        StringBuffer sb = new StringBuffer();
        if (code == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(huc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
        }
        return sb.toString();
    }

    public void dispatch(String strurl, HttpServletResponse res) throws Exception {
        try {
            URL url = new URL(strurl);
            HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
            int code = huc.getResponseCode();
            res.addHeader("Link", huc.getHeaderField("Link"));
            Map m = huc.getHeaderFields();
            if (code == 200) {
                res.setContentType(huc.getContentType());
                InputStream is = huc.getInputStream();
                OutputStream out = res.getOutputStream();
                byte[] bytes = new byte[1024];
                int len;
                while ((len = is.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                out.close();
                is.close();
            } else {
                System.out.println("An error of type " + code + " occurred for:" + strurl);
                throw new Exception("Cannot get " + url.toString());
            }
        } catch (MalformedURLException e) {
            throw new Exception("A MalformedURLException occurred for:" + strurl);
        } catch (IOException e) {
            throw new Exception("An IOException occurred attempting to connect to " + strurl);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {
        String user_agent = request.getHeader("user-agent");
        String host = request.getRemoteHost();
        String ip = request.getRemoteAddr();
        String id = request.getParameter("what");
        String file = request.getRequestURI();
        String _file = file;
        if (request.getQueryString() != null) {
            file += '?' + request.getQueryString();
        }
        URL reconstructedURL = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), file);
        URL _URL = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), _file);
        String url = reconstructedURL.toString();
        String _url = _URL.toString();
        int j = _url.lastIndexOf("/");
        String base = _url.substring(0, j);
        String servicestr = _url.substring(j + 1);
        String service = "info:lanl-repo/svc/" + servicestr;
        String resolver = baseurl + "/" + service + "?url_ver=Z39.88-2004" + "&rft_id=" + java.net.URLEncoder.encode(id, "UTF8");
        System.out.println("resolver url:" + resolver);
        String xml = getAuthInfo(ip);
        System.out.println(xml);
        AuthRequestParser pr = new AuthRequestParser();
        HashMap map = new HashMap();
        try {
            map = pr.processContent(xml);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        String inst = (String) map.get("inst");
        System.out.println("inst" + inst);
        String sfxurl = (String) map.get("sfxurl");
        System.out.println("sfxurl" + sfxurl);
        try {
            if (user_agent.indexOf("Mozilla") >= 0) {
                resolver = resolver + "&svc_id=" + service + "&svc.openurl=false" + "&req_id=human" + "&res_id=" + java.net.URLEncoder.encode(permurl, "UTF8") + "&svc.access=" + inst + "&svc.sfxbaseurl=" + java.net.URLEncoder.encode(sfxurl, "UTF8");
                dispatch(resolver, res);
            } else {
                resolver = resolver + "&svc_id=" + service + "&svc.openurl=false" + "&res_id=" + java.net.URLEncoder.encode(permurl, "UTF8") + "&svc.access=" + inst + "&svc.sfxbaseurl=" + java.net.URLEncoder.encode(sfxurl, "UTF8");
                dispatch(resolver, res);
            }
            mylog.info(formatter.format(new Date()) + "," + url + "," + host + "," + ip);
        } catch (Exception e) {
            throw new ServletException();
        }
    }
}
