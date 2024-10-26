package edu.mit.wi.omnigene.omnidas.handler;

import edu.mit.wi.omnigene.omnidas.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * The base class for all <code> RequestHandler Objects
 * This class is intended to take in a DASRequest object
 * Inspect its contents and then construct a properly formatted
 * <code>URL</code> from that object
 * a typical use for these objects would be to
 * construct a DASRequest:
 *
 *
 * DASDSNRequestHandler handler = new DASRequestHandler();
 *
 * set its DASRequest with {@link #setDASRequest(DASRequest) DASRequest}
 *
 * get some MetaData about the server {@link #fillINDASMetaData() MetaData}
 *
 * and then execute {@link #getDASResponse() DASResponse}
 *@author Brian Gilman
 *@version $Revision: 1.9 $
 */
public abstract class RequestHandler {

    private DASResponse theResponse = null;

    private DASMetaData theMetaData = null;

    private Hashtable headers = new Hashtable();

    private String dasRespVersion = null;

    private String dasSchema = null;

    private String dasSchemaVersion = null;

    private float dasVersion;

    private int dasStatus;

    private HttpURLConnection con = null;

    public RequestHandler() {
    }

    public abstract DASResponse getDASResponse() throws DASException;

    public abstract void setDASRequest(DASRequest req);

    public synchronized DASMetaData fillInDASMetaData(URL url) throws DASException {
        try {
            con = (HttpURLConnection) url.openConnection();
            dasRespVersion = con.getHeaderField("X-DAS-Version");
            dasSchema = con.getHeaderField("X-DAS-SchemaName");
            dasSchemaVersion = con.getHeaderField("X-DAS-SchemaVersion");
            String dasStatusString = con.getHeaderField("X-DAS-Status");
            if (dasStatusString == null) {
                throw new DASException("Temporary DAS Error");
            }
            if (dasStatusString.indexOf(" ") != -1) {
                dasStatusString = dasStatusString.substring(0, dasStatusString.indexOf(" "));
            }
            dasStatus = Integer.parseInt(dasStatusString);
            if (dasStatus != 200) {
                throw new DASException("Command cannot be executed: Error was " + Integer.toString(dasStatus));
            }
        } catch (IOException e) {
            throw new DASException("Cannot connect to data source");
        }
        if (dasSchema != null && dasSchemaVersion != null) {
            headers.put("X-DAS-Version", dasRespVersion);
            headers.put("X-DAS-SchemaName", dasSchema);
            headers.put("X-DAS-SchemaVersion", dasSchemaVersion);
            dasVersion = Float.parseFloat(dasRespVersion.substring(dasRespVersion.indexOf("/") + 1, dasRespVersion.length()));
            theMetaData = new DASMetaDataImpl(dasVersion, Float.parseFloat(dasSchemaVersion), dasSchema);
        } else {
            dasVersion = Float.parseFloat(dasRespVersion.substring(dasRespVersion.indexOf("/") + 1, dasRespVersion.length()));
            headers.put("X-DAS-Version", dasRespVersion);
            theMetaData = new DASMetaDataImpl(dasVersion);
        }
        String lengthStr = con.getHeaderField("content-length");
        if (lengthStr != null) headers.put("content-length", lengthStr);
        theMetaData.setDASHeaders(headers);
        return theMetaData;
    }
}
