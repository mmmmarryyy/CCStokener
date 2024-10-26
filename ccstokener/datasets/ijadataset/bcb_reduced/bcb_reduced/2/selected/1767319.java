package net.sf.sageplugins.webserver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.ServerSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.sageplugins.sageutils.SageApi;

/**
 * This class will be used to manage the transcode streams created by
 * VLC.
 */
public class VlcTranscodeMgr {

    private static final String PROP_MAX_STREAMS = "MaxStreams";

    private static final String PROP_SERVER_BUFFER = "ServerBuffer";

    private static final String PROP_WIN_VLC_WK_DIR = "WinVlcWkDir";

    private static final String PROP_WIN_VLC_CMD = "WinVlcCmd";

    private static final String PROP_UNIX_VLC_WK_DIR = "UnixVlcWkDir";

    private static final String PROP_UNIX_VLC_CMD = "UnixVlcCmd";

    private static final String PROP_VLC_OPTS = "VlcOpts";

    private static final String PROP_START_PORT = "StartAtPort";

    private static final String PROP_STOP_PORT = "StopAtPort";

    private static final String PROP_MAX_RETRIES = "MaxRetries";

    private static final String PROP_LOCALHOST_ADDR = "LocalHostAddr";

    private static final String PROP_FILE_EXT = "fileExt/";

    private static final String PROP_TRANSCODE_MODES = "transcodeMode/";

    private static final String PROP_DEBUG_LOG = "DebugLog";

    private static final String PROP_ORB_INSTALLED = "OrbInstalled";

    private static final String OPT_TOKEN_LOCAL_PATH = "%LOCAL_PATH%";

    private static final String OPT_TOKEN_VIDEO_CODEC = "%VIDEO_CODEC%";

    private static final String OPT_TOKEN_VIDEO_BITRATE = "%VIDEO_BITRATE%";

    private static final String OPT_TOKEN_AUDIO_CODEC = "%AUDIO_CODEC%";

    private static final String OPT_TOKEN_AUDIO_BITRATE = "%AUDIO_BITRATE%";

    private static final String OPT_TOKEN_SERVERPORT = "%SERVERPORT%";

    private static final String OPT_TOKEN_SCALE = "%SCALE%";

    private static final String OPT_TOKEN_DEINT = "%DEINTERLACE%";

    private static final String OPT_TOKEN_MUX = "%MUX%";

    private long propsLastLoaded = 0;

    private static VlcTranscodeMgr instance = null;

    private final HashSet<TranscodeInputStream> streams = new HashSet<TranscodeInputStream>();

    private final Map<String, String> fileExtMap = new HashMap<String, String>();

    private final Map<String, List<String[]>> transcodeFormatMap = new TreeMap<String, List<String[]>>();

    private int maxStreams;

    private int serverBuffer;

    private File vlcWkDir;

    private String vlcCmd;

    private String vlcOpts;

    private InetAddress localhostAddr;

    private int startPort;

    private int stopPort;

    private int maxRetries;

    private boolean debugLog = true;

    private boolean orbInstalled = false;

    private VlcTranscodeMgr() {
        readProperties();
    }

    /**
     * Retrives the singleton instance of this manager.
     * @return the singleton instance
     */
    public static synchronized VlcTranscodeMgr getInstance() {
        if (instance == null) {
            instance = new VlcTranscodeMgr();
        }
        return instance;
    }

