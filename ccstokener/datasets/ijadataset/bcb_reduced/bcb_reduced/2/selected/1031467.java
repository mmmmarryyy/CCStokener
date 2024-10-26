package com.jspx.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.net.*;

public class ReadHtml extends AbstractRead {

    private static final Log log = LogFactory.getLog(ReadHtml.class);

    private BufferedReader in;

    public ReadHtml() {
    }

    public ReadHtml(String s) {
        resource = s;
    }

    public boolean open() {
        try {
            URL url = new URL(resource);
            URLConnection conn = url.openConnection();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), encode));
        } catch (MalformedURLException e) {
            log.error("Uable to connect URL:" + resource, e);
            return false;
        } catch (IOException e) {
            log.error("IOExeption when connecting to URL" + resource, e);
            return false;
        }
        return true;
    }

    protected void readContent() {
        result = new StringBuffer();
        try {
            if (in != null) {
                String str;
                while ((str = in.readLine()) != null) {
                    result.append(str);
                }
            }
        } catch (IOException e) {
            log.error("Read file error !", e);
        }
    }

    protected void close() {
        if (in != null) {
            try {
                in.close();
                in = null;
                resource = null;
            } catch (IOException e) {
                log.error("IO error !", e);
            }
        }
    }
}
