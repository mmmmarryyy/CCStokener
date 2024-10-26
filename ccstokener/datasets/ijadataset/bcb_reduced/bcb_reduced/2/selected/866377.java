package it.newinstance.jrainbow.parser;

import it.newinstance.jrainbow.source.TaggedSource;
import it.newinstance.util.Configuration;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * Base class for implementing parsers. 
 * Defines basic logic for parser subclasses.
 * 
 * @author Luigi R. Viggiano
 * @version $Id: ParserBase.java 138 2007-06-12 12:23:29Z luigi.viggiano $
 */
public abstract class ParserBase implements Parser {

    private static final Configuration conf = Configuration.getConfiguration(Parser.class);

    public TaggedSource parse(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        return parse(conn.getInputStream());
    }

    public TaggedSource parse(InputStream inputStream) throws IOException {
        return parse(new InputStreamReader(inputStream));
    }

    public TaggedSource parse(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        int ch;
        while ((ch = reader.read()) != -1) writer.write(ch);
        reader.close();
        return parse(writer.toString());
    }

    public TaggedSource parse(String string) {
        String sourceCode = replaceTabs(string, conf.getInt("tab.size"));
        TaggedSource source = new TaggedSource(sourceCode);
        parse(source);
        return source;
    }

    public static String replaceTabs(String string, int tabSize) {
        char[] tab = new char[tabSize];
        for (int i = 0; i < tab.length; ++i) tab[i] = ' ';
        return string.toString().replaceAll("\t", new String(tab));
    }
}
