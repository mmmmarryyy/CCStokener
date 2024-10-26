package net.sf.vat4net.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Vector;

/**
 * Implements a simple reader for trace files.
 * Will be removed in the future.
 * 
 * @author $Author: jana78 $
 * @version $Revision: 1.7 $
 */
public class TraceFileReader {

    private boolean status = true;

    private Vector lines = new Vector();

    private int recCount = 0;

    private String record = null;

    private BufferedReader br;

    public TraceFileReader() {
    }

    public TraceFileReader(URL url) {
        try {
            readFile(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            System.out.println("got a readLine Error");
            e.printStackTrace();
            this.status = false;
        }
    }

    public TraceFileReader(String filename) {
        try {
            readFile(new FileReader(filename));
        } catch (IOException e) {
            System.out.println("got a readLine Error");
            e.printStackTrace();
            this.status = false;
        }
    }

    public void readFile(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        record = new String();
        while ((record = br.readLine()) != null) {
            this.recCount++;
            lines.add(record);
        }
    }

    public void initReader(String filename) {
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (IOException e) {
            System.out.println("got a readLine Error");
            e.printStackTrace();
            this.status = false;
        }
    }

    public void initReader(URL url) {
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            System.out.println("got a readLine Error");
            e.printStackTrace();
            this.status = false;
        }
    }

    public String readLine() throws IOException {
        String line = br.readLine();
        if (line == null) {
            System.out.println("null line in trace file reader.");
            return "EOF";
        } else {
            return line;
        }
    }

    /** Status Information: whole file reading successful or not
    *
    **/
    public boolean getStatus() {
        return status;
    }

    /** Number of Lines read by the TraceFileReader
    *
    **/
    public int getNumOfLines() {
        return recCount;
    }

    /** Return a specificated line
    *   @return String of line i read by the TraceFileReader
    **/
    public String getLineAt(int i) {
        return (String) this.lines.elementAt(i);
    }
}
