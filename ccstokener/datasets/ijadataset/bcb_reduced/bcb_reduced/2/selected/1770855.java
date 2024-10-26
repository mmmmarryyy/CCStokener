package de.grogra.ext.x3d.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3d;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Vector3d;
import de.grogra.ext.x3d.X3DImport;
import de.grogra.imp.io.ImageReader;
import de.grogra.imp.objects.FixedImageAdapter;
import de.grogra.imp.objects.ImageAdapter;
import de.grogra.math.RGBColor;
import de.grogra.pf.ui.Workbench;
import de.grogra.pf.ui.registry.FileFactory;

/**
 * This class provides usefull static methods for recurring tasks.
 * 
 * @author Udo Bischof, Uwe Mannl
 *
 */
public class Util {

    /**
	 * Regular expression which is used to split single values in strings of x3d attributes.
	 */
    private static String splitExpr = "[ ,\n]+";

    /**
	 * @param value
	 * @return
	 */
    public static float[] splitStringToArray2f(String value) {
        return splitStringToArray2f(value, 0.0f, 0.0f);
    }

    /**
	 * @param value String value
	 * @param default1
	 * @param default2
	 * @return
	 */
    public static float[] splitStringToArray2f(String value, float default1, float default2) {
        float[] returnValue = { default1, default2 };
        if (value != null) {
            value = value.trim();
            String[] results = value.split(splitExpr);
            for (int i = 0; (i < 2) && (i < results.length); i++) {
                returnValue[i] = Float.valueOf(results[i]);
            }
        }
        return returnValue;
    }

    /**
	 * @param value
	 * @return
	 */
    public static double[] splitStringToArray3d(String value) {
        return splitStringToArray3d(value, 0.0d, 0.0d, 0.0d);
    }

    /**
	 * @param value
	 * @return
	 */
    public static float[] splitStringToArray3f(String value) {
        return splitStringToArray3f(value, 0.0f, 0.0f, 0.0f);
    }

    /**
	 * @param value String value
	 * @param default1
	 * @param default2
	 * @param default3
	 * @return
	 */
    public static double[] splitStringToArray3d(String value, double default1, double default2, double default3) {
        double[] returnValue = { default1, default2, default3 };
        if (value != null) {
            value = value.trim();
            String[] results = value.split(splitExpr);
            for (int i = 0; (i < 3) && (i < results.length); i++) {
                returnValue[i] = Double.valueOf(results[i]);
            }
        }
        return returnValue;
    }

    /**
	 * @param value String value
	 * @param default1
	 * @param default2
	 * @param default3
	 * @return
	 */
    public static float[] splitStringToArray3f(String value, float default1, float default2, float default3) {
        float[] returnValue = { default1, default2, default3 };
        if (value != null) {
            value = value.trim();
            String[] results = value.split(splitExpr);
            for (int i = 0; (i < 3) && (i < results.length); i++) {
                returnValue[i] = Float.valueOf(results[i]);
            }
        }
        return returnValue;
    }

