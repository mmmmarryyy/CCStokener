package elf;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
import elf.xml.sounds.SoundsBaseType;
import elf.xml.sounds.SoundsMusicType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

/**
 * The <code>ExamplePlayer</code> thread class will simply download and play OGG
 * media. All you need to do is supply a valid URL as the first argument.
 * 
 * @author Jon Kristensen
 * @version 1.0
 */
public class MusicFactory {

    private static MusicFactory instance = null;

    private boolean playRepeat = false;

    private OggPlayer player = null;

    private String oggfilepath = null;

    private boolean playing = false;

    private boolean isLoaded = false;

    private HashMap<String, String> musicsMap;

    private boolean musicSet = true;

    /**
	 * This method return a single instance of the Music class
	 * @return Single instance of the Music class
	 */
    public static MusicFactory getInstance() {
        if (instance == null) {
            instance = new MusicFactory();
        }
        return instance;
    }

    /**
	 * Sole contructor of the Music class
	 */
    private MusicFactory() {
        musicsMap = new HashMap<String, String>();
    }

    /**
	 * Enable the music
	 * @param set
	 */
    public void setEnabled(boolean set) {
        musicSet = set;
    }

    /**
	 * Check if the music is enabled
	 */
    public boolean isEnabled() {
        return musicSet;
    }

    /**
	 * Start playing the song loaded with <code>loadMusic</code>.
	 * @param play the song repeatedly if set to <code>true</code>
	 */
    public void play(boolean repeat) {
        if (musicSet) {
            playRepeat = repeat;
            play();
        }
    }

    /**
	 * Start playing the song loaded with <code>loadMusic</code>.
	 * The song is played once only
	 */
    public void play() {
        if (musicSet) {
            if (isLoaded && player == null) open();
            if (player != null) {
                player.play();
                playing = true;
            }
        }
    }

    /**
	 * Stops playing the song
	 */
    public void stop() {
        if (musicSet) {
            if (player != null) player.stop();
            player = null;
            playing = false;
        }
    }

    /**
	 * Pause the song. To continue playing the song use the <code>play()</code> or <code>play(boolean repeat)</code> to 
	 */
    public void pause() {
        if (musicSet) {
            if (player != null) player.pause();
            playing = false;
        }
    }

    /**
	 * Load a song to play by its name
	 * @param musicname the name of the song to play
	 */
    public void loadMusic(String musicname) {
        oggfilepath = musicsMap.get(musicname);
        System.out.println("Loading music " + musicname + "@" + oggfilepath + " for playing");
        playRepeat = false;
        open();
        isLoaded = true;
    }

    /**
	 * Prepare the ogg file player to play the song
	 */
    private void open() {
        player = new OggPlayer(oggfilepath, this);
    }

