package de.gee.erep.server.util.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

/**
 * @author Matthew Gee created: 09.05.2011
 */
public class Download {

    public static File download(URL url, String fName) {
        String localFile = "";
        try {
            System.out.println("Opening connection to " + url.toString() + "...");
            URLConnection urlC = url.openConnection();
            InputStream is = url.openStream();
            System.out.flush();
            FileOutputStream fos = null;
            StringTokenizer st = new StringTokenizer(url.getFile(), "/");
            while (st.hasMoreTokens()) {
                localFile = st.nextToken();
            }
            fos = new FileOutputStream("war/Ressources/temp/citizens/" + fName + "_" + localFile);
            int oneChar, count = 0;
            while ((oneChar = is.read()) != -1) {
                fos.write(oneChar);
                count++;
            }
            is.close();
            fos.close();
            System.out.println(count + " byte(s) copied");
        } catch (MalformedURLException e) {
            System.err.println(e.toString());
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return new File("war/Ressources/temp/citizens/" + localFile);
    }
}
