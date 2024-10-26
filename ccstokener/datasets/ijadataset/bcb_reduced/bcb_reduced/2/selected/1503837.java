package de.shandschuh.jaolt.gui.core;

import java.io.File;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.jgoodies.forms.builder.PanelBuilder;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.core.auction.Picture;
import de.shandschuh.jaolt.core.exception.CommonException;
import de.shandschuh.jaolt.gui.FormManager;
import de.shandschuh.jaolt.gui.Lister;
import de.shandschuh.jaolt.tools.download.core.Download;
import de.shandschuh.jaolt.tools.download.core.SimpleFileDownloader;
import de.shandschuh.jaolt.tools.url.URLHelper;

public class PictureURLFormManager extends FormManager {

    private PictureURLJDialog parent;

    private JTextArea urlJTextArea;

    private JProgressBar progressBar;

    private JLabel sizeJLabel;

    private PictureJPanel pictureJPanel;

    private Picture[] pictures;

    public PictureURLFormManager(PictureURLJDialog parent, Picture selectedPicture) {
        this.parent = parent;
        urlJTextArea = new JTextArea(3, 30);
        if (selectedPicture != null && selectedPicture.getURL() != null) {
            urlJTextArea.setText(selectedPicture.getURL().toString());
        }
        pictureJPanel = new PictureJPanel(selectedPicture, 30);
        sizeJLabel = new JLabel();
        progressBar = new JProgressBar();
    }

    @Override
    protected void addPanelBuilderComponents(PanelBuilder panelBuilder) {
        panelBuilder.addLabel(Language.translateStatic("URL"), getCellConstraints(1, 1));
        panelBuilder.add(new JScrollPane(urlJTextArea), getCellConstraints(3, 1));
        panelBuilder.add(pictureJPanel, getCellConstraints(1, 3));
        panelBuilder.add(sizeJLabel, getCellConstraints(3, 3));
        panelBuilder.add(progressBar, getCellConstraints(1, 5, 3));
    }

    @Override
    protected String getColumnLayout() {
        return "p, 4dlu, fill:p:grow";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected String getRowLayout() {
        return "p, 3dlu, fill:max(p;30dlu):grow, 3dlu, p";
    }

    @Override
    public boolean rebuildNeeded() {
        return false;
    }

    @Override
    protected void reloadLocal(boolean rebuild) {
    }

    @Override
    protected void saveLocal() throws Exception {
        parent.setSuccess(false);
        String fileName = Lister.getCurrentInstance().getMember().getFileName();
        File pictureDir = new File(fileName.substring(0, fileName.lastIndexOf(".")) + File.separator + "online" + File.separator);
        pictureDir.mkdirs();
        URL url = null;
        String[] urlStrings = urlJTextArea.getText().trim().split("\\s");
        pictures = new Picture[urlStrings.length];
        for (int n = 0, i = urlStrings.length; n < i; n++) {
            try {
                url = URLHelper.contructURL(urlStrings[n]);
            } catch (Exception e) {
                throw new CommonException(Language.translateStatic("ERROR_INVALIDURL"));
            }
            sizeJLabel.setText(Download.getFormatedSize(url.openConnection().getContentLength()));
            File file = SimpleFileDownloader.downloadFile(url, pictureDir, progressBar);
            pictures[n] = new Picture(file);
            pictures[n].setURL(url);
            pictureJPanel.setPicture(pictures[n]);
        }
        parent.setSuccess(true);
        parent.dispose();
    }

    @Override
    protected void validateLocal() throws Exception {
    }

    @Override
    public boolean saveIsThreaded() {
        return false;
    }

    public Picture[] getPictures() {
        return pictures;
    }
}
