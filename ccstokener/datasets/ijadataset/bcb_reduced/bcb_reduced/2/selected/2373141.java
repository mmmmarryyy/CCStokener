package cn.edu.wuse.musicxml.demo;

import java.applet.Applet;
import java.net.URL;
import javax.sound.midi.MidiSystem;
import javax.swing.JOptionPane;
import cn.edu.wuse.musicxml.gui.Player;
import cn.edu.wuse.musicxml.gui.PlayerUI;
import cn.edu.wuse.musicxml.parser.MidiRenderer;
import cn.edu.wuse.musicxml.parser.PartwiseParser;

public class PlayerApplet extends Applet {

    private static final long serialVersionUID = 1L;

    private PlayerUI playerUI;

    private Player player;

    public void init() {
        super.init();
        setSize(500, 100);
        player = new Player();
        playerUI = new PlayerUI(player);
        player.addPlayProcessor(playerUI);
        player.stop();
        add(playerUI);
    }

    public void destroy() {
        super.destroy();
    }

    public void auto(String url) {
        if (player.getSingal()) player.stop();
        try {
            URL url2 = new URL(url);
            if (url.toLowerCase().endsWith(".mid") || url.toLowerCase().endsWith(".mid")) player.setSequence(MidiSystem.getSequence(url2.openStream())); else {
                PartwiseParser parser = new PartwiseParser();
                MidiRenderer renderer = new MidiRenderer();
                parser.addMusicParserListener(renderer);
                parser.parse(url2.openStream());
                player.setSequence(renderer.getSequence());
            }
            player.play();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ERROR", "警告", JOptionPane.WARNING_MESSAGE);
        }
    }
}
