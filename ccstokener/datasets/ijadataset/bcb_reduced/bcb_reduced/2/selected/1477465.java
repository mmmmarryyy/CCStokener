package org.bongolipi.btrans.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.bongolipi.util.Data;
import java.io.FileNotFoundException;
import java.io.IOException;
import static org.bongolipi.btrans.util.BtransConstants.*;

public class InputConverter {

    private static final String CONVERTIBLE_STRING = "CONVERTIBLE";

    private static final String ESCAPED_STRING = "ESCAPED";

    private static final Logger log = Logger.getLogger(InputConverter.class);

    private static Hashtable<String, InputConverter> converters = new Hashtable<String, InputConverter>();

    private Object[][] aPattern = null;

    private int patternCount = 0;

    private String schemeName = "";

    private String schemeFile = null;

    private String schemeFilePath = null;

    private String escapeStartStr = null;

    private String escapeEndStr = null;

    private String escapeStartReplaceStr = null;

    private String escapeEndReplaceStr = null;

    private boolean isEscapeSet = false;

    /**
	 * This is the only way to get an instance of this class.
	 */
    public static InputConverter getConverter(String fromScheme, String toScheme) throws BtransException {
        String lSchemeName = new StringBuilder(fromScheme).append('_').append(toScheme).toString().toLowerCase();
        InputConverter c = converters.get(lSchemeName);
        if (null == c) {
            c = new InputConverter(fromScheme, toScheme);
            converters.put(lSchemeName, c);
        }
        return c;
    }

    /**
	 * Private constructor
	 */
    private InputConverter(String fromScheme, String toScheme) throws BtransException {
        this.schemeName = new StringBuilder(fromScheme).append('_').append(toScheme).toString().toLowerCase();
        schemeFile = this.schemeName.toLowerCase() + TYPING_SCHEME_FILE_SUFFIX;
        String[] lines = readFileToLineArray(schemeFile);
        this.populatePatternArray(lines);
        Data.sort(lines);
    }

