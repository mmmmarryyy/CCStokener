package edu.rpi.usf.utils;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class Parameters extends Properties {

    private boolean addToSystemProperties;

    private File homeDirectory;

    private static Parameters systemParameters;

    public Parameters() {
        this(true);
    }

    private Parameters(boolean addToSystemProperties) {
        this.addToSystemProperties = addToSystemProperties;
    }

    public void setHomeDirectory(File f) {
        homeDirectory = f;
    }

    public File getHomeDirectory() {
        return (homeDirectory);
    }

    public String expand(String value) {
        StringBuffer expansion = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\\') {
                switch(value.charAt(i + 1)) {
                    case 'b':
                        expansion.deleteCharAt(expansion.length() - 1);
                        i++;
                        break;
                    case 'r':
                        expansion.append('\r');
                        i++;
                        break;
                    case 'n':
                        expansion.append('\n');
                        i++;
                        break;
                    case 't':
                        expansion.append('\t');
                        i++;
                        break;
                    default:
                        expansion.append(value.charAt(i + 1));
                        i++;
                        break;
                }
                continue;
            }
            if ((value.charAt(i) == '$') && (i < value.length() - 1)) {
                if (value.charAt(i + 1) == '{') {
                    int braceCount = 0;
                    StringBuffer envVar = new StringBuffer();
                    for (int j = i + 2; j < value.length(); j++) {
                        if (value.charAt(j) == '\\') {
                            envVar.append(value.charAt(j));
                            if (j < value.length()) {
                                envVar.append(value.charAt(j + 1));
                                j++;
                            }
                            continue;
                        }
                        if (value.charAt(j) == '{') {
                            braceCount++;
                        }
                        if (value.charAt(j) == '}') {
                            braceCount--;
                        }
                        if (braceCount < 0) {
                            expansion.append(getProperty(expand(envVar.toString()), ""));
                            i = j;
                            break;
                        } else envVar.append(value.charAt(j));
                    }
                    continue;
                }
            }
            expansion.append(value.charAt(i));
        }
        return (expansion.toString());
    }

    public static String expandValue(String value) {
        return (getSystemParameters().expand(value));
    }

    public static Parameters getSystemParameters() {
        if (systemParameters == null) {
            systemParameters = convertToParameters(System.getProperties());
            systemParameters.addToSystemProperties = true;
            System.setProperties(systemParameters);
            File userDir = systemParameters.getDirectory("user.dir");
            systemParameters.setHomeDirectory(userDir);
            try {
                systemParameters.load(systemParameters.getFile("config.file"));
            } catch (IOException ioe) {
            }
        }
        return (systemParameters);
    }

    public void load(File f) throws IOException {
        if (f != null) loadParams(new FileInputStream(f));
    }

    public void load(URL url) throws IOException {
        if (url != null) loadParams(url.openStream());
    }

    public synchronized void loadParams(InputStream inStream) throws IOException {
        Parameters p = new Parameters();
        p.loadParameters(inStream);
        Parameters sysProps;
        if (addToSystemProperties) sysProps = (Parameters) System.getProperties(); else sysProps = this;
        Parameters tmpProps = sysProps;
        while (tmpProps.defaults != null) tmpProps = (Parameters) tmpProps.defaults;
        tmpProps.defaults = p;
    }

    private synchronized void loadParameters(InputStream inStream) throws IOException {
        load(inStream);
    }

    private static final String keyValueSeparators = "=: \t\r\n\f";

    private static final String strictKeyValueSeparators = "=:";

    private static final String whiteSpaceChars = " \t\r\n\f";

    public synchronized void load(InputStream inStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "8859_1"));
        while (true) {
            String line = in.readLine();
            if (line == null) return;
            if (line.length() > 0) {
                char firstChar = line.charAt(0);
                if ((firstChar != '#') && (firstChar != '!')) {
                    while (continueLine(line)) {
                        String nextLine = in.readLine();
                        if (nextLine == null) nextLine = new String("");
                        String loppedLine = line.substring(0, line.length() - 1);
                        int startIndex = 0;
                        for (startIndex = 0; startIndex < nextLine.length(); startIndex++) if (whiteSpaceChars.indexOf(nextLine.charAt(startIndex)) == -1) break;
                        nextLine = nextLine.substring(startIndex, nextLine.length());
                        line = new String(loppedLine + nextLine);
                    }
                    int len = line.length();
                    int keyStart;
                    for (keyStart = 0; keyStart < len; keyStart++) {
                        if (whiteSpaceChars.indexOf(line.charAt(keyStart)) == -1) break;
                    }
                    int separatorIndex;
                    for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++) {
                        char currentChar = line.charAt(separatorIndex);
                        if (currentChar == '\\') separatorIndex++; else if (keyValueSeparators.indexOf(currentChar) != -1) break;
                    }
                    int valueIndex;
                    for (valueIndex = separatorIndex + 1; valueIndex < len; valueIndex++) if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;
                    if (valueIndex < len) if (strictKeyValueSeparators.indexOf(line.charAt(valueIndex)) != -1) valueIndex++;
                    while (valueIndex < len) {
                        if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;
                        valueIndex++;
                    }
                    String key = line.substring(keyStart, separatorIndex);
                    String value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";
                    key = loadConvert(key);
                    put(key, value);
                }
            }
        }
    }

    private boolean continueLine(String line) {
        int slashCount = 0;
        int index = line.length() - 1;
        while ((index >= 0) && (line.charAt(index--) == '\\')) slashCount++;
        return (slashCount % 2 == 1);
    }

    private static String loadConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch(aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't') aChar = '\t'; else if (aChar == 'r') aChar = '\r'; else if (aChar == 'n') aChar = '\n'; else if (aChar == 'f') aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }

    public String verifyPath(String inString) {
        char aChar;
        StringBuffer outBuffer = new StringBuffer();
        if (inString != null && (inString.length() > 1)) {
            if (inString.indexOf("\\") != -1) {
                for (int x = 0; x < inString.length(); ) {
                    aChar = inString.charAt(x++);
                    if (aChar == '\\') {
                        outBuffer.append(aChar);
                        aChar = inString.charAt(x++);
                        if (aChar == '\\') {
                            outBuffer.append(aChar);
                        } else {
                            outBuffer.append('\\');
                            outBuffer.append(aChar);
                        }
                    } else {
                        outBuffer.append(aChar);
                    }
                }
                return (outBuffer.toString());
            } else return inString;
        }
        return inString;
    }

    /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property list,
   * and its defaults, recursively, are then checked. The method returns
   * null if the property is not found.
   *
   * @param   key   the property key.
   * @return  the value in this property list with the specified key value.
   * @see     java.util.Properties#defaults
  */
    public String getProperty(String key) {
        Object oval = super.get(key);
        String sval = (oval instanceof String) ? (String) oval : null;
        if ((sval != null) && !this.containsKey(key)) sval = loadConvert(sval);
        return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
    }

    /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property list,
   * and its defaults, recursively, are then checked. The method returns the
   * default value argument if the property is not found.
   *
   * @param   key            the hashtable key.
   * @param   defaultValue   a default value.
   *
   * @return  the value in this property list with the specified key value.
   * @see     java.util.Properties#defaults
  */
    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        if (val != null) val = loadConvert(val);
        return (val == null) ? defaultValue : val;
    }

    public Integer getInteger(String paramName) {
        return (getInteger(paramName, null));
    }

    public Integer getInteger(String paramName, Integer defaultValue) {
        String value;
        if ((value = getProperty(paramName)) != null) {
            try {
                defaultValue = new Integer(value);
            } catch (Exception e) {
            }
        }
        return (defaultValue);
    }

    public Long getLong(String paramName) {
        return (getLong(paramName, null));
    }

    public Long getLong(String paramName, Long defaultValue) {
        String value;
        if ((value = getProperty(paramName)) != null) {
            try {
                defaultValue = new Long(value);
            } catch (Exception e) {
            }
        }
        return (defaultValue);
    }

    public Double getDouble(String paramName) {
        return (getDouble(paramName, null));
    }

    public Double getDouble(String paramName, Double defaultValue) {
        String value;
        if ((value = getProperty(paramName)) != null) {
            try {
                defaultValue = new Double(value);
            } catch (Exception e) {
            }
        }
        return (defaultValue);
    }

    public boolean getBoolean(String paramName) {
        return (getBoolean(paramName, Boolean.FALSE));
    }

    public boolean getBoolean(String paramName, Boolean defaultValue) {
        String value;
        if ((value = getProperty(paramName)) != null) {
            try {
                defaultValue = new Boolean(value);
            } catch (Exception e) {
            }
        }
        return (defaultValue.booleanValue());
    }

    public File getFile(String paramName) {
        String property = getProperty(paramName);
        property = verifyPath(property);
        return (locateFile(property));
    }

    public File getFile(String paramName, String defaultName) {
        File f = null;
        if ((f = getFile(paramName)) == null) return (locateFile(defaultName));
        return (f);
    }

    private File locateFile(String fileName) {
        if (fileName != null) {
            File f = new File(fileName);
            if (f.isFile()) return (f);
            if (homeDirectory != null) {
                f = new File(homeDirectory, fileName);
                if (f.exists()) return (f);
            }
        }
        return (null);
    }

    private File locateDirectory(String dirName) {
        if (dirName != null) {
            File f = new File(dirName);
            if (f.isDirectory()) return (f);
            if (homeDirectory != null) {
                f = new File(homeDirectory, dirName);
                if (f.isDirectory()) return (f);
            }
        }
        return (null);
    }

    public File getDirectory(String paramName) {
        String property = getProperty(paramName);
        property = verifyPath(property);
        return (locateDirectory(property));
    }

    public File getDirectory(String paramName, String defaultName) {
        File f = null;
        if ((f = getDirectory(paramName)) == null) return (locateDirectory(defaultName));
        return (f);
    }

    public static Parameters getSubParameters(String prefix, Properties origProps) {
        if (origProps == null) {
            return (null);
        }
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        Parameters componentParameters = new Parameters(false);
        for (Enumeration e = origProps.propertyNames(); e.hasMoreElements(); ) {
            String property = (String) e.nextElement();
            if (property.startsWith(prefix)) {
                componentParameters.put(property.substring(prefix.length()), origProps.getProperty(property));
            }
        }
        return (componentParameters);
    }

    public static Parameters convertToParameters(Properties p) {
        Field defaultsField = null;
        Stack defaults = new Stack(), defaults2 = new Stack();
        Parameters params = null;
        try {
            defaultsField = Properties.class.getDeclaredField("defaults");
            defaultsField.setAccessible(true);
            defaults.push(p);
            Properties defaultProps = (Properties) defaultsField.get(p);
            while (defaultProps != null) {
                defaults.push(defaultProps);
                defaultProps = (Properties) defaultsField.get(p);
            }
            while (!defaults.empty()) {
                Parameters defaultParams = new Parameters(false);
                p = (Properties) defaults.pop();
                for (Iterator i = p.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) i.next();
                    if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
                        String val = (String) entry.getValue();
                        val = loadConvert(val);
                        defaultParams.setProperty((String) entry.getKey(), val);
                    }
                }
                defaults2.push(defaultParams);
            }
            params = (Parameters) defaults2.pop();
            Parameters parent = params;
            while (!defaults2.empty()) {
                parent.defaults = (Properties) defaults2.pop();
                parent = (Parameters) parent.defaults;
            }
        } catch (Throwable t) {
            params = new Parameters(false);
            for (Iterator i = Collections.list(p.propertyNames()).iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                params.put(key, p.getProperty(key));
            }
        }
        return (params);
    }
}
