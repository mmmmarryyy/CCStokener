package sears.tools.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import sears.gui.MainWindow;
import sears.tools.SearsProperties;
import sears.tools.Trace;
import sears.tools.Utils;
import sun.misc.BASE64Encoder;

/**
 * Class VLCPlayer.
 * <br><b>Summary:</b><br>
 * This class implements the PlayerInterface, and is designed to control the vlc software.
 * VLC is controled through its http server mode.
 * You can found vlc at http://www.videolan.org/
 * @author David DEBARGE
 */
public class VLCPlayer implements PlayerInterface {

    /**The VLC parameter which is used to exec VLC player*/
    public static String vlcParameter;

    /**The hostname uses to connect to VLC in remote control mode*/
    private static String hostName = "localhost";

    /**The default port number uses to connect to VLC in remote control mode*/
    private static final int DEFAULT_PORT = 8080;

    /**The port number uses to connect to VLC in remote control mode*/
    private static int portNumber;

    /**The VLC exec process*/
    private static Process vlcProcess = null;

    /**The VLC current video file name*/
    private static String currentVideoFile = null;

    /**The VLC exec process input stream thread*/
    private static Thread vlcInputStreamThread = null;

    /**The VLC exec process error stream thread*/
    private static Thread vlcErrorStreamThread = null;

    /**The temporary subtitle file which is used by VLC*/
    private static File vlcSubtitleFile = null;

    /**The temporary logo file*/
    private static File vlcLogoFile = null;

    /**The default VLC restart parameter*/
    public static final String DEFAULT_VLC_RESTART = "0";

    /**The empty playlist http request */
    private static final String EMPTY_PLAYLIST_REQUEST = "/?control=empty";

    /**The add playlist http request */
    private static final String ADD_PLAYLIST_REQUEST = "/?control=add&mrl=";

    /**The play http request */
    private static final String PLAY_REQUEST = "/?control=play&item=0";

    /**The replay http request */
    private static final String REPLAY_REQUEST = "/?control=next";

    /**The pause http request */
    private static final String PAUSE_REQUEST = "/?control=pause";

    /**The stop http request */
    private static final String STOP_REQUEST = "/?control=stop";

    /**The seek http request */
    private static final String SEEK_REQUEST = "/?control=seek&seek_value=";

    /**The get time http request */
    private static final String GET_TIME_REQUEST = "/old/admin/dboxfiles.html?stream_time=true";

    /**The get length http request */
    private static final String GET_LENGTH_REQUEST = "/old/admin/dboxfiles.html?stream_length=true";

    /**The quit http request */
    private static final String QUIT_REQUEST = "/old/admin/?control=shutdown";

    /** default VLC path on Mac OS X os*/
    public static final String DEFAULT_VLC_PATH_MAC = "/Applications/VLC.app/Contents/MacOS/VLC";

    /** default VLC path in Window os */
    public static final String DEFAULT_VLC_PATH_LINUX = "/usr/bin/vlc";

    /** default VLC path in Linux os*/
    public static final String DEFAULT_VLC_PATH_WINDOWS = "C:\\Program Files\\VideoLAN\\VLC";

