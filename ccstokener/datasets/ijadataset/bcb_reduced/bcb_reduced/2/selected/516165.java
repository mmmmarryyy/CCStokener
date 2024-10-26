package cn.sh.fang.chenance;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Splash implements MouseListener, MouseMoveListener {

    static Logger LOG = Logger.getLogger(Splash.class);

    private Display display;

    private Shell shell;

    private Boolean isRunning = false;

    private boolean isMoving;

    private Point point;

    public Splash(Display display) {
        this.display = display;
    }

    Image getSplashImage() {
        URL url = this.getClass().getClassLoader().getResource("splash");
        LOG.debug(url);
        ArrayList<String> l = new ArrayList<String>();
        if (url.getProtocol().startsWith("jar")) {
            JarURLConnection conn = null;
            JarFile jarfile = null;
            try {
                conn = (JarURLConnection) url.openConnection();
                jarfile = conn.getJarFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Enumeration<JarEntry> e = jarfile.entries();
            while (e.hasMoreElements()) {
                String j = e.nextElement().getName();
                if (j.startsWith("splash") && j.endsWith(".png")) {
                    l.add(j);
                }
            }
            return new Image(null, this.getClass().getClassLoader().getResourceAsStream(l.get(new Random(new Date().getTime()).nextInt(l.size()))));
        } else {
            File dir = null;
            try {
                dir = new File(url.toURI());
                LOG.debug(url.toURI());
            } catch (URISyntaxException e) {
            }
            CollectionUtils.addAll(l, dir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    LOG.debug(name);
                    return name.endsWith(".png");
                }
            }));
            return new Image(null, this.getClass().getClassLoader().getResourceAsStream("splash/" + l.get(new Random(new Date().getTime()).nextInt(l.size()))));
        }
    }

    private void init() {
        shell = new Shell(display, SWT.ON_TOP | SWT.NO_TRIM);
        final Image image = getSplashImage();
        Region region = new Region();
        Rectangle pixel = new Rectangle(0, 0, 1, 1);
        Rectangle bounds = image.getBounds();
        ImageData id = image.getImageData();
        int trans = id.transparentPixel;
        trans = 0xffffff;
        LOG.debug("transparentPixel: " + trans);
        LOG.debug("(0,0): " + id.getPixel(0, 0));
        for (int y = 0; y < bounds.height; y++) {
            for (int x = 0; x < bounds.width; x++) {
                int p = id.getPixel(x, y);
                if (p != trans) {
                    pixel.x = x;
                    pixel.y = y;
                    region.add(pixel);
                }
            }
        }
        shell.setRegion(region);
        shell.setSize(bounds.width, bounds.height);
        Rectangle size = region.getBounds();
        Rectangle b = display.getPrimaryMonitor().getBounds();
        LOG.debug(b);
        LOG.debug(size);
        shell.setLocation(b.x + (b.width - size.width) / 2, b.y + (b.height - size.height) / 2);
        shell.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent e) {
                e.gc.drawImage(image, 0, 0);
            }
        });
        shell.addMouseListener(this);
        shell.addMouseMoveListener(this);
    }

    public void close() {
        synchronized (isRunning) {
            if (isRunning) {
                isRunning = false;
                display.wake();
            }
        }
    }

    public void run(Runnable runnable) {
        isRunning = true;
        init();
        if (!isRunning) {
            return;
        }
        shell.setVisible(true);
        display.asyncExec(runnable);
        while (isRunning) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        shell.setVisible(false);
    }

    public static void main(String[] args) {
        Display display = new Display();
        final Splash s = new Splash(display);
        s.run(new Runnable() {

            public void run() {
                System.out.println("run()");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("run() end");
                s.close();
            }
        });
        display.dispose();
    }

    public void mouseDoubleClick(MouseEvent arg0) {
    }

    public void mouseDown(MouseEvent e) {
        isMoving = true;
        point = new Point(e.x, e.y);
    }

    public void mouseUp(MouseEvent e) {
        isMoving = false;
    }

    public void mouseMove(MouseEvent e) {
        if (isMoving) {
            Point p = shell.getLocation();
            shell.setLocation(p.x + e.x - point.x, p.y + e.y - point.y);
        }
    }
}
