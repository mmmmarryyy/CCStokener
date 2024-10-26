package downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JProgressBar;
import downloader.IHM.*;

/**
 *
 * @author Julien
 */
public class FileToDownload extends Thread {

    private String myURL;

    private String targetDirectory;

    private JProgressBar myProgressBar;

    private MainWindows myMainWindows;

    private String fileName;

    public FileToDownload(String myURL, String targetDirectory, MainWindows myMainWindows) {
        this.myURL = myURL;
        this.targetDirectory = targetDirectory;
        this.myMainWindows = myMainWindows;
    }

    @Override
    public void run() {
        InputStream input = null;
        FileOutputStream writeFile = null;
        try {
            URL url = new URL(myURL);
            URLConnection connection = url.openConnection();
            int fileLength = connection.getContentLength();
            System.out.println("Taille du fichier : " + fileLength + " ko");
            String fileType = connection.getContentType();
            System.out.println(fileType);
            if (fileLength == -1) {
                System.out.println("Invalide URL or file.");
                return;
            }
            input = connection.getInputStream();
            fileName = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
            String targetFile = targetDirectory + '\\' + fileName;
            myProgressBar = new JProgressBar(0, fileLength);
            myProgressBar.setString(fileName);
            myProgressBar.setStringPainted(true);
            myProgressBar.setSize(494, 25);
            myMainWindows.addProgressBar(myProgressBar);
            writeFile = new FileOutputStream(targetFile);
            byte[] buffer = new byte[512];
            int read;
            int i = 0;
            while ((read = input.read(buffer)) > 0) {
                writeFile.write(buffer, 0, read);
                i = i + read;
                myProgressBar.setValue(i);
            }
            writeFile.flush();
            System.out.println("Done : " + fileName);
        } catch (IOException e) {
            System.out.println("Error while trying to download the file.");
            e.printStackTrace();
        } finally {
            try {
                writeFile.close();
                input.close();
                myProgressBar.setString("Completed : " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
