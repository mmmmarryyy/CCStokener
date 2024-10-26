package com.blazinggames.urcl.data;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * A class that holds references to all of the resources that a game
 * needs. Resources are classified into four categories:
 * <UL>
 * <LI>Images - gif or jpg images</LI>
 * <LI>Audio - au files (for applets)</LI>
 * <LI>binaries - raw (byte array) data</LI>
 * <LI>textGroups - a useful way of holding blocks of text data</LI>
 * </UL>
 *
 * Text groups are stored as a hashtable consisting of a topic (stored
 * in the data file as a line starting with the topic tag containd
 * within a pair of square brackets) with the contents being an array
 * of strings.
 * 
 * @author Billy D. Spelchan
 * @version OSR1.00
 */
public class ResMan {

    Applet _applet;

    protected Hashtable _images;

    protected Hashtable _audio;

    protected Hashtable _binaries;

    protected Hashtable _textGroups;

    /**
     * default constructor.
     */
    public ResMan() {
        _applet = null;
        _images = new Hashtable();
        _audio = new Hashtable();
        _binaries = new Hashtable();
        _textGroups = new Hashtable();
    }

    /**
     * Applet constructor - used when want to use sounds.
     *
     * @param a applet to use for grabbling/playing audio
     */
    public ResMan(Applet a) {
        this();
        _applet = a;
    }

    /**
     * adds an image to the list of images by loading image from url
     *
     * @param key name used to reference image
     * @param name string version of url
     * @return true if image was added, false if failed or already exists
    */
    public boolean addImage(String key, String name) {
        URL temp = null;
        try {
            temp = new URL(_applet.getCodeBase(), name);
        } catch (Exception e) {
            System.err.println("ResMan had problem creating url for " + name);
            System.err.println(e.getMessage());
            return false;
        }
        return addImage(key, temp);
    }

    /**
     * adds an image to the list of images by loading image from url
     *
     * @param key name used to reference image
     * @param url the URL of the image to load
     * @return true if image was loaded, false if failed or already exists
     */
    public boolean addImage(String key, URL url) {
        if (_images.containsKey(key)) {
            return false;
        }
        if (_applet == null) {
            System.err.println("ResMan failure - applet error");
            return false;
        }
        Image temp = _applet.getImage(url);
        MediaTracker mt = new MediaTracker(_applet);
        mt.addImage(temp, 1);
        try {
            mt.waitForAll();
        } catch (Exception e) {
            System.err.println("Problem getting image " + e.getMessage());
        }
        if (temp.getWidth(_applet) < 1) {
            System.err.println("ResMan couldn't get data");
            return false;
        }
        _images.put(key, temp);
        return true;
    }

    /**
    * adds an image to the list of images
     *
     * @param key name used to reference image
     * @param i image to add to list
     * @return true if image was loaded, false if failed or already exists
     */
    public boolean addImage(String key, Image i) {
        if (_images.containsKey(key)) return false;
        _images.put(key, i);
        return true;
    }

    /**
     * returns indicated image or null if no image
     *
     * @return indicated image or null if no image
     */
    public Image getImage(String key) {
        return (Image) _images.get(key);
    }

    /**
     * returns an array of all images currently in the library
     *
     * @return an array of all images currently in the library
     */
    public String[] getImageList() {
        String list[] = new String[_images.size()];
        int index = 0;
        for (Enumeration e = _images.keys(); e.hasMoreElements(); ++index) {
            list[index] = (String) e.nextElement();
        }
        return list;
    }

    /**
     * removes all images from the image library
    */
    public void removeAllImages() {
        _images.clear();
    }

    /**
     * removes the indicated image from the library
     *
     * @param key image to remove from the library
    */
    public Image removeImage(String key) {
        return ((Image) _images.remove(key));
    }

    /**
     * adds an audioclip to the list of sounds by loading the clip from url
     *
     * @param key name used to reference the audio clip
     * @param name string version of url
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addAudioClip(String key, String name) {
        if (_audio.containsKey(key)) return false;
        if (_applet == null) return false;
        AudioClip ac = _applet.getAudioClip(_applet.getCodeBase(), name);
        _audio.put(key, ac);
        return true;
    }

    /**
     * adds an audioclip to the list of sounds by loading the clip from url
     *
     * @param key name used to reference the audio clip
     * @param url URL to load audio clip from
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addAudioClip(String key, URL url) {
        if (_audio.containsKey(key)) return false;
        if (_applet == null) return false;
        AudioClip ac = _applet.getAudioClip(url);
        _audio.put(key, ac);
        return true;
    }

    /**
     * adds an audioclip to the list of sounds
     *
     * @param key name used to reference the audio clip
     * @param ac audio clip to load
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addAudioClip(String key, AudioClip ac) {
        if (_audio.containsKey(key)) return false;
        _audio.put(key, ac);
        return true;
    }

    /**
     * returns indicated audio clip or null if no clip
     *
     * @return indicated audio clip or null if no clip
     */
    public AudioClip getAudioClip(String key) {
        return ((AudioClip) _audio.get(key));
    }

