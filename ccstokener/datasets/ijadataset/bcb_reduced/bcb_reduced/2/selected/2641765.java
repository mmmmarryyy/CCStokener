package de.uos.virtuos.virtpresenter.verwalter.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.uos.virtuos.virtpresenter.verwalter.protokolle.EnhancedPodcastServerClient;
import de.uos.virtuos.virtpresenter.verwalter.protokolle.VideoKonvertierungsServerClient;
import de.uos.virtuos.virtpresenter.verwalter.server.EnhancedPodcastServer;
import de.uos.virtuos.virtpresenter.verwalter.server.NoServerAvailableException;
import de.uos.virtuos.virtpresenter.verwalter.server.Server;
import de.uos.virtuos.virtpresenter.verwalter.server.VideoKonvertierungsServer;
import de.uos.virtuos.virtpresenter.verwalter.server.VideoServer;
import de.uos.virtuos.virtpresenter.verwalter.tools.DatabaseConnection;
import de.uos.virtuos.virtpresenter.verwalter.tools.Zeiten;

public class Job implements Comparable {

    String id = null;

    String seminarID = null;

    Seminar seminar = null;

    String generatorXML = null;

    Document doc = null;

    String listenerUrl = null;

    Date aufnahmedatum = null;

    String titel = null;

    String beschreibung = null;

    String dozent = null;

    long videoDauer = -1;

    long videoGesamtdauer = -1;

    long videoNachlauf = -1;

    String dateinameVideo = null;

    VideoServer videoserver = null;

    String dateinamePresentation;

    Server folienKonvertierungsServer = null;

    int statusFolienKonvertierung = -1;

    Server webserver;

    Server red5Server;

    Server podcastServer;

    boolean loaded = false;

