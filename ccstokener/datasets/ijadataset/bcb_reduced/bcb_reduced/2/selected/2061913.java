package org.jmule.core.protocol.donkey;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jmule.util.LogUtil;
import org.jmule.util.VectorNotifier;
import org.jmule.util.XMLWriter;

/** Handles list of the eDonkey2000 servers.
 * Take a look at description of {@link org.jmule.core.protocol.donkey.DonkeyServerList#save(String fileName) save(String fileName)}
 * for serverlist xml example.
 * @author Frank Viernau
 * @author emarant 
 * @version $Revision: 1.2 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/09/09 15:28:34 $
 */
public class DonkeyServerList extends VectorNotifier implements org.jmule.util.XMLnode {

    static final Logger log = Logger.getLogger(DonkeyServerList.class.getName());

    static FileInputStream smet;

    static byte xx[];

    static int size;

    static LinkedList output;

    public static LinkedList importServerMet(File serverMet) throws IOException {
        if (!(serverMet.exists() && serverMet.canRead())) throw new IOException("Can't read file \"" + serverMet.toString() + "\".");
        smet = new FileInputStream(serverMet);
        byte x[] = new byte[smet.available()];
        size = smet.read(x);
        xx = x;
        return analyze(addbytes(1, 4));
    }

    private static int readbyte(int pos) {
        if ((int) xx[pos] < 0) {
            return (256 + (int) xx[pos]);
        } else {
            return (int) xx[pos];
        }
    }

    private static String readstring(int pos, int length) {
        String word = "";
        for (int i = 0; i < length; ++i) {
            word += (char) readbyte(pos + i);
        }
        return word;
    }

    private static int addbytes(int pos, int length) {
        double sum = 0;
        for (double i = 0; i < length; ++i) {
            sum += (double) readbyte(pos + (int) i) * Math.pow((double) 16, 2 * i);
        }
        return (int) sum;
    }

    private static String readip(int pos) {
        String ip = "";
        for (int i = 0; i < 4; ++i) {
            ip += readbyte(pos + i);
            if (i < 3) {
                ip += ".";
            }
            ;
        }
        return ip;
    }

    private static LinkedList analyze(int servers) {
        int pos = 5;
        int numberoftags, tagtype = -1;
        LinkedList serv = new LinkedList();
        for (int i = 0; i < servers; ++i) {
            DonkeyServer ds = new DonkeyServer(new InetSocketAddress(readip(pos), addbytes(pos + 4, 2)));
            pos += 6;
            numberoftags = addbytes(pos, 4);
            pos += 4;
            for (int j = 0; j < numberoftags; ++j) {
                tagtype = readbyte(pos);
                ++pos;
                if (addbytes(pos, 2) == 1) {
                    pos += 2;
                    if (readbyte(pos) == 1) {
                        ds.setName(readstring(pos + 3, addbytes(pos + 1, 2)));
                    }
                    if (readbyte(pos) == 11) {
                        ds.setDescription(readstring(pos + 3, addbytes(pos + 1, 2)));
                    }
                    if (readbyte(pos) == 14) {
                        ds.setPriority(addbytes(pos + 1, 4));
                    }
                    if (tagtype == 2) {
                        pos += addbytes(pos + 1, 2) + 3;
                    } else {
                        pos += 5;
                    }
                } else {
                    pos += addbytes(pos, 2) + 6;
                }
            }
            serv.add(ds);
        }
        return serv;
    }

    public static void getFromUrl(URL url, File target) throws Exception {
        LogUtil.entering(log, "Getting serverlist from " + url.toString() + ".");
        if (target.exists()) {
            if (!target.delete()) throw new Exception("File " + target.toString() + " exists, can't delete.");
        }
        target.createNewFile();
        URLConnection urlCon = url.openConnection();
        InputStream inStream = new BufferedInputStream(urlCon.getInputStream());
        FileOutputStream outStream = new FileOutputStream(target);
        byte[] buffer = new byte[2048];
        long totalSize = 0;
        try {
            while (true) {
                int nbytes = inStream.read(buffer);
                if (nbytes <= 0) break;
                totalSize += nbytes;
                outStream.write(buffer, 0, nbytes);
            }
        } catch (IOException ioe) {
            log.warning("Error: " + ioe.getMessage());
        }
        inStream.close();
        outStream.close();
        log.finest("Wrote " + totalSize + "bytes to " + target.toString());
    }

    private DonkeyServer lastserver = null;

    /**
     * Iterates, *endless*, throught the serverlist for autoconnecting propose.
     * @return if one exsist a DonkeyServer with isAutoConnectAllowed()==<tt>true</tt>, otherwise <tt>null</tt>
     * @see org.jmule.core.protocol.donkey.DonkeyServerList#save(String fileName)
     */
    public DonkeyServer nextServer() {
        int in, count = 0;
        if (!this.isEmpty()) {
            do {
                if (lastserver != null && (in = indexOf(lastserver)) != -1 && ++in < this.size()) {
                    lastserver = (DonkeyServer) this.elementAt(in);
                } else {
                    lastserver = (DonkeyServer) this.firstElement();
                }
                if (lastserver.isAutoConnectAllowed()) {
                    return lastserver;
                }
            } while (count++ < this.size());
            lastserver = null;
        } else {
            lastserver = null;
        }
        return lastserver;
    }

