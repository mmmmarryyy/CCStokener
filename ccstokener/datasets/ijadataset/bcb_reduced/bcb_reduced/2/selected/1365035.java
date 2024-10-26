package com.nullfish.lib.vfs.impl.http.manipulation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import com.nullfish.lib.vfs.FileAttribute;
import com.nullfish.lib.vfs.FileType;
import com.nullfish.lib.vfs.VFile;
import com.nullfish.lib.vfs.exception.VFSException;
import com.nullfish.lib.vfs.exception.VFSIOException;
import com.nullfish.lib.vfs.exception.WrongPathException;
import com.nullfish.lib.vfs.impl.DefaultFileAttribute;
import com.nullfish.lib.vfs.manipulation.abst.AbstractGetAttributesManipulation;

/**
 * @author shunji
 * 
 */
public class HTTPGetAttributesManipulation extends AbstractGetAttributesManipulation {

    private HttpURLConnection con;

    public HTTPGetAttributesManipulation(VFile file) {
        super(file);
    }

    public FileAttribute doGetAttribute(VFile file) throws VFSException {
        try {
            URL url = file.getURI().toURL();
            return getAttribute(url);
        } catch (MalformedURLException e) {
            throw new WrongPathException(file.getAbsolutePath());
        } catch (URISyntaxException e) {
            throw new WrongPathException(file.getAbsolutePath());
        }
    }

    public FileAttribute getAttribute(URL url) throws VFSException {
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);
            int response = con.getResponseCode();
            if (response >= 400) {
                return new DefaultFileAttribute(false, 0, null, FileType.NOT_EXISTS);
            }
            boolean redirect = (response >= 300 && response <= 399);
            if (redirect) {
                String location = con.getHeaderField("Location");
                return getAttribute(new URL(url, location));
            }
            return new DefaultFileAttribute(true, con.getContentLength(), new Date(con.getLastModified()), url.toString().endsWith("/") ? FileType.DIRECTORY : FileType.FILE);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new WrongPathException(file.getAbsolutePath());
        } catch (IOException e) {
            throw new VFSIOException("IOException opening " + file.getAbsolutePath(), e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public void doStop() {
        getWorkThread().interrupt();
        if (con != null) {
            con.disconnect();
        }
    }
}
