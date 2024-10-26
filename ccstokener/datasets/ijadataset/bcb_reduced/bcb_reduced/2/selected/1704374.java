package de.moonflower.jfritz.struct;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import de.moonflower.jfritz.JFritz;
import de.moonflower.jfritz.Main;
import de.moonflower.jfritz.dialogs.quickdial.QuickDials;
import de.moonflower.jfritz.dialogs.sip.SipProvider;
import de.moonflower.jfritz.exceptions.InvalidFirmwareException;
import de.moonflower.jfritz.exceptions.WrongPasswordException;
import de.moonflower.jfritz.firmware.FritzBoxFirmware;
import de.moonflower.jfritz.utils.Debug;
import de.moonflower.jfritz.utils.Encryption;
import de.moonflower.jfritz.utils.JFritzUtils;
import de.moonflower.jfritz.utils.network.UPNPAddonInfosListener;
import de.moonflower.jfritz.utils.network.AddonInfosXMLHandler;
import de.moonflower.jfritz.utils.network.UPNPCommonLinkPropertiesListener;
import de.moonflower.jfritz.utils.network.UPNPExternalIpListener;
import de.moonflower.jfritz.utils.network.UPNPStatusInfoListener;
import de.moonflower.jfritz.utils.network.UPNPUtils;

public class FritzBox {

    private static String POSTDATA_CLEAR = "&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Amenu=fon&telcfg%3Asettings/ClearJournal=1";

    private static String POSTDATA_SIPPROVIDER = "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=siplist&login%3Acommand%2Fpassword=";

    private static String POSTDATA_LIST = "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=foncalls&login%3Acommand%2Fpassword=";

    private static String POSTDATA_FETCH_CALLERLIST = "getpage=../html/$LANG/$CSV_FILE&errorpage=..%2Fhtml%2F$LANG%2Fmenus%2Fmenu2.html&var%3Alang=$LANG&var%3Apagename=foncalls&var%3Aerrorpagename=foncalls&var%3Amenu=fon&var%3Apagemaster=&time%3Asettings%2Ftime=1136559837%2C-60";

    private static String POSTDATA_QUICKDIAL = "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=kurzwahlen&login%3Acommand%2Fpassword=";

    private static String POSTDATA_QUICKDIAL_NEW = "&var%3Alang=$LANG&var%3Amenu=fon&var%3Apagename=fonbuch&login%3Acommand%2Fpassword=";

    private static String POSTDATA_LOGIN = "&login:command/password=$PASSWORT";

    private static String POSTDATA_CALL = "&login:command/password=$PASSWORT&telcfg:settings/UseClickToDial=1&telcfg:settings/DialPort=$NEBENSTELLE&telcfg:command/Dial=$NUMMER";

    private static String POSTDATA_HANGUP = "&login:command/password=$PASSWORT&telcfg:settings/UseClickToDial=1&telcfg:command%2FHangup";

    private static String URL_SERVICE_ADDONINFOS = ":49000/upnp/control/WANCommonIFC1";

    private static String URN_SERVICE_ADDONINFOS = "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1#GetAddonInfos";

    private static String URL_SERVICE_DSLLINK = ":49000/upnp/control/WANDSLLinkC1";

    private static String URN_SERVICE_DSLLINK = "urn:schemas-upnp-org:service:WANDSLLinkConfig:1#GetDSLLinkInfo";

    private static String URL_SERVICE_EXTERNALIP = ":49000/upnp/control/WANIPConn1";

    private static String URN_SERVICE_EXTERNALIP = "urn:schemas-upnp-org:service:WANIPConnection:1#GetExternalIPAddress";

    private static String URL_SERVICE_STATUSINFO = ":49000/upnp/control/WANIPConn1";

    private static String URN_SERVICE_STATUSINFO = "urn:schemas-upnp-org:service:WANIPConnection:1#GetStatusInfo";

    private static String URL_SERVICE_COMMONLINK = ":49000/upnp/control/WANCommonIFC1";

    private static String URN_SERVICE_COMMONLINK = "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1#GetCommonLinkProperties";

    private static String URL_SERVICE_GETINFO = ":49000/upnp/control/any";

    private static String URN_SERVICE_GETINFO = "urn:schemas-any-com:service:Any:1#GetInfo";

    private static String URL_SERVICE_AUTOCONFIG = ":49000/upnp/control/WANDSLLinkC1";

