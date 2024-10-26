package musicbox.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import musicbox.backend.comparators.AlbumComparator;
import musicbox.backend.comparators.ArtistComparator;
import musicbox.backend.Backend;
import noTalent.MusicOutputDesign;

/**
 *
 * @author Isaac Hammon
 */
public class SmallAlbumArt extends javax.swing.JDialog {

    private ImageIcon i;

    private static String artist;

    /**
     *
     */
    public static boolean googleSearched;

    private static int googleImageLocation = 0;

    private static Vector<String> googleImages;

    private static JLabel j;

    private static String custom;

    /** Creates new form SmallAlbumArt
     * @param parent
     * @param j
     * @param i
     * @param s 
     */
    @SuppressWarnings({ "static-access", "static-access" })
    public SmallAlbumArt(java.awt.Frame parent, ImageIcon i, String s, JLabel j) {
        super(parent);
        this.i = i;
        this.artist = s;
        this.j = j;
        initComponents();
        this.setIconImage(MusicBoxView.musicBoxIcon16.getImage());
        getContentPane().remove(jPanel1);
        getContentPane().remove(jPanel2);
        pack();
    }

    private static void appendToPlayList() {
        Vector<MusicOutputDesign> temp = (Vector<MusicOutputDesign>) MusicBoxView.allMusic.clone();
        Vector<MusicOutputDesign> append = new Vector<MusicOutputDesign>();
        int sel = MusicBoxView.jTabbedPane2.getSelectedIndex();
        if (sel == 0) {
            Collections.sort(temp, new ArtistComparator());
        } else {
            Collections.sort(temp, new AlbumComparator());
        }
        int loc = 0;
        while (loc != -1) {
            if (sel == 0) {
                loc = binarySearchArtist(temp, 0, temp.size());
            } else {
                loc = binarySearchAlbum(temp, 0, temp.size());
            }
            if (loc != -1) {
                append.add(temp.remove(loc));
            }
        }
        Collections.sort(append, new AlbumComparator());
        MusicBoxView.appendToPlayList(append);
    }

    private static int binarySearchArtist(Vector<MusicOutputDesign> v, int low, int high) {
        if (high < low) {
            return -1;
        }
        int mid = (high + low) / 2;
        String s = v.elementAt(mid).getArtist().replace("\\", "/");
        if (s.compareToIgnoreCase(artist) > 0) {
            return binarySearchArtist(v, low, mid - 1);
        } else if (s.compareToIgnoreCase(artist) < 0) {
            return binarySearchArtist(v, mid + 1, high);
        } else {
            return mid;
        }
    }

    private static int binarySearchAlbum(Vector<MusicOutputDesign> v, int low, int high) {
        if (high < low) {
            return -1;
        }
        int mid = (high + low) / 2;
        String s = v.elementAt(mid).getAlbum().replace("\\", "/");
        if (s.compareToIgnoreCase(artist) > 0) {
            return binarySearchAlbum(v, low, mid - 1);
        } else if (s.compareToIgnoreCase(artist) < 0) {
            return binarySearchAlbum(v, mid + 1, high);
        } else {
            return mid;
        }
    }

