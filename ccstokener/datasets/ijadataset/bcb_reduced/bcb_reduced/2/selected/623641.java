package plugin.installPlugs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import plugin.installPlugs.data.PlugData;
import eu.popeye.middleware.groupmanagement.management.WorkgroupManager;
import eu.popeye.middleware.pluginmanagement.plugin.PlugInDescriptor;
import eu.popeye.middleware.pluginmanagement.plugin.Plugin;
import eu.popeye.middleware.pluginmanagement.runtime.data.IPlugData;
import eu.popeye.networkabstraction.communication.basic.util.ExampleFileFilter;
import eu.popeye.networkabstraction.communication.basic.util.FileUtils;
import eu.popeye.networkabstraction.communication.basic.util.PopeyeException;

public class PluginFramework extends Plugin {

    private static final long serialVersionUID = 1L;

    private JMenuBar jJMenuBar = null;

    private JMenu jMenuFile = null;

    private JMenuItem jMenuItemInstallPluginFS = null;

    private JPanel jPanelMain = null;

    private JMenuItem jMenuItemInstallPluginWeb = null;

    private JMenu jMenuHelp = null;

    private JMenuItem jMenuItemAbout = null;

    private JMenuItem jMenuItemInstalledPluginsList = null;

    private JMenu jMenuList = null;

    private JMenuItem jMenuItemStartedPluginsList = null;

    private JMenu jMenuStartedList = null;

    private JMenuItem jMenuItemExit = null;

    private PlugData plugData;

    private WorkgroupManager wgManager = null;

    private JMenuItem jMenuItemInstallPluginPeer = null;

    private JMenu jMenuInstall = null;

    private static final String PLUGIN_DIRECTORY = "downloadedPlugins";

    private PluginFrameworkDialogs dialogs;

    public String Prova() {
        return "works!!!";
    }

    public void init() {
        this.setName("Install Plugins");
        this.add(getJPanelMain());
        this.add(getJJMenuBar());
        this.setBounds(45, 25, 462, 341);
        this.pluginManager = this.getPlugManager();
        this.wgManager = this.getPlugManager().getWorkGroupManager();
        plugDataWin.getMainPanel().add("Install Plugins", this);
        dialogs = new PluginFrameworkDialogsSwing(this);
    }

    private void doExit() {
        pluginManager.deletedPlugin(this);
    }

    /**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJMenuFile());
            jJMenuBar.add(getJMenuInstall());
            jJMenuBar.add(getJMenuList());
            jJMenuBar.add(getJMenuStartedList());
            jJMenuBar.add(getJMenuHelp());
        }
        return jJMenuBar;
    }

    /**
	 * This method initializes jMenuFile	
	 * 	
	 * @return javax.swing.JMenu	
	 */
    private JMenu getJMenuFile() {
        if (jMenuFile == null) {
            jMenuFile = new JMenu();
            jMenuFile.setText("File");
            jMenuFile.setMnemonic(KeyEvent.VK_F);
            jMenuFile.add(getJMenuItemExit());
        }
        return jMenuFile;
    }

