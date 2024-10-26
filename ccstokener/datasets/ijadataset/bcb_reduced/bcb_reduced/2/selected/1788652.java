package gnu.rhuelga.cirl.cirllib;

import java.net.*;
import java.io.*;

class ResourceConnection extends Thread {

    private Resource resource;

    private boolean status;

    private FileOutputStream file_out;

    private BufferedInputStream bin;

    private int command;

    private static final int DOWNLOAD = 0;

    private static final int HEADINFO = 1;

    private static final int WAIT_FOR_A_BYTE_TIME = 10;

    private boolean life;

    ResourceConnection(Resource resource) {
        this.resource = resource;
        status = false;
        file_out = null;
        bin = null;
        command = DOWNLOAD;
    }

    public void run() {
        life = true;
        switch(command) {
            case DOWNLOAD:
                downloadThread();
                break;
            case HEADINFO:
                headinfoThread();
                break;
            default:
                break;
        }
    }

    void download() {
        command = DOWNLOAD;
        this.start();
    }

    private void downloadThread() {
        int c;
        status = false;
        try {
            URLConnection urlc = resource.url.openConnection();
            File f = resource.createFile();
            boolean resume = false;
            resource.resetBytesDown();
            if (f.exists()) {
                if (f.lastModified() > resource.date.getTime()) {
                    if ((resource.getFileSize() == f.length())) {
                        status = true;
                        return;
                    } else {
                        urlc.setRequestProperty("Range", "bytes=" + f.length() + "-");
                        resume = true;
                        resource.incrementBytesDown(f.length());
                        System.out.println("Resume download");
                        System.out.println("file length: " + f.length());
                    }
                }
            }
            urlc.connect();
            bin = new BufferedInputStream(urlc.getInputStream());
            file_out = new FileOutputStream(f.getPath(), resume);
            while (life) {
                if (bin.available() > 0) {
                    c = bin.read();
                    if (c == -1) {
                        break;
                    }
                    file_out.write(c);
                    if (resource.incrementBytesDown()) {
                        break;
                    } else {
                        continue;
                    }
                }
                sleep(WAIT_FOR_A_BYTE_TIME);
            }
            file_out.flush();
            status = true;
        } catch (IOException e) {
            System.out.println("excepcion cpoy file");
        } catch (InterruptedException e) {
            System.out.println("InterruptException download");
            System.out.println(e);
        }
    }

    void headinfo() {
        command = HEADINFO;
        this.start();
    }

    private void headinfoThread() {
        try {
            URLConnection urlc = resource.url.openConnection();
            resource.setFileSize(urlc.getContentLength());
            resource.setDate(urlc.getLastModified());
        } catch (IOException e) {
            System.out.println("Error ResourceConnection, downloading headinfo");
            System.out.println(e);
        }
    }

    void stopDownload() {
        life = false;
    }

    void close() {
        if (this.isAlive()) {
            this.interrupt();
            while (!this.isInterrupted()) {
                System.out.println(".");
            }
        }
        try {
            if (bin != null) {
                bin.close();
                bin = null;
            }
            if (file_out != null) {
                file_out.close();
                file_out = null;
            }
        } catch (IOException e) {
            System.out.println("Error: close in ResourceConnection");
            System.out.println(e);
        }
    }

    boolean getStatus() {
        return status;
    }
}
