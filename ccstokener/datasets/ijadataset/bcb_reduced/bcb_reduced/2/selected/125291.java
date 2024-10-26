package org.gdi3d.xnavi.services.w3ds.x040;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.swing.JOptionPane;
import net.opengis.ows.x11.CodeType;
import org.apache.xmlbeans.XmlException;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.services.w3ds.Attribute;
import org.gdi3d.xnavi.services.w3ds.LI_Layer;
import org.gdi3d.xnavi.services.w3ds.LayerInfo;
import org.gdi3d.xnavi.services.w3ds.Values;
import org.gdi3d.xnavi.services.w3ds.Web3DService;

public class LayerInfoLoader {

    private final String service = "W3DS";

    private final String version = "0.4.0";

    private Web3DService web3DService;

    public LayerInfoLoader(Web3DService web3DService) {
        this.web3DService = web3DService;
    }

    public Attribute[] getAttributeNames(String layerIdentifier) {
        Attribute[] attributes = null;
        String request = this.web3DService.getServiceEndPoint() + "?" + "SERVICE=" + service + "&" + "REQUEST=getLayerInfo&" + "VERSION=" + version + "&" + "Layer=" + layerIdentifier;
        LayerInfo layerInfo = load(request);
        if (layerInfo != null) {
            LI_Layer li_layer = layerInfo.getLI_Layer();
            if (li_layer != null) {
                attributes = li_layer.getAttributes();
            }
        }
        return attributes;
    }

    public Vector<String> getAttributeValues(String layerIdentifier, String attributeName) {
        Vector<String> values = null;
        String request = this.web3DService.getServiceEndPoint() + "?" + "SERVICE=" + service + "&" + "REQUEST=getLayerInfo&" + "VERSION=" + version + "&" + "Layer=" + layerIdentifier + "&" + "COLUMNNAMES=" + attributeName;
        LayerInfo layerInfo = load(request);
        if (layerInfo != null) {
            LI_Layer li_layer = layerInfo.getLI_Layer();
            if (li_layer != null) {
                Attribute[] attributes = li_layer.getAttributes();
                if (attributes != null && attributes.length > 0) {
                    Values vs = attributes[0].getValues();
                    if (vs != null) {
                        String[] valuesArray = vs.getValues();
                        if (valuesArray != null) {
                            values = new Vector<String>();
                            for (int i = 0; i < valuesArray.length; i++) {
                                values.add(valuesArray[i]);
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    private LayerInfo load(String request) {
        LayerInfo layerInfo = null;
        if (Navigator.isVerbose()) {
            System.out.println(request);
        }
        InputStream urlIn = null;
        try {
            URL url = new URL(request);
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            if (web3DService.getEncoding() != null) {
                urlc.setRequestProperty("Authorization", "Basic " + web3DService.getEncoding());
            }
            urlIn = urlc.getInputStream();
            if (urlIn != null) {
                layerInfo = load(urlIn);
            }
        } catch (org.gdi3d.xnavi.services.w3ds.x040.WrongLayerInfoVersionException wv) {
            if (Navigator.isVerbose()) {
                System.out.println("LayerInfo version is not 0.4.0");
            }
        } catch (java.net.SocketTimeoutException toe) {
            Object[] objects = { Navigator.i18n.getString("SYMBOLOGY_TIMEOUT") };
            JOptionPane.showMessageDialog(null, objects, Navigator.i18n.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                urlIn.close();
            } catch (Exception e) {
            }
        }
        return layerInfo;
    }

    private LayerInfo load(InputStream inputStream) throws IOException, WrongLayerInfoVersionException {
        LayerInfo layerInfo = null;
        net.opengis.w3Ds.x040.LayerInfoDocument poDoc;
        try {
            poDoc = net.opengis.w3Ds.x040.LayerInfoDocument.Factory.parse(inputStream);
            net.opengis.w3Ds.x040.LayerInfoType layerInfoType = poDoc.getLayerInfo();
            layerInfo = new LayerInfo();
            net.opengis.w3Ds.x040.LILayer[] opengis_LILayerArray = layerInfoType.getLILayerArray();
            if (opengis_LILayerArray != null && opengis_LILayerArray.length > 0) {
                layerInfo.setLI_Layer(parselI_Layer(opengis_LILayerArray[0]));
            }
        } catch (XmlException xe) {
            if (xe.getMessage().startsWith("error: The document is not a LayerInfo or the version is wrong")) {
                throw new WrongLayerInfoVersionException();
            } else {
                xe.printStackTrace();
            }
        }
        return layerInfo;
    }

    private LI_Layer parselI_Layer(net.opengis.w3Ds.x040.LILayer opengis_liLayer) {
        LI_Layer li_Layer = new LI_Layer();
        net.opengis.w3Ds.x040.Attribute[] opengis_AttributeArray = opengis_liLayer.getAttributeArray();
        if (opengis_AttributeArray != null) {
            Attribute[] attributeArray = new Attribute[opengis_AttributeArray.length];
            for (int i = 0; i < opengis_AttributeArray.length; i++) {
                attributeArray[i] = parseAttribute(opengis_AttributeArray[i]);
            }
            li_Layer.setAttributes(attributeArray);
        }
        CodeType identifier = opengis_liLayer.getIdentifier();
        if (identifier != null) {
            li_Layer.setIdentifier(identifier.getStringValue());
        }
        return li_Layer;
    }

    private Attribute parseAttribute(net.opengis.w3Ds.x040.Attribute opengis_Attribute) {
        Attribute attribute = new Attribute();
        CodeType name = opengis_Attribute.getName();
        if (name != null) {
            attribute.setName(name.getStringValue());
        }
        attribute.setType(opengis_Attribute.getType());
        attribute.setUniqueCount(opengis_Attribute.getUniqueCount());
        net.opengis.w3Ds.x040.Values opengis_Values = opengis_Attribute.getValues();
        if (opengis_Values != null) {
            attribute.setValues(parseValues(opengis_Values));
        }
        return attribute;
    }

    private org.gdi3d.xnavi.services.w3ds.Values parseValues(net.opengis.w3Ds.x040.Values opengis_Values) {
        Values values = new Values();
        values.setValues(opengis_Values.getValueArray());
        return values;
    }
}
