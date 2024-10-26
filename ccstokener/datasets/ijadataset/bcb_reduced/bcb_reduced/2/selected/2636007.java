package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.formaria.aria.ProjectManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;

/**
 * <p>
 * Draws an expanditem
 * </p>
 * <p>
 * Copyright (c) Formaria Ltd., 2008
 * </p>
 * License: see license.txt $Revision: 2.18 $ Notice : You must put a layout in
 * parameter
 */
public class ExpandItem extends Composite {

    private org.eclipse.swt.widgets.ExpandItem item;

    /**
   * Create a new expanditem
   * 
   * @param parent
   *          parent object
   */
    public ExpandItem(Object parent) {
        super((ExpandBar) parent, SWT.NONE);
        item = new org.eclipse.swt.widgets.ExpandItem((ExpandBar) parent, SWT.NONE);
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Set the image url
   * @param imageUrl the relative image URL
   */
    public void setImage(String imageUrl) {
        try {
            InputStream url = ProjectManager.getCurrentProject().getUrl(imageUrl).openStream();
            Image im = new Image(getDisplay(), url);
            if (im != null) item.setImage(im);
        } catch (IOException e) {
            e.printStackTrace();
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
   * Set the new content
   */
    public void setContent(String content) {
        item.setText(content);
    }

    /**
   * Set the display attributes
   */
    public void setDisplayAttributes() {
        item.setHeight(computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        item.setControl(this);
    }
}
