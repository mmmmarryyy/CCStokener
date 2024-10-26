package com.esri.gpt.server.openls.provider.services.poi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.esri.gpt.server.csw.provider.components.OwsException;
import com.esri.gpt.server.openls.provider.components.IOperationProvider;
import com.esri.gpt.server.openls.provider.components.IProviderFactory;
import com.esri.gpt.server.openls.provider.components.IResponseGenerator;
import com.esri.gpt.server.openls.provider.components.OperationContext;
import com.esri.gpt.server.openls.provider.util.DistanceCalc;
import com.esri.gpt.server.openls.provider.util.Point;
import com.esri.gpt.server.openls.provider.util.Units;

/**
 * Provides the Openls Directory operation.
 */
public class DirectoryProvider implements IOperationProvider {

    /** The Logger. */
    private static Logger LOGGER = Logger.getLogger(DirectoryProvider.class.getName());

    /** Default constructor */
    public DirectoryProvider() {
        super();
    }

    /**
   * Executes a parsed operation request.
   * @param context the operation context
   * @throws Exception if a processing exception occurs
   */
    private void generateResponse(OperationContext context) throws Exception {
        LOGGER.finer("Executing xls:Directory request...");
        IProviderFactory factory = context.getProviderFactory();
        IResponseGenerator generator = factory.makeResponseGenerator(context);
        if (generator == null) {
            String msg = "IProviderFactory.makeResponseGenerator: instantiation failed.";
            LOGGER.log(Level.SEVERE, msg);
            throw new OwsException(msg);
        } else {
            generator.generateResponse(context);
        }
    }

    /**
   * Handles a URL based request (HTTP GET).
   * @param context the operation context
   * @param request the HTTP request
   * @throws Exception if a processing exception occurs
   */
    public void handleGet(OperationContext context, HttpServletRequest request) throws Exception {
    }

