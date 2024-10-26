package net.sourceforge.pergamon.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.pergamon.document.Document;
import sun.net.www.protocol.file.FileURLConnection;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;
import com.ibm.icu.text.CharsetDetector;

/**
 * <p>
 * This class is the entry-point to the system. It has a bunch of create methods, each of which
 * returns a different kind of parser, for a single type of file.
 * </p>
 * 
 * <p>
 * <code>
 * final Parser parser = new HTMLParser(); // Parser for HTML files<br>
 * final Parser parser = new DOCParser(); // Parser for Microsoft Word files.<br>
 * final Parser parser = Parser.createParserBy("text/html"); // Parser for HTML files.
 * </code>
 * </p>
 * 
 * <p>
 * After creating a parser, you can call any of the available <code>parse(...)</code> methods, each
 * accepting a different input, like an InputStream, or an URL.
 * </p>
 * 
 * <p>
 * <code>
 * final Document document = parser.parse("http://retriever.stela.org.br"); // Parses an HTML file in the Web <br>
 * final Document document = parser.parse("file:/C:/workspace/Pergamo/test/resource/test.doc"); // Parses a DOC file in a local disk on Windows<br>
 * final Document document = parser.parse("file:///home/user/test.doc"); // Parses a DOC file in a local disk on Linux
 * </code>
 * </p>
 */
public abstract class Parser {

    private static final Map<String, String> contentTypesByFileType = new HashMap<String, String>();

    private static final Map<String, Class<? extends Parser>> parsersByContentType = new HashMap<String, Class<? extends Parser>>();

    private static int connectTimeout = 60000;

    private static int readTimeout = 60000 * 5;

    static {
        fillContentTypesByFileType();
        fillParsersByContentType();
    }

