package org.siberia.ui.component.docking;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import net.infonode.docking.DockingWindow;
import net.infonode.docking.DockingWindowAdapter;
import net.infonode.docking.DockingWindowListener;
import net.infonode.docking.OperationAbortedException;
import net.infonode.docking.RootWindow;
import net.infonode.docking.TabWindow;
import net.infonode.docking.View;
import net.infonode.docking.WindowBar;
import net.infonode.docking.WindowPopupMenuFactory;
import net.infonode.docking.action.MaximizeWithAbortWindowAction;
import net.infonode.docking.action.NullWindowAction;
import net.infonode.docking.action.RestoreParentWithAbortWindowAction;
import net.infonode.docking.action.RestoreWithAbortWindowAction;
import net.infonode.docking.action.StateDependentWindowAction;
import net.infonode.docking.theme.BlueHighlightDockingTheme;
import net.infonode.docking.theme.ClassicDockingTheme;
import net.infonode.docking.theme.DefaultDockingTheme;
import net.infonode.docking.theme.DockingWindowsTheme;
import net.infonode.docking.theme.GradientDockingTheme;
import net.infonode.docking.theme.LookAndFeelDockingTheme;
import net.infonode.docking.theme.ShapedGradientDockingTheme;
import net.infonode.docking.theme.SlimFlatDockingTheme;
import net.infonode.docking.theme.SoftBlueIceDockingTheme;
import net.infonode.docking.util.PropertiesUtil;
import net.infonode.docking.util.ViewMap;
import net.infonode.docking.util.WindowMenuUtil;
import org.apache.log4j.Logger;
import org.siberia.ResourceLoader;
import org.siberia.editor.Editor;
import org.siberia.exception.ResourceException;
import org.siberia.ui.component.docking.layout.BorderViewLayout;
import org.siberia.ui.component.docking.layout.ViewLayout;
import org.siberia.ui.component.docking.layout.ViewConstraints;
import org.siberia.ui.component.docking.layout.factory.ViewLayoutFactory;
import org.siberia.ui.component.docking.view.AccessibleView;
import org.siberia.ui.component.docking.view.EditorView;
import org.siberia.utilities.random.Randomizer;

/**
 *
 * Extension of the RootWindow to ensure inner component enlargement when Maj + escape is pressed and
 * to allow using a RootWindow in an xml graphical user interface description
 *
 * @author alexis
 */
public class RootDockingPanel extends RootWindow implements PropertyChangeListener {

    /** logger */
    private transient Logger logger = Logger.getLogger(RootDockingPanel.class);

    /** style */
    public static final String STYLE_BLUE = "blue";

    public static final String STYLE_CLASSIC = "classic";

    public static final String STYLE_DEFAULT = "default";

    public static final String STYLE_GRADIENT = "gradient";

    public static final String STYLE_LAF = "lookandfeel";

    public static final String STYLE_SHAPED = "shaped";

    public static final String STYLE_SLIMFLAT = "slimflat";

    public static final String STYLE_ICE = "ice";

    /** views */
    private Map<Integer, View> views = null;

    /** viewmap */
    private ViewMap map = null;

    /** view popup menu activated */
    private boolean viewPopupActivated = false;

    /** window listener to force the call of removeView() */
    private DockingWindowListener listener = null;

    /** layout to be used when new view has to be displayed in the root window */
    private ViewLayout layout = null;

    /** Creates a new instance of RootDockingPanel */
    public RootDockingPanel() {
        this(new ViewMap());
        this.addListener(new DockingWindowAdapter() {

            public void viewFocusChanged(View previouslyFocusedView, View focusedView) {
                logger.debug("calling viewFocusChanged(" + previouslyFocusedView + ", " + focusedView + ")");
                Editor previouslyFocusedEditor = null;
                Editor focusedEditor = null;
                if (previouslyFocusedView instanceof EditorView) {
                    previouslyFocusedEditor = ((EditorView) previouslyFocusedView).getEditor();
                }
                if (focusedView instanceof EditorView) {
                    focusedEditor = ((EditorView) focusedView).getEditor();
                }
                logger.debug("calling viewFocusChanged(editor=" + previouslyFocusedEditor + ", editor=" + focusedEditor + ")");
                if (previouslyFocusedEditor != null) {
                    previouslyFocusedEditor.fireEditorFocusLost(focusedEditor);
                }
                if (focusedEditor != null) {
                    focusedEditor.fireEditorFocusGained(previouslyFocusedEditor);
                }
            }
        });
    }

