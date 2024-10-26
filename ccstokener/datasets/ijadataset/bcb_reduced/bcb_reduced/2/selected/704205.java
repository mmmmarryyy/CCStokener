package org.gvsig.remoteClient.arcims;

import com.iver.cit.gvsig.fmap.core.v02.FConstant;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.fmap.rendering.ILegend;
import org.apache.log4j.Logger;
import org.gvsig.remoteClient.arcims.exceptions.ArcImsException;
import org.gvsig.remoteClient.arcims.styling.renderers.ArcImsFLegendFactory;
import org.gvsig.remoteClient.arcims.styling.renderers.ArcImsRendererFactory;
import org.gvsig.remoteClient.arcims.styling.renderers.Renderer;
import org.gvsig.remoteClient.arcims.utils.ArcImsDownloadUtils;
import org.gvsig.remoteClient.arcims.utils.CatalogInfoTags;
import org.gvsig.remoteClient.arcims.utils.FieldInformation;
import org.gvsig.remoteClient.arcims.utils.GetVariables;
import org.gvsig.remoteClient.arcims.utils.ServiceInfoTags;
import org.gvsig.remoteClient.arcims.utils.ServiceInformation;
import org.gvsig.remoteClient.arcims.utils.ServiceInformationLayer;
import org.gvsig.remoteClient.arcims.utils.ServiceInformationLayerFeatures;
import org.gvsig.remoteClient.arcims.utils.ServiceInformationLayerImage;
import org.gvsig.remoteClient.utils.BoundaryBox;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Abstract class that represents handlers to comunicate via ArcIms protocol
 *
 * @author jsanz
 * @author jcarrasco
 *
 */
public abstract class ArcImsProtocolHandler {

    public static final double INCHES = .02540005;

    private static Logger logger = Logger.getLogger(ArcImsProtocolHandler.class.getName());

    /**
     * Protocol handler name
     */
    protected String name;

    /**
     * protocol handler version
     */
    protected String version;

    /**
     * host of the ArcIms to connect
     */
    protected String host;

    /**
     * port number of the comunication channel of the ArcIms to connect
     */
    protected String port;

    protected String service;

    /**
     * ArcIms metadata
     */
    protected ServiceInformation serviceInfo;

    public TreeMap layers;

    public Vector srs;

    public ArcImsProtocolHandler() {
        this.version = "4.0.1";
        this.name = "ArcIms4.0.1";
        this.serviceInfo = new ServiceInformation();
        this.layers = new TreeMap();
    }

