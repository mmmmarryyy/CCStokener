package com.jspx.io;

import com.jspx.utils.FileUtil;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User:chenYuan (mail:cayurain@21cn.com)
 * Date: 2001-1-1
 * Time: 17:29:39
 */
public class ReadURLFile extends AbstractRead {

    private static final Log log = LogFactory.getLog(ReadURLFile.class);

    private InputStream in = null;

    private HttpURLConnection con = null;

    private final ByteArrayOutputStream message = new ByteArrayOutputStream();

    public ReadURLFile() {
    }

    protected boolean open() {
        try {
            if (!FileUtil.canRead(resource)) return false;
            URL url = new URL(resource);
            con = (HttpURLConnection) url.openConnection();
            in = con.getInputStream();
        } catch (Exception e) {
            log.error("Can not open URL :" + resource, e);
            return false;
        }
        return true;
    }

    protected void readContent() {
        try {
            byte[] data = new byte[1024];
            int nbRead = 0;
            while (nbRead >= 0) {
                try {
                    nbRead = in.read(data);
                    if (nbRead >= 0) message.write(data, 0, nbRead);
                    Thread.sleep(10);
                } catch (Exception e) {
                    nbRead = -1;
                }
            }
        } catch (NoClassDefFoundError ignoreSSL) {
        }
        result = new StringBuffer(message.toString());
    }

    protected void close() {
        if (in != null) {
            try {
                in.close();
                in = null;
                resource = null;
                con = null;
                message.reset();
            } catch (IOException e) {
                log.error("IO error !", e);
            }
        }
        if (con != null) con.disconnect();
    }
}
