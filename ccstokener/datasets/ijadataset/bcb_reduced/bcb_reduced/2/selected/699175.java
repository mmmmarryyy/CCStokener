package jsystem.treeui.publisher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import jsystem.extensions.report.xml.DbPublish;
import jsystem.framework.DBProperties;
import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;
import jsystem.runner.ErrorLevel;
import jsystem.runner.agent.publisher.Publisher;
import jsystem.runner.agent.publisher.PublisherProgress;
import jsystem.runner.agent.reportdb.tables.Package;
import jsystem.runner.agent.reportdb.tables.PropertiesList;
import jsystem.runner.agent.reportdb.tables.Run;
import jsystem.runner.agent.reportdb.tables.Step;
import jsystem.runner.agent.reportdb.tables.Test;
import jsystem.runner.agent.reportdb.tables.TestName;
import jsystem.runner.agent.reportdb.tables.TestProperties;
import jsystem.treeui.DBConnectionListener;
import jsystem.treeui.DbGuiUtility;
import jsystem.treeui.TestRunner;
import jsystem.utils.StringUtils;
import jsystem.utils.UploadRunner;

public class BackgroundPublisher implements Publisher, ActionListener, DBConnectionListener {

    boolean debug = true;

    private Run run;

    private PublisherProgress progressBar;

    private static Logger log = Logger.getLogger(DefaultPublisher.class.getName());

    private PublisherRunInfoFrame frame;

    private DbPublish reportInfo;

    private long reportIndex;

    private static Connection conn;

    private UploadRunner uploader;

    public Thread thread;

    private boolean save = false;

    private static boolean publishInProgress = false;

    public void publish() {
        try {
            createReport();
        } catch (Exception e) {
            DbGuiUtility.publishError("Publish error- fail to connect to the Database", "Fail to connect to the Database\n\n" + "Try the following :\n\n" + "1)Check that Database Server is on\n\n" + "2)Check You Database Properties \n\n" + StringUtils.getStackTrace(e), ErrorLevel.Error);
        }
    }

