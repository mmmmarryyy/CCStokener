package org.jazzteam.edu.tests.seleniumServer.src.rootSeleniumServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.openqa.selenium.server.SeleniumServer;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

/**
 * @author Hor1zonT
 * 
 */
public abstract class RootSeleniumServer {

    protected static Selenium selenium;

    private SeleniumServer sel;

    private String browser;

    private String siteAddress;

    private boolean runHTTPCommand(final String theCommand) throws IOException {
        URL url = new URL(theCommand);
        URLConnection seleniumConnection = url.openConnection();
        seleniumConnection.connect();
        InputStream inputStream = seleniumConnection.getInputStream();
        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int streamLength;
        while ((streamLength = inputStream.read(buffer)) != -1) {
            outputSteam.write(buffer, 0, streamLength);
        }
        inputStream.close();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String stringifiedOutput = outputSteam.toString();
        if (stringifiedOutput.startsWith("OK")) return true;
        return false;
    }

    /**
	 * This method you should call first in test Start server and load in
	 * browser
	 */
    public void startSelenium() {
        String stopSeleniumCommand = "http://localhost:4444/selenium-server/driver/?cmd=shutDownSeleniumServer";
        try {
            sel = new SeleniumServer();
            if (browser == null) {
                browser = "*iehta";
            }
            if (siteAddress == null) {
                siteAddress = "http://pcpdemo.textfuser.net/";
            }
            selenium = new DefaultSelenium("localhost", 4444, browser, siteAddress);
            sel.boot();
        } catch (java.net.BindException bE) {
            System.out.println("could not bind - carrying on");
            try {
                if (runHTTPCommand(stopSeleniumCommand)) {
                    try {
                        sel = new SeleniumServer();
                        sel.boot();
                    } catch (Exception e) {
                        throw new IllegalStateException("Could not stop existing server on blocked port 4444", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Can't start selenium server", e);
        }
        selenium.start();
    }

    /**
	 * This method you should call last in class Close browser and stop server
	 */
    public void stopSelenium() {
        this.selenium.close();
        this.selenium.stop();
        this.sel.stop();
    }

    /**
	 * @param browser
	 *            the browser to set
	 */
    public void setBrowser(String browser) {
        this.browser = browser;
    }

    /**
	 * @param siteAddress
	 *            the siteAddress to set
	 */
    public void setSiteAddress(String siteAddress) {
        this.siteAddress = siteAddress;
    }
}
