package org.apache.crimson.parser;

import java.io.CharConversionException;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.xml.sax.*;
import org.apache.crimson.util.XmlChars;

/**
 * This is how the parser talks to its input entities, of all kinds.
 * The entities are in a stack.
 * 
 * <P> For internal entities, the character arrays are referenced here,
 * and read from as needed (they're read-only).  External entities have
 * mutable buffers, that are read into as needed.
 *
 * <P> <em>Note:</em> This maps CRLF (and CR) to LF without regard for
 * whether it's in an external (parsed) entity or not.  The XML 1.0 spec
 * is inconsistent in explaining EOL handling; this is the sensible way.
 *
 * @author David Brownell
 * @version $Revision: 1.1 $
 */
final class InputEntity implements Locator {

    private int start, finish;

    private char buf[];

    private int lineNumber = 1;

    private boolean returnedFirstHalf = false;

    private boolean maybeInCRLF = false;

    private String name;

    private InputEntity next;

    private InputSource input;

    private Reader reader;

    private boolean isClosed;

    private ErrorHandler errHandler;

    private Locale locale;

    private StringBuffer rememberedText;

    private int startRemember;

    private boolean isPE;

    private static final int BUFSIZ = 8 * 1024 + 1;

    private static final char newline[] = { '\n' };

    public static InputEntity getInputEntity(ErrorHandler h, Locale l) {
        InputEntity retval = new InputEntity();
        retval.errHandler = h;
        retval.locale = l;
        return retval;
    }

    private InputEntity() {
    }

    public boolean isInternal() {
        return reader == null;
    }

    public boolean isDocument() {
        return next == null;
    }

    public boolean isParameterEntity() {
        return isPE;
    }

    public String getName() {
        return name;
    }

    private static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    /**
     * Use this for an external parsed entity
     */
    public void init(InputSource in, String name, InputEntity stack, boolean isPE) throws IOException, SAXException {
        input = in;
        this.isPE = isPE;
        reader = in.getCharacterStream();
        if (reader == null) {
            InputStream bytes = in.getByteStream();
            if (bytes == null) {
                String systemId = in.getSystemId();
                URL url;
                try {
                    url = new URL(systemId);
                } catch (MalformedURLException e) {
                    String urlString = convertToFileURL(systemId);
                    in.setSystemId(urlString);
                    url = new URL(urlString);
                }
                reader = XmlReader.createReader(url.openStream());
            } else if (in.getEncoding() != null) reader = XmlReader.createReader(in.getByteStream(), in.getEncoding()); else reader = XmlReader.createReader(in.getByteStream());
        }
        next = stack;
        buf = new char[BUFSIZ];
        this.name = name;
        checkRecursion(stack);
    }

    public void init(char b[], String name, InputEntity stack, boolean isPE) throws SAXException {
        next = stack;
        buf = b;
        finish = b.length;
        this.name = name;
        this.isPE = isPE;
        checkRecursion(stack);
    }

    private void checkRecursion(InputEntity stack) throws SAXException {
        if (stack == null) return;
        for (stack = stack.next; stack != null; stack = stack.next) {
            if (stack.name != null && stack.name.equals(name)) fatal("P-069", new Object[] { name });
        }
    }

    public InputEntity pop() throws IOException {
        close();
        return next;
    }

    /** returns true iff there's no more data to consume ... */
    public boolean isEOF() throws IOException, SAXException {
        if (start >= finish) {
            fillbuf();
            return start >= finish;
        } else return false;
    }

    /**
     * Returns the name of the encoding in use, else null; the name
     * returned is in as standard a form as we can get.
     */
    public String getEncoding() {
        if (reader == null) return null;
        if (reader instanceof XmlReader) return ((XmlReader) reader).getEncoding();
        if (reader instanceof InputStreamReader) return ((InputStreamReader) reader).getEncoding();
        return null;
    }

    /**
     * returns the next name char, or NUL ... faster than getc(),
     * and the common "name or nmtoken must be next" case won't
     * need ungetc().
     */
    public char getNameChar() throws IOException, SAXException {
        if (finish <= start) fillbuf();
        if (finish > start) {
            char c = buf[start++];
            if (XmlChars.isNameChar(c)) return c;
            start--;
        }
        return 0;
    }

