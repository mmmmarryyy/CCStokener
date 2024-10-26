package org.openbroad.client.streaming;

import java.io.*;
import java.rmi.*;
import java.net.*;
import java.util.*;
import org.openbroad.client.user.view.*;
import org.openbroad.client.user.view.model.*;
import org.openbroad.client.user.view.control.*;
import org.openbroad.client.stub.*;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.*;
import javazoom.jl.player.advanced.*;

public class Streamer extends PlaybackListener {

    public Streamer(java.util.List fileList, OpenBroad gui, int[] filIdList) {
        this.fileList = fileList;
        this.gui = gui;
        this.filIdList = filIdList;
    }

    public void createStreamlog(Calendar startTime, Calendar endTime, int fileId) {
        org.openbroad.client.stub.Streamlog strLog = new org.openbroad.client.stub.Streamlog();
        strLog.setUsername(Util.getUser());
        strLog.setFileid(fileId);
        strLog.setStarttime(startTime);
        strLog.setEndtime(endTime);
        try {
            URL url = new URL("http://" + Config.getConfig().getProperty("server") + ":8080/jboss-net/services/StreamLogService");
            StreamLogServiceRemoteServiceLocator slLoc = new StreamLogServiceRemoteServiceLocator();
            StreamLogServiceSoapBindingStub slstub = (StreamLogServiceSoapBindingStub) slLoc.getStreamLogService(url);
            slstub.setUsername(Config.getConfig().getProperty("username"));
            slstub.setPassword(Config.getConfig().getProperty("password"));
            slstub.createStreamlog(strLog);
        } catch (javax.xml.rpc.ServiceException e) {
            e.printStackTrace();
        } catch (java.net.MalformedURLException murle) {
            murle.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void play(String fileName) throws JavaLayerException {
        try {
            gui.getPlaylistTable().setDefaultRenderer(Object.class, new PlayListRenderer(playingFileNr));
            gui.getPlaylistTable().repaint();
            URI uri = new URI(fileName);
            InputStream in = null;
            if (remote == true) {
                in = getURLInputStream(fileName);
            } else {
                in = getInputStream(fileName);
            }
            AudioDevice dev = getAudioDevice();
            steamingFile = new AdvancedPlayer(in, dev);
            steamingFile.setPlayBackListener(this);
            steamingFile.play();
        } catch (IOException ex) {
            throw new JavaLayerException("Problem playing file " + fileName, ex);
        } catch (Exception ex) {
            throw new JavaLayerException("Problem playing file " + fileName, ex);
        }
    }

    /**
     * Playing file from URL (Streaming).
     */
    protected InputStream getURLInputStream(String name) throws Exception {
        URL url = new URL(name);
        InputStream is = url.openStream();
        BufferedInputStream bin = new BufferedInputStream(is, 2 * 1024);
        return bin;
    }

    /**
     * Playing file from FileInputStream.
     */
    protected InputStream getInputStream(String name) throws IOException {
        FileInputStream fin = new FileInputStream(name);
        BufferedInputStream bin = new BufferedInputStream(fin);
        return bin;
    }

    protected AudioDevice getAudioDevice() throws JavaLayerException {
        return FactoryRegistry.systemRegistry().createAudioDevice();
    }

    public int getPlayingFileNr() {
        return playingFileNr;
    }

    public void start(int count) {
        playingFileNr = count;
        try {
            play(fileList.get(playingFileNr).toString());
        } catch (Exception ex) {
            System.err.println(ex);
            ex.printStackTrace(System.err);
        }
    }

    public void stop() {
        if (steamingFile != null) {
            steamingFile.close();
            steamingFile = null;
        }
    }

    public void playbackFinished(PlaybackEvent evt) {
        endTime = new GregorianCalendar();
        createStreamlog(startTime, endTime, filIdList[playingFileNr]);
        try {
            playingFileNr++;
            if (fileList.size() > playingFileNr) {
                play(fileList.get(playingFileNr).toString());
                gui.getPlaylistTable().setDefaultRenderer(Object.class, new PlayListRenderer(-1));
                gui.getPlaylistTable().repaint();
            } else {
                stop();
            }
        } catch (Exception ex) {
            System.err.println(ex);
            ex.printStackTrace(System.err);
        }
    }

    public void playbackStarted(PlaybackEvent evt) {
        startTime = new GregorianCalendar();
    }

    private java.util.List fileList = null;

    private OpenBroad gui;

    private AdvancedPlayer steamingFile = null;

    private PlaybackListener listener = null;

    private boolean remote = true;

    private int playingFileNr = 0;

    private int[] filIdList;

    private Calendar startTime = null;

    private Calendar endTime = null;
}
