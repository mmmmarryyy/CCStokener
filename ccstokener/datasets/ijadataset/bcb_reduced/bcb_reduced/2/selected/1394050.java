package gov.sns.tools.data.profile;

import gov.sns.tools.text.SnsTimeStampParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

/**
 * File parser for data files produced by SNS beam profile diagnostic devices.
 * Objects of type <code>ProfileData</code> are created to store the data.  Note,
 * however, that because the data files may contain data sets for multiple devices
 * the returned objects are <code>Collections</code> of such objects.
 * 
 * 
 * @author Christopher K. Allen
 *
 */
public class SnsProFileParser {

    /** Version Number */
    public static final int CLASS_VERSION = 2;

    /** Regular expression - regular expression whitespace specifier */
    public static final String STR_REGEX_WHITESPACE = "\\s+";

    /** Time stamp delimiter - within the date line, the actual date/time comes after this marker */
    public static final String STR_DEL_TIMESTAMP = "start time:";

    /** Data delimiter - e.i., we look for this token and assume the data comes afterward */
    public static final String STR_DEL_PROFILEDATA = "--------";

    /** PV Logger delimiter - the PV Logger record id comes after this */
    public static final String STR_DEL_PVLOGGERID = "PVLogger";

    /** Stream buffer head room from current mark */
    public static final int INT_STREAM_HEADROOM = 256;

    /** The date formatting object used for time stamps */
    public static final DateFormat DFO_TIME_STAMP = DateFormat.getDateTimeInstance();

    /**
     * <p>
     * Parse the given file of wire-scanner measurement data.  The data is returned as
     * a <code>Collection</code> of concrete objects with super type 
     * <code>ProfileData</code>.  The exact type depends upon the type of measurement
     * device used.  The number of data objects in the collection dependents upon the number
     * of data sets in the file (one data set per measurement device).
     * </p>
     * 
     * <p>
     * NOTE:
     * This is a convenience method. We have no way of enforcing that the given argument is 
     * actually a well-formed URL string, other than throwing an exception.
     * </p>
     *  
     * @param   strFileData  path string of profile measurement data file
     *   
     * @return              collection of raw profile data sets
     * 
     * @throws IOException  Bad URL string, or bad data format (see exception message)
     */
    public static Collection<? extends ProfileData> parseWireFile(String strFileData) throws IOException {
        try {
            File fileData = new File(strFileData);
            return parseWireFile(fileData);
        } catch (NullPointerException e) {
            throw new IOException("Cannot find file - " + strFileData);
        }
    }

    /**
     * Parse the given file of wire-scanner measurement data.  The data is returned as
     * a <code>Collection</code> of concrete objects with super type 
     * <code>ProfileData</code>.  The exact type depends upon the type of measurement
     * device used.  The number of data objects in the collection dependents upon the number
     * of data sets in the file (one data set per measurement device).
     *  
     * @param   fileData    file containing profile measurement data
     *   
     * @return              collection of raw profile data sets
     * 
     * @throws IOException  Bad file name, or bad data format (see exception message)
     */
    public static Collection<? extends ProfileData> parseWireFile(File fileData) throws IOException {
        try {
            URI uriData = fileData.toURI();
            URL urlData = uriData.toURL();
            return parseWireFile(urlData);
        } catch (MalformedURLException e) {
            throw new IOException("Unknown protocol for file URL" + fileData.toURI());
        } catch (IllegalArgumentException e) {
            throw new IOException("Bad file path - " + fileData.getAbsolutePath());
        }
    }

    /**
     * Parse the given file URL for wire-scanner measurement data.  The data is returned as
     * a <code>Collection</code> of concrete objects with super type 
     * <code>ProfileData</code>.  The exact type depends upon the type of measurement
     * device used.  The number of data objects in the collection dependents upon the number
     * of data sets in the file (one data set per measurement device).
     *  
     * @param   urlData     URL of profile measurement data file
     *   
     * @return              collection of raw profile data sets
     * 
     * @throws IOException  Bad file name, or bad data format (see exception message)
     */
    public static Collection<? extends ProfileData> parseWireFile(URL urlData) throws IOException {
        SnsProFileParser parser = new SnsProFileParser(urlData);
        return parser.parseWireFile();
    }

    /** URL of the data file associated with this parser */
    private URL urlData;

    /** Time stamp of data file */
    private Date dateTmStamp;

    /** Process Variable Logger (PVLogger) record identifier */
    private int intPvLogId;

    /** Collection of profile measurement data sets */
    private Collection<ProfileData> colProjData;

    /**
     * Create a parser object and connect it to the given file containing
     * profile measurement data in the current SNS format.
     *  
     * @param   urlData     URL of profile measurement data file
     */
    public SnsProFileParser(URL urlData) {
        this.urlData = urlData;
        this.dateTmStamp = null;
        this.intPvLogId = 0;
    }

