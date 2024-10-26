package org.regenstrief.xhl7;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/** This is a class that parses HL7 messages and generates a
    very simple XML format. This is NOT the same format as
    the HL7 v2xml specification for a number of reasons. The 
    most important reason being that the v2xml specification
    format cannot be generated from HL7 instances alone without
    much help from the specification (e.g., which data type
    a certain field is, and it requires groups to be identified
    as elements on their own.)

    <pre>
    &lt;hl7>
      &lt;segment tag="MSH">
        &lt;field>SIMPLE CONTENT&lt;/field>
        &lt;field>FIRST COMPONENT&lt;component>SECOND COMPONENT&lt;/component>
	  &lt;component>THIRD COMPONENT&lt;/component>
	&lt;/field>
        &lt;field>FIRST REPETITION&lt;repeat>SECOND REPETITION&lt;/repeat>
          &lt;repeat>THIRD REPETITION&lt;/repeat>
	&lt;/field>
        &lt;field>FIRST COMPONENT FIRST ITEM&lt;component>SECOND COMPNENT&lt;/component>
           &lt;repeat>...&lt;/repeat>
        &lt;field>...&lt;component>...&lt;subcomponent>...&lt;/subcomponent>
              &lt;subcomponent>&lt;/subcomponent>
            &lt;/component>
         &lt;/repeat>
       &lt;/field>
     &lt;/segment>
   &lt;/hl7>
   </pre>

   <p>This is what I call "lazy structure", i.e., structural tags are 
   only used at the point where they are really needed. It is easy to
   use in XSLT and XPath. The first node in a tag is the first field
   content. The next node, if any, is a structural tag that will
   tell you on what structural level the first text node was. Since
   HL7 has no mixed content models, there is never any ambiguity.</p>

   <p>This follows the lazy spirit of HL7 v2. It does so not because
   the author believes that that is a good way of thinking and
   handling information, but because the real world is just that messy
   and after having done all a person can do to produce a
   structure-anal HL7 parser (ProtoGen/HL7) the author has given up
   any hope that HL7 v2.x use will ever get there.</p>

   <p>This class behaves like an XML SAX parser, i.e., upon reading an
   HL7 message it generates SAX events. It is extremely simple and 
   extremely easy to use with standard XML tools in Java. One can 
   simply run the HL7 message through an XSLT transform. And this is 
   really the main purpose of this class: to open up the HL7 v2.x 
   message of any uglyness into the world of powerful XSLT transforms.
   This can be used to drive message processors or just message 
   transformers that end up emitting the result of the transformation
   in HL7 v2 syntax.</p>

   <p>Note also that there is no guarrantee the result is actually
   an HL7 message. It could be a batch or a continuation of a preceeding
   message. That's why the toplevel element isn't called "message"
   but simply "hl7".</p>

   <p>You can invoke this in various ways according to the TRAX
   specification, as this class implements the SAX XMLReader interface.
   I recommend using Saxon v7 and higher as follows:</p>

   <p><code>$ saxon7 -x HL7XMLReader some.hl7 transform.xsl</code></p>

   @author Gunther Schadow
   @version $Id: HL7XMLReader.java 2697 2006-08-22 10:01:22Z gunterze $ 
*/
public class HL7XMLReader implements XMLReader, HL7XMLLiterate {

    static final Logger log = Logger.getLogger(HL7XMLReader.class);

    ContentHandler _contentHandler;

    AttributesImpl _atts = new AttributesImpl();

    /** Returns the content handler currently set.

      @see javax.xml.sax.XMLReader 
  */
    public ContentHandler getContentHandler() {
        return this._contentHandler;
    }

    /** Sets content handler that will receive the next event that
      we emit (and all events after that until another content
      handler is set).
      
      @see javax.xml.sax.XMLReader 
  */
    public void setContentHandler(ContentHandler contentHandler) {
        this._contentHandler = contentHandler;
    }

