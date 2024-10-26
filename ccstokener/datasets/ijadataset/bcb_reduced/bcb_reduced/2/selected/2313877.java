package edu.mit.osidimpl.repository.cache;

import edu.mit.osidimpl.manager.*;
import edu.mit.osidimpl.repository.shared.*;

/**
 * Asset manages the Asset itself.  Assets have content as well as Records
 * appropriate to the AssetType and RecordStructures for the Asset.  Assets
 * may also contain other Assets.
 * 
 * <p>
 * OSID Version: 2.0
 * </p>
 * 
 * <p>
 * Licensed under the {@link org.osid.SidImplementationLicenseMIT MIT
 * O.K.I&#46; OSID Definition License}.
 * </p>
 */
public class Asset implements org.osid.repository.Asset {

    private org.osid.repository.Asset asset;

    private RepositoryManager mgr;

    private Repository repository;

    private OsidLogger logger;

    private boolean cached = false;

    private long length = 0;

    private long lastModified = 0;

    private String mimeType;

    /**
     *  Constructs a new <code>Asset</code>
     */
    public Asset(Repository repository, org.osid.repository.Asset asset) {
        this.mgr = repository.mgr;
        this.repository = repository;
        this.asset = asset;
        this.logger = mgr.logger;
    }

    /**
     *  Update the display name for this Asset.
     *
     *  @param displayName
     *  @throws org.osid.repository.RepositoryException An exception with 
     *          one of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}, {@link
     *          org.osid.repository.RepositoryException#PERMISSION_DENIED
     *          PERMISSION_DENIED}, {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}
     */
    public void updateDisplayName(String displayName) throws org.osid.repository.RepositoryException {
        logger.logMethod(displayName);
        this.asset.updateDisplayName(displayName);
        return;
    }

