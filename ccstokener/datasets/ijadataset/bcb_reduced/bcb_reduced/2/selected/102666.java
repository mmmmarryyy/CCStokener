package org.oboedit.gui;

import java.awt.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.xml.transform.TransformerException;
import java.net.MalformedURLException;
import java.net.URL;
import org.bbop.framework.GUIManager;
import org.bbop.util.*;
import org.obo.datamodel.*;
import org.obo.datamodel.impl.*;
import org.obo.util.VersionNumber;
import org.oboedit.gui.event.ReconfigEvent;
import org.oboedit.gui.event.ReconfigListener;
import org.oboedit.gui.widget.TextIcon;
import org.oboedit.util.GUIUtil;
import org.apache.log4j.*;

public class Preferences {

    protected static final Logger logger = Logger.getLogger(Preferences.class);

    protected Font font;

    protected Color backgroundColor = null;

    protected Color buttonColor = null;

    protected Color selectionColor = null;

    protected Color lightSelectionColor = null;

    protected Color orange = new Color(255, 153, 0);

    protected Color purple = new Color(102, 0, 204);

    protected boolean confirmOnExit = false;

    protected boolean advxpMatrixEditorCheckBox = false;

    protected boolean advIntersectionEditorCheckBox = false;

    protected boolean advSemanticParserCheckBox = false;

    protected boolean useReasoner = false;

    protected String reasonerName = "OFF";

    protected boolean isIncOn = false;

    protected boolean isStepIncOn = false;

    protected boolean showComplete = false;

    protected boolean useBasicRootDetection = true;

    protected boolean onlyOneGlobalOTE = true;

    protected boolean autosaveEnabled = true;

    protected boolean caseSensitiveSort = false;

    protected boolean showToolTips = true;

    protected boolean excludeObsoletesFromSearches = false;

    protected boolean allowCycles = false;

    protected boolean warnBeforeDelete = true;

    protected boolean warnBeforeDefinitionLoss = true;

    protected boolean allowExtendedCharacters = false;

    protected String mem;

    protected String browserCommand = "";

    protected String userName = System.getProperty("user.name");

    protected String fullName = "";

    protected String email = "";

    protected int autosaveWaitTime = 20;

    protected int autosaveExpirationDays = 7;

    protected int selectionBatchSize = 100;

    protected boolean autoCommitTextEdits = false;

    protected boolean warnBeforeDiscardingEdits = true;

    protected Dbxref personalDbxref;

    protected List<Dbxref> personalDbxrefs = null;

    protected String defaultDef;

    protected boolean usePersonalDefinition = false;

    protected MultiProperties adapterChooserProperties = new MultiProperties();

    protected boolean useModalProgressMonitors = !System.getProperty("useModalProgressMonitors", "true").equals("false");

    protected Collection<ReconfigListener> reconfigListeners = new LinkedList<ReconfigListener>();

    protected static String appName = "OBO-Edit";

    protected static boolean batchMode = true;

    public void addReconfigListener(ReconfigListener e) {
        reconfigListeners.add(e);
    }

    public void removeReconfigListener(ReconfigListener e) {
        reconfigListeners.remove(e);
    }

    public void fireReconfigEvent(ReconfigEvent event) {
        Collection<ReconfigListener> temp = new LinkedList<ReconfigListener>(reconfigListeners);
        for (ReconfigListener listener : temp) {
            listener.configReloaded(event);
        }
    }

    protected static File installationDir;

    protected File autosavePath;

    protected String logFile = "";

    protected Map<String, Icon> iconIndex = new HashMap<String, Icon>();

    protected Map<String, String> iconURLIndex = new HashMap<String, String>();

    protected Map<String, Color> colorIndex = new HashMap<String, Color>();

    protected static VersionNumber version;

