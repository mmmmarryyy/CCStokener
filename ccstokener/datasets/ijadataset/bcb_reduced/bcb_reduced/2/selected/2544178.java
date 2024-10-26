package org.amhm.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.amhm.core.constants.AMHMConst;
import org.amhm.core.schemas.Amhm;
import org.amhm.core.schemas.FileType;
import org.amhm.core.schemas.PreferenceType;
import org.amhm.core.schemas.SearchType;
import org.amhm.core.schemas.SearchesType;
import org.amhm.core.schemas.UpdVersionType;
import org.amhm.core.schemas.VersionType;
import org.amhm.core.schemas.WorkspaceType;
import org.amhm.persistence.Program;
import org.amhm.persistence.Season;
import org.amhm.services.updates.AmhmWsdl;
import org.amhm.services.updates.AmhmWsdlLocator;
import org.amhm.services.updates.AmhmWsdlPortType;

public class Updater {

    public static final String VERSION = "VERSION";

    private String filename = AMHMConst.PREF_FILENAME;

    private Amhm amhm_prefs = null;

    public void update() {
        AmhmWsdl service = new AmhmWsdlLocator();
        try {
            AmhmWsdlPortType port = service.getAmhmWsdlPort();
            readVersion();
            if (amhm_prefs == null) {
                amhm_prefs = new Amhm();
                amhm_prefs.setVersion(new VersionType(0, 0, 0, 0));
                amhm_prefs.setWorkspace(new WorkspaceType());
                amhm_prefs.getWorkspace().setPreferences(new PreferenceType());
                amhm_prefs.getWorkspace().getPreferences().setSearches(new SearchesType());
                List<SearchType> list = amhm_prefs.getWorkspace().getPreferences().getSearches().getSearch();
                SearchType st = new SearchType();
                st.setSearchName("Inscription 2009");
                st.setSerializable(new Season(1, "Saison 2009", null, null, null));
                list.add(st);
                st = new SearchType();
                st.setSearchName("Inscrits Cycle 2009");
                st.setSerializable(new Program(1, "Cycle", null, null, 1));
                list.add(st);
            }
            if (!port.updateValidation(amhm_prefs.getVersion())) {
                UpdVersionType[] updvers = port.getLatestVersion();
                amhm_prefs.setVersion(convertToVersionType(updvers[0].getVersion()));
                amhm_prefs.getVersion().setChanges(updvers[0].getChangelog());
                for (FileType ft : updvers[0].getFiles()) {
                    if (ft.isRem()) {
                        removeFile(ft.getDest(), ft.getFilename());
                    } else {
                        download(ft.getSrc(), ft.getDest(), ft.getFilename());
                    }
                }
                writeVersion();
            } else {
                System.out.println("System uptodate");
            }
        } catch (javax.xml.rpc.ServiceException e) {
            e.printStackTrace();
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();
        } catch (java.util.InvalidPropertiesFormatException e) {
            e.printStackTrace();
        }
    }

    private VersionType convertToVersionType(String vers) throws java.util.InvalidPropertiesFormatException {
        String arrVers[] = vers.split("[.]");
        if (arrVers.length != 4) throw new java.util.InvalidPropertiesFormatException("Illegal Conversion, Expected VersionType [4] parameters instead of [" + arrVers.length + "]");
        return new VersionType(arrVers[0], arrVers[1], arrVers[2], arrVers[3]);
    }

    private VersionType readVersion() {
        try {
            JAXBContext context = JAXBContext.newInstance("org.amhm.core.schemas");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            amhm_prefs = (Amhm) unmarshaller.unmarshal(new FileReader(this.filename));
            return amhm_prefs.getVersion();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("prefs.xml doesn't exist!");
        }
        return null;
    }

    private void writeVersion() {
        try {
            File file = new File(this.filename);
            String path = file.getCanonicalPath().replace(file.getName(), "");
            file = new File(path);
            file.mkdirs();
            JAXBContext context = JAXBContext.newInstance("org.amhm.core.schemas");
            Marshaller m = context.createMarshaller();
            m.marshal(amhm_prefs, new FileWriter(this.filename));
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void download(String srcDir, String destDir, String filename) {
        System.out.println("download : " + AMHMConst.AMHM_URL + srcDir + "/" + filename + " to " + destDir + "/" + filename);
        try {
            URL url = new URL(AMHMConst.AMHM_URL + srcDir + "/" + filename);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            File file = new File(destDir + "/");
            file.mkdirs();
            file = new File(destDir + "/" + filename);
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeFile(String destDir, String filename) {
        System.out.println("delete : " + destDir + "/" + filename);
        String fileName = destDir + "/" + filename;
        File f = new File(fileName);
        if (!f.exists()) throw new IllegalArgumentException("Delete: no such file or directory: " + fileName);
        if (!f.canWrite()) throw new IllegalArgumentException("Delete: write protected: " + fileName);
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0) throw new IllegalArgumentException("Delete: directory not empty: " + fileName);
        }
        boolean success = f.delete();
        if (!success) throw new IllegalArgumentException("Delete: deletion failed");
    }
}
