package org.quartz.plugins.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.transaction.UserTransaction;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.jobs.FileScanJob;
import org.quartz.jobs.FileScanListener;
import org.quartz.plugins.SchedulerPluginWithUserTransactionSupport;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.JobSchedulingDataProcessor;

/**
 * This plugin loads XML file(s) to add jobs and schedule them with triggers
 * as the scheduler is initialized, and can optionally periodically scan the
 * file for changes.
 * 
 * <p>
 * The periodically scanning of files for changes is not currently supported in a 
 * clustered environment.
 * </p>
 * 
 * <p>
 * If using the JobInitializationPlugin with JobStoreCMT, be sure to set the
 * plugin property <em>wrapInUserTransaction</em> to true.  Also, if have a 
 * positive <em>scanInterval</em> be sure to set 
 * <em>org.quartz.scheduler.wrapJobExecutionInUserTransaction</em> to true.
 * </p>
 * 
 * @author James House
 * @author Pierre Awaragi
 * @author pl47ypus
 */
public class JobInitializationPlugin extends SchedulerPluginWithUserTransactionSupport implements FileScanListener {

    private static final int MAX_JOB_TRIGGER_NAME_LEN = 80;

    private static final String JOB_INITIALIZATION_PLUGIN_NAME = "JobInitializationPlugin";

    private static final String FILE_NAME_DELIMITERS = ",";

    private boolean overWriteExistingJobs = false;

    private boolean failOnFileNotFound = true;

    private String fileNames = JobSchedulingDataProcessor.QUARTZ_XML_FILE_NAME;

    private Map jobFiles = new HashMap();

    private boolean validating = false;

    private boolean validatingSchema = true;

    private long scanInterval = 0;

    boolean started = false;

    protected ClassLoadHelper classLoadHelper = null;

    private Set jobTriggerNameSet = new HashSet();

    public JobInitializationPlugin() {
    }

    /**
     * The file name (and path) to the XML file that should be read.
     * @deprecated Use fileNames with just one file.
     */
    public String getFileName() {
        return fileNames;
    }

    /**
     * The file name (and path) to the XML file that should be read.
     * @deprecated Use fileNames with just one file.
     */
    public void setFileName(String fileName) {
        getLog().warn("The \"filename\" plugin property is deprecated.  Please use \"filenames\" in the future.");
        this.fileNames = fileName;
    }

    /**
     * Comma separated list of file names (with paths) to the XML files that should be read.
     */
    public String getFileNames() {
        return fileNames;
    }

    /**
     * The file name (and path) to the XML file that should be read.
     */
    public void setFileNames(String fileNames) {
        this.fileNames = fileNames;
    }

    /**
     * Whether or not jobs defined in the XML file should be overwrite existing
     * jobs with the same name.
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
     * The interval (in seconds) at which to scan for changes to the file.  
     * If the file has been changed, it is re-loaded and parsed.   The default 
     * value for the interval is 0, which disables scanning.
     * 
     * @return Returns the scanInterval.
     */
    public long getScanInterval() {
        return scanInterval / 1000;
    }

    /**
     * The interval (in seconds) at which to scan for changes to the file.  
     * If the file has been changed, it is re-loaded and parsed.   The default 
     * value for the interval is 0, which disables scanning.
     * 
     * @param scanInterval The scanInterval to set.
     */
    public void setScanInterval(long scanInterval) {
        this.scanInterval = scanInterval * 1000;
    }

    /**
     * Whether or not initialization of the plugin should fail (throw an
     * exception) if the file cannot be found. Default is <code>true</code>.
     */
    public boolean isFailOnFileNotFound() {
        return failOnFileNotFound;
    }

    /**
     * Whether or not initialization of the plugin should fail (throw an
     * exception) if the file cannot be found. Default is <code>true</code>.
     */
    public void setFailOnFileNotFound(boolean failOnFileNotFound) {
        this.failOnFileNotFound = failOnFileNotFound;
    }

    /**
     * Whether or not the XML should be validated. Default is <code>false</code>.
     */
    public boolean isValidating() {
        return validating;
    }

    /**
     * Whether or not the XML should be validated. Default is <code>false</code>.
     */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Whether or not the XML schema should be validated. Default is <code>true</code>.
     */
    public boolean isValidatingSchema() {
        return validatingSchema;
    }

    /**
     * Whether or not the XML schema should be validated. Default is <code>true</code>.
     */
    public void setValidatingSchema(boolean validatingSchema) {
        this.validatingSchema = validatingSchema;
    }

    /**
     * <p>
     * Called during creation of the <code>Scheduler</code> in order to give
     * the <code>SchedulerPlugin</code> a chance to initialize.
     * </p>
     * 
     * @throws org.quartz.SchedulerConfigException
     *           if there is an error initializing.
     */
    public void initialize(String name, final Scheduler scheduler) throws SchedulerException {
        super.initialize(name, scheduler);
        classLoadHelper = new CascadingClassLoadHelper();
        classLoadHelper.initialize();
        getLog().info("Registering Quartz Job Initialization Plug-in.");
        StringTokenizer stok = new StringTokenizer(fileNames, FILE_NAME_DELIMITERS);
        while (stok.hasMoreTokens()) {
            final String fileName = stok.nextToken();
            final JobFile jobFile = new JobFile(fileName);
            jobFiles.put(fileName, jobFile);
        }
    }

