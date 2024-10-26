import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

class Support {

    interface Param {

        String getName();

        String getValue();
    }

    private static class ParamImpl implements Param {

        private final String name;

        private final String value;

        ParamImpl(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public static Param newParam(String name, String value) {
        return new ParamImpl(name, value);
    }

    private static final String HOST_ONLINE = "jeffpalm.com";

    private static final String HOST_OFFLINE = "localhost/~jeff";

    public static final String getHost() {
        return isOnline() ? HOST_ONLINE : HOST_OFFLINE;
    }

    public static String urlencode(String s) {
        return URLEncoder.encode(s);
    }

    public static String getURL(String type, String method, Param[] params) {
        return getURL(type, method, params, new boolean[] { true, true });
    }

    public static String getURL(String type, String method, Param[] params, boolean[] encode) {
        String page = getPage(type, method, params, encode);
        if (isOnline()) {
            return "http://" + HOST_ONLINE + page;
        } else {
            return "http://" + HOST_OFFLINE + page;
        }
    }

    public static String getPage(String type, String method, Param[] params, boolean[] encode) {
        String page = "/db/" + decapitilize(type) + "." + "get" + capitilize(method) + ".do.php?_=1";
        for (Param p : params) {
            page += "&" + (encode != null && encode[0] ? urlencode(p.getName()) : p.getName()) + "=";
            page += encode != null && encode[1] ? urlencode(p.getValue()) : p.getValue();
        }
        return page;
    }

    public static String execute(String type, String method, Param[] params) {
        String url = getURL(type, method, params);
        note("url:" + url);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (in != null) in.close();
            String str = sb.toString();
            String target = "id=\"result\"";
            int itarget = str.indexOf(target);
            if (itarget == -1) return null;
            int igt = str.indexOf(">", itarget);
            if (igt == -1) return null;
            int ilt = str.indexOf("<", igt);
            if (ilt == -1) return null;
            return str.substring(igt + 1, ilt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String decapitilize(String s) {
        if (s == null) return s;
        if (s.length() == 0) return s;
        if (s.length() == 1) return s.toLowerCase();
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    static String capitilize(String s) {
        if (s == null) return s;
        if (s.length() == 0) return s;
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static final void note(String msg) {
        if (debug) System.err.println(msg);
    }

    private static final boolean debug;

    private static final boolean isOnline;

    static {
        boolean tmp = false;
        try {
            InputStream in = new URL("http://google.com").openStream();
            in.close();
            tmp = true;
        } catch (Exception e) {
            tmp = false;
        }
        isOnline = tmp;
        String s = System.getProperty("debug");
        debug = s != null && !s.equals("");
    }

    public static final boolean isOnline() {
        return isOnline;
    }
}
