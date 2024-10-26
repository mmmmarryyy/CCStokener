import java.net.*;
import java.io.*;

public class HtmlProcessor {

    private URL url;

    private char[] htmlDoc;

    private static final int HTMLDOCSIZE = 131072;

    private int htmlDocLength, htmlDocMaxLength;

    private boolean loaded;

    private boolean hasbody;

    private boolean index;

    private int debug;

    private String charset;

    private String[] excludeFile;

    private int MAXEXCLUDE;

    private String[] ignoreWords;

    private int MAXIGNORE;

    private static final char DASH = '-';

    private static final char LT = '<';

    private static final char GT = '>';

    private static final char NEWLINE = '\n';

    private static final char SPACE = ' ';

    HouseSpider hs;

    /**
     * Initialises HtmlProcessor.
     * The starting buffer size is set to 131072 characters, enough for most web pages.
     * The buffer will grow if required.
     */
    public HtmlProcessor() {
        htmlDoc = new char[HTMLDOCSIZE];
        htmlDocMaxLength = HTMLDOCSIZE;
        loaded = false;
        debug = hs.getDebugLevel();
        charset = hs.getCharset();
        MAXEXCLUDE = hs.getMAXEXCLUDE();
        excludeFile = hs.getFileExclude();
        MAXIGNORE = hs.getMAXIGNORE();
        ignoreWords = hs.getIgnoreWords();
    }

    public void load(URL urlin) throws IOException {
        index = hs.getDoIndex();
        loaded = false;
        url = urlin;
        int c, i;
        htmlDocLength = 0;
        HtmlReader in = new HtmlReader(new InputStreamReader(url.openStream(), charset));
        try {
            if (debug >= 2) System.out.print("Loading " + urlin.toString() + " ... ");
            while ((c = in.read()) >= 0) {
                htmlDoc[htmlDocLength++] = (char) (c);
                if (htmlDocLength == htmlDocMaxLength) {
                    char[] newHtmlDoc = new char[2 * htmlDocMaxLength];
                    System.arraycopy(htmlDoc, 0, newHtmlDoc, 0, htmlDocMaxLength);
                    htmlDocMaxLength = 2 * htmlDocMaxLength;
                    htmlDoc = newHtmlDoc;
                }
            }
            if (debug >= 2) System.out.println("done.");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            if (debug >= 1) System.out.println("Error, reading file into memory (too big) - skipping " + urlin.toString());
            loaded = false;
            return;
        }
        in.close();
        fetchURLpos = 0;
        dumpPos = 0;
        dumpLastChar = SPACE;
        loaded = true;
        frameset = false;
        titledone = false;
        headdone = false;
        checkhead = false;
        checkbody = false;
    }

    /**
     * Gets the URL if one is currently loaded.
     * @return the URL if one is loaded, null otherwise.
     */
    public URL getURL() {
        if (!loaded) return (null);
        return (url);
    }

    /**
     * Gets the base URL of the document, if one is currently loaded.
     * Correctly handles &lt;base href=&quot;&quot;&gt; tag if present, otherwise
     * returns getURL() as base.
     * @return the document's base URL if one is loaded, null otherwise.
     */
    public URL getURLBase() {
        if (!loaded) return (null);
        int i, j;
        i = nextIndexOf(0, "<base href=\"");
        if (i > 0) {
            j = nextIndexOf(i, "\"");
            fetchURLpos = j;
            String href = new String(htmlDoc, i, (j - 1 - i));
            try {
                return (new URL(href));
            } catch (MalformedURLException mue) {
            }
        }
        return (url);
    }

    public String getTitle() {
        if (!loaded) return (null);
        int i, index1, index2;
        index1 = nextIndexOf(0, "<title>");
        if (index1 > 0) {
            index2 = nextIndexOf(index1, "</title>");
            if ((index2 - 8) > index1) {
                return (new String(htmlDoc, index1, (index2 - 8 - index1)));
            } else if (index1 == (index2 - 8)) return ("untitled page");
        }
        return (null);
    }