    /**
	 * Reads the typing schema file line-by-line and puts the lines in an array
	 */
    private String[] readFileToLineArray(String fileName) throws BtransException {
        InputStream is = getFileAsInputStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        Vector<String> v = new Vector<String>();
        String[] dummy = {};
        log.info("Loading typing scheme file: " + fileName);
        try {
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(TYPING_SCHEME_LINE_COMMENT_CHAR) && !line.trim().equals("")) {
                    if (log.isDebugEnabled()) {
                        log.debug(line);
                    }
                    v.add(line);
                }
            }
        } catch (IOException ioEx) {
            throw new BtransException(ioEx);
        }
        return v.toArray(dummy);
    }

    /**
	 * Loads the schema file as InputStream
	 */
    private InputStream getFileAsInputStream(String file) throws BtransException {
        InputStream is;
        ClassLoader loader;
        URL url = null;
        try {
            loader = InputConverter.class.getClassLoader();
            url = loader.getResource(file);
            log.info("Loading file from " + url);
            schemeFilePath = new File(url.toURI()).getAbsolutePath();
            is = url.openStream();
            return is;
        } catch (FileNotFoundException fnfEx) {
            throw new BtransException("Cannot find file " + url.toString());
        } catch (Exception ex) {
            throw new BtransException("Cannot load file " + file + ". Reason: " + ex.getMessage());
        }
    }

    /**
	 * Takes a sorted String array - typically generated by 
	 * readFileToLineArray() method. It than breaks each array elemen into 
	 * three-parts: ordinal number, pattern string, replacement string. First 
	 * two part are delimited by a period (".") and last two parts are 
	 * delimited by an equal ("=") characher.
	 * <br />
	 * It then populates a 2-D Object araay with regex Pattern object of 
	 * the pattern string as the 0-th element and corresponding replacement
	 * string as the 1st element of each row.
	 */
    private void populatePatternArray(String[] aLines) throws BtransException {
        patternCount = 0;
        String line, key, value;
        Pattern p;
        Vector vPattern = new Vector();
        Object[] aPatternLine = null;
        log.info("Creating regext pattern for typing scheme " + schemeName);
        for (int cnt = 0; cnt < aLines.length; cnt++) {
            line = aLines[cnt];
            int delimAt = line.indexOf(TYPING_SCHEME_DELIM);
            if (-1 == delimAt || delimAt == line.length()) {
                throw new BtransException("Invalid line at " + (cnt + 1) + " in " + schemeFile);
            }
            key = line.substring(0, delimAt);
            value = line.substring(delimAt + 1);
            delimAt = key.indexOf(TYPING_SCHEME_ORDINAL_DELIM);
            if (-1 == delimAt || delimAt == key.length()) {
                throw new BtransException("Invalid line at " + (cnt + 1) + " in " + schemeFile);
            }
            key = key.substring(delimAt + 1);
            if (key.equals(ESCAPE_START_KEYWORD)) {
                escapeStartStr = value;
            } else if (key.equals(ESCAPE_END_KEYWORD)) {
                escapeEndStr = value;
            }
            if (key.equals(ESCAPE_START_REPLACE_KEYWORD)) {
                escapeStartReplaceStr = value;
            } else if (key.equals(ESCAPE_END_REPLACE_KEYWORD)) {
                escapeEndReplaceStr = value;
            } else {
                aPatternLine = new Object[2];
                p = Pattern.compile(key);
                aPatternLine[0] = p;
                aPatternLine[1] = value;
                vPattern.add(aPatternLine);
                log.info("regex: '" + key + "' | replacment: '" + value + "'");
            }
        }
        validate();
        patternCount = vPattern.size();
        aPattern = new Object[patternCount][2];
        aPattern = (Object[][]) vPattern.toArray(aPattern);
    }

    /**
	 * Returns the typing schema name that this InputConverter represents
	 */
    public String getSchemeName() {
        return schemeName;
    }

    /**
	 * Returns the path to the typing schema file that this InputConverter 
	 * represents
	 */
    public String getSchemeFilePath() {
        return schemeFilePath;
    }

    public String convert(String input) throws Exception {
        String converted = "";
        StringBuilder output = new StringBuilder();
        if (log.isDebugEnabled()) {
            log.debug("Number of patterns: " + patternCount);
            log.debug("input: " + input);
        }
        Pattern p;
        String replace;
        String[][] chunks = this.doChunking(input);
        for (int count = 0; count < chunks.length; count++) {
            if (chunks[count][0].equals(CONVERTIBLE_STRING)) {
                converted = chunks[count][1];
                if (log.isDebugEnabled()) {
                    log.debug("convertible chunk found");
                }
                for (int cnt = 0; cnt < patternCount; cnt++) {
                    p = (Pattern) aPattern[cnt][0];
                    replace = (String) aPattern[cnt][1];
                    converted = this.convert(p, converted, replace);
                }
                output.append(converted);
            } else if (chunks[count][0].equals(ESCAPED_STRING)) {
                if (log.isDebugEnabled()) {
                    log.debug("escaped chunk found");
                }
                output.append(escapeStartReplaceStr).append(chunks[count][1]).append(escapeEndReplaceStr);
            }
            if (log.isDebugEnabled()) {
                log.debug("convert(): output text now: " + output.toString());
            }
        }
        return output.toString();
    }

    private boolean validate() {
        if (null != escapeStartStr && null == escapeStartReplaceStr) {
            log.info("No value for " + ESCAPE_START_REPLACE_KEYWORD + " found. Using default " + ESCAPE_START_REPLACE_DEFAULT);
            escapeStartReplaceStr = ESCAPE_START_REPLACE_DEFAULT;
        }
        if (null != escapeEndStr && null == escapeEndReplaceStr) {
            log.info("No value for " + ESCAPE_END_REPLACE_KEYWORD + " found. Using default " + ESCAPE_END_REPLACE_DEFAULT);
            escapeEndReplaceStr = ESCAPE_END_REPLACE_DEFAULT;
        }
        return true;
    }

    private String[][] doChunking(String input) {
        Vector chunk = new Vector();
        String[] contentArr = null;
        String[][] dummy = new String[0][0];
        int escapeStart = -1, escapeEnd = -1;
        int startFrom = 0;
        String escaped;
        if (input.length() == 1) {
            contentArr = new String[2];
            contentArr[0] = CONVERTIBLE_STRING;
            contentArr[1] = input;
            chunk.add(contentArr);
        }
        while (startFrom < input.length() - 1) {
            escapeStart = input.indexOf(escapeStartStr, startFrom);
            if (escapeStart != -1) {
                escapeEnd = input.indexOf(escapeEndStr, escapeStart + escapeStartStr.length());
                if (escapeEnd != -1) {
                    contentArr = new String[2];
                    contentArr[0] = CONVERTIBLE_STRING;
                    contentArr[1] = input.substring(startFrom, escapeStart);
                    chunk.add(contentArr);
                    contentArr = new String[2];
                    contentArr[0] = ESCAPED_STRING;
                    contentArr[1] = input.substring(escapeStart + escapeStartStr.length(), escapeEnd);
                    chunk.add(contentArr);
                    startFrom = escapeEnd + escapeEndStr.length();
                    continue;
                } else {
                    contentArr = new String[2];
                    contentArr[0] = ESCAPED_STRING;
                    contentArr[1] = input.substring(escapeStart + escapeStartStr.length());
                    chunk.add(contentArr);
                }
            } else {
                contentArr = new String[2];
                contentArr[0] = CONVERTIBLE_STRING;
                contentArr[1] = input.substring(startFrom);
                chunk.add(contentArr);
            }
            startFrom = input.length() - 1;
        }
        return (String[][]) chunk.toArray(dummy);
    }

    private String convert(Pattern p, String input, String replace) throws Exception {
        StringBuffer out = new StringBuffer();
        Matcher m = p.matcher(input);
        if (log.isDebugEnabled()) {
            log.debug("looking in scheme " + schemeName + " for pattern '" + p.pattern() + "' to be replaced with '" + replace + "'");
        }
        try {
            while (m.find()) {
                m.appendReplacement(out, replace);
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println("Pattern = '" + p.pattern() + "', replacement = '" + replace + "'");
            ex.printStackTrace();
            throw (ex);
        }
        m.appendTail(out);
        return out.toString();
    }

    private String readFile(String path) throws IOException {
        System.out.println("Reading from " + path);
        BufferedReader br = new BufferedReader(new FileReader(path));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void writeFile(String content, String path) throws IOException {
        System.out.println("Writing to " + path);
        FileWriter writer = new FileWriter(path, false);
        writer.write(content, 0, content.length());
        writer.close();
    }
}
