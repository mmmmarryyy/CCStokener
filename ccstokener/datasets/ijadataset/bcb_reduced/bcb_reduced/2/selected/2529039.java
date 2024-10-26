package com.mutchek.vonaje;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

/**
 * Representation of a single phone number within a Vonage
 * customer account.  All configuration and voice functionality
 * is enabled through the VonagePhoneNumber under a
 * VonageAccount
 * @author jmutchek
 */
public class VonagePhoneNumber {

    /**
	 * Filesystem location of the local voicemail message cache
	 */
    private String messageCache = null;

    /**
	 * The phone number
	 */
    private PhoneNumber number = null;

    /**
	 * The Vonage account with which this number is associated
	 */
    private VonageAccount account;

    /**
	 * Voicemail messages attached to this account
	 */
    private ArrayList messages;

    private int inPlanMinutesUsed = 0;

    private int inPlanMinutesRemaining = 0;

    private int regionalMinutesUsed = 0;

    private int freeInNetworkMinutesUsed = 0;

    private int freeInAccountMinutesUsed = 0;

    private int freeMinutesUsed = 0;

    private boolean doneBilling = false;

    private boolean doneActivity = false;

    public VonagePhoneNumber(VonageAccount account, String number) throws InvalidPhoneNumberException {
        this.number = new PhoneNumber(number);
        this.account = account;
    }

    /**
	 * Set the local filesystem location at which voicemail mav files should be cached
	 * @param dir complete filesystem path
	 */
    public void setMessageCache(String dir) {
        messageCache = dir;
        if (!messageCache.endsWith(File.separator)) {
            messageCache = messageCache + File.separator;
        }
    }

    /**
	 * Retrieve the complete path the voicemail file cache
	 * @return
	 */
    public String getMessageCache() {
        return messageCache;
    }

    /**
	 * Retrieve the Vonage account with which this phone number is associated
	 * @return
	 */
    public VonageAccount getVonageAccount() {
        return account;
    }

