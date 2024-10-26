package me.foq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Nikeplus {

    protected static String authURL = "https://secure-nikeplus.nike.com/nikeplus/v1/services/widget/generate_pin.jhtml?login=<0>&password=<1>";

    protected static String runListURL = "https://secure-nikeplus.nike.com/nikeplus/v1/services/app/run_list.jhtml";

    protected String myCookie = null;

    /**
	 * Download the content of a URL.
	 *
	 * Nothing too fancy here. It just puts the cookie on the
	 * request before reading the content of the URL. It also
	 * takes the cookie off the request when I'm done, in case
	 * I am just logging in.
	 *
	 * (note to self - not sure if this is the shortest way
	 *  of doing this ...)
	 *
	 * @param url
	 * @return
	 */
    protected String downloadContent(String url) {
        StringBuffer result = null;
        try {
            URL myUrl = new URL(url);
            URLConnection urlConn = myUrl.openConnection();
            if (myCookie != null) {
                urlConn.setRequestProperty("Cookie", myCookie);
            }
            urlConn.connect();
            Map<String, List<String>> m = urlConn.getHeaderFields();
            if (m.containsKey("Set-Cookie")) {
                List<String> l = m.get("Set-Cookie");
                if (l != null) {
                    Iterator<String> iterator = l.iterator();
                    myCookie = "";
                    while (iterator.hasNext()) {
                        String element = (String) iterator.next();
                        if (myCookie.length() != 0) {
                            myCookie = myCookie + ";";
                        }
                        myCookie = myCookie + element;
                    }
                }
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String s;
            while ((s = in.readLine()) != null) {
                if (result == null) {
                    result = new StringBuffer(s);
                } else {
                    result.append(s);
                }
            }
            in.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }

    /**
	 * Checks if I currently have a valid Nike+ session or not.
	 * @return
	 */
    public boolean isConnected() {
        return (myCookie != null && (myCookie.indexOf("id.nike.com") >= 0));
    }

    /**
	 * Login with your Nike+ username and password
	 *
	 * @param user
	 * @param passwd
	 * @return the pin from the Nike+ website
	 */
    public String login(String user, String passwd) {
        String pin = "";
        String temp;
        String authURI = authURL.replaceAll("<0>", user);
        authURI = authURI.replaceAll("<1>", passwd);
        temp = downloadContent(authURI);
        OneClassXML xml = OneClassXML.parseXML(temp);
        OneClassXML status = xml.findFirstSubElement("plusService, status");
        if (status != null) {
            if ("success".equals(status.getValue())) {
                OneClassXML pinXML = xml.findFirstSubElement("plusService, pin");
                pin = pinXML.getValue();
            }
        }
        return pin;
    }

    /**
	 * Gets "runs" from the Nike+ website.
	 * These will be the run of however is logged in a the moment, so you have to
	 * call the above login() method with a propper username and password first
	 *
	 * A "Run" is a piece of XML that looks something like this
	 *
	 * <run id = "561318341" workoutType="standard">
	 * 	<startTime>2006-12-16T14:34:36+00:00</startTime>
	 * 	<distance>12.0457</distance>
	 * 	<duration>3367563</duration>
	 * 	<syncTime>2006-12-16T16:15:35+00:00</syncTime>
	 *  <calories>0</calories>
	 *  <name></name>
	 *  <description></description>
	 * </run>
	 * 	 *
	 * @return
	 */
    public OneClassXML getRuns() {
        OneClassXML runs = null;
        String runXMLStr = downloadContent(runListURL);
        OneClassXML runsXML = OneClassXML.parseXML(runXMLStr);
        OneClassXML status = runsXML.findFirstSubElement("plusService, status");
        if (status != null) {
            if ("success".equals(status.getValue())) {
                runs = runsXML.findFirstSubElement("plusService, runList");
            }
        }
        return runs;
    }

    /**
	 * main() - just run a "self test" with a couple of basic examples
	 * @param args
	 */
    public static void main(String[] args) {
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String id = null;
        String password = null;
        try {
            System.out.println("Enter Nike+ ID (i.e. the email address):");
            id = userInput.readLine();
            System.out.println("Enter password:");
            password = userInput.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Nikeplus np = new Nikeplus();
        np.login(id, password);
        System.out.println("isConnected(): " + np.isConnected());
        System.out.println("fetching runs - this may take some time ...");
        OneClassXML runs = np.getRuns();
        if (runs != null) {
            Collection<OneClassXML> allRuns = runs.findAllSubElements("runList, run");
            Double distance = new Double(0.0);
            if (allRuns != null) {
                Iterator<OneClassXML> iterator = allRuns.iterator();
                while (iterator.hasNext()) {
                    OneClassXML singleRun = (OneClassXML) iterator.next();
                    String s = singleRun.findFirstSubElement("run, distance").getValue();
                    distance += Double.parseDouble(s);
                }
                System.out.println("distance covered: " + distance);
                System.out.println("in " + allRuns.size() + " runs.");
            }
        }
    }
}
