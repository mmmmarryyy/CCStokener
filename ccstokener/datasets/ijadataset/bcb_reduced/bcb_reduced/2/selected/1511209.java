package hu.scytha.update;

import hu.scytha.common.MessageSystem;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

class AvailableUpdatesRunnable implements IRunnableWithProgress {

    private List<IUpdateable> listOfUpdates;

    private String[] mirrors;

    private String fConnection;

    private HashMap<String, String> mirrorMap = new HashMap<String, String>();

    public AvailableUpdatesRunnable(String connection) {
        this.fConnection = connection.endsWith("/") ? connection : connection + "/";
    }

    public List<IUpdateable> getUpdates() {
        return listOfUpdates;
    }

    public String[] getMirrors() {
        return mirrors;
    }

    public HashMap<String, String> getMirrorMap() {
        return mirrorMap;
    }

    @SuppressWarnings("unchecked")
    public void run2() {
        InputStream istream = null;
        try {
            URL url = new URL(fConnection + IUpdateable.UPDATE_XML);
            URLConnection conn = url.openConnection();
            istream = conn.getInputStream();
            UpdateXmlReader reader = new UpdateXmlReader(istream);
            listOfUpdates = reader.getUpdateableModules();
            url = new URL(fConnection + IUpdateable.MIRROR_LIST);
            conn = url.openConnection();
            istream = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(istream));
            String line = "";
            ArrayList<String> al = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    StringTokenizer st = new StringTokenizer(line, "=");
                    if (st.hasMoreTokens()) {
                        String mirror = st.nextToken();
                        if (st.hasMoreTokens()) {
                            al.add(mirror);
                            mirrorMap.put(mirror, st.nextToken());
                        }
                    }
                }
            }
            mirrors = al.toArray(new String[0]);
        } catch (MalformedURLException e) {
            new RuntimeException(e);
        } catch (IOException e) {
            new RuntimeException(e);
        } catch (Exception e) {
            MessageSystem.logException("", getClass().getName(), "run", null, e);
            new RuntimeException(e);
        } finally {
            try {
                if (istream != null) {
                    istream.close();
                }
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                monitor.beginTask(Messages.getString("AvailableUpdatesRunnable.search.for.update"), IProgressMonitor.UNKNOWN);
            }

            ;
        });
        InputStream istream = null;
        try {
            URL url = new URL(fConnection + IUpdateable.UPDATE_XML);
            URLConnection conn = url.openConnection();
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            istream = conn.getInputStream();
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            UpdateXmlReader reader = new UpdateXmlReader(istream);
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            listOfUpdates = reader.getUpdateableModules();
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            url = new URL(fConnection + IUpdateable.MIRROR_LIST);
            conn = url.openConnection();
            istream = conn.getInputStream();
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(istream));
            String line = "";
            ArrayList<String> al = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    StringTokenizer st = new StringTokenizer(line, "=");
                    if (st.hasMoreTokens()) {
                        String mirror = st.nextToken();
                        if (st.hasMoreTokens()) {
                            al.add(mirror);
                            mirrorMap.put(mirror, st.nextToken());
                        }
                    }
                }
            }
            mirrors = al.toArray(new String[0]);
        } catch (InterruptedException e) {
        } catch (MalformedURLException e) {
            new RuntimeException(e);
        } catch (IOException e) {
            new RuntimeException(e);
        } catch (Exception e) {
            MessageSystem.logException("", getClass().getName(), "run", null, e);
            new RuntimeException(e);
        } finally {
            try {
                if (istream != null) {
                    istream.close();
                }
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                monitor.done();
            }
        });
    }
}