    public Job(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDateinamePresentation() {
        return dateinamePresentation;
    }

    public void setDateinamePresentation(String dateinamePresentation) {
        this.dateinamePresentation = dateinamePresentation;
        if (videoGesamtdauer < 0 && videoserver != null) videoGesamtdauer = Zeiten.stunden2ms(videoserver.getVideoduration(dateinameVideo));
    }

    public String getDateinameVideo() {
        if (dateinameVideo == null) dateinameVideo = id + ".mpg";
        return dateinameVideo;
    }

    public void setDateinameVideo(String dateinameVideo) {
        this.dateinameVideo = dateinameVideo;
    }

    public Server getRed5Server() {
        if (red5Server == null) {
            if (seminar == null) seminar = Seminar.readSeminar(getSeminarID());
            setRed5Server(Server.readServer(seminar.getPreferedRed5Server()));
        }
        return red5Server;
    }

    public void setRed5Server(Server red5Server) {
        if (red5Server == null) {
            System.out.println("Kein Red5-Server zugewiesen");
            return;
        }
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (database.readActionId(id, red5Server.getId()) == 0) {
            database.statusSpeichern(id, 110, red5Server.getId(), "Streaming Server zugewiesen");
        }
        this.red5Server = red5Server;
    }

    public long getVideoDauer() {
        return videoDauer;
    }

    public void setVideoDauer(long videoDauer) {
        this.videoDauer = videoDauer;
    }

    public String getVideoDauerZeit() {
        return Zeiten.ms2stunden(videoDauer);
    }

    public long getVideoNachlauf() {
        return videoNachlauf;
    }

    public String getVideoNachlaufZeit() {
        return Zeiten.ms2stunden(videoNachlauf);
    }

    public void setVideoNachlauf(long videoNachlauf) {
        this.videoNachlauf = videoNachlauf;
    }

    public Server getVideoserver() {
        return videoserver;
    }

    public void setVideoserver(VideoServer videoserver) {
        this.videoserver = videoserver;
    }

    public long updateVideoGesamtdauer() {
        if (videoserver != null && dateinameVideo != null) return Zeiten.stunden2ms(videoserver.getVideoduration(dateinameVideo));
        return -1;
    }

    public Server getWebserver() {
        if (webserver == null) {
            if (seminar == null) seminar = Seminar.readSeminar(getSeminarID());
            setWebserver(Server.readServer(seminar.getPreferedWebserver()));
        }
        return webserver;
    }

    public void setWebserver(Server webserver) {
        if (webserver == null) {
            System.out.println("Kein webserver zugewiesen");
            return;
        }
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (database.readActionId(id, webserver.getId()) == 0) {
            database.statusSpeichern(id, 110, webserver.getId(), "Webserver Server zugewiesen");
        }
        this.webserver = webserver;
    }

    public long getVideoGesamtdauer() {
        if (videoGesamtdauer < 0) videoGesamtdauer = updateVideoGesamtdauer();
        return videoGesamtdauer;
    }

    public void setVideoGesamtdauer(long videoGesamtdauer) {
        this.videoGesamtdauer = videoGesamtdauer;
    }

    public String getVideoGesamtDauerZeit() {
        return Zeiten.ms2stunden(videoGesamtdauer);
    }

    public String getVideoStartZeit() {
        if (videoGesamtdauer <= 0) videoGesamtdauer = updateVideoGesamtdauer();
        if (videoGesamtdauer <= 0) return (Zeiten.ms2stunden(0));
        return Zeiten.ms2stunden(videoGesamtdauer - videoDauer);
    }

    public long getVideoStart() {
        if (videoGesamtdauer <= 0) updateVideoGesamtdauer();
        if (videoGesamtdauer <= 0) return 0;
        return videoGesamtdauer;
    }

    public void setStatus(String dienstId, int code) {
        if (dienstId.equalsIgnoreCase("videokonverter")) {
            setStatusVideoKonvertierung(code);
        } else if (dienstId.equalsIgnoreCase("folienkonverter")) {
        }
    }

    public void setStatus(int status, int serverId) {
        setStatus(status, serverId, 0);
    }

    public void setStatus(int status, int serverId, int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        String message = "";
        switch(status) {
            case 200:
                message = "Videokonvertierung problemlos beendet";
                break;
            case 411:
                message = "MPEG2 Video konnte nicht vom Server geladen werden";
                break;
            case 412:
                message = "Video konnte nicht konvertiert werden";
                break;
            case 413:
                message = "Video konnte nicht nachbearbeitet werden";
                break;
            case 414:
                message = "Video konnte nicht auf Server kopiert werden";
                break;
        }
        database.updateStatus(readActionId(serverId, codecId), status, message);
        int freigabe = readFreigabe();
        if (isVideoDone(codecId) && !finished(codecId)) {
            if (codecId != 1) doFreigabe(freigabe, codecId);
            database.statusSpeichern(id, 210, codecId, 1, "virtPresenter-Aufzeichnung erfolgreich abgeschlossen");
        }
        if (isVirtpresenterDone() && !finished(0)) {
            doFreigabe(freigabe, codecId);
            saveVideoXMLOnWebserver();
            saveLecturerecordingsXMLOnWebserver();
            database.statusSpeichern(id, 210, 0, 1, "virtPresenter-Aufzeichnung erfolgreich abgeschlossen");
        }
        if (codecId == 5 && isVideoDone(codecId)) {
            startEnhancedPodcastProduction();
        }
    }

    public boolean startEnhancedPodcastProduction() {
        if (seminar == null) seminar = Seminar.readSeminar(getSeminarID());
        LinkedList<Encoding> formate = seminar.getFormate();
        Encoding format = null;
        for (int i = 0; i < formate.size(); i++) {
            if (formate.get(i).getCodecID() == 7) {
                format = formate.get(i);
            }
        }
        if (format == null) return false;
        try {
            EnhancedPodcastServer epServer = Datenspeicher.getEnhancedPodcastServer().getEnhancedPodcastServer();
            EnhancedPodcastServerClient client = epServer.getClient();
            DatabaseConnection database = Datenspeicher.getDatabase();
            if (!database.isConnected()) database.connect();
            database.statusSpeichern(id, 100, epServer.getId(), format.getCodecID(), "Von Job gestartet");
            boolean status = true;
            if (!client.isConnected()) if (!client.connect()) status = false;
            if (!client.isAuthentifiziert()) if (!client.anmeldung()) status = false;
            if (!client.add(id, format)) status = false;
            if (!client.quit()) status = false;
            client.close();
            if (!status) System.err.println("Warnung Probleme bei Kommunikation mit EnhancedPodcastServer. Format: " + format.getConverterTag());
        } catch (NoServerAvailableException e) {
            System.err.println("Kein Server zur EnhancedPodcast Erstellung verf�gbar. Format: " + format.getConverterTag());
            return false;
        }
        return true;
    }

    void doFreigabe(int freigabe, int codecId) {
        switch(freigabe) {
            case 1:
            case 2:
            case 5:
                if (!Datenspeicher.getVaderConnect().jobIsInVader(this.getId(), codecId)) {
                    Datenspeicher.getVaderConnect().veranstaltungEintragen(this, codecId);
                    System.out.println("JOB: Eingetragen in VADER, id:" + this.getId() + ", format: " + codecId);
                } else {
                    Datenspeicher.getVaderConnect().veranstaltungAendern(this, codecId);
                    System.out.println("JOB: Upgedatet in VADER, id:" + this.getId() + ", format: " + codecId);
                }
                break;
        }
    }

    /**
	 * �berpr�ft, ob die Videokonvertierung f�r das gew�nschte Format erfolgreich beendet wurde UND ob die
	 * @param codecId Die Codec-Id f�r die getestet werden soll.
	 * @return true, wenn Job f�r diesen Codec erfolgreich abgeschlossen wurde.
	 */
    public boolean finished(int codecId) {
        return readIsFinished(codecId);
    }

    /**
	 * �berpr�ft, ob die Videokonvertierung f�r das gew�nschte Format erfolgreich beendet wurde
	 * @param codecId Die Codec-Id f�r die getestet werden soll.
	 * @return true, wenn Konvertierung erfolgreich abgeschlossen wurde.
	 */
    public boolean isVideoDone(int codecId) {
        int status = readStatusForFormat(codecId);
        if (status >= 200 && status < 300) return true;
        return false;
    }

    public boolean isVirtpresenterDone() {
        return readIsFinished(1);
    }

    public void setStatusVideoKonvertierung(int statusVideoKonvertierung) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        if (statusVideoKonvertierung == 0) {
            database.updateStatus(readActionId(0, 0), 200, "Videokonvertierung problemlos beendet");
        }
        String text = "";
        try {
            URL url = new URL("http://localhost:8080/virtPresenterVerwalter/videofile.jsp?id=" + this.getId());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String zeile = "";
            while ((zeile = in.readLine()) != null) {
                text += zeile + "\n";
            }
            in.close();
            http.disconnect();
            saveVideoXMLOnWebserver(text);
            database.statusSpeichern(id, 210, 0, "Auftrag beendet");
            saveLecturerecordingsXMLOnWebserver();
            if (seminar == null) seminar = Seminar.readSeminar(seminarID);
            if (seminar != null && seminar.getStandardFreigabe() != 0) {
                saveFreigabe(seminar.getStandardFreigabe(), "system");
                if (seminar.getStandardFreigabe() != 3 && seminar.getStandardFreigabe() != 4) {
                    if (Datenspeicher.getVaderConnect().jobIsInVader(id)) Datenspeicher.getVaderConnect().veranstaltungAendern(this); else Datenspeicher.getVaderConnect().veranstaltungEintragen(this);
                } else if (Datenspeicher.getVaderConnect().jobIsInVader(id)) Datenspeicher.getVaderConnect().veranstaltungLoeschen(id);
            }
            System.err.println("Job " + this.getId() + " erfolgreich bearbeitet!");
        } catch (MalformedURLException e) {
            System.err.println("Job " + this.getId() + ": Konnte video.xml oder lecturerecordings.xml nicht erstellen. Verbindung konnte nicht aufgebaut werden.");
        } catch (IOException e) {
            System.err.println("Job " + this.getId() + ": Konnte video.xml oder lecturerecordings.xml nicht erstellen. Konnte Daten nicht lesen/schreiben.");
        }
    }