    /**
     * Method constructor.
     * <br><b>Summary:</b><br>
     * The class constructor
     * @param hostName              The <b>String</b> hostname to connect to VLC
     * @param portNumber            The <b>int</b> port number to connect to VLC
     * @throws PlayerException 		if an error occurs
     */
    public VLCPlayer(String hostName, int portNumber) throws PlayerException {
        VLCPlayer.hostName = hostName;
        String portString = SearsProperties.getProperty(SearsProperties.VLC_PORT);
        if (portString == null || portString.equals("")) {
            portString = "" + DEFAULT_PORT;
            SearsProperties.setProperty(SearsProperties.VLC_PORT, "" + DEFAULT_PORT);
        }
        try {
            VLCPlayer.portNumber = Integer.parseInt(SearsProperties.getProperty(SearsProperties.VLC_PORT, "" + VLCPlayer.DEFAULT_PORT));
        } catch (NumberFormatException e) {
            VLCPlayer.portNumber = DEFAULT_PORT;
            SearsProperties.setProperty(SearsProperties.VLC_PORT, "" + DEFAULT_PORT);
        }
        try {
            vlcSubtitleFile = java.io.File.createTempFile("vlcSubtitle", null);
            vlcSubtitleFile.deleteOnExit();
            vlcLogoFile = java.io.File.createTempFile("vlcLogo", ".png");
            vlcLogoFile.deleteOnExit();
        } catch (IOException e) {
            throw new PlayerException("Cannot create the temporary files for VLC");
        }
        vlcParameter = " --nofullscreen" + " --osd" + " --no-sub-autodetect-file" + " --intf http" + " --http-host " + hostName + ":" + portNumber + " --sub-file " + vlcSubtitleFile.getAbsolutePath();
        try {
            InputStream in = MainWindow.instance.getClass().getResourceAsStream("/sears/gui/resources/sears.png");
            OutputStream out = new FileOutputStream(vlcLogoFile);
            Utils.copyStream(in, out);
            vlcParameter += " --logo-file " + vlcLogoFile.getAbsolutePath() + " --sub-filter logo" + " --logo-position 6" + " --logo-transparency 30";
        } catch (Exception e) {
            Trace.trace("Cannot copy the logo file:" + e.getMessage());
            vlcLogoFile = null;
        }
    }

    /**
     * Method constructor.
     * The class constructor using the default value of hostName and portNumber
     * to connect to VLC
     * <br><b>Summary:</b><br>
     * @throws PlayerException if an error occurs
     */
    public VLCPlayer() throws PlayerException {
        this(hostName, portNumber);
    }

