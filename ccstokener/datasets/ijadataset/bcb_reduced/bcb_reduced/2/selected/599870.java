package cn.fantix.gnualbumalpha2;

import org.dom4j.Element;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 * 还没有注释。
 * </pre>
 *
 * @author fantix
 * @date 2007-7-11 20:51:34
 */
public class UrlResourceObject extends ResourceObject {

    private String url;

    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?<=charset\\=)[a-zA-Z0-9\\-]+");

    public UrlResourceObject(Element el) {
        this.url = el.getTextTrim();
    }

    public byte[] getData(AlbumRequest request) throws IOException {
        String u = request.replace(url);
        BufferedInputStream bis = null;
        try {
            URL url = new URL(u);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int ch;
            while ((ch = bis.read()) != -1) {
                baos.write(ch);
            }
            byte[] bytes = baos.toByteArray();
            String charset = "UTF-8";
            String s = new String(bytes, charset);
            Matcher m = CHARSET_PATTERN.matcher(s);
            if (m.find()) {
                charset = m.group();
                s = new String(bytes, charset);
            }
            return s.getBytes();
        } finally {
            if (bis != null) try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