    /** Creates a new instance of RootDockingPanel
     *  @param serializer a ViewSerializer
     */
    private RootDockingPanel(ViewMap views) {
        super(false, views);
        this.setPopupViewActivated(true);
        this.setTheme(STYLE_GRADIENT);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.SHIFT_MASK), "enlarge");
        this.getActionMap().put("enlarge", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                DockingWindow window = getFocusedView();
                if (window != null) {
                    if ((window.getWindowParent() instanceof WindowBar) && getRootWindowProperties().getDoubleClickRestoresWindow()) RestoreWithAbortWindowAction.INSTANCE.perform(window); else {
                        new StateDependentWindowAction(MaximizeWithAbortWindowAction.INSTANCE, NullWindowAction.INSTANCE, RestoreParentWithAbortWindowAction.INSTANCE).perform(window);
                    }
                }
            }
        });
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK), "closeCurrentView");
        this.getActionMap().put("closeCurrentView", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                Component c = FocusManager.getCurrentManager().getFocusOwner();
                System.out.println("focus owner is  :" + c.getClass());
                while (c != null) {
                    if (c instanceof TabWindow) {
                        break;
                    }
                    if (c instanceof DockingWindow) {
                        try {
                            System.out.println("closing " + c.getClass());
                            ((DockingWindow) c).closeWithAbort();
                        } catch (OperationAbortedException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    }
                    System.out.println("current : " + c.getClass());
                    c = c.getParent();
                }
            }
        });
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK), "goToNextNeighbour");
        this.getActionMap().put("goToNextNeighbour", new DockingTabbedPaneNavigationAction(0));
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK), "goToPreviousNeighbour");
        this.getActionMap().put("goToPreviousNeighbour", new DockingTabbedPaneNavigationAction(1));
        this.listener = new DockingWindowAdapter() {

            public void windowClosed(DockingWindow window) {
                System.err.println("windowClosed");
                if (window instanceof View) {
                    removeView((View) window);
                    if (window instanceof EditorView) {
                        ((EditorView) window).dispose();
                    }
                }
            }
        };
    }

    /** set the configuration file path for this panel
     *	@param configurationFilePath the siberia representation of a resource that represents a configuration file for a docking panel
     */
    public void setConfigurationFilePath(String configurationFilePath) {
        try {
            URL url = ResourceLoader.getInstance().getRcResource(configurationFilePath);
            Properties properties = new Properties();
            properties.load(url.openStream());
            String factoryClassName = properties.getProperty("layout.factory");
            if (factoryClassName == null) {
                logger.error("could not find parameter 'layout.factory' from resource '" + configurationFilePath + "'");
            } else {
                try {
                    Class factoryClass = ResourceLoader.getInstance().getClass(factoryClassName);
                    Object o = factoryClass.newInstance();
                    if (o instanceof ViewLayoutFactory) {
                        ViewLayout layout = ((ViewLayoutFactory) o).createViewLayout(properties);
                        this.setViewLayout(layout);
                        logger.info("setting layout " + layout + " for docking panel " + this);
                    } else {
                        logger.error("could not configure a docking panel with a non layout factory " + o);
                    }
                } catch (ResourceException e) {
                    logger.error("could not load class '" + factoryClassName, e);
                } catch (Exception e) {
                    logger.error("could not create an instance of class '" + factoryClassName, e);
                }
            }
        } catch (ResourceException e) {
            logger.error("could not load docking panel configuration file '" + configurationFilePath, e);
        } catch (IOException e) {
            logger.error("could not open stream on resource '" + configurationFilePath, e);
        }
    }

    /** method that print the window tree */
    public void printWindowTree() {
        System.out.println(this.getClass() + " " + this.hashCode());
        this.printWindowChild(this, 1);
    }

    /** method that print the tree of a DockingWindow
     *  @param window a DockingWindow
     *  @param level an integer representing the level of the window in the window tree
     */
    protected void printWindowChild(DockingWindow window, int level) {
        for (int i = 0; i < window.getChildWindowCount(); i++) {
            DockingWindow current = window.getChildWindow(i);
            StringBuffer tab = new StringBuffer();
            for (int j = 0; j < level; j++) {
                tab.append("|   ");
            }
            System.out.println(tab.toString() + current.getClass() + " " + current.hashCode());
            this.printWindowChild(current, level + 1);
        }
    }

    /** initialize the layout
     *  @param layout a ViewLayout
     */
    public void setViewLayout(ViewLayout layout) {
        if (logger.isDebugEnabled()) {
            logger.debug("setting layout " + layout + " for docking panel " + this);
        }
        this.layout = layout;
    }

    /** initialize the layout
     *  @param layoutClass the class of the layout
     */
    public void setViewLayout(Class layoutClass) {
        try {
            Object o = layoutClass.newInstance();
            if (o instanceof ViewLayout) this.setViewLayout((ViewLayout) o);
        } catch (Exception e) {
        }
    }

    /** return the layout to be used
     *  @return a ViewLayout
     */
    public ViewLayout getViewLayout() {
        if (this.layout == null) this.layout = new BorderViewLayout();
        return this.layout;
    }

    /** return an Object related to the given View<br>
     *  To overwritte for specific needs
     *  @param view a View
     *  @return an Object
     */
    public Object getRelatedInstanceTo(View view) {
        return null;
    }

    /** initialize the theme of the root window
     *  @param theme the name of a theme :<br>
     */
    public void setTheme(String theme) {
        if (theme != null) {
            DockingWindowsTheme t = null;
            if (theme.equalsIgnoreCase(STYLE_BLUE)) {
                t = new BlueHighlightDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_CLASSIC)) {
                t = new ClassicDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_DEFAULT)) {
                t = new DefaultDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_GRADIENT)) {
                t = new GradientDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_LAF)) {
                t = new LookAndFeelDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_SHAPED)) {
                t = new ShapedGradientDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_SLIMFLAT)) {
                t = new SlimFlatDockingTheme();
            } else if (theme.equalsIgnoreCase(STYLE_ICE)) {
                t = new SoftBlueIceDockingTheme();
            }
            if (t != null) {
                this.getRootWindowProperties().addSuperObject(t.getRootWindowProperties());
                this.getRootWindowProperties().addSuperObject(PropertiesUtil.createTitleBarStyleRootWindowProperties());
            }
        }
    }

    /** tell if the view popup menu have to be activated
     *  @param activated true if the view popup menu have to be activated
     */
    public void setPopupViewActivated(boolean activated) {
        WindowPopupMenuFactory popup = null;
        if (activated) {
            if (this.map == null) {
                this.map = new ViewMap();
            }
            popup = WindowMenuUtil.createWindowMenuFactory(this.map, true);
        }
        this.viewPopupActivated = activated;
        this.setPopupMenuFactory(popup);
    }

    /** return true if the view popup menu have to be activated
     *  @return true if the view popup menu have to be activated
     */
    public boolean isPopupViewActivated() {
        return this.viewPopupActivated;
    }

    /** show a view in the docking panel
     *  @param view a view to show
     */
    public void showView(View view) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling showView(" + view + ")");
        }
        if (view != null) {
            if (this.map != null) {
                if (this.map.contains(view)) {
                    view.restore();
                    if (logger.isDebugEnabled()) {
                        logger.debug("restoring view " + view);
                    }
                }
            }
            view.makeVisible();
            if (logger.isDebugEnabled()) {
                logger.debug("making view " + view + " visible");
            }
            view.restoreFocus();
            if (logger.isDebugEnabled()) {
                logger.debug("restoring focus on view " + view);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end of showView(" + view + ")");
        }
    }

    /** remove a view from the docking panel<br>
     *  If the view is accessible and is set to always accessible, then, the view could be show with <br>
     *  the popup menu of the root window.
     *  @param view the view to remove
     */
    public void removeView(View view) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling removeView(" + view + ")");
        }
        if (view != null) {
            if (this.map != null) {
                if (this.map.contains(view)) {
                    view.close();
                    if (logger.isDebugEnabled()) {
                        logger.debug("closing view " + view);
                    }
                    super.removeView(view);
                    boolean removeFromMap = true;
                    if (view instanceof AccessibleView) {
                        removeFromMap = !((AccessibleView) view).isAlwaysAccessible();
                        if (!removeFromMap) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("the view will not be removed because it must be always accessible");
                            }
                        }
                    }
                    if (removeFromMap) {
                        Iterator<Integer> ids = this.views.keySet().iterator();
                        int id = -1;
                        while (ids.hasNext()) {
                            Integer currentId = ids.next();
                            View currentView = this.views.get(currentId);
                            if (currentView == view) {
                                id = currentId.intValue();
                                break;
                            }
                        }
                        this.views.remove(id);
                        if (id >= 0) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("removing view " + view + " from the viewmap");
                            }
                            this.map.removeView(id);
                        } else {
                            logger.warn("could not remove view from viewmap");
                        }
                        if (view instanceof AccessibleView) {
                            ((AccessibleView) view).removePropertyChangeListener(AccessibleView.PROP_ACCESSIBILITY, this);
                        }
                        view.removeListener(this.listener);
                    }
                } else {
                    logger.warn("the viewmap does not contains view " + view + " --> could not be removed");
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end of removeView(" + view + ")");
        }
    }

    /** add a view to the docking panel and select it
     *  @param view a new view to add
     */
    public void addView(View view) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling addView(" + view + ")");
        }
        this.addView(view, true);
        if (logger.isDebugEnabled()) {
            logger.debug("end of addView(" + view + ")");
        }
    }

    /** add a view to the docking panel
     *  @param view a new view to add
     *  @param select true if the new View must be selected
     */
    public void addView(View view, boolean select) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling addView(" + view + ", select?" + select + ")");
        }
        if (view == null) {
            logger.warn("could not add a null view");
        } else {
            if (this.views == null) {
                this.views = new HashMap<Integer, View>();
                if (logger.isDebugEnabled()) {
                    logger.debug("initializing the view id maps");
                }
            }
            if (this.map == null) {
                this.map = new ViewMap();
                if (logger.isDebugEnabled()) {
                    logger.debug("initializing the viewmap");
                }
            }
            if (this.map.contains(view)) {
                logger.info("could not add view " + view + " cause the viewmap already contains it");
            } else {
                int id = 0;
                try {
                    id = Randomizer.randomInteger(this.views.keySet());
                } catch (Randomizer.RandomException e) {
                    e.printStackTrace();
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("assigning id '" + id + "' for view " + view);
                }
                boolean addToViewMap = true;
                if (view instanceof AccessibleView) addToViewMap = ((AccessibleView) view).isAlwaysAccessible();
                if (addToViewMap) this.map.addView(id, view);
                this.views.put(id, view);
                if (view instanceof AccessibleView) {
                    ((AccessibleView) view).addPropertyChangeListener(AccessibleView.PROP_ACCESSIBILITY, this);
                }
                view.addListener(this.listener);
                this.placeView(view);
                if (logger.isDebugEnabled()) {
                    logger.debug("placing view " + view);
                }
                DockingWindowUtilities.removeEmptyContainers(this);
            }
            if (select) {
                this.showView(view);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end of addView(" + view + ", select?" + select + ")");
        }
    }

    /** place an existing view
     *  @param view to view to place
     */
    protected void placeView(View view) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling placeView(" + view + ")");
        }
        this.placeView(view, null);
        if (logger.isDebugEnabled()) {
            logger.debug("end of placeView(" + view + ")");
        }
    }

    /** place an existing view
     *  @param view to view to place
     *  @param placement a ViewPlacement
     */
    protected void placeView(final View view, ViewConstraints placement) {
        if (logger.isDebugEnabled()) {
            logger.debug("calling placeView(" + view + ", " + placement + ")");
        }
        if (view == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("could not add a null view");
            }
        } else {
            if (view.getWindowParent() != null) {
                view.close();
                if (logger.isDebugEnabled()) {
                    logger.debug("view was already added --> calling close to remove it from its current parent");
                }
            }
            Object o = RootDockingPanel.this.getRelatedInstanceTo(view);
            if (o != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("calling doLayout for view " + view + " for layout " + this.getViewLayout());
                }
                this.getViewLayout().doLayout(this, view, o.getClass());
                DockingWindowUtilities.removeEmptyContainers(this);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end of placeView(" + view + ", " + placement + ")");
        }
    }

    public Component add(Component component) {
        if (component instanceof DockingWindow) {
            System.out.println("setting main Window : " + component.getClass());
            this.setWindow((DockingWindow) component);
        } else super.add(component);
        return component;
    }

    /**
     * This method gets called when a bound property is changed.
     * 
     * @param evt A PropertyChangeEvent object describing the event source 
     *   	and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof AccessibleView) {
            if (evt.getNewValue() instanceof Boolean) {
                if (!((Boolean) evt.getNewValue()).booleanValue()) {
                    if (this.map != null) {
                        this.removeView((View) evt.getSource());
                    }
                }
            }
        }
    }

    /** action that allow to go to the next tab of a DockingTabbedPane
     *	if the current view is part of one
     */
    private static class DockingTabbedPaneNavigationAction extends AbstractAction {

        /** 0 --> move next
	 *  1 --> move previous
	 */
        private int direction = 0;

        /** create a new DockingTabbedPaneNavigationAction
	 *  @param direction 0 or 1
	 */
        public DockingTabbedPaneNavigationAction(int direction) {
            this.direction = direction;
        }

        public void actionPerformed(ActionEvent e) {
            Component c = FocusManager.getCurrentManager().getFocusOwner();
            while (c != null) {
                if (c instanceof org.siberia.ui.component.docking.DockingTabbedPane) {
                    org.siberia.ui.component.docking.DockingTabbedPane tabbedDock = (org.siberia.ui.component.docking.DockingTabbedPane) c;
                    int index = tabbedDock.getChildWindowIndex(tabbedDock.getSelectedWindow());
                    if (index >= 0) {
                        if (direction == 0) {
                            if (index == tabbedDock.getChildWindowCount() - 1) {
                                tabbedDock.setSelectedTab(0);
                            } else {
                                tabbedDock.setSelectedTab(index + 1);
                            }
                        } else {
                            if (index == 0) {
                                tabbedDock.setSelectedTab(tabbedDock.getChildWindowCount() - 1);
                            } else {
                                tabbedDock.setSelectedTab(index - 1);
                            }
                        }
                    }
                    break;
                }
                c = c.getParent();
            }
        }
    }
}
