package net.sourceforge.jget3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andrew ``Bass'' Shcheglov (andrewbass@users.sourceforge.net)
 * @author $Author: andrewbass $
 * @version $Revision: 12 $, $Date:: 2009-10-16 09:08:36 -0400 #$
 * @todo lock file being written to to prevent two processes from writing into the same file
 * @todo add a default action depending on whether the Ui is interactive or not
 * @todo add an option of downloading every segment twice in order to ensure file consistency
 * @todo Use commons-vfs or jcifs (http://jcifs.samba.org) for SMB connections.
 * @todo maintain a mapping of netbios names to loopback addresses (127.X.Y.Z), excluding 127.0.0.1 
 * @todo Use autossh+ssh (native) or j2ssh (http://sshtools.sourceforge.net) or jsch (http://www.jcraft.com/jsch/) for SSH connections 
 * @todo preserve ctime/mtime/attributes
 * @todo add logger UI, terminal UI
 * @todo build script
 * @todo start script (UNIX shell), DISPLAY variable analysis
 * @todo JNLP support
 */
abstract class Main {

    private static final String OPTION_GUI = "--gui";

    private Main() {
        assert false;
    }

    /**
	 * @param directory
	 * @param outDir
	 * @param ui
	 * @throws IOException
	 */
    private static List<DownloadUnit> buildQueue(final File directory, final File outDir, final Ui ui) throws IOException {
        final URI uri = directory.toURI();
        final URL url = uri.toURL();
        final String contentType = url.openConnection().getContentType();
        ui.println("URI: {0}", uri);
        ui.println("URL: {0}", url);
        if (!directory.exists()) {
            ui.println("No such file or directory.");
            return Collections.emptyList();
        }
        if (!directory.isDirectory()) {
            ui.println("Not a directory.");
            return Collections.emptyList();
        }
        final long inFileLength = directory.length();
        final String basename = directory.getName();
        final File newOutDir = new File(outDir, basename);
        ui.println("Downloading {0} ({1}, {2} byte(s)) to {3}", url, contentType, Long.valueOf(inFileLength), newOutDir);
        final File inFiles[] = directory.listFiles();
        if (inFiles.length == 0) {
            return Collections.emptyList();
        }
        final List<DownloadUnit> queue = new ArrayList<DownloadUnit>();
        for (final File inFile : inFiles) {
            if (inFile.isDirectory()) {
                queue.addAll(buildQueue(inFile, newOutDir, ui));
            } else {
                queue.add(new DownloadUnit(inFile, newOutDir));
            }
        }
        return queue;
    }

    /**
	 * @param inFile
	 * @param outDir
	 * @param sleepTimeout
	 * @param ui
	 * @throws IOException
	 */
    private static void download(final File inFile, final File outDir, final long sleepTimeout, final Ui ui) throws IOException {
        if (!inFile.exists()) {
            ui.println("{0}: no such file or directory.", inFile);
            return;
        }
        final boolean file = inFile.isFile();
        if (!file && !inFile.isDirectory()) {
            ui.println("{0}: is a special file.", inFile);
        }
        if (file) {
            downloadFile(inFile, outDir, sleepTimeout, ui);
        } else {
            final List<DownloadUnit> queue = buildQueue(inFile, outDir, ui);
            long bytesDownloaded = 0L;
            long bytesTotal0 = 0L;
            for (final DownloadUnit unit : queue) {
                bytesTotal0 += unit.getInFileLength();
            }
            final Long bytesTotal = Long.valueOf(bytesTotal0);
            int countDownloaded = 0;
            final Integer countTotal = Integer.valueOf(queue.size());
            ui.println("Downloading {0} byte(s) in {1} file(s)...", bytesTotal, countTotal);
            for (final DownloadUnit unit : queue) {
                downloadFile(unit.getInFile(), unit.getOutDir(), sleepTimeout, ui);
                ui.println("Downloaded {0} of {1} byte(s).", Long.valueOf(bytesDownloaded += unit.getInFileLength()), bytesTotal);
                ui.println("Downloaded {0} of {1} file(s).", Integer.valueOf(++countDownloaded), countTotal);
            }
        }
    }

    /**
	 * @param inFileName
	 * @param outDirName
	 * @param sleepTimeout
	 * @param ui
	 * @throws IOException
	 */
    private static void download(final String inFileName, final String outDirName, final long sleepTimeout, final Ui ui) throws IOException {
        download(new File(inFileName), new File(outDirName), sleepTimeout, ui);
    }

    /**
	 * @param inFile
	 * @param outDir
	 * @param sleepTimeout
	 * @param ui
	 * @throws IOException
	 */
    private static void downloadFile(final File inFile, final File outDir, final long sleepTimeout, final Ui ui) throws IOException {
        final URI uri = inFile.toURI();
        final URL url = uri.toURL();
        final String contentType = url.openConnection().getContentType();
        ui.println("URI: {0}", uri);
        ui.println("URL: {0}", url);
        if (!inFile.exists()) {
            ui.println("No such file or directory.");
            return;
        }
        if (inFile.isDirectory()) {
            ui.println("File is a directory.");
            return;
        }
        final boolean outDirExists = mkdir(outDir, ui);
        if (!outDirExists) {
            ui.println("{0}: failed to create directory.", outDir);
            return;
        }
        final long inFileLength = inFile.length();
        final String basename = inFile.getName();
        final File outFile = new File(outDir, basename);
        ui.println("Downloading {0} ({1}, {2} byte(s)) to {3}", url, contentType, Long.valueOf(inFileLength), outFile);
        ui.setProgressMaximum(inFileLength);
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile, true));
        final long initialOutFileLength = outFile.length();
        final AtomicLong progressValue = new AtomicLong(initialOutFileLength);
        final Thread progressUpdater = new Thread("ProgressUpdater") {

            @Override
            public void run() {
                while (ui.setProgressValue(progressValue.get()) != inFileLength && !interrupted()) {
                    try {
                        sleep(100);
                    } catch (final InterruptedException ie) {
                        break;
                    }
                }
            }
        };
        progressUpdater.start();
        final long t0 = System.currentTimeMillis();
        final long t0Precise = System.nanoTime();
        while (!Thread.interrupted()) {
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(inFile));
                if (outFile.exists()) {
                    if (!outFile.isFile()) {
                        ui.println("{0} is not a regular file.", outFile);
                        break;
                    }
                    final long outFileLength = outFile.length();
                    if (outFileLength == inFileLength) {
                        ui.println("File has been fully retrieved.");
                        break;
                    } else if (outFileLength > inFileLength) {
                        ui.println("Local file is {0} byte(s) longer than the remote one (local file length: {1}; remote file length: {2}).", Long.valueOf(outFileLength - inFileLength), Long.valueOf(outFileLength), Long.valueOf(inFileLength));
                        progressUpdater.interrupt();
                        break;
                    }
                    if (outFileLength != 0) {
                        ui.println("Skipping {0} byte(s)...", Long.valueOf(outFileLength));
                        final long bytesSkipped = in.skip(outFileLength);
                        if (bytesSkipped != outFileLength) {
                            ui.println("Expected to skip {0} byte(s); skipped {1} instead.", Long.valueOf(outFileLength), Long.valueOf(bytesSkipped));
                            progressUpdater.interrupt();
                            break;
                        }
                        ui.println("Done skipping");
                    }
                }
                int b;
                while ((b = in.read()) != -1) {
                    out.write(b);
                    progressValue.getAndIncrement();
                }
                break;
            } catch (final IOException ioe) {
                ui.println(ioe.getMessage());
                ui.println("Continuing in {0} ms...", Long.valueOf(sleepTimeout));
                try {
                    Thread.sleep(sleepTimeout);
                } catch (final InterruptedException ie) {
                    ui.println("Interrupted");
                    break;
                }
                continue;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (final IOException ioe) {
                }
            }
        }
        out.flush();
        out.close();
        final long t1 = System.currentTimeMillis();
        final long t1Precise = System.nanoTime();
        try {
            progressUpdater.join();
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final long timeMillis = t1 - t0;
        final double timeMillisPrecise = (t1Precise - t0Precise) / 1000 / 1e3;
        final long finalOutFileLength = outFile.length();
        final long bytesTransferred = finalOutFileLength - initialOutFileLength;
        final double time;
        final long rate;
        if (timeMillis == 0) {
            time = timeMillisPrecise / 1e3;
            rate = (long) (bytesTransferred / timeMillisPrecise * 1000L);
        } else {
            time = timeMillis / 1e3;
            rate = (long) (bytesTransferred / ((double) timeMillis) * 1000L);
        }
        ui.println("{0} of {1} byte(s) retrieved in {2} second(s); {3} byte(s) per second", Long.valueOf(finalOutFileLength), Long.valueOf(inFileLength), Double.valueOf(time), Long.valueOf(rate));
    }

    /**
	 * @param dirName
	 * @param ui
	 */
    private static boolean mkdir(final File dir, final Ui ui) {
        if (dir.isFile()) {
            ui.println("{0} exists and is a regular file, not a directory.", dir);
            return false;
        }
        final boolean exists = dir.exists();
        if (exists && !dir.isDirectory()) {
            ui.println("{0} exists and is a special file, not a directory.", dir);
            return false;
        }
        return exists || dir.mkdirs();
    }

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(final String[] args) throws IOException {
        final List<String> argsFiltered = new LinkedList<String>(Arrays.asList(args));
        final boolean useGui = argsFiltered.remove(OPTION_GUI);
        final Ui ui = useGui ? new SwingUi() : new ConsoleUi(System.out);
        if (argsFiltered.isEmpty()) {
            ui.usage();
            return;
        }
        ui.init();
        final String outDirName = System.getProperty("user.dir");
        for (final String arg : argsFiltered) {
            download(arg, outDirName, 1000L, ui);
        }
        ui.enableExit();
    }
}
