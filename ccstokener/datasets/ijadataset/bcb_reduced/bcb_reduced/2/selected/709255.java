package hu.sztaki.lpds.storage.service.carmen.client;

import hu.sztaki.lpds.storage.service.carmen.server.quota.QuotaBean;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Sends the quota information collected
 *  on the submitter side to the storage
 * (if the submitter is on local mode than
 * the quota information needed to be sent
 * with an independent call to storage)
 *
 * @author lpds
 */
public class QuotaSender {

    private String storageUrlString;

    private Vector quotaBeans;

    /**
     * Parameter is the URL of the storage service (receiverServlet).
     *
     * (http://localhost:8080/storage/receiver)
     *
     * @param storageUrlString
     */
    public QuotaSender(String storageUrlString) {
        this.storageUrlString = storageUrlString;
    }

    /**
     * Setting the sending parameters
     *
     * @param quotaBeans
     * @throws Exception
     */
    public void setParameters(Vector quotaBeans) throws Exception {
        this.quotaBeans = quotaBeans;
        if (!validParameters()) {
            throw new Exception("Not valid parameters: quotaBeans !");
        }
    }

    /**
     * Checks whether the base parameter values are real or not
     *
     * @return
     */
    private boolean validParameters() {
        if (quotaBeans != null) {
            return true;
        }
        return false;
    }

    /**
     * Sends the set parameters to the storage
     *
     * @param localMode
     *
     * (if the submitter and the storage are on the same machine, than it is true, else false)
     * @throws Exception quota data sending error
     */
    public void sendInformations(boolean localMode) throws Exception {
        sendLocal();
    }

    /**
     * Sends the files given in the basic 
     * parameters with POST request to the given servlet.
     */
    private void sendLocal() throws Exception {
        if (validParameters()) {
            URL url = new URL(storageUrlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            RequestUtils requestUtils = new RequestUtils();
            requestUtils.preRequestAddParameter("senderObj", "QuotaSender");
            requestUtils.preRequestAddParameter("beanNumbers", new String().valueOf(quotaBeans.size()));
            for (int vPos = 0; vPos < quotaBeans.size(); vPos++) {
                QuotaBean bean = (QuotaBean) quotaBeans.get(vPos);
                requestUtils.preRequestAddParameter("" + vPos + "#portalID", bean.getPortalID());
                requestUtils.preRequestAddParameter("" + vPos + "#userID", bean.getUserID());
                requestUtils.preRequestAddParameter("" + vPos + "#workflowID", bean.getWorkflowID());
                requestUtils.preRequestAddParameter("" + vPos + "#runtimeID", bean.getRuntimeID());
                requestUtils.preRequestAddParameter("" + vPos + "#plussQuotaSize", bean.getPlussQuotaSize().toString());
            }
            requestUtils.preRequestAddFile("zipFileName", "dummyZipFileName.zip");
            requestUtils.createPostRequest();
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + requestUtils.getBoundary());
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            try {
                httpURLConnection.connect();
                OutputStream out = httpURLConnection.getOutputStream();
                byte[] preBytes = requestUtils.getPreRequestStringBytes();
                out.write(preBytes);
                out.flush();
                out.write(new String("dummyFile").getBytes());
                byte[] postBytes = requestUtils.getPostRequestStringBytes();
                out.write(postBytes);
                out.flush();
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                in.readLine();
                in.close();
                if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
                    throw new Exception("response not HTTP_OK !");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Cannot connect to: " + storageUrlString, e);
            }
        } else {
            throw new Exception("Not valid parameters: quotaBeans !");
        }
    }
}
