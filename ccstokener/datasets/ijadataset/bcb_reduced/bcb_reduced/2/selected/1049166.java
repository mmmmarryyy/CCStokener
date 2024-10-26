package org.apache.commons.vfs.abstractclasses;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;
import org.ietf.jgss.GSSCredential;
import org.apache.commons.vfs.provider.gridftp.cogjglobus.GridFtpFileSystem;
import org.apache.commons.vfs.provider.gridftp.cogjglobus.GridFtpFileSystemConfigBuilder;
import org.apache.commons.vfs.helper.VfsTestHelper;
import org.apache.commons.vfs.provider.storageresourcebroker.SRBVfsFileSystem;
import org.apache.commons.vfs.provider.storageresourcebroker.SRBFileSystemConfigBuilder;
import org.apache.commons.vfs.provider.irods.IRODSFileSystemConfigBuilder;
import org.apache.commons.vfs.provider.irods.IRODSVfsFileSystem;
import uk.ac.dl.escience.vfs.util.SshTrustAllHostsUserInfo;
import uk.ac.dl.escience.vfs.util.VFSUtil;
import static org.junit.Assert.*;

/**
 * @author David Meredith
 * 
 */
public abstract class AbstractTestClass {

    public VfsTestHelper vfsTestHelp = new VfsTestHelper();

    public VFSUtil vfs = new VFSUtil();

    public String proxyCertificatePath;

    public GSSCredential cred;

    public String dummyFileDirName;

    public boolean assertContentInWriteTests = false;

    public Integer idleTimeTestDelay = 300;

    public Integer gridftpTimeoutMilliSecs;

    public String gridftpHost1;

    public Integer gridftpPort1;

    public String gridftpHost2;

    public Integer gridftpPort2;

    public String srbGsiHost;

    public Integer srbGsiPort;

    public Integer srbGsiPortMax;

    public Integer srbGsiPortMin;

    public String srbGsiDefaultResource;

    public String srbEncryptHost;

    public Integer srbEncryptPort;

    public Integer srbEncryptPortMax;

    public Integer srbEncryptPortMin;

    public String srbEncryptDefaultResource;

    public String srbEncryptHomeDirectory;

    public String srbEncryptMdasDomainName;

    public String srbEncryptMcatZone;

    public String srbEncryptUsername;

    public String srbEncryptPassword;

    public String httpUri;

    public String httpProxy;

    public Integer httpPort;

    public String ftpUri;

    public String sftpHost;

    public Integer sftpPort = 22;

    public String sftpPath;

    public String sftpUsername;

    public String sftpPassword;

    public Integer sftpTimeoutMilliSecs;

    public String fileUri;

    public Boolean useSrbGsiInFsCopyTest;

    public Boolean useSrbEncryptInFsCopyTest;

    public Boolean useGridftpHost1InFsCopyTest;

    public Boolean useGridftpHost2InFsCopyTest;

    public Boolean useSftpInFsCopyTest;

    public Boolean useLocalFileInFsCopyTest;

    public Boolean useIrodsGsiCopyTest;

    public Boolean useIrodsEncryptCopyTest;

    public String irodsEncryptHost;

    public Integer irodsEncryptPort;

    public Integer irodsEncryptPortMin;

    public Integer irodsEncryptPortMax;

    public String irodsEncryptResource;

    public String irodsEncryptHomeDirectory;

    public String irodsEncryptZone;

    public String irodsEncryptUsername;

    public String irodsEncryptPassword;

    public String irodsGsiHost;

    public Integer irodsGsiPort;

    public String irodsGsiZone;

    public Integer srbQueryTimeout;

