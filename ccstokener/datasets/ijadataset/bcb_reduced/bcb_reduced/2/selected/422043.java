package org.apache.axis2.tool.codegen;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL11ToAxisServiceBuilder;
import org.apache.axis2.util.CommandLineOption;
import org.apache.axis2.util.CommandLineOptionConstants;
import javax.wsdl.WSDLException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WSDL2JavaGenerator {

    /**
     * Maps a string containing the name of a language to a constant defined in CommandLineOptionConstants.LanguageNames
     * 
     * @param UILangValue a string containg a language, e.g. "java", "cs", "cpp" or "vb"
     * @return a normalized string constant
     */
    private String mapLanguagesWithCombo(String UILangValue) {
        return UILangValue;
    }

    /**
     * Creates a list of parameters for the code generator based on the decisions made by the user on the OptionsPage
     * (page2). For each setting, there is a Command-Line option for the Axis2 code generator.
     * 
     * @return a Map with keys from CommandLineOptionConstants with the values entered by the user on the Options Page.
     */
    public Map fillOptionMap(boolean isAyncOnly, boolean isSyncOnly, boolean isServerSide, boolean isServerXML, boolean isTestCase, boolean isGenerateAll, String serviceName, String portName, String databindingName, String WSDLURI, String packageName, String selectedLanguage, String outputLocation, String namespace2packageList, boolean isServerSideInterface, HashMap advanceOptions) {
        Map optionMap = new HashMap();
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.WSDL_LOCATION_URI_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.WSDL_LOCATION_URI_OPTION, getStringArray(WSDLURI)));
        if (isAyncOnly) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_ASYNC_ONLY_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_ASYNC_ONLY_OPTION, new String[0]));
        }
        if (isSyncOnly) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_SYNC_ONLY_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_SYNC_ONLY_OPTION, new String[0]));
        }
        if (isServerSide) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_CODE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_CODE_OPTION, new String[0]));
            if (isServerXML) {
                optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_SERVICE_DESCRIPTION_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_SERVICE_DESCRIPTION_OPTION, new String[0]));
            }
            if (isGenerateAll) {
                optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_ALL_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_ALL_OPTION, new String[0]));
            }
        }
        if (isTestCase) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_TEST_CASE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_TEST_CASE_OPTION, new String[0]));
        }
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, getStringArray(packageName)));
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.STUB_LANGUAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.STUB_LANGUAGE_OPTION, getStringArray(mapLanguagesWithCombo(selectedLanguage))));
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.OUTPUT_LOCATION_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.OUTPUT_LOCATION_OPTION, getStringArray(outputLocation)));
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.DATA_BINDING_TYPE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.DATA_BINDING_TYPE_OPTION, getStringArray(databindingName)));
        if (portName != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.PORT_NAME_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.PORT_NAME_OPTION, getStringArray(portName)));
        }
        if (serviceName != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVICE_NAME_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVICE_NAME_OPTION, getStringArray(serviceName)));
        }
        if (namespace2packageList != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.NAME_SPACE_TO_PACKAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.NAME_SPACE_TO_PACKAGE_OPTION, getStringArray(namespace2packageList)));
        }
        if (isServerSideInterface) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_INTERFACE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_INTERFACE_OPTION, new String[0]));
        }
        if (advanceOptions != null) {
            for (Iterator iterator = advanceOptions.keySet().iterator(); iterator.hasNext(); ) {
                String type = (String) iterator.next();
                String[] parameters;
                if (advanceOptions.get(type) == null) parameters = new String[0]; else parameters = (String[]) advanceOptions.get(type);
                optionMap.put(type, new CommandLineOption(type, parameters));
            }
        }
        return optionMap;
    }

    public String getBaseUri(String wsdlURI) {
        try {
            URL url;
            if (wsdlURI.indexOf("://") == -1) {
                url = new URL("file", "", wsdlURI);
            } else {
                url = new URL(wsdlURI);
            }
            String baseUri;
            if ("file".equals(url.getProtocol())) {
                baseUri = new File(url.getFile()).getParentFile().toURL().toExternalForm();
            } else {
                baseUri = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf("/"));
            }
            return baseUri;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the WSDL Object Model from the given location.
     * 
     * @param wsdlURI the filesystem location (full path) of the WSDL file to read in.
     * @return the WSDLDescription object containing the WSDL Object Model of the given WSDL file
     * @throws WSDLException when WSDL File is invalid
     * @throws IOException on errors reading the WSDL file
     */
    public AxisService getAxisService(String wsdlURI) throws Exception {
        URL url;
        if (wsdlURI.indexOf("://") == -1) {
            url = new URL("file", "", wsdlURI);
        } else {
            url = new URL(wsdlURI);
        }
        WSDL11ToAxisServiceBuilder builder = new WSDL11ToAxisServiceBuilder(url.openConnection().getInputStream());
        builder.setDocumentBaseUri(url.toString());
        builder.setBaseUri(getBaseUri(wsdlURI));
        builder.setCodegen(true);
        return builder.populateService();
    }

    /**
     * Converts a single String into a String Array
     * 
     * @param value a single string
     * @return an array containing only one element
     */
    private String[] getStringArray(String value) {
        String[] values = new String[1];
        values[0] = value;
        return values;
    }
}
