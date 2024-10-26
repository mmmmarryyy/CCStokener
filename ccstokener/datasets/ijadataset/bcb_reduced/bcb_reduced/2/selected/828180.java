package network;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import core.RSSFeed;
import core.errorhandler.*;

/**
 * Diese Klasse fuehrt den Download von XML-Daten eines oder mehrerer RSS-Feeds durch. Die Aufteilung der RSS-Feeds
 * geschieht durch den Network-Controller. 
 * 
 * @author Mattes Besuden, Patrik Kluge, Nadine Pollmann, Tobias Pude, Thomas Rix, Tobias Teichmann
 *
 */
public class Downloader extends Thread {

    private ErrorHandler errorHandler;

    private LinkedList<RSSFeed> jobs, doneJobs;

    private boolean runFlag = false;

    ;

    private boolean terminated = false;

    private int id;

    private boolean debug = false;

    /**
	 * Konstruktor der Klasse Downloader.
	 * 
	 * @param jobs Liste der zu bearbeitenden RSS-Feeds.
	 * @param id Nummer zur Identifikation der verschiedenen Threads.
	 * @param errorHandler Der ErrorHandler des Programms.
	 */
    public Downloader(LinkedList<RSSFeed> jobs, int id, ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        this.jobs = jobs;
        this.doneJobs = new LinkedList<RSSFeed>();
        this.id = id;
    }

    @Override
    public void run() {
        if (debug) System.out.println("downloader " + id + " gestartet");
        int count = 0;
        if (debug) System.out.println("Anzahl Jobs fuer Downloader " + id + ": " + jobs.size());
        this.runFlag = true;
        for (RSSFeed feed : jobs) {
            if (!runFlag) {
                break;
            }
            if (debug) System.out.println("id " + id + " count " + count);
            count++;
            try {
                URL url = new URL(feed.getUrl());
                URLConnection conn = url.openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] chunk = new byte[1024];
                int chunkSize;
                while ((chunkSize = is.read(chunk)) != -1) {
                    bos.write(chunk, 0, chunkSize);
                }
                String xmlString = bos.toString("UTF-8");
                if (xmlString.toLowerCase().contains("<?xml")) {
                    if (xmlString.toLowerCase().contains("encoding=\"iso-8859-1\"")) {
                        xmlString = bos.toString("ISO-8859-1");
                    }
                    feed.setXml(xmlString);
                    doneJobs.add(feed);
                } else {
                    errorHandler.receiveException(new RSSNetworkException(feed.getTitle() + "\n" + "Die URL (" + feed.getUrl() + ") verweist nicht auf eine XML-Datei!", false, true));
                }
                bos.flush();
                bos.close();
                is.close();
            } catch (SocketTimeoutException e) {
                errorHandler.receiveException(new RSSNetworkException(feed.getUsertitle() + "\nTimeout!" + "\nDie angegebene URL (" + feed.getUrl() + ") ist nicht erreichbar.", false, false));
            } catch (MalformedURLException e) {
                errorHandler.receiveException(new RSSNetworkException(feed.getUsertitle() + "\nFalsches URL-Format oder unbekanntes Uebertragungsprotokoll." + "\nBitte ueberpruefen sie die Feed-URL: " + feed.getUrl(), false, true));
            } catch (UnsupportedEncodingException e) {
                errorHandler.receiveException(new RSSNetworkException("Die angegebene Formatkodierung wird nicht unterstuetzt.", false, false));
            } catch (UnknownHostException e) {
                errorHandler.receiveException(new RSSNetworkException(feed.getUsertitle() + "\nDer angegebene Host (" + feed.getUrl() + ") konnte nicht gefunden werden." + "\nBitte ueberpruefen sie ihre Internetverbindung.", false, true));
            } catch (IOException e) {
                errorHandler.receiveException(new RSSNetworkException(feed.getUsertitle() + "\nEin-Ausgabefehler, wahrscheinlich wurde die Internetverbindung unterbrochen.", false, false));
            } catch (Exception e) {
                errorHandler.receiveException(new RSSNetworkException(feed.getUsertitle() + "\nEs ist ein unbekannter Fehler aufgetreten.", true, true));
            }
        }
        runFlag = false;
        terminated = true;
    }

    /**
	 * Diese Funktion setzt das Flag runFlag auf false und stoppt dadurch den Thread.
	 */
    public void stopDownload() {
        this.runFlag = false;
    }

    /**
	 * Diese Funktion gibt das Flag runFlag zurueck.
	 * 
	 * @return true, falls Thread gestoppt, sonst true.
	 */
    public boolean getRunFlag() {
        return runFlag;
    }

    /**
	 * Diese Funktion gibt das Flag terminated zurueck.
	 * 
	 * @return true, falls Thread planmae�ig terminiert ist, sonst false.
	 */
    public boolean isTerminated() {
        return terminated;
    }

    /**
	 * Diese Funktion gibt die bearbeitete Liste der RSS-Feeds zurueck. 
	 * 
	 * @return LinkedList<RSSFeed> Liste der bearbeiteten Feeds.
	 */
    public LinkedList<RSSFeed> getDoneJobs() {
        if (!runFlag) {
            return doneJobs;
        }
        return null;
    }

    /**
	 * Diese Funktion gibt die Nummer des Threads zurueck.
	 * 
	 * @return Nummer des Threads.
	 */
    public int getID() {
        return id;
    }
}