    /**
     * load up the properties file used for testing 
     */
    public void loadProperties() {
        try {
            java.util.Properties props = new java.util.Properties();
            java.net.URL url = ClassLoader.getSystemResource("env.properties");
            props.load(url.openStream());
            this.proxyCertificatePath = props.getProperty("proxy.certificate.path");
            this.dummyFileDirName = props.getProperty("delete.dummyFileDirName");
            this.idleTimeTestDelay = new Integer(props.getProperty("idleTimeTestDelaySeconds"));
            if (props.getProperty("gridftp.timeoutMilliSecs") != null) {
                this.gridftpTimeoutMilliSecs = new Integer(props.getProperty("gridftp.timeoutMilliSecs").trim());
            }
            this.assertContentInWriteTests = new Boolean(props.getProperty("assertContentInWriteTests"));
            this.gridftpHost1 = props.getProperty("gridftp.host1");
            this.gridftpPort1 = new Integer(props.getProperty("gridftp.port1"));
            this.gridftpHost2 = props.getProperty("gridftp.host2");
            this.gridftpPort2 = new Integer(props.getProperty("gridftp.port2"));
            this.srbGsiHost = props.getProperty("srb.gsi.host");
            this.srbGsiPort = new Integer(props.getProperty("srb.gsi.port"));
            this.srbGsiPortMin = new Integer(props.getProperty("srb.gsi.port.min"));
            this.srbGsiPortMax = new Integer(props.getProperty("srb.gsi.port.max"));
            this.srbGsiDefaultResource = props.getProperty("srb.gsi.defaultResource");
            this.srbEncryptHost = props.getProperty("srb.encrypt.host");
            this.srbEncryptPort = new Integer(props.getProperty("srb.encrypt.port"));
            this.srbEncryptPortMin = new Integer(props.getProperty("srb.encrypt.port.min"));
            this.srbEncryptPortMax = new Integer(props.getProperty("srb.encrypt.port.max"));
            this.srbEncryptDefaultResource = props.getProperty("srb.encrypt.defaultResource");
            this.srbEncryptHomeDirectory = props.getProperty("srb.encrypt.homeDirectory");
            this.srbEncryptMcatZone = props.getProperty("srb.encrypt.mcatZone");
            this.srbEncryptMdasDomainName = props.getProperty("srb.encrypt.mdasDomainName");
            this.srbEncryptUsername = props.getProperty("srb.encrypt.username");
            this.srbEncryptPassword = props.getProperty("srb.encrypt.password");
            this.sftpHost = props.getProperty("sftp.host");
            this.sftpPort = new Integer(props.getProperty("sftp.port"));
            this.sftpPath = props.getProperty("sftp.path");
            this.sftpUsername = props.getProperty("sftp.username");
            this.sftpPassword = props.getProperty("sftp.password");
            if (props.getProperty("sftp.timeoutMilliSecs") != null) {
                this.sftpTimeoutMilliSecs = new Integer(props.getProperty("sftp.timeoutMilliSecs").trim());
            }
            irodsEncryptHost = props.getProperty("irods.encrypt.host");
            irodsEncryptPort = new Integer(props.getProperty("irods.encrypt.port"));
            irodsEncryptResource = props.getProperty("irods.encrypt.defaultResource");
            irodsEncryptHomeDirectory = props.getProperty("irods.encrypt.homeDirectory");
            irodsEncryptZone = props.getProperty("irods.encrypt.zone");
            irodsEncryptUsername = props.getProperty("irods.encrypt.username");
            irodsEncryptPassword = props.getProperty("irods.encrypt.password");
            irodsGsiHost = props.getProperty("irods.gsi.host");
            irodsGsiPort = new Integer(props.getProperty("irods.gsi.port"));
            irodsGsiZone = props.getProperty("irods.gsi.zone");
            srbQueryTimeout = new Integer(props.getProperty("srb.query.timeout"));
            this.ftpUri = props.getProperty("ftp.uri");
            this.httpUri = props.getProperty("http.uri");
            this.httpProxy = props.getProperty("http.proxy");
            this.httpPort = new Integer(props.getProperty("http.port"));
            this.fileUri = props.getProperty("file.uri");
            java.net.URI tempUri = new java.net.URI(this.fileUri);
            File f = new File(tempUri);
            if (!f.exists()) {
                String temp = System.getProperty("java.io.tmpdir");
                System.out.println("Cannot list [" + fileUri + "] listing java.io.tmpdir instead [" + temp + "]");
                this.fileUri = temp;
            }
            useSrbGsiInFsCopyTest = new Boolean(props.getProperty("srb.gsi.use.in.fs.copy.test"));
            useSrbEncryptInFsCopyTest = new Boolean(props.getProperty("srb.encrypt.use.in.fs.copy.test"));
            useGridftpHost1InFsCopyTest = new Boolean(props.getProperty("gridftp.host1.use.in.fs.copy.test"));
            useGridftpHost2InFsCopyTest = new Boolean(props.getProperty("gridftp.host2.use.in.fs.copy.test"));
            useSftpInFsCopyTest = new Boolean(props.getProperty("sftp.use.in.fs.copy.test"));
            useLocalFileInFsCopyTest = new Boolean(props.getProperty("file.use.in.fs.copy.test"));
            useIrodsGsiCopyTest = new Boolean(props.getProperty("irods.gsi.use.in.fs.copy.test"));
            useIrodsEncryptCopyTest = new Boolean(props.getProperty("irods.encrypt.use.in.fs.copy.test"));
            assertNotNull(this.proxyCertificatePath);
            assertNotNull(this.dummyFileDirName);
            assertNotNull(this.idleTimeTestDelay);
            assertNotNull(this.ftpUri);
            assertNotNull(this.httpUri);
        } catch (Exception ex) {
            Logger.getLogger(AbstractTestClass.class.getName()).log(Level.SEVERE, null, ex);
            fail("Unable to locate and load 'testsettings.properties' file in source " + ex);
        }
    }

