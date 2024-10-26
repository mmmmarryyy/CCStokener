package gotha;

import java.awt.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.rmi.RemoteException;
import javax.help.*;

/**
 * Contains general purpose constants
 * and simple static general purpose methods
 * @author Luc Vannier
 */
public class Gotha {

    static Locale locale = Locale.getDefault();

    static final long GOTHA_VERSION = 318L;

    static final long GOTHA_MINOR_VERSION = 3L;

    static final java.util.Date GOTHA_RELEASE_DATE = (new GregorianCalendar(2010, Calendar.MAY, 25)).getTime();

    static final long GOTHA_DATA_VERSION = 105L;

    static final int MAX_NUMBER_OF_ROUNDS = 20;

    static final int MAX_NUMBER_OF_PLAYERS = 1200;

    static final int MAX_NUMBER_OF_CATEGORIES = 5;

    static final int MAX_RANK = 8;

    static final int MIN_RANK = -30;

    static final int MAX_NUMBER_OF_TABLES = MAX_NUMBER_OF_PLAYERS / 2;

    static final int RUNNING_MODE_UNDEFINED = 0;

    static final int RUNNING_MODE_SAL = 1;

    static final int RUNNING_MODE_SRV = 2;

    static final int RUNNING_MODE_CLI = 3;

    static int runningMode = RUNNING_MODE_UNDEFINED;

    static String serverName = "";

    static String clientName = "";

    public static File runningDirectory;

    public static File tournamentDirectory;

    public static File exportDirectory;

    public static File exportHTMLDirectory;

    public static File exportXMLDirectory;

    public static String getGothaVersionNumber() {
        int mainVersion = (int) (GOTHA_VERSION / 100L);
        int auxVersion = (int) (GOTHA_VERSION % 100L);
        String strMainVersion = "" + mainVersion;
        String strAuxVersion = "" + auxVersion;
        if (auxVersion <= 9) strAuxVersion = "0" + auxVersion;
        return strMainVersion + "." + strAuxVersion;
    }

    /**
     * Returns X.yy if minor version = 0
     * Returns X.yy.zz if minor version != 0
     */
    public static String getGothaFullVersionNumber() {
        int minorVersion = (int) GOTHA_MINOR_VERSION;
        String strMinorVersion = "";
        if (minorVersion != 0) {
            strMinorVersion += minorVersion;
            if (minorVersion <= 9) strMinorVersion = "0" + minorVersion;
            strMinorVersion = "." + strMinorVersion;
        }
        return getGothaVersionNumber() + strMinorVersion;
    }

