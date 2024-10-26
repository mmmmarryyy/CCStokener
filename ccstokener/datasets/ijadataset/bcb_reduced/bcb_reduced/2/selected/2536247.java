package org.npsnet.v.models.terrain;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * Parses DTED data.
 *
 * @author Chris Thorne
 * @author Dave Back
 * @author Victor Spears
 * @author Andrzej Kapolka
 */
public class DTEDParser {

    /**
     * The size of the UHL element.
     */
    private static final int UHL_SIZE = 80;

    /**
     * The size of the DSI element.
     */
    private static final int DSI_SIZE = 648;

    /**
     * The size of the ACC element.
     */
    private static final int ACC_SIZE = 2700;

    /**
     * The offset, within the UHL element, of the number-of-records element
     * (encoded as a string of fixed length).
     */
    private static final int NUMBER_OF_RECORDS_OFFSET = 47;

    /**
     * The size of the number-of-records element.
     */
    private static final int NUMBER_OF_RECORDS_SIZE = 4;

    /**
     * The offset, within the UHL element, of the record-size element
     * (encoded as a string of fixed length).
     */
    private static final int RECORD_SIZE_OFFSET = 51;

    /**
     * The size of the record-size element.
     */
    private static final int RECORD_SIZE_SIZE = 4;

    /**
     * The offset, within the UHL element, of the latitudinal-spacing element
     * (encoded as a string of fixed length).
     */
    private static final int LATITUDINAL_SPACING_OFFSET = 25;

    /**
     * The size of the latitudinal-spacing element.
     */
    private static final int LATITUDINAL_SPACING_SIZE = 3;

    /**
     * The offset, within the UHL element, of the longitudinal-spacing element
     * (encoded as a string of fixed length).
     */
    private static final int LONGITUDINAL_SPACING_OFFSET = 21;

    /**
     * The size of the longitudinal-spacing element.
     */
    private static final int LONGITUDINAL_SPACING_SIZE = 3;

    /**
     * The scale factor to apply to the spacing elements to convert them into
     * degrees.
     */
    private static final double SPACING_SCALE = 1.0 / 36000.0;

    /**
     * The column recognition sentinel.
     */
    private static final int COLUMN_SENTINEL = 0xAA;

    /**
     * The size of the DTED column headers.
     */
    private static final int COLUMN_HEADER_SIZE = 8;

    /**
     * The size of the DTED checksum.
     */
    private static final int CHECKSUM_SIZE = 4;

    /**
     * The minimum allowed height.
     */
    private static final int MINIMUM_HEIGHT = -12000;

    /**
     * The heightfield array.
     */
    private double[] heightfield;

    /**
     * The number of samples in the x direction.
     */
    private int numberOfSamplesX;

    /**
     * The number of samples in the z direction.
     */
    private int numberOfSamplesZ;

    /**
     * The latitudinal size of the region.
     */
    private double sizeLatitude;

    /**
     * The longitudinal size of the region.
     */
    private double sizeLongitude;

    /**
     * Returns a reference to the heightfield data read.
     *
     * @return a reference to the heightfield data read
     */
    public double[] getHeightfield() {
        return heightfield;
    }

    /**
     * Returns the number of samples in the x direction.
     *
     * @return the number of samples in the x direction
     */
    public int getNumberOfSamplesX() {
        return numberOfSamplesX;
    }

    /**
     * Returns the number of samples in the z direction.
     *
     * @return the number of samples in the z direction
     */
    public int getNumberOfSamplesZ() {
        return numberOfSamplesZ;
    }

    /**
     * Returns the latitudinal size of the region (in degrees).
     *
     * @return the latitudinal size of the region (in degrees)
     */
    public double getSizeLatitude() {
        return sizeLatitude;
    }

    /**
     * Returns the longitudinal size of the region (in degrees).
     *
     * @return the longitudinal size of the region (in degrees)
     */
    public double getSizeLongitude() {
        return sizeLongitude;
    }

    /**
     * Parses the DTED file located at the specified URL.
     *
     * @param url the URL of the DTED file to parse
     * @exception IOException if an error occurs
     */
    public void parse(URL url) throws IOException {
        parse(url.openStream());
    }

    /**
     * Reads a DTED file from the specified input stream.
     *
     * @param is the input stream to read from
     * @exception IOException if an error occurs
     */
    public void parse(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        byte[] uhl = new byte[UHL_SIZE], numRecordsValue = new byte[NUMBER_OF_RECORDS_SIZE], recordSizeValue = new byte[RECORD_SIZE_SIZE], latSpacingValue = new byte[LATITUDINAL_SPACING_SIZE], longSpacingValue = new byte[LONGITUDINAL_SPACING_SIZE];
        dis.readFully(uhl);
        System.arraycopy(uhl, NUMBER_OF_RECORDS_OFFSET, numRecordsValue, 0, NUMBER_OF_RECORDS_SIZE);
        System.arraycopy(uhl, RECORD_SIZE_OFFSET, recordSizeValue, 0, RECORD_SIZE_SIZE);
        System.arraycopy(uhl, LATITUDINAL_SPACING_OFFSET, latSpacingValue, 0, LATITUDINAL_SPACING_SIZE);
        System.arraycopy(uhl, LONGITUDINAL_SPACING_OFFSET, longSpacingValue, 0, LONGITUDINAL_SPACING_SIZE);
        int numRecords, recordSize;
        double latSpacing, longSpacing;
        try {
            numRecords = Integer.parseInt(new String(numRecordsValue, "US-ASCII"));
            recordSize = Integer.parseInt(new String(recordSizeValue, "US-ASCII"));
            latSpacing = Integer.parseInt(new String(latSpacingValue, "US-ASCII")) * SPACING_SCALE;
            longSpacing = Integer.parseInt(new String(longSpacingValue, "US-ASCII")) * SPACING_SCALE;
        } catch (Exception e) {
            throw new IOException("Bad DTED file");
        }
        numberOfSamplesX = numRecords;
        numberOfSamplesZ = recordSize;
        sizeLatitude = (numberOfSamplesZ - 1) * latSpacing;
        sizeLongitude = (numberOfSamplesX - 1) * longSpacing;
        dis.skip(DSI_SIZE + ACC_SIZE);
        heightfield = new double[numRecords * recordSize];
        int b, height;
        for (int i = 0; i < numRecords; i++) {
            boolean headerFound = false;
            while (!headerFound) {
                b = dis.read();
                if (b == COLUMN_SENTINEL) {
                    b = ((dis.read() & 0xFF) << 16) | ((dis.read() & 0xFF) << 8) | (dis.read() & 0xFF);
                    if (b == i) headerFound = true;
                }
            }
            ;
            dis.skip(COLUMN_HEADER_SIZE - 4);
            for (int j = 0; j < recordSize; j++) {
                height = dis.readShort();
                if ((height & 0x8000) != 0) {
                    height = -(height & 0x7FFF);
                }
                heightfield[((recordSize - 1) - j) * numRecords + i] = (height < MINIMUM_HEIGHT) ? 0.0 : height;
            }
            dis.skip(CHECKSUM_SIZE);
        }
    }
}
