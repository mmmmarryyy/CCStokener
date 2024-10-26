package mainpackage;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.text.Document;

public class InternetAccesPage {

    private static final String LYRICSCATCHER = "http://lyricscatcher.sourceforge.net/";

    private static final String JSCRIPT = "http://apps.sourceforge.net/piwik/lyricscatcher/piwik.js";

    private static final String OPENINGSCRIPT = "<script type=\"text/javascript\">";

    private static final String CLOSINGSCRIPT = "</script>";

    public static boolean addToCounter(String str) {
        try {
            String mypage = ReadURLString(LYRICSCATCHER + str);
            String javascriptpage = ReadURLString(JSCRIPT);
            ArrayList<String> scripts = RetrieveStringsBetweenMarks(mypage, OPENINGSCRIPT, CLOSINGSCRIPT);
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("javascript");
            String script = scripts.get(1).replaceAll("pkBaseURL", "\"" + "http://apps.sourceforge.net/piwik/lyricscatcher/" + "\"");
            script = script.replaceAll("php\";\n", "php\";\n println( ");
            script = script.replaceAll("url\\);", "url));\nprintln('*****************************');\nprintln( _pk_getUrlLog(piwik_action_name, piwik_idsite, piwik_url));");
            script += javascriptpage;
            script = script.replaceAll("document.location.href", "\"" + LYRICSCATCHER + "piwik.php" + "\"");
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            script = script.replaceAll("screen.width", "" + screenSize.width);
            script = script.replaceAll("screen.height", "" + screenSize.height);
            script = script.replaceAll("document.writeln\\(", "println(");
            script = script.replaceAll("document.write\\(", "println(");
            script = script.replaceAll("window.location.hostname", "\"1.1.1.1\"");
            script = script.replaceAll("navigator.javaEnabled\\(\\)", "0");
            script = script.replaceAll("navigator.userAgent.toLowerCase\\(\\)", "0");
            script = script.replaceAll("navigator.appName.indexOf\\(\"Netscape\"\\)", "0");
            script = script.replaceAll("navigator.userAgent.toLowerCase\\(\\)", "0");
            script = script.replaceAll("_pk_agent.indexOf\\(.*\"\\)", "0");
            script = script.replaceAll("navigator.cookieEnabled", "0");
            script = script.replaceAll("if\\(parent\\)", "if(1>2)");
            script = script.replaceAll("= document.referrer", "= ''");
            script = script.replaceAll("document.title", "\"INSIDE JAVA\"");
            script = script.replaceAll("return _pk_src;", "println(_pk_src);return _pk_src;");
            System.out.println(script);
            System.out.println("result = " + engine.eval(script));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static ArrayList<String> RetrieveStringsBetweenMarks(String source, String start, String end) {
        ArrayList<String> result = new ArrayList<String>();
        while (source.indexOf(start) >= 0) {
            source = source.substring(source.indexOf(start) + start.length());
            result.add(source.substring(0, source.indexOf(end)));
        }
        return result;
    }

    public static String ReadURLString(String str) throws IOException {
        try {
            URL url = new URL(str);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            String line = "";
            int i = 0;
            while ((inputLine = in.readLine()) != null) {
                line += inputLine + "\n";
            }
            is.close();
            isr.close();
            in.close();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String args[]) {
        try {
            URL url = new URL("http://dev.activeanalytics.ca/piwik.php?url=http%3a%2f%2flyricscatcher.sourceforge.net%2fpiwik.php&action_name=&idsite=1&res=1440x900&h=17&m=2&s=16&fla=1&dir=1&qt=1&realp=1&pdf=1&wma=1&java=1&cookie=0&title=JAVAACCESS&urlref=http%3a%2f%2flyricscatcher.sourceforge.net%2fcomputeraccespage.html");
            InputStream ist = url.openStream();
            InputStreamReader isr = new InputStreamReader(ist);
            BufferedReader in = new BufferedReader(isr);
            String line = "";
            String inputline = "";
            while ((inputline = in.readLine()) != null) {
                line += inputline + "\n";
            }
            System.out.println("finished: length=" + line.length() + "line=" + line);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            URL url = new URL("http://apps.sourceforge.net/piwik/lyricscatcher/piwik.php?url=http%3a%2f%2flyricscatcher.sourceforge.net%2fpiwik.php&action_name=&idsite=1&res=1440x900&h=0&m=22&s=1&fla=1&dir=1&qt=1&realp=1&pdf=1&wma=1&java=1&cookie=0&title=JAVAACCESS&urlref=http%3a%2f%2flyricscatcher.sourceforge.net%2fcomputeraccespage.html");
            InputStream ist = url.openStream();
            InputStreamReader isr = new InputStreamReader(ist);
            BufferedReader in = new BufferedReader(isr);
            String line = "";
            String inputline = "";
            while ((inputline = in.readLine()) != null) {
                line += inputline + "\n";
            }
            System.out.println("finished: length=" + line.length() + "line=" + line);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            URL url = new URL("http://apps.sourceforge.net/piwik/lyricscatcher/piwik.php?url=http%3A%2F%2Flyricscatcher.sourceforge.net%2Fcomputeracces.html&action_name=&idsite=1&res=1440x900&h=0&m=28&s=36&fla=1&dir=1&qt=1&realp=0&pdf=1&wma=1&java=1&cookie=1&title=&urlref=");
            InputStream ist = url.openStream();
            InputStreamReader isr = new InputStreamReader(ist);
            BufferedReader in = new BufferedReader(isr);
            String line = "";
            String inputline = "";
            while ((inputline = in.readLine()) != null) {
                line += inputline + "\n";
            }
            System.out.println("finished: length=" + line.length() + "line=" + line);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void test() {
        addToCounter("computeracces.html");
        try {
            JFrame jfr = new JFrame("Webconnect");
            URL url = new URL("http://apps.sourceforge.net/piwik/lyricscatcher/piwik.php?idsite=1");
            JEditorPane jep = new JEditorPane();
            jfr.add(jep);
            String urlstr = "http://apps.sourceforge.net/piwik/lyricscatcher/piwik.php?url=http%3a%2f%2flyricscatcher.sourceforge.net%2fpiwik.php&action_name=&idsite=1&res=1440x900&h=";
            Calendar cal = Calendar.getInstance();
            urlstr += cal.get(Calendar.HOUR_OF_DAY);
            urlstr += "&m=";
            urlstr += cal.get(Calendar.MINUTE);
            urlstr += "&s=";
            urlstr += cal.get(Calendar.SECOND);
            urlstr += "&fla=1&dir=1&qt=1&realp=1&pdf=1&wma=1&java=1&cookie=0&title=JAVAACCESS&urlref=http%3a%2f%2flyricscatcher.sourceforge.net%2fcomputeraccespage.html";
            System.out.println(urlstr);
            URL nurl = new URL(urlstr);
            InputStream ist = nurl.openStream();
            InputStreamReader isr = new InputStreamReader(ist);
            BufferedReader in = new BufferedReader(isr);
            String line = "";
            String inputline = "";
            while ((inputline = in.readLine()) != null) {
                line += inputline + "\n";
            }
            System.out.println("finished: length of correct url=" + line.length());
            URL myurl = new URL(urlstr);
            URLConnection urlc = myurl.openConnection();
            urlc.getContent();
            System.out.println(urlc.getLastModified());
            System.out.println(urlc.getPermission());
            System.out.println(urlc.getRequestProperties());
            System.out.println(urlc.getContentEncoding());
            System.out.println(urlc.getContentLength());
            urlc.connect();
            InputStream dist = myurl.openStream();
            while (ist.available() >= 0) {
                ist.read();
            }
            ist.close();
            Document d = jep.getEditorKitForContentType("html").createDefaultDocument();
            d.getDefaultRootElement();
            jep.setContentType("text/html");
            jep.setText("<html><img src=\"http://apps.sourceforge.net/piwik/lyricscatcher/piwik.php?idsite=1\" alt=\"there's a problem...\"/><img src=\"" + urlstr + "\" alt=\"Another problem\" style=\"border:0\" /></html>");
            jfr.setLocationByPlatform(true);
            jfr.setSize(100, 100);
            jfr.show();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