    public static String getGothaReleaseMonthYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMM yyyy", Gotha.locale);
        return sdf.format(GOTHA_RELEASE_DATE);
    }

    private static String getGothaName() {
        return "OpenGotha";
    }

    public static String getGothaVersionnedName() {
        return getGothaName() + " " + getGothaFullVersionNumber();
    }

    public static String getCopyLeftText() {
        String str = Gotha.getGothaVersionnedName();
        str += "    " + Gotha.getGothaReleaseMonthYear();
        str += "\n" + "OpenGotha is a free software";
        str += "\n" + "You may copy and deal it as far as you respect the terms of";
        str += "\n" + "GPL licence (GNU Public Licence)";
        str += "\n" + "as published by the Free Software Foundation";
        str += "\n\n" + "Full text of licence can be found in gpl.txt";
        str += "\n\n" + "OpenGotha project site is hosted by \"Google code\".";
        str += "\n" + "Use OpenGotha project site to get sources, report issues or ask for new features.";
        str += "\n" + "http://code.google.com/p/opengotha/ ";
        return str;
    }

    public static String getThanksToText() {
        String str = "";
        str += "\n\nOpenGotha  has  been designed and written by Luc Vannier,";
        str += "\nwith precious help from many people.";
        str += "\nMaximum matching algorithm is a development by UCSB JICOS project";
        str += "\nof an O(n^3) implementation of Edmonds' algorithm, as presented";
        str += "\nby Harold N. Gabow. Jean-Fran�ois Bocquet adapted the algorithm";
        str += "\nto 64 bits";
        str += "\nDirect Confrontation algorithm has been designed and written by";
        str += "\nMatthieu Walraet";
        str += "\nMulti-Format File Reading is managed by SmartEncodingInputStream,";
        str += "\nwritten by Guillaume Laforge";
        str += "\nAnd thanks, also to Paul Baratou, Claude Brisson, Claude Burvenich,";
        str += "\nTilo Dickopp, Andr� Engels, Marc Krauth, Roland Lezuo,";
        str += "\nDamien Martin-Guillerez, Richard Mullens, Fran�ois Mizessyn,";
        str += "\nKonstantin Pelepelin, Philippe Pelleter, Sylvain Ravera,";
        str += "\nYves Rutschle, Wandrille Sacqu�p�e, Emeric Salmon and many others.";
        return str;
    }

    /** Converts a yyyy-MM-dd String (ISO-8601) into a Date object
     *  returns null if strDate is not a valid String
     */
    public static java.util.Date convertStringToDate(String strDate) {
        strDate = strDate.trim();
        if (strDate.length() != 10) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(strDate);
        } catch (ParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a String with numberOfCharacters characters, whatever the length of str is.
     */
    public static String leftString(String str, int numberOfCharacters) {
        String resStr;
        if (str == null) resStr = ""; else resStr = str;
        while (resStr.length() < numberOfCharacters) resStr = resStr + " ";
        resStr = resStr.substring(0, numberOfCharacters);
        return resStr;
    }

    public static Image getIconImage() {
        URL iconURL = null;
        try {
            iconURL = new URL("file:///" + Gotha.runningDirectory + "/resources/gothalogo64.jpg");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        ImageIcon ico = new ImageIcon();
        if (iconURL != null) ico = new ImageIcon(iconURL);
        return ico.getImage();
    }

    /**
     * for debug purpose
     */
    public static void printTopChrono(String str) {
        long topC = System.nanoTime();
        long nSEC = (topC / 1000000000) % 1000;
        long nMS = (topC % 1000000000) / 1000000;
        String strSEC = "000" + nSEC;
        strSEC = strSEC.substring(strSEC.length() - 3);
        String strMS = "000" + nMS;
        strMS = strMS.substring(strMS.length() - 3);
        System.out.println("topCn = " + strSEC + "." + strMS + " " + str);
    }

    public static TournamentInterface getTournamentFromFile(File f) throws FileNotFoundException {
        TournamentInterface t = null;
        FileInputStream fis = new FileInputStream(f);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(fis);
            if (ois == null) {
                System.out.println("ois = null");
            }
            t = (TournamentInterface) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ex) {
            Logger.getLogger(JFrGotha.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JFrGotha.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            t.getKeyName();
        } catch (RemoteException ex) {
            Logger.getLogger(JFrGotha.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return t;
    }

    public static void download(JProgressBar pgb, String strURL, File fDestination) throws MalformedURLException, IOException {
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        URL url = new URL(strURL);
        URLConnection urlc = url.openConnection();
        bis = new BufferedInputStream(urlc.getInputStream());
        fos = new FileOutputStream(fDestination);
        int i;
        int contentLength = urlc.getContentLength();
        int nbChars = 0;
        if (pgb != null) {
            pgb.setValue(0);
            pgb.setVisible(true);
        }
        while ((i = bis.read()) != -1) {
            fos.write(i);
            nbChars++;
            if (nbChars % 2000 == 0) {
                int percent = 100 * nbChars / contentLength;
                if (pgb != null) {
                    pgb.setValue(percent);
                    pgb.paintImmediately(0, 0, pgb.getWidth(), pgb.getHeight());
                }
            }
        }
        if (pgb != null) {
            pgb.setVisible(false);
        }
        fos.close();
        bis.close();
    }

    public static String getHostName() {
        String hostName = "";
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            hostName = addr.getHostName();
        } catch (java.net.UnknownHostException e) {
        }
        return hostName;
    }

    /**
     * gets all available InetAddresses from all available NetworkInterfaces
     *
     * @return
     */
    public static ArrayList<InetAddress> getAvailableIPAddresses() {
        ArrayList<InetAddress> al = new ArrayList<InetAddress>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress adIP = enumIpAddr.nextElement();
                    al.add(adIP);
                }
            }
        } catch (SocketException e) {
            System.out.println(" (error retrieving network interface list)");
        }
        return al;
    }

    /**
     * chooses the best IP Address
     * eliminates :
     * <br> non IP32 formed addresses
     * <br> loopback addresses
     * <br> stops elimination process when only one address remains.
     * <br> if several addresses remains, returns the first one
     * @param alIPAd
     * @return
     */
    public static InetAddress getBestIPAd() {
        ArrayList<InetAddress> alIPAd = Gotha.getAvailableIPAddresses();
        if (alIPAd.size() == 0) return null;
        if (alIPAd.size() == 1) return alIPAd.get(0);
        for (int i = alIPAd.size() - 1; i >= 0; i--) {
            if (alIPAd.size() == 1) return alIPAd.get(0);
            InetAddress ipAd = alIPAd.get(i);
            byte[] b = ipAd.getAddress();
            if (b.length != 4) alIPAd.remove(i);
        }
        for (int i = alIPAd.size() - 1; i >= 0; i--) {
            if (alIPAd.size() == 1) return alIPAd.get(0);
            InetAddress ipAd = alIPAd.get(i);
            byte[] b = ipAd.getAddress();
            if (b[0] == 127) alIPAd.remove(i);
        }
        return alIPAd.get(0);
    }

    public static void displayGothaHelp(String topic) {
        HelpSet hs = null;
        File f = new File(Gotha.runningDirectory + "/gothahelp/helpset.hs");
        try {
            URI uri = f.toURI();
            URL url = uri.toURL();
            hs = new HelpSet(null, url);
            HelpBroker hb = hs.createHelpBroker();
            hb.setCurrentID(topic);
            new CSH.DisplayHelpFromSource(hb);
            hb.setDisplayed(true);
        } catch (MalformedURLException ex) {
            Logger.getLogger(JFrGotha.class.getName()).log(Level.SEVERE, null, ex);
        } catch (HelpSetException ex) {
            Logger.getLogger(JFrGotha.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadIDException ex) {
            System.out.println("Non existing topic");
        }
    }
}
