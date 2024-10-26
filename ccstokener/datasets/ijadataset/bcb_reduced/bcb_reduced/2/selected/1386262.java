package org.tigr.cloe.model.facade.consensusFacade.sliceService;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.tigr.cloe.model.facade.consensusFacade.ConsensusFacadeParameters;
import org.tigr.cloe.model.facade.consensusFacade.IAssemblySliceRange;
import org.tigr.seq.aserver.SliceServerException;
import org.tigr.seq.seqdata.SeqdataException;

/**
 * This implementation of <code>TigrSliceServerJob</code>
 * contacts the new Jboss Server that will
 * replace Aserver.  This new server receives requests
 * via HTTP POSTs and returns data via HTTP responses .
 * @author dkatzel
 *
 *
 */
public class AshleyTigrSliceServerJob extends TigrSliceServerJob {

    private String URL_PATH = "http://localhost:8080/Ashley/Consensus";

    private URL url;

    public AshleyTigrSliceServerJob(IAssemblySliceRange pAssemblySliceRange, ConsensusFacadeParameters sliceParams, String consensusFacadeURL) throws SliceServerException, SeqdataException {
        super(pAssemblySliceRange, sliceParams, consensusFacadeURL);
        try {
            url = new URL(consensusFacadeURL);
        } catch (MalformedURLException e) {
            throw new SliceServerException("Error trying to create Ashley consensus URL", e);
        }
    }

    @Override
    protected InputStream sendRequest(byte[] pRequest) throws SliceServerException {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            OutputStream os = urlConnection.getOutputStream();
            os.write(pRequest);
            os.close();
            return urlConnection.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
            throw new SliceServerException("Error trying to send data to slice server " + e.getMessage(), e);
        }
    }
}
