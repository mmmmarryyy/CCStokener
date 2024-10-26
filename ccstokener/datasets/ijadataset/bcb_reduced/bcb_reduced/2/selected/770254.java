package org.pachyderm.assetdb.eof;

import java.net.URL;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.pachyderm.apollo.core.CXDirectoryServices;
import org.pachyderm.apollo.core.UTType;
import org.pachyderm.apollo.core.eof.CXDirectoryPersonEO;
import org.pachyderm.apollo.data.CXManagedObject;
import org.pachyderm.apollo.data.CXManagedObjectMetadata;
import org.pachyderm.apollo.data.CXURLObject;
import org.pachyderm.apollo.data.MD;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSSet;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSURL;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXProperties;

public class AssetDBRecord extends _AssetDBRecord implements CXManagedObjectMetadata {

    private static Logger LOG = Logger.getLogger(AssetDBRecord.class);

    private static final long serialVersionUID = 2416830371724337621L;

    private Boolean logInAwake = ERXProperties.booleanForKeyWithDefault("pachy.logInAwakeMethods", false);

    private CXManagedObject _managedObjectRef = null;

    private static final NSSet<?> _InspectableAttributes = new NSSet<Object>(new Object[] { ACCESS_RIGHTS_KEY, "permissions", ALTERNATIVE_KEY, ALT_TEXT_KEY, AUDIENCE_KEY, CONTRIBUTOR_KEY, MD.Authors, COVERAGE_KEY, CREATED_KEY, CREATOR_KEY, DATE_KEY, "dateAccepted", "dateCopyrighted", "copyright", "dateIssued", "dateModified", MD.AttributeChangeDate, "dateSubmitted", "date_added", "description", MD.Description, "educationLevel", "extent", "format", "identifier", "identifieruri", "language", "license", "location", "longDesc", "mediaLabel", "Tombstone", "mediator", "medium", PROVENANCE_KEY, "publisher", "relation", "rights", "rightsHolder", "owner", "source", "spatial", "subject", MD.Keywords, "subjectTokens", "syncCaption", "temporal", "title", MD.Title, MD.DisplayName, "transcript", "type", MD.ContentType, "valid", "isdeleted", "uid" });

    private static final NSSet<?> _CollisionAttributes = new NSSet<Object>(new Object[] { "identifier", MD.Title, "ContentType", MD.ContentType, "Tombstone", MD.AttributeChangeDate });

    private static final NSSet<?> _MutableAttributes = null;

    @Override
    public void awakeFromFetch(EOEditingContext ec) {
        super.awakeFromFetch(ec);
        LOG.trace("----->  awakeFromFetch: (" + ec + ") " + "EOs: (" + ec.registeredObjects().count() + "), +(" + ec.insertedObjects().count() + "), " + "~(" + ec.updatedObjects().count() + "), -(" + ec.deletedObjects().count() + ")");
        if (logInAwake) {
            @SuppressWarnings("unchecked") NSArray<ERXGenericRecord> genericRecords = ec.registeredObjects();
            for (ERXGenericRecord genericRecord : genericRecords) LOG.trace("EOs: " + genericRecord);
        }
    }

