package affd.logic;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.NumericCharacterReference;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.Tag;

/**
 * A HtmlDocument is a Html document editor. It allows to modify the text content of
 * the Html document.
 * 
 * @author Joteitti joteitti@cc.hut.fi
 *
 */
public class HtmlDocument {

    public static final String MODIFIED_EXTENSION = "modified";

    public static final String TOKEN_MODIFIED = "modified";

    public static final String TOKEN_UNCERTAIN = "uncertain";

    protected String filename;

    protected Source source;

    protected ArrayList<Text> texts;

    protected ArrayList<Sentence> sentences;

    protected Tokenizer tokenizer;

    protected ArrayList<String> writes;

    /**
	 * Constructs a Html document.
	 * 
	 * @param filename Document to be loaded.
	 */
    public HtmlDocument(String filename, Tokenizer tokenizer) {
        this.filename = filename;
        this.tokenizer = tokenizer;
        texts = new ArrayList<Text>();
        sentences = new ArrayList<Sentence>();
        writes = new ArrayList<String>();
    }

    /**
	 * Reads the document from the disk.
	 * 
	 * @throws LogicException
	 */
    public void read() throws LogicException {
        try {
            File file = new File(filename);
            URL url = file.toURI().toURL();
            source = new Source(url.openConnection());
        } catch (Exception e) {
            throw new LogicException("Failed to read " + filename + " !", e);
        }
        ArrayList<Segment> segments = new ArrayList<Segment>();
        List<Element> elements = source.getChildElements();
        for (Element element : elements) {
            Segment segment = element.getContent();
            Iterator<Segment> iterator = segment.getNodeIterator();
            while (iterator.hasNext()) {
                Segment current = iterator.next();
                if (isPlainText(current)) {
                    segments.add(current);
                }
            }
        }
        texts.clear();
        sentences.clear();
        for (int i = 0; i < segments.size(); i++) {
            ArrayList<Segment> group = new ArrayList<Segment>();
            group.add(segments.get(i));
            while (i < (segments.size() - 1) && segments.get(i).getEnd() == segments.get(i + 1).getBegin()) {
                group.add(segments.get(i + 1));
                i++;
            }
            texts.add(new Text(group, tokenizer));
        }
        ArrayList<Token> tokens = new ArrayList<Token>();
        for (Text text : texts) {
            tokens.addAll(text.getTokens());
        }
        sentences = tokenizer.toSentences(tokens);
    }

    /**
	 * Tests if the segment contains only text.
	 * 
	 * @param segment Segment to be tested.
	 * @return True if the segment contains only text otherwise false.
	 */
    private boolean isPlainText(Segment segment) {
        return !(segment instanceof Tag) && !segment.isWhiteSpace() && !(segment instanceof NumericCharacterReference);
    }

    /**
	 * Writes the document to the disk. If extarctModifedTokens is true then
	 * a separate file containing the modified tokens is created. The filename
	 * of the separate file is the filename with "modified" extension.
	 * 
	 * @param filename Filename where the document will be written.
	 * @param extarctModifedTokens Extract token information.
	 * @param extarctModifedTokens Ignore token tags.
	 * @throws LogicException
	 */
    public void write(String filename, boolean ignoreTokenTags) throws LogicException {
        try {
            OutputDocument document = new OutputDocument(source);
            for (Text text : texts) {
                text.assign(document, ignoreTokenTags);
            }
            document.writeTo(Utilities.createWriter(filename, source.getEncoding()));
        } catch (Exception e) {
            throw new LogicException("Failed to write document " + filename + " !", e);
        } finally {
            writes.add(new File(filename).getAbsolutePath());
        }
    }

    /**
	 * Writes the modified tokens to file. Each line contains one token with following data:
	 * content, original content, token id and token group. Properties are separated with comma.
	 * 
	 * @param filename Filename where the modified tokens will be written.
	 * @throws LogicException
	 */
    public void writeModified(String filename, int level) throws LogicException {
        filename = filename + "." + MODIFIED_EXTENSION;
        try {
            BufferedWriter writer = Utilities.createWriter(filename, null);
            writer.write(String.valueOf(level));
            writer.newLine();
            for (Text text : texts) {
                for (Token token : text.getTokens()) {
                    if (token.isContentModified() || token.isContentUncertain()) {
                        StringBuffer tokenBuffer = new StringBuffer();
                        if (token.isContentModified()) {
                            tokenBuffer.append(TOKEN_MODIFIED + ",");
                        } else {
                            tokenBuffer.append(TOKEN_UNCERTAIN + ",");
                        }
                        tokenBuffer.append(token.getContent() + ",");
                        if (token.isContentModified()) {
                            tokenBuffer.append(token.getOriginal() + ",");
                        }
                        Token.HtmlTag tag = token.getTag();
                        if (token.isContentUncertain()) {
                            tokenBuffer.append(tag.getAttribute(Token.HTML_ATTRIBUTE_UNCERTAIN) + ",");
                        }
                        tokenBuffer.append(tag.getAttribute(Token.HTML_ATTRIBUTE_CLASS) + ",");
                        tokenBuffer.append(tag.getAttribute(Token.HTML_ATTRIBUTE_ID));
                        if (tag.getAttribute(Token.HTML_ATTRIBUTE_GROUP_ID) != null) {
                            tokenBuffer.append("," + tag.getAttribute(Token.HTML_ATTRIBUTE_GROUP_ID));
                        }
                        writer.write(tokenBuffer.toString());
                        writer.newLine();
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            throw new LogicException("Failed to write modified tokens " + filename + " !", e);
        }
    }

    /**
	 * Cleanup the document files.
	 */
    public void cleanup(String originalFilename) {
        String originalPath = new File(originalFilename).getAbsolutePath();
        String filenamePath = new File(filename).getAbsolutePath();
        if (!writes.contains(filenamePath) && !originalPath.equals(filenamePath)) {
            Utilities.deleteFile(filename);
        }
    }

    /**
	 * Returns the document's sentences.
	 * 
	 * @return The sentences of the document.
	 */
    public ArrayList<Sentence> getSentences() {
        return sentences;
    }

    /**
	 * Returns the written document names.
	 * 
	 * @return The written documents names.
	 */
    public ArrayList<String> getWrites() {
        return writes;
    }
}
