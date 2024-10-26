package net.sourceforge.ondex.server.plugins.utils;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sourceforge.ondex.server.exceptions.WSImportException;
import net.sourceforge.ondex.wsapi.plugins.ZipFormat;
import org.apache.log4j.Logger;

/**
 *
 * @author christian
 */
public class Unpacker {

    private File tempDir;

    private static final Logger logger = Logger.getLogger(Unpacker.class);

    private static final int CONNECTION_RETRIES = 10;

    public Unpacker(File rootDir) {
        tempDir = rootDir;
    }

    public Unpacker() {
        if (System.getProperty("webapp.root") == null) {
            tempDir = new File("/Ondex/tempData/");
        } else {
            tempDir = new File(System.getProperty("webapp.root") + File.separator + "temp");
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    public File toDirectory(String name, File root) throws WSImportException {
        File exists = new File(name);
        if (exists.isDirectory()) {
            return exists;
        }
        if (exists.isFile()) {
            ZipFormat zipFormat = ZipFormat.parseString(name);
            if (zipFormat.isDirectory()) {
                try {
                    InputStream fis = new FileInputStream(exists);
                    File parent = createTempDirectory(root);
                    toDirectory(fis, zipFormat, parent);
                    return parent;
                } catch (FileNotFoundException ex) {
                    throw new WSImportException("Error finding " + exists.getAbsolutePath(), ex, logger);
                } catch (IOException ex) {
                    throw new WSImportException("Error extracting " + exists.getAbsolutePath(), ex, logger);
                }
            }
            throw new WSImportException("File " + name + " is a file not a directory or known zip format.", logger);
        }
        if (name.toLowerCase().startsWith("ftp")) {
            return fromFtpDirectory(name, root);
        }
        try {
            ZipFormat zipFormat = ZipFormat.parseString(name);
            if (zipFormat.isDirectory()) {
                URL url = new URL(name);
                URLConnection connection = url.openConnection();
                long date = connection.getDate();
                InputStream uis = url.openStream();
                File parent = createTempDirectory(root);
                toDirectory(uis, zipFormat, parent);
                return parent;
            }
            throw new IOException("Url " + name + " does not appear to point to known directory zip format.");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new WSImportException("The arguement " + name + " of type String, could not be converted " + "to either a URL or an existing file.", logger);
        }
    }

    public void toDirectory(InputStream input, ZipFormat zipFormat, File root) throws WSImportException {
        switch(zipFormat) {
            case ZIP:
                unzip(input, root);
                break;
            case TAR:
                untar(input, root);
                break;
            case TAR_GZIP:
                try {
                    InputStream ungz = new GZIPInputStream(input);
                    untar(ungz, root);
                } catch (IOException ex) {
                    throw new WSImportException("Error creating GZIP stream. ", ex, logger);
                }
                break;
            default:
                throw new WSImportException("Unexpected zipFormat in toDirectory", logger);
        }
    }

    private void unzip(InputStream input, File root) throws WSImportException {
        final int BUFFER = 2048;
        BufferedOutputStream dest = null;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));
        ZipEntry entry;
        String rootPath = root.getAbsolutePath() + File.separator;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("Extracting: " + entry);
                int count;
                byte[] data = new byte[BUFFER];
                String tempName = rootPath + entry.getName();
                if (tempName.endsWith("/")) {
                    File output = new File(tempName);
                    System.out.println("new directory " + output.getAbsoluteFile());
                    output.mkdirs();
                } else {
                    System.out.println("ouput file is " + tempName);
                    FileOutputStream fos = new FileOutputStream(tempName);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                }
            }
            zis.close();
        } catch (IOException ex) {
            throw new WSImportException("Error unzipping ", ex, logger);
        }
    }

    private void untar(InputStream input, File root) throws WSImportException {
        final int BUFFER = 2048;
        BufferedOutputStream dest = null;
        TarInputStream tis = new TarInputStream(new BufferedInputStream(input));
        TarEntry entry;
        String rootPath = root.getAbsolutePath() + File.separator;
        try {
            while ((entry = tis.getNextEntry()) != null) {
                System.out.println("Extracting: " + entry);
                int count;
                byte[] data = new byte[BUFFER];
                String tempName = rootPath + entry.getName();
                if (tempName.endsWith("/")) {
                    File output = new File(tempName);
                    System.out.println("new directory " + output.getAbsoluteFile());
                    output.mkdirs();
                } else {
                    System.out.println("ouput file is " + tempName);
                    FileOutputStream fos = new FileOutputStream(tempName);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = tis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                }
                tis.close();
            }
        } catch (IOException ex) {
            throw new WSImportException("Error untaring ", ex, logger);
        }
    }

    public File createTempDirectory(File parent) throws IOException {
        final File temp;
        temp = File.createTempFile("temp", "", parent);
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    private void disconnect(FileTransferClient ftp) {
        ftp.getRemoteHost();
        try {
            ftp.disconnect();
        } catch (Exception ex) {
            logger.error("Error disconnecting from FTP host " + ftp.getRemoteHost());
            logger.error(ex.toString());
            StackTraceElement[] trace = ex.getStackTrace();
            for (StackTraceElement element : trace) {
                logger.error(element.toString());
            }
        }
    }

    private File fromFtpDirectory(String name, File root) throws WSImportException {
        String localName = name.toLowerCase().replace('\\', '/');
        if (localName.startsWith("ftp://")) {
            localName = localName.substring(6, localName.length());
        }
        int hostCutOff = localName.indexOf('/');
        int nameCutOff = localName.lastIndexOf('/');
        String host;
        host = localName.substring(0, hostCutOff);
        String directory;
        directory = localName.substring(hostCutOff, nameCutOff);
        String fileName;
        fileName = localName.substring(nameCutOff + 1, localName.length());
        System.out.println(host);
        System.out.println(directory);
        System.out.println(fileName);
        File target = new File(root, host + directory);
        if (target.isFile()) {
            throw new WSImportException("Unable to create target directory for " + name, logger);
        }
        target.mkdirs();
        String username = "anonymous";
        String password = "";
        FileTransferClient ftp = new FileTransferClient();
        try {
            ftp.setRemoteHost(host);
            ftp.setUserName(username);
            ftp.setPassword(password);
            ftp.connect();
            ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
            ftp.setContentType(FTPTransferType.BINARY);
        } catch (FTPException ex) {
            throw new WSImportException("Error connecting to " + host, ex, logger);
        } catch (IOException ex) {
            throw new WSImportException("Error connecting to " + host, ex, logger);
        }
        changeDirectory(ftp, directory);
        FTPFile[] files = getDirectoryList(ftp);
        FTPFile file = null;
        for (int i = 0; i < files.length; i++) {
            System.out.println(files[i].getName());
            if (files[i].getName().equalsIgnoreCase(fileName)) {
                file = files[i];
            }
        }
        if (file == null) {
            disconnect(ftp);
            throw new WSImportException("Unable to find " + name + " in " + host + directory, logger);
        }
        fromFptFile(ftp, file, target);
        disconnect(ftp);
        return target;
    }

    private void changeDirectory(FileTransferClient ftp, String directory) throws WSImportException {
        FTPFile[] files;
        try {
            ftp.changeDirectory(directory);
            files = ftp.directoryList();
        } catch (ParseException ex) {
            throw new WSImportException("Error parsing ftp directory " + directory, ex, logger);
        } catch (FTPException ex) {
            throw new WSImportException("Error finding ftp directory listing for " + directory, ex, logger);
        } catch (IOException ex) {
            throw new WSImportException("Error finding ftp directory listing for " + directory, ex, logger);
        }
    }

    private FTPFile[] getDirectoryList(FileTransferClient ftp) throws WSImportException {
        int count = 0;
        while (true) {
            try {
                return ftp.directoryList();
            } catch (ConnectException ex) {
                count++;
                if (count > CONNECTION_RETRIES) {
                    throw new WSImportException("Failed to connect to " + ftp.getRemoteHost() + " even after " + CONNECTION_RETRIES + " retries ", ex, logger);
                } else {
                    logger.error("Failed to connect to " + ftp.getRemoteHost() + " trying.");
                }
            } catch (ParseException ex) {
                throw new WSImportException("Error parsing ftp directory ", ex, logger);
            } catch (FTPException ex) {
                throw new WSImportException("Error finding ftp directory ", ex, logger);
            } catch (IOException ex) {
                throw new WSImportException("Error finding ftp directory listing ", ex, logger);
            }
        }
    }

    private void downloadFile(FileTransferClient ftp, String target, String source) throws WSImportException {
        int count = 0;
        while (true) {
            try {
                ftp.downloadFile(target, source);
                return;
            } catch (ConnectException ex) {
                count++;
                if (count > CONNECTION_RETRIES) {
                    throw new WSImportException("Failed to connect to " + ftp.getRemoteHost() + " even after " + CONNECTION_RETRIES + " retries ", ex, logger);
                } else {
                    logger.error("Failed to connect to " + ftp.getRemoteHost() + " trying.");
                }
            } catch (FTPException ex) {
                throw new WSImportException("Error downloading " + source, ex, logger);
            } catch (IOException ex) {
                throw new WSImportException("Error downloading " + source, ex, logger);
            }
        }
    }

    private void fromFptFile(FileTransferClient ftp, FTPFile ftpFile, File root) throws WSImportException {
        if (ftpFile.isDir()) {
            System.out.println("Directory: " + ftpFile.getName());
            File directory = new File(root, ftpFile.getName());
            if (directory.isDirectory()) {
            } else {
                directory.mkdirs();
            }
            changeDirectory(ftp, ftpFile.getName());
            FTPFile[] files = getDirectoryList(ftp);
            for (int i = 0; i < files.length; i++) {
                fromFptFile(ftp, files[i], directory);
            }
            changeDirectory(ftp, "..");
        } else if (ftpFile.isFile()) {
            String fileName = ftpFile.getName().toLowerCase();
            ZipFormat zipFormat = ZipFormat.parseString(fileName);
            if (zipFormat.isDirectory()) {
                try {
                    InputStream inputStream = ftp.downloadStream(ftpFile.getName());
                    toDirectory(inputStream, zipFormat, root);
                } catch (FTPException ex) {
                    throw new WSImportException("Error getting import stream for " + ftpFile.toString(), ex, logger);
                } catch (IOException ex) {
                    throw new WSImportException("Error getting import stream for " + ftpFile.toString(), ex, logger);
                }
            } else {
                File target = new File(root, ftpFile.getName());
                if (target.exists()) {
                    if (ftpFile.lastModified().before(new Date(target.lastModified()))) {
                        System.out.println("Skipping " + ftpFile.getName());
                        return;
                    } else {
                        System.out.println("Overwriting " + ftpFile.getName());
                    }
                } else {
                    System.out.println(ftpFile.getPath() + "/" + ftpFile.getName() + " to " + target.getAbsolutePath());
                }
                downloadFile(ftp, target.getAbsolutePath(), fileName);
            }
        }
    }

    private void showFTPFile(FTPFile file) {
        System.out.println(file.getName());
        System.out.println("\t Created: " + file.created());
        System.out.println("\t isDir: " + file.isDir());
        System.out.println("\t isFile: " + file.isFile());
        System.out.println("\t isLink: " + file.isLink());
        System.out.println("\t lastModified: " + file.lastModified());
        System.out.println("\t size: " + file.size());
        System.out.println("\t group: " + file.getGroup());
        System.out.println("\t LinkedName: " + file.getLinkedName());
        System.out.println("\t Owner: " + file.getOwner());
        System.out.println("\t Path: " + file.getPath());
        System.out.println("\t Permissions: " + file.getPermissions());
        System.out.println("\t raw: " + file.getRaw());
        System.out.println("\t toString: " + file.toString());
    }

    public static void main(String[] args) throws Exception {
        Unpacker unpacker = new Unpacker();
        File test = unpacker.toDirectory("http://www.enterprisedt.com/products/edtftpj/download/edtftpj.zip", unpacker.tempDir);
        System.out.println(test.getAbsolutePath());
    }
}
