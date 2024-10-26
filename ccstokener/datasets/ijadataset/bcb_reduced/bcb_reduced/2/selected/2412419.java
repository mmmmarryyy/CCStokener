package pl.edu.mimuw.xqtav.xqgen.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.impl.Log4JLogger;
import pl.edu.mimuw.xqtav.xqgen.XQGeneratorException;
import pl.edu.mimuw.xqtav.xqgen.api.TavXQueryGenerator;

/**
 * @author marchant
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XQGeneratorWrapper {

    public String generatorClass = "pl.edu.mimuw.xqtav.xqgen.xqgenerator_1.XQGenerator_1";

    public byte[] getXQueryForWorkflow(String workflowURI, Log4JLogger log) throws MalformedURLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (workflowURI == null) {
            throw new XQGeneratorException("Null workflow URI");
        }
        URL url = new URL(workflowURI);
        URLConnection urlconn = url.openConnection();
        urlconn.setAllowUserInteraction(false);
        urlconn.setDoInput(true);
        urlconn.setDoOutput(false);
        urlconn.setUseCaches(true);
        urlconn.connect();
        InputStream is = urlconn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TavXQueryGenerator generator = (TavXQueryGenerator) Class.forName(generatorClass).newInstance();
        generator.setLogger(log);
        generator.setInputStream(is);
        generator.setOutputStream(baos);
        generator.generateXQuery();
        is.close();
        return baos.toByteArray();
    }
}