    /**
   * Handles an XML based request (normally HTTP POST).
   * @param context the operation context
   * @param root the root node
   * @param xpath an XPath to enable queries (properly configured with name spaces)
   * @throws Exception if a processing exception occurs
   */
    public void handleXML(OperationContext context, Node root, XPath xpath) throws Exception {
        LOGGER.finer("Handling xls:Directory request XML...");
        String locator = "xls:DirectoryRequest";
        Node ndReq = (Node) xpath.evaluate(locator, root, XPathConstants.NODE);
        if (ndReq != null) {
            parseRequest(context, ndReq, xpath);
        }
        try {
            executeRequest(context);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        generateResponse(context);
    }

    /**
   * Parse directory request
   * @param context
   * @param ndReq
   * @param xpath
   * @throws XPathExpressionException
   */
    private void parseRequest(OperationContext context, Node ndReq, XPath xpath) throws XPathExpressionException {
        DirectoryParams params = context.getRequestOptions().getDirectoryOptions();
        HashMap<String, String> poiProperties = null;
        HashMap<String, Object> poiLocations = null;
        Node ndPoiLoc = (Node) xpath.evaluate("xls:POILocation", ndReq, XPathConstants.NODE);
        if (ndPoiLoc != null) {
            poiLocations = new HashMap<String, Object>();
            Node ndPos = (Node) xpath.evaluate("xls:Nearest/xls:Position/gml:Point/gml:pos", ndPoiLoc, XPathConstants.NODE);
            if (ndPos != null) {
                String[] xy = ndPos.getTextContent().split(" ");
                Point loc = new Point(xy[0], xy[1]);
                poiLocations.put("nearest", loc);
            }
            @SuppressWarnings("unused") Node ndWDAL = (Node) xpath.evaluate("xls:WithinDistance/xls:POI/xls:POIAttributeList/xls:POIInfoList", ndPoiLoc, XPathConstants.NODE);
            String maxDist = (String) xpath.evaluate("xls:WithinDistance/xls:MaximumDistance/@value", ndPoiLoc, XPathConstants.STRING);
            if (maxDist != null) {
                poiLocations.put("withinDistance", maxDist);
            }
        }
        Node ndPoiProp = (Node) xpath.evaluate("xls:POIProperties", ndReq, XPathConstants.NODE);
        if (ndPoiProp != null) {
            NodeList nlProp = (NodeList) xpath.evaluate("xls:POIProperty", ndPoiProp, XPathConstants.NODESET);
            if (nlProp != null) {
                for (int j = 0; j < nlProp.getLength(); j++) {
                    Node ndProp = nlProp.item(j);
                    poiProperties = new HashMap<String, String>();
                    String name = (String) xpath.evaluate("@name", ndProp, XPathConstants.STRING);
                    String param = context.getRequestContext().getApplicationConfiguration().getCatalogConfiguration().getParameters().getValue(name);
                    String value = (String) xpath.evaluate("@value", ndProp, XPathConstants.STRING);
                    poiProperties.put(param, value);
                }
            }
        }
        params.setPoiLocations(poiLocations);
        params.setPoiProperties(poiProperties);
    }

    /**
     * Executes directory request
     * @param context
     * @throws java.lang.Throwable
     */
    private void executeRequest(OperationContext context) throws java.lang.Throwable {
        long t1 = System.currentTimeMillis();
        DirectoryParams params = context.getRequestOptions().getDirectoryOptions();
        try {
            String srvCfg = context.getRequestContext().getApplicationConfiguration().getCatalogConfiguration().getParameters().getValue("openls.directory");
            HashMap<String, String> poiProperties = params.getPoiProperties();
            Set<String> keys = poiProperties.keySet();
            Iterator<String> iter = keys.iterator();
            StringBuffer filter = new StringBuffer();
            while (iter.hasNext()) {
                String key = iter.next();
                QueryFilter queryFilter = new QueryFilter(key, poiProperties.get(key));
                filter.append(makePOIRequest(queryFilter));
            }
            String sUrl = srvCfg + "/query?" + filter.toString();
            LOGGER.info("REQUEST=\n" + sUrl);
            URL url = new URL(sUrl);
            URLConnection conn = url.openConnection();
            String line = "";
            String sResponse = "";
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader rd = new BufferedReader(isr);
            while ((line = rd.readLine()) != null) {
                sResponse += line;
            }
            rd.close();
            url = null;
            parsePOIResponse(sResponse, params);
        } catch (Exception p_e) {
            LOGGER.severe("Throwing exception" + p_e.getMessage());
            throw p_e;
        } finally {
            long t2 = System.currentTimeMillis();
            LOGGER.info("PERFORMANCE: " + (t2 - t1) + " ms spent performing service");
        }
    }

    private String makePOIRequest(QueryFilter qfilters) throws UnsupportedEncodingException {
        StringBuffer params = new StringBuffer();
        params.append("text=&geometry=&geometryType=esriGeometryPoint&inSR=&spatialRel=esriSpatialRelIntersects").append("&relationParam=&objectIds=&time=&returnIdsOnly=false&returnGeometry=true&maxAllowableOffset=&outSR=&outFields=name,match_addr,type,desc_,source,distance,objectid&f=json");
        String whereClause = null;
        if (qfilters != null) {
            String[] temp = qfilters.toString().split("=");
            if (temp != null && temp.length >= 2) {
                whereClause = temp[0].trim() + "=" + java.net.URLEncoder.encode(temp[1].trim());
            }
        }
        params.append("&where=").append(whereClause);
        return params.toString();
    }

    /**
	 * Parses directory request response
	 * @param sResponse
	 * @param params
	 * @throws JSONException
	 */
    private void parsePOIResponse(String sResponse, DirectoryParams params) throws JSONException {
        JSONObject jResponse = new JSONObject(sResponse);
        String xResponse = "<?xml version='1.0'?><response>" + org.json.XML.toString(jResponse) + "</response>";
        LOGGER.info("XML from JSON = " + xResponse);
        HashMap<String, Object> poiLocations = null;
        try {
            JSONArray fields = null;
            try {
                fields = jResponse.getJSONArray("fields");
            } catch (JSONException e) {
                throw new OwsException("Error occured while processing request.");
            }
            if (fields == null) {
                return;
            }
            JSONArray pois = jResponse.getJSONArray("features");
            double X, Y, distance;
            ArrayList<POIContext> poiContexts = new ArrayList<POIContext>();
            for (int i = 0; i < pois.length(); i++) {
                JSONObject poi = pois.getJSONObject(i);
                JSONObject attrs = poi.getJSONObject("attributes");
                POIContext poiCtx = new POIContext();
                POI poiObj = new POI();
                POIInfoList poiInfoList = new POIInfoList();
                for (int k = 0; k < fields.length(); k++) {
                    JSONObject fieldInfo = fields.getJSONObject(k);
                    String field = fieldInfo.getString("name");
                    String value = attrs.getString(field);
                    POIInfo poiInfo = new POIInfo();
                    if (field.equalsIgnoreCase("name")) {
                        poiObj.setPoiName(value);
                    } else if (field.equalsIgnoreCase("objectid")) {
                        poiObj.setId(value);
                    }
                    poiInfo.setName(field);
                    poiInfo.setValue(value);
                    poiInfoList.add(poiInfo);
                }
                poiObj.setPoiAttributeList(poiInfoList);
                JSONObject geom = poi.getJSONObject("geometry");
                Point pt = null;
                if (geom != null) {
                    X = Double.parseDouble(geom.getString("x"));
                    Y = Double.parseDouble(geom.getString("y"));
                    pt = new Point(X, Y);
                    poiObj.setLocation(pt);
                }
                poiLocations = params.getPoiLocations();
                Point location = null;
                if (poiLocations != null) {
                    Set<String> keys = poiLocations.keySet();
                    Iterator<String> iter = keys.iterator();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        if (key == "nearest") {
                            location = (Point) poiLocations.get(key);
                        }
                    }
                }
                if (location != null) {
                    distance = DistanceCalc.getApproxMeters(location, pt);
                    double dist_miles = Math.round(((Units.convert(distance, 5, 4)) * 10));
                    double distance_display = dist_miles / 10;
                    poiCtx.setDistance(distance_display);
                }
                poiCtx.setPoint(poiObj);
                poiContexts.add(poiCtx);
            }
            if (poiContexts.size() > 1) {
                Collections.sort(poiContexts);
                if (poiLocations.containsKey("nearest")) {
                    for (int k = poiContexts.size() - 1; k > 0; k--) {
                        poiContexts.remove(k);
                    }
                } else if (poiLocations.containsKey("withinDistance")) {
                    double dist = Double.parseDouble((String) poiLocations.get("withinDistance"));
                    for (int k = poiContexts.size() - 1; k > 0; k--) {
                        if (poiContexts.get(k).getDistance() > dist) {
                            poiContexts.remove(k);
                        }
                    }
                }
            }
            params.setPoiContexts(poiContexts);
        } catch (Exception e) {
            LOGGER.severe("Caught Exception" + e.getMessage());
            e.printStackTrace();
        }
    }
}
