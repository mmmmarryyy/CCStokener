package tufts.oki.dr.fedora;

import java.io.*;
import java.net.*;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;

public class FedoraUtils {

    /** Creates a new instance of FedoraUtils */
    public static final String SEPARATOR = ",";

    public static final String NOT_DEFINED = "Property not defined";

    private static java.util.Map prefsCache = new java.util.HashMap();

    public static java.util.Vector stringToVector(String str) {
        java.util.Vector vector = new java.util.Vector();
        java.util.StringTokenizer st = new java.util.StringTokenizer(str, SEPARATOR);
        while (st.hasMoreTokens()) {
            vector.add(st.nextToken());
        }
        return vector;
    }

    public static String processId(String pid) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(pid, ":");
        String processString = "";
        while (st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }

    public static String getFedoraProperty(DR dr, String pLookupKey) throws osid.dr.DigitalRepositoryException {
        try {
            return getPreferences(dr).get(pLookupKey, NOT_DEFINED);
        } catch (Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraUtils.getFedoraProperty: " + ex);
        }
    }

    public static Preferences getPreferences(DR dr) throws java.io.FileNotFoundException, java.io.IOException, java.util.prefs.InvalidPreferencesFormatException {
        if (dr.getPrefernces() != null) {
            return dr.getPrefernces();
        } else {
            return getDefaultPreferences(dr.getConfiguration());
        }
    }

    public static Preferences getDefaultPreferences(URL url) throws java.io.FileNotFoundException, java.io.IOException, java.util.prefs.InvalidPreferencesFormatException {
        if (url == null) {
            url = new FedoraUtils().getClass().getResource("fedora.conf");
            new Throwable("using default preferences " + url).printStackTrace();
        }
        Preferences prefs = (Preferences) prefsCache.get(url);
        if (prefs != null) return prefs;
        Class clazz = new FedoraUtils().getClass();
        prefs = Preferences.userRoot().node(clazz.getPackage().getName());
        System.out.println("*** " + clazz.getName() + ".getPreferences: node=" + prefs);
        System.out.println("*** " + clazz.getName() + ".getPreferences: loading & caching prefs from \"" + url + "\"");
        InputStream stream = new BufferedInputStream(url.openStream());
        prefs.importPreferences(stream);
        prefsCache.put(url, prefs);
        stream.close();
        return prefs;
    }

    public static String[] getFedoraPropertyArray(DR dr, String pLookupKey) throws osid.dr.DigitalRepositoryException {
        String pValue = getFedoraProperty(dr, pLookupKey);
        return pValue.split(SEPARATOR);
    }

    public static String[] getAdvancedSearchFields(DR dr) throws osid.dr.DigitalRepositoryException {
        return getFedoraPropertyArray(dr, "fedora.search.advanced.fields");
    }

    public static String[] getAdvancedSearchOperators(DR dr) throws osid.dr.DigitalRepositoryException {
        return getFedoraPropertyArray(dr, "fedora.search.advanced.operators");
    }

    public static String getAdvancedSearchOperatorsActuals(DR dr, String pOperator) throws osid.dr.DigitalRepositoryException {
        String[] pOperators = getAdvancedSearchOperators(dr);
        String[] pOperatorsActuals = getFedoraPropertyArray(dr, "fedora.search.advanced.operators.actuals");
        String pValue = NOT_DEFINED;
        boolean flag = true;
        for (int i = 0; i < pOperators.length && flag; i++) {
            if (pOperators[i].equalsIgnoreCase(pOperator)) {
                pValue = pOperatorsActuals[i];
                flag = false;
            }
        }
        return pValue;
    }

    public static String getSaveFileName(osid.shared.Id objectId, osid.shared.Id behaviorId, osid.shared.Id disseminationId) throws osid.OsidException {
        String saveFileName = processId(objectId.getIdString() + "-" + behaviorId.getIdString() + "-" + disseminationId.getIdString());
        return saveFileName;
    }

    public static AbstractAction getFedoraAction(osid.dr.InfoRecord infoRecord, osid.dr.DigitalRepository dr) throws osid.dr.DigitalRepositoryException {
        final DR mDR = (DR) dr;
        final InfoRecord mInfoRecord = (InfoRecord) infoRecord;
        try {
            AbstractAction fedoraAction = new AbstractAction(infoRecord.getId().getIdString()) {

                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                        String fedoraUrl = mInfoRecord.getInfoField(new PID(getFedoraProperty(mDR, "DisseminationURLInfoPartId"))).getValue().toString();
                        URL url = new URL(fedoraUrl);
                        URLConnection connection = url.openConnection();
                        System.out.println("FEDORA ACTION: Content-type:" + connection.getContentType() + " for url :" + fedoraUrl);
                        tufts.Util.openURL(fedoraUrl);
                    } catch (Exception ex) {
                    }
                }
            };
            return fedoraAction;
        } catch (Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraUtils.getFedoraAction " + ex.getMessage());
        }
    }

    private FedoraUtils() {
    }
}
