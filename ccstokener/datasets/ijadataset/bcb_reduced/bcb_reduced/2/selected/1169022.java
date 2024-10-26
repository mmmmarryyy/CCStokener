package org.azrul.mewit.client;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import com.wavemaker.runtime.server.ParamName;
import com.wavemaker.runtime.server.DownloadResponse;
import java.io.FileInputStream;
import org.azrul.epice.domain.FileRepository;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.domain.Attachment;
import org.azrul.epice.rest.dto.UploadFileRequest;
import org.azrul.epice.rest.dto.UploadFileResponse;
import com.wavemaker.runtime.RuntimeAccess;
import java.util.Properties;

/**
 * This is a client-facing service class.  All
 * public methods will be exposed to the client.  Their return
 * values and parameters will be passed to the client or taken
 * from the client, respectively.  This will be a singleton
 * instance, shared between all requests. 
 * 
 * To log, call the superclass method log(LOG_LEVEL, String) or log(LOG_LEVEL, String, Exception).
 * LOG_LEVEL is one of FATAL, ERROR, WARN, INFO and DEBUG to modify your log level.
 * For info on these levels, look for tomcat/log4j documentation
 */
public class FileServices extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public FileServices() {
        super(INFO);
    }

    public void doSomething(FileRepository fileRepository, Attachment attachment) {
    }

    public String doUpload(@ParamName(name = "file") MultipartFile file, @ParamName(name = "uploadDirectory") String _uploadDirectory) throws IOException {
        String sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        String tempUploadDir = MewitProperties.getTemporaryUploadDirectory();
        if (!tempUploadDir.endsWith("/") && !tempUploadDir.endsWith("\\")) {
            tempUploadDir += "\\";
        }
        String fileName = null;
        int position = file.getOriginalFilename().lastIndexOf(".");
        if (position <= 0) {
            fileName = java.util.UUID.randomUUID().toString();
        } else {
            fileName = java.util.UUID.randomUUID().toString() + file.getOriginalFilename().substring(position);
        }
        File outputFile = new File(tempUploadDir, fileName);
        log(INFO, "writing the content of uploaded file to: " + outputFile);
        FileOutputStream fos = new FileOutputStream(outputFile);
        IOUtils.copy(file.getInputStream(), fos);
        file.getInputStream().close();
        fos.close();
        return doUploadFile(sessionId, outputFile, file.getOriginalFilename());
    }

    public DownloadResponse doDownload(@ParamName(name = "filename") String filename) throws IOException {
        DownloadResponse ret = new DownloadResponse();
        File localFile = new File(MewitProperties.getTemporaryUploadDirectory(), filename);
        FileInputStream fis = new FileInputStream(localFile);
        ret.setContents(fis);
        ret.setFileName(filename);
        return ret;
    }

    public DownloadResponse doDownloadFile(Attachment attachment) throws IOException {
        DownloadResponse ret = new DownloadResponse();
        File localFile = new File(attachment.getFilePath());
        FileInputStream fis = new FileInputStream(localFile);
        log(INFO, "File path=" + attachment.getFilePath());
        log(INFO, "Exist =" + localFile.exists());
        ret.setContents(fis);
        ret.setFileName(attachment.getFileName());
        return ret;
    }

    private String doUploadFile(String sessionId, File file, String originalFileName) throws UnsupportedEncodingException, IOException {
        log(INFO, sessionId);
        log(INFO, file.getPath());
        DefaultHttpClient httpclient = new DefaultHttpClient();
        UploadFileRequest request = new UploadFileRequest();
        request.setSessionId(sessionId);
        request.setDirectory(UUID.randomUUID().toString());
        request.setFileName(originalFileName);
        URL url = file.toURI().toURL();
        URLConnection urlCon = url.openConnection();
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("UploadFileRequest", UploadFileRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("UploadFileResponse", UploadFileResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/uploadFile?REQUEST=" + strRequest);
        FileEntity fileEntity = new FileEntity(file, urlCon.getContentType());
        httppost.setEntity(fileEntity);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            log(INFO, result);
            UploadFileResponse oResponse = (UploadFileResponse) reader.fromXML(result);
            return oResponse.getFile();
        }
        return null;
    }
}
