package seco.notebook.syntax.completion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.StringTokenizer;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 *  HTML Parser. It retrieves sections of the javadoc HTML file.
 *
 * @author  Martin Roskanin
 */
public class HTMLJavadocParser {

    /** Gets the javadoc text from the given URL
     *  @param url nbfs protocol URL
     *  @param pkg true if URL should be retrieved for a package
     */
    public static String getJavadocText(URL url, boolean pkg) {
        if (url == null) return null;
        HTMLEditorKit.Parser parser = null;
        InputStream is = null;
        String charset = null;
        for (; ; ) {
            try {
                is = url.openStream();
                parser = new ParserDelegator();
                String urlStr = url.toString();
                int offsets[] = new int[2];
                Reader reader = charset == null ? new InputStreamReader(is) : new InputStreamReader(is, charset);
                if (pkg) {
                    offsets = parsePackage(reader, parser, charset != null);
                } else if (urlStr.indexOf('#') > 0) {
                    String memberName = urlStr.substring(urlStr.indexOf('#') + 1);
                    if (memberName.length() > 0) offsets = parseMember(reader, memberName, parser, charset != null);
                } else {
                    offsets = parseClass(reader, parser, charset != null);
                }
                if (offsets != null && offsets[0] != -1 && offsets[1] > offsets[0]) {
                    return getTextFromURLStream(url, offsets[0], offsets[1], charset);
                }
                break;
            } catch (ChangedCharSetException e) {
                if (charset == null) {
                    charset = getCharSet(e);
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (IOException ioe) {
                break;
            } finally {
                parser = null;
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private static String getCharSet(ChangedCharSetException e) {
        String spec = e.getCharSetSpec();
        if (e.keyEqualsCharSet()) {
            return spec;
        }
        int index = spec.indexOf(";");
        if (index != -1) {
            spec = spec.substring(index + 1);
        }
        spec = spec.toLowerCase();
        StringTokenizer st = new StringTokenizer(spec, " \t=", true);
        boolean foundCharSet = false;
        boolean foundEquals = false;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(" ") || token.equals("\t")) {
                continue;
            }
            if (foundCharSet == false && foundEquals == false && token.equals("charset")) {
                foundCharSet = true;
                continue;
            } else if (foundEquals == false && token.equals("=")) {
                foundEquals = true;
                continue;
            } else if (foundEquals == true && foundCharSet == true) {
                return token;
            }
            foundCharSet = false;
            foundEquals = false;
        }
        return null;
    }

    /** Gets the part from URLStream as a String
     *  @param startOffset start offset from where to retrieve text
     *  @param endOffset end offset to where the text reach
     *  @throws IOException if the startOffset>endOffset
     */
    public static String getTextFromURLStream(URL url, int startOffset, int endOffset) throws IOException {
        return getTextFromURLStream(url, startOffset, endOffset, null);
    }

    private static String getTextFromURLStream(URL url, int startOffset, int endOffset, String charset) throws IOException {
        if (url == null) return null;
        if (startOffset > endOffset) throw new IOException();
        InputStream fis = url.openStream();
        InputStreamReader fisreader = charset == null ? new InputStreamReader(fis) : new InputStreamReader(fis, charset);
        int len = endOffset - startOffset;
        int bytesAlreadyRead = 0;
        char buffer[] = new char[len];
        int bytesToSkip = startOffset;
        long bytesSkipped = 0;
        do {
            bytesSkipped = fisreader.skip(bytesToSkip);
            bytesToSkip -= bytesSkipped;
        } while ((bytesToSkip > 0) && (bytesSkipped > 0));
        do {
            int count = fisreader.read(buffer, bytesAlreadyRead, len - bytesAlreadyRead);
            if (count < 0) {
                break;
            }
            bytesAlreadyRead += count;
        } while (bytesAlreadyRead < len);
        fisreader.close();
        return new String(buffer);
    }

    /** Retrieves the position (start offset and end offset) of class javadoc info
      * in the raw html file */
    private static int[] parseClass(Reader reader, final HTMLEditorKit.Parser parser, boolean ignoreCharset) throws IOException {
        final int INIT = 0;
        final int CLASS_DATA_START = 1;
        final int TEXT_START = 2;
        final int state[] = new int[1];
        final int offset[] = new int[2];
        offset[0] = -1;
        offset[1] = -1;
        state[0] = INIT;
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {

            int nextHRPos = -1;

            int lastHRPos = -1;

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.HR) {
                    if (state[0] == TEXT_START) {
                        nextHRPos = pos;
                    }
                    lastHRPos = pos;
                }
            }

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.P && state[0] == CLASS_DATA_START) {
                    state[0] = TEXT_START;
                    offset[0] = pos;
                }
                if (t == HTML.Tag.A && state[0] == TEXT_START) {
                    String attrName = (String) a.getAttribute(HTML.Attribute.NAME);
                    if (attrName != null && attrName.length() > 0) {
                        if (nextHRPos != -1) {
                            offset[1] = nextHRPos;
                        } else {
                            offset[1] = pos;
                        }
                        state[0] = INIT;
                    }
                }
            }

            public void handleComment(char[] data, int pos) {
                String comment = String.valueOf(data);
                if (comment != null) {
                    if (comment.indexOf("START OF CLASS DATA") > 0) {
                        state[0] = CLASS_DATA_START;
                    } else if (comment.indexOf("NESTED CLASS SUMMARY") > 0) {
                        if (lastHRPos != -1) {
                            offset[1] = lastHRPos;
                        } else {
                            offset[1] = pos;
                        }
                    }
                }
            }
        };
        parser.parse(reader, callback, ignoreCharset);
        callback = null;
        return offset;
    }

    /** Retrieves the position (start offset and end offset) of member javadoc info
      * in the raw html file */
    private static int[] parseMember(Reader reader, final String name, final HTMLEditorKit.Parser parser, boolean ignoreCharset) throws IOException {
        final int INIT = 0;
        final int A_OPEN = 1;
        final int A_CLOSE = 2;
        final int PRE_OPEN = 3;
        final int state[] = new int[1];
        final int offset[] = new int[2];
        offset[0] = -1;
        offset[1] = -1;
        state[0] = INIT;
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {

            int hrPos = -1;

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.HR && state[0] != INIT) {
                    if (state[0] == PRE_OPEN) {
                        hrPos = pos;
                    }
                }
            }

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.A) {
                    String attrName = (String) a.getAttribute(HTML.Attribute.NAME);
                    if (name.equals(attrName)) {
                        state[0] = A_OPEN;
                    } else {
                        if (state[0] == PRE_OPEN && attrName != null) {
                            state[0] = INIT;
                            offset[1] = (hrPos != -1) ? hrPos : pos;
                        }
                    }
                } else if (t == HTML.Tag.PRE && state[0] == A_CLOSE) {
                    state[0] = PRE_OPEN;
                    offset[0] = pos;
                }
            }

            public void handleEndTag(HTML.Tag t, int pos) {
                if (t == HTML.Tag.A && state[0] == A_OPEN) {
                    state[0] = A_CLOSE;
                }
            }
        };
        parser.parse(reader, callback, ignoreCharset);
        callback = null;
        return offset;
    }

    /** Retrieves the position (start offset and end offset) of member javadoc info
      * in the raw html file */
    private static int[] parsePackage(Reader reader, final HTMLEditorKit.Parser parser, boolean ignoreCharset) throws IOException {
        final String name = "package_description";
        final int INIT = 0;
        final int A_OPEN = 1;
        final int state[] = new int[1];
        final int offset[] = new int[2];
        offset[0] = -1;
        offset[1] = -1;
        state[0] = INIT;
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {

            int hrPos = -1;

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.HR && state[0] != INIT) {
                    if (state[0] == A_OPEN) {
                        hrPos = pos;
                        offset[1] = pos;
                    }
                }
            }

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (t == HTML.Tag.A) {
                    String attrName = (String) a.getAttribute(HTML.Attribute.NAME);
                    if (name.equals(attrName)) {
                        state[0] = A_OPEN;
                        offset[0] = pos;
                    } else {
                        if (state[0] == A_OPEN && attrName != null) {
                            state[0] = INIT;
                            offset[1] = (hrPos != -1) ? hrPos : pos;
                        }
                    }
                }
            }
        };
        parser.parse(reader, callback, ignoreCharset);
        callback = null;
        return offset;
    }
}
