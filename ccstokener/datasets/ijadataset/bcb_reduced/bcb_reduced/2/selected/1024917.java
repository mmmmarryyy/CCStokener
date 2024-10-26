package cn.edu.wuse.musicxml.demo;

import java.applet.Applet;
import java.net.URL;
import javax.swing.JOptionPane;
import cn.edu.wuse.musicxml.gui.BrowPage;
import cn.edu.wuse.musicxml.parser.PartwiseParser;
import cn.edu.wuse.musicxml.parser.ScoreRenderer;

public class ScoreApplet extends Applet {

    private static final long serialVersionUID = 1L;

    private BrowPage page;

    public void destroy() {
        super.destroy();
    }

    public void init() {
        super.init();
        setSize(500, 300);
    }

    public void autoPlay(String url) {
        try {
            URL url2 = new URL(url);
            PartwiseParser parser = new PartwiseParser();
            ScoreRenderer renderer = new ScoreRenderer();
            parser.addPrintListener(renderer);
            parser.parse(url2.openStream());
            page = new BrowPage(renderer.getScore());
            setLayout(null);
            page.setBounds(0, 0, 800, 600);
            add(page);
            repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ERROR", "警告", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
    }
}
