package edu.upmc.opi.caBIG.caTIES.installer.pipes.creole;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Loads regular expression set from a file.
 * 
 * @author mitchellkj@upmc.edu
 * @author SOO IL KIM (skim@dsg.bwh.harvard.edu)
 * @version $Id: CaTIES_RegExPatternMapper.java,v 1.1 2005/12/06 18:51:45
 * mitchellkj Exp $
 * @since 1.4.2_04
 */
public class CaTIES_RegExPatternMapper {

    /**
     * Field MAX_PATTERN_NUMBER. (value is "50 ;")
     */
    protected static final int MAX_PATTERN_NUMBER = 50;

    /**
     * Field COMMENT_DELIMETER. (value is ""//" ")
     */
    protected static final String COMMENT_DELIMETER = "//";

    /**
     * Field TAG_DELIMETER. (value is ""@" ")
     */
    protected static final String TAG_DELIMETER = "@";

    /**
     * Field tagKeyMap.
     */
    protected Map tagKeyMap = new HashMap();

    /**
     * Field patternsAsStrings.
     */
    protected String[] patternsAsStrings = new String[MAX_PATTERN_NUMBER];

    /**
     * Field patterns.
     */
    protected Pattern[] patterns = null;

    /**
     * Field configFileName.
     */
    protected String configFileName = null;

    /**
     * Field debugging.
     */
    protected boolean debugging = false;

    /**
     * Field currentTagIndex.
     */
    protected int currentTagIndex = 0;

    /**
     * Constructor for CaTIES_RegExPatternMapper.
     */
    public CaTIES_RegExPatternMapper() {
        configTagPattern(this.configFileName);
        preCompilePatterns();
    }

    /**
     * Constructor for CaTIES_RegExPatternMapper.
     * 
     * @param configFileName String
     */
    public CaTIES_RegExPatternMapper(String configFileName) {
        this.configFileName = configFileName;
        configTagPattern(this.configFileName);
        preCompilePatterns();
    }

    /**
     * Method getDebugging.
     * 
     * @return Boolean
     */
    public Boolean getDebugging() {
        return new Boolean(this.debugging);
    }

    /**
     * Method setDebugging.
     * 
     * @param debugging Boolean
     */
    public void setDebugging(Boolean debugging) {
        this.debugging = debugging.booleanValue();
    }

    /**
     * Method configTagPattern.
     * 
     * @param configFileName String
     */
    private void configTagPattern(String configFileName) {
        String line = "";
        String tag = "";
        try {
            String patternFileAsString = null;
            if (configFileName.startsWith("http")) {
                patternFileAsString = readFileUsingHttp(configFileName);
            } else {
                patternFileAsString = readFileUsingFileUrl(configFileName);
            }
            StringTokenizer st = new StringTokenizer(patternFileAsString, "\n");
            while (st.hasMoreTokens()) {
                processLine(st.nextToken());
            }
        } catch (Exception ex) {
            System.err.println("Error opening file: " + ex.getMessage());
        }
    }

    /**
     * Method processLine.
     * 
     * @param line String
     */
    protected void processLine(String line) {
        line = line.trim();
        if (this.debugging) {
            System.out.println("processing ==> " + String.valueOf(line));
        }
        if (line.length() == 0 || line.startsWith(COMMENT_DELIMETER)) {
            return;
        } else if (line.startsWith(TAG_DELIMETER) && line.length() > TAG_DELIMETER.length()) {
            String tag = line.substring(TAG_DELIMETER.length(), line.length());
            if (this.tagKeyMap.get(tag) == null) {
                this.tagKeyMap.put(tag, new Integer(this.tagKeyMap.size()));
            }
            this.currentTagIndex = ((Integer) this.tagKeyMap.get(tag)).intValue();
            this.patternsAsStrings[this.currentTagIndex] = new String();
        } else {
            this.patternsAsStrings[this.currentTagIndex] = patternsAsStrings[this.currentTagIndex] + line;
        }
    }

    /**
     * Method readFileUsingFileReader.
     * 
     * @param fileName String
     * 
     * @return String
     */
    protected String readFileUsingFileReader(String fileName) {
        String response = "";
        BufferedReader inbuffer;
        String line = null;
        try {
            String curDir = System.getProperty("user.dir");
            if (this.debugging) {
                System.out.println("The user directory is " + curDir);
            }
            String fullPathFileName = curDir + fileName;
            inbuffer = new BufferedReader(new FileReader(fullPathFileName));
            while (line != null) {
                line = inbuffer.readLine();
                response += line + "\n";
            }
            if (response.endsWith("\n")) {
                response = response.substring(0, response.length() - 1);
            }
            inbuffer.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
        return response;
    }

    /**
     * Method readFileUsingFileUrl.
     * 
     * @param fileUrlName String
     * 
     * @return String
     */
    protected String readFileUsingFileUrl(String fileUrlName) {
        String response = "";
        try {
            URL url = new URL(fileUrlName);
            URLConnection connection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                response += inputLine + "\n";
            }
            if (response.endsWith("\n")) {
                response = response.substring(0, response.length() - 1);
            }
            in.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
        return response;
    }

    /**
     * Method readFileUsingHttp.
     * 
     * @param fileUrlName String
     * 
     * @return String
     */
    protected String readFileUsingHttp(String fileUrlName) {
        String response = "";
        try {
            URL url = new URL(fileUrlName);
            URLConnection connection = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            httpConn.setRequestProperty("Content-Type", "text/html");
            httpConn.setRequestProperty("Content-Length", "0");
            httpConn.setRequestMethod("GET");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.setAllowUserInteraction(false);
            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                response += inputLine + "\n";
            }
            if (response.endsWith("\n")) {
                response = response.substring(0, response.length() - 1);
            }
            in.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
        return response;
    }

    /**
     * Method preCompilePatterns.
     */
    protected void preCompilePatterns() {
        int idx = 0;
        try {
            this.patterns = new Pattern[this.currentTagIndex + 1];
            for (; idx <= this.currentTagIndex; idx++) {
                String patternToCompile = this.patternsAsStrings[idx];
                if (this.debugging) {
                    System.out.println("Compiling pattern " + patternToCompile);
                }
                this.patterns[idx] = Pattern.compile(patternToCompile);
            }
        } catch (Exception x) {
            System.err.println("FAILED to compile pattern\n" + String.valueOf(this.patternsAsStrings[idx]));
            x.printStackTrace();
        }
    }

    /**
     * Method getPattern.
     * 
     * @param tag String
     * 
     * @return Pattern
     */
    public Pattern getPattern(String tag) {
        int patternIndex = ((Integer) this.tagKeyMap.get(tag)).intValue();
        return this.patterns[patternIndex];
    }

    /**
     * Method getTagKeyMap.
     * 
     * @return Map
     */
    public Map getTagKeyMap() {
        return this.tagKeyMap;
    }

    /**
     * Method toString.
     * 
     * @return String
     */
    public String toString() {
        String s = "";
        Set regexPatterns = this.tagKeyMap.keySet();
        Iterator regexPatternsIterator = regexPatterns.iterator();
        while (regexPatternsIterator.hasNext()) {
            String tag = (String) regexPatternsIterator.next();
            s += TAG_DELIMETER + tag + "\n";
            Pattern compiledPattern = getPattern(tag);
            if (compiledPattern != null) {
                s += compiledPattern.pattern() + "\n";
            } else {
                s += "ERROR Null pattern!!!\n";
            }
        }
        return s;
    }
}
