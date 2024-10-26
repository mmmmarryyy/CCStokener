package com.nogoodatcoding.cip.idle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.ParserException;

/**
 *
 * Represents a Cisco IP Phone
 *
 * @author no.good.at.coding
 */
public class CIPPhone {

    private static Logger log_ = Logger.getLogger(CIPPhone.class);

    private static ResourceBundle messages_ = ResourceBundle.getBundle("com.nogoodatcoding.cip.idle.messages.Messages_CIPPhone");

    private String ipAddress = null;

    private String macAddress = null;

    private String hostName = null;

    private String phoneDN = null;

    private String modelNumber = null;

    private String xCiscoIPPhoneModelName = null;

    private String xCiscoIPPhoneDisplay = null;

    private String xCiscoIPPhoneSDKVersion = null;

    private long lastUpdatedTime = 0;

    private int width = 133;

    private int height = 65;

    private int colorDepth = 2;

    private boolean isColor = false;

    /**
     *
     * Creates a new {@code CIPPhone} object for the given IP address, after
     * verification that it's actually an Cisco IP Phone
     *
     * @param ipAddressOfRequest The IP address from where the request
     *                           originated
     *
     * @param xCiscoIPPhoneModelName The x-CiscoIPPhoneModelName Cisco phone
     *                               header
     *
     * @param xCiscoIPPhoneDisplay The x-CiscoIPPhoneDisplay Cisco phone
     *                             header
     *
     * @param xCiscoIPPhoneSDKVersion The x-CiscoIPPhoneSDKVersion Cisco phone
     *                                header
     *
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     * @throws org.htmlparser.util.ParserException
     */
    public CIPPhone(String ipAddressOfRequest, String xCiscoIPPhoneModelName, String xCiscoIPPhoneDisplay, String xCiscoIPPhoneSDKVersion) throws MalformedURLException, IOException, ParserException {
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.newIPAddress") + ipAddressOfRequest);
        if (xCiscoIPPhoneDisplay == null || xCiscoIPPhoneModelName == null || xCiscoIPPhoneSDKVersion == null) {
            CIPPhone.log_.error(CIPPhone.messages_.getString("cipPhone.log.error.notCIP.nullHeaders"));
            throw new ParserException(CIPPhone.messages_.getString("cipPhone.log.error.notCIP.nullHeaders"));
        } else {
            CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.foundHeaders"));
        }
        this.xCiscoIPPhoneDisplay = xCiscoIPPhoneDisplay;
        this.xCiscoIPPhoneModelName = xCiscoIPPhoneModelName;
        this.xCiscoIPPhoneSDKVersion = xCiscoIPPhoneSDKVersion;
        if (xCiscoIPPhoneDisplay != null) {
            try {
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.parsingXCiscoIPPhoneDisplayHeader") + xCiscoIPPhoneDisplay);
                String[] displayValues = xCiscoIPPhoneDisplay.split("\\s*,\\s*");
                width = Integer.parseInt(displayValues[0]);
                height = Integer.parseInt(displayValues[1]);
                colorDepth = Integer.parseInt(displayValues[2]);
                isColor = displayValues[3].equalsIgnoreCase("C") ? true : false;
            } catch (Exception e) {
                CIPPhone.log_.error(CIPPhone.messages_.getString("cipPhone.log.error.parsingXCiscoIPPhoneDisplayHeader"), e);
            }
        }
        this.ipAddress = ipAddressOfRequest;
        scrape(ipAddressOfRequest);
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    /**
     *
     * @return The hostname for this Cisco IP Phone
     */
    public String getHostName() {
        return hostName;
    }

    /**
     *
     * @return The IP address for this Cisco IP Phone
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     *
     * @return The MAC address for this Cisco IP Phone
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     *
     * @return The phone number for this Cisco IP Phone
     */
    public String getPhoneDN() {
        return phoneDN;
    }

    /**
     *
     * @return The last updated time for this Cisco IP Phone
     */
    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    /**
     *
     * @return The model number for this Cisco IP Phone
     */
    public String getModelNumber() {
        return modelNumber;
    }

    /**
     *
     * @return The x-CiscoIPPhoneDisplay header for this Cisco IP Phone
     */
    public String getXCiscoIPPhoneDisplay() {
        return xCiscoIPPhoneDisplay;
    }

    /**
     *
     * @return The x-CiscoIPPhoneModelName header for this Cisco IP Phone
     */
    public String getXCiscoIPPhoneModelName() {
        return xCiscoIPPhoneModelName;
    }

    /**
     *
     * @return The x-CiscoIPPhoneSDKVersion header for this Cisco IP Phone
     */
    public String getXCiscoIPPhoneSDKVersion() {
        return xCiscoIPPhoneSDKVersion;
    }

    /**
     *
     * @return The color depth (in bits) for this Cisco IP Phone
     */
    public int getColorDepth() {
        return colorDepth;
    }

    /**
     *
     * @return The height (in pixels) of the display area accessible to services
     *         for this Cisco IP Phone
     */
    public int getHeight() {
        return height;
    }

    /**
     *
     * @return The width (in pixels) of the display area accessible to services
     *         for this Cisco IP Phone
     */
    public int getWidth() {
        return width;
    }

    /**
     * 
     * @return A {@code boolean} indicating if this phones display is color or
     *         grayscale
     */
    public boolean isColor() {
        return isColor;
    }

    /**
     *
     * Forces an update of the phone details, in case the IP address has changed
     *
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     * @throws org.htmlparser.util.ParserException
     */
    public void update() throws MalformedURLException, IOException, ParserException {
        CIPPhone.log_.info(CIPPhone.messages_.getString("cipPhone.log.info.update"));
        scrape(ipAddress);
    }

    /**
     *
     * Check the phone by getting the page it hosts and verifying that it is
     * a page hosted by a Cisco IP Phone
     *
     * @param ipAddress The IP address to check
     *
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     * @throws org.htmlparser.util.ParserException
     */
    private void scrape(String ipAddress) throws MalformedURLException, IOException, ParserException {
        URL url = new URL("http://" + ipAddress + "/DeviceInformation");
        CIPPhone.log_.info(CIPPhone.messages_.getString("cipPhone.log.info.scraping") + url);
        Lexer lexer = new Lexer();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        lexer.setPage(new Page(conn));
        Node currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        while (!currentNode.getText().toUpperCase().startsWith("TITLE")) {
            currentNode = lexer.nextNode();
            CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        }
        currentNode = lexer.nextNode();
        String titleText = currentNode.getText();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.titleValue") + titleText);
        if (!titleText.startsWith("Cisco Systems, Inc.")) {
            CIPPhone.log_.error(CIPPhone.messages_.getString("cipPhone.log.error.notCIP.invalidTitle"));
            throw new ParserException(CIPPhone.messages_.getString("cipPhone.log.error.notCIP.invalidTitle"));
        }
        int valuesFound = 0;
        String toMatch = null;
        while (valuesFound < 3) {
            currentNode = lexer.nextNode();
            CIPPhone.log_.debug("Current node: " + currentNode.getText());
            switch(valuesFound) {
                case 0:
                    toMatch = "MAC Address";
                    CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.toMatch") + toMatch);
                    break;
                case 1:
                    toMatch = "Host Name";
                    CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.toMatch") + toMatch);
                    break;
                case 2:
                    toMatch = "Phone DN";
                    CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.toMatch") + toMatch);
                    break;
            }
            if (currentNode.getText().equalsIgnoreCase(toMatch)) {
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                currentNode = lexer.nextNode();
                CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
                if (valuesFound == 0) {
                    this.macAddress = currentNode.getText();
                } else if (valuesFound == 1) {
                    this.hostName = currentNode.getText();
                } else if (valuesFound == 2) {
                    this.phoneDN = currentNode.getText();
                }
                valuesFound++;
            }
        }
        toMatch = "Model Number";
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.toMatch") + toMatch);
        while (!currentNode.getText().equalsIgnoreCase(toMatch)) {
            currentNode = lexer.nextNode();
        }
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + toMatch);
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        currentNode = lexer.nextNode();
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        CIPPhone.log_.debug(CIPPhone.messages_.getString("cipPhone.log.debug.htmlNode") + currentNode.getText());
        this.modelNumber = currentNode.getText();
    }
}
