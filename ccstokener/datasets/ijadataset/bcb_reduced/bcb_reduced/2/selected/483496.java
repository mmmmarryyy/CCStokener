package gnu.kinsight.report;

import java.io.*;
import java.net.*;
import javax.swing.*;
import gnu.kinsight.*;

public abstract class KinsightReport {

    ProgressMonitor progress;

    int progress_amt = 0;

    Family family;

    String directory;

    /**
     * <code>go</code> starts the report making process
     *
     * @param dir the directory in which to save the "html", "latex",
     *            etc subdirectory
     * @exception IOException if problems opening report files files and so on
     */
    protected abstract void go(String dir) throws IOException;

    /**
     * <code>createReport</code> starts the report making process in a new
     * thread. 
     */
    public void createReport() {
        progress.setMillisToPopup(0);
        final SwingWorker worker = new SwingWorker() {

            public Object construct() {
                try {
                    go(directory);
                } catch (IOException E) {
                    E.printStackTrace();
                }
                return null;
            }
        };
        worker.start();
    }

    protected String getName(Person p) {
        StringBuffer name = new StringBuffer();
        name.append(p.getFirstName());
        name.append(" " + p.getMiddleName() + " " + p.getLastName() + p.getSuffix());
        if (!p.getMaidenName().equals("")) name.append(" (" + p.getMaidenName() + ")");
        return name.toString();
    }

    protected String getSex(Person p) {
        return (p.getSex() == Person.MALE ? "Male" : "Female");
    }

    protected String getBirthDeath(Person p) {
        Date bdate = p.getBirthDate();
        Date ddate = p.getDeathDate();
        if (bdate != null || ddate != null) {
            String bstring = bdate == null ? "?" : bdate.toString();
            String dstring = p.isDead() ? (ddate == null ? "?" : ddate.toString()) : " ";
            return "(" + bstring + "-" + dstring + ")";
        } else return null;
    }

    protected void copyFile(String file, String dir, String name) throws IOException {
        copyFile(ClassLoader.getSystemResource(file), dir, name);
    }

    protected void copyFile(URL url, String dir, String name) throws IOException {
        copyFile(url.openStream(), dir, name);
    }

    protected void copyFile(InputStream is, String dir, String name) throws IOException {
        OutputStream os = new FileOutputStream(dir + name);
        int i;
        while ((i = is.read()) != -1) os.write(i);
        os.close();
    }
}
