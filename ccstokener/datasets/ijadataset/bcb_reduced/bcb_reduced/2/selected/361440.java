package net.sf.bluex.controller;

import java.awt.Font;
import net.sf.bluex.plugin.*;
import net.sf.bluex.components.ProtocolNotSupportedException;
import net.sf.bluex.components.FileAssociation;
import net.sf.bluex.components.Stack;
import net.sf.bluex.components.BlueXStatics;
import net.sf.bluex.plugin.Plugin;
import net.sf.bluex.plugin.PluginMetaData;
import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import net.sf.bluex.boundary.MapFileAssociation;
import net.sf.bluex.explorer.boundary.CopyFrame;
import net.sf.bluex.explorer.boundary.DeleteFrame;
import net.sf.bluex.parser.PluginParsingListener;
import net.sf.bluex.parser.PluginSaving;
import newComponents.GlobalParser;
import newComponents.components.ColorProfile;

/**
 *
 * @author Blue
 */
public class UsefulMethods {

    public static String getFileSize(long size) {
        return newComponents.components.UsefulMethods.getFileSize(size);
    }

    /**
     * This method will return a Icon object as per the filename is given. This method gets the icon from the current directory.
     * which was set previously.
     */
    public static Icon getIcon(String fileName) {
        return newComponents.components.UsefulMethods.getIcon(FileModule.ICON_FOLDER + FileModule.getIconFolderWithCurrentSet() + File.separatorChar + fileName);
    }

    public static Icon getIconFromIconSet(String fileName, int iconSetNumber) {
        return newComponents.components.UsefulMethods.getIcon(FileModule.ICON_FOLDER + iconSetNumber + File.separatorChar + fileName);
    }

    public static Image getImage(String fileName) {
        return newComponents.components.UsefulMethods.getImage(fileName);
    }

    /**
     * this method will find the unique name for the file or folder in a given folder
     */
    public static String getUniqueName(String fileName, java.io.File srcFile) {
        java.io.File[] loopFiles = FolderIntruder.getFilesFolders(srcFile);
        for (int i = 0; i < loopFiles.length; i++) {
            for (java.io.File tempFile : loopFiles) {
                if (fileName.equalsIgnoreCase(tempFile.getName())) {
                    fileName = "CopyOF " + fileName;
                }
            }
        }
        return fileName;
    }

    public static final int FILE_MODE = 1, FOLDER_MODE = 2;

    /**
     * this method will return true or false according to the file has been found or not
     * it takes two File Object and an integer value so that it can check for the folder and the file
     * respectively.
     * The two file object file1 and file2 could be understood as follows.
     * As file2 is searched in the file1's contents if found then false is returned.
     * The integer value could be FILE_MODE or FOLDER_MODE for different situations.
     * It also checks for the 2 platforms (i.e. for Windows and UNIX)
     */
    public static boolean fileExists(File file1, File file2, int mode) {
        boolean isUnix = BlueXStatics.isUnix();
        java.io.File[] allFiles = file1.listFiles();
        for (File tempFile : allFiles) {
            if (mode == FILE_MODE) {
                if (isUnix || tempFile.isFile()) {
                    if (!isUnix) {
                        if (file2.getName().equalsIgnoreCase(tempFile.getName())) return true;
                    } else if (file2.getName().equals(tempFile.getName())) return true;
                }
            } else if (mode == FOLDER_MODE) {
                if (isUnix || tempFile.isDirectory()) {
                    if (!isUnix) {
                        if (file2.getName().equalsIgnoreCase(tempFile.getName())) return true;
                    } else if (file2.getName().equals(tempFile.getName())) return true;
                }
            }
        }
        return false;
    }

    /**
     * it removes the Focus paint from the components which implements AbstractButton Class
     * @param p
     */
    public static void removeFocusable(javax.swing.JComponent p) {
        newComponents.components.UsefulMethods.removeFocusable(p);
    }