    @Override
    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
        LOG.info("-----> awakeFromInsert: (" + ec + ") " + "EOs: (" + ec.registeredObjects().count() + "), +(" + ec.insertedObjects().count() + "), " + "~(" + ec.updatedObjects().count() + "), -(" + ec.deletedObjects().count() + ")");
        CXDirectoryPersonEO person = CXDirectoryServices.sessionPerson();
        if (person != null) setRightsHolder(person.stringId());
        setAccessRights("0");
        setDateSubmitted();
        setDateModified();
        LOG.info("<----- awakeFromInsert");
    }

    public CXManagedObject managedObject() {
        if (_managedObjectRef == null) {
            _managedObjectRef = CXURLObject.objectWithURL(location());
            _managedObjectRef.attachMetadata(this);
        }
        return _managedObjectRef;
    }

    public void willUpdate() {
        LOG.info("-----> willUpdate .. setDateModified()");
        setDateModified();
        super.willUpdate();
    }

    /**
   * Returns NSURL object representing the object store from which this asset originates
   * 
   * @return NSSURL object store URL
   */
    public NSURL storeURL() {
        return new NSURL("jdbc:mysql://localhost/pachyderm22");
    }

    /**
   * Returns NSSet object filled with inspectable Attributes for this asset
   */
    public NSSet<?> inspectableAttributes() {
        return _InspectableAttributes;
    }

    /**
   * Returns NSSet object filled with mutable Attributes for this asset
   */
    public NSSet<?> mutableAttributes() {
        return _MutableAttributes;
    }

    /**
   * Returns Object value for attribute string. Attribute string must be present in the set of inspectable attributes.
   */
    public Object valueForAttribute(String attribute) {
        if (!(_InspectableAttributes.containsObject(attribute))) {
            return null;
        }
        return (_CollisionAttributes.containsObject(attribute)) ? valueForKey("p" + attribute) : valueForKey(attribute);
    }

    public static AssetDBRecord fetchPXMetadata(EOEditingContext editingContext, String keyString) {
        try {
            return (AssetDBRecord) EOUtilities.objectWithPrimaryKeyValue(editingContext, AssetDBRecord.ENTITY_NAME, Integer.valueOf(keyString));
        } catch (java.lang.NumberFormatException e) {
            return null;
        }
    }

    public String getFilename() {
        return NSPathUtilities.lastPathComponent(location());
    }

    /**
   * returns String value of primary key from the database for the record.
   * 
   * @return String primary key id
   */
    public String identifier() {
        NSDictionary<?, ?> pkDict = EOUtilities.primaryKeyForObject(editingContext(), this);
        return pkDict.objectForKey("identifier").toString();
    }

    /**
   * Returns an NSArray populated with the tokenized return value of subject().
   */
    public NSArray<String> subjectTokens() {
        String keywords = subject();
        if (keywords == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(keywords, ";,");
        int count = tokenizer.countTokens();
        if (count < 2) {
            tokenizer = new StringTokenizer(keywords);
            count = tokenizer.countTokens();
        }
        NSMutableArray<String> tokens = new NSMutableArray<String>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            tokens.addObject(tokenizer.nextToken().trim());
        }
        return tokens;
    }

    public String prightsHolder() {
        return rightsHolder();
    }

    public String pRightsHolder() {
        return rightsHolder();
    }

    public NSTimestamp pAttributeChangeDate() {
        return dateModified();
    }

    /**
   * Returns an NSArray object populated with comma-delimited value of DC:Contributor attribute of metadata record for asset.
   */
    public NSArray<String> pAuthors() {
        return new NSArray<String>(contributor());
    }

    public String pTitle() {
        return title();
    }

    /**
   * Returns UTI (Uniform Type Identifier) for asset, either derived from the value of DC:Type or from the content 
   * type returned from the URL of the asset. (Note: should probably derive not from DC:Type, but from DC:Format)
   */
    public String pContentType() {
        String type = type();
        if (type == null) {
            try {
                URL url = new URL(location());
                type = url.openConnection().getContentType();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        String uti = null;
        if (type != null) {
            uti = UTType.preferredIdentifierForTag(UTType.MIMETypeTagClass, type, null);
        }
        if (uti == null) {
            String file = location();
            if (file != null) {
                uti = UTType.preferredIdentifierForTag(UTType.FilenameExtensionTagClass, (NSPathUtilities.pathExtension(file)).toLowerCase(), null);
            }
            if (uti == null) {
                uti = UTType.Item;
            }
        }
        return uti;
    }

    public void setDateSubmitted() {
        setDateSubmitted(new NSTimestamp());
    }

    public void setDateModified() {
        setDateModified(new NSTimestamp());
    }

    public Integer getOwner() {
        return Integer.decode(rightsHolder());
    }

    public void setOwner(Integer value) {
        setRightsHolder(value.toString());
    }

    public Integer getPermissions() {
        return Integer.decode(accessRights());
    }

    public void setPermissions(Integer value) {
        setAccessRights(value.toString());
    }

    public void setValid(Boolean valid) {
        setValid((valid) ? "0" : "1");
    }

    public Boolean isValid() {
        return valid() == null || valid().equals("0");
    }

    public Boolean isDeleted() {
        return valid().equals("1");
    }

    public String toString() {
        return "<AssetDBRecord (id: " + identifier() + ", modDate: " + dateModified() + ")";
    }

    public void setValueForAttribute(Object value, String attribute) {
    }

    public boolean hasNativeDataRepresentation() {
        return false;
    }

    public String nativeDataRepresentationType() {
        return null;
    }

    public NSData nativeDataRepresentation() {
        return null;
    }
}
