package net.benojt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.benojt.FractalWindow.MenuAdapter;
import net.benojt.coloring.*;
import net.benojt.display.*;
import net.benojt.dlgs.BenojtDlg;
import net.benojt.dlgs.FileChooser;
import net.benojt.iterator.*;
import net.benojt.renderer.*;
import net.benojt.tools.*;
import net.benojt.ui.IntegerSpinner;
import net.benojt.xml.XMLNode;
import net.benojt.xml.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * a jpanel in which a fractal image is shown. can be added to a frame or anywhere else
 * manages all modules of a fractal i.e. iterator, renderer, display and colorings
 * @author felfe
 *
 */
public class FractalPanel extends JPanel {

    static final String XMLNodeName = "panel";

    static final String XMLNodeWidth = "width";

    static final String XMLNodeHeight = "height";

    public static final String defaultsDir = "data/defaults/";

    private Iterator iterator;

    private Coloring coloring;

    private Renderer renderer;

    private Display display;

    private IteratorManager iteratorManager;

    private MenuManager menuManager;

    /** the window or applet this fractal panel is contained in */
    private FractalFrame thisFrame;

    /** the thread that periodically repaints this panel */
    private RepaintThread repaintThread;

    /** if true the fractal must be re-rendered */
    private boolean markedForRender = false;

    /** if true any render attempt is blocked */
    private boolean blockRender = true;

    /** set to this so anonymous classes can access this */
    private FractalPanel fp;

    private JScrollPane jsp1;

    private JPanel contPan;

    private JPopupMenu popupMenu;

    private MouseEvent popupEvent;

    private BenojtDlg configDlg;

    private ViewStackElem currentView;

    /** default config of the current iterator is read from iterator when initialized */
    private XMLUtils.XMLPanelData defaultIteratorConfig;

