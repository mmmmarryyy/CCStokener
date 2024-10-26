package elf;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import elf.xml.sounds.SoundsBaseType;

public class SoundFactory {

    private static SoundFactory instance = null;

    private boolean sfxSet = false;

    private HashMap<String, Clip> soundsMap;

    public static SoundFactory getInstance() {
        if (instance == null) {
            instance = new SoundFactory();
        }
        return instance;
    }

    private SoundFactory() {
        soundsMap = new HashMap<String, Clip>();
    }

    public void init(String soundsdesc) {
        URL url = SoundFactory.class.getResource(soundsdesc);
        try {
            JAXBContext context = JAXBContext.newInstance("elf.xml.sounds");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SoundsBaseType root = null;
            Object tmpobj = unmarshaller.unmarshal(url.openConnection().getInputStream());
            if (tmpobj instanceof JAXBElement<?>) {
                if (((JAXBElement<?>) tmpobj).getValue() instanceof SoundsBaseType) {
                    root = (SoundsBaseType) ((JAXBElement<?>) tmpobj).getValue();
                    addSound("E_DROPGIFT", root.getEnemyDropgift().getSoundpath());
                    addSound("E_EXPLODE", root.getEnemyExplode().getSoundpath());
                    addSound("E_HURT", root.getEnemyHurt().getSoundpath());
                    addSound("E_SHOT", root.getEnemyShot().getSoundpath());
                    addSound("P_DIE", root.getPlayerDie().getSoundpath());
                    addSound("P_FALL", root.getPlayerFall().getSoundpath());
                    addSound("P_HURT", root.getPlayerHurt().getSoundpath());
                    addSound("P_SHOT", root.getPlayerShot().getSoundpath());
                    addSound("P_TAKEITEM", root.getPlayerTakeitem().getSoundpath());
                    addSound("A_DIE", root.getAnimalDie().getSoundpath());
                    addSound("S_CHANGEITEM", root.getShopChangeitem().getSoundpath());
                    addSound("S_CHANGEPAGE", root.getShopChangepage().getSoundpath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean addSound(String soundName, String soundPath) {
        AudioInputStream ais;
        try {
            ais = AudioSystem.getAudioInputStream(this.getClass().getResource(soundPath));
            Clip sndClip = AudioSystem.getClip();
            sndClip.open(ais);
            soundsMap.put(soundName, sndClip);
            return true;
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        System.err.println("Sound : can't load sound " + soundName + "@" + soundPath);
        return false;
    }

    public void clearSounds() {
        soundsMap.clear();
    }

    public void playSound(String soundName) {
        if (sfxSet) {
            Clip clip = soundsMap.get(soundName);
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void repeatSoundStart(String soundName) {
        if (sfxSet) {
            Clip clip = soundsMap.get(soundName);
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void repeatSoundStop(String soundName) {
        if (sfxSet) {
            Clip clip = soundsMap.get(soundName);
            clip.stop();
        }
    }

    public void setEnabled(boolean set) {
        this.sfxSet = set;
    }

    public boolean isEnabled() {
        return sfxSet;
    }
}
