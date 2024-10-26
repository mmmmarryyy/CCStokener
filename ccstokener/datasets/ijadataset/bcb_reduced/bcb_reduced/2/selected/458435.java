package com.wgo.precise.client.ui.view.projecttree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import remato.domain.common.Category;
import remato.domain.common.Project;
import remato.domain.common.Requirement;
import com.wgo.bpot.server.persist.Persistent;
import com.wgo.precise.client.ui.controller.ModelStatus;
import com.wgo.precise.client.ui.controller.RequirementPlugin;
import com.wgo.precise.client.ui.controller.exceptions.RematoClientException;
import com.wgo.precise.client.ui.view.util.ViewerAction;

public class CreatePdfAction extends ViewerAction {

    private static final String PDF_SERVICE = "/createpdf";

    private FileDialog selectFolderDialog;

    private Runnable pdfCreator;

    public CreatePdfAction(Shell shell, String name) {
        super(name);
        init(shell);
        setEnabled(!RequirementPlugin.getInstance().getSession().isDirty());
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
    }

    public CreatePdfAction(Shell shell, Project project) {
        this(shell, "Create project PDF");
        pdfCreator = new PdfCreator("project", project);
    }

    public CreatePdfAction(Shell shell, Category category) {
        this(shell, "Create category PDF");
        pdfCreator = new PdfCreator("category", category);
    }

    public CreatePdfAction(Shell shell, Requirement requirement) {
        this(shell, "Create requirement PDF");
        pdfCreator = new PdfCreator("requirement", requirement);
    }

    public void init(Shell shell) {
        selectFolderDialog = new FileDialog(shell, SWT.SAVE);
        selectFolderDialog.setFileName("myPdfFile.pdf");
        selectFolderDialog.setFilterExtensions(new String[] { "*.pdf" });
        setToolTipText("Create PDF");
    }

    @Override
    public void run() {
        pdfCreator.run();
        super.run();
    }

    private class PdfCreator implements Runnable {

        private String componentName = "UNKNOWN";

        private Persistent concept;

        public PdfCreator(String componentName, Persistent concept) {
            this.componentName = componentName;
            this.concept = concept;
        }

        public void run() {
            String fileName = selectFolderDialog.open();
            if (null != fileName) {
                File file = new File(fileName);
                String args = "component=" + componentName + "&dbId=" + concept.getDbId();
                URL remoteServiceUrl = RequirementPlugin.getInstance().getSession().getServerContextUrl();
                URL pdfRequestUrl = null;
                try {
                    pdfRequestUrl = new URL(remoteServiceUrl.getProtocol(), remoteServiceUrl.getHost(), remoteServiceUrl.getPort(), PDF_SERVICE + "?" + args);
                } catch (MalformedURLException e) {
                    throw new RematoClientException(new ModelStatus(IStatus.ERROR, "PDF creation error. Invalid remote URL-path: " + PDF_SERVICE + "?" + args, e));
                }
                writeUrlToFile(pdfRequestUrl, file);
            }
        }
    }

    public static void writeUrlToFile(String urlAddress, File file) {
        URL url;
        try {
            url = new URL(urlAddress);
            writeUrlToFile(url, file);
        } catch (MalformedURLException e) {
            throw new RematoClientException(new ModelStatus(IStatus.ERROR, "PDF creation error. Invalid remote URL: " + urlAddress, e));
        }
    }

    public static void writeUrlToFile(URL url, File file) {
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            throw new RematoClientException(new ModelStatus(IStatus.ERROR, "PDF creation error. Unable to open remote URL: " + url.toExternalForm(), e));
        }
        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RematoClientException(new ModelStatus(IStatus.ERROR, "PDF creation error. Unable to open local file: " + file.getAbsolutePath(), e));
        }
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            is.close();
            out.close();
        } catch (Exception e) {
            throw new RematoClientException(new ModelStatus(IStatus.ERROR, "PDF creation error. Unable to read form remote file or write to local file: " + file.getAbsolutePath(), e));
        }
        MessageDialog.openInformation(RequirementPlugin.getInstance().getActiveShell().getShell(), "Successfully created PDF", "The file is located at: " + file.getAbsolutePath());
    }
}
