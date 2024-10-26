import java.io.*;
import java.net.*;
import java.util.regex.*;

public class Extractor {

    private String sorePath = "..\\b.txt";

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

    public void extractUrl(String url) throws IOException {
        String title = null;
        String suburl = null;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(sorePath)));
            String str = Extractor.getDocumentAt(url);
            Pattern pt_title = Pattern.compile("<title>(.*)</title>", Pattern.MULTILINE | Pattern.DOTALL);
            Pattern pt_listtxt = Pattern.compile("<\\s*table\\s+([^>]*)\\s*>(.*?)</table>", Pattern.MULTILINE);
            Matcher mc = pt_title.matcher(str);
            while (mc.find()) {
                title = mc.group(1).trim();
                bw.write(title + "\r\n");
                System.out.println("title:" + title);
            }
            mc = pt_listtxt.matcher(str);
            while (mc.find()) {
                suburl = mc.group(1).trim();
                bw.write(suburl + "\r\n");
                System.out.println("subtitle:" + suburl);
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

    public static void main(String[] args) {
        Extractor extractor = new Extractor();
        try {
            extractor.extractUrl("http://localhost:8080/Discuz/bbs/viewthread.php?tid=21&extra=page%3D1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
