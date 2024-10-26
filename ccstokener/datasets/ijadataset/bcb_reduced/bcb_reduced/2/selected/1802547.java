package de.offis.semanticmm4u.media_elements_connector.uri_and_mda;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import component_interfaces.semanticmm4u.realization.IMetadata;
import component_interfaces.semanticmm4u.realization.IQueryObject;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMediaList;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediaElementsAccessor;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediumElementCreator;
import component_interfaces.semanticmm4u.realization.media_elements_connector.realization.IMediumCreatorManager;
import component_interfaces.semanticmm4u.realization.user_profile_connector.provided.IUserProfile;
import de.offis.mda_evaluation.AbstractMetadataEvaluation;
import de.offis.mda_evaluation.EvaluateDate;
import de.offis.mda_evaluation.EvaluateEvents;
import de.offis.mda_evaluation.EvaluateExposure;
import de.offis.mda_evaluation.EvaluateInOutdoor;
import de.offis.mda_evaluation.EvaluateLocation;
import de.offis.mda_evaluation.EvaluateMotiv;
import de.offis.mda_evaluation.EvaluatePersons;
import de.offis.mda_evaluation.EvaluateSharpness;
import de.offis.mda_evaluation.EvaluateSimilarity;
import de.offis.mda_evaluation.EvaluateTimeClusterInformation;
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
import de.offis.sma.common.MDAContextObject;
import de.offis.sma.common.UtilSMA;
import de.offis.sma.enrichment.extraction.ApplicationToGenerateMDAInformation;
import de.offis.sma.storage.filesystem.MDAFileStorage;

/**
 * This is the MM4U internal implementation of a media server. It supports the own mediaIndex-format as well as directly
 * accessing media elements via their URL.
 **/
public class MDA_URIMediaElementsConnectorFactory extends MediaElementsAccessorToolkit {

    public static final String KEY_MISSED_THE_MDA_TESTS_FLAG = "missed_the_mda_tests";

    private String baseURI = null;

    private String indexFilename = null;

    private PropertyList mediaIndex = null;

    private static MediaCache mediaCache = null;

    private String mediaDownloadServletURL = null;

    private boolean useMediaIndex;

    private boolean CHECK_IF_MDA_METADATA_EXIST = false;

    public MDA_URIMediaElementsConnectorFactory(MDA_URIMediaElementsConnectorLocator myLocator) {
        this.baseURI = myLocator.getStringValue(MDA_URIMediaElementsConnectorLocator.BASE_URI);
        this.indexFilename = myLocator.getStringValue(MDA_URIMediaElementsConnectorLocator.INDEX_FILENAME);
        this.useMediaIndex = myLocator.getBooleanValue(MDA_URIMediaElementsConnectorLocator.USE_INDEX_FILE);
    }

