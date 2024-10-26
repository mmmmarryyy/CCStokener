package net.dongliu.jalus.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtils {

    /**
	 * URL拼接
	 * @param url1
	 * @param url2
	 * @return
	 */
    public static String connectUrl(String base, String... urlPeice) {
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        String last = base;
        for (String s : urlPeice) {
            if (s == null || s.equals("")) {
                continue;
            }
            if (last.endsWith("/") && s.startsWith("/")) {
                sb.append(s.substring(1));
            } else if (!last.endsWith("/") && !s.startsWith("/")) {
                sb.append("/").append(s);
            } else {
                sb.append(s);
            }
            last = s;
        }
        return sb.toString();
    }

    /**
	 * 获取参数值。参数形式如/post/123,或者/post/123/
	 * @param parameterName
	 * @return
	 */
    public static String getParameter(String uri, String parameterName) {
        String temp = "/" + parameterName + "/";
        int start = uri.indexOf(temp);
        if (start == -1) {
            return null;
        }
        start += temp.length();
        int end = uri.indexOf("/", start);
        if (end == -1) {
            end = uri.length();
        }
        if (end <= start) {
            return null;
        }
        return uri.substring(start, end);
    }

    /**
	 * 过滤HTML标签
	 * @param html
	 * @return
	 */
    public static String escapeHtmlTag(String html) {
        if (html == null) return null;
        html = html.replaceAll("&", "&amp;");
        html = html.replaceAll("<", "&lt;");
        html = html.replace(">", "&gt;");
        html = html.replaceAll("\r\n", "<br>");
        html = html.replaceAll("\n", "<br>");
        html = html.replaceAll(" ", "&nbsp;");
        html = html.replace("\"", "&quot");
        return html;
    }

    public static String removeHtmlTag(String content) {
        return content.replaceAll("<[^>]*?>", "");
    }

    public static String htmlToText(String content) {
        content = content.replaceAll("&amp;", "&");
        content = content.replaceAll("&lt;", "<");
        content = content.replaceAll("<br.*?(/)>", "\n");
        content = content.replaceAll("&nbsp;", " ");
        content = content.replace("&quot", "\"");
        content = content.replace("&gt;", ">");
        content = content.replace("&reg;", "@");
        content = content.replace("&ldquo;", "\"");
        content = content.replace("&hellip;", "…");
        return content;
    }

    /**
	 * 支持一下UBB标签：
	 * [quote=author] [/quote]
	 * */
    public static String ubbtohtml(String content) {
        if (content.indexOf("[/quote]") < 0) {
            return content;
        }
        int startPos = 0;
        StringBuilder newContent = new StringBuilder();
        int openCodeTagBegin = -1;
        int openCodeTagEnd = -1;
        int closeCodeTagBegin = -1;
        String item = "";
        content = content.replaceAll("<br>(\\[quote\\]|\\[/quote\\])", "$1");
        content = content.replaceAll("(\\[quote\\]|\\[/quote\\])<br>", "$1");
        while ((openCodeTagBegin = content.indexOf("[quote]", startPos)) >= 0) {
            openCodeTagEnd = openCodeTagBegin + "[quote]".length();
            if ((closeCodeTagBegin = content.indexOf("[/quote]", openCodeTagEnd)) >= 0) {
                item = content.substring(openCodeTagEnd, closeCodeTagBegin);
            } else {
                break;
            }
            newContent.append(content.substring(startPos, openCodeTagBegin));
            newContent.append("<div class=quote>" + item + "</div>");
            startPos = closeCodeTagBegin + "[/quote]".length();
        }
        newContent.append(content.substring(startPos));
        return newContent.toString();
    }

    public static String htmltoubb(String content) {
        return content;
    }

    /**
	 * 处理文章中的[code=java][/code]标签
	 * @param content
	 * @return
	 */
    public static String tranlateSourceCode(String content) {
        if (content.indexOf("[/code]") < 0) {
            return content;
        }
        int startPos = 0;
        StringBuilder newContent = new StringBuilder();
        int openCodeTagBegin = -1;
        int openCodeTagEnd = -1;
        int closeCodeTagBegin = -1;
        String lang = "";
        String source = "";
        while ((openCodeTagBegin = content.indexOf("[code=", startPos)) >= 0) {
            openCodeTagEnd = content.indexOf("]", openCodeTagBegin);
            if ((openCodeTagEnd = content.indexOf("]", openCodeTagBegin)) >= 0) {
                lang = content.substring(openCodeTagBegin + 6, openCodeTagEnd);
            } else {
                break;
            }
            if ((closeCodeTagBegin = content.indexOf("[/code]", openCodeTagEnd)) >= 0) {
                source = content.substring(openCodeTagEnd + 1, closeCodeTagBegin);
            } else {
                break;
            }
            newContent.append(content.substring(startPos, openCodeTagBegin));
            newContent.append(processSourceCode(lang, source));
            startPos = closeCodeTagBegin + 7;
        }
        newContent.append(content.substring(startPos));
        return newContent.toString();
    }

    public static String processSourceCode(String lang, String source) {
        source.replaceAll("\t", "    ");
        try {
            String postStr = "lang=" + URLEncoder.encode(lang, "UTF-8") + "&source=" + URLEncoder.encode(source, "UTF-8");
            URL url = new URL("http://xinetools.appspot.com/syntaxhighlight");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.getOutputStream().write(postStr.getBytes());
            connection.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return source;
        }
    }

    /**
	 * 拼接带参数的URL
	 * @param base    Cannot be null
	 * @param parameterMap Cannot be null
	 * @return
	 */
    public static String makeUrl(String base, Map<String, String> parameterMap) {
        StringBuilder sb = new StringBuilder(base);
        sb.append("?");
        for (String key : parameterMap.keySet()) {
            String value = parameterMap.get(key);
            if (value != null) {
                sb.append(key);
                sb.append("=");
                sb.append(value);
                sb.append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