    /**
     * Gets the text in the &lt;META name="keywords" content=""&gt; tag.
     * <P>Currently this method does not parse each individual keyword. 
     * This method should be made like the getNextURL method.
     * @returns keyword String or a null if no tag found
     */
    public String getMetaKeywords() {
        if (!loaded) return (null);
        int i, index1, index2;
        index1 = nextIndexOf(0, "<meta");
        if (index1 > 0) {
            index1 = nextIndexOf(index1, "name=\"keywords\"");
            if (index1 > 0) {
                index1 = nextIndexOf(index1, "content=\"");
                if (index1 > 0) {
                    index2 = nextIndexOf(index1, "\"");
                    if (index1 < index2) {
                        return (new String(htmlDoc, index1, (index2 - 1 - index1)));
                    }
                }
            }
        }
        return (null);
    }

    /**
     * Gets the text in the &lt;META name="description" content=""&gt; tag.
     * @returns description String or a null if no tag found
     */
    public String getMetaDescription() {
        if (!loaded) return (null);
        int i, index1, index2;
        index1 = nextIndexOf(0, "<meta");
        if (index1 > 0) {
            index1 = nextIndexOf(index1, "name=\"description\"");
            if (index1 > 0) {
                index1 = nextIndexOf(index1, "content=\"");
                if (index1 > 0) {
                    index2 = nextIndexOf(index1, "\"");
                    if (index1 < index2) {
                        return (new String(htmlDoc, index1, (index2 - 1 - index1)));
                    }
                }
            }
        }
        return (null);
    }

    /**
     * Returns the value of hasbody.
     * @return a boolean telling whether the html document has a body element,
     * true if no URL has been loaded.
     */
    public boolean hasBody() {
        if (!loaded) return (false);
        return (hasbody);
    }

    private int fetchURLpos;

    public String getNextHREF() {
        if (!loaded) return (null);
        boolean ignore = false;
        String href, hrefLC;
        int i, index0, index1, index2, index3;
        index1 = 0;
        index0 = 0;
        index1 = nextIndexOfTag(fetchURLpos, "<a", "href=");
        if (frameset) {
            index0 = nextIndexOfTag(fetchURLpos, "<frame", "src=");
            if (index0 > 0) {
                if (((index1 > 0) && (index0 < index1)) || (index1 < 0)) index1 = index0;
            }
        }
        index0 = nextIndexOfTag(fetchURLpos, "<iframe", "src=");
        if (index0 > 0) {
            if (((index1 > 0) && (index0 < index1)) || (index1 < 0)) index1 = index0;
        }
        if (index1 > 0) {
            char delimiter = htmlDoc[index1];
            index1++;
            if (delimiter != '\'' && delimiter != '"') {
                fetchURLpos = index1;
                if (debug >= 3) System.out.println("Warning, ignoring URL not enclosed by single or double quotes.");
                return ("");
            } else {
                index0 = nextIndexOf(index1, ">");
                index2 = nextIndexOf(index1, Character.toString(delimiter));
                if ((index2 > index0) || (index2 < 0)) {
                    fetchURLpos = index1;
                    if (debug >= 3) System.out.println("Warning, ignoring URL - mismatched or missing ending quotes.");
                    return ("");
                }
            }
            if (index2 > index1) {
                fetchURLpos = index2;
                href = new String(htmlDoc, index1, (index2 - 1 - index1));
                hrefLC = href.toLowerCase();
                if (hrefLC.startsWith("file:") || hrefLC.startsWith("mailto:") || hrefLC.startsWith("ftp:") || hrefLC.startsWith("news:") || hrefLC.startsWith("telnet:") || hrefLC.startsWith("javascript:") || hrefLC.endsWith(".js") || hrefLC.endsWith(".css") || hrefLC.endsWith(".pdf") || hrefLC.endsWith(".ps") || hrefLC.endsWith(".dvi") || hrefLC.endsWith(".doc") || hrefLC.endsWith(".rtf") || hrefLC.endsWith(".xls") || hrefLC.endsWith(".ppt") || hrefLC.endsWith(".exe") || hrefLC.endsWith(".dll") || hrefLC.endsWith(".jar") || hrefLC.endsWith(".bat") || hrefLC.endsWith(".jpg") || hrefLC.endsWith(".jpeg") || hrefLC.endsWith(".jfif") || hrefLC.endsWith(".tif") || hrefLC.endsWith(".tiff") || hrefLC.endsWith(".gif") || hrefLC.endsWith(".png") || hrefLC.endsWith(".xbm") || hrefLC.endsWith(".bmp") || hrefLC.endsWith(".au") || hrefLC.endsWith(".aiff") || hrefLC.endsWith(".avi") || hrefLC.endsWith(".mpeg") || hrefLC.endsWith(".mpg") || hrefLC.endsWith(".mp3") || hrefLC.endsWith(".mov") || hrefLC.endsWith(".qt") || hrefLC.endsWith(".wav") || hrefLC.endsWith(".gz") || hrefLC.endsWith(".bz2") || hrefLC.endsWith(".z") || hrefLC.endsWith(".tar") || hrefLC.endsWith(".tgz") || hrefLC.endsWith(".zip") || hrefLC.endsWith(".hqx") || hrefLC.endsWith(".sit") || hrefLC.endsWith(".sea")) {
                    ignore = true;
                    if (debug >= 3) System.out.println("Warning, ignoring: " + href + " - unsupported file format/protocol.");
                }
                if (!ignore && excludeFile != null) {
                    i = 0;
                    while (!ignore && excludeFile[i] != null && i < MAXEXCLUDE) {
                        if (hrefLC.endsWith(excludeFile[i])) {
                            ignore = true;
                            if (debug >= 3) System.out.println("Excluding <file> " + href + ".");
                        } else i++;
                    }
                }
                if (ignore) return (""); else return (href);
            } else if (index1 == index2) {
                fetchURLpos = index2 + 1;
                return ("");
            }
        }
        return (null);
    }

