package org.gdi3d.xnavi.services.w3ds.x030;

import java.io.IOException;
import java.io.InputStream;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.services.w3ds.Attribute;
import org.gdi3d.xnavi.services.w3ds.Web3DService;

public class LayerInfoLoader {

    private final String service = "W3DS";

    private final String version = "0.3.0";

    private Web3DService web3DService;

    public LayerInfoLoader(Web3DService web3DService) {
        this.web3DService = web3DService;
    }

    public Attribute[] getAttributeNames(String layerName) {
        Attribute[] attributes = null;
        String request = this.web3DService.getServiceEndPoint() + "?" + "SERVICE=" + service + "&" + "REQUEST=getLayerInfo&" + "VERSION=" + version + "&" + "Layer=" + layerName;
        if (Navigator.isVerbose()) {
            System.out.println(request);
        }
        URL url = null;
        try {
            InputStream urlIn;
            url = new URL(request);
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            if (web3DService.getEncoding() != null) {
                urlc.setRequestProperty("Authorization", "Basic " + web3DService.getEncoding());
            }
            urlIn = urlc.getInputStream();
            if (urlIn != null) {
                org.gdi3d.xnavi.services.w3ds.x030.GetLayerInfoLoader getLayerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x030.GetLayerInfoLoader(urlIn);
                Vector<String> attributeNames = getLayerInfoLoader.getAttr();
                if (attributeNames != null) {
                    int numAttributeNames = attributeNames.size();
                    attributes = new Attribute[numAttributeNames];
                    for (int i = 0; i < numAttributeNames; i++) {
                        attributes[i] = new Attribute();
                        attributes[i].setName(attributeNames.get(i));
                    }
                }
                urlIn.close();
            }
        } catch (NoRouteToHostException e) {
            e.printStackTrace();
        } catch (java.lang.NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return attributes;
    }

    /**
	 * the Methode "getLayerInfoColumnNames" send the GetLayerInfo-Request with the called LayerName and ColumnName and return the response of Values from the Column.
	 * @param layerName
	 * @param columnname
	 * @return
	 */
    public Vector<String> getAttributeValues(String layerName, String columnname) {
        Vector<String> values = null;
        String request = null;
        request = this.web3DService.getServiceEndPoint() + "?" + "SERVICE=" + service + "&" + "REQUEST=getLayerInfo&" + "VERSION=" + version + "&" + "Layer=" + layerName + "&" + "columnName=" + columnname;
        if (Navigator.isVerbose()) {
            System.out.println(request);
        }
        URL url = null;
        try {
            InputStream urlIn;
            url = new URL(request);
            URLConnection conn = url.openConnection();
            if (web3DService.getEncoding() != null) {
                conn.setRequestProperty("Authorization", "Basic " + web3DService.getEncoding());
            }
            urlIn = conn.getInputStream();
            if (urlIn != null) {
                org.gdi3d.xnavi.services.w3ds.x030.GetLayerInfoLoader getLayerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x030.GetLayerInfoLoader(urlIn);
                Vector<String> attributeValues = getLayerInfoLoader.getValue();
                if (attributeValues != null) {
                    int numAttributeValues = attributeValues.size();
                    values = new Vector<String>();
                    for (int i = 0; i < numAttributeValues; i++) {
                        values.add(attributeValues.get(i));
                    }
                }
                urlIn.close();
            }
        } catch (NoRouteToHostException e) {
            e.printStackTrace();
        } catch (java.lang.NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return values;
    }
}
