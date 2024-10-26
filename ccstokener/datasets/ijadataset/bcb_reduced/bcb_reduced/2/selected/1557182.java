package de.fu.tracebook.core.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;
import android.os.AsyncTask;
import de.fu.tracebook.R;
import de.fu.tracebook.core.data.implementation.NewDBBug;
import de.fu.tracebook.core.overlays.BugOverlayItem;
import de.fu.tracebook.core.overlays.BugOverlayItem.BugType;
import de.fu.tracebook.gui.activity.MapsForgeActivity;
import de.fu.tracebook.util.LogIt;

/**
 * Implementation of {@link IBugManager}.
 */
public class NewBugManager implements IBugManager {

    /**
     * The list of OpenStreetBugs.
     */
    List<IDataBug> osbugs = new ArrayList<IDataBug>();

    /**
     * Add a user created bug.
     * 
     * @param bug
     *            The bug to add.
     */
    public void addBug(IDataBug bug) {
        NewDBBug newBug = new NewDBBug();
        newBug.description = bug.getDescription();
        newBug.point = bug.getPosition();
        newBug.track = StorageFactory.getStorage().getTrack().getName();
        newBug.insert();
    }

    /**
     * Will load Bugs from OpenStreetBugs. At least 100 Bugs are loaded. The
     * area is the current position +0.25 degrees in all directions.
     * 
     * @param activity
     *            The MapsForgeActivity which uses this BugManager.
     * @param pos
     *            The current position.
     */
    public void downloadBugs(final MapsForgeActivity activity, final GeoPoint pos) {
        if (pos == null) {
            return;
        }
        (new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                BufferedReader reader = null;
                Boolean ret = Boolean.FALSE;
                String osbUrl = "http://openstreetbugs.schokokeks.org/api/0.1/getBugs?b=" + (pos.getLatitude() - 0.25f) + "&t=" + (pos.getLatitude() + 0.25f) + "&l=" + (pos.getLongitude() - 0.25f) + "&r=" + (pos.getLongitude() + 0.25f);
                LogIt.d("Url is: " + osbUrl);
                if (MapsForgeActivity.isOnline(activity)) {
                    ret = Boolean.TRUE;
                    try {
                        URL url = new URL(osbUrl);
                        URLConnection conn = url.openConnection();
                        InputStream in = conn.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        osbugs.clear();
                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                            IDataBug b = extractBug(line);
                            if (b != null) {
                                osbugs.add(b);
                            }
                        }
                        LogIt.d("Found " + osbugs.size() + " bugs!");
                        activity.fillBugs();
                    } catch (IOException e) {
                        ret = Boolean.FALSE;
                        LogIt.e("Download error: " + e.getMessage());
                    } finally {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException e) {
                        }
                    }
                }
                return ret;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result.booleanValue()) {
                    LogIt.popup(activity, activity.getResources().getString(R.string.alert_mapsforgeactivity_faileddownload));
                }
            }
        }).execute();
    }

    /**
     * Get OverlayItems for all bugs.
     * 
     * @return A list of all OverlayItems for all Bugs.
     */
    public Collection<OverlayItem> getBugOverlays() {
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        List<NewDBBug> dbbugs = NewDBBug.getByTrack(StorageFactory.getStorage().getTrack().getName());
        for (NewDBBug bug : dbbugs) {
            items.add(new BugOverlayItem(new NewBug(bug), BugType.USERBUG));
        }
        for (IDataBug b : osbugs) {
            items.add(new BugOverlayItem(b, BugType.OPENSTREETBUG));
        }
        return items;
    }

    /**
     * Get OverlayItems for all bugs.
     * 
     * @return A list of all OverlayItems for all Bugs.
     */
    public List<IDataBug> getBugs() {
        ArrayList<IDataBug> items = new ArrayList<IDataBug>();
        List<NewDBBug> dbbugs = NewDBBug.getByTrack(StorageFactory.getStorage().getTrack().getName());
        for (NewDBBug bug : dbbugs) {
            items.add(new NewBug(bug));
        }
        for (IDataBug b : osbugs) {
            items.add(b);
        }
        return items;
    }

    /**
     * Remove a bug.
     * 
     * @param bug
     *            The bug to remove.
     */
    public void remove(IDataBug bug) {
        osbugs.remove(bug);
        bug.removeFromDb();
    }

    /**
     * Returns the number of user recorded bugs.
     * 
     * @return The size of the list of user recorded bugs.
     */
    public int size() {
        return NewDBBug.getByTrack(StorageFactory.getStorage().getTrack().getName()).size();
    }

    /**
     * Used for parsing the loaded OpenStreetBugs. Splits a line according to
     * the needs for parsing the lines.
     */
    private List<String> splitLine(String line) {
        List<String> splits = new LinkedList<String>();
        String tmp = line;
        for (int ind = tmp.indexOf(","); ind >= 0; ind = tmp.indexOf(",")) {
            splits.add(tmp.substring(0, ind));
            tmp = tmp.substring(ind + 1);
        }
        splits.add(tmp);
        return splits;
    }

    /**
     * Used for parsing the loaded OpenStreetBugs. Extracts a bug from a line.
     * 
     * @param line
     *            The line to parse.
     * @return The parsed bug.
     */
    IDataBug extractBug(String line) {
        String description = "";
        double longitude = 0;
        double latitude = 0;
        List<String> lines = splitLine(line);
        if (lines.size() >= 5) {
            StringBuilder desc = new StringBuilder();
            desc.append(lines.get(3));
            for (int i = 4; i < lines.size() - 1; ++i) {
                desc.append(lines.get(i));
            }
            description = desc.toString();
            longitude = Double.parseDouble(lines.get(1).trim());
            latitude = Double.parseDouble(lines.get(2).trim());
        }
        if (lines.get(lines.size() - 1).charAt(1) != '0') {
            return null;
        }
        return new NewBug(description, new GeoPoint(latitude, longitude));
    }
}
