package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.lucene.demo.html.HTMLParser;

/**
 * Parser HTML documents. Extracts document encoding from header, and delegates
 * to lucene HTML parser for extraction of title, summary, and content.
 */
public class HTMLDocParser {

    public static final int MAX_OFFSET = 2048;

    static final String ELEMENT_META = "META";

    static final String ELEMENT_BODY = "body";

    static final String ELEMENT_HEAD = "head";

    static final String ATTRIBUTE_HTTP = "http-equiv";

    static final String ATTRIBUTE_HTTP_VALUE = "content-type";

    static final String ATTRIBUTE_CONTENT = "content";

    static final int STATE_ELEMENT_START = 0;

    static final int STATE_ELEMENT_AFTER_LT = 1;

    static final int STATE_ELEMENT_AFTER_LT_SLASH = 2;

    static final int STATE_ELEMENT_META = 3;

    static final int STATE_HTTP_START = 0;

    static final int STATE_HTTP_AFTER_NAME = 1;

    static final int STATE_HTTP_AFTER_EQ = 2;

    static final int STATE_HTTP_DONE = 3;

    static final int STATE_CONTENT_START = 0;

    static final int STATE_CONTENT_AFTER_NAME = 1;

    static final int STATE_CONTENT_AFTER_EQ = 2;

    static final int STATE_CONTENT_DONE = 3;

    private HTMLParser htmlParser;

    private InputStream inputStream = null;

    /**
	 * @param url
	 * @throws IOException
	 */
    public void openDocument(URL url) throws IOException {
        inputStream = url.openStream();
        String encoding = getCharsetFromHTML(inputStream);
        try {
            inputStream.close();
        } catch (IOException closeIOE) {
        }
        inputStream = url.openStream();
        if (encoding != null) {
            try {
                htmlParser = new HTMLParser(new InputStreamReader(inputStream, encoding));
            } catch (UnsupportedEncodingException uee) {
                htmlParser = new HTMLParser(new InputStreamReader(inputStream));
            }
        } else {
            htmlParser = new HTMLParser(new InputStreamReader(inputStream));
        }
    }

