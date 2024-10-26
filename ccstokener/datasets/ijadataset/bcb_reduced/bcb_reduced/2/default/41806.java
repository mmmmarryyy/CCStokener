import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;

public class BBSTextExtractor {

    String BasePath = "..\\BBS\\";

    String BaseSite = "http://localhost:8080/Discuz/bbs/";

    private static String getDocumentAt(String urlString) {
        StringBuffer html_text = new StringBuffer();
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) html_text.append(line + "\n");
            reader.close();
        } catch (MalformedURLException e) {
            System.out.println("��Ч��URL: " + urlString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html_text.toString();
    }

    public String BBSWebExt(String str_url) throws IOException {
        String str_content = "<url=" + str_url + ">\r\n";
        String str_title = null;
        String str_author = null;
        String str_time = null;
        int count = 0;
        BufferedWriter bw = null;
        String sorePath = BasePath + "bbs.txt";
        try {
            String str_html = BBSTextExtractor.getDocumentAt(str_url);
            Pattern pt_author = Pattern.compile("<a href=(.*) class=(.*) target=(.*)>(.*)</a><em id=(.*)>������ <span title=(.*)>(.*)</span></em>", Pattern.MULTILINE);
            Pattern pt_title = Pattern.compile("<title>(.*)</title>", Pattern.MULTILINE);
            Matcher mc = pt_title.matcher(str_html);
            while (mc.find()) {
                str_title = mc.group(1).trim();
                System.out.println("title:" + str_title);
            }
            mc = pt_author.matcher(str_html);
            Parser parser = new Parser(str_url);
            parser.setEncoding("gb2312");
            NodeFilter filter_table = new TagNameFilter("table");
            NodeList nodelist = parser.parse(filter_table);
            long num = nodelist.size();
            for (int i = 2; i < num && mc.find(); i += 2) {
                count++;
                str_author = mc.group(4).trim();
                str_time = mc.group(6).trim();
                Node node_title = nodelist.elementAt(i);
                str_content += "<#" + count + " " + str_author + " " + str_time + ">\r\n";
                str_content += "<" + node_title.toPlainTextString().trim() + ">\r\n";
            }
            str_content = str_content.replaceAll("&nbsp;", "");
            str_content = str_content.replaceAll("&quot;", "\"");
            str_content = str_content.replaceAll("&lt;", "<");
            str_content = str_content.replaceAll("&gt;", ">");
            str_content = str_content.replaceAll("\n;", "\r\n");
            System.out.println(str_content.trim());
            sorePath += str_title + ".txt";
            bw = new BufferedWriter(new FileWriter(new File(sorePath)));
            bw.write(str_content.trim());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PatternSyntaxException e) {
            System.out.println("������ʽ�﷨����");
        } catch (IllegalStateException e) {
            System.out.println("�Ҳ���ƥ���ַ�");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bw != null) bw.close();
        }
        return str_content;
    }

    public void extractUrl(String url) throws IOException {
        String title = null;
        String subtitle = null;
        String suburl = null;
        BufferedWriter bw = null;
        String sorePath = BasePath + "bbs.txt";
        try {
            bw = new BufferedWriter(new FileWriter(new File(sorePath)));
            String str = BBSTextExtractor.getDocumentAt(url);
            Pattern pt_title = Pattern.compile("<title>(.*)</title>", Pattern.MULTILINE | Pattern.DOTALL);
            Pattern pt_listtxt = Pattern.compile("<span id=\"(.*)\"><a href=\"(.*)\">(.*)</a></span>", Pattern.MULTILINE);
            Matcher mc = pt_title.matcher(str);
            while (mc.find()) {
                title = mc.group(1).trim();
                bw.write(title + "\r\n");
                System.out.println("title:" + title);
            }
            mc = pt_listtxt.matcher(str);
            while (mc.find()) {
                suburl = mc.group(2).trim();
                String[] names = suburl.split("\"");
                suburl = names[0];
                subtitle = mc.group(3).trim();
                bw.write("<Title=" + subtitle + "\t" + "scr=" + BaseSite + suburl + ">\r\n");
                System.out.println("<Title=" + subtitle + "\t" + "scr=" + BaseSite + suburl + ">");
                this.BBSWebExt(BaseSite + suburl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PatternSyntaxException e) {
            System.out.println("������ʽ�﷨����");
        } catch (IllegalStateException e) {
            System.out.println("�Ҳ���ƥ���ַ�");
        } finally {
            if (bw != null) bw.close();
        }
    }

    public void SetFold(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return;
        } else file.mkdir();
    }

    public static void main(String[] args) {
        try {
            BBSTextExtractor bbsext = new BBSTextExtractor();
            bbsext.BasePath = "..\\ABS\\";
            bbsext.SetFold(bbsext.BasePath);
            bbsext.BaseSite = "http://localhost:8080/Discuz/bbs/";
            String url = "http://localhost:8080/Discuz/bbs/forumdisplay.php?fid=14";
            bbsext.extractUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
