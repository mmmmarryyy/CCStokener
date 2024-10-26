package com.stieglitech.problomatic.handlers;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import com.stieglitech.problomatic.InitException;
import com.stieglitech.problomatic.Problem;

/**
 * @author danstieglitz
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class XSLTransformationHandler extends AbstractProblemHandler {

    private static final String XSL_URL = "xsltransformationhandler.xslurl";

    private static final String PATH_TO_FILES = "xsltransformationhandler.pathtofiles";

    private static final String WRITE_TO_DISK = "xsltransformationhandler.writeToDisk";

    /**
	 * todo this implementation of initializing properties is not very clean...
	 * find a better way
	 */
    public void init(Properties props) throws InitException {
        setProperty(props, PATH_TO_FILES);
        setRequiredProperty(props, XSL_URL);
        setRequiredProperty(props, WRITE_TO_DISK);
    }

    public void handleProblem(Problem aProblem) {
        if (aProblem.hasAttribute("transformFlag") && aProblem.hasAttribute("URL")) {
            aProblem.removeAttribute("transformFlag");
            try {
                URL sourceURL = new java.net.URL(aProblem.getAttribute("URL").toString());
                String pathToDest = getProperty("pathtofiles") + java.io.File.separatorChar + +new Date().getTime() + ".html";
                transform(sourceURL, pathToDest);
                aProblem.setAttribute("URL", "file://" + pathToDest);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private void transform(URL urlToSource, String pathToDest) {
        PrintWriter outStream = null;
        try {
            String xslURL = getProperty("xslurl");
            TransformerFactory xformFactory = TransformerFactory.newInstance();
            StreamSource xslSource = new StreamSource(xslURL);
            Transformer transformer = xformFactory.newTransformer(xslSource);
            StreamSource xmlSource = new StreamSource(urlToSource.openStream());
            outStream = new PrintWriter(new FileOutputStream(pathToDest));
            StreamResult fileResult = new StreamResult(outStream);
            transformer.transform(xmlSource, fileResult);
        } catch (TransformerException transEx) {
            System.err.println("\nTransformation error");
            System.err.println(transEx.getMessage());
            Throwable ex = transEx;
            if (transEx.getException() != null) {
                ex = transEx.getException();
                System.err.println(ex.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            outStream.close();
        }
    }
}
