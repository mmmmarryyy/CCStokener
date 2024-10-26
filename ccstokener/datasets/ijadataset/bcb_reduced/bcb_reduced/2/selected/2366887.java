package org.gdi3d.xnavi.services.transform;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import javax.media.ding3d.Transform3D;
import javax.media.ding3d.vecmath.Matrix3d;
import javax.media.ding3d.vecmath.Point3d;
import javax.media.ding3d.vecmath.Vector3d;
import org.gdi3d.xnavi.navigator.Navigator;

public class CoordinateTransformService {

    public static double erdRadius = 6378137.0;

    private String serviceEndPoint;

    public CoordinateTransformService(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    private CoordinateTransformService() {
    }

    public void transform(String sourceCRS, String targetCRS, Point3d point) {
        try {
            URL url = new URL(serviceEndPoint + "?" + "sourceCRS=" + sourceCRS + "&targetCRS=" + targetCRS + "&coordinate=" + point.x + "," + point.y + "," + point.z);
            System.out.println("url " + url);
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            urlc.connect();
            InputStream is = urlc.getInputStream();
            byte[] buffer = new byte[200];
            int length = is.read(buffer);
            String buffer_s = new String(buffer);
            String coord_s = buffer_s.substring(0, length);
            StringTokenizer tok = new StringTokenizer(coord_s, ",");
            String xs = tok.nextToken();
            String ys = tok.nextToken();
            String zs = tok.nextToken();
            double x = Double.parseDouble(xs);
            double y = Double.parseDouble(ys);
            double z = Double.parseDouble(zs);
            point.x = x;
            point.y = y;
            point.z = z;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Point3d CRS_to_Java3D(Point3d CRSCenter) {
        if (Navigator.globe) {
            Transform3D t3d = getLatLonTransform(CRSCenter);
            Vector3d translation = new Vector3d();
            t3d.get(translation);
            return new Point3d(translation);
        } else {
            return new Point3d(CRSCenter.x, CRSCenter.z, -CRSCenter.y);
        }
    }

    public static double CRS_to_Java3D(double length, double crs_y, double height) {
        if (Navigator.globe) {
            if (length >= 1.2730491657210756E7) {
                return erdRadius * (length / 1.2730491657210756E7);
            }
            double lat = 2 * Math.atan(Math.exp(crs_y / 6378137.0)) - Math.PI / 2.0;
            double lon_factor = 1.0;
            double a = -(Math.PI / 2.0 - lat);
            double sin_a = Math.sin(a);
            double cos_a = Math.cos(a);
            double b = length / 6378137.0;
            if (b > Math.PI) {
                b = Math.PI;
                lon_factor = b / Math.PI;
            }
            double sin_b = Math.sin(b);
            double cos_b = Math.cos(b);
            Matrix3d xrot_mat = new Matrix3d();
            xrot_mat.setIdentity();
            xrot_mat.m11 = cos_a;
            xrot_mat.m21 = -sin_a;
            xrot_mat.m12 = sin_a;
            xrot_mat.m22 = cos_a;
            Matrix3d yrot_mat = new Matrix3d();
            yrot_mat.setIdentity();
            yrot_mat.m00 = cos_b;
            yrot_mat.m20 = -sin_b;
            yrot_mat.m02 = sin_b;
            yrot_mat.m22 = cos_b;
            Transform3D xrot = new Transform3D();
            xrot.set(xrot_mat);
            Transform3D yrot = new Transform3D();
            yrot.set(yrot_mat);
            Vector3d view_cart_vec2 = new Vector3d(0.0, erdRadius + height, 0.0);
            xrot.transform(view_cart_vec2);
            Vector3d p0 = new Vector3d(view_cart_vec2);
            yrot.transform(view_cart_vec2);
            Vector3d p1 = new Vector3d(view_cart_vec2);
            p1.sub(p0);
            double result = lon_factor * p1.length();
            return result;
        } else {
            return length;
        }
    }

    public static void getLatLon(Point3d CRSCenter, Point3d wgs84Target) {
        wgs84Target.x = CRSCenter.x / erdRadius;
        wgs84Target.y = 2 * Math.atan(Math.exp(CRSCenter.y / erdRadius)) - Math.PI / 2.0;
        wgs84Target.z = CRSCenter.z;
    }

    public static Point3d getLatLon(Point3d CRSCenter) {
        double lon = CRSCenter.x / erdRadius;
        double lat = 2 * Math.atan(Math.exp(CRSCenter.y / erdRadius)) - Math.PI / 2.0;
        return new Point3d(lon, lat, CRSCenter.z);
    }

    public static Transform3D getLatLonTransform(Point3d CRSCenter) {
        double lon = CRSCenter.x / erdRadius;
        double lat = 2 * Math.atan(Math.exp(CRSCenter.y / erdRadius)) - Math.PI / 2.0;
        return getLatLonTransform(lat, lon, CRSCenter.z);
    }

    public static Transform3D getLatLonTransform(double lat, double lon, double height) {
        double a = -(Math.PI / 2.0 - lat);
        double sin_a = Math.sin(a);
        double cos_a = Math.cos(a);
        double b = lon;
        double sin_b = Math.sin(b);
        double cos_b = Math.cos(b);
        Matrix3d xrot_mat = new Matrix3d();
        xrot_mat.setIdentity();
        xrot_mat.m11 = cos_a;
        xrot_mat.m21 = -sin_a;
        xrot_mat.m12 = sin_a;
        xrot_mat.m22 = cos_a;
        Matrix3d yrot_mat = new Matrix3d();
        yrot_mat.setIdentity();
        yrot_mat.m00 = cos_b;
        yrot_mat.m20 = -sin_b;
        yrot_mat.m02 = sin_b;
        yrot_mat.m22 = cos_b;
        Transform3D xrot = new Transform3D();
        xrot.set(xrot_mat);
        Transform3D yrot = new Transform3D();
        yrot.set(yrot_mat);
        Vector3d view_cart_vec2 = new Vector3d(0.0, erdRadius + height, 0.0);
        xrot.transform(view_cart_vec2);
        yrot.transform(view_cart_vec2);
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(view_cart_vec2);
        t3d.mul(yrot);
        t3d.mul(xrot);
        return t3d;
    }

    public static Vector3d getCRSPosition(Vector3d center) {
        Vector3d vector = new Vector3d(center);
        double height = vector.length() - erdRadius;
        Vector3d down = new Vector3d(0, -1, 0);
        vector.normalize();
        double a = down.dot(vector);
        double lat = Math.acos(a) - Math.PI / 2.0;
        if (lat > Math.PI / 2.0) lat = Math.PI / 2.0;
        if (lat < -Math.PI / 2.0) lat = -Math.PI / 2.0;
        vector.y = 0.0;
        vector.normalize();
        Vector3d front = new Vector3d(0, 0, 1);
        double b = front.dot(vector);
        Vector3d right = new Vector3d(1, 0, 0);
        double c = right.dot(vector);
        double lon = Math.acos(b);
        if (c < 0.0) lon = -lon;
        double crs_x = lon * erdRadius;
        double crs_y = Math.log(Math.tan((lat + Math.PI / 2.0) / 2.0)) * erdRadius;
        return new Vector3d(crs_x, crs_y, height);
    }

    /**
	 * @param args
	 */
    public static void main2(String[] args) {
        String sourceCRS = "epsg:31467";
        String targetCRS = "epsg:4326";
        Point3d ll = new Point3d(3461000, 5465000, 0);
        Point3d ur = new Point3d(3493000, 5489000, 0);
        CoordinateTransformService cts = new CoordinateTransformService("http://131.220.111.121:8080/CoordinateTransformService/cts");
        cts.transform(sourceCRS, targetCRS, ll);
        System.out.println("ll: " + ll.x + " " + ll.y + " " + ll.z);
        cts.transform(sourceCRS, targetCRS, ur);
        System.out.println("ur: " + ur.x + " " + ur.y + " " + ur.z);
    }

    public static void main3(String[] args) {
        double lon = -180.0;
        double lat = -89.999;
        lon = lon * Math.PI / 180.0;
        lat = lat * Math.PI / 180.0;
        double crs_x = lon * 6378137.0;
        double crs_y = Math.log(Math.tan((lat + Math.PI / 2.0) / 2.0)) * 6378137.0;
        double a = 6378137.0;
        System.out.println(crs_x + " " + crs_y);
    }

    public static void main(String[] args) {
        Point3d CRSCenter = new Point3d(0, 20037508.342789244, 0);
        Transform3D latlon_t3d = CoordinateTransformService.getLatLonTransform(CRSCenter);
        Vector3d translation = new Vector3d();
        latlon_t3d.get(translation);
        System.out.println(translation.x + " " + translation.y + " " + translation.z);
    }
}
