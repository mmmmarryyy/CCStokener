package com.google.api.adwords.lib.utils;

import com.google.api.adwords.lib.AdWordsService;
import com.google.api.adwords.lib.AdWordsUser;
import com.google.api.adwords.v13.ReportInterface;
import com.google.api.adwords.v13.ReportJobStatus;
import com.google.api.adwords.v13.ReportService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

/**
 * Retrieves reports using an {@link AdWordsUser} or an already created
 * {@link ReportService}.
 * <p>
 * There are two main functions of this class:
 * <ul>
 * <li>To download a report in Gzip or plain-text format to a file or any
 * {@code OutputStream}</li>
 * <li>To get the report XML and perform a number of tasks on that XML</li>
 * </ul>
 * <p>
 * {@code ReportUtils} also provides the method
 * {@link #whenReportReady(ReportCallback)} to wait for a scheduled report to
 * finish processing before taking an action on the report through the
 * supplied {@link ReportCallback}.
 *
 * @author api.arogal@gmail.com (Adam Rogal)
 */
public class ReportUtils {

    /** The time to sleep before each request to the service. */
    public static final int SLEEP_TIMER = 30000;

    /** The charset for the report XML. */
    private static final Charset REPORT_XML_CHARSET = Charset.forName("UTF-8");

    private final ReportInterface reportService;

    private final long reportJobId;

    /**
   * Constructs a {@code ReportUtils} object for a {@link AdWordsUser} and
   * a report job id that the the class works on.
   *
   * @param user the {@code AdWordsUser} to use
   * @param reportJobId the report job ID
   * @throws ServiceException if ReportService could not be instantiated
   */
    public ReportUtils(AdWordsUser user, long reportJobId) throws ServiceException {
        this((ReportInterface) user.getService(AdWordsService.V13.REPORT_SERVICE), reportJobId);
    }

    /**
   * Constructs a {@code ReportUtils} object for a {@link ReportInterface} and
   * a report job id that the the class works on.
   *
   * @param reportService the ReportService stub to make calls to
   * @param reportJobId the report job ID
   */
    public ReportUtils(ReportInterface reportService, long reportJobId) {
        this.reportJobId = reportJobId;
        this.reportService = reportService;
    }