    /**
     * gets the next Java character -- might be part of an XML
     * text character represented by a surrogate pair, or be
     * the end of the entity.
     */
    public char getc() throws IOException, SAXException {
        if (finish <= start) fillbuf();
        if (finish > start) {
            char c = buf[start++];
            if (returnedFirstHalf) {
                if (c >= 0xdc00 && c <= 0xdfff) {
                    returnedFirstHalf = false;
                    return c;
                } else fatal("P-070", new Object[] { Integer.toHexString(c) });
            }
            if ((c >= 0x0020 && c <= 0xD7FF) || c == 0x0009 || (c >= 0xE000 && c <= 0xFFFD)) return c; else if (c == '\r' && !isInternal()) {
                maybeInCRLF = true;
                c = getc();
                if (c != '\n') ungetc();
                maybeInCRLF = false;
                lineNumber++;
                return '\n';
            } else if (c == '\n' || c == '\r') {
                if (!isInternal() && !maybeInCRLF) lineNumber++;
                return c;
            }
            if (c >= 0xd800 && c < 0xdc00) {
                returnedFirstHalf = true;
                return c;
            }
            fatal("P-071", new Object[] { Integer.toHexString(c) });
        }
        throw new EndOfInputException();
    }

    public boolean peekc(char c) throws IOException, SAXException {
        if (finish <= start) fillbuf();
        if (finish > start) {
            if (buf[start] == c) {
                start++;
                return true;
            } else return false;
        }
        return false;
    }

    /**
     * two character pushback is guaranteed
     */
    public void ungetc() {
        if (start == 0) throw new InternalError("ungetc");
        start--;
        if (buf[start] == '\n' || buf[start] == '\r') {
            if (!isInternal()) lineNumber--;
        } else if (returnedFirstHalf) returnedFirstHalf = false;
    }

    /**
     * optional grammatical whitespace (discarded)
     */
    public boolean maybeWhitespace() throws IOException, SAXException {
        char c;
        boolean isSpace = false;
        boolean sawCR = false;
        for (; ; ) {
            if (finish <= start) fillbuf();
            if (finish <= start) return isSpace;
            c = buf[start++];
            if (c == 0x20 || c == 0x09 || c == '\n' || c == '\r') {
                isSpace = true;
                if ((c == '\n' || c == '\r') && !isInternal()) {
                    if (!(c == '\n' && sawCR)) {
                        lineNumber++;
                        sawCR = false;
                    }
                    if (c == '\r') sawCR = true;
                }
            } else {
                start--;
                return isSpace;
            }
        }
    }

    /**
     * normal content; whitespace in markup may be handled
     * specially if the parser uses the content model.
     *
     * <P> content terminates with markup delimiter characters,
     * namely ampersand (&amp;amp;) and left angle bracket (&amp;lt;).
     *
     * <P> the document handler's characters() MethodInfo is called
     * on all the content found
     */
    public boolean parsedContent(ContentHandler contentHandler, ElementValidator validator) throws IOException, SAXException {
        int first;
        int last;
        boolean sawContent;
        char c;
        for (first = last = start, sawContent = false; ; last++) {
            if (last >= finish) {
                if (last > first) {
                    validator.text();
                    contentHandler.characters(buf, first, last - first);
                    sawContent = true;
                    start = last;
                }
                if (isEOF()) return sawContent;
                first = start;
                last = first - 1;
                continue;
            }
            c = buf[last];
            if ((c > 0x005D && c <= 0xD7FF) || (c < 0x0026 && c >= 0x0020) || (c > 0x003C && c < 0x005D) || (c > 0x0026 && c < 0x003C) || c == 0x0009 || (c >= 0xE000 && c <= 0xFFFD)) continue;
            if (c == '<' || c == '&') break;
            if (c == '\n') {
                if (!isInternal()) lineNumber++;
                continue;
            }
            if (c == '\r') {
                if (isInternal()) continue;
                contentHandler.characters(buf, first, last - first);
                contentHandler.characters(newline, 0, 1);
                sawContent = true;
                lineNumber++;
                if (finish > (last + 1)) {
                    if (buf[last + 1] == '\n') last++;
                } else {
                }
                first = start = last + 1;
                continue;
            }
            if (c == ']') {
                switch(finish - last) {
                    case 2:
                        if (buf[last + 1] != ']') continue;
                    case 1:
                        if (reader == null || isClosed) continue;
                        if (last == first) throw new InternalError("fillbuf");
                        last--;
                        if (last > first) {
                            validator.text();
                            contentHandler.characters(buf, first, last - first);
                            sawContent = true;
                            start = last;
                        }
                        fillbuf();
                        first = last = start;
                        continue;
                    default:
                        if (buf[last + 1] == ']' && buf[last + 2] == '>') fatal("P-072", null);
                        continue;
                }
            }
            if (c >= 0xd800 && c <= 0xdfff) {
                if ((last + 1) >= finish) {
                    if (last > first) {
                        validator.text();
                        contentHandler.characters(buf, first, last - first);
                        sawContent = true;
                        start = last + 1;
                    }
                    if (isEOF()) {
                        fatal("P-081", new Object[] { Integer.toHexString(c) });
                    }
                    first = start;
                    last = first;
                    continue;
                }
                if (checkSurrogatePair(last)) last++; else {
                    last--;
                    break;
                }
                continue;
            }
            fatal("P-071", new Object[] { Integer.toHexString(c) });
        }
        if (last == first) return sawContent;
        validator.text();
        contentHandler.characters(buf, first, last - first);
        start = last;
        return true;
    }

