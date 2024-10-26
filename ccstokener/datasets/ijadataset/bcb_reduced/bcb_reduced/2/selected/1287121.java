package ucalgary.ebe.ci.mice.cursor;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import ucalgary.ebe.ci.mice.utils.MouseUtils;

/**
 * This class is the mouse cursor object. A mouse cursor is created as a shell.
 * @author herbiga
 *
 */
public class MouseCursor {

    private int mouseId = 0;

    private Image image = null;

    private Shell shell = null;

    private Region region = null;

    private String cursorText;

    private RGB cursorColor;

    private Display display;

    private int rotateDirection;

    private Point mouseShellLocation = new Point(0, 0);

    private Point rotateDelta;

    /**
	 * Constructor to create a new mouse cursor. cursorText and cursorColor can be null
	 * @param parent
	 * @param display
	 * @param mouseId
	 * @param cursorText
	 * @param cursorColor
	 * @param rotateDirection
	 */
    public MouseCursor(Shell parent, Display display, int mouseId, String cursorText, RGB cursorColor, int rotateDirection) {
        this.mouseId = mouseId;
        if (cursorText == null) {
            this.cursorText = "";
        } else {
            this.cursorText = cursorText;
        }
        if (cursorColor == null) {
            this.cursorColor = MouseUtils.CC_WHITE;
        } else {
            this.cursorColor = cursorColor;
        }
        this.rotateDirection = rotateDirection;
        this.display = display;
        this.image = loadImage();
        this.rotateDelta = setRotateDelta();
        setImageText();
        if (this.rotateDirection != 0) rotateCursor();
        addTransparancy();
        changeCursorColor(MouseUtils.CC_WHITE);
        createCursor(parent);
    }

    /**
	 * Garbage collection for cursor
	 */
    public void dispose() {
        shell.dispose();
        region.dispose();
        image.dispose();
    }

    /**
	 * Changes the color of the cursor
	 * @param newColor
	 */
    public void setColor(RGB newColor) {
        RGB oldColor = this.cursorColor;
        this.cursorColor = newColor;
        changeCursorColor(oldColor);
    }

    /**
	 * Sets a new text for the cursor
	 * @param newText
	 */
    public void setText(String newText) {
        image = loadImage();
        this.cursorText = newText;
        setImageText();
        if (this.rotateDirection != 0) rotateCursor();
        addTransparancy();
        changeCursorColor(MouseUtils.CC_WHITE);
        shapeShellAndRedraw();
    }

    /**
	 * Rotates the cursor
	 * @param rotateDirection
	 * @return
	 */
    public Point rotateCursor(int rotateDirection) {
        image = loadImage();
        setImageText();
        this.rotateDirection = rotateDirection;
        this.rotateDelta = setRotateDelta();
        if (this.rotateDirection != 0) rotateCursor();
        addTransparancy();
        changeCursorColor(MouseUtils.CC_WHITE);
        shapeShellAndRedraw();
        return this.rotateDelta;
    }

    /**
	 * Changes the style of the cursor
	 * @param cursorColor
	 * @param cursorText
	 * @param rotateDirection
	 * @return
	 */
    public Point changeCursorStyle(RGB cursorColor, String cursorText, int rotateDirection) {
        this.cursorColor = cursorColor;
        this.cursorText = cursorText;
        image = loadImage();
        setImageText();
        this.rotateDirection = rotateDirection;
        this.rotateDelta = setRotateDelta();
        if (this.rotateDirection != 0) rotateCursor();
        addTransparancy();
        changeCursorColor(MouseUtils.CC_WHITE);
        shapeShellAndRedraw();
        return this.rotateDelta;
    }

