package com.neoworks.shout;

import java.net.URL;
import java.net.URLEncoder;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Category;
import com.neoworks.jukex.ControlPlayback;
import com.neoworks.jukex.Track;
import com.neoworks.jukex.TrackStoreFactory;
import com.neoworks.jukex.Attribute;
import com.neoworks.jukex.tracksource.TrackSource;
import com.neoworks.mpeg.MPEGFrame;
import com.neoworks.mpeg.MPEGStream;

/**
 * The shouter thread. This class gets Tracks from a TrackSource and shouts them to a
 * [Shout|Ice]Cast server.
 *
 * @author Nigel Atkinson (<a href="mailto:nigel@neoworks.com">nigel@neoworks.com</a>)
 */
public class Shouter implements Runnable, ControlPlayback {

    private static final Category log = Category.getInstance(Shouter.class.getName());

    private static final boolean logDebugEnabled = log.isDebugEnabled();

    private static final boolean logInfoEnabled = log.isInfoEnabled();

    private static final int outputBufferSize = 4096;

    private static final int msPerFrame = 26;

    private DataOutputStream ps = null;

    private Socket trackInfoSock = null;

    private DataOutputStream trackInfoStream = null;

    private Stream stream;

    private TrackSource playlist;

    private Track playing = null;

    private Track nextTrack = null;

    private long trackPlayingTime = 0;

    private long trackPosition = 0;

    private boolean badBitrate = false;

    private Thread kicker = null;

    private boolean connected = false;

    private boolean stop = false;

    private boolean next = false;

    private boolean pause = false;

    private static byte[] nullBuffer = null;

    /**
	 * Static initialiser
	 */
    static {
        nullBuffer = new byte[outputBufferSize];
        Arrays.fill(nullBuffer, (byte) 0);
    }

    /**
	 * Public constructor
	 */
    Shouter() {
    }

    /**
	 * Get the Track that is currently being played (or is about to be played)
	 *
	 * @return The currently playing Track
	 */
    public synchronized Track getPlaying() {
        if (nextTrack != null) return nextTrack;
        return this.playing;
    }

    /**
	 * Get the remaining time for the current Track
	 *
	 * @return The remaining playing time in milliseconds
	 */
    public synchronized long getRemainingPlayingTime() {
        if (logInfoEnabled) log.info("track play time: " + trackPlayingTime + " current pos: " + trackPosition);
        return trackPlayingTime - trackPosition;
    }

    /**
	 * Get the playing time for the current Track
	 *
	 * @return The playing time in milliseconds
	 */
    public synchronized long getPlayingTime() {
        return trackPlayingTime;
    }

    /**
	 * Get the current TrackSource
	 *
	 * @return The TrackSource
	 */
    public TrackSource getTrackSource() {
        return this.playlist;
    }

    /**
	 * Set the TrackSource
	 *
	 * @param p The TrackSource to request tracks from 
	 */
    public void setTrackSource(TrackSource p) {
        if (logDebugEnabled) log.debug("+++ setting tracksource to " + p.getName());
        this.playlist = p;
    }

    /**
	 * Set the Stream descriptor
	 *
	 * @param s The Stream descriptor object
	 */
    public void setStream(Stream s) {
        this.stream = s;
    }

