package jreceiver.server.util.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esau.ptarmigan.Generator;
import org.esau.ptarmigan.GeneratorFactory;
import org.esau.ptarmigan.util.HelperURL;
import org.esau.ptarmigan.util.PtarURI;
import jreceiver.common.rec.Rec;
import jreceiver.common.rec.site.Site;
import jreceiver.common.rec.source.Mexternal;
import jreceiver.common.rec.source.Mfile;
import jreceiver.common.rec.source.Playlist;
import jreceiver.common.rec.source.Source;
import jreceiver.common.rec.source.Tune;
import jreceiver.server.bus.BusException;
import jreceiver.server.bus.FolderBus;
import jreceiver.server.bus.MexternalBus;
import jreceiver.server.bus.MfileBus;
import jreceiver.server.bus.MimeBus;
import jreceiver.server.bus.PlaylistBus;
import jreceiver.server.bus.SourceBus;
import jreceiver.server.bus.SourceListBus;
import jreceiver.server.bus.TuneBus;
import jreceiver.server.util.SAXErrorHandler;
import jreceiver.util.HelperFile;

/**
 * Common source parsing routines for the JReceiver project.
 *
 * @author Reed Esau
 * @version $Revision: 1.11 $ $Date: 2003/06/09 21:24:24 $
 */
public class SourceBuilder {

    static final int GENERATOR_READ_LIMIT = 128 * 1024;

    static final String TRANSFORMER_FACTORY = "org.apache.xalan.processor.TransformerFactoryImpl";

    static final String TAG_POLICY_TRANSFORM = "etc" + File.separatorChar + "tag_policy.xslt";

    /** CTOR */
    public SourceBuilder() {
        m_entry_list = new Stack();
        m_source_filter = new SourceFilter(m_entry_list);
    }

    /**
     * Retrieve metadata from tune or playlist and store to
     * database.
     * <p>
     * A record for the tune or playlist may already exist in
     * the database, in which case the associated records should
     * be updated.
     */
    public synchronized void buildAndStore(File base_file) throws IOException, BusException, BuilderException {
        log.debug("buildAndStore: file=" + base_file);
        if (HelperFile.canOpenAndRead(base_file) == false) {
            log.warn("buildAndStore: unable to read [" + base_file + "]; odd characters?");
            return;
        }
        buildAndStore(new PtarURI(base_file));
    }

    /**
     * Retrieve metadata from tune or playlist and store to
     * database.
     * <p>
     * A record for the tune or playlist may already exist in
     * the database, in which case the associated records should
     * be updated.
     * <p>
     * The system_id provided should be prefixed by a protocol
     * but should NOT be encoded (e.g., %20 or + for space).
     */
    public synchronized void buildAndStore(PtarURI uri) throws IOException, BusException, BuilderException {
        if (log.isInfoEnabled()) log.info("buildAndStore: uri=" + uri.toExternalForm());
        resetData();
        addSource(uri);
        if (log.isDebugEnabled()) log.debug("buildAndStore: uri added");
        PtarURI workingURI;
        while ((workingURI = getNextSource()) != null) {
            Source source_rec = getSourceRec(workingURI);
            processSource(source_rec, workingURI);
        }
        if (log.isDebugEnabled()) log.debug("buildAndStore: all sources exhausted");
        if (m_playlist_id >= Source.MIN_SRC_ID) {
            PlaylistBus pl_bus = PlaylistBus.getInstance();
            pl_bus.refreshStats(m_playlist_id);
        }
    }

    /**
     * examine (and possibly scan) a single source
     */
    void processSource(Source source_rec, PtarURI uri) throws IOException, BusException, BuilderException {
        if (log.isDebugEnabled()) log.debug("processSource: rec=" + source_rec);
        boolean must_generate = true;
        int folder_id = 0;
        int src_id = (source_rec != null ? source_rec.getSrcId() : 0);
        if (source_rec != null && source_rec instanceof Mfile) {
            Mfile mfile = (Mfile) source_rec;
            File file_on_disk = mfile.getFile();
            if (file_on_disk == null) throw new BuilderException("unexpectedly the file path is missing");
            if (file_on_disk.exists() == false) {
                log.warn("buildAndStore: cannot find [" + file_on_disk + "]; odd filename?");
                return;
            }
            must_generate = (((file_on_disk.lastModified() / 1000) * 1000) != mfile.getLastModified());
            folder_id = mfile.getFolderId();
        }
        if (must_generate) {
            if (source_rec != null && source_rec.getIsPlaylist()) {
                Vector keys = new Vector();
                keys.add(new Integer(src_id));
                SourceListBus sl_bus = SourceListBus.getInstance();
                sl_bus.deleteForPlaylists(keys);
            }
            generateAndStore(uri, src_id, folder_id);
        } else if (src_id > 0 && m_playlist_id > 0) {
            addToPlaylist(src_id);
        }
    }