    /**
     * Starts the transcode process and returns the inputstream to the transcoded stream.  The various arguments
     * are the arguments accepted by VLC.
     *
     * @param localPath
     * @param videoCodec mp1v, mp2v, mp4v, DIV1, DIV2, DIV3, H263, h264, WMV1, WMV2, MJPG, theo
     * @param videoBitrate in kilobits
     * @param audioCodec mpga, mp2a, mp3, mp4a, a52, vorb, flac, spx, s16l, fl32
     * @param audioBitrate in kilobits
     * @param scale down-rez-ing of video in float (eg 0.3)
     * @param deint whether to deinterlace the input.
     * 
     * @return The inputstream from the transcoded movie
     */
    public TranscodeInputStream startTranscodeProcess(String localPath, String videoCodec, int videoBitrate, String audioCodec, int audioBitrate, String scale, boolean deint, String mux) throws Throwable {
        readProperties();
        String fileExt = (String) fileExtMap.get(mux);
        if (fileExt == null) {
            throw new IOException("Invalid mux specified");
        }
        if (streams.size() >= maxStreams) {
            throw new IOException("Maximum number of streams reached.");
        }
        ServerSocket socket = findAvailablePort();
        if (socket == null) {
            throw new IOException("Unable to allocate port for streaming " + "between specified ports " + startPort + "-" + stopPort);
        }
        int port = socket.getLocalPort();
        StringBuffer cmdBuff = new StringBuffer();
        cmdBuff.append(" \"");
        cmdBuff.append(vlcCmd);
        cmdBuff.append("\" ");
        cmdBuff.append(vlcOpts);
        replaceFirst(cmdBuff, OPT_TOKEN_LOCAL_PATH, localPath);
        replaceFirst(cmdBuff, OPT_TOKEN_VIDEO_CODEC, videoCodec);
        replaceFirst(cmdBuff, OPT_TOKEN_VIDEO_BITRATE, String.valueOf(videoBitrate));
        replaceFirst(cmdBuff, OPT_TOKEN_AUDIO_CODEC, audioCodec);
        replaceFirst(cmdBuff, OPT_TOKEN_AUDIO_BITRATE, String.valueOf(audioBitrate));
        replaceFirst(cmdBuff, OPT_TOKEN_SERVERPORT, localhostAddr.getHostAddress() + ":" + port);
        replaceFirst(cmdBuff, OPT_TOKEN_SCALE, scale);
        replaceFirst(cmdBuff, OPT_TOKEN_MUX, mux);
        if (deint) {
            replaceFirst(cmdBuff, OPT_TOKEN_DEINT, ",deinterlace");
        } else {
            replaceFirst(cmdBuff, OPT_TOKEN_DEINT, "");
        }
        if (debugLog) Acme.Serve.Serve.extLog("OS: " + System.getProperty("os.name"));
        if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
            if (debugLog) Acme.Serve.Serve.extLog("Converting Command for Linux: " + cmdBuff);
            String UQ = "(?<=[^\\\\])\\\"";
            String US = "(?<=[^\\\\])\\s";
            Pattern p = Pattern.compile(UQ + ".*?" + UQ);
            Matcher m = p.matcher(cmdBuff.toString());
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String s = cmdBuff.toString().substring(m.start() + 1, m.end() - 1);
                Acme.Serve.Serve.extLog("s1: " + s);
                s = s.replaceAll(US, "\\\\ ");
                Acme.Serve.Serve.extLog("s2: " + s);
                s = s.replace("\\", "\\\\");
                Acme.Serve.Serve.extLog("s3: " + s);
                m.appendReplacement(sb, s);
            }
            m.appendTail(sb);
            cmdBuff = sb;
            replaceFirst(cmdBuff, "--dummy-quiet", "");
        }
        TranscodeInputStream is = null;
        synchronized (streams) {
            socket.close();
            if (debugLog) Acme.Serve.Serve.extLog("Executing command: " + cmdBuff.substring(1));
            final Process proc = Runtime.getRuntime().exec(cmdBuff.toString().substring(1), null, vlcWkDir);
            if (proc != null) {
                Thread stdout_reader = new Thread() {

                    public void run() {
                        BufferedReader proc_out = new BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
                        String line;
                        try {
                            while (null != (line = proc_out.readLine())) if (debugLog) Acme.Serve.Serve.extLog("VLC(out): " + line);
                        } catch (IOException e) {
                        }
                        try {
                            proc_out.close();
                        } catch (IOException e) {
                        }
                    }
                };
                stdout_reader.start();
                Thread stderr_reader = new Thread() {

                    public void run() {
                        BufferedReader proc_err = new BufferedReader(new java.io.InputStreamReader(proc.getErrorStream()));
                        String line;
                        try {
                            while (null != (line = proc_err.readLine())) if (debugLog) Acme.Serve.Serve.extLog("VLC(err): " + line);
                        } catch (IOException e) {
                        }
                        try {
                            proc_err.close();
                        } catch (IOException e) {
                        }
                    }
                };
                stderr_reader.start();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                int retryCount = 0;
                URL url = new URL("http://" + localhostAddr.getHostAddress() + ":" + String.valueOf(socket.getLocalPort()));
                while (retryCount < maxRetries) {
                    try {
                        URLConnection conn = url.openConnection();
                        if (debugLog) Acme.Serve.Serve.extLog("Connected to " + conn.toString());
                        is = new TranscodeInputStream(conn.getInputStream(), proc, port, fileExt, serverBuffer);
                        streams.add(is);
                        if (debugLog) Acme.Serve.Serve.extLog("Returning InputStream: " + is);
                        return is;
                    } catch (IOException ex) {
                        Acme.Serve.Serve.extLog("Unable to connect, retrying. " + ex);
                        retryCount++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                throw new IOException("Too many retries");
            } catch (Throwable e) {
                Acme.Serve.Serve.extLog("Failed to set up InputStream: " + e);
                Acme.Serve.Serve.extLog("Shutting down vlc transcoder process");
                if (proc != null) proc.destroy();
                throw e;
            }
        }
    }

    /**
     * Cleans up the transcode process associated with this inputstream.
     * @param is
     */
    public void stopTranscodeProcess(TranscodeInputStream is) {
        if (is != null) {
            Process proc = is.getProcess();
            if (proc != null) {
                proc.destroy();
            }
            streams.remove(is);
        }
    }

    /**
     * Reads or re-reads the properties file if modified
     */
    private void readProperties() {
        String workPath = "webserver";
        try {
            SageApi.GetProperty("nielm/webserver/root", "webserver");
        } catch (java.lang.reflect.InvocationTargetException e) {
            System.out.println(e);
        }
        if (propsLastLoaded + 30000 > System.currentTimeMillis()) return;
        File propsFile = new File(workPath, "transcode.properties");
        if (propsLastLoaded >= propsFile.lastModified()) return;
        Properties props = new Properties();
        if (propsFile.canRead()) {
            InputStream is = null;
            try {
                is = new java.io.FileInputStream(propsFile);
                props.load(is);
                propsLastLoaded = System.currentTimeMillis();
            } catch (IOException e) {
                Acme.Serve.Serve.extLog("Exception occurred trying to load properties file: " + e.getMessage());
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException e) {
                }
            }
        } else {
            Acme.Serve.Serve.extLog("Unable to load transcode.properties.  Using defaults!");
            props.put(PROP_MAX_STREAMS, "1");
            props.put(PROP_SERVER_BUFFER, "default");
            props.put(PROP_WIN_VLC_WK_DIR, "c:\\Progra~1\\VideoLAN\\VLC");
            props.put(PROP_WIN_VLC_CMD, "vc:\\Progra~1\\VideoLAN\\VLC\\vlc.exe");
            props.put(PROP_UNIX_VLC_WK_DIR, "");
            props.put(PROP_UNIX_VLC_CMD, "/usr/bin/vlc.exe");
            props.put(PROP_VLC_OPTS, "--no-sub-autodetect-file --quiet %LOCAL_PATH% :sout=#transcode{vcodec=%VIDEO_CODEC%," + "vb=%VIDEO_BITRATE%,scale=%SCALE%,acodec=%AUDIO_CODEC%,ab=%AUDIO_BITRATE%,channels=2,threads=2%DEINTERLACE%}" + ":duplicate{dst=std{access=http,mux=ts,dst=%LOCALHOST%:%PORT%}}");
            props.put(PROP_LOCALHOST_ADDR, "127.0.0.1");
            props.put(PROP_START_PORT, "8000");
            props.put(PROP_STOP_PORT, "9000");
            props.put(PROP_MAX_RETRIES, "10");
            props.put(PROP_FILE_EXT + "ts", ".ts");
            props.put(PROP_FILE_EXT + "ps", ".mpg");
            props.put(PROP_FILE_EXT + "asf", ".asf");
            props.put(PROP_FILE_EXT + "mpeg1", ".mpg");
            props.put(PROP_FILE_EXT + "ogg", ".ogg");
            props.put(PROP_FILE_EXT + "mp4", ".mp4");
            props.put(PROP_FILE_EXT + "mov", ".mov");
            props.put(PROP_DEBUG_LOG, "false");
            props.put(PROP_ORB_INSTALLED, "auto");
        }
        maxStreams = Integer.parseInt(props.getProperty(PROP_MAX_STREAMS));
        String serverBufferString = props.getProperty(PROP_SERVER_BUFFER);
        try {
            serverBuffer = Integer.parseInt(serverBufferString) * 1000;
        } catch (NumberFormatException e) {
            serverBuffer = -1;
        }
        boolean IsWin = true;
        try {
            IsWin = net.sf.sageplugins.sageutils.SageApi.booleanApi("IsWindowsOS", null);
        } catch (java.lang.reflect.InvocationTargetException e) {
            System.out.println("VlcTranscodeMgr(): " + e);
        }
        String wkDirStr;
        if (IsWin) {
            wkDirStr = props.getProperty(PROP_WIN_VLC_WK_DIR);
            vlcCmd = props.getProperty(PROP_WIN_VLC_CMD).trim();
        } else {
            wkDirStr = props.getProperty(PROP_UNIX_VLC_WK_DIR);
            vlcCmd = props.getProperty(PROP_UNIX_VLC_CMD).trim();
        }
        if (wkDirStr == null || wkDirStr.trim().length() == 0) {
            vlcWkDir = null;
        } else {
            vlcWkDir = new File(wkDirStr.trim());
        }
        startPort = Integer.parseInt(props.getProperty(PROP_START_PORT));
        stopPort = Integer.parseInt(props.getProperty(PROP_STOP_PORT));
        vlcOpts = props.getProperty(PROP_VLC_OPTS).trim();
        maxRetries = Integer.parseInt(props.getProperty(PROP_MAX_RETRIES));
        try {
            localhostAddr = java.net.InetAddress.getByName("127.0.0.1");
            localhostAddr = java.net.InetAddress.getByName(props.getProperty(PROP_LOCALHOST_ADDR).trim());
        } catch (java.net.UnknownHostException e) {
            Acme.Serve.Serve.extLog("VlcTranscodeMgr(): " + e);
        }
        String debugLogStr = props.getProperty(PROP_DEBUG_LOG);
        if (debugLogStr == null || (!debugLogStr.equalsIgnoreCase("true") && !debugLogStr.equals("1"))) debugLog = false; else debugLog = true;
        fileExtMap.clear();
        java.util.Enumeration<Object> e = props.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(PROP_FILE_EXT)) {
                fileExtMap.put(key.substring(PROP_FILE_EXT.length()), props.getProperty(key));
            }
        }
        transcodeFormatMap.clear();
        String[] keys = (String[]) props.keySet().toArray(new String[0]);
        java.util.Arrays.sort(keys);
        for (int keynum = 0; keynum < keys.length; keynum++) {
            String key = keys[keynum];
            if (key.startsWith(PROP_TRANSCODE_MODES)) {
                String[] mode = key.split("/");
                if (mode.length == 3) {
                    String[] params = new String[] { mode[2], props.getProperty(key) };
                    if (!transcodeFormatMap.containsKey(mode[1])) {
                        Vector<String[]> formats = new Vector<String[]>();
                        formats.add(params);
                        transcodeFormatMap.put(mode[1], formats);
                    } else {
                        Vector<String[]> formats = (Vector<String[]>) transcodeFormatMap.get(mode[1]);
                        formats.add(params);
                    }
                } else {
                    Acme.Serve.Serve.extLog("invalid transcode format name" + key);
                }
            }
        }
        String orbtest = props.getProperty(PROP_ORB_INSTALLED);
        if (orbtest == null || orbtest.trim().equalsIgnoreCase("auto")) orbInstalled = new File("C:\\Program Files\\Orb Networks\\Orb\\bin\\OrbTray.exe").exists(); else {
            orbInstalled = orbtest.trim().equalsIgnoreCase("true");
        }
    }

    private ServerSocket findAvailablePort() {
        int port = startPort;
        ServerSocket socket = null;
        while (socket == null && port <= stopPort) {
            try {
                socket = new ServerSocket(port, 0, localhostAddr);
            } catch (IOException e) {
                socket = null;
                port++;
            }
        }
        return socket;
    }

    private void replaceFirst(StringBuffer buffer, String replace, String replaceWith) {
        if (buffer == null || replace == null || replaceWith == null) {
            return;
        }
        int index = buffer.indexOf(replace);
        if (index >= 0) {
            buffer.delete(index, index + replace.length());
            buffer.insert(index, replaceWith);
        }
    }

    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println("Usage: java VlcTranscodeMgr <file> <video-codec> <video-bitrate-Kb> <audio-codec> <audio-bitrate-Kb> <scale> <deint|nodeint> <mux>");
            System.out.println("  video-codecs:  mp1v, mp2v, mp4v, DIV1, DIV2, DIV3, H263, h264, WMV1, WMV2, MJPG, theo");
            System.out.println("  audio-codecs:  mpga, mp2a, mp3, mp4a, a52, vorb, flac, spx, s16l, fl32");
            System.out.println("  scale:  floating point value specifying the scale to output.  (1=100%, 0.5=50%, etc.)");
            System.exit(0);
        }
        String localPath = args[0];
        String videoCodec = args[1];
        String videoBitrate = args[2];
        String audioCodec = args[3];
        String audioBitrate = args[4];
        String scale = args[5];
        String mux = args[7];
        boolean deint = false;
        if (args[6].equalsIgnoreCase("deint")) deint = true;
        VlcTranscodeMgr mgr = VlcTranscodeMgr.getInstance();
        InputStream is = null;
        try {
            is = mgr.startTranscodeProcess(localPath, videoCodec, Integer.parseInt(videoBitrate), audioCodec, Integer.parseInt(audioBitrate), scale, deint, mux);
            System.out.println("Inputstream: " + is);
            Thread.sleep(20000);
        } catch (Throwable ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     *  Return map of trancode modes from properties file
     *  Map is a 
     *  Map<String{format}, Vector <String[2] { desc, params }>
     */
    public Map<String, List<String[]>> getTranscodeFormatMap() {
        readProperties();
        return java.util.Collections.unmodifiableMap(transcodeFormatMap);
    }

    /**
     * Check if the VLC executable exists
     */
    public boolean isVlcInstalled() {
        try {
            readProperties();
            if (vlcCmd != null && vlcCmd.trim().length() > 0) return new File(vlcCmd).exists();
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * get the file extension for the specified mux
     */
    public String getFileExt(String mux) {
        readProperties();
        return (String) fileExtMap.get(mux);
    }

    public boolean isOrbInstalled() {
        return orbInstalled;
    }
}
