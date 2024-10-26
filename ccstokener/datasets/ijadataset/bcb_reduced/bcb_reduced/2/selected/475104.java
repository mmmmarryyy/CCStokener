package j3dworkbench.tagsensor.j3dextensions;

import j3dworkbench.core.J3DWorkbenchUtility;
import j3dworkbench.event.MessageDispatcher;
import j3dworkbench.tagsensor.proxy.SensorTransformProxy;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.media.j3d.Behavior;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import com.sun.j3d.utils.scenegraph.io.SceneGraphIO;
import com.sun.j3d.utils.scenegraph.io.SceneGraphObjectReferenceControl;

public class SensorTransformBehavior extends Behavior implements SceneGraphIO {

    private long interval = 50;

    private Tuple3d offset = new Point3d();

    private WakeupOnElapsedFrames wake = new WakeupOnElapsedFrames(0);

    private final Vector3d vecNewPosition = new Vector3d();

    private final Vector3d vecCurrentPos = new Vector3d();

    private final Vector3d vecInertia = new Vector3d();

    private final Transform3D transform = new Transform3D();

    private final Transform3D transTemp = new Transform3D();

    private final List<Vector3d> positions = new ArrayList<Vector3d>(5);

    private String tagID = "";

    private boolean trackY = false;

    private boolean faceCenter = true;

    public boolean isTrackY() {
        return trackY;
    }

    public void setTrackY(boolean p_trackY) {
        this.trackY = p_trackY;
        if (!trackY) {
            offset.y = 0;
        }
    }

    private String trackingURL = "http://localhost/";

    public static void main(String[] args) {
        SensorTransformBehavior sensor = new SensorTransformBehavior();
        sensor.initialize();
    }

    public SensorTransformBehavior() {
        super();
        this.setEnable(false);
    }

