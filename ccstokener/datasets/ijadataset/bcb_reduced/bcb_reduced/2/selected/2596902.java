package nl.BobbinWork.diagram.gui;

import static nl.BobbinWork.bwlib.gui.Localizer.*;
import java.awt.Component;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import nl.BobbinWork.bwlib.gui.LocaleMenuItem;

/**
 * A menu that lets a user open a web page as a stream. Most menu items specify
 * predefined URLs of sample diagrams. One item launches a dialog allowing a
 * user to enter a URL.
 * 
 * @author J. Pol
 * 
 */
@SuppressWarnings("serial")
public class SampleMenu extends JMenu {

    private static final String REVISIONED_URL = "http://bobbinwork.googlecode.com/svn/!svn/bc/569/wiki/diagrams/";

    private static final String LATEST_URL = "http://bobbinwork.googlecode.com/svn/wiki/diagrams/";

    private static final String[] SAMPLE_URLS = new String[] { "stars.xml", "trapgevel.xml", "snow.xml", "plaits.xml", "flanders.xml", "flandersPea.xml", "braid-half-stitch.xml", "braid-chaos.xml", "braid-row-cloth-row-half-stitch.xml", "snake-cloth.xml", "snake-cloth-footside.xml", "snake-half.xml" };

    /**
   * Handed down to dialogs.
   */
    private final Component parent;

    private final ActionListener externalActionListener;

    /**
   * Creates an inputStream from the specified URL.
   * 
   * @param event
   * 
   * @param url
   *          selected or entered by the user
   * @return
   */
    private void createInputStream(ActionEvent event, String url) {
        try {
            event.setSource((new URL(url)).openStream());
            externalActionListener.actionPerformed(event);
        } catch (Exception exception) {
            final String message = url + NEW_LINE + exception.getClass().getName() + NEW_LINE + exception.getLocalizedMessage();
            JOptionPane.showMessageDialog(parent, message, getString("Load_error_caption"), JOptionPane.ERROR_MESSAGE);
        }
    }

    static final String NEW_LINE = System.getProperty("line.separator");

    /**
   * @param parent
   *          handed down to dialogs
   * @param externalActionListener
   *          triggered when an InputStream is created from a user selected URL
   */
    public SampleMenu(Component parent, ActionListener externalActionListener) {
        super();
        applyStrings(this, "MenuFile_LoadSample");
        this.externalActionListener = externalActionListener;
        this.parent = parent;
        add(createItems(REVISIONED_URL, "MenuFile_StableSample"));
        add(createItems(LATEST_URL, "MenuFile_LatestSample"));
        add(createDownloadOther());
    }

    private JMenu createItems(String path, String key) {
        JMenu menu = new JMenu();
        applyStrings(menu, key);
        for (final String f : SAMPLE_URLS) {
            menu.add(createItem(path + f));
        }
        return menu;
    }

    private JMenuItem createItem(String url) {
        final JMenuItem jMenuItem;
        jMenuItem = new JMenuItem(url);
        jMenuItem.setActionCommand(url);
        jMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                createInputStream(event, event.getActionCommand());
            }
        });
        return jMenuItem;
    }

    private JMenuItem createDownloadOther() {
        final Component parent = this.parent;
        final JMenuItem jMenuItem = new LocaleMenuItem("MenuFile_ChooseSample");
        jMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                final String url = (String) JOptionPane.showInputDialog(parent, "url", "http://");
                createInputStream(event, url);
            }
        });
        return jMenuItem;
    }
}
