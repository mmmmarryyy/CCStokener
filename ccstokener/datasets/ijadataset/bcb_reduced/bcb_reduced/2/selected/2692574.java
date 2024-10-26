package br.ufrgs.inf.prav.interop.metadata;

import br.ufrgs.inf.prav.interop.metadata.lom.OBAA;
import br.ufrgs.inf.prav.interop.metadata.lom.general.General;
import br.ufrgs.inf.prav.interop.metadata.lom.general.GeneralReader;
import br.ufrgs.inf.prav.interop.metadata.lom.technical.Technical;
import br.ufrgs.inf.prav.interop.metadata.lom.technical.TechnicalReader;
import br.ufrgs.inf.prav.interop.util.PathParser;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.jColtrane.handler.JColtraneXMLHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Fernando
 */
public class OBAAMetadataHandler {

    private static String metadataFileName = "lom000156.xml";

    public OBAAMetadataHandler() {
    }

    public static OBAA getResourceMetadata(FacesContext context, String lomId) {
        Map appAtts = context.getExternalContext().getApplicationMap();
        try {
            OBAA metadata = (OBAA) appAtts.get(getCurrentAppName(context) + lomId + "-metadata");
            if (metadata != null) {
                System.out.println("diferente de null");
                return metadata;
            } else {
                System.out.println("igual a null");
                if (loadResourceMetadata(context, lomId)) return (OBAA) appAtts.get(getCurrentAppName(context) + lomId + "-metadata"); else return new OBAA();
            }
        } catch (Exception e) {
            System.out.println("Erro em lomMetadata.getResourceMetadata: " + e.getMessage());
            loadResourceMetadata(context, lomId);
        }
        return null;
    }

    /**
     * Performs the load of the metadata for the current application. First, it
     * will find out which app the user is viewing, then it'll read the metada
     * from a file and put to the user session.
     * The metadata file MUST be name "lom.xml" and MUST be placed in the root
     * folder of the target tebsite (e.g. "storage/outras_infancias/lom.xml"
     * @param context   the FacesContext
     * @return  true if it was sucefull, false otherwise
     */
    private static boolean loadResourceMetadata(FacesContext context, String lomId) {
        ExternalContext ectx = context.getExternalContext();
        String appName = getCurrentAppName(context);
        HttpServletRequest request = (HttpServletRequest) ectx.getRequest();
        String reqURL = request.getRequestURL().toString();
        int indexOfPathInfo = reqURL.indexOf(appName);
        String metadataURL = reqURL.substring(0, indexOfPathInfo + appName.length());
        if (lomId != null) metadataURL += "metadata/" + lomId + ".xml"; else metadataURL += "metadata/" + metadataFileName;
        System.out.println("metadataURL: " + metadataURL);
        try {
            OBAA lom = loadMetadataFromURL(new URL(metadataURL));
            Map appAtts = context.getExternalContext().getApplicationMap();
            appAtts.put(appName + lomId + "-metadata", lom);
            return true;
        } catch (Exception e) {
            System.out.println("Erro em loadMetada: " + e.getMessage());
        }
        return false;
    }

    /**
     * Loads lom metadata from a file that must exist in the given URL
     * @param url   the URL to the file
     * @return  a Lom containing the read metadata
     */
    private static OBAA loadMetadataFromURL(URL url) {
        SAXParser parser = null;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        if (parser != null) {
            try {
                OBAA lom = new OBAA();
                GeneralReader generalReader = new GeneralReader();
                parser.parse(url.openStream(), new JColtraneXMLHandler(generalReader));
                General general = generalReader.getGeneral();
                if (general != null) lom.setGeneral(general);
                TechnicalReader technicalReader = new TechnicalReader();
                parser.parse(url.openStream(), new JColtraneXMLHandler(technicalReader));
                Technical technical = technicalReader.getTechnical();
                if (technical != null) {
                    lom.setTechnical(technical);
                }
                return lom;
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Get the name of the current application (website) that the user is
     * viewing. The name will be a composition of the base path ("storage) plus
     * the actual name of the website (e.g. "storage/outras_infancias"
     * @param context   the FacesContext
     * @return  the name of the current website
     */
    private static String getCurrentAppName(FacesContext context) {
        ExternalContext ectx = context.getExternalContext();
        String appName = ectx.getRequestPathInfo();
        appName = appName.replaceAll("/faces", "");
        appName = PathParser.setNumberOfDirs(appName, 2);
        return appName;
    }
}
