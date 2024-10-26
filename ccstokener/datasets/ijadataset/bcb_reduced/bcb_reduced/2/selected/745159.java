package com.peterhi.ui;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import com.peterhi.Util;

/**
 * @author ������
 *
 */
public class ToolKit {

    private static Map<URL, Image[]> imageMap = new HashMap<URL, Image[]>();

    public static void dispose(Widget widget, final Resource resource) {
        widget.addListener(SWT.Dispose, new Listener() {

            @Override
            public void handleEvent(Event e) {
                dispose(resource);
            }
        });
    }

    public static void dispose(Object[] resource) {
        if (!Util.empty(resource)) {
            for (Object item : resource) {
                dispose(item);
            }
        }
    }

    public static void dispose(Object resource) {
        if (resource == null) {
            return;
        }
        try {
            Method method = resource.getClass().getMethod("isDisposed");
            if (!(Boolean) method.invoke(resource)) {
                method = resource.getClass().getMethod("dispose");
                method.invoke(resource);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Image image(String text, int direction) {
        GC measureGC = new GC(Display.getCurrent());
        Point textSize = measureGC.stringExtent(text);
        dispose(measureGC);
        Image image = new Image(Display.getCurrent(), textSize.x, textSize.y);
        GC paintGC = new GC(image);
        paintGC.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        paintGC.fillRectangle(0, 0, textSize.x, textSize.y);
        paintGC.drawString(text, 0, 0);
        dispose(paintGC);
        if (direction == SWT.NONE) {
            return image;
        } else {
            ImageData srcData = image.getImageData();
            ImageData dstData = new ImageData(srcData.height, srcData.width, srcData.depth, srcData.palette);
            for (int y = 0; y < srcData.height; y++) {
                for (int x = 0; x < srcData.width; x++) {
                    int newX = direction == SWT.LEFT ? y : srcData.height - y - 1;
                    int newY = direction == SWT.LEFT ? srcData.width - x - 1 : x;
                    dstData.setPixel(newX, newY, srcData.getPixel(x, y));
                }
            }
            dispose(image);
            return new Image(Display.getCurrent(), dstData);
        }
    }

    public static Image image(String title, Image icon, int gap, int direction) {
        if (Util.empty(title)) {
            return new Image(Display.getCurrent(), icon, SWT.IMAGE_COPY);
        }
        Image textImage = image(title, direction);
        if (icon == null) {
            PaletteData palette = new PaletteData(0xff, 0xff00, 0xff0000);
            ImageData srcData = textImage.getImageData();
            ImageData data = new ImageData(srcData.width, srcData.height, srcData.depth, palette);
            Color transparentColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            data.transparentPixel = palette.getPixel(transparentColor.getRGB());
            Image image = new Image(Display.getCurrent(), data);
            GC gc = new GC(image);
            gc.setBackground(transparentColor);
            gc.fillRectangle(image.getBounds());
            gc.drawImage(textImage, 0, 0);
            dispose(gc);
            dispose(textImage);
            return image;
        }
        ImageData data;
        int depth = Display.getCurrent().getDepth();
        PaletteData palette = new PaletteData(0xff, 0xff00, 0xff0000);
        if (direction == SWT.NONE) {
            data = new ImageData(textImage.getBounds().width + gap + icon.getBounds().width, Math.max(textImage.getBounds().height, icon.getBounds().height), depth, palette);
        } else {
            data = new ImageData(Math.max(textImage.getBounds().width, icon.getBounds().width), textImage.getBounds().height + gap + icon.getBounds().height, depth, palette);
        }
        Color transparentColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        data.transparentPixel = palette.getPixel(transparentColor.getRGB());
        Image image = new Image(Display.getCurrent(), data);
        GC gc = new GC(image);
        gc.setBackground(transparentColor);
        gc.fillRectangle(image.getBounds());
        if (direction == SWT.NONE) {
            gc.drawImage(icon, 0, (image.getBounds().height - icon.getBounds().height) / 2);
            gc.drawImage(textImage, icon.getBounds().width + gap, (icon.getBounds().height - icon.getBounds().height) / 2);
        } else {
            gc.drawImage(icon, (image.getBounds().width - icon.getBounds().width) / 2, 0);
            gc.drawImage(textImage, (icon.getBounds().width - icon.getBounds().width) / 2, icon.getBounds().height + gap);
        }
        dispose(gc);
        dispose(textImage);
        return image;
    }

    public static Image image(URL url) throws IOException {
        Image[] images = imageMap.get(url);
        if (Util.empty(images)) {
            images = new Image[1];
            images[0] = new Image(Display.getCurrent(), url.openStream());
            imageMap.put(url, images);
        }
        return images[0];
    }

    public static Image image(String resource, String key) {
        try {
            return image(ToolKit.class.getResource(resource + key + ".png"));
        } catch (Exception ex) {
            try {
                return image(ToolKit.class.getResource("/com/peterhi/resource/missing_image.png"));
            } catch (RuntimeException innerEx) {
                throw innerEx;
            } catch (Exception innerEx) {
                throw new RuntimeException(innerEx);
            }
        }
    }

    public static Image image(String key) {
        return image("/com/peterhi/resource/", key);
    }

    public static String string(String key) {
        return Util.string("com/peterhi/resource/lang", key);
    }

    public static void event(Widget widget, int type, final Object target, final String method) {
        widget.addListener(type, new Listener() {

            @Override
            public void handleEvent(Event e) {
                Util.call(target, method, e);
            }
        });
    }
}