    public boolean saveVideoXMLOnWebserver() {
        String text = "";
        boolean erg = false;
        try {
            URL url = new URL("http://localhost:8080/virtPresenterVerwalter/videofile.jsp?id=" + this.getId());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String zeile = "";
            while ((zeile = in.readLine()) != null) {
                text += zeile + "\n";
            }
            in.close();
            http.disconnect();
            erg = saveVideoXMLOnWebserver(text);
            System.err.println("Job " + this.getId() + " erfolgreich bearbeitet!");
        } catch (MalformedURLException e) {
            System.err.println("Job " + this.getId() + ": Konnte video.xml nicht erstellen. Verbindung konnte nicht aufgebaut werden.");
            return false;
        } catch (IOException e) {
            System.err.println("Job " + this.getId() + ": Konnte video.xml nicht erstellen. Konnte Daten nicht lesen/schreiben.");
            return false;
        }
        return erg;
    }

    public boolean saveVideoXMLOnWebserver(String text) {
        boolean error = false;
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(this.getWebserver().getUrl());
            System.out.println("Connected to " + this.getWebserver().getUrl() + ".");
            System.out.print(ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return false;
            }
            if (!ftp.login(this.getWebserver().getFtpBenutzer(), this.getWebserver().getFtpPasswort())) {
                System.err.println("FTP server: Login incorrect");
            }
            String tmpSeminarID = this.getSeminarID();
            if (tmpSeminarID == null) tmpSeminarID = "unbekannt";
            try {
                ftp.changeWorkingDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/" + this.getId() + "/data");
            } catch (Exception e) {
                ftp.makeDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/" + this.getId() + "/data");
                ftp.changeWorkingDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/" + this.getId() + "/data");
            }
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ByteArrayInputStream videoIn = new ByteArrayInputStream(text.getBytes());
            ftp.enterLocalPassiveMode();
            ftp.storeFile("video.xml", videoIn);
            videoIn.close();
            ftp.logout();
            ftp.disconnect();
        } catch (IOException e) {
            System.err.println("Job " + this.getId() + ": Datei video.xml konnte nicht auf Webserver kopiert werden.");
            error = true;
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return error;
    }

