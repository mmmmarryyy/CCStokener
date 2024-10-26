package de.tum.in.botl.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.nfunk.jep.JEP;
import org.nfunk.jep.function.PostfixMathCommandI;

/**
 * @author marschal
 *
 */
public class FunctionUtils {

    private static final String PROPERTY_FILE_NAME = "CustomFunctions.properties";

    private static Set customFunctionNames;

    private static Properties props = null;

    public static void addCustomFunctions(JEP parser) {
        if (props == null) props = loadProperties();
        customFunctionNames = new HashSet();
        for (Iterator it = props.keySet().iterator(); it.hasNext(); ) {
            String functionName = (String) it.next();
            String functionClassName = (String) props.getProperty(functionName);
            if (functionClassName != null) {
                Class functionClass = null;
                try {
                    functionClass = Class.forName(functionClassName);
                } catch (ClassNotFoundException e) {
                    System.out.println("Warning: Cannot find custom function class " + functionClassName + ". " + e.getMessage());
                }
                if (functionClass != null) try {
                    parser.addFunction(functionName, (PostfixMathCommandI) functionClass.newInstance());
                    customFunctionNames.add(functionName);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Warning: Cannot instantiate custom function class " + functionClass.getName() + ". " + e.getMessage());
                }
            }
        }
    }

    public static Properties loadProperties() {
        return loadProperties(PROPERTY_FILE_NAME);
    }

    public static Properties loadProperties(String fileName) {
        Properties props = new Properties();
        try {
            URL url = ClassLoader.getSystemResource(fileName);
            InputStream in = url.openStream();
            props.load(in);
            return props;
        } catch (Exception e) {
            System.out.println("Warning: Cannot load property file " + PROPERTY_FILE_NAME + ". " + e.getMessage());
            return props;
        }
    }

    public static Set getCustomFunctionNames() {
        return customFunctionNames;
    }
}
