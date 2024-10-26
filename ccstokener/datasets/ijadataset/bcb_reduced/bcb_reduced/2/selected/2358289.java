package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.formaria.aria.ImageHolder;
import org.formaria.aria.Project;
import org.formaria.aria.ProjectManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * <p>
 * Draws an image
 * </p>
 * <p>
 * Copyright (c) Formaria Ltd., 2008
 * </p>
 * License: see license.txt $Revision: 2.18 $
 */
public class Image extends Canvas implements ImageHolder {

    private org.eclipse.swt.graphics.Image image = null;

    private String imageName;

    private boolean scale = false;

    private boolean alignment = false;

    private boolean full = false;

    private Color bg = null;

    /**
   * The current project
   */
    protected Project currentProject;

    /**
   * Create a new image
   * 
   * @param parent
   *          parent object
   */
    public Image(Object parent) {
        super((Composite) parent, SWT.BORDER | SWT.NO_BACKGROUND);
        currentProject = ProjectManager.getCurrentProject();
        addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent event) {
                paint(event.gc);
            }
        });
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Sets the image to display.
   * 
   * @param img
   *          the image
   */
    public void setImage(java.awt.Image img) {
        if (imageName != null && imageName.length() > 0) {
            try {
                InputStream url = currentProject.getUrl(imageName).openStream();
                image = new org.eclipse.swt.graphics.Image(getDisplay(), url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    redraw();
                }
            });
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
   * Set the background color
   * @param clrStr the new color string (hex)
   */
    public void setBg(String clrStr) {
        bg = new Color(getDisplay(), Integer.parseInt(clrStr.substring(0, 2), 16), Integer.parseInt(clrStr.substring(2, 4), 16), Integer.parseInt(clrStr.substring(4, 6), 16));
    }

    /**
   * Renders the component
   * 
   * @param g
   *          the graphics context
   */
    protected void paint(GC gc) {
        if (image != null) {
            Rectangle boundsImage = image.getBounds();
            Rectangle size = getBounds();
            if (full) setBounds(size.x, size.y, boundsImage.width, boundsImage.height);
            if (scale || full) {
                if (bg != null) {
                    gc.setBackground(bg);
                    gc.fillRectangle(new Rectangle(0, 0, size.width, size.height));
                }
                gc.drawImage(image, boundsImage.x, boundsImage.y, boundsImage.width, boundsImage.height, 0, 0, size.width, size.height);
            } else {
                int srcX = 0;
                int srcY = 0;
                int srcWidth = boundsImage.width;
                int srcHeight = boundsImage.height;
                int destX = srcX;
                int destY = srcY;
                int destWidth = srcWidth;
                int destHeight = srcHeight;
                if (alignment) {
                    if (boundsImage.width < size.width) {
                        srcX = 0;
                        destX = (size.width - boundsImage.width) / 2;
                        srcWidth = boundsImage.width;
                        destWidth = boundsImage.width;
                    } else if (boundsImage.width > size.width) {
                        srcX = (boundsImage.width - size.width) / 2;
                        destX = 0;
                        srcWidth = size.width;
                        destWidth = size.width;
                    }
                    if (boundsImage.height < size.height) {
                        srcY = 0;
                        destY = (size.height - boundsImage.height) / 2;
                        srcHeight = boundsImage.height;
                        destHeight = boundsImage.height;
                    } else if (boundsImage.height > size.height) {
                        srcY = (boundsImage.height - size.height) / 2;
                        destY = 0;
                        srcHeight = size.height;
                        destHeight = size.height;
                    }
                }
                if (bg != null) {
                    gc.setBackground(bg);
                    if (this instanceof HotspotImage) {
                        gc.fillRectangle(new Rectangle(0, 0, size.width, destY));
                        gc.fillRectangle(new Rectangle(0, destY, destX, destHeight));
                        gc.fillRectangle(new Rectangle(destX + destWidth, destY, destX, destHeight));
                        gc.fillRectangle(new Rectangle(0, destY + destHeight, size.width, destY));
                    } else gc.fillRectangle(new Rectangle(0, 0, size.width, size.height));
                }
                gc.drawImage(image, srcX, srcY, srcWidth, srcHeight, destX, destY, destWidth, destHeight);
            }
        }
    }

    /**
   * Sets the name of the image being displayed.
   * @param name the image name
   */
    public void setImageName(String name) {
        imageName = name;
        setImage(currentProject.getImage(imageName));
    }

    /**
   * Gets the name of the image being displayed.
   * @return the image name
   */
    public String getImageName() {
        return imageName;
    }

    public boolean isScale() {
        return scale;
    }

    public void setScale(boolean scale) {
        this.scale = scale;
    }

    public boolean getAlignment() {
        return alignment;
    }

    public void setAlignment(boolean alignment) {
        this.alignment = alignment;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }
}
