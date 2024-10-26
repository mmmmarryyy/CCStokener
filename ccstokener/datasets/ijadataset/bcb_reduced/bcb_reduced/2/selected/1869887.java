package URLcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import settingsStorage.ConfigLoader;
import settingsStorage.NoSuchParameterException;
import settingsStorage.StatisticsStorage;

public class URLLoadClass {

    public static void main(String args[]) {
        System.out.println(removeFirstLines("abcd1\nabcd2\nabcd3\nabcd4\n5", 2));
    }

    public static synchronized String ReadURLString(URL url) throws IOException {
        try {
            StatisticsStorage.numberofdownloadedbytes += EstimateLengthOfPage(url);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            String line = "";
            int i = 0;
            while ((inputLine = in.readLine()) != null) {
                line += inputLine + "\n";
            }
            is.close();
            isr.close();
            in.close();
            return line;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String ReadGZIPURLString(URL url) throws IOException {
        try {
            InputStream is = url.openStream();
            GZIPInputStream zipin = new GZIPInputStream(is);
            InputStreamReader isr = new InputStreamReader(zipin);
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            String line = "";
            while ((inputLine = in.readLine()) != null) {
                line += inputLine + "\n";
            }
            is.close();
            isr.close();
            in.close();
            return line;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int EstimateLengthOfPage(URL url) {
        if (url == null) return -1;
        try {
            URLConnection uc = url.openConnection();
            int l = uc.getContentLength();
            return l;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getContentTypeOfPage(URL url) {
        if (url == null) return "";
        try {
            URLConnection uc = url.openConnection();
            String l = uc.getContentType();
            return l;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static ArrayList<String> RetrieveStringsBetweenMarks(String source, String start, String end) {
        ArrayList<String> result = new ArrayList<String>();
        while (source.indexOf(start) >= 0) {
            source = source.substring(source.indexOf(start) + start.length());
            if (source.indexOf(end) != -1) result.add(source.substring(0, source.indexOf(end)));
        }
        return result;
    }

    public static String ReadURLStringAndWrite(URL url, String str) throws Exception {
        String stringToReverse = URLEncoder.encode(str, "UTF-8");
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(stringToReverse);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String decodedString;
        String back = "";
        while ((decodedString = in.readLine()) != null) {
            back += decodedString + "\n";
        }
        in.close();
        return back;
    }

    public static URL createDownloadURL(String text) {
        try {
            String URL = ConfigLoader.readString("yv0");
            if (text.indexOf(ConfigLoader.readString("yv1")) >= 0) {
                URL += ConfigLoader.readString("yv2");
            } else {
                URL += ConfigLoader.readString("yv3");
            }
            URL += ConfigLoader.readString("yv4");
            ArrayList<String> list = RetrieveStringsBetweenMarks(text, ConfigLoader.readString("yv5"), ConfigLoader.readString("yv6"));
            if (list.size() == 0) return null;
            URL += list.get(0);
            URL += ConfigLoader.readString("yv7");
            list = RetrieveStringsBetweenMarks(text, ConfigLoader.readString("yv8"), ConfigLoader.readString("yv9"));
            if (list.size() == 0) return null;
            URL += list.get(0);
            try {
                URL url = new URL(URL);
                return url;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String removeFirstLines(String str, int n) {
        while (n > 0 && str.contains("\n")) {
            str = str.substring(str.indexOf("\n") + 1);
            n--;
        }
        return str;
    }
}
