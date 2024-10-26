package pierre.util;

import pedro.system.*;
import pedro.desktopDeployment.*;
import pedro.util.SystemLog;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DownloadUtility {

    public void presentHyperlinkOptions(JEditorPane editorPane, URL url, PedroFormContext pedroFormContext, JDialog parentDialog) {
        try {
            String urlName = url.toString().toUpperCase();
            if (urlName.endsWith(".HTML") == true) {
                editorPane.setPage(url);
            } else if (urlName.endsWith(".PDZ") == true) {
                String[] downloadOptions = new String[2];
                downloadOptions[0] = "View with Pedro";
                downloadOptions[1] = "Download File";
                String selectedValue = (String) JOptionPane.showInputDialog(null, "Choose one", "Input", JOptionPane.INFORMATION_MESSAGE, null, downloadOptions, downloadOptions[0]);
                PedroApplicationContext pedroApplicationContext = pedroFormContext.getApplicationContext();
                if (selectedValue.equals(downloadOptions[0]) == true) {
                    launchWithPedro(url, pedroApplicationContext, parentDialog);
                } else if (selectedValue.equals(downloadOptions[1]) == true) {
                    download(url, parentDialog);
                }
            } else {
                download(url, parentDialog);
            }
        } catch (Exception exception) {
            SystemLog.addError(exception);
        }
    }

    private void launchWithPedro(URL url, PedroApplicationContext pedroApplicationContext, JDialog dialog) {
        StringBuffer buff = new StringBuffer();
        buff.append("C:");
        buff.append(File.separator);
        buff.append("butanol3.pdz");
        File fileToOpen = new File(url.getFile());
        try {
            PedroService pedroService = new PedroService(pedroApplicationContext, dialog);
            pedroService.openFile(fileToOpen);
            pedroService.setModal(true);
        } catch (Exception err) {
            err.printStackTrace(System.out);
        }
    }

    public void download(URL url, JDialog parentDialog) throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        int result = fileChooser.showSaveDialog(parentDialog);
        if (result == JFileChooser.CANCEL_OPTION) {
            return;
        }
        File currentFile = fileChooser.getSelectedFile();
        download(url, currentFile);
    }

    public void download(URL url, File targetFile) throws Exception {
        if (targetFile == null) {
            return;
        }
        System.out.println("DU download url==" + url.toString() + "==");
        System.out.println("DU download targetFile==" + targetFile.getAbsolutePath() + "==");
        URLConnection urlConnection = url.openConnection();
        System.out.println("DU download 3");
        DataInputStream in = new DataInputStream(urlConnection.getInputStream());
        DataOutputStream out = new DataOutputStream(new FileOutputStream(targetFile));
        int data = in.read();
        while (data != -1) {
            out.write(data);
            data = in.read();
        }
        out.flush();
        in.close();
        out.close();
    }
}
