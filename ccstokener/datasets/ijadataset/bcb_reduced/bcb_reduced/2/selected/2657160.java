package com.htdsoft.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class MiseAJour {

    private String xmlPath = "http://www.htdsoft.com/productools/Version.xml";

    private Document xmlDocument = null;

    private ArrayList<String> getVersions() {
        ArrayList<String> versions = new ArrayList<String>();
        try {
            URL xmlUrl = new URL(xmlPath);
            URLConnection urlConnection = xmlUrl.openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            InputStream stream = urlConnection.getInputStream();
            SAXBuilder sxb = new SAXBuilder();
            try {
                xmlDocument = sxb.build(stream);
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Element racine = xmlDocument.getRootElement();
            List listVersions = racine.getChildren("version");
            Iterator iteratorVersions = listVersions.iterator();
            while (iteratorVersions.hasNext()) {
                Element version = (Element) iteratorVersions.next();
                Element elementNom = version.getChild("nom");
                versions.add(elementNom.getText());
            }
            Collections.sort(versions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return versions;
    }

    private void downloadFile(String filePath, String destination) {
        URLConnection connection = null;
        InputStream is = null;
        FileOutputStream destinationFile = null;
        try {
            URL url = new URL(filePath);
            connection = url.openConnection();
            int length = connection.getContentLength();
            if (length == -1) {
                throw new IOException("Fichier vide");
            }
            is = new BufferedInputStream(connection.getInputStream());
            byte[] data = new byte[length];
            int currentBit = 0;
            int deplacement = 0;
            while (deplacement < length) {
                currentBit = is.read(data, deplacement, data.length - deplacement);
                if (currentBit == -1) break;
                deplacement += currentBit;
            }
            if (deplacement != length) {
                throw new IOException("Le fichier n'a pas �t� lu en entier (seulement " + deplacement + " sur " + length + ")");
            }
            destinationFile = new FileOutputStream(destination);
            destinationFile.write(data);
            destinationFile.flush();
        } catch (MalformedURLException e) {
            System.err.println("Probl�me avec l'URL : " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                destinationFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String lanceurPath = "Lanceur";

    private String version = "0";

    private String currentFolder = System.getProperty("user.dir");

    public void update() {
        ArrayList<String> versions = getVersions();
        if (versions.size() == 0) {
            JOptionPane.showMessageDialog(null, "Impossible de se connecter au service, v�rifiez votre " + "connection internet");
        } else {
            if (!versions.get(versions.size() - 1).equals(version)) {
                String versionChoisie = (String) JOptionPane.showInputDialog(null, "Choississez la version � " + "installer", "Versions disponibles", JOptionPane.QUESTION_MESSAGE, null, versions.toArray(), versions.get(versions.size() - 1));
                if (versionChoisie != "") {
                    Element racine = xmlDocument.getRootElement();
                    List listVersions = racine.getChildren("version");
                    Iterator iteratorVersions = listVersions.iterator();
                    while (iteratorVersions.hasNext()) {
                        Element version = (Element) iteratorVersions.next();
                        Element elementNom = version.getChild("nom");
                        if (elementNom.getText().equals((String) versionChoisie)) {
                            Element elementFiles = version.getChild("files");
                            List listFiles = elementFiles.getChildren("file");
                            Iterator iteratorFiles = listFiles.iterator();
                            while (iteratorFiles.hasNext()) {
                                Element file = (Element) iteratorFiles.next();
                                downloadFile(file.getChildText("url"), currentFolder + File.separator + file.getChildText("destination"));
                            }
                            break;
                        }
                    }
                    JOptionPane.showMessageDialog(null, "La nouvelle version a �t� t�l�charg�e, " + "le programme va �tre relanc�");
                    File lanceur = new File(lanceurPath);
                    try {
                        Desktop.open(lanceur);
                        System.exit(0);
                    } catch (DesktopException e) {
                        JOptionPane.showMessageDialog(null, "Impossible de relancer le programme");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Pas de nouvelles version disponible pour le moment");
            }
        }
    }
}