    /**
	 * @param result Tuple2f result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple2f splitStringToTuple2f(Tuple2f result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Float.valueOf(results[0]);
        result.y = Float.valueOf(results[1]);
        return result;
    }

    /**
	 * Split string and convert into groimp coordinates.
	 * @param result Tuple3f result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple3f convertStringToTuple3f(Tuple3f result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Float.valueOf(results[0]);
        result.y = -Float.valueOf(results[2]);
        result.z = Float.valueOf(results[1]);
        return result;
    }

    /**
	 * Split string without convert.
	 * @param result Tuple3f result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple3f splitStringToTuple3f(Tuple3f result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Float.valueOf(results[0]);
        result.y = Float.valueOf(results[1]);
        result.z = Float.valueOf(results[2]);
        return result;
    }

    /**
	 * Split string and convert into groimp coordinates.
	 * @param result Tuple3d result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple3d convertStringToTuple3d(Tuple3d result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Double.valueOf(results[0]);
        result.y = -Double.valueOf(results[2]);
        result.z = Double.valueOf(results[1]);
        return result;
    }

    /**
	 * Split string without convert.
	 * @param result Tuple3d result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple3d splitStringToTuple3d(Tuple3d result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Double.valueOf(results[0]);
        result.y = Double.valueOf(results[1]);
        result.z = Double.valueOf(results[2]);
        return result;
    }

    /**
	 * Split string without convert.
	 * @param result Tuple4f result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple4f splitStringToTuple4f(Tuple4f result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Float.valueOf(results[0]);
        result.y = Float.valueOf(results[1]);
        result.z = Float.valueOf(results[2]);
        result.w = Float.valueOf(results[3]);
        return result;
    }

    /**
	 * Split string and convert into groimp coordinates.
	 * @param result Tuple4f result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static Tuple4f convertStringToTuple4f(Tuple4f result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Float.valueOf(results[0]);
        result.y = -Float.valueOf(results[2]);
        result.z = Float.valueOf(results[1]);
        result.w = Float.valueOf(results[3]);
        return result;
    }

    /**
	 * Split string and convert into groimp coordinates.
	 * @param result AxisAngle4d result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static AxisAngle4d convertStringToAxisAngle4d(AxisAngle4d result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Double.valueOf(results[0]);
        result.y = -Double.valueOf(results[2]);
        result.z = Double.valueOf(results[1]);
        result.angle = Double.valueOf(results[3]);
        return result;
    }

    /**
	 * Split string without convert.
	 * @param result AxisAngle4d result
	 * @param value String value (must not be null!)
	 * @return
	 */
    public static AxisAngle4d splitStringToAxisAngle4d(AxisAngle4d result, String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        result.x = Double.valueOf(results[0]);
        result.y = Double.valueOf(results[1]);
        result.z = Double.valueOf(results[2]);
        result.angle = Double.valueOf(results[3]);
        return result;
    }

    /**
	 * 
	 * @param value String with int values
	 * @param def default values
	 * @return
	 */
    public static int[] splitStringToArrayOfInt(String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        int[] returnValue = new int[results.length];
        for (int i = 0; i < results.length; i++) {
            returnValue[i] = Integer.valueOf(results[i]);
        }
        return returnValue;
    }

    /**
	 * 
	 * @param value String with int values
	 * @param def default values
	 * @return
	 */
    public static int[] splitStringToArrayOfInt(String value, int[] defaultArray) {
        if (value != null) return splitStringToArrayOfInt(value);
        return defaultArray;
    }

    /**
	 * 
	 * @param value String with float values
	 * @param def default values
	 * @return
	 */
    public static float[] splitStringToArrayOfFloat(String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        float[] returnValue = new float[results.length];
        for (int i = 0; i < results.length; i++) {
            returnValue[i] = Float.valueOf(results[i]);
        }
        return returnValue;
    }

    /**
	 * 
	 * @param value String with float values
	 * @param def default values
	 * @return
	 */
    public static float[] splitStringToArrayOfFloat(String value, float[] defaultArray) {
        if (value != null) return splitStringToArrayOfFloat(value);
        return defaultArray;
    }

    /**
	 * 
	 * @param value String with double values
	 * @return
	 */
    public static double[] splitStringToArrayOfDouble(String value) {
        value = value.trim();
        String[] results = value.split(splitExpr);
        double[] returnValue = new double[results.length];
        for (int i = 0; i < results.length; i++) {
            returnValue[i] = Double.valueOf(results[i]);
        }
        return returnValue;
    }

    /**
	 * 
	 * @param value String with double values
	 * @param def default values
	 * @return
	 */
    public static double[] splitStringToArrayOfDouble(String value, double[] def) {
        if (value != null) return splitStringToArrayOfDouble(value);
        return def;
    }