    public void loadCertificate() {
        try {
            System.out.println("loading certificate....");
            cred = VFSUtil.loadProxy(this.proxyCertificatePath);
            System.out.println("cred: " + cred.getName());
            if (cred.getRemainingLifetime() <= 0) {
                fail("Proxy has no lifetime for tests");
            }
        } catch (Exception ex) {
            Logger.getLogger(AbstractTestClass.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error loading proxy certificate " + ex);
        }
    }

    /**
     * Create FileSystemOptions with this classes instance variabls as options, 
     * e.g. this.cred, this.srbGsiPortMin, this.srbGsiPortMax, this.srbGsiDefaultResource
     * 
     * @return FileSystemOptions
     * @throws org.apache.commons.vfs.FileSystemException 
     */
    public FileSystemOptions createFileSystemOptions() throws FileSystemException {
        assertNotNull(this.cred);
        assertNotNull(this.srbGsiPortMin);
        assertNotNull(this.srbGsiPortMax);
        assertNotNull(this.srbGsiDefaultResource);
        FileSystemOptions options1 = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options1, "no");
        GridFtpFileSystemConfigBuilder.getInstance().setGSSCredential(options1, cred);
        if (this.gridftpTimeoutMilliSecs != null) {
            GridFtpFileSystemConfigBuilder.getInstance().setTimeout(options1, this.gridftpTimeoutMilliSecs);
        }
        SRBFileSystemConfigBuilder.getInstance().setGSSCredential(options1, cred);
        SRBFileSystemConfigBuilder.getInstance().setFileWallPortMin(options1, srbGsiPortMin);
        SRBFileSystemConfigBuilder.getInstance().setFileWallPortMax(options1, srbGsiPortMax);
        SRBFileSystemConfigBuilder.getInstance().setDefaultStorageResource(options1, srbGsiDefaultResource);
        return options1;
    }

    protected DefaultFileSystemManager getFsManager() throws FileSystemException {
        return VFSUtil.createNewFsManager(true, true, true, true, true, true, true, null);
    }

    protected FileObject setupSrbFileObjectEncrypt1Auth(DefaultFileSystemManager fsManager) throws Exception {
        assertNotNull(this.srbEncryptHost);
        assertNotNull(this.srbEncryptPort);
        assertNotNull(this.srbEncryptHomeDirectory);
        assertNotNull(this.srbEncryptMdasDomainName);
        assertNotNull(this.srbEncryptMcatZone);
        assertNotNull(this.srbEncryptDefaultResource);
        assertNotNull(this.srbEncryptUsername);
        assertNotNull(this.srbEncryptUsername);
        String uri = "srb://" + this.srbEncryptHost + ":" + this.srbEncryptPort;
        FileSystemOptions opts = new FileSystemOptions();
        SRBFileSystemConfigBuilder.getInstance().setHomeDirectory(opts, this.srbEncryptHomeDirectory);
        SRBFileSystemConfigBuilder.getInstance().setMdasDomainName(opts, this.srbEncryptMdasDomainName);
        SRBFileSystemConfigBuilder.getInstance().setMcatZone(opts, this.srbEncryptMcatZone);
        SRBFileSystemConfigBuilder.getInstance().setDefaultStorageResource(opts, this.srbEncryptDefaultResource);
        StaticUserAuthenticator auth = new StaticUserAuthenticator(null, this.srbEncryptUsername, this.srbEncryptPassword);
        SRBFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        FileObject fo1 = fsManager.resolveFile(uri, opts);
        String homeDir = (String) fo1.getFileSystem().getAttribute(SRBVfsFileSystem.HOME_DIRECTORY);
        System.out.println("HOME_DIRECTORY: " + homeDir);
        FileObject relativeToFO_ = fsManager.resolveFile(uri + homeDir, opts);
        assertEquals(FileType.FOLDER, relativeToFO_.getType());
        return relativeToFO_;
    }

