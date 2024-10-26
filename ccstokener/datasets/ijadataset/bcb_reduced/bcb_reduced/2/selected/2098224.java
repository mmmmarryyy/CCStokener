package org.nmc.pachyderm.assetdb;

import ca.ucalgary.apollo.core.*;
import ca.ucalgary.apollo.data.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.math.BigDecimal;
import java.util.*;
import java.net.*;

public class AssetDBRecord extends EOGenericRecord implements CXManagedObjectMetadata {

    private CXManagedObject _managedObjectRef = null;

    private static final NSSet _InspectableAttributes = new NSSet(new Object[] { MD.AttributeChangeDate, MD.ContentType, MD.Keywords, MD.Title, MD.Description, MD.Authors, MD.DisplayName, "Tombstone", "relation", "uid" });

    private static final NSSet _MutableAttributes = null;

    public AssetDBRecord() {
        super();
    }

    public CXManagedObject managedObject() {
        if (_managedObjectRef == null) {
            _managedObjectRef = CXURLObject.objectWithURL(location());
            _managedObjectRef.attachMetadata(this);
        }
        return _managedObjectRef;
    }

    public NSURL storeURL() {
        AssetDBObjectStore store = (AssetDBObjectStore) editingContext().delegate();
        return store.url();
    }

    public String identifier() {
        NSDictionary pkDict = EOUtilities.primaryKeyForObject(editingContext(), this);
        return pkDict.objectForKey("id").toString();
    }

    public NSSet inspectableAttributes() {
        return _InspectableAttributes;
    }

    public NSSet mutableAttributes() {
        return _MutableAttributes;
    }

    public Object valueForAttribute(String attribute) {
        return _InspectableAttributes.containsObject(attribute) ? valueForKey("p" + attribute) : null;
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

    public NSTimestamp pAttributeChangeDate() {
        return date_modified();
    }

    public String pContentType() {
        String type = type();
        String uti = null;
        if (type == null) {
            try {
                URL url = new URL(location());
                type = url.openConnection().getContentType();
            } catch (java.io.IOException e) {
            }
        }
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

    public NSArray pKeywords() {
        String keywords = keywords();
        if (keywords == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(keywords, ";,");
        int count = tokenizer.countTokens();
        if (count < 2) {
            tokenizer = new StringTokenizer(keywords);
            count = tokenizer.countTokens();
        }
        NSMutableArray tokens = new NSMutableArray(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            tokens.addObject(tokenizer.nextToken().trim());
        }
        return tokens;
    }

    public String pTitle() {
        return title();
    }

    public String pDescription() {
        return description();
    }

    public NSArray pAuthors() {
        return new NSArray(contributor());
    }

    public String pTombstone() {
        return tombstone();
    }

    public String pDisplayName() {
        return title();
    }

    public String pRelation() {
        return relation();
    }

    public String puid() {
        return (String) this.storedValueForKey("uid");
    }

    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
        CXDirectoryPerson person = CXDirectoryServices.currentPerson();
        if (person != null) {
            takeStoredValueForKey(person.uniqueId(), "uid");
        }
        takeStoredValueForKey(new Integer(0), "permissions");
    }

    public Integer isDeleted() {
        return (Integer) storedValueForKey("isDeleted");
    }

    public void setIsDeleted(Integer value) {
        takeStoredValueForKey(value, "isDeleted");
        setDate_modified();
    }

    public Integer permissions() {
        return (Integer) storedValueForKey("permissions");
    }

    public void setPermissions(Integer value) {
        takeStoredValueForKey(value, "permissions");
        setDate_modified();
    }

    public String title() {
        return (String) storedValueForKey("title");
    }

    public void setTitle(String value) {
        takeStoredValueForKey(value, "title");
        setDate_modified();
    }

    public String tombstone() {
        return (String) storedValueForKey("tombstone");
    }

    public void setTombstone(String value) {
        takeStoredValueForKey(value, "tombstone");
        setDate_modified();
    }

    public String description() {
        return (String) storedValueForKey("description");
    }

    public void setDescription(String value) {
        takeStoredValueForKey(value, "description");
        setDate_modified();
    }

    public String keywords() {
        return (String) storedValueForKey("keywords");
    }

    public void setKeywords(String value) {
        takeStoredValueForKey(value, "keywords");
        setDate_modified();
    }

    public String location() {
        return (String) storedValueForKey("location");
    }

    public void setLocation(String value) {
        takeStoredValueForKey(value, "location");
        setDate_modified();
    }

    public String format() {
        return (String) storedValueForKey("format");
    }

    public void setFormat(String value) {
        takeStoredValueForKey(value, "format");
        setDate_modified();
    }

    public String owner() {
        return (String) storedValueForKey("owner");
    }

    public String contributor() {
        return (String) storedValueForKey("contributor");
    }

    public void setOwner(String value) {
        takeStoredValueForKey(value, "owner");
        setDate_modified();
    }

    public void setContributor(String value) {
        takeStoredValueForKey(value, "owner");
        setDate_modified();
    }

    public String copyright() {
        return (String) storedValueForKey("copyright");
    }

    public void setCopyright(String value) {
        takeStoredValueForKey(value, "copyright");
        setDate_modified();
    }

    public NSTimestamp date_added() {
        return (NSTimestamp) storedValueForKey("date_added");
    }

    public void setDate_added(NSTimestamp value) {
        takeStoredValueForKey(value, "date_added");
    }

    public NSTimestamp date_modified() {
        return (NSTimestamp) storedValueForKey("date_modified");
    }

    public void setDate_modified(NSTimestamp value) {
        takeStoredValueForKey(value, "date_modified");
    }

    public void setDate_modified() {
        NSTimestamp ts = new NSTimestamp();
        takeValueForKey(ts, "date_modified");
    }

    public String creator() {
        return (String) storedValueForKey("creator");
    }

    public void setCreator(String value) {
        takeStoredValueForKey(value, "creator");
        setDate_modified();
    }

    public String publisher() {
        return (String) storedValueForKey("publisher");
    }

    public void setPublisher(String value) {
        takeStoredValueForKey(value, "publisher");
        setDate_modified();
    }

    public String type() {
        return (String) storedValueForKey("type");
    }

    public void setType(String value) {
        takeStoredValueForKey(value, "type");
        setDate_modified();
    }

    public String source() {
        return (String) storedValueForKey("source");
    }

    public void setSource(String value) {
        takeStoredValueForKey(value, "source");
        setDate_modified();
    }

    public String language() {
        return (String) storedValueForKey("language");
    }

    public void setLanguage(String value) {
        takeStoredValueForKey(value, "language");
        setDate_modified();
    }

    public String relation() {
        return (String) storedValueForKey("relation");
    }

    public void setRelation(String value) {
        takeStoredValueForKey(value, "relation");
        setDate_modified();
    }

    public String coverage() {
        return (String) storedValueForKey("coverage");
    }

    public void setCoverage(String value) {
        takeStoredValueForKey(value, "coverage");
        setDate_modified();
    }
}
