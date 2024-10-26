package com.mockturtlesolutions.snifflib.guitools.components;

import java.io.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.net.URL;
import javax.swing.*;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.Image;
import java.awt.GridLayout;
import java.awt.Component;

/**
Serve icons bases on the associations in the configuration file. 
*/
public class IconServer {

    private String usrConfigFile;

    private String usrIconDir;

    private DomainNameTree iconMap;

    private int stdwidth;

    private int stdheight;

    private int scaling_mode;

    public static final int FIT_HEIGHT = 0;

    public static final int FIT_WIDTH = 1;

    public static final int FIT_BOTH = 2;

    public static final int FIT_NONE = 3;

    public IconServer() {
        this((String) System.getProperty("user.home").concat(File.separator).concat(".mydomainiconmappings"), (String) System.getProperty("user.home").concat(File.separator).concat(".mydomainicons"));
    }

    /**
	Serve icons bases on the associations in the configuration file.
	*/
    public IconServer(String iconmappings, String icondir) {
        this.usrConfigFile = iconmappings;
        this.usrIconDir = icondir;
        this.iconMap = new DomainNameTree();
        this.processRegistry();
        this.scaling_mode = IconServer.FIT_HEIGHT;
        this.stdwidth = 15;
        this.stdheight = 15;
    }

    /**
	Get path to the default image repository.
	*/
    public String getIconRepository() {
        return (this.usrIconDir);
    }

    /**
	Set path to the default image repository(directory).
	*/
    public void setIconRepository(String path) {
        this.usrIconDir = path;
    }

    public void setConfigFile(String file) {
        this.usrConfigFile = file;
    }

    public String getConfigFile() {
        return (this.usrConfigFile);
    }

    public void setScalingMethod(int t) {
        this.scaling_mode = t;
    }

    public void setIconWidth(int x) {
        this.stdwidth = x;
    }

    public void setIconHeight(int x) {
        this.stdheight = x;
    }

    public String getImagePathForDomain(String domain) {
        return (this.iconMap.getImageFile(domain));
    }

    /**
	Look up the domain name in the registry and create a new icon using the 
	image specified there.
	*/
    public ImageIcon getIconForDomain(String domain) {
        return (this.iconMap.getImage(domain));
    }

    /**
	Returns a tree of icon mappings.
	*/
    public DomainNameTree getIconMap() {
        return (this.iconMap);
    }

    /**
	Set a tree of icon mappings.
	*/
    public void setIconMap(DomainNameTree x) {
        this.iconMap = x;
    }

    public void saveToRegistry() {
        this.saveToRegistry(this.iconMap);
    }

