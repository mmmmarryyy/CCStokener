package v201203.reportservice;

import com.google.api.ads.dfp.lib.DfpService;
import com.google.api.ads.dfp.lib.DfpServiceLogger;
import com.google.api.ads.dfp.lib.DfpUser;
import com.google.api.ads.dfp.v201203.ExportFormat;
import com.google.api.ads.dfp.v201203.ReportServiceInterface;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * This example downloads a completed report. To run a report, run
 * RunDeliveryReportExample.java. To use the
 * {@link com.google.api.ads.dfp.lib.utils.v201203.ReportUtils} class, see
 * RunAndDownloadReport.java under /examples/v201203/utils.
 *
 * Tags: ReportService.getReportDownloadURL
 *
 * @author api.arogal@gmail.com (Adam Rogal)
 */
public class DownloadReportExample {

    public static void main(String[] args) throws Exception {
        DfpServiceLogger.log();
        DfpUser user = new DfpUser();
        ReportServiceInterface reportService = user.getService(DfpService.V201203.REPORT_SERVICE);
        Long reportJobId = Long.parseLong("INSERT_REPORT_JOB_ID_HERE");
        String folderPath = "/path/to/folder";
        ExportFormat exportFormat = ExportFormat.CSV;
        String filePath = folderPath + File.separator + "report-" + System.currentTimeMillis() + "." + exportFormat.toString().toLowerCase() + ".gz";
        System.out.print("Downloading report to " + filePath + "...");
        String downloadUrl = reportService.getReportDownloadURL(reportJobId, exportFormat);
        downloadFile(downloadUrl, filePath);
        System.out.println("done.");
    }

    /**
   * Writes the contents of the URL to the file path.
   *
   * @param url the URL locating the report XML
   * @param filePath the file path to write to
   * @throws IOException if an I/O error occurs
   */
    public static void downloadFile(String url, String filePath) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        try {
            int i = 0;
            while ((i = inputStream.read()) != -1) {
                bos.write(i);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
    }
}
