package ru.ksu.niimm.cll.mocassin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class IOUtil {

    private IOUtil() {
    }

    /**
	 * read a set of string lines from input stream and close it
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
    public static Set<String> readLineSet(InputStream stream) throws IOException {
        Set<String> values = new HashSet<String>();
        try {
            LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(stream));
            String line = null;
            while ((line = lineReader.readLine()) != null) {
                values.add(line);
            }
            return values;
        } finally {
            stream.close();
        }
    }

    /**
	 * read an ordered list of string lines from input stream and close it
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
    public static LinkedList<String> readLineList(InputStream stream) throws IOException {
        LinkedList<String> values = new LinkedList<String>();
        try {
            LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(stream));
            String line = null;
            while ((line = lineReader.readLine()) != null) {
                values.add(line);
            }
            return values;
        } finally {
            stream.close();
        }
    }

    public static String readContents(URL url) throws IOException {
        InputStream inputStream = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer contentsBuffer = new StringBuffer();
        try {
            String value;
            while ((value = reader.readLine()) != null) {
                contentsBuffer.append(value);
                contentsBuffer.append("\n");
            }
        } finally {
            reader.close();
            inputStream.close();
        }
        return contentsBuffer.toString();
    }
}
