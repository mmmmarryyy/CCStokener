package edu.upmc.opi.caBIG.common;

import java.io.*;
import java.net.URL;
import org.apache.log4j.Logger;

public class CaBIG_ReadWriteTextFile {

    private static Logger logger = Logger.getLogger(CaBIG_ReadWriteTextFile.class);

    /**
     * Fetch the entire contents of a text file, and return it in a String. This
     * style of implementation does not throw Exceptions to the caller.
     * 
     * @param file is a file which already exists and can be read.
     * 
     * @return the contents
     */
    public static String getContents(File file) {
        StringBuffer contents = new StringBuffer();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return contents.toString();
    }

    /**
     * Change the contents of text file in its entirety, overwriting any
     * existing text.
     * 
     * This style of implementation throws all exceptions to the caller.
     * 
     * @param contents the a contents
     * @param file is an existing file which can be written to.
     * 
     * @throws FileNotFoundException if the file does not exist.
     * @throws IOException if problem encountered during write.
     * @throws IllegalArgumentException if param does not comply.
     */
    public static void setContents(File file, String contents) throws FileNotFoundException, IOException {
        if (file == null) {
            throw new IllegalArgumentException("File should not be null.");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist: " + file);
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Should not be a directory: " + file);
        }
        if (!file.canWrite()) {
            throw new IllegalArgumentException("File cannot be written: " + file);
        }
        Writer output = null;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(contents);
        } finally {
            if (output != null) output.close();
        }
    }

    public static String readFileFromURL(URL url) {
        StringBuffer contents = new StringBuffer();
        try {
            InputStream in = url.openStream();
            BufferedReader dis = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = dis.readLine()) != null) {
                contents.append(line + "\n");
            }
            in.close();
        } catch (IOException e) {
            ;
        }
        return contents.toString();
    }

    public static String deriveFileFromClassPath(Class cls, String fileName) {
        String fileAsString = null;
        try {
            InputStream resourceInputStream = cls.getResourceAsStream("/" + fileName);
            byte[] data = new byte[resourceInputStream.available()];
            resourceInputStream.read(data, 0, resourceInputStream.available());
            fileAsString = new String(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileAsString;
    }

    public static void main(String[] aArguments) throws IOException {
        File testFile = new File("C:\\CaBIG_ReadWriteTest.txt");
        setContents(testFile, "blah blah blah");
        logger.debug("File contents: " + getContents(testFile));
        testFile.delete();
    }
}
