package org.mobicents.media.server.impl.resource.mediaplayer.audio.tts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.sound.sampled.AudioInputStream;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import com.sun.speech.freetts.Voice;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 * @author baranowb
 */
public class TtsTrackImpl implements Track {

    /** audio stream */
    private transient AudioInputStream stream = null;

    private AudioFormat format = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

    private int period = 20;

    private int frameSize;

    private boolean eom;

    private boolean isReady = false;

    private Vector<byte[]> outputList;

    private Voice voice;

    private long duration;

    private long timestamp;

    private VoicesCache voiceCache;

    public TtsTrackImpl(URL url, String voiceName, VoicesCache vc) throws IOException {
        this.voiceCache = vc;
        isReady = false;
        URLConnection connection = url.openConnection();
        frameSize = (int) (period * format.getChannels() * format.getSampleSizeInBits() * format.getSampleRate() / 8000);
        voice = voiceCache.allocateVoice(voiceName);
        TTSAudioBuffer audioBuffer = new TTSAudioBuffer();
        this.voice.setAudioPlayer(audioBuffer);
        this.voice.speak(connection.getInputStream());
        audioBuffer.flip();
    }

    public TtsTrackImpl(String text, String voiceName, VoicesCache vc) {
        this.voiceCache = vc;
        isReady = false;
        voice = voiceCache.allocateVoice(voiceName);
        TTSAudioBuffer audioBuffer = new TTSAudioBuffer();
        voice.setAudioPlayer(audioBuffer);
        voice.speak(text);
        audioBuffer.flip();
        frameSize = (int) (period * format.getChannels() * format.getSampleSizeInBits() * format.getSampleRate() / 8000);
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSizeInBits() * format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return timestamp;
    }

    public void setMediaTime(long timestamp) {
        this.timestamp = timestamp;
        try {
            stream.reset();
            long offset = frameSize * (timestamp / period);
            stream.skip(offset);
        } catch (IOException e) {
        }
    }

    public long getDuration() {
        return duration;
    }

    /**
     * Reads packet from currently opened stream.
     * 
     * @param packet
     *            the packet to read
     * @param offset
     *            the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        int length = 0;
        try {
            while (length < psize) {
                int len = stream.read(packet, offset + length, psize - length);
                if (len == -1) {
                    return length;
                }
                length += len;
            }
            return length;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = 0;
        }
    }

    private void switchEndian(byte[] b, int off, int readCount) {
        for (int i = off; i < (off + readCount); i += 2) {
            byte temp;
            temp = b[i];
            b[i] = b[i + 1];
            b[i + 1] = temp;
        }
    }

    public void process(Buffer buffer) throws IOException {
        if (!isReady) {
            buffer.setDiscard(true);
            return;
        }
        byte[] data = buffer.getData();
        if (data == null) {
            data = new byte[frameSize];
        }
        buffer.setData(data);
        int len = readPacket(data, 0, frameSize);
        if (len == 0) {
            eom = true;
        }
        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }
        buffer.setOffset(0);
        buffer.setLength(frameSize);
        buffer.setEOM(eom);
        buffer.setDuration(20);
    }

    public void close() {
        try {
            voiceCache.releaseVoice(this.voice);
            this.voice = null;
            stream.close();
        } catch (Exception e) {
        }
    }

    private class TTSAudioBuffer implements com.sun.speech.freetts.audio.AudioPlayer {

        private javax.sound.sampled.AudioFormat fmt;

        private float volume;

        private byte[] localBuff;

        private int curIndex = 0;

        private int totalBytes = 0;

        public TTSAudioBuffer() {
            outputList = new Vector<byte[]>();
        }

        public void setAudioFormat(javax.sound.sampled.AudioFormat fmt) {
            this.fmt = fmt;
        }

        public javax.sound.sampled.AudioFormat getAudioFormat() {
            return fmt;
        }

        public void pause() {
        }

        public void resume() {
        }

        public void reset() {
            curIndex = 0;
            localBuff = null;
            isReady = false;
        }

        public boolean drain() {
            return true;
        }

        public void begin(int size) {
            localBuff = new byte[size];
            curIndex = 0;
        }

        public boolean end() {
            outputList.add(localBuff);
            totalBytes += localBuff.length;
            System.out.println("end() called totalBytes = " + totalBytes);
            isReady = true;
            return true;
        }

        public void cancel() {
        }

        public void close() {
        }

        public void flip() {
            byte[] rawData = null;
            if (outputList.size() == 1) {
                rawData = outputList.firstElement();
            } else {
                int offSet = 0;
                rawData = new byte[totalBytes];
                for (byte[] byteArr : outputList) {
                    System.arraycopy(byteArr, 0, rawData, offSet, byteArr.length);
                    offSet += byteArr.length;
                }
            }
            if (fmt.isBigEndian()) {
                switchEndian(rawData, 0, rawData.length);
                fmt = new javax.sound.sampled.AudioFormat(fmt.getEncoding(), fmt.getSampleRate(), fmt.getSampleSizeInBits(), fmt.getChannels(), fmt.getFrameSize(), fmt.getFrameRate(), false);
            }
            duration = (long) (totalBytes * 1000 / (fmt.getSampleSizeInBits() / 8 * fmt.getSampleRate()));
            if (fmt.getSampleRate() != 8000) {
                double originalFrequency = fmt.getSampleRate();
                double targetFrequency = 8000;
                double targetDX = 1 / targetFrequency;
                double originalDX = 1 / originalFrequency;
                int byteCount = fmt.getSampleSizeInBits() / 8;
                int originalSampleCount = totalBytes / byteCount;
                int[] originalSignal = new int[originalSampleCount];
                int j = 0;
                for (int i = 0; i < originalSignal.length; i++) {
                    originalSignal[i] = (rawData[j++] & 0xff) | (rawData[j++] << 8);
                }
                double ratio = fmt.getSampleRate() / 8000;
                int count = (int) (originalSignal.length / ratio);
                int[] resampledSignal = new int[count];
                for (int k = 0; k < count; k++) {
                    double xk = (double) targetDX * k;
                    int i = (int) (xk / originalDX);
                    double tang = (originalSignal[i + 1] - originalSignal[i]) / (originalDX);
                    double resampled = originalSignal[i] + (xk - i * originalDX) * tang;
                    resampledSignal[k] = (int) resampled;
                }
                rawData = new byte[resampledSignal.length * 2];
                j = 0;
                for (int i = 0; i < resampledSignal.length; i++) {
                    rawData[j++] = (byte) (resampledSignal[i]);
                    rawData[j++] = (byte) (resampledSignal[i] >> 8);
                }
                javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(fmt.getEncoding(), 8000, fmt.getSampleSizeInBits(), fmt.getChannels(), fmt.getFrameSize(), 8000, fmt.isBigEndian());
            }
            InputStream is = new ByteArrayInputStream(rawData);
            stream = new AudioInputStream(is, fmt, rawData.length / fmt.getFrameSize());
        }

        public float getVolume() {
            return volume;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public long getTime() {
            return 0;
        }

        public void resetTime() {
        }

        public void startFirstSampleTimer() {
        }

        public boolean write(byte[] buff) {
            return write(buff, 0, buff.length);
        }

        public boolean write(byte[] buff, int off, int len) {
            System.arraycopy(buff, off, localBuff, curIndex, len);
            curIndex += len;
            return true;
        }

        public void showMetrics() {
        }
    }

    public Format getFormat() {
        return format;
    }
}
