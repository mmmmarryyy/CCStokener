package com.webhiker.dreambox.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.webhiker.dreambox.api.epg.EPG;
import com.webhiker.dreambox.api.service.Audio;
import com.webhiker.dreambox.api.service.ServiceData;
import com.webhiker.dreambox.api.services.Service;
import com.webhiker.dreambox.api.services.Services;
import com.webhiker.dreambox.api.status.Status;
import com.webhiker.dreambox.api.streaminfo.StreamInfo;

/**
 * The Class DreamboxAPI. For events to be generated for registered listeners, a new Thread needs to be started 
 * e.g. new Thread(new DreamboxAPI("127.0.0.1","80","root","dreambox")).start();
 */
public class DreamboxAPI implements Runnable {

    private Logger log = Logger.getAnonymousLogger();

    /**
	 * The set of available Dreambox commands.
	 */
    public enum Command {

        /** Put the Dreambox into STANDBY mode. */
        STANDBY, /** Put the Dreambox into  WAKEUP. */
        WAKEUP, /** REBOOT the Dreambox. */
        REBOOT, /** RESTART the Dreambox. */
        RESTART, /** SHUTDOWN the Dreambox. */
        SHUTDOWN
    }

    public enum Mode {

        TV, RADIO, DATA, MOVIES, ROOT
    }

    public enum SubMode {

        NA, All, Satellites, Providers, Bouquets
    }

    /** The password. */
    private String host, username, password;

    /** The port. */
    private int port;

    /** The dreambox listeners. */
    private List<DreamboxListener> dreamboxListeners;

    /** The running. */
    private boolean running;

    private Status status;

    private ServiceData serviceData;

    private Services services;

    /**
	 * Instantiates a new dreambox api.
	 * 
	 * @param host the host
	 * @param port the port
	 * @param username the username
	 * @param password the password
	 */
    public DreamboxAPI(String host, int port, String username, String password) {
        setHost(host);
        setPort(port);
        setUsername(username);
        setPassword(password);
    }

    /**
	 * Gets the host.
	 * 
	 * @return the host
	 */
    public String getHost() {
        return host;
    }

    /**
	 * Sets the host.
	 * 
	 * @param host the new host
	 */
    public void setHost(String host) {
        this.host = host;
    }

