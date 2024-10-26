package com.ibm.wsdl.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Deals with strings (probably need to elaborate some more).
 *
 * @author   Matthew J. Duftler
 */
public class StringUtils {

    public static final String lineSeparator = System.getProperty("line.separator", "\n");

    public static final String lineSeparatorStr = cleanString(lineSeparator);

    public static String cleanString(String str) {
        if (str == null) return null; else {
            char[] charArray = str.toCharArray();
            StringBuffer sBuf = new StringBuffer();
            for (int i = 0; i < charArray.length; i++) switch(charArray[i]) {
                case '\"':
                    sBuf.append("\\\"");
                    break;
                case '\\':
                    sBuf.append("\\\\");
                    break;
                case '\n':
                    sBuf.append("\\n");
                    break;
                case '\r':
                    sBuf.append("\\r");
                    break;
                default:
                    sBuf.append(charArray[i]);
                    break;
            }
            return sBuf.toString();
        }
    }

    public static String getClassName(Class targetClass) {
        String className = targetClass.getName();
        return targetClass.isArray() ? parseDescriptor(className) : className;
    }

    private static String parseDescriptor(String className) {
        char[] classNameChars = className.toCharArray();
        int arrayDim = 0;
        int i = 0;
        while (classNameChars[i] == '[') {
            arrayDim++;
            i++;
        }
        StringBuffer classNameBuf = new StringBuffer();
        switch(classNameChars[i++]) {
            case 'B':
                classNameBuf.append("byte");
                break;
            case 'C':
                classNameBuf.append("char");
                break;
            case 'D':
                classNameBuf.append("double");
                break;
            case 'F':
                classNameBuf.append("float");
                break;
            case 'I':
                classNameBuf.append("int");
                break;
            case 'J':
                classNameBuf.append("long");
                break;
            case 'S':
                classNameBuf.append("short");
                break;
            case 'Z':
                classNameBuf.append("boolean");
                break;
            case 'L':
                classNameBuf.append(classNameChars, i, classNameChars.length - i - 1);
                break;
        }
        for (i = 0; i < arrayDim; i++) classNameBuf.append("[]");
        return classNameBuf.toString();
    }

    public static URL getURL(URL contextURL, String spec) throws MalformedURLException {
        try {
            return new URL(contextURL, spec);
        } catch (MalformedURLException e) {
            File tempFile = new File(spec);
            if (contextURL == null || (contextURL != null && tempFile.isAbsolute())) {
                return tempFile.toURL();
            }
            throw e;
        }
    }

    public static InputStream getContentAsInputStream(URL url) throws SecurityException, IllegalArgumentException, IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null.");
        }
        try {
            InputStream content = url.openStream();
            if (content == null) {
                throw new IllegalArgumentException("No content.");
            }
            return content;
        } catch (SecurityException e) {
            throw new SecurityException("Your JVM's SecurityManager has " + "disallowed this.");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("This file was not found: " + url);
        }
    }

    public static List parseNMTokens(String nmTokens) {
        StringTokenizer strTok = new StringTokenizer(nmTokens, " ");
        List tokens = new Vector();
        while (strTok.hasMoreTokens()) {
            tokens.add(strTok.nextToken());
        }
        return tokens;
    }

    public static String getNMTokens(List list) {
        if (list != null) {
            StringBuffer strBuf = new StringBuffer();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                String token = (String) list.get(i);
                strBuf.append((i > 0 ? " " : "") + token);
            }
            return strBuf.toString();
        } else {
            return null;
        }
    }
}
