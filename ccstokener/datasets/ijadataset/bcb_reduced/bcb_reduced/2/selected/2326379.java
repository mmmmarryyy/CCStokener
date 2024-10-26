package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import javax.sound.midi.InvalidMidiDataException;
import vavi.sound.mfi.spi.MfiDeviceProvider;
import vavi.sound.mfi.spi.MfiFileReader;
import vavi.sound.mfi.spi.MfiFileWriter;
import vavi.util.Debug;

/**
 * MfiSystem.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 021222 nsano use META-INF/services files <br>
 *          0.02 030817 nsano uncomment isFileTypeSupported <br>
 *          0.03 030819 nsano add toMidiSequence, toMfiSequence <br>
 *          0.04 031212 nsano add toMfiSequence(Sequence, int) <br>
 */
public final class MfiSystem {

    /** �A�N�Z�X�ł��܂���B */
    private MfiSystem() {
    }

    /** �f�t�H���g�v���o�C�_���炷�ׂẴf�o�C�X���擾���܂��B */
    public static MfiDevice.Info[] getMfiDeviceInfo() {
        return provider.getDeviceInfo();
    }

    /** �f�t�H���g�v���o�C�_����w�肵�����̃f�o�C�X���擾���܂��B */
    public static MfiDevice getMfiDevice(MfiDevice.Info info) throws MfiUnavailableException {
        return provider.getDevice(info);
    }

