package org.formaria.svg;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.animation.AnimationElement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.formaria.aria.Project;
import org.formaria.aria.ProjectManager;
import org.formaria.aria.build.BuildProperties;

/**
 * 
 *
 * <p> Copyright (c) Formaria Ltd., 2008, This software is licensed under
 * the GNU Public License (GPL), please see license.txt for more details. If
 * you make commercial use of this software you must purchase a commercial
 * license from Formaria.</p>
 * <p> $Revision: 1.2 $</p>
 */
public class SvgImageMap extends JPanel implements MouseMotionListener, MouseListener {

    private String file, displayGroup;

    private SVGUniverse universe;

    private SVGDiagram diagram;

    private SVGElement element;

    private URI uri;

    private File svgFile;

    private double w, h;

    private String[] baseShapes;

    private double scaleX, scaleY, centreX, centreY, previousWidth, previousHeight;

    private int savedX, savedY, savedW, savedH;

    private SvgRolloverFinder rolloverFinder;

    private URL url, previousUrl;

    private Project currentProject;

    private boolean setRollOvers, firstResize, drawImages;

    private BufferedImage bufferedImage;

    private Group selected;

    private int bufferX, bufferY;

    private RenderingSemaphore semaphore;

    private BufferedImage[] images;

    private ArrayList listeners;

    private JPanel glassPane;

    private PointSystem pointSystem;

    private boolean firstZoom, coalesce;

    private double zoomHeight, zoomWidth, initialX, initialY, panX, panY, previousPanX, previousPanY;

    private DecimalFormat formatter;

    protected String blockPrefix = "b_";

    protected String rolloverPrefix = "r_";

    private boolean preserveAspect = true;

    private boolean centreImage = true;

