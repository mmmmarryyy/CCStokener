package net.assimilator.qa.tests.monitor.opstring;

import net.assimilator.qa.core.OpStringUtils;
import net.assimilator.qa.core.VariableExpander;
import java.io.*;
import java.net.URL;

/**
 * The class provides utility methods for tests in the
 * <code>net.assimilator.qa.tests.monitor.opstring</code> package.
 *
 * @version $Id: Utils.java 117 2007-04-10 03:04:49Z khartig $
 */
public class Utils {

    /**
     * Adds commonly used OpString variables to a variable expander.
     * The variables are:
     * <ul>
     * <li><code>codeBasePrefix</code> - is set to expand to
     * <code>${net.assimilator.qa.core.jsbcodebaseprefix}</code>
     * <li><code>group</code> - is set to expand to
     * <code>${testGroupsAndLocators}</code>
     * </ul>
     *
     * @param expander the variable expander
     */
    public static void addCommonVariables(VariableExpander expander) {
        String val = OpStringUtils.getStringConfigVal("net.assimilator.qa.core.jsbcodebaseprefix", null);
        expander.addVariable("codeBasePrefix", val);
        val = OpStringUtils.getGroupsAndLocatorsItem("testGroupsAndLocators", null, "0");
        expander.addVariable("group", val);
    }

    /**
     * Reads a <code>URL</code> into a string.
     *
     * @param url the <code>URL</code> from which to read
     * @return the resulting string
     * @throws IOException if an I/O exception occurs
     */
    public static String read(URL url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringWriter res = new StringWriter();
        PrintWriter writer = new PrintWriter(new BufferedWriter(res));
        String line;
        while ((line = reader.readLine()) != null) {
            writer.println(line);
        }
        reader.close();
        writer.close();
        return res.toString();
    }
}
