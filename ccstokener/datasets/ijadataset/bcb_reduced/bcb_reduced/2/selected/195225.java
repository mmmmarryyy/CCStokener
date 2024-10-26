package org.formaria.swt;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.formaria.debug.AWTExceptionHandler;
import org.formaria.xml.XmlElement;
import org.formaria.aria.PageSupport;
import org.formaria.aria.ApplicationContext;
import org.formaria.aria.ComponentFactory;
import org.formaria.aria.Project;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.StartupObject;
import org.formaria.aria.build.BuildProperties;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;

/**
 * <p>
 * This class is constructed with a window or frame and can be part of an applet
 * or an application. The class acts as the main entry point to an Aria
 * application and provides some of the infrastructure needed to support the
 * application.
 * </p>
 * <p>
 * The applet can provide support for a frameset or a single page. Page display
 * functions are also supported to allow the application to display more than a
 * single page or change the page that is displayed.
 * </p>
 * <p>
 * By choosing either the AWT or Swing version of the Applet you choose to have
 * either an AWT or a Swing application/applet. In general once this choice has
 * been made you should not mix toolkits.
 * </p>
 * <p>
 * Copyright (c) Formaria Ltd., <br>
 * License: see license.txt
 * 
 * @version $Revision: 1.12 $
 */
public class Applet extends Composite implements StartupObject {

    protected Shell clientShell;

    protected Display display;

    private Composite hiddenPage;

    protected ApplicationContext applicationContext;

    /**
   * main method to be invoked as an application. This method is invoked as the
   * entry point to the 'Application', it is not used if an Applet is being
   * launched. This method establishes the frame within which the application
   * runs. If overloading this method remeber to call the setup method.
   * 
   * @param args
   *          the command line arguments
   */
    public static void main(String args[]) {
        if (BuildProperties.DEBUG) AWTExceptionHandler.register();
        Display display = Display.getCurrent();
        if (display == null) display = new Display();
        Shell clientShell = new Shell(display);
        new Applet(args, clientShell, display);
    }

    /**
   * A default constructor. Most of the setup work is actually done by the
   * initialize method and is called by the main method or the init method
   * depending on whether or not an application of applet is being launched.
   */
    public Applet() {
        this(null, null, null);
    }

    /**
   * Create a new application. Most of the setup work is actually done by the
   * initialize method and is called by the main method or the init method
   * depending on whether or not an application of applet is being launched.
   * 
   * @param args
   *          the application command-line arguments
   * @param f
   *          the parent shell
   * @param d
   *          the parent display
   */
    public Applet(String args[], Shell f, Display d) {
        super(f, 0);
        clientShell = f;
        display = d;
        SwtWidgetAdapter.getInstance();
        SwtDataBindingFactory.register(ProjectManager.getCurrentProject(this));
        hiddenPage = new Composite(clientShell, 0);
        SwtWidgetAdapter.setHiddenPage(hiddenPage);
        applicationContext = new ApplicationContext(this, "org.formaria.swt.SwtTarget", args);
    }

    /**
   * Get the parent object
   * 
   * @return the parent
   */
    public Object getParentObject() {
        return getParent();
    }

    /**
   * Get the package name for the default widget set
   */
    public String getWidgetClassPackage() {
        return PageSupport.ARIA_SWT_PACKAGE;
    }

    /**
   * Setup frameset. This method is called prior to the addition of any target
   * areas in the framset and prior to the display of any pages. Since this
   * applet does not support configurable framesets, this method ignores the
   * parameter values passed.
   * 
   * @param params
   *          the framset parameters if any
   */
    public void setupFrameset(Hashtable params) {
    }

    /**
   * Display a window decoration, for example a toolbar
   * 
   * @param page
   *          the new page
   * @param constraint
   *          a value controlling how and where the decoration is displayed,
   *          this value is application type specific
   * @return the page being displayed
   */
    public Object displayDecoration(PageSupport page, String constraint) {
        return null;
    }