    private static String URN_SERVICE_AUTOCONFIG = "urn:schemas-upnp-org:service:WANDSLLinkConfig:1#GetAutoConfig";

    private static String URL_SERVICE_CONNECTIONTYPEINFO = ":49000/upnp/control/WANIPConn1";

    private static String URN_SERVICE_CONNECTIONTYPEINFO = "urn:schemas-upnp-org:service:WANIPConnection:1#GetConnectionTypeInfo";

    private static String URL_SERVICE_GENERICPORTMAPPING = ":49000/upnp/control/WANIPConn1";

    private static String URN_SERVICE_GENERICPORTMAPPING = "urn:schemas-upnp-org:service:WANIPConnection:1#GetGenericPortMappingEntry";

    private static final String CSV_FILE_EN = "FRITZ!Box_Calllist.csv";

    private static final String CSV_FILE_DE = "FRITZ!Box_Anrufliste.csv";

    private static final String PATTERN_SIPPROVIDER_OLD = "<!-- \"(\\d)\" / \"(\\w*)\" -->" + "\\s*<td class=\"c1\">\\s*<input type=checkbox id=\"uiViewActivsip\\d\"" + "\\s*onclick=\"uiOnChangeActivated\\('uiViewActivsip\\d','uiPostActivsip\\d'\\); return true;\">" + "\\s*</td>" + "\\s*<td class=\"c2\">([^<]*)</td>" + "\\s*<td class=\"c3\"><script type=\"text/javascript\">document.write\\(ProviderDisplay\\(\"([^\"]*)\"\\)\\);</script></td>";

    private static final String PATTERN_SIPPROVIDER_37_91 = "<!-- \"(\\d)\" / \"(\\w*)\" -->" + "\\s*<td class=\"c1\">\\s*<input type=checkbox id=\"uiViewActivsip\\d\"" + "\\s*onclick=\"uiOnChangeActivated\\('uiViewActivsip\\d','uiPostActivsip\\d'\\); return true;\">" + "\\s*</td>" + "\\s*<td class=\"c2\">([^<]*)</td>" + "\\s*<td class=\"c3\"><script type=\"text/javascript\">document.write\\(ProviderDisplay\\(\"([^\"]*)\"\\)\\);</script></td>" + "\\s*<td class=\"c6\"><script type=\"text/javascript\">document.write\\(AuswahlDisplay\\(\"([^\"]*)\"\\)\\);</script></td>";

    private static final String PATTERN_SIPPROVIDER_96 = "<!--\\s*\"(\\d[\\d]*)\"\\s*/\\s*\"(\\w*)\"\\s*/\\s*\"\\w*\"\\s*-->" + "\\s*<td class=\"c1\">\\s*<input type=checkbox id=\"uiViewActivsip\\d[\\d]*\"" + "\\s*onclick=\"uiOnChangeActivated\\('uiViewActivsip\\d[\\d]*','uiPostActivsip\\d[\\d]*'\\); return true;\">" + "\\s*</td>" + "\\s*<td class=\"c2\">([^<]*)</td>" + "\\s*<td class=\"c3\"><script type=\"text/javascript\">document.write\\(ProviderDisplay\\(\"([^\"]*)\"\\)\\);</script></td>" + "\\s*<td class=\"c6\"><script type=\"text/javascript\">document.write\\(AuswahlDisplay\\(\"([^\"]*)\"\\)\\);</script></td>";

    private static final String PATTERN_SIPPROVIDER_ACTIVE = "<input type=\"hidden\" name=\"sip:settings/sip(\\d[\\d]*)/activated\" value=\"(\\d[\\d]*)\" id=\"uiPostActivsip";

    private static final String PATTERN_QUICKDIAL = "<tr class=\"Dialoglist\">" + "\\s*<td style=\"text-align: center;\">(\\d*)</td>" + "\\s*<td>(\\w*)</td>" + "\\s*<td>([^<]*)</td>" + "\\s*<td style=\"text-align: right;\"><button [^>]*>\\s*<img [^>]*></button></td>" + "\\s*<td style=\"text-align: right;\"><button [^>]*>\\s*<img [^>]*></button></td>" + "\\s*</tr>";

