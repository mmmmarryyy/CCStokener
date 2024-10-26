package tufts.oki.repository.fedora;

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

    public static String getFedoraProperty(Repository repository, String pLookupKey) throws osid.repository.RepositoryException {
        try {
            return getPreferences(repository).get(pLookupKey, NOT_DEFINED);
        } catch (Exception ex) {
            throw new osid.repository.RepositoryException("FedoraUtils.getFedoraProperty: " + ex);
        }
    }

    public static Preferences getPreferences(Repository repository) throws java.io.FileNotFoundException, java.io.IOException, java.util.prefs.InvalidPreferencesFormatException {
        if (repository.getPrefernces() != null) {
            return repository.getPrefernces();
        } else {
            URL url = repository.getConfiguration();
            Preferences prefs = (Preferences) prefsCache.get(url);
            if (prefs != null) return prefs;
            prefs = Preferences.userRoot().node("/");
            System.out.println("*** FedoraUtils.getPreferences: loading & caching prefs from \"" + url + "\"");
            InputStream stream = new BufferedInputStream(url.openStream());
            prefs.importPreferences(stream);
            prefsCache.put(url, prefs);
            stream.close();
            return prefs;
        }
    }

    public static String[] getFedoraPropertyArray(Repository repository, String pLookupKey) throws osid.repository.RepositoryException {
        String pValue = getFedoraProperty(repository, pLookupKey);
        return pValue.split(SEPARATOR);
    }

    public static String[] getAdvancedSearchFields(Repository repository) throws osid.repository.RepositoryException {
        return getFedoraPropertyArray(repository, "fedora.search.advanced.fields");
    }

    public static String[] getAdvancedSearchOperators(Repository repository) throws osid.repository.RepositoryException {
        return getFedoraPropertyArray(repository, "fedora.search.advanced.operators");
    }

    public static String getAdvancedSearchOperatorsActuals(Repository repository, String pOperator) throws osid.repository.RepositoryException {
        String[] pOperators = getAdvancedSearchOperators(repository);
        String[] pOperatorsActuals = getFedoraPropertyArray(repository, "fedora.search.advanced.operators.actuals");
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

    public static AbstractAction getFedoraAction(osid.repository.Record record, osid.repository.Repository repository) throws osid.repository.RepositoryException {
        final Repository mRepository = (Repository) repository;
        final Record mRecord = (Record) record;
        try {
            AbstractAction fedoraAction = new AbstractAction(record.getId().getIdString()) {

                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                        osid.shared.Id id = new PID(getFedoraProperty(mRepository, "DisseminationURLInfoPartId"));
                        osid.repository.PartIterator partIterator = mRecord.getParts();
                        while (partIterator.hasNextPart()) {
                            osid.repository.Part part = partIterator.nextPart();
                            {
                                String fedoraUrl = part.getValue().toString();
                                URL url = new URL(fedoraUrl);
                                URLConnection connection = url.openConnection();
                                System.out.println("FEDORA ACTION: Content-type:" + connection.getContentType() + " for url :" + fedoraUrl);
                                tufts.Util.openURL(fedoraUrl);
                                break;
                            }
                        }
                    } catch (Throwable t) {
                    }
                }
            };
            return fedoraAction;
        } catch (Throwable t) {
            throw new osid.repository.RepositoryException("FedoraUtils.getFedoraAction " + t.getMessage());
        }
    }
}
