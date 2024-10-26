package purej.job.plugins.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import purej.context.ApplicationContext;
import purej.context.ApplicationContextFactory;
import purej.job.JobDetail;
import purej.job.Scheduler;
import purej.job.SchedulerConfigException;
import purej.job.SchedulerException;
import purej.job.SimpleTrigger;
import purej.job.simpl.CascadingClassLoadHelper;
import purej.job.spi.ClassLoadHelper;
import purej.job.spi.SchedulerPlugin;
import purej.job.task.FileScanJob;
import purej.job.task.FileScanListener;
import purej.job.xml.JobSchedulingDataProcessor;
import purej.logging.Logger;
import purej.logging.LoggerFactory;

/**
 * This plugin loads an XML file to add jobs and schedule them with triggers as
 * the scheduler is initialized, and can optionally periodically scan the file
 * for changes.
 * 
 * @author James House
 * @author Pierre Awaragi
 * @since 1.1 �⽺���� ���� ������ ���ø����̼� ���ؽ�Ʈ�� ���� �ε��ϰ� ����
 */
public class JobInitializationPlugin implements SchedulerPlugin, FileScanListener {

    private String name;

    private Scheduler scheduler;

    private boolean overWriteExistingJobs = false;

    private boolean failOnFileNotFound = true;

    private boolean fileFound = false;

    private String fileName = null;

    private String filePath = null;

    private boolean useContextClassLoader = true;

    private boolean validating = false;

    private boolean validatingSchema = true;

    private long scanInterval = 0;

    boolean initializing = true;

    boolean started = false;

    protected ClassLoadHelper classLoadHelper = null;

    public JobInitializationPlugin() {
        initialize();
    }

    /**
     * �⽺���췯 ���� ���� �ʱ�ȭ
     */
    private void initialize() {
        try {
            ApplicationContextFactory factory = ApplicationContextFactory.getInstance();
            ApplicationContext appContext = factory.getApplicationContext();
            fileName = appContext.getFRAMEWORK_REPOSITORY_PATH() + appContext.getMODULE_JOB_SCHEDULE_CONFIG_PATH();
            java.io.File jobsConfigFile = new java.io.File(fileName);
            if (!jobsConfigFile.exists()) throw new Exception(" Job schedule config file not found error : " + fileName);
        } catch (Exception ex) {
            String msg = "JobScheduler initialize exception : " + ex.getMessage();
            getLog().error(msg, ex);
            ex.printStackTrace();
        }
    }

    /**
     * The file name (and path) to the XML file that should be read.
     * 
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * The file name (and path) to the XML file that should be read.
     * 
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Whether or not jobs defined in the XML file should be overwrite existing
     * jobs with the same name.
     * 
     * @return
     */
    public boolean isOverWriteExistingJobs() {
        return overWriteExistingJobs;
    }

    /**
     * Whether or not jobs defined in the XML file should be overwrite existing
     * jobs with the same name.
     * 
     * @param overWriteExistingJobs
     */
    public void setOverWriteExistingJobs(boolean overWriteExistingJobs) {
        this.overWriteExistingJobs = overWriteExistingJobs;
    }

    /**
     * The interval (in seconds) at which to scan for changes to the file. If
     * the file has been changed, it is re-loaded and parsed. The default value
     * for the interval is 0, which disables scanning.
     * 
     * @return Returns the scanInterval.
     */
    public long getScanInterval() {
        return scanInterval / 1000;
    }

    /**
     * The interval (in seconds) at which to scan for changes to the file. If
     * the file has been changed, it is re-loaded and parsed. The default value
     * for the interval is 0, which disables scanning.
     * 
     * @param scanInterval
     *                The scanInterval to set.
     */
    public void setScanInterval(long scanInterval) {
        this.scanInterval = scanInterval * 1000;
    }

    /**
     * Whether or not initialization of the plugin should fail (throw an
     * exception) if the file cannot be found. Default is <code>true</code>.
     * 
     * @return
     */
    public boolean isFailOnFileNotFound() {
        return failOnFileNotFound;
    }

    /**
     * Whether or not initialization of the plugin should fail (throw an
     * exception) if the file cannot be found. Default is <code>true</code>.
     * 
     * @param overWriteExistingJobs
     */
    public void setFailOnFileNotFound(boolean failOnFileNotFound) {
        this.failOnFileNotFound = failOnFileNotFound;
    }

    /**
     * Whether or not the context class loader should be used. Default is
     * <code>true</code>.
     * 
     * @return
     */
    public boolean isUseContextClassLoader() {
        return useContextClassLoader;
    }

