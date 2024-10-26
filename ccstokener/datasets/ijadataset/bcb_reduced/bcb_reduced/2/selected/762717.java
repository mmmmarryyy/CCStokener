package dk.tiede.linkchecker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author kbt
 */
public class LinkChecker {

    private LinkChecker() {
    }

    /**
     * 
     * @param theUrl 
     * @throws java.net.MalformedURLException 
     * @return 
     */
    public static Result checkLink(String theUrl) throws MalformedURLException {
        URL url = new URL(theUrl);
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            return new Result(urlConnection.getResponseCode(), false);
        } catch (IOException e) {
            return new Result(0, true);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static class Result {

        private int responseCode = 0;

        private boolean ioError = false;

        /**
         * 
         * @param responseCode 
         * @param ioError 
         */
        public Result(int responseCode, boolean ioError) {
            this.responseCode = responseCode;
            this.ioError = ioError;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public boolean isIoError() {
            return ioError;
        }

        public boolean isSuccess() {
            return (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_FORBIDDEN) && !isIoError();
        }
    }
}
