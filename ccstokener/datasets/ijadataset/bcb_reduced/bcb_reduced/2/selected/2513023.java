package unico.net;

import unico.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.jmule.core.*;
import org.jmule.core.protocol.donkey.*;
import org.jmule.core.event.*;

public class DonkeyManager implements Runnable, ConnectionListener, SearchEventsListener {

    public static String status;

    private static final String[] mets = { "http://www.gruk.org/server.met", "http://peerates.net/peerates/certifiedservers.met", "http://peerates.net/peerates/trueservers.met" };

    public DonkeyManager() {
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        try {
            DonkeyProtocol p = DonkeyProtocol.getInstance();
            p.setEnabled(true);
            p.setAutoConnectEnabled(true);
            p.setMaxConcurrentConnections(5);
            SearchManager.getInstance().addSearchEventsListener(this);
            CoreManager core = new CoreManager();
            core.init();
            core.start();
            p.start();
        } catch (InitException e3) {
            e3.printStackTrace();
        }
    }

    public static void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
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
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        DonkeyProtocol.getInstance().setEnabled(true);
        Thread t = new Thread(new DonkeyManager());
        t.start();
        System.out.println("--->>>" + ConnectionManager.getInstance().numConnections());
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SearchQuery q = new SearchQuery("windows vista");
        SearchManager.getInstance().search(q);
        System.out.println(q.getResultCount());
    }

    public static int getNumConnections() {
        return ConnectionManager.getInstance().numConnections();
    }

    public void end() {
    }

    public Connection getNextNewConnection() {
        return null;
    }

    public boolean hasNewConnection() {
        return false;
    }

    public void init() throws Exception {
    }

    public void searchFired(SearchEvent e) {
        if (e.getQueryInfo().getQuery().getResultCount() > 0) {
            Iterator i = Unico.searches.iterator();
            while (i.hasNext()) {
                Search search = (Search) i.next();
                if (search.donkeySearchQuery.hashCode() == e.getQueryInfo().getQuery().hashCode()) {
                    ArrayList a = e.getQueryInfo().getResults();
                    if (a != null) {
                        Iterator j = a.iterator();
                        for (int x = 0; x <= search.donkeySearchIndex; x++) j.next();
                        while (j.hasNext()) {
                            org.jmule.core.SearchResult r = (org.jmule.core.SearchResult) j.next();
                            if (r.getFileHash() != null) {
                                search.donkeySearchIndex++;
                                search.addResult(new DonkeyResult(r));
                            } else System.out.println("file has nullo! risultato eliminato..");
                        }
                    }
                }
            }
        }
    }
}
