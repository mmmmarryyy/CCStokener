package com.example.http;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.Timer;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class CbSaxHandler extends DefaultHandler {

    private String pano_id = null;

    private String[] pano_id_link;

    private int iLinks;

    private String pano_yaw_deg = null;

    private String tilt_yaw_deg = null;

    private String[] yaw_deg_link;

    private int iDegLinks;

    private String lat;

    private String lng;

    static final int MAX_LINK = 6;

    public String getPanoId() {
        return pano_id;
    }

    public String getPanoId(int iLink) {
        return pano_id_link[iLink];
    }

    public Double getYawDeg() {
        return Double.valueOf(pano_yaw_deg);
    }

    public Double getYawDeg(int iLink) {
        return Double.valueOf(yaw_deg_link[iLink]);
    }

    public Double getLat() {
        return Double.valueOf(lat);
    }

    public Double getLng() {
        return Double.valueOf(lng);
    }

    public void startDocument() {
        System.out.println("Document start");
        pano_id = null;
        pano_id_link = new String[MAX_LINK];
        iLinks = 0;
        for (int i = 0; i < MAX_LINK; i++) {
            pano_id_link[i] = null;
        }
        yaw_deg_link = new String[MAX_LINK];
        iDegLinks = 0;
        for (int i = 0; i < MAX_LINK; i++) {
            yaw_deg_link[i] = null;
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        System.out.println("Element start: " + qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            System.out.print("Attr: " + attributes.getQName(i));
            System.out.println(", " + attributes.getValue(i));
            if (pano_id == null && attributes.getQName(i) == "pano_id") {
                pano_id = attributes.getValue(i);
            } else if (pano_id != null && iLinks < MAX_LINK && attributes.getQName(i) == "pano_id") {
                pano_id_link[iLinks] = attributes.getValue(i);
                System.out.println("linked! [" + iLinks + "] from " + pano_id + " to " + pano_id_link[iLinks]);
                iLinks++;
            }
            if (attributes.getQName(i) == "pano_yaw_deg") {
                pano_yaw_deg = attributes.getValue(i);
            } else if (attributes.getQName(i) == "tilt_yaw_deg") {
                tilt_yaw_deg = attributes.getValue(i);
            } else if (pano_id != null && iDegLinks < 3 && attributes.getQName(i) == "yaw_deg") {
                yaw_deg_link[iDegLinks] = attributes.getValue(i);
                iDegLinks++;
            }
            if (attributes.getQName(i) == "lat") {
                lat = attributes.getValue(i);
            } else if (attributes.getQName(i) == "lng") {
                lng = attributes.getValue(i);
            }
        }
    }
}

public class StreetView extends JFrame implements ActionListener, IGpsDevice {

    static final int INTERVAL = 5000;

    Image img;

    static final int CAP_WIDTH = 640;

    static final int CAP_HEIGHT = 480;

    private static final int CONNECT_TIMEOUT = 1000;

    private static final int SOCKET_TIMEOUT = 10000;

    static final double DIVIDED_DEG = 52.0;

    static final int MAX_LINK = 6;

    JMenuBar menuBar;

    JMenu menuGo;

    JMenuItem menuGoStrait;

    JMenuItem menuGoRight;

    JMenuItem menuGoLeft;

    JCheckBoxMenuItem menuKeepGoing;

    boolean fKeepGoing = false;

    JMenu menuTurn;

    JMenuItem menuTurnRight;

    JMenuItem menuTurnLeft;

    JCheckBoxMenuItem menuKeepTurning;

    boolean fKeepTurning = false;

    Double mLat;

    Double mLng;

    String mPanoId, mPanoIdPrev, mPanoIdNext;

    int mXid, mXidPrev;

    String[] pano_id_link;

    Double mDir, mDirPrev;

    public StreetView() {
        menuBar = new JMenuBar();
        menuGo = new JMenu("Go");
        menuGoStrait = new JMenuItem("Go Ahead");
        menuGoRight = new JMenuItem("Go Other way");
        menuGoLeft = new JMenuItem("Go Back");
        menuKeepGoing = new JCheckBoxMenuItem("Keep Going");
        menuGoStrait.addActionListener(this);
        menuGoRight.addActionListener(this);
        menuGoLeft.addActionListener(this);
        menuKeepGoing.addActionListener(this);
        menuBar.add(menuGo);
        menuGo.add(menuGoStrait);
        menuGo.add(menuGoRight);
        menuGo.add(menuGoLeft);
        menuGo.add(menuKeepGoing);
        menuTurn = new JMenu("Turn");
        menuTurnRight = new JMenuItem("Turn Right");
        menuTurnLeft = new JMenuItem("Turn Left");
        menuKeepTurning = new JCheckBoxMenuItem("Keep Turning");
        menuTurnRight.addActionListener(this);
        menuTurnLeft.addActionListener(this);
        menuKeepTurning.addActionListener(this);
        menuBar.add(menuTurn);
        menuTurn.add(menuTurnRight);
        menuTurn.add(menuTurnLeft);
        menuTurn.add(menuKeepTurning);
        getRootPane().setJMenuBar(menuBar);
        img = null;
        mLat = 35.685727;
        mLng = 139.76121;
        mPanoId = mPanoIdPrev = mPanoIdNext = null;
        mXid = mXidPrev = 0;
        pano_id_link = new String[MAX_LINK + 1];
        for (int i = 0; i < MAX_LINK + 1; i++) {
            pano_id_link[i] = null;
        }
        mDir = 195.84 + DIVIDED_DEG * (mXid - 3);
        if (mDir < 0.0) {
            mDir += 360.0;
        } else if (mDir > 360.0) {
            mDir -= 360.0;
        }
        mDirPrev = mDir;
        new Timer(INTERVAL, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                repaint();
                if (img == null || !mPanoId.equals(mPanoIdNext) || mXidPrev != mXid) {
                    try {
                        grab(mLat, mLng, mPanoIdNext, mXid);
                        if (fKeepGoing) {
                            mXid = -1;
                            mPanoIdNext = pano_id_link[0];
                        } else if (fKeepTurning) {
                            if (mXid < 0) {
                                mXid = mXidPrev;
                            }
                            mXid++;
                            if (mXid > 6) mXid = 0;
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();
    }

    synchronized void grab(Double lat, Double lng, String pano_id, int xid) throws Exception {
        System.out.println("grabbing...");
        InputStream in = null;
        int response = -1;
        boolean fTurning = xid < 0 ? false : true;
        try {
            URL url;
            if (pano_id == null) {
                url = new URL("http://maps.google.co.jp/cbk?output=xml&cb_client=maps_sv&ll=" + lat + "%2C" + lng);
            } else {
                url = new URL("http://cbk0.google.com/cbk?output=xml&panoid=" + pano_id);
            }
            URLConnection conn = url.openConnection();
            if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection.");
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setConnectTimeout(CONNECT_TIMEOUT);
            httpConn.setReadTimeout(SOCKET_TIMEOUT);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
                try {
                    SAXParserFactory spfactory = SAXParserFactory.newInstance();
                    SAXParser parser = spfactory.newSAXParser();
                    CbSaxHandler cbsh = new CbSaxHandler();
                    parser.parse(in, cbsh);
                    System.out.println("------------>" + cbsh.getPanoId());
                    pano_id = cbsh.getPanoId();
                    for (int i = 0; i < MAX_LINK; i++) {
                        pano_id_link[i] = cbsh.getPanoId(i);
                    }
                    mLat = cbsh.getLat();
                    mLng = cbsh.getLng();
                    if (xid < 0) {
                        double YaYa;
                        double Ya = cbsh.getYawDeg();
                        mXid = xid = mXidPrev;
                        for (int i = 0; i < 7; i++) {
                            YaYa = Ya + DIVIDED_DEG * (i - 3) - DIVIDED_DEG / 2;
                            if (YaYa < 0.0) {
                                YaYa += 360.0;
                            } else if (YaYa > 360.0) {
                                YaYa -= 360.0;
                            }
                            if (YaYa <= mDirPrev && mDirPrev <= (YaYa + DIVIDED_DEG)) {
                                mXid = xid = i;
                                break;
                            }
                        }
                    }
                    mDir = cbsh.getYawDeg() + DIVIDED_DEG * (mXid - 3);
                    if (mDir < 0.0) {
                        mDir += 360.0;
                    } else if (mDir > 360.0) {
                        mDir -= 360.0;
                    }
                    mDirPrev = mDir;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
        if (pano_id != null) {
            try {
                in = null;
                response = -1;
                URL url = new URL("http://cbk0.google.com/cbk?output=tile&panoid=" + pano_id + "&zoom=3&x=" + xid + "&y=1");
                URLConnection conn = url.openConnection();
                if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection.");
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                httpConn.setAllowUserInteraction(false);
                httpConn.setConnectTimeout(CONNECT_TIMEOUT);
                httpConn.setReadTimeout(SOCKET_TIMEOUT);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod("GET");
                httpConn.connect();
                response = httpConn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    in = httpConn.getInputStream();
                    File file = new File("cap.jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    System.out.println("saving...");
                    for (int i = 0; ; ) {
                        i = in.read();
                        if (i == -1) break;
                        fos.write((char) i);
                    }
                    fos.close();
                    img = loadImage("cap.jpg");
                    if (!fTurning) {
                        mPanoIdPrev = mPanoId;
                    }
                    System.out.println("Prev:" + mPanoIdPrev + "(" + mXidPrev + ")");
                    mPanoIdNext = mPanoId = pano_id;
                    mXidPrev = mXid = xid;
                    fixLinkages();
                    System.out.println("Next:" + mPanoIdNext + "(" + mXid + ")");
                    System.out.println("LinkTo:" + pano_id_link[0] + "," + pano_id_link[1] + "," + pano_id_link[2]);
                }
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void fixLinkages() {
        int i, j;
        for (i = 0; i < MAX_LINK && pano_id_link[i] != null; i++) {
            if (pano_id_link[i].equals(mPanoIdPrev)) {
                for (j = i; j < MAX_LINK; j++) {
                    pano_id_link[j] = pano_id_link[j + 1];
                }
                break;
            }
        }
        if (pano_id_link[0] == null) {
            pano_id_link[0] = mPanoIdPrev;
        }
        if (pano_id_link[1] == null) {
            pano_id_link[1] = pano_id_link[0];
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Go Ahead")) {
            mXid = -1;
            mPanoIdNext = pano_id_link[0];
        } else if (e.getActionCommand().equals("Go Other way")) {
            mXid = -1;
            mPanoIdNext = pano_id_link[1];
        } else if (e.getActionCommand().equals("Go Back")) {
            mXid = -1;
            mPanoIdNext = mPanoIdPrev;
        } else if (e.getActionCommand().equals("Keep Going")) {
            fKeepGoing = !fKeepGoing;
            if (fKeepGoing) {
                mXid = -1;
                mPanoIdNext = pano_id_link[0];
            }
        } else if (e.getActionCommand().equals("Turn Right")) {
            if (mXid < 0) {
                mXid = mXidPrev;
            }
            mXid++;
            if (mXid > 6) mXid = 0;
        } else if (e.getActionCommand().equals("Turn Left")) {
            if (mXid < 0) {
                mXid = mXidPrev;
            }
            mXid--;
            if (mXid < 0) mXid = 6;
        } else if (e.getActionCommand().equals("Keep Turning")) {
            fKeepTurning = !fKeepTurning;
            if (fKeepTurning) {
                if (mXid < 0) {
                    mXid = mXidPrev;
                }
                mXid++;
                if (mXid > 6) mXid = 0;
            }
        }
        System.out.println("[" + e.getActionCommand() + "]");
    }

    public static Image loadImage(String file_name) {
        InputStream is = null;
        try {
            is = new FileInputStream(file_name);
            BufferedImage img = ImageIO.read(is);
            return img;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        if (img == null) {
            return;
        }
        g.drawImage(img, 0, 0, CAP_WIDTH, CAP_HEIGHT, null);
    }

    public synchronized void saveImage() {
    }

    @Override
    public Double getDir() {
        return mDir;
    }

    @Override
    public Double getLat() {
        return mLat;
    }

    @Override
    public Double getLng() {
        return mLng;
    }
}
