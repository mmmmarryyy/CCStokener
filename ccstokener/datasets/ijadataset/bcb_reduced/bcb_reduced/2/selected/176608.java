package org.blackdog.type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.siberia.type.SibURL;
import org.siberia.type.annotation.bean.Bean;

/**
 *
 * Abstract implementation of an AudioItem
 *
 * @author alexis
 */
@Bean(name = "multimedia item", internationalizationRef = "org.blackdog.rc.i18n.type.MultimediaItem", expert = false, hidden = false, preferred = true, propertiesClassLimit = Object.class, methodsClassLimit = Object.class)
public abstract class MultimediaItem extends SibURL implements CategorizedItem, Playable {

    /** Creates a new instance of MultimediaItem */
    public MultimediaItem() {
    }

    /** return an InputStream
     *  @return an InputStream
     *
     *  @exception IOException if the creation failed
     */
    public InputStream createInputStream() throws IOException {
        InputStream stream = null;
        URL url = this.getValue();
        if (url != null) {
            stream = url.openStream();
        }
        return stream;
    }

    /** return the simple name of the item
     *  @return a String that does not contains '.' (example : 'mp3', 'ogg', etc..)
     */
    private String getSimpleName() {
        String result = null;
        URL url = this.getValue();
        if (url != null) {
            String file = url.getFile();
            if (file != null) {
                int lastSlashIndex = file.lastIndexOf(File.separator);
                if (lastSlashIndex != -1) {
                    result = file.substring(lastSlashIndex + 1);
                }
            }
        }
        return result;
    }

    /** return the extension of the item
     *  @return a String that does not contains '.' (example : 'mp3', 'ogg', etc..)
     */
    public String getExtension() {
        String result = null;
        String simpleName = this.getSimpleName();
        if (simpleName != null) {
            int lastPointIndex = simpleName.lastIndexOf('.');
            if (lastPointIndex != -1) {
                result = simpleName.substring(lastPointIndex + 1);
            }
        }
        return result;
    }

    /** return the name of the playable item
     *  @return the name
     */
    public String getPlayableName() {
        String result = null;
        String simpleName = this.getSimpleName();
        if (simpleName != null) {
            int lastPointIndex = simpleName.lastIndexOf('.');
            if (lastPointIndex != -1) {
                result = simpleName.substring(0, lastPointIndex);
            }
        }
        return result;
    }
}
