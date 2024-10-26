package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Utils {

    private Utils() {
    }

    public static final double EPSILON = 0.005;

    private static final double SMALLEPSILON = 1.0e-9;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);

    private static final NumberFormat fd1 = format(1);

    private static final NumberFormat fd2 = format(2);

    private static final NumberFormat fd3 = format(3);

    private static final NumberFormat fd = new DecimalFormat("0.##", dfs);

    public static boolean isZero(double d) {
        return Math.round(d * 100) == 0;
    }

    public static boolean isPositive(double d) {
        return Math.round(d * 10000) >= 0;
    }

    public static boolean ge(double x, double y) {
        return x - y >= -0.005;
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double ceil2(double value) {
        return Math.ceil(value * 100.0 - SMALLEPSILON) / 100.0;
    }

    public static double floor2(double value) {
        return Math.floor(value * 100.0 + SMALLEPSILON) / 100.0;
    }

    public static double ceil3(double value) {
        return Math.ceil(value * 1000.0 - SMALLEPSILON) / 1000.0;
    }

    public static double floor3(double value) {
        return Math.floor(value * 1000.0 + SMALLEPSILON) / 1000.0;
    }

    public static double d(String string) {
        return Double.parseDouble(string);
    }

    public static float f(String string) {
        return Float.parseFloat(string);
    }

    public static int i(String string) {
        return Integer.parseInt(string);
    }

    public static String d0(double value) {
        return String.valueOf(Math.round(value));
    }

    public static String d1(double value) {
        return fd1.format(value);
    }

    public static String d2(double value) {
        return fd2.format(value);
    }

    public static String d3(double value) {
        return fd3.format(value);
    }

    public static String d2a(double value) {
        return fd.format(value);
    }

    public static NumberFormat format(int digits) {
        NumberFormat fd = NumberFormat.getNumberInstance(Locale.US);
        fd.setGroupingUsed(false);
        fd.setMaximumFractionDigits(digits);
        fd.setMinimumFractionDigits(digits);
        return fd;
    }

    public static String[] split(String str) {
        int last = str.length();
        do {
            if (last == 0) return EMPTY_STRING_ARRAY;
        } while (Character.isWhitespace(str.charAt(--last)));
        int index = 0;
        while (Character.isWhitespace(str.charAt(index))) ++index;
        int i = 0;
        int[] marks = new int[last - index + 2];
        marks[i++] = index;
        while (++index <= last) {
            if (Character.isWhitespace(str.charAt(index))) {
                marks[i++] = index;
                while (Character.isWhitespace(str.charAt(++index))) {
                }
                marks[i++] = index;
            }
        }
        marks[i++] = index;
        String[] result = new String[i / 2];
        i = 0;
        for (int j = 0; j < result.length; ++j) {
            result[j] = str.substring(marks[i++], marks[i++]);
        }
        return result;
    }

    private static final Pattern lineSep = Pattern.compile("\r?\n");

    public static String[] splitLines(CharSequence s) {
        return lineSep.split(s);
    }

    public static int skipSpaces(CharSequence s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) ++i;
        return i;
    }

    public static int scanToComment(CharSequence s) {
        int i = skipSpaces(s);
        if (i < s.length() && s.charAt(i) != '#') while (i < s.length() && s.charAt(i) != ';') ++i;
        return i;
    }

    public static String xmlEscape(CharSequence s) {
        int len = s.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            char ch = s.charAt(i);
            switch(ch) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String path(String s) {
        return s == null ? null : s.replaceAll("\\\\", "/");
    }

    /**
	 * @deprecated Use CommunicationService instead of this.
	 */
    @Deprecated
    public static boolean doHttpPost(URL url, String charset, Map<String, String> params) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            OutputStream out = conn.getOutputStream();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) out.write('&'); else first = false;
                out.write(URLEncoder.encode(entry.getKey(), charset).getBytes("US-ASCII"));
                out.write('=');
                out.write(URLEncoder.encode(entry.getValue(), charset).getBytes("US-ASCII"));
            }
            out.flush();
            out.close();
            conn.connect();
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == HttpURLConnection.HTTP_OK;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static String uriquote(CharSequence s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            int c = (int) s.charAt(i);
            if (Character.isISOControl(c) || Character.isSpaceChar(c) || "%?:=#".indexOf(c) >= 0) {
                if (c < 128) out.append(String.format("%%%02X", c)); else {
                    try {
                        for (byte b : String.valueOf((char) c).getBytes("UTF-8")) out.append(String.format("%%%02X", b));
                    } catch (UnsupportedEncodingException ignored) {
                    }
                }
            } else out.append((char) c);
        }
        return out.toString();
    }
}