    private static final String PATTERN_QUICKDIAL_NEW = "<td class=\"c5\"><span title=\"[^\"]*\">([^<]*)</span></td>" + "\\s*<td class=\"c3\"><span title=\"[^\"]*\">([^<]*)</span></td>" + "\\s*<td class=\"c1\">(\\d*)</td>" + "\\s*<td class=\"c2\"><span title=\"[^\"]*\">([^<]*)";

    private static final String PATTERN_QUICKDIAL_BETA = "<script type=\"text/javascript\">document.write\\(TrFon\\(\"[^\"]*\", \"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\"\\)\\);</script>";

    private FritzBoxFirmware firmware;

    private String box_address;

    private String box_password;

    private String box_port;

    private static int[] quickdial_indizes = { -1, -1, -1, -1 };

    private static final int VANITY = 0;

    private static final int NAME = 1;

    private static final int QUICKDIAL = 2;

    private static final int NUMBER = 3;

    public FritzBox(String address, String password, String port, Exception exc) {
        box_address = address;
        box_password = password;
        box_port = port;
        exc = null;
        try {
            detectFirmware();
        } catch (WrongPasswordException e) {
            exc = e;
            Debug.error(Main.getMessage("box.wrong_password"));
        } catch (InvalidFirmwareException e) {
            exc = e;
            Debug.error(Main.getMessage("unknown_firmware"));
        } catch (IOException e) {
            exc = e;
            Debug.error(Main.getMessage("box.not_found"));
        }
    }

    public void updateSettings() throws WrongPasswordException, InvalidFirmwareException, IOException {
        box_address = Main.getProperty("box.address");
        box_password = Encryption.decrypt(Main.getProperty("box.password"));
        box_port = Main.getProperty("box.port");
        detectFirmware();
    }

    /**
	 * Detects firmware version
	 * @return
	 */
    public void detectFirmware() throws WrongPasswordException, InvalidFirmwareException, IOException {
        if (Main.getProperty("network.type").equals("2") && Boolean.parseBoolean(Main.getProperty("option.clientCallList"))) {
            Debug.netMsg("JFritz is running as a client and using call list from server, canceling firmware detection");
        } else {
            firmware = FritzBoxFirmware.detectFirmwareVersion(box_address, box_password, box_port);
        }
    }

    /**
	 * Returns reference to firmware
	 * @return
	 */
    public FritzBoxFirmware getFirmware() {
        return firmware;
    }

    /**
	 * clears the caller list on the fritz box
	 * 
	 * @throws IOException
	 * @throws WrongPasswordException
	 */
    public void clearListOnFritzBox() throws WrongPasswordException, IOException {
        Debug.info("Clearing List");
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        String postdata = firmware.getAccessMethod() + POSTDATA_CLEAR.replaceAll("\\$LANG", firmware.getLanguage());
        JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
    }

    /**
	 * retrieves vector of SipProviders stored in the FritzBox
	 * 
	 * @return Vector of SipProvider
	 * @throws WrongPasswordException
	 * @throws IOException
	 *             author robotniko
	 * @throws InvalidFirmwareException
	 */
    public Vector<SipProvider> retrieveSipProvider() throws WrongPasswordException, IOException, InvalidFirmwareException {
        if (firmware == null) throw new InvalidFirmwareException("No valid firmware");
        String postdata = firmware.getAccessMethod() + POSTDATA_SIPPROVIDER.replaceAll("\\$LANG", firmware.getLanguage()) + URLEncoder.encode(box_password, "ISO-8859-1");
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        Debug.debug("Postdata: " + postdata);
        Debug.debug("Urlstr: " + urlstr);
        String data = JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
        if (false) {
            String filename = "./Firmware 96/sipProvider.html";
            Debug.debug("Debug mode: Loading " + filename);
            try {
                data = "";
                String thisLine;
                BufferedReader in = new BufferedReader(new FileReader(filename));
                while ((thisLine = in.readLine()) != null) {
                    data += thisLine;
                }
                in.close();
            } catch (IOException e) {
                Debug.error("File not found: " + filename);
            }
        }
        Vector<SipProvider> list = parseSipProvider(data, firmware);
        return list;
    }

