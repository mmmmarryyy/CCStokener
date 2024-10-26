package musicbox.gui;

import musicbox.database.Database;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import musicbox.backend.MusicBoxApp;
import noTalent.MusicOutputDesign;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3FileWriter;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;

/**
 *
 * @author Isaac Hammon
 */
public class SongInfo extends javax.swing.JDialog {

    File file;

    /**
     *
     */
    public AudioFile f;

    private MusicOutputDesign m;

    private int currentlyDisplaying;

    private Vector<MusicOutputDesign> info;

    private boolean close = false;

    private AudioFileFormat aFF;

    private Map properties;

    private int googleImageLocation;

    private static Vector<String> googleImages = new Vector<String>();

    private int[] locations;

    private int loc;

    private boolean canReadFile = true;

    /** Creates new form SongInfo
     * @param parent
     * @param v
     * @param loc
     * @param locations
     */
    public SongInfo(java.awt.Frame parent, Vector<MusicOutputDesign> v, int loc, int locations[]) {
        super(parent);
        this.loc = loc;
        this.locations = locations;
        this.currentlyDisplaying = this.locations[loc];
        this.info = v;
        m = info.elementAt(currentlyDisplaying);
        m.setFilePath(replaceDoubleApostrophe(m.getFilePath()));
        m.setFile(replaceDoubleApostrophe(m.getFile()));
        m.setAlbum(replaceDoubleApostrophe(m.getAlbum()));
        m.setArtist(replaceDoubleApostrophe(m.getArtist()));
        m.setTrackTitle(replaceDoubleApostrophe(m.getTrackTitle()));
        this.file = new File(m.getFilePath() + "/" + m.getFile());
        canReadFile = this.file.canRead();
        if (!canReadFile) {
            MusicBoxView.showFileUnavailable(m);
        }
        if (canReadFile && !file.getName().toLowerCase().endsWith(".ape")) {
            try {
                this.f = AudioFileIO.read(file);
            } catch (CannotReadException ex) {
                MusicBoxView.showFileUnavailable(m);
                MusicBoxView.showErrorDialog(ex);
            } catch (IOException ex) {
                MusicBoxView.showFileUnavailable(m);
                MusicBoxView.showErrorDialog(ex);
            } catch (TagException ex) {
                MusicBoxView.showErrorDialog(ex);
            } catch (ReadOnlyFileException ex) {
                MusicBoxView.showErrorDialog(ex);
            } catch (InvalidAudioFrameException ex) {
                MusicBoxView.showErrorDialog(ex);
            }
        } else {
            if (canReadFile) {
                try {
                    aFF = AudioSystem.getAudioFileFormat(file);
                    properties = aFF.properties();
                    this.f = null;
                } catch (UnsupportedAudioFileException ex) {
                    MusicBoxView.showFileUnavailable(m);
                    MusicBoxView.showErrorDialog(ex);
                } catch (IOException ex) {
                    MusicBoxView.showFileUnavailable(m);
                    MusicBoxView.showErrorDialog(ex);
                }
            }
        }
        initComponents();
        this.setIconImage(MusicBoxView.musicBoxIcon16.getImage());
        getArtWork();
    }

    private String replaceDoubleApostrophe(String s) {
        if (s.contains("\'\'")) {
            s = s.replace("\'\'", "\'");
        }
        return s;
    }

