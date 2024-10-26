package org.mcisb.jws.ws;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.jws.*;
import javax.jws.soap.*;
import javax.xml.ws.*;
import org.mcisb.util.io.*;

/**
 * 
 * @author Neil Swainston
 */
@WebService(name = "JwsInterface", serviceName = "JwsInterfaceServer")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class JwsInterfaceImpl {

    /**
	 * 
	 */
    private static final String URL_SEPARATOR = "/";

    /**
	 * 
	 */
    private static final String SERVER = "http://jjj.biochem.sun.ac.za";

    /**
	 * 
	 */
    private static final String DOWNLOAD_BASE_URL = SERVER + URL_SEPARATOR + "database/";

    /**
	 * 
	 */
    private static final String UPLOAD_URL = SERVER + URL_SEPARATOR + "webMathematica/upload/upload.jsp";

    /**
	 * 
	 */
    private static final String EXTENSION = ".xml";

    /**
	 * @param modelId
	 * @return String
	 * @throws java.io.IOException
	 */
    @WebMethod
    public String get(@WebParam(name = "modelId") final String modelId) throws java.io.IOException {
        final String url = DOWNLOAD_BASE_URL + modelId + URL_SEPARATOR + modelId + EXTENSION;
        return new String(StreamReader.read(new URL(url).openStream()));
    }

    /**
	 * 
	 * @param filename
	 * @param fileContent
	 * @return String
	 * @throws java.io.IOException
	 */
    @WebMethod
    public String put(@WebParam(name = "filename") final String filename, @WebParam(name = "fileContent") final String fileContent) throws java.io.IOException {
        final URL url = new URL(UPLOAD_URL);
        final Map<String, Object> nameValuePairs = new HashMap<String, Object>();
        final File file = new File(System.getProperty("java.io.tmpdir"), filename);
        new FileUtils().write(file, fileContent.getBytes());
        nameValuePairs.put("upfile", file);
        return new String(StreamReader.read(ClientHttpRequest.doPost(url, nameValuePairs)));
    }

    /**
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        Endpoint.publish(args[0], new JwsInterfaceImpl());
    }
}
