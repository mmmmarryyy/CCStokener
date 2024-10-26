package nl.weeaboo.ogg;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLInput implements RandomOggInput {

    private URL url;

    private int length;

    private long responseTime;

    public URLInput(URL u) {
        url = u;
        length = -1;
        responseTime = 10000L;
        InputStream in = null;
        try {
            in = openStream(0, 0);
        } catch (IOException ioe) {
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ioe) {
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public InputStream openStream() throws IOException {
        return openStream(0, length());
    }

    @Override
    public InputStream openStream(long off, long len) throws IOException {
        URLConnection con = url.openConnection();
        if (!(con instanceof HttpURLConnection)) {
            return null;
        }
        long t0 = System.currentTimeMillis();
        HttpURLConnection urlcon = (HttpURLConnection) con;
        urlcon.setRequestProperty("Connection", "Keep-Alive");
        String rangeS = "";
        if (off > 0) rangeS += "bytes=" + off + "-";
        if (len > 0 && off + len < length) rangeS += (len - 1);
        if (rangeS.length() > 0) {
            urlcon.setRequestProperty("Range", rangeS);
        }
        urlcon.setRequestProperty("Content-Type", "application/octet-stream");
        InputStream in = urlcon.getInputStream();
        rangeS = urlcon.getHeaderField("Content-Range");
        long responseOff = 0;
        long responseEnd = -1;
        if (rangeS != null) {
            int start = rangeS.indexOf(' ') + 1;
            int end = rangeS.indexOf('-', start);
            String offS = rangeS.substring(start, end).trim();
            responseOff = Long.parseLong(offS);
            start = end + 1;
            end = rangeS.indexOf('/', start);
            String lenS = rangeS.substring(start, end).trim();
            responseEnd = 1 + Long.parseLong(lenS);
        }
        while (responseOff < off && responseOff <= responseEnd) {
            long s = in.skip(off - responseOff);
            if (s <= 0) {
                break;
            }
            responseOff += s;
        }
        length = urlcon.getHeaderFieldInt("Content-Length", -1);
        long respTime = System.currentTimeMillis() - t0;
        if (responseTime < 0) {
            responseTime = respTime;
        } else {
            responseTime = Math.round(0.5 * responseTime + 0.5 * respTime);
        }
        return in;
    }

    @Override
    public int read(byte[] b, int off, long foff, int len) throws IOException {
        InputStream in = openStream(foff, len);
        int read = 0;
        while (read < len) {
            int r = in.read(b, off + read, len - read);
            if (r < 0) {
                break;
            }
            read += r;
        }
        return read;
    }

    @Override
    public boolean isReadSlow() {
        return true;
    }

    @Override
    public boolean isSeekSlow() {
        return responseTime <= 100;
    }

    @Override
    public long length() throws IOException {
        return length;
    }
}