    /**
     * generate metadata from the specified source, which may be local or remote.
     */
    void generateAndStore(final PtarURI uri, final int src_id, int folder_id) throws IOException, BusException, BuilderException {
        if (log.isDebugEnabled()) log.debug("generateAndStore: uri=" + uri.toExternalForm() + " src_id=" + src_id + " folder_id=" + folder_id);
        try {
            Source generated_source = generate(uri);
            if (generated_source == null) {
                log.info("generateAndStore: skipping " + uri.toExternalForm());
                return;
            }
            if (log.isDebugEnabled()) log.debug("generateAndStore: generated_source=" + generated_source);
            String clean_title = fixupTitle(generated_source.getTitle());
            generated_source.setTitle(clean_title);
            if (generated_source instanceof Mfile) {
                Mfile mfile = (Mfile) generated_source;
                if (folder_id == 0) {
                    File file = mfile.getFile();
                    if (log.isDebugEnabled()) log.debug("file=" + file + " path=" + mfile.getFilePath());
                    FolderBus fldr_bus = FolderBus.getInstance();
                    folder_id = fldr_bus.getFolderId(Site.SITE_HOME, file.getParentFile());
                }
                mfile.setFolderId(folder_id);
            } else if (generated_source instanceof Mexternal) {
                Mexternal mex = (Mexternal) generated_source;
                mex.setDirectURI(uri);
            }
            MimeBus mime_bus = MimeBus.getInstance();
            String mime_type = generated_source.getMime();
            if (mime_type == null || mime_type.trim().length() == 0) {
                log.debug("mime-type not provided; source ignored");
                return;
            }
            mime_bus.getIdForType(mime_type);
            Source existing_source = null;
            if (src_id >= Source.MIN_SRC_ID) {
                SourceBus src_bus = SourceBus.getInstance();
                existing_source = (Source) src_bus.getRec(src_id, null);
                if (log.isDebugEnabled()) log.debug("generateAndStore: existing_source=" + existing_source);
                generated_source.setTitle(existing_source.getTitle());
            }
            if (generated_source instanceof Playlist) {
                if (existing_source != null && existing_source.getIsPlaylist()) {
                    log.debug("assigning src_id=" + src_id + " to generated PLAYLIST");
                    generated_source.setSrcId(src_id);
                }
                PlaylistBus bus = PlaylistBus.getInstance();
                int pl_src_id = bus.storeRec((Rec) generated_source);
                m_playlist_id = pl_src_id;
            } else if (generated_source instanceof Tune) {
                if (existing_source != null && existing_source.getIsTune()) {
                    log.debug("assigning src_id=" + src_id + " to generated TUNE");
                    generated_source.setSrcId(src_id);
                }
                if (existing_source != null && existing_source.getIsPlaylist()) {
                    log.debug("assigning src_id=" + src_id + " to m_playlist");
                    m_playlist_id = src_id;
                }
                TuneBus tune_bus = TuneBus.getInstance();
                int tune_src_id = tune_bus.storeRec((Rec) generated_source);
                if (m_playlist_id > 0) addToPlaylist(tune_src_id);
            }
        } catch (SAXException e) {
            throw new BuilderException("sax-problem building source", e);
        } catch (InstantiationException e) {
            throw new BuilderException("instantiation-problem building source", e);
        } catch (IllegalAccessException e) {
            throw new BuilderException("access-problem building source", e);
        } catch (TransformerException e) {
            throw new BuilderException("transformer-problem building source", e);
        }
    }

