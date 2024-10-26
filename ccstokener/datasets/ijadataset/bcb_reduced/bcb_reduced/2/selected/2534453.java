package ti.targetinfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.GZIPInputStream;
import org.eclipse.core.runtime.IConfigurationElement;
import oscript.data.OJavaException;
import ti.event.EventSeries;
import ti.event.EventUtil;
import ti.exceptions.FatalError;
import ti.io.UDataInputStream;
import ti.io.UDataOutputStream;
import ti.mcore.Environment;
import ti.mcore.EventListeners;
import ti.mcore.u.FileUtil;
import ti.mcore.u.PluginUtil;
import ti.mcore.u.io.BigEndianBufferAccessor;
import ti.mcore.u.io.BufferAccessor;
import ti.mcore.u.io.LittleEndianBufferAccessor;
import ti.mcore.u.io.LocalBuffer;
import ti.mcore.u.log.PlatoLogger;
import ti.series.disk.Header;
import ti.sutc.SUTConnection;
import ti.sutc.feature.Modular;
import ti.sutc.feature.VersionInfo;
import ti.targetinfo.symtable.SymbolTableDatabase;
import ti.targetinfo.td.PaddingRule;
import ti.targetinfo.td.TypeInfo;

/**
 * This class serves to encapsulate all information that is specific to the
 * the software build running on a particular core in a target that we are
 * connected to (or have been connected to at the time the logfile was
 * generated).  This includes the CCD and {@link TypeInfo} tables for 
 * decoding complex types (ie. structs/unions/air-messages), symbol tables 
 * for accessing variables and calling functions in that core, etc.
 * 
 * @author Rob Clark
 */
public class TargetInfo {

    private static final PlatoLogger LOGGER = PlatoLogger.getLogger(TargetInfo.class);

    /** supports external databases stored by TargetInfo */
    private static final byte EXTERNAL_DATABASE_SUPPORTED_VERSION = 0x01;

    /** supports compressed headers */
    private static final byte GZIP_FORMAT_SUPPORTED_VERSION = 0x02;

    /**
   * the current file format version
   */
    private static byte VERSION = GZIP_FORMAT_SUPPORTED_VERSION;

    /**
   * The containing series.
   */
    private EventSeries series;

    /**
   * The node-id of the core that this object contains tables/info for.
   */
    private short nodeId;

    /**
   * Are headers stored with gzip compression?
   */
    private boolean compressed = true;

    /**
   * The name of the core that this object is created for. 
   */
    private String sutcName = "unknown";

    /**
   * Are we currently in the middle of loading a database?
   */
    private boolean loading = false;

    private transient BufferAccessor bufferAccessor;

    private transient SUTConnection sutc;

    private PaddingRule paddingRule = new ti.targetinfo.td.DefaultPaddingRule();

    private boolean bigEndian;

    private transient EventListeners<TargetInfoListener> typeDescriptorTableListeners = new EventListeners<TargetInfoListener>();

    /**
   * Maps current database class name to old database class name, for backwards compatibility
   */
    private static transient Map<String, String> oldTypeNameTable = new HashMap<String, String>();

    /**
   * Maps a database class name to a series header name.
   * <p>
   * Note the reason to use the class name, rather than the class, is that 
   * the class might actually be provided by a different plugin, which 
   * ti.mcore does not have an explicit dependency on
   */
    private Map<String, String> dbHeaderTable = new Hashtable<String, String>();

    /**
   * maps a database class name to a {@link Database} object in memory or
   * Boolean.FALSE for negative cache.
   */
    private transient Map<String, Object> dbCachedTable = new HashMap<String, Object>();

    /**
   * The following constants are used as keys for mapping the attributes associated with this object.
   */
    public static final String INITIAL = "timestamp.initial";

    public static final String PC_TIME = "timestamp.pctime";

    public static final String UNIT = "timestamp.unit";

    public static final String BUILD_NAME = "build.name";

    public static final String BUILD_DATE = "build.date";

    public static final String BUILD_TIME = "build.time";

