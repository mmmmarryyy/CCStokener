package de.fu_berlin.inf.gmanda.imports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import org.joda.time.DateTime;
import de.fu_berlin.inf.gmanda.gui.preferences.CacheDirectoryProperty;
import de.fu_berlin.inf.gmanda.imports.GmaneImporter.ImportSettings;
import de.fu_berlin.inf.gmanda.util.progress.IProgress;
import de.fu_berlin.inf.gmanda.util.progress.ProgressInputStream;
import de.fu_berlin.inf.gmanda.util.progress.IProgress.ProgressStyle;

public class GmaneMboxFetcher {

    CacheDirectoryProperty cacheDirectory;

    public GmaneMboxFetcher(CacheDirectoryProperty cacheLocation) {
        this.cacheDirectory = cacheLocation;
    }

    public static final int CHUNKSIZE = 500;

    public int getEstimatedStartEmail(IProgress progress, String mailingList, DateTime c) {
        progress.setScale(20);
        progress.setStyle(ProgressStyle.ROTATING);
        progress.start();
        try {
            int result = 1;
            if (c == null) return result;
            while (true) {
                Message m;
                try {
                    m = getSingleMail(progress.getSub(1), mailingList, result);
                    if (m == null || result > 10000) {
                        return Math.max(1, result - CHUNKSIZE);
                    }
                    if (m != null && (m.getSentDate() == null || m.getSentDate().after(c.toDate()))) {
                        return Math.max(1, result - CHUNKSIZE);
                    }
                } catch (Exception e) {
                    return Math.max(1, result - CHUNKSIZE);
                }
                result += CHUNKSIZE;
            }
        } finally {
            progress.done();
        }
    }

    public int getEstimatedEndEmail(IProgress progress, String mailingList, int start, DateTime c) {
        progress.setScale(20);
        progress.setStyle(ProgressStyle.ROTATING);
        progress.start();
        try {
            if (start == -1) start = 1;
            int result = start;
            while (true) {
                Message m;
                try {
                    m = getSingleMail(progress.getSub(1), mailingList, result);
                    if (m == null || (m.getSentDate() != null && c != null && m.getSentDate().after(c.toDate()))) {
                        return result;
                    }
                } catch (Exception e) {
                    return result;
                }
                result += CHUNKSIZE;
            }
        } finally {
            progress.done();
        }
    }

    public void fetch(IProgress progress, ImportSettings settings) throws IOException {
        progress.setScale(100);
        progress.start();
        try {
            settings.initialize(progress.getSub(10), this);
            IProgress fetchProgress = progress.getSub(90);
            fetchProgress.setScale(settings.rangeEnd - settings.rangeStart);
            fetchProgress.start();
            PrintWriter pw = new PrintWriter(settings.mboxFile);
            try {
                for (int i = settings.rangeStart; i < settings.rangeEnd; i += CHUNKSIZE) {
                    fetchProgress.setNote(String.format("Fetching Email #%d", i));
                    appendFetch(fetchProgress.getSub(CHUNKSIZE), pw, settings.listName, i, Math.min(i + CHUNKSIZE, settings.rangeEnd));
                }
            } finally {
                pw.close();
                fetchProgress.done();
            }
        } finally {
            progress.done();
        }
    }

    public static URL getGmaneURL(String list, int from, int to) {
        list = list.trim();
        if (!list.startsWith("gmane.")) list = "gmane." + list;
        try {
            return new URL(String.format("http://download.gmane.org/%s/%d/%d", list, from, to));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void appendFetch(IProgress progress, PrintWriter pw, String list, int from, int to) throws IOException {
        progress.start();
        try {
            File storage = new File(cacheDirectory.getValue(), "mboxes");
            storage.mkdirs();
            File mbox = new File(storage, list + "-" + from + "-" + to + ".mbox");
            if (mbox.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new ProgressInputStream(new FileInputStream(mbox), progress, 10000)));
                String line;
                while ((line = in.readLine()) != null) {
                    pw.write(line);
                    pw.write('\n');
                }
                in.close();
                return;
            }
            progress.setScale(100);
            IProgress subProgress1 = progress.getSub(75);
            URL url = getGmaneURL(list, from, to);
            BufferedReader in = new BufferedReader(new InputStreamReader(new ProgressInputStream(url.openStream(), subProgress1, 10000)));
            PrintWriter writeToMbox = new PrintWriter(mbox);
            int lines = 0;
            String line;
            while ((line = in.readLine()) != null) {
                lines++;
                if (line.matches("^From .*$") && !line.matches("^From .*? .*[0-9][0-9]:[0-9][0-9]:[0-9][0-9].*$")) {
                    line = ">" + line;
                }
                writeToMbox.write(line);
                writeToMbox.write('\n');
            }
            in.close();
            writeToMbox.close();
            appendFetch(progress.getSub(25), pw, list, from, to);
        } finally {
            progress.done();
        }
    }

    public Message getSingleMail(IProgress pm, String mailingList, int number) throws MessagingException, IOException {
        pm.setScale(100);
        pm.start();
        ImportSettings settings = new ImportSettings();
        settings.listName = mailingList;
        settings.rangeStart = number;
        settings.rangeEnd = number + 1;
        fetch(pm.getSub(50), settings);
        Folder folder = MyMimeUtils.getFolder(pm.getSub(50), settings.mboxFile);
        settings.mboxFile.delete();
        if (folder == null || folder.getMessageCount() != 1) return null;
        Message message;
        try {
            message = folder.getMessage(1);
        } finally {
            Store s = folder.getStore();
            folder.close(false);
            s.close();
        }
        pm.done();
        return message;
    }

    /**
	 * Returns a list of all Messages from the given mailing-list that have IDs
	 * from 'from' (inclusive) to 'to' (exclusive).
	 * 
	 */
    public List<Message> getMails(IProgress progress, String mailingList, int from, int to) throws MessagingException, IOException {
        progress.setScale(100);
        progress.start();
        ImportSettings settings = new ImportSettings();
        settings.listName = mailingList;
        settings.rangeStart = from;
        settings.rangeEnd = to;
        fetch(progress.getSub(60), settings);
        Folder folder = MyMimeUtils.getFolder(progress.getSub(40), settings.mboxFile);
        settings.mboxFile.delete();
        if (folder == null || folder.getMessageCount() == 0) return null;
        try {
            return Arrays.asList(folder.getMessages());
        } finally {
            Store s = folder.getStore();
            folder.close(false);
            s.close();
        }
    }
}
