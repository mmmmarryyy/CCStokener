package com.weespers.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class NetUtil {

    public static URL createSafeURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static Image getRemoteImage(String url) {
        try {
            Image remote = new Image(Display.getCurrent(), NetUtil.createSafeURL(url).openStream());
            return resize(remote, 100, 100);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Image resize(Image image, int width, int height) {
        Image scaled = new Image(Display.getDefault(), width, height);
        GC gc = new GC(scaled);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
        gc.dispose();
        image.dispose();
        return scaled;
    }
}
