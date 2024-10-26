package org.buginese.fetcher.jira;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import com.atlassian.jira.rpc.soap.client.RemoteAttachment;

public class AttachmentsRetriever {

    static RemoteAttachment[] attachments;

    static File actual;

    static String attachmentAuthor;

    static String attachmentCreated;

    static String attachmentFileName;

    static Long attachmentFileSize;

    static String attachmentId;

    static String attachmentMimeType;

    static String attachmentPath;

    static SimpleDateFormat formatter;

    static int serverCode;

    static HttpURLConnection urlConnection;

    public static void retrieveAttachments(RemoteAttachment[] attachments, String id, String projectName, String key, SimpleDateFormat formatter, java.sql.Connection connect) {
        if (attachments.length != 0) {
            for (RemoteAttachment attachment : attachments) {
                attachmentAuthor = attachment.getAuthor();
                if (attachment.getCreated() != null) {
                    attachmentCreated = formatter.format(attachment.getCreated().getTime());
                }
                attachmentFileName = attachment.getFilename();
                attachmentFileSize = attachment.getFilesize();
                attachmentId = attachment.getId();
                attachmentMimeType = attachment.getMimetype();
                if (attachmentMimeType.startsWith("text")) {
                    URL attachmentUrl;
                    try {
                        attachmentUrl = new URL("https://issues.apache.org/jira/secure/attachment/" + attachmentId + "/" + attachmentFileName);
                        urlConnection = (HttpURLConnection) attachmentUrl.openConnection();
                        urlConnection.connect();
                        serverCode = urlConnection.getResponseCode();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (serverCode == 200) {
                        actual = new File("../attachments/" + projectName + "/" + key);
                        if (!actual.exists()) {
                            actual.mkdirs();
                        }
                        attachmentPath = "../attachments/" + projectName + "/" + key + "/" + attachmentFileName;
                        BufferedInputStream bis;
                        try {
                            bis = new BufferedInputStream(urlConnection.getInputStream());
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(attachmentPath));
                            byte[] b = new byte[1024];
                            int len = -1;
                            while ((len = bis.read(b)) != -1) {
                                if (len == 1024) {
                                    bos.write(b);
                                } else {
                                    bos.write(b, 0, len);
                                }
                            }
                            bos.close();
                            bis.close();
                            insertAttachment(connect, id);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static int retrieveAttachments(RemoteAttachment[] attachments) {
        int screenShotNumber = 0;
        if (attachments.length != 0) {
            for (RemoteAttachment attachment : attachments) {
                attachmentMimeType = attachment.getMimetype();
                if ((attachmentMimeType.startsWith("image"))) {
                    ++screenShotNumber;
                }
            }
        }
        return screenShotNumber;
    }

    public static void insertAttachment(java.sql.Connection connect, String id) throws SQLException {
        String insertAttachment_query = "insert ignore into attachments " + "(id,issueid,name,author,created,path,size,mimetype) values " + "(?,?,?,?,?,?,?,?)";
        PreparedStatement stmt2 = connect.prepareStatement(insertAttachment_query);
        stmt2.setString(1, attachmentId);
        stmt2.setString(2, id);
        stmt2.setString(3, attachmentFileName);
        stmt2.setString(4, attachmentAuthor);
        stmt2.setString(5, attachmentCreated);
        stmt2.setString(6, attachmentPath);
        stmt2.setLong(7, attachmentFileSize);
        stmt2.setString(8, attachmentMimeType);
        stmt2.executeUpdate();
    }
}
