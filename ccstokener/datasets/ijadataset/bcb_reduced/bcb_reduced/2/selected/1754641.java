package org.feeddreamwork.fetcher;

import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.zip.*;
import org.feeddreamwork.*;

public class HttpFetcher extends HttpFetcherBase {

    private boolean gzip = false;

    public HttpFetcher(String url) throws MalformedURLException {
        super(url);
    }

    @Override
    public String getContent() throws IOException {
        BufferedReader reader = null;
        int retry = ApplicationProperty.HTTP_RETRY_TIMES;
        int fetchLimit = ApplicationProperty.HTTP_FETCH_LIMIT;
        while (true) try {
            HttpURLConnection conn = getConnection(url);
            reader = getBufferedReader(url, getEncoding(conn));
            String line;
            StringBuilder contentBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null && contentBuilder.length() <= fetchLimit) contentBuilder.append(line).append('\n');
            String content = contentBuilder.toString();
            if (content.length() > fetchLimit) content.substring(0, fetchLimit);
            if (content.contains("400 Bad Request") && content.contains("nginx")) throw new IOException();
            if ((content.contains("<p>该页面不存在</p>") || content.contains("<p>该页面不存在,3秒后自动回首页</p>")) && content.contains("<p><a href=\"http://www.cnbeta.com\">cnBeta.COM </a><br />")) throw new IOException();
            if (content.contains("<script>location.href=\"/subapple/articleblog/art_id/")) throw new IOException();
            return content;
        } catch (IOException e) {
            retry--;
            if (retry == 0) throw e;
        } finally {
            if (reader != null) reader.close();
        }
    }

    private String getEncoding(HttpURLConnection conn) throws IOException {
        BufferedReader reader = null;
        String encoding = null;
        String ctype = null;
        try {
            ctype = conn.getContentType();
        } catch (Exception e) {
        }
        if (!Utils.isNullOrEmpty(ctype)) {
            String[] params = ctype.split(";");
            for (String param : params) {
                if (param.toLowerCase().contains("charset=")) return extractEncoding(param.replaceFirst("charset=", "").trim(), true);
            }
        }
        reader = getBufferedReader(url);
        String line, mat;
        while ((line = reader.readLine()) != null) {
            line = line.toLowerCase();
            mat = matchPattern(line, "(?<=charset=).*?(?=\")");
            if (!Utils.isNullOrEmpty(mat)) return extractEncoding(mat, true);
            mat = matchPattern(line, "(?<=charset=\").*?(?=\")");
            if (!Utils.isNullOrEmpty(mat)) return extractEncoding(mat, true);
            mat = matchPattern(line, "(?<=encoding=\").*?(?=\")");
            if (!Utils.isNullOrEmpty(mat)) return extractEncoding(mat, true);
            if (line.contains("charset") || line.contains("encoding")) {
                encoding = extractEncoding(line, false);
                if (!Utils.isNullOrEmpty(encoding)) return encoding;
            }
        }
        return ApplicationProperty.DEFAULT_ENCODING;
    }

    private String matchPattern(String str, String regex) {
        Matcher mat = Pattern.compile(regex).matcher(str);
        if (mat.find()) return mat.group(); else return null;
    }

    private BufferedReader getBufferedReader(URL url) throws IOException {
        return getBufferedReader(url, null);
    }

    private BufferedReader getBufferedReader(URL url, String encoding) throws IOException {
        HttpURLConnection conn = getConnection(url);
        return getBufferedReader(conn, encoding);
    }

    private BufferedReader getBufferedReader(HttpURLConnection conn, String encoding) throws IOException {
        BufferedReader reader = null;
        String gzip = null;
        try {
            gzip = conn.getContentEncoding();
        } catch (Exception e) {
        }
        InputStream in = conn.getInputStream();
        if (gzip != null && gzip.toLowerCase().equals("gzip")) {
            this.gzip = true;
            GZIPInputStream gzipin = new GZIPInputStream(in);
            if (Utils.isNullOrEmpty(encoding)) reader = new BufferedReader(new InputStreamReader(gzipin)); else reader = new BufferedReader(new InputStreamReader(gzipin, encoding));
        } else {
            if (Utils.isNullOrEmpty(encoding)) reader = new BufferedReader(new InputStreamReader(in)); else reader = new BufferedReader(new InputStreamReader(in, encoding));
        }
        return reader;
    }

    private HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("User-Agent", ApplicationProperty.USER_AGENT);
        conn.connect();
        return conn;
    }

    public boolean getGZIPStatus() {
        return gzip;
    }

    private String extractEncoding(String source, boolean strict) {
        if (source.contains("gb2312") || source.contains("GB2312") || source.contains("gbk") || source.contains("GBK")) return ApplicationProperty.DEFAULT_SC_DECODING;
        if (source.contains("big5") || source.contains("big-5") || source.contains("BIG5") || source.contains("BIG-5")) return ApplicationProperty.DEFAULT_TC_DECODING;
        if (source.contains("utf-8") || source.contains("utf8") || source.contains("UTF-8") || source.contains("UTF8")) return "UTF-8";
        if (strict) return source; else return null;
    }
}
