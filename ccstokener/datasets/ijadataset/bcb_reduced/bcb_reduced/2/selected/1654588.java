package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class IPCameraFrameGrabber extends FrameGrabber {

    public static final Logger LOG = Logger.getLogger(IPCameraFrameGrabber.class.getCanonicalName());

    private URL url;

    private URLConnection connection;

    private InputStream input;

    private Map<String, List<String>> headerfields;

    private String boundryKey;

    public IPCameraFrameGrabber(String urlstr) {
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            LOG.error(e);
        }
    }

    @Override
    public void start() throws Exception {
        LOG.error("connecting to " + url);
        connection = url.openConnection();
        headerfields = connection.getHeaderFields();
        if (headerfields.containsKey("Content-Type")) {
            List<String> ct = headerfields.get("Content-Type");
            for (int i = 0; i < ct.size(); ++i) {
                String key = ct.get(i);
                int j = key.indexOf("boundary=");
                if (j != -1) {
                    boundryKey = key.substring(j + 9);
                }
            }
        }
        input = connection.getInputStream();
    }

    @Override
    public void stop() throws Exception {
        input.close();
        input = null;
        connection = null;
        url = null;
    }

    @Override
    public void trigger() throws Exception {
    }

    @Override
    public IplImage grab() throws Exception {
        return IplImage.createFrom(grabBufferedImage());
    }

    public BufferedImage grabBufferedImage() throws Exception {
        byte[] buffer = new byte[4096];
        int n = -1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuffer sb = new StringBuffer();
        int total = 0;
        int c;
        while ((c = input.read()) != -1) {
            if (c > 0) {
                sb.append((char) c);
                if (c == 13) {
                    sb.append((char) input.read());
                    c = input.read();
                    sb.append((char) c);
                    if (c == 13) {
                        sb.append((char) input.read());
                        break;
                    }
                }
            }
        }
        String subheader = sb.toString();
        LOG.error(subheader);
        int contentLength = -1;
        int c0 = subheader.indexOf("Content-Length: ");
        int c1 = subheader.indexOf('\r', c0);
        c0 += 16;
        contentLength = Integer.parseInt(subheader.substring(c0, c1));
        LOG.info("Content-Length: " + contentLength);
        if (contentLength > buffer.length) {
            buffer = new byte[contentLength];
        }
        n = -1;
        total = 0;
        while ((n = input.read(buffer, 0, contentLength - total)) != -1) {
            total += n;
            baos.write(buffer, 0, n);
            if (total == contentLength) {
                break;
            }
        }
        baos.flush();
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        return bi;
    }

    @Override
    public void release() throws Exception {
    }
}
