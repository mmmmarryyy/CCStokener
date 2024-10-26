package com.beanstalktech.common.script;

import com.beanstalktech.common.script.ScriptToken;
import com.beanstalktech.common.utility.StringUtility;
import com.beanstalktech.common.command.CommandManager;
import com.beanstalktech.common.context.Application;
import com.beanstalktech.common.context.AppEvent;
import com.beanstalktech.common.context.Context;
import com.beanstalktech.common.utility.Logger;
import com.beanstalktech.common.utility.TokenizerParseException;
import com.beanstalktech.common.packager.PackagerManager;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Vector;
import java.util.Stack;

/**
/**
 * Parses a stream into a vector of tokens (TokenizedScript)
 * representing an executable goose script.
 * The tokenize method accepts the name of a file,
 * a URL, or a string stream. The input stream is parsed into
 * an array of ScriptToken objects which can then be executed.
 * <P>
 * @author Stuart Sugarman/Beanstalk Technologies LLC
 * @version 1.0 5/31/2001
 * @since Beanstalk V1.1
 */
public class ScriptTokenizer {

    public static final int FILE_STREAM = 0;

    public static final int URL_STREAM = 1;

    public static final int STRING_STREAM = 2;

    public static final String NOP = "NOP";

    public static final String SYNCHRONIZE = "Sync";

    public static final String IF = "IF";

    public static final String FOR = "FOR";

    public static final String WHILE = "WHILE";

    public static final String ELSE = "ELSE";

    public static final String TABLE = "TABLE";

    public static final String CONTEXT = "CONTEXT";

    public static final String ASSIGNMENT = "ASSIGNMENT";

    public static final char BLOCK_START_CHAR = '{';

    public static final char BLOCK_END_CHAR = '}';

    public static final char COMMAND_END_CHAR = ';';

    public static final char COMMENT_END_CHAR = '/';

    public static final char ASSIGNMENT_CHAR = '=';

    public static final char INCREMENT_CHAR = '+';

    public static final char DECREMENT_CHAR = '-';

    private static final int NULL_COMMAND = 0;

    private static final int ASSIGNMENT_COMMAND = 1;

    private static final int DIRECTIVE_COMMAND = 2;

    private static final int INCREMENT_BY_ONE_COMMAND = 3;

    private static final int DECREMENT_BY_ONE_COMMAND = 4;

    private static final int INCREMENT_BY_VAL_COMMAND = 5;

    private static final int DECREMENT_BY_VAL_COMMAND = 6;

    private static final int INVALID_COMMAND = 9;

    protected URL m_documentBase;

    protected String[] m_scriptPath;

    protected boolean m_useCache = true;

    protected boolean m_getScriptFromResources = false;

    protected String m_resourcesScriptPathString = "";

    protected Application m_application;

    protected Logger m_logger;

    private PackagerManager m_packagerManager;

    /**
    * Constructs and initializes tokenizer from application properties.
    *
    * @param application Application controlling the properties
    */
    public ScriptTokenizer(Application application) {
        try {
            m_application = application;
            m_logger = application.getLogger();
            m_packagerManager = application.getPackagerManager();
            String useCacheStr = application.getApplicationContext().getProperty("useScriptCache");
            if (useCacheStr != null && useCacheStr.equalsIgnoreCase("FALSE")) {
                m_useCache = false;
            }
            String getScriptFromResources = application.getApplicationContext().getProperty("getScriptFromResources");
            if (getScriptFromResources != null && getScriptFromResources.equalsIgnoreCase("true")) {
                m_getScriptFromResources = true;
                m_resourcesScriptPathString = application.getApplicationContext().getProperty("resourcesScriptPathString");
                if (m_resourcesScriptPathString == null) {
                    m_resourcesScriptPathString = "";
                }
            }
            setScriptPath(application.getApplicationContext().getProperty("scriptPath"));
            m_documentBase = application.getDocumentBase();
        } catch (Exception e) {
            m_logger.logError(1, "ScriptTokenizer: Failed to initialize. Exception: " + e);
        }
    }

