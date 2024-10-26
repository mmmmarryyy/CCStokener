package com.saret.crawler.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.saret.crawler.parser.PdfTextParser;
import com.saret.utils.FileLocator;
import com.saret.utils.UtfFileHandle;
import org.htmlparser.parserapplications.StringExtractor;
import java.io.*;
import java.net.URL;
import java.util.logging.Logger;

/**
 * User: biniam.gebremichael
 * Date: Nov 17, 2008
 */
@SuppressWarnings({ "ALL" })
@Singleton
public class DownloadPage {

    private static final Logger logger = Logger.getLogger(DownloadPage.class.getName());

    @Inject
    public DownloadPage() {
        UtfFileHandle.cleanDirectory(FileLocator.getWorkingDir());
    }

    public File download(String address, String fileName, String type) {
        if (type.equals("html")) {
            return downloadAsText(address, fileName);
        } else if (type.equals("pdf")) {
            return downloadPdfAsText(address, fileName);
        } else {
            return downloadAny(address, fileName);
        }
    }

    public File downloadPdfAsText(String address, String fileName) {
        File pdfFile = downloadAny(address, fileName + ".pdf");
        File textFile = new File(FileLocator.getConvertedDir(), fileName + ".txt");
        PdfTextParser.pdf2Text(pdfFile, textFile);
        return textFile;
    }

    private File downloadAny(String address, String fileName) {
        File localFileName = new File(FileLocator.getDownloadDir(), fileName);
        OutputStream out = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            in = url.openConnection().getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
        } catch (Exception exception) {
            logger.warning(exception.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                logger.warning(ioe.getMessage());
            }
        }
        File completed = new File(FileLocator.getCompletedDir(), localFileName.getName());
        localFileName.renameTo(completed);
        logger.info(String.format("%s --> %s (%.1fKB)", address, localFileName, (double) (completed.length() / 1024)));
        return completed;
    }

    public File downloadAsText(String address, String fileName) {
        File localFileName = new File(FileLocator.getDownloadDir(), fileName + ".txt");
        StringExtractor se = new StringExtractor(address);
        try {
            String value = se.extractStrings(true);
            if (value.contains("org.htmlparser.util.ParserException")) {
                logger.warning(address + " does not exits " + value);
            } else {
                UtfFileHandle.write(localFileName, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info(String.format("%s --> %s (%.1fKB)", address, localFileName, (double) (localFileName.length() / 1024)));
        File dest = new File(FileLocator.getConvertedDir(), localFileName.getName());
        localFileName.renameTo(dest);
        return dest;
    }
}
