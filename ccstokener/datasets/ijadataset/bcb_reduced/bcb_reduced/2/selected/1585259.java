package edu.utah.cs.cs4960.climate.hadoop.name;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Downloader {

    public static final String FTP_FORMAT = "ftp://%s:%s@%s/%s;type=i";

    public Downloader() {
    }

    public static void downloadFromFtp(String user, String password, String folder, String file, String destination) {
        try {
            FileOutputStream out = new FileOutputStream(new File(destination));
            URL url = new URL(String.format(FTP_FORMAT, user, password, folder, file));
            URLConnection connection = url.openConnection();
            InputStream input = connection.getInputStream();
            int b = input.read();
            while (b != -1) out.write(b);
        } catch (Exception e) {
        }
    }

    public static void addToHadoop(File file) {
    }

    public static void sendEmail() {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", "mailgate.eng.utah.edu");
            Session session = Session.getDefaultInstance(properties, null);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("danner@eng.utah.edu"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("danner@eng.utah.edu"));
            message.setSubject("Hello JavaMail");
            message.setText("Welcome to JavaMail");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        sendEmail();
    }
}
