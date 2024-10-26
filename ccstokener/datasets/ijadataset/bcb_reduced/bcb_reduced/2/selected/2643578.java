package de.hpi.eworld.extensions.data.osm;

import java.net.HttpURLConnection;
import java.net.URL;
import com.trolltech.qt.QSignalEmitter;
import de.hpi.eworld.gui.eventlisteners.MainApp;

/**
 * @author Christian Holz
 *
 */
public class OSMThread extends QSignalEmitter implements Runnable {

    private URL url;

    private MainApp app;

    /**
	 * 
	 * @author Christian Holz
	 */
    public Signal0 done = new Signal0();

    /**
	 * 
	 * @author Christian Holz
	 */
    public Signal0 failed = new Signal0();

    /**
	 * 
	 */
    public Signal1<Integer> progress = new Signal1<Integer>();

    /**
	 * @author Christian Holz
	 * @param url
	 */
    public OSMThread(URL url, MainApp app) {
        this.url = url;
        this.app = app;
    }

    /**
	 * @author Christian Holz
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Osm2Model osm = new Osm2Model();
            osm.progress.connect(this, "progress(int)");
            osm.newEdge.connect(app, "addEdgeItem(Edge)");
            osm.newNode.connect(app, "addNodeItem(Node)");
            osm.newPOI.connect(app, "addPOIItem(PointOfInterest)");
            osm.parseFile(con.getInputStream(), con.getContentLength());
            done.emit();
        } catch (Exception e) {
            failed.emit();
        }
    }

    /**
	 * @author Christian Holz
	 * @param progress
	 */
    private void progress(int progress) {
        this.progress.emit(progress);
    }
}
