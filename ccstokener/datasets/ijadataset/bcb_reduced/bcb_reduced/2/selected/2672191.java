package Core;

import RSS.AllFeeds;
import RSS.NewEntries;
import RSS.RSSFeed;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JProgressBar;

/**
 *
 * @author H3R3T1C
 */
public class Files {

    public static String loadFileUrl(Frame f, String title, String defDir, String fileType) {
        FileDialog fd = new FileDialog(f, title, FileDialog.LOAD);
        fd.setFile(fileType);
        fd.setDirectory(defDir);
        fd.setLocation(50, 50);
        fd.show();
        if (fd.getFile() == null) return "";
        return fd.getDirectory() + fd.getFile();
    }

    public static void copyFile(String srFile, String dtFile) {
        try {
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            System.out.println("File copied.");
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static DefaultListModel getFeeds(NewEntries e) {
        DefaultListModel model = new DefaultListModel();
        try {
            BufferedReader in = new BufferedReader(new FileReader("User\\feeds.rss"));
            model.addElement(new AllFeeds());
            model.addElement(e);
            String line;
            while ((line = in.readLine()) != null) {
                RSSFeed feed = new RSSFeed(line, e);
                model.addElement(feed);
            }
            in.close();
            return model;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new DefaultListModel();
        }
    }

    public static void saveRssSettings(int a, int b, boolean c) {
        File file = new File("User\\rss.settings");
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(a + "\n" + b + "\n");
            if (c) {
                output.write("true");
            } else {
                output.write("false");
            }
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String[] loadRssSettings() {
        String[] s = new String[3];
        try {
            BufferedReader in = new BufferedReader(new FileReader("User\\rss.settings"));
            s[0] = in.readLine();
            s[1] = in.readLine();
            s[2] = in.readLine();
            in.close();
            return s;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveRSSFeeds(JList list) {
        if (list.getModel().getSize() == 2) return;
        File file = new File("User\\feeds.rss");
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            for (int i = 2; i < list.getModel().getSize(); i++) {
                RSSFeed feed = (RSSFeed) list.getModel().getElementAt(i);
                output.write(feed.getURL() + "\n");
            }
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String saveFileDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Select Dir...");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toString() + "\\";
        }
        return "";
    }

    public static void download(File file, String image, JProgressBar bar) {
        bar.setValue(0);
        URL url = null;
        BufferedOutputStream fOut = null;
        try {
            file.createNewFile();
            url = new URL(image);
            InputStream html = null;
            html = url.openStream();
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[32 * 1024];
            int bytesRead = 0;
            while ((bytesRead = html.read(buffer)) != -1) {
                bar.setValue(bar.getValue() + bytesRead);
                fOut.write(buffer, 0, bytesRead);
            }
            html.close();
            fOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String loadUserName() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("User\\user.me"));
            String line = in.readLine();
            in.close();
            return line;
        } catch (IOException e) {
            System.out.println(e);
            return "";
        }
    }

    public static String loadPass() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("User\\user.me"));
            in.readLine();
            String line = in.readLine();
            in.close();
            return line;
        } catch (IOException e) {
            System.out.println(e);
            return "";
        }
    }

    public static void saveData(String path, String[] data) {
        File file = new File(path);
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < data.length; i++) output.write(data[i] + "\n");
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveUser(String name, String pass) {
        File file = new File("User\\user.me");
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(name + "\n" + pass);
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveYouTubeUser(String name, String pass, boolean b) {
        File file = new File("User\\user_youtube.me");
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(name + "\n" + pass + "\n");
            if (b) {
                output.write("true");
            } else {
                output.write("false");
            }
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String[] getYoutubeLogin() {
        String[] s = new String[3];
        try {
            BufferedReader in = new BufferedReader(new FileReader("User\\user_youtube.me"));
            s[0] = in.readLine();
            s[1] = in.readLine();
            s[2] = in.readLine();
            in.close();
            return s;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveTmpFile(File file, String image) {
        URL url = null;
        BufferedOutputStream fOut = null;
        try {
            file.createNewFile();
            url = new URL(image);
            InputStream html = null;
            html = url.openStream();
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[32 * 1024];
            int bytesRead = 0;
            while ((bytesRead = html.read(buffer)) != -1) {
                fOut.write(buffer, 0, bytesRead);
            }
            html.close();
            fOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void creatDirs() {
        File file = new File("TMP");
        file.mkdir();
        file = new File("User");
        file.mkdir();
        file = new File("User\\feeds.rss");
        if (!file.exists()) try {
            file.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
        file = new File("User\\picasa.opw");
        if (!file.exists()) try {
            file.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
        file = new File("User\\docs.opw");
        if (!file.exists()) try {
            file.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
