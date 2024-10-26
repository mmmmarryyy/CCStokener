package com.threerings.getdown.launcher;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import ca.beq.util.win32.registry.RegistryKey;
import ca.beq.util.win32.registry.RegistryValue;
import ca.beq.util.win32.registry.RootKey;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.text.MessageUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.data.Application;
import com.threerings.getdown.data.Application.UpdateInterface.Step;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.net.Downloader;
import com.threerings.getdown.net.HTTPDownloader;
import com.threerings.getdown.tools.Patcher;
import com.threerings.getdown.util.ConfigUtil;
import com.threerings.getdown.util.LaunchUtil;
import com.threerings.getdown.util.MetaProgressObserver;
import com.threerings.getdown.util.ProgressObserver;
import static com.threerings.getdown.Log.log;

/**
 * Manages the main control for the Getdown application updater and deployment system.
 */
public abstract class Getdown extends Thread implements Application.StatusDisplay, ImageLoader {

    public static void main(String[] args) {
        GetdownApp.main(args);
    }

    public Getdown(File appDir, String appId) {
        this(appDir, appId, null, null, null);
    }

    public Getdown(File appDir, String appId, List<Certificate> signers, String[] jvmargs, String[] appargs) {
        super("Getdown");
        try {
            String silent = System.getProperty("silent");
            _silent = silent != null;
            if (_silent) {
                _launchInSilent = silent.equals("launch");
            }
            _delay = Integer.getInteger("delay", 0);
        } catch (SecurityException se) {
        }
        try {
            _msgs = ResourceBundle.getBundle("com.threerings.getdown.messages");
        } catch (Exception e) {
            String dir = appDir.toString();
            if (dir.equals(".")) {
                dir = System.getProperty("user.dir");
            }
            String errmsg = "The directory in which this application is installed:\n" + dir + "\nis invalid (" + e.getMessage() + "). If the full path to the app directory " + "contains the '!' character, this will trigger this error.";
            fail(errmsg);
        }
        _app = new Application(appDir, appId, signers, jvmargs, appargs);
        _startup = System.currentTimeMillis();
    }

    /**
     * This is used by the applet which always needs a user interface and wants to load it as soon
     * as possible.
     */
    public void preInit() {
        try {
            _ifc = _app.init(true);
            createInterface(true);
        } catch (Exception e) {
            log.warning("Failed to preinit: " + e);
            createInterface(true);
        }
    }

    @Override
    public void run() {
        if (_msgs == null) {
            return;
        }
        File instdir = _app.getLocalPath("");
        if (!instdir.canWrite()) {
            String path = instdir.getPath();
            if (path.equals(".")) {
                path = System.getProperty("user.dir");
            }
            fail(MessageUtil.tcompose("m.readonly_error", path));
            return;
        }
        try {
            _dead = false;
            if (detectProxy()) {
                getdown();
            } else if (_silent) {
                log.warning("Need a proxy, but we don't want to bother anyone.  Exiting.");
            } else {
                _container = createContainer();
                _container.add(new ProxyPanel(this, _msgs), BorderLayout.CENTER);
                showContainer();
                _dead = true;
            }
        } catch (Exception e) {
            log.warning("run() failed.", e);
            String msg = e.getMessage();
            if (msg == null) {
                msg = MessageUtil.compose("m.unknown_error", _ifc.installError);
            } else if (!msg.startsWith("m.")) {
                if (e instanceof FileNotFoundException) {
                    msg = MessageUtil.compose("m.missing_resource", MessageUtil.taint(msg), _ifc.installError);
                } else {
                    msg = MessageUtil.compose("m.init_error", MessageUtil.taint(msg), _ifc.installError);
                }
            }
            fail(msg);
        }
    }

    /**
     * Configures our proxy settings (called by {@link ProxyPanel}) and fires up the launcher.
     */
    public void configureProxy(String host, String port) {
        log.info("User configured proxy", "host", host, "port", port);
        if (!StringUtil.isBlank(host)) {
            File pfile = _app.getLocalPath("proxy.txt");
            try {
                PrintStream pout = new PrintStream(new FileOutputStream(pfile));
                pout.println("host = " + host);
                if (!StringUtil.isBlank(port)) {
                    pout.println("port = " + port);
                }
                pout.close();
            } catch (IOException ioe) {
                log.warning("Error creating proxy file '" + pfile + "': " + ioe);
            }
            setProxyProperties(host, port);
        }
        disposeContainer();
        _status = null;
        new Thread(this).start();
    }

