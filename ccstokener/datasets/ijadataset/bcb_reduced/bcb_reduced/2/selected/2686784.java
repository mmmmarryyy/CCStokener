package de.uni_mannheim.swt.codeconjurer.techsrv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;

/**
 * This class is used to send usage data to the developers. It is not intended
 * to collect or transfer any personal data or to violate privacy. The
 * information sent, should only help the developers in improving the software.
 * 
 * @author Werner Janjic
 * 
 */
public class UsageDataSender {

    private static Logger logger = Logger.getLogger(UsageDataSender.class);

    /**
	 * A shortcut to report information without a HashMap
	 * 
	 * @param values
	 *            A string array containing the information in the format
	 *            "NAME_VALUE"
	 * @return
	 */
    public static boolean sendInformation(String... values) {
        HashMap<String, String> data = new HashMap<String, String>();
        for (String value : values) {
            logger.debug("Send " + value);
            data.put(value.substring(0, value.indexOf("_")), value.substring(value.indexOf("_") + 1, value.length()));
        }
        return sendInformation("udc", data);
    }

    /**
	 * Report the data provided in the HashMap to the developers
	 * 
	 * @param reportType
	 *            a string indicating the type of report (e.g. udc or reuse)
	 * @param data
	 * @return
	 */
    public static boolean sendInformation(String reportType, HashMap<String, String> data) {
        if (Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_UDC)) {
            logger.debug("Report usage information to devs...");
            String transferData = "reportType=" + reportType;
            transferData += "&build=" + Platform.getBundle("de.uni_mannheim.swt.codeconjurer").getHeaders().get("Bundle-Version");
            transferData += "&Suppl-Server=" + Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SERVER);
            for (String key : data.keySet()) {
                transferData += "&Suppl-" + key + "=" + data.get(key);
            }
            try {
                URL url = new URL("http://www.merobase.com:7777/org.code_conjurer.udc/UsageReport");
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(transferData);
                writer.flush();
                StringBuffer answer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line + "\r\n");
                }
                writer.close();
                reader.close();
                logger.debug("UDC Server answer: " + answer.toString());
            } catch (Exception e) {
                CrashReporter.reportException(e);
                logger.debug("Could not report usage data: " + e.toString());
                return false;
            }
            return true;
        } else {
            logger.debug("Reporting not wished!");
            return false;
        }
    }
}
