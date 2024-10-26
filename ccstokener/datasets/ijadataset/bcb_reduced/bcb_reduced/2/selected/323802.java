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
public class MultiFrames extends HttpServlet {

    /**
	 * Initialize the logger.
	 */
    private static Logger log = Logger.getLogger(MultiFrames.class);

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
            String noOfFrames = getElementValue("00280008").trim();
            String frameTime = getElementValue("00181063");
            String frameTimeVector = "";
            if (frameTime != "") {
                if (frameTime.indexOf(".") > 0) frameTime = frameTime.substring(0, frameTime.indexOf("."));
                for (int x = 0; x < Integer.parseInt(noOfFrames); x++) frameTimeVector = frameTimeVector + frameTime + ":";
            } else {
                frameTimeVector = getElementValue("00181065");
            }
            dis.skipBytes(50000000);
            is.close();
            dis.close();
            out.println(frameTimeVector);
            out.close();
        } catch (Exception e) {
            log.error("Unable to read multiframe dicom elements", e);
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
        return (val.replace("\\", ":"));
    }
}
