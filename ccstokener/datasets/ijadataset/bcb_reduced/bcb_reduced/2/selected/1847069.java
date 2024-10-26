package org.xith3d.sound.loaders;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import org.xith3d.Xith3DDefaults;
import org.xith3d.sound.BufferFormat;
import org.xith3d.sound.SoundBuffer;
import org.xith3d.sound.SoundDataContainer;
import org.xith3d.sound.SoundDriver;
import org.xith3d.utility.logs.Log;
import org.xith3d.utility.logs.LogType;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * Loads Sound data from OGG Vorbis files.
 * 
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus) [code cleaning]
 */
public class OggLoader implements SoundDataContainer {

    private ByteBuffer bbuffer;

    private SoundBuffer buffer = null;

    private byte[] buf = null;

    private int bytes = 0;

    private static int bufferMultiple_ = 4;

    /**
     * The File Load Play Buffer.
     */
    private static int bufferSize_ = bufferMultiple_ * 256 * 2 * 10;

    /**
     * Description of the Field
     */
    private static int convsize = bufferSize_ * 2;

    private byte[] convbuffer = new byte[convsize];

    private InputStream oggBitStream_ = null;

    private SyncState oggSyncState_;

    private StreamState oggStreamState_;

    private Page oggPage_;

    private Packet oggPacket_;

    private Info vorbisInfo;

    private Comment vorbisComment;

    private DspState vorbisDspState;

    private Block vorbisBlock;

    private int index = 0;

    private long calcLength = 0;

    private float volumeMultiplier = 1;

    public OggLoader() {
    }

    /**
     * Initializes all the jOrbis and jOgg vars that are used for song playback.
     */
    private void init_jorbis() {
        oggSyncState_ = new SyncState();
        oggStreamState_ = new StreamState();
        oggPage_ = new Page();
        oggPacket_ = new Packet();
        vorbisInfo = new Info();
        vorbisComment = new Comment();
        vorbisDspState = new DspState();
        vorbisBlock = new Block(vorbisDspState);
        buffer = null;
        bytes = 0;
        oggSyncState_.init();
    }

    /**
     * Reads from the oggBitStream_ a specified number of Bytes(bufferSize_)
     * worth sarting at index and puts them in the specified buffer[].
     * 
     * @param buffer
     * @param index
     * @param bufferSize_
     * @return the number of bytes read or -1 if error.
     */
    private int readFromStream(byte[] buffer, int index, int bufferSize_) {
        int bytes = 0;
        try {
            bytes = oggBitStream_.read(buffer, index, bufferSize_);
        } catch (Exception e) {
            System.out.println("Cannot read from file");
            bytes = -1;
        }
        return (bytes);
    }

    /**
     * Loads the file from an URL.
     */
    public void load(URL url) throws IOException {
        try {
            oggBitStream_ = new BufferedInputStream(url.openStream());
        } catch (Exception ex) {
            System.err.println("ogg file " + url + " could not be loaded");
        }
        load();
    }

    /**
     * Loads the file from an input stream.
     */
    public void load(InputStream data) throws IOException {
        oggBitStream_ = new BufferedInputStream(data);
        load();
    }

