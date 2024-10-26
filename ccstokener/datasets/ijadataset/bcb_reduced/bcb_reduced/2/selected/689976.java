package com.beanstalktech.servlet.utility;

import com.beanstalktech.common.context.Application;
import com.beanstalktech.common.context.AppEvent;
import com.beanstalktech.common.context.Context;
import com.beanstalktech.common.context.Table;
import com.beanstalktech.common.utility.StringUtility;
import com.beanstalktech.common.utility.TokenizerParseException;
import com.beanstalktech.common.utility.TokenLabelNotFoundException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Vector;

/**
 * Parses a stream into a vector of tokens (TokenizedStream)
 * representing a character string with embedded
 * expression tags. The tokenize method accepts the name of a file or
 * a URL. The file/URL is expected to contain a stream of characters
 * with patterns of the form &lt;% expression %&gt; embedded. The expressions
 * are separated from the surrounding text in the tokenized stream. Each
 * expression or intervening text is represented by a StreamToken object
 * in the TokenizedStream vector.
 * <P>
 * Version 1.1: Adds ability to use a String object as the source of the
 * template.
 * <P>
 * Version 1.2: Modifies read of the template input stream to terminate
 * only on read() returning -1, indicating end of file. No longer terminates
 * when input stream reports isReady() = false. This was causing premature
 * termination for remote URLs.
 * <P>
 * Version 1.3: Modifies read of template from a URL to allow reading from a
 * local resource (jar) file. If application property "getTemplateFromResources"
 * is set to "true", the local resource file is read when a URL template is
 * requested. The value of application property "resourcesTemplatePathString"
 * is used as the path information in the resource file. If the URL is not found
 * in the resource file, normal HTTP URL processing takes place.
 * <P>
 * Version 1.4: New constructor accepting an escape character and an array of characters to
 * be escaped. (See below for description of usage in parsing and writing.)
 * <P>
 * Version 1.4: Parser method checks for existence of escape character. If one is defined,
 * characters following the escape character in the string being parsed are not matched to
 * delimiting characters in the template, and the escape character is removed from parsed values.
 * <P>
 * Version 1.4: Write method checks for existence of characters to be escaped, and an
 * escape character. As variable tokens are written, each occurrence of an escaped character
 * is preceded by the escape character as the output is written.
 * <P>
 * Version 2.0:
 * The tokenizer function is separated into a separate class (this one) from
 * the writing and parsing functions that use the tokenized stream (TokenizedStream).
 * <P>
 * Constructor accepts AppEvent object to provide access to application
 * properties during initialization. This enables multiple applications to
 * co-exist with different tokenizer properties.
 * <P>
 * @author Stuart Sugarman/Beanstalk Technologies LLC
 * @version 1.1 11/2/2000
 * @version 1.2 2/2/2001
 * @version 1.3 5/7/2001
 * @version 1.4 7/26/2001
 * @version 2.0 7/19/2002
 * @since Beanstalk V1.0
 * @see com.beanstalktech.common.utility.StreamToken
 */
public class StreamTokenizer {

    public static final int FILE_STREAM = 0;

    public static final int URL_STREAM = 1;

    public static final int STRING_STREAM = 2;

    protected static final int OUTSIDE_EXPRESSION = 0;

    protected static final int START_EXPRESSION = 1;

    protected static final int END_EXPRESSION = 2;

    protected static final int INSIDE_EXPRESSION = 3;

    protected static final char EXPRESSION_LEFT_CHAR = '<';

    protected static final char EXPRESSION_RIGHT_CHAR = '>';

    protected static final char EXPRESSION_INSIDE_CHAR = '%';

    protected String m_fileRoot;

    protected String m_URLRoot;

    protected URL m_documentBase;

    protected boolean m_getTemplateFromResources = false;

    protected String m_resourcesTemplatePathString = "";

    protected boolean m_useCache = true;

    /**
    * Constructs and initializes tokenizer from application properties.
    *
    * @param evt AppEvent with reference to Application
    */
    public StreamTokenizer(Application application) {
        try {
            String useCacheStr = application.getApplicationContext().getProperty("useTemplateCache");
            if (useCacheStr != null && useCacheStr.equalsIgnoreCase("FALSE")) {
                m_useCache = false;
            }
            m_URLRoot = application.getApplicationContext().getProperty("URLRoot");
            String getTemplateFromResources = application.getApplicationContext().getProperty("getTemplateFromResources");
            if (getTemplateFromResources != null && getTemplateFromResources.equalsIgnoreCase("true")) {
                m_getTemplateFromResources = true;
                m_resourcesTemplatePathString = application.getApplicationContext().getProperty("resourcesTemplatePathString");
                if (m_resourcesTemplatePathString == null) {
                    m_resourcesTemplatePathString = "";
                }
            }
            m_documentBase = application.getDocumentBase();
            m_fileRoot = application.getApplicationContext().getProperty("fileRoot");
        } catch (Exception e) {
            application.getLogger().logError(1, "StreamTokenizer: Failed to initialize. Exception: " + e);
        }
    }

