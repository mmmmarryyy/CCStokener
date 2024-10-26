package de.miethxml.cocoon.installer.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author <a href="mailto:simon.mieth@gmx.de">Simon Mieth</a>
 *
 *
 *
 */
public class DownloadView {

    private String installdir;

    private String unpackdir;

    private String url;

    private int filelength;

    private JProgressBar progressbar;

    private JLabel bytes;

    private JLabel step;

    private JLabel time;

    private JPanel panel;

    /**
     *
     */
    public DownloadView() {
        super();
    }

    public void init() {
        FormLayout layout = new FormLayout("3dlu,pref,2dlu,fill:80dlu:grow,3dlu", "3dlu,p,3dlu,p,2dlu,p,2dlu,p,9dlu,p,3dlu,p,3dlu");
        PanelBuilder panelbuilder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        panelbuilder.addSeparator("Info", cc.xywh(2, 2, 3, 1));
        panelbuilder.add(new JLabel("Step:"), cc.xy(2, 4));
        step = new JLabel("");
        panelbuilder.add(step, cc.xy(4, 4));
        panelbuilder.add(new JLabel("Count:"), cc.xy(2, 6));
        bytes = new JLabel("");
        panelbuilder.add(bytes, cc.xy(4, 6));
        panelbuilder.add(new JLabel("Time:"), cc.xy(2, 8));
        time = new JLabel("");
        panelbuilder.add(time, cc.xy(4, 8));
        panelbuilder.addSeparator("Progress", cc.xywh(2, 10, 3, 1));
        progressbar = new JProgressBar();
        panelbuilder.add(progressbar, cc.xywh(2, 12, 3, 1));
        panel = panelbuilder.getPanel();
    }

    public JComponent getView() {
        return panel;
    }

    public void setCocoonDirectory(String path) {
        this.installdir = path;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public void doInstallAction(CocoonInstallComponent parent) {
        boolean complete = true;
        String filename = null;
        try {
            URL url = new URL(this.url);
            filename = url.getFile();
            if (filename.lastIndexOf("/") > -1) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                System.out.println("filename=" + filename + " newlocation=" + installdir + File.separator + filename);
            }
            URLConnection connection = url.openConnection();
            filelength = connection.getContentLength();
            step.setText("Download now");
            progressbar.setMinimum(0);
            progressbar.setMaximum(filelength);
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(installdir + File.separator + filename));
            int current = 0;
            int process = 0;
            int length = -1;
            byte[] bytes = new byte[1024];
            while ((length = in.read(bytes, 0, bytes.length)) != -1) {
                out.write(bytes, 0, length);
                current += length;
                progressbar.setValue(current);
                this.bytes.setText(current + "/" + filelength);
            }
            in.close();
            out.flush();
            out.close();
            in = null;
            out = null;
            complete = true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (complete && (filename != null)) {
            try {
                ZipFile ziparchiv = new ZipFile(installdir + File.separator + filename);
                unpackdir = null;
                step.setText("Unpack");
                progressbar.setValue(0);
                progressbar.setMinimum(0);
                int process = 0;
                int progress = 0;
                Enumeration e = ziparchiv.entries();
                progressbar.setMaximum(ziparchiv.size());
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    if (entry.isDirectory()) {
                        if (unpackdir == null) {
                            unpackdir = installdir + File.separator + entry.getName();
                        }
                        File dir = new File(installdir + File.separator + entry.getName());
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        process++;
                        progressbar.setValue(process);
                    } else {
                        BufferedInputStream in = new BufferedInputStream(ziparchiv.getInputStream(entry));
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(installdir + File.separator + entry.getName()));
                        int current = 0;
                        int length = -1;
                        byte[] bytes = new byte[1024];
                        while ((length = in.read(bytes, 0, bytes.length)) != -1) {
                            out.write(bytes, 0, length);
                            current += length;
                            this.bytes.setText(current + "/" + entry.getSize());
                        }
                        in.close();
                        out.flush();
                        out.close();
                        in = null;
                        out = null;
                        process++;
                        progressbar.setValue(process);
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        parent.setDownloadComplete(true);
    }

    public String getNewCocoonDirectory() {
        return unpackdir;
    }
}
