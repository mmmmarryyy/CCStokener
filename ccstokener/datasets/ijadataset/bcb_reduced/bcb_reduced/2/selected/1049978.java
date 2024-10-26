package prohtml;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Basic class for parsing html doucments.
 * @invisible
 */
public abstract class HtmlCollection {

    /**
     * Arraylist holding the colors found in the page
     */
    public ArrayList colors;

    /**
     * Keeps the absolutepart (without file information) of the url of this document.
     */
    public String parentUrlPart;

    /**
     * String to keep the String of the page to parse
     */
    Reader page, keep;

    /**
     * Initializes a new HtmlTree according to the given url.
     * @param url String
     */
    public HtmlCollection(String url) throws InvalidUrlException {
        try {
            parentUrlPart = getParentUrlPart(url);
            page = new BufferedReader(new InputStreamReader(openStream(url)));
            page = new BufferedReader(new InputStreamReader(openStream(url)));
            colors = new ArrayList();
        } catch (Exception e) {
            throw new InvalidUrlException(url, e);
        }
    }

    /**
     * Initializes the collection with a StringReader to parse
     * @param url String
     * @throws InvalidUrlException
     */
    public HtmlCollection(StringReader page, String url) throws InvalidUrlException {
        try {
            parentUrlPart = getParentUrlPart(url);
            this.page = page;
            colors = new ArrayList();
        } catch (Exception e) {
            throw new InvalidUrlException();
        }
    }

    /**
	 * Use this MEthod to get the HTMLSourcecode of the HtmlCollection as String
	 * @return String, source of the HtmlCollection Object
	 * @invisible
	 */
    public String getSource() {
        int iChar;
        char cchar;
        StringBuffer result = new StringBuffer();
        try {
            while ((iChar = keep.read()) != -1) {
                result.append((char) iChar);
            }
        } catch (Exception e) {
            return ("fails");
        }
        return result.toString();
    }

    /**
     * Implement this method to initialize values that you need for your parsing
     */
    abstract void initBeforeParsing();

    private boolean firstTag = false;

