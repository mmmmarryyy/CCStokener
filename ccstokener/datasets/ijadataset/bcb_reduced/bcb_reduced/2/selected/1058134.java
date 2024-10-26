package gnu.java.awt.peer.x;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.PrintJob;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.im.InputMethodHighlight;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CanvasPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.LightweightPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.PanelPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.WindowPeer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import gnu.classpath.SystemProperties;
import gnu.java.awt.ClasspathToolkit;
import gnu.java.awt.EmbeddedWindow;
import gnu.java.awt.peer.ClasspathFontPeer;
import gnu.java.awt.peer.EmbeddedWindowPeer;
import gnu.java.awt.peer.swing.SwingCanvasPeer;
import gnu.java.awt.peer.swing.SwingLabelPeer;
import gnu.java.awt.peer.swing.SwingPanelPeer;

public class XToolkit extends ClasspathToolkit {

    /**
   * Set to true to enable debug output.
   */
    static boolean DEBUG = false;

    /**
   * Maps AWT colors to X colors.
   */
    HashMap colorMap = new HashMap();

    /**
   * The system event queue.
   */
    private EventQueue eventQueue;

    /**
   * The default color model of this toolkit.
   */
    private ColorModel colorModel;

    /**
   * Maps image URLs to Image instances.
   */
    private HashMap imageCache = new HashMap();

    /**
   * The cached fonts.
   */
    private WeakHashMap fontCache = new WeakHashMap();

    public XToolkit() {
        SystemProperties.setProperty("gnu.javax.swing.noGraphics2D", "true");
        SystemProperties.setProperty("java.awt.graphicsenv", "gnu.java.awt.peer.x.XGraphicsEnvironment");
    }

    public GraphicsEnvironment getLocalGraphicsEnvironment() {
        return new XGraphicsEnvironment();
    }

    /**
   * Returns the font peer for a font with the specified name and attributes.
   *
   * @param name the font name
   * @param attrs the font attributes
   *
   * @return the font peer for a font with the specified name and attributes
   */
    public ClasspathFontPeer getClasspathFontPeer(String name, Map attrs) {
        String canonical = XFontPeer.encodeFont(name, attrs);
        ClasspathFontPeer font;
        if (!fontCache.containsKey(canonical)) {
            String graphics2d = SystemProperties.getProperty("gnu.xawt.graphics2d");
            if (graphics2d != null && graphics2d.equals("gl")) font = new XFontPeer2(name, attrs); else font = new XFontPeer(name, attrs);
            fontCache.put(canonical, font);
        } else {
            font = (ClasspathFontPeer) fontCache.get(canonical);
        }
        return font;
    }

    public Font createFont(int format, InputStream stream) {
        return null;
    }

