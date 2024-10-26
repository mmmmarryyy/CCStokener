package architecture.ee.spring.resources.scanner.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import architecture.common.scanner.DirectoryListener;
import architecture.common.scanner.URLDirectoryScanner;
import architecture.common.util.vfs.VFSUtils;
import architecture.ee.component.admin.AdminHelper;
import architecture.ee.component.admin.AdminSqlQueryFactory;
import architecture.ee.spring.resources.scanner.DirectoryScanner;
import architecture.ee.util.ApplicationConstants;

/**
 * @author  donghyuck
 */
public class DirectoryScannerImpl implements InitializingBean, DisposableBean, DirectoryScanner {

    /**
	 */
    private URLDirectoryScanner scanner;

    /**
	 */
    private List<String> resourceLocations;

    private boolean fastDeploy = false;

    private Log log = LogFactory.getLog(getClass());

    public DirectoryScannerImpl() {
        this.resourceLocations = Collections.emptyList();
        try {
            this.scanner = createURLDirectoryScanner(true);
        } catch (Exception e) {
        }
    }

    public void setDirectoryListenerList(List<DirectoryListener> directoryListeners) {
        for (DirectoryListener listener : (List<DirectoryListener>) directoryListeners) {
            scanner.removeDirectoryListener(listener);
            scanner.addDirectoryListener(listener);
        }
    }

    private URLDirectoryScanner createURLDirectoryScanner(boolean recursive) throws Exception {
        URLDirectoryScanner scanner = new URLDirectoryScanner();
        scanner.setRecursiveSearch(true);
        scanner.setScanEnabled(true);
        scanner.create();
        return scanner;
    }

    /**
	 * @param fastDeploy
	 */
    public void setFastDeploy(boolean fastDeploy) {
        this.fastDeploy = fastDeploy;
    }

    /**
	 * @return
	 */
    public List<String> getResourceLocations() {
        return resourceLocations;
    }

    /**
	 * @param resourceLocations
	 */
    public void setResourceLocations(List<String> resourceLocations) {
        this.resourceLocations = resourceLocations;
    }

    public void setRecursiveSearch(boolean recurse) {
        scanner.setRecursiveSearch(recurse);
    }

    public void setPollIntervalMillis(int pollIntervalMillis) {
        scanner.setPollIntervalMillis(pollIntervalMillis);
    }

    public void addDirectoryListener(DirectoryListener fileListener) {
        scanner.addDirectoryListener(fileListener);
    }

    public void addScanDir(String path) {
        scanner.addScanDir(path);
    }

    public DirectoryListener[] getDirectoryListeners() {
        return scanner.getDirectoryListeners();
    }

    public void removeDirectoryListener(DirectoryListener fileListener) {
        scanner.removeDirectoryListener(fileListener);
    }

    public void removeScanURL(URL url) {
        scanner.removeScanURL(url);
    }

    public void addScanURI(URI uri) {
        try {
            scanner.addScanURL(uri.toURL());
        } catch (MalformedURLException e) {
        }
    }

    public void addScanURL(URL url) {
        scanner.addScanURL(url);
    }

    public void removeScanURI(URI uri) {
        try {
            scanner.removeScanURL(uri.toURL());
        } catch (MalformedURLException e) {
        }
    }

    public void destroy() throws Exception {
        if (scanner.isStarted()) scanner.doStop();
        scanner.destroy();
    }

    public void afterPropertiesSet() throws Exception {
        loadResourceLocations();
        if (!scanner.isStarted()) scanner.start();
    }

    protected void loadResourceLocations() {
        try {
            for (String path : resourceLocations) {
                if (path.startsWith("${") && path.endsWith("}")) {
                    int start = path.indexOf('{') + 1;
                    int end = path.indexOf('}');
                    String key = path.substring(start, end).trim();
                    if (key.equals(ApplicationConstants.RESOURCE_SQL_LOCATION_PROP_NAME)) path = AdminHelper.getRepository().getURI("sql"); else path = AdminHelper.getRepository().getSetupApplicationProperties().get(key);
                    log.debug(key + "=" + path);
                }
                FileObject fo = VFSUtils.resolveFile(path);
                if (fo.exists()) {
                    URL url = fo.getURL();
                    url.openConnection();
                    if (fastDeploy) {
                        if (log.isDebugEnabled()) {
                            log.debug("Fast deploy : " + url);
                            AdminSqlQueryFactory builder = null;
                            for (DirectoryListener listener : scanner.getDirectoryListeners()) {
                                if (listener instanceof AdminSqlQueryFactory) {
                                    builder = (AdminSqlQueryFactory) listener;
                                }
                            }
                            File file = new File(url.getFile());
                            fastDeploy(file, builder);
                        }
                    }
                    scanner.addScanURL(url);
                }
            }
        } catch (Exception e) {
        }
    }

    public void fastDeploy(File file, AdminSqlQueryFactory builder) {
        if (file.isFile()) {
            if (builder.validateFile(file)) {
                builder.fileCreated(file);
            }
        } else {
            for (File c : file.listFiles()) {
                fastDeploy(c, builder);
            }
        }
    }
}