    protected FileObject setupSrbFileObjectGsiAuth(DefaultFileSystemManager fsManager) throws Exception {
        assertNotNull(this.srbGsiHost);
        assertNotNull(this.srbGsiPort);
        String srbGsiUri = "srb://" + srbGsiHost + ":" + srbGsiPort;
        FileSystemOptions opts = this.createFileSystemOptions();
        SRBFileSystemConfigBuilder.getInstance().setQueryTimeout(opts, this.srbQueryTimeout);
        FileObject fo1 = fsManager.resolveFile(srbGsiUri, opts);
        String homeDir = (String) fo1.getFileSystem().getAttribute(SRBVfsFileSystem.HOME_DIRECTORY);
        System.out.println("HOME_DIRECTORY: " + homeDir);
        FileObject relativeToFO_ = fsManager.resolveFile(srbGsiUri + homeDir, opts);
        assertEquals(FileType.FOLDER, relativeToFO_.getType());
        return relativeToFO_;
    }

    protected FileObject setupGridFtpFileObjectGsiAuth1(DefaultFileSystemManager fsManager) throws Exception {
        assertNotNull(this.gridftpHost1);
        assertNotNull(this.gridftpPort1);
        System.out.println("GSIFTPHost1: " + gridftpHost1 + " port: " + gridftpPort1);
        String gridftpUri = "gsiftp://" + gridftpHost1 + ":" + gridftpPort1;
        return this.setupGridFtpFileObjectGsiAuth(fsManager, gridftpUri);
    }

    protected FileObject setupGridFtpFileObjectGsiAuth2(DefaultFileSystemManager fsManager) throws Exception {
        assertNotNull(this.gridftpHost2);
        assertNotNull(this.gridftpPort2);
        String gridftpUri = "gsiftp://" + gridftpHost2 + ":" + gridftpPort2;
        return this.setupGridFtpFileObjectGsiAuth(fsManager, gridftpUri);
    }

    private FileObject setupGridFtpFileObjectGsiAuth(DefaultFileSystemManager fsManager, String gridftpUri) throws Exception {
        FileSystemOptions opts = this.createFileSystemOptions();
        FileObject fo1 = fsManager.resolveFile(gridftpUri, opts);
        String homeDir = (String) fo1.getFileSystem().getAttribute(GridFtpFileSystem.HOME_DIRECTORY);
        System.out.println("HOME_DIRECTORY: " + homeDir);
        FileObject relativeToFO_ = fsManager.resolveFile(gridftpUri + homeDir, opts);
        assertEquals(FileType.FOLDER, relativeToFO_.getType());
        return relativeToFO_;
    }

    protected FileObject setupSftpFileObject(DefaultFileSystemManager fsManager) throws FileSystemException {
        assertNotNull(this.sftpHost);
        assertNotNull(this.sftpPort);
        assertNotNull(this.sftpPassword);
        assertNotNull(this.sftpUsername);
        String sftpUri = "sftp://" + this.sftpHost + ":" + this.sftpPort;
        if (this.sftpPath != null) {
            sftpUri += this.sftpPath;
        }
        SshTrustAllHostsUserInfo ui = new SshTrustAllHostsUserInfo();
        ui.setPassword(this.sftpPassword);
        ui.setPassphrase(this.sftpPassword);
        FileSystemOptions defaultOpts = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance().setUserInfo(defaultOpts, ui);
        if (this.sftpTimeoutMilliSecs != null) {
            SftpFileSystemConfigBuilder.getInstance().setTimeout(defaultOpts, this.sftpTimeoutMilliSecs);
        }
        StaticUserAuthenticator auth = new StaticUserAuthenticator(null, this.sftpUsername, this.sftpPassword);
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(defaultOpts, auth);
        FileObject relativeToFO_ = fsManager.resolveFile(sftpUri, defaultOpts);
        return relativeToFO_;
    }

