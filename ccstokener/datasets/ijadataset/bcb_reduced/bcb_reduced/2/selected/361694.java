package com.groovytagger.thread;

import com.groovytagger.utils.LogManager;
import com.groovytagger.utils.StaticObj;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.Player;

public class Mp3PlayerThread extends Thread {

    private String filePath = null;

    private boolean remote = false;

    private Player player = null;

    private boolean pauseMusic = false;

    private int savePoint = 0;

    public Mp3PlayerThread(String filePath) {
        this.filePath = filePath;
    }

    public void run() {
        try {
            if (savePoint > 0) {
                resumeMusic();
            } else if (filePath != null) {
                playMusic();
            } else {
                System.out.println("Nothing to play!");
            }
        } catch (Exception e) {
            LogManager.getInstance().getLogger().error(e);
            if (StaticObj.DEBUG) e.printStackTrace();
        }
    }

    public void pauseMusic() throws Exception {
        this.pauseMusic = true;
        savePoint = player.getPosition();
    }

    public void resumeMusic() throws Exception {
        player.play(savePoint);
    }

    public void playMusic() throws Exception {
        try {
            InputStream in = null;
            if (remote == true) {
                in = getURLInputStream();
            } else {
                in = getInputStream();
            }
            AudioDevice dev = getAudioDevice();
            player = new Player(in, dev);
            player.play();
        } catch (Exception ex) {
            LogManager.getInstance().getLogger().error(ex);
            if (StaticObj.DEBUG) {
                ex.printStackTrace();
                throw new JavaLayerException("Problem playing file " + filePath, ex);
            }
        }
    }

    public void stopMusic() throws Exception {
        try {
            if (player != null) {
                player.close();
                savePoint = 0;
            }
        } catch (Exception ex) {
            LogManager.getInstance().getLogger().error(ex);
            if (StaticObj.DEBUG) {
                ex.printStackTrace();
                throw new JavaLayerException("Problem closing file " + filePath, ex);
            }
        }
    }

    protected InputStream getURLInputStream() throws Exception {
        URL url = new URL(filePath);
        InputStream fin = url.openStream();
        BufferedInputStream bin = new BufferedInputStream(fin);
        return bin;
    }

    protected InputStream getInputStream() throws IOException {
        FileInputStream fin = new FileInputStream(filePath);
        BufferedInputStream bin = new BufferedInputStream(fin);
        return bin;
    }

    protected AudioDevice getAudioDevice() throws JavaLayerException {
        return FactoryRegistry.systemRegistry().createAudioDevice();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
