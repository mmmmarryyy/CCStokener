package com.cidero.upnp;

import java.awt.Toolkit;
import java.util.logging.Logger;
import java.text.NumberFormat;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.ImageIcon;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.cidero.util.BMPReader;
import org.cybergarage.xml.XML;

/**
 *
 *  Resource object for MediaServer Content Directory Service (CDS).
 *  Resources are basically URI's describing the media's location, along
 *  with some other protocol info.  A single media item can have multiple
 *  resources if different representations of the same content are available
 *  (such as a low-res version of an image for use as a thumbnail)
 *  
 *  This object based on the description of the resource attribute in
 *  Appendix B of the Content Directory Service spec
 *
 */
public class CDSResource implements Cloneable {

    private static Logger logger = Logger.getLogger("com.cidero.upnp");

    String name;

    long size;

    String duration;

    int bitRate;

    int sampleFreq;

    int bitsPerSample;

    int nrAudioChannels;

    String resolution;

    int colorDepth;

    String protocolInfo;

    String protection;

    String importURI;

    /**
   *  Construct a default content directory resource object. All fields 
   *  in the returned object are uninitialized.
   */
    public CDSResource() {
        size = -1;
        bitRate = -1;
        sampleFreq = -1;
        bitsPerSample = -1;
        nrAudioChannels = -1;
        colorDepth = -1;
    }

    public CDSResource(Node node) {
        size = -1;
        bitRate = -1;
        sampleFreq = -1;
        bitsPerSample = -1;
        nrAudioChannels = -1;
        colorDepth = -1;
        NamedNodeMap attrs = node.getAttributes();
        protocolInfo = CDS.getAttrAsString(attrs, "protocolInfo");
        setDuration(CDS.getAttrAsString(attrs, "duration"));
        resolution = CDS.getAttrAsString(attrs, "resolution");
        protection = CDS.getAttrAsString(attrs, "protection");
        importURI = CDS.getAttrAsString(attrs, "importURI");
        size = CDS.getAttrAsLong(attrs, "size");
        bitRate = CDS.getAttrAsInt(attrs, "bitrate");
        bitsPerSample = CDS.getAttrAsInt(attrs, "bitsPerSample");
        sampleFreq = CDS.getAttrAsInt(attrs, "sampleFrequency");
        nrAudioChannels = CDS.getAttrAsInt(attrs, "nrAudioChannels");
        colorDepth = CDS.getAttrAsInt(attrs, "colorDepth");
        name = CDS.getSingleTextNodeValue(node);
    }

    /**
   *  Construct a content directory resource object using the specified
   *  data. 
   *
   *  Use -1 for undefined integer fields, and null for undefined string data
   */
    public CDSResource(String name, long size, String duration, int bitRate, int sampleFreq, int bitsPerSample, int nrAudioChannels, String resolution, int colorDepth, String protocolInfo, String protection, String importURI) {
        this.name = name;
        this.size = size;
        setDuration(duration);
        this.bitRate = bitRate;
        this.sampleFreq = sampleFreq;
        this.bitsPerSample = bitsPerSample;
        this.nrAudioChannels = nrAudioChannels;
        this.resolution = resolution;
        this.colorDepth = colorDepth;
        this.protocolInfo = protocolInfo;
        this.protection = protection;
        this.importURI = importURI;
    }

    /**
   *  Construct a content directory resource object from a simple URL
   *  The protocol info is determined from the URL suffix ( .mpg, .jpg,
   *  etc... )
   */
    public CDSResource(String url) {
        setName(url);
        setProtocolInfoFromExtension("http-get:*:audio/mpeg:*");
    }

