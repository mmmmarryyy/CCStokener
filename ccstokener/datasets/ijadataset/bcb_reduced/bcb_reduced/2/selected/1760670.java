package util.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class ULRExtractor {

    /**
	 * @param args
	 */
    static String directory = "/home/sergio/projects/data/benchmark/websites/";

    public static void saveUrlToFile(File saveFile, String location) {
        URL url;
        try {
            url = new URL(location);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            BufferedWriter out = new BufferedWriter(new FileWriter(saveFile));
            char[] cbuf = new char[255];
            while ((in.read(cbuf)) != -1) {
                out.write(cbuf);
            }
            in.close();
            out.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String url = "http://www.nickjr.com/";
        String filename = "prueba.html";
        File file = new File(directory);
        saveUrlToFile(file, url);
    }
}
