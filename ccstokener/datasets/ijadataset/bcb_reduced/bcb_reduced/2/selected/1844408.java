package org.freedom.infra.functions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import org.freedom.infra.model.jdbc.DbConnection;

public class SystemFunctions {

    public static final int OS_LINUX = 0;

    public static final int OS_WINDOWS = 1;

    public static final int OS_OSX = 3;

    public static final int OS_VERSION_WINDOWS_XP = 10;

    public static final int OS_VERSION_WINDOWS_VISTA = 11;

    public static final int OS_VERSION_WINDOWS_SEVEN = 12;

    /**
	 * 
	 * @return A name of operational system.<BR>
	 */
    public static int getOS() {
        int ret = -1;
        final String system = System.getProperty("os.name").toLowerCase();
        if (system.indexOf("linux") > -1) {
            ret = OS_LINUX;
        } else if (system.indexOf("windows") > -1) {
            ret = OS_WINDOWS;
        } else if (system.indexOf("mac os x") > -1) {
            ret = OS_OSX;
        }
        return ret;
    }

    /**
	 * 
	 * @return A version of operational system windows.<BR>
	 */
    public static int getWindowsVersion() {
        int ret = -1;
        final String system = System.getProperty("os.name").toLowerCase();
        if (system.indexOf("windows") > -1) {
            if (System.getProperty("os.name").indexOf("xp") > -1) {
                ret = OS_VERSION_WINDOWS_XP;
            } else if (System.getProperty("os.name").indexOf("vista") > -1) {
                ret = OS_VERSION_WINDOWS_VISTA;
            } else if (System.getProperty("os.name").indexOf("7") > -1) {
                ret = OS_VERSION_WINDOWS_SEVEN;
            }
        }
        return ret;
    }

    public static java.util.Date getClassDateCompilation(Class<?> clazz) throws IOException {
        String className = clazz.getName();
        className = className.replaceAll("\\.", "/");
        className = "/" + className + ".class";
        URL url = Class.class.getResource(className);
        URLConnection urlConnection = url.openConnection();
        java.util.Date lastModified = new java.util.Date(urlConnection.getLastModified());
        return lastModified;
    }

    public static String getVersionSis(Class<?> clazz) {
        String versao = "";
        try {
            File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            JarFile jarfile = new JarFile(file.getAbsolutePath());
            Manifest manifest = jarfile.getManifest();
            Attributes att = manifest.getMainAttributes();
            versao = att.getValue("Signature-Version");
        } catch (Exception e) {
            versao = "Indefinida";
        }
        return versao;
    }

    public static String getVersionDB(DbConnection con) {
        String ret = "Indefinida";
        StringBuilder sql = new StringBuilder();
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            sql.append("select versao from sgretversao");
            ps = con.prepareStatement(sql.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getString("versao");
            }
            ps.close();
            rs.close();
            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getTxtFile(URL url) {
        String ret = "";
        int size = 0;
        char c = (char) 0;
        try {
            System.out.println("carregando arquivo: " + url.toString());
            File fArq = new File(url.getFile());
            FileReader frArq = new FileReader(fArq);
            try {
                size = (int) fArq.length();
                for (int i = 0; i < size; i++) {
                    c = (char) frArq.read();
                    ret += c;
                }
            } catch (IOException err) {
                err.printStackTrace();
                System.exit(0);
            }
        } catch (FileNotFoundException err) {
            err.printStackTrace();
        }
        return ret;
    }

    public static String getTxtFile(String url) {
        String ret = "";
        int size = 0;
        char c = (char) 0;
        try {
            System.out.println("carregando arquivo: " + url.toString());
            File fArq = new File(url);
            FileReader frArq = new FileReader(fArq);
            try {
                size = (int) fArq.length();
                for (int i = 0; i < size; i++) {
                    c = (char) frArq.read();
                    ret += c;
                }
            } catch (IOException err) {
                err.printStackTrace();
                System.exit(0);
            }
        } catch (FileNotFoundException err) {
            err.printStackTrace();
        }
        return ret;
    }

    public static Vector<String> getIniFile(String sNomeArq) {
        Vector<String> vRetorno = new Vector<String>();
        String sTemp = "";
        int iTam = 0;
        char c = (char) 0;
        try {
            File fArq = new File(sNomeArq);
            FileReader frArq = new FileReader(fArq);
            try {
                iTam = (int) fArq.length();
                for (int i = 0; i < iTam; i++) {
                    c = (char) frArq.read();
                    if (c == (char) 10) {
                        vRetorno.addElement(sTemp);
                        sTemp = "";
                    } else if (c == (char) 13) {
                        if (i == iTam) {
                            vRetorno.addElement(sTemp);
                            sTemp = "";
                        } else {
                            c = (char) frArq.read();
                            i++;
                            if (c == (char) 10) {
                                vRetorno.addElement(sTemp);
                                sTemp = "";
                            } else {
                                vRetorno.addElement(sTemp);
                                sTemp = "";
                                sTemp += c;
                            }
                        }
                    } else {
                        sTemp += c;
                    }
                }
            } catch (IOException err) {
                JOptionPane.showMessageDialog(null, "Erro ao carregar arquivo de configura��o!\n", "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } catch (FileNotFoundException err) {
            JOptionPane.showMessageDialog(null, "Erro ao carregar arquivo de configura��o!\n" + err.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        return vRetorno;
    }

    public static Vector<File> unzip(File zip) {
        Vector<File> ret = new Vector<File>();
        final int BUFFER = 2048;
        try {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry;
            ZipFile zipfile = new ZipFile(zip);
            Enumeration<?> e = zipfile.entries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                System.out.println("Extracting: " + entry);
                is = new BufferedInputStream(zipfile.getInputStream(entry));
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                ret.add(new File(entry.getName()));
                dest.flush();
                dest.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static File writeFileString(String sFileName, String content) {
        File ret = null;
        try {
            ret = new File(sFileName);
            ret.createNewFile();
            FileWriter fw = new FileWriter(ret);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (IOException ioError) {
            ioError.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
