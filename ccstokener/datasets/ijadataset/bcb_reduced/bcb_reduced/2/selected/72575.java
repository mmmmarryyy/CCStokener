package com.vessosa.g15lastfmplayer;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.TwilightSkin;
import com.melloware.jintellitype.JIntellitype;
import com.vessosa.g15lastfmplayer.controller.Controller;
import com.vessosa.g15lastfmplayer.model.MainModel;
import com.vessosa.g15lastfmplayer.util.Config;
import com.vessosa.g15lastfmplayer.util.InstanceChecker;
import com.vessosa.g15lastfmplayer.view.G15KeysImplementationView;
import com.vessosa.g15lastfmplayer.view.LCDScreen;
import com.vessosa.g15lastfmplayer.view.RadioSearchScreen;

/***
 * 
 * @author Luiz Vessosa
 * 
 */
public class G15LastfmPlayer {

    private static final Logger LOGGER = Logger.getLogger(G15LastfmPlayer.class);

    private static RadioSearchScreen radioSearchScreen;

    private static Controller controller;

    private static G15KeysImplementationView g15MediaKeys;

    public static void main(String[] args) {
        new InstanceChecker();
        LOGGER.debug("-------------------------------");
        LOGGER.debug("G15Lastfm Player v" + getVersion() + " started");
        LOGGER.debug("-------------------------------");
        LOGGER.debug("java.vendor=" + System.getProperty("java.vendor"));
        LOGGER.debug("java.version=" + System.getProperty("java.version"));
        LOGGER.debug("java.home=" + System.getProperty("java.home"));
        LOGGER.debug("os.name=" + System.getProperty("os.name"));
        LOGGER.debug("os.arch=" + System.getProperty("os.arch"));
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                SubstanceLookAndFeel.setSkin(new TwilightSkin());
            }
        });
        checkForArchiteture();
        Config.initConfig();
        if (Config.getValueAsBoolean(Config.CHECK_FOR_UPDATES_ON_STARTUP)) checkForUpdates();
        getController().addModel(new MainModel());
        getG15MediaKeys();
        registerMediaKeys();
        getRadioSearchScreen();
        LCDScreen lcdScreen = new LCDScreen(getController());
        getController().addView(lcdScreen);
        try {
            lcdScreen.init();
        } catch (Exception e) {
            LOGGER.debug(e);
        }
        LCDScreen.stopLCD();
        JIntellitype.getInstance().cleanUp();
        System.exit(0);
    }

    private static void checkForArchiteture() {
        if (!System.getProperty("os.arch").equals("x86")) {
            LOGGER.debug("User trying to execute applet in a x64 jvm, exiting");
            JOptionPane.showMessageDialog(null, "You are executing this applet in a x64 JVM environment.\nPlease download and install Java VM x32 at www.java.com and try again.", "G15LastfmPlayer", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private static void checkForUpdates() {
        LOGGER.debug("Checking for Updates");
        new Thread() {

            @Override
            public void run() {
                String lastVersion = null;
                try {
                    URL projectSite = new URL("http://code.google.com/p/g15lastfm/");
                    URLConnection urlC = projectSite.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlC.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.contains("<strong>Current version:")) {
                            lastVersion = inputLine;
                            break;
                        }
                    }
                    in.close();
                    if (lastVersion != null && lastVersion.length() > 0) {
                        lastVersion = lastVersion.substring(lastVersion.indexOf("Current version:") + 16);
                        lastVersion = lastVersion.substring(0, lastVersion.indexOf("</strong>")).trim();
                        LOGGER.debug("last Version=" + lastVersion);
                    }
                    if (lastVersion.equals(getVersion())) LOGGER.debug("Not necessary to update"); else {
                        LOGGER.debug("New update found!");
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                if (JOptionPane.showConfirmDialog(null, "New version of G15Lastfm is available to download!", "New Update for G15Lastfm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                    LOGGER.debug("User choose to update, opening browser.");
                                    Desktop desktop = Desktop.getDesktop();
                                    try {
                                        desktop.browse(new URI("http://code.google.com/p/g15lastfm/"));
                                    } catch (IOException e) {
                                        LOGGER.debug(e);
                                    } catch (URISyntaxException e) {
                                        LOGGER.debug(e);
                                    }
                                } else {
                                    LOGGER.debug("User choose to not update.");
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    LOGGER.debug(e);
                }
            }
        }.start();
    }

    public static String getVersion() {
        String version = "devMachine";
        URL url = ClassLoader.getSystemResource("resources/version.txt");
        if (url == null) {
            url = ClassLoader.getSystemResource("version.txt");
        }
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File(url.toURI().getPath())));
            version = br.readLine();
            br.close();
        } catch (FileNotFoundException e) {
            LOGGER.debug(e);
        } catch (URISyntaxException e) {
            LOGGER.debug(e);
        } catch (IOException e) {
            LOGGER.debug(e);
        }
        return version;
    }

    public static void exitApplication() {
        LCDScreen.stopLCD();
    }

    public static RadioSearchScreen getRadioSearchScreen() {
        if (radioSearchScreen == null) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    radioSearchScreen = new RadioSearchScreen(getController());
                    getController().addView(radioSearchScreen);
                }
            });
        }
        return radioSearchScreen;
    }

    private static Controller getController() {
        if (controller == null) {
            controller = new Controller();
        }
        return controller;
    }

    public static G15KeysImplementationView getG15MediaKeys() {
        if (g15MediaKeys == null) {
            g15MediaKeys = new G15KeysImplementationView(getController());
            getController().addView(g15MediaKeys);
        }
        return g15MediaKeys;
    }

    public static void registerMediaKeys() {
        try {
            JIntellitype.getInstance();
            LOGGER.debug("Intellitype is supported? " + JIntellitype.isJIntellitypeSupported());
            JIntellitype.getInstance().addIntellitypeListener(getG15MediaKeys());
        } catch (Exception e) {
            LOGGER.debug(e);
        }
    }
}
