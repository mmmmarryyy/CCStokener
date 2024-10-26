package org.opennms.protocols.sftp;

import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import junit.framework.Assert;
import org.junit.Test;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.protocols.xml.collector.UrlFactory;

/**
 * The Class Sftp3gppUrlConnectionTest.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class Sftp3gppUrlConnectionTest {

    /**
     * Test path for Standard SFTP
     *
     * @throws Exception the exception
     */
    @Test
    public void testPathForSFTP() throws Exception {
        URL url = UrlFactory.getUrl("sftp://admin:admin@192.168.1.1/opt/hitachi/cnp/data/pm/reports/3gpp/5/data.xml");
        URLConnection conn = url.openConnection();
        Assert.assertTrue(conn instanceof SftpUrlConnection);
        UrlFactory.disconnect(conn);
    }

    /**
     * Test path for 3GPP (NE Mode ~ A).
     *
     * @throws Exception the exception
     */
    @Test
    public void testPathFor3GPPA() throws Exception {
        URL url = UrlFactory.getUrl("sftp.3gpp://admin:admin@192.168.1.1/opt/hitachi/cnp/data/pm/reports/3gpp/5?step=300&timezone=GMT-5&neId=MME00001");
        URLConnection conn = url.openConnection();
        Assert.assertTrue(conn instanceof Sftp3gppUrlConnection);
        String path = ((Sftp3gppUrlConnection) conn).getPath();
        log().debug(path);
        UrlFactory.disconnect(conn);
    }

    /**
     * Test path for 3GPP (NE Mode ~ A), using custom timestamp as a reference.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCustomPathFor3GPPA() throws Exception {
        long ts = 1320257100000l;
        Date date = new Date(ts);
        log().debug("Timestamp = " + date);
        URL url = UrlFactory.getUrl("sftp.3gpp://admin:admin@192.168.1.1/opt/3gpp?step=300&timezone=GMT-5&neId=MME00001&referenceTimestamp=" + ts);
        URLConnection conn = url.openConnection();
        Assert.assertTrue(conn instanceof Sftp3gppUrlConnection);
        String path = ((Sftp3gppUrlConnection) conn).getPath();
        log().debug(path);
        UrlFactory.disconnect(conn);
        Assert.assertEquals("/opt/3gpp/A20111102.1300-0500-1305-0500_MME00001", path);
    }

    /**
     * Test the string to timestamp conversion for 3GPP file names.
     * 
     * @throws Exception the exception
     */
    @Test
    public void testGetTimeStampFromFile() throws Exception {
        URL url = UrlFactory.getUrl("sftp.3gpp://admin:admin@192.168.1.1/opt/3gpp?step=300&neId=MME00001&deleteFile=true");
        URLConnection conn = url.openConnection();
        Assert.assertTrue(conn instanceof Sftp3gppUrlConnection);
        Sftp3gppUrlConnection c = (Sftp3gppUrlConnection) conn;
        Assert.assertTrue(Boolean.parseBoolean(c.getQueryMap().get("deletefile")));
        long t1 = c.getTimeStampFromFile("A20111102.1300-0500-1305-0500_MME00001");
        long t2 = c.getTimeStampFromFile("A20111102.1305-0500-1310-0500_MME00001");
        Assert.assertTrue(t2 > t1);
        Assert.assertTrue(t2 - t1 == Long.parseLong(c.getQueryMap().get("step")) * 1000);
    }

    /**
     * Log.
     *
     * @return the thread category
     */
    private ThreadCategory log() {
        return ThreadCategory.getInstance(getClass());
    }
}
