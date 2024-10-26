package org.argouml.profile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.argouml.model.Model;
import org.argouml.model.UmlException;
import org.argouml.model.XmiReader;
import org.xml.sax.InputSource;

/**
 * Abstract ProfileModelLoader which loads models from a URL.
 *
 * @author Tom Morris, Thomas Neustupny
 */
public class URLModelLoader implements ProfileModelLoader {

    /**
     * @param url the url/system id to load
     * @param publicId the publicId for which the model will be known - must be
     *                equal in different machines in order to be possible to
     *                load the model.
     * @return a collection of top level elements in the profile (usually a
     *         single package stereotyped <<profile>>
     * @throws ProfileException if the XMIReader couldn't read the profile
     */
    public Collection loadModel(URL url, URL publicId) throws ProfileException {
        if (url == null) {
            throw new ProfileException("Null profile URL");
        }
        ZipInputStream zis = null;
        try {
            Collection elements = null;
            XmiReader xmiReader = Model.getXmiReader();
            if (url.getPath().toLowerCase().endsWith(".zip")) {
                zis = new ZipInputStream(url.openStream());
                ZipEntry entry = zis.getNextEntry();
                if (entry != null) {
                    url = makeZipEntryUrl(url, entry.getName());
                }
                zis.close();
            }
            InputSource inputSource = new InputSource(url.toExternalForm());
            inputSource.setPublicId(publicId.toString());
            elements = xmiReader.parse(inputSource, true);
            return elements;
        } catch (UmlException e) {
            throw new ProfileException("Invalid XMI data!", e);
        } catch (IOException e) {
            throw new ProfileException("Invalid zip file with XMI data!", e);
        }
    }

    /**
     * Load a profile from a ProfileReference.
     * 
     * @param reference ProfileReference for desired profile
     * @return a collection of top level elements in the profile (usually a
     *         single package stereotyped <<profile>>
     * @throws ProfileException if the XMIReader couldn't read the profile
     */
    public Collection loadModel(final ProfileReference reference) throws ProfileException {
        return loadModel(reference.getPublicReference(), reference.getPublicReference());
    }

    private URL makeZipEntryUrl(URL url, String entryName) throws MalformedURLException {
        String entryURL = "jar:" + url + "!/" + entryName;
        return new URL(entryURL);
    }
}