    /**
     * This is where the Magic happens.
     *
     * Generate a Source object populated with metadata from a URL.
     *
     * Set up and execute the following SAX pipeline:
     *
     * URL -> GENERATOR => SRC-FILTER => TRANSFORMER => DIGESTER -> OBJECT
     *
     * GENERATOR - is Ptarmigan, which reads metadata and playlist entries
     * from tunes and playlists.
     *
     * SRC-FILTER - for playlists, produces additional items to scan.
     *
     * TRANSFORMER - intiates the processing and produces output that
     * is easily digested by JRec.
     *
     * DIGESTER - populates a TuneRec or PlaylistRec depending on input.
     */
    Source generate(PtarURI uri) throws IOException, BuilderException, SAXException, TransformerException, MalformedURLException, InstantiationException, IllegalAccessException {
        if (log.isDebugEnabled()) log.debug("generate: uri=" + uri.toExternalForm());
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            if (uri.isFile()) {
                File file = uri.getFile();
                if (log.isDebugEnabled()) log.debug("generate: file=" + file);
                if (file == null || file.exists() == false) {
                    log.info("generate: file does not exist [" + file + "]");
                    return null;
                }
                is = new FileInputStream(file);
            } else {
                URL url = uri.getURL();
                if (log.isDebugEnabled()) log.debug("generate: url=" + url);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "JReceiver/@version@");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Icy-MetaData", "1");
                conn.connect();
                is = conn.getInputStream();
            }
            InputSource input = new InputSource(is);
            input.setSystemId(uri.toExternalForm());
            if (m_generator == null) initGenerator();
            if (m_digester == null) initDigester();
            if (m_transformer == null) initTransformer();
            m_generator.resetData();
            m_generator.setContentHandler(m_source_filter);
            m_source_filter.setParent(m_generator);
            m_source_filter.resetData();
            SAXSource transform_source = new SAXSource(m_source_filter, input);
            if (log.isDebugEnabled()) log.debug("generate: executing pipeline");
            m_transformer.clearParameters();
            m_transformer.transform(transform_source, new SAXResult(m_digester));
            log.debug("generate: finished pipeline");
            Source source = (Source) m_digester.getRoot();
            if (log.isDebugEnabled()) log.debug("generate: source=" + source);
            return source;
        } finally {
            log.debug("closing");
            HelperFile.safeClose(is);
            log.debug("closed");
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * obtain an mfile or mexternal rec if an existing source
     */
    Source getSourceRec(PtarURI uri) throws IOException, BusException {
        log.debug("getSourceRec: uri=" + uri.toExternalForm() + " isFile=" + uri.isFile());
        Source source = null;
        if (uri.isFile()) {
            File file = uri.getFile();
            MfileBus mfile_bus = MfileBus.getInstance();
            log.debug("getSourceRec: file=" + file);
            Hashtable args = new Hashtable();
            args.put(Source.POPULATE_DIRECT_URI, new Boolean(true));
            source = mfile_bus.getRecForPath(Site.SITE_HOME, file, args);
        } else {
            URL url = uri.getURL();
            MexternalBus mext_bus = MexternalBus.getInstance();
            source = mext_bus.getRecForURL(url, null);
        }
        return source;
    }

    /** configure the transformer */
    void initTransformer() throws TransformerException {
        if (m_transformer != null) return;
        String home_dir = System.getProperty("jreceiver.home");
        if (home_dir == null) throw new TransformerException("application root is not defined; cannot find xslt");
        File xslt_file = new File(home_dir + File.separatorChar + TAG_POLICY_TRANSFORM);
        log.info("initTransformer: xslt_file=" + xslt_file);
        if (xslt_file.exists() == false) throw new TransformerException("cannot find xslt_file=" + xslt_file);
        StreamSource stream_source = new StreamSource(xslt_file);
        TransformerFactory factory = getFactoryInstance();
        if (log.isDebugEnabled()) log.debug("initTransformer: SAXSource feature=" + factory.getFeature(SAXSource.FEATURE));
        m_transformer = factory.newTransformer(stream_source);
        log.debug("transformDocument: created transformer for stream source");
    }

    /** obtain a TransformerFactory */
    TransformerFactory getFactoryInstance() throws TransformerException {
        try {
            Class c = Class.forName(TRANSFORMER_FACTORY);
            return (TransformerFactory) c.newInstance();
        } catch (InstantiationException e) {
            throw new TransformerException("instantiation-problem loading xalan", e);
        } catch (IllegalAccessException e) {
            throw new TransformerException("access-problem loading xalan", e);
        } catch (ClassNotFoundException e) {
            throw new TransformerException("class-not-found problem loading xalan", e);
        }
    }

    /**
     * Configure the SAX generator
     */
    void initGenerator() throws InstantiationException, IllegalAccessException, SAXException {
        if (m_generator != null) {
            m_generator.resetData();
            return;
        }
        m_generator = GeneratorFactory.newInstance();
        m_generator.setFeature(Generator.FEATURE_INCLUDE_PLAYLIST_ENTRIES, true);
        m_generator.setProperty(Generator.PROPERTY_READ_LIMIT, new Integer(GENERATOR_READ_LIMIT));
    }

    /**
     * Configure the parsing rules that will be used to process input.
     *
     * TODO: move these rules into an external XML file
     *
     * TODO: add schema and enforce namespace
     */
    void initDigester() {
        if (m_digester != null) {
            m_digester.clear();
            return;
        }
        m_digester = new Digester();
        m_digester.setErrorHandler(new SAXErrorHandler());
        m_digester.addObjectCreate("source", "", "class-name");
        m_digester.addCallMethod("source/system-id", "setDirectURI", 0);
        m_digester.addCallMethod("source/mime-type", "setMime", 0);
        m_digester.addCallMethod("source/duration", "setDuration", 1, new Class[] { int.class });
        m_digester.addCallParam("source/duration", 0);
        m_digester.addCallMethod("source/title", "setTitle", 0);
        m_digester.addCallMethod("source/file-properties/last-modified", "setLastModifiedISO", 0);
        m_digester.addCallMethod("source/file-properties/length", "setFileSize", 1, new Class[] { long.class });
        m_digester.addCallParam("source/file-properties/length", 0);
        m_digester.addCallMethod("source/tune/tune-type", "setTuneType", 1, new Class[] { int.class });
        m_digester.addCallParam("source/tune/tune-type", 0);
        m_digester.addCallMethod("source/tune/bit-rate", "setBitrate", 1, new Class[] { int.class });
        m_digester.addCallParam("source/tune/bit-rate", 0);
        m_digester.addCallMethod("source/tune/data-offset", "setDataOffset", 1, new Class[] { int.class });
        m_digester.addCallParam("source/tune/data-offset", 0);
        m_digester.addCallMethod("source/tune/genres/genre", "addGenre", 0);
        m_digester.addCallMethod("source/tune/artists/artist", "setArtist", 0);
        m_digester.addCallMethod("source/tune/albums/album", "setAlbum", 0);
        m_digester.addCallMethod("source/tune/composers/composer", "setComposer", 0);
        m_digester.addCallMethod("source/tune/comments/comment", "addComment", 0);
        m_digester.addCallMethod("source/tune/track-no", "setTrackNo", 1, new Class[] { int.class });
        m_digester.addCallParam("source/tune/track-no", 0);
        m_digester.addCallMethod("source/tune/year", "setYear", 1, new Class[] { int.class });
        m_digester.addCallParam("source/tune/year", 0);
    }

    /**
     * add a uri to process (adds to end of list)
     */
    void addSource(PtarURI source) {
        m_entry_list.insertElementAt(source, 0);
    }

    /**
     * obtain the next uri to process
     */
    PtarURI getNextSource() {
        try {
            return (PtarURI) m_entry_list.pop();
        } catch (EmptyStackException ignored) {
            return null;
        }
    }

    /**
     * If a playlist is active for this build session, then add the tune to it.
     */
    void addToPlaylist(int tune_src_id) throws BusException {
        if (m_playlist_id > 0) {
            SourceListBus sl_bus = SourceListBus.getInstance();
            sl_bus.addTuneToPlaylist(m_playlist_id, tune_src_id);
        }
    }

    /**
     * The title may have escaped UTF-8 characters (e.g., %C8%A2) if
     * it was derived from the URL.
     *
     * Unescape it, converting to Unicode and remove any leading
     * non-alphanumeric.
     */
    String fixupTitle(String title) {
        if (title == null) return null;
        try {
            title = HelperURL.maybeDecode(title);
            int i = 0;
            int len = title.length();
            for (; i < len; i++) if (Character.isLetterOrDigit(title.charAt(i))) break;
            if (i > 0) title = title.substring(i);
            return title;
        } catch (MalformedURLException e) {
            return title;
        }
    }

    public synchronized void resetData() {
        if (m_generator != null) m_generator.resetData();
        if (m_digester != null) m_digester.clear();
        m_entry_list.clear();
        m_source_filter.resetData();
        m_playlist_id = 0;
    }

    /**
     * a FIFO of URLs to process; we use the Stack because it can also
     * function as a FIFO.
     */
    Stack m_entry_list;

    /** SAX PRODUCER - used for extracting tag info via SAX */
    Generator m_generator;

    /** */
    SourceFilter m_source_filter;

    /** used to transform from ptarmigan to our own schema */
    Transformer m_transformer;

    /** SAX CONSUMER */
    Digester m_digester;

    /**
     * the playlist under which all tune reads for this session will be
     * associated
     */
    int m_playlist_id;

    /**
     * logging object
     */
    static Log log = LogFactory.getLog(SourceBuilder.class);
}
