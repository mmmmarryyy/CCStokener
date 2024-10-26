package org.m4eclipse.maven.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.m4eclipse.M4EclipsePlugin;
import org.osgi.framework.Bundle;

/**
 * UnpackerJob
 */
class UnpackerJob extends Job {

    private final Bundle bundle;

    private final File indexDir;

    private final Set indexes;

    private final Object[] indexUrl;

    private final SimpleDateFormat FORMAT;

    public UnpackerJob(Bundle bundle, File indexDir, Object[] indexUrl, Set indexes) {
        super("Initializing indexes");
        this.bundle = bundle;
        this.indexDir = indexDir;
        this.indexUrl = indexUrl;
        this.indexes = indexes;
        this.FORMAT = new SimpleDateFormat("dd/MM/yyyy");
        this.FORMAT.setLenient(false);
        setPriority(Job.LONG);
    }

    protected IStatus run(IProgressMonitor monitor) {
        if (!indexDir.exists()) indexDir.mkdirs();
        for (int i = 0; i < indexUrl.length; i++) {
            String name = ((String[]) indexUrl[i])[0];
            String url = ((String[]) indexUrl[i])[1];
            if (download(name, url, monitor)) indexes.add(name);
        }
        return Status.OK_STATUS;
    }

    private boolean download(String name, String url, IProgressMonitor monitor) {
        File archive = new File(this.indexDir, name + ".zip");
        File timestamp = new File(this.indexDir, name + ".timestamp");
        File dir = new File(this.indexDir, name);
        URL indexArchive = null;
        try {
            if (url.startsWith("bundle:")) {
                String entry = url.substring("bundle:".length());
                indexArchive = bundle.getEntry(entry);
            } else {
                indexArchive = new URL(url);
            }
            if (!checkTimestamp(timestamp, indexArchive)) {
                download(archive, timestamp, indexArchive, monitor);
            }
            return extract(archive, dir, monitor);
        } catch (MalformedURLException e) {
            M4EclipsePlugin.log(new Status(IStatus.ERROR, M4EclipsePlugin.PLUGIN_ID, -1, "For index '" + name + "' url is malformed '" + url + "'", e));
        } catch (IOException e) {
            M4EclipsePlugin.log(new Status(IStatus.ERROR, M4EclipsePlugin.PLUGIN_ID, -1, "For index '" + name + "' can't download file from url '" + url + "'", e));
        }
        return false;
    }

    private boolean extract(File archive, File dir, IProgressMonitor monitor) {
        monitor.subTask("Extract : " + archive.getName());
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            File[] files = dir.listFiles();
            for (int j = 0; j < files.length; j++) {
                files[j].delete();
            }
        }
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(archive));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File indexFile = new File(dir, entry.getName());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(indexFile);
                    IOUtils.copy(zis, fos);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }
            return true;
        } catch (Exception ex) {
            M4EclipsePlugin.log(new Status(IStatus.ERROR, M4EclipsePlugin.PLUGIN_ID, -1, "For index '" + dir.getName() + "' Unable to initialize indexes", ex));
        } finally {
            IOUtils.closeQuietly(zis);
        }
        return false;
    }

    private void download(File archive, File timestamp, URL url, IProgressMonitor monitor) throws IOException {
        monitor.subTask("download " + url.toString());
        InputStream in = null;
        FileOutputStream out = null;
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            Writer writer = null;
            try {
                Date date = new Date(conn.getLastModified());
                writer = new FileWriter(timestamp);
                writer.write(this.FORMAT.format(date));
            } catch (IOException e) {
                timestamp.delete();
            } finally {
                IOUtils.closeQuietly(writer);
            }
            in = conn.getInputStream();
            out = new FileOutputStream(archive);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private boolean checkTimestamp(File timestamp, URL url) {
        try {
            if (timestamp.exists()) {
                FileReader reader = null;
                Date dateLocal = null;
                try {
                    reader = new FileReader(timestamp);
                    StringWriter tmp = new StringWriter();
                    IOUtils.copy(reader, tmp);
                    dateLocal = this.FORMAT.parse(tmp.toString());
                } catch (ParseException e) {
                    timestamp.delete();
                } catch (IOException e) {
                } finally {
                    IOUtils.closeQuietly(reader);
                }
                if (dateLocal != null) {
                    try {
                        URLConnection conn = url.openConnection();
                        Date date = this.FORMAT.parse(this.FORMAT.format(new Date(conn.getLastModified())));
                        return (date.compareTo(dateLocal) == 0);
                    } catch (IOException e) {
                    }
                }
            }
        } catch (Throwable t) {
        }
        return false;
    }
}
