package com.plato.etoh.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import com.plato.etoh.client.model.ApplicationDTO;
import com.plato.etoh.client.model.Comment;
import com.plato.etoh.client.model.YoneticiBaseInterface;

public class MailUtil {

    public static String etohum_dunya = "\n\nEtohum.com dünyası.\n" + "Etohum facebook grubuna katılmak için " + "http://twshot.com/?2B1\n" + "Etohum Mixxt grubu " + "http://twshot.com/?2B2\n" + "Etohum TV'yi izlemek için " + "http://www.etohum.tv\n" + "Ethoum blogu ... " + "http://www.etohum.com/blog/\n";

    public static void sendNewUserMail(String username, String email) {
        String body = "E-tohum hesabınız oluşturulmuştur. " + "\nHesabınıza giriş yapmak için basvuru2010.etohum.com adresinini kullanabilirsiniz." + "\nKullanici adınız:" + username + etohum_dunya + "\n\nE-tohum Ekibi";
        String title = null;
        try {
            title = URLEncoder.encode("Hoşgeliniz -- Etohum hesabınız açılmıştır", "UTF-8");
            body = URLEncoder.encode(body, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mailConnector(body, title, email);
    }

    public static void sendNewCommentMail(YoneticiBaseInterface yonetici, Comment comment, ApplicationDTO app, YoneticiBaseInterface owner) {
        String body = "Takibe aldığınız " + app.getId() + " nolu başvuruya yorum yapılmıştır.\n\n" + "Yorumu yapan " + owner.getEmail() + "\n" + "Yorum " + comment.getCommentText() + "\n\n" + "E-tohum Ekibi";
        String title = null;
        try {
            title = URLEncoder.encode("Etohum - " + app.getId() + " nolu başvuruya yeni yorum yapıldı", "UTF-8");
            body = URLEncoder.encode(body, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mailConnector(body, title, yonetici.getEmail());
    }

    public static void sendRecoverPassword(String username, String email, int pass) {
        String body = "E-tohum hesanınız için şifre hatırlatma isteğinde bulunulmuştur. " + "\nHesabınıza geçici şifrenizle http://basvuru2010.etohum.com adresinden ulaşabilirsiniz." + "\nKullanici adınız:" + username + "\nGeçici Şifre:" + pass + etohum_dunya + "\n\nE-tohum Ekibi";
        String title = null;
        try {
            title = URLEncoder.encode("Şifre Hatırlatma", "UTF-8");
            body = URLEncoder.encode(body, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        MailUtil.mailConnector(body, title, email);
    }

    private static void mailConnector(String msgBody, String title, String email) {
        String data = "to=" + email + "&title=" + title + "&message=" + msgBody;
        try {
            URL url = new URL("http://interrailmap.com:8080/MailUtil6/MailAt");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            StringBuffer answer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            writer.close();
            reader.close();
            System.out.println(answer.toString());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void mailGonderGeneric(String email, String msgBody, String subject) {
        String title = null;
        String body = null;
        try {
            title = URLEncoder.encode(subject, "UTF-8");
            body = URLEncoder.encode(msgBody, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mailConnector(body, title, email);
    }
}