    /**
     * Parse the associated wire-scanner measurement data file and return the 
     * raw data as a <code>Collection</code> of <code>ProfileData</code>
     * objects.  Note that the number of these objects depends upon the
     * number of measurement devices used to produce the target data file.
     * 
     * @return  collection of <code>ProfileData</code> objects 
     * 
     * @throws IOException  bad file URL or bad data format (see message)
     */
    public Collection<? extends ProfileData> parseWireFile() throws IOException {
        InputStream isRaw = this.urlData.openStream();
        InputStreamReader isRdr = new InputStreamReader(isRaw);
        BufferedReader isBuf = new BufferedReader(isRdr);
        this.colProjData = new LinkedList<ProfileData>();
        if (!this.processTimeStamp(isBuf)) throw new IOException("SnsProFileParser#parse() - could not read time stamp");
        if (!this.processDeviceData(isBuf)) throw new IOException("SnsProFileParser#parse() - could not read profile data");
        if (!this.processPvLoggerId(isBuf)) throw new IOException("SnsProFileParser#parse() - could not read PV Logger Id");
        isBuf.close();
        for (ProfileData datDevice : this.colProjData) datDevice.setPvLoggerId(this.getPvLoggerId());
        return this.colProjData;
    }

    /**
     * Return the time stamp of the measurement data.  
     * 
     * @return  time stamp of measurement data or null if file has not been parsed
     * 
     * @see SnsProFileParser#parseWireFile()
     */
    private Date getTimeStamp() {
        return this.dateTmStamp;
    }

    /**
     * Return the PV Logger Id of the measurement data.
     * 
     * @return  PV Logger Id of measurement data or 0 if file has not been parsed
     * 
     * @see SnsProFileParser#parseWireFile()
     */
    private int getPvLoggerId() {
        return this.intPvLogId;
    }

    /**
     * Read the time stamp of the file while advancing the 
     * stream position.
     * 
     * @param isBuf     input data stream
     * 
     * @return          true if the time stamp was set, false if EOS.
     * 
     * @throws IOException  error during file read
     */
    private boolean processTimeStamp(BufferedReader isBuf) throws IOException {
        String strBuffer;
        if ((strBuffer = this.loadLineBuffer(isBuf)) == null) return false;
        if (!strBuffer.startsWith(STR_DEL_TIMESTAMP)) return false;
        try {
            strBuffer = strBuffer.replaceFirst(STR_DEL_TIMESTAMP, "");
            this.dateTmStamp = SnsTimeStampParser.parseDateTime(strBuffer);
            return true;
        } catch (ParseException e) {
            throw new IOException("SnsProFileParser#processTimeStamp(): Bad date format - " + e.getMessage());
        }
    }

    /**
     * Read out the blocks of data corresponding to each measurement device.  Assume that the
     * input stream is positioned at the first block of data.  Each data block consists of
     * the measurement device id, statistical parameters of the data, the raw profile data, 
     * then the fitted data curves.  Since we are only interested in the raw data we throw 
     * out everything else, except the device id.
     *   
     * @param isBuf     input stream positioned at the first profile data block
     * 
     * @return
     * @throws IOException
     */
    private boolean processDeviceData(BufferedReader isBuf) throws IOException {
        String strDevId;
        ProfileData datDevice;
        ProfileData datFitted;
        while (true) {
            isBuf.mark(INT_STREAM_HEADROOM);
            if ((strDevId = this.loadDeviceId(isBuf)) == null) break;
            datDevice = this.loadDeviceData(isBuf, strDevId);
            datFitted = this.loadDeviceData(isBuf, strDevId);
            if (datDevice != null) {
                this.colProjData.add(datDevice);
            } else if (!isBuf.ready()) {
                break;
            }
        }
        isBuf.reset();
        return true;
    }

    /**
     * Read the PV Logger record identifier and advance the stream position.
     * 
     * @param isBuf     input stream corresponding to profile data file
     *  
     * @return          true if PV Logger id was set, false if not found or EOS
     * 
     * @throws IOException  error during file read, or error parsing PV logger id
     */
    private boolean processPvLoggerId(BufferedReader isBuf) throws IOException {
        String strBuffer;
        String[] arrTokens;
        if ((strBuffer = this.loadLineBuffer(isBuf)) == null) return false;
        if (!strBuffer.startsWith(STR_DEL_PVLOGGERID)) return false;
        arrTokens = strBuffer.split(STR_REGEX_WHITESPACE);
        try {
            this.intPvLogId = new Integer(Integer.parseInt(arrTokens[2]));
            return true;
        } catch (NumberFormatException e) {
            throw new IOException("SnsProfileParse#processPvLoggerId(): Number format exception - " + e.getMessage());
        }
    }

    /**
     * Read from the given stream until we find a non-empty line of text, that is,
     * ignore any empty lines encountered.  Returns a string buffer containing the
     * non-empty line of text.  The input stream is advanced past the first non-
     * empty line.
     * 
     * @param isBuf     input stream (data source)
     * 
     * @return  full text buffer if buffer was successfully loaded, null if end-of-stream encountered
     * 
     * @throws IOException  error during file read
     */
    private String loadLineBuffer(BufferedReader isBuf) throws IOException {
        String strBuffer;
        while (true) {
            strBuffer = isBuf.readLine();
            if (strBuffer == null) return null;
            if (strBuffer.length() != 0) return strBuffer;
        }
    }

