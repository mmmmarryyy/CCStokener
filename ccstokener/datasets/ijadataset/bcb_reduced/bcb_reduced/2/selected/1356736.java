package com.fasteasytrade.JRandTest.IO;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Zur Aougav
 * <p>
 * This class represents an encrypted or random source HTTP Url source.
 * <p>
 * Data, bytes or int32, are read from url.
 * 
 */
public class HttpGetUrlRandomStream implements RandomStream {

    boolean open = false;

    DataInputStream infile = null;

    String filename = null;

    URL url = null;

    URLConnection con = null;

    int lengthOfData = 0;

    int count = 0;

    int countLastRead = 0;

    final int SIZE = 4096 * 4;

    byte[] buffer = new byte[SIZE];

    public HttpGetUrlRandomStream() {
    }

    public HttpGetUrlRandomStream(String s) {
        filename = s;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#getFilename()
	 */
    public String getFilename() {
        return filename;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#setFilename(java.lang.String)
	 */
    public void setFilename(String s) {
        filename = s;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#isOpen()
	 */
    public boolean isOpen() {
        return open;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#openInputStream()
	 */
    public boolean openInputStream() throws Exception {
        open = false;
        if (filename == null) return false;
        try {
            url = new URL(filename);
            con = url.openConnection();
            con.connect();
            lengthOfData = con.getContentLength();
            System.out.println(" headers for url: " + url);
            System.out.println(" lengthOfData = " + lengthOfData);
            Map m = con.getHeaderFields();
            Set s = m.keySet();
            Iterator i = s.iterator();
            while (i.hasNext()) {
                String x = (String) i.next();
                Object o = m.get(x);
                String y = null;
                if (o instanceof String) y = (String) o; else if (o instanceof Collection) y = "" + (Collection) o; else if (o instanceof Integer) y = "" + (Integer) o; else y = o.getClass().getName();
                System.out.println(" header " + x + " = " + y);
            }
            infile = new DataInputStream(con.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        open = true;
        count = 0;
        countLastRead = 0;
        return true;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#closeInputStream()
	 */
    public boolean closeInputStream() {
        try {
            infile.close();
        } catch (Exception e) {
        }
        open = false;
        return true;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#readByte()
	 */
    public byte readByte() throws Exception {
        if (!isOpen()) return -1;
        try {
            if (count >= countLastRead) {
                count = 0;
                countLastRead = infile.read(buffer);
                if (countLastRead < 0) {
                    open = false;
                    return -1;
                }
            }
            byte temp = buffer[count];
            count++;
            return temp;
        } catch (Exception e) {
            open = false;
        }
        return -1;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#readInt()
	 */
    public int readInt() throws Exception {
        byte[] b = new byte[4];
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (0xff & readByte());
            if (!isOpen()) return -1;
        }
        return result;
    }

    /**
	 * @see com.fasteasytrade.JRandTest.IO.RandomStream#readLong()
	 */
    public long readLong() throws Exception {
        byte[] b = new byte[8];
        int result = 0;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (0xff & readByte());
            if (!isOpen()) return -1;
        }
        return result;
    }
}
