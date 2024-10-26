package UI;

import java.io.InputStream;
import java.net.URL;
import java.util.Vector;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.ImageLoaderEvent;
import org.eclipse.swt.graphics.ImageLoaderListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import Core.Tools.ToolIF;
import Core.plugins.ShapeIF;

/**
 * Another mode of interaction with images provided when the user switch the mode
 * of the application to image mode
 * @see ToolIF implemented interface
 * @see ShapeIF
 * @author Mostafa Eweda & Mohammed Abd El Salam
 * @version 1.0
 * @since JDK 1.6
 */
public class ImageMode implements Runnable {

    private Display display;

    private Shell shell;

    private Canvas imageCanvas;

    private Combo backgroundCombo;

    private Combo scaleXCombo;

    private Combo scaleYCombo;

    private Combo alphaCombo;

    private Button transparentCheck;

    private Button backgroundCheck;

    private Button previousButton;

    private Button nextButton;

    private Button animateButton;

    private Color whiteColor;

    private Color blackColor;

    private Color redColor;

    private Color greenColor;

    private Color blueColor;

    private Color canvasBackground;

    private Cursor crossCursor;

    private GC imageCanvasGC;

    private int ix = 0;

    private int iy = 0;

    private float xscale = 1;

    private float yscale = 1;

    private int alpha = 255;

    private boolean incremental = false;

    private boolean transparent = true;

    private boolean showMask = false;

    private boolean showBackground = false;

    private boolean animate = false;

    private Thread animateThread;

    private Thread incrementalThread;

    private String lastPath;

    private String currentName;

    private String fileName;

    private ImageLoader loader;

    private ImageData[] imageDataArray;

    private int imageDataIndex;

    private ImageData imageData;

    private Image image;

    private Vector<ImageLoaderEvent> incrementalEvents;

    private static final int ALPHA_CONSTANT = 1 << 0;

    private static final int ALPHA_X = 1 << 1;

    private static final int ALPHA_Y = 1 << 2;

    private static final int ALPHA_UP_TO_DOWN = 1 << 3;

    private static final int ALPHA_LEFT_TO_RIGHT = 1 << 4;

    private static final int ALPHA_LINES = 1 << 5;

    public static void main(String[] args) {
        new ImageMode().run();
    }

