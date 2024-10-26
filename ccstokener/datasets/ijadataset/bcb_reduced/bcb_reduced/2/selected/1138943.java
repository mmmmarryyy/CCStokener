package ru.korusconsulting.connector.base;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

public class ConnectionUtils {

    public static void writeToStream(byte[] data, OutputStream urlOutputStream) throws IOException {
        BufferedOutputStream os = null;
        try {
            os = new BufferedOutputStream(urlOutputStream);
            os.write(data);
            os.flush();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (urlOutputStream != null) {
                    urlOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] download(String address, ConnectionContext ccontext) throws IOException {
        ByteArrayOutputStream out = null;
        HttpURLConnection conn = null;
        InputStream in = null;
        String token = ccontext.getAuthToken();
        try {
            URL url = new URL(address);
            out = new ByteArrayOutputStream();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Cookie", "ZM_AUTH_TOKEN=" + token);
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
            return out.toByteArray();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                FunambolLogger logger = FunambolLoggerFactory.getLogger("funambol.zimbra.manager");
                logger.error("Downloading from Zimbra went wrong", ioe);
            }
        }
    }

    public static String upload(String fileName, byte[] fileContent, ConnectionContext ccontext, String servletUploadURL) throws IOException {
        String token = ccontext.getAuthToken();
        URL url = new URL(servletUploadURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        conn.setRequestProperty("Cookie", "ZM_AUTH_TOKEN=" + token);
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"upload\";" + " filename=\"" + fileName + "\"" + lineEnd);
        dos.writeBytes(lineEnd);
        dos.write(fileContent);
        dos.writeBytes(lineEnd);
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        dos.flush();
        dos.close();
        byte buffer[] = new byte[1024];
        int readed;
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((readed = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, readed);
        }
        String result = new String(baos.toByteArray());
        String params[] = result.split(",");
        if (params.length != 3) {
            throw new IOException("Unexpected result of upload:" + result);
        }
        String aid = params[2].substring(1, params[2].lastIndexOf('\''));
        return aid;
    }

    private static final String lineEnd = "\r\n";

    private static final String twoHyphens = "--";

    private static final String boundary = "*****";
}