    private static void fillContentTypesByFileType() {
        contentTypesByFileType.put("pdf", "application/pdf");
        contentTypesByFileType.put("xhtml", "application/xhtml+xml");
        contentTypesByFileType.put("js", "application/javascript");
        contentTypesByFileType.put("dtd", "application/xml-dtd");
        contentTypesByFileType.put("zip", "application/zip");
        contentTypesByFileType.put("mp3", "audio/mpeg");
        contentTypesByFileType.put("wma", "audio/x-ms-wma");
        contentTypesByFileType.put("wav", "audio/x-wav");
        contentTypesByFileType.put("gif", "image/gif");
        contentTypesByFileType.put("jpg", "image/jpeg");
        contentTypesByFileType.put("png", "image/png");
        contentTypesByFileType.put("css", "text/css");
        contentTypesByFileType.put("html", "text/html");
        contentTypesByFileType.put("htm", "text/html");
        contentTypesByFileType.put("txt", "text/plain");
        contentTypesByFileType.put("mpg", "video/mpeg");
        contentTypesByFileType.put("xml", "text/xml");
        contentTypesByFileType.put("mp4", "video/mp4");
        contentTypesByFileType.put("wmv", "video/x-ms-wmv");
        contentTypesByFileType.put("xls", "application/vnd.ms-excel");
        contentTypesByFileType.put("ppt", "application/vnd.ms-powerpoint");
        contentTypesByFileType.put("ppt", "application/vnd.ms-powerpoint");
        contentTypesByFileType.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        contentTypesByFileType.put("pps", "application/vnd.ms-powerpoint");
        contentTypesByFileType.put("doc", "application/msword");
        contentTypesByFileType.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contentTypesByFileType.put("rar", "application/x-rar-compressed");
        contentTypesByFileType.put("swf", "application/x-shockwave-flash");
        contentTypesByFileType.put("rtf", "application/rtf");
        contentTypesByFileType.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private static void fillParsersByContentType() {
        parsersByContentType.put(DOCParser.CONTENT_TYPE, DOCParser.class);
        parsersByContentType.put(EmailParser.CONTENT_TYPE, EmailParser.class);
        parsersByContentType.put(HTMLParser.CONTENT_TYPE, HTMLParser.class);
        parsersByContentType.put(PDFParser.CONTENT_TYPE, PDFParser.class);
        parsersByContentType.put(PlainTextParser.CONTENT_TYPE, PlainTextParser.class);
        parsersByContentType.put(PPTParser.CONTENT_TYPE, PPTParser.class);
        parsersByContentType.put(RTFParser.CONTENT_TYPE, RTFParser.class);
        fillParsersWithMultipleContentTypes(FeedParser.CONTENT_TYPES.split(";"), FeedParser.class);
        fillParsersWithMultipleContentTypes(XMLParser.CONTENT_TYPES.split(";"), XMLParser.class);
    }

    private static void fillParsersWithMultipleContentTypes(final String[] contentTypes, final Class<? extends Parser> parserClass) {
        for (String feedContentType : contentTypes) {
            parsersByContentType.put(feedContentType, parserClass);
        }
    }

    /**
         * TODO: test, javadoc.
         */
    public static void setReadTimeout(final int milliseconds) {
        Parser.readTimeout = milliseconds;
    }

    /**
         * TODO: test, javadoc.
         */
    public static void setConnectTimeout(final int milliseconds) {
        Parser.connectTimeout = milliseconds;
    }

    /**
         * Sets the content type of the document.
         * 
         * @param document The document which will have the content type settled.
         */
    protected abstract void setContentType(Document document);

    /**
         * Parses information within the InputStream to a Document instance.
         * 
         * @param inputStream Object with data to be extracted out.
         * @param document Object that will receive data extracted from the URLConnection.
         * @param URL TODO
         */
    protected abstract void parseSpecificInputStreamInformationIntoDocument(final InputStream inputStream, final List<Document> document, final URL url);

    /**
         * TODO Javadoc.
         */
    public List<Document> parse(final InputStream inputStream) {
        return this.parse(inputStream, null);
    }

    /**
         * Parsers a resource in the form of a <code>java.io.InputStream</code> to an instance of
         * the <code>net.sourceforge.retriever.collector.handler.Document</code> class.
         * 
         * @param inputStream The resource to be parsed.
         * @param utl TODO
         * @return The resource parsed.
         */
    public List<Document> parse(final InputStream inputStream, final URL url) {
        final InputStream copiedInputStream = this.copyInputStream(inputStream);
        copiedInputStream.mark(2147483647);
        final List<Document> document = new ArrayList<Document>();
        this.parseSpecificInputStreamInformationIntoDocument(copiedInputStream, document, url);
        return document;
    }

    /**
         * Parses a resource pointed by a <code>java.net.URL</code> to an instance of the
         * <code>net.sourceforge.retriever.collector.handler.Document</code> class.
         * 
         * @param url The resource to be parsed.
         * @return The resource parsed.
         */
    public List<Document> parse(final URL url) {
        URLConnection urlConnection = null;
        try {
            urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(Parser.connectTimeout);
            urlConnection.setReadTimeout(Parser.readTimeout);
            urlConnection.addRequestProperty("Connection", "close");
            urlConnection.connect();
            return this.parse(urlConnection.getInputStream(), url);
        } catch (final IOException e) {
            this.treatFetchException(urlConnection);
            return null;
        } finally {
            this.close(urlConnection);
        }
    }

    private void treatFetchException(final URLConnection urlConnection) {
        if (urlConnection == null) return;
        InputStream errorStream = null;
        try {
            try {
                errorStream = ((HttpURLConnection) urlConnection).getErrorStream();
            } catch (final ClassCastException e) {
                errorStream = ((HttpsURLConnectionImpl) urlConnection).getErrorStream();
            }
            if (errorStream == null) return;
            final byte[] buffer = new byte[1024];
            while (errorStream.read(buffer) > 0) {
            }
        } catch (final IOException e) {
        } finally {
            try {
                if (errorStream != null) errorStream.close();
            } catch (final Exception e) {
            }
        }
    }

    private void close(URLConnection urlConnection) {
        try {
            if (urlConnection != null) {
                if (urlConnection instanceof FileURLConnection) ((FileURLConnection) urlConnection).close();
                if (urlConnection.getInputStream() != null) {
                    urlConnection.getInputStream().close();
                }
            }
        } catch (final Throwable t) {
        }
    }

    /**
         * Parses a resource pointed by a <code>java.io.File</code> to an instance of the
         * <code>net.sourceforge.retriever.collector.handler.Document</code> class.
         * 
         * @param file The resource to be parsed.
         * @return The resource parsed.
         */
    public List<Document> parse(final File file) {
        try {
            return this.parse(file.toURI().toURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
         * <p>
         * Creates the right parser for a given content type.
         * </p>
         * 
         * <p>
         * If there isn't a parser for the content type, then a ContentTypeNotMappedException is
         * thrown.
         * </p>
         * 
         * @param contentType The content type used to create the right parser.
         * @return The parser that matches the given content type.
         * @throws ContentTypeNotMappedException If the content type doesn't have a parser.
         * @throws IllegalAccessException TODO
         * @throws InstantiationException TODO
         */
    public static Parser createUsingContentType(final String contentType) throws ContentTypeNotMappedException, InstantiationException, IllegalAccessException {
        final Class<? extends Parser> parser = parsersByContentType.get(contentType.toLowerCase());
        if (parser != null) {
            return parser.newInstance();
        } else {
            throw new ContentTypeNotMappedException("The content type " + contentType + " isn't supported.");
        }
    }

    /**
         * <p>
         * Given the extension of a file, returns the associated content type.
         * </p>
         * 
         * <p>
         * For instance, the extension html will return the content type text/html, while the pdf
         * extension will result in application/pdf.
         * </p>
         * 
         * @param extension The extension used to guess a content type.
         * @return The content type of some extension.
         * @throws ExtensionException If the extension is null or isn't mapped.
         */
    public static String getContentTypeFromExtension(final String extension) throws ExtensionException {
        if (extension == null) {
            throw new ExtensionException("The extension can't be null.");
        }
        final String contentType = contentTypesByFileType.get(extension.toLowerCase());
        if (contentType == null) {
            throw new ExtensionException("The extension " + extension + " doesn't have a content type or isn't mapped.");
        }
        return contentType;
    }

    /**
         * <p>
         * This method gets the path part of an URL, retrieves the extension portion of the resource
         * name and returns the content type with the extracted extension.
         * </p>
         * 
         * <p>
         * For instance, the URL http://www.test.com/index.html will return the content type
         * text/html.
         * </p>
         * 
         * @param url Object used to guess a content type.
         * @return The content type of the resource pointed by the URL.
         * @throws ExtensionException If the URL's extension can't be determined.
         */
    public static String getContentTypeFromURL(final URL url) throws ExtensionException {
        final String path = url.getPath();
        final int indexOfDot = path.lastIndexOf(".");
        if (indexOfDot == -1) {
            if (url.getProtocol().equalsIgnoreCase("http")) {
                return "text/html";
            } else {
                throw new ExtensionException("Could't find the extension of " + url.toExternalForm());
            }
        }
        return getContentTypeFromExtension(path.substring(indexOfDot + 1));
    }

    protected InputStream copyInputStream(final InputStream input) {
        final int BUFFER_SIZE = 2048;
        ByteArrayOutputStream fos = null;
        try {
            fos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            int length = input.read(buffer, 0, BUFFER_SIZE);
            while (length > -1) {
                fos.write(buffer, 0, length);
                length = input.read(buffer, 0, BUFFER_SIZE);
            }
            return new ByteArrayInputStream(fos.toByteArray());
        } catch (final Throwable t) {
            return input;
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (final IOException e) {
            }
        }
    }

    protected void setLenght(final InputStream inputStream, final Document document) {
        try {
            inputStream.reset();
            final int BUFFER_SIZE = 2048;
            final byte[] buffer = new byte[BUFFER_SIZE];
            int length = 0;
            int i = 0;
            while ((i = inputStream.read(buffer, 0, BUFFER_SIZE)) > 0) {
                length += i;
            }
            document.setLength(length);
        } catch (final IOException e) {
        } finally {
            try {
                inputStream.reset();
            } catch (final IOException e) {
            }
        }
    }

    protected void setCharSet(final InputStream inputStream, final Document document) {
        try {
            inputStream.reset();
            final CharsetDetector charsetDetector = new CharsetDetector();
            charsetDetector.setText(inputStream);
            document.setCharset(charsetDetector.detect().getName());
        } catch (final IOException e) {
        } finally {
            try {
                inputStream.reset();
            } catch (final IOException e) {
            }
        }
    }
}