    /**
     * returns an array of all audio clips currently in the library
     *
     * @return an array of all audio clips currently in the library
     */
    public String[] getSoundList() {
        String list[] = new String[_audio.size()];
        int index = 0;
        for (Enumeration e = _audio.keys(); e.hasMoreElements(); ++index) {
            list[index] = (String) e.nextElement();
        }
        return list;
    }

    /**
     * removes all sound clips from the sound library
     */
    public void removeAllSounds() {
        _audio.clear();
    }

    /**
     * removes the indicated audio clip from the library
     *
     * @param key audio clip to remove from the library
     */
    public AudioClip removeSound(String key) {
        return ((AudioClip) _audio.remove(key));
    }

    /**
     * starts playing the indicated audio clip.
     *
     * @param key audio clip to play
    */
    public void playAudio(String key) {
        AudioClip au = (AudioClip) _audio.get(key);
        if (au != null) {
            au.play();
        }
    }

    /**
     * coninuously plays the indicated audio clip.
     *
     * @param key audio clip to loop
     */
    public void loopAudio(String key) {
        AudioClip au = (AudioClip) _audio.get(key);
        if (au != null) {
            au.loop();
        }
    }

    /**
     * Stops playing the indicated audio clip
     *
     * @param key stops playing the audio clip
    */
    public void stopAudio(String key) {
        AudioClip au = (AudioClip) _audio.get(key);
        if (au != null) {
            au.stop();
        }
    }

    /**
     * adds a data to the list of binaries by loading data from url.
     * data is stored as a byte array.
     *
     * @param key name used to reference the data
     * @param name string version of url
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addBinary(String key, String name) {
        URL temp = null;
        try {
            temp = new URL(_applet.getCodeBase(), name);
        } catch (Exception e) {
            System.err.println("ResMan had problem creating url for " + name);
            System.err.println(e.getMessage());
            return false;
        }
        return addBinary(key, temp);
    }

    /**
     * adds a data to the list of binaries by loading data from url.
     * data is stored as a byte array.
     *
     * @param key name used to reference the data
     * @param url URL of data
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addBinary(String key, URL url) {
        if (_binaries.contains(key)) return false;
        byte chunk[], data[];
        Vector chunks = new Vector();
        int size, cntr, place;
        try {
            InputStream is = url.openStream();
            do {
                chunk = new byte[10000];
                size = is.read(chunk, 0, 10000);
                if (size == 10000) chunks.addElement(chunk);
            } while (size == 10000);
            is.close();
        } catch (IOException ioe) {
            System.err.println("ResMan could not get binary for " + key);
            return false;
        }
        if (size == -1) data = new byte[chunks.size() * 10000]; else data = new byte[chunks.size() * 10000 + size];
        place = 0;
        for (cntr = 0; cntr < chunks.size(); ++cntr) {
            System.arraycopy(chunks.elementAt(cntr), 0, data, place, 10000);
            place += 10000;
        }
        if (size > 0) System.arraycopy(chunk, 0, data, place, size);
        _binaries.put(key, data);
        return true;
    }

    /**
     * adds a data to the list of binaries by loading data from url.
     * data is stored as a byte array.
     *
     * @param key name used to reference the data
     * @param bin data to be added to list
     * @return true if audio clip was added, false if failed or already exists
     */
    public boolean addBinary(String key, byte[] bin) {
        if (_binaries.contains(key)) return false;
        _binaries.put(key, bin);
        return true;
    }

    /**
     * returns indicated data or null if none
     *
     * @return indicated data or null if none
     */
    public byte[] getBinary(String key) {
        return (byte[]) _binaries.get(key);
    }

    /**
     * returns an array of all binary data currently in the library
     *
     * @return an array of all binary data currently in the library
     */
    public String[] getBinaryList() {
        String list[] = new String[_binaries.size()];
        int index = 0;
        for (Enumeration e = _binaries.keys(); e.hasMoreElements(); ++index) {
            list[index] = (String) e.nextElement();
        }
        return list;
    }

    /**
     * removes all binaries from the binary library
     */
    public void removeAllBinaries() {
        _binaries.clear();
    }

    /**
     * removes the indicated binary from the library
     *
     * @param key binary to remove from the library
     */
    public byte[] removeBinary(String key) {
        return ((byte[]) _binaries.remove(key));
    }

