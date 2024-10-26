package gg.arkehion.lsd.fileimpl;

import gg.arkehion.configuration.Configuration;
import gg.arkehion.exceptions.ArEndTransferException;
import gg.arkehion.exceptions.ArFileException;
import gg.arkehion.exceptions.ArUnvalidIndexException;
import gg.arkehion.lsd.ArDocInterface;
import gg.arkehion.lsd.ArLegacyInterface;
import gg.arkehion.lsd.ArStoreInterface;
import gg.arkehion.store.ArkDirConstants;
import gg.arkehion.utils.ArByteToByte;
import gg.arkehion.utils.ArConstants;
import goldengate.common.crypto.KeyObject;
import goldengate.common.digest.FilesystemBasedDigest;
import goldengate.common.digest.FilesystemBasedDigest.DigestAlgo;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * @author frederic
 * 
 */
public class ArFsLegacy implements ArLegacyInterface {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ArFsLegacy.class);

    /**
     * LID : unique ID of the Legacy
     */
    private long LID = ArkDirConstants.invalide_idx;

    /**
     * Name of LID
     */
    private String nameLID = null;

    /**
     * Key of crypto (AES)
     */
    private byte[] secretkey = null;

    /**
     * This storage is in Crypted mode
     */
    protected boolean isCrypted = false;

    /**
     * The base path for the Legacy
     */
    private String basePath = null;

    /**
     * The base path for extraction/copy of the files for the Legacy
     */
    private String outPath = null;

    /**
     * The max size for each Storage in the set of this Legacy
     */
    private long sizemax = 0;

    /**
     * Crypto object associated with this Legacy
     */
    private KeyObject keyObject = null;

    /**
     * Which Digest is used in this Legacy
     */
    private DigestAlgo algo = null;

    /**
     * Is this Legacy running or not ? When not, only Get/Info/Move are allowed.
     * Move is allowed since if someone wants to implements a defrag operation,
     * it needs the legacy to be not locked but locked for others (Put/Del).
     */
    private boolean is_stopped = true;

    /**
     * New LSDDbLegacy from parameters.
     * 
     * By default, the Legacy is not ready after its creation.
     * 
     * You must call start() explicitely.
     * 
     * @param sNameLID
     *            : logical name of this Legacy
     * @param lid
     *            : unique id of the Legacy
     * @param iscrypted
     *            : status crypto for this Legacy
     * @param key
     *            : the key if crypted or null
     * @param basepath
     *            : the basepath for this Legacy
     * @param outbase
     *            : the basepath for the export/extraction for this Legacy
     * @param size
     *            : the max size of each Storage in the Set of this Legacy
     * @param algo
     *            : the Digest algorithm to use to check file hash
     * @throws ArUnvalidIndexException
     *             if one value is incorrect
     */
    public ArFsLegacy(String sNameLID, long lid, boolean iscrypted, byte key[], String basepath, String outbase, long size, DigestAlgo algo) throws ArUnvalidIndexException {
        this.nameLID = sNameLID;
        if (this.nameLID == null || this.nameLID.equalsIgnoreCase("")) {
            this.nameLID = "NONAME";
        }
        this.LID = lid;
        if (ArConstants.isIdUniqueKO(this.LID)) {
            throw new ArUnvalidIndexException("LID is not correct for " + this.nameLID);
        }
        this.isCrypted = iscrypted;
        if (this.isCrypted) {
            this.secretkey = key;
            if (key == null || (key.length == 0)) {
                this.isCrypted = false;
                this.secretkey = null;
                this.keyObject = null;
            } else {
                this.keyObject = ArFsCrypto.createNewKeyObject();
                this.keyObject.setSecretKey(this.secretkey);
            }
        } else {
            this.secretkey = null;
            this.keyObject = null;
        }
        this.basePath = basepath;
        if (this.basePath == null || this.basePath.equalsIgnoreCase("")) {
            throw new ArUnvalidIndexException("BasePath is not correct for " + this.nameLID);
        }
        if (!(this.basePath.endsWith("/") || this.basePath.endsWith("\\"))) {
            this.basePath += ArConstants.sseparator;
        }
        this.outPath = outbase;
        if (this.outPath == null || this.outPath.equalsIgnoreCase("")) {
            throw new ArUnvalidIndexException("OutPath is not correct for " + this.nameLID);
        }
        if (!(this.outPath.endsWith("/") || this.outPath.endsWith("\\"))) {
            this.outPath += ArConstants.sseparator;
        }
        this.sizemax = size;
        this.is_stopped = true;
        this.algo = algo;
    }

    /**
     * Return the base path of this Legacy
     * 
     * @return the sPath.
     */
    public String getBasePath() {
        return this.basePath;
    }

    /**
     * Return the base Out path of this Legacy
     * 
     * @return the base out path
     */
    public String getOutPath() {
        return this.outPath;
    }

    /**
     * Set a new base out Path
     * 
     * @param outPath
     */
    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

    /**
     * Force the creation of the basepath
     * 
     * @return True if OK, False else
     */
    public boolean createDir() {
        return Configuration.arDir.createDir(this.basePath);
    }

    /**
     * Force the creation of the basepath down to the storage with the given
     * sid.
     * 
     * @param sid
     * @return True if OK, False else
     * @throws ArUnvalidIndexException
     */
    public boolean createDir(long sid) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(sid)) {
            throw new ArUnvalidIndexException("Invalid ID for Store");
        }
        ArStoreInterface storage = getStore(sid);
        return Configuration.arDir.createDir(this.basePath + storage.getGlobalPath());
    }

    /**
     * Delete the basepath (if not empty)
     * 
     * @param recursive
     *            if the operation must be done recursively
     * @return True if OK, False else
     */
    public boolean deleteDir(boolean recursive) {
        if (recursive) {
            return Configuration.arDir.deleteRecursiveDir(this.basePath);
        } else {
            return Configuration.arDir.deleteDir(this.basePath);
        }
    }

    /**
     * Delete the basepath down to the storage with the given sid (if not empty)
     * 
     * @param sid
     * @param recursive
     *            if the operation must be done recursively
     * @return True if OK, False else
     * @throws ArUnvalidIndexException
     */
    public boolean deleteDir(long sid, boolean recursive) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(sid)) {
            throw new ArUnvalidIndexException("Invalid ID for Store");
        }
        ArStoreInterface storage = getStore(sid);
        if (recursive) {
            return Configuration.arDir.deleteRecursiveDir(this.basePath + storage.getGlobalPath());
        } else {
            return Configuration.arDir.deleteDir(this.basePath + storage.getGlobalPath());
        }
    }

    /**
     * Returns True if the basepath is empty.
     * 
     * Returns False if not or if is does not exist or is not a directory.
     * 
     * @param recursive
     *            if the operation must be done recursively
     * @return True if empty directory, False else
     */
    public boolean isDirEmpty(boolean recursive) {
        if (recursive) {
            return Configuration.arDir.isDirRecursiveEmpty(this.basePath);
        } else {
            return Configuration.arDir.isDirEmpty(this.basePath);
        }
    }

    /**
     * Returns True if the basepath down to the storage with the given sid is
     * empty.
     * 
     * Returns False if not or if is does not exist or is not a directory.
     * 
     * @param sid
     * @param recursive
     *            if the operation must be done recursively
     * @return True if empty directory, False else
     * @throws ArUnvalidIndexException
     */
    public boolean isDirEmpty(long sid, boolean recursive) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(sid)) {
            throw new ArUnvalidIndexException("Invalid ID for Store");
        }
        ArStoreInterface storage = getStore(sid);
        if (recursive) {
            return Configuration.arDir.isDirRecursiveEmpty(this.basePath + storage.getGlobalPath());
        } else {
            return Configuration.arDir.isDirEmpty(this.basePath + storage.getGlobalPath());
        }
    }

    /**
     * Create the base Out Path
     * 
     * @return True if OK, False else
     */
    public boolean createDirOut() {
        return Configuration.arDir.createDir(this.outPath);
    }

    /**
     * Delete the base Out Path if not empty
     * 
     * @return True if OK, False else
     */
    public boolean deleteDirOut() {
        return Configuration.arDir.deleteDir(this.outPath);
    }

    /**
     * Returns True if the base out path is empty.
     * 
     * Returns False if not or if is does not exist or is not a directory.
     * 
     * @return True if empty directory, False else
     */
    public boolean isDirOutEmpty() {
        return Configuration.arDir.isDirEmpty(this.outPath);
    }

    /**
     * Stop this Legacy (put and delete not allowed). When stopped, only
     * Get/Info/Move are allowed. Move is allowed since if someone wants to
     * implements a defrag operation, it needs the legacy to be not locked but
     * locked for others (Put/Del).
     * 
     * @return True if stopped
     */
    public boolean stop() {
        this.is_stopped = true;
        return this.is_stopped;
    }

    /**
     * Start this Legacy (put and delete allowed). When stopped, only
     * Get/Info/Move are allowed. Move is allowed since if someone wants to
     * implements a defrag operation, it needs the legacy to be not locked but
     * locked for others (Put/Del).
     * 
     * @return False if started
     */
    public boolean start() {
        boolean retour = this.createDir();
        if (retour) {
            retour = this.createDirOut();
        }
        if (retour) {
            this.is_stopped = false;
        } else {
            this.is_stopped = true;
        }
        return this.is_stopped;
    }

    /**
     * Put a file from the filename in the Document. Storage (sid) and Document
     * (did). Returns the size of insertion. The original file is not deleted.
     * The filename can be either a true file accessed from the filesystem of
     * the server, or either a file accessed from a net service (http, ftp,
     * ...).
     * 
     * @param fileName
     * @param sid
     * @param did
     * @return The size of insertion if is OK
     * @throws ArUnvalidIndexException
     * @throws ArFileException
     */
    public long put(String fileName, long sid, long did) throws ArUnvalidIndexException, ArFileException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        ArDocInterface doc = getDocument(sid, did);
        FileChannel fileChannelIn = null;
        FileInputStream fileInputStream = null;
        InputStream inputStream = null;
        boolean isNetwork = true;
        try {
            URI uri = new URI(fileName);
            if (uri.getScheme().equalsIgnoreCase("file")) {
                isNetwork = false;
            }
            if (isNetwork) {
                URL url = uri.toURL();
                inputStream = url.openStream();
                url = null;
            } else {
                fileInputStream = new FileInputStream(fileName);
            }
            uri = null;
        } catch (Exception e) {
            try {
                fileInputStream = new FileInputStream(fileName);
            } catch (FileNotFoundException e1) {
                throw new ArFileException("Error during open InputStream in put:" + fileName);
            }
            isNetwork = false;
        }
        long size = 0;
        if (isNetwork) {
            size = doc.write(inputStream);
        } else {
            fileChannelIn = fileInputStream.getChannel();
            size = doc.write(fileChannelIn);
        }
        try {
            if (isNetwork) {
                inputStream.close();
            } else {
                fileChannelIn.close();
            }
        } catch (IOException e) {
            logger.info("Error during close while put", e);
        }
        fileChannelIn = null;
        return size;
    }

    /**
     * Put a file from the fileChannelIn in the Storage (sid) and Document
     * (did). The size of insertion if is OK.
     * 
     * This call closes the fileChannelIn with fileChannelIn.close() if the
     * operation is in success.
     * 
     * @param fileChannelIn
     * @param sid
     * @param did
     * @return The size of insertion if is OK
     * @throws ArUnvalidIndexException
     * @throws ArFileException
     */
    public long put(FileChannel fileChannelIn, long sid, long did) throws ArUnvalidIndexException, ArFileException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        ArDocInterface doc = getDocument(sid, did);
        long size = 0;
        size = doc.write(fileChannelIn);
        try {
            fileChannelIn.close();
        } catch (IOException e) {
            logger.warn("Error during close while put", e);
        }
        doc.clear();
        doc = null;
        return size;
    }

    /**
     * Put a block of file from the array of byte to the document associated
     * with the ArFsDoc argument from the same Legacy.
     * 
     * 
     * @param bytes
     *            array of byte added to the corresponding document
     * @param document
     *            ArFsDoc from the same Legacy
     * @param lastblock
     *            Says if this is the last block
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public void putBlock(byte bytes[], ArDocInterface document, boolean lastblock) throws ArUnvalidIndexException, ArFileException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        if (!document.isInWriting()) {
            throw new ArFileException("Document is not in Writing");
        }
        if (lastblock) {
            document.writeBlockEnd(bytes);
        } else {
            document.writeBlock(bytes);
        }
    }

    /**
     * Get a block of the file associated with the ArFsDoc argument from the
     * same Legacy. The size of the block is at most sizeblock.
     * 
     * If the block size is less than sizeblock, this is the last block to read
     * from the document.
     * 
     * @param document
     *            ArFsDoc from the same Legacy
     * @param sizeblock
     *            Size of block to read
     * @return the byte array for the block are to be read.
     * @throws ArEndTransferException
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public byte[] getBlock(ArDocInterface document, int sizeblock) throws ArUnvalidIndexException, ArFileException, ArEndTransferException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        byte[] bytes = null;
        if (!document.isInReading()) {
            document.retrieve(sizeblock);
        }
        bytes = document.getBlockBytes();
        return bytes;
    }

    /**
     * Get the full file in one array of byte (limited to 2^32 bytes).
     * 
     * @param sid
     *            Storage id
     * @param did
     *            Document id
     * @return the array of byte
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public byte[] get(long sid, long did) throws ArUnvalidIndexException, ArFileException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        if (ArConstants.isIdUniqueKO(sid) || ArConstants.isIdUniqueKO(did)) {
            throw new ArUnvalidIndexException("Store and Doc are not valid");
        }
        ArDocInterface doc = getDocument(sid, did);
        byte[] bytes = null;
        bytes = doc.get();
        doc.clear();
        doc = null;
        return bytes;
    }

    /**
     * Get the the full path to access to a copy of the file in the base out
     * path defined for this Legacy.
     * 
     * The copy of the file is created during this call.
     * 
     * @param sid
     *            Storage id
     * @param did
     *            Document id
     * @return the full path (using base out path) to a copy of the file or null
     *         if an error occurs
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public String getInPath(long sid, long did) throws ArFileException, ArUnvalidIndexException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        if (ArConstants.isIdUniqueKO(sid) || ArConstants.isIdUniqueKO(did)) {
            throw new ArUnvalidIndexException("Store and Doc are not valid");
        }
        ArDocInterface doc = getDocument(sid, did);
        String path = doc.getAbstractName();
        File filecopy = new File(outPath + path);
        if (filecopy.isFile()) {
            filecopy = null;
            return path;
        }
        FileChannel fileChannelOut = getFileChannelOut(filecopy, 0);
        doc.get(fileChannelOut);
        doc.clear();
        doc = null;
        try {
            fileChannelOut.close();
        } catch (IOException e) {
            logger.info("Error during close in getPath", e);
        }
        fileChannelOut = null;
        filecopy = null;
        return path;
    }

    /**
     * Remove the physical file associated with this Storade sid and Document
     * did if the hash String is the correct one.
     * 
     * @param sid
     *            Storage id
     * @param did
     *            Document id
     * @param shash
     * @return 1 if deleted (already or newly deleted), else 0 if MD5 or Ids are
     *         bad, else -1 if not deleted
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public int remove(long sid, long did, String shash) throws ArUnvalidIndexException, ArFileException {
        if (this.is_stopped) {
            throw new ArUnvalidIndexException("Legacy is not started");
        }
        if (ArConstants.isIdUniqueKO(sid) || ArConstants.isIdUniqueKO(did)) {
            throw new ArUnvalidIndexException("Store and Doc are not valid");
        }
        ArDocInterface doc = getDocument(sid, did);
        int retour = 0;
        if (!doc.exists()) {
            retour = 1;
        } else if (doc.isMarkEqual(shash)) {
            if (doc.delete()) {
                retour = 1;
            } else {
                retour = (-1);
            }
        }
        doc.clear();
        doc = null;
        return retour;
    }

    /**
     * Returns the ArFsStore object associated with the current Legacy lid,
     * Storage sid
     * 
     * @param sid
     *            Storage id
     * @return the ArFsStore associated
     * @throws ArUnvalidIndexException
     */
    public ArStoreInterface getStore(long sid) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(sid)) {
            throw new ArUnvalidIndexException("Invalid Store index");
        }
        return new ArFsStore(this, sid);
    }

    /**
     * Returns the ArFsDoc object associated with the current Legacy lid,
     * Storage sid, Document did
     * 
     * @param sid
     *            Storage id
     * @param did
     *            Document id
     * @return the ArFsDoc associated
     * @throws ArUnvalidIndexException
     */
    public ArDocInterface getDocument(long sid, long did) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(sid) || ArConstants.isIdUniqueKO(did)) {
            throw new ArUnvalidIndexException("Invalid Store or Doc index");
        }
        ArStoreInterface storage = getStore(sid);
        if (isCrypted) {
            return new ArFsDocEncrypted(storage, did);
        } else {
            return new ArFsDocUnencryptedFC(storage, did);
        }
    }

    /**
     * Returns the ArFsDoc object associated with the current Legacy lid,
     * Storage sid, Document did
     * 
     * @param storage
     *            Storage
     * @param did
     *            Document id
     * @return the ArFsDoc associated
     * @throws ArUnvalidIndexException
     */
    public ArDocInterface getDocument(ArStoreInterface storage, long did) throws ArUnvalidIndexException {
        if (ArConstants.isIdUniqueKO(did)) {
            throw new ArUnvalidIndexException("Invalid Doc index");
        }
        if (isCrypted) {
            return new ArFsDocEncrypted(storage, did);
        } else {
            return new ArFsDocUnencryptedFC(storage, did);
        }
    }

    /**
     * Move one document in the same Legacy from one couple sid/did to another
     * couple sidnew/didnew
     * 
     * @param sid
     *            Storage id source
     * @param did
     *            Document id source
     * @param sidnew
     *            Storage id destination
     * @param didnew
     *            Document id destination
     * @return True if the move is ok, else False
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public boolean move(long sid, long did, long sidnew, long didnew) throws ArUnvalidIndexException, ArFileException {
        ArDocInterface doc = getDocument(sid, did);
        ArDocInterface docnew = doc.move(sidnew, didnew);
        doc.clear();
        doc = null;
        if (docnew != null) {
            docnew.clear();
            docnew = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Move one document from this Legacy (sid/did) to another document (newdoc)
     * that can be outside the current Legacy.
     * 
     * @param sid
     *            Storage id source
     * @param did
     *            Document id source
     * @param newdoc
     *            ArFsDoc as destination
     * @return True if the move is ok, else False
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public boolean move(long sid, long did, ArDocInterface newdoc) throws ArUnvalidIndexException, ArFileException {
        ArDocInterface doc = getDocument(sid, did);
        doc.move(newdoc);
        doc.clear();
        doc = null;
        return true;
    }

    /**
     * Same as move(sid,did,sidnew,didnew) but returning the new ArFsDoc
     * associated to the new place of the document or null in case of error.
     * 
     * @param sid
     *            Storage id source
     * @param did
     *            Document id source
     * @param sidnew
     *            Storage id destination
     * @param didnew
     *            Document id destination
     * @return the new ArFsDoc if the move is OK, False else
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    public ArDocInterface moveToDocument(long sid, long did, long sidnew, long didnew) throws ArUnvalidIndexException, ArFileException {
        ArDocInterface doc = getDocument(sid, did);
        ArDocInterface docnew = doc.move(sidnew, didnew);
        doc.clear();
        doc = null;
        return docnew;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "LEGACY:" + this.nameLID + ":" + this.LID + " Base:" + this.basePath + " Out:" + this.outPath + " Size:" + this.sizemax + " Crypted:" + this.isCrypted + " Stopped:" + this.is_stopped;
    }

    /**
     * Get the KeyObject
     * 
     * @return Returns the KeyObject.
     */
    protected KeyObject getKeyObject() {
        return keyObject;
    }

    /**
     * Get the sercretkey
     * 
     * @return Returns the secretkey.
     */
    protected byte[] getSecretkey() {
        return secretkey;
    }

    /**
     * Get the LID
     * 
     * @return the LID for this Legacy
     */
    public long getLID() {
        return this.LID;
    }

    /**
     * Get the max size of one Storage from this Legacy. <br>
     * Warning: this size must be less than real size since encryption should
     * add some bytes. In general, depending on files size and crypto, 5 percent
     * less than the real size should be enough.
     * 
     * @return the max size for this Legacy
     */
    public long getSize() {
        return this.sizemax;
    }

    /**
     * Get the name of the Legacy
     * 
     * @return the name of this Legacy
     */
    public String getName() {
        return this.nameLID;
    }

    /**
     * @return the isCrypted
     */
    public boolean isCrypted() {
        return isCrypted;
    }

    /**
     * @return the is_stopped
     */
    public boolean is_stopped() {
        return is_stopped;
    }

    @Override
    public boolean isLegacySharingKeyObject(ArLegacyInterface legacy) {
        return ArByteToByte.bytesEqualBytes(this.getSecretkey(), ((ArFsLegacy) legacy).getSecretkey());
    }

    /**
     * External Hash (including crypted file)
     * 
     * @param file
     * @return the hash from this Legacy for the given Doc
     * @throws ArFileException
     */
    public byte[] getHash(File file) throws ArFileException {
        try {
            return FilesystemBasedDigest.getHash(file, Configuration.useNIO, this.algo);
        } catch (IOException e) {
            throw new ArFileException("Cannot compute Hash");
        }
    }

    /**
     * Internal Hash (same whatever crypted or uncrypted Legacy)
     * 
     * @param file
     * @return the hash from this Legacy for the given Doc
     * @throws ArFileException
     */
    public byte[] getInternalHash(File file) throws ArFileException {
        if (isCrypted) {
            CipherInputStream cipherInputStream = getCipherInputStream(file);
            if (cipherInputStream == null) throw new ArFileException("Cannot read the doc");
            try {
                return FilesystemBasedDigest.getHashCipher(cipherInputStream, this.algo);
            } catch (IOException e) {
                try {
                    cipherInputStream.close();
                } catch (IOException e1) {
                }
                throw new ArFileException("Cannot compute Hash");
            }
        } else {
            return getHash(file);
        }
    }

    /**
     * Returns a CiptherInputStream for access to the file in Read mode.
     * 
     * @param file
     * @return the CipherInputStream or null in error
     * @throws ArFileException
     */
    public CipherInputStream getCipherInputStream(File file) throws ArFileException {
        if (!file.exists()) {
            throw new ArFileException("File does not exist");
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.info("File not found in getCipherInputStream");
            throw new ArFileException("File cannot be read");
        }
        Cipher cipherIn = keyObject.toDecrypt();
        if (cipherIn != null) {
            CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipherIn);
            return cipherInputStream;
        } else {
            try {
                fileInputStream.close();
            } catch (IOException e) {
            }
            throw new ArFileException("File cannot be read in Cipher mode");
        }
    }

    /**
     * Returns a CiptherOutputStream for access to the file in Write mode.
     * 
     * Crypted Mode
     * 
     * @param file
     * @return the CipherOutputStream or null in error
     * @throws ArFileException
     */
    public CipherOutputStream getCipherOutputStream(File file) throws ArFileException {
        Configuration.arDir.createDir(file.getParent());
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            logger.info("File not found in getCipherOutputStream");
            throw new ArFileException("File cannot be written");
        }
        Cipher cipherOut = keyObject.toCrypt();
        if (cipherOut != null) {
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipherOut);
            return cipherOutputStream;
        } else {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
            }
            throw new ArFileException("File cannot be written in Cipher mode");
        }
    }

    /**
     * Returns the FileOutputStream in Out mode associated for the file.
     * 
     * @param file
     * @param position
     *            greater than 0 if the FileOutputStream should be in append
     *            mode
     * @return the FileOutputStream (OUT)
     * @throws ArFileException
     */
    public FileOutputStream getFileOutputStream(File file, long position) throws ArFileException {
        Configuration.arDir.createDir(file.getParent());
        boolean append = false;
        if (position > 0) {
            append = true;
            if (file.length() < position) {
                throw new ArFileException("Cannot Change position in getFileOutputStream: file is smaller than required position");
            }
            RandomAccessFile raf = getRandomFile(file, position);
            try {
                raf.setLength(position);
                raf.close();
            } catch (IOException e) {
                throw new ArFileException("Change position in getFileOutputStream:", e);
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            throw new ArFileException("File not found in getRandomFile:", e);
        }
        return fos;
    }

    /**
     * Returns the RandomAccessFile in Out mode associated with the current
     * file. Used when position is greater than 0.
     * 
     * @param file
     * @param position
     *            greater than 0 if the FileOutputStream should be in append
     *            mode
     * @return the RandomAccessFile (OUT="rw")
     * @throws ArFileException
     */
    private RandomAccessFile getRandomFile(File file, long position) throws ArFileException {
        Configuration.arDir.createDir(file.getParent());
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.seek(position);
        } catch (FileNotFoundException e) {
            throw new ArFileException("File not found in getRandomFile:", e);
        } catch (IOException e) {
            throw new ArFileException("Change position in getRandomFile:", e);
        }
        return raf;
    }

    /**
     * Returns the FileChannel in Out mode associated with the current file.
     * 
     * @param file
     * @param position
     *            greater than 0 if the FileOutputStream should be in append
     *            mode
     * @return the FileChannel (OUT mode)
     * @throws ArFileException
     */
    public FileChannel getFileChannelOut(File file, long position) throws ArFileException {
        FileOutputStream fileOutputStream = getFileOutputStream(file, position);
        FileChannel fileChannel = fileOutputStream.getChannel();
        if (position != 0) {
            try {
                fileChannel = fileChannel.position(position);
            } catch (IOException e) {
                throw new ArFileException("Change position in getFileChannelOut:", e);
            }
        }
        return fileChannel;
    }

    /**
     * Returns the FileInputStream in In mode associated for the file.
     * 
     * @param file
     * @param position
     *            greater than 0 if the FileInputStream means skipping position
     *            bytes
     * @return the FileInputStream (IN)
     * @throws ArFileException
     */
    public FileInputStream getFileInputStream(File file, long position) throws ArFileException {
        Configuration.arDir.createDir(file.getParent());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            if (position > 0) fis.skip(position);
        } catch (FileNotFoundException e) {
            throw new ArFileException("File not found in getFileInputStream:", e);
        } catch (IOException e) {
            throw new ArFileException("Change position in getFileInputStream:", e);
        }
        return fis;
    }

    /**
     * Returns the FileChannel in In mode associated with the current file.
     * 
     * @param file
     * @param position
     *            greater than 0 if the FileInputStream means skipping position
     *            bytes
     * @return the FileChannel (IN mode)
     * @throws ArFileException
     */
    public FileChannel getFileChannelIn(File file, long position) throws ArFileException {
        FileInputStream fileInputStream = getFileInputStream(file, 0);
        FileChannel fileChannel = fileInputStream.getChannel();
        if (position != 0) {
            try {
                fileChannel = fileChannel.position(position);
            } catch (IOException e) {
                throw new ArFileException("Change position in getFileChannelIn:", e);
            }
        }
        return fileChannel;
    }
}
