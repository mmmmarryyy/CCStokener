package tirtilcam.model;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

@SuppressWarnings("unchecked")
public class TirtilCamStreamer implements Runnable, IpCameraController {

    protected EventListenerList listenerList = new EventListenerList();

    public String m_mjpgURL = null;

    private String m_ctype = null;

    private String m_boundary = null;

    DataInputStream m_dis;

    private BufferedImage m_image = null;

    HttpURLConnection m_httpConn = null;

    int counter = 0;

    public Dimension imageSize = null;

    public boolean connected = false;

    private boolean initCompleted = false;

    Component parent;

    Utils utils = null;

    PrintStream ps = null;

    private Toolkit m_tk;

    private Vector m_listeners;

    private String m_instanceName;

    String m_imageBaseFolder = "resimler";

    File m_camFolder = null;

    public TirtilCamStreamer(String p_name, Component parent_, String p_url) throws FileNotFoundException {
        m_instanceName = p_name;
        parent = parent_;
        m_mjpgURL = p_url;
        m_tk = Toolkit.getDefaultToolkit();
        m_listeners = new Vector();
        ps = new PrintStream(new File("out.txt"));
        createCameraFolder(p_name);
        parent.setPreferredSize(new Dimension(320, 160));
        parent.setMinimumSize(new Dimension(320, 160));
        parent.setMaximumSize(new Dimension(320, 160));
        parent.setVisible(true);
    }

    public void run() {
        connect();
        readStream();
    }

    @SuppressWarnings("static-access")
    public void connect() {
        try {
            URL url = new URL(m_mjpgURL);
            m_httpConn = (HttpURLConnection) url.openConnection();
            @SuppressWarnings("unused") Map mp = m_httpConn.getHeaderFields();
            Hashtable headers = utils.readHttpHeaders(m_httpConn);
            for (Object key : headers.keySet()) {
                System.out.println(key.toString() + " | " + headers.get(key).toString());
            }
            InputStream is = m_httpConn.getInputStream();
            connected = true;
            BufferedInputStream bis = new BufferedInputStream(is);
            m_dis = new DataInputStream(bis);
            utils = new Utils(m_dis);
            m_ctype = (String) headers.get("content-type");
            int bidx = m_ctype.indexOf("boundary=");
            m_boundary = m_ctype.substring(bidx + 9);
            System.err.println(m_boundary);
            if (!initCompleted) initDisplay();
        } catch (Exception e) {
            System.err.println("Baglanti Problemi !");
            e.printStackTrace();
        }
    }

    public void initDisplay() throws IOException {
        System.out.println("INIIIIIT");
        readMJPGStream();
        imageSize = new Dimension(m_image.getWidth(parent), m_image.getHeight(parent));
        parent.setPreferredSize(imageSize);
        parent.setSize(imageSize);
        parent.validate();
        initCompleted = true;
    }

    public void disconnect() {
        try {
            if (connected) {
                m_dis.close();
                connected = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paint(Graphics g) {
        if (m_image != null) {
            BufferedImage resizedImage = Utils.resizeImage(m_image, parent.getWidth(), parent.getHeight());
            g.drawImage(resizedImage, 0, 0, parent);
        }
    }

    public void readStream() {
        try {
            while (true) {
                readMJPGStream();
                this.paint(parent.getGraphics());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readMJPGStream() throws IOException {
        utils.readHeaders();
        readJPG();
        ps.close();
    }

    public void readJPG() {
        try {
            updateImage();
            String s = counter + ".jpg";
            counter++;
            String filename = "resim_" + s;
            File f = new File(m_camFolder.getAbsoluteFile() + File.separator + filename);
            ImageIO.write(m_image, "jpeg", f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readLine(int n, DataInputStream dis) {
        for (int i = 0; i < n; i++) {
            readLine(dis);
        }
    }

    public void readLine(DataInputStream dis) {
        try {
            boolean end = false;
            String lineEnd = "\n";
            byte[] lineEndBytes = lineEnd.getBytes();
            byte[] byteBuf = new byte[lineEndBytes.length];
            while (!end) {
                dis.read(byteBuf, 0, lineEndBytes.length);
                String t = new String(byteBuf);
                ps.print(t);
                if (t.equals(lineEnd)) end = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Image getScaledInstanceAWT(BufferedImage source, double factor) {
        int w = (int) (source.getWidth() * factor);
        int h = (int) (source.getHeight() * factor);
        return source.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    public static BufferedImage toBufferedImage(Image image) {
        new ImageIcon(image);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D g = bimage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }

    public void addImageChangeListener(ImageChangeListener cl) {
        m_listeners.addElement(cl);
    }

    public void removeImageChangeListener(ImageChangeListener cl) {
        m_listeners.removeElement(cl);
    }

    @Override
    public void start() {
        new Thread(this).start();
    }

    @Override
    public void stop() {
    }

    /**
     * 
     * Update the image
     * 
     * @throws IOException
     * @throws ImageFormatException
     * 
     */
    private void updateImage() throws ImageFormatException, IOException {
        JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(m_dis);
        m_image = decoder.decodeAsBufferedImage();
        m_image.getWidth(new ImageObserver() {

            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                boolean fully = ((infoflags & (ImageObserver.ALLBITS | ImageObserver.PROPERTIES)) != 0);
                if (fully) {
                    fireImageChange();
                }
                return !fully;
            }
        });
    }

    private void fireImageChange() {
        ImageChangeEvent ce = new ImageChangeEvent(this);
        for (Enumeration e = m_listeners.elements(); e.hasMoreElements(); ) {
            ((ImageChangeListener) e.nextElement()).imageChanged(ce);
        }
    }

    private void createCameraFolder(String camName) {
        m_camFolder = new File(m_imageBaseFolder + File.separator + camName);
        if (m_camFolder.exists() == false) {
            m_camFolder.mkdirs();
        }
    }
}
