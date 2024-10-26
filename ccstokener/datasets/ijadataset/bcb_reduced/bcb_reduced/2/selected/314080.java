package org.openscience.jmol.applet;

import org.openscience.jmol.*;
import java.awt.BorderLayout;
import java.io.InputStream;
import java.net.URL;

/**
 * Subset version of JMol which appears as a componant and can be controlled with strings.
**/
public class JmolSimpleBean extends java.awt.Panel implements java.awt.event.ComponentListener {

    private DisplaySettings settings = new DisplaySettings();

    private DisplayPanel display;

    private java.awt.Panel animPanel = null;

    private java.awt.Panel customViewPanel = null;

    private boolean ready = false;

    private boolean modelReady = false;

    private boolean typesReady = false;

    private String customViews = null;

    public JmolSimpleBean() {
        setLayout(new BorderLayout());
        display = new DisplayPanel();
        display.addComponentListener(this);
        display.setDisplaySettings(settings);
        add(display, "Center");
        setBackgroundColour("#FFFFFF");
        setForegroundColour("#000000");
    }

    public void toggleBonds() {
        display.toggleBonds();
    }

    public void setZoomFactor(float factor) {
        if (factor < 0.1f) {
            display.setZoomFactor(0.1f);
        } else {
            display.setZoomFactor(factor);
        }
    }

    public void setAtomSphereFactor(float factor) {
        if (factor < 0.1f) {
            settings.setAtomSphereFactor(0.2 * 0.1);
        } else {
            settings.setAtomSphereFactor(0.2 * factor);
        }
    }

    public void setCustomViews(String cv) {
        customViews = cv;
    }

    /**
   * Sets the current model to the ChemFile given.
   *
   * @param cf  the ChemFile
   */
    public void setModel(ChemFile cf) {
        if (cf.getNumberFrames() > 1) {
            animPanel = new java.awt.Panel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            java.awt.Button btn = new java.awt.Button("Prev");
            btn.setActionCommand("PREV");
            btn.addActionListener(display);
            animPanel.add(btn);
            btn = new java.awt.Button("Next");
            btn.setActionCommand("NEXT");
            btn.addActionListener(display);
            animPanel.add(btn);
            java.awt.Label lbl = new java.awt.Label();
            display.setFrameLabel(lbl);
            animPanel.add(lbl);
            add(animPanel, "South");
        } else {
            animPanel = null;
        }
        if ((customViews != null) && (customViews.length() > 0)) {
            customViewPanel = new java.awt.Panel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            java.util.StringTokenizer st = new java.util.StringTokenizer(customViews);
            try {
                String viewName = st.nextToken("{");
                while (true) {
                    String viewData = st.nextToken("}").substring(1);
                    java.awt.Button btn = new java.awt.Button(viewName);
                    btn.setActionCommand(DisplayPanel.customViewPrefix + viewData);
                    btn.addActionListener(display);
                    customViewPanel.add(btn);
                    viewName = st.nextToken("{").substring(1);
                }
            } catch (java.util.NoSuchElementException E) {
            }
            add(customViewPanel, "North");
        }
        modelReady = true;
        display.setChemFile(cf);
        ready = areWeReady();
    }

