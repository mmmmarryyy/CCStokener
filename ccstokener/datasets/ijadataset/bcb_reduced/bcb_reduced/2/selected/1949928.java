package modrcon;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;

/**
 * A JLabel with a single image for the Tooltip.
 *
 * @author Pyrite
 */
public class JImageLabel extends JLabel {

    private String map;

    /**
     * Constructs a JImageLabel.
     *
     * @param text The text for the JLabel.
     * @param resourcePath The path to the image for the tooltip.
     * @param stamina How long the tooltip should last.
     */
    public JImageLabel(String text, int stamina) {
        super(text);
        this.map = "foobar";
        this.setToolTipText("");
        ToolTipManager tm = ToolTipManager.sharedInstance();
        tm.setDismissDelay(stamina);
    }

    public void setMapName(String map) {
        this.map = map;
    }

    @Override
    public JToolTip createToolTip() {
        String strURL = "http://1upclan.info/serverstatus/servers2/maps/q3a/" + this.map + ".jpg";
        URL url = null;
        boolean flag = false;
        try {
            url = new URL(strURL);
            String type = url.openConnection().getContentType();
            if (type.equals("image/jpeg")) {
                flag = true;
            }
        } catch (MalformedURLException mue) {
            flag = false;
        } catch (IOException ioe) {
            flag = false;
        }
        if (flag) {
            JToolTipWithIcon tip = new JToolTipWithIcon(new ImageIcon(url));
            tip.setComponent(this);
            return tip;
        } else {
            JToolTipWithIcon tip = new JToolTipWithIcon(new ImageIcon(getClass().getResource("/modrcon/resources/maps/default_ut.jpg")));
            tip.setComponent(this);
            return tip;
        }
    }
}
