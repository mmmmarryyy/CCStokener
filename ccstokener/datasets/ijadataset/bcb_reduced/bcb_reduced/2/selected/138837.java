package it.paolomind.pwge.abstracts.xml;

import it.paolomind.pwge.abstracts.bo.generic.AVisualElement.AVisualElementDTO;
import it.paolomind.pwge.utils.AURLUtil;
import it.paolomind.pwge.xml.game.FileColletion;
import it.paolomind.pwge.xml.visual.ImageType;
import it.paolomind.pwge.xml.visual.Visualinfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

public abstract class AFileCollectionLoader<D extends XmlObject, O extends XmlObject, F extends Serializable> implements Enumeration<F> {

    private final FileColletion files;

    private int index = 0;

    private int max;

    public AFileCollectionLoader(final FileColletion files) {
        super();
        this.files = files;
        this.max = files.sizeOfFileArray();
    }

    protected abstract F createBO(O xmlbo) throws XmlException;

    protected final String getNextFile() {
        if (this.index < this.max) {
            return this.files.getFileArray(this.index++);
        } else {
            return null;
        }
    }

    protected InputStream openFile(final String url) throws IOException {
        return AURLUtil.toURL_notNull(url).openStream();
    }

    protected final D parseXML(final String url) throws IOException, XmlException {
        InputStream in = null;
        try {
            in = openFile(url);
            D xmldoc = xmlParsing(in);
            validate(xmldoc);
            return xmldoc;
        } catch (XmlException e) {
            throw new XmlException("error in " + url, e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected AVisualElementDTO setVisualDTO(final AVisualElementDTO dto, final String id, final Visualinfo xmlinfo) throws XmlException {
        ImageType[] array = xmlinfo.getImageurlArray();
        if (array == null || array.length == 0) {
            throw new XmlException("you must define at least one imge");
        }
        dto.setId(id);
        dto.setDescription(xmlinfo.getDescription());
        dto.setName(xmlinfo.getName());
        for (ImageType xmlimg : array) {
            String type = xmlimg.getType().toString();
            String image = xmlimg.getStringValue();
            if (AURLUtil.toURL(image) != null) {
                if (AVisualElementDTO.T_LARGEVIEW.equals(type) && dto.getLargeImage() == null) {
                    dto.setLargeImage(image);
                } else if (AVisualElementDTO.T_MAPVIEW.equals(type) && dto.getMapImage() == null) {
                    dto.setMapImage(image);
                } else if (AVisualElementDTO.T_PREVIEW.equals(type) && dto.getPreview() == null) {
                    dto.setPreview(image);
                }
            }
        }
        return dto;
    }

    protected void validate(final XmlObject xmldoc) throws XmlException {
        XmlOptions opts = new XmlOptions();
        Collection<XmlError> errorList = new ArrayList<XmlError>();
        opts.setErrorListener(errorList);
        if (!xmldoc.validate(opts)) {
            xmldoc.validate(opts);
            Iterator<XmlError> it = errorList.iterator();
            StringBuffer sBuff = new StringBuffer();
            while (it.hasNext()) {
                XmlError error = it.next();
                sBuff.append(" error: ");
                sBuff.append(error.getMessage());
                sBuff.append("\n");
            }
            throw new XmlException(sBuff.toString());
        }
    }

    protected abstract D xmlParsing(InputStream in) throws XmlException, IOException;
}
