package com.frinika.synth.soundbank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;
import com.frinika.synth.Synth;
import com.frinika.synth.settings.SynthSettings;

/**
 * 
 * @author Peter Johan Salomonsen
 *
 */
public class SynthRackSoundbankReader extends SoundbankReader {

    @Override
    public Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        return getSoundbank(url.openStream());
    }

    @Override
    public Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(stream);
        } catch (StreamCorruptedException e) {
            return null;
        }
        try {
            SynthSettings setup = (SynthSettings) in.readObject();
            SynthRackSoundbank soundbank = new SynthRackSoundbank();
            Serializable[] settings = setup.getSynthSettings();
            for (int index = 0; index < settings.length; index++) {
                if (settings[index] != null) {
                    String synthName = setup.getSynthClassNames()[index];
                    if (synthName == null) break;
                    if (synthName.equals("com.petersalomonsen.mystudio.mysynth.synths.SoundFont") || synthName.equals("com.petersalomonsen.mystudio.mysynth.synths.MySampler")) synthName = com.frinika.synth.synths.MySampler.class.getName();
                    Synth synth;
                    try {
                        synth = (Synth) Class.forName(synthName).getConstructors()[0].newInstance(new Object[] { this });
                        synth.loadSettings(setup.getSynthSettings()[index]);
                        Patch patch = new Patch(0, index);
                        soundbank.createAndRegisterInstrument(patch, synth);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return soundbank;
        } catch (ClassNotFoundException e) {
            throw new InvalidMidiDataException(e.getMessage());
        }
    }

    @Override
    public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        FileInputStream fis = new FileInputStream(file);
        Soundbank sbk = null;
        try {
            sbk = getSoundbank(fis);
        } finally {
            fis.close();
        }
        return sbk;
    }
}
