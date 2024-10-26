package de.offis.semanticmm4u.media_elements_connector.uri;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import component_interfaces.semanticmm4u.realization.IQueryObject;
import component_interfaces.semanticmm4u.realization.compositor.provided.IImage;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMediaList;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediaElementsAccessor;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediumElementCreator;
import component_interfaces.semanticmm4u.realization.media_elements_connector.realization.IMediumCreatorManager;
import component_interfaces.semanticmm4u.realization.user_profile_connector.provided.IUserProfile;
import de.offis.semanticmm4u.compositors.variables.media.Audio;
import de.offis.semanticmm4u.compositors.variables.media.Image;
import de.offis.semanticmm4u.compositors.variables.media.MediaList;
import de.offis.semanticmm4u.compositors.variables.media.Text;
import de.offis.semanticmm4u.compositors.variables.media.Video;
import de.offis.semanticmm4u.failures.MM4UException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotCloseMediaElementsConnectionException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotCreateMediumElementsException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotOpenMediaElementsConnectionException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UMediumElementNotFoundException;
import de.offis.semanticmm4u.failures.tools.media_cache.MM4UMediaCacheException;
import de.offis.semanticmm4u.failures.tools.media_transcoder_and_transscaler.MM4UMediaTranscoderException;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.global.Debug;
import de.offis.semanticmm4u.global.Metadata;
import de.offis.semanticmm4u.global.MyFilenameFilter;
import de.offis.semanticmm4u.global.Property;
import de.offis.semanticmm4u.global.PropertyList;
import de.offis.semanticmm4u.global.QueryObject;
import de.offis.semanticmm4u.global.StringVector;
import de.offis.semanticmm4u.global.Utilities;
import de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.FastImageMediumCreator;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.ImageIOImageMediumCreator;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.JMFAudioMediumCreator;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.JMFVideoMediumCreator;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.MM4UTextMediumCreator;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.MpegAudioSPIAudioMediumCreator;
import de.offis.semanticmm4u.tools.media_cache.MediaCache;
import de.offis.semanticmm4u.tools.media_download_servlet.MediaDownloadServlet;
import de.offis.semanticmm4u.tools.media_transcoder_and_transscaler.MediaTranscoderAndTransscaler;

/**
 * This is the MM4U internal implementation of a media server. It supports the
 * own mediaIndex-format as well as directly accessing media elements via their
 * URL.
 */
public class URIMediaElementsConnectorFactory extends MediaElementsAccessorToolkit {

    private String baseURI = null;

    private String indexFilename = null;

    private boolean useMediaIndex;

    private PropertyList mediaIndex = null;

    private static MediaCache mediaCache = null;

    private String mediaDownloadServletURL = null;

    public URIMediaElementsConnectorFactory(URIMediaElementsConnectorLocator myLocator) {
        this.baseURI = myLocator.getStringValue(URIMediaElementsConnectorLocator.BASE_URI);
        this.indexFilename = myLocator.getStringValue(URIMediaElementsConnectorLocator.INDEX_FILENAME);
        this.useMediaIndex = myLocator.getBooleanValue(URIMediaElementsConnectorLocator.USE_INDEX_FILE);
    }

    public void openConnection() throws MM4UCannotOpenMediaElementsConnectionException {
        String mediaIndexFile = this.baseURI + Constants.URI_SEPARATOR + this.indexFilename;
        if (this.useMediaIndex) {
            Debug.print("Read index file from " + mediaIndexFile);
            try {
                mediaIndex = this.readMetadataIndexFileFromNetwork(mediaIndexFile);
            } catch (IOException exception) {
                Debug.println("*** Warning: Index-file '" + this.baseURI + Constants.URI_SEPARATOR + this.indexFilename + "' not found!");
                throw new MM4UCannotOpenMediaElementsConnectionException(this, "openConnection", "The mediaserver.index-file was not found.");
            }
        }
        try {
            URIMediaElementsConnectorFactory.initMediaCache();
        } catch (MM4UMediaCacheException excp) {
            throw new MM4UCannotOpenMediaElementsConnectionException(this, "public void openConnection( MediaConnectorLocator mediaConnectorLocator )", "Error in creating the media cache: " + excp.getMessage());
        }
        this.mediaDownloadServletURL = Constants.getValue(Constants.CONFIG_MEDIA_DOWNLOAD_SERVLET);
        Debug.println("Done.");
        IMediumCreatorManager tempMediumCreatorManager = this.getMediumCreatorManager();
        tempMediumCreatorManager.addMediumCreator(new MM4UTextMediumCreator());
        tempMediumCreatorManager.addMediumCreator(new ImageIOImageMediumCreator());
        tempMediumCreatorManager.addMediumCreator(new FastImageMediumCreator());
        tempMediumCreatorManager.addMediumCreator(new MpegAudioSPIAudioMediumCreator());
        tempMediumCreatorManager.addMediumCreator(new JMFAudioMediumCreator());
        tempMediumCreatorManager.addMediumCreator(new JMFVideoMediumCreator());
    }

