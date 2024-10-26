package org.fudaa.ctulu.gis.shapefile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.*;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.GeometryAttributeType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.feature.type.BasicFeatureTypes;
import org.geotools.filter.CompareFilter;
import org.geotools.filter.Filter;
import org.geotools.filter.FilterType;
import org.geotools.filter.LengthFunction;
import org.geotools.filter.LiteralExpression;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.AbstractCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.gml.GMLSchema;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.gis.GISGeometryFactory;

/**
 * A DataStore implementation which allows reading and writing from Shapefiles.
 * 
 * @author Ian Schneider
 * @todo fix file creation bug
 * @source $URL:
 *         http://svn.geotools.org/geotools/tags/2.2-RC3/plugin/shapefile/src/org/geotools/data/shapefile/ShapefileDataStore.java $
 */
public class ShapefileDataStore extends AbstractFileDataStore {

    protected final URL shpURL_;

    protected final URL dbfURL_;

    protected final URL shxURL_;

    protected final URL prjURL_;

    protected final URL xmlURL_;

    protected Lock readWriteLock_ = new Lock();

    protected URI namespace_;

    protected FeatureType schema_;

    protected boolean useMemoryMappedBuffer_ = true;

    /**
   * Creates a new instance of ShapefileDataStore.
   * 
   * @param _url The URL of the shp file to use for this DataSource.
   * @throws NullPointerException DOCUMENT ME!
   * @throws . If computation of related URLs (dbf,shx) fails.
   */
    public ShapefileDataStore(final URL _url) throws java.net.MalformedURLException {
        String filename = null;
        if (_url == null) {
            throw new NullPointerException("Null URL for ShapefileDataSource");
        }
        try {
            filename = java.net.URLDecoder.decode(_url.getFile(), "US-ASCII");
        } catch (final java.io.UnsupportedEncodingException use) {
            throw new java.net.MalformedURLException("Unable to decode " + _url + " cause " + use.getMessage());
        }
        String shpext = ".shp";
        String dbfext = ".dbf";
        String shxext = ".shx";
        String prjext = ".prj";
        String xmlext = ".shp.xml";
        if (filename.endsWith(shpext) || filename.endsWith(dbfext) || filename.endsWith(shxext)) {
            filename = filename.substring(0, filename.length() - 4);
        } else if (filename.endsWith(".SHP") || filename.endsWith(".DBF") || filename.endsWith(".SHX")) {
            filename = filename.substring(0, filename.length() - 4);
            shpext = ".SHP";
            dbfext = ".DBF";
            shxext = ".SHX";
            prjext = ".PRJ";
            xmlext = ".SHP.XML";
        }
        shpURL_ = new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), filename + shpext);
        dbfURL_ = new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), filename + dbfext);
        shxURL_ = new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), filename + shxext);
        prjURL_ = new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), filename + prjext);
        xmlURL_ = new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), filename + xmlext);
    }

    /**
   * this sets the datastore's namespace during construction (so the schema - FeatureType - will have the correct value)
   * You can call this with namespace = null, but I suggest you give it an actual namespace.
   * 
   * @param _url
   * @param _namespace
   */
    public ShapefileDataStore(final URL _url, final URI _namespace, final GeometryFactory _fact) throws java.net.MalformedURLException {
        this(_url);
        this.namespace_ = _namespace;
    }

    /**
   * this sets the datastore's namespace during construction (so the schema - FeatureType - will have the correct value)
   * You can call this with namespace = null, but I suggest you give it an actual namespace.
   * 
   * @param _url
   * @param _namespace
   * @param _useMemoryMapped
   */
    public ShapefileDataStore(final URL _url, final URI _namespace, final boolean _useMemoryMapped, final GeometryFactory _fact) throws java.net.MalformedURLException {
        this(_url);
        this.namespace_ = _namespace;
        this.useMemoryMappedBuffer_ = _useMemoryMapped;
    }

    /**
   * Latch onto xmlURL if it is there, we may be able to get out of calculating the bounding box!
   * <p>
   * This method is called by the createTypeEntry anonymous inner class DefaultTypeEntry.
   * </p>
   * 
   * @param _typeName DOCUMENT ME!
   * @return Map with xmlURL parsed, or an EMPTY_MAP.
   */
    protected Map createMetadata(final String _typeName) {
        if (xmlURL_ == null) {
            return Collections.EMPTY_MAP;
        }
        try {
            final ShpXmlFileReader reader = new ShpXmlFileReader(xmlURL_);
            final Map map = new HashMap();
            map.put("shp.xml", reader.parse());
            return map;
        } catch (final Throwable t) {
            FuLog.warning("Could not parse " + xmlURL_ + ':' + t.getLocalizedMessage());
            return Collections.EMPTY_MAP;
        }
    }

    /**
   * Determine if the location of this shapefile is local or remote.
   * 
   * @return true if local, false if remote
   */
    public boolean isLocal() {
        return shpURL_.getProtocol().equals("file");
    }

    /**
   * Delete existing files.
   */
    private void clear() {
        if (isLocal()) {
            delete(shpURL_);
            delete(dbfURL_);
            delete(shxURL_);
            delete(prjURL_);
            delete(xmlURL_);
        }
    }

    /**
   * Delete a URL (file).
   */
    private void delete(final URL _u) {
        final File f = new File(_u.getFile());
        f.delete();
    }

    /**
   * Obtain a ReadableByteChannel from the given URL. If the url protocol is file, a FileChannel will be returned.
   * Otherwise a generic channel will be obtained from the urls input stream.
   * 
   * @param _url DOCUMENT ME!
   * @return DOCUMENT ME!
   * @throws IOException DOCUMENT ME!
   */
    protected ReadableByteChannel getReadChannel(final URL _url) throws IOException {
        ReadableByteChannel channel = null;
        if (_url.getProtocol().equals("file")) {
            File file = null;
            if ((_url.getHost() != null) && !_url.getHost().equals("")) {
                file = new File(_url.getHost() + ":" + _url.getFile());
            } else {
                file = new File(_url.getFile());
            }
            if (!file.exists() || !file.canRead()) {
                throw new IOException("File either doesn't exist or is unreadable : " + file);
            }
            final FileInputStream in = new FileInputStream(file);
            channel = in.getChannel();
        } else {
            final InputStream in = _url.openConnection().getInputStream();
            channel = Channels.newChannel(in);
        }
        return channel;
    }

    /**
   * Obtain a WritableByteChannel from the given URL. If the url protocol is file, a FileChannel will be returned.
   * Currently, this method will return a generic channel for remote urls, however both shape and dbf writing can only
   * occur with a local FileChannel channel.
   * 
   * @param _url DOCUMENT ME!
   * @return DOCUMENT ME!
   * @throws IOException DOCUMENT ME!
   */
    protected WritableByteChannel getWriteChannel(final URL _url) throws IOException {
        WritableByteChannel channel;
        if (_url.getProtocol().equals("file")) {
            File file = null;
            if ((_url.getHost() != null) && !_url.getHost().equals("")) {
                file = new File(_url.getHost() + ':' + _url.getFile());
            } else {
                file = new File(_url.getFile());
            }
            final RandomAccessFile raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            ((FileChannel) channel).lock();
        } else {
            final OutputStream out = _url.openConnection().getOutputStream();
            channel = Channels.newChannel(out);
        }
        return channel;
    }

    /**
   * Create a FeatureReader for the provided type name.
   * 
   * @param _typeName The name of the FeatureType to create a reader for.
   * @return A new FeatureReader.
   * @throws IOException If an error occurs during creation
   */
    protected FeatureReader getFeatureReader(final String _typeName) throws IOException {
        typeCheck(_typeName);
        return getFeatureReader();
    }

    protected FeatureReader getFeatureReader() throws IOException {
        try {
            return createFeatureReader(getSchema().getTypeName(), getAttributesReader(true), schema_);
        } catch (final SchemaException se) {
            throw new DataSourceException("Error creating schema", se);
        }
    }

    /**
   * Just like the basic version, but adds a small optimization: if no attributes are going to be read, don't uselessly
   * open and read the dbf file.
   * 
   * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String, org.geotools.data.Query)
   */
    protected FeatureReader getFeatureReader(final String _typeName, final Query _query) throws IOException {
        final String[] propertyNames = _query.getPropertyNames();
        final String defaultGeomName = schema_.getDefaultGeometry().getName();
        if ((propertyNames != null) && (propertyNames.length == 1) && propertyNames[0].equals(defaultGeomName)) {
            try {
                final FeatureType newSchema = DataUtilities.createSubType(schema_, propertyNames);
                return createFeatureReader(_typeName, getAttributesReader(false), newSchema);
            } catch (final SchemaException se) {
                throw new DataSourceException("Error creating schema", se);
            }
        }
        return super.getFeatureReader(_typeName, _query);
    }

    protected FeatureReader createFeatureReader(final String _typeName, final Reader _r, final FeatureType _readerSchema) throws SchemaException {
        return new org.geotools.data.FIDFeatureReader(_r, new DefaultFIDReader(_typeName), _readerSchema);
    }

    /**
   * Returns the attribute reader, allowing for a pure shapefile reader, or a combined dbf/shp reader.
   * 
   * @param _readDbf - if true, the dbf fill will be opened and read
   * @return
   * @throws IOException
   */
    protected Reader getAttributesReader(boolean _readDbf) throws IOException {
        AttributeType[] atts = (schema_ == null) ? readAttributes() : schema_.getAttributeTypes();
        if (!_readDbf) {
            LOGGER.fine("The DBF file won't be opened since no attributes will be read from it");
            atts = new AttributeType[] { schema_.getDefaultGeometry() };
            return new Reader(atts, openShapeReader(), null);
        }
        return new Reader(atts, openShapeReader(), openDbfReader());
    }

    /**
   * Convenience method for opening a ShapefileReader.
   * 
   * @return A new ShapefileReader.
   * @throws IOException If an error occurs during creation.
   * @throws DataSourceException DOCUMENT ME!
   */
    protected ShapefileReader openShapeReader() throws IOException {
        final ReadableByteChannel rbc = getReadChannel(shpURL_);
        if (rbc == null) {
            return null;
        }
        try {
            return new ShapefileReader(rbc, true, useMemoryMappedBuffer_, readWriteLock_, GISGeometryFactory.INSTANCE);
        } catch (final ShapefileException se) {
            throw new DataSourceException("Error creating ShapefileReader", se);
        }
    }

    /**
   * Convenience method for opening a DbaseFileReader.
   * 
   * @return A new DbaseFileReader
   * @throws IOException If an error occurs during creation.
   */
    protected DbaseFileReader openDbfReader() throws IOException {
        final ReadableByteChannel rbc = getReadChannel(dbfURL_);
        if (rbc == null) {
            return null;
        }
        return new DbaseFileReader(rbc, useMemoryMappedBuffer_);
    }

    /**
   * Convenience method for opening a DbaseFileReader.
   * 
   * @return A new DbaseFileReader
   * @throws IOException If an error occurs during creation.
   * @throws FactoryException DOCUMENT ME!
   */
    protected PrjFileReader openPrjReader() throws IOException, FactoryException {
        ReadableByteChannel rbc = null;
        try {
            rbc = getReadChannel(prjURL_);
        } catch (final IOException e) {
            LOGGER.warning("projection (.prj) for shapefile not available");
        }
        if (rbc == null) {
            return null;
        }
        PrjFileReader prj = null;
        try {
            prj = new PrjFileReader(rbc);
        } catch (final Exception e) {
        } finally {
            rbc.close();
        }
        return prj;
    }

    /**
   * Get an array of type names this DataStore holds.<BR/>ShapefileDataStore will always return a single name.
   * 
   * @return An array of length one containing the single type held.
   */
    public String[] getTypeNames() {
        return new String[] { getCurrentTypeName() };
    }

    /**
   * Create the type name of the single FeatureType this DataStore represents.<BR/> For example, if the urls path is
   * file:///home/billy/mytheme.shp, the type name will be mytheme.
   * 
   * @return A name based upon the last path component of the url minus the extension.
   */
    protected String createFeatureTypeName() {
        final String path = shpURL_.getPath();
        final int slash = Math.max(0, path.lastIndexOf('/') + 1);
        int dot = path.indexOf('.', slash);
        if (dot < 0) {
            dot = path.length();
        }
        return path.substring(slash, dot);
    }

    protected String getCurrentTypeName() {
        return (schema_ == null) ? createFeatureTypeName() : schema_.getTypeName();
    }

    /**
   * A convenience method to check if a type name is correct.
   * 
   * @param _requested The type name requested.
   * @throws IOException If the type name is not available
   */
    protected void typeCheck(final String _requested) throws IOException {
        if (!getCurrentTypeName().equals(_requested)) {
            throw new IOException("No such type : " + _requested);
        }
    }

    /**
   * Create a FeatureWriter for the given type name.
   * 
   * @param _typeName The typeName of the FeatureType to write
   * @param _transaction DOCUMENT ME!
   * @return A new FeatureWriter.
   * @throws IOException If the typeName is not available or some other error occurs.
   */
    protected FeatureWriter createFeatureWriter(final String _typeName, final Transaction _transaction) throws IOException {
        typeCheck(_typeName);
        return new Writer(_typeName);
    }

    /**
   * Obtain the FeatureType of the given name. ShapefileDataStore contains only one FeatureType.
   * 
   * @param _typeName The name of the FeatureType.
   * @return The FeatureType that this DataStore contains.
   * @throws IOException If a type by the requested name is not present.
   */
    public FeatureType getSchema(final String _typeName) throws IOException {
        typeCheck(_typeName);
        return getSchema();
    }

    public FeatureType getSchema() throws IOException {
        if (schema_ == null) {
            try {
                final AttributeType[] types = readAttributes();
                FeatureType parent = null;
                final Class geomType = types[0].getType();
                if ((geomType == Point.class) || (geomType == MultiPoint.class)) {
                    parent = BasicFeatureTypes.POINT;
                } else if ((geomType == Polygon.class) || (geomType == MultiPolygon.class)) {
                    parent = BasicFeatureTypes.POLYGON;
                } else if ((geomType == LineString.class) || (geomType == MultiLineString.class)) {
                    parent = BasicFeatureTypes.LINE;
                }
                if (parent != null) {
                    schema_ = FeatureTypes.newFeatureType(readAttributes(), createFeatureTypeName(), namespace_, false, new FeatureType[] { parent });
                } else {
                    if (namespace_ != null) {
                        schema_ = FeatureTypes.newFeatureType(readAttributes(), createFeatureTypeName(), namespace_, false);
                    } else {
                        schema_ = FeatureTypes.newFeatureType(readAttributes(), createFeatureTypeName(), GMLSchema.NAMESPACE, false);
                    }
                }
            } catch (final SchemaException se) {
                throw new DataSourceException("Error creating FeatureType", se);
            }
        }
        return schema_;
    }

    /**
   * Create the AttributeTypes contained within this DataStore.
   * 
   * @return An array of new AttributeTypes
   * @throws IOException If AttributeType reading fails
   */
    protected AttributeType[] readAttributes() throws IOException {
        final ShapefileReader shp = openShapeReader();
        final DbaseFileReader dbf = openDbfReader();
        final AbstractCRS cs = null;
        try {
            final GeometryAttributeType geometryAttribute = (GeometryAttributeType) AttributeTypeFactory.newAttributeType("the_geom", JTSUtilities.findBestGeometryClass(shp.getHeader().getShapeType()), true, 0, null, cs);
            AttributeType[] atts;
            if (dbf != null) {
                final DbaseFileHeader header = dbf.getHeader();
                atts = new AttributeType[header.getNumFields() + 1];
                atts[0] = geometryAttribute;
                for (int i = 0, ii = header.getNumFields(); i < ii; i++) {
                    final Class clazz = header.getFieldClass(i);
                    atts[i + 1] = AttributeTypeFactory.newAttributeType(header.getFieldName(i), clazz, true, header.getFieldLength(i));
                }
            } else {
                atts = new AttributeType[] { geometryAttribute };
            }
            return atts;
        } finally {
            try {
                shp.close();
            } catch (final IOException ioe) {
            }
            try {
                if (dbf != null) {
                    dbf.close();
                }
            } catch (final IOException ioe) {
            }
        }
    }

    /**
   * Set the FeatureType of this DataStore. This method will delete any existing local resources or throw an IOException
   * if the DataStore is remote.
   * 
   * @param _featureType The desired FeatureType.
   * @throws IOException If the DataStore is remote.
   */
    public void createSchema(final FeatureType _featureType) throws IOException {
        if (!isLocal()) {
            throw new IOException("Cannot create FeatureType on remote shapefile");
        }
        clear();
        schema_ = _featureType;
        CoordinateReferenceSystem cs = _featureType.getDefaultGeometry().getCoordinateSystem();
        final long temp = System.currentTimeMillis();
        if (isLocal()) {
            final Class geomType = _featureType.getDefaultGeometry().getType();
            ShapeType shapeType;
            if (Point.class.isAssignableFrom(geomType)) {
                shapeType = ShapeType.POINT;
            } else if (MultiPoint.class.isAssignableFrom(geomType)) {
                shapeType = ShapeType.MULTIPOINT;
            } else if (LineString.class.isAssignableFrom(geomType) || MultiLineString.class.isAssignableFrom(geomType)) {
                shapeType = ShapeType.ARC;
            } else if (Polygon.class.isAssignableFrom(geomType) || MultiPolygon.class.isAssignableFrom(geomType)) {
                shapeType = ShapeType.POLYGON;
            } else {
                return;
            }
            final FileChannel shpChannel = (FileChannel) getWriteChannel(getStorageURL(shpURL_, temp));
            final FileChannel shxChannel = (FileChannel) getWriteChannel(getStorageURL(shxURL_, temp));
            ShapefileWriter writer = null;
            try {
                writer = new ShapefileWriter(shpChannel, shxChannel, readWriteLock_, GISGeometryFactory.INSTANCE);
                final Envelope env = new Envelope(-179, 179, -89, 89);
                Envelope transformedBounds;
                if (cs != null) {
                    try {
                        transformedBounds = JTS.transform(env, CRS.transform(DefaultGeographicCRS.WGS84, cs, true));
                    } catch (final Exception e) {
                        cs = null;
                        transformedBounds = env;
                    }
                } else {
                    transformedBounds = env;
                }
                writer.writeHeaders(transformedBounds, shapeType, 0, 100);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
            final DbaseFileHeader dbfheader = createDbaseHeader(_featureType);
            dbfheader.setNumRecords(0);
            final WritableByteChannel writeChannel = getWriteChannel(getStorageURL(dbfURL_, temp));
            try {
                dbfheader.writeHeader(writeChannel);
            } finally {
                writeChannel.close();
            }
        }
        if (cs != null) {
            final String s = cs.toWKT();
            final FileWriter out = new FileWriter(getStorageFile(prjURL_, temp));
            try {
                out.write(s);
            } finally {
                out.close();
            }
        }
        copyAndDelete(shpURL_, temp);
        copyAndDelete(shxURL_, temp);
        copyAndDelete(dbfURL_, temp);
        if (!prjURL_.getPath().equals("")) {
            try {
                copyAndDelete(prjURL_, temp);
            } catch (final FileNotFoundException e) {
                LOGGER.warning(".prj could not be created.");
            }
        }
    }

    /**
   * Gets the bounding box of the file represented by this data store as a whole (that is, off all of the features in
   * the shapefile).
   * 
   * @return The bounding box of the datasource or null if unknown and too expensive for the method to calculate.
   * @throws DataSourceException DOCUMENT ME!
   */
    protected Envelope getBounds() throws DataSourceException {
        ReadableByteChannel in = null;
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(100);
            in = getReadChannel(shpURL_);
            in.read(buffer);
            buffer.flip();
            final ShapefileHeader header = new ShapefileHeader();
            header.read(buffer, true);
            final Envelope env = new Envelope(header.minX(), header.maxX(), header.minY(), header.maxY());
            if (schema_ != null) {
                return new ReferencedEnvelope(env, schema_.getDefaultGeometry().getCoordinateSystem());
            }
            return new ReferencedEnvelope(env, null);
        } catch (final IOException ioe) {
            throw new DataSourceException("Problem getting Bbox", ioe);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ioe) {
            }
        }
    }

    protected Envelope getBounds(final Query _query) throws IOException {
        if (_query.getFilter().equals(Filter.NONE)) {
            return getBounds();
        }
        return null;
    }

    public FeatureSource getFeatureSource(final String _typeName) throws IOException {
        final FeatureType featureType = getSchema(_typeName);
        if (isWriteable) {
            if (getLockingManager() != null) {
                return new AbstractFeatureLocking() {

                    public DataStore getDataStore() {
                        return ShapefileDataStore.this;
                    }

                    public void addFeatureListener(final FeatureListener _listener) {
                        listenerManager.addFeatureListener(this, _listener);
                    }

                    public void removeFeatureListener(final FeatureListener _listener) {
                        listenerManager.removeFeatureListener(this, _listener);
                    }

                    public FeatureType getSchema() {
                        return featureType;
                    }

                    public Envelope getBounds(final Query _query) throws IOException {
                        return ShapefileDataStore.this.getBounds(_query);
                    }
                };
            }
            return new AbstractFeatureStore() {

                public DataStore getDataStore() {
                    return ShapefileDataStore.this;
                }

                public void addFeatureListener(final FeatureListener _listener) {
                    listenerManager.addFeatureListener(this, _listener);
                }

                public void removeFeatureListener(final FeatureListener _listener) {
                    listenerManager.removeFeatureListener(this, _listener);
                }

                public FeatureType getSchema() {
                    return featureType;
                }

                public Envelope getBounds(final Query _query) throws IOException {
                    return ShapefileDataStore.this.getBounds(_query);
                }
            };
        }
        return new AbstractFeatureSource() {

            public DataStore getDataStore() {
                return ShapefileDataStore.this;
            }

            public void addFeatureListener(final FeatureListener _listener) {
                listenerManager.addFeatureListener(this, _listener);
            }

            public void removeFeatureListener(final FeatureListener _listener) {
                listenerManager.removeFeatureListener(this, _listener);
            }

            public FeatureType getSchema() {
                return featureType;
            }

            public Envelope getBounds(final Query _query) throws IOException {
                return ShapefileDataStore.this.getBounds(_query);
            }
        };
    }

    /**
   * @see org.geotools.data.AbstractDataStore#getCount(org.geotools.data.Query)
   */
    protected int getCount(final Query _query) throws IOException {
        if (_query.getFilter() == Filter.NONE) {
            ShapefileReader reader = null;
            int count = -1;
            try {
                reader = new ShapefileReader(getReadChannel(shpURL_), readWriteLock_, GISGeometryFactory.INSTANCE);
                count = reader.getCount(count);
            } catch (final IOException e) {
                throw e;
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (final IOException ioe) {
                }
            }
            return count;
        }
        return super.getCount(_query);
    }

    /**
   * Attempt to create a DbaseFileHeader for the FeatureType. Note, we cannot set the number of records until the write
   * has completed.
   * 
   * @param _featureType DOCUMENT ME!
   * @return DOCUMENT ME!
   * @throws IOException DOCUMENT ME!
   * @throws DbaseFileException DOCUMENT ME!
   */
    protected static DbaseFileHeader createDbaseHeader(final FeatureType _featureType) throws IOException {
        final DbaseFileHeader header = new DbaseFileHeader();
        for (int i = 0, ii = _featureType.getAttributeCount(); i < ii; i++) {
            final AttributeType type = _featureType.getAttributeType(i);
            final Class colType = type.getType();
            final String colName = type.getName();
            int fieldLen = -1;
            final Filter f = type.getRestriction();
            if ((f != null) && (f != Filter.ALL) && (f != Filter.NONE) && ((f.getFilterType() == FilterType.COMPARE_LESS_THAN) || (f.getFilterType() == FilterType.COMPARE_LESS_THAN_EQUAL))) {
                try {
                    final CompareFilter cf = (CompareFilter) f;
                    if (cf.getLeftValue() instanceof LengthFunction) {
                        fieldLen = Integer.parseInt(((LiteralExpression) cf.getRightValue()).getLiteral().toString());
                    } else {
                        if (cf.getRightValue() instanceof LengthFunction) {
                            fieldLen = Integer.parseInt(((LiteralExpression) cf.getLeftValue()).getLiteral().toString());
                        }
                    }
                } catch (final NumberFormatException e) {
                    fieldLen = 256;
                }
            } else {
                fieldLen = 256;
            }
            if (fieldLen <= 0) {
                fieldLen = 255;
            }
            if ((colType == Integer.class) || (colType == Short.class) || (colType == Byte.class)) {
                header.addColumn(colName, 'N', Math.min(fieldLen, 9), 0);
            } else if (colType == Long.class) {
                header.addColumn(colName, 'N', Math.min(fieldLen, 19), 0);
            } else if ((colType == Double.class) || (colType == Float.class) || (colType == Number.class)) {
                final int l = Math.min(fieldLen, 33);
                final int d = Math.max(l - 2, 0);
                header.addColumn(colName, 'N', l, d);
            } else if (java.util.Date.class.isAssignableFrom(colType)) {
                header.addColumn(colName, 'D', fieldLen, 0);
            } else if (colType == Boolean.class) {
                header.addColumn(colName, 'L', 1, 0);
            } else if (CharSequence.class.isAssignableFrom(colType)) {
                header.addColumn(colName, 'C', Math.min(254, fieldLen), 0);
            } else if (Geometry.class.isAssignableFrom(colType)) {
                continue;
            } else {
                throw new IOException("Unable to write : " + colType.getName());
            }
        }
        return header;
    }

    /**
   * Get a temporary URL for storage based on the one passed in.
   */
    protected URL getStorageURL(final URL _url, final long _temp) throws java.net.MalformedURLException {
        return (_temp == 0) ? _url : getStorageFile(_url, _temp).toURL();
    }

    /**
   * Get a temproray File based on the URL passed in.
   */
    protected File getStorageFile(final URL _url, final long _temp) {
        String f = _url.getFile();
        f = _temp + f.substring(f.lastIndexOf('/') + 1);
        final File tf = new File(System.getProperty("java.io.tmpdir"), f);
        return tf;
    }

    /**
   * Copy the file at the given URL to the original.
   */
    protected void copyAndDelete(final URL _src, final long _temp) throws IOException {
        final File storage = getStorageFile(_src, _temp);
        final File dest = new File(_src.getFile());
        FileChannel in = null;
        FileChannel out = null;
        if (storage.equals(dest)) {
            return;
        }
        try {
            readWriteLock_.lockWrite();
            if (dest.exists()) {
                dest.delete();
            }
            if (storage.exists() && !storage.renameTo(dest)) {
                in = new FileInputStream(storage).getChannel();
                out = new FileOutputStream(dest).getChannel();
                final long len = in.size();
                final long copied = out.transferFrom(in, 0, in.size());
                if (len != copied) {
                    throw new IOException("unable to complete write");
                }
            }
        } finally {
            readWriteLock_.unlockWrite();
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
            storage.delete();
        }
    }

    void throwIOExc() throws IOException {
        throw new IOException("Writer closed");
    }

    /**
   * An AttributeReader implementation for Shapefile. Pretty straightforward. <BR/>The default geometry is at position
   * 0, and all dbf columns follow. <BR/>The dbf file may not be necessary, if not, just pass null as the
   * DbaseFileReader
   */
    protected static class Reader extends AbstractAttributeIO implements AttributeReader {

        protected ShapefileReader shp_;

        protected DbaseFileReader dbf_;

        protected DbaseFileReader.Row row_;

        protected ShapefileReader.Record record_;

        /**
     * Create the shapefile reader.
     * 
     * @param _atts - the attributes that we are going to read.
     * @param _shp - the shapefile reader, required
     * @param _dbf - the dbf file reader. May be null, in this case no attributes will be read from the dbf file
     */
        public Reader(final AttributeType[] _atts, final ShapefileReader _shp, final DbaseFileReader _dbf) {
            super(_atts);
            this.shp_ = _shp;
            this.dbf_ = _dbf;
        }

        public void close() throws IOException {
            try {
                if (shp_ != null) {
                    shp_.close();
                }
                if (dbf_ != null) {
                    dbf_.close();
                }
            } finally {
                row_ = null;
                record_ = null;
                shp_ = null;
                dbf_ = null;
            }
        }

        public boolean hasNext() throws IOException {
            int n = shp_.hasNext() ? 1 : 0;
            if (dbf_ != null) {
                n += (dbf_.hasNext() ? 2 : 0);
            }
            if ((n == 3) || ((n == 1) && (dbf_ == null))) {
                return true;
            }
            if (n == 0) {
                return false;
            }
            throw new IOException(((n == 1) ? "Shp" : "Dbf") + " has extra record");
        }

        public void next() throws IOException {
            record_ = shp_.nextRecord();
            if (dbf_ != null) {
                row_ = dbf_.readRow();
            }
        }

        public Object read(final int _param) throws IOException, java.lang.ArrayIndexOutOfBoundsException {
            switch(_param) {
                case 0:
                    return record_.shape();
                default:
                    if (row_ != null) {
                        return row_.read(_param - 1);
                    }
                    return null;
            }
        }
    }

    /**
   * A FeatureWriter for ShapefileDataStore. Uses a write and annotate technique to avoid buffering attributes and
   * geometries. Because the shapefile and dbf require header information which can only be obtained by reading the
   * entire series of Features, the headers are updated after the initial write completes.
   */
    protected class Writer implements FeatureWriter {

        private long temp_;

        protected FeatureReader featureReader_;

        protected Reader attReader_;

        private Feature currentFeature_;

        private FeatureType featureType_;

        private Object[] emptyAtts_;

        private Object[] transferCache_;

        private ShapeType shapeType_;

        private ShapeHandler handler_;

        private int shapefileLength_ = 100;

        private int records_;

        private byte[] writeFlags_;

        private ShapefileWriter shpWriter_;

        private DbaseFileWriter dbfWriter_;

        private DbaseFileHeader dbfHeader_;

        private FileChannel dbfChannel_;

        private final Envelope bounds_ = new Envelope();

        public Writer(final String _typeName) throws IOException {
            try {
                attReader_ = getAttributesReader(true);
                featureReader_ = createFeatureReader(_typeName, attReader_, schema_);
                temp_ = System.currentTimeMillis();
            } catch (final Exception e) {
                getSchema();
                if (schema_ == null) {
                    throw new IOException("To create a shapefile, you must first call createSchema()");
                }
                featureReader_ = new EmptyFeatureReader(schema_);
                temp_ = 0;
            }
            this.featureType_ = featureReader_.getFeatureType();
            emptyAtts_ = new Object[featureType_.getAttributeCount()];
            writeFlags_ = new byte[featureType_.getAttributeCount()];
            int cnt = 0;
            for (int i = 0, ii = featureType_.getAttributeCount(); i < ii; i++) {
                if (!(featureType_.getAttributeType(i) instanceof GeometryAttributeType)) {
                    cnt++;
                    writeFlags_[i] = (byte) 1;
                }
            }
            transferCache_ = new Object[cnt];
            shpWriter_ = new ShapefileWriter((FileChannel) getWriteChannel(getStorageURL(shpURL_, temp_)), (FileChannel) getWriteChannel(getStorageURL(shxURL_, temp_)), readWriteLock_, GISGeometryFactory.INSTANCE);
            dbfChannel_ = (FileChannel) getWriteChannel(getStorageURL(dbfURL_, temp_));
            dbfHeader_ = createDbaseHeader(featureType_);
            dbfWriter_ = new DbaseFileWriter(dbfHeader_, dbfChannel_);
        }

        /**
     * Go back and update the headers with the required info.
     * 
     * @throws IOException DOCUMENT ME!
     */
        protected void flush() throws IOException {
            if ((records_ <= 0) && (shapeType_ == null)) {
                final GeometryAttributeType geometryAttributeType = featureType_.getDefaultGeometry();
                final Class gat = geometryAttributeType.getType();
                shapeType_ = JTSUtilities.getShapeType(gat);
            }
            shpWriter_.writeHeaders(bounds_, shapeType_, records_, shapefileLength_);
            dbfHeader_.setNumRecords(records_);
            dbfChannel_.position(0);
            dbfHeader_.writeHeader(dbfChannel_);
        }

        /**
     * In case someone doesn't close me.
     * 
     * @throws Throwable DOCUMENT ME!
     */
        protected void finalize() throws Throwable {
            if (featureReader_ != null) {
                try {
                    close();
                } catch (final Exception e) {
                }
            }
        }

        /**
     * Clean up our temporary write if there was one.
     * 
     * @throws IOException DOCUMENT ME!
     */
        protected void clean() throws IOException {
            if (temp_ == 0) {
                return;
            }
            copyAndDelete(shpURL_, temp_);
            copyAndDelete(shxURL_, temp_);
            copyAndDelete(dbfURL_, temp_);
        }

        /**
     * Release resources and flush the header information.
     * 
     * @throws IOException DOCUMENT ME!
     */
        public void close() throws IOException {
            if (featureReader_ == null) {
                throwIOExc();
            }
            if (currentFeature_ != null) {
                write();
            }
            if (attReader_ != null) {
                shapeType_ = attReader_.shp_.getHeader().getShapeType();
                handler_ = shapeType_.getShapeHandler(GISGeometryFactory.INSTANCE);
                if (records_ == 0) {
                    shpWriter_.writeHeaders(bounds_, shapeType_, 0, 0);
                }
                final double[] env = new double[4];
                while (attReader_.hasNext()) {
                    shapefileLength_ += attReader_.shp_.transferTo(shpWriter_, ++records_, env);
                    bounds_.expandToInclude(env[0], env[1]);
                    bounds_.expandToInclude(env[2], env[3]);
                    attReader_.dbf_.transferTo(dbfWriter_);
                }
            }
            try {
                featureReader_.close();
            } finally {
                try {
                    flush();
                } finally {
                    shpWriter_.close();
                    dbfWriter_.close();
                    dbfChannel_.close();
                }
                featureReader_ = null;
                shpWriter_ = null;
                dbfWriter_ = null;
                dbfChannel_ = null;
                clean();
            }
        }

        public org.geotools.feature.FeatureType getFeatureType() {
            return featureType_;
        }

        public boolean hasNext() throws IOException {
            if (featureReader_ == null) {
                throwIOExc();
            }
            return featureReader_.hasNext();
        }

        public org.geotools.feature.Feature next() throws IOException {
            if (featureReader_ == null) {
                throwIOExc();
            }
            if (currentFeature_ != null) {
                write();
            }
            if (featureReader_.hasNext()) {
                try {
                    return currentFeature_ = featureReader_.next();
                } catch (final IllegalAttributeException iae) {
                    throw new DataSourceException("Error in reading", iae);
                }
            }
            try {
                return currentFeature_ = DataUtilities.template(getFeatureType(), emptyAtts_);
            } catch (final IllegalAttributeException iae) {
                throw new DataSourceException("Error creating empty Feature", iae);
            }
        }

        public void remove() throws IOException {
            if (featureReader_ == null) {
                throwIOExc();
            }
            if (currentFeature_ == null) {
                throw new IOException("Current feature is null");
            }
            currentFeature_ = null;
        }

        public void write() throws IOException {
            if (currentFeature_ == null) {
                throw new IOException("Current feature is null");
            }
            if (featureReader_ == null) {
                throwIOExc();
            }
            Geometry g = currentFeature_.getDefaultGeometry();
            if (shapeType_ == null) {
                final int dims = JTSUtilities.guessCoorinateDims(g.getCoordinates());
                try {
                    shapeType_ = JTSUtilities.getShapeType(g, dims);
                    shpWriter_.writeHeaders(new Envelope(), shapeType_, 0, 0);
                    handler_ = shapeType_.getShapeHandler(GISGeometryFactory.INSTANCE);
                } catch (final ShapefileException se) {
                    throw new RuntimeException("Unexpected Error", se);
                }
            }
            g = JTSUtilities.convertToCollection(g, shapeType_);
            final Envelope b = g.getEnvelopeInternal();
            if (!b.isNull()) {
                bounds_.expandToInclude(b);
            }
            shapefileLength_ += (handler_.getLength(g) + 8);
            shpWriter_.writeGeometry(g);
            int idx = 0;
            for (int i = 0, ii = featureType_.getAttributeCount(); i < ii; i++) {
                if (writeFlags_[i] > 0) {
                    transferCache_[idx++] = currentFeature_.getAttribute(i);
                }
            }
            dbfWriter_.write(transferCache_);
            records_++;
            currentFeature_ = null;
        }
    }
}
