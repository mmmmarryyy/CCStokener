package src.utilities;

import java.io.*;
import java.net.URL;

/**
 *
 * Reading utilities
 *
 * @author Simon Eugster
 * ,		hb9eia
 */
public class IORead {

    /**
	 * Reads a File into a StringBuffer.
	 * @param f - Input File
	 * @return File in StringBuffer
	 */
    public static StringBuffer readSBuffer(File f) throws IOException {
        return new StringBuffer(readSBuilder(f));
    }

    public static StringBuilder readSBuilder(File f) throws IOException {
        FileInputStream fis = null;
        StringBuilder sb = new StringBuilder();
        try {
            fis = new FileInputStream(f);
            byte[] buffer = new byte[8 * 21000];
            int bytes = 0;
            while ((bytes = fis.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytes));
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
                fis = null;
            }
        }
        return sb;
    }

    public static StringBuffer readSBuffer(URL url) throws IOException {
        InputStream fis = null;
        StringBuffer sb = new StringBuffer();
        try {
            fis = url.openStream();
            byte[] buffer = new byte[8 * 21000];
            int bytes = 0;
            while ((bytes = fis.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytes));
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
                fis = null;
            }
        }
        return sb;
    }

    /**
	 * Reads a (text) file from the current jar file.
	 * @param filename - The path to the file
	 * @return The file as a StringBuffer
	 */
    public static StringBuffer readFromJar(String filename) {
        IORead ir = new IORead();
        InputStream is = ir.getClass().getResourceAsStream(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line = new String();
        try {
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    sb.append('\n');
                }
                sb.append(line + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

    /**
	 * Reads a line with readLine() from the command line.
	 */
    public static BufferedReader readCmdLine = new BufferedReader(new InputStreamReader(System.in));
}