    /**
     * Whether or not context class loader should be used. Default is
     * <code>true</code>.
     * 
     * @param useContextClassLoader
     */
    public void setUseContextClassLoader(boolean useContextClassLoader) {
        this.useContextClassLoader = useContextClassLoader;
    }

    /**
     * Whether or not the XML should be validated. Default is <code>false</code>.
     * 
     * @return
     */
    public boolean isValidating() {
        return validating;
    }

    /**
     * Whether or not the XML should be validated. Default is <code>false</code>.
     * 
     * @param validating
     */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Whether or not the XML schema should be validated. Default is
     * <code>true</code>.
     * 
     * @return
     */
    public boolean isValidatingSchema() {
        return validatingSchema;
    }

    /**
     * Whether or not the XML schema should be validated. Default is
     * <code>true</code>.
     * 
     * @param validatingSchema
     */
    public void setValidatingSchema(boolean validatingSchema) {
        this.validatingSchema = validatingSchema;
    }

    protected static Logger getLog() {
        return LoggerFactory.getLogger(JobInitializationPlugin.class, Logger.FRAMEWORK);
    }

    /**
     * <p>
     * Called during creation of the <code>Scheduler</code> in order to give
     * the <code>SchedulerPlugin</code> a chance to initialize.
     * </p>
     * 
     * @throws SchedulerConfigException
     *                 if there is an error initializing.
     */
    public void initialize(String name, final Scheduler scheduler) throws SchedulerException {
        initializing = true;
        classLoadHelper = new CascadingClassLoadHelper();
        classLoadHelper.initialize();
        try {
            this.name = name;
            this.scheduler = scheduler;
            getLog().info("Registering Job Initialization Plug-in.");
            findFile();
        } finally {
            initializing = false;
        }
    }

    private String getFilePath() throws SchedulerException {
        if (this.filePath == null) {
            findFile();
        }
        return this.filePath;
    }

    /**
     * 
     */
    private void findFile() throws SchedulerException {
        java.io.InputStream f = null;
        String furl = null;
        File file = new File(getFileName());
        if (!file.exists()) {
            URL url = classLoadHelper.getResource(getFileName());
            if (url != null) {
                try {
                    furl = URLDecoder.decode(url.getPath(), "UTF-8");
                    file = new File(furl);
                    f = url.openStream();
                } catch (java.io.UnsupportedEncodingException uee) {
                } catch (IOException ignor) {
                }
            }
        } else {
            try {
                f = new java.io.FileInputStream(file);
            } catch (FileNotFoundException e) {
            }
        }
        if (f == null && isFailOnFileNotFound()) {
            throw new SchedulerException("File named '" + getFileName() + "' does not exist. f == null && isFailOnFileNotFound()");
        } else if (f == null) {
            getLog().warn("File named '" + getFileName() + "' does not exist. f == null");
        } else {
            fileFound = true;
            try {
                if (furl != null) this.filePath = furl; else this.filePath = file.getAbsolutePath();
                f.close();
            } catch (IOException ioe) {
                getLog().warn("Error closing jobs file " + getFileName(), ioe);
            }
        }
    }

    public void start() {
        if (scanInterval > 0) {
            try {
                SimpleTrigger trig = new SimpleTrigger("JobInitializationPlugin_" + name, "JobInitializationPlugin", new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, scanInterval);
                trig.setVolatility(true);
                JobDetail job = new JobDetail("JobInitializationPlugin_" + name, "JobInitializationPlugin", FileScanJob.class);
                job.setVolatility(true);
                job.getJobDataMap().put(FileScanJob.FILE_NAME, getFilePath());
                job.getJobDataMap().put(FileScanJob.FILE_SCAN_LISTENER_NAME, "JobInitializationPlugin_" + name);
                scheduler.getContext().put("JobInitializationPlugin_" + name, this);
                scheduler.scheduleJob(job, trig);
            } catch (SchedulerException se) {
                getLog().error("Error starting background-task for watching jobs file.", se);
            }
        }
        try {
            processFile();
        } finally {
            started = true;
        }
    }

    /**
     * <p>
     * Called in order to inform the <code>SchedulerPlugin</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     * </p>
     */
    public void shutdown() {
    }

    public void processFile() {
        if (!fileFound) return;
        JobSchedulingDataProcessor processor = new JobSchedulingDataProcessor(isUseContextClassLoader(), isValidating(), isValidatingSchema());
        try {
            processor.processFileAndScheduleJobs(fileName, scheduler, isOverWriteExistingJobs());
        } catch (Exception e) {
            getLog().error("Error scheduling jobs: " + e.getMessage(), e);
        }
    }

    /**
     * @see purej.job.task.FileScanListener#fileUpdated(java.lang.String)
     */
    public void fileUpdated(String fileName) {
        if (started) processFile();
    }
}
