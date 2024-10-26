package org.zmpp.swingui;

import java.io.IOException;
import java.net.URL;
import org.zmpp.base.DefaultMemoryAccess;
import org.zmpp.blorb.BlorbResources;
import org.zmpp.blorb.BlorbStory;
import org.zmpp.iff.DefaultFormChunk;
import org.zmpp.iff.FormChunk;
import org.zmpp.io.IOSystem;
import org.zmpp.io.InputStream;
import org.zmpp.media.Resources;
import org.zmpp.vm.Machine;
import org.zmpp.vm.MachineFactory;
import org.zmpp.vm.SaveGameDataStore;
import org.zmpp.vm.ScreenModel;
import org.zmpp.vm.StatusLine;
import org.zmpp.vmutil.FileUtils;

/**
 * This class implements machine creation for an applet.
 * 
 * @author Wei-ju Wu
 * @version 1.0
 */
public class AppletMachineFactory extends MachineFactory<ZmppApplet> {

    private ZmppApplet applet;

    private java.io.InputStream storyis;

    private java.io.InputStream resourceis;

    private FormChunk blorbchunk;

    private SaveGameDataStore savegamestore;

    /**
   * Constructor.
   * 
   * @param applet the applet object
   * @param storyurl the story file url
   * @param resourceurl the blorb file url
   * @param settings the display settings
   * @param savetofile true if should save to file
   * @throws Exception if an error occurs
   */
    public AppletMachineFactory(ZmppApplet applet, URL storyurl, URL resourceurl, boolean savetofile) throws Exception {
        this.applet = applet;
        savegamestore = savetofile ? new FileSaveGameDataStore(applet) : new MemorySaveGameDataStore();
        try {
            storyis = storyurl.openStream();
            if (resourceurl != null) {
                resourceis = resourceurl.openStream();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public AppletMachineFactory(ZmppApplet applet, URL zblorburl, boolean savetofile) throws Exception {
        this.applet = applet;
        savegamestore = savetofile ? new FileSaveGameDataStore(applet) : new MemorySaveGameDataStore();
        try {
            resourceis = zblorburl.openStream();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * {@inheritDoc}
   */
    protected byte[] readStoryData() throws IOException {
        if (storyis != null) {
            return FileUtils.readFileBytes(storyis);
        } else {
            FormChunk formchunk = readBlorb();
            return formchunk != null ? new BlorbStory(readBlorb()).getStoryData() : null;
        }
    }

    private FormChunk readBlorb() throws IOException {
        if (blorbchunk == null) {
            byte[] data = FileUtils.readFileBytes(resourceis);
            if (data != null) blorbchunk = new DefaultFormChunk(new DefaultMemoryAccess(data));
        }
        return blorbchunk;
    }

    /**
   * {@inheritDoc}
   */
    protected Resources readResources() throws IOException {
        FormChunk formchunk = readBlorb();
        return (formchunk != null) ? new BlorbResources(formchunk) : null;
    }

    /**
   * {@inheritDoc}
   */
    protected void reportInvalidStory() {
        System.err.printf("invalid story file");
    }

    /**
   * {@inheritDoc}
   */
    protected IOSystem getIOSystem() {
        return applet;
    }

    /**
   * {@inheritDoc}
   */
    protected InputStream getKeyboardInputStream() {
        return applet;
    }

    /**
   * {@inheritDoc}
   */
    protected StatusLine getStatusLine() {
        return applet;
    }

    /**
   * {@inheritDoc}
   */
    protected ScreenModel getScreenModel() {
        return applet.getScreenModel();
    }

    /**
   * {@inheritDoc}
   */
    protected SaveGameDataStore getSaveGameDataStore() {
        return savegamestore;
    }

    /**
   * {@inheritDoc}
   */
    protected ZmppApplet initUI(Machine machine) {
        applet.initUI(machine);
        return applet;
    }

    /**
   * {@inheritDoc}
   */
    public ZmppApplet getUI() {
        return applet;
    }
}
