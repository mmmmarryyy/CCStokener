package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.TextHolder;
import org.formaria.aria.events.Actionable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.formaria.aria.ImageHolder;

/**
 * A wrapper for menu items
 * <p>
 * Copyright (c) Formaria Ltd., <br>
 * 
 * @version 1.0
 */
public class MenuItem extends org.eclipse.swt.widgets.MenuItem implements TextHolder, Actionable, ImageHolder {

    /**
   * Create a new menuitem
   * 
   * @param parent
   *          parent menu
   */
    public MenuItem(Menu parent) {
        super(parent, SWT.PUSH);
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Set an action object
   * 
   * @param action
   *          the action object
   */
    public void setAction(Object instance) {
    }

    /**
   * Set the image url
   * @param imageUrl the relative image URL
   */
    public void setImageName(String imageUrl) {
        try {
            InputStream url = ProjectManager.getCurrentProject().getUrl(imageUrl).openStream();
            Image im = new Image(getDisplay(), url);
            if (im != null) setImage(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImage(java.awt.Image img) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getImageName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