    /**
     * �f�t�H���g�v���o�C�_����V�[�P���T���擾���܂��B
     * <p>
     * {@link #getSequencer()} �ōĐ�����ꍇ��
     * �V�X�e���v���p�e�B <code>javax.sound.midi.Sequencer</code> �� <code>"#Real Time Sequencer"</code>
     * �𖾎�����悤�ɂ��Ă��������B<code>"Java MIDI(MFi/SMAF) ADPCM Sequencer"</code> ��
     * �f�t�H���g�V�[�P���T�ɂȂ����ꍇ�A{@link #getMetaEventListener()}�Ŏ擾�ł��郊�X�i�[
     * ���d�����ēo�^����Ă��܂��܂��B
     * </p>
     */
    public static Sequencer getSequencer() throws MfiUnavailableException {
        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof Sequencer) {
                return (Sequencer) device;
            }
        }
        throw new MfiUnavailableException("no sequencer available");
    }

    /** MIDI �V�[�P���T�ɕt�����郊�X�i���擾���܂��B */
    public static javax.sound.midi.MetaEventListener getMetaEventListener() throws MfiUnavailableException {
        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof javax.sound.midi.MetaEventListener) {
                return (javax.sound.midi.MetaEventListener) device;
            }
        }
        throw new MfiUnavailableException("no MetaEventListener available");
    }

    /** �f�t�H���g�v���o�C�_���� MIDI - MFi �R���o�[�^���擾���܂��B */
    public static MidiConverter getMidiConverter() throws MfiUnavailableException {
        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof MidiConverter) {
                return (MidiConverter) device;
            }
        }
        throw new MfiUnavailableException("no midiConverter available");
    }

    /** @deprecated use #toMfiSequence(javax.sound.midi.Sequence sequence, int) */
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence) throws InvalidMidiDataException, MfiUnavailableException {
        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMfiSequence(sequence);
    }

    /**
     * MIDI �V�[�P���X�� MFi �V�[�P���X�ɕϊ����܂��B
     * @param type    midi file type
     * @see MfiFileFormat#type
     */
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence, int type) throws InvalidMidiDataException, MfiUnavailableException {
        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMfiSequence(sequence, type);
    }

    /** MFi �V�[�P���X�� MIDI �V�[�P���X�ɕϊ����܂��B */
    public static javax.sound.midi.Sequence toMidiSequence(Sequence sequence) throws InvalidMfiDataException, MfiUnavailableException {
        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMidiSequence(sequence);
    }

    /** MFi �t�@�C���t�H�[�}�b�g���擾���܂��B */
    public static MfiFileFormat getMfiFileFormat(InputStream stream) throws InvalidMfiDataException, IOException {
        for (int i = 0; i < readers.length; i++) {
            try {
                MfiFileFormat mff = readers[i].getMfiFileFormat(stream);
                return mff;
            } catch (Exception e) {
                Debug.println(e);
            }
        }
        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** MFi �t�@�C���t�H�[�}�b�g���擾���܂��B */
    public static MfiFileFormat getMfiFileFormat(File file) throws InvalidMfiDataException, IOException {
        return getMfiFileFormat(new BufferedInputStream(new FileInputStream(file)));
    }

    /** MFi �t�@�C���t�H�[�}�b�g���擾���܂��B */
    public static MfiFileFormat getMfiFileFormat(URL url) throws InvalidMfiDataException, IOException {
        return getMfiFileFormat(new BufferedInputStream(url.openStream()));
    }

    /** MFi �V�[�P���X���擾���܂��B */
    public static Sequence getSequence(InputStream stream) throws InvalidMfiDataException, IOException {
        for (int i = 0; i < readers.length; i++) {
            try {
                Sequence sequence = readers[i].getSequence(stream);
                return sequence;
            } catch (InvalidMfiDataException e) {
                Debug.println(e);
                continue;
            }
        }
        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** MFi �V�[�P���X���擾���܂��B */
    public static Sequence getSequence(File file) throws InvalidMfiDataException, IOException {
        return getSequence(new BufferedInputStream(new FileInputStream(file)));
    }

    /** MFi �V�[�P���X���擾���܂��B */
    public static Sequence getSequence(URL url) throws InvalidMfiDataException, IOException {
        return getSequence(new BufferedInputStream(url.openStream()));
    }

    /** �T�|�[�g���� MFi �t�@�C���^�C�v���擾���܂��B */
    public static int[] getMfiFileTypes() {
        List<Integer> types = new ArrayList<Integer>();
        for (int i = 0; i < writers.length; i++) {
            int[] ts = writers[i].getMfiFileTypes();
            for (int j = 0; j < ts.length; j++) {
                types.add(ts[j]);
            }
        }
        int[] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }
        return result;
    }

    /** �w�肵���V�[�P���X�ɑΉ����� MFi �t�@�C���^�C�v���擾���܂��B */
    public static int[] getMfiFileTypes(Sequence sequence) {
        List<Integer> types = new ArrayList<Integer>();
        for (int i = 0; i < writers.length; i++) {
            int[] ts = writers[i].getMfiFileTypes(sequence);
            for (int j = 0; j < ts.length; j++) {
                types.add(ts[j]);
            }
        }
        int[] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }
        return result;
    }

    /** �t�@�C���^�C�v���T�|�[�g����邩�ǂ�����Ԃ��܂��B */
    public static boolean isFileTypeSupported(int fileType) {
        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType)) {
                return true;
            }
        }
        return false;
    }

    /** �t�@�C���^�C�v���w�肵���V�[�P���X�ŃT�|�[�g����邩�ǂ�����Ԃ��܂��B */
    public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType, sequence)) {
                return true;
            }
        }
        return false;
    }

    /** MFi or MIDI �ŏ����o���܂��B */
    public static int write(Sequence in, int fileType, OutputStream out) throws IOException {
        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType, in)) {
                return writers[i].write(in, fileType, out);
            }
        }
        Debug.println(Level.WARNING, "no writer found for: " + fileType);
        return 0;
    }

    /** MFi or MIDI �ŏ����o���܂��B */
    public static int write(Sequence in, int fileType, File out) throws IOException {
        return write(in, fileType, new BufferedOutputStream(new FileOutputStream(out)));
    }

    /** all �v���o�C�_ */
    private static MfiDeviceProvider[] providers;

    /** all ���[�_ */
    private static MfiFileReader[] readers;

    /** all ���C�^ */
    private static MfiFileWriter[] writers;

    /** default �v���o�C�_ */
    private static MfiDeviceProvider provider;

    /**
     * default �� MfiSystem.properties �Ŏw�肵�܂��B
     * <li>vavi.sound.mfi.spi.MfiDeviceProvider
     */
    static {
        final String dir = "/META-INF/services/";
        final String providerFile = "vavi.sound.mfi.spi.MfiDeviceProvider";
        final String readerFile = "vavi.sound.mfi.spi.MfiFileReader";
        final String writerFile = "vavi.sound.mfi.spi.MfiFileWriter";
        Properties props = new Properties();
        Properties mfiSystemProps = new Properties();
        try {
            Class<?> clazz = MfiSystem.class;
            mfiSystemProps.load(clazz.getResourceAsStream("MfiSystem.properties"));
            String defaultProvider = mfiSystemProps.getProperty("default.provider");
            props.load(clazz.getResourceAsStream(dir + providerFile));
            props.list(System.err);
            Enumeration<?> e = props.propertyNames();
            int i = 0;
            providers = new MfiDeviceProvider[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked") Class<MfiDeviceProvider> c = (Class<MfiDeviceProvider>) Class.forName((String) e.nextElement());
                providers[i] = c.newInstance();
                if (c.getName().equals(defaultProvider)) {
                    provider = providers[i];
                }
                i++;
            }
            Debug.println("default provider: " + provider.getClass().getName());
            props.clear();
            props.load(clazz.getResourceAsStream(dir + readerFile));
            props.list(System.err);
            e = props.propertyNames();
            i = 0;
            readers = new MfiFileReader[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked") Class<MfiFileReader> c = (Class<MfiFileReader>) Class.forName((String) e.nextElement());
                readers[i++] = c.newInstance();
            }
            props.clear();
            props.load(clazz.getResourceAsStream(dir + writerFile));
            props.list(System.err);
            e = props.propertyNames();
            i = 0;
            writers = new MfiFileWriter[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked") Class<MfiFileWriter> c = (Class<MfiFileWriter>) Class.forName((String) e.nextElement());
                writers[i++] = c.newInstance();
            }
        } catch (Exception e) {
            Debug.println(Level.SEVERE, e);
            Debug.printStackTrace(e);
            System.exit(1);
        }
    }

    /**
     * Tests this class.
     *
     * usage: java -Djavax.sound.midi.Sequencer="#Real Time Sequencer" MfiSystem mfi_file ...
     */
    public static void main(String[] args) throws Exception {
        final Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
        for (int i = 0; i < args.length; i++) {
            Debug.println("START: " + args[i]);
            Sequence sequence = MfiSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            if (i == args.length - 1) {
                sequencer.addMetaEventListener(new MetaEventListener() {

                    public void meta(MetaMessage meta) {
                        Debug.println(meta.getType());
                        if (meta.getType() == 47) {
                            sequencer.close();
                        }
                    }
                });
            }
            sequencer.start();
            Debug.println("END: " + args[i]);
        }
    }
}