    /**
	 * parses html data from the fritz!box's web interface and retrieves SIP
	 * information.
	 * 
	 * @param data
	 *            html data
	 * @return list of SipProvider objects author robotniko, akw
	 */
    public Vector<SipProvider> parseSipProvider(String data, FritzBoxFirmware firmware) {
        String sipProviderData = data;
        Vector<SipProvider> list = new Vector<SipProvider>();
        sipProviderData = JFritzUtils.removeDuplicateWhitespace(sipProviderData);
        Pattern p;
        if (firmware.getMajorFirmwareVersion() == 3 && firmware.getMinorFirmwareVersion() < 42) p = Pattern.compile(PATTERN_SIPPROVIDER_OLD); else if (firmware.getMajorFirmwareVersion() == 3 && firmware.getMinorFirmwareVersion() < 96) p = Pattern.compile(PATTERN_SIPPROVIDER_37_91); else p = Pattern.compile(PATTERN_SIPPROVIDER_96);
        Matcher m = p.matcher(sipProviderData);
        while (m.find()) {
            Debug.debug("FOUND SIP-PROVIDER");
            if (!(m.group(4).equals(""))) {
                if (firmware.getMajorFirmwareVersion() == 3 && firmware.getMinorFirmwareVersion() < 42) list.add(new SipProvider(Integer.parseInt(m.group(1)), m.group(3), m.group(4))); else list.add(new SipProvider(Integer.parseInt(m.group(5)), m.group(3), m.group(4)));
                Debug.debug("SIP-Provider: " + list.lastElement());
            }
        }
        p = Pattern.compile(PATTERN_SIPPROVIDER_ACTIVE);
        m = p.matcher(sipProviderData);
        while (m.find()) {
            Enumeration<SipProvider> en = list.elements();
            while (en.hasMoreElements()) {
                SipProvider sipProvider = en.nextElement();
                if (sipProvider.getProviderID() == Integer.parseInt(m.group(1))) {
                    if (Integer.parseInt(m.group(2)) == 1) {
                        sipProvider.setActive(true);
                    } else {
                        sipProvider.setActive(false);
                    }
                }
            }
        }
        return list;
    }