    /**
    * Constructs a TokenizedScript from a file, string, or URL containing
    * BCSL directives. This version of the constructor uses cache if
    * application property "useScriptCache" is true and a cache is passed.
    * <P>
    * @param evt Application event with reference to application
    * @param streamName The name of the file, URL or string containing
    * the script source.
    * @param streamType Indicates the type of the input stream. Must be
    * FILE_STREAM, URL_STREAM or STRING_STREAM. If it is STRING_STREAM,
    * the parameter stream (see below) must contain the source string.
    * @param cache A context containing cached TokenizedScripts. If not null, it
    * is examined
    * for a previously cached version of the named script.
    * @return The ScriptTokenizer
    * @throws TokenizerParseException
    */
    public TokenizedScript tokenize(AppEvent evt, String streamName, int streamType, String streamSource, Context cache) throws TokenizerParseException {
        return create(evt, streamName, streamType, streamSource, cache, m_useCache);
    }

    /**
    * Constructs a TokenizedScript from a file, string, or URL containing
    * BCSL directives. This version of the constructor uses cache if
    * argument "useCache" is true and a cache is passed.
    * <P>
    * @param evt Application event with reference to application
    * @param streamName The name of the file, URL or string containing
    * the script source.
    * @param streamType Indicates the type of the input stream. Must be
    * FILE_STREAM, URL_STREAM or STRING_STREAM. If it is STRING_STREAM,
    * the parameter stream (see below) must contain the source string.
    * @param cache A context containing cached TokenizedScripts. If not null, it
    * is examined
    * for a previously cached version of the named script.
    * @param writeCache If true, script is written to cache
    * @return The tokenized script
    * @throws TokenizerParseExcpeption
    */
    public TokenizedScript tokenize(AppEvent evt, String streamName, int streamType, String streamSource, Context cache, boolean writeCache) throws TokenizerParseException {
        return create(evt, streamName, streamType, streamSource, cache, writeCache);
    }

    /**
    * Constructs a TokenizedScript from a file, string, or URL containing
    * BCSL directives.
    * <P>
    * @param evt Application event with reference to application
    * @param streamName The name of the file, URL or string containing
    * the script source.
    * @param streamType Indicates the type of the input stream. Must be
    * FILE_STREAM, URL_STREAM or STRING_STREAM. If it is STRING_STREAM,
    * the parameter stream (see below) must contain the source string.
    * @param cache A context containing cached TokenizedScripts. If not null, it
    * is examined
    * for a previously cached version of the named script.
    * @param writeCache If true, script is written to cache
    * @return The tokenized script
    * @throws TokenizerParseException
    */
    protected TokenizedScript create(AppEvent evt, String streamName, int streamType, String streamSource, Context cache, boolean writeCache) throws TokenizerParseException {
        TokenizedScript tokenizedScript = null;
        try {
            tokenizedScript = tokenizeStream(evt, (streamType == STRING_STREAM ? streamSource : streamName), streamType, cache);
            return tokenizedScript;
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenizerParseException(e.getMessage());
        }
    }

