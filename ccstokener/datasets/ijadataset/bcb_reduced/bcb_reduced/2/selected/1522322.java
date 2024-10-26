package acme;

import java.util.*;
import java.io.*;
import java.net.*;

public class Utils {

    public static String lsDateStr(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        long dateTime = date.getTime();
        if (dateTime == -1L) return "------------";
        long nowTime = (new Date()).getTime();
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        String part1 = months[cal.get(Calendar.MONTH)] + Fmt.fmt(cal.get(Calendar.DATE), 3);
        if (Math.abs(nowTime - dateTime) < 183L * 24L * 60L * 60L * 1000L) return part1 + Fmt.fmt(cal.get(Calendar.HOUR_OF_DAY), 3) + ":" + Fmt.fmt(cal.get(Calendar.MINUTE), 2, Fmt.ZF); else return part1 + Fmt.fmt(cal.get(Calendar.YEAR), 6);
    }

    public static String pluralStr(long n) {
        if (n == 1) return ""; else return "s";
    }

    public static final long INT_SECOND = 1000L;

    public static final long INT_MINUTE = INT_SECOND * 60L;

    public static final long INT_HOUR = INT_MINUTE * 60L;

    public static final long INT_DAY = INT_HOUR * 24L;

    public static final long INT_WEEK = INT_DAY * 7L;

    public static final long INT_MONTH = INT_DAY * 30L;

    public static final long INT_YEAR = INT_DAY * 365L;

    public static final long INT_DECADE = INT_DAY * 3652L;

    public static String intervalStr(long interval) {
        long decades, years, months, weeks, days, hours, minutes, seconds, millis;
        decades = interval / INT_DECADE;
        interval -= decades * INT_DECADE;
        years = interval / INT_YEAR;
        interval -= years * INT_YEAR;
        months = interval / INT_MONTH;
        interval -= months * INT_MONTH;
        weeks = interval / INT_WEEK;
        interval -= weeks * INT_WEEK;
        days = interval / INT_DAY;
        interval -= days * INT_DAY;
        hours = interval / INT_HOUR;
        interval -= hours * INT_HOUR;
        minutes = interval / INT_MINUTE;
        interval -= minutes * INT_MINUTE;
        seconds = interval / INT_SECOND;
        interval -= seconds * INT_SECOND;
        millis = interval;
        if (decades > 0) if (years == 0) return decades + " decade" + pluralStr(decades); else return decades + " decade" + pluralStr(decades) + ", " + years + " years" + pluralStr(years); else if (years > 0) if (months == 0) return years + " year" + pluralStr(years); else return years + " year" + pluralStr(years) + ", " + months + " month" + pluralStr(months); else if (months > 0) if (weeks == 0) return months + " month" + pluralStr(months); else return months + " month" + pluralStr(months) + ", " + weeks + " week" + pluralStr(weeks); else if (weeks > 0) if (days == 0) return weeks + " week" + pluralStr(weeks); else return weeks + " week" + pluralStr(weeks) + ", " + days + " day" + pluralStr(days); else if (days > 0) if (hours == 0) return days + " day" + pluralStr(days); else return days + " day" + pluralStr(days) + ", " + hours + " hour" + pluralStr(hours); else if (hours > 0) if (minutes == 0) return hours + " hour" + pluralStr(hours); else return hours + " hour" + pluralStr(hours) + ", " + minutes + " minute" + pluralStr(minutes); else if (minutes > 0) if (seconds == 0) return minutes + " minute" + pluralStr(minutes); else return minutes + " minute" + pluralStr(minutes) + ", " + seconds + " second" + pluralStr(seconds); else if (seconds > 0) if (millis == 0) return seconds + " second" + pluralStr(seconds); else return seconds + " second" + pluralStr(seconds) + ", " + millis + " millisecond" + pluralStr(millis); else return millis + " millisecond" + pluralStr(millis);
    }

    public static int strSpan(String str, String charSet) {
        return strSpan(str, charSet, 0);
    }

    public static int strSpan(String str, String charSet, int fromIdx) {
        int i;
        for (i = fromIdx; i < str.length(); ++i) if (charSet.indexOf(str.charAt(i)) == -1) break;
        return i - fromIdx;
    }

    public static int strCSpan(String str, String charSet) {
        return strCSpan(str, charSet, 0);
    }

