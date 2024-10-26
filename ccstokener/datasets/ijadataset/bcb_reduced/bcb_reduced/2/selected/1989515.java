package com.jguigen.standard;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.util.zip.*;
import java.text.DateFormat;
import java.text.Format.*;
import java.text.*;
import java.util.jar.*;
import javax.swing.text.*;
import java.lang.Object;
import java.awt.event.*;
import java.net.URLConnection;
import javax.swing.JScrollPane;
import java.util.*;
import java.sql.*;
import java.util.logging.*;

/**
 * Description - History - Show the user History.html if it is more recent that the last they saw
 *  Each new version of JavaDeliver comes with an updated version of History.html.  This class looks
 * up the date of the last History.html the user has viewed (in the USER table) and if the one in
 * the jar file is newer, it offers them the option of viewing the file.
 * Part of the DayBegin routine that carries out several tasks like this.
 * When called from the main menu the view is forced to be true.
 * The class checks first for a history.html file in the current folder (for development and
 * testing.  If it doesn't find that, it looks in the jar file the program is running from
 * @author  - Hale Pringle
 * @version - 1.0
 *
 *
 * Took some of the code for reading files from inside archives from
 * javaworld "Modify archives, Part 1"
 * Allen Holub
 *
 * The StreamEditorPane code came from COre JFC by Kim Topley
 *
 * Needs a print button
 */
public class History {

    private JFrame f;

    private com.jguigen.standard.Model mod;

    private com.jguigen.standard.UserlogdataTableModel utmStd;

    private Logger logger;

    static java.util.ResourceBundle modelRb;

    private com.jguigen.standard.I18nWrapper getStr;

    /**
     *
     * Description - main method for testing History class
     *
     * @param - non used
     * @return - nothing
     */
    public static void main(String[] args) {
        java.util.Date oldDate = new java.util.Date();
        com.jguigen.standard.Model mod;
        com.jguigen.standard.DBMaster dbc;
        mod = new com.jguigen.standard.Model();
        modelRb = mod.getResourceBdl("JguigenmodelRb");
        try {
            dbc = new com.jguigen.standard.DBMaster(mod);
        } catch (IOException ex) {
            System.out.println("DBMaster quit on IOException error");
        }
        History hist = new History(mod, oldDate, true);
        String ret = hist.execute();
        hist = null;
    }