    /** Parse an HL7 message from the given InputSource.
     
     @see javax.xml.sax.XMLReader 
  */
    public void parse(InputSource input) throws IOException, SAXException {
        LineNumberReader reader = null;
        if (input.getCharacterStream() == null) if (input.getByteStream() == null) if (input.getSystemId() == null) throw new SAXException("no usable input"); else {
            parse(input.getSystemId());
            return;
        } else reader = new LineNumberReader(new InputStreamReader(input.getByteStream())); else reader = new LineNumberReader(input.getCharacterStream());
        String delimiters = DEFAULT_DELIMITERS;
        this._contentHandler.startDocument();
        this._contentHandler.startElement(NAMESPACE_URI, TAG_ROOT, TAG_ROOT, this._atts);
        String segmentTag = null;
        Tokenizer tokens = null;
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                log.warn("Detect duplicate segment delimitors after segment " + segmentTag);
                continue;
            }
            try {
                segmentTag = line.substring(0, 3).intern();
                if (segmentTag == "FHS" || segmentTag == "BHS" || segmentTag == "MSH") {
                    delimiters = line.substring(3, 8);
                    tokens = new Tokenizer(line.substring(8), delimiters);
                    this._atts.addAttribute(NAMESPACE_URI, ATT_DEL_FIELD, ATT_DEL_FIELD, CDATA, delimiters.substring(0, 1));
                    this._atts.addAttribute(NAMESPACE_URI, ATT_DEL_COMPONENT, ATT_DEL_COMPONENT, CDATA, delimiters.substring(1, 2));
                    this._atts.addAttribute(NAMESPACE_URI, ATT_DEL_REPEAT, ATT_DEL_REPEAT, CDATA, delimiters.substring(2, 3));
                    this._atts.addAttribute(NAMESPACE_URI, ATT_DEL_ESCAPE, ATT_DEL_ESCAPE, CDATA, delimiters.substring(3, 4));
                    this._atts.addAttribute(NAMESPACE_URI, ATT_DEL_SUBCOMPONENT, ATT_DEL_SUBCOMPONENT, CDATA, delimiters.substring(4, 5));
                } else {
                    tokens = new Tokenizer(line, delimiters);
                    segmentTag = tokens.nextToken();
                }
                this._contentHandler.startElement(NAMESPACE_URI, segmentTag, segmentTag, this._atts);
                this._atts.clear();
                this._status = 0;
                while (tokens.hasMoreTokens()) {
                    String token = tokens.nextToken();
                    if (token.length() == 1) {
                        switch(delimiters.indexOf(token.charAt(0))) {
                            case N_DEL_FIELD:
                                newElement(N_FIELD);
                                continue;
                            case N_DEL_REPEAT:
                                newElement(N_REPEAT);
                                continue;
                            case N_DEL_COMPONENT:
                                newElement(N_COMPONENT);
                                continue;
                            case N_DEL_SUBCOMPONENT:
                                newElement(N_SUBCOMPONENT);
                                continue;
                            case N_DEL_ESCAPE:
                                String closingEscape = tokens.lookAhead(2);
                                if (closingEscape == null) break;
                                if (closingEscape.length() != 1) break;
                                if (closingEscape.charAt(0) != delimiters.charAt(N_DEL_ESCAPE)) break;
                                if (likeEscapeContent(tokens.lookAhead(1)) < likeEscapeContent(tokens.lookAhead(3))) break;
                                toggleEscape();
                                token = tokens.nextToken();
                                this._contentHandler.characters(token.toCharArray(), 0, token.length());
                                token = tokens.nextToken();
                                toggleEscape();
                                continue;
                            default:
                                break;
                        }
                    }
                    this._contentHandler.characters(token.toCharArray(), 0, token.length());
                }
                closeElement(N_FIELD);
                this._contentHandler.endElement(NAMESPACE_URI, segmentTag, segmentTag);
            } catch (IndexOutOfBoundsException ex) {
                throw new SAXException("premature end of input");
            }
        }
        this._contentHandler.endElement(NAMESPACE_URI, TAG_ROOT, TAG_ROOT);
        this._contentHandler.endDocument();
    }

    /** Give some likelihood score that this is the content of 
      an escape sequence. I would really want to use regular
      expressions here, but don't want to push this beyond 
      JDK 1.3 for that, so that the VMS people can run it
      on VMS.
  */
    int likeEscapeContent(String content) {
        if (content == null) return -500;
        int length = content.length();
        char c0 = content.charAt(0);
        if (length == 1) {
            switch(c0) {
                case 'N':
                    return 100;
                case 'H':
                    return 100;
                case 'F':
                    return 100;
                case 'S':
                    return 100;
                case 'T':
                    return 100;
                case 'R':
                    return 100;
                case 'E':
                    return 100;
                default:
                    return (('A' < c0 && c0 < 'Z') ? 10 : -10);
            }
        } else if (length < 10) {
            switch(c0) {
                case '.':
                    char c1 = content.charAt(1);
                    switch(c1) {
                        case 's':
                            return 50;
                        case 'b':
                            return 30;
                        case 'i':
                            return 30;
                        case 't':
                            return 30;
                        case 'S':
                            return 30;
                        case 'B':
                            return 20;
                        case 'I':
                            return 20;
                        case 'T':
                            return 20;
                        default:
                            if ('a' < c1 && c1 < 'z') return 10; else if ('A' < c1 && c1 < 'Z') return 5;
                    }
                    return -10;
                case 'X':
                    return 3;
                case 'Z':
                    return 3;
                case 'C':
                    return 2;
                case 'M':
                    return 2;
                case 'U':
                    return 2;
                default:
                    return -10;
            }
        } else {
            switch(c0) {
                case '.':
                    return -10;
                case 'X':
                    return 3;
                case 'Z':
                    return 2;
                case 'C':
                    return -10;
                case 'M':
                    return -10;
                case 'U':
                    return -10;
                default:
                    return -100;
            }
        }
    }

    /** A buffering string tokenizer with ample look-ahead. */
    public static class Tokenizer {

        /** The StringTokenizer that does the real work. */
        private StringTokenizer _tokens;

        /** The size of the ring buffer. Also the modulo for all our
	buffer pointer arithmetics. */
        public static int BUFFER_SIZE = 8;

        /** A ring-buffer for storing look-ahead tokens. */
        private String _ringBuffer[] = new String[BUFFER_SIZE];

        /** The buffer pointer, whenever any arithmetic is done, it needs
	to be modulo BUFFER_SIZE.*/
        private int _bufferPointer = BUFFER_SIZE - 1;

        /** Create a tokenizer and fill the look-ahead buffer. */
        public Tokenizer(String data, String delimiters) {
            this._tokens = new StringTokenizer(data, delimiters, true);
            for (int i = 0; i < BUFFER_SIZE - 1; i++) {
                String token = (this._tokens.hasMoreTokens() ? this._tokens.nextToken() : null);
                this._ringBuffer[i] = token;
            }
        }

        /** True if nextToken will return a non-null token. */
        boolean hasMoreTokens() {
            return lookAhead(1) != null;
        }

        /** Returns the next token and increases and shifts the tokens by
	one. */
        String nextToken() {
            this._ringBuffer[this._bufferPointer] = (this._tokens.hasMoreTokens() ? this._tokens.nextToken() : null);
            this._bufferPointer = (this._bufferPointer + 1) % BUFFER_SIZE;
            return this._ringBuffer[this._bufferPointer];
        }

        /** Look ahead i number of tokens, where 0 means the current token. 
	Can only look ahead BUFFER_SIZE number of tokens.
    */
        String lookAhead(int i) {
            if (i < BUFFER_SIZE) {
                return this._ringBuffer[(this._bufferPointer + i) % BUFFER_SIZE];
            } else throw new IllegalArgumentException("lookahead by more than " + BUFFER_SIZE);
        }
    }

    private int _status = 0;

    private static int N_ESCAPE = 0;

    private static int N_SUBCOMPONENT = 1;

    private static int N_COMPONENT = 2;

    private static int N_REPEAT = 3;

    private static int N_FIELD = 4;

    private static int S_ESCAPE = 1 << N_ESCAPE;

    private static int S_SUBCOMPONENT = 1 << N_SUBCOMPONENT;

    private static int S_COMPONENT = 1 << N_COMPONENT;

    private static int S_REPEAT = 1 << N_REPEAT;

    private static int S_FIELD = 1 << N_FIELD;

    private static String tagName[] = { TAG_ESCAPE, TAG_SUBCOMPONENT, TAG_COMPONENT, TAG_REPEAT, TAG_FIELD };

    private void closeElement(int level) throws SAXException {
        for (int i = 0; i <= level; i++) {
            int mask = 1 << i;
            if ((this._status & mask) != 0) {
                this._contentHandler.endElement(NAMESPACE_URI, tagName[i], tagName[i]);
                this._status &= ~mask;
            }
        }
    }

    private void newElement(int level) throws SAXException {
        closeElement(level);
        this._contentHandler.startElement(NAMESPACE_URI, tagName[level], tagName[level], this._atts);
        this._status |= 1 << level;
    }

    private void toggleEscape() throws SAXException {
        if ((this._status & S_ESCAPE) != 0) closeElement(N_ESCAPE); else newElement(N_ESCAPE);
    }

    /** Parse an HL7 message from a URL.
     
     @see javax.xml.sax.XMLReader 
  */
    public void parse(String url) throws IOException, SAXException {
        try {
            parse(new InputSource((new URL(url)).openStream()));
        } catch (MalformedURLException ex) {
            throw new SAXException(ex);
        }
    }

    /** Echoes back features that had been set earlier (or their 
      defaults.)

      @see org.regenstrief.xhl7.setFeature for what is supported.

      @see javax.xml.sax.XMLReader 
  */
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(name);
    }

    /** Sets a feature. 

      <!-- The following features are handled:
      
      <ul>
      <li>HL7XMLReader.STRUCTURE_LAZY</li>
      <li>HL7XMLReader.STRUCTURE_EAGER</li>
      <li>HL7XMLReader.STRUCTURE_NORMALIZED</li>
      </ul>
      
      <p>These structure switches are like radio-buttons, overriding
      each other.</p -->

      <p> The following feature requests are tolerated without 
      error, but silently ignored and not echoed back with get-
      feature.</p>

      <ul>
      <li>"http://xml.org/sax/features/namespaces"</li>
      <li>"http://xml.org/sax/features/namespace-prefixes"</li>
      </ul>

      @see javax.xml.sax.XMLReader 
  */
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        name = name.intern();
        if (name == "http://xml.org/sax/features/namespaces" || name == "http://xml.org/sax/features/namespace-prefixes") {
        } else throw new SAXNotRecognizedException(name);
    }

    /** A no-op at this time.
      @see javax.xml.sax.XMLReader 
  */
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(name);
    }

    /** A no-op at this time.
      @see javax.xml.sax.XMLReader 
  */
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(name);
    }

    private ErrorHandler _ErrorHandler;

    /** A no-op at this time.
      @see javax.xml.sax.XMLReader 
  */
    public ErrorHandler getErrorHandler() {
        return this._ErrorHandler;
    }

    /** A no-op at this time.
      @see javax.xml.sax.XMLReader 
  */
    public void setErrorHandler(ErrorHandler ErrorHandler) {
        this._ErrorHandler = ErrorHandler;
    }

    /** A no-op. This is an irrelevant issue for HL7 parsing.
      @see javax.xml.sax.XMLReader 
  */
    public DTDHandler getDTDHandler() {
        return null;
    }

    /** A no-op. This is an irrelevant issue for HL7 parsing.
      @see javax.xml.sax.XMLReader 
  */
    public EntityResolver getEntityResolver() {
        return null;
    }

    /** A no-op. This is an irrelevant issue for HL7 parsing.
      @see javax.xml.sax.XMLReader 
  */
    public void setDTDHandler(DTDHandler x) {
    }

    /** A no-op. This is an irrelevant issue for HL7 parsing.
      @see javax.xml.sax.XMLReader 
  */
    public void setEntityResolver(EntityResolver x) {
    }

    /** Test utility, turn an HL7 message into XML. This is deliberately
      kept absolutely simple, stupid, and powerless. A dump of the 
      XML only. If you want to actually transform the XML, hook this
      XMLReader into your favorite XSLT engine. E.g., with Saxon you
      give the option: -x HL7XMLReader.
   */
    public static void main(String args[]) throws Exception {
        if (args[0].equals("-t")) {
            Tokenizer tok = new Tokenizer(args[1], args[2]);
            while (tok.hasMoreTokens()) System.out.println(tok.nextToken());
        } else {
            boolean doTransform = true;
            boolean outputTraditionally = false;
            XMLReader reader = new HL7XMLReader();
            if (!doTransform && outputTraditionally) {
                reader.setContentHandler(new HL7XMLWriter(System.out));
                reader.parse(args[0]);
            } else if (!doTransform && !outputTraditionally) {
                reader.setContentHandler(new ContentHandler() {

                    public void characters(char[] ch, int start, int length) {
                        System.out.print(new String(ch, start, length));
                    }

                    public void endElement(String namespaceURI, String localName, String qName) {
                        System.out.print("</" + qName + ">");
                    }

                    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
                        System.out.print("<" + qName + ">");
                    }

                    public void endDocument() {
                    }

                    public void startPrefixMapping(String prefix, String uri) {
                    }

                    public void endPrefixMapping(String prefix) {
                    }

                    public void ignorableWhitespace(char[] ch, int start, int length) {
                    }

                    public void processingInstruction(String target, String data) {
                    }

                    public void setDocumentLocator(org.xml.sax.Locator locator) {
                    }

                    public void skippedEntity(String name) {
                    }

                    public void startDocument() {
                    }
                });
                reader.parse(args[0]);
            } else if (doTransform) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new SAXSource(reader, new InputSource(args[0])), new StreamResult(System.out));
            }
        }
    }
}
