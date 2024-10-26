package net.sipvip.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sipvip.SevCommon.AhtmlLinks;
import net.sipvip.SevCommon.CsvfileReader;
import net.sipvip.SevCommon.DomainCsvReader;
import nl.bitwalker.useragentutils.UserAgent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StartServ extends HttpServlet {

    private static final Logger log = Logger.getLogger(StartServ.class.getName());

    private static String domain;

    private static String pathinfo;

    private static String locale;

    private static String links;

    private static String jsonContectResult;

    private static String title;

    private static String keyword;

    private static String link;

    private static StringBuffer strCont = new StringBuffer();

    public void process(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException, JSONException {
        String UrlStr = req.getRequestURL().toString();
        URL domainurl = new URL(UrlStr);
        domain = domainurl.getHost();
        pathinfo = req.getPathInfo();
        String user_agent = req.getHeader("user-agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(user_agent);
        String browser = userAgent.getBrowser().getName();
        String[] shot_domain_array = domain.split("\\.");
        String shot_domain = shot_domain_array[1] + "." + shot_domain_array[2];
        JSONArray jsonarray = CsvfileReader.CsvfileReader("www." + shot_domain + ".csv");
        JSONArray jsondomain = DomainCsvReader.DoaminCsvReader("domainpar.csv", domain);
        JSONObject domainObj = jsondomain.getJSONObject(0);
        String title = domainObj.getString("title");
        String charset = domainObj.getString("charset");
        locale = domainObj.getString("locale");
        links = domainObj.getString("links");
        String theme = domainObj.getString("theme");
        String facebookid = domainObj.getString("facebookid");
        String google = domainObj.getString("google");
        String slot_r = domainObj.getString("slot_r");
        String slot_up = domainObj.getString("slot_up");
        String slot_s = domainObj.getString("slot_s");
        String slot_l = domainObj.getString("slot_l");
        JSONArray jsonahtmllinks = AhtmlLinks.AhtmlLinks(links + ".csv");
        int ahtmllinkscount = jsonahtmllinks.length();
        if (browser.equalsIgnoreCase("Robot/Spider") || browser.equalsIgnoreCase("Lynx") || browser.equalsIgnoreCase("Downloading Tool")) {
            forSpidersReq("http://ntlserv0.appspot.com/contentgen");
            parsJsonSpidersRespost(jsonContectResult);
            createHtmlOut(req, resp);
        } else {
            if (!browser.equalsIgnoreCase("Unknown")) {
                log.info("clientBr " + browser);
            }
            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("<!doctype html>");
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta http-equiv='content-type' content='text/html; charset=" + charset + "'>");
            out.println("<meta name=\"gwt:property\" content=\"locale=" + locale + "\">");
            out.println("<link type='text/css' rel='stylesheet' href='/wwwvakuutusme/standard/standard.css'>");
            out.println("<link type=\"text/css\" rel=\"stylesheet\" href=\"/Wwwvakuutusme.css\">");
            out.println("<title>" + title + "</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div id=\"start\"></div>");
            out.println("<div id=\"seo_content\">");
            BufferedReader bufRdr = new BufferedReader(new InputStreamReader(new FileInputStream("www." + shot_domain + ".html"), "UTF8"));
            String line = null;
            while ((line = bufRdr.readLine()) != null) {
                String lineinutf8 = new String(line);
                out.println(lineinutf8);
            }
            bufRdr.close();
            if (!domain.equals("127.0.0.1")) {
                out.println("<script type=\"text/javascript\"><!--");
                out.println("google_ad_client = \"" + google + "\";");
                out.println("google_ad_slot = \"" + slot_r + "\";");
                out.println("google_ad_width = 160;");
                out.println("google_ad_height = 600;");
                out.println("//-->");
                out.println("</script>");
                out.println("<script type=\"text/javascript\"");
                out.println("src=\"http://pagead2.googlesyndication.com/pagead/show_ads.js\">");
                out.println("</script>");
            }
            for (int i = 0; i < jsonarray.length(); ++i) {
                JSONObject rec = jsonarray.getJSONObject(i);
                String tab = rec.getString("tab");
                out.println("<h2>" + tab + "</h2>");
                JSONArray rsscontent = rec.getJSONArray("linkcontext");
                for (int k = 0; k < rsscontent.length(); k++) {
                    JSONObject rssobj = rsscontent.getJSONObject(k);
                    String jtitle = rssobj.getString("title");
                    out.println("<b>" + jtitle + "</b>");
                    String description = rssobj.getString("description");
                    out.println("<p>" + description + "</p>");
                }
                for (int n = 0; n < ahtmllinkscount; n++) {
                    JSONObject alinksObj = jsonahtmllinks.getJSONObject(n);
                    if (n == i) {
                        String alink = alinksObj.getString("link");
                        String atitle = alinksObj.getString("title");
                        out.println("<a href=http://" + alink + ">" + atitle + "</a>");
                    }
                }
                out.println("------------------------------------------");
            }
            out.println("<script type=\"text/javascript\" language=\"javascript\" src=\"/wwwvakuutusme/wwwvakuutusme.nocache.js\"></script>");
            out.println("<script type='text/javascript'>");
            out.println("var jsondomain =" + jsondomain.toString() + ";");
            out.println("var jsoncontext =" + jsonarray.toString() + ";");
            out.println("</script>");
            out.println("</div>");
            out.println("<div id='fb-root'></div>");
            out.println("<script>");
            out.println("window.fbAsyncInit = function() {");
            out.println("FB.init({appId: '" + facebookid + "', status: true, cookie: true,xfbml: true});};");
            out.println("(function() {");
            out.println("var e = document.createElement('script'); e.async = true;");
            out.println("e.src = document.location.protocol +");
            out.println("'//connect.facebook.net/" + locale + "/all.js';");
            out.println("document.getElementById('fb-root').appendChild(e);");
            out.println("}());");
            out.println("</script>");
            out.println("</body></html>");
            out.close();
        }
    }

    public void forSpidersReq(String urlstr) throws IOException {
        URL url = new URL(urlstr);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("pathinfo", pathinfo);
        conn.setRequestProperty("domain", domain);
        conn.setRequestProperty("locale", locale);
        conn.setRequestProperty("links", links);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            StringBuffer response = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            jsonContectResult = response.toString();
        } catch (SocketTimeoutException e) {
            log.severe("SoketTimeout NO!! RC " + e.getMessage());
        } catch (Exception e) {
            log.severe("Except Rescue Start " + e.getMessage());
        } finally {
        }
    }

    public void parsJsonSpidersRespost(String jsonstr) {
        try {
            JSONObject jso = new JSONObject(jsonstr);
            String quant = jso.get("quant").toString();
            title = jso.get("title").toString();
            keyword = jso.get("keyword").toString();
            link = jso.get("link").toString();
            int q = Integer.parseInt(quant);
            strCont.setLength(0);
            for (int i = 0; i < q; i++) {
                String ln = jso.get("" + i).toString();
                strCont.append(ln);
            }
        } catch (JSONException e) {
            log.severe("NO rsq!!! " + e.getMessage());
        }
    }

    public void createHtmlOut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String endHtml = "/";
        Random rand2 = new Random();
        int randI2 = rand2.nextInt(2);
        if (randI2 == 0) {
            endHtml = ".html";
        }
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<title>" + title + "</title>");
        out.println("<head>");
        out.println("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
        out.println("<meta name='description' content='" + title.toUpperCase() + " " + keyword + "'/>");
        out.println("</head>");
        out.println("<body>");
        if (pathinfo.equals("/")) {
            out.println("<p><a href=/" + keyword + endHtml + "><b><i>" + keyword + "</i></b></a></p>");
        } else {
            out.println("<p><a href=/" + title.toLowerCase() + "/" + keyword + endHtml + "><b><i>" + keyword + "</i></b></a></p>");
        }
        out.println("<h1>" + title + "</h1>");
        out.print(strCont.toString().substring(0, 1).toUpperCase() + strCont.substring(1) + ".");
        if (link.length() > 0) {
            out.println("<p><a href=http://www." + link + "/>" + keyword.toUpperCase() + " " + title + "</a></p>");
        }
        out.println("</body></html>");
        out.close();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            process(req, resp);
        } catch (JSONException e) {
            log.severe(e.getMessage());
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            process(req, resp);
        } catch (JSONException e) {
            log.severe(e.getMessage());
        }
    }
}