    /**
     * Returns a plain unicode textdump of the htmldocument, one (int)(char) at a time.
     * Ignoring everything in the head of the document, comments and tags.
     * (Meta keywords are not ignored - handled by getMetaKeywords().)
     * @return int text character in htmldocument, whitespace is reduced to single spaces,
     * and the end of the file is returned as &quot;\n&quot;.
     */
    boolean headdone, titledone, frameset;

    boolean checkhead, checkbody;

    int dumpPos, i, len;

    String str;

    char dumpLastChar;

    public int dumpText() {
        try {
            do {
                while (htmlDoc[dumpPos] == LT) {
                    if ((htmlDoc[dumpPos + 1] == '!') && (htmlDoc[dumpPos + 2] == DASH) && (htmlDoc[dumpPos + 3] == DASH) && (htmlDoc[dumpPos + 4] == SPACE) && (htmlDoc[dumpPos + 5] == 'h') && (htmlDoc[dumpPos + 6] == 'o') && (htmlDoc[dumpPos + 7] == 'u') && (htmlDoc[dumpPos + 8] == 's') && (htmlDoc[dumpPos + 9] == 'e') && (htmlDoc[dumpPos + 10] == 's') && (htmlDoc[dumpPos + 11] == 'p') && (htmlDoc[dumpPos + 12] == 'i') && (htmlDoc[dumpPos + 13] == 'd') && (htmlDoc[dumpPos + 14] == 'e') && (htmlDoc[dumpPos + 15] == 'r') && (htmlDoc[dumpPos + 16] == SPACE)) {
                        dumpPos += 16;
                        if ((htmlDoc[dumpPos + 1] == 'n') && (htmlDoc[dumpPos + 2] == 'o') && (htmlDoc[dumpPos + 3] == 'i') && (htmlDoc[dumpPos + 4] == 'n') && (htmlDoc[dumpPos + 5] == 'd') && (htmlDoc[dumpPos + 6] == 'e') && (htmlDoc[dumpPos + 7] == 'x') && (htmlDoc[dumpPos + 8] == SPACE) && (htmlDoc[dumpPos + 9] == DASH) && (htmlDoc[dumpPos + 10] == DASH) && (htmlDoc[dumpPos + 11] == GT)) {
                            dumpPos += 11;
                            index = false;
                        } else if ((htmlDoc[dumpPos + 1] == 'i') && (htmlDoc[dumpPos + 2] == 'n') && (htmlDoc[dumpPos + 3] == 'd') && (htmlDoc[dumpPos + 4] == 'e') && (htmlDoc[dumpPos + 5] == 'x') && (htmlDoc[dumpPos + 6] == SPACE) && (htmlDoc[dumpPos + 7] == DASH) && (htmlDoc[dumpPos + 8] == DASH) && (htmlDoc[dumpPos + 9] == GT)) {
                            dumpPos += 9;
                            index = true;
                        }
                    } else if (!headdone && (Character.toLowerCase(htmlDoc[dumpPos + 1]) == 'h') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'a') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'd') && (htmlDoc[dumpPos + 5] == GT)) {
                        dumpPos += 5;
                        titledone = true;
                        dumpPos += 6;
                        while ((dumpPos < htmlDocLength) && !((htmlDoc[dumpPos - 6] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == '/') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 'h') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'a') && (Character.toLowerCase(htmlDoc[dumpPos - 1]) == 'd') && (htmlDoc[dumpPos] == GT))) {
                            if ((htmlDoc[dumpPos - 6] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == 'b') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 'o') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 'd') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'y') && (htmlDoc[dumpPos - 1] == GT)) {
                                hasbody = true;
                                System.out.println("Warning, missing \"</head>\".");
                                break;
                            } else {
                                dumpPos++;
                            }
                        }
                    } else if (!titledone && (Character.toLowerCase(htmlDoc[dumpPos + 1]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'l') && (Character.toLowerCase(htmlDoc[dumpPos + 5]) == 'e') && (htmlDoc[dumpPos + 6] == GT)) {
                        dumpPos += 6;
                        titledone = true;
                        dumpPos += 7;
                        while ((dumpPos < htmlDocLength) && (!((htmlDoc[dumpPos - 7] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 6]) == '/') && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'l') && (Character.toLowerCase(htmlDoc[dumpPos - 1]) == 'e') && (htmlDoc[dumpPos] == GT)))) dumpPos++;
                    } else if ((Character.toLowerCase(htmlDoc[dumpPos + 1]) == 's') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'c') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'r') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos + 5]) == 'p') && (Character.toLowerCase(htmlDoc[dumpPos + 6]) == 't')) {
                        dumpPos += 6;
                        dumpPos += 8;
                        while ((dumpPos < htmlDocLength) && (!((htmlDoc[dumpPos - 8] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 7]) == '/') && (Character.toLowerCase(htmlDoc[dumpPos - 6]) == 's') && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == 'c') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 'r') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'p') && (Character.toLowerCase(htmlDoc[dumpPos - 1]) == 't') && (htmlDoc[dumpPos] == GT)))) dumpPos++;
                    } else if ((Character.toLowerCase(htmlDoc[dumpPos + 1]) == 's') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'y') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'l') && (Character.toLowerCase(htmlDoc[dumpPos + 5]) == 'e')) {
                        dumpPos += 5;
                        dumpPos += 7;
                        while ((dumpPos < htmlDocLength) && (!((htmlDoc[dumpPos - 7] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 6]) == '/') && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == 's') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 't') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 'y') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'l') && (Character.toLowerCase(htmlDoc[dumpPos - 1]) == 'e') && (htmlDoc[dumpPos] == GT)))) dumpPos++;
                    } else if ((Character.toLowerCase(htmlDoc[dumpPos + 1]) == 'n') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'o') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'n') && (Character.toLowerCase(htmlDoc[dumpPos + 5]) == 'd') && (Character.toLowerCase(htmlDoc[dumpPos + 6]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos + 7]) == 'x')) {
                        dumpPos += 7;
                        dumpPos += 9;
                        while ((dumpPos < htmlDocLength) && (!((htmlDoc[dumpPos - 9] == LT) && (Character.toLowerCase(htmlDoc[dumpPos - 8]) == '/') && (Character.toLowerCase(htmlDoc[dumpPos - 7]) == 'n') && (Character.toLowerCase(htmlDoc[dumpPos - 6]) == 'o') && (Character.toLowerCase(htmlDoc[dumpPos - 5]) == 'i') && (Character.toLowerCase(htmlDoc[dumpPos - 4]) == 'n') && (Character.toLowerCase(htmlDoc[dumpPos - 3]) == 'd') && (Character.toLowerCase(htmlDoc[dumpPos - 2]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos - 1]) == 'x') && (htmlDoc[dumpPos] == GT)))) dumpPos++;
                    } else if ((htmlDoc[dumpPos + 1] == '!') && (htmlDoc[dumpPos + 2] == DASH) && (htmlDoc[dumpPos + 3] == DASH)) {
                        dumpPos += 3;
                        while ((dumpPos < htmlDocLength) && (!((htmlDoc[dumpPos - 2] == DASH) && (htmlDoc[dumpPos - 1] == DASH) && (htmlDoc[dumpPos] == GT)))) dumpPos++;
                    } else if (!hasbody && (Character.toLowerCase(htmlDoc[dumpPos + 1]) == 'b') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'o') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'd') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'y')) {
                        hasbody = true;
                    } else if (!frameset && (Character.toLowerCase(htmlDoc[dumpPos + 1]) == 'f') && (Character.toLowerCase(htmlDoc[dumpPos + 2]) == 'r') && (Character.toLowerCase(htmlDoc[dumpPos + 3]) == 'a') && (Character.toLowerCase(htmlDoc[dumpPos + 4]) == 'm') && (Character.toLowerCase(htmlDoc[dumpPos + 5]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos + 6]) == 's') && (Character.toLowerCase(htmlDoc[dumpPos + 7]) == 'e') && (Character.toLowerCase(htmlDoc[dumpPos + 8]) == 't')) {
                        frameset = true;
                    }
                    while (htmlDoc[dumpPos++] != GT) ;
                }
                i = 0;
                if (!index) {
                    dumpPos++;
                } else {
                    while ((htmlDoc[dumpPos] == SPACE) && (ignoreWords[i] != null) && (i < MAXIGNORE)) {
                        len = ignoreWords[i].length();
                        StringBuffer strbuf = new StringBuffer();
                        for (int j = 1; j < len + 1; j++) {
                            strbuf.append(htmlDoc[dumpPos + j]);
                        }
                        str = strbuf.toString();
                        if (str.equalsIgnoreCase(ignoreWords[i]) && ((htmlDoc[dumpPos + len + 1] == SPACE) || (htmlDoc[dumpPos + len + 1] == '.') || (htmlDoc[dumpPos + len + 1] == ',') || (htmlDoc[dumpPos + len + 1] == ':') || (htmlDoc[dumpPos + len + 1] == ';') || (htmlDoc[dumpPos + len + 1] == '!') || (htmlDoc[dumpPos + len + 1] == '?'))) {
                            dumpPos = dumpPos + len + 1;
                            if (debug >= 3) System.out.println("Ignoring \"" + ignoreWords[i] + "\".");
                            i = 0;
                        } else i++;
                    }
                    if ((dumpLastChar == SPACE) && (htmlDoc[dumpPos] == SPACE)) dumpPos++; else {
                        if (dumpPos >= htmlDocLength) return (NEWLINE);
                        dumpLastChar = htmlDoc[dumpPos];
                        return (htmlDoc[dumpPos++]);
                    }
                }
            } while (true);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return (NEWLINE);
        }
    }

    private int nextIndexOf(int startindex, String s) {
        int i, spos = 0, slength = s.length(), length = slength;
        for (i = startindex; (i < htmlDocLength); i++) {
            if (s.charAt(spos) == Character.toLowerCase(htmlDoc[i])) {
                spos++;
                if (spos == slength) return i + 1;
            } else if (!Character.isWhitespace(htmlDoc[i])) spos = 0;
        }
        return (-1);
    }

    private int nextIndexOf(int startindex, int stoppindex, String s) {
        int i, spos = 0, slength = s.length(), length = slength;
        for (i = startindex; (i < stoppindex + 1); i++) {
            if (s.charAt(spos) == Character.toLowerCase(htmlDoc[i])) {
                spos++;
                if (spos == slength) return i + 1;
            } else if (!Character.isWhitespace(htmlDoc[i])) spos = 0;
        }
        return (-1);
    }

    private int nextIndexOfTag(int startindex, String s1, String s2) {
        int i, start, end;
        i = startindex;
        while (i < htmlDocLength) {
            start = nextIndexOf(i, s1);
            if (start != -1) {
                end = nextIndexOf(start, ">");
                if (end == -1) {
                    i = start + 1;
                } else {
                    i = nextIndexOf(start, end, s2);
                    if (i == -1) {
                        i = end + 1;
                    } else {
                        return i;
                    }
                }
            } else {
                return (-1);
            }
        }
        return (-1);
    }
}