    /**
	 *(Get the next track to play
	 *
	 * @return An InputStream on the file at the track URL
	 */
    private synchronized InputStream getNextTrack() {
        String request = null;
        int trackBitrate = 0;
        boolean trackVBR = false;
        String trackTitle = null;
        String trackArtist = null;
        String htmlSafeTrackTitle = null;
        String htmlSafeTrackArtist = null;
        URL url = null;
        InputStream retVal = null;
        MPEGStream durationStream = null;
        badBitrate = false;
        trackPosition = 0;
        trackPlayingTime = 0;
        if (nextTrack == null) {
            nextTrack = playlist.getNextTrack();
        }
        if (nextTrack != null) {
            playing = nextTrack;
            nextTrack = null;
            trackTitle = playing.getAttributeValue("Title").getString();
            trackArtist = playing.getAttributeValue("Artist").getString();
            try {
                htmlSafeTrackTitle = fixEncoding(URLEncoder.encode(trackTitle, "UTF-8"));
                htmlSafeTrackArtist = fixEncoding(URLEncoder.encode(trackArtist, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.warn("Could not encode track details", e);
                htmlSafeTrackTitle = "borken";
                htmlSafeTrackArtist = "borken";
            }
            if (htmlSafeTrackTitle.equals("")) {
                htmlSafeTrackTitle = "NONE SPECIFIED";
            }
            if (htmlSafeTrackArtist.equals("")) {
                htmlSafeTrackArtist = "NONE SPECIFIED";
            }
            trackBitrate = playing.getAttributeValue("Bitrate").getInt();
            trackVBR = (playing.getAttributeValue("VBR").getInt() == 1) ? true : false;
            if (trackBitrate > 448 || trackBitrate < 32) {
                log.warn("Track: " + trackArtist + " - " + trackTitle + "BAD BITRATE: " + trackBitrate);
                badBitrate = true;
            }
            if (logInfoEnabled) log.info("+++ playing track " + trackTitle + "(" + trackBitrate + " kbps)" + (trackVBR ? " VBR" : ""));
            url = playing.getURL();
            if (logDebugEnabled) log.debug("+++ reading from URL: " + url.toString());
            try {
                retVal = url.openStream();
                durationStream = new MPEGStream(retVal);
                trackPlayingTime = durationStream.getPlayingTime();
                durationStream.close();
                durationStream = null;
                retVal = url.openStream();
                request = outputTrackInfo(stream.getPassword(), htmlSafeTrackArtist, htmlSafeTrackTitle, stream.getURL(), stream.getServerName(), stream.getTrackInfoPort());
                if (logDebugEnabled) log.debug("+++ sent track info request: " + request);
                if (logDebugEnabled) log.debug(retVal.available() + " bytes in file");
            } catch (FileNotFoundException e) {
                log.warn("Could not find file " + url.toString());
            } catch (IOException ioe) {
                log.warn("Exception getting stream from track URL", ioe);
            }
        }
        return retVal;
    }

    /**
	 * Start the shouter thread
	 */
    public synchronized void play() {
        if (kicker == null || !kicker.isAlive()) {
            kicker = null;
            if (logInfoEnabled) log.info("+++ starting shouter thread...");
            try {
                kicker = new Thread(this);
            } catch (Exception e) {
                log.warn("Exception encountered while starting kicker thread", e);
            }
            stop = false;
            kicker.start();
        } else {
            if (logInfoEnabled) log.info("+++ resuming...");
            stop = false;
            pause = false;
        }
    }

    /**
	 * Shutdown the shouter
	 */
    public void cpStop() {
        if (connected) {
            if (logDebugEnabled) log.debug("+++ stopping stream...");
            stop = true;
        }
    }

    /**
	 * Pause the stream
	 */
    public void pause() {
        if (connected) {
            pause = true;
        }
    }

    /**
	 * Test whether the shouter is in pause mode
	 *
	 * @return boolean indicating whether the Shouter is paused
	 */
    public boolean isPaused() {
        return pause;
    }

    /**
	 * Skip to the next track
	 */
    public synchronized void skip() {
        if (connected && !stop) {
            nextTrack = playlist.getNextTrack();
            next = true;
        }
    }

    /**
	 * The shouter thread
	 */
    public void run() {
        Socket sock = null;
        BufferedReader reader = null;
        InputStream mp3Is = null;
        MPEGFrame mp3Frame = null;
        ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
        double mark = 0.0;
        long fileByteCount = 0;
        int chunkByteCount = 0;
        long fileStreamingTimeMillis = 0;
        long startMark = 0;
        int amount = 0;
        MPEGStream mp3BS = null;
        double frameCount = 0.0;
        int chunks = 0;
        byte[] frameBuffer = null;
        int remBytes = 0;
        int fragBytes = 0;
        int streamingBitrate = 0;
        if (logInfoEnabled) log.info("+++ playing track source: " + playlist.getName());
        try {
            while (sock == null && kicker != null) {
                if (stop) {
                    break;
                }
                try {
                    if (logInfoEnabled) log.info("+++ attempting to connect to server " + stream.getServerName() + ":" + stream.getPort() + "...");
                    sock = new Socket(stream.getServerName(), stream.getPort());
                    reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    ps = new DataOutputStream(sock.getOutputStream());
                } catch (Exception e) {
                    if (logInfoEnabled) log.info("--- error connecting, retrying...\n");
                    sock = null;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ev) {
                    }
                }
            }
            if (serverLogin(reader)) {
                sendInfo(stream.getStreamName());
                if (logDebugEnabled) log.debug("+++ connected to server " + stream.getServerName());
                connected = true;
                while (sock != null && kicker != null && reader != null) {
                    if (stop) {
                        break;
                    }
                    mp3Is = getNextTrack();
                    if (mp3Is != null) {
                        mp3BS = new MPEGStream(mp3Is);
                        fragBytes = 0;
                        amount = 1;
                        chunks = 0;
                        startMark = System.currentTimeMillis();
                        if (logDebugEnabled) log.debug("Setting mark");
                        mark = (double) System.currentTimeMillis();
                        while (amount > 0) {
                            if (stop || next) {
                                if (logDebugEnabled) log.debug("+++ stopping...");
                                break;
                            } else if (pause) {
                                if (logInfoEnabled) log.info("+++ paused...");
                                Thread.sleep(2000);
                                mark = (double) System.currentTimeMillis();
                                continue;
                            } else {
                                frameCount = 0.0;
                                chunkByteCount = 0;
                                if (fragBytes > 0) {
                                    remBytes = amount - fragBytes;
                                    streamBuffer.write(frameBuffer, fragBytes, remBytes);
                                    chunkByteCount += remBytes;
                                    fileByteCount += remBytes;
                                    frameCount += (double) ((double) remBytes / (double) amount);
                                }
                                fragBytes = 0;
                                while (chunkByteCount < outputBufferSize) {
                                    mp3Frame = mp3BS.readFrame();
                                    if (mp3Frame != null) {
                                        frameBuffer = mp3Frame.getFrameData();
                                        amount = mp3Frame.getFrameSize();
                                        trackPosition += msPerFrame;
                                        if (logDebugEnabled) log.debug(mp3Frame.toString());
                                        if (frameBuffer.length != amount) {
                                            log.warn("PANIC: Buffer does not contain correct data, bombing out...");
                                            amount = 0;
                                            break;
                                        }
                                        if ((chunkByteCount + amount) <= outputBufferSize) {
                                            chunkByteCount += amount;
                                            fileByteCount += amount;
                                            frameCount++;
                                            streamBuffer.write(frameBuffer, 0, amount);
                                        } else {
                                            fragBytes = outputBufferSize - chunkByteCount;
                                            streamBuffer.write(frameBuffer, 0, fragBytes);
                                            frameCount += (double) ((double) fragBytes / (double) amount);
                                            chunkByteCount += fragBytes;
                                            fileByteCount += fragBytes;
                                        }
                                        streamBuffer.flush();
                                    } else {
                                        log.info("Null frame");
                                        remBytes = outputBufferSize - chunkByteCount;
                                        streamBuffer.write(nullBuffer, 0, remBytes);
                                        streamBuffer.flush();
                                        chunkByteCount += remBytes;
                                        fileByteCount += remBytes;
                                        amount = 0;
                                        break;
                                    }
                                }
                                if (logDebugEnabled) log.debug("byte count = " + chunkByteCount + " (" + (((double) chunkByteCount * (double) 8) / ((double) frameCount * (double) msPerFrame)) + " kb/s)");
                                if (logDebugEnabled) log.debug(frameCount + " frames, to play in " + ((frameCount * msPerFrame)) + "ms");
                                if (chunkByteCount != outputBufferSize) {
                                    log.warn("PANIC: Output buf size is incorrect");
                                }
                                mark += (frameCount * (double) msPerFrame);
                                if ((mark - (System.currentTimeMillis())) > 0) {
                                    Thread.sleep((long) (mark - (double) System.currentTimeMillis()));
                                }
                                ps.write(streamBuffer.toByteArray(), 0, chunkByteCount);
                                ps.flush();
                                streamBuffer.reset();
                                chunks++;
                            }
                        }
                        fileStreamingTimeMillis = System.currentTimeMillis() - startMark;
                        streamingBitrate = (int) ((fileStreamingTimeMillis > 0) ? (fileByteCount * 8 / fileStreamingTimeMillis) : -1);
                        if (logInfoEnabled) log.info("Streamed " + fileByteCount + " bytes in " + chunks + " chunks, during " + fileStreamingTimeMillis + "ms at " + streamingBitrate + " kbits/sec");
                        fileByteCount = 0;
                        if (badBitrate) {
                            Attribute bitrateAttribute = TrackStoreFactory.getTrackStore().getAttribute("Bitrate");
                            playing.replaceAttributeValues(bitrateAttribute, bitrateAttribute.getAttributeValue(streamingBitrate));
                            log.warn("Setting bitrate attribute to " + streamingBitrate);
                        }
                        mp3BS.close();
                        mp3Is.close();
                        if (next) {
                            if (logInfoEnabled) log.info("+++ skipping...");
                            next = false;
                        }
                        if (logDebugEnabled) log.debug("+++ finished file, getting next track");
                    } else {
                        if (logInfoEnabled) log.info("--- no track to play, going to sleep");
                        Thread.sleep(5000);
                    }
                }
                if (!stop) {
                    log.warn("--- something bad happened, closing sockets");
                    Thread.sleep(1000);
                } else {
                    if (logInfoEnabled) log.info("+++ STOP signal received, shutting down");
                }
            }
            connected = false;
            reader.close();
            ps.close();
            sock = null;
        } catch (Exception e) {
            log.warn("Exception encountered while playing", e);
            e.printStackTrace();
        }
        playing = null;
        connected = false;
    }

    private boolean serverLogin(BufferedReader reader) {
        boolean retVal = true;
        output(stream.getPassword() + "\n");
        try {
            String response = null;
            while (response == null) {
                response = reader.readLine();
            }
            if (response.equals("ERROR")) {
                log.warn("Error connecting to server, bad password");
                return false;
            }
        } catch (Exception e) {
            log.warn("Exception while reading data from the server", e);
            return false;
        }
        return retVal;
    }

    /**
	 * Send stream info to the server
	 *
	 * @param Stream name
	 */
    private void sendInfo(String name) {
        output("icy-name: " + name + "\n");
        output("icy-url: " + stream.getURL() + "\n");
        output("icy-pub: " + stream.getPublicShout() + "\n");
        output("icy-br: " + stream.getBitrate() + "\n");
        output("icy-desc: " + stream.getStreamDescription() + "\n");
        output("\n");
    }

    /**
	 * Output strings to the server (track info, etc.)
	 *
	 * @param str The String to output
	 * @return Success
	 */
    private boolean output(String str) {
        try {
            ps.writeBytes(str);
            ps.flush();
            if (logDebugEnabled) log.debug("Sending: " + str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Output raw data to the server (mp3s)
	 *
	 * @param bytes The data to send
	 * @param offset Offset into the data to start from
	 * @param length The length of the data block
	 * @return Success
	 */
    private boolean output(byte bytes[], int offset, int length) {
        try {
            ps.write(bytes, offset, length);
            ps.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Output track info to the server
	 *
	 * @param str The String to output
	 * @return Success
	 */
    private String outputTrackInfo(String password, String artist, String title, String url, String servername, int serverport) {
        StringBuffer request = new StringBuffer("GET /admin.cgi?pass=");
        request.append(password);
        request.append("&mode=updinfo&song=");
        request.append(artist);
        request.append("%20-%20");
        request.append(title);
        request.append("&url=");
        request.append(url);
        request.append(" HTTP/1.0\nUser-Agent: Telnet/NeoWorks JukeX 1.0\n\n");
        try {
            trackInfoSock = new Socket(servername, serverport);
            trackInfoStream = new DataOutputStream(trackInfoSock.getOutputStream());
            trackInfoStream.writeBytes(request.toString());
            trackInfoStream.flush();
            trackInfoStream.close();
            trackInfoSock = null;
            return request.toString();
        } catch (Exception e) {
            log.warn(e);
            return null;
        }
    }

    /**
	 * Convert application/x-www-form-urlencoded String to a URL safe String
	 *
	 * @param encStr The application/x-www-form-urlencoded String
	 * @return The URL safe String
	 */
    private String fixEncoding(String encStr) {
        Pattern p = Pattern.compile("\\+");
        Matcher m = p.matcher(encStr);
        return m.replaceAll("%20");
    }
}