    /**
     *
     * Description - Constructor for the History class
     *
     * @param - model, Model class passed to almost all the routines
     * @param - Date - the date the user last viewed the history.html file
     * @param - boolean - isForceView - should the user be asked if they want to see the history.html
     *          file (false) or should they be shown the file immediately (true)
     * @return  - nothing
     * @exception - none
     *
     */
    public History(com.jguigen.standard.Model model, java.util.Date inDate, boolean isForceView) {
        mod = model;
        logger = mod.getLogger();
        getStr = new I18nWrapper();
        modelRb = model.getResourceBdl("JguigenmodelRb");
        String tString = model.getRbString(modelRb, "History file not found in file system", "jguigenmodel");
        tString = model.getRbString(modelRb, "There is a new version of the Program.  View this Change log?", "jguigenmodel");
        tString = model.getRbString(modelRb, "(Note: it will take a few seconds to load)", "jguigenmodel");
        tString = model.getRbString(modelRb, "History Log", "jguigenmodel");
        tString = model.getRbString(modelRb, "Failed to open file", "jguigenmodel");
        tString = model.getRbString(modelRb, "Unexpected exception:", "jguigenmodel");
        tString = model.getRbString(modelRb, "history.htm file not found in jar", "jguigenmodel");
        long chkDate = inDate.getTime();
        GregorianCalendar checkDate = new GregorianCalendar();
        checkDate.setTimeInMillis(chkDate);
        utmStd = model.getStandardUserlogdataTableModel();
        try {
            URL imgPath1;
            String imgPath2, imgPath3, iniPath;
            imgPath1 = History.class.getProtectionDomain().getCodeSource().getLocation();
            imgPath2 = imgPath1.toString();
            imgPath3 = imgPath2;
            tString = File.separator;
            String imgPath4 = imgPath2.trim();
            String path_to_file = "history.htm";
            String jarFileName = model.getOneWordProjectName().trim() + ".jar";
            imgPath4 = imgPath4.replace('/', File.separatorChar);
            String userdir = imgPath4;
            userdir = userdir.substring(5);
            String jarPath = userdir;
            boolean isInJar = false;
            URL file_url;
            URL cache_url;
            Locale loc = model.getCurrentLocale();
            String language = loc.getLanguage();
            String country = loc.getCountry();
            String path = jarPath + File.separator;
            String path_language_country = language + "_" + country;
            String path_language = language;
            if (imgPath3.indexOf(".jar") > 0) {
                isInJar = true;
            }
            if (userdir.substring(0, 1).equals(File.separator)) {
            } else {
                userdir = File.separator + userdir;
            }
            userdir = model.replace(userdir, "%20", " ");
            long lDay = 0;
            boolean found = false;
            String fileName = "history.htm";
            String tmpFile = "";
            InputStream is;
            if (!(isInJar)) {
                String file_l_c = userdir + "History" + File.separator + path_language_country + File.separator + fileName;
                String file_l = userdir + "History" + File.separator + path_language + File.separator + fileName;
                String file = userdir + "History" + File.separator + fileName;
                if (new File(file_l_c).exists()) {
                    userdir = "file:" + file_l_c;
                } else if (new File(file_l).exists()) {
                    userdir = "file:" + file_l;
                } else if (new File(file).exists()) {
                    userdir = "file:" + file;
                } else {
                    System.out.println(model.getRbString(modelRb, "History file not found in file system", "jguigenmodel"));
                    userdir = "";
                    return;
                }
                cache_url = new URL(userdir);
                file_url = new URL(cache_url, fileName);
                URLConnection connection = (URLConnection) (file_url.openConnection());
                lDay = connection.getLastModified();
                boolean isFound = new FileExists().FileExists(model.getWorkingPath(), fileName, true, "History");
                if (!isFound) {
                    model.alert("History file case is incorrect.  Fix for other Operating Systems.  Should be " + fileName);
                }
                found = true;
            } else {
                System.out.println("IN Jar");
                ZipEntry zen1 = null;
                ZipEntry zen2 = null;
                ZipEntry zen3 = null;
                String jar_l_c = "";
                String jar_l = "";
                try {
                    JarFile jar = new JarFile(userdir);
                    jar_l_c = "History" + File.separator + path_language_country + File.separator + fileName;
                    zen1 = jar.getEntry(jar_l_c);
                    jar_l = "History" + File.separator + path_language + File.separator + fileName;
                    zen2 = jar.getEntry(jar_l);
                    zen3 = jar.getEntry(fileName);
                } catch (Exception ex) {
                    model.alert("Error trying to find history.htm in jar file");
                }
                ;
                if (zen1 != null) {
                    tmpFile = jar_l_c;
                } else if (zen2 != null) {
                    tmpFile = jar_l;
                } else if (zen3 != null) {
                    tmpFile = fileName;
                } else {
                    System.out.println(model.getRbString(modelRb, "history.htm file not found in jar", "jguigenmodel"));
                }
                String tmpDir = "jar:" + userdir;
                cache_url = new URL(tmpDir + File.separator);
                file_url = new URL(cache_url, tmpFile);
                JarURLConnection connection = (JarURLConnection) (file_url.openConnection());
                lDay = connection.getLastModified();
                found = true;
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.connect();
                is = connection.getInputStream();
            }
            if (!found) {
                return;
            }
            GregorianCalendar historyFileDate = new GregorianCalendar();
            historyFileDate.setTimeInMillis(lDay);
            logger.info(getStr.getRbString(modelRb, "Date of History file:", "jguigenmodel") + " " + historyFileDate.getTime());
            logger.info(getStr.getRbString(modelRb, "(History file should change every time a new version of the app is released.)", "jguigenmodel"));
            if (!isForceView) {
                if ((historyFileDate.get(Calendar.DAY_OF_MONTH) == checkDate.get(Calendar.DAY_OF_MONTH) && historyFileDate.get(Calendar.MONTH) == checkDate.get(Calendar.MONTH) && historyFileDate.get(Calendar.YEAR) == checkDate.get(Calendar.YEAR) && historyFileDate.get(Calendar.MINUTE) == checkDate.get(Calendar.MINUTE))) {
                    return;
                } else {
                    Object[] options = { model.getRbString(modelRb, "Yes", "jguigenmodel"), model.getRbString(modelRb, "No", "jguigenmodel") };
                    int n = JOptionPane.showOptionDialog(f, model.getRbString(modelRb, "There is a new version of the Program.  View this Change log?", "jguigenmodel") + "\n " + model.getRbString(modelRb, "(Note: it will take a few seconds to load)", "jguigenmodel"), model.getRbString(modelRb, "History Log", "jguigenmodel"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (n == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            }
            java.util.Date outDate = historyFileDate.getTime();
            SimpleDateFormat formatterTsStd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String longNewDate = formatterTsStd.format(outDate);
            String values = "viewhistory_dttime = {ts '" + longNewDate + "'}";
            String userName = mod.getCurrentUser();
            int userId = mod.getCurrentUserId();
            utmStd.updateTable(Integer.toString(userId), values, "user_id", null, null);
            utmStd.fire();
            String sText = "";
            String aline;
            boolean addBreak = false;
            if (isInJar) {
                try {
                    JarURLConnection connection = (JarURLConnection) (file_url.openConnection());
                    is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    while ((aline = reader.readLine()) != null) {
                        if (aline.trim().equalsIgnoreCase("</HEAD>")) {
                            addBreak = true;
                        }
                        sText = sText + aline;
                        if (addBreak) {
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Streamreader failed");
                }
                ;
            } else {
                userdir = File.separator + userdir.substring(5);
                if (userdir.substring(0, 1).equals(File.separator)) {
                } else {
                    userdir = File.separator + userdir;
                }
                BufferedReader reader = new BufferedReader(new FileReader(new File(userdir)));
                while ((aline = reader.readLine()) != null) {
                    if (aline.trim().equalsIgnoreCase("</HEAD>")) {
                        addBreak = true;
                    }
                    sText = sText + aline;
                    if (addBreak) {
                    }
                }
            }
            try {
                f = new JFrame(model.getRbString(modelRb, "History File", "jguigenmodel"));
                com.jguigen.standard.SetIcon.setFrameIcon(f, "JGuiGen.gif");
                com.jguigen.standard.StreamEditorPane ep = new com.jguigen.standard.StreamEditorPane();
                JScrollPane js = new JScrollPane(ep);
                f.getContentPane().add(js);
                f.setSize(600, 400);
                java.awt.Point lastLocation = null;
                lastLocation = f.getLocation();
                if (lastLocation != null) {
                    lastLocation.translate(40, 100);
                }
                f.setLocation(lastLocation);
                f.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent evt) {
                        f.setVisible(false);
                        f.dispose();
                    }
                });
                ep.setTextInput(sText);
                if (!(sText == null) && sText.trim().length() > 0) {
                    ep.setCaretPosition(0);
                }
                f.setVisible(true);
            } catch (FileNotFoundException e) {
                System.out.println(model.getRbString(modelRb, "Failed to open file", "jguigenmodel") + " " + path_to_file);
                f.setVisible(false);
                f.dispose();
            } catch (IOException e) {
                System.out.println(model.getRbString(modelRb, "Error while reading file", "jguigenmodel") + " " + path_to_file);
                f.setVisible(false);
                f.dispose();
            } catch (Throwable t) {
                System.out.println(model.getRbString(modelRb, "Unexpected exception:", "jguigenmodel") + " " + t);
                f.setVisible(false);
                f.dispose();
            }
        } catch (MalformedURLException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     *
     * Description - path_for takes a package name (e.g. com.john) and returns a path (eg./com/john)
     *
     * @param class name in dot format
     * @return path name with slashes instead of dots
     *
     */
    private String path_for(String class_name) {
        String path_name = class_name.replace('.', File.separatorChar);
        return File.separator + path_name;
    }

    public String execute() {
        return "sucess";
    }
}