    public void run() {
        display = new Display();
        shell = new Shell(display);
        shell.setImage(new Image(display, ImageMode.class.getResourceAsStream("paint.png")));
        shell.setText("Image Mode");
        shell.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent event) {
                resizeShell(event);
            }
        });
        shell.addShellListener(new ShellAdapter() {

            public void shellClosed(ShellEvent e) {
                animate = false;
                if (animateThread != null) while (animateThread.isAlive()) if (!display.readAndDispatch()) display.sleep();
                e.doit = true;
            }
        });
        shell.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                if (image != null) image.dispose();
                whiteColor.dispose();
                blackColor.dispose();
                redColor.dispose();
                greenColor.dispose();
                blueColor.dispose();
                crossCursor.dispose();
            }
        });
        whiteColor = new Color(display, 255, 255, 255);
        blackColor = new Color(display, 0, 0, 0);
        redColor = new Color(display, 255, 0, 0);
        greenColor = new Color(display, 0, 255, 0);
        blueColor = new Color(display, 0, 0, 255);
        crossCursor = new Cursor(display, SWT.CURSOR_CROSS);
        createMenuBar();
        createWidgets();
        shell.pack();
        imageCanvasGC = new GC(imageCanvas);
        imageCanvas.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                imageCanvasGC.dispose();
            }
        });
        shell.open();
        while (!shell.isDisposed()) if (!display.readAndDispatch()) display.sleep();
        display.dispose();
    }

    private void createWidgets() {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.numColumns = 2;
        shell.setLayout(layout);
        Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.horizontalAlignment = GridData.FILL;
        separator.setLayoutData(gridData);
        Composite controls = new Composite(shell, SWT.NONE);
        RowLayout rowLayout = new RowLayout();
        rowLayout.marginTop = 0;
        rowLayout.marginBottom = 5;
        rowLayout.spacing = 8;
        controls.setLayout(rowLayout);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        controls.setLayoutData(gridData);
        Group group = new Group(controls, SWT.NONE);
        group.setLayout(new RowLayout());
        group.setText("Background");
        backgroundCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        backgroundCombo.setItems(new String[] { "None", "White", "Black", "Red", "Green", "Blue" });
        backgroundCombo.select(backgroundCombo.indexOf("White"));
        backgroundCombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                changeBackground();
            }
        });
        String[] values = { "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
        group = new Group(controls, SWT.NULL);
        group.setLayout(new RowLayout());
        group.setText("X_scale");
        scaleXCombo = new Combo(group, SWT.DROP_DOWN);
        for (int i = 0; i < values.length; i++) {
            scaleXCombo.add(values[i]);
        }
        scaleXCombo.select(scaleXCombo.indexOf("1"));
        scaleXCombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                scaleX();
            }
        });
        group = new Group(controls, SWT.NULL);
        group.setLayout(new RowLayout());
        group.setText("Y_scale");
        scaleYCombo = new Combo(group, SWT.DROP_DOWN);
        for (int i = 0; i < values.length; i++) {
            scaleYCombo.add(values[i]);
        }
        scaleYCombo.select(scaleYCombo.indexOf("1"));
        scaleYCombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                scaleY();
            }
        });
        group = new Group(controls, SWT.NULL);
        group.setLayout(new RowLayout());
        group.setText("Alpha_K");
        alphaCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (int i = 0; i <= 255; i += 5) {
            alphaCombo.add(String.valueOf(i));
        }
        alphaCombo.select(alphaCombo.indexOf("255"));
        alphaCombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                alpha();
            }
        });
        group = new Group(controls, SWT.NULL);
        group.setLayout(new RowLayout());
        group.setText("Display");
        transparentCheck = new Button(group, SWT.CHECK);
        transparentCheck.setText("Transparent");
        transparentCheck.setSelection(transparent);
        transparentCheck.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                transparent = ((Button) event.widget).getSelection();
                if (image != null) {
                    imageCanvas.redraw();
                }
            }
        });
        backgroundCheck = new Button(group, SWT.CHECK);
        backgroundCheck.setText("Background");
        backgroundCheck.setSelection(showBackground);
        backgroundCheck.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                showBackground = ((Button) event.widget).getSelection();
            }
        });
        group = new Group(controls, SWT.NULL);
        group.setLayout(new RowLayout());
        group.setText("Animation");
        previousButton = new Button(group, SWT.PUSH);
        previousButton.setText("Previous");
        previousButton.setEnabled(false);
        previousButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                previous();
            }
        });
        nextButton = new Button(group, SWT.PUSH);
        nextButton.setText("Next");
        nextButton.setEnabled(false);
        nextButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                next();
            }
        });
        animateButton = new Button(group, SWT.PUSH);
        animateButton.setText("Animate");
        animateButton.setEnabled(false);
        animateButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                animate();
            }
        });
        imageCanvas = new Canvas(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE);
        imageCanvas.setBackground(whiteColor);
        imageCanvas.setCursor(crossCursor);
        gridData = new GridData();
        gridData.verticalSpan = 15;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalSpan = 2;
        imageCanvas.setLayoutData(gridData);
        imageCanvas.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent event) {
                if (image != null) paintImage(event);
            }
        });
        ScrollBar horizontal = imageCanvas.getHorizontalBar();
        horizontal.setVisible(true);
        horizontal.setMinimum(0);
        horizontal.setEnabled(false);
        horizontal.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                scrollHorizontally((ScrollBar) event.widget);
            }
        });
        ScrollBar vertical = imageCanvas.getVerticalBar();
        vertical.setVisible(true);
        vertical.setMinimum(0);
        vertical.setEnabled(false);
        vertical.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                scrollVertically((ScrollBar) event.widget);
            }
        });
        separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    }

    private Menu createMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menuBar);
        createFileMenu(menuBar);
        createAlphaMenu(menuBar);
        return menuBar;
    }

    private void createFileMenu(Menu menuBar) {
        MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
        item.setText("File");
        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        item.setMenu(fileMenu);
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("OpenFile");
        item.setAccelerator(SWT.MOD1 + 'O');
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuOpenFile();
            }
        });
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("OpenURL");
        item.setAccelerator(SWT.MOD1 + 'U');
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuOpenURL();
            }
        });
        new MenuItem(fileMenu, SWT.SEPARATOR);
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Save");
        item.setAccelerator(SWT.MOD1 + 'S');
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuSave();
            }
        });
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Save_as");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuSaveAs();
            }
        });
        new MenuItem(fileMenu, SWT.SEPARATOR);
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Print");
        item.setAccelerator(SWT.MOD1 + 'P');
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuPrint();
            }
        });
        new MenuItem(fileMenu, SWT.SEPARATOR);
        item = new MenuItem(fileMenu, SWT.PUSH);
        item.setText("Exit");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                shell.close();
            }
        });
    }

    private void createAlphaMenu(Menu menuBar) {
        MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
        item.setText("Alpha");
        Menu alphaMenu = new Menu(shell, SWT.DROP_DOWN);
        item.setMenu(alphaMenu);
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("K");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_CONSTANT);
            }
        });
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("(K + x) % 256");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_X);
            }
        });
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("(K + y) % 256");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_Y);
            }
        });
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("up to down");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_UP_TO_DOWN);
            }
        });
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("left to right");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_LEFT_TO_RIGHT);
            }
        });
        item = new MenuItem(alphaMenu, SWT.PUSH);
        item.setText("Lines effect");
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                menuComposeAlpha(ALPHA_LINES);
            }
        });
    }

    private void menuComposeAlpha(int alpha_op) {
        if (image == null) return;
        animate = false;
        Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
        shell.setCursor(waitCursor);
        imageCanvas.setCursor(waitCursor);
        try {
            if (alpha_op == ALPHA_CONSTANT) {
                imageData.alpha = alpha;
            } else {
                int width = imageData.width;
                int height = imageData.height;
                byte[] alphaData;
                imageData.alpha = -1;
                switch(alpha_op) {
                    case ALPHA_X:
                        for (int y = 0; y < imageData.height; y++) {
                            for (int x = 0; x < imageData.width; x++) {
                                imageData.setAlpha(x, y, (x + y + alpha) % 256);
                            }
                        }
                        break;
                    case ALPHA_Y:
                        for (int y = 0; y < imageData.height; y++) {
                            for (int x = 0; x < imageData.width; x++) {
                                imageData.setAlpha(x, y, (y + alpha) % 256);
                            }
                        }
                        break;
                    case ALPHA_UP_TO_DOWN:
                        alphaData = new byte[height * width];
                        for (int y = 0; y < height; y++) {
                            byte[] alphaRow = new byte[width];
                            for (int x = 0; x < width; x++) {
                                alphaRow[x] = (byte) ((255 * y) / height);
                            }
                            System.arraycopy(alphaRow, 0, alphaData, y * width, width);
                        }
                        imageData.alphaData = alphaData;
                        break;
                    case ALPHA_LEFT_TO_RIGHT:
                        alphaData = new byte[height * width];
                        for (int y = 0; y < width; y++) {
                            for (int x = 0; x < height; x++) {
                                alphaData[x * width + y] = (byte) ((255 * y) / width);
                            }
                        }
                        imageData.alphaData = alphaData;
                        break;
                    case ALPHA_LINES:
                        alphaData = new byte[height * width];
                        for (int y = 0; y < width; y++) {
                            byte[] alphaHeight = new byte[height];
                            for (int x = 0; x < height; x++) {
                                alphaHeight[x] = (byte) ((255 * x) / width);
                            }
                            System.arraycopy(alphaHeight, 0, alphaData, y * height, height);
                        }
                        imageData.alphaData = alphaData;
                        break;
                    default:
                        break;
                }
            }
            displayImage(imageData);
        } finally {
            shell.setCursor(null);
            imageCanvas.setCursor(crossCursor);
            waitCursor.dispose();
        }
    }

    private void menuOpenFile() {
        animate = false;
        resetScaleCombos();
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null) fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.bmp; *.gif; *.ico; *.jpg; *.pcx; *.png; *.tif", "*.bmp", "*.gif", "*.ico", "*.jpg", "*.pcx", "*.png", "*.tif" });
        fileChooser.setFilterNames(new String[] { "All_images" + " (bmp, gif, ico, jpg, pcx, png, tif)", "BMP (*.bmp)", "GIF (*.gif)", "ICO (*.ico)", "JPEG (*.jpg)", "PCX (*.pcx)", "PNG (*.png)", "TIFF (*.tif)" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null) return;
        Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
        shell.setCursor(waitCursor);
        imageCanvas.setCursor(waitCursor);
        try {
            loader = new ImageLoader();
            if (incremental) {
                loader.addImageLoaderListener(new ImageLoaderListener() {

                    public void imageDataLoaded(ImageLoaderEvent event) {
                        incrementalDataLoaded(event);
                    }
                });
                incrementalThreadStart();
            }
            imageDataArray = loader.load(filename);
            if (imageDataArray.length > 0) {
                currentName = filename;
                fileName = filename;
                previousButton.setEnabled(imageDataArray.length > 1);
                nextButton.setEnabled(imageDataArray.length > 1);
                animateButton.setEnabled(imageDataArray.length > 1 && loader.logicalScreenWidth > 0 && loader.logicalScreenHeight > 0);
                imageDataIndex = 0;
                displayImage(imageDataArray[imageDataIndex]);
                resetScrollBars();
            }
        } catch (SWTException e) {
            showErrorDialog("Loading_lc", filename, e);
        } catch (SWTError e) {
            showErrorDialog("Loading_lc", filename, e);
        } finally {
            shell.setCursor(null);
            imageCanvas.setCursor(crossCursor);
            waitCursor.dispose();
        }
    }

    private void menuOpenURL() {
        animate = false;
        resetScaleCombos();
        InputDialog dialog = new InputDialog(shell, "Open URL Dialog", "Enter URL of the image", "http://", new IInputValidator() {

            @Override
            public String isValid(String newText) {
                if (newText.startsWith("http://") || newText.startsWith("https://") || newText.startsWith("ftp://") || newText.startsWith("file://")) return newText;
                return null;
            }
        });
        if (dialog.open() == SWT.CANCEL) return;
        String urlName = dialog.getValue();
        Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
        shell.setCursor(waitCursor);
        imageCanvas.setCursor(waitCursor);
        try {
            URL url = new URL(urlName);
            InputStream stream = url.openStream();
            loader = new ImageLoader();
            if (incremental) {
                loader.addImageLoaderListener(new ImageLoaderListener() {

                    public void imageDataLoaded(ImageLoaderEvent event) {
                        incrementalDataLoaded(event);
                    }
                });
                incrementalThreadStart();
            }
            imageDataArray = loader.load(stream);
            stream.close();
            if (imageDataArray.length > 0) {
                currentName = urlName;
                fileName = null;
                previousButton.setEnabled(imageDataArray.length > 1);
                nextButton.setEnabled(imageDataArray.length > 1);
                animateButton.setEnabled(imageDataArray.length > 1 && loader.logicalScreenWidth > 0 && loader.logicalScreenHeight > 0);
                imageDataIndex = 0;
                displayImage(imageDataArray[imageDataIndex]);
                resetScrollBars();
            }
        } catch (Exception e) {
            showErrorDialog("Loading", urlName, e);
        } finally {
            shell.setCursor(null);
            imageCanvas.setCursor(crossCursor);
            waitCursor.dispose();
        }
    }

    /**
	 * Called to start a thread that draws incremental images as they are
	 * loaded.
	 */
    private void incrementalThreadStart() {
        incrementalEvents = new Vector<ImageLoaderEvent>();
        incrementalThread = new Thread("Incremental") {

            public void run() {
                while (incrementalEvents != null) {
                    synchronized (ImageMode.this) {
                        if (incrementalEvents != null) {
                            if (incrementalEvents.size() > 0) {
                                ImageLoaderEvent event = (ImageLoaderEvent) incrementalEvents.remove(0);
                                if (image != null) image.dispose();
                                image = new Image(display, event.imageData);
                                imageData = event.imageData;
                                imageCanvasGC.drawImage(image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);
                            } else {
                                yield();
                            }
                        }
                    }
                }
                display.wake();
            }
        };
        incrementalThread.setDaemon(true);
        incrementalThread.start();
    }

    /**
	 * Called when incremental image data has been loaded, for example, for
	 * interlaced GIF/PNG or progressive JPEG.
	 */
    private void incrementalDataLoaded(ImageLoaderEvent event) {
        synchronized (this) {
            incrementalEvents.addElement(event);
        }
    }

    private void menuSave() {
        if (image == null) return;
        animate = false;
        if (imageData.type == SWT.IMAGE_UNDEFINED || fileName == null) {
            menuSaveAs();
            return;
        }
        Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
        shell.setCursor(waitCursor);
        imageCanvas.setCursor(waitCursor);
        try {
            loader.data = new ImageData[] { imageData };
            loader.save(fileName, imageData.type);
        } catch (SWTException e) {
            showErrorDialog("Saving_lc", fileName, e);
        } catch (SWTError e) {
            showErrorDialog("Saving_lc", fileName, e);
        } finally {
            shell.setCursor(null);
            imageCanvas.setCursor(crossCursor);
            waitCursor.dispose();
        }
    }

    private void menuSaveAs() {
        if (image == null) return;
        animate = false;
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setFilterPath(lastPath);
        if (fileName != null) {
            String name = fileName;
            int nameStart = name.lastIndexOf(java.io.File.separatorChar);
            if (nameStart > -1) {
                name = name.substring(nameStart + 1);
            }
            fileChooser.setFileName(name);
        }
        fileChooser.setFilterExtensions(new String[] { "*.bmp", "*.gif", "*.ico", "*.jpg", "*.png" });
        fileChooser.setFilterNames(new String[] { "BMP (*.bmp)", "GIF (*.gif)", "ICO (*.ico)", "JPEG (*.jpg)", "PNG (*.png)" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null) return;
        int filetype = determineFileType(filename);
        if (filetype == SWT.IMAGE_UNDEFINED) {
            MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
            box.setMessage("Unknown_extension");
            box.open();
            return;
        }
        if (new java.io.File(filename).exists()) {
            MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
            box.setMessage("Overwrite Error");
            if (box.open() == SWT.CANCEL) return;
        }
        Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
        shell.setCursor(waitCursor);
        imageCanvas.setCursor(waitCursor);
        try {
            loader.data = new ImageData[] { imageData };
            loader.save(filename, filetype);
            fileName = filename;
        } catch (SWTException e) {
            showErrorDialog("Saving_lc", filename, e);
        } catch (SWTError e) {
            showErrorDialog("Saving_lc", filename, e);
        } finally {
            shell.setCursor(null);
            imageCanvas.setCursor(crossCursor);
            waitCursor.dispose();
        }
    }

    private void menuPrint() {
        if (image == null) return;
        try {
            PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
            PrinterData printerData = dialog.open();
            if (printerData == null) return;
            Printer printer = new Printer(printerData);
            Point screenDPI = display.getDPI();
            Point printerDPI = printer.getDPI();
            int scaleFactor = printerDPI.x / screenDPI.x;
            Rectangle trim = printer.computeTrim(0, 0, 0, 0);
            if (printer.startJob(currentName)) {
                if (printer.startPage()) {
                    GC gc = new GC(printer);
                    int transparentPixel = imageData.transparentPixel;
                    if (transparentPixel != -1 && !transparent) {
                        imageData.transparentPixel = -1;
                    }
                    Image printerImage = new Image(printer, imageData);
                    gc.drawImage(printerImage, 0, 0, imageData.width, imageData.height, -trim.x, -trim.y, scaleFactor * imageData.width, scaleFactor * imageData.height);
                    if (transparentPixel != -1 && !transparent) {
                        imageData.transparentPixel = transparentPixel;
                    }
                    printerImage.dispose();
                    gc.dispose();
                    printer.endPage();
                }
                printer.endJob();
            }
            printer.dispose();
        } catch (SWTError e) {
            MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
            box.setMessage("Printing_error" + e.getMessage());
            box.open();
        }
    }

    private void changeBackground() {
        String background = backgroundCombo.getText();
        if (background.equals("White")) {
            imageCanvas.setBackground(whiteColor);
        } else if (background.equals("Black")) {
            imageCanvas.setBackground(blackColor);
        } else if (background.equals("Red")) {
            imageCanvas.setBackground(redColor);
        } else if (background.equals("Green")) {
            imageCanvas.setBackground(greenColor);
        } else if (background.equals("Blue")) {
            imageCanvas.setBackground(blueColor);
        } else {
            imageCanvas.setBackground(null);
        }
    }

    /**
	 * Called when the ScaleX combo selection changes.
	 */
    private void scaleX() {
        try {
            xscale = Float.parseFloat(scaleXCombo.getText());
        } catch (NumberFormatException e) {
            xscale = 1;
            scaleXCombo.select(scaleXCombo.indexOf("1"));
        }
        if (image != null) {
            resizeScrollBars();
            imageCanvas.redraw();
        }
    }

    private void scaleY() {
        try {
            yscale = Float.parseFloat(scaleYCombo.getText());
        } catch (NumberFormatException e) {
            yscale = 1;
            scaleYCombo.select(scaleYCombo.indexOf("1"));
        }
        if (image != null) {
            resizeScrollBars();
            imageCanvas.redraw();
        }
    }

    /**
	 * Called when the Alpha combo selection changes.
	 */
    private void alpha() {
        try {
            alpha = Integer.parseInt(alphaCombo.getText());
        } catch (NumberFormatException e) {
            alphaCombo.select(alphaCombo.indexOf("255"));
            alpha = 255;
        }
    }

    /**
	 * Called when the Animate button is pressed.
	 */
    private void animate() {
        animate = !animate;
        if (animate && image != null && imageDataArray.length > 1) {
            animateThread = new Thread("Animation") {

                public void run() {
                    preAnimation();
                    try {
                        animateLoop();
                    } catch (final SWTException e) {
                        display.syncExec(new Runnable() {

                            public void run() {
                                showErrorDialog("Error", currentName, e);
                            }
                        });
                    }
                    postAnimation();
                }
            };
            animateThread.setDaemon(true);
            animateThread.start();
        }
    }

    /**
	 * Loop through all of the images in a multi-image file and display them one
	 * after another.
	 */
    private void animateLoop() {
        Image offScreenImage = new Image(display, loader.logicalScreenWidth, loader.logicalScreenHeight);
        GC offScreenImageGC = new GC(offScreenImage);
        try {
            display.syncExec(new Runnable() {

                public void run() {
                    canvasBackground = imageCanvas.getBackground();
                }
            });
            offScreenImageGC.setBackground(canvasBackground);
            offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);
            offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);
            int repeatCount = loader.repeatCount;
            while (animate && (loader.repeatCount == 0 || repeatCount > 0)) {
                if (imageData.disposalMethod == SWT.DM_FILL_BACKGROUND) {
                    Color bgColor = null;
                    int backgroundPixel = loader.backgroundPixel;
                    if (showBackground && backgroundPixel != -1) {
                        RGB backgroundRGB = imageData.palette.getRGB(backgroundPixel);
                        bgColor = new Color(null, backgroundRGB);
                    }
                    try {
                        offScreenImageGC.setBackground(bgColor != null ? bgColor : canvasBackground);
                        offScreenImageGC.fillRectangle(imageData.x, imageData.y, imageData.width, imageData.height);
                    } finally {
                        if (bgColor != null) bgColor.dispose();
                    }
                } else if (imageData.disposalMethod == SWT.DM_FILL_PREVIOUS) {
                    offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);
                }
                imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
                imageData = imageDataArray[imageDataIndex];
                image.dispose();
                image = new Image(display, imageData);
                offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);
                imageCanvasGC.drawImage(offScreenImage, 0, 0);
                try {
                    Thread.sleep(visibleDelay(imageData.delayTime * 10));
                } catch (InterruptedException e) {
                }
                if (imageDataIndex == imageDataArray.length - 1) repeatCount--;
            }
        } finally {
            offScreenImage.dispose();
            offScreenImageGC.dispose();
        }
    }

    private void preAnimation() {
        display.syncExec(new Runnable() {

            public void run() {
                animateButton.setText("Stop");
                previousButton.setEnabled(false);
                nextButton.setEnabled(false);
                backgroundCombo.setEnabled(false);
                scaleXCombo.setEnabled(false);
                scaleYCombo.setEnabled(false);
                alphaCombo.setEnabled(false);
                transparentCheck.setEnabled(false);
                resetScaleCombos();
                resetScrollBars();
            }
        });
    }

    /**
	 * Post animation reset.
	 */
    private void postAnimation() {
        display.syncExec(new Runnable() {

            public void run() {
                previousButton.setEnabled(true);
                nextButton.setEnabled(true);
                backgroundCombo.setEnabled(true);
                scaleXCombo.setEnabled(true);
                scaleYCombo.setEnabled(true);
                alphaCombo.setEnabled(true);
                transparentCheck.setEnabled(true);
                animateButton.setText("Animate");
                if (animate) {
                    animate = false;
                } else {
                    displayImage(imageDataArray[imageDataIndex]);
                }
            }
        });
    }

    /**
	 * Called when the Previous button is pressed. Display the previous image in
	 * a multi-image file.
	 */
    private void previous() {
        if (image != null && imageDataArray.length > 1) {
            if (imageDataIndex == 0) {
                imageDataIndex = imageDataArray.length;
            }
            imageDataIndex = imageDataIndex - 1;
            displayImage(imageDataArray[imageDataIndex]);
        }
    }

    /**
	 * Called when the Next button is pressed. Display the next image in a
	 * multi-image file.
	 */
    private void next() {
        if (image != null && imageDataArray.length > 1) {
            imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
            displayImage(imageDataArray[imageDataIndex]);
        }
    }

    private void displayImage(ImageData newImageData) {
        if (incremental && incrementalThread != null) {
            synchronized (this) {
                incrementalEvents = null;
            }
            while (incrementalThread.isAlive()) {
                if (!display.readAndDispatch()) display.sleep();
            }
        }
        if (image != null) image.dispose();
        try {
            image = new Image(display, newImageData);
            imageData = newImageData;
        } catch (SWTException e) {
            showErrorDialog("Creating_from" + " ", currentName, e);
            image = null;
            return;
        }
        imageCanvas.redraw();
    }

    private void paintImage(PaintEvent event) {
        Image paintImage = image;
        int transparentPixel = imageData.transparentPixel;
        if (transparentPixel != -1 && !transparent) {
            imageData.transparentPixel = -1;
            paintImage = new Image(display, imageData);
        }
        int w = Math.round(imageData.width * xscale);
        int h = Math.round(imageData.height * yscale);
        event.gc.drawImage(paintImage, 0, 0, imageData.width, imageData.height, ix + imageData.x, iy + imageData.y, w, h);
        if (showMask && (imageData.getTransparencyType() != SWT.TRANSPARENCY_NONE)) {
            ImageData maskImageData = imageData.getTransparencyMask();
            Image maskImage = new Image(display, maskImageData);
            event.gc.drawImage(maskImage, 0, 0, imageData.width, imageData.height, w + 10 + ix + imageData.x, iy + imageData.y, w, h);
            maskImage.dispose();
        }
        if (transparentPixel != -1 && !transparent) {
            imageData.transparentPixel = transparentPixel;
            paintImage.dispose();
        }
    }

    private void resizeShell(ControlEvent event) {
        if (image == null || shell.isDisposed()) return;
        resizeScrollBars();
    }

    private void resetScaleCombos() {
        xscale = 1;
        yscale = 1;
        scaleXCombo.select(scaleXCombo.indexOf("1"));
        scaleYCombo.select(scaleYCombo.indexOf("1"));
    }

    private void resetScrollBars() {
        if (image == null) return;
        ix = 0;
        iy = 0;
        resizeScrollBars();
        imageCanvas.getHorizontalBar().setSelection(0);
        imageCanvas.getVerticalBar().setSelection(0);
    }

    private void resizeScrollBars() {
        ScrollBar horizontal = imageCanvas.getHorizontalBar();
        ScrollBar vertical = imageCanvas.getVerticalBar();
        Rectangle canvasBounds = imageCanvas.getClientArea();
        int width = Math.round(imageData.width * xscale);
        if (width > canvasBounds.width) {
            horizontal.setEnabled(true);
            horizontal.setMaximum(width);
            horizontal.setThumb(canvasBounds.width);
            horizontal.setPageIncrement(canvasBounds.width);
        } else {
            horizontal.setEnabled(false);
            if (ix != 0) {
                ix = 0;
                imageCanvas.redraw();
            }
        }
        int height = Math.round(imageData.height * yscale);
        if (height > canvasBounds.height) {
            vertical.setEnabled(true);
            vertical.setMaximum(height);
            vertical.setThumb(canvasBounds.height);
            vertical.setPageIncrement(canvasBounds.height);
        } else {
            vertical.setEnabled(false);
            if (iy != 0) {
                iy = 0;
                imageCanvas.redraw();
            }
        }
    }

    private void scrollHorizontally(ScrollBar scrollBar) {
        if (image == null) return;
        Rectangle canvasBounds = imageCanvas.getClientArea();
        int width = Math.round(imageData.width * xscale);
        int height = Math.round(imageData.height * yscale);
        if (width > canvasBounds.width) {
            int x = -scrollBar.getSelection();
            if (x + width < canvasBounds.width) {
                x = canvasBounds.width - width;
            }
            imageCanvas.scroll(x, iy, ix, iy, width, height, false);
            ix = x;
        }
    }

    private void scrollVertically(ScrollBar scrollBar) {
        if (image == null) return;
        Rectangle canvasBounds = imageCanvas.getClientArea();
        int width = Math.round(imageData.width * xscale);
        int height = Math.round(imageData.height * yscale);
        if (height > canvasBounds.height) {
            int y = -scrollBar.getSelection();
            if (y + height < canvasBounds.height) {
                y = canvasBounds.height - height;
            }
            imageCanvas.scroll(ix, y, ix, iy, width, height, false);
            iy = y;
        }
    }

    /**
	 * Open an error dialog displaying the specified information.
	 */
    private void showErrorDialog(String operation, String filename, Throwable e) {
        MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
        String errorMessage = "";
        if (e != null) {
            if (e instanceof SWTException) {
                SWTException swte = (SWTException) e;
                errorMessage = swte.getMessage();
                if (swte.throwable != null) {
                    errorMessage += ":\n" + swte.throwable.toString();
                }
            } else if (e instanceof SWTError) {
                SWTError swte = (SWTError) e;
                errorMessage = swte.getMessage();
                if (swte.throwable != null) {
                    errorMessage += ":\n" + swte.throwable.toString();
                }
            } else {
                errorMessage = e.toString();
            }
        }
        box.open();
    }

    /**
	 * Open a dialog asking the user for more information on the type of BMP
	 * file to save.
	 */
    private int showBMPDialog() {
        final int[] bmpType = new int[1];
        bmpType[0] = SWT.IMAGE_BMP;
        SelectionListener radioSelected = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                Button radio = (Button) event.widget;
                if (radio.getSelection()) bmpType[0] = ((Integer) radio.getData()).intValue();
            }
        };
        final Shell dialog = new Shell(shell, SWT.DIALOG_TRIM);
        dialog.setText("Save_as");
        dialog.setLayout(new GridLayout());
        Label label = new Label(dialog, SWT.NONE);
        label.setText("Save_as");
        Button radio = new Button(dialog, SWT.RADIO);
        radio.setText("Save_as_type_no_compress");
        radio.setSelection(true);
        radio.setData(new Integer(SWT.IMAGE_BMP));
        radio.addSelectionListener(radioSelected);
        radio = new Button(dialog, SWT.RADIO);
        radio.setText("Save_as_type_rle_compress");
        radio.setData(new Integer(SWT.IMAGE_BMP_RLE));
        radio.addSelectionListener(radioSelected);
        radio = new Button(dialog, SWT.RADIO);
        radio.setText("Save_as_type_os2");
        radio.setData(new Integer(SWT.IMAGE_OS2_BMP));
        radio.addSelectionListener(radioSelected);
        label = new Label(dialog, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button ok = new Button(dialog, SWT.PUSH);
        ok.setText("OK");
        GridData data = new GridData();
        data.horizontalAlignment = SWT.CENTER;
        data.widthHint = 75;
        ok.setLayoutData(data);
        ok.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                dialog.close();
            }
        });
        dialog.pack();
        dialog.open();
        while (!dialog.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        return bmpType[0];
    }

    /**
	 * Return the specified number of milliseconds. If the specified number of
	 * milliseconds is too small to see a visual change, then return a higher
	 * number.
	 */
    private int visibleDelay(int ms) {
        if (ms < 20) return ms + 30;
        if (ms < 30) return ms + 10;
        return ms;
    }

    /**
	 * Return the specified file's image type, based on its extension. Note that
	 * this is not a very robust way to determine image type, and it is only to
	 * be used in the absence of any better method.
	 */
    private int determineFileType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        if (ext.equalsIgnoreCase("bmp")) {
            return showBMPDialog();
        }
        if (ext.equalsIgnoreCase("gif")) return SWT.IMAGE_GIF;
        if (ext.equalsIgnoreCase("ico")) return SWT.IMAGE_ICO;
        if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) return SWT.IMAGE_JPEG;
        if (ext.equalsIgnoreCase("png")) return SWT.IMAGE_PNG;
        return SWT.IMAGE_UNDEFINED;
    }
}