    /**
   *
   *  Set protocolInfo field from URL extension. Use default if extension
   *  unrecognized.
   */
    public void setProtocolInfoFromExtension(String defaultInfo) {
        String nameLower = name.toLowerCase();
        if (nameLower.endsWith(".mpg") || nameLower.endsWith(".mpeg")) {
            setProtocolInfo("http-get:*:audio/mpeg:*");
        } else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) {
            setProtocolInfo("http-get:*:image/jpeg:*");
        } else if (nameLower.endsWith(".gif")) {
            setProtocolInfo("http-get:*:image/gif:*");
        } else {
            logger.fine("Unknown extension - using default MIME-type " + defaultInfo);
            setProtocolInfo(defaultInfo);
        }
    }

    public Object clone() {
        try {
            CDSResource obj = (CDSResource) super.clone();
            return obj;
        } catch (CloneNotSupportedException e) {
            logger.warning("Exception " + e);
            e.printStackTrace();
            return null;
        }
    }

    /**
   *  Set resource name
   *
   *  @param  name
   */
    public void setName(String name) {
        this.name = name;
    }

    /**
   *  Get resource name
   *
   *  @return  name
   */
    public String getName() {
        return name;
    }

    /**
   *  Set resource size
   *
   *  @param  size    Size in bytes of the resource
   */
    public void setSize(long size) {
        this.size = size;
    }

    /**
   *  Get resource size
   *
   *  @return  Size in bytes of the resource
   */
    public long getSize() {
        return size;
    }

    /**
   *  Set resource duration
   *
   *  @param  duration    Duration of resource in [H+]:MM:SS[.F+] format.
   *                      HH portion is not required, nor is the fractional
   *                      second portion.  '+' means 1 or more of the 
   *                      preceding digit type is allowed.
   *
   */
    public void setDuration(String duration) {
        this.duration = duration;
    }

    /**
   *  Set resource duration from a duration specified in seconds. handles
   *  conversion to HH:MM:SS 
   *
   *  @param  duration    Duration of resource in seconds
   */
    static NumberFormat nf2 = null;

    public void setDurationSecs(int durationSecs) {
        if (nf2 == null) {
            nf2 = NumberFormat.getInstance();
            nf2.setMinimumIntegerDigits(2);
            nf2.setGroupingUsed(false);
        }
        int hour = (int) (durationSecs / 3600);
        int minute = (int) ((durationSecs % 3600) / 60);
        int sec = (int) durationSecs % 60;
        this.duration = nf2.format(hour) + ":" + nf2.format(minute) + ":" + nf2.format(sec);
    }

    public int getDurationSecs() {
        int durationSecs = 0;
        String noMillisec = getDurationNoMillisec();
        if (noMillisec != null && noMillisec.length() > 0) {
            String[] hhmmss = noMillisec.split(":");
            if (hhmmss.length == 1) {
                durationSecs = Integer.parseInt(hhmmss[0]);
            } else if (hhmmss.length == 2) {
                durationSecs = Integer.parseInt(hhmmss[0]) * 60 + Integer.parseInt(hhmmss[1]);
            } else if (hhmmss.length == 3) {
                durationSecs = Integer.parseInt(hhmmss[0]) * 3660 + Integer.parseInt(hhmmss[1]) * 60 + Integer.parseInt(hhmmss[2]);
            }
        }
        return durationSecs;
    }

    /**
   *  Get resource duration
   *
   *  @return  Duration string
   */
    public String getDuration() {
        return duration;
    }

    /**
   * Convenience routine to return duration with only seconds resolution
   * Some A/V servers have millisecond resolution for duration, e.g.:
   *   "01:03:45.073"
   */
    public String getDurationNoMillisec() {
        if (duration == null) return null;
        int index = duration.indexOf(".");
        if (index > 0) return duration.substring(0, index);
        return duration;
    }

    /**
   *  Set bit rate
   *
   *  @param  bitRate    Bit rate in bytes/sec
   */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /**
   *  Get bit rate
   *
   *  @return  Bit rate in bytes/sec
   */
    public int getBitRate() {
        return bitRate;
    }

    /**
   *  Get estimated bit rate, based on song size & duration if true
   *  bit rate is not available
   *
   *  @return  Bit rate in bytes/sec, or -1 if not enough data to estimate
   */
    public int getEstimatedBitRate() {
        if (bitRate != -1) return bitRate;
        int bytesPerSec = -1;
        int secs = getDurationSecs();
        if ((secs > 0) && (size > 0)) bytesPerSec = (int) (size / secs);
        return bytesPerSec;
    }

    /**
   *  Set sample frequency for audio component of media object
   *
   *  @param  sampleFreq    Sample frequency in HZ
   */
    public void setSampleFreq(int sampleFreq) {
        this.sampleFreq = sampleFreq;
    }

    /**
   *  Get sample frequency for audio component of media object
   *
   *  @return  Sample frequency in HZ
   */
    public int getSampleFreq() {
        return sampleFreq;
    }

    /**
   *  Set bits per sample for audio component of media object
   *
   *  @param  bitsPerSample    Number of bits per sample
   */
    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    /**
   *  Get bits per sample for audio component of media object
   *
   *  @return  Number of bits per sample
   */
    public int getBitsPerSample() {
        return bitsPerSample;
    }

    /**
   *  Set number of audio channels for media object
   *
   *  @param  numChannels      Number of channels
   */
    public void setNumAudioChannels(int nrAudioChannels) {
        this.nrAudioChannels = nrAudioChannels;
    }

    /**
   *  Get number of audio channels for media object
   *
   *  @return  Number of channels
   */
    public int getNumAudioChannels() {
        return nrAudioChannels;
    }

    /**
   *  Set X,Y resolution
   *
   *  @param  resolution    Resolution of the form "<X>x<Y>", for example
   *                        "640x480". Used for image/photo items
   */
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    /**
   *  Get X,Y resolution
   *
   *  @return  Resolution string  (like "640x480")
   */
    public String getResolution() {
        return resolution;
    }

    public int getWidth() {
        if (resolution == null) return -1;
        String[] res = resolution.split("[xX]");
        if (res.length != 2) return -1;
        return Integer.parseInt(res[0]);
    }

    public int getHeight() {
        if (resolution == null) return -1;
        String[] res = resolution.split("[xX]");
        if (res.length != 2) return -1;
        return Integer.parseInt(res[1]);
    }

    /**
   *  Set protocol infomation string
   *
   *  @param  protocolInfo    String identifying the recommended HTTP protocol
   *                          for transmitting the resource. Examples are:
   *          
   *                          "http-get:*:audio:mpeg:*" and
   *                          "rtsp:*:audio/m3u:*"
   *
   */
    public void setProtocolInfo(String protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    /**
   *  Get protocol infomation string
   *
   *  @return  Protocol information string ( e.g. "http-get:*:audio:mpeg:*" )
   */
    public String getProtocolInfo() {
        return protocolInfo;
    }

    /**
   *  Set protection type
   *
   *  @param  protection    Protection string (not standardized by UPNP)
   */
    public void setProtection(String protection) {
        this.protection = protection;
    }

    /**
   *  Get protection type
   *
   *  @return  Protection string (not standardized by UPNP)
   */
    public String getProtection() {
        return protection;
    }

    /**
   *  Set an object's importURI.
   *
   *  @param  id    URI string 
   */
    public void setImportURI(String importURI) {
        this.importURI = importURI;
    }

    /**
   *  Get object's importURI.
   *
   *  @return  URI string 
   */
    public String getImportURI() {
        return importURI;
    }

    /**
   *  Check if resource is a recognized playlist MIME-type.  
   *
   *  @return  URI string 
   */
    public boolean isPlaylist() {
        if ((protocolInfo.indexOf("/mpegurl") > 0) || (protocolInfo.indexOf("/x-mpegurl") > 0) || (protocolInfo.indexOf("/x-scpls") > 0)) {
            return true;
        } else {
            return false;
        }
    }

    public CDSObjectList getPlaylistItems() {
        return getPlaylistItems(false);
    }

    public CDSObjectList getPlaylistItems(boolean isAudioBroadcastPlaylist) {
        if (!isPlaylist()) {
            logger.warning("Resource is not a playlist");
            return null;
        }
        try {
            if (protocolInfo.indexOf("mpegurl") > 0) {
                M3UPlaylist m3uPlaylist = new M3UPlaylist(new URL(name), isAudioBroadcastPlaylist);
                if (m3uPlaylist.size() == 0) return null;
                return m3uPlaylist.getObjectList();
            } else if (protocolInfo.indexOf("scpls") > 0) {
                PLSPlaylist plsPlaylist = new PLSPlaylist(new URL(name), isAudioBroadcastPlaylist);
                if (plsPlaylist.size() == 0) return null;
                return plsPlaylist.getObjectList();
            } else {
                logger.warning("Unsupported playlist type (" + protocolInfo + ")");
                return null;
            }
        } catch (PlaylistException e) {
            logger.warning("Exception " + e);
            return null;
        } catch (MalformedURLException e) {
            logger.warning("Exception " + e);
            return null;
        } catch (IOException e) {
            logger.warning("Exception " + e);
            return null;
        }
    }

    /**
   * Test if resource is a windows media audio mime-type 
   */
    public boolean isWMA() {
        if (protocolInfo.indexOf("/x-ms-wma") > 0) return true;
        return false;
    }

    /**
   *  Get image icon from resource. Icon is cached for efficiency
   * 
   * @return icon object or null if resource is not some type of image 
   */
    ImageIcon imageIcon = null;

    public ImageIcon getImageIcon() {
        if (imageIcon == null) {
            try {
                logger.fine("Creating icon for resource '" + name + "'");
                long startTime = System.currentTimeMillis();
                if (name.startsWith("http:") || name.startsWith("file:")) {
                    URL url = new URL(name);
                    if (protocolInfo.indexOf("image/bmp") >= 0) {
                        imageIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(BMPReader.getBMPImage(url.openStream())));
                    } else {
                        imageIcon = new ImageIcon(url);
                    }
                } else {
                    imageIcon = new ImageIcon(name);
                }
                logger.fine("Icon creation time (ms): " + (System.currentTimeMillis() - startTime));
            } catch (MalformedURLException e) {
                logger.warning("Exception " + e);
                return null;
            } catch (IOException e) {
                logger.warning("Exception " + e);
                return null;
            }
        }
        return imageIcon;
    }

    /**
   *  Generate XML version of resource
   *
   *  @return  resource string
   *
   *  TODO: Only currently supporting a subset of attributes
   */
    public String toXML(CDSFilter filter) {
        StringBuffer buf = new StringBuffer();
        buf.append("  <res");
        buf.append(" protocolInfo=\"" + protocolInfo + "\"");
        if ((size != -1) && filter.propertyEnabled("res@size")) buf.append(" size=\"" + size + "\"");
        if ((duration != null) && filter.propertyEnabled("res@duration")) buf.append(" duration=\"" + duration + "\"");
        if ((resolution != null)) buf.append(" resolution=\"" + resolution + "\"");
        if ((bitRate != -1) && filter.propertyEnabled("res@bitRate")) buf.append(" bitrate=\"" + bitRate + "\"");
        if ((sampleFreq != -1) && filter.propertyEnabled("res@sampleFrequency")) buf.append(" sampleFrequency=\"" + sampleFreq + "\"");
        if ((bitsPerSample != -1) && filter.propertyEnabled("res@bitsPerSample")) buf.append(" bitsPerSample=\"" + bitsPerSample + "\"");
        if ((nrAudioChannels != -1) && filter.propertyEnabled("res@nrAudioChannels")) buf.append(" nrAudioChannels=\"" + nrAudioChannels + "\"");
        if ((protection != null) && filter.propertyEnabled("res@protection")) buf.append(" protection=\"" + protection + "\"");
        if ((importURI != null) && filter.propertyEnabled("res@importURI")) buf.append(" importURI=\"" + importURI + "\"");
        buf.append(">");
        buf.append(XML.escapeXMLChars(name));
        buf.append("</res>\n");
        return buf.toString();
    }
}
