package Parser;

import FontConverter.AGFtoUnicode;
import java.util.*;
import java.io.*;
import java.net.*;
import Database.*;
import FontConverter.*;

public class HtmlParser {

    private static Vector details;

    private static Vector link_list;

    private static String[] html_tags = { "HTML", "HEAD", "BODY", "DIV", "SPAN", "TITLE", "LINK", "META", "STYLE", "P", "H1, H2, H3, H4, H5, AND H6", "STRONG", "EM", "ABBR", "ACRONYM", "ADDRESS", "BDO", "BLOCKQUOTE", "CITE", "Q", "CODE", "INS", "DEL", "DFN", "KBD", "PRE", "SAMP", "VAR", "BR", "A", "BASE", "IMG", "AREA", "MAP", "OBJECT", "PARAM", "UL", "OL", "LI", "DL", "DT", "DD", "TABLE", "TR", "TD", "TH", "TBODY", "THEAD", "TFOOT", "COL", "COLGROUP", "CAPTION", "FORM", "INPUT", "TEXTAREA", "SELECT", "OPTION", "OPTGROUP", "BUTTON", "LABEL", "FIELDSET", "LEGEND", "SCRIPT", "NOSCRIPT", "B, I, TT, SUB, SUP, BIG, SMALL, HR", "A", "ABBR", "ACRONYM", "ADDRESS", "AREA", "B", "BASE", "BDO", "BIG", "BLOCKQUOTE", "BODY", "BR", "BUTTON", "CAPTION", "CITE", "CODE", "COL", "COLGROUP", "DD", "DEL", "DFN", "DIV", "DL", "DOCTYPE", "DT", "EM", "FIELDSET", "FORM", "H1, H2, H3, H4, H5, AND H6", "HEAD", "HTML", "HR", "I", "IMG", "INPUT", "INS", "KBD", "LABEL", "LEGEND", "LI", "LINK", "MAP", "META", "NOSCRIPT", "OBJECT", "OL", "OPTGROUP", "OPTION", "P", "PARAM", "PRE", "Q", "SAMP", "SCRIPT", "SELECT", "SMALL", "SPAN", "STRONG", "STYLE", "SUB", "SUP", "TABLE", "TBODY", "TD", "TEXTAREA", "TFOOT", "TH", "THEAD", "TITLE", "TR", "TT", "UL", "VAR" };

    private static String[] latin_chars = { "�", "�", "�", "�", "�", "�", "�", "�", "�", "?", "?", "?", "&", "?", "?", "�", "�", "?", "�", "�", "�", "�", "�", "?", "?", "�", "�", "?", "�", "�", "�", "�", "?", "?", "�", "?", "?", "�", "?", "?", "�", "�", "�", "?", "?", "�", "?", "?", "?", "�", "�", "�", "�", "�", "�", "�", "?", "?", "?", "?", "?", "?", "?", "?", "�", "�", "�", "�", "�", "?", "�", "?", "�", "�", "�", "?", "?", "?", "?", ">", "?", "?", "?", "�", "�", "�", "�", "�", "�", "�", "�", "?", "?", "?", "?", "?", "�", "?", "�", "�", "?", "?", "?", "?", "?", "�", "?", "?", "?", "�", "?", "?", "?", "?", "?", "�", "�", "<", "�", "�", "�", "�", "?", "?", "?", "?", "", "�", "?", "?", "�", "?", "?", "�", "�", "?", "?", "�", "�", "�", "�", "�", "�", "�", "�", "?", "?", "?", "?", "?", "?", "?", "�", "�", "�", "�", "�", "�", "?", "�", "�", "�", "?", "�", "?", "?", "?", "?", "?", "?", "�", "�", "?", "?", "?", "?", "?", "?", "\"", "?", "?", "�", "?", "?", "?", "�", "?", "�", "?", "?", "?", "?", "�", "�", "�", "�", "�", "?", "�", "�", "?", "?", "?", "?", "?", "?", "?", "?", "?", "�", "�", "�", "?", "�", "?", "?", "?", "?", "?", "?", "?", "�", "�", "�", "�", "�", "�", "�", "?", "?", "�", "�", "�", "�", "�", "?", "?", "?", "�", "�", "?", "?", "?", "�", "�", "�", "�", "�", "?", "?", " ", " " };

