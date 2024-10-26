package DE.FhG.IGD.semoa.net;

import DE.FhG.IGD.util.URL;
import DE.FhG.IGD.util.*;
import DE.FhG.IGD.semoa.net.*;
import DE.FhG.IGD.semoa.server.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Sends agents via the HTTP protocol, using a <code>PUT</code>
 * request.
 *
 * @author Patric Kabus
 * @author Volker Roth
 * @version "$Id: HTTPOutGate.java 117 2000-12-06 17:47:39Z vroth $"
 */
public class HTTPOutGate extends RawOutGate {

    /**
     * The protocol identifier.
     */
    public static final String PROTOCOL = "http";

    /**
     * The MIME-Type used for sending agents.
     */
    public static final String MIME_TYPE = "application/x-semoa-agent";

    /**
     * The size of the read/write buffer, currently 1K.
     */
    public static final int BUF_SIZE = 1024;

    /**
     * Creates an instance.
     */
    public HTTPOutGate() {
    }

    public String author() {
        return People.PKABUS + ", " + People.VROTH;
    }

    public String info() {
        return "Sends agents via HTTP";
    }

    public String revision() {
        return "$Revision: 117 $/$Date: 2000-12-06 12:47:39 -0500 (Wed, 06 Dec 2000) $";
    }

    public String protocol() {
        return PROTOCOL;
    }

    public void dispatchAgent(Resource struct, Ticket ticket) throws TicketNotSupportedException {
        BufferedOutputStream out;
        HttpURLConnection conn;
        java.net.URL url;
        URL[] targets;
        int n;
        if (ticket == null || struct == null) {
            throw new NullPointerException("Ticket or Resource!");
        }
        targets = ticket.getTarget(protocol());
        if (targets.length == 0) {
            throw new TicketNotSupportedException("Bad ticket, no \"" + protocol() + "\" target specified!");
        }
        try {
            url = new java.net.URL(targets[0].toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("content-type", MIME_TYPE);
            conn.setRequestProperty("User-Agent", "SeMoA-HttpOutGate");
            out = new BufferedOutputStream(conn.getOutputStream(), BUF_SIZE);
            Resources.zip(struct, out);
            out.flush();
            n = conn.getResponseCode();
            if (n == 200) {
                return;
            }
            throw new TicketNotSupportedException("[" + protocol() + "] Server returned error " + n);
        } catch (IOException e) {
            throw new TicketNotSupportedException(e.getMessage());
        }
    }
}
