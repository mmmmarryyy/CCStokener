package in.raster.oviyam.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Sends the DICOM dataset to the user.
 * @author bharathi
 * @version 0.7
 *
 */
public class DICOMDataset extends HttpServlet {

    /**
	 * Initialize the logger.
	 */
    private static Logger log = Logger.getLogger(DICOMDataset.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String dataSetURL = request.getParameter("datasetURL");
        String contentType = request.getParameter("contentType");
        String studyUID = request.getParameter("studyUID");
        String seriesUID = request.getParameter("seriesUID");
        String objectUID = request.getParameter("objectUID");
        dataSetURL += "&contentType=" + contentType + "&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID;
        dataSetURL = dataSetURL.replace("+", "%2B");
        InputStream resultInStream = null;
        OutputStream resultOutStream = response.getOutputStream();
        try {
            URL url = new URL(dataSetURL);
            resultInStream = url.openStream();
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = resultInStream.read(buffer)) != -1) {
                resultOutStream.write(buffer, 0, bytes_read);
            }
            resultOutStream.flush();
            resultOutStream.close();
            resultInStream.close();
        } catch (Exception e) {
            log.error("Unable to read and send the DICOM dataset page", e);
        }
    }
}