    protected static final String PLIST_TRANSFORM = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" + "  <xsl:output method=\"xml\" indent=\"yes\"/>\n" + "  <xsl:param name=\"memoryOption\" select=\"\'7M\'\"/>\n" + "  <xsl:variable name=\"firstDictID\" select=\"generate-id(/plist/dict[1])\"/>\n" + "  <xsl:template match=\"/\">\n" + "     <xsl:apply-templates/>\n" + "  </xsl:template>\n" + "  <xsl:template match=\"dict\">\n" + "    <xsl:copy>\n" + "      <xsl:copy-of select=\"@*\"/>\n" + "      <xsl:if test=\"generate-id() = $firstDictID and false() = (//key=\'VMOptions\')\">\n" + "	<key>Java</key>\n" + "	<dict>\n" + "	  <key>VMOptions</key>\n" + "	  <string>-Xmx<xsl:value-of select=\"$memoryOption\"/></string>\n" + "	</dict>\n" + "      </xsl:if>\n" + "      <xsl:apply-templates/>\n" + "   </xsl:copy>\n" + "  </xsl:template>\n" + "  <xsl:template match=\"string\">\n" + "   <xsl:copy>\n" + "     <xsl:copy-of select=\"@*\"/>\n" + "     <xsl:choose>\n" + "       <xsl:when test=\"preceding-sibling::*[1] = \'VMOptions\'\">\n" + "         <xsl:text>-Xmx</xsl:text><xsl:value-of select=\"$memoryOption\"/>\n" + "       </xsl:when>\n" + "       <xsl:otherwise>       \n" + "         <xsl:value-of select=\"text()\"/>\n" + "       </xsl:otherwise>\n" + "     </xsl:choose>\n" + "   </xsl:copy>	 	\n" + "  </xsl:template>\n" + "  <xsl:template match=\"node()\">\n" + "    <xsl:copy>\n" + "      <xsl:copy-of select=\"@*\"/>\n" + "      <xsl:apply-templates/>\n" + "    </xsl:copy>\n" + "  </xsl:template>\n" + "</xsl:stylesheet>";

    protected static File filterFile;

    protected static Preferences preferences;

    public Preferences() {
        setFont(new Font("Arial", 0, 12));
        filterFile = new File(getOBOEditPrefsDir(), "filters/");
        iconURLIndex.put(OBOProperty.IS_A.getID(), "resource:is_a.svg");
        iconURLIndex.put("part_of", "resource:part_of.svg");
        iconURLIndex.put("develops_from", "resource:develops_from.svg");
        iconURLIndex.put("involved_in", "resource:involved_in.svg");
        iconURLIndex.put("regulates", "resource:regulates.svg");
        iconURLIndex.put("positively_regulates", "resource:upregs-arrow.svg");
        iconURLIndex.put("negatively_regulates", "resource:downregs-arrow.svg");
        iconURLIndex.put("regulator_of", "resource:regulator_of.svg");
        iconURLIndex.put("positive_regulator_of", "resource:positive_regulator_of.svg");
        iconURLIndex.put("negative_regulator_of", "resource:negative_regulator_of");
        colorIndex.put(OBOProperty.IS_A.getID(), Color.blue);
        colorIndex.put("part_of", orange);
        colorIndex.put("develops_from", Color.green.darker());
        colorIndex.put("regulates", purple);
        colorIndex.put("positively_regulates", purple);
        colorIndex.put("negatively_regulates", purple);
    }

    public static Preferences getPreferences() {
        if (preferences == null) {
            XMLDecoder d;
            try {
                d = new XMLDecoder(new BufferedInputStream(new FileInputStream(Preferences.getPrefsXMLFile())));
                Preferences p = (Preferences) d.readObject();
                preferences = p;
                d.close();
            } catch (Exception e) {
                logger.info("Could not read preferences file from " + Preferences.getPrefsXMLFile());
            }
            if (preferences == null) preferences = new Preferences();
            GUIManager.addShutdownHook(new Runnable() {

                public void run() {
                    try {
                        writePreferences(getPreferences());
                    } catch (IOException ex) {
                        logger.info("Could not write verification settings!");
                        ex.printStackTrace();
                    }
                }
            });
        }
        return preferences;
    }