    /**
     * This method is used when we want the logo of Eighty_Coffee to be displayed.
     * It takes the color profile as argument, so that it can render itself
     * @param hlcp
     * @return
     */
    public static newComponents.HyperLink getLogo(ColorProfile hlcp) {
        if (hlcp == null) hlcp = new ColorProfile(java.awt.Color.LIGHT_GRAY, java.awt.Color.GRAY, null);
        newComponents.HyperLink lbLogo = new newComponents.HyperLink("", hlcp);
        lbLogo.setTextWithoutUnderline("E_C");
        lbLogo.setToolTipText("Click here to go the Eighty_Coffee home page.");
        Font font = null;
        lbLogo.addHyperLinkListener(new newComponents.events.HyperLinkListener() {

            public void hyperlinkClicked(newComponents.events.HyperLinkEvent he) {
                try {
                    java.net.URI uri = new java.net.URI("http://pratikabu.users.sourceforge.net");
                    java.awt.Desktop.getDesktop().browse(uri);
                } catch (Exception e) {
                }
            }
        });
        try {
            font = newComponents.components.UsefulMethods.getFont(FileModule.CONFIG_FOLDER + "\\BURECS__.TTF");
            font = font.deriveFont(java.awt.Font.BOLD, 23);
        } catch (Exception e) {
            font = Font.getFont(Font.DIALOG);
        }
        lbLogo.setFont(font);
        return lbLogo;
    }

    public static void requestOSToRun(File file) {
        if (file != null) {
            boolean open = true, risk = false;
            risk = getAssociatedPlugin(file) == null ? false : true;
            if (risk) {
                int choice = JOptionPane.showConfirmDialog(null, "Are you confirm to open from OS?\n\n" + "There exists a plugin which can open it.", "BlueX System", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) open = false;
            }
            if (open) {
                newComponents.components.UsefulMethods.requestOSToRun(file, BlueXStatics.getRunningCommand());
            }
        }
    }

