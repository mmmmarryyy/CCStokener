package net.nohaven.proj.javeau.crypt.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

public class InternetEntropyGatherer {

    private static final Pattern DETAIL = Pattern.compile("([\\dA-F]{64})");

    private static final String SRV_URL = "https://www.fourmilab.ch/cgi-bin/Hotbits?nbytes=32&fmt=hex";

    private static byte[] get256RandomBits() throws IOException {
        URL url = null;
        try {
            url = new URL(SRV_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpsURLConnection hu = (HttpsURLConnection) url.openConnection();
        hu.setConnectTimeout(2500);
        InputStream is = hu.getInputStream();
        byte[] content = new byte[is.available()];
        is.read(content);
        is.close();
        hu.disconnect();
        byte[] randomBits = new byte[32];
        String line = new String(content);
        Matcher m = DETAIL.matcher(line);
        if (m.find()) {
            for (int i = 0; i < 32; i++) randomBits[i] = (byte) (Integer.parseInt(m.group(1).substring(i * 2, i * 2 + 2), 16) & 0xFF);
        }
        return randomBits;
    }

    public static byte[] getRandomMaterial() {
        try {
            return get256RandomBits();
        } catch (IOException e) {
            System.err.println("Error in gathering entropy from the net.");
            System.err.println(e.toString());
            return new byte[0];
        }
    }
}