    private static String[] latin_rep = { "&Aacute;", "&aacute;", "&acirc;", "&Acirc;", "&acute;", "&aelig;", "&AElig;", "&Agrave;", "&agrave;", "&alefsym;", "&Alpha;", "&alpha;", "&amp;", "&and;", "&ang;", "&aring;", "&Aring;", "&asymp;", "&Atilde;", "&atilde;", "&Auml;", "&auml;", "&bdquo;", "&Beta;", "&beta;", "&brvbar;", "&bull;", "&cap;", "&Ccedil;", "&ccedil;", "&cedil;", "&cent;", "&chi;", "&Chi;", "&circ;", "&clubs;", "&cong;", "&copy;", "&crarr;", "&cup;", "&curren;", "&dagger;", "&Dagger;", "&dArr;", "&darr;", "&deg;", "&Delta;", "&delta;", "&diams;", "&divide;", "&eacute;", "&Eacute;", "&Ecirc;", "&ecirc;", "&egrave;", "&Egrave;", "&empty;", "&emsp;", "&ensp;", "&epsilon;", "&Epsilon;", "&equiv;", "&Eta;", "&eta;", "&eth;", "&ETH;", "&euml;", "&Euml;", "&euro;", "&exist;", "&fnof;", "&forall;", "&frac12;", "&frac14;", "&frac34;", "&frasl;", "&Gamma;", "&gamma;", "&ge;", "&gt;", "&hArr;", "&harr;", "&hearts;", "&hellip;", "&iacute;", "&Iacute;", "&icirc;", "&Icirc;", "&iexcl;", "&Igrave;", "&igrave;", "&image;", "&infin;", "&int;", "&Iota;", "&iota;", "&iquest;", "&isin;", "&Iuml;", "&iuml;", "&Kappa;", "&kappa;", "&lambda;", "&Lambda;", "&lang;", "&laquo;", "&larr;", "&lArr;", "&lceil;", "&ldquo;", "&le;", "&lfloor;", "&lowast;", "&loz;", "&lrm;", "&lsaquo;", "&lsquo;", "&lt;", "&macr;", "&mdash;", "&micro;", "&middot;", "&minus;", "&Mu;", "&mu;", "&nabla;", "&nbsp;", "&ndash;", "&ne;", "&ni;", "&not;", "&notin;", "&nsub;", "&ntilde;", "&Ntilde;", "&Nu;", "&nu;", "&oacute;", "&Oacute;", "&Ocirc;", "&ocirc;", "&OElig;", "&oelig;", "&ograve;", "&Ograve;", "&oline;", "&omega;", "&Omega;", "&Omicron;", "&omicron;", "&oplus;", "&or;", "&ordf;", "&ordm;", "&Oslash;", "&oslash;", "&Otilde;", "&otilde;", "&otimes;", "&Ouml;", "&ouml;", "&para;", "&part;", "&permil;", "&perp;", "&phi;", "&Phi;", "&Pi;", "&pi;", "&piv;", "&plusmn;", "&pound;", "&Prime;", "&prime;", "&prod;", "&prop;", "&psi;", "&Psi;", "&quot;", "&radic;", "&rang;", "&raquo;", "&rArr;", "&rarr;", "&rceil;", "&rdquo;", "&real;", "&reg;", "&rfloor;", "&Rho;", "&rho;", "&rlm;", "&rsaquo;", "&rsquo;", "&sbquo;", "&Scaron;", "&scaron;", "&sdot;", "&sect;", "&shy;", "&Sigma;", "&sigma;", "&sigmaf;", "&sim;", "&spades;", "&sub;", "&sube;", "&sum;", "&sup;", "&sup1;", "&sup2;", "&sup3;", "&supe;", "&szlig;", "&Tau;", "&tau;", "&there4;", "&Theta;", "&theta;", "&thetasym;", "&thinsp;", "&THORN;", "&thorn;", "&tilde;", "&times;", "&trade;", "&uacute;", "&Uacute;", "&uArr;", "&uarr;", "&ucirc;", "&Ucirc;", "&Ugrave;", "&ugrave;", "&uml;", "&upsih;", "&upsilon;", "&Upsilon;", "&uuml;", "&Uuml;", "&weierp;", "&xi;", "&Xi;", "&yacute;", "&Yacute;", "&yen;", "&yuml;", "&Yuml;", "&Zeta;", "&zeta;", "&zwj;", "&zwnj;" };

