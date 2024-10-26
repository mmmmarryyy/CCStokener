package org.pachyderm.assetdb;

import java.net.URL;
import java.util.StringTokenizer;
import org.pachyderm.apollo.core.CXDirectoryPerson;
import org.pachyderm.apollo.core.CXDirectoryServices;
import org.pachyderm.apollo.core.UTType;
import org.pachyderm.apollo.data.CXManagedObject;
import org.pachyderm.apollo.data.CXManagedObjectMetadata;
import org.pachyderm.apollo.data.CXURLObject;
import org.pachyderm.apollo.data.MD;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGenericRecord;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSSet;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSURL;

@SuppressWarnings("serial")
public class AssetDBRecord extends EOGenericRecord implements CXManagedObjectMetadata {

    /**
	 * 
	 */
    private CXManagedObject _managedObjectRef = null;

    /**
	 * 
	 */
    private static final NSSet _InspectableAttributes = new NSSet(new Object[] { "accessRights", "permissions", "alternative", "altText", "audience", "contributor", MD.Authors, "coverage", "created", "creator", "date", "dateAccepted", "dateCopyrighted", "copyright", "dateIssued", "dateModified", MD.AttributeChangeDate, "dateSubmitted", "date_added", "description", MD.Description, "educationLevel", "extent", "format", "identifier", "identifieruri", "language", "license", "location", "longDesc", "mediaLabel", "Tombstone", "mediator", "medium", "provenance", "publisher", "relation", "rights", "rightsHolder", "owner", "source", "spatial", "subject", MD.Keywords, "subjectTokens", "syncCaption", "temporal", "title", MD.Title, MD.DisplayName, "transcript", "type", MD.ContentType, "valid", "isdeleted", "uid" });

    private static final NSSet _MutableAttributes = null;

    private static final NSSet _CollisionAttributes = new NSSet(new Object[] { "identifier", MD.Title, "ContentType", MD.ContentType, "Tombstone", MD.AttributeChangeDate });

    /**
	 * Constructor
	 *
	 */
    public AssetDBRecord() {
        super();
    }

    /**
	 * Returns CXManagedObject object for the asset
	 * @return CXManagedObject object representing asset
	 */
    public CXManagedObject managedObject() {
        if (_managedObjectRef == null) {
            _managedObjectRef = CXURLObject.objectWithURL(location());
            _managedObjectRef.attachMetadata(this);
        }
        return _managedObjectRef;
    }

    /**
	 * Returns NSURL object representing the object store URL for the object store from which this asset originates
	 * @return NSSURL object store URL
	 */
    public NSURL storeURL() {
        AssetDBObjectStore store = (AssetDBObjectStore) editingContext().delegate();
        return store.url();
    }

    /**
	 * Returns NSSet object filled with inspectable Attributes for this asset
	 * @return NSSet object of inspectable attributes.
	 */
    public NSSet inspectableAttributes() {
        return _InspectableAttributes;
    }

    /**
	 * Returns NSSet object filled with mutable attributes for this asset
	 * @return NSSet object of mutable attributes
	 */
    public NSSet mutableAttributes() {
        return _MutableAttributes;
    }

    /**
	 * Returns Object value for attribute string. Attribute string must be present in the set of 
	 * inspectable attributes.
	 *
	 * @return Object value for attribute string
	 */
    public Object valueForAttribute(String attribute) {
        if (!(_InspectableAttributes.containsObject(attribute))) {
            return null;
        }
        return (_CollisionAttributes.containsObject(attribute)) ? valueForKey("p" + attribute) : valueForKey(attribute);
    }

    /**
	 * Not implemented.
	 */
    public void setValueForAttribute(Object value, String attribute) {
    }

    /**
	 * Not implemented.
	 * @return boolean false
	 */
    public boolean hasNativeDataRepresentation() {
        return false;
    }

    /**
	 * Not implemented.
	 * @return String null
	 */
    public String nativeDataRepresentationType() {
        return null;
    }

    /**
	 * Not implemented.
	 * @return NSData null
	 */
    public NSData nativeDataRepresentation() {
        return null;
    }