    /**
	 * Action to execute when a song is finished.
	 * Replay the song if we set it to repeat.
	 */
    private void finished() {
        if (player != null) {
            player.stop();
            if (playing && playRepeat) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                open();
                play();
            } else {
                playing = false;
            }
        }
    }

    /**
	 * Set the to repeat or not
	 * @param repeat set to <code>true</code> for playing the song repeatedly or to <code>false</code> to play a single time
	 */
    public void setRepeat(boolean repeat) {
        playRepeat = repeat;
    }

    /**
	 * Iniatialize the Music class by referencing the song files from the xml description file 
	 * @param file the xml description file to load from
	 */
    public void init(String file) {
        URL url = SoundFactory.class.getResource(file);
        try {
            JAXBContext context = JAXBContext.newInstance("elf.xml.sounds");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SoundsBaseType root = null;
            Object tmpobj = unmarshaller.unmarshal(url.openConnection().getInputStream());
            if (tmpobj instanceof JAXBElement<?>) {
                if (((JAXBElement<?>) tmpobj).getValue() instanceof SoundsBaseType) {
                    root = (SoundsBaseType) ((JAXBElement<?>) tmpobj).getValue();
                    addMusic("MENUSONG", root.getMenumusic().getMusicpath());
                    List<SoundsMusicType> musiclist = root.getMusic();
                    Iterator<SoundsMusicType> it = musiclist.iterator();
                    while (it.hasNext()) {
                        SoundsMusicType smt = it.next();
                        addMusic(smt.getMusicname(), smt.getMusicpath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Add a song to the list of referenced songs
	 * @param musicName the name of the song
	 * @param musicPath the path to the song file
	 */
    private void addMusic(String musicName, String musicPath) {
        musicsMap.put(musicName, musicPath);
    }

    private class OggPlayer implements Runnable {

        private URLConnection urlConnection = null;

        private InputStream inputStream = null;

        byte[] buffer = null;

        int bufferSize = 16384;

        int count = 0;

        int index = 0;

        byte[] convertedBuffer;

        int convertedBufferSize;

        private SourceDataLine outputLine = null;

        private float[][][] pcmInfo;

        private int[] pcmIndex;

        private Packet joggPacket = new Packet();

        private Page joggPage = new Page();

        private StreamState joggStreamState = new StreamState();

        private SyncState joggSyncState = new SyncState();

        private DspState jorbisDspState = new DspState();

        private Block jorbisBlock = new Block(jorbisDspState);

        private Comment jorbisComment = new Comment();

        private Info jorbisInfo = new Info();

        private URL pUrl = null;

        private boolean isPlaying;

        private Thread thread;

        private MusicFactory caller;

        /**
		 * Sets the <code>inputStream</code> object by taking an URL, opens a
		 * connection to it and get the <code>InputStream</code>.
		 * 
		 * @param pUrl
		 *            the url to the media file
		 */
        public OggPlayer(String filepath, MusicFactory caller) {
            this.caller = caller;
            pUrl = MusicFactory.class.getResource(filepath);
            try {
                urlConnection = pUrl.openConnection();
            } catch (UnknownServiceException exception) {
                System.err.println("The protocol does not support input.");
            } catch (IOException exception) {
                System.err.println("An I/O error occoured while trying create the " + "URL connection.");
            }
            if (urlConnection != null) {
                try {
                    inputStream = urlConnection.getInputStream();
                } catch (IOException exception) {
                    System.err.println("An I/O error occoured while trying to get an " + "input stream from the URL.");
                    System.err.println(exception);
                }
            }
        }

        /**
		 * This method is probably easiest understood by looking at the body.
		 * However, it will - if no problems occur - call methods to initialize
		 * the JOgg JOrbis libraries, read the header, initialize the sound
		 * system, read the body of the stream and clean up.
		 */
        public void run() {
            if (inputStream == null) {
                System.err.println("We don't have an input stream and therefor cannot continue.");
                return;
            }
            initializeJOrbis();
            if (readHeader()) {
                if (initializeSound()) {
                    readBody();
                }
            }
            cleanUp();
        }

        /**
		 * Initializes JOrbis. First, we initialize the <code>SyncState</code>
		 * object. After that, we prepare the <code>SyncState</code> buffer.
		 * Then we "initialize" our buffer, taking the data in
		 * <code>SyncState</code>.
		 */
        private void initializeJOrbis() {
            joggSyncState.init();
            joggSyncState.buffer(bufferSize);
            buffer = joggSyncState.data;
        }

        /**
		 * This method reads the header of the stream, which consists of three
		 * packets.
		 * 
		 * @return true if the header was successfully read, false otherwise
		 */
        private boolean readHeader() {
            boolean needMoreData = true;
            int packet = 1;
            while (needMoreData) {
                try {
                    count = inputStream.read(buffer, index, bufferSize);
                } catch (IOException exception) {
                    System.err.println("Could not read from the input stream.");
                    System.err.println(exception);
                }
                joggSyncState.wrote(count);
                switch(packet) {
                    case 1:
                        {
                            switch(joggSyncState.pageout(joggPage)) {
                                case -1:
                                    {
                                        System.err.println("There is a hole in the first " + "packet data.");
                                        return false;
                                    }
                                case 0:
                                    {
                                        break;
                                    }
                                case 1:
                                    {
                                        joggStreamState.init(joggPage.serialno());
                                        joggStreamState.reset();
                                        jorbisInfo.init();
                                        jorbisComment.init();
                                        if (joggStreamState.pagein(joggPage) == -1) {
                                            System.err.println("We got an error while " + "reading the first header page.");
                                            return false;
                                        }
                                        if (joggStreamState.packetout(joggPacket) != 1) {
                                            System.err.println("We got an error while " + "reading the first header packet.");
                                            return false;
                                        }
                                        if (jorbisInfo.synthesis_headerin(jorbisComment, joggPacket) < 0) {
                                            System.err.println("We got an error while " + "interpreting the first packet. " + "Apparantly, it's not Vorbis data.");
                                            return false;
                                        }
                                        packet++;
                                        break;
                                    }
                            }
                            if (packet == 1) break;
                        }
                    case 2:
                    case 3:
                        {
                            switch(joggSyncState.pageout(joggPage)) {
                                case -1:
                                    {
                                        System.err.println("There is a hole in the second " + "or third packet data.");
                                        return false;
                                    }
                                case 0:
                                    {
                                        break;
                                    }
                                case 1:
                                    {
                                        joggStreamState.pagein(joggPage);
                                        switch(joggStreamState.packetout(joggPacket)) {
                                            case -1:
                                                {
                                                    System.err.println("There is a hole in the first" + "packet data.");
                                                    return false;
                                                }
                                            case 0:
                                                {
                                                    break;
                                                }
                                            case 1:
                                                {
                                                    jorbisInfo.synthesis_headerin(jorbisComment, joggPacket);
                                                    packet++;
                                                    if (packet == 4) {
                                                        needMoreData = false;
                                                    }
                                                    break;
                                                }
                                        }
                                        break;
                                    }
                            }
                            break;
                        }
                }
                index = joggSyncState.buffer(bufferSize);
                buffer = joggSyncState.data;
                if (count == 0 && needMoreData) {
                    System.err.println("Not enough header data was supplied.");
                    return false;
                }
            }
            return true;
        }

        /**
		 * This method starts the sound system. It starts with initializing the
		 * <code>DspState</code> object, after which it sets up the
		 * <code>Block</code> object. Last but not least, it opens a line to the
		 * source data line.
		 * 
		 * @return true if the sound system was successfully started, false
		 *         otherwise
		 */
        private boolean initializeSound() {
            convertedBufferSize = bufferSize * 2;
            convertedBuffer = new byte[convertedBufferSize];
            jorbisDspState.synthesis_init(jorbisInfo);
            jorbisBlock.init(jorbisDspState);
            int channels = jorbisInfo.channels;
            int rate = jorbisInfo.rate;
            AudioFormat audioFormat = new AudioFormat((float) rate, 16, channels, true, false);
            DataLine.Info datalineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            if (!AudioSystem.isLineSupported(datalineInfo)) {
                System.err.println("Audio output line is not supported.");
                return false;
            }
            try {
                outputLine = (SourceDataLine) AudioSystem.getLine(datalineInfo);
                outputLine.open(audioFormat);
            } catch (LineUnavailableException exception) {
                System.out.println("The audio output line could not be opened due " + "to resource restrictions.");
                System.err.println(exception);
                return false;
            } catch (IllegalStateException exception) {
                System.out.println("The audio output line is already open.");
                System.err.println(exception);
                return false;
            } catch (SecurityException exception) {
                System.out.println("The audio output line could not be opened due " + "to security restrictions.");
                System.err.println(exception);
                return false;
            }
            outputLine.start();
            pcmInfo = new float[1][][];
            pcmIndex = new int[jorbisInfo.channels];
            return true;
        }

        /**
		 * This method reads the entire stream body. Whenever it extracts a
		 * packet, it will decode it by calling
		 * <code>decodeCurrentPacket()</code>.
		 */
        private void readBody() {
            boolean needMoreData = true;
            while (needMoreData) {
                switch(joggSyncState.pageout(joggPage)) {
                    case 0:
                        {
                            break;
                        }
                    case 1:
                        {
                            joggStreamState.pagein(joggPage);
                            if (joggPage.granulepos() == 0) {
                                needMoreData = false;
                                break;
                            }
                            processPackets: while (true) {
                                switch(joggStreamState.packetout(joggPacket)) {
                                    case 0:
                                        {
                                            break processPackets;
                                        }
                                    case 1:
                                        {
                                            while (!isPlaying) {
                                                try {
                                                    Thread.sleep(100);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            decodeCurrentPacket();
                                        }
                                }
                            }
                            if (joggPage.eos() != 0) {
                                needMoreData = false;
                            }
                        }
                }
                if (needMoreData) {
                    index = joggSyncState.buffer(bufferSize);
                    buffer = joggSyncState.data;
                    try {
                        if (inputStream.available() < 2048) {
                            bufferSize = inputStream.available();
                        }
                        count = inputStream.read(buffer, index, bufferSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    joggSyncState.wrote(count);
                    if (count == 0) {
                        needMoreData = false;
                    }
                }
            }
            caller.finished();
        }

        /**
		 * A clean-up method, called when everything is finished. Clears the
		 * JOgg/JOrbis objects and closes the <code>InputStream</code>.
		 */
        private void cleanUp() {
            joggStreamState.clear();
            jorbisBlock.clear();
            jorbisDspState.clear();
            jorbisInfo.clear();
            joggSyncState.clear();
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
            }
        }

        /**
		 * Decodes the current packet and sends it to the audio output line.
		 */
        private void decodeCurrentPacket() {
            int samples;
            if (jorbisBlock.synthesis(joggPacket) == 0) {
                jorbisDspState.synthesis_blockin(jorbisBlock);
            }
            int range;
            while ((samples = jorbisDspState.synthesis_pcmout(pcmInfo, pcmIndex)) > 0) {
                if (samples < convertedBufferSize) {
                    range = samples;
                } else {
                    range = convertedBufferSize;
                }
                for (int i = 0; i < jorbisInfo.channels; i++) {
                    int sampleIndex = i * 2;
                    for (int j = 0; j < range; j++) {
                        int value = (int) (pcmInfo[0][i][pcmIndex[i] + j] * 32767);
                        if (value > 32767) {
                            value = 32767;
                        }
                        if (value < -32768) {
                            value = -32768;
                        }
                        if (value < 0) value = value | 32768;
                        convertedBuffer[sampleIndex] = (byte) (value);
                        convertedBuffer[sampleIndex + 1] = (byte) (value >>> 8);
                        sampleIndex += 2 * (jorbisInfo.channels);
                    }
                }
                outputLine.write(convertedBuffer, 0, 2 * jorbisInfo.channels * range);
                jorbisDspState.synthesis_read(range);
            }
        }

        public void pause() {
            isPlaying = false;
            outputLine.stop();
        }

        public void play() {
            if (thread == null) {
                thread = Executors.defaultThreadFactory().newThread(this);
                thread.start();
            }
            if (outputLine != null && !outputLine.isActive()) {
                outputLine.start();
            }
            isPlaying = true;
        }

        public void stop() {
            pause();
            thread = null;
        }
    }
}
