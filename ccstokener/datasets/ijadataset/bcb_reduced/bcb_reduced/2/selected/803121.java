package cat.quadriga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.logging.Logger;

public abstract class Utils {

    private static Logger logger = Logger.getLogger(Utils.class.getCanonicalName());

    public static File findFile(String name) {
        try {
            URL url = Utils.class.getResource(name);
            return new File(url.toURI());
        } catch (Exception e) {
            return new File(name);
        }
    }

    public static InputStream findInputStream(String name) throws FileNotFoundException {
        try {
            URL url = Utils.class.getResource(name);
            return url.openStream();
        } catch (Exception e) {
            return new FileInputStream(name);
        }
    }

    public static String readFile(File f) {
        try {
            return readInputStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            logger.warning(e.toString());
            return null;
        }
    }

    public static String readInputStream(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String aux = reader.readLine();
            while (aux != null) {
                sb.append(aux);
                sb.append('\n');
                aux = reader.readLine();
            }
            reader.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            logger.warning(e.toString());
            return null;
        } catch (UnsupportedEncodingException e) {
            logger.warning(e.toString());
            return null;
        } catch (IOException e) {
            logger.warning(e.toString());
            return null;
        }
    }
}
