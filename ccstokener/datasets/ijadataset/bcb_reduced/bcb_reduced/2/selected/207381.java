package be.kuleuven.peno3.mobiletoledo.Data.Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import org.apache.commons.io.IOUtils;

public abstract class Client {

    protected static final String host = "http://ariadne.cs.kuleuven.be/peno-cwa3";

    protected static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        URLConnection c = url.openConnection();
        c.setConnectTimeout(2000);
        IOUtils.copy(c.getInputStream(), output);
        return output.toString();
    }

    protected static String encode(String s) {
        s = s.replaceAll(" ", "%20");
        s = s.replaceAll("\"", "%22");
        s = s.replaceAll("\\[", "%5B");
        s = s.replaceAll("\\]", "%5D");
        return s;
    }

    protected static String toSQLString(Calendar cal) {
        String year = "" + cal.get(Calendar.YEAR);
        String month = "" + (cal.get(Calendar.MONTH) + 1);
        String day = "" + (cal.get(Calendar.DAY_OF_MONTH));
        String hour = "" + cal.get(Calendar.HOUR_OF_DAY);
        String minute = "" + cal.get(Calendar.MINUTE);
        return year + "-" + month + "-" + day + "%20" + hour + ":" + minute + ":00";
    }
}