    public boolean saveLecturerecordingsXMLOnWebserver() {
        boolean error = false;
        FTPClient ftp = new FTPClient();
        String lecture = "";
        try {
            URL url = new URL("http://localhost:8080/virtPresenterVerwalter/lecturerecordings.jsp?seminarid=" + this.getSeminarID());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String zeile = "";
            while ((zeile = in.readLine()) != null) {
                lecture += zeile + "\n";
            }
            in.close();
            http.disconnect();
        } catch (Exception e) {
            System.err.println("Konnte lecturerecordings.xml nicht lesen.");
        }
        try {
            int reply;
            ftp.connect(this.getWebserver().getUrl());
            System.out.println("Connected to " + this.getWebserver().getUrl() + ".");
            System.out.print(ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return false;
            }
            if (!ftp.login(this.getWebserver().getFtpBenutzer(), this.getWebserver().getFtpPasswort())) {
                System.err.println("FTP server: Login incorrect");
            }
            String tmpSeminarID = this.getSeminarID();
            if (tmpSeminarID == null) tmpSeminarID = "unbekannt";
            try {
                ftp.changeWorkingDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/");
            } catch (Exception e) {
                ftp.makeDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/");
                ftp.changeWorkingDirectory(this.getWebserver().getDefaultPath() + "/" + tmpSeminarID + "/lectures/");
            }
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ByteArrayInputStream lectureIn = new ByteArrayInputStream(lecture.getBytes());
            System.err.println("FTP Verzeichnis: " + ftp.printWorkingDirectory());
            ftp.storeFile("lecturerecordings.xml", lectureIn);
            lectureIn.close();
            ftp.logout();
            ftp.disconnect();
        } catch (IOException e) {
            System.err.println("Job " + this.getId() + ": Datei lecturerecordings.xml konnte nicht auf Webserver kopiert werden.");
            error = true;
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Job " + this.getId() + ": Datei lecturerecordings.xml konnte nicht auf Webserver kopiert werden. (Kein Webserver zugewiesen)");
            error = true;
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return error;
    }

    public String loadGeneratorXML() {
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(this.getFolienKonvertierungsServer().getUrl());
            System.out.println("Connected to " + this.getFolienKonvertierungsServer().getUrl() + ".");
            System.out.print(ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return null;
            }
            if (!ftp.login(this.getFolienKonvertierungsServer().getFtpBenutzer(), this.getFolienKonvertierungsServer().getFtpPasswort())) {
                System.err.println("FTP server: Login incorrect");
            }
            String path;
            if (this.getFolienKonvertierungsServer().getDefaultPath().length() > 0) {
                path = "/" + this.getFolienKonvertierungsServer().getDefaultPath() + "/" + this.getId() + "/";
            } else {
                path = "/" + this.getId() + "/";
            }
            if (!ftp.changeWorkingDirectory(path)) System.err.println("Konnte Verzeichnis nicht wechseln: " + path);
            System.err.println("Arbeitsverzeichnis: " + ftp.printWorkingDirectory());
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            InputStream inStream = ftp.retrieveFileStream("generator.xml");
            if (inStream == null) {
                System.err.println("Job " + this.getId() + ": Datei generator.xml wurde nicht gefunden");
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            generatorXML = "";
            String zeile = "";
            while ((zeile = in.readLine()) != null) {
                generatorXML += zeile + "\n";
            }
            in.close();
            ftp.logout();
            ftp.disconnect();
        } catch (IOException e) {
            System.err.println("Job " + this.getId() + ": Datei generator.xml konnte nicht vom Webserver kopiert werden.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Job " + this.getId() + ": Datei generator.xml konnte nicht vom Webserver kopiert werden.");
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        if (generatorXML != null && generatorXML.length() == 0) {
            generatorXML = null;
        }
        return generatorXML;
    }

    public String getSeminarID() {
        if (loaded) return seminarID;
        if (seminarID == null) {
            seminarID = getFromGeneratorXML("message/SeminarID");
        }
        return seminarID;
    }

    public String getFromGeneratorXML(String tag) {
        String value = null;
        if (generatorXML == null) loadGeneratorXML();
        if (doc == null && generatorXML != null) {
            ByteArrayInputStream xmlIn = new ByteArrayInputStream(generatorXML.getBytes());
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder builder = null;
            try {
                builder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                System.err.println("Read generator.xml: DokumentBuilder could not be created");
                e.printStackTrace();
                return null;
            }
            doc = null;
            try {
                doc = builder.parse(xmlIn);
            } catch (SAXException e) {
                System.err.println("Read generator.xml: Dokument could not be build (Parser Error)");
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                System.err.println("Read generator.xml: Dokument could not be build (File not Found)");
                e.printStackTrace();
                return null;
            }
        }
        if (doc != null) {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            try {
                NodeList nodes = ((NodeList) xpath.evaluate(tag, doc, XPathConstants.NODESET));
                if (nodes != null && nodes.getLength() > 0) {
                    Node node = nodes.item(0);
                    value = node.getTextContent();
                }
            } catch (XPathExpressionException e) {
                System.err.println("Read generator.xml: Could not evaluate expression" + tag);
                e.printStackTrace();
            }
        }
        return value;
    }

    public void setSeminarID(String seminarID) {
        this.seminarID = seminarID;
    }

    public boolean saveJob() {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) if (!database.connect()) return false;
        if (seminarID == null) getSeminarID();
        if (videoGesamtdauer <= 0) getVideoGesamtdauer();
        if (dozent == null) getDozent();
        if (beschreibung == null) getBeschreibung();
        if (titel == null) getTitel();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        String command = "INSERT INTO jobs (job_id";
        String values = ") VALUES ('" + id + "'";
        if (seminarID != null) {
            command += ", seminar_id";
            values += ", '" + seminarID + "'";
        }
        if (videoGesamtdauer >= 0) {
            command += ", videogesamtdauer";
            values += ", '" + videoGesamtdauer + "'";
        }
        if (videoDauer >= 0) {
            command += ", videodauer";
            values += ", '" + videoDauer + "'";
        }
        if (videoNachlauf >= 0) {
            command += ", videonachlauf";
            values += ", '" + videoNachlauf + "'";
        }
        if (dateinamePresentation != null) {
            command += ", paesentationsname";
            values += ", '" + dateinamePresentation + "'";
        }
        if (beschreibung != null) {
            command += ", beschreibung";
            values += ", '" + beschreibung + "'";
        }
        if (listenerUrl != null) {
            command += ", listener_url";
            values += ", '" + listenerUrl + "'";
        }
        if (videoserver != null) {
            command += ", rekorder_id";
            values += ", '" + videoserver.getId() + "'";
        }
        if (folienKonvertierungsServer != null) {
            command += ", generator_id";
            values += ", '" + folienKonvertierungsServer.getId() + "'";
        }
        command += ", aufnahmedatum";
        values += ", '" + getAufnahmedatum().toString() + "'";
        if (titel != null) {
            command += ", titel";
            values += ", '" + titel + "'";
        }
        if (dozent != null) {
            command += ", dozent";
            values += ", '" + getDozent() + "'";
        }
        String query = command + values + ")";
        try {
            stmnt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("Error saving Job " + id);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean updateSavedJob() {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) if (!database.connect()) return false;
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        boolean komma = false;
        String query = "UPDATE jobs SET ";
        if (seminarID != null) {
            query += "seminar_id = '" + seminarID + "' ";
            komma = true;
        }
        if (videoGesamtdauer >= 0) {
            if (komma) query += " , ";
            query += "videogesamtdauer = '" + videoGesamtdauer + "' ";
            komma = true;
        }
        if (videoDauer >= 0) {
            if (komma) query += " , ";
            query += "videodauer = '" + videoDauer + "' ";
            komma = true;
        }
        if (videoNachlauf >= 0) {
            if (komma) query += " , ";
            query += "videonachlauf = '" + videoNachlauf + "' ";
            komma = true;
        }
        if (dateinamePresentation != null) {
            if (komma) query += " , ";
            query += "paesentationsname = '" + dateinamePresentation + "' ";
            komma = true;
        }
        if (beschreibung != null) {
            if (komma) query += " , ";
            query += "beschreibung = '" + beschreibung + "' ";
            komma = true;
        }
        if (listenerUrl != null) {
            if (komma) query += " , ";
            query += "listener_url = '" + listenerUrl + "' ";
            komma = true;
        }
        if (aufnahmedatum != null) {
            if (komma) query += " , ";
            query += "aufnahmedatum = '" + aufnahmedatum.toString() + "' ";
            komma = true;
        }
        if (titel != null) {
            if (komma) query += " , ";
            query += "titel = '" + titel + "' ";
            komma = true;
        }
        if (dozent != null) {
            if (komma) query += " , ";
            query += "dozent = '" + getDozent() + "' ";
        }
        query += " WHERE job_id = '" + id + "'";
        try {
            System.out.println("Update Job Query: " + query);
            stmnt.executeUpdate(query);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    public static Job readJob(String jobId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Job newJob = null;
        Statement stmnt = database.getStatement();
        if (stmnt == null) return null;
        String query = "SELECT * FROM jobs WHERE job_id = '" + jobId + "'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return null;
            newJob = new Job(jobId);
            newJob.seminarID = result.getString("seminar_id");
            newJob.videoGesamtdauer = result.getLong("videogesamtdauer");
            newJob.videoDauer = result.getLong("videodauer");
            newJob.videoNachlauf = result.getLong("videonachlauf");
            newJob.dateinamePresentation = result.getString("praesentationsname");
            newJob.beschreibung = result.getString("beschreibung");
            newJob.listenerUrl = result.getString("listener_url");
            newJob.videoserver = VideoServer.readServer(result.getInt("rekorder_id"));
            newJob.folienKonvertierungsServer = VideoServer.readServer(result.getInt("generator_id"));
            newJob.aufnahmedatum = result.getDate("aufnahmedatum");
            newJob.titel = result.getString("titel");
            newJob.dozent = result.getString("dozent");
            stmnt.close();
            result.close();
            stmnt = database.getStatement();
            result = stmnt.executeQuery("SELECT `server_id`  FROM  `status`  WHERE `job_id`  =  '" + newJob.getId() + "' AND  `server_id`  IN (  SELECT  `server_id`  FROM  `servers`  WHERE  `funktion_id`  =  '5' )");
            if (result.next()) newJob.red5Server = Server.readServer(result.getInt("server_id"));
            stmnt.close();
            result.close();
            stmnt = database.getStatement();
            result = stmnt.executeQuery("SELECT `server_id`  FROM  `status`  WHERE `job_id`  =  '" + newJob.getId() + "' AND  `server_id`  IN (  SELECT  `server_id`  FROM  `servers`  WHERE  `funktion_id`  =  '4' )");
            if (result.next()) newJob.webserver = Server.readServer(result.getInt("server_id"));
            stmnt.close();
            result.close();
        } catch (SQLException e) {
            System.err.println("Error reading Job " + jobId);
            e.printStackTrace();
            return null;
        }
        newJob.loaded = true;
        return newJob;
    }

    public int readVideokonvertierungsServerID(int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return 0;
        int erg = 0;
        ResultSet result;
        String query = "SELECT `server_id`  FROM  `status`  WHERE `job_id`  =  '" + id + "' AND `codec_id` = '" + codecId + "' AND  `server_id`  IN (  SELECT  `server_id`  FROM  `servers`  WHERE  `funktion_id`  =  '2' )";
        try {
            result = stmnt.executeQuery(query);
            if (result.next()) erg = result.getInt("server_id");
            stmnt.close();
            result.close();
        } catch (SQLException e) {
            System.err.println("Error reading ServerId: " + query);
            e.printStackTrace();
            return 0;
        }
        return erg;
    }

    public String getAufnahmedatumDeutsch() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(getAufnahmedatum().getTime());
        return "" + gc.get(Calendar.DAY_OF_MONTH) + "." + (gc.get(Calendar.MONTH) + 1) + "." + gc.get(Calendar.YEAR);
    }

    public String getAufnahmedatumELAN() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(getAufnahmedatum().getTime());
        String result = "" + gc.get(Calendar.YEAR);
        if (gc.get(Calendar.MONTH) < 9) result += "0";
        result += (gc.get(Calendar.MONTH) + 1);
        if (gc.get(Calendar.DAY_OF_MONTH) < 10) result += "0";
        result += gc.get(Calendar.DAY_OF_MONTH);
        return result;
    }

    public Date getAufnahmedatum() {
        if (aufnahmedatum == null) {
            StringTokenizer st = new StringTokenizer(id, "_");
            GregorianCalendar cal = new GregorianCalendar();
            String jahr = st.nextToken();
            String datum = "";
            while (!(jahr.startsWith("2") || jahr.startsWith("1")) && st.hasMoreElements()) {
                jahr = st.nextToken();
            }
            datum += jahr;
            try {
                String monat = st.nextToken();
                datum += "-" + monat;
                String tag = st.nextToken();
                datum += "-" + tag;
                cal.set(Integer.parseInt(jahr), Integer.parseInt(monat) - 1, Integer.parseInt(tag));
                System.err.println("Datum gesetzt: " + datum);
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen des Datums: " + datum);
            }
            aufnahmedatum = new Date(cal.getTimeInMillis());
        }
        return aufnahmedatum;
    }

    public int readActionId(int serverId, int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return 0;
        String query = "SELECT  `action_id`  FROM  `status`  WHERE `job_id`  =  '" + id + "' AND  `server_id` = '" + serverId + "' AND codec_id = '" + codecId + "'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return 0;
            return result.getInt("action_id");
        } catch (SQLException e) {
            System.err.println("Fehler bei der Ermittlung des Konvertierungs-Status " + id);
            e.printStackTrace();
            return 0;
        }
    }

