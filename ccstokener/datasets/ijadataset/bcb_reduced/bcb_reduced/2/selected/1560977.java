package org.jdeluxe.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The Class EDIDataInterfaceUBL2.
 */
public class EDIDataInterfaceUBL2 implements EDIDataInterface {

    /**
	 * Instantiates a new EDI data interface UB l2.
	 */
    public EDIDataInterfaceUBL2() {
    }

    /**
	 * Copy input stream.
	 *
	 * @param in the in
	 * @param out the out
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    /**
	 * Gets the data.
	 *
	 * @param in the in
	 *
	 * @return the data
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void getData(InputStream in) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        ZipEntry entry = null;
        while ((entry = zip.getNextEntry()) != null) {
            System.out.println("zip = " + entry.getName());
            System.out.println(entry.getExtra());
            if (entry.getMethod() == ZipEntry.DEFLATED) System.out.println("  Inflating: " + entry.getName()); else {
                System.out.println(" Extracting: " + entry.getName());
            }
        }
    }

    /**
	 * Gets the data.
	 *
	 * @param file the file
	 *
	 * @return the data
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void getData(File file) throws IOException {
        this.getData(new FileInputStream(file));
    }

    /**
	 * Gets the data.
	 *
	 * @param url the url
	 *
	 * @return the data
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void getData(URL url) throws IOException {
        this.getData(url.openStream());
    }

    /**
	 * The main method.
	 *
	 * @param args the arguments
	 *
	 * @throws Exception the exception
	 */
    public static void main(String[] args) throws Exception {
        EDIDataInterfaceUBL2 u2 = new EDIDataInterfaceUBL2();
        File f = new File("D:\\diplom/prd-UBL-2.0.zip");
        u2.getData(f);
    }
}
