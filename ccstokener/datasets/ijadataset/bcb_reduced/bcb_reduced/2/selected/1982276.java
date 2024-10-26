package com.skruk.elvis.applets;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * DOCUMENT ME!
 *
 * @author     skruk
 * @created    24 wrzesień 2003
 */
public class Image extends javax.swing.JApplet {

    /** Description of the Field */
    private String sIdImage = null;

    /** Description of the Field */
    private String sHashImage = null;

    /** Description of the Field */
    private String sJsessionId = null;

    /** Description of the Field */
    private String sColor = null;

    /** Description of the Field */
    private Color cColor = null;

    /** Description of the Field */
    private ImageIcon iiImage = null;

    /** Description of the Field */
    private String sUrl = null;

    /** Description of the Field */
    private String sToolTip = null;

    /** Description of the Field */
    private com.skruk.elvis.beans.ImagePanel imagePanel = null;

    /** Initializes the applet Image */
    public void init() {
        this.sColor = this.getParameter("bgColor");
        if (this.sColor != null) {
            this.cColor = this.getColor(this.sColor);
        } else {
            this.cColor = Color.WHITE;
        }
        this.setBackground(this.cColor);
        this.sJsessionId = this.getParameter("jsessionid");
        this.sIdImage = this.getParameter("idImage");
        this.sHashImage = this.getParameter("hashImage");
        this.sUrl = this.getParameter("url");
        this.sToolTip = this.getParameter("toolTip");
        this.loadImage();
        if (this.sToolTip != null) {
            imagePanel = new com.skruk.elvis.beans.ImagePanel();
        } else {
            imagePanel = new com.skruk.elvis.beans.ImagePanel();
        }
        imagePanel.setOpaque(true);
        getContentPane().add(imagePanel, java.awt.BorderLayout.CENTER);
        this.invalidate();
        imagePanel.invalidate();
    }

    /** Description of the Method */
    protected void loadImage() {
        java.net.URL urlImage = null;
        StringBuffer sb = new StringBuffer(this.sUrl);
        if (this.sIdImage != null) {
            sb.append("/servlet/ShowImage?id=").append(this.sIdImage);
        } else if (this.sHashImage != null) {
            sb.append("/servlet/ShowImage?hash=").append(this.sHashImage);
        }
        sb.append("&w=").append(this.getWidth()).append("&h=").append(this.getHeight());
        try {
            urlImage = new java.net.URL(sb.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (urlImage != null) {
            java.net.HttpURLConnection huc = null;
            InputStream inS = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1000];
            int len = 0;
            try {
                huc = (java.net.HttpURLConnection) urlImage.openConnection();
                huc.setRequestProperty("Cookie", "JSESSIONID=" + this.sJsessionId);
                inS = huc.getInputStream();
                while ((len = inS.read(buffer)) >= 0) {
                    baos.write(buffer, 0, len);
                }
                inS.close();
                java.awt.Image image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
                this.iiImage = new ImageIcon(image);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
        if (iiImage != null) {
            java.awt.Image img = null;
            img = this.iiImage.getImage();
            if ((this.getHeight() * img.getWidth(this)) < (this.getWidth() * img.getHeight(this))) {
                this.iiImage = new ImageIcon(img.getScaledInstance(-1, this.getHeight(), java.awt.Image.SCALE_SMOOTH));
            } else {
                this.iiImage = new ImageIcon(img.getScaledInstance(this.getWidth(), -1, java.awt.Image.SCALE_SMOOTH));
            }
        }
    }

    /**
	 * Przetwarza kolor zapisany w formacie RRGGBB na obiekt klasy <code>Color</code>
	 *
	 * @param  col  Reprezentaca RRGGBB koloru
	 * @return      Odpowiadający podanemu opisowi obiekt klasy <code>Color</code>
	 */
    Color getColor(String col) {
        int iCol = 0;
        int mul = 1;
        char[] cols = new char[6];
        col.getChars(0, 6, cols, 0);
        for (int i = 5; i >= 0; i--) {
            int step = 0;
            if ((cols[i] == 'A') || (cols[i] == 'B') || (cols[i] == 'C') || (cols[i] == 'D') || (cols[i] == 'E') || (cols[i] == 'F')) {
                step = cols[i] - 'A' + 10;
            } else if ((cols[i] == 'a') || (cols[i] == 'b') || (cols[i] == 'c') || (cols[i] == 'd') || (cols[i] == 'e') || (cols[i] == 'f')) {
                step = cols[i] - 'a' + 10;
            } else {
                step = cols[i] - '0';
            }
            iCol += (mul * step);
            mul *= 16;
        }
        return new Color(iCol);
    }
}
