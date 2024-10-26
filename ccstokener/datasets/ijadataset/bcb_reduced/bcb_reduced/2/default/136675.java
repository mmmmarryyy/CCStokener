import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

/**
 * The Speed class will attempt to retrieve a file and based on the time it took
 * and the filesize calculate the downloadspeed.
 * A timeout can be defined in case the download takes too long to complete.
 * 
 */
public class Speed implements iTest {

    private HashMap result = new HashMap();

    private String target = null;

    private String testTarget = null;

    private String fileName = null;

    private int timeout = 10;

    private boolean timedout = false;

    public Speed(HashMap paramMap) {
        this.testTarget = (String) paramMap.get("target");
        if (this.testTarget != null) {
            this.timeout = Integer.parseInt((String) paramMap.get("timeout"));
        }
    }

    /**
     * Executes the test and sets the result HashMap containing the testname and the result.
     */
    public void run() {
        try {
            URL url = new URL(this.testTarget);
            String[] urlSplit = this.testTarget.split("/");
            this.fileName = urlSplit[urlSplit.length - 1];
            this.target = url.getProtocol() + "://" + url.getHost();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(this.fileName));
            URLConnection urlConnection = url.openConnection();
            InputStream in = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long fileSize = 0;
            long start = System.currentTimeMillis();
            while (((numRead = in.read(buffer)) != -1) && (!timedout)) {
                out.write(buffer, 0, numRead);
                fileSize += numRead;
                long timePassed = System.currentTimeMillis() - start;
                if (timePassed >= (timeout * 1000)) {
                    timedout = true;
                }
            }
            long end = System.currentTimeMillis();
            in.close();
            out.close();
            long time = end - start;
            Debug.log("Speed Test", "start: " + start + ", end: " + end + ", diff: " + time);
            Debug.log("Speed Test", "filesize: " + fileSize);
            String speed = "" + ((fileSize / time) * 1000) / 1024;
            new File(this.fileName).delete();
            this.result.put("Speed", speed);
            Debug.log("Speed Test", "download speed: " + (fileSize / time) + " B/ms = " + speed + " KB/s");
        } catch (FileNotFoundException e) {
            Debug.log("Speed Test", "Couldn't download file");
            this.result.put("Speed", "FAIL");
        } catch (Exception e) {
            Debug.log("Speed Test", "Test failed");
            this.result.put("Speed", "FAIL");
        }
    }

    /**
     * @return a String containing the target url of the file to download
     */
    public String getTarget() {
        return this.target;
    }

    /**
     * 
     * @return a HashMap containing the result of the test.
     * The key of the HashMap entry contains the testname and value of that key contains the actual result
     */
    public HashMap getResult() {
        return this.result;
    }

    public String toString() {
        return "[Speed test " + testTarget + " timeout=" + timeout + "s]";
    }
}
