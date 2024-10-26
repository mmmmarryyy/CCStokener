package org.dcm4chee.xero.wado;

import static org.dcm4chee.xero.wado.WadoParams.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dcm4che2.util.CloseUtils;
import org.dcm4chee.xero.metadata.servlet.ServletResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns the object as read from the given URL
 * 
 * @author bwallace
 */
public class UrlServletResponseItem implements ServletResponseItem {

    private static final Logger log = LoggerFactory.getLogger(UrlServletResponseItem.class);

    URL url;

    String contentType;

    String filename;

    private Boolean memoryMap = null;

    int bufSize = 64 * 1024;

    /** Record the URL for playback */
    public UrlServletResponseItem(URL url, String contentType, String filename) {
        this.url = url;
        this.contentType = contentType;
        this.filename = filename;
    }

    /** Write the contents from the given URL to the servlet response */
    public void writeResponse(HttpServletRequest arg0, HttpServletResponse response) throws IOException {
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            log.warn("No response found for request.");
            return;
        }
        if (contentType != null) response.setContentType(contentType);
        if (filename != null) response.setHeader(CONTENT_DISPOSITION, "attachment;filename=" + filename);
        InputStream is;
        String surl = url.toString();
        long fileSize;
        OutputStream os = response.getOutputStream();
        if (surl.startsWith("file:")) {
            String fileName = url.getFile();
            File file = new File(fileName);
            fileSize = file.length();
            response.setContentLength((int) fileSize);
            if (memoryMap == null || memoryMap == false) {
                log.info("Using stream file {} of size {}", fileName, fileSize);
                streamFile(os, new FileInputStream(file), (int) Math.min(fileSize, bufSize));
            } else {
                log.info("Using memory mapped file {} of size {}", fileName, fileSize);
                memoryMapFile(os, fileName, bufSize);
            }
        } else {
            URLConnection conn = url.openConnection();
            log.info("Reading from URL connection " + surl);
            fileSize = conn.getContentLength();
            is = conn.getInputStream();
            if (fileSize > 0) {
                log.info("Returning {} bytes for file {}", fileSize, url);
                response.setContentLength((int) fileSize);
                bufSize = (int) Math.min(fileSize, bufSize);
            } else {
            }
            streamFile(os, is, bufSize);
        }
    }

    /**
	 * Sends a memory mapped file to the given output stream.
	 * 
	 * @param os
	 * @param fileName
	 * @param bufSize
	 * @throws IOException
	 */
    public static void memoryMapFile(OutputStream os, String fileName, int bufSize) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel fc = fis.getChannel();
        ByteBuffer bb = ByteBuffer.allocate(32 * 1024);
        try {
            int s = -1;
            while ((s = fc.read(bb)) > 0) {
                os.write(bb.array(), 0, s);
                bb.clear();
            }
        } finally {
            CloseUtils.safeClose(fc);
            CloseUtils.safeClose(fis);
            CloseUtils.safeClose(os);
        }
        return;
    }

    /** Streams the input stream to the output stream, reading bufSize elements at a time.
	 * 
	 * @param os Stream to write to
	 * @param is Stream to read from
	 * @param bufSize Size of the read buffer
	 * @throws IOException Thrown if the streams are crossed.
	 */
    public static void streamFile(OutputStream os, InputStream is, int bufSize) throws IOException {
        byte[] data = new byte[bufSize];
        try {
            int size = -1;
            while ((size = is.read(data)) > 0) os.write(data, 0, size);
        } finally {
            os.flush();
            CloseUtils.safeClose(os);
            CloseUtils.safeClose(is);
        }
    }

    public Boolean getMemoryMap() {
        return memoryMap;
    }

    public void setMemoryMap(Boolean memoryMap) {
        this.memoryMap = memoryMap;
    }

    public int getBufSize() {
        return bufSize;
    }

    public void setBufSize(int bufSize) {
        this.bufSize = bufSize;
    }
}