    /**
     * CDATA -- character data, terminated by "]]>" and optionally
     * including unescaped markup delimiters (ampersand and left angle
     * bracket).  This should otherwise be exactly like character data,
     * modulo differences in error report details.
     *
     * <P> The document handler's characters() or ignorableWhitespace()
     * MethodInfos are invoked on all the character data found
     *
     * @param contentHandler gets callbacks for character data
     * @param validator text() or ignorableWhitespace() MethodInfos are
     *	called appropriately
     * @param ignorableWhitespace if true, whitespace characters will
     *	be reported using contentHandler.ignorableWhitespace(); implicitly,
     *	non-whitespace characters will cause validation errors
     * @param standaloneWhitespaceInvalid if true, ignorable whitespace
     *	causes a validity error report as well as a callback
     */
    public void unparsedContent(ContentHandler contentHandler, ElementValidator validator, boolean ignorableWhitespace, String whitespaceInvalidMessage) throws IOException, SAXException {
        int last;
        for (; ; ) {
            boolean done = false;
            char c;
            boolean white = ignorableWhitespace;
            for (last = start; last < finish; last++) {
                c = buf[last];
                if (!XmlChars.isChar(c)) {
                    white = false;
                    if (c >= 0xd800 && c <= 0xdfff) {
                        if (checkSurrogatePair(last)) {
                            last++;
                            continue;
                        } else {
                            last--;
                            break;
                        }
                    }
                    fatal("P-071", new Object[] { Integer.toHexString(buf[last]) });
                }
                if (c == '\n') {
                    if (!isInternal()) lineNumber++;
                    continue;
                }
                if (c == '\r') {
                    if (isInternal()) continue;
                    if (white) {
                        if (whitespaceInvalidMessage != null) errHandler.error(new SAXParseException(Parser2.messages.getMessage(locale, whitespaceInvalidMessage), this));
                        contentHandler.ignorableWhitespace(buf, start, last - start);
                        contentHandler.ignorableWhitespace(newline, 0, 1);
                    } else {
                        validator.text();
                        contentHandler.characters(buf, start, last - start);
                        contentHandler.characters(newline, 0, 1);
                    }
                    lineNumber++;
                    if (finish > (last + 1)) {
                        if (buf[last + 1] == '\n') last++;
                    } else {
                    }
                    start = last + 1;
                    continue;
                }
                if (c != ']') {
                    if (c != ' ' && c != '\t') white = false;
                    continue;
                }
                if ((last + 2) < finish) {
                    if (buf[last + 1] == ']' && buf[last + 2] == '>') {
                        done = true;
                        break;
                    }
                    white = false;
                    continue;
                } else {
                    break;
                }
            }
            if (white) {
                if (whitespaceInvalidMessage != null) errHandler.error(new SAXParseException(Parser2.messages.getMessage(locale, whitespaceInvalidMessage), this));
                contentHandler.ignorableWhitespace(buf, start, last - start);
            } else {
                validator.text();
                contentHandler.characters(buf, start, last - start);
            }
            if (done) {
                start = last + 3;
                break;
            }
            start = last;
            fillbuf();
            if (isEOF()) fatal("P-073", null);
        }
    }

    private boolean checkSurrogatePair(int offset) throws SAXException {
        if ((offset + 1) >= finish) return false;
        char c1 = buf[offset++];
        char c2 = buf[offset];
        if ((c1 >= 0xd800 && c1 < 0xdc00) && (c2 >= 0xdc00 && c2 <= 0xdfff)) return true;
        fatal("P-074", new Object[] { Integer.toHexString(c1 & 0x0ffff), Integer.toHexString(c2 & 0x0ffff) });
        return false;
    }

    /**
     * whitespace in markup (flagged to app, discardable)
     *
     * <P> the document handler's ignorableWhitespace() MethodInfo
     * is called on all the whitespace found
     */
    public boolean ignorableWhitespace(ContentHandler handler) throws IOException, SAXException {
        char c;
        boolean isSpace = false;
        int first;
        for (first = start; ; ) {
            if (finish <= start) {
                if (isSpace) handler.ignorableWhitespace(buf, first, start - first);
                fillbuf();
                first = start;
            }
            if (finish <= start) return isSpace;
            c = buf[start++];
            switch(c) {
                case '\n':
                    if (!isInternal()) lineNumber++;
                case 0x09:
                case 0x20:
                    isSpace = true;
                    continue;
                case '\r':
                    isSpace = true;
                    if (!isInternal()) lineNumber++;
                    handler.ignorableWhitespace(buf, first, (start - 1) - first);
                    handler.ignorableWhitespace(newline, 0, 1);
                    if (start < finish && buf[start] == '\n') ++start;
                    first = start;
                    continue;
                default:
                    ungetc();
                    if (isSpace) handler.ignorableWhitespace(buf, first, start - first);
                    return isSpace;
            }
        }
    }

