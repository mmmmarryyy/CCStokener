package gov.nasa.gsfc.visbard.model.threadtask.soap;

import gov.nasa.gsfc.visbard.model.VisbardMain;
import gov.nasa.gsfc.visbard.model.threadtask.FileExaminer;
import gov.nasa.gsfc.visbard.model.threadtask.ThreadTask;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Category;

/**
 * ThreadTask for interacting with web services.
 *
 * @author asmith
 */
public class SoapConnectionTask extends ThreadTask {

    private static final Category LOG = Category.getInstance(SoapConnectionTask.class);

    private static final int XFER_BUFFER_SIZE = 1024 * 50;

    /**
     * Stores the SoapTasks value.
     * 
     * @see gov.nasa.gsfc.visbard.model.threadtask.soap.SoapTasks
     */
    private int _taskType;

    /**
     * Stores the URL of data returned by the VSPO service for a given 
     * product/accessor.
     */
    private String[] _dataUrls;

    /**
	 * Stores references to the data files on the local disk.
	 */
    private File _localFiles[] = null;

    /**
     * Creates a soap connection task with the given task type.
     * 
     * @param taskType
     */
    public SoapConnectionTask(int taskType) {
        super(SoapTasks.getTaskName(taskType), true);
        _taskType = taskType;
    }

    /**
     * Changes the task type to the given one.
     * 
     * @param taskType
     */
    public void newTask(int taskType) {
        setTaskTitle(SoapTasks.getTaskName(taskType));
        _taskType = taskType;
    }

    /**
     * Sets the current data URLs.
     * 
	 * @param dataUrls
	 */
    public void setDataUrls(String dataUrls[]) {
        _dataUrls = dataUrls;
    }

    /**
     * @return Returns the current task type.
     */
    protected int getTaskType() {
        return _taskType;
    }

    /**
     * @return Returns the data URLs that were last retrieved.
     */
    public String[] getDataUrls() {
        return _dataUrls;
    }

    /**
     * @return Returns the local files that were last retrieved.
     */
    public File[] getLocalFiles() {
        return _localFiles;
    }

    public void execute() throws Exception {
        switch(getTaskType()) {
            case SoapTasks.TRANSFER_FILES:
                if (_dataUrls == null) {
                    throw new Exception("URLs to data not provided. Aborting.");
                }
                _localFiles = new File[_dataUrls.length];
                String dir = VisbardMain.getRemoteDataDir().getAbsolutePath();
                for (int i = 0; i < _dataUrls.length; i++) {
                    if (_dataUrls[i] == null) throw new MalformedURLException("null URL provided");
                    URL url = new URL(_dataUrls[i]);
                    String fileName = url.getFile().substring(url.getFile().lastIndexOf('/'));
                    _localFiles[i] = new File(dir + fileName);
                    BufferedOutputStream bufOutStream = new BufferedOutputStream(new FileOutputStream(_localFiles[i]));
                    URLConnection URLConn = url.openConnection();
                    BufferedInputStream bufInStream = new BufferedInputStream(URLConn.getInputStream());
                    float fileSize = URLConn.getContentLength();
                    setTaskTitle("Retrieving " + fileName.substring(1) + "  Filesize: " + (int) fileSize);
                    setProgress(i / (float) _dataUrls.length);
                    byte buffer[] = new byte[XFER_BUFFER_SIZE];
                    int len = 0;
                    int offset = 0;
                    while ((len = bufInStream.read(buffer)) > 0) {
                        bufOutStream.write(buffer, 0, len);
                        offset += len;
                        setProgress(offset / fileSize);
                        if (this.isInterrupted()) {
                            bufInStream.close();
                            bufOutStream.close();
                            return;
                        }
                    }
                    bufInStream.close();
                    bufOutStream.close();
                }
                setProgress(1f);
                break;
            default:
                LOG.error("Unknown SoapConnectionTask requested");
                break;
        }
    }
}