    /**
     * Read the device identifier string and advance the input stream.
     * 
     * @param isBuf     input stream (data source)
     * 
     * @return          device string id, or null if EOS
     * 
     * @throws IOException  error during file read
     */
    private String loadDeviceId(BufferedReader isBuf) throws IOException {
        return this.loadLineBuffer(isBuf);
    }

    /**
     * <p>
     * Loads the profile data from the given input stream and returns it in
     * a new <code>ProfileData</code> object.  The returned data object has
     * the device id and time stamp given in the arguments.  The initial position
     * of the stream is assumed to be at the beginning of a profile data block,
     * (after the device id line).  Upon returning, the stream is advanced to the 
     * end of the data block.
     * </p>
     * 
     * <p>
     * <br><l>Operations are performed in the following order:</l>
     * <br><l> - The input stream is advanced until the profile data delimiter is found.</l>
     * <br><l> - If no delimiter is found a <code>null</code> is returned, indicating EOS.</l>
     * <br><l> - Data is read, line by line, and parsed according to column position. </l>
     * <br><l> - The parsed columns are stored in intermediate lists.</l>
     * <br><l> - Data reading stops when an empty line is encountered. </l>
     * <br><l> - The data are then stored in a new <code>ProfileData</code> object. </l>
     * </p>
     * <p>
     * <br>NOTE:
     * <br> - All stream data before the data delimiter (Sns.ProFileParser.STR_DEL_PROFILEDATA)
     * is ignored.  This includes any computed statistical parameters occurring before
     * the actual raw data.
     * <br> - This method has been modified to recognize three additional data columns in
     * the wire data file.  These columns are the sampling positions of the wires in the
     * beam coordinates. 
     * </p>
     * 
     * @param  isBuf        input stream positioned at the beginning of a profile data block
     * @param  strDevId     measurement device id for profile data
     * @param  dateTmStamp  time stamp for profile data
     * 
     * @return          <code>ProfileData</code> data structure
     * 
     * @throws IOException  error during read, or number format not parseable
     * 
     * @author Christopher K. Allen
     */
    private ProfileData loadDeviceData(BufferedReader isBuf, String strDevId) throws IOException {
        String strBuffer;
        String[] arrTokens;
        Date dateTmStamp = this.getTimeStamp();
        int szArrData = 0;
        boolean bolPosData = false;
        LinkedList<Double> lstAct = new LinkedList<Double>();
        LinkedList<Double> lstHor = new LinkedList<Double>();
        LinkedList<Double> lstVer = new LinkedList<Double>();
        LinkedList<Double> lstDia = new LinkedList<Double>();
        LinkedList<Double> lstPsH = new LinkedList<Double>();
        LinkedList<Double> lstPsV = new LinkedList<Double>();
        LinkedList<Double> lstPsD = new LinkedList<Double>();
        while (true) {
            if ((strBuffer = this.loadLineBuffer(isBuf)) == null) return null;
            if (strBuffer.startsWith(STR_DEL_PROFILEDATA)) break;
        }
        while (true) {
            if ((strBuffer = isBuf.readLine()) == null) return null;
            if (strBuffer.length() == 0) break;
            arrTokens = strBuffer.split(STR_REGEX_WHITESPACE);
            try {
                lstAct.add(Double.parseDouble(arrTokens[0]));
                lstHor.add(Double.parseDouble(arrTokens[1]));
                lstVer.add(Double.parseDouble(arrTokens[2]));
                lstDia.add(Double.parseDouble(arrTokens[3]));
                if (arrTokens.length >= 7) {
                    bolPosData = true;
                    lstPsH.add(Double.parseDouble(arrTokens[4]));
                    lstPsV.add(Double.parseDouble(arrTokens[5]));
                    lstPsD.add(Double.parseDouble(arrTokens[6]));
                }
            } catch (NumberFormatException e) {
                throw new IOException("SnsProfileParse#processDeviceData(): Number format exception - " + e.getMessage());
            }
            szArrData++;
        }
        if (szArrData == 0) return null;
        ProfileData rec = ProfileData.create(strDevId, dateTmStamp, szArrData);
        rec.setActuatorPositions(lstAct);
        rec.setProjection(ProfileData.Angle.HOR, lstHor);
        rec.setProjection(ProfileData.Angle.VER, lstVer);
        rec.setProjection(ProfileData.Angle.DIA, lstDia);
        if (bolPosData) {
            rec.setAxisPositions(ProfileData.Angle.HOR, lstPsH);
            rec.setAxisPositions(ProfileData.Angle.VER, lstPsV);
            rec.setAxisPositions(ProfileData.Angle.DIA, lstPsD);
        }
        return rec;
    }
}
