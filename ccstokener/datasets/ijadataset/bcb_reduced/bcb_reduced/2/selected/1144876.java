package org.xith3d.utility.view;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import org.xith3d.scenegraph.TransformNode;

/**
 * This class can be used to replay a pre-recorded camera flight.
 * 
 * @see CameraFlightRecorder
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class CameraFlight {

    public enum Format {

        UNCOMPRESSED((byte) 0), COMPRESSED((byte) 1);

        private byte b;

        public byte getByte() {
            return (b);
        }

        private Format(byte b) {
            this.b = b;
        }

        public static Format get(byte b) {
            switch(b) {
                case 0:
                    return (Format.UNCOMPRESSED);
                case 1:
                    return (Format.COMPRESSED);
                default:
                    throw (new IllegalArgumentException("Unknown type " + b));
            }
        }
    }

    private class InterpolationPoint {

        public Matrix3f rot;

        public Vector3f pos;

        public float deltaTime;
    }

    private List<InterpolationPoint> interPoints;

    private long startTime;

    private Matrix3f camRot;

    private Vector3f camPos;

    private float t0i, t0t;

    private float d;

    private int i1;

    private int frames;

    private InterpolationPoint ip1, ip2;

    private List<CameraFlightListener> listeners = new ArrayList<CameraFlightListener>(1);

    /**
     * Adds a new CameraFlightListener to the list
     * 
     * @param l the new listener to be added
     */
    public void addCameraFlightListener(CameraFlightListener l) {
        listeners.add(l);
    }

    /**
     * Remvoes a CameraFlightListener from the list
     * 
     * @param l the listener to be removed
     */
    public void removeCameraFlightListener(CameraFlightListener l) {
        listeners.remove(l);
    }

    /**
     * Interpolates View rotation and position.
     * 
     * @param cam the View to be updated
     * @param gameTime the current game time
     */
    public void updateCamera(TransformNode cam, long gameTime) {
        t0t = (float) (gameTime - startTime);
        ip1 = interPoints.get(i1);
        while (t0t > (t0i + ip1.deltaTime)) {
            t0i += ip1.deltaTime;
            if (i1 + 2 >= interPoints.size()) {
                restart(gameTime);
            }
            ip1 = interPoints.get(++i1);
        }
        ip2 = interPoints.get(i1 + 1);
        if (ip1.deltaTime > 0.0f) d = (t0t - t0i) / ip1.deltaTime; else d = 0.0f;
        camRot.m00 = (ip1.rot.m00 + ((ip2.rot.m00 - ip1.rot.m00) * d));
        camRot.m01 = (ip1.rot.m01 + ((ip2.rot.m01 - ip1.rot.m01) * d));
        camRot.m02 = (ip1.rot.m02 + ((ip2.rot.m02 - ip1.rot.m02) * d));
        camRot.m10 = (ip1.rot.m10 + ((ip2.rot.m10 - ip1.rot.m10) * d));
        camRot.m11 = (ip1.rot.m11 + ((ip2.rot.m11 - ip1.rot.m11) * d));
        camRot.m12 = (ip1.rot.m12 + ((ip2.rot.m12 - ip1.rot.m12) * d));
        camRot.m20 = (ip1.rot.m20 + ((ip2.rot.m20 - ip1.rot.m20) * d));
        camRot.m21 = (ip1.rot.m21 + ((ip2.rot.m21 - ip1.rot.m21) * d));
        camRot.m22 = (ip1.rot.m22 + ((ip2.rot.m22 - ip1.rot.m22) * d));
        camPos.x = ip1.pos.x + ((ip2.pos.x - ip1.pos.x) * d);
        camPos.y = ip1.pos.y + ((ip2.pos.y - ip1.pos.y) * d);
        camPos.z = ip1.pos.z + ((ip2.pos.z - ip1.pos.z) * d);
        cam.getTransform().set(camRot);
        cam.getTransform().setTranslation(camPos);
        frames++;
    }

    public void restart(long gameTime) {
        double averageFPS = ((double) frames / (double) t0t * 1000.0);
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onCameraFlightEnded(frames, (long) t0t, averageFPS);
        startTime = gameTime;
        t0i = 0.0f;
        t0t = 0.0f;
        i1 = -1;
        frames = 0;
    }

    public void start(long startTime) {
        this.camRot = new Matrix3f();
        this.camPos = new Vector3f();
        this.startTime = startTime;
        this.t0i = 0.0f;
        this.i1 = 0;
        this.frames = 0;
    }

    /**
     * Adds a camera-transformation-matrix to the list.
     * 
     * @param rot
     * @param pos
     * @param deltaTime
     */
    public void addRotPos(Matrix3f rot, Vector3f pos, float deltaTime) {
        InterpolationPoint interPoint = new InterpolationPoint();
        interPoint.rot = new Matrix3f(rot);
        interPoint.pos = new Vector3f(pos);
        interPoint.deltaTime = deltaTime;
        interPoints.add(interPoint);
    }

    private String readLine(InputStream in) throws IOException {
        StringBuffer str = new StringBuffer();
        char c;
        final char EOL = '\n';
        while ((in.available() > 0) && ((c = (char) in.read()) != EOL)) str.append(c);
        return (str.toString());
    }

    /**
     * Loads the CameraFlight from the specified InputStream.
     * 
     * @param in the InputStream to load from
     * 
     * @throws IOException
     */
    public void load(InputStream in) throws IOException {
        interPoints = new ArrayList<InterpolationPoint>();
        Matrix3f rot = new Matrix3f();
        Vector3f pos = new Vector3f();
        float t;
        Format format = Format.get(Byte.valueOf(readLine(in)));
        readLine(in);
        if (format == Format.COMPRESSED) {
            if (in instanceof BufferedInputStream) throw (new IllegalArgumentException("The InputStream must not be a BufferedInputStream, if read from a COMPRESSED file."));
            in = new InflaterInputStream(in);
        }
        if (!(in instanceof BufferedInputStream)) in = new BufferedInputStream(in);
        String line;
        String[] comps;
        while (true) {
            line = readLine(in);
            if ((line == null) || (line.length() == 0) || (Character.getType(line.charAt(0)) == 0)) break;
            comps = line.split(" ");
            rot.m00 = Float.parseFloat(comps[0]);
            rot.m01 = Float.parseFloat(comps[1]);
            rot.m02 = Float.parseFloat(comps[2]);
            line = readLine(in);
            comps = line.split(" ");
            rot.m10 = Float.parseFloat(comps[0]);
            rot.m11 = Float.parseFloat(comps[1]);
            rot.m12 = Float.parseFloat(comps[2]);
            line = readLine(in);
            comps = line.split(" ");
            rot.m20 = Float.parseFloat(comps[0]);
            rot.m21 = Float.parseFloat(comps[1]);
            rot.m22 = Float.parseFloat(comps[2]);
            line = readLine(in);
            comps = line.split(" ");
            pos.x = Float.parseFloat(comps[0]);
            pos.y = Float.parseFloat(comps[1]);
            pos.z = Float.parseFloat(comps[2]);
            line = readLine(in);
            t = (float) (Long.parseLong(line));
            line = readLine(in);
            addRotPos(rot, pos, t);
        }
    }

    /**
     * Loads the CameraFlight from the specified URL.<br>
     * If read from a URL, the resource must not be a compressed flight file.
     * 
     * @param url the URL to load from
     * 
     * @throws IOException
     */
    public void load(URL url) throws IOException {
        load(url.openStream());
    }

    /**
     * Loads the CameraFlight from the specified File.
     * 
     * @param file the File to load from
     * 
     * @throws IOException
     */
    public void load(File file) throws IOException {
        load(new FileInputStream(file));
    }

    /**
     * Loads the CameraFlight from the specified file.
     * 
     * @param filename the file to load from
     * 
     * @throws IOException
     */
    public void load(String filename) throws IOException {
        load(new File(filename));
    }

    /**
     * Creates a new CameraFlight
     */
    public CameraFlight() {
    }

    /**
     * Creates a new CameraFlight and loads data from the given InputStream.
     */
    public CameraFlight(InputStream in) throws IOException {
        this();
        load(in);
    }

    /**
     * Creates a new CameraFlight and loads data from the given URL.
     * If read from a URL, the resource must not be a compressed flight file.
     */
    public CameraFlight(URL url) throws IOException {
        this();
        load(url);
    }

    /**
     * Creates a new CameraFlight and loads data from the given file.
     */
    public CameraFlight(File file) throws IOException {
        this();
        load(file);
    }

    /**
     * Creates a new CameraFlight and loads data from the given file.
     */
    public CameraFlight(String filename) throws IOException {
        this();
        load(filename);
    }
}
