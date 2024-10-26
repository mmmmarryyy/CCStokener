package de.hpi.eworld.importer.osm;

import java.net.HttpURLConnection;
import java.net.URL;
import com.trolltech.qt.QSignalEmitter;

/**
 * @author Christian Holz, Marco Helmich
 *
 */
public class OsmThread extends QSignalEmitter implements Runnable {

    private URL url;

    /**
	 * Checkbox for pedestrian way importing
	 */
    private boolean pedestrian;

    /**
	 * Checkbox for filtering cyclic edges
	 */
    private boolean filterCyclic;

    /**
	 * done signal
	 * @author Christian Holz
	 */
    public Signal0 done = new Signal0();

    /**
	 * failed signal
	 * @author Christian Holz
	 */
    public Signal0 failed = new Signal0();

    /**
	 * empty signal
	 * @author Christian Holz
	 */
    public Signal0 nothing = new Signal0();

    /**
	 * for progress indication
	 */
    public Signal1<Integer> progress = new Signal1<Integer>();

    /**
	 * @author Christian Holz
	 * @param url
	 */
    public OsmThread(URL url, boolean pedestrian, boolean filterCyclic) {
        this.url = url;
        this.pedestrian = pedestrian;
        this.filterCyclic = filterCyclic;
    }

    /**
	 * @author Christian Holz
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Osm2Model osm = new Osm2Model(pedestrian, filterCyclic);
            osm.progress.connect(this, "progress(int)");
            osm.parseFile(con.getInputStream(), con.getContentLength());
            if (osm.somethingImported()) {
                done.emit();
            } else {
                nothing.emit();
            }
        } catch (Exception e) {
            failed.emit();
        }
    }

    /**
	 * @author Christian Holz
	 * @param progress
	 */
    @SuppressWarnings("unused")
    private void progress(int progress) {
        this.progress.emit(progress);
    }
}
