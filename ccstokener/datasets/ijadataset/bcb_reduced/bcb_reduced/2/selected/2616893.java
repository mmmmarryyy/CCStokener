package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.swt.widgets.Button;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.ValueHolder;
import org.formaria.aria.StateHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

/**
 * A wrapper for the SWT Checkbox class
 * <p>
 * Copyright (c) Formaria Ltd., <br>
 * License: see license.txt
 * 
 * @version $Revision: 2.6 $
 */
public class Checkbox extends Button implements StateHolder, ValueHolder {

    /**
   * The checkbox value
   */
    protected Object value;

    /**
   * Create a new checkbox
   * 
   * @param parent
   *          parent object
   */
    public Checkbox(Object parent) {
        super((Composite) parent, SWT.CHECK);
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
   * Get the component state
   * 
   * @return the Boolean value for the state
   */
    public Object getComponentState() {
        return new Boolean(getSelection());
    }

    /**
   * Set the component state
   * 
   * @param o
   *          the selection state. Possible values: 1 or true and 0 or false
   */
    public void setComponentState(Object o) {
        if (o != null) {
            String objValue = o.toString();
            boolean value = objValue.equals("1");
            if (!value) value |= objValue.equals("true");
            setSelection(value);
        } else setSelection(false);
    }

    /**
   * Get the checkbox's value if it has one or else get the text
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
