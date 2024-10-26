package com.jjcp;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

public class ImageViewer extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel jContentPane = null;

    private String addressURL = null;

    private JScrollPane jScrollPane = null;

    private float zoom = 1;

    private ImageIcon imageIcon;

    private boolean imageDownload = false;

    /**
	 * This is the default constructor
	 */
    public ImageViewer(ImageIcon icon) {
        super();
        this.zoom = zoom;
        this.imageIcon = icon;
        this.imageDownload = true;
        initialize();
    }

    public ImageViewer(String url, float zoom) {
        super();
        this.addressURL = url;
        this.zoom = zoom;
        initialize();
    }

    public ImageViewer(String url) {
        super();
        this.addressURL = url;
        initialize();
    }

    public ImageViewer(String url, int x, int y) {
        super();
        this.addressURL = url;
        initialize();
        this.setLocation(x, y);
    }

    public void setLocationXY(int x, int y) {
        try {
            this.setLocation(x, y);
        } catch (Exception e) {
        }
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(400, 600);
        this.setTitle(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMAGE_VIEW"));
        this.setMinimumSize(new Dimension(50, 50));
        this.setContentPane(getJContentPane());
        this.setTitle(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMAGE_VIEW"));
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            if (imageDownload) jContentPane.add(getJScrollPane2(), BorderLayout.CENTER); else jContentPane.add(getJScrollPane(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    public void setZoom(float zoomFinal) {
        Component[] comps = (Component[]) getJScrollPane().getComponents();
        JViewport viewPort = (JViewport) comps[0];
        JLabel jLabel = (JLabel) viewPort.getComponent(0);
        ImageIcon imageIcon = (ImageIcon) jLabel.getIcon();
        BufferedImage buffImage = (BufferedImage) imageIcon.getImage();
        BufferedImage bufImage1 = ImagePanel.resizeAnImage(buffImage, zoomFinal);
        ImageIcon icon = new ImageIcon(bufImage1);
        JLabel jLabelImage = new JLabel(icon);
        if (icon != null) {
            jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE."));
        } else {
            jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE_CAN_NOT_BE_SHOWED."));
        }
        jLabelImage.setForeground(Color.blue);
        jLabelImage.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelImage.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabelImage.setVerticalTextPosition(SwingConstants.BOTTOM);
        jLabelImage.setVerticalAlignment(SwingConstants.CENTER);
        jLabelImage.setFont(new Font(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("LUCIDA_GRANDE"), Font.PLAIN, 10));
        if (icon != null) {
            jLabelImage.setPreferredSize(new Dimension(icon.getIconWidth() + 10, icon.getIconHeight() + 10));
            jScrollPane = new JScrollPane(jLabelImage);
            jScrollPane.setSize(new Dimension(icon.getIconWidth() + 20, icon.getIconHeight() + 20));
            this.setSize(new Dimension(icon.getIconWidth() + 30, icon.getIconHeight() + 50));
        } else {
            jScrollPane = new JScrollPane(jLabelImage);
        }
        validate();
        repaint();
        System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMAGEN_ACTUALIZADA,_ZOOM:_") + zoomFinal);
    }

    /**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
    public JScrollPane getJScrollPane2() {
        if (jScrollPane == null) {
            try {
                JLabel jLabelImage = new JLabel(imageIcon);
                if (imageIcon != null) jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE")); else jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE_CAN_NOT_BE_SHOWED."));
                jLabelImage.setForeground(Color.blue);
                jLabelImage.setHorizontalAlignment(SwingConstants.CENTER);
                jLabelImage.setHorizontalTextPosition(SwingConstants.CENTER);
                jLabelImage.setVerticalTextPosition(SwingConstants.BOTTOM);
                jLabelImage.setVerticalAlignment(SwingConstants.CENTER);
                jLabelImage.setFont(new Font(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("LUCIDA_GRANDE"), Font.PLAIN, 10));
                if (imageIcon != null) {
                    jLabelImage.setPreferredSize(new Dimension(imageIcon.getIconWidth() + 10, imageIcon.getIconHeight() + 10));
                    jScrollPane = new JScrollPane(jLabelImage);
                    jScrollPane.setSize(new Dimension(imageIcon.getIconWidth() + 20, imageIcon.getIconHeight() + 20));
                    this.setSize(new Dimension(imageIcon.getIconWidth() + 30, imageIcon.getIconHeight() + 50));
                } else jScrollPane = new JScrollPane(jLabelImage);
            } catch (Exception e) {
                e.printStackTrace();
                JLabel jLabelImage = new JLabel(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE_CAN_NOT_BE_SHOWED."));
                jLabelImage.setForeground(Color.blue);
                jLabelImage.setHorizontalAlignment(SwingConstants.CENTER);
                jLabelImage.setHorizontalTextPosition(SwingConstants.CENTER);
                jLabelImage.setVerticalAlignment(SwingConstants.CENTER);
                jLabelImage.setVerticalTextPosition(SwingConstants.BOTTOM);
                jLabelImage.setFont(new Font(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("LUCIDA_GRANDE"), Font.PLAIN, 10));
                jLabelImage.setPreferredSize(new Dimension(100, 20));
                jScrollPane = new JScrollPane(jLabelImage);
            }
        }
        return jScrollPane;
    }

    public JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            ImageIcon icon = null;
            try {
                try {
                    URL url = new URL(addressURL);
                    URLConnection conn = null;
                    conn = url.openConnection();
                    InputStream in = conn.getInputStream();
                    in.close();
                    icon = new ImageIcon(url);
                    BufferedImage bufImage = ImageIO.read(url);
                    BufferedImage bufImage1 = ImagePanel.resizeAnImage(bufImage, zoom);
                    icon = new ImageIcon(bufImage1);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                JLabel jLabelImage = new JLabel(icon);
                if (icon != null) jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE.")); else jLabelImage.setText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE_CAN_NOT_BE_SHOWED."));
                jLabelImage.setForeground(Color.blue);
                jLabelImage.setHorizontalAlignment(SwingConstants.CENTER);
                jLabelImage.setHorizontalTextPosition(SwingConstants.CENTER);
                jLabelImage.setVerticalTextPosition(SwingConstants.BOTTOM);
                jLabelImage.setVerticalAlignment(SwingConstants.CENTER);
                jLabelImage.setFont(new Font(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("LUCIDA_GRANDE"), Font.PLAIN, 10));
                if (icon != null) {
                    jLabelImage.setPreferredSize(new Dimension(icon.getIconWidth() + 10, icon.getIconHeight() + 10));
                    jScrollPane = new JScrollPane(jLabelImage);
                    jScrollPane.setSize(new Dimension(icon.getIconWidth() + 20, icon.getIconHeight() + 20));
                    this.setSize(new Dimension(icon.getIconWidth() + 30, icon.getIconHeight() + 50));
                } else jScrollPane = new JScrollPane(jLabelImage);
            } catch (Exception e) {
                e.printStackTrace();
                JLabel jLabelImage = new JLabel(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECTED_IMAGE_CAN_NOT_BE_SHOWED."));
                jLabelImage.setForeground(Color.blue);
                jLabelImage.setHorizontalAlignment(SwingConstants.CENTER);
                jLabelImage.setHorizontalTextPosition(SwingConstants.CENTER);
                jLabelImage.setVerticalAlignment(SwingConstants.CENTER);
                jLabelImage.setVerticalTextPosition(SwingConstants.BOTTOM);
                jLabelImage.setFont(new Font(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("LUCIDA_GRANDE"), Font.PLAIN, 10));
                jLabelImage.setPreferredSize(new Dimension(100, 20));
                jScrollPane = new JScrollPane(jLabelImage);
            }
        }
        return jScrollPane;
    }

    public static void main(String[] args) {
        ImageViewer im = new ImageViewer(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTTP://127.0.0.1:8080/WADO?REQUESTTYPE=WADO&STUDYUID=1.2.840.113745.101000.1008000.38048.4626.5933732") + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=1.3.12.2.1107.5.1.4.36085.4.0.13457320123409917&OBJECTUID=1.3.12.2.1107.5.1.4.36085.4.0.13457333828791339"));
        im.setVisible(true);
    }
}
