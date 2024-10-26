package genj.util;

import java.io.*;
import java.net.*;
import java.util.zip.*;

/**
 * Class which stands for an origin of a resource - this Origin
 * is pointing to a ZIP file so all relative files are read
 * of that file, too
 */
public class ZipOrigin extends Origin {

    /** cached bytes */
    private byte[] cachedBits;

    /**
   * Constructor
   */
    protected ZipOrigin(URL url) {
        super(url);
    }

    /**
   * Open connection to this origin
   */
    public Connection open() throws IOException {
        String anchor = url.getRef();
        if ((anchor == null) || (anchor.length() == 0)) {
            throw new IOException("ZipOrigin needs anchor for open()");
        }
        return openRelativeFile(anchor);
    }

    /**
   * Returns a file that is relative to this Origin
   */
    protected Connection openRelativeFile(String file) throws IOException {
        if (cachedBits == null) {
            cachedBits = new ByteArray(url.openConnection().getInputStream()).getBytes();
        }
        ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(cachedBits));
        ZipEntry zentry;
        while (true) {
            zentry = zin.getNextEntry();
            if (zentry == null) {
                throw new IOException("Couldn't find resource " + file + " in ZIP-file");
            }
            if (zentry.getName().equals(file)) {
                return new Connection(zin, zentry.getSize());
            }
        }
    }

    /**
   * Whether it is possible to save to the Origin
   */
    public boolean isFile() {
        return false;
    }

    /**
   * Is not supported by ZipOrigin
   */
    public File getFile() {
        throw new IllegalArgumentException("ZipOrigin doesn support getFile()");
    }

    /**
   * Returns the Origin's Filename file://d:/gedcom/example.zip#[example.ged]
   */
    public String getFileName() {
        return url.getRef();
    }

    /**
   * The name of this origin file://d:/gedcom/[example.zip#example.ged]
   */
    public String getName() {
        return super.getName();
    }
}