    /**
	 * Gets the username.
	 * 
	 * @return the username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Sets the username.
	 * 
	 * @param username the new username
	 */
    public void setUsername(String username) {
        this.username = username;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
            }
        });
    }

    /**
	 * Gets the password.
	 * 
	 * @return the password
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * Sets the password.
	 * 
	 * @param password the new password
	 */
    public void setPassword(String password) {
        this.password = password;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
            }
        });
    }

    /**
	 * Gets the port.
	 * 
	 * @return the port
	 */
    public int getPort() {
        return port;
    }

    /**
	 * Sets the port.
	 * 
	 * @param port the new port
	 */
    public void setPort(int port) {
        this.port = port;
    }

    public void run() {
        setRunning(true);
        Status newStatus = new Status(), oldStatus = new Status();
        ServiceData newServiceData = new ServiceData(), oldServiceData = new ServiceData();
        StreamInfo newStreamInfo = new StreamInfo(), oldStreamInfo = new StreamInfo();
        try {
            getServices(true, Mode.TV, SubMode.Bouquets);
        } catch (IOException e1) {
            log.severe(e1.getMessage());
        } catch (ParserConfigurationException e1) {
            log.severe(e1.getMessage());
        } catch (SAXException e1) {
            log.severe(e1.getMessage());
        }
        while (isRunning()) {
            System.out.println("Dreambox API heartbeat");
            try {
                oldStatus = newStatus;
                newStatus = getStatus(false);
                if (oldStatus.getVolume() != newStatus.getVolume()) {
                    sendVolumeChangedEvent(newStatus);
                }
                if (oldStatus.isMute() != newStatus.isMute()) {
                    sendMuteChangedEvent(newStatus);
                }
                if (oldStatus.isStandby() != newStatus.isStandby()) {
                    sendStandbyChangedEvent(newStatus);
                }
                if (!oldStatus.getVLCURL().equals(newStatus.getVLCURL())) {
                    sendVLCURLChangedEvent(newStatus);
                }
                if (oldStatus.getUpdateDelay() != newStatus.getUpdateDelay()) {
                    sendUpdateDelayChangedEvent(newStatus);
                }
                if (!oldStatus.getServiceName().equals(newStatus.getServiceName())) {
                    sendServiceNameChangedEvent(newStatus);
                }
                if (!oldStatus.getNextProgramName().equals(newStatus.getNextProgramName())) {
                    sendNextProgramNameChangedEvent(newStatus);
                }
                if (!oldStatus.getServiceReference().equals(newStatus.getServiceReference())) {
                    sendServiceReferenceChangedEvent(newStatus);
                }
                if (!oldStatus.getProgramName().equals(newStatus.getProgramName())) {
                    sendProgramNameChangedEvent(newStatus);
                }
                if (!oldStatus.getProgramStartTime().equals(newStatus.getProgramStartTime())) {
                    sendProgramStartTimeChangedEvent(newStatus);
                }
                if (!oldStatus.getNextProgramStartTime().equals(newStatus.getNextProgramStartTime())) {
                    sendNextProgramStartTimeChangedEvent(newStatus);
                }
                if (oldStatus.isRecording() != newStatus.isRecording()) {
                    sendRecordingChangedEvent(newStatus);
                }
                if (oldStatus.isDolby() != newStatus.isDolby()) {
                    sendDolbyChangedEvent(newStatus);
                }
                oldServiceData = newServiceData;
                newServiceData = getServiceData(false);
                if (!oldServiceData.equals(newServiceData)) {
                    sendAudioChangedEvent(newServiceData.getAudio());
                }
                oldStreamInfo = newStreamInfo;
                newStreamInfo = getStreamInfo();
                if (!oldStreamInfo.equals(newStreamInfo)) {
                    sendStreamInfoChangedEvent(newStreamInfo);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                sendConnectionStatusChangedEvent(false);
            } finally {
                try {
                    Thread.sleep(3000);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        setRunning(false);
    }

    private void sendStreamInfoChangedEvent(StreamInfo streamInfo) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleStreamInfoChangedEvent(streamInfo);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    private void sendAudioChangedEvent(List<Audio> audio) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleAudioChangedEvent(audio);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    private void sendConnectionStatusChangedEvent(boolean b) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleConnectionStatusChangedEvent(b);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send service name changed event.
	 * 
	 * @param status the status
	 */
    private void sendServiceNameChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleServiceNameChanged(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send service reference changed event.
	 * 
	 * @param status the status
	 */
    private void sendServiceReferenceChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleServiceReferenceChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send dolby changed event.
	 * 
	 * @param status the status
	 */
    private void sendDolbyChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleDolbyChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send recording changed event.
	 * 
	 * @param status the status
	 */
    private void sendRecordingChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleRecordingChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send standby changed event.
	 * 
	 * @param status the status
	 */
    private void sendStandbyChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleStandbyChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send mute changed event.
	 * 
	 * @param status the status
	 */
    private void sendMuteChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleMuteChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send next program start time changed event.
	 * 
	 * @param status the status
	 */
    private void sendNextProgramStartTimeChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleNextProgramStartTimeChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send program start time changed event.
	 * 
	 * @param status the status
	 */
    private void sendProgramStartTimeChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleProgramStartTimeChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send program name changed event.
	 * 
	 * @param status the status
	 */
    private void sendProgramNameChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleProgramNameChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send next program name changed event.
	 * 
	 * @param status the status
	 */
    private void sendNextProgramNameChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleNextProgramNameChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send update delay changed event.
	 * 
	 * @param status the status
	 */
    private void sendUpdateDelayChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleUpdateDelayChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send vlcurl changed event.
	 * 
	 * @param status the status
	 */
    private void sendVLCURLChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleVLCURLChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Send volume changed event.
	 * 
	 * @param status the status
	 */
    private void sendVolumeChangedEvent(Status status) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleVolumeChangedEvent(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    private void sendServicesLoadedEvent(Services services) {
        synchronized (dreamboxListeners) {
            if (dreamboxListeners != null) {
                for (Iterator<DreamboxListener> i = dreamboxListeners.iterator(); i.hasNext(); ) {
                    try {
                        i.next().handleServicesLoadedEvent(services);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    ;
                }
            }
        }
    }

    /**
	 * Stop polling the Dreambox for status changes.
	 */
    public void stop() {
        setRunning(false);
    }

    public void selectService(Service service) throws IOException {
        String location = "/cgi-bin/zapTo?path=" + service.getReference();
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
    }

    public void selectAudio(Audio audio) throws IOException {
        String location = "/cgi-bin/setAudio?channel=" + audio.getChannel() + "&language=" + audio.getPid();
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
    }

    /**
	 * Sets the volume to the specified value.
	 * 
	 * @param volume the new volume, a value from 0 to 63.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void setVolume(int volume) throws IOException {
        String location = "/setVolume?volume=" + volume;
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
    }

    /**
	 * Gets the current volume as reported by the Dreambox.
	 * 
	 * @return the volume as a value from 0 to 63.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public int getVolume() throws IOException {
        String location = "/cgi-bin/audio";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        InputStream is = urlConn.getInputStream();
        StringBuffer sb = Utils.getStringBuffer(is);
        is.close();
        int start = sb.indexOf("volume: ") + 8;
        int end = sb.indexOf("<br>");
        return 63 - Integer.parseInt(sb.substring(start, end).trim());
    }

    /**
	 * This provides functionality for Dreambox control.
	 * 
	 * @param command the command to execute
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void command(Command command) throws IOException {
        String commandStr = "/cgi-bin/admin?command=" + command.toString().toLowerCase() + "&requester=webif";
        URLConnection urlConn = getConnection(commandStr);
        Utils.validateResponse(urlConn);
    }

    /**
	 * Send a message to the Dreambox display.
	 * 
	 * @param message the message
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void sendMessage(String message) throws IOException {
        String commandStr = "/control/message?msg=" + URLEncoder.encode(message, "utf-8");
        URLConnection urlConn = getConnection(commandStr);
        Utils.validateResponse(urlConn);
    }

    /**
	 * Gets the base.
	 * 
	 * @return the base
	 */
    private String getBase() {
        return "http://" + getHost() + ":" + getPort();
    }

    /**
	 * Gets the connection.
	 * 
	 * @param location the location
	 * 
	 * @return the connection
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    private URLConnection getConnection(String location) throws IOException {
        URL url = new URL(getBase() + location);
        URLConnection uc = url.openConnection();
        return uc;
    }

    /**
	 * Gets the VLC playable URL for the currently playing channel. If @ param translated is true, it will
	 * be converted to the hostname used to connect, else it will be the actual IP address
	 * of the Dreambox.
	 *  
	 * @param translated the translated
	 * 
	 * @return the vLCURL
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public String getVLCURL(boolean translated) throws IOException {
        String url = getStatus().getVLCURL();
        if (translated) {
            return "http://" + getUsername() + ':' + getPassword() + '@' + getHost() + url.substring(url.lastIndexOf(':'), url.length());
        } else {
            return url;
        }
    }

    /**
	 * Gets the status of the stream for the currently showing channel.
	 * 
	 * @return the status
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public Status getStatus() throws IOException {
        return getStatus(true);
    }

    /**
	 * Gets the status from the dreambox.
	 * 
	 * @param cached if true, the cached value willbe returned, if false, the cache will be ignored and 
	 * the Dreambox will be queried directly.
	 * 
	 * @return the status
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public Status getStatus(boolean cached) throws IOException {
        if ((status == null) || (!cached)) {
            String location = "/data";
            URLConnection urlConn = getConnection(location);
            Utils.validateResponse(urlConn);
            status = new Status(urlConn.getInputStream());
            return status;
        } else {
            return status;
        }
    }

    public ServiceData getServiceData() throws IOException, ParserConfigurationException, SAXException {
        return getServiceData(true);
    }

    public ServiceData getServiceData(boolean cached) throws IOException, ParserConfigurationException, SAXException {
        if ((serviceData == null) || (!cached)) {
            String location = "/xml/currentservicedata";
            URLConnection urlConn = getConnection(location);
            Utils.validateResponse(urlConn);
            serviceData = new ServiceData(urlConn.getInputStream());
            return serviceData;
        } else {
            return serviceData;
        }
    }

    /**
	 * Gets the Electronic Program Guide information for the currently showing channel.
	 * 
	 * @return the ePG
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 * @throws ParserConfigurationException the parser configuration exception
	 */
    public EPG getEPG() throws IOException, SAXException, ParserConfigurationException {
        String location = "/xml/serviceepg";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return new EPG(urlConn.getInputStream());
    }

    /**
	 * Gets the stream info for the currently displayed service.
	 * 
	 * @return the stream info
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the SAX exception
	 */
    public StreamInfo getStreamInfo() throws IOException, ParserConfigurationException, SAXException {
        String location = "/xml/streaminfo";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
        return new StreamInfo(urlConn.getInputStream());
    }

    public Services getServices(boolean cached, Mode mode, SubMode submode) throws IOException, ParserConfigurationException, SAXException {
        if (cached) {
            if (services != null) {
                return services;
            }
        }
        return services = getServices(mode, submode);
    }

    public synchronized Services getServices(Mode mode, SubMode submode) throws IOException, ParserConfigurationException, SAXException {
        String location = "/xml/services?mode=" + mode.ordinal() + "&submode=" + submode.ordinal();
        URLConnection urlConn = getConnection(location);
        System.out.println(urlConn.getURL().toString());
        Utils.validateResponse(urlConn);
        services = new Services(new InputStreamCleaner(urlConn.getInputStream()));
        sendServicesLoadedEvent(services);
        return services;
    }

    /**
	 * Adds the dreambox listener.
	 * 
	 * @param dl the dl
	 */
    public void addDreamboxListener(DreamboxListener dl) {
        if (dreamboxListeners == null) dreamboxListeners = new ArrayList<DreamboxListener>();
        dreamboxListeners.add(dl);
    }

    /**
	 * Utility method to fire status changed events to all registered listeners.
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
    public void fireStatusChanged(DreamboxListener dl) throws IOException, ParserConfigurationException, SAXException {
        Status status = getStatus();
        dl.handleDolbyChangedEvent(status);
        dl.handleMuteChangedEvent(status);
        dl.handleNextProgramNameChangedEvent(status);
        dl.handleNextProgramStartTimeChangedEvent(status);
        dl.handleRecordingChangedEvent(status);
        dl.handleProgramNameChangedEvent(status);
        dl.handleServiceNameChanged(status);
        dl.handleServiceReferenceChangedEvent(status);
        dl.handleProgramStartTimeChangedEvent(status);
        dl.handleStandbyChangedEvent(status);
        dl.handleVLCURLChangedEvent(status);
        dl.handleServicesLoadedEvent(getServices(true, Mode.TV, SubMode.All));
        dl.handleVolumeChangedEvent(status);
        ServiceData serviceData = getServiceData();
        dl.handleAudioChangedEvent(serviceData.getAudio());
        dl.handleStreamInfoChangedEvent(getStreamInfo());
    }

    /**
	 * Check connection parameter, and throw an exception if could not connect.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public void checkConnection() throws IOException {
        getStatus(false);
    }

    /**
	 * Returns true if Dreambox is connected, else returns false
	 * 
	 * @return true, if is connected
	 */
    public boolean isConnected() {
        try {
            checkConnection();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void toggleMute() throws IOException {
        String location = "/setVolume?mute=undefined";
        URLConnection urlConn = getConnection(location);
        Utils.validateResponse(urlConn);
    }

    public boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
        sendConnectionStatusChangedEvent(running);
    }

    /**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
    public static final void main(String[] args) {
        DreamboxAPI dba = new DreamboxAPI("192.168.1.150", 8888, "root", "dreambox");
        try {
            dba.getStreamInfo();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
