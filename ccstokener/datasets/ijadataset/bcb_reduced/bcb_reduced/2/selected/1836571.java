package org.hfbk.util;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

public class FileUtils {

    /**
	 * create a new File ready to write. a counter is increased until
	 * the given pathname, counter and suffix give a non existiend file's
	 * name.
	 *  
	 * @param path the wanted filename
	 * @return an empty File ready to write
	 * @throws IOException if path is not writable (no rights to, bad filename)
	 */
    public static File getNextFreeFile(String path) throws IOException {
        String suffix, prefix;
        int point = path.lastIndexOf(".");
        if (point > 0) {
            prefix = path.substring(0, point);
            suffix = path.substring(point);
        } else {
            prefix = path;
            suffix = "";
        }
        int i = 0;
        File f;
        do {
            f = new File(prefix + i + suffix);
            i++;
        } while (!f.createNewFile());
        return f;
    }

    /**
	 * saves the given url to the users home directory. tries to preserve remote filename,
	 * adds a counter if existing. 
	 * 
	 * @param url to copy 
	 */
    public static void saveLocal(String url) {
        try {
            String sourceName = new URL(url).getPath();
            int slash = sourceName.lastIndexOf("/");
            if (slash > 0) sourceName = sourceName.substring(slash + 1);
            InputStream is = new URL(url).openStream();
            OutputStream os = new FileOutputStream(getNextFreeFile(System.getProperty("user.home") + "/Vis_" + sourceName));
            byte[] buffer = new byte[0x4000];
            int bytesRead;
            while (true) {
                bytesRead = is.read(buffer);
                if (bytesRead <= 0) break;
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            System.out.println(url + " saved.");
        } catch (Exception e) {
            System.out.println("VisHUD.saveLocal:" + e);
        }
    }

    /** open a ressource and get InputStream */
    public static InputStream getStream(String ressource) {
        try {
            if (!ressource.matches("http://.*|file:.*")) ressource = "file:" + ressource;
            URL url = new URL(ressource);
            return url.openStream();
        } catch (Exception e) {
            throw (new RuntimeException(e));
        }
    }

    /** read in a text file. */
    public static String read(String path) {
        try {
            BufferedReader ir = new BufferedReader(new InputStreamReader(getStream(path)));
            StringBuffer text = new StringBuffer();
            String line;
            while ((line = ir.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            return "";
        }
    }

    /**
	 * try to open url in users default browser. needs jre1.6 but fails
	 * harmless if not. 
	 * 
	 * @param url to browse
	 */
    public static void browse(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable e) {
            System.out.println("Cannot browse:" + e);
        }
    }
}