    /**
	 * Splits a string containing multiple strings to an array of strings.
	 * The original string can have one of the following forms:
	 * <ul>
	 * <li>'"bla" "ble"'
	 * <li>'"bla","ble"'
	 * <li>'"bla"'
	 * <li>'bla'
	 * <li>"bla"
	 * </ul>
	 * @param value String with String values
	 * @param def default values
	 * @return
	 */
    public static String[] splitStringToArrayOfString(String value) {
        value = value.trim();
        String[] returnValue = value.split("\"[, ]+\"");
        returnValue[0] = returnValue[0].replace("\"", "");
        returnValue[returnValue.length - 1] = returnValue[returnValue.length - 1].replace("\"", "");
        return returnValue;
    }

    /**
	 * Splits a string containing multiple strings to an array of strings.
	 * The original string can have one of the following forms:
	 * <ul>
	 * <li>'"bla" "ble"'
	 * <li>'"bla","ble"'
	 * <li>'"bla"'
	 * <li>'bla'
	 * <li>"bla"
	 * </ul>
	 * @param value String with String values
	 * @param def default values
	 * @return
	 */
    public static String[] splitStringToArrayOfString(String value, String[] def) {
        if (value != null) {
            value = value.trim();
            String[] returnValue = value.split("\"[, ]+\"");
            returnValue[0] = returnValue[0].replace("\"", "");
            returnValue[returnValue.length - 1] = returnValue[returnValue.length - 1].replace("\"", "");
            return returnValue;
        }
        return def;
    }