    public RobotPeer createRobot(GraphicsDevice screen) throws AWTException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public EmbeddedWindowPeer createEmbeddedWindow(EmbeddedWindow w) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected ButtonPeer createButton(Button target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected TextFieldPeer createTextField(TextField target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected LabelPeer createLabel(Label target) {
        return new SwingLabelPeer(target);
    }

    protected ListPeer createList(List target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected CheckboxPeer createCheckbox(Checkbox target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected ScrollbarPeer createScrollbar(Scrollbar target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected ScrollPanePeer createScrollPane(ScrollPane target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected TextAreaPeer createTextArea(TextArea target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected ChoicePeer createChoice(Choice target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected FramePeer createFrame(Frame target) {
        XFramePeer frame = new XFramePeer(target);
        return frame;
    }

    protected CanvasPeer createCanvas(Canvas target) {
        return new SwingCanvasPeer(target);
    }

    protected PanelPeer createPanel(Panel target) {
        return new SwingPanelPeer(target);
    }

    protected WindowPeer createWindow(Window target) {
        return new XWindowPeer(target);
    }

    protected DialogPeer createDialog(Dialog target) {
        return new XDialogPeer(target);
    }

    protected MenuBarPeer createMenuBar(MenuBar target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected MenuPeer createMenu(Menu target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected PopupMenuPeer createPopupMenu(PopupMenu target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected MenuItemPeer createMenuItem(MenuItem target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected FileDialogPeer createFileDialog(FileDialog target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected FontPeer getFontPeer(String name, int style) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public Dimension getScreenSize() {
        return new Dimension(1024, 768);
    }

    public int getScreenResolution() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
   * Returns the color model used by this toolkit.
   *
   * @return the color model used by this toolkit
   */
    public ColorModel getColorModel() {
        if (colorModel == null) colorModel = new DirectColorModel(24, 0xFF0000, 0xFF00, 0xFF);
        return colorModel;
    }

    public String[] getFontList() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public FontMetrics getFontMetrics(Font name) {
        ClasspathFontPeer peer = (ClasspathFontPeer) name.getPeer();
        return peer.getFontMetrics(name);
    }

    public void sync() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
   * Returns an image that has its pixel data loaded from a file with the
   * specified name. If that file doesn't exist, an empty or error image
   * is returned instead.
   *
   * @param name the filename of the file that contains the pixel data
   *
   * @return the image
   */
    public Image getImage(String name) {
        Image image;
        try {
            File file = new File(name);
            image = getImage(file.toURL());
        } catch (MalformedURLException ex) {
            image = null;
        }
        return image;
    }

    /**
   * Returns an image that has its pixel data loaded from the specified URL.
   * If the image cannot be loaded for some reason, an empty or error image
   * is returned instead.
   *
   * @param url the URL to the image data
   *
   * @return the image
   */
    public Image getImage(URL url) {
        Image image;
        if (imageCache.containsKey(url)) {
            image = (Image) imageCache.get(url);
        } else {
            image = createImage(url);
            imageCache.put(url, image);
        }
        return image;
    }

    /**
   * Returns an image that has its pixel data loaded from a file with the
   * specified name. If that file doesn't exist, an empty or error image
   * is returned instead.
   *
   * @param filename the filename of the file that contains the pixel data
   *
   * @return the image
   */
    public Image createImage(String filename) {
        Image im;
        try {
            File file = new File(filename);
            URL url = file.toURL();
            im = createImage(url);
        } catch (MalformedURLException ex) {
            im = createErrorImage();
        }
        return im;
    }

    /**
   * Returns an image that has its pixel data loaded from the specified URL.
   * If the image cannot be loaded for some reason, an empty or error image
   * is returned instead.
   *
   * @param url the URL to the image data
   *
   * @return the image
   */
    public Image createImage(URL url) {
        Image image;
        try {
            image = createImage(url.openStream());
        } catch (IOException ex) {
            image = createErrorImage();
        }
        return image;
    }

    /**
   * Creates an image that is returned when calls to createImage() yields an
   * error.
   * 
   * @return an image that is returned when calls to createImage() yields an
   *         error
   */
    private Image createErrorImage() {
        return new XImage(1, 1);
    }

    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return true;
    }

    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public Image createImage(ImageProducer producer) {
        ImageConverter conv = new ImageConverter();
        producer.startProduction(conv);
        Image image = conv.getXImage();
        return image;
    }

    public Image createImage(byte[] data, int offset, int len) {
        Image image;
        try {
            ByteArrayInputStream i = new ByteArrayInputStream(data, offset, len);
            image = createImage(i);
        } catch (IOException ex) {
            image = createErrorImage();
        }
        return image;
    }

    private Image createImage(InputStream i) throws IOException {
        Image image;
        BufferedImage buffered = ImageIO.read(i);
        if (buffered != null && buffered.getTransparency() == Transparency.OPAQUE) {
            ImageProducer source = buffered.getSource();
            image = createImage(source);
        } else if (buffered != null) {
            image = buffered;
        } else {
            image = createErrorImage();
        }
        return image;
    }

    public PrintJob getPrintJob(Frame frame, String title, Properties props) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public void beep() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public Clipboard getSystemClipboard() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
   * Returns the eventqueue used by the XLib peers.
   *
   * @return the eventqueue used by the XLib peers
   */
    protected EventQueue getSystemEventQueueImpl() {
        if (eventQueue == null) eventQueue = new EventQueue();
        return eventQueue;
    }

    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent e) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public Map mapInputMethodHighlight(InputMethodHighlight highlight) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
   * Helper method to quickly fetch the default device (X Display).
   *
   * @return the default XGraphicsDevice
   */
    static XGraphicsDevice getDefaultDevice() {
        XGraphicsEnvironment env = (XGraphicsEnvironment) XGraphicsEnvironment.getLocalGraphicsEnvironment();
        return (XGraphicsDevice) env.getDefaultScreenDevice();
    }

    protected LightweightPeer createComponent(Component c) {
        return new XLightweightPeer(c);
    }
}