    /**
   * Class constructor.  This constructor will automatically deserialize
   * itself from the corresponding series header, if it exists.
   * 
   * @param series        the containing {@link EventSeries}
   * @param nodeId        the node-id of the node that this class has info about
   */
    public TargetInfo(final EventSeries series, final short nodeId) {
        LOGGER.dbg("reload target info: " + nodeId);
        this.series = series;
        this.nodeId = nodeId;
        try {
            Header header = series.getHeader(nodeId + ".tinfo", false);
            if (header != null) {
                InputStream is = header.getInputStream();
                UDataInputStream udis = new UDataInputStream(is);
                long tt = System.currentTimeMillis();
                readExternal(udis);
                is.close();
                LOGGER.dbg("time taken to read targetinfo: " + (System.currentTimeMillis() - tt));
                udis = null;
                is = null;
            } else {
                flush();
            }
        } catch (EOFException e) {
            LOGGER.dbg(e.getMessage() + "reload target info: " + nodeId);
        } catch (Throwable e) {
            LOGGER.logError(e);
        } finally {
            synchronized (TargetInfo.this) {
                TargetInfo.this.notifyAll();
            }
        }
        synchronized (targetInfosTable) {
            targetInfosTable.put(this, null);
        }
        getBuildId();
    }

    /**
   * This method is used to update the <code>TargetInfo</code> with information
   * from a live connection.
   */
    public void registerConnection(SUTConnection sutc) {
        this.sutc = sutc;
        sutcName = sutc.getName();
        try {
            String paddingRuleName = sutc.getPaddingRule(nodeId);
            if (paddingRuleName == null) return;
            Class<? extends PaddingRule> prc = (Class<? extends PaddingRule>) Class.forName("ti.targetinfo.td." + sutc.getPaddingRule(nodeId));
            this.paddingRule = prc.newInstance();
            this.bigEndian = sutc.isBigEndian(nodeId);
        } catch (Exception e) {
            LOGGER.logError(e);
        }
        LOGGER.dbg("target info: " + this);
        flush();
        AttributeTable attr = (AttributeTable) getDatabase(AttributeTable.class);
        String buildId = sutc.getBuildId(nodeId);
        if (buildId != null) attr.put("BUILD_ID", buildId);
        VersionInfo vi = (VersionInfo) ((Modular) sutc).getModule(nodeId, VersionInfo.class);
        if (vi != null) {
            attr.put(BUILD_NAME, vi.getName());
            attr.put(BUILD_DATE, vi.getBuildDate());
            attr.put(BUILD_TIME, vi.getBuildTime());
        }
        if (buildId != null) series.getDatabaseManager().autoLoadDatabases(this);
    }

