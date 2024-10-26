package net.mp3spider.player;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import javax.swing.JFrame;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

public abstract class Mp3SpiderPlayer extends JFrame implements BasicPlayerListener {

    private BasicController player = null;

    private BasicPlayer tmp = null;

    private String file;

    private InputStream input;

    private int songLength = 0;

    private boolean seeked = false;

    private boolean posValueJump = false;

    private URL urlLink = null;

    private URLConnection con = null;

    public void loadAndPlay(String filename) throws MalformedURLException, IOException {
        load(filename);
        this.play();
    }

    public void load(String filename) throws MalformedURLException {
        this.file = filename;
        this.urlLink = new URL(this.file);
    }

    public Mp3SpiderPlayer() {
        super();
        init();
    }

    public void init() {
        tmp = new BasicPlayer();
        tmp.addBasicPlayerListener(this);
        setPlayer((BasicController) tmp);
    }

    public void play() throws MalformedURLException, IOException {
        try {
            if (tmp.getStatus() == BasicPlayer.STOPPED || tmp.getStatus() == BasicPlayer.UNKNOWN) {
                con = urlLink.openConnection();
                String contentLength = con.getHeaderField("Content-Length");
                if (contentLength != null) setSongLength(Integer.parseInt(contentLength));
                this.setInput(con.getInputStream());
                getPlayer().open(this.getInput());
                getPlayer().play();
            } else if (tmp.getStatus() == BasicPlayer.PLAYING) {
                getPlayer().stop();
                this.getInput().close();
                this.play();
            } else if (tmp.getStatus() == BasicPlayer.PAUSED) {
                getPlayer().resume();
            }
        } catch (BasicPlayerException ex) {
            System.out.println(ex);
        }
    }

    public void stop() {
        if (tmp.getStatus() == BasicPlayer.PAUSED || tmp.getStatus() == BasicPlayer.PLAYING) {
            try {
                getPlayer().stop();
            } catch (BasicPlayerException ex) {
                System.out.println(ex);
            }
        }
    }

    public void pause() {
        try {
            if (tmp.getStatus() == BasicPlayer.PLAYING) {
                getPlayer().pause();
            } else if (tmp.getStatus() == BasicPlayer.PAUSED) {
                getPlayer().resume();
            }
        } catch (BasicPlayerException ex) {
            System.out.println(ex);
        }
    }

    public void seek(int pos) throws BasicPlayerException {
        long val = Math.round((songLength * pos) / 100);
        player.seek(val);
    }

    /**
     * @return the songLength
     */
    public int getSongLength() {
        return songLength;
    }

    /**
     * @param songLength the songLength to set
     */
    public void setSongLength(int songLength) {
        this.songLength = songLength;
    }

    /**
     * @return the seeked
     */
    public boolean isSeeked() {
        return seeked;
    }

    /**
     * @param seeked the seeked to set
     */
    public void setSeeked(boolean seeked) {
        this.seeked = seeked;
    }

    /**
     * @return the player
     */
    public BasicController getPlayer() {
        return player;
    }

    /**
     * @param player the player to set
     */
    public void setPlayer(BasicController player) {
        this.player = player;
    }

    /**
     * @return the posValueJump
     */
    public boolean isPosValueJump() {
        return posValueJump;
    }

    /**
     * @param posValueJump the posValueJump to set
     */
    public void setPosValueJump(boolean posValueJump) {
        this.posValueJump = posValueJump;
    }

    /**
     * @return the input
     */
    public InputStream getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(InputStream input) {
        this.input = input;
    }

    public void opened(Object arg0, Map arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void progress(int arg0, long arg1, byte[] arg2, Map arg3) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stateUpdated(BasicPlayerEvent arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setController(BasicController arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
