package com.aol.photoviewer;

import com.aol.services.AuraSerializer;
import com.aol.services.Serializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.Properties;
import java.lang.System;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class RefreshServlet extends HttpServlet {

    private ServletContext ctx = null;

    private Logger logger = null;

    public void init() {
        ctx = getServletContext();
        logger = (Logger) ctx.getAttribute("aol.logger");
    }

    public void destroy() {
        ctx = null;
        logger = null;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ServletOutputStream out = response.getOutputStream();
        Serializer serializer = new AuraSerializer();
        try {
            URL url = new URL("http://xml.channel.aol.com/xmlpublisher/fetch.v2.xml?id=304175");
            URLConnection conn = url.openConnection();
            HttpURLConnection c = (HttpURLConnection) conn;
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String readLine;
            String body = "";
            while (((readLine = br.readLine()) != null)) {
                body += readLine + "\n";
            }
            br.close();
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(body.getBytes()));
            doc.getDocumentElement().normalize();
            NodeList nl = doc.getElementsByTagName("imageurl");
            int len = nl.getLength();
            if (len > 0) {
                Object[] o = new Object[len];
                for (int i = 0; i < len; i++) {
                    Node eventNode = nl.item(i);
                    String imageurl = eventNode.getTextContent();
                    int lastSlash = imageurl.lastIndexOf('/');
                    String filename = imageurl.substring(lastSlash + 1);
                    int dot = filename.lastIndexOf('.');
                    String pngfilename = filename.substring(0, dot) + ".png";
                    o[i] = pngfilename;
                    Properties pp = new Properties();
                    pp = System.getProperties();
                    String catalina_home = System.getenv("CATALINA_HOME");
                    String fileseparator = pp.getProperty("file.separator");
                    String root = catalina_home + fileseparator + "webapps" + fileseparator + "photoviewer" + fileseparator + "bhi" + fileseparator + "content" + fileseparator + "images" + fileseparator;
                    String completeURL = root + pngfilename;
                    File f = new File(completeURL);
                    if (f.exists() == false) {
                        URL url2 = new URL(imageurl);
                        URLConnection conn2 = url2.openConnection();
                        HttpURLConnection c2 = (HttpURLConnection) conn2;
                        BufferedImage bsrc = ImageIO.read(c2.getInputStream());
                        int preferWidth = 320;
                        int preferHeight = 170;
                        int srcWidth = bsrc.getWidth();
                        int srcHeight = bsrc.getHeight();
                        double widthScaleFactor = (double) preferWidth / srcWidth;
                        double heightScaleFactor = (double) preferHeight / srcHeight;
                        double scaleFactor = (widthScaleFactor * srcHeight > preferHeight) ? heightScaleFactor : widthScaleFactor;
                        int destWidth = (int) (srcWidth * scaleFactor);
                        int destHeight = (int) (srcHeight * scaleFactor);
                        BufferedImage bdest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = bdest.createGraphics();
                        AffineTransform at = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
                        g.drawRenderedImage(bsrc, at);
                        ImageIO.write(bdest, "png", f);
                    }
                }
                serializer.write("Detail0", o);
            }
            nl = doc.getElementsByTagName("caption");
            len = nl.getLength();
            if (len > 0) {
                Object[] o = new Object[len];
                for (int i = 0; i < len; i++) {
                    o[i] = nl.item(i).getTextContent().replaceAll(",", "@#44#").replaceAll("\\<.*?>", "");
                }
                serializer.write("Topic0", o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        serializer.write("Status", "true").write("Err", "");
        out.write(serializer.flush().getBytes());
        return;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }
}