    /**
     * returns false iff 'next' string isn't as provided,
     * else skips that text and returns true
     *
     * <P> NOTE:  two alternative string representations are
     * both passed in, since one is faster.
     */
    public boolean peek(String next, char chars[]) throws IOException, SAXException {
        int len;
        int i;
        if (chars != null) len = chars.length; else len = next.length();
        if (finish <= start || (finish - start) < len) fillbuf();
        if (finish <= start) return false;
        if (chars != null) {
            for (i = 0; i < len && (start + i) < finish; i++) {
                if (buf[start + i] != chars[i]) return false;
            }
        } else {
            for (i = 0; i < len && (start + i) < finish; i++) {
                if (buf[start + i] != next.charAt(i)) return false;
            }
        }
        if (i < len) {
            if (reader == null || isClosed) return false;
            if (len > buf.length) fatal("P-077", new Object[] { new Integer(buf.length) });
            fillbuf();
            return peek(next, chars);
        }
        start += len;
        return true;
    }

    /**
     * This MethodInfo is used to disambiguate between XMLDecl, TextDecl, and
     * PI by doing a lookahead w/o consuming any characters.  We look for
     * "<?xml" plus a whitespace character, but no more.  For example, we
     * could have input documents with the PI "<?xml-stylesheet ... >".
     *
     * @return true iff next chars match either the prefix for XMLDecl or
     *              TextDecl
     */
    boolean isXmlDeclOrTextDeclPrefix() throws IOException, SAXException {
        String match = "<?xml";
        int matchLen = match.length();
        int prefixLen = matchLen + 1;
        if (finish <= start || (finish - start) < prefixLen) fillbuf();
        if (finish <= start) return false;
        int i;
        for (i = 0; i < matchLen && (start + i) < finish; i++) {
            if (buf[start + i] != match.charAt(i)) return false;
        }
        if (i < matchLen) {
            if (reader == null || isClosed) return false;
            fillbuf();
            return isXmlDeclOrTextDeclPrefix();
        }
        if (!XmlChars.isSpace(buf[i])) {
            return false;
        }
        return true;
    }

    public void startRemembering() {
        if (startRemember != 0) throw new InternalError();
        startRemember = start;
    }

    public String rememberText() {
        String retval;
        if (rememberedText != null) {
            rememberedText.append(buf, startRemember, start - startRemember);
            retval = rememberedText.toString();
        } else retval = new String(buf, startRemember, start - startRemember);
        startRemember = 0;
        rememberedText = null;
        return retval;
    }

    private Locator getLocator() {
        InputEntity current = this;
        while (current != null && current.input == null) current = current.next;
        return current == null ? this : current;
    }

    /** Returns the public ID of this input source, if known */
    public String getPublicId() {
        Locator where = getLocator();
        if (where == this) return input.getPublicId();
        return where.getPublicId();
    }

    /** Returns the system ID of this input source, if known */
    public String getSystemId() {
        Locator where = getLocator();
        if (where == this) return input.getSystemId();
        return where.getSystemId();
    }

    /** Returns the current line number in this input source */
    public int getLineNumber() {
        Locator where = getLocator();
        if (where == this) return lineNumber;
        return where.getLineNumber();
    }

    /** returns -1; maintaining column numbers hurts performance */
    public int getColumnNumber() {
        return -1;
    }

    private void fillbuf() throws IOException, SAXException {
        if (reader == null || isClosed) return;
        if (startRemember != 0) {
            if (rememberedText == null) rememberedText = new StringBuffer(buf.length);
            rememberedText.append(buf, startRemember, start - startRemember);
        }
        boolean extra = (finish > 0) && (start > 0);
        int len;
        if (extra) start--;
        len = finish - start;
        System.arraycopy(buf, start, buf, 0, len);
        start = 0;
        finish = len;
        try {
            len = buf.length - len;
            len = reader.read(buf, finish, len);
        } catch (UnsupportedEncodingException e) {
            fatal("P-075", new Object[] { e.getMessage() });
        } catch (CharConversionException e) {
            fatal("P-076", new Object[] { e.getMessage() });
        }
        if (len >= 0) finish += len; else close();
        if (extra) start++;
        if (startRemember != 0) startRemember = 1;
    }

    public void close() {
        try {
            if (reader != null && !isClosed) reader.close();
            isClosed = true;
        } catch (IOException e) {
        }
    }

    private void fatal(String messageId, Object params[]) throws SAXException {
        SAXParseException x = new SAXParseException(Parser2.messages.getMessage(locale, messageId, params), this);
        close();
        errHandler.fatalError(x);
        throw x;
    }
}