    public static File getOBOEditPrefsDir() {
        VersionNumber version = Preferences.getVersion();
        File prefsDir = new File(System.getProperty("user.home") + "/" + "oboedit_config");
        if (!prefsDir.exists() && !isBatchMode()) {
            File oldPrefsDir = new File(System.getProperty("user.home") + "/.obo-edit" + (version.isRC() ? "rc" : "") + (version.isBeta() ? "beta" : "") + "/");
            if (oldPrefsDir.exists()) GUIUtil.copyExistingConfigFiles(oldPrefsDir, prefsDir);
        }
        return prefsDir;
    }

    public static String getUserDictDir() {
        return getOBOEditPrefsDir() + "/dict";
    }

    public static File getStandardDictionaryFile() {
        return new File(getUserDictDir(), "standard.dict");
    }

    public static File getUserDefDictionaryFile() {
        return new File(getOBOEditPrefsDir() + "/dict", "user.dict");
    }

    public static File getPeriodWordsFile() {
        return new File(getOBOEditPrefsDir() + "/dict", "periodwords.dict");
    }

    public static File getAlwaysLowercaseFile() {
        return new File(getOBOEditPrefsDir() + "/dict", "alwayslowercase.dict");
    }

    public static File getAllowedRepeatsFile() {
        return new File(getOBOEditPrefsDir() + "/dict", "allowedrepeats.dict");
    }

    /**
	 * @return true if user is in autocommit mode
	 */
    public boolean getAutoCommitTextEdits() {
        return autoCommitTextEdits;
    }

    public void setAutoCommitTextEdits(boolean autoCommitTextEdits) {
        this.autoCommitTextEdits = autoCommitTextEdits;
    }

    public boolean getWarnBeforeDiscardingEdits() {
        return warnBeforeDiscardingEdits;
    }

    public void setWarnBeforeDiscardingEdits(boolean warnBeforeDiscardingEdits) {
        this.warnBeforeDiscardingEdits = warnBeforeDiscardingEdits;
    }

    public void setAllowExtendedCharacters(boolean allowExtendedCharacters) {
        this.allowExtendedCharacters = allowExtendedCharacters;
    }

    public boolean getAllowExtendedCharacters() {
        return allowExtendedCharacters;
    }

    public void setSelectionBatchSize(int selectionBatchSize) {
        this.selectionBatchSize = selectionBatchSize;
    }

    public int getSelectionBatchSize() {
        return selectionBatchSize;
    }

    public void setAdapterChooserProperties(MultiProperties adapterChooserProperties) {
        this.adapterChooserProperties = adapterChooserProperties;
    }

    public MultiProperties getAdapterChooserProperties() {
        return adapterChooserProperties;
    }

    public void setWarnBeforeDefinitionLoss(boolean warnBeforeDefinitionLoss) {
        this.warnBeforeDefinitionLoss = warnBeforeDefinitionLoss;
    }

    public boolean getWarnBeforeDefinitionLoss() {
        return warnBeforeDefinitionLoss;
    }

    public void setWarnBeforeDelete(boolean warnBeforeDelete) {
        this.warnBeforeDelete = warnBeforeDelete;
    }

    public boolean getWarnBeforeDelete() {
        return warnBeforeDelete;
    }

    public void setAllowCycles(boolean allowCycles) {
        this.allowCycles = allowCycles;
    }

    public boolean getAllowCycles() {
        return allowCycles;
    }

    public void setShowToolTips(boolean showToolTips) {
        this.showToolTips = showToolTips;
    }

    public boolean getShowToolTips() {
        return showToolTips;
    }

    public void setExcludeObsoletesFromSearches(boolean excludeObsoletesFromSearches) {
        this.excludeObsoletesFromSearches = excludeObsoletesFromSearches;
    }

    public boolean getExcludeObsoletesFromSearches() {
        return excludeObsoletesFromSearches;
    }

    public void setCaseSensitiveSort(boolean caseSensitiveSort) {
        this.caseSensitiveSort = caseSensitiveSort;
    }

    public boolean getCaseSensitiveSort() {
        return caseSensitiveSort;
    }