    public String readStatus(int funktionId) {
        return readStatus(funktionId, 0);
    }

    public String readStatus(int funktionId, int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return null;
        String query = "SELECT * FROM  `status`  WHERE `job_id`  =  '" + id + "' AND  `server_id`  IN (  SELECT  `server_id`  FROM  `servers`  WHERE  `funktion_id`  =  '" + funktionId + "' ) AND codec_id = '" + codecId + "'";
        String answer = "";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return null;
            answer = database.readStatuscodeText(result.getInt("status_id"));
            if (result.getString("message") != null) answer += ", " + result.getString("message");
            if (result.getString("zeit") != null) answer += ", " + result.getTimestamp("zeit").toString();
        } catch (SQLException e) {
            System.err.println("Fehler bei der Ermittlung des Status f�r Ausgabe" + id);
            e.printStackTrace();
            return null;
        }
        return answer;
    }

    public int readStatusForFormat(int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return 0;
        String query = "SELECT status_id FROM  `status`  WHERE `job_id` = '" + id + "' AND codec_id = '" + codecId + "'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return 0;
            return result.getInt("status_id");
        } catch (SQLException e) {
            System.err.println("Fehler bei der Ermittlung des Status f�r Ausgabe. Query" + query);
            e.printStackTrace();
            return 0;
        }
    }

    public boolean readIsFinished() {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        String query = "SELECT * FROM  `status`  WHERE `job_id`  =  '" + id + "' AND  `status_id`  = '210' AND codec_id IN ('0', '1')";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return false;
            return true;
        } catch (SQLException e) {
            System.err.println("Fehler bei der Ermittlung ob job beendet ist: " + id);
            e.printStackTrace();
            return false;
        }
    }

    public boolean readIsFinished(int codecId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        String query = "SELECT * FROM  `status`  WHERE `job_id`  =  '" + id + "' AND  `status_id`  = '210' AND codec_id = '" + codecId + "'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return false;
            return true;
        } catch (SQLException e) {
            System.err.println("Fehler bei der Ermittlung ob job beendet ist: " + id);
            e.printStackTrace();
            return false;
        }
    }

    public String readKonvertierungsStatus(int codecId) {
        return readStatus(2, codecId);
    }

    public String readGeneratorStatus() {
        return readStatus(3);
    }

    public Server getFolienKonvertierungsServer() {
        if (folienKonvertierungsServer == null) {
            setFolienKonvertierungsServer(Server.readServer(Seminar.readSeminar(seminarID).getPreferedGeneratorServer()));
        }
        return folienKonvertierungsServer;
    }

    public void setFolienKonvertierungsServer(Server folienKonvertierungsServer) {
        if (folienKonvertierungsServer == null) {
            System.out.println("Kein FolienKonvertierungsServer zugewiesen");
            return;
        }
        this.folienKonvertierungsServer = folienKonvertierungsServer;
    }

    public int compareTo(Object o) {
        if (o instanceof Job) {
            int value = this.getAufnahmedatum().compareTo(((Job) o).getAufnahmedatum());
            if (value == 0) value = this.getId().compareTo(((Job) o).getId());
            return value;
        }
        return 0;
    }

    public String getBeschreibung() {
        if (loaded) return beschreibung;
        if (beschreibung == null) {
            beschreibung = getFromGeneratorXML("message/Anmerkung");
        }
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getDozent() {
        if (loaded) return dozent;
        if (dozent == null) {
            dozent = getFromGeneratorXML("message/Dozent");
        }
        return dozent;
    }

    public void setDozent(String dozent) {
        this.dozent = dozent;
    }

    public String getTitel() {
        if (loaded) {
            if (titel == null) return "unbekannt"; else return titel;
        }
        if (titel == null) {
            titel = getFromGeneratorXML("message/Titel");
        }
        if (titel == null) return "unbekannt";
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public void setAufnahmedatum(Date aufnahmedatum) {
        this.aufnahmedatum = aufnahmedatum;
    }

    /**
	 * Liest die Freigabe f�r den aktuellen Job aus der Datenbank
	 * @return code f�r Freigabe
	 */
    public int readFreigabe() {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return 0;
        String query = "SELECT freigabe_id FROM freigabe WHERE job_id = '" + id + "'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            if (!result.next()) return 0;
            return result.getInt("freigabe_id");
        } catch (SQLException e) {
            System.err.println("Fehler beim laden der Freigabdaten f�r : " + id);
            e.printStackTrace();
            return 0;
        }
    }

    /**
	 * Liest die Freigabe f�r den aktuellen Job aus der Datenbank.
	 * Falls kein Wert gesetzt ist wird der Default-Wert des Seminars genommen und gespeichert;
	 * @return
	 */
    public int getFreigabe() {
        int freigabe = readFreigabe();
        if (seminar == null) seminar = Seminar.readSeminar(seminarID);
        if (freigabe == 0 && seminar != null) {
            int seminarfreigabe = seminar.getStandardFreigabe();
            if (seminarfreigabe != 0) {
                freigabe = seminarfreigabe;
                deleteFreigabe();
                saveFreigabe(freigabe, "system");
            }
        }
        return freigabe;
    }

    public boolean saveFreigabe(int freigabeId, String benutzerId) {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        String command = "INSERT INTO freigabe (job_id, freigabe_id";
        String values = ") VALUES ('" + id + "', '" + freigabeId + "'";
        if (benutzerId != null) {
            command += ", benutzer_id";
            values += ", '" + benutzerId + "'";
        }
        String query = command + values + ")";
        try {
            stmnt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern der Freigabedaten f�r: " + id);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deleteFreigabe() {
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return false;
        String query = "DELETE FROM freigabe WHERE job_id = '" + id + "'";
        try {
            stmnt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("Fehler beim L�schen der Freigabedaten f�r: " + id);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Date setDeutschesDatum(String eingabe) {
        GregorianCalendar gc = new GregorianCalendar();
        StringTokenizer st = new StringTokenizer(eingabe, ".");
        try {
            int day = Integer.parseInt(st.nextToken());
            int month = Integer.parseInt(st.nextToken()) - 1;
            int year = Integer.parseInt(st.nextToken());
            gc.set(GregorianCalendar.DAY_OF_MONTH, day);
            gc.set(GregorianCalendar.MONTH, month);
            gc.set(GregorianCalendar.YEAR, year);
        } catch (Exception e) {
            System.err.println("Deutsches Datum konnte nicht geparst werden!");
        }
        return new Date(gc.getTimeInMillis());
    }

    public Server getPodcastServer() {
        if (podcastServer == null) {
            if (seminar == null) seminar = Seminar.readSeminar(seminarID);
            setPodcastServer(Server.readServer(seminar.getPreferedPodcastServer()));
        }
        return podcastServer;
    }

    public void setPodcastServer(Server pocastServer) {
        if (pocastServer == null) {
            System.out.println("Kein webserver zugewiesen");
            return;
        }
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (database.readActionId(id, pocastServer.getId()) == 0) {
            database.statusSpeichern(id, 110, pocastServer.getId(), "Podcast Server zugewiesen");
        }
        this.podcastServer = pocastServer;
    }

    /**
 * Starts an encoding job for a given encoding format
 * @param format the parameters for encoding
 * @return true if the encoding started successfully
 */
    public boolean encodeJob(Encoding format) {
        if (format.getCodecID() == 7) {
            return true;
        }
        try {
            VideoKonvertierungsServer vcServer = Datenspeicher.getVideokonverter().getVideoKonvertierungsServer();
            VideoKonvertierungsServerClient client = vcServer.getClient();
            DatabaseConnection database = Datenspeicher.getDatabase();
            if (!database.isConnected()) database.connect();
            database.statusSpeichern(id, 100, vcServer.getId(), format.getCodecID(), "Von Job gestartet");
            boolean status = true;
            if (!client.isConnected()) if (!client.connect()) status = false;
            if (!client.isAuthentifiziert()) if (!client.anmeldung()) status = false;
            if (!client.add(id, format)) status = false;
            if (!client.quit()) status = false;
            client.close();
            if (!status) System.err.println("Warnung Probleme bei Kommunikation mit Videokonvertierungsserver. Format: " + format.getConverterTag());
        } catch (NoServerAvailableException e) {
            System.err.println("Kein Server zur Videokonvertierung verf�gbar. Format: " + format.getConverterTag());
            return false;
        }
        return true;
    }

    /**
 * encodes the Job in all Formats given by seminar presets. This can be use for reencoding the job in format, when resolution or something changed
 *
 */
    public void encodeAllFormats() {
        if (seminar == null) seminar = Seminar.readSeminar(getSeminarID());
        LinkedList<Encoding> formate = seminar.getFormate();
        if (formate.size() == 0) formate.add(new Encoding());
        for (int i = 0; i < formate.size(); i++) {
            encodeJob(formate.get(i));
        }
    }

    /**
	 * encodes the Job in all Formats given by seminar presets. This can be use for reencoding the job in format, when resolution or something changed
	 *
	 */
    public void encodeAllFormatsExceptFlash() {
        if (seminar == null) seminar = Seminar.readSeminar(getSeminarID());
        LinkedList<Encoding> formate = seminar.getFormate();
        for (int i = 0; i < formate.size(); i++) {
            if (!(formate.get(i).getCodecID() == 1)) encodeJob(formate.get(i));
        }
    }

    public LinkedList<Integer> getAllFinishedFormats() {
        LinkedList<Integer> fertig = new LinkedList<Integer>();
        DatabaseConnection database = Datenspeicher.getDatabase();
        if (!database.isConnected()) database.connect();
        Statement stmnt = database.getStatement();
        if (stmnt == null) return null;
        String query = "SELECT codec_id FROM  `status`  WHERE `job_id`  =  '" + id + "' AND  `status_id` = '200'";
        try {
            ResultSet result = stmnt.executeQuery(query);
            while (result.next()) {
                int erg = result.getInt("codec_id");
                if (erg == 0) erg = 1;
                fertig.add((new Integer(erg)));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim auslesen der fertigen Formate: " + id + " query: " + query);
            e.printStackTrace();
            return null;
        }
        if (fertig.size() == 0) return null;
        return fertig;
    }
}
