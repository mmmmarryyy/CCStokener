package org.apache.myfaces.resource;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ResourceUtils {

    private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";

    private static final String[] HTTP_REQUEST_DATE_HEADER = { "EEE, dd MMM yyyy HH:mm:ss zzz", "EEEEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMMM d HH:mm:ss yyyy" };

    private static TimeZone __GMT = TimeZone.getTimeZone("GMT");

    public static String formatDateHeader(long value) {
        SimpleDateFormat format = new SimpleDateFormat(HTTP_RESPONSE_DATE_HEADER, Locale.US);
        format.setTimeZone(__GMT);
        return format.format(new Date(value));
    }

    public static Long parseDateHeader(String value) {
        Date date = null;
        for (int i = 0; (date == null) && (i < HTTP_REQUEST_DATE_HEADER.length); i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(HTTP_REQUEST_DATE_HEADER[i], Locale.US);
                format.setTimeZone(__GMT);
                date = format.parse(value);
            } catch (ParseException e) {
                ;
            }
        }
        if (date == null) {
            return null;
        }
        return new Long(date.getTime());
    }

    public static long getResourceLastModified(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            String externalForm = url.toExternalForm();
            File file = new File(externalForm.substring(5));
            return file.lastModified();
        } else {
            return getResourceLastModified(url.openConnection());
        }
    }

    public static long getResourceLastModified(URLConnection connection) throws IOException {
        long modified;
        if (connection instanceof JarURLConnection) {
            URL jarFileUrl = ((JarURLConnection) connection).getJarFileURL();
            URLConnection jarFileConnection = jarFileUrl.openConnection();
            try {
                modified = jarFileConnection.getLastModified();
            } finally {
                try {
                    jarFileConnection.getInputStream().close();
                } catch (Exception exception) {
                }
            }
        } else {
            modified = connection.getLastModified();
        }
        return modified;
    }
}
