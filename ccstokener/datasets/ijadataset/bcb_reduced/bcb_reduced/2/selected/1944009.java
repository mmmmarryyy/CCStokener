package musicbox.gui;

import musicbox.backend.filefilters.ImageFilter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.datatype.Artwork;

/**
 *
 * @author Isaac Hammon
 */
public class AlbumArt extends javax.swing.JDialog {

    File f;

    Dimension d;

    private static int h;

    private static int w;

    private static String artist;

    private static String album;

    private static Vector<String> googleImages;

    private static int googleImageLocation = 0;

    private static boolean googleSearched = false;

    private String imagePath;

    /** Creates new form AlbumArt
     * @param parent
     * @param f
     */
    public AlbumArt(java.awt.Frame parent, File f) {
        super(parent);
        this.f = f;
        googleImages = new Vector<String>();
        googleSearched = false;
        getArtWork();
        initComponents();
        this.setIconImage(MusicBoxView.musicBoxIcon16.getImage());
        setVisible(true);
    }

    /**
     *
     * @param parent
     * @param imagePath
     * @param artist
     * @param album
     */
    @SuppressWarnings({ "static-access", "static-access" })
    public AlbumArt(java.awt.Frame parent, String imagePath, String artist, String album) {
        super(parent);
        this.imagePath = imagePath;
        AlbumArt.artist = artist;
        AlbumArt.album = album;
        googleSearched = false;
        jLabel = new JLabel();
        ImageIcon iI = new ImageIcon();
        if (imagePath.contains("http://")) {
            try {
                if (MusicBoxView.internetConnection) {
                    iI = new ImageIcon(new URL(imagePath));
                }
            } catch (MalformedURLException ex) {
                MusicBoxView.showErrorDialog(ex);
            }
        } else {
            iI = new ImageIcon(imagePath);
        }
        Image i = iI.getImage();
        d = new Dimension(iI.getIconWidth(), iI.getIconHeight());
        Image j = i.getScaledInstance(iI.getIconWidth(), iI.getIconHeight(), Image.SCALE_SMOOTH);
        ImageIcon iJ = new ImageIcon(j);
        h = iI.getIconHeight();
        w = iI.getIconWidth();
        if (h < 1 || w < 1) {
            iJ = MusicBoxView.noImage;
            h = iJ.getIconHeight();
            w = iJ.getIconWidth();
        }
        jLabel.setSize(w, h);
        jLabel.setIcon(iJ);
        add(jLabel, BorderLayout.CENTER);
        initComponents();
        this.setIconImage(MusicBoxView.musicBoxIcon16.getImage());
        setVisible(true);
    }

    /**
     *
     * @param parent
     * @param v
     * @param loc
     * @param artist
     * @param album
     */
    @SuppressWarnings({ "static-access", "static-access" })
    public AlbumArt(java.awt.Frame parent, Vector<String> v, int loc, String artist, String album) {
        super(parent);
        AlbumArt.artist = artist;
        AlbumArt.album = album;
        googleImages = v;
        googleSearched = true;
        googleImageLocation = loc;
        setArtWork();
        initComponents();
        this.setIconImage(MusicBoxView.musicBoxIcon16.getImage());
        setVisible(true);
    }

    private void setArtWork() {
        if (googleSearched) {
            if (MusicBoxView.internetConnection) {
                jLabel = new JLabel();
                ImageIcon icon;
                try {
                    icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    h = icon.getIconHeight();
                    w = icon.getIconWidth();
                    if (h < 1 || w < 1) {
                        icon = MusicBoxView.noImage;
                        h = icon.getIconHeight();
                        w = icon.getIconWidth();
                    }
                    jLabel.setSize(w, h);
                    jLabel.setIcon(icon);
                    add(jLabel, BorderLayout.CENTER);
                } catch (MalformedURLException ex1) {
                    MusicBoxView.showErrorDialog(ex1);
                }
            }
        }
    }

    private void getArtWork() {
        jLabel = new JLabel();
        boolean foundArt = false;
        if (!f.getName().endsWith(".ape")) {
            try {
                AudioFile file = AudioFileIO.read(f);
                Artwork albumArt = file.getTag().getFirstArtwork();
                artist = file.getTag().getFirstArtist();
                album = file.getTag().getFirstAlbum();
                try {
                    byte[] b = albumArt.getBinaryData();
                    ImageIcon iI = new ImageIcon(b);
                    Image i = iI.getImage();
                    d = new Dimension(iI.getIconWidth(), iI.getIconHeight());
                    Image j = i.getScaledInstance(iI.getIconWidth(), iI.getIconHeight(), Image.SCALE_SMOOTH);
                    ImageIcon iJ = new ImageIcon(j);
                    jLabel.setSize(iI.getIconWidth(), iI.getIconHeight());
                    h = iI.getIconHeight();
                    w = iI.getIconWidth();
                    jLabel.setIcon(iJ);
                    add(jLabel, BorderLayout.CENTER);
                    foundArt = true;
                    if (h < 0 && w < 0) {
                        foundArt = false;
                    }
                } catch (Exception e) {
                    if (f.getParentFile() != null) {
                        File myDir = f.getParentFile();
                        String[] dirNames = myDir.list(new ImageFilter());
                        if (dirNames.length > 0) {
                            ImageIcon max = new ImageIcon(myDir + "/" + dirNames[0]);
                            for (int p1 = 0; p1 < dirNames.length; p1++) {
                                ImageIcon iK = new ImageIcon(myDir + "/" + dirNames[p1]);
                                if (iK.getIconHeight() + iK.getIconWidth() > max.getIconHeight() + iK.getIconWidth()) {
                                    max = iK;
                                }
                            }
                            Image k = max.getImage();
                            d = new Dimension(max.getIconWidth(), max.getIconHeight());
                            h = max.getIconHeight();
                            w = max.getIconWidth();
                            Image l = k.getScaledInstance(max.getIconWidth(), max.getIconHeight(), Image.SCALE_SMOOTH);
                            ImageIcon iL = new ImageIcon(l);
                            jLabel.setSize(iL.getIconWidth(), iL.getIconHeight());
                            jLabel.setIcon(iL);
                            add(jLabel, BorderLayout.CENTER);
                            foundArt = true;
                        }
                    }
                }
            } catch (Exception p) {
                MusicBoxView.showErrorDialog(p);
            }
        }
        if (foundArt == false) {
            if (MusicBoxView.internetConnection) {
                googleImageSearch();
                ImageIcon icon;
                try {
                    icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    h = icon.getIconHeight();
                    w = icon.getIconWidth();
                    if (h < 1 || w < 1) {
                        icon = MusicBoxView.noImage;
                        h = icon.getIconHeight();
                        w = icon.getIconWidth();
                    }
                    jLabel.setSize(w, h);
                    jLabel.setIcon(icon);
                    add(jLabel, BorderLayout.CENTER);
                } catch (MalformedURLException ex1) {
                    MusicBoxView.showErrorDialog(ex1);
                    icon = MusicBoxView.noImage;
                    h = icon.getIconHeight();
                    w = icon.getIconWidth();
                    jLabel.setSize(w, h);
                    jLabel.setIcon(icon);
                    add(jLabel, BorderLayout.CENTER);
                }
            }
        }
    }