    static int stylelen = -1;

    public static String readme;

    /** Creates a new instance of HtmlParser */
    public HtmlParser() {
        details = new Vector();
        link_list = new Vector();
    }

    public static boolean isHtmlTag(String s) {
        for (int i = 0; i < html_tags.length; i++) if (s.toUpperCase().startsWith(html_tags[i]) || s.toUpperCase().equals("/" + html_tags[i])) {
            return true;
        }
        return false;
    }

    public static boolean isSanskrit(String s) {
        int c;
        for (int i = 0; i < s.length(); i++) {
            c = (int) s.charAt(i);
            if (c >= 0x0900 && c <= 0x097f) {
                return true;
            }
        }
        return false;
    }

    public static void extractLink(String a_tag, String urlreferer, String hosturl) {
        StringTokenizer link_tokens = new StringTokenizer(a_tag, " \'\"");
        while (link_tokens.hasMoreTokens()) {
            String link = link_tokens.nextToken().trim();
            if (link.startsWith("HREF") || link.startsWith("href")) {
                if (link.endsWith("=")) link = link_tokens.nextToken().trim(); else link = link.substring(link.indexOf("=") + 1);
                if (link.indexOf("javascript") == -1 && link.indexOf("yahoo") == -1 && !link.endsWith(".asx")) {
                    if (!link.startsWith("http://")) {
                        if (link.startsWith("/")) link = "http://" + hosturl + link; else if (urlreferer.endsWith("/")) link = urlreferer + link; else link = urlreferer.substring(0, urlreferer.lastIndexOf("/")) + "/" + link;
                    }
                    if (link.indexOf(".pdf") == -1 && link.indexOf(".wmv") == -1 && link.indexOf(".exe") == -1 && link.indexOf(".zip") == -1 && link.indexOf(".tar") == -1) link_list.add(new String(link));
                }
            }
        }
    }

    public static String extractCssLink(String link_tag, String urlreferer, String hosturl) {
        StringTokenizer link_tokens = new StringTokenizer(link_tag, " =\'\"");
        String link = "";
        while (link_tokens.hasMoreTokens()) {
            link = link_tokens.nextToken().trim();
            if (link.startsWith("HREF") || link.startsWith("href")) {
                link = link_tokens.nextToken().trim();
                if (!link.startsWith("http://")) {
                    if (link.startsWith("/")) link = "http://" + hosturl + link; else link = "http://" + hosturl + "/" + link;
                }
                break;
            }
        }
        return link;
    }

