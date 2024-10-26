import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Download applet
 */
public class RaphPhotoGalleryDownload extends JApplet implements ActionListener {

    private static final long serialVersionUID = 84562143214571238L;

    private Vector<RaphPhotoGalleryPhoto> photoList = new Vector<RaphPhotoGalleryPhoto>(200);

    private int photoListTotalSize = 0;

    private byte downloadState = 0;

    private JButton downloadButton;

    private File destinationDirectory;

    private JLabel progressBarTotalLabel1;

    private JLabel progressBarTotalLabel2;

    private JProgressBar progressBarTotal;

    private JLabel progressBarCurrentLabel;

    private JProgressBar progressBarCurrent;

    private JLabel speedLabel;

    private Date lastDate = null;

    private int lastTotalSize = 0;

    private LinkedList<Double> lastSpeed = new LinkedList<Double>();

    private int refreshSpeed;

    private double remainingTime;

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("Can't create GUI! " + e);
        }
    }

    public void createGUI() {
        try {
            System.getProperty("user.home");
        } catch (AccessControlException e) {
            System.err.println("AccessControlException! " + e);
            return;
        }
        String textColorString = getParameter("textColor");
        Color textColor = Color.BLACK;
        if (textColorString != null) textColor = Color.decode("0x" + textColorString);
        String backgroundColorString = getParameter("backgroundColor");
        Color backgroundColor = Color.WHITE;
        if (backgroundColorString != null) backgroundColor = Color.decode("0x" + backgroundColorString);
        Container contentPane = getContentPane();
        contentPane.setBackground(backgroundColor);
        Box box = Box.createVerticalBox();
        box.add(Box.createVerticalGlue());
        progressBarTotalLabel1 = new JLabel();
        progressBarTotalLabel1.setAlignmentX(Box.CENTER_ALIGNMENT);
        progressBarTotalLabel1.setForeground(textColor);
        box.add(progressBarTotalLabel1);
        progressBarTotalLabel2 = new JLabel();
        progressBarTotalLabel2.setAlignmentX(Box.CENTER_ALIGNMENT);
        progressBarTotalLabel2.setForeground(textColor);
        box.add(progressBarTotalLabel2);
        setProgressBarTotalLabels(0, 0, 0);
        progressBarTotal = new JProgressBar();
        progressBarTotal.setValue(0);
        progressBarTotal.setStringPainted(true);
        box.add(progressBarTotal);
        box.add(Box.createVerticalGlue());
        progressBarCurrentLabel = new JLabel();
        progressBarCurrentLabel.setAlignmentX(Box.CENTER_ALIGNMENT);
        progressBarCurrentLabel.setForeground(textColor);
        progressBarCurrentLabel.setText(" ");
        box.add(progressBarCurrentLabel);
        progressBarCurrent = new JProgressBar();
        progressBarCurrent.setValue(0);
        progressBarCurrent.setStringPainted(true);
        box.add(progressBarCurrent);
        box.add(Box.createVerticalGlue());
        speedLabel = new JLabel();
        speedLabel.setText(" ");
        speedLabel.setAlignmentX(Box.CENTER_ALIGNMENT);
        speedLabel.setForeground(textColor);
        box.add(speedLabel);
        box.add(Box.createVerticalGlue());
        downloadButton = new JButton(getParameter("i18n_download"));
        downloadButton.setAlignmentX(Box.CENTER_ALIGNMENT);
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(this);
        box.add(downloadButton);
        box.add(Box.createVerticalGlue());
        add(box);
    }

    public void reinitGUI() {
        progressBarTotal.setValue(0);
        progressBarCurrentLabel.setText("");
        progressBarCurrent.setValue(0);
        speedLabel.setText("");
        downloadButton.setText(getParameter("i18n_download"));
        downloadButton.setEnabled(true);
        downloadState = 0;
    }

    public boolean addFileToList(String id, String url, String fileName, String fileSize) {
        if (downloadState == 1) return false; else if (downloadState == 2) reinitGUI();
        boolean found = false;
        for (RaphPhotoGalleryPhoto photo : photoList) {
            if (photo.getId().equals(id)) found = true;
        }
        if (!found) {
            photoList.add(new RaphPhotoGalleryPhoto(id, url, fileName, Integer.parseInt(fileSize)));
            photoListTotalSize += Integer.parseInt(fileSize);
        }
        downloadButton.setEnabled(true);
        setProgressBarTotalLabels(0, photoList.size(), photoListTotalSize);
        return true;
    }

    public boolean removeFileFromList(String id) {
        if (downloadState == 1) return false; else if (downloadState == 2) reinitGUI();
        for (RaphPhotoGalleryPhoto photo : photoList) {
            if (photo.getId().equals(id)) {
                photoList.remove(photo);
                photoListTotalSize -= photo.getFileSize();
                break;
            }
        }
        if (photoList.size() == 0) downloadButton.setEnabled(false);
        setProgressBarTotalLabels(0, photoList.size(), photoListTotalSize);
        return true;
    }

    public boolean removeAllFilesFromList() {
        if (downloadState == 1) return false; else if (downloadState == 2) reinitGUI();
        photoList.removeAllElements();
        photoListTotalSize = 0;
        downloadButton.setEnabled(false);
        setProgressBarTotalLabels(0, photoList.size(), photoListTotalSize);
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(downloadButton)) {
            JFileChooser fc = new JFileChooser(destinationDirectory);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                downloadButton.setEnabled(false);
                downloadButton.setText(getParameter("i18n_downloading") + "...");
                destinationDirectory = fc.getSelectedFile();
                downloadState = 1;
                downloadFiles();
            }
        }
    }

    private void downloadFiles() {
        SwingWorker<Double, RaphPhotoGalleryPhoto> downloadFilesWorker = new SwingWorker<Double, RaphPhotoGalleryPhoto>() {

            Date startDownloadDate;

            @Override
            public Double doInBackground() {
                startDownloadDate = new Date();
                refreshSpeed = 0;
                lastDate = null;
                try {
                    int totalSizeRead = 0;
                    int totalNumberRead = 0;
                    for (RaphPhotoGalleryPhoto photo : photoList) {
                        URL url = new URL(getCodeBase().toString() + photo.getUrl());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        DataInputStream dis = new DataInputStream(connection.getInputStream());
                        FileOutputStream fos = new FileOutputStream(destinationDirectory.toString() + "/" + photo.getFileName());
                        byte[] b = new byte[65536];
                        int sizeRead;
                        photo.setProcessedSize(0);
                        totalNumberRead++;
                        while ((sizeRead = dis.read(b)) > -1) {
                            fos.write(b, 0, sizeRead);
                            totalSizeRead += sizeRead;
                            photo.addToProcessedSize(sizeRead);
                            photo.setTotalProcessedSize(totalSizeRead);
                            photo.setTotalProcessedNumber(totalNumberRead);
                            publish(photo);
                            try {
                            } catch (Exception ignore) {
                            }
                        }
                        fos.close();
                    }
                } catch (MalformedURLException e1) {
                    System.err.println("MalformedURLException: " + e1);
                } catch (IOException e2) {
                    System.err.println("IOException: " + e2);
                }
                long totalDiffTime = (new Date()).getTime() - startDownloadDate.getTime();
                double totalSpeed = photoListTotalSize / (totalDiffTime / 1000);
                return new Double(totalSpeed);
            }

            @Override
            protected void process(List<RaphPhotoGalleryPhoto> list) {
                RaphPhotoGalleryPhoto photo = list.get(list.size() - 1);
                progressBarTotal.setMaximum(photoListTotalSize);
                progressBarTotal.setValue(photo.getTotalProcessedSize());
                setProgressBarTotalLabels(photo.getTotalProcessedNumber(), photoList.size(), photoListTotalSize - photo.getTotalProcessedSize());
                progressBarCurrent.setMaximum(photo.getFileSize());
                progressBarCurrent.setValue(photo.getProcessedSize());
                progressBarCurrentLabel.setText(photo.getFileName());
                long diffTime = 0;
                if (lastDate == null) lastDate = new Date();
                diffTime = (new Date()).getTime() - lastDate.getTime();
                if (diffTime > 1000) {
                    double currentSpeed = (photo.getTotalProcessedSize() - lastTotalSize) / (diffTime / 1000.0);
                    if (currentSpeed > 0) lastSpeed.add(new Double(currentSpeed));
                    if (lastSpeed.size() == 10) lastSpeed.removeFirst();
                    double speed = 0;
                    for (Double d : lastSpeed) {
                        speed += d.doubleValue();
                    }
                    speed = Math.round(speed / lastSpeed.size());
                    if (refreshSpeed == 0) {
                        long totalDiffTime = (new Date()).getTime() - startDownloadDate.getTime();
                        double totalSpeed = (photo.getTotalProcessedSize()) / (totalDiffTime / 1000.0);
                        remainingTime = (photoListTotalSize - photo.getTotalProcessedSize()) / totalSpeed;
                        if (remainingTime < 8) refreshSpeed = 0; else refreshSpeed = 3;
                    } else {
                        refreshSpeed--;
                        remainingTime -= diffTime / 1000.0;
                    }
                    speedLabel.setText(String.format("%.0f", speed / 1024) + " " + getParameter("i18n_speedUnit") + " :  " + String.format("%.0f", remainingTime) + " s");
                    lastDate = new Date();
                    lastTotalSize = photo.getTotalProcessedSize();
                }
            }

            @Override
            protected void done() {
                try {
                    speedLabel.setText(String.format("%.0f", get().doubleValue() / 1024) + " " + getParameter("i18n_speedUnit"));
                    downloadButton.setText(getParameter("i18n_finished"));
                    downloadState = 2;
                } catch (Exception ignore) {
                }
            }
        };
        downloadFilesWorker.execute();
    }

    private String getSizeHumanReadable(int size) {
        double dSize = size / 1024;
        String unit = getParameter("i18n_kB");
        if (dSize > 1024) {
            dSize /= 1024;
            unit = getParameter("i18n_MB");
        }
        return String.format("%.1f", dSize) + " " + unit;
    }

    private void setProgressBarTotalLabels(int processedItems, int totalItems, int sizeLeft) {
        String s1;
        String s2;
        if (processedItems > 0) {
            if (totalItems > 1) {
                s1 = processedItems + " / " + totalItems + " " + getParameter("i18n_files");
            } else {
                s1 = processedItems + " / " + totalItems + " " + getParameter("i18n_file");
            }
        } else {
            if (totalItems > 1) {
                s1 = totalItems + " " + getParameter("i18n_files");
            } else {
                s1 = totalItems + " " + getParameter("i18n_file");
            }
        }
        s2 = "[" + getSizeHumanReadable(sizeLeft) + "]";
        progressBarTotalLabel1.setText(s1);
        progressBarTotalLabel2.setText(s2);
    }
}
