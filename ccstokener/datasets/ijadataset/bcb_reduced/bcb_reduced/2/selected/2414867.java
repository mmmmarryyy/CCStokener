package com.sderhy;

import BMPReader.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import tools.Tools;

public class OpenOther {

    public static int numImage = 0;

    public static boolean openStringURL(String StringURL, MainClass mc) {
        java.net.URL url;
        try {
            url = new java.net.URL(StringURL);
        } catch (MalformedURLException m) {
            Tools.debug(OpenOther.class, m.toString());
            return false;
        }
        return OpenGif.fromURL(mc, url);
    }

    public static boolean fromURL(URL url, MainClass mc) {
        TextField TF = mc.TF;
        PixCanvas canvas = mc.canvas;
        Image image = null;
        try {
            PPM.PPMDecoder ppm = new PPM.PPMDecoder();
            image = ppm.getImage(url);
        } catch (IOException e) {
            return false;
        }
        if (image == null) {
            TF.setText("Error not a typical image PPM, PGM or PBM format");
            return false;
        }
        MediaTracker tr = new MediaTracker(canvas);
        tr.addImage(image, 0);
        try {
            tr.waitForID(0);
        } catch (InterruptedException e) {
        }
        ;
        if (tr.isErrorID(0)) {
            Tools.debug(OpenOther.class, "Tracker error " + tr.getErrorsAny().toString());
            return false;
        }
        PixObject po = new PixObject(url, image, canvas, false, null);
        mc.vimages.addElement(po);
        TF.setText(url.toString());
        canvas.repaint();
        return true;
    }

    public static boolean BMPfromURL(URL url, MainClass mc) {
        TextField TF = mc.TF;
        PixCanvas canvas = mc.canvas;
        Image image = null;
        try {
            image = Toolkit.getDefaultToolkit().createImage(BMPReader.getBMPImage(url.openStream()));
        } catch (IOException e) {
            return false;
        }
        if (image == null) {
            TF.setText("Error not a typical image BMP format");
            return false;
        }
        MediaTracker tr = new MediaTracker(canvas);
        tr.addImage(image, 0);
        try {
            tr.waitForID(0);
        } catch (InterruptedException e) {
        }
        ;
        if (tr.isErrorID(0)) {
            Tools.debug(OpenOther.class, "Tracker error " + tr.getErrorsAny().toString());
            return false;
        }
        PixObject po = new PixObject(url, image, canvas, false, null);
        mc.vimages.addElement(po);
        TF.setText(url.toString());
        canvas.repaint();
        return true;
    }
}
