package gate;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.apache.commons.io.IOUtils;
import gate.corpora.MimeType;
import gate.corpora.RepositioningInfo;
import gate.creole.AbstractLanguageResource;
import gate.event.StatusListener;
import gate.util.BomStrippingInputStreamReader;
import gate.util.DocumentFormatException;

/** The format of Documents. Subclasses of DocumentFormat know about
  * particular MIME types and how to unpack the information in any
  * markup or formatting they contain into GATE annotations. Each MIME
  * type has its own subclass of DocumentFormat, e.g. XmlDocumentFormat,
  * RtfDocumentFormat, MpegDocumentFormat. These classes register themselves
  * with a static index residing here when they are constructed. Static
  * getDocumentFormat methods can then be used to get the appropriate
  * format class for a particular document.
  */
public abstract class DocumentFormat extends AbstractLanguageResource implements LanguageResource {

    /** Debug flag */
    private static final boolean DEBUG = false;

    /** The MIME type of this format. */
    private MimeType mimeType = null;

    /** Map of MimeTypeString to ClassHandler class. This is used to find the
    * language resource that deals with the specific Document format
    */
    protected static final Map<String, DocumentFormat> mimeString2ClassHandlerMap = new HashMap<String, DocumentFormat>();

    /** Map of MimeType to DocumentFormat Class. This is used to find the
    * DocumentFormat subclass that deals with a particular MIME type.
    */
    protected static final Map<String, MimeType> mimeString2mimeTypeMap = new HashMap<String, MimeType>();

    /** Map of Set of file suffixes to MimeType. This is used to figure
    * out what MIME type a document is from its file name.
    */
    protected static final Map<String, MimeType> suffixes2mimeTypeMap = new HashMap<String, MimeType>();

    /** Map of Set of magic numbers to MimeType. This is used to guess the
    * MIME type of a document, when we don't have any other clues.
    */
    protected static final Map<String, MimeType> magic2mimeTypeMap = new HashMap<String, MimeType>();

    /** Map of markup elements to annotation types. If it is null, the
    * unpackMarkup() method will convert all markup, using the element names
    * for annotation types. If it is non-null, only those elements specified
    * here will be converted.
    */
    protected Map markupElementsMap = null;

    /** This map is used inside uppackMarkup() method...
    * When an element from the map is encounted, The corresponding string
    * element is added to the document content
    */
    protected Map element2StringMap = null;

    /** The features of this resource */
    private FeatureMap features = null;

    /** Default construction */
    public DocumentFormat() {
    }

    /** listeners for status report */
    private transient Vector<StatusListener> statusListeners;

    /** Flag for enable/disable collecting of repositioning information */
    private Boolean shouldCollectRepositioning = new Boolean(false);

    /** If the document format could collect repositioning information
   *  during the unpack phase this method will return <B>true</B>.
   *  <BR>
   *  You should override this method in the child class of the defined
   *  document format if it could collect the repositioning information.
   */
    public Boolean supportsRepositioning() {
        return new Boolean(false);
    }

    public void setShouldCollectRepositioning(Boolean b) {
        if (supportsRepositioning().booleanValue() && b.booleanValue()) {
            shouldCollectRepositioning = b;
        } else {
            shouldCollectRepositioning = new Boolean(false);
        }
    }

    public Boolean getShouldCollectRepositioning() {
        return shouldCollectRepositioning;
    }

    /** Unpack the markup in the document. This converts markup from the
    * native format (e.g. XML, RTF) into annotations in GATE format.
    * Uses the markupElementsMap to determine which elements to convert, and
    * what annotation type names to use.
    */
    public abstract void unpackMarkup(Document doc) throws DocumentFormatException;

    public abstract void unpackMarkup(Document doc, RepositioningInfo repInfo, RepositioningInfo ampCodingInfo) throws DocumentFormatException;

    /** Unpack the markup in the document. This method calls unpackMarkup on the
    * GATE document, but after it saves its content as a feature atached to
    * the document. This method is usefull if one wants to save the content
    * of the document being unpacked. After the markups have been unpacked,
    * the content of the document will be replaced with a new one containing
    * the text between markups.
    *
    * @param doc the document that will be upacked
    * @param originalContentFeatureType the name of the feature that will hold
    * the document's content.
    */
    public void unpackMarkup(Document doc, String originalContentFeatureType) throws DocumentFormatException {
        FeatureMap fm = doc.getFeatures();
        if (fm == null) fm = Factory.newFeatureMap();
        fm.put(originalContentFeatureType, doc.getContent().toString());
        doc.setFeatures(fm);
        unpackMarkup(doc);
    }

