package net.claribole.zgrviewer;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;
import fr.inria.zvtm.engine.SwingWorker;
import fr.inria.zvtm.engine.VirtualSpaceManager;
import fr.inria.zvtm.svg.SVGReader;
import fr.inria.zvtm.glyphs.VText;
import org.w3c.dom.Document;

class GVLoader {

    Object application;

    GraphicsManager grMngr;

    ConfigManager cfgMngr;

    DOTManager dotMngr;

    GVLoader(Object app, GraphicsManager gm, ConfigManager cm, DOTManager dm) {
        this.application = app;
        this.grMngr = gm;
        this.cfgMngr = cm;
        this.dotMngr = dm;
    }

    void open(short prg, boolean parser) {
        if (ConfigManager.checkProgram(prg)) {
            openDOTFile(prg, parser);
        } else {
            Object[] options = { "Yes", "No" };
            int option = JOptionPane.showOptionDialog(null, ConfigManager.getDirStatus(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (option == JOptionPane.OK_OPTION) {
                openDOTFile(prg, parser);
            }
        }
    }

    void openDOTFile(final short prg, final boolean parser) {
        final JFileChooser fc = new JFileChooser(ConfigManager.m_LastDir != null ? ConfigManager.m_LastDir : ConfigManager.m_PrjDir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Find DOT File");
        int returnVal = fc.showOpenDialog(grMngr.mainView.getFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final SwingWorker worker = new SwingWorker() {

                public Object construct() {
                    grMngr.reset();
                    loadFile(fc.getSelectedFile(), prg, parser);
                    return null;
                }
            };
            worker.start();
        }
    }

    void openSVGFile() {
        final JFileChooser fc = new JFileChooser(ConfigManager.m_LastDir != null ? ConfigManager.m_LastDir : ConfigManager.m_PrjDir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Find SVG File");
        int returnVal = fc.showOpenDialog(grMngr.mainView.getFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final SwingWorker worker = new SwingWorker() {

                public Object construct() {
                    grMngr.reset();
                    loadSVG(fc.getSelectedFile());
                    return null;
                }
            };
            worker.start();
        }
    }

    void openOther() {
        new CallBox((ZGRViewer) application, grMngr);
    }

    void loadFile(File f, short prg, boolean parser) {
        if (f.exists()) {
            ConfigManager.m_LastDir = f.getParentFile();
            cfgMngr.lastFileOpened = f;
            dotMngr.lastProgramUsed = prg;
            if (grMngr.mainView.isBlank() == null) {
                grMngr.mainView.setBlank(cfgMngr.backgroundColor);
            }
            dotMngr.load(f, prg, parser);
            ConfigManager.defaultFont = VText.getMainFont();
            grMngr.mainView.setTitle(ConfigManager.MAIN_TITLE + " - " + f.getAbsolutePath());
            grMngr.reveal();
            if (grMngr.previousLocations.size() == 1) {
                grMngr.previousLocations.removeElementAt(0);
            }
            if (grMngr.rView != null) {
                grMngr.rView.getGlobalView(grMngr.mSpace.getCamera(1), 100);
                grMngr.cameraMoved(null, null, 0);
            }
        }
    }

    void loadSVG(File f) {
        grMngr.gp.setMessage("Parsing SVG...");
        grMngr.gp.setProgress(10);
        grMngr.gp.setVisible(true);
        try {
            grMngr.gp.setProgress(30);
            cfgMngr.lastFileOpened = f;
            dotMngr.lastProgramUsed = DOTManager.SVG_FILE;
            Document svgDoc = f.getName().toLowerCase().endsWith(".svgz") ? Utils.parse(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))), false) : Utils.parse(f, false);
            grMngr.gp.setMessage("Building graph...");
            grMngr.gp.setProgress(80);
            if (grMngr.mainView.isBlank() == null) {
                grMngr.mainView.setBlank(cfgMngr.backgroundColor);
            }
            SVGReader.load(svgDoc, grMngr.mSpace, true, f.toURI().toURL().toString());
            grMngr.seekBoundingBox();
            grMngr.buildLogicalStructure();
            ConfigManager.defaultFont = VText.getMainFont();
            grMngr.mainView.setTitle(ConfigManager.MAIN_TITLE + " - " + f.getAbsolutePath());
            grMngr.reveal();
            if (grMngr.previousLocations.size() == 1) {
                grMngr.previousLocations.removeElementAt(0);
            }
            if (grMngr.rView != null) {
                grMngr.rView.getGlobalView(grMngr.mSpace.getCamera(1), 100);
                grMngr.cameraMoved(null, null, 0);
            }
            grMngr.gp.setVisible(false);
        } catch (Exception ex) {
            grMngr.reveal();
            grMngr.gp.setVisible(false);
            ex.printStackTrace();
            JOptionPane.showMessageDialog(grMngr.mainView.getFrame(), Messages.loadError + f.toString());
        }
    }

    /** Method used by ZGRViewer - Applet to get the server-side generated SVG file.
     * Adds acceptance of gzip encoding in request and handles response with gzip
     * encoding. (i.e. SVGZ format).
     */
    void loadSVG(String svgFileURL) {
        try {
            URL url = new URL(svgFileURL);
            URLConnection c = url.openConnection();
            c.setRequestProperty("Accept-Encoding", "gzip");
            InputStream is = c.getInputStream();
            String encoding = c.getContentEncoding();
            if ("gzip".equals(encoding) || "x-gzip".equals(encoding) || svgFileURL.toLowerCase().endsWith(".svgz")) {
                is = new GZIPInputStream(is);
            }
            is = new BufferedInputStream(is);
            Document svgDoc = AppletUtils.parse(is, false);
            if (svgDoc != null) {
                if (grMngr.mainView.isBlank() == null) {
                    grMngr.mainView.setBlank(cfgMngr.backgroundColor);
                }
                SVGReader.load(svgDoc, grMngr.mSpace, true, svgFileURL);
                grMngr.seekBoundingBox();
                grMngr.buildLogicalStructure();
                ConfigManager.defaultFont = VText.getMainFont();
                grMngr.reveal();
                if (grMngr.previousLocations.size() == 1) {
                    grMngr.previousLocations.removeElementAt(0);
                }
                if (grMngr.rView != null) {
                    grMngr.rView.getGlobalView(grMngr.mSpace.getCamera(1), 100);
                }
                grMngr.cameraMoved(null, null, 0);
            } else {
                System.err.println("An error occured while loading file " + svgFileURL);
            }
        } catch (Exception ex) {
            grMngr.reveal();
            ex.printStackTrace();
        }
    }

    void load(String commandLine, String sourceFile) {
        grMngr.reset();
        dotMngr.loadCustom(sourceFile, commandLine);
        ConfigManager.defaultFont = VText.getMainFont();
        grMngr.mainView.setTitle(ConfigManager.MAIN_TITLE + " - " + sourceFile);
        grMngr.reveal();
        if (grMngr.previousLocations.size() == 1) {
            grMngr.previousLocations.removeElementAt(0);
        }
        if (grMngr.rView != null) {
            grMngr.rView.getGlobalView(grMngr.mSpace.getCamera(1), 100);
            grMngr.cameraMoved(null, null, 0);
        }
    }

    void reloadFile() {
        if (cfgMngr.lastFileOpened != null) {
            grMngr.reset();
            if (dotMngr.lastProgramUsed == DOTManager.SVG_FILE) {
                this.loadSVG(cfgMngr.lastFileOpened);
            } else {
                this.loadFile(cfgMngr.lastFileOpened, dotMngr.lastProgramUsed, false);
            }
        }
    }
}
