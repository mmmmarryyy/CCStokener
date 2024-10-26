package org.jbuzz.applet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import org.jbuzz.Client;
import org.jbuzz.awt.Component;
import org.jbuzz.awt.Container;
import org.jbuzz.awt.Graphics;
import org.jbuzz.awt.Image;
import org.jbuzz.awt.event.FocusEvent;
import org.jbuzz.awt.event.KeyEvent;
import org.jbuzz.telnet.terminal.Terminal;

public abstract class Applet extends Container {

    private FocusEvent focusEvent;

    HashMap<String, String> parameters;

    AppletContext appletContext;

    Component focusOwner;

    protected Applet() {
        this.focusEvent = new FocusEvent(null, 0, null);
    }

    public void setClient(Client client) {
        this.client = client;
    }

    /**
	 * Set graphics for the applet.
	 * 
	 * @param graphics
	 *            graphics for the applet
	 */
    public void setGraphics(Graphics graphics) {
        Terminal terminal = graphics.getTerminal();
        this.setSize(terminal.getWidth(), terminal.getHeight());
        this.graphics = graphics;
    }

    /**
	 * Get parameter with the specified name.
	 * 
	 * @param name
	 *            name of the parameter
	 * @return parameter with the specified name
	 */
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    /**
	 * Get context of the applet.
	 * 
	 * @return context of the applet
	 */
    public AppletContext getAppletContext() {
        return this.appletContext;
    }

    public Image getImage(URL url) {
        try {
            return new Image(url.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public Image getImage(URL url, String name) {
        try {
            url = new URL(url, name);
        } catch (MalformedURLException e) {
            return null;
        }
        return this.getImage(url);
    }

    /**
	 * A callback method when the applet is to be initializd.
	 */
    public void init() {
    }

    /**
	 * A callback method when the applet is to be started.
	 */
    public void start() {
    }

    /**
	 * A callback method when the applet is to be stopped.
	 */
    public void stop() {
    }

    /**
	 * A callback method when the applet is to be destroyed.
	 */
    public void destroy() {
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.repaint();
    }

    public void paint(Graphics g) {
        super.paint(g);
        Component focusOwner = this.getFocusOwner();
        if (focusOwner != null) {
            focusOwner.requestFocus();
        }
    }

    public Component getFocusOwner() {
        if (this.focusOwner == null) {
            if (this.first != null) {
                this.first.requestFocus();
            }
        }
        return this.focusOwner;
    }

    public void setFocusOwner(Component focusOwner) {
        if (this.focusOwner != null) {
            this.focusEvent.setSource(this.focusOwner);
            this.focusEvent.setID(FocusEvent.FOCUS_LOST);
            this.focusEvent.setOppositeComponent(focusOwner);
            this.focusOwner.fireFocusEvent(this.focusEvent);
            this.focusEvent.setSource(focusOwner);
            this.focusEvent.setID(FocusEvent.FOCUS_GAINED);
            this.focusEvent.setOppositeComponent(this.focusOwner);
            focusOwner.fireFocusEvent(this.focusEvent);
        }
        this.focusOwner = focusOwner;
    }

    public void processKeyEvent(KeyEvent e) {
        if (this.focusOwner != null) {
            this.focusOwner.processKeyEvent(e);
        }
        this.fireKeyEvent(e);
    }
}
