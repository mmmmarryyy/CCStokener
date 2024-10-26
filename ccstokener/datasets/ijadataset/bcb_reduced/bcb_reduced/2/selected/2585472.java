package jbaidu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

public abstract class WebExt {

    protected static String getDocumentAt(String urlString) {
        StringBuffer html_text = new StringBuffer();
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) html_text.append(line + "\n");
            reader.close();
            url = null;
            conn = null;
        } catch (MalformedURLException e) {
            System.out.println("��Ч��URL: " + urlString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html_text.toString();
    }

    protected String BasePath = ".\\�ٶ����\\";

    protected String BaseSite = "http://localhost:8080/Discuz/bbs/";

    String sorePath = null;

    String title = null;

    String ori_content = null;

    boolean isDebug = false;

    boolean isFromWeb = false;

    String url = null;

    WebExt() {
    }

    WebExt(String path) {
        this.loadHtml(path);
    }

    public String doWithContent(String txt) {
        txt = txt.replaceAll("&nbsp;", "");
        txt = txt.replaceAll("&quot;", "\"");
        txt = txt.replaceAll("&lt;", "<");
        txt = txt.replaceAll("&gt;", ">");
        txt = txt.replaceAll("\r", "\r\n");
        txt = txt.replaceAll("\n", "\r\n");
        return txt;
    }

    public String getOriHtml() {
        return ori_content;
    }

    public String getTitle() {
        return title;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public void HtmlFromFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("�ļ������ڣ�");
            return;
        }
        BufferedReader reader = null;
        StringBuffer content = null;
        try {
            content = new StringBuffer();
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) content.append(line + "\r\n");
            setOriHtml(content.toString());
            reader.close();
            reader = null;
            this.isFromWeb = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader = null;
            }
        }
    }

    public void HtmlFromWeb(String url) {
        setOriHtml(getDocumentAt(url));
        this.isFromWeb = true;
        setURL(url);
    }

    public boolean loadHtml(String html) {
        if (html.matches("http:.*")) {
            this.HtmlFromWeb(html);
            return true;
        } else if (html.matches("[a-zA-Z]:.*")) {
            this.HtmlFromFile(html);
            return true;
        }
        return false;
    }

    public void setFold(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return;
        } else file.mkdir();
    }

    public void setOriHtml(String content) {
        ori_content = content;
    }

    public void TitleExt() {
        try {
            Parser parser = new Parser();
            parser.setInputHTML(ori_content);
            parser.setEncoding("gb2312");
            NodeFilter filter = new TagNameFilter("title");
            NodeList nodelist = parser.parse(filter);
            if (nodelist.size() == 0) {
                System.err.println("ҳ��������ɹ�");
                return;
            }
            title = nodelist.elementAt(0).toPlainTextString();
            if (this.isDebug) {
                System.out.println("����-ExtTitle()");
                System.out.println("Title:" + title);
            }
        } catch (IllegalStateException e) {
            System.out.println("�Ҳ���ƥ���ַ�");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void writeToFile(String txt) throws IOException {
        writeToFile(txt, null);
    }

    public void writeToFile(String txt, String str_title) throws IOException {
        BufferedWriter bw = null;
        try {
            this.setFold(BasePath);
            if (str_title == null && title != null) sorePath = BasePath + title.hashCode() + ".txt";
            if (str_title != null) {
                sorePath = BasePath + str_title + ".txt";
            }
            bw = new BufferedWriter(new FileWriter(new File(sorePath)));
            bw.write(txt.trim());
            bw.close();
            bw = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bw != null) bw.close();
        }
    }

    public void setDebug(boolean state) {
        this.isDebug = state;
    }

    public boolean getDebug() {
        return this.isDebug;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ori_content == null) ? 0 : ori_content.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WebExt other = (WebExt) obj;
        if (ori_content == null) {
            if (other.ori_content != null) return false;
        } else if (!ori_content.equals(other.ori_content)) return false;
        return true;
    }
}