    /**
	 * This function fetches the call list from the box it first gets a
	 * connection to the fritz!Box opens the call list html page, then it sends
	 * a request to the box for the csv file then passes a bufferedReader to
	 * CallerList.importFromCSV() it passes back to the caller if there were new
	 * callers or not NOTE: Function no longer checks for invalid passwords!
	 * 
	 * 
	 * LAST MODIFIED: 14.04.06 Brian Jensen
	 * 
	 * @author Brian Jensen
	 * 
	 * @return if there were new calls or not
	 * @throws WrongPasswordException
	 * @throws IOException
	 */
    public boolean retrieveCSVList() throws IOException, MalformedURLException {
        synchronized (this) {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            boolean newEntries = false;
            Debug.info("Opening HTML Callerlist page");
            String postdata = firmware.getAccessMethod() + POSTDATA_LIST.replaceAll("\\$LANG", firmware.getLanguage()) + URLEncoder.encode(box_password, "ISO-8859-1");
            String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
            Debug.debug("Urlstr: " + urlstr);
            Debug.debug("Postdata: " + postdata);
            try {
                url = new URL(urlstr);
            } catch (MalformedURLException e) {
                Debug.error("URL invalid: " + urlstr);
                throw new MalformedURLException("URL invalid: " + urlstr);
            }
            if (url != null) {
                urlConn = url.openConnection();
                urlConn.setConnectTimeout(5000);
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                if (postdata != null) {
                    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    printout = new DataOutputStream(urlConn.getOutputStream());
                    printout.writeBytes(postdata);
                    printout.flush();
                    printout.close();
                }
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    while (reader.skip(100000) > 0) {
                    }
                    reader.close();
                    urlConn.getInputStream().close();
                } catch (IOException e1) {
                    throw new IOException("Network unavailable");
                }
            }
            Debug.info("Retrieving the CSV list from the box");
            urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
            try {
                url = new URL(urlstr);
            } catch (MalformedURLException e) {
                Debug.error("URL invalid: " + urlstr);
                throw new MalformedURLException("URL invalid: " + urlstr);
            }
            if (!JFritz.isShutdownInvoked() && url != null) {
                urlConn = url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                printout = new DataOutputStream(urlConn.getOutputStream());
                if (firmware.getLanguage().equals("de")) {
                    printout.writeBytes(POSTDATA_FETCH_CALLERLIST.replaceAll("\\$LANG", firmware.getLanguage()).replaceAll("\\$CSV_FILE", CSV_FILE_DE));
                } else if (firmware.getLanguage().equals("en")) {
                    printout.writeBytes(POSTDATA_FETCH_CALLERLIST.replaceAll("\\$LANG", firmware.getLanguage()).replaceAll("\\$CSV_FILE", CSV_FILE_EN));
                }
                printout.flush();
                printout.close();
                BufferedReader reader;
                try {
                    reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    Debug.info("Received response, begin processing call list");
                    newEntries = JFritz.getCallerList().importFromCSVFile(reader);
                    Debug.info("Finished processing response");
                    reader.close();
                    urlConn.getInputStream().close();
                } catch (IOException e1) {
                    throw new IOException("Network unavailable");
                }
            }
            return newEntries;
        }
    }

    /**
	 * 
	 * @return Vector of QuickDial objects
	 * @throws WrongPasswordException
	 * @throws IOException
	 * @throws InvalidFirmwareException
	 */
    public Vector<QuickDial> retrieveQuickDialsFromFritzBox(QuickDials model) throws WrongPasswordException, IOException, InvalidFirmwareException {
        String postdata;
        if (firmware.getMajorFirmwareVersion() == 4 && firmware.getMinorFirmwareVersion() >= 3) {
            postdata = firmware.getAccessMethod() + POSTDATA_QUICKDIAL_NEW.replaceAll("\\$LANG", firmware.getLanguage()) + URLEncoder.encode(box_password, "ISO-8859-1");
        } else {
            postdata = firmware.getAccessMethod() + POSTDATA_QUICKDIAL.replaceAll("\\$LANG", firmware.getLanguage()) + URLEncoder.encode(box_password, "ISO-8859-1");
        }
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        String data = JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
        return parseQuickDialData(model, data, firmware);
    }

    /**
	 * creates a list of QuickDial objects
	 * 
	 * @param data
	 * @param firmware
	 * @return list of QuickDial objects
	 */
    public Vector<QuickDial> parseQuickDialData(QuickDials model, String data, FritzBoxFirmware firmware) {
        String quickDialData = data;
        Vector<QuickDial> list = new Vector<QuickDial>();
        quickDialData = JFritzUtils.removeDuplicateWhitespace(quickDialData);
        Pattern p;
        if (firmware.getMajorFirmwareVersion() == 4 && firmware.getMinorFirmwareVersion() >= 3 && firmware.getMinorFirmwareVersion() < 5) {
            p = Pattern.compile(PATTERN_QUICKDIAL_NEW);
            quickdial_indizes[NAME] = 1;
            quickdial_indizes[QUICKDIAL] = 3;
            quickdial_indizes[VANITY] = 4;
            quickdial_indizes[NUMBER] = 2;
        } else if (firmware.getMajorFirmwareVersion() == 4 && firmware.getMinorFirmwareVersion() >= 5) {
            p = Pattern.compile(PATTERN_QUICKDIAL_BETA);
            quickdial_indizes[NAME] = 1;
            quickdial_indizes[QUICKDIAL] = 3;
            quickdial_indizes[VANITY] = 4;
            quickdial_indizes[NUMBER] = 2;
        } else {
            p = Pattern.compile(PATTERN_QUICKDIAL);
            quickdial_indizes[QUICKDIAL] = 1;
            quickdial_indizes[VANITY] = 2;
            quickdial_indizes[NUMBER] = 3;
        }
        Matcher m = p.matcher(quickDialData);
        while (m.find()) {
            String description = model.getDescriptionFromNumber(m.group(quickdial_indizes[NUMBER]));
            list.add(new QuickDial(m.group(quickdial_indizes[QUICKDIAL]), m.group(quickdial_indizes[VANITY]), m.group(quickdial_indizes[NUMBER]), description));
        }
        return list;
    }

    public boolean checkValidFirmware() throws WrongPasswordException, InvalidFirmwareException, IOException {
        firmware = FritzBoxFirmware.detectFirmwareVersion(box_address, box_password, box_port);
        return true;
    }

    /**
	 * Sends login data to FritzBox
	 */
    public void login() throws WrongPasswordException, IOException {
        String postdata = POSTDATA_LOGIN.replaceAll("\\$PASSWORT", URLEncoder.encode(box_password, "ISO-8859-1"));
        postdata = firmware.getAccessMethod() + postdata;
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
    }

    public void doCall(final String number, String port) throws WrongPasswordException, IOException {
        login();
        String currentNumber = number;
        currentNumber = currentNumber.replaceAll("\\+", "00");
        String portStr = "";
        if (port.equals("Fon 1")) {
            portStr = "1";
        } else if (port.equals("Fon 2")) {
            portStr = "2";
        } else if (port.equals("Fon 3")) {
            portStr = "3";
        } else if (port.equals("DECT 1")) {
            portStr = "60";
        } else if (port.equals("DECT 2")) {
            portStr = "61";
        } else if (port.equals("DECT 3")) {
            portStr = "62";
        } else if (port.equals("DECT 4")) {
            portStr = "63";
        } else if (port.equals("DECT 5")) {
            portStr = "64";
        } else if (port.equals("DECT 6")) {
            portStr = "65";
        } else if (port.equals(Main.getMessage("analog_telephones_all"))) {
            portStr = "9";
        } else if (port.equals("ISDN Alle")) {
            portStr = "50";
        } else if (port.equals("ISDN 1")) {
            portStr = "51";
        } else if (port.equals("ISDN 2")) {
            portStr = "52";
        } else if (port.equals("ISDN 3")) {
            portStr = "53";
        } else if (port.equals("ISDN 4")) {
            portStr = "54";
        } else if (port.equals("ISDN 5")) {
            portStr = "55";
        } else if (port.equals("ISDN 6")) {
            portStr = "56";
        } else if (port.equals("ISDN 7")) {
            portStr = "57";
        } else if (port.equals("ISDN 8")) {
            portStr = "58";
        } else if (port.equals("ISDN 9")) {
            portStr = "59";
        }
        String postdata = POSTDATA_CALL.replaceAll("\\$PASSWORT", URLEncoder.encode(box_password, "ISO-8859-1"));
        postdata = postdata.replaceAll("\\$NUMMER", currentNumber);
        postdata = postdata.replaceAll("\\$NEBENSTELLE", portStr);
        postdata = firmware.getAccessMethod() + postdata;
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
    }

    public void hangup() throws WrongPasswordException, IOException {
        login();
        String postdata = POSTDATA_HANGUP.replaceAll("\\$PASSWORT", URLEncoder.encode(box_password, "ISO-8859-1"));
        postdata = firmware.getAccessMethod() + postdata;
        String urlstr = "http://" + box_address + ":" + box_port + "/cgi-bin/webcm";
        JFritzUtils.fetchDataFromURL(urlstr, postdata, true);
    }

    /**
	 * Returns current box address
	 * @return
	 */
    public String getAddress() {
        return box_address;
    }

    /**
	 * Returns current box password
	 * @return
	 */
    public String getPassword() {
        return box_password;
    }

    /**
	 * Returns current box port
	 * @return
	 */
    public String getPort() {
        return box_port;
    }

    /**
	 * This function calls one of the upnp web services of the box and returns the raw data
	 * the data returned has the following format
	 * 
	 * <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body>
	 * <u:GetAddonInfosResponse xmlns:u="urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1">
	 * <NewByteSendRate>0</NewByteSendRate>
	 * <NewByteReceiveRate>0</NewByteReceiveRate>
	 * <NewPacketSendRate>0</NewPacketSendRate>
	 * <NewPacketReceiveRate>0</NewPacketReceiveRate>
	 * <NewTotalBytesSent>0</NewTotalBytesSent>
	 * <NewTotalBytesReceived>0</NewTotalBytesReceived>
	 * <NewAutoDisconnectTime>300</NewAutoDisconnectTime>
	 * <NewIdleDisconnectTime>7</NewIdleDisconnectTime>
	 * <NewDNSServer1>X.X.X.X</NewDNSServer1>
 	 * <NewDNSServer2>Y.Y.Y.Y</NewDNSServer2>
	 * <NewVoipDNSServer1>Z.Z.Z.Z</NewVoipDNSServer1>
	 * <NewVoipDNSServer2>0.0.0.0</NewVoipDNSServer2>
	 * <NewUpnpControlEnabled>0</NewUpnpControlEnabled>
	 * <NewRoutedBridgedModeBoth>0</NewRoutedBridgedModeBoth>
	 * </u:GetAddonInfosResponse>
	 * </s:Body> </s:Envelope>
	 * 
	 * @return the raw xml from the web service of the box
	 */
    public void getInternetStats(UPNPAddonInfosListener listener) {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body>" + "<u:GetAddonInfos xmlns:u=\"urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1\"></u:GetAddonInfos>\n" + "</s:Body>\n" + "</s:Envelope>";
        String result = UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_ADDONINFOS, URN_SERVICE_ADDONINFOS, xml);
        if (!result.equals("")) {
            try {
                XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                reader.setContentHandler(new AddonInfosXMLHandler(listener));
                reader.parse(new InputSource(new StringReader(result)));
            } catch (ParserConfigurationException e1) {
                Debug.error(e1.toString());
            } catch (SAXException e1) {
                Debug.error(e1.toString());
            } catch (IOException e1) {
                Debug.error(e1.toString());
            }
        }
    }

    /**
	 * This functions contains various web service calls that are not currently
	 * used but may be used in the future
	 *
	 */
    public void getWebservice() {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetDSLLinkInfo xmlns:u=\"urn:schemas-upnp-org:service:WANDSLLinkConfig:1\"></u:GetDSLLinkInfo>\n" + "</s:Body>\n" + "</s:Envelope>";
        UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_DSLLINK, URN_SERVICE_DSLLINK, xml);
    }

    public void getStatusInfo(UPNPStatusInfoListener listener) {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetDSLLinkInfo xmlns:u=\"urn:schemas-upnp-org:service:WANDSLLinkConfig:1\"></u:GetDSLLinkInfo>\n" + "</s:Body>\n" + "</s:Envelope>";
        String result = UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_STATUSINFO, URN_SERVICE_STATUSINFO, xml);
        Pattern p = Pattern.compile("<NewUptime>([^<]*)</NewUptime>");
        Matcher m = p.matcher(result);
        if (m.find()) listener.setUptime(m.group(1)); else listener.setUptime("-");
    }

    /**
	 * function gets the external IP address from the box using the upnp web services
	 * 
	 * @return
	 */
    public void getExternalIPAddress(UPNPExternalIpListener listener) {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetExternalIPAddress xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\"></u:GetExternalIPAddress>\n" + "</s:Body>\n" + "</s:Envelope>";
        String result = UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_EXTERNALIP, URN_SERVICE_EXTERNALIP, xml);
        Pattern p = Pattern.compile("<NewExternalIPAddress>([^<]*)</NewExternalIPAddress>");
        Matcher m = p.matcher(result);
        if (m.find()) listener.setExternalIp(m.group(1)); else listener.setExternalIp("-");
    }

    public void getCommonLinkInfo(UPNPCommonLinkPropertiesListener listener) {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetCommonLinkProperties xmlns:u=\"urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1\"></u:GetCommonLinkProperties>\n" + "</s:Body>\n" + "</s:Envelope>";
        String result = UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_COMMONLINK, URN_SERVICE_COMMONLINK, xml);
        Pattern p = Pattern.compile("<NewLayer1UpstreamMaxBitRate>([^<]*)</NewLayer1UpstreamMaxBitRate>");
        Matcher m = p.matcher(result);
        if (m.find()) listener.setUpstreamMaxBitRate(m.group(1)); else listener.setUpstreamMaxBitRate("-");
        p = Pattern.compile("<NewLayer1DownstreamMaxBitRate>([^<]*)</NewLayer1DownstreamMaxBitRate>");
        m = p.matcher(result);
        if (m.find()) listener.setDownstreamMaxBitRate(m.group(1)); else listener.setDownstreamMaxBitRate("-");
    }

    public void getInfo() {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetInfo xmlns:u=\"urn:schemas-any-com:service:Any:1\"></u:GetInfo>\n" + "</s:Body>\n" + "</s:Envelope>";
        UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_GETINFO, URN_SERVICE_GETINFO, xml);
    }

    public void getAutoConfig() {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetAutoConfig xmlns:u=\"urn:schemas-upnp-org:service:WANDSLLinkConfig:1\"></u:GetAutoConfig>\n" + "</s:Body>\n" + "</s:Envelope>";
        String result = UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_AUTOCONFIG, URN_SERVICE_AUTOCONFIG, xml);
        Debug.info("Result of getAutoConfig: " + result);
    }

    public void getConnectionTypeInfo() {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + ":encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetConnectionTypeInfo xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\"></u:GetConnectionTypeInfo>\n" + "</s:Body>\n" + "</s:Envelope>";
        UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_CONNECTIONTYPEINFO, URN_SERVICE_CONNECTIONTYPEINFO, xml);
    }

    public void getGenericPortMappingEntry() {
        String xml = "<?xml version=\"1.0\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "<s:Body><u:GetGenericPortMappingEntry xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">\n" + "<NewPortMappingIndex></NewPortMappingIndex>\n" + "</u:GetGenericPortMappingEntry>\n" + "</s:Body>\n" + "</s:Envelope>";
        UPNPUtils.getSOAPData("http://" + getAddress() + URL_SERVICE_GENERICPORTMAPPING, URN_SERVICE_GENERICPORTMAPPING, xml);
    }

    public String[] getAvailablePorts() {
        String[] ports = null;
        if (firmware == null) return null;
        switch(firmware.getBoxType()) {
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_FON:
            case FritzBoxFirmware.BOXTYPE_EUMEX_300:
                ports = new String[2];
                ports[0] = "Fon 1";
                ports[1] = "Fon 2";
                break;
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_FON_WLAN:
                ports = new String[3];
                ports[0] = "Fon 1";
                ports[1] = "Fon 2";
                ports[2] = Main.getMessage("analog_telephones_all");
                break;
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_ATA:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7140:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7141:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7113:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_SPEEDPORT_W501V:
                ports = new String[2];
                ports[0] = "Fon 1";
                ports[1] = "Fon 2";
                break;
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_5010:
                ports = new String[1];
                ports[0] = "Fon 1";
                break;
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_SPEEDPORT_W900V:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_5140:
                {
                    ports = new String[12];
                    ports[0] = "Fon 1";
                    ports[1] = "Fon 2";
                    ports[2] = "ISDN Alle";
                    ports[3] = "ISDN 1";
                    ports[4] = "ISDN 2";
                    ports[5] = "ISDN 3";
                    ports[6] = "ISDN 4";
                    ports[7] = "ISDN 5";
                    ports[8] = "ISDN 6";
                    ports[9] = "ISDN 7";
                    ports[10] = "ISDN 8";
                    ports[11] = "ISDN 9";
                    break;
                }
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_5050:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7050:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7170:
                {
                    ports = new String[13];
                    ports[0] = "Fon 1";
                    ports[1] = "Fon 2";
                    ports[2] = "Fon 3";
                    ports[3] = "ISDN Alle";
                    ports[4] = "ISDN 1";
                    ports[5] = "ISDN 2";
                    ports[6] = "ISDN 3";
                    ports[7] = "ISDN 4";
                    ports[8] = "ISDN 5";
                    ports[9] = "ISDN 6";
                    ports[10] = "ISDN 7";
                    ports[11] = "ISDN 8";
                    ports[12] = "ISDN 9";
                    break;
                }
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_5012:
                {
                    ports = new String[11];
                    ports[0] = "Fon 1";
                    ports[1] = "ISDN Alle";
                    ports[2] = "ISDN 1";
                    ports[3] = "ISDN 2";
                    ports[4] = "ISDN 3";
                    ports[5] = "ISDN 4";
                    ports[6] = "ISDN 5";
                    ports[7] = "ISDN 6";
                    ports[8] = "ISDN 7";
                    ports[9] = "ISDN 8";
                    ports[10] = "ISDN 9";
                    break;
                }
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7570:
            case FritzBoxFirmware.BOXTYPE_FRITZBOX_7270:
                {
                    ports = new String[18];
                    ports[0] = "Fon 1";
                    ports[1] = "Fon 2";
                    ports[2] = "ISDN Alle";
                    ports[3] = "ISDN 1";
                    ports[4] = "ISDN 2";
                    ports[5] = "ISDN 3";
                    ports[6] = "ISDN 4";
                    ports[7] = "ISDN 5";
                    ports[8] = "ISDN 6";
                    ports[9] = "ISDN 7";
                    ports[10] = "ISDN 8";
                    ports[11] = "ISDN 9";
                    ports[12] = "DECT 1";
                    ports[13] = "DECT 2";
                    ports[14] = "DECT 3";
                    ports[15] = "DECT 4";
                    ports[16] = "DECT 5";
                    ports[17] = "DECT 6";
                    break;
                }
        }
        return ports;
    }

    public void setAddress(String box_address) {
        this.box_address = box_address;
    }

    public void setPassword(String box_password) {
        this.box_password = box_password;
    }

    public void setPort(String box_port) {
        this.box_port = box_port;
    }
}
