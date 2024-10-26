package in.raster.oviyam.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.io.DataInputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Reads the Window Level and Window Width.
 * @author asgar
 * @version 0.7
 *
 */
public class DcmWindowLevel extends HttpServlet {

    /**
	 * Initialize the logger.
	 */
    private static Logger log = Logger.getLogger(DcmWindowLevel.class);

    private static final String WINDOW_CENTER_PARAM = "windowCenter";

    private static final String WINDOW_WIDTH_PARAM = "windowWidth";

    int[] dicomData = new int[10 * 1024];

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        int i;
        String dicomURL = request.getParameter("datasetURL");
        String contentType = request.getParameter("contentType");
        String studyUID = request.getParameter("studyUID");
        String seriesUID = request.getParameter("seriesUID");
        String objectUID = request.getParameter("objectUID");
        dicomURL += "&contentType=" + contentType + "&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID + "&transferSyntax=1.2.840.10008.1.2.1";
        dicomURL = dicomURL.replace("+", "%2B");
        InputStream is = null;
        DataInputStream dis = null;
        try {
            URL url = new URL(dicomURL);
            is = url.openStream();
            dis = new DataInputStream(is);
            for (i = 0; i < dicomData.length; i++) dicomData[i] = dis.readUnsignedByte();
            String windowCenter = getElementValue("00281050");
            String windowWidth = getElementValue("00281051");
            request.getSession(true).setAttribute(WINDOW_CENTER_PARAM, windowCenter == null ? null : windowCenter.trim());
            request.getSession(true).setAttribute(WINDOW_WIDTH_PARAM, windowWidth == null ? null : windowWidth.trim());
            dis.skipBytes(50000000);
            is.close();
            dis.close();
            out.println("Success");
            out.close();
        } catch (Exception e) {
            log.error("Unable to read and send the DICOM dataset page", e);
        }
    }

    private String getElementValue(String element) {
        int a, b, c, d;
        a = Integer.parseInt(element.substring(2, 4), 16);
        b = Integer.parseInt(element.substring(0, 2), 16);
        c = Integer.parseInt(element.substring(6, 8), 16);
        d = Integer.parseInt(element.substring(4, 6), 16);
        String val = "";
        int len;
        String ret_val[] = new String[2];
        for (int i = 0; i < dicomData.length; i++) {
            if (dicomData[i] == a && dicomData[i + 1] == b && dicomData[i + 2] == c && dicomData[i + 3] == d) {
                if (dicomData[i + 4] > 65) len = dicomData[i + 6]; else len = dicomData[i + 4];
                int m = i + 8 + len;
                for (int j = i + 8; j < m; j++) {
                    val += (char) dicomData[j];
                }
                break;
            }
        }
        ret_val = val.split("\\\\");
        return (ret_val[0]);
    }
}
