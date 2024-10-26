package org.xith3d.loaders.models.impl.dae.collada;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import javax.xml.bind.JAXBException;
import org.collada._2005._11.colladaschema.COLLADA;
import org.collada._2005._11.colladaschema.NodeType;
import org.xith3d.loaders.models.impl.dae.misc.JaxbCoder;
import org.xith3d.utility.general.ReaderInputStream;

/*********************************************************************
     * Loads COLLADA objects using JAXB.
     *
     * @version
     *   $Id: ColladaLoader.java 851 2006-11-27 21:23:55 +0000 (Mo, 27 Nov 2006) Qudus $
     * @since
     *   2005-01-25
     * @author
     *   Sunder Iyer
     * @author
     *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
     *********************************************************************/
public final class ColladaLoader {

    private final JaxbCoder jaxbCoder;

    public static COLLADA loadFromString(String colladaString) throws IOException, JAXBException {
        byte[] bytes = colladaString.getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new ColladaLoader().load(inputStream);
    }

    public static COLLADA loadFromUrl(URL url) throws IOException, JAXBException {
        return new ColladaLoader().load(url);
    }

    public static NodeType loadNodeFromString(final String colladaNodeString) throws IOException, JAXBException {
        final byte[] bytes = colladaNodeString.getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new ColladaLoader().loadNode(inputStream);
    }

    public ColladaLoader() throws JAXBException {
        jaxbCoder = new JaxbCoder(ColladaConstants.JAXB_CONTEXT);
    }

    public COLLADA load(String colladaFileName) throws IOException, JAXBException {
        return load(new BufferedInputStream(new FileInputStream(colladaFileName)));
    }

    public COLLADA load(Reader reader) throws IOException, JAXBException {
        return load(new ReaderInputStream(reader));
    }

    public COLLADA load(URL url) throws IOException, JAXBException {
        return load(new BufferedInputStream(url.openStream()));
    }

    public COLLADA load(InputStream inputStream) throws IOException, JAXBException {
        try {
            return (COLLADA) jaxbCoder.parse(inputStream, -1);
        } finally {
            inputStream.close();
        }
    }

    public NodeType loadNode(final InputStream inputStream) throws IOException, JAXBException {
        try {
            return (NodeType) jaxbCoder.parse(inputStream, -1);
        } finally {
            inputStream.close();
        }
    }
}