    public static void openAssociatedFile(File file) {
        PluginMetaData pmd = getAssociatedPlugin(file);
        if (pmd != null) {
            try {
                Plugin pg = getSelectedPlugin(pmd);
                pg.openFileAssociationPlugin(new File[] { file });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "There is some problem while loading the plugin.");
                e.printStackTrace();
            }
        } else {
            requestOSToRun(file);
        }
    }

    public static Plugin getSelectedPlugin(PluginMetaData pmd) throws Exception {
        File jarFile = new File(FileModule.PLUGIN_FOLDER + File.separatorChar + pmd.getJarFileName());
        URL[] urls = new URL[pmd.getDependencies().size() + 1];
        urls[0] = jarFile.toURI().toURL();
        int i = 1;
        for (Dependency dep : pmd.getDependencies()) {
            jarFile = new File(FileModule.SHARED_FOLDER + File.separatorChar + dep.getJarFileName());
            urls[i++] = jarFile.toURI().toURL();
        }
        ClassLoader cl = new URLClassLoader(urls);
        Class c = Class.forName(pmd.getPluginClass(), true, cl);
        Plugin pg = (Plugin) c.newInstance();
        return pg;
    }

    public static void openHomePageinBrowser() {
        newComponents.components.UsefulMethods.openPageInBrowser("http://pratikabu.users.sourceforge.net");
    }

    public static Vector<PluginMetaData> getAllInstalledPlugins() {
        return getPluginsFromFile(FileModule.PLUGINS_DB);
    }

    public static Vector<PluginMetaData> getPluginsFromFile(String location) {
        PluginParsingListener ppl = new PluginParsingListener();
        GlobalParser.parse(new File(location), ppl);
        return ppl.getPluginMetaDatas();
    }

    public static Vector<PluginMetaData> getAllInstalledPlugins(int moduleType) {
        Vector<PluginMetaData> vectPlugins = new Vector<PluginMetaData>();
        for (PluginMetaData pmd : getAllInstalledPlugins()) {
            for (ModuleDetail md : pmd.getModuleInstalled()) if (md.getModuleType() == moduleType) vectPlugins.add(pmd);
        }
        return vectPlugins;
    }

    /**
     * Get the PluginMetaData object of the plugin named pluginName.
     * @param pluginName
     * @return
     */
    public static PluginMetaData getPluginByName(String pluginName) {
        for (PluginMetaData pmd : getAllInstalledPlugins()) if (pmd.getName().equals(pluginName)) return pmd;
        return null;
    }

    /**
     * get all the filetypes saved
     * @return
     */
    public static java.util.Vector<FileAssociation> getAllFileAssociations() {
        Vector<PluginMetaData> vectPMD = getAllInstalledPlugins(ModuleDetail.FILE_ASSOCIATION_PLUGIN);
        Vector<FileAssociation> vectFA = new Vector<FileAssociation>();
        for (PluginMetaData pmd : vectPMD) for (ModuleDetail md : pmd.getModuleInstalled()) for (String extension : md.getMappingExtension()) vectFA.add(new FileAssociation(pmd.getName(), extension));
        return vectFA;
    }

    /**
     * get all the filetypes saved
     * @return
     */
    public static java.util.Vector<FileAssociation> getAllFileAssociations(String extension) {
        Vector<FileAssociation> vectFA = new Vector<FileAssociation>();
        for (FileAssociation fa : getAllFileAssociations()) if (fa.getExtension().equalsIgnoreCase(extension)) vectFA.add(fa);
        return vectFA;
    }

    /**
     * @param file
     * @return The associated default plugin metadata else null if no association is found
     */
    public static PluginMetaData getAssociatedPlugin(File file) {
        String extension = getExtension(file);
        for (Entry<Object, Object> ent : BlueXStatics.fa.entrySet()) if (((String) ent.getKey()).equalsIgnoreCase(extension)) if (ent.getValue().equals("Operating System")) return null; else return getPluginByName(ent.getValue().toString());
        return null;
    }

    /**
     * @param file
     * @return All associated plugin else empty vector with size=0
     */
    public static Vector<PluginMetaData> getAssociatedPlugins(File file) {
        String extension = getExtension(file);
        Vector<PluginMetaData> vectPMD = new Vector<PluginMetaData>();
        for (FileAssociation fa : getAllFileAssociations()) if (fa.getExtension().equalsIgnoreCase(extension)) if (!fa.getPluginName().equals("Operating System")) vectPMD.add(getPluginByName(fa.getPluginName()));
        return vectPMD;
    }

    public static String getExtension(File file) {
        String extension = "";
        try {
            extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        } catch (IndexOutOfBoundsException ioobe) {
        }
        return extension;
    }

    public static final int HTTP_PROTOCOL = 1, FTP_PROTOCOL = 2, SFTP_PROTOCOL = 3;

    /**
     *
     * @param urlString the address of the source file
     * @param outputFile the location of the destination file
     * @return
     */
    public static boolean downloadFile(String urlString, String outputFile, int protocol) {
        InputStream is = null;
        File file = new File(outputFile);
        FileOutputStream fos = null;
        if (protocol == HTTP_PROTOCOL) {
            try {
                URL url = new URL(urlString);
                URLConnection ucnn = null;
                if (BlueXStatics.proxy == null || url.getProtocol().equals("file")) ucnn = url.openConnection(); else ucnn = url.openConnection(BlueXStatics.proxy);
                is = ucnn.getInputStream();
                fos = new FileOutputStream(file);
                byte[] data = new byte[4096];
                int offset;
                while ((offset = is.read(data)) != -1) {
                    fos.write(data, 0, offset);
                }
                return true;
            } catch (Exception ex) {
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        } else throw new ProtocolNotSupportedException("The protocol selected is not supported by this version of downloadFile() method.");
        return false;
    }

    public static Properties getProperties(String file) {
        Properties prop = null;
        FileReader fr = null;
        try {
            prop = new Properties();
            fr = new FileReader(file);
            prop.load(fr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (Exception e) {
            }
        }
        return prop;
    }

    public static boolean saveProperties(Properties prop, String file) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            prop.store(fw, "BlueX");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static boolean extractCompressedFile(String source, File destinationFile) {
        File sourceFile = new File(source);
        int BUFFER = Integer.parseInt(BlueXStatics.prop.getProperty("allocationUnit"));
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(sourceFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File newFile = new File(destinationFile, entryName);
                newFile.getParentFile().mkdir();
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                int count;
                byte data[] = new byte[BUFFER];
                File opFile = new File(destinationFile, entry.getName());
                FileOutputStream fos = new FileOutputStream(opFile);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean installPlugin(PluginMetaData newPMD, String source, Vector<PluginMetaData> vectNewPluginsDB) {
        File extracetedSource = new File(FileModule.EXTRACTED_TEMP_FOLDER + File.separatorChar + newPMD.getName());
        if (extracetedSource.exists() || extracetedSource.mkdir()) {
            if (extractCompressedFile(source, extracetedSource)) {
                try {
                    File sourceFile = new File(extracetedSource, newPMD.getJarFileName());
                    downloadFile(sourceFile.toURI().toURL().toString(), FileModule.PLUGIN_FOLDER + File.separatorChar + newPMD.getJarFileName(), HTTP_PROTOCOL);
                    File dependencyFolder = new File(FileModule.DEPENDENCIES_FOLDER + File.separatorChar + newPMD.getName());
                    if (dependencyFolder.exists() || dependencyFolder.mkdir()) {
                        for (Dependency dep : newPMD.getDependencies()) {
                            sourceFile = new File(extracetedSource, dep.getJarFileName());
                            if (!downloadFile(sourceFile.toURI().toURL().toString(), dependencyFolder.getAbsolutePath() + File.separatorChar + dep.getJarFileName(), HTTP_PROTOCOL)) return false;
                        }
                    }
                    for (ModuleDetail md : newPMD.getModuleInstalled()) {
                        if (md.getModuleType() == ModuleDetail.FILE_ASSOCIATION_PLUGIN) MapFileAssociation.mapFileAssociations(md.getMappingExtension(), newPMD.getName());
                    }
                    if (newPMD.isHelpAvailable()) {
                        File helpFolder = new File(extracetedSource, "help" + File.separatorChar + newPMD.getName());
                        Vector<File> copyDS = new Vector<File>(1);
                        copyDS.add(helpFolder);
                        copy(copyDS);
                        File destination = new File(FileModule.HELP_FOLDER + File.separatorChar + newPMD.getName());
                        Stack<File> st = new Stack<File>();
                        st.push(destination);
                        DeleteFrame df = new DeleteFrame(st, null);
                        if (df != null) {
                            df.doStartJob();
                            df.getT().join();
                        }
                        CopyFrame cf = CopyFrame.showCopyProgressDialog(destination.getParentFile(), null);
                        if (cf != null) cf.getT().join();
                    }
                    vectNewPluginsDB.add(newPMD);
                    PluginSaving.saveToDB(vectNewPluginsDB);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean installNewPlugin(PluginMetaData newPMD, String source) {
        return installPlugin(newPMD, source, getAllInstalledPlugins());
    }

    public static boolean installUpdateOfPlugin(PluginMetaData selectedPMD, String source) {
        int choice = JOptionPane.showConfirmDialog(null, "<html><body>Close all running instances of:<br/>" + "<b>" + selectedPMD.getName() + "</b><br/>" + "If no instance is running then click Yes else click No.<br/>" + "Have you closed all instances? Click Yes to conitnue.", "Close Plugin", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            Vector<PluginMetaData> vectInstalledPlugins = new Vector<PluginMetaData>(getAllInstalledPlugins());
            vectInstalledPlugins.remove(selectedPMD);
            return installPlugin(selectedPMD, source, vectInstalledPlugins);
        } else return false;
    }

    public static void copy(final Vector<File> copyDS) {
        ClipboardOwner fileOwner = new ClipboardOwner() {

            public void lostOwnership(Clipboard clipboard, Transferable contents) {
            }
        };
        Transferable trans = new Transferable() {

            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.javaFileListFlavor };
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(DataFlavor.javaFileListFlavor);
            }

            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                List<File> list = new Vector<File>();
                if (copyDS != null) {
                    for (File file : copyDS) {
                        list.add(file);
                    }
                }
                return list;
            }
        };
        java.awt.datatransfer.Clipboard clip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(trans, fileOwner);
    }
}