    /** Imports all servers from the given serverListUrls. 
         * @param serverListUrls List of url's to the server.met files.
         */
    public void update(LinkedList serverListUrls) {
        LogUtil.entering(log);
        int i = 1;
        Iterator it = serverListUrls.iterator();
        while (it.hasNext()) {
            URL url = (URL) it.next();
            File newServerMet = new File("server" + i + ".met");
            try {
                getFromUrl(url, newServerMet);
                addAll(importServerMet(newServerMet));
                i++;
            } catch (Exception e) {
                log.warning("Updating server list failed: " + e.toString());
            }
        }
    }

    /** 
         * @see java.util.Collection#add(Object)
         */
    public boolean add(Object arg0) {
        if (arg0 instanceof DonkeyServer) {
            DonkeyServer newServer = (DonkeyServer) arg0;
            if (!this.contains(newServer)) {
                if (!newServer.hasBadInternetAddress()) {
                    log.fine("Adding new server \"" + newServer.getName() + "\"");
                    newServer.setServerLisServer(this);
                    return super.add(newServer);
                }
            } else {
                ((DonkeyServer) this.get(this.indexOf(newServer))).setAlive();
            }
        }
        return false;
    }

    /** @see java.util.Collection#addAll(Collection) */
    public boolean addAll(Collection arg0) {
        Iterator it = arg0.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
        return true;
    }

    /**
    * Removes *dead* Servers  from the list.
    * If criticalage is less than time between last notification of a server and now, a mortal Server is called *dead*.
    * @param criticalage in ms.
    * @return number of removed Servers
    * @see org.jmule.core.protocol.donkey.DonkeyServerList#save(String fileName)
    */
    public int pruneList(long criticalage) {
        int removed = 0;
        long now = System.currentTimeMillis();
        ListIterator lit = this.listIterator();
        DonkeyServer ds = null;
        while (lit.hasNext()) {
            ds = (DonkeyServer) lit.next();
            if (ds.isStaticDns()) {
                ds.resolve();
            }
        }
        if (ds != null && !ds.isImmortal() && (now - ds.getLastTimeKnown() > criticalage)) {
            lit.remove();
            removed++;
        }
        while (lit.hasPrevious()) {
            ds = (DonkeyServer) lit.previous();
            if (!ds.isImmortal() && (now - ds.getLastTimeKnown() > criticalage)) {
                lit.remove();
                removed++;
            }
        }
        return removed;
    }

    public DonkeyServer getIP(String ipport) throws IllegalArgumentException, NoSuchElementException {
        Pattern p = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
        Matcher m = p.matcher(ipport);
        int pos = -1;
        if (m.matches()) {
            pos = super.indexOf(new DonkeyServer(new InetSocketAddress(m.group(1), Integer.parseInt(m.group(2)))));
        } else {
            throw new IllegalArgumentException("no ip:port " + ipport);
        }
        if (pos == -1) {
            throw new NoSuchElementException(ipport);
        }
        return (DonkeyServer) super.get(pos);
    }

    public boolean removeIP(String ipport) throws IllegalArgumentException {
        Pattern p = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
        Matcher m = p.matcher(ipport);
        if (m.matches()) {
            return super.remove(new DonkeyServer(new InetSocketAddress(m.group(1), Integer.parseInt(m.group(2)))));
        } else {
            throw new IllegalArgumentException("no ip:port " + ipport);
        }
    }

    public static String TAG_edonkeyServerList = "edonkeysServerList";

    public static String TAG_edonkeyServer = "server";

    public static String TAG_formatVersion = "formatVersion";

    public static String TAG_ip = "ip";

    public static String TAG_tcpPort = "tcpPort";

    public static String TAG_dns = "dns";

    public static String TAG_method = "method";

    public static String Tag_name = "name";

    public static String Tag_description = "description";

    public static int THISFORMATVERSION = 1;

    public org.jmule.util.XMLnode fromXml(String xmlData) throws org.jmule.core.InvalidXmlStructureException {
        try {
            String[] splitSet;
            splitSet = org.jmule.util.XMLReader.parseAndSplitXMLData(xmlData);
            if (splitSet[0].equals(TAG_edonkeyServerList)) {
                splitSet = org.jmule.util.XMLReader.parseAndSplitXMLData(splitSet[1]);
                while (splitSet[0] != null) {
                    if (splitSet[0].equals(TAG_edonkeyServer)) {
                        DonkeyServer ds = new DonkeyServer(new InetSocketAddress(0));
                        ds = (DonkeyServer) ds.fromXml(splitSet[1]);
                        if (ds != null) {
                            add(ds);
                        }
                    }
                    splitSet = org.jmule.util.XMLReader.parseAndSplitXMLData(splitSet[2]);
                }
            }
        } catch (org.jmule.core.InvalidXmlStructureException ioe) {
            log.warning("problem parsing file cause of " + ioe.getMessage());
        }
        return this;
    }

