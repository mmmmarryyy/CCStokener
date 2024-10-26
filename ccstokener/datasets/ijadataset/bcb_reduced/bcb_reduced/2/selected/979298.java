package de.offis.semanticmm4u.media_elements_connector.media_elements_creators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import component_interfaces.semanticmm4u.realization.IMetadata;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediumElementCreator;
import de.offis.semanticmm4u.compositors.variables.media.Image;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotCreateMediumElementsException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotReadMediumElementsContentException;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.global.Metadata;
import de.offis.semanticmm4u.global.Utilities;

/**
 * FastImageMediumCreator
 * 
 * This image medium creator can read very fast the header information (size, 
 * etc.) of the following formats: 
 * "gif","jpg","jpeg","png","bmp","pcx"
 * It uses the ImageInfo class (Written by Marco Schmidt) for reading the 
 * header and uses ImageIO to get the content of the images.
 * 
 */
public class FastImageMediumCreator extends AbstractMediumCreator {

    @Override
    public IMedium createMedium(String urlString, IMetadata optionalMetadata) throws MM4UCannotCreateMediumElementsException {
        try {
            URL url = new URL(urlString);
            ImageInfo imageInfo = new ImageInfo();
            InputStream imageSteam = url.openStream();
            imageInfo.setInput(imageSteam);
            if (!imageInfo.check()) {
                throw new MM4UCannotCreateMediumElementsException(this, "public IMedium createMedium( String urlString, Metadata optionalMetadata )", "Can not create connector medium from given url: " + urlString + "\n(Reason: Not a supported image file format)");
            }
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            imageSteam.close();
            String mimeType = Utilities.getMimetype(Utilities.getURISuffix(urlString));
            if (optionalMetadata == null) optionalMetadata = new Metadata();
            optionalMetadata.addIfNotNull(IMedium.MEDIUM_METADATA_MIMETYPE, mimeType);
            return new Image(this, width, height, urlString, optionalMetadata);
        } catch (IOException excp) {
            throw new MM4UCannotCreateMediumElementsException(this, "createConnectorMedium", "Can not create connector medium from given url: " + urlString + "\n(Reason: " + excp.getMessage() + ")");
        }
    }

    @Override
    public Object getContent(IMedium medium) throws MM4UCannotReadMediumElementsContentException {
        String tempURL = medium.getURI();
        try {
            return ImageIO.read(new URL(tempURL));
        } catch (IOException exception) {
            throw new MM4UCannotReadMediumElementsContentException(this, "getContent", "Can not read medium content from given connector medium url: " + tempURL + "\n(Reason: " + exception.getMessage() + ")");
        }
    }

    @Override
    public String[] getSupportedFileExtensions(int mediaType) {
        if (mediaType == Constants.MEDIATYPE_IMAGE || mediaType == Constants.MEDIATYPE_ALL) return new String[] { "gif", "jpg", "jpeg", "png", "bmp", "pcx" }; else return new String[0];
    }

    @Override
    public IMediumElementCreator recursiveClone() {
        return new FastImageMediumCreator();
    }
}
