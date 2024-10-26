package bebopicker.io;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mark
 */
public class PicWriter {

    public PicWriter() {
    }

    public boolean get(String photoURL, String path) {
        boolean success = true;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(photoURL);
            in = url.openStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);
            out = new FileOutputStream(path);
            for (; ; ) {
                int data = bufIn.read();
                if (data == -1) {
                    break;
                } else {
                    out.write(data);
                }
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(PicWriter.class.getName()).log(Level.SEVERE, null, ex);
            success = false;
        } catch (IOException ex) {
            Logger.getLogger(PicWriter.class.getName()).log(Level.SEVERE, null, ex);
            success = false;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(PicWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(PicWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return success;
    }
}