    /**
	 * Split String on Whitespaces and return values as double-Array
	 * @param value
	 * @return
	 */
    public static double[] splitStringToArray4d(String value) {
        return splitStringToArray4d(value, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    /**
	 * Split String on Whitespaces and return values as double-Array
	 * @param value
	 * @return
	 */
    public static float[] splitStringToArray4f(String value) {
        return splitStringToArray4f(value, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
	 * Split String on Whitespaces and return values as double-Array
	 * @param value String value
	 * @param default1
	 * @param default2
	 * @param default3
	 * @param default4
	 * @return
	 */
    public static double[] splitStringToArray4d(String value, double default1, double default2, double default3, double default4) {
        double[] returnValue = { default1, default2, default3, default4 };
        if (value != null) {
            value = value.trim();
            String[] results = value.split(splitExpr);
            for (int i = 0; (i < 4) && (i < results.length); i++) {
                returnValue[i] = Double.valueOf(results[i]);
            }
        }
        return returnValue;
    }

    /**
	 * Split String on Whitespaces and return values as double-Array
	 * @param value String value
	 * @param default1
	 * @param default2
	 * @param default3
	 * @param default4
	 * @return
	 */
    public static float[] splitStringToArray4f(String value, float default1, float default2, float default3, float default4) {
        float[] returnValue = { default1, default2, default3, default4 };
        if (value != null) {
            value = value.trim();
            String[] results = value.split(splitExpr);
            for (int i = 0; (i < 4) && (i < results.length); i++) {
                returnValue[i] = Float.valueOf(results[i]);
            }
        }
        return returnValue;
    }

    /**
	 * Extract the path-part as a String from the URL.
	 * 
	 * @param URL url
	 * @return String path without the filename
	 */
    public static String getRealPath(URL url) {
        String cache = url.getPath();
        int beginIndex = 0;
        if (cache.startsWith("/")) beginIndex = 1;
        int endIndex = cache.lastIndexOf("/") + 1;
        String returnValue = cache.substring(beginIndex, endIndex);
        returnValue = returnValue.replace("%20", " ");
        return returnValue;
    }

    /**
	 * Extract the path-part as a String from the URL.
	 * 
	 * @param File file
	 * @return String path without the filename
	 */
    public static String getRealPath(File file) {
        return file.getParent() + File.separator;
    }

    /**
	 * This methode returns a transformation matrix with a rotational component.
	 * The rotation transforms the vec1 to vec2. 
	 * @param vec1
	 * @param vec2
	 * @return
	 */
    public static Matrix4d vectorsToTransMatrix(Vector3d vec1, Vector3d vec2) {
        Vector3d rotVec = new Vector3d();
        rotVec.cross(vec1, vec2);
        AxisAngle4d rot = null;
        if (rotVec.equals(new Vector3d(0, 0, 0))) {
            rotVec = findOrthogonalVector(vec1);
            rot = new AxisAngle4d(rotVec, vec1.angle(vec2) + Math.PI);
        } else rot = new AxisAngle4d(rotVec, vec1.angle(vec2));
        Matrix4d transMat = new Matrix4d();
        transMat.setIdentity();
        transMat.setRotation(rot);
        return transMat;
    }

    /**
	 * Returns an orthogonal vector to given vector. This is undefined
	 * in any direction and not normalized.
	 * @param vec1
	 * @return
	 */
    public static Vector3d findOrthogonalVector(Vector3d vec1) {
        Vector3d returnVector = new Vector3d(0, 0, 0);
        if (((vec1.x == 0) && (vec1.y == 0)) || ((vec1.x == 0) && (vec1.z == 0))) returnVector.x = 1; else if ((vec1.y == 0) && (vec1.z == 0)) returnVector.y = 1; else {
            if (vec1.x != 0) {
                returnVector.x = -vec1.x;
                returnVector.y = vec1.y;
                returnVector.z = vec1.z;
            } else {
                returnVector.x = vec1.x;
                returnVector.y = -vec1.y;
                returnVector.z = vec1.z;
            }
        }
        return returnVector;
    }

    /**
	 * Checks if given points are on a straight line.
	 * @param vectors
	 * @return
	 */
    public static boolean pointsOnLine(ArrayList<Point3d> points) {
        if (points.size() <= 2) return true;
        Vector3d refVec = new Vector3d(points.get(0).x - points.get(1).x, points.get(0).y - points.get(1).y, points.get(0).z - points.get(1).z);
        refVec.normalize();
        for (int i = 2; i < points.size(); i++) {
            Vector3d newVec = new Vector3d(points.get(0).x - points.get(i).x, points.get(0).y - points.get(i).y, points.get(0).z - points.get(i).z);
            newVec.normalize();
            if (!newVec.equals(refVec)) return false;
        }
        return true;
    }

    public static List<String> splitStringToListOfStrings(String value) {
        value = value.trim();
        String[] returnValue = value.split("\"[, ]+\"");
        returnValue[0] = returnValue[0].replace("\"", "");
        returnValue[returnValue.length - 1] = returnValue[returnValue.length - 1].replace("\"", "");
        List<String> strings = new ArrayList<String>();
        for (String s : returnValue) {
            strings.add(s);
        }
        return strings;
    }

    public static RGBColor intToRGB(int color) {
        float r = ((color >> 16) & 255) * (1f / 255);
        float g = ((color >> 8) & 255) * (1f / 255);
        float b = (color & 255) * (1f / 255);
        return new RGBColor(r, g, b);
    }

    public static float intToGray(int color) {
        float r = ((color >> 16) & 255) * (1f / 255);
        float g = ((color >> 8) & 255) * (1f / 255);
        float b = (color & 255) * (1f / 255);
        return (r + g + b) / 3f;
    }

    public static ImageAdapter getImageForURL(String imgUrl, boolean saveInGroIMP) {
        ImageAdapter ia = null;
        boolean loadedFromWeb = false;
        File f = null;
        URL url = null;
        Workbench wb = Workbench.current();
        try {
            String imgpath = imgUrl;
            if (imgpath.startsWith("\"") && imgpath.endsWith("\"")) imgpath = imgpath.substring(1, imgpath.length() - 1);
            if (imgpath.toLowerCase().startsWith("http://")) {
                String filename = imgpath.substring(imgpath.lastIndexOf("/") + 1, imgpath.lastIndexOf("."));
                String fileext = imgpath.substring(imgpath.lastIndexOf("."), imgpath.length());
                f = File.createTempFile(filename, fileext);
                url = new URL(imgpath);
                InputStream is = url.openStream();
                FileOutputStream os = new FileOutputStream(f);
                byte[] buffer = new byte[0xFFFF];
                for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
                is.close();
                os.close();
                url = f.toURI().toURL();
                loadedFromWeb = true;
            } else {
                if (imgpath.startsWith("/") || (imgpath.charAt(1) == ':')) {
                } else {
                    File x3dfile = X3DImport.getTheImport().getCurrentParser().getFile();
                    imgpath = Util.getRealPath(x3dfile) + imgpath;
                }
                f = new File(imgpath);
                url = f.toURI().toURL();
                Object testContent = url.getContent();
                if (testContent == null) return null;
                loadedFromWeb = false;
            }
            if (saveInGroIMP) {
                FileFactory ff = ImageReader.getFactory(wb.getRegistry());
                ia = (FixedImageAdapter) ff.addFromURL(wb.getRegistry(), url, null, wb);
            } else {
                ia = new FixedImageAdapter(ImageIO.read(url));
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            if (loadedFromWeb && f != null) {
                f.delete();
            }
        }
        return ia;
    }

    public static ImageAdapter getImageForURL(List<String> imgURL, HashMap<String, String> valueMap, boolean saveInGroIMP) {
        ImageAdapter ia = null;
        List<String> urls = null;
        if (valueMap.get("url") != null) {
            String allUrls = valueMap.get("url");
            urls = Util.splitStringToListOfStrings(allUrls);
        } else {
            urls = imgURL;
            String tmpUrls = urls.toString();
            urls = Util.splitStringToListOfStrings(tmpUrls);
        }
        int imageCount = urls.size();
        for (int imageIndex = 0; imageIndex < imageCount; imageIndex++) {
            ia = getImageForURL(urls.get(imageIndex), saveInGroIMP);
            X3DImport.getTheImport().increaseProgress();
        }
        return ia;
    }

    private static int f2i(float f) {
        int i = Math.round(f * 255);
        return (i < 0) ? 0 : (i > 255) ? 255 : i;
    }

    /**
	 * Converts a Tuple4f color object (range from 0-1) to an int color.
	 * @param color
	 * @return
	 */
    public static int colorToInt(Tuple4f color) {
        return (f2i(color.w) << 24) + (f2i(color.x) << 16) + (f2i(color.y) << 8) + f2i(color.z);
    }

    /**
	 * Converts a Tuple3f color object (range from 0-1) to an int color.
	 * @param color
	 * @return
	 */
    public static int colorToInt(Tuple3f color) {
        return (255 << 24) + (f2i(color.x) << 16) + (f2i(color.y) << 8) + f2i(color.z);
    }

    /**
	 * Calculates to an background pixel a new foreground pixel in consideration
	 * of transparency of foreground pixel.
	 * @param background
	 * @param foreground
	 * @return
	 */
    public static int overlapPixel(int background, int foreground) {
        int fgAlpha = (foreground >> 24) & 0xff;
        int fgRed = (foreground >> 16) & 0xff;
        int fgGreen = (foreground >> 8) & 0xff;
        int fgBlue = (foreground) & 0xff;
        int bgRed = (background >> 16) & 0xff;
        int bgGreen = (background >> 8) & 0xff;
        int bgBlue = (background) & 0xff;
        double ratio = fgAlpha / 255.0;
        bgRed = (int) ((1 - ratio) * bgRed + ratio * fgRed);
        bgGreen = (int) ((1 - ratio) * bgGreen + ratio * fgGreen);
        bgBlue = (int) ((1 - ratio) * bgBlue + ratio * fgBlue);
        return (255 << 24) + (bgRed << 16) + (bgGreen << 8) + (bgBlue);
    }

    /**
	 * Rounds the value with the Math.round() function and casts the result to int.
	 * @param value
	 * @return
	 */
    public static int round(double value) {
        return (int) Math.round(value);
    }
}