    /**
	 * This method is called when an asset db record is inserted into the database. It sets the 'rightsHolder' value 
	 * to the user's person.uniqueId() value, and sets accessRights to '0'.
	 */
    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
        CXDirectoryPerson person = CXDirectoryServices.currentPerson();
        if (person != null) {
            takeStoredValueForKey(person.uniqueId(), "rightsHolder");
        }
        takeStoredValueForKey("0", "accessRights");
    }

    /**
	 * 
	 * @return
	 */
    public String accessRights() {
        return (String) storedValueForKey("accessRights");
    }

    /**
	 * 
	 * @param value
	 */
    public void setAccessRights(String value) {
        takeStoredValueForKey(value, "accessRights");
        setDateModified();
    }

    /**
	 * Returns a String containing the value for DCTERMS:Alternative (Alternative Title)
	 * 
	 * @return String value of DCTERMS:Alternative
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#alternative
	 */
    public String alternative() {
        return (String) storedValueForKey("alternative");
    }

    /**
	 * 
	 * @param value
	 */
    public void setAlternative(String value) {
        takeStoredValueForKey(value, "alternative");
        setDateModified();
    }

    /**
	 * 
	 * @return
	 */
    public String altText() {
        return (String) storedValueForKey("altText");
    }

    /**
	 * 
	 * @param value
	 */
    public void setAltText(String value) {
        takeStoredValueForKey(value, "altText");
        setDateModified();
    }

    /**
	 * 
	 * @return
	 */
    public String audience() {
        return (String) storedValueForKey("audience");
    }

    /**
	 * 
	 * @param value
	 */
    public void setAudience(String value) {
        takeStoredValueForKey(value, "audience");
        setDateModified();
    }

    /**
	 * Returns a String value corresponding to the DC:Contributor 
	 * attribute of the metadata record for the asset.
	 * 
	 * @return DC:Contributor attribute of metadata record for asset.
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#contributor
	 */
    public String contributor() {
        return (String) storedValueForKey("contributor");
    }

    /**
	 * Sets the value of DC:Contributor attribute of the metadata 
	 * record for the asset.
	 * 
	 * @param value value to set for DC:Contributor attribute
	 */
    public void setContributor(String value) {
        System.out.println("setting contributor to " + value + "\n");
        takeStoredValueForKey(value, "contributor");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Coverage attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Coverage attribute of metadata record for asset.
	 */
    public String coverage() {
        return (String) storedValueForKey("coverage");
    }

    /**
	 * Sets the value of DC:Coverage attribute of the metadata 
	 * record for the asset.
	 * 
	 * @param value value to set for DC:Coverage attribute
	 */
    public void setCoverage(String value) {
        takeStoredValueForKey(value, "coverage");
        setDateModified();
    }

    /**
	 * Returns the value of DCTERMS:Created attribute of the metadata record for the asset
	 * 
	 * @return String value of DCTERMS:Created attribute for asset
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#created
	 */
    public String created() {
        return (String) storedValueForKey("created");
    }

    /**
	 * Sets the value of DCTERMS:Created attribute of the metadata record for the asset.
	 * @param value value to set for DCTERMS:Created attribute
	 */
    public void setCreated(String value) {
        takeStoredValueForKey(value, "created");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Creator attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Creator attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#creator
	 */
    public String creator() {
        return (String) storedValueForKey("creator");
    }

    /**
	 * Sets the value of DC:Creator attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Creator attribute
	 */
    public void setCreator(String value) {
        takeStoredValueForKey(value, "creator");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Date attribute of the metadata record
	 * for the asset.
	 * 
	 * @return DC:Date attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#date
	 */
    public String date() {
        return (String) storedValueForKey("date");
    }

    /**
	 * Sets the value of DC:Date attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Date attribute
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#date
	 */
    public void setDate(String value) {
        takeStoredValueForKey(value, "date");
        setDateModified();
    }

    /**
	 * Returns string value of DCTERMS:DateAccepted attribute of the metadata record for the asset.
	 * 
	 * @return String value of DCTERMS:DateAccepted attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#dateAccepted
	 */
    public String dateAccepted() {
        return (String) storedValueForKey("dateAccepted");
    }

    /**
	 * 
	 * @param value
	 */
    public void setDateAccepted(String value) {
        takeStoredValueForKey(value, "dateAccepted");
        setDateModified();
    }

    /**
	 * Returns a String value of DCTERMS:DateCopyrighted attribute of the metadata record for the asset.
	 * 
	 * @return String value of DCTERMS:DateCopyrighted attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#dateCopyrighted
	 */
    public String dateCopyrighted() {
        return (String) storedValueForKey("dateCopyrighted");
    }

    /**
	 * 
	 * @param value
	 */
    public void setDateCopyrighted(String value) {
        takeStoredValueForKey(value, "dateCopyrighted");
        setDateModified();
    }

    /**
	 * Returns a String value of DCTERMS:Issued attribute of the metadata record for the asset.
	 * 
	 * @return String value of DCTERMS:Issued attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#issued
	 */
    public String dateIssued() {
        return (String) storedValueForKey("dateIssued");
    }

    /**
	 * 
	 * @param value
	 */
    public void setDateIssued(String value) {
        takeStoredValueForKey(value, "dateIssued");
        setDateModified();
    }

    /**
	 * Returns an NSTimestamp of the value of DCTERMS:DateModified attribute of the
	 * metadata record for the asset.
	 * 
	 * @return NSTimestamp of DCTERMS:DateModified attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#modified
	 */
    public NSTimestamp dateModified() {
        return (NSTimestamp) storedValueForKey("dateModified");
    }

    /**
	 * 
	 * @param value
	 */
    public void setDateModified(NSTimestamp value) {
        takeStoredValueForKey(value, "dateModified");
    }

    /**
	 * Sets the value of DCTERMS:Modified attribute of the metadata record
	 * for the asset to the current time.
	 */
    public void setDateModified() {
        NSTimestamp ts = new NSTimestamp();
        setDateModified(ts);
    }

    /**
	 * Returns an NSTimestamp of the value of DCTERMS:DateSubmitted 
	 * attribute of the metadata record for the asset.
	 * 
	 * @return NSTimestamp of DCTERMS:DateSubmitted attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#dateSubmitted
	 */
    public NSTimestamp dateSubmitted() {
        return (NSTimestamp) storedValueForKey("dateSubmitted");
    }

    /**
	 * Sets the value of DCTERMS:DateSubmitted attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:DateSubmitted attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#dateSubmitted
	 */
    public void setDateSubmitted(NSTimestamp value) {
        takeStoredValueForKey(value, "dateSubmitted");
        setDateModified();
    }

    /**
	 * Sets the value of DCTERMS:Modified attribute of the metadata record
	 * for the asset to the current time.
	 */
    public void setDateSubmitted() {
        NSTimestamp ts = new NSTimestamp();
        setDateSubmitted(ts);
    }

    /**
	 * Returns the value of DC:Description attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Description attrubite of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#description
	 */
    public String description() {
        return (String) storedValueForKey("description");
    }

    /**
	 * Sets the value of DC:Description attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Description attribute
	 */
    public void setDescription(String value) {
        takeStoredValueForKey(value, "description");
        setDateModified();
    }

    /**
	 * 
	 * @return
	 */
    public String educationLevel() {
        return (String) storedValueForKey("educationLevel");
    }

    /**
	 * 
	 * @param value
	 */
    public void setEducationLevel(String value) {
        takeStoredValueForKey(value, "educationLevel");
        setDateModified();
    }

    /**
	 * Returns String value of DCTERMS:Extent attribute
	 * 
	 * @return String value of DCTERMS:Extent attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#extent
	 */
    public String extent() {
        return (String) storedValueForKey("extent");
    }

    /**
	 * 
	 * @param value
	 */
    public void setExtent(String value) {
        takeStoredValueForKey(value, "extent");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Format attribute of the metadata record
	 * for the asset.
	 * 
	 * @return DC:Format attribute of metadata record for asset
	 */
    public String format() {
        return (String) storedValueForKey("format");
    }

    /**
	 * Sets the value of DC:Format attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Format attribute
	 */
    public void setFormat(String value) {
        takeStoredValueForKey(value, "format");
        setDateModified();
    }

    /**
	 * returns String value of primary key from the database for the record.
	 * 
	 * @return String primary key id
	 */
    public String identifier() {
        NSDictionary pkDict = EOUtilities.primaryKeyForObject(editingContext(), this);
        return pkDict.objectForKey("identifier").toString();
    }

    /**
	 * returns String value of Accession Number or DC Identifier from the database for the record.
	 * 
	 * @return String primary key id
	 */
    public String identifieruri() {
        return (String) storedValueForKey("identifieruri");
    }

    /**
	 * Sets the String value of DC:Identifier.URI attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value String value to set for DC:Language attribute
	 */
    public void identifieruri(String value) {
        takeStoredValueForKey(value, "identifieruri");
        setDateModified();
    }

    /**
	 * Returns a string value of DCTERMS:InstructionalMethod attribute of the metadata record
	 * for the asset. 
	 * 
	 * @return String DCTERMS:InstructionalMethod attribute of metadata record for asset.
	 */
    public String instructionalMethod() {
        return (String) storedValueForKey("instructionalMethod");
    }

    /**
	 * 
	 * @param value
	 */
    public void setInstructionalMethod(String value) {
        takeStoredValueForKey(value, "instructionalMethod");
        setDateModified();
    }

    /**
	 * Returns the String value of DC:Language attribute of the metadata 
	 * record for the asset.
	 * 
	 * @return String DC:Language attribute of metadata record for asset
	 */
    public String language() {
        return (String) storedValueForKey("language");
    }

    /**
	 * Sets the String value of DC:Language attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value String value to set for DC:Language attribute
	 */
    public void setLanguage(String value) {
        takeStoredValueForKey(value, "language");
        setDateModified();
    }

    /**
	 * Returns the String value of DCTERMS:License attribute of the metadata
	 * record for the asset.
	 * 
	 * @return String DCTERMS:License attribute of metadata record for asset
	 */
    public String license() {
        return (String) storedValueForKey("license");
    }

    /**
	 * 
	 * @param value
	 */
    public void setLicense(String value) {
        takeStoredValueForKey(value, "license");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Identifier.URL attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Identifier.URL attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#identifier
	 */
    public String location() {
        return (String) storedValueForKey("location");
    }

    /**
	 * Sets the value of DC:Identifier.URL attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Identifier.URL attribute
	 */
    public void setLocation(String value) {
        takeStoredValueForKey(value, "location");
        setDateModified();
    }

    /**
	 * Returns String value of accessibility long description
	 * 
	 * @return String value of accessibility long description
	 */
    public String longDesc() {
        return (String) storedValueForKey("longDesc");
    }

    /**
	 * 
	 * @param value
	 */
    public void setLongDesc(String value) {
        takeStoredValueForKey(value, "longDesc");
        setDateModified();
    }

    /**
	 * Returns String of media label (formerly tombstone) to be included as caption for asset
	 * 
	 * @return String media label value
	 */
    public String mediaLabel() {
        return (String) storedValueForKey("mediaLabel");
    }

    /**
	 * 
	 * @param value
	 */
    public void setMediaLabel(String value) {
        takeStoredValueForKey(value, "mediaLabel");
        setDateModified();
    }

    /**
	 * Returns String value of DCTERMS:Mediator attribute
	 * 
	 * @return String value of DCTERMS:Mediator attribute
	 */
    public String mediator() {
        return (String) storedValueForKey("mediator");
    }

    /**
	 * 
	 * @param value
	 */
    public void setMediator(String value) {
        takeStoredValueForKey(value, "mediator");
        setDateModified();
    }

    /**
	 * Returns String value of DCTERMS:Medium attribute
	 * 
	 * @return String value of DCTERMS:Medium attribute
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#medium
	 */
    public String medium() {
        return (String) storedValueForKey("medium");
    }

    /**
	 * 
	 * @param value
	 */
    public void setMedium(String value) {
        takeStoredValueForKey(value, "medium");
        setDateModified();
    }

    /**
	 * Returns String value of DCTERMS:Provenance attribute 
	 * 
	 * @return String value of DCTERMS:Provenance attribute
	 */
    public String provenance() {
        return (String) storedValueForKey("provenance");
    }

    /**
	 * 
	 * @param value
	 */
    public void setProvenance(String value) {
        takeStoredValueForKey(value, "provenance");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Publisher attribute of the metadata 
	 * record for the asset.
	 * 
	 * @return DC:Publisher attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#publisher
	 */
    public String publisher() {
        return (String) storedValueForKey("publisher");
    }

    /**
	 * Sets the value of DC:Publisher attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Publisher attribute
	 */
    public void setPublisher(String value) {
        takeStoredValueForKey(value, "publisher");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Relation attribute of the metadata 
	 * record for the asset.
	 * 
	 * @return DC:Relation attribute of metadata record for asset
	 */
    public String relation() {
        return (String) storedValueForKey("relation");
    }

    /**
	 * Sets the value of DC:Relation attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Relation attribute
	 */
    public void setRelation(String value) {
        takeStoredValueForKey(value, "relation");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Rights attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Rights attribute of metadata record for asset
	 */
    public String rights() {
        return (String) storedValueForKey("rights");
    }

    /**
	 * Sets the value of DC:Rights attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Rights attribute
	 */
    public void rights(String value) {
        takeStoredValueForKey(value, "rights");
        setDateModified();
    }

    /**
	 * Returns the value of DCTERMS:RightsHolder attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DCTERMS:RightsHolder attribute of metadata record for asset
	 */
    public String rightsHolder() {
        return (String) storedValueForKey("rightsHolder");
    }

    public String prightsHolder() {
        return rightsHolder();
    }

    public String pRightsHolder() {
        return rightsHolder();
    }

    /**
	 * Sets the value of DCTERMS:RightsHolder attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:RightsHolder attribute
	 */
    public void setRightsHolder(String value) {
        System.out.println("setting RightsHolder to " + value + "\n");
        takeStoredValueForKey(value, "rightsHolder");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Source attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Source attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#source
	 */
    public String source() {
        return (String) storedValueForKey("source");
    }

    /**
	 * Sets the value of DC:Source attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Source attribute
	 */
    public void setSource(String value) {
        takeStoredValueForKey(value, "source");
        setDateModified();
    }

    /**
	 * Returns the String value of DCTERMS:Spatial attribute of the metadata record
	 * for the asset.
	 * 
	 * @return String value of DCTERMS:Spatial attribute of the metadata record
	 */
    public String spatial() {
        return (String) storedValueForKey("spatial");
    }

    /**
	 * 
	 * @param value
	 */
    public void setSpatial(String value) {
        takeStoredValueForKey(value, "spatial");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Subject attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Subject attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#subject
	 */
    public String subject() {
        return (String) storedValueForKey("subject");
    }

    /**
	 * Returns an NSArray populated with the tokenized return
	 * value of subject().
	 * 
	 * @return NSArray of tokenized return value of subject()
	 */
    public NSArray subjectTokens() {
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
        NSMutableArray tokens = new NSMutableArray(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            tokens.addObject(tokenizer.nextToken().trim());
        }
        return tokens;
    }

    /**
	 * Sets the value of DC:Subject attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Subject attribute
	 */
    public void setSubject(String value) {
        takeStoredValueForKey(value, "subject");
        setDateModified();
    }

    /**
	 * Returns String value of accessibility synchronized caption attribute
	 * 
	 * @return String value of accessibility synchronized caption attribute
	 */
    public String syncCaption() {
        return (String) storedValueForKey("syncCaption");
    }

    /**
	 * 
	 * @param value
	 */
    public void SetSyncCaption(String value) {
        takeStoredValueForKey(value, "syncCaption");
        setDateModified();
    }

    /**
	 * Returns String value of DCTERMS:Temporal attribute
	 * 
	 * @return String value of DCTERMS:Temporal attribute
	 */
    public String temporal() {
        return (String) storedValueForKey("temporal");
    }

    /**
	 * 
	 * @param value
	 */
    public void setTemporal(String value) {
        takeStoredValueForKey(value, "temporal");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Title attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Title attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#title
	 */
    public String title() {
        return (String) storedValueForKey("title");
    }

    /**
	 * Sets the value of DC:Title attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Title attribute
	 */
    public void setTitle(String value) {
        takeStoredValueForKey(value, "title");
        setDateModified();
    }

    /**
	 * Returns the String value of accessibility audio transcript attribute
	 * 
	 * @return String value of accessibility audio transcript attribute
	 */
    public String transcript() {
        return (String) storedValueForKey("transcript");
    }

    /**
	 * 
	 * @param value
	 */
    public void setTranscript(String value) {
        takeStoredValueForKey(value, "transcript");
        setDateModified();
    }

    /**
	 * Returns the value of DC:Type attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Type attribute of metadata record for asset
	 */
    public String type() {
        return (String) storedValueForKey("type");
    }

    /**
	 * Sets the value of DC:Type attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Type attribute
	 */
    public void setType(String value) {
        takeStoredValueForKey(value, "type");
        setDateModified();
    }

    /**
	 * Returns a String corresponding to the value of 
	 * DCTERMS:Valid attribute of the metadata record for the asset.
	 * 
	 * @return String corresponding to DCTERMS:Valid attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/qualifiers.shtml#valid
	 */
    public String valid() {
        return (String) storedValueForKey("valid");
    }

    /**
	 * Sets the value of DCTERMS:Valid attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:Valid attribute
	 */
    public void setValid(String value) {
        takeStoredValueForKey(value, "valid");
        setDateModified();
    }

    /**
	 * wrapper for dateModified()
	 * 
	 * @return return value of dateModified();
	 */
    public NSTimestamp pAttributeChangeDate() {
        return dateModified();
    }

    /**
	 * Returns an NSArray object populated with comma-delimited value of
	 * DC:Contributor attribute of metadata record for asset, split on delimiter
	 * 
	 * @return NSArray of DC:Contributor values
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#contributor
	 */
    public NSArray pAuthors() {
        return new NSArray(contributor());
    }

    /**
	 * Returns the value of DCTERMS:DateCopyrighted attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DCTERMS:DateCopyrighted attribute of metadata record for asset
	 */
    public String copyright() {
        return dateCopyrighted();
    }

    /**
	 * Sets the value of DCTERMS:DateCopyrighted attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:DateCopyrighted attribute
	 */
    public void setCopyright(String value) {
        setDateCopyrighted(value);
    }

    /**
	 * Returns UTI (Uniform Type Identifier) for asset, either 
	 * derived from the value of DC:Type or from the content 
	 * type returned from the URL of the asset. (Note: should probably
	 * derive not from DC:Type, but from DC:Format)
	 * 
	 * @return UTI for asset
	 * @see http://developer.apple.com/macosx/uniformtypeidentifiers.html
	 */
    public String pContentType() {
        System.out.println("AssetDBRecord.pContentType(): executing\n");
        String type = type();
        System.out.println("AssetDBRecord.pContentType(): type == " + type + "\n");
        String uti = null;
        if (type == null) {
            try {
                String location = location();
                System.out.println("AssetDBRecord.pContentType(): location = " + location + "\n");
                URL url = new URL(location());
                type = url.openConnection().getContentType();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        if (type != null) {
            uti = UTType.preferredIdentifierForTag(UTType.MIMETypeTagClass, type, null);
            System.out.println("AssetDBRecord.pContentType(): uti = " + uti + "\n");
        }
        if (uti == null) {
            String file = location();
            if (file != null) {
                uti = UTType.preferredIdentifierForTag(UTType.FilenameExtensionTagClass, (NSPathUtilities.pathExtension(file)).toLowerCase(), null);
                System.out.println("AssetDBRecord.pContentType(): uti = " + uti + "\n");
            }
            if (uti == null) {
                uti = UTType.Item;
            }
        }
        return uti;
    }

    /**
	 * 
	 * @return
	 */
    public String contentType() {
        return pContentType();
    }

    /**
	 * @return
	 */
    public String ContentType() {
        return pContentType();
    }

    /**
	 * Returns an NSTimestamp of the value of DCTERMS:DateSubmitted 
	 * attribute of the metadata record for the asset.
	 * 
	 * @return NSTimestamp of DCTERMS:DateSubmitted attribute of metadata record for asset
	 */
    public NSTimestamp date_added() {
        return dateSubmitted();
    }

    /**
	 * Sets the value of DCTERMS:DateSubmitted attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:DateSubmitted attribute
	 */
    public void setDate_added(NSTimestamp value) {
        setDateSubmitted(value);
    }

    /**
	 * Returns an NSTimestamp of the value of DCTERMS:Modified 
	 * attribute of the metadata record for the asset.
	 * 
	 * @return NSTimestamp of DCTERMS:Modified attribute of metadata record for asset
	 */
    public NSTimestamp date_modified() {
        return dateModified();
    }

    /**
	 * Sets the value of DCTERMS:Modified attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:Modified attribute
	 */
    public void setDate_modified(NSTimestamp value) {
        setDateModified(value);
    }

    /**
	 * Sets the value of DCTERMS:Modified attribute of the metadata record
	 * for the asset to the current time.
	 */
    public void setDate_modified() {
        setDateModified();
    }

    /**
	 * Wrapper class to support older API, calls results of description()
	 * 
	 * @return return value from description()
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#description
	 */
    public String pDescription() {
        return description();
    }

    /**
	 * Wrapper class to support older API, calls results of title()
	 * 
	 * @return return value from title()
	 */
    public String pDisplayName() {
        return title();
    }

    /**
	 * Returns an Integer object corresponding to the value of 
	 * DCTERMS:Valid attribute of the metadata record for the asset.
	 * 
	 * @return Integer object corresponding to DCTERMS:Valid attribute of metadata record for asset
	 */
    public Integer isDeleted() {
        return Integer.decode((String) storedValueForKey("valid"));
    }

    /**
	 * Sets the value of DCTERMS:Valid attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:Valid attribute
	 */
    public void setIsDeleted(Integer value) {
        setValid(value.toString());
    }

    /**
	 * Returns an NSArray populated with the tokenized return
	 * value of keywords().
	 * 
	 * @return NSArray of tokenized return value of keywords()
	 */
    public NSArray pKeywords() {
        return subjectTokens();
    }

    /**
	 * Returns an NSArray populated with the tokenized return
	 * value of keywords().
	 * 
	 * @return NSArray of tokenized return value of keywords()
	 */
    public NSArray Keywords() {
        return pKeywords();
    }

    /**
	 * Returns the value of DC:Subject attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DC:Subject attribute of metadata record for asset
	 * @see http://dublincore.org/documents/usageguide/elements.shtml#subject
	 */
    public String keywords() {
        return subject();
    }

    /**
	 * Sets the value of DC:Subject attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DC:Subject attribute
	 */
    public void setKeywords(String value) {
        setSubject(value);
    }

    /**
	 * Returns the value of DCTERMS:RightsHolder attribute of the metadata
	 * record for the asset.
	 * 
	 * @return DCTERMS:RightsHolder attribute of metadata record for asset
	 */
    public String owner() {
        return rightsHolder();
    }

    /**
	 * Sets the value of DCTERMS:RightsHolder attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value value to set for DCTERMS:RightsHolder attribute
	 */
    public void setOwner(String value) {
        setRightsHolder(value);
    }

    /**
	 * Returns the Integer value of DCTERMS:AccessRights attribute of the metadata record
	 * for the asset.
	 * 
	 * @return Integer value of DCTERMS:AccessRights attribute of the metadata record
	 * for the asset.
	 */
    public Integer permissions() {
        return Integer.decode(accessRights());
    }

    /**
	 * Sets an Integer object as the value of DCTERMS:AccessRights attribute of the metadata record
	 * for the asset.
	 * 
	 * @param value Integer object value to set for DCTERMS:AccessRights attribute
	 */
    public void setPermissions(Integer value) {
        setAccessRights(value.toString());
    }

    /**
	 * Wrapper class to support older API, calls results of relation()
	 * 
	 * @return return value from relation()
	 */
    public String pRelation() {
        return relation();
    }

    /**
	 * Wrapper class to support older API, calls results of title()
	 * 
	 * @return return value from title()
	 */
    public String pTitle() {
        return title();
    }

    /**
	 * 
	 * @return
	 */
    public String pTombstone() {
        return mediaLabel();
    }

    /**
	 * Returns String of media label (formerly tombstone) to be included as caption for asset
	 * 
	 * @return String media label value
	 */
    public String tombstone() {
        return mediaLabel();
    }

    /**
	 * 
	 * @param value
	 */
    public void setTombstone(String value) {
        setMediaLabel(value);
    }

    /**
	 * 
	 * @return
	 */
    public String puid() {
        return (String) this.storedValueForKey("uid");
    }

    /**
	 * 
	 * @return
	 */
    public String uid() {
        return puid();
    }
}