    private void googleImageSearch() {
        bottomShowing = true;
        googleSearched = true;
        googleImageLocation = 0;
        googleImages = new Vector<String>();
        custom = "";
        int r = JOptionPane.showConfirmDialog(this, "Customize google search?", "Google Search", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            custom = JOptionPane.showInputDialog("Custom Search", "");
        } else {
            custom = artist;
        }
        try {
            String u = "http://images.google.com/images?q=" + custom;
            if (u.contains(" ")) {
                u = u.replace(" ", "+");
            }
            URL url = new URL(u);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            BufferedReader readIn = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            googleImages.clear();
            String lin = new String();
            while ((lin = readIn.readLine()) != null) {
                while (lin.contains("href=\"/imgres?imgurl=")) {
                    while (!lin.contains(">")) {
                        lin += readIn.readLine();
                    }
                    String s = lin.substring(lin.indexOf("href=\"/imgres?imgurl="), lin.indexOf(">", lin.indexOf("href=\"/imgres?imgurl=")));
                    lin = lin.substring(lin.indexOf(">", lin.indexOf("href=\"/imgres?imgurl=")));
                    if (s.contains("&amp;") && s.indexOf("http://") < s.indexOf("&amp;")) {
                        s = s.substring(s.indexOf("http://"), s.indexOf("&amp;"));
                    } else {
                        s = s.substring(s.indexOf("http://"), s.length());
                    }
                    googleImages.add(s);
                }
            }
            readIn.close();
        } catch (Exception ex4) {
            MusicBoxView.showErrorDialog(ex4);
        }
        jButton1.setEnabled(false);
        getContentPane().remove(jLabel1);
        ImageIcon icon;
        try {
            icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon.getIconHeight();
            int w = icon.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon);
            add(jLabel1, BorderLayout.CENTER);
        } catch (MalformedURLException ex) {
            MusicBoxView.showErrorDialog(ex);
            jLabel1.setIcon(MusicBoxView.noImage);
        }
        add(jPanel1, BorderLayout.PAGE_END);
        pack();
    }

    /**
     *
     * @param start
     */
    public void googleImageSearch(String start) {
        try {
            String u = "http://images.google.com/images?q=" + custom + start;
            if (u.contains(" ")) {
                u = u.replace(" ", "+");
            }
            URL url = new URL(u);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            BufferedReader readIn = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            googleImages.clear();
            String text = "";
            String lin = "";
            while ((lin = readIn.readLine()) != null) {
                text += lin;
            }
            readIn.close();
            if (text.contains("\n")) {
                text = text.replace("\n", "");
            }
            String[] array = text.split("\\Qhref=\"/imgres?imgurl=\\E");
            for (String s : array) {
                if (s.startsWith("http://") || s.startsWith("https://") && s.contains("&amp;")) {
                    String s1 = s.substring(0, s.indexOf("&amp;"));
                    googleImages.add(s1);
                }
            }
        } catch (Exception ex4) {
            MusicBoxView.showErrorDialog(ex4);
        }
        jButton4.setEnabled(true);
        jButton2.setEnabled(true);
        getContentPane().remove(jLabel1);
        ImageIcon icon;
        try {
            icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon.getIconHeight();
            int w = icon.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon);
            add(jLabel1, BorderLayout.CENTER);
        } catch (MalformedURLException ex) {
            MusicBoxView.showErrorDialog(ex);
            jLabel1.setIcon(MusicBoxView.noImage);
        }
        add(jPanel1, BorderLayout.PAGE_END);
        pack();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPopupMenu1.setName("jPopupMenu1");
        JMenuItem append = new JMenuItem("Append to playlist");
        JMenuItem google = new JMenuItem("Google Image Search");
        jPopupMenu1.add(append);
        jPopupMenu1.add(google);
        append.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appendToPlayList();
            }
        });
        google.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (MusicBoxView.internetConnection) {
                    googleImageSearch();
                }
            }
        });
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setResizable(false);
        setUndecorated(true);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(musicbox.backend.MusicBoxApp.class).getContext().getResourceMap(SmallAlbumArt.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        jLabel1.setIcon(i);
        jLabel1.setToolTipText(artist + "\n (click to close)");
        jLabel1.setSize(130, 125);
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel1MouseClicked(evt);
            }
        });
        jLabel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jLabel1MouseDragged(evt);
            }

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jLabel1MouseMoved(evt);
            }
        });
        jLabel1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getContentPane().add(jLabel1, java.awt.BorderLayout.CENTER);
        jPanel1.setName("jPanel1");
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel1.setLayout(new java.awt.BorderLayout());
        jButton1.setText(resourceMap.getString("jButton1.text"));
        jButton1.setName("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1, java.awt.BorderLayout.LINE_START);
        jButton1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jButton2.setText(resourceMap.getString("jButton2.text"));
        jButton2.setName("jButton2");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2, java.awt.BorderLayout.LINE_END);
        jButton2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);
        jPanel2.setName("jPanel2");
        jPanel2.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel2.setLayout(new java.awt.BorderLayout());
        jButton3.setText(resourceMap.getString("jButton3.text"));
        jButton3.setName("jButton3");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jButton3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jPanel2.add(jButton3, java.awt.BorderLayout.LINE_START);
        jButton4.setText(resourceMap.getString("jButton4.text"));
        jButton4.setName("jButton4");
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton4, java.awt.BorderLayout.LINE_END);
        jButton4.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);
        pack();
    }

    private void jLabel1MouseDragged(java.awt.event.MouseEvent evt) {
        if (googleSearched) {
            setLocation(evt.getLocationOnScreen());
        }
    }

    private void jLabel1MouseMoved(java.awt.event.MouseEvent evt) {
        if (bottomShowing == true && ((double) evt.getY() / (double) jLabel1.getHeight()) * 100 < 50) {
            bottomShowing = false;
            topShowing = true;
            getContentPane().remove(jPanel1);
            getContentPane().add(jPanel2, BorderLayout.PAGE_START);
            pack();
        } else if (topShowing == true && ((double) evt.getY() / (double) jLabel1.getHeight()) * 100 >= 50) {
            bottomShowing = true;
            topShowing = false;
            getContentPane().remove(jPanel2);
            getContentPane().add(jPanel1, BorderLayout.PAGE_END);
            pack();
        }
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        googleImageLocation -= 1;
        ImageIcon icon1;
        getContentPane().remove(jLabel1);
        try {
            icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            if (h < 1 || w < 1) {
                icon1 = MusicBoxView.noImage;
                h = icon1.getIconHeight();
                w = icon1.getIconWidth();
            }
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        } catch (MalformedURLException ex1) {
            musicbox.gui.MusicBoxView.showErrorDialog(ex1);
            icon1 = MusicBoxView.noImage;
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        }
        if (googleImageLocation == 0) {
            jButton1.setEnabled(false);
            jButton3.setEnabled(false);
        } else {
            imageResults -= 21;
            googleImageLocation = 19;
            String start = "&sa=N&start=" + imageResults + "&ndsp=21";
            googleImageSearch(start);
        }
        if (googleImageLocation < googleImages.size() - 1) {
            jButton2.setEnabled(true);
            jButton4.setEnabled(true);
        } else {
            jButton2.setEnabled(false);
            jButton4.setEnabled(false);
        }
    }

    private void jLabel1MouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getButton() == 3) {
            jPopupMenu1.show(evt.getComponent(), evt.getX(), evt.getY());
        }
        if (evt.getButton() == 1) {
            if (googleSearched) {
                int r = JOptionPane.showConfirmDialog(jLabel1, "Use this image?", "Google Image", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    ImageIcon ii = (ImageIcon) jLabel1.getIcon();
                    j.setIcon(new ImageIcon(ii.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    Backend.setImagePath(googleImages.elementAt(googleImageLocation), artist);
                }
            }
            googleSearched = false;
            dispose();
        }
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        googleImageLocation += 1;
        getContentPane().remove(jLabel1);
        ImageIcon icon1;
        try {
            icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            if (h < 1 || w < 1) {
                icon1 = MusicBoxView.noImage;
                h = icon1.getIconHeight();
                w = icon1.getIconWidth();
            }
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        } catch (MalformedURLException ex1) {
            MusicBoxView.showErrorDialog(ex1);
            icon1 = MusicBoxView.noImage;
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        }
        if (googleImageLocation == 0) {
            jButton1.setEnabled(false);
            jButton3.setEnabled(false);
        } else {
            jButton1.setEnabled(true);
            jButton3.setEnabled(true);
        }
        if (googleImageLocation < googleImages.size() - 1) {
            jButton2.setEnabled(true);
            jButton4.setEnabled(true);
        } else {
            imageResults += 21;
            googleImageLocation = 0;
            String start = "&sa=N&start=" + imageResults + "&ndsp=21";
            googleImageSearch(start);
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        googleImageLocation -= 1;
        ImageIcon icon1;
        getContentPane().remove(jLabel1);
        try {
            icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            if (h < 1 || w < 1) {
                icon1 = MusicBoxView.noImage;
                h = icon1.getIconHeight();
                w = icon1.getIconWidth();
            }
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        } catch (MalformedURLException ex1) {
            musicbox.gui.MusicBoxView.showErrorDialog(ex1);
            icon1 = MusicBoxView.noImage;
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        }
        if (googleImageLocation == 0) {
            jButton1.setEnabled(false);
            jButton3.setEnabled(false);
        } else {
            imageResults -= 21;
            googleImageLocation = 19;
            String start = "&sa=N&start=" + imageResults + "&ndsp=21";
            googleImageSearch(start);
        }
        if (googleImageLocation < googleImages.size() - 1) {
            jButton2.setEnabled(true);
            jButton4.setEnabled(true);
        } else {
            jButton2.setEnabled(false);
            jButton4.setEnabled(false);
        }
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        googleImageLocation += 1;
        getContentPane().remove(jLabel1);
        ImageIcon icon1;
        try {
            icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            if (h < 1 || w < 1) {
                icon1 = MusicBoxView.noImage;
                h = icon1.getIconHeight();
                w = icon1.getIconWidth();
            }
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        } catch (MalformedURLException ex1) {
            MusicBoxView.showErrorDialog(ex1);
            icon1 = MusicBoxView.noImage;
            int h = icon1.getIconHeight();
            int w = icon1.getIconWidth();
            jLabel1.setSize(w, h);
            jLabel1.setIcon(icon1);
            add(jLabel1, BorderLayout.CENTER);
            pack();
        }
        if (googleImageLocation == 0) {
            jButton1.setEnabled(false);
            jButton3.setEnabled(false);
        } else {
            jButton1.setEnabled(true);
            jButton3.setEnabled(true);
        }
        if (googleImageLocation < googleImages.size() - 1) {
            jButton2.setEnabled(true);
            jButton4.setEnabled(true);
        } else {
            imageResults += 21;
            googleImageLocation = 0;
            String start = "&sa=N&start=" + imageResults + "&ndsp=21";
            googleImageSearch(start);
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    private static javax.swing.JLabel jLabel1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPopupMenu jPopupMenu1;

    private boolean topShowing = false;

    private boolean bottomShowing = false;

    /**
     *
     */
    public static int imageResults = 0;
}
