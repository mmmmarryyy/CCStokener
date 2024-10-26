package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.swt.widgets.Composite;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.TextHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

/**
 * Draws text. The text may be wrapped over multiple lines. Double buffering is
 * switched off by default.
 * <p>
 * Copyright (c) Formaria Ltd., 2008<br>
 * License: see license.txt
 * 
 * @version $Revision: 1.3 $
 */
public class Label extends org.eclipse.swt.widgets.Label implements TextHolder {

    protected String name;

    protected boolean doubleBuffered = false;

    protected int bufferWidth;

    protected int bufferHeight;

    /**
   * Create an Label
   * 
   * @param parent
   *          parent object
   */
    public Label(Object parent) {
        super((Composite) parent, SWT.NONE);
    }

    /**
   * Suppress the subclasing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Get the alignment style constant
   * 
   * @return the alignment value
   */
    public int getAlignment() {
        return super.getAlignment();
    }

    /**
   * Sets the transparency of the text.
   * 
   * @param b
   *          true to make text transparent
   */
    public void setTransparent(boolean b) {
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
   * Set the image url
   * @param imageUrl the relative image URL
   */
    public void setImage(String imageUrl) {
        try {
            InputStream url = ProjectManager.getCurrentProject().getUrl(imageUrl).openStream();
            Image im = new Image(getDisplay(), url);
            if (im != null) setImage(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * Gets the preferred size of this component.
   * 
   * @return a dimension object indicating this component's preferred size
   * @see #getMinimumSize
   * @see LayoutManager
   */
    public Object getPreferredSize() {
        return super.getSize();
    }

    /**
   * Toggle use of double buffering when painting this component
   * 
   * @param buffer
   *          true to double buffer
   */
    public void setDoubleBuffered(boolean buffer) {
        doubleBuffered = buffer;
    }

    /**
   * Set the text
   * 
   * @param text
   *          the new text
   */
    public void setText(String text) {
        if (text != null) super.setText(text);
    }
}