    private static synchronized void initMediaCache() throws MM4UMediaCacheException {
        if (URIMediaElementsConnectorFactory.mediaCache == null) URIMediaElementsConnectorFactory.mediaCache = new MediaCache();
    }

    public void closeConnection() throws MM4UCannotCloseMediaElementsConnectionException {
    }

    /**
	 * Note: Procedure is as follows: 1. The mediumID is intepretated as ID to
	 * be found in the metadata mediaserver.index-file, with other words the
	 * metadata to the given mediumID is searched in the index file. If it is
	 * found, the metadata there found are read out and added by the standard
	 * metadata that can be read out from the media object itself (width,
	 * height, length). [2. If the given mediumID is not found in the
	 * mediaserver.index-file the mediumID check if a concrete media is meant by
	 * it (e.g. picture.jpg, movie.mov) and the framework trys to read the media
	 * oject directly. If the media object is found, the media metadata are
	 * directly read from the file.] <-- Das hier ist nicht mehr
	 * Connector-unabh�ngig!
	 * 
	 * Note2: Bei der zweiten Variante kann das Medium nat�rlich auch in einem
	 * Unterverzeichnis der Base-URL des Medienservers liegen, z. B.
	 * "/Palace/Videos/high1.rm". Das ist hier nur eine Ausnahme, weil wie
	 * sollte man diese Funktionalit�t in einem ODBC Connector nachbilden?
	 * K�nnte man h�chsten �ber eine zus�tzliche Property
	 * medialocation="Palace/Videos/high1.rm" Man kann allerdings nicht absolute
	 * Adressen angeben, die mit http://..., https://... oder ftp://...
	 * beginnen. Wie sollte das auch f�r den ODBC-Connector umgesezt werden?
	 */
    public IMedium getMediumElement(String mediumID) throws MM4UMediumElementNotFoundException {
        Metadata tempMetadata = null;
        if (this.useMediaIndex) tempMetadata = this.extractMetadataFromIndexFile(mediumID); else tempMetadata = new Metadata();
        String uri = this.findMediumURI(mediumID, tempMetadata);
        try {
            return this.createMedium(uri, tempMetadata, null);
        } catch (MM4UException excp) {
            throw new MM4UMediumElementNotFoundException(this, "public IMedium getMedium( String mediumID )", excp.getMessage());
        }
    }

