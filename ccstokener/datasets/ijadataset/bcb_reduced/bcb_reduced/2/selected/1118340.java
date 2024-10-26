package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.formaria.aria.ProjectManager;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A Tooltip class for AWT components. Unlike other Aria-AWT components the
 * tooltips are not added via the component factory. In this initial
 * implementation tooltips must be added explicitly.
 * <p>
 * Copyright (c) Formaria Ltd., 2008
 * </p>
 * <p>
 * $Revision: 1.1 $
 * </p>
 * <p>
 * License: see License.txt
 * </p>
 */
public class ToolTip extends Label implements MouseTrackListener, MouseListener {

    protected String tip;

    protected Control owner;

    protected Composite mainComposite;

    protected boolean shown;

    protected final int VERTICAL_OFFSET = 1;

    protected final int HORIZONTAL_ENLARGE = 10;

    protected Font font;

    protected boolean showTip = false;

    protected int lag = 500;

    protected TooltipThread tooltipThread;

    /**
   * Create anew tooltip
   * 
   * @param tip
   *          the tip text
   * @param owner
   *          the owner component
   */
    public ToolTip(String tip, Control owner) {
        super(null, 0);
        this.tip = tip;
        this.owner = owner;
        owner.addMouseListener(this);
        owner.addMouseTrackListener(this);
        setFont(font);
    }

    /**
   * Get the tip's text
   * 
   * @return the tooltip text
   */
    public String getTip() {
        return tip;
    }

    /**
   * Get the tip's text
   * 
   * @param newTip
   *          the new text
   */
    public void setTip(String newTip) {
        tip = newTip;
        setText(newTip);
    }

    /**
   * Add a tooltip
   * 
   * @param pt
   *          point
   */
    protected void addToolTip(Point pt) {
        shown = true;
    }

    /**
   * Get the size
   */
    protected void calcsize() {
    }

    /**
   * Set a tooltip location
   * 
   * @param pt
   *          point
   */
    protected void setToolTipLocation(Point pt) {
    }

    /**
   * Remove a tooltip
   */
    protected void removeToolTip() {
        if (shown) shown = false;
        showTip = false;
    }

    /**
   * Find the main container
   */
    private void findMainContainer() {
    }

    /**
   * Show the tooltip at the specified point
   * 
   * @param pt
   */
    protected void showTip(Point pt) {
        if (tooltipThread == null) {
            tooltipThread = new TooltipThread(this);
            tooltipThread.start();
        }
        tooltipThread.setTipLocation(pt);
    }

    /**
   * Invoked when the mouse exits a component.
   */
    public void mouseExit(MouseEvent e) {
        showTip = false;
        removeToolTip();
    }

    /**
   * Invoked when the mouse enters a component.
   * 
   * @param e
   *          mouse event
   */
    public void mouseEnter(MouseEvent e) {
        if (!showTip) showTip = true;
    }

    /**
   * Invoked when a mouse button has been pressed on a component.
   * 
   * @param e
   *          mouse event
   */
    public void mouseDown(MouseEvent e) {
        showTip = false;
        removeToolTip();
    }

    public void mouseUp(MouseEvent e) {
    }

    public void mouseHover(MouseEvent e) {
    }

    public void mouseDoubleClick(MouseEvent e) {
    }

    /**
   * Support for setting the tooltips
   */
    class TooltipThread extends Thread {

        Point tipLocation;

        ToolTip tooltip;

        int maxIdle;

        /**
     * Create a tooltip thread
     * 
     * @param tt
     *          tooltip
     */
        public TooltipThread(ToolTip tt) {
            tooltip = tt;
            maxIdle = 100;
        }

        /**
     * Set the location
     * 
     * @param pt
     *          point
     */
        public synchronized void setTipLocation(Point pt) {
            tipLocation = pt;
        }

        /**
     * Run method
     */
        public void run() {
            int idleCount = 0;
            Date startTime = new Date();
            do {
                try {
                    sleep(tooltip.lag);
                    idleCount++;
                } catch (InterruptedException ex) {
                }
                if (tooltip.showTip) idleCount = 0;
                if (new Date().getTime() - startTime.getTime() > 5000) {
                    showTip = false;
                    removeToolTip();
                    break;
                }
            } while (idleCount < maxIdle);
            tooltip.tooltipThread = null;
        }
    }

    /**
   * Set the tooltip text
   * @param text the new text
   */
    public void setToolTip(String text) {
        super.setToolTipText(text);
    }

    /**
   * Get the tooltip text
   * @return the existing text if any
   */
    public String getToolTip() {
        return super.getToolTipText();
    }

    /**
   * Set the image name
   * @param imageName the relative URL of the new image
   */
    public void setImage(String imageName) {
        try {
            InputStream url = ProjectManager.getCurrentProject().getUrl(imageName).openStream();
            Image im = new Image(getDisplay(), url);
            if (im != null) setImage(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
