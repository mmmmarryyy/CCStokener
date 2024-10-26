package gov.lanl.COAS;

import java.io.*;
import java.net.*;
import org.omg.DsObservationValue.UniversalResourceIdentifier;
import org.omg.DsObservationValue.UniversalResourceIdentifierHelper;
import org.omg.DsObservationValue.Multimedia;
import org.omg.DsObservationValue.MultimediaHelper;
import org.omg.DsObservationAccess.ObservationDataStruct;
import org.apache.log4j.Logger;
import gov.lanl.Utility.XML;

/**
 * This class traverses an ObservationDataStruct tree, searches for URIs in Images
 * and converts these URIs into Base64 decoded blobs.
 *
 * Looks for 'DNS:telemed.lanl.gov/TraitCode/URI' code as a subtree of
 * 'DNS:telemed.lanl.gov/TraitCode/ImageStudy/ImageSeries/Image/FullImageData' and
 * 'DNS:telemed.lanl.gov/TraitCode/ImageStudy/ImageSeries/Image/ThumbImageData'
 * reads in the specified file by the URI, converts it into a blob and
 * replaces it with Multimedia 'DNS:telemed.lanl.gov/TraitCode/Multimedia'.
 *
 * @author Sascha A. Koenig
 * @version %I%, %G%
 */
public class URI2Blob {

    private static Logger cat = Logger.getLogger(URI2Blob.class.getName());

    private static String URI = XML.URI;

    private static String FULL_IMAGE = XML.FullImageData;

    private static String THUMB_IMAGE = XML.ThumbImageData;

    /**
     * an orb for creating Anys
     */
    private org.omg.CORBA.ORB orb;

    /**
     * an array of observation data to operate on
     */
    private ObservationDataStruct[] obsDataSeq;

    /**
     * Constructor declaration
     *
     * @param obsData the observation data, whose URIs should be converted into Blobs
     * @deprecated
     */
    public URI2Blob(ObservationDataStruct obsData, org.omg.CORBA.ORB the_orb) {
        orb = the_orb;
        obsDataSeq = new ObservationDataStruct[1];
        obsDataSeq[0] = obsData;
    }

    /**
     * Constructor declaration
     *
     *
     * @param obsDataSeq
     *
     * @see
     */
    public URI2Blob(ObservationDataStruct[] obsDataSeq, org.omg.CORBA.ORB the_orb) {
        orb = the_orb;
        this.obsDataSeq = obsDataSeq;
    }

    /**
     * starts the conversion and returns an observation data object
     *
     * @return the converted observation data object
     */
    public ObservationDataStruct getObservationData() {
        findNode(obsDataSeq[0], FULL_IMAGE);
        findNode(obsDataSeq[0], THUMB_IMAGE);
        return obsDataSeq[0];
    }

    /**
     * starts the conversion and returns a sequence of observation data objects
     */
    public ObservationDataStruct[] getObservationDataSeq() {
        for (int i = 0; i < obsDataSeq.length; i++) {
            findNode(obsDataSeq[i], FULL_IMAGE);
            findNode(obsDataSeq[i], THUMB_IMAGE);
        }
        return obsDataSeq;
    }

    /**
     * internal method for finding the a specific node
     *
     * @param obsData
     * @param qualCodeStr
     */
    void findNode(ObservationDataStruct obsData, String qualCodeStr) {
        if (!obsData.code.equals(qualCodeStr)) {
            for (int i = 0; i < obsData.composite.length; i++) {
                findNode(obsData.composite[i], qualCodeStr);
            }
        } else {
            for (int i = 0; i < obsData.composite.length; i++) {
                if (obsData.composite[i].code.equals(URI)) {
                    ObservationDataStruct multimediaNode = getMultimediaObsData(obsData.composite[i]);
                    obsData.composite = new ObservationDataStruct[1];
                    obsData.composite[0] = multimediaNode;
                    break;
                }
            }
        }
    }

    /**
     * internal method for creating a new multimedia object
     *
     * @param uriNode
     */
    ObservationDataStruct getMultimediaObsData(ObservationDataStruct uriNode) {
        UniversalResourceIdentifier uri = UniversalResourceIdentifierHelper.extract(uriNode.value[0]);
        String location = uri.address;
        try {
            URL url = new URL(location);
            InputStream inputStream = new BufferedInputStream(url.openStream());
            int len = inputStream.available();
            byte[] bytes = new byte[len];
            inputStream.read(bytes, 0, len);
            inputStream.close();
            String filename = url.getFile();
            int pos = filename.lastIndexOf(".");
            String ext;
            if (pos > 0) {
                ext = filename.substring(pos + 1);
            } else {
                ext = "";
            }
            Multimedia multimedia = new Multimedia("image", ext, bytes, len, null);
            org.omg.CORBA.Any[] any = { orb.create_any() };
            MultimediaHelper.insert(any[0], multimedia);
            ObservationDataStruct multimediaNode = new ObservationDataStruct(XML.MultiMedia, new ObservationDataStruct[0], new ObservationDataStruct[0], any);
            return multimediaNode;
        } catch (java.io.IOException e) {
            cat.error("Can't open URL " + location + " ; error " + e);
            return uriNode;
        }
    }
}
