package org.plazmaforge.studio.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import javax.imageio.ImageIO;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.plazmaforge.studio.core.util.ErrorUtils;

/** 
 * @author Oleh Hapon
 * $Id: AbstractStudioPlugin.java,v 1.7 2010/05/03 16:44:16 ohapon Exp $
 */
public abstract class AbstractStudioPlugin extends AbstractUIPlugin {

    public String getSymbolicName() {
        return getBundle().getSymbolicName();
    }

    public void log(IStatus istatus) {
        getLog().log(istatus);
    }

    public void log(String message) {
        log(message, IStatus.INFO);
    }

    public void log(String message, int staus) {
        log(message, null, staus);
    }

    public void logError(Throwable error) {
        logError("", error);
    }

    public void logError(String message, Throwable error) {
        log(message, error, IStatus.ERROR);
    }

    public void log(String message, Throwable error, int status) {
        Throwable cause = getCause(error);
        int j = 0;
        int curStatus = status;
        if (curStatus < IStatus.OK || curStatus > IStatus.CANCEL) {
            curStatus = IStatus.INFO;
        }
        log(new Status(curStatus, getSymbolicName(), j, message == null ? "<null>" : message, cause));
    }

    public void logWarn(String message) {
        log(message, IStatus.WARNING);
    }

    public void logInfo(String message) {
        log(message, IStatus.INFO);
    }

    protected static Throwable getCause(Throwable ex) {
        return ErrorUtils.getCause(ex);
    }

    public static void openErrorDialog(String message, Throwable error) {
        Throwable cause = getCause(error);
        Status status = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0, cause.getMessage(), cause);
        openErrorDialog(getShell(), "Error", message, status);
    }

    public static void openErrorDialog(String message, String reason) {
        Status status = reason == null ? null : new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0, reason, null);
        openErrorDialog(getShell(), "Error", message, status);
    }

    public static void openErrorDialog(String message) {
        openErrorDialog(message, (String) null);
    }

    public static void openErrorDialog(Throwable error) {
        openErrorDialog(null, error);
    }

    public static void openWarningDialog(String title, String message) {
        MessageDialog.openWarning(getShell(), title, message);
    }

    public static void openWarningDialog(String message) {
        MessageDialog.openWarning(getShell(), "Warning", message);
    }

    public static void openInformationDialog(String title, String message) {
        openInformationDialog(getShell(), title, message);
    }

    public static void openInformationDialog(String message) {
        openInformationDialog(null, message);
    }

    public static boolean openConfirmDialog(String title, String message) {
        return openConfirmDialog(getShell(), title, message);
    }

    public static boolean openConfirmDialog(String message) {
        return openConfirmDialog(null, message);
    }

    public static int openConfirmYesNoCancelDialog(String title, String message) {
        return openConfirmYesNoCancelDialog(getShell(), title, message);
    }

    public static int openConfirmYesNoCancelDialog(String message) {
        return openConfirmYesNoCancelDialog(null, message);
    }

    public static void openErrorDialog(Shell shell, String title, String message, Status status) {
        if (status == null) {
            MessageDialog.openError(getShell(), title, message);
            return;
        }
        ErrorDialog.openError(shell, title, message, status);
    }

    public static void openErrorDialog(Shell shell, String title, String message) {
        openErrorDialog(shell, title, message, null);
    }

    public static void openWarningDialog(Shell shell, String title, String message) {
        MessageDialog.openWarning(shell, title, message);
    }

    public static void openInformationDialog(Shell shell, String title, String message) {
        MessageDialog.openInformation(shell, title, message);
    }

    public static boolean openConfirmDialog(Shell shell, String title, String message) {
        return MessageDialog.openConfirm(shell, title, message);
    }

    public static int openConfirmYesNoCancelDialog(Shell parent, String title, String message) {
        String[] dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
        MessageDialog dialog = new MessageDialog(parent, title, null, message, MessageDialog.QUESTION, dialogButtonLabels, 0);
        return dialog.open();
    }

    public static Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static String format(String pattern, String argument) {
        return MessageFormat.format(pattern, new String[] { argument });
    }

    public void handleError(String message, Throwable error) {
        logError(message, error);
        openErrorDialog(message, error);
    }

    public void handleError(Throwable error) {
        logError("Error", error);
        openErrorDialog("Error", error);
    }

    public static URL getURL(String pluginId, String filePath) {
        if (pluginId == null || filePath == null) {
            throw new IllegalArgumentException();
        }
        Bundle bundle = Platform.getBundle(pluginId);
        if (!BundleUtility.isReady(bundle)) {
            return null;
        }
        URL url = BundleUtility.find(bundle, filePath);
        if (url == null) {
            try {
                url = new URL(filePath);
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return url;
    }

    public static InputStream getResourceStream(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL is null");
        }
        try {
            return new BufferedInputStream(url.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public static InputStream getResourceStream(String pluginId, String filePath) {
        URL url = getURL(pluginId, filePath);
        if (url == null) {
            return null;
        }
        return getResourceStream(url);
    }

    public static java.awt.Image getAWTImage(String pluginId, String filePath) {
        InputStream in = getResourceStream(pluginId, filePath);
        if (in == null) {
            return null;
        }
        try {
            java.awt.Image image = ImageIO.read(in);
            return image;
        } catch (Exception ex) {
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
