package org.jsresources.apps.jam.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jsresources.apps.jam.Debug;

/**
 */
public class ParsedAudioStream {

    private static int BUFFER_SIZE = 65536;

    private URL m_url;

    private AudioFormat m_format;

    private byte[] m_data;

    public ParsedAudioStream(URL url, AudioFormat audioFormat) {
        m_url = url;
        Debug.out("url: " + url);
        InputStream inputStream = null;
        try {
            inputStream = m_url.openStream();
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
            m_format = audioFormat;
            AudioFormat originalAudioFormat = audioStream.getFormat();
            if (!audioFormat.matches(originalAudioFormat)) {
                Debug.out("ParsedAudioStream.<init>(): audio formats do not match, trying to convert.");
                Debug.out("ParsedAudioStream.<init>(): source format: " + originalAudioFormat);
                Debug.out("ParsedAudioStream.<init>(): target format: " + audioFormat);
                AudioInputStream asold = audioStream;
                audioStream = AudioSystem.getAudioInputStream(getFormat(), asold);
                if (audioStream == null) {
                    Debug.out("ParsedAudioStream.<init>(): could not convert!");
                    Debug.out("ParsedAudioStream.<init>(): URL: " + url);
                    return;
                }
            }
            ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int bytesRead = audioStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                bAOS.write(buffer, 0, bytesRead);
            }
            m_data = bAOS.toByteArray();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public URL getURL() {
        return m_url;
    }

    public AudioFormat getFormat() {
        return m_format;
    }

    public AudioInputStream getAudioInputStream() {
        InputStream inputStream = new ByteArrayInputStream(getData());
        long lFrames = getData().length / getFormat().getFrameSize();
        return new AudioInputStream(inputStream, getFormat(), lFrames);
    }

    private byte[] getData() {
        return m_data;
    }
}