    /**
     * Reads and/or autodetects our proxy settings.
     *
     * @return true if we should proceed with running the launcher, false if we need to wait for
     * the user to enter proxy settings.
     */
    protected boolean detectProxy() {
        if (System.getProperty("http.proxyHost") != null) {
            return true;
        }
        if (RunAnywhere.isWindows()) {
            try {
                String host = null, port = null;
                boolean enabled = false;
                RegistryKey.initialize();
                RegistryKey r = new RegistryKey(RootKey.HKEY_CURRENT_USER, PROXY_REGISTRY);
                for (Iterator<?> iter = r.values(); iter.hasNext(); ) {
                    RegistryValue value = (RegistryValue) iter.next();
                    if (value.getName().equals("ProxyEnable")) {
                        enabled = value.getStringValue().equals("1");
                    }
                    if (value.getName().equals("ProxyServer")) {
                        String strval = value.getStringValue();
                        int cidx = strval.indexOf(":");
                        if (cidx != -1) {
                            port = strval.substring(cidx + 1);
                            strval = strval.substring(0, cidx);
                        }
                        host = strval;
                    }
                }
                if (enabled) {
                    setProxyProperties(host, port);
                    return true;
                } else {
                    log.info("Detected no proxy settings in the registry.");
                }
            } catch (Throwable t) {
                log.info("Failed to find proxy settings in Windows registry", "error", t);
            }
        }
        File pfile = _app.getLocalPath("proxy.txt");
        if (pfile.exists()) {
            try {
                Map<String, Object> pconf = ConfigUtil.parseConfig(pfile, false);
                setProxyProperties((String) pconf.get("host"), (String) pconf.get("port"));
                return true;
            } catch (IOException ioe) {
                log.warning("Failed to read '" + pfile + "': " + ioe);
            }
        }
        log.info("Checking whether we need to use a proxy...");
        try {
            _ifc = _app.init(true);
        } catch (IOException ioe) {
        }
        updateStatus("m.detecting_proxy");
        URL rurl = _app.getConfigResource().getRemote();
        try {
            URLConnection conn = rurl.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection hcon = (HttpURLConnection) conn;
                try {
                    hcon.setRequestMethod("HEAD");
                    hcon.connect();
                    if (hcon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        log.warning("Got a non-200 response but assuming we're OK because we got " + "something...", "url", rurl, "rsp", hcon.getResponseCode());
                    }
                } finally {
                    hcon.disconnect();
                }
            }
            log.info("No proxy appears to be needed.");
            try {
                pfile.createNewFile();
            } catch (IOException ioe) {
                log.warning("Failed to create blank proxy file '" + pfile + "': " + ioe);
            }
            return true;
        } catch (IOException ioe) {
            log.info("Failed to HEAD " + rurl + ": " + ioe);
            log.info("We probably need a proxy, but auto-detection failed.");
        }
        return false;
    }

    /**
     * Configures the JVM proxy system properties.
     */
    protected void setProxyProperties(String host, String port) {
        if (!StringUtil.isBlank(host)) {
            System.setProperty("http.proxyHost", host);
            if (!StringUtil.isBlank(port)) {
                System.setProperty("http.proxyPort", port);
            }
            log.info("Using proxy", "host", host, "port", port);
        }
    }

    /**
     * Does the actual application validation, update and launching business.
     */
    protected void getdown() {
        log.info("---------------- Proxy Info -----------------");
        log.info("-- Proxy Host: " + System.getProperty("http.proxyHost"));
        log.info("-- Proxy Port: " + System.getProperty("http.proxyPort"));
        log.info("---------------------------------------------");
        try {
            try {
                _ifc = _app.init(true);
            } catch (IOException ioe) {
                log.warning("Failed to parse 'getdown.txt': " + ioe);
                _app.attemptRecovery(this);
                _ifc = _app.init(true);
                createInterface(true);
            }
            if (!_app.lockForUpdates()) {
                throw new MultipleGetdownRunning();
            }
            File config = _app.getLocalPath(Application.CONFIG_FILE);
            if (!config.setLastModified(System.currentTimeMillis())) {
                log.warning("Unable to set modtime on config file, will be unable to check for " + "another instance of getdown running while this one waits.");
            }
            if (_delay > 0) {
                _app.releaseLock();
                long lastConfigModtime = config.lastModified();
                log.info("Waiting " + _delay + " minutes before beginning actual work.");
                Thread.sleep(_delay * 60 * 1000);
                if (lastConfigModtime < config.lastModified()) {
                    log.warning("getdown.txt was modified while getdown was waiting.");
                    throw new MultipleGetdownRunning();
                }
            }
            int[] alreadyValid = new int[1];
            Set<Resource> unpacked = new HashSet<Resource>();
            for (int ii = 0; ii < MAX_LOOPS; ii++) {
                if (!_app.haveValidJavaVersion()) {
                    log.info("Attempting to update Java VM...");
                    setStep(Step.UPDATE_JAVA);
                    _enableTracking = true;
                    try {
                        updateJava();
                    } finally {
                        _enableTracking = false;
                    }
                    continue;
                }
                setStep(Step.VERIFY_METADATA);
                setStatus("m.validating", -1, -1L, false);
                if (_app.verifyMetadata(this)) {
                    log.info("Application requires update.");
                    update();
                    continue;
                }
                setStep(Step.VERIFY_RESOURCES);
                setStatus("m.validating", -1, -1L, false);
                List<Resource> failures = _app.verifyResources(_progobs, alreadyValid, unpacked);
                if (failures == null) {
                    log.info("Resources verified.");
                    if (Boolean.getBoolean("check_unpacked")) {
                        File ufile = _app.getLocalPath("unpacked.dat");
                        if (!ufile.exists()) {
                            log.info("Performing initial unpack.");
                            setStep(Step.UNPACK);
                            updateStatus("m.validating");
                            _app.unpackResources(_progobs, unpacked);
                            ufile.createNewFile();
                        }
                    }
                    if (!_silent || _launchInSilent) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException("m.applet_stopped");
                        }
                        _app.lockForUpdates();
                        launch();
                    }
                    return;
                }
                try {
                    _enableTracking = (alreadyValid[0] == 0);
                    reportTrackingEvent("app_start", -1);
                    log.info(failures.size() + " of " + _app.getAllResources().size() + " rsrcs require update (" + alreadyValid[0] + " assumed valid).");
                    setStep(Step.REDOWNLOAD_RESOURCES);
                    download(failures);
                    reportTrackingEvent("app_complete", -1);
                } finally {
                    _enableTracking = false;
                }
            }
            log.warning("Pants! We couldn't get the job done.");
            throw new IOException("m.unable_to_repair");
        } catch (Exception e) {
            log.warning("getdown() failed.", e);
            String msg = e.getMessage();
            if (msg == null) {
                msg = MessageUtil.compose("m.unknown_error", _ifc.installError);
            } else if (!msg.startsWith("m.")) {
                if (e instanceof FileNotFoundException) {
                    msg = MessageUtil.compose("m.missing_resource", MessageUtil.taint(msg), _ifc.installError);
                } else {
                    msg = MessageUtil.compose("m.init_error", MessageUtil.taint(msg), _ifc.installError);
                }
            }
            fail(msg);
            _app.releaseLock();
        }
    }

    public void updateStatus(String message) {
        setStatus(message, -1, -1L, true);
    }

    /**
     * Load the image at the path.  Before trying the exact path/file specified we will look to see
     *  if we can find a localized version by sticking a _<language> in front of the "." in the
     *  filename.
     */
    public BufferedImage loadImage(String path) {
        if (StringUtil.isBlank(path)) {
            return null;
        }
        File imgpath = null;
        try {
            String localeStr = Locale.getDefault().getLanguage();
            imgpath = _app.getLocalPath(path.replace(".", "_" + localeStr + "."));
            return ImageIO.read(imgpath);
        } catch (IOException ioe) {
        }
        try {
            imgpath = _app.getLocalPath(path);
            return ImageIO.read(imgpath);
        } catch (IOException ioe2) {
            log.warning("Failed to load image", "path", imgpath, "error", ioe2);
            return null;
        }
    }

    /**
     * Downloads and installs an Java VM bundled with the application. This is called if we are not
     * running with the necessary Java version.
     */
    protected void updateJava() throws IOException, InterruptedException {
        Resource vmjar = _app.getJavaVMResource();
        if (vmjar == null) {
            throw new IOException("m.java_download_failed");
        }
        reportTrackingEvent("jvm_start", -1);
        updateStatus("m.downloading_java");
        List<Resource> list = new ArrayList<Resource>();
        list.add(vmjar);
        download(list);
        reportTrackingEvent("jvm_unpack", -1);
        updateStatus("m.unpacking_java");
        if (!vmjar.unpack()) {
            throw new IOException("m.java_unpack_failed");
        }
        vmjar.markAsValid();
        if (RunAnywhere.isLinux()) {
            String vmbin = LaunchUtil.LOCAL_JAVA_DIR + File.separator + "bin" + File.separator + "java";
            String cmd = "chmod a+rx " + _app.getLocalPath(vmbin);
            try {
                log.info("Please smack a Java engineer. Running: " + cmd);
                Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
                log.warning("Failed to mark VM binary as executable", "cmd", cmd, "error", e);
            }
        }
        String vmpath = LaunchUtil.getJVMPath(_app.getLocalPath(""));
        try {
            log.info("Regenerating classes.jsa for " + vmpath + "...");
            Runtime.getRuntime().exec(vmpath + " -Xshare:dump");
        } catch (Exception e) {
            log.warning("Failed to regenerate .jsa dum file", "error", e);
        }
        reportTrackingEvent("jvm_complete", -1);
    }

    /**
     * Called if the application is determined to be of an old version.
     */
    protected void update() throws IOException, InterruptedException {
        _app.clearValidationMarkers();
        Resource patch = _app.getPatchResource(null);
        if (patch != null) {
            List<Resource> list = new ArrayList<Resource>();
            list.add(patch);
            for (String auxgroup : _app.getAuxGroups()) {
                if (_app.isAuxGroupActive(auxgroup)) {
                    patch = _app.getPatchResource(auxgroup);
                    if (patch != null) {
                        list.add(patch);
                    }
                }
            }
            if (!StringUtil.isBlank(_ifc.patchNotesUrl)) {
                createInterface(false);
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        _patchNotes.setVisible(true);
                    }
                });
            }
            setStep(Step.DOWNLOAD);
            download(list);
            setStep(Step.PATCH);
            updateStatus("m.patching");
            MetaProgressObserver mprog = new MetaProgressObserver(_progobs, list.size());
            for (Resource prsrc : list) {
                mprog.startElement(1);
                try {
                    Patcher patcher = new Patcher();
                    patcher.patch(prsrc.getLocal().getParentFile(), prsrc.getLocal(), mprog);
                } catch (Exception e) {
                    log.warning("Failed to apply patch", "prsrc", prsrc, e);
                }
                if (!prsrc.getLocal().delete()) {
                    log.warning("Failed to delete '" + prsrc + "'.");
                    prsrc.getLocal().deleteOnExit();
                }
            }
        }
        _app.updateMetadata();
        _ifc = _app.init(true);
    }

    /**
     * Called if the application is determined to require resource downloads.
     */
    protected void download(List<Resource> resources) throws IOException, InterruptedException {
        createInterface(false);
        Downloader.Observer obs = new Downloader.Observer() {

            public void resolvingDownloads() {
                updateStatus("m.resolving");
            }

            public boolean downloadProgress(int percent, long remaining) {
                if (_lastCheck == -1 || percent >= _lastCheck + 10) {
                    if (_delay > 0) {
                        boolean locked = _app.lockForUpdates();
                        _app.releaseLock();
                        return locked;
                    }
                    _lastCheck = percent;
                }
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }
                setStatus("m.downloading", stepToGlobalPercent(percent), remaining, true);
                if (percent > 0) {
                    reportTrackingEvent("progress", percent);
                }
                return true;
            }

            public void downloadFailed(Resource rsrc, Exception e) {
                updateStatus(MessageUtil.tcompose("m.failure", e.getMessage()));
                log.warning("Download failed", "rsrc", rsrc, e);
            }

            protected int _lastCheck = -1;
        };
        Downloader dl = new HTTPDownloader(resources, obs);
        if (!dl.download()) {
            if (Thread.interrupted()) {
                throw new InterruptedException("m.applet_stopped");
            }
            throw new MultipleGetdownRunning();
        }
    }

    /**
     * Called to launch the application if everything is determined to be ready to go.
     */
    protected void launch() {
        setStep(Step.LAUNCH);
        setStatus("m.launching", stepToGlobalPercent(100), -1L, false);
        try {
            if (invokeDirect()) {
                disposeContainer();
                _app.releaseLock();
                _app.invokeDirect(getApplet());
            } else {
                Process proc;
                if (_app.hasOptimumJvmArgs()) {
                    proc = _app.createProcess(true);
                    long fallback = System.currentTimeMillis() + FALLBACK_CHECK_TIME;
                    boolean error = false;
                    while (fallback > System.currentTimeMillis()) {
                        try {
                            error = proc.exitValue() != 0;
                            break;
                        } catch (IllegalThreadStateException e) {
                            Thread.yield();
                        }
                    }
                    if (error) {
                        log.info("Failed to launch with optimum arguments; falling back.");
                        proc = _app.createProcess(false);
                    }
                } else {
                    proc = _app.createProcess(false);
                }
                proc.getInputStream().close();
                proc.getOutputStream().close();
                final InputStream stderr = proc.getErrorStream();
                if (LaunchUtil.mustMonitorChildren()) {
                    disposeContainer();
                    _status = null;
                    copyStream(stderr, System.err);
                    log.info("Process exited: " + proc.waitFor());
                } else {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            copyStream(stderr, System.err);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            }
            long uptime = System.currentTimeMillis() - _startup;
            if (_container != null && uptime < MIN_EXIST_TIME) {
                try {
                    Thread.sleep(MIN_EXIST_TIME - uptime);
                } catch (Exception e) {
                }
            }
            setStatus(null, 100, -1L, false);
            exit(0);
            if (_playAgain != null && _playAgain.isEnabled()) {
                Timer timer = new Timer("playAgain", true);
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        initPlayAgain();
                        _playAgain.setVisible(true);
                    }
                }, PLAY_AGAIN_TIME);
            }
        } catch (Exception e) {
            log.warning("launch() failed.", e);
        }
    }

    /**
     * Creates our user interface, which we avoid doing unless we actually have to update
     * something.
     *
     * @param reinit - if the interface should be reinitialized if it already exists.
     */
    protected void createInterface(final boolean reinit) {
        if (_silent || (_container != null && !reinit)) {
            return;
        }
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (_status == null) {
                    _container = createContainer();
                    _layers = new JLayeredPane();
                    _container.add(_layers, BorderLayout.CENTER);
                    _patchNotes = new JButton(new AbstractAction(_msgs.getString("m.patch_notes")) {

                        @Override
                        public void actionPerformed(ActionEvent event) {
                            showDocument(_ifc.patchNotesUrl);
                        }
                    });
                    _patchNotes.setFont(StatusPanel.FONT);
                    _layers.add(_patchNotes);
                    if (getApplet() != null) {
                        _playAgain = new JButton();
                        _playAgain.setEnabled(false);
                        _playAgain.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        _playAgain.setFont(StatusPanel.FONT);
                        _playAgain.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent event) {
                                _playAgain.setVisible(false);
                                _stepMinPercent = _lastGlobalPercent = 0;
                                EventQueue.invokeLater(new Runnable() {

                                    public void run() {
                                        getdown();
                                    }
                                });
                            }
                        });
                        _layers.add(_playAgain);
                    }
                    _status = new StatusPanel(_msgs);
                    _layers.add(_status);
                    initInterface();
                } else if (reinit) {
                    initInterface();
                }
                showContainer();
            }
        });
    }

    /**
     * Initializes the interface with the current UpdateInterface and backgrounds.
     */
    protected void initInterface() {
        RotatingBackgrounds newBackgrounds = getBackground();
        if (_background == null || newBackgrounds.getNumImages() > 0) {
            _background = newBackgrounds;
        }
        _status.init(_ifc, _background, getProgressImage());
        Dimension size = _status.getPreferredSize();
        _status.setSize(size);
        _layers.setPreferredSize(size);
        _patchNotes.setBounds(_ifc.patchNotes);
        _patchNotes.setVisible(false);
        initPlayAgain();
        _uiDisplayPercent = _lastGlobalPercent;
        _stepMinPercent = _lastGlobalPercent = 0;
    }

    protected void initPlayAgain() {
        if (_playAgain != null) {
            Image image = loadImage(_ifc.playAgainImage);
            boolean hasImage = image != null;
            if (hasImage) {
                _playAgain.setIcon(new ImageIcon(image));
                _playAgain.setText("");
            } else {
                _playAgain.setText(_msgs.getString("m.play_again"));
                _playAgain.setIcon(null);
            }
            _playAgain.setBorderPainted(!hasImage);
            _playAgain.setOpaque(!hasImage);
            _playAgain.setContentAreaFilled(!hasImage);
            if (_ifc.playAgain != null) {
                _playAgain.setBounds(_ifc.playAgain);
                _playAgain.setEnabled(true);
            }
            _playAgain.setVisible(false);
        }
    }

    protected RotatingBackgrounds getBackground() {
        if (_ifc.rotatingBackgrounds != null) {
            if (_ifc.backgroundImage != null) {
                log.warning("ui.background_image and ui.rotating_background were both specified. " + "The rotating images are being used.");
            }
            return new RotatingBackgrounds(_ifc.rotatingBackgrounds, _ifc.errorBackground, Getdown.this);
        } else if (_ifc.backgroundImage != null) {
            return new RotatingBackgrounds(loadImage(_ifc.backgroundImage));
        } else {
            return new RotatingBackgrounds();
        }
    }

    protected Image getProgressImage() {
        return loadImage(_ifc.progressImage);
    }

    protected void handleWindowClose() {
        if (_dead) {
            exit(0);
        } else {
            if (_abort == null) {
                _abort = new AbortPanel(Getdown.this, _msgs);
            }
            _abort.pack();
            SwingUtil.centerWindow(_abort);
            _abort.setVisible(true);
            _abort.setState(JFrame.NORMAL);
            _abort.requestFocus();
        }
    }

    /**
     * Update the status to indicate getdown has failed for the reason in <code>message</code>.
     */
    protected void fail(String message) {
        _dead = true;
        setStatus(message, stepToGlobalPercent(0), -1L, true);
    }

    /**
     * Set the current step, which will be used to globalize per-step percentages.
     */
    protected void setStep(Step step) {
        int finalPercent = -1;
        for (Integer perc : _ifc.stepPercentages.get(step)) {
            if (perc > _stepMaxPercent) {
                finalPercent = perc;
                break;
            }
        }
        if (finalPercent == -1) {
            return;
        }
        _stepMaxPercent = finalPercent;
        _stepMinPercent = _lastGlobalPercent;
    }

    /**
     * Convert a step percentage to the global percentage.
     */
    protected int stepToGlobalPercent(int percent) {
        int adjustedMaxPercent = ((_stepMaxPercent - _uiDisplayPercent) * 100) / (100 - _uiDisplayPercent);
        _lastGlobalPercent = Math.max(_lastGlobalPercent, _stepMinPercent + (percent * (adjustedMaxPercent - _stepMinPercent)) / 100);
        return _lastGlobalPercent;
    }

    /**
     * Update the status.
     */
    protected void setStatus(final String message, final int percent, final long remaining, boolean createUI) {
        if (_status == null && createUI) {
            createInterface(false);
        }
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (_status == null) {
                    if (message != null) {
                        log.info("Dropping status '" + message + "'.");
                    }
                    return;
                }
                if (message != null) {
                    _status.setStatus(message, _dead);
                }
                if (_dead) {
                    _status.setProgress(0, -1L);
                } else if (percent >= 0) {
                    _status.setProgress(percent, remaining);
                }
            }
        });
    }

    protected void reportTrackingEvent(String event, int progress) {
        if (!_enableTracking) {
            return;
        } else if (progress > 0) {
            do {
                URL url = _app.getTrackingProgressURL(++_reportedProgress);
                if (url != null) {
                    new ProgressReporter(url).start();
                }
            } while (_reportedProgress <= progress);
        } else {
            URL url = _app.getTrackingURL(event);
            if (url != null) {
                new ProgressReporter(url).start();
            }
        }
    }

    /**
     * Creates the container in which our user interface will be displayed.
     */
    protected abstract Container createContainer();

    /**
     * Shows the container in which our user interface will be displayed.
     */
    protected abstract void showContainer();

    /**
     * Disposes the container in which we have our user interface.
     */
    protected abstract void disposeContainer();

    /**
     * If this method returns true we will run the application in the same JVM, otherwise we will
     * fork off a new JVM. Some options are not supported if we do not fork off a new JVM.
     */
    protected boolean invokeDirect() {
        return Boolean.getBoolean("direct");
    }

    /**
     * Provides access to the applet that we'll pass on to our application when we're in "invoke
     * direct" mode.
     */
    protected JApplet getApplet() {
        return null;
    }

    /**
     * Requests to show the document at the specified URL in a new window.
     */
    protected abstract void showDocument(String url);

    /**
     * Requests that Getdown exit. In applet mode this does nothing.
     */
    protected abstract void exit(int exitCode);

    /**
     * Copies the supplied stream from the specified input to the specified output. Used to copy
     * our child processes stderr and stdout to our own stderr and stdout.
     */
    protected static void copyStream(InputStream in, PrintStream out) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                out.print(line);
                out.flush();
            }
        } catch (IOException ioe) {
            log.warning("Failure copying", "in", in, "out", out, "error", ioe);
        }
    }

    /** Used to fetch a progress report URL. */
    protected class ProgressReporter extends Thread {

        public ProgressReporter(URL url) {
            setDaemon(true);
            _url = url;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection ucon = (HttpURLConnection) _url.openConnection();
                if (_app.getTrackingCookieName() != null && _app.getTrackingCookieProperty() != null) {
                    String val = System.getProperty(_app.getTrackingCookieProperty());
                    if (val != null) {
                        ucon.setRequestProperty("Cookie", _app.getTrackingCookieName() + "=" + val);
                    }
                }
                ucon.connect();
                try {
                    if (ucon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        log.warning("Failed to report tracking event", "url", _url, "rcode", ucon.getResponseCode());
                    }
                } finally {
                    ucon.disconnect();
                }
            } catch (IOException ioe) {
                log.warning("Failed to report tracking event", "url", _url, "error", ioe);
            }
        }

        protected URL _url;
    }

    /** Used to pass progress on to our user interface. */
    protected ProgressObserver _progobs = new ProgressObserver() {

        public void progress(int percent) {
            setStatus(null, stepToGlobalPercent(percent), -1L, false);
        }
    };

    protected Application _app;

    protected Application.UpdateInterface _ifc = new Application.UpdateInterface();

    protected ResourceBundle _msgs;

    protected Container _container;

    protected JLayeredPane _layers;

    protected StatusPanel _status;

    protected JButton _patchNotes;

    protected JButton _playAgain;

    protected AbortPanel _abort;

    protected RotatingBackgrounds _background;

    protected boolean _dead;

    protected boolean _silent;

    protected boolean _launchInSilent;

    protected long _startup;

    protected boolean _enableTracking = true;

    protected int _reportedProgress = 0;

    /** Number of minutes to wait after startup before beginning any real heavy lifting. */
    protected int _delay;

    protected int _stepMaxPercent;

    protected int _stepMinPercent;

    protected int _lastGlobalPercent;

    protected int _uiDisplayPercent;

    protected static final int MAX_LOOPS = 5;

    protected static final long MIN_EXIST_TIME = 5000L;

    protected static final long FALLBACK_CHECK_TIME = 1000L;

    protected static final long PLAY_AGAIN_TIME = 3000L;

    protected static final String PROXY_REGISTRY = "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";
}
