package org.formaria.editor.langed;

import org.formaria.aria.build.BuildProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.swing.JPanel;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.formaria.debug.DebugLogger;
import org.formaria.editor.project.EditorProject;
import org.formaria.editor.project.EditorProjectManager;
import org.formaria.langmgr.EncodedResourceBundle;

/**
 * LangManager manages the language resources on behalf of the language editor.
 * <p>Copyright: Formaria Ltd. (c) 2001-2006. 
 * This software is licensed under the GNU Public License (GPL) see license.txt 
 * for more details on licensing</p>
 */
public class EdLangMgr {

    private EditorProject currentProject;

    private String projectPath;

    public EdLangMgr(boolean _bIsStandalone, URL _url) {
        bIsStandalone = _bIsStandalone;
        url = _url;
        currentProject = (EditorProject) EditorProjectManager.getCurrentProject();
        try {
            init(url);
        } catch (IOException ex) {
        }
    }

    public void init(URL _url) throws IOException {
        usesDatabaseSource = false;
        url = _url;
        langList = new Vector(5);
        if ((url != null) && (url.getFile().toLowerCase().indexOf("languagelist.properties") > 0)) readLanguageList(); else if (langList.size() == 0) {
            addLang("reference", "Reference", null, false);
            addLang("EN", "English", null, false);
            refLang = ((EdLangName) langList.elementAt(0)).edLang;
        }
    }

    private EdLangName readLang(String id, String code, String name) throws IOException {
        String str = url.getPath();
        String newStr = newStr = str.substring(0, str.lastIndexOf('/') + 1) + code.toLowerCase() + ".properties";
        try {
            File langFile = new File(newStr);
            if (langFile.exists()) return importPropertiesFile(new File(newStr), code, name);
        } catch (MalformedURLException excep) {
            System.out.println("Malformed URL: " + excep);
        } catch (IOException excep) {
            System.out.println("IO Error: " + excep);
        }
        return null;
    }

    public String getProjectPath() {
        if (currentProject != null) return currentProject.getPath();
        return projectPath;
    }

    public void setProjectPath(String pp) {
        projectPath = pp;
    }

    private void readLanguageList() {
        Properties languageList = new Properties();
        try {
            languageList.load(url.openStream());
            int numLangs = languageList.keySet().size();
            langList.ensureCapacity(numLangs + 3);
            EdLangName commentsLang = readLang("1000", "cm", "Comments");
            boolean readRef = false;
            if (commentsLang == null) {
                commentsLang = readLang("1000", "reference", "Reference");
                readRef = true;
            }
            if (refLang == null) {
                addLang("reference", "Reference", null, false);
                refLang = ((EdLangName) langList.elementAt(readRef ? 0 : 1)).edLang;
            }
            EdLangName hintsLang = readLang("1001", "HT", "Hints");
            int id = 0;
            Enumeration languageKeys = languageList.keys();
            while (languageKeys.hasMoreElements()) {
                String languageName = (String) languageKeys.nextElement();
                String languageCode = (String) languageList.get(languageName);
                String languageNameLwr = languageName.toLowerCase();
                if (languageNameLwr.equals("comments") || languageNameLwr.equals("hints") || languageNameLwr.equals("reference")) continue;
                readLang(Integer.toString(id++), languageCode, languageName);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (langList.size() == 0 || !languageList.containsKey("Reference")) {
                addLang("reference", "Reference", null, false);
                refLang = ((EdLangName) langList.elementAt(0)).edLang;
            }
        }
    }

    public void init(Vector languages, Vector translations) throws IOException {
        usesDatabaseSource = true;
        langList = new Vector(5);
        int numLangs = languages.size();
        langList.ensureCapacity(numLangs);
        for (int i = 0; i < numLangs; i++) {
        }
        currentLang = ((EdLangName) langList.elementAt(0)).edLang;
        refLang = ((EdLangName) langList.elementAt(0)).edLang;
        loadedFromExportedFiles = false;
    }

    public EdLangName importPropertiesFile(File theFile, String code, String name) throws IOException {
        usesDatabaseSource = false;
        int id = langList.size();
        EdLangName ln;
        if (!theFile.exists()) return null;
        String path = theFile.getParent();
        ResourceBundle props = null;
        String fileName = theFile.toString();
        String encoding = null;
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        Properties encodingProperties = new Properties();
        File encodingFile = new File(fileName + ".property_encodings");
        if (encodingFile.exists()) {
            try {
                encodingProperties.load(new FileInputStream(encodingFile));
            } catch (Exception ex) {
                if (BuildProperties.DEBUG) DebugLogger.logWarning("Could not load the property_encodings file for: " + fileName);
                encodingProperties = null;
            }
            String fileEncoding = (String) encodingProperties.get("encoding");
            if ((fileEncoding != null) && (fileEncoding.length() > 0)) encoding = fileEncoding;
        }
        InputStream fis = new FileInputStream(theFile);
        if (encoding != null) {
            InputStreamReader isr = new InputStreamReader(fis, encoding);
            props = new EncodedResourceBundle(new BufferedReader(isr), encodingProperties);
        } else props = new PropertyResourceBundle(fis);
        ln = new EdLangName();
        ln.id = ++id;
        ln.code = code;
        ln.name = name;
        ln.edLang = currentLang = new EdLanguage(this);
        ln.encoding = encoding;
        if (code.equals("reference")) refLang = currentLang;
        langList.addElement(ln);
        Enumeration enumeration = props.getKeys();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String value = (String) props.getObject(key);
            int stringId = refLang.findString(key);
            if (stringId < 0) stringId = refLang.addString(key, key);
            if (currentLang != refLang) currentLang.addString(stringId, key, value);
        }
        return ln;
    }

