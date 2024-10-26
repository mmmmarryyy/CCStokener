package org.fao.waicent.xmap2D.coordsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GeodeticPropertyFile {

    StringBuffer b;

    int a;

    public String readLine() throws IOException {
        if (b == null) {
            throw new IOException();
        }
        if (a >= b.length()) {
            throw new IOException();
        }
        char c;
        while ((c = b.charAt(a)) == '\n' || c == '\n' || c == '\r') {
            a++;
            if (a >= b.length()) {
                throw new IOException();
            }
        }
        int i = a;
        a++;
        int j = b.length() - 1;
        while ((c = b.charAt(a)) != '\n' && c != '\n' && c != '\r') {
            a++;
            if (a == j) {
                break;
            }
        }
        return b.substring(i, a);
    }

    public long length() throws IOException {
        if (b == null) {
            throw new IOException();
        } else {
            return (long) b.length();
        }
    }

    public void seek(long l) throws IOException {
        if (b == null || l > (long) b.length()) {
            throw new IOException();
        } else {
            a = (int) l;
            return;
        }
    }

    public long getFilePointer() throws IOException {
        if (b == null) {
            throw new IOException();
        } else {
            return (long) a;
        }
    }

    public GeodeticPropertyFile(URL url) throws IOException {
        super();
        a = 0;
        try {
            java.io.InputStream inputstream = url.openStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream, "UTF8");
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            int i = 0;
            b = new StringBuffer(4096);
            while ((i = bufferedreader.read()) != -1) {
                b.append((char) i);
            }
            bufferedreader.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