    public void setAutosaveExpirationDays(int autosaveExpirationDays) {
        this.autosaveExpirationDays = autosaveExpirationDays;
    }

    public int getAutosaveExpirationDays() {
        return autosaveExpirationDays;
    }

    public void setAutosaveWaitTime(int autosaveWaitTime) {
        this.autosaveWaitTime = autosaveWaitTime;
    }

    public int getAutosaveWaitTime() {
        return autosaveWaitTime;
    }

    public void setAutosaveEnabled(boolean autosaveEnabled) {
        this.autosaveEnabled = autosaveEnabled;
    }

    public boolean getAutosaveEnabled() {
        return autosaveEnabled;
    }

    public File getAutosavePath() {
        if (autosavePath == null) autosavePath = new File(getOBOEditPrefsDir(), "autosave");
        return autosavePath;
    }

    public void setAutosavePath(File autosavePath) {
        this.autosavePath = autosavePath;
    }

    public Dbxref getPersonalDbxref() {
        if (personalDbxref != null) return personalDbxref; else return new DbxrefImpl("", getUserName(), Dbxref.UNKNOWN);
    }

    public void setPersonalDbxref(Dbxref personalDbxref) {
        this.personalDbxref = personalDbxref;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setBrowserCommand(String browserCommand) {
        this.browserCommand = browserCommand;
    }

    public String getBrowserCommand() {
        return browserCommand;
    }

    public void setUseBasicRootDetection(boolean useBasicRootDetection) {
        this.useBasicRootDetection = useBasicRootDetection;
    }

    public boolean getUseBasicRootDetection() {
        return useBasicRootDetection;
    }

    public void setOnlyOneGlobalOTE(boolean onlyOneGlobalOTE) {
        this.onlyOneGlobalOTE = onlyOneGlobalOTE;
    }

    public boolean getOnlyOneGlobalOTE() {
        return onlyOneGlobalOTE;
    }

    protected static ClassLoader getExtensionLoader() {
        return Preferences.class.getClassLoader();
    }

    public static File[] getExtensionPaths() {
        File[] out = { new File(getInstallationDirectory(), "extensions"), new File(getOBOEditPrefsDir(), "extensions") };
        return out;
    }

    protected Icon loadLibraryIconLocal(String name) {
        URL url = getExtensionLoader().getResource("org/oboedit/gui/resources/icons/" + name);
        if (url == null) {
            url = getExtensionLoader().getResource("org/oboedit/gui/resources/icons/typeicons/" + name);
        }
        return getIconForURL(url);
    }

    public static Icon getIconForURL(URL url) {
        if (url == null) return null;
        try {
            String urlStr = url.toString();
            if (urlStr.endsWith("svg")) return new SVGIcon(urlStr);
        } catch (Exception e) {
            logger.info("WARNING: Exception getting icon for " + url + ": " + e);
        }
        return new ImageIcon(url);
    }

    public static Image loadLibraryImage(String name) {
        URL url = getExtensionLoader().getResource("org/oboedit/gui/resources/icons/" + name);
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    public static String readMemStringFromDisk() {
        File optionFile = new File(getInstallationDirectory(), getLauncherName() + ".vmoptions");
        String mem = null;
        if (optionFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(optionFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String s = line.toLowerCase().trim();
                    if (line.toLowerCase().trim().startsWith("-xmx")) {
                        mem = s.substring(4).toUpperCase();
                        reader.close();
                        logger.info("Got memory string " + mem + " from option file " + optionFile);
                        break;
                    }
                }
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (mem == null) {
            logger.error("WARNING: readMemStringFromDisk couldn't get memory string from vmoptions file " + optionFile);
            return null;
        }
        String numMem = mem.substring(0, mem.indexOf("M"));
        return numMem + "M";
    }

    public String getMemString() {
        if (mem == null) {
            mem = readMemStringFromDisk();
        }
        return mem;
    }

    public void setMemoryValue(String mem) {
        if (mem == null) return;
        this.mem = mem;
        String numMem = mem.substring(0, mem.indexOf("M"));
        String diskmem = readMemStringFromDisk();
        if (diskmem == null || !numMem.equals(diskmem)) {
            updateDiskMemValue(mem);
        }
    }

    public static void updateDiskMemValue(String mem) {
        File optionFile = new File(getInstallationDirectory(), getLauncherName() + ".vmoptions");
        if (mem == null || mem.toUpperCase().indexOf("M") < 1) {
            logger.error("updateDiskMemValue: memory string " + mem + " is not legal.");
            return;
        }
        String newmemValue = "-Xmx" + mem;
        if (optionFile.exists()) {
            File tempFile = new File(optionFile.getAbsolutePath() + ".tmp");
            try {
                BufferedReader reader = new BufferedReader(new FileReader(optionFile));
                PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug(" Updating OBOEdit.vmoptions to: " + newmemValue);
                    pw.println(newmemValue);
                    pw.flush();
                }
                pw.close();
                reader.close();
                if (!optionFile.delete()) {
                    logger.error("ERROR - Could not delete file");
                }
                if (!tempFile.renameTo(optionFile)) logger.error("Could not rename file");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean getUseReasoner() {
        return useReasoner;
    }

    public void setUseReasoner(boolean useReasoner) {
        logger.debug("Prefs.setUseReasoner " + useReasoner);
        this.useReasoner = useReasoner;
    }

    public boolean getStepIncReasoningStatus() {
        return isStepIncOn;
    }

    public void setStepIncReasoningStatus(boolean isStepIncrementalOn) {
        logger.debug("Preferences.setStepIncrementalReasoningStatus: " + isStepIncrementalOn);
        this.isStepIncOn = isStepIncrementalOn;
    }

    public boolean getIncReasoningStatus() {
        return isIncOn;
    }

    public void setIncReasoningStatus(boolean isIncOn) {
        logger.debug("Preferences.setIncReasoningStatus: " + isIncOn);
        this.isIncOn = isIncOn;
    }

    public String getReasonerName() {
        return reasonerName;
    }

    public void setReasonerName(String name) {
        logger.debug("Prefs.setReasonerName " + name);
        this.reasonerName = name;
    }

    public boolean getConfirmOnExit() {
        return confirmOnExit;
    }

    public void setConfirmOnExit(boolean confirmOnExit) {
        this.confirmOnExit = confirmOnExit;
        GUIManager.setConfirmOnExit(confirmOnExit);
    }

    public boolean getadvMatrixEditorOptions() {
        return advxpMatrixEditorCheckBox;
    }

    public void setadvMatrixEditorOptions(boolean advxpMatrixEditorCheckBox) {
        this.advxpMatrixEditorCheckBox = advxpMatrixEditorCheckBox;
        GUIManager.setadvxpMatrixEditorCheckBox(advxpMatrixEditorCheckBox);
    }

    public boolean getadvIntersectionEditorOptions() {
        return advIntersectionEditorCheckBox;
    }

    public void setadvIntersectionEditorOptions(boolean advIntersectionEditorCheckBox) {
        this.advIntersectionEditorCheckBox = advIntersectionEditorCheckBox;
        GUIManager.setadvIntersectionEditorCheckBox(advIntersectionEditorCheckBox);
    }

    public boolean getadvSemanticParserOptions() {
        return advSemanticParserCheckBox;
    }

    public void setadvSemanticParserOptions(boolean advSemanticParserCheckBox) {
        this.advSemanticParserCheckBox = advSemanticParserCheckBox;
        GUIManager.setadvSemanticParserCheckBox(advSemanticParserCheckBox);
    }

    public Color getLightSelectionColor() {
        if (lightSelectionColor == null) lightSelectionColor = new Color(230, 230, 255);
        return lightSelectionColor;
    }

    public void setLightSelectionColor(Color lightSelectionColor) {
        this.lightSelectionColor = lightSelectionColor;
    }

    public Color getSelectionColor() {
        if (selectionColor == null) selectionColor = new Color(180, 190, 255);
        return selectionColor;
    }

    public void setSelectionColor(Color selectionColor) {
        this.selectionColor = selectionColor;
    }

    public void setButtonColor(Color buttonColor) {
        this.buttonColor = buttonColor;
    }

    public Color getButtonColor() {
        if (buttonColor == null) buttonColor = new Color(100, 149, 237);
        return buttonColor;
    }

    public Color getBackgroundColor() {
        if (backgroundColor == null) backgroundColor = new Color(216, 223, 230);
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public java.util.List<Dbxref> getPersonalDbxrefs() {
        return personalDbxrefs;
    }

    public void setPersonalDbxrefs(java.util.List<Dbxref> personalDbxrefs) {
        this.personalDbxrefs = personalDbxrefs;
    }

    public String getPersonalDefinition() {
        return "<autodef>";
    }

    public void setPersonalDefinition(String defaultDef) {
        this.defaultDef = defaultDef;
    }

    public boolean getUsePersonalDefinition() {
        return usePersonalDefinition;
    }

    public void setUsePersonalDefinition(boolean usePersonalDefinition) {
        this.usePersonalDefinition = usePersonalDefinition;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        FontUIResource resource = new FontUIResource(font);
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object o = keys.nextElement();
            if (o instanceof String) {
                String key = (String) o;
                if (!key.toLowerCase().contains("menu") && key.toLowerCase().endsWith("font")) {
                    defaults.put(key, resource);
                }
            }
        }
        for (Frame f : Frame.getFrames()) {
            SwingUtilities.updateComponentTreeUI(f);
        }
    }

    public void updateInstallJLaunchers() throws IOException {
        File optionFile = new File(getInstallationDirectory(), getLauncherName() + ".vmoptions");
        logger.info("updateInstallJLaunchers: adding -Xmx" + getMemString() + " to " + optionFile);
        PrintWriter stream = new PrintWriter(new FileWriter(optionFile));
        stream.println("-Xmx" + getMemString());
        stream.close();
    }

    public void updateInfoPlist() throws IOException {
        String mem = getMemString();
        if (mem == null) return;
        File infoPlist = new File(getInstallationDirectory(), getAppName() + ".app/Contents/Info.plist");
        logger.info("infoPlist = " + infoPlist);
        Map<String, String> params = new HashMap<String, String>();
        params.put("memoryOption", mem);
        try {
            XMLUtil.transform(Preferences.PLIST_TRANSFORM, infoPlist, params);
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }
    }

    public void updateLauncherConfigurations() {
        if (GUIUtil.isMacOS()) {
            try {
                updateInfoPlist();
            } catch (IOException ex) {
                logger.info("Could not update Mac application launcher script (Info.plist)");
            }
        }
        try {
            updateInstallJLaunchers();
        } catch (IOException ex) {
            logger.info("Could not update InstallJ " + "launcher scripts");
        }
    }

    public static String getLauncherName() {
        return System.getProperty("launcherName", getAppName());
    }

    public static String getAppName() {
        return appName;
    }

    public static void setAppName(String app) {
        appName = app;
    }

    public static boolean isBatchMode() {
        return batchMode;
    }

    public static void setBatchMode(boolean batch) {
        logger.debug(appName + ": set batchMode to " + batchMode);
        batchMode = batch;
    }

    public static List<URL> getIconLibrary() {
        return ClassUtil.getResources(Preferences.class.getClassLoader(), "/org/oboedit/gui/resources/icons/typeicons/**", "gif", "jpg", "svg");
    }

    protected static void writePreferences(Preferences preferences) throws IOException {
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(getPrefsXMLFile())));
        logger.info("Writing preferences to " + getPrefsXMLFile());
        encoder.writeObject(preferences);
        encoder.close();
        preferences.updateLauncherConfigurations();
    }

    protected static void fillInInstallationDirectory() {
        String prop = System.getProperty("launcherDir");
        if (prop != null) {
            installationDir = new File(prop);
            if (installationDir.exists() && installationDir.isDirectory()) {
                return;
            }
        }
        StringTokenizer tokenizer = new StringTokenizer(System.getProperty("java.class.path"), System.getProperty("path.separator"));
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            File file = new File(token);
            if (file.getName().equals("oboedit.jar")) {
                try {
                    installationDir = file.getCanonicalFile().getParentFile().getParentFile();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (file.getName().equals("oboedit_launcher.jar")) {
                try {
                    installationDir = file.getCanonicalFile().getParentFile();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static VersionNumber getVersion() {
        if (version == null) {
            try {
                URL url = getExtensionLoader().getResource("org/oboedit/resources/VERSION");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                version = new VersionNumber(reader.readLine());
                reader.close();
            } catch (Exception e) {
                try {
                    version = new VersionNumber("0.0");
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return version;
    }

    public static File getInstallationDirectory() {
        if (installationDir == null) fillInInstallationDirectory();
        return installationDir;
    }

    public static File getPrefsXMLFile() {
        return new File(getOBOEditPrefsDir(), "config.xml");
    }

    public static File getFiltersDir() {
        return filterFile;
    }

    public void setLogfile(String lf) {
        getPreferences().logFile = lf;
    }

    public String getLogfile() {
        return getPreferences().logFile;
    }

    public static List<String> getSystemDictFilenames() {
        return CollectionUtil.list("allowedrepeats.dict", "alwayslowercase.dict", "standard.dict", "periodwords.dict");
    }

    public static List<String> getPrefsFilenames() {
        return CollectionUtil.list("config.xml", "filter_prefs.xml", "verify.xml", "component_prefs", "perspectives");
    }

    public static Icon loadLibraryIcon(String name) {
        return getPreferences().loadLibraryIconLocal(name);
    }

    public static URL getLibraryIconURL(String name) {
        URL url = getExtensionLoader().getResource("org/oboedit/gui/resources/icons/" + name);
        if (url == null) {
            url = getExtensionLoader().getResource("org/oboedit/gui/resources/icons/typeicons/" + name);
        }
        return url;
    }

    public static Color lightSelectionColor() {
        return getPreferences().getLightSelectionColor();
    }

    public static Color defaultSelectionColor() {
        return getPreferences().getSelectionColor();
    }

    public static Color defaultBackgroundColor() {
        return getPreferences().getBackgroundColor();
    }

    public static Color defaultButtonColor() {
        return new Color(100, 149, 237);
    }

    public boolean getUseModalProgressMonitors() {
        return useModalProgressMonitors;
    }

    public void setUseModalProgressMonitors(boolean useModalProgressMonitors) {
        this.useModalProgressMonitors = useModalProgressMonitors;
    }

    public void setIconURLIndex(Map<String, String> map) {
        this.iconURLIndex = map;
        iconIndex.clear();
    }

    public Map<String, String> getIconURLIndex() {
        return iconURLIndex;
    }

    public Color getColorForRelationshipType(OBOProperty type) {
        return getColorForRelationshipType(type.getID());
    }

    public Color getColorForRelationshipType(String id) {
        Color out = colorIndex.get(id);
        if (out == null) out = Color.lightGray;
        return out;
    }

    public Icon getIconForRelationshipType(OBOProperty type) {
        return getIconForRelationshipType(type.getID(), type.getName());
    }

    public Icon getIconForRelationshipType(String id, String name) {
        Icon out = iconIndex.get(id);
        if (out == null) {
            String iconURL = iconURLIndex.get(id);
            if (iconURL != null) {
                if (iconURL.startsWith("resource:")) {
                    out = loadLibraryIcon(iconURL.substring(9));
                } else {
                    try {
                        out = getIconForURL(new URL(iconURL));
                    } catch (MalformedURLException e) {
                        File file = new File(iconURL);
                        if (file.exists()) try {
                            out = getIconForURL(file.toURL());
                        } catch (MalformedURLException e1) {
                        }
                    }
                }
            }
            if (out == null) {
                out = new TextIcon((name == null) ? id : name);
            }
            iconIndex.put(id, out);
        }
        return out;
    }

    public Map<String, Color> getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(Map<String, Color> colorIndex) {
        this.colorIndex = colorIndex;
    }
}
