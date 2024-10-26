package pkHtml;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.JOptionPane;
import javax.swing.text.Segment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Several tools that depends on JDK only.
 *
 * @author Matthieu Casanova
 * @version $Id: HtmlUtilities.java 14923 2009-04-13 18:40:55Z shlomy $
 * @since 4.3pre5
 */
public class HtmlUtilities {

    private HtmlUtilities() {
    }

    /**
	 * Escapes newlines, tabs, backslashes, and quotes in the specified
	 * string.
	 * @param str The string
	 * @since jEdit 4.3pre15
	 */
    public static String charsToEscapes(String str) {
        return charsToEscapes(str, "\n\t\\\"'");
    }

    /**
	 * Escapes the specified characters in the specified string.
	 * @param str The string
	 * @param toEscape Any characters that require escaping
	 * @since jEdit 4.3pre15
	 */
    public static String charsToEscapes(String str, String toEscape) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (toEscape.indexOf(c) != -1) {
                if (c == '\n') buf.append("\\n"); else if (c == '\t') buf.append("\\t"); else {
                    buf.append('\\');
                    buf.append(c);
                }
            } else buf.append(c);
        }
        return buf.toString();
    }

    /**
	 * Compares two strings.<p>
	 *
	 * Unlike <function>String.compareTo()</function>,
	 * this method correctly recognizes and handles embedded numbers.
	 * For example, it places "My file 2" before "My file 10".<p>
	 *
	 * @param str1 The first string
	 * @param str2 The second string
	 * @param ignoreCase If true, case will be ignored
	 * @return negative If str1 &lt; str2, 0 if both are the same,
	 * positive if str1 &gt; str2
	 * @since jEdit 4.3pre5
	 */
    public static int compareStrings(String str1, String str2, boolean ignoreCase) {
        char[] char1 = str1.toCharArray();
        char[] char2 = str2.toCharArray();
        int len = Math.min(char1.length, char2.length);
        for (int i = 0, j = 0; i < len && j < len; i++, j++) {
            char ch1 = char1[i];
            char ch2 = char2[j];
            if (Character.isDigit(ch1) && Character.isDigit(ch2) && ch1 != '0' && ch2 != '0') {
                int _i = i + 1;
                int _j = j + 1;
                for (; _i < char1.length; _i++) {
                    if (!Character.isDigit(char1[_i])) {
                        break;
                    }
                }
                for (; _j < char2.length; _j++) {
                    if (!Character.isDigit(char2[_j])) {
                        break;
                    }
                }
                int len1 = _i - i;
                int len2 = _j - j;
                if (len1 > len2) return 1; else if (len1 < len2) return -1; else {
                    for (int k = 0; k < len1; k++) {
                        ch1 = char1[i + k];
                        ch2 = char2[j + k];
                        if (ch1 != ch2) return ch1 - ch2;
                    }
                }
                i = _i - 1;
                j = _j - 1;
            } else {
                if (ignoreCase) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
                if (ch1 != ch2) return ch1 - ch2;
            }
        }
        return char1.length - char2.length;
    }

    /**
	 * Adds "file:" infront. Replaces '\' with '/'.
	 *
	 * @modified 2010.05.12
	 * @since 2010.05.12 (v00.02.02)
	 * @author HoKoNoUmo
	 */
    public static String createUrlString(String sPath) {
        sPath = sPath.replace('\\', '/');
        if (sPath.startsWith("http")) {
            return sPath;
        } else if (!sPath.startsWith("file:")) {
            sPath = "file:" + sPath;
        }
        return sPath;
    }

    /**
	 * INPUT: an SFI: h0.1.4p4<br/>
	 * OUTPUT: the level (= number of periods) of heading elements.
	 *
	 * @modified 2010.08.15
	 * @since 2010.08.15 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static int findHeadingLevel(String sSFI) {
        int niC = 0;
        if (sSFI.indexOf("p") != -1) sSFI = sSFI.substring(0, sSFI.indexOf("p"));
        for (int i = 0; i < sSFI.length(); i++) {
            if (sSFI.charAt(i) == 46) {
                niC++;
            }
        }
        return niC;
    }

    /**
	 * INPUT: file:g:/file1/htmlmgr/doc/index.html#ifiSFI <br/>
	 * OUTPUT: ifiSFI or empty-string if no FragmentIdentifier.
	 *
	 * @modified 2010.08.18
	 * @since 2010.08.18 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String getFIfromUrlString(String sUrl) {
        if (sUrl.indexOf("#") != -1) return sUrl.substring(sUrl.indexOf("#") + 1); else return "";
    }

    /**
	 * @param str A java string
		 * @return the leading whitespace of that string, for indenting subsequent lines.
	 * @since jEdit 4.3pre10
	 */
    public static String getIndentString(String str) {
        StringBuilder indentString = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!Character.isWhitespace(ch)) break;
            indentString.append(ch);
        }
        return indentString.toString();
    }

    /**
	 * Returns the virtual column number (taking tabs into account) of the
	 * specified offset in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 */
    public static int getVirtualWidth(Segment seg, int tabSize) {
        int virtualPosition = 0;
        for (int i = 0; i < seg.count; i++) {
            char ch = seg.array[seg.offset + i];
            if (ch == '\t') {
                virtualPosition += tabSize - virtualPosition % tabSize;
            } else {
                ++virtualPosition;
            }
        }
        return virtualPosition;
    }

    /**
	 * Returns the array offset of a virtual column number (taking tabs
	 * into account) in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 * @param column The virtual column number
	 * @param totalVirtualWidth If this array is non-null, the total
	 * virtual width will be stored in its first location if this method
	 * returns -1.
	 *
	 * @return -1 if the column is out of bounds
	 */
    public static int getOffsetOfVirtualColumn(Segment seg, int tabSize, int column, int[] totalVirtualWidth) {
        int virtualPosition = 0;
        for (int i = 0; i < seg.count; i++) {
            char ch = seg.array[seg.offset + i];
            if (ch == '\t') {
                int tabWidth = tabSize - virtualPosition % tabSize;
                if (virtualPosition >= column) return i; else virtualPosition += tabWidth;
            } else {
                if (virtualPosition >= column) return i; else ++virtualPosition;
            }
        }
        if (totalVirtualWidth != null) totalVirtualWidth[0] = virtualPosition;
        return -1;
    }

    /**
	 * INPUT: &lt;elem&gt;text&lt;/elem&gt;<br/>
	 * OUTPUT: text.
	 * @modified 2010.08.30
	 * @since 2010.08.30 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String getTextContentOfElement(String sElem) {
        return sElem.substring(sElem.indexOf(">") + 1, sElem.lastIndexOf("<"));
    }

    /**
	 * INPUT: an SFI: h0.3.7.2.4 and a level-number eg 2.<br/>
	 * OUTUT: h0.3.7 (level-2 of SFI) of empty-string if something wrong.
	 *
	 * @modified 2010.08.31
	 * @since 2010.08.31 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String getSfiPartLevel(String sSFI, int niL) {
        String sOut = "";
        if (sSFI.indexOf("p") != -1) sSFI = sSFI.substring(0, sSFI.indexOf("p"));
        int niLevelSFI = findHeadingLevel(sSFI);
        int ni1, ni2, ni3, ni4, ni5, ni6, ni7, ni8;
        ni1 = ni2 = ni3 = ni4 = ni5 = ni6 = ni7 = ni8 = -1;
        if (niL > niLevelSFI) return ""; else if (niL == 1) {
            if (1 > niLevelSFI) return ""; else if (1 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni1));
            }
        } else if (niL == 2) {
            if (2 > niLevelSFI) return ""; else if (2 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni2));
            }
        } else if (niL == 3) {
            if (3 > niLevelSFI) return ""; else if (3 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                ni3 = sSFI.indexOf(".", ni2) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni3));
            }
        } else if (niL == 4) {
            if (4 > niLevelSFI) return ""; else if (4 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                ni3 = sSFI.indexOf(".", ni2) + 1;
                ni4 = sSFI.indexOf(".", ni3) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni4));
            }
        } else if (niL == 5) {
            if (5 > niLevelSFI) return ""; else if (5 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                ni3 = sSFI.indexOf(".", ni2) + 1;
                ni4 = sSFI.indexOf(".", ni3) + 1;
                ni5 = sSFI.indexOf(".", ni4) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni5));
            }
        } else if (niL == 6) {
            if (6 > niLevelSFI) return ""; else if (6 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                ni3 = sSFI.indexOf(".", ni2) + 1;
                ni4 = sSFI.indexOf(".", ni3) + 1;
                ni5 = sSFI.indexOf(".", ni4) + 1;
                ni6 = sSFI.indexOf(".", ni5) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni6));
            }
        } else if (niL == 7) {
            if (7 > niLevelSFI) return ""; else if (7 == niLevelSFI) return sSFI; else {
                ni1 = sSFI.indexOf(".") + 1;
                ni2 = sSFI.indexOf(".", ni1) + 1;
                ni3 = sSFI.indexOf(".", ni2) + 1;
                ni4 = sSFI.indexOf(".", ni3) + 1;
                ni5 = sSFI.indexOf(".", ni4) + 1;
                ni6 = sSFI.indexOf(".", ni5) + 1;
                ni7 = sSFI.indexOf(".", ni6) + 1;
                return sSFI.substring(0, sSFI.indexOf(".", ni7));
            }
        } else return "";
    }

    /**
	 * Converts a Unix-style glob to a regular expression.<p>
	 *
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 * @since jEdit 4.3pre7
	 */
    public static String globToRE(String glob) {
        if (glob.startsWith("(re)")) {
            return glob.substring(4);
        }
        final Object NEG = new Object();
        final Object GROUP = new Object();
        Stack<Object> state = new Stack<Object>();
        StringBuilder buf = new StringBuilder();
        boolean backslash = false;
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (backslash) {
                buf.append('\\');
                buf.append(c);
                backslash = false;
                continue;
            }
            switch(c) {
                case '\\':
                    backslash = true;
                    break;
                case '?':
                    buf.append('.');
                    break;
                case '.':
                case '+':
                case '(':
                case ')':
                    buf.append('\\');
                    buf.append(c);
                    break;
                case '*':
                    buf.append(".*");
                    break;
                case '|':
                    if (backslash) buf.append("\\|"); else buf.append('|');
                    break;
                case '{':
                    buf.append('(');
                    if (i + 1 != glob.length() && glob.charAt(i + 1) == '!') {
                        buf.append('?');
                        state.push(NEG);
                    } else state.push(GROUP);
                    break;
                case ',':
                    if (!state.isEmpty() && state.peek() == GROUP) buf.append('|'); else buf.append(',');
                    break;
                case '}':
                    if (!state.isEmpty()) {
                        buf.append(')');
                        if (state.pop() == NEG) buf.append(".*");
                    } else buf.append('}');
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
	 * Returns a boolean from a given object.
	 * @param obj the object
	 * @param def The default value
	 * @return the boolean value if obj is a Boolean,
	 * true if the value is "true", "yes", "on",
	 * false if the value is "false", "no", "off"
	 * def if the value is null or anything else
	 * @since jEdit 4.3pre17
	 */
    public static boolean getBoolean(Object obj, boolean def) {
        if (obj == null) return def; else if (obj instanceof Boolean) return ((Boolean) obj).booleanValue(); else if ("true".equals(obj) || "yes".equals(obj) || "on".equals(obj)) return true; else if ("false".equals(obj) || "no".equals(obj) || "off".equals(obj)) return false;
        return def;
    }

    /**
	 * Test if an html-file contains Structure-Fragment-Identifiers (SFI).<br/>
	 * GIVEN: the string-url of the file.
	 *
	 * @modified 2010.07.02
	 * @since 2010.07.02 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static boolean isSFIFile(String sUrl) {
        boolean bSFI = false;
        sUrl = createUrlString(sUrl);
        try {
            URL url = new URL(sUrl);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sLn = "";
            while ((sLn = br.readLine()) != null) {
                if (sLn.indexOf("<a name=\"h0\"></a>") != -1) {
                    bSFI = true;
                    break;
                }
            }
            br.close();
        } catch (Exception e) {
            System.out.println("HtmlUtilities.isSFIFile: " + sUrl + e.toString());
        }
        return bSFI;
    }

    /**
	 * Check if a string of html is SFI-html.
	 *
	 * @modified 2010.08.16
	 * @since 2010.08.16 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static boolean isSFIHtml(String sHtml) {
        if (sHtml.indexOf("<a name=\"h0\"></a>") != -1) return true; else return false;
    }

    /**
	 * Displays a dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
    public static void message(Component comp, String name, Object[] args) {
        JOptionPane.showMessageDialog(comp, HtmlMgr.getProperty(name.concat(".message"), args), HtmlMgr.getProperty(name.concat(".title"), args), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
	 * Returns if two strings are equal. This correctly handles null pointers,
	 * as opposed to calling <code>o1.equals(o2)</code>.
	 * @since jEdit 4.3pre6
	 */
    public static boolean objectsEqual(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) return true; else return false;
        } else if (o2 == null) return false; else return o1.equals(o2);
    }

    /**
	 *
	 * @modified 2010.05.20
	 * @since 2010.05.20 (v00.02.02)
	 * @author HoKoNoUmo, Slava Pestov
	 */
    public static boolean parseXML(InputStream in, DefaultHandler handler) throws IOException {
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            InputSource isrc = new InputSource(new BufferedInputStream(in));
            parser.setContentHandler(handler);
            parser.setDTDHandler(handler);
            parser.setEntityResolver(handler);
            parser.setErrorHandler(handler);
            parser.parse(isrc);
        } catch (SAXParseException se) {
            int line = se.getLineNumber();
            System.out.println("Utilites.parseXml from " + in + ": SAXParseException: line " + line + ": " + se.toString());
            return true;
        } catch (SAXException e) {
            System.out.println(e.toString());
            ;
            return true;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException io) {
                System.out.println(io.toString());
                ;
            }
        }
        return false;
    }

    /**
	 *
	 * @modified 2010.07.09
	 * @since 2010.07.09 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String replaceCharWithReference(String sIn) {
        String sOut;
        sOut = sIn.replace("\\u0022", "&quot;");
        sOut = sIn.replace("&", "&amp;");
        sOut = sIn.replace("<", "&lt;");
        sOut = sIn.replace(">", "&gt;");
        sOut = sIn.replace(" ", "&nbsp;");
        sOut = sIn.replace("¡", "&iexcl;");
        sOut = sIn.replace("¢", "&cent;");
        sOut = sIn.replace("£", "&pound;");
        sOut = sIn.replace("¤", "&curren;");
        sOut = sIn.replace("¥", "&yen;");
        sOut = sIn.replace("¦", "&brvbar;");
        sOut = sIn.replace("§", "&sect;");
        sOut = sIn.replace("¨", "&uml;");
        sOut = sIn.replace("©", "&copy;");
        sOut = sIn.replace("ª", "&ordf;");
        sOut = sIn.replace("«", "&laquo;");
        sOut = sIn.replace("¬", "&not;");
        sOut = sIn.replace("­", "&shy;");
        sOut = sIn.replace("®", "&reg;");
        sOut = sIn.replace("¯", "&macr;");
        sOut = sIn.replace("°", "&deg;");
        sOut = sIn.replace("±", "&plusmn;");
        sOut = sIn.replace("²", "&sup2;");
        sOut = sIn.replace("³", "&sup3;");
        sOut = sIn.replace("´", "&acute;");
        sOut = sIn.replace("µ", "&micro;");
        sOut = sIn.replace("¶", "&para;");
        sOut = sIn.replace("·", "&middot;");
        sOut = sIn.replace("¸", "&cedil;");
        sOut = sIn.replace("¹", "&sup1;");
        sOut = sIn.replace("º", "&ordm;");
        sOut = sIn.replace("»", "&raquo;");
        sOut = sIn.replace("¼", "&frac14;");
        sOut = sIn.replace("½", "&frac12;");
        sOut = sIn.replace("¾", "&frac34;");
        sOut = sIn.replace("¿", "&iquest;");
        sOut = sIn.replace("À", "&Agrave;");
        sOut = sIn.replace("Á", "&Aacute;");
        sOut = sIn.replace("Â", "&Acirc;");
        sOut = sIn.replace("Ã", "&Atilde;");
        sOut = sIn.replace("Ä", "&Auml;");
        sOut = sIn.replace("Å", "&Aring;");
        sOut = sIn.replace("Æ", "&AElig;");
        sOut = sIn.replace("Ç", "&Ccedil;");
        sOut = sIn.replace("È", "&Egrave;");
        sOut = sIn.replace("É", "&Eacute;");
        sOut = sIn.replace("Ê", "&Ecirc;");
        sOut = sIn.replace("Ë", "&Euml;");
        sOut = sIn.replace("Ì", "&Igrave;");
        sOut = sIn.replace("Í", "&Iacute;");
        sOut = sIn.replace("Î", "&Icirc;");
        sOut = sIn.replace("Ï", "&Iuml;");
        sOut = sIn.replace("Ð", "&ETH;");
        sOut = sIn.replace("Ñ", "&Ntilde;");
        sOut = sIn.replace("Ò", "&Ograve;");
        sOut = sIn.replace("Ó", "&Oacute;");
        sOut = sIn.replace("Ô", "&Ocirc;");
        sOut = sIn.replace("Õ", "&Otilde;");
        sOut = sIn.replace("Ö", "&Ouml;");
        sOut = sIn.replace("×", "&times;");
        sOut = sIn.replace("Ø", "&Oslash;");
        sOut = sIn.replace("Ù", "&Ugrave;");
        sOut = sIn.replace("Ú", "&Uacute;");
        sOut = sIn.replace("Û", "&Ucirc;");
        sOut = sIn.replace("Ü", "&Uuml;");
        sOut = sIn.replace("Ý", "&Yacute;");
        sOut = sIn.replace("Þ", "&THORN;");
        sOut = sIn.replace("ß", "&szlig;");
        sOut = sIn.replace("à", "&agrave;");
        sOut = sIn.replace("á", "&aacute;");
        sOut = sIn.replace("â", "&acirc;");
        sOut = sIn.replace("ã", "&atilde;");
        sOut = sIn.replace("ä", "&auml;");
        sOut = sIn.replace("å", "&aring;");
        sOut = sIn.replace("æ", "&aelig;");
        sOut = sIn.replace("ç", "&ccedil;");
        sOut = sIn.replace("è", "&egrave;");
        sOut = sIn.replace("é", "&eacute;");
        sOut = sIn.replace("ê", "&ecirc;");
        sOut = sIn.replace("ë", "&euml;");
        sOut = sIn.replace("ì", "&igrave;");
        sOut = sIn.replace("í", "&iacute;");
        sOut = sIn.replace("î", "&icirc;");
        sOut = sIn.replace("ï", "&iuml;");
        sOut = sIn.replace("ð", "&eth;");
        sOut = sIn.replace("ñ", "&ntilde;");
        sOut = sIn.replace("ò", "&ograve;");
        sOut = sIn.replace("ó", "&oacute;");
        sOut = sIn.replace("ô", "&ocirc;");
        sOut = sIn.replace("õ", "&otilde;");
        sOut = sIn.replace("ö", "&ouml;");
        sOut = sIn.replace("÷", "&divide;");
        sOut = sIn.replace("ø", "&oslash;");
        sOut = sIn.replace("ù", "&ugrave;");
        sOut = sIn.replace("ú", "&uacute;");
        sOut = sIn.replace("û", "&ucirc;");
        sOut = sIn.replace("ü", "&uuml;");
        sOut = sIn.replace("ý", "&yacute;");
        sOut = sIn.replace("þ", "&thorn;");
        sOut = sIn.replace("ÿ", "&yuml;");
        sOut = sIn.replace("Œ", "&OElig;");
        sOut = sIn.replace("œ", "&oelig;");
        sOut = sIn.replace("Š", "&Scaron;");
        sOut = sIn.replace("š", "&scaron;");
        sOut = sIn.replace("Ÿ", "&Yuml;");
        sOut = sIn.replace("ƒ", "&fnof;");
        sOut = sIn.replace("ˆ", "&circ;");
        sOut = sIn.replace("˜", "&tilde;");
        sOut = sIn.replace("Α", "&Alpha;");
        sOut = sIn.replace("Β", "&Beta;");
        sOut = sIn.replace("Γ", "&Gamma;");
        sOut = sIn.replace("Δ", "&Delta;");
        sOut = sIn.replace("Ε", "&Epsilon;");
        sOut = sIn.replace("Ζ", "&Zeta;");
        sOut = sIn.replace("Η", "&Eta;");
        sOut = sIn.replace("Θ", "&Theta;");
        sOut = sIn.replace("Ι", "&Iota;");
        sOut = sIn.replace("Κ", "&Kappa;");
        sOut = sIn.replace("Λ", "&Lambda;");
        sOut = sIn.replace("Μ", "&Mu;");
        sOut = sIn.replace("Ν", "&Nu;");
        sOut = sIn.replace("Ξ", "&Xi;");
        sOut = sIn.replace("Ο", "&Omicron;");
        sOut = sIn.replace("Π", "&Pi;");
        sOut = sIn.replace("Ρ", "&Rho;");
        sOut = sIn.replace("Σ", "&Sigma;");
        sOut = sIn.replace("Τ", "&Tau;");
        sOut = sIn.replace("Υ", "&Upsilon;");
        sOut = sIn.replace("Φ", "&Phi;");
        sOut = sIn.replace("Χ", "&Chi;");
        sOut = sIn.replace("Ψ", "&Psi;");
        sOut = sIn.replace("Ω", "&Omega;");
        sOut = sIn.replace("α", "&alpha;");
        sOut = sIn.replace("β", "&beta;");
        sOut = sIn.replace("γ", "&gamma;");
        sOut = sIn.replace("δ", "&delta;");
        sOut = sIn.replace("ε", "&epsilon;");
        sOut = sIn.replace("ζ", "&zeta;");
        sOut = sIn.replace("η", "&eta;");
        sOut = sIn.replace("θ", "&theta;");
        sOut = sIn.replace("ι", "&iota;");
        sOut = sIn.replace("κ", "&kappa;");
        sOut = sIn.replace("λ", "&lambda;");
        sOut = sIn.replace("μ", "&mu;");
        sOut = sIn.replace("ν", "&nu;");
        sOut = sIn.replace("ξ", "&xi;");
        sOut = sIn.replace("ο", "&omicron;");
        sOut = sIn.replace("π", "&pi;");
        sOut = sIn.replace("ρ", "&rho;");
        sOut = sIn.replace("ς", "&sigmaf;");
        sOut = sIn.replace("σ", "&sigma;");
        sOut = sIn.replace("τ", "&tau;");
        sOut = sIn.replace("υ", "&upsilon;");
        sOut = sIn.replace("φ", "&phi;");
        sOut = sIn.replace("χ", "&chi;");
        sOut = sIn.replace("ψ", "&psi;");
        sOut = sIn.replace("ω", "&omega;");
        sOut = sIn.replace("ϑ", "&thetasym;");
        sOut = sIn.replace("ϒ", "&upsih;");
        sOut = sIn.replace("ϖ", "&piv;");
        sOut = sIn.replace(" ", "&ensp;");
        sOut = sIn.replace(" ", "&emsp;");
        sOut = sIn.replace(" ", "&thinsp;");
        sOut = sIn.replace("‌", "&zwnj;");
        sOut = sIn.replace("‍", "&zwj;");
        sOut = sIn.replace("‎", "&lrm;");
        sOut = sIn.replace("‏", "&rlm;");
        sOut = sIn.replace("–", "&ndash;");
        sOut = sIn.replace("—", "&mdash;");
        sOut = sIn.replace("‘", "&lsquo;");
        sOut = sIn.replace("’", "&rsquo;");
        sOut = sIn.replace("‚", "&sbquo;");
        sOut = sIn.replace("“", "&ldquo;");
        sOut = sIn.replace("”", "&rdquo;");
        sOut = sIn.replace("„", "&bdquo;");
        sOut = sIn.replace("†", "&dagger;");
        sOut = sIn.replace("‡", "&Dagger;");
        sOut = sIn.replace("•", "&bull;");
        sOut = sIn.replace("…", "&hellip;");
        sOut = sIn.replace("‰", "&permil;");
        sOut = sIn.replace("′", "&prime;");
        sOut = sIn.replace("″", "&Prime;");
        sOut = sIn.replace("‹", "&lsaquo;");
        sOut = sIn.replace("›", "&rsaquo;");
        sOut = sIn.replace("‾", "&oline;");
        sOut = sIn.replace("⁄", "&frasl;");
        sOut = sIn.replace("€", "&euro;");
        sOut = sIn.replace("ℑ", "&image;");
        sOut = sIn.replace("℘", "&weierp;");
        sOut = sIn.replace("ℜ", "&real;");
        sOut = sIn.replace("™", "&trade;");
        sOut = sIn.replace("ℵ", "&alefsym;");
        sOut = sIn.replace("←", "&larr;");
        sOut = sIn.replace("↑", "&uarr;");
        sOut = sIn.replace("→", "&rarr;");
        sOut = sIn.replace("↓", "&darr;");
        sOut = sIn.replace("↔", "&harr;");
        sOut = sIn.replace("↵", "&crarr;");
        sOut = sIn.replace("⇐", "&lArr;");
        sOut = sIn.replace("⇑", "&uArr;");
        sOut = sIn.replace("⇒", "&rArr;");
        sOut = sIn.replace("⇓", "&dArr;");
        sOut = sIn.replace("⇔", "&hArr;");
        sOut = sIn.replace("∀", "&forall;");
        sOut = sIn.replace("∂", "&part;");
        sOut = sIn.replace("∃", "&exist;");
        sOut = sIn.replace("∅", "&empty;");
        sOut = sIn.replace("∇", "&nabla;");
        sOut = sIn.replace("∈", "&isin;");
        sOut = sIn.replace("∉", "&notin;");
        sOut = sIn.replace("∋", "&ni;");
        sOut = sIn.replace("∏", "&prod;");
        sOut = sIn.replace("∑", "&sum;");
        sOut = sIn.replace("−", "&minus;");
        sOut = sIn.replace("∗", "&lowast;");
        sOut = sIn.replace("√", "&radic;");
        sOut = sIn.replace("∝", "&prop;");
        sOut = sIn.replace("∞", "&infin;");
        sOut = sIn.replace("∠", "&ang;");
        sOut = sIn.replace("∧", "&and;");
        sOut = sIn.replace("∨", "&or;");
        sOut = sIn.replace("∩", "&cap;");
        sOut = sIn.replace("∪", "&cup;");
        sOut = sIn.replace("∫", "&int;");
        sOut = sIn.replace("∴", "&there4;");
        sOut = sIn.replace("∼", "&sim;");
        sOut = sIn.replace("≅", "&cong;");
        sOut = sIn.replace("≈", "&asymp;");
        sOut = sIn.replace("≠", "&ne;");
        sOut = sIn.replace("≡", "&equiv;");
        sOut = sIn.replace("≤", "&le;");
        sOut = sIn.replace("≥", "&ge;");
        sOut = sIn.replace("⊂", "&sub;");
        sOut = sIn.replace("⊃", "&sup;");
        sOut = sIn.replace("⊄", "&nsub;");
        sOut = sIn.replace("⊆", "&sube;");
        sOut = sIn.replace("⊇", "&supe;");
        sOut = sIn.replace("⊕", "&oplus;");
        sOut = sIn.replace("⊗", "&otimes;");
        sOut = sIn.replace("⊥", "&perp;");
        sOut = sIn.replace("⋅", "&sdot;");
        sOut = sIn.replace("⌈", "&lceil;");
        sOut = sIn.replace("⌉", "&rceil;");
        sOut = sIn.replace("⌊", "&lfloor;");
        sOut = sIn.replace("⌋", "&rfloor;");
        sOut = sIn.replace("〈", "&lang;");
        sOut = sIn.replace("〉", "&rang;");
        sOut = sIn.replace("◊", "&loz;");
        sOut = sIn.replace("♠", "&spades;");
        sOut = sIn.replace("♣", "&clubs;");
        sOut = sIn.replace("♥", "&hearts;");
        sOut = sIn.replace("♦", "&diams;");
        return sOut;
    }

    /**
	 *
	 * @modified 2010.05.24
	 * @since 2010.05.24 (v00.02.02)
	 * @author HoKoNoUmo
	 */
    public static String replaceCharReferencies(String sIn) {
        String sOut;
        sOut = sIn.replace("&quot;", "\\u0022");
        sOut = sIn.replace("&amp;", "&");
        sOut = sIn.replace("&lt;", "<");
        sOut = sIn.replace("&gt;", ">");
        sOut = sIn.replace("&nbsp;", " ");
        sOut = sIn.replace("&iexcl;", "¡");
        sOut = sIn.replace("&cent;", "¢");
        sOut = sIn.replace("&pound;", "£");
        sOut = sIn.replace("&curren;", "¤");
        sOut = sIn.replace("&yen;", "¥");
        sOut = sIn.replace("&brvbar;", "¦");
        sOut = sIn.replace("&sect;", "§");
        sOut = sIn.replace("&uml;", "¨");
        sOut = sIn.replace("&copy;", "©");
        sOut = sIn.replace("&ordf;", "ª");
        sOut = sIn.replace("&laquo;", "«");
        sOut = sIn.replace("&not;", "¬");
        sOut = sIn.replace("&shy;", "­");
        sOut = sIn.replace("&reg;", "®");
        sOut = sIn.replace("&macr;", "¯");
        sOut = sIn.replace("&deg;", "°");
        sOut = sIn.replace("&plusmn;", "±");
        sOut = sIn.replace("&sup2;", "²");
        sOut = sIn.replace("&sup3;", "³");
        sOut = sIn.replace("&acute;", "´");
        sOut = sIn.replace("&micro;", "µ");
        sOut = sIn.replace("&para;", "¶");
        sOut = sIn.replace("&middot;", "·");
        sOut = sIn.replace("&cedil;", "¸");
        sOut = sIn.replace("&sup1;", "¹");
        sOut = sIn.replace("&ordm;", "º");
        sOut = sIn.replace("&raquo;", "»");
        sOut = sIn.replace("&frac14;", "¼");
        sOut = sIn.replace("&frac12;", "½");
        sOut = sIn.replace("&frac34;", "¾");
        sOut = sIn.replace("&iquest;", "¿");
        sOut = sIn.replace("&Agrave;", "À");
        sOut = sIn.replace("&Aacute;", "Á");
        sOut = sIn.replace("&Acirc;", "Â");
        sOut = sIn.replace("&Atilde;", "Ã");
        sOut = sIn.replace("&Auml;", "Ä");
        sOut = sIn.replace("&Aring;", "Å");
        sOut = sIn.replace("&AElig;", "Æ");
        sOut = sIn.replace("&Ccedil;", "Ç");
        sOut = sIn.replace("&Egrave;", "È");
        sOut = sIn.replace("&Eacute;", "É");
        sOut = sIn.replace("&Ecirc;", "Ê");
        sOut = sIn.replace("&Euml;", "Ë");
        sOut = sIn.replace("&Igrave;", "Ì");
        sOut = sIn.replace("&Iacute;", "Í");
        sOut = sIn.replace("&Icirc;", "Î");
        sOut = sIn.replace("&Iuml;", "Ï");
        sOut = sIn.replace("&ETH;", "Ð");
        sOut = sIn.replace("&Ntilde;", "Ñ");
        sOut = sIn.replace("&Ograve;", "Ò");
        sOut = sIn.replace("&Oacute;", "Ó");
        sOut = sIn.replace("&Ocirc;", "Ô");
        sOut = sIn.replace("&Otilde;", "Õ");
        sOut = sIn.replace("&Ouml;", "Ö");
        sOut = sIn.replace("&times;", "×");
        sOut = sIn.replace("&Oslash;", "Ø");
        sOut = sIn.replace("&Ugrave;", "Ù");
        sOut = sIn.replace("&Uacute;", "Ú");
        sOut = sIn.replace("&Ucirc;", "Û");
        sOut = sIn.replace("&Uuml;", "Ü");
        sOut = sIn.replace("&Yacute;", "Ý");
        sOut = sIn.replace("&THORN;", "Þ");
        sOut = sIn.replace("&szlig;", "ß");
        sOut = sIn.replace("&agrave;", "à");
        sOut = sIn.replace("&aacute;", "á");
        sOut = sIn.replace("&acirc;", "â");
        sOut = sIn.replace("&atilde;", "ã");
        sOut = sIn.replace("&auml;", "ä");
        sOut = sIn.replace("&aring;", "å");
        sOut = sIn.replace("&aelig;", "æ");
        sOut = sIn.replace("&ccedil;", "ç");
        sOut = sIn.replace("&egrave;", "è");
        sOut = sIn.replace("&eacute;", "é");
        sOut = sIn.replace("&ecirc;", "ê");
        sOut = sIn.replace("&euml;", "ë");
        sOut = sIn.replace("&igrave;", "ì");
        sOut = sIn.replace("&iacute;", "í");
        sOut = sIn.replace("&icirc;", "î");
        sOut = sIn.replace("&iuml;", "ï");
        sOut = sIn.replace("&eth;", "ð");
        sOut = sIn.replace("&ntilde;", "ñ");
        sOut = sIn.replace("&ograve;", "ò");
        sOut = sIn.replace("&oacute;", "ó");
        sOut = sIn.replace("&ocirc;", "ô");
        sOut = sIn.replace("&otilde;", "õ");
        sOut = sIn.replace("&ouml;", "ö");
        sOut = sIn.replace("&divide;", "÷");
        sOut = sIn.replace("&oslash;", "ø");
        sOut = sIn.replace("&ugrave;", "ù");
        sOut = sIn.replace("&uacute;", "ú");
        sOut = sIn.replace("&ucirc;", "û");
        sOut = sIn.replace("&uuml;", "ü");
        sOut = sIn.replace("&yacute;", "ý");
        sOut = sIn.replace("&thorn;", "þ");
        sOut = sIn.replace("&yuml;", "ÿ");
        sOut = sIn.replace("&OElig;", "Œ");
        sOut = sIn.replace("&oelig;", "œ");
        sOut = sIn.replace("&Scaron;", "Š");
        sOut = sIn.replace("&scaron;", "š");
        sOut = sIn.replace("&Yuml;", "Ÿ");
        sOut = sIn.replace("&fnof;", "ƒ");
        sOut = sIn.replace("&circ;", "ˆ");
        sOut = sIn.replace("&tilde;", "˜");
        sOut = sIn.replace("&Alpha;", "Α");
        sOut = sIn.replace("&Beta;", "Β");
        sOut = sIn.replace("&Gamma;", "Γ");
        sOut = sIn.replace("&Delta;", "Δ");
        sOut = sIn.replace("&Epsilon;", "Ε");
        sOut = sIn.replace("&Zeta;", "Ζ");
        sOut = sIn.replace("&Eta;", "Η");
        sOut = sIn.replace("&Theta;", "Θ");
        sOut = sIn.replace("&Iota;", "Ι");
        sOut = sIn.replace("&Kappa;", "Κ");
        sOut = sIn.replace("&Lambda;", "Λ");
        sOut = sIn.replace("&Mu;", "Μ");
        sOut = sIn.replace("&Nu;", "Ν");
        sOut = sIn.replace("&Xi;", "Ξ");
        sOut = sIn.replace("&Omicron;", "Ο");
        sOut = sIn.replace("&Pi;", "Π");
        sOut = sIn.replace("&Rho;", "Ρ");
        sOut = sIn.replace("&Sigma;", "Σ");
        sOut = sIn.replace("&Tau;", "Τ");
        sOut = sIn.replace("&Upsilon;", "Υ");
        sOut = sIn.replace("&Phi;", "Φ");
        sOut = sIn.replace("&Chi;", "Χ");
        sOut = sIn.replace("&Psi;", "Ψ");
        sOut = sIn.replace("&Omega;", "Ω");
        sOut = sIn.replace("&alpha;", "α");
        sOut = sIn.replace("&beta;", "β");
        sOut = sIn.replace("&gamma;", "γ");
        sOut = sIn.replace("&delta;", "δ");
        sOut = sIn.replace("&epsilon;", "ε");
        sOut = sIn.replace("&zeta;", "ζ");
        sOut = sIn.replace("&eta;", "η");
        sOut = sIn.replace("&theta;", "θ");
        sOut = sIn.replace("&iota;", "ι");
        sOut = sIn.replace("&kappa;", "κ");
        sOut = sIn.replace("&lambda;", "λ");
        sOut = sIn.replace("&mu;", "μ");
        sOut = sIn.replace("&nu;", "ν");
        sOut = sIn.replace("&xi;", "ξ");
        sOut = sIn.replace("&omicron;", "ο");
        sOut = sIn.replace("&pi;", "π");
        sOut = sIn.replace("&rho;", "ρ");
        sOut = sIn.replace("&sigmaf;", "ς");
        sOut = sIn.replace("&sigma;", "σ");
        sOut = sIn.replace("&tau;", "τ");
        sOut = sIn.replace("&upsilon;", "υ");
        sOut = sIn.replace("&phi;", "φ");
        sOut = sIn.replace("&chi;", "χ");
        sOut = sIn.replace("&psi;", "ψ");
        sOut = sIn.replace("&omega;", "ω");
        sOut = sIn.replace("&thetasym;", "ϑ");
        sOut = sIn.replace("&upsih;", "ϒ");
        sOut = sIn.replace("&piv;", "ϖ");
        sOut = sIn.replace("&ensp;", " ");
        sOut = sIn.replace("&emsp;", " ");
        sOut = sIn.replace("&thinsp;", " ");
        sOut = sIn.replace("&zwnj;", "‌");
        sOut = sIn.replace("&zwj;", "‍");
        sOut = sIn.replace("&lrm;", "‎");
        sOut = sIn.replace("&rlm;", "‏");
        sOut = sIn.replace("&ndash;", "–");
        sOut = sIn.replace("&mdash;", "—");
        sOut = sIn.replace("&lsquo;", "‘");
        sOut = sIn.replace("&rsquo;", "’");
        sOut = sIn.replace("&sbquo;", "‚");
        sOut = sIn.replace("&ldquo;", "“");
        sOut = sIn.replace("&rdquo;", "”");
        sOut = sIn.replace("&bdquo;", "„");
        sOut = sIn.replace("&dagger;", "†");
        sOut = sIn.replace("&Dagger;", "‡");
        sOut = sIn.replace("&bull;", "•");
        sOut = sIn.replace("&hellip;", "…");
        sOut = sIn.replace("&permil;", "‰");
        sOut = sIn.replace("&prime;", "′");
        sOut = sIn.replace("&Prime;", "″");
        sOut = sIn.replace("&lsaquo;", "‹");
        sOut = sIn.replace("&rsaquo;", "›");
        sOut = sIn.replace("&oline;", "‾");
        sOut = sIn.replace("&frasl;", "⁄");
        sOut = sIn.replace("&euro;", "€");
        sOut = sIn.replace("&image;", "ℑ");
        sOut = sIn.replace("&weierp;", "℘");
        sOut = sIn.replace("&real;", "ℜ");
        sOut = sIn.replace("&trade;", "™");
        sOut = sIn.replace("&alefsym;", "ℵ");
        sOut = sIn.replace("&larr;", "←");
        sOut = sIn.replace("&uarr;", "↑");
        sOut = sIn.replace("&rarr;", "→");
        sOut = sIn.replace("&darr;", "↓");
        sOut = sIn.replace("&harr;", "↔");
        sOut = sIn.replace("&crarr;", "↵");
        sOut = sIn.replace("&lArr;", "⇐");
        sOut = sIn.replace("&uArr;", "⇑");
        sOut = sIn.replace("&rArr;", "⇒");
        sOut = sIn.replace("&dArr;", "⇓");
        sOut = sIn.replace("&hArr;", "⇔");
        sOut = sIn.replace("&forall;", "∀");
        sOut = sIn.replace("&part;", "∂");
        sOut = sIn.replace("&exist;", "∃");
        sOut = sIn.replace("&empty;", "∅");
        sOut = sIn.replace("&nabla;", "∇");
        sOut = sIn.replace("&isin;", "∈");
        sOut = sIn.replace("&notin;", "∉");
        sOut = sIn.replace("&ni;", "∋");
        sOut = sIn.replace("&prod;", "∏");
        sOut = sIn.replace("&sum;", "∑");
        sOut = sIn.replace("&minus;", "−");
        sOut = sIn.replace("&lowast;", "∗");
        sOut = sIn.replace("&radic;", "√");
        sOut = sIn.replace("&prop;", "∝");
        sOut = sIn.replace("&infin;", "∞");
        sOut = sIn.replace("&ang;", "∠");
        sOut = sIn.replace("&and;", "∧");
        sOut = sIn.replace("&or;", "∨");
        sOut = sIn.replace("&cap;", "∩");
        sOut = sIn.replace("&cup;", "∪");
        sOut = sIn.replace("&int;", "∫");
        sOut = sIn.replace("&there4;", "∴");
        sOut = sIn.replace("&sim;", "∼");
        sOut = sIn.replace("&cong;", "≅");
        sOut = sIn.replace("&asymp;", "≈");
        sOut = sIn.replace("&ne;", "≠");
        sOut = sIn.replace("&equiv;", "≡");
        sOut = sIn.replace("&le;", "≤");
        sOut = sIn.replace("&ge;", "≥");
        sOut = sIn.replace("&sub;", "⊂");
        sOut = sIn.replace("&sup;", "⊃");
        sOut = sIn.replace("&nsub;", "⊄");
        sOut = sIn.replace("&sube;", "⊆");
        sOut = sIn.replace("&supe;", "⊇");
        sOut = sIn.replace("&oplus;", "⊕");
        sOut = sIn.replace("&otimes;", "⊗");
        sOut = sIn.replace("&perp;", "⊥");
        sOut = sIn.replace("&sdot;", "⋅");
        sOut = sIn.replace("&lceil;", "⌈");
        sOut = sIn.replace("&rceil;", "⌉");
        sOut = sIn.replace("&lfloor;", "⌊");
        sOut = sIn.replace("&rfloor;", "⌋");
        sOut = sIn.replace("&lang;", "〈");
        sOut = sIn.replace("&rang;", "〉");
        sOut = sIn.replace("&loz;", "◊");
        sOut = sIn.replace("&spades;", "♠");
        sOut = sIn.replace("&clubs;", "♣");
        sOut = sIn.replace("&hearts;", "♥");
        sOut = sIn.replace("&diams;", "♦");
        return sOut;
    }

    /**
	 * Replaces the first occurance in the input string, a regular-expression
	 * with its replacement.
	 *
	 * @modified 2010.09.12
	 * @since 2010.09.12 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String replaceFirst(String sIn, String sWhat, String sWith) {
        Pattern p = Pattern.compile(sWhat);
        Matcher m = p.matcher(sIn);
        sIn = m.replaceFirst(sWith);
        return sIn;
    }

    /**
	 *
	 * @modified 2010.08.06
	 * @since 2010.08.06 (v00.02.03)
	 * @author HoKoNoUmo
	 */
    public static String setCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        Date currentTime = new Date();
        String stringDate = formatter.format(currentTime);
        return stringDate;
    }

    /**
	 * Input: a path "g:/.../index.html/" or "g:/.../index.html"<br/>
	 * Output: index.html (g:/.../)
	 *
	 * @modified 2010.05.28
	 * @since 2010.05.28 (v00.02.02)
	 * @author HoKoNoUmo
	 */
    public static String setLastPartFirst(String sPath) {
        sPath = sPath.replace('\\', '/');
        if (sPath.endsWith("/") || sPath.endsWith("\\")) sPath = sPath.substring(0, sPath.length() - 1);
        String lp = "";
        String fp = "";
        lp = sPath.substring(sPath.lastIndexOf("/") + 1);
        fp = sPath.substring(0, sPath.lastIndexOf("/") + 1);
        return lp + " (" + fp + ")";
    }

    /**
	 * Compares objects as strings.
	 */
    public static class StringCompare<E> implements Comparator<E> {

        private boolean icase;

        public StringCompare(boolean icase) {
            this.icase = icase;
        }

        public StringCompare() {
        }

        public int compare(E obj1, E obj2) {
            return compareStrings(obj1.toString(), obj2.toString(), icase);
        }
    }
}