    /**
   * Takes the argument, reads it as a file and allocates this as
   * the current atom types- eg radius etc.
   * @param propertiesFile The filename of the properties we want.
   */
    public void setAtomPropertiesFromFile(String propertiesFile) {
        try {
            AtomTypeSet ats1 = new AtomTypeSet();
            ats1.load(new java.io.FileInputStream(propertiesFile));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        typesReady = true;
        ready = areWeReady();
    }

    /**
   * Takes the argument, reads it and allocates this as the current
   * atom types- eg radius etc.
   * @param propertiesURL The URL of the properties we want.
   */
    public void setAtomPropertiesFromURL(URL propertiesURL) {
        try {
            AtomTypeSet ats1 = new AtomTypeSet();
            ats1.load(propertiesURL.openStream());
        } catch (java.io.IOException e1) {
            System.err.println("Error loading atom properties from URL '" + propertiesURL + "': " + e1);
        }
        typesReady = true;
        ready = areWeReady();
    }

    /**
   * Takes the argument, reads it as a URL and allocates this as the
   * current atom types- eg radius etc.
   * @param propertiesFileURL The URL of the properties we want.
   */
    public void setAtomPropertiesFromURL(String propertiesURL) {
        try {
            AtomTypeSet ats1 = new AtomTypeSet();
            java.net.URL url1 = new java.net.URL(propertiesURL);
            ats1.load(url1.openStream());
        } catch (java.io.IOException e1) {
            System.err.println("Error loading atom properties from URL '" + propertiesURL + "': " + e1);
        }
        typesReady = true;
        ready = areWeReady();
    }

    public void setAtomPropertiesFromStream(InputStream is) {
        try {
            AtomTypeSet ats1 = new AtomTypeSet();
            ats1.load(is);
        } catch (java.io.IOException e1) {
            System.err.println("Error loading atom properties from Stream'" + is.toString() + "': " + e1);
        }
        typesReady = true;
        ready = areWeReady();
    }

    /**
   * Set the background colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
    public void setBackgroundColour(String colourInHex) {
        DisplayPanel.setBackgroundColor(getColourFromHexString(colourInHex));
    }

    /**
   * Set the background colour.
   * @param colour The colour
   */
    public void setBackgroundColour(java.awt.Color colour) {
        DisplayPanel.setBackgroundColor(colour);
    }

    /**
   * Get the background colour.
   */
    public java.awt.Color getBackgroundColour() {
        return DisplayPanel.getBackgroundColor();
    }

    /**
   * Set the foreground colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
    public void setForegroundColour(String colourInHex) {
        display.setForegroundColor(getColourFromHexString(colourInHex));
    }

    /**
   * Set the foreground colour.
   * @param colour The colour
   */
    public void setForegroundColour(java.awt.Color colour) {
        display.setForegroundColor(colour);
    }

    /**
   * Get the foreground colour.
   */
    public java.awt.Color getForegroundColour() {
        return display.getForegroundColor();
    }

    /**
   * Causes Atoms to be shown or hidden.
   * @param value if 'T' then atoms are displayed, if 'F' then they aren't.
   */
    public void setAtomsShown(String value) {
        settings.setShowAtoms(getBooleanFromString(value));
    }

    /**
   * Causes Atoms to be shown or hidden.
   * @param value if true then atoms are displayed, if false then they aren't.
   */
    public void setAtomsShown(boolean value) {
        settings.setShowAtoms(value);
    }

    /**
   * Are Atoms to being shown or hidden?
   */
    public boolean getAtomsShown() {
        return settings.getShowAtoms();
    }

    /**
   * Causes bonds to be shown or hidden.
   * @param value if "T" then atoms are displayed, if "F" then they aren't.
   */
    public void setBondsShown(String value) {
        display.showBonds(getBooleanFromString(value));
    }

    /**
   * Causes bonds to be shown or hidden.
   * @param value if true then bonds are displayed, if false then they aren't.
   */
    public void setBondsShown(boolean value) {
        display.showBonds(value);
    }

    /**
   * Are bonds being shown or hidden?
   */
    public boolean getBondsShown() {
        return display.getShowBonds();
    }

    public void setWireframeRotation(boolean active) {
        display.setWireframeRotation(active);
    }

    public boolean getWireframeRotation() {
        return display.getWireframeRotation();
    }

    /**
   * Sets the rendering mode for atoms. Valid values are
   * 'QUICKDRAW', 'SHADED' and 'WIREFRAME'.
   */
    public void setAtomRenderingStyle(String style) {
        if (style.equalsIgnoreCase("QUICKDRAW")) {
            settings.setAtomDrawMode(DisplaySettings.QUICKDRAW);
        } else if (style.equalsIgnoreCase("SHADED")) {
            settings.setAtomDrawMode(DisplaySettings.SHADING);
        } else if (style.equalsIgnoreCase("WIREFRAME")) {
            settings.setAtomDrawMode(DisplaySettings.WIREFRAME);
        } else {
            throw new IllegalArgumentException("Unknown atom rendering style: " + style);
        }
        display.displaySettingsChanged();
        display.repaint();
    }

    /**
   * Gets the rendering mode for atoms. Values are 'QUICKDRAW',
   * 'SHADED' and 'WIREFRAME'.
   */
    public String getAtomRenderingStyleDescription() {
        if (settings.getAtomDrawMode() == DisplaySettings.QUICKDRAW) {
            return ("QUICKDRAW");
        } else if (settings.getAtomDrawMode() == DisplaySettings.SHADING) {
            return ("SHADED");
        } else if (settings.getAtomDrawMode() == DisplaySettings.WIREFRAME) {
            return ("WIREFRAME");
        }
        return "NULL";
    }

    /**
   * Sets the rendering mode for bonds. Valid values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'.
   */
    public void setBondRenderingStyle(String style) {
        if (style.equalsIgnoreCase("QUICKDRAW")) {
            settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
        } else if (style.equalsIgnoreCase("SHADED")) {
            settings.setBondDrawMode(DisplaySettings.SHADING);
        } else if (style.equalsIgnoreCase("LINE")) {
            settings.setBondDrawMode(DisplaySettings.LINE);
        } else if (style.equalsIgnoreCase("WIREFRAME")) {
            settings.setBondDrawMode(DisplaySettings.WIREFRAME);
        } else {
            throw new IllegalArgumentException("Unknown bond rendering style: " + style);
        }
        display.displaySettingsChanged();
        display.repaint();
    }

    /**
   * Gets the rendering mode for bonds. Values are 'QUICKDRAW',
   * 'SHADED', 'LINE' and 'WIREFRAME'.
   */
    public String getBondRenderingStyleDescription() {
        if (settings.getBondDrawMode() == DisplaySettings.QUICKDRAW) {
            return ("QUICKDRAW");
        } else if (settings.getBondDrawMode() == DisplaySettings.SHADING) {
            return ("SHADED");
        } else if (settings.getBondDrawMode() == DisplaySettings.LINE) {
            return ("LINE");
        } else if (settings.getBondDrawMode() == DisplaySettings.WIREFRAME) {
            return ("WIREFRAME");
        }
        return "NULL";
    }

    /**
   * Sets the rendering mode for labels. Valid values are 'NONE',
   * 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
    public void setLabelRenderingStyle(String style) {
        if (style.equalsIgnoreCase("NONE")) {
            settings.setLabelMode(DisplaySettings.NOLABELS);
        } else if (style.equalsIgnoreCase("SYMBOLS")) {
            settings.setLabelMode(DisplaySettings.SYMBOLS);
        } else if (style.equalsIgnoreCase("TYPES")) {
            settings.setLabelMode(DisplaySettings.TYPES);
        } else if (style.equalsIgnoreCase("NUMBERS")) {
            settings.setLabelMode(DisplaySettings.NUMBERS);
        } else {
            throw new IllegalArgumentException("Unknown label rendering style: " + style);
        }
        display.displaySettingsChanged();
        display.repaint();
    }

    /**
   * Gets the rendering mode for labels. Values are 'NONE',
   * 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
    public String getLabelRenderingStyleDescription() {
        if (settings.getLabelMode() == DisplaySettings.NOLABELS) {
            return ("NONE");
        } else if (settings.getLabelMode() == DisplaySettings.SYMBOLS) {
            return ("SYMBOLS");
        } else if (settings.getLabelMode() == DisplaySettings.TYPES) {
            return ("TYPES");
        } else if (settings.getLabelMode() == DisplaySettings.NUMBERS) {
            return ("NUMBERS");
        }
        return "NULL";
    }

    public void setPickingMode(String style) {
        if (style.equalsIgnoreCase("MULTIPLE")) {
            display.setPickingMode(DisplayPanel.MULTIPLEPICK);
        } else {
            display.setPickingMode(DisplayPanel.SINGLEPICK);
        }
    }

    /**
   * Sets the picked atoms.
   *
   * @param pickedAtoms a boolean array containing true or false representing
   * picked or not picked for each atom in the molecule.
   */
    public void setPickedAtoms(boolean[] pickedAtoms) {
        ChemFrame cf = display.getFrame();
        cf.deselectAll();
        cf.setPickedAtoms(pickedAtoms);
    }

    /**
   * Sets whether they view automatically goes to wireframe when they model is rotated.
   * @param doesIt String either "T" or "F"
   */
    public void setAutoWireframe(String doesIt) {
        display.setWireframeRotation(getBooleanFromString(doesIt));
    }

    /**
   * Sets whether they view automatically goes to wireframe when they model is rotated.
   * @param doesIt If true then wireframe rotation is on, otherwise its off.
   */
    public void setAutoWireframe(boolean doesIt) {
        display.setWireframeRotation(doesIt);
    }

    /**
   * Gets whether the view automatically goes to wireframe when they model is rotated.
   */
    public boolean getAutoWireframe() {
        return display.getWireframeRotation();
    }

    /**
   * Returns true if passed "T" and "F" if passed false. Throws
   * IllegalArgumentException if parameter is not "T" ot "F"
                                                                                                                                                                                                                                                                   *
   * @param value String equal to either "T" or "F"
                                                                                                                                                                                                                                                                   */
    protected boolean getBooleanFromString(String value) {
        if (value.equalsIgnoreCase("T")) {
            return true;
        } else if (value.equalsIgnoreCase("F")) {
            return false;
        } else {
            throw new IllegalArgumentException("Boolean string must be 'T' or 'F'");
        }
    }

    /**
   * Turns a string in the form '#RRGGBB' eg. '#FFFFFF' is white,
   * into a colour
                                                                                                                                                                                                                                                                   */
    protected java.awt.Color getColourFromHexString(String colourName) {
        if ((colourName == null) || (colourName.length() != 7)) {
            throw new IllegalArgumentException("Colour name: " + colourName + " is either null ot not seven chars long");
        }
        java.awt.Color colour = null;
        try {
            String rdColour = "0x" + colourName.substring(1, 3);
            String gnColour = "0x" + colourName.substring(3, 5);
            String blColour = "0x" + colourName.substring(5, 7);
            int red = (Integer.decode(rdColour)).intValue();
            int green = (Integer.decode(gnColour)).intValue();
            int blue = (Integer.decode(blColour)).intValue();
            colour = new java.awt.Color(red, green, blue);
        } catch (NumberFormatException e) {
            System.out.println("MDLView: Error extracting colour, using white");
            colour = new java.awt.Color(255, 255, 255);
        }
        return colour;
    }

    /**
   * Take the given string and chop it up into a series
   * of strings on whitespace boundries.  This is useful
   * for trying to get an array of strings out of the
   * resource file.
   */
    protected String[] tokenize(String input) {
        java.util.Vector v = new java.util.Vector();
        java.util.StringTokenizer t = new java.util.StringTokenizer(input);
        while (t.hasMoreTokens()) {
            v.addElement(t.nextToken());
        }
        String[] cmd = new String[v.size()];
        for (int i = 0; i < cmd.length; i++) {
            cmd[i] = (String) v.elementAt(i);
        }
        return cmd;
    }

    private boolean areWeReady() {
        return (modelReady && typesReady);
    }

    public void componentHidden(java.awt.event.ComponentEvent e) {
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
    }

    /**Warning this adds the mouseListener to the canvas itself to allow following of mouse 'on the bean'.**/
    public void addMouseListener(java.awt.event.MouseListener ml) {
        display.addMouseListener(ml);
    }

    /**Warning this adds the KeyListener to the canvas itself to allow following of key use 'on the bean'.**/
    public void addKeyListener(java.awt.event.KeyListener kl) {
        display.addKeyListener(kl);
    }
}
