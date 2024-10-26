package org.horen.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.jface.preference.IPreferenceStore;
import org.horen.core.HorenPlugin;

/**
 * BugReport is a class to report bugs to the development trac system.
 * 
 * @author Pascal
 */
public class TracBugReport {

    private static String m_strPriorities[] = null;

    private static String m_strComponents[] = null;

    static {
        System.setProperty("javax.net.ssl.trustStore", "lib/trusted_keys");
    }

    /**
	 * Send a bug report.
	 * 
	 * @param exception
	 *        the catched Exception
	 * @param strUserName
	 *        the user name of the reporter
	 * @param strShortDescription
	 *        short description to the bug
	 * @param strLongDescription
	 *        the description, how the bug was generated
	 * @param strPriority
	 *        priority of the bug fixing
	 * @param strComponent
	 *        defect component
	 */
    public static void track(Exception exception, String strUserName, String strShortDescription, String strLongDescription, String strPriority, String strComponent) {
        String newLongDescription = strLongDescription;
        newLongDescription += "\n-----\n\n";
        newLongDescription += "Folgende Exception ist aufgetreten:\n\n";
        newLongDescription += "{{{\n";
        newLongDescription += exception + " - " + exception.getMessage() + "\n";
        for (StackTraceElement elem : exception.getStackTrace()) {
            newLongDescription += elem.toString() + "\n";
        }
        newLongDescription += "}}}\n";
        String newShortDescription = "[" + exception + "] " + strShortDescription;
        track(strUserName, newShortDescription, newLongDescription, strPriority, strComponent);
    }

    /**
	 * Send a bug report.
	 * 
	 * @param exception
	 *        the catched Exception
	 * @param strUserName
	 *        the user name of the reporter
	 * @param strShortDescription
	 *        short description to the bug
	 * @param strPriority
	 *        priority of the bug fixing
	 * @param strComponent
	 *        defect component
	 */
    public static void track(Exception exception, String strUserName, String strShortDescription, String strPriority, String strComponent) {
        track(exception, strUserName, strShortDescription, "", strPriority, strComponent);
    }

    /**
	 * Send a bug report.
	 * 
	 * @param strUserName
	 *        the user name of the reporter
	 * @param strShortDescription
	 *        short description to the bug
	 * @param strLongDescription
	 *        the description, how the bug was generated
	 * @param strPriority
	 *        priority of the bug fixing
	 * @param strComponent
	 *        defect component
	 */
    public static void track(String strUserName, String strShortDescription, String strLongDescription, String strPriority, String strComponent) {
        String strFromToken = "";
        try {
            URL url = new URL(getTracUrl() + "newticket");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String buffer = reader.readLine();
            while (buffer != null) {
                if (buffer.contains("__FORM_TOKEN")) {
                    Pattern pattern = Pattern.compile("value=\"[^\"]*\"");
                    Matcher matcher = pattern.matcher(buffer);
                    int start = 0;
                    matcher.find(start);
                    int von = matcher.start() + 7;
                    int bis = matcher.end() - 1;
                    strFromToken = buffer.substring(von, bis);
                }
                buffer = reader.readLine();
            }
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(getTracUrl() + "newticket");
            method.setRequestHeader("Cookie", "trac_form_token=" + strFromToken);
            method.addParameter("__FORM_TOKEN", strFromToken);
            method.addParameter("reporter", strUserName);
            method.addParameter("summary", strShortDescription);
            method.addParameter("type", "Fehler");
            method.addParameter("description", strLongDescription);
            method.addParameter("action", "create");
            method.addParameter("status", "new");
            method.addParameter("priority", strPriority);
            method.addParameter("milestone", "");
            method.addParameter("component", strComponent);
            method.addParameter("keywords", "BugReporter");
            method.addParameter("cc", "");
            method.addParameter("version", "");
            client.executeMethod(method);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Send a bug report.
	 * 
	 * @param strUserName
	 *        the user name of the reporter
	 * @param strShortDescription
	 *        short description to the bug
	 * @param strPriority
	 *        priority of the bug fixing
	 * @param strComponent
	 *        defect component
	 */
    public static void track(String strUserName, String strShortDescription, String strPriority, String strComponent) {
        track(strUserName, strShortDescription, "", strPriority, strComponent);
    }

    /**
	 * select the available priorities from the trac system
	 * 
	 * @return list of supported priorities
	 * @throws BugReportException
	 */
    public static String[] getPriorities() {
        if (m_strPriorities == null) {
            setMembers();
        }
        if (m_strPriorities == null) {
            throw new BugReportException("Can't get Components.");
        }
        return m_strPriorities;
    }

    /**
	 * select the available components from the trac system
	 * 
	 * @return list of supported priorities
	 * @throws BugReportException
	 */
    public static String[] getComponents() {
        if (m_strComponents == null) {
            setMembers();
        }
        if (m_strComponents == null) {
            throw new BugReportException("Can't get Components.");
        }
        return m_strComponents;
    }

    /**
	 * find out the url of the trac system
	 * 
	 * @return url of the trac system
	 */
    private static String getTracUrl() {
        try {
            IPreferenceStore store = HorenPlugin.getInstance().getPreferenceStore();
            store.setDefault("TRAC_URL", "https://faracvs.cs.uni-magdeburg.de/projects/pheld-itSoftwareProjekt/");
            return store.getString("TRAC_URL");
        } catch (RuntimeException e) {
            return "https://faracvs.cs.uni-magdeburg.de/projects/pheld-itSoftwareProjekt/";
        }
    }

    private static void setMembers() {
        try {
            URL url = new URL(getTracUrl() + "newticket");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String buffer = reader.readLine();
            while (buffer != null) {
                if (buffer.contains("<select id=\"component\" name=\"component\">")) {
                    Pattern pattern = Pattern.compile(">[^<]+?<");
                    Matcher matcher = pattern.matcher(buffer);
                    Vector<String> erg = new Vector<String>();
                    int start = 0;
                    while (matcher.find(start)) {
                        int von = matcher.start() + 1;
                        int bis = matcher.end() - 1;
                        erg.add(Recoder.recode(buffer.substring(von, bis), "UTF-8", Recoder.getDefaultEncoding()));
                        start = bis;
                    }
                    m_strComponents = new String[erg.size()];
                    erg.toArray(m_strComponents);
                }
                if (buffer.contains("<select id=\"priority\" name=\"priority\">")) {
                    Pattern pattern = Pattern.compile(">[^<]+?<");
                    Matcher matcher = pattern.matcher(buffer);
                    Vector<String> erg = new Vector<String>();
                    int start = 0;
                    while (matcher.find(start)) {
                        int von = matcher.start() + 1;
                        int bis = matcher.end() - 1;
                        erg.add(Recoder.recode(buffer.substring(von, bis), "UTF-8", Recoder.getDefaultEncoding()));
                        start = bis;
                    }
                    m_strPriorities = new String[erg.size()];
                    erg.toArray(m_strPriorities);
                }
                buffer = reader.readLine();
            }
        } catch (MalformedURLException e) {
            System.out.println("e1");
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
