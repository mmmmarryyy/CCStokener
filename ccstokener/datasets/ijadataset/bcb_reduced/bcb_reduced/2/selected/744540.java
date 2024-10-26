package edu.pitt.dbmi.odie.gapp.gwt.server.util.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.allen_sauer.gwt.log.client.Log;

public class ODIE_NcboRestUtils {

    public static String CONST_CHAR_ENCODING_ISO = "ISO-8859-1";

    public static String CONST_CHAR_ENCODING_UTF = "UTF-8";

    public static String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public static String payLoadStartElement = "<payLoad>";

    public static String payLoadEndElement = "</payLoad>";

    public static String emptyPayLoad = payLoadStartElement + payLoadEndElement;

    public static String emptyPayLoadXml = xmlHeader + payLoadStartElement + payLoadEndElement;

    public static boolean notNull(String str) {
        return str != null && !str.equalsIgnoreCase("null");
    }

    public static boolean isNull(String str) {
        return str == null || str.equalsIgnoreCase("null");
    }

    public static boolean isPosLen(String input) {
        return input != null && input.length() > 0;
    }

    public static String getXmlHeader() {
        return xmlHeader;
    }

    public static String getEmptyPayload() {
        return emptyPayLoadXml;
    }

    public static InputStream getInputStream(String urlAsString) {
        InputStream result = null;
        URL url;
        try {
            url = new URL(urlAsString);
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            if (con.getResponseCode() >= 300) {
                Log.warn(con.getResponseMessage());
            } else {
                result = con.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getPayLoad(String urlAsString) {
        String result = null;
        InputStream inputStream = null;
        URL url;
        try {
            url = new URL(urlAsString);
            final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(60000);
            httpConnection.setReadTimeout(60000);
            inputStream = httpConnection.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(inputStream, bos);
            result = new String(bos.toByteArray());
            inputStream.close();
            httpConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.warn("Failed to connect at " + (new Date()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Long parseLong(String longAsString) {
        Long result = null;
        longAsString = longAsString.replaceAll("^\\s*", "").replaceAll("\\s*$", "");
        long longValue = Long.parseLong(longAsString);
        result = new Long(longValue);
        return result;
    }

    public static Timestamp parseTimeStamp(String timeStampString) {
        Timestamp result = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            Date parsed = format.parse(timeStampString);
            result = new Timestamp(parsed.getTime());
        } catch (ParseException pe) {
            Log.warn("ERROR: Cannot parse \"" + timeStampString + "\"");
        } catch (Exception x) {
            x.printStackTrace();
        }
        return result;
    }

    public static String getString(final InputStream is, final String charEncoding) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte ba[] = new byte[8192];
            int read = is.read(ba);
            while (read > -1) {
                out.write(ba, 0, read);
                read = is.read(ba);
            }
            return out.toString(charEncoding);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String generateTabsOfLength(int level) {
        StringBuffer sb = new StringBuffer();
        for (int idx = 0; idx < level; idx++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    /**
	 * Copy bytes from an InputStream to an OutputStream.
	 * 
	 * @param input
	 *            the InputStream to read from
	 * @param output
	 *            the OutputStream to write to
	 * @return the number of bytes copied
	 * @throws IOException
	 *             In case of an I/O problem
	 */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
