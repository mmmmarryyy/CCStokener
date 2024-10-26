package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.formaria.aria.ValueHolder;
import org.formaria.aria.ProjectManager;
import org.eclipse.swt.graphics.Image;

/**
 * <p>
 * A wrapper for the SWT Button class. In addition to wrapping the button this
 * object can hold a value.
 * </p>
 * <p>
 * Copyright (c) Formaria Ltd., 2008<br>
 * License: see license.txt
 * 
 * @version $Revision: 1.2 $
 */
public class Button extends org.eclipse.swt.widgets.Button implements ValueHolder {

    /**
   * The button value
   */
    protected Object value;

    /**
   * Create a new button
   * 
   * @param parent
   *          parent object
   */
    public Button(Object parent) {
        super((Composite) parent, SWT.PUSH);
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
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
   * Get the radiobutton's value if it has one or else get the text
   * 
   * @return the value for this button
   */
    public Object getValue() {
        if (value != null) return value;
        return getText();
    }

    /**
   * Set the value associated with this button
   * 
   * @param newValue
   *          the new button value
   */
    public void setValue(Object newValue) {
        value = newValue;
    }
}