    /**
	 * basic constructor.
	 * the iterator still needs to be set
	 * @param ff the frame this panel belongs to
	 */
    public FractalPanel(FractalFrame ff) {
        super();
        this.fp = this;
        this.thisFrame = ff;
        this.setLayout(new BorderLayout());
        this.iteratorManager = new IteratorManager(this);
        this.menuManager = new MenuManager(this);
        this.jsp1 = new JScrollPane();
        this.jsp1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.jsp1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contPan = new JPanel();
        contPan.setLayout(new java.awt.GridBagLayout());
        this.jsp1.getViewport().add(contPan);
        AdjustmentListener al = new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent e) {
                fp.getDisplay().getDisplayComponent().repaint();
            }
        };
        this.jsp1.getHorizontalScrollBar().addAdjustmentListener(al);
        this.jsp1.getVerticalScrollBar().addAdjustmentListener(al);
    }

    /**
	 * unblocks rendering and renders the fractal
	 */
    public void unBlock() {
        this.blockRender = false;
        this.renderImage();
    }

    public boolean handleKeyEvent(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_CONTEXT_MENU:
                this.popupMenu(null);
                e.consume();
                break;
            case MenuManager.MANAGER_KEY:
                this.getIteratorManager().showIteratorManagerDlg();
                e.consume();
                break;
            case KeyEvent.VK_BACK_SPACE:
                if (getCurrentView() != null) {
                    if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                        View nextView = getNextView();
                        if (nextView != null) {
                            if (nextView.getMaxIter() > 0) this.getIterator().setMaxIter(nextView.getMaxIter());
                            this.markForRedraw();
                        }
                    } else {
                        View previousView = getPreviousView();
                        if (previousView != null) {
                            if (previousView.getMaxIter() > 0) this.getIterator().setMaxIter(previousView.getMaxIter());
                            this.markForRedraw();
                        }
                    }
                }
                e.consume();
                break;
        }
        if (!e.isConsumed() && this.thisFrame instanceof FractalWindow) {
            if (!e.isConsumed()) Benojt.handleKeyEvent(e);
            if (!e.isConsumed()) ((FractalWindow) thisFrame).handleKeyEvent(e);
        }
        if (e.isConsumed()) {
            this.renderImage();
            return e.isConsumed();
        }
        if (!e.isConsumed() && (this.getDisplay() instanceof UIModule)) {
            ((UIModule) this.getDisplay()).handleKeyEvent(e);
            if (e.isConsumed()) ((UIModule) this.getDisplay()).rerender();
        }
        if (!e.isConsumed() && (this.getRenderer() instanceof UIModule)) {
            ((UIModule) this.getRenderer()).handleKeyEvent(e);
            if (e.isConsumed()) ((UIModule) this.renderer).rerender();
        }
        if (!e.isConsumed() && (this.getIterator() instanceof UIModule)) {
            ((UIModule) this.iterator).handleKeyEvent(e);
            if (e.isConsumed()) ((UIModule) this.getIterator()).rerender();
        }
        if (!e.isConsumed() && (this.getColoring() instanceof UIModule)) {
            ((UIModule) this.coloring).handleKeyEvent(e);
            if (e.isConsumed()) ((UIModule) this.getColoring()).rerender();
        }
        return e.isConsumed();
    }

    /**
	 * handle mouse events by forwarding them to the renderer and iterator.
	 * @param e mouse event
	 * @return true if event was handled
	 */
    public boolean handleMouseEvent(MouseEvent e) {
        if ((e.getID() == MouseEvent.MOUSE_RELEASED) && (e.getButton() == MouseEvent.BUTTON3)) {
            this.popupMenu(e);
            e.consume();
        }
        if (e.isConsumed()) this.renderImage();
        if (!e.isConsumed() && (this.getDisplay() instanceof UIModule)) {
            ((UIModule) this.getDisplay()).handleMouseEvent(e);
            if (e.isConsumed()) ((UIModule) this.getDisplay()).rerender();
        }
        if (!e.isConsumed() && (this.getRenderer() instanceof UIModule)) {
            ((UIModule) this.getRenderer()).handleMouseEvent(e);
            if (e.isConsumed()) ((UIModule) this.getRenderer()).rerender();
        }
        if (!e.isConsumed() && (this.iterator instanceof UIModule)) {
            ((UIModule) this.iterator).handleMouseEvent(e);
            if (e.isConsumed()) ((UIModule) this.getIterator()).rerender();
        }
        if (!e.isConsumed() && (this.coloring instanceof UIModule)) {
            ((UIModule) this.coloring).handleMouseEvent(e);
            if (e.isConsumed()) ((UIModule) this.getColoring()).rerender();
        }
        return e.isConsumed();
    }

    public XMLNode toXML(int optionFlags) {
        XMLNode cont = new XMLNode(XMLNodeName);
        cont.addProperty(XMLNodeWidth, this.getDisplay().getDisplayComponent().getWidth());
        cont.addProperty(XMLNodeHeight, this.getDisplay().getDisplayComponent().getHeight());
        if (this.repaintThread != null) cont.addProperty("refreshInterval", this.repaintThread.getInterval());
        if ((optionFlags & FileChooser.OPTION_ITERATOR) != 0) {
            Iterator it = getIterator();
            if (it instanceof UIModule) {
                XMLNode itNode = ((UIModule) it).toXML();
                cont.addNode(itNode);
            }
        }
        if ((optionFlags & FileChooser.OPTION_RENDERER) != 0) {
            Renderer re = getRenderer();
            if (re instanceof UIModule) cont.addNode(((UIModule) re).toXML());
        }
        if ((optionFlags & FileChooser.OPTION_COLORING) != 0) {
            Coloring co = getColoring();
            if (co instanceof UIModule) cont.addNode(((UIModule) co).toXML());
        }
        if ((optionFlags & FileChooser.OPTION_DISPLAY) != 0) {
            Display di = getDisplay();
            if (di instanceof UIModule) cont.addNode(((UIModule) di).toXML());
        }
        return cont;
    }

    /**
	 * load module config from nodelist.
	 * @param nodes the xml nodes to load config from
	 * @param optionFlags flags for module types to load config for
	 * @param reDraw TODO
	 * @return some error string
	 */
    public String loadConfig(NodeList nodes, int optionFlags, boolean reDraw) {
        StringBuffer errors = new StringBuffer();
        this.blockRender = true;
        XMLUtils.XMLPanelData panelData = XMLUtils.getPanelDataFromXML(nodes, errors);
        if ((optionFlags & FileChooser.OPTION_ITERATOR) != 0 && panelData.interatorName != null && panelData.iteratorNodes != null) {
            String iteratorErrors = getIteratorManager().loadConfig(panelData.interatorName, panelData.templateNode != null, panelData.iteratorNodes);
            errors.append(iteratorErrors);
        }
        if ((optionFlags & FileChooser.OPTION_COLORING) != 0 && panelData.coloringClass != null && panelData.coloringNodes != null) {
            String coloringErrors = this.setColoring(panelData.coloringClass, panelData.coloringNodes);
            errors.append(coloringErrors);
            if (reDraw) this.markForRedraw();
        }
        if ((optionFlags & FileChooser.OPTION_RENDERER) != 0 && panelData.rendererClass != null && panelData.rendererNodes != null) {
            String rendererErrors = this.setRenderer(panelData.rendererClass, panelData.rendererNodes);
            errors.append(rendererErrors);
            if (reDraw) this.markForRedraw();
        }
        if ((optionFlags & FileChooser.OPTION_DISPLAY) != 0 && panelData.displayClass != null && panelData.displayNodes != null) {
            String displayErrors = this.setDisplay(panelData.displayClass, panelData.displayNodes);
            errors.append(displayErrors);
            if (reDraw) this.markForRedraw();
        }
        try {
            String widthString = panelData.properties.get(XMLNodeWidth);
            Integer newWidth = null;
            if (widthString != null) newWidth = new Integer(widthString);
            String heightString = panelData.properties.get(XMLNodeHeight);
            Integer newHeight = null;
            if (heightString != null) newHeight = new Integer(heightString);
            if (newHeight != null && newWidth != null && newWidth > 0 && newHeight > 0 && thisFrame instanceof FractalWindow) {
                FractalWindow fw = (FractalWindow) thisFrame;
                fw.fixSizeCB.setSelected(true);
                this.getDisplay().setDimension(new Dimension(newWidth, newHeight));
                this.toggleFixSize();
            } else {
            }
        } catch (Exception ex) {
            errors.append("could not load panel size\n");
        }
        this.blockRender = false;
        this.renderImage();
        return errors.toString();
    }

    /**
	 * load config from file or url.
	 * @param source config source
	 * @param optionFlags flags for module types to load config for
	 * @param reDraw TODO
	 * @return error string
	 */
    public String loadConfig(Object source, Integer optionFlags, boolean reDraw) {
        String errors = "";
        Document doc = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            if (source instanceof URL) {
                URL url = (URL) source;
                doc = docBuilder.parse(url.openStream());
            } else if (source instanceof File) {
                doc = docBuilder.parse((File) source);
            } else if (source instanceof String) {
                doc = docBuilder.parse(new InputSource(new StringReader((String) source)));
            } else errors += "Can not load from source " + source.getClass().getSimpleName() + ".\n";
        } catch (java.io.IOException ioex) {
            errors += "Could not load file.\n";
        } catch (org.xml.sax.SAXException sex) {
            errors += "Could not parse file.\n";
        } catch (Exception ex) {
            System.out.println(ex);
            errors += "Unknown error.\n";
        }
        if (doc != null) {
            NodeList xmlChildren = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < xmlChildren.getLength(); ++i) {
                Node node = xmlChildren.item(i);
                if (node.getNodeName().equals(XMLNodeName)) errors += this.loadConfig(node.getChildNodes(), optionFlags == null ? FileChooser.OPTION_ALL_MODULES : optionFlags, reDraw);
            }
        }
        return errors;
    }

    /**
	 * starts rendering the fractal image if
	 * window is initialized, and is marked for render.
	 */
    public synchronized void renderImage() {
        if (!this.markedForRender || this.blockRender) return;
        if (this.getRenderer() == null) {
            System.out.println("renderer not initialized");
            return;
        }
        if (this.getIterator() == null) {
            System.out.println("iterator not initialized");
            return;
        }
        if (this.getColoring() == null) {
            System.out.println("coloring not initialized");
            return;
        }
        if (this.getDisplay() == null) {
            System.out.println("display not initialized");
            return;
        }
        if (this.coloring instanceof UIModule) ((UIModule) this.coloring).initialize(this);
        if (this.display instanceof UIModule) ((UIModule) this.display).initialize(this);
        if (this.iterator instanceof UIModule) ((UIModule) this.iterator).initialize(this);
        if (this.renderer instanceof UIModule) ((UIModule) this.renderer).initialize(this);
        this.markedForRender = false;
        this.getRenderer().startRendering(true);
    }

    /**
	 * forces start rendering.
	 *
	 */
    private void forceRenderImage() {
        this.markedForRender = true;
        renderImage();
    }

    public void cleanup() {
        if (this.iterator instanceof AbstractUIModule) ((AbstractUIModule) this.iterator).removeFractalPanel(this);
        if (this.display instanceof AbstractUIModule) ((AbstractUIModule) this.display).removeFractalPanel(this);
        if (this.renderer instanceof AbstractUIModule) ((AbstractUIModule) this.renderer).removeFractalPanel(this);
        if (this.coloring instanceof AbstractUIModule) ((AbstractUIModule) this.coloring).removeFractalPanel(this);
    }

    /**
	 * pops up the right click menu of this fractal panel.
	 * @param me
	 */
    private void popupMenu(final MouseEvent me) {
        if (this.popupMenu == null) {
            this.popupMenu = new JPopupMenu();
            final FractalPanel _this = this;
            final JMenu fileMenu = new JMenu("File");
            final JMenu iteratorMenu = new JMenu("Iterator");
            final JMenu rendererMenu = new JMenu("Renderer");
            final JMenu coloringMenu = new JMenu("Coloring");
            final JMenu displayMenu = new JMenu("Display");
            final JMenu helpMenu = new JMenu("Help");
            MenuAdapter ma = new MenuAdapter() {

                @Override
                public void menuSelected(javax.swing.event.MenuEvent e) {
                    if (e.getSource() == fileMenu) _this.getMenuManager().fillFileMenuItems(fileMenu, null);
                    if (e.getSource() == iteratorMenu) _this.getMenuManager().fillIteratorMenuItems(iteratorMenu, null);
                    if (e.getSource() == rendererMenu) _this.getMenuManager().fillRendererMenuItems(rendererMenu, popupEvent);
                    if (e.getSource() == coloringMenu) _this.getMenuManager().fillColoringMenuItems(coloringMenu, null);
                    if (e.getSource() == displayMenu) _this.getMenuManager().fillDisplayMenuItems(displayMenu, popupEvent);
                    if (e.getSource() == helpMenu) _this.getMenuManager().fillHelpMenuItems(helpMenu, null);
                }
            };
            fileMenu.addMenuListener(ma);
            iteratorMenu.addMenuListener(ma);
            rendererMenu.addMenuListener(ma);
            coloringMenu.addMenuListener(ma);
            displayMenu.addMenuListener(ma);
            helpMenu.addMenuListener(ma);
            if (thisFrame.getClass() == FractalWindow.class) this.popupMenu.add(fileMenu);
            this.popupMenu.add(iteratorMenu);
            this.popupMenu.add(rendererMenu);
            this.popupMenu.add(coloringMenu);
            this.popupMenu.add(displayMenu);
            if (thisFrame instanceof FractalWindow) this.popupMenu.add(helpMenu);
        }
        if (this.popupMenu.isShowing()) return;
        Component disp = this.getDisplay().getDisplayComponent();
        Point pos = me == null ? new Point(disp.getWidth() / 2, disp.getHeight() / 2) : new Point(me.getX(), me.getY());
        this.popupEvent = me;
        popupMenu.show(this.getDisplay().getDisplayComponent(), pos.x, pos.y);
    }

    /**
	 * set the iterator to a given Iterator instance. 
	 * read the default display. 
	 * @param it the new iterator
	 * @param rerender if true rendering is forced
	 */
    public void setIterator(Iterator it, boolean rerender) {
        if (this.getRenderer() != null) this.getRenderer().stopRendering(true);
        Point coords = null;
        if (this.iterator instanceof AbstractUIModule) coords = ((AbstractUIModule) this.iterator).getConfigDlgCoordinates();
        if (this.iterator instanceof UIModule) ((UIModule) this.iterator).cleanup();
        if (this.iterator instanceof AbstractUIModule) ((AbstractUIModule) this.iterator).removeFractalPanel(this);
        this.iterator = it;
        this.setDefaultIteratorConfig(it);
        if (this.iterator instanceof AbstractUIModule) ((AbstractUIModule) this.iterator).addFractalPanel(this);
        Frame frame = JOptionPane.getFrameForComponent(this.getDisplay().getDisplayComponent());
        if (frame instanceof JFrame) frame.setTitle(this.iterator.getClass().getSimpleName());
        if (coords != null && this.iterator instanceof AbstractUIModule) {
            ((AbstractUIModule) this.iterator).showConfigDlg(coords);
        }
        if (rerender) this.forceRenderImage();
    }

    private void setDefaultIteratorConfig(Iterator it) {
        this.defaultIteratorConfig = null;
        String defaultConfigString = it.getDefaultConfig();
        if (defaultConfigString != null) {
            Document doc = null;
            try {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                doc = docBuilder.parse(new InputSource(new StringReader(defaultConfigString)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (doc != null) {
                NodeList xmlChildren = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < xmlChildren.getLength(); ++i) {
                    Node node = xmlChildren.item(i);
                    if (node.getNodeName().equals(FractalPanel.XMLNodeName)) defaultIteratorConfig = XMLUtils.getPanelDataFromXML(node.getChildNodes(), null);
                }
            }
        }
    }

    /**
	 * creates an iterator from a given class and initializes the panel.
	 * @param cls the iterator class
	 * @param initDisplay initialize the panel from iterator preferences
	 */
    public void setIterator(Class<? extends Iterator> cls, boolean initDisplay) {
        Iterator oldIterator = this.iterator;
        try {
            Iterator it = null;
            try {
                it = cls.newInstance();
            } catch (InstantiationException iex) {
                java.lang.reflect.Constructor<? extends Iterator> constr = cls.getConstructor(new Class<?>[] { FractalPanel.class });
                it = constr.newInstance(new Object[] { this });
            }
            if (this.display == null) {
                this.setDisplay(SimpleDisplay.class, false, false);
            }
            View oldView = this.getCurrentView();
            this.setIterator(it, false);
            if (initDisplay) {
                this.currentView = null;
                this.setView(it.getBoundingBox());
                XMLUtils.XMLPanelData pd = this.getDefaultIteratorConfig();
                if (!(this.display instanceof ParameterSelector) && pd != null && pd.displayClass != null) {
                    if (!this.display.getClass().equals(pd.displayClass)) {
                        this.setDisplay(pd.displayClass, pd.displayNodes);
                    } else if (pd.displayNodes != null && this.display instanceof UIModule) {
                        ((UIModule) this.display).loadConfig(pd.displayNodes);
                    }
                }
                if (this.renderer == null || (pd != null && pd.rendererClass != null)) {
                    if (pd != null && pd.rendererClass != null) {
                        this.setRenderer(pd.rendererClass, pd.rendererNodes);
                    } else {
                        this.setRenderer(MultiPassRenderer.class, false);
                        System.out.println("no default renderer found");
                    }
                }
                if (this.coloring == null || (pd != null && pd.coloringClass != null)) {
                    if (pd != null && pd.coloringClass != null) {
                        this.setColoring(pd.coloringClass, pd.coloringNodes);
                    } else {
                        this.setColoring(SimpleGradient.class, false);
                        System.out.println("no default coloring found");
                    }
                }
            } else if (oldIterator != null) {
                this.iterator.setMaxIter(oldIterator.getMaxIter());
                this.iterator.setMaxValue(oldIterator.getMaxValue());
                if (this.iterator instanceof AbstractParameterIterator && oldIterator instanceof AbstractParameterIterator) {
                    ((AbstractParameterIterator) this.iterator).setBDC(((AbstractParameterIterator) oldIterator).getBDC(), false, false);
                }
                this.pushView(oldView);
            }
            this.forceRenderImage();
        } catch (Exception ex) {
            System.out.println("could not create iterator on " + cls.toString());
            ex.printStackTrace();
        }
    }

    /**
	 * set iterator and iterator config from defaults.
	 * @param cls the class of the new iterator
	 */
    public void setIteratorFromDefault(Class<? extends Iterator> cls) {
        this.setIterator(cls, true);
        if (this.defaultIteratorConfig != null && this.defaultIteratorConfig.iteratorNodes != null && (this.iterator instanceof UIModule)) {
            ((UIModule) this.iterator).loadConfig(this.defaultIteratorConfig.iteratorNodes);
            this.forceRenderImage();
        }
    }

    /**
	 * sets the renderer to be used in this window.
	 * @param cls the class of the new renderer
	 * @param rerender if true then force rendering
	 */
    public void setRenderer(Class<? extends Renderer> cls, boolean rerender) {
        try {
            if (this.getRenderer() != null) this.getRenderer().stopRendering(true);
            Renderer re = cls.newInstance();
            Point coords = null;
            if (this.renderer instanceof AbstractUIModule) coords = ((AbstractUIModule) this.renderer).getConfigDlgCoordinates();
            if (this.renderer instanceof UIModule) ((UIModule) this.renderer).cleanup();
            if (this.renderer instanceof AbstractUIModule) ((AbstractUIModule) this.renderer).removeFractalPanel(this);
            this.renderer = re;
            if (this.renderer instanceof AbstractUIModule) ((AbstractUIModule) this.renderer).addFractalPanel(this);
            if (rerender) this.forceRenderImage();
            if (coords != null && this.renderer instanceof AbstractUIModule) ((AbstractUIModule) this.renderer).showConfigDlg(coords);
            if (this.iterator instanceof AbstractUIModule) ((AbstractIterator) this.iterator).updateConfigDlg();
        } catch (Exception ex) {
            System.out.println("could not create renderer on " + cls.toString() + " ");
            ex.printStackTrace();
        }
    }

    /**
	 * set renderer for this panel from class name and XML-nodes with config
	 * @param className
	 * @param childNodes
	 * @return some error message
	 */
    public String setRenderer(Class<? extends Renderer> cls, NodeList childNodes) {
        String res = "";
        this.setRenderer(cls, false);
        Renderer re = this.getRenderer();
        if (childNodes != null && re instanceof UIModule) res += ((UIModule) re).loadConfig(childNodes);
        return res;
    }

    public void setColoring(Class<? extends Coloring> cls, boolean rerender) {
        try {
            Coloring col = cls.newInstance();
            boolean isBuffered = this.getDisplay() instanceof BufferedDisplay;
            this.setColoring(col, !isBuffered && rerender);
            if (isBuffered && (col instanceof UIModule) && !this.blockRender) {
                ((UIModule) col).initialize(this);
                ((BufferedDisplay) this.getDisplay()).reColor();
            }
        } catch (Exception ex) {
            System.out.println("could not create coloring on " + cls.toString());
            ex.printStackTrace();
        }
    }

    public void setColoring(Coloring col, boolean rerender) {
        if (this.getRenderer() != null) this.getRenderer().stopRendering(true);
        java.awt.Point coords = null;
        if (this.coloring instanceof AbstractUIModule) coords = ((AbstractUIModule) this.coloring).getConfigDlgCoordinates();
        if (this.coloring instanceof UIModule) ((UIModule) this.coloring).cleanup();
        if (this.coloring instanceof AbstractUIModule) ((AbstractUIModule) this.coloring).removeFractalPanel(this);
        this.coloring = col;
        if (this.coloring instanceof AbstractUIModule) ((AbstractUIModule) this.coloring).addFractalPanel(this);
        if (rerender) this.forceRenderImage();
        if (coords != null && this.coloring instanceof AbstractUIModule) ((AbstractUIModule) this.coloring).showConfigDlg(coords);
    }

    /**
	 * set coloring for this panel from class name and XML-nodes with config
	 * @param className
	 * @param childNodes
	 * @return some error message
	 */
    public String setColoring(Class<? extends Coloring> cls, NodeList childNodes) {
        String res = "";
        this.setColoring(cls, false);
        Coloring co = this.getColoring();
        if (childNodes != null && co instanceof UIModule) res = ((UIModule) co).loadConfig(childNodes);
        return res;
    }

    /**
	 * set the display to some new instance of given display class
	 * @param cls
	 * @param rerender
	 * @param init set colorings to some (by this display) prefered coloring
	 */
    public void setDisplay(Class<? extends Display> cls, boolean rerender, boolean init) {
        try {
            Display disp = cls.newInstance();
            this.setDisplay(disp, rerender, init);
        } catch (Exception ex) {
            System.out.println("could not create display on " + cls.toString());
            ex.printStackTrace();
        }
    }

    /**
	 * set the display to some display instance
	 * @param newDisplay
	 * @param rerender
	 * @param init set colorings to some (by this display) prefered coloring
	 */
    public void setDisplay(Display newDisplay, boolean rerender, boolean init) {
        if (this.getRenderer() != null) this.getRenderer().stopRendering(true);
        Dimension dim = null;
        Point coords = null;
        if (this.display != null) {
            dim = this.display.getDimension();
        }
        if (this.display instanceof AbstractUIModule) {
            coords = ((AbstractUIModule) this.display).getConfigDlgCoordinates();
            ((AbstractUIModule) this.display).removeFractalPanel(this);
        }
        this.removeAll();
        this.contPan.removeAll();
        if (this.display instanceof UIModule) ((UIModule) this.display).cleanup();
        this.display = newDisplay;
        if (this.display instanceof AbstractUIModule) ((AbstractUIModule) this.display).addFractalPanel(this);
        if (dim != null) this.display.setDimension(dim); else this.display.setDimension(new Dimension(480, 320));
        if ((this.thisFrame instanceof FractalWindow) && ((FractalWindow) this.thisFrame).fixSizeCB.isSelected()) {
            contPan.add(this.getDisplay().getDisplayComponent(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            this.add(this.jsp1);
        } else this.add(newDisplay.getDisplayComponent());
        if (this.thisFrame != null) this.thisFrame.addWindowListeners();
        if (init) {
            java.util.Collection<Class<? extends Coloring>> prefCols = this.display.getPreferedColorings();
            if (prefCols != null && !prefCols.contains(this.coloring.getClass())) this.setColoring(prefCols.iterator().next(), rerender);
        }
        if (rerender) this.forceRenderImage();
        if (coords != null && this.display instanceof AbstractUIModule) ((AbstractUIModule) this.display).showConfigDlg(coords);
    }

    /**
	 * set display for this panel from class name and XML-nodes with config
	 * @param className
	 * @param childNodes
	 * @return some error message
	 */
    public String setDisplay(Class<? extends Display> cls, NodeList childNodes) {
        String res = "";
        this.setDisplay(cls, false, false);
        Display di = this.getDisplay();
        if (childNodes != null && di instanceof UIModule) res += ((UIModule) di).loadConfig(childNodes);
        return res;
    }

    public void markForRedraw() {
        this.markedForRender = true;
    }

    /**
	 * returns the iterator used in this window.
	 * @return the iterator
	 */
    public Iterator getIterator() {
        return this.iterator;
    }

    public Coloring getColoring() {
        return this.coloring;
    }

    public Display getDisplay() {
        return this.display;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public MenuManager getMenuManager() {
        return this.menuManager;
    }

    public IteratorManager getIteratorManager() {
        return this.iteratorManager;
    }

    /**
	 * returns the window this fractal panel is contained in.
	 * @return the fractal frame this panel belongs to
	 */
    public FractalFrame getFractalFrame() {
        return this.thisFrame;
    }

    public void toggleFixSize() {
        if (!(thisFrame instanceof FractalWindow)) return;
        FractalWindow fw = (FractalWindow) thisFrame;
        this.removeAll();
        this.contPan.removeAll();
        if (fw.fixSizeCB.isSelected()) {
            contPan.add(this.getDisplay().getDisplayComponent(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            this.add(this.jsp1);
            this.getDisplay().getDisplayComponent().setPreferredSize(this.getDisplay().getDisplayComponent().getSize());
            fw.setPreferredSize(null);
        } else {
            this.add(this.getDisplay().getDisplayComponent());
            fw.setPreferredSize(null);
            this.getDisplay().getDisplayComponent().setPreferredSize(display.getDisplayComponent().getSize());
        }
        fw.pack();
    }

    public Rectangle getViewRect() {
        if (!(thisFrame instanceof FractalWindow)) return null;
        FractalWindow fw = (FractalWindow) thisFrame;
        if (!fw.fixSizeCB.isSelected()) return null;
        return this.jsp1.getViewport().getViewRect();
    }

    public void startRefresh() {
        if (this.repaintThread == null) {
            this.repaintThread = new RepaintThread(this);
            this.repaintThread.start();
        }
        this.repaintThread.setUpdate(true);
    }

    public void stopRefresh() {
        if (this.repaintThread != null) this.repaintThread.setUpdate(false);
    }

    public boolean isRefreshing() {
        return this.repaintThread != null && this.repaintThread.isActive();
    }

    @Override
    public String getName() {
        return "Fractal";
    }

    public void updateConfigDlg() {
        if (this.configDlg != null && this.configDlg.isShowing()) this.configDlg.dataInit();
    }

    public void showConfigDlg() {
        if (this.configDlg == null) {
            FractalFrame ff = this.getFractalFrame();
            java.awt.Frame frame = null;
            if (ff instanceof Frame) frame = (Frame) ff;
            this.configDlg = new ConfigDlg(frame);
            this.configDlg.setLocationByPlatform(true);
            this.configDlg.setLocationRelativeTo(null);
        }
        this.configDlg.setVisible(true);
    }

    public XMLUtils.XMLPanelData getDefaultIteratorConfig() {
        return this.defaultIteratorConfig;
    }

    public void setView(View view) {
        if (view != null) {
            this.currentView = null;
            this.pushView(view);
        }
    }

    /**
	 * add a new view above the current view.
	 * @param view the new 
	 */
    public void pushView(View view) {
        if (view == null) {
            this.currentView = null;
        } else if (view instanceof BoundingBox) {
            if (this.display != null) {
                this.pushView(this.display.getNewView(view));
            } else throw new java.lang.ClassCastException("cannot push boundingbox in fractal panel");
        } else {
            ViewStackElem vse = new ViewStackElem(view);
            if (this.currentView != null) this.currentView.setNext(vse);
            vse.setPrevious(this.currentView);
            this.currentView = vse;
            this.markForRedraw();
        }
    }

    /**
	 * get the current view.
	 * @return the current elements in the view stack
	 */
    public View getCurrentView() {
        View res = null;
        if (this.currentView != null) res = this.currentView.getView(); else System.out.println("no current view");
        return res;
    }

    /**
	 * set and return the view before the current view.
	 * @return the preview view in the view stack
	 */
    public View getPreviousView() {
        View res = null;
        if (this.currentView != null && this.currentView.getPrevious() != null) {
            this.currentView = this.currentView.getPrevious();
            return this.getCurrentView();
        }
        return res;
    }

    /**
	 * set and return the view after the current view.
	 * @return the next view in the view stack
	 */
    public View getNextView() {
        View res = null;
        if (this.currentView != null && this.currentView.getNext() != null) {
            this.currentView = this.currentView.getNext();
            res = this.getCurrentView();
        }
        return res;
    }

    /**
	 * wrapper class for a view in view stack.
	 * @author felfe
	 *
	 */
    private class ViewStackElem {

        ViewStackElem next, previous;

        View view;

        public ViewStackElem(View view) {
            this.view = view;
        }

        View getView() {
            return this.view;
        }

        void setNext(ViewStackElem viewStackElem) {
            this.next = viewStackElem;
        }

        void setPrevious(ViewStackElem viewStackElem) {
            this.previous = viewStackElem;
        }

        ViewStackElem getNext() {
            return this.next;
        }

        ViewStackElem getPrevious() {
            return this.previous;
        }
    }

    public class ConfigDlg extends BenojtDlg {

        IntegerSpinner widthSP, heightSP;

        JFrame frame;

        public ConfigDlg(java.awt.Frame frame) {
            super(frame, "Page properties", false);
            this.uiInit();
            this.dataInit();
            this.pack();
        }

        @Override
        protected void uiInit() {
            super.uiInit();
            this.addApplyButton();
            this.widthSP = new IntegerSpinner("Width:");
            this.heightSP = new IntegerSpinner("Height:");
            this.addContent(this.widthSP);
            this.addContent(this.heightSP, NEW_LINE);
        }

        @Override
        public void dataInit() {
            super.dataInit();
            this.widthSP.setNumber(fp.getDisplay().getDisplayComponent().getWidth());
            this.heightSP.setNumber(fp.getDisplay().getDisplayComponent().getHeight());
        }

        @Override
        protected void applyBT_action(java.awt.event.ActionEvent e) {
            int w = this.widthSP.getNumber();
            int h = this.heightSP.getNumber();
            fp.getDisplay().getDisplayComponent().setSize(w, h);
            fp.getDisplay().getDisplayComponent().setPreferredSize(new Dimension(w, h));
            FractalWindow fw = (FractalWindow) thisFrame;
            if (fw != null) {
                if (fw.fixSizeCB.isSelected()) fw.setPreferredSize(fw.getSize()); else fw.setPreferredSize(null);
                fw.pack();
            }
        }
    }
}
