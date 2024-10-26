package seismosurfer.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import seismosurfer.data.CatalogData;
import seismosurfer.data.DuplicateData;
import seismosurfer.data.QuakeData;
import seismosurfer.data.constants.UpdateConstants;
import seismosurfer.database.CatalogDAO;
import seismosurfer.database.QuakeDAO;
import seismosurfer.util.Assert;
import seismosurfer.util.SeismoException;
import seismosurfer.util.Util;
import com.bbn.openmap.util.Debug;

/**
 * The base class that handles the actual database update.
 * Contains a factory method to instantiate the correct
 * implementation for a given catalog and implements
 * common operations for its subclasses.
 *
 */
public abstract class Updater implements UpdateConstants {

    protected List data = new ArrayList(1000);

    protected List filtered = new ArrayList(1000);

    protected CatalogData catalog;

    protected CatalogDAO catalogDAO = new CatalogDAO();

    public Updater(int catalogID) {
        catalog = catalogDAO.getCatalog(catalogID);
        Assert.notNull(catalog);
    }

    /**
     * Defines the skeleton of the algorithm for
     * the db update.
     * Reads data form a catalog, 
     * filters them to hold only the needed
     * data, checks them for any duplicate records
     * abd finally inserts them in the db.
     *
     */
    public void doUpdate() {
        read();
        parse();
        for (Iterator iter = filtered.iterator(); iter.hasNext(); ) {
            QuakeData item = (QuakeData) iter.next();
            if (check(item)) {
                insert(item);
            }
        }
        filtered.clear();
    }

    /**
     * Filters the data to keep only the needed
     * pieces. Empty lines, lines with fewer
     * than expected tokens, data not in the
     * expected format, lines without Ms magnitude
     * are all ignored.
     *
     */
    protected abstract void parse();

    /**
     * Inserts the quake data in the database.
     * 
     * @param qd the QuakeData to be inserted in the db
     */
    protected abstract void insert(QuakeData qd);

    /**
     * A factory method that selects an implementation
     * of Updater (subclass) that is appropriate
     * for a given catalog and creates an instance of it.
     * 
     * @param catalogID the id of a catalog
     * @return an Updater subclass instance
     */
    public static Updater makeUpdater(int catalogID) {
        CatalogDAO dao = new CatalogDAO();
        String source = dao.getCatalogSource(catalogID);
        if (source.equalsIgnoreCase(GI_NOA_SOURCE_NAME)) {
            return new GINOAUpdater(catalogID);
        }
        if (source.equalsIgnoreCase(NEIC_SOURCE_NAME)) {
            String catName = dao.getCatalogName(catalogID);
            if (catName.equalsIgnoreCase(PDE_CATALOG)) {
                return new PDEUpdater(catalogID);
            }
            return new NEICUpdater(catalogID);
        }
        throw new SeismoException("Catalog has no source associated with it.");
    }

    /**
     * Create a BufferedReader for {@link #catalog this} catalog`s 
     * url.
     * 
     * @return a BufferedReader object
     */
    protected BufferedReader getDataReader() {
        try {
            URL url = new URL(this.catalog.getCatalogURL());
            Debug.output("Catalog URL:" + url.toString());
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException ex) {
            throw new SeismoException(ex);
        }
    }

    /**
     * Reads all the data from a catalog into 
     * a list in memory.
     *
     */
    protected void read() {
        String line;
        BufferedReader in = null;
        try {
            in = getDataReader();
            while ((line = in.readLine()) != null) {
                data.add(line);
            }
        } catch (IOException e) {
            throw new SeismoException(e);
        } finally {
            if (in != null) Util.close(in);
        }
    }

    /**
     * Checks a line if it`s empty.
     * 
     * @param line the String that contains a line of text
     * @return true if it`s empty
     */
    protected boolean empty(String line) {
        return (line.trim().length() == 0);
    }

    /**
     * Checks a line if it`s empty and if it is returns
     * the default value.
     * 
     * @param text the String that contains a line of text
     * @param defaultValue the String that contains the default value
     * @return The given text if it`s not empty, the default balue
     *         if it is.
     */
    protected String empty(String text, String defaultValue) {
        if (empty(text)) {
            return defaultValue;
        }
        return text;
    }

    /**
     * Checks the database for duplicate data
     * and determines if the given quake data
     * can be inserted in the database.
     * <p>
     * If it doesn`t find duplicate data then
     * the new record can be inserted in the db.
     * If it finds duplicate records then:
     * <ul>
     *   <li>if the duplicate record contains macroseismic
     *   data, the new record won`t be inserted.
     *   <li>if the duplicate record contains documents
     *   , the new record won`t be inserted. 
     *   <li>if the catalog from which the duplicate record
     *   was originated has higher or equal priority
     *   to the new record then the old record will
     *   be deleted and the new record will be inserted.
     *   <li>if the catalog from which the duplicate record
     *   was originated has lower priority
     *   to the new record then the new record won`t be inserted.
     *  </ul>
     * 
     * @param quake the QuakeData to be checked
     * @return true if the given quake data can be inserted in the db
     */
    protected boolean check(QuakeData quake) {
        Debug.output(quake.toString());
        QuakeDAO dao = new QuakeDAO();
        int catCount = this.catalogDAO.getCatalogCount();
        int catalogPriority = this.catalogDAO.getCatalogPriority(quake.getCatalogID());
        if (catCount == 0) {
            throw new SeismoException("The CATALOG table is empty. Make sure it has the correct entries.");
        }
        List result = dao.findDuplicates(quake);
        int size = result.size();
        if (size != 0) {
            if (Debug.debugging("checkQuake")) {
                Debug.output("We have " + size + " duplicate rows!!!");
                for (int k = 0; k < size; k++) {
                    DuplicateData d = (DuplicateData) result.get(k);
                    Debug.output("Duplicate Quake : " + d.getQuakeID() + " from catalog : " + d.getCatalogID());
                }
            }
            for (int i = 0; i < size; i++) {
                DuplicateData d = (DuplicateData) result.get(i);
                if (d.getMacro()) {
                    return false;
                }
                if (d.getInfo()) {
                    return false;
                }
                int duplCatalogPriority = this.catalogDAO.getCatalogPriority(d.getCatalogID());
                if (catalogPriority <= duplCatalogPriority) {
                    dao.deleteQuake(d.getQuakeID());
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