    /**
	 * This method initializes jMenuItemInstallPluginFS	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemInstallPluginFS() {
        if (jMenuItemInstallPluginFS == null) {
            jMenuItemInstallPluginFS = new JMenuItem();
            jMenuItemInstallPluginFS.setText("Plugin from FS...");
            jMenuItemInstallPluginFS.setMnemonic(KeyEvent.VK_F);
            jMenuItemInstallPluginFS.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    ExampleFileFilter filter = new ExampleFileFilter();
                    filter.addExtension("jar");
                    filter.setDescription("JAR files");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        System.out.println("You chose to open this file: " + chooser.getSelectedFile().getAbsoluteFile());
                        installPluginFromFS(chooser.getSelectedFile().getAbsoluteFile());
                    }
                }
            });
        }
        return jMenuItemInstallPluginFS;
    }

    private void installPluginFromFS(File file) {
        String filename = file.getName();
        File destFile = new File("." + File.separator + PLUGIN_DIRECTORY + File.separator + filename);
        if (destFile.exists()) {
            if (dialogs != null) if (!dialogs.overwriteFile(filename)) {
                return;
            }
        }
        try {
            FileUtils.copyFile(file, destFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlugInDescriptor selectedPd = null;
        ;
        try {
            selectedPd = pluginManager.installPlugin(destFile);
        } catch (PopeyeException e) {
            dialogs.showErrorMessage(e.getMessage());
        }
        try {
            pluginManager.instantiatePlugin(selectedPd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
        } catch (PopeyeException e) {
            dialogs.showErrorMessage(e.getMessage());
            dialogs.showErrorMessage(e.getStackTrace().toString());
        }
    }

    private void installPluginFromWWW(URL url) {
        DownloadThread dt = new DownloadThread(url);
        dt.start();
    }

    private class DownloadThread extends Thread {

        URL url = null;

        public DownloadThread(URL url) {
            this.url = url;
        }

        public void run() {
            String urlString = url.toString();
            int sep = urlString.lastIndexOf("/");
            String filename = urlString.substring(sep + 1);
            File destFile = new File(PLUGIN_DIRECTORY + File.separator + filename);
            if (destFile.exists()) {
                if (!dialogs.overwriteFile(filename)) {
                    return;
                }
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(destFile);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            System.out.println(destFile.toString());
            try {
                URLConnection uc = url.openConnection();
                InputStream in = uc.getInputStream();
                ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(null, "Downloading " + filename, in);
                ProgressMonitor pm = pmis.getProgressMonitor();
                pm.setMaximum(uc.getContentLength());
                int num = 0;
                byte b[] = new byte[100];
                try {
                    while ((num = pmis.read(b)) != -1) {
                        fos.write(b, 0, num);
                    }
                    fos.close();
                    pmis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            try {
                pluginManager.installPlugin(destFile);
            } catch (PopeyeException e) {
                dialogs.showErrorMessage(e.getMessage());
            }
        }
    }

    /**
	 * This method initializes jPanelMain	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getJPanelMain() {
        if (jPanelMain == null) {
            jPanelMain = new JPanel();
            jPanelMain.setLayout(new BorderLayout());
        }
        return jPanelMain;
    }

    /**
	 * This method initializes jMenuItemInstallPluginWeb	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemInstallPluginWeb() {
        if (jMenuItemInstallPluginWeb == null) {
            jMenuItemInstallPluginWeb = new JMenuItem();
            jMenuItemInstallPluginWeb.setText("Plugin from WWW...");
            jMenuItemInstallPluginWeb.setMnemonic(KeyEvent.VK_W);
            jMenuItemInstallPluginWeb.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String s = dialogs.loadFromURL();
                    try {
                        installPluginFromWWW(new URL(s));
                    } catch (MalformedURLException e1) {
                        dialogs.showErrorMessage("Malformed URL");
                    }
                }
            });
        }
        return jMenuItemInstallPluginWeb;
    }

    /**
	 * This method initializes jMenuHelp	
	 * 	
	 * @return javax.swing.JMenu	
	 */
    private JMenu getJMenuHelp() {
        if (jMenuHelp == null) {
            jMenuHelp = new JMenu();
            jMenuHelp.setText("Help");
            jMenuHelp.setMnemonic(KeyEvent.VK_H);
            jMenuHelp.add(getJMenuItemAbout());
        }
        return jMenuHelp;
    }

    /**
	 * This method initializes jMenuItemAbout	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemAbout() {
        if (jMenuItemAbout == null) {
            jMenuItemAbout = new JMenuItem();
            jMenuItemAbout.setText("About...");
            jMenuItemAbout.setMnemonic(KeyEvent.VK_A);
            jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    dialogs.about();
                }
            });
        }
        return jMenuItemAbout;
    }

    /**
	 * This method initializes jMenuItemInstalledPluginsList	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemInstalledPluginsList() {
        if (jMenuItemInstalledPluginsList == null) {
            jMenuItemInstalledPluginsList = new JMenuItem();
            jMenuItemInstalledPluginsList.setText("Installed plugins");
            jMenuItemInstalledPluginsList.setMnemonic(KeyEvent.VK_I);
            jMenuItemInstalledPluginsList.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    startInstalledPlugin();
                }
            });
        }
        return jMenuItemInstalledPluginsList;
    }

    /**
	 * This method initializes jMenuItemStartedPluginsList	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemStartedPluginsList() {
        if (jMenuItemStartedPluginsList == null) {
            jMenuItemStartedPluginsList = new JMenuItem();
            jMenuItemStartedPluginsList.setText("Started plugins");
            jMenuItemStartedPluginsList.setMnemonic(KeyEvent.VK_S);
            jMenuItemStartedPluginsList.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    startStartedPlugins();
                }
            });
        }
        return jMenuItemStartedPluginsList;
    }

    /**
	 * This method initializes jMenuList	
	 * 	
	 * @return javax.swing.JMenu	
	 */
    private JMenu getJMenuStartedList() {
        if (jMenuStartedList == null) {
            jMenuStartedList = new JMenu();
            jMenuStartedList.setText("Started Plugins");
            jMenuStartedList.setMnemonic(KeyEvent.VK_S);
            jMenuStartedList.add(getJMenuItemStartedPluginsList());
        }
        return jMenuStartedList;
    }

    /**
	 * This method initializes jMenuList	
	 * 	
	 * @return javax.swing.JMenu	
	 */
    private JMenu getJMenuList() {
        if (jMenuList == null) {
            jMenuList = new JMenu();
            jMenuList.setText("List");
            jMenuList.setMnemonic(KeyEvent.VK_L);
            jMenuList.add(getJMenuItemInstalledPluginsList());
        }
        return jMenuList;
    }

