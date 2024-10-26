package org.mbari.vars.annotation.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mbari.movie.Timecode;

/**
 * <p>
 * Reads the camera log. Use as:
 * </p>
 *
 * <pre>
 * CameraLogInput ci = new CameraLogInput(&quot;file:/somefile.dat&quot;);
 * ci.read();
 * </pre>
 *
 * <h2><u>License </u></h2>
 * <p>
 * <font size="-1" color="#336699"> <a href="http://www.mbari.org"> The Monterey
 * Bay Aquarium Research Institute (MBARI) </a> provides this documentation and
 * code &quot;as is&quot;, with no warranty, express or implied, of its quality
 * or consistency. It is provided without support and without obligation on the
 * part of MBARI to assist in its use, correction, modification, or enhancement.
 * This information should not be published or distributed to third parties
 * without specific written permission from MBARI. </font>
 * </p>
 * <p>
 * <font size="-1" color="#336699">Copyright 2003 MBARI. MBARI Proprietary
 * Information. All rights reserved. </font>
 * </p>
 *
 *
 *@author     <a href="http://www.mbari.org">MBARI </a>
 *@version    $Id: CameraLogInput.java 332 2006-08-01 18:38:46Z hohonuuli $
 */
public class CameraLogInput {

    /**
     *  Description of the Field
     */
    public static final FilenameFilter LOG_FILE_FILTER = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return LOG_FILE_PATTERN.matcher(name).matches();
        }
    };

    /**
     * The first group in this pattern will return the year, the second group
     * will return the year day, the third will return the vehicle ID.
     */
    public static final Pattern LOG_FILE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-(\\d\\d\\d)(V|T|t|v).log");

    private static final long TIME_FOR_BREAK_IN_LOG = 3200000L;

    private static final Logger log = LoggerFactory.getLogger("vars.annotation");

    private static final Comparator tcComparator = new Comparator() {

        public int compare(Object o1, Object o2) {
            final CameraLogRecord r1 = (CameraLogRecord) o1;
            final CameraLogRecord r2 = (CameraLogRecord) o2;
            final Timecode t1 = r1.getTimeCode();
            final Timecode t2 = r2.getTimeCode();
            return t1.compareTo(t2);
        }
    };

    /**
	 * @uml.property  name="records"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="org.mbari.vars.annotation.io.CameraLogRecord"
	 */
    private final List records = new ArrayList(5000);

    /**
	 * @uml.property  name="url"
	 */
    private final URL url;

    /**
     * Constructor for the CameraLogInput object
     *
     * @param  url  The camera-log to be read.
     */
    public CameraLogInput(final URL url) {
        this.url = url;
    }

    /**
     * Checks that 2 logs contain a dive that rolls over midnight GMT. The logs
     * should be sorted by epoch.
     *
     *
     * @param  a              The first log in the sequence
     * @param  b              The second log in the sequence
     * @return    true if the logs contain info about the same dive. false
     *         otherwise.
     */
    public static final boolean checkForContinuation(final CameraLogInput a, final CameraLogInput b) {
        if ((a == null) || (b == null)) {
            return false;
        }
        final List dataA = a.getRecords();
        final List dataB = b.getRecords();
        final boolean out = checkForContinuation(dataA, dataB);
        if (out) {
            if (log.isInfoEnabled()) {
                log.info("Since the log, " + a.getUrl() + ", ends within 2 hours and " + "500 frames of the start of, " + b.getUrl() + ", this indicates that a single dive occured during GMT " + "rollover ");
            }
        }
        return out;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param a
     * @param b
     *
     * @return
     */
    public static final boolean checkForContinuation(List a, List b) {
        if ((a == null) || (b == null)) {
            return false;
        }
        if ((a.size() == 0) || (b.size() == 0)) {
            return false;
        }
        final CameraLogRecord rA = (CameraLogRecord) a.get(a.size() - 1);
        final CameraLogRecord rB = (CameraLogRecord) b.get(0);
        final Timecode tA = rA.getTimeCode();
        final Timecode tB = rB.getTimeCode();
        final double dF = tB.diffFrames(tA);
        final long dE = rB.getEpoch() - rA.getEpoch();
        boolean out = false;
        if ((dF < 500) && (dF > 0) && (dE < CameraLogInput.TIME_FOR_BREAK_IN_LOG)) {
            out = true;
        }
        return out;
    }

    /**
     * Checks a Camera log to see if it contains a break of more than 2 hours
     * that occurs with a counter reset on the VCR
     *
     *
     * @param  logRecords              A list of CameraLogRecords
     * @return             The indices of the break (value between 0 and
     *         log.getLogData().size - 1) The index points to the start of the
     *         section following the break. If no breaks were found the returned
     *         List will have a size of 0. THe indices are stored as Integers
     */
    public static final List findBreaks(List logRecords) {
        final List breaks = new ArrayList();
        int idx = 0;
        if ((logRecords != null) && (logRecords.size() > 1)) {
            Iterator i = logRecords.iterator();
            CameraLogRecord current = (CameraLogRecord) i.next();
            while (i.hasNext()) {
                final CameraLogRecord next = (CameraLogRecord) i.next();
                idx++;
                final double dF = next.getTimeCode().diffFrames(current.getTimeCode());
                final long dE = next.getEpoch() - current.getEpoch();
                if ((dE > CameraLogInput.TIME_FOR_BREAK_IN_LOG) && (dF < 0)) {
                    breaks.add(Integer.valueOf(idx));
                }
                current = next;
            }
        }
        return breaks;
    }

    /**
     * Checks the logRecords for any counter resets. Ideally these will be occur
     * at the same time as the breaks, but this may not always be true.
     *
     *
     * @param  logRecords              A list of CameraLogRecords
     * @return             A List of Integers. Each integer indicates a the position of the
     *         first record that occurs after the reset.
     */
    public static final List findResets(List logRecords) {
        final List resets = new ArrayList();
        int idx = 0;
        if ((logRecords != null) && (logRecords.size() > 1)) {
            Iterator i = logRecords.iterator();
            CameraLogRecord current = (CameraLogRecord) i.next();
            while (i.hasNext()) {
                final CameraLogRecord next = (CameraLogRecord) i.next();
                idx++;
                final double dF = next.getTimeCode().diffFrames(current.getTimeCode());
                if (dF < 0) {
                    resets.add(Integer.valueOf(idx));
                }
                current = next;
            }
        }
        return resets;
    }

    /**
     * @return    An Unmodifiable Collection of CameraLogRecord objects sorted by
     *         Epoch seconds.
     */
    public List getRecords() {
        return Collections.unmodifiableList(records);
    }

    /**
	 * @return     The url of the source camera-log.
	 * @uml.property  name="url"
	 */
    public URL getUrl() {
        return url;
    }

    /**
     * Reads in all records from the camera-log. Records are initially sorted by Epoch.
     *
     * @throws  IOException Thrown if unable to read the camera-log
     */
    public void read() throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Reading the camera log, " + url);
        }
        final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        int i = 0;
        try {
            while ((line = in.readLine()) != null) {
                i++;
                try {
                    final CameraLogRecord logDatum = new CameraLogRecord(line);
                    records.add(logDatum);
                } catch (LogParseException e) {
                    if (log.isInfoEnabled()) {
                        log.info("Bad record in " + url + " at line:" + i);
                    }
                }
            }
        } finally {
            in.close();
        }
        Collections.sort(records);
        if (log.isInfoEnabled()) {
            log.info("Finished reading the camera log, " + url);
        }
    }

    /**
     * @param  tc                      The timecode to search for
     * @param  logRecords              A Sorted by timecode list of CameraLogRecords
     * @return             A matching log record, null if no match was found. If an exact
     *         match is not found but close matches are the result will be
     *         interpolated between the two nearest CameraLogRecords.
     * @see                sortByTimeCode
     */
    public static final CameraLogRecord searchByTimeCode(final Timecode tc, final List logRecords) {
        CameraLogRecord r1 = null;
        CameraLogRecord key = new CameraLogRecord(tc, 0, 0, 0, 0, 0);
        int idx = Collections.binarySearch(logRecords, key, tcComparator);
        if (idx >= 0) {
            r1 = (CameraLogRecord) logRecords.get(idx);
        } else {
            idx = -(idx + 1);
            if (idx == 0) {
                r1 = (CameraLogRecord) logRecords.get(idx);
            } else if (idx >= logRecords.size() - 1) {
                r1 = (CameraLogRecord) logRecords.get(logRecords.size() - 1);
            } else {
                r1 = (CameraLogRecord) logRecords.get(idx);
                final CameraLogRecord r0 = (CameraLogRecord) logRecords.get(idx - 1);
                try {
                    r1 = CameraLogRecord.interpolate(r0, r1, tc);
                } catch (Exception e) {
                    log.info("Failed to interpolate between " + r0 + " and " + r1 + ". Using " + r1, e);
                }
            }
        }
        return r1;
    }

    /**
     *  Sorts the log records by time (i.e epoch)
     *
     * @param  logRecords  A List containing <code>CameraLogRecord</code>s.
     */
    public static final void sortByEpoch(final List logRecords) {
        Collections.sort(logRecords);
    }

    /**
     *  Sorts the log records by video time-code
     *
     * @param  logRecords  A List containing <code>CameraLogRecord</code>s.
     */
    public static final void sortByTimeCode(final List logRecords) {
        Collections.sort(logRecords, tcComparator);
    }
}
