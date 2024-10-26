package org.doit.muffin;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.InputStream;
import org.doit.util.*;

public class HelpFrame extends MuffinFrame implements ActionListener, WindowListener {

    /**
	 * Serializable class should define this:
	 */
    private static final long serialVersionUID = 1L;

    public HelpFrame(String helpFile) {
        super(Strings.getString("help.title", helpFile));
        TextArea text = new TextArea();
        text.setEditable(false);
        String resourcePath = "/doc/" + helpFile + ".txt";
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            try {
                byte buf[] = new byte[8192];
                int n;
                InputStream in = url.openStream();
                while ((n = in.read(buf, 0, buf.length)) > 0) {
                    text.append(new String(buf, 0, n));
                }
                in.close();
            } catch (Exception e) {
            }
        } else {
            text.append(Strings.getString("not found: " + resourcePath));
        }
        add("Center", text);
        Button b;
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new GridLayout(1, 1));
        b = new Button(Strings.getString("close"));
        b.setActionCommand("doClose");
        b.addActionListener(this);
        buttonPanel.add(b);
        add("South", buttonPanel);
        addWindowListener(this);
        setSize(getPreferredSize());
        pack();
        show();
    }

    public void actionPerformed(ActionEvent event) {
        String arg = event.getActionCommand();
        if ("doClose".equals(arg)) {
            setVisible(false);
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        setVisible(false);
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }
}