    /**
	Saves the current icon mappings to the registry.
	*/
    public void saveToRegistry(DomainNameTree iconmap) {
        File iconregistry = new File(this.usrConfigFile);
        if (iconregistry.exists()) {
            BufferedWriter output = null;
            try {
                output = new BufferedWriter(new FileWriter(this.usrConfigFile));
                Set domains = iconmap.domainSet();
                Iterator iter = domains.iterator();
                String domain, imagePath;
                while (iter.hasNext()) {
                    domain = (String) iter.next();
                    imagePath = iconmap.getImageFile(domain);
                    output.write(domain + "\t" + imagePath + "\n");
                }
                output.close();
                this.processRegistry();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            int s = JOptionPane.showConfirmDialog(null, "Create icon mapping file " + this.usrConfigFile + "?", "Icon mapping file does not exist!", JOptionPane.YES_NO_CANCEL_OPTION);
            if (s == JOptionPane.YES_OPTION) {
                try {
                    iconregistry.createNewFile();
                    this.saveToRegistry();
                    this.processRegistry();
                } catch (IOException err) {
                    throw new IllegalArgumentException(err.getMessage());
                }
            } else {
                throw new IllegalArgumentException("Unable to open icon mapping file " + this.usrConfigFile + ".");
            }
        }
    }

    /**
	This method should be called to initialize the icon mapping before
	the instance of this class is used.
	*/
    public void processRegistry() {
        File iconregistry = new File(this.usrConfigFile);
        if (iconregistry.exists()) {
            BufferedReader input = null;
            String line = null;
            int lineno = 0;
            String[] vals;
            String imageFile;
            String domain;
            try {
                input = new BufferedReader(new FileReader(this.usrConfigFile));
                while ((line = input.readLine()) != null) {
                    vals = line.split("\t");
                    if (vals.length == 2) {
                        domain = vals[0];
                        imageFile = vals[1];
                        this.iconMap.insert(new DomainNameNode(vals[0], vals[1]));
                    } else {
                        throw new RuntimeException("Problem parsing icon mapping for the domain '" + line + "'.");
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            int s = JOptionPane.showConfirmDialog(null, "Create icon mapping file " + this.usrConfigFile + "?", "Icon mapping file does not exist!", JOptionPane.YES_NO_CANCEL_OPTION);
            if (s == JOptionPane.YES_OPTION) {
                try {
                    iconregistry.createNewFile();
                } catch (IOException err) {
                    throw new IllegalArgumentException(err.getMessage());
                }
            } else {
                throw new IllegalArgumentException("Unable to open icon mapping file " + this.usrConfigFile + ".");
            }
            populateDefaultIcons();
        }
    }

    /**
	Add the default icon mappings to the current icon mappings.  This default implementation
	just returns an empty map.
	
	This should be overridden in an extension of this class to achieve different default Icon settings.
	*/
    public DomainNameTree getDefaultIconMap() {
        DomainNameTree map = new DomainNameTree();
        DomainNameNode newNode = new DomainNameNode("com.google", "/com/mockturtlesolutions/snifflib/guitools/images/google_icon.png");
        map.insert(newNode);
        newNode = new DomainNameNode("com.mockturtlesolutions", "/com/mockturtlesolutions/snifflib/guitools/images/small_mock_turtle.png");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.msu", "/com/mockturtlesolutions/snifflib/guitools/images/small_msu_logo.jpg");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.ncsu", "/com/mockturtlesolutions/snifflib/guitools/images/small_ncsu_logo.gif");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.osu", "/com/mockturtlesolutions/snifflib/guitools/images/small_osu_logo.gif");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.umich", "/com/mockturtlesolutions/snifflib/guitools/images/small_umich_logo.gif");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.duke", "/com/mockturtlesolutions/snifflib/guitools/images/duke_logo.png");
        map.insert(newNode);
        newNode = new DomainNameNode("edu.unc", "/com/mockturtlesolutions/snifflib/guitools/images/unc_logo.png");
        map.insert(newNode);
        newNode = new DomainNameNode("gov.nih", "/com/mockturtlesolutions/snifflib/guitools/images/small_nih_logo.gif");
        map.insert(newNode);
        newNode = new DomainNameNode("gov.usda", "/com/mockturtlesolutions/snifflib/guitools/images/small_usda_logo.jpeg");
        map.insert(newNode);
        newNode = new DomainNameNode("gov.usda.ars", "/com/mockturtlesolutions/snifflib/guitools/images/usda_ars.jpeg");
        map.insert(newNode);
        return (map);
    }

    /**
	Parses a dot-separated URL to return the leading domain name.
	
	Example: splitDomainName("com.foo.application") would return {"com.foo","application"}
	
	*/
    public static String[] splitDomainName(String domainname) {
        int split = domainname.lastIndexOf('.');
        String domain = "";
        String name = "";
        if (split > 0) {
            domain = domainname.substring(0, split);
            name = domainname.substring(split + 1, domainname.length());
        } else {
            domain = domainname;
            name = "";
        }
        return (new String[] { domain, name });
    }

    public void populateDefaultIcons() {
        DomainNameTree defaultmap = this.getDefaultIconMap();
        DomainNameTree newmap = new DomainNameTree();
        File iconDir = new File(this.usrIconDir);
        if (!(iconDir.exists() && iconDir.isDirectory())) {
            int s = JOptionPane.showConfirmDialog(null, "Create icon directory " + this.usrIconDir + "?", "Icon directory does not exist!", JOptionPane.YES_NO_CANCEL_OPTION);
            if (s == JOptionPane.YES_OPTION) {
                iconDir.mkdir();
            } else {
                return;
            }
        }
        Set domains = defaultmap.domainSet();
        Iterator iter = domains.iterator();
        while (iter.hasNext()) {
            String dname = (String) iter.next();
            String fname = defaultmap.getImageFile(dname);
            if (fname != null) {
                System.out.println("Attempting to populate with:" + fname);
                if (!fname.equals("null")) {
                    File file = new File(fname);
                    String newname = this.usrIconDir.concat(File.separator).concat(file.getName());
                    File newfile = new File(newname);
                    URL url = this.getClass().getResource(fname);
                    if (url != null) {
                        InputStream from = null;
                        FileOutputStream to = null;
                        try {
                            byte[] buffer = new byte[4096];
                            from = url.openStream();
                            to = new FileOutputStream(newfile);
                            int bytes_read = 0;
                            while ((bytes_read = from.read(buffer)) != -1) {
                                to.write(buffer, 0, bytes_read);
                            }
                            newmap.insert(new DomainNameNode(dname, newname));
                        } catch (Exception err) {
                            throw new RuntimeException("Problem saving image to file.", err);
                        } finally {
                            if (from != null) {
                                try {
                                    from.close();
                                } catch (IOException err) {
                                    throw new RuntimeException("Problem closing URL input stream.");
                                }
                            }
                            if (to != null) {
                                try {
                                    to.close();
                                } catch (IOException err) {
                                    throw new RuntimeException("Problem closing file output stream.");
                                }
                            }
                        }
                    } else {
                        throw new RuntimeException("Trying to copy the default icon " + fname + " from " + this.getClass().getPackage() + " but it does not exist.");
                    }
                }
            }
        }
        int s = JOptionPane.showConfirmDialog(null, "Save default mappings in " + this.usrConfigFile + "?", "Icon directory populated...", JOptionPane.YES_NO_CANCEL_OPTION);
        if (s == JOptionPane.YES_OPTION) {
            saveToRegistry(newmap);
        }
    }
}
