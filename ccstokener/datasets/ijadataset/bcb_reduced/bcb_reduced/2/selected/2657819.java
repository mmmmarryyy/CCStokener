package edu.ucla.stat.SOCR.chart.j3d.gui;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import javax.swing.JOptionPane;
import edu.ucla.stat.SOCR.gui.SOCROptionPane;

/**
 * A trivial implementation of Binned2DData for test purposes
 * @author Joy Kyriakopulos (joyk@fnal.gov)
 * @version $Id: SOCRBinnedBinary2DData.java,v 1.4 2010/12/09 19:18:56 jiecui Exp $
 */
public class SOCRBinnedBinary2DData extends SOCRBinned2DData {

    public SOCRBinnedBinary2DData() throws IOException {
        loadDataFloat(new URL("http://"), "");
        xBins = 0;
        yBins = 0;
    }

    public SOCRBinnedBinary2DData(URL base, String fileName) throws IOException {
        if (fileName == null || fileName.length() == 0) super.loadDataFloat(base, fileName); else this.loadDataFloat(base, fileName);
    }

    public SOCRBinnedBinary2DData(URL url, int x, int y) throws IOException {
        xBins = x;
        yBins = y;
        this.loadDataFloat(url);
    }

    public SOCRBinnedBinary2DData(URL base, String fileName, int x, int y) throws IOException {
        xBins = x;
        yBins = y;
        if (fileName == null || fileName.length() == 0) {
            super.loadDataFloat(base, fileName);
            return;
        }
        this.loadDataFloat(base, fileName);
    }

    public void loadDataFloat(URL url) throws IOException {
        if (xBins == 0 || yBins == 0) loadBinSizes(10, 10);
        InputStream is = (url.openStream());
        BufferedInputStream bis = new BufferedInputStream(is);
        loadDataFloat(getBytesFromFile(bis));
    }

    public void loadDataFloat(byte[] bytes) throws IOException {
        if (xBins == 0 || yBins == 0) loadBinSizes(256, 256);
        data = new float[xBins][yBins];
        try {
            for (int i = 0; i < xBins; i++) for (int j = 0; j < xBins; j++) {
                data[i][j] = bytes[i * xBins + j];
            }
        } catch (NumberFormatException e) {
            SOCROptionPane.showMessageDialog(null, "Data format error, input data in the format of \" interger(x position) integer(y position) float(value) \" is excepted.");
            return;
        }
        findZRange();
    }

    public byte[] getBytesFromFile(BufferedInputStream bis) throws IOException {
        byte[] bytes = null;
        try {
            long length = bis.available();
            if (length > Integer.MAX_VALUE) {
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = bis.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + bis);
            }
        } finally {
            bis.close();
        }
        return bytes;
    }

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] bytes = null;
        try {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + is);
            }
        } finally {
            is.close();
        }
        return bytes;
    }

    public byte[] getBytesFromRemoteFile(InputStream is) throws IOException {
        byte[] bytes = null;
        String inputStreamToString = is.toString();
        bytes = inputStreamToString.getBytes();
        return bytes;
    }

    public byte[] getBytesFromRemoteFile2(File file) throws IOException {
        int size = (int) file.length();
        if (size > Integer.MAX_VALUE) {
            System.out.println("File is to larger");
        }
        byte[] bytes = new byte[size];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        int read = 0;
        int numRead = 0;
        while (read < bytes.length && (numRead = dis.read(bytes, read, bytes.length - read)) >= 0) {
            read = read + numRead;
        }
        System.out.println("File size: " + read);
        if (read < bytes.length) {
            System.out.println("Could not completely read: " + file.getName());
        }
        return bytes;
    }
}
