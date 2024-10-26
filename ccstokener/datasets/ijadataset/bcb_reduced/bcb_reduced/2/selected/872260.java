package org.nmc.pachyderm.okiosid;

import com.webobjects.foundation.*;
import java.net.*;
import java.util.*;
import ca.ucalgary.apollo.core.*;
import ca.ucalgary.apollo.data.*;
import org.nmc.pachyderm.foundation.metadata.*;
import org.osid.repository.RepositoryManager;
import org.osid.shared.*;

public class OKIOSIDManagedObject extends CXManagedObject {

    private String _id = null;

    private NSURL _OSIDStoreURL = null;

    private CXManagedObject _thumbnailImage;

    private URL _url = null;

    private String _contentType = null;

    private org.osid.repository.Asset osidAsset = null;

    private PachydermOSIDAssetMetadataPopulator populator = null;

    private PachydermOSIDAssetMetadataPopulator localPopulator = null;

    private static final NSSet _IntrinsicOKIOSIDAttributes = new NSSet(new String[] { PXMD.FSExists, PXMD.Title, "assetURLType", "thumbnailURLType", "uid", PXMD.ContentType });

    private NSMutableDictionary _intrinsicValuesByAttribute = new NSMutableDictionary();

    protected OKIOSIDManagedObject(String id) {
        this(id, null);
    }

    protected OKIOSIDManagedObject(String id, NSURL OSIDStoreURL) {
        super();
        _OSIDStoreURL = OSIDStoreURL;
        _id = id;
    }

    protected OKIOSIDManagedObject(String id, NSURL OSIDStoreURL, org.osid.repository.Asset asset) {
        super();
        _OSIDStoreURL = OSIDStoreURL;
        _id = id;
        osidAsset = asset;
        _initializePopulator();
    }