    /**
     * Loads the specified file. This method does not automatically search in
     * the classpath. For jar support load from an URL.
     */
    public void load(String filename) throws IOException {
        oggBitStream_ = new FileInputStream(filename);
        try {
            oggBitStream_ = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        load();
    }

    private void load() {
        init_jorbis();
        if (Xith3DDefaults.getLocalDebug(DEBUG)) Log.print(LogType.DEBUG, "Init file...");
        index = oggSyncState_.buffer(bufferSize_);
        buf = oggSyncState_.data;
        bytes = readFromStream(buf, index, bufferSize_);
        if (bytes == -1) {
            System.err.println("Cannot get any data from selected Ogg bitstream.");
            return;
        }
        oggSyncState_.wrote(bytes);
        if (oggSyncState_.pageout(oggPage_) != 1) {
            if (bytes < bufferSize_) {
                return;
            }
            System.err.println("Input does not appear to be an Ogg bitstream.");
            return;
        }
        oggStreamState_.init(oggPage_.serialno());
        vorbisInfo.init();
        vorbisComment.init();
        if (oggStreamState_.pagein(oggPage_) < 0) {
            System.err.println("Error reading first page of Ogg bitstream data.");
            return;
        }
        if (oggStreamState_.packetout(oggPacket_) != 1) {
            System.err.println("Error reading initial header packet.");
            return;
        }
        if (vorbisInfo.synthesis_headerin(vorbisComment, oggPacket_) < 0) {
            System.err.println("This Ogg bitstream does not contain Vorbis audio data.");
            return;
        }
        int i = 0;
        while (i < 2) {
            while (i < 2) {
                int result = oggSyncState_.pageout(oggPage_);
                if (result == 0) {
                    break;
                }
                if (result == 1) {
                    oggStreamState_.pagein(oggPage_);
                    while (i < 2) {
                        result = oggStreamState_.packetout(oggPacket_);
                        if (result == 0) {
                            break;
                        }
                        if (result == -1) {
                            System.err.println("Corrupt secondary header.  Exiting.");
                            return;
                        }
                        vorbisInfo.synthesis_headerin(vorbisComment, oggPacket_);
                        i++;
                    }
                }
            }
            index = oggSyncState_.buffer(bufferSize_);
            buf = oggSyncState_.data;
            bytes = readFromStream(buf, index, bufferSize_);
            if (bytes == -1) {
                break;
            }
            if (bytes == 0 && i < 2) {
                System.err.println("End of file before finding all Vorbis  headers!");
                return;
            }
            oggSyncState_.wrote(bytes);
        }
        convsize = bufferSize_ / vorbisInfo.channels;
        if (Xith3DDefaults.getLocalDebug(DEBUG)) Log.print(LogType.DEBUG, "convsize = " + convsize);
    }

    public int getNumChannels() {
        return (vorbisInfo.channels);
    }

    public void setVolumeMultiplier(float v) {
        volumeMultiplier = v;
    }

    public int getFreq() {
        return (vorbisInfo.rate);
    }

    /**
     * Can only be called after you get the data. Otherwise it will just return
     * zero.
     * 
     * @return The decoded size of the sound stream.
     */
    public long getDecodedSize() {
        return (calcLength);
    }

    /**
     * Currently always returns false, because streaming is not supported.
     */
    public boolean isStreaming() {
        return (false);
    }

    public SoundBuffer getData(SoundDriver driver) {
        if (buffer != null) return (buffer);
        LinkedList<byte[]> chunks = new LinkedList<byte[]>();
        int totalBytes = 0;
        int curBytes = 0;
        vorbisDspState.synthesis_init(vorbisInfo);
        vorbisBlock.init(vorbisDspState);
        int eos = 0;
        float[][][] _pcmf = new float[1][][];
        int[] _index = new int[vorbisInfo.channels];
        while (eos == 0) {
            while (eos == 0) {
                int result = oggSyncState_.pageout(oggPage_);
                if (result == 0) {
                    break;
                }
                if (result == -1) {
                    System.err.println("Corrupt or missing data in bitstream; " + "continuing...");
                } else {
                    oggStreamState_.pagein(oggPage_);
                    while (true) {
                        result = oggStreamState_.packetout(oggPacket_);
                        if (result == 0) {
                            break;
                        }
                        if (result == -1) {
                        } else {
                            int samples;
                            if (vorbisBlock.synthesis(oggPacket_) == 0) {
                                vorbisDspState.synthesis_blockin(vorbisBlock);
                            }
                            while ((samples = vorbisDspState.synthesis_pcmout(_pcmf, _index)) > 0) {
                                float[][] pcmf = _pcmf[0];
                                int bout = (samples < convsize ? samples : convsize);
                                double fVal = 0.0;
                                for (int i = 0; i < vorbisInfo.channels; i++) {
                                    int pointer = i * 2;
                                    int mono = _index[i];
                                    for (int j = 0; j < bout; j++) {
                                        fVal = (float) pcmf[i][mono + j] * 32767.;
                                        fVal = fVal * volumeMultiplier;
                                        int val = (int) (fVal);
                                        if (val > 32767) {
                                            val = 32767;
                                        }
                                        if (val < -32768) {
                                            val = -32768;
                                        }
                                        if (val < 0) {
                                            val = val | 0x8000;
                                        }
                                        convbuffer[pointer] = (byte) (val);
                                        convbuffer[pointer + 1] = (byte) (val >>> 8);
                                        pointer += 2 * (vorbisInfo.channels);
                                    }
                                }
                                curBytes = 2 * vorbisInfo.channels * bout;
                                totalBytes += curBytes;
                                byte[] chunk = new byte[curBytes];
                                System.arraycopy(convbuffer, 0, chunk, 0, curBytes);
                                chunks.add(chunk);
                                vorbisDspState.synthesis_read(bout);
                            }
                        }
                    }
                    if (oggPage_.eos() != 0) {
                        eos = 1;
                    }
                }
            }
            if (eos == 0) {
                index = oggSyncState_.buffer(bufferSize_);
                buf = oggSyncState_.data;
                bytes = readFromStream(buf, index, bufferSize_);
                if (bytes == -1) {
                    eos = 1;
                } else {
                    oggSyncState_.wrote(bytes);
                    if (bytes == 0) {
                        eos = 1;
                    }
                }
            }
        }
        oggStreamState_.clear();
        vorbisBlock.clear();
        vorbisDspState.clear();
        vorbisInfo.clear();
        buffer = driver.allocateSoundBuffer();
        BufferFormat format = BufferFormat.MONO16;
        if (getNumChannels() == 2) format = BufferFormat.STEREO16;
        calcLength = totalBytes;
        bbuffer = ByteBuffer.allocateDirect(totalBytes);
        bbuffer.order(ByteOrder.nativeOrder());
        for (byte[] chunk : chunks) bbuffer.put(chunk);
        buffer.setData(format, totalBytes, getFreq(), bbuffer);
        return (buffer);
    }

    public void returnData(SoundDriver driver, SoundBuffer buffer) {
    }

    public void rewind(SoundDriver driver) {
    }
}