    public static String getCssFile(String url) {
        StringBuffer buffer = new StringBuffer();
        try {
            buffer = new StringBuffer();
            URL urlToCrawl = new URL(url);
            URLConnection urlToCrawlConnection = urlToCrawl.openConnection();
            urlToCrawlConnection.setRequestProperty("User-Agent", "USER_AGENT");
            urlToCrawlConnection.setRequestProperty("Referer", "REFERER");
            urlToCrawlConnection.setUseCaches(false);
            InputStreamReader isr = new InputStreamReader(urlToCrawlConnection.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String str;
            while ((str = in.readLine()) != null) buffer.append(str);
            FileOutputStream fos = new FileOutputStream("c:\\downloads\\" + System.currentTimeMillis() + ".css");
            Writer out = new OutputStreamWriter(fos);
            out.write(buffer.toString());
            out.close();
        } catch (Exception e) {
            System.out.println("Error Downloading css file" + e);
        }
        return buffer.toString();
    }

    public static String[][] getEthopicStyleName(String style) {
        String[][] stylenames = new String[10][2];
        StringTokenizer tokens = new StringTokenizer(style, "{}");
        String prev_token = "";
        String token;
        int i = 0;
        stylelen = 0;
        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken().toUpperCase().trim();
            if (token.indexOf("FONT-FAMILY") > -1) {
                StringTokenizer styleTokens = new StringTokenizer(token, ":;");
                while (styleTokens.hasMoreTokens()) {
                    String t = styleTokens.nextToken().trim();
                    if (t.equals("FONT-FAMILY")) {
                        String font = styleTokens.nextToken().trim();
                        if (font.startsWith("VG2")) {
                            stylenames[i][0] = prev_token;
                            stylenames[i][1] = "VG2";
                            i++;
                        } else if (font.startsWith("Visual Geez 2")) {
                            stylenames[i][0] = prev_token;
                            stylenames[i][1] = "VG2";
                            i++;
                        } else if (font.startsWith("AGF")) {
                            stylenames[i][0] = prev_token;
                            stylenames[i][1] = "AGF";
                            System.out.println(prev_token);
                            i++;
                        } else if (font.startsWith("ET")) {
                            stylenames[i][0] = prev_token;
                            stylenames[i][1] = "ET";
                            System.out.println(prev_token);
                            i++;
                        } else if (font.startsWith("ALXethiopian")) {
                            stylenames[i][0] = prev_token;
                            stylenames[i][1] = "ALXethiopian";
                            System.out.println(prev_token);
                            i++;
                        }
                    }
                }
            }
            prev_token = token;
        }
        stylelen = i - 1;
        return stylenames;
    }

