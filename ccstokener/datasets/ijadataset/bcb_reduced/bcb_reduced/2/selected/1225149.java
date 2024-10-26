package tripleo.nio.javanio;

import java.io.*;
import java.net.URL;

/**
 * A simple example to illustrate using a URL to access a resource
 * and then store the result to a File.
 * <P>
 * Any type of URL can be used:  http, https, ftp, etc.
 *
 * @author Brad R. Wetmore
 * @author Mark Reinhold
 * @version 1.2, 04/07/26
 */
public class URLDumper {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage:  URLDumper <URL> <file>");
            System.exit(1);
        }
        String location = args[0];
        String file = args[1];
        URL url = new URL(location);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes = new byte[4096];
        InputStream is = url.openStream();
        int read;
        while ((read = is.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }
        is.close();
        fos.close();
    }
}
