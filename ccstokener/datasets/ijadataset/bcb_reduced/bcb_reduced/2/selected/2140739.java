package vavi.sound.midi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileReader;
import vavi.util.Debug;

/**
 * BasicMidiFileReader. 
 * �p�������N���X�� {@link #getSequence(InputStream)} ���������Ă��������B
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class BasicMidiFileReader extends MidiFileReader {

    /**
     * �p�������N���X�Ŏ������� {@link #getSequence(InputStream)} ��
     * ���������^�C�v����ϊ����ꂽ MIDI Sequence ���擾���܂��B
     * @param stream a midi stream
     * @throws IOException when the I/O does not support marking.  
     */
    public MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException {
        Sequence midiSequence = getSequence(stream);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MidiSystem.write(midiSequence, 0, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        MidiFileFormat midiFF = MidiSystem.getMidiFileFormat(is);
        return midiFF;
    }

    /** {@link #getMidiFileFormat(InputStream)} �ɈϏ� */
    public MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
        Debug.println("file: " + file);
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return getMidiFileFormat(is);
    }

    /** {@link #getMidiFileFormat(InputStream)} �ɈϏ� */
    public MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
        InputStream is = new BufferedInputStream(url.openStream());
        return getMidiFileFormat(is);
    }

    /** {@link #getSequence(InputStream)} �ɈϏ� */
    public Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return getSequence(is);
    }

    /** {@link #getSequence(InputStream)} �ɈϏ� */
    public Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
        InputStream is = new BufferedInputStream(url.openStream());
        return getSequence(is);
    }
}
