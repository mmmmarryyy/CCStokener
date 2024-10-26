package org.formaria.swing.video;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.media.CachingControl;
import javax.media.CachingControlEvent;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.SizeChangeEvent;
import javax.media.Time;
import java.awt.Component;
import java.awt.Container;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Seekable;
import javax.media.protocol.SourceStream;
import org.formaria.debug.DebugLogger;

/**
 * A wrapper for the video/jmf controller listener
 *
 * <p> Copyright (c) Formaria Ltd., 2008, This software is licensed under
 * the GNU Public License (GPL), please see license.txt for more details. If
 * you make commercial use of this software you must purchase a commercial
 * license from Formaria.</p>
 * <p> $Revision: 1.6 $</p>
 */
public class ControllerListener implements javax.media.ControllerListener, VideoController {

    protected Container video;

    private static boolean dllsLoaded = false;

    public ControllerListener() {
        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, Boolean.TRUE);
    }

    public void setVideoPanel(Container videoObj) {
        video = videoObj;
        setupPlayer();
    }

    /**
   * This controllerUpdate function must be defined in order to
   * implement a ControllerListener interface. This
   * function will be called whenever there is a media event
   */
    public synchronized void controllerUpdate(ControllerEvent event) {
        if (player == null) return;
        if (event instanceof RealizeCompleteEvent) {
            if (progressBar != null) {
                video.remove(progressBar);
                progressBar = null;
            }
            if (controlComponent == null) {
                if ((controlComponent = player.getControlPanelComponent()) != null) {
                    video.add(controlComponent, BorderLayout.SOUTH);
                    if (!showController) controlComponent.setVisible(false);
                }
            }
            if (visualComponent == null) {
                if ((visualComponent = player.getVisualComponent()) != null) video.add(visualComponent, BorderLayout.CENTER);
            }
            if (controlComponent != null) {
                controlComponent.invalidate();
                if (!video.isVisible()) controlComponent.setVisible(false);
            }
            if (!video.isVisible() && (visualComponent != null)) visualComponent.setVisible(false);
            video.doLayout();
            if (autoStart) {
                player.setMediaTime(new Time(0));
                if (!deallocateSync) player.start();
            }
        } else if (event instanceof CachingControlEvent) {
            if (player.getState() > Controller.Realizing) return;
            CachingControlEvent e = (CachingControlEvent) event;
            CachingControl cc = e.getCachingControl();
            if (progressBar == null) {
                if ((progressBar = cc.getControlComponent()) != null) {
                    video.add(progressBar);
                    video.validate();
                }
            }
        } else if (event instanceof EndOfMediaEvent) {
            if (loopPlayback) {
                player.setMediaTime(new Time(0));
                if (!deallocateSync && video.isVisible() && video.isShowing()) player.start();
            }
        } else if (event instanceof ControllerErrorEvent) {
            player = null;
            DebugLogger.logError("WIDGET", ((ControllerErrorEvent) event).getMessage());
        } else if (event instanceof ControllerClosedEvent) {
            video.removeAll();
        } else {
            try {
                if (event instanceof SizeChangeEvent) {
                    SizeChangeEvent sce = (SizeChangeEvent) event;
                    int nooWidth = (int) (sce.getWidth());
                    int nooHeight = (int) (sce.getHeight());
                    if (nooWidth != videoWidth || nooHeight != videoHeight) {
                        videoWidth = nooWidth;
                        videoHeight = nooHeight;
                    }
                    if (controlComponent != null) controlComponent.invalidate();
                    System.out.println("Preferred video size: " + Integer.toString(videoWidth) + "x" + Integer.toString(videoWidth + controlPanelHeight));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        if (player != null) {
            stop();
            player.close();
        }
    }

    /**
   * Starts playing the video
   */
    public void start() {
        if (player == null) setupPlayer();
        if ((player != null) && (visualComponent != null)) {
            player.setMediaTime(new Time(0));
            if (!deallocateSync) player.start();
        } else autoStart = true;
    }

    /**
   * Continuous playback of the video.
   */
    public void loop() {
        loopPlayback = true;
    }

    /**
   * Stops playback of the video.
   */
    public void stop() {
        if (player != null) {
            deallocateSync = true;
            player.stop();
            player.deallocate();
            deallocateSync = false;
        }
        loopPlayback = false;
    }

    public void setFile(String fileName) {
        mediaFile = fileName;
        if (player == null) setupPlayer();
    }

    protected void setupPlayer() {
        if (mediaFile == null) {
            DebugLogger.logError("WIDGET", "No media file specified");
            return;
        }
        if (!dllsLoaded) {
            dllsLoaded = true;
            loadNativeLib("jmutil");
            loadNativeLib("jsound");
            loadNativeLib("jmmpegv");
            loadNativeLib("jmutil");
            loadNativeLib("jmmpa");
            loadNativeLib("jmddraw");
            loadNativeLib("jmdaud");
            loadNativeLib("jmam");
        }
        MediaLocator mrl = null;
        if ((mrl = new MediaLocator(mediaFile)) == null) DebugLogger.logError("WIDGET", "Can't build URL for " + mediaFile);
        try {
            DataSource ds = null;
            if (mrl.getProtocol().equals("jar")) ds = new JarEntryDataSource(mrl); else ds = Manager.createDataSource(mrl);
            player = Manager.createPlayer(ds);
        } catch (Exception e) {
            DebugLogger.logError("WIDGET", "Could not create player for " + mrl);
        }
        player.addControllerListener(this);
        player.realize();
    }

    private void loadNativeLib(String libName) {
        try {
            System.loadLibrary(libName);
        } catch (Throwable t) {
            DebugLogger.logError("WIDGET", "Unable to load library: " + libName);
        }
    }

    public void showController(boolean display) {
        if (controlComponent != null) {
            controlComponent.setVisible(display);
            video.doLayout();
        } else showController = display;
    }

    String mediaFile;

    boolean firstTime = true;

    long CachingSize = 0L;

    boolean deallocateSync = false;

    boolean loopPlayback = false;

    boolean autoStart = false;

    boolean showController = true;

    Player player = null;

    int controlPanelHeight = 0;

    int videoWidth = 0;

    int videoHeight = 0;

    Component visualComponent = null;

    Component controlComponent = null;

    Component progressBar = null;
}

class JarEntryDataSource extends PullDataSource {

    protected static Object[] EMPTY_OBJECT_ARRAY = {};

    protected JarEntryPullStream jarIn;

    protected PullSourceStream[] sourceStreams;

    public JarEntryDataSource(MediaLocator ml) throws IllegalArgumentException, IOException {
        super();
        setLocator(ml);
    }

    protected void createJarIn() throws IOException {
        jarIn = new JarEntryPullStream();
        sourceStreams = new PullSourceStream[1];
        sourceStreams[0] = jarIn;
    }

    public void setLocator(MediaLocator ml) throws IllegalArgumentException {
        if (!ml.getProtocol().equals("jar")) throw new IllegalArgumentException("Not a jar:-style URL: " + ml.toString());
        super.setLocator(ml);
        try {
            createJarIn();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public PullSourceStream[] getStreams() {
        return sourceStreams;
    }

    public void connect() throws IOException {
    }

    public void disconnect() {
        if (jarIn != null) {
            jarIn.close();
            jarIn = null;
        }
    }

    /** A pretty bare-bones implementation, only supports
   *  <code>video.quicktime</code>, <code>video.mpeg</code>
   *  and <code>video.x_msvideo</code>, based solely on
   *  filename extension.
   */
    public String getContentType() {
        try {
            URL url = getLocator().getURL();
            String urlFile = url.getFile();
            if (urlFile.endsWith(".mov")) return "video.quicktime"; else if (urlFile.endsWith(".mpg")) return "video.mpeg"; else if (urlFile.endsWith(".avi")) return "video.x_msvideo"; else return "unknown";
        } catch (MalformedURLException murle) {
            return "unknown";
        }
    }

    public void start() {
    }

    public void stop() {
    }

    public Time getDuration() {
        return DataSource.DURATION_UNKNOWN;
    }

    public Object getControl(String controlName) {
        return null;
    }

    public Object[] getControls() {
        return EMPTY_OBJECT_ARRAY;
    }

    class JarEntryPullStream extends Object implements PullSourceStream, Seekable {

        protected InputStream in;

        protected ContentDescriptor unknownCD = new ContentDescriptor("unknown");

        protected long tellPoint;

        public JarEntryPullStream() throws IOException {
            open();
        }

        public void open() throws IOException {
            URL url = getLocator().getURL();
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            in = conn.getInputStream();
            tellPoint = 0;
        }

        public void close() {
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        public void thoroughSkip(long skipCount) throws IOException {
            long totalSkipped = 0;
            while (totalSkipped < skipCount) {
                long skipped = in.skip(skipCount - totalSkipped);
                totalSkipped += skipped;
                tellPoint += skipped;
            }
        }

        public int read(byte[] buf, int off, int length) throws IOException {
            int bytesRead = in.read(buf, off, length);
            tellPoint += bytesRead;
            return bytesRead;
        }

        public boolean willReadBlock() {
            try {
                return (in.available() > 0);
            } catch (IOException ioe) {
                return true;
            }
        }

        public long getContentLength() {
            return SourceStream.LENGTH_UNKNOWN;
        }

        public boolean endOfStream() {
            try {
                return (in.available() == -1);
            } catch (IOException ioe) {
                return true;
            }
        }

        public ContentDescriptor getContentDescriptor() {
            return unknownCD;
        }

        public Object getControl(String controlType) {
            return null;
        }

        public Object[] getControls() {
            return EMPTY_OBJECT_ARRAY;
        }

        public boolean isRandomAccess() {
            return true;
        }

        public long seek(long position) {
            try {
                if (position > tellPoint) thoroughSkip(position - tellPoint); else {
                    close();
                    open();
                    thoroughSkip(position);
                }
                return tellPoint;
            } catch (IOException ioe) {
                return 0;
            }
        }

        public long tell() {
            return tellPoint;
        }
    }
}
