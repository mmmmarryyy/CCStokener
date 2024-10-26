package com.myjavatools.lib;

import static com.myjavatools.lib.Files.writeToFile;
import static com.myjavatools.lib.Strings.join;
import static com.myjavatools.lib.foundation.Objects.isEmpty;
import static com.myjavatools.lib.foundation.Objects.oneOf;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Web {

    protected static Pattern HTML_CHARSET_PATTERN = Pattern.compile(".*<meta[^>]+content=\"[^\"]*charset=([^\"\\s]*)\".*", Pattern.CASE_INSENSITIVE);

    /**
   * Detects charset from html contents
   *
   * @param s the char sequence with html content
   * @return charset name
   *
   * <br><br><b>Examples</b>:
   * <li><code>getHtmlCharset("<html><head><meta HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset=shift-jis\">")</code>
   * returns "shift-jis".</li>
   */
    public static String getHtmlCharset(final CharSequence s) {
        if (isEmpty(s)) {
            return null;
        }
        final Matcher matcher = HTML_CHARSET_PATTERN.matcher(s);
        return matcher.matches() ? matcher.group(1).toLowerCase() : null;
    }

    protected static Pattern XML_CHARSET_PATTERN = Pattern.compile(".*<\\?xml[^>]+encoding=\"([^\"]*)\".*", Pattern.CASE_INSENSITIVE);

    /**
   * Detects charset from xml contents
   *
   * @param s the char sequence with html content
   * @return charset name
   *
   * <br><br><b>Examples</b>:
   * <li><code>getXmlCharset("&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?>")</code>
   * returns "utf-8".</li>
   */
    public static String getXmlCharset(final CharSequence s) {
        if (isEmpty(s)) {
            return null;
        }
        final Matcher matcher = XML_CHARSET_PATTERN.matcher(s);
        return matcher.matches() ? matcher.group(1).toLowerCase() : null;
    }

    /**
   * A short conversion table for IANA charsets and Java 1.3 "encodings".
   * You do not need them if you use Java 1.4 (and up) java.nio.
   */
    private static Map<String, String> CHARSET_TO_ENCODING;

    private static Map<String, String> ENCODING_TO_CHARSET;

    private static void addCharset(final String charset, final String encoding) {
        CHARSET_TO_ENCODING.put(charset, encoding);
        ENCODING_TO_CHARSET.put(encoding, charset);
    }

    static {
        CHARSET_TO_ENCODING = new LinkedHashMap<String, String>();
        ENCODING_TO_CHARSET = new LinkedHashMap<String, String>();
        addCharset("iso-2022-jp", "ISO2022JP");
        addCharset("iso-2022-cn-cns", "ISO2022CN_CNS");
        addCharset("iso-2022-cn-gb", "ISO2022CN_GB");
        addCharset("iso-2022-kr", "ISO2022KR");
        addCharset("iso-8859-1", "ISO8859_1");
        addCharset("iso-8859-2", "ISO8859_2");
        addCharset("iso-8859-3", "ISO8859_3");
        addCharset("iso-8859-4", "ISO8859_4");
        addCharset("iso-8859-5", "ISO8859_5");
        addCharset("iso-8859-6", "ISO8859_6");
        addCharset("iso-8859-7", "ISO8859_7");
        addCharset("iso-8859-8", "ISO8859_8");
        addCharset("iso-8859-9", "ISO8859_9");
        addCharset("iso-8859-13", "ISO8859_13");
        addCharset("shift_jis", "SJIS");
        addCharset("tis-620", "TIS620");
        addCharset("utf-8", "UTF8");
        addCharset("windows-1250", "Cp1250");
        addCharset("windows-1252", "Cp1252");
        addCharset("windows-1253", "Cp1253");
        addCharset("windows-1254", "Cp1254");
        addCharset("windows-1255", "Cp1255");
        addCharset("windows-1256", "Cp1256");
        addCharset("windows-1257", "Cp1257");
        addCharset("windows-1258", "Cp1258");
        addCharset("windows-31j", "MS932");
        addCharset("windows-949", "MS949");
        addCharset("windows-950", "MS950");
    }

    ;

    /**
   * gets IANA charset given Java encoding
   * @param encoding the Java charset
   * @return encoding name
   *
   * @deprecated should use java.nio.Charset
   */
    @Deprecated
    public static String getCharsetByEncoding(final String encoding) {
        if (encoding == null) {
            return "";
        }
        return oneOf(ENCODING_TO_CHARSET.get(encoding), encoding.replace('_', '-'), "");
    }

    /**
   * gets Java encoding given an IANA charset
   * @param charset the IANA charset
   * @return encoding name
   *
   */
    public static String getEncodingByCharset(final String charset) {
        if (charset == null) {
            return "";
        }
        final String candidate1 = charset.toLowerCase();
        final String candidate2 = candidate1.replace('-', '_');
        return oneOf(CHARSET_TO_ENCODING.get(candidate1), CHARSET_TO_ENCODING.get(candidate2), charset.replace('-', '_'), "");
    }

    /**
   * gets Java encoding of HTML contents
   * @param s HTML data
   * @return its most probable Java encoding
   *
   * <br><br><b>Examples</b>:
   * <li><code>getHtmlCharset("<html><head><meta HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset=shift-jis\">")</code>
   * returns "SJIS".</li>
   *
   */
    public static String getHtmlEncoding(final CharSequence s) {
        return getEncodingByCharset(getHtmlCharset(s));
    }

    /**
   * gets Java encoding of XML data
   * @param s XML data (first line is enough)
   * @return Java encoding to decode the data
   *
   * <br><br><b>Example</b>:
   * <li><code>getXmlEncoding("?xml version=\"1.0\" encoding=\"UTF-8"?>")</code>
   * will return "UTF8".</li>
   *
   */
    public static String getXmlEncoding(final CharSequence s) {
        final String charset = getXmlCharset(s);
        return getEncodingByCharset(charset);
    }

    /**
   * gets an input stream for a given url
   * @param url the universal resource locator
   * @return the input stream
   * @throws java.io.IOException
   * @throws java.lang.InstantiationException
   *
   * <br><br><b>Example</b>:
   * <li><code>getUrlInputStream(new URL("http://www.google.com/images/logo.gif"))</code>
   * will return an input stream that contains the Google gif.</li>
   */
    public static InputStream getUrlInputStream(final java.net.URL url) throws java.io.IOException, java.lang.InstantiationException {
        final java.net.URLConnection conn = url.openConnection();
        conn.connect();
        final InputStream input = url.openStream();
        if (input == null) {
            throw new java.lang.InstantiationException("Url " + url + " does not provide data.");
        }
        return input;
    }

    /**
   * Downloads a file from a specified URL
   *
   * @param url URL that points to a resource
   * @param filename the name of the file to store the contents
   * @return error message; an empty string if none
   *
   * <br><br><b>Example</b>:
   * <li><code>downloadFile(new URL("http://www.google.com/images/logo.gif"), "googlelogo.gif")</code>
   * will download the gif, store it into the file, and return an empty string.</li>
   *
   */
    public static String downloadFile(final java.net.URL url, final String filename) {
        try {
            writeToFile(getUrlInputStream(url), filename);
        } catch (final Exception e) {
            return "error: " + e;
        }
        return "";
    }

    /**
   * Downloads a file from a specified URL
   *
   * @param url URL string that points to a resource
   * @param filename the name of the file to store the contents
   * @return error message; an empty string if none
   *
   * <br><br><b>Example</b>:
   * <li><code>downloadFile("http://www.google.com/images/logo.gif", "googlelogo.gif")</code>
   * will download the gif, store it into the file, and return an empty string.</li>
   *
   */
    public static String downloadFile(final String url, final String filename) {
        try {
            downloadFile(new java.net.URL(url), filename);
        } catch (final Exception e) {
            return "error: " + e;
        }
        return "";
    }

    /**
   * sendMail sends emails.
   *
   * Borrowed from Slavik Dimitrovich, Richmond, Virginia
   * @see <a href="http://www.devx.com/java/free/tip.asp?content_id=3685">The original article</a>
   *
   * @param from sender address
   * @param to receiver address
   * @param subj message subject
   * @param message message body
   * @throws java.io.MalformedURLException
   * @throws java.io.IOException
   */
    public static void sendMail(final String from, final String to, final String subj, final String message) throws MalformedURLException, IOException {
        final URLConnection connection = new URL("mailto:" + to).openConnection();
        connection.setDoInput(false);
        connection.setDoOutput(true);
        connection.connect();
        final PrintWriter out = new PrintWriter(connection.getOutputStream());
        out.println("From: \"" + from + "\" <" + from + ">");
        out.println("To: " + to);
        out.println("Subject: " + subj);
        out.println();
        out.println(message);
        out.close();
    }

    /**
   * converts a name-value pair into an element of url string
   * @param name parameter name
   * @param value parameter value
   * @return a string like "name=value", where value is url-encoded<br>
   * returns an empty string if value is empty
   *
   * <br><br><b>Example</b>:
   * <li><code>urlEncode("dir", "C:\\Program Files"")</code>
   * will return "dir=C%3A%5CProgram+Files".</li>
   *
   */
    public static final String urlEncode(final String name, final String value) {
        try {
            return isEmpty(value) ? "" : (name + "=" + URLEncoder.encode(value, "UTF8"));
        } catch (final UnsupportedEncodingException ex) {
            return null;
        }
    }

    /**
   * produces a GET request string for a uri and a collection of parameters
   * @param href url where to send the request
   * @param nvPairList a collection of (already url-encoded) name-value pair
   *        strings that look like "name=value"
   * @return formatted url string, see example
   *
   * <br><br><b>Example</b>:
   * <li><code>urlEncode("http://example.myjavatools.com/mycomputer",
   * new ArrayList(new String[] {"dir=C%3A%5CProgram+Files", "cmd=dir+*"}))</code>
   * will return "http://example.myjavatools.com/mycomputer?dir=C%3A%5CProgram+Files&cmd=dir+*".</li>
   */
    public static final String url(final String href, final Collection nvPairList) {
        final String stringifiedAttributes = join("&", nvPairList);
        return href + (isEmpty(stringifiedAttributes) ? "" : "?" + stringifiedAttributes);
    }

    /**
   * produces a GET request string for a uri and request parameters
   * @param uri where to send the request
   * @param paramvalue String...
   * (etc)
   * @return formatted url string, see examples
   *
   * <br><br><b>Examples</b>:
   * <li><code>urlEncode("http://example.myjavatools.com/mycomputer",
   * "dir, "C:\Program Files"))</code>
   * will return "http://example.myjavatools.com/mycomputer?dir=C%3A%5CProgram+Files".</li>
   *
   * <li><code>urlEncode("http://example.myjavatools.com/mycomputer",
   * "dir, "C:\Program Files", "cmd", "dir *")</code>
   * will return "http://example.myjavatools.com/mycomputer?dir=C%3A%5CProgram+Files&cmd=dir+*".</li>
   *
   * <li><code>urlEncode("http://mycomputer",
   * "dir, "C:\Program Files", "cmd", "dir *", "login", "root", "password", "urowned")</code>
   * will return "http://mycomputer?dir=C%3A%5CProgram+Files&cmd=dir+*&login=root&password=urowned".</li>
   */
    public static final String url(final String uri, final String... paramvalue) {
        final StringBuffer buffer = new StringBuffer(uri);
        String separator = "?";
        for (int i = 0; i < paramvalue.length; i += 2) {
            final String nvpair = urlEncode(paramvalue[i], paramvalue[i + 1]);
            if (!isEmpty(nvpair)) {
                buffer.append(separator);
                buffer.append(nvpair);
                separator = "&";
            }
        }
        return buffer.toString();
    }

    /**
   * converts a string into something that could be literally placed into a web page,
   * that is, replaces CR with CRLF (to be polite with browser's 'view source'),
   * replaces all '<' with "&lt;", and html-encodes (&#dddd;) characters above low-ascii.
   *
   * @param s string to convert
   * @return converted string
   */
    public static final String toWebReadable(final String s) {
        if (isEmpty(s)) {
            return "";
        }
        final LineNumberReader in = new LineNumberReader(new StringReader(s));
        final StringBuffer out = new StringBuffer();
        String buf;
        try {
            while ((buf = in.readLine()) != null) {
                for (int i = 0; i < buf.length(); i++) {
                    final char c = buf.charAt(i);
                    final int k = c;
                    if (c == '&') {
                        out.append("&amp;");
                    } else if (c == '<') {
                        out.append("&lt;");
                    } else if (c > 0x7f) {
                        out.append("&#" + k + ";");
                    } else {
                        out.append(c);
                    }
                }
                out.append("\r\n");
            }
        } catch (final Exception e) {
        }
        ;
        return out.toString();
    }

    /**
   * surrounds a string with single quotes (apostrophes), which is convenient for
   * generating Javascript code, but just senseless otherwise.
   *
   * @param s string to quote
   * @return "'" + s + "'"
   */
    public static final String quote(final String s) {
        return "'" + s + "'";
    }
}