    /**
     *
     */
    public void googleImageSearch() {
        googleSearched = true;
        googleImageLocation = 0;
        try {
            String u = "http://images.google.com/images?q=" + artist + " - " + album;
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
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jLabel = jLabel;
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(musicbox.backend.MusicBoxApp.class).getContext().getResourceMap(AlbumArt.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        jLabel.setName("jLabel");
        jLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jLabelMouseMoved(evt);
            }
        });
        getContentPane().add(jLabel, java.awt.BorderLayout.CENTER);
        setTitle(artist + " - " + album);
        jPanel2.setName("jPanel2");
        jPanel2.setPreferredSize(new java.awt.Dimension(105, 33));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));
        jButton1.setText(resourceMap.getString("jButton1.text"));
        jButton1.setName("jButton1");
        jPanel2.add(jButton1);
        if (googleImageLocation > 0) jButton1.setEnabled(true); else jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                googleImageLocation -= 1;
                ImageIcon icon1;
                getContentPane().remove(jLabel);
                try {
                    icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    h = icon1.getIconHeight();
                    w = icon1.getIconWidth();
                    jLabel.setSize(w, h);
                    jLabel.setIcon(icon1);
                    add(jLabel, BorderLayout.CENTER);
                    pack();
                } catch (MalformedURLException ex1) {
                    musicbox.gui.MusicBoxView.showErrorDialog(ex1);
                }
                if (googleImageLocation == 0) {
                    jButton1.setEnabled(false);
                } else {
                    jButton1.setEnabled(true);
                }
                if (googleImageLocation < googleImages.size() - 1) {
                    jButton2.setEnabled(true);
                } else {
                    jButton2.setEnabled(false);
                }
            }
        });
        jButton1.addMouseListener(new java.awt.event.MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jButton1.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                jButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }
        });
        jButton2.setText(resourceMap.getString("jButton2.text"));
        jButton2.setName("jButton2");
        jPanel2.add(jButton2);
        if (googleSearched) {
            jButton2.setEnabled(true);
        } else {
            jButton2.setEnabled(false);
        }
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                googleImageLocation += 1;
                getContentPane().remove(jLabel);
                ImageIcon icon1;
                try {
                    icon1 = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    h = icon1.getIconHeight();
                    w = icon1.getIconWidth();
                    jLabel.setSize(w, h);
                    jLabel.setIcon(icon1);
                    add(jLabel, BorderLayout.CENTER);
                    pack();
                } catch (MalformedURLException ex1) {
                    MusicBoxView.showErrorDialog(ex1);
                }
                if (googleImageLocation == 0) {
                    jButton1.setEnabled(false);
                } else {
                    jButton1.setEnabled(true);
                }
                if (googleImageLocation < googleImages.size() - 1) {
                    jButton2.setEnabled(true);
                } else {
                    jButton2.setEnabled(false);
                }
            }
        });
        jButton2.addMouseListener(new java.awt.event.MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jButton2.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                jButton2.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }
        });
        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_END);
        if (googleSearched == false) {
            getContentPane().remove(jPanel2);
        } else {
            bottomShowing = true;
        }
        pack();
    }

    private void jLabelMouseMoved(java.awt.event.MouseEvent evt) {
        if (bottomShowing == true && ((double) evt.getY() / (double) jLabel.getHeight()) * 100 < 50) {
            bottomShowing = false;
            topShowing = true;
            getContentPane().remove(jPanel2);
            getContentPane().add(jPanel2, BorderLayout.PAGE_START);
            pack();
        } else if (topShowing == true && ((double) evt.getY() / (double) jLabel.getHeight()) * 100 >= 50) {
            bottomShowing = true;
            topShowing = false;
            getContentPane().remove(jPanel2);
            getContentPane().add(jPanel2, BorderLayout.PAGE_END);
            pack();
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JLabel jLabel;

    private javax.swing.JPanel jPanel2;

    boolean bottomShowing = false;

    boolean topShowing = false;
}
