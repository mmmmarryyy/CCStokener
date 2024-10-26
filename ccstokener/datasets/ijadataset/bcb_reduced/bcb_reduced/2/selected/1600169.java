package ca.theplanet.phoneplanwatcher.gather;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

/**
 * GatherServiceImpl gathers the data (minutes, bytes, etc) that is used on
 * phone plan watcher.
 * 
 */
public class GatherServiceImpl {

    private static final String TAG = "gatherserviceimpl";

    /**
	 * Read the number of bytes (sent and received) since boot on the GSM modem.
	 * I *think* that is any device called rmnet0, etc.
	 * 
	 * @return
	 */
    private static long getCumulativeBytes() {
        final File file = new File("/proc/net/dev");
        final Hashtable<String, ArrayList<Long>> table = new Hashtable<String, ArrayList<Long>>();
        try {
            final FileReader reader = new FileReader(file);
            final BufferedReader bufReader = new BufferedReader(reader, 256);
            while (true) {
                final String line = bufReader.readLine();
                if (line == null) break;
                final int colon = line.indexOf(':');
                if (colon > 0) {
                    final ArrayList<Long> numbers = new ArrayList<Long>();
                    final String iface = line.substring(0, colon).trim();
                    final String theRest = line.substring(colon + 1);
                    StringBuilder b = new StringBuilder();
                    for (final char c : theRest.toCharArray()) {
                        if (Character.isDigit(c)) {
                            b.append(c);
                        } else {
                            if (b.length() > 0) {
                                final Long l = Long.parseLong(b.toString());
                                numbers.add(l);
                                b = new StringBuilder();
                            }
                        }
                    }
                    table.put(iface, numbers);
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        long bytes = 0;
        for (final String key : table.keySet()) {
            if (key.startsWith("rmnet")) {
                final Long recv = table.get(key).get(0);
                final Long send = table.get(key).get(8);
                bytes = bytes + recv;
                bytes = bytes + send;
            }
        }
        return bytes;
    }

    private final Context context;

    private final Storage storage;

    /**
	 * Setup the GatherServiceImpl.
	 * 
	 * @param bootup
	 *            will be true if the phone has just recently booted up.
	 */
    public GatherServiceImpl(final Context context, final Storage storage, final boolean bootup) {
        this.storage = storage;
        this.context = context;
        if (bootup) {
            final long count = GatherServiceImpl.getCumulativeBytes();
            storage.setCumulativeBytesRecorded(count);
            storage.incrementDailyTotal(count);
        }
    }

    private void checkForUpgrade() {
        final SharedPreferences prefs = this.context.getSharedPreferences(GatherService.class.getName(), Context.MODE_PRIVATE);
        final long nextUpgradeCheck = prefs.getLong("nextUpgradeCheck", -1);
        if (System.currentTimeMillis() > nextUpgradeCheck) {
            PackageInfo pi;
            try {
                pi = this.context.getPackageManager().getPackageInfo("ca.theplanet.phoneplanwatcher", 0);
            } catch (final NameNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            final String currentVersion = pi.versionName;
            try {
                final URL url = new URL("http://www.theplanet.ca/phoneplanwatcher.php?version=" + currentVersion);
                final URLConnection con = url.openConnection();
                final InputStream input = con.getInputStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(input), 30);
                final String line = reader.readLine();
                if (line != null && line.trim().length() > 0) {
                    this.storage.setPreference("upgradeMarketId", line.trim());
                } else {
                    this.storage.setPreference("upgradeMarketId", null);
                }
            } catch (final Exception e) {
                Log.e(GatherServiceImpl.TAG, "Couldn't check for upgrades", e);
            }
            final Editor editor = prefs.edit();
            editor.putLong("nextUpgradeCheck", System.currentTimeMillis() + 604800000);
            editor.commit();
        }
    }

    private void countMinutesUsed() {
        final long currentTime = System.currentTimeMillis() - 1;
        long lastTimeMinutesWasChecked = this.storage.getLastTimeMinutesWasChecked();
        if (lastTimeMinutesWasChecked < 1) {
            final long sixtyDaysInMilliseconds = 60 * 24 * 60 * 60 * 1000;
            lastTimeMinutesWasChecked = currentTime - sixtyDaysInMilliseconds;
        }
        final ContentResolver resolver = this.context.getContentResolver();
        final String[] projection = { CallLog.Calls.DATE, CallLog.Calls.DURATION };
        final String[] selectionArgs = { Long.toString(lastTimeMinutesWasChecked), Long.toString(currentTime) };
        final Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, projection, CallLog.Calls.DATE + " > ? AND " + CallLog.Calls.DATE + " < ?", selectionArgs, CallLog.Calls.DATE);
        final HashMap<Long, Long> dates = new HashMap<Long, Long>();
        try {
            while (cursor.moveToNext()) {
                final long date = cursor.getLong(0);
                final long sec = cursor.getLong(1);
                final long dateForBucket = getDayAndHourFromDate(date);
                if (dates.containsKey(dateForBucket)) {
                    final long currentAmount = dates.get(dateForBucket);
                    dates.put(dateForBucket, currentAmount + sec);
                } else {
                    dates.put(dateForBucket, sec);
                }
            }
        } finally {
            cursor.close();
        }
        final SimpleDateFormat formatter = new SimpleDateFormat();
        for (final Long date : dates.keySet()) {
            final StringBuilder b = new StringBuilder();
            b.append(formatter.format(new Date(date)));
            b.append(" - ");
            b.append(dates.get(date));
            b.append("s");
            Log.d(GatherServiceImpl.TAG, b.toString());
            this.storage.setTotalMinutes(date, dates.get(date));
        }
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(currentTime);
        cal.add(Calendar.HOUR_OF_DAY, -1);
        cal.set(Calendar.MINUTE, 0);
        final long nextStartTime = cal.getTimeInMillis();
        this.storage.setLastTimeMinutesWasChecked(nextStartTime);
    }

    /**
	 * Do the actual gathering of data. This will be called once every two hours
	 * if the user is not actively using the program or else one ever 5 seconds
	 * if the user is currently looking at the data.
	 */
    public void doGathering() {
        Log.i(GatherServiceImpl.TAG, "checking ................");
        incrementBytes();
        countMinutesUsed();
        checkForUpgrade();
    }

    private long getDayAndHourFromDate(final long date) {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void incrementBytes() {
        final long cumulativeBytes = GatherServiceImpl.getCumulativeBytes();
        final long bytesRecorded = this.storage.getCumulativeBytesRecorded();
        final long increment = cumulativeBytes - bytesRecorded;
        Log.d(GatherServiceImpl.TAG, "cumulativeBytes=" + cumulativeBytes + ", bytesRecorded=" + bytesRecorded + ", increment=" + increment);
        this.storage.setCumulativeBytesRecorded(cumulativeBytes);
        this.storage.incrementDailyTotal(increment);
    }
}