    /**
    * Returns a MimeType having as input a fileSufix.
    * If the file sufix is <b>null</b> or not recognised then,
    * <b>null</b> will be returned.
    * @param fileSufix The file sufix associated with a recognisabe mime type.
    * @return The MimeType associated with this file suffix.
    */
    private static MimeType getMimeType(String fileSufix) {
        if (fileSufix == null) return null;
        return suffixes2mimeTypeMap.get(fileSufix.toLowerCase());
    }

    /**
    * Returns a MymeType having as input a URL object. If the MimeType wasn't
    * recognized it returns <b>null</b>.
    * @param url The URL object from which the MimeType will be extracted
    * @return A MimeType object for that URL, or <b>null</b> if the Mime Type is
    * unknown.
    */
    private static MimeType getMimeType(URL url) {
        String mimeTypeString = null;
        String charsetFromWebServer = null;
        String contentType = null;
        InputStream is = null;
        MimeType mimeTypeFromWebServer = null;
        MimeType mimeTypeFromFileSuffix = null;
        MimeType mimeTypeFromMagicNumbers = null;
        String fileSufix = null;
        if (url == null) return null;
        try {
            try {
                is = url.openConnection().getInputStream();
                contentType = url.openConnection().getContentType();
            } catch (IOException e) {
            }
            if (contentType != null) {
                StringTokenizer st = new StringTokenizer(contentType, ";");
                if (st.hasMoreTokens()) mimeTypeString = st.nextToken().toLowerCase();
                if (st.hasMoreTokens()) charsetFromWebServer = st.nextToken().toLowerCase();
                if (charsetFromWebServer != null) {
                    st = new StringTokenizer(charsetFromWebServer, "=");
                    charsetFromWebServer = null;
                    if (st.hasMoreTokens()) st.nextToken();
                    if (st.hasMoreTokens()) charsetFromWebServer = st.nextToken().toUpperCase();
                }
            }
            mimeTypeFromWebServer = mimeString2mimeTypeMap.get(mimeTypeString);
            fileSufix = getFileSufix(url);
            mimeTypeFromFileSuffix = getMimeType(fileSufix);
            mimeTypeFromMagicNumbers = guessTypeUsingMagicNumbers(is, charsetFromWebServer);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return decideBetweenThreeMimeTypes(mimeTypeFromWebServer, mimeTypeFromFileSuffix, mimeTypeFromMagicNumbers);
    }

    /**
    * This method decides what mimeType is in majority
    * @param aMimeTypeFromWebServer a MimeType
    * @param aMimeTypeFromFileSuffix a MimeType
    * @param aMimeTypeFromMagicNumbers a MimeType
    * @return the MimeType which occurs most. If all are null, then returns
    * <b>null</b>
    */
    protected static MimeType decideBetweenThreeMimeTypes(MimeType aMimeTypeFromWebServer, MimeType aMimeTypeFromFileSuffix, MimeType aMimeTypeFromMagicNumbers) {
        if (areEqual(aMimeTypeFromWebServer, aMimeTypeFromFileSuffix)) return aMimeTypeFromFileSuffix;
        if (areEqual(aMimeTypeFromFileSuffix, aMimeTypeFromMagicNumbers)) return aMimeTypeFromFileSuffix;
        if (areEqual(aMimeTypeFromWebServer, aMimeTypeFromMagicNumbers)) return aMimeTypeFromWebServer;
        if (aMimeTypeFromFileSuffix != null) aMimeTypeFromFileSuffix.addParameter("Priority", "1");
        if (aMimeTypeFromWebServer != null) aMimeTypeFromWebServer.addParameter("Priority", "2");
        if (aMimeTypeFromMagicNumbers != null) aMimeTypeFromMagicNumbers.addParameter("Priority", "3");
        return decideBetweenTwoMimeTypes(decideBetweenTwoMimeTypes(aMimeTypeFromWebServer, aMimeTypeFromFileSuffix), aMimeTypeFromMagicNumbers);
    }

    /** Decide between two mimeTypes. The decistion is made on "Priority"
    * parameter set into decideBetweenThreeMimeTypes method. If both mimeTypes
    * doesn't have "Priority" paramether set, it will return one on them.
    * @param aMimeType a MimeType object with "Prority" parameter set
    * @param anotherMimeType a MimeType object with "Prority" parameter set
    * @return One of the two mime types.
    */
    protected static MimeType decideBetweenTwoMimeTypes(MimeType aMimeType, MimeType anotherMimeType) {
        if (aMimeType == null) return anotherMimeType;
        if (anotherMimeType == null) return aMimeType;
        int priority1 = 0;
        int priority2 = 0;
        if (aMimeType.hasParameter("Priority")) try {
            priority1 = new Integer(aMimeType.getParameterValue("Priority")).intValue();
        } catch (NumberFormatException e) {
            return anotherMimeType;
        }
        if (anotherMimeType.hasParameter("Priority")) try {
            priority2 = new Integer(anotherMimeType.getParameterValue("Priority")).intValue();
        } catch (NumberFormatException e) {
            return aMimeType;
        }
        if (priority1 <= priority2) return aMimeType; else return anotherMimeType;
    }

    /**
    * Tests if two MimeType objects are equal.
    * @return true only if boths MimeType objects are different than <b>null</b>
    * and their Types and Subtypes are equals. The method is case sensitive.
    */
    protected static boolean areEqual(MimeType aMimeType, MimeType anotherMimeType) {
        if (aMimeType == null || anotherMimeType == null) return false;
        if (aMimeType.getType().equals(anotherMimeType.getType()) && aMimeType.getSubtype().equals(anotherMimeType.getSubtype())) return true; else return false;
    }

    /**
    * This method tries to guess the mime Type using some magic numbers.
    * @param aInputStream a InputStream which has to be transformed into a
    *        InputStreamReader
    * @param anEncoding the encoding. If is null or unknown then a
    * InputStreamReader with default encodings will be created.
    * @return the mime type associated with magic numbers
    */
    protected static MimeType guessTypeUsingMagicNumbers(InputStream aInputStream, String anEncoding) {
        if (aInputStream == null) return null;
        Reader reader = null;
        if (anEncoding != null) try {
            reader = new BomStrippingInputStreamReader(aInputStream, anEncoding);
        } catch (UnsupportedEncodingException e) {
            reader = null;
        }
        if (reader == null) reader = new BomStrippingInputStreamReader(aInputStream);
        return runMagicNumbers(reader);
    }

    /** Performs magic over Gate Document */
    protected static MimeType runMagicNumbers(Reader aReader) {
        if (aReader == null) return null;
        String strBuffer = null;
        int bufferSize = 2048;
        int charReads = 0;
        char[] cbuf = new char[bufferSize];
        try {
            charReads = aReader.read(cbuf, 0, bufferSize);
        } catch (IOException e) {
            return null;
        }
        if (charReads == -1) return null;
        strBuffer = new String(cbuf, 0, charReads);
        return getTypeFromContent(strBuffer);
    }

    private static MimeType getTypeFromContent(String aContent) {
        MimeType detectedMimeType = null;
        Set<String> magicSet = magic2mimeTypeMap.keySet();
        Iterator<String> iterator = magicSet.iterator();
        String magic;
        aContent = aContent.toLowerCase();
        while (iterator.hasNext()) {
            magic = iterator.next().toLowerCase();
            if (aContent.indexOf(magic) != -1) detectedMimeType = magic2mimeTypeMap.get(magic);
        }
        return detectedMimeType;
    }

    /**
    * Return the fileSuffix or null if the url doesn't have a file suffix
    * If the url is null then the file suffix will be null also
    */
    private static String getFileSufix(URL url) {
        String fileName = null;
        String fileSuffix = null;
        if (url != null) {
            fileName = url.getFile();
            StringTokenizer st = new StringTokenizer(fileName, ".");
            while (st.hasMoreTokens()) fileSuffix = st.nextToken();
        }
        return fileSuffix;
    }

    /**
    * Find a DocumentFormat implementation that deals with a particular
    * MIME type, given that type.
    * @param  aGateDocument this document will receive as a feature
    *                      the associated Mime Type. The name of the feature is
    *                      MimeType and its value is in the format type/subtype
    * @param  mimeType the mime type that is given as input
    */
    public static DocumentFormat getDocumentFormat(gate.Document aGateDocument, MimeType mimeType) {
        FeatureMap aFeatureMap = null;
        if (mimeType == null) {
            String content = aGateDocument.getContent().toString();
            if (content.length() > 2048) content = content.substring(0, 2048);
            mimeType = getTypeFromContent(content);
        }
        if (mimeType != null) {
            if (aGateDocument.getFeatures() == null) {
                aFeatureMap = Factory.newFeatureMap();
                aGateDocument.setFeatures(aFeatureMap);
            }
            aGateDocument.getFeatures().put("MimeType", mimeType.getType() + "/" + mimeType.getSubtype());
            return mimeString2ClassHandlerMap.get(mimeType.getType() + "/" + mimeType.getSubtype());
        }
        return null;
    }

    /**
    * Find a DocumentFormat implementation that deals with a particular
    * MIME type, given the file suffix (e.g. ".txt") that the document came
    * from.
    * @param  aGateDocument this document will receive as a feature
    *                     the associated Mime Type. The name of the feature is
    *                     MimeType and its value is in the format type/subtype
    * @param  fileSuffix the file suffix that is given as input
    */
    public static DocumentFormat getDocumentFormat(gate.Document aGateDocument, String fileSuffix) {
        return getDocumentFormat(aGateDocument, getMimeType(fileSuffix));
    }

    /**
    * Find a DocumentFormat implementation that deals with a particular
    * MIME type, given the URL of the Document. If it is an HTTP URL, we
    * can ask the web server. If it has a recognised file extension, we
    * can use that. Otherwise we need to use a map of magic numbers
    * to MIME types to guess the type, and then look up the format using the
    * type.
    * @param  aGateDocument this document will receive as a feature
    *                      the associated Mime Type. The name of the feature is
    *                      MimeType and its value is in the format type/subtype
    * @param  url  the URL that is given as input
    */
    public static DocumentFormat getDocumentFormat(gate.Document aGateDocument, URL url) {
        return getDocumentFormat(aGateDocument, getMimeType(url));
    }

    /** Get the feature set */
    public FeatureMap getFeatures() {
        return features;
    }

    /** Get the markup elements map */
    public Map getMarkupElementsMap() {
        return markupElementsMap;
    }

    /** Get the element 2 string map */
    public Map getElement2StringMap() {
        return element2StringMap;
    }

    /** Set the markup elements map */
    public void setMarkupElementsMap(Map markupElementsMap) {
        this.markupElementsMap = markupElementsMap;
    }

    /** Set the element 2 string map */
    public void setElement2StringMap(Map anElement2StringMap) {
        element2StringMap = anElement2StringMap;
    }

    /** Set the features map*/
    public void setFeatures(FeatureMap features) {
        this.features = features;
    }

    /** Set the mime type*/
    public void setMimeType(MimeType aMimeType) {
        mimeType = aMimeType;
    }

    /** Gets the mime Type*/
    public MimeType getMimeType() {
        return mimeType;
    }

    /**
   * Utility method to get a {@link MimeType} given the type string.
   */
    public static MimeType getMimeTypeForString(String typeString) {
        return mimeString2mimeTypeMap.get(typeString);
    }

    /**
   * Utility method to get the set of all file suffixes that are registered
   * with this class.
   */
    public static Set<String> getSupportedFileSuffixes() {
        return Collections.unmodifiableSet(suffixes2mimeTypeMap.keySet());
    }

    public synchronized void removeStatusListener(StatusListener l) {
        if (statusListeners != null && statusListeners.contains(l)) {
            @SuppressWarnings("unchecked") Vector<StatusListener> v = (Vector<StatusListener>) statusListeners.clone();
            v.removeElement(l);
            statusListeners = v;
        }
    }

    public synchronized void addStatusListener(StatusListener l) {
        @SuppressWarnings("unchecked") Vector<StatusListener> v = statusListeners == null ? new Vector<StatusListener>(2) : (Vector<StatusListener>) statusListeners.clone();
        if (!v.contains(l)) {
            v.addElement(l);
            statusListeners = v;
        }
    }

    protected void fireStatusChanged(String e) {
        if (statusListeners != null) {
            int count = statusListeners.size();
            for (int i = 0; i < count; i++) {
                statusListeners.elementAt(i).statusChanged(e);
            }
        }
    }
}
