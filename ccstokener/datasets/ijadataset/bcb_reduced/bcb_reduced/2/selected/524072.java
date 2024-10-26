package org.openscience.cdk.xws.services;

import java.io.InputStream;
import java.net.URL;
import net.bioclipse.xws.JavaDOMTools;
import net.bioclipse.xws.component.adhoc.function.FunctionInformation;
import net.bioclipse.xws.component.adhoc.function.IFunction;
import net.bioclipse.xws.component.xmpp.process.IProcessStatus;
import org.openscience.cdk.xws.ResourceAsStringTool;
import org.w3c.dom.Element;

public class RONBridge implements IFunction {

    private static final String FUNCTION_NAME = "Bridge to RDF.OpenMolecules.net";

    private static final String FUNCTION_DESCRIPTION = "Retrieves information on the given molecule.";

    private static final String FUNCTION_DETAILS = "Returns information in RDF format.";

    private static final String IN_SCHEMATA = ResourceAsStringTool.getAsString("org/openscience/cdk/xws/schema/rdfDescription.xml");

    private static final String OUT_SCHEMATA = ResourceAsStringTool.getAsString("org/openscience/cdk/xws/schema/rdfRDF.xml");

    private static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public FunctionInformation getFunctionInformation() {
        FunctionInformation info = new FunctionInformation(FUNCTION_NAME, FUNCTION_DESCRIPTION, FUNCTION_DETAILS, IN_SCHEMATA, OUT_SCHEMATA, false);
        return info;
    }

    public void run(IProcessStatus ps, Element input) {
        Element output = null;
        if (NS_RDF.equals(input.getNamespaceURI()) && "Description".equals(input.getLocalName())) {
            String resource = input.getAttribute("about");
            if (resource != null && resource.startsWith("http://rdf.openmolecules.net/?InChI=1/")) {
                System.out.println("resource: " + resource);
                try {
                    URL url = new URL(resource);
                    String rdfOutput = copyToString(url.openStream());
                    output = JavaDOMTools.string2Element(rdfOutput);
                } catch (Exception e) {
                    System.out.println("ERROR IN RDF.bridge.ron: creating element" + " failed: " + e.getMessage());
                    e.printStackTrace();
                    output = null;
                }
            } else {
                ps.setError("Expected the rdf:about value to start with " + "http://rdf.openmolecules.net/?InChI=1/, but got: " + resource);
            }
        } else {
            ps.setError("Expected rdf:Description but got: " + input.getNamespaceURI());
        }
        ps.setResult(output, "Done");
    }

    private String copyToString(InputStream input) throws Exception {
        StringBuffer strBuffer = new StringBuffer();
        byte buffer[] = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            strBuffer.append(new String(buffer, 0, bytesRead));
        }
        input.close();
        return strBuffer.toString();
    }
}
