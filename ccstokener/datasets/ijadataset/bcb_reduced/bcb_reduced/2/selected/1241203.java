package org.bpaul.rtalk.protocol;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class Avatar {

    private String keyurl;

    private String avatarimgurl;

    private String username;

    private String login;

    private String session;

    private String secret;

    private String key;

    private BufferedImage img;

    public Avatar(String keyurl, String avatarimgurl, String username, String login, String session, String secret) {
        this.keyurl = keyurl;
        this.avatarimgurl = avatarimgurl;
        this.username = username;
        this.login = login;
        this.session = session;
        this.secret = secret;
    }

    public void fetchKey() throws IOException {
        String strurl = MessageFormat.format(keyurl, new Object[] { username, secret, login, session });
        StringBuffer result = new StringBuffer();
        BufferedReader reader = null;
        URL url = null;
        try {
            url = new URL(strurl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
            }
        }
        Pattern p = Pattern.compile("<key>(.*)</key>");
        Matcher m = p.matcher(result.toString());
        if (m.matches()) {
            this.key = m.group(1);
        }
    }

    public void fetchImg() throws IOException {
        String strurl = MessageFormat.format(avatarimgurl, new Object[] { username, key });
        URL url = new URL(strurl);
        this.img = ImageIO.read(url);
    }

    public BufferedImage getImg() {
        return img;
    }

    public String getKey() {
        return key;
    }

    public String getUsername() {
        return username;
    }
}
