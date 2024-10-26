package uk.org.ogsadai.service.rest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestUtil {

    public static TestResponse post(String urlString, byte[] data, String contentType, String accept) throws IOException {
        HttpURLConnection httpCon = null;
        byte[] result = null;
        byte[] errorResult = null;
        try {
            URL url = new URL(urlString);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("POST");
            httpCon.setRequestProperty("Content-Type", contentType);
            httpCon.setRequestProperty("Accept", accept);
            if (data != null) {
                OutputStream output = httpCon.getOutputStream();
                output.write(data);
                output.close();
            }
            BufferedInputStream in = new BufferedInputStream(httpCon.getInputStream());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int next = in.read();
            while (next > -1) {
                os.write(next);
                next = in.read();
            }
            os.flush();
            result = os.toByteArray();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            InputStream errorStream = httpCon.getErrorStream();
            if (errorStream != null) {
                BufferedInputStream errorIn = new BufferedInputStream(errorStream);
                ByteArrayOutputStream errorOs = new ByteArrayOutputStream();
                int errorNext = errorIn.read();
                while (errorNext > -1) {
                    errorOs.write(errorNext);
                    errorNext = errorIn.read();
                }
                errorOs.flush();
                errorResult = errorOs.toByteArray();
                errorOs.close();
            }
            return new TestResponse(httpCon.getResponseCode(), errorResult, result);
        }
    }

    public static TestResponse get(String urlString, String accept) throws IOException {
        HttpURLConnection httpCon = null;
        byte[] result = null;
        byte[] errorResult = null;
        try {
            URL url = new URL(urlString);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod("GET");
            httpCon.setRequestProperty("Accept", accept);
            BufferedInputStream in = new BufferedInputStream(httpCon.getInputStream());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int next = in.read();
            while (next > -1) {
                os.write(next);
                next = in.read();
            }
            os.flush();
            result = os.toByteArray();
            os.close();
        } catch (IOException e) {
        } finally {
            InputStream errorStream = httpCon.getErrorStream();
            if (errorStream != null) {
                BufferedInputStream errorIn = new BufferedInputStream(errorStream);
                ByteArrayOutputStream errorOs = new ByteArrayOutputStream();
                int errorNext = errorIn.read();
                while (errorNext > -1) {
                    errorOs.write(errorNext);
                    errorNext = errorIn.read();
                }
                errorOs.flush();
                errorResult = errorOs.toByteArray();
                errorOs.close();
            }
            return new TestResponse(httpCon.getResponseCode(), errorResult, result);
        }
    }

    public static TestResponse delete(String urlString) throws IOException {
        HttpURLConnection httpCon = null;
        byte[] errorResult = null;
        try {
            URL url = new URL(urlString);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod("DELETE");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            InputStream errorStream = httpCon.getErrorStream();
            if (errorStream != null) {
                BufferedInputStream errorIn = new BufferedInputStream(errorStream);
                ByteArrayOutputStream errorOs = new ByteArrayOutputStream();
                int errorNext = errorIn.read();
                while (errorNext > -1) {
                    errorOs.write(errorNext);
                    errorNext = errorIn.read();
                }
                errorOs.flush();
                errorResult = errorOs.toByteArray();
                errorOs.close();
            }
            return new TestResponse(httpCon.getResponseCode(), errorResult, null);
        }
    }

    public static TestResponse put(String urlString, byte[] data, String contentType) throws IOException {
        HttpURLConnection httpCon = null;
        byte[] errorResult = null;
        try {
            URL url = new URL(urlString);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("PUT");
            httpCon.setRequestProperty("Content-Type", contentType);
            OutputStream output = httpCon.getOutputStream();
            output.write(data);
            output.close();
        } catch (IOException e) {
        } finally {
            InputStream errorStream = httpCon.getErrorStream();
            if (errorStream != null) {
                BufferedInputStream errorIn = new BufferedInputStream(errorStream);
                ByteArrayOutputStream errorOs = new ByteArrayOutputStream();
                int errorNext = errorIn.read();
                while (errorNext > -1) {
                    errorOs.write(errorNext);
                    errorNext = errorIn.read();
                }
                errorOs.flush();
                errorResult = errorOs.toByteArray();
                errorOs.close();
            }
            return new TestResponse(httpCon.getResponseCode(), errorResult, null);
        }
    }

    public static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) try {
                f.close();
            } catch (IOException ignored) {
            }
        }
        return new String(buffer);
    }
}
