package org.objectwiz.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTargetDragEvent;
import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URI;
import java.net.URLConnection;
import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An helper class that provides an abstraction to platform-specific
 * operations. 
 */
public abstract class Platforms {

    Platforms() {
    }

    public static Platforms instance() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") != -1 || os.indexOf("nt") != -1) {
            return new WindowsDesktop();
        } else if (os.equals("windows 95") || os.equals("windows 98")) {
            return new Windows9xDesktop();
        } else if (os.indexOf("mac") != -1) {
            return new OSXDesktop();
        } else if (os.indexOf("linux") != -1) {
            return new LinuxDesktop();
        } else {
            throw new UnsupportedOperationException(String.format("The platform %s is not supported ", os));
        }
    }

    public final void open(File file) {
        Process process;
        try {
            process = launchProcess(file);
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error while opening file with default editor: " + file, e);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Opening file with default editor returned an error [code=" + process.exitValue() + "]: " + file);
        }
    }

    protected abstract Process launchProcess(File file) throws Exception;

    public abstract boolean isSupportedDropEvent(DropTargetDragEvent event);

    public String getMimeType(File f) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(f.getName());
    }

    public void openUrl(URI uri) throws IOException {
        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop not supported");
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(uri);
    }

    public int getResponseCode(URI uri) {
        int response = -1;
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            response = connection.getResponseCode();
        } catch (MalformedURLException m) {
            throw new MalformedURLException("URL not correct");
        } catch (IOException e) {
            throw new IOException("can open connection");
        } finally {
            return response;
        }
    }

    public boolean canOpenURI(URI uri) {
        return (getResponseCode(uri) == 200);
    }

    static class WindowsDesktop extends Platforms {

        @Override
        public Process launchProcess(File file) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            return runtime.exec("rundll32 url.dll,FileProtocolHandler " + file.getAbsolutePath());
        }

        @Override
        public boolean isSupportedDropEvent(DropTargetDragEvent event) {
            return event.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }
    }

    static class Windows9xDesktop extends WindowsDesktop {

        @Override
        public Process launchProcess(File file) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            return runtime.exec("cmd /c start " + file.getAbsolutePath());
        }
    }

    static class OSXDesktop extends Platforms {

        @Override
        public Process launchProcess(File file) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            return runtime.exec("open " + file.getAbsolutePath());
        }

        @Override
        public boolean isSupportedDropEvent(DropTargetDragEvent event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    static class LinuxDesktop extends Platforms {

        private static final String unixFileDataFlavorDesc = "text/uri-list;class=java.lang.String";

        private DataFlavor unixFileDataFlavor;

        protected LinuxDesktop() {
            try {
                this.unixFileDataFlavor = new DataFlavor(unixFileDataFlavorDesc);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Flavor not supported: " + unixFileDataFlavorDesc);
            }
        }

        @Override
        public Process launchProcess(File file) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            return runtime.exec("xdg-open " + file.getAbsolutePath());
        }

        @Override
        public boolean isSupportedDropEvent(DropTargetDragEvent event) {
            return event.isDataFlavorSupported(this.unixFileDataFlavor);
        }
    }
}
