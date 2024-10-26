package jmathlib.toolbox.jmathlib.system;

import jmathlib.core.tokens.*;
import jmathlib.core.functions.ExternalFunction;
import jmathlib.core.interpreter.GlobalValues;
import jmathlib.core.interpreter.ErrorLogger;
import java.net.*;
import java.util.*;

/**An external function for checking for updates over the network*/
public class checkforupdates extends ExternalFunction {

    GlobalValues globals = null;

    public OperandToken evaluate(Token[] operands, GlobalValues _globals) {
        globals = _globals;
        String s = "";
        String lineFile = "";
        boolean silentB = false;
        String updateSiteS = "http://www.jmathlib.de/updates/";
        s = globals.getProperty("update.site.primary");
        if (s == null) s = globals.getProperty("update.site.secondary");
        if (s != null) updateSiteS = s;
        if (getNArgIn(operands) == 1) {
            if ((operands[0] instanceof CharToken)) {
                String st = ((CharToken) operands[0]).toString();
                if (st.equals("-silent")) {
                    silentB = true;
                } else {
                    updateSiteS = st;
                    globals.getInterpreter().displayText("New Update Site " + updateSiteS);
                }
            }
        }
        if (!silentB) globals.getInterpreter().displayText("Checking for Updates at " + updateSiteS);
        String[] lastUpdateS = globals.getProperty("update.date.last").split("/");
        int year = Integer.parseInt(lastUpdateS[0]);
        int month = Integer.parseInt(lastUpdateS[1]) - 1;
        int day = Integer.parseInt(lastUpdateS[2]);
        int intervall = Integer.parseInt(globals.getProperty("update.intervall"));
        GregorianCalendar calFile = new GregorianCalendar(year, month, day);
        GregorianCalendar calCur = new GregorianCalendar();
        calFile.add(Calendar.DATE, intervall);
        if (silentB) {
            if (calCur.after(calFile)) {
                checkForUpdatesThread ch = new checkForUpdatesThread(updateSiteS, silentB);
            }
        } else {
            checkForUpdatesThread ch = new checkForUpdatesThread(updateSiteS, silentB);
        }
        return null;
    }

    public class checkForUpdatesThread extends Thread {

        String updateSiteS = "";

        boolean silentB = false;

        public checkForUpdatesThread(String _updateSiteS, boolean _silentB) {
            updateSiteS = _updateSiteS;
            silentB = _silentB;
            Thread runner = new Thread(this);
            runner.start();
            System.out.println("checkForUpdates: constructor");
        }

        /**
         * separate thread which checks the update site
         * It is a thread in order to cause no time delay for the user.
         */
        public synchronized void run() {
            String s;
            URL url = null;
            try {
                String localVersionS = globals.getProperty("jmathlib.version").replaceAll("/", ".");
                url = new URL(updateSiteS + "?jmathlib_version=" + localVersionS + "&command=check");
            } catch (Exception e) {
                throwMathLibException("checkForUpdates: malformed url");
            }
            Properties props = new Properties();
            try {
                props.load(url.openStream());
            } catch (Exception e) {
                ErrorLogger.debugLine("checkForUpdates: Properties error");
            }
            String localVersionS = globals.getProperty("jmathlib.version");
            String updateVersionS = props.getProperty("update.toversion");
            String updateActionS = props.getProperty("update.action");
            if (updateActionS.equals("INCREMENTAL_DOWNLOAD")) {
                if (!silentB) {
                    globals.getInterpreter().displayText("A full download ist required");
                    globals.getInterpreter().displayText("A new version " + updateVersionS + " is available");
                    globals.getInterpreter().displayText("\n Just type    update    at the prompt.");
                }
            } else if (updateActionS.equals("FULL_DOWNLOAD_REQUIRED")) {
                if (!silentB) {
                    globals.getInterpreter().displayText("A full download ist required");
                    globals.getInterpreter().displayText("A new version " + updateVersionS + " is available");
                    globals.getInterpreter().displayText("Go to www.jmathlib.de and download the latest version");
                }
            } else if (updateActionS.equals("NO_ACTION")) {
                if (!silentB) globals.getInterpreter().displayText("The local version of JMathLib is up to date");
            } else if (updateActionS.equals("VERSION_UNKNOWN")) {
                if (!silentB) globals.getInterpreter().displayText("The local version of JMathLib ist not recognized by the server");
            } else {
                globals.getInterpreter().displayText("check for updates encountered an error.");
            }
            debugLine("checkForUpdates: web:" + updateVersionS + " local:" + localVersionS);
            Calendar cal = Calendar.getInstance();
            String checkedDate = Integer.toString(cal.get(Calendar.YEAR)) + "/" + Integer.toString(cal.get(Calendar.MONTH) + 1) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
            globals.setProperty("update.date.last", checkedDate);
            Enumeration propnames = props.propertyNames();
            while (propnames.hasMoreElements()) {
                String propName = (String) propnames.nextElement();
                String propValue = (String) props.getProperty(propName);
                ErrorLogger.debugLine("Property: " + propName + " = " + propValue);
                globals.setProperty(propName, propValue);
            }
        }
    }
}
