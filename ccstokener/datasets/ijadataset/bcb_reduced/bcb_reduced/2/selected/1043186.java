package org.mcisb.strenda.server.services;

import java.io.*;
import java.net.*;
import org.mcisb.gwt.shared.*;
import org.mcisb.strenda.client.services.*;
import org.mcisb.strenda.server.*;
import org.mcisb.strenda.shared.*;
import com.google.gwt.user.server.rpc.*;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class UniProtServiceImpl extends RemoteServiceServlet implements UniProtService {

    @Override
    public UniProtEntry[] getUniProtEntries(final String query) throws ServerException {
        InputStream is = null;
        try {
            final URL url = new URL("http://www.uniprot.org/uniprot/?query=" + query + "&format=xml");
            final URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                final int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalArgumentException("Unable to find UniProt entries for query: " + query);
                }
            }
            is = url.openStream();
            return UniProtUtils.getUniProtEntry(is);
        } catch (Exception e) {
            throw new ServerException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    throw new ServerException(e);
                }
            }
        }
    }
}