    /**
     * Parses a given String and gives back box with the parsed Element and the
     * String still have to be parsed.
     * @param toParse String
     * @return BoxToParseElement
     */
    void parsePage(Reader page) {
        firstTag = true;
        int iChar;
        char cChar;
        StringBuffer sbText = new StringBuffer();
        boolean bText = false;
        boolean bSpaceBefore = false;
        try {
            while ((iChar = page.read()) != -1) {
                cChar = (char) iChar;
                switch(cChar) {
                    case '\b':
                        break;
                    case '\n':
                        break;
                    case '\f':
                        break;
                    case '\r':
                        break;
                    case '\t':
                        break;
                    case '<':
                        if (bText) {
                            bText = false;
                            handleText(new TextElement(sbText.toString(), null));
                            sbText = new StringBuffer();
                        }
                        if ((iChar = page.read()) != -1) {
                            cChar = (char) iChar;
                            if (cChar == '/') {
                                page = handleEndTag(page);
                            } else if (cChar == '!') {
                                if ((iChar = page.read()) != -1) {
                                    cChar = (char) iChar;
                                    if (cChar == '-') {
                                        page = handleComment(page);
                                    } else {
                                        page = handleStartTag(page, new StringBuffer().append(cChar));
                                    }
                                }
                            } else {
                                page = handleStartTag(page, new StringBuffer().append(cChar));
                            }
                        }
                        break;
                    default:
                        if (cChar == ' ') {
                            sbText.append(cChar);
                        } else if (Character.isWhitespace(cChar) && cChar != ' ') {
                        } else {
                            bSpaceBefore = false;
                            bText = true;
                            sbText.append(cChar);
                        }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a Tag and extracts its Name and Attributes.
     * @param page Reader
     * @param alreadyParsed StringBuffer
     * @return Reader
     * @throws Exception
     */
    Reader handleStartTag(Reader page, StringBuffer alreadyParsed) throws Exception {
        int iChar;
        char cChar;
        boolean bTagName = true;
        boolean bSpaceBefore = false;
        boolean bLeftAttribute = false;
        StringBuffer sbTagName = alreadyParsed;
        StringBuffer sbAttributeName = new StringBuffer();
        StringBuffer sbAttributeValue = new StringBuffer();
        StringBuffer sbActual = sbTagName;
        HashMap attributes = new HashMap();
        boolean inValue = false;
        char oChar = ' ';
        while ((iChar = page.read()) != -1) {
            cChar = (char) iChar;
            switch(cChar) {
                case '\b':
                    break;
                case '\n':
                    break;
                case '\f':
                    break;
                case '\r':
                    break;
                case '\t':
                    break;
                case ' ':
                    if (!bSpaceBefore) {
                        if (!inValue) {
                            if (bTagName) {
                                bTagName = false;
                            } else {
                                String sAttributeName = sbAttributeName.toString().toLowerCase();
                                String sAttributeValue = sbAttributeValue.toString();
                                attributes.put(sAttributeName, sAttributeValue);
                                handleAttribute(sAttributeName, sAttributeValue);
                                sbAttributeName = new StringBuffer();
                                sbAttributeValue = new StringBuffer();
                                bLeftAttribute = false;
                            }
                            sbActual = sbAttributeName;
                        } else {
                            sbActual.append(cChar);
                        }
                    }
                    bSpaceBefore = true;
                    break;
                case '=':
                    if (!inValue) {
                        sbActual = sbAttributeValue;
                        bLeftAttribute = true;
                    } else {
                        sbActual.append(cChar);
                    }
                    break;
                case '"':
                    inValue = !inValue;
                    break;
                case '\'':
                    break;
                case '/':
                    if (inValue) sbActual.append(cChar);
                    break;
                case '>':
                    if (bLeftAttribute) {
                        String sAttributeName = sbAttributeName.toString().toLowerCase();
                        String sAttributeValue = sbAttributeValue.toString();
                        attributes.put(sAttributeName, sAttributeValue);
                        handleAttribute(sAttributeName, sAttributeValue);
                    }
                    String sTagName = sbTagName.toString().toLowerCase();
                    if (firstTag) {
                        firstTag = false;
                        if (!(sTagName.equals("doctype") || sTagName.equals("html") || sTagName.equals("?xml"))) throw new InvalidUrlException();
                    }
                    if (oChar == '/') {
                        handleStartTag(sTagName, attributes, true);
                    } else {
                        handleStartTag(sTagName, attributes, false);
                    }
                    return page;
                default:
                    bSpaceBefore = false;
                    sbActual.append(cChar);
            }
            oChar = cChar;
        }
        throw new InvalidUrlException();
    }

    /**
     * In this Method you have to define how parsed Starttags are handles.
     * For Example HtmlElementFinder filters the elements according to its given
     * kindOfElement in its implementation of this method.
     * @param sTagName String
     * @param attributes HashMap
     */
    abstract void handleStartTag(String sTagName, HashMap attributes, boolean standAlone);

    /**
     * Implementing this Method gives you the possibility to handle Attributes during parsing.
     * For Example HtmlTree save the color values of a page right here.
     * @param sAttributeName String
     * @param sAttributeValue String
     */
    abstract void handleAttribute(String sAttributeName, String sAttributeValue);

    /**
     * Parses the end tags of a html document
     * @param toParse Reader
     * @return Reader
     * @throws Exception
     */
    Reader handleEndTag(Reader toParse) throws Exception {
        int iChar;
        char cChar;
        while ((iChar = toParse.read()) != -1) {
            cChar = (char) iChar;
            switch(cChar) {
                case '\b':
                    break;
                case '\n':
                    break;
                case '\f':
                    break;
                case '\r':
                    break;
                case '\t':
                    break;
                case '>':
                    doAfterEndTag();
                    return toParse;
                default:
            }
        }
        throw new InvalidUrlException();
    }

    /**
     * In this method you can call operations being executed after parsing an end tag.
     */
    abstract void doAfterEndTag();

    /**
     * Parses the comments of a htmldocument
     * @param toParse Reader
     * @return Reader
     * @throws Exception
     */
    Reader handleComment(Reader toParse) throws Exception {
        int iChar;
        char cChar;
        char prevChar = ' ';
        while ((iChar = toParse.read()) != -1) {
            cChar = (char) iChar;
            if (prevChar == '-' && cChar == '>') {
                return toParse;
            }
            prevChar = cChar;
        }
        throw new InvalidUrlException();
    }

    /**
     * You have to implement this method to describe what happens to TextElements
     * @param toHandle TextElement
     */
    abstract void handleText(TextElement toHandle);

    /**
     * Gives you the absolute part of an url without any file parts.
     * http://www.mypage.de/index.html becomes http://www.mypage.de/
     * @param url String
     * @return String
     */
    String getParentUrlPart(String url) {
        File test = new File(url);
        File keep = test;
        int countDepth = 0;
        while (true) {
            keep = keep.getParentFile();
            if (keep == null) {
                break;
            }
            countDepth++;
        }
        final String seperator;
        if (url.contains("/")) seperator = "/"; else seperator = "\\";
        if (countDepth > 1 && url.lastIndexOf(seperator) < url.lastIndexOf(".")) {
            return url.substring(0, url.lastIndexOf(seperator));
        } else {
            return url;
        }
    }

    String getDomain(String url) {
        File test = new File(url);
        File keep = test;
        int countDepth = 0;
        while (keep.getParentFile() != null) {
            test = keep;
            keep = keep.getParentFile();
        }
        return test.getPath().replaceAll("\\\\", "//");
    }

    /**
 	 * Modified openStream Method from PApplet.
 	 * @param filename
 	 * @return InputStream
 	 */
    private InputStream openStream(String filename) {
        InputStream stream = null;
        try {
            URL url = new URL(filename);
            stream = url.openStream();
            return stream;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            throw new RuntimeException("Error downloading from URL " + filename);
        }
        try {
            File file = new File("data", filename);
            if (!file.exists()) {
                file = new File(filename);
            }
            if (file.exists()) {
                try {
                    String path = file.getCanonicalPath();
                    String filenameActual = new File(path).getName();
                    if (filenameActual.equalsIgnoreCase(filename) && !filenameActual.equals(filename)) {
                        throw new RuntimeException("This file is named " + filenameActual + " not " + filename + ".");
                    }
                } catch (IOException e) {
                }
            }
            stream = new FileInputStream(file);
            if (stream != null) return stream;
        } catch (IOException ioe) {
        } catch (SecurityException se) {
        }
        try {
            try {
                try {
                    File file = new File(filename);
                    stream = new FileInputStream(file);
                    if (stream != null) return stream;
                } catch (Exception e) {
                }
                try {
                    stream = new FileInputStream(new File("data", filename));
                    if (stream != null) return stream;
                } catch (IOException e2) {
                }
                try {
                    stream = new FileInputStream(filename);
                    if (stream != null) return stream;
                } catch (IOException e1) {
                }
            } catch (SecurityException se) {
            }
            if (stream == null) {
                throw new IOException("openStream() could not open " + filename);
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * returns the int value of a sequence of two hexvalues.
     * @param hex String
     * @return int
     */
    int doubleHexToInt(String hex) {
        return Integer.parseInt(hex, 16);
    }

    /**
     * Returns a Color corresponding to the given color in Hexformat
     * @param hex String
     * @return Color
     */
    int hexToColor(String hex) {
        hex = hex.replaceAll("#", "");
        int x, y, z;
        try {
            x = doubleHexToInt(hex.substring(0, 2));
        } catch (Exception e) {
            hex = ColorConts.colornameTohexvalue(hex);
            x = doubleHexToInt(hex.substring(0, 2));
        }
        y = doubleHexToInt(hex.substring(2, 4));
        z = doubleHexToInt(hex.substring(4, 6));
        if (x > 255) x = 255; else if (x < 0) x = 0;
        if (y > 255) y = 255; else if (y < 0) y = 0;
        if (z > 255) z = 255; else if (z < 0) z = 0;
        return 0xff000000 | (x << 16) | (y << 8) | z;
    }

    /**
	 * GetPageColor gives you a specified color of an HtmlPage. Use getNumbOfColors to get the number of found colors. 
	 * 
	 * @shortdesc GetPageColor gives you a specified color of an HtmlPage.
	 * @param value int, the number of the color you want to be returned
	 * @return color, the color for the given value
	 * @example HtmlTree_colors
	 * @related HtmlTree
	 * @related childNumbToWeight ( ) 
	 * @related getLinks ( ) 
	 * @related getNumbOfColors ( )
	 */
    public int getPageColor(int value) {
        return ((Integer) colors.get(value)).intValue();
    }

    /**
     * Use this Method to get the number of all different colors of a HTML document.
     * 
     * @return int, the number of found colors
     * @example HtmlTree_colors
     * @related HtmlTree
     * @related childNumbToWeight ( ) 
     * @related getLinks ( ) 
     * @related getPageColor ( ) 
     */
    public int getNumbOfColors() {
        return colors.size();
    }

    /**
     * Parses an url and checks if it is a valid link to another document
     * @param url String
     * @throws IllegalArgumentException
     * @return String
     */
    static String before = "";

    static String after = "";

    Url parseUrl(String url) throws IllegalArgumentException, InvalidUrlException {
        int pos = url.indexOf("#");
        if (pos == 0) {
            throw new InvalidUrlException("Just a link to an anchor: " + url);
        } else if (pos > 0) {
            url = url.substring(0, pos);
        }
        pos = url.indexOf("javascript:");
        if (pos != -1) {
            throw new InvalidUrlException("Just a link to a javascript: " + url);
        }
        String toCheck = url.replaceAll("(\\.mp3)|(\\.pdf)|(\\.zip)|(\\.exe)", "");
        if (toCheck.length() < url.length()) {
            throw new InvalidUrlException("Just a link to a not valid file " + url);
        }
        int startLook = 0;
        String urlPart = parentUrlPart;
        while (true) {
            if (url.indexOf("..") == -1) {
                break;
            }
            startLook = url.indexOf("..") + 3;
            url = url.substring(startLook, url.length());
            try {
                urlPart = urlPart.substring(0, urlPart.lastIndexOf("/"));
            } catch (Exception e) {
            }
        }
        pos = url.indexOf(":");
        if (pos == -1) {
            return new Url(url, urlPart + "/", getDomain(urlPart + "/"));
        }
        return new Url(url, "", getDomain(url));
    }
}
