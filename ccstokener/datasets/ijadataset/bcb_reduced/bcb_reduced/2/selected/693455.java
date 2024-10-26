package phex.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import phex.common.Phex;

/**
 * 
 */
public class GWebCacheListBuilder {

    private static final String listUrl = "http://www.rodage.net/gnetcache/gcache.php?urlfile=1000";

    private static List<String> dataList;

    public static void main(String[] args) throws Exception {
        dataList = new ArrayList<String>();
        System.setProperty("http.agent", Phex.getFullPhexVendor());
        URL url = new URL(listUrl);
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();
        readData(inputStream);
        System.out.println("Total data read: " + dataList.size());
        inputStream.close();
        writeToOutputFile();
    }

    private static void readData(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        if (line != null && line.startsWith("ERROR")) {
            System.err.println(line);
            return;
        }
        while (line != null) {
            try {
                URL url = new URL(line);
                if (!url.getProtocol().equals("http")) {
                    System.err.println("Skipped " + line);
                    continue;
                }
                dataList.add(line);
            } catch (MalformedURLException exp) {
                System.err.println("Skipped " + line);
            }
            line = reader.readLine();
        }
    }

    private static void writeToOutputFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/phex/resources/gwebcache.cfg"));
        Iterator iterator = dataList.iterator();
        while (iterator.hasNext()) {
            String line = (String) iterator.next();
            writer.write(line + "\n");
        }
        writer.close();
    }
}
