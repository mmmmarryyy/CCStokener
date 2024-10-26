package org.openscience.miniJmol;

import java.util.Hashtable;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

/**
 * Subset version of JMol which appears as a componant and can be controlled with strings.
**/
public class JmolSimpleBean extends javax.swing.JPanel implements java.awt.event.ComponentListener {

    private DisplaySettings settings = new DisplaySettings();

    private displayPanel display;

    private ChemFile cf;

    private boolean ready = false;

    private boolean modelReady = false;

    private boolean typesReady = false;

    public JmolSimpleBean() {
        setLayout(new BorderLayout());
        display = new displayPanel();
        display.addComponentListener(this);
        display.setDisplaySettings(settings);
        add(display, "Center");
        setBackgroundColour("#FFFFFF");
        setForegroundColour("#000000");
    }

    /**
    /**
     * Sets the current model to the ChemFile given.
     *
     * @param cf  the ChemFile
     */
    public void setModel(ChemFile cf) {
        this.cf = cf;
        modelReady = true;
        displayIfAreWeReady();
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
        displayIfAreWeReady();
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
        }
        typesReady = true;
        displayIfAreWeReady();
    }

    /**
     * Takes the argument, reads it and allocates this as the current
     * atom types- eg radius etc.
     * @param propertiesURL The URL of the properties we want.
     */
    public void setAtomPropertiesFromURL(java.net.URL propertiesURL) {
        try {
            AtomTypeSet ats1 = new AtomTypeSet();
            ats1.load(propertiesURL.openStream());
        } catch (java.io.IOException e1) {
        }
        typesReady = true;
        displayIfAreWeReady();
    }

    /**
     * Set the background colour.
     * @param colourInHex The colour in the format #FF0000 for red etc
     */
    public void setBackgroundColour(String colourInHex) {
        display.setBackgroundColor(getColourFromHexString(colourInHex));
    }

    /**
     * Set the background colour.
     * @param colour The colour
     */
    public void setBackgroundColour(java.awt.Color colour) {
        display.setBackgroundColor(colour);
    }

    /**
     * Get the background colour.
     */
    public java.awt.Color getBackgroundColour() {
        return display.getBackgroundColor();
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
     * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
     */
    public void setAtomsShown(String TorF) {
        display.showAtoms(getBooleanFromString(TorF));
    }

    /**
     * Causes Atoms to be shown or hidden.
     * @param TorF if true then atoms are displayed, if false then they aren't.
     */
    public void setAtomsShown(boolean TorF) {
        display.showAtoms(TorF);
    }

    /**
     * Are Atoms to being shown or hidden?
     */
    public boolean getAtomsShown() {
        return display.getShowAtoms();
    }

    /**
     * Causes bonds to be shown or hidden.
     * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
     */
    public void setBondsShown(String TorF) {
        display.showBonds(getBooleanFromString(TorF));
    }

    /**
     * Causes bonds to be shown or hidden.
     * @param TorF if true then bonds are displayed, if false then they aren't.
     */
    public void setBondsShown(boolean TorF) {
        display.showBonds(TorF);
    }

    /**
     * Are bonds being shown or hidden?
     */
    public boolean getBondsShown() {
        return display.getShowBonds();
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

    /**
     * Sets whether they view automatically goes to wireframe when they model is rotated.
     * @param doesIt String either 'T' or 'F'
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

    public void forceDisplay() {
        display.setChemFile(cf);
    }

    protected void displayIfAreWeReady() {
        if (areWeReady()) {
            display.setChemFile(cf);
        }
    }

    /**
     * returns the ChemFile that we are currently working with
     *
     * @see ChemFile
     */
    public ChemFile getCurrentFile() {
        return cf;
    }

    /**
     * Returns true if passed 'T' and 'F' if passed false. Throws
     * IllegalArgumentException if parameter is not 'T' ot 'F'
    *
     * @param TorF String equal to either TorF
    */
    protected boolean getBooleanFromString(String TorF) {
        if (TorF.equalsIgnoreCase("T")) {
            return true;
        } else if (TorF.equalsIgnoreCase("F")) {
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
        if (colourName == null || colourName.length() != 7) {
            throw new IllegalArgumentException("Colour name: " + colourName + " is either null ot not seven chars long");
        }
        java.awt.Color colour = null;
        try {
            int red;
            int green;
            int blue;
            String rdColour = "0x" + colourName.substring(1, 3);
            String gnColour = "0x" + colourName.substring(3, 5);
            String blColour = "0x" + colourName.substring(5, 7);
            red = (Integer.decode(rdColour)).intValue();
            green = (Integer.decode(gnColour)).intValue();
            blue = (Integer.decode(blColour)).intValue();
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
        String cmd[];
        while (t.hasMoreTokens()) v.addElement(t.nextToken());
        cmd = new String[v.size()];
        for (int i = 0; i < cmd.length; i++) {
            cmd[i] = (String) v.elementAt(i);
        }
        return cmd;
    }

    private boolean areWeReady() {
        return (modelReady && typesReady);
    }

    private void whyArentWeReady() throws IllegalStateException {
        if (ready) {
            throw new RuntimeException("Why aren't we ready? We ARE ready!!");
        } else if (!modelReady) {
            throw new IllegalStateException("Model has not been set with setCMLToRender or setModelToRender");
        } else if (!typesReady) {
            throw new IllegalStateException("Atom types have not been set with setAtomPropertiesFromFile");
        } else {
            throw new IllegalStateException("Serious Bug-a-roo! ready=false but I think we're ready!");
        }
    }

    public void componentHidden(java.awt.event.ComponentEvent e) {
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        displayIfAreWeReady();
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
        displayIfAreWeReady();
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
