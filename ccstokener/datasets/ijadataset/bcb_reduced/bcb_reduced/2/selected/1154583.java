package edu.ucsd.ncmir.INCFAgent;

import edu.ucsd.ncmir.spl.minixml.Document;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.minixml.JDOMException;
import edu.ucsd.ncmir.spl.minixml.SAXBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author spl
 */
public class INCFAgent {

    private static final String TRANSFMT = "http://incf-dev-local.crbs.ucsd.edu/central/atlas?service=WPS&version=1.0.0&request=Execute&Identifier=GetTransformationChain&DataInputs=inputSrsName=%s;outputSrsName=Mouse_WHS_0.9;filter=cerebellum";

    private static final String REGFMT = "http://image.wholebraincatalog.org:80/atlas-serverside/servlet/ImageAssemblyServlet?func=getImageDataForRegID&imageRegistrationID=%s";

    private static final double[] ABA_REFERENCE_OFFSET_TABLE = { 5.345, 5.245, 5.145, 5.045, 4.945, 4.845, 4.745, 4.645, 4.545, 4.445, 4.345, 4.245, 4.145, 4.045, 3.945, 3.845, 3.745, 3.645, 3.545, 3.445, 3.345, 3.245, 3.145, 3.045, 2.945, 2.845, 2.745, 2.620, 2.545, 2.445, 2.345, 2.245, 2.145, 2.045, 1.945, 1.845, 1.745, 1.645, 1.545, 1.420, 1.345, 1.245, 1.145, 1.045, 0.945, 0.845, 0.745, 0.620, 0.545, 0.445, 0.345, 0.245, 0.145, 0.020, -0.080, -0.180, -0.280, -0.380, -0.480, -0.555, -0.655, -0.755, -0.880, -0.955, -1.055, -1.155, -1.255, -1.355, -1.455, -1.555, -1.655, -1.755, -1.855, -1.955, -2.055, -2.155, -2.155, -2.355, -2.480, -2.555, -2.780, -2.880, -2.980, -3.080, -3.180, -3.280, -3.380, -3.455, -3.580, -3.680, -3.780, -3.880, -3.980, -4.080, -4.155, -4.280, -4.380, -4.455, -4.555, -4.655, -4.780, -4.855, -4.955, -5.055, -5.155, -5.255, -5.380, -5.455, -5.555, -5.655, -5.780, -5.550, -5.955, -6.550, -6.180, -6.255, -6.355, -6.455, -6.555, -6.655, -6.755, -6.855, -6.955, -7.055, -7.155, -7.255, -7.355, -7.455, -7.555, -7.655, -7.755, -7.905, 3.925, 3.725, 3.550, 3.325, 3.125, 2.950, 2.725, 2.525, 2.350, 2.150, 1.950, 1.725, 1.525, 1.350, 1.100, 0.875, 0.675, 0.475, 0.225, -0.050, -0.200 };

    private static final String[] ABA_REFERENCE_VIEW_TABLE = { "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "coronal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal", "sagittal" };

    private static Hashtable<String, Matrix> VIEW_LOOKUP = new Hashtable<String, Matrix>();

    static {
        INCFAgent.VIEW_LOOKUP.put("coronal", new Matrix(0, 0, 1, -1, 0, 0, 0, -1, 0));
        INCFAgent.VIEW_LOOKUP.put("sagittal", new Matrix(0, 0, 1, -1, 0, 0, 0, -1, 0));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new INCFAgent().convert(args[0]);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private static class Matrix {

        private double[][] _m = new double[3][3];

        public Matrix(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
            this._m[0][0] = m00;
            this._m[0][1] = m01;
            this._m[0][2] = m02;
            this._m[1][0] = m10;
            this._m[1][1] = m11;
            this._m[1][2] = m12;
            this._m[2][0] = m20;
            this._m[2][1] = m21;
            this._m[2][2] = m22;
        }

        double getX(double u, double v, double w) {
            return (u * this._m[0][0]) + (v * this._m[0][1]) + (w * this._m[0][2]);
        }

        double getY(double u, double v, double w) {
            return (u * this._m[1][0]) + (v * this._m[1][1]) + (w * this._m[1][2]);
        }

        double getZ(double u, double v, double w) {
            return (u * this._m[2][0]) + (v * this._m[2][1]) + (w * this._m[2][2]);
        }
    }

    private Element fetchFromURL(String url_string) throws MalformedURLException, IOException, JDOMException {
        URL url = new URL(url_string);
        InputStream is = url.openStream();
        Document d = new SAXBuilder().build(is);
        is.close();
        return d.getRootElement();
    }

    private void convert(String url_string) throws MalformedURLException, IOException, JDOMException {
        Element data = this.fetchFromURL(url_string).descendTo("AnnotationResponse");
        for (Element e : data.getChildren("Annotation")) this.handleAnnotation(e);
    }

    private void handleAnnotation(Element annotation) throws MalformedURLException, IOException, JDOMException {
        System.err.println(annotation);
        Element resource = annotation.descendTo("RESOURCE");
        String filepath = resource.getAttributeValue("filepath");
        Matrix m = new Matrix(1, 0, 0, 0, -1, 0, 0, 0, -1);
        double offset = 0;
        boolean is_image_ref = false;
        List<Element> geometry = annotation.getChild("GEOMETRIES").getChildren("GEOMETRY");
        for (Element g : geometry) {
            System.err.println(g);
            for (Element polygon : g.getChildren("POLYGON")) {
                String srs_name = polygon.getAttribute("srsName").getValue();
                Element pos = g.descendTo("pos");
                String[] c = pos.getText().split(" +");
                String url_string = String.format(INCFAgent.TRANSFMT, srs_name);
                Element chain = this.fetchFromURL(url_string).descendTo("CoordinateTransformationChain");
                System.err.println(chain);
                ArrayList<double[]> cvals = new ArrayList<double[]>();
                for (int i = 0; i < c.length - 2; i += 3) {
                    double x = new Double(c[i]).doubleValue();
                    double y = new Double(c[i + 1]).doubleValue();
                    double z = is_image_ref ? offset : new Double(c[i + 2]).doubleValue();
                    cvals.add(this.convert(chain, m.getX(x, y, z), m.getY(x, y, z), m.getZ(x, y, z)));
                }
            }
        }
    }

    private String insert(String s, String key, double val) {
        String[] parts = s.split(key);
        return parts[0] + key + val + ((parts.length == 2) ? parts[1] : "");
    }

    private double[] convert(Element chain, double u, double v, double w) throws MalformedURLException, IOException, JDOMException {
        if (chain == null) {
            System.err.println("null chain!");
            System.exit(1);
        }
        for (Element cvt : chain.getChildren("CoordinateTransformation")) {
            String cvt_url_string = cvt.getText().replaceAll("&amp;", "&");
            cvt_url_string = this.insert(cvt_url_string, "x=", u);
            cvt_url_string = this.insert(cvt_url_string, "y=", v);
            cvt_url_string = this.insert(cvt_url_string, "z=", w);
            Element cvted = this.fetchFromURL(cvt_url_string);
            Element pos = cvted.descendTo("pos");
            if (pos != null) {
                String[] triplet = pos.getText().split(" +");
                u = new Double(triplet[0]).doubleValue();
                v = new Double(triplet[1]).doubleValue();
                w = new Double(triplet[2]).doubleValue();
            } else {
                System.err.println("fail.");
                System.err.println(cvted);
                u = v = w = 0;
                break;
            }
        }
        return new double[] { u, v, w };
    }
}
