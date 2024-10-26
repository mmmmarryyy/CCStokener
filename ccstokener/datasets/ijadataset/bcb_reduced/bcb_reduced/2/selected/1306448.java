package org.gvsig.remoteClient.taskplanning.retrieving;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import org.gvsig.remoteClient.taskplanning.IRunnableTask;

/**
 * Clase para bajar ficheros en un thread independiente.
 * La idea (y parte del c�digo) est� tomada de EarthFlicks
 * @author Luis W. Sevilla (sevilla_lui@gva.es)
 * @see http://today.java.net/lpt/a/212
 */
public class URLRetrieveTask implements IRunnableTask {

    private boolean running, cancelled;

    private URLRequest request;

    private Vector listeners = new Vector();

    private RetrieveEvent event = new RetrieveEvent();

    private InputStream is;

    /**
	 * 
	 */
    public URLRetrieveTask(URLRequest request, RetrieveListener listener) {
        this.request = request;
        addRetrieveListener(listener);
        running = cancelled = false;
    }

    public void execute() {
        event.setType(RetrieveEvent.NOT_STARTED);
        fireEvent();
        cancelled = false;
        running = true;
        long t = System.currentTimeMillis();
        File f = new File(request.getFileName() + System.currentTimeMillis());
        while (f.exists()) {
            t++;
            f = new File(request.getFileName() + t);
        }
        URL url;
        try {
            event.setType(RetrieveEvent.CONNECTING);
            fireEvent();
            url = request.getUrl();
            request.setFileName(f.getAbsolutePath());
            System.out.println("downloading '" + url + "' to: " + f.getAbsolutePath());
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            byte[] buffer = new byte[1024 * 256];
            fireEvent();
            is = url.openStream();
            event.setType(RetrieveEvent.TRANSFERRING);
            fireEvent();
            if (!cancelled) {
                long readed = 0;
                for (int i = is.read(buffer); !cancelled && i > 0; i = is.read(buffer)) {
                    dos.write(buffer, 0, i);
                    readed += i;
                }
                dos.close();
            }
            if (cancelled) {
                System.out.println("download cancelled (" + url + ")");
                f.delete();
            } else {
                synchronized (this) {
                    RequestManager.getInstance().addDownloadedURLRequest(request, f.getAbsolutePath());
                }
            }
            running = false;
            if (cancelled) event.setType(RetrieveEvent.REQUEST_CANCELLED); else event.setType(RetrieveEvent.REQUEST_FINISHED);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            event.setType(RetrieveEvent.REQUEST_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            event.setType(RetrieveEvent.REQUEST_FAILED);
        }
        fireEvent();
    }

    private void fireEvent() {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            RetrieveListener listener = (RetrieveListener) it.next();
            listener.transferEventReceived(event);
        }
    }

    public void addRetrieveListener(RetrieveListener l) {
        if (l != null) listeners.add(l);
    }

    public void cancel() {
        cancelled = true;
        try {
            if (is != null) {
                is.close();
                is = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running && !cancelled;
    }

    public URLRequest getRequest() {
        return request;
    }

    public Vector getListeners() {
        return listeners;
    }

    public long getTaskTimeout() {
        return 30 * 1000;
    }
}