    /**
	 * Implement listener for outside stimulus
	 */
    @Override
    public void initialize() {
        try {
            factory.setValidating(false);
            factory.setXIncludeAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        bproxy = (SensorTransformProxy) J3DWorkbenchUtility.findProxyNode(this);
        wakeupOn(wake);
    }

    static final int VERSION = 1;

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder builder;

    Document doc;

    private float scale = 0.1f;

    private final Vector3d objScale = new Vector3d();

    private final Vector3d center = new Vector3d();

    private float threshhold = .2f;

    private int cyclesSinceMoved = -1;

    private transient SensorTransformProxy bproxy;

    private TransformGroup targetTG;

    private boolean filter;

    private int cyclesBeforeFaceFront = 3;

    private final List<Vector3d> values = new ArrayList<Vector3d>(4);

    private int numValuesForAverage = 4;

    private void debugStream(InputStream input) {
        int i = -1;
        try {
            i = input.read();
            while (i > -1) {
                System.out.println((char) i);
                i = input.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initPolling() {
        Thread poll = new Thread(new Runnable() {

            public void run() {
                InputStream input = null;
                URL url = null;
                HttpURLConnection conn = null;
                try {
                    url = new URL(trackingURL + tagID);
                } catch (MalformedURLException e) {
                    SensorTransformBehavior.this.setEnable(false);
                    e.printStackTrace();
                    return;
                }
                MessageDispatcher.getInstance().notifyStatus(bproxy.getName() + " is connecting to " + url);
                while (SensorTransformBehavior.this.getEnable()) {
                    try {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.connect();
                        input = (InputStream) conn.getContent();
                        parseCoords(input);
                        Thread.sleep(interval);
                    } catch (Exception e) {
                        conn.disconnect();
                        positions.clear();
                        SensorTransformBehavior.this.setEnable(false);
                        MessageDispatcher.getInstance().notifyError(bproxy.getName() + " reports " + e.toString());
                        return;
                    }
                }
            }
        });
        poll.start();
    }

    @Override
    public void setEnable(boolean enable) {
        if (getEnable() == enable) {
            return;
        }
        if (enable) {
            if (!isLive()) {
                return;
            }
            targetTG = (TransformGroup) getParent();
            targetTG.getTransform(this.transTemp);
            transTemp.get(vecNewPosition);
            center.set(bproxy.getLocalGeometricCenter());
            positions.clear();
            values.clear();
            super.setEnable(true);
            initPolling();
        } else {
            super.setEnable(false);
            targetTG = null;
        }
    }

    private void parseCoords(InputStream input) {
        try {
            doc = builder.parse(input);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element element = doc.getDocumentElement();
        element = (Element) element.getElementsByTagName("requestData").item(0);
        element = (Element) element.getElementsByTagName("tag").item(0);
        NamedNodeMap map = element.getAttributes();
        Vector3d v3d = new Vector3d();
        v3d.x = Float.parseFloat(map.getNamedItem("X").getNodeValue());
        if (trackY) {
            v3d.y = Float.parseFloat(map.getNamedItem("Z").getNodeValue());
        } else {
            v3d.y = 0;
        }
        v3d.z = Float.parseFloat(map.getNamedItem("Y").getNodeValue());
        if (v3d.x > 1000) {
            return;
        }
        v3d.scale(scale);
        v3d.add(offset);
        positions.add(v3d);
        builder.reset();
    }

    @Override
    public void processStimulus(@SuppressWarnings("rawtypes") Enumeration en) {
        if (positions.isEmpty()) {
            wakeupOn(wake);
            return;
        }
        double angleInertia = 0;
        targetTG.getTransform(transform);
        transform.get(vecCurrentPos);
        assert vecCurrentPos.equals(vecNewPosition);
        transform.getScale(objScale);
        if (filter) {
            vecNewPosition.set(getNextAvgPosition());
        } else {
            vecNewPosition.set(getNextPosition());
        }
        vecInertia.sub(vecNewPosition, vecCurrentPos);
        angleInertia = Math.atan2(-vecInertia.x, -vecInertia.z);
        if (Double.isNaN(angleInertia)) {
            System.out.println(this + " anomaly: angle is NaN");
            angleInertia = 0;
        }
        final boolean moved = (!isMotionFilter() && vecInertia.length() > 0.001) || (isMotionFilter() && vecInertia.length() > threshhold);
        if (!moved) {
            cyclesSinceMoved++;
        } else {
            cyclesSinceMoved = 0;
        }
        if (!moved) {
            if (filter) {
                if (cyclesSinceMoved < cyclesBeforeFaceFront || cyclesSinceMoved > cyclesBeforeFaceFront) {
                    wakeupOn(wake);
                    return;
                }
                vecNewPosition.set(vecCurrentPos);
            }
            if (faceCenter) {
                angleInertia = Math.atan2(vecNewPosition.x, vecNewPosition.z);
                if (Double.isNaN(angleInertia)) {
                    wakeupOn(wake);
                    return;
                }
            }
        }
        transform.set(vecNewPosition);
        transform.setScale(objScale);
        transTemp.rotY(angleInertia);
        transform.mul(transTemp);
        targetTG.setTransform(transform);
        wakeupOn(wake);
    }

    private Vector3d getNextPosition() {
        return positions.remove(0);
    }

    private Vector3d getNextAvgPosition() {
        Vector3d result = new Vector3d();
        Vector3d next = positions.remove(0);
        values.add(next);
        if (values.size() > numValuesForAverage) {
            values.remove(0);
        }
        assert values.size() <= numValuesForAverage;
        for (int i = 0; i < values.size(); i++) {
            result.add(values.get(i));
        }
        result.scale(1d / values.size());
        return result;
    }

    public String getTagID() {
        return tagID;
    }

    public void setTagID(String tagID) {
        this.tagID = tagID;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void createSceneGraphObjectReferences(SceneGraphObjectReferenceControl arg0) {
    }

    public void readSceneGraphObject(DataInput in) throws IOException {
        in.readUnsignedByte();
        interval = in.readLong();
        filter = in.readBoolean();
        threshhold = in.readFloat();
        offset.x = in.readFloat();
        offset.y = in.readFloat();
        offset.z = in.readFloat();
        numValuesForAverage = in.readInt();
        trackingURL = in.readUTF();
        tagID = in.readUTF();
        trackY = in.readBoolean();
    }

    public void restoreSceneGraphObjectReferences(SceneGraphObjectReferenceControl arg0) {
    }

    public boolean saveChildren() {
        return false;
    }

    /**
	 * ATTENTION: must write end-of-line character after writeBytes(), if not
	 * last field saved.
	 */
    public void writeSceneGraphObject(DataOutput out) throws IOException {
        out.write(VERSION);
        out.writeLong(interval);
        out.writeBoolean(filter);
        out.writeFloat(threshhold);
        out.writeFloat((float) offset.x);
        out.writeFloat((float) offset.y);
        out.writeFloat((float) offset.z);
        out.writeInt(numValuesForAverage);
        out.writeUTF(trackingURL);
        out.writeUTF(tagID);
        out.writeBoolean(trackY);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public String getTrackURL() {
        return trackingURL;
    }

    public void setTrackURL(String track_url) {
        trackingURL = track_url;
    }

    public float getThreshhold() {
        return threshhold;
    }

    public void setThreshhold(float threshhold) {
        this.threshhold = threshhold;
    }

    public boolean isMotionFilter() {
        return filter;
    }

    public void setMotionFilter(boolean f) {
        this.filter = f;
    }

    public void setOffset(Tuple3d poffset) {
        offset.set(poffset);
    }

    public Tuple3d getOffset() {
        return offset;
    }

    public int getNumValuesForAverage() {
        return numValuesForAverage;
    }

    public void setNumValuesForAverage(int numValuesForAverage) {
        this.numValuesForAverage = numValuesForAverage;
    }
}
