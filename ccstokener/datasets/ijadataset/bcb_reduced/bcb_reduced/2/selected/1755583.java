package taseanalyzer.net;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vainolo
 */
public class TASEFileFetcher {

    String baseAddress = "http://www.tase.co.il/FileDistribution/PackTarget/";

    String taseZipFilesPath;

    Logger logger = Logger.getLogger(TASEFileFetcher.class.getName());

    public TASEFileFetcher() {
        taseZipFilesPath = "files";
    }

    public TASEFileFetcher(String destinationPath) {
        taseZipFilesPath = destinationPath;
    }

    public File download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        boolean succeeded = false;
        try {
            logger.info("Trying to retrieve " + address);
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            logger.info(localFileName + "\t" + numWritten);
            succeeded = true;
        } catch (Exception e1) {
            logger.severe("Exception ocurred while reading file: " + localFileName);
            try {
                out.close();
                File f = new File(localFileName);
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception e2) {
                logger.severe("Exception ocurred while closing file after reading exception: " + localFileName);
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        if (succeeded) return new File(localFileName); else return null;
    }

    public List<File> fetchPeriod(Date startPeriod, Date endPeriod) {
        List<File> files = new ArrayList<File>();
        Calendar c = Calendar.getInstance();
        Date currentDate = startPeriod;
        while (currentDate.getTime() < endPeriod.getTime()) {
            c.setTime(currentDate);
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH);
            String filename = "Full" + yearToString(year) + monthToString(month) + dayToString(day) + "0.zip";
            File testFile = new File(filename);
            if (testFile.exists()) {
                if (testFile.length() == 0) {
                    testFile.delete();
                } else {
                    logger.info("Skipping file " + filename);
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    currentDate = c.getTime();
                    continue;
                }
            }
            File downloadedFile = download(baseAddress + filename, taseZipFilesPath + File.separator + filename);
            if (downloadedFile != null) files.add(downloadedFile);
            c.add(Calendar.DAY_OF_YEAR, 1);
            currentDate = c.getTime();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TASEFileFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return files;
    }

    private String yearToString(int year) {
        if (year >= 1000) {
            return Integer.toString(year);
        } else {
            return Integer.toString(year + 1900);
        }
    }

    private String monthToString(int month) {
        if (month >= 10) {
            return Integer.toString(month);
        } else {
            return "0" + Integer.toString(month);
        }
    }

    private String dayToString(int day) {
        if (day >= 10) {
            return Integer.toString(day);
        } else {
            return "0" + Integer.toString(day);
        }
    }

    public static void main(String args[]) {
        Calendar c1 = Calendar.getInstance();
        c1.clear();
        c1.set(2009, 5, 20);
        Calendar c2 = Calendar.getInstance();
        Date d1 = c1.getTime();
        c2.clear();
        c2.set(2009, 6, 20);
        Date d2 = c2.getTime();
        TASEFileFetcher ret = new TASEFileFetcher();
        ret.fetchPeriod(d1, d2);
    }
}