    /**
	 * This method initializes jMenuItemExit	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemExit() {
        if (jMenuItemExit == null) {
            jMenuItemExit = new JMenuItem();
            jMenuItemExit.setText("Exit");
            jMenuItemExit.setMnemonic(KeyEvent.VK_E);
            jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    doExit();
                }
            });
        }
        return jMenuItemExit;
    }

    /**
	 * This method initializes jMenuItemInstallPluginPeer	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
    private JMenuItem getJMenuItemInstallPluginPeer() {
        if (jMenuItemInstallPluginPeer == null) {
            jMenuItemInstallPluginPeer = new JMenuItem();
            jMenuItemInstallPluginPeer.setText("Plugin from another peer");
            jMenuItemInstallPluginPeer.setMnemonic(KeyEvent.VK_P);
            jMenuItemInstallPluginPeer.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    installPluginFromRemotePeer();
                }
            });
        }
        return jMenuItemInstallPluginPeer;
    }

    /**
	 * Shows a dialog window with local installed plugins and creates an
	 * instance of the selected plugin.
	 *
	 */
    private void startInstalledPlugin() {
        ArrayList pdList = pluginManager.getInstalledPlugins();
        Object[] pdArray = pdList.toArray();
        if (pdArray.length == 0) {
            dialogs.showInfoMessage("No installed plugin available!");
        } else {
            PlugInDescriptor selectedPd = (PlugInDescriptor) PluginListDialog.showDialog(null, jMenuItemInstalledPluginsList, "Select a plugin:", "Installed plugins list", "Start!", pdArray, pdArray[0], ((PlugInDescriptor) pdArray[0]).toString() + "   ");
            if (selectedPd != null) {
                System.out.println("You selected the plugin \"" + selectedPd.toString() + "\"");
                try {
                    pluginManager.instantiatePlugin(selectedPd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
                } catch (PopeyeException e) {
                    dialogs.showErrorMessage(e.getMessage());
                    dialogs.showErrorMessage(e.getStackTrace().toString());
                }
            }
        }
    }

    /**
	 * Shows a dialog window with local started plugins and creates an
	 * instance of the selected plugin.
	 *
	 */
    private void startStartedPlugins() {
        ArrayList pdList = pluginManager.getStartedPlugins();
        Object[] pdArray = pdList.toArray();
        if (pdArray.length == 0) {
            dialogs.showInfoMessage("No installed plugin available!");
        } else {
            PlugInDescriptor selectedPd = (PlugInDescriptor) PluginListDialog.showDialog(null, jMenuItemInstalledPluginsList, "Select a plugin:", "Installed plugins list", "Start!", pdArray, pdArray[0], ((PlugInDescriptor) pdArray[0]).toString() + "   ");
            if (selectedPd != null) {
                System.out.println("You selected the plugin \"" + selectedPd.toString() + "\"");
                try {
                    pluginManager.instantiatePlugin(selectedPd, plugDataWin.getMainPanel(), plugDataWin.getPlugManager().getWorkspace().getGroup().getWorkgroupName());
                } catch (PopeyeException e) {
                    dialogs.showErrorMessage(e.getMessage());
                    dialogs.showErrorMessage(e.getStackTrace().toString());
                }
            }
        }
    }

    /**
	 * Shows a dialog window with remote available plugins and installs the selected plugin. 
	 */
    private void installPluginFromRemotePeer() {
        System.out.println("RemotePluginsList");
        ArrayList pdList = null;
        try {
            pdList = pluginManager.getRemoteAvailablePlugins();
        } catch (PopeyeException e) {
            dialogs.showErrorMessage(e.getMessage());
        }
        Object[] pdArray = pdList.toArray();
        if (pdArray.length == 0) {
            dialogs.showInfoMessage("No remote plugin available!");
        } else {
            String selectedName = dialogs.selectPlugin(pdList);
            if (selectedName != null) {
                System.out.println("You selected the plugin \"" + selectedName + "\"");
                File file = requestRemotePluginFile(selectedName);
                if (file != null) {
                    try {
                        pluginManager.installPlugin(file);
                    } catch (PopeyeException e) {
                        dialogs.showErrorMessage(e.getMessage());
                    }
                }
            }
        }
    }

    private File requestRemotePluginFile(String name) {
        return null;
    }

    /**
	 * This method initializes jMenuInstall	
	 * 	
	 * @return javax.swing.JMenu	
	 */
    private JMenu getJMenuInstall() {
        if (jMenuInstall == null) {
            jMenuInstall = new JMenu();
            jMenuInstall.setText("Install");
            jMenuInstall.setMnemonic(KeyEvent.VK_I);
            jMenuInstall.add(getJMenuItemInstallPluginFS());
            jMenuInstall.add(getJMenuItemInstallPluginWeb());
            jMenuInstall.add(getJMenuItemInstallPluginPeer());
        }
        return jMenuInstall;
    }
}