    private void next() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        googleImages.clear();
        loc += 1;
        currentlyDisplaying = locations[loc];
        this.m = info.elementAt(currentlyDisplaying);
        this.file = new File(m.getFilePath() + "/" + m.getFile());
        canReadFile = this.file.canRead();
        if (!canReadFile) {
            MusicBoxView.showFileUnavailable(this.m);
        }
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile) {
                this.f = AudioFileIO.read(file);
            }
        } else {
            if (canReadFile) {
                try {
                    aFF = AudioSystem.getAudioFileFormat(file);
                } catch (UnsupportedAudioFileException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
                properties = aFF.properties();
            }
        }
        if (loc == locations.length - 1) {
            jButton4.setEnabled(false);
        } else {
            jButton4.setEnabled(true);
        }
        jButton3.setEnabled(true);
        resetFields();
    }

    private void previous() throws CannotReadException, IOException, TagException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        googleImages.clear();
        loc -= 1;
        currentlyDisplaying = locations[loc];
        this.m = info.elementAt(currentlyDisplaying);
        this.file = new File(m.getFilePath() + "/" + m.getFile());
        canReadFile = this.file.canRead();
        if (!canReadFile) {
            MusicBoxView.showFileUnavailable(this.m);
        }
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile) {
                this.f = AudioFileIO.read(file);
            }
        } else {
            if (canReadFile) {
                try {
                    aFF = AudioSystem.getAudioFileFormat(file);
                } catch (UnsupportedAudioFileException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
                properties = aFF.properties();
            }
        }
        if (loc == 0) {
            jButton3.setEnabled(false);
        } else {
            jButton3.setEnabled(true);
        }
        jButton4.setEnabled(true);
        resetFields();
    }

    private void resetFields() {
        jButton5.setEnabled(false);
        jButton6.setEnabled(false);
        boolean tagIsNull = false;
        if (f.getTag() == null) {
            tagIsNull = true;
        } else {
            tagIsNull = false;
        }
        if (!canReadFile) tagIsNull = true;
        oldM = m;
        newM = oldM;
        jTextField7.setText(file.getParent() + "/" + file.getName());
        oldFilePath = file.getParent();
        oldFile = file.getName();
        oldM.setFilePath(oldFilePath);
        oldM.setFile(oldFile);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstTitle() != null) {
                    oldTrackTitle = f.getTag().getFirstTitle();
                } else {
                    oldTrackTitle = m.getTrackTitle();
                }
            } else {
                oldTrackTitle = m.getTrackTitle();
            }
        } else {
            if (properties.containsKey("title")) {
                oldTrackTitle = (String) properties.get("title");
            }
        }
        jTextField1.setText(oldTrackTitle);
        oldM.setTrackTitle(oldTrackTitle);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstArtist() != null) {
                    oldArtist = f.getTag().getFirstArtist();
                } else {
                    oldArtist = m.getArtist();
                }
            } else {
                oldArtist = m.getArtist();
            }
        } else {
            if (properties.containsKey("author")) {
                oldArtist = (String) properties.get("author");
            }
        }
        jTextField2.setText(oldArtist);
        oldM.setArtist(oldArtist);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstAlbum() != null) {
                    oldAlbum = f.getTag().getFirstAlbum();
                } else {
                    oldAlbum = m.getAlbum();
                }
            } else {
                oldAlbum = m.getAlbum();
            }
        } else {
            if (properties.containsKey("album")) {
                oldAlbum = (String) properties.get("album");
            }
        }
        jTextField3.setText(oldAlbum);
        oldM.setAlbum(oldAlbum);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstTrack() != null) {
                    oldTrackNumber = f.getTag().getFirstTrack();
                } else {
                    oldTrackNumber = m.getTrackNumber();
                }
            } else {
                oldTrackNumber = m.getTrackNumber();
            }
        } else {
            if (properties.containsKey("track")) {
                oldTrackNumber = (String) properties.get("track");
            }
        }
        jTextField4.setText(oldTrackNumber);
        oldM.setTrackNumber(oldTrackNumber);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstYear() != null) {
                    jTextField5.setText(f.getTag().getFirstYear());
                } else {
                    jTextField5.setText("");
                }
            } else {
                jTextField5.setText("");
            }
        } else {
            if (properties.containsKey("year")) {
                jTextField5.setText((String) properties.get("year"));
            }
        }
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstGenre() != null) {
                    jTextField6.setText(f.getTag().getFirstGenre());
                } else {
                    jTextField6.setText("");
                }
            } else {
                jTextField6.setText("");
            }
        } else {
            if (properties.containsKey("genre")) {
                jTextField6.setText((String) properties.get("genre"));
            }
        }
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (!tagIsNull) {
                if (f.getTag().getFirstComment() != null) {
                    jTextArea1.setText(f.getTag().getFirstComment());
                } else {
                    jTextArea1.setText("");
                }
            } else {
                jTextArea1.setText("");
            }
        } else {
            if (properties.containsKey("comment")) {
                jTextArea1.setText((String) properties.get("comment"));
            }
        }
        jTextField8.setText(m.getRating());
        jTextField9.setText(m.getImagePath());
        oldImagePath = m.getImagePath();
        oldRating = m.getRating();
        oldM.setPlayCount(m.getPlayCount());
        newM.setPlayCount(m.getPlayCount());
        newM.setTrackLength(m.getTrackLength());
        getArtWork();
    }

    /**
     *
     */
    public void closeSongInfo() {
        googleImages.clear();
        dispose();
    }

    /**
     *
     */
    public void googleImageSearch() {
        googleImageLocation = 0;
        try {
            String u = "http://images.google.com/images?q=" + m.getArtist() + " - " + m.getAlbum();
            if (u.contains(" ")) {
                u = u.replace(" ", "+");
            }
            URL url = new URL(u);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            BufferedReader readIn = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            googleImages.clear();
            String text = "";
            String line = "";
            while ((line = readIn.readLine()) != null) {
                text += line;
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

    /**
     *
     * @throws FieldDataInvalidException
     */
    public void writeTag() throws FieldDataInvalidException {
        if (canReadFile && !file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() == null) {
                f.setTag(f.createDefaultTag());
            }
            f.getTag().setTitle(jTextField1.getText());
            f.getTag().setArtist(jTextField2.getText());
            f.getTag().setAlbum(jTextField3.getText());
            f.getTag().setTrack(jTextField4.getText());
            f.getTag().setYear(jTextField5.getText());
            f.getTag().setGenre(jTextField6.getText());
            f.getTag().setComment(jTextArea1.getText());
        }
        if (oldM.getTrackLength().compareTo("N/A") == 0 && oldM.getFile().toLowerCase().endsWith(".mp3")) {
            MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
            oldM.setTrackLength(audioHeader.getTrackLengthAsString());
            newM.setTrackLength(audioHeader.getTrackLengthAsString());
        }
        File nFile = new File(jTextField7.getText());
        newFilePath = nFile.getParent();
        newM.setFilePath(newFilePath);
        newFile = nFile.getName();
        newM.setFile(newFile);
        newTrackTitle = jTextField1.getText();
        newM.setTrackTitle(newTrackTitle);
        newArtist = jTextField2.getText();
        newM.setArtist(newArtist);
        newAlbum = jTextField3.getText();
        newM.setAlbum(newAlbum);
        newTrackNumber = jTextField4.getText();
        newM.setTrackNumber(newTrackNumber);
        newRating = jTextField8.getText();
        newM.setRating(newRating);
        newM.setPlayCount(oldM.getPlayCount());
        newImagePath = jTextField9.getText();
        newM.setImagePath(newImagePath);
        if (newM.getTrackNumber().compareTo(" ") == 0 || newM.getTrackNumber().length() == 0) {
            newM.setTrackNumber("0");
        }
        if (close) {
            googleImages.clear();
            dispose();
        }
        WriteTag wt = new WriteTag(file, f, oldM, newM, canReadFile);
        wt.start();
    }

    /**
     *
     */
    public void showLargeAlbumArtwork() {
        JFrame mainFrame = MusicBoxApp.getApplication().getMainFrame();
        AlbumArt.setDefaultLookAndFeelDecorated(true);
        if (jTextField9.getText().compareTo("NoImage") != 0 && jTextField9.getText().compareTo("embedded") != 0) {
            new AlbumArt(mainFrame, jTextField9.getText(), m.getArtist(), m.getAlbum());
        } else if (googleImages.size() == 0) {
            new AlbumArt(mainFrame, file);
        } else {
            new AlbumArt(mainFrame, googleImages, googleImageLocation, m.getArtist(), m.getAlbum());
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jPopupMenu1.setName("jPopupMenu1");
        JMenuItem googleSearch = new JMenuItem("Google Search");
        jPopupMenu1.add(googleSearch);
        googleSearch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (MusicBoxView.internetConnection) {
                    googleImageSearch();
                    jButton6.setEnabled(true);
                    try {
                        jLabel9.setIcon(new ImageIcon(new ImageIcon(new URL(googleImages.elementAt(googleImageLocation))).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    } catch (MalformedURLException ex) {
                        MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(musicbox.backend.MusicBoxApp.class).getContext().getResourceMap(SongInfo.class);
        setTitle(resourceMap.getString("Form.title"));
        setMinimumSize(new java.awt.Dimension(523, 480));
        setName("Form");
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, -1, 20));
        jLabel3.setText(resourceMap.getString("jLabel3.text"));
        jLabel3.setName("jLabel3");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, -1, 20));
        jLabel4.setText(resourceMap.getString("jLabel4.text"));
        jLabel4.setName("jLabel4");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, -1, 20));
        jLabel5.setText(resourceMap.getString("jLabel5.text"));
        jLabel5.setName("jLabel5");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, -1, 20));
        jLabel6.setText(resourceMap.getString("jLabel6.text"));
        jLabel6.setName("jLabel6");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 210, -1, 20));
        jTextField1.setName("jTextField1");
        getContentPane().add(jTextField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 50, 430, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstTitle() != null) {
                    oldTrackTitle = f.getTag().getFirstTitle();
                } else {
                    oldTrackTitle = m.getTrackTitle();
                }
            } else {
                oldTrackTitle = m.getTrackTitle();
            }
        } else {
            if (properties.containsKey("title")) {
                oldTrackTitle = (String) properties.get("title");
            }
        }
        jTextField1.setText(oldTrackTitle);
        oldM.setTrackTitle(oldTrackTitle);
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jTextField2.setName("jTextField2");
        getContentPane().add(jTextField2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 90, 440, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) if (canReadFile && f.getTag() != null) {
            if (f.getTag().getFirstArtist() != null) {
                oldArtist = f.getTag().getFirstArtist();
            } else {
                oldArtist = m.getArtist();
            }
        } else oldArtist = m.getArtist(); else {
            if (properties.containsKey("author")) {
                oldArtist = (String) properties.get("author");
            }
        }
        jTextField2.setText(oldArtist);
        oldM.setArtist(oldArtist);
        jTextField2.addKeyListener(new java.awt.event.KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        jTextField3.setName("jTextField3");
        getContentPane().add(jTextField3, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 130, 440, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstAlbum() != null) {
                    oldAlbum = f.getTag().getFirstAlbum();
                } else {
                    oldAlbum = m.getAlbum();
                }
            } else {
                oldAlbum = m.getAlbum();
            }
        } else {
            if (properties.containsKey("album")) {
                oldAlbum = (String) properties.get("album");
            }
        }
        jTextField3.setText(oldAlbum);
        oldM.setAlbum(oldAlbum);
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jTextField4.setName("jTextField4");
        getContentPane().add(jTextField4, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 210, 40, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstTrack() != null) {
                    oldTrackNumber = f.getTag().getFirstTrack();
                } else {
                    oldTrackNumber = m.getTrackNumber();
                }
            } else {
                oldTrackNumber = m.getTrackNumber();
            }
        } else {
            if (properties.containsKey("track")) {
                oldTrackNumber = (String) properties.get("track");
            }
        }
        jTextField4.setText(oldTrackNumber);
        oldM.setTrackNumber(oldTrackNumber);
        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jTextField5.setName("jTextField5");
        getContentPane().add(jTextField5, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 210, 50, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstYear() != null) {
                    jTextField5.setText(f.getTag().getFirstYear());
                }
            }
        } else {
            if (properties.containsKey("year")) {
                jTextField5.setText((String) properties.get("year"));
            }
        }
        jTextField5.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jLabel7.setText(resourceMap.getString("jLabel7.text"));
        jLabel7.setName("jLabel7");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 240, -1, 20));
        jScrollPane1.setName("jScrollPane1");
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1");
        jScrollPane1.setViewportView(jTextArea1);
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstComment() != null) {
                    jTextArea1.setText(f.getTag().getFirstComment());
                }
            }
        } else {
            if (properties.containsKey("comment")) {
                jTextArea1.setText((String) properties.get("comment"));
            }
        }
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 260, 470, 70));
        jButton1.setText(resourceMap.getString("jButton1.text"));
        jButton1.setName("jButton1");
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 440, 70, -1));
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    close = true;
                    writeTag();
                } catch (Exception e) {
                    musicbox.gui.MusicBoxView.showErrorDialog(e);
                }
            }
        });
        jButton1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jButton2.setText(resourceMap.getString("jButton2.text"));
        jButton2.setName("jButton2");
        getContentPane().add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 440, -1, -1));
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeSongInfo();
            }
        });
        jButton2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jLabel8.setText(resourceMap.getString("jLabel8.text"));
        jLabel8.setName("jLabel8");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 340, -1, 20));
        jLabel9.setName("jLabel9");
        getContentPane().add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 360, 110, 100));
        jLabel9.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jLabel9.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) showLargeAlbumArtwork(); else if (e.getButton() == MouseEvent.BUTTON3) jPopupMenu1.show(jLabel9, e.getX(), e.getY());
            }
        });
        DropTarget dT = new DropTarget(jLabel9, new DropTargetListener() {

            public void dragEnter(DropTargetDragEvent dtde) {
            }

            public void dragExit(DropTargetEvent dte) {
            }

            public void dragOver(DropTargetDragEvent dtde) {
            }

            public void drop(DropTargetDropEvent dtde) {
                DataFlavor[] flavors = dtde.getCurrentDataFlavors();
                if (flavors == null) {
                    return;
                }
                for (int i = 0; i < flavors.length; i++) {
                    if (flavors[i].equals(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(dtde.getDropAction());
                        Transferable transferable = dtde.getTransferable();
                        List<File> list = null;
                        try {
                            list = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        } catch (Exception e) {
                            musicbox.gui.MusicBoxView.showErrorDialog(e);
                            return;
                        }
                        Iterator<File> it = list.iterator();
                        while (it.hasNext()) {
                            image = it.next();
                            image = image.getAbsoluteFile();
                            jTextField9.setText(image.getAbsolutePath());
                            try {
                                ImageIcon iI9 = new ImageIcon(jTextField9.getText());
                                Image i9 = iI9.getImage();
                                Image j9 = i9.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                                albumArt = new ImageIcon(j9);
                                jLabel9.setSize(100, 100);
                                jLabel9.setIcon(albumArt);
                            } catch (Exception ex3) {
                                musicbox.gui.MusicBoxView.showErrorDialog(ex3);
                            }
                        }
                        break;
                    }
                }
            }

            public void dropActionChanged(DropTargetDragEvent dtde) {
            }
        });
        jLabel10.setText(resourceMap.getString("jLabel10.text"));
        jLabel10.setName("jLabel10");
        getContentPane().add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 210, -1, 20));
        jTextField6.setText(resourceMap.getString("jTextField6.text"));
        jTextField6.setName("jTextField6");
        getContentPane().add(jTextField6, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 210, 170, -1));
        if (!file.getName().toLowerCase().endsWith(".ape")) {
            if (canReadFile && f.getTag() != null) {
                if (f.getTag().getFirstGenre() != null) {
                    jTextField6.setText(f.getTag().getFirstGenre());
                }
            }
        } else {
            if (properties.containsKey("genre")) {
                jTextField6.setText((String) properties.get("genre"));
            }
        }
        jTextField6.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jTextField7.setText(resourceMap.getString("jTextField7.text"));
        jTextField7.setName("jTextField7");
        getContentPane().add(jTextField7, new org.netbeans.lib.awtextra.AbsoluteConstraints(9, 20, 500, -1));
        jTextField7.setText(file.getParent() + "/" + file.getName());
        oldFilePath = file.getParent();
        oldFile = file.getName();
        oldM.setFilePath(oldFilePath);
        oldM.setFile(oldFile);
        jTextField7.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 350, -1, 20));
        jTextField8.setText(resourceMap.getString("jTextField8.text"));
        jTextField8.setName("jTextField8");
        getContentPane().add(jTextField8, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 350, 30, -1));
        jTextField8.setText(m.getRating());
        oldRating = m.getRating();
        oldM.setPlayCount(m.getPlayCount());
        oldM.setTrackLength(m.getTrackLength());
        oldM.setId(m.getId());
        newM.setId(m.getId());
        newM.setTrackLength(m.getTrackLength());
        jTextField8.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        jButton3.setText(resourceMap.getString("jButton3.text"));
        jButton3.setName("jButton3");
        getContentPane().add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 440, -1, -1));
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    writeTag();
                    previous();
                } catch (Exception e) {
                    musicbox.gui.MusicBoxView.showErrorDialog(e);
                }
            }
        });
        if (loc == 0) jButton3.setEnabled(false); else jButton3.setEnabled(true);
        jButton3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jButton4.setText(resourceMap.getString("jButton4.text"));
        jButton4.setName("jButton4");
        getContentPane().add(jButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 440, -1, -1));
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    writeTag();
                    next();
                } catch (Exception e) {
                    musicbox.gui.MusicBoxView.showErrorDialog(e);
                }
            }
        });
        if (loc == locations.length - 1) jButton4.setEnabled(false); else jButton4.setEnabled(true);
        jButton4.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jButton5.setText(resourceMap.getString("jButton5.text"));
        jButton5.setToolTipText(resourceMap.getString("jButton5.toolTipText"));
        jButton5.setName("jButton5");
        getContentPane().add(jButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 390, -1, -1));
        jButton5.setEnabled(false);
        jButton5.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jButton5.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                googleImageLocation -= 1;
                ImageIcon icon;
                try {
                    jTextField9.setText(googleImages.elementAt(googleImageLocation));
                    icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    ImageIcon ico = new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                    jLabel9.setIcon(ico);
                    if (jLabel9.getIcon().getIconHeight() < 1 || jLabel9.getIcon().getIconWidth() < 1) {
                        ico = new ImageIcon(MusicBoxView.noImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                        jLabel9.setIcon(ico);
                    }
                } catch (MalformedURLException ex1) {
                    musicbox.gui.MusicBoxView.showErrorDialog(ex1);
                    ImageIcon ico = new ImageIcon(MusicBoxView.noImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                    jLabel9.setIcon(ico);
                }
                if (googleImageLocation == 0) jButton5.setEnabled(false); else jButton5.setEnabled(true);
                if (googleImageLocation < googleImages.size() - 1) jButton6.setEnabled(true); else jButton6.setEnabled(false);
            }
        });
        jButton6.setText(resourceMap.getString("jButton6.text"));
        jButton6.setToolTipText(resourceMap.getString("jButton6.toolTipText"));
        jButton6.setName("jButton6");
        getContentPane().add(jButton6, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 390, -1, -1));
        jButton6.setEnabled(false);
        jButton6.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                googleImageLocation += 1;
                ImageIcon icon;
                try {
                    jTextField9.setText(googleImages.elementAt(googleImageLocation));
                    icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                    ImageIcon ico = new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                    jLabel9.setIcon(ico);
                    if (jLabel9.getIcon().getIconHeight() < 1 || jLabel9.getIcon().getIconWidth() < 1) {
                        ico = new ImageIcon(MusicBoxView.noImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                        jLabel9.setIcon(ico);
                    }
                } catch (MalformedURLException ex1) {
                    musicbox.gui.MusicBoxView.showErrorDialog(ex1);
                    ImageIcon ico = new ImageIcon(MusicBoxView.noImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                    jLabel9.setIcon(ico);
                }
                if (googleImageLocation == 0) jButton5.setEnabled(false); else jButton5.setEnabled(true);
                if (googleImageLocation < googleImages.size() - 1) jButton6.setEnabled(true); else jButton6.setEnabled(false);
            }
        });
        jButton6.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jLabel11.setText(resourceMap.getString("jLabel11.text"));
        jLabel11.setName("jLabel11");
        getContentPane().add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, -1, -1));
        jTextField9.setText(resourceMap.getString("jTextField9.text"));
        jTextField9.setName("jTextField9");
        getContentPane().add(jTextField9, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 170, 430, -1));
        oldImagePath = m.getImagePath();
        if (oldImagePath.compareTo("NoImage") == 0) jTextField9.setText("NoImage"); else if (oldImagePath.compareTo("embedded") == 0) jTextField9.setText("embedded"); else jTextField9.setText(oldImagePath);
        jTextField9.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    close = true;
                    try {
                        writeTag();
                    } catch (Exception ex) {
                        musicbox.gui.MusicBoxView.showErrorDialog(ex);
                    }
                }
            }
        });
        pack();
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    public static javax.swing.JButton jButton5;

    public static javax.swing.JButton jButton6;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JPopupMenu jPopupMenu1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextField1;

    private javax.swing.JTextField jTextField2;

    private javax.swing.JTextField jTextField3;

    private javax.swing.JTextField jTextField4;

    private javax.swing.JTextField jTextField5;

    private javax.swing.JTextField jTextField6;

    private javax.swing.JTextField jTextField7;

    private javax.swing.JTextField jTextField8;

    private javax.swing.JTextField jTextField9;

    private JDialog largeAlbumArt;

    private ImageIcon albumArt;

    private Artwork aw = null;

    private File image;

    private String oldTrackNumber;

    private String oldTrackTitle;

    private String oldArtist;

    private String oldAlbum;

    private String oldFilePath;

    private String oldFile;

    private String oldRating;

    private String oldImagePath;

    private String newTrackNumber;

    private String newTrackTitle;

    private String newArtist;

    private String newAlbum;

    private String newFilePath;

    private String newFile;

    private String newRating;

    private String newImagePath;

    private MusicOutputDesign oldM = new MusicOutputDesign();

    private MusicOutputDesign newM = new MusicOutputDesign();

    private void getArtWork() {
        aw = null;
        boolean foundArt = false;
        if (m.getImagePath().compareTo("NoImage") != 0 && m.getImagePath().compareTo("embedded") != 0) {
            if (m.getImagePath().contains("http://")) {
                try {
                    if (MusicBoxView.internetConnection) {
                        jLabel9.setIcon(new ImageIcon(new ImageIcon(new URL(m.getImagePath())).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    }
                } catch (MalformedURLException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            } else {
                jLabel9.setIcon(new ImageIcon(new ImageIcon(m.getImagePath()).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            }
            googleImages.clear();
            foundArt = true;
        } else {
            if (!file.getName().toLowerCase().endsWith(".ape")) {
                if (f.getTag() != null) {
                    aw = f.getTag().getFirstArtwork();
                }
            }
            jLabel9.setVisible(true);
            try {
                byte[] b = aw.getBinaryData();
                ImageIcon iI = new ImageIcon(b);
                int h = iI.getIconHeight();
                int w = iI.getIconWidth();
                Image i = iI.getImage();
                Image j = i.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                albumArt = new ImageIcon(j);
                jLabel9.setSize(100, 100);
                jLabel9.setIcon(albumArt);
                foundArt = true;
                m.setImagePath("embedded");
                jTextField9.setText("embedded");
                if (h < 0 && w < 0) {
                    foundArt = false;
                    jTextField9.setText("NoImage");
                    m.setImagePath("NoImage");
                }
            } catch (Exception e) {
                if (file.getParentFile() != null) {
                    File myDir = file.getParentFile();
                    String[] dirNames = myDir.list(new musicbox.backend.filefilters.ImageFilter());
                    if (dirNames.length > 0) {
                        ImageIcon max = new ImageIcon(myDir + "/" + dirNames[0]);
                        for (int p1 = 0; p1 < dirNames.length; p1++) {
                            ImageIcon iK = new ImageIcon(myDir + "/" + dirNames[p1]);
                            if (iK.getIconHeight() + iK.getIconWidth() > max.getIconHeight() + iK.getIconWidth()) {
                                m.setImagePath(myDir + "/" + dirNames[p1]);
                                jTextField9.setText(m.getImagePath());
                                max = iK;
                            }
                        }
                        Image k = max.getImage();
                        Image l = k.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        albumArt = new ImageIcon(l);
                        jLabel9.setSize(100, 100);
                        jLabel9.setIcon(albumArt);
                        foundArt = true;
                    }
                }
            }
            if (foundArt == false) {
                if (MusicBoxView.internetConnection) {
                    jButton6.setEnabled(true);
                    googleImageSearch();
                    ImageIcon icon;
                    try {
                        m.setImagePath(googleImages.elementAt(googleImageLocation));
                        jTextField9.setText(m.getImagePath());
                        icon = new ImageIcon(new URL(googleImages.elementAt(googleImageLocation)));
                        ImageIcon ico = new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                        jLabel9.setIcon(ico);
                    } catch (MalformedURLException ex1) {
                        MusicBoxView.showErrorDialog(ex1);
                        ImageIcon ico = new ImageIcon(MusicBoxView.noImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                        jLabel9.setIcon(ico);
                    }
                }
            }
        }
    }
}

class WriteTag extends Thread {

    private File file;

    private AudioFile f;

    private MusicOutputDesign oldM;

    private MusicOutputDesign newM;

    private boolean canReadFile;

    public WriteTag(File file, AudioFile f, MusicOutputDesign oldM, MusicOutputDesign newM, boolean canReadFile) {
        this.file = file;
        this.f = f;
        this.oldM = oldM;
        this.newM = newM;
        this.canReadFile = canReadFile;
    }

    @Override
    public void run() {
        MusicBoxView.busyIconTimer.start();
        try {
            File f1 = new File("resources/MDB.odb");
            String db_file_name_prefix = f1.getAbsolutePath();
            Class.forName("org.hsqldb.jdbcDriver");
            Connection con = DriverManager.getConnection("jdbc:hsqldb:file:" + db_file_name_prefix);
            java.sql.Statement stmt = con.createStatement();
            Database.updateTags(stmt, oldM, newM);
            stmt.close();
        } catch (Exception e) {
            MusicBoxView.showErrorDialog(e);
        }
        MusicBoxView.updateVector(MusicBoxView.allMusic, newM, MusicBoxView.currentlyShowingTable);
        if (!MusicBoxView.currentlyPlaying.isEmpty()) {
            MusicBoxView.updateVector(MusicBoxView.allMusic, newM, MusicBoxView.currentlyPlayingTable);
        }
        if (!MusicBoxView.playList.isEmpty()) {
            MusicBoxView.updateVector(MusicBoxView.allMusic, newM, MusicBoxView.playlistTable);
        }
        if (canReadFile && file.toString().toLowerCase().endsWith(".mp3")) {
            MP3FileWriter mFW = new MP3FileWriter();
            try {
                mFW.writeFile(f);
            } catch (Exception e) {
                MusicBoxView.showErrorDialog(e);
            }
        } else {
            try {
                if (canReadFile && !file.getName().toLowerCase().endsWith(".ape")) {
                    AudioFileIO.write(f);
                }
            } catch (CannotWriteException ex) {
                MusicBoxView.showErrorDialog(ex);
            }
        }
        MusicBoxView.busyIconTimer.stop();
        Thread.currentThread().stop();
    }
}