    public TokenizedScript tokenizeStream(AppEvent evt, String streamName, int streamType, Context cache) throws TokenizerParseException {
        Stack blockTypeStack = new Stack();
        Stack blockHeadStack = new Stack();
        Stack breakContinueStack = new Stack();
        Vector tokens = new Vector(50, 10);
        Vector phrases = null;
        int phraseIndex = -1;
        String scriptName = streamType == STRING_STREAM ? "(None)" : streamName;
        try {
            String script = readStream(evt, streamName, streamType, cache);
            phrases = StringUtility.tokenizeExpressionNew(evt, script, new char[] { ';', '{', '}', '/' }, null, new char[] { '"', '\'', '[', '(' }, new char[] { '"', '\'', ']', ')' }, true, false, false);
            StringBuffer buffer = new StringBuffer();
            for (phraseIndex = 0; phraseIndex < phrases.size(); phraseIndex++) {
                String phrase = phrases.elementAt(phraseIndex).toString().trim();
                char delim = phrase.length() < 1 ? ' ' : phrase.charAt(0);
                switch(delim) {
                    case COMMAND_END_CHAR:
                        String command = buffer.toString();
                        buffer = new StringBuffer();
                        parseCommand(evt, tokens, command, breakContinueStack);
                        break;
                    case BLOCK_START_CHAR:
                        String prefix = buffer.toString();
                        buffer = new StringBuffer();
                        parseBlockStart(evt, tokens, prefix, blockTypeStack, blockHeadStack, breakContinueStack);
                        break;
                    case BLOCK_END_CHAR:
                        buffer = new StringBuffer();
                        parseBlockEnd(evt, tokens, blockTypeStack, blockHeadStack, breakContinueStack);
                        break;
                    default:
                        buffer.append(phrase);
                        break;
                }
            }
            if (buffer.length() > 0) {
                String command = buffer.toString();
                parseCommand(evt, tokens, command, breakContinueStack);
            }
            ScriptToken array[] = new ScriptToken[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                array[i] = (ScriptToken) tokens.elementAt(i);
            }
            return new TokenizedScript(streamName, array);
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            String message = "Encountered an exception while processing script: " + scriptName + " Exception: " + e.getMessage();
            throw new TokenizerParseException(message);
        }
    }