    public static CXManagedObject objectWithIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return (CXManagedObject) new OKIOSIDManagedObject(identifier);
    }

    public static CXManagedObject objectWithIdentifierAndObjectStoreURL(String identifier, NSURL OSIDStoreURL) {
        if ((identifier == null) || (OSIDStoreURL == null)) {
            return null;
        }
        return (CXManagedObject) new OKIOSIDManagedObject(identifier, OSIDStoreURL);
    }

    /**
	 * Returns the identifier for this managed object. This identifier can be stored and used to retrieve the managed object.
	 *
	 * @see CXManagedObject#objectWithIdentifier(String)
	 */
    public String identifier() {
        return _id;
    }

    public NSURL objectStoreURL() {
        return _OSIDStoreURL;
    }

    public void setObjectStoreURL(NSURL OSIDStoreURL) {
        _OSIDStoreURL = OSIDStoreURL;
    }

    public CXObjectStore objectStore() {
        if (objectStoreURL() != null) {
            return CXObjectStoreCoordinator.defaultCoordinator().objectStoreForURL(objectStoreURL());
        }
        return null;
    }

    private void _initializePopulator() {
        org.osid.repository.Asset tAsset = getOsidAsset();
        populator = new PachydermOSIDAssetMetadataPopulator();
        populator.initialize(tAsset);
    }

    public PachydermOSIDAssetMetadataPopulator populator() {
        if (populator == null) {
            _initializePopulator();
        }
        return populator;
    }

    /**
	 * Returns the type of this managed object. This value does not indicate the content type of the object, but rather its access or reference type. For example, a managed object that is identified by an URL will return <code>public.url</code> as its type. The returned value is a Uniform Type Identifier (UTI).
	 *
	 * @return the Uniform Type Identifier (UTI) for this object's reference
	 */
    public String typeIdentifier() {
        return (String) valueForAttribute("Type");
    }

    public URL url() {
        PachydermOSIDAssetContext.getInstance();
        localPopulator = populator();
        if (_url != null) {
            return _url;
        }
        String urlString = (String) _valueFromOSIDForAttribute("assetURLType");
        if (urlString == null) {
            urlString = (String) _valueFromOSIDForAttribute("assetURL");
        }
        if (urlString != null) {
            try {
                _url = new URL(urlString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("AssetURL (url()): " + _url + "\n");
        return _url;
    }

    public boolean FSExists() {
        if (url() != null) {
            return true;
        }
        return false;
    }

    public String title() {
        return (String) _valueFromOSIDForAttribute(PXMD.Title);
    }

    public String AssetURL() {
        return (String) url().toString();
    }

    public String assetURLType() {
        return (String) url().toString();
    }

    public String uid() {
        return "0";
    }

    public String contentType() {
        if (_contentType != null) {
            return (String) _contentType;
        }
        String uti = null;
        URL url = url();
        System.out.println("OKIOSIDManagedObject.contentType(): url = " + url + "\n");
        if (url != null) {
            String contentType = null;
            try {
                contentType = url.openConnection().getContentType();
            } catch (java.io.IOException e) {
                System.out.println("OKIOSIDManagedObject.contentType(): couldn't open URL connection!\n");
                return UTType.Item;
            }
            if (contentType != null) {
                System.out.println("OKIOSIDManagedObject.contentType(): contentType = " + contentType + "\n");
                uti = UTType.preferredIdentifierForTag(UTType.MIMETypeTagClass, contentType, null);
            }
            if (uti == null) {
                uti = UTType.Item;
            }
        } else {
            uti = UTType.Item;
        }
        _contentType = uti;
        System.out.println("OKIOSIDManagedObject.contentType(): uti = " + uti + "\n");
        return uti;
    }

    private Object _valueFromOSIDForAttribute(String attribute) {
        Object value = null;
        PachydermOSIDAssetContext.getInstance();
        localPopulator = populator();
        CXObjectStore ostore = objectStore();
        try {
            if (localPopulator != null) {
                System.out.println("looking for attribute from OSID " + attribute);
                if (PachydermOSIDAssetContext.getInstance().hasAttributeType(attribute)) {
                    System.out.println("looking for field from OSID with attribute " + attribute);
                    String metadataField = PachydermOSIDAssetContext.getInstance().getMetadataFieldForAttribute(attribute);
                    System.out.println("looking for value from OSID with field " + metadataField);
                    value = localPopulator.valueForKey(metadataField);
                    System.out.println("found value " + value);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return value;
    }

    public NSDictionary valuesForAttributes(NSArray attributes) {
        NSMutableDictionary values = new NSMutableDictionary();
        NSMutableArray attributesNotStored = new NSMutableArray();
        java.util.Enumeration enumerator = attributes.objectEnumerator();
        while (enumerator.hasMoreElements()) {
            String attribute = (String) enumerator.nextElement();
            Object value = storedValueForAttribute(attribute);
            if (value == null) {
                attributesNotStored.add(attribute);
            } else {
                values.takeValueForKey(value, attribute);
            }
        }
        if (attributesNotStored.count() > 0) {
            values.addEntriesFromDictionary(_valuesFromOSIDForAttributes(attributesNotStored.immutableClone()));
        }
        return values;
    }

    private NSDictionary _valuesFromOSIDForAttributes(NSArray attributes) {
        NSMutableDictionary values = new NSMutableDictionary();
        CXObjectStore ostore = objectStore();
        try {
            if (ostore != null) {
                org.osid.repository.RepositoryManager rm = ((OKIOSIDObjectStore) ostore).repositoryManager();
                if (rm != null) {
                    org.osid.repository.Asset asset = rm.getAsset(new OKIOSIDId(identifier()));
                    if (asset != null) {
                        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
                        while (recordIterator.hasNextRecord()) {
                            org.osid.repository.Record sourceRecord = recordIterator.nextRecord();
                            org.osid.repository.PartIterator partIterator = sourceRecord.getParts();
                            while (partIterator.hasNextPart()) {
                                org.osid.repository.Part sourcePart = partIterator.nextPart();
                                java.util.Enumeration enumerator = attributes.objectEnumerator();
                                while (enumerator.hasMoreElements()) {
                                    String attribute = (String) enumerator.nextElement();
                                    if (sourcePart.getPartStructure().getType().isEqual((org.osid.shared.Type) (PachydermOSIDAssetContext.getInstance().repositorySearchTypes().objectForKey(attribute)))) {
                                        Object value = sourcePart.getValue();
                                        values.takeValueForKey(value, attribute);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return values.immutableClone();
    }

    private Object _valueForKey(String s) {
        String first = s.substring(0, 1);
        String last = s.substring(1);
        first = first.toLowerCase();
        NSSelector sel1 = new NSSelector(s);
        try {
            if (sel1.implementedByObject(this)) {
                return sel1.invoke(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        s = new String(first + last);
        NSSelector sel2 = new NSSelector(s);
        try {
            if (sel2.implementedByObject(this)) {
                return sel2.invoke(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        s = new String("get" + s);
        NSSelector sel3 = new NSSelector(s);
        try {
            if (sel3.implementedByObject(this)) {
                return sel3.invoke(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object storedValueForAttribute(String attribute) {
        long startTime = System.currentTimeMillis();
        Object value = null;
        if (intrinsicOKIOSIDAttributes().containsObject(attribute)) {
            value = _intrinsicValueForKey(attribute);
            if (value == null) {
                value = _valueForKey(attribute);
            }
            if (value == null) {
                long osidTimeStart = System.currentTimeMillis();
                value = _valueFromOSIDForAttribute(attribute);
            }
        } else {
            value = _valueFromOSIDForAttribute(attribute);
        }
        if (value == null) {
            value = super.storedValueForAttribute(attribute);
        }
        return value;
    }

    protected void setStoredValueForAttribute(Object value, String attribute) {
        super.setStoredValueForAttribute(value, attribute);
    }

    protected NSSet intrinsicOKIOSIDAttributes() {
        return _IntrinsicOKIOSIDAttributes;
    }

    protected Object _intrinsicValueForKey(String attribute) {
        return _intrinsicValuesByAttribute.objectForKey(attribute);
    }

    public void takeIntrinsicValueForKey(Object value, String key) {
        _intrinsicValuesByAttribute.takeValueForKey(value, key);
    }

    /**
	 * Provides an opportunity for subclasses to perform custom ordering of attached metadata.
	 */
    protected void orderMetadata(NSMutableArray metadata) {
    }

    public void setPreviewImage(CXManagedObject preview) {
        _thumbnailImage = preview;
    }

    public void setPreviewImage(String previewURLString) {
        try {
            CXManagedObject previewObject = CXURLObject.objectWithURL(new URL(previewURLString));
            _thumbnailImage = previewObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CXManagedObject previewImage() {
        if (_thumbnailImage != null) {
            return _thumbnailImage;
        }
        setPreviewImage(previewImageForContext(DefaultPreviewImageContext));
        return _thumbnailImage;
    }

    public CXManagedObject previewImageForContext(NSDictionary context) {
        if (context != DefaultPreviewImageContext) {
            return null;
        }
        CXManagedObject previewObject = null;
        previewObject = _thumbnailImage;
        if (previewObject != null) {
            return previewObject;
        }
        String previewURLString = (String) valueForAttribute("thumbnailURLType");
        if ((previewURLString != null) && (previewURLString != "")) {
            try {
                previewObject = CXURLObject.objectWithURL(new URL(previewURLString));
                setPreviewImage(previewObject);
                System.out.println("successfully pulled thumbnail image from record.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (previewObject != null) {
            System.out.println("using thumbnail at: " + previewURLString + "\n");
            return previewObject;
        } else {
            System.out.println("couldn't find native thumbnail, going to generate.");
        }
        return CXObjectPreviewCentre.objectPreviewForObjectInContext(this, context);
    }

    private org.osid.repository.Asset getOsidAsset() {
        try {
            if (osidAsset == null) {
                CXObjectStore ostore = objectStore();
                org.osid.repository.RepositoryManager rm = ((OKIOSIDObjectStore) ostore).repositoryManager();
                if (rm != null) {
                    osidAsset = rm.getAsset(new OKIOSIDId(identifier()));
                }
            }
        } catch (Throwable e) {
        }
        return osidAsset;
    }
}
