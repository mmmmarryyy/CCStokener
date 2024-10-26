package org.jpedal.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import org.jpedal.utils.LogWriter;

public class RandomAccessFileBuffer extends RandomAccessFile implements RandomAccessBuffer {

    private String fileName = "";

    public RandomAccessFileBuffer(File file, String mode) throws FileNotFoundException {
        super(file, mode);
        fileName = file.getAbsolutePath();
    }

    public RandomAccessFileBuffer(String file, String mode) throws FileNotFoundException {
        super(file, mode);
        fileName = file;
    }

    public byte[] getPdfBuffer() {
        URL url = null;
        byte[] pdfByteArray = null;
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            url = new URL("file:" + fileName);
            is = url.openStream();
            os = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            pdfByteArray = os.toByteArray();
            is.close();
            os.close();
        } catch (IOException e) {
            LogWriter.writeLog("[PDF] Exception " + e + " getting byte[] for " + fileName);
        }
        return pdfByteArray;
    }
}