    /**
     * adds a text group to the list of text groups by loading
     * and parsing the text information from a text file via url.
     *
     * @param key name used to reference the text group
     * @param name string version of url
     * @return true if text group was added, false if failed or already exists
     */
    public boolean addTextGroup(String key, String name) {
        URL temp = null;
        try {
            temp = new URL(_applet.getCodeBase(), name);
        } catch (Exception e) {
            System.err.println("ResMan had problem creating url for " + name);
            System.err.println(e.getMessage());
            return false;
        }
        return addTextGroup(key, temp);
    }

    /**
     * adds a text group to the list of text groups by loading
     * and parsing the text information from a text file via url.
     *
     * @param key name used to reference the text group
     * @param url URL of the text that makes up the text group
     * @return true if text group was added, false if failed or already exists
     */
    public boolean addTextGroup(String key, URL url) {
        if (_textGroups.contains(key)) return false;
        String s;
        Hashtable tg = new Hashtable();
        String sGroupKey = "default";
        String sGroup[];
        Vector vGroup = new Vector();
        int cntr;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((s = in.readLine()) != null) {
                if (s.startsWith("[")) {
                    if (vGroup.size() > 0) {
                        sGroup = new String[vGroup.size()];
                        for (cntr = 0; cntr < vGroup.size(); ++cntr) sGroup[cntr] = (String) vGroup.elementAt(cntr);
                        tg.put(sGroupKey, sGroup);
                        vGroup.removeAllElements();
                    }
                    sGroupKey = s.substring(1, s.indexOf(']'));
                } else {
                    vGroup.addElement(s);
                }
            }
            if (vGroup.size() > 0) {
                sGroup = new String[vGroup.size()];
                for (cntr = 0; cntr < vGroup.size(); ++cntr) sGroup[cntr] = (String) vGroup.elementAt(cntr);
                tg.put(sGroupKey, sGroup);
            }
            in.close();
        } catch (IOException ioe) {
            System.err.println("Error reading file for " + key);
            System.err.println(ioe.getMessage());
            return false;
        }
        _textGroups.put(key, tg);
        return true;
    }

    /**
     * adds a text group to the list of text groups
     *
     * @param key name used to reference the text group
     * @param tg text group hashtable
     * @return true if text group was added, false if failed or already exists
     */
    public boolean addTextGroup(String key, Hashtable tg) {
        if (_textGroups.containsKey(key)) return false;
        _textGroups.put(key, tg);
        return true;
    }

    /**
     * Adds new text block to an existing text group
     *
     * key name of group to add text to
     * textkey name of key for new text group
     * text array of strings that make up the text groups data
    */
    public boolean addTextToTextGroup(String key, String textKey, String text[]) {
        Hashtable tg = (Hashtable) _textGroups.get(key);
        if (tg == null) {
            return false;
        }
        tg.put(textKey, text);
        return true;
    }

    /**
     * returns indicated text group or null if none
     *
     * @return indicated text group or null if none
     */
    public Hashtable getTextGroup(String key) {
        return ((Hashtable) _textGroups.get(key));
    }

    /**
     * Returns the data contained within a specific text group and topic.
     *
     * @param key text group to look into
     * @param textkey topic to look up
     * @reutrn the data contained within a specific text group and topic.
    */
    public String[] getTextGroupText(String key, String textKey) {
        Hashtable tg = (Hashtable) _textGroups.get(key);
        if (tg == null) {
            return null;
        }
        return ((String[]) tg.get(textKey));
    }

    /**
     * returns an array of all text groups currently in the library
     *
     * @return an array of all text groups currently in the library
     */
    public String[] getTextGroupList() {
        String list[] = new String[_textGroups.size()];
        int index = 0;
        for (Enumeration e = _textGroups.keys(); e.hasMoreElements(); ++index) {
            list[index] = (String) e.nextElement();
        }
        return list;
    }

    /**
     * removes all text groups from the text group library
     */
    public void removeAllTextGroups() {
        _textGroups.clear();
    }

    /**
     * removes the indicated text group from the library
     *
     * @param key text group to remove from the library
     */
    public Hashtable removeTextGroup(String key) {
        return ((Hashtable) _textGroups.remove(key));
    }

    /**
     * removes the indicated text group topc from the textgroup in the library
     *
     * @param key text group to remove the group from
     * @param textkey topic to remove from the textgroup
     */
    public String[] removeTextGroupText(String key, String textKey) {
        Hashtable tg = (Hashtable) _textGroups.get(key);
        if (tg == null) {
            return null;
        }
        return ((String[]) tg.remove(textKey));
    }
}
