package net.cryff.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The <code>FileUtil</code> class provides some methods to 
 * work with files, mainly to load or save them in some formats
 * 
 * @author Nino Wagensonner
 * @version 1.0 12/05/2007
 * @since CFF V.0.1r-2
 */
public class FileUtil {

    /**
	 * unzipFile loads and unzips a GZIP packed file
	 * @param input localpath to textfile
	 * @param exc excluding String for lines that can be ignored, like # for comments
	 * @return LinkedList<String> a LinkedList that concludes all lines of the given textfile
	 * @throws IOException if file is not accessable
	 */
    public static LinkedList<String> unzipFile(String input, String exc) throws IOException {
        LinkedList<String> daten = new LinkedList<String>();
        GZIPInputStream as = new GZIPInputStream(new FileInputStream(input));
        BufferedReader br = new BufferedReader(new InputStreamReader(as));
        String temp = "";
        while (true) {
            temp = br.readLine();
            if (temp == null) break;
            if (!temp.startsWith(exc)) {
                daten.add(temp);
            }
        }
        br.close();
        as.close();
        return daten;
    }

    /**
	 * zipFile saves a LinkedList<String> file in the GZIP format to a localpath
	 * @param data	needs a LinkedList<String> with lines to save
	 * @param output	specifies the path to save the file
	 * @throws IOException if file is not accessable
	 */
    public static void zipFile(LinkedList<String> data, String output) throws IOException {
        String enter = System.getProperty("line.separator");
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(output));
        for (int i = 0; i < data.size(); i++) {
            out.write(data.get(i).getBytes());
            out.write(enter.getBytes());
        }
        out.close();
    }

    /**
	 * read loads a local file input a LinkedList<String> 
	 * @param input	localpath to textfile
	 * @return LinkedList<String> a LinkedList that concludes all lines of the given textfile
	 * @throws IOException if file is not accessable
	 */
    public static LinkedList<String> read(String input) throws IOException {
        LinkedList<String> data = new LinkedList<String>();
        File f = new File(input);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = "";
        while (true) {
            line = br.readLine();
            if (line == null) break;
            data.add(line);
        }
        br.close();
        return data;
    }

    /**
	 * read loads a local file input a LinkedList<String> 
	 * @param input	localpath to textfile
	 * @param exc	excluding String for lines that can be ignored, like # for comments
	 * @return LinkedList<String> a LinkedList that concludes all lines of the given textfile
	 * @throws IOException if file is not accessable
	 */
    public static LinkedList<String> read(String input, String exc) throws IOException {
        LinkedList<String> data = new LinkedList<String>();
        File f = new File(input);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = "";
        while (true) {
            line = br.readLine();
            if (line == null) break;
            if (!line.startsWith(exc)) {
                data.add(line);
            }
        }
        br.close();
        return data;
    }

    /**
	 * read loads a txtfile from the internet and saves it into a LinkedList<String>
	 * @param url	the path to a textfile on the internet
	 * @return LinkedList<String> a LinkedList that concludes all lines of the given textfile
	 * @throws IOException if file is either not found or not accessable 
	 */
    public static LinkedList<String> read(URL url) throws IOException {
        LinkedList<String> data = new LinkedList<String>();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String input = "";
        while (true) {
            input = br.readLine();
            if (input == null) break;
            data.add(input);
        }
        br.close();
        return data;
    }

    /**
	 * read loads a txtfile from the internet and saves it into a LinkedList<String>
	 * @param url	the path to a textfile on the internet
	 * @param exc	excluding String for lines that can be ignored, like # for comments
	 * @return LinkedList<String> a LinkedList that concludes all lines of the given textfile
	 * @throws IOException if file is either not found or not accessable 
	 */
    public static LinkedList<String> read(URL url, String exc) throws IOException {
        LinkedList<String> data = new LinkedList<String>();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String input = "";
        while (true) {
            input = br.readLine();
            if (input == null) break;
            if (!input.startsWith(exc)) {
                data.add(input);
            }
        }
        br.close();
        return data;
    }

    /**
	 * write saves a LinkedList<String> into a textfile
	 * @param data	needs a LinkedList<String> with lines to write
	 * @param path	save to the path
	 * @throws IOException if file is not accessable 
	 */
    public static void write(LinkedList<String> data, String path) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
        for (int i = 0; i < data.size(); i++) {
            String tmp = data.get(i);
            if (tmp != null) {
                bw.write(tmp);
                bw.write(System.getProperty("line.separator"));
            }
        }
        bw.close();
    }
}