    /**
	 * Initiate a phone call between this number and the specified other number
	 * @param otherPhoneNumber
	 * @return true if the request to place the call was successful
	 */
    public void initiateCall(PhoneNumber otherPhoneNumber) throws Click2CallException {
        String results = "";
        try {
            URL url = new URL(VonageAccount.VONAGE_REST_MAKE_CALL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            Writer writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write("username=" + URLEncoder.encode(account.getUsername(), "UTF-8") + "&password=" + URLEncoder.encode(account.getPassword(), "UTF-8") + "&fromnumber=" + URLEncoder.encode(number.getNumber(), "UTF-8") + "&tonumber=" + URLEncoder.encode(otherPhoneNumber.getNumber(), "UTF-8"));
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            results = reader.readLine();
        } catch (MalformedURLException e) {
            throw new Click2CallException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new Click2CallException(e.getMessage());
        } catch (IOException e) {
            throw new Click2CallException(e.getMessage());
        }
        if (!results.substring(0, 3).equals("000")) {
            throw new Click2CallException(results);
        }
    }

    /**
	 * Initiate  a phone call between this number and the specified other number
	 * The other number must be a 11-digit phone number with no punctuation (12035551212)
	 * @param otherPhoneNumber
	 * @throws Click2CallException
	 */
    public void initiateCall(String otherPhoneNumber) throws Click2CallException {
        try {
            PhoneNumber opn = new PhoneNumber(otherPhoneNumber);
            initiateCall(opn);
        } catch (Exception e) {
            throw new Click2CallException(e.getMessage());
        }
    }

    /**
	 * Retrieve the raw 11-digit phone number
	 * @return the phone number
	 */
    public String getNumber() {
        return number.getNumber();
    }

    /**
	 * Exactly the same as getNumber()
	 */
    public String toString() {
        return number.getNumber();
    }

    /**
	 * Check to see if the message list has been downloaded
	 * @return
	 */
    public boolean hasMessageList() {
        if (messages == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * Retrieve a list of voicemail messages left for the phone number
	 * @return
	 * @throws VonageConnectivityException
	 */
    public ArrayList getMessageList() throws VonageConnectivityException {
        if (messages == null) {
            WebConversation wwwVonageSession = account.getVonageSession();
            messages = new ArrayList();
            try {
                WebResponse voicemailPage = wwwVonageSession.getResponse(account.getVonageWebVoicemail());
                WebTable voicemailTable = voicemailPage.getTableStartingWith("New");
                for (int i = 1; i < voicemailTable.getRowCount(); i++) {
                    Message tmp = new Message();
                    tmp.setVonagePhoneNumber(this);
                    WebLink newLinks[] = voicemailTable.getTableCell(i, 0).getLinks();
                    if (newLinks.length > 0) {
                        tmp.markNew(true);
                    } else {
                        tmp.markNew(false);
                    }
                    try {
                        tmp.setCallerNumber(voicemailTable.getCellAsText(i, 1));
                    } catch (InvalidPhoneNumberException e) {
                        System.err.println(e.getMessage());
                    }
                    String urlContainsListID = voicemailTable.getTableCell(i, 4).getLinkWith("Listen").getURLString();
                    Pattern pat = Pattern.compile("msgID=(\\d+)");
                    Matcher mat = pat.matcher(urlContainsListID);
                    mat.find();
                    tmp.setListID(Integer.parseInt(mat.group(1)));
                    tmp.setCallTime(voicemailTable.getCellAsText(i, 2));
                    tmp.setDuration(voicemailTable.getCellAsText(i, 3));
                    tmp.setURL(account.getVonageWeb() + voicemailTable.getTableCell(i, 4).getLinkWith("Listen").getURLString());
                    tmp.applyCallerID();
                    messages.add(tmp);
                }
            } catch (SAXException e1) {
                throw new VonageConnectivityException();
            } catch (IOException e) {
                throw new VonageConnectivityException();
            }
        }
        return messages;
    }

    public boolean deleteMessageIndex(int index) throws VonageConnectivityException {
        int id = ((Message) messages.get(index)).getListID();
        return deleteMessage(id);
    }

    public boolean deleteMessage(int id) throws VonageConnectivityException {
        boolean rv = false;
        String deleteURL = null;
        Message toRemove = null;
        if ((id < 1) || (id > messages.size())) {
            rv = false;
        } else {
            Iterator it = messages.iterator();
            while (it.hasNext()) {
                Message tmp = (Message) it.next();
                if (tmp.getListID() == id) {
                    deleteURL = account.getVonageWebVoicemail() + "?did=" + number.getNumber() + "&msgIDList=" + id + "&folder=Inbox" + "&action=handleDeleteMessages";
                    toRemove = tmp;
                } else if (tmp.getListID() > id) {
                    tmp.setListID(tmp.getListID() - 1);
                }
            }
            if (deleteURL != null) {
                toRemove.removeFromCache();
                messages.remove(toRemove);
                messages.trimToSize();
                WebConversation wwwVonageSession = account.getVonageSession();
                try {
                    WebResponse resp = wwwVonageSession.getResponse(deleteURL);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
            rv = true;
        }
        return rv;
    }

    /**
	 * Scrape all of the relevant data from the billing screen on vonage.com
	 * @throws VonageConnectivityException
	 */
    private void retrieveBillingScreenInfo() throws VonageConnectivityException {
        if (!doneBilling) {
            doneBilling = true;
            WebConversation wwwVonageSession = account.getVonageSession();
            try {
                WebResponse voicemailPage = wwwVonageSession.getResponse(account.getVonageWebBilling());
                WebTable inPlanTable = voicemailPage.getTableStartingWith("In-Plan Minutes used:");
                String intValue = "";
                intValue = inPlanTable.getCellAsText(0, 1);
                if (intValue.matches("^[0-9]+$")) {
                    inPlanMinutesUsed = Integer.parseInt(intValue);
                }
                intValue = inPlanTable.getCellAsText(1, 1);
                if (intValue.matches("^[0-9]+$")) {
                    inPlanMinutesRemaining = Integer.parseInt(intValue);
                }
                intValue = inPlanTable.getCellAsText(2, 1);
                if (intValue.matches("^[0-9]+$")) {
                    regionalMinutesUsed = Integer.parseInt(intValue);
                }
                intValue = inPlanTable.getCellAsText(3, 1);
                if (intValue.matches("^[0-9]+$")) {
                    freeInNetworkMinutesUsed = Integer.parseInt(intValue);
                }
                intValue = inPlanTable.getCellAsText(4, 1);
                if (intValue.matches("^[0-9]+$")) {
                    freeInAccountMinutesUsed = Integer.parseInt(intValue);
                }
                intValue = inPlanTable.getCellAsText(5, 1);
                if (intValue.matches("^[0-9]+$")) {
                    freeMinutesUsed = Integer.parseInt(intValue);
                }
            } catch (SAXException e1) {
                throw new VonageConnectivityException();
            } catch (IOException e) {
                throw new VonageConnectivityException();
            }
        }
    }

    /**
	 * NOT IMPLEMENTED
	 * @throws VonageConnectivityException
	 */
    private void retrieveActivityScreenInfo() throws VonageConnectivityException {
        throw new VonageConnectivityException("NOT IMPLEMENTED");
    }

    public int getInPlanMinutesUsed() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return inPlanMinutesUsed;
    }

    public int getInPlanMinutesRemaining() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return inPlanMinutesRemaining;
    }

    public int getRegionalMinutesUsed() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return regionalMinutesUsed;
    }

    public int getFreeInNetworkMinutesUsed() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return freeInNetworkMinutesUsed;
    }

    public int getFreeInAccountMinutesUsed() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return freeInAccountMinutesUsed;
    }

    public int getFreeMinutesUsed() throws VonageConnectivityException {
        retrieveBillingScreenInfo();
        return freeMinutesUsed;
    }
}
