package edu.ucsd.ncmir.jibber.listeners;

import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.RemoteFile;
import edu.ucsd.ncmir.asynchronous_event.AbstractAsynchronousEventListener;
import edu.ucsd.ncmir.asynchronous_event.AsynchronousEvent;
import edu.ucsd.ncmir.jibber.core.JibberImage;
import edu.ucsd.ncmir.jibber.events.CreateProgressBarEvent;
import edu.ucsd.ncmir.jibber.events.CreateWorkspaceEvent;
import edu.ucsd.ncmir.jibber.events.GetURIElementEvent;
import edu.ucsd.ncmir.jibber.events.GetURIEvent;
import edu.ucsd.ncmir.jibber.events.ProgressUpdateEvent;
import edu.ucsd.ncmir.jibber.events.ErrorEvent;
import edu.ucsd.ncmir.jibber.events.WaitEvent;
import edu.ucsd.ncmir.spl.filesystem.GeneralFileFactory;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.utilities.SHA1;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author spl
 */
public class ImageReaderEventListener extends AbstractAsynchronousEventListener {

    /**
     * Creates a new instance of ImageReaderEventListener
     */
    public ImageReaderEventListener() {
        new GetURIElementEventListener(this).enable();
        new GetURIEventListener(this).enable();
    }

    private URI _uri;

    public void handler(AsynchronousEvent event, Object object) {
        try {
            this._uri = (URI) object;
            String hash = SHA1.hash(this._uri.toString());
            String path = System.getProperty("user.home") + File.separator + "." + hash;
            File f = new File(path);
            BufferedImage image = null;
            if (f.exists()) {
                try {
                    image = ImageIO.read(f);
                } catch (Exception e) {
                    new ErrorEvent().send(e);
                    image = null;
                }
            }
            if (image == null) {
                new WaitEvent().send(true);
                ArrayList<ImageReader> readers = this.getImageReader(this._uri);
                if (readers != null) {
                    for (ImageReader ir : readers) try {
                        if ((image = ir.read(0)) != null) {
                            ImageIO.write(image, "png", f);
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                new ImageDimensionEvent().send(new Dimension(width, height));
                JibberImage ji = JibberImage.jibberify(image, new StatusObserver());
                new CreateWorkspaceEvent().sendWait(ji);
            } else new ErrorEvent().sendWait("Error reading image.");
        } catch (Exception ex) {
            new ErrorEvent().sendWait(ex);
        } catch (Error er) {
            new ErrorEvent().sendWait(er.toString());
        } finally {
            new WaitEvent().send(false);
        }
    }

    private static final long MAXSIZE = 1280 * 1280 * 4;

    private ArrayList<ImageReader> getImageReader(URI uri) throws IOException, InterruptedException {
        GeneralFile general_file = GeneralFileFactory.createFile(uri);
        if (general_file == null) throw new IOException("Unable to open " + uri);
        if (!general_file.exists()) throw new IOException(uri + " does not exist.");
        InputStream input_stream = null;
        String suffix = null;
        ImageIO.scanForPlugins();
        if (general_file instanceof RemoteFile) {
            RemoteFile remote_file = (RemoteFile) general_file;
            long length = remote_file.length();
            if (length > MAXSIZE) {
                ProxyThread pt = new ProxyThread(uri);
                pt.start();
                pt.join(60000);
                if (pt.isComplete()) {
                    input_stream = pt.getInputStream();
                    suffix = "ppm";
                } else {
                    new ErrorEvent().send("Error connecting to downsampler.");
                    pt.interrupt();
                }
            }
        }
        if (input_stream == null) {
            input_stream = GeneralFileFactory.createInputStream(general_file);
            String[] path = uri.getPath().split("\\.");
            suffix = path[path.length - 1];
        }
        ArrayList<ImageReader> readers = new ArrayList<ImageReader>();
        File path = new File(System.getProperty("user.home"));
        ImageInputStream iis = new FileCacheImageInputStream(input_stream, path);
        Iterator<ImageReader> list;
        list = ImageIO.getImageReaders(iis);
        while (list.hasNext()) {
            ImageReader ir = list.next();
            ir.setInput(iis);
            readers.add(ir);
        }
        if ((readers.isEmpty()) || (suffix != null)) {
            list = ImageIO.getImageReadersBySuffix(suffix);
            while (list.hasNext()) {
                ImageReader ir = list.next();
                ir.setInput(iis);
                readers.add(ir);
            }
        }
        return readers;
    }

    private class ProxyThread extends Thread {

        private URI _uri;

        ProxyThread(URI uri) {
            this._uri = uri;
        }

        private InputStream _input_stream = null;

        private boolean _complete = false;

        InputStream getInputStream() {
            return this._input_stream;
        }

        boolean isComplete() {
            return this._complete;
        }

        private static final String FALLBACK = "http://tirebiter.ucsd.edu/";

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            String server = System.getProperty("server.downsampler");
            if (server == null) server = FALLBACK;
            String url = server + "cgi-bin/downsample.cgi?" + this._uri.toString();
            url = url.replaceAll("\\?#$", "");
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setDoInput(true);
                this._input_stream = connection.getInputStream();
                while (this._input_stream.read() != '\n') ;
                this._complete = true;
            } catch (Exception e) {
                new ErrorEvent().send(e);
            }
        }
    }

    private class ImageDimensionEvent extends AsynchronousEvent {
    }

    private class StatusObserver implements ImageObserver {

        StatusObserver() {
            super();
        }

        private int _height;

        private Date _start;

        private boolean _been_here = false;

        private boolean _report_status = false;

        private int _last_percent = -1;

        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if ((infoflags & ImageObserver.HEIGHT) != 0) this._height = height;
            if ((infoflags & ImageObserver.PROPERTIES) != 0) this._start = new Date();
            if ((infoflags & ImageObserver.SOMEBITS) != 0) {
                if (!this._been_here) {
                    Date now = new Date();
                    long dt = now.getTime() - _start.getTime();
                    double projected = this._height * (dt / (y + 1));
                    if (projected > 5000.0) {
                        new CreateProgressBarEvent("Processing. . .").sendWait();
                        this._report_status = true;
                    }
                    this._been_here = true;
                }
                if (this._report_status) {
                    int percent = (int) ((y / (this._height - 1.0)) * 100.0);
                    if (this._last_percent < percent) new ProgressUpdateEvent().send(percent);
                    this._last_percent = percent;
                }
            }
            return true;
        }
    }

    private class GetURIElementEventListener extends AbstractAsynchronousEventListener {

        private ImageReaderEventListener _irel;

        GetURIElementEventListener(ImageReaderEventListener irel) {
            this._irel = irel;
        }

        public void handler(AsynchronousEvent event, Object object) {
            GetURIElementEvent gurie = (GetURIElementEvent) event;
            Element element = new Element("URI");
            element.setText(this._irel._uri.toString());
            gurie.setElement(element);
        }
    }

    private class GetURIEventListener extends AbstractAsynchronousEventListener {

        private ImageReaderEventListener _irel;

        GetURIEventListener(ImageReaderEventListener irel) {
            this._irel = irel;
        }

        public void handler(AsynchronousEvent event, Object object) {
            GetURIEvent gurie = (GetURIEvent) event;
            gurie.setURI(this._irel._uri.toString());
        }
    }
}