    public static int strCSpan(String str, String charSet, int fromIdx) {
        int i;
        for (i = fromIdx; i < str.length(); ++i) if (charSet.indexOf(str.charAt(i)) != -1) break;
        return i - fromIdx;
    }

    public static boolean match(String pattern, String string) {
        for (int p = 0; ; ++p) {
            for (int s = 0; ; ++p, ++s) {
                boolean sEnd = (s >= string.length());
                boolean pEnd = (p >= pattern.length() || pattern.charAt(p) == '|');
                if (sEnd && pEnd) return true;
                if (sEnd || pEnd) break;
                if (pattern.charAt(p) == '?') continue;
                if (pattern.charAt(p) == '*') {
                    int i;
                    ++p;
                    for (i = string.length(); i >= s; --i) if (match(pattern.substring(p), string.substring(i))) return true;
                    break;
                }
                if (pattern.charAt(p) != string.charAt(s)) break;
            }
            p = pattern.indexOf('|', p);
            if (p == -1) return false;
        }
    }

    public static int sameSpan(String str1, String str2) {
        int i;
        for (i = 0; i < str1.length() && i < str2.length() && str1.charAt(i) == str2.charAt(i); ++i) {
        }
        return i;
    }

    public static int charCount(String str, char c) {
        int n = 0;
        for (int i = 0; i < str.length(); ++i) if (str.charAt(i) == c) ++n;
        return n;
    }

    public static String[] splitStr(String str) {
        StringTokenizer st = new StringTokenizer(str);
        int n = st.countTokens();
        String[] strs = new String[n];
        for (int i = 0; i < n; ++i) strs[i] = st.nextToken();
        return strs;
    }

    public static String[] splitStr(String str, char delim) {
        int n = 1;
        int index = -1;
        while (true) {
            index = str.indexOf(delim, index + 1);
            if (index == -1) break;
            ++n;
        }
        String[] strs = new String[n];
        index = -1;
        for (int i = 0; i < n - 1; ++i) {
            int nextIndex = str.indexOf(delim, index + 1);
            strs[i] = str.substring(index + 1, nextIndex);
            index = nextIndex;
        }
        strs[n - 1] = str.substring(index + 1);
        return strs;
    }

