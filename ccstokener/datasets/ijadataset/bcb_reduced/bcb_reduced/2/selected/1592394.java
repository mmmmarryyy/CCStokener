package jalview.io;

import java.util.*;
import java.io.*;
import java.net.*;

public class FileParse {

    public File inFile;

    public int fileSize;

    public byte[] dataArray;

    public Vector lineArray;

    public int noLines;

    int bytes_read = 0;

    String inType;

    URL url;

    URLConnection urlconn;

    public FileParse() {
    }

    public FileParse(String fileStr, String type) throws MalformedURLException, IOException {
        this.inType = type;
        System.out.println("Input type = " + type);
        System.out.println("Input name = " + fileStr);
        if (type.equals("File")) {
            this.inFile = new File(fileStr);
            this.fileSize = (int) inFile.length();
            System.out.println("File: " + inFile);
            System.out.println("Bytes: " + fileSize);
        } else if (type.equals("URL")) {
            url = new URL(fileStr);
            this.fileSize = 0;
            urlconn = url.openConnection();
        } else {
            System.out.println("Unknwon FileParse inType " + inType);
        }
    }

    public void readLines(String inStr) {
        StringTokenizer str = new StringTokenizer(inStr, "\n");
        lineArray = new Vector();
        while (str.hasMoreTokens()) {
            lineArray.addElement(str.nextToken());
        }
        noLines = lineArray.size();
    }

    public void readLines() throws IOException {
        String line;
        this.lineArray = new Vector();
        DataInputStream dataIn;
        if (inType.equals("File")) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inFile));
            dataIn = new DataInputStream(bis);
        } else {
            dataIn = new DataInputStream(urlconn.getInputStream());
        }
        while ((line = dataIn.readLine()) != null) {
            lineArray.addElement(line);
        }
        noLines = lineArray.size();
    }

    public Vector splitLine(char splitChar, int element) {
        Vector letters = new Vector();
        Vector wordVector = new Vector();
        String line = (String) lineArray.elementAt(element);
        char[] charArray = line.toCharArray();
        int i = 0;
        int letter = 0;
        char[] word = new char[line.length()];
        char prev_char = '\n';
        for (i = 0; i < line.length(); i++) {
            if (charArray[i] != splitChar) {
                word[letter] = charArray[i];
                prev_char = charArray[i];
                letter++;
            } else {
                if ((prev_char != splitChar) && (prev_char != '\n')) {
                    wordVector.addElement(new String(word));
                    letter = 0;
                    word = null;
                    word = new char[line.length()];
                    prev_char = charArray[i];
                }
            }
        }
        if (line.length() != 0) {
            if (charArray[line.length() - 1] != splitChar) {
                wordVector.addElement(new String(word));
            }
        } else {
            return (null);
        }
        return (wordVector);
    }
}
