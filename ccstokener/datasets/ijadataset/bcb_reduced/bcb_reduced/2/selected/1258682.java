package org.xith3d.scenegraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

/**
 * Plays a midi sound file
 * 
 * @author Amos Wenger (aka BlueSky)
 */
public class MidiSound extends Sound {

    private Sequencer sequencer;

    public MidiSound(File file) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        load(new FileInputStream(file));
    }

    public MidiSound(URL url) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        load(url.openStream());
    }

    public MidiSound(InputStream inputStream) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        load(inputStream);
    }

    private void load(InputStream inputStream) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        sequencer = MidiSystem.getSequencer();
        sequencer.setSequence(MidiSystem.getSequence(inputStream));
        sequencer.open();
        sequencer.start();
    }

    public void finalize() {
        sequencer.stop();
        sequencer.close();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled && !sequencer.isRunning()) {
            sequencer.start();
        } else if (sequencer.isRunning()) {
            sequencer.stop();
        }
    }

    @Override
    public void setPaused(boolean paused) {
        super.setPaused(paused);
        setEnabled(!paused);
    }

    @Override
    public long getDuration() {
        return sequencer.getMicrosecondLength();
    }
}