    /**
	 * Parse a command from a phrase, creating a command script token.
	 * <P>
	 * @param evt Application event with reference to Application
	 * @param tokens Vector of script tokens to be added to
	 * @param command Phrase containing the command string
	 * @param breakContinueStack Stack containing break and continue token
	 * references.
	 * @throws ScriptCommandParserException
	 */
    protected static void parseCommand(AppEvent evt, Vector tokens, String command, Stack breakContinueStack) throws ScriptCommandParserException {
        try {
            Vector parts = StringUtility.tokenizeExpressionNew(evt, command, new char[] { '=', '(', ')', '+', '-' }, new char[] { '\n', '\r' }, new char[] { '"', '\'', '[' }, new char[] { '"', '\'', ']' }, true, false, false);
            int type = NULL_COMMAND;
            StringBuffer lval = new StringBuffer();
            StringBuffer rval = new StringBuffer();
            char pendingChar = ' ';
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.elementAt(i).toString().trim();
                if (type != NULL_COMMAND) {
                    rval.append(part);
                    continue;
                }
                char delim = part.length() < 1 ? ' ' : part.charAt(0);
                if (pendingChar == ' ') {
                    switch(delim) {
                        case '=':
                            type = ASSIGNMENT_COMMAND;
                            break;
                        case '(':
                            type = DIRECTIVE_COMMAND;
                            break;
                        case '+':
                        case '-':
                            pendingChar = delim;
                            break;
                        default:
                            lval.append(part);
                            break;
                    }
                } else {
                    switch(delim) {
                        case '=':
                            type = pendingChar == '+' ? INCREMENT_BY_VAL_COMMAND : pendingChar == '-' ? DECREMENT_BY_VAL_COMMAND : INVALID_COMMAND;
                            break;
                        case '+':
                            type = pendingChar == '+' ? INCREMENT_BY_ONE_COMMAND : INVALID_COMMAND;
                            break;
                        case '-':
                            type = pendingChar == '-' ? DECREMENT_BY_ONE_COMMAND : INVALID_COMMAND;
                            break;
                        default:
                            type = INVALID_COMMAND;
                            break;
                    }
                }
            }
            if (type == INVALID_COMMAND) {
                throw new TokenizerParseException("Invalid assignment operator in command.");
            }
            if (type == NULL_COMMAND) type = DIRECTIVE_COMMAND;
            String lvalString = lval.toString();
            String rvalString = rval.toString();
            if (lvalString.trim().equals("")) {
                throw new TokenizerParseException("Invalid or empty script command. ");
            }
            switch(type) {
                case ASSIGNMENT_COMMAND:
                    tokens.addElement(new AssignmentScriptToken(lvalString, rvalString));
                    break;
                case DIRECTIVE_COMMAND:
                    if (lvalString.equalsIgnoreCase("break")) {
                        BreakScriptToken token = new BreakScriptToken(-1);
                        tokens.addElement(token);
                        breakContinueStack.push(token);
                    } else if (lvalString.equalsIgnoreCase("continue")) {
                        ContinueScriptToken token = new ContinueScriptToken(-1);
                        tokens.addElement(token);
                        breakContinueStack.push(token);
                    } else {
                        int delimLocation = lvalString.lastIndexOf('.');
                        String target = "";
                        if (delimLocation > 0) {
                            String t = lvalString.substring(0, delimLocation);
                            target = "target=" + t + ",targetRef=\"" + t + "\"";
                            lvalString = lvalString.substring(delimLocation + 1);
                        }
                        if (rvalString.endsWith(")")) {
                            rvalString = rvalString.substring(0, rvalString.length() - 1);
                        }
                        tokens.addElement(new CommandScriptToken(lvalString, rvalString + (rvalString.equals("") || target.equals("") ? "" : ",") + target));
                        break;
                    }
                case INCREMENT_BY_ONE_COMMAND:
                    tokens.addElement(new AssignmentScriptToken(lvalString, lvalString + " + 1"));
                    break;
                case DECREMENT_BY_ONE_COMMAND:
                    tokens.addElement(new AssignmentScriptToken(lvalString, lvalString + " - 1"));
                    break;
                case INCREMENT_BY_VAL_COMMAND:
                    tokens.addElement(new AssignmentScriptToken(lvalString, lvalString + " + " + rvalString));
                    break;
                case DECREMENT_BY_VAL_COMMAND:
                    tokens.addElement(new AssignmentScriptToken(lvalString, lvalString + " - " + rvalString));
                    break;
            }
        } catch (Exception e) {
            String message = "ScriptTokenizer failed while attempting to translate the string: " + command + " into a command. Exception: " + e;
            throw new ScriptCommandParserException(message);
        }
    }

    /**
	 * Parse a block start from a phrase, creating a block start script token.
	 * <P>
	 * @param evt Application event with reference to Application
	 * @param tokens Vector of script tokens to be added to
	 * @param prefix Phrase containing the block prefix
	 * @param blockTypeStack Stack of previously encountered block types
	 * @param blockHeadStack Stack of previously created block script tokens
	 * @param breakContinueStack Stack containing break and continue token
	 * references.
	 * @throws TokenizerParseException
	 */
    protected static void parseBlockStart(AppEvent evt, Vector tokens, String prefix, Stack blockTypeStack, Stack blockHeadStack, Stack breakContinueStack) throws TokenizerParseException {
        try {
            Vector parts = StringUtility.tokenizeExpressionNew(evt, prefix, new char[] { '(', ')' }, null, new char[] { '"', '\'', '[' }, new char[] { '"', '\'', ']' }, true, false, false);
            boolean foundDirective = false;
            StringBuffer lval = new StringBuffer();
            StringBuffer rval = new StringBuffer();
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.elementAt(i).toString().trim();
                if (foundDirective) {
                    rval.append(part);
                    continue;
                }
                char delim = part.length() < 1 ? ' ' : part.charAt(0);
                switch(delim) {
                    case '(':
                        foundDirective = true;
                        break;
                    default:
                        lval.append(part);
                        break;
                }
            }
            String directive = lval.toString().toUpperCase();
            String expression = rval.toString();
            if (expression.endsWith(")")) {
                expression = expression.substring(0, expression.length() - 1);
            }
            int blockEndType = -1;
            if (directive.equals(IF)) {
                tokens.addElement(new IfScriptToken(expression));
                blockEndType = ScriptToken.IF_BLOCK_END_TOKEN;
            } else if (directive.equals(FOR)) {
                tokens.addElement(new ForScriptToken(expression));
                blockEndType = ScriptToken.FOR_BLOCK_END_TOKEN;
            } else if (directive.equals(WHILE)) {
                tokens.addElement(new WhileScriptToken(expression));
                blockEndType = ScriptToken.WHILE_BLOCK_END_TOKEN;
            } else if (directive.equals(ELSE)) {
                tokens.addElement(new ElseScriptToken());
                blockEndType = ScriptToken.ELSE_BLOCK_END_TOKEN;
            } else if (directive.equals(TABLE)) {
                tokens.addElement(new ForTableScriptToken(expression));
                blockEndType = ScriptToken.TABLE_BLOCK_END_TOKEN;
            } else if (directive.equals(CONTEXT)) {
                tokens.addElement(new ContextScriptToken(expression));
                blockEndType = ScriptToken.CONTEXT_BLOCK_END_TOKEN;
            }
            if (blockEndType == -1) {
                throw new TokenizerParseException("Found an opening brace, but couldn't identify the type of block. " + "It is preceded by the text: " + prefix);
            }
            blockTypeStack.push(new Integer(blockEndType));
            blockHeadStack.push(new Integer(tokens.size() - 1));
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            String message = "Couldn't translate the string: " + prefix + " into the beginning of a processing block. Exception: " + e.getMessage();
            throw new TokenizerParseException(message);
        }
    }

    /**
	 * Parse a block end from a phrase, creating a block end script token.
	 * <P>
	 * @param evt Application event with reference to Application
	 * @param tokens Vector of script tokens to be added to
	 * @param blockTypeStack Stack of previously encountered block types
	 * @param blockHeadStack Stack of previously created block script tokens
	 * @param breakContinueStack Stack containing break and continue token
	 * references.
	 */
    protected static void parseBlockEnd(AppEvent evt, Vector tokens, Stack blockTypeStack, Stack blockHeadStack, Stack breakContinueStack) throws TokenizerParseException {
        try {
            if (blockTypeStack.isEmpty() || blockHeadStack.isEmpty()) {
                throw new TokenizerParseException("A closing brace has no matching opening brace.");
            }
            BlockEndScriptToken endToken = new BlockEndScriptToken(((Integer) blockTypeStack.pop()).intValue(), ((Integer) blockHeadStack.pop()).intValue());
            tokens.addElement(endToken);
            if (endToken.iterates()) {
                int i = tokens.size() - 1;
                while (!breakContinueStack.isEmpty()) {
                    Object breakContinueToken = breakContinueStack.pop();
                    if (breakContinueToken instanceof BreakScriptToken) {
                        ((BreakScriptToken) breakContinueToken).setBlockEndIndex(i);
                    } else if (breakContinueToken instanceof ContinueScriptToken) {
                        ((ContinueScriptToken) breakContinueToken).setBlockEndIndex(i);
                    }
                }
            }
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            String message = "Failed to locate the end of a block. Exception: " + e.getMessage();
            throw new TokenizerParseException(message);
        }
    }

    /**
    * Physical read of a stream into a String object.
    *
    * @param evt Application event with reference to application
    * @param streamName See the constructor description above.
    * @param streamType See the constructor description above.
    * @param cache Cache of previously read streams
    *
    * @exception TokenizerParseException
    */
    public String readStream(AppEvent evt, String streamName, int streamType, Context cache) throws TokenizerParseException {
        StringBuffer stringBuffer = new StringBuffer(4096);
        Reader stream = null;
        boolean writeCache = false;
        try {
            if (streamType == STRING_STREAM) {
                return streamName;
            } else if (m_getScriptFromResources) {
                ClassLoader classLoader = getClass().getClassLoader();
                stream = new InputStreamReader(classLoader.getResourceAsStream(m_resourcesScriptPathString + streamName));
            } else if (streamType == URL_STREAM) {
                if (cache != null) {
                    Object object = cache.get(streamName);
                    if (object != null && object instanceof String) {
                        evt.getApplication().getLogger().logMessage(8, "ScriptTokenizer loading script from cache: " + streamName);
                        return (String) object;
                    } else {
                        writeCache = true;
                    }
                }
                stream = new BufferedReader(getURL(streamName));
            } else {
                stream = new BufferedReader(getFile(streamName));
            }
            int i = -1;
            while ((i = stream.read()) > -1) {
                stringBuffer.append((char) i);
            }
            if (stream != null) stream.close();
            String string = stringBuffer.toString();
            if (writeCache) {
                cache.put(streamName, string);
            }
            return string;
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            try {
                if (stream != null) stream.close();
            } catch (Exception e1) {
            }
            throw new TokenizerParseException("Unable to find script in the path(s) specified. Name: " + streamName + " Underlying Exception: " + e.getMessage());
        }
    }

    public synchronized void setUseCache(boolean useCache) {
        m_useCache = useCache;
    }

    /**
    * Get a script byte stream by attempting to connect to a URL. Directories in the
    * script server's space specified in the application property 'scriptPath'
    * are searched in the order they appear in the path.
    *
    * @parm streamName Name of the script (without extension or prefix)
    * @returns a reader for the script stream
    * @ TokenizerParseException if the script could not be found in the path
    */
    public Reader getURL(String streamName) throws TokenizerParseException {
        try {
            InputStream scriptStream = null;
            if (m_packagerManager != null) {
                for (int i = 0; i < m_scriptPath.length; i++) {
                    try {
                        scriptStream = m_packagerManager.getByteStream(m_scriptPath[i] + streamName + ".script");
                        if (scriptStream != null) {
                            InputStreamReader reader = new InputStreamReader(scriptStream);
                            return reader;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            for (int i = 0; i < m_scriptPath.length; i++) {
                try {
                    URL url = new URL(m_documentBase, m_scriptPath[i] + streamName + ".script");
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setDoOutput(false);
                    urlConnection.setDefaultUseCaches(true);
                    urlConnection.setUseCaches(true);
                    urlConnection.connect();
                    scriptStream = urlConnection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(scriptStream);
                    return reader;
                } catch (Exception e) {
                    continue;
                }
            }
            throw new TokenizerParseException("Could not find script in the URL path(s) specified: " + streamName);
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenizerParseException("Unable to find script in the URL path(s) specified. Name: " + streamName + " Underlying Exception: " + e);
        }
    }

    /**
    * Get a script byte stream by attempting to open a file. Directories in the
    * script server's space specified in the application property 'scriptPath'
    * are searched in the order they appear in the path.
    *
    * @parm streamName Name of the script (without extension or prefix)
    * @returns a reader for the script stream
    * @ TokenizerParseException if the script could not be found in the path
    */
    public Reader getFile(String streamName) throws TokenizerParseException {
        try {
            for (int i = 0; i < m_scriptPath.length; i++) {
                try {
                    String fileName = m_scriptPath[i] + streamName + ".script";
                    FileReader reader = new FileReader(fileName);
                    return reader;
                } catch (Exception e) {
                    continue;
                }
            }
            throw new TokenizerParseException("Could not find script in the file path(s) specified: " + streamName);
        } catch (TokenizerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenizerParseException("Unable to find script in the file path(s) specified. Name: " + streamName + " Underlying Exception: " + e);
        }
    }

    /**
	 * Set the script path that is searched for URL and file types
	 * of scripts. Original setting is from property 'scriptPath'.
	 *
	 * @parm scriptPath String containing the new script path, consisting
	 * of one or more directories (relative to the document root or file
	 * root). Directory names are separated by ';' characters.
	 */
    public void setScriptPath(String scriptPath) throws TokenizerParseException {
        try {
            m_scriptPath = scriptPath.split(";");
            for (int i = 0; i < m_scriptPath.length; i++) {
            }
        } catch (Exception e) {
            throw new TokenizerParseException("Unable to set the path for searching scripts. The path being parsed is: " + scriptPath + " The exception is: " + e);
        }
    }

    /**
	 * Validate a script's commands by checking the command
	 * name against registered commands.
	 *
	 * @param tokenizedScript - Tokenized verion of script to be
	 * checked
	 * @param commandManager - Command manager to be used to
	 * validate commands
	 * @throws TokenizerParseException if there is an unrecognized
	 * command
	 */
    public void checkCommands(TokenizedScript tokenizedScript, CommandManager commandManager) throws TokenizerParseException {
        TokenizerParseException te = new TokenizerParseException("Encountered invalid command(s): ");
        for (int i = 0; i < tokenizedScript.size(); i++) {
            ScriptToken token = tokenizedScript.getToken(i);
            if (token.getType() == ScriptToken.COMMAND_TOKEN) {
                String commandName = token.getTokenArgument(0);
                if (commandName.indexOf("<&") > 0) continue;
                if (!commandManager.isCommand(commandName)) {
                    if (!isScript(commandName)) {
                        te.addDetailMessage("A command, method or script is not recognized: " + commandName + " (Token " + i + ")");
                    }
                }
            }
        }
        if (te.getNumberOfMessages() > 0) throw te;
    }

    /**
	 * Determine if a string is the name of a script file
	 * in the script path
	 *
	 * @param scriptName Name of script
	 * @return true if the string is the name of a script file
	 */
    public boolean isScript(String scriptName) {
        for (int i = 0; i < m_scriptPath.length; i++) {
            try {
                String fileName = m_scriptPath[i] + scriptName + ".script";
                File scriptFile = new File(fileName);
                if (scriptFile.exists()) return true;
            } catch (Exception e) {
                continue;
            }
        }
        return false;
    }

    /**
	 * Validate a script's blocks by reconciling start
	 * blocks and endblocks.
	 *
	 * @param tokenizedScript - Tokenized verion of script to be
	 * checked
	 * @throws TokenizerParseException if there are mismatched
	 * blockstart and blockends
	 */
    public static void checkBlocks(TokenizedScript tokenizedScript) throws TokenizerParseException {
        int[] tokenCounts = new int[ScriptToken.TYPE_NAMES.length];
        for (int i = 0; i < tokenCounts.length; i++) {
            tokenCounts[i] = 0;
        }
        for (int i = 0; i < tokenizedScript.size(); i++) {
            int tokenType = tokenizedScript.getToken(i).getType();
            tokenCounts[tokenType]++;
        }
        if (tokenCounts[ScriptToken.IF_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.IF_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.IF_BLOCK_START_TOKEN, tokenizedScript);
        }
        if (tokenCounts[ScriptToken.ELSE_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.ELSE_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.ELSE_BLOCK_START_TOKEN, tokenizedScript);
        }
        if (tokenCounts[ScriptToken.FOR_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.FOR_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.FOR_BLOCK_START_TOKEN, tokenizedScript);
        }
        if (tokenCounts[ScriptToken.WHILE_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.WHILE_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.WHILE_BLOCK_START_TOKEN, tokenizedScript);
        }
        if (tokenCounts[ScriptToken.TABLE_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.TABLE_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.TABLE_BLOCK_START_TOKEN, tokenizedScript);
        }
        if (tokenCounts[ScriptToken.CONTEXT_BLOCK_START_TOKEN] != tokenCounts[ScriptToken.CONTEXT_BLOCK_END_TOKEN]) {
            throwUnbalancedException(ScriptToken.CONTEXT_BLOCK_START_TOKEN, tokenizedScript);
        }
    }

    /**
	 * Throw a TokenizerParseException indicating an unbalanced
	 * block
	 *
	 * @param tokenType - Type of block
	 * @param tokenizedScript - Script containing the unbalanced block
	 * @throws TokenizerParseException
	 */
    protected static void throwUnbalancedException(int tokenType, TokenizedScript tokenizedScript) throws TokenizerParseException {
        String tokenString = "(Unknown location)";
        for (int i = 0; i < tokenizedScript.size(); i++) {
            ScriptToken token = tokenizedScript.getToken(i);
            if (token.getType() == tokenType) {
                tokenString = token.toString();
                break;
            }
        }
        throw new TokenizerParseException("A block starting with a(n) " + ScriptToken.TYPE_NAMES[tokenType] + " token is missing a closing brace: " + tokenString);
    }
}
