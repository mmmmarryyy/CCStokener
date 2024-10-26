package de.iritgo.openmetix.webinstrument.gui;

import de.iritgo.openmetix.app.gui.InstrumentGUIPane;
import de.iritgo.openmetix.app.instrument.InstrumentDisplay;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.gui.GUIPane;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.framework.command.CommandTools;
import de.iritgo.openmetix.webinstrument.WebInstrument;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;

/**
 * This gui pane is used to display web instruments.
 *
 * @version $Id: WebInstrumentDisplay.java,v 1.1 2005/04/24 18:10:46 grappendorf Exp $
 */
public class WebInstrumentDisplay extends InstrumentGUIPane implements InstrumentDisplay, ActionListener {

    /** HTML display. */
    private JEditorPane html;

    /** Our context menu. */
    private JPopupMenu contextMenu;

    /** Timer. */
    private Timer timer;

    /**
	 * Create a new WebInstrumentDisplay.
	 */
    public WebInstrumentDisplay() {
        super("WebInstrumentDisplay");
    }

    /**
	 * Initialize the gui. Subclasses should override this method to create a
	 * custom gui.
	 */
    public void initGUI() {
        html = new JEditorPane();
        html.setEditable(false);
        JScrollPane sp = new JScrollPane(html);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        content.setLayout(new BorderLayout());
        content.add(sp, BorderLayout.CENTER);
    }

    /**
	 * Return a sample of the data object that is displayed in this gui pane.
	 *
	 * @return The sample oject.
	 */
    public IObject getSampleObject() {
        return new WebInstrument();
    }

    /**
	 * Return a clone of this gui pane.
	 *
	 * @return The gui pane clone.
	 */
    public GUIPane cloneGUIPane() {
        return new WebInstrumentDisplay();
    }

    /**
	 * Load the gui values from the data object attributes.
	 */
    public void loadFromObject() {
        WebInstrument webInstrument = (WebInstrument) iobject;
        if (webInstrument.isValid()) {
            return;
        }
        setTitle(webInstrument.getTitle());
        loadWebPage();
        if (timer != null) {
            timer.stop();
        }
        if (webInstrument.getReloadInterval() > 0) {
            timer = new Timer(1000 * webInstrument.getReloadInterval(), this);
            timer.start();
        }
        if (getDisplay().getProperty("metixReload") != null) {
            CommandTools.performSimple("StatusProgressStep");
            getDisplay().removeProperty("metixReload");
        }
    }

    /**
	 * Load the web page.
	 */
    private void loadWebPage() {
        final WebInstrument webInstrument = (WebInstrument) iobject;
        if (webInstrument.getUrl().length() > 0) {
            CommandTools.performAsync(new Command() {

                public void perform() {
                    try {
                        html.getDocument().putProperty(Document.StreamDescriptionProperty, null);
                        String urlSpecTmp = webInstrument.getUrl();
                        if (!urlSpecTmp.startsWith("http://") && !urlSpecTmp.startsWith("https://")) {
                            urlSpecTmp = "http://" + urlSpecTmp;
                        }
                        final String urlSpec = urlSpecTmp;
                        final URL url = new URL(urlSpec);
                        URLConnection urlConnection = url.openConnection();
                        if ("text/html".equals(urlConnection.getContentType())) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    try {
                                        html.setPage(new URL(urlSpec));
                                    } catch (Exception x) {
                                        html.setText(x.toString());
                                    }
                                }
                            });
                        } else {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    html.setContentType("text/html");
                                    html.setText("<html><body><img src=\"" + urlSpec + "\"></body></html>");
                                }
                            });
                        }
                    } catch (final Exception x) {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                html.setText(x.toString());
                            }
                        });
                    }
                }
            });
        }
    }

    /**
	 * Called to reload the web page.
	 *
	 * @param e The action event.
	 */
    public void actionPerformed(ActionEvent e) {
        loadWebPage();
    }

    /**
	 * Close the display.
	 */
    public void systemClose() {
        if (timer != null) {
            timer.stop();
        }
        super.close();
    }

    /**
	 * This method is called when the gui pane starts waiting
	 * for the attributes of it's iobject.
	 */
    public void waitingForNewObject() {
        setConfigured(false);
    }

    /**
	 * This method receives sensor measurements.
	 *
	 * The measurement values are sent from the server to the client
	 * instrument displays. A display should check if it really displays
	 * measurments for the given sensor. In this case it should
	 * update itself accordingly to the measurement values.
	 *
	 * @param timestamp The timestamp on which the measurement was done.
	 * @param value The measurement value.
	 * @param stationId The id of the gaging station.
	 * @param sensorId The id of the gaging sensor.
	 */
    public void receiveSensorValue(Timestamp timestamp, double value, long stationId, long sensorId) {
    }

    /**
	 * This method receives historical sensor measurements.
	 *
	 * The measurement values are sent from the server to the client
	 * instrument displays. A display should check if it really displays
	 * measurments for the given sensor. In this case it should
	 * update itself accordingly to the measurement values.
	 *
	 * @param timestamp The timestamp on which the measurement was done.
	 * @param value The measurement value.
	 * @param stationId The id of the gaging station.
	 * @param sensorId The id of the gaging sensor.
	 */
    public void receiveHistoricalSensorValue(long instumentUniqueId, Timestamp timestamp, double value, long stationId, long sensorId) {
    }

    /**
	 * Configure the display new.
	 */
    public void configureDisplay() {
    }

    /**
	 * Check wether this display is editable or not.
	 *
	 * @return True if the display is editable.
	 */
    public boolean isEditable() {
        return true;
    }

    /**
	 * Check wether this display is printable or not.
	 *
	 * @return True if the display is printable.
	 */
    public boolean isPrintable() {
        return false;
    }

    /**
	 * Print the display.
	 */
    public void print() {
    }
}
