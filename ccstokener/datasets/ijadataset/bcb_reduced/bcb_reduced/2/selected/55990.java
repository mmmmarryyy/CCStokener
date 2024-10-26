package prajna.geo;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import prajna.data.*;

/**
 * Reads ESRI shape files and associated files to construct geographic layers
 * of data. This class can read shape files and the associated feature files
 * from a file system or accessible URL. It uses these to construct the
 * geographic shapes which can then be used for a variety of tasks, such as map
 * displays or geographic filtering.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class EsriReader extends GeoReader {

    private DataInputStream dbfStream = null;

    private DataInputStream shpStream = null;

    private ArrayList<DataRecord> shapeRecords = new ArrayList<DataRecord>();

    private int currentShape = 0;

    private URL base = null;

    private String baseName = null;

    private SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");

    private ArrayList<ShapeFieldDescriptor> descriptors = new ArrayList<ShapeFieldDescriptor>();

    /**
     * This class stores a description of DBF record fields (name, type and
     * length). Descriptors are created when reading the header of a DBF file.
     */
    private class ShapeFieldDescriptor implements Serializable {

        private static final long serialVersionUID = -4419321656701361993L;

        public final String name;

        public final byte type;

        public int length;

        /**
         * Constructor.
         * 
         * @param nam Field name
         * @param typ field type, corresponding to Esri field types
         * @param len field length
         */
        public ShapeFieldDescriptor(String nam, byte typ, int len) {
            name = nam;
            type = typ;
            length = len;
            if (length < 0) {
                length += 256;
            }
        }

        /**
         * Gets the length.
         * 
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * Gets the name.
         * 
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the type.
         * 
         * @return the type
         */
        public byte getType() {
            return type;
        }
    }

    /**
     * Instantiates a new EsriReader. This class initializes the reader to
     * point to the specified file. Any extensions of the name are stripped
     * internally, so that this reader can access ESRI files with different
     * extensions.
     * 
     * @param shapeFile the URL of the shape file
     */
    public EsriReader(File shapeFile) {
        File parent = shapeFile.getParentFile();
        try {
            base = parent.toURI().toURL();
        } catch (MalformedURLException exc) {
            exc.printStackTrace();
        }
        String fileName = shapeFile.getName();
        int dotInx = fileName.indexOf('.');
        baseName = (dotInx > -1) ? fileName.substring(0, dotInx) : fileName;
        loadShapes();
    }

    /**
     * Instantiates a new EsriReader. This class initializes the reader to
     * point to the specified file. Any extensions of the name are stripped
     * internally, so that this reader can access ESRI files with different
     * extensions. The URL may specify a local file or an accessible URL on a
     * remote system
     * 
     * @param shapeUrl the URL of the shape file
     */
    public EsriReader(URL shapeUrl) {
        String baseStr = shapeUrl.toString();
        int lastSlash = baseStr.lastIndexOf('/');
        try {
            base = new URL(baseStr.substring(0, lastSlash + 1));
        } catch (MalformedURLException exc) {
            exc.printStackTrace();
        }
        int dotInx = baseStr.indexOf('.', lastSlash + 1);
        baseName = (dotInx > -1) ? baseStr.substring(lastSlash + 1, dotInx) : baseStr.substring(lastSlash + 1);
        loadShapes();
    }

    /**
     * Instantiates a new EsriReader. This class initializes the reader to
     * point to the specified base directory and file name. Any extensions of
     * the name are stripped internally, so that this reader can access ESRI
     * files with different extensions. The directory and files can be either
     * on a local file system or an accessible URL.
     * 
     * @param baseDir the base directory.
     * @param name the shape file name.
     */
    public EsriReader(URL baseDir, String name) {
        base = baseDir;
        int inx = name.indexOf('.');
        if (inx > -1) {
            baseName = name.substring(0, inx);
        } else {
            baseName = name;
        }
        loadShapes();
    }

    /**
     * Convert a series of bytes to a double
     * 
     * @param bytes byte sequence
     * @param offset Index within the sequence where to start converting.
     * @return the double value
     */
    private final double convertToDouble(byte[] bytes, int offset) {
        long lngBits = 0;
        long current;
        int shift = 0;
        for (int i = 0; i < 8; i++) {
            current = bytes[offset + i];
            lngBits += (current << shift);
            shift += 8;
        }
        return Double.longBitsToDouble(lngBits);
    }

    /**
     * Convert a series of bytes to an integer
     * 
     * @param bytes byte sequence
     * @param offset Index within the sequence where to start converting.
     * @return the integer value
     */
    private final int convertToInt(byte[] bytes, int offset) {
        int intBits = 0;
        int current;
        int shift = 0;
        for (int i = 0; i < 4; i++) {
            current = bytes[offset + i];
            intBits += (current << shift);
            shift += 8;
        }
        return intBits;
    }

    /**
     * Convert a series of bytes to a short integer
     * 
     * @param bytes byte sequence
     * @param offset Index within the sequence where to start converting.
     * @return the short value
     */
    private final short convertToShort(byte[] bytes, int offset) {
        short shBits = 0;
        int shift = 0;
        for (int i = 0; i < 2; i++) {
            int current = bytes[offset + i];
            shBits += (current << shift);
            shift += 8;
        }
        return shBits;
    }

    /**
     * Return a map of the fields for this shapefile, along with data types.
     * 
     * @return a field map including field names and types
     */
    public Map<String, DataType> getFields() {
        HashMap<String, DataType> fieldMap = new HashMap<String, DataType>();
        for (ShapeFieldDescriptor desc : descriptors) {
            DataType type = null;
            char chType = (char) desc.getType();
            if (chType == 'C') {
                type = DataType.TEXT;
            } else if (chType == 'D' || chType == '@') {
                type = DataType.TIME;
            } else if (chType == 'I' || chType == '2' || chType == '4') {
                type = DataType.INT;
            } else if (chType == 'N' || chType == 'O' || chType == '8') {
                type = DataType.MEASURE;
            }
            if (type != null) {
                fieldMap.put(desc.getName(), type);
            }
        }
        return fieldMap;
    }

    /**
     * Gets the name associated with this reader. For an ESRI reader, this
     * method returns the base name. The base name is the base file name for
     * this reader, without extensions.
     * 
     * @return the base file name
     */
    @Override
    public String getName() {
        return baseName;
    }

    /**
     * Load the shapes and associated data from the shape files. If the reader
     * has not already loaded the data, this method first reads the shape
     * record files (DBF extension). This loads all data records into an
     * internal list. It then reads the geographic shapes themselves (SHP
     * extension). The data records and shape files should correspond to each
     * other on a one-for-one basis.
     */
    private void loadShapes() {
        try {
            readDBF();
        } catch (IOException ioe) {
        }
        try {
            readSHP();
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot find shape file: " + baseName + " at " + base, ioe);
        }
    }

    /**
     * Read the DBF File. The DBF file contains a list of record data for each
     * shape in the corresponding SHP file. The order of the two files is
     * consistent.
     * 
     * @throws IOException if the file is not available, or there is a problem
     *             reading the data.
     */
    private void readDBF() throws IOException {
        URL url = new URL(base, baseName + ".dbf");
        dbfStream = new DataInputStream(url.openStream());
        dbfStream.skipBytes(4);
        int numRecs = readInt(dbfStream);
        short headerSize = readShort(dbfStream);
        short recordSize = readShort(dbfStream);
        dbfStream.skipBytes(20);
        int numFields = (headerSize - 33) / 32;
        for (int i = 0; i < numFields; i++) {
            String fieldName = readString(dbfStream, 11);
            byte fieldType = dbfStream.readByte();
            dbfStream.skipBytes(4);
            byte fieldLength = dbfStream.readByte();
            dbfStream.skipBytes(15);
            ShapeFieldDescriptor field = new ShapeFieldDescriptor(fieldName, fieldType, fieldLength);
            descriptors.add(field);
        }
        dbfStream.skipBytes(2);
        byte records[] = new byte[recordSize * numRecs];
        dbfStream.read(records);
        for (int i = 0; i < numRecs; i++) {
            DataRecord node = readShapeRecord(descriptors, records, i * recordSize);
            shapeRecords.add(node);
        }
        dbfStream.close();
    }

    /**
     * Read a double precision value from the data stream
     * 
     * @param stream the data stream
     * @return the double value that was read
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private final double readDouble(DataInputStream stream) throws IOException {
        long lngBits = 0;
        for (int shift = 0; shift < 64; shift += 8) {
            long current = stream.readUnsignedByte();
            lngBits += (current << shift);
        }
        return Double.longBitsToDouble(lngBits);
    }

    /**
     * Read a sequence of double values. This method is used to read the M and
     * Z values of shapes with multiple points.
     * 
     * @param count the number of points in the double set
     * @return the sequence of double values
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private double[] readDoubleRange(int count) throws IOException {
        double[] vals = new double[count];
        readDouble(shpStream);
        readDouble(shpStream);
        for (int i = 0; i < count; i++) {
            vals[i] = readDouble(shpStream);
        }
        return vals;
    }

    /**
     * Read an integer from the data stream
     * 
     * @param stream the data stream
     * @return the integer value that was read
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private final int readInt(DataInputStream stream) throws IOException {
        int intBits = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int current = stream.readUnsignedByte();
            intBits += (current << shift);
        }
        return intBits;
    }

    /**
     * Read a geographic point shape.
     * 
     * @param ptType the point type for the polygon. Valid values are ' ' (2D
     *            coordinates), 'M' (measured coordinates), and 'Z' (3D
     *            coordinates).
     * @return the geo marker representing the point shape.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private GeoMarker readPoint(char ptType) throws IOException {
        double lon = readDouble(shpStream);
        double lat = readDouble(shpStream);
        double alt = 0;
        if (ptType == 'M') {
            readDouble(shpStream);
        } else if (ptType == 'Z') {
            alt = readDouble(shpStream);
            readDouble(shpStream);
        }
        GeoCoord coord = new GeoCoord(lat, lon, alt);
        GeoMarker point = new GeoMarker(coord);
        return point;
    }

    /**
     * Read arc and polygon shapes. If the <code>isClosed</code> flag is set,
     * this method returns polygons. Otherwise, it returns polylines. This
     * method can read a composite shape, returning a GeoMultiShape containing
     * all of the shapes read. This allows a single DataRecord to match
     * multiple shapes when the geographic entity is not contiguous (for
     * example, Michigan or Hawaii).
     * 
     * @param isClosed the closed flag, determining if open polylines or closed
     *            polygons should be returned.
     * @param ptType the point type for the polygon. Valid values are ' ' (2D
     *            coordinates), 'M' (measured coordinates), and 'Z' (3D
     *            coordinates).
     * @return the shape or shapes read.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private GeoShape readPolyline(boolean isClosed, char ptType) throws IOException {
        readDouble(shpStream);
        readDouble(shpStream);
        readDouble(shpStream);
        readDouble(shpStream);
        int nParts = readInt(shpStream);
        int nPoints = readInt(shpStream);
        int parts[] = new int[nParts];
        for (int i = 0; i < nParts; i++) {
            parts[i] = readInt(shpStream);
        }
        int nums[] = new int[nParts];
        for (int i = 0; i < nParts; i++) {
            if (i != nParts - 1) {
                nums[i] = parts[i + 1] - parts[i];
            } else {
                nums[i] = nPoints - parts[i];
            }
        }
        GeoCoord[] coords = new GeoCoord[nPoints];
        for (int i = 0; i < nPoints; i++) {
            double lon = readDouble(shpStream);
            double lat = readDouble(shpStream);
            coords[i] = new GeoCoord(lat, lon);
        }
        if (ptType == 'Z') {
            double[] alts = readDoubleRange(nPoints);
            for (int i = 0; i < nPoints; i++) {
                coords[i].setAltitude(alts[i]);
            }
        }
        if (ptType == 'M' || ptType == 'Z') {
            readDoubleRange(nPoints);
        }
        GeoMultiShape multi = new GeoMultiShape();
        for (int i = 0; i < nParts; i++) {
            int partPts = nums[i];
            GeoCoord[] polyCoords = new GeoCoord[partPts];
            for (int j = 0; j < partPts; j++) {
                polyCoords[j] = coords[parts[i] + j];
            }
            if (isClosed) {
                multi.add(new GeoPolygon(polyCoords));
            } else {
                multi.add(new GeoPolyline(polyCoords));
            }
        }
        return (multi.size() == 1) ? multi.get(0) : multi;
    }

    /**
     * Reads the contents of the next record.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private final void readRecordContents() throws IOException {
        int stype = readInt(shpStream);
        GeoShape shape = null;
        switch(stype) {
            case 0:
                break;
            case 1:
                shape = readPoint(' ');
                break;
            case 3:
                shape = readPolyline(false, ' ');
                break;
            case 5:
                shape = readPolyline(true, ' ');
                break;
            case 8:
                shape = readSimplePolyline(' ');
                break;
            case 11:
                shape = readPoint('Z');
                break;
            case 13:
                shape = readPolyline(false, 'Z');
                break;
            case 15:
                shape = readPolyline(true, 'Z');
                break;
            case 18:
                shape = readSimplePolyline('Z');
                break;
            case 21:
                shape = readPoint('M');
                break;
            case 23:
                shape = readPolyline(false, 'M');
                break;
            case 25:
                shape = readPolyline(true, 'M');
                break;
            case 28:
                shape = readSimplePolyline('M');
                break;
            default:
                throw new IOException("Unknown record type");
        }
        if (shape != null) {
            shape.setData(shapeRecords.get(currentShape));
            addShape(shape);
            currentShape++;
        }
    }

    /**
     * Reads the header of a record from the shape file.
     * 
     * @return true, if this method read the record header
     */
    private final boolean readRecordHeader() {
        try {
            shpStream.readInt();
            int length = shpStream.readInt();
            return (length != 0);
        } catch (Exception exc) {
            return false;
        }
    }

    /**
     * Returns field value with the specified name. This method reads data from
     * the internal buffer, and converts the data into a DataRecord
     * 
     * @param fields the list of fields
     * @param values the value buffer
     * @param offset the current offset
     * @return the DataRecord created
     */
    private DataRecord readShapeRecord(List<ShapeFieldDescriptor> fields, byte[] values, int offset) {
        DataRecord node = new DataRecord("");
        int off = 0;
        for (ShapeFieldDescriptor descr : fields) {
            String str = null;
            int len = descr.getLength();
            switch(descr.getType()) {
                case 'C':
                    str = new String(values, offset + off, len).trim();
                    if (descr.getName().equalsIgnoreCase("name")) {
                        node.setName(str);
                    }
                    node.setTextField(descr.getName(), str);
                    break;
                case 'D':
                    str = new String(values, offset + off, 8).trim();
                    try {
                        Date date = dateFmt.parse(str);
                        node.setTimeField(descr.getName(), new TimeSpan(date));
                    } catch (ParseException e) {
                    }
                    break;
                case 'F':
                    str = new String(values, offset + off, len).trim();
                    try {
                        double dblVal = Double.parseDouble(str);
                        node.setFloatField(descr.getName(), dblVal);
                    } catch (NumberFormatException exc2) {
                        node.setTextField(descr.getName(), str);
                    }
                    break;
                case 'N':
                    str = new String(values, offset + off, len).trim();
                    try {
                        int intVal = Integer.parseInt(str);
                        node.setIntField(descr.getName(), intVal);
                    } catch (NumberFormatException exc) {
                        try {
                            double dblVal = Double.parseDouble(str);
                            node.setFloatField(descr.getName(), dblVal);
                        } catch (NumberFormatException exc2) {
                            node.setTextField(descr.getName(), str);
                        }
                    }
                    break;
                case '2':
                    int shrt = convertToShort(values, offset + off);
                    node.setIntField(descr.getName(), shrt);
                    break;
                case '4':
                    int intVal = convertToInt(values, offset + off);
                    node.setIntField(descr.getName(), intVal);
                    break;
                case '8':
                    double dblVal = convertToDouble(values, offset + off);
                    node.setFloatField(descr.getName(), dblVal);
                    break;
                case '@':
                    int days = convertToInt(values, offset + off);
                    int millis = convertToInt(values, offset + off + 4);
                    days -= 2440587;
                    long dayMillis = days * 86400000;
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(dayMillis);
                    cal.set(Calendar.HOUR, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    dayMillis = cal.getTime().getTime() + millis;
                    Date date = new Date(dayMillis);
                    node.setTimeField(descr.getName(), new TimeSpan(date));
                    break;
                default:
                    System.err.println("Cannot Handle type " + descr.getType() + " for " + descr.getName());
            }
            off += len;
        }
        return node;
    }

    /**
     * Read a short integer from the data stream
     * 
     * @param stream the data stream
     * @return the short value that was read
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private final short readShort(DataInputStream stream) throws IOException {
        short shBits = 0;
        for (int shift = 0; shift < 16; shift += 8) {
            int current = stream.readUnsignedByte();
            shBits += (current << shift);
        }
        return shBits;
    }

    /**
     * Read the shape File. The shape file contains the actual geographic shape
     * data for a geographic layer. The data is stored in a binary format
     * 
     * @throws IOException if the file is not available, or there is a problem
     *             reading the data.
     */
    private void readSHP() throws IOException {
        URL url = new URL(base, baseName + ".shp");
        shpStream = new DataInputStream(url.openStream());
        int magic = shpStream.readInt();
        if (magic != 9994) {
            throw new IOException("Unknown file");
        }
        shpStream.skipBytes(20);
        shpStream.readInt();
        if (readInt(shpStream) != 1000) {
            throw new IOException("Bad version");
        }
        readInt(shpStream);
        double xMin = readDouble(shpStream);
        double yMin = readDouble(shpStream);
        double xMax = readDouble(shpStream);
        double yMax = readDouble(shpStream);
        if (xMin < -180) {
            xMin = -180;
        }
        if (yMin < -80) {
            yMin = -80;
        }
        if (xMax > 180) {
            xMax = 180;
        }
        if (yMax > 80) {
            yMax = 80;
        }
        shpStream.skipBytes(32);
        while (readRecordHeader()) {
            try {
                readRecordContents();
            } catch (Exception e) {
            }
        }
        shpStream.close();
    }

    /**
     * Read a simple polyline. This method reads the current shape stream,
     * accessing the current polygonal line segment
     * 
     * @param ptType the point type for the polygon. Valid values are ' ' (2D
     *            coordinates), 'M' (measured coordinates), and 'Z' (3D
     *            coordinates).
     * @return the geographic polyline that was read.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private GeoPolyline readSimplePolyline(char ptType) throws IOException {
        for (int i = 0; i < 4; i++) {
            readDouble(shpStream);
        }
        int nPoints = readInt(shpStream);
        GeoCoord coords[] = new GeoCoord[nPoints];
        for (int i = 0; i < nPoints; i++) {
            double lon = readDouble(shpStream);
            double lat = readDouble(shpStream);
            coords[i] = new GeoCoord(lat, lon);
        }
        if (ptType == 'Z') {
            double[] alts = readDoubleRange(nPoints);
            for (int i = 0; i < nPoints; i++) {
                coords[i].setAltitude(alts[i]);
            }
        }
        if (ptType == 'M' || ptType == 'Z') {
            readDoubleRange(nPoints);
        }
        GeoPolyline poly = new GeoPolyline(coords);
        return poly;
    }

    /**
     * Read a string from the data stream
     * 
     * @param stream the data stream
     * @param length the length of string to read
     * @return the string read
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private final String readString(DataInputStream stream, int length) throws IOException {
        int last = 0;
        byte readStringBytes[] = new byte[512];
        for (int c = 0; c < length; c++) {
            readStringBytes[c] = stream.readByte();
            if (readStringBytes[c] != 0) {
                last++;
            }
        }
        return new String(readStringBytes, 0, last);
    }
}