    /**
     * loads serverlist from *.xml;
     * @param fileName if <tt>null</tt> the edonkeyservers.xml is used
     * @see org.jmule.core.protocol.donkey.DonkeyServerList#save(String fileName)
     */
    public void load(String fileName) throws IOException {
        log.fine("loading serverlist");
        if (fileName == null) fileName = "edonkeyservers.xml";
        try {
            org.jmule.util.XMLReader reader = new org.jmule.util.XMLReader(this);
            reader.load(fileName);
            reader.read();
        } catch (IOException ioe) {
            log.warning(ioe.getMessage());
        } catch (org.jmule.core.InvalidXmlStructureException ixse) {
            log.warning(ixse.getMessage());
        }
    }

    /**
    * saves serverlist as *.xml; use export to get server.met<br>
    * first version.<br>
    * method - tag:
    * <ul>manual - no autoconnecting allowed</ul>
    * <ul>immortal - never remove server automatic from list ;)</ul>
    * example file:<br>
    * <pre> &lt;?xml version="1.0" encoding="UTF-8"?&gt;
    * &lt;edonkeysServerList&gt;
    *     &lt;formatVersion&gt; 1 &lt;/formatVersion&gt;
    *     &lt;server&gt;
    *         &lt;method&gt; immortal auto &lt;/method&gt;
    *         &lt;ip&gt; 123.45.67.89 &lt;/ip&gt;
    *         &lt;tcpPort&gt; 4242 &lt;/tcpPort&gt;
    *         &lt;priority&gt; 0 &lt;/priority&gt;
    *     &lt;/server&gt;
    *     &lt;server&gt;
    *         &lt;method&gt; mortal manual &lt;/method&gt;
    *         &lt;dns&gt; mydomain.dyndns.org &lt;/dns&gt;
    *         &lt;ip&gt; 234.56.78.91 &lt;/ip&gt;
    *         &lt;tcpPort&gt; 4661 &lt;/tcpPort&gt;
    *         &lt;priority&gt; 0 &lt;/priority&gt;
    *     &lt;/server&gt;
    *     &lt;server&gt;
    *         &lt;method&gt; mortal auto &lt;/method&gt;
    *         &lt;ip&gt; 34.56.78.90 &lt;/ip&gt;
    *         &lt;tcpPort&gt; 4661 &lt;/tcpPort&gt;
    *         &lt;priority&gt; 0 &lt;/priority&gt;
    *     &lt;/server&gt;
    *&lt;/edonkeysServerList&gt;
    *</pre>
    * @param fileName if <tt>null</tt> the edonkeyservers.xml is used
    */
    public void save(String fileName) throws IOException {
        log.fine("Saving serverlist");
        String configDir = ".";
        if (fileName == null) fileName = "edonkeyservers";
        File stateFile = new File(configDir + File.separatorChar + fileName + ".xml");
        File stateTmpFile = new File(configDir + File.separatorChar + fileName + ".xml" + ".tmp");
        XMLWriter xw = new XMLWriter(stateTmpFile);
        xw.startDocument();
        xw.startElement(TAG_edonkeyServerList);
        xw.addField(TAG_formatVersion, Integer.toString(THISFORMATVERSION));
        Iterator it = this.iterator();
        while (it.hasNext()) {
            DonkeyServer ds = (DonkeyServer) it.next();
            xw.startElement(TAG_edonkeyServer);
            String methoditem = "mortal";
            if (ds.isImmortal()) {
                methoditem = "immortal";
            }
            if (!ds.isAutoConnectAllowed()) {
                methoditem = methoditem + " manual";
            } else {
                methoditem = methoditem + " auto";
            }
            xw.addField(TAG_method, methoditem);
            if (ds.isStaticDns()) {
                xw.addField(TAG_dns, ds.getStaticDns());
            }
            xw.addField(TAG_ip, ds.getSocketAddress().getAddress().getHostAddress());
            xw.addField(TAG_tcpPort, Integer.toString(ds.getSocketAddress().getPort()));
            if (!ds.getName().equals("no name")) {
                xw.addField(Tag_name, XMLWriter.encodeSequence(ds.getName()));
            }
            if (!ds.getDescription().equals("no description")) {
                xw.addField(Tag_description, XMLWriter.encodeSequence(ds.getDescription()));
            }
            xw.endElement();
        }
        xw.endElement();
        xw.endDocument();
        stateFile.delete();
        stateTmpFile.renameTo(stateFile);
    }

    void fireServerChange(DonkeyServer item) {
        super.fireEventItemChanged(item);
    }

    public void disconnect() {
        DonkeyServer aserver = DonkeyProtocol.getInstance().getCurrentServer();
    }
}
