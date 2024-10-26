package org.gdi3d.xnavi.services.w3ds;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.media.ding3d.Appearance;
import javax.media.ding3d.BoundingSphere;
import javax.media.ding3d.Bounds;
import javax.media.ding3d.BranchGroup;
import javax.media.ding3d.IndexedLineStripArray;
import javax.media.ding3d.LineArray;
import javax.media.ding3d.LineAttributes;
import javax.media.ding3d.Material;
import javax.media.ding3d.Node;
import javax.media.ding3d.PolygonAttributes;
import javax.media.ding3d.Shape3D;
import javax.media.ding3d.Transform3D;
import javax.media.ding3d.TransformGroup;
import javax.media.ding3d.TransparencyAttributes;
import javax.swing.JOptionPane;
import javax.media.ding3d.vecmath.Color3f;
import javax.media.ding3d.vecmath.Point2d;
import javax.media.ding3d.vecmath.Point3d;
import javax.media.ding3d.vecmath.Vector3d;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.gdi3d.bgutils.BranchGroupCompiler;
import org.gdi3d.bgutils.Google2Cart;
import org.gdi3d.vrmlloader.VrmlLoader;
import org.gdi3d.xnavi.coordtransform.BranchGroupCoordinateCounter;
import org.gdi3d.xnavi.coordtransform.BranchGroupCoordinateSystemTransformer;
import org.gdi3d.xnavi.coordtransform.SimpleCoordinateTransform;
import org.gdi3d.xnavi.navigator.BASE64Encoder;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.navigator.NavigatorPreferences;
import org.gdi3d.xnavi.navigator.SceneBranchGroup;
import org.gdi3d.xnavi.navigator.SceneBranchGroupManager;
import org.gdi3d.xnavi.navigator.Tile;
import org.gdi3d.xnavi.panels.memory.Memory;
import org.gdi3d.xnavi.services.transform.CoordinateTransformService;
import org.gdi3d.xnavi.viewer.AddCollisionDetector;
import javax.media.ding3d.loaders.Scene;
import javax.media.ding3d.utils.geometry.Box;
import javax.media.ding3d.utils.geometry.Cone;
import javax.media.ding3d.utils.geometry.Cylinder;
import javax.media.ding3d.utils.geometry.Sphere;
import javax.media.ding3d.utils.scenegraph.io.SceneGraphStreamReader;

public class Web3DService {

    private final String[] acceptVersions = { "0.4.1", "0.4.0" };

    public String username = null;

    public String password = null;

    public int w3ds_epsg_code = 2154;

    private static Memory memory;

    private static NumberFormat numberFormat;

    private HttpClient httpClient;

    public String encoding;

    private static final int VERSION_NOT_SPECIFIED = 0;

    private static final int VERSION_030 = 30;

    private static final int VERSION_040 = 40;

    private static final int VERSION_041 = 41;

    private int version = VERSION_NOT_SPECIFIED;

    private final String service = "W3DS";

    private Capabilities capabilities;

    public String getService() {
        return service;
    }

    private String serviceEndPoint;

    public static SceneBranchGroupManager sceneBranchGroupManager = new SceneBranchGroupManager();

    static int historyPeriod = 50;

    static long[] heapSizeHistory;

    static int[] cacheSizeHistory;

    static int historyCounter = 0;

    static long intialHeapSize;

    private static final boolean AUTO_HEIGHT = true;

    private static String mimeType = "model/vrml";

    public static final int INIT_READER = 0;

    public static final int INIT_INPUT_STREAM = 1;

    public static int intitMethod;

    private static Appearance debugAppearance;

    static {
        if (mimeType.equals("model/vrml") || mimeType.equals("model/vrml.gzip")) {
            intitMethod = INIT_READER;
        } else if (mimeType.equals("model/x3d") || mimeType.equals("model/x3d+xml")) {
            intitMethod = INIT_READER;
        } else if (mimeType.equals("model/x3d+binary")) {
            intitMethod = INIT_READER;
        } else if (mimeType.equals("model/d3d")) {
            intitMethod = INIT_INPUT_STREAM;
        }
        debugAppearance = new Appearance();
        Material mat = new Material();
        Color3f col = new Color3f(255f / 255f, 138f / 255f, 0.0f);
        mat.setDiffuseColor(col);
        mat.setAmbientColor(col);
        mat.setSpecularColor(new Color3f(0.0f, 0.0f, 0.0f));
        debugAppearance.setMaterial(mat);
        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparency(0.2f);
        ta.setTransparencyMode(TransparencyAttributes.NICEST);
        debugAppearance.setTransparencyAttributes(ta);
        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        debugAppearance.setPolygonAttributes(pa);
        Runtime rt = Runtime.getRuntime();
        memory = new Memory();
        memory.setMaxMemory(rt.maxMemory());
        heapSizeHistory = new long[historyPeriod];
        cacheSizeHistory = new int[historyPeriod];
        intialHeapSize = rt.totalMemory() - rt.freeMemory();
    }