    /**
	 * Releases resources (closes streams)
	 */
    public void closeDocument() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException closeIOE) {
            }
        }
    }

    public String getTitle() throws IOException {
        if (htmlParser == null) {
            throw new NullPointerException();
        }
        try {
            return htmlParser.getTitle();
        } catch (InterruptedException ie) {
            return "";
        }
    }

    public String getSummary(String title) throws IOException {
        try {
            return htmlParser.getSummary();
        } catch (InterruptedException ie) {
            return "";
        }
    }

    public Reader getContentReader() throws IOException {
        if (htmlParser == null) {
            throw new NullPointerException();
        }
        return htmlParser.getReader();
    }

    /**
	 * Private. Parses HTML to extract document encoding specified in HTTP
	 * equivalent META tag in the document header. Example of such META tag is
	 * <META HTTP-EQUIV="content-type" CONTENT="text/html; charset=UTF-8">
	 * 
	 * @return String or null if encoding not found
	 */
    public static String getCharsetFromHTML(InputStream is) {
        Reader asciiReader = new ASCIIReader(is, MAX_OFFSET);
        StreamTokenizer tokenizer = new StreamTokenizer(asciiReader);
        tokenizer.lowerCaseMode(false);
        tokenizer.ordinaryChar('\'');
        tokenizer.ordinaryChar('/');
        String charset = getCharsetFromHTMLTokens(tokenizer);
        if (asciiReader != null) {
            try {
                asciiReader.close();
            } catch (IOException ioe) {
            }
        }
        return charset;
    }

    public static String getCharsetFromHTMLTokens(StreamTokenizer tokenizer) {
        String contentValue = null;
        int stateContent = STATE_HTTP_START;
        int stateElement = STATE_ELEMENT_START;
        int stateHttp = STATE_HTTP_START;
        try {
            for (int token = tokenizer.nextToken(); token != StreamTokenizer.TT_EOF; token = tokenizer.nextToken()) {
                switch(stateElement) {
                    case STATE_ELEMENT_START:
                        if (token == '<') {
                            stateElement = STATE_ELEMENT_AFTER_LT;
                        }
                        break;
                    case STATE_ELEMENT_AFTER_LT:
                        if (token == StreamTokenizer.TT_WORD) {
                            if (ELEMENT_META.equalsIgnoreCase(tokenizer.sval)) {
                                stateElement = STATE_ELEMENT_META;
                                stateHttp = STATE_HTTP_START;
                                stateContent = STATE_CONTENT_START;
                                contentValue = null;
                            } else if (ELEMENT_BODY.equalsIgnoreCase(tokenizer.sval)) {
                                return null;
                            } else {
                                stateElement = STATE_ELEMENT_START;
                            }
                        } else if (token == '/') {
                            stateElement = STATE_ELEMENT_AFTER_LT_SLASH;
                        } else {
                            stateElement = STATE_ELEMENT_START;
                        }
                        break;
                    case STATE_ELEMENT_AFTER_LT_SLASH:
                        if (token == StreamTokenizer.TT_WORD && ELEMENT_HEAD.equalsIgnoreCase(tokenizer.sval)) {
                            return null;
                        }
                        stateElement = STATE_ELEMENT_START;
                        break;
                    default:
                        switch(token) {
                            case '>':
                                stateElement = STATE_ELEMENT_START;
                                break;
                            case StreamTokenizer.TT_WORD:
                                if (ATTRIBUTE_HTTP.equalsIgnoreCase(tokenizer.sval)) {
                                    stateHttp = STATE_HTTP_AFTER_NAME;
                                } else if (ATTRIBUTE_CONTENT.equalsIgnoreCase(tokenizer.sval)) {
                                    stateContent = STATE_CONTENT_AFTER_NAME;
                                } else if (stateHttp == STATE_HTTP_AFTER_EQ && ATTRIBUTE_HTTP_VALUE.equalsIgnoreCase(tokenizer.sval)) {
                                    stateHttp = STATE_HTTP_DONE;
                                } else {
                                    if (stateHttp != STATE_HTTP_DONE) {
                                        stateHttp = STATE_HTTP_START;
                                    }
                                    if (stateContent != STATE_CONTENT_DONE) {
                                        stateContent = STATE_CONTENT_START;
                                    }
                                }
                                break;
                            case '=':
                                if (stateHttp == STATE_HTTP_AFTER_NAME) {
                                    stateHttp = STATE_HTTP_AFTER_EQ;
                                } else if (stateContent == STATE_CONTENT_AFTER_NAME) {
                                    stateContent = STATE_CONTENT_AFTER_EQ;
                                } else {
                                    if (stateHttp != STATE_HTTP_DONE) {
                                        stateHttp = STATE_HTTP_START;
                                    }
                                    if (stateContent != STATE_CONTENT_DONE) {
                                        stateContent = STATE_CONTENT_START;
                                    }
                                }
                                break;
                            case '\"':
                                if (stateHttp == STATE_HTTP_AFTER_EQ) {
                                    if (ATTRIBUTE_HTTP_VALUE.equalsIgnoreCase(tokenizer.sval)) {
                                        stateHttp = STATE_HTTP_DONE;
                                    }
                                } else if (stateContent == STATE_CONTENT_AFTER_EQ) {
                                    stateContent = STATE_CONTENT_DONE;
                                    contentValue = tokenizer.sval;
                                } else {
                                    stateHttp = STATE_HTTP_START;
                                    stateContent = STATE_CONTENT_START;
                                }
                                break;
                            default:
                                if (stateHttp != STATE_HTTP_DONE) {
                                    stateHttp = STATE_HTTP_START;
                                }
                                if (stateContent != STATE_CONTENT_DONE) {
                                    stateContent = STATE_CONTENT_START;
                                }
                                break;
                        }
                        break;
                }
                if (contentValue != null && stateHttp == STATE_HTTP_DONE && stateContent == STATE_CONTENT_DONE) {
                    return getCharsetFromHTTP(contentValue);
                }
            }
        } catch (IOException ioe) {
            return null;
        }
        return null;
    }

    /**
	 * Parses HTTP1.1 Content-Type entity-header field for example,
	 * Content-Type: text/html; charset=ISO-8859-4, and extracts charset
	 * parameter value of the media sub type.
	 * 
	 * @return value of charset parameter, for example ISO-8859-4 or null if
	 *         parameter does not exist
	 */
    public static String getCharsetFromHTTP(String contentValue) {
        StringTokenizer t = new StringTokenizer(contentValue, ";");
        while (t.hasMoreTokens()) {
            String parameter = t.nextToken().trim();
            if (parameter.toLowerCase(Locale.ENGLISH).startsWith("charset=")) {
                String charset = parameter.substring("charset=".length()).trim();
                if (charset.length() > 0) {
                    return charset;
                }
            }
        }
        return null;
    }
}
