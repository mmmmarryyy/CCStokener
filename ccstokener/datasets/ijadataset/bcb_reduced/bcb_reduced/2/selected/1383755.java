package indiji.web;

import indiji.io.IO;
import indiji.io.Log;
import indiji.struct.Tuple;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Source;

/**
 * A class for crawling websites, downloading web-resources,etc.
 * @author Pascal Lehwark
 *
 */
public class Spider {

    private long lastwebaccess = 0;

    private HashMap<String, String> cookies;

    private String lasturl = "";

    private boolean useCookies = true;

    /**
	 * Default Constructor.
	 */
    public Spider() {
        cookies = new HashMap<String, String>();
    }

    /**
	 * Get the Website for the given url as a string, next request will
	 * block until given delay has passed.
	 * @param url The url to download from
	 * @param delay The delay in millis to wait before next request.
	 * @return The website as a string.
	 */
    public String getWebsite(String url, int delay) {
        try {
            long now = new Date().getTime();
            long waittime = delay - (now - lastwebaccess);
            if (waittime > 0) IO.sleep((int) waittime);
            URL u = new URL(url);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setReadTimeout(30000);
            con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; InfoPath.1; .NET CLR 2.0.50727)");
            if (useCookies) writeCookies(con);
            con.connect();
            if (useCookies) readCookies(con);
            InputStream in;
            String enc = con.getHeaderField("Content-Encoding");
            if (enc != null && enc.equals("gzip")) {
                System.out.println("Will deflate");
                in = new GZIPInputStream(con.getInputStream());
            } else in = con.getInputStream();
            StringWriter sw = new StringWriter();
            int c = in.read();
            while (c >= 0) {
                sw.write(c);
                c = in.read();
            }
            sw.close();
            in.close();
            con.disconnect();
            lastwebaccess = new Date().getTime();
            lasturl = url;
            return sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.err("Spider: Error in getWebsite(" + url + "," + delay + ")");
        }
        return "";
    }

    /**
	 * Get the Website for the given url as a string, next request will
	 * block until given delay has passed.
	 * @param url The url to download from
	 * @param delay The delay in millis to wait before next request.
	 * @return The website as a string.
	 */
    public String getWebsite(String url, int delay, boolean cache) {
        try {
            if (!cache) return getWebsite(url, delay);
            File d = new File("webcache");
            if (!d.exists()) d.mkdirs();
            File f = new File(d.getAbsolutePath() + "/" + URLEncoder.encode(url, "UTF-8"));
            if (f.exists()) return IO.readFile(f);
            String p = getWebsite(url, delay);
            IO.writeFile(f, p);
            return p;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Get the Website for the given url using a POST-Request, next request will
	 * block until given delay has passed.
	 * @param url The url to download from
	 * @param delay The delay in millis to wait before next request.
	 * @param content The POST-Content
	 * @param cookieread Allow reading cookies
	 * @return The website as a string.
	 */
    public String getPostWebsite(String url, int delay, Vector<Tuple<String, String>> content, boolean cookieread) {
        try {
            long now = new Date().getTime();
            long waittime = delay - (now - lastwebaccess);
            System.out.print("wait " + waittime + "ms..");
            if (waittime > 0) Thread.sleep(waittime);
            StringBuilder sb = new StringBuilder(10000);
            java.net.URL u = new java.net.URL(url);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            String query = "";
            for (Tuple<String, String> e : content) query += URLEncoder.encode(e.getKey().trim(), "UTF-8").trim() + "=" + URLEncoder.encode(e.getValue().trim(), "UTF-8").trim() + "&";
            query = query.substring(0, query.length() - 1);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setAllowUserInteraction(false);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.11) Gecko/2009060308 Ubuntu/9.04 (jaunty) Firefox/3");
            con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            con.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            con.setRequestProperty("Referer", lasturl);
            writeCookies(con);
            DataOutputStream dout = new DataOutputStream(con.getOutputStream());
            dout.writeBytes(query);
            dout.flush();
            dout.close();
            con.connect();
            System.out.println(con.getResponseCode() + "..");
            if (cookieread) readCookies(con);
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            InputStream in = con.getInputStream();
            int c = in.read();
            while (c >= 0) {
                sb.append((char) c);
                c = in.read();
            }
            in.close();
            lastwebaccess = new Date().getTime();
            sb.trimToSize();
            reader.close();
            con.disconnect();
            lasturl = url;
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void readCookies(HttpURLConnection con) {
        try {
            for (Entry<String, List<String>> e : con.getHeaderFields().entrySet()) {
                if (e.getKey() != null && e.getKey().equals("Set-Cookie")) {
                    for (String c : e.getValue()) {
                        String tmp[] = c.split(";");
                        if (tmp.length > 0) {
                            String ckv[] = tmp[0].split("=");
                            if (ckv.length == 2) cookies.put(ckv[0], ckv[1]);
                        }
                    }
                    return;
                }
            }
        } catch (Exception e) {
            Log.err("Error reading Cookies (" + e.getMessage() + ") from " + con);
        }
    }

    private void writeCookies(HttpURLConnection con) {
        String s = "";
        for (Entry<String, String> e : cookies.entrySet()) s += e.getKey() + "=" + e.getValue() + "; ";
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 2);
            con.setRequestProperty("Cookie", s);
        }
    }

    /**
	 * True, if cookies are used.
	 * @return
	 */
    public boolean isUseCookies() {
        return useCookies;
    }

    /**
	 * Set false to disable reading and writing of cookies (default is true).
	 * @param useCookies
	 */
    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
    }

    /**
	 * Get the cookies.
	 * @return
	 */
    public HashMap<String, String> getCookies() {
        return cookies;
    }

    /**
	 * Set the cookies.
	 * @param cookies
	 */
    public void setCookies(HashMap<String, String> cookies) {
        this.cookies = cookies;
    }

    /**
	 * Download a file from the Web and save it into given filename.
	 * @param address
	 * @param localFileName
	 * @return
	 */
    public static boolean download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Get the inner HTML of element with given name,attribute and atrribute value.
	 * 
	 * @param html
	 * @param elementname
	 * @param attribute
	 * @param value
	 * @return
	 */
    public static String getInnerHTML(String html, String elementname, String attribute, String value) {
        try {
            MicrosoftTagTypes.register();
            PHPTagTypes.register();
            PHPTagTypes.PHP_SHORT.deregister();
            MasonTagTypes.register();
            Source source = new Source(html);
            List<Element> elementList = source.getAllElements();
            for (Element element : elementList) {
                if (element.getName().toLowerCase().equals(elementname.toLowerCase()) && (value == null || value.equals(element.getAttributeValue(attribute)))) return element.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return html;
    }

    /**
	 * Remove all HTML-Tags from given string.
	 */
    public static String stripHTML(String s) {
        if (s == null) return null;
        boolean on = true;
        StringBuilder r = new StringBuilder(s.length());
        for (int n = 0; n < s.length(); n++) {
            char c = s.charAt(n);
            if (c == '<') on = false; else if (c == '>') on = true;
            if (on && c != '>') r.append(c);
        }
        r.trimToSize();
        return r.toString();
    }
}
