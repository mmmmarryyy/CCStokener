package org.npsnet.v.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import org.npsnet.v.kernel.Module;
import org.npsnet.v.services.gui.ModuleContextMenuItemFactory;
import org.npsnet.v.services.gui.InvokerInfo;
import org.npsnet.v.services.resource.ResourceDescriptor;

/**
 * A context menu item factory for prototype options.
 *
 * @author Andrzej Kapolka
 */
public class PrototypeContextItemFactory implements ModuleContextMenuItemFactory {

    /**
     * The load prototype option.
     */
    private static final String LOAD = "Load Prototype";

    /**
     * The save prototype option.
     */
    private static final String SAVE = "Save Prototype";

    /**
     * The publish prototype option.
     */
    private static final String PUBLISH = "Publish Prototype";

    /**
     * The view/edit prototype option.
     */
    private static final String VIEW_EDIT = "View/Edit Prototype";

    /**
     * A private menu item listener class.
     */
    private class MenuItemListener implements ActionListener {

        private Module target;

        private InvokerInfo invokerInfo;

        public MenuItemListener(Module pTarget, InvokerInfo pInvokerInfo) {
            target = pTarget;
            invokerInfo = pInvokerInfo;
        }

        public void actionPerformed(ActionEvent ae) {
            Window win = SwingUtilities.getWindowAncestor(invokerInfo.getComponent());
            if (ae.getActionCommand().equals(LOAD)) {
                URLChooser uc;
                if (win instanceof Frame) {
                    uc = new URLChooser((Frame) win);
                } else {
                    uc = new URLChooser((Dialog) win);
                }
                uc.setTitle("Load Prototype");
                uc.setLabelText("  Prototype URL:  ");
                uc.setNullSelectionValid(false);
                uc.setFileFilter(new javax.swing.filechooser.FileFilter() {

                    public boolean accept(File f) {
                        return f.getName().endsWith(".xml") || f.isDirectory();
                    }

                    public String getDescription() {
                        return "XML Prototype Files";
                    }
                });
                uc.setResourceFilter(new ResourceFilter() {

                    public boolean accept(ResourceDescriptor rd) {
                        return rd.getType().equals(ResourceDescriptor.NPSNETV_PROTOTYPE);
                    }
                });
                GUIUtilities.positionDialog(invokerInfo.getComponent(), invokerInfo.getInvocationPoint(), uc);
                if (uc.showDialog(null)) {
                    URL url = uc.getSelectedURL();
                    try {
                        target.applyPrototype(url);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(invokerInfo.getComponent(), e, "Error Loading Prototype", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (ae.getActionCommand().equals(SAVE)) {
                URLChooser uc;
                if (win instanceof Frame) {
                    uc = new URLChooser((Frame) win);
                } else {
                    uc = new URLChooser((Dialog) win);
                }
                uc.setTitle("Save Prototype");
                uc.setLabelText("  Prototype URL:  ");
                uc.setNullSelectionValid(false);
                uc.setFileFilter(new javax.swing.filechooser.FileFilter() {

                    public boolean accept(File f) {
                        return f.getName().endsWith(".xml") || f.isDirectory();
                    }

                    public String getDescription() {
                        return "XML Prototype Files";
                    }
                });
                uc.setResourceFilter(new ResourceFilter() {

                    public boolean accept(ResourceDescriptor rd) {
                        return rd.getType().equals(ResourceDescriptor.NPSNETV_PROTOTYPE);
                    }
                });
                GUIUtilities.positionDialog(invokerInfo.getComponent(), invokerInfo.getInvocationPoint(), uc);
                if (uc.showDialog(null)) {
                    URL url = uc.getSelectedURL();
                    try {
                        PrintStream ps;
                        HttpURLConnection huc = null;
                        if (url.getProtocol().equals("file")) {
                            ps = new PrintStream(new FileOutputStream(url.getFile()));
                        } else {
                            URLConnection urlc = url.openConnection();
                            urlc.setDoOutput(true);
                            if (urlc instanceof HttpURLConnection) {
                                huc = ((HttpURLConnection) urlc);
                                huc.setRequestMethod("PUT");
                            }
                            ps = new PrintStream(urlc.getOutputStream());
                        }
                        target.writePrototype(ps);
                        if (huc != null) {
                            huc.getResponseCode();
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(invokerInfo.getComponent(), e, "Error Saving Prototype", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (ae.getActionCommand().equals(PUBLISH)) {
                PublishPrototypeDialog ppd;
                if (win instanceof Frame) {
                    ppd = new PublishPrototypeDialog((Frame) win, target);
                } else {
                    ppd = new PublishPrototypeDialog((Dialog) win, target);
                }
                GUIUtilities.positionDialog(invokerInfo.getComponent(), invokerInfo.getInvocationPoint(), ppd);
                ppd.show();
            } else if (ae.getActionCommand().equals(VIEW_EDIT)) {
                ViewEditPrototypeDialog vepd;
                if (win instanceof Frame) {
                    vepd = new ViewEditPrototypeDialog((Frame) win, target);
                } else {
                    vepd = new ViewEditPrototypeDialog((Dialog) win, target);
                }
                GUIUtilities.positionDialog(invokerInfo.getComponent(), invokerInfo.getInvocationPoint(), vepd);
                vepd.show();
            }
        }
    }

    /**
     * Returns the weight of this option handler.  The weight is used to
     * determine the order in which interaction options are displayed.
     *
     * @return the weight of this option handler
     */
    public double getWeight() {
        return 0.02;
    }

    /**
     * Activates the default menu item associated with this factory
     * on the specified target, if present and applicable.
     *
     * @param target the target module
     * @param ii information about the invoker
     * @return <code>true</code> if the default item was successfully applied,
     * <code>false</code> if there was no default item or the default item was
     * not applicable to the given target
     */
    public boolean selectDefaultContextMenuItem(Module target, InvokerInfo ii) {
        return false;
    }

    /**
     * Creates and returns a new set of context menu items for the specified
     * target.  Each element of the returned <code>Enumeration</code> is
     * a <code>JMenuItem</code>.  If none of the items created by this factory
     * are applicable to the specified target, an empty <code>Enumeration</code>
     * is returned.
     *
     * @param target the target module
     * @param ii information about the invoker
     * @return an <code>Enumeration</code> over the set of newly created
     * menu items
     */
    public Enumeration newContextMenuItems(Module target, InvokerInfo ii) {
        JMenuItem loadItem = new JMenuItem(LOAD), saveItem = new JMenuItem(SAVE), publishItem = new JMenuItem(PUBLISH), viewEditItem = new JMenuItem(VIEW_EDIT);
        MenuItemListener mil = new MenuItemListener(target, ii);
        loadItem.addActionListener(mil);
        saveItem.addActionListener(mil);
        publishItem.addActionListener(mil);
        viewEditItem.addActionListener(mil);
        Vector items = new Vector();
        items.add(loadItem);
        items.add(saveItem);
        items.add(publishItem);
        items.add(viewEditItem);
        return items.elements();
    }
}