    /**
	 * Reads media using the context object.
	 * 
	 * @param contextObject
	 *            Identify one or many media.
	 * 
	 * @return A <code>MediaList</code> with objects fitting to their media
	 *         (e.g. <code>Image</code> for an image.
	 * @throws MM4UMediumElementNotFoundException
	 * 
	 * @see de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit#getMediaElements(IQueryObject,
	 *      IUserProfile)
	 */
    public IMediaList getMediaElements(IQueryObject contextObject, IUserProfile userProfile) throws MM4UMediumElementNotFoundException {
        MediaList mediaList = new MediaList();
        String[] filterList = (String[]) contextObject.getObjectValue(QueryObject.ATTRIBUTE_FILTER_LIST);
        if (filterList == null) {
            String id = contextObject.getStringValue(QueryObject.ATTRIBUTE_SINGLE_MEDIUM_ID);
            Metadata tempMetadata = null;
            if (this.useMediaIndex) tempMetadata = this.extractMetadataFromIndexFile(id); else tempMetadata = new Metadata();
            String uri = this.findMediumURI(id, tempMetadata);
            IMedium medium;
            try {
                medium = this.createMedium(uri, tempMetadata, contextObject);
            } catch (MM4UException excp) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", excp.getMessage());
            }
            mediaList.add(medium);
        } else {
            String path = null;
            try {
                path = (new URL(contextObject.getStringValue(QueryObject.ATTRIBUTE_URI))).getPath();
            } catch (MalformedURLException excp) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "The URI is not valid: " + contextObject.getStringValue(QueryObject.ATTRIBUTE_URI));
            }
            if (!contextObject.getStringValue(QueryObject.ATTRIBUTE_URI).startsWith("file:")) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "Multible file are supported with 'file:' only.");
            }
            File mediaPath = new File(path);
            if (!mediaPath.exists()) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "Can not read media path: '" + mediaPath);
            }
            FilenameFilter filter = new MyFilenameFilter(filterList);
            String fileList[] = mediaPath.list(filter);
            for (int i = 0; i < fileList.length; i++) {
                String uri = contextObject.getStringValue(QueryObject.ATTRIBUTE_URI) + fileList[i];
                IMedium medium;
                try {
                    medium = this.createMedium(uri, new Metadata(), contextObject);
                } catch (MM4UException excp1) {
                    throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", excp1.getMessage());
                }
                mediaList.add(medium);
            }
        }
        return mediaList;
    }

    public String getMediaElementsIdentifierSeperator() {
        return Constants.URI_SEPARATOR;
    }

    /**
	 * Finds the correct uri for the given medium. It supports direct paths aund
	 * indirect filenames.
	 * 
	 * @param myMediumID
	 *            The id of the medium.
	 * @param myMetadata
	 *            The metadata of the medium.
	 * @return The uri of the medium.
	 */
    private String findMediumURI(String myMediumID, Metadata myMetadata) {
        String uri = "";
        if (myMetadata.getValue(IMedium.MEDIUM_METADATA_URI) != null) {
            uri = myMetadata.getValue(IMedium.MEDIUM_METADATA_URI);
        } else {
            if (myMediumID.startsWith("http://") || myMediumID.startsWith("ftp://") || myMediumID.startsWith("file:")) {
                uri = myMediumID;
            } else {
                uri = this.baseURI + Constants.URI_SEPARATOR + myMediumID;
            }
        }
        return uri;
    }

    private IMedium createMedium(String uri, Metadata tempMetadata, IQueryObject contextObject) throws MM4UMediaCacheException, MM4UCannotCreateMediumElementsException {
        Debug.println("### Create medium: " + uri);
        IMedium tempMedium = null;
        IMediumElementCreator tempMediumCreator = super.determineMediumElementCreator(uri);
        Debug.println("### Selected class for automatic metadata extraction: " + tempMediumCreator);
        if (tempMediumCreator != null) {
            try {
                tempMedium = tempMediumCreator.createMedium(uri, tempMetadata);
                Debug.println("### Result of automatic metadata extraction: " + tempMedium);
            } catch (MM4UCannotCreateMediumElementsException exception) {
                Debug.println("Warning: The medium creator could not extract the meta data. Exception is " + exception);
            }
        }
        if (tempMedium == null) {
            String type = tempMetadata.getValue(IMedium.MEDIUM_METADATA_TYPE);
            int width = tempMetadata.getIntegerValue(IMedium.MEDIUM_METADATA_WIDTH);
            int height = tempMetadata.getIntegerValue(IMedium.MEDIUM_METADATA_HEIGHT);
            int length = tempMetadata.getIntegerValue(IMedium.MEDIUM_METADATA_DURATION);
            String description = tempMetadata.getValue(IMedium.MEDIUM_METADATA_DESCRIPTION);
            Metadata additionalMetadata = new Metadata();
            if (description != null) additionalMetadata.add(IMedium.MEDIUM_METADATA_DESCRIPTION, description);
            if (type.equals("text")) {
                tempMedium = new Text(tempMediumCreator, width, height, uri, additionalMetadata);
            } else if (type.equals("image")) {
                tempMedium = new Image(tempMediumCreator, width, height, uri, additionalMetadata);
            } else if (type.equals("audio")) {
                tempMedium = new Audio(tempMediumCreator, length, uri, additionalMetadata);
            } else if (type.equals("video")) {
                tempMedium = new Video(tempMediumCreator, width, height, length, uri, additionalMetadata);
            }
            Debug.println("### Result of media index file: " + tempMedium);
        }
        ((Metadata) tempMedium.getMetadata()).addAllButDoNotOverride(tempMetadata);
        String originalUri = tempMedium.getURI();
        tempMedium = this.workOnContextObject(contextObject, tempMedium);
        if ((contextObject != null) && (contextObject.getBooleanValue(QueryObject.ATTRIBUTE_USE_DOWNLOAD_SERVLET)) && (tempMedium.getURI().startsWith("file:"))) {
            String newURI = MediaDownloadServlet.recodeURL(this.mediaDownloadServletURL, tempMedium.getURI());
            tempMedium.setURI(newURI);
        }
        ((Metadata) tempMedium.getMetadata()).put(IMedium.MEDIUM_METADATA_ORIGINAL_URI, originalUri);
        return tempMedium;
    }

    private IMedium workOnContextObject(IQueryObject contextObject, IMedium tempMedium) throws MM4UCannotCreateMediumElementsException, MM4UMediaCacheException {
        if ((contextObject != null) && (tempMedium instanceof Image)) {
            IImage image = (IImage) tempMedium;
            if (checkIfRescalingIsRequired(image, contextObject)) {
                tempMedium = this.handleContextObjectHeightAndWidthInformation(image, contextObject);
            }
        }
        return tempMedium;
    }

    private boolean checkIfRescalingIsRequired(IImage image, IQueryObject contextObject) {
        int targetWidth = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_WIDTH);
        int targetHeight = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_HEIGHT);
        if (targetWidth == Constants.UNDEFINED_INTEGER || targetHeight == Constants.UNDEFINED_INTEGER) {
            return false;
        }
        if (targetWidth == image.getWidth() && targetHeight == image.getHeight()) return false;
        if ((!contextObject.getBooleanValue(QueryObject.ATTRIBUTE_ENLARGE_IMAGES)) && (targetWidth > image.getWidth() && targetHeight > image.getHeight())) {
            return false;
        }
        return true;
    }

    private IImage handleContextObjectHeightAndWidthInformation(IImage image, IQueryObject contextObject) throws MM4UCannotCreateMediumElementsException, MM4UMediaCacheException {
        int targetWidth = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_WIDTH);
        int targetHeight = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_HEIGHT);
        if (contextObject.getIntegerValue(QueryObject.ATTRIBUTE_FIT) == IQueryObject.FIT_MEET) {
            int newSize[] = MediaTranscoderAndTransscaler.getFittingSize(image.getWidth(), image.getHeight(), targetWidth, targetHeight);
            targetWidth = newSize[0];
            targetHeight = newSize[1];
        }
        Debug.println("Check media cache for hit ... ");
        IImage cachedImage = (IImage) URIMediaElementsConnectorFactory.mediaCache.getMedium(image.getURI(), targetWidth, targetHeight);
        if (cachedImage != null) {
            return cachedImage;
        }
        Debug.println("Media cache failed ... work on medium ... ");
        URL imageURL = null;
        try {
            imageURL = new URL(image.getURI());
        } catch (MalformedURLException excp1) {
            throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp1.getMessage());
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            String imageMimeType = "image/" + Utilities.getURISuffix(image.getURI());
            try {
                MediaTranscoderAndTransscaler.transformSize(imageURL.openStream(), outStream, imageMimeType, targetWidth, targetHeight);
            } catch (IOException excp2) {
                throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp2.getMessage());
            }
        } catch (MM4UMediaTranscoderException excp) {
            throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp.getMessage());
        }
        IImage newImage = URIMediaElementsConnectorFactory.mediaCache.addImage(image.getURI(), targetWidth, targetHeight, outStream);
        return newImage;
    }

    private Metadata readMetadataIndexFileFromNetwork(String mediaMetadataURI) throws IOException {
        Metadata tempMetadata = new Metadata();
        URL url = new URL(mediaMetadataURI);
        BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
        String tempLine = null;
        while ((tempLine = input.readLine()) != null) {
            Property tempProperty = PropertyList.splitStringIntoKeyAndValue(tempLine);
            if (tempProperty != null) {
                tempMetadata.addIfNotNull(tempProperty.getKey(), tempProperty.getValue());
            }
        }
        input.close();
        return tempMetadata;
    }

    private Metadata extractMetadataFromIndexFile(String mediumID) {
        Metadata tempMetadata = new Metadata();
        if (this.mediaIndex.contains(mediumID)) {
            Debug.println("Medium with id = " + mediumID + " found.");
            String metadataString = this.mediaIndex.getValue(mediumID);
            StringVector metadataVector = Utilities.tokenizeStringToStringVector(metadataString, ";");
            String type = metadataVector.elementAt(0);
            String tempURI = metadataVector.elementAt(1);
            String additionalPropertiesString = metadataVector.elementAt(2);
            StringVector additionalPropertiesVector = Utilities.tokenizeStringToStringVector(additionalPropertiesString, ",");
            for (Enumeration tokenEnumerator = additionalPropertiesVector.elements(); tokenEnumerator.hasMoreElements(); ) {
                String tokenString = (String) tokenEnumerator.nextElement();
                String tempKey = PropertyList.splitStringIntoKeyAndValue(tokenString).getKey();
                String tempValue = PropertyList.splitStringIntoKeyAndValue(tokenString).getValue();
                tempMetadata.add(tempKey, tempValue);
            }
            tempMetadata.add(IMedium.MEDIUM_METADATA_TYPE, type);
            tempMetadata.add(IMedium.MEDIUM_METADATA_URI, Utilities.addBaseURI(this.baseURI, tempURI));
        }
        return tempMetadata;
    }

    /**
	 * Clone the object recursive.
	 * 
	 * 
	 * @return a copy of the Object.
	 * @see de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit#recursiveClone()
	 */
    public IMediaElementsAccessor recursiveClone() {
        URIMediaElementsConnectorFactory object = new URIMediaElementsConnectorFactory(new URIMediaElementsConnectorLocator(this.baseURI, this.indexFilename, this.useMediaIndex));
        super.recursiveClone(object);
        object.baseURI = this.baseURI;
        object.indexFilename = this.indexFilename;
        if (this.mediaIndex != null) object.mediaIndex = (PropertyList) this.mediaIndex.recursiveClone();
        return object;
    }
}