    public void openConnection() throws MM4UCannotOpenMediaElementsConnectionException {
        String mediaIndexFile = this.baseURI + Constants.URI_SEPARATOR + this.indexFilename;
        if (this.useMediaIndex) {
            Debug.print("Read index file from " + mediaIndexFile);
            try {
                this.mediaIndex = this.readMetadataIndexFileFromNetwork(mediaIndexFile);
            } catch (IOException exception) {
                Debug.println("*** Warning: Index-file '" + this.baseURI + Constants.URI_SEPARATOR + this.indexFilename + "' not found!");
                throw new MM4UCannotOpenMediaElementsConnectionException(this, "openConnection", "The mediaserver.index-file was not found.");
            }
        }
        try {
            MDA_URIMediaElementsConnectorFactory.initMediaCache();
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
        if (MDA_URIMediaElementsConnectorFactory.mediaCache == null) MDA_URIMediaElementsConnectorFactory.mediaCache = new MediaCache();
    }

    public void closeConnection() throws MM4UCannotCloseMediaElementsConnectionException {
    }

    /**
     * Note:
     * Procedure is as follows:
     * 1. The mediumID is intepretated as ID to be found in the metadata mediaserver.index-file, with other
     * words the metadata to the given mediumID is searched in the index file.
     * If it is found, the metadata there found are read out and added by the standard metadata that
     * can be read out from the media object itself (width, height, length).
     * [2. If the given mediumID is not found in the mediaserver.index-file the mediumID check if
     * a concrete media is meant by it (e.g. picture.jpg, movie.mov) and the framework trys to
     * read the media oject directly. If the media object is found, the media metadata are directly
     * read from the file.] <-- Das hier ist nicht mehr Connector-unabh�ngig!
     * 
     * Note2: Bei der zweiten Variante kann das Medium nat�rlich auch in einem Unterverzeichnis
     * der Base-URL des Medienservers liegen, z. B. "/Palace/Videos/high1.rm".
     * Das ist hier nur eine Ausnahme, weil wie sollte man diese Funktionalit�t in einem ODBC Connector
     * nachbilden? K�nnte man h�chsten �ber eine zus�tzliche Property medialocation="Palace/Videos/high1.rm"
     * Man kann allerdings nicht absolute Adressen angeben, die mit http://..., https://... oder
     * ftp://... beginnen. Wie sollte das auch f�r den ODBC-Connector umgesezt werden?
     **/
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
		 * @param contextObject Identify one or many media.
		 * @return A <code>MediaList</code> with objects fitting to their media
		 *         (e.g. <code>Image</code> for an image.
		 * @throws MM4UMediumElementNotFoundException
		 * 
		 * @see de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit#getMedia(IQueryObject)
		 */
    public IMediaList getMediaElements(IQueryObject contextObject, IUserProfile profile) throws MM4UMediumElementNotFoundException {
        IMediaList mediaList = new MediaList();
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
            String coUri = contextObject.getStringValue(QueryObject.ATTRIBUTE_URI);
            String path = null;
            try {
                path = (new URL(coUri)).getPath();
            } catch (MalformedURLException excp) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "The URI is not valid: " + coUri);
            }
            if (!coUri.startsWith("file:")) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "Multible file are supported with 'file:' only.");
            }
            File mediaPath = new File(path);
            if (!mediaPath.exists()) {
                throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", "Can not read media path: '" + mediaPath);
            }
            FilenameFilter filter = new MyFilenameFilter(filterList);
            String fileList[] = mediaPath.list(filter);
            for (int i = 0; i < fileList.length; i++) {
                String uri = coUri + fileList[i];
                IMedium medium;
                try {
                    medium = this.createMedium(uri, new Metadata(), contextObject);
                } catch (MM4UException excp1) {
                    throw new MM4UMediumElementNotFoundException(this, "public IMediaList getMedia( ContextObject contextObject )", excp1.getMessage());
                }
                if (medium != null) {
                    mediaList.add(medium);
                }
            }
        }
        if (!contextObject.getBooleanValue(MDAContextObject.ATTRIBUTE_MDA_DISABLE_USAGE_OF_MDA)) mediaList = this.applyMDAResults(mediaList, contextObject);
        return mediaList;
    }

    public String getMediaElementsIdentifierSeperator() {
        return Constants.URI_SEPARATOR;
    }

    /**
     * Finds the correct uri for the given medium. 
     * It supports direct paths aund indirect filenames.
     *   
     * @param myMediumID	The id of the medium.
     * @param myMetadata	The metadata of the medium.
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

    public IMedium createMedium(String uri, Metadata tempMetadata, IQueryObject contextObject) throws MM4UMediaCacheException, MM4UCannotCreateMediumElementsException {
        Debug.println("### Create medium: " + uri);
        IMedium tempMedium = null;
        IMediumElementCreator tempMediumCreator = this.determineMediumElementCreator(uri);
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
            additionalMetadata.addIfNotNull(IMedium.MEDIUM_METADATA_DESCRIPTION, description);
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
        (tempMedium.getMetadata()).addAllButDoNotOverride(tempMetadata);
        tempMedium = this.workOnContextObject(contextObject, tempMedium);
        if ((contextObject != null) && (contextObject.getBooleanValue(QueryObject.ATTRIBUTE_USE_DOWNLOAD_SERVLET)) && (tempMedium.getURI().startsWith("file:"))) {
            String newURI = MediaDownloadServlet.recodeURL(this.mediaDownloadServletURL, tempMedium.getURI());
            tempMedium.setURI(newURI);
        }
        tempMedium.getMetadata().put(IMedium.MEDIUM_METADATA_ORIGINAL_URI, uri);
        return tempMedium;
    }

    private IMedium workOnContextObject(IQueryObject contextObject, IMedium tempMedium) throws MM4UCannotCreateMediumElementsException, MM4UMediaCacheException {
        if ((contextObject != null) && (tempMedium instanceof Image)) {
            Image image = (Image) tempMedium;
            int coWidth = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_WIDTH);
            int coHeight = contextObject.getIntegerValue(QueryObject.ATTRIBUTE_HEIGHT);
            if (coWidth == Constants.UNDEFINED_INTEGER || coHeight == Constants.UNDEFINED_INTEGER) return tempMedium;
            if (coWidth == image.getWidth() && coHeight == image.getHeight()) return tempMedium;
            if (contextObject.getIntegerValue(QueryObject.ATTRIBUTE_FIT) == IQueryObject.FIT_MEET) {
                int newSize[] = MediaTranscoderAndTransscaler.getFittingSize(image.getWidth(), image.getHeight(), coWidth, coHeight);
                coWidth = newSize[0];
                coHeight = newSize[1];
            }
            Debug.println("Check media cache for hit ... ");
            IMedium cachedImage = MDA_URIMediaElementsConnectorFactory.mediaCache.getMedium(image.getURI(), coWidth, coHeight);
            if (cachedImage != null) {
                return cachedImage;
            }
            Debug.println("Media cache failed ... transcode medium ... ");
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                URL imageURL = null;
                try {
                    imageURL = new URL(tempMedium.getURI());
                } catch (MalformedURLException excp1) {
                    throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp1.getMessage());
                }
                String imageFormat = "image/" + Utilities.getURISuffix(tempMedium.getURI());
                try {
                    MediaTranscoderAndTransscaler.transformSize(imageURL.openStream(), outStream, imageFormat, coWidth, coHeight);
                } catch (IOException excp2) {
                    throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp2.getMessage());
                }
                return MDA_URIMediaElementsConnectorFactory.mediaCache.addImage(image.getURI(), coWidth, coHeight, outStream);
            } catch (de.offis.semanticmm4u.failures.tools.media_transcoder_and_transscaler.MM4UMediaTranscoderException excp) {
                throw new MM4UCannotCreateMediumElementsException(this, "private IMedium createMedium( String uri, Metadata tempMetadata, ContextObject contextObject  )", excp.getMessage());
            }
        }
        return tempMedium;
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
     * @return a copy of the Object.
     * @see de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit#recursiveClone()
     */
    public IMediaElementsAccessor recursiveClone() {
        MDA_URIMediaElementsConnectorFactory object = new MDA_URIMediaElementsConnectorFactory(new MDA_URIMediaElementsConnectorLocator(this.baseURI, this.indexFilename, this.useMediaIndex));
        super.recursiveClone(object);
        object.baseURI = this.baseURI;
        object.indexFilename = this.indexFilename;
        if (this.mediaIndex != null) object.mediaIndex = (PropertyList) this.mediaIndex.recursiveClone();
        return object;
    }

    /**
     * 
     * Diese Methode wendet die Ausgew�hlten Evaluationsmethoden auf die in der �bergebenen MediaList
     * enthaltenen Medien an. Die Grenzwerte werde mit dem ContextObject �bergeben
     * @param fileList
     * @param contextObject
     * @return IMediaList
     */
    public IMediaList applyMDAResults(IMediaList mediaList, IQueryObject contextObject) {
        MDAContextObject mdaContextObject = Utilities.toMDAContextObject(contextObject);
        if (CHECK_IF_MDA_METADATA_EXIST) {
            String tempFName = mediaList.elementAt(0).getMetadata().getValue(IMedium.MEDIUM_METADATA_ORIGINAL_URI);
            String tempPath = Utilities.convertURIToPathname(tempFName);
            tempPath = tempPath.substring(0, tempPath.lastIndexOf("\\"));
            File testFile = new File(tempPath.concat("\\clusters.sav"));
            if (!testFile.exists()) {
                ApplicationToGenerateMDAInformation.generateMetadataForDirectory(tempPath + "\\");
            }
        }
        this.copyMetadata(mediaList);
        if (mdaContextObject.getFloatValue(MDAContextObject.ATTRIBUTE_MDA_SHARPNESS) != Constants.UNDEFINED_FLOAT) {
            AbstractMetadataEvaluation sharpnessEvaluation = new EvaluateSharpness();
            System.out.println("-----------METADATEN---------------");
            System.out.println(mediaList.toString());
            System.out.println((mediaList.elementAt(0)).getMetadata());
            mediaList = sharpnessEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getFloatValue(MDAContextObject.ATTRIBUTE_MDA_EXPOSURE) != Constants.UNDEFINED_FLOAT) {
            AbstractMetadataEvaluation exposureEvaluation = new EvaluateExposure();
            mediaList = exposureEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getFloatValue(MDAContextObject.ATTRIBUTE_MDA_SIMILARITY) != Constants.UNDEFINED_FLOAT) {
            AbstractMetadataEvaluation similarityEvaluation = new EvaluateSimilarity();
            mediaList = similarityEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getIntegerValue(MDAContextObject.ATTRIBUTE_MDA_PICTURECOUNT) != Constants.UNDEFINED_INTEGER) {
            AbstractMetadataEvaluation timeClusterEvaluation = new EvaluateTimeClusterInformation();
            mediaList = timeClusterEvaluation.evaluate(mediaList, mdaContextObject);
        }
        long dateFrom = mdaContextObject.getLongValue(MDAContextObject.ATTRIBUTE_DATE_FROM);
        long dateTo = mdaContextObject.getLongValue(MDAContextObject.ATTRIBUTE_DATE_TO);
        if (dateFrom != Constants.UNDEFINED_LONG && dateTo != Constants.UNDEFINED_LONG) {
            AbstractMetadataEvaluation dateEvaluation = new EvaluateDate();
            mediaList = dateEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getStringValue(MDAContextObject.ATTRIBUTE_INOUTDOOR) != null) {
            AbstractMetadataEvaluation inOutdoorEvaluation = new EvaluateInOutdoor();
            mediaList = inOutdoorEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getStringValue(MDAContextObject.ATTRIBUTE_MOTIV) != null) {
            AbstractMetadataEvaluation motivEvaluation = new EvaluateMotiv();
            mediaList = motivEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getStringValue(MDAContextObject.ATTRIBUTE_LOCATION) != null) {
            AbstractMetadataEvaluation locationEvaluation = new EvaluateLocation();
            mediaList = locationEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getStringValue(MDAContextObject.ATTRIBUTE_PERSONS) != null) {
            AbstractMetadataEvaluation personsEvaluation = new EvaluatePersons();
            mediaList = personsEvaluation.evaluate(mediaList, mdaContextObject);
        }
        if (mdaContextObject.getStringValue(MDAContextObject.ATTRIBUTE_EVENTS) != null) {
            AbstractMetadataEvaluation eventsEvaluation = new EvaluateEvents();
            mediaList = eventsEvaluation.evaluate(mediaList, mdaContextObject);
        }
        for (int mediaCounter = 0; mediaCounter < mediaList.size(); mediaCounter++) {
            if ((mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateSharpness.KEY_FOR_MISSED_SHARPNESS_TEST) == true) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateExposure.KEY_FOR_MISSED_EXPOSURE_TEST) == true) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateSimilarity.KEY_FOR_MISSED_SIMILARITY_TEST) == true) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateTimeClusterInformation.KEY_FOR_MISSED_TIMECLUSTER_TEST) == true) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateDate.MISSED_DATE_TEST_KEY)) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateMotiv.MISSED_MOTIV_TEST_KEY)) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateLocation.MISSED_LOCATION_TEST_KEY)) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluatePersons.MISSED_PERSONS_TEST_KEY)) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateEvents.MISSED_EVENTS_TEST_KEY)) | (mediaList.elementAt(mediaCounter).getMetadata().getBooleanValue(EvaluateInOutdoor.MISSED_INOUTDOOR_TEST_KEY))) {
                mediaList.elementAt(mediaCounter).getMetadata().add(KEY_MISSED_THE_MDA_TESTS_FLAG, true);
            } else {
                mediaList.elementAt(mediaCounter).getMetadata().add(KEY_MISSED_THE_MDA_TESTS_FLAG, false);
            }
        }
        return mediaList;
    }

    /**
 * Diese Methode kopiert einfach alle Metadaten ins medium 
 * 
 * @param mediaList
 */
    private void copyMetadata(IMediaList mediaList) {
        MDAFileStorage storage = new MDAFileStorage();
        String tempFileNames[] = new String[mediaList.size()];
        for (int mediaCounter = 0; mediaCounter < mediaList.size(); mediaCounter++) {
            String tempFName = mediaList.elementAt(mediaCounter).getMetadata().getValue(IMedium.MEDIUM_METADATA_ORIGINAL_URI);
            tempFileNames[mediaCounter] = UtilSMA.convertURIToPathname(tempFName);
        }
        for (int mediaCounter = 0; mediaCounter < mediaList.size(); mediaCounter++) {
            de.offis.sma.common.Metadata mDat = (de.offis.sma.common.Metadata) storage.readMetaData(tempFileNames[mediaCounter]);
            if (mDat == null) {
                Debug.println("Warning: 'storage.readMetaData' returns no metadata!");
                continue;
            }
            ArrayList keys = mDat.getKeys();
            IMetadata metadata = mediaList.elementAt(mediaCounter).getMetadata();
            for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
                String key = (String) iter.next();
                metadata.putObject(key, mDat.get(key).getValues());
            }
        }
    }
}
