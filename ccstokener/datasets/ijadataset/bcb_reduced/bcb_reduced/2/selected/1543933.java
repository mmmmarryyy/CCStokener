package javapoint.Update;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import javapoint.Main;
import javapoint.MainFrame;
import javapoint.StaticClasses.Utilities;
import javapoint.xml.Constants;
import javapoint.xml.SlidesWriter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

/**
 *
 * @author Kyle
 */
class ProgressMonitorDemo extends JDialog implements ActionListener, PropertyChangeListener {

    private ProgressMonitor progressMonitor;

    private JButton startButton;

    private JLabel message;

    private Task task;

    private URL url;

    private MainFrame mainFrame;

    double contentLength = -1;

    class Task extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            java.io.FileOutputStream fos = null;
            try {
                String localFile = "JavaPointNew.jar";
                java.io.BufferedInputStream in = null;
                try {
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    contentLength = httpConn.getContentLength();
                    if (contentLength == -1) {
                        System.out.println("unknown content length");
                    } else {
                        System.out.println("content length: " + contentLength + " bytes");
                    }
                    in = new java.io.BufferedInputStream(httpConn.getInputStream());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                fos = new java.io.FileOutputStream(localFile);
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte[] data = new byte[1024];
                int x = 0;
                int bytes = 0;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    bout.write(data, 0, x);
                    bytes += x;
                    setProgress((int) ((Math.min(bytes * 100 / contentLength, 100)) + .5));
                }
                bout.close();
                in.close();
                File jarFile = mainFrame.presentationFile;
                if (jarFile.exists()) {
                    final File temporaryJarFile = Utilities.createTemporaryFile("presentation", ".jar", false, null);
                    final JarFile updatedJarFile = new JarFile(localFile);
                    final JarOutputStream output = new JarOutputStream(new FileOutputStream(temporaryJarFile));
                    for (final JarEntry entry : Utilities.toList(updatedJarFile.entries())) {
                        if (!entry.getName().startsWith(Constants.JAR_ENTRY_PRESENTATION_PREFIX)) {
                            final InputStream entryStream = updatedJarFile.getInputStream(entry);
                            output.putNextEntry(entry);
                            Utilities.write(entryStream, output);
                        }
                    }
                    new SlidesWriter().write(mainFrame.getSlides(), output);
                    jarFile.delete();
                    new File(localFile).delete();
                    if (!temporaryJarFile.renameTo(jarFile)) {
                        throw new RuntimeException("Failed to update " + jarFile);
                    }
                } else {
                    new SlidesWriter().write(mainFrame.getSlides(), new JarOutputStream(new FileOutputStream(jarFile)));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public void done() {
            startButton.setEnabled(true);
            progressMonitor.setProgress(0);
        }
    }

    public ProgressMonitorDemo(MainFrame mainFrame, final Updater u) {
        super();
        setTitle("Checking For Updates...");
        setSize(300, 150);
        this.setLocationByPlatform(true);
        startButton = new JButton("Start Download");
        startButton.setActionCommand("start");
        startButton.addActionListener(this);
        startButton.setEnabled(false);
        add(startButton, BorderLayout.PAGE_END);
        add(message = new JLabel(" Checking For Updates..."), BorderLayout.CENTER);
        message.setSize(this.getWidth(), message.getHeight());
        this.setVisible(true);
        this.repaint();
        EventQueue.invokeLater(new Runnable() {

            @Override
            public final void run() {
                int revision = u.getNewRevision();
                System.out.println(revision);
                if (revision == -1) {
                    message.setText(" Error while checking for updates");
                } else if (revision > javapoint.StaticClasses.Utilities.getCurrentRevision()) {
                    startButton.setEnabled(true);
                    url = u.getFile();
                    message.setText(" Click to update Java Point from " + javapoint.StaticClasses.Utilities.getCurrentRevision() + " to version " + revision);
                    Main.writeToServer("Updating JavaPoint from version " + javapoint.StaticClasses.Utilities.getCurrentRevision() + " to version " + revision);
                } else {
                    message.setText(" Java Point is up to date");
                }
            }
        });
    }

    /**
     * Invoked when the user presses the start button.
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        progressMonitor = new ProgressMonitor(ProgressMonitorDemo.this, "Downloading JavaPoint...", "", 0, 100);
        progressMonitor.setProgress(0);
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
        startButton.setEnabled(false);
    }

    /**
     * Invoked when task's progress property changes.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);
            String monitorMessage = String.format("Completed %d%%.\n", progress);
            progressMonitor.setNote(monitorMessage);
            if (progressMonitor.isCanceled() || task.isDone()) {
                if (progressMonitor.isCanceled()) {
                    task.cancel(true);
                }
                startButton.setEnabled(true);
            }
        }
    }
}