    protected FileObject setupIrodsEncryptFileObject(DefaultFileSystemManager fsManager) throws FileSystemException {
        assertNotNull(this.irodsEncryptHost);
        assertNotNull(this.irodsEncryptPort);
        assertNotNull(this.irodsEncryptResource);
        assertNotNull(this.irodsEncryptZone);
        assertNotNull(this.irodsEncryptHomeDirectory);
        assertNotNull(this.irodsEncryptUsername);
        assertNotNull(this.irodsEncryptPassword);
        String uri = "irods://" + this.irodsEncryptHost + ":" + this.irodsEncryptPort;
        System.out.println("connection uri: " + uri);
        FileSystemOptions opts = new FileSystemOptions();
        IRODSFileSystemConfigBuilder.getInstance().setHomeDirectory(opts, this.irodsEncryptHomeDirectory);
        IRODSFileSystemConfigBuilder.getInstance().setZone(opts, this.irodsEncryptZone);
        IRODSFileSystemConfigBuilder.getInstance().setDefaultStorageResource(opts, this.irodsEncryptResource);
        StaticUserAuthenticator auth = new StaticUserAuthenticator(null, this.irodsEncryptUsername, this.irodsEncryptPassword);
        IRODSFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        FileObject fo1 = fsManager.resolveFile(uri, opts);
        String homeDir = (String) fo1.getFileSystem().getAttribute(IRODSVfsFileSystem.HOME_DIRECTORY);
        System.out.println("HOME_DIRECTORY: " + homeDir);
        FileObject relativeToFO_ = fsManager.resolveFile(uri + homeDir, opts);
        assertEquals(FileType.FOLDER, relativeToFO_.getType());
        return relativeToFO_;
    }

    protected FileObject setupIrodsGsiFileObject(DefaultFileSystemManager fsManager) throws FileSystemException {
        assertNotNull(this.irodsGsiHost);
        assertNotNull(this.irodsGsiPort);
        assertNotNull(this.irodsGsiZone);
        String uri = "irods://" + this.irodsGsiHost + ":" + this.irodsGsiPort;
        FileSystemOptions opts = new FileSystemOptions();
        IRODSFileSystemConfigBuilder.getInstance().setZone(opts, this.irodsGsiZone);
        IRODSFileSystemConfigBuilder.getInstance().setGSSCredential(opts, cred);
        FileObject fo1 = fsManager.resolveFile(uri, opts);
        String homeDir = (String) fo1.getFileSystem().getAttribute(IRODSVfsFileSystem.HOME_DIRECTORY);
        System.out.println("HOME_DIRECTORY: " + homeDir);
        FileObject relativeToFO_ = fsManager.resolveFile(uri + homeDir, opts);
        assertEquals(FileType.FOLDER, relativeToFO_.getType());
        return relativeToFO_;
    }

    /**
     * Force the SoftRefFilesChache to free all files
     * 
     * @param relativeToFO
     * @param fsManager
     * @throws java.lang.InterruptedException
     */
    protected void cleanUp(FileObject relativeToFO, DefaultFileSystemManager fsManager) throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            System.err.println(".");
            System.gc();
            Thread.sleep(1000);
        }
        try {
            if (fsManager != null) {
                fsManager.freeUnusedResources();
            }
        } finally {
            if (fsManager != null) {
                fsManager.close();
            }
        }
    }

    /**
     * Delay for this.idleTimeTestDelay. Used for testing automatic reconnects. 
     * 
     * @throws java.lang.InterruptedException
     */
    protected void delayForIdleTime() throws InterruptedException {
        System.out.println("Sleeping for given idle time");
        if (this.idleTimeTestDelay == null) {
            this.idleTimeTestDelay = 100;
        }
        for (int i = 0; i < this.idleTimeTestDelay; i++) {
            System.err.println("wait: " + i + "/" + idleTimeTestDelay);
            Thread.sleep(1000);
        }
    }
}
