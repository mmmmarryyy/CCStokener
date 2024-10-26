import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScreenScraper {

    public String getScreenShot(String ticker) {
        URL url;
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        ticker = ticker.toLowerCase();
        String yahooFinance = null;
        String yahooAddress = ("http://finance.yahoo.com/q?s=" + ticker);
        try {
            url = new URL(yahooAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((yahooFinance = in.readLine()) != null) {
                    sb.append(yahooFinance);
                }
            } else {
                System.out.println("Response code: " + responseCode + " ----  Error reading url: " + yahooAddress);
            }
        } catch (IOException e) {
            System.err.println("IOException attempting to read url " + ticker);
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
        String screenShot = sb.toString();
        return screenShot;
    }

    public String getPrice(String screenShot, String ticker) {
        String searchString = new String();
        searchString = getScreenShot(ticker);
        String searchFragment1 = "yfs_l10_";
        String span = "</span>";
        String searchPoint1 = searchFragment1 + ticker;
        int match = searchString.indexOf(searchPoint1);
        int end = searchString.indexOf(span, match);
        int begin = 0;
        if (ticker.length() == 4) {
            begin = match + 14;
        } else if (ticker.length() == 3) {
            begin = match + 13;
        } else {
            begin = match + 12;
        }
        String result = new String();
        try {
            result = searchString.substring(begin, end);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            System.out.println("ScreenScraper.getPrice()...Cannot get price...Attempting to recover");
        } finally {
        }
        return result;
    }
}
