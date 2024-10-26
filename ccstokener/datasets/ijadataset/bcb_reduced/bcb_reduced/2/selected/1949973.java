package cn.edu.bit.whitesail.crawl;

import cn.edu.bit.whitesail.WhiteSail;
import cn.edu.bit.whitesail.page.Page;
import cn.edu.bit.whitesail.page.URL;
import cn.edu.bit.whitesail.parser.HtmlParser;
import cn.edu.bit.whitesail.parser.Parser;
import cn.edu.bit.whitesail.utils.MD5Signature;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

/**
 *
 *
 * @version 0.1
 * @author baifan
 * @since 0.1
 */
public class Crawler extends Thread {

    private static final Log LOG = LogFactory.getLog(Crawler.class);

    private HttpClient httpClient;

    private static transient boolean isFinished = false;

    private Parser parser;

    private String fileName;

    private long block = 1;

    private File file;

    private FileOutputStream fos;

    private String detectedEncoding;

    public Crawler() {
        httpClient = new DefaultHttpClient();
        parser = new HtmlParser();
        HttpParams params = httpClient.getParams();
        params.setParameter("http.useragent", "BaiFan Robot");
        params.setParameter("http.protocol.single-cookie-header", Boolean.TRUE);
        params.setParameter("http.socket.timeout", 20000);
        params.setParameter("http.protocol.max-redirects", 3);
        params.setParameter("http.protocol.cookie-policy", "compatibility");
    }

    @Override
    public void run() {
        while (!isFinished) {
            URL u = fetchURL();
            if (u == null || u.to == null || u.to.equals("")) {
                continue;
            }
            if (!WhiteSail.VISITIED_URL_TABLE.add(u.to)) {
                continue;
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("download " + u.to);
            }
            byte[] contents = download(u.to);
            if (contents == null) {
                continue;
            }
            if (!WhiteSail.VISITIED_PAGE_TABLE.add(MD5Signature.calculate(contents))) {
                continue;
            }
            Page page = new Page(contents, detectedEncoding);
            page.fromURL = u.from;
            page.URL = u.to;
            WhiteSail.UNVISITED_URL_TABLE.add(parser.extractURLFromContent(page));
            appendData(page);
        }
        httpClient.getConnectionManager().shutdown();
        try {
            fos.close();
        } catch (IOException ex) {
            LOG.error("fileoutputstream can not close");
        }
    }

    private URL fetchURL() {
        URL result = null;
        while ((result = WhiteSail.UNVISITED_URL_TABLE.get()) == null) {
            try {
                LOG.info("unvisitable is empty , crawler sleeps for a while");
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                LOG.warn("crawler therad catch InterruptedException");
                continue;
            }
        }
        return result;
    }

    private byte[] download(String URL) {
        byte[] result = null;
        HttpEntity httpEntity = null;
        try {
            HttpGet httpGet = new HttpGet(URL);
            httpGet.addHeader("Accept-Language", "zh-cn,zh,en");
            httpGet.addHeader("Accept-Encoding", "gzip,deflate");
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            Header header = response.getFirstHeader("content-type");
            if (header == null || header.getValue().indexOf("text/html") < 0) {
                return null;
            }
            int pos = header.getValue().indexOf("charset=");
            if (pos >= 0) {
                detectedEncoding = header.getValue().substring(pos + 8);
            }
            httpEntity = response.getEntity();
            InputStream in = null;
            in = httpEntity.getContent();
            header = response.getFirstHeader("Content-Encoding");
            if (null != header) {
                if (header.getValue().indexOf("gzip") >= 0) {
                    in = new GZIPInputStream(in);
                } else if (header.getValue().indexOf("deflate") >= 0) {
                    in = new InflaterInputStream(in, new Inflater(true));
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            result = out.toByteArray();
        } catch (IOException ex) {
            LOG.warn("downloading error,abandon");
            result = null;
        }
        return result;
    }

    private void appendData(Page page) {
        if (file == null) {
            fileName = WhiteSail.DATA_DIRECTORY + "/" + getId() + "_" + block;
            file = new File(fileName);
        }
        if (fos == null) {
            try {
                fos = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                LOG.error("can not open file");
            }
        }
        try {
            fos.write(page.toByteArray());
            fos.write(page.rawContent);
            fos.flush();
        } catch (IOException ex) {
            LOG.error("can not write file");
        }
    }
}
