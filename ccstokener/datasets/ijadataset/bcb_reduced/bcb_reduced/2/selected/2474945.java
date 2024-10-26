package org.gdi3d.xnavi.services.eqs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.media.ding3d.vecmath.Point3d;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKTReader;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.navigator.Tile;
import org.gdi3d.xnavi.services.w3ds.Style;
import org.gdi3d.xnavi.services.w3ds.W3DS_Layer;

public class ElevationQueryService {

    private String serviceEndPoint = null;

    private HttpClient httpClient;

    public ElevationQueryService(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams cm_params = connectionManager.getParams();
        cm_params.setDefaultMaxConnectionsPerHost(20);
        cm_params.setConnectionTimeout(Navigator.TIME_OUT);
        cm_params.setSoTimeout(Navigator.TIME_OUT);
        connectionManager.setParams(cm_params);
        httpClient = new HttpClient(connectionManager);
    }

    public LineString processLine(Point3d[] coordinates) {
        LineString new_linestring = null;
        String errorMessage = "";
        try {
            String xml_request = createConvertGeometryRequest_Post(coordinates);
            InputStream response_inputstream = null;
            PostMethod method = new PostMethod(serviceEndPoint);
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
            BufferedInputStream bufis = new BufferedInputStream(response_inputstream);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = bufis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            String wkt = bos.toString();
            bufis.close();
            bos.close();
            if (Navigator.isVerbose()) System.out.println("wkt " + wkt);
            WKTReader wr = new WKTReader();
            new_linestring = (LineString) wr.read(wkt);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage += "<p>Error occured while connecting to ElevationQueryService</p>";
        }
        if (!errorMessage.equals("")) {
            System.out.println("\nerrorMessage: " + errorMessage + "\n\n");
            JLabel label1 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">Error</span></body></html>");
            JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + errorMessage + "<br>" + "<p>please check Java console. If problem persits, please report to system manager</p>" + "</span></body></html>");
            Object[] objects = { label1, label2 };
            JOptionPane.showMessageDialog(null, objects, "Error Message", JOptionPane.ERROR_MESSAGE);
        }
        return new_linestring;
    }

    public double getHeight(double x, double y, int level) {
        double height = -1.0;
        String errorMessage = "";
        try {
            URL url = new URL(serviceEndPoint + "?" + "request=getHeight" + "&coordinate=" + x + "," + y);
            if (Navigator.isVerbose()) System.out.println("ElevationQueryService url " + url);
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            urlc.connect();
            InputStream is = urlc.getInputStream();
            byte[] buffer = new byte[200];
            int length = is.read(buffer);
            String buffer_s = new String(buffer);
            String zs = buffer_s.substring(0, length);
            height = Double.parseDouble(zs);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage += "<p>Error occured while connecting to ElevationQueryService</p>";
        }
        if (!errorMessage.equals("")) {
            System.out.println("\nerrorMessage: " + errorMessage + "\n\n");
            JLabel label1 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">Error</span></body></html>");
            JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + errorMessage + "<br>" + "<p>please check Java console. If problem persits, please report to system manager</p>" + "</span></body></html>");
            Object[] objects = { label1, label2 };
            JOptionPane.showMessageDialog(null, objects, "Error Message", JOptionPane.ERROR_MESSAGE);
        }
        return height;
    }

    public static String createConvertGeometryRequest_Post(Point3d[] coordinates) {
        StringBuffer postRequest = new StringBuffer();
        String version = "0.1.0";
        if (version.equals("0.1.0")) {
            postRequest.append("<?xml version='1.0' encoding='UTF-8'?>\n <ConvertGeometry ");
            postRequest.append(" xmlns='http://www.opengis.net/eqs/0.1.0' xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:gml='http://www.opengis.net/gml' service='EQS' request='ConvertGeometry' version='0.1.0'");
            postRequest.append(">\n");
            postRequest.append("<CRS>EPSG:" + Navigator.getEpsg_code() + "</CRS>");
            postRequest.append("<gml:LineString>");
            postRequest.append("    <gml:coordinates>");
            int numCoordinates = coordinates.length;
            for (int i = 0; i < numCoordinates; i++) {
                postRequest.append(coordinates[i].x + "," + coordinates[i].y + " ");
            }
            postRequest.append("  </gml:coordinates>");
            postRequest.append("</gml:LineString >");
            postRequest.append("\n</ConvertGeometry>");
        }
        if (Navigator.isVerbose()) System.out.println(postRequest.toString() + "\n");
        return postRequest.toString();
    }
}
