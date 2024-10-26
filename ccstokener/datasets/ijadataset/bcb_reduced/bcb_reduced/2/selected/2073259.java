package org.ofbiz.testtools.seleniumxml.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;

public class TestUtils {

    static char[] charMap = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    public static String createUniqueString() {
        long time = System.currentTimeMillis();
        return String.valueOf(time);
    }

    public static String createRandomString(int size) {
        return createRandomString(null, size);
    }

    public static String createRandomString(String prefix, String size) {
        return createRandomString(prefix, Integer.valueOf(size).intValue());
    }

    public static String createRandomString(String prefix, int size) {
        StringBuffer buff = new StringBuffer(size);
        int startIndx = 0;
        if (prefix != null) {
            buff.append(prefix);
            startIndx = prefix.length();
        }
        Random rad = new Random();
        for (int i = startIndx; i < size; i++) {
            buff.append(charMap[rad.nextInt(charMap.length)]);
        }
        return buff.toString();
    }

    public static String readUrlText(String urlString) throws IOException {
        URL url = new URL(urlString);
        InputStream stream = url.openStream();
        StringBuilder buf = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream));
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            System.out.println("Error reading text from URL [" + url + "]: " + e.toString());
            throw e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println("Error closing after reading text from URL [" + url + "]: " + e.toString());
                }
            }
        }
        return buf.toString();
    }
}
