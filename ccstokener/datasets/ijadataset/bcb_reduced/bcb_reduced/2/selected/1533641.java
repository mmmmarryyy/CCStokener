package com.usoog.commons.oggplayer;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This is a small wrapper around the JOrbis library to make it a little easier
 * to handle. This is now complete and required an additional layer however,
 * the OggPlayer.
 *
 * @author Jimmy Axenhus
 * @author ymnk, JCraft,Inc.
 */
class JOrbisPlayer implements Runnable {

    /**
	 * For serialization.
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * The current thread the player is running in.
	 */
    private Thread thread;

    /**
	 * The InputStream we use to get data from.
	 */
    private InputStream bitStream = null;

    /**
	 * The UDP port we use for UDP streams, if any.
	 */
    private int udp_port = -1;

    /**
	 * The broadcast address of the UDP stream, aka stramer.
	 */
    private String udp_baddress = null;

    /**
	 * The size of our music buffer.
	 */
    private static final int BUFSIZE = 4096 * 2;

    /**
	 * The size of our...what?
	 */
    static int convsize = BUFSIZE * 2;

    /**
	 * Out buffer of....what?
	 */
    static byte[] convbuffer = new byte[convsize];

    private int RETRY = 3;

    int retry = RETRY;

    SyncState oy;

    StreamState os;

    Page og;

    Packet op;

    Info vi;

    Comment vc;

    DspState vd;

    Block vb;

    byte[] buffer = null;

    int bytes = 0;

    int format;

    int rate = 0;

    int channels = 0;

    SourceDataLine outputLine = null;

    String current_source = null;

    int frameSizeInBytes;

    int bufferLengthInBytes;

    /**
	 * The ogg player we use to notify about new stuff.
	 */
    private OggPlayer oggPlayer;

    /**
	 * Constructor with the required arguments.
	 *
	 * @param player The ogg player we use to notify about new stuff.
	 */
    JOrbisPlayer(OggPlayer player) {
        oggPlayer = player;
        init();
    }

    /**
	 * Initialize this player and prepare it for playback.
	 */
    private void init() {
        oy = new SyncState();
        os = new StreamState();
        og = new Page();
        op = new Packet();
        vi = new Info();
        vc = new Comment();
        vd = new DspState();
        vb = new Block(vd);
        buffer = null;
        bytes = 0;
        oy.init();
    }

    public SourceDataLine getOutputLine(int channels, int rate) {
        if (outputLine == null || this.rate != rate || this.channels != channels) {
            if (outputLine != null) {
                outputLine.drain();
                outputLine.stop();
                outputLine.close();
            }
            init_audio(channels, rate);
            outputLine.start();
        }
        return outputLine;
    }

