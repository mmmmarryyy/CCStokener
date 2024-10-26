package de.offis.semanticmm4u.media_elements_connector.media_elements_creators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import component_interfaces.semanticmm4u.realization.IMetadata;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.media_elements_connector.provided.IMediumElementCreator;
import de.offis.semanticmm4u.compositors.variables.media.Text;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotCreateMediumElementsException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotReadMediumElementsContentException;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.global.Metadata;
import de.offis.semanticmm4u.global.Utilities;

/**
 * Title: TextMediumCreator, part of the Learning Object Composer<br>
 * <p/>
 * Description: This class extends AbstractMediumCreator in order to support the creation of
 *              MM4U Text objects from urls with the following extensions:
 *                          txt, twm
 *
 * @version 1.0
 */
public class MM4UTextMediumCreator extends AbstractMediumCreator {

    /**
   * called to create the components object specified by the given url.
   * @param mtk the used MediaAccessorToolkit
   * @param urlString the url specifying the components
   * @param optionalMetadata any optional mediadata, my be null.
   * @return the components object
   * @throws java.io.IOException thrown, if any exception occurs, while the creator try to extract the metadata of the components
   */
    @Override
    public IMedium createMedium(String urlString, IMetadata optionalMetadata) throws MM4UCannotCreateMediumElementsException {
        int textWidth = Constants.UNDEFINED_INTEGER;
        int textHeight = Constants.UNDEFINED_INTEGER;
        try {
            URL anUrl = new URL(urlString);
            InputStream tempInputStream = anUrl.openStream();
            Properties twmProperties = new Properties();
            twmProperties.load(tempInputStream);
            textWidth = Utilities.string2Integer(twmProperties.getProperty("width"));
            textHeight = Utilities.string2Integer(twmProperties.getProperty("height"));
            tempInputStream.close();
            tempInputStream = null;
        } catch (IOException exception) {
        }
        if (optionalMetadata == null) {
            optionalMetadata = new Metadata();
        }
        String mimeType = Utilities.getMimetype(Utilities.getURISuffix(urlString));
        optionalMetadata.addIfNotNull(IMedium.MEDIUM_METADATA_MIMETYPE, mimeType);
        return new Text(this, textWidth, textHeight, urlString, optionalMetadata);
    }

    /**
   * called by the MediaAccessorToolkit to get the content of the medium
   * @param medium the medium
   * @return the content of the medium
   */
    @Override
    public Object getContent(IMedium medium) throws MM4UCannotReadMediumElementsContentException {
        String tempURL = medium.getURI();
        try {
            URL url = new URL(tempURL);
            InputStream tempInputStream = url.openStream();
            Properties twmProperties = new Properties();
            twmProperties.load(tempInputStream);
            String content = twmProperties.getProperty("text");
            if (content == null) {
                tempInputStream.close();
                tempInputStream = url.openStream();
                content = "";
                byte[] temp = new byte[1024];
                int length = -1;
                while ((length = tempInputStream.read(temp)) != -1) content += new String(temp, 0, length);
            }
            tempInputStream.close();
            tempInputStream = null;
            return content;
        } catch (IOException exception) {
            throw new MM4UCannotReadMediumElementsContentException(this, "getContent", "Can not read medium content from given connector medium url: " + tempURL);
        }
    }

    /**
   * returns the supported file extensions.
   * @return the supported file extensions.
   */
    @Override
    public String[] getSupportedFileExtensions(int type) {
        if (type == Constants.MEDIATYPE_TEXT || type == Constants.MEDIATYPE_ALL) return new String[] { "txt", "twm" }; else return new String[0];
    }

    /**
	 * Clone the object recursive.
		 * 
	 * @see de.offis.semanticmm4u.media_elements_connector.media_elements_creators.AbstractMediumCreator#recursiveClone()
	 * @return a copy of the Object.
	 */
    @Override
    public IMediumElementCreator recursiveClone() {
        MM4UTextMediumCreator object = new MM4UTextMediumCreator();
        return object;
    }
}