    /**
     * Method sendRequest.
     * <br><b>Summary:</b><br>
     * Send http request to VLC.
     * @param   request  			<b>String</b> the http request.
     * @param   useAuthorization  	<b>boolean</b> use Authorization.
     * @param   getResponse  		<b>boolean</b> attempt response or not.
     * @return  Response     		<b>String</b> that correspond to request response.
     */
    private static synchronized String sendRequest(String request, boolean useAuthorization, boolean getResponse) {
        String response = "";
        try {
            URL url = new URL("http://" + hostName + ":" + portNumber + request);
            URLConnection urlC = url.openConnection();
            if (useAuthorization) {
                String loginPassword = "admin:admin";
                BASE64Encoder enc = new sun.misc.BASE64Encoder();
                urlC.setRequestProperty("Authorization", "Basic " + enc.encode(loginPassword.getBytes()));
            }
            InputStream content = (InputStream) urlC.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            if (getResponse) {
                int car = in.read();
                while ((car != -1) && (car != (int) 'D')) {
                    response += new Character((char) car).toString();
                    car = in.read();
                }
            }
            while ((in.readLine()) != null) {
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return response;
    }

    /**
     * Method vlcIsRunning.
     * <br><b>Summary:</b><br>
     * Check if the VLC program is running or not.
     * @return  isRunning     		<b>boolean</b> is running or not.
     */
    private static boolean isRunning() {
        boolean vlcIsRunning = false;
        if (vlcProcess != null) {
            try {
                vlcProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                vlcIsRunning = true;
            }
        }
        return vlcIsRunning;
    }

    public void play(String videoFile, String subtitleFile) throws PlayerException {
        if (videoFile != null) {
            int videoPosition = -1;
            if (isRunning()) {
                videoPosition = getPosition();
            }
            if (SearsProperties.getProperty(SearsProperties.VLC_RESTART, VLCPlayer.DEFAULT_VLC_RESTART).equals("1")) {
                quit();
            }
            try {
                if (subtitleFile != null) {
                    Utils.copyFile(new File(subtitleFile), vlcSubtitleFile);
                } else {
                    OutputStream out = new FileOutputStream(vlcSubtitleFile);
                    out.close();
                }
            } catch (IOException e) {
                throw new PlayerException("Cannot copy the subtitle file");
            }
            if (!isRunning()) {
                quit();
                String vlcFile = SearsProperties.getProperty(SearsProperties.PLAYER_FULL_PATH);
                try {
                    vlcProcess = Runtime.getRuntime().exec(vlcFile + " " + vlcParameter);
                    Thread.sleep(4000);
                    vlcInputStreamThread = new Thread("vlcInputStream") {

                        public void run() {
                            try {
                                BufferedReader is = new BufferedReader(new InputStreamReader(vlcProcess.getInputStream()));
                                while ((is.readLine()) != null) {
                                }
                            } catch (IOException e) {
                                Trace.trace("VLC input stream thread failed:" + e.getMessage());
                            }
                        }
                    };
                    vlcInputStreamThread.start();
                    vlcErrorStreamThread = new Thread("vlcErrorStream") {

                        public void run() {
                            try {
                                BufferedReader is = new BufferedReader(new InputStreamReader(vlcProcess.getErrorStream()));
                                while ((is.readLine()) != null) {
                                }
                            } catch (IOException e) {
                                Trace.trace("VLC error stream thread failed:" + e.getMessage());
                            }
                        }
                    };
                    vlcErrorStreamThread.start();
                } catch (Exception e) {
                    quit();
                    throw new PlayerException("Cannot run the VLC program:" + vlcFile);
                }
            }
            if ((currentVideoFile == null) || (currentVideoFile != videoFile)) {
                sendRequest(EMPTY_PLAYLIST_REQUEST, false, false);
                sendRequest(ADD_PLAYLIST_REQUEST + videoFile.replaceAll(" ", "+"), false, false);
                currentVideoFile = videoFile;
            } else {
                sendRequest(REPLAY_REQUEST, false, false);
            }
            if ((videoPosition != -1) && (videoPosition != 0)) {
                videoPosition -= 5;
                if (videoPosition < 0) videoPosition = 0;
                setPosition(videoPosition);
            }
            sendRequest(PLAY_REQUEST, false, false);
        }
    }

    /**
	 * Empty method
	 * @param offset			the offset
	 * @throws PlayerException	if an error occurs
	 */
    public void goToOffset(int offset) throws PlayerException {
    }

    public void quit() {
        currentVideoFile = null;
        if (vlcProcess != null) {
            try {
                vlcProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                sendRequest(QUIT_REQUEST, true, false);
            }
        }
        if (vlcInputStreamThread != null) {
            vlcInputStreamThread.interrupt();
            vlcInputStreamThread = null;
        }
        if (vlcErrorStreamThread != null) {
            vlcErrorStreamThread.interrupt();
            vlcErrorStreamThread = null;
        }
        if (vlcProcess != null) {
            try {
                vlcProcess.destroy();
            } catch (Exception e) {
            }
            vlcProcess = null;
        }
    }

    public int getPosition() throws PlayerException {
        int timeInSecond = -1;
        if (isRunning()) {
            String response = sendRequest(GET_TIME_REQUEST, true, true);
            timeInSecond = Integer.valueOf(response).intValue();
        }
        return timeInSecond;
    }

    public int getLength() throws PlayerException {
        int timeInSecond = -1;
        if (isRunning()) {
            String response = sendRequest(GET_LENGTH_REQUEST, true, true);
            timeInSecond = Integer.valueOf(response).intValue();
        }
        return timeInSecond;
    }

    public void pause() throws PlayerException {
        if (isRunning()) {
            sendRequest(PAUSE_REQUEST, true, false);
        }
    }

    public void stop() throws PlayerException {
        if (isRunning()) {
            sendRequest(STOP_REQUEST, true, false);
        }
    }

    public void setPosition(int offset) throws PlayerException {
        if (isRunning()) {
            sendRequest(SEEK_REQUEST + Integer.toString(offset), true, false);
        }
    }
}