    /**
   * Waits for the report to be ready and then calls:
   * <ul>
   * <li>{@link ReportCallback#onSuccess()} for a successful scheduling</li>
   * <li>{@link ReportCallback#onFailure()} for a failed scheduling due to a
   * {@link ReportJobStatus#Failed}</li>
   * <li>{@link ReportCallback#onInterruption()} if the wait thread is
   * interrupted</li>
   * <li>{@link ReportCallback#onException(Exception)} if there was an
   * exception while waiting for the report to finish</li>
   * </ul>
   *
   * @param callback the {@code ReportCallback} to call when the job has
   *     finished, successfully or otherwise
   * @throws IllegalArgumentException if {@code callback == null}
   * @return the thread created that handles waiting for the report.
   *     {@link Thread#interrupt()} can be called on the returned thread to
   *     interrupt it.
   */
    public Thread whenReportReady(final ReportCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Report callback cannot be null.");
        }
        Thread waitThread = new Thread("ReportUtils.whenReportReady " + reportJobId) {

            @Override
            public void run() {
                try {
                    if (waitForReportReady()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure();
                    }
                } catch (RemoteException e) {
                    callback.onException(e);
                } catch (InterruptedException e) {
                    callback.onInterruption();
                } catch (RuntimeException e) {
                    callback.onException(e);
                }
            }
        };
        waitThread.start();
        return waitThread;
    }

    /**
   * Blocks and waits for a report to be ready. When a {@link ReportJobStatus}
   * is received that is not {@code ReportJobStatus#Pending} or
   * {@code ReportJobStatus#InProgress}, the report is considered finished, and
   * the method is returned with a {@code true} if the report was succesfful,
   * or an {@code false} if not.
   *
   * @return {@code true} if the report was succesful, {@code false} otherwise
   * @throws RemoteException if there was an error performing one of the SOAP
   *     calls
   * @throws InterruptedException if the thread was interrupted
   */
    public boolean waitForReportReady() throws RemoteException, InterruptedException {
        ReportJobStatus status = reportService.getReportJobStatus(reportJobId);
        while (status == ReportJobStatus.Pending || status == ReportJobStatus.InProgress) {
            Thread.sleep(SLEEP_TIMER);
            status = reportService.getReportJobStatus(reportJobId);
        }
        return status == ReportJobStatus.Completed;
    }

    /**
   * Downloads a Gzip or plain-text format report XML to file located at {@code
   * fileName}. This method uses 1 API unit to get the download URL.
   *
   * @param gzip {@code true} if a Gzipped report should be downloaded,
   *     plain-text otherwise
   * @param fileName the file location to download the report to
   * @throws IOException if there was an error performing any I/O action,
   *     including any SOAP calls
   * @throws IllegalStateException if the report is not ready to be downloaded
   */
    public void downloadReport(boolean gzip, String fileName) throws IOException {
        downloadReport(gzip, new FileOutputStream(fileName));
    }

    /**
   * Downloads a Gzip or plain-text format report XML to output stream indicated
   * by {@code outputStream}. This method uses 1 API unit to get the download
   * URL.
   *
   * @param gzip {@code true} if a Gzipped report should be downloaded,
   *     plain-text otherwise
   * @param outputStream the output stream to download the report to
   * @throws IOException if there was an error performing any I/O action,
   *     including any SOAP calls
   * @throws IllegalStateException if the report is not ready to be downloaded
   */
    public void downloadReport(boolean gzip, OutputStream outputStream) throws IOException {
        writeUrlContentsToStream(getDownloadUrl(gzip), outputStream);
    }

    /**
   * Gets the plain-text format report XML as a {@code String}. This method uses
   * 1 API unit to get the download URL. The method
   * {@link XmlUtils#getXmlDocument(String)} can be used to transform this
   * {@code String} into a {@link Document}.
   *
   * @return the plain-text format report XML as a {@code String}
   * @throws IOException if there was an error performing any I/O action,
   *     including any SOAP calls
   * @throws IllegalStateException if the report is not ready to be downloaded
   */
    public String getReportXml() throws IOException {
        return getGzipUrlStringContents(getDownloadUrl(true));
    }

    /**
   * Gets the download URL for a GZip or plain-text format report. This method
   * uses 1 API unit to get the download URL.
   *
   * @param gzip {@code true} if a Gzipped report should be downloaded,
   *     plain-text otherwise
   * @return the URL for the report download
   * @throws RemoteException if there was an error performing any Axis call
   * @throws IllegalStateException if the report is not ready to be downloaded
   */
    private String getDownloadUrl(boolean gzip) throws RemoteException {
        ReportJobStatus status = reportService.getReportJobStatus(reportJobId);
        if (status != ReportJobStatus.Completed) {
            throw new IllegalStateException("Report " + reportJobId + " must be completed before downloading. It is currently: " + status);
        }
        if (gzip) {
            return reportService.getGzipReportDownloadUrl(reportJobId);
        } else {
            return reportService.getReportDownloadUrl(reportJobId);
        }
    }

    /**
   * Gets the report contents of a URL that is assumed to be Gzipped encoded.
   *
   * @param url the URL locating the Gzipped report XML
   * @return the report contents of the URL
   * @throws IOException if an I/O error occurs
   */
    private String getGzipUrlStringContents(String url) throws IOException {
        return getInputStreamStringContents(new GZIPInputStream(new URL(url).openStream()));
    }

    /**
   * Gets the report contents of an input stream. This method assumes that
   * the stream's contents are encoded in UTF-8 which is the standard for
   * AdWords API reports.
   *
   * @param inputStream the {@code InputStream} containing the report contents
   * @return the report contents of the stream
   * @throws IOException if an I/O error occurs
   */
    private String getInputStreamStringContents(InputStream inputStream) throws IOException {
        BufferedReader br = null;
        try {
            StringBuilder xmlBuilder = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(inputStream, REPORT_XML_CHARSET));
            String xmlLine;
            while ((xmlLine = br.readLine()) != null) {
                xmlBuilder.append(xmlLine + "\n");
            }
            return xmlBuilder.toString();
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private void writeUrlContentsToStream(String url, OutputStream outputStream) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
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

    /**
   * Gets a serialized CSV document in the form of {@code List<String[]>}.
   * The first element in the list is the header. Only rows from the report are
   * included in the CSV. That is, the totals and grandTotals sections are
   * not included.
   *
   * @param xml the report XML
   * @return a serialized CSV document in the form of {@code List<String[]>}
   * @throws IOException if an I/O error occurs
   * @throws SAXException if the document has SAX decoding problems
   * @throws ParserConfigurationException if construction problems occur
   */
    public static List<String[]> getReportCsv(String xml) throws ParserConfigurationException, SAXException, IOException {
        List<String[]> csvData = new ArrayList<String[]>();
        Document doc = XmlUtils.getXmlDocument(xml);
        NodeList columns = doc.getElementsByTagName("column");
        String[] headerArray = new String[columns.getLength()];
        for (int i = 0; i < columns.getLength(); i++) {
            headerArray[i] = ((Element) columns.item(i)).getAttribute("name");
        }
        csvData.add(headerArray);
        NodeList rows = doc.getElementsByTagName("row");
        for (int i = 0; i < rows.getLength(); i++) {
            String[] rowData = new String[columns.getLength()];
            Element row = (Element) rows.item(i);
            for (int j = 0; j < headerArray.length; j++) {
                rowData[j] = row.getAttribute(headerArray[j]);
            }
            csvData.add(rowData);
        }
        return csvData;
    }
}