    /**
	 * Loads the standard cursor (GIF) 50x25 pixels 
	 */
    private Image loadImage() {
        Image newImage = null;
        try {
            newImage = new Image(display, "./resources/mouse4text.gif");
        } catch (Exception e) {
            java.net.URL url = this.getClass().getResource("/resources/mouse4text.gif");
            InputStream stream = null;
            try {
                stream = url.openStream();
                newImage = (new Image(display, stream));
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return newImage;
    }

    /**
	 * Sets the deltas which are caused through a rotation of the mouse cursor picture
	 * @return
	 */
    private Point setRotateDelta() {
        ImageData imageData = image.getImageData();
        Point temp = new Point(0, 0);
        switch(rotateDirection) {
            case SWT.LEFT:
                temp.y = imageData.width;
                temp.x = 0;
                break;
            case SWT.RIGHT:
                temp.y = 0;
                temp.x = imageData.height;
                break;
            case SWT.DOWN:
                temp.y = imageData.height;
                temp.x = imageData.width;
                break;
            default:
                temp.y = 0;
                temp.x = 0;
                break;
        }
        return temp;
    }

    /**
	 * Shapes and redraw the shell
	 */
    private void shapeShellAndRedraw() {
        region = new Region();
        final ImageData imageData = image.getImageData();
        if (imageData.alphaData != null) {
            Rectangle pixel = new Rectangle(0, 0, 1, 1);
            for (int y = 0; y < imageData.height; y++) {
                for (int x = 0; x < imageData.width; x++) {
                    if (imageData.getAlpha(x, y) == 255) {
                        pixel.x = imageData.x + x;
                        pixel.y = imageData.y + y;
                        region.add(pixel);
                    }
                }
            }
        } else {
            ImageData mask = imageData.getTransparencyMask();
            Rectangle pixel = new Rectangle(0, 0, 1, 1);
            for (int y = 0; y < mask.height; y++) {
                for (int x = 0; x < mask.width; x++) {
                    if (mask.getPixel(x, y) != 0) {
                        pixel.x = imageData.x + x;
                        pixel.y = imageData.y + y;
                        region.add(pixel);
                    }
                }
            }
        }
        shell.setRegion(region);
        shell.setSize(imageData.x + imageData.width, imageData.y + imageData.height);
        shell.redraw();
    }

    /**
	 * Sets the spezified text into the picture/cursor
	 */
    private void setImageText() {
        GC gc = new GC(image);
        gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
        gc.drawText(cursorText, 11, -3, true);
        gc.dispose();
    }

    /**
	 * Sets the spezified color to the cursor
	 */
    private void changeCursorColor(RGB oldColor) {
        ImageData imageData = image.getImageData();
        for (int x = 0; x < imageData.width; x++) {
            for (int y = 0; y < imageData.height; y++) {
                if (imageData.getPixel(x, y) == imageData.palette.getPixel(oldColor)) {
                    imageData.setPixel(x, y, imageData.palette.getPixel(cursorColor));
                }
            }
        }
        image = new Image(display, imageData);
    }

    /**
	 * Adds transparancy to the cursor picture
	 */
    private void addTransparancy() {
        ImageData imageData = image.getImageData();
        int transPixel = imageData.palette.getPixel(MouseUtils.CC_TRANSPARENCY);
        imageData.transparentPixel = transPixel;
        image = new Image(display, imageData);
    }

    /**
	 * Calls the rotate method
	 */
    private void rotateCursor() {
        image = new Image(display, rotate(image.getImageData(), rotateDirection));
    }

    /**
	 * Rotates an image
	 * Code is borrowed and modified from: http://www.eclipse.org/swt/snippets/
	 * Example snippet: rotate and flip an image
	 * Contributors:
	 * IBM Corporation - initial API and implementation
	 * @param srcData
	 * @param direction
	 * @return
	 */
    private static ImageData rotate(ImageData srcData, int direction) {
        int bytesPerPixel = srcData.bytesPerLine / srcData.width;
        int destBytesPerLine = (direction == SWT.DOWN) ? srcData.width * bytesPerPixel : srcData.height * bytesPerPixel;
        byte[] newData = new byte[(direction == SWT.DOWN) ? srcData.height * destBytesPerLine : srcData.width * destBytesPerLine];
        int width = 0, height = 0;
        for (int srcY = 0; srcY < srcData.height; srcY++) {
            for (int srcX = 0; srcX < srcData.width; srcX++) {
                int destX = 0, destY = 0, destIndex = 0, srcIndex = 0;
                switch(direction) {
                    case SWT.LEFT:
                        destX = srcY;
                        destY = srcData.width - srcX - 1;
                        width = srcData.height;
                        height = srcData.width;
                        break;
                    case SWT.RIGHT:
                        destX = srcData.height - srcY - 1;
                        destY = srcX;
                        width = srcData.height;
                        height = srcData.width;
                        break;
                    case SWT.DOWN:
                        destX = srcData.width - srcX - 1;
                        destY = srcData.height - srcY - 1;
                        width = srcData.width;
                        height = srcData.height;
                        break;
                }
                destIndex = (destY * destBytesPerLine) + (destX * bytesPerPixel);
                srcIndex = (srcY * srcData.bytesPerLine) + (srcX * bytesPerPixel);
                System.arraycopy(srcData.data, srcIndex, newData, destIndex, bytesPerPixel);
            }
        }
        return new ImageData(width, height, srcData.depth, srcData.palette, destBytesPerLine, newData);
    }

    /**
	 * This method creates the shell which simulates a mouse cursor
	 * Code is borrowed and modified from: http://www.eclipse.org/swt/snippets/
	 * Region snippet: Create non-rectangular shell from an image with transparency
	 * Original Contributors:
	 * IBM Corporation - initial API and implementation
	 * @param parent
	 */
    private void createCursor(Shell parent) {
        shell = new Shell(parent, SWT.NO_TRIM);
        region = new Region();
        final ImageData imageData = image.getImageData();
        if (imageData.alphaData != null) {
            Rectangle pixel = new Rectangle(0, 0, 1, 1);
            for (int y = 0; y < imageData.height; y++) {
                for (int x = 0; x < imageData.width; x++) {
                    if (imageData.getAlpha(x, y) == 255) {
                        pixel.x = imageData.x + x;
                        pixel.y = imageData.y + y;
                        region.add(pixel);
                    }
                }
            }
        } else {
            ImageData mask = imageData.getTransparencyMask();
            Rectangle pixel = new Rectangle(0, 0, 1, 1);
            for (int y = 0; y < mask.height; y++) {
                for (int x = 0; x < mask.width; x++) {
                    if (mask.getPixel(x, y) != 0) {
                        pixel.x = imageData.x + x;
                        pixel.y = imageData.y + y;
                        region.add(pixel);
                    }
                }
            }
        }
        shell.setRegion(region);
        Listener l = new Listener() {

            public void handleEvent(Event e) {
                if (e.type == SWT.Paint) {
                    e.gc.drawImage(image, imageData.x, imageData.y);
                }
            }
        };
        shell.addListener(SWT.Paint, l);
        shell.setSize(imageData.x + imageData.width, imageData.y + imageData.height);
        shell.open();
    }

    /**
	 * Returns the mouse ID
	 * @return
	 */
    public int getMouseId() {
        return mouseId;
    }

    /**
	 * Returns the location of the mouse cursor shell
	 * @return
	 */
    public Point getMouseShellLocation() {
        return mouseShellLocation;
    }

    /**
	 * Sets the location of the fake cursor
	 * @param mouseLocation
	 */
    public void setMouseShellLocation(Point mouseLocation) {
        this.mouseShellLocation = mouseLocation;
        shell.setLocation(mouseLocation);
    }

    /**
	 * Returns the mouse pointer location (including the rotation delta)
	 * @return
	 */
    public Point getMouseLocation() {
        Point mouseLocation = new Point(this.mouseShellLocation.x + this.rotateDelta.x, this.mouseShellLocation.y + this.rotateDelta.y);
        return mouseLocation;
    }

    /**
	 * Returns the rotate direction
	 * @return
	 */
    public int getRotateDirection() {
        return rotateDirection;
    }

    /**
	 * Returns the rotate delta
	 * @return
	 */
    public Point getRotateDelta() {
        return rotateDelta;
    }
}