    /**
	 * create a publish form
	 * 
	 * @throws Exception
	 */
    private void createReport() throws Exception {
        reportIndex = System.currentTimeMillis();
        DbGuiUtility.showMsgWithTime("publish.bypass = true");
        File reportCurrent = new File(JSystemProperties.getInstance().getPreference(FrameworkOptions.LOG_FOLDER), "current");
        reportInfo = new DbPublish(reportCurrent);
        run = reportInfo.getRunForForm(reportIndex);
        frame = new PublisherRunInfoFrame(run);
        progressBar = PublisherRunInfoFrame.getProgress();
        progressBar.setBarValue(0);
        frame.addActionListener(this);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                frame.showMy();
            }
        });
        if (!checkDBPropertiesFile()) {
            return;
        }
        frame.enableButtons(false);
        DbGuiUtility.validateDialogParams(this, frame.getDb(), true);
    }

    /**
	 * start the publishing: a. zip and upload file b. publish to DB
	 * 
	 */
    public void startPublish() {
        (new Thread() {

            public void run() {
                if (publishInProgress) {
                    JOptionPane.showConfirmDialog(TestRunner.treeView, "Another publishing is currently in progress", "Publisher Message", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
                    frame.close();
                    return;
                }
                publishInProgress = true;
                frame.setStartedPublish(true);
                int numOfTests = reportInfo.getNumberOfTests() - 1;
                progressBar.setBarMinMax(0, (numOfTests + 1) * 4);
                progressBar.setBarValue(0);
                if (!writeToDB(reportInfo, reportIndex, conn, numOfTests)) {
                    publishAborted();
                    return;
                }
                if (!uploadFile(reportInfo, reportIndex)) {
                    publishAborted();
                    return;
                }
                frame.close();
                publishInProgress = false;
                DBProperties.closeConnectionIfExists(conn);
                JOptionPane.showConfirmDialog(TestRunner.treeView, "All files uploaded succesfully to the report server", "Publisher Message", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        }).start();
    }

    /**
	 * signal that a publish was terminated and do the necessary actions
	 * 
	 */
    public void publishAborted() {
        progressBar.setBarValue(0);
        frame.setStartedPublish(false);
        publishInProgress = false;
    }

    /**
	 * check that there is a db.properties file present, creates one if not
	 * 
	 * @return true if file exists
	 */
    private boolean checkDBPropertiesFile() {
        try {
            DBProperties.getInstance();
            return true;
        } catch (Exception e) {
            frame.showDBProperties();
        }
        return false;
    }

    /**
	 * zip and upload log files to server
	 * 
	 * 
	 * 1.You need to add to db.properties line serverIP="you server ip"
	 * 
	 * 2.If you server use port other than 8080 you must also change this in
	 * db.properties ->browser.port
	 * 
	 * 
	 * serverUploadUrl=http://"you server ip"/reports/upload
	 * 
	 */
    private boolean uploadFile(DbPublish reportInfo, long reportIndex) {
        File tempDir = new File(JSystemProperties.getInstance().getPreference(FrameworkOptions.LOG_FOLDER));
        File tempCurrentDir = new File(tempDir, "current");
        uploader = new UploadRunner(tempCurrentDir, reportIndex);
        DbGuiUtility.showMsgWithTime("Uploading file to reports Server");
        int max = progressBar.getMaxValue();
        try {
            uploader.zipFile();
        } catch (Exception e) {
            DbGuiUtility.publishError("Publish error- fail to zip file for publishing", "check that there is enought space for zipping\n\n" + StringUtils.getStackTrace(e), ErrorLevel.Error);
            return false;
        }
        progressBar.setBarValue(max / 4);
        try {
            frame.setMoveToBackgroundEnabled(true);
            uploader.upload();
        } catch (Exception e) {
            DbGuiUtility.publishError("Publish error- fail to upload file to reports server", "check that the reports server is running\n\n" + StringUtils.getStackTrace(e), ErrorLevel.Error);
            return false;
        } finally {
            frame.setMoveToBackgroundEnabled(false);
        }
        progressBar.setBarValue(max / 2);
        return true;
    }

    /**
	 * write all data to DB
	 * 
	 * @param reportInfo
	 *            the DbPublish object
	 * @param reportIndex
	 *            the long id of the report
	 * @param conn
	 *            the connection to the DB
	 * @param numOfTests
	 *            the number of tests to publish
	 * @return true if all succeeded
	 */
    private boolean writeToDB(DbPublish reportInfo, long reportIndex, Connection conn, int numOfTests) {
        try {
            DbGuiUtility.showMsgWithTime("creating report information from xml files");
            final int runIndex = reportInfo.addRunInfo(reportIndex, conn);
            if (runIndex == -1) {
                return false;
            }
            reportInfo.publishTestsInfo(conn, progressBar, runIndex);
            progressBar.setBarValue(numOfTests);
            DbGuiUtility.showMsgWithTime("setings tests data");
            DbGuiUtility.showMsgWithTime("finished processing tests information for publish");
            PublisherTreePanel.setPublishBtnEnable(true);
            DbGuiUtility.showMsgWithTime("finished publishing");
            log.log(Level.FINEST, "finished publishing");
            return true;
        } catch (Exception e) {
            frame.setStartedPublish(false);
            DbGuiUtility.publishError("Publish error- fail to to connect to the Database", "Fail to connect to the Database\n\n" + "Try the following :\n\n" + "1)Check that Database Server is on\n\n" + "2)Check You Database Properties \n\n" + StringUtils.getStackTrace(e), ErrorLevel.Error);
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source.equals(PublisherRunInfoFrame.okButton)) {
            run = frame.saveRunData();
            startPublish();
        } else if (source.equals(PublisherRunInfoFrame.cancelButton)) {
            frame.dispose();
            DBProperties.closeConnectionIfExists(conn);
        } else if (source.equals(DefineDbPropertiesDialog.okButton)) {
            save = true;
            DbGuiUtility.validateDialogParams(this, frame.getDb(), true);
        } else if (source.equals(DefineDbPropertiesDialog.cancelButton)) {
            DbGuiUtility.validateDialogParams(this, false);
        } else if (source.equals(PublisherRunInfoFrame.moveToBackgroudButton)) {
            frame.getFrame().setVisible(false);
        }
    }

    public void connectionIsOk(boolean status, Connection con) {
        BackgroundPublisher.conn = con;
        frame.connectionToDbExist(status);
        if (save && status) {
            DbGuiUtility.saveDialogToDb(frame.getDb(), conn);
        }
        save = false;
    }

    /**
	 * This method will cast to DBConnectionListener as it was build for this
	 * reason. casting is not dangerous if implements wasn't changed in
	 * DBGuiUtils
	 * 
	 * @param dbSettingParams
	 *            represents the following: (host, port, driver, type, dbHost,
	 *            dbName, dbUser, dbPassword);
	 */
    @Override
    public boolean validatePublisher(Object object, String... dbSettingParams) {
        DBConnectionListener listener = (DBConnectionListener) object;
        String host = dbSettingParams[0];
        String port = dbSettingParams[1];
        String driver = dbSettingParams[2];
        String type = dbSettingParams[3];
        String dbHost = dbSettingParams[4];
        String dbName = dbSettingParams[5];
        String dbUser = dbSettingParams[6];
        String dbPassword = dbSettingParams[7];
        boolean validPublisher = false;
        String url = "http://" + host + ":" + port + "/reports";
        try {
            URL _url = new URL(url);
            _url.openConnection().connect();
            validPublisher = true;
        } catch (Exception e) {
            log.log(Level.FINE, "Failed validating url " + url, e);
        }
        if (validPublisher) {
            Connection conn;
            try {
                if (driver != null) {
                    conn = DBProperties.getInstance().getConnection(driver, dbHost, dbName, type, dbUser, dbPassword);
                } else {
                    conn = DBProperties.getInstance().getConnection();
                }
            } catch (Exception e) {
                conn = null;
                listener.connectionIsOk(false, null);
                validPublisher = false;
            }
            if (validPublisher) {
                if (!allNecessaryTablesCreated(conn)) {
                    conn = null;
                    listener.connectionIsOk(false, null);
                    validPublisher = false;
                }
                listener.connectionIsOk(true, conn);
            }
        } else {
            listener.connectionIsOk(false, null);
        }
        return validPublisher;
    }

    /**
	 * Description: Verify the database contains all necessary tables for proper
	 * operation
	 * 
	 * @param conn
	 *            - A Connection object to the database.
	 * @return: True - If database contains all necessary tables False - If
	 *          database does not contain all necessary tables
	 */
    private static boolean allNecessaryTablesCreated(Connection conn) {
        boolean allCreated = true;
        try {
            allCreated = new Run().check(conn) && new TestProperties().check(conn) && new Package().check(conn) && new PropertiesList().check(conn) && new Step().check(conn) && new Test().check(conn) && new TestName().check(conn);
        } catch (SQLException e) {
            allCreated = false;
        }
        return allCreated;
    }
}
