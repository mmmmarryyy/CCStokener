package com.gnizr.core.web.clustermap;

import info.aduna.clustermap.ClusterMapMediator;
import info.aduna.clustermap.ClusterMapMediatorListener;
import info.aduna.clustermap.ClusterMapUI;
import info.aduna.clustermap.DefaultObject;
import info.aduna.clustermap.GraphPanel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * <p>An applet version of the Aduna Clustermap application (2006.3-beta2). This implementation
 * wraps the core functions provided by the Clustermap application. Some functions such 
 * as opening a URL link can't be directly ported into a browser environment. This class overrides
 * those functions.</p>
 * <p>To use this applet, the <code>APPLET</code> in the client HTML page must defined a 
 * <code>PARAM</code> value called <code>dataSourceUrl</code>.
 * </p> 
 * <p>
 * <pre>
 * &ltapplet code="com.gnizr.core.web.clustermap.ClusterMapApplet.class" 
 *      codebase="/gnizr/applets"
 *      archive="gnizr-clustermap-applet.jar"
 *       width="100" height="100"&gt
 *     &ltparam name=datasourceurl value="[url-points-to-a-clustermap-data-XML-document]"
 * &lt/applet&gt
 * </pre>
 * </P>
 * @author Harry Chen
 *
 */
public class ClusterMapApplet extends JApplet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 477476797909541563L;

    private static final Logger logger = Logger.getLogger(ClusterMapApplet.class);

    /**
	 * Applet paramater name <code>dataSourceUrl</code>, which defines the
	 * URL of an XML document that defines the data model of this
	 * <code>ClusterMapApplet</code>
	 */
    public static final String DATA_SOURCE_URL = "dataSourceUrl";

    private ClusterMapMediator mediator;

    private ClusterMapUI clusterMapGui;

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
	 * Initializes the applet UI
	 */
    @Override
    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            logger.error("createGUI didn't successfully complete");
        }
    }

    private ProgressMonitor progressMonitor;

    private void createGUI() {
        clusterMapGui = new ClusterMapUI();
        mediator = clusterMapGui.getMediator();
        registerDoubleClickListener(mediator);
        add(clusterMapGui);
        progressMonitor = new ProgressMonitor(this.getRootPane(), "Building Clustermap", "", 0, 3);
        progressMonitor.setMillisToPopup(0);
        progressMonitor.setMillisToDecideToPopup(0);
        progressMonitor.setNote("Fetching data... Please wait...");
        progressMonitor.setProgress(1);
        getContentPane().setEnabled(false);
        mediator.addClusterMapMediatorListener(new ClusterMapMediatorListener() {

            public void classificationTreeChanged(ClusterMapMediator cmm) {
                progressMonitor.setProgress(3);
                getContentPane().setEnabled(true);
            }

            public void propertyChanged(String arg0, Object arg1, ClusterMapMediator arg2) {
            }

            public void visualisedClassesChanged(ClusterMapMediator arg0) {
            }
        });
    }

    private void registerDoubleClickListener(ClusterMapMediator mediator) {
        final GraphPanel panel = mediator.getGraphPanel();
        MouseListener[] listeners = panel.getMouseListeners();
        if (listeners != null && listeners.length == 2) panel.removeMouseListener(listeners[1]);
        panel.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int x = e.getX();
                    int y = e.getY();
                    Object obj = panel.resolveObject(x, y);
                    if (obj != null && (obj instanceof DefaultObject)) {
                        DefaultObject defObj = (DefaultObject) obj;
                        if (defObj.getLocation() != null) {
                            URL url;
                            try {
                                url = new URL(defObj.getLocation());
                                getAppletContext().showDocument(url, "_blank");
                            } catch (MalformedURLException e1) {
                                logger.error(e1);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
	 * Load clustermap data model XML from the defined URL
	 */
    @Override
    public void start() {
        final String xmlDataSource = getParameter(DATA_SOURCE_URL);
        logger.debug("PARAM: " + DATA_SOURCE_URL + "=" + xmlDataSource);
        if (xmlDataSource != null) {
            try {
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        reloadData(xmlDataSource);
                    }
                });
                t.start();
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    private void reloadData(String dataSourceUrl) {
        try {
            URL url = new URL(dataSourceUrl);
            InputStream is = url.openStream();
            if (progressMonitor.isCanceled() == false) {
                progressMonitor.setNote("Building classifications...");
                progressMonitor.setProgress(2);
                mediator.loadClassificationTree(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Disposes the UI of Clustermap
	 */
    @Override
    public void stop() {
        super.stop();
        clusterMapGui.dispose();
    }
}
