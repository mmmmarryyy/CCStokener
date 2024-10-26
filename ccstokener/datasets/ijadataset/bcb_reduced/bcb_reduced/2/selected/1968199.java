package com.peterhix.net.client.launcher;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SplashScreen {

    private static final SplashScreen instance = new SplashScreen();

    public static SplashScreen getInstance() {
        return instance;
    }

    private static final Rectangle STATUSTEXT_BOUNDS = new Rectangle(253, 132, 158, 28);

    private static final Rectangle PROGRESS_BOUNDS = new Rectangle(STATUSTEXT_BOUNDS.x + 10, STATUSTEXT_BOUNDS.y + STATUSTEXT_BOUNDS.height + 2, STATUSTEXT_BOUNDS.width - 20, 14);

    private static final String DEFAULT_FAMILY_NAME = "Dialog";

    private static final int DEFAULT_SIZE = 12;

    private static final String PROGRESS_FAMILY_NAME = "Dialog";

    private static final int PROGRESS_SIZE = 10;

    private long total = 0;

    private long size = 0;

    private java.awt.SplashScreen splashScreen;

    private Graphics2D g;

    private Set<AbstractRunnable> eventQueue;

    private Object monitor = new Object();

    private boolean disposed;

    private Runner runner;

    protected SplashScreen() {
        splashScreen = java.awt.SplashScreen.getSplashScreen();
        try {
            splashScreen.setImageURL(new URL("http://www.peterhi.com/app/webstart/launch-peterhi.jpg"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        eventQueue = new HashSet<AbstractRunnable>();
        g = splashScreen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        g.setComposite(AlphaComposite.Clear);
        g.setPaintMode();
        runner = new Runner();
        runner.start();
    }

    public Graphics2D getGraphics() {
        if (isRunnerThread()) {
            return g;
        } else {
            return null;
        }
    }

    public boolean isRunnerThread() {
        return (Thread.currentThread() == runner);
    }

    public void setStatusText(String text) {
        genericDrawText(text, STATUSTEXT_BOUNDS.x, STATUSTEXT_BOUNDS.y, STATUSTEXT_BOUNDS.width, STATUSTEXT_BOUNDS.height);
    }

    public void setProgress(String text, float percent) {
        if (percent > 1) {
            percent = 1;
        }
        genericDrawProgress(text, PROGRESS_BOUNDS.x, PROGRESS_BOUNDS.y, PROGRESS_BOUNDS.width, PROGRESS_BOUNDS.height, percent, true);
    }

    public void setProgress(int percent) {
        if (percent > 100) {
            percent = 100;
        }
        genericDrawProgress(String.format("%d%%", percent), PROGRESS_BOUNDS.x, PROGRESS_BOUNDS.y, PROGRESS_BOUNDS.width, PROGRESS_BOUNDS.height, (float) percent / 100, true);
    }

    public void setProgress(int curK, int maxK) {
        if (curK > maxK) {
            curK = maxK;
        }
        float percent = ((float) curK / (float) maxK);
        genericDrawProgress(String.format("%d / %d", curK, maxK), PROGRESS_BOUNDS.x, PROGRESS_BOUNDS.y, PROGRESS_BOUNDS.width, PROGRESS_BOUNDS.height, percent, true);
    }

    public void hideProgress() {
        genericDrawProgress("", PROGRESS_BOUNDS.x, PROGRESS_BOUNDS.y, PROGRESS_BOUNDS.width, PROGRESS_BOUNDS.height, 0, false);
    }

    public void invokeLater(AbstractRunnable doRun) {
        eventQueue.add(doRun);
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void dispose() {
        synchronized (monitor) {
            eventQueue.clear();
            disposed = true;
            monitor.notify();
        }
    }

    /**
	 * Download files from a list of urls and store them
	 * altogether in a directory
	 * @param url
	 * @param dir
	 */
    public void downloadAll(URL[] url, File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("file is not a directory");
        }
        for (int i = 0; i < url.length; i++) {
            try {
                URL cur = url[i];
                File file = new File(dir, Launcher.getFileName(cur.toExternalForm()));
                FileOutputStream out = new FileOutputStream(file);
                download(cur, out);
                out.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
	 * Download an individual file from a url. You may get
	 * the file in a whole chunk of byte array, or wire
	 * in an output stream to have the file written
	 * @param url
	 * @param out
	 * @return
	 * @throws IOException
	 */
    public byte[] download(URL url, OutputStream out) throws IOException {
        boolean returnByByteArray = (out == null);
        ByteArrayOutputStream helper = null;
        if (returnByByteArray) {
            helper = new ByteArrayOutputStream();
        }
        String s = url.toExternalForm();
        URLConnection conn = url.openConnection();
        String name = Launcher.getFileName(s);
        InputStream in = conn.getInputStream();
        total = url.openConnection().getContentLength();
        setStatusText(String.format("Downloading %s (%.2fMB)...", name, ((float) total / 1024 / 1024)));
        long justNow = System.currentTimeMillis();
        int numRead = -1;
        byte[] buffer = new byte[2048];
        while ((numRead = in.read(buffer)) != -1) {
            size += numRead;
            if (returnByByteArray) {
                helper.write(buffer, 0, numRead);
            } else {
                out.write(buffer, 0, numRead);
            }
            long now = System.currentTimeMillis();
            if ((now - justNow) > 250) {
                setProgress((int) (((float) size / (float) total) * 100));
                justNow = now;
            }
        }
        hideProgress();
        if (returnByByteArray) {
            return helper.toByteArray();
        } else {
            return null;
        }
    }

    public URLClassLoader getClassLoader(URL urlOrJars) {
        return new URLClassLoader(new URL[] { urlOrJars });
    }

    public URLClassLoader getClassLoader(File dir) {
        hideProgress();
        File[] f = dir.listFiles();
        Set<URL> url = new HashSet<URL>();
        for (int i = 0; i < f.length; i++) {
            File cur = f[i];
            if (cur.getName().endsWith(".jar")) {
                try {
                    System.out.println("loading " + cur);
                    setStatusText(String.format("Loading %s...", cur.getName()));
                    url.add(cur.toURI().toURL());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        URL[] files = new URL[url.size()];
        url.toArray(files);
        return new URLClassLoader(files);
    }

    private void genericDrawText(final String str, final int x, final int y, final int w, final int h) {
        if (isRunnerThread()) {
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            g.fillRect(x, y, w, h);
            String[] lines = str.split("\\\n");
            Point currentlyAt = new Point(x, y);
            for (int i = 0; i < lines.length; i++) {
                String text = lines[i];
                if ((text != null) && (text.length() > 0)) {
                    g.setColor(Color.BLACK);
                    AttributedString attr = new AttributedString(text);
                    attr.addAttribute(TextAttribute.FAMILY, DEFAULT_FAMILY_NAME);
                    attr.addAttribute(TextAttribute.SIZE, DEFAULT_SIZE);
                    LineBreakMeasurer breaker = new LineBreakMeasurer(attr.getIterator(), g.getFontRenderContext());
                    while (breaker.getPosition() < text.length()) {
                        TextLayout lay = breaker.nextLayout(w);
                        float nextY = currentlyAt.y + lay.getLeading() + lay.getAscent();
                        float maxY = y + h;
                        if (nextY > maxY) {
                            break;
                        }
                        currentlyAt.y += lay.getLeading();
                        currentlyAt.y += lay.getAscent();
                        lay.draw(g, currentlyAt.x, currentlyAt.y);
                        currentlyAt.y += lay.getDescent();
                    }
                }
            }
            g.setColor(oldColor);
            splashScreen.update();
        } else {
            invokeLater(new AbstractRunnable() {

                public void run() {
                    genericDrawText(str, x, y, w, h);
                }
            });
        }
    }

    private void genericDrawProgress(final String text, final int x, final int y, final int w, final int h, final float percent, final boolean visible) {
        if (isRunnerThread()) {
            Color oldColor = g.getColor();
            if (!visible) {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, w + 1, h + 1);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, w + 1, h + 1);
                g.setColor(Color.GREEN);
                g.fillRect(x, y, (int) (w * percent), h);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, w, h);
                FontMetrics fontMetrics = g.getFontMetrics();
                Rectangle2D bounds = fontMetrics.getStringBounds(text, g);
                AttributedString attr = new AttributedString(text);
                attr.addAttribute(TextAttribute.FAMILY, PROGRESS_FAMILY_NAME);
                attr.addAttribute(TextAttribute.SIZE, PROGRESS_SIZE);
                float textX = (float) ((x + w / 2) - bounds.getWidth() / 2);
                float textY = (float) ((y + h / 2) - bounds.getHeight() / 2) + fontMetrics.getAscent();
                g.drawString(attr.getIterator(), textX, textY);
            }
            g.setColor(oldColor);
            splashScreen.update();
        } else {
            invokeLater(new AbstractRunnable() {

                public void run() {
                    genericDrawProgress(text, x, y, w, h, percent, visible);
                }
            });
        }
    }

    class Runner extends Thread {

        Runner() {
            super("splashscreen-runner");
        }

        public void run() {
            while (!disposed) {
                synchronized (monitor) {
                    if (eventQueue.size() > 0) {
                        Object[] array;
                        synchronized (eventQueue) {
                            array = eventQueue.toArray();
                            eventQueue.clear();
                        }
                        Arrays.sort(array);
                        for (int i = 0; i < array.length; i++) {
                            Runnable doRun = (Runnable) array[i];
                            doRun.run();
                        }
                    } else {
                        try {
                            monitor.wait();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
    }
}
