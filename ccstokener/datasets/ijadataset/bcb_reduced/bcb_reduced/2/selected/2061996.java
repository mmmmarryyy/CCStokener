package org.exist.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.exist.util.ConfigurationHelper;

/**
 *  A set of helper methods for the validation tests.
 *
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class TestTools {

    public static final String VALIDATION_HOME_COLLECTION = "validation";

    public static final String VALIDATION_DTD_COLLECTION = "dtd";

    public static final String VALIDATION_XSD_COLLECTION = "xsd";

    public static final String VALIDATION_TMP_COLLECTION = "tmp";

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }

    /**
     *
     * @param file     File to be uploaded
     * @param target  Target URL (e.g. xmldb:exist:///db/collection/document.xml)
     * @throws java.lang.Exception  Oops.....
     */
    public static void insertDocumentToURL(String file, String target) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(file);
            final URL url = new URL(target);
            final URLConnection connection = url.openConnection();
            os = connection.getOutputStream();
            TestTools.copyStream(is, os);
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static String getEXistHome() {
        return ConfigurationHelper.getExistHome().getAbsolutePath();
    }

    public static byte[] getHamlet() throws IOException {
        return loadSample("shakespeare/hamlet.xml");
    }

    public static byte[] loadSample(String sampleRelativePath) throws IOException {
        File file = new File(getEXistHome(), "samples/" + sampleRelativePath);
        InputStream fis = null;
        ByteArrayOutputStream baos = null;
        try {
            fis = new FileInputStream(file);
            baos = new ByteArrayOutputStream();
            TestTools.copyStream(fis, baos);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
        return baos.toByteArray();
    }

    public static void insertDocumentToURL(byte[] data, String target) throws IOException {
        final URL url = new URL(target);
        final URLConnection connection = url.openConnection();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new ByteArrayInputStream(data);
            os = connection.getOutputStream();
            TestTools.copyStream(is, os);
            os.flush();
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}