    public Web3DService(String serviceEndPoint) {
        this.setServiceEndPoint(serviceEndPoint);
        this.w3ds_epsg_code = Navigator.epsg_code;
        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.HALF_DOWN);
        numberFormat.setMaximumFractionDigits(4);
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams cm_params = connectionManager.getParams();
        cm_params.setDefaultMaxConnectionsPerHost(20);
        cm_params.setConnectionTimeout(Navigator.TIME_OUT);
        cm_params.setSoTimeout(Navigator.TIME_OUT);
        connectionManager.setParams(cm_params);
        httpClient = new HttpClient(connectionManager);
        if (username != null && password != null) {
            setUserPassword(username + ":" + password);
        }
        loadCapabilities();
    }

    public Capabilities getCapabilities() {
        if (capabilities == null) {
            loadCapabilities();
        }
        return capabilities;
    }

    public void loadCapabilities() {
        String request0 = serviceEndPoint + "?" + "SERVICE=" + service + "&" + "request=GetCapabilities&" + "SECTIONS=OperationsMetadata,Contents,ServiceIdentification&" + "ACCEPTFORMATS=text/xml&" + "ACCEPTVERSIONS=";
        String request1 = serviceEndPoint + "?" + "do=GetCapabilities&" + "SERVICE=" + service + "&" + "request=GetCapabilities&" + "SECTIONS=OperationsMetadata,Contents,ServiceIdentification&" + "ACCEPTFORMATS=text/xml&" + "ACCEPTVERSIONS=";
        String requests[] = new String[2];
        requests[0] = request0;
        requests[1] = request1;
        try {
            for (String request : requests) {
                for (int i = 0; i < acceptVersions.length; i++) {
                    if (i > 0) request += ",";
                    request += acceptVersions[i];
                }
                if (Navigator.isVerbose()) {
                    System.out.println("GetCapabilities Request: " + request);
                }
                URL url = new URL(request);
                version = VERSION_NOT_SPECIFIED;
                for (String tryVersion : this.acceptVersions) {
                    if (tryVersion.equals("0.4.1")) {
                        InputStream urlIn_041 = null;
                        try {
                            URLConnection urlc = url.openConnection();
                            urlc.setReadTimeout(Navigator.TIME_OUT);
                            if (getEncoding() != null) {
                                urlc.setRequestProperty("Authorization", "Basic " + getEncoding());
                            }
                            urlIn_041 = urlc.getInputStream();
                            if (urlIn_041 != null) {
                                org.gdi3d.xnavi.services.w3ds.x041.CapabilitiesLoader capLoader = new org.gdi3d.xnavi.services.w3ds.x041.CapabilitiesLoader(this);
                                capabilities = capLoader.load(urlIn_041);
                                version = VERSION_041;
                                if (Navigator.isVerbose()) {
                                    System.out.println("W3DS version is 0.4.1");
                                }
                            }
                        } catch (org.gdi3d.xnavi.services.w3ds.x041.WrongCapabilitiesVersionException wv) {
                            if (Navigator.isVerbose()) {
                                System.out.println("W3DS version is not 0.4.1");
                            }
                        } finally {
                            try {
                                urlIn_041.close();
                            } catch (Exception e) {
                            }
                        }
                    } else if (tryVersion.equals("0.4.0")) {
                        InputStream urlIn_040 = null;
                        try {
                            URLConnection urlc = url.openConnection();
                            urlc.setReadTimeout(Navigator.TIME_OUT);
                            if (getEncoding() != null) {
                                urlc.setRequestProperty("Authorization", "Basic " + getEncoding());
                            }
                            urlIn_040 = urlc.getInputStream();
                            if (urlIn_040 != null) {
                                org.gdi3d.xnavi.services.w3ds.x040.CapabilitiesLoader capLoader = new org.gdi3d.xnavi.services.w3ds.x040.CapabilitiesLoader(this);
                                capabilities = capLoader.load(urlIn_040);
                                version = VERSION_040;
                                if (Navigator.isVerbose()) {
                                    System.out.println("W3DS version is 0.4.0");
                                }
                            }
                        } catch (org.gdi3d.xnavi.services.w3ds.x040.WrongCapabilitiesVersionException wv) {
                            if (Navigator.isVerbose()) {
                                System.out.println("W3DS version is not 0.4.0");
                            }
                        } finally {
                            try {
                                urlIn_040.close();
                            } catch (Exception e) {
                            }
                        }
                    } else if (tryVersion.equals("0.3.0")) {
                        InputStream urlIn_030 = null;
                        try {
                            URLConnection urlc = url.openConnection();
                            urlc.setReadTimeout(Navigator.TIME_OUT);
                            if (getEncoding() != null) {
                                urlc.setRequestProperty("Authorization", "Basic " + getEncoding());
                            }
                            urlIn_030 = urlc.getInputStream();
                            if (urlIn_030 != null) {
                                org.gdi3d.xnavi.services.w3ds.x030.CapabilitiesLoader capLoader = new org.gdi3d.xnavi.services.w3ds.x030.CapabilitiesLoader(urlIn_030);
                                capabilities = capLoader.getCapabilities();
                                version = VERSION_030;
                                if (Navigator.isVerbose()) {
                                    System.out.println("W3DS version is 0.3.0");
                                }
                            }
                        } catch (org.gdi3d.xnavi.services.w3ds.x030.WrongCapabilitiesVersionException wv) {
                            if (Navigator.isVerbose()) {
                                System.out.println("W3DS version is not 0.3.0");
                            }
                        } finally {
                            try {
                                urlIn_030.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (version != VERSION_NOT_SPECIFIED) {
                        break;
                    }
                }
                if (version != VERSION_NOT_SPECIFIED) {
                    break;
                }
            }
            if (version == VERSION_NOT_SPECIFIED) {
                capabilities = null;
                JOptionPane.showMessageDialog(null, "Problem connecting to W3DS. \n" + "Must be version 0.3.0 or 0.4.0. \n " + "Check console for details.", "W3DS Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NoRouteToHostException e) {
            JOptionPane.showMessageDialog(null, "Error occurred while attempting to connect a socket \n" + "to a remote address and port. Typically, the remote host \n" + "cannot be reached because of an intervening firewall, \n" + "or if an intermediate router is down. ", "Connecting Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (java.lang.NullPointerException e) {
            JOptionPane.showMessageDialog(null, "No connection with the server.\n " + "(NullPointer) ???", "Connecting Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (IOException e) {
            capabilities = null;
            JOptionPane.showMessageDialog(null, "The connection with the server could not be produced.\n" + " Please, check your Internet connection or Internet settings. ", "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public String createGET_Request(Tile tile) {
        String get_request = null;
        String queryString = createGET_QueryString(tile);
        get_request = serviceEndPoint + "?" + queryString;
        return get_request;
    }

    public String createGET_QueryString(Tile tile) {
        StringBuffer queryString = new StringBuffer();
        ;
        Point3d CRSCenter = tile.getCRSCenter();
        double size = tile.getCRSSize();
        W3DS_Layer layer = tile.getLayer();
        double lx = CRSCenter.x - size / 2;
        double ux = CRSCenter.x + size / 2;
        double ly = CRSCenter.y - size / 2;
        double uy = CRSCenter.y + size / 2;
        String styleIdentifier = null;
        Style style = layer.getCurrentStyle();
        if (style != null) styleIdentifier = style.getIdentifier();
        String versionString = null;
        if (version == VERSION_040) {
            versionString = "0.4.0";
        } else if (version == VERSION_041) {
            versionString = "0.4.1";
        } else if (version == VERSION_030) {
            versionString = "0.3.0";
        }
        if (layer.isTiled()) {
            String crs = "EPSG:" + Navigator.getEpsg_code();
            int level = tile.getW3DS_TileLevel();
            int col = tile.getW3DS_TileX();
            int row = tile.getW3DS_TileY();
            queryString.append("SERVICE=" + service + "&");
            queryString.append("REQUEST=GetTile&");
            queryString.append("VERSION=" + versionString + "&");
            queryString.append("CRS=" + crs + "&");
            queryString.append("FORMAT=" + this.mimeType + "&");
            queryString.append("LAYER=" + layer.getIdentifier() + "&");
            if (version == VERSION_040) {
                queryString.append("TILELEVEL=" + level + "&");
                queryString.append("TILEROW=" + row + "&");
                queryString.append("TILECOL=" + col);
            } else if (version == VERSION_041) {
                queryString.append("TILELEVEL=" + level + "&");
                queryString.append("Y=" + row + "&");
                queryString.append("X=" + col);
            }
            if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                queryString.append("&STYLE=" + styleIdentifier);
            }
        } else {
            String lxs = numberFormat.format(lx).replace(",", ".");
            String lys = numberFormat.format(ly).replace(",", ".");
            String uxs = numberFormat.format(ux).replace(",", ".");
            String uys = numberFormat.format(uy).replace(",", ".");
            Point3d lower = new Point3d(new Double(lxs), new Double(lys), 0);
            Point3d upper = new Point3d(new Double(uxs), new Double(uys), 0);
            if (Navigator.epsg_code != this.w3ds_epsg_code) {
                lower = SimpleCoordinateTransform.coordinateTransformServiceTransform(lower, "epsg:" + Navigator.epsg_code, "epsg:" + this.w3ds_epsg_code);
                upper = SimpleCoordinateTransform.coordinateTransformServiceTransform(upper, "epsg:" + Navigator.epsg_code, "epsg:" + this.w3ds_epsg_code);
                double tmp = lower.x;
                lower.x = lower.y;
                lower.y = tmp;
                tmp = upper.x;
                upper.x = upper.y;
                upper.y = tmp;
            }
            String crs = "EPSG:" + this.w3ds_epsg_code;
            if (serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8080")) {
                queryString.append("do=GetScene&");
                crs = "EPSG:3857";
            }
            if (serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8081")) {
                queryString.append("do=GetScene&");
            }
            queryString.append("SERVICE=" + service + "&");
            queryString.append("REQUEST=GetScene&");
            queryString.append("VERSION=" + versionString + "&");
            queryString.append("CRS=" + crs + "&");
            queryString.append("FORMAT=model/vrml&");
            queryString.append("BoundingBox=" + lower.x + "," + lower.y + "," + upper.x + "," + upper.y);
            if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                queryString.append("&layers=" + layer.getIdentifier() + "&styles=" + styleIdentifier);
                if (style.getType() == Style.TYPE_USERSTYLE) {
                    try {
                        queryString.append("&SLD_BODY=" + URLEncoder.encode(style.getSldDoc(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8080") || serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8081")) {
                    queryString.append("&layers=" + layer.getTitle());
                } else {
                    queryString.append("&layers=" + layer.getIdentifier());
                }
            }
        }
        return queryString.toString();
    }

    public String createXML_Request(Tile tile) {
        Point3d CRSCenter = tile.getCRSCenter();
        double size = tile.getCRSSize();
        W3DS_Layer layer = tile.getLayer();
        double lx = CRSCenter.x - size / 2;
        double ux = CRSCenter.x + size / 2;
        double ly = CRSCenter.y - size / 2;
        double uy = CRSCenter.y + size / 2;
        String styleIdentifier = null;
        Style style = layer.getCurrentStyle();
        if (style != null) {
            styleIdentifier = style.getIdentifier();
        }
        StringBuffer postRequest = new StringBuffer();
        if (layer.isTiled()) {
            int level = tile.getW3DS_TileLevel();
            int x = tile.getW3DS_TileX();
            int y = tile.getW3DS_TileY();
            if (version == VERSION_040) {
                postRequest.append("<?xml version='1.0' encoding='UTF-8'?>\n <GetTile ");
                postRequest.append(" xmlns='http://www.opengis.net/w3ds/0.4.0' xmlns:ows='http://www.opengis.net/ows/1.1' service='W3DS' request='GetTile' version='0.4.0'");
                postRequest.append(">\n");
                postRequest.append("<CRS>EPSG:" + Navigator.getEpsg_code() + "</CRS>");
                postRequest.append("<Layer>" + layer.getIdentifier() + "</Layer>");
                if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                    postRequest.append("<Style>" + styleIdentifier + "</Style>");
                }
                postRequest.append("<Format>" + mimeType + "</Format>");
                postRequest.append("<TileLevel>" + level + "</TileLevel>");
                postRequest.append("<TileRow>" + y + "</TileRow>");
                postRequest.append("<TileCol>" + x + "</TileCol>");
                postRequest.append("\n</GetTile>");
            } else if (version == VERSION_041) {
                postRequest.append("<?xml version='1.0' encoding='UTF-8'?>\n <GetTile ");
                postRequest.append(" xmlns='http://www.opengis.net/w3ds/0.4.1' xmlns:ows='http://www.opengis.net/ows/2.0' service='W3DS' request='GetTile'");
                postRequest.append(">\n");
                postRequest.append("<CRS>EPSG:" + Navigator.getEpsg_code() + "</CRS>");
                postRequest.append("<Layer>" + layer.getIdentifier() + "</Layer>");
                if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                    postRequest.append("<Style>" + styleIdentifier + "</Style>");
                }
                postRequest.append("<Format>" + mimeType + "</Format>");
                postRequest.append("<Level>" + level + "</Level>");
                postRequest.append("<X>" + x + "</X>");
                postRequest.append("<Y>" + y + "</Y>");
                postRequest.append("\n</GetTile>");
            }
        } else {
            String lxs = numberFormat.format(lx).replace(",", ".");
            String lys = numberFormat.format(ly).replace(",", ".");
            String uxs = numberFormat.format(ux).replace(",", ".");
            String uys = numberFormat.format(uy).replace(",", ".");
            String styledLayerDescriptor = "";
            if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                if (style.getType() == Style.TYPE_USERSTYLE) {
                    styledLayerDescriptor = style.getSldDoc();
                    try {
                        styledLayerDescriptor = URLEncoder.encode(styledLayerDescriptor, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (version == VERSION_040) {
                postRequest.append("<?xml version='1.0' encoding='UTF-8'?>\n <GetScene ");
                postRequest.append(" xmlns='http://www.opengis.net/w3ds/0.4.0' xmlns:ows='http://www.opengis.net/ows/1.1' service='W3DS' request='GetScene' version='0.4.0'");
                postRequest.append(">\n");
                if (style.getType() == Style.TYPE_USERSTYLE) {
                    postRequest.append(styledLayerDescriptor);
                }
                postRequest.append("<CRS>EPSG:" + Navigator.getEpsg_code() + "</CRS>");
                postRequest.append("<ows:BoundingBox>" + "<ows:LowerCorner>" + lxs + " " + lys + "</ows:LowerCorner>" + "<ows:UpperCorner>" + uxs + " " + uys + "</ows:UpperCorner>" + "</ows:BoundingBox>");
                postRequest.append("<SpatialSelection>contains_center</SpatialSelection>");
                postRequest.append("<Format>" + mimeType + "</Format>");
                if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                    postRequest.append("<Layers>" + "<ows:Identifier>" + layer.getIdentifier() + "</ows:Identifier>" + "</Layers>");
                    postRequest.append("<Styles>" + "<ows:Identifier>" + styleIdentifier + "</ows:Identifier>" + " </Styles>");
                } else {
                    postRequest.append("<Layers>" + "<ows:Identifier>" + layer.getIdentifier() + "</ows:Identifier>" + "</Layers>");
                }
                postRequest.append("\n</GetScene>");
            } else if (version == VERSION_041) {
                postRequest.append("<?xml version='1.0' encoding='UTF-8'?>\n <GetScene ");
                postRequest.append(" xmlns='http://www.opengis.net/w3ds/0.4.0' xmlns:ows='http://www.opengis.net/ows/1.1' service='W3DS' request='GetScene' version='0.4.0'");
                postRequest.append(">\n");
                if (style.getType() == Style.TYPE_USERSTYLE) {
                    postRequest.append(styledLayerDescriptor);
                }
                postRequest.append("<CRS>EPSG:" + Navigator.getEpsg_code() + "</CRS>");
                postRequest.append("<ows:BoundingBox>" + "<ows:LowerCorner>" + lxs + " " + lys + "</ows:LowerCorner>" + "<ows:UpperCorner>" + uxs + " " + uys + "</ows:UpperCorner>" + "</ows:BoundingBox>");
                postRequest.append("<SpatialSelection>contains_center</SpatialSelection>");
                postRequest.append("<Format>" + mimeType + "</Format>");
                if (styleIdentifier != null && !styleIdentifier.equalsIgnoreCase("default")) {
                    postRequest.append("<Layers>" + "<ows:Identifier>" + layer.getIdentifier() + "</ows:Identifier>" + "</Layers>");
                    postRequest.append("<Styles>" + "<ows:Identifier>" + styleIdentifier + "</ows:Identifier>" + " </Styles>");
                } else {
                    postRequest.append("<Layers>" + "<ows:Identifier>" + layer.getIdentifier() + "</ows:Identifier>" + "</Layers>");
                }
                postRequest.append("\n</GetScene>");
            }
        }
        return postRequest.toString();
    }

    public InputStream initInputStream(Tile tile) {
        InputStream inputStream = null;
        String xml_request = tile.getXML_Request();
        if (xml_request == null) {
            tile.initXML_Request();
            xml_request = tile.getXML_Request();
        }
        if (Navigator.isVerbose()) {
            String get_request = createGET_Request(tile);
            System.out.println("sending get_request\n" + get_request + "\n");
        }
        try {
            InputStream response_inputstream = null;
            PostMethod method = new PostMethod(serviceEndPoint);
            method.setRequestHeader("Authorization", "Basic " + getEncoding());
            method.setRequestHeader("Content-Type", "application/xml");
            method.setRequestHeader("Accept-Encoding", "gzip,deflate");
            method.setRequestHeader("Cache-Control", "no-cache");
            method.setRequestHeader("Pragma", "no-cache");
            method.setRequestHeader("User-Agent", "XNavigator " + Navigator.version);
            method.setRequestHeader("Connection", "keep-alive");
            method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
            method.setRequestBody(xml_request);
            httpClient.executeMethod(method);
            response_inputstream = method.getResponseBodyAsStream();
            HttpMethodParams p = method.getParams();
            String content_type = "not specified";
            Header header_content_type = method.getResponseHeader("Content-Type");
            content_type = header_content_type.getValue();
            String content_encoding = "not specified";
            Header header_content_encoding = method.getResponseHeader("Content-Encoding");
            content_encoding = header_content_encoding.getValue();
            if (content_type.equalsIgnoreCase("model/d3d")) {
                if (content_encoding.equals("gzip")) {
                    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(response_inputstream));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    bufis.close();
                    bos.close();
                    byte[] ba = bos.toByteArray();
                    inputStream = new ByteArrayInputStream(ba);
                } else {
                    BufferedInputStream bufis = new BufferedInputStream(response_inputstream);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    byte[] ba = bos.toByteArray();
                    inputStream = new ByteArrayInputStream(ba);
                }
            } else if (content_type.equalsIgnoreCase("text/xml")) {
                byte[] ba = null;
                if (false) {
                    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(response_inputstream));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    bufis.close();
                    bos.close();
                    ba = bos.toByteArray();
                } else {
                    BufferedInputStream bufis = new BufferedInputStream(response_inputstream);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    ba = bos.toByteArray();
                }
                String text = new String(ba);
                System.out.println(text);
            }
            response_inputstream.close();
            method.releaseConnection();
            response_inputstream.close();
            method.releaseConnection();
        } catch (MalformedURLException mURLe) {
            mURLe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return inputStream;
    }

    public Reader initReader(Tile tile) {
        Reader reader = null;
        Operation getSceneOperation = capabilities.getGetSceneOperation();
        boolean supportsPostXML = getSceneOperation.isSupportsPostXML();
        boolean supportsGetKVP = getSceneOperation.isSupportsGetKVP();
        if (Navigator.isVerbose()) {
            String get_request = createGET_Request(tile);
            System.out.println("sending get_request\n" + get_request + "\n");
        }
        org.apache.commons.httpclient.HttpMethodBase method = null;
        try {
            if (supportsPostXML) {
                String xml_request = tile.getXML_Request();
                if (xml_request == null) {
                    tile.initXML_Request();
                    xml_request = tile.getXML_Request();
                }
                PostMethod postMethod = new PostMethod(serviceEndPoint);
                postMethod.setRequestHeader("Content-Type", "application/xml");
                postMethod.setRequestBody(xml_request);
                method = postMethod;
                if (Navigator.verbose) System.out.println("Execute POST-Method");
            } else if (supportsGetKVP) {
                String queryString = createGET_QueryString(tile);
                GetMethod getMethod = new GetMethod(serviceEndPoint);
                getMethod.setQueryString(queryString);
                method = getMethod;
                if (Navigator.verbose) System.out.println("Execute GET-Method");
            }
            if (encoding != null) {
                method.setRequestHeader("Authorization", "Basic " + encoding);
            }
            method.setRequestHeader("Accept-Encoding", "gzip,deflate");
            method.setRequestHeader("Cache-Control", "no-cache");
            method.setRequestHeader("Pragma", "no-cache");
            method.setRequestHeader("User-Agent", "XNavigator " + Navigator.version);
            method.setRequestHeader("Connection", "keep-alive");
            method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
            httpClient.executeMethod(method);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            InputStream response_inputstream = null;
            response_inputstream = method.getResponseBodyAsStream();
            HttpMethodParams p = method.getParams();
            String content_type = "not specified";
            Header header_content_type = method.getResponseHeader("Content-Type");
            if (header_content_type != null) {
                content_type = header_content_type.getValue();
            }
            String content_encoding = "not specified";
            Header header_content_encoding = method.getResponseHeader("Content-Encoding");
            if (header_content_encoding != null) {
                content_encoding = header_content_encoding.getValue();
            }
            if (content_type == null || content_type.equalsIgnoreCase("x-world/x-vrml") || content_type.equalsIgnoreCase("model/vrml") || content_type.equalsIgnoreCase("model/vrml;charset=ISO-8859-1")) {
                if (content_encoding.equals("gzip")) {
                    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(response_inputstream));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    reader = new BufferedReader(new StringReader(retval));
                } else {
                    ByteBuf bb = new ByteBuf();
                    int r;
                    long time0 = System.currentTimeMillis();
                    while ((r = response_inputstream.read()) != -1) {
                        bb.add((byte) r);
                    }
                    long time1 = System.currentTimeMillis();
                    long perf = bb.array.length / (time1 - time0) * 1000 / 1024;
                    System.out.println("download model/vrml with " + perf + " kB/s");
                    bb.trim();
                    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array);
                    reader = new BufferedReader(new InputStreamReader(bais));
                }
            } else if (content_type.equalsIgnoreCase("model/vrml.gzip")) {
                if (true) {
                    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(response_inputstream));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    reader = new BufferedReader(new StringReader(retval));
                }
            } else if (content_type.equalsIgnoreCase("model/d3d")) {
                if (content_encoding.equals("gzip")) {
                    BufferedInputStream bufis = new BufferedInputStream(new GZIPInputStream(response_inputstream));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    reader = new BufferedReader(new StringReader(retval));
                } else {
                    BufferedInputStream bufis = new BufferedInputStream(response_inputstream);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = bufis.read(buf)) > 0) {
                        bos.write(buf, 0, len);
                    }
                    String retval = bos.toString();
                    bufis.close();
                    bos.close();
                    reader = new BufferedReader(new StringReader(retval));
                }
            } else if (content_type.equalsIgnoreCase("model/vrml.encrypted")) {
            } else if (content_type.equalsIgnoreCase("model/vrml.gzip.encrypted")) {
            } else if (content_type.equalsIgnoreCase("text/plain")) {
            }
            response_inputstream.close();
            method.releaseConnection();
        } catch (MalformedURLException mURLe) {
            mURLe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return reader;
    }

    public SceneBranchGroup loadData(Tile tile) throws InterruptedException {
        String xml_request = tile.getXML_Request();
        if (xml_request == null) {
            tile.initXML_Request();
            xml_request = tile.getXML_Request();
        }
        SceneBranchGroup data = Web3DService.sceneBranchGroupManager.findBranchGroup(xml_request);
        if (data != null) {
            if (Navigator.isVerbose()) System.out.println("Web3DService.loadData: found branchgroup in cache");
            if (data.getParent() != null) {
                System.out.println("W3DS 1: data with parent");
                System.out.println("data.isLive() " + data.isLive());
                System.out.println("tile.childrenStatus " + tile.childrenStatus);
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                for (StackTraceElement s : st) {
                    System.out.println(s);
                }
            }
        } else {
            if (Navigator.isVerbose()) System.out.println("Web3DService.loadData: loading data from server");
            boolean interrupted = true;
            try {
                data = this.doLoadData(tile);
                interrupted = false;
            } catch (InterruptedException e) {
                System.out.println("Web3DService.loadData interrupted");
            }
            if (!interrupted) {
                Web3DService.sceneBranchGroupManager.storeBranchGroup(xml_request, data);
            }
            if (data.getParent() != null) {
                System.out.println("W3DS 2: data with parent");
            }
        }
        if (data != null) {
            tile.setCRSCenter(data.getCRSCenter());
            tile.setJava3DCenter(data.getJava3DCenter());
        }
        return data;
    }

    private BranchGroup loadFromInputStream(Tile tile, W3DS_Layer layer) {
        BranchGroup bg = null;
        InputStream inputStream = tile.getInputStream();
        if (inputStream == null) {
            inputStream = initInputStream(tile);
        }
        try {
            SceneGraphStreamReader sceneGraphStreamReader = new SceneGraphStreamReader(inputStream);
            bg = sceneGraphStreamReader.readBranchGraph(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tile.setInputStream(null);
        return bg;
    }

    private BranchGroup loadFromReader(Tile tile, W3DS_Layer layer) {
        if (Navigator.verbose) System.out.println("Load from reader");
        BranchGroup bg = null;
        String xml_request = tile.getXML_Request();
        Reader reader = tile.getReader();
        if (reader == null) {
            reader = initReader(tile);
            if (reader == null) {
                System.out.println("Error: Web3DService.doLoadData could not init reader for " + xml_request);
            }
        }
        if (mimeType.equals("model/vrml") || mimeType.equals("model/vrml.gzip")) {
            int flags = 0;
            if (!tile.getLayer().isCompileContent()) {
                flags = VrmlLoader.LOAD_ANIMATIONS;
            }
            VrmlLoader loader = new VrmlLoader(flags);
            NavigatorPreferences prefs = Navigator.getPreferences();
            loader.setGenerateMipMaps(prefs.isGenerateMipMaps());
            loader.setAnisotropicFiltering(prefs.isAnisotropicFiltering());
            loader.setRemoveViewpoints(true);
            loader.setRemoveLights(true);
            loader.setReduceTextures(false);
            if (layer.isCompileContent()) {
                loader.setAutoClearRegisters(true);
            } else loader.setAutoClearRegisters(false);
            loader.setCurrentRegisterKey(layer);
            loader.setVerbose(Navigator.isVerbose());
            loader.setDebug(false);
            if (false) {
                loader.setSaveReducedTextures(true);
                loader.setSaveReducedTexturesPath("C:/Test/maps");
                loader.setSaveOrignialTexturesPath("C:/Test/org_maps");
            }
            int renderMode = prefs.getRenderMode();
            if (renderMode == NavigatorPreferences.RENDERMODE_FLAT) loader.setRenderMode(VrmlLoader.RENDERMODE_FLAT); else if (renderMode == NavigatorPreferences.RENDERMODE_GOURAUD) loader.setRenderMode(VrmlLoader.RENDERMODE_GOURAUD); else if (renderMode == NavigatorPreferences.RENDERMODE_WIREFRAME) loader.setRenderMode(VrmlLoader.RENDERMODE_WIREFRAME); else if (renderMode == NavigatorPreferences.RENDERMODE_PHONG) loader.setRenderMode(VrmlLoader.RENDERMODE_PHONG); else if (renderMode == NavigatorPreferences.RENDERMODE_ATMOSPHERIC) loader.setRenderMode(VrmlLoader.RENDERMODE_ATMOSPHERIC);
            Point3d loader_offset = null;
            if (version == VERSION_041) {
                if (!layer.isTiled()) {
                    loader_offset = new Point3d(tile.getCRSCenter().x, tile.getCRSCenter().z, -tile.getCRSCenter().y);
                    loader.setOffset(loader_offset);
                }
            } else {
                loader_offset = new Point3d(tile.getCRSCenter().x, 0.0, -tile.getCRSCenter().y);
                loader.setOffset(loader_offset);
            }
            loader.setIndexGeometries(false);
            loader.setStripGeometries(false);
            if (getEncoding() != null) {
                loader.setEncoding(getEncoding());
            }
            if (reader != null) {
                try {
                    URL url = new URL(serviceEndPoint);
                    loader.setBaseUrl(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                Scene scene = null;
                try {
                    if (Navigator.verbose) System.out.println("1");
                    if (serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8080")) {
                        loader.setLoadTextures(true);
                        loader.setZUp(true);
                    }
                    if (serviceEndPoint.contains("pc1869.igd.fraunhofer.de:8081")) {
                        loader.setLoadTextures(true);
                        loader.setZUp(true);
                        loader.setMaxTextureResolution(10.0);
                    }
                    scene = loader.load(reader);
                    if (scene != null) {
                        if (Navigator.verbose) System.out.println("2");
                        bg = scene.getSceneGroup();
                        if (Navigator.getEpsg_code() != this.w3ds_epsg_code) {
                            BranchGroupCoordinateSystemTransformer branchGroupCoordinateSystemTransformer = new BranchGroupCoordinateSystemTransformer("epsg:" + this.w3ds_epsg_code, "epsg:" + Navigator.epsg_code);
                            branchGroupCoordinateSystemTransformer.setOffset(loader_offset);
                            branchGroupCoordinateSystemTransformer.transform(bg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error er) {
                    er.printStackTrace();
                }
            } else {
                System.out.println("Error: Web3DService.doLoadData reader == null");
            }
            if (layer.isCompileContent()) {
                loader.clearRegistersForCurrentKey();
            }
        } else if (mimeType.equals("model/x3d") || mimeType.equals("model/x3d+xml")) {
            System.out.println("loadFromReader x3d+xml");
        } else if (mimeType.equals("model/x3d+binary")) {
            System.out.println("loadFromReader x3d+binary");
        }
        tile.setReader(null);
        return bg;
    }

    private SceneBranchGroup doLoadData(Tile tile) throws InterruptedException {
        SceneBranchGroup data = null;
        try {
            W3DS_Layer layer = tile.getLayer();
            Point3d tile_j3dcenter = tile.getJava3DCenter();
            Point3d tile_crscenter = tile.getCRSCenter();
            String xml_request = tile.getXML_Request();
            if (xml_request == null) {
                tile.initXML_Request();
                xml_request = tile.getXML_Request();
            }
            BranchGroup bg = null;
            if (Web3DService.intitMethod == Web3DService.INIT_READER) {
                bg = loadFromReader(tile, layer);
            } else if (Web3DService.intitMethod == Web3DService.INIT_INPUT_STREAM) {
                bg = loadFromInputStream(tile, layer);
            }
            tile.setXML_Request(null);
            if (bg != null) {
                data = new SceneBranchGroup();
                TransformGroup tg = new TransformGroup();
                Transform3D t3d = new Transform3D();
                t3d.set(new Vector3d(tile.getCRSCenter().x, 0, -tile.getCRSCenter().y));
                tg.setTransform(t3d);
                if (false) {
                    Enumeration allChildren = bg.getAllChildren();
                    while (allChildren.hasMoreElements()) {
                        Node child = (Node) allChildren.nextElement();
                        bg.removeChild(child);
                        tg.addChild(child);
                    }
                    int numChildren = bg.numChildren();
                    data.setNumChildren(numChildren);
                } else {
                    tg.addChild(bg);
                    int numChildren = bg.numChildren();
                    data.setNumChildren(numChildren);
                    if (false) {
                        BoundingSphere bounds = (BoundingSphere) data.getBounds();
                        double bounds_radius = tile.getCRSSize();
                        Sphere sphere = new Sphere((float) (bounds_radius / 80.0), Sphere.GENERATE_NORMALS, 10, debugAppearance);
                        tg.addChild(sphere);
                        float cylinder_height = (float) bounds_radius / 8;
                        Cylinder cylinder = new Cylinder((float) (bounds_radius / 140.0), cylinder_height, Cylinder.GENERATE_NORMALS, debugAppearance);
                        TransformGroup cylinder_tg = new TransformGroup();
                        Transform3D cylinder_t3d = new Transform3D();
                        cylinder_t3d.set(new Vector3d(0, cylinder_height / 2, 0));
                        cylinder_tg.setTransform(cylinder_t3d);
                        cylinder_tg.addChild(cylinder);
                        tg.addChild(cylinder_tg);
                        float cone_radius = (float) (bounds_radius / 40.0);
                        float cone_height = (float) (bounds_radius / 40.0);
                        Cone cone = new Cone(cone_radius, cone_height, Cone.GENERATE_NORMALS, 10, 10, debugAppearance);
                        TransformGroup cone_tg = new TransformGroup();
                        Transform3D cone_t3d = new Transform3D();
                        cone_t3d.set(new Vector3d(0, cylinder_height + cone_height / 2, 0));
                        cone_tg.setTransform(cone_t3d);
                        cone_tg.addChild(cone);
                        tg.addChild(cone_tg);
                    }
                }
                data.addChild(tg);
                if (Navigator.globe && (Navigator.epsg_code == 900913 || Navigator.epsg_code == 3857)) {
                    Google2Cart g2c = new Google2Cart();
                    g2c.transformGroup(data);
                }
                data.setName("sceneBranchGroup");
                data.setCapability(BranchGroup.ALLOW_DETACH);
                if (AUTO_HEIGHT) {
                    if (Navigator.globe) {
                        data.setBoundsAutoCompute(true);
                        Point3d j3dcenter = new Point3d();
                        BoundingSphere bounds = (BoundingSphere) data.getBounds();
                        bounds.getCenter(j3dcenter);
                        Vector3d vec = new Vector3d(j3dcenter);
                        double center_height = vec.length() - CoordinateTransformService.erdRadius;
                        if (center_height < 0) center_height = 0;
                        Vector3d heightAdd = new Vector3d(tile.getNormal());
                        heightAdd.scale(center_height);
                        tile_crscenter.z = center_height;
                        tile_j3dcenter.add(heightAdd);
                        data.setBoundsAutoCompute(false);
                        data.setBounds(bounds);
                    } else {
                        data.setBoundsAutoCompute(true);
                        Point3d j3dcenter = new Point3d();
                        BoundingSphere bounds = (BoundingSphere) data.getBounds();
                        bounds.getCenter(j3dcenter);
                        double center_height = j3dcenter.y;
                        tile_crscenter.z = center_height;
                        tile_j3dcenter.y = center_height;
                        data.setBoundsAutoCompute(false);
                        data.setBounds(bounds);
                    }
                }
                data.setXml_request(xml_request);
                data.setJava3DCenter(tile_j3dcenter);
                data.setCRSCenter(tile_crscenter);
                data.setLayer(layer);
                data.setCRSSize(tile.getCRSSize());
                data.setJ3DSize(tile.getJ3DSize());
                data.setNormal(tile.getNormal());
                data.setUserData(data);
                if (false) {
                    Bounds collisionBounds = Navigator.viewer.getView().getViewPlatform().getBounds();
                    if (collisionBounds != null) {
                        AddCollisionDetector addCollisionDetector = new AddCollisionDetector(collisionBounds);
                        addCollisionDetector.checkGroup(data);
                    } else System.out.println("collisionBounds == null");
                }
                if (layer.isCompileContent()) {
                    boolean enablePicking = true;
                    FeatureTypeProperties featureTypeProperties = layer.getFeatureTypeProperties();
                    if (featureTypeProperties != null) {
                        boolean isTangible = featureTypeProperties.isTangible();
                        if (isTangible) {
                            enablePicking = true;
                        } else {
                            enablePicking = false;
                        }
                    }
                    BranchGroupCompiler compiler = new BranchGroupCompiler();
                    compiler.compile(data, enablePicking);
                }
            } else {
                System.out.println("Error: Web3DService.doLoadData scene == null");
            }
            if (data == null) {
                System.out.println("Error loading branchgroup from W3DS");
                data = new SceneBranchGroup();
                data.setName("sceneBranchGroup");
                data.setCapability(BranchGroup.ALLOW_DETACH);
                data.setXml_request(xml_request);
                data.setBounds(new BoundingSphere(new Point3d(0, 0, 0), Float.POSITIVE_INFINITY));
                data.setJava3DCenter(tile_j3dcenter);
                data.setCRSCenter(tile.getCRSCenter());
                data.setLayer(layer);
                data.setCRSSize(tile.getCRSSize());
                data.setJ3DSize(tile.getJ3DSize());
                data.setNormal(tile.getNormal());
                data.setUserData(data);
                if (Navigator.globe && (Navigator.epsg_code == 900913 || Navigator.epsg_code == 3857)) {
                    Google2Cart g2c = new Google2Cart();
                    g2c.transformGroup(data);
                }
            }
        } catch (java.lang.OutOfMemoryError er) {
            er.printStackTrace();
            Web3DService.sceneBranchGroupManager.clearAllBranchGroups();
            Object[] objects = { Navigator.i18n.getString("OUT_OF_MEMORY") };
            JOptionPane.showMessageDialog(null, objects, Navigator.i18n.getString("WARNING"), JOptionPane.WARNING_MESSAGE);
            Thread.currentThread().sleep(10000);
        }
        return data;
    }

    private BranchGroup makeNormal(Tile tile) {
        Transform3D latlon_t3d = CoordinateTransformService.getLatLonTransform(tile.getCRSCenter());
        Vector3d normal = new Vector3d(0, 1, 0);
        latlon_t3d.transform(normal);
        normal.normalize();
        normal.scale(tile.getJ3DSize() / 4.0);
        Vector3d translation = new Vector3d();
        latlon_t3d.get(translation);
        Point3d basePoint = new Point3d(translation);
        Point3d topPoint = new Point3d(basePoint);
        topPoint.add(normal);
        Point3d[] coordinates = new Point3d[2];
        coordinates[0] = basePoint;
        coordinates[1] = topPoint;
        Appearance app = new Appearance();
        Material mat = new Material();
        mat.setAmbientColor(new Color3f(0f, 0f, 0f));
        mat.setDiffuseColor(new Color3f(0f, 0f, 0f));
        mat.setEmissiveColor(new Color3f(0f, 0f, 0f));
        mat.setSpecularColor(new Color3f(0.0f, 0.0f, 0.0f));
        mat.setShininess(0.0f);
        app.setMaterial(mat);
        LineAttributes lineAttributes = new LineAttributes();
        lineAttributes.setLineWidth(1f);
        lineAttributes.setLineAntialiasingEnable(true);
        app.setLineAttributes(lineAttributes);
        int vertexCount = coordinates.length;
        int indexCount = vertexCount;
        int[] coordinateIndices = new int[indexCount];
        for (int i = 0; i < indexCount; i++) {
            coordinateIndices[i] = i;
        }
        Color3f[] colors = new Color3f[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            colors[i] = new Color3f(0f, 0f, 0f);
        }
        int[] stripIndexCounts = new int[1];
        stripIndexCounts[0] = coordinateIndices.length;
        IndexedLineStripArray ilsa = new IndexedLineStripArray(vertexCount, LineArray.COORDINATES | LineArray.COLOR_3, indexCount, stripIndexCounts);
        ilsa.setCoordinates(0, coordinates);
        ilsa.setCoordinateIndices(0, coordinateIndices);
        ilsa.setColors(0, colors);
        Shape3D shape = new Shape3D(ilsa, app);
        BranchGroup bg = new BranchGroup();
        bg.setBounds(new BoundingSphere(new Point3d(0, 0, 0), Float.POSITIVE_INFINITY));
        bg.addChild(shape);
        return bg;
    }

    public static TransformGroup makeBox(Tile tile, double height, Color3f col) {
        double size = tile.getCRSSize();
        TransformGroup tg = new TransformGroup();
        Transform3D t3d = new Transform3D();
        t3d.set(new Vector3d(tile.getJava3DCenter()));
        tg.setTransform(t3d);
        Appearance app = new Appearance();
        Material mat = new Material();
        mat.setDiffuseColor(col);
        mat.setAmbientColor(col);
        mat.setSpecularColor(new Color3f(0.0f, 0.0f, 0.0f));
        app.setMaterial(mat);
        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparency(0.8f);
        ta.setTransparencyMode(TransparencyAttributes.NICEST);
        app.setTransparencyAttributes(ta);
        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        app.setPolygonAttributes(pa);
        Box box = new Box((float) size * 0.49f, (float) height * 0.5f, (float) size * 0.49f, app);
        tg.addChild(box);
        return tg;
    }

    public static TransformGroup makeBox(Point3d crs_center, double size, double height, Color3f col) {
        TransformGroup tg = new TransformGroup();
        Transform3D t3d = new Transform3D();
        t3d.set(new Vector3d(crs_center.x, crs_center.z, -crs_center.y));
        tg.setTransform(t3d);
        Appearance app = new Appearance();
        Material mat = new Material();
        mat.setDiffuseColor(col);
        mat.setAmbientColor(col);
        mat.setSpecularColor(new Color3f(0.0f, 0.0f, 0.0f));
        app.setMaterial(mat);
        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparency(0.8f);
        ta.setTransparencyMode(TransparencyAttributes.NICEST);
        app.setTransparencyAttributes(ta);
        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        app.setPolygonAttributes(pa);
        Box box = new Box((float) size * 0.49f, (float) height * 0.5f, (float) size * 0.49f, app);
        tg.addChild(box);
        return tg;
    }

    static Runtime rt = Runtime.getRuntime();

    public static synchronized void checkCache() {
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long allocatedHeapSize = totalMemory - freeMemory;
        memory.setTotalMemory(totalMemory);
        memory.setFreeMemory(freeMemory);
        Navigator.setMemory(memory);
        long cacheHeapSize = allocatedHeapSize - intialHeapSize;
        memory.setCacheHeapSize(cacheHeapSize);
        memory.setCacheSize(Navigator.getMemoryCacheSize());
        int numCacheEntries = Web3DService.sceneBranchGroupManager.numCacheEntries();
        heapSizeHistory[historyCounter] = cacheHeapSize;
        cacheSizeHistory[historyCounter] = numCacheEntries;
        historyCounter++;
        if (historyCounter >= historyPeriod) historyCounter = 0;
        long accuHeapSize = 0;
        int accuCacheSize = 0;
        for (int i = 0; i < historyPeriod; i++) {
            accuHeapSize += heapSizeHistory[i];
            accuCacheSize += cacheSizeHistory[i];
        }
        int targetNumCacheEntries = 0;
        if (accuCacheSize != 0) {
            long meanBytesPerCacheEntry = accuHeapSize / accuCacheSize;
            targetNumCacheEntries = (int) (Navigator.getMemoryCacheSize() / meanBytesPerCacheEntry);
        }
        if (numCacheEntries > targetNumCacheEntries) {
            if (cacheHeapSize > Navigator.getMemoryCacheSize()) {
                Vector3d currentPosition = new Vector3d();
                Navigator.currentJ3DTransform.get(currentPosition);
                Vector3d currentViewingDirection = Navigator.viewer.navigation.getCRSViewingDirection();
                Web3DService.sceneBranchGroupManager.reduceMemoryCacheByOne(currentPosition, currentViewingDirection);
            }
        } else {
        }
    }

    public static void clearCache() {
        Web3DService.sceneBranchGroupManager.clearAllBranchGroups();
    }

    public Reader getGETReader_bak(URL url) {
        Reader reader = null;
        if (Navigator.isVerbose()) System.out.println("Web3DService.getGETReader caching " + url);
        int contentLength = -1;
        URLConnection urlc;
        try {
            urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            if (getEncoding() != null) {
                urlc.setRequestProperty("Authorization", "Basic " + getEncoding());
            }
            urlc.connect();
            String content_type = urlc.getContentType();
            if (content_type == null || content_type.equalsIgnoreCase("x-world/x-vrml") || content_type.equalsIgnoreCase("model/vrml") || content_type.equalsIgnoreCase("model/vrml;charset=ISO-8859-1")) {
                InputStream is = urlc.getInputStream();
                DataInputStream d = new DataInputStream(is);
                contentLength = urlc.getContentLength();
                byte[] content = new byte[contentLength];
                if (d != null) {
                    d.readFully(content, 0, contentLength);
                }
                is.close();
                d.close();
                ByteArrayInputStream bais = new ByteArrayInputStream(content);
                reader = new InputStreamReader(bais);
            } else if (content_type.equalsIgnoreCase("model/vrml.gzip")) {
                InputStream is = urlc.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                GZIPInputStream gis = new GZIPInputStream(bis);
                StringBuffer sb = new StringBuffer();
                BufferedReader zipReader = new BufferedReader(new InputStreamReader(gis));
                char chars[] = new char[10240];
                int len = 0;
                contentLength = 0;
                while ((len = zipReader.read(chars, 0, chars.length)) >= 0) {
                    sb.append(chars, 0, len);
                    contentLength += len;
                }
                chars = null;
                gis.close();
                zipReader.close();
                bis.close();
                is.close();
                reader = new StringReader(sb.toString());
            } else if (content_type.equalsIgnoreCase("model/vrml.encrypted")) {
                InputStream is = urlc.getInputStream();
                StringBuffer sb = new StringBuffer();
                Cipher pbeCipher = createCipher();
                if (pbeCipher != null) {
                    CipherInputStream cis = new CipherInputStream(is, pbeCipher);
                    BufferedReader bufReader = new BufferedReader(new InputStreamReader(cis));
                    char chars[] = new char[1024];
                    int len = 0;
                    contentLength = 0;
                    while ((len = bufReader.read(chars, 0, chars.length)) >= 0) {
                        sb.append(chars, 0, len);
                        contentLength += len;
                    }
                    chars = null;
                    cis.close();
                    bufReader.close();
                    reader = new StringReader(sb.toString());
                }
            } else if (content_type.equalsIgnoreCase("model/vrml.gzip.encrypted")) {
                InputStream is = urlc.getInputStream();
                StringBuffer sb = new StringBuffer();
                Cipher pbeCipher = createCipher();
                if (pbeCipher != null) {
                    CipherInputStream cis = new CipherInputStream(is, pbeCipher);
                    GZIPInputStream gis = new GZIPInputStream(cis);
                    BufferedReader bufReader = new BufferedReader(new InputStreamReader(gis));
                    char chars[] = new char[1024];
                    int len = 0;
                    contentLength = 0;
                    while ((len = bufReader.read(chars, 0, chars.length)) >= 0) {
                        sb.append(chars, 0, len);
                        contentLength += len;
                    }
                    chars = null;
                    bufReader.close();
                    gis.close();
                    cis.close();
                    reader = new StringReader(sb.toString());
                }
            } else if (content_type.equalsIgnoreCase("text/html;charset=utf-8")) {
                System.out.println("text/html;charset=utf-8");
            } else {
                System.err.println("ContentNegotiator.startLoading unsupported MIME type: " + content_type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reader;
    }

    public String getFeatureInfoHTML(Point3d GKposition, String[] layerIds, int featureCount) {
        String html = "";
        try {
            String request = null;
            if (version == VERSION_030) {
                org.gdi3d.xnavi.services.w3ds.x030.GetFeatureInfo getFeatureInfo = new org.gdi3d.xnavi.services.w3ds.x030.GetFeatureInfo(this.serviceEndPoint);
                request = getFeatureInfo.createRequest(GKposition, layerIds, featureCount);
            } else if (version == VERSION_040) {
                org.gdi3d.xnavi.services.w3ds.x040.GetFeatureInfo getFeatureInfo = new org.gdi3d.xnavi.services.w3ds.x040.GetFeatureInfo(this.serviceEndPoint);
                request = getFeatureInfo.createRequest(GKposition, layerIds, featureCount);
            } else if (version == VERSION_041) {
                org.gdi3d.xnavi.services.w3ds.x041.GetFeatureInfo getFeatureInfo = new org.gdi3d.xnavi.services.w3ds.x041.GetFeatureInfo(this.serviceEndPoint);
                request = getFeatureInfo.createRequest(GKposition, layerIds, featureCount);
            }
            if (Navigator.isVerbose()) System.out.println(request);
            URL url = new URL(request);
            int contentLength = -1;
            URLConnection urlc;
            urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            if (getEncoding() != null) {
                urlc.setRequestProperty("Authorization", "Basic " + getEncoding());
            }
            urlc.connect();
            String content_type = urlc.getContentType();
            if (content_type.equalsIgnoreCase("text/html") || content_type.equalsIgnoreCase("text/html;charset=UTF-8")) {
                InputStream is = urlc.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                StringBuffer sb = new StringBuffer();
                InputStreamReader isr = new InputStreamReader(bis);
                char chars[] = new char[10240];
                int len = 0;
                contentLength = 0;
                while ((len = isr.read(chars, 0, chars.length)) >= 0) {
                    sb.append(chars, 0, len);
                    contentLength += len;
                }
                chars = null;
                isr.close();
                bis.close();
                is.close();
                html = sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return html;
    }

    /**
	 * the Methode "getLayerInfoColumnNames" send the GetLayerInfo-Request with the called LayerName and return the response of ColumnNames from the Layer.
	 * @param layerName
	 * @return
	 */
    public Attribute[] getLayerInfoAttributeNames(String layerName) {
        if (version == VERSION_030) {
            org.gdi3d.xnavi.services.w3ds.x030.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x030.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeNames(layerName);
        } else if (version == VERSION_040) {
            org.gdi3d.xnavi.services.w3ds.x040.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x040.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeNames(layerName);
        } else if (version == VERSION_041) {
            org.gdi3d.xnavi.services.w3ds.x041.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x041.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeNames(layerName);
        } else {
            return null;
        }
    }

    public Vector<String> getLayerInfoAttributeValues(String layerName, String columnname) {
        if (version == VERSION_030) {
            org.gdi3d.xnavi.services.w3ds.x030.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x030.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeValues(layerName, columnname);
        } else if (version == VERSION_040) {
            org.gdi3d.xnavi.services.w3ds.x040.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x040.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeValues(layerName, columnname);
        } else if (version == VERSION_041) {
            org.gdi3d.xnavi.services.w3ds.x041.LayerInfoLoader layerInfoLoader = new org.gdi3d.xnavi.services.w3ds.x041.LayerInfoLoader(this);
            return layerInfoLoader.getAttributeValues(layerName, columnname);
        } else {
            return null;
        }
    }

    private Cipher createCipher() {
        Cipher pbeCipher = null;
        try {
            byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };
            int count = 20;
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
            char[] sdfiRgsdgfed = "gFer4FgwertJk8OSDby543HwddfMoaQ".toCharArray();
            PBEKeySpec pbeKeySpec = new PBEKeySpec(sdfiRgsdgfed);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
            pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pbeCipher;
    }

    public static void clearLayer(W3DS_Layer layerInfo) {
        Web3DService.sceneBranchGroupManager.clearLayer(layerInfo);
    }

    public SceneBranchGroup findBranchGroup(String request) {
        return Web3DService.sceneBranchGroupManager.findBranchGroup(request);
    }

    public String getServiceEndPoint() {
        return serviceEndPoint;
    }

    public void setServiceEndPoint(String serviceEndPoint) {
        String endPointSplitAt[] = serviceEndPoint.split("@");
        if (endPointSplitAt.length > 1) {
            String endPointSplitSlash[] = endPointSplitAt[0].split("//");
            String endPointSplitColon[] = endPointSplitSlash[1].split(":");
            this.username = endPointSplitColon[0];
            this.password = endPointSplitColon[1];
            this.setUserPassword(this.username + ":" + this.password);
            serviceEndPoint = endPointSplitSlash[0] + "//" + endPointSplitAt[1];
        }
        if (serviceEndPoint.equals("http://rax.geog.uni-heidelberg.de/W3DS_HD/W3DS")) {
            this.username = "xxx";
            this.password = "xxx";
            this.setUserPassword(this.username + ":" + this.password);
        }
        this.serviceEndPoint = serviceEndPoint;
    }

    public static String getMimeType() {
        return mimeType;
    }

    public static void setMimeType(String mimeType) {
        Web3DService.mimeType = mimeType;
    }

    public void setUserPassword(String userPassword) {
        encoding = new BASE64Encoder().encode(userPassword.getBytes());
        ;
    }

    public String getEncoding() {
        return encoding;
    }

    public static void clearStaticFields() {
        System.out.println("Web3DService.clearStaticFields");
        sceneBranchGroupManager = new SceneBranchGroupManager();
        numberFormat = null;
    }

    public String[] getAcceptVersions() {
        return acceptVersions;
    }
}