    /**
     * Parse the xml data retrieved from the ArcIms, it will parse the ArcIms Capabilities
     * @param f the XML file to parse to obtain the ServiceInfo
     * @throws ArcImsException
     *
     */
    public ServiceInformation parseServiceInfo(ServiceInformation si, File f) throws ArcImsException {
        Reader reader = null;
        ServiceInformation my_si = si;
        try {
            FileInputStream fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            reader = br;
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        int tag;
        KXmlParser kxmlParser = null;
        kxmlParser = new KXmlParser();
        try {
            kxmlParser.setInput(reader);
            kxmlParser.nextTag();
            if (kxmlParser.getEventType() != KXmlParser.END_DOCUMENT) {
                kxmlParser.require(KXmlParser.START_TAG, null, ServiceInfoTags.tARCXML);
                tag = kxmlParser.nextTag();
                while (tag != KXmlParser.END_DOCUMENT) {
                    switch(tag) {
                        case KXmlParser.START_TAG:
                            String name = kxmlParser.getName();
                            if (name.compareTo(ServiceInfoTags.tENVIRONMENT) == 0) {
                                parseEnvironmentTag(my_si, kxmlParser);
                            } else if (name.compareTo(ServiceInfoTags.tPROPERTIES) == 0) {
                                parsePropertiesTag(my_si, kxmlParser);
                            } else if (name.compareTo(ServiceInfoTags.tLAYERINFO) == 0) {
                                parseLayerInfoTag(my_si, kxmlParser);
                            } else if (name.compareTo(ServiceInfoTags.tERROR) == 0) {
                                logger.error("Error parsing GET_SERVICE_INFO:\r\n" + kxmlParser.nextText());
                                throw new ArcImsException("arcims_catalog_error");
                            }
                            break;
                        case KXmlParser.END_TAG:
                            break;
                        case KXmlParser.TEXT:
                            break;
                    }
                    tag = kxmlParser.next();
                }
                kxmlParser.require(KXmlParser.END_DOCUMENT, null, null);
            }
        } catch (XmlPullParserException parser_ex) {
            logger.error("Parser error", parser_ex);
            throw new ArcImsException("parse_error");
        } catch (FileNotFoundException fe) {
            logger.error("FileNotFound Error", fe);
            throw new ArcImsException("arcims_server_error");
        } catch (IOException e) {
            logger.error("IO Error", e);
            throw new ArcImsException("arcims_server_error");
        } catch (NumberFormatException nfe) {
            logger.warn("NumberFormat Error");
            char dsTemp = my_si.getSeparators().getDs();
            if (dsTemp == '.') {
                logger.warn("NumberFormat Error, changing . by ,");
                my_si.getSeparators().setDs(',');
                my_si = this.parseServiceInfo(my_si, f);
            } else {
                if (dsTemp == ',') {
                    logger.warn("NumberFormat Error, changing , by .");
                    my_si.getSeparators().setDs('.');
                    my_si = this.parseServiceInfo(my_si, f);
                } else {
                    logger.warn("NumberFormat Error: No Decimal Separator found");
                    throw new ArcImsException("arcims_decimal_error");
                }
            }
        } finally {
        }
        return my_si;
    }

    /**
    * <p>Parses the Environment Information </p>
     * @param my_si 
    * @param parser A KXmlParser object to parse
    * @throws IOException
    * @throws XmlPullParserException
    */
    private void parseEnvironmentTag(ServiceInformation my_si, KXmlParser parser) throws IOException, XmlPullParserException {
        int currentTag;
        boolean end = false;
        ServiceInformation serviceInfo = my_si;
        parser.require(KXmlParser.START_TAG, null, ServiceInfoTags.tENVIRONMENT);
        currentTag = parser.next();
        while (!end) {
            switch(currentTag) {
                case KXmlParser.START_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tLOCALE) == 0) {
                        String language = null;
                        language = parser.getAttributeValue("", ServiceInfoTags.aLANGUAGE);
                        if (language != null) {
                            serviceInfo.getLocale().setLanguage(language);
                        }
                        String country = null;
                        country = parser.getAttributeValue("", ServiceInfoTags.aCOUNTRY);
                        if (country != null) {
                            serviceInfo.getLocale().setCountry(country);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tUIFONT) == 0) {
                        String name = null;
                        name = parser.getAttributeValue("", ServiceInfoTags.aNAME);
                        if (name != null) {
                            serviceInfo.getUifont().setName(name);
                        }
                        String color = null;
                        color = parser.getAttributeValue("", ServiceInfoTags.aCOLOR);
                        if (color != null) {
                            serviceInfo.getUifont().setColor(color);
                        }
                        String size = null;
                        size = parser.getAttributeValue("", ServiceInfoTags.aSIZE);
                        if (name != null) {
                            serviceInfo.getUifont().setSize(size);
                        }
                        String style = null;
                        style = parser.getAttributeValue("", ServiceInfoTags.aSTYLE);
                        if (style != null) {
                            serviceInfo.getUifont().setStyle(style);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tSEPARATORS) == 0) {
                        String ts = null;
                        ts = parser.getAttributeValue("", ServiceInfoTags.aTS);
                        if (ts != null) {
                            serviceInfo.getSeparators().setTs(ts);
                        }
                        String cs = null;
                        cs = parser.getAttributeValue("", ServiceInfoTags.aCS);
                        if (cs != null) {
                            serviceInfo.getSeparators().setCs(cs);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tSCREEN) == 0) {
                        String dpi = null;
                        dpi = parser.getAttributeValue("", ServiceInfoTags.aDPI);
                        if (dpi != null) {
                            serviceInfo.setScreen_dpi(Integer.parseInt(dpi));
                            serviceInfo.setDpiAssumed(false);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tIMAGELIMIT) == 0) {
                        String pixelcount = null;
                        pixelcount = parser.getAttributeValue("", ServiceInfoTags.aPIXELCOUNT);
                        if (pixelcount != null) {
                            serviceInfo.setImagelimit_pixelcount(pixelcount);
                        }
                    }
                    break;
                case KXmlParser.END_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tENVIRONMENT) == 0) {
                        end = true;
                    }
                    break;
                case KXmlParser.TEXT:
                    break;
            }
            if (!end) {
                currentTag = parser.next();
            }
        }
        if (serviceInfo.getSeparators().getDs() == 'c') {
            String lang = this.serviceInfo.getLocale().getLanguage();
            String coun = this.serviceInfo.getLocale().getCountry();
            Locale local = new Locale(lang, coun);
            DecimalFormatSymbols dfs = new DecimalFormatSymbols(local);
            serviceInfo.getSeparators().setDs(dfs.getDecimalSeparator());
        }
        parser.require(KXmlParser.END_TAG, null, ServiceInfoTags.tENVIRONMENT);
    }

    /**
     * <p>Parses the Properties Information </p>
     * @param my_si 
     * @param parser A KXmlParser object to parse
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void parsePropertiesTag(ServiceInformation my_si, KXmlParser parser) throws IOException, XmlPullParserException, NumberFormatException {
        int currentTag;
        boolean end = false;
        ServiceInformation serviceInfo = my_si;
        parser.require(KXmlParser.START_TAG, null, ServiceInfoTags.tPROPERTIES);
        currentTag = parser.next();
        char ds = my_si.getSeparators().getDs();
        while (!end) {
            switch(currentTag) {
                case KXmlParser.START_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tENVELOPE) == 0) {
                        BoundaryBox parseEnvelope = new BoundaryBox();
                        String value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMINX);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            parseEnvelope.setXmin(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMINY);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            parseEnvelope.setYmin(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMAXY);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            parseEnvelope.setYmax(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMAXX);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            parseEnvelope.setXmax(Double.parseDouble(value));
                        }
                        if ((parser.getAttributeValue("", ServiceInfoTags.aNAME) != null) && (parser.getAttributeValue("", ServiceInfoTags.aNAME).compareTo(ServiceInfoTags.aENVELOPEEL) == 0)) {
                            serviceInfo.setEnvelopeEL(parseEnvelope);
                        } else {
                            serviceInfo.setEnvelope(parseEnvelope);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tMAPUNITS) == 0) {
                        String value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aUNITS);
                        if (value != null) {
                            serviceInfo.setMapunits(value);
                        }
                    } else if (parser.getName().compareTo(ServiceInfoTags.tFEATURECOORDSYS) == 0) {
                        String value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aID);
                        if (value != null) {
                            serviceInfo.setFeaturecoordsys(value);
                            serviceInfo.setSrsAssumed(false);
                        }
                    }
                    break;
                case KXmlParser.END_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tPROPERTIES) == 0) {
                        end = true;
                    }
                    break;
                case KXmlParser.TEXT:
                    break;
            }
            if (!end) {
                currentTag = parser.next();
            }
        }
        parser.require(KXmlParser.END_TAG, null, ServiceInfoTags.tPROPERTIES);
        if (serviceInfo.getFeaturecoordsys().compareTo("") == 0) {
            serviceInfo.setSrsAssumed(true);
        }
    }

    /**
     * <p>Parses the LayerInfo Information </p>
     * @param my_si 
     * @param parser A KXmlParser object to parse
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ArcImsException
     */
    private void parseLayerInfoTag(ServiceInformation my_si, KXmlParser parser) throws IOException, XmlPullParserException, NumberFormatException, ArcImsException {
        String value = null;
        String type = null;
        ServiceInformation serviceInfo = my_si;
        char ds = serviceInfo.getSeparators().getDs();
        ServiceInformationLayer sil;
        type = parser.getAttributeValue("", ServiceInfoTags.aTYPE);
        if (type.compareToIgnoreCase(ServiceInfoTags.vLAYERTYPE_F) == 0) {
            sil = new ServiceInformationLayerFeatures("");
        } else {
            sil = new ServiceInformationLayerImage();
        }
        value = parser.getAttributeValue("", ServiceInfoTags.aID);
        if (value != null) {
            sil.setId(value);
        }
        value = parser.getAttributeValue("", ServiceInfoTags.aVISIBLE);
        if (value != null) {
            sil.setVisible(value);
        }
        value = parser.getAttributeValue("", ServiceInfoTags.aMAXSCALE);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            sil.setMaxscale(Double.parseDouble(value));
        }
        value = parser.getAttributeValue("", ServiceInfoTags.aMINSCALE);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            sil.setMinscale(Double.parseDouble(value));
        }
        value = parser.getAttributeValue("", ServiceInfoTags.aNAME);
        if (value != null) {
            sil.setName(value);
        }
        int currentTag;
        boolean end = false;
        currentTag = parser.next();
        while (!end) {
            switch(currentTag) {
                case KXmlParser.START_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tENVELOPE) == 0) {
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMINX);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            sil.getEnvelope().setXmin(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMINY);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            sil.getEnvelope().setYmin(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMAXX);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            sil.getEnvelope().setXmax(Double.parseDouble(value));
                        }
                        value = null;
                        value = parser.getAttributeValue("", ServiceInfoTags.aMAXY);
                        if (value != null) {
                            if (ds != '.') {
                                value = value.replace(ds, '.');
                            }
                            sil.getEnvelope().setYmax(Double.parseDouble(value));
                        }
                    }
                    if (type.compareToIgnoreCase(ServiceInfoTags.vLAYERTYPE_F) == 0) {
                        ServiceInformationLayerFeatures silf = (ServiceInformationLayerFeatures) sil;
                        if (parser.getName().compareTo(ServiceInfoTags.tFCLASS) == 0) {
                            value = null;
                            value = parser.getAttributeValue("", ServiceInfoTags.aTYPE);
                            if (value != null) {
                                silf.setFclasstype(value);
                            }
                        } else if (parser.getName().compareTo(ServiceInfoTags.tFIELD) == 0) {
                            FieldInformation fieldInfo = new FieldInformation();
                            value = null;
                            value = parser.getAttributeValue("", ServiceInfoTags.aNAME);
                            if (value != null) {
                                fieldInfo.setName(value);
                            }
                            value = null;
                            value = parser.getAttributeValue("", ServiceInfoTags.aTYPE);
                            if (value != null) {
                                fieldInfo.setType(Integer.parseInt(value));
                            }
                            value = null;
                            value = parser.getAttributeValue("", ServiceInfoTags.aPRECISION);
                            if (value != null) {
                                fieldInfo.setPrecision(Integer.parseInt(value));
                            }
                            value = null;
                            value = parser.getAttributeValue("", ServiceInfoTags.aSIZE);
                            if (value != null) {
                                fieldInfo.setSize(Integer.parseInt(value));
                            }
                            silf.addFieldInformation(fieldInfo);
                        } else if (ArcImsRendererFactory.isRenderer(parser.getName())) {
                            String fclasstype = silf.getFclasstype();
                            int ftype = getFType(fclasstype);
                            ArcImsRendererFactory airf = new ArcImsRendererFactory(ftype);
                            Renderer render = airf.getRenderer(parser);
                            silf.setLayerMainRenderer(render);
                        }
                    }
                    break;
                case KXmlParser.END_TAG:
                    if (parser.getName().compareTo(ServiceInfoTags.tLAYERINFO) == 0) {
                        end = true;
                    }
                    break;
                case KXmlParser.TEXT:
                    break;
            }
            if (!end) {
                currentTag = parser.next();
            }
        }
        serviceInfo.addLayer(sil);
    }

    /**
     * Converts ArcIMS feature type to a FConstant value
     * @param fclasstype
     * @return
     */
    public static int getFType(String fclasstype) {
        if (fclasstype.equals(ServiceInfoTags.aPOLYGON)) {
            return FConstant.SHAPE_TYPE_POLYGON;
        }
        if (fclasstype.equals(ServiceInfoTags.aPOLYLINE)) {
            return FConstant.SHAPE_TYPE_POLYLINE;
        }
        if (fclasstype.equals(ServiceInfoTags.aMULTIPOINT)) {
            return FConstant.SHAPE_TYPE_MULTIPOINT;
        }
        return FConstant.SHAPE_TYPE_NULL;
    }

    /**
     * Method to parse a request to obtain the Extent of a Service
     * @param reader
     * @param ds the Decimal Separator
     * @return Rectangle with the boundary box of the Service
       * @throws ArcImsException
     */
    protected Rectangle2D parseEnvelopeTag(FileReader reader, char ds) throws ArcImsException {
        BoundaryBox parseEnvelope = new BoundaryBox();
        try {
            KXmlParser parser = new KXmlParser();
            parser.setInput(reader);
            int currentTag;
            boolean end = false;
            currentTag = parser.next();
            while (!end) {
                switch(currentTag) {
                    case KXmlParser.START_TAG:
                        if (parser.getName().compareTo(ServiceInfoTags.tENVELOPE) == 0) {
                            parseEnvelope = parseEnvelope(parser, ds);
                        } else if (parser.getName().compareTo(ServiceInfoTags.tERROR) == 0) {
                            logger.error("Error requesting Service Extent:\r\n" + parser.nextText());
                            throw new ArcImsException("arcims_extent_error");
                        }
                        break;
                    case KXmlParser.END_TAG:
                        if (parser.getName().compareTo(ServiceInfoTags.tARCXML) == 0) {
                            end = true;
                        }
                        break;
                    case KXmlParser.TEXT:
                        break;
                }
                if (!end) {
                    currentTag = parser.next();
                }
            }
        } catch (XmlPullParserException parser_ex) {
            parser_ex.printStackTrace();
        } catch (ConnectException ce) {
            logger.error("Timed out error", ce);
            throw new ArcImsException("arcims_server_timeout");
        } catch (FileNotFoundException fe) {
            logger.error("FileNotFound Error", fe);
            throw new ArcImsException("arcims_server_error");
        } catch (IOException e) {
            logger.error("IO Error", e);
            throw new ArcImsException("arcims_server_error");
        }
        Rectangle2D rect = new Rectangle2D.Double();
        rect.setFrameFromDiagonal(parseEnvelope.getXmin(), parseEnvelope.getYmin(), parseEnvelope.getXmax(), parseEnvelope.getYmax());
        return rect;
    }

    /**
     * Static method to create a BoundaryBox from a ENVELOPE tag.
     * @param parser
     * @param ds
     * @return
     */
    protected static BoundaryBox parseEnvelope(KXmlParser parser, char ds) {
        BoundaryBox parseEnvelope = new BoundaryBox();
        String value = null;
        value = parser.getAttributeValue("", ServiceInfoTags.aMINX);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            parseEnvelope.setXmin(Double.parseDouble(value));
        }
        value = null;
        value = parser.getAttributeValue("", ServiceInfoTags.aMINY);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            parseEnvelope.setYmin(Double.parseDouble(value));
        }
        value = null;
        value = parser.getAttributeValue("", ServiceInfoTags.aMAXY);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            parseEnvelope.setYmax(Double.parseDouble(value));
        }
        value = null;
        value = parser.getAttributeValue("", ServiceInfoTags.aMAXX);
        if (value != null) {
            if (ds != '.') {
                value = value.replace(ds, '.');
            }
            parseEnvelope.setXmax(Double.parseDouble(value));
        }
        return parseEnvelope;
    }

    public abstract String getElementInfo(ArcImsStatus status, int x, int y, int featureCount) throws ArcImsException;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ServiceInformation getServiceInformation() {
        return this.serviceInfo;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String _host) {
        host = _host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String _port) {
        port = _port;
    }

    /**
     * Method to request a GET_SERVICE_INFO to an ArcIMS Server
     * @param status
     * @throws ArcImsException
     */
    public void getCapabilities(ArcImsStatus status) throws ArcImsException {
        try {
            String request = ArcXML.getServiceInfoRequest(getVersion());
            URL url = new URL(buildCapabilitiesRequest(status));
            logger.info("Requesting ArcIMS Service Information");
            File f = ArcImsDownloadUtils.doRequestPost(url, request, "serviceInfo.xml");
            this.serviceInfo = parseServiceInfo(this.serviceInfo, f);
        } catch (MalformedURLException e) {
            logger.error(e.getMessage(), e);
            throw new ArcImsException("arcims_server_error");
        }
    }

    /**
     * Builds the GetCapabilitiesRequest according to the ArcIms Specifications stored in
     * a @see ArcImsStatus object
     * @param status
     * @return string A convenient request
     */
    protected String buildCapabilitiesRequest(ArcImsStatus status) {
        StringBuffer req = new StringBuffer();
        String onlineResource;
        String service;
        if ((status == null) || (status.getOnlineResource() == null)) {
            onlineResource = getHost();
            service = getService();
        } else {
            onlineResource = status.getOnlineResource();
            service = status.getService();
        }
        req.append(onlineResource);
        req.append("?" + GetVariables.SERVICENAME + "=");
        req.append(service);
        return req.toString();
    }

    /**
     * Builds the GetFeatureInfo according to the ArcIms Specifications stored in
     * a @see ArcImsStatus object
     * @param status
     * @param _type of ArcIMS service
     * @return string A convenient request
     */
    protected String buildGetFeatureInfoRequest(ArcImsStatus status, String _type) {
        StringBuffer req = new StringBuffer();
        String onlineResource;
        String service;
        onlineResource = getHost();
        if ((status == null) || (status.getOnlineResource() == null)) {
            onlineResource = getHost();
            service = getService();
        } else {
            onlineResource = status.getOnlineResource();
            service = status.getService();
        }
        req.append(onlineResource);
        req.append("?" + GetVariables.SERVICENAME + "=");
        req.append(service);
        if ((this.serviceInfo.getType().equals(ServiceInfoTags.vIMAGESERVICE)) && _type.equals(ServiceInfoTags.vLAYERTYPE_F)) {
            req.append("&" + GetVariables.CUSTOMSERVICE + "=" + GetVariables.QUERY);
        }
        return req.toString();
    }

    public void close() {
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    /**
     * Method that retrieves a catalog from the ArcIMS Server and returns an ArrayList
     * of name, type and status of every service offered by the server
     * @param url
     * @return ArrayList
     * @throws ArcImsException
     */
    public static ArrayList getCatalog(URL url, boolean override) throws ArcImsException {
        ArrayList services = new ArrayList();
        try {
            URL urlComplete = new URL(url.toString() + "?" + GetVariables.SERVICENAME + "=" + GetVariables.CATALOG);
            String request = ArcXML.getClientServices();
            logger.info("Requesting ArcIMS Catalog");
            File f = ArcImsDownloadUtils.doRequestPost(urlComplete, request, "getClientServices.xml", override);
            services = parseClientServices(f);
        } catch (MalformedURLException e) {
            logger.error(e.getMessage(), e);
            throw new ArcImsException("arcims_server_error");
        }
        return services;
    }

    /**
     * Gets the catalog without setting the override boolean
     * @see #getCatalog(URL, boolean)
     * @param url
     * @return ArrayList with the Catalgo of the Service
     * @throws ArcImsException
     */
    public static ArrayList getCatalog(URL url) throws ArcImsException {
        return getCatalog(url, false);
    }

    /**
     * Method that parses a ArcXML response for the GETCLIENTSERVICES request
     * @param f
     * @return ArrayList of ArrayLists with name, type and status
     * @throws ArcImsException
     */
    private static ArrayList parseClientServices(File f) throws ArcImsException {
        ArrayList services = new ArrayList();
        ArrayList service;
        try {
            KXmlParser parser = new KXmlParser();
            parser.setInput(new FileReader(f));
            int currentTag;
            boolean end = false;
            currentTag = parser.next();
            while (!end) {
                switch(currentTag) {
                    case KXmlParser.START_TAG:
                        if (parser.getName().compareTo(CatalogInfoTags.SERVICE) == 0) {
                            String servName = new String();
                            String servType = new String();
                            String servStatus = new String();
                            String value = null;
                            value = parser.getAttributeValue("", CatalogInfoTags.SERVICE_ATT_NAME);
                            if (value != null) {
                                servName = value;
                            }
                            value = null;
                            value = parser.getAttributeValue("", CatalogInfoTags.SERVICE_ATT_TYPE);
                            if (value != null) {
                                servType = value;
                            }
                            value = null;
                            value = parser.getAttributeValue("", CatalogInfoTags.SERVICE_ATT_STATUS);
                            if (value != null) {
                                servStatus = value;
                            }
                            service = new ArrayList();
                            service.add(servName);
                            service.add(servType);
                            service.add(servStatus);
                            services.add(service);
                            service = null;
                        }
                        break;
                    case KXmlParser.END_TAG:
                        if (parser.getName().compareTo(ServiceInfoTags.tARCXML) == 0) {
                            end = true;
                        }
                        break;
                    case KXmlParser.TEXT:
                        break;
                }
                if (!end) {
                    currentTag = parser.next();
                }
            }
        } catch (XmlPullParserException parser_ex) {
            parser_ex.printStackTrace();
        } catch (ConnectException ce) {
            logger.error("Timed out error", ce);
            throw new ArcImsException("arcims_server_timeout");
        } catch (FileNotFoundException fe) {
            logger.error("FileNotFound Error", fe);
            throw new ArcImsException("arcims_server_error");
        } catch (IOException e) {
            logger.error("IO Error", e);
            throw new ArcImsException("arcims_server_error");
        }
        return services;
    }

    /**
     * Method that does a Ping to an ArcIMS Server
     * @param url
     * @return true when server exits, false otherwise
     * @throws ArcImsException
     */
    public static boolean getPing(URL url) throws ArcImsException {
        try {
            URL urlWithPing = new URL(url.toString() + "?" + GetVariables.COMMAND + "=" + GetVariables.PING);
            HttpURLConnection conn = (HttpURLConnection) urlWithPing.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            logger.info("Trying connect to: " + conn.getURL().toString());
            conn.connect();
            Reader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK) && line.startsWith("IMS")) {
                logger.info("Connection succeeded");
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.warn("Malformed url", e);
            return false;
        } catch (ConnectException ce) {
            logger.error("Timed out error", ce);
            throw new ArcImsException("arcims_server_timeout");
        } catch (NullPointerException npe) {
            logger.error("NullPointerException", npe);
            throw new ArcImsException("arcims_server_error");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Method that pings with several SERVLETS trying to find a correct one.
     * @param url to start searching
     * @return Correct URL
     * @throws ArcImsException
     */
    public static URL getUrlWithServlet(URL url) throws ArcImsException {
        URL urlTemp;
        URL urlWithServlet = null;
        if (url.toString().endsWith("/")) {
            String strUrl = url.toString();
            int size = strUrl.length();
            strUrl = strUrl.substring(0, size - 1);
            try {
                url = new URL(strUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
                throw new ArcImsException("wrong_url");
            }
        }
        int n = GetVariables.SERVLETS.length;
        logger.info("Start searching correct servlet");
        for (int i = 0; i < n; i++) {
            try {
                urlTemp = new URL(url.toString() + GetVariables.SERVLETS[i]);
            } catch (MalformedURLException e) {
                continue;
            }
            if (ArcImsProtocolHandler.getPing(urlTemp)) {
                urlWithServlet = urlTemp;
                break;
            }
        }
        if (urlWithServlet == null) {
            throw new ArcImsException("arcims_no_server");
        }
        logger.info("Complete ArcIMS URL: " + urlWithServlet.toString());
        return urlWithServlet;
    }

    public static String[] getVersion(URL url) throws ArcImsException {
        String[] getV = new String[2];
        Reader isr;
        URL urlCmd;
        HttpURLConnection conn;
        try {
            urlCmd = new URL(url.toString() + "?" + GetVariables.COMMAND + "=" + GetVariables.GETVERSION);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new ArcImsException("arcims_no_server");
        }
        try {
            conn = (HttpURLConnection) urlCmd.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.connect();
            isr = new InputStreamReader(conn.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String strTemp = new String();
            strTemp = reader.readLine();
            getV[0] = strTemp.substring(strTemp.indexOf("=") + 1);
            strTemp = reader.readLine();
            getV[1] = strTemp.substring(strTemp.indexOf("=") + 1);
            conn.disconnect();
        } catch (ConnectException ce) {
            logger.error("Timed out error", ce);
            throw new ArcImsException("arcims_server_timeout");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ArcImsException("arcims_no_server");
        }
        return getV;
    }

    /**
     * This method returns a Legend to the client requested by
     * layerId string
     * @param layerId
     * @return Legend with layer simbology
     * @throws ArcImsException
     */
    public ILegend getLegend(String layerId, SelectableDataSource sds) throws ArcImsException {
        ArcImsFLegendFactory aiff = new ArcImsFLegendFactory(this.serviceInfo, layerId);
        return aiff.getMainLegend(sds);
    }
}