    static String getCurrentFont(String font_tag) {
        String font = "UNKNOWN";
        String token;
        if (font_tag.indexOf("CLASS") > -1) {
            StringTokenizer tokens = new StringTokenizer(font_tag, " =\"\'");
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken().trim();
                if (token.equals("CLASS")) {
                    font = tokens.nextToken().trim();
                    break;
                }
            }
        }
        return font;
    }

    public static String removeUnwantedChars(String content) {
        content = content.replace('፟', ' ');
        content = content.replace('፠', ' ');
        content = content.replace('፡', ' ');
        content = content.replace('።', ' ');
        content = content.replace('፣', ' ');
        content = content.replace('፤', ' ');
        content = content.replace('፥', ' ');
        content = content.replace('፦', ' ');
        content = content.replace('፧', ' ');
        content = content.replace('፨', ' ');
        return content;
    }

    public static String replaceSpecialChars(String str) {
        for (int i = 0; i < latin_chars.length; i++) {
            str = str.replaceAll(latin_rep[i], latin_chars[i]);
        }
        return str;
    }

    public static Vector parse(String htmldoc, String urlreferer, String hosturl, long date_modified, String size) throws Exception {
        StringTokenizer tokens = new StringTokenizer(htmldoc, "<>", true);
        String prev_token = new String("");
        String title = "Untitled";
        StringBuffer content = new StringBuffer();
        String[][] stylenames = new String[10][2];
        boolean ethiopic = false;
        Indexer indexer = new Indexer();
        stylelen = 0;
        link_list.clear();
        while (tokens.hasMoreTokens()) {
            String token = replaceSpecialChars(tokens.nextToken().trim());
            String token_up = token.toUpperCase();
            if (prev_token.equals("TITLE")) {
                title = tokens.nextToken().trim();
                if (title.length() == 0 || title.equals("<")) {
                    title = "Untitled";
                }
                StringTokenizer title_tokens = new StringTokenizer(title.toLowerCase(), " ,.;:`\'\"?<>[]{}()&|!~*/-+_=@#$^%");
                while (title_tokens.hasMoreTokens()) {
                    String word = title_tokens.nextToken().trim();
                    if (word != null && word.length() > 0 && details.indexOf(word) == -1) {
                        details.add(new String(word));
                    }
                }
                if (details.size() > 0) {
                    details.clear();
                }
            } else if (prev_token.startsWith("H") && prev_token.length() > 1 && Character.isDigit(prev_token.charAt(1))) {
                String topic = tokens.nextToken().trim();
                StringTokenizer topic_tokens = new StringTokenizer(topic.toLowerCase(), " ,.;:`\'\"?<>[]{}()&|!~*/-+_=@#$^%");
                while (topic_tokens.hasMoreTokens()) {
                    String word = topic_tokens.nextToken().trim();
                    if (word != null && word.length() > 0 && details.indexOf(word) == -1) {
                        details.add(new String(word));
                    }
                }
                if (details.size() > 0) {
                    details.clear();
                }
            } else if (prev_token.equals("<") && token_up.startsWith("LINK")) {
                if (token_up.indexOf("STYLESHEET") > -1 && token_up.indexOf("TEXT/CSS") > -1) {
                    String css_loc = extractCssLink(token, urlreferer, hosturl);
                    String css_def = getCssFile(css_loc);
                    stylenames = getEthopicStyleName(css_def);
                }
            } else if (prev_token.equals("<") && token_up.startsWith("FONT")) {
                String stack[] = new String[15];
                int i = -1;
                if (token_up.indexOf("CLASS") != -1) {
                    prev_token = token_up;
                    stack[++i] = getCurrentFont(token_up);
                    while (tokens.hasMoreTokens()) {
                        token = replaceSpecialChars(tokens.nextToken().trim());
                        token_up = token.toUpperCase();
                        if (token_up.startsWith("FONT")) {
                            stack[++i] = getCurrentFont(token_up);
                        } else if (token_up.equals("/FONT")) {
                            if (--i < 0) break;
                        } else if (!token_up.equals("<") && !token_up.equals(">") && !isHtmlTag(token_up)) {
                            for (int k = 0; k < stylelen + 1; k++) {
                                if (stylenames[k][0].endsWith(stack[i])) {
                                    if (stylenames[k][1].equals("VG2")) {
                                        String converted = VG2toUnicode.Convert(token);
                                        System.out.println(converted);
                                        content.append(converted + "\n");
                                        break;
                                    } else if (stylenames[k][1].equals("AGF")) {
                                        String converted = AGFtoUnicode.Convert(token);
                                        System.out.println(converted);
                                        content.append(converted + "\n");
                                        break;
                                    } else if (stylenames[k][1].equals("ALEXethiopian")) {
                                        String converted = ALEXEthiotoUnicode.Convert(token);
                                        System.out.println(converted);
                                        content.append(converted + "\n");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (prev_token.equals("<") && (token.startsWith("A ") || token.startsWith("a "))) {
                extractLink(token, urlreferer, hosturl);
            } else if (isSanskrit(token)) {
                content.append(token + "\n");
            }
            prev_token = token_up;
        }
        HtmlIndexer.indexDocument(urlreferer, title, removeUnwantedChars(content.toString()), String.valueOf(date_modified), size);
        return link_list;
    }

    public static Vector parseUTF(String htmldoc, String urlreferer, String hosturl, long date_modified, String size) throws Exception {
        StringTokenizer tokens = new StringTokenizer(htmldoc, "<>", true);
        String prev_token = new String("");
        String title = "Untitled";
        StringBuffer content = new StringBuffer();
        String[][] stylenames = new String[10][2];
        System.out.println(urlreferer + ":" + hosturl);
        boolean ethiopic = false;
        boolean body = false;
        Indexer indexer = new Indexer();
        stylelen = 0;
        link_list.clear();
        while (tokens.hasMoreTokens()) {
            String token = replaceSpecialChars(tokens.nextToken().trim());
            String token_up = token.toUpperCase();
            if (prev_token.equals("<") && (token.startsWith("A ") || token.startsWith("a "))) {
                extractLink(token, urlreferer, hosturl);
            } else if (prev_token.equals("<") && token.toLowerCase().startsWith("body")) {
                body = true;
            } else if (prev_token.equals("TITLE")) {
                title = tokens.nextToken().trim();
                if (title.length() == 0 || title.equals("<")) {
                    title = "Untitled";
                }
            } else if (isSanskrit(token)) {
                if (body) content.append(token + "\n");
            }
            prev_token = token_up;
        }
        readme = content.toString();
        System.out.println("Inside parser and the contents might be " + size + content);
        HtmlIndexer.indexDocument(urlreferer, title, removeUnwantedChars(content.toString()), String.valueOf(date_modified), size);
        return link_list;
    }
}