    public boolean hasLang(String code) {
        int numLangs = langList.size();
        for (int i = 0; i < numLangs; i++) {
            if (((EdLangName) langList.elementAt(i)).code.equals(code)) return true;
        }
        return false;
    }

    public void importFile(JPanel parent, File theFile) throws IOException {
    }

    /**
   * Export content to a file based language resource
   * @param theFile
   * @param selectedLangs the list of languages to export
   * @throws IOException
   */
    public void exportFile(File theFile, char delimiterStartChar, char delimiterEndChar, String[] selectedLangs) throws IOException {
        URL theUrl = url;
        url = theFile.toURL();
        try {
            Vector selection = new Vector();
            int numSelectedLangs = selectedLangs.length;
            int numLangs = langList.size();
            for (int i = 0; i < numSelectedLangs; i++) {
                String selectedLang = selectedLangs[i];
                for (int j = 0; j < numLangs; j++) {
                    EdLangName eln = (EdLangName) langList.elementAt(j);
                    if (selectedLang.equals(eln.name)) {
                        selection.add(eln);
                        break;
                    }
                }
            }
            saveSelected(delimiterStartChar, delimiterEndChar, false, true, selection);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            url = theUrl;
        }
    }

    /**
   * Export content to an excel file based language resource. This assumes that
   * the first language exported is the master language containing all the keys
   * @param theFile
   * @param selectedLangs the list of languages to export
   * @throws IOException
   */
    public void exportExcel(File theFile, String[] selectedLangs) throws IOException {
        URL theUrl = url;
        url = theFile.toURL();
        String sql;
        WritableWorkbook workbook = Workbook.createWorkbook(theFile);
        WritableSheet sheet = workbook.createSheet("Translations", 0);
        Vector selection = new Vector();
        int numSelectedLangs = selectedLangs.length;
        int numLangs = langList.size();
        if (numSelectedLangs == 0) return;
        try {
            sheet.addCell(new Label(0, 0, "id"));
            for (int i = 0; i < numSelectedLangs; i++) {
                String selectedLang = selectedLangs[i];
                for (int j = 0; j < numLangs; j++) {
                    EdLangName eln = (EdLangName) langList.elementAt(j);
                    if (selectedLang.equals(eln.name)) {
                        Label label = new Label(i + 1, 0, selectedLang);
                        sheet.addCell(label);
                        selection.add(eln);
                        break;
                    }
                }
            }
            int numStrings = ((EdLangName) langList.elementAt(0)).edLang.getNumStrings();
            for (int i = 0; i < numStrings; i++) {
                sheet.addCell(new jxl.write.Number(0, i + 1, (double) i));
                for (int j = 0; j < numSelectedLangs; j++) {
                    EdLangName eln = (EdLangName) selection.elementAt(j);
                    String txt = eln.edLang.getString(i);
                    if (txt == null) {
                        txt = "";
                    }
                    sheet.addCell(new Label(j + 1, i + 1, txt));
                }
            }
            workbook.write();
            workbook.close();
        } catch (Exception ex) {
        }
    }