    /**
   * Return the collection of databases that are actually loaded
   */
    public Collection<Database> getDatabases() {
        waitToBeLoaded();
        LinkedList<Database> list = new LinkedList<Database>();
        synchronized (dbCachedTable) {
            for (Map.Entry<String, Object> entry : dbCachedTable.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Database) {
                    list.add((Database) value);
                }
            }
        }
        return list;
    }

    /**
   * Is the database of the specified type loaded?
   */
    public boolean isLoaded(Class<Database> type) {
        return dbHeaderTable.containsKey(type.getName());
    }

    /**
   * Get a particular database type.  Will return <code>null</code>
   * if the database is not yet loaded.
   */
    public Database getDatabase(Class<? extends Database> type) {
        return getDatabase(type.getName());
    }

    public Database getDatabase(final String type) {
        try {
            Database database = getLoadedDatabase(type);
            if (database == null) database = getUnloadedDatabase(type);
            return database;
        } catch (IOException e) {
            return null;
        }
    }

    /**
   * Get a database which might not be in memory.  This can complete 
   * asynchronously if called from the UI thread (to avoid the UI becoming 
   * unresponsive), in which case it will return <code>null</code> immediately
   * but later calls to {@link #getLoadedDatabase(String)} will succeed once
   * the asynchronous load-from-disk completes.
   */
    private Database getUnloadedDatabase(final String type) throws IOException {
        waitToBeLoaded();
        Database database = getLoadedDatabase(type);
        if (database == null) {
            final DatabaseFactoryInfo df = getDatabaseFactoryInfo(type);
            if (df == null) {
                LOGGER.logError("warning, unknown DatabaseFactory");
                return null;
            }
            if (loading) return null;
            final String header = getHeaderName(type);
            final Header sh = (header == null) ? null : series.getHeader(header, false);
            if (sh == null) return null;
            begunLoading(df.toString());
            LOGGER.dbg("restoring: " + type);
            Environment.getEnvironment().run(new Runnable() {

                public void run() {
                    try {
                        InputStream is = sh.getInputStream();
                        UDataInputStream udis = null;
                        if (compressed) udis = new UDataInputStream(new GZIPInputStream(is)); else udis = new UDataInputStream(is);
                        Database database = df.loadDatabase(TargetInfo.this, udis);
                        udis.close();
                        is.close();
                        udis = null;
                        is = null;
                        if (database != null) {
                            series.getDatabaseManager().shareDatabase(header, database);
                            synchronized (dbCachedTable) {
                                dbCachedTable.put(type, database);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.logError(e);
                        try {
                            OutputStream os = sh.getOutputStream(false);
                            os.flush();
                            os.close();
                        } catch (Exception e2) {
                            LOGGER.logError(e2);
                        }
                        synchronized (dbCachedTable) {
                            dbCachedTable.put(type, Boolean.FALSE);
                        }
                        LOGGER.logError("Unable to load the database for " + type);
                    } finally {
                        System.err.println("done loading: " + type);
                        finishedLoading();
                    }
                }
            }, "loading database: " + ((df != null) ? df.getDescription() : ""));
            Object obj = dbCachedTable.get(type);
            if (obj == Boolean.FALSE) database = null; else database = (Database) obj;
        }
        return database;
    }

    /** 
   * Get a database that is already in memory, or which can be loaded 
   * immediately (see {@link CacheDatabaseFactory} and 
   * {@link AutoloadDatabaseFactory})
   * <p>
   * This will not block for an unreasonable amount of time (ie.
   * file I/O) so it is safe to call from UI thread.
   */
    private Database getLoadedDatabase(String type) throws IOException {
        Object obj = dbCachedTable.get(type);
        if (obj == Boolean.FALSE) return null;
        Database database = (Database) obj;
        if (database != null) return database;
        final DatabaseFactoryInfo df = getDatabaseFactoryInfo(type);
        if (df == null) {
            LOGGER.dbg("warning, unknown DatabaseFactory");
            return null;
        }
        if (df.isCacheDb()) {
            database = df.loadDatabase(this);
            synchronized (dbCachedTable) {
                dbCachedTable.put(type, database);
            }
            return database;
        }
        String header = getHeaderName(type);
        if (header != null) {
            database = series.getDatabaseManager().getSharedDatabase(header);
            if (database != null) {
                LOGGER.dbg("loaded shared db: " + type + " -> " + header);
                synchronized (dbCachedTable) {
                    dbCachedTable.put(type, database);
                }
                return database;
            }
        }
        final Header sh = (header == null) ? null : series.getHeader(header, false);
        if (sh == null) {
            if (df.isAutoLoadDb()) {
                database = df.loadDatabase(this);
                if (database != null) {
                    synchronized (dbCachedTable) {
                        dbCachedTable.put(type, database);
                    }
                    dbHeaderTable.put(type, getNonSharedHeaderName(df.getExtension()));
                }
                return database;
            } else {
                dbHeaderTable.remove(type);
                synchronized (dbCachedTable) {
                    dbCachedTable.put(type, Boolean.FALSE);
                }
                return null;
            }
        }
        return null;
    }

    /** 
   * utility to get header name, if the specified db type is already 
   * loaded.  Manages backwards compatibility (if db type has changed).
   * Returns <code>null</code> if the specified db is not in
   * {@link #dbHeaderTable}
   */
    private String getHeaderName(String type) {
        String header = dbHeaderTable.get(type);
        if (header == null) {
            String oldHeaderType = oldTypeNameTable.get(type);
            if (oldHeaderType != null) {
                header = dbHeaderTable.get(oldHeaderType);
                if (header != null) {
                    dbHeaderTable.remove(oldHeaderType);
                    dbHeaderTable.put(type, header);
                }
            }
        }
        return header;
    }

    /**
   * Remove a database from the series this object contains.
   * @param type    the class of the database to remove
   */
    public void unloadDatabase(String type) {
        waitToBeLoaded();
        DatabaseFactoryInfo df = getDatabaseFactoryInfo(type);
        series.getDatabaseManager().unloadDatabase(TargetInfo.this, df);
        dbCachedTable.remove(type);
        dbHeaderTable.remove(type);
        flushCoreInfo(this.getSeries());
        fireTargetInfoEvent("unloadDatabase");
    }

    /**
   * Provided for backwards compatibility
   * @deprecated use {@link #loadDatabase(URL)}
   */
    public final void loadDatabase(File f) {
        loadDatabase(FileUtil.getURL(f));
    }

    /**
   * Load a database from a file specified.  It is expected that there is 
   * {@link DatabaseFactory}  registered before loading the database file.
   * 
   * @param url   a database file.  
   */
    public void loadDatabase(URL url) {
        _loadDatabase(url);
    }

    /**
   * As a special extension, for db's which in turn manage other child
   * db's, the <code>url</code> can use a query string to further
   * qualify the db type.  This ensures that the new db will not override
   * another db of the same type.  In this case, however, the db will not
   * be returned by calls to {@link #getDatabase(Class)}, however it will
   * be cached, shared (provided the query string matches), and persisted
   * normally.
   * <p>
   * If in doubt, you probably don't want to use this method.. use
   * {@link #loadDatabase(URL)} instead
   * 
   * @return the type value, which should be treated as opaque but can
   *   be used at any point later to retrieve the db by passing to
   *   {@link #getDatabase(String)}
   */
    public String _loadDatabase(URL url) {
        LOGGER.dbg("loading: " + url);
        final DatabaseFactoryInfo df = DatabaseFactoryInfo.getDatabaseFactoryInfo(url);
        if (df == null) {
            LOGGER.logError("No database factory found for " + url);
            return null;
        }
        URLConnection con;
        String header;
        String type;
        try {
            con = url.openConnection();
            header = DatabaseManager.getSharedHeaderName(con, compressed, false);
            type = getDatabaseType(df, url);
            if (loadSharedDatabase(df, con, header)) return type;
        } catch (IOException e) {
            LOGGER.logError(e);
            return null;
        }
        Database db = null;
        try {
            begunLoading(df.toString());
            db = series.getDatabaseManager().loadDatabase(this, df, con);
            if (db instanceof PrivateDatabase) header = getNonSharedHeaderName(header);
            if (db != null) {
                dbHeaderTable.put(type, header);
                synchronized (dbCachedTable) {
                    dbCachedTable.put(type, db);
                }
            }
        } catch (Exception e) {
            Environment.getEnvironment().showErrorMessage("Failed to load " + url + " " + e.getMessage());
            e.printStackTrace();
            LOGGER.logError(e);
        } finally {
            finishedLoading();
            if (db != null) {
                flush(series, db);
                series.getDatabaseManager().shareDatabase(header, db);
            }
        }
        return type;
    }

    /**
   * As a special extension, for db's which in turn manage other child
   * db's.. don't count on this API staying here, since I'd like to find
   * a cleaner way..
   */
    public void _fireTargetInfoEvent(String reason) {
        fireTargetInfoEvent(reason);
    }

    /** 
   * called by {@link DatabaseManager} when it has automatically found a 
   * matching database file from a previous session (based on the build-id)
   */
    DatabaseFactoryInfo autoLoadDatabase(URL url, UDataInputStream in) throws IOException {
        DatabaseFactoryInfo df = DatabaseFactoryInfo.getDatabaseFactoryInfo(url);
        URLConnection con = url.openConnection();
        String header = DatabaseManager.getSharedHeaderName(con, compressed, false);
        String type = getDatabaseType(df, url);
        if (loadSharedDatabase(df, con, header)) return df;
        Database db = null;
        try {
            begunLoading(df.toString());
            db = df.loadDatabase(this, in);
            dbHeaderTable.put(type, header);
            synchronized (dbCachedTable) {
                dbCachedTable.put(type, db);
            }
        } finally {
            finishedLoading();
            if (db != null) {
                flush(series, db);
                series.getDatabaseManager().shareDatabase(header, db);
            }
        }
        return df;
    }

    private boolean loadSharedDatabase(DatabaseFactoryInfo df, URLConnection con, String header) throws IOException {
        String type = getDatabaseType(df, con.getURL());
        Header sh = series.getHeader(header, false);
        if ((sh != null) && (sh.getLength() > 0)) {
            dbHeaderTable.put(type, header);
            dbCachedTable.remove(type);
            LOGGER.dbg("already loaded " + type + " -> " + header);
            flushCoreInfo(series);
            finishedLoading();
            series.getDatabaseManager().loadSharedDatabase(this, df, con);
            return true;
        }
        return false;
    }

    /**
   * Helper to deal with types which have a query string.. use this rather
   * than calling {@link DatabaseFactoryInfo#getDatabaseType()} directly
   */
    private static String getDatabaseType(DatabaseFactoryInfo df, URL url) {
        String type = df.getDatabaseType();
        String query = url.getQuery();
        if (query != null) type += "?" + query;
        return type;
    }

    /**
   * Helper to deal with types which have a query string to further qualify 
   * the type.. use this rather than calling {@link DatabaseFactoryInfo#getDatabaseFactoryInfo(String)}
   * directly
   */
    private static DatabaseFactoryInfo getDatabaseFactoryInfo(String type) {
        int idx = type.indexOf('?');
        if (idx != -1) type = type.substring(0, idx);
        return DatabaseFactoryInfo.getDatabaseFactoryInfo(type);
    }

    /**
   * Get header name for databases which are not shared between different
   * {@link TargetInfo}s.
   */
    private String getNonSharedHeaderName(String ext) {
        return this.nodeId + "$" + ext;
    }

    /**
   * Get the series associated with this target-info
   */
    public EventSeries getSeries() {
        return series;
    }

    /**
   * Add a {@link TargetInfoListener}.  Normally other parts of the code will
   * not call this method directly, but instead register through 
   * {@link EventSeries#addTargetInfoListener(TargetInfoListener)}
   */
    public void addTargetInfoListener(TargetInfoListener listener) {
        typeDescriptorTableListeners.add(listener);
    }

    /**
   * Remove a {@link TargetInfoListener}.  Normally other parts of the code
   * will not call this method directly, but instead  
   * {@link EventSeries#removeTargetInfoListener(TargetInfoListener)}
   */
    public void removeTargetInfoListener(TargetInfoListener listener) {
        typeDescriptorTableListeners.remove(listener);
    }

    /** 
   * This is currently needed by ti.mcore.gsp to fire events to all listeners
   * when the PCON/AIR-Msg settings change... we need to find a cleaner way.
   * <p>
   * TODO: find a better way..
   */
    public static void fireTargetInfoEventToAll() {
        TargetInfo[] targetInfos;
        synchronized (targetInfosTable) {
            targetInfos = targetInfosTable.keySet().toArray(new TargetInfo[targetInfosTable.size()]);
        }
        for (int i = 0; i < targetInfos.length; i++) if (targetInfos[i] != null) targetInfos[i].fireTargetInfoEvent("fireTargetInfoEventToAll");
    }

    /** keep a table of all un-gc'd TargetInfo's for the implementation of {@link #fireTargetInfoEventToAll()} */
    private static WeakHashMap<TargetInfo, Object> targetInfosTable = new WeakHashMap<TargetInfo, Object>();

    private void fireTargetInfoEvent(String reason) {
        LOGGER.dbg("fireTargetInfoEvent: %s", reason);
        typeDescriptorTableListeners.fire(new EventListeners.EventDispatcher<TargetInfoListener>() {

            public void dispatch(TargetInfoListener listener) {
                listener.update(TargetInfo.this);
            }
        });
    }

    /**
   * Read the target-info in serialized form.
   * 
   * @param in   the input stream to read from
   */
    public void readExternal(final UDataInputStream in) throws java.io.IOException {
        int fileVersion = in.readByte();
        nodeId = in.readShort();
        bigEndian = in.readBoolean();
        try {
            paddingRule = (PaddingRule) (Class.forName(in.readUTF()).newInstance());
        } catch (Exception e) {
            paddingRule = new ti.targetinfo.td.DefaultPaddingRule();
            e.printStackTrace();
            LOGGER.dbg(e.getMessage());
        }
        if (fileVersion == EXTERNAL_DATABASE_SUPPORTED_VERSION) {
            dbHeaderTable = (Map<String, String>) readObject(in);
            compressed = false;
        } else if (fileVersion >= GZIP_FORMAT_SUPPORTED_VERSION) {
            dbHeaderTable = (Map<String, String>) readObject(in);
            compressed = in.readBoolean();
            sutcName = in.readUTF();
        } else {
            throw new FatalError("unknown version: " + fileVersion);
        }
    }

    /**
   * Write the target-info in serialized form.
   * 
   * @param out   the output stream to write to
   */
    public void writeExternal(UDataOutputStream out) throws java.io.IOException {
        out.writeByte(VERSION);
        LOGGER.dbg("writeExternal");
        LOGGER.dbg("nodeId" + nodeId);
        LOGGER.dbg("bigEndian " + bigEndian);
        LOGGER.dbg("paddingRule " + paddingRule.getClass().getName());
        out.writeShort(nodeId);
        out.writeBoolean(bigEndian);
        out.writeUTF(paddingRule.getClass().getName());
        writeObject(out, dbHeaderTable);
        out.writeBoolean(compressed);
        out.writeUTF(sutcName);
    }

    /**
   * Write an object using serialization, but precede it with a length
   * field, so when reading in the object, if there is some incompatible
   * change, we know how many bytes to skip over to get to the next
   * object. 
   */
    private static void writeObject(UDataOutputStream out, Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (obj != null) {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
        }
        byte[] buf = baos.toByteArray();
        out.writeInt(buf.length);
        if (buf.length > 0) out.write(buf);
    }

    /**
   * The purpose of this function is to read any object from a log file
   * saved used an old version (0X00) of TargetInfo.
   * @param in
   * @return
   * @throws IOException
   */
    private static Object readObject(UDataInputStream in) {
        try {
            byte[] buf = new byte[in.readInt()];
            if (buf.length == 0) return null;
            in.readFully(buf);
            ByteArrayInputStream bois = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bois);
            return ois.readObject();
        } catch (Exception e) {
            LOGGER.logError(e);
            return null;
        }
    }

    /**
   * Accessor for SUTConnection... ensures that we are still "live"
   */
    public SUTConnection getSUTConnection() {
        if (sutc != null) if ((!sutc.isConnected()) || EventUtil.getConnectionCount(nodeId) != sutc.getConnectionCount()) sutc = null;
        return sutc;
    }

    /**
   * @return the node-id of the core that this object contains tables/info for.
   */
    public short getNodeId() {
        return nodeId;
    }

    /**
   * Get the appropriate {@link PaddingRule} 
   */
    public PaddingRule getPaddingRule() {
        return paddingRule;
    }

    /**
   * Return <code>true</code> if the core is big endian, else 
   * <code>false</code> if it is little endian.
   */
    public boolean isBigEndian() {
        return bigEndian;
    }

    /**
   * Get the build-id of the target, if it is known.  Otherwise return
   * <code>null</code>
   */
    public String getBuildId() {
        AttributeTable at = ((AttributeTable) getDatabase(AttributeTable.class));
        if (at == null) return null;
        return (String) at.get("BUILD_ID");
    }

    /**
   * Get an appropriatly constructed buffer-accessor, taking into consideration
   * the endianess and padding of the remote processor.
   */
    public BufferAccessor getBufferAccessor() {
        if (bufferAccessor == null) {
            if (isBigEndian()) bufferAccessor = new BigEndianBufferAccessor(); else bufferAccessor = new LittleEndianBufferAccessor();
        }
        return bufferAccessor;
    }

    /**
   * Access a remote buffer on the core.
   * 
   * @param addr    the base address for the remote buffer
   * @return the buffer
   */
    public RemoteBuffer getRemoteBuffer(long addr) {
        return new RemoteBuffer(addr, this);
    }

    /**
   * Get a local buffer.
   * 
   * @param size    the size in bytes of the local buffer
   * @return a local buffer
   */
    public LocalBuffer getLocalBuffer(int size) {
        return new LocalBuffer(size);
    }

    /**
   * Get a local buffer.
   * 
   * @param buf   the byte array to use as a local buffer
   * @return a local buffer
   */
    public LocalBuffer getLocalBuffer(byte[] buf) {
        return new LocalBuffer(buf);
    }

    /**
   * This is a bit of a hack, but if called from a non-UI thread, it will
   * block until the target-info is done being re-loaded
   */
    private void waitToBeLoaded() {
        if (Environment.getEnvironment().isSafeToSuspend()) {
            synchronized (this) {
                while (loading) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw OJavaException.makeJavaExceptionWrapper(e);
                    }
                }
            }
        }
    }

    /** called when the process of loading a db from disk is started */
    private synchronized boolean begunLoading(String db) {
        boolean wasLoading = loading;
        loading = true;
        if (wasLoading == false) currentLoadingDb = db; else LOGGER.dbg("still loading " + currentLoadingDb);
        return wasLoading;
    }

    private String currentLoadingDb = null;

    /** called when the process of loading a db is completed (should be in a finally { stopLoading(); }) */
    private synchronized void finishedLoading() {
        loading = false;
        notifyAll();
        fireTargetInfoEvent("finishedLoading: " + currentLoadingDb);
    }

    /**
   * Copy this target-info into a new series.  This creates a new object, which
   * references the specified series, and has it's serialized representation
   * written into a header in the new series
   * 
   * @param series       the series to copy into
   * @return a reparented clone of this object
   */
    public TargetInfo clone(EventSeries series) {
        if (this.series == series) throw new IllegalArgumentException("can't clone to the same series");
        flush(series, null);
        return new TargetInfo(series, nodeId);
    }

    /**
   * Flush any changes to a header in the series file.
   */
    private void flush() {
        waitToBeLoaded();
        flush(series, null);
    }

    /**
   * Flush the database to the series file.  This is called by databases
   * that are mutable (ie. can change after they are loaded)
   * 
   * @param db  the database to flush
   */
    public void flush(Database db) {
        flush(series, db);
    }

    /** 
   * flush this <code>TargetInfo</code> to the specified series, which in the
   * case of a {@link #clone(EventSeries)} operation is different from this
   * <code>TargetInfo</code>'s series.
   * 
   * @param series   the series to flush into
   * @param fdb      the database to flush, or <code>null</code> for all
   */
    private synchronized void flush(EventSeries series, Database fdb) {
        LOGGER.dbg("flush nodeId = " + nodeId);
        flushCoreInfo(series);
        for (Map.Entry<String, String> entry : (new Hashtable<String, String>(dbHeaderTable)).entrySet()) {
            String typeName = entry.getKey();
            String name = entry.getValue();
            Database db = getDatabase(typeName);
            if ((db != null) && ((fdb == null) || (fdb == db))) write2header(series, name, db);
        }
    }

    /**
   * Flushes only the core info.
   * Could not use flush(sereis, db) due to dead locks since in loadDatabase(file)
   * flush(db) will be called.
   * @param series
   */
    private synchronized void flushCoreInfo(EventSeries series) {
        try {
            OutputStream os = series.getHeader(nodeId + ".tinfo", true).getOutputStream(false);
            UDataOutputStream udos = new UDataOutputStream(os);
            writeExternal(udos);
            udos.flush();
            os.close();
        } catch (IOException e) {
            LOGGER.logError(e);
        }
    }

    /**
   * Write a cached database to a series header
   * @param hdrName  header name determines where the database will be serialized
   * @param db      the database to be serialized
   */
    private void write2header(EventSeries series, String hdrName, Database db) {
        try {
            OutputStream os = series.getHeader(hdrName, true).getOutputStream(false);
            series.getDatabaseManager().writeDatabase(db, os, compressed);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.logError(e);
        }
    }

    public String toString() {
        return "[TargetInfo: nodeId=" + nodeId + ", bigEndian=" + bigEndian + ", paddingRule=" + paddingRule + "]";
    }

    /**
   * Returns the name of the connection session associated with this target-info.  
   */
    public String getName() {
        return sutcName + "-" + nodeId;
    }

    static {
        try {
            oldTypeNameTable.put("ti.gsp.CCDDatabase", "dlls.CCDDatabase");
            oldTypeNameTable.put("ti.gsp.CCDDatabase", "ti.pco.CCDDatabase");
            oldTypeNameTable.put("ti.gsp.Str2IndDatabase", "ti.pco.Str2IndDatabase");
            IConfigurationElement[] points = PluginUtil.getExtensionPoints("ti.mcore.databaseFactory");
            for (int i = 0; i < points.length; i++) DatabaseFactoryInfo.registerDatabaseFactory(new DatabaseFactoryInfo(points[i]));
            SymbolTableDatabase.registerDatabaseFactories();
        } catch (Throwable t) {
            LOGGER.logError(t);
        }
    }
}