    private void init_audio(int channels, int rate) {
        try {
            AudioFormat audioFormat = new AudioFormat((float) rate, 16, channels, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            if (!AudioSystem.isLineSupported(info)) {
                return;
            }
            try {
                outputLine = (SourceDataLine) AudioSystem.getLine(info);
                outputLine.open(audioFormat);
            } catch (LineUnavailableException ex) {
                System.out.println("Unable to open the sourceDataLine: " + ex);
                return;
            } catch (IllegalArgumentException ex) {
                System.out.println("Illegal Argument: " + ex);
                return;
            }
            frameSizeInBytes = audioFormat.getFrameSize();
            int bufferLengthInFrames = outputLine.getBufferSize() / frameSizeInBytes / 2;
            bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            this.rate = rate;
            this.channels = channels;
        } catch (Exception ee) {
            System.out.println(ee);
        }
    }

    public void run() {
        Thread me = Thread.currentThread();
        String item = "http://usoog.com:8000/listen.music";
        while (true) {
            bitStream = getInputStream(item);
            if (bitStream != null) {
                play_stream(me);
            }
            if (thread != me) {
                break;
            }
            bitStream = null;
        }
        thread = null;
    }

    private void play_stream(Thread me) {
        boolean chained = false;
        retry = RETRY;
        loop: while (true) {
            int eos = 0;
            int index = oy.buffer(BUFSIZE);
            buffer = oy.data;
            try {
                bytes = bitStream.read(buffer, index, BUFSIZE);
            } catch (Exception e) {
                System.err.println(e);
                return;
            }
            oy.wrote(bytes);
            if (chained) {
                chained = false;
            } else {
                if (oy.pageout(og) != 1) {
                    if (bytes < BUFSIZE) {
                        break;
                    }
                    System.err.println("Input does not appear to be an Ogg bitstream.");
                    return;
                }
            }
            os.init(og.serialno());
            os.reset();
            vi.init();
            if (os.pagein(og) < 0) {
                System.err.println("Error reading first page of Ogg bitstream data.");
                return;
            }
            retry = RETRY;
            if (os.packetout(op) != 1) {
                System.err.println("Error reading initial header packet.");
                break;
            }
            if (vi.synthesis_headerin(vc, op) < 0) {
                System.err.println("This Ogg bitstream does not contain Vorbis audio data.");
                return;
            }
            int i = 0;
            while (i < 2) {
                while (i < 2) {
                    int result = oy.pageout(og);
                    if (result == 0) {
                        break;
                    }
                    if (result == 1) {
                        os.pagein(og);
                        while (i < 2) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }
                            if (result == -1) {
                                System.err.println("Corrupt secondary header.  Exiting.");
                                break loop;
                            }
                            vi.synthesis_headerin(vc, op);
                            i++;
                        }
                    }
                }
                index = oy.buffer(BUFSIZE);
                buffer = oy.data;
                try {
                    bytes = bitStream.read(buffer, index, BUFSIZE);
                } catch (Exception e) {
                    System.err.println(e);
                    return;
                }
                if (bytes == 0 && i < 2) {
                    System.err.println("End of file before finding all Vorbis headers!");
                    return;
                }
                oy.wrote(bytes);
            }
            {
                byte[][] ptr = vc.user_comments;
                List<String> comments = new ArrayList<String>();
                for (int j = 0; j < ptr.length; j++) {
                    if (ptr[j] == null) {
                        break;
                    }
                    String comment = new String(ptr[j], 0, ptr[j].length - 1);
                    System.err.println("Comment: " + comment);
                    comments.add(comment);
                }
                oggPlayer.notifyChangedSong(comments);
                System.err.println("Bitstream is " + vi.channels + " channel, " + vi.rate + "Hz");
                System.err.println("Encoded by: " + new String(vc.vendor, 0, vc.vendor.length - 1) + "\n");
            }
            convsize = BUFSIZE / vi.channels;
            vd.synthesis_init(vi);
            vb.init(vd);
            float[][][] _pcmf = new float[1][][];
            int[] _index = new int[vi.channels];
            getOutputLine(vi.channels, vi.rate);
            while (eos == 0) {
                while (eos == 0) {
                    if (thread != me) {
                        try {
                            bitStream.close();
                            outputLine.drain();
                            outputLine.stop();
                            outputLine.close();
                            outputLine = null;
                        } catch (Exception ee) {
                        }
                        return;
                    }
                    int result = oy.pageout(og);
                    if (result == 0) {
                        break;
                    }
                    if (result == -1) {
                    } else {
                        os.pagein(og);
                        if (og.granulepos() == 0) {
                            chained = true;
                            eos = 1;
                            break;
                        }
                        while (true) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }
                            if (result == -1) {
                            } else {
                                int samples;
                                if (vb.synthesis(op) == 0) {
                                    vd.synthesis_blockin(vb);
                                }
                                while ((samples = vd.synthesis_pcmout(_pcmf, _index)) > 0) {
                                    float[][] pcmf = _pcmf[0];
                                    int bout = (samples < convsize ? samples : convsize);
                                    for (i = 0; i < vi.channels; i++) {
                                        int ptr = i * 2;
                                        int mono = _index[i];
                                        for (int j = 0; j < bout; j++) {
                                            int val = (int) (pcmf[i][mono + j] * 32767.);
                                            if (val > 32767) {
                                                val = 32767;
                                            }
                                            if (val < -32768) {
                                                val = -32768;
                                            }
                                            if (val < 0) {
                                                val |= 0x8000;
                                            }
                                            convbuffer[ptr] = (byte) (val);
                                            convbuffer[ptr + 1] = (byte) (val >>> 8);
                                            ptr += 2 * (vi.channels);
                                        }
                                    }
                                    outputLine.write(convbuffer, 0, 2 * vi.channels * bout);
                                    vd.synthesis_read(bout);
                                }
                            }
                        }
                        if (og.eos() != 0) {
                            eos = 1;
                        }
                    }
                }
                if (eos == 0) {
                    index = oy.buffer(BUFSIZE);
                    buffer = oy.data;
                    try {
                        bytes = bitStream.read(buffer, index, BUFSIZE);
                    } catch (Exception e) {
                        System.err.println(e);
                        return;
                    }
                    if (bytes == -1) {
                        break;
                    }
                    oy.wrote(bytes);
                    if (bytes == 0) {
                        eos = 1;
                    }
                }
            }
            os.clear();
            vb.clear();
            vd.clear();
            vi.clear();
        }
        oy.clear();
        try {
            if (bitStream != null) {
                bitStream.close();
            }
        } catch (Exception e) {
        }
    }

    public void stop() {
        try {
            outputLine.drain();
            outputLine.stop();
            outputLine.close();
            outputLine = null;
            if (bitStream != null) {
                bitStream.close();
            }
        } catch (Exception e) {
        }
        thread = null;
    }

    Vector playlist = new Vector();

    public void play_sound() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    public void stop_sound() {
        if (thread == null) {
            return;
        }
        thread = null;
    }

    private InputStream getInputStream(String item) {
        InputStream is = null;
        URLConnection urlc = null;
        try {
            URL url = new URL(item);
            urlc = url.openConnection();
            is = urlc.getInputStream();
            current_source = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + url.getFile();
        } catch (Exception ee) {
            System.err.println(ee);
        }
        int i = 0;
        udp_port = -1;
        udp_baddress = null;
        while (urlc != null) {
            String s = urlc.getHeaderField(i);
            String t = urlc.getHeaderFieldKey(i);
            if (s == null) {
                break;
            }
            i++;
            if ("udp-port".equals(t)) {
                try {
                    udp_port = Integer.parseInt(s);
                } catch (Exception e) {
                }
            } else if ("udp-broadcast-address".equals(t)) {
                udp_baddress = s;
            }
        }
        return is;
    }
}
