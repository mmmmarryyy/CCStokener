package de.hpi.eworld.importer;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import org.apache.log4j.Logger;
import de.hpi.eworld.core.ModelManager;
import de.hpi.eworld.observer.NotificationType;
import de.hpi.eworld.observer.ObserverNotification;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Thomas Beyhl, Jonas Truemper
 * 
 */
public class OsmThread extends Observable implements Runnable, Observer {

    /**
	 * the path to import the map from
	 */
    private final String path;

    /**
	 * Checkbox for pedestrian way importing
	 */
    private final boolean pedestrian;

    /**
	 * Checkbox for filtering cyclic edges
	 */
    private final boolean filterCyclic;

    /**
	 * Checkbox for filtering duplicate edges
	 */
    private final boolean filterDuplicateEdges;

    /**
	 * the osm xml handler
	 */
    private Osm2Model osm;

    /**
	 * the osm permalink url
	 */
    public static final String permalink = "http://www.openstreetmap.org/api/0.6/map?bbox=";

    /**
	 * @author Thomas Beyhl, Jonas Truemper
	 * @param fileName
	 *            the maps filename
	 * @param pedestrian
	 *            true if pedestrian ways shall be imported
	 * @param filterCyclic
	 *            true if cyclic edges shall be imported
	 */
    public OsmThread(final String path, final boolean pedestrian, final boolean filterCyclic, final boolean filterDuplicateEdges) {
        super();
        this.path = path;
        this.pedestrian = pedestrian;
        this.filterCyclic = filterCyclic;
        this.filterDuplicateEdges = filterDuplicateEdges;
    }

    /**
	 * @author Thomas Beyhl, Jonas Truemper
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        InputStream inputStream = null;
        int length = -1;
        if (path.contains(permalink)) {
            URL url = null;
            try {
                url = new URL(path);
            } catch (final MalformedURLException e) {
                stopImport("Url isn't a permalink!");
                return;
            }
            try {
                final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                inputStream = con.getInputStream();
                length = con.getContentLength();
            } catch (final IOException e) {
                stopImport("Couldn't open http connection!\n" + e.getMessage());
                return;
            }
        } else {
            try {
                inputStream = new FileInputStream(path);
            } catch (final FileNotFoundException e) {
                stopImport("File doesn't exist!");
                return;
            }
        }
        try {
            final ModelManager modelManager = ModelManager.getInstance();
            modelManager.setChanged();
            modelManager.notifyObservers(new ObserverNotification(NotificationType.startBatchProcess));
            modelManager.clearChanged();
            osm = new Osm2Model(pedestrian, filterCyclic);
            osm.addObserver(this);
            boolean missingData = false;
            if (length >= 0) {
                osm.parseFile(inputStream, length);
            } else {
                missingData = osm.parseFile(inputStream);
            }
            setChanged();
            if (osm.somethingImported()) {
                this.notifyObservers(new ObserverNotification(NotificationType.done, null));
            } else {
                this.notifyObservers(new ObserverNotification(NotificationType.nothing, null));
            }
            if (!osm.wasInterrupted()) {
                modelManager.setChanged();
                modelManager.notifyObservers(new ObserverNotification(NotificationType.endBatchProcess, new Boolean(filterDuplicateEdges), new Boolean(missingData)));
                modelManager.clearChanged();
            }
            clearChanged();
        } catch (final Exception e) {
            Logger.getLogger(this.getClass()).error("Error occured during OSM file import", e);
            this.notifyObservers(new ObserverNotification(NotificationType.failed));
        } finally {
            clearChanged();
        }
    }

    /**
	 * Stops the import by calling the handlers interrupt method.
	 * 
	 * @see Osm2Model#interrupt()
	 */
    public void stopImport(final String failure) {
        Logger.getLogger(this.getClass()).warn(failure);
        if (osm != null) {
            osm.interrupt();
        }
        setChanged();
        this.notifyObservers(new ObserverNotification(NotificationType.failed, failure));
        clearChanged();
    }

    /**
	 * Updates the osm thread and therefore notifies all its observers
	 * 
	 * @author Thomas Beyhl
	 */
    public synchronized void update(final Observable o, final Object arg) {
        setChanged();
        this.notifyObservers(arg);
        clearChanged();
    }
}