    /**
   * Class constructor. 
   */
    public SvgImageMap() {
        currentProject = ProjectManager.getCurrentProject();
        setLayout(new BorderLayout());
        universe = SVGCache.getSVGUniverse();
        addMouseMotionListener(this);
        addMouseListener(this);
        diagram = null;
        uri = null;
        displayGroup = null;
        setRollOvers = true;
        scaleX = 1.0;
        scaleY = 1.0;
        centreX = 0.0;
        centreY = 0.0;
        previousWidth = getWidth();
        previousHeight = getHeight();
        glassPane = new JPanel();
        glassPane.setLayout(new BorderLayout());
        glassPane.setOpaque(false);
        glassPane.setLayout(null);
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(glassPane, JLayeredPane.PALETTE_LAYER);
        add(layeredPane);
        this.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent evt) {
                resize();
                repaint();
                glassPane.setSize(getWidth(), getHeight());
            }
        });
        setDoubleBuffered(false);
        setOpaque(false);
        semaphore = new RenderingSemaphore(1);
        listeners = new ArrayList();
        pointSystem = new PointSystem(this);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        formatter = (DecimalFormat) nf;
        formatter.applyPattern(".00000");
    }

    /**
   * Returns the glass pane component of the layered pane
   * @return the <CODE>JPanel</CODE> component returned
   */
    public JPanel getGlassPane() {
        return glassPane;
    }

    /**
   * Converts a coordinate from the glasspane coordinate system to the svg map coordinate system. 
   * @param x a <CODE>double</CODE> specifying the x coordinate to be converted.
   * @param y a <CODE>double</CODE> specifying the y coordinate to be converted.
   * @return <CODE>Point2D</CODE> object containing the converted coordinates.  
   */
    public Point2D glassPaneToSvg(double x, double y) {
        return pointSystem.glassPaneToSvg(x, y);
    }

    /**
   * Converts a coordinate from the svg map coordinate system to the glasspane coordinate system. 
   * @param x a <CODE>double</CODE> specifying the x coordinate to be converted.
   * @param y a <CODE>double</CODE> specifying the y coordinate to be converted.
   * @return <CODE>Point2D</CODE> object containing the converted coordinates.
   */
    public Point2D svgToGlassPane(double x, double y) {
        return pointSystem.svgToGlassPane(x, y);
    }

    /**
   * Set the compare interfaces for comparing groups/elements for 
   * identification of the rollovers
   * @param sim the comparison class
   */
    public void setSvgRolloverFinder(SvgRolloverFinder sim) {
        rolloverFinder = sim;
    }

    /**
   * Get the compare interface
   * @return the interface object or null if none has been set.
   */
    public SvgRolloverFinder getSvgRolloverFinder() {
        return rolloverFinder;
    }

    /**
   * Set the prefix for each block within the svg image.
   * @param prefix <CODE>String</CODE> specifying the block prefix.
   */
    public void setBlockPrefix(String prefix) {
        rolloverPrefix = prefix;
    }

    /**
   * Set the prefix for each rollover within the svg image.
   * @param prefix <CODE>String</CODE> specifying the rollover prefix.
   */
    public void setRolloverPrefix(String prefix) {
        rolloverPrefix = prefix;
    }

    /**
   * Set teh content
   * @param value the path specifying the location of the SVG image.
   */
    public void setContent(String value) {
        setURL(currentProject.findResource(value));
    }

    /**
   * Used to set the URL pointing to the SVG image that is to be displayed.
   * @param newUrl <code>URL</code> object specifying the location of the SVG image.
   */
    public void setURL(URL newUrl) {
        url = newUrl;
        w = 0;
        h = 0;
        display();
    }

    /**
   * Used to return the URL pointing to the SVG image that is being displayed.
   * @return <code>URL</code> object returned specifying the location of the SVG image.
   */
    public URL getURL() {
        return url;
    }

    /**
   * Displays the SVG image by adding the file to an SVGUniverse instance, 
   * extracting an SVGDiagram instance from SVGUniverse and displaying the diagram in an SVGDisplayPanel
   */
    public SVGDiagram display() {
        if (url != null) {
            try {
                uri = universe.loadSVG(url.openStream(), url.toString());
                diagram = universe.getDiagram(uri);
                bufferedImage = null;
                resize();
                previousUrl = url;
            } catch (Exception ex) {
                if (BuildProperties.DEBUG) ex.printStackTrace();
            }
        }
        return diagram;
    }

    /**
   * Reset the image buffer meaning that the image will be re-rendered the next time 
   * the paintComponent method is called. 
   */
    public void resetBuffer() {
        bufferedImage = null;
    }

    /**
   * Notify all listening components that the svg has been re-rendered. 
   */
    public void notifyListeners() {
        firePropertyChange("bufferedImage", null, bufferedImage);
    }

    /**
   * Used to paint a buffered image of the rendered svg to the component.
   * @param g the delegate <CODE>Graphics</CODE> object.
   */
    public void paintComponent(Graphics g) {
        if ((previousUrl == null) || !url.equals(previousUrl)) display();
        if (diagram != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            getParent().setBackground(Color.white);
            if (bufferedImage == null) {
                SVGRoot root = diagram.getRoot();
                if (root != null) {
                    int[] attr = root.getPresAbsolute("viewBox").getIntList();
                    bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D buffer = bufferedImage.createGraphics();
                    buffer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    diagram.setIgnoringClipHeuristic(true);
                    semaphore.acquire();
                    try {
                        root.render(buffer);
                    } catch (Exception ex) {
                        if (BuildProperties.DEBUG) ex.printStackTrace();
                    }
                    semaphore.release();
                    buffer.dispose();
                    bufferX = 0;
                    bufferY = 0;
                }
            }
            g2d.drawImage(bufferedImage, bufferX, bufferY, this);
            if (drawImages) drawAuxilaryImages(g2d);
            g2d.dispose();
        }
    }

    /**
   * Resizes the SVG image to fit the panel
   */
    public void resize() {
        if (diagram == null) return;
        SVGRoot root = diagram.getRoot();
        if (root != null) {
            try {
                double width = root.getPresAbsolute("width").getDoubleValue();
                double height = root.getPresAbsolute("height").getDoubleValue();
                double pWidth = getWidth();
                double pHeight = getHeight();
                if ((w == 0) || (h == 0)) {
                    w = (int) width;
                    h = (int) height;
                }
                if (((pHeight / h) * w) > pWidth) {
                    pHeight = pWidth / w * h;
                    scaleX = width / pWidth;
                    scaleY = height / pHeight;
                    centreY = (((getHeight() - pHeight) * scaleY) / 2.0) * -1.0;
                    centreX = 0.0;
                } else {
                    pWidth = pHeight / h * w;
                    scaleX = width / pWidth;
                    scaleY = height / pHeight;
                    centreX = (((getWidth() - pWidth) * scaleX) / 2.0) * -1.0;
                    centreY = 0.0;
                }
                if (preserveAspect) {
                    scaleX = Math.max(scaleX, scaleY);
                    scaleY = scaleX;
                }
                String viewBox = formatter.format(centreX) + " " + formatter.format(centreY) + " " + formatter.format(width * scaleX) + " " + formatter.format(height * scaleY);
                root.setAttribute("viewBox", AnimationElement.AT_XML, viewBox);
                root.build();
                resetBuffer();
                repaint();
                notifyListeners();
                firstZoom = true;
                initialX = centreX;
                initialY = centreY;
                previousPanX = 0;
                previousPanY = 0;
                panX = 0;
                panY = 0;
            } catch (Exception ex) {
                if (BuildProperties.DEBUG) ex.printStackTrace();
            }
        }
    }

    /**
   * Used to pan the image based on percentage of the current x and y locations.
   * @param x <CODE>double</CODE> specifying the percentage to increase/decrease the x-coordinate by.
   * @param y <CODE>double</CODE> specifying the percentage to increase/decrease the y-coordinate by.
   */
    public void pan(double x, double y) {
        SVGRoot root = diagram.getRoot();
        double[] attr = root.getPresAbsolute("viewBox").getDoubleList();
        boolean negX = false;
        boolean negY = false;
        if (Double.toString(x).charAt(0) == '-') {
            x = Double.parseDouble(Double.toString(x).substring(1));
            negX = true;
        }
        if (Double.toString(y).charAt(0) == '-') {
            y = Double.parseDouble(Double.toString(y).substring(1));
            negY = true;
        }
        double difX = (attr[2] * x) - attr[2];
        double difY = (attr[3] * y) - attr[3];
        double newX, newY;
        if (negX) newX = attr[0] - difX; else newX = attr[0] + difX;
        if (negY) newY = attr[1] - difY; else newY = attr[1] + difY;
        String viewBox = formatter.format(newX) + " " + formatter.format(newY) + " " + attr[2] + " " + attr[3];
        try {
            root.setAttribute("viewBox", AnimationElement.AT_XML, viewBox);
            root.build();
        } catch (Exception ex) {
            if (BuildProperties.DEBUG) ex.printStackTrace();
        }
        if (!coalesce) {
            resetBuffer();
            repaint();
            notifyListeners();
        }
        panX = previousPanX + (newX - initialX);
        panY = previousPanY + (newY - initialY);
    }

    /**
   * Resets the image to its original displayed coordinates.
   */
    public void resetPanning() {
        resize();
    }

    /**
   * Zooms the image by the scaling factor specified.
   * @param zoom <CODE>double</CODE> specifying the scaling factor.
   */
    public void zoom(double zoom) {
        previousPanX = panX;
        previousPanY = panY;
        if (diagram == null) return;
        SVGRoot root = diagram.getRoot();
        try {
            double[] attr = root.getPresAbsolute("viewBox").getDoubleList();
            double width = root.getPresAbsolute("width").getDoubleValue();
            double height = root.getPresAbsolute("height").getDoubleValue();
            double pWidth = getWidth();
            double pHeight = getHeight();
            double nonZoomWidth, nonZoomHeight, zW, zH;
            if (((pHeight / height) * width) > pWidth) {
                pHeight = pWidth / width * height;
                scaleX = width / pWidth;
                scaleY = height / pHeight;
                nonZoomWidth = width * scaleX;
                nonZoomHeight = height * scaleY;
                if (firstZoom) {
                    zoomWidth = scaleX;
                    zoomHeight = scaleY;
                    firstZoom = false;
                }
                scaleX = zoomWidth / zoom;
                scaleY = zoomHeight / zoom;
                zoomWidth = scaleX;
                zoomHeight = scaleY;
                zW = width * scaleX;
                zH = height * scaleY;
                centreY = (((getHeight() - pHeight) * scaleY) / 2.0) * -1.0;
                centreX = 0.0;
            } else {
                pWidth = pHeight / height * width;
                scaleX = width / pWidth;
                scaleY = height / pHeight;
                nonZoomWidth = width * scaleX;
                nonZoomHeight = height * scaleY;
                if (firstZoom) {
                    zoomWidth = scaleX;
                    zoomHeight = scaleY;
                    firstZoom = false;
                }
                scaleX = zoomWidth / zoom;
                scaleY = zoomHeight / zoom;
                zoomWidth = scaleX;
                zoomHeight = scaleY;
                zW = width * scaleX;
                zH = height * scaleY;
                centreX = (((getWidth() - pWidth) * scaleX) / 2.0) * -1.0;
                centreY = 0.0;
            }
            centreX = centreX + panX + ((nonZoomWidth - zW) / 2.0) * (pWidth / width);
            centreY = centreY + panY + ((nonZoomHeight - zH) / 2.0) * (pHeight / height);
            if (preserveAspect) {
                scaleX = Math.max(scaleX, scaleY);
                scaleY = scaleX;
            }
            String viewBox = formatter.format(centreX) + " " + formatter.format(centreY) + " " + formatter.format(width * scaleX) + " " + formatter.format(height * scaleY);
            initialX = centreX;
            initialY = centreY;
            root.setAttribute("viewBox", AnimationElement.AT_XML, viewBox);
            root.build();
            if (!coalesce) {
                resetBuffer();
                repaint();
                notifyListeners();
            }
        } catch (Exception ex) {
            if (BuildProperties.DEBUG) ex.printStackTrace();
        }
    }

    /**
   * Start coalesce, of panning and zooming actions.
   * The svg won't be repainted until the endCoalesce method is called.
   */
    public void startCoalesce() {
        coalesce = true;
    }

    /**
   * Stop coalescing, meaning the image will be repainted after the next pan or zoom action.
   */
    public void endCoalesce() {
        coalesce = false;
    }

    /** 
   * Set position of where the pan method is to begin panning from in the next pan action.
   * @param x <CODE>double</CODE> specifying the x coordinate of where the image is to be panned from.
   * @param y <CODE>double</CODE> specifying the y coordinate of where the image is to be panned from.
   */
    public void setPanCoords(double x, double y) {
        panX = previousPanX + (x - initialX);
        panY = previousPanY + (y - initialY);
    }

    /**
   * Resets the image to its original zoomed view.
   */
    public void resetZoom() {
        resize();
    }

    /**
   * Set the auxilary images to be displayed when dragging the image map.
   * @param images <CODE>BufferedImage</CODE> array containing the auxilary images to be displayed.
   */
    public void setAuxilaryImages(BufferedImage[] images) {
        this.images = images;
    }

    /**
   * Draws the auxilary images within the image map component.
   * @param g2d the <CODE>Graphics2D</CODE> object used to draw the images.
   */
    public void drawAuxilaryImages(Graphics2D g2d) {
        if (images != null) {
            g2d.drawImage(images[0], (bufferX - (images[0].getWidth())), (int) (bufferY - (images[0].getHeight())), this);
            g2d.drawImage(images[1], bufferX, (bufferY - (images[1].getHeight())), this);
            g2d.drawImage(images[2], (bufferX + getWidth()), (bufferY - (images[2].getHeight())), this);
            g2d.drawImage(images[3], (bufferX - (images[3].getWidth())), (bufferY), this);
            g2d.drawImage(images[4], (bufferX + getWidth()), bufferY, this);
            g2d.drawImage(images[5], (bufferX - images[5].getWidth()), (bufferY + getHeight()), this);
            g2d.drawImage(images[6], bufferX, (bufferY + getHeight()), this);
            g2d.drawImage(images[7], (bufferX + getWidth()), (bufferY + getHeight()), this);
        }
    }

    /**
   * Sets whether the auxilary images are to be drawn or not.
   * @param b <CODE>boolean</CODE> specifying whether the auxilary images are to be drawn or not.
   */
    public void setDrawAuxImages(boolean b) {
        drawImages = b;
    }

    /**
   * Returns the semaphore used by this class to block on rendering events.
   * @return RenderingSemaphore the semaphore instance used by this class.
   */
    public RenderingSemaphore getSemaphore() {
        return semaphore;
    }

    /**
   * Set the location of the buffered image in the panel.
   * @param x the x coordinate of the buffered image.
   * @param y the y coordinate of the buffered image.
   */
    public void setImageLocation(int x, int y) {
        bufferX = x;
        bufferY = y;
    }

    /**
   * Sets the buffered image to be painted.
   * @param b <CODE>BufferedImage</CODE> set to true if movable image is to be repainted.
   */
    public void setImage(BufferedImage b) {
        bufferedImage = b;
    }

    /**
   * Returns the rendered buffered image of the svg displayed by this class
   * @return the returned <CODE>BufferedImage</CODE> instance
   */
    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    /**
   * Returns the instance of the SVG diagram used to access the image data.
   */
    public SVGDiagram getSvgDiagram() {
        return diagram;
    }

    /**
   * Sets the viewbox at the specified coordinates and preserves the aspect ratio of the image.
   * @param x <CODE>double</CODE> specifying the x coordinate
   * @param y <CODE>double</CODE> specifying the y coordinate
   * @param width <CODE>double</CODE> specifying the width
   * @param height <CODE>double</CODE> specifying the height
   */
    public void setViewBox(double x, double y, double width, double height) {
        SVGRoot root = diagram.getRoot();
        double newWidth = 0, newHeight = 0;
        double pWidth = getWidth();
        double pHeight = getHeight();
        double startWidth = root.getPresAbsolute("width").getDoubleValue();
        double startHeight = root.getPresAbsolute("height").getDoubleValue();
        if (pWidth < (pHeight * (startHeight / startWidth))) {
            newWidth = (width * (startWidth / pWidth));
            newHeight = (newWidth * (startHeight / startWidth));
            y = y - (((newHeight - height) / (startHeight / pHeight)) / 2.0);
        } else {
            newHeight = (height * (startHeight / pHeight));
            newWidth = (newHeight * (startWidth / startHeight));
            x = x - (((newWidth - width) / (startWidth / pWidth)) / 2.0);
        }
        String viewBox = Integer.toString((int) (x)) + " " + Integer.toString((int) (y)) + " " + Integer.toString((int) (newWidth)) + " " + Integer.toString((int) (newHeight));
        try {
            root.setAttribute("viewBox", AnimationElement.AT_XML, viewBox);
            root.build();
            resetBuffer();
            repaint();
        } catch (Exception ex) {
            if (BuildProperties.DEBUG) ex.printStackTrace();
        }
    }

    /**
   * Extracts Group objects from SVG file and 
   * loads them into the corresponding arrays
   */
    public void loadArrays() {
        SVGRoot root = diagram.getRoot();
        Vector vector = new Vector();
        root.getChildren(vector);
        int bSize = 0;
        int loop = 0;
        int numElements = vector.size();
        String[] bases = new String[numElements];
        while (loop < vector.size()) {
            SVGElement element = (SVGElement) vector.get(loop);
            if (!(element instanceof Group)) vector.remove(loop); else {
                String elementId = element.getId();
                if ((elementId.startsWith(blockPrefix)) || (rolloverFinder != null && rolloverFinder.isRolloverName(elementId))) {
                    bases[bSize] = elementId;
                    bSize += 1;
                }
                loop++;
            }
        }
        baseShapes = new String[bSize];
        for (int i = 0; i < bSize; i++) baseShapes[i] = bases[i];
        bases = null;
        if (rolloverFinder != null) rolloverFinder.setup(vector, baseShapes);
    }

    /**
   * Sets the visibility of a group
   * @param group <code>String</code> specifying the SVG grouping to be displayed
   * @param v <code>boolean</code> specifying whether the group is to be set visible or hidden
   */
    public void setVisibility(String group, boolean v) {
        selected = (Group) diagram.getElement(group);
        try {
            selected.setAttribute("visibility", AnimationElement.AT_XML, v ? "visible" : "hidden");
            selected.updateTime(0.0);
            resetBuffer();
            repaint();
        } catch (Exception ex) {
            if (BuildProperties.DEBUG) ex.printStackTrace();
        }
    }

    /**
   * Monitors mouse movement and calls checkBlock method.
   * to check if the mouse pointer position is within an SVG group
   * @param e the <code>MouseEvent</code> that occured 
   */
    public void mouseMoved(MouseEvent e) {
        if (setRollOvers) {
            try {
                if (baseShapes == null) return;
                if (displayGroup != null) setVisibility(displayGroup, false);
                int i = 0;
                boolean found = false;
                while ((i < baseShapes.length) && !found) {
                    if (checkBlock(e.getPoint(), baseShapes[i])) {
                        if (rolloverFinder != null) displayGroup = rolloverFinder.getRolloverName(displayGroup); else displayGroup = rolloverPrefix + baseShapes[i].substring(blockPrefix.length());
                        found = true;
                    }
                    i++;
                }
                if (found == false) displayGroup = null;
                if (displayGroup != null) setVisibility(displayGroup, true);
            } catch (Exception ex) {
                if (BuildProperties.DEBUG) ex.printStackTrace();
            }
        }
        fireActionPerformed(e);
    }

    /**
   * Monitors mouse dragged events.
   * @param e the <code>MouseEvent</code> that occured
   */
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
   * Checks if the mouse pointer position is within a particular group
   * @param p <code>Point</code> specifying the current mouse pointer position
   * @param s <code>String</code> specifying the name of the group to be checked
   */
    private boolean checkBlock(Point p, String s) {
        Group group = (Group) diagram.getElement(s);
        Shape shape = group.getShape();
        if (shape.contains((p.getX() * scaleX + centreX), (p.getY() * scaleY + centreY))) return true;
        return false;
    }

    /**
   * Set the content of the image map.
   * @param attribValue <CODE>Object</CODE> specifying the content.
   */
    public void setImageName(String attribValue) {
        setContent(attribValue);
    }

    /**
   * Returns the content of the image map. 
   * @return <CODE>String</CODE> specifying the content value.
   */
    public String getImageName() {
        if (url != null) {
            String name = url.toString();
            name = name.substring(name.lastIndexOf("/") + 1);
            return name;
        }
        return null;
    }

    /**
   * Returns the url of the image displayed in the image map. 
   * @return <CODE>String</CODE> specifying the content value.
   */
    public String getUrl() {
        if (url != null) return url.toString();
        return null;
    }

    /**
   * Allow rollovers or not based on boolean value.
   * @param setRollOvers <CODE>boolean</CODE> used to set if rollovers are to be allowed or not.
   */
    public void setRollOverStatus(boolean setRollOvers) {
        this.setRollOvers = setRollOvers;
    }

    /**
   * Return the scaleFactor currently being applied to the x-axis.
   */
    public double getScaleX() {
        return scaleX;
    }

    /**
   * Return the scaleFactor currently being applied to the y-axis.
   */
    public double getScaleY() {
        return scaleY;
    }

    /**
   * Return the center position of the image on the x-axis.
   */
    public double getCenterX() {
        return centreX;
    }

    /**
   * Return the center position of the image on the y-axis.
   */
    public double getCenterY() {
        return centreY;
    }

    /**
   * Adds a mouse listener to this component to listen for mouse events.
   * @param l the <CODE>MouseListener</CODE> that is to be added. 
   */
    public void addMapMouseListener(MouseListener l) {
        addMouseListener(l);
        super.addMouseListener(this);
        listeners.add(l);
        listeners.add(this);
    }

    /**
   * Removes a mouse listener to this component.
   * @param l the <CODE>MouseListener</CODE> that is to be removed.
   */
    public void removeMapMouseListener(MouseListener l) {
        removeMouseListener(l);
        super.removeMouseListener(this);
        listeners.remove(l);
        listeners.remove(this);
    }

    /**
   * Detect if mouse has exited the component and reset visible rollove to hidden.
   */
    public void mouseExited(MouseEvent e) {
        if (displayGroup != null) setVisibility(displayGroup, false);
        fireActionPerformed(e);
    }

    public void mouseClicked(MouseEvent e) {
        fireActionPerformed(e);
    }

    public void mousePressed(MouseEvent e) {
        fireActionPerformed(e);
    }

    public void mouseReleased(MouseEvent e) {
        fireActionPerformed(e);
    }

    public void mouseEntered(MouseEvent e) {
        fireActionPerformed(e);
    }

    /**
   * Used to add an <code>ActionListener</code> to the list of action listeners.
   * @param al the listener to be added
   */
    public void addActionListener(ActionListener al) {
        listenerList.add(ActionListener.class, al);
    }

    /**
  * Removes an <code>ActionListener</code> from the button.
  * If the listener is the currently set <code>Action</code>
  * for the button, then the <code>Action</code>
  * is set to <code>null</code>.
  *
  * @param l the listener to be removed
  */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
  * Notifies all mouse listeners that have registered interest for
  * notification on mouse event types.  The event instance 
  * is lazily created using the <code>event</code> 
  * parameter.
  *
  * @param event the <code>MouseEvent</code> object
  */
    protected void fireActionPerformed(MouseEvent event) {
        MouseEvent e = null;
        for (int i = 0; i < listeners.size(); i++) {
            if (listeners.get(i) instanceof MouseListener) {
                if (e == null) {
                    switch(event.getID()) {
                        case MouseEvent.MOUSE_ENTERED:
                            e = new MouseEvent(SvgImageMap.this, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false);
                            ((MouseListener) listeners.get(i)).mouseEntered(e);
                        case MouseEvent.MOUSE_EXITED:
                            e = new MouseEvent(SvgImageMap.this, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false);
                            ((MouseListener) listeners.get(i)).mouseExited(e);
                        case MouseEvent.MOUSE_RELEASED:
                            e = new MouseEvent(SvgImageMap.this, MouseEvent.MOUSE_RELEASED, 0, 0, 0, 0, 0, false);
                            ((MouseListener) listeners.get(i)).mouseReleased(e);
                        case MouseEvent.MOUSE_PRESSED:
                            e = new MouseEvent(SvgImageMap.this, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 0, false);
                            ((MouseListener) listeners.get(i)).mousePressed(e);
                        case MouseEvent.MOUSE_CLICKED:
                            e = new MouseEvent(SvgImageMap.this, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 0, false);
                            ((MouseListener) listeners.get(i)).mouseClicked(e);
                    }
                }
            }
        }
    }

    /**
  * Notifies all listeners that have registered interest for
  * notification on this event type.  The event instance 
  * is lazily created using the <code>event</code> 
  * parameter.
  *
  * @param event the <code>ActionEvent</code> object
  * @see EventListenerList
  */
    protected void fireActionPerformed(ActionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    String actionCommand = event.getActionCommand();
                    e = new ActionEvent(SvgImageMap.this, ActionEvent.ACTION_PERFORMED, actionCommand, event.getWhen(), event.getModifiers());
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    /**
   * Returns the id of the rollover block currently selected.
   * @param e the triggered <CODE>MouseEvent</CODE>.
   * @return the id <CODE>String</CODE>.
   */
    public String getSelectedRollover(MouseEvent e) {
        if (selected != null) {
            if (checkBlock(e.getPoint(), selected.getId())) {
                return selected.getId();
            }
        }
        return null;
    }

    /**
   * Does the component preserve the aspect ratio of the image?
   * @return true if the aspect ratio is preserved.
   */
    public boolean getPreserveAspect() {
        return preserveAspect;
    }

    /**
   * Set the aspect ratio preservation flag
   * @param preserve true to preserve the aspect
   */
    public void setPreserveAspect(boolean preserve) {
        preserveAspect = preserve;
    }
}