    /**
    * Constructs a TokenizedStream from a file or URL cotaining
    * embedded expressions.
    *
    * @param evt AppEvent with reference to Application
    * @param streamName The name of the file or URL. Version 1.1: can
    * also be the template string itself if streamType is STRING_STREAM.
    * @param streamType Indicates the type of the input stream. Must be either
    * FILE_STREAM or URL_STREAM.
    * @param escapeChar Character to treat as an escaping character in parsing and/or
    * a character to precede escaped charactes in writing.
    * @param escapedChars Array of characters to be preceded by the escape character
    * in writing.
    * <P>
    * @exception java.io.IOException
    */
    public TokenizedStream tokenize(AppEvent evt, String streamName, int streamType) throws IOException {
        return tokenize(evt, streamName, streamType, TokenizedStream.NO_ESCAPE_CHAR, null);
    }

    /**
    * Constructs a TokenizedStream from a file or URL cotaining
    * embedded expressions.
    *
    * @param evt AppEvent with reference to Application
    * @param streamName The name of the file or URL. Version 1.1: can
    * also be the template string itself if streamType is STRING_STREAM.
    * @param streamType Indicates the type of the input stream. Must be either
    * FILE_STREAM or URL_STREAM.
    * @param escapeChar Character to treat as an escaping character in parsing and/or
    * a character to precede escaped charactes in writing.
    * @param escapedChars Array of characters to be preceded by the escape character
    * in writing.
    * <P>
    * @exception java.io.IOException
    */
    public TokenizedStream tokenize(AppEvent evt, String streamName, int streamType, char escapeChar, char escapedChars[]) throws IOException {
        try {
            TokenizedStream tokens = new TokenizedStream(escapeChar, escapedChars);
            Reader stream;
            if (streamType == STRING_STREAM) {
                stream = new StringReader(streamName);
            } else if (streamType == URL_STREAM) {
                stream = new BufferedReader(getURL(streamName));
            } else {
                stream = new BufferedReader(getFile(streamName));
            }
            StringBuffer stringBuffer = new StringBuffer(512);
            int tokenType = StreamToken.TEXT_TOKEN;
            int state = OUTSIDE_EXPRESSION;
            int i = -1;
            while ((i = stream.read()) > -1) {
                char c = (char) i;
                switch(c) {
                    case EXPRESSION_LEFT_CHAR:
                        if (state == OUTSIDE_EXPRESSION) {
                            state = START_EXPRESSION;
                        } else {
                            stringBuffer.append(c);
                        }
                        break;
                    case EXPRESSION_INSIDE_CHAR:
                        if (state == START_EXPRESSION) {
                            state = INSIDE_EXPRESSION;
                            tokens.addElement(new StreamToken(tokenType, stringBuffer.toString()));
                            stringBuffer = new StringBuffer(512);
                            tokenType = StreamToken.VARIABLE_TOKEN;
                        } else if (state == INSIDE_EXPRESSION) {
                            state = END_EXPRESSION;
                        } else {
                            stringBuffer.append(c);
                        }
                        break;
                    case EXPRESSION_RIGHT_CHAR:
                        if (state == END_EXPRESSION) {
                            state = OUTSIDE_EXPRESSION;
                            putExpressionToken(tokens, stringBuffer);
                            stringBuffer = new StringBuffer(512);
                            tokenType = StreamToken.TEXT_TOKEN;
                        } else {
                            stringBuffer.append(c);
                        }
                        break;
                    default:
                        if (state == START_EXPRESSION) {
                            stringBuffer.append(EXPRESSION_LEFT_CHAR);
                            state = OUTSIDE_EXPRESSION;
                        } else if (state == END_EXPRESSION) {
                            stringBuffer.append(EXPRESSION_INSIDE_CHAR);
                            state = INSIDE_EXPRESSION;
                        }
                        stringBuffer.append(c);
                        break;
                }
            }
            if (stringBuffer.length() > 0) {
                tokens.addElement(new StreamToken(tokenType, stringBuffer.toString()));
            }
            stream.close();
            return tokens;
        } catch (IOException e) {
            evt.getApplication().getLogger().logError(1, "StreamTokenizer encountered IO Exception while tokenizing stream. " + "Exception: \n" + e);
            throw e;
        } catch (Exception e) {
            evt.getApplication().getLogger().logError(1, "StreamTokenizer encountered exception while tokenizing stream. " + "Exception: \n" + e);
            throw new IOException();
        }
    }

    /**
   * Parses an embedded expression token and stores it with
   * appropriate token type.
   */
    protected static void putExpressionToken(Vector tokens, StringBuffer tokenBuffer) throws TokenizerParseException {
        String tokenValue = tokenBuffer.toString().trim();
        tokens.addElement(new StreamToken(tokenValue));
    }

    public InputStreamReader getURL(String streamName) throws IOException, MalformedURLException {
        URL url = null;
        if (m_getTemplateFromResources) {
            url = m_documentBase.getClass().getResource(m_resourcesTemplatePathString + streamName);
        }
        if (url == null) {
            url = new URL(m_documentBase, m_URLRoot + streamName);
        }
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(false);
        urlConnection.setUseCaches(false);
        return new InputStreamReader((urlConnection.getInputStream()));
    }

    public Reader getFile(String streamName) throws FileNotFoundException {
        Reader reader;
        if (m_getTemplateFromResources) {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(m_resourcesTemplatePathString + streamName);
            reader = new InputStreamReader(stream);
        } else {
            reader = new FileReader(m_fileRoot + streamName);
        }
        return reader;
    }

    public boolean fileExists(String streamName) {
        if (m_getTemplateFromResources) {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(m_resourcesTemplatePathString + streamName);
            return (stream != null);
        } else {
            return new File(m_fileRoot + streamName).exists();
        }
    }
}
