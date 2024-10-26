package ch.jester.common.reportengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.osgi.framework.Bundle;
import ch.jester.commonservices.api.reportengine.IBundleReport;

/**
 * Defaultimplementierung für einen Report der vom FileSystem importiert wird.
 *
 */
public class DefaultFSReport extends DefaultReport implements IBundleReport {

    private String mBundleSourceRoot;

    private Bundle mBundle;

    private String mBundleFile;

    @Override
    public String getBundleReportFile() {
        return mBundleFile;
    }

    @Override
    public void setBundleReportFile(String pFilePath) {
        mBundleFile = pFilePath;
    }

    @Override
    public void setBundle(Bundle b) {
        mBundle = b;
    }

    @Override
    public InputStream getBundleFileAsStream() throws IOException {
        URL url = mBundle.getResource(mBundleFile);
        return url.openStream();
    }

    @Override
    public void setBundleSourceRoot(String pRoot) {
        mBundleSourceRoot = pRoot;
    }

    @Override
    public String getBundleSourceRoot() {
        return mBundleSourceRoot;
    }

    @Override
    public Bundle getBundle() {
        return mBundle;
    }
}
