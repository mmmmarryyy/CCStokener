package org.formaria.swt;

import java.io.IOException;
import java.io.InputStream;
import org.formaria.aria.ModelHolder;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.data.DataModel;
import org.formaria.aria.events.Actionable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * <p>
 * Draws a coolitem
 * </p>
 * <p>
 * Copyright (c) Formaria Ltd., 2008
 * </p>
 * License: see license.txt $Revision: 2.18 $
 */
public class ToolItem extends org.eclipse.swt.widgets.ToolItem implements Actionable, ModelHolder {

    protected Menu menu;

    protected DataModel model;

    protected final ToolBar tb;

    /**
   * Constructs a new toolitem for a toolbar
   * 
   * @param parent
   *          parent toolbar
   * @param style
   *          the style
   */
    public ToolItem(ToolBar parent, int style) {
        super(parent, style);
        tb = parent;
        if (style == SWT.DROP_DOWN) menu = new Menu(getDisplay().getActiveShell(), SWT.POP_UP);
    }

    /**
   * Suppress the subclassing exception
   */
    protected void checkSubclass() {
    }

    /**
   * Set an Action for the toolitem - does nothing
   * 
   * @param action
   *          the action object
   */
    public void setAction(Object action) {
    }

    /**
   * Set one or more attributes of the component.
   * 
   * @param attribName
   *          the name of the attribute
   * @param attribValue
   *          the value of the attribute
   * @return 0 for success, non zero for failure or to require some further
   *         action
   */
    public int setAttribute(String attribName, Object attribValue) {
        String attribNameLwr = attribName.toLowerCase();
        String attribValueStr = (String) attribValue;
        int rc = 0;
        if (attribNameLwr.equals("content")) setText(attribValueStr); else if (attribNameLwr.equals("tooltip")) setToolTip(attribValueStr); else if (attribNameLwr.equals("image")) setImage(attribValueStr); else rc = -1;
        ((ToolBar) getParent()).setDisplayAttributes();
        return rc;
    }

    /**
   * Set the DataModel which we will be generating the toolitem dropdown from
   * 
   * @param xmodel
   *          the DataModel of data
   */
    public void setDataModel(DataModel xmodel) {
        model = xmodel;
        if (model != null) {
            model.get();
        }
        update();
    }

    /**
   * Get the model
   */
    public DataModel getModel() {
        return model;
    }

    /**
   * Update the toolitem dropdown
   */
    public void update() {
        menu = new Menu(getDisplay().getActiveShell(), SWT.POP_UP);
        int nbItems = model.getNumChildren();
        for (int i = 0; i < nbItems; i++) {
            String text = getText(model, i);
            MenuItem mi = new MenuItem(menu, SWT.PUSH);
            mi.setText(text);
        }
        addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (event.detail == SWT.ARROW) {
                    Rectangle rect = getBounds();
                    Point pt = new Point(rect.x, rect.y + rect.height);
                    pt = tb.toDisplay(pt);
                    menu.setLocation(pt.x, pt.y);
                    menu.setVisible(true);
                }
            }
        });
    }

    public Menu getMenu() {
        return menu;
    }

    /**
   * Get the appropriate text inside the model
   * 
   * @param xmodel
   *          parent model
   * @param i
   *          index of the children
   * @return the text
   */
    public String getText(DataModel xmodel, int i) {
        DataModel xm = xmodel.get(i);
        String value = xm.getAttribValueAsString(xm.getAttribute("value"));
        if (value != null) return value;
        return xm.getAttribValueAsString(xm.getAttribute("id"));
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
