package de.offis.semanticmm4u.media_elements_connector.media_elements_creators;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.tritonus.share.sampled.file.TAudioFileFormat;
import component_interfaces.semanticmm4u.realization.IMetadata;
import component_interfaces.semanticmm4u.realization.compositor.provided.IAudio;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediumElementCreator;
import de.offis.semanticmm4u.compositors.variables.media.Audio;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotCreateMediumElementsException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotReadMediumElementsContentException;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.global.Debug;
import de.offis.semanticmm4u.global.Utilities;
import de.offis.semanticmm4u.tools.media_cache.MediaCache;

/**
 * This class extends AbstractMediumCreator in order to support the creation of
 * MM4U Audio objects from urls with the extensions specified below.
 * 
 * This implementation uses JavaZOOM's MpegAudioSPI
 * (http://www.javazoom.net/mp3spi/sources.html) to extract the length of the
 * audio files.
 * 
 * 
 */
public class MpegAudioSPIAudioMediumCreator extends AbstractMediumCreator {

    private static final long serialVersionUID = 6831114949397008681L;

    /**
	 * Called to create the components object specified by the given url.
	 * 
	 * @param mtk
	 *            the used MediaAccessorToolkit
	 * @param urlString
	 *            the url specifying the components
	 * @param optionalMetadata
	 *            any optional mediadata, my be null.
	 * @return the components object
	 * @throws IOException
	 *             thrown, if any exception occurs, while the creator try to
	 *             extract the metadata of the components
	 */
    @Override
    public IMedium createMedium(String urlString, IMetadata optionalMetadata) throws MM4UCannotCreateMediumElementsException {
        Debug.println("createMedium(): URL: " + urlString);
        IAudio tempAudio = null;
        try {
            String cachedFileUri = null;
            try {
                URL url = new URL(urlString);
                InputStream is = url.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) out.write(buf, 0, len);
                MediaCache cache = new MediaCache();
                cachedFileUri = cache.addAudio(urlString, out).getURI().substring(5);
            } catch (MalformedURLException e) {
                cachedFileUri = urlString;
            }
            TAudioFileFormat fFormat = null;
            try {
                fFormat = (TAudioFileFormat) new MpegAudioFileReader().getAudioFileFormat(new File(cachedFileUri));
            } catch (Exception e) {
                System.err.println("getAudioFileFormat() failed: " + e);
            }
            int length = Constants.UNDEFINED_INTEGER;
            if (fFormat != null) {
                length = Math.round(Integer.valueOf(fFormat.properties().get("duration").toString()).intValue() / 1000);
            }
            String mimeType = Utilities.getMimetype(Utilities.getURISuffix(urlString));
            optionalMetadata.addIfNotNull(IMedium.MEDIUM_METADATA_MIMETYPE, mimeType);
            if (length != Constants.UNDEFINED_INTEGER) {
                tempAudio = new Audio(this, length, urlString, optionalMetadata);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
        return tempAudio;
    }

    /**
	 * returns null
	 * 
	 * @param medium
	 * @return
	 * @throws java.io.IOException
	 */
    @Override
    public Object getContent(IMedium medium) throws MM4UCannotReadMediumElementsContentException {
        return null;
    }

    /**
	 * Returns the supported file extensions.
	 * 
	 * @return the supported file extensions.
	 */
    @Override
    public String[] getSupportedFileExtensions(int type) {
        if (type == Constants.MEDIATYPE_AUDIO) {
            return new String[] { "mp3" };
        } else if (type == Constants.MEDIATYPE_ALL) {
            return new String[] { "mp3" };
        } else {
            return new String[0];
        }
    }

    /**
	 * Clones the object recursive.
	 * 
	 * @see de.offis.semanticmm4u.media_elements_connector.media_elements_creators.AbstractMediumCreator#recursiveClone()
	 * @return a copy of the Object.
	 */
    @Override
    public IMediumElementCreator recursiveClone() {
        MpegAudioSPIAudioMediumCreator object = new MpegAudioSPIAudioMediumCreator();
        return object;
    }
}