    /**
   * Refresh the parent shell
   */
    public void refresh() {
        if (clientShell != null) clientShell.layout();
        clientShell.redraw();
        while (!clientShell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        dispose();
    }

    /**
   * <p>Restore the normal page views, as in the case of the docking layout where 
   * panels may be zoomed or minimized. The method is called prior to the 
   * display of a new page.</p>
   * <p>In this context the method has no effect.</p>
   */
    public void restoreViews() {
    }

    /**
   * Get the panel which has the content
   */
    public Object getContentPaneEx() {
        return this;
    }

    /**
   * Set the title to the parent shell
   */
    public void setAppTitle(String title) {
        try {
            if (clientShell != null) clientShell.setText(title);
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
    }

    /**
   * Set the icon to the parent shell
   */
    public void setIcon(java.awt.Image icon) {
    }

    /**
   * Setup the windowing.
   * 
   * @param context
   *          the owner application context
   * @param currentProject
   *          the owner project
   * @param clientWidth
   *          the desired width of the application
   * @param clientHeight
   *          the desired height of the application
   */
    public void setupWindow(ApplicationContext context, Project currentProject, int clientWidth, int clientHeight) {
        currentProject.setStartupParam("MainClass", "org.formaria.swt.Applet");
        ComponentFactory.setRequiresParent(true);
        currentProject.setStartupParam("DefaultClass", "org.formaria.swt.SwtPage");
        currentProject.setEventHandlerClass("org.formaria.swt.SwtEventHandler");
        clientShell.setSize(clientWidth, clientHeight);
        clientShell.setVisible(true);
        clientShell.setLayout(new RowLayout());
        currentProject.setObject("Applet", this);
        currentProject.setObject("ClientShell", clientShell);
        currentProject.setObject("Display", display);
        String icon = currentProject.getStartupParam("Icon");
        if (icon != null) {
            try {
                InputStream url = currentProject.getUrl(icon).openStream();
                clientShell.setImage(new Image(display, url));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        clientShell.open();
    }

    /**
   * Invoked when used as an applet. Sets up the startup file and initialises
   * the application. Reads the applet parameters and calls initialize.
   */
    public void init() {
        applicationContext.init();
    }

    /**
   * Gets the Frame containing the applet.
   * 
   * @return Frame which is the applet or application's parent
   */
    public Object getFrame() {
        return display;
    }

    /**
   * Validates the Applet
   */
    public void validate() {
    }

    /**
   * Sets the size to the shell
   * 
   * @param size
   *          size of the shell
   */
    public void setSize(Dimension size) {
        clientShell.setSize(size.width, size.height);
    }

    /**
   * Gets of the document
   */
    public URL getDocumentBase() {
        return null;
    }

    /**
   * Get a startup parameter
   * 
   * @param param
   *          the name of the parameter
   */
    public String getParameter(String param) {
        return null;
    }

    /**
   * Get the menubar, setting it up if it is not already added to the
   * application frame
   * 
   * @return the menu bar
   */
    public Object getApplicationMenuBar() {
        return null;
    }

    /**
   * Set the menubar
   * 
   * @param mb
   *          the menubar
   */
    public void setApplicationMenuBar(Object mb) {
    }

    /**
   * Restore the application state
   * @return the elements containing the page state
   */
    public void restoreState(XmlElement stateElement) {
        String[] b = stateElement.getAttribute("b").split(",");
        Rectangle bounds = new Rectangle(Integer.parseInt(b[0]), Integer.parseInt(b[1]), Integer.parseInt(b[2]), Integer.parseInt(b[3]));
        setBounds(bounds);
    }

    /**
   * Save the application state
   * @param the elements to hold the page state
   */
    public void saveState(XmlElement stateElement) {
        Rectangle bounds = getBounds();
        stateElement.setAttribute("b", "" + bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height + ",");
    }
}