    public static String flattenStrarr(String[] strs) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < strs.length; ++i) {
            if (i > 0) sb.append(' ');
            sb.append(strs[i]);
        }
        return sb.toString();
    }

    public static void sortStrings(String[] strings) {
        for (int i = 0; i < strings.length - 1; ++i) {
            for (int j = i + 1; j < strings.length; ++j) {
                if (strings[i].compareTo(strings[j]) > 0) {
                    String t = strings[i];
                    strings[i] = strings[j];
                    strings[j] = t;
                }
            }
        }
    }

    public static int indexOfString(String[] strings, String string) {
        for (int i = 0; i < strings.length; ++i) if (string.equals(strings[i])) return i;
        return -1;
    }

    public static int indexOfStringIgnoreCase(String[] strings, String string) {
        for (int i = 0; i < strings.length; ++i) if (string.equalsIgnoreCase(strings[i])) return i;
        return -1;
    }

    public static boolean equalsStrings(String[] strings1, String[] strings2) {
        if (strings1.length != strings2.length) return false;
        for (int i = 0; i < strings1.length; ++i) if (!strings1[i].equals(strings2[i])) return false;
        return true;
    }

    public static long pow(long a, long b) throws ArithmeticException {
        if (b < 0) throw new ArithmeticException();
        long r = 1;
        while (b != 0) {
            if (odd(b)) r *= a;
            b >>>= 1;
            a *= a;
        }
        return r;
    }

    public static int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return def;
        }
    }

    public static long parseLong(String str, long def) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return def;
        }
    }

    public static String arrayToString(Object o) {
        if (o == null) return "null";
        String cl = o.getClass().getName();
        if (!cl.startsWith("[")) return o.toString();
        StringBuffer sb = new StringBuffer("{ ");
        if (o instanceof byte[]) {
            byte[] ba = (byte[]) o;
            for (int i = 0; i < ba.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append("(byte)");
                sb.append(ba[i]);
            }
        } else if (o instanceof char[]) {
            char[] ca = (char[]) o;
            for (int i = 0; i < ca.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append("'");
                sb.append(ca[i]);
                sb.append("'");
            }
        } else if (o instanceof short[]) {
            short[] sa = (short[]) o;
            for (int i = 0; i < sa.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append("(short)");
                sb.append(sa[i]);
            }
        } else if (o instanceof int[]) {
            int[] ia = (int[]) o;
            for (int i = 0; i < ia.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(ia[i]);
            }
        } else if (o instanceof long[]) {
            long[] la = (long[]) o;
            for (int i = 0; i < la.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(la[i]);
                sb.append("L");
            }
        } else if (o instanceof float[]) {
            float[] fa = (float[]) o;
            for (int i = 0; i < fa.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fa[i]);
                sb.append("F");
            }
        } else if (o instanceof double[]) {
            double[] da = (double[]) o;
            for (int i = 0; i < da.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(da[i]);
                sb.append("D");
            }
        } else if (o instanceof String) {
            String[] sa = (String[]) o;
            for (int i = 0; i < sa.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append("\"");
                sb.append(sa[i]);
                sb.append("\"");
            }
        } else if (cl.startsWith("[L")) {
            Object[] oa = (Object[]) o;
            for (int i = 0; i < oa.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(oa[i]);
            }
        } else if (cl.startsWith("[[")) {
            Object[] aa = (Object[]) o;
            for (int i = 0; i < aa.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(arrayToString(aa[i]));
            }
        } else sb.append("(unknown array type)");
        sb.append(" }");
        return sb.toString();
    }

    public static boolean instanceOf(Object o, Class cl) {
        if (o == null || cl == null) return false;
        Class ocl = o.getClass();
        if (ocl.equals(cl)) return true;
        if (!cl.isInterface()) {
            Class ifs[] = cl.getInterfaces();
            for (int i = 0; i < ifs.length; ++i) if (instanceOf(o, ifs[i])) return true;
        }
        Class scl = cl.getSuperclass();
        if (scl != null) if (instanceOf(o, scl)) return true;
        return false;
    }

    public static boolean even(long n) {
        return (n & 1) == 0;
    }

    public static boolean odd(long n) {
        return (n & 1) != 0;
    }

    public static int countOnes(byte n) {
        return countOnes(n & 0xffL);
    }

    public static int countOnes(int n) {
        return countOnes(n & 0xffffffffL);
    }

    public static int countOnes(long n) {
        int count = 0;
        while (n != 0) {
            if (odd(n)) ++count;
            n >>>= 1;
        }
        return count;
    }

    public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len <= 0) return 0;
        int c = in.read();
        if (c == -1) return -1;
        if (b != null) b[off] = (byte) c;
        int i;
        for (i = 1; i < len; ++i) {
            c = in.read();
            if (c == -1) break;
            if (b != null) b[off + i] = (byte) c;
        }
        return i;
    }

    public static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        int l;
        for (l = 0; l < len; ) {
            int r = read(in, b, off + l, len - l);
            if (r == -1) return -1;
            l += r;
        }
        return l;
    }

    public static URL plainUrl(URL context, String urlStr) throws MalformedURLException {
        URL url = new URL(context, urlStr);
        String fileStr = url.getFile();
        int i = fileStr.indexOf('?');
        if (i != -1) fileStr = fileStr.substring(0, i);
        url = new URL(url.getProtocol(), url.getHost(), url.getPort(), fileStr);
        if ((!fileStr.endsWith("/")) && urlStrIsDir(url.toExternalForm())) {
            fileStr = fileStr + "/";
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), fileStr);
        }
        return url;
    }

    public static URL plainUrl(String urlStr) throws MalformedURLException {
        return plainUrl(null, urlStr);
    }

    public static String baseUrlStr(String urlStr) {
        if (urlStr.endsWith("/")) return urlStr;
        if (urlStrIsDir(urlStr)) return urlStr + "/";
        return urlStr.substring(0, urlStr.lastIndexOf('/') + 1);
    }

    public static String fixDirUrlStr(String urlStr) {
        if (urlStr.endsWith("/")) return urlStr;
        if (urlStrIsDir(urlStr)) return urlStr + "/";
        return urlStr;
    }

    public static boolean urlStrIsDir(String urlStr) {
        if (urlStr.endsWith("/")) return true;
        int lastSlash = urlStr.lastIndexOf('/');
        int lastPeriod = urlStr.lastIndexOf('.');
        if (lastPeriod != -1 && (lastSlash == -1 || lastPeriod > lastSlash)) return false;
        String urlStrWithSlash = urlStr + "/";
        try {
            URL url = new URL(urlStrWithSlash);
            InputStream f = url.openStream();
            f.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean urlStrIsAbsolute(String urlStr) {
        if (urlStr.startsWith("/") || urlStr.indexOf(":/") != -1) return true;
        return false;
    }

    public static String absoluteUrlStr(String urlStr, URL contextUrl) throws MalformedURLException {
        URL url = new URL(contextUrl, urlStr);
        return url.toExternalForm();
    }

    public static String urlDecoder(String encoded) {
        StringBuffer decoded = new StringBuffer();
        int len = encoded.length();
        for (int i = 0; i < len; ++i) {
            if (encoded.charAt(i) == '%' && i + 2 < len) {
                int d1 = Character.digit(encoded.charAt(i + 1), 16);
                int d2 = Character.digit(encoded.charAt(i + 2), 16);
                if (d1 != -1 && d2 != -1) decoded.append((char) ((d1 << 4) + d2));
                i += 2;
            } else if (encoded.charAt(i) == '+') decoded.append(' '); else decoded.append(encoded.charAt(i));
        }
        return decoded.toString();
    }

    public static String base64Encode(byte[] src) {
        StringBuffer encoded = new StringBuffer();
        int i, phase = 0;
        char c = 0;
        for (i = 0; i < src.length; ++i) {
            switch(phase) {
                case 0:
                    c = b64EncodeTable[(src[i] >> 2) & 0x3f];
                    encoded.append(c);
                    c = b64EncodeTable[(src[i] & 0x3) << 4];
                    encoded.append(c);
                    ++phase;
                    break;
                case 1:
                    c = b64EncodeTable[(b64DecodeTable[c] | (src[i] >> 4)) & 0x3f];
                    encoded.setCharAt(encoded.length() - 1, c);
                    c = b64EncodeTable[(src[i] & 0xf) << 2];
                    encoded.append(c);
                    ++phase;
                    break;
                case 2:
                    c = b64EncodeTable[(b64DecodeTable[c] | (src[i] >> 6)) & 0x3f];
                    encoded.setCharAt(encoded.length() - 1, c);
                    c = b64EncodeTable[src[i] & 0x3f];
                    encoded.append(c);
                    phase = 0;
                    break;
            }
        }
        while (phase++ < 3) encoded.append('=');
        return encoded.toString();
    }

    private static char b64EncodeTable[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    private static int b64DecodeTable[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

    public static String base64Encode(String srcString) {
        byte[] src = new byte[srcString.length()];
        srcString.getBytes(0, src.length, src, 0);
        return base64Encode(src);
    }

    public static boolean arraycontains(Object[] array, Object element) {
        for (int i = 0; i < array.length; ++i) if (array[i].equals(element)) return true;
        return false;
    }

    public static int system(String cmd) {
        try {
            return runCommand(cmd).waitFor();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            return -1;
        }
    }

    public static InputStream popenr(String cmd) {
        try {
            return runCommand(cmd).getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public static OutputStream popenw(String cmd) {
        try {
            return runCommand(cmd).getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public static Process runCommand(String cmd) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        String[] shCmd = new String[3];
        shCmd[0] = "/bin/sh";
        shCmd[1] = "-c";
        shCmd[2] = cmd;
        return runtime.exec(shCmd);
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
    }

    public static void copyStream(Reader in, Writer out) throws IOException {
        char[] buf = new char[4096];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
    }

    public static void copyStream(InputStream in, Writer out) throws IOException {
        byte[] buf1 = new byte[4096];
        char[] buf2 = new char[4096];
        int len, i;
        while ((len = in.read(buf1)) != -1) {
            for (i = 0; i < len; ++i) buf2[i] = (char) buf1[i];
            out.write(buf2, 0, len);
        }
    }

    public static void copyStream(Reader in, OutputStream out) throws IOException {
        char[] buf1 = new char[4096];
        byte[] buf2 = new byte[4096];
        int len, i;
        while ((len = in.read(buf1)) != -1) {
            for (i = 0; i < len; ++i) buf2[i] = (byte) buf1[i];
            out.write(buf2, 0, len);
        }
    }

    public static void dumpStack(PrintStream p) {
        (new Throwable()).printStackTrace(p);
    }

    public static void dumpStack() {
        (new Throwable()).printStackTrace();
    }
}
