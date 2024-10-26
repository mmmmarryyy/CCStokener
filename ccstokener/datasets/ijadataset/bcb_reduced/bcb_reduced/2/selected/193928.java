package net.sourceforge.seqware.common.util.filetools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import net.sourceforge.seqware.common.util.configtools.ConfigTools;
import org.apache.commons.codec.binary.Base64;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class ProvisionFilesUtil {

    protected final int READ_ATTEMPTS = 1000;

    protected long size = 0;

    protected long position = 0;

    protected String fileName = "";

    protected String originalFileName = "";

    protected File inputFile = null;

    protected Key dataEncryptionKey = null;

    private boolean verbose;

    private AmazonS3Client s3;

    private static final String DATA_ENCRYPTION_ALGORITHM = "DESede";

    /**
   * Default ctor.
   */
    public ProvisionFilesUtil() {
    }

    ;

    /**
   * Set verbose mode ctor.
   * 
   * @param verbose
   */
    public ProvisionFilesUtil(boolean verbose) {
        this.setVerbose(true);
    }

    /**
   * Gets the file name. Available after the getSourceReader has been invoked.
   * 
   * @return String representation of the proceeded file name
   */
    public String getFileName() {
        return fileName;
    }

    /**
   * Creates symlink of input to output.
   * 
   * @param output
   * @param input
   * @return
   */
    public boolean createSymlink(String output, String input) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process result = null;
            String exe = new String("ln" + " -s " + input + " " + output + File.separator + fileName);
            result = rt.exec(exe);
            try {
                if (result.exitValue() != 0) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
   * Gets cipher by DecryptKey.
   * 
   * @param decryptKey
   * @return Cipher object
   */
    public Cipher getCipher(String decryptKey) {
        Cipher cipher = null;
        setDataEncryptionKeyString(decryptKey);
        try {
            cipher = createDecryptCipherInternal();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            cipher = null;
        }
        return cipher;
    }

    /**
   * Copy reader into output.
   * 
   * @param reader
   * @param output
   * @param bufLen
   * @param input
   * @return written File object
   */
    public File copyToFile(BufferedInputStream reader, String output, int bufLen, String input) {
        return copyToFile(reader, output, bufLen, input, null);
    }

    /**
   * Copy reader into output using Cipher.
   * 
   * @param reader
   * @param output
   * @param bufLen
   * @param input
   * @param cipher
   * @return written File object
   */
    public File copyToFile(BufferedInputStream reader, String output, int bufLen, String input, Cipher cipher) {
        BufferedOutputStream writer;
        File outputObj = new File(output + File.separator + fileName);
        outputObj.getParentFile().mkdirs();
        try {
            int attempts = 0;
            if (cipher != null) {
                writer = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(outputObj), cipher), bufLen);
            } else {
                writer = new BufferedOutputStream(new FileOutputStream(outputObj), bufLen);
            }
            while (true) {
                try {
                    int data = reader.read();
                    if (data == -1) {
                        break;
                    }
                    writer.write(data);
                    this.position++;
                } catch (IOException e) {
                    attempts++;
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    if (attempts > this.READ_ATTEMPTS) {
                        System.err.println("Giving up after " + attempts + " attempts!");
                        return null;
                    }
                    System.err.println("Trying to re-open the reader at position " + this.position);
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        System.err.println(e1.getMessage());
                        e1.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (java.lang.InterruptedException e2) {
                    }
                    reader = getSourceReader(input, bufLen, this.position);
                    if (reader == null) {
                        return null;
                    }
                }
            }
            reader.close();
            writer.close();
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return outputObj;
    }

    /**
   * Not supported yet.
   * 
   * @return
   */
    public boolean putToHttp() {
        return (false);
    }

    /**
   * Copy file using reader into output.
   * 
   * @param reader
   * @param output
   * @return true if OK
   */
    public boolean putToS3(BufferedInputStream reader, String output) {
        Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
        Matcher m = p.matcher(output);
        boolean result = m.find();
        String accessKey = null;
        String secretKey = null;
        String URL = output;
        if (result) {
            accessKey = m.group(1);
            secretKey = m.group(2);
            URL = "s3://" + m.group(3);
        } else {
            try {
                HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                accessKey = settings.get("AWS_ACCESS_KEY");
                secretKey = settings.get("AWS_SECRET_KEY");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        if (accessKey == null || secretKey == null) {
            return false;
        }
        p = Pattern.compile("s3://([^/]+)/(\\S+)/*");
        m = p.matcher(URL);
        result = m.find();
        if (result) {
            String bucket = m.group(1);
            String key = m.group(2);
            if (key.endsWith("/")) {
                key = key + fileName;
            } else {
                key = key + "/" + fileName;
            }
            ObjectMetadata omd = new ObjectMetadata();
            omd.setContentLength(this.size);
            TransferManager tm = new TransferManager(new BasicAWSCredentials(accessKey, secretKey));
            if (inputFile != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    System.err.println(e1.getMessage());
                    e1.printStackTrace();
                }
                Upload upload = tm.upload(bucket, key, inputFile);
                try {
                    upload.waitForCompletion();
                } catch (AmazonServiceException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (AmazonClientException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } else {
                Transfer myUpload = tm.upload(bucket, key, reader, omd);
                try {
                    while (myUpload.isDone() == false) {
                        if (isVerbose()) {
                            System.out.println("Transfer: " + myUpload.getDescription());
                            System.out.println("  - State:    " + myUpload.getState());
                            System.out.println("  - Progress: " + myUpload.getProgress().getBytesTransfered() + " of " + this.size);
                            if (myUpload.getProgress().getBytesTransfered() < this.size) {
                                System.out.println("  - InputStream bytes available: " + reader.available());
                            }
                        }
                        if (myUpload.getState() == TransferState.Failed) {
                            System.err.println("Failure Uploading: " + myUpload.getDescription());
                            tm.shutdownNow();
                            return false;
                        }
                        Thread.sleep(500);
                    }
                    tm.shutdownNow();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public URL getS3Url(String input) {
        URL url = null;
        if (input.startsWith("s3://")) {
            Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
            Matcher m = p.matcher(input);
            boolean result = m.find();
            String accessKey = null;
            String secretKey = null;
            String URL = input;
            if (result) {
                accessKey = m.group(1);
                secretKey = m.group(2);
                URL = "s3://" + m.group(3);
            } else {
                try {
                    HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                    accessKey = settings.get("AWS_ACCESS_KEY");
                    secretKey = settings.get("AWS_SECRET_KEY");
                } catch (Exception e) {
                    e.printStackTrace();
                    return (null);
                }
            }
            if (accessKey == null || secretKey == null) {
                return (null);
            }
            p = Pattern.compile("s3://([^/]+)/(\\S+)");
            m = p.matcher(URL);
            if (m.find()) {
                String bucket = m.group(1);
                String key = m.group(2);
                s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
                url = s3.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET).withExpiration(new Date(new Date().getTime() + 10000)));
            }
        }
        return url;
    }

    public URL getS3Url(String input, String accessKey, String secretKey) {
        URL url = null;
        Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
        Matcher m = p.matcher(input);
        boolean result = m.find();
        String URL = input;
        if (result) {
            accessKey = m.group(1);
            secretKey = m.group(2);
            URL = "s3://" + m.group(3);
        }
        p = Pattern.compile("s3://([^/]+)/(\\S+)");
        m = p.matcher(URL);
        if (m.find()) {
            String bucket = m.group(1);
            String key = m.group(2);
            s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
            url = s3.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET).withExpiration(new Date(new Date().getTime() + 10000)));
        }
        return url;
    }

    /**
   * This attempts to resume if passed in startPosition > 0.
   * 
   * @param input
   * @param bufLen
   * @param startPosition
   * @return reader of input file
   */
    public BufferedInputStream getSourceReader(String input, int bufLen, long startPosition) {
        this.originalFileName = input;
        BufferedInputStream reader = null;
        inputFile = null;
        if (input.startsWith("s3://")) {
            reader = getS3InputStream(input, bufLen, startPosition);
        } else if (input.startsWith("http://") || input.startsWith("https://")) {
            reader = getHttpInputStream(input, bufLen, startPosition);
        } else {
            reader = getFileInputStream(input, startPosition);
        }
        return reader;
    }

    private BufferedInputStream getFileInputStream(String input, long startPosition) {
        BufferedInputStream reader = null;
        try {
            inputFile = new File(input);
            String[] paths = inputFile.getAbsolutePath().split("/");
            fileName = paths[paths.length - 1];
            size = inputFile.length();
            reader = new BufferedInputStream(new FileInputStream(new File(input)));
            reader.skip(startPosition);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return (null);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return (null);
        }
        return reader;
    }

    public BufferedInputStream getHttpInputStream(String input, int bufLen, long startPosition) {
        BufferedInputStream reader = null;
        Pattern p = Pattern.compile("(https*)://(\\S+):(\\S+)@(\\S+)");
        Matcher m = p.matcher(input);
        boolean result = m.find();
        String protocol = null;
        String user = null;
        String pass = null;
        String URL = input;
        if (result) {
            protocol = m.group(1);
            user = m.group(2);
            pass = m.group(3);
            URL = protocol + "://" + m.group(4);
        }
        URL urlObj = null;
        try {
            urlObj = new URL(URL);
            URLConnection urlConn = urlObj.openConnection();
            if (user != null && pass != null) {
                String userPassword = user + ":" + pass;
                String encoding = Base64.encodeBase64String(userPassword.getBytes());
                urlConn.setRequestProperty("Authorization", "Basic " + encoding);
            }
            urlConn.setRequestProperty("Range", "bytes=" + startPosition + "-");
            p = Pattern.compile("://([^/]+)/(\\S+)");
            m = p.matcher(URL);
            result = m.find();
            if (result) {
                String path = m.group(2);
                String[] paths = path.split("/");
                fileName = paths[paths.length - 1];
                this.size = urlConn.getContentLength();
                reader = new BufferedInputStream(urlConn.getInputStream(), bufLen);
            } else {
                return (null);
            }
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return (null);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return (null);
        }
        return reader;
    }

    public BufferedInputStream getS3InputStream(String input, int bufLen, long startPosition, String accessKey, String secretKey) {
        BufferedInputStream reader = null;
        S3Object object = null;
        Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
        Matcher m = p.matcher(input);
        boolean result = m.find();
        String URL = input;
        if (result) {
            accessKey = m.group(1);
            secretKey = m.group(2);
            URL = "s3://" + m.group(3);
        }
        s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        p = Pattern.compile("s3://([^/]+)/(\\S+)");
        m = p.matcher(URL);
        result = m.find();
        if (result) {
            String bucket = m.group(1);
            String key = m.group(2);
            String[] paths = key.split("/");
            fileName = paths[paths.length - 1];
            try {
                GetObjectRequest gor = new GetObjectRequest(bucket, key);
                this.size = s3.getObject(gor).getObjectMetadata().getContentLength();
                gor.setRange(startPosition, size);
                object = s3.getObject(gor);
                reader = new BufferedInputStream(object.getObjectContent(), bufLen);
            } catch (AmazonServiceException e) {
                e.printStackTrace();
                return null;
            } catch (AmazonClientException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
        return reader;
    }

    public BufferedInputStream getS3InputStream(String input, int bufLen, long startPosition) {
        Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
        Matcher m = p.matcher(input);
        boolean result = m.find();
        String accessKey = null;
        String secretKey = null;
        if (!result) {
            try {
                HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                accessKey = settings.get("AWS_ACCESS_KEY");
                secretKey = settings.get("AWS_SECRET_KEY");
            } catch (Exception e) {
                e.printStackTrace();
                return (null);
            }
        }
        if (accessKey == null || secretKey == null) {
            return (null);
        }
        return getS3InputStream(input, bufLen, startPosition, accessKey, secretKey);
    }

    /**
   * Sets data encryption key.
   * 
   * @param value
   *          BASE64-encoded key
   */
    public void setDataEncryptionKeyString(String value) {
        byte[] bytes = getBase64().decode(value);
        dataEncryptionKey = new SecretKeySpec(bytes, DATA_ENCRYPTION_ALGORITHM);
    }

    private static Base64 getBase64() {
        return new Base64(Integer.MAX_VALUE, new byte[0]);
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
   * Enable class verbose mode.
   * 
   * @param verbose
   */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private Cipher createDecryptCipherInternal() throws Exception {
        Cipher cipher = Cipher.getInstance(DATA_ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, dataEncryptionKey);
        return cipher;
    }

    /**
   * Creates abstract pathname.
   * 
   * @param folderStore
   * @param email
   * @param fileName
   * @return
   */
    public static String createTargetPath(String folderStore, String email, String fileName) {
        String fileDownlodName = fileName.trim();
        String separator = java.io.File.separator;
        Date dateNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        StringBuilder strNow = new StringBuilder(dateFormat.format(dateNow));
        String pathCurrDir = (new StringBuilder()).append(folderStore).append(email).append(separator).append(strNow).append(separator).toString();
        java.io.File currDir = new java.io.File(pathCurrDir);
        if (!currDir.exists()) currDir.mkdirs();
        String targetPath = (new StringBuilder()).append(pathCurrDir).append(fileDownlodName).toString();
        return targetPath;
    }

    /**
   * Creates abstract pathname.
   * 
   * @param folderStore
   * @param email
   * @param fileName
   * @return
   */
    public static String createTargetDirectory(String folderStore, String email) {
        String separator = java.io.File.separator;
        Date dateNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        StringBuilder strNow = new StringBuilder(dateFormat.format(dateNow));
        String pathCurrDir = (new StringBuilder()).append(folderStore).append(email).append(separator).append(strNow).append(separator).toString();
        java.io.File currDir = new java.io.File(pathCurrDir);
        if (!currDir.exists()) currDir.mkdirs();
        String targetPath = (new StringBuilder()).append(pathCurrDir).toString();
        return targetPath;
    }

    public static long getFileSize(String path) throws Exception {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            Pattern p = Pattern.compile("(https*)://(\\S+):(\\S+)@(\\S+)");
            Matcher m = p.matcher(path);
            boolean result = m.find();
            String protocol = null;
            String user = null;
            String pass = null;
            String URL = path;
            if (result) {
                protocol = m.group(1);
                user = m.group(2);
                pass = m.group(3);
                URL = protocol + "://" + m.group(4);
            }
            URL urlObj = null;
            try {
                urlObj = new URL(URL);
                URLConnection urlConn = urlObj.openConnection();
                if (user != null && pass != null) {
                    String userPassword = user + ":" + pass;
                    String encoding = Base64.encodeBase64String(userPassword.getBytes());
                    urlConn.setRequestProperty("Authorization", "Basic " + encoding);
                }
                return urlConn.getContentLength();
            } catch (MalformedURLException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        if (path.startsWith("s3://")) {
            String accessKey = null;
            String secretKey = null;
            Pattern p = Pattern.compile("s3://(\\S+):(\\S+)@(\\S+)");
            Matcher m = p.matcher(path);
            boolean result = m.find();
            String URL = path;
            if (result) {
                accessKey = m.group(1);
                secretKey = m.group(2);
                URL = "s3://" + m.group(3);
            } else {
                try {
                    HashMap<String, String> settings = (HashMap<String, String>) ConfigTools.getSettings();
                    accessKey = settings.get("AWS_ACCESS_KEY");
                    secretKey = settings.get("AWS_SECRET_KEY");
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
            p = Pattern.compile("s3://([^/]+)/(\\S+)");
            m = p.matcher(URL);
            result = m.find();
            if (result) {
                String bucket = m.group(1);
                String key = m.group(2);
                try {
                    GetObjectRequest gor = new GetObjectRequest(bucket, key);
                    return s3.getObject(gor).getObjectMetadata().getContentLength();
                } catch (AmazonServiceException e) {
                    e.printStackTrace();
                    throw e;
                } catch (AmazonClientException e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                return 0;
            }
        } else {
            File file = new File(path);
            if (!file.exists()) {
                throw new IllegalStateException("File not exist " + path);
            }
            return file.length();
        }
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
}