    public void start(UserTransaction userTransaction) {
        try {
            if (jobFiles.isEmpty() == false) {
                if (scanInterval > 0) {
                    getScheduler().getContext().put(JOB_INITIALIZATION_PLUGIN_NAME + '_' + getName(), this);
                }
                Iterator iterator = jobFiles.values().iterator();
                while (iterator.hasNext()) {
                    JobFile jobFile = (JobFile) iterator.next();
                    if (scanInterval > 0) {
                        String jobTriggerName = buildJobTriggerName(jobFile.getFileBasename());
                        SimpleTrigger trig = new SimpleTrigger(jobTriggerName, JOB_INITIALIZATION_PLUGIN_NAME, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, scanInterval);
                        trig.setVolatility(true);
                        JobDetail job = new JobDetail(jobTriggerName, JOB_INITIALIZATION_PLUGIN_NAME, FileScanJob.class);
                        job.setVolatility(true);
                        job.getJobDataMap().put(FileScanJob.FILE_NAME, jobFile.getFileName());
                        job.getJobDataMap().put(FileScanJob.FILE_SCAN_LISTENER_NAME, JOB_INITIALIZATION_PLUGIN_NAME + '_' + getName());
                        getScheduler().scheduleJob(job, trig);
                    }
                    processFile(jobFile);
                }
            }
        } catch (SchedulerException se) {
            getLog().error("Error starting background-task for watching jobs file.", se);
        } finally {
            started = true;
        }
    }

    /**
     * Helper method for generating unique job/trigger name for the  
     * file scanning jobs (one per FileJob).  The unique names are saved
     * in jobTriggerNameSet.
     */
    private String buildJobTriggerName(String fileBasename) {
        String jobTriggerName = JOB_INITIALIZATION_PLUGIN_NAME + '_' + getName() + '_' + fileBasename.replace('.', '_');
        if (jobTriggerName.length() > MAX_JOB_TRIGGER_NAME_LEN) {
            jobTriggerName = jobTriggerName.substring(0, MAX_JOB_TRIGGER_NAME_LEN);
        }
        int currentIndex = 1;
        while (jobTriggerNameSet.add(jobTriggerName) == false) {
            if (currentIndex > 1) {
                jobTriggerName = jobTriggerName.substring(0, jobTriggerName.lastIndexOf('_'));
            }
            String numericSuffix = "_" + currentIndex++;
            if (jobTriggerName.length() > (MAX_JOB_TRIGGER_NAME_LEN - numericSuffix.length())) {
                jobTriggerName = jobTriggerName.substring(0, (MAX_JOB_TRIGGER_NAME_LEN - numericSuffix.length()));
            }
            jobTriggerName += numericSuffix;
        }
        return jobTriggerName;
    }

    /**
     * Overriden to ignore <em>wrapInUserTransaction</em> because shutdown()
     * does not interact with the <code>Scheduler</code>. 
     */
    public void shutdown() {
    }

    private void processFile(JobFile jobFile) {
        if (jobFile == null || !jobFile.getFileFound()) {
            return;
        }
        JobSchedulingDataProcessor processor = new JobSchedulingDataProcessor(this.classLoadHelper, isValidating(), isValidatingSchema());
        try {
            processor.processFileAndScheduleJobs(jobFile.getFileName(), jobFile.getFileName(), getScheduler(), isOverWriteExistingJobs());
        } catch (Exception e) {
            getLog().error("Error scheduling jobs: " + e.getMessage(), e);
        }
    }

    public void processFile(String filePath) {
        processFile((JobFile) jobFiles.get(filePath));
    }

    /** 
     * @see org.quartz.jobs.FileScanListener#fileUpdated(java.lang.String)
     */
    public void fileUpdated(String fileName) {
        if (started) {
            processFile(fileName);
        }
    }

    class JobFile {

        private String fileName;

        private String filePath;

        private String fileBasename;

        private boolean fileFound;

        protected JobFile(String fileName) throws SchedulerException {
            this.fileName = fileName;
            initialize();
        }

        protected String getFileName() {
            return fileName;
        }

        protected boolean getFileFound() {
            return fileFound;
        }

        protected String getFilePath() {
            return filePath;
        }

        protected String getFileBasename() {
            return fileBasename;
        }

        private void initialize() throws SchedulerException {
            InputStream f = null;
            try {
                String furl = null;
                File file = new File(getFileName());
                if (!file.exists()) {
                    URL url = classLoadHelper.getResource(getFileName());
                    if (url != null) {
                        furl = URLDecoder.decode(url.getPath());
                        file = new File(furl);
                        try {
                            f = url.openStream();
                        } catch (IOException ignor) {
                        }
                    }
                } else {
                    try {
                        f = new java.io.FileInputStream(file);
                    } catch (FileNotFoundException e) {
                    }
                }
                if (f == null) {
                    if (isFailOnFileNotFound()) {
                        throw new SchedulerException("File named '" + getFileName() + "' does not exist.");
                    } else {
                        getLog().warn("File named '" + getFileName() + "' does not exist.");
                    }
                } else {
                    fileFound = true;
                    filePath = (furl != null) ? furl : file.getAbsolutePath();
                    fileBasename = file.getName();
                }
            } finally {
                try {
                    if (f != null) {
                        f.close();
                    }
                } catch (IOException ioe) {
                    getLog().warn("Error closing jobs file " + getFileName(), ioe);
                }
            }
        }
    }
}