    /**
   * Import content to an excel file based language resource. This assumes that
   * the first language exported is the master language containing all the keys
   * @param theFile
   * @param selectedLangs the list of languages to export
   * @throws IOException
   */
    public void importExcel(Component parent, File theFile) throws IOException {
        Vector selectedLangs = new Vector();
        URL theUrl = url;
        url = theFile.toURL();
        String sql;
        try {
            Workbook workbook = Workbook.getWorkbook(theFile);
            Sheet sheet = workbook.getSheet(0);
            Vector selectedLanguages = new Vector();
            int numLangs = sheet.getColumns() - 1;
            for (int i = 1; i <= numLangs; i++) {
                selectedLangs.addElement(sheet.getCell(i, 0).getContents());
                selectedLanguages.addElement(new EdLanguage(this));
            }
            EdLanguage idLang = getLang(0);
            int numRows = sheet.getRows();
            for (int i = 1; i < numRows; i++) {
                String keyStr = sheet.getCell(0, i).getContents();
                if (keyStr != null) {
                    int id;
                    if (idLang != null) id = idLang.findString(keyStr); else if (keyStr.length() > 0) id = Integer.parseInt(keyStr); else break;
                    if (id >= 0) {
                        for (int j = 0; j < numLangs; j++) {
                            EdLanguage el = (EdLanguage) selectedLanguages.elementAt(j);
                            el.addString(id, null, sheet.getCell(j + 1, i).getContents());
                        }
                    }
                }
            }
            int nextId = 0;
            int numExistingLangs = langList.size();
            for (int i = 0; i < numLangs; i++) {
                String selectedLang = ((String) selectedLangs.elementAt(i)).toLowerCase();
                int j = 0;
                for (; j < numExistingLangs; j++) {
                    EdLangName eln = (EdLangName) langList.elementAt(j);
                    nextId = Math.max(nextId, eln.id);
                    if (selectedLang.equals(eln.name.toLowerCase())) {
                        eln.edLang = (EdLanguage) selectedLanguages.elementAt(i);
                        break;
                    }
                }
                if (j == numExistingLangs) {
                    EdLangName eln = new EdLangName();
                    if ((i == 0) && (nextId > 0)) nextId++;
                    eln.id = nextId++;
                    eln.code = selectedLang;
                    eln.name = selectedLang;
                    eln.edLang = (EdLanguage) selectedLanguages.elementAt(i);
                    langList.add(eln);
                }
            }
            if (numLangs > 0) {
                EdLangName tempLang = ((EdLangName) langList.elementAt(0));
                currentLang = tempLang.edLang;
                refLang = currentLang;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            url = theUrl;
        }
    }

    public boolean isLoadedFromExportedFiles() {
        return loadedFromExportedFiles;
    }

    /**
   * Sets the URL to which the languages will be saved.
   * @param url The URL for the file save.
   */
    void setUrl(URL _url) {
        url = _url;
    }

    /**
   * Gets the URL to which the languages will be saved.
   * @param url The URL for the file save.
   */
    URL getUrl() {
        return url;
    }

    /**
   * Adds a new language
   * This function is intended primarily for internal use by the
   * catalogue viewer.
   * @param code The ISO code for a locale.
   * @param name A plain text name for the language in the national language.
   */
    public void addLang(String code, String name, String encoding, boolean useDb) {
        if (langList == null) {
            langList = new Vector(5);
            currentLang = null;
        }
        int numLangs = langList.size();
        int id = 0;
        for (int i = 0; i < numLangs; i++) {
            EdLangName tempLang = ((EdLangName) langList.elementAt(i));
            id = Math.max(id, tempLang.id);
        }
        EdLangName ln = new EdLangName();
        ln.id = ++id;
        ln.code = code;
        ln.name = name;
        ln.edLang = new EdLanguage(this);
        ln.encoding = encoding;
        if (currentLang == null) currentLang = ln.edLang;
        langList.add(ln);
        if (useDb) {
        }
    }

    /**
   * Returns a language from the specified index in the list.
   * @param index <CODE>int</CODE> specifing the index
   * @return the returned <CODE>EdLanguage</CODE> instance
   */
    public EdLanguage getLangFromList(int index) {
        int items = langList.size();
        if (index < items) {
            EdLangName tempLangName = (EdLangName) langList.elementAt(index);
            if (tempLangName.edLang.getLangCode() == null) {
                tempLangName.edLang.setLangCode(tempLangName.code);
                tempLangName.edLang.setLangName(tempLangName.name);
            }
            return tempLangName.edLang;
        }
        return currentLang;
    }

    /**
   * Loads a language and sets it as the default language.
   * This function is intended primarily for internal use by the
   * catalogue viewer.
   * @param index the index for the language.
   */
    public EdLanguage getLang(int index) {
        int items = langList.size();
        if (index < items) {
            EdLanguage backupLang = currentLang;
            EdLangName tempLangName = (EdLangName) langList.elementAt(index);
            if (tempLangName.edLang.getLangCode() == null) {
                tempLangName.edLang.setLangCode(tempLangName.code);
                tempLangName.edLang.setLangName(tempLangName.name);
            }
            currentLang = tempLangName.edLang;
        }
        return currentLang;
    }

    /**
   * Loads a language and sets it as the default language.
   * This function is intended primarily for internal use by the
   * catalogue viewer.
   * @param index the index for the language.
   */
    public EdLanguage getLang(String langName) {
        int items = langList.size();
        for (int i = 0; i < items; i++) {
            if (((EdLangName) langList.elementAt(i)).name == langName) {
                EdLanguage backupLang = currentLang;
                EdLangName tempLangName = (EdLangName) langList.elementAt(i);
                if (tempLangName.edLang.getLangCode() == null) {
                    tempLangName.edLang.setLangCode(tempLangName.code);
                    tempLangName.edLang.setLangName(tempLangName.name);
                }
                return tempLangName.edLang;
            }
        }
        return currentLang;
    }

    /**
   * Returns the current language... the comments language
   */
    public EdLanguage getCurrentLang() {
        return currentLang;
    }

    /**
   * Gets the key corresponding to an ID
   * @param id
   * @return
   */
    public String getKey(int id) {
        String src = refLang.getString(id);
        if (usesReferenceLang) return src.replaceAll(" ", "_");
        return "str_" + Integer.toString(id);
    }

    public void setId(String id) {
        refLang.addString("", id);
    }

    /**
   * Reads a language from the vector
   */
    private EdLanguage read(Vector translations, int field) {
        currentLang = new EdLanguage(this);
        currentLang.read(translations, field, false);
        return currentLang;
    }

    /**
   * Returns the number of languages
   */
    public int getNumLangs() {
        return langList == null ? 0 : langList.size();
    }

    /**
   * Returns the name of the indexed language
   */
    public String getLangName(int index) {
        int items = langList.size();
        if ((items > 0) && (index < items)) return ((EdLangName) langList.elementAt(index)).name;
        return "";
    }

    /**
   * Returns the name of the indexed language
   */
    public String getLangCode(int index) {
        int items = langList.size();
        if ((items > 0) && (index < items)) return ((EdLangName) langList.elementAt(index)).code;
        return "";
    }

    public void setCurrentLang(String langName) {
        currentLang = getLang(langName);
    }

    public void saveFile(char delimiterStartChar, char delimiterEndChar) {
        saveSelected(delimiterStartChar, delimiterEndChar, true, false, langList);
    }

    /**
   * Saves all languages back to their original URL.
   */
    private void saveSelected(char delimiterStartChar, char delimiterEndChar, boolean doDatabaseSave, boolean bExported, Vector selectedList) {
        if (langList == null) return;
        int maxStrId = 0;
        int items = langList.size();
        if (items == 0) return;
        try {
            String projectPath = getProjectPath();
            if (projectPath != null) {
                BufferedWriter os;
                File file = new File(projectPath + File.separator + "lang" + File.separator + "LanguageList.properties");
                if (!file.exists()) file.createNewFile();
                os = new BufferedWriter(new FileWriter(file));
                int numLangs = langList.size();
                for (int i = 0; i < numLangs; i++) {
                    EdLangName tempLang = ((EdLangName) langList.elementAt(i));
                    String s = tempLang.name + "=" + tempLang.code + "\r\n";
                    os.write(s);
                    String str = projectPath + File.separator + "lang" + File.separator + tempLang.code.toLowerCase() + ".properties";
                    if (tempLang.edLang != null) tempLang.edLang.saveProperties(str, tempLang.encoding);
                }
                os.close();
            }
        } catch (IOException exp) {
            System.out.println("IO Exception: " + exp);
        }
    }

    private void saveToDatabase() {
    }

    public void newFile() {
        EdLangName ln = new EdLangName();
        ln.id = 0;
        ln.code = "CM";
        ln.name = "Comments";
        ln.edLang = new EdLanguage(this);
        langList = new Vector();
        langList.addElement(ln);
    }

    private EdLanguage currentLang, refLang;

    private Vector langList;

    private String langName;

    private URL url;

    private boolean bIsStandalone = true;

    private boolean usesDatabaseSource = false;

    private boolean usesReferenceLang = true;

    private boolean loadedFromExportedFiles = false;
}

class EdLangName {

    public EdLanguage edLang;

    public int id;

    public String code;

    public String name;

    public String encoding;

    public EdLangName() {
        edLang = null;
        encoding = null;
    }

    public String toString() {
        return name;
    }
}
