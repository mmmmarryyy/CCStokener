package com.mutchek.vonaje;

import java.io.BufferedReader;
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
import java.util.Properties;
import org.xml.sax.SAXException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;

/**
 * Representation of a customer account on vonage.com
 * @author jmutchek
 */
public class VonageAccount {

    public static String VONAGE_REST = "https://secure.click2callu.com/tpcc";

    public static String VONAGE_REST_GET_NUMBERS = VONAGE_REST + "/getnumbers";

    public static String VONAGE_REST_MAKE_CALL = VONAGE_REST + "/makecall";

    /**
	 * These variables hold the URLs of the Vonage service as specified in a country .properties file 
	 */
    private String vonage_web = "";

    private String vonage_web_action = "";

    /**
	 * Country properties
	 */
    private Properties country;

    /**
	 * Vonage account username
	 */
    private String username;

    /**
	 * Vonage account password
	 */
    private String password;

    /**
	 * Vonage account number
	 */
    private String accountNumber;

    /**
	 * Collection of phone numbers associated with this account
	 */
    private ArrayList numbers;

    /**
	 * Persistent web session at secure.vonage.com
	 */
    private WebConversation wwwVonageSession = null;

    public VonageAccount(String username, String password) {
        this(username, password, System.getProperty("vonaje.country"));
    }

    public VonageAccount(String username, String password, String countryCode) {
        this.username = username;
        this.password = password;
        if (countryCode == null) {
            countryCode = "us";
        }
        country = new Properties();
        try {
            country.load(this.getClass().getResourceAsStream("country/" + countryCode + ".properties"));
            vonage_web = country.getProperty("vonage_web");
            vonage_web_action = country.getProperty("vonage_web_action");
        } catch (IOException e) {
            vonage_web = "https://secure.vonage.com";
            vonage_web_action = "/vonage-web/";
        }
    }

    /**
	 * Retrieve the persistent web session to secure.vonage.com
	 * and manage logging in and inactivity timeouts as necessary
	 * @return the persistent web session to secure.vonage.com
	 * @throws VonageConnectivityException when there is an error connecting to secure.vonage.com
	 */
    protected WebConversation getVonageSession() throws VonageConnectivityException {
        if (wwwVonageSession == null) {
            wwwVonageSession = new WebConversation();
        }
        try {
            WebResponse loginPage = null;
            loginPage = wwwVonageSession.getResponse(getVonageWebDashboard());
            WebForm loginForm = loginPage.getFormWithID("logonForm");
            if (loginForm != null) {
                loginForm.setParameter("username", username);
                loginForm.setParameter("password", password);
                WebResponse targetPage = loginForm.submit();
                loginForm = targetPage.getFormWithID("logonForm");
                if (loginForm != null) {
                    throw new VonageConnectivityException("Login attempt denied.");
                }
            }
        } catch (MalformedURLException e) {
            throw new VonageConnectivityException(e.getMessage());
        } catch (IOException e) {
            throw new VonageConnectivityException("IO error.");
        } catch (SAXException e) {
            throw new VonageConnectivityException("Unexpected response.");
        }
        return wwwVonageSession;
    }

    /**
	 * Retrieve a collection of phone numbers associated with this Vonage account
	 * @return collection of VonagePhoneNumber objects
	 */
    public ArrayList getPhoneNumbers() {
        if (numbers == null) {
            String results = "";
            try {
                URL url = new URL(VonageAccount.VONAGE_REST_GET_NUMBERS);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                Writer writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write("username=" + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8"));
                writer.close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                results = reader.readLine();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] allNumbers = results.split(",");
            numbers = new ArrayList(allNumbers.length);
            for (int i = 0; i < allNumbers.length; i++) {
                try {
                    numbers.add(new VonagePhoneNumber(this, allNumbers[i]));
                } catch (InvalidPhoneNumberException e) {
                }
            }
        }
        return numbers;
    }

    /**
	 * Retrieve the Vonage account number
	 * @return the account number
	 */
    public String getAccountNumber() {
        return this.accountNumber;
    }

    /**
	 * Retrieve the Vonage username
	 * @return the username
	 */
    public String getUsername() {
        return this.username;
    }

    /**
	 * Retrieve the Vonage password
	 * @return the password
	 */
    public String getPassword() {
        return this.password;
    }

    /**
	 * Retrieve the URL of the Vonage web site
	 * @return the Vonage URL
	 */
    public String getVonageWeb() {
        return vonage_web;
    }

    /**
	 * Retrieve the URL of the Vonage voicemail page
	 * @return the voicemail URL
	 */
    public String getVonageWebVoicemail() {
        return vonage_web + vonage_web_action + "features/voicemail/messages/view.htm";
    }

    /**
	 * Retrieve the URL of the Vonage dashboard page
	 * @return the dashboard URL
	 */
    public String getVonageWebDashboard() {
        return vonage_web + vonage_web_action + "dashboard/index.htm";
    }

    /**
	 * Retrieve the URL of the Vonage billing page
	 * @return the billing URL
	 */
    public String getVonageWebBilling() {
        return vonage_web + vonage_web_action + "billing/index.htm";
    }

    /**
	 * Retrieve the URL of the Vonage activity page
	 * @return the activity URL
	 */
    public String getVonageWebActivity() {
        return vonage_web + vonage_web_action + "activity/index.htm";
    }

    public Properties getCountryProperties() {
        return country;
    }
}