    /**
     *  Update the date at which this Asset is effective.
     *
     *  @param effectiveDate (the number of milliseconds since January 1, 
     *         1970, 00:00:00 GMT)
     *  @throws org.osid.repository.RepositoryException An exception with 
     *          one of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public void updateEffectiveDate(long effectiveDate) throws org.osid.repository.RepositoryException {
        logger.logMethod(effectiveDate);
        this.asset.updateEffectiveDate(effectiveDate);
        return;
    }

    /**
     *  Update the date at which this Asset expires.
     *
     *  @param expirationDate (the number of milliseconds since January 1, 
     *         1970, 00:00:00 GMT)
     *  @throws org.osid.repository.RepositoryException An exception with 
     *          one of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public void updateExpirationDate(long expirationDate) throws org.osid.repository.RepositoryException {
        logger.logMethod(expirationDate);
        this.asset.updateEffectiveDate(expirationDate);
        return;
    }

    /**
     *  Get the display name for this Asset.
     *
     *  @return String the display name
     *  @throws org.osid.repository.RepositoryException
     */
    public String getDisplayName() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getDisplayName());
    }

    /**
     *  Get the description for this Asset.
     *
     *  @return String the description
     *  @throws org.osid.repository.RepositoryException
     */
    public String getDescription() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getDescription());
    }

    /**
     *  Get the unique Id for this Asset.
     *
     *  @return org.osid.shared.Id A unique Id that is usually set by a create
     *          method's implementation.
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.shared.Id getId() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getId());
    }

    /**
     *  Get the AssetType of this Asset.  AssetTypes are used to categorize
     *  Assets.
     *
     *  @return org.osid.shared.Type
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.shared.Type getAssetType() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getAssetType());
    }

    /**
     *  Get the date at which this Asset is effective.
     *
     *  @return long the number of milliseconds since January 1, 1970, 00:00:00
     *          GMT
     *  @throws org.osid.repository.RepositoryException
     */
    public long getEffectiveDate() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getEffectiveDate());
    }

    /**
     *  Get the date at which this Asset expires.
     *
     *  @return long the number of milliseconds since January 1, 1970, 00:00:00
     *          GMT
     *  @throws org.osid.repository.RepositoryException
     */
    public long getExpirationDate() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getExpirationDate());
    }

    /**
     *  Update the description for this Asset.
     *
     *  @param description
     *  @throws org.osid.repository.RepositoryException An exception with one of
     *          the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}, {@link
     *          org.osid.repository.RepositoryException#PERMISSION_DENIED
     *          PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}
     */
    public void updateDescription(String description) throws org.osid.repository.RepositoryException {
        logger.logMethod(description);
        this.asset.updateDescription(description);
        return;
    }

    /**
     *  Get the Id of the Repository in which this Asset resides.  This is set
     *  by the Repository's createAsset method.
     *
     *  @return org.osid.shared.Id A unique Id that is usually set by a create
     *          method's implementation. repositoryId
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.shared.Id getRepository() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.repository.getId());
    }

    /**
     *  Get an Asset's content.  This method can be a convenience if one is not
     *  interested in all the structure of the Records. This method returns a 
     *  ByteValueIterator through the Serialzable interface.
     *
     *  @return java.io.Serializable
     *  @throws org.osid.repository.RepositoryException An exception with 
     *          one of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}
     */
    public java.io.Serializable getContent() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        if (!this.cached) {
            logger.logTrace("not cached.. getting content");
            Object object = this.asset.getContent();
            if (object instanceof String) {
                String s = (String) object;
                if (s.startsWith("http://")) {
                    try {
                        java.net.URL url = new java.net.URL(s);
                        java.io.InputStream is = url.openStream();
                        java.io.File file = getCacheFile();
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                        int len;
                        byte[] b = new byte[10240];
                        this.length = 0;
                        while ((len = is.read(b)) >= 0) {
                            fos.write(b, 0, len);
                            this.length += len;
                        }
                        fos.close();
                        is.close();
                        java.net.URLConnection urlc = new java.net.URL(s).openConnection();
                        this.lastModified = urlc.getLastModified();
                        this.mimeType = urlc.getContentType();
                    } catch (java.io.IOException ie) {
                        logger.logError("error writing file", ie);
                    }
                }
            }
            this.cached = true;
        } else {
            logger.logTrace("cached..");
        }
        try {
            return (new SerializableInputStream(new java.io.FileInputStream(getCacheFile())));
        } catch (java.io.IOException ie) {
            logger.logError("cannot get content", ie);
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
    }

    /**
     *  Update an Asset's content.
     *
     *  @param content
     *  @throws org.osid.repository.RepositoryException An exception with 
     *          one of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}, {@link
     *          org.osid.repository.RepositoryException#PERMISSION_DENIED
     *          PERMISSION_DENIED}, {@link
     *          org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *          CONFIGURATION_ERROR}, {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}, {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}
     */
    public void updateContent(java.io.Serializable content) throws org.osid.repository.RepositoryException {
        logger.logMethod(content);
        this.asset.updateContent(content);
        return;
    }

    /**
     *  Add an Asset to this Asset.
     *
     *  @param assetId
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public void addAsset(org.osid.shared.Id assetId) throws org.osid.repository.RepositoryException {
        logger.logMethod(assetId);
        this.asset.addAsset(assetId);
        return;
    }

    /**
     *  Remove an Asset from this Asset.  This method does not delete the Asset
     * from the Repository.
     *
     *  @param assetId
     *  @param includeChildren
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public void removeAsset(org.osid.shared.Id assetId, boolean includeChildren) throws org.osid.repository.RepositoryException {
        logger.logMethod(assetId, includeChildren);
        this.asset.removeAsset(assetId, includeChildren);
        return;
    }

    /**
     *  Get all the Assets in this Asset. Iterators return a set, one at a
     *  time.
     *
     *  @return AssetIterator The order of the objects returned by the Iterator
     *          is not guaranteed.
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.repository.AssetIterator getAssets() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getAssets());
    }

    /**
     *  Get all the Assets of the specified AssetType in this Asset.
     *  Iterators return a set, one at a time.
     *
     *  @return AssetIterator The order of the objects returned by the Iterator
     *          is not guaranteed.
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type assetType) throws org.osid.repository.RepositoryException {
        logger.logMethod(assetType);
        return (this.asset.getAssetsByType(assetType));
    }

    /**
     *  Create a new Asset Record of the specified RecordStructure.   The
     *  implementation of this method sets the Id for the new object.
     *
     *  @param recordStructureId
     *  @return Record
     *
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}, {@link
     *          org.osid.repository.RepositoryException#PERMISSION_DENIED
     *          PERMISSION_DENIED}, {@link
     *          org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *          CONFIGURATION_ERROR}, {@link
     *          org.osid.repository.RepositoryException#UNIMPLEMENTED
     *          UNIMPLEMENTED}, {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}, {@link
     *          org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.Record createRecord(org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(recordStructureId);
        return (this.asset.createRecord(recordStructureId));
    }

    /**
     * Add the specified RecordStructure and all the related Records from the
     * specified asset.  The current and future content of the specified
     * Record is synchronized automatically.
     *
     * @param assetId
     * @param recordStructureId
     *
     * @throws org.osid.repository.RepositoryException An exception with one of
     *         the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#PERMISSION_DENIED
     *         PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *         CONFIGURATION_ERROR}, {@link
     *         org.osid.repository.RepositoryException#UNIMPLEMENTED
     *         UNIMPLEMENTED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID},
     *         {@link
     *         org.osid.repository.RepositoryException#ALREADY_INHERITING_STRUCTURE
     *         ALREADY_INHERITING_STRUCTURE}
     */
    public void inheritRecordStructure(org.osid.shared.Id assetId, org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(assetId, recordStructureId);
        this.asset.inheritRecordStructure(assetId, recordStructureId);
        return;
    }

    /**
     * Add the specified RecordStructure and all the related Records from the
     * specified asset.
     *
     * @param assetId
     * @param recordStructureId
     *
     * @throws org.osid.repository.RepositoryException An exception with one of
     *         the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#PERMISSION_DENIED
     *         PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *         CONFIGURATION_ERROR}, {@link
     *         org.osid.repository.RepositoryException#UNIMPLEMENTED
     *         UNIMPLEMENTED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID},
     *         {@link
     *         org.osid.repository.RepositoryException#CANNOT_COPY_OR_INHERIT_SELF
     *         CANNOT_COPY_OR_INHERIT_SELF}
     */
    public void copyRecordStructure(org.osid.shared.Id assetId, org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(assetId, recordStructureId);
        this.asset.copyRecordStructure(assetId, recordStructureId);
        return;
    }

    /**
     * Delete a Record.  If the specified Record has content that is inherited
     * by other Records, those other Records will not be deleted, but they
     * will no longer have a source from which to inherit value changes.
     *
     * @param recordId
     *
     * @throws org.osid.repository.RepositoryException An exception with one of
     *         the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#PERMISSION_DENIED
     *         PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *         CONFIGURATION_ERROR}, {@link
     *         org.osid.repository.RepositoryException#UNIMPLEMENTED
     *         UNIMPLEMENTED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public void deleteRecord(org.osid.shared.Id recordId) throws org.osid.repository.RepositoryException {
        logger.logMethod(recordId);
        this.asset.deleteRecord(recordId);
        return;
    }

    /**
     *  Get all the Records for this Asset.  Iterators return a set, one at a
     *  time.
     *  @return RecordIterator  The order of the objects returned by the
     *          Iterator is not guaranteed.
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.repository.RecordIterator getRecords() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getRecords());
    }

    /**
     *  Get all the Records of the specified RecordStructure for this Asset.
     *  Iterators return a set, one at a time.
     *
     *  @param recordStructureId
     *  @return RecordIterator  The order of the objects returned by the
     *          Iterator is not guaranteed.
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}, {@link
     *          org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.RecordIterator getRecordsByRecordStructure(org.osid.shared.Id recordStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(recordStructureId);
        return (this.asset.getRecordsByRecordStructure(recordStructureId));
    }

    /**
     *  Get all the Records of the specified RecordStructureType for this Asset.
     *  Iterators return a set, one at a time.
     *
     *  @param recordStructureType
     *  @return RecordIterator  The order of the objects returned by the
     *         Iterator is not guaranteed.
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.RecordIterator getRecordsByRecordStructureType(org.osid.shared.Type recordStructureType) throws org.osid.repository.RepositoryException {
        logger.logMethod(recordStructureType);
        return (this.asset.getRecordsByRecordStructureType(recordStructureType));
    }

    /**
     *  Get all the RecordStructures for this Asset.  RecordStructures are used
     *  to categorize information about Assets.  Iterators return a set, one at
     *  a time.
     *
     *  @return RecordStructureIterator  The order of the objects returned by
     *          the Iterator is not guaranteed.
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.repository.RecordStructureIterator getRecordStructures() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getRecordStructures());
    }

    /**
     *  Get the RecordStructure associated with this Asset's content.
     *
     *  @return RecordStructure
     *  @throws org.osid.repository.RepositoryException
     */
    public org.osid.repository.RecordStructure getContentRecordStructure() throws org.osid.repository.RepositoryException {
        logger.logMethod();
        return (this.asset.getContentRecordStructure());
    }

    /**
     *  Get the Record for this Asset that matches this Record's unique Id.
     *
     *  @param recordId
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}, {@link
     *          org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.Record getRecord(org.osid.shared.Id recordId) throws org.osid.repository.RepositoryException {
        logger.logMethod(recordId);
        return (this.asset.getRecord(recordId));
    }

    /**
     *  Get the Part for a Record for this Asset that matches this Part's 
     *  unique Id.
     *
     *  @param partId
     *  @throws org.osid.repository.RepositoryException An exception with one
     *         of the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.Part getPart(org.osid.shared.Id partId) throws org.osid.repository.RepositoryException {
        logger.logMethod(partId);
        return (this.asset.getPart(partId));
    }

    /**
     *  Get the Value of the Part of the Record for this Asset that matches
     *  this Part's unique Id.
     *
     *  @param partId
     *  @throws org.osid.repository.RepositoryException An exception with one
     *          of the following messages defined in
     *          org.osid.repository.RepositoryException may be thrown: {@link
     *          org.osid.repository.RepositoryException#OPERATION_FAILED
     *          OPERATION_FAILED}, {@link
     *          org.osid.repository.RepositoryException#NULL_ARGUMENT
     *          NULL_ARGUMENT}, {@link
     *          org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public java.io.Serializable getPartValue(org.osid.shared.Id partId) throws org.osid.repository.RepositoryException {
        logger.logMethod(partId);
        return (this.asset.getPartValue(partId));
    }

    /**
     *  Get the Parts of the Records for this Asset that are based on this
     *  RecordStructure PartStructure's unique Id.
     *
     *  @return partStructureId
     *  @throws org.osid.repository.RepositoryException An exception with one
     *         of the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#PERMISSION_DENIED
     *         PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *         CONFIGURATION_ERROR}, {@link
     *         org.osid.repository.RepositoryException#UNIMPLEMENTED
     *         UNIMPLEMENTED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.repository.PartIterator getPartsByPartStructure(org.osid.shared.Id partStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(partStructureId);
        return (this.asset.getPartsByPartStructure(partStructureId));
    }

    /**
     * Get the Values of the Parts of the Records for this Asset that are based
     * on this RecordStructure PartStructure's unique Id.
     *
     * @return partStructureId
     *
     * @throws org.osid.repository.RepositoryException An exception with one of
     *         the following messages defined in
     *         org.osid.repository.RepositoryException may be thrown: {@link
     *         org.osid.repository.RepositoryException#OPERATION_FAILED
     *         OPERATION_FAILED}, {@link
     *         org.osid.repository.RepositoryException#PERMISSION_DENIED
     *         PERMISSION_DENIED}, {@link
     *         org.osid.repository.RepositoryException#CONFIGURATION_ERROR
     *         CONFIGURATION_ERROR}, {@link
     *         org.osid.repository.RepositoryException#UNIMPLEMENTED
     *         UNIMPLEMENTED}, {@link
     *         org.osid.repository.RepositoryException#NULL_ARGUMENT
     *         NULL_ARGUMENT}, {@link
     *         org.osid.repository.RepositoryException#UNKNOWN_ID UNKNOWN_ID}
     */
    public org.osid.shared.ObjectIterator getPartValuesByPartStructure(org.osid.shared.Id partStructureId) throws org.osid.repository.RepositoryException {
        logger.logMethod(partStructureId);
        return (this.asset.getPartValuesByPartStructure(partStructureId));
    }

    private java.io.File getCacheFile() throws java.io.IOException {
        java.io.File file = new java.io.File(this.mgr.asset_cache);
        try {
            file = new java.io.File(file, this.repository.getId().getIdString());
            if (!file.exists()) {
                logger.logTrace("creating directory: " + file.getPath());
                file.mkdirs();
            }
            file = new java.io.File(file, this.asset.getId().getIdString());
        } catch (org.osid.shared.SharedException se) {
        }
        return (file);
    }

    public long getLength() {
        return (this.length);
    }

    public long getLastModified() {
        return (this.lastModified);
    }

    public String getMimeType() {
        return (this.mimeType);
    }
}
