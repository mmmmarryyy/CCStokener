package com.tegsoft.tobe.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.commons.io.IOUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import com.tegsoft.tobe.db.Counter;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.connection.Connection;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.ui.MessageType;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.FileUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.UiUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class Upgrade {

    public static void init() throws Exception {
        ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredHbox")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredAlert")).setVisible(false);
        Dataset TBLINSTALLATION = new Dataset("TBLINSTALLATION", "TBLINSTALLATION");
        TBLINSTALLATION.fill(new Command("SELECT * FROM TBLINSTALLATION  ORDER BY VERSION DESC,ORDERID"));
        boolean restartRequired = false;
        boolean unapplied = false;
        boolean downloaded = false;
        for (int i = 0; i < TBLINSTALLATION.getRowCount(); i++) {
            DataRow rowTBLINSTALLATION = TBLINSTALLATION.getRow(i);
            if ("UNAPPLIED".equals(rowTBLINSTALLATION.getString("STATUS"))) {
                unapplied = true;
                if ("TEGSOFTJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "TOBEJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "ALLJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "CONFIGASTERISK".equals(rowTBLINSTALLATION.getString("UPGRADETYPE"))) {
                    restartRequired = true;
                }
            }
            if ("DOWNLOADED".equals(rowTBLINSTALLATION.getString("STATUS"))) {
                downloaded = true;
                if ("TEGSOFTJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "TOBEJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "ALLJARS".equals(rowTBLINSTALLATION.getString("UPGRADETYPE")) || "CONFIGASTERISK".equals(rowTBLINSTALLATION.getString("UPGRADETYPE"))) {
                    restartRequired = true;
                }
            }
        }
        if (unapplied) {
            ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(true);
            ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(false);
        } else if (downloaded) {
            ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(false);
            ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(true);
        }
        if (restartRequired) {
            ((Component) UiUtil.findComponent("restartRequiredHbox")).setVisible(true);
            ((Component) UiUtil.findComponent("restartRequiredAlert")).setVisible(true);
        }
    }

    public static void checkForUpgrade(Event event) throws Exception {
        ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredHbox")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredAlert")).setVisible(false);
        new Command("DELETE FROM TBLINSTALLATION WHERE STATUS<>'APPLIED'").executeNonQuery();
        String clientAppliedVersion = new Command("SELECT MAX(VERSION) FROM TBLINSTALLATION WHERE STATUS='APPLIED'").executeScalarAsString();
        if (NullStatus.isNull(clientAppliedVersion)) {
            clientAppliedVersion = "20050101";
        }
        String macquery = "";
        String clientMAC = "";
        String clientUNITID = UiUtil.getUNITUID();
        ArrayList<String> macList = getMacList();
        for (int i = 0; i < macList.size(); i++) {
            clientMAC += macList.get(i) + ",";
            macquery += "'" + macList.get(i) + "'";
            if (i < macList.size() - 1) {
                macquery += ",";
            }
        }
        URL urlLICENSEQUERY = new URL("http://www.tegsoft.com/Tobe/forms/TobeOS/upgrade/upgrade_current.jsp?tegsoftCLIENTVERSION=" + clientAppliedVersion + "&tegsoftCLIENTUNITID=" + clientUNITID + "&tegsoftCLIENTMAC=" + clientMAC + "&tegsoftCOMMAND=LICENSEQUERY");
        URLConnection urlConnection = urlLICENSEQUERY.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String commingList = "";
        String line;
        while ((line = in.readLine()) != null) {
            commingList += line;
        }
        in.close();
        String licenseArray[] = commingList.split("</ROW>");
        if (licenseArray.length > 0) {
            String fields[] = licenseArray[0].split("</FLD>");
            String STATUS = fields[0];
            String ERRORMSG = fields[1];
            if ("FAIL".equals(STATUS)) {
                if ("NOUNITID".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_1));
                    return;
                }
                if ("NOCUSTOMER".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_1));
                    return;
                }
                if ("NOVALIDLICENSE".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_2));
                    return;
                }
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_3));
                return;
            }
        }
        Dataset TBLLICENSE = new Dataset("TBLLICENSE", "TBLLICENSE");
        Command command = new Command("DELETE FROM TBLLICENSE WHERE UNITUID={UNITUID} AND (NOTES IN (");
        command.append(macquery);
        command.append(") OR NOTES IS NULL) ");
        command.executeNonQuery();
        for (int i = 1; i < licenseArray.length; i++) {
            String fields[] = licenseArray[i].split("</FLD>");
            String LICTYPE = fields[0];
            String UNIQUEKEY = fields[1];
            String LICKEY = fields[2];
            DataRow dataRow = TBLLICENSE.addNewDataRow();
            dataRow.set("LICENSENAME", LICTYPE);
            dataRow.set("LICENSE", LICKEY);
            dataRow.set("NOTES", UNIQUEKEY);
            TBLLICENSE.save();
        }
        URL url = new URL("http://www.tegsoft.com/Tobe/forms/TobeOS/upgrade/upgrade_current.jsp?tegsoftCLIENTVERSION=" + clientAppliedVersion + "&tegsoftCLIENTUNITID=" + clientUNITID + "&tegsoftCLIENTMAC=" + clientMAC);
        urlConnection = url.openConnection();
        in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String upgradeList = "";
        while ((line = in.readLine()) != null) {
            upgradeList += line;
        }
        in.close();
        Dataset TBLINSTALLATION = new Dataset("TBLINSTALLATION", "TBLINSTALLATION");
        String upgrades[] = upgradeList.split("</ROW>");
        if (upgrades.length > 0) {
            String fields[] = upgrades[0].split("</FLD>");
            String STATUS = fields[0];
            String ERRORMSG = fields[1];
            if ("FAIL".equals(STATUS)) {
                if ("NOUNITID".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_1));
                    return;
                }
                if ("NOCUSTOMER".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_1));
                    return;
                }
                if ("NOVALIDLICENSE".equals(ERRORMSG)) {
                    UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_2));
                    return;
                }
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_3));
                return;
            }
        }
        boolean restartRequired = false;
        boolean updateAvailable = false;
        for (int i = 1; i < upgrades.length; i++) {
            String fields[] = upgrades[i].split("</FLD>");
            String VERSION = fields[0];
            String ORDERID = fields[1];
            String UPGRADETYPE = fields[2];
            String DESCRIPTION = fields[3];
            String STATUS = "UNAPPLIED";
            String PRDNAME = "TegsoftCC";
            if ("TEGSOFTJARS".equals(UPGRADETYPE) || "TOBEJARS".equals(UPGRADETYPE) || "ALLJARS".equals(UPGRADETYPE) || "CONFIGASTERISK".equals(UPGRADETYPE)) {
                restartRequired = true;
            }
            DataRow dataRow = TBLINSTALLATION.addNewDataRow();
            dataRow.set("ORDERID", ORDERID);
            dataRow.set("PRDNAME", PRDNAME);
            dataRow.set("UPGRADETYPE", UPGRADETYPE);
            dataRow.set("VERSION", VERSION);
            dataRow.set("STATUS", STATUS);
            dataRow.set("DESCRIPTION", DESCRIPTION);
            TBLINSTALLATION.save();
            updateAvailable = true;
        }
        UiUtil.getDataset("TBLINSTALLATION").reFill();
        if (restartRequired) {
            ((Component) UiUtil.findComponent("restartRequiredHbox")).setVisible(true);
            ((Component) UiUtil.findComponent("restartRequiredAlert")).setVisible(true);
        }
        if (updateAvailable) {
            ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(true);
            UiUtil.showMessage(MessageType.INFO, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_4));
        } else {
            UiUtil.showMessage(MessageType.INFO, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_5));
        }
    }

    public static ArrayList<String> getMacList() throws Exception {
        ArrayList<String> macList = new ArrayList<String>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        if (nets != null) {
            while (nets.hasMoreElements()) {
                NetworkInterface netint = nets.nextElement();
                String macKey = "";
                if (netint.getHardwareAddress() == null) {
                    continue;
                }
                for (int i = 0; i < netint.getHardwareAddress().length; i++) {
                    macKey += Converter.asHexString(netint.getHardwareAddress()[i]);
                    if (i < netint.getHardwareAddress().length - 1) {
                        macKey += ":";
                    }
                }
                macList.add(macKey);
            }
        }
        return macList;
    }

    public static void downloadUpgrade(Event event) throws Exception {
        Dataset TBLINSTALLATION = new Dataset("TBLINSTALLATION", "TBLINSTALLATION");
        TBLINSTALLATION.fill(new Command("SELECT * FROM TBLINSTALLATION WHERE STATUS='UNAPPLIED' ORDER BY VERSION DESC,ORDERID"));
        String maxVersion = TBLINSTALLATION.getRow(0).getString("VERSION");
        ArrayList<String> operationList = new ArrayList<String>();
        for (int i = 0; i < TBLINSTALLATION.getRowCount(); i++) {
            String UPGRADETYPE = TBLINSTALLATION.getRow(i).getString("UPGRADETYPE");
            if (operationList.indexOf(UPGRADETYPE) < 0) {
                operationList.add(UPGRADETYPE);
            }
        }
        for (int i = 0; i < operationList.size(); i++) {
            String downloadFileName = "";
            if (Compare.equal("FORMS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_FORMS_" + maxVersion + ".zip";
            } else if (Compare.equal("DB", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_DB_" + maxVersion + ".zip";
            } else if (Compare.equal("IMAGES", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_IMAGES_" + maxVersion + ".zip";
            } else if (Compare.equal("VIDEOS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_VIDEOS_" + maxVersion + ".zip";
            } else if (Compare.equal("TEGSOFTJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_TEGSOFTJARS_" + maxVersion + ".zip";
            } else if (Compare.equal("TOBEJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_TOBEJARS_" + maxVersion + ".zip";
            } else if (Compare.equal("ALLJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_ALLJARS_" + maxVersion + ".zip";
            } else if (Compare.equal("CONFIGSERVICE", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGSERVICE_" + maxVersion + ".zip";
            } else if (Compare.equal("CONFIGSCRIPTS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGSCRIPTS_" + maxVersion + ".zip";
            } else if (Compare.equal("CONFIGFOP", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGFOP_" + maxVersion + ".zip";
            } else if (Compare.equal("CONFIGASTERISK", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGASTERISK_" + maxVersion + ".zip";
            }
            downloadFile(downloadFileName);
            for (int j = 0; j < TBLINSTALLATION.getRowCount(); j++) {
                if (Compare.equal(TBLINSTALLATION.getRow(j).getString("UPGRADETYPE"), operationList.get(i))) {
                    TBLINSTALLATION.getRow(j).setString("STATUS", "DOWNLOADED");
                }
            }
            TBLINSTALLATION.save();
        }
        UiUtil.getDataset("TBLINSTALLATION").reFill();
        UiUtil.showMessage(MessageType.INFO, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_6));
        ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(true);
    }

    private static void downloadFile(String downloadFileName) throws Exception {
        URL getFileUrl = new URL("http://www.tegsoft.com/Tobe/getFile" + "?tegsoftFileName=" + downloadFileName);
        URLConnection getFileUrlConnection = getFileUrl.openConnection();
        InputStream is = getFileUrlConnection.getInputStream();
        String tobeHome = UiUtil.getParameter("RealPath.Context");
        OutputStream out = new FileOutputStream(tobeHome + "/setup/" + downloadFileName);
        IOUtils.copy(is, out);
        is.close();
        out.close();
    }

    public static void applyUpgrade(Event event) throws Exception {
        if (((Component) UiUtil.findComponent("restartRequiredAlert")).isVisible()) {
            if (!((Checkbox) UiUtil.findComponent("restartRequired")).isChecked()) {
                UiUtil.showMessage(MessageType.ERROR, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_9));
                return;
            }
        }
        Dataset TBLINSTALLATION = new Dataset("TBLINSTALLATION", "TBLINSTALLATION");
        TBLINSTALLATION.fill(new Command("SELECT * FROM TBLINSTALLATION WHERE STATUS='DOWNLOADED' ORDER BY VERSION DESC,ORDERID"));
        String maxVersion = TBLINSTALLATION.getRow(0).getString("VERSION");
        ArrayList<String> operationList = new ArrayList<String>();
        for (int i = 0; i < TBLINSTALLATION.getRowCount(); i++) {
            String UPGRADETYPE = TBLINSTALLATION.getRow(i).getString("UPGRADETYPE");
            if (operationList.indexOf(UPGRADETYPE) < 0) {
                operationList.add(UPGRADETYPE);
            }
        }
        String tobeHome = UiUtil.getParameter("RealPath.Context");
        new File(tobeHome + "/backup/").mkdirs();
        SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
        yyyyMMdd.setTimeZone(DateUtil.getTimezone());
        long currentDate = Long.parseLong(yyyyMMdd.format(DateUtil.today()));
        boolean databaseExecutionRequired = false;
        boolean restartRequired = false;
        for (int i = 0; i < operationList.size(); i++) {
            String downloadFileName = "";
            if (Compare.equal("FORMS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_FORMS_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/forms", tobeHome + "/backup/FORMS" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteDir(tobeHome + "/forms/");
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome);
            } else if (Compare.equal("DB", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_DB_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/sql", tobeHome + "/backup/DB" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteDir(tobeHome + "/sql/");
                databaseExecutionRequired = true;
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome);
            } else if (Compare.equal("IMAGES", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_IMAGES_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/image", tobeHome + "/backup/IMAGES" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteDir(tobeHome + "/image/");
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome);
            } else if (Compare.equal("VIDEOS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_VIDEOS_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/videos", tobeHome + "/backup/VIDEOS" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteDir(tobeHome + "/videos/");
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome);
            } else if (Compare.equal("TEGSOFTJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_TEGSOFTJARS_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/WEB-INF/lib/", tobeHome + "/backup/TEGSOFTJARS" + currentDate + Counter.getUUIDString() + ".zip", "Tegsoft", "jar");
                FileUtil.deleteMatchingFilesInDir(new File(tobeHome + "/WEB-INF/lib/"), "Tegsoft", "jar");
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome + "/WEB-INF/");
                restartRequired = true;
            } else if (Compare.equal("TOBEJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_TOBEJARS_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/WEB-INF/lib/", tobeHome + "/backup/TOBEJARS" + currentDate + Counter.getUUIDString() + ".zip", "Tobe", "jar");
                FileUtil.deleteMatchingFilesInDir(new File(tobeHome + "/WEB-INF/lib/"), "Tobe", "jar");
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome + "/WEB-INF/");
                restartRequired = true;
            } else if (Compare.equal("ALLJARS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_ALLJARS_" + maxVersion + ".zip";
                FileUtil.createZipPackage(tobeHome + "/WEB-INF/lib/", tobeHome + "/backup/AllJARS" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteAllFilesInDir(new File(tobeHome + "/WEB-INF/lib/"));
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, tobeHome + "/WEB-INF/");
                restartRequired = true;
            } else if (Compare.equal("CONFIGSERVICE", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGSERVICE_" + maxVersion + ".zip";
                FileUtil.createZipPackage("/etc/init.d/", tobeHome + "/backup/CONFIGSERVICE" + currentDate + Counter.getUUIDString() + ".zip", "tegsoft", null);
                FileUtil.deleteMatchingFilesInDir(new File("/etc/init.d/"), "tegsoft", null);
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, "/etc/");
            } else if (Compare.equal("CONFIGSCRIPTS", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGSCRIPTS_" + maxVersion + ".zip";
                FileUtil.createZipPackage("/root/", tobeHome + "/backup/CONFIGSCRIPTS" + currentDate + Counter.getUUIDString() + ".zip", "tegsoft", null);
                FileUtil.deleteMatchingFilesInDir(new File("/root/"), "tegsoft", null);
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, "/");
            } else if (Compare.equal("CONFIGFOP", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGFOP_" + maxVersion + ".zip";
                FileUtil.createZipPackage("/root/fop/", tobeHome + "/backup/CONFIGFOP" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteAllFilesInDir(new File("/root/fop/"));
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, "/root/");
            } else if (Compare.equal("CONFIGASTERISK", operationList.get(i))) {
                downloadFileName = "Tegsoft" + "_CONFIGASTERISK_" + maxVersion + ".zip";
                FileUtil.createZipPackage("/etc/asterisk/", tobeHome + "/backup/CONFIGASTERISK" + currentDate + Counter.getUUIDString() + ".zip");
                FileUtil.deleteAllFilesInDir(new File("/etc/asterisk/"));
                FileUtil.extractZipPackage(tobeHome + "/setup/" + downloadFileName, "/etc/");
                restartRequired = true;
            }
            for (int j = 0; j < TBLINSTALLATION.getRowCount(); j++) {
                if (Compare.equal("DB", operationList.get(i))) {
                    continue;
                }
                if (Compare.equal(TBLINSTALLATION.getRow(j).getString("UPGRADETYPE"), operationList.get(i))) {
                    TBLINSTALLATION.getRow(j).setString("STATUS", "APPLIED");
                }
            }
            TBLINSTALLATION.save();
        }
        if (databaseExecutionRequired) {
            File sqlFolder = new File(tobeHome + "/sql");
            File sqlFiles[] = sqlFolder.listFiles();
            for (int i = 0; i < sqlFiles.length; i++) {
                BufferedReader in = new BufferedReader(new FileReader(sqlFiles[i]));
                String sql = "";
                String str = "";
                while ((str = in.readLine()) != null) {
                    sql += str + "\n ";
                }
                in.close();
                String sqlCommands[] = sql.split(";");
                for (String sqlCommand : sqlCommands) {
                    if (NullStatus.isNull(sqlCommand)) {
                        continue;
                    }
                    if (sqlCommand.startsWith("//")) {
                        continue;
                    }
                    if (sqlCommand.startsWith("--")) {
                        continue;
                    }
                    Command command = new Command(sqlCommand);
                    try {
                        command.executeNonQuery();
                        Connection.getActive().commit();
                    } catch (Exception ex) {
                    }
                    Connection.closeActive();
                }
            }
            for (int j = 0; j < TBLINSTALLATION.getRowCount(); j++) {
                if (Compare.equal(TBLINSTALLATION.getRow(j).getString("UPGRADETYPE"), "DB")) {
                    TBLINSTALLATION.getRow(j).setString("STATUS", "APPLIED");
                }
            }
            TBLINSTALLATION.save();
        }
        String clientAppliedVersion = new Command("SELECT MAX(VERSION) FROM TBLINSTALLATION WHERE STATUS='APPLIED'").executeScalarAsString();
        if (NullStatus.isNull(clientAppliedVersion)) {
            clientAppliedVersion = "20050101";
        }
        String clientMAC = "";
        String clientUNITID = UiUtil.getUNITUID();
        ArrayList<String> macList = getMacList();
        for (int i = 0; i < macList.size(); i++) {
            clientMAC += macList.get(i) + ",";
        }
        URL urlUPGRADECOMPLETE = new URL("http://www.tegsoft.com/Tobe/forms/TobeOS/upgrade/upgrade_current.jsp?tegsoftCLIENTVERSION=" + clientAppliedVersion + "&tegsoftCLIENTUNITID=" + clientUNITID + "&tegsoftCLIENTMAC=" + clientMAC + "&tegsoftCOMMAND=UPGRADECOMPLETE");
        URLConnection connectionUPGRADECOMPLETE = urlUPGRADECOMPLETE.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connectionUPGRADECOMPLETE.getInputStream()));
        while (in.readLine() != null) {
        }
        in.close();
        UiUtil.getDataset("TBLINSTALLATION").reFill();
        ((Component) UiUtil.findComponent("downloadUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("applyUpgrade")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredHbox")).setVisible(false);
        ((Component) UiUtil.findComponent("restartRequiredAlert")).setVisible(false);
        if (restartRequired) {
            Runtime.getRuntime().exec("/root/tegsoft_restartSystem.sh");
            UiUtil.showMessage(MessageType.INFO, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_7));
        } else {
            UiUtil.showMessage(MessageType.INFO, MessageUtil.getMessage(Upgrade.class, Messages.upgrade_8));
        }
    }

    public static void main(String[] args) throws Exception {
        URL urlUPGRADECOMPLETE = new URL("http://www.tegsoft.com/Tobe/forms/TobeOS/upgrade/upgrade_current.jsp?tegsoftCLIENTVERSION=" + "20110810" + "&tegsoftCLIENTUNITID=" + "4a55c1e3-edd5-46ef-b66f-d74634e8469a" + "&tegsoftCLIENTMAC=" + "98:4b:e1:4d:b1:b1" + "&tegsoftCOMMAND=UPGRADECOMPLETE");
        URLConnection connectionUPGRADECOMPLETE = urlUPGRADECOMPLETE.openConnection();
        InputStream is = connectionUPGRADECOMPLETE.getInputStream();
        is.close();
    }

    public enum Messages {

        /**
		 * Your customer registration is not valid.
		 * 
		 */
        upgrade_1, /**
		 * Your license is not valid.
		 * 
		 */
        upgrade_2, /**
		 * An error occurred while transferring data from server.
		 * 
		 */
        upgrade_3, /**
		 * There are updates to apply.
		 * 
		 */
        upgrade_4, /**
		 * Your version is the latest version.
		 * 
		 */
        upgrade_5, /**
		 * Download complete. Please continue with apply button.
		 * 
		 */
        upgrade_6, /**
		 * Upgrade complete successfully. System will restart now. Please login
		 * after 2 minutes.
		 * 
		 */
        upgrade_7, /**
		 * Upgrade complete successfully.
		 * 
		 */
        upgrade_8, /**
		 * This upgrade requires system restart. To apply this upgrade you have
		 * to accept restart and check checkbox.
		 * 
		 */
        upgrade_9
    }
}
